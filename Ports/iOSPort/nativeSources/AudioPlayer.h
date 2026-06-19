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
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#include "TargetConditionals.h"
#if !TARGET_OS_WATCH
// AudioToolbox is unavailable on watchOS; AudioPlayer uses AVAudioPlayer
// (AVFoundation, present on watch) and doesn't need AudioToolbox types here.
#import <AudioToolbox/AudioToolbox.h>
#endif

@interface AudioPlayer : NSObject<AVAudioPlayerDelegate> {
    AVAudioPlayer* playerInstance;
    AVPlayer* avPlayerInstance;
    NSError* errorInfo;
    void* runnableCallback;
}
- (id)initWithURL:(NSString*)url callback:(void*)callback error:(NSError **)outError;
- (id)initWithNSData:(NSData*)data callback:(void*)callback error:(NSError **)outError;
- (void)audioPlayerBeginInterruption:(AVAudioPlayer *)player;
- (void)audioPlayerDecodeErrorDidOccur:(AVAudioPlayer *)player error:(NSError *)error;
- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag;
- (void)audioPlayerEndInterruption:(AVAudioPlayer *)player;

- (int)getAudioDuration;
- (int)getAudioTime;
- (void)pauseAudio;
- (void)playAudio;
- (void)setAudioTime:(int)time;
- (BOOL) isPlaying; 
+ (float)getVolume;
+ (void)setVolume:(float)vol;
+ (void)stop;

@end
