package utils;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import test.TestID7_OneServerFails;

public class OneServerFailsTestRunner {

	public static void main(String[] args) {
		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(TestID7_OneServerFails.class);
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.printf("Test ran: %s, Failed: %s%n", result.getRunCount(), result.getFailureCount());
	}

}
