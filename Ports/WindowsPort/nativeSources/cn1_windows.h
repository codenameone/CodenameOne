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
#ifndef CN1_WINDOWS_H
#define CN1_WINDOWS_H

/*
 * Shared internals for the native Codename One Windows (Win32) port. The whole
 * header is gated on _WIN32 so it is inert anywhere the port is not built. The
 * public surface that the Java side (com.codename1.impl.windows.WindowsNative)
 * binds to is the set of ParparVM-mangled bridge functions defined across the
 * windowing / graphics / input / services translation units; this header holds
 * the state and helpers those units share.
 *
 * COM is used in its C binding: COBJMACROS gives the ID2D1RenderTarget_Xxx(p, ..)
 * call macros and CINTERFACE selects the C vtable layout, so Direct2D /
 * DirectWrite / WIC are usable without a C++ compiler.
 */

#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN

/*
 * Both Direct2D (d2d1.h) and DirectWrite (dwrite.h) ship only a C++ binding, so
 * the COM rendering translation units of this port (window / graphics / image /
 * dwrite) are compiled as C++ and use real C++ COM method calls (the
 * COBJMACROS-style call sites resolve through cn1_windows_comc.h). The remaining
 * C translation units (text / io / net) never invoke a Direct2D method; they
 * still include this header, so CINTERFACE is defined for C only, which lets
 * d2d1.h parse as opaque (incomplete) C structs that are used purely as
 * pointers. <dwrite.h> is never pulled in here -- fonts are opaque void* and all
 * DirectWrite work lives in cn1_windows_dwrite.cpp behind a plain-C facade.
 */
#ifndef __cplusplus
#define CINTERFACE
#endif
#include <windows.h>
#include <d2d1.h>
#include <wincodec.h>
#include <stdint.h>

#ifdef __cplusplus
/* C++ method-call macros for the COBJMACROS-style call sites. */
#include "cn1_windows_comc.h"
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* The ParparVM runtime is C; keep its declarations C-linked under C++. */
#include "cn1_globals.h"

/* ------------------------------------------------------------------ events */

/*
 * Win32 runs its message pump on the thread that created the window, but the
 * Codename One EDT lives on a translated Java thread. Input is therefore handed
 * across a small lock-protected ring buffer: the window proc pushes encoded
 * events, the EDT drains them through the pollEvent bridge. Keeping the bridge
 * Java-driven (Java pulls; C never calls arbitrary Java back) avoids threading
 * the GC/thread-state machinery through the Win32 callback.
 */
typedef enum {
    CN1_EVENT_NONE = 0,
    CN1_EVENT_POINTER_PRESSED = 1,
    CN1_EVENT_POINTER_RELEASED = 2,
    CN1_EVENT_POINTER_DRAGGED = 3,
    CN1_EVENT_KEY_PRESSED = 4,
    CN1_EVENT_KEY_RELEASED = 5,
    CN1_EVENT_SIZE_CHANGED = 6,
    CN1_EVENT_CLOSE = 7
} CN1EventType;

typedef struct {
    JAVA_INT type;
    JAVA_INT x;
    JAVA_INT y;
    JAVA_INT keyCode;
} CN1Event;

#define CN1_EVENT_QUEUE_CAPACITY 1024

/* --------------------------------------------------------------- graphics */

/*
 * A graphics peer wraps a Direct2D render target plus the mutable Codename One
 * pen state (color/alpha/clip/font). The window target and every mutable image
 * target each own one. Brushes are created lazily and recoloured in place.
 */
typedef struct CN1Graphics {
    ID2D1RenderTarget* target;       /* window or bitmap render target       */
    ID2D1SolidColorBrush* brush;     /* reused, recoloured per draw          */
    JAVA_INT color;                  /* 0xRRGGBB                              */
    JAVA_INT alpha;                  /* 0..255                               */
    JAVA_INT clipX, clipY, clipW, clipH;
    JAVA_BOOLEAN clipIsRect;
    struct CN1Font* font;            /* current font, not owned              */
    JAVA_BOOLEAN inFrame;            /* between BeginDraw / EndDraw           */
    void* wicBitmap;                 /* IWICBitmap* for offscreen targets, else NULL */
} CN1Graphics;

typedef struct CN1Font {
    void* format;                    /* IDWriteTextFormat*, opaque to C       */
    float size;
    int face;
    int style;
    float ascent;                    /* cached metrics in DIPs               */
    float height;
    wchar_t* family;                 /* owned UTF-16 copy of the family name  */
} CN1Font;

typedef struct CN1Image {
    ID2D1Bitmap* bitmap;             /* GPU bitmap for the active target     */
    JAVA_INT width;
    JAVA_INT height;
    uint32_t* argb;                  /* optional CPU copy for getRGB / scale */
    CN1Graphics* mutableGraphics;    /* non-null for mutable images          */
} CN1Image;

/* --------------------------------------------------------------- context */

typedef struct {
    HWND hwnd;
    ID2D1Factory* d2dFactory;
    IWICImagingFactory* wicFactory;
    /* The DirectWrite factory is owned privately by cn1_windows_dwrite.cpp (its
     * header has no C binding), so it is intentionally not held here. */
    CN1Graphics* windowGraphics;
    CN1Font* defaultFont;
    JAVA_INT width;
    JAVA_INT height;
    float dpiScale;                  /* 96dpi == 1.0                         */

    CRITICAL_SECTION eventLock;
    HANDLE eventSignal;              /* auto-reset, set when an event arrives */
    CN1Event events[CN1_EVENT_QUEUE_CAPACITY];
    volatile LONG eventHead;
    volatile LONG eventTail;

    volatile LONG initialized;

    /* Headless screenshot mode: when enabled, initDisplay renders into an
     * offscreen WIC bitmap instead of an HWND, and once the form has painted
     * the bitmap is encoded to shotPath and the process exits. Used by CI to
     * capture a deterministic PNG of the rendered UI with no display. */
    volatile LONG headless;
    WCHAR* shotPath;
    JAVA_INT shotW;
    JAVA_INT shotH;

    /* Pending window resize. WM_SIZE (main thread) records the new size here and
     * the EDT applies the Direct2D Resize between its own frames -- resizing the
     * HWND render target from another thread while the EDT is mid-BeginDraw is
     * invalid and presents black, so all render-target ops stay on the EDT. */
    volatile LONG pendingResize;
    volatile LONG pendingW;
    volatile LONG pendingH;
} CN1WindowsContext;

extern CN1WindowsContext cn1Win;

/* ------------------------------------------------------------ internal API */

/* windowing (cn1_windows_window.c) */
void cn1WindowsLog(const char* message);
void cn1WinApplyPendingResize(void);
int  cn1WinCreateWindow(const char* utf8Title, int width, int height);
void cn1WinPushEvent(CN1EventType type, int x, int y, int keyCode);
int  cn1WinPollEvent(CN1Event* out);
LRESULT CALLBACK cn1WinWndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);

/* graphics (cn1_windows_graphics.c) */
CN1Graphics* cn1WinCreateGraphics(ID2D1RenderTarget* target);
void cn1WinBeginFrame(CN1Graphics* g);
void cn1WinEndFrame(CN1Graphics* g);
ID2D1SolidColorBrush* cn1WinBrush(CN1Graphics* g);
D2D1_COLOR_F cn1WinColorF(JAVA_INT rgb, JAVA_INT alpha);

/* text (cn1_windows_text.c) -- DirectWrite work delegated to the C++ layer */
CN1Font* cn1WinCreateFont(int face, int style, int size);

/* offscreen rendering (cn1_windows_screenshot.cpp) */
CN1Graphics* cn1WinCreateOffscreenGraphics(int width, int height);
JAVA_BOOLEAN cn1WinEncodeGraphicsToPng(CN1Graphics* g, const WCHAR* path);

/* images (cn1_windows_image.c) */
CN1Image* cn1WinDecodeImage(const BYTE* data, UINT32 len);
CN1Image* cn1WinImageFromArgb(const uint32_t* argb, int width, int height);
ID2D1Bitmap* cn1WinEnsureBitmap(CN1Image* img, ID2D1RenderTarget* target);

/* shared string helper: a freshly malloc'd UTF-16 copy of a Java String. */
WCHAR* cn1WinJavaStringToWide(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str, UINT32* outLen);

#ifdef __cplusplus
}
#endif

#endif /* _WIN32 */
#endif /* CN1_WINDOWS_H */
