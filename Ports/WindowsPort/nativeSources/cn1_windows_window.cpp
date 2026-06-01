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
 * Windowing anchor for the Codename One Windows (Win32) port. Owns the single
 * CN1WindowsContext, the Win32 window + its Direct2D HWND render target, the
 * DirectWrite / WIC factories, the cross-file string helper, and the input
 * event ring buffer.
 *
 * Threading model: the Codename One EDT (the translated Java main thread that
 * called init -> initDisplay) creates the window on itself and is the single
 * thread that pumps the Win32 queue (through pollEvent / waitForEvent) and
 * renders. Keeping one thread for both window messages and Direct2D drawing
 * matches Win32's "messages on the creating thread" rule and avoids any
 * cross-thread render-target synchronisation. The window proc only enqueues
 * encoded events; the EDT drains them via pollEvent.
 */

#ifdef _WIN32

#include "cn1_windows.h"
#include <windowsx.h>   /* GET_X_LPARAM / GET_Y_LPARAM */

/* This unit is C++ (Direct2D has no C binding), but the ParparVM bridge
 * functions and the shared helpers must keep C linkage so the translated C
 * runtime links to them; the whole body is therefore wrapped in extern "C".
 * stringToUTF8 and the other runtime helpers come from cn1_globals.h. */
extern "C" {

CN1WindowsContext cn1Win;

/* The HWND render target is kept as its concrete type here so WM_SIZE can call
 * Resize; everything else uses the base ID2D1RenderTarget via windowGraphics. */
static ID2D1HwndRenderTarget* g_hwndTarget;

/* --------------------------------------------------------------- logging */

void cn1WindowsLog(const char* message) {
    if (message == NULL) {
        return;
    }
    OutputDebugStringA(message);
    OutputDebugStringA("\n");
    fputs(message, stderr);
    fputc('\n', stderr);
    fflush(stderr);
}

/* Last-resort crash logger: prints the exception code + faulting address (and a
 * few raw return addresses) so a silent native crash leaves a breadcrumb. */
static LONG WINAPI cn1WinUnhandled(EXCEPTION_POINTERS* info) {
    char buf[256];
    sprintf(buf, "UNHANDLED EXCEPTION code=0x%08lX addr=%p base=%p",
            (unsigned long) info->ExceptionRecord->ExceptionCode,
            (void*) info->ExceptionRecord->ExceptionAddress,
            (void*) GetModuleHandleW(NULL));
    cn1WindowsLog(buf);
#ifdef _M_ARM64
    /* On ARM64 the crash context's Lr is the faulting function's caller return
     * address; walk the x29 frame-pointer chain for the frames above it. */
    CONTEXT* ctx = info->ContextRecord;
    sprintf(buf, "  pc=%p lr=%p fp=%p", (void*) ctx->Pc, (void*) ctx->Lr, (void*) ctx->Fp);
    cn1WindowsLog(buf);
    DWORD64 fp = ctx->Fp;
    for (int i = 0; i < 24 && fp != 0; i++) {
        DWORD64 ret = ((DWORD64*) fp)[1];
        DWORD64 next = ((DWORD64*) fp)[0];
        sprintf(buf, "  fpframe[%d]=%p", i, (void*) ret);
        cn1WindowsLog(buf);
        if (next <= fp) {
            break;
        }
        fp = next;
    }
#endif
    return EXCEPTION_EXECUTE_HANDLER;
}

/* ----------------------------------------------------------- string helper */

WCHAR* cn1WinJavaStringToWide(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str, UINT32* outLen) {
    if (str == JAVA_NULL) {
        if (outLen != NULL) {
            *outLen = 0;
        }
        WCHAR* empty = (WCHAR*) malloc(sizeof(WCHAR));
        if (empty != NULL) {
            empty[0] = 0;
        }
        return empty;
    }
    const char* utf8 = stringToUTF8(threadStateData, str);
    int wlen = MultiByteToWideChar(CP_UTF8, 0, utf8, -1, NULL, 0); /* includes NUL */
    if (wlen <= 0) {
        wlen = 1;
    }
    WCHAR* w = (WCHAR*) malloc((size_t) wlen * sizeof(WCHAR));
    if (w == NULL) {
        return NULL;
    }
    MultiByteToWideChar(CP_UTF8, 0, utf8, -1, w, wlen);
    if (outLen != NULL) {
        *outLen = (UINT32) (wlen - 1);
    }
    return w;
}

/* ------------------------------------------------------------ event queue */

void cn1WinPushEvent(CN1EventType type, int x, int y, int keyCode) {
    EnterCriticalSection(&cn1Win.eventLock);
    LONG next = (cn1Win.eventTail + 1) % CN1_EVENT_QUEUE_CAPACITY;
    if (next != cn1Win.eventHead) {
        CN1Event* e = &cn1Win.events[cn1Win.eventTail];
        e->type = (JAVA_INT) type;
        e->x = x;
        e->y = y;
        e->keyCode = keyCode;
        cn1Win.eventTail = next;
        SetEvent(cn1Win.eventSignal);
    }
    /* On overflow the oldest unread events are kept and the newest dropped;
     * the EDT drains continuously so this is only a backstop. */
    LeaveCriticalSection(&cn1Win.eventLock);
}

int cn1WinPollEvent(CN1Event* out) {
    int hasEvent = 0;
    EnterCriticalSection(&cn1Win.eventLock);
    if (cn1Win.eventHead != cn1Win.eventTail) {
        *out = cn1Win.events[cn1Win.eventHead];
        cn1Win.eventHead = (cn1Win.eventHead + 1) % CN1_EVENT_QUEUE_CAPACITY;
        hasEvent = 1;
    }
    LeaveCriticalSection(&cn1Win.eventLock);
    return hasEvent;
}

/* ------------------------------------------------------------- window proc */

LRESULT CALLBACK cn1WinWndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
        case WM_LBUTTONDOWN:
            SetCapture(hwnd);
            cn1WinPushEvent(CN1_EVENT_POINTER_PRESSED, GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam), 0);
            return 0;
        case WM_LBUTTONUP:
            ReleaseCapture();
            cn1WinPushEvent(CN1_EVENT_POINTER_RELEASED, GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam), 0);
            return 0;
        case WM_MOUSEMOVE:
            if ((wParam & MK_LBUTTON) != 0) {
                cn1WinPushEvent(CN1_EVENT_POINTER_DRAGGED, GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam), 0);
            }
            return 0;
        case WM_KEYDOWN:
            cn1WinPushEvent(CN1_EVENT_KEY_PRESSED, 0, 0, (int) wParam);
            return 0;
        case WM_KEYUP:
            cn1WinPushEvent(CN1_EVENT_KEY_RELEASED, 0, 0, (int) wParam);
            return 0;
        case WM_SIZE:
            cn1Win.width = LOWORD(lParam);
            cn1Win.height = HIWORD(lParam);
            if (g_hwndTarget != NULL) {
                D2D1_SIZE_U size;
                size.width = (UINT32) cn1Win.width;
                size.height = (UINT32) cn1Win.height;
                ID2D1HwndRenderTarget_Resize(g_hwndTarget, &size);
            }
            cn1WinPushEvent(CN1_EVENT_SIZE_CHANGED, cn1Win.width, cn1Win.height, 0);
            return 0;
        case WM_PAINT: {
            /* Codename One drives painting from its own loop; just validate the
             * update region so Windows stops re-posting WM_PAINT. */
            PAINTSTRUCT ps;
            BeginPaint(hwnd, &ps);
            EndPaint(hwnd, &ps);
            return 0;
        }
        case WM_CLOSE:
            cn1WinPushEvent(CN1_EVENT_CLOSE, 0, 0, 0);
            DestroyWindow(hwnd);
            return 0;
        case WM_DESTROY:
            PostQuitMessage(0);
            return 0;
        default:
            return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}

/* --------------------------------------------------------- window creation */

int cn1WinCreateWindow(const char* utf8Title, int width, int height) {
    HINSTANCE hInstance = GetModuleHandleW(NULL);

    WNDCLASSEXW wc;
    ZeroMemory(&wc, sizeof(wc));
    wc.cbSize = sizeof(wc);
    wc.style = CS_HREDRAW | CS_VREDRAW;
    wc.lpfnWndProc = cn1WinWndProc;
    wc.hInstance = hInstance;
    /* IDC_ARROW resolves to the ANSI MAKEINTRESOURCE without UNICODE defined;
     * cast to the wide resource id for LoadCursorW. */
    wc.hCursor = LoadCursorW(NULL, (LPCWSTR) IDC_ARROW);
    wc.lpszClassName = L"CodenameOneWindow";
    RegisterClassExW(&wc);

    int titleLen = MultiByteToWideChar(CP_UTF8, 0, utf8Title, -1, NULL, 0);
    if (titleLen <= 0) {
        titleLen = 1;
    }
    WCHAR* wTitle = (WCHAR*) malloc((size_t) titleLen * sizeof(WCHAR));
    MultiByteToWideChar(CP_UTF8, 0, utf8Title, -1, wTitle, titleLen);

    cn1Win.hwnd = CreateWindowExW(0, L"CodenameOneWindow", wTitle,
            WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, width, height,
            NULL, NULL, hInstance, NULL);
    free(wTitle);
    return cn1Win.hwnd != NULL ? 1 : 0;
}

/* ------------------------------------------------- WindowsNative bridge */

JAVA_VOID com_codename1_impl_windows_WindowsNative_nativeLog___java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    if (__cn1Arg1 == JAVA_NULL) {
        return;
    }
    cn1WindowsLog(stringToUTF8(threadStateData, __cn1Arg1));
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_initDisplay___java_lang_String_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    if (InterlockedCompareExchange(&cn1Win.initialized, 1, 0) != 0) {
        return; /* already initialised */
    }

    CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
    cn1WindowsLog("initDisplay: enter");
    SetUnhandledExceptionFilter(cn1WinUnhandled);
    InitializeCriticalSection(&cn1Win.eventLock);
    cn1Win.eventSignal = CreateEventW(NULL, FALSE, FALSE, NULL);
    cn1Win.eventHead = 0;
    cn1Win.eventTail = 0;
    cn1Win.dpiScale = 1.0f;

    const char* utf8Title = __cn1Arg1 != JAVA_NULL ? stringToUTF8(threadStateData, __cn1Arg1) : "Codename One";
    if (!cn1WinCreateWindow(utf8Title, __cn1Arg2, __cn1Arg3)) {
        cn1WindowsLog("initDisplay: failed to create window");
        return;
    }

    /* Direct2D factory + HWND render target sized to the client area. */
    /* In C++ the COM REFIID/REFCLSID parameters are references, so the GUID is
     * passed by value (no &). */
    // Multi-threaded: the window is created/pumped on the app's main thread but
    // the render target is drawn from the Codename One EDT, so D2D must guard
    // its own resources across threads.
    D2D1CreateFactory(D2D1_FACTORY_TYPE_MULTI_THREADED, IID_ID2D1Factory, NULL,
            (void**) &cn1Win.d2dFactory);

    RECT rc;
    GetClientRect(cn1Win.hwnd, &rc);
    cn1Win.width = rc.right - rc.left;
    cn1Win.height = rc.bottom - rc.top;

    D2D1_RENDER_TARGET_PROPERTIES rtProps;
    ZeroMemory(&rtProps, sizeof(rtProps));
    rtProps.type = D2D1_RENDER_TARGET_TYPE_DEFAULT;
    rtProps.pixelFormat.format = DXGI_FORMAT_B8G8R8A8_UNORM;
    rtProps.pixelFormat.alphaMode = D2D1_ALPHA_MODE_PREMULTIPLIED;

    D2D1_HWND_RENDER_TARGET_PROPERTIES hwndProps;
    ZeroMemory(&hwndProps, sizeof(hwndProps));
    hwndProps.hwnd = cn1Win.hwnd;
    hwndProps.pixelSize.width = (UINT32) cn1Win.width;
    hwndProps.pixelSize.height = (UINT32) cn1Win.height;
    hwndProps.presentOptions = D2D1_PRESENT_OPTIONS_NONE;

    if (FAILED(ID2D1Factory_CreateHwndRenderTarget(cn1Win.d2dFactory, &rtProps, &hwndProps, &g_hwndTarget))) {
        cn1WindowsLog("initDisplay: failed to create HWND render target");
        return;
    }
    cn1WindowsLog("initDisplay: render target created");
    cn1Win.windowGraphics = cn1WinCreateGraphics((ID2D1RenderTarget*) g_hwndTarget);

    /* WIC factory for the image layer. The DirectWrite factory is created lazily
     * inside the C++ text layer (cn1_windows_dwrite.cpp), not here. */
    CoCreateInstance(CLSID_WICImagingFactory, NULL, CLSCTX_INPROC_SERVER,
            IID_IWICImagingFactory, (void**) &cn1Win.wicFactory);

    ShowWindow(cn1Win.hwnd, SW_SHOW);
    UpdateWindow(cn1Win.hwnd);
    cn1WindowsLog("initDisplay: window shown");
}

JAVA_INT com_codename1_impl_windows_WindowsNative_getDisplayWidth___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1Win.width;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_getDisplayHeight___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1Win.height;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_getWindowGraphics___R_long(CODENAME_ONE_THREAD_STATE) {
    return (JAVA_LONG) (intptr_t) cn1Win.windowGraphics;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_flushGraphics___long_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3,
        JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5) {
    /* The dirty rectangle is advisory; the HWND render target presents the whole
     * surface. EndDraw flushes the batched Direct2D commands and presents. */
    CN1Graphics* g = (CN1Graphics*) (intptr_t) __cn1Arg1;
    if (g != NULL) {
        cn1WinEndFrame(g);
    }
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_pollEvent___int_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    /* The message loop runs on the main thread (runMessageLoop); the EDT only
     * dequeues here -- it must not PeekMessage on its own (empty) queue. */
    CN1Event ev;
    if (!cn1WinPollEvent(&ev)) {
        return JAVA_FALSE;
    }
    JAVA_ARRAY_INT* out = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) __cn1Arg1).data;
    int len = (*(JAVA_ARRAY) __cn1Arg1).length;
    if (len > 0) { out[0] = ev.type; }
    if (len > 1) { out[1] = ev.x; }
    if (len > 2) { out[2] = ev.y; }
    if (len > 3) { out[3] = ev.keyCode; }
    return JAVA_TRUE;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_waitForEvent___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    /* EDT idle: block until the window proc (on the main thread) signals that an
     * event was queued, or the timeout elapses. */
    if (cn1Win.eventSignal != NULL) {
        WaitForSingleObject(cn1Win.eventSignal, (DWORD) __cn1Arg1);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_runMessageLoop__(CODENAME_ONE_THREAD_STATE) {
    /* Runs on the app's main thread (the window's owner) after Display.init.
     * Pumps Win32 messages so the window is responsive; the window proc enqueues
     * input that the EDT drains via pollEvent. Returns when the window closes. */
    cn1WindowsLog("runMessageLoop: enter");
    MSG msg;
    while (GetMessageW(&msg, NULL, 0, 0) > 0) {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }
    cn1WindowsLog("runMessageLoop: exit");
}

} /* extern "C" */

#endif /* _WIN32 */
