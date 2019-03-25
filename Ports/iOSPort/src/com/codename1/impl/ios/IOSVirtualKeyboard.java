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

import com.codename1.impl.VirtualKeyboardInterface;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;

/**
 *
 * @author shannah
 */
public class IOSVirtualKeyboard implements VirtualKeyboardInterface {
    private IOSImplementation impl;
    
    public IOSVirtualKeyboard(IOSImplementation impl) {
        this.impl = impl;
    }
    
    @Override
    public void setInputType(int inputType) {
        Log.p("setInputType not supported on this keyboard");
    }

    @Override
    public String getVirtualKeyboardName() {
        return "IOS Keyboard";
    }

    @Override
    public void showKeyboard(boolean show) {
        if (show) {
            if (isVirtualKeyboardShowing()) {
                return;
            }
            Form f = CN.getCurrentForm();
            if (f != null) {
                Component focused = f.getFocused();
                if (focused != null && focused.isEditable() && focused.isEnabled() && focused.isVisible() && !focused.isEditing()) {
                    focused.startEditingAsync();
                }
            }
        } else {
            if (!isVirtualKeyboardShowing()) {
                return;
            }
            impl.stopTextEditing();
        }
    }

    @Override
    public boolean isVirtualKeyboardShowing() {
        return impl.keyboardShowing;
    }
    
}
