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

import android.webkit.JavascriptInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A class that is used by executeAndReturnString to be exposed in Javascript so
 * that it can accept return values.
 */
public class AndroidBrowserComponentCallback {

    static final String JS_VAR_NAME = "com_codename1_impl_AndroidImplementation_AndroidBrowserComponent";
    static final String JS_RETURNVAL_VARNAME = "window.com_codename1_impl_AndroidImplementation_AndroidBrowserComponent_returnValue";

    
    public String jsCleanup() {
        if (toClean.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        List<Integer> cleanNow = new ArrayList<Integer>(toClean);
        for (Integer i : cleanNow) {
            sb.append("delete ").append(JS_RETURNVAL_VARNAME).append("[").append(i).append("];");
        }
        toClean.clear();
        return sb.toString();
    }
    
    public String jsInit() {
        StringBuilder sb = new StringBuilder();
        sb.append(JS_RETURNVAL_VARNAME).append("=").append(JS_RETURNVAL_VARNAME).append("||{};");
        return sb.toString();
    }
    
    
    private Map<Integer, String> returnValues = new HashMap<Integer, String>();
    private List<Integer> toClean = new ArrayList<Integer>();
    

    @JavascriptInterface
    public synchronized void addReturnValue(int index, String value) {
        returnValues.put(index, value);
        notifyAll();
    }

    public String getReturnValue(int index) {
        return returnValues.get(index);
    }
    
    
    public boolean isValueSet(int index) {
        return returnValues.containsKey(index);
    }
    
    public boolean isIndexAvailable(int index) {
        return !isValueSet(index) && !toClean.contains(index);
    }

    public void remove(int index) {
        returnValues.remove(index);
        toClean.add(index);
    }
}
