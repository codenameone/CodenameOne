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

import com.codename1.flutter.StatelessElement;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.DartRuntime;
import dart.runtime.Dyn;

/**
 * Element for a {@link Provider}: when a rebuild hands it a different value, the widgets
 * that read the old one rebuild, as provider's updateShouldNotify (by default,
 * {@code previous != next}) has them do. Swapping the value alone did nothing on
 * screen whenever the provider's child was the same widget instance -- reconciliation
 * skips an identical child, and nothing told a Consumer or Selector below it that the
 * value it showed had been replaced.
 */
public class ProviderElement extends StatelessElement {

    public ProviderElement(StatelessWidget widget) {
        super(widget);
    }

    /// The value this element made by calling the provider's `create`, kept across
    /// rebuilds: the parent hands down a new Provider widget every build, and Flutter
    /// creates once per element, not once per widget.
    private Object created;
    private boolean hasCreated;

    @Override
    protected void firstBuild() {
        // After Element.mount linked the parent, so create(context) can read the
        // providers above it -- `create: (c) => Cubit(c.read<Repo>())`.
        supplyCreated(widget());
        super.firstBuild();
    }

    private void supplyCreated(Widget w) {
        if (!(w instanceof Provider)) {
            return;
        }
        Provider p = (Provider) w;
        if (p.getValue() != null || p.create == null) {
            return;
        }
        if (!hasCreated) {
            created = Dyn.invoke(p.create, this);
            hasCreated = true;
        }
        p.value(created);
    }

    @Override
    public void unmount() {
        if (hasCreated && widget() instanceof Provider) {
            ((Provider) widget()).disposeCreated(this, created);
        }
        super.unmount();
    }

    @Override
    public void update(Widget newWidget) {
        Object before = valueOf(widget());
        supplyCreated(newWidget);
        super.update(newWidget);
        if (!DartRuntime.eq(before, valueOf(widget()))) {
            rebuildProviderDependents();
        }
    }

    private static Object valueOf(Widget w) {
        return w instanceof Provider ? ((Provider) w).getValue() : null;
    }
}
