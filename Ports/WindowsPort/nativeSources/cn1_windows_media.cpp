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
 * Media playback for the Windows port, backed by the Media Foundation Media
 * Engine (IMFMediaEngine -- the same HTML5-<video>-style engine Edge uses). It
 * plays anything MF can demux/decode (wav/mp3/aac/mp4/...) and exposes the small
 * surface com.codename1.media.Media needs: play/pause/seek/duration/volume/state.
 *
 * Threading: the engine owns its own worker threads and signals readiness/ended
 * through an IMFMediaEngineNotify callback (events arrive on MF threads, so the
 * flags are touched with Interlocked*). Java polls the flags via the bridge.
 *
 * Include order matters: the Media Foundation + WRL headers MUST precede
 * cn1_windows.h. cn1_globals.h (pulled in by cn1_windows.h) installs macros for
 * the bytecode runtime that otherwise break the C++ standard library and SDK
 * headers -- see the WebView2 peer (cn1_windows_browser.cpp) for the same rule.
 */

#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <mfapi.h>
#include <mfmediaengine.h>
#include <mferror.h>
#include <wrl/client.h>
#include <wrl/implements.h>
#include <stdlib.h>
#include <wchar.h>

#include "cn1_windows.h"

using Microsoft::WRL::ComPtr;
using Microsoft::WRL::Make;
using Microsoft::WRL::RuntimeClass;
using Microsoft::WRL::RuntimeClassFlags;
using Microsoft::WRL::ClassicCom;

struct CN1Media;

/* Media Engine event sink. Only the readiness/ended/error transitions are needed;
 * everything else is queried on demand. */
class CN1MediaNotify : public RuntimeClass<RuntimeClassFlags<ClassicCom>, IMFMediaEngineNotify> {
public:
    CN1Media* owner;
    CN1MediaNotify() : owner(NULL) {}
    STDMETHODIMP EventNotify(DWORD event, DWORD_PTR param1, DWORD param2) override;
};

struct CN1Media {
    ComPtr<IMFMediaEngine> engine;
    ComPtr<CN1MediaNotify> notify;
    wchar_t tempPath[MAX_PATH];
    volatile LONG ready;
    volatile LONG ended;
    volatile LONG errored;
    CN1Media() : ready(0), ended(0), errored(0) {
        tempPath[0] = L'\0';
    }
};

STDMETHODIMP CN1MediaNotify::EventNotify(DWORD event, DWORD_PTR param1, DWORD param2) {
    if (owner == NULL) {
        return S_OK;
    }
    switch (event) {
        case MF_MEDIA_ENGINE_EVENT_CANPLAY:
        case MF_MEDIA_ENGINE_EVENT_CANPLAYTHROUGH:
            InterlockedExchange(&owner->ready, 1);
            break;
        case MF_MEDIA_ENGINE_EVENT_ENDED:
            InterlockedExchange(&owner->ended, 1);
            break;
        case MF_MEDIA_ENGINE_EVENT_ERROR:
            InterlockedExchange(&owner->errored, 1);
            break;
        default:
            break;
    }
    return S_OK;
}

static bool g_mfStarted = false;

static void cn1MediaEnsureStarted() {
    if (!g_mfStarted) {
        /* The calling (EDT) thread needs COM for CoCreateInstance; ignore an
         * already-initialised apartment (S_FALSE / RPC_E_CHANGED_MODE). */
        CoInitializeEx(NULL, COINIT_MULTITHREADED);
        if (SUCCEEDED(MFStartup(MF_VERSION, MFSTARTUP_FULL))) {
            g_mfStarted = true;
        }
    }
}

/* Picks a file extension from the mime type so the MF source resolver gets a
 * hint; defaults to the raw bytes' own sniffing when unknown. */
static const wchar_t* cn1MediaExtForMime(const wchar_t* mime) {
    if (mime == NULL) {
        return L".dat";
    }
    if (wcsstr(mime, L"wav") != NULL) { return L".wav"; }
    if (wcsstr(mime, L"mpeg") != NULL || wcsstr(mime, L"mp3") != NULL) { return L".mp3"; }
    if (wcsstr(mime, L"aac") != NULL) { return L".aac"; }
    if (wcsstr(mime, L"mp4") != NULL) { return L".mp4"; }
    if (wcsstr(mime, L"ogg") != NULL) { return L".ogg"; }
    if (wcsstr(mime, L"webm") != NULL) { return L".webm"; }
    return L".dat";
}

/* Spools the media bytes to a uniquely named temp file (MF plays from a URL/path,
 * not an arbitrary Java stream). Returns 1 on success. */
static int cn1MediaSpoolTemp(const JAVA_ARRAY_BYTE* data, int len, const wchar_t* ext, wchar_t* out, int outLen) {
    wchar_t dir[MAX_PATH];
    DWORD n = GetTempPathW(MAX_PATH, dir);
    if (n == 0 || n >= MAX_PATH) {
        return 0;
    }
    wchar_t base[MAX_PATH];
    if (GetTempFileNameW(dir, L"cn1", 0, base) == 0) {
        return 0;
    }
    /* GetTempFileNameW creates a .tmp file; append the media extension so MF
     * resolves the right source. */
    if ((int) (wcslen(base) + wcslen(ext) + 1) > outLen) {
        return 0;
    }
    wcscpy(out, base);
    wcscat(out, ext);
    HANDLE h = CreateFileW(out, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_TEMPORARY, NULL);
    if (h == INVALID_HANDLE_VALUE) {
        return 0;
    }
    int ok = 1;
    if (len > 0 && data != NULL) {
        DWORD written = 0;
        if (!WriteFile(h, data, (DWORD) len, &written, NULL) || written != (DWORD) len) {
            ok = 0;
        }
    }
    CloseHandle(h);
    if (!ok) {
        DeleteFileW(out);
    }
    return ok;
}

extern "C" {

JAVA_LONG com_codename1_impl_windows_WindowsNative_mediaCreate___byte_1ARRAY_int_java_lang_String_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT dataArr, JAVA_INT len, JAVA_OBJECT mimeObj) {
    cn1MediaEnsureStarted();
    if (!g_mfStarted || dataArr == NULL) {
        return 0;
    }
    const JAVA_ARRAY_BYTE* data = (const JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY) dataArr).data;
    WCHAR* mime = cn1WinJavaStringToWide(threadStateData, mimeObj, NULL);
    const wchar_t* ext = cn1MediaExtForMime(mime);

    CN1Media* m = new CN1Media();
    int spooled = cn1MediaSpoolTemp(data, (int) len, ext, m->tempPath, MAX_PATH);
    free(mime);
    if (!spooled) {
        delete m;
        return 0;
    }

    ComPtr<IMFMediaEngineClassFactory> factory;
    HRESULT hr = CoCreateInstance(CLSID_MFMediaEngineClassFactory, NULL, CLSCTX_INPROC_SERVER,
            IID_PPV_ARGS(&factory));
    if (FAILED(hr) || !factory) {
        DeleteFileW(m->tempPath);
        delete m;
        return 0;
    }

    m->notify = Make<CN1MediaNotify>();
    if (!m->notify) {
        DeleteFileW(m->tempPath);
        delete m;
        return 0;
    }
    m->notify->owner = m;

    ComPtr<IMFAttributes> attrs;
    if (FAILED(MFCreateAttributes(&attrs, 1))) {
        DeleteFileW(m->tempPath);
        delete m;
        return 0;
    }
    attrs->SetUnknown(MF_MEDIA_ENGINE_CALLBACK, m->notify.Get());

    /* Audio-only engine: no DXGI manager / swap chain required. Video sources
     * still decode (HasVideo() reports true); a future video peer would create
     * the engine with a DXGI manager and call TransferVideoFrame. */
    hr = factory->CreateInstance(MF_MEDIA_ENGINE_AUDIOONLY, attrs.Get(), &m->engine);
    if (FAILED(hr) || !m->engine) {
        DeleteFileW(m->tempPath);
        delete m;
        return 0;
    }

    BSTR url = SysAllocString(m->tempPath);
    if (url == NULL) {
        DeleteFileW(m->tempPath);
        delete m;
        return 0;
    }
    hr = m->engine->SetSource(url);
    SysFreeString(url);
    if (FAILED(hr)) {
        DeleteFileW(m->tempPath);
        delete m;
        return 0;
    }
    return (JAVA_LONG) (intptr_t) m;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_mediaPlay___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG h) {
    CN1Media* m = (CN1Media*) (intptr_t) h;
    if (m && m->engine) {
        InterlockedExchange(&m->ended, 0);
        m->engine->Play();
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_mediaPause___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG h) {
    CN1Media* m = (CN1Media*) (intptr_t) h;
    if (m && m->engine) {
        m->engine->Pause();
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_mediaSetTime___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG h, JAVA_INT ms) {
    CN1Media* m = (CN1Media*) (intptr_t) h;
    if (m && m->engine) {
        m->engine->SetCurrentTime((double) ms / 1000.0);
    }
}

JAVA_INT com_codename1_impl_windows_WindowsNative_mediaGetTime___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG h) {
    CN1Media* m = (CN1Media*) (intptr_t) h;
    if (m && m->engine) {
        double t = m->engine->GetCurrentTime();
        if (t > 0.0 && t == t) {
            return (JAVA_INT) (t * 1000.0);
        }
    }
    return 0;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_mediaGetDuration___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG h) {
    CN1Media* m = (CN1Media*) (intptr_t) h;
    if (m && m->engine) {
        double d = m->engine->GetDuration();
        /* GetDuration is NaN until metadata loads and +Inf for live streams. */
        if (d > 0.0 && d == d && d < 1.0e9) {
            return (JAVA_INT) (d * 1000.0);
        }
    }
    return 0;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_mediaSetVolume___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG h, JAVA_INT vol) {
    CN1Media* m = (CN1Media*) (intptr_t) h;
    if (m && m->engine) {
        double v = (double) vol / 100.0;
        if (v < 0.0) { v = 0.0; }
        if (v > 1.0) { v = 1.0; }
        m->engine->SetVolume(v);
    }
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_mediaIsPlaying___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG h) {
    CN1Media* m = (CN1Media*) (intptr_t) h;
    if (m && m->engine) {
        return (!m->engine->IsPaused() && !m->engine->IsEnded()) ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_mediaIsEnded___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG h) {
    CN1Media* m = (CN1Media*) (intptr_t) h;
    if (m && m->engine) {
        return (m->engine->IsEnded() || InterlockedCompareExchange(&m->ended, 0, 0)) ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_mediaIsVideo___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG h) {
    CN1Media* m = (CN1Media*) (intptr_t) h;
    if (m && m->engine) {
        return m->engine->HasVideo() ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_mediaDestroy___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG h) {
    CN1Media* m = (CN1Media*) (intptr_t) h;
    if (m == NULL) {
        return;
    }
    if (m->engine) {
        m->engine->Shutdown();
    }
    if (m->notify) {
        m->notify->owner = NULL;
    }
    if (m->tempPath[0] != L'\0') {
        DeleteFileW(m->tempPath);
    }
    delete m;
}

} /* extern "C" */

#endif /* _WIN32 */
