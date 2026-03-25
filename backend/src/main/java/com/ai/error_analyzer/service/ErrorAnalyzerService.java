package com.ai.error_analyzer.service;

import com.ai.error_analyzer.dto.AnalyzeResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ErrorAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(ErrorAnalyzerService.class);
    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.5;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public ErrorAnalyzerService(VectorStore vectorStore, ChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @CircuitBreaker(name = "llm", fallbackMethod = "analyzeFallback")
    public AnalyzeResponse analyze(String logContent) {
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(logContent)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build()
        );

        String relatedErrors = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        String systemPrompt = """
                You are an expert error analyst. Analyze the provided error log and suggest possible causes and solutions.
                If similar previous errors or knowledge base context is provided, use it to inform your analysis.
                Be concise and actionable. Format your response with clear sections: Summary, Possible Causes, Recommended Actions.
                """;

        String analysis = chatClient.prompt()
                .system(systemPrompt)
                .user("""
                        Error log to analyze:
                        %s

                        Similar previous errors / knowledge base context:
                        %s
                        """.formatted(logContent, relatedErrors.isEmpty() ? "None found." : relatedErrors))
                .call()
                .content();

        return new AnalyzeResponse(analysis, relatedErrors);
    }

    public AnalyzeResponse analyzeFallback(String logContent, Throwable ex) {
        log.error("LLM call failed", ex);
        log.warn("Returning circuit-breaker fallback. Cause: {}", ex.getMessage());
        return new AnalyzeResponse(
                "Error analysis is temporarily unavailable due to LLM service timeout or failure. Please try again later.",
                ""
        );
    }
}
