package com.ai.error_analyzer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
@ConditionalOnBean(VectorStore.class)
public class DocumentIngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final VectorStore vectorStore;
    private final String knowledgeBasePath;

    public DocumentIngestionService(VectorStore vectorStore,
                                   @Value("${app.knowledge-base.path:docs/knowledge-base.md}") String knowledgeBasePath) {
        this.vectorStore = vectorStore;
        this.knowledgeBasePath = knowledgeBasePath;
    }

    @Override
    public void run(ApplicationArguments args) {
        Resource resource = new FileSystemResource(Path.of(knowledgeBasePath));
        if (!resource.exists()) {
            log.warn("Knowledge base file not found at {}. Skipping document ingestion.", knowledgeBasePath);
            return;
        }

        try {
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(true)
                    .withIncludeBlockquote(true)
                    .withAdditionalMetadata("source", "knowledge-base")
                    .build();

            MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
            List<Document> documents = reader.get();

            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(500)
                    .withMinChunkSizeChars(100)
                    .build();
            List<Document> splitDocuments = splitter.apply(documents);

            vectorStore.write(splitDocuments);
            log.info("Ingested {} document chunks from knowledge base into vector store.", splitDocuments.size());
        } catch (Exception e) {
            log.error("Failed to ingest knowledge base from {}", knowledgeBasePath, e);
        }
    }
}
