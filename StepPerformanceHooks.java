package com.yourcompany.hooks;

import com.yourcompany.utils.PerformanceMetrics;
import com.yourcompany.utils.PerformanceStorage;
import io.cucumber.java.AfterStep;
import io.cucumber.java.BeforeStep;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Captures step-level performance metrics
 */
public class StepPerformanceHooks {
    
    private long stepStartTime;
    private WebDriver driver;
    private SPAPerformanceTracker spaTracker;
    private int stepCounter = 0;
    
    @BeforeStep(order = 0)
    public void beforeStep(Scenario scenario) {
        // Skip performance tracking if scenario has the @skip-performance tag
        if (shouldSkipPerformanceTracking(scenario)) {
            return;
        }

        stepStartTime = System.currentTimeMillis();
        driver = DriverManager.getDriver();

        if (spaTracker == null) {
            spaTracker = new SPAPerformanceTracker(driver);
        }

        stepCounter++;
    }
    
    @AfterStep(order = 100)
    public void afterStep(Scenario scenario) {
        // Skip performance tracking if scenario has the @skip-performance tag
        if (shouldSkipPerformanceTracking(scenario)) {
            return;
        }

        long stepDuration = System.currentTimeMillis() - stepStartTime;
        
        try {
            Thread.sleep(200);
            
            boolean domChanged = spaTracker.hasDOMChanged();
            
            if (domChanged) {
                SPAPerformanceTracker.PerformanceSnapshot snapshot = spaTracker.getQuickMetrics();
                
                if (snapshot != null) {
                    
                    // Create performance metrics object
                    PerformanceMetrics metrics = new PerformanceMetrics();
                    metrics.setStepName("Step #" + stepCounter);
                    metrics.setScenarioName(scenario.getName());
                    metrics.setFeatureName(getFeatureName(scenario));
                    metrics.setPageLoadTime(snapshot.getPageLoadTime());
                    metrics.setDomReadyTime(snapshot.getDomReadyTime());
                    metrics.setResponseTime(snapshot.getResponseTime());
                    metrics.setTtfb(snapshot.getTtfb());
                    metrics.setConnectTime(snapshot.getConnectTime());
                    metrics.setDomainLookupTime(snapshot.getDomainLookupTime());
                    metrics.setFromCache(snapshot.isFromCache());
                    
                    // Store metrics globally
                    PerformanceStorage.addMetrics(metrics);
                    
                    // ═══════════════════════════════════════════════════
                    // ALLURE: Create detailed performance attachment
                    // ═══════════════════════════════════════════════════
                    
                    String textSummary = createTextSummary(stepCounter, metrics);
                    Allure.addAttachment(
                        String.format("📊 Step #%d Performance", stepCounter),
                        "text/plain",
                        new ByteArrayInputStream(textSummary.getBytes(StandardCharsets.UTF_8)),
                        ".txt"
                    );
                    
                    // Create HTML dashboard
                    String htmlDashboard = createHtmlDashboard(stepCounter, metrics);
                    Allure.addAttachment(
                        String.format("📈 Step #%d Dashboard", stepCounter),
                        "text/html",
                        new ByteArrayInputStream(htmlDashboard.getBytes(StandardCharsets.UTF_8)),
                        ".html"
                    );
                    
                    // Attach raw JSON
                    String navPerfJSON = spaTracker.getNavigationPerformanceJSON();
                    Allure.addAttachment(
                        String.format("📋 Step #%d Raw Data", stepCounter),
                        "application/json",
                        navPerfJSON,
                        ".json"
                    );

                    String pageLoadStatus = getPerformanceStatusText(metrics.getPageLoadTime(), 2000, 3000);
                    
                    // Cucumber log
                    scenario.log(String.format(
                        "┌─────────────────────────────────────────────────┐\n" +
                        "│ 🔄 STEP #%d PERFORMANCE METRICS - %s\n" +
                        "├─────────────────────────────────────────────────┤\n" +
                        "│ 📄 Page Load:      %4d ms %s\n" +
                        "│ 🔄 DOM Ready:      %4d ms %s\n" +
                        "│ 📡 Response:       %4d ms %s\n" +
                        "│ ⏱️  TTFB:           %4d ms %s\n" +
                        "│ 🔌 Connect:        %4d ms %s\n" +
                        "│ 🌐 DNS Lookup:     %4d ms %s\n" +
                        "│ 💾 From Cache:     %s\n" +
                        "└─────────────────────────────────────────────────┘",
                        stepCounter,
                        pageLoadStatus,
                        metrics.getPageLoadTime(), getStatusEmoji(metrics.getPageLoadTime(), 3000),
                        metrics.getDomReadyTime(), getStatusEmoji(metrics.getDomReadyTime(), 2000),
                        metrics.getResponseTime(), getStatusEmoji(metrics.getResponseTime(), 1000),
                        metrics.getTtfb(), getStatusEmoji(metrics.getTtfb(), 500),
                        metrics.getConnectTime(), getStatusEmoji(metrics.getConnectTime(), 300),
                        metrics.getDomainLookupTime(), getStatusEmoji(metrics.getDomainLookupTime(), 200),
                        metrics.isFromCache() ? "Yes ✅" : "No"
                    ));
                }
            }
            
        } catch (Exception e) {
            scenario.log("⚠️ Performance error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String getPerformanceStatusText(long value, long goodThreshold, long poorThreshold) {
    if (value <= goodThreshold) {
        return "EXCELLENT";  // ← Categories will match this
    } else if (value <= poorThreshold) {
        return "GOOD";       // ← Categories will match this
    } else {
        return "SLOW";       // ← Categories will match this
    }
        
    private String createTextSummary(int stepNumber, PerformanceMetrics metrics) {
        return String.format(
            "╔══════════════════════════════════════════════════════════════════╗\n" +
            "║              STEP #%-3d PERFORMANCE METRICS                       ║\n" +
            "╠══════════════════════════════════════════════════════════════════╣\n" +
            "║  📄 Page Load Time:      %-25s ms            ║\n" +
            "║  🔄 DOM Ready Time:      %-25s ms            ║\n" +
            "║  📡 Response Time:       %-25s ms            ║\n" +
            "║  ⏱️  TTFB:                %-25s ms            ║\n" +
            "║  🔌 Connect Time:        %-25s ms            ║\n" +
            "║  🌐 Domain Lookup Time:  %-25s ms            ║\n" +
            "║  💾 From Cache:          %-35s    ║\n" +
            "╠══════════════════════════════════════════════════════════════════╣\n" +
            "║  Scenario: %-56s ║\n" +
            "║  Feature:  %-56s ║\n" +
            "╚══════════════════════════════════════════════════════════════════╝",
            stepNumber,
            metrics.getPageLoadTime(),
            metrics.getDomReadyTime(),
            metrics.getResponseTime(),
            metrics.getTtfb(),
            metrics.getConnectTime(),
            metrics.getDomainLookupTime(),
            metrics.isFromCache() ? "Yes ✅" : "No",
            truncate(metrics.getScenarioName(), 56),
            truncate(metrics.getFeatureName(), 56)
        );
    }
    
    private String createHtmlDashboard(int stepNumber, PerformanceMetrics metrics) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }");
        html.append(".dashboard { background: white; border-radius: 12px; padding: 30px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); }");
        html.append("h1 { color: #333; margin-top: 0; border-bottom: 3px solid #667eea; padding-bottom: 15px; }");
        html.append(".metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 15px; margin: 20px 0; }");
        html.append(".metric-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px; text-align: center; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
        html.append(".metric-value { font-size: 32px; font-weight: bold; margin: 10px 0; }");
        html.append(".metric-label { font-size: 12px; opacity: 0.9; text-transform: uppercase; }");
        html.append(".metric-status { font-size: 24px; margin-top: 5px; }");
        html.append("</style>");
        html.append("</head><body>");
        html.append("<div class='dashboard'>");
        html.append(String.format("<h1>📊 Step #%d Performance Dashboard</h1>", stepNumber));
        
        // Metrics Grid
        html.append("<div class='metrics-grid'>");
        
        html.append(String.format(
            "<div class='metric-card'><div class='metric-label'>Page Load Time</div><div class='metric-value'>%d ms</div><div class='metric-status'>%s</div></div>",
            metrics.getPageLoadTime(), getStatusEmoji(metrics.getPageLoadTime(), 3000)
        ));
        
        html.append(String.format(
            "<div class='metric-card'><div class='metric-label'>DOM Ready Time</div><div class='metric-value'>%d ms</div><div class='metric-status'>%s</div></div>",
            metrics.getDomReadyTime(), getStatusEmoji(metrics.getDomReadyTime(), 2000)
        ));
        
        html.append(String.format(
            "<div class='metric-card'><div class='metric-label'>Response Time</div><div class='metric-value'>%d ms</div><div class='metric-status'>%s</div></div>",
            metrics.getResponseTime(), getStatusEmoji(metrics.getResponseTime(), 1000)
        ));
        
        html.append(String.format(
            "<div class='metric-card'><div class='metric-label'>TTFB</div><div class='metric-value'>%d ms</div><div class='metric-status'>%s</div></div>",
            metrics.getTtfb(), getStatusEmoji(metrics.getTtfb(), 500)
        ));
        
        html.append(String.format(
            "<div class='metric-card'><div class='metric-label'>Connect Time</div><div class='metric-value'>%d ms</div><div class='metric-status'>%s</div></div>",
            metrics.getConnectTime(), getStatusEmoji(metrics.getConnectTime(), 300)
        ));
        
        html.append(String.format(
            "<div class='metric-card'><div class='metric-label'>DNS Lookup</div><div class='metric-value'>%d ms</div><div class='metric-status'>%s</div></div>",
            metrics.getDomainLookupTime(), getStatusEmoji(metrics.getDomainLookupTime(), 200)
        ));
        
        html.append("</div>");
        html.append("</div>");
        html.append("</body></html>");
        
        return html.toString();
    }
    
    private String getStatusEmoji(long value, long threshold) {
        if (value < threshold) return "✅";
        if (value < threshold * 1.5) return "⚡";
        return "❌";
    }
    
    private String getFeatureName(Scenario scenario) {
        String uri = scenario.getUri().toString();
        return uri.substring(uri.lastIndexOf("/") + 1).replace(".feature", "");
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Check if performance tracking should be skipped for this scenario
     * Returns true if scenario has @skip-performance tag
     */
    private boolean shouldSkipPerformanceTracking(Scenario scenario) {
        return scenario.getSourceTagNames().stream()
                .anyMatch(tag -> tag.equalsIgnoreCase("@skip-performance") ||
                                 tag.equalsIgnoreCase("@skipperformance") ||
                                 tag.equalsIgnoreCase("@no-performance"));
    }

}
