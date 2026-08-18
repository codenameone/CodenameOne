package com.codename1.flutter.widgets;

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Insets its child to avoid system intrusions — the status bar, the notch and the home
 * indicator. Flutter's {@code SafeArea}.
 *
 * <p>It used to render the child unchanged, on the theory that Codename One's Form already
 * keeps content clear. That is not true of a Flutter subtree laid out inside a raw
 * container: content ran under the notch and the home indicator, and controls near the
 * bottom edge could not be reached at all. Every SafeArea in the app was a no-op.</p>
 *
 * <p>It resolves to a {@link Padding} over the ambient {@code MediaQuery.padding}, taking
 * only the edges whose flags are set and never less than {@code minimum} — which is what
 * Flutter's own implementation does.</p>
 */
public class SafeArea extends com.codename1.flutter.StatelessWidget implements HasChild {

    private boolean left = true;
    private boolean top = true;
    private boolean right = true;
    private boolean bottom = true;
    private EdgeInsets minimum;
    private boolean maintainBottomViewPadding;
    private Widget child;

    public void left(boolean v) {
        this.left = v;
    }

    public void top(boolean v) {
        this.top = v;
    }

    public void right(boolean v) {
        this.right = v;
    }

    public void bottom(boolean v) {
        this.bottom = v;
    }

    public void minimum(EdgeInsets v) {
        this.minimum = v;
    }

    public void maintainBottomViewPadding(boolean v) {
        this.maintainBottomViewPadding = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(com.codename1.flutter.BuildContext context) {
        EdgeInsets safe = com.codename1.flutter.MediaQuery.of(context).padding();
        double l = left ? safe.left() : 0;
        double t = top ? safe.top() : 0;
        double r = right ? safe.right() : 0;
        double b = bottom ? safe.bottom() : 0;
        if (minimum != null) {
            l = Math.max(l, minimum.left());
            t = Math.max(t, minimum.top());
            r = Math.max(r, minimum.right());
            b = Math.max(b, minimum.bottom());
        }
        if (l == 0 && t == 0 && r == 0 && b == 0) {
            return child;
        }
        Padding pad = new Padding();
        pad.padding(EdgeInsets.fromLTRB(l, t, r, b));
        pad.child(child);
        return pad;
    }
}
