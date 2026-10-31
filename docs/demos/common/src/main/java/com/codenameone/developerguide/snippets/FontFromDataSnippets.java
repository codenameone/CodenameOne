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
package com.codenameone.developerguide.snippets;

import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.ui.CN;
import com.codename1.ui.Font;
import com.codename1.ui.Label;

import java.io.IOException;
import java.io.InputStream;

/// Snippets for creating a font from data at runtime, included by the
/// developer guide's CSS fonts section.
public class FontFromDataSnippets {
    Label fontFromStorage() {
        // tag::font-from-data[]
        Label label = new Label("Handlee");
        Font base = null;
        if (Font.isTrueTypeStreamSupported()) {
            try {
                InputStream in = Storage.getInstance().createInputStream("Handlee-Regular.ttf");
                base = Font.createTrueTypeFontFromData("Handlee", in);
            } catch (IOException err) {
                Log.e(err);
            }
        }
        if (base == null) {
            base = Font.createTrueTypeFont("native:MainRegular");
        }
        label.getAllStyles().setFont(base.derive(CN.convertToPixels(4), Font.STYLE_PLAIN));
        // end::font-from-data[]
        return label;
    }
}
