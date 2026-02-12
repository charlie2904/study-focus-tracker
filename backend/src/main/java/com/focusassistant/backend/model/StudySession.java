package com.focusassistant.backend.model;

public class StudySession {

    private int id;
    private String subject;
    private int duration; // minutes
    private String sessionDate;

    public StudySession() {
    }

    public StudySession(String subject, int duration, String sessionDate) {
        this.subject = subject;
        this.duration = duration;
        this.sessionDate = sessionDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(String sessionDate) {
        this.sessionDate = sessionDate;
    }
}
