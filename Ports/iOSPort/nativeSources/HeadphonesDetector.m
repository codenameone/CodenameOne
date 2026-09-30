/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

//#define DETECT_HEADPHONE
#ifdef DETECT_HEADPHONE
#import "HeadphonesDetector.h"
#import <AVFoundation/AVFoundation.h>
#import "CodenameOne_GLViewController.h"
#include "com_codename1_impl_ios_IOSImplementation.h"

/*
 * Reports headphones connecting and disconnecting to IOSImplementation (ios.headphoneCallback).
 *
 * This used the C AudioSession API (AudioSessionInitialize / AudioSessionAddPropertyListener),
 * deprecated since iOS 7 and absent from tvOS, so a TV slice of an app using the callback failed
 * to compile. It also never ran: nothing created the detector, so the listener was never
 * registered and neither callback fired on any device. It now observes AVAudioSession's route
 * changes, available on iOS and tvOS, and starts itself when the image loads.
 */
static HeadphonesDetector *headphonesDetector;

@implementation HeadphonesDetector

@dynamic headphonesArePlugged;

+ (void)load {
    // +load runs before main; create the detector once the application is running.
    dispatch_async(dispatch_get_main_queue(), ^{
        [HeadphonesDetector sharedDetector];
    });
}

+ (HeadphonesDetector *) sharedDetector {
    if (headphonesDetector == nil) {
        headphonesDetector = [[self alloc] init];
    }
    return headphonesDetector;
}

- (BOOL) headphonesArePlugged {
    for (AVAudioSessionPortDescription *output in [[[AVAudioSession sharedInstance] currentRoute] outputs]) {
        NSString *port = [output portType];
        if ([port isEqualToString:AVAudioSessionPortHeadphones]
                || [port isEqualToString:AVAudioSessionPortBluetoothA2DP]
                || [port isEqualToString:AVAudioSessionPortBluetoothHFP]
                || [port isEqualToString:AVAudioSessionPortBluetoothLE]) {
            return YES;
        }
    }
    return NO;
}

- (id) init {
    if ((self = [super init])) {
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(routeChanged:)
                                                     name:AVAudioSessionRouteChangeNotification
                                                   object:nil];
    }
    return self;
}

- (void) routeChanged:(NSNotification *)notification {
    NSUInteger reason = [[[notification userInfo] objectForKey:AVAudioSessionRouteChangeReasonKey]
                         unsignedIntegerValue];
    if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
        com_codename1_impl_ios_IOSImplementation_headphonesDisconnected__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    } else if (reason == AVAudioSessionRouteChangeReasonNewDeviceAvailable) {
        com_codename1_impl_ios_IOSImplementation_headphonesConnected__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    }
}

- (void) dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [super dealloc];
}

@end
#endif
