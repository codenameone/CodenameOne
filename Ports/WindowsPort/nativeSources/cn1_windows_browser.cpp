/*
 * BrowserComponent peer for the native Windows port, backed by WebView2.
 *
 * WebView2 is COM/STA: its controller must be created on, and all its methods
 * called from, the thread that pumps the host window's messages -- here the
 * main thread that owns cn1Win.hwnd and runs runMainEventLoop(). The Codename
 * One EDT is a different thread, so the EDT-facing native methods marshal each
 * operation to the main thread by queuing a command on the peer and posting
 * WM_CN1_BROWSER to cn1Win.hwnd; cn1WinWndProc forwards it to cn1WinBrowserDispatch.
 *
 * The screenshot pipeline renders the Form offscreen via Direct2D, so the live
 * WebView2 visual (a separate composition) is not in that target. Instead the
 * peer is drawn from a cached PNG: on NavigationCompleted we CapturePreview the
 * WebView2 to PNG bytes; PeerComponent paints that image (generatePeerImage).
 * onLoad and the JS return-value bridge (a navigation to a /!cn1return/ URL we
 * cancel) are delivered to Java by queuing events the peer polls.
 *
 * The whole WebView2 body is gated on CN1_HAVE_WEBVIEW2 (defined by the CMake
 * build only when the WebView2 SDK is present); without it the natives are
 * stubs so isNativeBrowserComponentSupported() reports false and the build
 * still links.
 */
#ifdef CN1_HAVE_WEBVIEW2
/* Include the WebView2 SDK and the C++ standard library headers BEFORE
 * cn1_windows.h: cn1_globals.h installs macros for the bytecode runtime that
 * otherwise break the STL headers (notably <atomic>, pulled in by <mutex>). */
#include <windows.h>
#include <wrl.h>
#include <shlwapi.h>
#include <deque>
#include <string>
#include <vector>
#include "WebView2.h"
/* Deliberately NOT <mutex>: it pulls <atomic>, which the clean target's
 * clang-cl + MSVC STL combination fails to parse. A Win32 CRITICAL_SECTION
 * (see CN1Browser::lock / CritLock) does the same job without the STL header. */
#endif

#include "cn1_windows.h"

extern "C" {

#ifdef CN1_HAVE_WEBVIEW2

} /* extern "C" -- the C++ WebView2 machinery below is not C-linked */

using namespace Microsoft::WRL;

enum { OP_NAV_HTML = 2, OP_NAV_URL = 3, OP_EXECUTE = 4, OP_BOUNDS = 5, OP_DESTROY = 6 };

struct CritLock {
    CRITICAL_SECTION* cs;
    CritLock(CRITICAL_SECTION* c) : cs(c) { EnterCriticalSection(cs); }
    ~CritLock() { LeaveCriticalSection(cs); }
};

struct CN1Browser {
    ComPtr<ICoreWebView2Controller> controller;
    ComPtr<ICoreWebView2> webview;
    bool ready = false;
    int x = 0, y = 0, w = 1, h = 1;
    CRITICAL_SECTION lock;
    std::deque<std::pair<int, std::wstring>> cmds; // queued ops (run on main thread)
    std::deque<std::string> events;                // UTF-8: "LOAD" | "NAV|<url>"
    std::string png;                               // last CapturePreview PNG bytes
    CN1Browser() { InitializeCriticalSection(&lock); }
    ~CN1Browser() { DeleteCriticalSection(&lock); }
};

static std::wstring utf8ToWide(const char* s) {
    if (s == NULL) return std::wstring();
    int n = MultiByteToWideChar(CP_UTF8, 0, s, -1, NULL, 0);
    if (n <= 0) return std::wstring();
    std::wstring w((size_t) n - 1, L'\0');
    MultiByteToWideChar(CP_UTF8, 0, s, -1, &w[0], n);
    return w;
}
static std::string wideToUtf8(LPCWSTR w) {
    if (w == NULL) return std::string();
    int n = WideCharToMultiByte(CP_UTF8, 0, w, -1, NULL, 0, NULL, NULL);
    if (n <= 0) return std::string();
    std::string s((size_t) n - 1, '\0');
    WideCharToMultiByte(CP_UTF8, 0, w, -1, &s[0], n, NULL, NULL);
    return s;
}

static void cn1BrowserEnqueueEvent(CN1Browser* b, const std::string& ev) {
    CritLock g(&b->lock);
    b->events.push_back(ev);
}

/* Capture the current WebView2 content to PNG and queue a LOAD event. Main thread. */
static void cn1BrowserCapture(CN1Browser* b) {
    if (!b->webview) return;
    ComPtr<IStream> stream;
    if (FAILED(CreateStreamOnHGlobal(NULL, TRUE, &stream))) return;
    b->webview->CapturePreview(COREWEBVIEW2_CAPTURE_PREVIEW_IMAGE_FORMAT_PNG, stream.Get(),
        Callback<ICoreWebView2CapturePreviewCompletedHandler>(
            [b, stream](HRESULT hr) -> HRESULT {
                if (SUCCEEDED(hr)) {
                    HGLOBAL hg = NULL;
                    if (SUCCEEDED(GetHGlobalFromStream(stream.Get(), &hg)) && hg) {
                        SIZE_T sz = GlobalSize(hg);
                        void* p = GlobalLock(hg);
                        if (p && sz > 0) {
                            CritLock g(&b->lock);
                            b->png.assign((const char*) p, (size_t) sz);
                        }
                        if (p) GlobalUnlock(hg);
                    }
                }
                cn1BrowserEnqueueEvent(b, "LOAD");
                return S_OK;
            }).Get());
}

static void cn1BrowserApplyBounds(CN1Browser* b) {
    if (!b->controller) return;
    RECT rc; rc.left = b->x; rc.top = b->y; rc.right = b->x + b->w; rc.bottom = b->y + b->h;
    b->controller->put_Bounds(rc);
    b->controller->put_IsVisible(TRUE);
}

static void cn1BrowserRunCmd(CN1Browser* b, int op, const std::wstring& data) {
    switch (op) {
        case OP_NAV_HTML: if (b->webview) b->webview->NavigateToString(data.c_str()); break;
        case OP_NAV_URL:  if (b->webview) b->webview->Navigate(data.c_str()); break;
        case OP_EXECUTE:  if (b->webview) b->webview->ExecuteScript(data.c_str(), nullptr); break;
        case OP_BOUNDS:   cn1BrowserApplyBounds(b); break;
        case OP_DESTROY:
            if (b->controller) { b->controller->Close(); }
            delete b;
            return;
    }
}

static void cn1BrowserDrain(CN1Browser* b) {
    for (;;) {
        int op; std::wstring data;
        {
            CritLock g(&b->lock);
            if (b->cmds.empty()) return;
            op = b->cmds.front().first; data = b->cmds.front().second; b->cmds.pop_front();
        }
        cn1BrowserRunCmd(b, op, data);
        if (op == OP_DESTROY) return; // b is freed
    }
}

/* Create the WebView2 environment/controller for this peer. Main thread, async. */
static void cn1BrowserCreate(CN1Browser* b) {
    CreateCoreWebView2EnvironmentWithOptions(nullptr, nullptr, nullptr,
        Callback<ICoreWebView2CreateCoreWebView2EnvironmentCompletedHandler>(
            [b](HRESULT r, ICoreWebView2Environment* env) -> HRESULT {
                if (FAILED(r) || !env) { cn1BrowserEnqueueEvent(b, "LOAD"); return S_OK; }
                env->CreateCoreWebView2Controller(cn1Win.hwnd,
                    Callback<ICoreWebView2CreateCoreWebView2ControllerCompletedHandler>(
                        [b](HRESULT r2, ICoreWebView2Controller* ctl) -> HRESULT {
                            if (FAILED(r2) || !ctl) { cn1BrowserEnqueueEvent(b, "LOAD"); return S_OK; }
                            b->controller = ctl;
                            ctl->get_CoreWebView2(&b->webview);
                            cn1BrowserApplyBounds(b);
                            // Intercept the /!cn1return/ bridge navigation: cancel it and
                            // hand the URL to Java (BrowserComponent.fireBrowserNavigationCallbacks).
                            EventRegistrationToken tok;
                            b->webview->add_NavigationStarting(
                                Callback<ICoreWebView2NavigationStartingEventHandler>(
                                    [b](ICoreWebView2* s, ICoreWebView2NavigationStartingEventArgs* a) -> HRESULT {
                                        LPWSTR uri = nullptr;
                                        if (a && SUCCEEDED(a->get_Uri(&uri)) && uri) {
                                            std::string u = wideToUtf8(uri);
                                            CoTaskMemFree(uri);
                                            if (u.find("/!cn1return/") != std::string::npos) {
                                                a->put_Cancel(TRUE);
                                                cn1BrowserEnqueueEvent(b, std::string("NAV|") + u);
                                            }
                                        }
                                        return S_OK;
                                    }).Get(), &tok);
                            b->webview->add_NavigationCompleted(
                                Callback<ICoreWebView2NavigationCompletedEventHandler>(
                                    [b](ICoreWebView2* s, ICoreWebView2NavigationCompletedEventArgs* a) -> HRESULT {
                                        cn1BrowserCapture(b);
                                        return S_OK;
                                    }).Get(), &tok);
                            b->ready = true;
                            cn1BrowserDrain(b); // run any commands queued before we were ready
                            return S_OK;
                        }).Get());
                return S_OK;
            }).Get());
}

extern "C" {

/* Forwarded from cn1WinWndProc on the main thread for WM_CN1_BROWSER.
 * wParam == 1 starts WebView2 creation for the peer; otherwise it drains the
 * peer's queued commands (no-op until the controller is ready -- the create
 * callback drains anything queued before then). */
void cn1WinBrowserHandleMessage(WPARAM wParam, LPARAM lParam) {
    CN1Browser* b = (CN1Browser*) lParam;
    if (b == NULL) return;
    if (wParam == 1) {
        cn1BrowserCreate(b);
    } else if (b->ready) {
        cn1BrowserDrain(b);
    }
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_browserSupported___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return JAVA_TRUE;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_browserCreate___int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT w, JAVA_INT h) {
    CN1Browser* b = new CN1Browser();
    b->w = w > 0 ? w : 1; b->h = h > 0 ? h : 1;
    PostMessageW(cn1Win.hwnd, WM_CN1_BROWSER, 1, (LPARAM) b); // start async creation on the main thread
    return (JAVA_LONG) (intptr_t) b;
}

static void cn1BrowserPost(CN1Browser* b, int op, const std::wstring& data) {
    {
        CritLock g(&b->lock);
        b->cmds.push_back(std::make_pair(op, data));
    }
    PostMessageW(cn1Win.hwnd, WM_CN1_BROWSER, 0, (LPARAM) b);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_browserSetHtml___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT html) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer; if (!b) return;
    cn1BrowserPost(b, OP_NAV_HTML, utf8ToWide(stringToUTF8(threadStateData, html)));
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_browserSetUrl___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT url) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer; if (!b) return;
    cn1BrowserPost(b, OP_NAV_URL, utf8ToWide(stringToUTF8(threadStateData, url)));
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_browserExecute___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT js) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer; if (!b) return;
    cn1BrowserPost(b, OP_EXECUTE, utf8ToWide(stringToUTF8(threadStateData, js)));
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_browserSetBounds___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer; if (!b) return;
    { CritLock g(&b->lock); b->x = x; b->y = y; b->w = w > 0 ? w : 1; b->h = h > 0 ? h : 1; }
    cn1BrowserPost(b, OP_BOUNDS, std::wstring());
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_browserPollEvent___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer; if (!b) return JAVA_NULL;
    std::string ev;
    {
        CritLock g(&b->lock);
        if (b->events.empty()) return JAVA_NULL;
        ev = b->events.front(); b->events.pop_front();
    }
    return newStringFromCString(threadStateData, ev.c_str());
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_browserCapturePng___long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer; if (!b) return JAVA_NULL;
    std::string bytes;
    {
        CritLock g(&b->lock);
        if (b->png.empty()) return JAVA_NULL;
        bytes = b->png;
    }
    JAVA_OBJECT result = allocArray(threadStateData, (int) bytes.size(), &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if (result != JAVA_NULL) {
        memcpy((*(JAVA_ARRAY) result).data, bytes.data(), bytes.size());
    }
    return result;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_browserDestroy___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer; if (!b) return;
    cn1BrowserPost(b, OP_DESTROY, std::wstring());
}

#else /* !CN1_HAVE_WEBVIEW2 -- stubs so the port links without the SDK */

void cn1WinBrowserHandleMessage(WPARAM wParam, LPARAM lParam) {}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_browserSupported___R_boolean(CODENAME_ONE_THREAD_STATE) { return JAVA_FALSE; }
JAVA_LONG com_codename1_impl_windows_WindowsNative_browserCreate___int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT w, JAVA_INT h) { return 0; }
JAVA_VOID com_codename1_impl_windows_WindowsNative_browserSetHtml___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG p, JAVA_OBJECT s) {}
JAVA_VOID com_codename1_impl_windows_WindowsNative_browserSetUrl___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG p, JAVA_OBJECT s) {}
JAVA_VOID com_codename1_impl_windows_WindowsNative_browserExecute___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG p, JAVA_OBJECT s) {}
JAVA_VOID com_codename1_impl_windows_WindowsNative_browserSetBounds___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG p, JAVA_INT a, JAVA_INT b2, JAVA_INT c, JAVA_INT d) {}
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_browserPollEvent___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG p) { return JAVA_NULL; }
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_browserCapturePng___long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG p) { return JAVA_NULL; }
JAVA_VOID com_codename1_impl_windows_WindowsNative_browserDestroy___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG p) {}

#endif /* CN1_HAVE_WEBVIEW2 */

} /* extern "C" */
