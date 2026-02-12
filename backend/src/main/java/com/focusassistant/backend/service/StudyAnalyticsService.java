package com.focusassistant.backend.service;

import com.focusassistant.backend.model.StudyAnalytics;
import com.focusassistant.backend.util.DBUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class StudyAnalyticsService {

    public StudyAnalytics getAnalytics() {

        StudyAnalytics analytics = new StudyAnalytics();

        try (
                Connection con = DBUtil.getConnection();
                Statement st = con.createStatement();
        ) {

            // total sessions
            ResultSet rs1 = st.executeQuery("SELECT COUNT(*) FROM study_sessions");
            if (rs1.next()) {
                analytics.setTotalSessions(rs1.getInt(1));
            }

            // total study minutes
            ResultSet rs2 = st.executeQuery("SELECT SUM(duration) FROM study_sessions");
            if (rs2.next()) {
                analytics.setTotalStudyMinutes(rs2.getInt(1));
            }

            // most studied subject
            ResultSet rs3 = st.executeQuery(
                    "SELECT subject FROM study_sessions GROUP BY subject ORDER BY COUNT(*) DESC LIMIT 1"
            );
            if (rs3.next()) {
                analytics.setMostStudiedSubject(rs3.getString(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return analytics;
    }
}
