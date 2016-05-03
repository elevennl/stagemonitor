package org.stagemonitor.requestmonitor.prof;

import net.sf.ehcache.pool.sizeof.AgentSizeOf;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.profiler.Profiler;

public class ProfilerTest {

	@BeforeClass
	public static void attachProfiler() {
		// tests whether the agent still works if other agents are around
		new AgentSizeOf();
		Stagemonitor.init();
	}

	@Test
	public void testProfiler() {
		ProfilerTest profilerTest = new ProfilerTest();
		CallStackElement total = Profiler.activateProfiling("total");
		Assert.assertEquals(21, profilerTest.method1());
		Profiler.stop();

		Assert.assertEquals(total.toString(), 1, total.getChildren().size());
		Assert.assertEquals(total.toString(), 3, total.getChildren().get(0).getChildren().size());
		final String method5 = total.getChildren().get(0).getChildren().get(2).getSignature();
		Assert.assertTrue(method5, method5.contains("org.stagemonitor.requestmonitor.prof.ProfilerTest.method5"));
	}

	@Test
	public void testInnerPrivateMethod() {
		class Test {
			private void test() {
			}
		}

		Test test = new Test();
		CallStackElement total = Profiler.activateProfiling("total");
		test.test();
		Profiler.stop();

		Assert.assertFalse(total.toString(), total.getChildren().iterator().next().getSignature().contains("access$"));
	}

	public int method1() {
		return method2(1) + method3() + method5();
	}

	private int method2(int i) {
		return 1 + i;
	}

	private int method3() {
		return method4();
	}

	private int method4() {
		return 4;
	}

	private int method5() {
		return method6() + method7();
	}

	private int method6() {
		int value = 1;
		if (Math.random() > 0.5) {
			return 6;
		}
		switch (value) {
			case 1:
				value = 6;
				break;
		}
		return value;
	}

	private int method7() {
		return method8();
	}

	private int method8() {
		return method9();
	}

	private int method9() {
			return 9;
		}

}
