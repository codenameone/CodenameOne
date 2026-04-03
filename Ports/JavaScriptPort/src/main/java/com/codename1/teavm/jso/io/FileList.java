/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.jso.io;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;

/**
 *
 * @author shannah
 */
public interface FileList extends JSObject {
    public Blob item(int index);
    
    @JSProperty
    public int getLength();
    
    
}
