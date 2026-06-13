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
 * Audio recording for the Windows port via the classic waveIn (winmm) capture
 * API, writing a PCM WAV file. waveIn is chosen over a Media Foundation encode
 * pipeline because it is dependency-free and robust: there is no codec / media-
 * type negotiation to get wrong, and a 16-bit PCM WAV is universally playable
 * (and is itself decodable by the port's MF-based playback). This backs
 * WindowsImplementation.createMediaRecorder / captureAudio.
 *
 * A worker thread drains filled capture buffers to the file as they arrive (the
 * waveIn callback only signals an event -- per MSDN you must not call waveIn*
 * from the callback). Stop flushes, patches the RIFF/data sizes into the header,
 * and closes.
 */

#ifdef _WIN32

#include "cn1_windows.h"
#include <mmsystem.h>
#include <stdlib.h>
#include <string.h>

#define CN1_REC_BUFFERS 4
#define CN1_REC_BUFBYTES 8192

typedef struct CN1AudioRec {
    HWAVEIN hwi;
    HANDLE file;
    HANDLE event;          /* signaled by the waveIn callback when a buffer is done */
    HANDLE thread;
    WAVEHDR headers[CN1_REC_BUFFERS];
    char* buffers[CN1_REC_BUFFERS];
    volatile LONG recording;
    DWORD dataBytes;       /* total PCM bytes written (for the WAV header) */
    WAVEFORMATEX fmt;
} CN1AudioRec;

/* Writes a 44-byte WAV/RIFF header with placeholder sizes (patched on stop). */
static int cn1RecWriteHeader(CN1AudioRec* r) {
    unsigned char h[44];
    DWORD wr = 0;
    DWORD byteRate = r->fmt.nSamplesPerSec * r->fmt.nBlockAlign;
    memcpy(h, "RIFF", 4);
    h[4] = h[5] = h[6] = h[7] = 0;            /* RIFF chunk size, patched later */
    memcpy(h + 8, "WAVE", 4);
    memcpy(h + 12, "fmt ", 4);
    h[16] = 16; h[17] = h[18] = h[19] = 0;    /* fmt chunk size = 16 (PCM) */
    h[20] = 1; h[21] = 0;                      /* audio format = PCM */
    h[22] = (unsigned char) r->fmt.nChannels; h[23] = 0;
    h[24] = (unsigned char) (r->fmt.nSamplesPerSec & 0xff);
    h[25] = (unsigned char) ((r->fmt.nSamplesPerSec >> 8) & 0xff);
    h[26] = (unsigned char) ((r->fmt.nSamplesPerSec >> 16) & 0xff);
    h[27] = (unsigned char) ((r->fmt.nSamplesPerSec >> 24) & 0xff);
    h[28] = (unsigned char) (byteRate & 0xff);
    h[29] = (unsigned char) ((byteRate >> 8) & 0xff);
    h[30] = (unsigned char) ((byteRate >> 16) & 0xff);
    h[31] = (unsigned char) ((byteRate >> 24) & 0xff);
    h[32] = (unsigned char) r->fmt.nBlockAlign; h[33] = 0;
    h[34] = (unsigned char) r->fmt.wBitsPerSample; h[35] = 0;
    memcpy(h + 36, "data", 4);
    h[40] = h[41] = h[42] = h[43] = 0;        /* data chunk size, patched later */
    return WriteFile(r->file, h, 44, &wr, NULL) && wr == 44;
}

/* Patches RIFF size (offset 4) and data size (offset 40) once recording stops. */
static void cn1RecPatchHeader(CN1AudioRec* r) {
    DWORD wr = 0;
    DWORD riff = 36 + r->dataBytes;
    unsigned char v[4];
    v[0] = (unsigned char) (riff & 0xff); v[1] = (unsigned char) ((riff >> 8) & 0xff);
    v[2] = (unsigned char) ((riff >> 16) & 0xff); v[3] = (unsigned char) ((riff >> 24) & 0xff);
    SetFilePointer(r->file, 4, NULL, FILE_BEGIN);
    WriteFile(r->file, v, 4, &wr, NULL);
    v[0] = (unsigned char) (r->dataBytes & 0xff); v[1] = (unsigned char) ((r->dataBytes >> 8) & 0xff);
    v[2] = (unsigned char) ((r->dataBytes >> 16) & 0xff); v[3] = (unsigned char) ((r->dataBytes >> 24) & 0xff);
    SetFilePointer(r->file, 40, NULL, FILE_BEGIN);
    WriteFile(r->file, v, 4, &wr, NULL);
}

static DWORD WINAPI cn1RecThread(LPVOID arg) {
    CN1AudioRec* r = (CN1AudioRec*) arg;
    while (r->recording) {
        WaitForSingleObject(r->event, 100);
        for (int i = 0; i < CN1_REC_BUFFERS; i++) {
            WAVEHDR* hdr = &r->headers[i];
            if ((hdr->dwFlags & WHDR_DONE) != 0) {
                if (hdr->dwBytesRecorded > 0) {
                    DWORD wr = 0;
                    if (WriteFile(r->file, hdr->lpData, hdr->dwBytesRecorded, &wr, NULL)) {
                        r->dataBytes += wr;
                    }
                }
                hdr->dwFlags &= ~WHDR_DONE;
                if (r->recording) {
                    waveInAddBuffer(r->hwi, hdr, sizeof(WAVEHDR));
                }
            }
        }
    }
    return 0;
}

/* Opens the default capture device, starts recording into a WAV at `path`, and
 * returns an opaque peer (0 on failure). */
JAVA_LONG com_codename1_impl_windows_WindowsNative_audioRecStart___java_lang_String_int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj, JAVA_INT sampleRate, JAVA_INT channels) {
    CN1AudioRec* r;
    WCHAR* path;
    int i;
    if (pathObj == JAVA_NULL) {
        return 0;
    }
    r = (CN1AudioRec*) calloc(1, sizeof(CN1AudioRec));
    if (r == NULL) {
        return 0;
    }
    r->fmt.wFormatTag = WAVE_FORMAT_PCM;
    r->fmt.nChannels = (WORD) (channels > 0 ? channels : 1);
    r->fmt.nSamplesPerSec = (DWORD) (sampleRate > 0 ? sampleRate : 44100);
    r->fmt.wBitsPerSample = 16;
    r->fmt.nBlockAlign = (WORD) (r->fmt.nChannels * r->fmt.wBitsPerSample / 8);
    r->fmt.nAvgBytesPerSec = r->fmt.nSamplesPerSec * r->fmt.nBlockAlign;
    r->fmt.cbSize = 0;

    path = cn1WinJavaStringToWide(threadStateData, pathObj, NULL);
    if (path == NULL) {
        free(r);
        return 0;
    }
    r->file = CreateFileW(path, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    free(path);
    if (r->file == INVALID_HANDLE_VALUE) {
        free(r);
        return 0;
    }
    if (!cn1RecWriteHeader(r)) {
        CloseHandle(r->file);
        free(r);
        return 0;
    }
    r->event = CreateEventW(NULL, FALSE, FALSE, NULL);
    if (waveInOpen(&r->hwi, WAVE_MAPPER, &r->fmt, (DWORD_PTR) r->event, 0, CALLBACK_EVENT) != MMSYSERR_NOERROR) {
        CloseHandle(r->file);
        CloseHandle(r->event);
        free(r);
        return 0;
    }
    for (i = 0; i < CN1_REC_BUFFERS; i++) {
        r->buffers[i] = (char*) malloc(CN1_REC_BUFBYTES);
        ZeroMemory(&r->headers[i], sizeof(WAVEHDR));
        r->headers[i].lpData = r->buffers[i];
        r->headers[i].dwBufferLength = CN1_REC_BUFBYTES;
        waveInPrepareHeader(r->hwi, &r->headers[i], sizeof(WAVEHDR));
        waveInAddBuffer(r->hwi, &r->headers[i], sizeof(WAVEHDR));
    }
    r->recording = 1;
    r->thread = CreateThread(NULL, 0, cn1RecThread, r, 0, NULL);
    waveInStart(r->hwi);
    return (JAVA_LONG) (intptr_t) r;
}

/* Stops recording, flushes buffers, patches the WAV header and frees the peer. */
JAVA_VOID com_codename1_impl_windows_WindowsNative_audioRecStop___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1AudioRec* r = (CN1AudioRec*) (intptr_t) peer;
    int i;
    if (r == NULL) {
        return;
    }
    InterlockedExchange(&r->recording, 0);
    SetEvent(r->event);
    if (r->thread != NULL) {
        WaitForSingleObject(r->thread, 2000);
        CloseHandle(r->thread);
    }
    waveInStop(r->hwi);
    waveInReset(r->hwi);
    /* Drain any final buffers the reset returned. */
    for (i = 0; i < CN1_REC_BUFFERS; i++) {
        WAVEHDR* hdr = &r->headers[i];
        if ((hdr->dwFlags & WHDR_DONE) != 0 && hdr->dwBytesRecorded > 0) {
            DWORD wr = 0;
            if (WriteFile(r->file, hdr->lpData, hdr->dwBytesRecorded, &wr, NULL)) {
                r->dataBytes += wr;
            }
        }
        waveInUnprepareHeader(r->hwi, hdr, sizeof(WAVEHDR));
        free(r->buffers[i]);
    }
    waveInClose(r->hwi);
    cn1RecPatchHeader(r);
    CloseHandle(r->file);
    CloseHandle(r->event);
    free(r);
}

#endif /* _WIN32 */
