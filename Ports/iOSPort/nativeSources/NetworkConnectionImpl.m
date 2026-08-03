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
#if !TARGET_OS_WATCH && !TARGET_OS_TV
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
#endif
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
#if TARGET_OS_WATCH || TARGET_OS_TV
    // NSURLConnection's synchronous delegate initializer is unavailable on
    // watchOS (NSURLSession is the supported API). Networking via this legacy
    // path is a no-op on the watch slice for now.
    connection = nil;
#else
    dispatch_sync(dispatch_get_main_queue(), ^{
         connection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:YES];
    });
#endif
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
    // The chain is collected and offered to Java even for an insecure request. An
    // insecure request asks us to accept a certificate the OS would reject -- a
    // self-signed development server -- and that is a decision about OS trust
    // evaluation, not a decision to stop looking. Returning here meant a host with
    // enforced pins accepted any certificate at all as long as the request happened
    // to be insecure, which is the opposite of what pinning is for.
    
    CFIndex count = SecTrustGetCertificateCount(trustRef);
    for (int i=0; i<count; i++) {
            SecCertificateRef certRef = SecTrustGetCertificateAtIndex(trustRef, i);
            if (i>0) {
                [certs appendString:@","];
            }
            // CHAIN:<n> opens each certificate's group; index 0 is the leaf. The
            // Java side keeps this and the SPKI entry out of the legacy flat list
            // so existing checkSSLCertificates overrides see unchanged data.
            [certs appendFormat:@"CHAIN:%d,", i];
            [certs appendString:@"SHA-256:"];
            [certs appendString:[self getFingerprint256:certRef]];
            [certs appendString:@",SHA1:"];
            [certs appendString:[self getFingerprint:certRef]];
            NSString* spki = [self getPublicKeyDigest:certRef];
            if (spki != nil) {
                [certs appendString:@",SPKI-SHA-256:"];
                [certs appendString:spki];
            }
        }
    // Released first, and only under manual retain/release. The delegate is entered once
    // per challenge and a connection can face several -- a redirect to another host
    // presents its own chain -- so overwriting the field leaked the previous string. The
    // rest of this file already keeps its retains behind this guard; an unconditional one
    // here does not compile under ARC at all.
#ifdef CN1_USE_ARC
    sslCertificates = [NSString stringWithString:certs];
#else
    [sslCertificates release];
    sslCertificates = [[NSString stringWithString:certs] retain];
#endif
    if (!com_codename1_io_NetworkManager_checkCertificatesNativeCallback___int_R_boolean(CN1_THREAD_GET_STATE_PASS_ARG connectionId)) {
        // Java rejected the chain -- a per-request check or a guard pin mismatch.
        // That veto applies to insecure requests too.
        [challenge.sender cancelAuthenticationChallenge:challenge];
        return;
    }
    if (insecure) {
        // Accepted despite whatever the OS thinks of the chain, which is what the
        // caller asked for by setting it insecure.
        [[challenge sender] useCredential:[NSURLCredential credentialForTrust:trustRef] forAuthenticationChallenge:challenge];
        return;
    }
    [challenge.sender performDefaultHandlingForAuthenticationChallenge:challenge];
}

-(void)setConnectionId:(JAVA_INT)connId {
    connectionId = connId;
}

- (NSString*) getFingerprint: (SecCertificateRef) cert {
    // SecCertificateCopyData follows the Copy rule, so the result is owned here.
    // The bridged cast alone leaked it on every handshake.
    CFDataRef certData = SecCertificateCopyData(cert);
    if (certData == NULL) {
        return @"";
    }
    unsigned char sha1Bytes[CC_SHA1_DIGEST_LENGTH];
    CC_SHA1(CFDataGetBytePtr(certData), (CC_LONG)CFDataGetLength(certData), sha1Bytes);
    CFRelease(certData);
    NSMutableString *fingerprint = [NSMutableString stringWithCapacity:CC_SHA1_DIGEST_LENGTH * 3];
    for (int i = 0; i < CC_SHA1_DIGEST_LENGTH; ++i) {
        [fingerprint appendFormat:@"%02x ", sha1Bytes[i]];
    }
    return [fingerprint stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
}

/**
 * Reads one DER TLV at `off`. Returns NO when the buffer is too short or the
 * length encoding is one we do not handle (indefinite length, or a length that
 * would not fit). `headerLen` is the tag plus length bytes, `totalLen` covers
 * the whole TLV.
 */
static BOOL cn1ReadDerTlv(const uint8_t* buf, NSUInteger len, NSUInteger off,
                          uint8_t* tag, NSUInteger* headerLen, NSUInteger* totalLen) {
    // off is walked forward by the caller, so an off past the end must not be able to
    // make `off + 2` wrap back into range before the comparison.
    if (off > len || len - off < 2) {
        return NO;
    }
    *tag = buf[off];
    NSUInteger n = buf[off + 1];
    if (n < 0x80) {
        *headerLen = 2;
        // n < 0x80 and len - off >= 2, so this cannot overflow; the range check below
        // is what decides whether the TLV actually fits.
        *totalLen = 2 + n;
    } else {
        NSUInteger countBytes = n & 0x7f;
        // 0x80 is indefinite length, which DER forbids; more than 4 length bytes
        // would mean a certificate larger than anything we will ever be handed.
        if (countBytes == 0 || countBytes > 4 || len - off - 2 < countBytes) {
            return NO;
        }
        NSUInteger contentLen = 0;
        for (NSUInteger i = 0; i < countBytes; i++) {
            contentLen = (contentLen << 8) | buf[off + 2 + i];
        }
        *headerLen = 2 + countBytes;
        // Checked against what is actually left rather than by forming the sum first.
        // A crafted length wraps NSUInteger, `off + *totalLen <= len` then passes, and
        // the caller hands the wrapped value to CC_SHA256, which reads past the buffer.
        // The certificate comes off the wire, so it is attacker-shaped by definition.
        if (contentLen > len - off - *headerLen) {
            return NO;
        }
        *totalLen = *headerLen + contentLen;
    }
    // Against what is left, not by forming off + *totalLen. That sum can wrap
    // NSUInteger for a large off, and a wrapped sum compares small -- so the check
    // that exists to stop an overread would be the thing that let it through. The
    // subtraction cannot wrap: off <= len is this function's precondition and is
    // re-established on the short-form path above.
    return off <= len && *totalLen <= len - off;
}

/**
 * Base64 SHA-256 over the certificate's SubjectPublicKeyInfo, which is what a
 * public-key pin is computed over.
 *
 * Walks the certificate DER rather than going through SecCertificateCopyKey +
 * SecKeyCopyExternalRepresentation. That pair hands back the *raw* key, so
 * reconstructing the SPKI means prepending a hand-maintained ASN.1 header chosen
 * per key type and size -- a table that silently produces wrong digests for any
 * key type it does not know about. The DER walk is algorithm-agnostic and
 * matches `openssl x509 -pubkey | openssl pkey -pubin -outform der` exactly.
 *
 * Returns nil if the structure is not what we expect, in which case the caller
 * simply omits the entry and pinning falls back to whole-certificate digests.
 */
- (NSString*) getPublicKeyDigest: (SecCertificateRef) cert {
    // Plain CoreFoundation rather than a toll-free bridge cast: this file builds
    // both with and without ARC, and the correct bridging annotation differs
    // between the two. An explicit CFRelease is unambiguous in either mode.
    CFDataRef certData = SecCertificateCopyData(cert);
    if (certData == NULL) {
        return nil;
    }
    const uint8_t* buf = CFDataGetBytePtr(certData);
    NSUInteger len = (NSUInteger) CFDataGetLength(certData);
    NSString* result = nil;
    uint8_t tag;
    NSUInteger headerLen, totalLen;
    NSUInteger off;
    int i;

    // Certificate ::= SEQUENCE { tbsCertificate, signatureAlgorithm, signature }
    if (!cn1ReadDerTlv(buf, len, 0, &tag, &headerLen, &totalLen) || tag != 0x30) {
        goto cleanup;
    }
    off = headerLen;

    // tbsCertificate ::= SEQUENCE { ... }
    if (!cn1ReadDerTlv(buf, len, off, &tag, &headerLen, &totalLen) || tag != 0x30) {
        goto cleanup;
    }
    off += headerLen;

    // [0] EXPLICIT Version is optional and absent in a v1 certificate.
    if (!cn1ReadDerTlv(buf, len, off, &tag, &headerLen, &totalLen)) {
        goto cleanup;
    }
    if (tag == 0xA0) {
        off += totalLen;
    }

    // Skip serialNumber, signature, issuer, validity, subject. The next element
    // is subjectPublicKeyInfo.
    for (i = 0; i < 5; i++) {
        if (!cn1ReadDerTlv(buf, len, off, &tag, &headerLen, &totalLen)) {
            goto cleanup;
        }
        off += totalLen;
    }

    if (cn1ReadDerTlv(buf, len, off, &tag, &headerLen, &totalLen) && tag == 0x30) {
        uint8_t digest[CC_SHA256_DIGEST_LENGTH];
        CC_SHA256(buf + off, (CC_LONG) totalLen, digest);
        NSData* digestData = [NSData dataWithBytes:digest length:CC_SHA256_DIGEST_LENGTH];
        result = [digestData base64EncodedStringWithOptions:0];
    }

cleanup:
    CFRelease(certData);
    return result;
}

- (NSString*) getFingerprint256: (SecCertificateRef) cert {
    // Same ownership rule as getFingerprint: this was leaking one certificate's
    // worth of data per digest, on every connection.
    CFDataRef keyData = SecCertificateCopyData(cert);
    if (keyData == NULL) {
        return @"";
    }
    uint8_t digest[CC_SHA256_DIGEST_LENGTH]={0};
    CC_SHA256(CFDataGetBytePtr(keyData), (CC_LONG)CFDataGetLength(keyData), digest);
    CFRelease(keyData);
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
#if !TARGET_OS_WATCH && !TARGET_OS_TV
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
#endif
    }
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
    connectionReceivedData((BRIDGE_CAST void*)self, data);
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
    connectionComplete((BRIDGE_CAST void*)self);
    connections--;
    if(connections < 1) {
#if !TARGET_OS_WATCH && !TARGET_OS_TV
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
#endif
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
