package com.example.rag.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VectorUtilsTest {

    @Test
    void formatsVectorForPostgres() {
        assertEquals("[1.0,2.5,-0.25]", VectorUtils.toLiteral(new float[] { 1.0f, 2.5f, -0.25f }));
    }

    @Test
    void emptyVector() {
        assertEquals("[]", VectorUtils.toLiteral(new float[0]));
    }
}
