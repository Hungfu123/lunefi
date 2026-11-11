package finance.bdd;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("eintraege")
@SelectClasspathResource("gemeinsam")
@SelectClasspathResource("sparziele")
@SelectClasspathResource("budgets")
@SelectClasspathResource("konten")
public class RunCucumberTest {
}
