package com.focusassistant.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PomodoroCompleteRequest {

    @NotNull(message = "Focus rating is required")
    @Min(value = 1, message = "Focus rating must be between 1 and 5")
    @Max(value = 5, message = "Focus rating must be between 1 and 5")
    private Integer focusRating;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    public Integer getFocusRating() { return focusRating; }
    public void setFocusRating(Integer focusRating) { this.focusRating = focusRating; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}