-- ============ RUN THIS ONCE, BEFORE starting the app (switches 1536 -> 768 dims) ============
DROP INDEX IF EXISTS document_chunks_embedding_idx;
ALTER TABLE document_chunks ALTER COLUMN embedding TYPE vector(768);

-- ============ After running POST /api/embeddings/run ============
-- 1. Every chunk should have a vector (this should return 0)
SELECT COUNT(*) AS still_missing FROM document_chunks WHERE embedding IS NULL;

-- 2. Check the dimensions (should be 768)
SELECT id, source, vector_dims(embedding) AS dims FROM document_chunks LIMIT 3;

-- 3. Mini search: find the 3 chunks most similar to chunk id 1
SELECT id, source, chunk_index, LEFT(content, 80) AS preview,
       1 - (embedding <=> (SELECT embedding FROM document_chunks WHERE id = 1)) AS score
FROM document_chunks
ORDER BY embedding <=> (SELECT embedding FROM document_chunks WHERE id = 1)
LIMIT 3;
