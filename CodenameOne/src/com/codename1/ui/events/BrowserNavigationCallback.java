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
package com.codename1.ui.events;

/**
 * <b>Important: Calls to this interface are always performed on a separate
 * thread from the EDT! They are performed on the native webkit rendering
 * thread or native event dispatch thread, this interface MUST NEVER block
 * or synchronize against the EDT which WILL lead to deadlocks. </b><br>
 * This interface can be used to bind functionality to URL navigation
 * which is a very portable way to invoke Java functionality from the
 * JavaScript side of things.<br>
 * This interface should be applied to the BrowserComponent or WebBrowser
 * class.
 * 
 * @author shannah
 */
public interface BrowserNavigationCallback {
    /**
     * <b>Important: Calls to this interface are always performed on a separate
     * thread from the EDT! They are performed on the native webkit rendering
     * thread or native event dispatch thread, this interface MUST NEVER block
     * or synchronize against the EDT which WILL lead to deadlocks. </b><br>
     * This method can be used to bind functionality to URL navigation
     * which is a very portable way to invoke Java functionality from the
     * JavaScript side of things. The method should return true if navigation
     * should occur otherwise it can return false and do any processing it
     * desires with the requested URL.
     * 
     * @param url the URL requested
     * @return true to navigate to the given URL, false to ignore it.
     */
    public boolean shouldNavigate(String url);
}
