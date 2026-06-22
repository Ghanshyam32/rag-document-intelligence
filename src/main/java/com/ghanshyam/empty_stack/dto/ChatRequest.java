package com.ghanshyam.empty_stack.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private String conversationId;
    private Integer topK = 5;
}