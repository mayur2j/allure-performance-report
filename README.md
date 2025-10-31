# Allure Performance Report

A comprehensive performance metrics tracking solution for Cucumber/Selenium tests that integrates with Allure Reports.

## Features

- **Step-level performance tracking**: Captures detailed performance metrics for each test step
- **Suite-level summary**: Generates overall performance statistics across all tests
- **Beautiful HTML dashboards**: Visual representation of performance data
- **Allure integration**: Seamless integration with Allure reporting
- **Selective tracking**: Skip performance tracking for slow operations using tags

## Performance Metrics Captured

- **Page Load Time**: Total time for page to load
- **DOM Ready Time**: Time until DOM is ready
- **Response Time**: Server response time
- **TTFB (Time To First Byte)**: Time to receive first byte from server
- **Connect Time**: TCP connection time
- **Domain Lookup Time**: DNS resolution time
- **Cache Status**: Whether resources were loaded from cache

## Usage

### Basic Setup

1. Add the hooks to your test runner configuration
2. Ensure your tests use Selenium WebDriver
3. Run your tests as normal
4. Generate Allure report with: `allure serve target/allure-results`

### Skipping Performance Tracking

For scenarios that involve slow operations (e.g., filling hundreds of form fields), you can skip performance tracking by adding a tag to your scenario:

```gherkin
@skip-performance
Scenario: Fill large form with 100+ fields
  Given I am on the registration page
  When I fill all 100 text fields
  Then the form should be submitted successfully
```

**Supported Tags:**
- `@skip-performance` (recommended)
- `@skipperformance`
- `@no-performance`

**When to use this:**
- Form filling with many fields (100+)
- File upload operations
- Long-running data entry steps
- Any intentionally slow operations that would skew metrics

### Example Feature File

```gherkin
Feature: User Registration

  # Normal scenario - performance will be tracked
  Scenario: Quick login
    Given I am on the login page
    When I enter credentials
    Then I should be logged in

  # Slow scenario - performance tracking skipped
  @skip-performance
  Scenario: Complete detailed registration form
    Given I am on the registration page
    When I fill out the complete registration form with 150 fields
    Then registration should be complete
```

## Report Output

### Step-level Reports
- **Text Summary**: Plain text performance metrics
- **HTML Dashboard**: Visual dashboard with color-coded metrics
- **Raw JSON**: Complete performance data for analysis

### Suite-level Reports
- **environment.properties**: Performance summary in Allure environment section
- **widgets/summary.json**: Custom widget data for Allure
- **Performance Grade**: Overall grade (A+ to D) based on metrics
- **categories.json**: Categorizes tests as "Slow" or "Good" performance

## Performance Thresholds

| Metric | Excellent | Good | Slow |
|--------|-----------|------|------|
| Page Load | ≤ 2000ms | ≤ 3000ms | > 3000ms |
| DOM Ready | ≤ 1500ms | ≤ 2500ms | > 2500ms |
| Response Time | ≤ 800ms | ≤ 1200ms | > 1200ms |
| TTFB | ≤ 400ms | ≤ 600ms | > 600ms |
| Connect Time | ≤ 200ms | ≤ 400ms | > 400ms |
| DNS Lookup | ≤ 100ms | ≤ 200ms | > 200ms |

## Architecture

- **StepPerformanceHooks.java**: Captures per-step metrics using @BeforeStep/@AfterStep
- **SuitePerformanceHooks.java**: Generates suite-level summary using shutdown hook
- **PerformanceMetrics.java**: Data model for performance metrics
- **PerformanceStorage.java**: Thread-safe storage for metrics collection
- **SPAPerformanceTracker.java**: Interfaces with browser performance APIs

## Tips

1. **Use tags liberally**: Tag any scenario that has intentionally slow operations
2. **Review metrics regularly**: Use the performance grade to track improvements
3. **Set realistic thresholds**: Adjust thresholds based on your application's requirements
4. **Cache optimization**: Monitor cache hit rate to optimize resource loading

## Troubleshooting

**No metrics appearing?**
- Ensure SPAPerformanceTracker is properly initialized
- Check that DOM changes are being detected
- Verify driver is accessible via DriverManager

**Metrics seem incorrect?**
- Tag slow scenarios with `@skip-performance`
- Ensure adequate wait times after page loads
- Check browser network conditions

**Widget not showing in Allure?**
- Verify `target/allure-results/widgets/summary.json` exists
- Ensure shutdown hook executed (check console output)
- Regenerate report: `allure generate target/allure-results --clean`
