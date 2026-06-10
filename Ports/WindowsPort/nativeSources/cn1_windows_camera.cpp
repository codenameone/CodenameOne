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
 * Camera still capture for the Windows port (the legacy Capture API:
 * capturePhoto). A desktop has no built-in camera-capture UI, so this grabs a
 * single frame from the default webcam through Media Foundation -- the honest
 * desktop equivalent of a snapshot -- and hands the pixels back to Java, which
 * builds an Image and writes the file. The richer com.codename1.camera CameraImpl
 * (live preview peer, video) needs the generic native-peer placement that the
 * port does not have yet, so createCameraImpl() stays null; this covers the
 * common "take a photo" path.
 *
 * A few frames are discarded first so auto-exposure / white-balance settle before
 * the captured frame. RGB32 from MF is BGRA in memory and may be bottom-up
 * (negative stride); both are handled when packing into CN1's 0xAARRGGBB ints.
 */

#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <mfapi.h>
#include <mfidl.h>
#include <mfreadwrite.h>
#include <mferror.h>
#include <wrl/client.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "cn1_windows.h"

using Microsoft::WRL::ComPtr;

extern "C" {

extern struct clazz class_array1__JAVA_INT;
extern struct clazz class_array1__JAVA_BYTE;

static bool cn1CameraEnsureMF() {
    static bool mfStarted = false;
    if (!mfStarted) {
        CoInitializeEx(NULL, COINIT_MULTITHREADED);
        if (FAILED(MFStartup(MF_VERSION, MFSTARTUP_LITE))) {
            return false;
        }
        mfStarted = true;
    }
    return true;
}

/*
 * Captures one frame from the default video capture device and returns it as a
 * CN1 ARGB int[] (length width*height), filling outDims[0]=width, [1]=height.
 * Returns JAVA_NULL when there is no camera or capture fails.
 */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_cameraCaptureFrame___int_1ARRAY_R_int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT outDims) {
    static bool mfStarted = false;
    if (!mfStarted) {
        CoInitializeEx(NULL, COINIT_MULTITHREADED);
        if (FAILED(MFStartup(MF_VERSION, MFSTARTUP_LITE))) {
            return JAVA_NULL;
        }
        mfStarted = true;
    }

    /* Enumerate video capture devices and activate the first. */
    ComPtr<IMFAttributes> attr;
    if (FAILED(MFCreateAttributes(&attr, 1))) {
        return JAVA_NULL;
    }
    attr->SetGUID(MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE, MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE_VIDCAP_GUID);
    IMFActivate** devices = NULL;
    UINT32 count = 0;
    if (FAILED(MFEnumDeviceSources(attr.Get(), &devices, &count)) || count == 0) {
        if (devices) {
            CoTaskMemFree(devices);
        }
        return JAVA_NULL;
    }
    ComPtr<IMFMediaSource> source;
    HRESULT hr = devices[0]->ActivateObject(IID_PPV_ARGS(&source));
    for (UINT32 i = 0; i < count; i++) {
        if (devices[i]) {
            devices[i]->Release();
        }
    }
    CoTaskMemFree(devices);
    if (FAILED(hr) || !source) {
        return JAVA_NULL;
    }

    ComPtr<IMFSourceReader> reader;
    if (FAILED(MFCreateSourceReaderFromMediaSource(source.Get(), NULL, &reader))) {
        source->Shutdown();
        return JAVA_NULL;
    }

    /* Ask the reader to decode to 32-bit RGB so we get packed BGRA frames. */
    ComPtr<IMFMediaType> outType;
    MFCreateMediaType(&outType);
    outType->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);
    outType->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_RGB32);
    reader->SetCurrentMediaType((DWORD) MF_SOURCE_READER_FIRST_VIDEO_STREAM, NULL, outType.Get());

    /* Frame dimensions from the negotiated type. */
    ComPtr<IMFMediaType> current;
    UINT32 width = 0, height = 0;
    if (SUCCEEDED(reader->GetCurrentMediaType((DWORD) MF_SOURCE_READER_FIRST_VIDEO_STREAM, &current)) && current) {
        MFGetAttributeSize(current.Get(), MF_MT_FRAME_SIZE, &width, &height);
    }
    if (width == 0 || height == 0) {
        source->Shutdown();
        return JAVA_NULL;
    }

    JAVA_OBJECT result = JAVA_NULL;
    /* Read a few frames so exposure settles, then keep the first one with data. */
    for (int frame = 0; frame < 12 && result == JAVA_NULL; frame++) {
        DWORD streamIndex = 0, flags = 0;
        LONGLONG timestamp = 0;
        ComPtr<IMFSample> sample;
        if (FAILED(reader->ReadSample((DWORD) MF_SOURCE_READER_FIRST_VIDEO_STREAM, 0,
                &streamIndex, &flags, &timestamp, &sample))) {
            break;
        }
        if ((flags & MF_SOURCE_READERF_ENDOFSTREAM) != 0) {
            break;
        }
        if (!sample || frame < 4) {
            continue; /* discard the first few frames */
        }
        ComPtr<IMFMediaBuffer> buffer;
        if (FAILED(sample->ConvertToContiguousBuffer(&buffer)) || !buffer) {
            continue;
        }
        BYTE* data = NULL;
        DWORD maxLen = 0, curLen = 0;
        if (FAILED(buffer->Lock(&data, &maxLen, &curLen)) || data == NULL) {
            continue;
        }
        if (curLen >= width * height * 4) {
            result = allocArray(threadStateData, (int) (width * height), &class_array1__JAVA_INT,
                    sizeof(JAVA_ARRAY_INT), 1);
            if (result != JAVA_NULL) {
                JAVA_ARRAY_INT* argb = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) result).data;
                int stride = (int) (width * 4);
                for (UINT32 y = 0; y < height; y++) {
                    /* MFVideoFormat_RGB32 is bottom-up: row 0 is the last in memory. */
                    const BYTE* row = data + (size_t) (height - 1 - y) * stride;
                    JAVA_ARRAY_INT* dst = argb + (size_t) y * width;
                    for (UINT32 x = 0; x < width; x++) {
                        BYTE b = row[x * 4 + 0];
                        BYTE g = row[x * 4 + 1];
                        BYTE r = row[x * 4 + 2];
                        dst[x] = (JAVA_ARRAY_INT) (0xff000000u | (r << 16) | (g << 8) | b);
                    }
                }
            }
        }
        buffer->Unlock();
    }

    source->Shutdown();

    if (result != JAVA_NULL && outDims != JAVA_NULL) {
        JAVA_ARRAY_INT* dims = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) outDims).data;
        int dlen = (*(JAVA_ARRAY) outDims).length;
        if (dlen > 0) { dims[0] = (JAVA_ARRAY_INT) width; }
        if (dlen > 1) { dims[1] = (JAVA_ARRAY_INT) height; }
    }
    return result;
}

/* ------------- Continuous capture session (com.codename1.camera CameraImpl) ----
 *
 * A worker thread runs an IMFSourceReader loop and keeps the most recent frame
 * as a CN1 ARGB buffer behind a lock. Java polls cameraSessionLatestFrame for the
 * preview peer (image-based, like the WebView2 peer -- so it renders in the
 * offscreen screenshot pipeline as well as in a live window) and for takePhoto /
 * the frame listener. No native->Java callback is used: ParparVM threads must be
 * attached to call into Java, so polling the latest frame from the EDT is both
 * simpler and robust. Honest desktop semantics -- a real webcam frame, never
 * synthetic; flash/zoom/focus/video are reported unsupported on the Java side.
 */
struct CN1CameraSession {
    CRITICAL_SECTION lock;
    HANDLE thread;
    volatile LONG running;
    volatile LONG paused;
    int reqW, reqH, deviceIndex;
    JAVA_ARRAY_INT* latest;   /* width*height ARGB, malloc'd; NULL until first frame */
    int latestW, latestH;
    CN1CameraSession() : thread(NULL), running(1), paused(0), reqW(0), reqH(0),
            deviceIndex(0), latest(NULL), latestW(0), latestH(0) {
        InitializeCriticalSection(&lock);
    }
    ~CN1CameraSession() { if (latest) free(latest); DeleteCriticalSection(&lock); }
};

/* Activate the Nth video capture device and build an RGB32 source reader. */
static bool cn1CameraOpenReader(int deviceIndex, ComPtr<IMFMediaSource>& source,
        ComPtr<IMFSourceReader>& reader, UINT32& width, UINT32& height) {
    ComPtr<IMFAttributes> attr;
    if (FAILED(MFCreateAttributes(&attr, 1))) return false;
    attr->SetGUID(MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE, MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE_VIDCAP_GUID);
    IMFActivate** devices = NULL;
    UINT32 count = 0;
    if (FAILED(MFEnumDeviceSources(attr.Get(), &devices, &count)) || count == 0) {
        if (devices) CoTaskMemFree(devices);
        return false;
    }
    int idx = (deviceIndex >= 0 && (UINT32) deviceIndex < count) ? deviceIndex : 0;
    HRESULT hr = devices[idx]->ActivateObject(IID_PPV_ARGS(&source));
    for (UINT32 i = 0; i < count; i++) { if (devices[i]) devices[i]->Release(); }
    CoTaskMemFree(devices);
    if (FAILED(hr) || !source) return false;
    if (FAILED(MFCreateSourceReaderFromMediaSource(source.Get(), NULL, &reader))) {
        source->Shutdown();
        return false;
    }
    ComPtr<IMFMediaType> outType;
    MFCreateMediaType(&outType);
    outType->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);
    outType->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_RGB32);
    reader->SetCurrentMediaType((DWORD) MF_SOURCE_READER_FIRST_VIDEO_STREAM, NULL, outType.Get());
    ComPtr<IMFMediaType> current;
    width = 0; height = 0;
    if (SUCCEEDED(reader->GetCurrentMediaType((DWORD) MF_SOURCE_READER_FIRST_VIDEO_STREAM, &current)) && current) {
        MFGetAttributeSize(current.Get(), MF_MT_FRAME_SIZE, &width, &height);
    }
    if (width == 0 || height == 0) { source->Shutdown(); return false; }
    return true;
}

static DWORD WINAPI cn1CameraWorker(LPVOID param) {
    CN1CameraSession* s = (CN1CameraSession*) param;
    CoInitializeEx(NULL, COINIT_MULTITHREADED);
    cn1CameraEnsureMF();
    ComPtr<IMFMediaSource> source;
    ComPtr<IMFSourceReader> reader;
    UINT32 width = 0, height = 0;
    if (!cn1CameraOpenReader(s->deviceIndex, source, reader, width, height)) {
        CoUninitialize();
        return 0;
    }
    while (InterlockedCompareExchange(&s->running, 1, 1)) {
        if (InterlockedCompareExchange(&s->paused, 0, 0)) { Sleep(30); continue; }
        DWORD streamIndex = 0, flags = 0;
        LONGLONG timestamp = 0;
        ComPtr<IMFSample> sample;
        if (FAILED(reader->ReadSample((DWORD) MF_SOURCE_READER_FIRST_VIDEO_STREAM, 0,
                &streamIndex, &flags, &timestamp, &sample))) {
            break;
        }
        if ((flags & MF_SOURCE_READERF_ENDOFSTREAM) != 0) break;
        if (!sample) continue;
        ComPtr<IMFMediaBuffer> buffer;
        if (FAILED(sample->ConvertToContiguousBuffer(&buffer)) || !buffer) continue;
        BYTE* data = NULL;
        DWORD maxLen = 0, curLen = 0;
        if (FAILED(buffer->Lock(&data, &maxLen, &curLen)) || data == NULL) continue;
        if (curLen >= width * height * 4) {
            JAVA_ARRAY_INT* frame = (JAVA_ARRAY_INT*) malloc((size_t) width * height * sizeof(JAVA_ARRAY_INT));
            if (frame) {
                int stride = (int) (width * 4);
                for (UINT32 y = 0; y < height; y++) {
                    const BYTE* row = data + (size_t) (height - 1 - y) * stride; /* RGB32 is bottom-up */
                    JAVA_ARRAY_INT* dst = frame + (size_t) y * width;
                    for (UINT32 x = 0; x < width; x++) {
                        BYTE b = row[x * 4 + 0], g = row[x * 4 + 1], r = row[x * 4 + 2];
                        dst[x] = (JAVA_ARRAY_INT) (0xff000000u | (r << 16) | (g << 8) | b);
                    }
                }
                EnterCriticalSection(&s->lock);
                if (s->latest) free(s->latest);
                s->latest = frame; s->latestW = (int) width; s->latestH = (int) height;
                LeaveCriticalSection(&s->lock);
            }
        }
        buffer->Unlock();
    }
    source->Shutdown();
    CoUninitialize();
    return 0;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_cameraSessionStart___int_int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_INT deviceIndex, JAVA_INT reqW, JAVA_INT reqH) {
    CN1CameraSession* s = new CN1CameraSession();
    s->deviceIndex = deviceIndex; s->reqW = reqW; s->reqH = reqH;
    s->thread = CreateThread(NULL, 0, cn1CameraWorker, s, 0, NULL);
    if (!s->thread) { delete s; return 0; }
    return (JAVA_LONG) (intptr_t) s;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_cameraSessionStop___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1CameraSession* s = (CN1CameraSession*) (intptr_t) handle;
    if (!s) return;
    InterlockedExchange(&s->running, 0);
    if (s->thread) { WaitForSingleObject(s->thread, 5000); CloseHandle(s->thread); }
    delete s;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_cameraSessionSetPaused___long_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_BOOLEAN paused) {
    CN1CameraSession* s = (CN1CameraSession*) (intptr_t) handle;
    if (!s) return;
    InterlockedExchange(&s->paused, paused ? 1 : 0);
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_cameraSessionLatestFrame___long_int_1ARRAY_R_int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT outDims) {
    CN1CameraSession* s = (CN1CameraSession*) (intptr_t) handle;
    if (!s) return JAVA_NULL;
    JAVA_OBJECT result = JAVA_NULL;
    EnterCriticalSection(&s->lock);
    if (s->latest && s->latestW > 0 && s->latestH > 0) {
        int n = s->latestW * s->latestH;
        result = allocArray(threadStateData, n, &class_array1__JAVA_INT, sizeof(JAVA_ARRAY_INT), 1);
        if (result != JAVA_NULL) {
            memcpy((*(JAVA_ARRAY) result).data, s->latest, (size_t) n * sizeof(JAVA_ARRAY_INT));
            if (outDims != JAVA_NULL) {
                JAVA_ARRAY_INT* dims = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) outDims).data;
                int dlen = (*(JAVA_ARRAY) outDims).length;
                if (dlen > 0) dims[0] = s->latestW;
                if (dlen > 1) dims[1] = s->latestH;
            }
        }
    }
    LeaveCriticalSection(&s->lock);
    return result;
}

/* Friendly names of the video capture devices: "name|external|0|0;name|...". */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_cameraEnumerate___R_java_lang_String(
        CODENAME_ONE_THREAD_STATE) {
    if (!cn1CameraEnsureMF()) return newStringFromCString(threadStateData, "");
    ComPtr<IMFAttributes> attr;
    if (FAILED(MFCreateAttributes(&attr, 1))) return newStringFromCString(threadStateData, "");
    attr->SetGUID(MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE, MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE_VIDCAP_GUID);
    IMFActivate** devices = NULL;
    UINT32 count = 0;
    if (FAILED(MFEnumDeviceSources(attr.Get(), &devices, &count)) || count == 0) {
        if (devices) CoTaskMemFree(devices);
        return newStringFromCString(threadStateData, "");
    }
    char buf[4096];
    int pos = 0;
    buf[0] = '\0';
    for (UINT32 i = 0; i < count; i++) {
        WCHAR* name = NULL;
        UINT32 nlen = 0;
        if (SUCCEEDED(devices[i]->GetAllocatedString(MF_DEVSOURCE_ATTRIBUTE_FRIENDLY_NAME, &name, &nlen)) && name) {
            char utf8[512];
            WideCharToMultiByte(CP_UTF8, 0, name, -1, utf8, (int) sizeof(utf8), NULL, NULL);
            CoTaskMemFree(name);
            for (char* p = utf8; *p; p++) { if (*p == '|' || *p == ';') *p = ' '; }
            int need = snprintf(buf + pos, sizeof(buf) - pos, "%s%s|external|0|0",
                    pos > 0 ? ";" : "", utf8);
            if (need > 0 && pos + need < (int) sizeof(buf)) { pos += need; }
        }
        devices[i]->Release();
    }
    CoTaskMemFree(devices);
    return newStringFromCString(threadStateData, buf);
}

} /* extern "C" */

#endif /* _WIN32 */
