package com.ai.error_analyzer;

import com.ai.error_analyzer.service.ErrorAnalyzerService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ErrorAnalyzerApplicationTests {

    @MockBean
    ErrorAnalyzerService errorAnalyzerService;

    @Test
    void contextLoads() {
    }
}
