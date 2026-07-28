@integration
Feature: Client registry
  Wealth managers register their clients through the API. Email is the unique
  identity, and the request is validated before anything is written.

  Scenario: Register a new client
    When I create a client with:
      """
      { "first_name": "Grace", "last_name": "Hopper", "email": "grace.hopper@example.com" }
      """
    Then the response status should be 201
    And the response field "first_name" should be "Grace"
    And the response field "email" should be "grace.hopper@example.com"

  Scenario: Reject a duplicate email
    # john.doe@neviswealth.com is seeded by the demo profile.
    When I create a client with:
      """
      { "first_name": "Johnny", "last_name": "Doe", "email": "john.doe@neviswealth.com" }
      """
    Then the response status should be 409
    And the response field "code" should be "EMAIL_ALREADY_EXISTS"

  Scenario: Reject a malformed email
    When I create a client with:
      """
      { "first_name": "Ada", "last_name": "Lovelace", "email": "not-an-email" }
      """
    Then the response status should be 400
    And the response field "code" should be "VALIDATION_FAILED"
    And the response field "details.0.field" should be "email"

  Scenario: Reject a blank last name
    When I create a client with:
      """
      { "first_name": "Ada", "last_name": "   ", "email": "ada.lovelace@example.com" }
      """
    Then the response status should be 400
    And the response field "code" should be "VALIDATION_FAILED"

  Scenario: Reject a non-http social link
    When I create a client with:
      """
      { "first_name": "Ada", "last_name": "Lovelace", "email": "ada.lovelace@example.com",
        "social_links": ["ftp://example.com/profile"] }
      """
    Then the response status should be 400
    And the response field "code" should be "VALIDATION_FAILED"

  Scenario: List clients includes the seeded demo data
    When I list all clients
    Then the response status should be 200
    And the client list should include email "john.doe@neviswealth.com"
