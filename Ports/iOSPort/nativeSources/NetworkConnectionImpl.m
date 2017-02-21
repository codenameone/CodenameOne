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
#import "NetworkConnectionImpl.h"
#import <UIKit/UIKit.h>
#include "xmlvm.h"
#include "CodenameOne_GLViewController.h"
#include "com_codename1_impl_ios_IOSImplementation.h"

extern int isIOS8();
extern NSString* fixFilePath(NSString* ns);

int connections = 0;
@implementation NetworkConnectionImpl

- (id)init
{
    self = [super init];
    if (self) {
        chunkedStreamingLen = -1;
        contentLength = -1;
        request = nil;
        allHeaderFields = nil;
        connection = nil;
    }
    
    return self;
}

- (void*)openConnection:(NSString*)url timeout:(int)timeout {
    [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
    connections++;
    float time = ((float)timeout) / 1000.0;
    
    // workaround for exception where the | character is considered to be illegal by apple but is required by facebook
    url = [url stringByReplacingOccurrencesOfString:@"|" withString:@"%7C"];
    request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:url]
                                              cachePolicy:NSURLRequestReloadIgnoringLocalCacheData
                                              timeoutInterval:time];
#ifndef CN1_USE_ARC
    [request retain];
#endif
    return (BRIDGE_CAST void*)self;
}

- (void)connect {
    dispatch_sync(dispatch_get_main_queue(), ^{
         connection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:YES];
    });
}

-(NSCachedURLResponse*)connection:(NSURLConnection*)connection willCacheResponse:(NSCachedURLResponse *)cachedResponse {
    return nil;
}

- (void)setMethod:(NSString*)mtd {
    [request setHTTPMethod:mtd];
}

-(void)setChunkedStreamingLen:(int)len {
    chunkedStreamingLen = len;
    if (!isIOS8() && len > -1) {
        NSLog(@"Attempt to set chunked streaming mode detected.  Chunked streaming mode is only supported in iOS 8 and higher");
    }
}

- (int)getResponseCode {
    return responseCode;
}

- (NSString*)getResponseMessage {
    return [NSHTTPURLResponse localizedStringForStatusCode:responseCode];
}

- (int)getContentLength {
    return contentLength;
}

- (NSString*)getResponseHeader:(NSString*)name {
    return [allHeaderFields objectForKey:name];
}

- (void)addHeader:(NSString*)key value:(NSString*)value {
    [request addValue:value forHTTPHeaderField:key];
}

- (void)setBody:(void*)body size:(int)size {
    [request setHTTPBody:[NSData dataWithBytes:body length:size]];
}

-(void)setBody:(NSString*)file {
#ifdef __IPHONE_8_0
    if (isIOS8() && chunkedStreamingLen > -1) {
        NSInputStream * input = [NSInputStream inputStreamWithFileAtPath:fixFilePath(file)];
        [request setHTTPBodyStream: input];
     } else {
         NSData* d = [[NSFileManager defaultManager] contentsAtPath:fixFilePath(file)];
         [request setHTTPBody: d];
     }
#else
    NSData* d = [[NSFileManager defaultManager] contentsAtPath:fixFilePath(file)];
    [request setHTTPBody: d];
#endif
}


- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
    contentLength = [response expectedContentLength];
    NSHTTPURLResponse* urlRes = (NSHTTPURLResponse*)response;
    responseCode = [urlRes statusCode];
    allHeaderFields = [urlRes allHeaderFields];
#ifndef CN1_USE_ARC
    [allHeaderFields retain];
#endif
}

- (NSURLRequest *)connection:(NSURLConnection *)connection
             willSendRequest:(NSURLRequest *)_request
            redirectResponse:(NSHTTPURLResponse *)response {
    if (response.statusCode >= 300 && response.statusCode < 400) {
        return nil;
    }
    return _request;
}

extern void connectionComplete(void* peer);

extern void connectionReceivedData(void* peer, NSData* data);

extern void connectionError(void* peer, NSString* message);

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error {
    connectionError((BRIDGE_CAST void*)self, [error localizedDescription]);
    connections--;
    if(connections < 1) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
    }
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
    connectionReceivedData((BRIDGE_CAST void*)self, data);
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
    connectionComplete((BRIDGE_CAST void*)self);
    connections--;
    if(connections < 1) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
    }
}

- (int)getResponseHeaderCount {
    return [allHeaderFields count];
}

- (NSString*)getResponseHeaderName:(int)offset {
    NSEnumerator* n = [allHeaderFields keyEnumerator];
    return [[n allObjects] objectAtIndex:offset];
}


#ifndef CN1_USE_ARC
-(void)dealloc {
    if(allHeaderFields != nil) {
        [allHeaderFields release];
    }
    if(connection != nil) {
        [connection release];
    }
    [request release];
	[super dealloc];
}
#endif


@end
