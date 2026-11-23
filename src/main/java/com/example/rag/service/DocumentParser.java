package com.example.rag.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

/**
 * Turns a PDF / DOCX / TXT file into plain text using Apache Tika.
 */
@Component
public class DocumentParser {

    private final Tika tika = new Tika();

    public DocumentParser() {
        // By default Tika stops after 100,000 characters. -1 means "no limit".
        tika.setMaxStringLength(-1);
    }

    public String extractText(InputStream in) throws IOException, TikaException {
        String text = tika.parseToString(in);
        // PostgreSQL cannot store the NUL character, and some PDFs contain it.
        return text.replace("\u0000", "");
    }
}
