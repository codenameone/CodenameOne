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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <wchar.h>
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
    /* The source URL, so readAudio can open a reader of its own. A plain
     * malloc'd copy rather than std::wstring: including <string> drags in the
     * MSVC STL, which hard-asserts on the compiler version (STL1000) and broke
     * the cross-compile job, whose clang is not pinned to 19 the way the
     * build+run job's is. Freed in the destructor below. */
    wchar_t* url;
    CN1VideoReader() : url(NULL) {}
    ~CN1VideoReader() { free(url); }
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
    unsigned long audioSamplesWritten;
    unsigned long long audioBytesWritten;
    DWORD videoStream;
    DWORD audioStream;
    int width;
    int height;
    float frameRate;
    bool hasAudio;
    /* The rate the AAC encoder was configured at. Media Foundation's AAC encoder
     * accepts 44100 or 48000 Hz only, so a caller asking for anything else (the
     * conformance suite records an 8 kHz tone) gets its PCM resampled on the way
     * in rather than losing the audio stream. */
    int audioEncRate;
    int audioChannels;
    HRESULT audioSetupHr;
};

/* The sample rates Media Foundation's AAC encoder will accept. */
static int cn1AacEncoderRate(int requested) {
    if (requested == 44100 || requested == 48000) {
        return requested;
    }
    /* 44100 is the closer target for anything derived from CD-family rates
     * (11025/22050); everything else lands on 48000. */
    return (requested % 11025) == 0 ? 44100 : 48000;
}

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

    // Select the streams explicitly. Which streams a source reader starts with
    // depends on the presentation descriptor, and SetCurrentMediaType succeeds on
    // a stream that is not selected -- so the audio configuration below reported
    // success, hasAudio became true, and ReadSample then produced nothing at all.
    // That is exactly what VideoIORoundTripTest saw on Windows: "decoded clip
    // reports audio but no PCM samples were returned". Deselect everything, then
    // turn on the two streams this reader actually consumes.
    reader->SetStreamSelection((DWORD) MF_SOURCE_READER_ALL_STREAMS, FALSE);
    reader->SetStreamSelection((DWORD) MF_SOURCE_READER_FIRST_VIDEO_STREAM, TRUE);
    reader->SetStreamSelection((DWORD) MF_SOURCE_READER_FIRST_AUDIO_STREAM, TRUE);

    CN1VideoReader* st = new CN1VideoReader();
    st->reader = reader;
    if (url != NULL) {
        size_t urlChars = wcslen(url) + 1;
        st->url = (wchar_t*) malloc(urlChars * sizeof(wchar_t));
        if (st->url != NULL) {
            memcpy(st->url, url, urlChars * sizeof(wchar_t));
        }
    }
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
            // Media Foundation's H.264/HEVC decode delivers RGB32 top-down here,
            // so read straight (only honour an explicit negative stride). The
            // encoder side compensates for MF's bottom-up RGB32 *input* by
            // flipping there; flipping here as well would just cancel out.
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
    // readAudio() promises the entire audio track, and the shared reader has
    // usually been driven to end-of-file by frameAt()/readFrames() first. Rewinding
    // it with SetCurrentPosition did not bring the audio stream back: the very
    // first ReadSample after the seek returned MF_SOURCE_READERF_ENDOFSTREAM with
    // zero bytes (reads=1 lastFlags=0x2), which is what left VideoIORoundTripTest
    // reporting "reports audio but no PCM samples were returned" on Windows.
    //
    // Open a reader of our own instead. A fresh source reader starts at the
    // beginning by construction, so the audio track no longer depends on seek
    // semantics or on what the video pass did to the shared position.
    ComPtr<IMFSourceReader> audioReader;
    if (st->url != NULL) {
        ComPtr<IMFAttributes> attrs;
        if (SUCCEEDED(MFCreateAttributes(&attrs, 1))) {
            MFCreateSourceReaderFromURL(st->url, attrs.Get(), &audioReader);
        }
    }
    if (audioReader != NULL) {
        /* Configure it completely or not at all. Every call here was previously
         * unchecked, so a reader that opened but could not select its stream or
         * negotiate PCM stayed non-null and was used anyway -- reading an
         * unselected stream, or the source's COMPRESSED native format, while the
         * bytes were handed back with the cached 16-bit PCM rate and channel
         * count beside them. Silent garbage is worse than the seek path this
         * replaced, so a partially configured reader is discarded and the shared
         * reader is rewound instead. */
        bool configured = false;
        if (SUCCEEDED(audioReader->SetStreamSelection((DWORD) MF_SOURCE_READER_ALL_STREAMS, FALSE))
                && SUCCEEDED(audioReader->SetStreamSelection(
                        (DWORD) MF_SOURCE_READER_FIRST_AUDIO_STREAM, TRUE))) {
            ComPtr<IMFMediaType> pcmType;
            if (SUCCEEDED(MFCreateMediaType(&pcmType))) {
                pcmType->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Audio);
                pcmType->SetGUID(MF_MT_SUBTYPE, MFAudioFormat_PCM);
                pcmType->SetUINT32(MF_MT_AUDIO_BITS_PER_SAMPLE, 16);
                configured = SUCCEEDED(audioReader->SetCurrentMediaType(
                        (DWORD) MF_SOURCE_READER_FIRST_AUDIO_STREAM, NULL, pcmType.Get()));
            }
        }
        if (!configured) {
            printf("CN1SS:INFO:winAudio fresh reader configuration failed; rewinding the shared reader\n");
            fflush(stdout);
            audioReader.Reset();
        }
    }
    if (audioReader == NULL) {
        /* No usable fresh reader: fall back to rewinding the shared one. */
        PROPVARIANT pos;
        PropVariantInit(&pos);
        pos.vt = VT_I8;
        pos.hVal.QuadPart = 0;
        st->reader->SetCurrentPosition(GUID_NULL, pos);
        PropVariantClear(&pos);
    }
    IMFSourceReader* src = audioReader != NULL ? audioReader.Get() : st->reader.Get();
    unsigned char* pcm = NULL;
    size_t pcmLen = 0, pcmCap = 0;
    /* Diagnostics: readAudio returning empty is reported by the suite as
     * "reports audio but no PCM samples were returned", which says nothing about
     * WHY Media Foundation produced nothing -- and this only reproduces on a
     * Windows runner. Report the reason once so the next run names it instead of
     * inviting another guess. */
    int loops = 0, nullSamples = 0;
    HRESULT lastHr = S_OK;
    DWORD lastFlags = 0;
    for (;;) {
        DWORD streamFlags = 0;
        LONGLONG timestamp = 0;
        ComPtr<IMFSample> sample;
        loops++;
        lastHr = src->ReadSample((DWORD) MF_SOURCE_READER_FIRST_AUDIO_STREAM, 0, NULL, &streamFlags, &timestamp, &sample);
        lastFlags = streamFlags;
        if (FAILED(lastHr)) {
            break;
        }
        if (streamFlags & MF_SOURCE_READERF_ENDOFSTREAM) {
            break;
        }
        if (sample == NULL) {
            nullSamples++;
            if (nullSamples > 512) {
                break;   /* a stream that only ever ticks would spin here forever */
            }
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
    printf("CN1SS:INFO:winAudio reads=%d nullSamples=%d bytes=%u lastHr=0x%08lx lastFlags=0x%lx rate=%d ch=%d\n",
           loops, nullSamples, (unsigned) pcmLen, (unsigned long) lastHr,
           (unsigned long) lastFlags, st->audioRate, st->audioChannels);
    fflush(stdout);
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
    st->audioSamplesWritten = 0;
    st->audioBytesWritten = 0;
    st->width = width;
    st->height = height;
    st->frameRate = fps;
    st->hasAudio = hasAudio;
    st->audioStream = 0;
    st->audioEncRate = 0;
    st->audioChannels = channels > 0 ? channels : 1;
    st->audioSetupHr = S_OK;

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
        /* Media Foundation's AAC encoder publishes input types at 44100 and
         * 48000 Hz only. Asking it for the caller's rate made AddStream fail for
         * an 8 kHz tone, and the failure was swallowed by clearing hasAudio: the
         * file then carried a video stream alone while the writer reported
         * success, so VideoIORoundTripTest saw a clip whose audio never arrived.
         * Configure the encoder at a rate it accepts and convert on the way in.
         * Byte rate is likewise constrained (the encoder publishes 12000, 16000,
         * 20000 and 24000 bytes/sec), so an unlisted bit rate is rounded to the
         * nearest supported one instead of failing the stream. */
        static const UINT32 aacByteRates[] = { 12000, 16000, 20000, 24000 };
        UINT32 wanted = (UINT32) (audioBitRate / 8);
        UINT32 byteRate = aacByteRates[0];
        for (size_t i = 1; i < sizeof(aacByteRates) / sizeof(aacByteRates[0]); i++) {
            UINT32 best = byteRate > wanted ? byteRate - wanted : wanted - byteRate;
            UINT32 here = aacByteRates[i] > wanted ? aacByteRates[i] - wanted : wanted - aacByteRates[i];
            if (here < best) {
                byteRate = aacByteRates[i];
            }
        }
        st->audioEncRate = cn1AacEncoderRate(sampleRate);
        st->audioChannels = channels > 0 ? channels : 1;

        ComPtr<IMFMediaType> audioOut;
        MFCreateMediaType(&audioOut);
        audioOut->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Audio);
        audioOut->SetGUID(MF_MT_SUBTYPE, MFAudioFormat_AAC);
        audioOut->SetUINT32(MF_MT_AUDIO_BITS_PER_SAMPLE, 16);
        audioOut->SetUINT32(MF_MT_AUDIO_SAMPLES_PER_SECOND, (UINT32) st->audioEncRate);
        audioOut->SetUINT32(MF_MT_AUDIO_NUM_CHANNELS, (UINT32) st->audioChannels);
        audioOut->SetUINT32(MF_MT_AUDIO_AVG_BYTES_PER_SECOND, byteRate);
        st->audioSetupHr = writer->AddStream(audioOut.Get(), &st->audioStream);
        if (SUCCEEDED(st->audioSetupHr)) {
            ComPtr<IMFMediaType> audioIn;
            MFCreateMediaType(&audioIn);
            audioIn->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Audio);
            audioIn->SetGUID(MF_MT_SUBTYPE, MFAudioFormat_PCM);
            audioIn->SetUINT32(MF_MT_AUDIO_BITS_PER_SAMPLE, 16);
            audioIn->SetUINT32(MF_MT_AUDIO_SAMPLES_PER_SECOND, (UINT32) st->audioEncRate);
            audioIn->SetUINT32(MF_MT_AUDIO_NUM_CHANNELS, (UINT32) st->audioChannels);
            /* The PCM input type is under-specified without these two: the
             * encoder needs the frame size and byte rate to accept the type. */
            audioIn->SetUINT32(MF_MT_AUDIO_BLOCK_ALIGNMENT, (UINT32) (2 * st->audioChannels));
            audioIn->SetUINT32(MF_MT_AUDIO_AVG_BYTES_PER_SECOND,
                    (UINT32) (st->audioEncRate * 2 * st->audioChannels));
            st->audioSetupHr = writer->SetInputMediaType(st->audioStream, audioIn.Get(), NULL);
        }
        if (FAILED(st->audioSetupHr)) {
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
    // RGBA (Java, top-down) -> RGB32/BGRA (MF input). Media Foundation treats
    // RGB32 input as bottom-up, so write our rows reversed: buffer row 0 must be
    // the image's bottom row for the encoder to store the frame right-side-up.
    BYTE* bgra = (BYTE*) malloc(len);
    if (bgra == NULL) {
        return;
    }
    for (int y = 0; y < h; y++) {
        const BYTE* srcRow = rgba + (size_t) (h - 1 - y) * w * 4;
        BYTE* dstRow = bgra + (size_t) y * w * 4;
        for (int x = 0; x < w; x++) {
            dstRow[x * 4] = srcRow[x * 4 + 2];
            dstRow[x * 4 + 1] = srcRow[x * 4 + 1];
            dstRow[x * 4 + 2] = srcRow[x * 4];
            dstRow[x * 4 + 3] = srcRow[x * 4 + 3];
        }
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
    int ch = channels > 0 ? channels : 1;
    DWORD frames = len / (DWORD) (2 * ch);
    BYTE* resampled = NULL;

    /* The encoder runs at a rate it will accept (see cn1WriterOpen), so PCM
     * recorded at any other rate is converted here. Linear interpolation between
     * neighbouring frames: enough for the tone the conformance suite records,
     * and it keeps the signal level -- and therefore the RMS the test measures --
     * where the caller put it. */
    if (sampleRate > 0 && st->audioEncRate > 0 && sampleRate != st->audioEncRate && frames > 0) {
        double ratio = (double) st->audioEncRate / (double) sampleRate;
        DWORD outFrames = (DWORD) (frames * ratio);
        if (outFrames > 0) {
            DWORD outLen = outFrames * (DWORD) (2 * ch);
            resampled = (BYTE*) malloc(outLen);
            if (resampled != NULL) {
                const short* in = (const short*) pcm;
                short* out = (short*) resampled;
                for (DWORD f = 0; f < outFrames; f++) {
                    double srcPos = (double) f / ratio;
                    DWORD i0 = (DWORD) srcPos;
                    DWORD i1 = i0 + 1 < frames ? i0 + 1 : frames - 1;
                    double frac = srcPos - (double) i0;
                    for (int c = 0; c < ch; c++) {
                        double a = (double) in[i0 * ch + c];
                        double b = (double) in[i1 * ch + c];
                        out[f * ch + c] = (short) (a + (b - a) * frac);
                    }
                }
                pcm = resampled;
                len = outLen;
                frames = outFrames;
                sampleRate = st->audioEncRate;
            }
        }
    }

    LONGLONG dur = sampleRate > 0 ? (LONGLONG) ((LONGLONG) frames * CN1_HNS_PER_SEC / sampleRate) : 0;
    cn1WriterWriteSample(st, st->audioStream, pcm, len, (LONGLONG) ptsMs * CN1_HNS_PER_MS, dur);
    /* A freshly opened reader cannot be positioned at EOF, yet it still reports
     * ENDOFSTREAM on its first audio read -- so the file carries an audio stream
     * header with no samples behind it, which points here rather than at the
     * reader. Count what actually goes in. */
    st->audioSamplesWritten++;
    st->audioBytesWritten += (unsigned long long) len;
    free(resampled);
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_videoWriterClose___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1VideoWriter* st = WR(peer);
    if (!st) {
        return JAVA_FALSE;
    }
    HRESULT hr = st->writer->Finalize();
    printf("CN1SS:INFO:winWriter audioSamples=%lu audioBytes=%llu hasAudio=%d encRate=%d "
           "audioSetupHr=0x%08lx finalizeHr=0x%08lx\n",
           st->audioSamplesWritten, st->audioBytesWritten, st->hasAudio ? 1 : 0,
           st->audioEncRate, (unsigned long) st->audioSetupHr, (unsigned long) hr);
    fflush(stdout);
    delete st;
    return SUCCEEDED(hr) ? JAVA_TRUE : JAVA_FALSE;
}

} // extern "C"
