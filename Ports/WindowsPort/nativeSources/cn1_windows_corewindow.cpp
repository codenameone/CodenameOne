/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
 * The UWP host of the Windows port: what cn1_windows_window.cpp is for a Win32
 * window, this is for a CoreWindow, which is the only kind of window the app
 * container on an Xbox in Developer Mode will give an application. Compiled only
 * with CN1_WINDOWS_UWP (build hint windows.xbox=true).
 *
 * Startup. The process cannot begin in the translated main: its first thread
 * must go to CoreApplication::Run, which creates the window and then calls
 * IFrameworkView::Run back on that same thread. The translator renames the Java
 * main to cn1JavaMain in this build (ByteCodeClass), main below starts
 * CoreApplication, and Run calls cn1JavaMain. From there the threading model is
 * the desktop one unchanged: this thread runs Display.init and then owns the
 * event pump (pumpMessages -> CoreDispatcher::ProcessEvents); the CoreWindow
 * handlers push into the shared input ring; the EDT drains it and paints.
 *
 * Rendering. Direct2D draws through a device context on a D3D11 device rather
 * than an HWND render target. Codename One repaints only dirty regions and
 * relies on the rest of the surface surviving a present, which a flip-model
 * swap chain does not do, so the EDT draws into a persistent target bitmap and
 * each flush copies it to the back buffer and presents. Mutable images keep
 * using WIC render targets from the same (multi-threaded) factory.
 *
 * Input. Pointer events arrive in view pixels and are scaled to physical ones.
 * Keys arrive as virtual keys, and on an Xbox that includes the controller:
 * a CoreWindow app is not in mouse mode, so the D-pad, the sticks and A/B/X/Y
 * come through KeyDown as VirtualKey_Gamepad*. WindowsImplementation maps those
 * to game actions.
 */

#if defined(_WIN32) && defined(CN1_WINDOWS_UWP)

#include "cn1_windows.h"
#include <d3d11.h>
#include <dxgi1_2.h>
#include <d2d1_1.h>
#include <roapi.h>
#include <wrl.h>
#include <wrl/event.h>
#include <wrl/wrappers/corewrappers.h>
#include <windows.foundation.h>
#include <windows.applicationmodel.core.h>
#include <windows.ui.core.h>
#include <windows.ui.input.h>
#include <windows.devices.input.h>
#include <windows.graphics.display.h>
#include <windows.ui.viewmanagement.h>
#include <windows.storage.h>
#include <string.h>
#include <math.h>

using namespace Microsoft::WRL;
using namespace Microsoft::WRL::Wrappers;
using namespace ABI::Windows::Foundation;
using namespace ABI::Windows::ApplicationModel::Core;
using namespace ABI::Windows::ApplicationModel::Activation;
using namespace ABI::Windows::UI::Core;

extern "C" int cn1JavaMain(int argc, char* argv[]);

/* ------------------------------------------------------------------ state */

static int g_argc;
static char** g_argv;
static ComPtr<ICoreWindow> g_window;
static ComPtr<ICoreDispatcher> g_dispatcher;
static volatile LONG g_closed;

/* Physical pixels per view pixel (2.0 on a 1080p Xbox output). Written on the
 * UI thread before the EDT exists and on DPI change; read by input handlers on
 * the same thread. */
static float g_scale = 1.0f;

static ComPtr<ID3D11Device> g_d3d;
static ComPtr<IDXGISwapChain1> g_swapChain;
static ComPtr<ID2D1DeviceContext> g_dc;
static ComPtr<ID2D1Bitmap1> g_canvas;   /* what the EDT draws into; survives presents */
static ComPtr<ID2D1Bitmap1> g_backBuffer;

static float cn1UwpScale(void) {
    ComPtr<ABI::Windows::Graphics::Display::IDisplayInformationStatics> statics;
    ComPtr<ABI::Windows::Graphics::Display::IDisplayInformation> info;
    ComPtr<ABI::Windows::Graphics::Display::IDisplayInformation2> info2;
    DOUBLE raw = 1.0;
    if (SUCCEEDED(GetActivationFactory(HStringReference(
                RuntimeClass_Windows_Graphics_Display_DisplayInformation).Get(), &statics))
            && SUCCEEDED(statics->GetForCurrentView(&info))
            && SUCCEEDED(info.As(&info2))
            && SUCCEEDED(info2->get_RawPixelsPerViewPixel(&raw)) && raw > 0) {
        return (float) raw;
    }
    return 1.0f;
}

static int cn1UwpToPixels(float viewPixels) {
    return (int) floorf(viewPixels * g_scale + 0.5f);
}

/* The window's size in physical pixels. */
static void cn1UwpWindowPixels(int* w, int* h) {
    Rect bounds = {};
    if (g_window) {
        g_window->get_Bounds(&bounds);
    }
    *w = cn1UwpToPixels(bounds.Width);
    *h = cn1UwpToPixels(bounds.Height);
    if (*w < 1) *w = 1;
    if (*h < 1) *h = 1;
}

/* ------------------------------------------------------------ rendering */

static HRESULT cn1UwpBindBackBuffer(void) {
    ComPtr<IDXGISurface> surface;
    HRESULT hr = g_swapChain->GetBuffer(0, IID_PPV_ARGS(&surface));
    if (FAILED(hr)) {
        return hr;
    }
    D2D1_BITMAP_PROPERTIES1 props = D2D1::BitmapProperties1(
            D2D1_BITMAP_OPTIONS_TARGET | D2D1_BITMAP_OPTIONS_CANNOT_DRAW,
            D2D1::PixelFormat(DXGI_FORMAT_B8G8R8A8_UNORM, D2D1_ALPHA_MODE_IGNORE), 96.0f, 96.0f);
    return g_dc->CreateBitmapFromDxgiSurface(surface.Get(), &props, &g_backBuffer);
}

static HRESULT cn1UwpCreateCanvas(int w, int h) {
    D2D1_BITMAP_PROPERTIES1 props = D2D1::BitmapProperties1(D2D1_BITMAP_OPTIONS_TARGET,
            D2D1::PixelFormat(DXGI_FORMAT_B8G8R8A8_UNORM, D2D1_ALPHA_MODE_PREMULTIPLIED), 96.0f, 96.0f);
    g_canvas.Reset();
    HRESULT hr = g_dc->CreateBitmap(D2D1::SizeU((UINT32) w, (UINT32) h), NULL, 0, &props, &g_canvas);
    if (SUCCEEDED(hr)) {
        g_dc->SetTarget(g_canvas.Get());
    }
    return hr;
}

static int cn1UwpCreateDevice(int w, int h) {
    static const D3D_FEATURE_LEVEL levels[] = {
        D3D_FEATURE_LEVEL_11_1, D3D_FEATURE_LEVEL_11_0, D3D_FEATURE_LEVEL_10_1,
        D3D_FEATURE_LEVEL_10_0, D3D_FEATURE_LEVEL_9_3, D3D_FEATURE_LEVEL_9_1
    };
    HRESULT hr = D3D11CreateDevice(NULL, D3D_DRIVER_TYPE_HARDWARE, NULL, D3D11_CREATE_DEVICE_BGRA_SUPPORT,
            levels, ARRAYSIZE(levels), D3D11_SDK_VERSION, &g_d3d, NULL, NULL);
    if (FAILED(hr)) {
        hr = D3D11CreateDevice(NULL, D3D_DRIVER_TYPE_WARP, NULL, D3D11_CREATE_DEVICE_BGRA_SUPPORT,
                levels, ARRAYSIZE(levels), D3D11_SDK_VERSION, &g_d3d, NULL, NULL);
    }
    if (FAILED(hr)) {
        cn1WindowsLog("initDisplay(uwp): no D3D11 device");
        return 0;
    }
    ComPtr<IDXGIDevice> dxgiDevice;
    ComPtr<IDXGIAdapter> adapter;
    ComPtr<IDXGIFactory2> dxgiFactory;
    if (FAILED(g_d3d.As(&dxgiDevice)) || FAILED(dxgiDevice->GetAdapter(&adapter))
            || FAILED(adapter->GetParent(IID_PPV_ARGS(&dxgiFactory)))) {
        cn1WindowsLog("initDisplay(uwp): no DXGI factory");
        return 0;
    }

    /* Multi-threaded, as on the desktop: this thread owns the window, the EDT
     * draws. The factory is kept as the ID2D1Factory every other file uses. */
    ComPtr<ID2D1Factory1> factory;
    D2D1_FACTORY_OPTIONS options = {};
    if (FAILED(D2D1CreateFactory(D2D1_FACTORY_TYPE_MULTI_THREADED, __uuidof(ID2D1Factory1), &options,
            (void**) factory.GetAddressOf()))) {
        cn1WindowsLog("initDisplay(uwp): no Direct2D factory");
        return 0;
    }
    ComPtr<ID2D1Device> d2dDevice;
    if (FAILED(factory->CreateDevice(dxgiDevice.Get(), &d2dDevice))
            || FAILED(d2dDevice->CreateDeviceContext(D2D1_DEVICE_CONTEXT_OPTIONS_NONE, &g_dc))) {
        cn1WindowsLog("initDisplay(uwp): no Direct2D device context");
        return 0;
    }
    /* 96 DPI: one Direct2D unit is one pixel, which is what Codename One draws in. */
    g_dc->SetDpi(96.0f, 96.0f);
    cn1Win.d2dFactory = factory.Detach();

    DXGI_SWAP_CHAIN_DESC1 desc = {};
    desc.Width = (UINT) w;
    desc.Height = (UINT) h;
    desc.Format = DXGI_FORMAT_B8G8R8A8_UNORM;
    desc.SampleDesc.Count = 1;
    desc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    desc.BufferCount = 2;
    desc.SwapEffect = DXGI_SWAP_EFFECT_FLIP_SEQUENTIAL;
    desc.Scaling = DXGI_SCALING_STRETCH;
    desc.AlphaMode = DXGI_ALPHA_MODE_IGNORE;
    ComPtr<IUnknown> windowUnknown;
    g_window.As(&windowUnknown);
    if (FAILED(dxgiFactory->CreateSwapChainForCoreWindow(g_d3d.Get(), windowUnknown.Get(), &desc, NULL,
            &g_swapChain))) {
        cn1WindowsLog("initDisplay(uwp): CreateSwapChainForCoreWindow failed");
        return 0;
    }
    if (FAILED(cn1UwpBindBackBuffer()) || FAILED(cn1UwpCreateCanvas(w, h))) {
        cn1WindowsLog("initDisplay(uwp): could not bind the swap chain");
        return 0;
    }
    return 1;
}

/* Copies the canvas to the back buffer and presents. EDT, outside a frame. */
static void cn1UwpPresent(void) {
    if (!g_swapChain || !g_canvas || !g_backBuffer) {
        return;
    }
    g_dc->SetTarget(g_backBuffer.Get());
    g_dc->BeginDraw();
    g_dc->SetTransform(D2D1::Matrix3x2F::Identity());
    g_dc->DrawImage(g_canvas.Get(), D2D1_INTERPOLATION_MODE_NEAREST_NEIGHBOR, D2D1_COMPOSITE_MODE_SOURCE_COPY);
    g_dc->EndDraw();
    g_dc->SetTarget(g_canvas.Get());
    g_swapChain->Present(1, 0);
}

/* ------------------------------------------------------------- input */

static int cn1UwpButtons(ABI::Windows::UI::Input::IPointerPoint* point, int* touchFlag, boolean* inContact) {
    using namespace ABI::Windows::Devices::Input;
    int mask = 0;
    *touchFlag = 0;
    *inContact = 0;
    point->get_IsInContact(inContact);
    ComPtr<IPointerDevice> device;
    PointerDeviceType type = PointerDeviceType_Mouse;
    if (SUCCEEDED(point->get_PointerDevice(&device)) && device) {
        device->get_PointerDeviceType(&type);
    }
    if (type == PointerDeviceType_Touch) {
        *touchFlag = CN1_PE_TOUCH_FLAG;
    } else if (type == PointerDeviceType_Pen) {
        *touchFlag = CN1_PE_PEN_FLAG;
    }
    ComPtr<ABI::Windows::UI::Input::IPointerPointProperties> props;
    if (SUCCEEDED(point->get_Properties(&props)) && props) {
        boolean b = 0;
        if (SUCCEEDED(props->get_IsLeftButtonPressed(&b)) && b) mask |= CN1_PE_MASK_PRIMARY;
        if (SUCCEEDED(props->get_IsRightButtonPressed(&b)) && b) mask |= CN1_PE_MASK_SECONDARY;
        if (SUCCEEDED(props->get_IsMiddleButtonPressed(&b)) && b) mask |= CN1_PE_MASK_MIDDLE;
    }
    if (mask == 0 && *inContact) {
        mask = CN1_PE_MASK_PRIMARY;   /* a finger or a pen tip */
    }
    return mask;
}

typedef ITypedEventHandler<CoreWindow*, PointerEventArgs*> PointerHandler;

static HRESULT cn1UwpOnPointer(IPointerEventArgs* args, CN1EventType kind) {
    ComPtr<ABI::Windows::UI::Input::IPointerPoint> point;
    if (FAILED(args->get_CurrentPoint(&point)) || !point) {
        return S_OK;
    }
    Point p = {};
    point->get_Position(&p);
    int x = cn1UwpToPixels(p.X);
    int y = cn1UwpToPixels(p.Y);
    int touch = 0;
    boolean inContact = 0;
    int mask = cn1UwpButtons(point.Get(), &touch, &inContact);
    if (kind == CN1_EVENT_POINTER_RELEASED) {
        /* The released button is no longer reported as pressed. */
        cn1WinPushEvent(CN1_EVENT_POINTER_RELEASED, x, y, CN1_PE_MASK_PRIMARY | touch);
    } else if (kind == CN1_EVENT_POINTER_PRESSED) {
        cn1WinPushEvent(CN1_EVENT_POINTER_PRESSED, x, y, (mask != 0 ? mask : CN1_PE_MASK_PRIMARY) | touch);
    } else if (mask != 0) {
        cn1WinPushEvent(CN1_EVENT_POINTER_DRAGGED, x, y, mask | touch);
    } else {
        cn1WinPushEvent(CN1_EVENT_POINTER_HOVER, x, y, touch);
    }
    return S_OK;
}

static HRESULT cn1UwpOnWheel(IPointerEventArgs* args) {
    ComPtr<ABI::Windows::UI::Input::IPointerPoint> point;
    ComPtr<ABI::Windows::UI::Input::IPointerPointProperties> props;
    if (FAILED(args->get_CurrentPoint(&point)) || !point || FAILED(point->get_Properties(&props)) || !props) {
        return S_OK;
    }
    Point p = {};
    point->get_Position(&p);
    INT32 delta = 0;
    boolean horizontal = 0;
    props->get_MouseWheelDelta(&delta);
    props->get_IsHorizontalMouseWheel(&horizontal);
    cn1WinPushEvent(horizontal ? CN1_EVENT_MOUSE_HWHEEL : CN1_EVENT_MOUSE_WHEEL,
            cn1UwpToPixels(p.X), cn1UwpToPixels(p.Y), (int) delta);
    return S_OK;
}

/* The TV-safe insets in pixels (left, top, right, bottom). ApplicationView and
 * the window bounds answer only on the view thread, while the EDT asks during
 * layout, so the view thread computes them (initDisplay, SizeChanged) and the
 * native only copies these out. */
static volatile LONG g_safeInsets[4];
static volatile LONG g_safeKnown;

static void cn1UwpUpdateSafeArea(void) {
    using namespace ABI::Windows::UI::ViewManagement;
    ComPtr<IApplicationViewStatics2> statics;
    ComPtr<IApplicationView> view;
    ComPtr<IApplicationView2> view2;
    Rect visible = {};
    Rect bounds = {};
    if (!g_window || FAILED(GetActivationFactory(
            HStringReference(RuntimeClass_Windows_UI_ViewManagement_ApplicationView).Get(), &statics))
            || FAILED(statics->GetForCurrentView(&view)) || FAILED(view.As(&view2))
            || FAILED(view2->get_VisibleBounds(&visible)) || FAILED(g_window->get_Bounds(&bounds))) {
        return;
    }
    int edge[4];
    edge[0] = cn1UwpToPixels(visible.X - bounds.X);
    edge[1] = cn1UwpToPixels(visible.Y - bounds.Y);
    edge[2] = cn1UwpToPixels((bounds.X + bounds.Width) - (visible.X + visible.Width));
    edge[3] = cn1UwpToPixels((bounds.Y + bounds.Height) - (visible.Y + visible.Height));
    for (int i = 0; i < 4; i++) {
        InterlockedExchange(&g_safeInsets[i], edge[i] > 0 ? edge[i] : 0);
    }
    InterlockedExchange(&g_safeKnown, 1);
}

/* Handled lives on ICoreWindowEventArgs, which every key event also implements. */
static void cn1UwpMarkHandled(IKeyEventArgs* a) {
    ComPtr<ICoreWindowEventArgs> base;
    if (SUCCEEDED(a->QueryInterface(IID_PPV_ARGS(&base)))) {
        base->put_Handled(TRUE);
    }
}

static void cn1UwpRegisterInput(void) {
    EventRegistrationToken token;
    g_window->add_PointerPressed(Callback<PointerHandler>([](ICoreWindow*, IPointerEventArgs* a) {
        return cn1UwpOnPointer(a, CN1_EVENT_POINTER_PRESSED);
    }).Get(), &token);
    g_window->add_PointerReleased(Callback<PointerHandler>([](ICoreWindow*, IPointerEventArgs* a) {
        return cn1UwpOnPointer(a, CN1_EVENT_POINTER_RELEASED);
    }).Get(), &token);
    g_window->add_PointerMoved(Callback<PointerHandler>([](ICoreWindow*, IPointerEventArgs* a) {
        return cn1UwpOnPointer(a, CN1_EVENT_POINTER_DRAGGED);
    }).Get(), &token);
    g_window->add_PointerWheelChanged(Callback<PointerHandler>([](ICoreWindow*, IPointerEventArgs* a) {
        return cn1UwpOnWheel(a);
    }).Get(), &token);

    typedef ITypedEventHandler<CoreWindow*, KeyEventArgs*> KeyHandler;
    /* Handled = true: otherwise the shell treats Gamepad B as "go back" and the
     * D-pad as its own focus navigation, on top of the application's. */
    g_window->add_KeyDown(Callback<KeyHandler>([](ICoreWindow*, IKeyEventArgs* a) {
        ABI::Windows::System::VirtualKey key = ABI::Windows::System::VirtualKey_None;
        a->get_VirtualKey(&key);
        cn1UwpMarkHandled(a);
        cn1WinPushEvent(CN1_EVENT_KEY_PRESSED, 0, 0, (int) key);
        return S_OK;
    }).Get(), &token);
    g_window->add_KeyUp(Callback<KeyHandler>([](ICoreWindow*, IKeyEventArgs* a) {
        ABI::Windows::System::VirtualKey key = ABI::Windows::System::VirtualKey_None;
        a->get_VirtualKey(&key);
        cn1UwpMarkHandled(a);
        cn1WinPushEvent(CN1_EVENT_KEY_RELEASED, 0, 0, (int) key);
        return S_OK;
    }).Get(), &token);

    typedef ITypedEventHandler<CoreWindow*, WindowSizeChangedEventArgs*> SizeHandler;
    g_window->add_SizeChanged(Callback<SizeHandler>([](ICoreWindow*, IWindowSizeChangedEventArgs*) {
        int w, h;
        g_scale = cn1UwpScale();
        cn1UwpWindowPixels(&w, &h);
        cn1UwpUpdateSafeArea();
        if (w == cn1Win.width && h == cn1Win.height) {
            return S_OK;
        }
        cn1Win.width = w;
        cn1Win.height = h;
        /* Applied by the EDT before its next frame (cn1WinApplyPendingResize),
         * never here: the EDT may be mid-frame on the canvas. */
        cn1Win.pendingW = w;
        cn1Win.pendingH = h;
        InterlockedExchange(&cn1Win.pendingResize, 1);
        cn1WinPushEvent(CN1_EVENT_SIZE_CHANGED, w, h, 0);
        return S_OK;
    }).Get(), &token);

    typedef ITypedEventHandler<CoreWindow*, CoreWindowEventArgs*> ClosedHandler;
    g_window->add_Closed(Callback<ClosedHandler>([](ICoreWindow*, ICoreWindowEventArgs*) {
        InterlockedExchange(&g_closed, 1);
        cn1WinPushEvent(CN1_EVENT_CLOSE, 0, 0, 0);
        return S_OK;
    }).Get(), &token);
}

/* ------------------------------------------------- CoreApplication view */

class Cn1View : public RuntimeClass<IFrameworkView> {
public:
    IFACEMETHODIMP Initialize(ICoreApplicationView* view) override {
        EventRegistrationToken token;
        typedef ITypedEventHandler<CoreApplicationView*, IActivatedEventArgs*> ActivatedHandler;
        return view->add_Activated(Callback<ActivatedHandler>([](ICoreApplicationView*, IActivatedEventArgs*) {
            /* Until the window is activated the console keeps showing the splash. */
            if (g_window) {
                g_window->Activate();
            }
            return S_OK;
        }).Get(), &token);
    }

    IFACEMETHODIMP SetWindow(ICoreWindow* window) override {
        g_window = window;
        g_window->get_Dispatcher(&g_dispatcher);
        return S_OK;
    }

    IFACEMETHODIMP Load(HSTRING) override {
        return S_OK;
    }

    IFACEMETHODIMP Run() override {
        /* The Java program, on the window's own thread; it returns when the
         * application exits. */
        cn1JavaMain(g_argc, g_argv);
        return S_OK;
    }

    IFACEMETHODIMP Uninitialize() override {
        return S_OK;
    }
};

class Cn1ViewSource : public RuntimeClass<IFrameworkViewSource> {
public:
    IFACEMETHODIMP CreateView(IFrameworkView** view) override {
        return Make<Cn1View>().CopyTo(view);
    }
};

int main(int argc, char* argv[]) {
    g_argc = argc;
    g_argv = argv;
    RoInitializeWrapper init(RO_INIT_MULTITHREADED);
    ComPtr<ICoreApplication> app;
    if (FAILED(GetActivationFactory(HStringReference(
            RuntimeClass_Windows_ApplicationModel_Core_CoreApplication).Get(), &app))) {
        return 1;
    }
    ComPtr<Cn1ViewSource> source = Make<Cn1ViewSource>();
    return FAILED(app->Run(source.Get())) ? 1 : 0;
}

/* --------------------------------------------------------------- bridge */

extern "C" {

int cn1UwpLocalFolder(WCHAR* buf, int capacity) {
    using namespace ABI::Windows::Storage;
    ComPtr<IApplicationDataStatics> statics;
    ComPtr<IApplicationData> data;
    ComPtr<IStorageFolder> folder;
    ComPtr<IStorageItem> item;
    HString path;
    if (buf == NULL || capacity <= 0
            || FAILED(GetActivationFactory(HStringReference(RuntimeClass_Windows_Storage_ApplicationData).Get(),
                    &statics))
            || FAILED(statics->get_Current(&data)) || FAILED(data->get_LocalFolder(&folder))
            || FAILED(folder.As(&item)) || FAILED(item->get_Path(path.GetAddressOf()))) {
        return 0;
    }
    UINT32 len = 0;
    const wchar_t* raw = path.GetRawBuffer(&len);
    if (raw == NULL || (int) len >= capacity) {
        return 0;
    }
    memcpy(buf, raw, len * sizeof(WCHAR));
    buf[len] = L'\0';
    return 1;
}

void cn1WinApplyPendingResize(void) {
    if (!InterlockedExchange(&cn1Win.pendingResize, 0) || !g_swapChain) {
        return;
    }
    int w = (int) cn1Win.pendingW;
    int h = (int) cn1Win.pendingH;
    g_dc->SetTarget(NULL);
    g_backBuffer.Reset();
    if (FAILED(g_swapChain->ResizeBuffers(2, (UINT) w, (UINT) h, DXGI_FORMAT_B8G8R8A8_UNORM, 0))
            || FAILED(cn1UwpBindBackBuffer()) || FAILED(cn1UwpCreateCanvas(w, h))) {
        cn1WindowsLog("uwp: resize failed");
    }
}

extern "C" JAVA_VOID com_codename1_impl_windows_WindowsNative_mainWindowSetTitle___java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1);

JAVA_VOID com_codename1_impl_windows_WindowsNative_initDisplay___java_lang_String_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    (void) __cn1Arg2;
    (void) __cn1Arg3;   /* the window is the whole screen; its size is not ours to pick */
    if (!cn1WinInitRuntime()) {
        return;
    }
    if (!g_window) {
        cn1WindowsLog("initDisplay(uwp): no CoreWindow -- not started through main?");
        return;
    }
    g_scale = cn1UwpScale();
    int w, h;
    cn1UwpWindowPixels(&w, &h);
    cn1Win.width = w;
    cn1Win.height = h;
    if (!cn1UwpCreateDevice(w, h)) {
        return;
    }
    cn1Win.windowGraphics = cn1WinCreateGraphics((ID2D1RenderTarget*) g_dc.Get());
    if (cn1Win.windowGraphics != NULL) {
        cn1Win.windowGraphics->isWindowTarget = JAVA_TRUE;
    }
    CoCreateInstance(CLSID_WICImagingFactory, NULL, CLSCTX_INPROC_SERVER,
            IID_IWICImagingFactory, (void**) &cn1Win.wicFactory);
    cn1UwpRegisterInput();
    if (__cn1Arg1 != JAVA_NULL) {
        com_codename1_impl_windows_WindowsNative_mainWindowSetTitle___java_lang_String(threadStateData, __cn1Arg1);
    }
    g_window->Activate();
    cn1UwpUpdateSafeArea();
    cn1WindowsLog("initDisplay(uwp): CoreWindow swap chain created");
}

/* Sets the title on the view thread, the only one ApplicationView answers on. */
static void cn1UwpApplyTitle(const WCHAR* title) {
    using namespace ABI::Windows::UI::ViewManagement;
    ComPtr<IApplicationViewStatics2> statics;
    ComPtr<IApplicationView> view;
    if (SUCCEEDED(GetActivationFactory(HStringReference(RuntimeClass_Windows_UI_ViewManagement_ApplicationView).Get(),
            &statics)) && SUCCEEDED(statics->GetForCurrentView(&view))) {
        view->put_Title(HStringReference(title != NULL ? title : L"").Get());
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_mainWindowSetTitle___java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    if (!g_dispatcher) {
        return;
    }
    UINT32 len = 0;
    WCHAR* title = __cn1Arg1 != JAVA_NULL ? cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len) : NULL;
    boolean onViewThread = 0;
    if (SUCCEEDED(g_dispatcher->get_HasThreadAccess(&onViewThread)) && onViewThread) {
        cn1UwpApplyTitle(title);
        free(title);
        return;
    }
    /* From the EDT: RunAsync is callable from any thread, runs the handler on
     * the view thread and wakes the pump that is blocked in ProcessEvents. The
     * handler owns the string. */
    typedef ABI::Windows::UI::Core::IDispatchedHandler DispatchedHandler;
    ComPtr<ABI::Windows::Foundation::IAsyncAction> action;
    HRESULT hr = g_dispatcher->RunAsync(ABI::Windows::UI::Core::CoreDispatcherPriority_Normal,
            Callback<DispatchedHandler>([title]() {
                cn1UwpApplyTitle(title);
                free(title);
                return S_OK;
            }).Get(), &action);
    if (FAILED(hr)) {
        free(title);
    }
}

JAVA_INT com_codename1_impl_windows_WindowsNative_getDisplayWidth___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1Win.width;
}

/* The logical DPI: 96 times the view scale. On a television that is what makes
 * millimetre-sized theme metrics readable from the couch; the raw DPI of a 55"
 * panel would size everything for a phone held at arm's length. */
JAVA_INT com_codename1_impl_windows_WindowsNative_screenDpi___R_int(CODENAME_ONE_THREAD_STATE) {
    return (JAVA_INT) floorf(96.0f * g_scale + 0.5f);
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_isTouchDevice___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_isHighContrastEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_isReduceMotionEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_isScreenReaderEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return JAVA_FALSE;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_flushGraphics___long_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3,
        JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5) {
    CN1Graphics* g = (CN1Graphics*) (intptr_t) __cn1Arg1;
    if (g != NULL) {
        cn1WinEndFrame(g);
        if (g == cn1Win.windowGraphics) {
            cn1UwpPresent();
        }
    }
    InterlockedIncrement(&cn1Win.flushGen);
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_pumpMessages___R_boolean(CODENAME_ONE_THREAD_STATE) {
    /* Same contract as the Win32 pump: block for input, dispatch the burst,
     * return so the caller drains the ring; false once the window is gone. The
     * handlers only push into the ring and never touch the Java heap, so the
     * thread stays yielded through the whole dispatch. */
    if (!g_dispatcher || g_closed) {
        return JAVA_FALSE;
    }
    CN1_YIELD_THREAD;
    HRESULT hr = g_dispatcher->ProcessEvents(CoreProcessEventsOption_ProcessOneAndAllPending);
    CN1_RESUME_THREAD;
    return (FAILED(hr) || g_closed) ? JAVA_FALSE : JAVA_TRUE;
}

/* The TV-safe area, in pixels from each edge: what ApplicationView reports as
 * visible inside the CoreWindow. A television may crop the outer few percent of
 * the picture, so an Xbox reports an inset there; anything else reports none. */
JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_safeAreaInsets___int_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    if (__cn1Arg1 == JAVA_NULL || (*(JAVA_ARRAY) __cn1Arg1).length < 4 || !g_safeKnown) {
        return JAVA_FALSE;
    }
    JAVA_ARRAY_INT* out = (JAVA_ARRAY_INT*) CN1_ARRAY_DATA(__cn1Arg1);
    for (int i = 0; i < 4; i++) {
        out[i] = (JAVA_ARRAY_INT) g_safeInsets[i];
    }
    return JAVA_TRUE;
}

} /* extern "C" */

#endif /* _WIN32 && CN1_WINDOWS_UWP */
