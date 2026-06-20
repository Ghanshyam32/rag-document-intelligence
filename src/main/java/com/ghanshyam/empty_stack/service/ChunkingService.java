package com.ghanshyam.empty_stack.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    @Value("${app.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap:50}")
    private int chunkOverlap;

    // Entry point — give it a file, get back list of text chunks
    public List<String> chunkFile(MultipartFile file) throws IOException {
        String text = extractText(file);
        return splitIntoChunks(text);
    }

    // Step 1: Extract raw text from PDF or TXT
    private String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();

        if (filename != null && filename.endsWith(".pdf")) {
            // PDFBox reads the PDF and pulls out all text
            try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(doc);
            }
        } else {
            // Plain text file — just read it directly
            return new String(file.getBytes());
        }
    }

    // Step 2: Split text into overlapping chunks
    // Why overlap? So sentences at the boundary aren't cut off
    // e.g. chunk 1 ends at word 500, chunk 2 starts at word 450
    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+"); // split on whitespace

        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + chunkSize, words.length);

            // Join words back into a string
            String chunk = String.join(" ", java.util.Arrays.copyOfRange(words, start, end));
            chunks.add(chunk.trim());

            // Move forward by (chunkSize - overlap) so next chunk shares last N words
            start += (chunkSize - chunkOverlap);
        }

        return chunks;
    }
}