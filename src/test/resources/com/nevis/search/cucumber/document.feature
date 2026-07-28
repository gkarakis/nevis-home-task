@integration
Feature: Document ingestion
  Every document belongs to a client. On write the content is chunked and embedded,
  so the document must reference an existing client and carry a title and body.

  Background:
    Given a client named "Acme" exists

  Scenario: Add a document to an existing client
    When I add a document to client "Acme" with:
      """
      { "title": "Utility bill", "content": "Electricity bill for 10 Downing Street dated January 2026. Total amount due 84.20 GBP." }
      """
    Then the response status should be 201
    And the response field "title" should be "Utility bill"
    And the document should have at least 1 chunk

  Scenario: Reject a document for an unknown client
    When I add a document to a non-existent client with:
      """
      { "title": "Orphan", "content": "This document points at a client that does not exist." }
      """
    Then the response status should be 404
    And the response field "code" should be "CLIENT_NOT_FOUND"

  Scenario: Reject a document with a blank title
    When I add a document to client "Acme" with:
      """
      { "title": "   ", "content": "The body is fine but the title is blank." }
      """
    Then the response status should be 400
    And the response field "code" should be "VALIDATION_FAILED"
