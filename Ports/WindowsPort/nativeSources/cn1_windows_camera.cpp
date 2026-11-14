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
 * builds an Image and writes the file. The continuous capture session further
 * down backs the com.codename1.camera CameraImpl (WindowsCameraImpl): live
 * preview, stills, zoom/focus through IAMCameraControl and H.264 MP4 recording
 * through an IMFSinkWriter.
 *
 * A few frames are discarded first so auto-exposure / white-balance settle before
 * the captured frame. RGB32 from MF is BGRA in memory and may be bottom-up
 * (negative stride); both are handled when packing into CN1's 0xAARRGGBB ints.
 */

#if defined(_WIN32) && !defined(CN1_WINDOWS_UWP) /* desktop only: stubbed for UWP in cn1_windows_uwp_stubs.c */

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <mfapi.h>
#include <mfidl.h>
#include <mfreadwrite.h>
#include <mferror.h>
#include <strmif.h>   /* IAMCameraControl: zoom / focus on a UVC device */
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
    // Enable advanced video processing so a YUY2/NV12 webcam (the common case) is
    // converted to RGB32; without it the RGB32 request below is ignored and frames
    // come back 2-bpp, failing the size check (verified on a real YUY2 webcam).
    ComPtr<IMFAttributes> readerAttr;
    MFCreateAttributes(&readerAttr, 1);
    readerAttr->SetUINT32(MF_SOURCE_READER_ENABLE_ADVANCED_VIDEO_PROCESSING, TRUE);
    if (FAILED(MFCreateSourceReaderFromMediaSource(source.Get(), readerAttr.Get(), &reader))) {
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
                JAVA_ARRAY_INT* argb = (JAVA_ARRAY_INT*) CN1_ARRAY_DATA(result);
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
        JAVA_ARRAY_INT* dims = (JAVA_ARRAY_INT*) CN1_ARRAY_DATA(outDims);
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
 * synthetic.
 *
 * Zoom and focus go through IAMCameraControl, queried from the session's
 * IMFMediaSource (the MF capture source exposes the UVC camera-terminal controls
 * through it). A device that lacks a control is a quiet no-op.
 *
 * Recording: while a sink writer is attached, the worker hands every RGB32 sample
 * it already reads to IMFSinkWriter, which inserts the colour converter and the
 * H.264 encoder and writes an MP4. Audio, when asked for, comes from a second
 * source reader on the default audio capture device (PCM 16-bit), run on its own
 * thread and encoded to AAC by the same writer. The two capture sources do not
 * share a presentation clock, so both streams are stamped with MFGetSystemTime()
 * on arrival relative to the recording start; that keeps them in sync to within
 * the capture latency of each device.
 */
struct CN1CameraSession {
    CRITICAL_SECTION lock;
    HANDLE thread;
    volatile LONG running;
    volatile LONG paused;
    int reqW, reqH, deviceIndex;
    JAVA_ARRAY_INT* latest;   /* width*height ARGB, malloc'd; NULL until first frame */
    int latestW, latestH;
    /* Set by the worker once the device is open (AddRef'd), under lock. */
    IMFMediaSource* source;
    IMFMediaType* readerType;   /* the reader's negotiated RGB32 output type */
    /* Recording state, under lock. writer is NULL while not recording. */
    IMFSinkWriter* writer;
    DWORD videoStream, audioStream;
    bool hasAudio;
    LONGLONG recStart, lastVideoTs, lastAudioTs;
    LONGLONG frameDuration;
    HANDLE audioThread;
    volatile LONG audioRunning;
    IMFMediaSource* audioSource;
    IMFSourceReader* audioReader;
    CN1CameraSession() : thread(NULL), running(1), paused(0), reqW(0), reqH(0),
            deviceIndex(0), latest(NULL), latestW(0), latestH(0), source(NULL),
            readerType(NULL), writer(NULL), videoStream(0), audioStream(0),
            hasAudio(false), recStart(0), lastVideoTs(-1), lastAudioTs(-1),
            frameDuration(333333), audioThread(NULL), audioRunning(0),
            audioSource(NULL), audioReader(NULL) {
        InitializeCriticalSection(&lock);
    }
    ~CN1CameraSession() {
        if (latest) free(latest);
        if (source) source->Release();
        if (readerType) readerType->Release();
        DeleteCriticalSection(&lock);
    }
};

static bool cn1CameraStopRecording(CN1CameraSession* s);

/* Write one sample to the attached sink writer, stamped with arrival time.
   Caller holds s->lock. */
static void cn1CameraWriteLocked(CN1CameraSession* s, IMFSample* sample, DWORD stream,
        LONGLONG* last, LONGLONG duration) {
    LONGLONG ts = MFGetSystemTime() - s->recStart;
    if (ts < 0) ts = 0;
    if (*last >= 0 && ts <= *last) ts = *last + 1; /* keep each stream monotonic */
    *last = ts;
    sample->SetSampleTime(ts);
    if (duration > 0) sample->SetSampleDuration(duration);
    s->writer->WriteSample(stream, sample);
}

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
    // Enable the reader's video processor so a YUY2/NV12 webcam (the common case)
    // is converted to RGB32; without it SetCurrentMediaType(RGB32) fails and frames
    // arrive in the native 2-bpp format, failing the width*height*4 size check.
    ComPtr<IMFAttributes> rattr;
    MFCreateAttributes(&rattr, 1);
    rattr->SetUINT32(MF_SOURCE_READER_ENABLE_ADVANCED_VIDEO_PROCESSING, TRUE);
    if (FAILED(MFCreateSourceReaderFromMediaSource(source.Get(), rattr.Get(), &reader))) {
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
    {
        ComPtr<IMFMediaType> cur;
        IMFMediaType* copy = NULL;
        if (SUCCEEDED(reader->GetCurrentMediaType((DWORD) MF_SOURCE_READER_FIRST_VIDEO_STREAM, &cur))
                && SUCCEEDED(MFCreateMediaType(&copy))) {
            cur->CopyAllItems(copy);
        }
        EnterCriticalSection(&s->lock);
        s->source = source.Get();
        s->source->AddRef();
        s->readerType = copy;
        LeaveCriticalSection(&s->lock);
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
        EnterCriticalSection(&s->lock);
        if (s->writer) {
            cn1CameraWriteLocked(s, sample.Get(), s->videoStream, &s->lastVideoTs, s->frameDuration);
        }
        LeaveCriticalSection(&s->lock);
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
    /* Finalize a recording still running so its file is playable. */
    CN1_YIELD_THREAD;
    cn1CameraStopRecording(s);
    CN1_RESUME_THREAD;
    InterlockedExchange(&s->running, 0);
    if (s->thread) {
        /* Parked for the wait: Windows cannot force-stop a thread, so a collection would
           otherwise wait out up to five seconds for this one. */
        CN1_YIELD_THREAD;
        WaitForSingleObject(s->thread, 5000);
        CN1_RESUME_THREAD;
        CloseHandle(s->thread);
    }
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
            memcpy(CN1_ARRAY_DATA(result), s->latest, (size_t) n * sizeof(JAVA_ARRAY_INT));
            if (outDims != JAVA_NULL) {
                JAVA_ARRAY_INT* dims = (JAVA_ARRAY_INT*) CN1_ARRAY_DATA(outDims);
                int dlen = (*(JAVA_ARRAY) outDims).length;
                if (dlen > 0) dims[0] = s->latestW;
                if (dlen > 1) dims[1] = s->latestH;
            }
        }
    }
    LeaveCriticalSection(&s->lock);
    return result;
}

/* ---------------------------- zoom / focus (IAMCameraControl) ---------------- */

/* The session's camera control, or NULL while the device is not open yet or the
   driver exposes none. Caller releases. */
static IAMCameraControl* cn1CameraControl(CN1CameraSession* s) {
    IAMCameraControl* ctl = NULL;
    EnterCriticalSection(&s->lock);
    if (s->source) {
        s->source->QueryInterface(__uuidof(IAMCameraControl), (void**) &ctl);
    }
    LeaveCriticalSection(&s->lock);
    return ctl;
}

static bool cn1CameraHasAutoFocus(IAMCameraControl* ctl) {
    long mn = 0, mx = 0, step = 0, def = 0, caps = 0;
    return ctl && SUCCEEDED(ctl->GetRange(CameraControl_Focus, &mn, &mx, &step, &def, &caps))
            && (caps & CameraControl_Flags_Auto) != 0;
}

/*
 * Zoom ratio (1.0 = none) onto CameraControl_Zoom. UVC reports absolute zoom as
 * a focal length in device units (Windows documents millimetres, 10..600), and a
 * zoom ratio is a ratio of focal lengths, so a positive minimum maps as
 * min * ratio. A driver whose range starts at 0 or below gives no such meaning;
 * its range is spread linearly over 1x..4x. Out-of-range values clamp.
 */
JAVA_VOID com_codename1_impl_windows_WindowsNative_cameraSessionSetZoom___long_float(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_FLOAT ratio) {
    CN1CameraSession* s = (CN1CameraSession*) (intptr_t) handle;
    if (!s) return;
    IAMCameraControl* ctl = cn1CameraControl(s);
    if (!ctl) return;
    long mn = 0, mx = 0, step = 0, def = 0, caps = 0;
    if (SUCCEEDED(ctl->GetRange(CameraControl_Zoom, &mn, &mx, &step, &def, &caps)) && mx > mn) {
        double r = ratio < 1.0f ? 1.0 : (double) ratio;
        double v = mn > 0 ? mn * r : mn + (r - 1.0) / 3.0 * (mx - mn);
        if (v > mx) v = mx;
        if (v < mn) v = mn;
        long value = (long) (v + 0.5);
        if (step > 1) value = mn + ((value - mn) / step) * step;
        ctl->Set(CameraControl_Zoom, value, CameraControl_Flags_Manual);
    }
    ctl->Release();
}

/*
 * UVC has no focus point, so a tap can only ask the lens to refocus: switch the
 * focus control to auto. Drivers that were left in manual focus (or that only
 * refocus on the manual->auto transition) are nudged by re-setting auto. A device
 * without an auto focus control is a quiet no-op.
 */
JAVA_VOID com_codename1_impl_windows_WindowsNative_cameraSessionFocus___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1CameraSession* s = (CN1CameraSession*) (intptr_t) handle;
    if (!s) return;
    IAMCameraControl* ctl = cn1CameraControl(s);
    if (!ctl) return;
    if (cn1CameraHasAutoFocus(ctl)) {
        long value = 0, flags = 0;
        if (FAILED(ctl->Get(CameraControl_Focus, &value, &flags))) value = 0;
        ctl->Set(CameraControl_Focus, value, CameraControl_Flags_Auto);
    }
    ctl->Release();
}

/* ------------------------------ video recording ------------------------------ */

/* Opens the default audio capture device as a PCM 16-bit reader at 48 kHz
   (stereo, or mono for a mono device) -- the input the AAC encoder accepts. */
static bool cn1CameraOpenAudio(ComPtr<IMFMediaSource>& source, ComPtr<IMFSourceReader>& reader,
        UINT32& rate, UINT32& channels) {
    ComPtr<IMFAttributes> attr;
    if (FAILED(MFCreateAttributes(&attr, 1))) return false;
    attr->SetGUID(MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE, MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE_AUDCAP_GUID);
    IMFActivate** devices = NULL;
    UINT32 count = 0;
    if (FAILED(MFEnumDeviceSources(attr.Get(), &devices, &count)) || count == 0) {
        if (devices) CoTaskMemFree(devices);
        return false;
    }
    HRESULT hr = devices[0]->ActivateObject(IID_PPV_ARGS(&source));
    for (UINT32 i = 0; i < count; i++) { if (devices[i]) devices[i]->Release(); }
    CoTaskMemFree(devices);
    if (FAILED(hr) || !source) return false;
    if (FAILED(MFCreateSourceReaderFromMediaSource(source.Get(), NULL, &reader))) {
        source->Shutdown();
        return false;
    }
    UINT32 nativeCh = 2;
    ComPtr<IMFMediaType> nativeType;
    if (SUCCEEDED(reader->GetNativeMediaType((DWORD) MF_SOURCE_READER_FIRST_AUDIO_STREAM, 0, &nativeType))) {
        nativeCh = MFGetAttributeUINT32(nativeType.Get(), MF_MT_AUDIO_NUM_CHANNELS, 2);
    }
    channels = nativeCh == 1 ? 1 : 2;
    rate = 48000;
    ComPtr<IMFMediaType> pcm;
    MFCreateMediaType(&pcm);
    pcm->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Audio);
    pcm->SetGUID(MF_MT_SUBTYPE, MFAudioFormat_PCM);
    pcm->SetUINT32(MF_MT_AUDIO_BITS_PER_SAMPLE, 16);
    pcm->SetUINT32(MF_MT_AUDIO_SAMPLES_PER_SECOND, rate);
    pcm->SetUINT32(MF_MT_AUDIO_NUM_CHANNELS, channels);
    pcm->SetUINT32(MF_MT_AUDIO_BLOCK_ALIGNMENT, channels * 2);
    pcm->SetUINT32(MF_MT_AUDIO_AVG_BYTES_PER_SECOND, rate * channels * 2);
    if (FAILED(reader->SetCurrentMediaType((DWORD) MF_SOURCE_READER_FIRST_AUDIO_STREAM, NULL, pcm.Get()))) {
        source->Shutdown();
        return false;
    }
    return true;
}

static DWORD WINAPI cn1CameraAudioWorker(LPVOID param) {
    CN1CameraSession* s = (CN1CameraSession*) param;
    CoInitializeEx(NULL, COINIT_MULTITHREADED);
    while (InterlockedCompareExchange(&s->audioRunning, 1, 1)) {
        DWORD streamIndex = 0, flags = 0;
        LONGLONG timestamp = 0;
        ComPtr<IMFSample> sample;
        if (FAILED(s->audioReader->ReadSample((DWORD) MF_SOURCE_READER_FIRST_AUDIO_STREAM, 0,
                &streamIndex, &flags, &timestamp, &sample))) {
            break;
        }
        if ((flags & MF_SOURCE_READERF_ENDOFSTREAM) != 0) break;
        if (!sample) continue;
        LONGLONG duration = 0;
        sample->GetSampleDuration(&duration);
        EnterCriticalSection(&s->lock);
        if (s->writer && s->hasAudio) {
            cn1CameraWriteLocked(s, sample.Get(), s->audioStream, &s->lastAudioTs, duration);
        }
        LeaveCriticalSection(&s->lock);
    }
    CoUninitialize();
    return 0;
}

/* Detaches and finalizes the writer, stopping the audio thread first. */
static bool cn1CameraStopRecording(CN1CameraSession* s) {
    if (s->audioThread) {
        InterlockedExchange(&s->audioRunning, 0);
        /* Shutting the source down unblocks a ReadSample the worker is parked in,
           so the wait below is bounded without a timeout -- and must be: releasing
           the reader while the worker still calls it would be a use after free. */
        if (s->audioSource) s->audioSource->Shutdown();
        WaitForSingleObject(s->audioThread, INFINITE);
        CloseHandle(s->audioThread);
        s->audioThread = NULL;
    }
    EnterCriticalSection(&s->lock);
    IMFSinkWriter* w = s->writer;
    s->writer = NULL;
    s->hasAudio = false;
    IMFMediaSource* audioSource = s->audioSource;
    IMFSourceReader* audioReader = s->audioReader;
    s->audioSource = NULL;
    s->audioReader = NULL;
    LeaveCriticalSection(&s->lock);
    if (audioSource) { audioSource->Shutdown(); audioSource->Release(); }
    if (audioReader) { audioReader->Release(); }
    if (!w) return false;
    /* Finalize fails when not a single sample reached the writer (paused all along,
       or stopped at once); the file is then not a playable MP4. */
    bool ok = SUCCEEDED(w->Finalize());
    w->Release();
    return ok;
}

/*
 * Starts writing the session's frames to an H.264 MP4 at the given native path.
 * Returns 0 when recording with the requested streams, 1 when recording video
 * only because audio was requested but no audio capture device could be opened,
 * -1 for a bad handle or a recording already running, -2 while the camera is
 * not open yet, -3 when the writer or encoder could not be set up.
 */
JAVA_INT com_codename1_impl_windows_WindowsNative_cameraSessionStartRecording___long_java_lang_String_boolean_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT pathObj, JAVA_BOOLEAN audio) {
    CN1CameraSession* s = (CN1CameraSession*) (intptr_t) handle;
    if (!s || pathObj == JAVA_NULL) return -1;
    EnterCriticalSection(&s->lock);
    bool busy = s->writer != NULL || s->audioThread != NULL;
    IMFMediaType* inType = s->readerType;
    if (inType) inType->AddRef();
    LeaveCriticalSection(&s->lock);
    if (busy) { if (inType) inType->Release(); return -1; }
    if (!inType) return -2;
    ComPtr<IMFMediaType> videoIn;
    videoIn.Attach(inType);

    UINT32 width = 0, height = 0, fpsN = 30, fpsD = 1;
    MFGetAttributeSize(videoIn.Get(), MF_MT_FRAME_SIZE, &width, &height);
    if (FAILED(MFGetAttributeRatio(videoIn.Get(), MF_MT_FRAME_RATE, &fpsN, &fpsD)) || fpsN == 0 || fpsD == 0) {
        fpsN = 30; fpsD = 1;
        MFSetAttributeRatio(videoIn.Get(), MF_MT_FRAME_RATE, fpsN, fpsD);
    }
    videoIn->SetUINT32(MF_MT_INTERLACE_MODE, MFVideoInterlace_Progressive);
    MFSetAttributeRatio(videoIn.Get(), MF_MT_PIXEL_ASPECT_RATIO, 1, 1);

    ComPtr<IMFSourceReader> audioReader;
    ComPtr<IMFMediaSource> audioSource;
    UINT32 rate = 0, channels = 0;
    bool withAudio = audio && cn1CameraOpenAudio(audioSource, audioReader, rate, channels);

    WCHAR* path = cn1WinJavaStringToWide(threadStateData, pathObj, NULL);
    if (!path) { if (audioSource) audioSource->Shutdown(); return -3; }
    ComPtr<IMFSinkWriter> writer;
    DWORD vStream = 0, aStream = 0;
    HRESULT hr = E_FAIL;
    /* Two passes at most: when the AAC stream cannot be set up (no AAC encoder,
       an input type it refuses) the writer is rebuilt for video alone rather than
       failing the whole recording. */
    for (int pass = 0; pass < 2; pass++) {
        writer.Reset();
        ComPtr<IMFAttributes> wattr;
        MFCreateAttributes(&wattr, 2);
        wattr->SetUINT32(MF_READWRITE_ENABLE_HARDWARE_TRANSFORMS, TRUE);
        /* MP4 regardless of the file's extension. */
        wattr->SetGUID(MF_TRANSCODE_CONTAINERTYPE, MFTranscodeContainerType_MPEG4);
        hr = MFCreateSinkWriterFromURL(path, NULL, wattr.Get(), &writer);
        if (SUCCEEDED(hr)) {
            ComPtr<IMFMediaType> h264;
            MFCreateMediaType(&h264);
            h264->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);
            h264->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_H264);
            /* ~0.15 bits per pixel per frame: good webcam quality, clamped. */
            double bps = (double) width * height * ((double) fpsN / fpsD) * 0.15;
            if (bps < 500000) bps = 500000;
            if (bps > 20000000) bps = 20000000;
            h264->SetUINT32(MF_MT_AVG_BITRATE, (UINT32) bps);
            h264->SetUINT32(MF_MT_INTERLACE_MODE, MFVideoInterlace_Progressive);
            MFSetAttributeSize(h264.Get(), MF_MT_FRAME_SIZE, width, height);
            MFSetAttributeRatio(h264.Get(), MF_MT_FRAME_RATE, fpsN, fpsD);
            MFSetAttributeRatio(h264.Get(), MF_MT_PIXEL_ASPECT_RATIO, 1, 1);
            hr = writer->AddStream(h264.Get(), &vStream);
            /* RGB32 in: the writer inserts the colour converter ahead of the encoder. */
            if (SUCCEEDED(hr)) hr = writer->SetInputMediaType(vStream, videoIn.Get(), NULL);
        }
        if (FAILED(hr)) break; /* the video stream itself failed: nothing to retry */
        if (withAudio) {
            ComPtr<IMFMediaType> aac;
            MFCreateMediaType(&aac);
            aac->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Audio);
            aac->SetGUID(MF_MT_SUBTYPE, MFAudioFormat_AAC);
            aac->SetUINT32(MF_MT_AUDIO_BITS_PER_SAMPLE, 16);
            aac->SetUINT32(MF_MT_AUDIO_SAMPLES_PER_SECOND, rate);
            aac->SetUINT32(MF_MT_AUDIO_NUM_CHANNELS, channels);
            aac->SetUINT32(MF_MT_AUDIO_AVG_BYTES_PER_SECOND, 16000); /* 128 kbit/s */
            ComPtr<IMFMediaType> pcm;
            HRESULT ahr = writer->AddStream(aac.Get(), &aStream);
            if (SUCCEEDED(ahr)) ahr = audioReader->GetCurrentMediaType((DWORD) MF_SOURCE_READER_FIRST_AUDIO_STREAM, &pcm);
            if (SUCCEEDED(ahr)) ahr = writer->SetInputMediaType(aStream, pcm.Get(), NULL);
            if (FAILED(ahr)) {
                withAudio = false;
                hr = ahr;
                continue;
            }
        }
        hr = writer->BeginWriting();
        break;
    }
    free(path);
    if (FAILED(hr)) {
        if (audioSource) audioSource->Shutdown();
        return -3;
    }

    EnterCriticalSection(&s->lock);
    s->videoStream = vStream;
    s->audioStream = aStream;
    s->hasAudio = withAudio;
    s->frameDuration = (LONGLONG) (10000000.0 * fpsD / fpsN);
    s->lastVideoTs = -1;
    s->lastAudioTs = -1;
    s->recStart = MFGetSystemTime();
    s->writer = writer.Detach();
    if (withAudio) {
        s->audioSource = audioSource.Detach();
        s->audioReader = audioReader.Detach();
    }
    LeaveCriticalSection(&s->lock);
    if (withAudio) {
        InterlockedExchange(&s->audioRunning, 1);
        s->audioThread = CreateThread(NULL, 0, cn1CameraAudioWorker, s, 0, NULL);
        if (!s->audioThread) {
            /* The writer already declares an AAC stream that would never get a
               sample, so its file could not be finalized. Fail the start rather
               than record something unplayable, and let the microphone go. */
            InterlockedExchange(&s->audioRunning, 0);
            EnterCriticalSection(&s->lock);
            IMFSinkWriter* w = s->writer;
            IMFMediaSource* src = s->audioSource;
            IMFSourceReader* rdr = s->audioReader;
            s->writer = NULL;
            s->hasAudio = false;
            s->audioSource = NULL;
            s->audioReader = NULL;
            LeaveCriticalSection(&s->lock);
            if (w) w->Release();
            if (src) { src->Shutdown(); src->Release(); }
            if (rdr) rdr->Release();
            return -3;
        }
    } else if (audioSource) {
        audioSource->Shutdown();
    }
    return (audio && !withAudio) ? 1 : 0;
}

/* Stops recording and finalizes the MP4; false when nothing playable was written. */
JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_cameraSessionStopRecording___long_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1CameraSession* s = (CN1CameraSession*) (intptr_t) handle;
    if (!s) return JAVA_FALSE;
    CN1_YIELD_THREAD;
    bool ok = cn1CameraStopRecording(s);
    CN1_RESUME_THREAD;
    return ok ? JAVA_TRUE : JAVA_FALSE;
}

/* Friendly names of the video capture devices: "name|external|flash|autofocus;...",
   flash always 0 (see WindowsCameraImpl.setFlashMode). */
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
            /* Auto focus is reported from the device's own focus control. Activating
               the source to ask costs a few tens of milliseconds per device but does
               not start streaming (no capture LED); ShutdownObject releases it. */
            int af = 0;
            IMFMediaSource* probe = NULL;
            if (SUCCEEDED(devices[i]->ActivateObject(IID_PPV_ARGS(&probe))) && probe) {
                IAMCameraControl* ctl = NULL;
                if (SUCCEEDED(probe->QueryInterface(__uuidof(IAMCameraControl), (void**) &ctl)) && ctl) {
                    af = cn1CameraHasAutoFocus(ctl) ? 1 : 0;
                    ctl->Release();
                }
                probe->Release();
                devices[i]->ShutdownObject();
            }
            int need = snprintf(buf + pos, sizeof(buf) - pos, "%s%s|external|0|%d",
                    pos > 0 ? ";" : "", utf8, af);
            if (need > 0 && pos + need < (int) sizeof(buf)) { pos += need; }
        }
        devices[i]->Release();
    }
    CoTaskMemFree(devices);
    return newStringFromCString(threadStateData, buf);
}

} /* extern "C" */

#endif /* _WIN32 */
