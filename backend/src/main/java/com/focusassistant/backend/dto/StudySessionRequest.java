package com.focusassistant.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public class StudySessionRequest {

    @NotBlank(message = "Subject cannot be empty")
    private String subject;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer duration;

    @NotNull(message = "Planned duration is required")
    @Min(value = 1, message = "Planned duration must be at least 1 minute")
    private Integer plannedDuration;

    @NotNull(message = "Focus rating is required")
    @Min(value = 1, message = "Focus rating must be between 1 and 5")
    @Max(value = 5, message = "Focus rating must be between 1 and 5")
    private Integer focusRating;

    @NotNull(message = "Session date is required")
    @PastOrPresent(message = "Session date cannot be in the future")
    private LocalDate sessionDate;

    // Getters and Setters

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getPlannedDuration() {
        return plannedDuration;
    }

    public void setPlannedDuration(Integer plannedDuration) {
        this.plannedDuration = plannedDuration;
    }

    public Integer getFocusRating() {
        return focusRating;
    }

    public void setFocusRating(Integer focusRating) {
        this.focusRating = focusRating;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }
}