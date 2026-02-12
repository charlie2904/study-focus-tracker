package com.focusassistant.backend.model;

public class StudyAnalytics {

    private int totalSessions;
    private int totalStudyMinutes;
    private String mostStudiedSubject;

    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public int getTotalStudyMinutes() {
        return totalStudyMinutes;
    }

    public void setTotalStudyMinutes(int totalStudyMinutes) {
        this.totalStudyMinutes = totalStudyMinutes;
    }

    public String getMostStudiedSubject() {
        return mostStudiedSubject;
    }

    public void setMostStudiedSubject(String mostStudiedSubject) {
        this.mostStudiedSubject = mostStudiedSubject;
    }
}
