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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.InheritedWidget;

/**
 * Establishes an ambient {@link IconThemeData} for its subtree — Flutter's
 * {@code IconTheme}. Descendant {@code Icon}s read {@code IconTheme.of(context)}
 * for their default size and colour.
 *
 * <p>This is how an app bar tints its glyphs. The bar sets one icon theme and
 * every icon below it picks the colour up; nothing hands each icon a colour
 * individually. While {@link #of(BuildContext)} answered a fresh default, that
 * whole mechanism was inert: the gallery's white-on-purple bars rendered black
 * glyphs, because a Codename One style does not inherit a foreground colour
 * from a parent container the way the ambient theme is expected to.</p>
 */
public class IconTheme extends InheritedWidget {

    private IconThemeData data;

    public void data(IconThemeData v) {
        this.data = v;
    }

    public IconThemeData getData() {
        return data;
    }

    /**
     * Dart's {@code IconTheme.of(context)}: the nearest ambient icon theme,
     * falling back to the material theme's, then to an empty one.
     *
     * <p>Merged down the chain, as Flutter does: an {@code IconTheme.merge}
     * that only sets a colour must not erase the size an outer theme set.</p>
     */
    public static IconThemeData of(BuildContext context) {
        IconThemeData resolved = null;
        if (context != null) {
            IconTheme t = context.maybeDependOnInheritedWidgetOfExactType(IconTheme.class);
            if (t != null) {
                resolved = t.data;
            }
        }
        // Only walk the tree a second time for the material theme when the
        // nearer icon theme actually leaves something to inherit. Every Icon in
        // the app calls this on every update, and each lookup is a walk to the
        // root — asking for the theme unconditionally doubled that for no gain.
        if (resolved != null && resolved.color() != null && resolved.size() != null) {
            return resolved;
        }
        IconThemeData themed = null;
        try {
            themed = Theme.of(context).iconTheme();
        } catch (Throwable t) {
            // no ambient material theme
        }
        if (resolved == null) {
            return themed != null ? themed : new IconThemeData();
        }
        if (themed == null) {
            return resolved;
        }
        return merged(themed, resolved);
    }

    /** {@code over} wins field by field; anything it leaves null falls through to {@code under}. */
    private static IconThemeData merged(IconThemeData under, IconThemeData over) {
        IconThemeData out = new IconThemeData();
        out.color(over.color() != null ? over.color() : under.color());
        Double size = over.size() != null ? over.size() : under.size();
        if (size != null) {
            out.size(size.doubleValue());
        }
        Double opacity = over.opacity() != null ? over.opacity() : under.opacity();
        if (opacity != null) {
            out.opacity(opacity.doubleValue());
        }
        return out;
    }

    /** Dart's {@code IconTheme.merge(...)} named constructor. */
    public static IconTheme merge(Key key, IconThemeData data, Widget child) {
        IconTheme t = new IconTheme();
        t.key(key);
        t.data(data);
        t.child(child);
        return t;
    }

    /** Convenience for the runtime's own wrapping: an icon theme of one colour. */
    public static IconTheme tint(com.codename1.flutter.Color color, Widget child) {
        IconThemeData d = new IconThemeData();
        d.color(color);
        IconTheme t = new IconTheme();
        t.data(d);
        t.child(child);
        return t;
    }

    @Override
    public boolean updateShouldNotify(InheritedWidget oldWidget) {
        if (!(oldWidget instanceof IconTheme)) {
            return true;
        }
        IconThemeData was = ((IconTheme) oldWidget).data;
        if (was == data) {
            return false;
        }
        if (was == null || data == null) {
            return true;
        }
        return !sameColor(was.color(), data.color()) || !sameSize(was.size(), data.size());
    }

    private static boolean sameColor(com.codename1.flutter.Color a, com.codename1.flutter.Color b) {
        return a == b || (a != null && b != null && a.value() == b.value());
    }

    private static boolean sameSize(Double a, Double b) {
        return a == b || (a != null && b != null && a.doubleValue() == b.doubleValue());
    }
}
