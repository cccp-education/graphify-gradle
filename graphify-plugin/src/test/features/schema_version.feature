Feature: Graph schema version contract
  As a consumer of the graph.json contract (Brooklin Flow, bakery, codebase)
  I want a versioned, forward-compatible schema
  So that a new field never breaks a strict reader and a legacy file stays readable

  Scenario: An in-memory model carries the current schema version
    Given a graph model built in memory
    Then the schema version is the current one

  Scenario: The schema version is serialized as a top-level field
    Given a graph model built in memory
    When the model is serialized
    Then the JSON exposes a top-level "schemaVersion" field equal to 1

  Scenario: A model round-trips its schema version
    Given a graph model built in memory
    When the model is serialized and read back
    Then the read-back schema version is the current one

  Scenario: A legacy graph without a schema version is read with the default
    Given a legacy graph JSON without a "schemaVersion" field
    When the legacy graph is read by a strict reader
    Then the read-back schema version is the current one

  Scenario: An unknown top-level field is ignored by a strict reader
    Given a graph JSON carrying an unknown top-level field
    When the graph is read by a strict reader
    Then the graph is read without error

  Scenario: An unknown node field is ignored by a strict reader
    Given a graph JSON carrying an unknown node field
    When the graph is read by a strict reader
    Then the graph is read without error
    And the node keeps its known fields
