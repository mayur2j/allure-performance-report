package com.yourcompany.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Thread-safe storage for performance metrics across all tests
 */
public class PerformanceStorage {
    
    private static final Map<String, List<PerformanceMetrics>> scenarioMetrics = new ConcurrentHashMap<>();
    private static final List<PerformanceMetrics> allMetrics = new CopyOnWriteArrayList<>();
    
    /**
     * Add performance metrics for a step
     */
    public static void addMetrics(PerformanceMetrics metrics) {
        allMetrics.add(metrics);
        
        String scenarioKey = metrics.getScenarioName();
        scenarioMetrics.computeIfAbsent(scenarioKey, k -> new CopyOnWriteArrayList<>()).add(metrics);
    }
    
    /**
     * Get all metrics for a specific scenario
     */
    public static List<PerformanceMetrics> getScenarioMetrics(String scenarioName) {
        return scenarioMetrics.getOrDefault(scenarioName, Collections.emptyList());
    }
    
    /**
     * Get all metrics across all scenarios
     */
    public static List<PerformanceMetrics> getAllMetrics() {
        return new ArrayList<>(allMetrics);
    }
    
    /**
     * Calculate average metrics for entire suite
     */
    public static Map<String, Double> calculateSuiteAverages() {
        if (allMetrics.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, Double> averages = new HashMap<>();
        
        averages.put("avgPageLoadTime", allMetrics.stream()
            .mapToLong(PerformanceMetrics::getPageLoadTime)
            .average()
            .orElse(0.0));
        
        averages.put("avgDomReadyTime", allMetrics.stream()
            .mapToLong(PerformanceMetrics::getDomReadyTime)
            .average()
            .orElse(0.0));
        
        averages.put("avgResponseTime", allMetrics.stream()
            .mapToLong(PerformanceMetrics::getResponseTime)
            .average()
            .orElse(0.0));
        
        averages.put("avgTtfb", allMetrics.stream()
            .mapToLong(PerformanceMetrics::getTtfb)
            .average()
            .orElse(0.0));
        
        averages.put("avgConnectTime", allMetrics.stream()
            .mapToLong(PerformanceMetrics::getConnectTime)
            .average()
            .orElse(0.0));
        
        averages.put("avgDomainLookupTime", allMetrics.stream()
            .mapToLong(PerformanceMetrics::getDomainLookupTime)
            .average()
            .orElse(0.0));
        
        averages.put("totalSteps", (double) allMetrics.size());
        averages.put("cachedSteps", (double) allMetrics.stream().filter(PerformanceMetrics::isFromCache).count());
        
        return averages;
    }
    
    /**
     * Calculate average metrics for a specific scenario
     */
    public static Map<String, Double> calculateScenarioAverages(String scenarioName) {
        List<PerformanceMetrics> metrics = getScenarioMetrics(scenarioName);
        
        if (metrics.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, Double> averages = new HashMap<>();
        
        averages.put("avgPageLoadTime", metrics.stream()
            .mapToLong(PerformanceMetrics::getPageLoadTime)
            .average()
            .orElse(0.0));
        
        averages.put("avgDomReadyTime", metrics.stream()
            .mapToLong(PerformanceMetrics::getDomReadyTime)
            .average()
            .orElse(0.0));
        
        averages.put("avgResponseTime", metrics.stream()
            .mapToLong(PerformanceMetrics::getResponseTime)
            .average()
            .orElse(0.0));
        
        averages.put("avgTtfb", metrics.stream()
            .mapToLong(PerformanceMetrics::getTtfb)
            .average()
            .orElse(0.0));
        
        averages.put("avgConnectTime", metrics.stream()
            .mapToLong(PerformanceMetrics::getConnectTime)
            .average()
            .orElse(0.0));
        
        averages.put("avgDomainLookupTime", metrics.stream()
            .mapToLong(PerformanceMetrics::getDomainLookupTime)
            .average()
            .orElse(0.0));
        
        averages.put("totalSteps", (double) metrics.size());
        averages.put("cachedSteps", (double) metrics.stream().filter(PerformanceMetrics::isFromCache).count());
        
        return averages;
    }
    
    /**
     * Export all metrics to JSON file
     */
    public static void exportToJson(String outputPath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            
            Map<String, Object> export = new HashMap<>();
            export.put("totalMetrics", allMetrics.size());
            export.put("suiteAverages", calculateSuiteAverages());
            export.put("allMetrics", allMetrics);
            export.put("scenarioMetrics", scenarioMetrics);
            
            mapper.writeValue(outputFile, export);
            
            System.out.println(" Performance metrics exported to: " + outputPath);
            
        } catch (IOException e) {
            System.err.println(" Error exporting metrics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clear all stored metrics (useful for test cleanup)
     */
    public static void clear() {
        allMetrics.clear();
        scenarioMetrics.clear();
    }
    
    /**
     * Get statistics summary
     */
    public static Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSteps", allMetrics.size());
        stats.put("totalScenarios", scenarioMetrics.size());
        stats.put("averages", calculateSuiteAverages());
        
        return stats;
    }
}