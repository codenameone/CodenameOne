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
 * Real-time PCM output for com.codename1.media.audio.AudioContext
 * (WindowsAudioOutput, a PushAudioOutput), through the classic waveOut (winmm)
 * API the port already links for waveIn recording.
 *
 * The Java render thread pushes interleaved float frames into a single
 * producer / single consumer ring with audioOutWrite, which never blocks. A
 * native worker thread keeps CN1_AO_BUFFERS waveOut buffers queued, refilling
 * each one from the ring (silence on an underrun) when the driver signals it
 * done. waveOut* is never called from the driver callback, per MSDN.
 */

#if defined(_WIN32) && !defined(CN1_WINDOWS_UWP) /* desktop only: stubbed for UWP in cn1_windows_uwp_stubs.cpp */

#include "cn1_windows.h"
#include <mmsystem.h>
#include <stdlib.h>
#include <string.h>

#define CN1_AO_RING_FRAMES 1024
#define CN1_AO_BUFFERS 4
#define CN1_AO_BUFFER_FRAMES 256

typedef struct {
    float* ring;
    int channels;
    int rate;
    LONG64 writePos;
    LONG64 readPos;
    HWAVEOUT wave;
    HANDLE event;
    HANDLE thread;
    volatile LONG alive;
    WAVEHDR headers[CN1_AO_BUFFERS];
    short* pcm[CN1_AO_BUFFERS];
} CN1AudioOut;

static LONG64 cn1AoLoad(LONG64 volatile* p) {
    return InterlockedCompareExchange64(p, 0, 0);
}

static void cn1AoFill(CN1AudioOut* o, int b) {
    short* d = o->pcm[b];
    LONG64 r = o->readPos;
    LONG64 avail = cn1AoLoad(&o->writePos) - r;
    int n = (int) (avail < CN1_AO_BUFFER_FRAMES ? avail : CN1_AO_BUFFER_FRAMES);
    int ch = o->channels;
    int i;
    int c;
    for (i = 0; i < n; i++) {
        const float* s = o->ring + (int) ((r + i) % CN1_AO_RING_FRAMES) * ch;
        for (c = 0; c < ch; c++) {
            float v = s[c];
            v = v > 1.0f ? 1.0f : (v < -1.0f ? -1.0f : v);
            d[i * ch + c] = (short) (v * 32767.0f);
        }
    }
    memset(d + n * ch, 0, sizeof(short) * (size_t) ((CN1_AO_BUFFER_FRAMES - n) * ch));
    InterlockedExchange64(&o->readPos, r + n);
    waveOutWrite(o->wave, &o->headers[b], sizeof(WAVEHDR));
}

static DWORD WINAPI cn1AoThread(LPVOID arg) {
    CN1AudioOut* o = (CN1AudioOut*) arg;
    int b;
    for (b = 0; b < CN1_AO_BUFFERS; b++) {
        cn1AoFill(o, b);
    }
    while (InterlockedCompareExchange(&o->alive, 0, 0)) {
        WaitForSingleObject(o->event, 50);
        for (b = 0; b < CN1_AO_BUFFERS; b++) {
            if ((o->headers[b].dwFlags & WHDR_DONE) && InterlockedCompareExchange(&o->alive, 0, 0)) {
                cn1AoFill(o, b);
            }
        }
    }
    return 0;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_audioOutOpen___int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_INT sampleRate, JAVA_INT channels) {
    CN1AudioOut* o;
    WAVEFORMATEX fmt;
    int b;
    int ch = channels == 1 ? 1 : 2;
    int rate = sampleRate > 0 ? sampleRate : 48000;
    o = (CN1AudioOut*) calloc(1, sizeof(CN1AudioOut));
    if (o == NULL) {
        return 0;
    }
    o->channels = ch;
    o->rate = rate;
    o->ring = (float*) calloc((size_t) (CN1_AO_RING_FRAMES * ch), sizeof(float));
    o->event = CreateEvent(NULL, FALSE, FALSE, NULL);
    memset(&fmt, 0, sizeof(fmt));
    fmt.wFormatTag = WAVE_FORMAT_PCM;
    fmt.nChannels = (WORD) ch;
    fmt.nSamplesPerSec = (DWORD) rate;
    fmt.wBitsPerSample = 16;
    fmt.nBlockAlign = (WORD) (ch * 2);
    fmt.nAvgBytesPerSec = (DWORD) (rate * ch * 2);
    if (o->ring == NULL || o->event == NULL
            || waveOutOpen(&o->wave, WAVE_MAPPER, &fmt, (DWORD_PTR) o->event, 0, CALLBACK_EVENT) != MMSYSERR_NOERROR) {
        if (o->event) CloseHandle(o->event);
        free(o->ring);
        free(o);
        return 0;
    }
    waveOutPause(o->wave);
    for (b = 0; b < CN1_AO_BUFFERS; b++) {
        o->pcm[b] = (short*) calloc((size_t) (CN1_AO_BUFFER_FRAMES * ch), sizeof(short));
        o->headers[b].lpData = (LPSTR) o->pcm[b];
        o->headers[b].dwBufferLength = (DWORD) (CN1_AO_BUFFER_FRAMES * ch * sizeof(short));
        waveOutPrepareHeader(o->wave, &o->headers[b], sizeof(WAVEHDR));
    }
    o->alive = 1;
    o->thread = CreateThread(NULL, 0, cn1AoThread, o, 0, NULL);
    if (o->thread) {
        SetThreadPriority(o->thread, THREAD_PRIORITY_TIME_CRITICAL);
    }
    return (JAVA_LONG) (intptr_t) o;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_audioOutRate___long_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    return o == NULL ? 0 : o->rate;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_audioOutWrite___long_float_1ARRAY_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT pcm, JAVA_INT offset, JAVA_INT frames) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    const JAVA_ARRAY_FLOAT* src;
    LONG64 w;
    LONG64 space;
    int n;
    int i;
    if (o == NULL || pcm == JAVA_NULL || frames <= 0) {
        return 0;
    }
    src = (const JAVA_ARRAY_FLOAT*) CN1_ARRAY_DATA(pcm);
    w = o->writePos;
    space = CN1_AO_RING_FRAMES - (w - cn1AoLoad(&o->readPos));
    n = (int) (space < frames ? space : frames);
    for (i = 0; i < n; i++) {
        memcpy(o->ring + (int) ((w + i) % CN1_AO_RING_FRAMES) * o->channels,
               src + (offset + i) * o->channels, sizeof(float) * (size_t) o->channels);
    }
    InterlockedExchange64(&o->writePos, w + n);
    return n;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_audioOutStart___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    if (o != NULL) {
        waveOutRestart(o->wave);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_audioOutStop___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    if (o != NULL) {
        waveOutPause(o->wave);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_audioOutClose___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    int b;
    if (o == NULL) {
        return;
    }
    InterlockedExchange(&o->alive, 0);
    SetEvent(o->event);
    if (o->thread) {
        WaitForSingleObject(o->thread, 2000);
        CloseHandle(o->thread);
    }
    waveOutReset(o->wave);
    for (b = 0; b < CN1_AO_BUFFERS; b++) {
        waveOutUnprepareHeader(o->wave, &o->headers[b], sizeof(WAVEHDR));
        free(o->pcm[b]);
    }
    waveOutClose(o->wave);
    CloseHandle(o->event);
    free(o->ring);
    free(o);
}

JAVA_DOUBLE com_codename1_impl_windows_WindowsNative_audioOutLatency___long_R_double(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    if (o == NULL) {
        return 0;
    }
    return (CN1_AO_RING_FRAMES + CN1_AO_BUFFERS * CN1_AO_BUFFER_FRAMES) / (double) o->rate;
}

#endif
