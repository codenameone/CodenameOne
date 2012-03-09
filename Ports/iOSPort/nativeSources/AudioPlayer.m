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
#import "AudioPlayer.h"
#include "java_lang_Runnable.h"

static float volume = -1;
AudioPlayer* currentlyPlaying = nil;

@implementation AudioPlayer

- (id)initWithURL:(NSString*)url callback:(void*)callback {
    self = [super init];
    if (self) {
        runnableCallback = callback;
        errorInfo = [[NSError alloc] init];
        playerInstance = [[AVAudioPlayer alloc] initWithContentsOfURL:[NSURL URLWithString:url] error:errorInfo];
        playerInstance.delegate = self;
        if(volume > -1) {
            playerInstance.volume = volume;
        }
        if(currentlyPlaying == nil) {
            currentlyPlaying = self;
        }
        [playerInstance retain];
    }
    
    return self;    
}

- (id)initWithNSData:(NSData*)data callback:(void*)callback {
    self = [super init];
    if (self) {
        runnableCallback = callback;
        errorInfo = [[NSError alloc] init];
        playerInstance = [[AVAudioPlayer alloc] initWithData:data error:errorInfo];
        playerInstance.delegate = self;
        if(volume > -1) {
            playerInstance.volume = volume;
        }
        if(currentlyPlaying == nil) {
            currentlyPlaying = self;
        }
        [playerInstance retain];
    }
    
    return self;    
}

- (void)audioPlayerBeginInterruption:(AVAudioPlayer *)player {
    
}

- (void)audioPlayerDecodeErrorDidOccur:(AVAudioPlayer *)player error:(NSError *)error {
    (*(void (*)(JAVA_OBJECT)) *(((java_lang_Object*)runnableCallback)->tib->itableBegin)[XMLVM_ITABLE_IDX_java_lang_Runnable_run__])(runnableCallback);
}

- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag {
    (*(void (*)(JAVA_OBJECT)) *(((java_lang_Object*)runnableCallback)->tib->itableBegin)[XMLVM_ITABLE_IDX_java_lang_Runnable_run__])(runnableCallback);
}

- (void)audioPlayerEndInterruption:(AVAudioPlayer *)player {
    
}

- (int)getAudioDuration {
    return (int)(playerInstance.duration * 1000);
}

- (int)getAudioTime {
    return (int)(playerInstance.currentTime * 1000);
}

- (void)playAudio {
    currentlyPlaying = self;
    [playerInstance play];
}

- (void)pauseAudio {
    [playerInstance pause];
}

- (void)setAudioTime:(int)time {
    playerInstance.currentTime = ((float)time) / 1000.0;
}

+ (float)getVolume {
    return volume;
}

- (void)vol:(float)vol {
    playerInstance.volume = vol;
}

+ (void)setVolume:(float)vol {
    volume = vol;
    if(currentlyPlaying != nil) {
        [currentlyPlaying vol:vol];
    }
}


-(void)dealloc {
    if(currentlyPlaying == self) {
        currentlyPlaying = nil;
    }
    [errorInfo release];
    [playerInstance release];
	[super dealloc];
}

@end
