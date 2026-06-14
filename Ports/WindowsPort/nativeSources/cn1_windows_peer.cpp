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

/*
 * Generic native-peer placement for the Windows port.
 *
 * A Codename One PeerComponent wraps a native widget. When an app's
 * @NativeInterface returns a native peer (an HWND, boxed as a long), the core
 * builds a PeerComponent (WindowsGenericPeer) over it; this layer positions /
 * shows / hides that child HWND over the lightweight component's bounds, the same
 * job IOSImplementation does with a UIView frame. The HWND was created by the
 * app's own native code; we never create or destroy it -- we only reparent it
 * onto the host window (cn1Win.hwnd) and move/size/show it.
 *
 * The offscreen screenshot pipeline renders the Form via Direct2D into a WIC
 * bitmap, where a live child HWND does not appear, so PeerComponent can fall back
 * to a peer image: peerCaptureArgb PrintWindow's the HWND into a DIB and returns
 * the pixels for Java to build that image (mirroring how the WebView2 peer paints
 * from a cached capture). Win32 window manipulation is allowed cross-thread, so
 * these run directly on the EDT-facing native calls.
 */

#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <string.h>

#include "cn1_windows.h"

#ifndef PW_RENDERFULLCONTENT
#define PW_RENDERFULLCONTENT 0x00000002
#endif

extern "C" {

extern struct clazz class_array1__JAVA_INT;

static HWND cn1PeerHwnd(JAVA_LONG peer) {
    return (HWND) (intptr_t) peer;
}

/* Reparent the app's HWND onto the host window and place + show it. */
JAVA_VOID com_codename1_impl_windows_WindowsNative_peerInitialized___long_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    HWND hwnd = cn1PeerHwnd(peer);
    if (!hwnd || !IsWindow(hwnd)) return;
    SetParent(hwnd, cn1Win.hwnd);
    LONG_PTR style = GetWindowLongPtrW(hwnd, GWL_STYLE);
    style = (style | WS_CHILD) & ~(WS_POPUP | WS_OVERLAPPED);
    SetWindowLongPtrW(hwnd, GWL_STYLE, style);
    SetWindowPos(hwnd, HWND_TOP, x, y, w > 0 ? w : 1, h > 0 ? h : 1, SWP_SHOWWINDOW);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_peerSetBounds___long_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    HWND hwnd = cn1PeerHwnd(peer);
    if (!hwnd || !IsWindow(hwnd)) return;
    SetWindowPos(hwnd, HWND_TOP, x, y, w > 0 ? w : 1, h > 0 ? h : 1, SWP_NOACTIVATE);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_peerSetVisible___long_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_BOOLEAN visible) {
    HWND hwnd = cn1PeerHwnd(peer);
    if (!hwnd || !IsWindow(hwnd)) return;
    ShowWindow(hwnd, visible ? SW_SHOW : SW_HIDE);
}

/* Hide and detach the app's HWND -- it owns the widget's lifetime, not us. */
JAVA_VOID com_codename1_impl_windows_WindowsNative_peerDeinitialized___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    HWND hwnd = cn1PeerHwnd(peer);
    if (!hwnd || !IsWindow(hwnd)) return;
    ShowWindow(hwnd, SW_HIDE);
    SetParent(hwnd, NULL);
}

/* Preferred size = the HWND's current size; Java falls back to a default if 0. */
JAVA_VOID com_codename1_impl_windows_WindowsNative_peerCalcPreferredSize___long_int_int_int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT dispW, JAVA_INT dispH, JAVA_OBJECT out) {
    if (out == JAVA_NULL) return;
    JAVA_ARRAY_INT* o = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) out).data;
    int olen = (*(JAVA_ARRAY) out).length;
    int w = 0, h = 0;
    HWND hwnd = cn1PeerHwnd(peer);
    RECT rc;
    if (hwnd && IsWindow(hwnd) && GetWindowRect(hwnd, &rc)) {
        w = rc.right - rc.left;
        h = rc.bottom - rc.top;
    }
    if (olen > 0) o[0] = w;
    if (olen > 1) o[1] = h;
}

/* PrintWindow the HWND into a top-down 32-bit DIB and return CN1 ARGB pixels for
 * the offscreen/transition peer image; fills outDims[0]=w, [1]=h. NULL on failure. */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_peerCaptureArgb___long_int_1ARRAY_R_int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT outDims) {
    HWND hwnd = cn1PeerHwnd(peer);
    if (!hwnd || !IsWindow(hwnd)) return JAVA_NULL;
    RECT rc;
    if (!GetWindowRect(hwnd, &rc)) return JAVA_NULL;
    int w = rc.right - rc.left, h = rc.bottom - rc.top;
    if (w <= 0 || h <= 0) return JAVA_NULL;

    HDC screen = GetDC(NULL);
    HDC mem = CreateCompatibleDC(screen);
    BITMAPINFO bi;
    ZeroMemory(&bi, sizeof(bi));
    bi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bi.bmiHeader.biWidth = w;
    bi.bmiHeader.biHeight = -h;          /* top-down */
    bi.bmiHeader.biPlanes = 1;
    bi.bmiHeader.biBitCount = 32;
    bi.bmiHeader.biCompression = BI_RGB;
    void* bits = NULL;
    HBITMAP dib = CreateDIBSection(mem, &bi, DIB_RGB_COLORS, &bits, NULL, 0);
    HGDIOBJ old = SelectObject(mem, dib);

    JAVA_OBJECT result = JAVA_NULL;
    if (dib && bits && PrintWindow(hwnd, mem, PW_RENDERFULLCONTENT)) {
        result = allocArray(threadStateData, w * h, &class_array1__JAVA_INT, sizeof(JAVA_ARRAY_INT), 1);
        if (result != JAVA_NULL) {
            const BYTE* src = (const BYTE*) bits;   /* BGRA, top-down */
            JAVA_ARRAY_INT* dst = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) result).data;
            int n = w * h;
            for (int i = 0; i < n; i++) {
                BYTE b = src[i * 4 + 0], g = src[i * 4 + 1], r = src[i * 4 + 2];
                dst[i] = (JAVA_ARRAY_INT) (0xff000000u | (r << 16) | (g << 8) | b);
            }
            if (outDims != JAVA_NULL) {
                JAVA_ARRAY_INT* dims = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) outDims).data;
                int dlen = (*(JAVA_ARRAY) outDims).length;
                if (dlen > 0) dims[0] = w;
                if (dlen > 1) dims[1] = h;
            }
        }
    }

    SelectObject(mem, old);
    if (dib) DeleteObject(dib);
    DeleteDC(mem);
    ReleaseDC(NULL, screen);
    return result;
}

} /* extern "C" */

#endif /* _WIN32 */
