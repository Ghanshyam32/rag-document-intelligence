package com.ghanshyam.empty_stack.controller;

import com.ghanshyam.empty_stack.model.Document;
import com.ghanshyam.empty_stack.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // POST /api/documents/upload
    @PostMapping("/upload")
    public ResponseEntity<Document> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Document saved = documentService.uploadDocument(file);
        return ResponseEntity.ok(saved);
    }

    // GET /api/documents
    @GetMapping
    public ResponseEntity<List<Document>> getAll() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    // GET /api/documents/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Document> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    // DELETE /api/documents/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}