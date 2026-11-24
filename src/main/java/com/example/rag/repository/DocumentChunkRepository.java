package com.example.rag.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.rag.dto.PendingChunk;
import com.example.rag.dto.SearchResult;
import com.example.rag.util.VectorUtils;

@Repository
public class DocumentChunkRepository {

    private final JdbcTemplate jdbc;

    public DocumentChunkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int deleteBySource(String source) {
        return jdbc.update("DELETE FROM document_chunks WHERE source = ?", source);
    }

    /** Saves all chunks in one batch. The embedding column stays NULL until we embed them. */
    public void saveChunks(String source, List<String> chunks, String metadataJson) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            rows.add(new Object[] { source, i, chunks.get(i), metadataJson });
        }
        jdbc.batchUpdate(
                "INSERT INTO document_chunks (source, chunk_index, content, metadata) "
                        + "VALUES (?, ?, ?, ?::jsonb)",
                rows);
    }

    public List<Map<String, Object>> listDocuments() {
        return jdbc.queryForList(
                "SELECT source, COUNT(*) AS chunks, MIN(created_at) AS uploaded_at "
                        + "FROM document_chunks GROUP BY source ORDER BY source");
    }

    // ---------- Week 3: embeddings ----------

    public List<PendingChunk> findChunksWithoutEmbedding(int limit) {
        return jdbc.query(
                "SELECT id, content FROM document_chunks WHERE embedding IS NULL ORDER BY id LIMIT ?",
                (rs, rowNum) -> new PendingChunk(rs.getLong("id"), rs.getString("content")),
                limit);
    }

    public void updateEmbeddings(List<Long> ids, List<float[]> vectors) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            rows.add(new Object[] { VectorUtils.toLiteral(vectors.get(i)), ids.get(i) });
        }
        jdbc.batchUpdate("UPDATE document_chunks SET embedding = ?::vector WHERE id = ?", rows);
    }

    public int countWithoutEmbedding() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM document_chunks WHERE embedding IS NULL", Integer.class);
        return n == null ? 0 : n;
    }

    public int countAll() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM document_chunks", Integer.class);
        return n == null ? 0 : n;
    }

    // ---------- Week 4: similarity search ----------

    /**
     * Returns the topK chunks closest to the query vector.
     * "<=>" is cosine DISTANCE (0 = identical), so score = 1 - distance (1 = identical).
     * ORDER BY the distance expression + LIMIT is what lets PostgreSQL use the HNSW index.
     */
    public List<SearchResult> searchSimilar(String queryVectorLiteral, int topK) {
        return jdbc.query(
                "SELECT id, source, chunk_index, content, "
                        + "       1 - (embedding <=> ?::vector) AS score "
                        + "FROM document_chunks "
                        + "WHERE embedding IS NOT NULL "
                        + "ORDER BY embedding <=> ?::vector "
                        + "LIMIT ?",
                (rs, rowNum) -> new SearchResult(
                        rs.getLong("id"),
                        rs.getString("source"),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        rs.getDouble("score")),
                queryVectorLiteral, queryVectorLiteral, topK);
    }
}
