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
 * Threading model: the app's main thread calls Display.init (which runs
 * initDisplay here, creating the window on the main thread) and then owns the
 * Win32 message pump via pumpMessages. Win32 delivers a window's messages to the
 * thread that created it, so the pump must live on the main thread. The window
 * proc only enqueues encoded events into a small ring buffer; the main thread
 * then drains that ring (WindowsImplementation.drainInput) into Codename One,
 * which wakes the EDT. The EDT is a separate thread (spawned by Display.init)
 * that consumes the Codename One event queue, lays out, and renders with
 * Direct2D. This producer (main) / consumer (EDT) split mirrors how every
 * desktop Codename One port feeds input from the native UI thread to the EDT.
 */

#ifdef _WIN32

#include "cn1_windows.h"
#include <windowsx.h>   /* GET_X_LPARAM / GET_Y_LPARAM */
#include <stdio.h>
#include <string.h>
#include <malloc.h>     /* _resetstkoflw (stack-overflow guard re-arm) */

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
    /* Also append to %TEMP%\cn1windows.log so logs survive when the console is
     * hidden on an interactive launch (see initDisplay). */
    {
        char path[MAX_PATH];
        DWORD n = GetTempPathA(MAX_PATH, path);
        if (n > 0 && n < MAX_PATH - 16) {
            FILE* f;
            strcpy(path + n, "cn1windows.log");
            f = fopen(path, "a");
            if (f != NULL) {
                fputs(message, f);
                fputc('\n', f);
                fclose(f);
            }
        }
    }
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
    /* The access type + faulting data address pin down what was dereferenced
     * (e.g. a corrupt String pointer vs a small null+offset). */
    if (info->ExceptionRecord->NumberParameters >= 2) {
        ULONG_PTR acc = info->ExceptionRecord->ExceptionInformation[0];
        sprintf(buf, "  access=%s dataAddr=%p",
                acc == 0 ? "read" : (acc == 1 ? "write" : (acc == 8 ? "exec" : "?")),
                (void*) info->ExceptionRecord->ExceptionInformation[1]);
        cn1WindowsLog(buf);
    }
#ifdef _M_ARM64
    /* On ARM64 the crash context's Lr is the faulting function's caller return
     * address; walk the x29 frame-pointer chain for the frames above it. */
    CONTEXT* ctx = info->ContextRecord;
    sprintf(buf, "  pc=%p lr=%p fp=%p sp=%p", (void*) ctx->Pc, (void*) ctx->Lr, (void*) ctx->Fp, (void*) ctx->Sp);
    cn1WindowsLog(buf);
    for (int r = 0; r <= 8; r++) {
        sprintf(buf, "  x%d=%p", r, (void*) ctx->X[r]);
        cn1WindowsLog(buf);
    }
    /* Proper ARM64 unwind via .pdata (RtlVirtualUnwind); works even when the
     * x29 frame-pointer chain is corrupt (fp=0x14 in the observed crash).
     * Prints each frame's RVA so it symbolizes against the PDB. */
    {
        DWORD64 modBase = (DWORD64) GetModuleHandleW(NULL);
        CONTEXT uc = *ctx;
        for (int i = 0; i < 40 && uc.Pc != 0; i++) {
            sprintf(buf, "  stack[%d]=%p rva=%p", i, (void*) uc.Pc, (void*) (uc.Pc - modBase));
            cn1WindowsLog(buf);
            DWORD64 imageBase = 0;
            PRUNTIME_FUNCTION fe = RtlLookupFunctionEntry(uc.Pc, &imageBase, NULL);
            if (fe == NULL) {
                /* leaf function: the return address is in Lr. */
                if (uc.Lr == 0 || uc.Lr == uc.Pc) {
                    break;
                }
                uc.Pc = uc.Lr;
                continue;
            }
            PVOID handlerData = NULL;
            DWORD64 establisher = 0;
            RtlVirtualUnwind(UNW_FLAG_NHANDLER, imageBase, uc.Pc, fe, &uc,
                             &handlerData, &establisher, NULL);
        }
    }
#endif
    return EXCEPTION_EXECUTE_HANDLER;
}

/* ----------------------------------------- hardware fault -> Java exception
 *
 * The ParparVM "clean" C target only emits an NPE check for a method's `this`
 * (and only at method entry, after the receiver's vtable has already been read
 * for the virtual dispatch). A null *argument* deref, or a virtual call on a
 * null receiver, therefore faults in native code as a raw EXCEPTION_ACCESS_-
 * VIOLATION instead of a catchable NullPointerException -- and that hard-crashes
 * the whole process (and, on CI, the entire screenshot suite) with no Java stack
 * trace. The iOS port solves the same problem with a SIGSEGV handler that calls
 * throwException(); this is the Win32 analog.
 *
 * A vectored exception handler fires first-chance, like a Unix signal. Rather
 * than run throwException() in the kernel's exception-dispatch context (where a
 * longjmp out is fragile), we redirect the faulting instruction pointer to a
 * trampoline and resume: the trampoline then runs on the faulting thread's own
 * stack, in a normal context, so throwException()'s longjmp to the nearest CN1
 * try/catch frame behaves exactly as it would for a Java-thrown exception. The
 * exception unwinds to the test runner's catch, which logs it (with Java method
 * names, via printStackTrace) and moves on to the next test.
 *
 * As the iOS comment notes, this WILL interfere with a native debugger -- a
 * debugger sees the first-chance AV before us. It is a release/CI resilience
 * mechanism, not a debugging aid.
 */

/* Deliver `exc` to the nearest matching CN1 try/catch frame by restoring that
 * frame's saved setjmp() register state directly into the faulting CONTEXT, then
 * returning EXCEPTION_CONTINUE_EXECUTION so the kernel resumes at the setjmp()
 * continuation. This is precisely what longjmp() does MINUS the SEH stack unwind
 * (RtlUnwind) -- and that unwind is exactly what cannot be driven from a fault-
 * handler context (it fast-fails and silently kills the process), so we bypass
 * it and restore the registers ourselves from the jmp_buf (_JUMP_BUFFER).
 *
 * The block search mirrors throwException() in cn1_globals.c: synchronized-method
 * monitors are exited as their frames are skipped, and a frame matches when its
 * exceptionClass is <=0 (catch-all) or the thrown class is instanceof it.
 * Returns 1 with ctx updated when a handler is found, 0 when none exists. */
static int cn1WinDeliverViaContext(CONTEXT* ctx, struct ThreadLocalData* t, JAVA_OBJECT exc) {
    java_lang_Throwable_fillInStack__(t, exc);
    t->exception = exc;
    int excClassId = exc->__codenameOneParentClsReference->classId;
    t->tryBlockOffset--;
    while (t->tryBlockOffset >= 0) {
        struct TryBlock* blk = &t->blocks[t->tryBlockOffset];
        if (blk->monitor != 0) {
            monitorExitBlock(t, blk->monitor);
            t->tryBlockOffset--;
            continue;
        }
        if (blk->exceptionClass <= 0 || instanceofFunction(blk->exceptionClass, excClassId)) {
            _JUMP_BUFFER* jb = (_JUMP_BUFFER*) blk->destination;
#if defined(_M_ARM64)
            for (int i = 19; i <= 28; i++) {     /* x19-x28 callee-saved */
                ctx->X[i] = (&jb->X19)[i - 19];
            }
            ctx->Fp = jb->Fp;
            ctx->Lr = jb->Lr;
            ctx->Sp = jb->Sp;
            ctx->Pc = jb->Lr;   /* setjmp resumes at its saved return address... */
            ctx->X[0] = 1;      /* ...returning 1 (the longjmp value) */
            return 1;
#elif defined(_M_X64)
            ctx->Rbx = jb->Rbx;
            ctx->Rsp = jb->Rsp;
            ctx->Rbp = jb->Rbp;
            ctx->Rsi = jb->Rsi;
            ctx->Rdi = jb->Rdi;
            ctx->R12 = jb->R12;
            ctx->R13 = jb->R13;
            ctx->R14 = jb->R14;
            ctx->R15 = jb->R15;
            ctx->Rip = jb->Rip; /* the instruction after the setjmp() call */
            ctx->Rax = 1;       /* setjmp returns 1 (the longjmp value) */
            return 1;
#else
            return 0;
#endif
        }
        t->tryBlockOffset--;
    }
    return 0;
}

static LONG WINAPI cn1WinFaultToException(EXCEPTION_POINTERS* info) {
    DWORD code = info->ExceptionRecord->ExceptionCode;
    struct ThreadLocalData* t;
    JAVA_OBJECT exc;

    if (code == EXCEPTION_ACCESS_VIOLATION) {
        /* Only treat a genuine null-object deref (null + small field/vtable
         * offset) as an NPE. A wild/large faulting address is real memory
         * corruption -- leave it for the unhandled-exception logger so it stays
         * diagnosable instead of being masked as an NPE. */
        ULONG_PTR faultAddr = info->ExceptionRecord->NumberParameters >= 2
                ? info->ExceptionRecord->ExceptionInformation[1] : (ULONG_PTR) ~0;
        if (faultAddr >= 0x10000) {
            return EXCEPTION_CONTINUE_SEARCH;
        }
        t = getThreadLocalData();
        if (t == NULL || t->tryBlockOffset <= 0) {
            return EXCEPTION_CONTINUE_SEARCH;
        }
        exc = __NEW_INSTANCE_java_lang_NullPointerException(t);
    } else if (code == EXCEPTION_STACK_OVERFLOW) {
        /* Re-arm the guard page the overflow consumed so the (small) work below
         * has stack; the context restore then jumps to a frame high up the stack
         * with room to spare. */
        _resetstkoflw();
        t = getThreadLocalData();
        if (t == NULL || t->tryBlockOffset <= 0) {
            return EXCEPTION_CONTINUE_SEARCH;
        }
        exc = __NEW_INSTANCE_java_lang_StackOverflowError(t);
    } else {
        return EXCEPTION_CONTINUE_SEARCH;
    }

    if (cn1WinDeliverViaContext(info->ContextRecord, t, exc)) {
        return EXCEPTION_CONTINUE_EXECUTION;
    }
    /* No CN1 catch frame on this thread: fall through to the unhandled-exception
     * logger, which records the fault and terminates. */
    return EXCEPTION_CONTINUE_SEARCH;
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
        case WM_MOUSEWHEEL:
        case WM_MOUSEHWHEEL: {
            /* The wheel message reports the cursor in SCREEN coordinates; the
             * input ring (and the synthetic scroll the EDT builds from it) work
             * in client coordinates, so map it. The delta is signed, a multiple
             * of WHEEL_DELTA (120). */
            POINT pt;
            pt.x = GET_X_LPARAM(lParam);
            pt.y = GET_Y_LPARAM(lParam);
            ScreenToClient(hwnd, &pt);
            int delta = GET_WHEEL_DELTA_WPARAM(wParam);
            cn1WinPushEvent(msg == WM_MOUSEHWHEEL ? CN1_EVENT_MOUSE_HWHEEL : CN1_EVENT_MOUSE_WHEEL,
                    pt.x, pt.y, delta);
            return 0;
        }
        case WM_KEYDOWN:
            cn1WinPushEvent(CN1_EVENT_KEY_PRESSED, 0, 0, (int) wParam);
            return 0;
        case WM_KEYUP:
            cn1WinPushEvent(CN1_EVENT_KEY_RELEASED, 0, 0, (int) wParam);
            return 0;
        case WM_SIZE:
            cn1Win.width = LOWORD(lParam);
            cn1Win.height = HIWORD(lParam);
            /* Defer the Direct2D Resize to the EDT (cn1WinApplyPendingResize):
             * resizing the target from this (window) thread while the EDT is
             * mid-frame corrupts the present and leaves the new area black. */
            cn1Win.pendingW = cn1Win.width;
            cn1Win.pendingH = cn1Win.height;
            cn1Win.pendingResize = 1;
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
        case WM_CN1_BROWSER:
            /* WebView2 operation marshaled from the EDT (cn1_windows_browser.cpp). */
            cn1WinBrowserHandleMessage(wParam, lParam);
            return 0;
        case WM_CN1_EDIT:
            /* Native EDIT-control op marshaled from the EDT (cn1_windows_edit.c). */
            cn1WinEditHandleMessage(wParam, lParam);
            return 0;
        case WM_CN1_FILEDIALOG:
            /* Modal file open/save dialog, run synchronously on this (pump) thread
             * while the EDT blocks in SendMessage (cn1_windows_io.c). */
            return cn1WinFileDialogHandleMessage(wParam);
        case WM_CTLCOLOREDIT: {
            /* Colour the native edit overlay to match the CN1 field it stands in
             * for; fall through to default when it is not our control. */
            HBRUSH br = cn1WinEditCtlColor((HDC) wParam, (HWND) lParam);
            if (br != NULL) {
                return (LRESULT) br;
            }
            return DefWindowProcW(hwnd, msg, wParam, lParam);
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

    /* WS_CLIPCHILDREN so the Direct2D present does not paint over native child
     * controls overlaid on the form (the WebView2 browser peer and the EDIT
     * control used for native text editing). */
    cn1Win.hwnd = CreateWindowExW(0, L"CodenameOneWindow", wTitle,
            WS_OVERLAPPEDWINDOW | WS_CLIPCHILDREN, CW_USEDEFAULT, CW_USEDEFAULT, width, height,
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

/*
 * Applies a pending window resize to the HWND render target. Called on the EDT
 * (from cn1WinBeginFrame) so the Resize happens between frames on the same
 * thread that draws -- never while a frame is open on another thread.
 */
void cn1WinApplyPendingResize(void) {
    if (cn1Win.pendingResize && g_hwndTarget != NULL) {
        D2D1_SIZE_U size;
        size.width = (UINT32) cn1Win.pendingW;
        size.height = (UINT32) cn1Win.pendingH;
        ID2D1HwndRenderTarget_Resize(g_hwndTarget, &size);
        cn1Win.pendingResize = 0;
    }
}

/* True when CN1_FAULT_SELFTEST is set in the environment. The clean target does
 * not translate System.getenv, so the launcher's fault-handler self-test gate
 * reads the environment here via Win32 instead. */
JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_faultSelfTestEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    char buf[8];
    DWORD n = GetEnvironmentVariableA("CN1_FAULT_SELFTEST", buf, (DWORD) sizeof(buf));
    return (n > 0 && n < sizeof(buf)) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_initDisplay___java_lang_String_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    if (InterlockedCompareExchange(&cn1Win.initialized, 1, 0) != 0) {
        return; /* already initialised */
    }

    CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
    SetUnhandledExceptionFilter(cn1WinUnhandled);
    /* First-chance: turn null-deref / stack-overflow faults into catchable Java
     * exceptions (see cn1WinFaultToException). Installed first (FirstHandler=1)
     * so it runs before the OS default, but it only redirects genuine null
     * derefs / overflows with a CN1 catch frame and lets everything else pass
     * through to the unhandled-exception logger above. */
    AddVectoredExceptionHandler(1, cn1WinFaultToException);

    /* The exe is linked as a GUI-subsystem app (see writeCmakeProject), so
     * double-clicking it never allocates a console -- no stray window, no stdout.
     * When launched from a console (cmd), GUI-subsystem processes do NOT get their
     * CRT stdout/stderr wired to that console automatically, so attach to the
     * parent console (if any) and reopen the C streams onto it. AttachConsole fails
     * with ERROR_INVALID_HANDLE when there is no parent console (the double-click
     * case) -- we then leave stdio alone so nothing is shown. A redirected stdout
     * pipe (the screenshot CI harness) is already inherited and must be preserved,
     * so only reopen a stream when it is not already redirected to a file/pipe. We
     * must NOT touch GetConsoleWindow() here: from cmd that is the user's own
     * window. cn1WindowsLog also mirrors to %TEMP%\cn1windows.log regardless. */
    if (AttachConsole(ATTACH_PARENT_PROCESS)) {
        if (GetFileType(GetStdHandle(STD_OUTPUT_HANDLE)) == FILE_TYPE_UNKNOWN) {
            FILE* f = NULL;
            freopen_s(&f, "CONOUT$", "w", stdout);
        }
        if (GetFileType(GetStdHandle(STD_ERROR_HANDLE)) == FILE_TYPE_UNKNOWN) {
            FILE* f = NULL;
            freopen_s(&f, "CONOUT$", "w", stderr);
        }
    }

    InitializeCriticalSection(&cn1Win.eventLock);
    cn1Win.eventSignal = CreateEventW(NULL, FALSE, FALSE, NULL);
    cn1Win.eventHead = 0;
    cn1Win.eventTail = 0;
    cn1Win.dpiScale = 1.0f;

    /* Headless screenshot mode: no window; render into an offscreen WIC bitmap
     * that headlessTick later encodes to PNG. The EDT paints through the same
     * getWindowGraphics + flushGraphics path as on-screen. */
    if (cn1Win.headless) {
        cn1Win.width = cn1Win.shotW > 0 ? cn1Win.shotW : 400;
        cn1Win.height = cn1Win.shotH > 0 ? cn1Win.shotH : 600;
        cn1Win.windowGraphics = cn1WinCreateOffscreenGraphics(cn1Win.width, cn1Win.height);
        if (cn1Win.windowGraphics == NULL) {
            cn1WindowsLog("initDisplay: headless offscreen target failed");
        } else {
            cn1WindowsLog("initDisplay: headless offscreen target created");
        }
        return;
    }

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
    /* RETAIN_CONTENTS: Codename One repaints only the dirty region each frame and
     * relies on the rest of the surface being preserved across presents. The
     * default (PRESENT_OPTIONS_NONE) discards the back buffer after Present, so
     * everything the EDT did not repaint that frame showed stale pixels -- the
     * overscroll "smear" at the form edges (a window resize recreated the target
     * and briefly hid it). Retaining the contents makes partial repaints correct. */
    hwndProps.presentOptions = D2D1_PRESENT_OPTIONS_RETAIN_CONTENTS;

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

/* Real horizontal screen DPI (96 == 100% scale), so the desktop port sizes
 * mm-based theme metrics correctly instead of using the mobile density
 * heuristic, which over-scales everything. */
JAVA_INT com_codename1_impl_windows_WindowsNative_screenDpi___R_int(CODENAME_ONE_THREAD_STATE) {
    int dpi = 96;
    HDC dc = GetDC(cn1Win.hwnd);
    if (dc != NULL) {
        int v = GetDeviceCaps(dc, LOGPIXELSX);
        ReleaseDC(cn1Win.hwnd, dc);
        if (v > 0) {
            dpi = v;
        }
    }
    return (JAVA_INT) dpi;
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
    /* Mark a completed present so the headless capture can tell when the EDT has
     * stopped painting (see runHeadlessLoop). */
    InterlockedIncrement(&cn1Win.flushGen);
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_pollEvent___int_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    /* The message loop runs on the main thread (pumpMessages), which also drains
     * this ring into Codename One. pollEvent must not PeekMessage on its own
     * (empty) queue. */
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

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_pumpMessages___R_boolean(CODENAME_ONE_THREAD_STATE) {
    /* Runs on the app's main thread (the window's owner) after Display.init.
     * Blocks for the next window message, dispatches it plus any already-queued
     * burst, then returns to Java so the caller can drain the translated input
     * into Codename One -- which is what wakes the EDT (it sleeps on the Display
     * lock, not on the native message queue). Returns JAVA_FALSE once the window
     * has closed (WM_QUIT) so the Java loop terminates.
     *
     * Window messages are delivered to the thread that created the window (this
     * one). The EDT is a different thread, so it can neither see nor pump them --
     * the producer/consumer split is deliberate and mirrors how every desktop CN1
     * port feeds input from the native UI thread to the EDT. */
    MSG msg;
    BOOL got;
    /* Yield the thread state across the (indefinitely) blocking GetMessage so the
     * GC never waits on this thread. Resume before dispatching so the window proc
     * runs with an active thread. */
    CN1_YIELD_THREAD;
    got = GetMessageW(&msg, NULL, 0, 0);
    CN1_RESUME_THREAD;
    if (got <= 0) {
        return JAVA_FALSE;
    }
    TranslateMessage(&msg);
    DispatchMessageW(&msg);
    /* Drain the rest of an input/resize burst without blocking, so the whole
     * batch is translated before we hand control back to drain it. */
    while (PeekMessageW(&msg, NULL, 0, 0, PM_REMOVE)) {
        if (msg.message == WM_QUIT) {
            return JAVA_FALSE;
        }
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }
    return JAVA_TRUE;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_runHeadlessLoop__(CODENAME_ONE_THREAD_STATE) {
    /* Headless capture has no window / message loop. The main thread parks here
     * to keep the process alive while the EDT (a separate thread) paints into
     * the offscreen target, then -- after a fixed settle -- encodes the bitmap
     * to PNG and exits. Driving the capture from here (not the EDT idle hook)
     * makes it independent of the EDT's scheduling and guarantees termination.
     * The UI is static, so by the settle the EDT is idle and not mid-paint. */
    cn1WindowsLog("runHeadlessLoop: enter");
    /* Wait for painting to SETTLE rather than for a fixed wall-clock time. A flat
     * settle that is comfortable for a light form (one fillArc) is too short for a
     * heavy one (draw-arc fills 100 concentric arcs per cell, draw-image-rect a
     * dozen scaled blits) on the software-rendered headless target -- the frame
     * was grabbed mid-paint with only the first grid cell drawn. Instead, snapshot
     * once flushGraphics (one per completed present) has not advanced for a quiet
     * window, after at least one present, bounded by a hard ceiling so a wedged
     * EDT still terminates. When the count is stable the EDT is between frames, so
     * reading the offscreen target is safe. */
    const int QUIET_MS = 900;     /* paint considered settled after this idle gap */
    const int MIN_MS = 1500;      /* always give the first paint at least this long */
    const int MAX_MS = 30000;     /* hard ceiling: capture regardless after this   */
    int waitedMs = 0;
    int quietMs = 0;
    LONG lastGen = -1;
    CN1_YIELD_THREAD;
    while (waitedMs < MAX_MS) {
        Sleep(50);
        waitedMs += 50;
        LONG gen = cn1Win.flushGen;
        if (gen != lastGen) {
            lastGen = gen;
            quietMs = 0;            /* a present landed -- restart the quiet timer */
        } else {
            quietMs += 50;
        }
        /* Settled: at least one present, then a quiet stretch, past the floor. */
        if (lastGen > 0 && quietMs >= QUIET_MS && waitedMs >= MIN_MS) {
            break;
        }
    }
    CN1_RESUME_THREAD;
    JAVA_BOOLEAN ok = cn1WinEncodeGraphicsToPng(cn1Win.windowGraphics, cn1Win.shotPath);
    cn1WindowsLog(ok ? "runHeadlessLoop: screenshot saved" : "runHeadlessLoop: screenshot FAILED");
    ExitProcess(ok ? 0 : 2);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_exitProcess___int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT __cn1Arg1) {
    ExitProcess((UINT) __cn1Arg1);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_sleepMillis___int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT __cn1Arg1) {
    if (__cn1Arg1 > 0) {
        CN1_YIELD_THREAD;
        Sleep((DWORD) __cn1Arg1);
        CN1_RESUME_THREAD;
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_parkMainThread___int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT __cn1Arg1) {
    /* Keeps the main thread (and process) alive while worker threads -- the EDT,
     * the WebSocket reader -- do their work, up to a timeout safety net. Callers
     * exit early via exitProcess once their async work has completed. */
    int waitedMs = 0;
    CN1_YIELD_THREAD;
    while (waitedMs < __cn1Arg1) {
        Sleep(50);
        waitedMs += 50;
    }
    CN1_RESUME_THREAD;
    ExitProcess(3);
}

} /* extern "C" */

#endif /* _WIN32 */
