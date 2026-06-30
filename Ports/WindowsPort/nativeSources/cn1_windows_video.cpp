/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
 *
 * Native backend for com.codename1.media.VideoIO on Windows using Media
 * Foundation: IMFSourceReader for frame-accurate decoding to RGBA, IMFSinkWriter
 * for H.264/HEVC + AAC encoding. The mf/mfplat/mfreadwrite/mfuuid libraries are
 * already linked by the generated Windows CMakeLists (see ByteCodeTranslator).
 *
 * This file deliberately avoids the C++ standard library (<string> etc.): the
 * always-compiled native sources are built with clang-cl against the xwin MSVC
 * SDK whose <yvals_core.h> rejects the toolchain's Clang version (STL1000), so
 * we use plain wchar_t buffers / malloc instead.
 */
#include <windows.h>
#include <mfapi.h>
#include <mfidl.h>
#include <mfreadwrite.h>
#include <mferror.h>
#include <wrl/client.h>
#include <stdlib.h>
#include <string.h>
#include "cn1_windows.h"

using Microsoft::WRL::ComPtr;

extern struct clazz class_array1__JAVA_BYTE;

// 100-ns units used by Media Foundation timestamps.
static const LONGLONG CN1_HNS_PER_MS = 10000LL;
static const LONGLONG CN1_HNS_PER_SEC = 10000000LL;

static bool g_mfStarted = false;
static void cn1EnsureMF() {
    if (!g_mfStarted) {
        if (SUCCEEDED(MFStartup(MF_VERSION, MFSTARTUP_LITE))) {
            g_mfStarted = true;
        }
    }
}

// Strips a file: prefix and converts the UTF-8 path to a wide string in the
// caller-supplied buffer (no std::wstring; avoids pulling in the MSVC STL).
static void cn1StripFileWide(const char* utf8, wchar_t* out, int outLen) {
    const char* s = utf8 ? utf8 : "";
    if (strncmp(s, "file://", 7) == 0) {
        s += 7;
    } else if (strncmp(s, "file:", 5) == 0) {
        s += 5;
    }
    if (MultiByteToWideChar(CP_UTF8, 0, s, -1, out, outLen) == 0 && outLen > 0) {
        out[0] = L'\0';
    }
}

struct CN1VideoReader {
    ComPtr<IMFSourceReader> reader;
    int width;
    int height;
    LONGLONG durationMs;
    float frameRate;
    bool hasVideo;
    bool hasAudio;
    int audioRate;
    int audioChannels;
    LONG stride;
};

struct CN1VideoWriter {
    ComPtr<IMFSinkWriter> writer;
    DWORD videoStream;
    DWORD audioStream;
    int width;
    int height;
    float frameRate;
    bool hasAudio;
};

// --------------------------------------------------------------------------
// Reader
// --------------------------------------------------------------------------
static JAVA_LONG cn1ReaderOpen(const wchar_t* url) {
    cn1EnsureMF();
    ComPtr<IMFAttributes> attrs;
    MFCreateAttributes(&attrs, 1);
    attrs->SetUINT32(MF_SOURCE_READER_ENABLE_VIDEO_PROCESSING, TRUE);

    ComPtr<IMFSourceReader> reader;
    if (FAILED(MFCreateSourceReaderFromURL(url, attrs.Get(), &reader))) {
        return 0;
    }

    CN1VideoReader* st = new CN1VideoReader();
    st->reader = reader;
    st->width = 0;
    st->height = 0;
    st->durationMs = -1;
    st->frameRate = 30.0f;
    st->hasVideo = false;
    st->hasAudio = false;
    st->audioRate = -1;
    st->audioChannels = -1;
    st->stride = 0;

    // Configure the video stream to deliver RGB32 (BGRA byte order).
    ComPtr<IMFMediaType> rgbType;
    if (SUCCEEDED(MFCreateMediaType(&rgbType))) {
        rgbType->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);
        rgbType->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_RGB32);
        if (SUCCEEDED(reader->SetCurrentMediaType((DWORD) MF_SOURCE_READER_FIRST_VIDEO_STREAM, NULL, rgbType.Get()))) {
            st->hasVideo = true;
            ComPtr<IMFMediaType> current;
            if (SUCCEEDED(reader->GetCurrentMediaType((DWORD) MF_SOURCE_READER_FIRST_VIDEO_STREAM, &current))) {
                UINT32 w = 0, h = 0;
                MFGetAttributeSize(current.Get(), MF_MT_FRAME_SIZE, &w, &h);
                st->width = (int) w;
                st->height = (int) h;
                UINT32 num = 0, den = 0;
                if (SUCCEEDED(MFGetAttributeRatio(current.Get(), MF_MT_FRAME_RATE, &num, &den)) && den != 0) {
                    st->frameRate = (float) num / (float) den;
                }
                LONG stride = 0;
                if (SUCCEEDED(current->GetUINT32(MF_MT_DEFAULT_STRIDE, (UINT32*) &stride))) {
                    st->stride = stride;
                } else {
                    st->stride = (LONG) w * 4;
                }
            }
        }
    }

    // Configure the audio stream to deliver 16-bit PCM.
    ComPtr<IMFMediaType> pcmType;
    if (SUCCEEDED(MFCreateMediaType(&pcmType))) {
        pcmType->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Audio);
        pcmType->SetGUID(MF_MT_SUBTYPE, MFAudioFormat_PCM);
        pcmType->SetUINT32(MF_MT_AUDIO_BITS_PER_SAMPLE, 16);
        if (SUCCEEDED(reader->SetCurrentMediaType((DWORD) MF_SOURCE_READER_FIRST_AUDIO_STREAM, NULL, pcmType.Get()))) {
            st->hasAudio = true;
            ComPtr<IMFMediaType> current;
            if (SUCCEEDED(reader->GetCurrentMediaType((DWORD) MF_SOURCE_READER_FIRST_AUDIO_STREAM, &current))) {
                UINT32 rate = 0, ch = 0;
                current->GetUINT32(MF_MT_AUDIO_SAMPLES_PER_SECOND, &rate);
                current->GetUINT32(MF_MT_AUDIO_NUM_CHANNELS, &ch);
                st->audioRate = (int) rate;
                st->audioChannels = (int) ch;
            }
        }
    }

    PROPVARIANT durVar;
    PropVariantInit(&durVar);
    if (SUCCEEDED(reader->GetPresentationAttribute((DWORD) MF_SOURCE_READER_MEDIASOURCE, MF_PD_DURATION, &durVar))
            && durVar.vt == VT_UI8) {
        st->durationMs = (LONGLONG) (durVar.uhVal.QuadPart / CN1_HNS_PER_MS);
    }
    PropVariantClear(&durVar);

    return (JAVA_LONG) (intptr_t) st;
}

static JAVA_OBJECT cn1ReaderFrameAt(CODENAME_ONE_THREAD_STATE, CN1VideoReader* st, JAVA_LONG ms) {
    if (!st->hasVideo) {
        return JAVA_NULL;
    }
    LONGLONG target = (LONGLONG) ms * CN1_HNS_PER_MS;
    PROPVARIANT pos;
    PropVariantInit(&pos);
    pos.vt = VT_I8;
    pos.hVal.QuadPart = target;
    st->reader->SetCurrentPosition(GUID_NULL, pos);
    PropVariantClear(&pos);

    JAVA_OBJECT result = JAVA_NULL;
    for (;;) {
        DWORD streamFlags = 0;
        LONGLONG timestamp = 0;
        ComPtr<IMFSample> sample;
        if (FAILED(st->reader->ReadSample((DWORD) MF_SOURCE_READER_FIRST_VIDEO_STREAM, 0, NULL, &streamFlags, &timestamp, &sample))) {
            break;
        }
        if (streamFlags & MF_SOURCE_READERF_ENDOFSTREAM) {
            break;
        }
        if (sample == NULL) {
            continue;
        }
        if (timestamp + CN1_HNS_PER_SEC / 60 < target) {
            // decode forward until we reach the requested frame
            continue;
        }
        ComPtr<IMFMediaBuffer> buffer;
        if (FAILED(sample->ConvertToContiguousBuffer(&buffer))) {
            break;
        }
        BYTE* data = NULL;
        DWORD maxLen = 0, curLen = 0;
        if (SUCCEEDED(buffer->Lock(&data, &maxLen, &curLen))) {
            int w = st->width;
            int h = st->height;
            LONG stride = st->stride != 0 ? st->stride : (LONG) w * 4;
            bool bottomUp = stride < 0;
            LONG absStride = bottomUp ? -stride : stride;
            result = allocArray(threadStateData, w * h * 4, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
            if (result != JAVA_NULL) {
                BYTE* out = (BYTE*) (*(JAVA_ARRAY) result).data;
                for (int y = 0; y < h; y++) {
                    int srcRow = bottomUp ? (h - 1 - y) : y;
                    BYTE* src = data + (size_t) srcRow * absStride;
                    BYTE* dst = out + (size_t) y * w * 4;
                    for (int x = 0; x < w; x++) {
                        // MF RGB32 is B,G,R,X -> our RGBA
                        dst[x * 4] = src[x * 4 + 2];
                        dst[x * 4 + 1] = src[x * 4 + 1];
                        dst[x * 4 + 2] = src[x * 4];
                        dst[x * 4 + 3] = 0xFF;
                    }
                }
            }
            buffer->Unlock();
        }
        break;
    }
    return result;
}

static JAVA_OBJECT cn1ReaderReadAudio(CODENAME_ONE_THREAD_STATE, CN1VideoReader* st) {
    if (!st->hasAudio) {
        return JAVA_NULL;
    }
    unsigned char* pcm = NULL;
    size_t pcmLen = 0, pcmCap = 0;
    for (;;) {
        DWORD streamFlags = 0;
        LONGLONG timestamp = 0;
        ComPtr<IMFSample> sample;
        if (FAILED(st->reader->ReadSample((DWORD) MF_SOURCE_READER_FIRST_AUDIO_STREAM, 0, NULL, &streamFlags, &timestamp, &sample))) {
            break;
        }
        if (streamFlags & MF_SOURCE_READERF_ENDOFSTREAM) {
            break;
        }
        if (sample == NULL) {
            continue;
        }
        ComPtr<IMFMediaBuffer> buffer;
        if (FAILED(sample->ConvertToContiguousBuffer(&buffer))) {
            continue;
        }
        BYTE* data = NULL;
        DWORD maxLen = 0, curLen = 0;
        if (SUCCEEDED(buffer->Lock(&data, &maxLen, &curLen))) {
            if (pcmLen + curLen > pcmCap) {
                size_t newCap = (pcmLen + curLen) * 2;
                unsigned char* grown = (unsigned char*) realloc(pcm, newCap);
                if (grown != NULL) {
                    pcm = grown;
                    pcmCap = newCap;
                }
            }
            if (pcm != NULL && pcmLen + curLen <= pcmCap) {
                memcpy(pcm + pcmLen, data, curLen);
                pcmLen += curLen;
            }
            buffer->Unlock();
        }
    }
    JAVA_OBJECT result = JAVA_NULL;
    if (pcm != NULL && pcmLen > 0) {
        result = allocArray(threadStateData, (int) pcmLen, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
        if (result != JAVA_NULL) {
            memcpy((*(JAVA_ARRAY) result).data, pcm, pcmLen);
        }
    }
    free(pcm);
    return result;
}

// --------------------------------------------------------------------------
// Writer
// --------------------------------------------------------------------------
static JAVA_LONG cn1WriterOpen(const wchar_t* url, bool hevc, int width, int height, float fps,
        int videoBitRate, int gop, bool hasAudio, int audioBitRate, int sampleRate, int channels) {
    cn1EnsureMF();
    UINT32 fpsNum = (UINT32) (fps <= 0 ? 30 : (UINT32) (fps + 0.5f));
    UINT32 fpsDen = 1;

    ComPtr<IMFSinkWriter> writer;
    if (FAILED(MFCreateSinkWriterFromURL(url, NULL, NULL, &writer))) {
        return 0;
    }

    CN1VideoWriter* st = new CN1VideoWriter();
    st->writer = writer;
    st->width = width;
    st->height = height;
    st->frameRate = fps;
    st->hasAudio = hasAudio;
    st->audioStream = 0;

    // ---- video output (H.264 / HEVC) ----
    ComPtr<IMFMediaType> videoOut;
    MFCreateMediaType(&videoOut);
    videoOut->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);
    videoOut->SetGUID(MF_MT_SUBTYPE, hevc ? MFVideoFormat_HEVC : MFVideoFormat_H264);
    videoOut->SetUINT32(MF_MT_AVG_BITRATE, (UINT32) videoBitRate);
    videoOut->SetUINT32(MF_MT_INTERLACE_MODE, MFVideoInterlace_Progressive);
    videoOut->SetUINT32(MF_MT_MAX_KEYFRAME_SPACING, (UINT32) gop);
    MFSetAttributeSize(videoOut.Get(), MF_MT_FRAME_SIZE, width, height);
    MFSetAttributeRatio(videoOut.Get(), MF_MT_FRAME_RATE, fpsNum, fpsDen);
    MFSetAttributeRatio(videoOut.Get(), MF_MT_PIXEL_ASPECT_RATIO, 1, 1);
    if (FAILED(writer->AddStream(videoOut.Get(), &st->videoStream))) {
        delete st;
        return 0;
    }

    ComPtr<IMFMediaType> videoIn;
    MFCreateMediaType(&videoIn);
    videoIn->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video);
    videoIn->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_RGB32);
    videoIn->SetUINT32(MF_MT_INTERLACE_MODE, MFVideoInterlace_Progressive);
    MFSetAttributeSize(videoIn.Get(), MF_MT_FRAME_SIZE, width, height);
    MFSetAttributeRatio(videoIn.Get(), MF_MT_FRAME_RATE, fpsNum, fpsDen);
    MFSetAttributeRatio(videoIn.Get(), MF_MT_PIXEL_ASPECT_RATIO, 1, 1);
    if (FAILED(writer->SetInputMediaType(st->videoStream, videoIn.Get(), NULL))) {
        delete st;
        return 0;
    }

    // ---- audio output (AAC) ----
    if (hasAudio) {
        ComPtr<IMFMediaType> audioOut;
        MFCreateMediaType(&audioOut);
        audioOut->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Audio);
        audioOut->SetGUID(MF_MT_SUBTYPE, MFAudioFormat_AAC);
        audioOut->SetUINT32(MF_MT_AUDIO_BITS_PER_SAMPLE, 16);
        audioOut->SetUINT32(MF_MT_AUDIO_SAMPLES_PER_SECOND, (UINT32) sampleRate);
        audioOut->SetUINT32(MF_MT_AUDIO_NUM_CHANNELS, (UINT32) channels);
        audioOut->SetUINT32(MF_MT_AUDIO_AVG_BYTES_PER_SECOND, (UINT32) (audioBitRate / 8));
        if (SUCCEEDED(writer->AddStream(audioOut.Get(), &st->audioStream))) {
            ComPtr<IMFMediaType> audioIn;
            MFCreateMediaType(&audioIn);
            audioIn->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Audio);
            audioIn->SetGUID(MF_MT_SUBTYPE, MFAudioFormat_PCM);
            audioIn->SetUINT32(MF_MT_AUDIO_BITS_PER_SAMPLE, 16);
            audioIn->SetUINT32(MF_MT_AUDIO_SAMPLES_PER_SECOND, (UINT32) sampleRate);
            audioIn->SetUINT32(MF_MT_AUDIO_NUM_CHANNELS, (UINT32) channels);
            writer->SetInputMediaType(st->audioStream, audioIn.Get(), NULL);
        } else {
            st->hasAudio = false;
        }
    }

    if (FAILED(writer->BeginWriting())) {
        delete st;
        return 0;
    }
    return (JAVA_LONG) (intptr_t) st;
}

static void cn1WriterWriteSample(CN1VideoWriter* st, DWORD stream, const BYTE* bytes, DWORD len, LONGLONG ptsHns, LONGLONG durHns) {
    ComPtr<IMFMediaBuffer> buffer;
    if (FAILED(MFCreateMemoryBuffer(len, &buffer))) {
        return;
    }
    BYTE* dst = NULL;
    DWORD maxLen = 0;
    if (SUCCEEDED(buffer->Lock(&dst, &maxLen, NULL))) {
        memcpy(dst, bytes, len);
        buffer->Unlock();
    }
    buffer->SetCurrentLength(len);
    ComPtr<IMFSample> sample;
    if (FAILED(MFCreateSample(&sample))) {
        return;
    }
    sample->AddBuffer(buffer.Get());
    sample->SetSampleTime(ptsHns);
    sample->SetSampleDuration(durHns);
    st->writer->WriteSample(stream, sample.Get());
}

// --------------------------------------------------------------------------
// JNI-style exports
// --------------------------------------------------------------------------
#define RD(peer) ((CN1VideoReader*)(intptr_t)(peer))
#define WR(peer) ((CN1VideoWriter*)(intptr_t)(peer))

extern "C" {

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_videoBackendAvailable___R_boolean(CODENAME_ONE_THREAD_STATE) {
    cn1EnsureMF();
    return g_mfStarted ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_videoSupportsHEVC___R_boolean(CODENAME_ONE_THREAD_STATE) {
    cn1EnsureMF();
    MFT_REGISTER_TYPE_INFO outInfo = { MFMediaType_Video, MFVideoFormat_HEVC };
    IMFActivate** activates = NULL;
    UINT32 count = 0;
    HRESULT hr = MFTEnumEx(MFT_CATEGORY_VIDEO_ENCODER, MFT_ENUM_FLAG_SYNCMFT | MFT_ENUM_FLAG_HARDWARE | MFT_ENUM_FLAG_SORTANDFILTER,
            NULL, &outInfo, &activates, &count);
    if (SUCCEEDED(hr) && activates != NULL) {
        for (UINT32 i = 0; i < count; i++) {
            if (activates[i]) {
                activates[i]->Release();
            }
        }
        CoTaskMemFree(activates);
    }
    return (SUCCEEDED(hr) && count > 0) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_videoReaderOpen___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    wchar_t wpath[2048];
    cn1StripFileWide(stringToUTF8(threadStateData, pathObj), wpath, 2048);
    return cn1ReaderOpen(wpath);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_videoReaderWidth___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    return RD(peer)->width;
}
JAVA_INT com_codename1_impl_windows_WindowsNative_videoReaderHeight___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    return RD(peer)->height;
}
JAVA_LONG com_codename1_impl_windows_WindowsNative_videoReaderDuration___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    return (JAVA_LONG) RD(peer)->durationMs;
}
JAVA_FLOAT com_codename1_impl_windows_WindowsNative_videoReaderFrameRate___long_R_float(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    return RD(peer)->frameRate;
}
JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_videoReaderHasVideo___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    return RD(peer)->hasVideo ? JAVA_TRUE : JAVA_FALSE;
}
JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_videoReaderHasAudio___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    return RD(peer)->hasAudio ? JAVA_TRUE : JAVA_FALSE;
}
JAVA_INT com_codename1_impl_windows_WindowsNative_videoReaderAudioSampleRate___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    return RD(peer)->audioRate;
}
JAVA_INT com_codename1_impl_windows_WindowsNative_videoReaderAudioChannels___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    return RD(peer)->audioChannels;
}
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_videoReaderFrameAt___long_long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_LONG ms) {
    return cn1ReaderFrameAt(threadStateData, RD(peer), ms);
}
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_videoReaderReadAudio___long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    return cn1ReaderReadAudio(threadStateData, RD(peer));
}
JAVA_VOID com_codename1_impl_windows_WindowsNative_videoReaderClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1VideoReader* st = RD(peer);
    if (st) {
        delete st;
    }
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_videoWriterOpen___java_lang_String_boolean_int_int_float_int_int_boolean_int_int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj, JAVA_BOOLEAN hevc, JAVA_INT width, JAVA_INT height, JAVA_FLOAT fps,
        JAVA_INT videoBitRate, JAVA_INT gop, JAVA_BOOLEAN hasAudio, JAVA_INT audioBitRate, JAVA_INT sampleRate, JAVA_INT channels) {
    wchar_t wpath[2048];
    cn1StripFileWide(stringToUTF8(threadStateData, pathObj), wpath, 2048);
    return cn1WriterOpen(wpath, hevc != JAVA_FALSE, width, height, fps, videoBitRate, gop,
            hasAudio != JAVA_FALSE, audioBitRate, sampleRate, channels);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_videoWriterFrame___long_byte_1ARRAY_int_int_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT rgbaObj, JAVA_INT w, JAVA_INT h, JAVA_LONG ptsMs) {
    CN1VideoWriter* st = WR(peer);
    if (!st || rgbaObj == JAVA_NULL) {
        return;
    }
    BYTE* rgba = (BYTE*) (*(JAVA_ARRAY) rgbaObj).data;
    DWORD len = (DWORD) (w * h * 4);
    // RGBA (Java) -> RGB32/BGRA (MF input)
    BYTE* bgra = (BYTE*) malloc(len);
    if (bgra == NULL) {
        return;
    }
    for (int i = 0; i < w * h; i++) {
        bgra[i * 4] = rgba[i * 4 + 2];
        bgra[i * 4 + 1] = rgba[i * 4 + 1];
        bgra[i * 4 + 2] = rgba[i * 4];
        bgra[i * 4 + 3] = rgba[i * 4 + 3];
    }
    LONGLONG dur = st->frameRate > 0 ? (LONGLONG) (CN1_HNS_PER_SEC / st->frameRate) : (CN1_HNS_PER_SEC / 30);
    cn1WriterWriteSample(st, st->videoStream, bgra, len, (LONGLONG) ptsMs * CN1_HNS_PER_MS, dur);
    free(bgra);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_videoWriterAudio___long_byte_1ARRAY_int_int_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT pcmObj, JAVA_INT sampleRate, JAVA_INT channels, JAVA_LONG ptsMs) {
    CN1VideoWriter* st = WR(peer);
    if (!st || !st->hasAudio || pcmObj == JAVA_NULL) {
        return;
    }
    BYTE* pcm = (BYTE*) (*(JAVA_ARRAY) pcmObj).data;
    DWORD len = (DWORD) (*(JAVA_ARRAY) pcmObj).length;
    DWORD frames = len / (DWORD) (2 * (channels > 0 ? channels : 1));
    LONGLONG dur = sampleRate > 0 ? (LONGLONG) ((LONGLONG) frames * CN1_HNS_PER_SEC / sampleRate) : 0;
    cn1WriterWriteSample(st, st->audioStream, pcm, len, (LONGLONG) ptsMs * CN1_HNS_PER_MS, dur);
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_videoWriterClose___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1VideoWriter* st = WR(peer);
    if (!st) {
        return JAVA_FALSE;
    }
    HRESULT hr = st->writer->Finalize();
    delete st;
    return SUCCEEDED(hr) ? JAVA_TRUE : JAVA_FALSE;
}

} // extern "C"
