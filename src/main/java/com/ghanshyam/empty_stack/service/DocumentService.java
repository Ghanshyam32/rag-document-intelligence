package com.ghanshyam.empty_stack.service;

import com.ghanshyam.empty_stack.model.Document;
import com.ghanshyam.empty_stack.model.DocumentChunk;
import com.ghanshyam.empty_stack.model.User;
import com.ghanshyam.empty_stack.repository.DocumentChunkRepository;
import com.ghanshyam.empty_stack.repository.DocumentRepository;
import com.ghanshyam.empty_stack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public Document uploadDocument(MultipartFile file) throws IOException {
        User user = getCurrentUser();

        Document document = new Document();
        document.setFileName(file.getName());
        document.setOriginalName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setStatus("PROCESSING");
        document.setUploadedBy(user);
        document = documentRepository.save(document);

        List<String> chunks = chunkingService.chunkFile(file);

        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            float[] embedding = embeddingService.embedDocument(chunkText);

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(document);
            chunk.setDocumentName(file.getOriginalFilename());
            chunk.setContent(chunkText);
            chunk.setChunkIndex(i);
            chunk.setEmbedding(embedding);
            chunkEntities.add(chunk);
        }

        documentChunkRepository.saveAll(chunkEntities);

        document.setTotalChunks(chunks.size());
        document.setStatus("INDEXED");
        return documentRepository.save(document);
    }

    public List<Document> getAllDocuments() {
        User user = getCurrentUser();
        return documentRepository.findByUploadedByEmail(user.getEmail());
    }

    public Document getDocument(UUID id) {
        User user = getCurrentUser();
        return documentRepository.findByIdAndUploadedByEmail(id, user.getEmail())
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    @Transactional
    public void deleteDocument(UUID id) {
        User user = getCurrentUser();
        Document doc = documentRepository.findByIdAndUploadedByEmail(id, user.getEmail())
                .orElseThrow(() -> new RuntimeException("Document not found"));
        documentChunkRepository.deleteByDocumentId(doc.getId());
        documentRepository.deleteById(doc.getId());
    }
}