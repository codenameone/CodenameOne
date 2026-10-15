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



#include "cn1_windows.h"
#include <dwmapi.h>
static int marginUpdates;
static MARGINS lastMargins;
static HRESULT WINAPI recordFrameMargins(HWND window, const MARGINS* margins) {
    marginUpdates++;
    lastMargins = *margins;
    return DwmExtendFrameIntoClientArea(window, margins);
}
// Observe the real DWM call while retaining its native implementation.
#define DwmExtendFrameIntoClientArea recordFrameMargins
#include "cn1_windows_chrome.cpp"
#undef DwmExtendFrameIntoClientArea
#include <assert.h>
#include <stdio.h>
extern "C" {
CN1WindowsContext cn1Win;
void cn1WinMenuSetIntegrated(BOOL integrated) { (void)integrated; }
struct ThreadLocalData* getThreadLocalData(void) {
    static struct ThreadLocalData state;
    return &state;
}
}
static LRESULT CALLBACK windowProc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp) {
    LRESULT result;
    if (cn1WinChromeMessage(hwnd, msg, wp, lp, &result)) return result;
    return DefWindowProcW(hwnd, msg, wp, lp);
}
static void settle(void) {
    for (int i = 0; i < 30; i++) {
        MSG msg;
        while (PeekMessageW(&msg, NULL, 0, 0, PM_REMOVE)) {
            TranslateMessage(&msg);
            DispatchMessageW(&msg);
        }
        Sleep(5);
    }
}
static LRESULT hit(HWND window, int x, int y) {
    POINT point = {x, y};
    ClientToScreen(window, &point);
    return SendMessageW(window, WM_NCHITTEST, 0, MAKELPARAM(point.x, point.y));
}
int main(void) {
    WNDCLASSW type = {};
    type.lpfnWndProc = windowProc;
    type.hInstance = GetModuleHandleW(NULL);
    type.lpszClassName = L"CN1TitleTest";
    assert(RegisterClassW(&type));
    HWND window = CreateWindowExW(0, type.lpszClassName, L"Integrated title test",
            WS_OVERLAPPEDWINDOW, 80, 80, 720, 420, NULL, NULL, type.hInstance, NULL);
    assert(window);
    cn1Win.hwnd = window;
    ShowWindow(window, SW_SHOW);
    settle();
    int metrics[5] = {};
    SendMessageW(window, WM_CN1_CHROME_METRICS, 0, (LPARAM)metrics);
    BOOL compositorAvailable = cn1ChromeEnabled;
    if (compositorAvailable) {
        assert(metrics[1] > 0 && metrics[4] > 0);
    } else {
        // Wine may expose no compositor. Verify the safe native-frame fallback,
        // then exercise the production hit-test math separately. This is not
        // Windows 11 caption-button/Snap visual acceptance.
        assert(metrics[0] == 0 && metrics[1] == 0 && metrics[4] == 0);
        puts("No DWM compositor: testing fallback and hit routing only");
    }
    assert((GetWindowLongPtrW(window, GWL_STYLE) & WS_OVERLAPPEDWINDOW) == WS_OVERLAPPEDWINDOW);
    int source[] = {0, 0, 700, 50, 20, 12, 80, 28};
    int* regions = (int*)malloc(sizeof(source));
    memcpy(regions, source, sizeof(source));
    SendMessageW(window, WM_CN1_CHROME_REGIONS, 8, (LPARAM)regions);
    cn1ChromeEnabled = TRUE;
    SetWindowPos(window, NULL, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);
    int beforeDpi = marginUpdates;
    RECT suggested;
    GetWindowRect(window, &suggested);
    LRESULT result = 0;
    UINT dpi = cn1ChromeDpi(window);
    // Processing the chrome message must refresh DWM and leave the event for
    // the outer window proc to resize the HWND and publish the Java DPI event.
    assert(!cn1WinChromeMessage(window, WM_DPICHANGED, MAKELONG(dpi, dpi),
            (LPARAM)&suggested, &result));
    assert(marginUpdates == beforeDpi + 1);
    assert(lastMargins.cyTopHeight == cn1ChromeMetric(window, SM_CYCAPTION)
            + cn1ChromeMetric(window, SM_CYSIZEFRAME));
    assert(lastMargins.cxLeftWidth == 0 && lastMargins.cxRightWidth == 0
            && lastMargins.cyBottomHeight == 0);
    assert(hit(window, 30, 25) == HTCLIENT);
    assert(hit(window, 200, 25) == HTCAPTION);
    assert(hit(window, 200, 100) == HTCLIENT);
    assert(hit(window, 200, 1) == HTTOP);
    assert(!cn1ChromeDragPoint(-1, 20));
    assert(!cn1ChromeDragPoint(700, 20));
    assert(!cn1ChromeDragPoint(200, 50));
    SendMessageW(window, WM_SYSCOMMAND, SC_MAXIMIZE, 0);
    settle();
    assert(IsZoomed(window));
    SendMessageW(window, WM_SYSCOMMAND, SC_RESTORE, 0);
    settle();
    assert(!IsZoomed(window));
    SendMessageW(window, WM_SYSCOMMAND, SC_MINIMIZE, 0);
    settle();
    assert(IsIconic(window));
    SendMessageW(window, WM_SYSCOMMAND, SC_RESTORE, 0);
    settle();
    assert(!IsIconic(window));
    SendMessageW(window, WM_CN1_CHROME_REGIONS, 0, 0);
    assert(!cn1ChromeEnabled);
    assert(!cn1ChromeRegions);
    beforeDpi = marginUpdates;
    assert(!cn1WinChromeMessage(window, WM_DPICHANGED, MAKELONG(dpi, dpi),
            (LPARAM)&suggested, &result));
    assert(marginUpdates == beforeDpi); // native-frame mode needs no custom margin
    SendMessageW(window, WM_CLOSE, 0, 0);
    assert(!IsWindow(window));
    puts("WINDOWS_TITLEBAR_OK");
}
