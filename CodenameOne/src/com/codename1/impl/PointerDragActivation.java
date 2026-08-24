/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl;

/// The drag-activation filter's state for one surface: where the gesture started,
/// how many moves it has seen, and whether it has crossed the threshold into a real
/// drag. Without it a pixel of jitter after a press reads as a drag, which activates
/// drag and drop and moves a draggable component on what was meant as a click.
///
/// The application's main surface keeps this state in fields on
/// `CodenameOneImplementation`, exactly as it always has. Each native window owns one
/// of these instead. That is deliberate: the alternative is a fixed table of slots
/// indexed by window id, which caps how many windows can drag at once, needs a
/// claim/release protocol on every press, and leaks a slot whenever a window is
/// disposed with a press still down -- after which the filter silently stops
/// filtering. State that belongs to a window and dies with it has none of those
/// failure modes.
public final class PointerDragActivation {

    /// Whether the gesture has been recognized as a drag. Package private rather
    /// than behind accessors: the only reader is the implementation, which is in
    /// this package.
    boolean started;

    /// How many moves this gesture has produced.
    int counter;

    /// Where the gesture started.
    int x;

    /// Where the gesture started.
    int y;

    /// Starts the filter over, so the next move begins a fresh gesture. Called when a
    /// press starts one, when a release ends one, and when a window's input is
    /// cancelled with a press still down.
    public void reset() {
        started = false;
        counter = 0;
    }
}
