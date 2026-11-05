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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Kit;
import dart.core.DartList;
import dart.runtime.Funcs;

/** Flutter's {@code ButtonSegment<T>}: one segment of a {@link SegmentedButton}. */
public class ButtonSegment<T> {
    private T value;
    private Widget label;
    private Widget icon;
    private boolean enabled = true;
    public void value(T v) { this.value = v; }
    public void label(Widget v) { this.label = v; }
    public void icon(Widget v) { this.icon = v; }
    public void enabled(Boolean v) { this.enabled = v == null || v.booleanValue(); }
    public void tooltip(String v) { }
    public T getValue() { return value; }
    public Widget getLabel() { return label; }
    public Widget getIcon() { return icon; }
    public boolean isEnabled() { return enabled; }
}
