package com.nevis.search;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Runs the Gherkin feature files under {@code com/nevis/search/cucumber} through the
 * Cucumber engine. Named {@code *IntegrationTest} so the Failsafe plugin runs it in
 * the integration phase; every scenario is tagged {@code @integration}, so Surefire
 * (which excludes that group) skips it during the fast unit build.
 *
 * <p>Requires Docker and the embedding model — see {@link com.nevis.search.cucumber.CucumberSpringConfiguration}.
 * The JUnit Platform Suite engine does not evaluate Jupiter conditions such as
 * {@code @DisabledOnOs}, so the macOS-Intel skip that {@code SearchIntegrationTest}
 * gets from that annotation is applied here by an OS-activated Maven profile that
 * excludes this runner from Failsafe (see {@code pom.xml}).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("com/nevis/search/cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.nevis.search.cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class CucumberIntegrationTest {
}
