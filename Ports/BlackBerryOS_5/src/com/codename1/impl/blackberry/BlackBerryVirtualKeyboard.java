/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.impl.blackberry;

import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.impl.VirtualKeyboardInterface;
import net.rim.device.api.ui.VirtualKeyboard;

/**
 * Interface to the RIM native virtual keyboard
 *
 * @author Chen Fishbein
 */
public class BlackBerryVirtualKeyboard implements VirtualKeyboardInterface{
    static boolean blockFolding;
    private BlackBerryCanvas canvas;
    
    BlackBerryVirtualKeyboard(BlackBerryCanvas canvas) {
        this.canvas = canvas;
    }
    
    
    public String getVirtualKeyboardName() {
        return "BlackBerry VirtualKeyboard";
    }

    public void showKeyboard(boolean show) {
        // we need this since when opening the VKB the text field loses focus and tries to fold the keyboard for some reason
        if(blockFolding && !show) {
            return;
        }
        if(canvas.getVirtualKeyboard() == null){
            return;
        }
        if(show) { 
            if(isVirtualKeyboardShowing()){
                return;
            }
            canvas.getVirtualKeyboard().setVisibility(VirtualKeyboard.SHOW);
            
            Form current = canvas.getImplementation().getCurrentForm();
            Component focus = current.getFocused();
            if(focus != null && focus instanceof TextArea){
                TextArea txtCmp = (TextArea) focus;
                if((txtCmp.getConstraint() & TextArea.NON_PREDICTIVE) == 0){
                    canvas.getImplementation().nativeEdit(txtCmp, txtCmp.getMaxSize(), txtCmp.getConstraint(), txtCmp.getText(), 0);
                }
            }            
        } else {
            canvas.getVirtualKeyboard().setVisibility(VirtualKeyboard.HIDE);
            canvas.getImplementation().finishEdit(true);
        }
    }

    public boolean isVirtualKeyboardShowing() {
        if(canvas.getVirtualKeyboard() == null){
            return false;
        }
        return canvas.getVirtualKeyboard().getVisibility() == VirtualKeyboard.SHOW;
    }

    public void setInputType(int inputType) {
        //no API to select Virtual Keyboard input type
    }

}
