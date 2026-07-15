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
        // Apply via the CanvasRenderingContext2D INTERFACE (setTransform/transform),
        // not JSAffineTransform.Factory.* -- those are @JSBody natives that do
        // ``context.setTransform(...)`` in raw JS and only work when ``context`` is
        // a real canvas 2D context. In the surface-id model ``context`` is a
        // SurfaceCommandRecorder (a Java object), so the raw-JS call hit an
        // undefined dispatch (RuntimeException: Cannot read properties of undefined
        // (reading 'apply')) and threw -- aborting the whole display drain before
        // graphics.flush(), which left codenameone-canvas on the prior frame and
        // froze every transform/clip/chart test. The interface call records an
        // OP_SET_TRANSFORM/OP_TRANSFORM that the host replays onto the real context.
        // Matrix order mirrors the old @JSBody: setTransform(m00,m10,m01,m11,m02,m12).
        double m00 = t.getScaleX(), m10 = t.getShearY(), m01 = t.getShearX(),
               m11 = t.getScaleY(), m02 = t.getTranslateX(), m12 = t.getTranslateY();
        if (replace) {
            context.setTransform(m00, m10, m01, m11, m02, m12);
        } else {
            context.transform(m00, m10, m01, m11, m02, m12);
        }
    }
    
    @JSBody(params={"context"}, script="console.log(context.currentTransform)")
    private native static void printCurrentTransform(CanvasRenderingContext2D context);

    @Override
    public String getDescription() {
        return "SetTransform";
    }
    
}
