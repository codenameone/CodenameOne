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
package com.codename1.ui.accessibility;

import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Slider;
import com.codename1.ui.Window;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.table.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Builds, caches, diffs, and dispatches actions for the portable semantic tree.
///
/// The tree is always constructed on the Codename One EDT. Native ports read the
/// last immutable snapshot and post actions through {@link #performAction(long, String, Object)},
/// so native accessibility threads never synchronously enter application code.
public final class AccessibilityManager {
    public static final int CHANGE_STRUCTURE = 1;
    public static final int CHANGE_CONTENT = 2;
    public static final int CHANGE_STATE = 4;
    public static final int CHANGE_VALUE = 8;
    public static final int CHANGE_BOUNDS = 16;
    public static final int CHANGE_FOCUS = 32;
    public static final int CHANGE_ACTIONS = 64;
    public static final int CHANGE_LIVE_REGION = 128;
    public static final int CHANGE_PANE = 256;
    public static final int CHANGE_ALL = 0x7fffffff;

    private static final AccessibilityManager INSTANCE = new AccessibilityManager();
    private long nextId = 1;
    private long generation;
    private boolean refreshScheduled;
    private int pendingChanges = CHANGE_ALL;
    /// The root the cached snapshot describes. A `Container` rather than a `Form`,
    /// because a `com.codename1.ui.Window` is a root in its own right.
    private Container snapshotRoot;

    /// Snapshots for roots other than the most recent one, so a screen reader moving
    /// between two windows does not rebuild both trees on every hop. Bounded, and
    /// cleared whole by `#invalidate(Component, int)` -- staleness is tracked per root,
    /// because per root dirtiness is a second thing to get wrong.
    private final LinkedHashMap<Container, AccessibilityTreeSnapshot> snapshotsByRoot =
            new LinkedHashMap<Container, AccessibilityTreeSnapshot>();

    /// How many roots' snapshots to keep.
    private static final int MAX_CACHED_ROOTS = 8;

    /// Roots invalidated since the pending refresh was scheduled, all of which it has
    /// to rebuild rather than only the one that scheduled it.
    private final ArrayList<Container> pendingRoots = new ArrayList<Container>();

    /// Whether a mutation with no resolvable surface is waiting for the refresh. Not
    /// the same as an empty queue, which is also what disposing every queued root
    /// leaves behind.
    private boolean pendingRootlessRefresh;

    /// The cached roots whose trees are stale.
    ///
    /// Staleness used to be one flag for the whole manager, which is wrong the moment
    /// there is more than one surface: invalidating one root and then rebuilding
    /// another cleared it, and the first root's cached tree was handed back as though
    /// it were current. A rebuild clears only the root it rebuilt.
    private final ArrayList<Container> dirtyRoots = new ArrayList<Container>();

    /// Queues every surface known to be alive, so the refresh rebuilds all of them
    /// rather than picking one.
    ///
    /// Its own method rather than inline in `#invalidate(Component, int)`: the loop
    /// walks a generic collection, and the compiler's cast for that would sit inside
    /// that method's catch of Throwable -- which ParparVM does not raise for a failed
    /// cast, so the repository's cast-semantics gate rejects it.
    private void queueEveryLiveRoot() {
        for (Container root : snapshotsByRoot.keySet()) {
            if (!pendingRoots.contains(root)) {
                pendingRoots.add(root);
            }
        }
        TopLevelContainer current = CN.getCurrentTopLevel();
        Container currentRoot = current == null ? null : current.asContainer();
        if (currentRoot != null && !pendingRoots.contains(currentRoot)) {
            pendingRoots.add(currentRoot);
        }
    }

    /// How many roots are currently recorded as stale.
    ///
    /// #### Returns
    ///
    /// the size of the dirty set
    public synchronized int dirtyRootCount() {
        return dirtyRoots.size();
    }

    /// A tree describing nothing.
    ///
    /// For a surface that no longer exists. A port can outlive a window -- an
    /// accessibility bridge is held by the platform and asked for its tree after the
    /// window it describes has been disposed -- and the alternatives are both wrong:
    /// the last tree built anywhere belongs to some other window, and rebuilding is
    /// impossible with nothing to walk.
    ///
    /// #### Returns
    ///
    /// an empty snapshot, never null
    public synchronized AccessibilityTreeSnapshot emptySnapshot() {
        generation++;
        return new AccessibilityTreeSnapshot(generation,
                Collections.<Long>emptyList(),
                Collections.<Long, AccessibilityNodeSnapshot>emptyMap());
    }

    /// How many roots currently have a cached tree.
    ///
    /// #### Returns
    ///
    /// the size of the snapshot cache
    public synchronized int cachedRootCount() {
        return snapshotsByRoot.size();
    }

    /// Marks a root's tree stale.
    ///
    /// #### Parameters
    ///
    /// - `root`: the root, or null for every cached one
    private void markDirty(Container root) {
        if (root == null) {
            for (Container cached : snapshotsByRoot.keySet()) {
                if (!dirtyRoots.contains(cached)) {
                    dirtyRoots.add(cached);
                }
            }
            return;
        }
        // Only a root that actually has a cached tree. Marking one that has none
        // achieves nothing -- a cache miss rebuilds it anyway -- and this list is held
        // by a singleton, so recording every form the application has ever shown, which
        // is what Display.setCurrent invalidating each new one amounts to, pinned every
        // one of them and its whole component hierarchy for the life of the process.
        // Only a disposed window ever releases a root explicitly.
        if (snapshotsByRoot.containsKey(root) && !dirtyRoots.contains(root)) {
            dirtyRoots.add(root);
        }
    }
    private AccessibilityTreeSnapshot snapshot = new AccessibilityTreeSnapshot(
            0, Collections.<Long>emptyList(), Collections.<Long, AccessibilityNodeSnapshot>emptyMap());

    private AccessibilityManager() {
    }

    public static AccessibilityManager getInstance() {
        return INSTANCE;
    }

    public synchronized void invalidate(Component component, int changeType) {
        pendingChanges |= changeType;
        // Deliberately not clearing the per-root cache. The eager refresh below
        // rebuilds one root, so emptying all of them would leave every other surface
        // with nothing to hand an off-EDT reader until that surface happened to mutate
        // -- a screen reader on another window would lose its whole tree. Correctness
        // on the EDT does not depend on the clear either: the root is marked stale and
        // rebuilt there, and the rebuild overwrites the entry it replaces. Off the EDT
        // a stale tree for the right surface is the documented contract; an empty one
        // is not.
        // The root the changed component actually lives on, not whatever form happens
        // to be current: a change inside a window used to schedule a rebuild of the
        // main form's tree instead, so the window's own tree stayed stale.
        // A null component is invalidateAll(): every surface is stale, not the focused
        // one. Substituting the current top level here turned an all-root invalidation
        // into a single-root refresh of whichever window happened to have focus, and
        // the rebuild then cleared the global dirty flag -- so a window that asked for
        // accessibility while unfocused was left with nothing until it next changed.
        final boolean allRoots = component == null;
        TopLevelContainer changedTop = allRoots ? null : component.getTopLevelContainer();
        if (changedTop == null && !allRoots) {
            changedTop = CN.getCurrentTopLevel();
        }
        final Container refreshRoot = changedTop == null ? null : changedTop.asContainer();
        markDirty(allRoots ? null : refreshRoot);
        try {
            // Most mutations only need to make the cached snapshot stale. Ports
            // that can pull the tree do so on demand, and ports such as Android
            // opt into eager projection only while assistive technology is active.
            // This keeps layout, scrolling, and text setters at O(1) when nobody
            // is consuming the semantic tree.
            if (!Display.getInstance().isAccessibilityTreeUpdateRequired()) {
                return;
            }
            // Queued rather than captured. A second invalidation on another root
            // while this one is still pending used to be swallowed by the
            // refreshScheduled flag: the callback rebuilt only the root it had closed
            // over, and the other root was never rebuilt at all, so its tree stayed
            // stale for good -- which off-EDT screen readers, now
            // that they are handed their own surface's tree, would have read forever.
            // Not capped. The list holds one entry per distinct root, and it is
            // drained by the refresh, so it is bounded by the number of live surfaces
            // -- while a cap would silently drop the earliest window's refresh and
            // leave it stale, which is the defect this queue exists to fix. The
            // snapshot cache is capped because it holds whole trees; this holds
            // references to containers that are alive anyway.
            if (allRoots) {
                queueEveryLiveRoot();
            } else if (refreshRoot == null) {
                // A mutation whose component belongs to no surface at all. Recorded
                // separately from the queue, because an empty queue can also mean every
                // root that was in it has since been disposed -- and clearing every
                // cached surface is right for the first and destroys every other
                // window's tree for the second.
                pendingRootlessRefresh = true;
            } else if (!pendingRoots.contains(refreshRoot)) {
                pendingRoots.add(refreshRoot);
            }
            if (!refreshScheduled) {
                refreshScheduled = true;
                Display.getInstance().callSerially(new RefreshPass());
            }
        } catch (Throwable ignored) {
            // Display may not be initialized yet while an application constructs its first form.
            refreshScheduled = false;
        }
    }

    /// One drain of the refresh queue, and the re-post that keeps it honest.
    ///
    /// Named rather than anonymous because it re-schedules itself: snapshots are built
    /// outside the lock, so an invalidation arriving during a pass finds the queue
    /// already emptied and `refreshScheduled` still set, and schedules nothing of its
    /// own. Clearing the flag without looking left that root stale until some unrelated
    /// later invalidation happened to come along.
    private final class RefreshPass implements Runnable {
        @Override
        public void run() {
            int changes;
            ArrayList<Container> roots;
            boolean rootless;
            synchronized (AccessibilityManager.this) {
                // Taken, not just read. The rebuilds below used to clear this as a
                // side effect, so a bit arriving while this pass was walking an earlier
                // root was wiped before the pass it queued could report it -- losing the
                // pane change VoiceOver needs to move focus to a newly opened pane.
                changes = pendingChanges;
                pendingChanges = 0;
                roots = new ArrayList<Container>(pendingRoots);
                pendingRoots.clear();
                rootless = pendingRootlessRefresh;
                pendingRootlessRefresh = false;
                // Released here, with the queue, rather than held until the end. The
                // defect this pass exists to fix is that an invalidation arriving while
                // it runs found a refresh still scheduled and queued nothing of its own,
                // and the pass then cleared the flag having already emptied the queue --
                // so that root stayed stale. Clearing it now lets those invalidations
                // schedule an ordinary pass of their own instead.
                //
                // Re-posting from inside the pass fixed the same defect and cost the
                // JavaScript port its suite: that runtime drains the serial queue until
                // it is empty within one turn, so a runnable that re-queues itself while
                // work keeps arriving holds the drain and nothing is ever painted. An
                // animating surface produces work every frame, so the screen froze and
                // the test waiting on it timed out with no screenshot. Scheduling
                // through the ordinary path lands in a later turn, which yields.
                refreshScheduled = false;
            }
            if (rootless) {
                getSnapshotForRoot(null);
            }
            int count = roots.size();
            for (int iter = 0; iter < count; iter++) {
                synchronized (AccessibilityManager.this) {
                    // Marked stale again for each one in turn, so none is
                    // served out of the cache this refresh exists to replace.
                    markDirty(roots.get(iter));
                }
                getSnapshotForRoot(roots.get(iter));
            }
            // Named per surface. A port that pushes the tree into a native view needs
            // to know which one it just described: told only that something changed, it
            // reads whatever was rebuilt last and installs that on the main view, so a
            // change inside a window replaced the main surface's elements with the
            // window's. Ports with one surface, and ports that pull, are unaffected --
            // the two argument form forwards to the old one by default.
            if (count == 0) {
                Display.getInstance().accessibilityTreeChanged(changes);
            } else {
                for (int iter = 0; iter < count; iter++) {
                    Display.getInstance().accessibilityTreeChanged(changes,
                            windowIdOf(roots.get(iter)));
                }
            }
        }
    }

    public void invalidateAll() {
        invalidate(null, CHANGE_ALL);
    }

    public AccessibilityTreeSnapshot getCurrentSnapshot() {
        synchronized (this) {
            if (!Display.getInstance().isEdt()) {
                return snapshot;
            }
        }
        return getSnapshot(CN.getCurrentTopLevel());
    }

    /// The accessibility tree for a top level, which may be a
    /// `com.codename1.ui.Window` rather than a `Form`.
    ///
    /// #### Parameters
    ///
    /// - `top`: the top level to describe, may be null
    ///
    /// #### Returns
    ///
    /// the snapshot, never null
    public AccessibilityTreeSnapshot getSnapshot(TopLevelContainer top) {
        return getSnapshotForRoot(top == null ? null : top.asContainer());
    }

    /// Drops any snapshot cached for a root that is going away.
    ///
    /// #### Parameters
    ///
    /// - `root`: the root being disposed
    public synchronized void releaseRoot(Container root) {
        if (root == null) {
            return;
        }
        snapshotsByRoot.remove(root);
        dirtyRoots.remove(root);
        // Out of the refresh queue as well, or a refresh already scheduled would walk
        // a hierarchy that has just been destroyed and cache a tree for it -- putting
        // back exactly what this method exists to take away.
        pendingRoots.remove(root);
        if (snapshotRoot == root) { //NOPMD CompareObjectsWithEquals
            snapshotRoot = null;
            // The snapshot itself, not only the reference to its root. It holds a node
            // per component of a hierarchy that has just been destroyed, and it is what
            // off-EDT accessibility callers are handed -- so a late screen reader query
            // would read, or act on, a window that is gone.
            generation++;
            snapshot = new AccessibilityTreeSnapshot(generation,
                    Collections.<Long>emptyList(),
                    Collections.<Long, AccessibilityNodeSnapshot>emptyMap());
            // Nothing is marked stale here: the root that was is gone, and every other
            // surface's tree is still exactly as current as it was.
        }
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public synchronized AccessibilityTreeSnapshot getSnapshot(Form form) {
        return getSnapshotForRoot(form);
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private synchronized AccessibilityTreeSnapshot getSnapshotForRoot(Container form) {
        // Walking a live lightweight component hierarchy off the Codename One
        // EDT is unsafe. Native bridges on other threads receive an immutable
        // snapshot; active bridges arrange eager refreshes on the EDT.
        if (!Display.getInstance().isEdt()) {
            // The one cached for the surface actually being asked about. An
            // invalidation rebuilds a single root and then notifies every window
            // bridge, so the most recently built tree usually belongs to a different
            // window -- handing that back would have a screen reader on one window
            // announce, and act on, another window's contents. An empty tree when
            // nothing has been built for this root yet, because "nothing known here"
            // is recoverable on the next refresh and describing the wrong window
            // is not.
            if (form == null) {
                return snapshot;
            }
            AccessibilityTreeSnapshot cached = snapshotsByRoot.get(form);
            if (cached != null) {
                return cached;
            }
            return form == snapshotRoot ? snapshot //NOPMD CompareObjectsWithEquals
                    : new AccessibilityTreeSnapshot(generation,
                            Collections.<Long>emptyList(),
                            Collections.<Long, AccessibilityNodeSnapshot>emptyMap());
        }
        // This root's staleness, not the manager's. Asking the global flag meant that
        // rebuilding one surface declared every other surface fresh, and the next pull
        // for one of them was answered out of a cache that had already been invalidated.
        if (!dirtyRoots.contains(form)) {
            if (form == snapshotRoot) { //NOPMD CompareObjectsWithEquals
                return snapshot;
            }
            AccessibilityTreeSnapshot cached = snapshotsByRoot.get(form);
            if (cached != null) {
                snapshot = cached;
                snapshotRoot = form;
                snapshotRootWasShowing = isShowingRoot(form);
                return snapshot;
            }
        }
        if (form == null) {
            generation++;
            AccessibilityTreeSnapshot emptySnapshot = new AccessibilityTreeSnapshot(
                    generation, Collections.<Long>emptyList(),
                    Collections.<Long, AccessibilityNodeSnapshot>emptyMap());
            snapshot = emptySnapshot;
            snapshotRoot = null;
            snapshotRootWasShowing = false;
            snapshotsByRoot.clear();
            dirtyRoots.clear();
            // The bits are not cleared by building a tree. They describe changes that
            // have not been announced yet, and only the refresh pass announces them --
            // clearing here wiped whatever had arrived while that pass was running.
            return snapshot;
        }

        List<BuildNode> roots = new ArrayList<BuildNode>();
        resolveComponent(form, roots);
        sortTree(roots);
        List<Long> rootIds = new ArrayList<Long>();
        LinkedHashMap<Long, AccessibilityNodeSnapshot> nodes = new LinkedHashMap<Long, AccessibilityNodeSnapshot>();
        freeze(roots, -1, rootIds, nodes);
        generation++;
        AccessibilityTreeSnapshot updatedSnapshot = new AccessibilityTreeSnapshot(generation, rootIds, nodes);
        snapshot = updatedSnapshot;
        snapshotRoot = form;
        // Whether this surface was the one on screen when it was described. That is what
        // separates a surface the application has since navigated away from -- whose
        // retained ids must stop working -- from one that was never on screen to begin
        // with, which is a form being prepared, or one shown a moment ago that the event
        // thread has not made current yet. Both of those are live surfaces a caller acts
        // on legitimately, and they are indistinguishable from the navigated-away one by
        // asking only whether it is showing now.
        snapshotRootWasShowing = isShowingRoot(form);
        snapshotsByRoot.put(form, updatedSnapshot);
        evictStaleRoots();
        dirtyRoots.remove(form);
        return snapshot;
    }

    /// Trims the cache without ever dropping a surface the user can still see.
    ///
    /// The cap is here because a root is only released explicitly when a window is
    /// disposed -- an application walking through fifty forms would otherwise keep a
    /// frozen tree for every one of them. But evicting by age alone dropped live
    /// windows once there were more of them than the cap, and an off-EDT reader on an
    /// evicted window is handed an empty tree, which is a screen reader losing the
    /// whole hierarchy. So age decides only among the surfaces that are no longer
    /// showing; when every entry is still showing the cache is allowed to exceed the
    /// cap, which is bounded anyway by how many surfaces can exist at once.
    private void evictStaleRoots() {
        while (snapshotsByRoot.size() > MAX_CACHED_ROOTS) {
            Container evictable = null;
            for (Container root : snapshotsByRoot.keySet()) {
                boolean showing = root instanceof TopLevelContainer
                        && ((TopLevelContainer) root).isTopLevelShowing();
                if (!showing) {
                    evictable = root;
                    break;
                }
            }
            if (evictable == null) {
                return;
            }
            snapshotsByRoot.remove(evictable);
            // And out of the dirty set with it. Dirtiness is only ever recorded for a
            // root that has a cached tree, so an entry left behind here describes a
            // tree that no longer exists -- and because a form never releases its root
            // explicitly, that entry would hold the form and its whole hierarchy for
            // good.
            dirtyRoots.remove(evictable);
        }
    }

    private AccessibilityNodeSnapshot findNode(long nodeId) {
        // The tree most recently built, whatever it describes. Gating this one on the
        // root being current as well refused an action the moment a caller performed one
        // straight after showing a form -- the device suite does exactly that, and the
        // surface is not current at that instant. The hazard reported was ids surviving
        // in the *cached* trees below, which is where the check belongs.
        // Unless that surface has since been taken off screen. A window hidden without
        // being disposed stays the last tree built, and its ids went on resolving here
        // -- so a reader holding one could press a button on a window nobody can see.
        AccessibilityNodeSnapshot node = isWithdrawnRoot(snapshotRoot)
                ? null : snapshot.getNode(nodeId);
        if (node != null) {
            return node;
        }
        for (Map.Entry<Container, AccessibilityTreeSnapshot> cached
                : snapshotsByRoot.entrySet()) {
            // Only surfaces that are actually on screen. A tree stays cached after the
            // main form has navigated away -- deliberately, so a live secondary window
            // keeps its own -- and an id retained from the old one still resolved here,
            // so a reader could invoke a command on a form nobody can see any more.
            if (!isShowingRoot(cached.getKey())) {
                continue;
            }
            node = cached.getValue().getNode(nodeId);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /// The surface a rebuilt root belongs to, zero for the application's main one.
    ///
    /// #### Parameters
    ///
    /// - `root`: the root that was rebuilt, may be null
    ///
    /// #### Returns
    ///
    /// the window id, or zero for a form
    private static int windowIdOf(Container root) {
        return root instanceof Window ? ((Window) root).getWindowId() : 0;
    }

    /// Whether a root has been taken off screen since its tree was built.
    ///
    /// Only a window can be: the platform maps it and unmaps it again, and once it is
    /// unmapped an id retained from it must not act on it. A form's showing flag cannot
    /// answer this question -- it means "is the current surface", which is equally false
    /// for a form being prepared off screen and for the instant between `Form#show()` and
    /// the event thread processing it. Both are live surfaces a caller legitimately acts
    /// on, which is why the check below is the wrong one to apply here.
    ///
    /// #### Parameters
    ///
    /// - `root`: the root that owns the most recently built tree, may be null
    ///
    /// #### Returns
    ///
    /// true when it is a window that is no longer on screen
    private boolean isWithdrawnRoot(Container root) {
        if (root instanceof Window) {
            return !((Window) root).isWindowShowing();
        }
        // A form, which has no mapped/unmapped state of its own: it is withdrawn when it
        // was the surface on screen at the time it was described and is not any more.
        return snapshotRootWasShowing && !isShowingRoot(root);
    }

    /// Whether `#snapshotRoot` was the surface on screen when its tree was built.
    private boolean snapshotRootWasShowing;

    /// Whether a cached root is a surface the user can currently see.
    ///
    /// #### Parameters
    ///
    /// - `root`: the cached root, may be null
    ///
    /// #### Returns
    ///
    /// true when it is a top level and it is showing
    private static boolean isShowingRoot(Container root) {
        return root instanceof TopLevelContainer
                && ((TopLevelContainer) root).isTopLevelShowing();
    }

    public synchronized int getPendingChanges() {
        return pendingChanges;
    }

    public boolean performAction(long nodeId, String actionId, final Object argument) {
        final AccessibilityNodeSnapshot node;
        final AccessibilityAction action;
        synchronized (this) {
            // Across every cached surface, not just the last one built. Off-EDT
            // readers are now handed their own window's tree, so the node they act on
            // routinely comes from a snapshot other than the current one -- resolving
            // only against that one would leave every button on a secondary window
            // inert.
            node = findNode(nodeId);
            action = node == null ? null : node.getAction(actionId);
        }
        if (node == null || action == null || !action.isEnabled()) {
            return false;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                action.perform(node.getComponent(), argument);
                invalidate(node.getComponent(), CHANGE_STATE | CHANGE_VALUE | CHANGE_CONTENT);
            }
        });
        return true;
    }

    private long idFor(Component component) {
        AccessibilityNode semantics = component.getSemantics();
        long id = semantics.getInternalId();
        if (id == 0) {
            id = nextId++;
            semantics.setInternalId(id);
        }
        return id;
    }

    private long idForVirtual(Component host, String path) {
        AccessibilityNode semantics = host.getSemantics();
        Long id = semantics.getInternalVirtualId(path);
        if (id == null) {
            id = Long.valueOf(nextId++);
            semantics.putInternalVirtualId(path, id.longValue());
        }
        return id.longValue();
    }

    private void resolveComponent(Component component, List<BuildNode> destination) {
        if (component == null || (!(component instanceof Form) && !component.isVisible()) || component.isHidden(true)) {
            return;
        }
        AccessibilityNode config = component.getSemantics();
        AccessibilityGrouping grouping = config.getGrouping();
        if (grouping == AccessibilityGrouping.EXCLUDE_SUBTREE) {
            return;
        }

        BuildNode node = buildComponentNode(component, config);
        List<BuildNode> children = new ArrayList<BuildNode>();
        if (grouping != AccessibilityGrouping.LEAF && component instanceof Container) {
            Container container = (Container) component;
            for (int i = 0; i < container.getComponentCount(); i++) {
                resolveComponent(container.getComponentAt(i), children);
            }
        }
        if (grouping != AccessibilityGrouping.LEAF) {
            addVirtualChildren(component, config, "custom", children);
            if (component instanceof com.codename1.ui.List) {
                addListChildren((com.codename1.ui.List) component, children);
            }
        }

        boolean expose = shouldExpose(component, config, node);
        if (grouping == AccessibilityGrouping.EXCLUDE) {
            expose = false;
        }
        if (grouping == AccessibilityGrouping.MERGE_DESCENDANTS) {
            String merged = collectLabels(children);
            if (isEmpty(node.builder.label)) {
                node.builder.label = merged;
            } else if (!isEmpty(merged)) {
                node.builder.description = join(node.builder.description, merged);
            }
            children.clear();
            expose = true;
        }
        if (grouping == AccessibilityGrouping.LEAF || grouping == AccessibilityGrouping.GROUP) {
            expose = true;
        }

        if (expose) {
            node.children.addAll(children);
            destination.add(node);
        } else {
            destination.addAll(children);
        }
    }

    private BuildNode buildComponentNode(final Component component, AccessibilityNode config) {
        BuildNode out = new BuildNode();
        out.id = idFor(component);
        out.builder.id = out.id;
        out.builder.component = component;
        out.builder.identifier = config.getIdentifier();
        out.builder.label = firstNonEmpty(config.getLabel(), component.getAccessibilityText());
        out.builder.hint = config.getHint();
        out.builder.description = config.getDescription();
        out.builder.value = config.getValue();
        out.builder.validationError = config.getValidationError();
        out.builder.paneTitle = config.getPaneTitle();
        out.builder.roleDescription = config.getRoleDescription();
        out.builder.role = config.getRole() == AccessibilityRole.NONE ? inferRole(component) : config.getRole();
        out.builder.checked = inferChecked(component, config);
        out.builder.liveRegion = config.getLiveRegion();
        out.builder.range = config.getRange() == null ? inferRange(component) : config.getRange();
        out.builder.collectionInfo =
                config.getCollectionInfo() == null ? inferCollection(component) : config.getCollectionInfo();
        out.builder.collectionItemInfo = config.getCollectionItemInfo() == null ? inferCollectionItem(component)
                                                                                : config.getCollectionItemInfo();
        out.builder.bounds = componentBounds(component);
        out.builder.selected = config.getSelected();
        if (out.builder.selected == null) {
            applyInferredSelected(component, out.builder);
        }
        out.builder.expanded = config.getExpanded();
        out.builder.enabled =
                config.getEnabled() == null ? Boolean.valueOf(component.isEnabled()) : config.getEnabled();
        out.builder.invalid = config.getInvalid();
        out.builder.busy = config.getBusy();
        out.builder.readOnly = config.getReadOnly();
        out.builder.required = config.getRequired();
        out.builder.multiline = config.getMultiline();
        out.builder.obscured = config.getObscured();
        applyInferredTextStates(component, out.builder);
        out.builder.pressed = config.getPressed();
        out.builder.current = config.getCurrent();
        out.builder.modal = config.isModal() || component instanceof Dialog;
        out.builder.focusable = component.isFocusable() || isInteractiveRole(out.builder.role);
        out.builder.focused = component.hasFocus();
        out.builder.headingLevel = config.getHeadingLevel();
        out.builder.sortKey = config.getSortKey();
        out.builder.traversalBefore = config.getTraversalBefore();
        out.builder.traversalAfter = config.getTraversalAfter();
        out.builder.actions.addAll(config.getActions());
        addDefaultActions(component, out.builder);
        if (component instanceof Form && isEmpty(out.builder.paneTitle)) {
            out.builder.paneTitle = ((Form) component).getTitle();
        }
        out.component = component;
        return out;
    }

    private void addVirtualChildren(Component host, AccessibilityNode config, String path,
                                    List<BuildNode> destination) {
        List<AccessibilityNode> virtual = new ArrayList<AccessibilityNode>();
        virtual.addAll(config.getChildren());
        if (config.getChildProvider() != null) {
            List<AccessibilityNode> provided = config.getChildProvider().getAccessibilityChildren(host);
            if (provided != null) {
                virtual.addAll(provided);
            }
        }
        for (int i = 0; i < virtual.size(); i++) {
            AccessibilityNode child = virtual.get(i);
            String key = child.getVirtualKey();
            if (isEmpty(key)) {
                key = "index-" + i;
            }
            BuildNode resolved = buildVirtualNode(host, child, path + "/" + key);
            addVirtualChildren(host, child, path + "/" + key, resolved.children);
            destination.add(resolved);
        }
    }

    private BuildNode buildVirtualNode(Component host, AccessibilityNode config, String path) {
        BuildNode out = new BuildNode();
        out.id = idForVirtual(host, path);
        out.builder.id = out.id;
        out.builder.component = host;
        out.builder.virtualKey = config.getVirtualKey();
        out.builder.identifier = config.getIdentifier();
        out.builder.label = config.getLabel();
        out.builder.hint = config.getHint();
        out.builder.description = config.getDescription();
        out.builder.value = config.getValue();
        out.builder.validationError = config.getValidationError();
        out.builder.paneTitle = config.getPaneTitle();
        out.builder.roleDescription = config.getRoleDescription();
        out.builder.role = config.getRole() == AccessibilityRole.NONE ? AccessibilityRole.GENERIC : config.getRole();
        out.builder.checked = config.getChecked();
        out.builder.liveRegion = config.getLiveRegion();
        out.builder.range = config.getRange();
        out.builder.collectionInfo = config.getCollectionInfo();
        out.builder.collectionItemInfo = config.getCollectionItemInfo();
        out.builder.bounds = virtualBounds(host, config.getBounds());
        out.builder.selected = config.getSelected();
        out.builder.expanded = config.getExpanded();
        out.builder.enabled = config.getEnabled() == null ? Boolean.valueOf(host.isEnabled()) : config.getEnabled();
        out.builder.invalid = config.getInvalid();
        out.builder.busy = config.getBusy();
        out.builder.readOnly = config.getReadOnly();
        out.builder.required = config.getRequired();
        out.builder.multiline = config.getMultiline();
        out.builder.obscured = config.getObscured();
        out.builder.pressed = config.getPressed();
        out.builder.current = config.getCurrent();
        out.builder.modal = config.isModal();
        out.builder.focusable = !config.getActions().isEmpty() || isInteractiveRole(out.builder.role);
        out.builder.headingLevel = config.getHeadingLevel();
        out.builder.sortKey = config.getSortKey();
        out.builder.actions.addAll(config.getActions());
        out.component = host;
        return out;
    }

    private void addListChildren(final com.codename1.ui.List list, List<BuildNode> destination) {
        int size = list.size();
        int[] visibleItems = list.getAccessibilityVisibleItemIndices();
        for (int i : visibleItems) {
            final int index = i;
            AccessibilityNode item = new AccessibilityNode("item-" + i)
                                             .setRole(AccessibilityRole.LIST_ITEM)
                                             .setLabel(list.getAccessibilityItemText(i))
                                             .setSelected(Boolean.valueOf(i == list.getSelectedIndex()))
                                             .setCollectionItemInfo(new AccessibilityCollectionItemInfo(
                                                     i, 1, 0, 1, i + 1, size, 1, false))
                                             .setBounds(list.getAccessibilityItemBounds(i, new Rectangle()))
                                             .addAction(new AccessibilityAction(AccessibilityAction.ACTIVATE, null,
                                                                                new ListActivateHandler(list, index)));
            destination.add(buildVirtualNode(list, item, "list/item-" + i));
        }
    }

    private void addDefaultActions(final Component component, AccessibilityNodeSnapshot.Builder builder) {
        if (component.isFocusable() && !hasAction(builder.actions, AccessibilityAction.FOCUS)) {
            builder.actions.add(new AccessibilityAction(AccessibilityAction.FOCUS, null, FocusHandler.INSTANCE));
        }
        if (component instanceof Button && !hasAction(builder.actions, AccessibilityAction.ACTIVATE)) {
            builder.actions.add(new AccessibilityAction(AccessibilityAction.ACTIVATE, null, ActivateHandler.INSTANCE));
        }
        if (component instanceof Slider) {
            final Slider slider = (Slider) component;
            if (slider.isEditable() && !hasAction(builder.actions, AccessibilityAction.INCREMENT)) {
                builder.actions.add(new AccessibilityAction(AccessibilityAction.INCREMENT, null,
                                                            new SliderAdjustmentHandler(slider, 1)));
            }
            if (slider.isEditable() && !hasAction(builder.actions, AccessibilityAction.DECREMENT)) {
                builder.actions.add(new AccessibilityAction(AccessibilityAction.DECREMENT, null,
                                                            new SliderAdjustmentHandler(slider, -1)));
            }
        }
        if (component instanceof com.codename1.ui.List) {
            final com.codename1.ui.List list = (com.codename1.ui.List) component;
            if (list.size() > 0 && !hasAction(builder.actions, AccessibilityAction.SCROLL_FORWARD)) {
                builder.actions.add(new AccessibilityAction(AccessibilityAction.SCROLL_FORWARD, null,
                                                            new ListScrollHandler(list, 1)));
            }
            if (list.size() > 0 && !hasAction(builder.actions, AccessibilityAction.SCROLL_BACKWARD)) {
                builder.actions.add(new AccessibilityAction(AccessibilityAction.SCROLL_BACKWARD, null,
                                                            new ListScrollHandler(list, -1)));
            }
        }
        if (component instanceof TextArea) {
            final TextArea text = (TextArea) component;
            if (!hasAction(builder.actions, AccessibilityAction.FOCUS)) {
                builder.actions.add(new AccessibilityAction(AccessibilityAction.FOCUS, null, FocusHandler.INSTANCE));
            }
            if (text.isEditable() && !hasAction(builder.actions, AccessibilityAction.SET_TEXT)) {
                builder.actions.add(
                        new AccessibilityAction(AccessibilityAction.SET_TEXT, null, new SetTextHandler(text)));
            }
        }
    }

    private AccessibilityRole inferRole(Component component) {
        if (component.getParent() instanceof Table) {
            return ((Table) component.getParent()).getCellRow(component) < 0 ? AccessibilityRole.COLUMN_HEADER
                                                                             : AccessibilityRole.CELL;
        }
        Tabs tabOwner = tabPanelOwner(component);
        if (tabOwner != null) {
            return AccessibilityRole.TAB_PANEL;
        }
        if (component instanceof Dialog) {
            return AccessibilityRole.DIALOG;
        }
        if (component instanceof RadioButton) {
            return AccessibilityRole.RADIO_BUTTON;
        }
        if (component instanceof CheckBox) {
            return AccessibilityRole.CHECKBOX;
        }
        if (component instanceof Button) {
            if (isTabButton((Button) component)) {
                return AccessibilityRole.TAB;
            }
            return ((Button) component).isToggle() ? AccessibilityRole.TOGGLE_BUTTON : AccessibilityRole.BUTTON;
        }
        if (component instanceof Slider) {
            return ((Slider) component).isEditable() ? AccessibilityRole.SLIDER : AccessibilityRole.PROGRESS_BAR;
        }
        if (component instanceof TextField) {
            return AccessibilityRole.TEXT_FIELD;
        }
        if (component instanceof TextArea) {
            return AccessibilityRole.TEXT_FIELD;
        }
        if (component instanceof com.codename1.ui.List) {
            return AccessibilityRole.LIST;
        }
        if (component instanceof Table) {
            return AccessibilityRole.GRID;
        }
        if (component instanceof Tabs) {
            return AccessibilityRole.TAB_LIST;
        }
        if (component instanceof Form) {
            return AccessibilityRole.GENERIC;
        }
        if (component instanceof Label) {
            return AccessibilityRole.STATIC_TEXT;
        }
        return AccessibilityRole.NONE;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private boolean isTabButton(Button button) {
        Container parent = button.getParent();
        while (parent != null) {
            Container owner = parent.getParent();
            if (owner instanceof Tabs && ((Tabs) owner).getTabsContainer() == parent) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private AccessibilityCheckedState inferChecked(Component component, AccessibilityNode config) {
        if (config.getChecked() != AccessibilityCheckedState.UNSPECIFIED) {
            return config.getChecked();
        }
        if (component instanceof CheckBox || component instanceof RadioButton) {
            return ((Button) component).isSelected() ? AccessibilityCheckedState.CHECKED
                                                     : AccessibilityCheckedState.UNCHECKED;
        }
        return AccessibilityCheckedState.UNSPECIFIED;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private void applyInferredSelected(Component component, AccessibilityNodeSnapshot.Builder builder) {
        if (component instanceof Button && (((Button) component).isToggle() || isTabButton((Button) component))) {
            builder.selected = Boolean.valueOf(((Button) component).isSelected());
            return;
        }
        Tabs tabOwner = tabPanelOwner(component);
        if (tabOwner != null) {
            builder.selected = Boolean.valueOf(tabOwner.getSelectedComponent() == component);
        }
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private Tabs tabPanelOwner(Component component) {
        Container parent = component.getParent();
        if (parent != null && parent.getParent() instanceof Tabs) {
            Tabs tabs = (Tabs) parent.getParent();
            if (tabs.getContentPane() == parent) {
                return tabs;
            }
        }
        return null;
    }

    private AccessibilityRange inferRange(Component component) {
        if (component instanceof Slider) {
            Slider slider = (Slider) component;
            return new AccessibilityRange(slider.getMinValue(), slider.getMaxValue(), slider.getProgress(),
                                          slider.getIncrements(), null);
        }
        return null;
    }

    private AccessibilityCollectionInfo inferCollection(Component component) {
        if (component instanceof com.codename1.ui.List) {
            return new AccessibilityCollectionInfo(((com.codename1.ui.List) component).size(), 1, false,
                                                   AccessibilityCollectionInfo.SELECTION_SINGLE);
        }
        if (component instanceof Table) {
            Table table = (Table) component;
            return new AccessibilityCollectionInfo(table.getModel().getRowCount(), table.getModel().getColumnCount(),
                                                   false, AccessibilityCollectionInfo.SELECTION_SINGLE);
        }
        if (component instanceof Tabs) {
            return new AccessibilityCollectionInfo(1, ((Tabs) component).getTabCount(), false,
                                                   AccessibilityCollectionInfo.SELECTION_SINGLE);
        }
        return null;
    }

    private AccessibilityCollectionItemInfo inferCollectionItem(Component component) {
        if (component.getParent() instanceof Table) {
            Table table = (Table) component.getParent();
            int sourceRow = table.getCellRow(component);
            int row = sourceRow < 0 ? 0 : sourceRow + (table.isIncludeHeader() ? 1 : 0);
            int column = table.getCellColumn(component);
            return new AccessibilityCollectionItemInfo(row, 1, column, 1, column + 1, table.getModel().getColumnCount(),
                                                       1, sourceRow < 0);
        }
        return null;
    }

    private void applyInferredTextStates(Component component, AccessibilityNodeSnapshot.Builder builder) {
        if (!(component instanceof TextArea)) {
            return;
        }
        TextArea text = (TextArea) component;
        if (builder.readOnly == null) {
            builder.readOnly = Boolean.valueOf(!text.isEditable());
        }
        if (builder.multiline == null) {
            builder.multiline = Boolean.valueOf(!(component instanceof TextField) || text.getRows() > 1);
        }
        if (builder.obscured == null) {
            builder.obscured = Boolean.valueOf((text.getConstraint() & TextArea.PASSWORD) != 0);
        }
    }

    private boolean shouldExpose(Component component, AccessibilityNode config, BuildNode node) {
        if (component instanceof Form) {
            return true;
        }
        if (config.hasExplicitConfiguration()) {
            return true;
        }
        return node.builder.role != AccessibilityRole.NONE &&
                (!isEmpty(node.builder.label) || !isEmpty(node.builder.value) || node.builder.focusable ||
                 !node.builder.actions.isEmpty() || node.builder.collectionInfo != null || node.builder.range != null);
    }

    private Rectangle componentBounds(Component component) {
        Rectangle bounds = new Rectangle(component.getAbsoluteX() + component.getScrollX(),
                                         component.getAbsoluteY() + component.getScrollY(), component.getWidth(),
                                         component.getHeight());
        Container parent = component.getParent();
        while (parent != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
            Rectangle clip =
                    new Rectangle(parent.getAbsoluteX() + parent.getScrollX(),
                                  parent.getAbsoluteY() + parent.getScrollY(), parent.getWidth(), parent.getHeight());
            intersect(bounds, clip);
            parent = parent.getParent();
        }
        return bounds;
    }

    private Rectangle virtualBounds(Component host, Rectangle relative) {
        if (relative == null) {
            return componentBounds(host);
        }
        Rectangle bounds = new Rectangle(host.getAbsoluteX() + host.getScrollX() + relative.getX(),
                                         host.getAbsoluteY() + host.getScrollY() + relative.getY(), relative.getWidth(),
                                         relative.getHeight());
        intersect(bounds, componentBounds(host));
        return bounds;
    }

    private void intersect(Rectangle target, Rectangle clip) {
        int x1 = Math.max(target.getX(), clip.getX());
        int y1 = Math.max(target.getY(), clip.getY());
        int x2 = Math.min(target.getX() + target.getWidth(), clip.getX() + clip.getWidth());
        int y2 = Math.min(target.getY() + target.getHeight(), clip.getY() + clip.getHeight());
        target.setBounds(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
    }

    private void sortTree(List<BuildNode> nodes) {
        Collections.sort(nodes, SortKeyComparator.INSTANCE);
        applyRelativeOrder(nodes);
        for (BuildNode node : nodes) {
            sortTree(node.children);
        }
    }

    private void applyRelativeOrder(List<BuildNode> nodes) {
        int remainingPasses = nodes.size();
        while (remainingPasses > 0) {
            boolean changed = false;
            for (int i = 0; i < nodes.size(); i++) {
                BuildNode node = nodes.get(i);
                int target = indexOf(nodes, node.builder.traversalBefore);
                if (target >= 0 && i > target) {
                    nodes.remove(i);
                    nodes.add(target, node);
                    changed = true;
                    break;
                }
                target = indexOf(nodes, node.builder.traversalAfter);
                if (target >= 0 && i < target) {
                    nodes.remove(i);
                    nodes.add(Math.min(target, nodes.size()), node);
                    changed = true;
                    break;
                }
            }
            if (!changed) {
                return;
            }
            remainingPasses--;
        }
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private int indexOf(List<BuildNode> nodes, Component component) {
        if (component == null) {
            return -1;
        }
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).component == component) {
                return i;
            }
        }
        return -1;
    }

    private void freeze(List<BuildNode> source, long parentId, List<Long> childIds,
                        LinkedHashMap<Long, AccessibilityNodeSnapshot> nodes) {
        for (BuildNode node : source) {
            node.builder.parentId = parentId;
            for (BuildNode child : node.children) {
                node.builder.childIds.add(Long.valueOf(child.id));
            }
            AccessibilityNodeSnapshot frozen = new AccessibilityNodeSnapshot(node.builder);
            nodes.put(Long.valueOf(node.id), frozen);
            childIds.add(Long.valueOf(node.id));
            List<Long> ignored = new ArrayList<Long>();
            freeze(node.children, node.id, ignored, nodes);
        }
    }

    private String collectLabels(List<BuildNode> nodes) {
        String value = null;
        for (BuildNode node : nodes) {
            value = join(value, node.builder.label);
            value = join(value, collectLabels(node.children));
        }
        return value;
    }

    private static boolean hasAction(List<AccessibilityAction> actions, String id) {
        for (AccessibilityAction action : actions) {
            if (id.equals(action.getId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInteractiveRole(AccessibilityRole role) {
        return role == AccessibilityRole.BUTTON || role == AccessibilityRole.TOGGLE_BUTTON ||
                role == AccessibilityRole.CHECKBOX || role == AccessibilityRole.RADIO_BUTTON ||
                role == AccessibilityRole.SWITCH || role == AccessibilityRole.LINK ||
                role == AccessibilityRole.TEXT_FIELD || role == AccessibilityRole.SEARCH_FIELD ||
                role == AccessibilityRole.SLIDER || role == AccessibilityRole.TAB ||
                role == AccessibilityRole.MENU_ITEM || role == AccessibilityRole.SPIN_BUTTON ||
                role == AccessibilityRole.COMBO_BOX || role == AccessibilityRole.TREE_ITEM;
    }

    private static String firstNonEmpty(String first, String second) {
        return !isEmpty(first) ? first : second;
    }

    private static String join(String first, String second) {
        if (isEmpty(first)) {
            return second;
        }
        if (isEmpty(second)) {
            return first;
        }
        return first + ", " + second;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    private static final class BuildNode {
        long id;
        Component component;
        AccessibilityNodeSnapshot.Builder builder = new AccessibilityNodeSnapshot.Builder();
        List<BuildNode> children = new ArrayList<BuildNode>();
    }

    private static final class SortKeyComparator implements Comparator<BuildNode> {
        private static final SortKeyComparator INSTANCE = new SortKeyComparator();

        @Override
        public int compare(BuildNode a, BuildNode b) {
            boolean an = Double.isNaN(a.builder.sortKey);
            boolean bn = Double.isNaN(b.builder.sortKey);
            if (an && bn) {
                return 0;
            }
            if (an) {
                return 1;
            }
            if (bn) {
                return -1;
            }
            return Double.compare(a.builder.sortKey, b.builder.sortKey);
        }
    }

    private static final class FocusHandler implements AccessibilityAction.Handler {
        private static final FocusHandler INSTANCE = new FocusHandler();

        @Override
        public boolean perform(Component source, Object argument) {
            if (!source.isEnabled()) {
                return false;
            }
            source.requestFocus();
            return true;
        }
    }

    private static final class ActivateHandler implements AccessibilityAction.Handler {
        private static final ActivateHandler INSTANCE = new ActivateHandler();

        @Override
        public boolean perform(Component source, Object argument) {
            if (!source.isEnabled()) {
                return false;
            }
            source.keyReleased(Display.getInstance().getKeyCode(Display.GAME_FIRE));
            return true;
        }
    }

    private static final class ListActivateHandler implements AccessibilityAction.Handler {
        private final com.codename1.ui.List list;
        private final int index;

        private ListActivateHandler(com.codename1.ui.List list, int index) {
            this.list = list;
            this.index = index;
        }

        @Override
        public boolean perform(Component source, Object argument) {
            if (!list.isEnabled()) {
                return false;
            }
            list.setSelectedIndex(index);
            list.keyReleased(Display.getInstance().getKeyCode(Display.GAME_FIRE));
            return true;
        }
    }

    private static final class ListScrollHandler implements AccessibilityAction.Handler {
        private final com.codename1.ui.List list;
        private final int direction;

        private ListScrollHandler(com.codename1.ui.List list, int direction) {
            this.list = list;
            this.direction = direction;
        }

        @Override
        public boolean perform(Component source, Object argument) {
            if (!list.isEnabled() || list.size() == 0) {
                return false;
            }
            int selected = list.getSelectedIndex();
            if (selected < 0) {
                selected = direction > 0 ? 0 : list.size() - 1;
            } else {
                selected = Math.max(0, Math.min(list.size() - 1, selected + direction));
            }
            if (selected == list.getSelectedIndex()) {
                return false;
            }
            list.setSelectedIndex(selected);
            return true;
        }
    }

    private static final class SliderAdjustmentHandler implements AccessibilityAction.Handler {
        private final Slider slider;
        private final int direction;

        private SliderAdjustmentHandler(Slider slider, int direction) {
            this.slider = slider;
            this.direction = direction;
        }

        @Override
        public boolean perform(Component source, Object argument) {
            int increment = Math.max(1, slider.getIncrements());
            int value = slider.getProgress() + direction * increment;
            slider.setProgress(Math.max(slider.getMinValue(), Math.min(slider.getMaxValue(), value)));
            return true;
        }
    }

    private static final class SetTextHandler implements AccessibilityAction.Handler {
        private final TextArea text;

        private SetTextHandler(TextArea text) {
            this.text = text;
        }

        @Override
        public boolean perform(Component source, Object argument) {
            if (!(argument instanceof String)) {
                return false;
            }
            text.setText((String) argument);
            return true;
        }
    }
}
