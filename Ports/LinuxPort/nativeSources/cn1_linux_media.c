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
 *
 * GStreamer is loaded with dlopen() at first use rather than linked, so the port
 * binary depends only on the GTK3 core at runtime: a desktop without GStreamer
 * installed still runs -- media/audio-recording/camera just report unsupported.
 * The headers are still included (for the types/macros/enums) but no GStreamer
 * symbol is referenced at link time; every entry point is resolved through dlsym
 * (see cn1LoadGst).
 */

#define _GNU_SOURCE
#include "cn1_linux.h"
#include <gst/gst.h>
#include <gst/app/gstappsink.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <pthread.h>

extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim);
extern struct clazz class_array1__JAVA_INT;

/* --- lazily resolved GStreamer entry points (dlopen, not linked) ---------- */
/* core: libgstreamer-1.0.so.0 */
static __typeof__(gst_init)*                       p_gst_init;
static __typeof__(gst_element_factory_make)*       p_gst_element_factory_make;
static __typeof__(gst_element_set_state)*          p_gst_element_set_state;
static __typeof__(gst_element_get_state)*          p_gst_element_get_state;
static __typeof__(gst_element_seek_simple)*        p_gst_element_seek_simple;
static __typeof__(gst_element_query_position)*     p_gst_element_query_position;
static __typeof__(gst_element_query_duration)*     p_gst_element_query_duration;
static __typeof__(gst_element_send_event)*         p_gst_element_send_event;
static __typeof__(gst_event_new_eos)*              p_gst_event_new_eos;
static __typeof__(gst_bus_pop_filtered)*           p_gst_bus_pop_filtered;
static __typeof__(gst_bus_timed_pop_filtered)*     p_gst_bus_timed_pop_filtered;
static __typeof__(gst_message_unref)*              p_gst_message_unref;
static __typeof__(gst_object_unref)*               p_gst_object_unref;
static __typeof__(gst_parse_launch)*               p_gst_parse_launch;
static __typeof__(gst_bin_get_by_name)*            p_gst_bin_get_by_name;
static __typeof__(gst_sample_get_buffer)*          p_gst_sample_get_buffer;
static __typeof__(gst_sample_get_caps)*            p_gst_sample_get_caps;
static __typeof__(gst_sample_unref)*               p_gst_sample_unref;
static __typeof__(gst_caps_get_structure)*         p_gst_caps_get_structure;
static __typeof__(gst_structure_get_int)*          p_gst_structure_get_int;
static __typeof__(gst_buffer_map)*                 p_gst_buffer_map;
static __typeof__(gst_buffer_unmap)*               p_gst_buffer_unmap;
static __typeof__(gst_device_monitor_new)*         p_gst_device_monitor_new;
static __typeof__(gst_device_monitor_add_filter)*  p_gst_device_monitor_add_filter;
static __typeof__(gst_device_monitor_get_devices)* p_gst_device_monitor_get_devices;
static __typeof__(gst_device_get_display_name)*    p_gst_device_get_display_name;
/* app: libgstapp-1.0.so.0 */
static __typeof__(gst_app_sink_try_pull_sample)*   p_gst_app_sink_try_pull_sample;

static int cn1_gst_state = 0; /* 0 = untried, 1 = available, -1 = unavailable */

/* dlopen GStreamer and resolve the entry points once. Returns non-zero when the
 * media/audio/camera peers can be created; 0 (and a one-time log) when GStreamer
 * is not installed, so the rest of the UI keeps working on a GTK3-only system. */
static int cn1LoadGst(void) {
    void* core;
    void* app;
    int ok = 1;
    if (cn1_gst_state) { return cn1_gst_state > 0; }
    core = dlopen("libgstreamer-1.0.so.0", RTLD_LAZY | RTLD_GLOBAL);
    if (!core) { core = dlopen("libgstreamer-1.0.so", RTLD_LAZY | RTLD_GLOBAL); }
    app = dlopen("libgstapp-1.0.so.0", RTLD_LAZY | RTLD_GLOBAL);
    if (!app) { app = dlopen("libgstapp-1.0.so", RTLD_LAZY | RTLD_GLOBAL); }
    if (!core || !app) {
        cn1_gst_state = -1;
        cn1LinuxStubOnce("GStreamer (libgstreamer-1.0) not installed; media unsupported");
        return 0;
    }
#define CN1_GST_SYM(h, ptr, name) do { *(void**)(&ptr) = dlsym(h, name); if (!(ptr)) { ok = 0; } } while (0)
    CN1_GST_SYM(core, p_gst_init, "gst_init");
    CN1_GST_SYM(core, p_gst_element_factory_make, "gst_element_factory_make");
    CN1_GST_SYM(core, p_gst_element_set_state, "gst_element_set_state");
    CN1_GST_SYM(core, p_gst_element_get_state, "gst_element_get_state");
    CN1_GST_SYM(core, p_gst_element_seek_simple, "gst_element_seek_simple");
    CN1_GST_SYM(core, p_gst_element_query_position, "gst_element_query_position");
    CN1_GST_SYM(core, p_gst_element_query_duration, "gst_element_query_duration");
    CN1_GST_SYM(core, p_gst_element_send_event, "gst_element_send_event");
    CN1_GST_SYM(core, p_gst_event_new_eos, "gst_event_new_eos");
    CN1_GST_SYM(core, p_gst_bus_pop_filtered, "gst_bus_pop_filtered");
    CN1_GST_SYM(core, p_gst_bus_timed_pop_filtered, "gst_bus_timed_pop_filtered");
    CN1_GST_SYM(core, p_gst_message_unref, "gst_message_unref");
    CN1_GST_SYM(core, p_gst_object_unref, "gst_object_unref");
    CN1_GST_SYM(core, p_gst_parse_launch, "gst_parse_launch");
    CN1_GST_SYM(core, p_gst_bin_get_by_name, "gst_bin_get_by_name");
    CN1_GST_SYM(core, p_gst_sample_get_buffer, "gst_sample_get_buffer");
    CN1_GST_SYM(core, p_gst_sample_get_caps, "gst_sample_get_caps");
    CN1_GST_SYM(core, p_gst_sample_unref, "gst_sample_unref");
    CN1_GST_SYM(core, p_gst_caps_get_structure, "gst_caps_get_structure");
    CN1_GST_SYM(core, p_gst_structure_get_int, "gst_structure_get_int");
    CN1_GST_SYM(core, p_gst_buffer_map, "gst_buffer_map");
    CN1_GST_SYM(core, p_gst_buffer_unmap, "gst_buffer_unmap");
    CN1_GST_SYM(core, p_gst_device_monitor_new, "gst_device_monitor_new");
    CN1_GST_SYM(core, p_gst_device_monitor_add_filter, "gst_device_monitor_add_filter");
    CN1_GST_SYM(core, p_gst_device_monitor_get_devices, "gst_device_monitor_get_devices");
    CN1_GST_SYM(core, p_gst_device_get_display_name, "gst_device_get_display_name");
    CN1_GST_SYM(app, p_gst_app_sink_try_pull_sample, "gst_app_sink_try_pull_sample");
#undef CN1_GST_SYM
    cn1_gst_state = ok ? 1 : -1;
    if (!ok) { cn1LinuxStubOnce("GStreamer present but an expected symbol was missing; media unsupported"); }
    return ok;
}

static void cn1GstInit(void) {
    static int inited = 0;
    if (!inited) {
        p_gst_init(0, 0);
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
    if (data == JAVA_NULL || length <= 0 || !cn1LoadGst()) {
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
    m->playbin = p_gst_element_factory_make("playbin", 0);
    if (!m->playbin) {
        /* No playbin element (GStreamer base plugins missing): fail cleanly so the
         * Java side returns a null Media rather than us dereferencing NULL below. */
        unlink(tmpl);
        free(m->tempPath);
        free(m);
        return 0;
    }
    uri = g_strconcat("file://", tmpl, NULL);
    g_object_set(m->playbin, "uri", uri, NULL);
    g_free(uri);
    p_gst_element_set_state(m->playbin, GST_STATE_PAUSED);
    return (JAVA_LONG) (intptr_t) m;
}

/* Creates a playbin straight from a URI (http/https/file/...) instead of buffering
 * the bytes into a temp file. This is the path the URL-based MediaManager.createMedia
 * uses: playbin resolves remote sources via souphttpsrc, so streaming works without
 * downloading the whole asset first, and local file:// URIs play in place. */
JAVA_LONG com_codename1_impl_linux_LinuxNative_mediaCreateUri___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT uri) {
    CN1Media* m;
    const char* u;
    if (uri == JAVA_NULL || !cn1LoadGst()) {
        return 0;
    }
    cn1GstInit();
    u = stringToUTF8(threadStateData, uri);
    if (!u || !*u) {
        return 0;
    }
    m = (CN1Media*) calloc(1, sizeof(CN1Media));
    m->tempPath = 0; /* no temp file to clean up */
    m->playbin = p_gst_element_factory_make("playbin", 0);
    if (!m->playbin) {
        free(m);
        return 0;
    }
    g_object_set(m->playbin, "uri", u, NULL);
    p_gst_element_set_state(m->playbin, GST_STATE_PAUSED);
    return (JAVA_LONG) (intptr_t) m;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mediaPlay___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    if (m) {
        p_gst_element_set_state(m->playbin, GST_STATE_PLAYING);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mediaPause___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    if (m) {
        p_gst_element_set_state(m->playbin, GST_STATE_PAUSED);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mediaSetTime___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT millis) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    if (m) {
        p_gst_element_seek_simple(m->playbin, GST_FORMAT_TIME,
                GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_KEY_UNIT,
                (gint64) millis * GST_MSECOND);
    }
}

JAVA_INT com_codename1_impl_linux_LinuxNative_mediaGetTime___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    gint64 pos = 0;
    if (m && p_gst_element_query_position(m->playbin, GST_FORMAT_TIME, &pos)) {
        return (JAVA_INT) (pos / GST_MSECOND);
    }
    return 0;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_mediaGetDuration___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Media* m = (CN1Media*) (intptr_t) peer;
    gint64 dur = 0;
    if (m && p_gst_element_query_duration(m->playbin, GST_FORMAT_TIME, &dur)) {
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
        p_gst_element_get_state(m->playbin, &state, 0, 0);
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
    msg = p_gst_bus_pop_filtered(GST_ELEMENT_BUS(m->playbin), GST_MESSAGE_EOS);
    if (msg) {
        ended = 1;
        p_gst_message_unref(msg);
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
    p_gst_element_set_state(m->playbin, GST_STATE_NULL);
    p_gst_object_unref(m->playbin);
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
    if (!p || !cn1LoadGst()) {
        return 0;
    }
    cn1GstInit();
    desc = g_strdup_printf(
            "autoaudiosrc ! audioconvert ! audioresample ! audio/x-raw,rate=%d,channels=%d ! wavenc ! filesink location=\"%s\"",
            rate, ch, p);
    rec = (CN1AudioRec*) calloc(1, sizeof(CN1AudioRec));
    rec->pipeline = p_gst_parse_launch(desc, &err);
    g_free(desc);
    if (!rec->pipeline) {
        if (err) g_error_free(err);
        free(rec);
        return 0;
    }
    p_gst_element_set_state(rec->pipeline, GST_STATE_PLAYING);
    return (JAVA_LONG) (intptr_t) rec;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_audioRecStop___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1AudioRec* rec = (CN1AudioRec*) (intptr_t) peer;
    if (!rec) {
        return;
    }
    /* Send EOS so wavenc finalizes the header, wait briefly, then tear down. */
    p_gst_element_send_event(rec->pipeline, p_gst_event_new_eos());
    {
        GstMessage* msg = p_gst_bus_timed_pop_filtered(GST_ELEMENT_BUS(rec->pipeline),
                2 * GST_SECOND, GST_MESSAGE_EOS | GST_MESSAGE_ERROR);
        if (msg) {
            p_gst_message_unref(msg);
        }
    }
    p_gst_element_set_state(rec->pipeline, GST_STATE_NULL);
    p_gst_object_unref(rec->pipeline);
    free(rec);
}

/* --------------------------------------------------------------- camera */

/* Pulls one BGRA sample from an appsink into a freshly allocated CN1 ARGB int[]. */
static JAVA_OBJECT cn1CameraSampleToArgb(CODENAME_ONE_THREAD_STATE, GstSample* sample, int* outDims) {
    GstBuffer* buf = p_gst_sample_get_buffer(sample);
    GstCaps* caps = p_gst_sample_get_caps(sample);
    GstStructure* s = p_gst_caps_get_structure(caps, 0);
    int w = 0, h = 0;
    GstMapInfo map;
    JAVA_OBJECT arr;
    p_gst_structure_get_int(s, "width", &w);
    p_gst_structure_get_int(s, "height", &h);
    if (w <= 0 || h <= 0) {
        return JAVA_NULL;
    }
    if (!p_gst_buffer_map(buf, &map, GST_MAP_READ)) {
        return JAVA_NULL;
    }
    arr = allocArray(threadStateData, w * h, &class_array1__JAVA_INT, sizeof(JAVA_INT), 1);
    if (arr != JAVA_NULL && map.size >= (gsize) (w * h * 4)) {
        memcpy((*(JAVA_ARRAY) arr).data, map.data, (size_t) (w * h * 4));
    }
    p_gst_buffer_unmap(buf, &map);
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
    if (!cn1LoadGst()) {
        return JAVA_NULL;
    }
    cn1GstInit();
    pipe = p_gst_parse_launch(
            "v4l2src num-buffers=1 ! videoconvert ! video/x-raw,format=BGRA ! appsink name=s max-buffers=1 drop=true sync=false",
            &err);
    if (!pipe) {
        if (err) g_error_free(err);
        return JAVA_NULL;
    }
    sink = p_gst_bin_get_by_name(GST_BIN(pipe), "s");
    p_gst_element_set_state(pipe, GST_STATE_PLAYING);
    sample = p_gst_app_sink_try_pull_sample(GST_APP_SINK(sink), 5 * GST_SECOND);
    if (sample) {
        result = cn1CameraSampleToArgb(threadStateData, sample, dims);
        p_gst_sample_unref(sample);
    }
    p_gst_element_set_state(pipe, GST_STATE_NULL);
    p_gst_object_unref(sink);
    p_gst_object_unref(pipe);
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
    if (!cn1LoadGst()) {
        JAVA_OBJECT r = newStringFromCString(threadStateData, out->str);
        g_string_free(out, TRUE);
        return r;
    }
    cn1GstInit();
    mon = p_gst_device_monitor_new();
    p_gst_device_monitor_add_filter(mon, "Video/Source", 0);
    devices = p_gst_device_monitor_get_devices(mon);
    for (it = devices; it != 0; it = it->next) {
        GstDevice* dev = (GstDevice*) it->data;
        char* name = p_gst_device_get_display_name(dev);
        g_string_append_printf(out, "%s|false|0|0;", name ? name : "Camera");
        g_free(name);
        p_gst_object_unref(dev);
    }
    g_list_free(devices);
    p_gst_object_unref(mon);
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
        sample = p_gst_app_sink_try_pull_sample(GST_APP_SINK(s->sink), 200 * GST_MSECOND);
        if (!sample) {
            continue;
        }
        {
            GstBuffer* buf = p_gst_sample_get_buffer(sample);
            GstStructure* st = p_gst_caps_get_structure(p_gst_sample_get_caps(sample), 0);
            int w = 0, h = 0;
            GstMapInfo map;
            p_gst_structure_get_int(st, "width", &w);
            p_gst_structure_get_int(st, "height", &h);
            if (w > 0 && h > 0 && p_gst_buffer_map(buf, &map, GST_MAP_READ)) {
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
                p_gst_buffer_unmap(buf, &map);
            }
        }
        p_gst_sample_unref(sample);
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
    if (!cn1LoadGst()) {
        return 0;
    }
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
    s->pipe = p_gst_parse_launch(desc, &err);
    g_free(desc);
    if (!s->pipe) {
        if (err) g_error_free(err);
        free(s);
        return 0;
    }
    s->sink = p_gst_bin_get_by_name(GST_BIN(s->pipe), "s");
    p_gst_element_set_state(s->pipe, GST_STATE_PLAYING);
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
    p_gst_element_set_state(s->pipe, GST_STATE_NULL);
    p_gst_object_unref(s->sink);
    p_gst_object_unref(s->pipe);
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
