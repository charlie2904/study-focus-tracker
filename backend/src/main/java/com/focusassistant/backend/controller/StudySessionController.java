package com.focusassistant.backend.controller;

import com.focusassistant.backend.dao.StudySessionDao;
import com.focusassistant.backend.model.StudySession;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StudySessionController {

    private StudySessionDao sessionDao = new StudySessionDao();

    @PostMapping("/sessions")
    public String saveSession(@RequestBody StudySession session) {
        boolean saved = sessionDao.saveSession(session);

        if (saved) {
            return "Study session saved successfully";
        } else {
            return "Failed to save study session";
        }
    }

    @GetMapping("/sessions")
    public List<StudySession> getAllSessions() {
        return sessionDao.getAllSessions();

    }
}
