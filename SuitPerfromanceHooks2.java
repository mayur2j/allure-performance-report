package com.yourcompany.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.yourcompany.utils.PerformanceMetrics;
import com.yourcompany.utils.PerformanceStorage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Generates detailed performance report with pass/fail status and thresholds
 */
public class SuitPerfromanceHooks2 {

    private static boolean reportGenerated = false;

    // Define thresholds as constants for easy maintenance
    private static final long PAGE_LOAD_GOOD = 2000;
    private static final long PAGE_LOAD_POOR = 3000;
    private static final long DOM_READY_GOOD = 1500;
    private static final long DOM_READY_POOR = 2500;
    private static final long RESPONSE_GOOD = 800;
    private static final long RESPONSE_POOR = 1200;
    private static final long TTFB_GOOD = 400;
    private static final long TTFB_POOR = 600;
    private static final long CONNECT_GOOD = 200;
    private static final long CONNECT_POOR = 400;
    private static final long DNS_GOOD = 100;
    private static final long DNS_POOR = 200;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!reportGenerated) {
                generateDetailedReport();
            }
        }));
    }

    public static synchronized void generateDetailedReport() {
        if (reportGenerated) {
            return;
        }

        System.out.println("🔄 Generating detailed performance report...");

        Map<String, Double> averages = PerformanceStorage.calculateSuiteAverages();
        Map<String, Object> stats = PerformanceStorage.getStatistics();

        if (averages.isEmpty()) {
            System.out.println("⚠️  No performance metrics collected");
            return;
        }

        try {
            createDetailedHtmlReport(averages, stats);
            PerformanceStorage.exportToJson("target/allure-results/performance-metrics.json");

            reportGenerated = true;
            System.out.println("✅ Detailed performance report generated");

        } catch (Exception e) {
            System.err.println("❌ Error generating performance report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createDetailedHtmlReport(Map<String, Double> averages, Map<String, Object> stats) {
        try {
            StringBuilder html = new StringBuilder();

            // Calculate overall pass/fail
            boolean overallPassed = calculateOverallPass(averages);
            int passedCount = countPassedMetrics(averages);
            int totalMetrics = 6;

            html.append("<!DOCTYPE html><html><head>");
            html.append("<meta charset='UTF-8'>");
            html.append("<title>Performance Test Report</title>");
            html.append("<style>");

            // CSS Styles
            html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
            html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f7fa; padding: 20px; }");
            html.append(".container { max-width: 1400px; margin: 0 auto; background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); overflow: hidden; }");

            // Header styles
            html.append(".header { padding: 40px; text-align: center; color: white; }");
            html.append(".header.passed { background: linear-gradient(135deg, #28a745 0%, #20c997 100%); }");
            html.append(".header.failed { background: linear-gradient(135deg, #dc3545 0%, #c82333 100%); }");
            html.append(".header h1 { font-size: 48px; margin-bottom: 10px; }");
            html.append(".header .status { font-size: 72px; margin: 20px 0; }");
            html.append(".header .message { font-size: 24px; opacity: 0.95; }");
            html.append(".header .score { font-size: 20px; margin-top: 15px; opacity: 0.9; }");

            // Section styles
            html.append(".section { padding: 30px 40px; border-bottom: 1px solid #e9ecef; }");
            html.append(".section:last-child { border-bottom: none; }");
            html.append(".section-title { font-size: 28px; margin-bottom: 25px; color: #2c3e50; border-left: 5px solid #007bff; padding-left: 15px; }");

            // Metrics grid
            html.append(".metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 25px; }");
            html.append(".metric-card { border-radius: 10px; padding: 25px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); transition: transform 0.2s, box-shadow 0.2s; }");
            html.append(".metric-card:hover { transform: translateY(-3px); box-shadow: 0 4px 15px rgba(0,0,0,0.15); }");
            html.append(".metric-card.passed { background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%); border: 2px solid #28a745; }");
            html.append(".metric-card.warning { background: linear-gradient(135deg, #fff3cd 0%, #ffe69c 100%); border: 2px solid #ffc107; }");
            html.append(".metric-card.failed { background: linear-gradient(135deg, #f8d7da 0%, #f5c6cb 100%); border: 2px solid #dc3545; }");

            html.append(".metric-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }");
            html.append(".metric-name { font-size: 18px; font-weight: 600; color: #2c3e50; }");
            html.append(".metric-status { font-size: 32px; }");
            html.append(".metric-value { font-size: 42px; font-weight: bold; margin: 10px 0; }");
            html.append(".metric-card.passed .metric-value { color: #155724; }");
            html.append(".metric-card.warning .metric-value { color: #856404; }");
            html.append(".metric-card.failed .metric-value { color: #721c24; }");

            html.append(".threshold-info { margin-top: 15px; padding-top: 15px; border-top: 1px solid rgba(0,0,0,0.1); }");
            html.append(".threshold-row { display: flex; justify-content: space-between; margin: 8px 0; font-size: 14px; }");
            html.append(".threshold-label { font-weight: 600; color: #495057; }");
            html.append(".threshold-value { color: #6c757d; }");
            html.append(".status-badge { display: inline-block; padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: bold; margin-top: 10px; }");
            html.append(".status-badge.passed { background: #28a745; color: white; }");
            html.append(".status-badge.warning { background: #ffc107; color: #000; }");
            html.append(".status-badge.failed { background: #dc3545; color: white; }");

            // Scenario table styles
            html.append(".scenario-table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
            html.append(".scenario-table th { background: #007bff; color: white; padding: 15px; text-align: left; font-weight: 600; }");
            html.append(".scenario-table td { padding: 12px 15px; border-bottom: 1px solid #dee2e6; }");
            html.append(".scenario-table tr:hover { background: #f8f9fa; }");
            html.append(".scenario-table .scenario-name { font-weight: 600; color: #2c3e50; }");
            html.append(".scenario-table .metric-cell { text-align: center; font-family: 'Courier New', monospace; }");
            html.append(".scenario-table .passed-cell { color: #28a745; font-weight: bold; }");
            html.append(".scenario-table .warning-cell { color: #ffc107; font-weight: bold; }");
            html.append(".scenario-table .failed-cell { color: #dc3545; font-weight: bold; }");

            // Summary boxes
            html.append(".summary-boxes { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }");
            html.append(".summary-box { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 25px; border-radius: 10px; text-align: center; }");
            html.append(".summary-box .label { font-size: 14px; opacity: 0.9; margin-bottom: 10px; }");
            html.append(".summary-box .value { font-size: 36px; font-weight: bold; }");

            html.append("</style>");
            html.append("</head><body>");

            html.append("<div class='container'>");

            // Header with overall pass/fail status
            html.append(String.format("<div class='header %s'>", overallPassed ? "passed" : "failed"));
            html.append("<h1>Performance Test Report</h1>");
            html.append(String.format("<div class='status'>%s</div>", overallPassed ? "✅" : "❌"));
            html.append(String.format("<div class='message'>Performance Test %s</div>", overallPassed ? "PASSED" : "FAILED"));
            html.append(String.format("<div class='score'>%d of %d metrics passed threshold requirements</div>", passedCount, totalMetrics));
            html.append("</div>");

            // Summary Section
            html.append("<div class='section'>");
            html.append("<h2 class='section-title'>Test Summary</h2>");
            html.append("<div class='summary-boxes'>");

            html.append(String.format(
                "<div class='summary-box'><div class='label'>Total Scenarios</div><div class='value'>%s</div></div>",
                stats.get("totalScenarios")
            ));

            html.append(String.format(
                "<div class='summary-box'><div class='label'>Total Steps</div><div class='value'>%s</div></div>",
                stats.get("totalSteps")
            ));

            double cacheRate = averages.get("totalSteps") > 0
                ? (averages.get("cachedSteps") / averages.get("totalSteps")) * 100
                : 0;
            html.append(String.format(
                "<div class='summary-box'><div class='label'>Cache Hit Rate</div><div class='value'>%.1f%%</div></div>",
                cacheRate
            ));

            html.append(String.format(
                "<div class='summary-box'><div class='label'>Metrics Passed</div><div class='value'>%d/%d</div></div>",
                passedCount, totalMetrics
            ));

            html.append("</div></div>");

            // Detailed Metrics Section
            html.append("<div class='section'>");
            html.append("<h2 class='section-title'>Performance Metrics Details</h2>");
            html.append("<div class='metrics-grid'>");

            // Page Load Time
            addMetricCard(html, "Page Load Time", "📄",
                averages.get("avgPageLoadTime").longValue(),
                PAGE_LOAD_GOOD, PAGE_LOAD_POOR);

            // DOM Ready Time
            addMetricCard(html, "DOM Ready Time", "🔄",
                averages.get("avgDomReadyTime").longValue(),
                DOM_READY_GOOD, DOM_READY_POOR);

            // Response Time
            addMetricCard(html, "Response Time", "📡",
                averages.get("avgResponseTime").longValue(),
                RESPONSE_GOOD, RESPONSE_POOR);

            // TTFB
            addMetricCard(html, "Time To First Byte", "⏱️",
                averages.get("avgTtfb").longValue(),
                TTFB_GOOD, TTFB_POOR);

            // Connect Time
            addMetricCard(html, "Connection Time", "🔌",
                averages.get("avgConnectTime").longValue(),
                CONNECT_GOOD, CONNECT_POOR);

            // DNS Lookup
            addMetricCard(html, "DNS Lookup Time", "🌐",
                averages.get("avgDomainLookupTime").longValue(),
                DNS_GOOD, DNS_POOR);

            html.append("</div></div>");

            // Scenario-wise Performance Section
            addScenarioWiseSummary(html);

            html.append("</div>");
            html.append("</body></html>");

            // Write to file
            File htmlFile = new File("target/allure-results/suite-performance-summary.html");
            htmlFile.getParentFile().mkdirs();
            Files.write(Paths.get(htmlFile.toURI()), html.toString().getBytes(StandardCharsets.UTF_8));

            System.out.println("✅ Detailed HTML report created: " + htmlFile.getAbsolutePath());
            System.out.println("   Overall Status: " + (overallPassed ? "PASSED ✅" : "FAILED ❌"));
            System.out.println("   Metrics Passed: " + passedCount + "/" + totalMetrics);

        } catch (IOException e) {
            System.err.println("❌ Error creating detailed report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addMetricCard(StringBuilder html, String name, String icon,
                                     long value, long goodThreshold, long poorThreshold) {
        String status = getMetricStatus(value, goodThreshold, poorThreshold);
        String statusClass = status.equals("PASSED") ? "passed" : (status.equals("WARNING") ? "warning" : "failed");
        String statusEmoji = status.equals("PASSED") ? "✅" : (status.equals("WARNING") ? "⚡" : "❌");

        html.append(String.format("<div class='metric-card %s'>", statusClass));
        html.append("<div class='metric-header'>");
        html.append(String.format("<span class='metric-name'>%s %s</span>", icon, name));
        html.append(String.format("<span class='metric-status'>%s</span>", statusEmoji));
        html.append("</div>");
        html.append(String.format("<div class='metric-value'>%d ms</div>", value));
        html.append(String.format("<div class='status-badge %s'>%s</div>", statusClass, status));
        html.append("<div class='threshold-info'>");
        html.append("<div class='threshold-row'>");
        html.append("<span class='threshold-label'>Good (Passed):</span>");
        html.append(String.format("<span class='threshold-value'>≤ %d ms</span>", goodThreshold));
        html.append("</div>");
        html.append("<div class='threshold-row'>");
        html.append("<span class='threshold-label'>Acceptable (Warning):</span>");
        html.append(String.format("<span class='threshold-value'>%d - %d ms</span>", goodThreshold + 1, poorThreshold));
        html.append("</div>");
        html.append("<div class='threshold-row'>");
        html.append("<span class='threshold-label'>Poor (Failed):</span>");
        html.append(String.format("<span class='threshold-value'>> %d ms</span>", poorThreshold));
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");
    }

    private static void addScenarioWiseSummary(StringBuilder html) {
        List<PerformanceMetrics> allMetrics = PerformanceStorage.getAllMetrics();

        // Group by scenario
        Map<String, List<PerformanceMetrics>> byScenario = new LinkedHashMap<>();
        for (PerformanceMetrics metric : allMetrics) {
            byScenario.computeIfAbsent(metric.getScenarioName(), k -> new ArrayList<>()).add(metric);
        }

        if (byScenario.isEmpty()) {
            return;
        }

        html.append("<div class='section'>");
        html.append("<h2 class='section-title'>Scenario-wise Performance Summary</h2>");
        html.append("<table class='scenario-table'>");
        html.append("<thead><tr>");
        html.append("<th>Scenario</th>");
        html.append("<th>Steps</th>");
        html.append("<th>Page Load</th>");
        html.append("<th>DOM Ready</th>");
        html.append("<th>Response</th>");
        html.append("<th>TTFB</th>");
        html.append("<th>Connect</th>");
        html.append("<th>DNS</th>");
        html.append("<th>Status</th>");
        html.append("</tr></thead>");
        html.append("<tbody>");

        for (Map.Entry<String, List<PerformanceMetrics>> entry : byScenario.entrySet()) {
            String scenarioName = entry.getKey();
            List<PerformanceMetrics> metrics = entry.getValue();

            // Calculate averages for this scenario
            long avgPageLoad = (long) metrics.stream().mapToLong(PerformanceMetrics::getPageLoadTime).average().orElse(0);
            long avgDomReady = (long) metrics.stream().mapToLong(PerformanceMetrics::getDomReadyTime).average().orElse(0);
            long avgResponse = (long) metrics.stream().mapToLong(PerformanceMetrics::getResponseTime).average().orElse(0);
            long avgTtfb = (long) metrics.stream().mapToLong(PerformanceMetrics::getTtfb).average().orElse(0);
            long avgConnect = (long) metrics.stream().mapToLong(PerformanceMetrics::getConnectTime).average().orElse(0);
            long avgDns = (long) metrics.stream().mapToLong(PerformanceMetrics::getDomainLookupTime).average().orElse(0);

            // Determine scenario status
            int passedCount = 0;
            if (avgPageLoad <= PAGE_LOAD_GOOD) passedCount++;
            if (avgDomReady <= DOM_READY_GOOD) passedCount++;
            if (avgResponse <= RESPONSE_GOOD) passedCount++;
            if (avgTtfb <= TTFB_GOOD) passedCount++;
            if (avgConnect <= CONNECT_GOOD) passedCount++;
            if (avgDns <= DNS_GOOD) passedCount++;

            String scenarioStatus = passedCount >= 4 ? "PASSED ✅" : "FAILED ❌";
            String rowClass = passedCount >= 4 ? "passed-cell" : "failed-cell";

            html.append("<tr>");
            html.append(String.format("<td class='scenario-name'>%s</td>", scenarioName));
            html.append(String.format("<td class='metric-cell'>%d</td>", metrics.size()));
            html.append(formatScenarioMetricCell(avgPageLoad, PAGE_LOAD_GOOD, PAGE_LOAD_POOR));
            html.append(formatScenarioMetricCell(avgDomReady, DOM_READY_GOOD, DOM_READY_POOR));
            html.append(formatScenarioMetricCell(avgResponse, RESPONSE_GOOD, RESPONSE_POOR));
            html.append(formatScenarioMetricCell(avgTtfb, TTFB_GOOD, TTFB_POOR));
            html.append(formatScenarioMetricCell(avgConnect, CONNECT_GOOD, CONNECT_POOR));
            html.append(formatScenarioMetricCell(avgDns, DNS_GOOD, DNS_POOR));
            html.append(String.format("<td class='metric-cell %s'>%s</td>", rowClass, scenarioStatus));
            html.append("</tr>");
        }

        html.append("</tbody></table>");
        html.append("</div>");
    }

    private static String formatScenarioMetricCell(long value, long goodThreshold, long poorThreshold) {
        String status = getMetricStatus(value, goodThreshold, poorThreshold);
        String cellClass = status.equals("PASSED") ? "passed-cell" : (status.equals("WARNING") ? "warning-cell" : "failed-cell");
        String emoji = status.equals("PASSED") ? "✅" : (status.equals("WARNING") ? "⚡" : "❌");
        return String.format("<td class='metric-cell %s'>%s %d ms</td>", cellClass, emoji, value);
    }

    private static String getMetricStatus(long value, long goodThreshold, long poorThreshold) {
        if (value <= goodThreshold) {
            return "PASSED";
        } else if (value <= poorThreshold) {
            return "WARNING";
        } else {
            return "FAILED";
        }
    }

    private static boolean calculateOverallPass(Map<String, Double> averages) {
        int passedCount = countPassedMetrics(averages);
        // Pass if at least 4 out of 6 metrics meet the good threshold
        return passedCount >= 4;
    }

    private static int countPassedMetrics(Map<String, Double> averages) {
        int count = 0;
        if (averages.get("avgPageLoadTime") <= PAGE_LOAD_GOOD) count++;
        if (averages.get("avgDomReadyTime") <= DOM_READY_GOOD) count++;
        if (averages.get("avgResponseTime") <= RESPONSE_GOOD) count++;
        if (averages.get("avgTtfb") <= TTFB_GOOD) count++;
        if (averages.get("avgConnectTime") <= CONNECT_GOOD) count++;
        if (averages.get("avgDomainLookupTime") <= DNS_GOOD) count++;
        return count;
    }
}
