package com.focusassistant.backend.service;

import com.focusassistant.backend.dto.PomodoroCompleteRequest;
import com.focusassistant.backend.dto.PomodoroStartRequest;
import com.focusassistant.backend.dto.PomodoroStateResponse;
import com.focusassistant.backend.model.*;
import com.focusassistant.backend.repository.PomodoroSessionRepository;
import com.focusassistant.backend.repository.StudySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PomodoroService")
class PomodoroServiceTest {

    @Mock
    private PomodoroSessionRepository pomodoroRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @InjectMocks
    private PomodoroService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("rishabh");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    /** Builds a session in an arbitrary state, started {@code minutesAgo} minutes back. */
    private PomodoroSession sessionStartedMinutesAgo(int minutesAgo, PomodoroStatus status) {
        PomodoroSession s = new PomodoroSession();
        s.setSubject("DSA");
        s.setFocusMinutes(25);
        s.setShortBreakMinutes(5);
        s.setLongBreakMinutes(15);
        s.setStatus(status);
        s.setStartedAt(Instant.now().minus(minutesAgo, ChronoUnit.MINUTES));
        s.setUser(user);
        return s;
    }

    private void noActiveSession() {
        when(pomodoroRepository.findByUserAndStatusIn(eq(user), anyList()))
                .thenReturn(Optional.empty());
    }

    private void activeSession(PomodoroSession s) {
        when(pomodoroRepository.findByUserAndStatusIn(eq(user), anyList()))
                .thenReturn(Optional.of(s));
    }

    // ==========================================================
    @Nested
    @DisplayName("start()")
    class Start {

        @Test
        @DisplayName("applies 25/5/15 defaults when durations are omitted")
        void appliesDefaults() {
            noActiveSession();
            when(pomodoroRepository.save(any(PomodoroSession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PomodoroStartRequest request = new PomodoroStartRequest();
            request.setSubject("DSA");

            PomodoroStateResponse state = service.start(user, request);

            assertThat(state.getFocusMinutes()).isEqualTo(25);
            assertThat(state.getShortBreakMinutes()).isEqualTo(5);
            assertThat(state.getLongBreakMinutes()).isEqualTo(15);
            assertThat(state.getStatus()).isEqualTo("RUNNING");
            assertThat(state.getRemainingFocusSeconds()).isEqualTo(25 * 60);
        }

        @Test
        @DisplayName("honours custom durations when supplied")
        void honoursCustomDurations() {
            noActiveSession();
            when(pomodoroRepository.save(any(PomodoroSession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PomodoroStartRequest request = new PomodoroStartRequest();
            request.setSubject("DBMS");
            request.setFocusMinutes(45);

            PomodoroStateResponse state = service.start(user, request);

            assertThat(state.getFocusMinutes()).isEqualTo(45);
            assertThat(state.getShortBreakMinutes()).isEqualTo(5);   // still default
        }

        @Test
        @DisplayName("rejects a second session with 409 while one is active")
        void rejectsConcurrentSession() {
            activeSession(sessionStartedMinutesAgo(3, PomodoroStatus.RUNNING));

            PomodoroStartRequest request = new PomodoroStartRequest();
            request.setSubject("OS");

            assertThatThrownBy(() -> service.start(user, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("statusCode", HttpStatus.CONFLICT);

            verify(pomodoroRepository, never()).save(any());
        }
    }

    // ==========================================================
    @Nested
    @DisplayName("pause()")
    class Pause {

        @Test
        @DisplayName("sets PAUSED and increments the interruption count")
        void pausesAndCountsInterruption() {
            PomodoroSession s = sessionStartedMinutesAgo(5, PomodoroStatus.RUNNING);
            activeSession(s);
            when(pomodoroRepository.save(any(PomodoroSession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PomodoroStateResponse state = service.pause(user);

            assertThat(state.getStatus()).isEqualTo("PAUSED");
            assertThat(state.getInterruptions()).isEqualTo(1);
            assertThat(s.getPausedAt()).isNotNull();
        }

        @Test
        @DisplayName("rejects pausing a session that is already on a break")
        void rejectsPauseWhileOnBreak() {
            PomodoroSession s = sessionStartedMinutesAgo(5, PomodoroStatus.ON_BREAK);
            s.setBreakStartedAt(Instant.now());
            activeSession(s);

            assertThatThrownBy(() -> service.pause(user))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("statusCode", HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("returns 404 when there is no active session")
        void rejectsPauseWithNoSession() {
            noActiveSession();

            assertThatThrownBy(() -> service.pause(user))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
        }
    }

    // ==========================================================
    @Nested
    @DisplayName("resume()")
    class Resume {

        @Test
        @DisplayName("banks the paused duration and returns to RUNNING")
        void banksPausedTime() {
            PomodoroSession s = sessionStartedMinutesAgo(10, PomodoroStatus.PAUSED);
            s.setPausedAt(Instant.now().minus(4, ChronoUnit.MINUTES));
            activeSession(s);
            when(pomodoroRepository.save(any(PomodoroSession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PomodoroStateResponse state = service.resume(user);

            assertThat(state.getStatus()).isEqualTo("RUNNING");
            assertThat(s.getPausedAt()).isNull();
            assertThat(s.getAccumulatedPausedSeconds()).isBetween(235L, 245L); // ~4 min
        }

        @Test
        @DisplayName("banks the break duration when resuming from a break")
        void banksBreakTime() {
            PomodoroSession s = sessionStartedMinutesAgo(10, PomodoroStatus.ON_BREAK);
            s.setBreakStartedAt(Instant.now().minus(3, ChronoUnit.MINUTES));
            activeSession(s);
            when(pomodoroRepository.save(any(PomodoroSession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PomodoroStateResponse state = service.resume(user);

            assertThat(state.getStatus()).isEqualTo("RUNNING");
            assertThat(s.getBreakStartedAt()).isNull();
            assertThat(s.getAccumulatedBreakSeconds()).isBetween(175L, 185L); // ~3 min
        }

        @Test
        @DisplayName("rejects resuming a session that is already running")
        void rejectsResumeWhileRunning() {
            activeSession(sessionStartedMinutesAgo(5, PomodoroStatus.RUNNING));

            assertThatThrownBy(() -> service.resume(user))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("statusCode", HttpStatus.CONFLICT);
        }
    }

    // ==========================================================
    @Nested
    @DisplayName("startBreak()")
    class Break {

        @Test
        @DisplayName("sets ON_BREAK and increments the break count")
        void startsBreak() {
            PomodoroSession s = sessionStartedMinutesAgo(12, PomodoroStatus.RUNNING);
            activeSession(s);
            when(pomodoroRepository.save(any(PomodoroSession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PomodoroStateResponse state = service.startBreak(user);

            assertThat(state.getStatus()).isEqualTo("ON_BREAK");
            assertThat(state.getBreaksTaken()).isEqualTo(1);
            assertThat(s.getBreakStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("rejects starting a break from a paused session")
        void rejectsBreakWhilePaused() {
            PomodoroSession s = sessionStartedMinutesAgo(12, PomodoroStatus.PAUSED);
            s.setPausedAt(Instant.now());
            activeSession(s);

            assertThatThrownBy(() -> service.startBreak(user))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("statusCode", HttpStatus.CONFLICT);
        }
    }

    // ==========================================================
    @Nested
    @DisplayName("elapsed focus time")
    class ElapsedTime {

        @Test
        @DisplayName("excludes paused and break time from the focus total")
        void excludesPausesAndBreaks() {
            // Started 30 minutes ago, of which 5 were paused and 5 on break.
            PomodoroSession s = sessionStartedMinutesAgo(30, PomodoroStatus.RUNNING);
            s.setAccumulatedPausedSeconds(5 * 60);
            s.setAccumulatedBreakSeconds(5 * 60);
            activeSession(s);

            PomodoroStateResponse state = service.getActive(user);

            // 30 - 5 - 5 = 20 minutes of actual focus
            assertThat(state.getElapsedFocusSeconds()).isBetween(1195L, 1205L);
        }

        @Test
        @DisplayName("never reports negative remaining time once the target is passed")
        void clampsRemainingAtZero() {
            PomodoroSession s = sessionStartedMinutesAgo(40, PomodoroStatus.RUNNING); // target is 25
            activeSession(s);

            PomodoroStateResponse state = service.getActive(user);

            assertThat(state.getRemainingFocusSeconds()).isZero();
        }

        @Test
        @DisplayName("keeps counting paused time while the session is paused")
        void countsOngoingPause() {
            PomodoroSession s = sessionStartedMinutesAgo(20, PomodoroStatus.PAUSED);
            s.setPausedAt(Instant.now().minus(6, ChronoUnit.MINUTES));
            activeSession(s);

            PomodoroStateResponse state = service.getActive(user);

            assertThat(state.getTotalPausedSeconds()).isBetween(355L, 365L);   // ~6 min
            assertThat(state.getElapsedFocusSeconds()).isBetween(835L, 845L);  // ~14 min
        }
    }

    // ==========================================================
    @Nested
    @DisplayName("complete()")
    class Complete {

        @Test
        @DisplayName("converts the Pomodoro into a persisted StudySession")
        void writesStudySession() {
            PomodoroSession s = sessionStartedMinutesAgo(25, PomodoroStatus.RUNNING);
            s.setBreaksTaken(2);
            s.setInterruptions(1);
            activeSession(s);
            when(studySessionRepository.save(any(StudySession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PomodoroCompleteRequest request = new PomodoroCompleteRequest();
            request.setFocusRating(4);
            request.setNotes("Graph algorithms");

            StudySession saved = service.complete(user, request);

            ArgumentCaptor<StudySession> captor = ArgumentCaptor.forClass(StudySession.class);
            verify(studySessionRepository).save(captor.capture());
            StudySession written = captor.getValue();

            assertThat(written.getSubject()).isEqualTo("DSA");
            assertThat(written.getPlannedDuration()).isEqualTo(25);
            assertThat(written.getFocusRating()).isEqualTo(4);
            assertThat(written.getBreaksTaken()).isEqualTo(2);
            assertThat(written.getInterruptions()).isEqualTo(1);
            assertThat(written.getNotes()).isEqualTo("Graph algorithms");
            assertThat(written.getUser()).isEqualTo(user);
            assertThat(written.getStartTime()).isNotNull();
            assertThat(saved).isSameAs(written);
        }

        @Test
        @DisplayName("marks the Pomodoro COMPLETED and stamps endedAt")
        void closesThePomodoro() {
            PomodoroSession s = sessionStartedMinutesAgo(25, PomodoroStatus.RUNNING);
            activeSession(s);
            when(studySessionRepository.save(any(StudySession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PomodoroCompleteRequest request = new PomodoroCompleteRequest();
            request.setFocusRating(3);

            service.complete(user, request);

            assertThat(s.getStatus()).isEqualTo(PomodoroStatus.COMPLETED);
            assertThat(s.getEndedAt()).isNotNull();
            verify(pomodoroRepository).save(s);
        }

        @Test
        @DisplayName("scores a full session at rating x 20")
        void scoresFullSession() {
            PomodoroSession s = sessionStartedMinutesAgo(25, PomodoroStatus.RUNNING);
            activeSession(s);
            when(studySessionRepository.save(any(StudySession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PomodoroCompleteRequest request = new PomodoroCompleteRequest();
            request.setFocusRating(4);

            StudySession saved = service.complete(user, request);

            // 25 of 25 minutes focused, rating 4  ->  (25/25) * 4 * 20 = 80
            assertThat(saved.getFocusScore()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("scores a half-length session proportionally lower")
        void scoresPartialSession() {
            PomodoroSession s = sessionStartedMinutesAgo(25, PomodoroStatus.RUNNING);
            s.setAccumulatedBreakSeconds(15 * 60);   // only 10 minutes of real focus
            activeSession(s);
            when(studySessionRepository.save(any(StudySession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PomodoroCompleteRequest request = new PomodoroCompleteRequest();
            request.setFocusRating(5);

            StudySession saved = service.complete(user, request);

            // 10 of 25 minutes, rating 5  ->  (10/25) * 5 * 20 = 40
            assertThat(saved.getFocusScore()).isEqualTo(40.0);
        }

        @Test
        @DisplayName("banks an open break before converting")
        void banksOpenBreakOnComplete() {
            PomodoroSession s = sessionStartedMinutesAgo(20, PomodoroStatus.ON_BREAK);
            s.setBreakStartedAt(Instant.now().minus(4, ChronoUnit.MINUTES));
            activeSession(s);
            when(studySessionRepository.save(any(StudySession.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PomodoroCompleteRequest request = new PomodoroCompleteRequest();
            request.setFocusRating(3);

            service.complete(user, request);

            assertThat(s.getBreakStartedAt()).isNull();
            assertThat(s.getAccumulatedBreakSeconds()).isBetween(235L, 245L);
        }
    }

    // ==========================================================
    @Nested
    @DisplayName("abandon()")
    class Abandon {

        @Test
        @DisplayName("marks the session ABANDONED without writing a StudySession")
        void abandonsWithoutSaving() {
            PomodoroSession s = sessionStartedMinutesAgo(8, PomodoroStatus.RUNNING);
            activeSession(s);

            service.abandon(user);

            assertThat(s.getStatus()).isEqualTo(PomodoroStatus.ABANDONED);
            assertThat(s.getEndedAt()).isNotNull();
            verify(pomodoroRepository).save(s);
            verifyNoInteractions(studySessionRepository);
        }
    }
}