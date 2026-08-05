package com.focusassistant.backend.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "study_sessions")
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;
    private int duration;
    private int plannedDuration;
    private int focusRating;
    private double focusScore;

    private LocalDate sessionDate;

    // ===== New fields =====

    private LocalTime startTime;

    private Integer breaksTaken;

    private Integer interruptions;

    @Column(length = 500)
    private String notes;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public StudySession() {
    }

    public Long getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public int getPlannedDuration() {
        return plannedDuration;
    }

    public void setPlannedDuration(int plannedDuration) {
        this.plannedDuration = plannedDuration;
    }

    public int getFocusRating() {
        return focusRating;
    }

    public void setFocusRating(int focusRating) {
        this.focusRating = focusRating;
    }

    public double getFocusScore() {
        return focusScore;
    }

    public void setFocusScore(double focusScore) {
        this.focusScore = focusScore;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public Integer getBreaksTaken() {
        return breaksTaken;
    }

    public void setBreaksTaken(Integer breaksTaken) {
        this.breaksTaken = breaksTaken;
    }

    public Integer getInterruptions() {
        return interruptions;
    }

    public void setInterruptions(Integer interruptions) {
        this.interruptions = interruptions;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}