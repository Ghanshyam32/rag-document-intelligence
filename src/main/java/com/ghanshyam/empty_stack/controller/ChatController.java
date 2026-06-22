package com.ghanshyam.empty_stack.controller;

import com.ghanshyam.empty_stack.dto.ChatRequest;
import com.ghanshyam.empty_stack.dto.ChatResponse;
import com.ghanshyam.empty_stack.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.ask(request));
    }

    @DeleteMapping("/history/{conversationId}")
    public ResponseEntity<Void> clearHistory(@PathVariable String conversationId) {
        chatService.clearHistory(conversationId);
        return ResponseEntity.noContent().build();
    }
}