Feature: Incremental scan cache
  As a consumer of the graph.json contract (Brooklin Flow, bakery, codebase)
  I want a re-scan to reuse unchanged file extractions
  So that a large workspace scan gets faster without ever changing the output

  Scenario: An empty cache reports no fast path
    Given an empty scan cache
    When the fast path is checked for "src/App.kt" with size 10 and mtime 1000
    Then the fast path is not available
    And the extraction for "src/App.kt" is absent

  Scenario: A recorded fingerprint answers the fast path
    Given a scan cache recording "src/App.kt" with size 10 and mtime 1000
    When the fast path is checked for "src/App.kt" with size 10 and mtime 1000
    Then the fast path is available
    And the extraction for "src/App.kt" is present

  Scenario: A drifted size or mtime invalidates the fast path
    Given a scan cache recording "src/App.kt" with size 10 and mtime 1000
    When the fast path is checked for "src/App.kt" with size 11 and mtime 1000
    Then the fast path is not available
    When the fast path is checked for "src/App.kt" with size 10 and mtime 9999
    Then the fast path is not available

  Scenario: Identical content under a new fingerprint is reused by content hash
    Given a scan cache recording "src/App.kt" with size 10 and mtime 1000
    When a file with the same content is recorded under "src/Moved.kt" with a newer mtime
    Then only one extraction is stored for that content
    And the extraction for "src/Moved.kt" is present

  Scenario: Pruning drops vanished paths and their orphan extractions
    Given a scan cache recording "src/App.kt" with size 10 and mtime 1000
    And a scan cache recording "src/Gone.kt" with size 20 and mtime 2000
    When the cache is pruned to the live paths "src/App.kt"
    Then the cache holds only the path "src/App.kt"
    And the extraction of the vanished path is dropped

  Scenario: A cache survives a JSON round-trip
    Given a scan cache recording "src/App.kt" with size 10 and mtime 1000
    When the cache is saved and loaded again
    Then the loaded cache equals the original cache

  Scenario: A cache written by an unknown schema version is ignored
    Given a cache file written by schema version 99
    When the cache file is loaded
    Then the loaded cache is empty
