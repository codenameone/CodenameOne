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

import com.codename1.ui.Display;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Produces test reporting in the format of JUnit XML for compatibility with
 * tools that consume JUnit test case results see http://code.google.com/p/codenameone/issues/detail?id=446
 * for more details.<br>
 *
 * @author Shai Almog
 */
public class JUnitXMLReporting extends TestReporting {
    private String testCases = "";
    private String output;
    private int passed;
    private int failed;
    
    /**
     * {@inheritDoc}
     */
    public void startingTestCase(UnitTest test) {
        testCases += "<testcase classname=\"" + test.getClass().getName() + "\" >";
        output = "";
    }

    /**
     * {@inheritDoc}
     */
    public void logMessage(String message) {
        output += message + "\n";
    }
    
    /**
     * {@inheritDoc}
     */
    public void logException(Throwable err) {
        testCases += "<error type=\"" + err.getClass().getName() + "\">" + err.toString() + "</error>\n";
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
            testCases += "<system-out>\n" + output + "</system-out><error/>\n"
                    + "</testcase>";
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void writeReport(OutputStream os) throws IOException {
        os.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                + "<testsuite failures=\"" + failed + "\"  "
                + "skipped=\"0\" tests=\"" + (passed + failed) + "\" name=\"" + Display.getInstance().getProperty("AppName", "Unnamed")
                + "\">\n" + testCases + 
                "\n</testsuite>").getBytes());
    }
    
    /**
     * {@inheritDoc}
     */
    public void testExecutionFinished() {
    }
}
