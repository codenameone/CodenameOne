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
    CN1_EVENT_CLOSE = 7,
    /* Mouse wheel: x/y are the cursor's client coordinates, keyCode carries the
     * signed wheel delta in WHEEL_DELTA (120) units. The EDT turns it into a
     * synthetic scroll gesture (see WindowsImplementation.drainInput). */
    CN1_EVENT_MOUSE_WHEEL = 8,
    CN1_EVENT_MOUSE_HWHEEL = 9,
    /* Trackpad pinch / rotate: x/y are the gesture's client coordinates and
     * keyCode is the incremental value in 1/10000 units -- an incremental scale
     * multiplier for PINCH (10000 == scale 1.0) and incremental radians for
     * ROTATE. drainInput decodes these into Display.fireMagnifyGesture /
     * fireRotationGesture, the same hooks the macOS trackpad drives. */
    CN1_EVENT_PINCH = 10,
    CN1_EVENT_ROTATE = 11
} CN1EventType;

/* Fixed-point scale for the gesture keyCode field (see CN1_EVENT_PINCH). */
#define CN1_GESTURE_FIXED 10000

typedef struct {
    JAVA_INT type;
    JAVA_INT x;
    JAVA_INT y;
    JAVA_INT keyCode;
} CN1Event;

/* For pointer (pressed/released/dragged) events the otherwise-unused keyCode
 * field carries the pointer metadata: the low bits are a button bitmask that
 * mirrors com.codename1.ui.events.PointerEvent.MASK_* (so a press/release carry
 * the button that changed and a drag carries the buttons held down), and the
 * high bits flag a touch / pen digitizer so the Java side reports the right
 * PointerEvent type. A value of 0 means "no detail" and defaults to a primary
 * mouse press. WindowsImplementation.drainInput decodes this. */
#define CN1_PE_MASK_PRIMARY   1
#define CN1_PE_MASK_SECONDARY 2
#define CN1_PE_MASK_MIDDLE    4
#define CN1_PE_MASK_BACK      8
#define CN1_PE_MASK_FORWARD   16
#define CN1_PE_TOUCH_FLAG     256
#define CN1_PE_PEN_FLAG       512

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
    void* clipGeom;                  /* ID2D1PathGeometry* when clip is a shape, else NULL */
    void* clipLayer;                 /* ID2D1Layer* pushed for a shape clip, else NULL     */
    D2D1_MATRIX_3X2_F clipMaskTransform; /* world transform captured at setClipShape time;
                                      * applied to the (raw) clip geometry as the layer
                                      * maskTransform so a shape clip lands exactly where
                                      * drawShape draws the same path -- independent of the
                                      * transform active when each clipped primitive draws */
    struct CN1Font* font;            /* current font, not owned              */
    JAVA_BOOLEAN inFrame;            /* between BeginDraw / EndDraw           */
    void* wicBitmap;                 /* IWICBitmap* for offscreen targets, else NULL */
    D2D1_MATRIX_3X2_F transform;     /* current affine; re-applied each BeginDraw so
                                      * a transform set before the first primitive
                                      * (the mutable-image path) isn't reset to identity */
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
    ID2D1Bitmap* bitmap;             /* GPU bitmap, valid only on bitmapTarget */
    void* bitmapTarget;              /* render target ->bitmap was created on  */
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

    /* Bumped on every flushGraphics (i.e. each completed form paint / present).
     * The headless capture watches this so it snapshots only once painting has
     * settled, instead of after a fixed sleep that a slow first paint can
     * outlast -- the draw-arc / draw-image-rect forms paint a heavy first cell
     * and the flat settle used to grab the frame before the rest had rendered. */
    volatile LONG flushGen;

    /* Headless screenshot mode: when enabled, initDisplay renders into an
     * offscreen WIC bitmap instead of an HWND, and once the form has painted
     * the bitmap is encoded to shotPath and the process exits. Used by CI to
     * capture a deterministic PNG of the rendered UI with no display. */
    volatile LONG headless;
    WCHAR* shotPath;
    JAVA_INT shotW;
    JAVA_INT shotH;

    /* Offscreen-capture mode (the cn1ss WebSocket screenshot suite): a real
     * (hidden) window is still created so the message pump, DPI and exact client
     * size are identical to a normal run, but windowGraphics is pointed at an
     * offscreen WIC bitmap of that same client size instead of the HWND target.
     * That makes captureWindowToPngBytes read back real frames (the proven WIC
     * path) instead of falling back to a fresh per-screenshot mutable-image
     * repaint -- which is the expensive step that stalled the slow windows-11-arm
     * runner mid-suite. Unlike `headless` there is no single-shot auto-exit. */
    volatile LONG offscreenCapture;

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

/* BrowserComponent / WebView2 peer (cn1_windows_browser.cpp). The EDT-facing
 * native methods marshal each WebView2 operation to the main (pump) thread by
 * posting WM_CN1_BROWSER to cn1Win.hwnd; cn1WinWndProc forwards it here. */
#define WM_CN1_BROWSER (WM_APP + 17)
void cn1WinBrowserHandleMessage(WPARAM wParam, LPARAM lParam);

/* Native in-place text editing (cn1_windows_edit.c). The EDIT control must be
 * created/destroyed on the pump thread that owns the window; the EDT-facing
 * editStringAt/editClose post WM_CN1_EDIT to cn1Win.hwnd and cn1WinWndProc
 * forwards it here. */
#define WM_CN1_EDIT (WM_APP + 18)
void cn1WinEditHandleMessage(WPARAM wParam, LPARAM lParam);
/* WM_CTLCOLOREDIT hook: colours the native edit control to match the CN1 field. */
HBRUSH cn1WinEditCtlColor(HDC hdc, HWND control);

/* Native file open/save dialog (cn1_windows_io.c). The common dialog is modal
 * and must run on the thread that owns the window, so the EDT-facing fileDialog
 * sends (not posts) WM_CN1_FILEDIALOG to cn1Win.hwnd -- a blocking SendMessage
 * that returns once the user has chosen -- and cn1WinWndProc forwards it here. */
#define WM_CN1_FILEDIALOG (WM_APP + 19)
LRESULT cn1WinFileDialogHandleMessage(WPARAM wParam);

/* Local notifications via Shell_NotifyIcon (cn1_windows_notify.c). A Timer on a
 * worker thread posts WM_CN1_NOTIFY to the pump thread (which owns the tray icon)
 * to display a balloon; cn1WinWndProc forwards both that and the tray's own
 * callback message (WM_CN1_TRAY, sent when the user clicks the balloon) here. */
#define WM_CN1_NOTIFY (WM_APP + 20)
#define WM_CN1_TRAY   (WM_APP + 21)
void cn1WinNotifyHandleMessage(WPARAM wParam);
void cn1WinTrayHandleMessage(WPARAM wParam, LPARAM lParam);

/* System share via WinRT DataTransferManager (cn1_windows_winrt.cpp). ShowShareUI
 * must run on the window-owning thread, so the EDT-facing shareText sends
 * WM_CN1_SHARE to cn1Win.hwnd and cn1WinWndProc forwards it here. No-op stub when
 * the port is built without WinRT. */
#define WM_CN1_SHARE (WM_APP + 22)
void cn1WinShareHandleMessage(WPARAM wParam);

/* System print dialog (cn1_windows_print.cpp). PrintDlgW is modal and must run
 * on the window-owning pump thread, so the printing worker thread sends (not
 * posts) WM_CN1_PRINTDLG to cn1Win.hwnd -- a blocking SendMessage that returns
 * once the user has chosen a printer -- and cn1WinWndProc forwards it here. The
 * document is then rasterized and spooled on the worker thread. */
#define WM_CN1_PRINTDLG (WM_APP + 23)
LRESULT cn1WinPrintDialogHandleMessage(WPARAM wParam);

/* graphics (cn1_windows_graphics.c) */
CN1Graphics* cn1WinCreateGraphics(ID2D1RenderTarget* target);
void cn1WinBeginFrame(CN1Graphics* g);
void cn1WinEndFrame(CN1Graphics* g);
ID2D1SolidColorBrush* cn1WinBrush(CN1Graphics* g);
D2D1_COLOR_F cn1WinColorF(JAVA_INT rgb, JAVA_INT alpha);
/* Push/pop g's current clip (axis-aligned rect, or a layer for a shape clip)
 * around a single drawing operation. Exposed so the DirectWrite text path
 * (cn1_windows_text.c) clips glyphs exactly like the graphics primitives do --
 * without it, text drawn past the dirty region (e.g. overscrolled list rows)
 * bleeds into the retained surface margins and smears. */
void cn1WinPushClip(CN1Graphics* g);
void cn1WinPopClip(CN1Graphics* g);

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
