package com.ai.example.demo.controller;

import com.ai.example.demo.config.rag.RAGConfiguration;
import com.ai.example.demo.domain.Assistant;
import com.ai.example.demo.domain.StructuredTemplate;
import com.ai.example.demo.domain.UserMessage;
import com.ai.example.demo.service.QDrantService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AIController {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    private final ChatLanguageModel chatModel;

    private final RAGConfiguration ragConfiguration;

    private final Environment env;

    private Assistant assistant;

    private final QDrantService qDrantService;

    @PostMapping("/chat")
    public String chatWithOpenAI(@RequestBody UserMessage userRequest) {
        ChatLanguageModel customModel = new OpenAiChatModel.OpenAiChatModelBuilder()
                .apiKey(env.getProperty("langchain4j.open-ai.chat-model.api-key"))
                .temperature(0.5)
                .modelName("gpt-4-turbo")
                .build();
        return customModel.generate(userRequest.message());

    }

    @PostMapping("/imagem")
    public String generateImage(@RequestBody UserMessage userRequest) {
        try {
            ImageModel imageModel = new OpenAiImageModel.OpenAiImageModelBuilder()
                    .apiKey(env.getProperty("langchain4j.open-ai.chat-model.api-key"))
                    .modelName("dall-e")
                    .build();
            return imageModel.generate(userRequest.message()).content().url().toURL().toString();
        } catch (Exception ex) {
            return null;
        }

    }

    @PostMapping("/template")
    public String template(@RequestBody UserMessage userRequest) {

        PromptTemplate template = PromptTemplate.from("Você é um mecânico de veículos. Você deve fornecer respostas para a seguinte pergunta: {{message}}. Não use termos técnicos. Use uma linguagem de fácil compreensão para leigos");

        Map<String, Object> mapa = new HashMap<>();
        mapa.put("message", userRequest.message());
        Prompt prompt = template.apply(mapa);

        ChatLanguageModel customModel = new OpenAiChatModel.OpenAiChatModelBuilder()
                .apiKey(env.getProperty("langchain4j.open-ai.chat-model.api-key"))
                .temperature(0.5)
                .modelName("gpt-4-turbo")
                .build();
        return customModel.generate(prompt.text());

    }

    @PostMapping("/ragchat")
    public String chatWithRag(@RequestBody UserMessage userRequest) {

        try {
            if (assistant == null) {
                assistant = ragConfiguration.configure();
            }
            return assistant.answer(userRequest.message());
        }catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @GetMapping("/receita")
    public String facaUmaReceita() {
        StructuredTemplate template = new StructuredTemplate();
        StructuredTemplate.PromptDeReceita rcPrompt = new StructuredTemplate.PromptDeReceita();
        rcPrompt.prato = "Fried";
        rcPrompt.ingredientes = Arrays.asList("chicken", "tomato", "onion", "pepper");

        Prompt prompt = StructuredPromptProcessor.toPrompt(rcPrompt);

        return chatModel.generate(prompt.text());
    }


    @GetMapping("/setup")
    public String doSetupQdrant(){
        try{

            qDrantService.setupQDrant();
            return "Setup has been completed";

        }catch (Exception e) {
            e.printStackTrace();
            return "Error to setup Qdrant service";
        }
    }

    @PostMapping("/chatwithqdrant")
    public String chatWithQdrant(@RequestBody UserMessage userRequest) {

        Assistant assistant = qDrantService.generateAssistant();

        final String response = assistant.answer(userRequest.message());

        return response;
    }
}
