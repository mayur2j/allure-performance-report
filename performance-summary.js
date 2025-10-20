allure.api.addWidget('performance-summary', 'Performance Summary', function() {
    const container = document.createElement('div');
    container.className = 'widget';
    container.style.cssText = 'padding:20px; background:linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius:8px; color:white; margin-bottom:20px;';
    
    // Fetch performance data
    fetch('widgets/performance-widget.json')
        .then(response => response.json())
        .then(data => {
            const averages = data.averages;
            const stats = data.stats;
            const cacheHitRate = data.cacheHitRate;
            
            container.innerHTML = `
                <h3 style="margin-top:0; color:white;">⚡ Performance Summary</h3>
                <div style="display:grid; grid-template-columns:repeat(3,1fr); gap:15px; margin-top:20px;">
                    <div style="text-align:center;">
                        <div style="font-size:32px; font-weight:bold;">${Math.round(averages.avgPageLoadTime)} ms</div>
                        <div style="font-size:12px; opacity:0.9; text-transform:uppercase;">Avg Page Load</div>
                    </div>
                    <div style="text-align:center;">
                        <div style="font-size:32px; font-weight:bold;">${Math.round(averages.avgDomReadyTime)} ms</div>
                        <div style="font-size:12px; opacity:0.9; text-transform:uppercase;">Avg DOM Ready</div>
                    </div>
                    <div style="text-align:center;">
                        <div style="font-size:32px; font-weight:bold;">${Math.round(averages.avgTtfb)} ms</div>
                        <div style="font-size:12px; opacity:0.9; text-transform:uppercase;">Avg TTFB</div>
                    </div>
                    <div style="text-align:center;">
                        <div style="font-size:32px; font-weight:bold;">${Math.round(averages.avgResponseTime)} ms</div>
                        <div style="font-size:12px; opacity:0.9; text-transform:uppercase;">Avg Response</div>
                    </div>
                    <div style="text-align:center;">
                        <div style="font-size:32px; font-weight:bold;">${stats.totalSteps}</div>
                        <div style="font-size:12px; opacity:0.9; text-transform:uppercase;">Total Steps</div>
                    </div>
                    <div style="text-align:center;">
                        <div style="font-size:32px; font-weight:bold;">${Math.round(cacheHitRate)}%</div>
                        <div style="font-size:12px; opacity:0.9; text-transform:uppercase;">Cache Hit Rate</div>
                    </div>
                </div>
                <div style="margin-top:20px; padding-top:15px; border-top:1px solid rgba(255,255,255,0.3);">
                    <div style="display:grid; grid-template-columns:repeat(2,1fr); gap:10px; font-size:13px;">
                        <div>🔌 Avg Connect: ${Math.round(averages.avgConnectTime)} ms</div>
                        <div>🌐 Avg DNS Lookup: ${Math.round(averages.avgDomainLookupTime)} ms</div>
                    </div>
                </div>
            `;
        })
        .catch(error => {
            container.innerHTML = '<p style="color:white;">Performance data not available</p>';
            console.error('Error loading performance widget:', error);
        });
    
    return container;
});