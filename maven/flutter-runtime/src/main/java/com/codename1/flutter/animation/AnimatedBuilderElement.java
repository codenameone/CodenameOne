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

import com.codename1.flutter.ComposedElement;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Element for {@link AnimatedBuilder}: subscribes to the widget's animation on
 * mount and rebuilds (re-invoking the builder) on every notification, mirroring
 * Flutter's {@code AnimatedWidget}/{@code _AnimatedState} listen-and-rebuild.
 */
public class AnimatedBuilderElement extends ComposedElement {

    private com.codename1.flutter.foundation.Listenable listened;
    private final Funcs.VoidFunc0 handler = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            markNeedsBuild();
        }
    };

    public AnimatedBuilderElement(AnimatedBuilder widget) {
        super(widget);
    }

    @Override
    public void mount(Element parent, int slot) {
        super.mount(parent, slot);
        subscribe();
    }

    @Override
    public void update(Widget newWidget) {
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
        listened = ((AnimatedBuilder) widget()).getAnimation();
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

    @Override
    protected Widget build() {
        AnimatedBuilder w = (AnimatedBuilder) widget();
        Funcs.Func2<com.codename1.flutter.BuildContext, Widget, Widget> b = w.getBuilder();
        if (b == null) {
            return null;
        }
        return b.call(this, w.getChild());
    }
}
