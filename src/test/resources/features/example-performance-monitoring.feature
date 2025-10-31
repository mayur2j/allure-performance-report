Feature: Performance Monitoring Examples
  This feature demonstrates how to use the @MonitorPerformance tag
  to selectively enable performance monitoring for specific scenarios

  # This scenario WILL be monitored for performance
  @MonitorPerformance
  Scenario: Login with performance monitoring
    Given user navigates to login page
    When user enters valid credentials
    And user clicks login button
    Then user should see dashboard

  # This scenario will NOT be monitored (no tag)
  Scenario: Simple navigation without monitoring
    Given user is on home page
    When user clicks about link
    Then user should see about page

  # This scenario WILL be monitored for performance
  @MonitorPerformance
  Scenario: Checkout process with performance monitoring
    Given user has items in cart
    When user proceeds to checkout
    And user enters shipping information
    And user completes payment
    Then order should be confirmed

  # This scenario will NOT be monitored (no tag)
  Scenario: View product details without monitoring
    Given user is on products page
    When user clicks on a product
    Then product details should be displayed

  # You can combine with other tags
  @MonitorPerformance @critical @smoke
  Scenario: Critical user flow with performance monitoring
    Given user starts registration
    When user fills registration form
    And user submits registration
    Then user should receive confirmation email
