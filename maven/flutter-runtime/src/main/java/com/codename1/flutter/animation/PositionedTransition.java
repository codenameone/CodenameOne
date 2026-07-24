package com.codename1.flutter.animation;

import com.codename1.flutter.Element;
import com.codename1.flutter.RelativeRect;
import com.codename1.flutter.widgets.Positioned;
import com.codename1.flutter.widgets.PositionedRenderElement;

/**
 * Animates the position/size (a {@code RelativeRect}) of a child within a
 * Stack — Flutter's {@code PositionedTransition}. It resolves the animation's
 * current {@code RelativeRect} and hosts the child as a {@link Positioned}
 * (LTRB insets from the stack edges) so the Stack lays it out in place. The
 * gallery's Backdrop drives two of these to slide the home/settings panels; at
 * rest the home's rect is {@code RelativeRect.fill} (fills the stack) and the
 * settings rect sits off the top edge (hidden). Interpolated motion during the
 * slide is deferred; the resolved rest/target frame is correct.
 */
public class PositionedTransition extends AnimatedChildWidget {

    private Animation<?> rect;

    public void rect(Animation<?> v) {
        this.rect = v;
    }

    public Animation<?> getRect() {
        return rect;
    }

    @Override
    public Element createElement() {
        Positioned p = new Positioned();
        Object v = rect != null ? rect.value() : null;
        RelativeRect r = v instanceof RelativeRect ? (RelativeRect) v : RelativeRect.fill;
        p.left(r.left());
        p.top(r.top());
        p.right(r.right());
        p.bottom(r.bottom());
        p.child(getChild());
        return new PositionedRenderElement(p);
    }
}
