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
    request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:url]
                                              cachePolicy:NSURLRequestReloadIgnoringLocalCacheData
                                              timeoutInterval:time];
    [request retain];
    return self;
}

- (void)connect {
    dispatch_sync(dispatch_get_main_queue(), ^{
        connection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:YES];
    });
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
    [allHeaderFields retain];
}

extern void connectionComplete(void* peer);

extern void connectionReceivedData(void* peer, NSData* data);

extern void connectionError(void* peer, NSString* message);

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error {
    connectionError(self, [error localizedDescription]);
    connections--;
    if(connections < 1) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
    }
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
    connectionReceivedData(self, data);
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
    connectionComplete(self);
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


@end
