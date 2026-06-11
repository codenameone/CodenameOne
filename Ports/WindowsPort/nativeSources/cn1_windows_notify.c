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
 * Local notifications for the Windows port via Shell_NotifyIcon balloons. This is
 * the desktop semantic the JavaSE port uses (a tray icon + balloon, surfaced while
 * the app runs): the Java side schedules a Timer (WindowsImplementation), and at
 * fire time posts the title/body/id to the pump thread which owns the tray icon.
 * When the user clicks the balloon, the tray's callback (WM_CN1_TRAY) records the
 * notification id; WindowsImplementation.drainInput polls it and dispatches it to
 * the app's LocalNotificationCallback, mirroring mobile.
 *
 * The tray icon and Shell_NotifyIcon calls must run on the window-owning pump
 * thread (the icon's callback is delivered to that window), so the EDT-facing
 * showNotification posts WM_CN1_NOTIFY rather than calling Shell_NotifyIcon itself.
 */

#include "cn1_windows.h"

#ifdef _WIN32

#include <shellapi.h>
#include <stdlib.h>
#include <string.h>

/* NIN_BALLOONUSERCLICK lives behind NTDDI >= Vista; define it if the SDK shim
 * for this toolchain did not expose it. */
#ifndef NIN_BALLOONUSERCLICK
#define NIN_BALLOONUSERCLICK (WM_USER + 5)
#endif

#define CN1_TRAY_UID 1

typedef struct CN1NotifyReq {
    char* id;       /* owned UTF-8 */
    WCHAR* title;   /* owned */
    WCHAR* body;    /* owned */
} CN1NotifyReq;

static int g_trayAdded = 0;
static char* g_lastNotifyId = NULL;   /* id of the most recently shown balloon */
static char* g_clickedId = NULL;      /* set when the balloon is clicked, drained by Java */

/* Shows (or re-shows) the tray balloon. Runs on the pump thread. */
void cn1WinNotifyHandleMessage(WPARAM wParam) {
    CN1NotifyReq* req = (CN1NotifyReq*) wParam;
    NOTIFYICONDATAW nid;
    if (req == NULL || cn1Win.hwnd == NULL) {
        goto cleanup;
    }
    ZeroMemory(&nid, sizeof(nid));
    nid.cbSize = sizeof(nid);
    nid.hWnd = cn1Win.hwnd;
    nid.uID = CN1_TRAY_UID;
    if (!g_trayAdded) {
        nid.uFlags = NIF_MESSAGE | NIF_ICON | NIF_TIP;
        nid.uCallbackMessage = WM_CN1_TRAY;
        nid.hIcon = LoadIconW(NULL, (LPCWSTR) IDI_APPLICATION);
        lstrcpynW(nid.szTip, L"Codename One", (int) (sizeof(nid.szTip) / sizeof(WCHAR)));
        if (Shell_NotifyIconW(NIM_ADD, &nid)) {
            g_trayAdded = 1;
        }
        ZeroMemory(&nid, sizeof(nid));
        nid.cbSize = sizeof(nid);
        nid.hWnd = cn1Win.hwnd;
        nid.uID = CN1_TRAY_UID;
    }
    nid.uFlags = NIF_INFO;
    nid.dwInfoFlags = NIIF_INFO;
    if (req->title != NULL) {
        lstrcpynW(nid.szInfoTitle, req->title, (int) (sizeof(nid.szInfoTitle) / sizeof(WCHAR)));
    }
    if (req->body != NULL) {
        lstrcpynW(nid.szInfo, req->body, (int) (sizeof(nid.szInfo) / sizeof(WCHAR)));
    }
    Shell_NotifyIconW(NIM_MODIFY, &nid);

    /* Remember which notification this balloon represents for the click route. */
    EnterCriticalSection(&cn1Win.eventLock);
    if (g_lastNotifyId != NULL) {
        free(g_lastNotifyId);
    }
    g_lastNotifyId = req->id;   /* take ownership; not freed below */
    req->id = NULL;
    LeaveCriticalSection(&cn1Win.eventLock);

cleanup:
    if (req != NULL) {
        if (req->id != NULL) {
            free(req->id);
        }
        if (req->title != NULL) {
            free(req->title);
        }
        if (req->body != NULL) {
            free(req->body);
        }
        free(req);
    }
}

/* The tray icon's callback (pump thread): a balloon click hands the last shown
 * notification id to the click slot the EDT drains. */
void cn1WinTrayHandleMessage(WPARAM wParam, LPARAM lParam) {
    (void) wParam;
    if (LOWORD(lParam) == NIN_BALLOONUSERCLICK) {
        EnterCriticalSection(&cn1Win.eventLock);
        if (g_lastNotifyId != NULL) {
            if (g_clickedId != NULL) {
                free(g_clickedId);
            }
            g_clickedId = _strdup(g_lastNotifyId);
        }
        LeaveCriticalSection(&cn1Win.eventLock);
    }
}

/* --------------------------------------------------------- Java bridge */

JAVA_VOID com_codename1_impl_windows_WindowsNative_showNotification___java_lang_String_java_lang_String_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT id, JAVA_OBJECT title, JAVA_OBJECT body) {
    CN1NotifyReq* req;
    if (cn1Win.hwnd == NULL) {
        return; /* headless: no tray */
    }
    req = (CN1NotifyReq*) calloc(1, sizeof(CN1NotifyReq));
    if (req == NULL) {
        return;
    }
    if (id != JAVA_NULL) {
        const char* utf8 = stringToUTF8(threadStateData, id);
        req->id = _strdup(utf8 != NULL ? utf8 : "");
    }
    req->title = title != JAVA_NULL ? cn1WinJavaStringToWide(threadStateData, title, NULL) : NULL;
    req->body = body != JAVA_NULL ? cn1WinJavaStringToWide(threadStateData, body, NULL) : NULL;
    PostMessageW(cn1Win.hwnd, WM_CN1_NOTIFY, (WPARAM) req, 0);
}

/* Returns (and clears) the id of a clicked notification, or null. Polled by the
 * EDT in drainInput so a balloon click reaches the app's LocalNotificationCallback. */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_notificationPollClicked___R_java_lang_String(
        CODENAME_ONE_THREAD_STATE) {
    JAVA_OBJECT result = JAVA_NULL;
    char* id = NULL;
    EnterCriticalSection(&cn1Win.eventLock);
    if (g_clickedId != NULL) {
        id = g_clickedId;
        g_clickedId = NULL;
    }
    LeaveCriticalSection(&cn1Win.eventLock);
    if (id != NULL) {
        result = newStringFromCString(threadStateData, id);
        free(id);
    }
    return result;
}

#endif /* _WIN32 */
