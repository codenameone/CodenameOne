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
 * (LinuxAudioOutput, a PushAudioOutput), through PulseAudio's simple API.
 *
 * libpulse-simple is loaded with dlopen() at first use rather than linked, as
 * GStreamer is in cn1_linux_media.c: a machine without it (PipeWire answers the
 * Pulse protocol on current desktops) still starts the application, and
 * audioOutOpen answers 0, which AudioContext reports as unsupported. The few
 * Pulse types used are declared here so no Pulse headers are needed to build.
 *
 * The Java render thread pushes interleaved float frames into a single
 * producer / single consumer ring with audioOutWrite, which never blocks; a
 * native worker thread drains it into pa_simple_write, whose blocking paces it.
 */

#define _GNU_SOURCE
#include "cn1_linux.h"
#include <dlfcn.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define CN1_AO_RING_FRAMES 1024
#define CN1_AO_CHUNK_FRAMES 256

typedef struct { int format; uint32_t rate; uint8_t channels; } cn1_pa_sample_spec;
typedef struct { uint32_t maxlength, tlength, prebuf, minreq, fragsize; } cn1_pa_buffer_attr;
typedef void cn1_pa_simple;
#define CN1_PA_SAMPLE_FLOAT32LE 5
#define CN1_PA_STREAM_PLAYBACK 1

static cn1_pa_simple* (*p_pa_simple_new)(const char*, const char*, int, const char*, const char*,
        const cn1_pa_sample_spec*, const void*, const cn1_pa_buffer_attr*, int*);
static int (*p_pa_simple_write)(cn1_pa_simple*, const void*, size_t, int*);
static void (*p_pa_simple_free)(cn1_pa_simple*);
static int cn1_pa_state = 0; /* 0 untried, 1 loaded, -1 unavailable */

static int cn1LoadPulse(void) {
    void* lib;
    if (cn1_pa_state != 0) {
        return cn1_pa_state > 0;
    }
    lib = dlopen("libpulse-simple.so.0", RTLD_LAZY);
    if (!lib) {
        lib = dlopen("libpulse-simple.so", RTLD_LAZY);
    }
    if (lib) {
        p_pa_simple_new = dlsym(lib, "pa_simple_new");
        p_pa_simple_write = dlsym(lib, "pa_simple_write");
        p_pa_simple_free = dlsym(lib, "pa_simple_free");
    }
    cn1_pa_state = lib && p_pa_simple_new && p_pa_simple_write && p_pa_simple_free ? 1 : -1;
    return cn1_pa_state > 0;
}

typedef struct {
    float* ring;
    int channels;
    int rate;
    long long writePos;
    long long readPos;
    int running;
    int alive;
    cn1_pa_simple* stream;
    pthread_t thread;
    float* chunk;
} CN1AudioOut;

static void* cn1AoThread(void* arg) {
    CN1AudioOut* o = (CN1AudioOut*) arg;
    int ch = o->channels;
    while (__atomic_load_n(&o->alive, __ATOMIC_ACQUIRE)) {
        long long r;
        long long avail;
        int n;
        int i;
        int err = 0;
        if (!__atomic_load_n(&o->running, __ATOMIC_ACQUIRE)) {
            usleep(10000);
            continue;
        }
        r = o->readPos;
        avail = __atomic_load_n(&o->writePos, __ATOMIC_ACQUIRE) - r;
        n = (int) (avail < CN1_AO_CHUNK_FRAMES ? avail : CN1_AO_CHUNK_FRAMES);
        for (i = 0; i < n; i++) {
            memcpy(o->chunk + i * ch, o->ring + (int) ((r + i) % CN1_AO_RING_FRAMES) * ch, sizeof(float) * (size_t) ch);
        }
        memset(o->chunk + n * ch, 0, sizeof(float) * (size_t) ((CN1_AO_CHUNK_FRAMES - n) * ch));
        __atomic_store_n(&o->readPos, r + n, __ATOMIC_RELEASE);
        p_pa_simple_write(o->stream, o->chunk, sizeof(float) * (size_t) (CN1_AO_CHUNK_FRAMES * ch), &err);
    }
    return 0;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_audioOutOpen___int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT sampleRate, JAVA_INT channels) {
    CN1AudioOut* o;
    cn1_pa_sample_spec ss;
    cn1_pa_buffer_attr attr;
    int err = 0;
    int ch = channels == 1 ? 1 : 2;
    int rate = sampleRate > 0 ? sampleRate : 48000;
    if (!cn1LoadPulse()) {
        return 0;
    }
    ss.format = CN1_PA_SAMPLE_FLOAT32LE;
    ss.rate = (uint32_t) rate;
    ss.channels = (uint8_t) ch;
    attr.maxlength = (uint32_t) -1;
    attr.tlength = (uint32_t) (sizeof(float) * CN1_AO_CHUNK_FRAMES * 2 * ch);
    attr.prebuf = (uint32_t) -1;
    attr.minreq = (uint32_t) -1;
    attr.fragsize = (uint32_t) -1;
    o = (CN1AudioOut*) calloc(1, sizeof(CN1AudioOut));
    if (o == NULL) {
        return 0;
    }
    o->stream = p_pa_simple_new(NULL, "Codename One", CN1_PA_STREAM_PLAYBACK, NULL, "AudioContext", &ss, NULL, &attr, &err);
    if (!o->stream) {
        free(o);
        return 0;
    }
    o->channels = ch;
    o->rate = rate;
    o->ring = (float*) calloc((size_t) (CN1_AO_RING_FRAMES * ch), sizeof(float));
    o->chunk = (float*) calloc((size_t) (CN1_AO_CHUNK_FRAMES * ch), sizeof(float));
    o->alive = 1;
    if (pthread_create(&o->thread, NULL, cn1AoThread, o) != 0) {
        p_pa_simple_free(o->stream);
        free(o->ring);
        free(o->chunk);
        free(o);
        return 0;
    }
    return (JAVA_LONG) (intptr_t) o;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_audioOutRate___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    return o == NULL ? 0 : o->rate;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_audioOutWrite___long_float_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT pcm, JAVA_INT offset, JAVA_INT frames) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    const JAVA_ARRAY_FLOAT* src;
    long long w;
    long long space;
    int n;
    int i;
    if (o == NULL || pcm == JAVA_NULL || frames <= 0) {
        return 0;
    }
    src = (const JAVA_ARRAY_FLOAT*) CN1_ARRAY_DATA(pcm);
    w = o->writePos;
    space = CN1_AO_RING_FRAMES - (w - __atomic_load_n(&o->readPos, __ATOMIC_ACQUIRE));
    n = (int) (space < frames ? space : frames);
    for (i = 0; i < n; i++) {
        memcpy(o->ring + (int) ((w + i) % CN1_AO_RING_FRAMES) * o->channels,
               src + (offset + i) * o->channels, sizeof(float) * (size_t) o->channels);
    }
    __atomic_store_n(&o->writePos, w + n, __ATOMIC_RELEASE);
    return n;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_audioOutStart___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    if (o != NULL) {
        __atomic_store_n(&o->running, 1, __ATOMIC_RELEASE);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_audioOutStop___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    if (o != NULL) {
        __atomic_store_n(&o->running, 0, __ATOMIC_RELEASE);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_audioOutClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    if (o == NULL) {
        return;
    }
    __atomic_store_n(&o->alive, 0, __ATOMIC_RELEASE);
    pthread_join(o->thread, NULL);
    p_pa_simple_free(o->stream);
    free(o->ring);
    free(o->chunk);
    free(o);
}

JAVA_DOUBLE com_codename1_impl_linux_LinuxNative_audioOutLatency___long_R_double(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*) (intptr_t) handle;
    if (o == NULL) {
        return 0;
    }
    /* The ring, the chunk being written and the server's target length (two
     * chunks). pa_simple_get_latency would be exact, but a pa_simple is not
     * thread safe and the worker is inside pa_simple_write. */
    return (CN1_AO_RING_FRAMES + 3 * CN1_AO_CHUNK_FRAMES) / (double) o->rate;
}
