Feature: Publication hygiene against the published workspace catalog
  As a maintainer of the cross-borough release chain
  I want the local self version checked against the published workspace catalog
  So that a drift is caught at build time, never raced by a neighbour repository

  Scenario: A local version matching the published catalog is consistent
    Given a local catalog declaring graphify-plugin "0.0.5"
    And a published workspace catalog version "0.0.5"
    When the publication hygiene is checked
    Then the hygiene verdict is consistent

  Scenario: A local version ahead of the published catalog is a drift
    Given a local catalog declaring graphify-plugin "0.0.6"
    And a published workspace catalog version "0.0.5"
    When the publication hygiene is checked
    Then the hygiene verdict is a violation
    And the message names both the local and the published version

  Scenario: A version declared outside the versions section never resolves
    Given a local catalog whose versions section is empty
    And the alias graphify declares "9.9.9" outside the versions section
    When the local version is looked up
    Then the local version is absent

  Scenario: A commented-out version never resolves
    Given a local catalog with only a commented graphify-plugin "9.9.9"
    When the local version is looked up
    Then the local version is absent

  Scenario: A missing published version is reported as a violation
    Given a local catalog declaring graphify-plugin "0.0.5"
    And no published workspace catalog version
    When the publication hygiene is checked
    Then the hygiene verdict is a violation
    And the message names the published version as missing
