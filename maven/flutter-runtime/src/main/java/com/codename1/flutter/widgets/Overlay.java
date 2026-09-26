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
import com.codename1.flutter.ComposedElement;
import com.codename1.flutter.Element;
import com.codename1.flutter.StackFit;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * The stack of {@link OverlayEntry} objects floating above a route — Flutter's
 * {@code Overlay}.
 *
 * <p>An overlay is what lets something be drawn over the whole screen without
 * the widget that asked for it having to sit at the top of the tree: dialogs,
 * modal sheets, drag feedback, and the gallery's feature-discovery coach marks
 * all reach the nearest overlay and insert an entry.
 *
 * <p>The whole subsystem used to be inert — {@code of()} answered one shared
 * state whose {@code insert} was an empty method body, so every entry ever
 * created went into a void. Nothing threw, and the only sign was a screen that
 * quietly lacked whatever should have floated above it.
 *
 * <p>Every route mounts inside one (see {@code Navigator}), so {@code of()}
 * finds the overlay belonging to the route the caller is on rather than a
 * process-wide singleton.
 */
public class Overlay extends Widget {

    private DartList<OverlayEntry> initialEntries;
    private Object clipBehavior;
    /** The widget the entries float above — supplied by the runtime, not by the app. */
    private Widget base;

    public void initialEntries(DartList<OverlayEntry> v) {
        this.initialEntries = v;
    }

    public DartList<OverlayEntry> getInitialEntries() {
        return initialEntries;
    }

    public void clipBehavior(Object v) {
        this.clipBehavior = v;
    }

    public Widget getBase() {
        return base;
    }

    /** An overlay hosting {@code base}, which every route is wrapped in. */
    public static Overlay hosting(Widget base) {
        Overlay o = new Overlay();
        o.base = base;
        return o;
    }

    /** Flutter's {@code Overlay.of} — the nearest ancestor overlay's state. */
    public static OverlayState of(BuildContext context, boolean rootOverlay, Object debugRequiredFor) {
        OverlayState s = maybeOf(context, rootOverlay);
        return s == null ? new OverlayState() : s;
    }

    /** Flutter's {@code Overlay.maybeOf}. */
    public static OverlayState maybeOf(BuildContext context, boolean rootOverlay) {
        Element e = context instanceof Element ? (Element) context : null;
        OverlayElement found = null;
        while (e != null) {
            if (e instanceof OverlayElement) {
                found = (OverlayElement) e;
                if (!rootOverlay) {
                    return found.state();
                }
            }
            e = e.ancestor();
        }
        return found == null ? null : found.state();
    }

    @Override
    public Element createElement() {
        return new OverlayElement(this);
    }

    /**
     * Builds the overlay's content: the base with every live entry stacked on
     * top, in insertion order.
     */
    static final class OverlayElement extends ComposedElement {

        private final OverlayState state = new OverlayState();

        OverlayElement(Overlay widget) {
            super(widget);
            state.attach(this);
            Overlay o = widget;
            if (o.initialEntries != null) {
                // Attached as OverlayState.insert attaches, minus the rebuild --
                // the first build is about to include them. Copied in bare, they
                // had no owner: markNeedsBuild() did nothing and remove() marked
                // the entry unmounted without the rebuild that takes it off
                // screen, so an initial entry could never be dismissed.
                for (OverlayEntry entry : o.initialEntries) {
                    if (entry != null && !state.entries().contains(entry)) {
                        entry.attach(state);
                        state.entries().add(entry);
                    }
                }
            }
        }

        OverlayState state() {
            return state;
        }

        @Override
        protected Widget build() {
            Overlay o = (Overlay) widget();
            DartList<Widget> children = new DartList<Widget>();
            if (o.getBase() != null) {
                children.add(o.getBase());
            }
            for (OverlayEntry entry : state.entries()) {
                if (!entry.mounted() || entry.getBuilder() == null) {
                    continue;
                }
                Widget w = entry.getBuilder().call(this);
                if (w != null) {
                    children.add(w);
                }
            }
            if (children.isEmpty()) {
                return null;
            }
            // ALWAYS a Stack, even for a lone base.
            //
            // Returning the base directly when there are no entries and a Stack
            // once there is one changes the widget TYPE of the overlay's child,
            // and a type change is exactly what Widget.canUpdate refuses -- so
            // inserting the first entry replaced the entire route subtree
            // instead of updating it. That is self-defeating for anything that
            // inserts an overlay from inside the tree: the feature-discovery
            // coach mark inserted its entry, the insert tore down the widget
            // that had just inserted it, its animation controllers were
            // disposed mid-run, and the replacement -- built fresh -- saw the
            // app's one-shot "already shown" flag and never inserted anything
            // again. The entry stayed on screen at radius zero.
            Stack stack = new Stack();
            stack.children(children);
            // The entries cover the route, so the stack takes the whole box
            // rather than shrink-wrapping its largest child.
            stack.fit(StackFit.expand);
            return stack;
        }
    }
}
