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
#include <wrl.h>
#include <wrl/event.h>
#include <wrl/wrappers/corewrappers.h>
#include <windows.foundation.h>
#include <windows.security.credentials.ui.h>

using namespace ABI::Windows::Foundation;
using namespace ABI::Windows::Security::Credentials::UI;
using namespace Microsoft::WRL;
using namespace Microsoft::WRL::Wrappers;

/* Blocks the calling (CN1 worker) thread until a WinRT IAsyncOperation<T>
 * completes, then fetches its result. Used for the inherently-async WinRT APIs;
 * callers invoke these natives off the EDT (via AsyncResource) so blocking is
 * fine. */
template <typename T>
static HRESULT cn1AwaitOp(IAsyncOperation<T>* op, T* result) {
    Event done(CreateEventExW(nullptr, nullptr, 0, EVENT_ALL_ACCESS));
    if (!done.IsValid()) {
        return E_FAIL;
    }
    HRESULT hr = op->put_Completed(Callback<IAsyncOperationCompletedHandler<T>>(
            [&done](IAsyncOperation<T>*, AsyncStatus) {
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

} /* extern "C" */

#endif /* _WIN32 */
