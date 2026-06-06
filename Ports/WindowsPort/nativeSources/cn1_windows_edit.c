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
 * Native in-place text editing for the Windows port. While a Codename One
 * TextField/TextArea is edited it is overlaid by a real Win32 EDIT control (a
 * native caret, selection, IME and keyboard), positioned at the component's
 * bounds; on commit the text is read back and the control is torn down. This is
 * the Win32 analog of the iOS native field overlay.
 *
 * The EDIT control must live on the thread that owns the main window and pumps
 * messages (only that thread receives the control's input), so creation,
 * teardown and focus are marshaled to the pump thread via WM_CN1_EDIT, mirroring
 * the WebView2 peer. The EDT, blocked in invokeAndBlock, polls editIsDone and
 * then reads the text with a cross-thread WM_GETTEXT (serviced by the pump).
 */

#include "cn1_windows.h"

#ifdef _WIN32

#define CN1_EDIT_OP_CREATE  1
#define CN1_EDIT_OP_DESTROY 2

typedef struct CN1Edit {
    HWND hwnd;             /* the EDIT control, created on the pump thread       */
    int x, y, w, h;
    JAVA_BOOLEAN singleLine;
    int maxSize;
    WCHAR* initialText;    /* owned; set on create then freed                    */
    WNDPROC origProc;      /* subclass chain                                     */
    volatile LONG done;    /* set when the user commits (Enter / focus loss)     */
} CN1Edit;

/*
 * Subclass proc for the EDIT control: a single-line field commits on Enter,
 * any field commits when it loses focus (the user clicked elsewhere) or on
 * Escape. WM_GETDLGCODE asks Windows to route Enter/Tab to us rather than the
 * dialog manager. The committed flag is polled by the EDT.
 */
static LRESULT CALLBACK cn1EditSubclassProc(HWND h, UINT msg, WPARAM wp, LPARAM lp) {
    CN1Edit* e = (CN1Edit*) GetWindowLongPtrW(h, GWLP_USERDATA);
    if (e == NULL) {
        return DefWindowProcW(h, msg, wp, lp);
    }
    switch (msg) {
        case WM_GETDLGCODE:
            return DLGC_WANTALLKEYS | CallWindowProcW(e->origProc, h, msg, wp, lp);
        case WM_KEYDOWN:
            if ((wp == VK_RETURN && e->singleLine) || wp == VK_ESCAPE) {
                InterlockedExchange(&e->done, 1);
                return 0;
            }
            break;
        case WM_CHAR:
            /* Swallow the Enter character on a single-line field so it does not
             * beep after we have already consumed the key-down as a commit. */
            if (wp == VK_RETURN && e->singleLine) {
                return 0;
            }
            break;
        case WM_KILLFOCUS:
            InterlockedExchange(&e->done, 1);
            break;
        default:
            break;
    }
    return CallWindowProcW(e->origProc, h, msg, wp, lp);
}

/* Runs on the pump thread (dispatched from cn1WinWndProc on WM_CN1_EDIT). */
void cn1WinEditHandleMessage(WPARAM op, LPARAM lp) {
    CN1Edit* e = (CN1Edit*) lp;
    if (e == NULL) {
        return;
    }
    if (op == CN1_EDIT_OP_CREATE) {
        DWORD style = WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL | ES_LEFT;
        if (!e->singleLine) {
            style |= ES_MULTILINE | ES_AUTOVSCROLL | ES_WANTRETURN | WS_VSCROLL;
        }
        e->hwnd = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"", style,
                e->x, e->y, e->w, e->h, cn1Win.hwnd, NULL, GetModuleHandleW(NULL), NULL);
        if (e->hwnd != NULL) {
            SetWindowLongPtrW(e->hwnd, GWLP_USERDATA, (LONG_PTR) e);
            e->origProc = (WNDPROC) SetWindowLongPtrW(e->hwnd, GWLP_WNDPROC,
                    (LONG_PTR) cn1EditSubclassProc);
            SendMessageW(e->hwnd, WM_SETFONT, (WPARAM) GetStockObject(DEFAULT_GUI_FONT), TRUE);
            if (e->maxSize > 0) {
                SendMessageW(e->hwnd, EM_SETLIMITTEXT, (WPARAM) e->maxSize, 0);
            }
            if (e->initialText != NULL) {
                SetWindowTextW(e->hwnd, e->initialText);
                int len = (int) wcslen(e->initialText);
                SendMessageW(e->hwnd, EM_SETSEL, (WPARAM) len, (LPARAM) len);
            }
            SetFocus(e->hwnd);
        }
        if (e->initialText != NULL) {
            free(e->initialText);
            e->initialText = NULL;
        }
    } else if (op == CN1_EDIT_OP_DESTROY) {
        if (e->hwnd != NULL) {
            DestroyWindow(e->hwnd);
            e->hwnd = NULL;
        }
        free(e);
    }
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_editStringAt___int_int_int_int_java_lang_String_boolean_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h,
        JAVA_OBJECT text, JAVA_BOOLEAN singleLine, JAVA_INT maxSize) {
    /* No host window (headless screenshot mode) -> no native editing surface. */
    if (cn1Win.hwnd == NULL) {
        return 0;
    }
    CN1Edit* e = (CN1Edit*) calloc(1, sizeof(CN1Edit));
    if (e == NULL) {
        return 0;
    }
    e->x = x;
    e->y = y;
    e->w = w > 0 ? w : 1;
    e->h = h > 0 ? h : 1;
    e->singleLine = singleLine;
    e->maxSize = maxSize;
    if (text != JAVA_NULL) {
        e->initialText = cn1WinJavaStringToWide(threadStateData, text, NULL);
    }
    PostMessageW(cn1Win.hwnd, WM_CN1_EDIT, CN1_EDIT_OP_CREATE, (LPARAM) e);
    return (JAVA_LONG) (intptr_t) e;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_editIsDone___long_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Edit* e = (CN1Edit*) (intptr_t) peer;
    return (e != NULL && e->done) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_editGetText___long_R_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Edit* e = (CN1Edit*) (intptr_t) peer;
    if (e == NULL || e->hwnd == NULL) {
        return JAVA_NULL;
    }
    /* Cross-thread sends are serviced by the pump thread inside its GetMessage. */
    int len = (int) SendMessageW(e->hwnd, WM_GETTEXTLENGTH, 0, 0);
    WCHAR* buf = (WCHAR*) malloc((size_t) (len + 1) * sizeof(WCHAR));
    if (buf == NULL) {
        return JAVA_NULL;
    }
    buf[0] = 0;
    SendMessageW(e->hwnd, WM_GETTEXT, (WPARAM) (len + 1), (LPARAM) buf);
    JAVA_OBJECT result = JAVA_NULL;
    int n = WideCharToMultiByte(CP_UTF8, 0, buf, -1, NULL, 0, NULL, NULL);
    if (n > 0) {
        char* utf8 = (char*) malloc((size_t) n);
        if (utf8 != NULL) {
            WideCharToMultiByte(CP_UTF8, 0, buf, -1, utf8, n, NULL, NULL);
            result = newStringFromCString(threadStateData, utf8);
            free(utf8);
        }
    }
    free(buf);
    return result;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_editClose___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Edit* e = (CN1Edit*) (intptr_t) peer;
    if (e == NULL) {
        return;
    }
    PostMessageW(cn1Win.hwnd, WM_CN1_EDIT, CN1_EDIT_OP_DESTROY, (LPARAM) e);
}

#endif /* _WIN32 */
