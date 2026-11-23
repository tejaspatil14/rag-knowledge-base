package com.example.rag.util;

public final class VectorUtils {

    private VectorUtils() {
    }

    /** Converts {0.1, 0.2} into the text "[0.1,0.2]" which PostgreSQL casts with ?::vector. */
    public static String toLiteral(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
