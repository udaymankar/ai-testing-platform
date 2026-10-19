package com.uday.aitesting.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Dispatches browser jobs off the HTTP request thread. */
@Service
public class BrowserAutomationService {
    private final BrowserAutomationWorker worker;

    public BrowserAutomationService(BrowserAutomationWorker worker) {
        this.worker = worker;
    }

    @Async
    public void executeAsync(UUID runId) {
        worker.execute(runId);
    }
}
