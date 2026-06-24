package com.ghanshyam.empty_stack.repository;

import com.ghanshyam.empty_stack.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUploadedByEmail(String email);
    Optional<Document> findByIdAndUploadedByEmail(UUID id, String email);
}