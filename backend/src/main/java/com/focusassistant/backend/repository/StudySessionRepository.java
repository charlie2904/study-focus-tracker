package com.focusassistant.backend.repository;

import com.focusassistant.backend.model.StudySession;
import com.focusassistant.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByUser(User user);
    long countByUser(User user);
}