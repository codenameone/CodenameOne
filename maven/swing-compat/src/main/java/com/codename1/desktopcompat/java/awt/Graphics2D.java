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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.geom.AffineTransform;
import com.codename1.desktopcompat.java.awt.image.ImageObserver;

import java.util.Map;

/// A graphics with shapes, strokes, paints and an affine transform.
public abstract class Graphics2D extends Graphics {

    protected Graphics2D() {
    }

    @Override
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        Paint p = getPaint();
        Color c = getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        setColor(raised ? brighter : darker);
        fillRect(x, y, 1, height + 1);
        fillRect(x + 1, y, width - 1, 1);
        setColor(raised ? darker : brighter);
        fillRect(x + 1, y + height, width, 1);
        fillRect(x + width, y, 1, height);
        setPaint(p);
    }

    @Override
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        Paint p = getPaint();
        Color c = getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        if (!raised) {
            setColor(darker);
        } else if (p != c) {
            setColor(c);
        }
        fillRect(x + 1, y + 1, width - 2, height - 2);
        setColor(raised ? brighter : darker);
        fillRect(x, y, 1, height);
        fillRect(x + 1, y, width - 2, 1);
        setColor(raised ? darker : brighter);
        fillRect(x + 1, y + height - 1, width - 1, 1);
        fillRect(x + width - 1, y, 1, height - 1);
        setPaint(p);
    }

    public abstract void draw(Shape s);

    public abstract boolean drawImage(Image img, AffineTransform xform, ImageObserver obs);

    @Override
    public abstract void drawString(String str, int x, int y);

    public abstract void drawString(String str, float x, float y);

    public abstract void fill(Shape s);

    public abstract void setComposite(Composite comp);

    public abstract void setPaint(Paint paint);

    public abstract void setStroke(Stroke s);

    public abstract void setRenderingHint(RenderingHints.Key hintKey, Object hintValue);

    public abstract Object getRenderingHint(RenderingHints.Key hintKey);

    public abstract void setRenderingHints(Map<?, ?> hints);

    public abstract void addRenderingHints(Map<?, ?> hints);

    public abstract RenderingHints getRenderingHints();

    @Override
    public abstract void translate(int x, int y);

    public abstract void translate(double tx, double ty);

    public abstract void rotate(double theta);

    public abstract void rotate(double theta, double x, double y);

    public abstract void scale(double sx, double sy);

    public abstract void shear(double shx, double shy);

    public abstract void transform(AffineTransform Tx);

    public abstract void setTransform(AffineTransform Tx);

    public abstract AffineTransform getTransform();

    public abstract Paint getPaint();

    public abstract Composite getComposite();

    public abstract void setBackground(Color color);

    public abstract Color getBackground();

    public abstract Stroke getStroke();

    public abstract void clip(Shape s);
}
