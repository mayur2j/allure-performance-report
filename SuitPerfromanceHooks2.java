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
}
