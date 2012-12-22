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
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Runs the test cases from the test build of the app, notice that this class
 * is abstract since device/app specific code can exist in the implementation
 *
 * @author Shai Almog
 */
public abstract class DeviceRunner {
    private static final int VERSION = 1;
    
    /**
     * Run all the test cases
     */
    public void runTests() {
        try {
            InputStream is = getClass().getResourceAsStream("/tests.dat");
            if(is == null) {
                Log.p("Test data not found in the file, make sure the ant task was executed in full");
                System.exit(2);
                return;
            }
            DataInputStream di = new DataInputStream(is);
            int version = di.readInt();
            if(version > VERSION) {
                Log.p("Tests were built with a new version of Codename One and can't be executed with this runner");
                System.exit(4);
                return;
            }

            String[] tests = new String[di.readInt()];
            for(int iter = 0 ; iter < tests.length ; iter++) {
                tests[iter] = di.readUTF();
            }
            di.close();
            
            for(int iter = 0 ; iter < tests.length ; iter++) {
                runTest(tests[iter]);
            }
        } catch(IOException err) {
            TestReporting.getInstance().logException(err);
        }
        TestReporting.getInstance().testExecutionFinished();
    }
    
    /**
     * This method starts a new application instance
     */
    protected abstract void startApplicationInstance();
    
    /**
     * This method should cleanup the application so the next test case can run on a clean 
     * application instance
     */
    protected abstract void stopApplicationInstance();
    
    /**
     * Runs the given test case
     * 
     * @param testClassName the class name of the test case
     */
    public void runTest(String testClassName) {
        try {
            UnitTest t = (UnitTest)Class.forName(testClassName).newInstance();
            try {
                TestReporting.getInstance().startingTestCase(t);
                startApplicationInstance();
                t.prepare();
                boolean result = t.runTest();
                t.cleanup();
                stopApplicationInstance();
                TestReporting.getInstance().finishedTestCase(t, result);
            } catch(Throwable err) {
                TestReporting.getInstance().logException(err);
                TestReporting.getInstance().finishedTestCase(t, false);
            }
        } catch(Throwable t) {
            TestReporting.getInstance().logMessage("Failed to create instance of " + testClassName);
            TestReporting.getInstance().logMessage("Verify the class is public and doesn't have a specialized constructor");
            TestReporting.getInstance().logException(t);
        }
    }
    
    
}
