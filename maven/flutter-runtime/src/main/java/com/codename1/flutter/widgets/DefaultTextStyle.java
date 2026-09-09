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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;

/**
 * The default {@link TextStyle} for descendant {@code Text} widgets that do not
 * supply their own — Flutter's {@code DefaultTextStyle}, an
 * {@link InheritedWidget}. This pass stores the style and text layout hints and
 * renders its single {@code child}; propagating the style into unstyled Text is
 * deferred to the text layer.
 */
public class DefaultTextStyle extends InheritedWidget {

    private TextStyle style;
    private TextAlign textAlign;
    private Boolean softWrap;
    private Object overflow;
    private Integer maxLines;

    public void style(TextStyle v) {
        this.style = v;
    }

    public void textAlign(TextAlign v) {
        this.textAlign = v;
    }

    public void softWrap(boolean v) {
        this.softWrap = v;
    }

    public void overflow(Object v) {
        this.overflow = v;
    }

    public void maxLines(int v) {
        this.maxLines = v;
    }

    public TextStyle getStyle() {
        return style;
    }

    public TextAlign getTextAlign() {
        return textAlign;
    }

    /**
     * Nearest ancestor DefaultTextStyle — Flutter's {@code
     * DefaultTextStyle.of(context)}. Inherited-widget lookup is not yet wired,
     * so this returns an empty fallback whose style is null.
     */
    public static DefaultTextStyle of(BuildContext context) {
        return new DefaultTextStyle();
    }
}
