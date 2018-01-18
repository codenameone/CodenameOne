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

/**
 * Thrown by JavaScript code
 * 
 *  * <p>
 * <strong>NOTE:</strong> The {@link com.codename1.javascript } package is now
 * deprecated. The preferred method of Java/Javascript interop is to use {@link BrowserComponent#execute(java.lang.String) }, {@link BrowserComponent#execute(java.lang.String, com.codename1.util.SuccessCallback) },
 * {@link BrowserComponent#executeAndWait(java.lang.String) }, etc.. as these
 * work asynchronously (except in the XXXAndWait() variants, which use
 * invokeAndBlock() to make the calls synchronously.</p>
 *
 * @author Steve Hannah
 */
class JSException extends RuntimeException {
    public JSException(String msg){
        super(msg);
    }
}