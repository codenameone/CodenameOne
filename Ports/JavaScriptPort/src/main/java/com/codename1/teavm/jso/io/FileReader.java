/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
