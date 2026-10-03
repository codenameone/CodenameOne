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

import com.codename1.flutter.EdgeInsetsGeometry;
import com.codename1.flutter.Widget;

/**
 * A single selectable entry in a {@link NavigationRail} — Flutter's
 * {@code NavigationRailDestination}. Signature-only: the icon/label widgets are
 * captured for the rail to lay out; disabled/tooltip are recorded but unused.
 */
public class NavigationRailDestination {

    private Widget icon;
    private Widget selectedIcon;
    private Widget label;
    private EdgeInsetsGeometry padding;
    private boolean disabled;
    private String indicatorColorTooltip;

    public void icon(Widget v) { this.icon = v; }
    public void selectedIcon(Widget v) { this.selectedIcon = v; }
    public void label(Widget v) { this.label = v; }
    public void padding(EdgeInsetsGeometry v) { this.padding = v; }
    public void disabled(boolean v) { this.disabled = v; }
    public void indicatorColorTooltip(String v) { this.indicatorColorTooltip = v; }

    public Widget getIcon() { return icon; }
    public Widget getSelectedIcon() { return selectedIcon; }
    public Widget getLabel() { return label; }
}
