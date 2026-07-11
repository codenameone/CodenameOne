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
 * Floating layered widget windows: the plain-exe desktop lowering of
 * com.codename1.surfaces (home-screen widgets + live activity pill) for the
 * native Windows port. Each pinned widget / live activity is a frameless
 * WS_POPUP window with WS_EX_LAYERED | WS_EX_TOPMOST | WS_EX_TOOLWINDOW |
 * WS_EX_NOACTIVATE:
 *
 *  - LAYERED + UpdateLayeredWindow with per-pixel alpha: the Java side pushes
 *    the SurfaceRasterizer's straight-alpha ARGB pixels, this file
 *    premultiplies them into a 32-bit top-down DIB and blits. Per-pixel alpha
 *    means the descriptor's rounded corners come for free (transparent pixels
 *    simply are not part of the window).
 *  - TOPMOST keeps the widget above normal windows (the desktop analog of a
 *    home-screen surface); TOOLWINDOW keeps it out of the taskbar/alt-tab;
 *    NOACTIVATE keeps focus with the user's foreground app.
 *
 * Threading (the same rules as every other window in this port):
 *  - Windows belong to the thread that created them, so creation, destruction,
 *    pixel upload, positioning and hit-rect updates are marshaled to the main
 *    pump thread by posting WM_CN1_WIDGET to cn1Win.hwnd with a heap-allocated
 *    op struct the handler frees (the cn1_windows_notify.c pattern; never a
 *    blocking SendMessage from the EDT).
 *  - Events travel the other way through a mutex-guarded string queue
 *    ("<handle>;click;<x>;<y>" / "<handle>;moved;<x>;<y>") that the EDT drains
 *    via widgetPollEvent, exactly like browserPollEvent's queue.
 *
 * Interaction: WM_NCHITTEST returns HTCAPTION outside the action hit-rects --
 * so dragging anywhere on the widget body moves the window, courtesy of the
 * default caption-drag handling -- and HTCLIENT inside a hit-rect, so a click
 * there reaches WM_LBUTTONUP and is queued for Java to resolve against the
 * rasterizer's action rectangles. WM_EXITSIZEMOVE reports the final position
 * so Java can persist it.
 *
 * Per-monitor DPI: widgetGetDpiScale exposes the window's current DPI scale
 * (GetDpiForWindow, resolved dynamically with a 96-DPI fallback for older
 * systems) so Java rasterizes dips at the right pixel size; WM_DPICHANGED
 * refreshes the cached scale and queues a "moved" event, prompting Java to
 * re-check the scale and re-render.
 *
 * Rendering is pure Win32/GDI (DIB + UpdateLayeredWindow); this file
 * deliberately never touches the shared Direct2D render target, so it cannot
 * poison an in-flight BeginDraw/EndDraw batch on the EDT.
 */

/* Include the C++ standard library and SDK headers BEFORE cn1_windows.h:
 * cn1_globals.h installs macros for the bytecode runtime that otherwise break
 * the STL headers. */
#ifdef _WIN32
#include <windows.h>
#include <windowsx.h>
#include <deque>
#include <string>
#include <vector>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <limits.h>
#endif

#include "cn1_windows.h"

#ifdef _WIN32

/* --------------------------------------------------------------- op codes */

enum {
    CN1_WIDGET_OP_CREATE = 1,
    CN1_WIDGET_OP_DESTROY = 2,
    CN1_WIDGET_OP_SETPIXELS = 3,
    CN1_WIDGET_OP_SETPOS = 4,
    CN1_WIDGET_OP_SETHITRECTS = 5,
    CN1_WIDGET_OP_FOCUSAPP = 6
};

/* widgetSetPosition x sentinel: center the window horizontally on the primary
 * work area (real coordinates can be legitimately negative on multi-monitor
 * setups, so a plain "x < 0" test would break position restore there). Must
 * match WindowsNative.WIDGET_POS_CENTER_H on the Java side. */
#define CN1_WIDGET_POS_CENTER_H INT_MIN

/* ------------------------------------------------------------------ state */

/* One floating widget window. Allocated by widgetCreate (EDT), freed by the
 * OP_DESTROY handler (pump thread). The hwnd is touched only on the pump
 * thread; the cached x/y/w/h/dpiScale and the hit-rects are shared between the
 * pump thread (writer) and the EDT (reader) under g_widgetLock. */
struct CN1Widget {
    HWND hwnd;
    int x;
    int y;
    int w;                       /* current pixel size (window == DIB size)   */
    int h;
    float dpiScale;              /* 96 dpi == 1.0                              */
    std::vector<RECT> hitRects;  /* action rectangles in client pixels        */

    CN1Widget() : hwnd(NULL), x(0), y(0), w(1), h(1), dpiScale(1.0f) {
    }
};

/* A marshaled operation. Posted as the WM_CN1_WIDGET wParam; the handler owns
 * and frees it (and its payloads). Allocated with calloc/free -- only plain
 * data crosses the post. */
typedef struct CN1WidgetOp {
    int op;
    CN1Widget* widget;
    int x;
    int y;
    int w;
    int h;
    uint32_t* pixels;   /* OP_SETPIXELS: owned, premultiplied BGRA, w*h       */
    int* rects;         /* OP_SETHITRECTS: owned, packed x,y,w,h per rect     */
    int rectCount;
} CN1WidgetOp;

/* The queue lock also guards every CN1Widget's shared fields. A global C++
 * constructor initializes it before main -- no lazy-init race. */
struct CN1WidgetLock {
    CRITICAL_SECTION cs;
    CN1WidgetLock() {
        InitializeCriticalSection(&cs);
    }
};
static CN1WidgetLock g_widgetLock;

/* Outbound event queue, drained by the EDT (widgetPollEvent). Declared after
 * g_widgetLock so its constructor runs second within this translation unit. */
static std::deque<std::string> g_widgetEvents;

static bool g_widgetClassRegistered = false;

/* ---------------------------------------------------------------- helpers */

static void cn1WidgetEnqueueEvent(CN1Widget* w, const char* kind, int x, int y) {
    char buf[96];
    _snprintf(buf, sizeof(buf), "%lld;%s;%d;%d", (long long) (intptr_t) w, kind, x, y);
    buf[sizeof(buf) - 1] = '\0';
    EnterCriticalSection(&g_widgetLock.cs);
    g_widgetEvents.push_back(std::string(buf));
    LeaveCriticalSection(&g_widgetLock.cs);
}

/* GetDpiForWindow shipped with Windows 10 1607; resolve it dynamically so the
 * exe still starts on older systems (which then report the 96-DPI fallback). */
typedef UINT (WINAPI* CN1GetDpiForWindowFn)(HWND);

static float cn1WidgetDpiScaleFor(HWND hwnd) {
    static CN1GetDpiForWindowFn fn = (CN1GetDpiForWindowFn) GetProcAddress(
            GetModuleHandleW(L"user32.dll"), "GetDpiForWindow");
    if (fn != NULL && hwnd != NULL) {
        UINT dpi = fn(hwnd);
        if (dpi > 0) {
            return dpi / 96.0f;
        }
    }
    /* Fallback: the (system) screen DPI, or plain 96. */
    HDC dc = GetDC(hwnd);
    if (dc != NULL) {
        int v = GetDeviceCaps(dc, LOGPIXELSX);
        ReleaseDC(hwnd, dc);
        if (v > 0) {
            return v / 96.0f;
        }
    }
    return 1.0f;
}

/* True when the client-space point lies inside one of the widget's action
 * hit-rects. Called from the widget's WndProc (pump thread). */
static bool cn1WidgetHitTest(CN1Widget* w, int cx, int cy) {
    bool hit = false;
    EnterCriticalSection(&g_widgetLock.cs);
    for (size_t i = 0; i < w->hitRects.size(); i++) {
        const RECT& r = w->hitRects[i];
        if (cx >= r.left && cx < r.right && cy >= r.top && cy < r.bottom) {
            hit = true;
            break;
        }
    }
    LeaveCriticalSection(&g_widgetLock.cs);
    return hit;
}

/* ------------------------------------------------------- widget WndProc */

static LRESULT CALLBACK cn1WidgetWndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    if (msg == WM_NCCREATE) {
        /* Wire the CN1Widget* handed through CreateWindowExW's lpParam. */
        CREATESTRUCTW* cs = (CREATESTRUCTW*) lParam;
        SetWindowLongPtrW(hwnd, GWLP_USERDATA, (LONG_PTR) cs->lpCreateParams);
        return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
    CN1Widget* w = (CN1Widget*) GetWindowLongPtrW(hwnd, GWLP_USERDATA);
    if (w == NULL) {
        return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
    switch (msg) {
        case WM_NCHITTEST: {
            /* HTCAPTION outside the hit-rects makes the whole widget body a
             * drag handle (default caption dragging); HTCLIENT inside a
             * hit-rect lets the click reach WM_LBUTTONUP below. */
            POINT pt;
            pt.x = GET_X_LPARAM(lParam);
            pt.y = GET_Y_LPARAM(lParam);
            ScreenToClient(hwnd, &pt);
            return cn1WidgetHitTest(w, pt.x, pt.y) ? HTCLIENT : HTCAPTION;
        }
        case WM_LBUTTONUP: {
            /* Only hit-rect areas are HTCLIENT, so this is a click on an
             * action region; Java maps the coordinates back to the action. */
            int cx = GET_X_LPARAM(lParam);
            int cy = GET_Y_LPARAM(lParam);
            if (cn1WidgetHitTest(w, cx, cy)) {
                cn1WidgetEnqueueEvent(w, "click", cx, cy);
            }
            return 0;
        }
        case WM_MOVE: {
            /* Borderless popup: the client origin equals the window origin. */
            EnterCriticalSection(&g_widgetLock.cs);
            w->x = (int) (short) LOWORD(lParam);
            w->y = (int) (short) HIWORD(lParam);
            LeaveCriticalSection(&g_widgetLock.cs);
            return 0;
        }
        case WM_EXITSIZEMOVE: {
            /* Drag finished: report the final position for persistence. */
            RECT rc;
            if (GetWindowRect(hwnd, &rc)) {
                EnterCriticalSection(&g_widgetLock.cs);
                w->x = rc.left;
                w->y = rc.top;
                LeaveCriticalSection(&g_widgetLock.cs);
                cn1WidgetEnqueueEvent(w, "moved", rc.left, rc.top);
            }
            return 0;
        }
        case WM_DPICHANGED: {
            /* Dragged onto a monitor with a different scale: refresh the
             * cached scale and nudge Java with a "moved" event -- the bridge
             * re-reads the scale and re-renders at the new pixel size (the
             * next UpdateLayeredWindow resizes the window to match). */
            UINT dpi = LOWORD(wParam);
            RECT rc;
            EnterCriticalSection(&g_widgetLock.cs);
            if (dpi > 0) {
                w->dpiScale = dpi / 96.0f;
            }
            LeaveCriticalSection(&g_widgetLock.cs);
            if (GetWindowRect(hwnd, &rc)) {
                cn1WidgetEnqueueEvent(w, "moved", rc.left, rc.top);
            }
            return 0;
        }
        case WM_MOUSEACTIVATE:
            /* Belt and braces next to WS_EX_NOACTIVATE: interacting with the
             * widget never steals focus from the foreground app. */
            return MA_NOACTIVATE;
        default:
            return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}

/* ------------------------------------------- pump-thread op implementations */

static void cn1WidgetHandleCreate(CN1Widget* w) {
    HINSTANCE hInstance = GetModuleHandleW(NULL);
    if (!g_widgetClassRegistered) {
        WNDCLASSEXW wc;
        ZeroMemory(&wc, sizeof(wc));
        wc.cbSize = sizeof(wc);
        wc.lpfnWndProc = cn1WidgetWndProc;
        wc.hInstance = hInstance;
        wc.hCursor = LoadCursorW(NULL, (LPCWSTR) IDC_ARROW);
        wc.lpszClassName = L"CodenameOneWidgetWindow";
        RegisterClassExW(&wc);
        g_widgetClassRegistered = true;
    }
    /* Default placement: top-right of the primary work area (mirrors the
     * JavaSE floating widgets); Java restores a persisted position right
     * after creation when it has one. */
    RECT wa;
    wa.left = 0;
    wa.top = 0;
    wa.right = GetSystemMetrics(SM_CXSCREEN);
    wa.bottom = GetSystemMetrics(SM_CYSCREEN);
    SystemParametersInfoW(SPI_GETWORKAREA, 0, &wa, 0);
    int x = wa.right - w->w - 40;
    int y = wa.top + 60;
    HWND hwnd = CreateWindowExW(
            WS_EX_LAYERED | WS_EX_TOPMOST | WS_EX_TOOLWINDOW | WS_EX_NOACTIVATE,
            L"CodenameOneWidgetWindow", L"", WS_POPUP,
            x, y, w->w, w->h, NULL, NULL, hInstance, w);
    if (hwnd == NULL) {
        cn1WindowsLog("widget: CreateWindowExW failed");
        return;
    }
    float scale = cn1WidgetDpiScaleFor(hwnd);
    EnterCriticalSection(&g_widgetLock.cs);
    w->hwnd = hwnd;
    w->x = x;
    w->y = y;
    w->dpiScale = scale;
    LeaveCriticalSection(&g_widgetLock.cs);
    /* A layered window shows nothing until its first UpdateLayeredWindow, so
     * showing it "empty" here never flashes. */
    ShowWindow(hwnd, SW_SHOWNOACTIVATE);
}

/* Blits premultiplied BGRA pixels into the layered window via a 32-bit
 * top-down DIB + UpdateLayeredWindow, resizing the window to the pixel block
 * (per-pixel alpha -> rounded corners for free). Pump thread. */
static void cn1WidgetHandleSetPixels(CN1Widget* w, uint32_t* premul, int pw, int ph) {
    if (w->hwnd == NULL || premul == NULL || pw <= 0 || ph <= 0) {
        return;
    }
    BITMAPINFO bmi;
    ZeroMemory(&bmi, sizeof(bmi));
    bmi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bmi.bmiHeader.biWidth = pw;
    bmi.bmiHeader.biHeight = -ph;   /* negative height -> top-down rows */
    bmi.bmiHeader.biPlanes = 1;
    bmi.bmiHeader.biBitCount = 32;
    bmi.bmiHeader.biCompression = BI_RGB;

    HDC screen = GetDC(NULL);
    HDC mem = CreateCompatibleDC(screen);
    void* bits = NULL;
    HBITMAP dib = CreateDIBSection(mem, &bmi, DIB_RGB_COLORS, &bits, NULL, 0);
    if (dib != NULL && bits != NULL) {
        /* The 0xAARRGGBB premultiplied ints match the DIB's little-endian
         * BGRA byte order exactly, so a straight copy suffices. */
        memcpy(bits, premul, (size_t) pw * (size_t) ph * 4);
        HGDIOBJ old = SelectObject(mem, dib);
        SIZE size;
        size.cx = pw;
        size.cy = ph;
        POINT src;
        src.x = 0;
        src.y = 0;
        BLENDFUNCTION blend;
        blend.BlendOp = AC_SRC_OVER;
        blend.BlendFlags = 0;
        blend.SourceConstantAlpha = 255;
        blend.AlphaFormat = AC_SRC_ALPHA;
        if (!UpdateLayeredWindow(w->hwnd, screen, NULL, &size, mem, &src, 0,
                &blend, ULW_ALPHA)) {
            cn1WindowsLog("widget: UpdateLayeredWindow failed");
        }
        SelectObject(mem, old);
    }
    if (dib != NULL) {
        DeleteObject(dib);
    }
    DeleteDC(mem);
    ReleaseDC(NULL, screen);

    EnterCriticalSection(&g_widgetLock.cs);
    w->w = pw;
    w->h = ph;
    LeaveCriticalSection(&g_widgetLock.cs);
}

static void cn1WidgetHandleSetPos(CN1Widget* w, int x, int y) {
    if (w->hwnd == NULL) {
        return;
    }
    if (x == CN1_WIDGET_POS_CENTER_H) {
        RECT wa;
        wa.left = 0;
        wa.top = 0;
        wa.right = GetSystemMetrics(SM_CXSCREEN);
        wa.bottom = GetSystemMetrics(SM_CYSCREEN);
        SystemParametersInfoW(SPI_GETWORKAREA, 0, &wa, 0);
        int width;
        EnterCriticalSection(&g_widgetLock.cs);
        width = w->w;
        LeaveCriticalSection(&g_widgetLock.cs);
        x = wa.left + ((wa.right - wa.left) - width) / 2;
    }
    SetWindowPos(w->hwnd, NULL, x, y, 0, 0, SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
    EnterCriticalSection(&g_widgetLock.cs);
    w->x = x;
    w->y = y;
    LeaveCriticalSection(&g_widgetLock.cs);
}

static void cn1WidgetHandleSetHitRects(CN1Widget* w, const int* rects, int rectCount) {
    EnterCriticalSection(&g_widgetLock.cs);
    w->hitRects.clear();
    for (int i = 0; i < rectCount; i++) {
        RECT r;
        r.left = rects[i * 4];
        r.top = rects[i * 4 + 1];
        r.right = r.left + rects[i * 4 + 2];
        r.bottom = r.top + rects[i * 4 + 3];
        w->hitRects.push_back(r);
    }
    LeaveCriticalSection(&g_widgetLock.cs);
}

static void cn1WidgetHandleDestroy(CN1Widget* w) {
    HWND hwnd = w->hwnd;
    if (hwnd != NULL) {
        /* Detach the struct first so late messages during destruction fall
         * through to DefWindowProc instead of touching freed memory. */
        SetWindowLongPtrW(hwnd, GWLP_USERDATA, 0);
        DestroyWindow(hwnd);
    }
    delete w;
}

static void cn1WidgetHandleFocusApp(void) {
    if (cn1Win.hwnd == NULL) {
        return;
    }
    if (IsIconic(cn1Win.hwnd)) {
        ShowWindow(cn1Win.hwnd, SW_RESTORE);
    }
    SetForegroundWindow(cn1Win.hwnd);
}

/* Forwarded from cn1WinWndProc (pump thread) for WM_CN1_WIDGET. Owns and frees
 * the posted op struct and its payloads. */
void cn1WinWidgetHandleMessage(WPARAM wParam) {
    CN1WidgetOp* op = (CN1WidgetOp*) wParam;
    if (op == NULL) {
        return;
    }
    switch (op->op) {
        case CN1_WIDGET_OP_CREATE:
            cn1WidgetHandleCreate(op->widget);
            break;
        case CN1_WIDGET_OP_SETPIXELS:
            cn1WidgetHandleSetPixels(op->widget, op->pixels, op->w, op->h);
            break;
        case CN1_WIDGET_OP_SETPOS:
            cn1WidgetHandleSetPos(op->widget, op->x, op->y);
            break;
        case CN1_WIDGET_OP_SETHITRECTS:
            cn1WidgetHandleSetHitRects(op->widget, op->rects, op->rectCount);
            break;
        case CN1_WIDGET_OP_DESTROY:
            cn1WidgetHandleDestroy(op->widget);
            break;
        case CN1_WIDGET_OP_FOCUSAPP:
            cn1WidgetHandleFocusApp();
            break;
        default:
            break;
    }
    if (op->pixels != NULL) {
        free(op->pixels);
    }
    if (op->rects != NULL) {
        free(op->rects);
    }
    free(op);
}

/* --------------------------------------------------------- Java bridge */

/* Posts an op to the pump thread; frees it (and payloads) on post failure so
 * nothing leaks when the window is gone. EDT side. */
static void cn1WidgetPostOp(CN1WidgetOp* op) {
    if (cn1Win.hwnd == NULL
            || !PostMessageW(cn1Win.hwnd, WM_CN1_WIDGET, (WPARAM) op, 0)) {
        if (op->op == CN1_WIDGET_OP_CREATE || op->op == CN1_WIDGET_OP_DESTROY) {
            delete op->widget;
        }
        if (op->pixels != NULL) {
            free(op->pixels);
        }
        if (op->rects != NULL) {
            free(op->rects);
        }
        free(op);
    }
}

/* Premultiplies one straight-alpha ARGB pixel (the CN1 getRGB layout) for
 * UpdateLayeredWindow's AC_SRC_ALPHA blend. Rounded, so a full ramp
 * round-trips visually cleanly. */
static uint32_t cn1WidgetPremultiply(uint32_t argb) {
    uint32_t a = (argb >> 24) & 0xff;
    if (a == 255) {
        return argb;
    }
    if (a == 0) {
        return 0;
    }
    uint32_t r = ((((argb >> 16) & 0xff) * a) + 127) / 255;
    uint32_t g = ((((argb >> 8) & 0xff) * a) + 127) / 255;
    uint32_t b = (((argb & 0xff) * a) + 127) / 255;
    return (a << 24) | (r << 16) | (g << 8) | b;
}

extern "C" {

JAVA_LONG com_codename1_impl_windows_WindowsNative_widgetCreate___int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_INT w, JAVA_INT h) {
    if (cn1Win.hwnd == NULL) {
        return 0; /* headless: no pump thread to own the window */
    }
    CN1Widget* widget = new CN1Widget();
    widget->w = w > 0 ? w : 1;
    widget->h = h > 0 ? h : 1;
    CN1WidgetOp* op = (CN1WidgetOp*) calloc(1, sizeof(CN1WidgetOp));
    if (op == NULL) {
        delete widget;
        return 0;
    }
    op->op = CN1_WIDGET_OP_CREATE;
    op->widget = widget;
    if (!PostMessageW(cn1Win.hwnd, WM_CN1_WIDGET, (WPARAM) op, 0)) {
        delete widget;
        free(op);
        return 0;
    }
    return (JAVA_LONG) (intptr_t) widget;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_widgetUpdatePixels___long_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT argb, JAVA_INT w, JAVA_INT h) {
    CN1Widget* widget = (CN1Widget*) (intptr_t) peer;
    if (widget == NULL || argb == JAVA_NULL || w <= 0 || h <= 0) {
        return;
    }
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) argb).data;
    int len = (*(JAVA_ARRAY) argb).length;
    if (len < w * h) {
        return;
    }
    /* Copy + premultiply here (EDT) so the posted payload no longer references
     * the Java array -- the GC may move/collect it before the pump thread runs. */
    uint32_t* premul = (uint32_t*) malloc((size_t) w * (size_t) h * 4);
    if (premul == NULL) {
        return;
    }
    for (int i = 0; i < w * h; i++) {
        premul[i] = cn1WidgetPremultiply((uint32_t) data[i]);
    }
    CN1WidgetOp* op = (CN1WidgetOp*) calloc(1, sizeof(CN1WidgetOp));
    if (op == NULL) {
        free(premul);
        return;
    }
    op->op = CN1_WIDGET_OP_SETPIXELS;
    op->widget = widget;
    op->pixels = premul;
    op->w = w;
    op->h = h;
    cn1WidgetPostOp(op);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_widgetSetPosition___long_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT x, JAVA_INT y) {
    CN1Widget* widget = (CN1Widget*) (intptr_t) peer;
    if (widget == NULL) {
        return;
    }
    CN1WidgetOp* op = (CN1WidgetOp*) calloc(1, sizeof(CN1WidgetOp));
    if (op == NULL) {
        return;
    }
    op->op = CN1_WIDGET_OP_SETPOS;
    op->widget = widget;
    op->x = x;
    op->y = y;
    cn1WidgetPostOp(op);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_widgetGetX___long_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Widget* widget = (CN1Widget*) (intptr_t) peer;
    if (widget == NULL) {
        return 0;
    }
    EnterCriticalSection(&g_widgetLock.cs);
    int x = widget->x;
    LeaveCriticalSection(&g_widgetLock.cs);
    return x;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_widgetGetY___long_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Widget* widget = (CN1Widget*) (intptr_t) peer;
    if (widget == NULL) {
        return 0;
    }
    EnterCriticalSection(&g_widgetLock.cs);
    int y = widget->y;
    LeaveCriticalSection(&g_widgetLock.cs);
    return y;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_widgetSetHitRects___long_int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT rects) {
    CN1Widget* widget = (CN1Widget*) (intptr_t) peer;
    if (widget == NULL) {
        return;
    }
    int* copy = NULL;
    int rectCount = 0;
    if (rects != JAVA_NULL) {
        JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) rects).data;
        int len = (*(JAVA_ARRAY) rects).length;
        rectCount = len / 4;
        if (rectCount > 0) {
            copy = (int*) malloc((size_t) rectCount * 4 * sizeof(int));
            if (copy == NULL) {
                return;
            }
            memcpy(copy, data, (size_t) rectCount * 4 * sizeof(int));
        }
    }
    CN1WidgetOp* op = (CN1WidgetOp*) calloc(1, sizeof(CN1WidgetOp));
    if (op == NULL) {
        if (copy != NULL) {
            free(copy);
        }
        return;
    }
    op->op = CN1_WIDGET_OP_SETHITRECTS;
    op->widget = widget;
    op->rects = copy;
    op->rectCount = rectCount;
    cn1WidgetPostOp(op);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_widgetDestroy___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Widget* widget = (CN1Widget*) (intptr_t) peer;
    if (widget == NULL) {
        return;
    }
    CN1WidgetOp* op = (CN1WidgetOp*) calloc(1, sizeof(CN1WidgetOp));
    if (op == NULL) {
        return;
    }
    op->op = CN1_WIDGET_OP_DESTROY;
    op->widget = widget;
    cn1WidgetPostOp(op);
}

/* Next queued widget event ("<handle>;click;<x>;<y>" / "<handle>;moved;<x>;<y>"),
 * or null when none. Drained by the EDT in WindowsImplementation.drainInput. */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_widgetPollEvent___R_java_lang_String(
        CODENAME_ONE_THREAD_STATE) {
    std::string ev;
    {
        EnterCriticalSection(&g_widgetLock.cs);
        if (g_widgetEvents.empty()) {
            LeaveCriticalSection(&g_widgetLock.cs);
            return JAVA_NULL;
        }
        ev = g_widgetEvents.front();
        g_widgetEvents.pop_front();
        LeaveCriticalSection(&g_widgetLock.cs);
    }
    return newStringFromCString(threadStateData, ev.c_str());
}

/* The widget window's current DPI scale (96 dpi == 1.0); peer 0 reports the
 * main window's scale (the best pre-creation estimate). */
JAVA_FLOAT com_codename1_impl_windows_WindowsNative_widgetGetDpiScale___long_R_float(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Widget* widget = (CN1Widget*) (intptr_t) peer;
    if (widget == NULL) {
        return cn1WidgetDpiScaleFor(cn1Win.hwnd);
    }
    EnterCriticalSection(&g_widgetLock.cs);
    float scale = widget->dpiScale;
    LeaveCriticalSection(&g_widgetLock.cs);
    return scale;
}

/* Brings the main app window to the foreground (widget click -> open app).
 * Marshaled to the pump thread like every other window operation. */
JAVA_VOID com_codename1_impl_windows_WindowsNative_widgetFocusApp__(
        CODENAME_ONE_THREAD_STATE) {
    if (cn1Win.hwnd == NULL) {
        return;
    }
    CN1WidgetOp* op = (CN1WidgetOp*) calloc(1, sizeof(CN1WidgetOp));
    if (op == NULL) {
        return;
    }
    op->op = CN1_WIDGET_OP_FOCUSAPP;
    if (!PostMessageW(cn1Win.hwnd, WM_CN1_WIDGET, (WPARAM) op, 0)) {
        free(op);
    }
}

} /* extern "C" */

#endif /* _WIN32 */
