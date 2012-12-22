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

import com.codename1.io.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Test reports can be overriden to provide custom test reporting options
 * you can replace the test reporter on the device by sending the build
 * argument build.testReporter='com.x.MyTestReporterClass'.
 *
 * @author Shai Almog
 */
public class TestReporting {
    private static TestReporting instance;
    private Hashtable testsExecuted = new Hashtable();
    
    
    /**
     * Gets the test reporting instance
     */
    public static TestReporting getInstance() {
        if(instance == null) {
            instance = new TestReporting();
        }
        return instance;
    }
    
    /**
     * Sets the test reporting instance to a subclass of this class.
     * @param i the new instance
     */
    public static void setInstance(TestReporting i) {
        instance = i;
    }
    
    /**
     * Invoked when a unit test is started
     * @param test the test case
     */
    public void startingTestCase(UnitTest test) {
        Log.p("Starting test case " + test.getClass().getName());
    }

    /**
     * Indicates a message from the current test case
     * @param message the message
     */
    public void logMessage(String message) {
        Log.p(message);
    }
    
    /**
     * Indicates an error from the current test case
     * @param err the error message
     */
    public void logException(Throwable err) {
        Log.e(err);
    }
    
    /**
     * Invoked when a unit test has completed
     * @param test the test case
     * @param passed true if the test passed and false otherwise
     */
    public void finishedTestCase(UnitTest test, boolean passed) {
        if(passed) {
            Log.p(test.getClass().getName() + " passed");
            testsExecuted.put(test.getClass().getName(), Boolean.TRUE);
        } else {
            Log.p(test.getClass().getName() + " failed");
            testsExecuted.put(test.getClass().getName(), Boolean.FALSE);
        }
    }
    
    /**
     * Writes a test report to the given stream
     * @param os the destination stream
     */
    public void writeReport(OutputStream os) throws IOException {
        Enumeration e = testsExecuted.elements();
        while(e.hasMoreElements()) {
            String key = (String)e.nextElement();
            Boolean v = (Boolean)testsExecuted.get(key);
            if(v.booleanValue()) {
                os.write((key + " passed\n").getBytes());
            } else {
                os.write((key + " failed\n").getBytes());
            }
        }
    }
    
    /**
     * Callback to indicate the test execution has finished allowing
     * for a report to be generated if appropriate
     */
    public void testExecutionFinished() {
    }
}
