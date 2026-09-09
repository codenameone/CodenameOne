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
 * Display-metric scope, mirroring Flutter's {@code MediaQuery.of(context)}.
 *
 * <p>An in-tree MediaQuery now actually SCOPES its subtree. It used to be decorative:
 * {@code of(context)} ignored the context and always recomputed from the CN1 Display, so a
 * widget that wrapped part of the app to override the metrics — a smaller size, a different
 * text scale, the safe-area padding it wants its children to see — was silently overruled.
 * Flutter's own {@code removePadding}/{@code copyWith} idiom depends on this working.</p>
 *
 * <p>Without an ancestor the metrics still come from the Display, which is the right default
 * for the root of the app.</p>
 */
public class MediaQuery extends com.codename1.flutter.widgets.InheritedWidget {

    private MediaQueryData data;

    public MediaQuery() {
    }

    /** The metrics this scope imposes on its subtree — Flutter's {@code MediaQuery.data}. */
    public void data(MediaQueryData v) {
        this.data = v;
    }

    public MediaQueryData getData() {
        return data;
    }

    @Override
    public boolean updateShouldNotify(com.codename1.flutter.widgets.InheritedWidget oldWidget) {
        return !(oldWidget instanceof MediaQuery) || ((MediaQuery) oldWidget).data != data;
    }

    /** The nearest enclosing scope's metrics, else the Display's. */
    public static MediaQueryData of(BuildContext context) {
        if (context != null) {
            try {
                MediaQuery q = context.dependOnInheritedWidgetOfExactType(MediaQuery.class);
                if (q != null && q.data != null) {
                    return q.data;
                }
            } catch (Throwable t) {
                // fall back to the Display below
            }
        }
        return MediaQueryData.fromDisplay(formOf(context));
    }

    /**
     * The Form the subtree at {@code context} belongs to, or null.
     *
     * <p>Asked instead of {@code Display.getCurrent()} because the first screen is BUILT
     * BEFORE IT IS SHOWN: {@code runApp} mounts the tree into a new Form and shows it
     * afterwards, so during that first build nothing is current and every safe-area
     * lookup answered zero. The gallery's home page then laid its title out under the
     * status bar and its scroll view took no top inset, and the screen only corrected
     * itself if something later re-mounted it -- which a route push does, and which is
     * why this was invisible to any measurement taken after opening a route.</p>
     */
    private static com.codename1.ui.Form formOf(BuildContext context) {
        if (!(context instanceof Element)) {
            return null;
        }
        // WALK UP. An element's own host is not always the Form's: every paint effect --
        // Material, Opacity, Transform, a clip -- owns a nested RenderHost for its
        // subtree, and a nested host does not necessarily carry the Form. Asking only the
        // immediate host therefore answered null for anything under one, the safe area
        // came back as zero, and the page laid its content out under the status bar.
        //
        // That is how the mail study's message view ended up with its title across the
        // clock while the compose page beside it was correct: the message view is opened
        // through an OpenContainer, which wraps the page in a Material, and the compose
        // page is not.
        for (Element e = (Element) context; e != null; e = e.parent()) {
            com.codename1.flutter.rendering.RenderHost h = e.host();
            if (h != null && h.form() != null) {
                return h.form();
            }
        }
        return null;
    }

    /** {@code MediaQuery.sizeOf}: the ambient display size. */
    public static com.codename1.flutter.rendering.Size sizeOf(BuildContext context) {
        return of(context).size();
    }

    /** {@code MediaQuery.paddingOf}: the ambient safe-area padding. */
    public static EdgeInsets paddingOf(BuildContext context) {
        return of(context).padding();
    }

    /**
     * {@code MediaQuery.viewInsetsOf}: the insets intruded by the system (e.g.
     * the on-screen keyboard). This runtime does not model view insets, so the
     * value is zero.
     */
    public static EdgeInsets viewInsetsOf(BuildContext context) {
        return EdgeInsets.zero;
    }

    /**
     * {@code MediaQuery.removePadding}: returns a subtree with the selected
     * padding edges removed from the ambient media query. This runtime does not
     * scope media metrics through the element tree, so the child is returned
     * unchanged (the removed edges are a no-op).
     */
    public static Widget removePadding(BuildContext context, Boolean removeLeft, Boolean removeTop,
            Boolean removeRight, Boolean removeBottom, Widget child) {
        return child;
    }
}
