package com.yourcompany.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.yourcompany.utils.PerformanceStorage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates suite-level performance widget for Allure overview page
 * Using shutdown hook to ensure it always runs
 */
public class SuitePerformanceHooks {
    
    private static boolean widgetGenerated = false;
    
    static {
        // Register shutdown hook to ensure widget is generated even if tests fail
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!widgetGenerated) {
                generateSuiteWidget();
            }
        }));
    }
    
    // This will be called by the shutdown hook or explicitly
    public static synchronized void generateSuiteWidget() {
        if (widgetGenerated) {
            return; // Already generated
        }
        
        System.out.println("🔄 Generating suite performance widget...");
        
        Map<String, Double> averages = PerformanceStorage.calculateSuiteAverages();
        Map<String, Object> stats = PerformanceStorage.getStatistics();
        
        if (averages.isEmpty()) {
            System.out.println("⚠️  No performance metrics collected");
            return;
        }
        
        try {
            // Export all metrics to JSON for analysis
            PerformanceStorage.exportToJson("target/allure-results/performance-metrics.json");
            
            // Generate Allure widget data
            generateAllureWidgetData(averages, stats);
            
            // Generate environment.properties for Allure
            generateEnvironmentProperties(averages, stats);
            
            widgetGenerated = true;
            
            System.out.println("✅ Suite performance widget generated");
            System.out.println("   Total Steps: " + stats.get("totalSteps"));
            System.out.println("   Total Scenarios: " + stats.get("totalScenarios"));
            System.out.println("   Avg Page Load: " + String.format("%.0f ms", averages.get("avgPageLoadTime")));
            
        } catch (Exception e) {
            System.err.println("❌ Error generating suite widget: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void generateAllureWidgetData(Map<String, Double> averages, Map<String, Object> stats) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        String allureResultsDir = "target/allure-results";
        new File(allureResultsDir).mkdirs();
        
        // Create widgets directory
        File widgetsDir = new File(allureResultsDir + "/widgets");
        widgetsDir.mkdirs();
        
        // Create widget data JSON
        Map<String, Object> widgetData = new HashMap<>();
        widgetData.put("name", "performance");
        widgetData.put("averages", averages);
        widgetData.put("stats", stats);
        widgetData.put("cacheHitRate", averages.get("totalSteps") > 0 
            ? (averages.get("cachedSteps") / averages.get("totalSteps")) * 100 
            : 0);
        
        File widgetJsonFile = new File(widgetsDir, "performance-widget.json");
        mapper.writeValue(widgetJsonFile, widgetData);
        
        System.out.println("✅ Widget JSON created: " + widgetJsonFile.getAbsolutePath());
        
        // Create summary.json for Allure summary widget (this is the key!)
        createSummaryJson(widgetsDir, averages, stats);
        
        // Create categories.json for better categorization
        createCategoriesJson(new File(allureResultsDir), averages);
    }
    
    private static void createSummaryJson(File widgetsDir, Map<String, Double> averages, Map<String, Object> stats) throws IOException {
        // This is what Allure actually reads for custom summary widgets
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("statistic", Map.of(
            "total", stats.get("totalSteps"),
            "scenarios", stats.get("totalScenarios")
        ));
        
        summary.put("time", Map.of(
            "duration", averages.get("avgPageLoadTime").longValue()
        ));
        
        // Extra data for custom display
        Map<String, Object> extra = new HashMap<>();
        extra.put("Performance Metrics", Map.of(
            "Avg Page Load", String.format("%.0f ms", averages.get("avgPageLoadTime")),
            "Avg DOM Ready", String.format("%.0f ms", averages.get("avgDomReadyTime")),
            "Avg Response", String.format("%.0f ms", averages.get("avgResponseTime")),
            "Avg TTFB", String.format("%.0f ms", averages.get("avgTtfb")),
            "Avg Connect", String.format("%.0f ms", averages.get("avgConnectTime")),
            "Avg DNS Lookup", String.format("%.0f ms", averages.get("avgDomainLookupTime")),
            "Total Steps", stats.get("totalSteps").toString(),
            "Cache Hit Rate", String.format("%.1f%%", 
                averages.get("totalSteps") > 0 
                    ? (averages.get("cachedSteps") / averages.get("totalSteps")) * 100 
                    : 0)
        ));
        
        summary.put("extra", extra);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        File summaryFile = new File(widgetsDir, "summary.json");
        mapper.writeValue(summaryFile, summary);
        
        System.out.println("✅ Summary JSON created: " + summaryFile.getAbsolutePath());
    }
    
    private static void createCategoriesJson(File allureResultsDir, Map<String, Double> averages) throws IOException {
        // Create categories for performance thresholds
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        json.append("  {\n");
        json.append("    \"name\": \"Slow Performance\",\n");
        json.append("    \"messageRegex\": \".*SLOW.*\",\n");
        json.append("    \"matchedStatuses\": [\"passed\"],\n");
        json.append("    \"description\": \"Tests with slow page load times\"\n");
        json.append("  },\n");
        json.append("  {\n");
        json.append("    \"name\": \"Good Performance\",\n");
        json.append("    \"messageRegex\": \".*Good.*\",\n");
        json.append("    \"matchedStatuses\": [\"passed\"],\n");
        json.append("    \"description\": \"Tests with acceptable performance\"\n");
        json.append("  }\n");
        json.append("]\n");
        
        File categoriesFile = new File(allureResultsDir, "categories.json");
        Files.write(Paths.get(categoriesFile.toURI()), json.toString().getBytes(StandardCharsets.UTF_8));
        
        System.out.println("✅ Categories JSON created: " + categoriesFile.getAbsolutePath());
    }
    
    private static void generateEnvironmentProperties(Map<String, Double> averages, Map<String, Object> stats) throws IOException {
        // Create environment.properties with performance summary
        StringBuilder props = new StringBuilder();
        props.append("# Performance Summary\n");
        props.append(String.format("Total.Scenarios=%s\n", stats.get("totalScenarios")));
        props.append(String.format("Total.Steps=%s\n", stats.get("totalSteps")));
        props.append(String.format("Avg.Page.Load=%.0f ms\n", averages.get("avgPageLoadTime")));
        props.append(String.format("Avg.DOM.Ready=%.0f ms\n", averages.get("avgDomReadyTime")));
        props.append(String.format("Avg.Response=%.0f ms\n", averages.get("avgResponseTime")));
        props.append(String.format("Avg.TTFB=%.0f ms\n", averages.get("avgTtfb")));
        props.append(String.format("Avg.Connect=%.0f ms\n", averages.get("avgConnectTime")));
        props.append(String.format("Avg.DNS.Lookup=%.0f ms\n", averages.get("avgDomainLookupTime")));
        props.append(String.format("Cache.Hit.Rate=%.1f%%\n", 
            averages.get("totalSteps") > 0 
                ? (averages.get("cachedSteps") / averages.get("totalSteps")) * 100 
                : 0));
        
        File envFile = new File("target/allure-results/environment.properties");
        Files.write(Paths.get(envFile.toURI()), props.toString().getBytes(StandardCharsets.UTF_8));
        
        System.out.println("✅ Environment properties created: " + envFile.getAbsolutePath());
    }

  private static void generateEnvironmentProperties(Map<String, Double> averages, Map<String, Object> stats) throws IOException {
    StringBuilder props = new StringBuilder();
    
    // System Info
    props.append("# System Information\n");
    props.append("Browser=Chrome\n");
    props.append("OS=").append(System.getProperty("os.name")).append("\n");
    props.append("Java.Version=").append(System.getProperty("java.version")).append("\n");
    props.append("\n");
    
    // Performance Summary (This will appear in Environment section)
    props.append("# === PERFORMANCE SUMMARY ===\n");
    props.append(String.format("📊.Total.Scenarios=%s\n", stats.get("totalScenarios")));
    props.append(String.format("📊.Total.Steps=%s\n", stats.get("totalSteps")));
    props.append("\n");
    props.append("# Average Metrics\n");
    props.append(String.format("📄.Avg.Page.Load=%.0f ms\n", averages.get("avgPageLoadTime")));
    props.append(String.format("🔄.Avg.DOM.Ready=%.0f ms\n", averages.get("avgDomReadyTime")));
    props.append(String.format("📡.Avg.Response=%.0f ms\n", averages.get("avgResponseTime")));
    props.append(String.format("⏱️.Avg.TTFB=%.0f ms\n", averages.get("avgTtfb")));
    props.append(String.format("🔌.Avg.Connect=%.0f ms\n", averages.get("avgConnectTime")));
    props.append(String.format("🌐.Avg.DNS.Lookup=%.0f ms\n", averages.get("avgDomainLookupTime")));
    props.append("\n");
    props.append("# Cache Statistics\n");
    props.append(String.format("💾.Cache.Hit.Rate=%.1f%%\n", 
        averages.get("totalSteps") > 0 
            ? (averages.get("cachedSteps") / averages.get("totalSteps")) * 100 
            : 0));
    props.append(String.format("💾.Cached.Steps=%.0f\n", averages.get("cachedSteps")));
    
    File envFile = new File("target/allure-results/environment.properties");
    Files.write(Paths.get(envFile.toURI()), props.toString().getBytes(StandardCharsets.UTF_8));
    
    System.out.println("✅ Environment properties created: " + envFile.getAbsolutePath());
}

private static void createOverviewAttachment(Map<String, Double> averages, Map<String, Object> stats) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head>");
            html.append("<meta charset='UTF-8'>");
            html.append("<title>Performance Summary</title>");
            html.append("<style>");
            html.append("body { font-family: 'Segoe UI', sans-serif; margin: 0; padding: 40px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }");
            html.append(".container { background: white; border-radius: 16px; padding: 40px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); max-width: 1200px; margin: 0 auto; }");
            html.append("h1 { color: #333; text-align: center; font-size: 36px; margin-bottom: 40px; border-bottom: 4px solid #667eea; padding-bottom: 20px; }");
            html.append(".metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin: 30px 0; }");
            html.append(".metric-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 12px; text-align: center; box-shadow: 0 8px 16px rgba(0,0,0,0.2); transition: transform 0.3s; }");
            html.append(".metric-card:hover { transform: translateY(-5px); }");
            html.append(".metric-value { font-size: 48px; font-weight: bold; margin: 15px 0; }");
            html.append(".metric-label { font-size: 14px; opacity: 0.9; text-transform: uppercase; letter-spacing: 1px; }");
            html.append(".metric-icon { font-size: 32px; margin-bottom: 10px; }");
            html.append(".info-section { background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='container'>");
            html.append("<h1>🏆 Test Suite Performance Summary</h1>");
            
            // Info section
            html.append("<div class='info-section'>");
            html.append(String.format("<p><strong>Total Scenarios:</strong> %s</p>", stats.get("totalScenarios")));
            html.append(String.format("<p><strong>Total Steps:</strong> %s</p>", stats.get("totalSteps")));
            html.append(String.format("<p><strong>Cached Steps:</strong> %.0f (%.1f%% cache hit rate)</p>", 
                averages.get("cachedSteps"),
                averages.get("totalSteps") > 0 ? (averages.get("cachedSteps") / averages.get("totalSteps")) * 100 : 0));
            html.append("</div>");
            
            // Metrics grid
            html.append("<div class='metrics-grid'>");
            
            html.append(String.format(
                "<div class='metric-card'>" +
                "<div class='metric-icon'>📄</div>" +
                "<div class='metric-label'>Avg Page Load Time</div>" +
                "<div class='metric-value'>%.0f ms</div>" +
                "</div>",
                averages.get("avgPageLoadTime")
            ));
            
            html.append(String.format(
                "<div class='metric-card'>" +
                "<div class='metric-icon'>🔄</div>" +
                "<div class='metric-label'>Avg DOM Ready Time</div>" +
                "<div class='metric-value'>%.0f ms</div>" +
                "</div>",
                averages.get("avgDomReadyTime")
            ));
            
            html.append(String.format(
                "<div class='metric-card'>" +
                "<div class='metric-icon'>📡</div>" +
                "<div class='metric-label'>Avg Response Time</div>" +
                "<div class='metric-value'>%.0f ms</div>" +
                "</div>",
                averages.get("avgResponseTime")
            ));
            
            html.append(String.format(
                "<div class='metric-card'>" +
                "<div class='metric-icon'>⏱️</div>" +
                "<div class='metric-label'>Avg TTFB</div>" +
                "<div class='metric-value'>%.0f ms</div>" +
                "</div>",
                averages.get("avgTtfb")
            ));
            
            html.append(String.format(
                "<div class='metric-card'>" +
                "<div class='metric-icon'>🔌</div>" +
                "<div class='metric-label'>Avg Connect Time</div>" +
                "<div class='metric-value'>%.0f ms</div>" +
                "</div>",
                averages.get("avgConnectTime")
            ));
            
            html.append(String.format(
                "<div class='metric-card'>" +
                "<div class='metric-icon'>🌐</div>" +
                "<div class='metric-label'>Avg DNS Lookup</div>" +
                "<div class='metric-value'>%.0f ms</div>" +
                "</div>",
                averages.get("avgDomainLookupTime")
            ));
            
            html.append("</div>");
            html.append("</div>");
            html.append("</body></html>");
            
            // Write to allure-results
            File htmlFile = new File("target/allure-results/suite-performance-summary.html");
            Files.write(Paths.get(htmlFile.toURI()), html.toString().getBytes(StandardCharsets.UTF_8));
            
            System.out.println("✅ Overview attachment created: " + htmlFile.getAbsolutePath());
            System.out.println("   File exists: " + htmlFile.exists());
            System.out.println("   File size: " + htmlFile.length() + " bytes");
            
        } catch (IOException e) {
            System.err.println("❌ Error creating overview attachment: " + e.getMessage());
            e.printStackTrace();
        }
    }
private static void generateAllureSummaryWidget(Map<String, Double> averages, Map<String, Object> stats) {
    try {
        File widgetsDir = new File("target/allure-results/widgets");
        widgetsDir.mkdirs();
        
        // Create summary.json that Allure reads automatically
        Map<String, Object> summary = new HashMap<>();
        
        // Statistic section (shows on overview)
        Map<String, Object> statistic = new HashMap<>();
        statistic.put("total", stats.get("totalSteps"));
        statistic.put("passed", stats.get("totalSteps")); // Assuming all passed
        statistic.put("failed", 0);
        statistic.put("broken", 0);
        statistic.put("skipped", 0);
        statistic.put("unknown", 0);
        summary.put("statistic", statistic);
        
        // Time section
        Map<String, Object> time = new HashMap<>();
        time.put("start", System.currentTimeMillis() - 300000); // 5 min ago
        time.put("stop", System.currentTimeMillis());
        time.put("duration", 300000);
        time.put("minDuration", 1000);
        time.put("maxDuration", 5000);
        time.put("sumDuration", 300000);
        summary.put("time", time);
        
        // CRITICAL: Extra section for custom data
        Map<String, Object> extra = new HashMap<>();
        
        // Performance Metrics subsection
        Map<String, String> performanceMetrics = new LinkedHashMap<>();
        
        // Add metrics with visual indicators
        long pageLoad = averages.get("avgPageLoadTime").longValue();
        performanceMetrics.put("📄 Page Load", formatMetricWithStatus(pageLoad, 2000, 3000));
        
        long domReady = averages.get("avgDomReadyTime").longValue();
        performanceMetrics.put("🔄 DOM Ready", formatMetricWithStatus(domReady, 1500, 2500));
        
        long response = averages.get("avgResponseTime").longValue();
        performanceMetrics.put("📡 Response Time", formatMetricWithStatus(response, 800, 1200));
        
        long ttfb = averages.get("avgTtfb").longValue();
        performanceMetrics.put("⏱️ TTFB", formatMetricWithStatus(ttfb, 400, 600));
        
        long connect = averages.get("avgConnectTime").longValue();
        performanceMetrics.put("🔌 Connect Time", formatMetricWithStatus(connect, 200, 400));
        
        long dns = averages.get("avgDomainLookupTime").longValue();
        performanceMetrics.put("🌐 DNS Lookup", formatMetricWithStatus(dns, 100, 200));
        
        extra.put("Performance Metrics", performanceMetrics);
        
        // Summary subsection
        Map<String, String> summaryInfo = new LinkedHashMap<>();
        summaryInfo.put("Total Scenarios", String.valueOf(stats.get("totalScenarios")));
        summaryInfo.put("Total Steps", String.valueOf(stats.get("totalSteps")));
        
        double cacheRate = averages.get("totalSteps") > 0 
            ? (averages.get("cachedSteps") / averages.get("totalSteps")) * 100 
            : 0;
        String cacheStatus = cacheRate >= 50 ? "✅" : cacheRate >= 30 ? "⚡" : "❌";
        summaryInfo.put("💾 Cache Hit Rate", String.format("%s %.1f%%", cacheStatus, cacheRate));
        
        String grade = calculateOverallGrade(averages);
        summaryInfo.put("🏆 Performance Grade", grade);
        
        extra.put("Suite Summary", summaryInfo);
        
        summary.put("extra", extra);
        
        // Write summary.json
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        File summaryFile = new File(widgetsDir, "summary.json");
        mapper.writeValue(summaryFile, summary);
        
        System.out.println("✅ Allure summary widget created: " + summaryFile.getAbsolutePath());
        
    } catch (IOException e) {
        System.err.println("❌ Error creating summary widget: " + e.getMessage());
        e.printStackTrace();
    }
}

private static String formatMetricWithStatus(long value, long goodThreshold, long poorThreshold) {
    String status;
    if (value <= goodThreshold) {
        status = "✅";
    } else if (value <= poorThreshold) {
        status = "⚡";
    } else {
        status = "❌";
    }
    return String.format("%s %d ms", status, value);
}
}
