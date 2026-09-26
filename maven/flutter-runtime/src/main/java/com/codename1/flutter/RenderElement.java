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

import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * An element that participates in layout — the RenderBox-lite protocol.
 *
 * <p>A render element may own a retained CN1 {@link Component} (Text owns a
 * Label, FloatingActionButton owns a CN1 FAB, ...) or own none at all
 * (Column, Center, Padding are pure positioning math). All owned components
 * across a subtree are FLAT children of the host's single CN1 container; this
 * class positions them absolutely from the results of the constraint pass.</p>
 *
 * <p>Protocol: {@link #layout(BoxConstraints)} — constraints go down, sizes
 * come up (cached per constraints until {@link #markNeedsLayout()}), then
 * {@link #position(int, int)} — absolute coordinates accumulate down the
 * render tree and are written to the CN1 components.</p>
 */
public abstract class RenderElement extends Element {

    private Component component;
    private Size size = Size.ZERO;
    private BoxConstraints lastConstraints;
    /// The dry-layout slot, kept apart from the real one so the two cannot evict each other.
    private Size drySize;
    private BoxConstraints lastDryConstraints;
    private boolean needsLayout = true;

    /// True while this box is inside its own {@link #performLayout}.
    private boolean inLayout;

    /// Set when something invalidates this box WHILE it is being laid out.
    ///
    /// A layout is only valid for the children it was measured over, and a child can
    /// appear in the middle of the pass that measures its parent: a LayoutBuilder sits
    /// out a speculative measurement and inflates its subtree on a later one, which
    /// happens underneath the enclosing Column's performLayout. The inflation marks the
    /// Column dirty, but the Column then finished the very pass it had invalidated and
    /// cleared the flag on its way out, so the mark was lost and the offsets it had
    /// stored for the children it did NOT have were kept.
    ///
    /// The visible result was a screen with nothing on it. Returning from Reply's search
    /// page rebuilt the mail list this way; every card was then positioned at the list's
    /// origin with a pane of zero size, and a zero-sized pane clips the subtree it hosts,
    /// so the cards were not merely stacked, they were invisible. Nothing recovered it
    /// either, because every later pass hit the same clean cache.
    private boolean remarkedDuringLayout;

    /** Offset of this box within its parent render element, set by the parent's performLayout. */
    private double relX;
    private double relY;

    /** Absolute position within the host container, set by position(). */
    private int absX;
    private int absY;

    protected RenderElement(Widget widget) {
        super(widget);
    }

    // ------------------------------------------------------------------
    // Element lifecycle
    // ------------------------------------------------------------------

    /// How much of start-up goes into creating and styling Codename One
    /// components, and how many there are. Read via {@link #componentCost()}.
    /// Attribution, not a feature: the first frame is dominated by layout, and
    /// "layout" here includes realising a component for every box.
    private static long componentMs;
    private static int componentCount;

    private static long attachMs;
    private static long selfMountMs;
    private static long neutralizeMs;

    /// Component creation cost per render-element class. Creating a Codename
    /// One component is 330us on the native build, which is two orders of
    /// magnitude more than allocating one should cost; this says which
    /// elements own it.
    private static final java.util.Map<Class<?>, long[]> CREATE_BY_CLASS =
            new java.util.HashMap<Class<?>, long[]>();

    /** Component creation, worst render-element classes first. */
    public static String componentBreakdown() {
        java.util.List<java.util.Map.Entry<Class<?>, long[]>> rows =
                new java.util.ArrayList<java.util.Map.Entry<Class<?>, long[]>>(
                        CREATE_BY_CLASS.entrySet());
        java.util.Collections.sort(rows,
                new java.util.Comparator<java.util.Map.Entry<Class<?>, long[]>>() {
                    @Override
                    public int compare(java.util.Map.Entry<Class<?>, long[]> a,
                            java.util.Map.Entry<Class<?>, long[]> b) {
                        return Long.compare(b.getValue()[0], a.getValue()[0]);
                    }
                });
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size() && i < 8; i++) {
            java.util.Map.Entry<Class<?>, long[]> e = rows.get(i);
            String n = e.getKey().getName();
            int dot = n.lastIndexOf('.');
            sb.append(' ').append(dot < 0 ? n : n.substring(dot + 1))
                    .append('=').append(e.getValue()[0]).append("ms/")
                    .append(e.getValue()[1]).append('x');
        }
        return sb.toString();
    }

    /** Components created so far, and what they cost. */
    public static String componentCost() {
        return componentCount + " component(s) in " + componentMs + "ms"
                + " (create=" + (componentMs - neutralizeMs) + "ms neutralize=" + neutralizeMs
                + "ms attach=" + attachMs + "ms selfMount=" + selfMountMs + "ms)";
    }

    @Override
    public void mount(Element parent, int slot) {
        if (!Trace.on()) {
            super.mount(parent, slot);
            component = createComponent();
            neutralizeCn1Behaviors(component);
            if (component != null) {
                componentCount++;
            }
            if (host != null && ownsComponent()) {
                host.attach(this);
            }
            dirty = true;
            performRebuild();
            return;
        }
        // Everything here EXCEPT performRebuild is this element's own cost;
        // performRebuild recurses into the subtree, so timing it would just
        // report the total again.
        long m0 = System.currentTimeMillis();
        super.mount(parent, slot);
        long t0 = System.currentTimeMillis();
        component = createComponent();
        long tn = System.currentTimeMillis();
        neutralizeCn1Behaviors(component);
        if (component != null) {
            componentCount++;
            long took = System.currentTimeMillis() - t0;
            componentMs += took;
            neutralizeMs += System.currentTimeMillis() - tn;
            long[] row = CREATE_BY_CLASS.get(getClass());
            if (row == null) {
                row = new long[2];
                CREATE_BY_CLASS.put(getClass(), row);
            }
            row[0] += took;
            row[1]++;
        }
        long a0 = System.currentTimeMillis();
        if (host != null && ownsComponent()) {
            // Components attach in mount (depth-first) order, which equals
            // element-tree order; when this mount replaces an existing
            // subtree, Element.updateChild anchors the host's insertion
            // cursor at the replaced components' index so the flat
            // container's z-order stays in sync with the tree.
            host.attach(this);
        }
        attachMs += System.currentTimeMillis() - a0;
        selfMountMs += System.currentTimeMillis() - m0;
        dirty = true;
        performRebuild();
    }

    /**
     * Whether this element contributes a component to the host's flat
     * container. Defaults to owning a real CN1 component; headless test
     * doubles may override to exercise the attach-order bookkeeping without
     * instantiating CN1 components (which require an initialized Display).
     */
    protected boolean ownsComponent() {
        return component != null;
    }

    /**
     * Disables CN1 component behaviors that fight the Flutter layout model:
     * <ul>
     *   <li>Text tickers — CN1 starts a marquee on a focused Label/Button
     *       whose text doesn't fit; Flutter clips instead, and a single
     *       transient under-sized layout pass would otherwise latch the
     *       ticker permanently ("bouncing" UI).</li>
     *   <li>Margins — styles derived from the Material theme carry
     *       millimeter margins. Our flat layout ignores margins, but CN1's
     *       own bookkeeping (focus scroll-to-visible, outer-size math) still
     *       sees them as phantom inflation, causing focus-driven jitter.</li>
     * </ul>
     */
    static void neutralizeCn1Behaviors(Component c) {
        if (c == null) {
            return;
        }
        if (c instanceof com.codename1.ui.Label) {
            ((com.codename1.ui.Label) c).setTickerEnabled(false);
        }
        try {
            if (needsNeutralizing(c)) {
                com.codename1.ui.plaf.Style all = c.getAllStyles();
                all.setMarginUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
                all.setMargin(0, 0, 0, 0);
                unifyStateMetrics(c);
            }
        } catch (Throwable ignore) {
            // styles unavailable headless
        }
    }

    /**
     * Flutter geometry is state-invariant, but theme-derived CN1 styles can
     * give the selected/pressed state a DIFFERENT font (often larger) and
     * padding than unselected. Our constraint pass sizes components from the
     * unselected metrics, so on focus CN1 would paint and re-measure with
     * bigger metrics — text overflow plus layout shuffle ("bounce"). Copy
     * the unselected font and padding onto every other state so a state
     * change can never alter geometry; state styles keep their own colors.
     */
    /// Whether this component needs its CN1 styling neutralised at all.
    ///
    /// getAllStyles() is not a read: it CREATES the selected, pressed and
    /// disabled styles -- UIManager documents that each "always return a new
    /// style instance" -- and then a proxy over the four. That is five Style
    /// objects per component, built during the first frame purely so a margin
    /// can be set to zero and the state metrics unified.
    ///
    /// Almost never necessary. A UIID whose theme already gives every state a
    /// zero margin and matching font and padding is already what neutralising
    /// would make it, so the whole thing can be skipped and those five objects
    /// never exist. Decided once per UIID from the THEME's own styles (which
    /// UIManager caches) rather than once per component, and re-decided when the
    /// theme generation changes.
    ///
    /// When a UIID genuinely does differ it is neutralised exactly as before, so
    /// this is a cost cut and not a behaviour change.
    private static final java.util.HashMap<String, Boolean> NEEDS_NEUTRALIZE =
            new java.util.HashMap<String, Boolean>();
    private static int neutralizeGeneration = -1;

    /// A component that can never enter the selected, pressed or disabled state
    /// never consults those styles, so unifying them is work for nobody.
    ///
    /// This is most of the tree. Text, icons, images, dividers and plain boxes
    /// are not focusable, are not Buttons and are enabled, so Codename One will
    /// only ever paint them from the unselected style -- yet each was made to
    /// build all three state styles plus a proxy so their metrics could be
    /// matched against a state that cannot happen.
    ///
    /// Deliberately narrow: anything focusable, any Button (which paints a
    /// pressed style on touch without being focused) and anything already
    /// disabled still goes through the full path, so the geometry guarantee
    /// holds exactly where a state change is possible.
    private static boolean canChangeState(Component c) {
        return c.isFocusable()
                || c instanceof com.codename1.ui.Button
                || !c.isEnabled();
    }

    private static boolean needsNeutralizing(Component c) {
        if (!canChangeState(c)) {
            return false;
        }
        String uiid = c.getUIID();
        if (uiid == null) {
            return true;
        }
        int gen = com.codename1.ui.plaf.UIManager.getThemeGeneration();
        if (gen != neutralizeGeneration) {
            NEEDS_NEUTRALIZE.clear();
            neutralizeGeneration = gen;
        }
        Boolean known = NEEDS_NEUTRALIZE.get(uiid);
        if (known != null) {
            return known.booleanValue();
        }
        boolean needs;
        try {
            com.codename1.ui.plaf.UIManager m = c.getUIManager();
            com.codename1.ui.plaf.Style un = m.getComponentStyle(uiid);
            needs = hasMargin(un)
                    || hasMargin(m.getComponentSelectedStyle(uiid))
                    || hasMargin(m.getComponentCustomStyle(uiid, "press"))
                    || hasMargin(m.getComponentCustomStyle(uiid, "dis"))
                    || metricsDiffer(un, m.getComponentSelectedStyle(uiid))
                    || metricsDiffer(un, m.getComponentCustomStyle(uiid, "dis"))
                    || metricsDiffer(un, m.getComponentCustomStyle(uiid, "press"));
        } catch (Throwable t) {
            needs = true;
        }
        NEEDS_NEUTRALIZE.put(uiid, Boolean.valueOf(needs));
        return needs;
    }

    /// A zero margin is zero in any unit, so the unit does not have to match.
    private static boolean hasMargin(com.codename1.ui.plaf.Style s) {
        if (s == null) {
            return false;
        }
        return s.getMarginTop() != 0 || s.getMarginBottom() != 0
                || s.getMarginLeftNoRTL() != 0 || s.getMarginRightNoRTL() != 0;
    }

    /// Whether a UIID's state styles differ from its unselected one at all.
    ///
    /// Asked ONCE per UIID, not once per component. getSelectedStyle(),
    /// getDisabledStyle() and getPressedStyle() do not read a shared object --
    /// UIManager documents that they "always return a new style instance" -- so
    /// touching all three to unify them minted three Style objects for every
    /// component mounted. At 377 components that is over eleven hundred Styles
    /// built during the first frame, and it measured as the 22ms "create" half
    /// of the component cost.
    ///
    /// Almost none of them need it: a UIID whose theme gives every state the
    /// same font and padding is already state-invariant, which is what the
    /// unification was there to guarantee. Deciding that from the THEME (whose
    /// per-UIID styles UIManager caches) settles it for every component sharing
    /// the UIID, and the ones that genuinely differ still get unified.
    ///
    /// Keyed by UIID and theme generation, so a theme change re-decides.
    private static final java.util.HashMap<String, Boolean> STATE_METRICS_DIFFER =
            new java.util.HashMap<String, Boolean>();
    private static int stateMetricsGeneration = -1;

    private static boolean statesDifferForUiid(Component c) {
        String uiid = c.getUIID();
        if (uiid == null) {
            return true;
        }
        com.codename1.ui.plaf.UIManager m = c.getUIManager();
        int gen = com.codename1.ui.plaf.UIManager.getThemeGeneration();
        if (gen != stateMetricsGeneration) {
            STATE_METRICS_DIFFER.clear();
            stateMetricsGeneration = gen;
        }
        Boolean known = STATE_METRICS_DIFFER.get(uiid);
        if (known != null) {
            return known.booleanValue();
        }
        boolean differs;
        try {
            com.codename1.ui.plaf.Style un = m.getComponentStyle(uiid);
            differs = metricsDiffer(un, m.getComponentSelectedStyle(uiid))
                    || metricsDiffer(un, m.getComponentCustomStyle(uiid, "dis"))
                    || metricsDiffer(un, m.getComponentCustomStyle(uiid, "press"));
        } catch (Throwable t) {
            differs = true;
        }
        STATE_METRICS_DIFFER.put(uiid, Boolean.valueOf(differs));
        return differs;
    }

    private static boolean metricsDiffer(com.codename1.ui.plaf.Style a,
                                         com.codename1.ui.plaf.Style b) {
        if (b == null) {
            return false;
        }
        return a.getFont() != b.getFont()
                || a.getPaddingTop() != b.getPaddingTop()
                || a.getPaddingBottom() != b.getPaddingBottom()
                || a.getPaddingLeftNoRTL() != b.getPaddingLeftNoRTL()
                || a.getPaddingRightNoRTL() != b.getPaddingRightNoRTL();
    }

    private static void unifyStateMetrics(Component c) {
        if (!statesDifferForUiid(c)) {
            // Already state-invariant: touching the state styles here would
            // create them for nothing.
            return;
        }
        com.codename1.ui.plaf.Style un = c.getUnselectedStyle();
        com.codename1.ui.Font font = un.getFont();
        int pt = un.getPaddingTop();
        int pb = un.getPaddingBottom();
        int pl = un.getPaddingLeftNoRTL();
        int pr = un.getPaddingRightNoRTL();
        java.util.List<com.codename1.ui.plaf.Style> states = new java.util.ArrayList<com.codename1.ui.plaf.Style>();
        states.add(c.getSelectedStyle());
        states.add(c.getDisabledStyle());
        if (c instanceof com.codename1.ui.Button) {
            states.add(((com.codename1.ui.Button) c).getPressedStyle());
        }
        for (com.codename1.ui.plaf.Style s : states) {
            if (font != null) {
                s.setFont(font);
            }
            s.setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
            s.setPadding(pt, pb, pl, pr);
            s.setMarginUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
            s.setMargin(0, 0, 0, 0);
        }
    }

    /**
     * Creates the retained CN1 component for this element, or null for pure
     * layout elements. Called once, on mount.
     */
    protected Component createComponent() {
        return null;
    }

    /**
     * Applies the current widget configuration to the retained component.
     * Called on every widget update (the component is mutated in place).
     */
    protected void updateComponent(Component c) {
    }

    @Override
    public void update(Widget newWidget) {
        super.update(newWidget);
        if (component != null) {
            updateComponent(component);
        }
        dirty = true;
        performRebuild();
        if (updateAffectsLayout()) {
            markNeedsLayout();
        } else {
            markNeedsPaint();
        }
    }

    /**
     * Whether a new configuration for THIS element can change geometry.
     *
     * <p>Flutter draws a hard line between {@code markNeedsPaint} and
     * {@code markNeedsLayout}, and it is not an optimization detail — marking
     * layout walks to the root, so a purely visual change to one widget would
     * otherwise relayout the entire screen on every frame it animates.</p>
     *
     * <p>Returning false is only safe when this element's size cannot depend on
     * its own configuration. It says nothing about the CHILDREN: if a rebuild
     * replaces a child, that child's own update marks layout and the walk passes
     * through here as usual.</p>
     */
    protected boolean updateAffectsLayout() {
        return true;
    }

    /**
     * The subtree must repaint, but every measurement stays valid.
     */
    public void markNeedsPaint() {
        if (component != null) {
            component.repaint();
        }
    }

    @Override
    protected void performRebuild() {
        dirty = false;
        syncChildren();
    }

    /**
     * Reconciles child widgets into child elements. Default: no children.
     */
    protected void syncChildren() {
    }

    @Override
    public void unmount() {
        super.unmount();
        if (host != null) {
            host.detach(this);
        }
    }

    /**
     * Theme change: re-apply the widget config (which re-derives any
     * programmatic, Theme.of-based styling) to the retained component and
     * invalidate the cached layout — fonts/metrics may have changed.
     */
    @Override
    public void themeChanged() {
        if (component != null) {
            updateComponent(component);
        }
        markNeedsLayout();
        super.themeChanged();
    }

    public Component component() {
        return component;
    }

    // ------------------------------------------------------------------
    // Layout protocol
    // ------------------------------------------------------------------

    /**
     * Runs (or reuses the cached result of) the layout pass for this box.
     */
    /// Diagnostic counters for {@link BuildOwner#traceFrames(boolean)}: how much of a layout
    /// pass the constraints-to-size cache actually absorbs. A pass that misses on nearly
    /// every box is re-laying out the whole tree for one changed leaf.
    /// True while a dry measurement is running, so nested layout() calls measure dryly too.
    private static boolean dryPass;

    /**
     * Whether the pass currently running is a dry measurement.
     *
     * <p>performLayout is allowed to have side effects — it writes child
     * offsets, and a text box writes the lines it wrapped — but a DRY pass runs
     * against constraints that are not the ones the box will be painted at, so
     * anything it publishes for the painter is wrong. Text was writing its
     * wrapped lines unconditionally, so a dry measurement at unbounded width
     * left the label holding one long unwrapped line and it painted straight
     * past its own edge.
     */
    protected static boolean isDryPass() {
        return dryPass;
    }

    static long layoutCalls;
    static long layoutHits;
    static long layoutMissDirty;
    static long layoutMissConstraints;

    /// Self time inside performLayout per element class, so a slow pass names the widget
    /// responsible instead of just being slow. Self time, not total: a parent's entry would
    /// otherwise swallow its whole subtree and every pass would blame the root.
    static final java.util.Map<String, long[]> layoutSelfNanos = new java.util.HashMap<String, long[]>();
    private static long childNanos;

    static void resetLayoutCounters() {
        layoutCalls = 0;
        layoutHits = 0;
        layoutMissDirty = 0;
        layoutMissConstraints = 0;
        layoutSelfNanos.clear();
        childNanos = 0;
    }

    /// The costliest element classes by self time, worst first.
    static String hotLayoutClasses(int top) {
        java.util.List<java.util.Map.Entry<String, long[]>> all =
                new java.util.ArrayList<java.util.Map.Entry<String, long[]>>(layoutSelfNanos.entrySet());
        java.util.Collections.sort(all, new java.util.Comparator<java.util.Map.Entry<String, long[]>>() {
            @Override
            public int compare(java.util.Map.Entry<String, long[]> a, java.util.Map.Entry<String, long[]> b) {
                return Long.compare(b.getValue()[0], a.getValue()[0]);
            }
        });
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(top, all.size()); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"class\":\"").append(all.get(i).getKey())
                    .append("\",\"ms\":").append(all.get(i).getValue()[0] / 1000000)
                    .append(",\"calls\":").append(all.get(i).getValue()[1]).append('}');
        }
        return sb.append(']').toString();
    }

    /// Runs performLayout while attributing only its OWN time to this element's class.
    private Size timedPerformLayout(BoxConstraints constraints) {
        if (!Trace.on()) {
            return performLayout(constraints);
        }
        long start = System.nanoTime();
        long childrenBefore = childNanos;
        childNanos = 0;
        Size result;
        try {
            result = performLayout(constraints);
        } finally {
            long elapsed = System.nanoTime() - start;
            long self = elapsed - childNanos;
            String key = getClass().getSimpleName();
            long[] slot = layoutSelfNanos.get(key);
            if (slot == null) {
                slot = new long[2];
                layoutSelfNanos.put(key, slot);
            }
            slot[0] += self;
            slot[1]++;
            childNanos = childrenBefore + elapsed;
        }
        return result;
    }

    /**
     * Measures this box WITHOUT making the result the authoritative layout - Flutter's dry
     * layout. Codename One asks a container for its preferred size far more often than it
     * lays it out (scroll extents, focus maths, revalidate), and that question arrives with
     * different constraints than the real pass. With one cache slot the two alternate and
     * evict each other on every box in the tree, so a single changed leaf re-measured
     * everything: on the gallery home that was a 92% miss rate over 7600 layout calls.
     *
     * <p>The dry result gets its own slot and never clears {@code needsLayout}, so the real
     * pass still runs. A dry HIT skips the subtree entirely, which is the whole point.</p>
     */
    public final Size dryLayout(BoxConstraints constraints) {
        layoutCalls++;
        if (!needsLayout && drySize != null && constraints.equals(lastDryConstraints)) {
            layoutHits++;
            return drySize;
        }
        if (needsLayout) {
            layoutMissDirty++;
        } else {
            layoutMissConstraints++;
        }
        lastDryConstraints = constraints;
        // Dryness has to propagate. performLayout measures its children through layout(),
        // so without this flag a dry pass would write dry constraints into every
        // descendant's REAL slot and the real pass that follows would miss on all of them -
        // which is most of what made a one-element change re-measure the whole tree.
        // Layout runs on the EDT, so a plain static is the whole of the bookkeeping.
        boolean outer = dryPass;
        dryPass = true;
        try {
            drySize = timedPerformLayout(constraints);
        } finally {
            dryPass = outer;
        }
        // performLayout is NOT side-effect free: it writes child offsets, and it just wrote
        // them for the dry constraints. So the real pass has to recompute them - if it were
        // allowed to hit its cache it would keep the dry offsets and place children wrongly
        // (this put the study card's caption at the top of the card instead of the bottom).
        // Drop only THIS element's real result; ancestors are untouched, so this does not
        // escalate into the whole-tree invalidation the cache exists to avoid.
        lastConstraints = null;
        trace(true, constraints, drySize);
        return drySize;
    }

    /**
     * Drops this element's cached layout result, so the next {@code layout} call
     * runs {@link #performLayout} again even if the constraints have not changed.
     *
     * <p>For a pass that deliberately did not compute the real answer. The dry
     * path above does this for itself; {@code LayoutBuilder} needs it when it
     * sits out a speculative unbounded measurement, because the pass that
     * follows can arrive with those same constraints and must not be handed the
     * placeholder the sat-out pass returned.</p>
     */
    protected final void invalidateLayoutCache() {
        lastConstraints = null;
    }

    public final Size layout(BoxConstraints constraints) {
        if (dryPass) {
            return dryLayout(constraints);
        }
        layoutCalls++;
        if (!needsLayout && constraints.equals(lastConstraints)) {
            layoutHits++;
            return size;
        }
        if (needsLayout) {
            layoutMissDirty++;
        } else {
            layoutMissConstraints++;
        }
        lastConstraints = constraints;
        boolean wasInLayout = inLayout;
        boolean wasRemarked = remarkedDuringLayout;
        inLayout = true;
        remarkedDuringLayout = false;
        try {
            size = timedPerformLayout(constraints);
        } finally {
            inLayout = wasInLayout;
        }
        // A box invalidated while it was being computed is not clean when it finishes.
        needsLayout = remarkedDuringLayout;
        if (remarkedDuringLayout) {
            // ...and its cached constraints must not answer for the stale pass either.
            lastConstraints = null;
        }
        remarkedDuringLayout = wasRemarked;
        trace(false, constraints, size);
        return size;
    }

    /// Set to a class-name substring with -Dcn1.flutter.layout.trace to print every
    /// layout of the matching elements: which constraints went in, which size came out,
    /// and whether the pass was dry.
    ///
    /// The distinction that matters is dry-vs-real. A dry measurement runs the same
    /// performLayout, so it writes whatever that method keeps in fields; if the last pass
    /// over an element was dry, its component can end up sized from a measurement taken
    /// under constraints that were never real.
    private static final String LAYOUT_TRACE = System.getProperty("cn1.flutter.layout.trace");

    private void trace(boolean dry, BoxConstraints c, Size s) {
        if (LAYOUT_TRACE == null || !getClass().getName().contains(LAYOUT_TRACE)) {
            return;
        }
        com.codename1.io.Log.p((dry ? "[dry] " : "[lay] ") + getClass().getSimpleName()
                + " in=" + c + " out=" + s);
    }

    /**
     * Computes this box's size under the given constraints and stores each
     * render child's offset via {@link #setChildOffset}.
     */
    protected abstract Size performLayout(BoxConstraints constraints);

    /**
     * Invalidates the cached layout of this box and all its render ancestors
     * so the next pass recomputes down this branch.
     */
    /// Whether this box still owes a layout -- true when it has never been measured, or
    /// when something invalidated it while it was being measured.
    public boolean needsLayout() {
        return needsLayout;
    }

    public void markNeedsLayout() {
        for (Element a = this; a != null; a = a.parent) {
            if (a instanceof RenderElement) {
                RenderElement r = (RenderElement) a;
                r.needsLayout = true;
                if (r.inLayout) {
                    r.remarkedDuringLayout = true;
                }
                // The dry measurement is just as stale as the real one.
                r.drySize = null;
                r.lastDryConstraints = null;
            }
        }
    }

    /**
     * Positions this box at absolute host coordinates, writes the bounds of
     * the retained component (if any) and recursively positions render
     * children at their stored offsets.
     */
    public void position(int x, int y) {
        absX = x;
        absY = y;
        if (component != null) {
            component.setX(x);
            component.setY(y);
            component.setWidth((int) Math.round(size.width()));
            component.setHeight((int) Math.round(size.height()));
        }
        positionChildren(x, y);
    }

    /**
     * Default child positioning: every same-host render child goes to its
     * offset stored during performLayout.
     */
    protected void positionChildren(int x, int y) {
        for (RenderElement child : renderChildren()) {
            child.position(x + (int) Math.round(child.relX), y + (int) Math.round(child.relY));
        }
    }

    protected void setChildOffset(RenderElement child, double dx, double dy) {
        child.relX = dx;
        child.relY = dy;
    }

    public double relX() {
        return relX;
    }

    public double relY() {
        return relY;
    }

    public int x() {
        return absX;
    }

    public int y() {
        return absY;
    }

    public Size size() {
        return size;
    }

    // ------------------------------------------------------------------
    // Render tree navigation
    // ------------------------------------------------------------------

    /**
     * Descends from an element through composition elements to the first
     * render element (Flutter's renderObject lookup), or null.
     */
    public static RenderElement findRenderElement(Element e) {
        while (e != null) {
            if (e instanceof RenderElement) {
                return (RenderElement) e;
            }
            final Element[] first = new Element[1];
            e.visitChildren(new Funcs.VoidFunc1<Element>() {
                @Override
                public void call(Element c) {
                    if (first[0] == null) {
                        first[0] = c;
                    }
                }
            });
            e = first[0];
        }
        return null;
    }

    /**
     * The render elements directly below this one (descending through
     * composition), in tree order, excluding children routed to a different
     * host (e.g. a root Scaffold's appBar living in the Toolbar).
     */
    public List<RenderElement> renderChildren() {
        final List<RenderElement> out = new ArrayList<RenderElement>();
        visitChildren(new Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element c) {
                RenderElement r = findRenderElement(c);
                if (r != null && r.host == RenderElement.this.host) {
                    out.add(r);
                }
            }
        });
        return out;
    }
}
