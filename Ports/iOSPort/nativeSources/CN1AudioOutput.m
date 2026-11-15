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
 */

/*
 * Real-time PCM output for com.codename1.media.audio.AudioContext
 * (IOSImplementation.IOSAudioOutput, a PushAudioOutput).
 *
 * The Java render thread pushes interleaved float frames into a single
 * producer / single consumer ring with audioOutWrite, which never blocks; an
 * AVAudioSourceNode's render block -- the device's real-time thread -- drains
 * it and writes silence on an underrun. Nothing here calls into Java and no
 * native method waits, so the translated thread is never parked in C.
 *
 * Shared with the native macOS port (AVAudioEngine is the same API there).
 * AVAudioSourceNode needs iOS 13 / macOS 10.15 / tvOS 13; below that, and on
 * the watch, audioOutOpen answers 0 and AudioContext.isSupported() is false.
 */
#include "TargetConditionals.h"
#import <Foundation/Foundation.h>
#import "xmlvm.h"
#include <stdlib.h>
#include <string.h>

#if !TARGET_OS_WATCH
#import <AVFoundation/AVFoundation.h>
#endif

/* Ring capacity in frames: what Java may queue ahead of the device. It is part
 * of the output latency and reported in audioOutLatency. */
#define CN1_AUDIO_OUT_RING_FRAMES 1024

typedef struct {
    float* ring;
    int channels;
    int capacity;            /* frames */
    long long writePos;      /* frames written, producer only stores */
    long long readPos;       /* frames read, consumer only stores */
    void* engine;            /* AVAudioEngine, retained */
    void* node;              /* AVAudioSourceNode, retained */
    double rate;
} CN1AudioOut;

static long long cn1AoLoad(long long* p) {
    return __atomic_load_n(p, __ATOMIC_ACQUIRE);
}

static void cn1AoStore(long long* p, long long v) {
    __atomic_store_n(p, v, __ATOMIC_RELEASE);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_audioOutOpen___int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT sampleRate, JAVA_INT channels) {
#if TARGET_OS_WATCH
    return 0;
#else
    if (@available(iOS 13.0, macOS 10.15, tvOS 13.0, *)) {
        JAVA_LONG result = 0;
        @autoreleasepool {
        int ch = channels == 1 ? 1 : 2;
#if TARGET_OS_IOS || TARGET_OS_TV
        [[AVAudioSession sharedInstance] setActive:YES error:nil];
#endif
        AVAudioEngine* engine = [[AVAudioEngine alloc] init];
        double hw = [engine.outputNode outputFormatForBus:0].sampleRate;
        double rate = sampleRate > 0 ? sampleRate : (hw > 0 ? hw : 48000);
        AVAudioFormat* fmt = [[AVAudioFormat alloc] initStandardFormatWithSampleRate:rate channels:(AVAudioChannelCount)ch];
        CN1AudioOut* o = (CN1AudioOut*)calloc(1, sizeof(CN1AudioOut));
        o->channels = ch;
        o->capacity = CN1_AUDIO_OUT_RING_FRAMES;
        o->ring = (float*)calloc((size_t)(o->capacity * ch), sizeof(float));
        o->rate = rate;
        CN1AudioOut* ao = o;
        AVAudioSourceNode* node = [[AVAudioSourceNode alloc] initWithFormat:fmt renderBlock:^OSStatus(BOOL* isSilence, const AudioTimeStamp* ts, AVAudioFrameCount frameCount, AudioBufferList* out) {
            long long r = ao->readPos;
            long long avail = cn1AoLoad(&ao->writePos) - r;
            int n = (int)(avail < (long long)frameCount ? avail : (long long)frameCount);
            int c;
            for (c = 0; c < (int)out->mNumberBuffers && c < ao->channels; c++) {
                float* d = (float*)out->mBuffers[c].mData;
                int i;
                for (i = 0; i < n; i++) {
                    d[i] = ao->ring[(int)((r + i) % ao->capacity) * ao->channels + c];
                }
                for (; i < (int)frameCount; i++) {
                    d[i] = 0;
                }
            }
            cn1AoStore(&ao->readPos, r + n);
            return noErr;
        }];
        [engine attachNode:node];
        /* connect:to:format:error: replaces this from the 27 SDKs on, and does
         * not exist on the systems this still has to run on. */
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
        [engine connect:node to:engine.mainMixerNode format:fmt];
#pragma clang diagnostic pop
        [engine prepare];
        o->engine = (void*)CFRetain((__bridge CFTypeRef)engine);
        o->node = (void*)CFRetain((__bridge CFTypeRef)node);
#ifndef CN1_USE_ARC
        [engine release];
        [node release];
        [fmt release];
#endif
        result = (JAVA_LONG)(intptr_t)o;
        }
        return result;
    }
    return 0;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_audioOutRate___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*)(intptr_t)handle;
    return o == NULL ? 0 : (JAVA_INT)o->rate;
}

JAVA_INT com_codename1_impl_ios_IOSNative_audioOutWrite___long_float_1ARRAY_int_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle, JAVA_OBJECT pcm, JAVA_INT offset, JAVA_INT frames) {
    CN1AudioOut* o = (CN1AudioOut*)(intptr_t)handle;
    if (o == NULL || pcm == JAVA_NULL || frames <= 0) {
        return 0;
    }
    JAVA_ARRAY_FLOAT* src = (JAVA_ARRAY_FLOAT*)CN1_ARRAY_DATA((JAVA_ARRAY)pcm);
    long long w = o->writePos;
    long long space = o->capacity - (w - cn1AoLoad(&o->readPos));
    int n = (int)(space < frames ? space : frames);
    int i;
    int ch = o->channels;
    for (i = 0; i < n; i++) {
        memcpy(o->ring + (int)((w + i) % o->capacity) * ch, src + (offset + i) * ch, sizeof(float) * ch);
    }
    cn1AoStore(&o->writePos, w + n);
    return n;
}

JAVA_VOID com_codename1_impl_ios_IOSNative_audioOutStart___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle) {
#if !TARGET_OS_WATCH
    CN1AudioOut* o = (CN1AudioOut*)(intptr_t)handle;
    if (o != NULL) {
        @autoreleasepool {
        [(__bridge AVAudioEngine*)o->engine startAndReturnError:nil];
        }
    }
#endif
}

JAVA_VOID com_codename1_impl_ios_IOSNative_audioOutStop___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle) {
#if !TARGET_OS_WATCH
    CN1AudioOut* o = (CN1AudioOut*)(intptr_t)handle;
    if (o != NULL) {
        @autoreleasepool {
        [(__bridge AVAudioEngine*)o->engine pause];
        }
    }
#endif
}

JAVA_VOID com_codename1_impl_ios_IOSNative_audioOutClose___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle) {
#if !TARGET_OS_WATCH
    CN1AudioOut* o = (CN1AudioOut*)(intptr_t)handle;
    if (o != NULL) {
        @autoreleasepool {
        AVAudioEngine* engine = (__bridge AVAudioEngine*)o->engine;
        [engine stop];
        [engine detachNode:(__bridge AVAudioNode*)o->node];
        CFRelease((CFTypeRef)o->node);
        CFRelease((CFTypeRef)o->engine);
        }
        free(o->ring);
        free(o);
    }
#endif
}

JAVA_DOUBLE com_codename1_impl_ios_IOSNative_audioOutLatency___long_R_double(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_LONG handle) {
    CN1AudioOut* o = (CN1AudioOut*)(intptr_t)handle;
    if (o == NULL) {
        return 0;
    }
    double device = 0;
#if !TARGET_OS_WATCH
    device = ((__bridge AVAudioEngine*)o->engine).outputNode.presentationLatency;
#if TARGET_OS_IOS || TARGET_OS_TV
    device += [AVAudioSession sharedInstance].IOBufferDuration;
#endif
#endif
    return device + o->capacity / o->rate;
}
