package com.focusassistant.backend.service;

import com.focusassistant.backend.dto.StudySessionRequest;
import com.focusassistant.backend.model.StudySession;
import com.focusassistant.backend.repository.StudySessionRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import com.focusassistant.backend.dto.StudyAnalyticsResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import java.util.List;

@Service
public class StudyAnalyticsService {

    private final StudySessionRepository repository;

    public StudyAnalyticsService(StudySessionRepository repository) {
        this.repository = repository;
    }

    // ===== SAVE SESSION WITH AI LOGIC =====
    public StudySession saveSessionWithFocus(StudySessionRequest request) {

        StudySession session = new StudySession();
        session.setSubject(request.getSubject());
        session.setDuration(request.getDuration());
        session.setPlannedDuration(request.getPlannedDuration());
        session.setFocusRating(request.getFocusRating());
        session.setSessionDate(request.getSessionDate());

        // Calculate focus score
        double focusScore = 0;
        if (request.getPlannedDuration() != 0) {
            focusScore = ((double) request.getDuration() / request.getPlannedDuration())
                    * request.getFocusRating() * 20;
        }

        session.setFocusScore(focusScore);

        return repository.save(session);
    }
    public StudyAnalyticsResponse getSummary() {

        List<StudySession> sessions = repository.findAll();

        long totalSessions = sessions.size();

        int totalMinutes = sessions.stream()
                .mapToInt(StudySession::getDuration)
                .sum();

        double averageFocusScore = sessions.stream()
                .mapToDouble(StudySession::getFocusScore)
                .average()
                .orElse(0.0);

        // 🔥 Find Best Study Day
        Map<DayOfWeek, Integer> minutesPerDay = new HashMap<>();

        for (StudySession session : sessions) {
            LocalDate date = session.getSessionDate();
            DayOfWeek day = date.getDayOfWeek();

            minutesPerDay.put(day,
                    minutesPerDay.getOrDefault(day, 0) + session.getDuration());
        }

        String bestDay = minutesPerDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().toString())
                .orElse("No Data");

        return new StudyAnalyticsResponse(
                totalSessions,
                totalMinutes,
                averageFocusScore,
                bestDay
        );
    }
    // ===== ANALYTICS METHODS =====
    public long getTotalSessions() {
        return repository.count();
    }

    public int getTotalStudyMinutes() {
        List<StudySession> sessions = repository.findAll();
        return sessions.stream()
                .mapToInt(StudySession::getDuration)
                .sum();
    }
}
