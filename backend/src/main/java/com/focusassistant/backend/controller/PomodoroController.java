package com.focusassistant.backend.controller;

import com.focusassistant.backend.dto.PomodoroCompleteRequest;
import com.focusassistant.backend.dto.PomodoroStartRequest;
import com.focusassistant.backend.dto.PomodoroStateResponse;
import com.focusassistant.backend.dto.StudySessionResponse;
import com.focusassistant.backend.model.StudySession;
import com.focusassistant.backend.model.User;
import com.focusassistant.backend.repository.UserRepository;
import com.focusassistant.backend.security.JwtService;
import com.focusassistant.backend.service.PomodoroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/pomodoro")
@CrossOrigin
public class PomodoroController {

    private final PomodoroService pomodoroService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public PomodoroController(PomodoroService pomodoroService,
                              JwtService jwtService,
                              UserRepository userRepository) {
        this.pomodoroService = pomodoroService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    private User currentUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or malformed token");
        }
        String username = jwtService.extractUsername(authHeader.substring(7));
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public PomodoroStateResponse start(@Valid @RequestBody PomodoroStartRequest request,
                                       @RequestHeader("Authorization") String authHeader) {
        return pomodoroService.start(currentUser(authHeader), request);
    }

    @GetMapping("/active")
    public PomodoroStateResponse active(@RequestHeader("Authorization") String authHeader) {
        return pomodoroService.getActive(currentUser(authHeader));
    }

    @PostMapping("/pause")
    public PomodoroStateResponse pause(@RequestHeader("Authorization") String authHeader) {
        return pomodoroService.pause(currentUser(authHeader));
    }

    @PostMapping("/resume")
    public PomodoroStateResponse resume(@RequestHeader("Authorization") String authHeader) {
        return pomodoroService.resume(currentUser(authHeader));
    }

    @PostMapping("/break")
    public PomodoroStateResponse startBreak(@RequestHeader("Authorization") String authHeader) {
        return pomodoroService.startBreak(currentUser(authHeader));
    }

    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.CREATED)
    public StudySessionResponse complete(@Valid @RequestBody PomodoroCompleteRequest request,
                                         @RequestHeader("Authorization") String authHeader) {

        User user = currentUser(authHeader);
        StudySession saved = pomodoroService.complete(user, request);

        return new StudySessionResponse(
                saved.getId(),
                saved.getSubject(),
                saved.getDuration(),
                saved.getPlannedDuration(),
                saved.getFocusRating(),
                saved.getFocusScore(),
                saved.getSessionDate(),
                saved.getStartTime(),
                saved.getBreaksTaken(),
                saved.getInterruptions(),
                saved.getNotes(),
                user.getUsername()
        );
    }

    @PostMapping("/abandon")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abandon(@RequestHeader("Authorization") String authHeader) {
        pomodoroService.abandon(currentUser(authHeader));
    }
}