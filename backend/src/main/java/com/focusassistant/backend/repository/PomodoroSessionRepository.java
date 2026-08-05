package com.focusassistant.backend.repository;

import com.focusassistant.backend.model.PomodoroSession;
import com.focusassistant.backend.model.PomodoroStatus;
import com.focusassistant.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PomodoroSessionRepository extends JpaRepository<PomodoroSession, Long> {

    Optional<PomodoroSession> findByUserAndStatusIn(User user, List<PomodoroStatus> statuses);
}