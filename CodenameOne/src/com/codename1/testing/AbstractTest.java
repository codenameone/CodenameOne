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

import com.codename1.ui.Component;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;

/**
 * A unit test class that simplifies the process of writing test cases
 * for Codename One.
 *
 * @author Shai Almog
 */
public abstract class AbstractTest implements UnitTest {
    /**
     * @inheritDoc
     */
    public void prepare() {
    }

    /**
     * @inheritDoc
     */
    public void cleanup() {
    }

    /**
     * Defaults to two minutes.<br />
     * @inheritDoc
     */
    public int getTimeoutMillis() {
        return 120000;
    }
    
    /**
     * Returns false, default tests run in their own thread.
     * 
     * @inheritDoc
     */
    public boolean shouldExecuteOnEDT() {
        return false;
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertBool(boolean b) {
        TestUtils.assertBool(b);
    }
    

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void waitFor(final int millis) {
        TestUtils.waitFor(millis);
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public Component findByName(String componentName) {
        return TestUtils.findByName(componentName);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public Label findLabelText(String text) {
        return TestUtils.findLabelText(text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void clickButtonByLabel(String text) {
        TestUtils.clickButtonByLabel(text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void clickButtonByName(String name) {
        TestUtils.clickButtonByName(name);
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void clickButtonByPath(int[] path) {
        TestUtils.clickButtonByPath(path);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void goBack() {
        TestUtils.goBack();
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void clickMenuItem(String name) {
        TestUtils.clickMenuItem(name);
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void ensureVisible(Component c) {
        TestUtils.ensureVisible(c);
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void ensureVisible(int[] c) {
        TestUtils.ensureVisible(c);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void ensureVisible(String c) {
        TestUtils.ensureVisible(c);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void waitForFormTitle(final String title) {
        TestUtils.waitForFormTitle(title);
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void waitForFormName(final String name) {
        TestUtils.waitForFormName(name);
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void log(String t) {
        TestUtils.log(t);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void log(Throwable t) {
        TestUtils.log(t);
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public boolean screenshotTest(String screenshotName) {
        return TestUtils.screenshotTest(screenshotName);
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void keyPress(int keyCode) {
        TestUtils.keyPress(keyCode);
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void keyRelease(int keyCode) {
        TestUtils.keyRelease(keyCode);
    }
    
    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void gameKeyPress(int gameKey) {
        TestUtils.gameKeyPress(gameKey);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public void gameKeyRelease(int gameKey) {
        TestUtils.gameKeyRelease(gameKey);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public void pointerPress(float x, float y, String componentName) {
        TestUtils.pointerPress(x, y, componentName);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public void pointerRelease(float x, float y, String componentName) {
        TestUtils.pointerRelease(x, y, componentName);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public void pointerDrag(float x, float y, String componentName) {
        TestUtils.pointerDrag(x, y, componentName);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public void pointerPress(float x, float y, int[] path) {
        TestUtils.pointerPress(x, y, path);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public void pointerRelease(float x, float y, int[] path) {
        TestUtils.pointerRelease(x, y, path);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public void pointerDrag(float x, float y, int[] path) {
        TestUtils.pointerDrag(x, y, path);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public Component getComponentByPath(int[] path) {
        return TestUtils.getComponentByPath(path);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public void setText(String name, String text) {
        TestUtils.setText(name, text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public void setText(int[] path, String text) {
        TestUtils.setText(path, text);
    }


    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public static void assertTitle(String title) {
        TestUtils.assertTitle(title);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public static void assertLabel(String name, String text) {
        TestUtils.assertLabel(name, text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public static void assertLabel(String text) {
        TestUtils.assertLabel(text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public static void assertTextArea(String name, String text) {
        TestUtils.assertLabel(name, text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public static void assertTextArea(String text) {
        TestUtils.assertTextArea(text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience 
     * @see TestUtils
     */
    public static TextArea findTextAreaText(String text) {
        return TestUtils.findTextAreaText(text);
    }
}
