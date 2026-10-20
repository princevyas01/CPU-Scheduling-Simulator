package cpuscheduler;

import java.util.Map;

/**
 * Standalone test runner that executes the automated test suite without external dependencies.
 */
public class TestRunner {
    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println(" CPU SCHEDULING STUDIO: AUTOMATED CORRECTNESS & INVARIANT TEST SUITE");
        System.out.println("======================================================================");

        Map<String, SchedulerCorrectnessTest.TestCase> tests = SchedulerCorrectnessTest.getTests();
        int total = tests.size();
        int passed = 0;
        int failed = 0;

        long startTime = System.currentTimeMillis();

        for (Map.Entry<String, SchedulerCorrectnessTest.TestCase> entry : tests.entrySet()) {
            String testName = entry.getKey();
            SchedulerCorrectnessTest.TestCase testCase = entry.getValue();

            System.out.print(String.format("[RUNNING] %-60s ... ", testName));
            try {
                testCase.run();
                System.out.println("PASS");
                passed++;
            } catch (Throwable t) {
                System.out.println("FAIL!");
                System.err.println("   >>> ERROR in " + testName + ": " + t.getMessage());
                t.printStackTrace();
                failed++;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("======================================================================");
        System.out.println(String.format("TEST EXECUTION COMPLETE in %d ms", elapsed));
        System.out.println(String.format("Total: %d | Passed: %d | Failed: %d", total, passed, failed));
        System.out.println("======================================================================");

        if (failed > 0) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }
}
