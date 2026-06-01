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
#import "WebSocketImpl.h"
#import "xmlvm.h"
#import "CodenameOne_GLViewController.h"
#import "com_codename1_impl_ios_IOSWebSocketImpl.h"

extern JAVA_OBJECT nsDataToByteArr(CN1_THREAD_STATE_MULTI_ARG NSData* data);
extern JAVA_OBJECT fromNSString(CN1_THREAD_STATE_MULTI_ARG NSString* str);

// MRC: this file participates in the same manual-retain-release regime as
// the rest of Ports/iOSPort/nativeSources/*.m (see SocketImpl.m for the
// reference pattern). Do not introduce __weak / __strong annotations or
// ARC-only casts.
@implementation CN1WebSocketImpl

-(instancetype)initWithId:(int)cid url:(NSString*)urlString {
    self = [super init];
    if (self) {
        connectionId = cid;
        url = [[NSURL URLWithString:urlString] retain];
        closed = NO;
        session = nil;
        task = nil;
    }
    return self;
}

-(void)dealloc {
    [url release];
    [task release];
    [session release];
    [super dealloc];
}

-(void)connectWithTimeoutMs:(int)timeoutMs {
    if (closed || url == nil) {
        return;
    }
    NSURLSessionConfiguration* cfg = [NSURLSessionConfiguration defaultSessionConfiguration];
    if (timeoutMs > 0) {
        cfg.timeoutIntervalForRequest = ((NSTimeInterval) timeoutMs) / 1000.0;
    }
    if (@available(iOS 13.0, *)) {
        session = [[NSURLSession sessionWithConfiguration:cfg delegate:self delegateQueue:nil] retain];
        task = [[session webSocketTaskWithURL:url] retain];
        [task resume];
        [self armReceive];
    }
}

-(void)armReceive {
    if (closed || task == nil) {
        return;
    }
    if (!(@available(iOS 13.0, *))) {
        return;
    }
    CN1WebSocketImpl* capturedSelf = self;
    [task receiveMessageWithCompletionHandler:^(NSURLSessionWebSocketMessage* message, NSError* error) {
        if (error != nil) {
            [capturedSelf fireErrorWithMessage:[error localizedDescription]];
            return;
        }
        if (message != nil) {
            if (message.type == NSURLSessionWebSocketMessageTypeString) {
                NSString* text = message.string;
                if (text != nil) {
                    JAVA_OBJECT jstr = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG text);
                    com_codename1_impl_ios_IOSWebSocketImpl_fireTextMessage___int_java_lang_String(
                        CN1_THREAD_GET_STATE_PASS_ARG capturedSelf->connectionId, jstr);
                }
            } else {
                NSData* data = message.data;
                if (data != nil) {
                    JAVA_OBJECT jbytes = nsDataToByteArr(CN1_THREAD_GET_STATE_PASS_ARG data);
                    com_codename1_impl_ios_IOSWebSocketImpl_fireBinaryMessage___int_byte_1ARRAY(
                        CN1_THREAD_GET_STATE_PASS_ARG capturedSelf->connectionId, jbytes);
                }
            }
        }
        [capturedSelf armReceive];
    }];
}

-(void)closeConnection {
    if (closed) {
        return;
    }
    closed = YES;
    if (@available(iOS 13.0, *)) {
        if (task != nil) {
            [task cancelWithCloseCode:NSURLSessionWebSocketCloseCodeNormalClosure reason:nil];
        }
    }
    if (session != nil) {
        [session finishTasksAndInvalidate];
    }
}

-(void)sendText:(NSString*)message {
    if (closed || task == nil || message == nil) {
        return;
    }
    if (!(@available(iOS 13.0, *))) {
        return;
    }
    NSURLSessionWebSocketMessage* m = [[[NSURLSessionWebSocketMessage alloc] initWithString:message] autorelease];
    CN1WebSocketImpl* capturedSelf = self;
    [task sendMessage:m completionHandler:^(NSError* error) {
        if (error != nil) {
            [capturedSelf fireErrorWithMessage:[error localizedDescription]];
        }
    }];
}

-(void)sendBinary:(NSData*)data {
    if (closed || task == nil || data == nil) {
        return;
    }
    if (!(@available(iOS 13.0, *))) {
        return;
    }
    NSURLSessionWebSocketMessage* m = [[[NSURLSessionWebSocketMessage alloc] initWithData:data] autorelease];
    CN1WebSocketImpl* capturedSelf = self;
    [task sendMessage:m completionHandler:^(NSError* error) {
        if (error != nil) {
            [capturedSelf fireErrorWithMessage:[error localizedDescription]];
        }
    }];
}

-(void)fireErrorWithMessage:(NSString*)msg {
    if (closed) {
        return;
    }
    closed = YES;
    NSString* finalMsg = (msg == nil ? @"WebSocket error" : msg);
    JAVA_OBJECT jmsg = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG finalMsg);
    com_codename1_impl_ios_IOSWebSocketImpl_fireError___int_java_lang_String(
        CN1_THREAD_GET_STATE_PASS_ARG connectionId, jmsg);
    if (session != nil) {
        [session finishTasksAndInvalidate];
    }
}

#pragma mark - NSURLSessionWebSocketDelegate

-(void)URLSession:(NSURLSession*)sess webSocketTask:(NSURLSessionWebSocketTask*)t
     didOpenWithProtocol:(NSString*)protocol API_AVAILABLE(ios(13.0)) {
    com_codename1_impl_ios_IOSWebSocketImpl_fireConnect___int(
        CN1_THREAD_GET_STATE_PASS_ARG connectionId);
}

-(void)URLSession:(NSURLSession*)sess webSocketTask:(NSURLSessionWebSocketTask*)t
     didCloseWithCode:(NSURLSessionWebSocketCloseCode)closeCode reason:(NSData*)reason API_AVAILABLE(ios(13.0)) {
    if (closed) {
        return;
    }
    closed = YES;
    NSString* reasonStr = nil;
    if (reason != nil && [reason length] > 0) {
        reasonStr = [[[NSString alloc] initWithData:reason encoding:NSUTF8StringEncoding] autorelease];
    }
    if (reasonStr == nil) {
        reasonStr = @"";
    }
    JAVA_OBJECT jreason = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG reasonStr);
    com_codename1_impl_ios_IOSWebSocketImpl_fireClose___int_int_java_lang_String(
        CN1_THREAD_GET_STATE_PASS_ARG connectionId, (JAVA_INT) closeCode, jreason);
}

-(void)URLSession:(NSURLSession*)sess task:(NSURLSessionTask*)t
     didCompleteWithError:(NSError*)error {
    if (error != nil && !closed) {
        [self fireErrorWithMessage:[error localizedDescription]];
    }
}

@end
