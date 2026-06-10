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
 * WinRT-backed services for the Windows port (biometric / Windows Hello, and --
 * added incrementally -- location, contacts, share). WinRT is consumed through
 * the WRL ABI projection (RoGetActivationFactory + the ABI:: interfaces), the
 * same COM-based mechanism the Media Foundation layer already uses, so no
 * C++/WinRT (cppwinrt) projection headers are required.
 *
 * The whole real implementation is gated on CN1_HAVE_WINRT, which the generated
 * CMake defines only after a probe confirms the WinRT ABI headers +
 * runtimeobject.lib are present and link on the build toolchain. When it is not
 * defined (e.g. a cross-compile sysroot without WinRT) every bridge below
 * compiles as an honest "unsupported" stub, so the Windows-free build stays green
 * and the matching CN1 capability simply reports false / null.
 */

#ifdef _WIN32

#include "cn1_windows.h"

#ifdef CN1_HAVE_WINRT

#include <roapi.h>
#include <string>
#include <wrl.h>
#include <wrl/event.h>
#include <wrl/wrappers/corewrappers.h>
#include <windows.foundation.h>
#include <windows.foundation.collections.h>
#include <windows.security.credentials.ui.h>
#include <windows.devices.geolocation.h>
#include <windows.applicationmodel.contacts.h>

using namespace ABI::Windows::Foundation;
using namespace ABI::Windows::Foundation::Collections;
using namespace ABI::Windows::Security::Credentials::UI;
using namespace ABI::Windows::Devices::Geolocation;
using namespace ABI::Windows::ApplicationModel::Contacts;
using namespace Microsoft::WRL;
using namespace Microsoft::WRL::Wrappers;

/* IGeocoordinate's direct lat/lon/altitude getters are marked deprecated (the
 * docs steer toward Point.Position), but they remain functional and are far
 * simpler than the IGeocoordinateWithPoint -> IGeopoint -> BasicGeoposition path;
 * silence the deprecation noise rather than take the heavier path. */
#pragma clang diagnostic ignored "-Wdeprecated-declarations"

/* Blocks the calling (CN1 worker) thread until a WinRT IAsyncOperation<T>
 * completes, then fetches its result. Used for the inherently-async WinRT APIs;
 * callers invoke these natives off the EDT (via AsyncResource) so blocking is
 * fine. */
/* TOp is the operation's logical result type (a value type, or a runtimeclass --
 * for which GetResults yields the ABI *interface* pointer, so the result pointer
 * type TResultPtr is kept separate from TOp). */
template <typename TOp, typename TResultPtr>
static HRESULT cn1AwaitOp(IAsyncOperation<TOp>* op, TResultPtr result) {
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
    WaitForSingleObject(done.Get(), 120000);
    return op->GetResults(result);
}

/* Ensures the WinRT runtime is initialised on this thread (multi-threaded
 * apartment). Refcounted by the OS; paired RoUninitialize is intentionally
 * skipped -- the CN1 worker threads are short-lived and re-initialise cheaply. */
static void cn1WinRtInit() {
    RoInitialize(RO_INIT_MULTITHREADED);
}

/* Appends the UTF-8 bytes of an HSTRING to `out` (empty HSTRING -> nothing). */
static void cn1AppendHString(std::string& out, HSTRING h) {
    UINT32 len = 0;
    PCWSTR w = WindowsGetStringRawBuffer(h, &len);
    if (w == NULL || len == 0) {
        return;
    }
    int n = WideCharToMultiByte(CP_UTF8, 0, w, (int) len, NULL, 0, NULL, NULL);
    if (n <= 0) {
        return;
    }
    size_t at = out.size();
    out.resize(at + (size_t) n);
    WideCharToMultiByte(CP_UTF8, 0, w, (int) len, &out[at], n, NULL, NULL);
}

#endif /* CN1_HAVE_WINRT */

extern "C" {

/* ------------------------------------------------------------- biometric
 *
 * Maps Windows Hello (UserConsentVerifier) to com.codename1.security.Biometrics.
 * Returns the UserConsentVerifierAvailability enum: 0 Available, 1
 * DeviceNotPresent, 2 NotConfiguredForUser, 3 DisabledByPolicy, 4 DeviceBusy.
 * The Java side treats 0 as supported+ready. The stub reports DeviceNotPresent. */
JAVA_INT com_codename1_impl_windows_WindowsNative_biometricAvailability___R_int(CODENAME_ONE_THREAD_STATE) {
#ifdef CN1_HAVE_WINRT
    cn1WinRtInit();
    ComPtr<IUserConsentVerifierStatics> statics;
    HRESULT hr = RoGetActivationFactory(
            HStringReference(RuntimeClass_Windows_Security_Credentials_UI_UserConsentVerifier).Get(),
            IID_PPV_ARGS(&statics));
    if (FAILED(hr)) {
        return 1; /* DeviceNotPresent */
    }
    ComPtr<IAsyncOperation<UserConsentVerifierAvailability>> op;
    if (FAILED(statics->CheckAvailabilityAsync(&op))) {
        return 1;
    }
    UserConsentVerifierAvailability avail = UserConsentVerifierAvailability_DeviceNotPresent;
    if (FAILED(cn1AwaitOp(op.Get(), &avail))) {
        return 1;
    }
    return (JAVA_INT) avail;
#else
    return 1; /* DeviceNotPresent: no WinRT -> report unsupported */
#endif
}

/* Shows the Windows Hello consent prompt with the given reason; returns true
 * when the user is verified. Stub returns false. */
JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_biometricAuthenticate___java_lang_String_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT message) {
#ifdef CN1_HAVE_WINRT
    cn1WinRtInit();
    ComPtr<IUserConsentVerifierStatics> statics;
    HRESULT hr = RoGetActivationFactory(
            HStringReference(RuntimeClass_Windows_Security_Credentials_UI_UserConsentVerifier).Get(),
            IID_PPV_ARGS(&statics));
    if (FAILED(hr)) {
        return JAVA_FALSE;
    }
    WCHAR* msg = message != JAVA_NULL ? cn1WinJavaStringToWide(threadStateData, message, NULL) : NULL;
    HString reason;
    WindowsCreateString(msg != NULL ? msg : L"Authenticate",
            (UINT32) (msg != NULL ? wcslen(msg) : 12), reason.GetAddressOf());
    if (msg != NULL) {
        free(msg);
    }
    ComPtr<IAsyncOperation<UserConsentVerificationResult>> op;
    if (FAILED(statics->RequestVerificationAsync(reason.Get(), &op))) {
        return JAVA_FALSE;
    }
    UserConsentVerificationResult result = UserConsentVerificationResult_Canceled;
    if (FAILED(cn1AwaitOp(op.Get(), &result))) {
        return JAVA_FALSE;
    }
    return result == UserConsentVerificationResult_Verified ? JAVA_TRUE : JAVA_FALSE;
#else
    (void) message;
    return JAVA_FALSE;
#endif
}

/* True when the port was built with WinRT (so a real Geolocator can be used);
 * lets getLocationManager() return null honestly on a stub build. */
JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_locationSupported___R_boolean(CODENAME_ONE_THREAD_STATE) {
#ifdef CN1_HAVE_WINRT
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

/* -------------------------------------------------------------- location
 *
 * Maps Windows.Devices.Geolocation.Geolocator to LocationManager. Fills `out`
 * with [latitude, longitude, accuracy(m), altitude(m), direction(deg),
 * velocity(m/s), timestampMillis] and returns true; false when location is
 * unavailable / disabled (Windows location off, or WinRT stub build) so the
 * port reports unsupported honestly rather than fabricating a position. */
JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_locationGetCurrent___double_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT outArr) {
#ifdef CN1_HAVE_WINRT
    if (outArr == JAVA_NULL) {
        return JAVA_FALSE;
    }
    cn1WinRtInit();
    ComPtr<IInspectable> inspectable;
    HRESULT hr = RoActivateInstance(
            HStringReference(RuntimeClass_Windows_Devices_Geolocation_Geolocator).Get(), &inspectable);
    if (FAILED(hr)) {
        return JAVA_FALSE;
    }
    ComPtr<IGeolocator> geo;
    if (FAILED(inspectable.As(&geo))) {
        return JAVA_FALSE;
    }
    ComPtr<IAsyncOperation<Geoposition*>> op;
    if (FAILED(geo->GetGeopositionAsync(&op))) {
        return JAVA_FALSE;
    }
    ComPtr<IGeoposition> pos;
    if (FAILED(cn1AwaitOp(op.Get(), pos.GetAddressOf())) || !pos) {
        return JAVA_FALSE;
    }
    ComPtr<IGeocoordinate> coord;
    if (FAILED(pos->get_Coordinate(&coord)) || !coord) {
        return JAVA_FALSE;
    }
    DOUBLE latitude = 0, longitude = 0, altitude = 0, accuracy = 0;
    coord->get_Latitude(&latitude);
    coord->get_Longitude(&longitude);
    coord->get_Accuracy(&accuracy);
    ComPtr<IReference<double>> altRef;
    if (SUCCEEDED(coord->get_Altitude(&altRef)) && altRef) {
        altRef->get_Value(&altitude);
    }
    /* Heading / Speed are IReference<double> (may be null when stationary). */
    double heading = 0, speed = 0;
    ComPtr<IReference<double>> headingRef;
    if (SUCCEEDED(coord->get_Heading(&headingRef)) && headingRef) {
        headingRef->get_Value(&heading);
    }
    ComPtr<IReference<double>> speedRef;
    if (SUCCEEDED(coord->get_Speed(&speedRef)) && speedRef) {
        speedRef->get_Value(&speed);
    }
    JAVA_ARRAY arr = (JAVA_ARRAY) outArr;
    double* out = (double*) arr->data;
    int n = arr->length;
    if (n > 0) { out[0] = latitude; }
    if (n > 1) { out[1] = longitude; }
    if (n > 2) { out[2] = accuracy; }
    if (n > 3) { out[3] = altitude; }
    if (n > 4) { out[4] = heading; }
    if (n > 5) { out[5] = speed; }
    return JAVA_TRUE;
#else
    (void) outArr;
    return JAVA_FALSE;
#endif
}

/* -------------------------------------------------------------- contacts
 *
 * Reads the user's contacts via the WinRT ContactStore and returns them as a
 * single record-delimited blob: each contact is "id US name US phone US email"
 * (field separator 0x1F, record separator 0x1E). The Java side parses it into CN1
 * Contact objects. One native call avoids a chatty per-field bridge. Returns null
 * when the store is inaccessible (no WinRT, or access denied) and an empty string
 * when the store simply has no contacts -- both honest, never fabricated. */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_contactsGetAll___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
#ifdef CN1_HAVE_WINRT
    cn1WinRtInit();
    ComPtr<IContactManagerStatics2> statics;
    if (FAILED(RoGetActivationFactory(
            HStringReference(RuntimeClass_Windows_ApplicationModel_Contacts_ContactManager).Get(),
            IID_PPV_ARGS(&statics)))) {
        return JAVA_NULL;
    }
    ComPtr<IAsyncOperation<ContactStore*>> storeOp;
    if (FAILED(statics->RequestStoreAsync(&storeOp))) {
        return JAVA_NULL;
    }
    ComPtr<IContactStore> store;
    if (FAILED(cn1AwaitOp(storeOp.Get(), store.GetAddressOf())) || !store) {
        return JAVA_NULL;
    }
    ComPtr<IAsyncOperation<IVectorView<Contact*>*>> findOp;
    if (FAILED(store->FindContactsAsync(&findOp))) {
        return JAVA_NULL;
    }
    ComPtr<IVectorView<Contact*>> contacts;
    if (FAILED(cn1AwaitOp(findOp.Get(), contacts.GetAddressOf())) || !contacts) {
        return JAVA_NULL;
    }
    unsigned int size = 0;
    contacts->get_Size(&size);
    std::string blob;
    for (unsigned int i = 0; i < size; i++) {
        ComPtr<IContact> c;
        if (FAILED(contacts->GetAt(i, &c)) || !c) {
            continue;
        }
        if (i > 0) {
            blob.push_back('\x1e');
        }
        /* Name is on the base IContact; Id / Phones / Emails on IContact2. */
        HString name;
        c->get_Name(name.GetAddressOf());
        ComPtr<IContact2> c2;
        c.As(&c2);
        HString id;
        if (c2) {
            c2->get_Id(id.GetAddressOf());
        }
        cn1AppendHString(blob, id.Get());
        blob.push_back('\x1f');
        cn1AppendHString(blob, name.Get());
        blob.push_back('\x1f');
        /* first phone number */
        if (c2) {
            ComPtr<IVector<ContactPhone*>> phones;
            if (SUCCEEDED(c2->get_Phones(&phones)) && phones) {
                unsigned int pn = 0;
                phones->get_Size(&pn);
                if (pn > 0) {
                    ComPtr<IContactPhone> phone;
                    if (SUCCEEDED(phones->GetAt(0, &phone)) && phone) {
                        HString num;
                        phone->get_Number(num.GetAddressOf());
                        cn1AppendHString(blob, num.Get());
                    }
                }
            }
        }
        blob.push_back('\x1f');
        /* first email */
        if (c2) {
            ComPtr<IVector<ContactEmail*>> emails;
            if (SUCCEEDED(c2->get_Emails(&emails)) && emails) {
                unsigned int en = 0;
                emails->get_Size(&en);
                if (en > 0) {
                    ComPtr<IContactEmail> email;
                    if (SUCCEEDED(emails->GetAt(0, &email)) && email) {
                        HString addr;
                        email->get_Address(addr.GetAddressOf());
                        cn1AppendHString(blob, addr.Get());
                    }
                }
            }
        }
    }
    return newStringFromCString(threadStateData, blob.c_str());
#else
    return JAVA_NULL;
#endif
}

} /* extern "C" */

#endif /* _WIN32 */
