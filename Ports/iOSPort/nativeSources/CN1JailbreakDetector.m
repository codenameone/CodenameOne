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

#import "CN1JailbreakDetector.h"
#import <TargetConditionals.h>
#import <dlfcn.h>
#import <sys/sysctl.h>
#import <mach-o/dyld.h>
#import <unistd.h>
#import <stdlib.h>

// Note: there is deliberately no fork() probe here. The classic form,
// "if (fork() == 0) { exit(0); }", terminates the *child* and lets the parent
// sail on, so it never did what it claimed. Reinstating it correctly would
// mean calling a restricted syscall that trips App Review static analysis, for
// a signal the dyld-image and restricted-path probes already carry.

NSString *cn1JailbreakSignals(void) {
#if (TARGET_IPHONE_SIMULATOR)
    return @"";
#else
    NSMutableArray *signals = [NSMutableArray array];

    // Dynamic library injection, as used by Frida/Objection and friends.
    if (getenv("DYLD_INSERT_LIBRARIES") != NULL) {
        [signals addObject:@"dyldInsert"];
    }

    // Known hooking / jailbreak-bypass libraries loaded into the process.
    NSArray *bypassLibraries = @[
        @"LibertyLite.dylib",
        @"Substrate.dylib",
        @"MobileSubstrate.dylib",
        @"SubstrateInserter.dylib",
        @"tsProtector.dylib",
        @"FridaGadget"
    ];
    for (uint32_t i = 0; i < _dyld_image_count(); i++) {
        const char *imageName = _dyld_get_image_name(i);
        if (imageName == NULL) {
            continue;
        }
        NSString *libraryName = [NSString stringWithUTF8String:imageName];
        for (NSString *bypassLibrary in bypassLibraries) {
            if ([libraryName containsString:bypassLibrary]) {
                [signals addObject:@"hookLib"];
                i = _dyld_image_count();
                break;
            }
        }
    }

    // The two filesystem probes below describe an iOS sandbox that has been broken out
    // of, and a Mac is not that sandbox. /bin/bash and /usr/sbin/sshd ship with macOS and
    // /private is writable there, so on Mac Catalyst both fire on a stock machine -- and
    // an app that asks isJailbrokenDevice() at startup, as ours does, refuses to launch on
    // every Mac. The instrumentation probes on either side of this stay, because an
    // injected dylib or a hooking library means the same thing wherever it is loaded.
#if !TARGET_OS_MACCATALYST && !TARGET_OS_OSX
    // Files that only exist once the sandbox has been broken out of.
    NSArray *restrictedPaths = @[
        @"/Applications/Cydia.app",
        @"/Library/MobileSubstrate/MobileSubstrate.dylib",
        @"/usr/sbin/sshd",
        @"/bin/bash",
        @"/etc/apt",
        @"/private/var/lib/apt/"
    ];
    NSFileManager *fileManager = [NSFileManager defaultManager];
    for (NSString *path in restrictedPaths) {
        if ([fileManager fileExistsAtPath:path]) {
            [signals addObject:@"jailbreakFile"];
            break;
        }
    }

    // Writing outside the sandbox should be impossible.
    NSString *testPath = @"/private/cn1JailbreakTest.txt";
    NSError *error = nil;
    BOOL wroteFile = [@"Test" writeToFile:testPath atomically:YES
                                 encoding:NSUTF8StringEncoding error:&error];
    if (wroteFile && error == nil) {
        [fileManager removeItemAtPath:testPath error:nil];
        [signals addObject:@"restrictedWrite"];
    }
#endif

    // A debugger or instrumentation tool attached to the process.
    struct kinfo_proc info;
    size_t size = sizeof(info);
    int name[4] = {CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid()};
    if (sysctl(name, 4, &info, &size, NULL, 0) == 0
            && (info.kp_proc.p_flag & P_TRACED) != 0) {
        [signals addObject:@"traced"];
    }

    return [signals componentsJoinedByString:@","];
#endif
}

#ifdef CN1_DETECT_JAILBREAK
/**
 * Whether a signal says the DEVICE is compromised, as opposed to saying somebody is
 * looking at the app.
 *
 * The probe list grew a `traced` signal so DeviceIntegrity could report a debugger,
 * which is worth reporting -- attaching one is how a build gets instrumented. Exiting on
 * it is a different matter: a clean physical device launched from Xcode is traced, so the
 * launch gate terminated every ordinary debug session on a project that leaves
 * ios.detectJailbreak on. That reads as "the app crashes on device", and the usual fix a
 * developer reaches for is turning the protection off.
 */
static BOOL cn1IsJailbreakSignal(NSString *signal) {
    return [signal isEqualToString:@"dyldInsert"]
        || [signal isEqualToString:@"hookLib"]
        || [signal isEqualToString:@"jailbreakFile"]
        || [signal isEqualToString:@"restrictedWrite"];
}

void cn1DetectJailbreakBypassesAndExit(void) {
    NSString *signals = cn1JailbreakSignals();
    NSMutableArray *fatal = [NSMutableArray array];
    for (NSString *signal in [signals componentsSeparatedByString:@","]) {
        if (cn1IsJailbreakSignal(signal)) {
            [fatal addObject:signal];
        }
    }
    if (fatal.count > 0) {
        NSLog(@"Jailbreak bypass detected: %@", [fatal componentsJoinedByString:@","]);
        exit(0);
    }
}
#endif
