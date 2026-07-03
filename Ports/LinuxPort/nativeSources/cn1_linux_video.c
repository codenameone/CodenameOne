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
 * Native backend for com.codename1.media.VideoIO on Linux using GStreamer:
 * filesrc ! decodebin ! videoconvert ! appsink for frame accurate decode
 * (ACCURATE seek), appsrc ! videoconvert ! x264enc ! mp4mux ! filesink for
 * encode. GStreamer is dlopen()ed (not linked), matching cn1_linux_media.c.
 */
#include "cn1_linux.h"
#include <gst/gst.h>
#include <gst/app/gstappsink.h>
#include <gst/app/gstappsrc.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <unistd.h>

extern struct clazz class_array1__JAVA_BYTE;

/* --- lazily resolved GStreamer entry points (dlopen, not linked) ---------- */
static __typeof__(gst_init)*                       p_gst_init;
static __typeof__(gst_parse_launch)*               p_gst_parse_launch;
static __typeof__(gst_element_set_state)*          p_gst_element_set_state;
static __typeof__(gst_element_get_state)*          p_gst_element_get_state;
static __typeof__(gst_element_seek_simple)*        p_gst_element_seek_simple;
static __typeof__(gst_element_query_duration)*     p_gst_element_query_duration;
static __typeof__(gst_element_get_bus)*            p_gst_element_get_bus;
static __typeof__(gst_bin_get_by_name)*            p_gst_bin_get_by_name;
static __typeof__(gst_object_unref)*               p_gst_object_unref;
static __typeof__(gst_bus_timed_pop_filtered)*     p_gst_bus_timed_pop_filtered;
static __typeof__(gst_message_unref)*              p_gst_message_unref;
static __typeof__(gst_sample_get_buffer)*          p_gst_sample_get_buffer;
static __typeof__(gst_sample_get_caps)*            p_gst_sample_get_caps;
static __typeof__(gst_sample_unref)*               p_gst_sample_unref;
static __typeof__(gst_caps_get_structure)*         p_gst_caps_get_structure;
static __typeof__(gst_structure_get_int)*          p_gst_structure_get_int;
static __typeof__(gst_structure_get_fraction)*     p_gst_structure_get_fraction;
static __typeof__(gst_buffer_map)*                 p_gst_buffer_map;
static __typeof__(gst_buffer_unmap)*               p_gst_buffer_unmap;
static __typeof__(gst_buffer_new_allocate)*        p_gst_buffer_new_allocate;
/* app: libgstapp-1.0.so.0 */
static __typeof__(gst_app_sink_try_pull_sample)*   p_gst_app_sink_try_pull_sample;
static __typeof__(gst_app_src_push_buffer)*        p_gst_app_src_push_buffer;
static __typeof__(gst_app_src_end_of_stream)*      p_gst_app_src_end_of_stream;

static int cn1_gstv_state = 0; /* 0 = untried, 1 = available, -1 = unavailable */

static int cn1LoadGstVideo(void) {
    void* core;
    void* app;
    int ok = 1;
    if (cn1_gstv_state) { return cn1_gstv_state > 0; }
    core = dlopen("libgstreamer-1.0.so.0", RTLD_LAZY | RTLD_GLOBAL);
    if (!core) { core = dlopen("libgstreamer-1.0.so", RTLD_LAZY | RTLD_GLOBAL); }
    app = dlopen("libgstapp-1.0.so.0", RTLD_LAZY | RTLD_GLOBAL);
    if (!app) { app = dlopen("libgstapp-1.0.so", RTLD_LAZY | RTLD_GLOBAL); }
    if (!core || !app) {
        cn1_gstv_state = -1;
        return 0;
    }
#define CN1_GST_SYM(h, ptr, name) do { *(void**)(&ptr) = dlsym(h, name); if (!(ptr)) { ok = 0; } } while (0)
    CN1_GST_SYM(core, p_gst_init, "gst_init");
    CN1_GST_SYM(core, p_gst_parse_launch, "gst_parse_launch");
    CN1_GST_SYM(core, p_gst_element_set_state, "gst_element_set_state");
    CN1_GST_SYM(core, p_gst_element_get_state, "gst_element_get_state");
    CN1_GST_SYM(core, p_gst_element_seek_simple, "gst_element_seek_simple");
    CN1_GST_SYM(core, p_gst_element_query_duration, "gst_element_query_duration");
    CN1_GST_SYM(core, p_gst_element_get_bus, "gst_element_get_bus");
    CN1_GST_SYM(core, p_gst_bin_get_by_name, "gst_bin_get_by_name");
    CN1_GST_SYM(core, p_gst_object_unref, "gst_object_unref");
    CN1_GST_SYM(core, p_gst_bus_timed_pop_filtered, "gst_bus_timed_pop_filtered");
    CN1_GST_SYM(core, p_gst_message_unref, "gst_message_unref");
    CN1_GST_SYM(core, p_gst_sample_get_buffer, "gst_sample_get_buffer");
    CN1_GST_SYM(core, p_gst_sample_get_caps, "gst_sample_get_caps");
    CN1_GST_SYM(core, p_gst_sample_unref, "gst_sample_unref");
    CN1_GST_SYM(core, p_gst_caps_get_structure, "gst_caps_get_structure");
    CN1_GST_SYM(core, p_gst_structure_get_int, "gst_structure_get_int");
    CN1_GST_SYM(core, p_gst_structure_get_fraction, "gst_structure_get_fraction");
    CN1_GST_SYM(core, p_gst_buffer_map, "gst_buffer_map");
    CN1_GST_SYM(core, p_gst_buffer_unmap, "gst_buffer_unmap");
    CN1_GST_SYM(core, p_gst_buffer_new_allocate, "gst_buffer_new_allocate");
    CN1_GST_SYM(app, p_gst_app_sink_try_pull_sample, "gst_app_sink_try_pull_sample");
    CN1_GST_SYM(app, p_gst_app_src_push_buffer, "gst_app_src_push_buffer");
    CN1_GST_SYM(app, p_gst_app_src_end_of_stream, "gst_app_src_end_of_stream");
#undef CN1_GST_SYM
    if (ok) {
        p_gst_init(0, 0);
    }
    cn1_gstv_state = ok ? 1 : -1;
    return ok;
}

static void cn1StripFile(const char* in, char* out, size_t n) {
    if (in == NULL) { out[0] = '\0'; return; }
    if (strncmp(in, "file://", 7) == 0) { snprintf(out, n, "%s", in + 7); }
    else if (strncmp(in, "file:", 5) == 0) { snprintf(out, n, "%s", in + 5); }
    else { snprintf(out, n, "%s", in); }
}

/* ------------------------------------------------------------------ reader */
typedef struct {
    GstElement* pipeline;
    GstElement* vsink;
    int width;
    int height;
    long durationMs;
    float frameRate;
    int hasVideo;
    int hasAudio;
    int audioRate;
    int audioChannels;
    char path[2048];
} CN1VideoReader;

/* Pulls one decoded RGBA sample's metadata into the reader, leaving the pipeline paused. */
static void cn1ReaderProbe(CN1VideoReader* r) {
    GstState st = GST_STATE_NULL;
    p_gst_element_set_state(r->pipeline, GST_STATE_PAUSED);
    p_gst_element_get_state(r->pipeline, &st, NULL, 5 * GST_SECOND);
    GstSample* sample = p_gst_app_sink_try_pull_sample((GstAppSink*) r->vsink, 3 * GST_SECOND);
    if (sample) {
        GstCaps* caps = p_gst_sample_get_caps(sample);
        if (caps) {
            GstStructure* s = p_gst_caps_get_structure(caps, 0);
            p_gst_structure_get_int(s, "width", &r->width);
            p_gst_structure_get_int(s, "height", &r->height);
            int num = 0, den = 0;
            if (p_gst_structure_get_fraction(s, "framerate", &num, &den) && den != 0) {
                r->frameRate = (float) num / (float) den;
            }
            r->hasVideo = (r->width > 0 && r->height > 0);
        }
        p_gst_sample_unref(sample);
    }
    gint64 dur = 0;
    if (p_gst_element_query_duration(r->pipeline, GST_FORMAT_TIME, &dur) && dur > 0) {
        r->durationMs = (long) (dur / GST_MSECOND);
    }
}

/* A throwaway audio pipeline used only to detect audio + sample rate/channels. */
static void cn1ReaderProbeAudio(CN1VideoReader* r) {
    char desc[4096];
    snprintf(desc, sizeof(desc),
            "filesrc location=\"%s\" ! decodebin ! audioconvert ! audioresample ! "
            "audio/x-raw,format=S16LE ! appsink name=a sync=false", r->path);
    GError* err = NULL;
    GstElement* pipe = p_gst_parse_launch(desc, &err);
    if (!pipe) { return; }
    GstElement* sink = p_gst_bin_get_by_name(GST_BIN(pipe), "a");
    GstState st = GST_STATE_NULL;
    p_gst_element_set_state(pipe, GST_STATE_PAUSED);
    p_gst_element_get_state(pipe, &st, NULL, 3 * GST_SECOND);
    if (sink) {
        GstSample* sample = p_gst_app_sink_try_pull_sample((GstAppSink*) sink, 2 * GST_SECOND);
        if (sample) {
            GstCaps* caps = p_gst_sample_get_caps(sample);
            if (caps) {
                GstStructure* s = p_gst_caps_get_structure(caps, 0);
                r->hasAudio = 1;
                p_gst_structure_get_int(s, "rate", &r->audioRate);
                p_gst_structure_get_int(s, "channels", &r->audioChannels);
            }
            p_gst_sample_unref(sample);
        }
        p_gst_object_unref(sink);
    }
    p_gst_element_set_state(pipe, GST_STATE_NULL);
    p_gst_object_unref(pipe);
}

static JAVA_OBJECT cn1ReaderFrameAt(CODENAME_ONE_THREAD_STATE, CN1VideoReader* r, long ms) {
    if (!r->hasVideo) { return JAVA_NULL; }
    p_gst_element_seek_simple(r->pipeline, GST_FORMAT_TIME,
            (GstSeekFlags) (GST_SEEK_FLAG_FLUSH | GST_SEEK_FLAG_ACCURATE),
            (gint64) ms * GST_MSECOND);
    GstSample* sample = p_gst_app_sink_try_pull_sample((GstAppSink*) r->vsink, 3 * GST_SECOND);
    if (!sample) { return JAVA_NULL; }
    JAVA_OBJECT result = JAVA_NULL;
    GstBuffer* buf = p_gst_sample_get_buffer(sample);
    GstMapInfo map;
    if (buf && p_gst_buffer_map(buf, &map, GST_MAP_READ)) {
        int needed = r->width * r->height * 4;
        if ((int) map.size >= needed) {
            result = allocArray(threadStateData, needed, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
            if (result != JAVA_NULL) {
                memcpy((*(JAVA_ARRAY) result).data, map.data, needed);
            }
        }
        p_gst_buffer_unmap(buf, &map);
    }
    p_gst_sample_unref(sample);
    return result;
}

static JAVA_OBJECT cn1ReaderReadAudio(CODENAME_ONE_THREAD_STATE, CN1VideoReader* r) {
    if (!r->hasAudio) { return JAVA_NULL; }
    int ch = r->audioChannels > 0 ? r->audioChannels : 2;
    int rate = r->audioRate > 0 ? r->audioRate : 44100;
    char desc[4096];
    snprintf(desc, sizeof(desc),
            "filesrc location=\"%s\" ! decodebin ! audioconvert ! audioresample ! "
            "audio/x-raw,format=S16LE,channels=%d,rate=%d ! appsink name=a sync=false",
            r->path, ch, rate);
    GError* err = NULL;
    GstElement* pipe = p_gst_parse_launch(desc, &err);
    if (!pipe) { return JAVA_NULL; }
    GstElement* sink = p_gst_bin_get_by_name(GST_BIN(pipe), "a");
    p_gst_element_set_state(pipe, GST_STATE_PLAYING);

    unsigned char* pcm = NULL;
    size_t pcmLen = 0;
    if (sink) {
        for (;;) {
            GstSample* sample = p_gst_app_sink_try_pull_sample((GstAppSink*) sink, 2 * GST_SECOND);
            if (!sample) { break; }
            GstBuffer* buf = p_gst_sample_get_buffer(sample);
            GstMapInfo map;
            if (buf && p_gst_buffer_map(buf, &map, GST_MAP_READ)) {
                unsigned char* grown = (unsigned char*) realloc(pcm, pcmLen + map.size);
                if (grown) {
                    pcm = grown;
                    memcpy(pcm + pcmLen, map.data, map.size);
                    pcmLen += map.size;
                }
                p_gst_buffer_unmap(buf, &map);
            }
            p_gst_sample_unref(sample);
        }
        p_gst_object_unref(sink);
    }
    p_gst_element_set_state(pipe, GST_STATE_NULL);
    p_gst_object_unref(pipe);

    JAVA_OBJECT result = JAVA_NULL;
    if (pcm && pcmLen > 0) {
        result = allocArray(threadStateData, (int) pcmLen, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
        if (result != JAVA_NULL) {
            memcpy((*(JAVA_ARRAY) result).data, pcm, pcmLen);
        }
    }
    free(pcm);
    return result;
}

/* ------------------------------------------------------------------ writer */
typedef struct {
    GstElement* pipeline;
    GstElement* vsrc;
    GstElement* asrc;
    int width;
    int height;
    float frameRate;
    int hasAudio;
} CN1VideoWriter;

static void cn1WriterPush(GstElement* src, const unsigned char* bytes, int len, gint64 ptsNs, gint64 durNs) {
    GstBuffer* buf = p_gst_buffer_new_allocate(NULL, (gsize) len, NULL);
    if (!buf) { return; }
    GstMapInfo map;
    if (p_gst_buffer_map(buf, &map, GST_MAP_WRITE)) {
        memcpy(map.data, bytes, (size_t) len);
        p_gst_buffer_unmap(buf, &map);
    }
    GST_BUFFER_PTS(buf) = (GstClockTime) ptsNs;
    GST_BUFFER_DURATION(buf) = (GstClockTime) durNs;
    p_gst_app_src_push_buffer((GstAppSrc*) src, buf); /* takes ownership */
}

/* ------------------------------------------------------------------ exports */
#define RD(peer) ((CN1VideoReader*)(intptr_t)(peer))
#define WR(peer) ((CN1VideoWriter*)(intptr_t)(peer))

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_videoBackendAvailable___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return cn1LoadGstVideo() ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_videoReaderOpen___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    if (!cn1LoadGstVideo()) { return 0; }
    char path[2048];
    cn1StripFile(stringToUTF8(threadStateData, pathObj), path, sizeof(path));
    char desc[4096];
    snprintf(desc, sizeof(desc),
            "filesrc location=\"%s\" ! decodebin ! videoconvert ! "
            "video/x-raw,format=RGBA ! appsink name=v sync=false max-buffers=2", path);
    GError* err = NULL;
    GstElement* pipe = p_gst_parse_launch(desc, &err);
    if (!pipe) { return 0; }
    CN1VideoReader* r = (CN1VideoReader*) calloc(1, sizeof(CN1VideoReader));
    r->pipeline = pipe;
    r->vsink = p_gst_bin_get_by_name(GST_BIN(pipe), "v");
    r->durationMs = -1;
    r->frameRate = 30.0f;
    r->audioRate = -1;
    r->audioChannels = -1;
    snprintf(r->path, sizeof(r->path), "%s", path);
    cn1ReaderProbe(r);
    cn1ReaderProbeAudio(r);
    return (JAVA_LONG) (intptr_t) r;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_videoReaderWidth___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) { return RD(peer)->width; }
JAVA_INT com_codename1_impl_linux_LinuxNative_videoReaderHeight___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) { return RD(peer)->height; }
JAVA_LONG com_codename1_impl_linux_LinuxNative_videoReaderDuration___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) { return (JAVA_LONG) RD(peer)->durationMs; }
JAVA_FLOAT com_codename1_impl_linux_LinuxNative_videoReaderFrameRate___long_R_float(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) { return RD(peer)->frameRate; }
JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_videoReaderHasVideo___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) { return RD(peer)->hasVideo ? JAVA_TRUE : JAVA_FALSE; }
JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_videoReaderHasAudio___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) { return RD(peer)->hasAudio ? JAVA_TRUE : JAVA_FALSE; }
JAVA_INT com_codename1_impl_linux_LinuxNative_videoReaderAudioSampleRate___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) { return RD(peer)->audioRate; }
JAVA_INT com_codename1_impl_linux_LinuxNative_videoReaderAudioChannels___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) { return RD(peer)->audioChannels; }

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_videoReaderFrameAt___long_long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_LONG ms) {
    return cn1ReaderFrameAt(threadStateData, RD(peer), (long) ms);
}
JAVA_OBJECT com_codename1_impl_linux_LinuxNative_videoReaderReadAudio___long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    return cn1ReaderReadAudio(threadStateData, RD(peer));
}
JAVA_VOID com_codename1_impl_linux_LinuxNative_videoReaderClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1VideoReader* r = RD(peer);
    if (!r) { return; }
    if (r->vsink) { p_gst_object_unref(r->vsink); }
    if (r->pipeline) {
        p_gst_element_set_state(r->pipeline, GST_STATE_NULL);
        p_gst_object_unref(r->pipeline);
    }
    free(r);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_videoWriterOpen___java_lang_String_boolean_int_int_float_int_int_boolean_int_int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj, JAVA_BOOLEAN hevc, JAVA_INT width, JAVA_INT height, JAVA_FLOAT fps,
        JAVA_INT videoBitRate, JAVA_INT gop, JAVA_BOOLEAN hasAudio, JAVA_INT audioBitRate, JAVA_INT sampleRate, JAVA_INT channels) {
    if (!cn1LoadGstVideo()) { return 0; }
    char out[2048];
    cn1StripFile(stringToUTF8(threadStateData, pathObj), out, sizeof(out));
    int fpsNum = (int) (fps <= 0 ? 30 : (fps + 0.5f));
    const char* venc = (hevc != JAVA_FALSE) ? "x265enc" : "x264enc";
    const char* vparse = (hevc != JAVA_FALSE) ? "h265parse" : "h264parse";
    char desc[8192];
    int pos = snprintf(desc, sizeof(desc),
            "appsrc name=vsrc format=time is-live=false do-timestamp=false "
            "caps=video/x-raw,format=RGBA,width=%d,height=%d,framerate=%d/1 ! "
            "videoconvert ! %s bitrate=%d key-int-max=%d ! %s ! mp4mux name=mux ! filesink location=\"%s\"",
            width, height, fpsNum, venc, videoBitRate / 1000, gop, vparse, out);
    if (hasAudio != JAVA_FALSE) {
        snprintf(desc + pos, sizeof(desc) - pos,
                " appsrc name=asrc format=time caps=audio/x-raw,format=S16LE,rate=%d,channels=%d,layout=interleaved ! "
                "audioconvert ! avenc_aac bitrate=%d ! aacparse ! mux.",
                sampleRate, channels, audioBitRate);
    }
    GError* err = NULL;
    GstElement* pipe = p_gst_parse_launch(desc, &err);
    if (!pipe) { return 0; }
    CN1VideoWriter* w = (CN1VideoWriter*) calloc(1, sizeof(CN1VideoWriter));
    w->pipeline = pipe;
    w->vsrc = p_gst_bin_get_by_name(GST_BIN(pipe), "vsrc");
    w->asrc = (hasAudio != JAVA_FALSE) ? p_gst_bin_get_by_name(GST_BIN(pipe), "asrc") : NULL;
    w->width = width;
    w->height = height;
    w->frameRate = fps;
    w->hasAudio = (hasAudio != JAVA_FALSE) && (w->asrc != NULL);
    if (!w->vsrc || p_gst_element_set_state(pipe, GST_STATE_PLAYING) == GST_STATE_CHANGE_FAILURE) {
        if (w->vsrc) { p_gst_object_unref(w->vsrc); }
        if (w->asrc) { p_gst_object_unref(w->asrc); }
        p_gst_element_set_state(pipe, GST_STATE_NULL);
        p_gst_object_unref(pipe);
        free(w);
        return 0;
    }
    return (JAVA_LONG) (intptr_t) w;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_videoWriterFrame___long_byte_1ARRAY_int_int_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT rgba, JAVA_INT w, JAVA_INT h, JAVA_LONG ptsMs) {
    CN1VideoWriter* st = WR(peer);
    if (!st || !st->vsrc || rgba == JAVA_NULL) { return; }
    int len = (*(JAVA_ARRAY) rgba).length;
    gint64 dur = st->frameRate > 0 ? (gint64) (GST_SECOND / st->frameRate) : (GST_SECOND / 30);
    cn1WriterPush(st->vsrc, (const unsigned char*) (*(JAVA_ARRAY) rgba).data, len, (gint64) ptsMs * GST_MSECOND, dur);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_videoWriterAudio___long_byte_1ARRAY_int_int_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT pcm, JAVA_INT sampleRate, JAVA_INT channels, JAVA_LONG ptsMs) {
    CN1VideoWriter* st = WR(peer);
    if (!st || !st->hasAudio || !st->asrc || pcm == JAVA_NULL) { return; }
    int len = (*(JAVA_ARRAY) pcm).length;
    int frames = len / (2 * (channels > 0 ? channels : 1));
    gint64 dur = sampleRate > 0 ? (gint64) ((gint64) frames * GST_SECOND / sampleRate) : 0;
    cn1WriterPush(st->asrc, (const unsigned char*) (*(JAVA_ARRAY) pcm).data, len, (gint64) ptsMs * GST_MSECOND, dur);
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_videoWriterClose___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1VideoWriter* st = WR(peer);
    if (!st) { return JAVA_FALSE; }
    if (st->vsrc) { p_gst_app_src_end_of_stream((GstAppSrc*) st->vsrc); }
    if (st->asrc) { p_gst_app_src_end_of_stream((GstAppSrc*) st->asrc); }
    int ok = 0;
    GstBus* bus = p_gst_element_get_bus(st->pipeline);
    if (bus) {
        GstMessage* msg = p_gst_bus_timed_pop_filtered(bus, 30 * GST_SECOND,
                (GstMessageType) (GST_MESSAGE_EOS | GST_MESSAGE_ERROR));
        if (msg) {
            ok = (GST_MESSAGE_TYPE(msg) == GST_MESSAGE_EOS);
            p_gst_message_unref(msg);
        }
        p_gst_object_unref(bus);
    }
    if (st->vsrc) { p_gst_object_unref(st->vsrc); }
    if (st->asrc) { p_gst_object_unref(st->asrc); }
    p_gst_element_set_state(st->pipeline, GST_STATE_NULL);
    p_gst_object_unref(st->pipeline);
    free(st);
    return ok ? JAVA_TRUE : JAVA_FALSE;
}
