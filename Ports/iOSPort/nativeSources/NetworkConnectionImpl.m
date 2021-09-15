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
#import "com_codename1_io_NetworkManager.h"
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
        sslCertificates = nil;
        pendingDataPos = 0;
        pendingData = nil;
        insecure = NO;
    }
    
    return self;
}

- (void*)openConnection:(NSString*)url timeout:(int)timeout {
    dispatch_async(dispatch_get_main_queue(), ^{
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
    });
    connections++;
    float time = ((float)timeout) / 1000.0;
    
    // workaround for exception where the | character is considered to be illegal by apple but is required by facebook
    url = [url stringByReplacingOccurrencesOfString:@"|" withString:@"%7C"];
    request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:url]
                                              cachePolicy:NSURLRequestReloadIgnoringLocalCacheData
                                              timeoutInterval:time];
    request.HTTPShouldHandleCookies = NO;
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

-(void)setInsecure:(BOOL)ins {
    insecure = ins;
}

- (void)setMethod:(NSString*)mtd {
    [request setHTTPMethod:mtd];
}

-(void)setChunkedStreamingLen:(int)len {
    chunkedStreamingLen = len;
    if (!isIOS8() && len > -1) {
        CN1Log(@"Attempt to set chunked streaming mode detected.  Chunked streaming mode is only supported in iOS 8 and higher");
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
    [request setValue:value forHTTPHeaderField:key];
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

- (JAVA_OBJECT) getSSLCertificates {
    if (sslCertificates == nil) {
        return JAVA_NULL;
    }
    return fromNSString(getThreadLocalData(), sslCertificates);
}

//- (void) connection: (NSURLConnection*)connection willSendRequestForAuthenticationChallenge: (NSURLAuthenticationChallenge*)challenge {
-(void) connection: (NSURLConnection*)connection willSendRequestForAuthenticationChallenge:(nonnull NSURLAuthenticationChallenge *)challenge {
    SecTrustRef trustRef = [[challenge protectionSpace] serverTrust];
    SecTrustEvaluate(trustRef, NULL);
    NSMutableString* certs = [NSMutableString string];
    if (insecure) {
        [[challenge sender] useCredential:[NSURLCredential credentialForTrust:[[challenge protectionSpace] serverTrust]] forAuthenticationChallenge:challenge];
        return;
    }
    //[connection cancel];
    
    CFIndex count = SecTrustGetCertificateCount(trustRef);
    for (int i=0; i<count; i++) {
            SecCertificateRef certRef = SecTrustGetCertificateAtIndex(trustRef, i);
            if (i>0) {
                [certs appendString:@","];
            }
            [certs appendString:@"SHA-256:"];
            [certs appendString:[self getFingerprint256:certRef]];
            [certs appendString:@",SHA1:"];
            [certs appendString:[self getFingerprint:certRef]];

        }
    sslCertificates = [[NSString stringWithString:certs] retain];
    if (com_codename1_io_NetworkManager_checkCertificatesNativeCallback___int_R_boolean(CN1_THREAD_GET_STATE_PASS_ARG connectionId)) {
        [challenge.sender performDefaultHandlingForAuthenticationChallenge:challenge];
    } else {
        [challenge.sender cancelAuthenticationChallenge:challenge];
    }
}

-(void)setConnectionId:(JAVA_INT)connId {
    connectionId = connId;
}

- (NSString*) getFingerprint: (SecCertificateRef) cert {
    NSData* certData = (__bridge NSData*) SecCertificateCopyData(cert);
    unsigned char sha1Bytes[CC_SHA1_DIGEST_LENGTH];
    CC_SHA1(certData.bytes, (int)certData.length, sha1Bytes);
    NSMutableString *fingerprint = [NSMutableString stringWithCapacity:CC_SHA1_DIGEST_LENGTH * 3];
    for (int i = 0; i < CC_SHA1_DIGEST_LENGTH; ++i) {
        [fingerprint appendFormat:@"%02x ", sha1Bytes[i]];
    }
    return [fingerprint stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
}

- (NSString*) getFingerprint256: (SecCertificateRef) cert {
    NSData* keyData = (__bridge NSData*) SecCertificateCopyData(cert);

    uint8_t digest[CC_SHA256_DIGEST_LENGTH]={0};
    CC_SHA256(keyData.bytes, keyData.length, digest);
    NSData *out=[NSData dataWithBytes:digest length:CC_SHA256_DIGEST_LENGTH];
    NSString *hash=[out description];
    hash = [hash stringByReplacingOccurrencesOfString:@" " withString:@""];
    hash = [hash stringByReplacingOccurrencesOfString:@"<" withString:@""];
    hash = [hash stringByReplacingOccurrencesOfString:@">" withString:@""];
    return hash;

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

- (JAVA_INT)available {
    int count = 0;
    if (pendingData == nil) {
        return 0;
    }
    for (NSData* data in pendingData) {
        count += [data length];
    }
    return count;
    
}
- (JAVA_INT)shiftByte {
    if (pendingData == nil || [pendingData count] == 0) {
        return (JAVA_INT)-1;
    }
    NSData* data = (NSData*)[pendingData firstObject];
    if (pendingDataPos >= [data length]) {
        pendingDataPos = 0;
        [pendingData removeObjectAtIndex:0];
        return [self shiftByte];
    }
    const char* fileBytes = (const char*)[data bytes];
    JAVA_INT result = fileBytes[pendingDataPos];
    
    pendingDataPos++;
    return result;
    
}
- (void)appendData:(NSData*)data {
    if (pendingData == nil) {
        pendingData = [[NSMutableArray alloc] init];
    }
    [pendingData addObject:data];
            
}
- (JAVA_INT)readData:(JAVA_OBJECT)buffer offset:(JAVA_INT)offset len:(JAVA_INT)len {
    if (pendingData == nil || [pendingData count] == 0) {
        return (JAVA_INT)0;
    }
    NSData* data = (NSData*)[pendingData firstObject];
    if (pendingDataPos >= [data length]) {
        pendingDataPos = 0;
        [pendingData removeObjectAtIndex:0];
        return [self readData:buffer offset:offset len:len];
    }
    int count = 0;
    int toFill = len;
    
    while (toFill > 0) {
        if ([data length] - pendingDataPos >= toFill) {
            [data getBytes:((JAVA_ARRAY)buffer)->data+offset+count range:NSMakeRange(pendingDataPos, toFill)];
            count += toFill;
            pendingDataPos += toFill;
            toFill = 0;
            return count;
        } else {
            [data getBytes:((JAVA_ARRAY)buffer)->data+offset+count range:NSMakeRange(pendingDataPos, [data length] - pendingDataPos)];
            count += ([data length] - pendingDataPos);
            
            toFill -= ([data length]- pendingDataPos);
            pendingDataPos = 0;
            [pendingData removeObjectAtIndex:0];
            if ([pendingData count] == 0) {
                return count;
            } else {
                data = (NSData*)[pendingData firstObject];
            }
        }
    }
    return count;
}


#ifndef CN1_USE_ARC
-(void)dealloc {
    if(allHeaderFields != nil) {
        [allHeaderFields release];
        allHeaderFields = nil;
    }
    if(connection != nil) {
        [connection release];
        connection = nil;
    }
    if (sslCertificates != nil) {
       [sslCertificates release];
        sslCertificates = nil;
    }
    if (pendingData != nil) {
        [pendingData release];
        pendingData = nil;
    }
    if (request != nil) {
        [request release];
        request = nil;
    }
	[super dealloc];
}
#endif


@end
