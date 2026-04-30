/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.dom.HTMLInputElement;

/**
 *
 * @author shannah
 */
public class FileChooser {
    
    @JSBody(params={"types"}, script="return jQuery('<input type=\"file\" name=\"pic\" id=\"pic\" accept=\"'+types.join(', ')+'\" />').get(0)")
    private native static HTMLInputElement createFileInput(String[] types);
    
    public String openDialog(String[] allowedTypes) {
        throw new UnsupportedOperationException("openDialog not implemented yet");
    }
    
}
