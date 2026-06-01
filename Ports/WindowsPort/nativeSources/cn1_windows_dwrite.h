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
#ifndef CN1_WINDOWS_DWRITE_H
#define CN1_WINDOWS_DWRITE_H

/*
 * Plain-C facade over the C++-only DirectWrite API. Implemented in
 * cn1_windows_dwrite.cpp (the only translation unit that includes <dwrite.h>).
 * All COM objects are passed as opaque void* so this header is safe to include
 * from the C translation units of the port (cn1_windows_text.c). Sizes and
 * coordinates are in device-independent pixels (DIPs).
 */

#ifdef _WIN32

#include <wchar.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Creates an IDWriteTextFormat (opaque handle, NULL on failure). */
void* cn1dwCreateFormat(const wchar_t* family, float sizePx, int bold, int italic);

/* Vertical metrics, in DIPs, for a text format handle. */
float cn1dwFontHeight(void* format);
float cn1dwFontAscent(void* format);

/* Width in DIPs of a text run measured with the format handle. */
float cn1dwMeasure(void* format, const wchar_t* text, int len);

/* Draws a text run with its top-left at (x,y) using the given Direct2D render
 * target and solid-color brush (both opaque ID2D1* pointers). */
void cn1dwDrawText(void* renderTarget, void* format, void* brush, const wchar_t* text, int len, float x, float y);

#ifdef __cplusplus
}
#endif

#endif /* _WIN32 */
#endif /* CN1_WINDOWS_DWRITE_H */
