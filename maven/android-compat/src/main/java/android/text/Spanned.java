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

/// Text with markup objects attached to ranges.
public interface Spanned extends CharSequence {
    int SPAN_POINT_MARK_MASK = 0x33;
    int SPAN_MARK_MARK = 0x11;
    int SPAN_MARK_POINT = 0x12;
    int SPAN_POINT_MARK = 0x21;
    int SPAN_POINT_POINT = 0x22;
    int SPAN_PARAGRAPH = 0x33;
    int SPAN_INCLUSIVE_EXCLUSIVE = SPAN_MARK_MARK;
    int SPAN_INCLUSIVE_INCLUSIVE = SPAN_MARK_POINT;
    int SPAN_EXCLUSIVE_EXCLUSIVE = SPAN_POINT_MARK;
    int SPAN_EXCLUSIVE_INCLUSIVE = SPAN_POINT_POINT;
    int SPAN_COMPOSING = 0x100;
    int SPAN_INTERMEDIATE = 0x200;
    int SPAN_USER_SHIFT = 24;
    int SPAN_USER = 0xFFFFFFFF << SPAN_USER_SHIFT;
    int SPAN_PRIORITY_SHIFT = 16;
    int SPAN_PRIORITY = 0xFF << SPAN_PRIORITY_SHIFT;

    <T> T[] getSpans(int start, int end, Class<T> type);

    int getSpanStart(Object tag);

    int getSpanEnd(Object tag);

    int getSpanFlags(Object tag);

    int nextSpanTransition(int start, int limit, Class type);
}
