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
package com.codename1.impl.android;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.impl.VirtualKeyboardInterface;

/**
 *
 * @author cf130546
 */
public class AndroidKeyboard implements VirtualKeyboardInterface {

    private AndroidImplementation impl;

    public AndroidKeyboard(AndroidImplementation impl) {
        this.impl = impl;
    }

    public String getVirtualKeyboardName() {
        return "Android Keyboard";

    }

    public void showKeyboard(boolean show) {
//        manager.restartInput(myView);
//        if (keyboardShowing != show) {
//            manager.toggleSoftInputFromWindow(myView.getWindowToken(), 0, 0);
//            this.keyboardShowing = show;
//        }
        System.out.println("showKeyboard " + show);
        Form current = Display.getInstance().getCurrent();
        if(current != null){
            Component cmp = current.getFocused();
            if(cmp != null && cmp instanceof TextArea){
                TextArea txt = (TextArea)cmp;
                if(show){
                    Display.getInstance().editString(txt, txt.getMaxSize(), txt.getConstraint(), txt.getText(), 0);
                }
            }
        }else{
            InPlaceEditView.endEdit();
        }
//        if(!show){
//            impl.saveTextEditingState();
//        }
    }

    public boolean isVirtualKeyboardShowing() {
        return InPlaceEditView.isEditing();
    }

    public void setInputType(int inputType) {
    }
}
