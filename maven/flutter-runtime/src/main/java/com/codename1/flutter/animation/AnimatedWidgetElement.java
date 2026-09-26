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

import com.codename1.flutter.StatelessElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.foundation.Listenable;

import dart.runtime.Funcs;

/**
 * Element for {@link AnimatedWidget}: listens to the widget's {@code listenable} and rebuilds
 * on every notification — Flutter's {@code _AnimatedState}.
 *
 * <p>Without this an AnimatedWidget is a plain StatelessWidget that happens to read an
 * animation: it samples the value once, paints a correct first frame, and never moves again.
 * The gallery's settings button is exactly that shape — it renders
 * {@code SettingsIcon(animationController.value)}, so a frozen subscription leaves the icon
 * stuck on whichever glyph it was built with while the panel behind it opens and closes.</p>
 *
 * <p>{@link AnimatedBuilderElement} already did this for the builder-callback form; the
 * subclass form went without, which is why three of the gallery's widgets — this icon,
 * Shrine's backdrop title and Rally's pie chart — were all still.</p>
 */
public class AnimatedWidgetElement extends StatelessElement {

    private Listenable listened;
    private final Funcs.VoidFunc0 handler = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            markNeedsBuild();
        }
    };

    public AnimatedWidgetElement(AnimatedWidget widget) {
        super(widget);
    }

    @Override
    public void mount(com.codename1.flutter.Element parent, int slot) {
        super.mount(parent, slot);
        subscribe();
    }

    @Override
    public void update(Widget newWidget) {
        // Resubscribed around the swap: a rebuilt widget may carry a DIFFERENT listenable,
        // and holding the old one leaks a listener onto a controller that outlives us.
        unsubscribe();
        super.update(newWidget);
        subscribe();
    }

    @Override
    public void unmount() {
        unsubscribe();
        super.unmount();
    }

    private void subscribe() {
        Widget w = widget();
        if (!(w instanceof AnimatedWidget)) {
            return;
        }
        listened = ((AnimatedWidget) w).listenable();
        if (listened != null) {
            listened.addListener(handler);
        }
    }

    private void unsubscribe() {
        if (listened != null) {
            listened.removeListener(handler);
            listened = null;
        }
    }
}
