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
package com.codename1.impl.ios;

import com.codename1.ui.ComboBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.list.DefaultListModel;

/**
 * Helper method for textfield onscreen keyboard (editStringAt)
 * @author jaanus.hansen@nowinnovations.com
 */
public class TextEditUtil {
   
    /**
     * The currently edited TextArea
     */
    public static Component curEditedComponent;
   
    /**
     * Sets currently edited textarea.
     * @param current the textarea
     */
    public static void setCurrentEditComponent(Component current) {
        curEditedComponent = current;
    }
   
    /**
     * @return if teh currently edited textarea is the last component
     * on the form
     */
    public static boolean isLastEditComponent() {
        return getNextEditComponent() == null;
    }
           
    /**
     * Opens onscreenkeyboard for the next textfield.
     * The method works in EDT if needed.
     */
    public static void editNextTextArea() {
        Runnable task = new Runnable() {

            public void run() {
                Component next = getNextEditComponent();
                if (next != null) {
                    if (next instanceof TextArea) {
                        TextArea text = (TextArea) next;
                        text.requestFocus();
                        Display.getInstance().editString(next,
                            text.getMaxSize(), text.getConstraint(), text.getText(), 0);
                    }
                }
            }
        };
        Display.getInstance().callSerially(task);
    }
           
    /**
     *
     * @return the next editable TextArea after the currently edited component.
     * CONSIDER: JaanusH: not toally sure if the approach to find the next
     * TextArea by components order
     * is correct, but it has worked for my apps
     */
    private static Component getNextEditComponent() {
        Form currentForm = Display.getInstance().getCurrent();
        return getNextEditComponent(currentForm, new BooleanContainer());
    }
   
    /**
     * Recursive function to find the next editable TextArea.
     * @param container Container which components are iterated to find the
     * next textfield.
     * @param hasFoundActive if false then the method iterates so long as it finds
     *          currently active textfield.
     *              if true then returns as soon as it as iterated to an TextArea.
     * @return
     */
    private static Component getNextEditComponent(Container container, BooleanContainer hasFoundActive) {
        int c = container.getComponentCount();
        for (int i = 0; i < c; i++) {
            Component component = container.getComponentAt(i);
            if (component == curEditedComponent) {
                hasFoundActive.setOn();
            }
            else {
                if (component instanceof Container) {
                    Component result = getNextEditComponent((Container)component, hasFoundActive);
                    if (result != null) {
                        return result;
                    }
                }
                else if (hasFoundActive.isOn() && component instanceof TextArea) {
                    TextArea result = (TextArea) component;
                    if (result.isEditable()) {
                        return result;
                    }
                }
            }
        }
        return null;
    }  
   
}
/**
 * Helper container for mutabale boolean.
 */
class BooleanContainer {

    private boolean on;

    /**
     * Creates a instance which is turned off.
     */
    public BooleanContainer() {
        on = false;
    }

    /**
     * Turns the boolean on.
     */
    public void setOn() {
        this.on = true;
    }

    /**
     *
     * @return if the variable is true
     */
    public boolean isOn() {
        return on;
    }
   
}
