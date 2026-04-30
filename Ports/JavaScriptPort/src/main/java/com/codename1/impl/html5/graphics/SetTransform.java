/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.teavm.geom.JSAffineTransform;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;

/**
 *
 * @author shannah
 */
public class SetTransform implements ExecutableOp {
    
    private final JSAffineTransform t;
    private final boolean replace;
    
    
    public SetTransform(JSAffineTransform t, boolean replace){
        this.t = t;
        this.replace = replace;
       
    }

    @JSBody(params={"str"}, script="console.log(str)")
    private native static void log(String str);
    
    
    @JSBody(params={"str"}, script="console.log(str)")
    private native static void log(JSObject str);
    @Override
    public void execute(CanvasRenderingContext2D context) {
        if (replace) {
            JSAffineTransform.Factory.setTransform(context, t);
        } else {
            JSAffineTransform.Factory.transform(context, t);
        }
    }
    
    @JSBody(params={"context"}, script="console.log(context.currentTransform)")
    private native static void printCurrentTransform(CanvasRenderingContext2D context);

    @Override
    public String getDescription() {
        return "SetTransform";
    }
    
}
