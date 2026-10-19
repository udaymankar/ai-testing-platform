package com.uday.aitesting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Produces a constrained plan of read-only checks; AI output is treated as untrusted data. */
@Service
public class TestPlanService {
    public record PlannedCheck(String id, String name, String description) {}

    private static final Map<String, PlannedCheck> ALLOWED_CHECKS;
    static {
        Map<String, PlannedCheck> checks = new LinkedHashMap<>();
        checks.put("PAGE_TITLE", new PlannedCheck("PAGE_TITLE", "Page title is present", "Confirm the loaded page has a non-empty title."));
        checks.put("PAGE_REACHED", new PlannedCheck("PAGE_REACHED", "Browser reaches the target page", "Confirm the browser ends on an HTTP or HTTPS page."));
        checks.put("LINKS_PRESENT", new PlannedCheck("LINKS_PRESENT", "Links are discoverable", "Inspect anchor links without following or submitting them."));
        checks.put("IMAGES_ALT_TEXT", new PlannedCheck("IMAGES_ALT_TEXT", "Images have alternative text", "Inspect image alt attributes for basic accessibility."));
        checks.put("HEADINGS_PRESENT", new PlannedCheck("HEADINGS_PRESENT", "Headings are discoverable", "Check for at least one H1, H2, or H3 heading."));
        checks.put("FORMS_DISCOVERED", new PlannedCheck("FORMS_DISCOVERED", "Forms are inspected safely", "Count forms without typing, submitting, or changing data."));
        ALLOWED_CHECKS = Map.copyOf(checks);
    }

    @Value("${app.openai.api-key:}") private String apiKey;
    @Value("${app.openai.model:gpt-4o-mini}") private String model;
    private final ObjectMapper objectMapper;

    public TestPlanService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<PlannedCheck> createPlan(String scope, String url) {
        List<String> requestedIds = requestAiPlan(scope, url);
        LinkedHashMap<String, PlannedCheck> selected = new LinkedHashMap<>();
        selected.put("PAGE_REACHED", ALLOWED_CHECKS.get("PAGE_REACHED"));
        selected.put("PAGE_TITLE", ALLOWED_CHECKS.get("PAGE_TITLE"));
        for (String id : requestedIds) {
            PlannedCheck check = ALLOWED_CHECKS.get(id);
            if (check != null) selected.putIfAbsent(id, check);
            if (selected.size() >= 5) break;
        }
        if (selected.size() == 2) {
            selected.put("LINKS_PRESENT", ALLOWED_CHECKS.get("LINKS_PRESENT"));
            selected.put("HEADINGS_PRESENT", ALLOWED_CHECKS.get("HEADINGS_PRESENT"));
        }
        return List.copyOf(selected.values());
    }

    private List<String> requestAiPlan(String scope, String url) {
        if (apiKey == null || apiKey.isBlank()) return fallbackIds();
        try {
            RestClient client = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
            Map<?, ?> response = client.post().uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of(
                    "model", model,
                    "temperature", 0,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                        Map.of("role", "system", "content",
                            "Return a JSON object with one property named checkIds containing an array of up to 5 IDs selected only from: LINKS_PRESENT, IMAGES_ALT_TEXT, HEADINGS_PRESENT, FORMS_DISCOVERED. Never return code, selectors, URLs, credentials, or actions that click, type, submit, purchase, delete, log in, or bypass controls."),
                        Map.of("role", "user", "content", "Authorized target URL: " + url + "\nTesting scope: " + scope)
                    )
                )).retrieve().body(Map.class);
            if (response == null) return fallbackIds();
            Object choicesObject = response.get("choices");
            if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) return fallbackIds();
            Object messageObject = ((Map<?, ?>) choices.get(0)).get("message");
            if (!(messageObject instanceof Map<?, ?> message)) return fallbackIds();
            Object contentObject = message.get("content");
            if (!(contentObject instanceof String content)) return fallbackIds();
            Map<String, Object> parsed = objectMapper.readValue(content, new TypeReference<>() {});
            Object idsObject = parsed.get("checkIds");
            if (!(idsObject instanceof List<?> ids)) return fallbackIds();
            List<String> result = new ArrayList<>();
            for (Object value : ids) {
                if (value instanceof String id) {
                    String normalized = id.trim().toUpperCase(Locale.ROOT);
                    if (ALLOWED_CHECKS.containsKey(normalized) && !result.contains(normalized)) result.add(normalized);
                    if (result.size() == 5) break;
                }
            }
            return result.isEmpty() ? fallbackIds() : result;
        } catch (Exception ignored) {
            return fallbackIds();
        }
    }

    private List<String> fallbackIds() {
        return List.of("LINKS_PRESENT", "HEADINGS_PRESENT", "IMAGES_ALT_TEXT");
    }
}
