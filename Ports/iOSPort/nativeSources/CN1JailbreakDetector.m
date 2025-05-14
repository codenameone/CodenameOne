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
#ifdef CN1_DETECT_JAILBREAK
#import <UIKit/UIKit.h>
#import <dlfcn.h>
#import <sys/sysctl.h>
#import <mach-o/dyld.h>

void cn1DetectJailbreakBypassesAndExit() {
#if (TARGET_IPHONE_SIMULATOR)
    return;
#endif
    // List of known libraries used by bypass tools like Liberty Lite and Substrate
    NSArray *bypassLibraries = @[
        @"LibertyLite.dylib",
        @"Substrate.dylib",
        @"MobileSubstrate.dylib",
        @"SubstrateInserter.dylib",
        @"tsProtector.dylib",
        @"FridaGadget"
    ];
    
    // Check all loaded dynamic libraries
    for (int i = 0; i < _dyld_image_count(); i++) {
        const char *imageName = _dyld_get_image_name(i);
        NSString *libraryName = [NSString stringWithUTF8String:imageName];
        
        // Check if the library name matches any known bypass tool libraries
        for (NSString *bypassLibrary in bypassLibraries) {
            if ([libraryName containsString:bypassLibrary]) {
                // Jailbreak bypass detected, exit the app
                NSLog(@"Bypass library detected: %@", bypassLibrary);
                exit(0);  // Exit the app if a bypass tool is detected
            }
        }
    }
    
    // Additional check for file access to system areas (indicates potential bypass)
    NSArray *restrictedPaths = @[
        @"/Applications/Cydia.app",
        @"/usr/sbin/sshd",
        @"/bin/bash",
        @"/etc/apt",
        @"/Library/MobileSubstrate/MobileSubstrate.dylib"
    ];
    
    NSFileManager *fileManager = [NSFileManager defaultManager];
    for (NSString *path in restrictedPaths) {
        if ([fileManager fileExistsAtPath:path]) {
            // Jailbreak files detected, exit the app
            NSLog(@"Jailbreak-related file detected: %@", path);
            exit(0);  // Exit the app if a jailbreak-related file is found
        }
    }
    
    // Check if we can write to a restricted area (bypasses may allow this)
    NSString *testPath = @"/private/jailbreakTest.txt";
    NSError *error;
    [@"Test" writeToFile:testPath atomically:YES encoding:NSUTF8StringEncoding error:&error];
    if (!error) {
        // Able to write to restricted area, exit the app
        NSLog(@"Write access to restricted area detected.");
        exit(0);  // Exit the app if write access to restricted areas is detected
    }
    
    // Check for abnormal system behavior like successful fork()
    if (fork() == 0) {
        // fork() should not succeed on non-jailbroken devices, exit if it does
        NSLog(@"Fork succeeded, indicating jailbreak bypass.");
        exit(0);  // Exit the app if fork() succeeds
    }
    
    // Check for process tracing (which could indicate Liberty Lite tampering)
    struct kinfo_proc info;
    size_t size = sizeof(info);
    int name[4] = {CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid()};
    if (sysctl(name, 4, &info, &size, NULL, 0) == 0 && (info.kp_proc.p_flag & P_TRACED) != 0) {
        // Process is being traced, likely due to a jailbreak bypass
        NSLog(@"Process tracing detected, indicating jailbreak bypass.");
        exit(0);  // Exit the app if process tracing is detected
    }
    
    // If no jailbreak bypass was detected, the app continues as normal
    NSLog(@"No jailbreak bypass detected.");
}
#endif
