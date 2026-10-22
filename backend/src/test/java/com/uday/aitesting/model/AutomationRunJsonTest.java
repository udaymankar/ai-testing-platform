package com.uday.aitesting.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutomationRunJsonTest {
    @Test
    void serializationDoesNotExposeLazyTestingRequestRelationship() throws Exception {
        TestingRequest request = new TestingRequest(
                "https://example.com",
                "General exploratory checks",
                "Chrome",
                true,
                null
        );
        AutomationRun run = new AutomationRun(request);

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(run);

        assertThat(json).contains("\"status\":\"QUEUED\"");
        assertThat(json).doesNotContain("testingRequest");
        assertThat(json).doesNotContain("targetUrl");
    }
}
