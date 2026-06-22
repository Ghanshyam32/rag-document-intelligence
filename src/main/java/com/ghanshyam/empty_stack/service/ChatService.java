package com.ghanshyam.empty_stack.service;

import com.ghanshyam.empty_stack.dto.ChatRequest;
import com.ghanshyam.empty_stack.dto.ChatResponse;
import com.ghanshyam.empty_stack.dto.ChunkSearchResult;
import com.ghanshyam.empty_stack.model.DocumentChunk;
import com.ghanshyam.empty_stack.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;
    private final ChatModel chatModel;

    @Value("${app.rag.top-k:5}")
    private int topK;

    // In-memory conversation history: conversationId -> last 5 exchanges
    private final Map<String, List<String>> conversationHistory = new ConcurrentHashMap<>();

    public ChatResponse ask(ChatRequest request) {
        // Step 1: Generate conversationId if not provided
        String convId = request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        int k = request.getTopK() != null ? request.getTopK() : topK;

        // Step 2: Embed the user's question
        float[] queryEmbedding = embeddingService.embedQuery(request.getQuestion());

        // Step 3: Find similar chunks from DB
        String vectorStr = toVectorString(queryEmbedding);
        List<ChunkSearchResult> similarChunks = chunkRepository.findSimilarChunks(vectorStr, k);

        // Step 4: Build context string from chunks
        String context = similarChunks.stream()
                .map(c -> "[" + c.getDocumentName() + "]: " + c.getContent())
                .collect(Collectors.joining("\n\n"));

        // Step 5: Get conversation history
        List<String> history = conversationHistory.getOrDefault(convId, List.of());
        String historyStr = String.join("\n", history);

        // Step 6: Build prompt
        String systemPrompt = """
                You are a helpful assistant that answers questions based only on the provided context.
                If the answer is not in the context, say "I don't have enough information to answer this."
                Always mention which document your answer comes from.
                
                Context:
                """ + context + """
                
                Conversation History:
                """ + historyStr;

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(request.getQuestion())
        ));

        // Step 7: Call Gemini
        String answer = chatModel.call(prompt).getResult().getOutput().getText();

        // Step 8: Update conversation history (keep last 5 exchanges)
        List<String> updatedHistory = new java.util.ArrayList<>(history);
        updatedHistory.add("User: " + request.getQuestion());
        updatedHistory.add("Assistant: " + answer);
        if (updatedHistory.size() > 10) {
            updatedHistory = updatedHistory.subList(updatedHistory.size() - 10, updatedHistory.size());
        }
        conversationHistory.put(convId, updatedHistory);

        // Step 9: Build response with sources
        List<ChatResponse.Source> sources = similarChunks.stream().map(c -> {
            ChatResponse.Source src = new ChatResponse.Source();
            src.setDocumentName(c.getDocumentName());
            src.setChunkIndex(c.getChunkIndex());
            src.setExcerpt(c.getContent().substring(0, Math.min(150, c.getContent().length())));
            return src;
        }).toList();

        ChatResponse response = new ChatResponse();
        response.setAnswer(answer);
        response.setConversationId(convId);
        response.setSources(sources);
        return response;
    }

    public void clearHistory(String conversationId) {
        conversationHistory.remove(conversationId);
    }

    // Convert float[] to pgvector string format: [0.1, 0.2, ...]
    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}