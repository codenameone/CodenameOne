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
package com.codename1.flutter;

/**
 * An image painted into a {@link BoxDecoration} — Flutter's
 * {@code DecorationImage}. Retained as configuration; painting the decoration
 * image is deferred for this milestone.
 */
public class DecorationImage {

    private ImageProvider image;
    private BoxFit fit;
    private Object alignment;
    private Object colorFilter;
    private Object repeat;
    private double scale = 1.0;
    private double opacity = 1.0;
    private boolean matchTextDirection;

    public void image(ImageProvider v) {
        this.image = v;
    }

    public void fit(BoxFit v) {
        this.fit = v;
    }

    public void alignment(Object v) {
        this.alignment = v;
    }

    public void colorFilter(Object v) {
        this.colorFilter = v;
    }

    public void repeat(Object v) {
        this.repeat = v;
    }

    public void scale(double v) {
        this.scale = v;
    }

    public void opacity(double v) {
        this.opacity = v;
    }

    public void matchTextDirection(boolean v) {
        this.matchTextDirection = v;
    }

    public ImageProvider getImage() {
        return image;
    }

    public BoxFit getFit() {
        return fit;
    }
}
