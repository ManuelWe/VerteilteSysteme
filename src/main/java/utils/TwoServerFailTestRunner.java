package utils;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import test.TwoServerFail;

public class TwoServerFailTestRunner {

	public static void main(String[] args) {
		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(TwoServerFail.class);
		try {
			Thread.sleep(45000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.printf("Test ran: %s, Failed: %s%n", result.getRunCount(), result.getFailureCount());
	}

}
