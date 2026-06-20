CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_chunks (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
                                 document_name VARCHAR(255),
                                 content TEXT NOT NULL,
                                 chunk_index INT NOT NULL,
                                 embedding vector(1536),
                                 created_at TIMESTAMP DEFAULT NOW()
);

-- IVFFlat index for fast approximate nearest neighbor search
CREATE INDEX ON document_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);