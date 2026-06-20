CREATE TABLE documents (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           file_name VARCHAR(255) NOT NULL,
                           original_name VARCHAR(255) NOT NULL,
                           file_type VARCHAR(50) NOT NULL,
                           total_chunks INT DEFAULT 0,
                           status VARCHAR(50) DEFAULT 'PROCESSING',
                           uploaded_at TIMESTAMP DEFAULT NOW()
);