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
 * com.codename1.hardware on Windows (WindowsHardware.java).
 *
 * The Java side is the Linux one (KernelHardware): every primitive answers a
 * handle or a result >= 0, or a negative *Linux* errno, so the error mapping
 * and the edge/timeout protocol are shared rather than reinvented here.
 *
 * - Serial: Win32 COM ports (CreateFileW + DCB), on every Windows.
 * - GPIO, I2C, SPI, PWM: the Windows.Devices.* WinRT APIs. They answer on
 *   Windows IoT Enterprise boards whose BSP ships the GpioClx/SpbCx drivers
 *   (NXP i.MX 8M, Qualcomm IoT, and x64 boards with a vendor driver) and on
 *   nothing else; an ordinary PC reports no controller, which reaches Java as
 *   ENODEV.
 * - Backlight: DDC/CI (dxva2) for an external monitor, else WMI's
 *   WmiMonitorBrightness for an integrated panel -- the usual panel PC case.
 * - CAN and the watchdog have no Windows standard: both are vendor SDKs, so
 *   they answer ENOTSUP and WindowsHardware does not advertise them.
 *
 * A UWP build (CN1_WINDOWS_UWP) gets the ENOTSUP half only: the app container
 * has neither the COM-port nor the WMI path, and an Xbox has no buses.
 */
#include "cn1_windows.h"

#ifdef _WIN32

#include <stdlib.h>
#include <string.h>

/* Linux errno values: KernelHardware formats and compares these. */
#define CN1HW_EIO 5
#define CN1HW_EBADF 9
#define CN1HW_EBUSY 16
#define CN1HW_ENODEV 19
#define CN1HW_EINVAL 22
#define CN1HW_EMFILE 24
#define CN1HW_ENOTSUP 95
#define CN1HW_ETIMEDOUT 110

#if !defined(CN1_WINDOWS_UWP)

#include <roapi.h>
#include <wrl.h>
#include <wrl/event.h>
#include <wrl/wrappers/corewrappers.h>
#include <windows.foundation.h>
#include <windows.foundation.collections.h>
#include <windows.devices.enumeration.h>
#include <windows.devices.gpio.h>
#include <windows.devices.i2c.h>
#include <windows.devices.spi.h>
#include <windows.devices.pwm.h>
#include <physicalmonitorenumerationapi.h>
#include <highlevelmonitorconfigurationapi.h>
#include <wbemcli.h>
#include <oleauto.h>

using namespace ABI::Windows::Foundation;
using namespace ABI::Windows::Foundation::Collections;
using namespace ABI::Windows::Devices::Enumeration;
using namespace ABI::Windows::Devices::Gpio;
using namespace ABI::Windows::Devices::I2c;
using namespace ABI::Windows::Devices::Spi;
using namespace ABI::Windows::Devices::Pwm;
using namespace Microsoft::WRL;
using namespace Microsoft::WRL::Wrappers;

/* ------------------------------------------------------------ handle table
 *
 * One slot per open device. Slots are claimed and released under one SRW lock;
 * the device calls themselves run outside it, on a reference the caller holds
 * (AddRef'd interface, or the duplicated serial HANDLE), so a close racing a
 * transfer cannot free what the transfer is using. */

enum { HW_FREE = 0, HW_GPIO, HW_I2C, HW_SPI, HW_SERIAL, HW_PWM };

#define CN1HW_SLOTS 128
#define CN1HW_EDGE_RING 32

typedef struct CN1WinHwSlot {
    int kind;
    IUnknown* device;        /* IGpioPin / II2cDevice / ISpiDevice / IPwmPin */
    IUnknown* owner;         /* IPwmController a PWM pin needs kept alive    */
    HANDLE serial;
    EventRegistrationToken edgeToken;
    HANDLE edgeEvent;        /* auto-reset; signalled per queued edge        */
    int edgeHead;
    int edgeCount;
    int edgeRising[CN1HW_EDGE_RING];
    LONGLONG edgeNanos[CN1HW_EDGE_RING];
} CN1WinHwSlot;

static CN1WinHwSlot cn1WinHwSlots[CN1HW_SLOTS];
static SRWLOCK cn1WinHwLock = SRWLOCK_INIT;

static int cn1WinHwAlloc(int kind) {
    int h = -CN1HW_EMFILE;
    AcquireSRWLockExclusive(&cn1WinHwLock);
    for (int i = 0; i < CN1HW_SLOTS; i++) {
        if (cn1WinHwSlots[i].kind == HW_FREE) {
            ZeroMemory(&cn1WinHwSlots[i], sizeof(CN1WinHwSlot));
            cn1WinHwSlots[i].kind = kind;
            cn1WinHwSlots[i].serial = INVALID_HANDLE_VALUE;
            h = i;
            break;
        }
    }
    ReleaseSRWLockExclusive(&cn1WinHwLock);
    return h;
}

/* A referenced copy of the slot's device interface, or NULL for a stale or
 * wrong-kind handle. The caller releases it. */
static IUnknown* cn1WinHwRef(int h, int kind) {
    IUnknown* d = NULL;
    if (h < 0 || h >= CN1HW_SLOTS) {
        return NULL;
    }
    AcquireSRWLockShared(&cn1WinHwLock);
    if (cn1WinHwSlots[h].kind == kind && cn1WinHwSlots[h].device != NULL) {
        d = cn1WinHwSlots[h].device;
        d->AddRef();
    }
    ReleaseSRWLockShared(&cn1WinHwLock);
    return d;
}

/* A duplicate of a serial slot's HANDLE (closed by the caller), so the port
 * stays open for a read that is still running when another thread closes it. */
static HANDLE cn1WinHwSerialRef(int h) {
    HANDLE out = INVALID_HANDLE_VALUE;
    if (h < 0 || h >= CN1HW_SLOTS) {
        return out;
    }
    AcquireSRWLockShared(&cn1WinHwLock);
    if (cn1WinHwSlots[h].kind == HW_SERIAL) {
        if (!DuplicateHandle(GetCurrentProcess(), cn1WinHwSlots[h].serial, GetCurrentProcess(), &out,
                0, FALSE, DUPLICATE_SAME_ACCESS)) {
            out = INVALID_HANDLE_VALUE;
        }
    }
    ReleaseSRWLockShared(&cn1WinHwLock);
    return out;
}

static void cn1WinHwInit() {
    /* Refcounted by the OS; the worker threads re-enter it cheaply. The paired
     * RoUninitialize is skipped for the reason cn1_windows_winrt.cpp gives. */
    RoInitialize(RO_INIT_MULTITHREADED);
}

static int cn1WinHwFromHr(HRESULT hr) {
    if (hr == HRESULT_FROM_WIN32(ERROR_ACCESS_DENIED) || hr == E_ACCESSDENIED
            || hr == HRESULT_FROM_WIN32(ERROR_SHARING_VIOLATION)) {
        return -CN1HW_EBUSY;
    }
    if (hr == HRESULT_FROM_WIN32(ERROR_FILE_NOT_FOUND) || hr == HRESULT_FROM_WIN32(ERROR_NOT_FOUND)
            || hr == REGDB_E_CLASSNOTREG) {
        return -CN1HW_ENODEV;
    }
    if (hr == E_INVALIDARG || hr == E_BOUNDS) {
        return -CN1HW_EINVAL;
    }
    return -CN1HW_EIO;
}

static int cn1WinHwFromWin32(DWORD err) {
    switch (err) {
        case ERROR_FILE_NOT_FOUND:
        case ERROR_PATH_NOT_FOUND:
            return -CN1HW_ENODEV;
        case ERROR_ACCESS_DENIED:
        case ERROR_SHARING_VIOLATION:
            return -CN1HW_EBUSY;
        case ERROR_INVALID_PARAMETER:
            return -CN1HW_EINVAL;
        default:
            return -CN1HW_EIO;
    }
}

/* Blocks the calling worker thread until a WinRT operation completes. The same
 * helper as cn1_windows_winrt.cpp's, kept file-local as that one is. */
template <typename TOp, typename TResultPtr>
static HRESULT cn1WinHwAwait(IAsyncOperation<TOp>* op, TResultPtr result) {
    Event done(CreateEventExW(nullptr, nullptr, 0, EVENT_ALL_ACCESS));
    if (!done.IsValid()) {
        return E_FAIL;
    }
    HRESULT hr = op->put_Completed(Callback<IAsyncOperationCompletedHandler<TOp>>(
            [&done](IAsyncOperation<TOp>*, AsyncStatus) {
                SetEvent(done.Get());
                return S_OK;
            }).Get());
    if (FAILED(hr)) {
        return hr;
    }
    WaitForSingleObject(done.Get(), 30000);
    return op->GetResults(result);
}

/* Monotonic nanoseconds, the clock the Linux port's edge timestamps use. */
static LONGLONG cn1WinHwNanos() {
    static LARGE_INTEGER freq;
    LARGE_INTEGER now;
    if (freq.QuadPart == 0) {
        QueryPerformanceFrequency(&freq);
    }
    QueryPerformanceCounter(&now);
    return (LONGLONG) ((double) now.QuadPart * 1000000000.0 / (double) freq.QuadPart);
}

/* The device ids an AQS selector matches, in the order Windows enumerates them:
 * bus N in the Java API is the Nth. Answers the count and fills ids[] (each a
 * new HSTRING the caller deletes) up to max. */
static int cn1WinHwFindDevices(HSTRING selector, HSTRING* ids, int max) {
    ComPtr<IDeviceInformationStatics> statics;
    if (FAILED(RoGetActivationFactory(HStringReference(RuntimeClass_Windows_Devices_Enumeration_DeviceInformation).Get(),
            IID_PPV_ARGS(&statics)))) {
        return 0;
    }
    ComPtr<IAsyncOperation<DeviceInformationCollection*>> op;
    if (FAILED(statics->FindAllAsyncAqsFilter(selector, &op))) {
        return 0;
    }
    ComPtr<IVectorView<DeviceInformation*>> list;
    if (FAILED(cn1WinHwAwait(op.Get(), list.GetAddressOf())) || list == nullptr) {
        return 0;
    }
    unsigned size = 0;
    list->get_Size(&size);
    for (unsigned i = 0; i < size && (int) i < max; i++) {
        ComPtr<IDeviceInformation> info;
        ids[i] = NULL;
        if (SUCCEEDED(list->GetAt(i, &info))) {
            info->get_Id(&ids[i]);
        }
    }
    return (int) size;
}

#define CN1HW_MAX_BUSES 16

/* The device id of bus `index` for a selector, or NULL. */
static HSTRING cn1WinHwBusId(HSTRING selector, int index) {
    HSTRING ids[CN1HW_MAX_BUSES];
    int n = cn1WinHwFindDevices(selector, ids, CN1HW_MAX_BUSES);
    HSTRING out = NULL;
    for (int i = 0; i < n && i < CN1HW_MAX_BUSES; i++) {
        if (i == index) {
            out = ids[i];
        } else if (ids[i] != NULL) {
            WindowsDeleteString(ids[i]);
        }
    }
    return out;
}

extern "C" {

/* ------------------------------------------------------------------ GPIO */

JAVA_INT com_codename1_impl_windows_WindowsNative_hwGpioOpen___java_lang_String_int_boolean_boolean_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT chip, JAVA_INT line, JAVA_BOOLEAN output, JAVA_BOOLEAN high,
        JAVA_INT bias, JAVA_INT edges) {
    /* Windows exposes one GPIO controller to applications (GpioController's
     * default, which a BSP may back with several banks numbered contiguously),
     * so the chip name is not a selector here: WindowsHardware lists it as
     * "gpio0", and any name opens it. */
    (void) chip;
    if (line < 0) {
        return -CN1HW_EINVAL;
    }
    cn1WinHwInit();
    ComPtr<IGpioControllerStatics> statics;
    if (FAILED(RoGetActivationFactory(HStringReference(RuntimeClass_Windows_Devices_Gpio_GpioController).Get(),
            IID_PPV_ARGS(&statics)))) {
        return -CN1HW_ENODEV;
    }
    ComPtr<IGpioController> controller;
    if (FAILED(statics->GetDefault(&controller)) || controller == nullptr) {
        return -CN1HW_ENODEV;
    }
    ComPtr<IGpioPin> pin;
    GpioOpenStatus status = GpioOpenStatus_PinUnavailable;
    boolean opened = false;
    HRESULT hr = controller->TryOpenPin(line, GpioSharingMode_Exclusive, &pin, &status, &opened);
    if (FAILED(hr) || !opened || pin == nullptr) {
        return status == GpioOpenStatus_SharingViolation ? -CN1HW_EBUSY : -CN1HW_ENODEV;
    }
    GpioPinDriveMode mode = GpioPinDriveMode_Input;
    if (output) {
        mode = GpioPinDriveMode_Output;
        /* Latch the level first so the pin never glitches the other way when
         * it turns into an output. */
        pin->Write(high ? GpioPinValue_High : GpioPinValue_Low);
    } else if (bias == 2) {
        mode = GpioPinDriveMode_InputPullUp;
    } else if (bias == 3) {
        mode = GpioPinDriveMode_InputPullDown;
    }
    boolean supported = false;
    if (FAILED(pin->IsDriveModeSupported(mode, &supported)) || !supported) {
        return -CN1HW_ENOTSUP;
    }
    if (FAILED(pin->SetDriveMode(mode))) {
        return -CN1HW_EIO;
    }
    int h = cn1WinHwAlloc(HW_GPIO);
    if (h < 0) {
        return h;
    }
    if (!output && edges != 0) {
        HANDLE ev = CreateEventW(NULL, FALSE, FALSE, NULL);
        int slot = h;
        EventRegistrationToken token;
        hr = pin->add_ValueChanged(Callback<ITypedEventHandler<GpioPin*, GpioPinValueChangedEventArgs*>>(
                [slot, edges](IGpioPin*, IGpioPinValueChangedEventArgs* args) -> HRESULT {
                    GpioPinEdge edge = GpioPinEdge_FallingEdge;
                    args->get_Edge(&edge);
                    int rising = edge == GpioPinEdge_RisingEdge ? 1 : 0;
                    if ((rising && (edges & 1) == 0) || (!rising && (edges & 2) == 0)) {
                        return S_OK;
                    }
                    LONGLONG ns = cn1WinHwNanos();
                    AcquireSRWLockExclusive(&cn1WinHwLock);
                    CN1WinHwSlot* s = &cn1WinHwSlots[slot];
                    if (s->kind == HW_GPIO && s->edgeEvent != NULL) {
                        /* A full ring drops the oldest edge, as the kernel's
                         * line-event buffer does. */
                        int tail = (s->edgeHead + s->edgeCount) % CN1HW_EDGE_RING;
                        if (s->edgeCount == CN1HW_EDGE_RING) {
                            s->edgeHead = (s->edgeHead + 1) % CN1HW_EDGE_RING;
                        } else {
                            s->edgeCount++;
                        }
                        s->edgeRising[tail] = rising;
                        s->edgeNanos[tail] = ns;
                        SetEvent(s->edgeEvent);
                    }
                    ReleaseSRWLockExclusive(&cn1WinHwLock);
                    return S_OK;
                }).Get(), &token);
        AcquireSRWLockExclusive(&cn1WinHwLock);
        cn1WinHwSlots[h].edgeEvent = ev;
        cn1WinHwSlots[h].edgeToken = token;
        ReleaseSRWLockExclusive(&cn1WinHwLock);
        if (FAILED(hr)) {
            AcquireSRWLockExclusive(&cn1WinHwLock);
            cn1WinHwSlots[h].kind = HW_FREE;
            cn1WinHwSlots[h].edgeEvent = NULL;
            ReleaseSRWLockExclusive(&cn1WinHwLock);
            CloseHandle(ev);
            return -CN1HW_ENOTSUP;
        }
    }
    AcquireSRWLockExclusive(&cn1WinHwLock);
    cn1WinHwSlots[h].device = pin.Detach();
    ReleaseSRWLockExclusive(&cn1WinHwLock);
    return h;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwGpioRead___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h) {
    IUnknown* d = cn1WinHwRef(h, HW_GPIO);
    if (d == NULL) {
        return -CN1HW_EBADF;
    }
    GpioPinValue v = GpioPinValue_Low;
    HRESULT hr = ((IGpioPin*) d)->Read(&v);
    d->Release();
    return FAILED(hr) ? cn1WinHwFromHr(hr) : (v == GpioPinValue_High ? 1 : 0);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwGpioWrite___int_boolean_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h,
        JAVA_BOOLEAN high) {
    IUnknown* d = cn1WinHwRef(h, HW_GPIO);
    if (d == NULL) {
        return -CN1HW_EBADF;
    }
    HRESULT hr = ((IGpioPin*) d)->Write(high ? GpioPinValue_High : GpioPinValue_Low);
    d->Release();
    return FAILED(hr) ? cn1WinHwFromHr(hr) : 0;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwGpioWaitEdge___int_int_long_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_INT h, JAVA_INT timeoutMs, JAVA_OBJECT tsOut) {
    HANDLE ev = NULL;
    if (h < 0 || h >= CN1HW_SLOTS) {
        return -CN1HW_EBADF;
    }
    AcquireSRWLockShared(&cn1WinHwLock);
    if (cn1WinHwSlots[h].kind == HW_GPIO && cn1WinHwSlots[h].edgeEvent != NULL) {
        DuplicateHandle(GetCurrentProcess(), cn1WinHwSlots[h].edgeEvent, GetCurrentProcess(), &ev, 0, FALSE,
                DUPLICATE_SAME_ACCESS);
    }
    ReleaseSRWLockShared(&cn1WinHwLock);
    if (ev == NULL) {
        return -CN1HW_EBADF;
    }
    int r = -CN1HW_ETIMEDOUT;
    LONGLONG ns = 0;
    DWORD deadline = GetTickCount() + (DWORD) (timeoutMs < 0 ? 0 : timeoutMs);
    for (;;) {
        AcquireSRWLockExclusive(&cn1WinHwLock);
        CN1WinHwSlot* s = &cn1WinHwSlots[h];
        if (s->kind != HW_GPIO) {
            r = -CN1HW_EBADF;
        } else if (s->edgeCount > 0) {
            r = s->edgeRising[s->edgeHead];
            ns = s->edgeNanos[s->edgeHead];
            s->edgeHead = (s->edgeHead + 1) % CN1HW_EDGE_RING;
            s->edgeCount--;
        }
        ReleaseSRWLockExclusive(&cn1WinHwLock);
        if (r != -CN1HW_ETIMEDOUT) {
            break;
        }
        DWORD wait = INFINITE;
        if (timeoutMs >= 0) {
            DWORD now = GetTickCount();
            if ((LONG) (deadline - now) <= 0) {
                break;
            }
            wait = deadline - now;
        }
        CN1_YIELD_THREAD;
        DWORD w = WaitForSingleObject(ev, wait);
        CN1_RESUME_THREAD;
        if (w != WAIT_OBJECT_0 && timeoutMs >= 0) {
            break;
        }
    }
    CloseHandle(ev);
    if (r >= 0 && tsOut != JAVA_NULL && CN1_ARRAY_LENGTH(tsOut) > 0) {
        ((JAVA_LONG*) CN1_ARRAY_DATA(tsOut))[0] = (JAVA_LONG) ns;
    }
    return r;
}

/* ------------------------------------------------------------------- I2C */

JAVA_INT com_codename1_impl_windows_WindowsNative_hwI2cOpen___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT bus,
        JAVA_INT address) {
    if (bus < 0 || address < 0 || address > 0x3ff) {
        return -CN1HW_EINVAL;
    }
    cn1WinHwInit();
    ComPtr<II2cDeviceStatics> statics;
    if (FAILED(RoGetActivationFactory(HStringReference(RuntimeClass_Windows_Devices_I2c_I2cDevice).Get(),
            IID_PPV_ARGS(&statics)))) {
        return -CN1HW_ENODEV;
    }
    HString selector;
    if (FAILED(statics->GetDeviceSelector(selector.GetAddressOf()))) {
        return -CN1HW_ENODEV;
    }
    HSTRING id = cn1WinHwBusId(selector.Get(), bus);
    if (id == NULL) {
        return -CN1HW_ENODEV;
    }
    ComPtr<II2cConnectionSettingsFactory> settingsFactory;
    ComPtr<II2cConnectionSettings> settings;
    HRESULT hr = RoGetActivationFactory(HStringReference(RuntimeClass_Windows_Devices_I2c_I2cConnectionSettings).Get(),
            IID_PPV_ARGS(&settingsFactory));
    if (SUCCEEDED(hr)) {
        hr = settingsFactory->Create(address, &settings);
    }
    if (SUCCEEDED(hr) && address > 0x7f) {
        hr = settings->put_AddressingMode(I2cAddressingMode_TenBit);
    }
    ComPtr<IAsyncOperation<I2cDevice*>> op;
    if (SUCCEEDED(hr)) {
        hr = statics->FromIdAsync(id, settings.Get(), &op);
    }
    WindowsDeleteString(id);
    ComPtr<II2cDevice> device;
    if (SUCCEEDED(hr)) {
        CN1_YIELD_THREAD;
        hr = cn1WinHwAwait(op.Get(), device.GetAddressOf());
        CN1_RESUME_THREAD;
    }
    if (FAILED(hr)) {
        return cn1WinHwFromHr(hr);
    }
    if (device == nullptr) {
        /* FromIdAsync answers null for a bus another process holds. */
        return -CN1HW_EBUSY;
    }
    int h = cn1WinHwAlloc(HW_I2C);
    if (h >= 0) {
        AcquireSRWLockExclusive(&cn1WinHwLock);
        cn1WinHwSlots[h].device = device.Detach();
        ReleaseSRWLockExclusive(&cn1WinHwLock);
    }
    return h;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwI2cTransfer___int_byte_1ARRAY_int_byte_1ARRAY_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT w, JAVA_INT wl, JAVA_OBJECT rd, JAVA_INT rl) {
    if (wl < 0 || rl < 0 || (wl > 0 && (w == JAVA_NULL || wl > CN1_ARRAY_LENGTH(w)))
            || (rl > 0 && (rd == JAVA_NULL || rl > CN1_ARRAY_LENGTH(rd))) || (wl == 0 && rl == 0)) {
        return -CN1HW_EINVAL;
    }
    IUnknown* d = cn1WinHwRef(h, HW_I2C);
    if (d == NULL) {
        return -CN1HW_EBADF;
    }
    II2cDevice* dev = (II2cDevice*) d;
    BYTE* wp = wl > 0 ? (BYTE*) CN1_ARRAY_DATA(w) : NULL;
    BYTE* rp = rl > 0 ? (BYTE*) CN1_ARRAY_DATA(rd) : NULL;
    HRESULT hr;
    CN1_YIELD_THREAD;
    if (wl > 0 && rl > 0) {
        hr = dev->WriteRead((UINT32) wl, wp, (UINT32) rl, rp);
    } else if (wl > 0) {
        hr = dev->Write((UINT32) wl, wp);
    } else {
        hr = dev->Read((UINT32) rl, rp);
    }
    CN1_RESUME_THREAD;
    d->Release();
    return FAILED(hr) ? cn1WinHwFromHr(hr) : 0;
}

/* ------------------------------------------------------------------- SPI */

JAVA_INT com_codename1_impl_windows_WindowsNative_hwSpiOpen___int_int_int_int_int_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_INT bus, JAVA_INT cs, JAVA_INT mode, JAVA_INT speed, JAVA_INT bits) {
    if (bus < 0 || cs < 0 || mode < 0 || mode > 3 || speed <= 0 || bits < 1 || bits > 32) {
        return -CN1HW_EINVAL;
    }
    cn1WinHwInit();
    ComPtr<ISpiDeviceStatics> statics;
    if (FAILED(RoGetActivationFactory(HStringReference(RuntimeClass_Windows_Devices_Spi_SpiDevice).Get(),
            IID_PPV_ARGS(&statics)))) {
        return -CN1HW_ENODEV;
    }
    HString selector;
    if (FAILED(statics->GetDeviceSelector(selector.GetAddressOf()))) {
        return -CN1HW_ENODEV;
    }
    HSTRING id = cn1WinHwBusId(selector.Get(), bus);
    if (id == NULL) {
        return -CN1HW_ENODEV;
    }
    ComPtr<ISpiConnectionSettingsFactory> settingsFactory;
    ComPtr<ISpiConnectionSettings> settings;
    HRESULT hr = RoGetActivationFactory(HStringReference(RuntimeClass_Windows_Devices_Spi_SpiConnectionSettings).Get(),
            IID_PPV_ARGS(&settingsFactory));
    if (SUCCEEDED(hr)) {
        hr = settingsFactory->Create(cs, &settings);
    }
    if (SUCCEEDED(hr)) {
        settings->put_Mode((SpiMode) mode);
        settings->put_ClockFrequency(speed);
        settings->put_DataBitLength(bits);
    }
    ComPtr<IAsyncOperation<SpiDevice*>> op;
    if (SUCCEEDED(hr)) {
        hr = statics->FromIdAsync(id, settings.Get(), &op);
    }
    WindowsDeleteString(id);
    ComPtr<ISpiDevice> device;
    if (SUCCEEDED(hr)) {
        CN1_YIELD_THREAD;
        hr = cn1WinHwAwait(op.Get(), device.GetAddressOf());
        CN1_RESUME_THREAD;
    }
    if (FAILED(hr)) {
        return cn1WinHwFromHr(hr);
    }
    if (device == nullptr) {
        return -CN1HW_EBUSY;
    }
    int h = cn1WinHwAlloc(HW_SPI);
    if (h >= 0) {
        AcquireSRWLockExclusive(&cn1WinHwLock);
        cn1WinHwSlots[h].device = device.Detach();
        ReleaseSRWLockExclusive(&cn1WinHwLock);
    }
    return h;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwSpiTransfer___int_byte_1ARRAY_byte_1ARRAY_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT tx, JAVA_OBJECT rx, JAVA_INT len) {
    if (len <= 0 || (tx != JAVA_NULL && len > CN1_ARRAY_LENGTH(tx)) || (rx != JAVA_NULL && len > CN1_ARRAY_LENGTH(rx))
            || (tx == JAVA_NULL && rx == JAVA_NULL)) {
        return -CN1HW_EINVAL;
    }
    IUnknown* d = cn1WinHwRef(h, HW_SPI);
    if (d == NULL) {
        return -CN1HW_EBADF;
    }
    ISpiDevice* dev = (ISpiDevice*) d;
    HRESULT hr;
    CN1_YIELD_THREAD;
    if (tx != JAVA_NULL && rx != JAVA_NULL) {
        hr = dev->TransferFullDuplex((UINT32) len, (BYTE*) CN1_ARRAY_DATA(tx), (UINT32) len, (BYTE*) CN1_ARRAY_DATA(rx));
    } else if (tx != JAVA_NULL) {
        hr = dev->Write((UINT32) len, (BYTE*) CN1_ARRAY_DATA(tx));
    } else {
        hr = dev->Read((UINT32) len, (BYTE*) CN1_ARRAY_DATA(rx));
    }
    CN1_RESUME_THREAD;
    d->Release();
    return FAILED(hr) ? cn1WinHwFromHr(hr) : 0;
}

/* ---------------------------------------------------------------- serial */

/* "COM3" or "\\.\COM3", and nothing else: the port opens a COM device, never a
 * file or another DOS device an application names. Answers the number. */
static int cn1WinHwComNumber(const char* name) {
    if (name == NULL) {
        return -1;
    }
    if (strncmp(name, "\\\\.\\", 4) == 0) {
        name += 4;
    }
    if ((name[0] != 'C' && name[0] != 'c') || (name[1] != 'O' && name[1] != 'o') || (name[2] != 'M' && name[2] != 'm')) {
        return -1;
    }
    int n = 0;
    const char* p = name + 3;
    if (*p == 0) {
        return -1;
    }
    for (; *p; p++) {
        if (*p < '0' || *p > '9' || n > 1000) {
            return -1;
        }
        n = n * 10 + (*p - '0');
    }
    return n > 0 ? n : -1;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwSerialOpen___java_lang_String_int_int_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT dev, JAVA_INT baud, JAVA_INT dataBits, JAVA_INT parity, JAVA_INT stopBits) {
    int com = cn1WinHwComNumber(dev == JAVA_NULL ? NULL : stringToUTF8(threadStateData, dev));
    if (com < 0 || baud <= 0 || dataBits < 5 || dataBits > 8 || stopBits < 1 || stopBits > 2 || parity < 0 || parity > 2) {
        return -CN1HW_EINVAL;
    }
    WCHAR path[32];
    wsprintfW(path, L"\\\\.\\COM%d", com);
    HANDLE port = CreateFileW(path, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL);
    if (port == INVALID_HANDLE_VALUE) {
        return cn1WinHwFromWin32(GetLastError());
    }
    DCB dcb;
    ZeroMemory(&dcb, sizeof(dcb));
    dcb.DCBlength = sizeof(dcb);
    if (!GetCommState(port, &dcb)) {
        CloseHandle(port);
        return -CN1HW_EIO;
    }
    /* Raw, like cfmakeraw on Linux: binary, no flow control, no character
     * substitution. */
    dcb.BaudRate = (DWORD) baud;
    dcb.ByteSize = (BYTE) dataBits;
    dcb.Parity = parity == 1 ? ODDPARITY : parity == 2 ? EVENPARITY : NOPARITY;
    dcb.StopBits = stopBits == 2 ? TWOSTOPBITS : ONESTOPBIT;
    dcb.fBinary = TRUE;
    dcb.fParity = parity != 0;
    dcb.fOutxCtsFlow = FALSE;
    dcb.fOutxDsrFlow = FALSE;
    dcb.fDtrControl = DTR_CONTROL_ENABLE;
    dcb.fDsrSensitivity = FALSE;
    dcb.fOutX = FALSE;
    dcb.fInX = FALSE;
    dcb.fErrorChar = FALSE;
    dcb.fNull = FALSE;
    dcb.fRtsControl = RTS_CONTROL_ENABLE;
    dcb.fAbortOnError = FALSE;
    if (!SetCommState(port, &dcb)) {
        CloseHandle(port);
        return -CN1HW_EINVAL;
    }
    PurgeComm(port, PURGE_RXCLEAR | PURGE_TXCLEAR);
    int h = cn1WinHwAlloc(HW_SERIAL);
    if (h < 0) {
        CloseHandle(port);
        return h;
    }
    AcquireSRWLockExclusive(&cn1WinHwLock);
    cn1WinHwSlots[h].serial = port;
    ReleaseSRWLockExclusive(&cn1WinHwLock);
    return h;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwSerialRead___int_byte_1ARRAY_int_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT buf, JAVA_INT off, JAVA_INT len, JAVA_INT timeoutMs) {
    if (buf == JAVA_NULL || off < 0 || len < 0 || off > CN1_ARRAY_LENGTH(buf) - len) {
        return -CN1HW_EINVAL;
    }
    HANDLE port = cn1WinHwSerialRef(h);
    if (port == INVALID_HANDLE_VALUE) {
        return -CN1HW_EBADF;
    }
    if (len == 0) {
        CloseHandle(port);
        return 0;
    }
    /* Return as soon as any byte is there, or after timeoutMs with nothing: the
     * documented MAXDWORD/MAXDWORD/constant combination. A negative timeout
     * waits indefinitely, which the constant cannot say, so it is a long one. */
    COMMTIMEOUTS t;
    ZeroMemory(&t, sizeof(t));
    t.ReadIntervalTimeout = MAXDWORD;
    t.ReadTotalTimeoutMultiplier = MAXDWORD;
    t.ReadTotalTimeoutConstant = timeoutMs < 0 ? (MAXDWORD - 1) : (timeoutMs == 0 ? 1 : (DWORD) timeoutMs);
    t.WriteTotalTimeoutConstant = 0;
    SetCommTimeouts(port, &t);
    DWORD n = 0;
    CN1_YIELD_THREAD;
    BOOL ok = ReadFile(port, (char*) CN1_ARRAY_DATA(buf) + off, (DWORD) len, &n, NULL);
    CN1_RESUME_THREAD;
    DWORD err = ok ? 0 : GetLastError();
    CloseHandle(port);
    return ok ? (JAVA_INT) n : cn1WinHwFromWin32(err);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwSerialWrite___int_byte_1ARRAY_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT buf, JAVA_INT off, JAVA_INT len) {
    if (buf == JAVA_NULL || off < 0 || len < 0 || off > CN1_ARRAY_LENGTH(buf) - len) {
        return -CN1HW_EINVAL;
    }
    HANDLE port = cn1WinHwSerialRef(h);
    if (port == INVALID_HANDLE_VALUE) {
        return -CN1HW_EBADF;
    }
    int written = 0;
    int r = 0;
    CN1_YIELD_THREAD;
    while (written < len) {
        DWORD n = 0;
        if (!WriteFile(port, (char*) CN1_ARRAY_DATA(buf) + off + written, (DWORD) (len - written), &n, NULL)) {
            r = cn1WinHwFromWin32(GetLastError());
            break;
        }
        written += (int) n;
    }
    CN1_RESUME_THREAD;
    CloseHandle(port);
    return r < 0 ? r : written;
}

/* ------------------------------------------------------------------- PWM */

JAVA_INT com_codename1_impl_windows_WindowsNative_hwPwmOpen___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT chip,
        JAVA_INT channel) {
    if (chip < 0 || channel < 0) {
        return -CN1HW_EINVAL;
    }
    cn1WinHwInit();
    ComPtr<IPwmControllerStatics3> statics;
    if (FAILED(RoGetActivationFactory(HStringReference(RuntimeClass_Windows_Devices_Pwm_PwmController).Get(),
            IID_PPV_ARGS(&statics)))) {
        return -CN1HW_ENODEV;
    }
    HString selector;
    if (FAILED(statics->GetDeviceSelector(selector.GetAddressOf()))) {
        return -CN1HW_ENODEV;
    }
    HSTRING id = cn1WinHwBusId(selector.Get(), chip);
    if (id == NULL) {
        return -CN1HW_ENODEV;
    }
    ComPtr<IAsyncOperation<PwmController*>> op;
    HRESULT hr = statics->FromIdAsync(id, &op);
    WindowsDeleteString(id);
    ComPtr<IPwmController> controller;
    if (SUCCEEDED(hr)) {
        CN1_YIELD_THREAD;
        hr = cn1WinHwAwait(op.Get(), controller.GetAddressOf());
        CN1_RESUME_THREAD;
    }
    if (FAILED(hr) || controller == nullptr) {
        return FAILED(hr) ? cn1WinHwFromHr(hr) : -CN1HW_EBUSY;
    }
    INT32 count = 0;
    controller->get_PinCount(&count);
    if (channel >= count) {
        return -CN1HW_ENODEV;
    }
    ComPtr<IPwmPin> pin;
    hr = controller->OpenPin(channel, &pin);
    if (FAILED(hr) || pin == nullptr) {
        return FAILED(hr) ? cn1WinHwFromHr(hr) : -CN1HW_EBUSY;
    }
    int h = cn1WinHwAlloc(HW_PWM);
    if (h >= 0) {
        AcquireSRWLockExclusive(&cn1WinHwLock);
        cn1WinHwSlots[h].device = pin.Detach();
        cn1WinHwSlots[h].owner = controller.Detach();
        ReleaseSRWLockExclusive(&cn1WinHwLock);
    }
    return h;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwPwmConfigure___int_long_long_boolean_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_INT h, JAVA_LONG period, JAVA_LONG duty, JAVA_BOOLEAN enabled) {
    if (period <= 0 || duty < 0 || duty > period) {
        return -CN1HW_EINVAL;
    }
    IUnknown* d = cn1WinHwRef(h, HW_PWM);
    if (d == NULL) {
        return -CN1HW_EBADF;
    }
    IPwmPin* pin = (IPwmPin*) d;
    ComPtr<IPwmController> controller;
    HRESULT hr = pin->get_Controller(&controller);
    if (SUCCEEDED(hr)) {
        /* Windows sets the frequency per controller, not per channel: two
         * channels of one controller share it, which the documentation of
         * the Windows target states. */
        double actual = 0;
        hr = controller->SetDesiredFrequency(1000000000.0 / (double) period, &actual);
    }
    if (SUCCEEDED(hr)) {
        hr = pin->SetActiveDutyCyclePercentage((double) duty / (double) period);
    }
    if (SUCCEEDED(hr)) {
        boolean started = false;
        pin->get_IsStarted(&started);
        if (enabled && !started) {
            hr = pin->Start();
        } else if (!enabled && started) {
            hr = pin->Stop();
        }
    }
    d->Release();
    return FAILED(hr) ? cn1WinHwFromHr(hr) : 0;
}

/* -------------------------------------------------- CAN and the watchdog */

JAVA_INT com_codename1_impl_windows_WindowsNative_hwCanOpen___java_lang_String_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_OBJECT iface) {
    return -CN1HW_ENOTSUP;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwCanSend___int_int_boolean_byte_1ARRAY_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_INT id, JAVA_BOOLEAN ext, JAVA_OBJECT data, JAVA_INT len) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwCanReceive___int_int_1ARRAY_byte_1ARRAY_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT idOut, JAVA_OBJECT data, JAVA_INT timeoutMs) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwWatchdogOpen___int_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_INT timeoutSeconds) {
    return -CN1HW_ENOTSUP;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwWatchdogKick___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h) {
    return -CN1HW_EBADF;
}

/* ----------------------------------------------------------------- close */

JAVA_VOID com_codename1_impl_windows_WindowsNative_hwClose___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_INT h,
        JAVA_BOOLEAN disarm) {
    (void) disarm; /* only a watchdog disarms, and there is none here */
    if (h < 0 || h >= CN1HW_SLOTS) {
        return;
    }
    AcquireSRWLockExclusive(&cn1WinHwLock);
    CN1WinHwSlot s = cn1WinHwSlots[h];
    cn1WinHwSlots[h].kind = HW_FREE;
    cn1WinHwSlots[h].device = NULL;
    cn1WinHwSlots[h].owner = NULL;
    cn1WinHwSlots[h].serial = INVALID_HANDLE_VALUE;
    cn1WinHwSlots[h].edgeEvent = NULL;
    ReleaseSRWLockExclusive(&cn1WinHwLock);
    if (s.kind == HW_FREE) {
        return;
    }
    if (s.kind == HW_GPIO && s.device != NULL && s.edgeEvent != NULL) {
        ((IGpioPin*) s.device)->remove_ValueChanged(s.edgeToken);
    }
    if (s.kind == HW_PWM && s.device != NULL) {
        ((IPwmPin*) s.device)->Stop();
    }
    if (s.device != NULL) {
        /* Close releases the pin or bus for other processes now, not when the
         * last reference a transfer may still hold goes away. */
        ComPtr<IClosable> closable;
        if (SUCCEEDED(s.device->QueryInterface(IID_PPV_ARGS(&closable)))) {
            closable->Close();
        }
        s.device->Release();
    }
    if (s.owner != NULL) {
        s.owner->Release();
    }
    if (s.serial != INVALID_HANDLE_VALUE) {
        CloseHandle(s.serial);
    }
    if (s.edgeEvent != NULL) {
        /* Wakes a waiter, which then finds the slot gone and answers EBADF. */
        SetEvent(s.edgeEvent);
        CloseHandle(s.edgeEvent);
    }
}

/* ----------------------------------------------------------------- lists */

static void cn1WinHwAppend(char* out, size_t cap, const char* line) {
    size_t used = strlen(out);
    size_t n = strlen(line);
    if (used + n + 2 < cap) {
        if (used > 0) {
            out[used++] = '\n';
        }
        memcpy(out + used, line, n + 1);
    }
}

static void cn1WinHwListBuses(HSTRING selector, const char* prefix, char* out, size_t cap) {
    HSTRING ids[CN1HW_MAX_BUSES];
    int n = cn1WinHwFindDevices(selector, ids, CN1HW_MAX_BUSES);
    for (int i = 0; i < n && i < CN1HW_MAX_BUSES; i++) {
        char line[32];
        wsprintfA(line, "%s%d", prefix, i);
        cn1WinHwAppend(out, cap, line);
        if (ids[i] != NULL) {
            WindowsDeleteString(ids[i]);
        }
    }
}

/* kind: 0 GPIO controllers, 1 I2C buses, 2 SPI buses, 3 serial ports, 4 CAN.
 * Newline separated, in the shape WindowsHardware parses: "gpio0", "i2c-N",
 * "spi-N", "COMn". */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_hwList___int_R_java_lang_String(CODENAME_ONE_THREAD_STATE,
        JAVA_INT kind) {
    char out[4096];
    out[0] = 0;
    cn1WinHwInit();
    if (kind == 0) {
        ComPtr<IGpioControllerStatics> statics;
        ComPtr<IGpioController> controller;
        if (SUCCEEDED(RoGetActivationFactory(HStringReference(RuntimeClass_Windows_Devices_Gpio_GpioController).Get(),
                IID_PPV_ARGS(&statics))) && SUCCEEDED(statics->GetDefault(&controller)) && controller != nullptr) {
            cn1WinHwAppend(out, sizeof(out), "gpio0");
        }
    } else if (kind == 1) {
        ComPtr<II2cDeviceStatics> statics;
        HString selector;
        if (SUCCEEDED(RoGetActivationFactory(HStringReference(RuntimeClass_Windows_Devices_I2c_I2cDevice).Get(),
                IID_PPV_ARGS(&statics))) && SUCCEEDED(statics->GetDeviceSelector(selector.GetAddressOf()))) {
            cn1WinHwListBuses(selector.Get(), "i2c-", out, sizeof(out));
        }
    } else if (kind == 2) {
        ComPtr<ISpiDeviceStatics> statics;
        HString selector;
        if (SUCCEEDED(RoGetActivationFactory(HStringReference(RuntimeClass_Windows_Devices_Spi_SpiDevice).Get(),
                IID_PPV_ARGS(&statics))) && SUCCEEDED(statics->GetDeviceSelector(selector.GetAddressOf()))) {
            cn1WinHwListBuses(selector.Get(), "spi-", out, sizeof(out));
        }
    } else if (kind == 3) {
        /* Every DOS device name, filtered to COMn: no SetupAPI needed. */
        DWORD cap = 65536;
        WCHAR* names = (WCHAR*) malloc(cap * sizeof(WCHAR));
        if (names != NULL && QueryDosDeviceW(NULL, names, cap) != 0) {
            for (WCHAR* p = names; *p; p += wcslen(p) + 1) {
                char narrow[64];
                if (WideCharToMultiByte(CP_UTF8, 0, p, -1, narrow, sizeof(narrow), NULL, NULL) > 0
                        && cn1WinHwComNumber(narrow) > 0) {
                    cn1WinHwAppend(out, sizeof(out), narrow);
                }
            }
        }
        free(names);
    }
    return newStringFromCString(threadStateData, out);
}

/* ------------------------------------------------------------- backlight
 *
 * op: 0 current, 1 maximum, 2 set. DDC/CI first (an external monitor that
 * answers it), then WMI's WmiMonitorBrightness, which is what an integrated
 * panel -- a panel PC, a tablet -- reports through. WMI's scale is 0..100. */

static int cn1WinHwDdc(int op, int value) {
    HMONITOR mon = MonitorFromPoint(POINT{0, 0}, MONITOR_DEFAULTTOPRIMARY);
    DWORD count = 0;
    if (!GetNumberOfPhysicalMonitorsFromHMONITOR(mon, &count) || count == 0) {
        return -CN1HW_ENODEV;
    }
    PHYSICAL_MONITOR* monitors = (PHYSICAL_MONITOR*) calloc(count, sizeof(PHYSICAL_MONITOR));
    if (monitors == NULL || !GetPhysicalMonitorsFromHMONITOR(mon, count, monitors)) {
        free(monitors);
        return -CN1HW_ENODEV;
    }
    DWORD lo = 0, cur = 0, hi = 0;
    int r = -CN1HW_ENODEV;
    if (GetMonitorBrightness(monitors[0].hPhysicalMonitor, &lo, &cur, &hi)) {
        if (op == 0) {
            r = (int) cur;
        } else if (op == 1) {
            r = (int) hi;
        } else {
            DWORD v = value < (int) lo ? lo : (value > (int) hi ? hi : (DWORD) value);
            r = SetMonitorBrightness(monitors[0].hPhysicalMonitor, v) ? 0 : -CN1HW_EIO;
        }
    }
    DestroyPhysicalMonitors(count, monitors);
    free(monitors);
    return r;
}

static int cn1WinHwWmi(int op, int value) {
    if (op == 1) {
        return 100;
    }
    CoInitializeEx(NULL, COINIT_MULTITHREADED);
    ComPtr<IWbemLocator> locator;
    if (FAILED(CoCreateInstance(__uuidof(WbemLocator), NULL, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&locator)))) {
        return -CN1HW_ENODEV;
    }
    ComPtr<IWbemServices> services;
    BSTR ns = SysAllocString(L"ROOT\\WMI");
    HRESULT hr = locator->ConnectServer(ns, NULL, NULL, NULL, 0, NULL, NULL, &services);
    SysFreeString(ns);
    if (FAILED(hr)) {
        return -CN1HW_ENODEV;
    }
    CoSetProxyBlanket(services.Get(), RPC_C_AUTHN_WINNT, RPC_C_AUTHZ_NONE, NULL, RPC_C_AUTHN_LEVEL_CALL,
            RPC_C_IMP_LEVEL_IMPERSONATE, NULL, EOAC_NONE);
    BSTR lang = SysAllocString(L"WQL");
    BSTR query = SysAllocString(op == 0 ? L"SELECT CurrentBrightness FROM WmiMonitorBrightness"
            : L"SELECT * FROM WmiMonitorBrightnessMethods");
    ComPtr<IEnumWbemClassObject> results;
    hr = services->ExecQuery(lang, query, WBEM_FLAG_FORWARD_ONLY | WBEM_FLAG_RETURN_IMMEDIATELY, NULL, &results);
    SysFreeString(lang);
    SysFreeString(query);
    if (FAILED(hr)) {
        return -CN1HW_ENODEV;
    }
    ComPtr<IWbemClassObject> obj;
    ULONG got = 0;
    if (FAILED(results->Next(WBEM_INFINITE, 1, &obj, &got)) || got == 0) {
        return -CN1HW_ENODEV;
    }
    VARIANT v;
    VariantInit(&v);
    if (op == 0) {
        int r = -CN1HW_EIO;
        if (SUCCEEDED(obj->Get(L"CurrentBrightness", 0, &v, NULL, NULL))) {
            r = v.vt == VT_UI1 ? (int) v.bVal : (v.vt == VT_I4 ? (int) v.lVal : -CN1HW_EIO);
        }
        VariantClear(&v);
        return r;
    }
    /* WmiSetBrightness(Timeout uint32, Brightness uint8) on the instance. */
    if (FAILED(obj->Get(L"__PATH", 0, &v, NULL, NULL)) || v.vt != VT_BSTR) {
        VariantClear(&v);
        return -CN1HW_EIO;
    }
    BSTR path = SysAllocString(v.bstrVal);
    VariantClear(&v);
    ComPtr<IWbemClassObject> cls;
    ComPtr<IWbemClassObject> inDef;
    ComPtr<IWbemClassObject> in;
    BSTR clsName = SysAllocString(L"WmiMonitorBrightnessMethods");
    BSTR method = SysAllocString(L"WmiSetBrightness");
    hr = services->GetObject(clsName, 0, NULL, &cls, NULL);
    if (SUCCEEDED(hr)) {
        hr = cls->GetMethod(method, 0, &inDef, NULL);
    }
    if (SUCCEEDED(hr)) {
        hr = inDef->SpawnInstance(0, &in);
    }
    if (SUCCEEDED(hr)) {
        VARIANT a;
        VariantInit(&a);
        a.vt = VT_I4;
        a.lVal = 0;
        in->Put(L"Timeout", 0, &a, 0);
        a.vt = VT_UI1;
        a.bVal = (BYTE) (value < 0 ? 0 : (value > 100 ? 100 : value));
        in->Put(L"Brightness", 0, &a, 0);
        hr = services->ExecMethod(path, method, 0, NULL, in.Get(), NULL, NULL);
    }
    SysFreeString(clsName);
    SysFreeString(method);
    SysFreeString(path);
    return FAILED(hr) ? -CN1HW_EIO : 0;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwBacklight___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT op,
        JAVA_INT value) {
    int r = cn1WinHwDdc(op, value);
    if (r == -CN1HW_ENODEV) {
        r = cn1WinHwWmi(op, value);
    }
    return r;
}

} /* extern "C" */

#else /* CN1_WINDOWS_UWP: no COM ports, no WMI, no buses on an Xbox. */

extern "C" {

JAVA_INT com_codename1_impl_windows_WindowsNative_hwGpioOpen___java_lang_String_int_boolean_boolean_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT chip, JAVA_INT line, JAVA_BOOLEAN output, JAVA_BOOLEAN high,
        JAVA_INT bias, JAVA_INT edges) {
    return -CN1HW_ENOTSUP;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwGpioRead___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwGpioWrite___int_boolean_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h,
        JAVA_BOOLEAN high) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwGpioWaitEdge___int_int_long_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_INT h, JAVA_INT timeoutMs, JAVA_OBJECT tsOut) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwI2cOpen___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT bus,
        JAVA_INT address) {
    return -CN1HW_ENOTSUP;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwI2cTransfer___int_byte_1ARRAY_int_byte_1ARRAY_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT w, JAVA_INT wl, JAVA_OBJECT rd, JAVA_INT rl) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwSpiOpen___int_int_int_int_int_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_INT bus, JAVA_INT cs, JAVA_INT mode, JAVA_INT speed, JAVA_INT bits) {
    return -CN1HW_ENOTSUP;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwSpiTransfer___int_byte_1ARRAY_byte_1ARRAY_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT tx, JAVA_OBJECT rx, JAVA_INT len) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwSerialOpen___java_lang_String_int_int_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT dev, JAVA_INT baud, JAVA_INT dataBits, JAVA_INT parity, JAVA_INT stopBits) {
    return -CN1HW_ENOTSUP;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwSerialRead___int_byte_1ARRAY_int_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT buf, JAVA_INT off, JAVA_INT len, JAVA_INT timeoutMs) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwSerialWrite___int_byte_1ARRAY_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT buf, JAVA_INT off, JAVA_INT len) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwPwmOpen___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT chip,
        JAVA_INT channel) {
    return -CN1HW_ENOTSUP;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwPwmConfigure___int_long_long_boolean_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_INT h, JAVA_LONG period, JAVA_LONG duty, JAVA_BOOLEAN enabled) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwCanOpen___java_lang_String_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_OBJECT iface) {
    return -CN1HW_ENOTSUP;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwCanSend___int_int_boolean_byte_1ARRAY_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_INT id, JAVA_BOOLEAN ext, JAVA_OBJECT data, JAVA_INT len) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwCanReceive___int_int_1ARRAY_byte_1ARRAY_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT idOut, JAVA_OBJECT data, JAVA_INT timeoutMs) {
    return -CN1HW_EBADF;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwWatchdogOpen___int_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_INT timeoutSeconds) {
    return -CN1HW_ENOTSUP;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwWatchdogKick___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h) {
    return -CN1HW_EBADF;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_hwClose___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_INT h,
        JAVA_BOOLEAN disarm) {
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_hwList___int_R_java_lang_String(CODENAME_ONE_THREAD_STATE,
        JAVA_INT kind) {
    return newStringFromCString(threadStateData, "");
}

JAVA_INT com_codename1_impl_windows_WindowsNative_hwBacklight___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT op,
        JAVA_INT value) {
    return -CN1HW_ENODEV;
}

} /* extern "C" */

#endif /* CN1_WINDOWS_UWP */

#endif /* _WIN32 */
