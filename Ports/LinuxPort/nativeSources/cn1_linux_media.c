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
 * Media playback, audio recording and camera capture for the native Codename One
 * Linux port, backed by GStreamer. Playback is a playbin pipeline (audio + video);
 * recording is autoaudiosrc -> wavenc -> filesink; camera capture pulls frames
 * from v4l2src via an appsink as CN1 ARGB (BGRA byte order == 0xAARRGGBB LE).
 */

#define _GNU_SOURCE
#include "cn1_linux.h"
#include <gst/gst.h>
#include <gst/app/gstappsink.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <pthread.h>

extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim);
extern struct clazz class_array1__JAVA_INT;

static void cn1GstInit(void) {
    static int inited = 0;
    if (!inited) {
        gst_init(0, 0);
        inited = 1;
    }
}

/* ------------------------------------------------------------ playback */

typedef struct {
    GstElement* playbin;
    char* tempPath;
} CN1Media;

JAVA_LONG com_codename1_impl_linux_LinuxNative_mediaCreate___byte_1ARRAY_int_java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data, JAVA_INT length, JAVA_OBJECT mimeType) {
    CN1Media* m;
    char tmpl[] = "/tmp/cn1mediaXXXXXX";
    int fd;
    unsigned char* bytes;
    char* uri;
    (void) mimeType;
    if (data == JAVA_NULL || length <= 0) {
        return 0;
    }
    cn1GstInit();
    fd = mkstemp(tmpl);
    if (fd < 0) {
        return 0;
    }
    bytes = (unsigned char*) (*(JAVA_ARRAY) data).data;
    if (write(fd, bytes, (size_t) length) != length) {
        close(fd);
        unlink(tmpl);
        return 0;
    }
    close(fd);
    m = (CN1Media*) calloc(1, sizeof(CN1Media));
    m->tempPath = strdup(tmpl);
    m->playbin = gst_element_factory_make("playbin", 0);
    uri = g_strconcat("file://", tmpl, NULL);
    g_object_set(m->playbin, "uri", uri, NULL);
    g_free(uri);
    gst_element_set_state(m->playbin, GST_STATE_PAUSED);
    return (JAVA_LONG) (intptr_t) m;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mediaPlay___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    if (m) {
        gst_element_set_state(m->playbin, GST_STATE_PLAYING);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mediaPause___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    if (m) {
        gst_element_set_state(m->playbin, GST_STATE_PAUSED);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mediaSetTime___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT millis) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    if (m) {
        gst_element_seek_simple(m->playbin, GST_FORMAT_TIME,
                GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_KEY_UNIT,
                (gint64) millis * GST_MSECOND);
    }
}

JAVA_INT com_codename1_impl_linux_LinuxNative_mediaGetTime___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    gint64 pos = 0;
    if (m && gst_element_query_position(m->playbin, GST_FORMAT_TIME, &pos)) {
        return (JAVA_INT) (pos / GST_MSECOND);
    }
    return 0;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_mediaGetDuration___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    gint64 dur = 0;
    if (m && gst_element_query_duration(m->playbin, GST_FORMAT_TIME, &dur)) {
        return (JAVA_INT) (dur / GST_MSECOND);
    }
    return 0;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mediaSetVolume___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT volume) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    if (m) {
        gdouble v = volume / 100.0;
        if (v < 0) v = 0;
        if (v > 1) v = 1;
        g_object_set(m->playbin, "volume", v, NULL);
    }
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_mediaIsPlaying___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    GstState state = GST_STATE_NULL;
    if (m) {
        gst_element_get_state(m->playbin, &state, 0, 0);
        return state == GST_STATE_PLAYING ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_mediaIsEnded___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    GstMessage* msg;
    int ended = 0;
    if (!m) {
        return JAVA_FALSE;
    }
    msg = gst_bus_pop_filtered(GST_ELEMENT_BUS(m->playbin), GST_MESSAGE_EOS);
    if (msg) {
        ended = 1;
        gst_message_unref(msg);
    }
    return ended ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_mediaIsVideo___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    gint nVideo = 0;
    if (m) {
        g_object_get(m->playbin, "n-video", &nVideo, NULL);
        return nVideo > 0 ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mediaDestroy___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    if (!m) {
        return;
    }
    gst_element_set_state(m->playbin, GST_STATE_NULL);
    gst_object_unref(m->playbin);
    if (m->tempPath) {
        unlink(m->tempPath);
        free(m->tempPath);
    }
    free(m);
}

/* -------------------------------------------------------- audio recording */

typedef struct { GstElement* pipeline; } CN1AudioRec;

JAVA_LONG com_codename1_impl_linux_LinuxNative_audioRecStart___java_lang_String_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path, JAVA_INT sampleRate, JAVA_INT channels) {
    const char* p = path == JAVA_NULL ? 0 : stringToUTF8(threadStateData, path);
    CN1AudioRec* rec;
    char* desc;
    GError* err = 0;
    int rate = sampleRate > 0 ? sampleRate : 44100;
    int ch = channels > 0 ? channels : 1;
    if (!p) {
        return 0;
    }
    cn1GstInit();
    desc = g_strdup_printf(
            "autoaudiosrc ! audioconvert ! audioresample ! audio/x-raw,rate=%d,channels=%d ! wavenc ! filesink location=\"%s\"",
            rate, ch, p);
    rec = (CN1AudioRec*) calloc(1, sizeof(CN1AudioRec));
    rec->pipeline = gst_parse_launch(desc, &err);
    g_free(desc);
    if (!rec->pipeline) {
        if (err) g_error_free(err);
        free(rec);
        return 0;
    }
    gst_element_set_state(rec->pipeline, GST_STATE_PLAYING);
    return (JAVA_LONG) (intptr_t) rec;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_audioRecStop___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1AudioRec* rec = (CN1AudioRec*) (intptr_t) peer;
    if (!rec) {
        return;
    }
    /* Send EOS so wavenc finalizes the header, wait briefly, then tear down. */
    gst_element_send_event(rec->pipeline, gst_event_new_eos());
    {
        GstMessage* msg = gst_bus_timed_pop_filtered(GST_ELEMENT_BUS(rec->pipeline),
                2 * GST_SECOND, GST_MESSAGE_EOS | GST_MESSAGE_ERROR);
        if (msg) {
            gst_message_unref(msg);
        }
    }
    gst_element_set_state(rec->pipeline, GST_STATE_NULL);
    gst_object_unref(rec->pipeline);
    free(rec);
}

/* --------------------------------------------------------------- camera */

/* Pulls one BGRA sample from an appsink into a freshly allocated CN1 ARGB int[]. */
static JAVA_OBJECT cn1CameraSampleToArgb(CODENAME_ONE_THREAD_STATE, GstSample* sample, int* outDims) {
    GstBuffer* buf = gst_sample_get_buffer(sample);
    GstCaps* caps = gst_sample_get_caps(sample);
    GstStructure* s = gst_caps_get_structure(caps, 0);
    int w = 0, h = 0;
    GstMapInfo map;
    JAVA_OBJECT arr;
    gst_structure_get_int(s, "width", &w);
    gst_structure_get_int(s, "height", &h);
    if (w <= 0 || h <= 0) {
        return JAVA_NULL;
    }
    if (!gst_buffer_map(buf, &map, GST_MAP_READ)) {
        return JAVA_NULL;
    }
    arr = allocArray(threadStateData, w * h, &class_array1__JAVA_INT, sizeof(JAVA_INT), 1);
    if (arr != JAVA_NULL && map.size >= (gsize) (w * h * 4)) {
        memcpy((*(JAVA_ARRAY) arr).data, map.data, (size_t) (w * h * 4));
    }
    gst_buffer_unmap(buf, &map);
    if (outDims) {
        outDims[0] = w;
        outDims[1] = h;
    }
    return arr;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_cameraCaptureFrame___int_1ARRAY_R_int_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT outDims) {
    GstElement* pipe;
    GstElement* sink;
    GstSample* sample;
    GError* err = 0;
    JAVA_OBJECT result = JAVA_NULL;
    int dims[2] = { 0, 0 };
    cn1GstInit();
    pipe = gst_parse_launch(
            "v4l2src num-buffers=1 ! videoconvert ! video/x-raw,format=BGRA ! appsink name=s max-buffers=1 drop=true sync=false",
            &err);
    if (!pipe) {
        if (err) g_error_free(err);
        return JAVA_NULL;
    }
    sink = gst_bin_get_by_name(GST_BIN(pipe), "s");
    gst_element_set_state(pipe, GST_STATE_PLAYING);
    sample = gst_app_sink_try_pull_sample(GST_APP_SINK(sink), 5 * GST_SECOND);
    if (sample) {
        result = cn1CameraSampleToArgb(threadStateData, sample, dims);
        gst_sample_unref(sample);
    }
    gst_element_set_state(pipe, GST_STATE_NULL);
    gst_object_unref(sink);
    gst_object_unref(pipe);
    if (outDims != JAVA_NULL && (*(JAVA_ARRAY) outDims).length >= 2) {
        ((JAVA_INT*) (*(JAVA_ARRAY) outDims).data)[0] = dims[0];
        ((JAVA_INT*) (*(JAVA_ARRAY) outDims).data)[1] = dims[1];
    }
    return result;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_cameraEnumerate___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    GstDeviceMonitor* mon;
    GList* devices;
    GList* it;
    GString* out = g_string_new("");
    cn1GstInit();
    mon = gst_device_monitor_new();
    gst_device_monitor_add_filter(mon, "Video/Source", 0);
    devices = gst_device_monitor_get_devices(mon);
    for (it = devices; it != 0; it = it->next) {
        GstDevice* dev = (GstDevice*) it->data;
        char* name = gst_device_get_display_name(dev);
        g_string_append_printf(out, "%s|false|0|0;", name ? name : "Camera");
        g_free(name);
        gst_object_unref(dev);
    }
    g_list_free(devices);
    gst_object_unref(mon);
    {
        JAVA_OBJECT r = newStringFromCString(threadStateData, out->str);
        g_string_free(out, TRUE);
        return r;
    }
}

/* Continuous capture session: a worker thread runs the appsink pipeline and keeps
 * the latest frame; cameraSessionLatestFrame copies it out. */
typedef struct {
    GstElement* pipe;
    GstElement* sink;
    pthread_t thread;
    pthread_mutex_t lock;
    unsigned char* frame;   /* latest BGRA bytes */
    int w, h;
    volatile int running;
    volatile int paused;
} CN1CameraSession;

static void* cn1CameraThread(void* arg) {
    CN1CameraSession* s = (CN1CameraSession*) arg;
    while (s->running) {
        GstSample* sample;
        if (s->paused) {
            usleep(50000);
            continue;
        }
        sample = gst_app_sink_try_pull_sample(GST_APP_SINK(s->sink), 200 * GST_MSECOND);
        if (!sample) {
            continue;
        }
        {
            GstBuffer* buf = gst_sample_get_buffer(sample);
            GstStructure* st = gst_caps_get_structure(gst_sample_get_caps(sample), 0);
            int w = 0, h = 0;
            GstMapInfo map;
            gst_structure_get_int(st, "width", &w);
            gst_structure_get_int(st, "height", &h);
            if (w > 0 && h > 0 && gst_buffer_map(buf, &map, GST_MAP_READ)) {
                pthread_mutex_lock(&s->lock);
                if (s->w != w || s->h != h) {
                    free(s->frame);
                    s->frame = (unsigned char*) malloc((size_t) w * h * 4);
                    s->w = w;
                    s->h = h;
                }
                if (map.size >= (gsize) (w * h * 4)) {
                    memcpy(s->frame, map.data, (size_t) w * h * 4);
                }
                pthread_mutex_unlock(&s->lock);
                gst_buffer_unmap(buf, &map);
            }
        }
        gst_sample_unref(sample);
    }
    return 0;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_cameraSessionStart___int_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT deviceIndex, JAVA_INT reqW, JAVA_INT reqH) {
    CN1CameraSession* s;
    GError* err = 0;
    char* desc;
    char wcaps[32] = "";
    char hcaps[32] = "";
    (void) deviceIndex;
    cn1GstInit();
    if (reqW > 0) {
        snprintf(wcaps, sizeof(wcaps), ",width=%d", reqW);
    }
    if (reqH > 0) {
        snprintf(hcaps, sizeof(hcaps), ",height=%d", reqH);
    }
    desc = g_strdup_printf(
            "v4l2src ! videoconvert ! video/x-raw,format=BGRA%s%s ! appsink name=s max-buffers=1 drop=true sync=false",
            wcaps, hcaps);
    s = (CN1CameraSession*) calloc(1, sizeof(CN1CameraSession));
    pthread_mutex_init(&s->lock, 0);
    s->pipe = gst_parse_launch(desc, &err);
    g_free(desc);
    if (!s->pipe) {
        if (err) g_error_free(err);
        free(s);
        return 0;
    }
    s->sink = gst_bin_get_by_name(GST_BIN(s->pipe), "s");
    gst_element_set_state(s->pipe, GST_STATE_PLAYING);
    s->running = 1;
    pthread_create(&s->thread, 0, cn1CameraThread, s);
    return (JAVA_LONG) (intptr_t) s;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_cameraSessionStop___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1CameraSession* s = (CN1CameraSession*) (intptr_t) handle;
    if (!s) {
        return;
    }
    s->running = 0;
    pthread_join(s->thread, 0);
    gst_element_set_state(s->pipe, GST_STATE_NULL);
    gst_object_unref(s->sink);
    gst_object_unref(s->pipe);
    free(s->frame);
    pthread_mutex_destroy(&s->lock);
    free(s);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_cameraSessionSetPaused___long_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_BOOLEAN paused) {
    CN1CameraSession* s = (CN1CameraSession*) (intptr_t) handle;
    if (s) {
        s->paused = paused ? 1 : 0;
    }
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_cameraSessionLatestFrame___long_int_1ARRAY_R_int_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT outDims) {
    CN1CameraSession* s = (CN1CameraSession*) (intptr_t) handle;
    JAVA_OBJECT arr = JAVA_NULL;
    if (!s) {
        return JAVA_NULL;
    }
    pthread_mutex_lock(&s->lock);
    if (s->frame && s->w > 0 && s->h > 0) {
        arr = allocArray(threadStateData, s->w * s->h, &class_array1__JAVA_INT, sizeof(JAVA_INT), 1);
        if (arr != JAVA_NULL) {
            memcpy((*(JAVA_ARRAY) arr).data, s->frame, (size_t) s->w * s->h * 4);
        }
        if (outDims != JAVA_NULL && (*(JAVA_ARRAY) outDims).length >= 2) {
            ((JAVA_INT*) (*(JAVA_ARRAY) outDims).data)[0] = s->w;
            ((JAVA_INT*) (*(JAVA_ARRAY) outDims).data)[1] = s->h;
        }
    }
    pthread_mutex_unlock(&s->lock);
    return arr;
}
