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
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.util.ImageIO;
import java.io.IOException;

/**
 * Various utility classes to automate UI testing
 *
 * @author Shai Almog
 */
public class TestUtils {
    private TestUtils() {}
    
    /**
     * Waits for the given number of milliseconds even if the waiting is on the EDT thread
     * @param millis the number of milliseconds to wait
     */
    public static void waitFor(final int millis) {
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
        return findByName(Display.getInstance().getCurrent(), componentName);
    }
    
    /**
     * Finds a component with the given name, works even with UI's that weren't created with the GUI builder
     * @param componentName the name of the component to find
     * @return the component with the given name within the tree
     */
    private static Component findByName(Container root, String componentName) {
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
        return findLabelText(Display.getInstance().getCurrent(), text);
    }

    
    /**
     * Finds a component with the given name, works even with UI's that weren't created with the GUI builder
     * @param text the text of the label/button
     * @return the component with the given label text within the tree
     */
    private static Label findLabelText(Container root, String text) {
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
        Button b = (Button)findLabelText(text);
        b.pressed();
        b.released();
    }

    /**
     * Clicks the button with the given label
     * @param name the name of the button
     */
    public static void clickButtonByName(String name) {
        Button b = (Button)findByName(name);
        b.pressed();
        b.released();
    }

    /**
     * Clicks the button with the given component path
     * @param path the path
     */
    public static void clickButtonByPath(int[] path) {
        Button b = (Button)getComponentByPath(path);
        b.pressed();
        b.released();
    }
    
    /**
     * Executes the back command for the current form, similarly to pressing the back button
     */
    public static void goBack() {
        Form f = Display.getInstance().getCurrent();
        Command c = f.getBackCommand();
        if(c != null) {
            f.dispatchCommand(c, new ActionEvent(f));
        }
    }
    
    /**
     * Executes a menu command with the given name
     * @param name the name of the command
     */
    public static void clickMenuItem(String name) {
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
        Form f = Display.getInstance().getCurrent();
        f.scrollComponentToVisible(c);
    }

    /**
     * Scrolls to show the component in case it is invisible currently
     * @param componentName the component
     */
    public static void ensureVisible(String componentName) {
        ensureVisible(findByName(componentName));
    }

    /**
     * Scrolls to show the component in case it is invisible currently
     * @param path the path to the component
     */
    public static void ensureVisible(int[] path) {
        ensureVisible(getComponentByPath(path));
    }
    
    /**
     * Waits for a form change and if no form change occurred after a given timeout then fail the test
     * @param title the title of the form to wait for
     */
    public static void waitForFormTitle(final String title) {
        if(Display.getInstance().isEdt()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    waitForFormTitleImpl(title);
                }
            });
        } else {
            waitForFormTitleImpl(title);
        }
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
        if(Display.getInstance().isEdt()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    waitForFormNameImpl(name);
                }
            });
        } else {
            waitForFormNameImpl(name);
        }
    }

    private static void waitForFormNameImpl(String title) {
        while(!title.equals(Display.getInstance().getCurrent().getTitle())) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
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
        Display.getInstance().getCurrent().keyPressed(keyCode);
    }
    
    /**
     * Simulates a device key release
     * @param keyCode the keycode
     */
    public static void keyRelease(int keyCode) {
        Display.getInstance().getCurrent().keyReleased(keyCode);
    }
    
    /**
     * Simulates a game key press
     * @param gameKey the game key (arrows etc.)
     */
    public static void gameKeyPress(int gameKey) {
        Display.getInstance().getCurrent().keyPressed(Display.getInstance().getKeyCode(gameKey));
    }

    /**
     * Simulates a game key release
     * @param gameKey the game key (arrows etc.)
     */
    public static void gameKeyRelease(int gameKey) {
        Display.getInstance().getCurrent().keyReleased(Display.getInstance().getKeyCode(gameKey));
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
        Component c = findByName(componentName);
        int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
        int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
        Display.getInstance().getCurrent().pointerPressed(actualX, actualY);
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
        Component c = findByName(componentName);
        int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
        int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
        Display.getInstance().getCurrent().pointerReleased(actualX, actualY);
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
        Component c = findByName(componentName);
        int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
        int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
        Display.getInstance().getCurrent().pointerPressed(actualX, actualY);
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
        Component c = getComponentByPath(path);
        int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
        int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
        Display.getInstance().getCurrent().pointerPressed(actualX, actualY);
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
        Component c = getComponentByPath(path);
        int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
        int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
        Display.getInstance().getCurrent().pointerReleased(actualX, actualY);
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
        Component c = getComponentByPath(path);
        int actualX = c.getAbsoluteX() + (int)(x * c.getWidth());
        int actualY = c.getAbsoluteY() + (int)(y * c.getHeight());
        Display.getInstance().getCurrent().pointerPressed(actualX, actualY);
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
        Component c = findByName(name);
        if(c instanceof Label) {
            ((Label)c).setText(text);
            return;
        }
        ((TextArea)c).setText(text);
    }

    /**
     * Sets the text for the given component
     * @param path the path to the component
     * @param text the text to set
     */
    public static void setText(int[] path, String text) {
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
        if(!b) {
            throw new RuntimeException();
        }
    }
    
    /**
     * Verifies the current title is the same otherwise throws an exception
     * @param title the tile to verify
     */
    public static void assertTitle(String title) {
        assertBool(Display.getInstance().getCurrent().getTitle().equals(title));
    }

    /**
     * Asserts that we have a label with the given text baring the given name
     * @param name the name of the label
     * @param text the text of the label
     */
    public static void assertLabel(String name, String text) {
        Label l = (Label)findByName(name);
        assertBool(l != null);
        assertBool(l.getText().equals(text));
    }

    /**
     * Asserts that we have a label with the given text baring the given name
     * @param text the text of the label
     */
    public static void assertLabel(String text) {
        Label l = findLabelText(text);
        assertBool(l != null);
    }

    /**
     * Asserts that we have a label with the given text baring the given name
     * @param name the name of the label
     * @param text the text of the label
     */
    public static void assertTextArea(String name, String text) {
        TextArea l = (TextArea)findByName(name);
        assertBool(l != null);
        assertBool(l.getText().equals(text));
    }

    /**
     * Asserts that we have a label with the given text baring the given name
     * @param text the text of the label
     */
    public static void assertTextArea(String text) {
        TextArea l = findTextAreaText(text);
        assertBool(l != null);
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
