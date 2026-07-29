package com.focusassistant.backend.controller;

import com.focusassistant.backend.dto.StudyAnalyticsResponse;
import com.focusassistant.backend.service.StudyAnalyticsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin
public class StudyAnalyticsController {

    private final StudyAnalyticsService analyticsService;

    public StudyAnalyticsController(StudyAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public StudyAnalyticsResponse getSummary() {
        return analyticsService.getSummary();
    }
}
