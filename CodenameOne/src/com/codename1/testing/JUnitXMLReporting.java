/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.testing;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Produces test reporting in the format of JUnit XML for compatibility with
 * tools that consume JUnit test case results see http://code.google.com/p/codenameone/issues/detail?id=446
 * for more details.<br>
 * See https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd for the schema
 *
 * @author Shai Almog
 */
public class JUnitXMLReporting extends TestReporting {
    private String testCases = "";
    private String output;
    private int passed;
    private int failed;
    private int errors;
    
    /**
     * {@inheritDoc}
     */
    public void startingTestCase(UnitTest test) {
        testCases += "<testcase classname=\"" + test.getClass().getName() + "\">\n";
        output = "";
        super.startingTestCase(test);
    }

    /**
     * {@inheritDoc}
     */
    public void logMessage(String message) {
        output += message + "\n";
        super.logMessage(message);
    }
    
    /**
     * {@inheritDoc}
     */
    public void logException(Throwable err) {
        errors++;
        testCases += "<error type=\"" + err.getClass().getName() + "\">" + err.toString() + "</error>\n";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        err.printStackTrace(pw);
        pw.close();
        output += sw.toString();
        super.logException(err);
    }
    
    /**
     * {@inheritDoc}
     */
    public void finishedTestCase(UnitTest test, boolean passed) {
        if(passed) {
            this.passed++;
            testCases += "<system-out>\n" + output + "</system-out>\n"
                    + "</testcase>";
        } else {
            failed++;
            testCases += "<system-out>\n" + output + "</system-out>\n"
                    + "</testcase>";
        }
        super.finishedTestCase(test, passed);
    }
    
    /**
     * {@inheritDoc}
     */
    public void writeReport(String testSuiteName, OutputStream os) throws IOException {
        os.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                + "<testsuite failures=\"" + (failed - errors) + "\" "
                + "errors=\"" + errors + "\" "
                + "skipped=\"0\" tests=\"" + (passed + failed) + "\" name=\"" + testSuiteName
                + "\">\n" + testCases + 
                "\n</testsuite>").getBytes());
    }
    
    /**
     * {@inheritDoc}
     */
    public void testExecutionFinished(String testSuiteName) {
        String reportFilename = "TEST-" + testSuiteName + ".xml";
        File reportFile = new File(reportFilename);
        //noinspection ResultOfMethodCallIgnored
        reportFile.delete();

        OutputStream fos = null;
        try {
            fos = new FileOutputStream(reportFile);
            writeReport(testSuiteName, fos);
        } catch (IOException ex) {
            System.err.println("Failed to write JUnit XML report: " + ex.toString());
            ex.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {}
            }
        }
        super.testExecutionFinished(testSuiteName);
    }
}
