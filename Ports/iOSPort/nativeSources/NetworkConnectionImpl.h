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

@interface NetworkConnectionImpl : NSObject {
    NSMutableURLRequest *request;
    int contentLength;
    int responseCode;
    NSDictionary* allHeaderFields;
    NSURLConnection *connection;
}

- (void*)openConnection:(NSString*)url timeout:(int)timeout;
- (void)connect;
- (void)setMethod:(NSString*)mtd;
- (int)getResponseCode;
- (NSString*)getResponseMessage;
- (int)getContentLength;
- (NSString*)getResponseHeader:(NSString*)name;
- (void)addHeader:(NSString*)key value:(NSString*)value;
- (void)setBody:(void*)body size:(int)size;
- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response;
- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error;
- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data;
- (void)connectionDidFinishLoading:(NSURLConnection *)connection;
- (int)getResponseHeaderCount;
- (NSString*)getResponseHeaderName:(int)offset;

@end
