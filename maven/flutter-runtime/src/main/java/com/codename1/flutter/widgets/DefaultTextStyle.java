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
 * {@link InheritedWidget}.
 *
 * <p>This is the mechanism a container uses to style the text inside it without
 * touching each {@code Text}: an app bar sets one default and its title picks up
 * the colour and weight. While {@link #of(BuildContext)} returned an empty
 * fallback, none of that reached the text, so a themed bar rendered its title in
 * the default ink.</p>
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

    public Boolean getSoftWrap() {
        return softWrap;
    }

    public Integer getMaxLines() {
        return maxLines;
    }

    public Object getOverflow() {
        return overflow;
    }

    /**
     * Nearest ancestor DefaultTextStyle — Flutter's {@code
     * DefaultTextStyle.of(context)}, or an empty one when nothing above sets a
     * default (its {@code getStyle()} is then null, meaning "inherit").
     */
    public static DefaultTextStyle of(BuildContext context) {
        if (context != null) {
            DefaultTextStyle d =
                    context.maybeDependOnInheritedWidgetOfExactType(DefaultTextStyle.class);
            if (d != null) {
                return d;
            }
        }
        return new DefaultTextStyle();
    }

    /** Convenience for the runtime's own wrapping: a default style over a child. */
    public static DefaultTextStyle wrap(TextStyle style, Widget child) {
        DefaultTextStyle d = new DefaultTextStyle();
        d.style(style);
        d.child(child);
        return d;
    }

    @Override
    public boolean updateShouldNotify(com.codename1.flutter.widgets.InheritedWidget oldWidget) {
        if (!(oldWidget instanceof DefaultTextStyle)) {
            return true;
        }
        return ((DefaultTextStyle) oldWidget).style != style;
    }
}
