package com.ghanshyam.empty_stack.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    // Convert a single text string → float array (the vector)
    public float[] embed(String text) {
        EmbeddingRequest request = new EmbeddingRequest(
                List.of(text),
                GoogleGenAiTextEmbeddingOptions.builder()
                        .model("text-embedding-004")
                        .build()
        );
        EmbeddingResponse response = embeddingModel.call(request);
        return response.getResult().getOutput();
    }

    // Convert multiple texts at once (used during document upload)
    public List<float[]> embedAll(List<String> texts) {
        return texts.stream()
                .map(this::embed)
                .toList();
    }
}