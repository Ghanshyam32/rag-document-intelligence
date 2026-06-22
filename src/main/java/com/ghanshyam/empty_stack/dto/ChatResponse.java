package com.ghanshyam.empty_stack.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatResponse {
    private String answer;
    private String conversationId;
    private List<Source> sources;

    @Data
    public static class Source {
        private String documentName;
        private Integer chunkIndex;
        private String excerpt;
    }
}