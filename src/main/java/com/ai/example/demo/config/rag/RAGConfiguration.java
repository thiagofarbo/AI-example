package com.ai.example.demo.config.rag;

import com.ai.example.demo.domain.Assistant;
import com.ai.example.demo.service.DataEmbededService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

@Configuration
public class RAGConfiguration {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Autowired
    private Environment env;

    public Assistant configure() throws Exception {

        List<Document> docs = FileSystemDocumentLoader.loadDocuments(DataEmbededService.getPath("documents/"), DataEmbededService.glob("*.txt"));
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(OpenAiChatModel.withApiKey(apiKey))
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(createContenteRetriever(docs))
                .build();
        return assistant;
    }

    public static ContentRetriever createContenteRetriever(List<Document> documents){
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);
        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }
}
