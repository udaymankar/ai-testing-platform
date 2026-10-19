package com.uday.aitesting.service;

import com.uday.aitesting.model.*;
import com.uday.aitesting.repository.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BrowserAutomationService {
    private final AutomationRunRepository runRepository;
    private final AutomationResultRepository resultRepository;
    private final TestingRequestRepository requestRepository;
    private final UrlSafetyService urlSafetyService;
    private final TestPlanService testPlanService;

    @Value("${app.browser.remote-url:}") private String remoteUrl;
    @Value("${app.browser.timeout-seconds:20}") private long timeoutSeconds;
    @Value("${app.artifacts-dir:./artifacts}") private String artifactsDir;

    public BrowserAutomationService(AutomationRunRepository runRepository,
                                    AutomationResultRepository resultRepository,
                                    TestingRequestRepository requestRepository,
                                    UrlSafetyService urlSafetyService,
                                    TestPlanService testPlanService) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.requestRepository = requestRepository;
        this.urlSafetyService = urlSafetyService;
        this.testPlanService = testPlanService;
    }

    @Async
    public void executeAsync(UUID runId) {
        execute(runId);
    }

    @Transactional
    public void execute(UUID runId) {
        AutomationRun run = runRepository.findById(runId).orElse(null);
        if (run == null) return;
        TestingRequest request = run.getTestingRequest();
        WebDriver driver = null;
        try {
            run.start();
            request.setStatus(RequestStatus.RUNNING);
            runRepository.save(run);
            requestRepository.save(request);

            urlSafetyService.validateAndNormalize(request.getTargetUrl());
            driver = createDriver(request.getBrowser());
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeoutSeconds));
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            driver.get(request.getTargetUrl());

            String title = driver.getTitle();
            saveResult(run, "Page loads and has a non-empty title",
                    title != null && !title.isBlank() ? TestOutcome.PASS : TestOutcome.FAIL,
                    "Page title: " + safe(title), driver);

            String currentUrl = driver.getCurrentUrl();
            saveResult(run, "Browser reached a page",
                    currentUrl != null && currentUrl.startsWith("http") ? TestOutcome.PASS : TestOutcome.FAIL,
                    "Final URL: " + safe(currentUrl), driver);

            List<WebElement> links = driver.findElements(By.cssSelector("a[href]"));
            saveResult(run, "Page exposes useful links when present",
                    links.isEmpty() ? TestOutcome.SKIPPED : TestOutcome.PASS,
                    "Detected " + links.size() + " anchor links.", driver);

            run.complete();
            request.setStatus(RequestStatus.COMPLETED);
        } catch (Exception ex) {
            run.fail(ex.getClass().getSimpleName() + ": " + safe(ex.getMessage()));
            request.setStatus(RequestStatus.FAILED);
            saveResult(run, "Browser execution", TestOutcome.ERROR,
                    ex.getClass().getSimpleName() + ": " + safe(ex.getMessage()), driver);
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
            }
            runRepository.save(run);
            requestRepository.save(request);
        }
    }

    private WebDriver createDriver(String browser) throws Exception {
        String normalized = browser == null ? "chrome" : browser.toLowerCase();
        if (remoteUrl != null && !remoteUrl.isBlank()) {
            URL endpoint = new URL(remoteUrl);
            return switch (normalized) {
                case "firefox" -> new RemoteWebDriver(endpoint, new FirefoxOptions());
                case "edge" -> new RemoteWebDriver(endpoint, new EdgeOptions());
                default -> new RemoteWebDriver(endpoint, chromeOptions());
            };
        }
        return switch (normalized) {
            case "firefox" -> new FirefoxDriver(new FirefoxOptions());
            case "edge" -> new EdgeDriver(new EdgeOptions());
            default -> new ChromeDriver(chromeOptions());
        };
    }

    private ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--disable-gpu", "--window-size=1440,1000");
        return options;
    }

    private void saveResult(AutomationRun run, String name, TestOutcome outcome, String details, WebDriver driver) {
        String screenshot = null;
        if (driver instanceof TakesScreenshot takesScreenshot) {
            try {
                Path dir = Paths.get(artifactsDir, run.getId().toString());
                Files.createDirectories(dir);
                Path target = dir.resolve(UUID.randomUUID() + ".png");
                Files.write(target, takesScreenshot.getScreenshotAs(OutputType.BYTES));
                screenshot = target.toString();
            } catch (Exception ignored) {}
        }
        resultRepository.save(new AutomationResult(run, name, outcome, details, screenshot));
    }

    private String safe(String value) {
        if (value == null) return "(none)";
        return value.length() > 1800 ? value.substring(0, 1800) : value;
    }
}
