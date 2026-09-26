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
