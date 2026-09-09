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

import com.codename1.flutter.Element;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.Widget;

/**
 * A paragraph of mixed-style text described by a {@link TextSpan} tree
 * (Flutter's RichText). The span tree is flattened into styled runs, wrapped
 * across lines with per-run fonts, and custom-painted honoring
 * {@link TextAlign}.
 */
public class RichText extends Widget {

    private TextSpan text;
    private TextAlign textAlign;

    public void text(TextSpan v) {
        this.text = v;
    }

    public void textAlign(TextAlign v) {
        this.textAlign = v;
    }

    public TextSpan getText() {
        return text;
    }

    public TextAlign getTextAlign() {
        return textAlign;
    }

    @Override
    public Element createElement() {
        return new RichTextRenderElement(this);
    }
}
