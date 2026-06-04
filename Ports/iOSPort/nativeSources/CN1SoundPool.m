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
#import "CN1SoundPool.h"

@implementation CN1Sound
@end

@implementation CN1SoundPool

- (id)initWithMaxStreams:(int)max {
    self = [super init];
    if (self) {
        maxStreams = max < 1 ? 1 : max;
        nextVoiceId = 1;
        allSounds = [[NSMutableArray alloc] init];
        voices = [[NSMutableDictionary alloc] init];
    }
    return self;
}

- (AVAudioPlayer*)newPlayer:(NSData*)d {
    NSError* err = nil;
    AVAudioPlayer* p = [[AVAudioPlayer alloc] initWithData:d error:&err];
    if (p != nil) {
        p.enableRate = YES;
        [p prepareToPlay];
    }
    return p;
}

- (CN1Sound*)loadData:(NSData*)d ringSize:(int)ring {
    CN1Sound* s = [[CN1Sound alloc] init];
    s->data = d;
#ifndef CN1_USE_ARC
    [d retain];
#endif
    int n = ring < 1 ? 1 : ring;
    if (n > maxStreams) {
        n = maxStreams;
    }
    s->ring = [[NSMutableArray alloc] init];
    for (int i = 0; i < n; i++) {
        AVAudioPlayer* p = [self newPlayer:d];
        if (p != nil) {
            [s->ring addObject:p];
#ifndef CN1_USE_ARC
            [p release];
#endif
        }
    }
    [allSounds addObject:s];
#ifndef CN1_USE_ARC
    [s release];
#endif
    return s;
}

- (void)pruneFinished {
    NSMutableArray* dead = [NSMutableArray array];
    for (NSNumber* key in voices) {
        AVAudioPlayer* p = [voices objectForKey:key];
        if (!p.isPlaying) {
            [dead addObject:key];
        }
    }
    [voices removeObjectsForKeys:dead];
}

- (int)play:(CN1Sound*)s volume:(float)v pan:(float)p rate:(float)r loop:(int)loop {
    [self pruneFinished];
    if ((int)[voices count] >= maxStreams) {
        return -1;
    }
    AVAudioPlayer* free = nil;
    for (AVAudioPlayer* candidate in s->ring) {
        if (!candidate.isPlaying) {
            free = candidate;
            break;
        }
    }
    if (free == nil) {
        return -1;
    }
    free.volume = v;
    free.pan = p;
    free.rate = (r <= 0) ? 1.0f : r;
    free.numberOfLoops = loop; // 0 once, -1 forever, n extra repeats
    free.currentTime = 0;
    if (![free play]) {
        return -1;
    }
    int voiceId = nextVoiceId++;
    [voices setObject:free forKey:[NSNumber numberWithInt:voiceId]];
    return voiceId;
}

- (AVAudioPlayer*)voice:(int)voiceId {
    return [voices objectForKey:[NSNumber numberWithInt:voiceId]];
}

- (void)setVoiceVolume:(int)voiceId value:(float)v {
    [[self voice:voiceId] setVolume:v];
}

- (void)setVoiceRate:(int)voiceId value:(float)r {
    AVAudioPlayer* p = [self voice:voiceId];
    if (p != nil) {
        p.rate = (r <= 0) ? 1.0f : r;
    }
}

- (void)setVoicePan:(int)voiceId value:(float)pan {
    [[self voice:voiceId] setPan:pan];
}

- (void)pauseVoice:(int)voiceId {
    [[self voice:voiceId] pause];
}

- (void)resumeVoice:(int)voiceId {
    [[self voice:voiceId] play];
}

- (void)stopVoice:(int)voiceId {
    AVAudioPlayer* p = [self voice:voiceId];
    if (p != nil) {
        [p stop];
        p.currentTime = 0;
        [voices removeObjectForKey:[NSNumber numberWithInt:voiceId]];
    }
}

- (void)stopAll {
    for (NSNumber* key in voices) {
        AVAudioPlayer* p = [voices objectForKey:key];
        [p stop];
        p.currentTime = 0;
    }
    [voices removeAllObjects];
}

- (void)autoPauseAll {
    for (NSNumber* key in voices) {
        [[voices objectForKey:key] pause];
    }
}

- (void)autoResumeAll {
    for (NSNumber* key in voices) {
        [[voices objectForKey:key] play];
    }
}

- (void)unloadSound:(CN1Sound*)s {
    for (AVAudioPlayer* p in s->ring) {
        [p stop];
    }
    [allSounds removeObject:s];
}

#ifndef CN1_USE_ARC
- (void)dealloc {
    [allSounds release];
    [voices release];
    [super dealloc];
}
#endif

@end
