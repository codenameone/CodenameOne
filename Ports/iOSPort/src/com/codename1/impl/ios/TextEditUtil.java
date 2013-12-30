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
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.list.DefaultListModel;

/**
 * Helper method for textfield onscreen keyboard (editStringAt)
 *
 * @author jaanus.hansen@nowinnovations.com
 */
public class TextEditUtil {

    /**
     * The currently edited TextArea
     */
    public static Component curEditedComponent;

    /**
     * Sets currently edited textarea.
     *
     * @param current the textarea
     */
    public static void setCurrentEditComponent(Component current) {
        curEditedComponent = current;
    }

    /**
     * @return if the currently edited textarea is the last component on the
     * form
     */
    public static boolean isLastEditComponent() {
        return getNextEditComponent() == null;
    }

    /**
     * Opens onscreenkeyboard for the next textfield. The method works in EDT if
     * needed.
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
                } else {
                    IOSImplementation.foldKeyboard();
                }
            }
        };
        Display.getInstance().callSerially(task);
    }

    /**
     *
     * @return the next editable TextArea after the currently edited component.
     */
    private static Component getNextEditComponent() {
        Component nextTextArea = null;
        if (curEditedComponent != null) {
            Component next = curEditedComponent.getNextFocusDown();
            if (next == null) {
                next = curEditedComponent.getComponentForm().findNextFocusVertical(true);
            }

            if (next != null && next instanceof TextArea && ((TextArea) next).isEditable() && ((TextArea) next).isEnabled()) {
                nextTextArea = (TextArea) next;
            }
        }
        return nextTextArea;
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
