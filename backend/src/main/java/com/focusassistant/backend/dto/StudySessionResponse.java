package com.focusassistant.backend.dto;

import java.time.LocalDate;

public class StudySessionResponse {

    private Long id;
    private String subject;
    private int duration;
    private int plannedDuration;
    private int focusRating;
    private double focusScore;
    private LocalDate sessionDate;
    private String username;

    public StudySessionResponse(Long id,
                                String subject,
                                int duration,
                                int plannedDuration,
                                int focusRating,
                                double focusScore,
                                LocalDate sessionDate,
                                String username) {

        this.id = id;
        this.subject = subject;
        this.duration = duration;
        this.plannedDuration = plannedDuration;
        this.focusRating = focusRating;
        this.focusScore = focusScore;
        this.sessionDate = sessionDate;
        this.username = username;
    }

    // getters only (no setters needed for response DTO)
    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public int getDuration() { return duration; }
    public int getPlannedDuration() { return plannedDuration; }
    public int getFocusRating() { return focusRating; }
    public double getFocusScore() { return focusScore; }
    public LocalDate getSessionDate() { return sessionDate; }
    public String getUsername() { return username; }
}
