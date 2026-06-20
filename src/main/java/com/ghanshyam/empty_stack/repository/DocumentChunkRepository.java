package com.ghanshyam.empty_stack.repository;

import com.ghanshyam.empty_stack.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    // This is the core RAG query — find top K most similar chunks to a query vector
    @Query(value = """
        SELECT dc.id, dc.document_id, dc.document_name, dc.content, 
               dc.chunk_index, dc.embedding, dc.created_at,
               1 - (dc.embedding <=> CAST(:queryEmbedding AS vector)) AS similarity
        FROM document_chunks dc
        ORDER BY dc.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(@Param("queryEmbedding") String queryEmbedding,
                                           @Param("topK") int topK);

    void deleteByDocumentId(UUID documentId);
}