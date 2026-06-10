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

#include "cn1_windows.h"

using Microsoft::WRL::ComPtr;

extern "C" {

extern struct clazz class_array1__JAVA_INT;

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

} /* extern "C" */

#endif /* _WIN32 */
