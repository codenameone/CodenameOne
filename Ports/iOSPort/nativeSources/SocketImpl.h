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

@interface SocketImpl : NSObject<NSStreamDelegate> {
    NSInputStream *inputStream;
    NSOutputStream *outputStream;
    int availableValue;
    NSString* errorMessage;
    int errorCode;
    BOOL connected;
    /// Accepted server-side sockets do raw descriptor I/O rather than going through
    /// NSStream: they are serviced on a background thread whose run loop never runs, and
    /// a scheduled CFStream would simply never deliver anything. -1 when unused.
    int rawFd;
}

-(BOOL)connect:(NSString*)host port:(int)port timeout:(int)timeout;
-(int)getAvailableInput;
-(NSString*)getErrorMessage;
+(NSString*)getIP;
-(NSData*)readFromStream;
-(void)writeToStream:(NSData*)param;
-(void)disconnect;
-(BOOL)listen:(int)param;
-(BOOL)listenLoopback:(int)param;
/// Closes the loopback listening socket for a port, bringing a thread blocked in accept
/// back out instead of leaving it waiting for a client nobody wants any more.
+(void)closeLoopbackListenerForPort:(int)port;
-(BOOL)isConnected;
-(int)getErrorCode;
-(BOOL)isSupported;

@end
