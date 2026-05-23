package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.dto.GeminiRequest;
import com.tracker.smarthabittracker.dto.GeminiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class GeminiService {

    @Value("${google.gemini.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public GeminiService() {
        this.restTemplate = new RestTemplate();
    }

    public String generateContent(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Gemini API key is not configured. Falling back to local heuristics.");
            return null;
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        try {
            GeminiRequest request = GeminiRequest.forText(prompt);
            GeminiResponse response = restTemplate.postForObject(url, request, GeminiResponse.class);

            if (response != null && response.getText() != null) {
                return stripMarkdownCodeBlocks(response.getText());
            }
        } catch (Exception e) {
            log.error("Error communicating with Gemini API: {}", e.getMessage());
        }

        return null;
    }

    private String stripMarkdownCodeBlocks(String text) {
        if (text == null) return null;
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
}
