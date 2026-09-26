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

import dart.core.DartList;

import java.util.ArrayList;
import java.util.List;

/**
 * The mutable state of an {@link Overlay} — Flutter's {@code OverlayState}.
 *
 * <p>Holds the live entry list and rebuilds the overlay whenever it changes.
 * {@code insert} and {@code insertAll} used to be empty method bodies, so an
 * entry could be built, inserted and removed without anything ever appearing.
 */
public class OverlayState {

    private final List<OverlayEntry> entries = new ArrayList<OverlayEntry>();
    private Element element;

    void attach(Element e) {
        this.element = e;
    }

    List<OverlayEntry> entries() {
        return entries;
    }

    public void insert(OverlayEntry entry, OverlayEntry below, OverlayEntry above) {
        if (entry == null || entries.contains(entry)) {
            return;
        }
        entry.attach(this);
        int at = entries.size();
        if (below != null && entries.contains(below)) {
            at = entries.indexOf(below);
        } else if (above != null && entries.contains(above)) {
            at = entries.indexOf(above) + 1;
        }
        entries.add(at, entry);
        rebuild();
    }

    public void insertAll(DartList<OverlayEntry> newEntries, OverlayEntry below, OverlayEntry above) {
        if (newEntries == null) {
            return;
        }
        for (OverlayEntry e : newEntries) {
            insert(e, below, above);
        }
    }

    /** Drops an entry that has been removed, and rebuilds without it. */
    void forget(OverlayEntry entry) {
        if (entries.remove(entry)) {
            rebuild();
        }
    }

    void rebuild() {
        if (element != null) {
            element.markNeedsBuild();
        }
    }
}
