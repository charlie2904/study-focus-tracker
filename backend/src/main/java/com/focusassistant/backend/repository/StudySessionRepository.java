package com.focusassistant.backend.repository;

import com.focusassistant.backend.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import com.focusassistant.backend.model.User;
import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByUser(User user);
}
