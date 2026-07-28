@integration
Feature: Unified search
  A single query returns clients and their documents as one ranked list. Clients are
  matched by name/email; documents are matched semantically and lexically, then fused.
  These scenarios run against the seeded demo data.

  Scenario: Find a client by an email substring
    When I search for "NevisWealth"
    Then the response status should be 200
    And the results should include a client with email "john.doe@neviswealth.com"

  Scenario: Client search is case-insensitive
    When I search for "neviswealth"
    Then the results should include a client with email "john.doe@neviswealth.com"

  Scenario Outline: Name variants all resolve to the same client
    When I search for "<query>"
    Then the results should include a client with email "jack.ohara@example.com"

    Examples:
      | query  |
      | O'Hara |
      | O’Hara |
      | OHara  |
      | o hara |

  Scenario: A semantic query surfaces the relevant document and ranks it first
    When I search for "address proof"
    Then the results should include a document titled "John's utility bill"
    And document "John's utility bill" should rank above document "Portfolio note"

  Scenario: An off-topic query returns an empty list
    When I search for "elephant breeding habits"
    Then the response status should be 200
    And the response should be an empty JSON array

  Scenario: A punctuation-only query is rejected without a table scan
    When I search for "---"
    Then the response status should be 400
    And the response field "code" should be "INVALID_QUERY"

  Scenario: A missing query parameter is rejected
    When I search with no query parameter
    Then the response status should be 400

  Scenario: An oversized limit is clamped, not rejected
    When I search for "John" with limit 5000
    Then the response status should be 200
    And the result count should be at most 100
