/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.unitycompat.unityengine;

/// What to draw this frame, back to front.
///
/// The commands are recycled from one frame to the next: a scene of a
/// thousand sprites would otherwise make a thousand objects sixty times a
/// second, and the collector's pauses would show as stutter.
public final class DrawList {
    private DrawCommand[] commands = new DrawCommand[64];
    private DrawCommand[] spare = new DrawCommand[64];
    private int count;
    /// False when the scene has no camera; nothing is drawn then.
    public boolean hasCamera;
    /// ARGB the surface is cleared to.
    public int backgroundColor;

    DrawList() {
    }

    public int size() {
        return count;
    }

    public DrawCommand get(int index) {
        return commands[index];
    }

    void clear() {
        count = 0;
        hasCamera = false;
        backgroundColor = 0xff000000;
    }

    /// The next command to fill in. What it held last frame is still in
    /// it, so every field has to be set.
    DrawCommand next() {
        if (count == commands.length) {
            DrawCommand[] grown = new DrawCommand[count * 2];
            System.arraycopy(commands, 0, grown, 0, count);
            commands = grown;
        }
        DrawCommand c = commands[count];
        if (c == null) {
            c = new DrawCommand();
            commands[count] = c;
        }
        count++;
        return c;
    }

    /// Takes back the command [#next()] just gave, for something that
    /// turned out not to be visible.
    void drop() {
        count--;
    }

    private static boolean inOrder(DrawCommand a, DrawCommand b) {
        if (a.group != b.group) {
            return a.group < b.group;
        }
        if (a.sortingLayer != b.sortingLayer) {
            return a.sortingLayer < b.sortingLayer;
        }
        if (a.sortingOrder != b.sortingOrder) {
            return a.sortingOrder < b.sortingOrder;
        }
        return a.depth >= b.depth;
    }

    /// Orders the commands back to front, leaving equals as they were
    /// added. A merge sort: an insertion sort is quadratic when a scene's
    /// first object is drawn over everything added after it, which is the
    /// ordinary case of a player created before a field of stars.
    void sort() {
        boolean sorted = true;
        for (int i = 1; i < count && sorted; i++) {
            sorted = inOrder(commands[i - 1], commands[i]);
        }
        if (sorted) {
            return;
        }
        if (spare.length < commands.length) {
            spare = new DrawCommand[commands.length];
        }
        DrawCommand[] from = commands;
        DrawCommand[] to = spare;
        for (int width = 1; width < count; width *= 2) {
            for (int low = 0; low < count; low += width * 2) {
                int mid = low + width < count ? low + width : count;
                int high = low + width * 2 < count ? low + width * 2 : count;
                int a = low;
                int b = mid;
                for (int k = low; k < high; k++) {
                    if (a < mid && (b >= high || inOrder(from[a], from[b]))) {
                        to[k] = from[a++];
                    } else {
                        to[k] = from[b++];
                    }
                }
            }
            DrawCommand[] swap = from;
            from = to;
            to = swap;
        }
        // Both arrays hold every command, in different orders, and the
        // slots past the count have to keep theirs: carry those across
        // before the sorted one becomes the list.
        if (from != commands) { // NOPMD CompareObjectsWithEquals
            for (int i = count; i < commands.length; i++) {
                from[i] = commands[i];
            }
            spare = commands;
            commands = from;
        }
    }
}
