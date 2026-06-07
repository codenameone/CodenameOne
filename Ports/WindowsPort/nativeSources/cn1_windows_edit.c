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
 * text area and styled to match the field (same font/family/size, foreground and
 * background colour, alignment, borderless) so it reads as the same field; on
 * commit the text is read back and the control is torn down.
 *
 * The EDIT control must live on the thread that owns the main window and pumps
 * messages (only that thread receives the control's input), so creation,
 * teardown and focus are marshaled to the pump thread via WM_CN1_EDIT, mirroring
 * the WebView2 peer. Editing is asynchronous: the EDT polls editIsDone and reads
 * the text with a cross-thread WM_GETTEXT (serviced by the pump).
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
    int align;             /* 0 left, 1 center, 2 right                          */
    void* font;            /* CN1Font* used to build the GDI font, or NULL       */
    COLORREF fg;
    COLORREF bg;
    int hasColors;         /* 1 when fg/bg are valid                             */
    HFONT gdiFont;         /* owned                                              */
    HBRUSH bgBrush;        /* owned, for WM_CTLCOLOREDIT                          */
    WCHAR* initialText;    /* owned; set on create then freed                    */
    WNDPROC origProc;      /* subclass chain                                     */
    volatile LONG done;    /* set when the user commits (Enter / focus loss)     */
} CN1Edit;

/* The single active edit (Codename One edits one field at a time). Used by the
 * window proc's WM_CTLCOLOREDIT to colour the control. */
static CN1Edit* g_currentEdit = NULL;

static COLORREF cn1EditColorRef(JAVA_INT rgb) {
    int r = (rgb >> 16) & 0xff;
    int g = (rgb >> 8) & 0xff;
    int b = rgb & 0xff;
    return RGB(r, g, b);
}

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

/*
 * Called by the window proc for WM_CTLCOLOREDIT: paints the control with the
 * field's foreground/background so it matches the Codename One field. Returns a
 * background brush, or NULL to let the default handling run.
 */
HBRUSH cn1WinEditCtlColor(HDC hdc, HWND control) {
    CN1Edit* e = g_currentEdit;
    if (e == NULL || e->hwnd != control || !e->hasColors) {
        return NULL;
    }
    SetTextColor(hdc, e->fg);
    SetBkColor(hdc, e->bg);
    SetBkMode(hdc, OPAQUE);
    if (e->bgBrush == NULL) {
        e->bgBrush = CreateSolidBrush(e->bg);
    }
    return e->bgBrush;
}

/* Runs on the pump thread (dispatched from cn1WinWndProc on WM_CN1_EDIT). */
void cn1WinEditHandleMessage(WPARAM op, LPARAM lp) {
    CN1Edit* e = (CN1Edit*) lp;
    if (e == NULL) {
        return;
    }
    if (op == CN1_EDIT_OP_CREATE) {
        DWORD style = WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL;
        style |= (e->align == 1) ? ES_CENTER : (e->align == 2 ? ES_RIGHT : ES_LEFT);
        if (!e->singleLine) {
            style |= ES_MULTILINE | ES_AUTOVSCROLL | ES_WANTRETURN;
        }
        /* No WS_EX_CLIENTEDGE: the Codename One field already draws its border and
         * background around the control's (padding-inset) text area, so the EDIT is
         * borderless and colour-matched to blend in. */
        e->hwnd = CreateWindowExW(0, L"EDIT", L"", style,
                e->x, e->y, e->w, e->h, cn1Win.hwnd, NULL, GetModuleHandleW(NULL), NULL);
        if (e->hwnd != NULL) {
            g_currentEdit = e;
            SetWindowLongPtrW(e->hwnd, GWLP_USERDATA, (LONG_PTR) e);
            e->origProc = (WNDPROC) SetWindowLongPtrW(e->hwnd, GWLP_WNDPROC,
                    (LONG_PTR) cn1EditSubclassProc);
            /* Build a GDI font matching the field's DirectWrite font (same family
             * and pixel size) so the overlaid text looks identical. */
            CN1Font* f = (CN1Font*) e->font;
            if (f != NULL && f->family != NULL && f->size > 0.0f) {
                int px = (int) (f->size + 0.5f);
                e->gdiFont = CreateFontW(-px, 0, 0, 0,
                        (f->style & 1) ? FW_BOLD : FW_NORMAL,
                        (f->style & 2) ? TRUE : FALSE,
                        FALSE, FALSE, DEFAULT_CHARSET, OUT_DEFAULT_PRECIS,
                        CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, DEFAULT_PITCH, f->family);
            }
            SendMessageW(e->hwnd, WM_SETFONT,
                    (WPARAM) (e->gdiFont != NULL ? e->gdiFont : GetStockObject(DEFAULT_GUI_FONT)), TRUE);
            if (e->maxSize > 0) {
                SendMessageW(e->hwnd, EM_SETLIMITTEXT, (WPARAM) e->maxSize, 0);
            }
            /* Tighten the internal margins so the text starts where the field's
             * own text does (the padding is already applied to the bounds). */
            SendMessageW(e->hwnd, EM_SETMARGINS, EC_LEFTMARGIN | EC_RIGHTMARGIN, MAKELPARAM(0, 0));
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
        if (g_currentEdit == e) {
            g_currentEdit = NULL;
        }
        if (e->hwnd != NULL) {
            DestroyWindow(e->hwnd);
            e->hwnd = NULL;
        }
        if (e->gdiFont != NULL) {
            DeleteObject(e->gdiFont);
            e->gdiFont = NULL;
        }
        if (e->bgBrush != NULL) {
            DeleteObject(e->bgBrush);
            e->bgBrush = NULL;
        }
        free(e);
    }
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_editStringAt___int_int_int_int_java_lang_String_boolean_int_long_int_int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h,
        JAVA_OBJECT text, JAVA_BOOLEAN singleLine, JAVA_INT maxSize, JAVA_LONG fontPeer,
        JAVA_INT fgColor, JAVA_INT bgColor, JAVA_INT align) {
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
    e->align = align;
    e->font = (void*) (intptr_t) fontPeer;
    if (fgColor >= 0 && bgColor >= 0) {
        e->fg = cn1EditColorRef(fgColor);
        e->bg = cn1EditColorRef(bgColor);
        e->hasColors = 1;
    }
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
