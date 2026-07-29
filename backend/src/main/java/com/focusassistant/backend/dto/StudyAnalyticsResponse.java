package com.focusassistant.backend.dto;

public class StudyAnalyticsResponse {

    private long totalSessions;
    private int totalMinutes;
    private double averageFocusScore;
    private String bestDay;

    public StudyAnalyticsResponse(long totalSessions,
                                  int totalMinutes,
                                  double averageFocusScore,
                                  String bestDay) {
        this.totalSessions = totalSessions;
        this.totalMinutes = totalMinutes;
        this.averageFocusScore = averageFocusScore;
        this.bestDay = bestDay;
    }

    public long getTotalSessions() {
        return totalSessions;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public double getAverageFocusScore() {
        return averageFocusScore;
    }

    public String getBestDay() {
        return bestDay;
    }
}
