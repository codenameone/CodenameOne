/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Dp;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

/** Renders a {@link ShaderMask} subtree offscreen and blends the gradient into it per pixel. */
public class ShaderMaskRenderElement extends EffectRenderElement {
    public ShaderMaskRenderElement(Widget widget) {
        super(widget);
    }

    private ShaderMask w() {
        return (ShaderMask) widget();
    }

    @Override
    protected Widget effectChild() {
        return w().getChild();
    }

    private static double dpr() {
        double d = Dp.scale();
        return d <= 0 ? 1 : d;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        int pw = pane.getWidth();
        int ph = pane.getHeight();
        Object cb = w().shaderCallback;
        if (pw <= 0 || ph <= 0 || !(cb instanceof dart.runtime.Funcs.Func1)) {
            paintChildren.paint(g);
            return;
        }
        double d = dpr();
        Object shader = ((dart.runtime.Funcs.Func1<Object, Object>) cb)
                .call(com.codename1.flutter.Rect.fromLTWH(0, 0, pw / d, ph / d));
        Image layer = shader instanceof com.codename1.flutter.Gradient.GradientShader
                ? layer(pane, paintChildren) : null;
        if (layer == null) {
            paintChildren.paint(g);
            return;
        }
        int[] dst = layer.getRGB();
        int[] src = com.codename1.flutter.GradientRaster.rasterize(
                ((com.codename1.flutter.Gradient.GradientShader) shader).gradient(), pw, ph);
        com.codename1.flutter.BlendMode mode = w().blendMode == null
                ? com.codename1.flutter.BlendMode.modulate : w().blendMode;
        for (int i = 0; i < dst.length && i < src.length; i++) {
            dst[i] = blend(mode, src[i], dst[i]);
        }
        g.drawImage(Image.createImage(dst, pw, ph), pane.getX(), pane.getY());
    }

    /// Porter-Duff and the separable modes ShaderMask is used with; the shader is the
    /// source and the child the destination.
    static int blend(com.codename1.flutter.BlendMode mode, int s, int c) {
        int sa = s >>> 24;
        int da = c >>> 24;
        switch (mode) {
            case srcIn:
                return (s & 0xffffff) | ((sa * da / 255) << 24);
            case dstIn:
                return (c & 0xffffff) | ((da * sa / 255) << 24);
            case dstOut:
                return (c & 0xffffff) | ((da * (255 - sa) / 255) << 24);
            case srcOut:
                return (s & 0xffffff) | ((sa * (255 - da) / 255) << 24);
            case src:
                return s;
            case dst:
                return c;
            case clear:
                return 0;
            case srcATop: {
                int out = 0;
                for (int sh = 0; sh < 24; sh += 8) {
                    int sc = (s >>> sh) & 0xff;
                    int dc = (c >>> sh) & 0xff;
                    out |= ((sc * sa + dc * (255 - sa)) / 255) << sh;
                }
                return out | (da << 24);
            }
            default: {
                // modulate (and the multiplying modes): channel and alpha products.
                int out = 0;
                for (int sh = 0; sh < 32; sh += 8) {
                    out |= ((((s >>> sh) & 0xff) * ((c >>> sh) & 0xff)) / 255) << sh;
                }
                return out;
            }
        }
    }
}
