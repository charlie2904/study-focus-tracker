package com.focusassistant.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class PomodoroStartRequest {

    @NotBlank(message = "Subject cannot be empty")
    private String subject;

    @Min(value = 1, message = "Focus minutes must be at least 1")
    @Max(value = 180, message = "Focus minutes cannot exceed 180")
    private Integer focusMinutes;

    @Min(value = 1, message = "Short break must be at least 1 minute")
    @Max(value = 60, message = "Short break cannot exceed 60 minutes")
    private Integer shortBreakMinutes;

    @Min(value = 1, message = "Long break must be at least 1 minute")
    @Max(value = 120, message = "Long break cannot exceed 120 minutes")
    private Integer longBreakMinutes;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Integer getFocusMinutes() { return focusMinutes; }
    public void setFocusMinutes(Integer focusMinutes) { this.focusMinutes = focusMinutes; }

    public Integer getShortBreakMinutes() { return shortBreakMinutes; }
    public void setShortBreakMinutes(Integer shortBreakMinutes) { this.shortBreakMinutes = shortBreakMinutes; }

    public Integer getLongBreakMinutes() { return longBreakMinutes; }
    public void setLongBreakMinutes(Integer longBreakMinutes) { this.longBreakMinutes = longBreakMinutes; }
}