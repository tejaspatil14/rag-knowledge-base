CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS document_chunks (
    id           SERIAL PRIMARY KEY,
    source       TEXT NOT NULL,
    chunk_index  INT  NOT NULL,
    content      TEXT NOT NULL,
    embedding    VECTOR(768),
    metadata     JSONB,
    created_at   TIMESTAMP DEFAULT NOW()
);

-- HNSW index = fast approximate search by cosine distance.
-- Unlike ivfflat it works fine on an empty table, so we can create it right away.
CREATE INDEX IF NOT EXISTS document_chunks_embedding_idx
    ON document_chunks USING hnsw (embedding vector_cosine_ops);
