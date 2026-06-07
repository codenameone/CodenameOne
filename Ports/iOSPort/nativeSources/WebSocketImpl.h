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

/// Objective-C wrapper around NSURLSessionWebSocketTask used by
/// com.codename1.impl.ios.IOSWebSocketImpl. NSURLSessionWebSocketTask is
/// iOS 13+, which matches the deployment-target floor in
/// IPhoneBuilder.DEFAULT_MIN_DEPLOYMENT_VERSION.
API_AVAILABLE(ios(13.0))
@interface CN1WebSocketImpl : NSObject<NSURLSessionWebSocketDelegate> {
    int connectionId;
    NSURL* url;
    NSURLSession* session;
    NSURLSessionWebSocketTask* task;
    BOOL closed;
}

-(instancetype)initWithId:(int)cid url:(NSString*)urlString;
-(void)connectWithTimeoutMs:(int)timeoutMs protocols:(NSArray*)protocols;
-(void)closeConnection;
-(void)sendText:(NSString*)message;
-(void)sendBinary:(NSData*)data;

@end
