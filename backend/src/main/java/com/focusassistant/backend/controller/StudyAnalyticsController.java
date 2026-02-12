package com.focusassistant.backend.controller;

import com.focusassistant.backend.model.StudyAnalytics;
import com.focusassistant.backend.service.StudyAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class StudyAnalyticsController {

    private final StudyAnalyticsService analyticsService = new StudyAnalyticsService();

    @GetMapping
    public StudyAnalytics getAnalytics() {
        return analyticsService.getAnalytics();
    }
}

