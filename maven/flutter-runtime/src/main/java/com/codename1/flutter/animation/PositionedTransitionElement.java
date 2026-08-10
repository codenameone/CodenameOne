package com.codename1.flutter.animation;

import com.codename1.flutter.Element;
import com.codename1.flutter.RelativeRect;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Positioned;
import com.codename1.flutter.widgets.PositionedRenderElement;

import dart.runtime.Funcs;

/**
 * Element for {@link PositionedTransition}: re-reads the animation's
 * {@code RelativeRect} on every notification and re-lays the child out at the new
 * insets, mirroring Flutter's {@code AnimatedWidget} rebuild.
 *
 * <p>It used to resolve the rect once when the element was created. That is fine while the
 * animation is at rest at its END value and catastrophic when it rests at its START value:
 * the gallery's settings panel begins one full screen-height ABOVE the viewport and slides
 * down, so a panel that never moved simply stayed off-screen. Tapping the settings button
 * ran the whole toggle — the notifier flipped, the controllers animated, the subtree
 * rebuilt — and produced no visible change, which reads as a dead button.</p>
 *
 * <p>It stays a {@link PositionedRenderElement} rather than a composed element that builds
 * one, because {@code StackRenderElement} decides what is positioned by looking for this
 * type; wrapping it would make the Stack treat the panel as a non-positioned child and
 * size the whole Stack to it.</p>
 */
public class PositionedTransitionElement extends PositionedRenderElement {

    private com.codename1.flutter.foundation.Listenable listened;

    private final Funcs.VoidFunc0 handler = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            applyRect();
        }
    };

    public PositionedTransitionElement(Positioned positioned) {
        super(positioned);
    }

    /// The transition this element animates. Held separately from {@code widget()},
    /// which is the synthesised Positioned carrying the current insets.
    private PositionedTransition transition;

    void transition(PositionedTransition t) {
        this.transition = t;
    }

    @Override
    public void mount(Element parent, int slot) {
        super.mount(parent, slot);
        subscribe();
        applyRect();
    }

    @Override
    public void unmount() {
        unsubscribe();
        super.unmount();
    }

    private void subscribe() {
        Animation<?> a = transition == null ? null : transition.getRect();
        if (a instanceof com.codename1.flutter.foundation.Listenable) {
            listened = (com.codename1.flutter.foundation.Listenable) a;
            listened.addListener(handler);
        }
    }

    private void unsubscribe() {
        if (listened != null) {
            listened.removeListener(handler);
            listened = null;
        }
    }

    /** Copies the animation's current rect onto the Positioned and re-lays it out. */
    private void applyRect() {
        if (transition == null) {
            return;
        }
        Animation<?> a = transition.getRect();
        Object v = a == null ? null : a.value();
        RelativeRect r = v instanceof RelativeRect ? (RelativeRect) v : RelativeRect.fill;
        Positioned p = positioned();
        p.left(r.left());
        p.top(r.top());
        p.right(r.right());
        p.bottom(r.bottom());
        // Insets are geometry: the Stack resolves them into this box's constraints and
        // offset, so a changed rect is a layout change, not a repaint.
        markNeedsLayout();
        RenderElement host = this;
        while (host != null && host.parent() instanceof RenderElement) {
            host = (RenderElement) host.parent();
        }
        if (host != null && host.host() != null) {
            host.host().revalidate();
        }
    }

    @Override
    public void update(Widget newWidget) {
        super.update(newWidget);
        applyRect();
    }
}
