package com.ai.example.demo.service;


import com.ai.example.demo.domain.Assistant;
import com.ai.example.demo.domain.QDrantConstants;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@Service
public class QDrantService {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String openAIKey;

    public void setupQDrant(){
        createCollection();
        insertDocuments();
    }

    private QdrantClient getQdranteClient(){
        return new QdrantClient(QdrantGrpcClient.newBuilder(
                QDrantConstants.QDRANT_URL,
                QDrantConstants.PORT, true)
                .withApiKey(QDrantConstants.QDRANT_KEY).build());
    }

    private void createCollection(){
        QdrantClient client = getQdranteClient();

        try {

            client.createCollectionAsync(
                    QDrantConstants.COLLECTION_NAME,
                    Collections.VectorParams.newBuilder()
                            .setDistance(Collections.Distance.Cosine)
                            .setSize(QDrantConstants.OPENAI_EMBEDDING_SIZE)
                            .build())
                    .get();

        }catch (Exception ex){
            System.out.println("Error to create collection");
        }
    }

    private EmbeddingModel getEmbeddingModel(){
        return OpenAiEmbeddingModel.withApiKey(openAIKey);
    }

    private EmbeddingStore<TextSegment> getEmbeddingStore(){
        return QdrantEmbeddingStore.builder()
                .collectionName(QDrantConstants.COLLECTION_NAME)
                .host(QDrantConstants.QDRANT_URL)
                .port(QDrantConstants.PORT)
                .apiKey(QDrantConstants.QDRANT_KEY)
                .useTls(true)
                .build();
    }

    private String getFileContent(){

        Resource resource = new ClassPathResource("documents/text.txt");

        try{

            File file = resource.getFile();
            String content = new String(Files.readAllBytes(file.toPath()));
            return content;
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return "";
    }

    private void insertDocuments(){
        EmbeddingModel embeddingModel = getEmbeddingModel();

        DocumentSplitter documentSplitter = DocumentSplitters.recursive(1000,150);

        String fileContent = getFileContent();

        Document doc = Document.from(fileContent, Metadata.from("document-type", "info-document"));

        EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore();

        List<TextSegment>  segments = documentSplitter.split(doc);

        Response<List<Embedding>> embeddingResponse = embeddingModel.embedAll(segments);

        List<Embedding> embeddings = embeddingResponse.content();

        embeddingStore.addAll(embeddings, segments);
    }

    public ContentRetriever getEmbeddingStoreContentRetriever() {

        EmbeddingStore<TextSegment> embeddingStore = QdrantEmbeddingStore.builder()
                .collectionName(QDrantConstants.COLLECTION_NAME)
                .host(QDrantConstants.QDRANT_URL)
                .port(QDrantConstants.PORT)
                .apiKey(QDrantConstants.QDRANT_KEY)
                .useTls(true)
                .build();

        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.withApiKey(openAIKey);

        return EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2) // return maximum two similar results
                .minScore(0.6) // with a degree of precision of 0.6
                .build();
    }

    public Assistant generateAssistant(){
        ChatLanguageModel chatModel = OpenAiChatModel
                .builder()
                .apiKey(openAIKey)
                .modelName("gpt-4o")
                .build();

        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);

        ContentRetriever contentRetriever = getEmbeddingStoreContentRetriever();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        return AiServices.builder(Assistant.class)
                .chatLanguageModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(memory)
                .build();
    }

}
