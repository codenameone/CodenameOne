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
#import "CodenameOne_GLViewController.h"
#include "java_lang_Runnable.h"
#include "com_codename1_ui_Display.h"
#include "xmlvm.h"

static float volume = -1;
AudioPlayer* currentlyPlaying = nil;

@implementation AudioPlayer

- (id)initWithURL:(NSString*)url callback:(void*)callback {
    self = [super init];
    if (self) {
        runnableCallback = callback;
        errorInfo = nil;
        playerInstance = nil;
        avPlayerInstance = nil;
        if(currentlyPlaying == nil) {
            currentlyPlaying = self;
        }
        if ([url hasPrefix:@"http"]) {
            avPlayerInstance = [[AVPlayer alloc] initWithURL:[NSURL URLWithString:url]];
            avPlayerInstance.actionAtItemEnd = AVPlayerActionAtItemEndNone;
            
            [[NSNotificationCenter defaultCenter] addObserver:self
                                                     selector:@selector(playerItemDidReachEnd:)
                                                         name:AVPlayerItemDidPlayToEndTimeNotification
                                                       object:[avPlayerInstance currentItem]];
        } else {
#ifndef CN1_USE_ARC
            playerInstance = [[AVAudioPlayer alloc] initWithContentsOfURL:[NSURL URLWithString:url] error:&errorInfo];
#else
            playerInstance = [[AVAudioPlayer alloc] initWithContentsOfURL:[NSURL URLWithString:url] error:nil];
#endif
            if(playerInstance != nil)
            {
                playerInstance.delegate = self;
                if(volume > -1) {
                    playerInstance.volume = volume;
                }
            }
        }
    }
    
    return self;    
}

- (id)initWithNSData:(NSData*)data callback:(void*)callback {
    self = [super init];
    if (self) {
        runnableCallback = callback;
#ifndef CN1_USE_ARC
        playerInstance = [[AVAudioPlayer alloc] initWithData:data error:&errorInfo];
#else
        playerInstance = [[AVAudioPlayer alloc] initWithData:data error:nil];
#endif
        if(playerInstance != nil)
        {
            playerInstance.delegate = self;
            if(volume > -1) {
                playerInstance.volume = volume;
            }
        }
        if(currentlyPlaying == nil) {
            currentlyPlaying = self;
        }
    }
    
    return self;    
}

- (void)audioPlayerBeginInterruption:(AVAudioPlayer *)player {
    
}

- (void)audioPlayerDecodeErrorDidOccur:(AVAudioPlayer *)player error:(NSError *)error {
    if(runnableCallback != 0) {
#ifdef NEW_CODENAME_ONE_VM
        virtual_java_lang_Runnable_run__(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_OBJECT)runnableCallback);
#else
        (*(void (*)(JAVA_OBJECT)) *(((java_lang_Object*)runnableCallback)->tib->itableBegin)[XMLVM_ITABLE_IDX_java_lang_Runnable_run__])(runnableCallback);
#endif
    }
}

- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag {
/*#ifndef CN1_USE_ARC
    if(playerInstance != nil) {
        [self release];
    }
#endif*/
    if(runnableCallback != 0) {
        JAVA_OBJECT o = com_codename1_ui_Display_getInstance__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        com_codename1_ui_Display_callSerially___java_lang_Runnable(CN1_THREAD_GET_STATE_PASS_ARG o, runnableCallback);
    }
}

- (void)playerItemDidReachEnd:(NSNotification *)notification {
    if(runnableCallback != 0) {
        JAVA_OBJECT o = com_codename1_ui_Display_getInstance__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        com_codename1_ui_Display_callSerially___java_lang_Runnable(CN1_THREAD_GET_STATE_PASS_ARG o, runnableCallback);
    }
}

- (void)audioPlayerEndInterruption:(AVAudioPlayer *)player {
    
}

- (int)getAudioDuration {
    if(playerInstance != nil) {
        return (int)(playerInstance.duration * 1000);
    }
    if(avPlayerInstance.currentItem != nil) {
        return (int)(CMTimeGetSeconds(avPlayerInstance.currentItem.duration) * 1000);
    }
    return -1;
}

- (int)getAudioTime {
    if(playerInstance != nil) {
        return (int)(playerInstance.currentTime * 1000);
    }
    if(avPlayerInstance.currentItem != nil) {
        return (int)(CMTimeGetSeconds(avPlayerInstance.currentItem.currentTime) * 1000);
    }
    return -1;
}

- (void)playAudio {
    //[[AVAudioSession sharedInstance] setCategory: AVAudioSessionCategoryPlayAndRecord error: nil];
    //UInt32 audioRouteOverride = kAudioSessionOverrideAudioRoute_Speaker;
    //AudioSessionSetProperty (kAudioSessionProperty_OverrideAudioRoute,sizeof (audioRouteOverride),&audioRouteOverride);

    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    NSError *setCategoryError = nil;
    BOOL success = [audioSession setCategory:AVAudioSessionCategoryPlayback error:&setCategoryError];
    if (!success) {
        NSLog(@"ERROR");
    }
    NSError *activationError = nil;
    success = [audioSession setActive:YES error:&activationError];
    if (!success) {
        NSLog(@"ERROR");
    }
    [[UIApplication sharedApplication] beginReceivingRemoteControlEvents];
    
    currentlyPlaying = self;
    if(playerInstance != nil) {
/*#ifndef CN1_USE_ARC
        [self retain];
#endif*/
        BOOL res = [playerInstance play];
        if (!res) {
            NSLog(@"Failed to play");
        }
    } else if(avPlayerInstance != nil){
        [avPlayerInstance play];
    } else if(runnableCallback != 0) {
        JAVA_OBJECT o = com_codename1_ui_Display_getInstance__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
        com_codename1_ui_Display_callSerially___java_lang_Runnable(CN1_THREAD_GET_STATE_PASS_ARG o, runnableCallback);
    }
}

- (void)pauseAudio {
    if(playerInstance != nil) {
        [playerInstance pause];
    } else {
        if(avPlayerInstance != nil){
            [avPlayerInstance pause];
        }
    }
}

- (void)setAudioTime:(int)time {
    if(playerInstance != nil) {
        playerInstance.currentTime = ((float)time) / 1000.0;
    } else {
        if(avPlayerInstance != nil){
            [avPlayerInstance seekToTime:CMTimeMakeWithSeconds(((float)time) / 1000.0, avPlayerInstance.currentItem.currentTime.timescale)];
        }
    }
}

+ (float)getVolume {
    return volume;
}

- (void)vol:(float)vol {
    if(playerInstance != nil) {
        playerInstance.volume = vol;
    }
}

+ (void)setVolume:(float)vol {
    volume = vol;
    if(currentlyPlaying != nil) {
        [currentlyPlaying vol:vol];
    }
}

- (BOOL) isPlaying {
    if(playerInstance != nil) {
        return playerInstance.isPlaying;
    }
    if(avPlayerInstance != nil) {
        return [avPlayerInstance rate] != 0.0;
    }
    return false;
}

-(void)dealloc {
    if(currentlyPlaying == self) {
        currentlyPlaying = nil;
    }
    if(playerInstance != nil) {
#ifndef CN1_USE_ARC
        [playerInstance release];
#endif
        playerInstance = nil;
    } else {
        if(avPlayerInstance != nil){
#ifndef CN1_USE_ARC
            [avPlayerInstance release];
#endif
            avPlayerInstance = nil;
        }
    }
#ifndef CN1_USE_ARC
    [super dealloc];
#endif
}

@end
