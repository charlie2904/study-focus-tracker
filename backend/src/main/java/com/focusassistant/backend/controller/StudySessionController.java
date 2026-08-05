package com.focusassistant.backend.controller;

import com.focusassistant.backend.dto.StudySessionRequest;
import com.focusassistant.backend.dto.StudySessionResponse;
import com.focusassistant.backend.model.StudySession;
import com.focusassistant.backend.model.User;
import com.focusassistant.backend.repository.StudySessionRepository;
import com.focusassistant.backend.repository.UserRepository;
import com.focusassistant.backend.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    // ================= HELPER: resolve the logged-in user =================
    private User currentUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or malformed token");
        }
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // ================= HELPER: entity -> DTO =================
    private StudySessionResponse toResponse(StudySession s, String username) {
        return new StudySessionResponse(
                s.getId(),
                s.getSubject(),
                s.getDuration(),
                s.getPlannedDuration(),
                s.getFocusRating(),
                s.getFocusScore(),
                s.getSessionDate(),
                username
        );
    }

    // ================= SAVE SESSION =================
    @PostMapping("/sessions")
    public StudySessionResponse saveSession(
            @Valid @RequestBody StudySessionRequest request,
            @RequestHeader("Authorization") String authHeader) {

        User user = currentUser(authHeader);

        StudySession session = new StudySession();
        session.setSubject(request.getSubject());
        session.setDuration(request.getDuration());
        session.setPlannedDuration(request.getPlannedDuration());
        session.setFocusRating(request.getFocusRating());
        session.setSessionDate(request.getSessionDate());

        // Focus score, guarded against divide-by-zero
        double focusScore = 0.0;
        if (request.getPlannedDuration() > 0) {
            focusScore = ((double) request.getDuration()
                    / request.getPlannedDuration()) * request.getFocusRating() * 20;
        }
        session.setFocusScore(Math.round(focusScore * 100.0) / 100.0);

        session.setUser(user);

        StudySession saved = repository.save(session);

        return toResponse(saved, user.getUsername());
    }

    // ================= GET USER SESSIONS =================
    @GetMapping("/sessions")
    public List<StudySessionResponse> getUserSessions(
            @RequestHeader("Authorization") String authHeader) {

        User user = currentUser(authHeader);

        return repository.findByUser(user)
                .stream()
                .map(s -> toResponse(s, user.getUsername()))
                .toList();
    }

    // ================= DELETE SESSION =================
    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        User user = currentUser(authHeader);

        StudySession session = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        // Ownership check — you can only delete your own sessions
        if (!session.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your session");
        }

        repository.delete(session);
    }
}