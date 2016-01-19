/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

/**
 * A version of paint that caches font height for performance
 *
 * @author Shai Almog
 */
public class CodenameOneTextPaint extends TextPaint {
    int fontHeight = -1;
    private int ascent = -1;

    public CodenameOneTextPaint(CodenameOneTextPaint paint) {
        super(paint);
        this.fontHeight = paint.fontHeight;
        this.ascent = ascent;
    }

    public CodenameOneTextPaint(Typeface tf) {
        super.setTypeface(tf);
    }

    @Override
    public Typeface setTypeface(Typeface typeface) {
        throw new RuntimeException("Can't set typeface in runtime!");
    }

    public int getFontAscent() {
        if(ascent < 0) {
            ascent = getFontMetricsInt().ascent;
        }
        return ascent;
    }
    
    public CodenameOneTextPaint() {
    }

    public CodenameOneTextPaint(Paint p) {
        super(p);
    }
}
