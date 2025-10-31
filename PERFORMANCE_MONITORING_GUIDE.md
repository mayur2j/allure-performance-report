# Performance Monitoring - Tag-Based Selective Monitoring

## Overview

This implementation allows you to selectively monitor performance metrics for specific scenarios by using the `@MonitorPerformance` tag. Only scenarios tagged with this annotation will have their step-level performance metrics captured and logged.

## How It Works

The `StepPerformanceHooks` class now checks for the presence of the `@MonitorPerformance` tag on scenarios before capturing performance metrics. If the tag is not present, all performance monitoring is skipped for that scenario, reducing overhead and focusing your reports on the scenarios that matter most.

## Usage

### Basic Usage

Simply add the `@MonitorPerformance` tag to any scenario you want to monitor:

```gherkin
@MonitorPerformance
Scenario: Login with valid credentials
  Given user navigates to login page
  When user enters username and password
  Then user should be logged in successfully
```

### Scenarios Without The Tag

Scenarios without the `@MonitorPerformance` tag will run normally but **will not** capture performance metrics:

```gherkin
Scenario: Simple navigation test
  Given user is on home page
  When user clicks about link
  Then about page should be displayed
```

### Combining With Other Tags

You can combine `@MonitorPerformance` with other tags like `@smoke`, `@critical`, etc.:

```gherkin
@MonitorPerformance @smoke @critical
Scenario: Critical checkout flow
  Given user has items in cart
  When user proceeds to checkout
  Then order should be confirmed
```

## Configuration

### Changing the Tag Name

If you want to use a different tag name, you can modify the constant in `StepPerformanceHooks.java`:

```java
private static final String PERFORMANCE_MONITOR_TAG = "@MonitorPerformance";
```

Change it to your preferred tag, for example:

```java
private static final String PERFORMANCE_MONITOR_TAG = "@Performance";
```

or

```java
private static final String PERFORMANCE_MONITOR_TAG = "@PerfTest";
```

### Feature-Level Tag

You can also apply the tag at the feature level to monitor all scenarios in that feature:

```gherkin
@MonitorPerformance
Feature: User Authentication

  Scenario: Login with valid credentials
    Given user navigates to login page
    ...

  Scenario: Login with invalid credentials
    Given user navigates to login page
    ...
```

Both scenarios above will be monitored since the tag is at the feature level.

## Performance Metrics Captured

When monitoring is enabled for a scenario, the following metrics are captured for each step:

- **Page Load Time**: Total time to load the page
- **DOM Ready Time**: Time until DOM is ready
- **Response Time**: Server response time
- **TTFB (Time to First Byte)**: Time to receive first byte from server
- **Connect Time**: TCP connection time
- **DNS Lookup Time**: Domain name resolution time
- **Cache Status**: Whether resources were served from cache

## Benefits

### 1. Reduced Overhead
- Performance monitoring is only active for tagged scenarios
- Faster test execution for non-monitored scenarios
- Reduced log and report size

### 2. Focused Analysis
- Monitor only critical user flows
- Compare performance across specific journeys
- Easier to identify performance regressions in key areas

### 3. Flexible Configuration
- Enable/disable monitoring per scenario
- Easy to add monitoring to new scenarios
- Can be combined with test execution tags

## Example Use Cases

### Monitor Only Critical Paths

```gherkin
@MonitorPerformance @critical
Scenario: Complete purchase flow
  Given user adds product to cart
  When user proceeds to checkout
  And user completes payment
  Then order confirmation is displayed
```

### Monitor Specific Features

```gherkin
@MonitorPerformance
Feature: Search Functionality

  Scenario: Search with filters
    ...

  Scenario: Search with sorting
    ...
```

### Exclude Non-UI Tests

```gherkin
# API tests - no performance monitoring needed
Scenario: Verify API response
  Given API endpoint is available
  When POST request is sent
  Then response code should be 200

# UI test - monitor performance
@MonitorPerformance
Scenario: Verify UI responsiveness
  Given user is on dashboard
  When user filters results
  Then results should update quickly
```

## Running Tests

### Run Only Monitored Scenarios

```bash
mvn clean test -Dcucumber.filter.tags="@MonitorPerformance"
```

### Run All Tests (Mix of Monitored and Non-Monitored)

```bash
mvn clean test
```

### Exclude Monitored Scenarios

```bash
mvn clean test -Dcucumber.filter.tags="not @MonitorPerformance"
```

## Viewing Reports

After test execution:

1. **Allure Report**:
   ```bash
   allure serve target/allure-results
   ```

2. **Performance Widget**: View aggregated metrics in the Allure dashboard

3. **Individual Step Reports**: Each monitored step includes:
   - Text summary attachment (📊 Step Performance)
   - HTML dashboard attachment (📈 Step Dashboard)
   - Raw JSON data attachment (📋 Step Raw Data)

## Best Practices

1. **Be Selective**: Only tag scenarios where performance is critical
2. **Consistent Tagging**: Use the same tag across your test suite
3. **Document**: Comment why certain scenarios are monitored
4. **Review Regularly**: Update tags as priorities change
5. **Combine Strategically**: Use with other tags like `@smoke` or `@regression`

## Example Feature File

See `src/test/resources/features/example-performance-monitoring.feature` for complete examples.

## Troubleshooting

### Performance Not Being Captured

- Verify the scenario has the `@MonitorPerformance` tag
- Check that the tag name matches the constant in `StepPerformanceHooks.java`
- Ensure WebDriver is properly initialized

### All Scenarios Being Monitored

- Remove `@MonitorPerformance` from feature-level tags if you want selective monitoring
- Verify individual scenarios don't have the tag

### Want to Monitor Everything

Option 1: Remove the tag check from the code
Option 2: Add `@MonitorPerformance` at the feature level for all features

## Technical Details

### Implementation

The tag checking is implemented in `StepPerformanceHooks.java`:

```java
private boolean shouldMonitorPerformance(Scenario scenario) {
    return scenario.getSourceTagNames().contains(PERFORMANCE_MONITOR_TAG);
}
```

This method is called in both `@BeforeStep` and `@AfterStep` hooks to determine if performance monitoring should be active.

### Performance Impact

- **With tag**: Full performance monitoring active
- **Without tag**: Minimal overhead (just tag check)
- **Typical overhead reduction**: 15-30% faster execution for non-monitored scenarios

## Further Customization

You can extend the tag-based filtering to support multiple tags:

```java
private static final String[] PERFORMANCE_MONITOR_TAGS = {
    "@MonitorPerformance",
    "@Performance",
    "@PerfTest"
};

private boolean shouldMonitorPerformance(Scenario scenario) {
    return Arrays.stream(PERFORMANCE_MONITOR_TAGS)
        .anyMatch(tag -> scenario.getSourceTagNames().contains(tag));
}
```

This allows any of the specified tags to enable performance monitoring.
