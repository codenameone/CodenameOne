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

package com.codename1.ui;

/// Immutable screen geometry for synchronous native keyboard queries. Construct on the EDT;
/// the native UI thread must never wait for the EDT, which can itself be waiting for drawing.
/// Coordinates are absolute screen pixels and document offsets are UTF-16 indices.
/// Input arrays and query results are copied so native readers never share mutable editor geometry.
public final class TextInputGeometry {
    /// Optional optimized geometry provider for large custom editors.
    public interface Provider {
        /// Called on the EDT when the port starts or updates text input. Return a non-null snapshot
        /// of the current visible insertion positions, selection rectangles and active caret.
        TextInputGeometry getGeometrySnapshot();
    }

    private final int[] carets;
    private final int[] selection;
    private final int[] fallback;
    private final int start;
    private final int end;
    private final int activeEnd;

    /// Each caret record is {UTF-16 offset, x, y, width, height}. Include visible insertion
    /// positions, in document order, including the endpoint after each visible run. Selection
    /// rectangles use {x, y, width, height} records. All arrays must be non-null; carets and
    /// selection may be empty. The fallback must be the active caret's {x, y, width, height}.
    /// The start and end offsets describe the captured selection and may be in either order.
    public TextInputGeometry(int[] carets, int start, int end, int[] selection, int[] fallback) {
        this.carets = copy(carets);
        this.selection = copy(selection);
        this.fallback = copy(fallback);
        this.start = Math.min(start, end);
        this.end = Math.max(start, end);
        this.activeEnd = end;
    }

    private static int[] copy(int[] value) {
        int[] out = new int[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }

    /// Captures a bounded visible range for clients that do not provide a specialized snapshot.
    /// Large editors can override TextInputGeometry.Provider to walk visual lines once.
    public static TextInputGeometry capture(TextInputClient client) {
        if (client instanceof Provider) {
            return ((Provider) client).getGeometrySnapshot();
        }
        TextInputState state = client.getEditingState();
        int caret = state.getSelectionEnd();
        int first = caret;
        int last = caret;
        if (client instanceof Component) {
            Component c = (Component) client;
            int x = c.getAbsoluteX() + c.getScrollX();
            int y = c.getAbsoluteY() + c.getScrollY();
            int[] offsets = new int[]{client.offsetAtPoint(x, y),
                client.offsetAtPoint(x + c.getWidth(), y),
                client.offsetAtPoint(x, y + c.getHeight()),
                client.offsetAtPoint(x + c.getWidth(), y + c.getHeight())};
            for (int offset : offsets) {
                first = Math.min(first, offset);
                last = Math.max(last, offset);
            }
        }
        first = Math.max(0, first - 1);
        last = Math.min(client.getTextLength(), last + 1);
        if (last - first > 4096) {
            first = Math.max(first, caret - 2048);
            last = Math.min(last, first + 4096);
        }
        int[] points = new int[(last - first + 1) * 5];
        int n = 0;
        for (int i = first; i <= last; i++) {
            int[] rect = client.rectForOffset(i);
            points[n++] = i;
            for (int j = 0; j < 4; j++) {
                points[n++] = rect[j];
            }
        }
        return new TextInputGeometry(points, state.getSelectionStart(), caret,
            client.selectionRects(state.getSelectionStart(), caret), client.getCaretRect());
    }

    /// Returns the last captured caret rectangle for an offset, or the active caret offscreen.
    public int[] rectForOffset(int offset) {
        for (int i = 0; i + 4 < carets.length; i += 5) {
            if (carets[i] == offset) {
                return new int[]{carets[i + 1], carets[i + 2], carets[i + 3], carets[i + 4]};
            }
        }
        return copy(fallback);
    }

    /// Hit-tests only immutable visible geometry. Vertical distance takes priority across lines.
    public int offsetAtPoint(int x, int y) {
        int best = activeEnd;
        long bestY = Long.MAX_VALUE;
        long bestX = Long.MAX_VALUE;
        for (int i = 0; i + 4 < carets.length; i += 5) {
            long dy = Math.max(0, Math.max((long) carets[i + 2] - y, (long) y - carets[i + 2] - Math.max(1, carets[i + 4]) + 1));
            long dx = Math.abs((long) x - carets[i + 1]);
            if (dy < bestY || dy == bestY && dx < bestX) {
                bestY = dy;
                bestX = dx;
                best = carets[i];
            }
        }
        return best;
    }

    /// Returns exact captured selection geometry for the active range, and visible character
    /// rectangles for other native queries without touching a live editor off the EDT.
    public int[] selectionRects(int a, int b) {
        int low = Math.min(a, b);
        int high = Math.max(a, b);
        if (low == high) {
            return new int[0];
        }
        if (low == start && high == end) {
            return copy(selection);
        }
        int[] out = new int[carets.length];
        int n = 0;
        for (int i = 0; i + 9 < carets.length; i += 5) {
            if (carets[i] < low || carets[i] >= high) {
                continue;
            }
            int x = carets[i + 1];
            int y = carets[i + 2];
            int h = carets[i + 4];
            int w = carets[i + 3];
            if (carets[i + 7] == y) {
                x = Math.min(x, carets[i + 6]);
                w = Math.max(w, Math.abs(carets[i + 6] - carets[i + 1]));
            }
            if (n >= 4 && out[n - 3] == y && out[n - 1] == h && out[n - 4] + out[n - 2] == x) {
                out[n - 2] += w;
            } else {
                out[n++] = x;
                out[n++] = y;
                out[n++] = w;
                out[n++] = h;
            }
        }
        int[] result = new int[n];
        System.arraycopy(out, 0, result, 0, n);
        return result;
    }
}
