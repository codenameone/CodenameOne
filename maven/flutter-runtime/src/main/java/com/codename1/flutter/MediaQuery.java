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
        com.codename1.flutter.rendering.RenderHost h = ((Element) context).host();
        return h == null ? null : h.form();
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
