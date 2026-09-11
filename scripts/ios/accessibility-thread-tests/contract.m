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
#import <Foundation/Foundation.h>
#import <dispatch/dispatch.h>
#include <assert.h>
#include <stdlib.h>
#include <string.h>

// Override the host SDK's platform flags to exercise each production branch.
#undef TARGET_OS_OSX
#undef TARGET_OS_WATCH
#define TARGET_OS_OSX (CN1_TEST_PLATFORM == 1)
#define TARGET_OS_WATCH (CN1_TEST_PLATFORM == 2)

typedef void *JAVA_OBJECT;
typedef int JAVA_BOOLEAN;
#define JAVA_TRUE 1
#define JAVA_FALSE 0
#define CN1_THREAD_STATE_MULTI_ARG
#define CN1_THREAD_GET_STATE_PASS_SINGLE_ARG

static BOOL voiceOver, switchControl, assistiveTouch;
static int queryCount, notificationCount;
static NSString *UIAccessibilityVoiceOverStatusDidChangeNotification = @"CN1TestVoiceOver";
static NSString *UIAccessibilitySwitchControlStatusDidChangeNotification = @"CN1TestSwitchControl";
static NSString *UIAccessibilityAssistiveTouchStatusDidChangeNotification = @"CN1TestAssistiveTouch";

static BOOL query(BOOL value) {
    assert([NSThread isMainThread] && "UIKit query ran off Apple's main thread");
    queryCount++;
    return value;
}
static BOOL UIAccessibilityIsVoiceOverRunning(void) { return query(voiceOver); }
static BOOL UIAccessibilityIsSwitchControlRunning(void) { return query(switchControl); }
static BOOL touchQuery(void) { return query(assistiveTouch); }
// A pointer also exercises an absent weakly-linked iOS 10 function.
static BOOL (*UIAccessibilityIsAssistiveTouchRunning)(void) = touchQuery;
BOOL CN1MacHostIsVoiceOverRunning(void) { return voiceOver; }
void com_codename1_impl_ios_IOSImplementation_assistiveTechnologyStatusChanged__(void) {
    __atomic_add_fetch(&notificationCount, 1, __ATOMIC_RELAXED);
}

#include "implementation.h"

static BOOL active(void) {
    return com_codename1_impl_ios_IOSNative_isAssistiveTechnologyActive___R_boolean(NULL);
}

// Hold the main thread until every background caller has returned. A sync
// main-queue hop would time out here, modelling Java's held accessibility lock.
static void checkBackgroundCalls(BOOL expected) {
    dispatch_group_t group = dispatch_group_create();
    for(int i = 0; i < 32; i++) {
        dispatch_group_async(group, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            @autoreleasepool { assert(active() == expected); }
        });
    }
    assert(dispatch_group_wait(group, dispatch_time(DISPATCH_TIME_NOW, 3 * NSEC_PER_SEC)) == 0);
    dispatch_release(group);
}

static void drainMainQueue(void) {
    __block BOOL drained = NO;
    dispatch_async(dispatch_get_main_queue(), ^{ drained = YES; });
    NSDate *deadline = [NSDate dateWithTimeIntervalSinceNow:3];
    while(!drained && [deadline timeIntervalSinceNow] > 0) {
        [[NSRunLoop mainRunLoop] runUntilDate:[NSDate dateWithTimeIntervalSinceNow:0.01]];
    }
    assert(drained);
}

int main(int argc, const char **argv) {
    @autoreleasepool {
        assert(argc == 2);
        unsetenv("CN1_EAGER_A11Y");
        voiceOver = strcmp(argv[1], "voiceover") == 0;
        switchControl = strcmp(argv[1], "switch") == 0;
        assistiveTouch = strcmp(argv[1], "touch") == 0;
        BOOL eager = strcmp(argv[1], "eager") == 0;
        BOOL unavailable = strcmp(argv[1], "unavailable") == 0;
        if(eager) setenv("CN1_EAGER_A11Y", "1", 1);
        if(unavailable) {
            UIAccessibilityIsAssistiveTouchRunning = NULL;
            UIAccessibilityAssistiveTouchStatusDidChangeNotification = nil;
        }
#if TARGET_OS_WATCH
        checkBackgroundCalls(eager);
        assert(active() == eager);
#elif TARGET_OS_OSX
        checkBackgroundCalls(eager || voiceOver);
        assert(active() == (eager || voiceOver));
#else
        // Before the main queue initializes the cache, conservatively project.
        checkBackgroundCalls(YES);
        assert(queryCount == 0);
        drainMainQueue();
        BOOL expected = eager || voiceOver || switchControl || assistiveTouch;
        assert(active() == expected);
        int initialQueries = queryCount;
        if(!eager) assert(initialQueries > 0);
        checkBackgroundCalls(expected);
        for(int i = 0; i < 1000; i++) assert(active() == expected);
        drainMainQueue();
        assert(queryCount == initialQueries && "UI invalidations must use the cache");
        if(!eager) {
            // A client query also latches technologies with no public running flag.
            if(unavailable) {
                cn1AccessibilityNoteClientQuery();
                cn1AccessibilityNoteClientQuery();
                assert(notificationCount == 1);
            } else {
                CFNotificationCenterPostNotification(CFNotificationCenterGetLocalCenter(),
                    (__bridge CFStringRef)UIAccessibilityVoiceOverStatusDidChangeNotification,
                    NULL, NULL, true);
                assert(notificationCount == 1);
            }
            checkBackgroundCalls(YES);
            assert(active());
        }
#endif
#if TARGET_OS_WATCH || TARGET_OS_OSX
        assert(queryCount == 0);
#endif
    }
    return 0;
}
