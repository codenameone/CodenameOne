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
package com.codename1.flutter.widgets;

import com.codename1.flutter.ComposedElement;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.foundation.ValueListenable;

import dart.runtime.Funcs;

/**
 * Element for {@link ValueListenableBuilder}: subscribes to the listenable on mount and
 * rebuilds on every notification, mirroring Flutter's
 * {@code _ValueListenableBuilderState}.
 *
 * <p>Without the subscription the builder still produced a correct FIRST frame, which is
 * why this looked like it worked: the widget rendered, and only stopped tracking after
 * that. Everything driven by a ValueNotifier was therefore frozen at its initial value —
 * in the gallery, the settings button toggled its notifier and nothing on screen moved,
 * so the whole settings panel was unreachable.</p>
 */
public class ValueListenableBuilderElement extends ComposedElement {

    private ValueListenable<Object> listened;

    private final Funcs.VoidFunc0 handler = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            markNeedsBuild();
        }
    };

    public ValueListenableBuilderElement(ValueListenableBuilder<?> widget) {
        super(widget);
    }

    @Override
    public void mount(Element parent, int slot) {
        super.mount(parent, slot);
        subscribe();
    }

    @Override
    public void update(Widget newWidget) {
        // The new configuration may name a DIFFERENT listenable; resubscribing
        // unconditionally is simpler than comparing and cannot leave a stale listener
        // attached to the old one.
        unsubscribe();
        super.update(newWidget);
        subscribe();
    }

    @Override
    public void unmount() {
        unsubscribe();
        super.unmount();
    }

    @SuppressWarnings("unchecked")
    private void subscribe() {
        Object l = ((ValueListenableBuilder<?>) widget()).getValueListenable();
        if (l instanceof ValueListenable) {
            listened = (ValueListenable<Object>) l;
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
        return ((ValueListenableBuilder<?>) widget()).buildWith(this);
    }
}
