Feature: Graph schema type aliases
  As a consumer of the graph.json contract (Brooklin Flow, bakery, codebase)
  I want the graph schema to canonicalise type aliases without losing data
  So that a legacy or foreign spelling never breaks navigation or filtering

  Scenario: A canonical node type is returned unchanged
    Given the graph schema vocabulary
    When the node type "file" is canonicalised
    Then the canonical node type is "file"

  Scenario: Plural and legacy spellings collapse onto the canonical singular
    Given the graph schema vocabulary
    When the node type "dir" is canonicalised
    Then the canonical node type is "directory"
    When the node type "module" is canonicalised
    Then the canonical node type is "project"

  Scenario: A canonical edge type is returned unchanged
    Given the graph schema vocabulary
    When the edge type "agent_reference" is canonicalised
    Then the canonical edge type is "agent_reference"

  Scenario: Plural edge spellings collapse onto the canonical singular
    Given the graph schema vocabulary
    When the edge type "imports" is canonicalised
    Then the canonical edge type is "import"
    When the edge type "references" is canonicalised
    Then the canonical edge type is "reference"

  Scenario: An unknown type is preserved as-is and never dropped
    Given the graph schema vocabulary
    When the node type "domain" is canonicalised
    Then the canonical node type is "domain"
    When the edge type "depends_on" is canonicalised
    Then the canonical edge type is "depends_on"

  Scenario: Canonicalisation is idempotent
    Given the graph schema vocabulary
    When the node type "dir" is canonicalised
    And the node type result is canonicalised again
    Then the canonical node type is "directory"

  Scenario: A whole legacy graph is canonicalised while every other field is preserved
    Given a legacy graph with a "dir" node and an "imports" edge
    When the graph is canonicalised
    Then the node type becomes "directory"
    And the edge type becomes "import"
    And every non-type field is preserved
