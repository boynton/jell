import org.testng.annotations.Test;
import static org.testng.Assert.*;
import static org.jelled.Ell.*;
import java.io.FileReader;

public class TestRuntime {
	
	@Test
	public void testBenchmark() {	
		runModule("src/main/scm/benchx.scm", new EllPrimitives());
	}

}
