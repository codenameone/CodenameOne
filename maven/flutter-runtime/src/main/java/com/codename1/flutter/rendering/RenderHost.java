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
package com.codename1.flutter.rendering;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.Layout;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * The bridge between a Flutter element subtree and the single flat CN1
 * {@link Container} that hosts its leaf components. Every element carries a
 * reference to its host; render elements attach/detach their CN1 component
 * here and the host's {@link FlutterRootLayout} drives the constraint pass.
 *
 * <p>All CN1 references are optional so the reconciler and layout algorithms
 * can be exercised in headless unit tests (no Display, no components).</p>
 */
public class RenderHost {

    private Container container;
    private Form form;
    private Toolbar toolbar;
    private boolean toolbarTitleHost;
    private Funcs.Func0<Element> rootSupplier;

    public Container container() {
        return container;
    }

    public void container(Container container) {
        this.container = container;
    }

    /**
     * The CN1 Form hosting this subtree; only set on the main host created by
     * {@code FlutterUI.runApp} (null under {@code FlutterUI.wrap} and tests).
     */
    public Form form() {
        return form;
    }

    public void form(Form form) {
        this.form = form;
    }

    /**
     * The Toolbar this host renders into, when this host is the title area of
     * a root Scaffold's app bar.
     */
    public Toolbar toolbar() {
        return toolbar;
    }

    public void toolbar(Toolbar toolbar) {
        this.toolbar = toolbar;
    }

    private boolean formToolbarBound;

    /**
     * Whether a root-mode Scaffold in this host's tree claimed the Form's
     * Toolbar for its AppBar.
     *
     * <p>A route whose Scaffold is nested (inside a ColoredBox, say) renders
     * its AppBar as an in-canvas strip instead, and then the Form must show no
     * Toolbar at all — otherwise the page carries two bars and CN1's toolbar
     * inset shrinks the Flutter canvas.</p>
     */
    public boolean isFormToolbarBound() {
        return formToolbarBound;
    }

    public void formToolbarBound(boolean v) {
        this.formToolbarBound = v;
    }

    public boolean isToolbarTitleHost() {
        return toolbarTitleHost;
    }

    public void toolbarTitleHost(boolean v) {
        this.toolbarTitleHost = v;
    }

    /**
     * Supplies the root element of the subtree this host displays. A supplier
     * (not a fixed element) because reconciliation may replace the element.
     */
    public void rootSupplier(Funcs.Func0<Element> supplier) {
        this.rootSupplier = supplier;
    }

    public void rootElement(final Element e) {
        this.rootSupplier = new Funcs.Func0<Element>() {
            @Override
            public Element call() {
                return e;
            }
        };
    }

    public Element rootElement() {
        return rootSupplier == null ? null : rootSupplier.call();
    }

    /**
     * The first render element at or below the root element — the entry point
     * of the layout pass.
     */
    public RenderElement rootRenderElement() {
        return RenderElement.findRenderElement(rootElement());
    }

    // ------------------------------------------------------------------
    // Component plumbing (no-ops when headless)
    // ------------------------------------------------------------------

    private final List<RenderElement> attachOrder = new ArrayList<RenderElement>();
    private int insertionCursor = -1;

    /**
     * The component-owning render elements attached to this host, in flat
     * container order (mirrors the CN1 container's child order at runtime;
     * observable headless for unit tests).
     */
    public List<RenderElement> attachOrder() {
        return attachOrder;
    }

    /**
     * Points the attach cursor at a flat-container index: subsequent
     * {@link #attach} calls insert sequentially there instead of appending.
     * Used by {@code Element.updateChild} to drop a replacement subtree's
     * components into the slots the replaced subtree occupied, keeping the
     * container's z-order aligned with element-tree order. Returns the
     * previous cursor for restoration via {@link #endInsertion}.
     */
    public int beginInsertion(int index) {
        int prev = insertionCursor;
        insertionCursor = index;
        return prev;
    }

    public void endInsertion(int previous) {
        insertionCursor = previous;
    }

    /**
     * The flat-container index of the first component in the given element
     * subtree that is attached to this host, or -1.
     */
    public int firstAttachIndex(Element subtree) {
        if (subtree == null) {
            return -1;
        }
        if (subtree instanceof RenderElement) {
            int i = attachOrder.indexOf(subtree);
            if (i >= 0) {
                return i;
            }
        }
        final int[] found = {-1};
        subtree.visitChildren(new Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element c) {
                if (found[0] < 0) {
                    found[0] = firstAttachIndex(c);
                }
            }
        });
        return found[0];
    }

    /**
     * Attaches an element's component at the insertion cursor (advancing it)
     * or, without an active cursor, at the end — mount order is depth-first,
     * so appending preserves element-tree order for fresh subtrees.
     */
    public void attach(RenderElement owner) {
        if (owner == null) {
            return;
        }
        int index = insertionCursor >= 0
                ? Math.min(insertionCursor, attachOrder.size())
                : attachOrder.size();
        attachOrder.add(index, owner);
        Component c = owner.component();
        if (container != null && c != null) {
            container.addComponent(Math.min(index, container.getComponentCount()), c);
        }
        if (insertionCursor >= 0) {
            insertionCursor = index + 1;
        }
    }

    public void detach(RenderElement owner) {
        if (owner == null) {
            return;
        }
        int index = attachOrder.indexOf(owner);
        if (index >= 0) {
            attachOrder.remove(index);
            if (insertionCursor > index) {
                insertionCursor--;
            }
        }
        Component c = owner.component();
        if (c == null) {
            return;
        }
        // Remove it from WHATEVER container holds it, not only from this host's own.
        //
        // An element's component does not always sit in the container of the host that
        // attached it: a scroll view puts its content pane inside its ScrollPane, and
        // other effects re-parent as well. Removing it only when the parent happened to
        // be this host's container meant an unmounted element could keep a live
        // component on screen -- deleting a row from a list left the whole previous
        // content pane attached beside the new one, so every surviving row drew twice
        // and the deleted row was still there.
        com.codename1.ui.Container parent = c.getParent();
        if (parent != null) {
            parent.removeComponent(c);
        }
    }

    /**
     * Moves the given attach entries (a block of already-attached elements,
     * e.g. one parent's child subtrees flattened in NEW tree order) so they
     * appear in that order, both in {@link #attachOrder} and in the CN1
     * container — keyed reconciliation can match surviving children at new
     * indices, and without this their components would keep the OLD paint
     * order. Entries not in {@code desired} are untouched; the block is
     * re-inserted at the first index it currently occupies.
     */
    public void reorderToTreeOrder(List<RenderElement> desired) {
        if (desired.size() < 2) {
            return;
        }
        java.util.Set<RenderElement> members = new java.util.HashSet<RenderElement>(desired);
        List<RenderElement> current = new ArrayList<RenderElement>();
        for (RenderElement r : attachOrder) {
            if (members.contains(r)) {
                current.add(r);
            }
        }
        if (current.equals(desired)) {
            return;
        }
        int insertAt = attachOrder.indexOf(current.get(0));
        attachOrder.removeAll(current);
        attachOrder.addAll(insertAt, desired);
        if (container != null) {
            for (int i = 0; i < desired.size(); i++) {
                Component c = desired.get(i).component();
                if (c != null && c.getParent() == container) {
                    container.removeComponent(c);
                    int target = Math.min(insertAt + i, container.getComponentCount());
                    container.addComponent(target, c);
                }
            }
        }
    }

    /**
     * Lays out THIS host's subtree after a build changed it.
     *
     * <p>Deliberately not {@code revalidate()}. In Codename One a finished layout is
     * finished; revalidate goes to the Form root and lays the whole hierarchy out again,
     * which for a Flutter build flush is the wrong scope by a wide margin - a setState on
     * one leaf would re-lay out the toolbar, the side menu and every other container on the
     * form. Marking this container's own subtree and calling {@code layoutContainer()} does
     * only the work that a change inside this host can possibly have affected.</p>
     *
     * <p>The Flutter pass this triggers is tight against the host's own bounds, so the host
     * does not change size and its parent has nothing to redo.</p>
     */
    public void revalidate() {
        if (container == null) {
            return;
        }
        // Never from inside a pass. A relayout requested mid-pass walks a tree
        // that is currently being replaced; see FlutterRootLayout.inLayout.
        if (FlutterRootLayout.inLayout()) {
            FlutterRootLayout.deferRevalidate(this);
            return;
        }
        // Run OUR constraint pass and nothing else. It writes every component's bounds
        // absolutely, so none of Codename One's own layout machinery has to participate.
        //
        // What must NOT happen here is invalidating preferred sizes.
        // setShouldCalcPreferredSize(true) - which is also the first thing revalidate() does
        // - recurses down every child container and throws away CN1's cached measurements,
        // so every Label re-measures its text. Measured on the gallery home, that was the
        // whole of the cost: the Flutter constraint pass itself is ~2ms across ~4900 boxes,
        // while the frame was ~200ms. A component whose content actually changed invalidates
        // itself (Label.setText does), so blanket-invalidating a subtree only discards
        // measurements that were still valid.
        Layout layout = container.getLayout();
        if (layout != null) {
            layout.layoutContainer(container);
        }
        container.repaint();
    }
}
