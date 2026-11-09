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
#import "com_codename1_ai_whisper_NativeWhisperRecognizerImpl.h"
// The port headers exist in an application build, which is where the
// builder switches INCLUDE_CN1_WHISPER on. Compiled alone (the cn1lib probe in
// scripts/ci/check-ios-cn1lib.py) there are none, the define is absent and
// the file builds as the unsupported fallback.
#if __has_include("cn1_globals.h") && __has_include("CodenameOne_GLViewController.h")
#include "cn1_globals.h"
#import "CodenameOne_GLViewController.h"
#endif
#include <stdlib.h>

// whisper.cpp comes from the cn1-whisper-ios xcframework, which the build
// links for every app that depends on this cn1lib (PlatformFeatureCatalog).
// It exports Codename One's own C API over whisper.cpp,
// scripts/ai-natives/whisper/src/cn1_whisper.h, and nothing else -- these
// prototypes are that header's. Never declare whisper.h's structs here: a
// hand-written whisper_full_params is how this file once handed whisper_full
// a struct a fraction of the real size.
#if defined(INCLUDE_CN1_WHISPER)
extern char *cn1whisper_transcribe(const char *model_path, const char *wav_path, int segments);
extern void cn1whisper_free(char *text);
#endif

@implementation com_codename1_ai_whisper_NativeWhisperRecognizerImpl

// A transcription takes seconds, and ParparVM's collector can stop a thread
// only at a safepoint, so the call runs between CN1_YIELD_THREAD and
// CN1_RESUME_THREAD. Only C strings are touched in between: the arguments
// are already NSStrings and the result becomes one after the resume.
-(NSString*)run:(NSString*)model audio:(NSString*)audio segments:(int)segments {
#if !defined(INCLUDE_CN1_WHISPER)
    return @"";
#else
    if (model == nil || audio == nil) {
        return @"";
    }
    char *m = strdup([model UTF8String]);
    char *a = strdup([audio UTF8String]);
    CN1_YIELD_THREAD;
    char *text = cn1whisper_transcribe(m, a, segments);
    CN1_RESUME_THREAD;
    free(m);
    free(a);
    if (text == NULL) {
        return @"";
    }
    NSString *out = [[NSString alloc] initWithBytes:text length:strlen(text)
                                           encoding:NSUTF8StringEncoding];
    cn1whisper_free(text);
#if !__has_feature(objc_arc)
    [out autorelease];
#endif
    return out == nil ? @"" : out;
#endif
}

-(NSString*)transcribe:(NSString*)param param1:(NSString*)param1 {
    return [self run:param audio:param1 segments:0];
}

-(NSString*)transcribeSegments:(NSString*)param param1:(NSString*)param1 {
    return [self run:param audio:param1 segments:1];
}

-(BOOL)isSupported{
#if defined(INCLUDE_CN1_WHISPER)
    return YES;
#else
    return NO;
#endif
}

@end
