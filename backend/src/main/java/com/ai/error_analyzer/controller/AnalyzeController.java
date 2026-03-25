package com.ai.error_analyzer.controller;

import com.ai.error_analyzer.dto.AnalyzeRequest;
import com.ai.error_analyzer.dto.AnalyzeResponse;
import com.ai.error_analyzer.service.ErrorAnalyzerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final ErrorAnalyzerService errorAnalyzerService;

    public AnalyzeController(ErrorAnalyzerService errorAnalyzerService) {
        this.errorAnalyzerService = errorAnalyzerService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@Valid @RequestBody AnalyzeRequest request) {
        AnalyzeResponse response = errorAnalyzerService.analyze(request.log().trim());
        return ResponseEntity.ok(response);
    }
}
