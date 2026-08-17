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

    /** Registers {@code e} as reading this widget; idempotent, since a rebuild re-reads. */
    public void addDependent(Element e) {
        if (e != null && !dependents.contains(e)) {
            dependents.add(e);
        }
    }

    public void removeDependent(Element e) {
        dependents.remove(e);
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
                for (int i = 0; i < snapshot.size(); i++) {
                    Element e = snapshot.get(i);
                    // Re-checked here, not at snapshot time: an element that left the tree
                    // in the meantime must never be scheduled.
                    if (e.isMounted()) {
                        e.markNeedsBuild();
                    } else {
                        dependents.remove(e);
                    }
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
        // GATED OFF by default. Rebuilding the readers is what this class is for, and it
        // is correct in every headless test - but on a device it empties the settings page,
        // both when run inline and when deferred past the flush. Something about rebuilding
        // one of these particular readers tears the page down, and shipping a blank page is
        // worse than shipping a stale switch. Flip cn1.flutter.inheritedNotify to work on
        // it; cn1.flutter.inheritedCensus above reports who the readers actually are, which
        // is the missing piece.
        if ("true".equals(com.codename1.ui.Display.getInstance()
                .getProperty("cn1.flutter.inheritedNotify", "false"))) {
            com.codename1.ui.CN.callSerially(mark);
        }
    }
}
