-- How many chunks per document?
SELECT source, COUNT(*) AS chunks FROM document_chunks GROUP BY source;

-- Look at the first 3 chunks (first 150 characters each)
SELECT id, source, chunk_index, LEFT(content, 150) AS preview, metadata
FROM document_chunks
ORDER BY id
LIMIT 3;

-- Embeddings should still be empty (we add them in Week 3)
SELECT COUNT(*) AS chunks_without_embedding FROM document_chunks WHERE embedding IS NULL;
