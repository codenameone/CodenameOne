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
import com.codename1.flutter.StatelessElement;
import com.codename1.flutter.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Element for an {@link InheritedWidget}: remembers who read it, and rebuilds them when it
 * changes — Flutter's {@code InheritedElement}.
 *
 * <p>Without this, {@code dependOnInheritedWidgetOfExactType} is a plain ancestor search: a
 * consumer gets the value that was current the first time it built and never hears about
 * another. Everything context-delivered depends on it — the gallery's options model, the
 * theme, provider — so a setting could be changed and nothing that read it would notice.</p>
 *
 * <p><b>Order matters.</b> Dependents are notified AFTER {@code super.update} has swapped
 * the widget <i>and</i> rebuilt this element's own subtree, which is where Flutter puts it
 * ({@code ProxyElement.update} runs {@code updated()} then rebuilds; the notification takes
 * effect on the dependents' own rebuild). Notifying first — before this element's child
 * tree has been rebuilt — marks dependents dirty against a tree that is about to be
 * replaced underneath them, and the elements they rebuilt into are unmounted moments later.
 * That renders as whole pages going blank, with no error anywhere, and it passes every
 * headless test.</p>
 */
public class InheritedElement extends StatelessElement {

    private final List<Element> dependents = new ArrayList<Element>();

    public InheritedElement(InheritedWidget widget) {
        super(widget);
    }

    /**
     * Adds itself to what its subtree can see (see Element.publishAsInherited) BEFORE the
     * first build mounts that subtree. It used to happen after mount returned, by which
     * time the initial descendants had already copied the parent's map without this
     * widget in it. Under a root with no map the lookups fell back to walking ancestors
     * and found it anyway, which is why a headless test never noticed; under a real app,
     * whose map is never empty, a nested inherited widget was invisible to its children.
     */
    @Override
    protected void firstBuild() {
        publishAsInherited();
        super.firstBuild();
    }

    /** Registers {@code e} as reading this widget; idempotent, since a rebuild re-reads. */
    public void addDependent(Element e) {
        if (e != null && !dependents.contains(e)) {
            dependents.add(e);
        }
    }

    public void removeDependent(Element e) {
        dependents.remove(e);
    }

    /** How many elements currently depend on this one; for tests. */
    int dependentCount() {
        return dependents.size();
    }

    @Override
    public void update(Widget newWidget) {
        Widget old = widget();
        super.update(newWidget);
        if (old == newWidget || !(old instanceof InheritedWidget)
                || !(newWidget instanceof InheritedWidget)) {
            return;
        }
        if (((InheritedWidget) newWidget).updateShouldNotify((InheritedWidget) old)) {
            notifyDependents();
        }
    }

    /**
     * Marks the readers dirty — but not before the build that changed this widget has
     * finished.
     *
     * <p>This runs from inside {@code update()}, which is itself inside a build flush that
     * is part-way through rebuilding this subtree. Marking a dependent dirty at that moment
     * puts it back in the queue while its ancestors are still being replaced around it, and
     * the flush then rebuilds it against a tree that is torn down underneath it: the page
     * renders blank, with no error, and every headless test still passes. Deferring to a
     * serial call means the whole tree is consistent before any reader is asked to rebuild,
     * which is the same guarantee {@code setState} already relies on.</p>
     */
    /// Whether {@code e}'s widget is named in a comma-separated allow-list. Used to notify
    /// readers a few at a time while working out which rebuild is destructive - the census
    /// says WHO reads the value, this says which of them get told.
    private static boolean matches(Element e, String csv) {
        Widget w = e.widget();
        if (w == null) {
            return false;
        }
        // Scanned by hand rather than with String.split: ParparVM's Java API does not
        // carry the regex-based String methods, so split compiles on the desktop and fails
        // the iOS translation with an undeclared-function error.
        String n = w.getClass().getSimpleName();
        int from = 0;
        while (from <= csv.length()) {
            int comma = csv.indexOf(',', from);
            int end = comma < 0 ? csv.length() : comma;
            if (n.equals(csv.substring(from, end).trim())) {
                return true;
            }
            if (comma < 0) {
                break;
            }
            from = comma + 1;
        }
        return false;
    }

    private void notifyDependents() {
        if (dependents.isEmpty()) {
            return;
        }
        // Snapshot: a dependent's rebuild re-runs its lookups and re-registers, mutating
        // this list.
        final List<Element> snapshot = new ArrayList<Element>(dependents);
        Runnable mark = new Runnable() {
            @Override
            public void run() {
                String only = com.codename1.ui.Display.isInitialized()
                        ? com.codename1.ui.Display.getInstance()
                                .getProperty("cn1.flutter.inheritedOnly", "")
                        : "";
                for (int i = 0; i < snapshot.size(); i++) {
                    Element e = snapshot.get(i);
                    // Re-checked here, not at snapshot time: an element that left the tree
                    // in the meantime must never be scheduled.
                    if (!e.isMounted()) {
                        dependents.remove(e);
                        continue;
                    }
                    if (only != null && only.length() > 0 && !matches(e, only)) {
                        continue;
                    }
                    e.didChangeDependencies();
                }
            }
        };
        if (!com.codename1.ui.Display.isInitialized()) {
            mark.run();   // headless tests drive the flush themselves
            return;
        }
        if ("true".equals(com.codename1.ui.Display.getInstance()
                .getProperty("cn1.flutter.inheritedCensus", "false"))) {
            StringBuilder who = new StringBuilder();
            for (int i = 0; i < snapshot.size() && i < 12; i++) {
                if (who.length() > 0) {
                    who.append(", ");
                }
                Widget w = snapshot.get(i).widget();
                who.append(w == null ? "?" : w.getClass().getSimpleName());
            }
            com.codename1.flutter.FlutterErrorReport.unimplemented("InheritedNotify",
                    snapshot.size() + " dependents of "
                    + (widget() == null ? "?" : widget().getClass().getSimpleName())
                    + ": " + who);
        }
        // Deferred, not inline: this runs from update(), part-way through a build flush
        // that is still replacing this subtree, and a reader marked dirty at that moment
        // rebuilds against a tree being torn down around it.
        //
        // This was gated off for a while because turning it on emptied whole pages. That
        // turned out not to be a fault of the notification at all: a rebuilt subtree can
        // contain scroll and effect panes carrying RenderHosts of their own, and the build
        // flush only revalidated the host the rebuilt element was mounted into - so a
        // nested host kept children it never laid out. Fixed in BuildOwner, and readers are
        // notified normally now.
        com.codename1.ui.CN.callSerially(mark);
    }
}
