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
#import "CN1AudioUnit.h"
#import <AudioToolbox/AudioToolbox.h>
#import "com_codename1_media_MediaManager.h"
#import "com_codename1_media_AudioBuffer.h"
#import "com_codename1_impl_ios_IOSImplementation.h"

static void HandleInputBuffer (
                              void                                 *userData,
                              AudioQueueRef                        inAQ,
                              AudioQueueBufferRef                  inBuffer,
                              const AudioTimeStamp                 *inStartTime,
                              UInt32                               inNumPackets,
                              const AudioStreamPacketDescription   *inPacketDesc

                              ) {
    CN1AudioUnit* audioUnit = (CN1AudioUnit*) userData;
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    enteringNativeAllocations();
    
    JAVA_ARRAY convertedSampleBuffer = [audioUnit convertedSampleBuffer];
    JAVA_ARRAY_FLOAT* sampleData = (JAVA_ARRAY_FLOAT*)convertedSampleBuffer->data;
    int len = convertedSampleBuffer->length;

    JAVA_OBJECT audioBuffer = com_codename1_media_MediaManager_getAudioBuffer___java_lang_String_boolean_int_R_com_codename1_media_AudioBuffer(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG [audioUnit path]), JAVA_TRUE, 64);
    
    SInt16 *inputFrames = (SInt16*)( inBuffer->mAudioData);
    UInt32 numFrames = inNumPackets;
    
    // If your DSP code can use integers, then don't bother converting to
    // floats here, as it just wastes CPU. However, most DSP algorithms rely
    // on floating point, and this is especially true if you are porting a
    // VST/AU to iOS.
    int index = 0;
    for(int i = 0; i < numFrames; i++) {
      sampleData[index] = (JAVA_ARRAY_FLOAT)inputFrames[i] / 32768;
      index++;
      if (index >= len) {
          com_codename1_media_AudioBuffer_copyFrom___int_int_float_1ARRAY_int_int(CN1_THREAD_GET_STATE_PASS_ARG audioBuffer, (JAVA_INT)[audioUnit sampleRate], [audioUnit channels], (JAVA_OBJECT)convertedSampleBuffer, 0, index);
          index = 0;
      }
    }

    if (index > 0) {
      com_codename1_media_AudioBuffer_copyFrom___int_int_float_1ARRAY_int_int(CN1_THREAD_GET_STATE_PASS_ARG audioBuffer, (JAVA_INT)[audioUnit sampleRate], [audioUnit channels], (JAVA_OBJECT)convertedSampleBuffer, 0, index);
      index = 0;
    }
    
    AudioQueueEnqueueBuffer ([audioUnit queue], inBuffer, 0, NULL);
    
    finishedNativeAllocations();
    
}

@implementation CN1AudioUnit

-(JAVA_ARRAY)convertedSampleBuffer {
    return convertedSampleBuffer;
}

-(NSString*)path {
    return path;
}

-(AudioBuffer*)audioBuffer {
    return buff;
}

-(AudioQueueRef)queue {
    return queue;
}

-(AudioStreamBasicDescription)fmt {
    return fmt;
}



-(id)initWithPath:(NSString*)_path channels:(int)_channels sampleRate:(float)_sampleRate sampleBuffer:(JAVA_ARRAY)sampleBuffer {
    com_codename1_impl_ios_IOSImplementation_retain___java_lang_Object(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG, (JAVA_OBJECT)sampleBuffer);
    
    convertedSampleBuffer = sampleBuffer;
    path = _path;
    channels = _channels;
    sampleRate = _sampleRate;
    
    
    

    return self;
}

-(void)run {
    
    NSError * error;

    fmt.mFormatID         = kAudioFormatLinearPCM;
    fmt.mSampleRate       = sampleRate;
    fmt.mChannelsPerFrame = channels;
    fmt.mBitsPerChannel   = 16;
    fmt.mFramesPerPacket  = 1;
    fmt.mBytesPerFrame = sizeof (SInt16);
    fmt.mBytesPerPacket = sizeof (SInt16);


    fmt.mFormatFlags =  kLinearPCMFormatFlagIsSignedInteger  | kLinearPCMFormatFlagIsPacked;

    [NSRunLoop  currentRunLoop];
    

    OSStatus status = AudioQueueNewInput (                              // 1
                        &fmt,                          // 2
                        HandleInputBuffer,                            // 3
                        self,                                      // 4
                       NULL,                                         // 5
                        kCFRunLoopCommonModes,                        // 6
                        0,                                            // 7
                        &queue                                // 8
                        );


      int kNumberBuffers = 5;
      int kSamplesSize = 4096;
    AudioQueueBufferRef  buffers[kNumberBuffers];
    UInt32 bufferByteSize = kSamplesSize;
    for (int i = 0; i < kNumberBuffers; ++i) {
        OSStatus allocateStatus;
        allocateStatus =  AudioQueueAllocateBuffer (
                                  queue,
                                  bufferByteSize,
                                  &buffers[i]
                                  );
        OSStatus  enqueStatus;
        NSLog(@"allocateStatus = %d" , allocateStatus);
        enqueStatus =   AudioQueueEnqueueBuffer (
                                 queue,
                                 buffers[i],
                                 0,
                                 NULL
                                 );
        NSLog(@"enqueStatus = %d" , enqueStatus);
    }
    AudioQueueStart (queue, NULL);
}

-(BOOL)start {
    [self run];
    return YES;
}
-(BOOL)stop {
    AudioQueueStop(queue, YES);
    AudioQueueDispose(queue, YES);
    return YES;
}
-(float)sampleRate {
    return sampleRate;
}
-(int)channels {
    return channels;
}

-(void)dealloc {

    if (convertedSampleBuffer != NULL) {
        com_codename1_impl_ios_IOSImplementation_release___java_lang_Object(CN1_THREAD_GET_STATE_PASS_ARG (JAVA_OBJECT)convertedSampleBuffer);
        convertedSampleBuffer = NULL;
    }
    
    
    [super dealloc];
}

@end

