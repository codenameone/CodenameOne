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

import com.codename1.ui.Command;
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
     * {@inheritDoc}
     */
    public void prepare() {
    }

    /**
     * {@inheritDoc}
     */
    public void cleanup() {
    }

    /**
     * Defaults to two minutes.<br>
     * {@inheritDoc}
     */
    public int getTimeoutMillis() {
        return 120000;
    }

    /**
     * Returns false, default tests run in their own thread.
     *
     * {@inheritDoc}
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
    public void assertBool(boolean b, String errorMessage) {
        TestUtils.assertBool(b, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void fail() {
        TestUtils.fail();
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void fail(String errorMessage) {
        TestUtils.fail(errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertTrue(boolean value) {
        TestUtils.assertTrue(value);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertTrue(boolean value, String errorMessage) {
        TestUtils.assertTrue(value, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertFalse(boolean value) {
        TestUtils.assertFalse(value);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertFalse(boolean value, String errorMessage) {
        TestUtils.assertFalse(value, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNull(Object object) {
        TestUtils.assertNull(object);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNull(Object object, String errorMessage) {
        TestUtils.assertNull(object, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotNull(Object object) {
        TestUtils.assertNotNull(object);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotNull(Object object, String errorMessage) {
        TestUtils.assertNotNull(object, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertSame(Object expected, Object actual) {
        TestUtils.assertSame(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertSame(Object expected, Object actual, String errorMessage) {
        TestUtils.assertSame(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotSame(Object expected, Object actual) {
        TestUtils.assertNotSame(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotSame(Object expected, Object actual, String errorMessage) {
        TestUtils.assertNotSame(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(byte expected, byte actual) {
        TestUtils.assertEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(byte expected, byte actual, String errorMessage) {
        TestUtils.assertEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(short expected, short actual) {
        TestUtils.assertEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(short expected, short actual, String errorMessage) {
        TestUtils.assertEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(int expected, int actual) {
        TestUtils.assertEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(int expected, int actual, String errorMessage) {
        TestUtils.assertEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(long expected, long actual) {
        TestUtils.assertEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(long expected, long actual, String errorMessage) {
        TestUtils.assertEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(float expected, float actual, double maxRelativeError) {
        TestUtils.assertEqual(expected, actual, maxRelativeError);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(float expected, float actual, double maxRelativeError, String errorMessage) {
        TestUtils.assertEqual(expected, actual, maxRelativeError, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(double expected, double actual, double maxRelativeError) {
        TestUtils.assertEqual(expected, actual, maxRelativeError);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(double expected, double actual, double maxRelativeError, String errorMessage) {
        TestUtils.assertEqual(expected, actual, maxRelativeError, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(Object expected, Object actual) {
        TestUtils.assertEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertEqual(Object expected, Object actual, String errorMessage) {
        TestUtils.assertEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(byte expected, byte actual) {
        TestUtils.assertNotEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(byte expected, byte actual, String errorMessage) {
        TestUtils.assertNotEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(short expected, short actual) {
        TestUtils.assertNotEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(short expected, short actual, String errorMessage) {
        TestUtils.assertNotEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(int expected, int actual) {
        TestUtils.assertNotEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(int expected, int actual, String errorMessage) {
        TestUtils.assertNotEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(long expected, long actual) {
        TestUtils.assertNotEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(long expected, long actual, String errorMessage) {
        TestUtils.assertNotEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(float expected, float actual, double minRelativeError) {
        TestUtils.assertNotEqual(expected, actual, minRelativeError);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(float expected, float actual, double minRelativeError, String errorMessage) {
        TestUtils.assertNotEqual(expected, actual, minRelativeError, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(double expected, double actual, double minRelativeError) {
        TestUtils.assertNotEqual(expected, actual, minRelativeError);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(double expected, double actual, double minRelativeError, String errorMessage) {
        TestUtils.assertNotEqual(expected, actual, minRelativeError, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(Object expected, Object actual) {
        TestUtils.assertNotEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertNotEqual(Object expected, Object actual, String errorMessage) {
        TestUtils.assertNotEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(byte[] expected, byte[] actual) {
        TestUtils.assertArrayEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(byte[] expected, byte[] actual, String errorMessage) {
        TestUtils.assertArrayEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(short[] expected, short[] actual) {
        TestUtils.assertArrayEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(short[] expected, short[] actual, String errorMessage) {
        TestUtils.assertArrayEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(int[] expected, int[] actual) {
        TestUtils.assertArrayEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(int[] expected, int[] actual, String errorMessage) {
        TestUtils.assertArrayEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(long[] expected, long[] actual, long maxRelativeError) {
        TestUtils.assertArrayEqual(expected, actual, maxRelativeError);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(long[] expected, long[] actual, long maxRelativeError, String errorMessage) {
        TestUtils.assertArrayEqual(expected, actual, maxRelativeError, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(float[] expected, float[] actual, double maxRelativeError) {
        TestUtils.assertArrayEqual(expected, actual, maxRelativeError);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(float[] expected, float[] actual, double maxRelativeError, String errorMessage) {
        TestUtils.assertArrayEqual(expected, actual, maxRelativeError, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(double[] expected, double[] actual, double maxRelativeError) {
        TestUtils.assertArrayEqual(expected, actual, maxRelativeError);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(double[] expected, double[] actual, double maxRelativeError, String errorMessage) {
        TestUtils.assertArrayEqual(expected, actual, maxRelativeError, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(Object[] expected, Object[] actual) {
        TestUtils.assertArrayEqual(expected, actual);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertArrayEqual(Object[] expected, Object[] actual, String errorMessage) {
        TestUtils.assertArrayEqual(expected, actual, errorMessage);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     *
     * @see TestUtils
     */
    public void assertException(RuntimeException exception, Runnable expression) {
        TestUtils.assertException(exception, expression);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     *
     * @see TestUtils
     */
    public void assertException(RuntimeException exception, Runnable expression, String message) {
        TestUtils.assertException(exception, expression, message);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     *
     * @see TestUtils
     */
    public void assertNoException(Runnable expression) {
        TestUtils.assertNoException(expression);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     *
     * @see TestUtils
     */
    public void assertNoException(Runnable expression, String message) {
        TestUtils.assertNoException(expression, message);
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
     * Waits for a form change and if no form change occurred after a given timeout then fail the test
     */
    public void waitForUnnamedForm() {
        TestUtils.waitForUnnamedForm();
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
    public void assertTitle(String title) {
        TestUtils.assertTitle(title);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertLabel(String name, String text) {
        TestUtils.assertLabel(name, text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertLabel(String text) {
        TestUtils.assertLabel(text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertLabel(int[] path, String text) {
        TestUtils.assertLabel(path, text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertTextArea(String name, String text) {
        TestUtils.assertTextArea(name, text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertTextArea(int[] path, String text) {
        TestUtils.assertTextArea(path, text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void assertTextArea(String text) {
        TestUtils.assertTextArea(text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public TextArea findTextAreaText(String text) {
        return TestUtils.findTextAreaText(text);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void setVerboseMode(boolean v) {
        TestUtils.setVerboseMode(v);
    }

    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void selectInList(String listName, int offset) {
        TestUtils.selectInList(listName, offset);
    }


    /**
     * This method just invokes the test utils method, it is here for convenience
     * @see TestUtils
     */
    public void selectInList(int[] path, int offset) {
        TestUtils.selectInList(path, offset);
    }


    /**
     * Returns all the command objects from the toolbar in the order of left, right, overflow &amp; sidemenu
     * @return the set of commands
     */
    public Command[] getToolbarCommands() {
        return TestUtils.getToolbarCommands();
    }
    
    /**
     * Executes a command from the offset returned by {@link #getToolbarCommands()}
     * 
     * @param offset the offset of the command we want to execute
     */
    public void executeToolbarCommandAtOffset(int offset) {
        TestUtils.executeToolbarCommandAtOffset(offset);
    }
    
    /**
     * Shows the sidemenu UI
     */
    public void showSidemenu() {
        TestUtils.showSidemenu();
    }    
}
