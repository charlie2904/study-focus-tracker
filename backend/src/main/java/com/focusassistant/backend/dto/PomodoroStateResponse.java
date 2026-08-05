package com.focusassistant.backend.dto;

public class PomodoroStateResponse {

    private Long id;
    private String subject;
    private String status;

    private int focusMinutes;
    private int shortBreakMinutes;
    private int longBreakMinutes;

    /** Seconds of actual focus time so far (excludes pauses and breaks). */
    private long elapsedFocusSeconds;

    /** Seconds left in the current focus block; 0 if the target is met. */
    private long remainingFocusSeconds;

    /** Seconds elapsed in the current break; 0 if not on a break. */
    private long currentBreakSeconds;

    private long totalPausedSeconds;
    private long totalBreakSeconds;

    private int breaksTaken;
    private int interruptions;

    public PomodoroStateResponse(Long id,
                                 String subject,
                                 String status,
                                 int focusMinutes,
                                 int shortBreakMinutes,
                                 int longBreakMinutes,
                                 long elapsedFocusSeconds,
                                 long remainingFocusSeconds,
                                 long currentBreakSeconds,
                                 long totalPausedSeconds,
                                 long totalBreakSeconds,
                                 int breaksTaken,
                                 int interruptions) {
        this.id = id;
        this.subject = subject;
        this.status = status;
        this.focusMinutes = focusMinutes;
        this.shortBreakMinutes = shortBreakMinutes;
        this.longBreakMinutes = longBreakMinutes;
        this.elapsedFocusSeconds = elapsedFocusSeconds;
        this.remainingFocusSeconds = remainingFocusSeconds;
        this.currentBreakSeconds = currentBreakSeconds;
        this.totalPausedSeconds = totalPausedSeconds;
        this.totalBreakSeconds = totalBreakSeconds;
        this.breaksTaken = breaksTaken;
        this.interruptions = interruptions;
    }

    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public String getStatus() { return status; }
    public int getFocusMinutes() { return focusMinutes; }
    public int getShortBreakMinutes() { return shortBreakMinutes; }
    public int getLongBreakMinutes() { return longBreakMinutes; }
    public long getElapsedFocusSeconds() { return elapsedFocusSeconds; }
    public long getRemainingFocusSeconds() { return remainingFocusSeconds; }
    public long getCurrentBreakSeconds() { return currentBreakSeconds; }
    public long getTotalPausedSeconds() { return totalPausedSeconds; }
    public long getTotalBreakSeconds() { return totalBreakSeconds; }
    public int getBreaksTaken() { return breaksTaken; }
    public int getInterruptions() { return interruptions; }
}