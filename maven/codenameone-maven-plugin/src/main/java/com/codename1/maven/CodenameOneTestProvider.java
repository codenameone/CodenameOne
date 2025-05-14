package com.codename1.maven;

import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.*;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.ReflectionUtils;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.ScannerFilter;
import org.apache.maven.surefire.api.util.TestsToRun;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.maven.surefire.api.util.internal.ObjectUtils.systemProps;

public class CodenameOneTestProvider extends AbstractProvider {

    private final ScanResult scanResult;
    private final ClassLoader testClassLoader;
    private final ReporterFactory reporterFactory;
    private TestsToRun testsToRun;
    public CodenameOneTestProvider(ProviderParameters params) {
        scanResult = params.getScanResult();
        testClassLoader = params.getTestClassLoader();
        reporterFactory = params.getReporterFactory();
    }

    @Override
    public Iterable<Class<?>> getSuites() {
        if (true) {
            // This test provider will just be a dummy as we will be using the CN1Test mojo to actually run the tests.
            // We just want to disable surefire...
            // THIS IS A HACK
            // Eventually, we should change the strategy implement a proper custom surefire provider

            return new ArrayList<Class<?>>();
        }
        testsToRun = scanClassPath();
        return testsToRun;
    }

    @Override
    public RunResult invoke(Object testSet) throws TestSetFailedException, ReporterException, InvocationTargetException {
        if ( testsToRun == null ) {
            if ( testSet instanceof TestsToRun ) {
                testsToRun = (TestsToRun) testSet;
            } else if ( testSet instanceof Class ) {
                testsToRun = TestsToRun.fromClass((Class<?>) testSet);
            } else {
                testsToRun = scanClassPath();
            }
        }

        RunResult runResult;
        try {
            final RunListener reporter = reporterFactory.createReporter();
            ConsoleOutputCapture.startCapture( (ConsoleOutputReceiver) reporter );
            Map<String, String> systemProperties = systemProps();
            String smClassName = System.getProperty( "surefire.security.manager" );
            if ( smClassName != null ) {
                SecurityManager securityManager =
                        ReflectionUtils.instantiate( getClass().getClassLoader(), smClassName, SecurityManager.class );
                System.setSecurityManager( securityManager );
            }

            for ( Class<?> clazz : testsToRun ) {
                SurefireTestSet surefireTestSet = new CodenameOneTestSet(clazz);
                executeTestSet( surefireTestSet, reporter, testClassLoader, systemProperties );
            }
        }
        finally
        {
            runResult = reporterFactory.close();
        }
        return runResult;

    }

    private void executeTestSet( SurefireTestSet testSet, RunListener reporter, ClassLoader classLoader,
                                 Map<String, String> systemProperties )
            throws TestSetFailedException
    {
        String clazz = testSet.getName();

        try
        {
            TestSetReportEntry started = new SimpleReportEntry( clazz, null, null, null );
            reporter.testSetStarting( started );
            testSet.execute( reporter, classLoader );
        }
        finally
        {
            TestSetReportEntry completed = new SimpleReportEntry( clazz, null, null, null, systemProperties );
            reporter.testSetCompleted( completed );
        }
    }

    private boolean impl(Class cls) {
        for(Class current : cls.getInterfaces()) {
            if(current.getName().equals("com.codename1.testing.UnitTest")) {
                return true;
            }
        }
        Class parent = cls.getSuperclass();
        if(parent == Object.class || parent == null) {
            return false;
        }
        return impl(parent);
    }

    private boolean isTestCase(Class cls) {
        try {
            if(Modifier.isAbstract(cls.getModifiers())) {
                return false;
            }
            if(impl(cls)) {
                return true;
            }
        } catch(Throwable t) {
        }
        return false;
    }

    private TestsToRun scanClassPath() {
         final TestsToRun testsToRun = scanResult.applyFilter(new ScannerFilter() {


            @Override
            public boolean accept(Class aClass) {
                return isTestCase(aClass);
            }
        }, testClassLoader);
        return testsToRun;
    }

    public static interface SurefireTestSet
    {
        void execute( RunListener reportManager, ClassLoader loader )
                throws TestSetFailedException;

        String getName();
    }

    public static class CodenameOneTestSet implements SurefireTestSet {

        private Class<?> testClass;

        public CodenameOneTestSet(Class<?> testClass) {
            this.testClass = testClass;
        }

        @Override
        public void execute(RunListener reportManager, ClassLoader loader) throws TestSetFailedException {

        }

        @Override
        public String getName() {
            return testClass.getName();
        }
    }
}
