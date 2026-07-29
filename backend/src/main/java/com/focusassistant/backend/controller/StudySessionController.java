package com.focusassistant.backend.controller;

import com.focusassistant.backend.dto.StudySessionRequest;
import com.focusassistant.backend.dto.StudySessionResponse;
import com.focusassistant.backend.model.StudySession;
import com.focusassistant.backend.model.User;
import com.focusassistant.backend.repository.StudySessionRepository;
import com.focusassistant.backend.repository.UserRepository;
import com.focusassistant.backend.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class StudySessionController {

    private final StudySessionRepository repository;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public StudySessionController(StudySessionRepository repository,
                                  JwtService jwtService,
                                  UserRepository userRepository) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    // ================= SAVE SESSION =================
    @PostMapping("/sessions")
    public StudySessionResponse saveSession(
            @Valid @RequestBody StudySessionRequest request,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7); // remove "Bearer "
        String username = jwtService.extractUsername(token);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudySession session = new StudySession();
        session.setSubject(request.getSubject());
        session.setDuration(request.getDuration());
        session.setPlannedDuration(request.getPlannedDuration());
        session.setFocusRating(request.getFocusRating());
        session.setSessionDate(request.getSessionDate());

        // ✅ Proper Focus Score Logic
        double focusScore = ((double) request.getDuration()
                / request.getPlannedDuration()) * request.getFocusRating() * 20;

        session.setFocusScore(Math.round(focusScore * 100.0) / 100.0);

        // IMPORTANT: link user
        session.setUser(user);

        StudySession saved = repository.save(session);

        // ✅ Return DTO (NO PASSWORD LEAK)
        return new StudySessionResponse(
                saved.getId(),
                saved.getSubject(),
                saved.getDuration(),
                saved.getPlannedDuration(),
                saved.getFocusRating(),
                saved.getFocusScore(),
                saved.getSessionDate(),
                user.getUsername()
        );
    }

    // ================= GET USER SESSIONS =================
    @GetMapping("/sessions")
    public List<StudySession> getUserSessions(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return repository.findByUser(user);
    }

    // ================= DELETE SESSION =================
    @DeleteMapping("/sessions/{id}")
    public void deleteSession(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        jwtService.extractUsername(token); // just validate token

        repository.deleteById(id);
    }
}
