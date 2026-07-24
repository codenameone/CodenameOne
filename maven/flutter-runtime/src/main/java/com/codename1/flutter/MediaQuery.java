package com.codename1.flutter;

/**
 * Display-metric lookup, mirroring Flutter's {@code MediaQuery.of(context)}.
 * There is no inherited-widget scoping in this runtime — the metrics are
 * computed on demand from the CN1 Display, so every context sees the same
 * (current) values.
 */
public class MediaQuery extends StatelessWidget {

    private MediaQueryData data;
    private Widget child;

    public MediaQuery() {
    }

    /** The metrics this scope imposes on its subtree — Flutter's {@code MediaQuery.data}. */
    public void data(MediaQueryData v) {
        this.data = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }

    public static MediaQueryData of(BuildContext context) {
        return MediaQueryData.fromDisplay();
    }

    /** {@code MediaQuery.sizeOf}: the ambient display size. */
    public static com.codename1.flutter.rendering.Size sizeOf(BuildContext context) {
        return MediaQueryData.fromDisplay().size();
    }

    /** {@code MediaQuery.paddingOf}: the ambient safe-area padding. */
    public static EdgeInsets paddingOf(BuildContext context) {
        return MediaQueryData.fromDisplay().padding();
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
