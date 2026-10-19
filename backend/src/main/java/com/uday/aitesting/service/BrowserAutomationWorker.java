package com.uday.aitesting.service;

import com.uday.aitesting.model.*;
import com.uday.aitesting.repository.AutomationResultRepository;
import com.uday.aitesting.repository.AutomationRunRepository;
import com.uday.aitesting.repository.TestingRequestRepository;
import com.uday.aitesting.service.TestPlanService.PlannedCheck;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BrowserAutomationWorker {
    private final AutomationRunRepository runRepository;
    private final AutomationResultRepository resultRepository;
    private final TestingRequestRepository requestRepository;
    private final UrlSafetyService urlSafetyService;
    private final TestPlanService testPlanService;

    @Value("${app.browser.remote-url:}") private String remoteUrl;
    @Value("${app.browser.timeout-seconds:20}") private long timeoutSeconds;
    @Value("${app.artifacts-dir:./artifacts}") private String artifactsDir;

    public BrowserAutomationWorker(AutomationRunRepository runRepository,
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

    /** Uses independent repository transactions so a long-running browser session does not hold a DB transaction open. */
    public void execute(UUID runId) {
        AutomationRun run = runRepository.findWithTestingRequestById(runId).orElse(null);
        if (run == null) return;
        TestingRequest request = run.getTestingRequest();
        WebDriver driver = null;
        try {
            run.start();
            request.setStatus(RequestStatus.RUNNING);
            runRepository.save(run);
            requestRepository.save(request);

            URI target = urlSafetyService.validateAndNormalize(request.getTargetUrl());
            driver = createDriver(request.getBrowser());
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeoutSeconds));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(timeoutSeconds));
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            driver.get(target.toString());

            // This is a secondary check only: browser redirects may already have been followed.
            List<PlannedCheck> plan = testPlanService.restorePlan(request.getPlannedCheckIds());
            boolean finalUrlIsSafe = isSafeFinalUrl(driver.getCurrentUrl());
            for (PlannedCheck check : plan) {
                if (!finalUrlIsSafe && "PAGE_REACHED".equals(check.id())) {
                    saveResult(run, check.name(), TestOutcome.FAIL,
                            "The browser ended at a URL that failed the URL safety check.", driver);
                    continue;
                }
                if (!finalUrlIsSafe) {
                    saveResult(run, check.name(), TestOutcome.SKIPPED,
                            "Skipped because the final browser URL failed the safety check.", driver);
                    continue;
                }
                try {
                    CheckResult result = executeCheck(check.id(), driver);
                    saveResult(run, check.name(), result.outcome(), result.details(), driver);
                } catch (Exception checkException) {
                    saveResult(run, check.name(), TestOutcome.ERROR,
                            checkException.getClass().getSimpleName() + ": " + safe(checkException.getMessage()), driver);
                }
            }

            run.complete();
            request.setStatus(RequestStatus.COMPLETED);
        } catch (Exception ex) {
            run.fail(ex.getClass().getSimpleName() + ": " + safe(ex.getMessage()));
            request.setStatus(RequestStatus.FAILED);
            saveResult(run, "Browser execution", TestOutcome.ERROR,
                    ex.getClass().getSimpleName() + ": " + safe(ex.getMessage()), driver);
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) { }
            }
            runRepository.save(run);
            requestRepository.save(request);
        }
    }

    private CheckResult executeCheck(String id, WebDriver driver) {
        return switch (id) {
            case "PAGE_TITLE" -> {
                String title = driver.getTitle();
                yield new CheckResult(title != null && !title.isBlank() ? TestOutcome.PASS : TestOutcome.FAIL,
                        "Page title: " + safe(title));
            }
            case "PAGE_REACHED" -> {
                String currentUrl = driver.getCurrentUrl();
                boolean reached = currentUrl != null && (currentUrl.startsWith("http://") || currentUrl.startsWith("https://"));
                yield new CheckResult(reached ? TestOutcome.PASS : TestOutcome.FAIL, "Final URL: " + safe(currentUrl));
            }
            case "LINKS_PRESENT" -> {
                int count = driver.findElements(By.cssSelector("a[href]")).size();
                yield new CheckResult(count == 0 ? TestOutcome.SKIPPED : TestOutcome.PASS,
                        "Detected " + count + " anchor links. Links are inspected but not followed.");
            }
            case "IMAGES_ALT_TEXT" -> {
                List<WebElement> images = driver.findElements(By.cssSelector("img"));
                if (images.isEmpty()) yield new CheckResult(TestOutcome.SKIPPED, "No images were found.");
                long missing = images.stream().filter(image -> image.getAttribute("alt") == null).count();
                yield new CheckResult(missing == 0 ? TestOutcome.PASS : TestOutcome.FAIL,
                        images.size() + " images found; " + missing + " are missing an alt attribute.");
            }
            case "HEADINGS_PRESENT" -> {
                int count = driver.findElements(By.cssSelector("h1, h2, h3")).size();
                yield new CheckResult(count > 0 ? TestOutcome.PASS : TestOutcome.FAIL,
                        "Detected " + count + " H1-H3 headings.");
            }
            case "FORMS_DISCOVERED" -> {
                int count = driver.findElements(By.cssSelector("form")).size();
                yield new CheckResult(count == 0 ? TestOutcome.SKIPPED : TestOutcome.PASS,
                        "Detected " + count + " forms. Forms were inspected only; none were changed or submitted.");
            }
            default -> new CheckResult(TestOutcome.SKIPPED, "Unknown check identifier was rejected by the execution allow-list.");
        };
    }

    private boolean isSafeFinalUrl(String rawUrl) {
        try {
            urlSafetyService.validateAndNormalize(rawUrl);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private WebDriver createDriver(String browser) throws Exception {
        String normalized = browser == null ? "chrome" : browser.toLowerCase(Locale.ROOT);
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
            } catch (Exception ignored) { }
        }
        resultRepository.save(new AutomationResult(run, name, outcome, details, screenshot));
    }

    private String safe(String value) {
        if (value == null) return "(none)";
        String sanitized = value.replaceAll("[\\r\\n\\t]+", " ");
        return sanitized.length() > 1800 ? sanitized.substring(0, 1800) : sanitized;
    }

    private record CheckResult(TestOutcome outcome, String details) { }
}
