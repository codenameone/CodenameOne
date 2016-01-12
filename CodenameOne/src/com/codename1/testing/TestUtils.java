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

import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.list.ContainerList;
import com.codename1.ui.spinner.BaseSpinner;
import com.codename1.ui.spinner.GenericSpinner;
import com.codename1.ui.util.ImageIO;
import java.io.IOException;

/**
 * Various utility classes to automate UI testing
 *
 * @author Shai Almog
 */
public class TestUtils {
    private static boolean verbose;
    private TestUtils() {}

    /**
     * Activates/deactivates the verbose test mode
     * @param v true for verbosity
     */
    public static void setVerboseMode(boolean v) {
        verbose = v;
    }

    /**
     * Waits for the given number of milliseconds even if the waiting is on the EDT thread
     * @param millis the number of milliseconds to wait
     */
    public static void waitFor(final int millis) {
        if(verbose) {
            log("waitFor(" + millis + ")");
        }
        if(Display.getInstance().isEdt()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException ex) {
                    }
                }
            });
        } else {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Finds a component with the given name, works even with UI's that weren't created with the GUI builder
     * @param componentName the name of the component to find
     * @return the component with the given name within the tree
     */
    public static Component findByName(String componentName) {
        if(verbose) {
            log("findByName(" + componentName + ")");
        }
        Component c = findByName(Display.getInstance().getCurrent(), componentName);
        if(c == null) {
            waitFor(30);
            return findByName(Display.getInstance().getCurrent(), componentName);
        }
        return c;
    }

    /**
     * Selects the given offset in a list
     * @param listName the name of the list component
     * @param offset the offset to select
     */
    public static void selectInList(String listName, int offset) {
        selectListOffset(findByName(listName), offset);
    }

    private static void selectListOffset(Component c, int offset) {
        assertBool(c != null, "List not found");
        if(c instanceof List) {
            ((List)c).setSelectedIndex(offset);
            return;
        }
        if(c instanceof ContainerList) {
            ((ContainerList)c).setSelectedIndex(offset);
            return;
        }
        if(c instanceof GenericSpinner) {
            ((GenericSpinner)c).getModel().setSelectedIndex(offset);
            return;
        }
        assertBool(false, "Unsupported list type: " + c.getName());
    }

    /**
     * Selects the given offset in a list
     * @param listName the name of the list component
     * @param offset the offset to select
     */
    public static void selectInList(int[] path, int offset) {
        selectListOffset(getComponentByPath(path), offset);
    }

    /**
     * Finds a component with the given name, works even with UI's that weren't created with the GUI builder
     * @param componentName the name of the component to find
     * @return the component with the given name within the tree
     */
    private static Component findByName(Container root, String componentName) {
        if(verbose) {
            log("findByName(" + root + ", " + componentName + ")");
        }
        int count = root.getComponentCount();
        for(int iter = 0 ; iter < count ; iter++) {
            Component c = root.getComponentAt(iter);
            String n = c.getName();
            if(n != null && n.equals(componentName)) {
                return c;
            }
            if(c instanceof Container) {
                c = findByName((Container)c, componentName);
                if(c != null) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Finds a component with the given name, works even with UI's that weren't created with the GUI builder
     * @param text the text of the label/button
     * @return the component with the given label text within the tree
     */
    public static Label findLabelText(String text) {
        if(verbose) {
            log("findLabelText(" + text + ")");
        }
        return findLabelText(Display.getInstance().getCurrent(), text);
    }


    /**
     * Finds a component with the given name, works even with UI's that weren't created with the GUI builder
     * @param text the text of the label/button
     * @return the component with the given label text within the tree
     */
    private static Label findLabelText(Container root, String text) {
        if(verbose) {
            log("findLabelText(" + root + ", " + text + ")");
        }
        int count = root.getComponentCount();
        for(int iter = 0 ; iter < count ; iter++) {
            Component c = root.getComponentAt(iter);
            if(c instanceof Label) {
                String n = ((Label)c).getText();
                if(n != null && n.equals(text)) {
                    return (Label)c;
                }
                continue;
            }
            if(c instanceof Container) {
                Label l = findLabelText((Container)c, text);
                if(l != null) {
                    return l;
                }
            }
        }
        return null;
    }

    /**
     * Clicks the button with the given label
     * @param text the text on the button
     */
    public static void clickButtonByLabel(String text) {
        if(verbose) {
            log("clickButtonByLabel(" + text + ")");
        }
        Button b = (Button)findLabelText(text);
        waitFor(20);
        b.pressed();
        waitFor(20);
        b.released();
        waitFor(20);
    }

    /**
     * Clicks the button with the given label
     * @param name the name of the button
     */
    public static void clickButtonByName(String name) {
        if(verbose) {
            log("clickButtonByName(" + name + ")");
        }
        Button b = (Button)findByName(name);
        waitFor(20);
        b.pressed();
        waitFor(20);
        b.released();
        waitFor(20);
    }

    private static String toString(int[] p) {
        if(p == null) {
            return "null";
        }
        if(p.length == 0) {
            return "{}";
        }
        String s = "{" + p[0];
        for(int iter = 1 ; iter < p.length ; iter++) {
            s += ", " + p[iter];

        }
        return s + "}";
    }

    /**
     * Clicks the button with the given component path
     * @param path the path
     */
    public static void clickButtonByPath(int[] path) {
        if(verbose) {
            log("clickButtonByPath(" + toString(path) + ")");
        }
        Button b = (Button)getComponentByPath(path);
        b.pressed();
        waitFor(10);
        b.released();
        waitFor(10);
    }

    /**
     * Executes the back command for the current form, similarly to pressing the back button
     */
    public static void goBack() {
        if(verbose) {
            log("goBack()");
        }
        Form f = Display.getInstance().getCurrent();
        Command c = f.getBackCommand();
        assertBool(c != null, "The current form doesn't have a back command at this moment! for form name " + f.getName());
        f.dispatchCommand(c, new ActionEvent(c));
        waitFor(20);
    }

    /**
     * Executes a menu command with the given name
     * @param name the name of the command
     */
    public static void clickMenuItem(String name) {
        if(verbose) {
            log("clickMenuItem(" + name + ")");
        }
        Form f = Display.getInstance().getCurrent();
        for(int iter = 0 ; iter < f.getCommandCount() ; iter++) {
            Command c = f.getCommand(iter);
            if(name.equals(c.getCommandName())) {
                f.dispatchCommand(c, new ActionEvent(c));
                return;
            }
        }
        throw new RuntimeException("Command not found: " + name);
    }

    /**
     * Scrolls to show the component in case it is invisible currently
     * @param c the component
     */
    public static void ensureVisible(Component c) {
        if(verbose) {
            log("ensureVisible(" + c + ")");
        }
        Form f = Display.getInstance().getCurrent();
        f.scrollComponentToVisible(c);
    }

    /**
     * Scrolls to show the component in case it is invisible currently
     * @param componentName the component
     */
    public static void ensureVisible(String componentName) {
        if(verbose) {
            log("ensureVisible(" + componentName + ")");
        }
        ensureVisible(findByName(componentName));
    }

    /**
     * Scrolls to show the component in case it is invisible currently
     * @param path the path to the component
     */
    public static void ensureVisible(int[] path) {
        if(verbose) {
            log("ensureVisible(" + toString(path) + ")");
        }
        ensureVisible(getComponentByPath(path));
    }

    /**
     * Waits for a form change and if no form change occurred after a given timeout then fail the test
     * @param title the title of the form to wait for
     */
    public static void waitForFormTitle(final String title) {
        if(verbose) {
            log("waitForFormTitle(" + title + ")");
        }
        if(Display.getInstance().isEdt()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    waitForFormTitleImpl(title);
                }
            });
        } else {
            waitForFormTitleImpl(title);
        }
        waitFor(50);
    }

    private static void waitForFormTitleImpl(String title) {
        while(!title.equals(Display.getInstance().getCurrent().getTitle())) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Waits for a form change and if no form change occurred after a given timeout then fail the test
     * @param name the name of the form to wait for
     */
    public static void waitForFormName(final String name) {
        if(verbose) {
            log("waitForFormName(" + name + ")");
        }
        if(Display.getInstance().isEdt()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    waitForFormNameImpl(name);
                }
            });
        } else {
            waitForFormNameImpl(name);
        }
        waitFor(50);
    }

    private static void waitForFormNameImpl(String title) {
        long t = System.currentTimeMillis() + 90000;
        while(!title.equals(Display.getInstance().getCurrent().getName())) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
            if(System.currentTimeMillis() > t) {
                assertBool(false, "Waiting for form " + title + " timed out! Current form name is: " + Display.getInstance().getCurrent().getName());
            }
        }
    }

    /**
     * The screenshot test takes a screenshot of the screen and compares it to
     * a prior screenshot, if both are 100% identical the test passes. If not
     * the test fails.<br>
     * If this is the first time the test is run then the screenshot is taken
     * and saved under the given name in the devices storage. The test passes
     * for this case but a warning is printed to the console. The name will have
     * .png appended to it so it will be identified.<br/>
     * This test will only work on devices that support the ImageIO API with PNG
     * file format.
     *
     * @param screenshotName the name to use for the storage, must be unique!
     * @return true if the screenshots are identical or no prior screenshot exists
     * or if the test can't be run on this device. False if a screenshot exists and
     * it isn't 100% identical.
     */
    public static boolean screenshotTest(String screenshotName) {
        if(verbose) {
            log("screenshotTest(" + screenshotName + ")");
        }
        try {
            ImageIO io = ImageIO.getImageIO();
            if(io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                log("screenshot test skipped due to no image IO support for PNG format");
                return true;
            }

            Image mute = Image.createImage(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
            Display.getInstance().getCurrent().paint(mute.getGraphics());
            screenshotName = screenshotName + ".png";
            if(Storage.getInstance().exists(screenshotName)) {
                int[] rgba = mute.getRGBCached();
                Image orig = Image.createImage(Storage.getInstance().createInputStream(screenshotName));
                int[] origRgba = orig.getRGBCached();
                orig = null;
                for(int iter = 0 ; iter < rgba.length ; iter++) {
                    if(rgba[iter] != origRgba[iter]) {
                        log("screenshots do not match at offset " + iter + " saving additional image under " + screenshotName + ".fail");
                        io.save(mute, Storage.getInstance().createOutputStream(screenshotName + ".fail"), ImageIO.FORMAT_PNG, 1);
                        return false;
                    }
                }
            } else {
                io.save(mute, Storage.getInstance().createOutputStream(screenshotName), ImageIO.FORMAT_PNG, 1);
            }
            return true;
        } catch(IOException err) {
            log(err);
            return false;
        }
    }

    /**
     * Log to the test log
     * @param t the string to log
     */
    public static void log(String t) {
        TestReporting.getInstance().logMessage(t);
    }

    /**
     * Log to the test log
     * @param t exception to log
     */
    public static void log(Throwable t) {
        TestReporting.getInstance().logException(t);
    }


    /**
     * Simulates a device key press
     * @param keyCode the keycode
     */
    public static void keyPress(int keyCode) {
        if(verbose) {
            log("keyPress(" + keyCode + ")");
        }
        Display.getInstance().getCurrent().keyPressed(keyCode);
        waitFor(10);
    }

    /**
     * Simulates a device key release
     * @param keyCode the keycode
     */
    public static void keyRelease(int keyCode) {
        if(verbose) {
            log("keyRelease(" + keyCode + ")");
        }
        Display.getInstance().getCurrent().keyReleased(keyCode);
        waitFor(10);
    }

    /**
     * Simulates a game key press
     * @param gameKey the game key (arrows etc.)
     */
    public static void gameKeyPress(int gameKey) {
        if(verbose) {
            log("gameKeyPress(" + gameKey + ")");
        }
        Display.getInstance().getCurrent().keyPressed(Display.getInstance().getKeyCode(gameKey));
        waitFor(10);
    }

    /**
     * Simulates a game key release
     * @param gameKey the game key (arrows etc.)
     */
    public static void gameKeyRelease(int gameKey) {
        if(verbose) {
            log("gameKeyRelease(" + gameKey + ")");
        }
        Display.getInstance().getCurrent().keyReleased(Display.getInstance().getKeyCode(gameKey));
        waitFor(10);
    }

    /**
     * A component press on a given named component at x/y where x and y are <b>NOT pixels</b>
     * but rather a number between 0 to 1 representing the percentage within the component where the
     * event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
     * @param x the offset within the component as a number between 0 and 1
     * @param y the offset within the component as a number between 0 and 1
     * @param componentName the name of the component
     */
    public static void pointerPress(float x, float y, String componentName) {
        if(verbose) {
            if(componentName == null) {
                log("pointerPress(" + x + ", " + y + ", null)");
            } else {
                log("pointerPress(" + x + ", " + y + ", " + componentName + ")");
            }
        }
        waitFor(20);
        if(componentName != null) {
            Component c = findByName(componentName);
            int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
            int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
            Display.getInstance().getCurrent().pointerPressed(actualX, actualY);
        } else {
            Display.getInstance().getCurrent().pointerPressed((int)(x * Display.getInstance().getDisplayWidth()), 
                    (int)(y * Display.getInstance().getDisplayHeight()));
        }
        waitFor(10);
    }

    /**
     * A component release on a given named component at x/y where x and y are <b>NOT pixels</b>
     * but rather a number between 0 to 1 representing the percentage within the component where the
     * event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
     * @param x the offset within the component as a number between 0 and 1
     * @param y the offset within the component as a number between 0 and 1
     * @param componentName the name of the component
     */
    public static void pointerRelease(float x, float y, String componentName) {
        if(verbose) {
            log("pointerRelease(" + x + ", " + y + ", " + componentName + ")");
        }
        Component c = findByName(componentName);
        int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
        int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
        Display.getInstance().getCurrent().pointerReleased(actualX, actualY);
        waitFor(30);
    }

    /**
     * A component drag on a given named component at x/y where x and y are <b>NOT pixels</b>
     * but rather a number between 0 to 1 representing the percentage within the component where the
     * event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
     * @param x the offset within the component as a number between 0 and 1
     * @param y the offset within the component as a number between 0 and 1
     * @param componentName the name of the component
     */
    public static void pointerDrag(float x, float y, String componentName) {
        if(verbose) {
            log("pointerDrag(" + x + ", " + y + ", " + componentName + ")");
        }
        Component c = findByName(componentName);
        int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
        int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
        Display.getInstance().getCurrent().pointerDragged(actualX, actualY);
    }

    /**
     * A component press on a given named component at x/y where x and y are <b>NOT pixels</b>
     * but rather a number between 0 to 1 representing the percentage within the component where the
     * event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
     * @param x the offset within the component as a number between 0 and 1
     * @param y the offset within the component as a number between 0 and 1
     * @param path the path to the component
     */
    public static void pointerPress(float x, float y, int[] path) {
        if(verbose) {
            log("pointerPress(" + x + ", " + y + ", " + toString(path) + ")");
        }
        Component c = getComponentByPath(path);
        int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
        int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
        Display.getInstance().getCurrent().pointerPressed(actualX, actualY);
        waitFor(10);
    }

    /**
     * A component release on a given named component at x/y where x and y are <b>NOT pixels</b>
     * but rather a number between 0 to 1 representing the percentage within the component where the
     * event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
     * @param x the offset within the component as a number between 0 and 1
     * @param y the offset within the component as a number between 0 and 1
     * @param path the path to the component
     */
    public static void pointerRelease(float x, float y, int[] path) {
        if(verbose) {
            log("pointerRelease(" + x + ", " + y + ", " + toString(path) + ")");
        }
        Component c = getComponentByPath(path);
        int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
        int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
        Display.getInstance().getCurrent().pointerReleased(actualX, actualY);
        waitFor(10);
    }

    /**
     * A component drag on a given named component at x/y where x and y are <b>NOT pixels</b>
     * but rather a number between 0 to 1 representing the percentage within the component where the
     * event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
     * @param x the offset within the component as a number between 0 and 1
     * @param y the offset within the component as a number between 0 and 1
     * @param path the path to the component
     */
    public static void pointerDrag(float x, float y, int[] path) {
        if(verbose) {
            log("pointerDrag(" + x + ", " + y + ", " + toString(path) + ")");
        }
        Component c = getComponentByPath(path);
        int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
        int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
        Display.getInstance().getCurrent().pointerDragged(actualX, actualY);
    }

    /**
     * Gets the component from the current form based on its path. A path is a
     * set of offsets starting from the content pane and moving inwards so a path
     * of { 0, 3 } would mean that the first component within the Content pane (by
     * index) is a Container whose 3rd component (again by index) is the component we
     * want.
     * @param path an array
     * @return a component
     */
    public static Component getComponentByPath(int[] path) {
        Component current = Display.getInstance().getCurrent().getContentPane();
        for(int iter = 0 ; iter < path.length ; iter++) {
            current = ((Container)current).getComponentAt(path[iter]);
        }
        return current;
    }


    /**
     * Sets the text for the given component
     * @param name the name of the component
     * @param text the text to set
     */
    public static void setText(String name, String text) {
        if(verbose) {
            log("setText(" + name + ", " + text + ")");
        }
        Component c = findByName(name);
        if(c instanceof Label) {
            ((Label)c).setText(text);
            return;
        }
        ((TextArea)c).setText(text);
        Display.getInstance().onEditingComplete(c, text);

    }

    /**
     * Sets the text for the given component
     * @param path the path to the component
     * @param text the text to set
     */
    public static void setText(int[] path, String text) {
        if(verbose) {
            log("setText(" + toString(path) + ", " + text + ")");
        }
        Component c = getComponentByPath(path);
        if(c instanceof Label) {
            ((Label)c).setText(text);
            return;
        }
        ((TextArea)c).setText(text);
    }

    /**
     * Assertions allow for simpler test code
     * @param b must be true, otherwise an exception is thrown thus failing the test
     */
    public static void assertBool(boolean b) {
        if(verbose) {
            log("assertBool(" + b + ")");
        }
        if(!b) {
            throw new RuntimeException();
        }
    }

    /**
     * Assertions allow for simpler test code
     * @param b must be true, otherwise an exception is thrown thus failing the test
     */
    public static void assertBool(boolean b, String errorMessage) {
        if(verbose) {
            log("assertBool(" + b + ", " + errorMessage + ")");
        }
        if(!b) {
            log("Assert failed on: " + errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * An assertion that always fails a test.
     */
    public static void fail() {
        if(verbose) {
            log("fail()");
        }
        assertBool(false);
    }

    /**
     *  An assertion that always fails a test.
     * @param errorMessage is a string describing the failure
     */
    public static void fail(String errorMessage) {
        if(verbose) {
            log("fail(" + errorMessage + ")");
        }
        assertBool(false, errorMessage);
    }

    /**
     * Asserts that the given expression evaluates to true
     */
    public static void assertTrue(boolean value) {
        if(verbose) {
            log("assertTrue(" + value + ")");
        }
        assertBool(value);
    }

    /**
     * Asserts that the given expression evaluates to true
     * @param errorMessage is a string describing the failure
     */
    public static void assertTrue(boolean value, String errorMessage) {
        if(verbose) {
            log("assertTrue(" + value + ", " + errorMessage + ")");
        }
        assertBool(value, errorMessage);
    }

    /**
     * Asserts that the given expression evaluates to false
     */
    public static void assertFalse(boolean value) {
        if(verbose) {
            log("assertFalse(" + value + ")");
        }
        assertBool(!value);
    }

    /**
     * Asserts that the given expression evaluates to false
     * @param errorMessage is a string describing the failure
     */
    public static void assertFalse(boolean value, String errorMessage) {
        if(verbose) {
            log("assertFalse(" + value + ", " + errorMessage + ")");
        }
        assertBool(!value, errorMessage);
    }

    /**
     * Asserts that the given expression evaluates to null
     */
    public static void assertNull(Object object) {
        if(verbose) {
            log("assertNull(" + object + ")");
        }
        assertBool(object == null);
    }

    /**
     * Asserts that the given expression evaluates to null
     * @param errorMessage is a string describing the failure
     */
    public static void assertNull(Object object, String errorMessage) {
        if(verbose) {
            log("assertNull(" + object + ", " + errorMessage + ")");
        }
        assertBool(object == null, errorMessage);
    }

    /**
     * Asserts that the given expression does not evaluate to null
     */
    public static void assertNotNull(Object object) {
        if(verbose) {
            log("assertNotNull(" + object + ")");
        }
        assertBool(object != null);
    }

    /**
     * Asserts that the given expression does not evaluate to null
     * @param errorMessage is a string describing the failure
     */
    public static void assertNotNull(Object object, String errorMessage) {
        if(verbose) {
            log("assertNotNull(" + object + ", " + errorMessage + ")");
        }
        assertBool(object != null, errorMessage);
    }

    /**
     * Asserts that the given parameters reference the same object
     */
    public static void assertSame(Object expected, Object actual) {
        if(verbose) {
            log("assertSame(" + expected + ", " + actual + ")");
        }
        assertBool(expected == actual);
    }

    /**
     * Asserts that the given parameters reference the same object
     * @param errorMessage is a string describing the failure
     */
    public static void assertSame(Object expected, Object actual, String errorMessage) {
        if(verbose) {
            log("assertSame(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected == actual, errorMessage);
    }

    /**
     * Asserts that the given parameters do not reference the same object
     */
    public static void assertNotSame(Object expected, Object actual) {
        if(verbose) {
            log("assertNotSame(" + expected + ", " + actual + ")");
        }
        assertBool(expected != actual);
    }

    /**
     * Asserts that the given parameters do not reference the same object
     * @param errorMessage is a string describing the failure
     */
    public static void assertNotSame(Object expected, Object actual, String errorMessage) {
        if(verbose) {
            log("assertNotSame(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected != actual, errorMessage);
    }

    private static void assertRelativeErrorExceeded(float expected, float actual, double minRelativeError) {
        if(verbose) {
            log("assertRelativeErrorExceeded(" + expected + ", " + actual + ", " + minRelativeError + ")");
        }
        double relative_error = Math.abs((expected - actual) / actual) * 100;
        if (relative_error < minRelativeError) {
            assertBool(false);
        }
    }

    private static void assertRelativeErrorExceeded(double expected, double actual, double minRelativeError) {
        if(verbose) {
            log("assertRelativeErrorExceeded("+ expected + ", " + actual + ", " + minRelativeError + ")");
        }
        double relative_error = Math.abs((expected - actual) / actual) * 100;
        if (relative_error < minRelativeError) {
            assertBool(false);
        }
    }

    private static void assertRelativeErrorExceeded(float expected, float actual, double minRelativeError, String errorMessage) {
        if(verbose) {
            log("assertRelativeErrorExceeded(" + expected + ", " + actual + ", " + minRelativeError + ", " + errorMessage + ")");
        }
        double relative_error = Math.abs((expected - actual) / actual) * 100;
        if (relative_error < minRelativeError) {
            assertBool(false, errorMessage);
        }
    }

    private static void assertRelativeErrorExceeded(double expected, double actual, double minRelativeError, String errorMessage) {
        if(verbose) {
            log("assertRelativeErrorExceeded(" + expected + ", " + actual + ", " + minRelativeError + ", " + errorMessage + ")");
        }
        double relative_error = Math.abs((expected - actual) / actual) * 100;
        if (relative_error < minRelativeError) {
            assertBool(false, errorMessage);
        }
    }

    private static void assertRelativeErrorNotExceeded(float expected, float actual, double maxRelativeError) {
        if(verbose) {
            log("assertRelativeErrorNotExceeded(" + expected + ", " + actual + ", " + maxRelativeError + ")");
        }
        double relative_error = Math.abs((expected - actual) / actual) * 100;
        if (relative_error > maxRelativeError) {
            assertBool(false);
        }
    }

    private static void assertRelativeErrorNotExceeded(double expected, double actual, double maxRelativeError) {
        if(verbose) {
            log("assertRelativeErrorNotExceeded("+ expected + ", " + actual + ", " + maxRelativeError + ")");
        }
        double relative_error = Math.abs((expected - actual) / actual) * 100;
        if (relative_error > maxRelativeError) {
            assertBool(false);
        }
    }

    private static void assertRelativeErrorNotExceeded(float expected, float actual, double maxRelativeError, String errorMessage) {
        if(verbose) {
            log("assertRelativeErrorNotExceeded(" + expected + ", " + actual + ", " + maxRelativeError + ", " + errorMessage + ")");
        }
        double relative_error = Math.abs((expected - actual) / actual) * 100;
        if (relative_error > maxRelativeError) {
            assertBool(false, errorMessage);
        }
    }

    private static void assertRelativeErrorNotExceeded(double expected, double actual, double maxRelativeError, String errorMessage) {
        if(verbose) {
            log("assertRelativeErrorNotExceeded(" + expected + ", " + actual + ", " + maxRelativeError + ", " + errorMessage + ")");
        }
        double relative_error = Math.abs((expected - actual) / actual) * 100;
        if (relative_error > maxRelativeError) {
            assertBool(false, errorMessage);
        }
    }

    /**
     * Asserts that the given bytes are equal
     */
    public static void assertEqual(byte expected, byte actual) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected == actual);
    }

    /**
     * Asserts that the given bytes are equal
     * @param errorMessage is a string describing the failure
     */
    public static void assertEqual(byte expected, byte actual, String errorMessage) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected == actual, errorMessage);
    }

    /**
     * Asserts that the given shorts are equal
     */
    public static void assertEqual(short expected, short actual) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected == actual);
    }

    /**
     * Asserts that the given shorts are equal
     * @param errorMessage is a string describing the failure
     */
    public static void assertEqual(short expected, short actual, String errorMessage) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected == actual, errorMessage);
    }

    /**
     * Asserts that the given ints are equal
     */
    public static void assertEqual(int expected, int actual) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected == actual);
    }

    /**
     * Asserts that the given ints are equal
     * @param errorMessage is a string describing the failure
     */
    public static void assertEqual(int expected, int actual, String errorMessage) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected == actual, errorMessage);
    }

    /**
     * Asserts that the given longs are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     */
    public static void assertEqual(long expected, long actual) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected == actual);
    }

    /**
     * Asserts that the given longs are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     * @param errorMessage is a string describing the failure
     */
    public static void assertEqual(long expected, long actual, String errorMessage) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected == actual, errorMessage);
    }

    /**
     * Asserts that the given floats are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     * @param errorMessage is a string describing the failure
     */
    public static void assertEqual(float expected, float actual, double maxRelativeError) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        if (expected != actual) {
            assertRelativeErrorNotExceeded(expected, actual, maxRelativeError);
        }
    }

    /**
     * Asserts that the given floats are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     * @param errorMessage is a string describing the failure
     */
    public static void assertEqual(float expected, float actual, double maxRelativeError, String errorMessage) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        if (expected != actual) {
            assertRelativeErrorNotExceeded(expected, actual, maxRelativeError, errorMessage);
        }
    }

    /**
     * Asserts that the given doubles are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     * @param errorMessage is a string describing the failure
     */
    public static void assertEqual(double expected, double actual, double maxRelativeError) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        if (expected != actual) {
            assertRelativeErrorNotExceeded(expected, actual, maxRelativeError);
        }
    }

    /**
     * Asserts that the given doubles are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     * @param errorMessage is a string describing the failure
     */
    public static void assertEqual(double expected, double actual, double maxRelativeError, String errorMessage) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        if (expected != actual) {
            assertRelativeErrorNotExceeded(expected, actual, maxRelativeError, errorMessage);
        }
    }

    /**
     * Asserts that the given objects are equal using the first object's .equal() method
     */
    public static void assertEqual(Object expected, Object actual) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected.equals(actual));
    }

    /**
     * Asserts that the given objects are equal using the first object's .equal() method
     * @param errorMessage is a string describing the failure
     */
    public static void assertEqual(Object expected, Object actual, String errorMessage) {
        if(verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected.equals(actual), errorMessage);
    }

    /**
     * Asserts that the given bytes are not equal
     */
    public static void assertNotEqual(byte expected, byte actual) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected != actual);
    }

    /**
     * Asserts that the given bytes are not equal
     * @param errorMessage is a string describing the failure
     */
    public static void assertNotEqual(byte expected, byte actual, String errorMessage) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected != actual, errorMessage);
    }

    /**
     * Asserts that the given shorts are not equal
     */
    public static void assertNotEqual(short expected, short actual) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected != actual);
    }

    /**
     * Asserts that the given shorts are not equal
     * @param errorMessage is a string describing the failure
     */
    public static void assertNotEqual(short expected, short actual, String errorMessage) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected != actual, errorMessage);
    }

    /**
     * Asserts that the given ints are not equal
     */
    public static void assertNotEqual(int expected, int actual) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected != actual);
    }

    /**
     * Asserts that the given ints are not equal
     * @param errorMessage is a string describing the failure
     */
    public static void assertNotEqual(int expected, int actual, String errorMessage) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected != actual, errorMessage);
    }

    /**
     * Asserts that the given longs are not equal
     */
    public static void assertNotEqual(long expected, long actual, double minRelativeError) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + minRelativeError + ")");
        }
        if (expected == actual) {
            assertBool(false);
        }
        else {
            assertRelativeErrorExceeded(expected, actual, minRelativeError);
        }
    }

    /**
     * Asserts that the given longs are not equal
     * @param minRelativeError is the minimum allowed error, a value of 1 represents a 1% error.
     * @param errorMessage is a string describing the failure
     */
    public static void assertNotEqual(long expected, long actual, double minRelativeError, String errorMessage) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + minRelativeError + ", " + errorMessage + ")");
        }
        if (expected == actual) {
            TestUtils.assertBool(false, errorMessage);
        }
        else {
            assertRelativeErrorExceeded(expected, actual, minRelativeError, errorMessage);
        }
    }

    /**
     * Asserts that the given doubles are not equal
     * @param minRelativeError is the minimum allowed error, a value of 1 represents a 1% error.
     */
    public static void assertNotEqual(double expected, double actual, double minRelativeError) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + minRelativeError + ")");
        }
        if (expected == actual) {
            TestUtils.assertBool(false);
        }
        else {
            assertRelativeErrorExceeded(expected, actual, minRelativeError);
        }
    }

    /**
     * Asserts that the given doubles are not equal
     * @param minRelativeError is the minimum allowed error, a value of 1 represents a 1% error.
     * @param errorMessage is a string describing the failure
     */
    public static void assertNotEqual(double expected, double actual, double minRelativeError, String errorMessage) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + minRelativeError + ", " + errorMessage + ")");
        }
        if (expected == actual) {
            TestUtils.assertBool(false, errorMessage);
        }
        else {
            assertRelativeErrorExceeded(expected, actual, minRelativeError, errorMessage);
        }
    }

    /**
     * Asserts that the given objects are not equal using the first object's .equal() method
     */
    public static void assertNotEqual(Object expected, Object actual) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ")");
        }
        TestUtils.assertBool(!expected.equals(actual));
    }

    /**
     * Asserts that the given objects are not equal using the first object's .equal() method
     * @param errorMessage is a string describing the failure
     */
    public static void assertNotEqual(Object expected, Object actual, String errorMessage) {
        if(verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        TestUtils.assertBool(!expected.equals(actual), errorMessage);
    }

    /**
     * Pretty-prints a byte array
     */
    private static String arrayToString(byte[] array) {
        StringBuilder result = new StringBuilder("[");
        for(int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Pretty-prints a short array
     */
    private static String arrayToString(short[] array) {
        StringBuilder result = new StringBuilder("[");
        for(int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Pretty-prints a int array
     */
    private static String arrayToString(int[] array) {
        StringBuilder result = new StringBuilder("[");
        for(int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Pretty-prints a long array
     */
    private static String arrayToString(long[] array) {
        StringBuilder result = new StringBuilder("[");
        for(int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Pretty-prints a float array
     */
    private static String arrayToString(float[] array) {
        StringBuilder result = new StringBuilder("[");
        for(int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Pretty-prints a double array
     */
    private static String arrayToString(double[] array) {
        StringBuilder result = new StringBuilder("[");
        for(int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Pretty-prints an object array
     */
    private static String arrayToString(Object[] array) {
        StringBuilder result = new StringBuilder("[");
        for(int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Asserts that the given byte arrays are equal
     */
    public static void assertArrayEqual(byte[] expected, byte[] actual) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index]);
        }
    }

    /**
     * Asserts that the given byte arrays are equal
     * @param errorMessage is a string describing the failure
     */
    public static void assertArrayEqual(byte[] expected, byte[] actual, String errorMessage) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
      if (expected.length != actual.length) {
        TestUtils.assertBool(false);
      }
      for (int index = 0; index < expected.length; ++index) {
        assertEqual(expected[index], actual[index], errorMessage);
      }
    }

    /**
     * Asserts that the given short arrays are equal
     */
    public static void assertArrayEqual(short[] expected, short[] actual) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index]);
        }
    }

    /**
     * Asserts that the given short arrays are equal
     * @param errorMessage is a string describing the failure
     */
    public static void assertArrayEqual(short[] expected, short[] actual, String errorMessage) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
      if (expected.length != actual.length) {
        TestUtils.assertBool(false);
      }
      for (int index = 0; index < expected.length; ++index) {
        assertEqual(expected[index], actual[index], errorMessage);
      }
    }

    /**
     * Asserts that the given int arrays are equal
     */
    public static void assertArrayEqual(int[] expected, int[] actual) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index]);
        }
    }

    /**
     * Asserts that the given int arrays are equal
     * @param errorMessage is a string describing the failure
     */
    public static void assertArrayEqual(int[] expected, int[] actual, String errorMessage) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
      if (expected.length != actual.length) {
        TestUtils.assertBool(false);
      }
      for (int index = 0; index < expected.length; ++index) {
        assertEqual(expected[index], actual[index], errorMessage);
      }
    }

    /**
     * Asserts that the given long arrays are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     */
    public static void assertArrayEqual(long[] expected, long[] actual, double maxRelativeError) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
      if (expected.length != actual.length) {
        TestUtils.assertBool(false);
      }
      for (int index = 0; index < expected.length; ++index) {
        assertEqual(expected[index], actual[index], maxRelativeError);
      }
    }

    /**
     * Asserts that the given long arrays are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     * @param errorMessage is a string describing the failure
     */
    public static void assertArrayEqual(long[] expected, long[] actual, double maxRelativeError, String errorMessage) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
      if (expected.length != actual.length) {
        TestUtils.assertBool(false);
      }
      for (int index = 0; index < expected.length; ++index) {
        assertEqual(expected[index], actual[index], maxRelativeError, errorMessage);
      }
    }

    /**
     * Asserts that the given float arrays are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     */
    public static void assertArrayEqual(float[] expected, float[] actual, double maxRelativeError) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
      if (expected.length != actual.length) {
        TestUtils.assertBool(false);
      }
      for (int index = 0; index < expected.length; ++index) {
        assertEqual(expected[index], actual[index], maxRelativeError);
      }
    }

    /**
     * Asserts that the given double arrays are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     * @param errorMessage is a string describing the failure
     */
    public static void assertArrayEqual(float[] expected, float[] actual, double maxRelativeError, String errorMessage) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
      if (expected.length != actual.length) {
        TestUtils.assertBool(false);
      }
      for (int index = 0; index < expected.length; ++index) {
        assertEqual(expected[index], actual[index], maxRelativeError, errorMessage);
      }
    }

    /**
     * Asserts that the given double arrays are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     */
    public static void assertArrayEqual(double[] expected, double[] actual, double maxRelativeError) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
      if (expected.length != actual.length) {
        TestUtils.assertBool(false);
      }
      for (int index = 0; index < expected.length; ++index) {
        assertEqual(expected[index], actual[index], maxRelativeError);
      }
    }

    /**
     * Asserts that the given double arrays are equal
     * @param maxRelativeError is the maximum allowed error, a value of 1 represents a 1% error.
     * @param errorMessage is a string describing the failure
     */
    public static void assertArrayEqual(double[] expected, double[] actual, double maxRelativeError, String errorMessage) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
      if (expected.length != actual.length) {
        TestUtils.assertBool(false);
      }
      for (int index = 0; index < expected.length; ++index) {
        assertEqual(expected[index], actual[index], maxRelativeError, errorMessage);
      }
    }

    /**
     * Asserts that the given object arrays are equal
     */
    public static void assertArrayEqual(Object[] expected, Object[] actual) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
      if (expected.length != actual.length) {
        TestUtils.assertBool(false);
      }
      for (int index = 0; index < expected.length; ++index) {
        assertEqual(expected[index], actual[index]);
      }
    }

    /**
     * Asserts that the given object arrays are equal
     * @param errorMessage is a string describing the failure
     */
    public static void assertArrayEqual(Object[] expected, Object[] actual, String errorMessage) {
        if(verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
      if (expected.length != actual.length) {
        TestUtils.assertBool(false);
      }
      for (int index = 0; index < expected.length; ++index) {
        assertEqual(expected[index], actual[index], errorMessage);
      }
    }

    /**
     * Asserts that a certain exception is raised
     *
     * @param exception
     * @param expression
     */
    public static void assertException(RuntimeException exception, Runnable expression) {
        if (verbose) {
            log("assertException(" + exception + ", " + expression + ")");
        }
        try {
            expression.run();
        } catch (RuntimeException failure) {
            if (exception.getClass() != failure.getClass()) {
                TestUtils.fail();
            }
            return;
        }
        TestUtils.fail();
    }

    /**
     * Asserts that a certain exception is raised
     *
     * @param exception
     * @param expression
     * @param errorMessage
     */
    public static void assertException(RuntimeException exception, Runnable expression, String errorMessage) {
        if (verbose) {
            log("assertException(" + exception + ", " + expression + ", " + errorMessage + ")");
        }
        try {
            expression.run();
        } catch (RuntimeException failure) {
            if (exception.getClass() != failure.getClass()) {
                TestUtils.fail(errorMessage);
            }
            return;
        }
        TestUtils.fail(errorMessage);
    }

    /**
     * Asserts that no exception is raised
     *
     * @param expression
     */
    public static void assertNoException(Runnable expression) {
        if (verbose) {
            log("assertNoException(" + expression + ")");
        }
        try {
            expression.run();
        } catch (RuntimeException failure) {
            TestUtils.fail();
        }
    }

    /**
     * Asserts that no exception is raised
     *
     * @param expression
     * @param errorMessage
     */
    public static void assertNoException(Runnable expression, String errorMessage) {
        if (verbose) {
            log("assertNoException(" + expression + ", " + errorMessage + ")");
        }
        try {
            expression.run();
        } catch (RuntimeException failure) {
            TestUtils.fail(errorMessage);
        }
    }

    /**
     * Verifies the current title is the same otherwise throws an exception
     * @param title the tile to verify
     */
    public static void assertTitle(String title) {
        if(verbose) {
            log("assertTitle(" + title + ")");
        }
        assertBool(Display.getInstance().getCurrent().getTitle().equals(title), title);
    }

    /**
     * Asserts that we have a label with the given text baring the given name
     * @param name the name of the label
     * @param text the text of the label
     */
    public static void assertLabel(String name, String text) {
        if(verbose) {
            log("assertLabel(" + name + ", " + text + ")");
        }
        Label l = (Label)findByName(name);
        assertBool(l != null, "Null label" + text);
        assertBool(text == l.getText() || text.equals(l.getText()), name + " != " + text);
    }

    /**
     * Asserts that we have a label with the given text baring the given name
     * @param path the path of the label
     * @param text the text of the label
     */
    public static void assertLabel(int[] path, String text) {
        if(verbose) {
            log("assertLabel(" + toString(path) + ", " + text + ")");
        }
        Label l = (Label)getComponentByPath(path);
        assertBool(l != null, "Null label" + text);
        assertBool(text == l.getText() || text.equals(l.getText()), ("" + l.getText()) + " != " + text);
    }

    /**
     * Asserts that we have a label with the given text baring the given name
     * @param text the text of the label
     */
    public static void assertLabel(String text) {
        if(verbose) {
            log("assertLabel(" + text + ")");
        }
        Label l = findLabelText(text);
        assertBool(l != null, "Null label " + text);
    }

    /**
     * Asserts that we have a label with the given text baring the given name
     * @param name the name of the label
     * @param text the text of the label
     */
    public static void assertTextArea(String name, String text) {
        if(verbose) {
            log("assertTextArea(" + name + ", " + text + ")");
        }
        TextArea l = (TextArea)findByName(name);
        assertBool(l != null, "Null area " + text);
        assertBool(l.getText().equals(text), "assertTextArea: " + l.getText() + " != " + text);
    }

    /**
     * Asserts that we have a label with the given text baring the given name
     * @param path the path to the text area
     * @param text the text of the label
     */
    public static void assertTextArea(int[] path, String text) {
        if(verbose) {
            log("assertTextArea(" + toString(path) + ", " + text + ")");
        }
        TextArea l = (TextArea)getComponentByPath(path);
        assertBool(l != null, "Null area " + text);
        assertBool(l.getText().equals(text), "assertTextArea: " + l.getText() + " != " + text);
    }

    /**
     * Asserts that we have a label with the given text baring the given name
     * @param text the text of the label
     */
    public static void assertTextArea(String text) {
        if(verbose) {
            log("assertTextArea(" + text + ")");
        }
        TextArea l = findTextAreaText(text);
        assertBool(l != null, "Null text " + text);
    }

    /**
     * Finds a component with the given name, works even with UI's that weren't created with the GUI builder
     * @param text the text of the label/button
     * @return the component with the given TextArea text within the tree
     */
    public static TextArea findTextAreaText(String text) {
        return findTextAreaText(Display.getInstance().getCurrent(), text);
    }

    /**
     * Finds a component with the given name, works even with UI's that weren't created with the GUI builder
     * @param text the text of the label/button
     * @return the component with the given label text within the tree
     */
    private static TextArea findTextAreaText(Container root, String text) {
        int count = root.getComponentCount();
        for(int iter = 0 ; iter < count ; iter++) {
            Component c = root.getComponentAt(iter);
            if(c instanceof TextArea) {
                String n = ((TextArea)c).getText();
                if(n != null && n.equals(text)) {
                    return (TextArea)c;
                }
                continue;
            }
            if(c instanceof Container) {
                TextArea l = findTextAreaText((Container)c, text);
                if(l != null) {
                    return l;
                }
            }
        }
        return null;
    }
}
