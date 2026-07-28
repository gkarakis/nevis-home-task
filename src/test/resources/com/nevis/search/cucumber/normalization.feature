@integration
Feature: Search normalization
  Java and database normalization stay aligned, because substring search depends
  on both implementations producing the same normalized form.

  Scenario: Java and SQL normalization agree for all fixtures
    Then Java and SQL normalization should agree for all fixtures
