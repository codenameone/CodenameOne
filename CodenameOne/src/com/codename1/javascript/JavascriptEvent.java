/*
 * Copyright (c) 2012, Steve Hannah/Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.javascript;


import com.codename1.ui.events.ActionEvent;


/**
 * An event that encapsulates a Javascript method call.  When commands are
 * received in a JavascriptContext via the BrowserNavigationCallback mechanism,
 * the requests are parsed and wrapped in a JavascriptEvent, which is then fired
 * and ultimately handled by another event handler to actually call the method.
 * @author shannah
 */
class JavascriptEvent extends ActionEvent {
    
    Object[] args;
    String method;
    public JavascriptEvent(JSObject source, String method, Object[] args){
        super(source,ActionEvent.Type.JavaScript);
        this.args = args;
        this.method = method;
    }
    
    public Object[] getArgs(){
        return args;
    }
    
    public String getMethod(){
        return method;
    }
    
    public JSObject getSelf(){
        return (JSObject)this.getSource();
    }
}
