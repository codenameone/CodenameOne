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
package com.codename1.flutter.material;

import com.codename1.flutter.StatelessElement;
import com.codename1.flutter.Widget;

/**
 * Element for a {@link LocalizationsScope}: when an app rebuild hands it resources for a
 * different locale (or a different set of delegates), every widget that looked them up
 * through {@code Localizations.of} rebuilds. The app's page is normally the same widget
 * instance across such a rebuild, so reconciliation alone never reaches its readers.
 */
public class LocalizationsScopeElement extends StatelessElement {

    public LocalizationsScopeElement(LocalizationsScope widget) {
        super(widget);
    }

    @Override
    public void update(Widget newWidget) {
        Widget before = widget();
        super.update(newWidget);
        if (before != newWidget && before instanceof LocalizationsScope
                && newWidget instanceof LocalizationsScope
                && !((LocalizationsScope) newWidget).sameResourcesAs((LocalizationsScope) before)) {
            rebuildProviderDependents();
        }
    }
}
