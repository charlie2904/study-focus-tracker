package com.focusassistant.backend.service;

import com.focusassistant.backend.dto.PomodoroCompleteRequest;
import com.focusassistant.backend.dto.PomodoroStartRequest;
import com.focusassistant.backend.dto.PomodoroStateResponse;
import com.focusassistant.backend.model.*;
import com.focusassistant.backend.repository.PomodoroSessionRepository;
import com.focusassistant.backend.repository.StudySessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class PomodoroService {

    private static final int DEFAULT_FOCUS_MINUTES = 25;
    private static final int DEFAULT_SHORT_BREAK_MINUTES = 5;
    private static final int DEFAULT_LONG_BREAK_MINUTES = 15;

    private static final List<PomodoroStatus> ACTIVE_STATUSES =
            List.of(PomodoroStatus.RUNNING, PomodoroStatus.PAUSED, PomodoroStatus.ON_BREAK);

    private final PomodoroSessionRepository pomodoroRepository;
    private final StudySessionRepository studySessionRepository;

    public PomodoroService(PomodoroSessionRepository pomodoroRepository,
                           StudySessionRepository studySessionRepository) {
        this.pomodoroRepository = pomodoroRepository;
        this.studySessionRepository = studySessionRepository;
    }

    // ================= START =================

    @Transactional
    public PomodoroStateResponse start(User user, PomodoroStartRequest request) {

        pomodoroRepository.findByUserAndStatusIn(user, ACTIVE_STATUSES)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "You already have an active session. Complete or abandon it first.");
                });

        PomodoroSession session = new PomodoroSession();
        session.setSubject(request.getSubject());
        session.setFocusMinutes(orDefault(request.getFocusMinutes(), DEFAULT_FOCUS_MINUTES));
        session.setShortBreakMinutes(orDefault(request.getShortBreakMinutes(), DEFAULT_SHORT_BREAK_MINUTES));
        session.setLongBreakMinutes(orDefault(request.getLongBreakMinutes(), DEFAULT_LONG_BREAK_MINUTES));
        session.setStatus(PomodoroStatus.RUNNING);
        session.setStartedAt(Instant.now());
        session.setUser(user);

        return toState(pomodoroRepository.save(session));
    }

    // ================= ACTIVE =================

    public PomodoroStateResponse getActive(User user) {
        return toState(requireActive(user));
    }

    // ================= PAUSE =================

    @Transactional
    public PomodoroStateResponse pause(User user) {

        PomodoroSession session = requireActive(user);

        if (session.getStatus() != PomodoroStatus.RUNNING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Can only pause a running session");
        }

        session.setStatus(PomodoroStatus.PAUSED);
        session.setPausedAt(Instant.now());
        session.setInterruptions(session.getInterruptions() + 1);

        return toState(pomodoroRepository.save(session));
    }

    // ================= RESUME =================

    @Transactional
    public PomodoroStateResponse resume(User user) {

        PomodoroSession session = requireActive(user);

        if (session.getStatus() == PomodoroStatus.PAUSED) {
            session.setAccumulatedPausedSeconds(
                    session.getAccumulatedPausedSeconds() + secondsSince(session.getPausedAt()));
            session.setPausedAt(null);

        } else if (session.getStatus() == PomodoroStatus.ON_BREAK) {
            session.setAccumulatedBreakSeconds(
                    session.getAccumulatedBreakSeconds() + secondsSince(session.getBreakStartedAt()));
            session.setBreakStartedAt(null);

        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Session is already running");
        }

        session.setStatus(PomodoroStatus.RUNNING);

        return toState(pomodoroRepository.save(session));
    }

    // ================= BREAK =================

    @Transactional
    public PomodoroStateResponse startBreak(User user) {

        PomodoroSession session = requireActive(user);

        if (session.getStatus() != PomodoroStatus.RUNNING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Can only start a break from a running session");
        }

        session.setStatus(PomodoroStatus.ON_BREAK);
        session.setBreakStartedAt(Instant.now());
        session.setBreaksTaken(session.getBreaksTaken() + 1);

        return toState(pomodoroRepository.save(session));
    }

    // ================= COMPLETE =================

    @Transactional
    public StudySession complete(User user, PomodoroCompleteRequest request) {

        PomodoroSession session = requireActive(user);

        // Close out whichever timer is currently open
        if (session.getStatus() == PomodoroStatus.PAUSED) {
            session.setAccumulatedPausedSeconds(
                    session.getAccumulatedPausedSeconds() + secondsSince(session.getPausedAt()));
            session.setPausedAt(null);
        } else if (session.getStatus() == PomodoroStatus.ON_BREAK) {
            session.setAccumulatedBreakSeconds(
                    session.getAccumulatedBreakSeconds() + secondsSince(session.getBreakStartedAt()));
            session.setBreakStartedAt(null);
        }

        Instant now = Instant.now();
        session.setEndedAt(now);
        session.setStatus(PomodoroStatus.COMPLETED);
        pomodoroRepository.save(session);

        // Convert into a permanent StudySession record
        long focusSeconds = elapsedFocusSeconds(session, now);
        int focusMinutes = (int) Math.max(1, Math.round(focusSeconds / 60.0));

        ZoneId zone = ZoneId.systemDefault();

        StudySession study = new StudySession();
        study.setSubject(session.getSubject());
        study.setDuration(focusMinutes);
        study.setPlannedDuration(session.getFocusMinutes());
        study.setFocusRating(request.getFocusRating());
        study.setSessionDate(LocalDate.ofInstant(session.getStartedAt(), zone));
        study.setStartTime(LocalTime.ofInstant(session.getStartedAt(), zone).withNano(0));
        study.setBreaksTaken(session.getBreaksTaken());
        study.setInterruptions(session.getInterruptions());
        study.setNotes(request.getNotes());
        study.setUser(user);

        double focusScore = 0.0;
        if (session.getFocusMinutes() > 0) {
            focusScore = ((double) focusMinutes / session.getFocusMinutes())
                    * request.getFocusRating() * 20;
        }
        study.setFocusScore(Math.round(focusScore * 100.0) / 100.0);

        return studySessionRepository.save(study);
    }

    // ================= ABANDON =================

    @Transactional
    public void abandon(User user) {
        PomodoroSession session = requireActive(user);
        session.setStatus(PomodoroStatus.ABANDONED);
        session.setEndedAt(Instant.now());
        pomodoroRepository.save(session);
    }

    // ================= HELPERS =================

    private PomodoroSession requireActive(User user) {
        return pomodoroRepository.findByUserAndStatusIn(user, ACTIVE_STATUSES)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active Pomodoro session"));
    }

    private int orDefault(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private long secondsSince(Instant from) {
        if (from == null) return 0;
        return Duration.between(from, Instant.now()).getSeconds();
    }

    /**
     * Focus time = wall clock since start, minus all pauses and breaks.
     * Computed on demand rather than ticked, so it survives restarts and never drifts.
     */
    private long elapsedFocusSeconds(PomodoroSession session, Instant now) {

        long wallClock = Duration.between(session.getStartedAt(), now).getSeconds();

        long paused = session.getAccumulatedPausedSeconds();
        if (session.getStatus() == PomodoroStatus.PAUSED && session.getPausedAt() != null) {
            paused += Duration.between(session.getPausedAt(), now).getSeconds();
        }

        long onBreak = session.getAccumulatedBreakSeconds();
        if (session.getStatus() == PomodoroStatus.ON_BREAK && session.getBreakStartedAt() != null) {
            onBreak += Duration.between(session.getBreakStartedAt(), now).getSeconds();
        }

        return Math.max(0, wallClock - paused - onBreak);
    }

    private PomodoroStateResponse toState(PomodoroSession session) {

        Instant now = Instant.now();

        long elapsedFocus = elapsedFocusSeconds(session, now);
        long targetSeconds = session.getFocusMinutes() * 60L;
        long remaining = Math.max(0, targetSeconds - elapsedFocus);

        long currentBreak = 0;
        if (session.getStatus() == PomodoroStatus.ON_BREAK && session.getBreakStartedAt() != null) {
            currentBreak = Duration.between(session.getBreakStartedAt(), now).getSeconds();
        }

        long totalPaused = session.getAccumulatedPausedSeconds();
        if (session.getStatus() == PomodoroStatus.PAUSED && session.getPausedAt() != null) {
            totalPaused += Duration.between(session.getPausedAt(), now).getSeconds();
        }

        return new PomodoroStateResponse(
                session.getId(),
                session.getSubject(),
                session.getStatus().name(),
                session.getFocusMinutes(),
                session.getShortBreakMinutes(),
                session.getLongBreakMinutes(),
                elapsedFocus,
                remaining,
                currentBreak,
                totalPaused,
                session.getAccumulatedBreakSeconds() + currentBreak,
                session.getBreaksTaken(),
                session.getInterruptions()
        );
    }
}
