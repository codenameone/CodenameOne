package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.TestReporting;

public class Cn1ssDeviceRunnerReporter extends TestReporting {
    @Override
    public void startingTestCase(String testName) {
        super.startingTestCase(testName);
        System.out.println("CN1SS:INFO:test=" + Cn1ssDeviceRunnerHelper.sanitizeTestName(testName) + " status=START");
    }

    @Override
    public void finishedTestCase(String testName, boolean passed) {
        super.finishedTestCase(testName, passed);
        String status = passed ? "PASSED" : "FAILED";
        System.out.println("CN1SS:INFO:test=" + Cn1ssDeviceRunnerHelper.sanitizeTestName(testName) + " status=" + status);
    }

    @Override
    public void logMessage(String message) {
        super.logMessage(message);
        if (message != null && message.length() > 0) {
            System.out.println("CN1SS:INFO:message=" + message);
        }
    }

    @Override
    public void logException(Throwable err) {
        super.logException(err);
        System.out.println("CN1SS:ERR:exception=" + err);
        if (err != null) {
            err.printStackTrace();
        }
    }

    @Override
    public void testExecutionFinished(String suiteName) {
        super.testExecutionFinished(suiteName);
        System.out.println("CN1SS:SUITE:FINISHED");
    }
}
