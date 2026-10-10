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
package com.codename1.flutter.provider;

import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.foundation.Listenable;

import dart.runtime.Funcs;

/**
 * Element for a provider whose value is a {@link Listenable}: subscribes on mount and
 * rebuilds its subtree on every notification.
 *
 * <p>Without this a provider was a plain widget that read its value once. Calling
 * {@code notifyListeners()} on the model then changed nothing on screen, and every
 * control whose whole job is to set a field on it did nothing at all -- the mail study's
 * search button, its mailbox switcher, its starring and deleting. They were not
 * unwired: the handler ran, the model changed, and no one was listening.</p>
 */
public class ChangeNotifierProviderElement extends ProviderElement {

    private Listenable listened;

    private final Funcs.VoidFunc0 handler = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            markNeedsBuild();
            // And everything that READ the model. Rebuilding only this element achieves
            // nothing: its build hands back the same child widget instance and
            // reconciliation returns early.
            rebuildProviderDependents();
        }
    };

    public ChangeNotifierProviderElement(StatelessWidget widget) {
        super(widget);
    }

    @Override
    public void mount(Element parent, int slot) {
        super.mount(parent, slot);
        subscribe();
    }

    @Override
    public void update(Widget newWidget) {
        // ProviderElement.update rebuilds the dependents when this brings a different
        // model; the subscription moves to the new one either way.
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
        Object value = widget() instanceof Provider ? ((Provider) widget()).getValue() : null;
        if (value instanceof Listenable) {
            listened = (Listenable) value;
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
