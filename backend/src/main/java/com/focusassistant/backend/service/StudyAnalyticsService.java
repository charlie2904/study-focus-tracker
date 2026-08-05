package com.focusassistant.backend.service;

import com.focusassistant.backend.dto.StudyAnalyticsResponse;
import com.focusassistant.backend.model.StudySession;
import com.focusassistant.backend.model.User;
import com.focusassistant.backend.repository.StudySessionRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudyAnalyticsService {

    private final StudySessionRepository repository;

    public StudyAnalyticsService(StudySessionRepository repository) {
        this.repository = repository;
    }

    public StudyAnalyticsResponse getSummary(User user) {

        List<StudySession> sessions = repository.findByUser(user);

        long totalSessions = sessions.size();

        int totalMinutes = sessions.stream()
                .mapToInt(StudySession::getDuration)
                .sum();

        double averageFocusScore = sessions.stream()
                .mapToDouble(StudySession::getFocusScore)
                .average()
                .orElse(0.0);

        averageFocusScore = Math.round(averageFocusScore * 100.0) / 100.0;

        Map<DayOfWeek, Integer> minutesPerDay = new HashMap<>();

        for (StudySession session : sessions) {
            LocalDate date = session.getSessionDate();
            if (date == null) continue;
            DayOfWeek day = date.getDayOfWeek();
            minutesPerDay.put(day, minutesPerDay.getOrDefault(day, 0) + session.getDuration());
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

    public long getTotalSessions(User user) {
        return repository.countByUser(user);
    }

    public int getTotalStudyMinutes(User user) {
        return repository.findByUser(user).stream()
                .mapToInt(StudySession::getDuration)
                .sum();
    }
}