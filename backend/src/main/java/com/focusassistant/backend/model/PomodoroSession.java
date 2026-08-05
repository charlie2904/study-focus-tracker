package com.focusassistant.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "pomodoro_sessions")
public class PomodoroSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;

    /** Configurable durations, in minutes. */
    private int focusMinutes;
    private int shortBreakMinutes;
    private int longBreakMinutes;

    @Enumerated(EnumType.STRING)
    private PomodoroStatus status;

    /** When the session was first started. */
    private Instant startedAt;

    /** Set when paused; null while running. */
    private Instant pausedAt;

    /** Set when a break begins; null otherwise. */
    private Instant breakStartedAt;

    /** Running totals, in seconds. */
    private long accumulatedPausedSeconds;
    private long accumulatedBreakSeconds;

    private int breaksTaken;
    private int interruptions;

    private Instant endedAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public PomodoroSession() {
    }

    public Long getId() { return id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public int getFocusMinutes() { return focusMinutes; }
    public void setFocusMinutes(int focusMinutes) { this.focusMinutes = focusMinutes; }

    public int getShortBreakMinutes() { return shortBreakMinutes; }
    public void setShortBreakMinutes(int shortBreakMinutes) { this.shortBreakMinutes = shortBreakMinutes; }

    public int getLongBreakMinutes() { return longBreakMinutes; }
    public void setLongBreakMinutes(int longBreakMinutes) { this.longBreakMinutes = longBreakMinutes; }

    public PomodoroStatus getStatus() { return status; }
    public void setStatus(PomodoroStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getPausedAt() { return pausedAt; }
    public void setPausedAt(Instant pausedAt) { this.pausedAt = pausedAt; }

    public Instant getBreakStartedAt() { return breakStartedAt; }
    public void setBreakStartedAt(Instant breakStartedAt) { this.breakStartedAt = breakStartedAt; }

    public long getAccumulatedPausedSeconds() { return accumulatedPausedSeconds; }
    public void setAccumulatedPausedSeconds(long accumulatedPausedSeconds) { this.accumulatedPausedSeconds = accumulatedPausedSeconds; }

    public long getAccumulatedBreakSeconds() { return accumulatedBreakSeconds; }
    public void setAccumulatedBreakSeconds(long accumulatedBreakSeconds) { this.accumulatedBreakSeconds = accumulatedBreakSeconds; }

    public int getBreaksTaken() { return breaksTaken; }
    public void setBreaksTaken(int breaksTaken) { this.breaksTaken = breaksTaken; }

    public int getInterruptions() { return interruptions; }
    public void setInterruptions(int interruptions) { this.interruptions = interruptions; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}