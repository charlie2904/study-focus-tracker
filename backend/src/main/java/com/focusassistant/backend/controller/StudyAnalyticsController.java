package com.focusassistant.backend.controller;

import com.focusassistant.backend.dto.StudyAnalyticsResponse;
import com.focusassistant.backend.model.User;
import com.focusassistant.backend.repository.UserRepository;
import com.focusassistant.backend.security.JwtService;
import com.focusassistant.backend.service.StudyAnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin
public class StudyAnalyticsController {

    private final StudyAnalyticsService analyticsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public StudyAnalyticsController(StudyAnalyticsService analyticsService,
                                    JwtService jwtService,
                                    UserRepository userRepository) {
        this.analyticsService = analyticsService;
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

    @GetMapping("/summary")
    public StudyAnalyticsResponse getSummary(
            @RequestHeader("Authorization") String authHeader) {
        return analyticsService.getSummary(currentUser(authHeader));
    }
}