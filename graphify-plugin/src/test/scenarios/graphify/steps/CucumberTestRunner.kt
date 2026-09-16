package graphify.steps

import io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Cucumber test runner for the graphify-gradle borough (EPIC GF-SCHEMA).
 *
 * Glue lives in [graphify.steps] so the cucumber convention plugin can exclude
 * `*.scenarios.*` from the unit `test` task. The tag filter keeps this gate on
 * domain-level schema scenarios only.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "graphify.steps")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber.html, json:build/reports/cucumber.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @integration")
class CucumberTestRunner
