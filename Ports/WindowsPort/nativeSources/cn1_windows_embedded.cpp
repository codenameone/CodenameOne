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
 * The embedded (kiosk) target: `windows.embedded=true`, compiled with
 * CN1_WINDOWS_EMBEDDED. The application is the only thing on the screen of a
 * Windows IoT Enterprise device -- usually as its shell, through Shell
 * Launcher, which `cn1:deploy-device` configures:
 *
 * - one borderless, topmost window covering a whole monitor, no title bar;
 * - the monitor rotated when the panel is mounted that way;
 * - the Windows key, Alt+Tab, Alt+Esc, Ctrl+Esc and Alt+F4 filtered while the
 *   application has the foreground (Ctrl+Alt+Del is the Secure Attention
 *   Sequence and only Keyboard Filter can suppress it; deploy-device turns that
 *   on);
 * - the cursor hidden on a touch panel;
 * - text edited through the shared on-screen keyboard (EmbeddedTextEditor).
 *
 * The board profile's defaults arrive as CN1_DEFAULT_* defines, and each can be
 * overridden on the device by the CN1_* environment variable of the same name,
 * the convention of the embedded Linux target (cn1_linux_embedded.c).
 */
#include "cn1_windows.h"

#ifdef _WIN32

#include <stdio.h>
#include <stdlib.h>

#ifndef CN1_DEFAULT_ROTATION
#define CN1_DEFAULT_ROTATION 0
#endif
#ifndef CN1_DEFAULT_DPI
#define CN1_DEFAULT_DPI 0
#endif
#ifndef CN1_DEFAULT_MONITOR
#define CN1_DEFAULT_MONITOR 0
#endif
/* 1 shows the cursor (a panel with a mouse), 0 hides it (touch only). */
#ifndef CN1_DEFAULT_CURSOR
#define CN1_DEFAULT_CURSOR 0
#endif

extern "C" {

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_isEmbeddedDisplay___R_boolean(CODENAME_ONE_THREAD_STATE) {
#if defined(CN1_WINDOWS_EMBEDDED) && !defined(CN1_WINDOWS_UWP)
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

#if defined(CN1_WINDOWS_EMBEDDED) && !defined(CN1_WINDOWS_UWP)

static int cn1EmbEnvInt(const char* name, int def) {
    char buf[32];
    DWORD n = GetEnvironmentVariableA(name, buf, sizeof(buf));
    if (n == 0 || n >= sizeof(buf)) {
        return def;
    }
    return atoi(buf);
}

typedef struct {
    int wanted;
    int seen;
    HMONITOR found;
} CN1EmbMonitorSearch;

static BOOL CALLBACK cn1EmbMonitorProc(HMONITOR mon, HDC, LPRECT, LPARAM data) {
    CN1EmbMonitorSearch* search = (CN1EmbMonitorSearch*) data;
    if (search->seen == search->wanted) {
        search->found = mon;
        return FALSE;
    }
    search->seen++;
    return TRUE;
}

/* Monitor N in enumeration order; the primary when there is no such monitor. */
static HMONITOR cn1EmbMonitor() {
    CN1EmbMonitorSearch search;
    search.wanted = cn1EmbEnvInt("CN1_MONITOR", CN1_DEFAULT_MONITOR);
    search.seen = 0;
    search.found = NULL;
    if (search.wanted > 0) {
        EnumDisplayMonitors(NULL, NULL, cn1EmbMonitorProc, (LPARAM) &search);
    }
    if (search.found == NULL) {
        POINT origin = {0, 0};
        search.found = MonitorFromPoint(origin, MONITOR_DEFAULTTOPRIMARY);
    }
    return search.found;
}

/* Rotates the monitor for this session. CDS_FULLSCREEN makes the change
 * temporary: Windows restores the panel's own orientation when the process
 * exits, so a crash never leaves the device sideways for whatever runs next. */
static void cn1EmbRotate(HMONITOR mon, int degrees) {
    DWORD orientation;
    switch (degrees) {
        case 90: orientation = DMDO_90; break;
        case 180: orientation = DMDO_180; break;
        case 270: orientation = DMDO_270; break;
        default: return;
    }
    MONITORINFOEXW info;
    ZeroMemory(&info, sizeof(info));
    info.cbSize = sizeof(info);
    if (!GetMonitorInfoW(mon, (MONITORINFO*) &info)) {
        return;
    }
    DEVMODEW dm;
    ZeroMemory(&dm, sizeof(dm));
    dm.dmSize = sizeof(dm);
    if (!EnumDisplaySettingsW(info.szDevice, ENUM_CURRENT_SETTINGS, &dm)) {
        return;
    }
    if (dm.dmDisplayOrientation == orientation) {
        return;
    }
    /* A quarter turn from where the panel is swaps its width and height. */
    if (((dm.dmDisplayOrientation + orientation) & 1) != 0) {
        DWORD w = dm.dmPelsWidth;
        dm.dmPelsWidth = dm.dmPelsHeight;
        dm.dmPelsHeight = w;
    }
    dm.dmDisplayOrientation = orientation;
    dm.dmFields = DM_DISPLAYORIENTATION | DM_PELSWIDTH | DM_PELSHEIGHT;
    LONG r = ChangeDisplaySettingsExW(info.szDevice, &dm, NULL, CDS_FULLSCREEN, NULL);
    if (r != DISP_CHANGE_SUCCESSFUL) {
        cn1WindowsLog("embedded: the display driver refused the rotation");
    }
}

static HWND cn1EmbWindow;
static HHOOK cn1EmbKeyboardHook;

/* Swallows the shell's own shortcuts while the kiosk window has the
 * foreground. A low-level hook sees them before the shell does, which is the
 * only place the Windows key can be stopped from user mode. */
static LRESULT CALLBACK cn1EmbKeyboardProc(int code, WPARAM wParam, LPARAM lParam) {
    if (code == HC_ACTION && GetForegroundWindow() == cn1EmbWindow) {
        const KBDLLHOOKSTRUCT* k = (const KBDLLHOOKSTRUCT*) lParam;
        BOOL alt = (k->flags & LLKHF_ALTDOWN) != 0;
        BOOL ctrl = (GetAsyncKeyState(VK_CONTROL) & 0x8000) != 0;
        if (k->vkCode == VK_LWIN || k->vkCode == VK_RWIN
                || (alt && (k->vkCode == VK_TAB || k->vkCode == VK_ESCAPE || k->vkCode == VK_F4))
                || (ctrl && k->vkCode == VK_ESCAPE)) {
            return 1;
        }
    }
    return CallNextHookEx(cn1EmbKeyboardHook, code, wParam, lParam);
}

int cn1WinEmbeddedActive(void) {
    return 1;
}

void cn1WinEmbeddedPlaceWindow(DWORD* style, DWORD* exStyle, RECT* rc) {
    HMONITOR mon = cn1EmbMonitor();
    cn1EmbRotate(mon, cn1EmbEnvInt("CN1_ROTATION", CN1_DEFAULT_ROTATION));
    MONITORINFO info;
    ZeroMemory(&info, sizeof(info));
    info.cbSize = sizeof(info);
    GetMonitorInfoW(mon, &info);
    /* The whole monitor, not its work area: under Shell Launcher there is no
     * taskbar, and under any other shell the window covers it. */
    *rc = info.rcMonitor;
    *style = WS_POPUP | WS_CLIPCHILDREN;
    *exStyle = WS_EX_TOPMOST;
}

void cn1WinEmbeddedWindowShown(HWND hwnd) {
    cn1EmbWindow = hwnd;
    SetWindowPos(hwnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_SHOWWINDOW);
    SetForegroundWindow(hwnd);
    if (cn1EmbEnvInt("CN1_CURSOR", CN1_DEFAULT_CURSOR) == 0) {
        /* ShowCursor is a per-thread display counter; one decrement on the
         * window's own thread hides it over the window. */
        ShowCursor(FALSE);
    }
    if (cn1EmbKeyboardHook == NULL) {
        cn1EmbKeyboardHook = SetWindowsHookExW(WH_KEYBOARD_LL, cn1EmbKeyboardProc, GetModuleHandleW(NULL), 0);
        if (cn1EmbKeyboardHook == NULL) {
            cn1WindowsLog("embedded: keyboard filter not installed");
        }
    }
}

int cn1WinEmbeddedDpi(void) {
    int dpi = cn1EmbEnvInt("CN1_DPI", CN1_DEFAULT_DPI);
    return dpi > 0 ? dpi : 0;
}

void cn1WinEmbeddedRedirectLog(void) {
    char path[MAX_PATH];
    DWORD n = GetEnvironmentVariableA("CN1_LOG_FILE", path, sizeof(path));
    if (n == 0 || n >= sizeof(path)) {
        return;
    }
    FILE* f = NULL;
    freopen_s(&f, path, "a", stdout);
    f = NULL;
    freopen_s(&f, path, "a", stderr);
    /* Line buffered, so cn1:device-logs follows the application as it runs
     * rather than a buffer at a time. */
    setvbuf(stdout, NULL, _IOLBF, 1024);
    setvbuf(stderr, NULL, _IONBF, 0);
}

#else /* a desktop or UWP build */

int cn1WinEmbeddedActive(void) {
    return 0;
}

void cn1WinEmbeddedPlaceWindow(DWORD* style, DWORD* exStyle, RECT* rc) {
}

void cn1WinEmbeddedWindowShown(HWND hwnd) {
}

int cn1WinEmbeddedDpi(void) {
    return 0;
}

void cn1WinEmbeddedRedirectLog(void) {
}

#endif

} /* extern "C" */

#endif /* _WIN32 */
