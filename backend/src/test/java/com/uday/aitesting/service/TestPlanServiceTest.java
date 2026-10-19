package com.uday.aitesting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TestPlanServiceTest {
    private final TestPlanService service = new TestPlanService(new ObjectMapper());

    @Test
    void fallbackPlanAlwaysIncludesRequiredCoreChecks() {
        var plan = service.createPlan("general checks", "https://example.com");
        var ids = plan.stream().map(TestPlanService.PlannedCheck::id).collect(Collectors.toSet());
        assertTrue(ids.contains("PAGE_REACHED"));
        assertTrue(ids.contains("PAGE_TITLE"));
        assertTrue(plan.size() <= 5);
    }

    @Test
    void fallbackPlanContainsOnlySupportedReadOnlyChecks() {
        Set<String> allowed = Set.of("PAGE_REACHED", "PAGE_TITLE", "LINKS_PRESENT",
                "IMAGES_ALT_TEXT", "HEADINGS_PRESENT", "FORMS_DISCOVERED");
        var plan = service.createPlan("search and filters", "https://example.com");
        assertTrue(plan.stream().allMatch(item -> allowed.contains(item.id())));
        assertEquals(plan.size(), plan.stream().map(TestPlanService.PlannedCheck::id).distinct().count());
    }
}
