-- Run this in psql / DBeaver / pgAdmin to confirm pgvector works.
-- Uses a small scratch table with 3-dimensional vectors so you can see the numbers.

CREATE TABLE IF NOT EXISTS vector_test (
    id SERIAL PRIMARY KEY,
    label TEXT,
    embedding VECTOR(3)
);

INSERT INTO vector_test (label, embedding) VALUES
  ('cat',    '[1, 0.9, 0]'),
  ('kitten', '[0.95, 0.85, 0.1]'),
  ('car',    '[0, 0.1, 1]');

-- Which item is most similar to "cat"? (<=> is cosine distance, score = 1 - distance)
SELECT label, 1 - (embedding <=> '[1, 0.9, 0]') AS score
FROM vector_test
ORDER BY embedding <=> '[1, 0.9, 0]'
LIMIT 3;

-- Expected: cat (1.0), kitten (~0.99), car (low)

DROP TABLE vector_test;
