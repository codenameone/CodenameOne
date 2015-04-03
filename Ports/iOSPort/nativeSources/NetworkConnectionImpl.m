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

int connections = 0;
@implementation NetworkConnectionImpl

- (id)init
{
    self = [super init];
    if (self) {
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
#ifdef NEW_CODENAME_ONE_VM
    request.HTTPShouldHandleCookies = com_codename1_impl_ios_IOSImplementation_isUseNativeCookiesNativeCallback___R_boolean(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
#else
    request.HTTPShouldHandleCookies = com_codename1_impl_ios_IOSImplementation_isUseNativeCookiesNativeCallback__();
#endif
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


- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
    contentLength = [response expectedContentLength];
    NSHTTPURLResponse* urlRes = (NSHTTPURLResponse*)response;
    responseCode = [urlRes statusCode];
    allHeaderFields = [urlRes allHeaderFields];
#ifndef CN1_USE_ARC
    [allHeaderFields retain];
#endif
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
