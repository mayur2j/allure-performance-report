package com.yourcompany.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model for performance metrics
 */
public class PerformanceMetrics {
    
    // Step-level metrics
    private String stepName;
    private long pageLoadTime;
    private long domReadyTime;
    private long responseTime;
    private long ttfb;
    private long connectTime;
    private long domainLookupTime;
    private boolean fromCache;
    
    // Scenario information
    private String scenarioName;
    private String featureName;
    private long timestamp;
    
    public PerformanceMetrics() {
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    
    public long getPageLoadTime() { return pageLoadTime; }
    public void setPageLoadTime(long pageLoadTime) { this.pageLoadTime = pageLoadTime; }
    
    public long getDomReadyTime() { return domReadyTime; }
    public void setDomReadyTime(long domReadyTime) { this.domReadyTime = domReadyTime; }
    
    public long getResponseTime() { return responseTime; }
    public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
    
    public long getTtfb() { return ttfb; }
    public void setTtfb(long ttfb) { this.ttfb = ttfb; }
    
    public long getConnectTime() { return connectTime; }
    public void setConnectTime(long connectTime) { this.connectTime = connectTime; }
    
    public long getDomainLookupTime() { return domainLookupTime; }
    public void setDomainLookupTime(long domainLookupTime) { this.domainLookupTime = domainLookupTime; }
    
    public boolean isFromCache() { return fromCache; }
    public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
    
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    
    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return String.format(
            "PerformanceMetrics{step='%s', pageLoad=%d, domReady=%d, response=%d, ttfb=%d, connect=%d, domainLookup=%d, cache=%s}",
            stepName, pageLoadTime, domReadyTime, responseTime, ttfb, connectTime, domainLookupTime, fromCache
        );
    }
}