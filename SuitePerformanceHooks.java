package com.yourcompany.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.yourcompany.utils.PerformanceStorage;
import io.cucumber.java.AfterAll;
import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates suite-level performance widget for Allure overview page
 */
public class SuitePerformanceHooks {
    
    @AfterAll
    public static void afterAllTests() {
        System.out.println("🔄 Generating suite performance summary...");
        
        Map<String, Double> averages = PerformanceStorage.calculateSuiteAverages();
        Map<String, Object> stats = PerformanceStorage.getStatistics();
        
        if (!averages.isEmpty()) {
            
            // Export all metrics to JSON for analysis
            PerformanceStorage.exportToJson("target/allure-results/performance-metrics.json");
            
            // Create suite summary attachment
            String summary = createSuiteSummary(averages, stats);
            Allure.addAttachment(
                "🏆 Suite Performance Summary",
                "text/plain",
                new ByteArrayInputStream(summary.getBytes(StandardCharsets.UTF_8)),
                ".txt"
            );
            
            // Generate custom Allure widget data
            generateAllureWidget(averages, stats);
            
            System.out.println("✅ Suite performance summary generated");
            System.out.println("   Total Steps: " + stats.get("totalSteps"));
            System.out.println("   Total Scenarios: " + stats.get("totalScenarios"));
            System.out.println("   Avg Page Load: " + String.format("%.0f ms", averages.get("avgPageLoadTime")));
        }
    }
    
    private static String createSuiteSummary(Map<String, Double> averages, Map<String, Object> stats) {
        return String.format(
            "╔══════════════════════════════════════════════════════════════════╗\n" +
            "║              TEST SUITE PERFORMANCE SUMMARY                      ║\n" +
            "╠══════════════════════════════════════════════════════════════════╣\n" +
            "║  Total Scenarios:        %-41s ║\n" +
            "║  Total Steps:            %-41s ║\n" +
            "║  Cached Steps:           %-41.0f ║\n" +
            "╠══════════════════════════════════════════════════════════════════╣\n" +
            "║  SUITE-WIDE AVERAGE METRICS                                      ║\n" +
            "╠══════════════════════════════════════════════════════════════════╣\n" +
            "║  📄 Avg Page Load Time:  %-35.0f ms    ║\n" +
            "║  🔄 Avg DOM Ready Time:  %-35.0f ms    ║\n" +
            "║  📡 Avg Response Time:   %-35.0f ms    ║\n" +
            "║  ⏱️  Avg TTFB:            %-35.0f ms    ║\n" +
            "║  🔌 Avg Connect Time:    %-35.0f ms    ║\n" +
            "║  🌐 Avg DNS Lookup:      %-35.0f ms    ║\n" +
            "╠══════════════════════════════════════════════════════════════════╣\n" +
            "║  CACHE EFFICIENCY                                                ║\n" +
            "╠══════════════════════════════════════════════════════════════════╣\n" +
            "║  Cache Hit Rate:         %-35.1f%%     ║\n" +
            "╚══════════════════════════════════════════════════════════════════╝",
            stats.get("totalScenarios"),
            stats.get("totalSteps"),
            averages.get("cachedSteps"),
            averages.get("avgPageLoadTime"),
            averages.get("avgDomReadyTime"),
            averages.get("avgResponseTime"),
            averages.get("avgTtfb"),
            averages.get("avgConnectTime"),
            averages.get("avgDomainLookupTime"),
            (averages.get("cachedSteps") / averages.get("totalSteps")) * 100
        );
    }
    
    private static void generateAllureWidget(Map<String, Double> averages, Map<String, Object> stats) {
        try {
            // Create widget data structure
            Map<String, Object> widgetData = new HashMap<>();
            widgetData.put("name", "performance");
            widgetData.put("averages", averages);
            widgetData.put("stats", stats);
            widgetData.put("cacheHitRate", (averages.get("cachedSteps") / averages.get("totalSteps")) * 100);
            
            // Write to allure-results directory
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            String allureResultsDir = "target/allure-results";
            new File(allureResultsDir).mkdirs();
            
            File widgetFile = new File(allureResultsDir + "/widgets/performance-widget.json");
            widgetFile.getParentFile().mkdirs();
            
            mapper.writeValue(widgetFile, widgetData);
            
            // Also create the widget HTML template
            createWidgetHtml(allureResultsDir, averages, stats);
            
            System.out.println("✅ Allure widget data generated: " + widgetFile.getAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("❌ Error generating Allure widget: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createWidgetHtml(String allureResultsDir, Map<String, Double> averages, Map<String, Object> stats) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<div class='widget' style='padding:20px; background:linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius:8px; color:white;'>\n");
            html.append("  <h3 style='margin-top:0;'>⚡ Performance Summary</h3>\n");
            html.append("  <div style='display:grid; grid-template-columns:repeat(3,1fr); gap:10px;'>\n");
            
            html.append(String.format(
                "    <div style='text-align:center;'><div style='font-size:24px; font-weight:bold;'>%.0f ms</div><div style='font-size:11px; opacity:0.9;'>AVG PAGE LOAD</div></div>\n",
                averages.get("avgPageLoadTime")
            ));
            
            html.append(String.format(
                "    <div style='text-align:center;'><div style='font-size:24px; font-weight:bold;'>%.0f ms</div><div style='font-size:11px; opacity:0.9;'>AVG TTFB</div></div>\n",
                averages.get("avgTtfb")
            ));
            
            html.append(String.format(
                "    <div style='text-align:center;'><div style='font-size:24px; font-weight:bold;'>%s</div><div style='font-size:11px; opacity:0.9;'>TOTAL STEPS</div></div>\n",
                stats.get("totalSteps")
            ));
            
            html.append("  </div>\n");
            html.append("</div>\n");
            
            // Write widget HTML
            File htmlFile = new File(allureResultsDir + "/widgets/performance-widget.html");
            Files.write(Paths.get(htmlFile.toURI()), html.toString().getBytes(StandardCharsets.UTF_8));
            
            System.out.println("✅ Allure widget HTML generated: " + htmlFile.getAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("❌ Error creating widget HTML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}