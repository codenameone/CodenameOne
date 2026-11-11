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
 */
public class TextEditUtil {

    
    /**
     * The currently edited TextArea
     */
    public static Component curEditedComponent;
    public static Component nextEditedComponent;

    /**
     * Sets currently edited textarea.
     *
     * @param current the textarea
     */
    public static void setCurrentEditComponent(Component current) {
        curEditedComponent = current;
    }
    
    public static void setNextEditComponent(Component cmp) {
        nextEditedComponent = cmp;
    }

    /**
     * The cursor position native code reports when editing ended on the Return
     * key or the toolbar's Next button.
     */
    static final int ENDED_BY_RETURN_OR_NEXT = -2;

    /**
     * The cursor position native code reports when editing ended on the
     * toolbar's Done button (METALView keyboardDoneClicked).
     */
    static final int ENDED_BY_DONE = -3;

    /**
     * Whether the component chose its keyboard action with the "nextButton" or
     * "doneButton" client property.
     */
    static boolean choosesKeyboardAction(Component cmp) {
        return cmp.getClientProperty("doneButton") != null || cmp.getClientProperty("nextButton") != null;
    }

    /**
     * The component the keyboard moves to from cmp. "doneButton" answers null
     * whatever follows: the native side derives the Return key, the toolbar
     * button and whether Return advances from "is there a next component", so
     * saying there is none is the one place Done has to be decided.
     *
     * @param cmp the component about to be edited
     * @param following the component after it in the tab order, or null
     */
    static Component nextFor(Component cmp, Component following) {
        return cmp.getClientProperty("doneButton") != null ? null : following;
    }

    /**
     * Whether ending the edit of a component fires its Done listener. Every
     * keyboard ending does, as it always has, with one exception: a field that
     * asked for Next explicitly and has somewhere to go moves on without
     * finishing, which is what Android does with its Next action. Its toolbar's
     * Done button, where it has one, still fires.
     *
     * @param endCode the cursor position native code reported
     * @param edited the component whose edit ended, possibly null
     */
    static boolean firesDoneEvent(int endCode, Component edited) {
        if (edited == null) {
            return false;
        }
        if (endCode == ENDED_BY_DONE) {
            return true;
        }
        if (endCode != ENDED_BY_RETURN_OR_NEXT) {
            return false;
        }
        return edited.getClientProperty("nextButton") == null || isLastEditComponent();
    }

    private static boolean explicitKeyboardAction;

    /**
     * Records whether the component about to be edited chose its keyboard action
     * with the "nextButton" or "doneButton" client property.
     *
     * @param explicit true if it did
     */
    public static void setExplicitKeyboardAction(boolean explicit) {
        explicitKeyboardAction = explicit;
    }

    /**
     * Callback for native code. An explicit action is the developer saying the
     * field must offer that action, so the toolbar carrying it is shown on an
     * iPad too, where it is otherwise left out.
     *
     * @return true if the edited component chose its keyboard action explicitly
     */
    public static boolean isExplicitKeyboardAction() {
        return explicitKeyboardAction;
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
                    if (!(next instanceof TextArea)) {
                        IOSImplementation.foldKeyboard();
                    }
                    next.requestFocus();
                    next.startEditingAsync();
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
        return nextEditedComponent;
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
