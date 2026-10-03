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
package com.codename1.androidcompat.runtime;

import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/// Breaks text into lines for a width: hard breaks at newlines, soft breaks
/// at the last space that fits, and a character break for a word wider than
/// the line. Optional end ellipsis when the line count is capped.
public final class TextLayout {

    public final List<String> lines = new ArrayList<String>();
    public float maxLineWidth;

    private TextLayout() {
    }

    /// `width` <= 0 means unbounded (no soft wrapping).
    public static TextLayout layout(String text, Paint paint, int width, boolean singleLine, int maxLines,
                                    boolean ellipsizeEnd) {
        TextLayout l = new TextLayout();
        if (text == null) {
            text = "";
        }
        if (singleLine) {
            text = text.replace('\n', ' ');
        }
        int start = 0;
        int n = text.length();
        while (start <= n) {
            int nl = text.indexOf('\n', start);
            String para = nl < 0 ? text.substring(start) : text.substring(start, nl);
            if (singleLine || width <= 0) {
                l.add(para, paint);
            } else {
                wrap(l, para, paint, width);
            }
            if (nl < 0) {
                break;
            }
            start = nl + 1;
        }
        if (maxLines > 0 && l.lines.size() > maxLines) {
            List<String> kept = new ArrayList<String>(l.lines.subList(0, maxLines));
            l.lines.clear();
            l.lines.addAll(kept);
            if (ellipsizeEnd) {
                l.ellipsizeLast(paint, width);
            }
        } else if (ellipsizeEnd && singleLine && width > 0 && !l.lines.isEmpty()
                && paint.measureText(l.lines.get(0)) > width) {
            l.ellipsizeLast(paint, width);
        }
        l.maxLineWidth = 0;
        for (String s : l.lines) {
            l.maxLineWidth = Math.max(l.maxLineWidth, paint.measureText(s));
        }
        return l;
    }

    private void add(String s, Paint paint) {
        lines.add(s);
    }

    private void ellipsizeLast(Paint paint, int width) {
        int last = lines.size() - 1;
        String s = lines.get(last);
        String ell = "\u2026";
        if (width <= 0) {
            lines.set(last, s + ell);
            return;
        }
        int end = s.length();
        while (end > 0 && paint.measureText(s.substring(0, end) + ell) > width) {
            end--;
        }
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        lines.set(last, s.substring(0, end) + ell);
    }

    private static void wrap(TextLayout l, String para, Paint paint, int width) {
        if (para.length() == 0) {
            l.lines.add("");
            return;
        }
        int pos = 0;
        int len = para.length();
        while (pos < len) {
            int fit = fit(para, pos, paint, width);
            if (pos + fit >= len) {
                l.lines.add(para.substring(pos));
                return;
            }
            int brk = -1;
            for (int i = pos + fit; i > pos; i--) {
                char c = para.charAt(i);
                if (c == ' ' || c == '\t') {
                    brk = i;
                    break;
                }
                if (i < pos + fit && (c == '-' || c == '/')) {
                    brk = i + 1;
                    break;
                }
            }
            if (brk <= pos) {
                brk = pos + Math.max(1, fit);
                l.lines.add(para.substring(pos, brk));
                pos = brk;
            } else {
                l.lines.add(trimEnd(para.substring(pos, brk)));
                pos = brk;
                while (pos < len && para.charAt(pos) == ' ') {
                    pos++;
                }
            }
        }
    }

    private static String trimEnd(String s) {
        int e = s.length();
        while (e > 0 && s.charAt(e - 1) == ' ') {
            e--;
        }
        return s.substring(0, e);
    }

    /// How many characters from `pos` fit in `width`.
    private static int fit(String s, int pos, Paint paint, int width) {
        int lo = 0;
        int hi = s.length() - pos;
        if (paint.measureText(s, pos, s.length()) <= width) {
            return hi;
        }
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (paint.measureText(s, pos, pos + mid) <= width) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }
}
