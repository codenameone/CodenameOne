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

import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 * One DOM mutation of the text layer, recorded into the surface command stream.
 *
 * <p>The text layer renders Codename One text as real DOM above the canvas, so every frame has
 * two halves: the pixels the canvas draws and the elements the layer positions. Those halves
 * are only ever seen together if they are applied together. Issuing the DOM half where it is
 * decided -- inside the component paint -- does the opposite: the paint records canvas commands
 * that are not replayed until the frame is drained an animation frame later, so the browser
 * composites the new DOM over the previous frame's pixels. Text released early leaves a gap
 * where the glyphs still ought to be; text added early draws it twice, once in each place.</p>
 *
 * <p>Carrying the mutation as an ordinary {@link ExecutableOp} closes that window by
 * construction. It is queued into the same buffer as the draws around it, replayed into the
 * same {@link SurfaceCommandRecorder}, shipped in the same flush message, and applied by the
 * host inside the same task that replays the canvas commands -- in the draw order the paint
 * gave it. No rendering opportunity exists between the two halves for the compositor to take.
 * {@code OP_BLUR_SELF_REGION} rides the stream for the same reason.</p>
 *
 * <p>Only the recorder understands these ops. Executed against anything else -- an offscreen
 * surface's immediate context, which never carries promoted text -- it does nothing.</p>
 *
 * @author Codename One
 */
public final class TextLayerOp implements ExecutableOp {
    private final int kind;
    private final Object target;
    private final Object child;
    private final String value;

    /**
     * Creates a recorded text-layer mutation.
     *
     * @param kind one of the {@code SurfaceCommandRecorder.OP_TEXT_*} opcodes
     * @param target the element the mutation applies to, as a host reference
     * @param child the element being attached or detached, null for the other kinds
     * @param value the CSS declaration or text content, null for the other kinds
     */
    public TextLayerOp(int kind, Object target, Object child, String value) {
        this.kind = kind;
        this.target = target;
        this.child = child;
        this.value = value;
    }

    @Override
    public void execute(CanvasRenderingContext2D ctx) {
        if (ctx instanceof SurfaceCommandRecorder) {
            ((SurfaceCommandRecorder)ctx).textLayerMutation(kind, target, child, value);
        }
    }

    @Override
    public String getDescription() {
        return "TextLayerOp(" + kind + ")";
    }
}
