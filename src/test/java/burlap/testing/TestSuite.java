package burlap.testing;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	TestGridWorld.class,
	TestPlanning.class,
	TestBlockDude.class,
	TestHashing.class
})
public class TestSuite {

}
