package com.uday.aitesting.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Service
public class TestPlanService {
    @Value("${app.openai.api-key:}") private String apiKey;
    @Value("${app.openai.model:gpt-4o-mini}") private String model;

    public List<String> createPlan(String scope, String url) {
        if (apiKey == null || apiKey.isBlank()) {
            return List.of("Page loads and has a non-empty title", "Page has a valid HTTP response",
                    "Page exposes at least one useful link when links are present");
        }
        try {
            RestClient client = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
            Map<?, ?> response = client.post().uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of(
                    "model", model,
                    "temperature", 0,
                    "messages", List.of(
                        Map.of("role", "system", "content",
                            "Return only a JSON array of up to 5 short, safe, read-only browser test names. Do not suggest login, form submission, purchases, deletion, or bypassing access controls. Do not generate code."),
                        Map.of("role", "user", "content", "Authorized target URL: " + url + "\\nTesting scope: " + scope)
                    )
                )).retrieve().body(Map.class);
            if (response == null) return fallbackPlan();
            Object choicesObject = response.get("choices");
            if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) return fallbackPlan();
            Object messageObject = ((Map<?, ?>) choices.get(0)).get("message");
            Object contentObject = ((Map<?, ?>) messageObject).get("content");
            if (!(contentObject instanceof String content)) return fallbackPlan();
            // Keep AI output informational only. The browser runner executes fixed safe checks, not AI-provided code.
            String cleaned = content.replace("[", "").replace("]", "").replace("\"", "");
            return cleaned.lines().map(String::trim).filter(s -> !s.isBlank()).limit(5).toList();
        } catch (Exception ignored) {
            return fallbackPlan();
        }
    }

    private List<String> fallbackPlan() {
        return List.of("Page loads and has a non-empty title", "Page has a valid HTTP response",
                "Page exposes at least one useful link when links are present");
    }
}
