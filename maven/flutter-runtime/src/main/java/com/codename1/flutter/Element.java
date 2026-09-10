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

import com.codename1.flutter.rendering.RenderHost;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * An instantiation of a {@link Widget} at a particular location in the tree.
 * Elements are the retained structure: they survive across rebuilds when the
 * incoming widget {@link Widget#canUpdate(Widget, Widget) can update} the one
 * they currently hold. This class implements Flutter's reconciliation
 * decision table ({@link #updateChild}) and the keyed linear multi-child
 * reconciler ({@link #updateChildren}).
 */
public abstract class Element implements BuildContext {

    Widget widget;
    Element parent;
    int slot;
    int depth;
    boolean dirty;
    boolean mounted;
    BuildOwner owner;
    RenderHost host;
    /** Ancestor-lookup continuation for a route root — see {@link #ancestorOf}. */
    private Element contextFallback;

    protected Element(Widget widget) {
        this.widget = widget;
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public Widget widget() {
        return widget;
    }

    public Element parent() {
        return parent;
    }

    public boolean isMounted() {
        return mounted;
    }

    public boolean isDirty() {
        return dirty;
    }

    public int depth() {
        return depth;
    }

    public BuildOwner owner() {
        return owner;
    }

    public RenderHost host() {
        return host;
    }

    // ------------------------------------------------------------------
    // BuildContext
    // ------------------------------------------------------------------

    /**
     * The next element up for a {@code BuildContext} ancestor lookup: the
     * structural parent, or — at the root of a tree pushed as a route — the
     * context that pushed it.
     *
     * <p>In Flutter a route's page builds below the Navigator, so it inherits
     * the whole app above it. Here a pushed route mounts as a fresh
     * element-tree root (its own CN1 Form), so its structural parent chain
     * ends immediately and every {@code Foo.of(context)} in the page would
     * resolve to null. The fallback restores the inheritance without joining
     * the two trees structurally — the render/host logic (which decides, for
     * instance, whether a Scaffold owns the Form's Toolbar) still sees a
     * genuine root.</p>
     */
    private static Element ancestorOf(Element e) {
        return e.parent != null ? e.parent : e.contextFallback;
    }

    /**
     * The next element up the inheritance chain — {@link #parent()}, or the
     * pushing context at a route root. Use this rather than {@code parent()}
     * when walking for an ancestor widget, or the walk stops at every route.
     */
    public Element ancestor() {
        return ancestorOf(this);
    }

    /**
     * Whether {@code o} is an instance of {@code type} — the single predicate
     * the whole inherited-widget mechanism rests on, kept in one place so any
     * future portability question about it has exactly one answer to change.
     */
    public static boolean isInstanceOf(Class<?> type, Object o) {
        return type != null && o != null && type.isInstance(o);
    }

    /**
     * Links this root element's ancestor lookups to the context that pushed
     * it. Set by the Navigator when mounting a route.
     */
    public void contextFallback(Element e) {
        this.contextFallback = e;
    }

    @Override
    public <W extends Widget> W findAncestorWidgetOfExactType(Class<W> widgetType) {
        Element a = ancestorOf(this);
        while (a != null) {
            if (a.widget != null && a.widget.getClass() == widgetType) {
                return widgetType.cast(a.widget);
            }
            a = ancestorOf(a);
        }
        return null;
    }

    @Override
    public <W extends Widget> W dependOnInheritedWidgetOfExactType(Class<W> type) {
        Element a = ancestorOf(this);
        while (a != null) {
            if (isInstanceOf(type, a.widget)) {
                // REGISTER, do not merely read: the name is depend-on. Flutter records this
                // element as a dependent so a later change to the widget rebuilds it, and
                // without that every consumer is a one-shot read.
                if (a instanceof com.codename1.flutter.widgets.InheritedElement) {
                    ((com.codename1.flutter.widgets.InheritedElement) a).addDependent(this);
                }
                return type.cast(a.widget);
            }
            a = ancestorOf(a);
        }
        reportMissingAncestor(type);
        return null;
    }

    /**
     * Names the ancestors that were searched when an inherited-widget lookup
     * comes up empty.
     *
     * <p>Dart code almost always writes {@code Foo.of(context)!}, so a failed
     * lookup surfaces as a null-check TypeError somewhere else entirely, with
     * no indication of WHICH widget was missing or what the context could
     * actually see. Reporting it at the point of failure turns that into a
     * one-line diagnosis. Capped, because a missing provider is usually
     * missing on every build of every frame.</p>
     */
    private static int missingAncestorReports;

    private void reportMissingAncestor(Class<?> type) {
        if (missingAncestorReports >= 5) {
            return;
        }
        missingAncestorReports++;
        try {
            StringBuilder sb = new StringBuilder("Flutter runtime: no ");
            sb.append(type == null ? "?" : type.getName());
            sb.append(" above this context; ancestors were:");
            Element a = ancestorOf(this);
            int depth = 0;
            while (a != null && depth < 24) {
                sb.append(depth == 0 ? " " : " < ");
                sb.append(a.widget == null ? "null" : a.widget.getClass().getName());
                a = ancestorOf(a);
                depth++;
            }
            com.codename1.io.Log.p(sb.toString());
        } catch (Throwable t) {
            // diagnostics must never become the failure
        }
    }

    @Override
    public Object providerValueOfType(Class<?> type) {
        Element a = ancestorOf(this);
        int providers = 0;
        while (a != null) {
            if (a.widget instanceof InheritedValueProvider) {
                providers++;
                Object v = ((InheritedValueProvider) a.widget).providedValueFor(type);
                if (v != null) {
                    // Remember that WE read this, so a change to the model can come back
                    // and rebuild us. Without it a provider could notice its model change
                    // and rebuild itself and nothing else: its build returns the same
                    // child WIDGET INSTANCE, reconciliation sees widget == newWidget and
                    // returns early, and the subtree that actually reads the value never
                    // runs again. Every control whose job is to set a field on a model
                    // then looked dead -- the handler ran, the model changed, the screen
                    // did not.
                    a.addProviderDependent(this);
                    return v;
                }
            }
            a = ancestorOf(a);
        }
        reportMissingProvider(type, providers);
        return null;
    }

    /// Elements that read a provided value from this element.
    ///
    /// Flutter tracks this as an InheritedWidget dependency; a provider here is an
    /// ordinary widget, so the dependency has to be recorded by hand.
    private java.util.List<Element> providerDependents;

    void addProviderDependent(Element dependent) {
        if (dependent == null || dependent == this) {
            return;
        }
        if (providerDependents == null) {
            providerDependents = new java.util.ArrayList<Element>();
        }
        if (!providerDependents.contains(dependent)) {
            providerDependents.add(dependent);
        }
    }

    /**
     * Marks everything that read a value from this element for rebuild.
     *
     * <p>Unmounted readers are dropped as they are found: a dependency list that only
     * grows would hold a whole popped route alive and keep rebuilding it.</p>
     */
    public void rebuildProviderDependents() {
        if (providerDependents == null) {
            return;
        }
        java.util.List<Element> snapshot =
                new java.util.ArrayList<Element>(providerDependents);
        for (Element e : snapshot) {
            if (e.mounted) {
                e.markNeedsBuild();
            } else {
                providerDependents.remove(e);
            }
        }
    }

    private void reportMissingProvider(Class<?> type, int providersSeen) {
        if (missingAncestorReports >= 5) {
            return;
        }
        missingAncestorReports++;
        // The summary goes out FIRST and on its own: the ancestor walk below
        // touches every ancestor's class, and if any step of that throws the
        // whole report would vanish into the guard.
        try {
            com.codename1.io.Log.p("Flutter runtime: no provider of "
                    + (type == null ? "?" : type.getName())
                    + " (" + providersSeen + " provider(s) searched)");
        } catch (Throwable t) {
            return;
        }
        try {
            StringBuilder sb = new StringBuilder("Flutter runtime:   ancestors were:");
            Element a = ancestorOf(this);
            int depth = 0;
            while (a != null && depth < 30) {
                sb.append(depth == 0 ? " " : " < ");
                sb.append(a.widget == null ? "null" : a.widget.getClass().getName());
                a = ancestorOf(a);
                depth++;
            }
            com.codename1.io.Log.p(sb.toString());
        } catch (Throwable t) {
            // diagnostics must never become the failure
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T watch(Class<T> type) {
        return (T) providerValueOfType(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(Class<T> type) {
        return (T) providerValueOfType(type);
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Assigns the owner and render host of a root element before mounting.
     * Non-root elements inherit both from their parent during {@link #mount}.
     */
    public void bootstrap(BuildOwner owner, RenderHost host) {
        this.owner = owner;
        this.host = host;
    }

    /**
     * Adds this element to the tree. Subclasses extend this to create their
     * retained objects (State, CN1 components) and inflate their children.
     */
    public void mount(Element parent, int slot) {
        this.parent = parent;
        this.slot = slot;
        if (parent != null) {
            this.owner = parent.owner;
            this.host = parent.hostForChild(slot);
            this.depth = parent.depth + 1;
        }
        this.mounted = true;
    }

    /**
     * The render host a child mounted in {@code slot} should attach its CN1
     * components to. Overridden by elements that route a child subtree into a
     * different CN1 container (e.g. a root Scaffold's appBar into the
     * Toolbar's title area).
     */
    protected RenderHost hostForChild(int slot) {
        return host;
    }

    /**
     * Absorbs a new widget configuration. Callers guarantee
     * {@code Widget.canUpdate(this.widget, newWidget)}.
     */
    public void update(Widget newWidget) {
        this.widget = newWidget;
    }

    /**
     * Removes this element (only) from the tree. Subclasses release their
     * retained resources here. Use {@link #deactivateChild} to remove a whole
     * subtree.
     */
    public void unmount() {
        this.mounted = false;
        this.dirty = false;
    }

    /**
     * Visits the direct children of this element in tree order.
     */
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
    }

    /**
     * Notifies this subtree that the effective theme changed (M4): render
     * elements re-apply their programmatic, theme-derived styling. Called by
     * MaterialAppElement after re-installing the UIManager overlay — a plain
     * rebuild would miss subtrees whose widget INSTANCES were reused
     * (Element.updateChild's identity shortcut skips their update()).
     */
    public void themeChanged() {
        visitChildren(new Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element c) {
                c.themeChanged();
            }
        });
    }

    // ------------------------------------------------------------------
    // Building
    // ------------------------------------------------------------------

    /**
     * Marks this element dirty and schedules it with the build owner.
     */
    public void markNeedsBuild() {
        FlutterUI.assertEdt();
        if (!mounted || dirty) {
            return;
        }
        dirty = true;
        if (owner != null) {
            owner.scheduleBuildFor(this);
        }
    }

    /**
     * Rebuilds this element if it is dirty.
     */
    public void rebuild() {
        if (!mounted || !dirty) {
            return;
        }
        performRebuild();
    }

    /**
     * Actually rebuilds: composition elements call build() and reconcile the
     * result, render elements re-sync their configuration and children.
     * Implementations must clear the dirty flag.
     */
    protected abstract void performRebuild();

    // ------------------------------------------------------------------
    // Reconciliation
    // ------------------------------------------------------------------

    /**
     * Flutter's updateChild decision table:
     * <pre>
     *                     newWidget == null      newWidget != null
     * child == null       returns null           returns new Element
     * child != null       old child removed      old child updated in place
     *                                            when canUpdate, else removed
     *                                            and a new Element inflated
     * </pre>
     */
    protected Element updateChild(Element child, Widget newWidget, int newSlot) {
        if (newWidget == null) {
            if (child != null) {
                deactivateChild(child);
                markEnclosingLayoutDirty();
            }
            return null;
        }
        if (child != null) {
            if (child.widget == newWidget) {
                child.slot = newSlot;
                return child;
            }
            if (Widget.canUpdate(child.widget, newWidget)) {
                child.slot = newSlot;
                child.update(newWidget);
                return child;
            }
            // Mid-life replacement: anchor the host's attach cursor at the
            // flat-container index the replaced subtree's components occupy,
            // so the replacement's components land there (element-tree order)
            // instead of at the end of the container (z-order drift).
            RenderHost childHost = child.host;
            int anchor = childHost == null ? -1 : childHost.firstAttachIndex(child);
            deactivateChild(child);
            if (anchor >= 0) {
                int prev = childHost.beginInsertion(anchor);
                try {
                    return inflatedChild(newWidget, newSlot);
                } finally {
                    childHost.endInsertion(prev);
                }
            }
        }
        // A FRESH child, not a replacement. Anchor it too: appending puts its components
        // at the END of the flat host container, which is the top of the paint order.
        //
        // A subtree does not always arrive in tree order. A LayoutBuilder sits out a
        // speculative measurement pass and builds on a later one, by which time its
        // siblings are attached -- so a Scaffold's BODY, which is a LayoutBuilder in the
        // mail study, was created after the bottom bar and the floating action button and
        // painted over both of them. The bar and the button were not missing or
        // mispositioned; they were underneath the body, showing only where it did not
        // cover them.
        //
        // Anchor before the first already-attached sibling that belongs AFTER this slot,
        // so the new subtree lands where the element tree says it goes.
        int anchor = anchorForSlot(newSlot);
        if (anchor >= 0) {
            RenderHost childHost = hostForNewChild(newSlot);
            if (childHost != null) {
                int prev = childHost.beginInsertion(anchor);
                try {
                    return inflatedChild(newWidget, newSlot);
                } finally {
                    childHost.endInsertion(prev);
                }
            }
        }
        return inflatedChild(newWidget, newSlot);
    }

    /**
     * Inflates a new child and invalidates the layout that was measured without it.
     *
     * <p>A cached layout is only valid for the children it was measured over, so an
     * element that gains or loses one has a stale size and stale offsets for everything
     * it does contain. Nothing used to say so: {@code updateChild} rebuilt the element
     * tree and left every ancestor's {@code needsLayout} clear. The rebuild queue marks
     * the branch it rebuilds, but a child inflated during a LAYOUT pass -- a LayoutBuilder
     * building on a pass it had sat out -- never goes through that queue at all.</p>
     *
     * <p>Only a STRUCTURAL change invalidates. A child updated in place keeps its element,
     * and whether that dirties layout is its own business: treating every rebuild as a
     * layout change is what made the carousel stutter, one relayout of the page per frame
     * of the drag.</p>
     */
    private Element inflatedChild(Widget newWidget, int newSlot) {
        Element inflated = inflateWidget(newWidget, newSlot);
        markEnclosingLayoutDirty();
        return inflated;
    }

    /// Marks the nearest enclosing render element -- and through it every render
    /// ancestor -- as needing layout.
    private void markEnclosingLayoutDirty() {
        for (Element a = this; a != null; a = a.parent) {
            if (a instanceof RenderElement) {
                ((RenderElement) a).markNeedsLayout();
                return;
            }
        }
    }

    /// Where a child of {@code slot} should attach, or -1 when appending is already right.
    ///
    /// The first attach index of any already-attached child whose slot sorts AFTER this
    /// one. Nothing after it means the end of the container is the correct place, which
    /// is what appending already does.
    private int anchorForSlot(int slot) {
        RenderHost childHost = hostForNewChild(slot);
        if (childHost == null) {
            return -1;
        }
        // The next element in TREE order after where this child goes: first among our own
        // later children, then -- since the late builder is usually a leaf with no later
        // sibling of its own -- the first later sibling of an ancestor. A Scaffold's body
        // is a LayoutBuilder, so the sibling that has to be painted over is the bottom
        // bar three levels up, not anything the builder can see.
        int at = laterSiblingAttachIndex(this, slot, childHost);
        if (at >= 0) {
            return at;
        }
        Element node = this;
        while (node != null && node.parent() != null) {
            at = laterSiblingAttachIndex(node.parent(), node.slot, childHost);
            if (at >= 0) {
                return at;
            }
            node = node.parent();
        }
        return -1;
    }

    /// The earliest attach index, in {@code host}, of a child of {@code parent} whose slot
    /// sorts after {@code slot}. -1 when there is none.
    private static int laterSiblingAttachIndex(Element parent, final int slot,
            final RenderHost host) {
        if (parent == null) {
            return -1;
        }
        final int[] best = {-1};
        parent.visitChildren(new dart.runtime.Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element c) {
                if (c == null || c.slot <= slot) {
                    return;
                }
                int at = host.firstAttachIndex(c);
                if (at >= 0 && (best[0] < 0 || at < best[0])) {
                    best[0] = at;
                }
            }
        });
        return best[0];
    }

    /// The host a child of this slot attaches into.
    private RenderHost hostForNewChild(int slot) {
        if (this instanceof RenderElement) {
            RenderHost h = ((RenderElement) this).hostForChild(slot);
            if (h != null) {
                return h;
            }
        }
        return host();
    }

    protected Element inflateWidget(Widget newWidget, int newSlot) {
        Element child = newWidget.createElement();
        child.mount(this, newSlot);
        return child;
    }

    /**
     * Removes a child subtree from the tree. M1 has no GlobalKey
     * reactivation, so deactivation unmounts immediately and recursively.
     */
    protected void deactivateChild(Element child) {
        child.unmountRecursively();
        child.parent = null;
    }

    final void unmountRecursively() {
        visitChildren(new Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element c) {
                c.unmountRecursively();
            }
        });
        unmount();
    }

    /**
     * Flutter's keyed linear multi-child reconciler
     * (RenderObjectElement.updateChildren): sync a leading run and a trailing
     * run by canUpdate, match the middle by key, inflate everything else,
     * deactivate leftovers.
     */
    protected List<Element> updateChildren(List<Element> oldChildren, List<Widget> newWidgets) {
        int newChildrenTop = 0;
        int oldChildrenTop = 0;
        int newChildrenBottom = newWidgets.size() - 1;
        int oldChildrenBottom = oldChildren.size() - 1;

        Element[] newChildren = new Element[newWidgets.size()];

        // Update the top of the list.
        while ((oldChildrenTop <= oldChildrenBottom) && (newChildrenTop <= newChildrenBottom)) {
            Element oldChild = oldChildren.get(oldChildrenTop);
            Widget newWidget = newWidgets.get(newChildrenTop);
            if (oldChild == null || !Widget.canUpdate(oldChild.widget, newWidget)) {
                break;
            }
            newChildren[newChildrenTop] = updateChild(oldChild, newWidget, newChildrenTop);
            newChildrenTop++;
            oldChildrenTop++;
        }

        // Scan the bottom of the list (matched pairs are synced later so
        // middle inserts/removes keep correct slots).
        while ((oldChildrenTop <= oldChildrenBottom) && (newChildrenTop <= newChildrenBottom)) {
            Element oldChild = oldChildren.get(oldChildrenBottom);
            Widget newWidget = newWidgets.get(newChildrenBottom);
            if (oldChild == null || !Widget.canUpdate(oldChild.widget, newWidget)) {
                break;
            }
            oldChildrenBottom--;
            newChildrenBottom--;
        }

        // Scan the old middle: collect keyed children, drop the rest.
        Map<Key, Element> oldKeyedChildren = null;
        boolean haveOldChildren = oldChildrenTop <= oldChildrenBottom;
        if (haveOldChildren) {
            oldKeyedChildren = new HashMap<Key, Element>();
            while (oldChildrenTop <= oldChildrenBottom) {
                Element oldChild = oldChildren.get(oldChildrenTop);
                if (oldChild != null) {
                    Key k = oldChild.widget == null ? null : oldChild.widget.getKey();
                    if (k != null) {
                        oldKeyedChildren.put(k, oldChild);
                    } else {
                        deactivateChild(oldChild);
                    }
                }
                oldChildrenTop++;
            }
        }

        // Update the new middle, reusing keyed matches.
        while (newChildrenTop <= newChildrenBottom) {
            Element oldChild = null;
            Widget newWidget = newWidgets.get(newChildrenTop);
            if (haveOldChildren) {
                Key key = newWidget.getKey();
                if (key != null) {
                    oldChild = oldKeyedChildren.get(key);
                    if (oldChild != null) {
                        if (Widget.canUpdate(oldChild.widget, newWidget)) {
                            oldKeyedChildren.remove(key);
                        } else {
                            oldChild = null;
                        }
                    }
                }
            }
            newChildren[newChildrenTop] = updateChild(oldChild, newWidget, newChildrenTop);
            newChildrenTop++;
        }

        // Sync the bottom run that was scanned earlier.
        newChildrenBottom = newWidgets.size() - 1;
        oldChildrenBottom = oldChildren.size() - 1;
        while ((oldChildrenTop <= oldChildrenBottom) && (newChildrenTop <= newChildrenBottom)) {
            Element oldChild = oldChildren.get(oldChildrenTop);
            Widget newWidget = newWidgets.get(newChildrenTop);
            newChildren[newChildrenTop] = updateChild(oldChild, newWidget, newChildrenTop);
            newChildrenTop++;
            oldChildrenTop++;
        }

        // Deactivate leftover keyed children that were not reused.
        if (haveOldChildren && !oldKeyedChildren.isEmpty()) {
            for (Element leftover : oldKeyedChildren.values()) {
                deactivateChild(leftover);
            }
        }

        List<Element> result = new ArrayList<Element>(newChildren.length);
        for (Element e : newChildren) {
            result.add(e);
        }
        // Keyed children matched in a NEW order keep their elements (and CN1
        // components) but those components still sit at their OLD flat
        // container indices; move them so paint order and hit-testing match
        // the new tree order.
        reattachInTreeOrder(result);
        return result;
    }

    /**
     * Ensures the flat container components of the given children (this
     * host's attach entries under each child subtree) appear in the
     * container in tree order, moving survivors as needed. Subtree-internal
     * order is preserved — nested reorders are each child's own concern.
     */
    private void reattachInTreeOrder(List<Element> childrenInTreeOrder) {
        if (host == null) {
            return;
        }
        java.util.Set<Element> attached = new HashSet<Element>(host.attachOrder());
        List<RenderElement> desired = new ArrayList<RenderElement>();
        AttachedCollector collector = new AttachedCollector(attached, desired);
        for (int i = 0, n = childrenInTreeOrder.size(); i < n; i++) {
            Element c = childrenInTreeOrder.get(i);
            if (c != null) {
                collector.visit(c);
            }
        }
        host.reorderToTreeOrder(desired);
    }

    /**
     * Collects this host's attach entries under a subtree, in tree order.
     *
     * <p>One visitor for the whole traversal rather than one per node: the
     * recursion used to hand visitChildren a fresh capturing callback at every
     * element, even though the state it captured -- the attached set, the
     * output list, and the enclosing element's host -- is identical at every
     * level. An allocation census of a single screen counted 5,019 of those
     * callbacks, the largest anonymous-class count in the runtime, and they
     * are pure overhead: the same object serves the entire walk.</p>
     *
     * <p>Safe to reuse down the recursion because it holds no per-node state;
     * `attached` is read-only here and `out` only ever accumulates.</p>
     */
    private final class AttachedCollector implements Funcs.VoidFunc1<Element> {
        private final java.util.Set<Element> attached;
        private final List<RenderElement> out;

        AttachedCollector(java.util.Set<Element> attached, List<RenderElement> out) {
            this.attached = attached;
            this.out = out;
        }

        @Override
        public void call(Element c) {
            visit(c);
        }

        void visit(Element e) {
            if (e.host == Element.this.host && attached.contains(e)) {
                out.add((RenderElement) e);
            }
            e.visitChildren(this);
        }
    }
}
