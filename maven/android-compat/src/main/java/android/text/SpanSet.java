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
package android.text;

import java.util.ArrayList;

/// The span bookkeeping the spannable classes share. Ranges move with edits
/// following each span's point/mark flags, as on Android.
final class SpanSet {

    static final class Entry {
        final Object what;
        int start;
        int end;
        final int flags;

        Entry(Object what, int start, int end, int flags) {
            this.what = what;
            this.start = start;
            this.end = end;
            this.flags = flags;
        }
    }

    final ArrayList<Entry> entries = new ArrayList<Entry>();

    SpanSet copy() {
        SpanSet s = new SpanSet();
        for (Entry e : entries) {
            s.entries.add(new Entry(e.what, e.start, e.end, e.flags));
        }
        return s;
    }

    /// The spans `[start, end)` of the text keeps, clipped to it and moved
    /// to begin at 0: what a subsequence carries over. A span that only
    /// touches the range is left behind, exactly as [#get] leaves it out.
    SpanSet slice(int start, int end) {
        SpanSet s = new SpanSet();
        for (Entry e : entries) {
            if (e.start > end || e.end < start) {
                continue;
            }
            if (start != end && e.start != e.end && (e.start == end || e.end == start)) {
                continue;
            }
            s.entries.add(new Entry(e.what, Math.max(e.start, start) - start,
                    Math.min(e.end, end) - start, e.flags));
        }
        return s;
    }

    void set(Object what, int start, int end, int flags) {
        remove(what);
        entries.add(new Entry(what, start, end, flags));
    }

    void remove(Object what) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).what == what) {
                entries.remove(i);
            }
        }
    }

    Entry find(Object what) {
        for (Entry e : entries) {
            if (e.what == what) {
                return e;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    <T> T[] get(int start, int end, Class<T> type) {
        ArrayList<Entry> out = new ArrayList<Entry>();
        for (Entry e : entries) {
            if (e.start > end || e.end < start) {
                continue;
            }
            if (start != end && e.start != e.end && (e.start == end || e.end == start)) {
                continue;
            }
            if (type == null || type == Object.class || type.isInstance(e.what)) {
                int index = out.size();
                int priority = e.flags & Spanned.SPAN_PRIORITY;
                while (index > 0 && (out.get(index - 1).flags & Spanned.SPAN_PRIORITY) < priority) {
                    index--;
                }
                out.add(index, e);
            }
        }
        // A typed array, so callers can assign it to Foo[]; Object[] would
        // fail that cast on the JVM.
        T[] arr = (T[]) java.lang.reflect.Array.newInstance(type == null ? Object.class : type, out.size());
        for (int i = 0; i < out.size(); i++) {
            arr[i] = (T) out.get(i).what;
        }
        return arr;
    }

    int nextTransition(int start, int limit, Class type) {
        int next = limit;
        for (Entry e : entries) {
            if (type != null && !type.isInstance(e.what)) {
                continue;
            }
            if (e.start > start && e.start < next) {
                next = e.start;
            }
            if (e.end > start && e.end < next) {
                next = e.end;
            }
        }
        return next;
    }

    /// Adjusts ranges after `[st, en)` was replaced by `len` characters.
    void replaced(int st, int en, int len) {
        int delta = len - (en - st);
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry e = entries.get(i);
            e.start = adjust(e.start, st, en, delta, (e.flags & 0xf0) == 0x20);
            e.end = adjust(e.end, st, en, delta, (e.flags & 0x0f) == 0x02);
            if (e.end < e.start) {
                e.end = e.start;
            }
            if (en > st && e.start == e.end
                    && (e.flags & 0xff) == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) {
                entries.remove(i);
            }
        }
    }

    private static int adjust(int p, int st, int en, int delta, boolean point) {
        if (p < st) {
            return p;
        }
        if (p > en) {
            return p + delta;
        }
        if (p == st && st == en) {
            return point ? p + delta : p;
        }
        if (p == en) {
            return p + delta;
        }
        return st;
    }
}
