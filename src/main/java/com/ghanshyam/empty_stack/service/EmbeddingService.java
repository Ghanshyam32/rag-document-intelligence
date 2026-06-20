package com.ghanshyam.empty_stack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class EmbeddingService {

    private static final String MODEL = "models/gemini-embedding-001";
    private static final String EMBED_URL =
            "https://generativelanguage.googleapis.com/v1beta/" + MODEL + ":embedContent";

    private final RestClient restClient;
    private final String apiKey;

    public EmbeddingService(@Value("${spring.ai.google.genai.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    public float[] embedDocument(String text) {
        return callEmbeddingApi(text, "RETRIEVAL_DOCUMENT");
    }

    public float[] embedQuery(String text) {
        return callEmbeddingApi(text, "RETRIEVAL_QUERY");
    }

    public List<float[]> embedAll(List<String> texts) {
        return texts.stream().map(this::embedDocument).toList();
    }

    private float[] callEmbeddingApi(String text, String taskType) {
        var request = new EmbedRequest(
                new EmbedRequest.Content(List.of(new EmbedRequest.Part(text))),
                taskType
        );
        EmbedResponse response = restClient.post()
                .uri(EMBED_URL + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmbedResponse.class);
        return response.embedding().values();
    }

    private record EmbedRequest(Content content, String taskType) {
        private record Content(List<Part> parts) {}
        private record Part(String text) {}
    }

    private record EmbedResponse(Embedding embedding) {
        private record Embedding(float[] values) {}
    }
}
