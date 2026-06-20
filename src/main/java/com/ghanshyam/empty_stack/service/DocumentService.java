package com.ghanshyam.empty_stack.service;

import com.ghanshyam.empty_stack.model.Document;
import com.ghanshyam.empty_stack.model.DocumentChunk;
import com.ghanshyam.empty_stack.repository.DocumentChunkRepository;
import com.ghanshyam.empty_stack.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public Document uploadDocument(MultipartFile file) throws IOException {

        Document document = new Document();
        document.setFileName(file.getName());
        document.setOriginalName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setStatus("PROCESSING");
        document = documentRepository.save(document);

        List<String> chunks = chunkingService.chunkFile(file);

        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i).replace("\u0000", "");

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
        return documentRepository.findAll();
    }

    public Document getDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
    }

    @Transactional
    public void deleteDocument(UUID id) {
        documentChunkRepository.deleteByDocumentId(id);
        documentRepository.deleteById(id);
    }
}