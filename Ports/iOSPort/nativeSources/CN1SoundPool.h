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
#import <AVFoundation/AVFoundation.h>

/// A loaded short sound: the source data plus a ring of pre-prepared players for
/// polyphony.
@interface CN1Sound : NSObject {
@public
    NSData* data;
    NSMutableArray* ring; // AVAudioPlayer*
}
@end

/// Low latency game sound pool backed by a ring of pre-prepared AVAudioPlayer
/// instances per sound. AVAudioPlayer natively supports per play volume, stereo pan,
/// playback rate (pitch) and looping, which map directly onto the gaming SoundPool
/// API.
@interface CN1SoundPool : NSObject {
    int maxStreams;
    int nextVoiceId;
    NSMutableArray* allSounds;   // CN1Sound*
    NSMutableDictionary* voices; // NSNumber(voiceId) -> AVAudioPlayer*
}
- (id)initWithMaxStreams:(int)max;
- (CN1Sound*)loadData:(NSData*)d ringSize:(int)ring;
- (int)play:(CN1Sound*)s volume:(float)v pan:(float)p rate:(float)r loop:(int)loop;
- (void)setVoiceVolume:(int)voiceId value:(float)v;
- (void)setVoiceRate:(int)voiceId value:(float)r;
- (void)setVoicePan:(int)voiceId value:(float)p;
- (void)pauseVoice:(int)voiceId;
- (void)resumeVoice:(int)voiceId;
- (void)stopVoice:(int)voiceId;
- (void)stopAll;
- (void)autoPauseAll;
- (void)autoResumeAll;
- (void)unloadSound:(CN1Sound*)s;
@end
