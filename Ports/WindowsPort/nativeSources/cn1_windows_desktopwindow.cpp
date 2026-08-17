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
 * Additional desktop windows for the Windows port.
 *
 * The application's main window stays exactly where it was, in cn1Win: its HWND,
 * its ID2D1HwndRenderTarget and its CN1Graphics are untouched by this file. A
 * Codename One Window instead takes a slot in the table below, with its own HWND,
 * its own render target and its own graphics, so the single-window path -- which
 * every existing app and every screenshot baseline exercises -- cannot change
 * behaviour.
 *
 * Window identity in the window procedure comes from GWLP_USERDATA, set in
 * WM_NCCREATE. That is O(1) and needs no lock, which matters because the
 * procedure runs on the pump thread while the EDT is drawing.
 *
 * Windows must be created on the thread that owns the message pump, so creation
 * and destruction marshal through WM_CN1_DESKTOPWINDOW using the same blocking
 * SendMessageW pattern the native edit control and the file dialog already use.
 * Everything else (move, size, show, title) is legal cross-thread and runs
 * directly.
 */

#include "cn1_windows.h"
#include <windowsx.h>
#include <stdlib.h>
#include <string.h>

/* --------------------------------------------------------------- table */

typedef struct {
    HWND hwnd;
    ID2D1HwndRenderTarget* target;
    CN1Graphics* graphics;
    JAVA_INT width;
    JAVA_INT height;
    volatile LONG pendingResize;
    volatile LONG pendingW;
    volatile LONG pendingH;
    int windowId;      /* framework assigned; 0 marks a free slot */
    int monitorIndex;
    int minimized;
    int inUse;
} CN1DesktopWindow;

static CN1DesktopWindow g_windows[CN1_MAX_DESKTOP_WINDOWS];
static int g_classRegistered = 0;

/* Op codes marshaled through WM_CN1_DESKTOPWINDOW. */
#define CN1_DW_OP_CREATE  1
#define CN1_DW_OP_DESTROY 2

typedef struct {
    int op;
    int slot;
    int windowId;
    const char* utf8Title;
    int x;
    int y;
    int width;
    int height;
    int decorated;
    int resizable;
    int ownerSlot;
    int positionSet;
    int result;
} CN1DesktopWindowOp;

static CN1DesktopWindow* slotAt(int slot) {
    if (slot < 0 || slot >= CN1_MAX_DESKTOP_WINDOWS) {
        return NULL;
    }
    if (!g_windows[slot].inUse) {
        return NULL;
    }
    return &g_windows[slot];
}

HWND cn1WinDesktopHwnd(int slot) {
    CN1DesktopWindow* w = slotAt(slot);
    return w == NULL ? NULL : w->hwnd;
}

int cn1WinDesktopSlotForHwnd(HWND hwnd) {
    int iter;
    for (iter = 0; iter < CN1_MAX_DESKTOP_WINDOWS; iter++) {
        if (g_windows[iter].inUse && g_windows[iter].hwnd == hwnd) {
            return iter;
        }
    }
    return -1;
}

/* ------------------------------------------------------------- monitors */

typedef struct {
    RECT bounds[CN1_MAX_DESKTOP_WINDOWS];
    RECT work[CN1_MAX_DESKTOP_WINDOWS];
    HMONITOR handles[CN1_MAX_DESKTOP_WINDOWS];
    int primary;
    int count;
} CN1MonitorTable;

static CN1MonitorTable g_monitors;

static BOOL CALLBACK cn1WinMonitorEnum(HMONITOR mon, HDC hdc, LPRECT rect, LPARAM data) {
    MONITORINFO info;
    (void) hdc;
    (void) rect;
    (void) data;
    if (g_monitors.count >= CN1_MAX_DESKTOP_WINDOWS) {
        return FALSE;
    }
    ZeroMemory(&info, sizeof(info));
    info.cbSize = sizeof(info);
    if (GetMonitorInfoW(mon, &info)) {
        int i = g_monitors.count;
        g_monitors.handles[i] = mon;
        g_monitors.bounds[i] = info.rcMonitor;
        g_monitors.work[i] = info.rcWork;
        if (info.dwFlags & MONITORINFOF_PRIMARY) {
            g_monitors.primary = i;
        }
        g_monitors.count++;
    }
    return TRUE;
}

/* Re-reads the attached monitors. Cheap and called on demand rather than cached
 * across time, because a display can be unplugged or reconfigured at any moment
 * and a stale table would place windows off-screen. */
static void cn1WinRefreshMonitors(void) {
    g_monitors.count = 0;
    g_monitors.primary = 0;
    EnumDisplayMonitors(NULL, NULL, cn1WinMonitorEnum, 0);
    if (g_monitors.count == 0) {
        /* Degenerate but survivable: report the virtual screen as one monitor. */
        RECT r;
        r.left = 0;
        r.top = 0;
        r.right = GetSystemMetrics(SM_CXSCREEN);
        r.bottom = GetSystemMetrics(SM_CYSCREEN);
        g_monitors.bounds[0] = r;
        g_monitors.work[0] = r;
        g_monitors.handles[0] = NULL;
        g_monitors.count = 1;
    }
}

static int cn1WinMonitorIndexForHwnd(HWND hwnd) {
    HMONITOR mon = MonitorFromWindow(hwnd, MONITOR_DEFAULTTONEAREST);
    int iter;
    cn1WinRefreshMonitors();
    for (iter = 0; iter < g_monitors.count; iter++) {
        if (g_monitors.handles[iter] == mon) {
            return iter;
        }
    }
    return g_monitors.primary;
}

/*
 * Dots per inch of one monitor. GetDpiForMonitor lives in shcore.dll, which is
 * only present from Windows 8.1, so it is resolved dynamically and falls back to
 * the system DPI. Resolving it per call is fine -- these are not hot paths, and
 * the loader caches the module.
 */
typedef HRESULT (WINAPI *CN1GetDpiForMonitor)(HMONITOR, int, UINT*, UINT*);

static int cn1WinMonitorDpi(int monitor) {
    HMODULE shcore;
    if (monitor < 0 || monitor >= g_monitors.count) {
        return 96;
    }
    shcore = LoadLibraryW(L"shcore.dll");
    if (shcore != NULL) {
        CN1GetDpiForMonitor fn =
                (CN1GetDpiForMonitor) GetProcAddress(shcore, "GetDpiForMonitor");
        if (fn != NULL && g_monitors.handles[monitor] != NULL) {
            UINT dpiX = 96;
            UINT dpiY = 96;
            /* 0 == MDT_EFFECTIVE_DPI */
            if (SUCCEEDED(fn(g_monitors.handles[monitor], 0, &dpiX, &dpiY))) {
                FreeLibrary(shcore);
                return (int) dpiX;
            }
        }
        FreeLibrary(shcore);
    }
    {
        HDC screen = GetDC(NULL);
        int dpi = screen != NULL ? GetDeviceCaps(screen, LOGPIXELSX) : 96;
        if (screen != NULL) {
            ReleaseDC(NULL, screen);
        }
        return dpi;
    }
}

/* ------------------------------------------------------- window procedure */

static void cn1WinDesktopPushPointer(CN1DesktopWindow* w, CN1EventType type,
        LPARAM lParam, int mask) {
    cn1WinPushWindowEvent(w->windowId, type, GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam), mask);
}

static LRESULT CALLBACK cn1WinDesktopWndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    CN1DesktopWindow* w;
    if (msg == WM_NCCREATE) {
        CREATESTRUCTW* cs = (CREATESTRUCTW*) lParam;
        SetWindowLongPtrW(hwnd, GWLP_USERDATA, (LONG_PTR) cs->lpCreateParams);
        return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
    w = (CN1DesktopWindow*) (LONG_PTR) GetWindowLongPtrW(hwnd, GWLP_USERDATA);
    if (w == NULL || !w->inUse) {
        return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
    switch (msg) {
        case WM_LBUTTONDOWN:
            SetCapture(hwnd);
            cn1WinDesktopPushPointer(w, CN1_EVENT_POINTER_PRESSED, lParam, CN1_PE_MASK_PRIMARY);
            return 0;
        case WM_LBUTTONUP:
            ReleaseCapture();
            cn1WinDesktopPushPointer(w, CN1_EVENT_POINTER_RELEASED, lParam, CN1_PE_MASK_PRIMARY);
            return 0;
        case WM_RBUTTONDOWN:
            cn1WinDesktopPushPointer(w, CN1_EVENT_POINTER_PRESSED, lParam, CN1_PE_MASK_SECONDARY);
            return 0;
        case WM_RBUTTONUP:
            cn1WinDesktopPushPointer(w, CN1_EVENT_POINTER_RELEASED, lParam, CN1_PE_MASK_SECONDARY);
            return 0;
        case WM_MBUTTONDOWN:
            cn1WinDesktopPushPointer(w, CN1_EVENT_POINTER_PRESSED, lParam, CN1_PE_MASK_MIDDLE);
            return 0;
        case WM_MBUTTONUP:
            cn1WinDesktopPushPointer(w, CN1_EVENT_POINTER_RELEASED, lParam, CN1_PE_MASK_MIDDLE);
            return 0;
        case WM_MOUSEMOVE:
            if ((wParam & (MK_LBUTTON | MK_RBUTTON | MK_MBUTTON)) != 0) {
                int mask = 0;
                if (wParam & MK_LBUTTON) { mask |= CN1_PE_MASK_PRIMARY; }
                if (wParam & MK_RBUTTON) { mask |= CN1_PE_MASK_SECONDARY; }
                if (wParam & MK_MBUTTON) { mask |= CN1_PE_MASK_MIDDLE; }
                cn1WinDesktopPushPointer(w, CN1_EVENT_POINTER_DRAGGED, lParam, mask);
            }
            return 0;
        case WM_MOUSEWHEEL:
        case WM_MOUSEHWHEEL: {
            /* Same shape as the main window's handler: the wheel message reports the
             * cursor in SCREEN coordinates while the input ring works in client
             * coordinates, and the delta is a signed multiple of WHEEL_DELTA (120).
             * The windowId is what makes the EDT scroll this window's content rather
             * than the main form's. */
            POINT pt;
            pt.x = GET_X_LPARAM(lParam);
            pt.y = GET_Y_LPARAM(lParam);
            ScreenToClient(hwnd, &pt);
            cn1WinPushWindowEvent(w->windowId,
                    msg == WM_MOUSEHWHEEL ? CN1_EVENT_MOUSE_HWHEEL : CN1_EVENT_MOUSE_WHEEL,
                    pt.x, pt.y, GET_WHEEL_DELTA_WPARAM(wParam));
            return 0;
        }
        case WM_KEYDOWN:
        case WM_SYSKEYDOWN:
            cn1WinPushWindowEvent(w->windowId, CN1_EVENT_KEY_PRESSED, 0, 0, (int) wParam);
            return 0;
        case WM_KEYUP:
        case WM_SYSKEYUP:
            cn1WinPushWindowEvent(w->windowId, CN1_EVENT_KEY_RELEASED, 0, 0, (int) wParam);
            return 0;
        case WM_SIZE:
            /* A minimize arrives as a resize to zero. Reporting only that leaves the
             * framework thinking the window is still on screen: it keeps painting it
             * and an animation in it keeps the event dispatch thread awake. */
            if (wParam == SIZE_MINIMIZED) {
                if (!w->minimized) {
                    w->minimized = 1;
                    cn1WinPushWindowEvent(w->windowId, CN1_EVENT_WINDOW_HIDDEN, 0, 0, 0);
                }
                return 0;
            }
            if (wParam == SIZE_RESTORED && w->minimized) {
                w->minimized = 0;
                cn1WinPushWindowEvent(w->windowId, CN1_EVENT_WINDOW_SHOWN, 0, 0, 0);
            }
            w->width = LOWORD(lParam);
            w->height = HIWORD(lParam);
            /* The Direct2D Resize has to happen on the drawing thread between
             * frames -- resizing a render target while the EDT is mid-BeginDraw
             * is invalid and presents black -- so record it and let the EDT apply
             * it, exactly as the main window does. */
            w->pendingW = w->width;
            w->pendingH = w->height;
            w->pendingResize = 1;
            cn1WinPushWindowEvent(w->windowId, CN1_EVENT_SIZE_CHANGED, w->width, w->height, 0);
            return 0;
        case WM_MOVE: {
            int now;
            cn1WinPushWindowEvent(w->windowId, CN1_EVENT_WINDOW_MOVED, 0, 0, 0);
            now = cn1WinMonitorIndexForHwnd(hwnd);
            if (now != w->monitorIndex) {
                w->monitorIndex = now;
                cn1WinPushWindowEvent(w->windowId, CN1_EVENT_WINDOW_MONITOR, 0, 0, now);
            }
            return 0;
        }
        case WM_DPICHANGED:
            /* Windows hands us the rectangle the window should occupy at the new
             * scale; honouring it is what keeps a drag between mixed-DPI displays
             * from leaving the window the wrong physical size. */
            if (lParam != 0) {
                RECT* suggested = (RECT*) lParam;
                SetWindowPos(hwnd, NULL, suggested->left, suggested->top,
                        suggested->right - suggested->left,
                        suggested->bottom - suggested->top,
                        SWP_NOZORDER | SWP_NOACTIVATE);
            }
            w->monitorIndex = cn1WinMonitorIndexForHwnd(hwnd);
            cn1WinPushWindowEvent(w->windowId, CN1_EVENT_WINDOW_MONITOR, 0, 0, w->monitorIndex);
            return 0;
        case WM_ACTIVATE:
            cn1WinPushWindowEvent(w->windowId, CN1_EVENT_WINDOW_FOCUS, 0, 0,
                    LOWORD(wParam) == WA_INACTIVE ? 0 : 1);
            return 0;
        case WM_CLOSE:
            /* Never destroy here. Codename One decides, because an application
             * may veto the close from a listener. */
            cn1WinPushWindowEvent(w->windowId, CN1_EVENT_WINDOW_CLOSE, 0, 0, 0);
            return 0;
        case WM_DESTROY:
            /* Deliberately no PostQuitMessage: only the main window ends the
             * message loop. A secondary window closing must not exit the app. */
            return 0;
        default:
            return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}

/* ------------------------------------------------------- create / destroy */

static void cn1WinDesktopEnsureClass(void) {
    WNDCLASSEXW wc;
    if (g_classRegistered) {
        return;
    }
    ZeroMemory(&wc, sizeof(wc));
    wc.cbSize = sizeof(wc);
    wc.style = CS_HREDRAW | CS_VREDRAW;
    wc.lpfnWndProc = cn1WinDesktopWndProc;
    wc.hInstance = GetModuleHandleW(NULL);
    wc.hCursor = LoadCursorW(NULL, (LPCWSTR) IDC_ARROW);
    wc.lpszClassName = L"CodenameOneDesktopWindow";
    RegisterClassExW(&wc);
    g_classRegistered = 1;
}

static void cn1WinDesktopCreateOnPump(CN1DesktopWindowOp* op) {
    CN1DesktopWindow* w = &g_windows[op->slot];
    DWORD style;
    int titleLen;
    WCHAR* wTitle;
    D2D1_RENDER_TARGET_PROPERTIES rtProps;
    D2D1_HWND_RENDER_TARGET_PROPERTIES hwndProps;
    RECT rc;

    cn1WinDesktopEnsureClass();

    ZeroMemory(w, sizeof(*w));
    w->windowId = op->windowId;
    w->inUse = 1;

    style = op->decorated ? WS_OVERLAPPEDWINDOW : WS_POPUP;
    if (op->decorated && !op->resizable) {
        style &= ~(WS_THICKFRAME | WS_MAXIMIZEBOX);
    }

    titleLen = MultiByteToWideChar(CP_UTF8, 0, op->utf8Title, -1, NULL, 0);
    if (titleLen <= 0) {
        titleLen = 1;
    }
    wTitle = (WCHAR*) malloc((size_t) titleLen * sizeof(WCHAR));
    MultiByteToWideChar(CP_UTF8, 0, op->utf8Title, -1, wTitle, titleLen);

    /* WS_CLIPCHILDREN for the same reason the main window uses it: the Direct2D
     * present must not paint over native child controls overlaid on the form
     * (the WebView2 peer and the EDIT control used for native text editing). */
    /* An owned window stays above its owner and is minimized with it, which is
     * exactly what setOwnerWindow() promises; passing the owner HWND is the only way
     * Windows establishes that. Falling back to the main window keeps a window opened
     * from the main form on top of it, which is what a user expects of a tool window. */
    {
        /* ownerSlot: >= 0 another Codename One window, -2 the application's main
         * window, anything else unowned. An unowned window must not be silently
         * parented to the main one -- that would minimize it with the main window. */
        CN1DesktopWindow* owner = op->ownerSlot >= 0 ? slotAt(op->ownerSlot) : NULL;
        HWND ownerHwnd = owner != NULL ? owner->hwnd
                : (op->ownerSlot == -2 ? cn1Win.hwnd : NULL);
        w->hwnd = CreateWindowExW(0, L"CodenameOneDesktopWindow", wTitle,
                style | WS_CLIPCHILDREN,
                op->positionSet ? op->x : CW_USEDEFAULT,
                op->positionSet ? op->y : CW_USEDEFAULT,
                op->width, op->height,
                ownerHwnd, NULL, GetModuleHandleW(NULL), w);
    }
    free(wTitle);

    if (w->hwnd == NULL) {
        w->inUse = 0;
        op->result = 0;
        return;
    }

    GetClientRect(w->hwnd, &rc);
    w->width = rc.right - rc.left;
    w->height = rc.bottom - rc.top;

    ZeroMemory(&rtProps, sizeof(rtProps));
    rtProps.type = D2D1_RENDER_TARGET_TYPE_DEFAULT;
    rtProps.pixelFormat.format = DXGI_FORMAT_B8G8R8A8_UNORM;
    rtProps.pixelFormat.alphaMode = D2D1_ALPHA_MODE_PREMULTIPLIED;

    ZeroMemory(&hwndProps, sizeof(hwndProps));
    hwndProps.hwnd = w->hwnd;
    hwndProps.pixelSize.width = (UINT32) (w->width > 0 ? w->width : 1);
    hwndProps.pixelSize.height = (UINT32) (w->height > 0 ? w->height : 1);
    /* RETAIN_CONTENTS for the same reason as the main window: Codename One
     * repaints only the dirty region and relies on the rest being preserved. */
    hwndProps.presentOptions = D2D1_PRESENT_OPTIONS_RETAIN_CONTENTS;

    if (FAILED(ID2D1Factory_CreateHwndRenderTarget(cn1Win.d2dFactory, &rtProps,
            &hwndProps, &w->target))) {
        cn1WindowsLog("desktopWindow: failed to create HWND render target");
        DestroyWindow(w->hwnd);
        w->hwnd = NULL;
        w->inUse = 0;
        op->result = 0;
        return;
    }

    w->graphics = cn1WinCreateGraphics((ID2D1RenderTarget*) w->target);
    if (w->graphics != NULL) {
        /* Enables the #5273 flush-region clip clamp, exactly as for the main
         * window: a clip set while a component paints is confined to the region
         * about to be flushed so a fill cannot escape into the retained surface. */
        w->graphics->isWindowTarget = JAVA_TRUE;
    }
    w->monitorIndex = cn1WinMonitorIndexForHwnd(w->hwnd);
    op->result = 1;
}

static void cn1WinDesktopDestroyOnPump(CN1DesktopWindowOp* op) {
    CN1DesktopWindow* w = slotAt(op->slot);
    if (w == NULL) {
        return;
    }
    if (w->graphics != NULL) {
        /* cn1WinCreateGraphics mallocs the struct and does not own the target;
         * releasing the target below is what frees the Direct2D resources. */
        if (w->graphics->brush != NULL) {
            ID2D1SolidColorBrush_Release(w->graphics->brush);
        }
        free(w->graphics);
        w->graphics = NULL;
    }
    if (w->target != NULL) {
        ID2D1HwndRenderTarget_Release(w->target);
        w->target = NULL;
    }
    if (w->hwnd != NULL) {
        DestroyWindow(w->hwnd);
        w->hwnd = NULL;
    }
    w->inUse = 0;
    w->windowId = 0;
}

void cn1WinDesktopHandleMessage(WPARAM wParam, LPARAM lParam) {
    CN1DesktopWindowOp* op = (CN1DesktopWindowOp*) lParam;
    (void) wParam;
    if (op == NULL) {
        return;
    }
    if (op->op == CN1_DW_OP_CREATE) {
        cn1WinDesktopCreateOnPump(op);
    } else if (op->op == CN1_DW_OP_DESTROY) {
        cn1WinDesktopDestroyOnPump(op);
    }
}

/* Applies a pending resize on the drawing thread, mirroring the main window's
 * cn1WinApplyPendingResize. Called from the graphics layer before a frame. */
extern "C" void cn1WinDesktopApplyPendingResize(int slot) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w != NULL && w->pendingResize && w->target != NULL) {
        D2D1_SIZE_U size;
        size.width = (UINT32) (w->pendingW > 0 ? w->pendingW : 1);
        size.height = (UINT32) (w->pendingH > 0 ? w->pendingH : 1);
        ID2D1HwndRenderTarget_Resize(w->target, &size);
        w->pendingResize = 0;
    }
}

/* ------------------------------------------------------ WindowsNative bridge */

extern "C" {

JAVA_INT com_codename1_impl_windows_WindowsNative_desktopWindowCreate___int_java_lang_String_int_int_int_int_boolean_boolean_int_boolean_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT windowId, JAVA_OBJECT title,
        JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height,
        JAVA_BOOLEAN decorated, JAVA_BOOLEAN resizable, JAVA_INT ownerSlot,
        JAVA_BOOLEAN positionSet) {
    CN1DesktopWindowOp op;
    int slot = -1;
    int iter;
    for (iter = 0; iter < CN1_MAX_DESKTOP_WINDOWS; iter++) {
        if (!g_windows[iter].inUse) {
            slot = iter;
            break;
        }
    }
    if (slot < 0 || cn1Win.hwnd == NULL) {
        return -1;
    }
    ZeroMemory(&op, sizeof(op));
    op.op = CN1_DW_OP_CREATE;
    op.slot = slot;
    op.windowId = windowId;
    op.utf8Title = title == JAVA_NULL ? "" : stringToUTF8(threadStateData, title);
    op.x = x;
    op.y = y;
    op.width = width;
    op.height = height;
    op.decorated = decorated == JAVA_TRUE ? 1 : 0;
    op.resizable = resizable == JAVA_TRUE ? 1 : 0;
    op.ownerSlot = ownerSlot;
    op.positionSet = positionSet == JAVA_TRUE ? 1 : 0;
    /* Blocking send: the window must be created on the thread that owns the pump,
     * and the caller needs the slot back before it can use it. */
    SendMessageW(cn1Win.hwnd, WM_CN1_DESKTOPWINDOW, 0, (LPARAM) &op);
    return op.result ? slot : -1;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_desktopWindowDestroy___int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot) {
    CN1DesktopWindowOp op;
    if (cn1Win.hwnd == NULL) {
        return;
    }
    ZeroMemory(&op, sizeof(op));
    op.op = CN1_DW_OP_DESTROY;
    op.slot = slot;
    SendMessageW(cn1Win.hwnd, WM_CN1_DESKTOPWINDOW, 0, (LPARAM) &op);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_desktopWindowShow___int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_BOOLEAN visible) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w != NULL) {
        ShowWindow(w->hwnd, visible == JAVA_TRUE ? SW_SHOW : SW_HIDE);
        if (visible == JAVA_TRUE) {
            UpdateWindow(w->hwnd);
        }
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_desktopWindowSetTitle___int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_OBJECT title) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w != NULL && title != JAVA_NULL) {
        const char* utf8 = stringToUTF8(threadStateData, title);
        int len = MultiByteToWideChar(CP_UTF8, 0, utf8, -1, NULL, 0);
        if (len > 0) {
            WCHAR* wide = (WCHAR*) malloc((size_t) len * sizeof(WCHAR));
            MultiByteToWideChar(CP_UTF8, 0, utf8, -1, wide, len);
            SetWindowTextW(w->hwnd, wide);
            free(wide);
        }
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_desktopWindowSetBounds___int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_INT x, JAVA_INT y,
        JAVA_INT width, JAVA_INT height) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w != NULL) {
        SetWindowPos(w->hwnd, NULL, x, y, width, height, SWP_NOZORDER | SWP_NOACTIVATE);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_desktopWindowGetBounds___int_int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_OBJECT out) {
    CN1DesktopWindow* w = slotAt(slot);
    RECT r;
    JAVA_ARRAY_INT* data;
    if (w == NULL || out == JAVA_NULL) {
        return;
    }
    if (!GetWindowRect(w->hwnd, &r)) {
        return;
    }
    data = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) out).data;
    if ((*(JAVA_ARRAY) out).length >= 4) {
        data[0] = r.left;
        data[1] = r.top;
        data[2] = r.right - r.left;
        data[3] = r.bottom - r.top;
    }
}

JAVA_INT com_codename1_impl_windows_WindowsNative_desktopWindowGetWidth___int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot) {
    CN1DesktopWindow* w = slotAt(slot);
    return w == NULL ? 0 : w->width;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_desktopWindowGetHeight___int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot) {
    CN1DesktopWindow* w = slotAt(slot);
    return w == NULL ? 0 : w->height;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_desktopWindowGraphics___int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w == NULL) {
        return 0;
    }
    /* Apply any resize the pump recorded, on this (drawing) thread and between
     * frames -- the same contract the main window's begin-frame path follows. */
    cn1WinDesktopApplyPendingResize(slot);
    return (JAVA_LONG) (intptr_t) w->graphics;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_desktopWindowSetResizable___int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_BOOLEAN resizable) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w != NULL) {
        LONG_PTR style = GetWindowLongPtrW(w->hwnd, GWL_STYLE);
        if (resizable == JAVA_TRUE) {
            style |= (WS_THICKFRAME | WS_MAXIMIZEBOX);
        } else {
            style &= ~(WS_THICKFRAME | WS_MAXIMIZEBOX);
        }
        SetWindowLongPtrW(w->hwnd, GWL_STYLE, style);
        SetWindowPos(w->hwnd, NULL, 0, 0, 0, 0,
                SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_desktopWindowSetAlwaysOnTop___int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_BOOLEAN onTop) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w != NULL) {
        SetWindowPos(w->hwnd, onTop == JAVA_TRUE ? HWND_TOPMOST : HWND_NOTOPMOST,
                0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_desktopWindowSetUtility___int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_BOOLEAN utility) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w != NULL) {
        /* WS_EX_TOOLWINDOW is what keeps a palette out of the task bar and the
         * Alt-Tab switcher, and gives it the narrower title bar users expect. The
         * frame has to be recalculated for the change to show. */
        LONG_PTR ex = GetWindowLongPtrW(w->hwnd, GWL_EXSTYLE);
        if (utility == JAVA_TRUE) {
            ex |= WS_EX_TOOLWINDOW;
            ex &= ~WS_EX_APPWINDOW;
        } else {
            ex &= ~WS_EX_TOOLWINDOW;
        }
        SetWindowLongPtrW(w->hwnd, GWL_EXSTYLE, ex);
        SetWindowPos(w->hwnd, NULL, 0, 0, 0, 0,
                SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_desktopWindowSetEnabled___int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_BOOLEAN enabled) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w != NULL) {
        EnableWindow(w->hwnd, enabled == JAVA_TRUE ? TRUE : FALSE);
    }
}

/* Disables or re-enables the main window, which is how an application-modal
 * Codename One window gets the platform's own modal behaviour on top of the
 * framework's input blocking. */
JAVA_VOID com_codename1_impl_windows_WindowsNative_mainWindowSetEnabled___boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_BOOLEAN enabled) {
    if (cn1Win.hwnd != NULL) {
        EnableWindow(cn1Win.hwnd, enabled == JAVA_TRUE ? TRUE : FALSE);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_desktopWindowFocus___int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w != NULL) {
        SetForegroundWindow(w->hwnd);
        SetFocus(w->hwnd);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_desktopWindowSetState___int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_INT state) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w != NULL) {
        /* 0 restore, 1 minimize, 2 toggle maximize */
        if (state == 1) {
            ShowWindow(w->hwnd, SW_MINIMIZE);
        } else if (state == 2) {
            WINDOWPLACEMENT pl;
            ZeroMemory(&pl, sizeof(pl));
            pl.length = sizeof(pl);
            GetWindowPlacement(w->hwnd, &pl);
            ShowWindow(w->hwnd, pl.showCmd == SW_SHOWMAXIMIZED ? SW_RESTORE : SW_MAXIMIZE);
        } else {
            ShowWindow(w->hwnd, SW_RESTORE);
        }
    }
}

/* ---- monitors ---- */

JAVA_INT com_codename1_impl_windows_WindowsNative_monitorCount___R_int(
        CODENAME_ONE_THREAD_STATE) {
    cn1WinRefreshMonitors();
    return g_monitors.count;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_primaryMonitor___R_int(
        CODENAME_ONE_THREAD_STATE) {
    cn1WinRefreshMonitors();
    return g_monitors.primary;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_monitorBounds___int_boolean_int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_INT monitor, JAVA_BOOLEAN workArea, JAVA_OBJECT out) {
    JAVA_ARRAY_INT* data;
    RECT r;
    if (out == JAVA_NULL) {
        return;
    }
    cn1WinRefreshMonitors();
    if (monitor < 0 || monitor >= g_monitors.count) {
        monitor = g_monitors.primary;
    }
    r = workArea == JAVA_TRUE ? g_monitors.work[monitor] : g_monitors.bounds[monitor];
    data = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) out).data;
    if ((*(JAVA_ARRAY) out).length >= 4) {
        data[0] = r.left;
        data[1] = r.top;
        data[2] = r.right - r.left;
        data[3] = r.bottom - r.top;
    }
}

JAVA_INT com_codename1_impl_windows_WindowsNative_monitorDpi___int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT monitor) {
    cn1WinRefreshMonitors();
    return cn1WinMonitorDpi(monitor);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_monitorForWindow___int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot) {
    CN1DesktopWindow* w = slotAt(slot);
    if (w == NULL) {
        cn1WinRefreshMonitors();
        return g_monitors.primary;
    }
    return cn1WinMonitorIndexForHwnd(w->hwnd);
}

} /* extern "C" */
