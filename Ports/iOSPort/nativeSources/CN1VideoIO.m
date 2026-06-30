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
 * Native AVFoundation backend for com.codename1.media.VideoIO. Provides frame
 * accurate decoding (AVAssetImageGenerator with zero tolerance) + PCM audio
 * extraction (AVAssetReader) and encoding (AVAssetWriter +
 * AVAssetWriterInputPixelBufferAdaptor) used by IOSImplementation / IOSNative.
 */
#include "xmlvm.h"
#include "java_lang_String.h"
#import "CodenameOne_GLViewController.h"

// watchOS lacks the AVFoundation video classes (AVAssetReader / AVAssetWriter /
// AVAssetImageGenerator and the AVVideoCodecType* constants are all
// API_UNAVAILABLE(watchos)), and VideoIO is not supported on the Watch target
// (IOSImplementation.getVideoIO() returns null there). Build the real backend on
// every other target; on watchOS emit stub entry points below so the shared
// translated IOSNative bytecode still links.
#if !TARGET_OS_WATCH
#import <AVFoundation/AVFoundation.h>
#import <CoreMedia/CoreMedia.h>
#import <CoreVideo/CoreVideo.h>

// toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT) is declared in cn1_globals.h
// (pulled in via xmlvm.h); do not redeclare it here.

// --------------------------------------------------------------------------
// Reader / writer state objects
// --------------------------------------------------------------------------
@interface CN1VideoReaderState : NSObject {
@public
    AVAsset* asset;
    AVAssetTrack* videoTrack;
    AVAssetTrack* audioTrack;
    AVAssetImageGenerator* generator;
    int displayWidth;
    int displayHeight;
}
@end
@implementation CN1VideoReaderState
@end

@interface CN1VideoWriterState : NSObject {
@public
    AVAssetWriter* writer;
    AVAssetWriterInput* videoInput;
    AVAssetWriterInputPixelBufferAdaptor* adaptor;
    AVAssetWriterInput* audioInput;
    BOOL hasVideo;
    BOOL hasAudio;
    int width;
    int height;
}
@end
@implementation CN1VideoWriterState
@end

// --------------------------------------------------------------------------
// Helpers
// --------------------------------------------------------------------------
static NSString* cn1StripFilePrefix(NSString* p) {
    if ([p hasPrefix:@"file://"]) {
        return [p substringFromIndex:7];
    }
    if ([p hasPrefix:@"file:"]) {
        return [p substringFromIndex:5];
    }
    return p;
}

static JAVA_LONG cn1RetainAsPeer(id obj) {
#ifndef CN1_USE_ARC
    [obj retain];
#endif
    return (JAVA_LONG)(void*)obj;
}

static NSData* cn1RetainData(NSData* d) {
#ifndef CN1_USE_ARC
    [d retain];
#endif
    return d;
}

// ---- Reader ----------------------------------------------------------------
static JAVA_LONG cn1VideoReaderOpen(NSString* path) {
    POOL_BEGIN();
    NSURL* url = [NSURL fileURLWithPath:cn1StripFilePrefix(path)];
    AVURLAsset* asset = [AVURLAsset URLAssetWithURL:url options:nil];
    if (asset == nil) {
        POOL_END();
        return 0;
    }
    CN1VideoReaderState* st = [[CN1VideoReaderState alloc] init];
    st->asset = asset;
    NSArray* vtracks = [asset tracksWithMediaType:AVMediaTypeVideo];
    NSArray* atracks = [asset tracksWithMediaType:AVMediaTypeAudio];
    if ([vtracks count] > 0) {
        st->videoTrack = [vtracks objectAtIndex:0];
        CGSize n = st->videoTrack.naturalSize;
        CGSize d = CGSizeApplyAffineTransform(n, st->videoTrack.preferredTransform);
        st->displayWidth = (int)round(fabs(d.width));
        st->displayHeight = (int)round(fabs(d.height));
        st->generator = [[AVAssetImageGenerator alloc] initWithAsset:asset];
        st->generator.appliesPreferredTrackTransform = YES;
        st->generator.requestedTimeToleranceBefore = kCMTimeZero;
        st->generator.requestedTimeToleranceAfter = kCMTimeZero;
    }
    if ([atracks count] > 0) {
        st->audioTrack = [atracks objectAtIndex:0];
    }
#ifndef CN1_USE_ARC
    [st->asset retain];
    [st->videoTrack retain];
    [st->audioTrack retain];
    [st->generator retain];
#endif
    JAVA_LONG peer = (JAVA_LONG)(void*)st;
    POOL_END();
    return peer;
}

static const AudioStreamBasicDescription* cn1AudioASBD(CN1VideoReaderState* st) {
    if (st->audioTrack == nil || [st->audioTrack.formatDescriptions count] == 0) {
        return NULL;
    }
    CMAudioFormatDescriptionRef fmt = (__bridge CMAudioFormatDescriptionRef)[st->audioTrack.formatDescriptions objectAtIndex:0];
    return CMAudioFormatDescriptionGetStreamBasicDescription(fmt);
}

static JAVA_LONG cn1FrameAt(CN1VideoReaderState* st, JAVA_LONG ms) {
    if (st->generator == nil) {
        return 0;
    }
    POOL_BEGIN();
    CMTime t = CMTimeMake(ms, 1000);
    NSError* err = nil;
    CGImageRef img = [st->generator copyCGImageAtTime:t actualTime:NULL error:&err];
    if (img == NULL) {
        POOL_END();
        return 0;
    }
    int w = st->displayWidth;
    int h = st->displayHeight;
    size_t stride = (size_t)w * 4;
    void* buffer = malloc(stride * h);
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    CGContextRef ctx = CGBitmapContextCreate(buffer, w, h, 8, stride, cs,
            kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    // Draw straight: a raw CGBitmapContext already stores row 0 == top of the
    // image, so CGContextDrawImage yields top-down RGBA. (An extra CTM flip here
    // mirrored the frames vertically -- decoded video came out upside down.)
    CGContextDrawImage(ctx, CGRectMake(0, 0, w, h), img);
    CGContextRelease(ctx);
    CGColorSpaceRelease(cs);
    CGImageRelease(img);
    NSData* data = cn1RetainData([NSData dataWithBytes:buffer length:stride * h]);
    free(buffer);
    JAVA_LONG peer = (JAVA_LONG)(void*)data;
    POOL_END();
    return peer;
}

static JAVA_LONG cn1ReadAudio(CN1VideoReaderState* st) {
    if (st->audioTrack == nil) {
        return 0;
    }
    POOL_BEGIN();
    NSError* err = nil;
    AVAssetReader* reader = [[AVAssetReader alloc] initWithAsset:st->asset error:&err];
    if (reader == nil) {
        POOL_END();
        return 0;
    }
    NSDictionary* settings = @{
        (id)AVFormatIDKey: @(kAudioFormatLinearPCM),
        (id)AVLinearPCMBitDepthKey: @(16),
        (id)AVLinearPCMIsBigEndianKey: @(NO),
        (id)AVLinearPCMIsFloatKey: @(NO),
        (id)AVLinearPCMIsNonInterleaved: @(NO)
    };
    AVAssetReaderTrackOutput* out = [[AVAssetReaderTrackOutput alloc] initWithTrack:st->audioTrack outputSettings:settings];
    [reader addOutput:out];
    [reader startReading];
    NSMutableData* pcm = [NSMutableData data];
    while (reader.status == AVAssetReaderStatusReading) {
        CMSampleBufferRef sb = [out copyNextSampleBuffer];
        if (sb == NULL) {
            break;
        }
        CMBlockBufferRef bb = CMSampleBufferGetDataBuffer(sb);
        if (bb != NULL) {
            size_t len = CMBlockBufferGetDataLength(bb);
            char* tmp = malloc(len);
            CMBlockBufferCopyDataBytes(bb, 0, len, tmp);
            [pcm appendBytes:tmp length:len];
            free(tmp);
        }
        CFRelease(sb);
    }
#ifndef CN1_USE_ARC
    [out release];
    [reader release];
#endif
    NSData* result = cn1RetainData(pcm);
    JAVA_LONG peer = (JAVA_LONG)(void*)result;
    POOL_END();
    return peer;
}

static void cn1ReaderClose(CN1VideoReaderState* st) {
#ifndef CN1_USE_ARC
    [st->generator release];
    [st->videoTrack release];
    [st->audioTrack release];
    [st->asset release];
    [st release];
#endif
}

// ---- Writer ----------------------------------------------------------------
static JAVA_LONG cn1WriterOpen(NSString* path, int width, int height, float fps, JAVA_BOOLEAN hevc,
        int bitRate, int gop, JAVA_BOOLEAN hasAudio, int sampleRate, int channels, int audioBitRate) {
    POOL_BEGIN();
    NSString* clean = cn1StripFilePrefix(path);
    [[NSFileManager defaultManager] removeItemAtPath:clean error:nil];
    NSURL* url = [NSURL fileURLWithPath:clean];
    NSError* err = nil;
    AVAssetWriter* writer = [[AVAssetWriter alloc] initWithURL:url fileType:AVFileTypeMPEG4 error:&err];
    if (writer == nil) {
        POOL_END();
        return 0;
    }
    CN1VideoWriterState* st = [[CN1VideoWriterState alloc] init];
    st->writer = writer;
    st->width = width;
    st->height = height;
    st->hasVideo = YES;
    st->hasAudio = hasAudio;

    NSString* codec = AVVideoCodecTypeH264;
    if (hevc) {
        if (@available(iOS 11.0, *)) {
            codec = AVVideoCodecTypeHEVC;
        }
    }
    NSDictionary* compression = @{
        AVVideoAverageBitRateKey: @(bitRate),
        AVVideoMaxKeyFrameIntervalKey: @(gop)
    };
    NSDictionary* videoSettings = @{
        AVVideoCodecKey: codec,
        AVVideoWidthKey: @(width),
        AVVideoHeightKey: @(height),
        AVVideoCompressionPropertiesKey: compression
    };
    st->videoInput = [[AVAssetWriterInput alloc] initWithMediaType:AVMediaTypeVideo outputSettings:videoSettings];
    st->videoInput.expectsMediaDataInRealTime = NO;
    NSDictionary* pba = @{
        (id)kCVPixelBufferPixelFormatTypeKey: @(kCVPixelFormatType_32BGRA),
        (id)kCVPixelBufferWidthKey: @(width),
        (id)kCVPixelBufferHeightKey: @(height)
    };
    st->adaptor = [[AVAssetWriterInputPixelBufferAdaptor alloc]
            initWithAssetWriterInput:st->videoInput sourcePixelBufferAttributes:pba];
    [writer addInput:st->videoInput];

    if (hasAudio) {
        NSDictionary* audioSettings = @{
            AVFormatIDKey: @(kAudioFormatMPEG4AAC),
            AVSampleRateKey: @(sampleRate),
            AVNumberOfChannelsKey: @(channels),
            AVEncoderBitRateKey: @(audioBitRate)
        };
        st->audioInput = [[AVAssetWriterInput alloc] initWithMediaType:AVMediaTypeAudio outputSettings:audioSettings];
        st->audioInput.expectsMediaDataInRealTime = NO;
        [writer addInput:st->audioInput];
    }

    if (![writer startWriting]) {
        POOL_END();
        return 0;
    }
    [writer startSessionAtSourceTime:kCMTimeZero];
#ifndef CN1_USE_ARC
    [st->writer retain];
    [st->videoInput retain];
    [st->adaptor retain];
    [st->audioInput retain];
#endif
    JAVA_LONG peer = (JAVA_LONG)(void*)st;
    POOL_END();
    return peer;
}

static void cn1WriterAddFrame(CN1VideoWriterState* st, JAVA_ARRAY_BYTE* rgba, int w, int h, JAVA_LONG ptsMs) {
    POOL_BEGIN();
    while (!st->videoInput.readyForMoreMediaData) {
        [NSThread sleepForTimeInterval:0.002];
    }
    CVPixelBufferRef pb = NULL;
    CVPixelBufferPoolRef pbPool = st->adaptor.pixelBufferPool;
    if (pbPool != NULL) {
        CVPixelBufferPoolCreatePixelBuffer(NULL, pbPool, &pb);
    }
    if (pb == NULL) {
        CVPixelBufferCreate(kCFAllocatorDefault, w, h, kCVPixelFormatType_32BGRA, NULL, &pb);
    }
    CVPixelBufferLockBaseAddress(pb, 0);
    unsigned char* dst = (unsigned char*)CVPixelBufferGetBaseAddress(pb);
    size_t dstStride = CVPixelBufferGetBytesPerRow(pb);
    for (int y = 0; y < h; y++) {
        unsigned char* dstRow = dst + y * dstStride;
        JAVA_ARRAY_BYTE* srcRow = rgba + (size_t)y * w * 4;
        for (int x = 0; x < w; x++) {
            unsigned char r = (unsigned char)srcRow[x * 4];
            unsigned char g = (unsigned char)srcRow[x * 4 + 1];
            unsigned char b = (unsigned char)srcRow[x * 4 + 2];
            unsigned char a = (unsigned char)srcRow[x * 4 + 3];
            dstRow[x * 4] = b;       // BGRA
            dstRow[x * 4 + 1] = g;
            dstRow[x * 4 + 2] = r;
            dstRow[x * 4 + 3] = a;
        }
    }
    CVPixelBufferUnlockBaseAddress(pb, 0);
    [st->adaptor appendPixelBuffer:pb withPresentationTime:CMTimeMake(ptsMs, 1000)];
    CVPixelBufferRelease(pb);
    POOL_END();
}

static void cn1WriterAddAudio(CN1VideoWriterState* st, JAVA_ARRAY_BYTE* pcm, int byteLen, int sampleRate, int channels, JAVA_LONG ptsMs) {
    if (st->audioInput == nil || byteLen <= 0) {
        return;
    }
    POOL_BEGIN();
    while (!st->audioInput.readyForMoreMediaData) {
        [NSThread sleepForTimeInterval:0.002];
    }
    AudioStreamBasicDescription asbd;
    memset(&asbd, 0, sizeof(asbd));
    asbd.mSampleRate = sampleRate;
    asbd.mFormatID = kAudioFormatLinearPCM;
    asbd.mFormatFlags = kAudioFormatFlagIsSignedInteger | kAudioFormatFlagIsPacked;
    asbd.mBitsPerChannel = 16;
    asbd.mChannelsPerFrame = channels;
    asbd.mFramesPerPacket = 1;
    asbd.mBytesPerFrame = 2 * channels;
    asbd.mBytesPerPacket = 2 * channels;

    CMAudioFormatDescriptionRef format = NULL;
    CMAudioFormatDescriptionCreate(kCFAllocatorDefault, &asbd, 0, NULL, 0, NULL, NULL, &format);

    CMBlockBufferRef block = NULL;
    CMBlockBufferCreateWithMemoryBlock(kCFAllocatorDefault, NULL, byteLen, kCFAllocatorDefault,
            NULL, 0, byteLen, 0, &block);
    CMBlockBufferReplaceDataBytes(pcm, block, 0, byteLen);

    CMSampleBufferRef sb = NULL;
    CMItemCount numSamples = byteLen / (2 * channels);
    CMSampleTimingInfo timing;
    timing.duration = CMTimeMake(1, sampleRate);
    timing.presentationTimeStamp = CMTimeMake(ptsMs, 1000);
    timing.decodeTimeStamp = kCMTimeInvalid;
    CMSampleBufferCreateReady(kCFAllocatorDefault, block, format, numSamples, 1, &timing, 0, NULL, &sb);
    if (sb != NULL) {
        [st->audioInput appendSampleBuffer:sb];
        CFRelease(sb);
    }
    if (block != NULL) {
        CFRelease(block);
    }
    if (format != NULL) {
        CFRelease(format);
    }
    POOL_END();
}

static JAVA_BOOLEAN cn1WriterClose(CN1VideoWriterState* st) {
    POOL_BEGIN();
    if (st->videoInput != nil) {
        [st->videoInput markAsFinished];
    }
    if (st->audioInput != nil) {
        [st->audioInput markAsFinished];
    }
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    [st->writer finishWritingWithCompletionHandler:^{
        dispatch_semaphore_signal(sem);
    }];
    dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
    BOOL ok = st->writer.status == AVAssetWriterStatusCompleted;
#ifndef CN1_USE_ARC
    [st->videoInput release];
    [st->adaptor release];
    [st->audioInput release];
    [st->writer release];
    [st release];
#endif
    POOL_END();
    return ok ? JAVA_TRUE : JAVA_FALSE;
}

// --------------------------------------------------------------------------
// JNI-style exported entry points (both the plain and _R_<ret> mangled forms)
// --------------------------------------------------------------------------
#define READER(peer) ((CN1VideoReaderState*)(void*)(peer))
#define WRITER(peer) ((CN1VideoWriterState*)(void*)(peer))

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoSupportsHEVC__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (@available(iOS 11.0, *)) {
        return JAVA_TRUE;
    }
    return JAVA_FALSE;
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoSupportsHEVC___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return com_codename1_impl_ios_IOSNative_videoSupportsHEVC__(CN1_THREAD_STATE_PASS_ARG me);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderOpen___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path) {
    return cn1VideoReaderOpen(toNSString(CN1_THREAD_STATE_PASS_ARG path));
}
JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderOpen___java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path) {
    return cn1VideoReaderOpen(toNSString(CN1_THREAD_STATE_PASS_ARG path));
}

JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderWidth___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return READER(peer)->displayWidth;
}
JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderWidth___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return READER(peer)->displayWidth;
}

JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderHeight___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return READER(peer)->displayHeight;
}
JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderHeight___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return READER(peer)->displayHeight;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderDuration___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return (JAVA_LONG)round(CMTimeGetSeconds(READER(peer)->asset.duration) * 1000.0);
}
JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderDuration___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_videoReaderDuration___long(CN1_THREAD_STATE_PASS_ARG me, peer);
}

JAVA_FLOAT com_codename1_impl_ios_IOSNative_videoReaderFrameRate___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    AVAssetTrack* t = READER(peer)->videoTrack;
    return t == nil ? 0 : t.nominalFrameRate;
}
JAVA_FLOAT com_codename1_impl_ios_IOSNative_videoReaderFrameRate___long_R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_videoReaderFrameRate___long(CN1_THREAD_STATE_PASS_ARG me, peer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoReaderHasVideo___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return READER(peer)->videoTrack != nil ? JAVA_TRUE : JAVA_FALSE;
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoReaderHasVideo___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_videoReaderHasVideo___long(CN1_THREAD_STATE_PASS_ARG me, peer);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoReaderHasAudio___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return READER(peer)->audioTrack != nil ? JAVA_TRUE : JAVA_FALSE;
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoReaderHasAudio___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_videoReaderHasAudio___long(CN1_THREAD_STATE_PASS_ARG me, peer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderAudioSampleRate___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    const AudioStreamBasicDescription* a = cn1AudioASBD(READER(peer));
    return a == NULL ? -1 : (int)a->mSampleRate;
}
JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderAudioSampleRate___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_videoReaderAudioSampleRate___long(CN1_THREAD_STATE_PASS_ARG me, peer);
}

JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderAudioChannels___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    const AudioStreamBasicDescription* a = cn1AudioASBD(READER(peer));
    return a == NULL ? -1 : (int)a->mChannelsPerFrame;
}
JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderAudioChannels___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return com_codename1_impl_ios_IOSNative_videoReaderAudioChannels___long(CN1_THREAD_STATE_PASS_ARG me, peer);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderFrameAt___long_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer, JAVA_LONG ms) {
    return cn1FrameAt(READER(peer), ms);
}
JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderFrameAt___long_long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer, JAVA_LONG ms) {
    return cn1FrameAt(READER(peer), ms);
}

JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderReadAudio___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return cn1ReadAudio(READER(peer));
}
JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderReadAudio___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return cn1ReadAudio(READER(peer));
}

void com_codename1_impl_ios_IOSNative_videoReaderClose___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    cn1ReaderClose(READER(peer));
}

JAVA_LONG com_codename1_impl_ios_IOSNative_videoWriterOpen___java_lang_String_int_int_float_boolean_int_int_boolean_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_INT width, JAVA_INT height, JAVA_FLOAT fps, JAVA_BOOLEAN hevc, JAVA_INT bitRate, JAVA_INT gop, JAVA_BOOLEAN hasAudio, JAVA_INT sampleRate, JAVA_INT channels, JAVA_INT audioBitRate) {
    return cn1WriterOpen(toNSString(CN1_THREAD_STATE_PASS_ARG path), width, height, fps, hevc, bitRate, gop, hasAudio, sampleRate, channels, audioBitRate);
}
JAVA_LONG com_codename1_impl_ios_IOSNative_videoWriterOpen___java_lang_String_int_int_float_boolean_int_int_boolean_int_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_INT width, JAVA_INT height, JAVA_FLOAT fps, JAVA_BOOLEAN hevc, JAVA_INT bitRate, JAVA_INT gop, JAVA_BOOLEAN hasAudio, JAVA_INT sampleRate, JAVA_INT channels, JAVA_INT audioBitRate) {
    return cn1WriterOpen(toNSString(CN1_THREAD_STATE_PASS_ARG path), width, height, fps, hevc, bitRate, gop, hasAudio, sampleRate, channels, audioBitRate);
}

void com_codename1_impl_ios_IOSNative_videoWriterAddFrame___long_byte_1ARRAY_int_int_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer, JAVA_OBJECT rgba, JAVA_INT w, JAVA_INT h, JAVA_LONG ptsMs) {
#ifndef NEW_CODENAME_ONE_VM
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)((org_xmlvm_runtime_XMLVMArray*)rgba)->fields.org_xmlvm_runtime_XMLVMArray.array_;
#else
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)rgba)->data;
#endif
    cn1WriterAddFrame(WRITER(peer), data, w, h, ptsMs);
}

void com_codename1_impl_ios_IOSNative_videoWriterAddAudio___long_byte_1ARRAY_int_int_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer, JAVA_OBJECT pcm, JAVA_INT sampleRate, JAVA_INT channels, JAVA_LONG ptsMs) {
#ifndef NEW_CODENAME_ONE_VM
    org_xmlvm_runtime_XMLVMArray* arr = (org_xmlvm_runtime_XMLVMArray*)pcm;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)arr->fields.org_xmlvm_runtime_XMLVMArray.array_;
    int len = arr->fields.org_xmlvm_runtime_XMLVMArray.length_;
#else
    JAVA_ARRAY arr = (JAVA_ARRAY)pcm;
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)arr->data;
    int len = arr->length;
#endif
    cn1WriterAddAudio(WRITER(peer), data, len, sampleRate, channels, ptsMs);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoWriterClose___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return cn1WriterClose(WRITER(peer));
}
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoWriterClose___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) {
    return cn1WriterClose(WRITER(peer));
}

#else
// --------------------------------------------------------------------------
// watchOS stub entry points. VideoIO is unsupported on the Watch
// (IOSImplementation.getVideoIO() returns null), so these are never invoked at
// runtime; they exist solely to satisfy the linker for the shared translated
// IOSNative bytecode that references them.
// --------------------------------------------------------------------------
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoSupportsHEVC__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) { return JAVA_FALSE; }
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoSupportsHEVC___R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) { return JAVA_FALSE; }

JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderOpen___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path) { return 0; }
JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderOpen___java_lang_String_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path) { return 0; }

JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderWidth___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return 0; }
JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderWidth___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return 0; }

JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderHeight___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return 0; }
JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderHeight___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return 0; }

JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderDuration___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return 0; }
JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderDuration___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return 0; }

JAVA_FLOAT com_codename1_impl_ios_IOSNative_videoReaderFrameRate___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return 0; }
JAVA_FLOAT com_codename1_impl_ios_IOSNative_videoReaderFrameRate___long_R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return 0; }

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoReaderHasVideo___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return JAVA_FALSE; }
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoReaderHasVideo___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return JAVA_FALSE; }

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoReaderHasAudio___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return JAVA_FALSE; }
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoReaderHasAudio___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return JAVA_FALSE; }

JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderAudioSampleRate___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return -1; }
JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderAudioSampleRate___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return -1; }

JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderAudioChannels___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return -1; }
JAVA_INT com_codename1_impl_ios_IOSNative_videoReaderAudioChannels___long_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return -1; }

JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderFrameAt___long_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer, JAVA_LONG ms) { return 0; }
JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderFrameAt___long_long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer, JAVA_LONG ms) { return 0; }

JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderReadAudio___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return 0; }
JAVA_LONG com_codename1_impl_ios_IOSNative_videoReaderReadAudio___long_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return 0; }

void com_codename1_impl_ios_IOSNative_videoReaderClose___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { }

JAVA_LONG com_codename1_impl_ios_IOSNative_videoWriterOpen___java_lang_String_int_int_float_boolean_int_int_boolean_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_INT width, JAVA_INT height, JAVA_FLOAT fps, JAVA_BOOLEAN hevc, JAVA_INT bitRate, JAVA_INT gop, JAVA_BOOLEAN hasAudio, JAVA_INT sampleRate, JAVA_INT channels, JAVA_INT audioBitRate) { return 0; }
JAVA_LONG com_codename1_impl_ios_IOSNative_videoWriterOpen___java_lang_String_int_int_float_boolean_int_int_boolean_int_int_int_R_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT path, JAVA_INT width, JAVA_INT height, JAVA_FLOAT fps, JAVA_BOOLEAN hevc, JAVA_INT bitRate, JAVA_INT gop, JAVA_BOOLEAN hasAudio, JAVA_INT sampleRate, JAVA_INT channels, JAVA_INT audioBitRate) { return 0; }

void com_codename1_impl_ios_IOSNative_videoWriterAddFrame___long_byte_1ARRAY_int_int_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer, JAVA_OBJECT rgba, JAVA_INT w, JAVA_INT h, JAVA_LONG ptsMs) { }
void com_codename1_impl_ios_IOSNative_videoWriterAddAudio___long_byte_1ARRAY_int_int_long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer, JAVA_OBJECT pcm, JAVA_INT sampleRate, JAVA_INT channels, JAVA_LONG ptsMs) { }

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoWriterClose___long(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return JAVA_FALSE; }
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_videoWriterClose___long_R_boolean(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG peer) { return JAVA_FALSE; }

#endif
