package com.focusassistant.backend.dao;

import com.focusassistant.backend.model.StudySession;
import com.focusassistant.backend.util.DBUtil;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import java.util.List;
import java.util.ArrayList;

public class StudySessionDao {

    public boolean saveSession(StudySession session) {

        String sql = "INSERT INTO study_sessions (subject, duration, session_date) VALUES (?, ?, ?)";

        try (
                Connection con = DBUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, session.getSubject());
            ps.setInt(2, session.getDuration());
            ps.setString(3, session.getSessionDate());

            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<StudySession> getAllSessions() {

        List<StudySession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM study_sessions";

        try (
                Connection con = DBUtil.getConnection();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)
        ) {

            while (rs.next()) {
                StudySession session = new StudySession();
                session.setId(rs.getInt("id"));
                session.setSubject(rs.getString("subject"));
                session.setDuration(rs.getInt("duration"));
                session.setSessionDate(rs.getString("session_date"));

                sessions.add(session);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sessions;
    }
}
