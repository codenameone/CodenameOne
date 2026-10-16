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

import com.codename1.flutter.navigation.Route;

import dart.core.DartIterable;
import dart.core.DartList;

/**
 * A {@link Route} that inserts one or more {@link OverlayEntry} objects into the
 * navigator's {@link Overlay} — Flutter's {@code OverlayRoute}. Subclasses
 * override {@link #createOverlayEntries()} to supply their entries (e.g. a page
 * plus its modal barrier). This pass captures the entries; wiring them into the
 * live overlay lands with the navigation renderer.
 *
 * @param <T> the value the route completes with when popped
 */
public class OverlayRoute<T> extends Route<T> {

    /** The overlay entries this route paints. Subclasses override. */
    public DartIterable<OverlayEntry> createOverlayEntries() {
        return DartIterable.wrap(new DartList<OverlayEntry>());
    }
}
