-- Is the HNSW index being used? (look for "Index Scan using document_chunks_embedding_idx").
-- With only ~70 rows PostgreSQL may prefer a plain scan, which is fine and just as fast at this size.
EXPLAIN ANALYZE
SELECT id, 1 - (embedding <=> (SELECT embedding FROM document_chunks WHERE id = 1)) AS score
FROM document_chunks
ORDER BY embedding <=> (SELECT embedding FROM document_chunks WHERE id = 1)
LIMIT 5;
