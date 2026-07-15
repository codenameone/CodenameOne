/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.teavm.jso.io;


import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;
import com.codename1.html5.js.dom.EventListener;

/**
 *
 * @author shannah
 */
public interface FileReader extends JSObject {

    public static final int EMPTY = 0;
    public static final int LOADING = 1;
    public static final int DONE = 2;

    @JSProperty
    public JSObject getError();

    @JSProperty
    public int getReadyState();

    @JSProperty
    public JSObject getResult();

    public void abort();

    public void readAsArrayBuffer(JSObject blob);

    public void readAsBinaryString(JSObject blob);

    public void readAsDataURL(JSObject blob);

    public void readAsText(JSObject blob);

    @JSProperty
    public void setOnabort(EventListener l);

    @JSProperty
    public void setOnerror(EventListener l);

    @JSProperty
    public void setOnload(EventListener l);

    @JSProperty
    public void setOnloadstart(EventListener l);

    @JSProperty
    public void setOnloadend(EventListener l);

    @JSProperty
    public void setOnprogress(EventListener l);
    
    public static interface Factory extends JSObject {
        @JSBody(params={},script="return new FileReader()")
        public FileReader createFileReader();
    }

}
