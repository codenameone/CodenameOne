/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

#import "CN1WatchConnectivity.h"

#if defined(CN1_USE_WATCHCONNECTIVITY) && !TARGET_OS_TV && !TARGET_OS_MACCATALYST

// Keys inside the dictionaries WCSession carries. WCSession moves property lists, and the Java
// payload is opaque bytes, so every transfer is a two-entry dictionary: the path it is addressed to
// and the bytes themselves.
static NSString *const kPathKey = @"cn1.path";
static NSString *const kBodyKey = @"cn1.body";
static NSString *const kTokenKey = @"cn1.token";
static NSString *const kReplyKey = @"cn1.reply";
/// Reserved key holding a path-to-sequence map inside the application context, so two halves that
/// publish the same path can be ordered. Never a caller's path: it is filtered out of dataPaths and
/// of received-context delivery.
static NSString *const kSeqKey = @"cn1.seq";

/// A monotonic publication stamp. Wall-clock millis order correctly against the peer's stamps (both
/// devices are time-synced far more tightly than a context replication takes), and the counter
/// breaks ties between two publishes inside the same millisecond on this device.
static int64_t cn1WearableNextSequence(void) {
    static int64_t last = 0;
    static dispatch_once_t once;
    static NSLock *lock = nil;
    dispatch_once(&once, ^{
        lock = [[NSLock alloc] init];
    });
    [lock lock];
    int64_t now = (int64_t) ([[NSDate date] timeIntervalSince1970] * 1000.0);
    last = now > last ? now : last + 1;
    int64_t result = last;
    [lock unlock];
    return result;
}

/// The stamp a context recorded for a path, or 0 when it predates stamping.
static int64_t cn1WearableSequenceFor(NSDictionary *ctx, NSString *path) {
    id seq = ctx[kSeqKey];
    if (![seq isKindOfClass:[NSDictionary class]]) {
        return 0;
    }
    id value = ((NSDictionary *) seq)[path];
    return [value isKindOfClass:[NSNumber class]] ? [value longLongValue] : 0;
}


/// Builds the WearableMessage wire form for a received file: a two-entry payload carrying "name"
/// (string) and "contents" (bytes). Mirrors com.codename1.wearable.WearableMessage#toByteArray, so
/// the shapes have to stay in step -- see FORMAT_VERSION there.
static NSData *cn1WearableWrapFile(NSString *name, NSData *contents) {
    const uint8_t kFormatVersion = 1;
    const uint8_t kTypeString = 1;
    const uint8_t kTypeBytes = 6;
    NSMutableData *out = [NSMutableData data];
    [out appendBytes:&kFormatVersion length:1];
    uint16_t count = CFSwapInt16HostToBig(2);
    [out appendBytes:&count length:2];

    NSData *nameKey = [@"name" dataUsingEncoding:NSUTF8StringEncoding];
    NSData *nameVal = [(name == nil ? @"file" : name) dataUsingEncoding:NSUTF8StringEncoding];
    NSData *bodyKey = [@"contents" dataUsingEncoding:NSUTF8StringEncoding];

    uint32_t len = CFSwapInt32HostToBig((uint32_t) nameKey.length);
    [out appendBytes:&len length:4];
    [out appendData:nameKey];
    [out appendBytes:&kTypeString length:1];
    len = CFSwapInt32HostToBig((uint32_t) nameVal.length);
    [out appendBytes:&len length:4];
    [out appendData:nameVal];

    len = CFSwapInt32HostToBig((uint32_t) bodyKey.length);
    [out appendBytes:&len length:4];
    [out appendData:bodyKey];
    [out appendBytes:&kTypeBytes length:1];
    len = CFSwapInt32HostToBig((uint32_t) contents.length);
    [out appendBytes:&len length:4];
    [out appendData:contents];
    return out;
}

@implementation CN1WatchConnectivity {
    // Reply blocks for messages the peer sent us that expect an answer. The Java side answers
    // asynchronously on the EDT, so the block has to outlive the delegate callback.
    NSMutableDictionary<NSNumber *, void (^)(NSDictionary<NSString *, id> *)> *_pendingReplies;
    int _nextInboundToken;
    /// Keys the peer's last context carried, so a key that vanishes is reported as a removal.
    NSSet<NSString *> *_lastReceivedKeys;
}

+ (CN1WatchConnectivity *)shared {
    static CN1WatchConnectivity *instance = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        instance = [[CN1WatchConnectivity alloc] init];
        [instance activate];
    });
    return instance;
}

- (instancetype)init {
    self = [super init];
    if (self != nil) {
        _pendingReplies = [[NSMutableDictionary alloc] init];
        _nextInboundToken = 1;
        _lastReceivedKeys = [[NSSet alloc] init];
    }
    return self;
}

- (void)activate {
    if ([WCSession isSupported]) {
        WCSession *s = [WCSession defaultSession];
        s.delegate = self;
        [s activate];
    }
}

- (WCSession *)session {
    return [WCSession isSupported] ? [WCSession defaultSession] : nil;
}

// --- state ---------------------------------------------------------------

- (BOOL)isSupported {
    return [WCSession isSupported];
}

- (BOOL)isPaired {
#if TARGET_OS_WATCH
    // The watch always has a phone; there is no isPaired on this side.
    return [WCSession isSupported];
#else
    WCSession *s = [self session];
    return s != nil && s.isPaired;
#endif
}

- (BOOL)isReachable {
    WCSession *s = [self session];
    return s != nil && s.reachable;
}

- (BOOL)isCompanionInstalled {
    WCSession *s = [self session];
    if (s == nil) {
        return NO;
    }
#if TARGET_OS_WATCH
    return s.isCompanionAppInstalled;
#else
    return s.isWatchAppInstalled;
#endif
}

// --- messages ------------------------------------------------------------

- (void)sendMessage:(NSString *)path payload:(NSData *)payload replyToken:(int)replyToken {
    WCSession *s = [self session];
    if (s == nil || !s.reachable) {
        if (replyToken != 0) {
            cn1_wearable_deliverReply(replyToken, NULL, 0, "The peer app is not reachable");
        }
        return;
    }
    NSDictionary *msg = @{kPathKey: (path == nil ? @"" : path),
                          kBodyKey: (payload == nil ? [NSData data] : payload)};
    if (replyToken == 0) {
        [s sendMessage:msg replyHandler:nil errorHandler:^(NSError *error) {
            // Nothing to report: the sender asked for no answer, so a failure here is the same
            // "dropped because unreachable" the API documents.
        }];
        return;
    }
    [s sendMessage:msg replyHandler:^(NSDictionary<NSString *, id> *reply) {
        NSData *body = reply[kReplyKey];
        cn1_wearable_deliverReply(replyToken, body.bytes, (int) body.length, NULL);
    } errorHandler:^(NSError *error) {
        cn1_wearable_deliverReply(replyToken, NULL, 0,
                error.localizedDescription.UTF8String);
    }];
}

- (void)sendReply:(int)replyToken payload:(NSData *)payload {
    void (^handler)(NSDictionary<NSString *, id> *);
    @synchronized (_pendingReplies) {
        NSNumber *key = @(replyToken);
        // ARC is off in this port, so the dictionary's reference is the only one keeping the block
        // alive: retain before removing, or the block is deallocated before it is called.
        handler = [_pendingReplies[key] retain];
        [_pendingReplies removeObjectForKey:key];
    }
    if (handler != nil) {
        handler(@{kReplyKey: (payload == nil ? [NSData data] : payload)});
        [handler release];
    }
}

// --- replicated data -----------------------------------------------------

// Replicated data is the session's application context: one dictionary that survives both apps
// being killed and is handed to the peer whenever it next runs. Each CN1 path is one entry, so
// publishing a path replaces only that path.

- (void)putData:(NSString *)path payload:(NSData *)payload {
    WCSession *s = [self session];
    if (s == nil || path == nil) {
        return;
    }
    NSMutableDictionary *ctx = [[s applicationContext] mutableCopy];
    if (ctx == nil) {
        ctx = [[NSMutableDictionary alloc] init];
    }
    ctx[path] = (payload == nil ? [NSData data] : payload);
    // Stamp the publication so a reader can tell our value from a newer one the peer sent. The
    // stamps ride in a reserved sub-dictionary of the same context, which is the only channel
    // WatchConnectivity gives us -- the context is replaced wholesale on every publish.
    NSMutableDictionary *seq = [ctx[kSeqKey] mutableCopy];
    if (seq == nil) {
        seq = [[NSMutableDictionary alloc] init];
    }
    seq[path] = @(cn1WearableNextSequence());
    ctx[kSeqKey] = seq;
    [seq release];
    NSError *err = nil;
    [s updateApplicationContext:ctx error:&err];
    if (err != nil) {
        NSLog(@"[cn1.wearable] failed to publish %@: %@", path, err.localizedDescription);
    }
    [ctx release];
}

- (NSData *)getData:(NSString *)path {
    WCSession *s = [self session];
    if (s == nil || path == nil) {
        return nil;
    }
    // Our own published values live in applicationContext; values the peer published arrive in
    // receivedApplicationContext. Both halves may publish the same path, so "whichever exists" is
    // not enough: preferring ours unconditionally would keep answering with a stale local value
    // after a newer one arrived from the peer, contradicting the single-latest-value contract. Each
    // publish stamps its path, so the two stamps decide.
    NSDictionary *localCtx = [s applicationContext];
    NSDictionary *peerCtx = [s receivedApplicationContext];
    NSData *mine = localCtx[path];
    NSData *theirs = peerCtx[path];
    if (mine == nil) {
        return theirs;
    }
    if (theirs == nil) {
        return mine;
    }
    return cn1WearableSequenceFor(localCtx, path) >= cn1WearableSequenceFor(peerCtx, path)
            ? mine : theirs;
}

- (void)removeData:(NSString *)path {
    WCSession *s = [self session];
    if (s == nil || path == nil) {
        return;
    }
    NSMutableDictionary *ctx = [[s applicationContext] mutableCopy];
    if (ctx == nil) {
        return;
    }
    if (ctx[path] == nil) {
        [ctx release];
        return;
    }
    [ctx removeObjectForKey:path];
    // Drop the path's stamp with it, or the map grows for the life of the app.
    NSMutableDictionary *seq = [ctx[kSeqKey] mutableCopy];
    if (seq != nil) {
        [seq removeObjectForKey:path];
        if (seq.count == 0) {
            [ctx removeObjectForKey:kSeqKey];
        } else {
            ctx[kSeqKey] = seq;
        }
        [seq release];
    }
    NSError *err = nil;
    [s updateApplicationContext:ctx error:&err];
    [ctx release];
}

- (NSArray<NSString *> *)dataPaths {
    WCSession *s = [self session];
    if (s == nil) {
        return @[];
    }
    NSMutableSet *paths = [NSMutableSet setWithArray:[s applicationContext].allKeys];
    [paths addObjectsFromArray:[s receivedApplicationContext].allKeys];
    // The stamp map is bookkeeping, not a path an app published.
    [paths removeObject:kSeqKey];
    return paths.allObjects;
}

- (void)transferFile:(NSString *)path name:(NSString *)name contents:(NSData *)contents {
    WCSession *s = [self session];
    if (s == nil || contents == nil) {
        return;
    }
    // A per-transfer directory, because the system reads the staged file asynchronously and on its
    // own schedule. Staging by name alone means two transfers of the same name -- or of the unnamed
    // default -- overwrite each other's bytes while WatchConnectivity is still reading the first,
    // corrupting one transfer or both. The directory carries the uniqueness so the file keeps the
    // caller's name, which is what the receiver reads back out of lastPathComponent.
    NSString *dir = [NSTemporaryDirectory() stringByAppendingPathComponent:
            [NSString stringWithFormat:@"cn1-wearable-%@", [[NSUUID UUID] UUIDString]]];
    NSError *dirErr = nil;
    if (![[NSFileManager defaultManager] createDirectoryAtPath:dir
                                  withIntermediateDirectories:YES
                                                   attributes:nil
                                                        error:&dirErr]) {
        NSLog(@"[cn1.wearable] could not stage a transfer directory: %@",
              dirErr.localizedDescription);
        return;
    }
    NSString *file = [dir stringByAppendingPathComponent:
            (name.length > 0 ? name : @"cn1-wearable-transfer")];
    if (![contents writeToFile:file atomically:YES]) {
        NSLog(@"[cn1.wearable] could not stage %@ for transfer", file);
        [[NSFileManager defaultManager] removeItemAtPath:dir error:nil];
        return;
    }
    [s transferFile:[NSURL fileURLWithPath:file]
           metadata:@{kPathKey: (path == nil ? @"" : path)}];
}

// --- WCSessionDelegate ---------------------------------------------------

- (void)session:(WCSession *)session
        activationDidCompleteWithState:(WCSessionActivationState)activationState
                                 error:(NSError *)error {
    cn1_wearable_notifyStateChanged();
}

#if !TARGET_OS_WATCH
- (void)sessionDidBecomeInactive:(WCSession *)session {
    cn1_wearable_notifyStateChanged();
}

- (void)sessionDidDeactivate:(WCSession *)session {
    // The user switched to a different watch. Re-activating is what keeps the link alive.
    [session activate];
    cn1_wearable_notifyStateChanged();
}

- (void)sessionWatchStateDidChange:(WCSession *)session {
    cn1_wearable_notifyStateChanged();
}
#else
- (void)sessionCompanionAppInstalledDidChange:(WCSession *)session {
    cn1_wearable_notifyStateChanged();
}
#endif

- (void)sessionReachabilityDidChange:(WCSession *)session {
    cn1_wearable_notifyStateChanged();
}

- (void)session:(WCSession *)session didReceiveMessage:(NSDictionary<NSString *, id> *)message {
    [self dispatchInbound:message reply:nil];
}

- (void)session:(WCSession *)session
        didReceiveMessage:(NSDictionary<NSString *, id> *)message
             replyHandler:(void (^)(NSDictionary<NSString *, id> *))replyHandler {
    [self dispatchInbound:message reply:replyHandler];
}

- (void)dispatchInbound:(NSDictionary<NSString *, id> *)message
                  reply:(void (^)(NSDictionary<NSString *, id> *))replyHandler {
    NSString *path = message[kPathKey];
    NSData *body = message[kBodyKey];
    int token = 0;
    if (replyHandler != nil) {
        // Park the block so the Java side can answer after it has hopped to the EDT.
        @synchronized (_pendingReplies) {
            token = _nextInboundToken++;
            // -copy returns +1 under manual reference counting and the dictionary retains it too,
            // so hand off the copy's ownership rather than leaking it.
            void (^stored)(NSDictionary<NSString *, id> *) = [replyHandler copy];
            _pendingReplies[@(token)] = stored;
            [stored release];
        }
    }
    cn1_wearable_deliverMessage(path.UTF8String, body.bytes, (int) body.length, token);
}

- (void)session:(WCSession *)session
        didReceiveApplicationContext:(NSDictionary<NSString *, id> *)applicationContext {
    // The peer replaces its whole context on every publish, so a removal shows up as a key that has
    // simply stopped being there. Report what is present, then whatever disappeared since last
    // time -- otherwise removeData on one side is invisible on the other.
    for (NSString *path in applicationContext) {
        NSData *body = applicationContext[path];
        if ([body isKindOfClass:[NSData class]]) {
            cn1_wearable_deliverDataChanged(path.UTF8String, body.bytes, (int) body.length);
        }
    }
    for (NSString *gone in _lastReceivedKeys) {
        if (applicationContext[gone] == nil) {
            cn1_wearable_deliverDataRemoved(gone.UTF8String);
        }
    }
    [_lastReceivedKeys release];
    NSMutableSet *keys = [NSMutableSet setWithArray:applicationContext.allKeys];
    // Not a path: if the peer's stamp map ever emptied we would report a removal for it.
    [keys removeObject:kSeqKey];
    _lastReceivedKeys = [keys retain];
}

- (void)session:(WCSession *)session didReceiveFile:(WCSessionFile *)file {
    // The only delivery path decodes bytes as a WearableMessage, so raw file contents would arrive
    // as a malformed payload with the name lost. Encode name+contents into one, matching what the
    // Android bridge publishes for a transfer.
    NSString *path = file.metadata[kPathKey];
    NSData *body = [NSData dataWithContentsOfURL:file.fileURL];
    if (body == nil) {
        return;
    }
    NSData *wrapped = cn1WearableWrapFile(file.fileURL.lastPathComponent, body);
    cn1_wearable_deliverDataChanged(path.UTF8String, wrapped.bytes, (int) wrapped.length);
}

@end

#endif // CN1_USE_WATCHCONNECTIVITY
