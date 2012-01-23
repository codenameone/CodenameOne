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
package com.codename1.impl;

/**
 * Virtual keyboards needs to implement this interface to be registered as a platform 
 * keyboard.
 * 
 * @author Chen Fishbein
 */
public interface VirtualKeyboardInterface {
    
    /**
     * This can be used to indicate to the VirtualKeyboard what type of input
     * to display.
     * 
     * @param constraint one of TextArea.ANY, TextArea.EMAILADDR, 
     * TextArea.NUMERIC, TextArea.PHONENUMBER, TextArea.URL, TextArea.DECIMAL
     * it can be bitwised or'd with one of TextArea.PASSWORD, 
     * TextArea.UNEDITABLE, TextArea.SENSITIVE, TextArea.NON_PREDICTIVE,
     * INITIAL_CAPS_SENTENCE, INITIAL_CAPS_WORD. E.g. ANY | PASSWORD.
     */
    public void setInputType(int inputType);

    /**
     * Returns the Virtual Keyboard name.
     * This is a unique indentifier for the Virtual Keyboard implementation
     * @return a unique id that represents this virtual keyboard.
     */
    public String getVirtualKeyboardName();
    
    /**
     * Shows or dispose the virtual keyboard
     * @param show if true shows the virtual keyboard
     */
    public void showKeyboard(boolean show);
    
    /**
     * Indicates if the Virtual Keyboard is currently showing.
     * @return true if the Virtual Keyboard is currently showing
     */
    public boolean isVirtualKeyboardShowing();
            
}
