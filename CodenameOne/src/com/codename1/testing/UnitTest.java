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

/**
 * A Codename One unit test interface, you would normally like to derive from
 * AbstractTest which is less verbose and contains many helper methods. 
 *
 * @author Shai Almog
 */
public interface UnitTest {
    /**
     * Runs a unit test, if it returns true it passed. If it threw an exception or returned
     * false it failed.
     * 
     * @return whether it passed
     * @throws Exception thrown if it failed
     */
    public boolean runTest() throws Exception;
    
    /**
     * Prepares the unit test for execution
     */
    public void prepare();
    
    /**
     * Cleanup after a test case executed 
     */
    public void cleanup();
    
    /**
     * Returns the time in milliseconds after which the test should be automatically failed.

     * @return time in milliseconds
     */
    public int getTimeoutMillis();

    /**
     * Returns true to indicate that the test expects to be executed on the EDT
     * @return whether the test should execute on the EDT or the testing thread
     */
    public boolean shouldExecuteOnEDT();
}
