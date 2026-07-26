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
#import "SocketImpl.h"
#include <ifaddrs.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <netinet/in.h>
#include <unistd.h>
#include <arpa/inet.h>
#include "xmlvm.h"
#import "CodenameOne_GLViewController.h"

@implementation SocketImpl

-(id)init {
    self = [super init];
    if(self != nil) {
        rawFd = -1;   // stream-backed unless an accept installs a descriptor
    }
    return self;
}

static void _yield() {
#ifdef NEW_CODENAME_ONE_VM
    CN1_YIELD_THREAD;
#endif
}

static void _resume() {
#ifdef NEW_CODENAME_ONE_VM
    CN1_RESUME_THREAD;
#endif
}

-(BOOL)connect:(NSString*)host port:(int)port timeout:(int)timeout{
    CFReadStreamRef readStream;
    CFWriteStreamRef writeStream;
    CFStreamCreatePairWithSocketToHost(NULL, (BRIDGE_CAST CFStringRef)host, port, &readStream, &writeStream);
    inputStream = (BRIDGE_CAST NSInputStream *)readStream;
    outputStream = (BRIDGE_CAST NSOutputStream *)writeStream;
    [inputStream setDelegate:self];
    [outputStream setDelegate:self];
    [inputStream scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
    [outputStream scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
    SocketImpl* _self = self;
    if (timeout > 0) {
         dispatch_async(dispatch_get_main_queue(), ^{
            NSTimer *timer = [NSTimer scheduledTimerWithTimeInterval:timeout/1000.0
                    target:[NSBlockOperation blockOperationWithBlock:^{
                        if (!connected && inputStream != NULL) {
                            [_self disconnect];
                            errorMessage = @"Connect timeout";
                            errorCode = -1;
                        }
                    }]
                    selector:@selector(main)
                    userInfo:nil
                    repeats:NO
            ];
         });
        
    }
    if (inputStream != nil) {
        [inputStream open];
    }
    if (outputStream != nil) {
        [outputStream open];
    }
    while (outputStream != nil && [outputStream streamStatus] == NSStreamStatusOpening) {
        _yield();
        usleep(100000);
        _resume();
    }
    while (inputStream != nil && [inputStream streamStatus] == NSStreamStatusOpening) {
        _yield();
        usleep(100000);
        _resume();
    }
    if (outputStream == nil || inputStream == nil || [self isInputShutdown] || [self isOutputShutdown]) {
        connected = NO;
    } else {
        connected = YES;
    }
    return connected;
}

-(BOOL)isInputShutdown{
    errorMessage = NULL;
    NSStreamStatus status = [inputStream streamStatus];
    return (status == NSStreamStatusOpening || status == NSStreamStatusNotOpen || NSStreamStatusClosed == status || status == NSStreamStatusError);
}

-(BOOL)isOutputShutdown{
    errorMessage = NULL;
    NSStreamStatus status = [outputStream streamStatus];
    return (status == NSStreamStatusOpening ||  status == NSStreamStatusNotOpen || NSStreamStatusClosed == status || status == NSStreamStatusError);
}

-(int)getAvailableInput{
    if(rawFd >= 0) {
        int pending = 0;
        if(ioctl(rawFd, FIONREAD, &pending) < 0) {
            return 0;
        }
        return pending;
    }
    //return availableValue;
    return [inputStream hasBytesAvailable] ? 1 : 0;
}

-(NSString*)getErrorMessage{
    return errorMessage;
}

+(NSString*)getIP{
  NSString *address = @"error";
  struct ifaddrs *interfaces = NULL;
  struct ifaddrs *temp_addr = NULL;
  int success = 0;

  // retrieve the current interfaces - returns 0 on success
  success = getifaddrs(&interfaces);
  if (success == 0) {
    // Loop through linked list of interfaces
    temp_addr = interfaces;
    while (temp_addr != NULL) {
      if( temp_addr->ifa_addr->sa_family == AF_INET) {
        // Check if interface is en0 which is the wifi connection on the iPhone
        if ([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:@"en0"]) {
          // Get NSString from C String
          address = [NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)];
        }
      }

      temp_addr = temp_addr->ifa_next;
    }
  }

  // Free memory
  freeifaddrs(interfaces);

  return address;
}

-(NSData*)readFromStream{
    uint8_t buffer[8192];
    int len;

    if(rawFd >= 0) {
        // Blocking recv: the caller is a reader thread and expects to wait for data,
        // and returning nil in a spin would burn the CPU.
        _yield();
        ssize_t r = recv(rawFd, buffer, sizeof(buffer), 0);
        _resume();
        if(r > 0) {
            return [NSData dataWithBytes:buffer length:r];
        }
        // End of stream or a read error: close here rather than only marking the socket
        // disconnected. The Java side closes a stream by calling disconnectSocket only
        // while the socket still reports itself connected, so leaving the descriptor open
        // once connected is NO would strand it for the life of the process.
        [self closeRawDescriptor];
        return nil;
    }

    if([inputStream hasBytesAvailable]) {
        len = [inputStream read:buffer maxLength:sizeof(buffer)];
        if (len > 0) {
            return [NSData dataWithBytes:buffer length:len];
        }
    }
    return nil;
}

-(void)writeToStream:(NSData*)param{
    if(rawFd >= 0) {
        const uint8_t* bytes = (const uint8_t*)[param bytes];
        size_t remaining = [param length];
        while(remaining > 0) {
            // send() blocks once the send buffer fills, so hand the thread back to the VM
            // across it exactly as the accept and recv paths do; holding it would stall
            // cooperative scheduling for as long as the peer is slow to drain.
            _yield();
            ssize_t w = send(rawFd, bytes, remaining, 0);
            _resume();
            if(w <= 0) {
                [self closeRawDescriptor];
                return;
            }
            bytes += w;
            remaining -= w;
        }
        return;
    }
    [outputStream write:[param bytes] maxLength:[param length]];
}

/// Closes the accepted descriptor once, and records the socket as disconnected. Callers
/// reach this from either direction of the connection failing.
-(void)closeRawDescriptor{
    if(rawFd >= 0) {
        close(rawFd);
        rawFd = -1;
    }
    connected = NO;
}

-(void)disconnect{
    if(rawFd >= 0) {
        [self closeRawDescriptor];
        return;
    }
    if(inputStream != nil) {
        [outputStream close];
        [outputStream removeFromRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
        [inputStream close];
        [inputStream removeFromRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
        [outputStream release];
        [inputStream release];
        inputStream = nil;
        outputStream = nil;
    }
}

-(BOOL)listen:(int)param{
    // Wildcard-bound server sockets remain unsupported on iOS; only the loopback
    // variant below is offered, so an app cannot accidentally publish a listening
    // port to the network.
    return NO;
}

/// Listening sockets bound to loopback, keyed by port, kept across accepts so a caller
/// can accept a sequence of connections the way the desktop and Android ports do.
static NSMutableDictionary* _cn1LoopbackListeners = nil;

+(int)loopbackListenerForPort:(int)port errorOut:(NSString**)errorOut {
    @synchronized([SocketImpl class]) {
        if(_cn1LoopbackListeners == nil) {
            _cn1LoopbackListeners = [[NSMutableDictionary alloc] init];
        }
        NSNumber* key = [NSNumber numberWithInt:port];
        NSNumber* existing = [_cn1LoopbackListeners objectForKey:key];
        if(existing != nil) {
            return [existing intValue];
        }
        int fd = socket(AF_INET, SOCK_STREAM, 0);
        if(fd < 0) {
            *errorOut = @"Could not create a socket";
            return -1;
        }
        int yes = 1;
        setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(yes));
        struct sockaddr_in addr;
        memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_port = htons((uint16_t)port);
        // INADDR_LOOPBACK, never INADDR_ANY: this channel must not be reachable from
        // the network, only from the device itself.
        addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
        if(bind(fd, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
            *errorOut = [NSString stringWithFormat:@"Could not bind port %i on loopback", port];
            close(fd);
            return -1;
        }
        if(listen(fd, 50) < 0) {
            *errorOut = [NSString stringWithFormat:@"Could not listen on port %i", port];
            close(fd);
            return -1;
        }
        [_cn1LoopbackListeners setObject:[NSNumber numberWithInt:fd] forKey:key];
        return fd;
    }
}

/// Closes the listening socket for a port and forgets it, so a thread blocked in accept
/// returns instead of waiting for a client that the caller no longer wants, and a later
/// listener on the same port binds a fresh descriptor.
+(void)closeLoopbackListenerForPort:(int)port {
    @synchronized([SocketImpl class]) {
        if(_cn1LoopbackListeners == nil) {
            return;
        }
        NSNumber* key = [NSNumber numberWithInt:port];
        NSNumber* existing = [_cn1LoopbackListeners objectForKey:key];
        if(existing != nil) {
            close([existing intValue]);
            [_cn1LoopbackListeners removeObjectForKey:key];
        }
    }
}

-(BOOL)listenLoopback:(int)param{
    NSString* bindError = nil;
    int listenFd = [SocketImpl loopbackListenerForPort:param errorOut:&bindError];
    if(listenFd < 0) {
        errorMessage = bindError;
        errorCode = -1;
        connected = NO;
        return NO;
    }

    // accept() blocks; hand the VM back this thread while it does.
    int clientFd;
    _yield();
    clientFd = accept(listenFd, NULL, NULL);
    _resume();
    if(clientFd < 0) {
        errorMessage = @"Accept failed";
        errorCode = -1;
        connected = NO;
        return NO;
    }

    // Serve this socket with raw descriptor I/O. An accepted connection is read on a
    // background thread whose run loop never runs, so a scheduled CFStream would open
    // but never deliver a byte.
    //
    // Writing to a peer that has gone away raises SIGPIPE by default, and this process
    // installs a SIGPIPE handler (CodenameOne_GLAppDelegate), so the ordinary case of a
    // client disconnecting mid-write would surface as a signal rather than as the EPIPE
    // the write path is written to handle. SO_NOSIGPIPE turns it into a plain error.
    int noSigPipe = 1;
    setsockopt(clientFd, SOL_SOCKET, SO_NOSIGPIPE, &noSigPipe, sizeof(noSigPipe));
    rawFd = clientFd;
    connected = YES;
    return YES;
}

-(BOOL)isConnected{
    return connected;
}

-(int)getErrorCode{
    return errorCode;
}

-(BOOL)isSupported{
    return YES;
}

- (void)stream:(NSStream *)theStream handleEvent:(NSStreamEvent)streamEvent {
	switch (streamEvent) {
            
		case NSStreamEventOpenCompleted:
            connected = YES;
			break;
            
		case NSStreamEventHasBytesAvailable:
            availableValue = 1;
			break;
            
		case NSStreamEventErrorOccurred:
            errorCode = 1;
            errorMessage = @"General Error";
            connected = NO;
			break;
            
		case NSStreamEventEndEncountered:
            [theStream close];
            [theStream removeFromRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
 			break;
            
		default:
			CN1Log(@"Unknown event");
	}
}

@end
