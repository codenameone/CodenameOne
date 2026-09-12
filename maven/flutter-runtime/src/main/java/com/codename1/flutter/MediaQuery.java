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
 *
 * <h2>Safe area: the one rule</h2>
 *
 * <p>Status bars, notches, display cutouts and home indicators are all the same thing here,
 * and there is exactly one path from the device to the pixels. Nothing else may inset for
 * them, on any platform:</p>
 *
 * <ol>
 * <li>The <b>port</b> answers {@code Form.getSafeArea()}. This is the only platform-specific
 *     input, and it is the only place a platform difference is allowed to exist.</li>
 * <li>{@code MediaQueryData.fromDisplay} converts that rectangle into {@code padding} once,
 *     turning device pixels into logical ones. Everything downstream reads padding and never
 *     asks the port again.</li>
 * <li>{@code FlutterUI.stripChrome} turns Codename One's own safe-area layout OFF
 *     ({@code Container.setSafeArea(false)}). CN1 would otherwise hold the content off the
 *     cutout as well, and the inset would be applied twice -- which shows up as a band of the
 *     Form's colour above everything the app drew.</li>
 * <li>A widget that <b>spends</b> some of the padding removes what it spent for its
 *     descendants, via {@code removePadding}: the Scaffold body drops the top when an AppBar
 *     stands in for it and the bottom under a bottom bar, and a scroll view drops its own
 *     axis after padding its content. {@code SafeArea} then applies whatever is left.</li>
 * </ol>
 *
 * <p>Because every consumer subtracts rather than recomputes, the invariant is that padding
 * is applied exactly once along any path from the root to a leaf. Two things break it, and
 * both have:</p>
 *
 * <ul>
 * <li><b>Insetting outside this chain.</b> Any port-specific "hold it off the status bar"
 *     is a second application. The answer is always to read {@code MediaQuery.padding}.</li>
 * <li><b>Carrying a subtraction across a boundary it does not belong to.</b> A route is
 *     mounted with a fallback ancestor, and using the widget that pushed it let a scroll
 *     view's {@code removePadding} leak into the new page, whose SafeArea then resolved to
 *     zero. Routes anchor at the nearest navigator scope for this reason -- see
 *     {@code Navigator.pushingElement}.</li>
 * </ul>
 *
 * <p>The rule is verified on desktop and iOS by opening a study, pushing a page out of a
 * scrolled list, and checking that the pushed page starts at the inset rather than at zero.</p>
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
                MediaQuery q = context.maybeDependOnInheritedWidgetOfExactType(MediaQuery.class);
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
     * {@code MediaQuery.removePadding}: a subtree that sees the ambient metrics
     * with the selected padding edges already spent.
     */
    public static Widget removePadding(BuildContext context, Boolean removeLeft, Boolean removeTop,
            Boolean removeRight, Boolean removeBottom, Widget child) {
        return scope(of(context).removePadding(
                removeLeft, removeTop, removeRight, removeBottom), child);
    }

    /** A subtree that sees {@code data} instead of whatever is ambient. */
    public static MediaQuery scope(MediaQueryData data, Widget child) {
        MediaQuery q = new MediaQuery();
        q.data(data);
        q.child(child);
        return q;
    }
}
