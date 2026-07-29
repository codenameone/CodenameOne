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

    @Override
    public void mount(Element parent, int slot) {
        super.mount(parent, slot);
        component = createComponent();
        neutralizeCn1Behaviors(component);
        if (host != null && ownsComponent()) {
            // Components attach in mount (depth-first) order, which equals
            // element-tree order; when this mount replaces an existing
            // subtree, Element.updateChild anchors the host's insertion
            // cursor at the replaced components' index so the flat
            // container's z-order stays in sync with the tree.
            host.attach(this);
        }
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
            com.codename1.ui.plaf.Style all = c.getAllStyles();
            all.setMarginUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
            all.setMargin(0, 0, 0, 0);
            unifyStateMetrics(c);
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
    private static void unifyStateMetrics(Component c) {
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
        markNeedsLayout();
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

    static long layoutCalls;
    static long layoutHits;
    static long layoutMissDirty;
    static long layoutMissConstraints;

    static void resetLayoutCounters() {
        layoutCalls = 0;
        layoutHits = 0;
        layoutMissDirty = 0;
        layoutMissConstraints = 0;
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
            drySize = performLayout(constraints);
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
        return drySize;
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
        size = performLayout(constraints);
        needsLayout = false;
        return size;
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
    public void markNeedsLayout() {
        for (Element a = this; a != null; a = a.parent) {
            if (a instanceof RenderElement) {
                RenderElement r = (RenderElement) a;
                r.needsLayout = true;
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
