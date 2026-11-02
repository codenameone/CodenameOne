/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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


/* Integrated main-window title bar. The window keeps WS_OVERLAPPEDWINDOW:
 * DWM owns the real caption buttons and DefWindowProc owns system commands,
 * resize tracking, caption double-click and the window system menu. */
#if defined(_WIN32) && !defined(CN1_WINDOWS_UWP) /* desktop only: stubbed for UWP in cn1_windows_uwp_stubs.c */
#include "cn1_windows.h"
#include <dwmapi.h>
#include <windowsx.h>
#include <stdlib.h>
#include <string.h>

extern "C" {
static BOOL cn1ChromeEnabled;
static int* cn1ChromeRegions;
static int cn1ChromeRegionCount;

static UINT cn1ChromeDpi(HWND hwnd) {
    typedef UINT (WINAPI *GetDpiFn)(HWND);
    GetDpiFn fn = (GetDpiFn)GetProcAddress(GetModuleHandleW(L"user32.dll"), "GetDpiForWindow");
    return fn ? fn(hwnd) : 96;
}
static int cn1ChromeMetric(HWND hwnd, int metric) {
    typedef int (WINAPI *MetricFn)(int, UINT);
    MetricFn fn = (MetricFn)GetProcAddress(GetModuleHandleW(L"user32.dll"), "GetSystemMetricsForDpi");
    return fn ? fn(metric, cn1ChromeDpi(hwnd)) : GetSystemMetrics(metric);
}
static void cn1ChromeAppearance(HWND hwnd) {
    DWORD light = 1, bytes = sizeof(light);
    RegGetValueW(HKEY_CURRENT_USER,
            L"Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            L"AppsUseLightTheme", RRF_RT_REG_DWORD, NULL, &light, &bytes);
    BOOL dark = light == 0;
    // Windows 10 2004+; older desktop builds use attribute 19.
    if (FAILED(DwmSetWindowAttribute(hwnd, 20, &dark, sizeof(dark))))
        DwmSetWindowAttribute(hwnd, 19, &dark, sizeof(dark));
}
static void cn1ChromeEnable(HWND hwnd, BOOL enable) {
    BOOL composed = FALSE;
    enable = enable && SUCCEEDED(DwmIsCompositionEnabled(&composed)) && composed;
    if (enable == cn1ChromeEnabled) return;
    cn1ChromeEnabled = enable;
    // Keep the native command/accelerator registry, but let the CN1 Toolbar
    // expose menus in the integrated title area instead of overlapping it with
    // Win32's non-client menu row. Native mode restores the ordinary HMENU.
    cn1WinMenuSetIntegrated(enable);
    cn1ChromeAppearance(hwnd);
    MARGINS margins = {0, 0, enable ? cn1ChromeMetric(hwnd, SM_CYCAPTION) + cn1ChromeMetric(hwnd, SM_CYSIZEFRAME) : 0, 0};
    DwmExtendFrameIntoClientArea(hwnd, &margins);
    SetWindowPos(hwnd, NULL, 0, 0, 0, 0,
            SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED);
}
static void cn1ChromeInsets(HWND hwnd, int* result) {
    memset(result, 0, 5 * sizeof(int));
    if (!cn1ChromeEnabled) return;
    int border = cn1ChromeMetric(hwnd, SM_CYSIZEFRAME) + cn1ChromeMetric(hwnd, SM_CXPADDEDBORDER);
    RECT buttons = {0};
    RECT window;
    POINT origin = {0, 0};
    GetWindowRect(hwnd, &window);
    ClientToScreen(hwnd, &origin);
    int reserve = 3 * cn1ChromeMetric(hwnd, SM_CXSIZE);
    int height = cn1ChromeMetric(hwnd, SM_CYCAPTION) + border;
    BOOL rtl = (GetWindowLongPtrW(hwnd, GWL_EXSTYLE) & WS_EX_LAYOUTRTL) != 0;
    if (SUCCEEDED(DwmGetWindowAttribute(hwnd, DWMWA_CAPTION_BUTTON_BOUNDS, &buttons, sizeof(buttons)))
            && buttons.right > buttons.left && buttons.bottom > buttons.top) {
        reserve = rtl ? window.left + buttons.right - origin.x
                : window.right - (window.left + buttons.left);
        height = window.top + buttons.bottom - origin.y;
    }
    result[rtl ? 0 : 1] = reserve + (int)(4 * cn1ChromeDpi(hwnd) / 96);
    result[2] = IsZoomed(hwnd) ? 0 : border;
    result[4] = height > result[2] ? height : result[2] + 1;
}
static BOOL cn1ChromeDragPoint(int x, int y) {
    if (cn1ChromeRegionCount < 4) return FALSE;
    for (int i = 0; i < cn1ChromeRegionCount; i += 4) {
        int* r = cn1ChromeRegions + i;
        BOOL inside = x >= r[0] && y >= r[1] && x - r[0] < r[2] && y - r[1] < r[3];
        if ((i == 0 && !inside) || (i > 0 && inside)) return FALSE;
    }
    return TRUE;
}

/* All state, DWM queries and HWND changes stay on the window-owning thread. */
BOOL cn1WinChromeMessage(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp, LRESULT* result) {
    if (msg == WM_CN1_CHROME_METRICS) {
        cn1ChromeEnable(hwnd, TRUE);
        cn1ChromeInsets(hwnd, (int*)lp);
        *result = 0;
        return TRUE;
    }
    if (msg == WM_CN1_CHROME_REGIONS) {
        free(cn1ChromeRegions);
        cn1ChromeRegions = (int*)lp;
        cn1ChromeRegionCount = (int)wp;
        cn1ChromeEnable(hwnd, wp >= 4);
        *result = 0;
        return TRUE;
    }
    if (msg == WM_NCDESTROY) {
        free(cn1ChromeRegions);
        cn1ChromeRegions = NULL;
        cn1ChromeRegionCount = 0;
        cn1ChromeEnabled = FALSE;
    }
    if (msg == WM_DWMCOMPOSITIONCHANGED) cn1ChromeEnable(hwnd, cn1ChromeRegionCount >= 4);
    if (!cn1ChromeEnabled) return FALSE;
    if (msg == WM_NCCALCSIZE && wp) {
        NCCALCSIZE_PARAMS* size = (NCCALCSIZE_PARAMS*)lp;
        LONG top = size->rgrc[0].top;
        DefWindowProcW(hwnd, msg, wp, lp); // retain side/bottom resizing borders
        int border = cn1ChromeMetric(hwnd, SM_CYSIZEFRAME) + cn1ChromeMetric(hwnd, SM_CXPADDEDBORDER);
        size->rgrc[0].top = top + (IsZoomed(hwnd) ? border : 0);
        *result = 0;
        return TRUE;
    }
    // Native caption buttons must see non-client messages before app hit testing.
    if (DwmDefWindowProc(hwnd, msg, wp, lp, result)) return TRUE;
    if (msg == WM_NCHITTEST) {
        LRESULT hit = DefWindowProcW(hwnd, msg, wp, lp);
        if ((hit >= HTLEFT && hit <= HTBOTTOMRIGHT) || hit == HTCLOSE
                || hit == HTMINBUTTON || hit == HTMAXBUTTON) {
            *result = hit;
            return TRUE;
        }
        POINT p = {GET_X_LPARAM(lp), GET_Y_LPARAM(lp)};
        ScreenToClient(hwnd, &p);
        int insets[5];
        cn1ChromeInsets(hwnd, insets);
        if (!IsZoomed(hwnd) && p.y >= 0 && p.y < insets[2]) {
            RECT client;
            GetClientRect(hwnd, &client);
            int corner = cn1ChromeMetric(hwnd, SM_CXSIZEFRAME) + cn1ChromeMetric(hwnd, SM_CXPADDEDBORDER);
            *result = p.x < corner ? HTTOPLEFT : p.x >= client.right - corner ? HTTOPRIGHT : HTTOP;
        } else {
            *result = cn1ChromeDragPoint(p.x, p.y) ? HTCAPTION : HTCLIENT;
        }
        return TRUE;
    }
    if (msg == WM_SETTINGCHANGE || msg == WM_THEMECHANGED) cn1ChromeAppearance(hwnd);
    // DWM margins are physical pixels and must follow the new monitor DPI too.
    if (msg == WM_ACTIVATE || msg == WM_DWMCOMPOSITIONCHANGED || msg == WM_DPICHANGED) {
        MARGINS margins = {0, 0, cn1ChromeMetric(hwnd, SM_CYCAPTION) + cn1ChromeMetric(hwnd, SM_CYSIZEFRAME), 0};
        DwmExtendFrameIntoClientArea(hwnd, &margins);
    }
    return FALSE;
}
JAVA_VOID com_codename1_impl_windows_WindowsNative_mainWindowTitleBarInsets___int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) {
    if (array == JAVA_NULL || ((JAVA_ARRAY)array)->length < 5 || !cn1Win.hwnd) return;
    int result[5] = {0};
    CN1_YIELD_THREAD;
    SendMessageW(cn1Win.hwnd, WM_CN1_CHROME_METRICS, 0, (LPARAM)result);
    CN1_RESUME_THREAD;
    memcpy(CN1_ARRAY_DATA(array), result, sizeof(result));
}
JAVA_VOID com_codename1_impl_windows_WindowsNative_mainWindowTitleBarRegions___int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) {
    int count = array == JAVA_NULL ? 0 : ((JAVA_ARRAY)array)->length;
    if (count % 4 != 0 || !cn1Win.hwnd) return;
    int* copy = count ? (int*)malloc((size_t)count * sizeof(int)) : NULL;
    if (count && !copy) return;
    if (count) memcpy(copy, CN1_ARRAY_DATA(array), (size_t)count * sizeof(int));
    if (!PostMessageW(cn1Win.hwnd, WM_CN1_CHROME_REGIONS, count, (LPARAM)copy)) free(copy);
}
}
#endif
