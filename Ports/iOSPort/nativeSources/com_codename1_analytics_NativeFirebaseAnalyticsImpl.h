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

// Objective-C peer for the com.codename1.analytics.NativeFirebaseAnalytics
// native interface. The Codename One iOS builder generates a bridge that
// resolves this class by name (cn1_createNativeInterfacePeer) and invokes
// these selectors; the selector keywords (logEvent:param1: etc.) match the
// builder's keyword convention (first arg is the method name, subsequent args
// are "paramN").

#import <Foundation/Foundation.h>

@interface com_codename1_analytics_NativeFirebaseAnalyticsImpl : NSObject
- (BOOL)isSupported;
- (void)logEvent:(NSString*)param0 param1:(NSString*)param1;
- (void)logScreen:(NSString*)param0;
- (void)setUserId:(NSString*)param0;
- (void)setUserProperty:(NSString*)param0 param1:(NSString*)param1;
@end
