# Performance Report Enhancements

## Overview
The HTML performance report has been enhanced with clear pass/fail indicators, color coding, threshold information, and scenario-wise summaries.

## Key Features

### 1. Overall Pass/Fail Status
- **Large header banner** at the top showing overall test status
- **Green background** with ✅ for PASSED tests
- **Red background** with ❌ for FAILED tests
- Shows count of metrics that passed threshold requirements

### 2. Clear Thresholds
Each metric has three threshold levels:
- **Good (Passed)**: ✅ Green - Performance meets target
- **Acceptable (Warning)**: ⚡ Yellow - Performance acceptable but could be better
- **Poor (Failed)**: ❌ Red - Performance needs improvement

### 3. Suite-wise Grouping (Top Level Collapsible)
- **Blue collapsible button** at the top level showing all suites
- Groups data by feature/suite/runner (e.g., User.class, Product.class)
- Shows total suite count and total scenario count
- Click to expand/collapse all suites

### 4. Scenario-wise Summary (Nested Collapsible)
- **Purple collapsible button** for each suite
- Shows suite name with scenario and step counts
- Table displaying all scenarios within that suite
- Each scenario row shows performance metrics and pass/fail status
- Click to expand/collapse individual suite's scenarios

### 5. Step-wise Details (Double Nested Collapsible)
- **Teal/cyan collapsible button** under each scenario
- Shows individual step performance metrics
- Includes step name, all 6 performance metrics, and cache status
- Color-coded metrics with emojis for each step
- Cache status shown with green "✓ Yes" or red "✗ No" badges

### 6. Performance Thresholds

| Metric | Good (Passed) | Acceptable (Warning) | Poor (Failed) |
|--------|--------------|---------------------|---------------|
| Page Load Time | ≤ 2000 ms | 2001-3000 ms | > 3000 ms |
| DOM Ready Time | ≤ 1500 ms | 1501-2500 ms | > 2500 ms |
| Response Time | ≤ 800 ms | 801-1200 ms | > 1200 ms |
| TTFB | ≤ 400 ms | 401-600 ms | > 600 ms |
| Connection Time | ≤ 200 ms | 201-400 ms | > 400 ms |
| DNS Lookup Time | ≤ 100 ms | 101-200 ms | > 200 ms |

### 7. Color-Coded Metric Cards
Each performance metric is displayed in a card with:
- **Border and background color** matching the status (green/yellow/red)
- **Large emoji** indicator (✅/⚡/❌)
- **Status badge** showing PASSED/WARNING/FAILED
- **Threshold information** showing all three levels
- **Hover effects** for better interactivity

### 8. Summary Statistics
Four key summary boxes displaying:
- Total Scenarios
- Total Steps
- Cache Hit Rate
- Metrics Passed (X/6)

## Overall Pass Criteria
The test suite is marked as **PASSED** if:
- At least **4 out of 6** metrics meet the "Good" threshold
- Otherwise marked as **FAILED**

Each scenario in the scenario-wise summary follows the same rule.

## Report Location
The enhanced report is generated at:
```
target/allure-results/suite-performance-summary.html
```

## Automatic Generation
The report is automatically generated via a shutdown hook when the test suite completes. No manual invocation is required.

## Hierarchical Structure

The report follows a clear three-level hierarchy:

```
Suite Level (Always Visible)
├── Overall Pass/Fail Header
├── Summary Statistics
├── Suite-level Metrics (6 cards)
│
└── 📊 Suite-wise Performance Summary (Collapsible - BLUE)
    │
    ├── 🎯 Suite 1 - User.class (Nested Collapsible - PURPLE)
    │   ├── Scenario 1 (table row with average metrics)
    │   │   └── 📝 Step-wise Details (Double Nested - TEAL)
    │   │       ├── Step 1 - individual metrics + cache
    │   │       ├── Step 2 - individual metrics + cache
    │   │       └── Step N - individual metrics + cache
    │   │
    │   ├── Scenario 2 (table row)
    │   │   └── 📝 Step-wise Details
    │   │       └── ...
    │   └── ...
    │
    ├── 🎯 Suite 2 - Product.class (Nested Collapsible - PURPLE)
    │   ├── Scenario 1 (table row)
    │   │   └── 📝 Step-wise Details
    │   │       └── ...
    │   └── ...
    │
    └── 🎯 Suite N - Other.class
        └── ...
```

## Interactive Features

### Collapsible Sections (Three Levels)
1. **Suite-wise Summary (Top Level)**: Click the blue button to expand/collapse all suites
2. **Individual Suite (Nested Level)**: Click each purple button to expand/collapse that suite's scenarios
3. **Step-wise Details (Double Nested)**: Click the teal button under each scenario to view individual steps
- **Visual Feedback**: Arrow rotates 180° when expanded at any level
- **Smooth Animation**: Content smoothly expands/collapses
- **Independent Control**: Each section can be expanded/collapsed independently

### Color Coding (Three-Level Hierarchy)
- **Top Level (All Suites)**: Blue gradient (#007bff to #0056b3)
- **Nested Level (Individual Suite)**: Purple gradient (#6f42c1 to #5a32a3)
- **Double Nested (Steps)**: Teal gradient (#17a2b8 to #117a8b)
- **Passed Metrics**: Green (#28a745)
- **Warning Metrics**: Yellow/Amber (#ffc107)
- **Failed Metrics**: Red (#dc3545)

## Visual Design
- **Modern, responsive design** with gradient backgrounds
- **Professional color scheme** using Bootstrap-inspired colors
- **Card-based layout** for metrics
- **Interactive hover effects**
- **Clear typography** with proper hierarchy
- **Collapsible sections** to manage large amounts of data

## Usage
Simply run your tests as usual. The detailed performance report will be automatically generated when the test suite completes. Open the HTML file in any web browser to view the comprehensive performance analysis.

### Navigating the Report
1. View the overall pass/fail status at the top
2. Review suite-level summary statistics
3. Examine the 6 key performance metric cards
4. Click the **blue** "Suite-wise Performance Summary" button to see all suites
5. Click individual **purple** suite buttons (e.g., "User.class") to see scenarios in that suite
6. Review scenario-level metrics in the table
7. Click **teal** "Step-wise Details" buttons to drill down to individual steps
8. Use color coding to quickly identify problem areas at any level
