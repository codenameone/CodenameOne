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

//#define CN1_DETECT_JAILBREAK 1

/**
 * Runs every jailbreak / hooking probe and returns the ones that fired as a
 * comma separated list of stable codes, or an empty string on a clean device.
 * Codes: dyldInsert, hookLib, jailbreakFile, restrictedWrite, traced.
 *
 * Always compiled, independent of CN1_DETECT_JAILBREAK, because
 * DeviceIntegrity.getCompromiseReasons() surfaces these at runtime without
 * terminating the app. Returns an empty string on the simulator.
 */
NSString *cn1JailbreakSignals(void);

#ifdef CN1_DETECT_JAILBREAK
/**
 * Legacy hard gate kept for the ios.detectJailbreak build hint: runs
 * cn1JailbreakSignals() and terminates the process if anything fired.
 */
void cn1DetectJailbreakBypassesAndExit(void);
#endif
