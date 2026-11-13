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

import com.codename1.flutter.Element;
import com.codename1.flutter.foundation.Listenable;

/// An InheritedWidget whose dependents rebuild whenever its {@code notifier}
/// notifies -- Flutter's `InheritedNotifier`. A subclass need not implement
/// {@code updateShouldNotify}: a different notifier is what counts as a change.
public class InheritedNotifier<T extends Listenable> extends InheritedWidget {

    private T notifier;

    public void notifier(T v) {
        this.notifier = v;
    }

    /// Flutter's {@code notifier}.
    public T notifier() {
        return notifier;
    }

    @Override
    public boolean updateShouldNotify(InheritedWidget oldWidget) {
        return !(oldWidget instanceof InheritedNotifier) || ((InheritedNotifier<?>) oldWidget).notifier != notifier;
    }

    @Override
    public Element createElement() {
        return new NotifierElement(this);
    }

    /// Listens to the current widget's notifier while mounted, and tells the
    /// dependents when it fires.
    static final class NotifierElement extends InheritedElement {
        private Listenable listening;
        private final dart.runtime.Funcs.VoidFunc0 changed = new dart.runtime.Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (isMounted()) {
                    dependenciesChanged();
                }
            }
        };

        NotifierElement(InheritedNotifier<?> w) {
            super(w);
        }

        private void listen() {
            Listenable n = widget() instanceof InheritedNotifier ? ((InheritedNotifier<?>) widget()).notifier : null;
            if (n == listening) {
                return;
            }
            if (listening != null) {
                listening.removeListener(changed);
            }
            listening = n;
            if (n != null) {
                n.addListener(changed);
            }
        }

        @Override
        public void mount(Element parent, int slot) {
            super.mount(parent, slot);
            listen();
        }

        @Override
        public void update(com.codename1.flutter.Widget newWidget) {
            super.update(newWidget);
            listen();
        }

        @Override
        public void unmount() {
            if (listening != null) {
                listening.removeListener(changed);
                listening = null;
            }
            super.unmount();
        }
    }
}
