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
/// The application context is a flat dictionary shared with the peer, and it has to hold both the
/// published values and the bookkeeping that orders them. Reserving a top-level key for the
/// bookkeeping would collide with an application that publishes a path of the same name -- and since
/// values are NSData and the bookkeeping is a dictionary, that collision is an unrecognized-selector
/// crash rather than a wrong answer.
///
/// So every application path is prefixed instead. The namespaces are then disjoint by construction:
/// no caller's path can land on a metadata key, whatever it is called.
///
/// - `v.<path>` the published bytes
/// - `s.<path>` the sequence the bytes were published at
/// - `t.<path>` the sequence a removal happened at (a tombstone, with no `v.` entry)
static NSString *const kValuePrefix = @"v.";
static NSString *const kStampPrefix = @"s.";
static NSString *const kTombPrefix = @"t.";

/// A monotonic publication stamp. Wall-clock millis order correctly against the peer's stamps (both
/// devices are time-synced far more tightly than a context replication takes), the counter breaks
/// ties between two publishes inside the same millisecond on this device, and -- because wall time
/// alone is not enough when the peer runs ahead or this clock is corrected backwards -- it is also
/// raised past every stamp the peer has shown us.
///
/// Where that floor is kept so it survives a relaunch. Without persistence the counter restarts at
/// wall time on every launch and a peer that ran ahead wins all over again.
static NSString *const kCn1WearableClockKey = @"cn1.wearable.clock";

static NSLock *cn1WearableClockLock(void) {
    static dispatch_once_t once;
    static NSLock *lock = nil;
    dispatch_once(&once, ^{
        lock = [[NSLock alloc] init];
    });
    return lock;
}

/// The high-water mark: wall time, our own prior writes, AND every stamp a peer has shown us.
static int64_t cn1WearableClockFloor(void) {
    static dispatch_once_t once;
    static int64_t restored = 0;
    dispatch_once(&once, ^{
        restored = (int64_t) [[NSUserDefaults standardUserDefaults] doubleForKey:kCn1WearableClockKey];
    });
    return restored;
}

static int64_t cn1WearableLast = 0;

/// Raises the clock past a stamp we have just seen from the peer.
///
/// Without this the counter only ever observed local time and local writes, so a peer publishing
/// while its clock ran ahead -- or this device's clock being corrected backwards -- left every
/// subsequent local putData/removeData carrying a LOWER stamp than the peer's existing entry. The
/// peer's older value then keeps winning, and getData() keeps returning it, until wall time
/// catches up. This is the same Lamport rule the Android side applies in sequenceOf().
static void cn1WearableObserveSequence(int64_t seen) {
    if (seen <= 0) {
        return;
    }
    NSLock *lock = cn1WearableClockLock();
    [lock lock];
    int64_t floorValue = cn1WearableClockFloor();
    if (seen > cn1WearableLast) {
        cn1WearableLast = seen;
    }
    if (seen > floorValue) {
        // Persist so the floor outlives this process; a relaunch that forgot it would hand the
        // peer the advantage back.
        [[NSUserDefaults standardUserDefaults] setDouble:(double) seen forKey:kCn1WearableClockKey];
    }
    [lock unlock];
}

/// Serializes the whole applicationContext read-modify-write.
///
/// WCSession has no merge: updateApplicationContext REPLACES the dictionary. putData and removeData
/// therefore copy it, change one path, and write the whole thing back, and two of those running
/// concurrently both copy the SAME starting dictionary -- the second write then discards the first
/// caller's path entirely. Nothing about it is atomic, and the loss is silent: the publish
/// "succeeds" and the value simply is not there.
///
/// Both methods take this lock across copy, mutate AND update, because holding it for only part of
/// the cycle leaves exactly the same window.
static NSLock *cn1WearableContextLock(void) {
    static dispatch_once_t once;
    static NSLock *lock = nil;
    dispatch_once(&once, ^{
        lock = [[NSLock alloc] init];
    });
    return lock;
}

static int64_t cn1WearableNextSequence(void) {
    NSLock *lock = cn1WearableClockLock();
    [lock lock];
    int64_t now = (int64_t) ([[NSDate date] timeIntervalSince1970] * 1000.0);
    int64_t floorValue = cn1WearableClockFloor();
    if (floorValue > cn1WearableLast) {
        cn1WearableLast = floorValue;
    }
    cn1WearableLast = now > cn1WearableLast ? now : cn1WearableLast + 1;
    int64_t result = cn1WearableLast;
    [[NSUserDefaults standardUserDefaults] setDouble:(double) result forKey:kCn1WearableClockKey];
    [lock unlock];
    return result;
}

static NSString *cn1WearableValueKey(NSString *path) {
    return [kValuePrefix stringByAppendingString:path];
}

static NSString *cn1WearableStampKey(NSString *path) {
    return [kStampPrefix stringByAppendingString:path];
}

static NSString *cn1WearableTombKey(NSString *path) {
    return [kTombPrefix stringByAppendingString:path];
}

/// One side's knowledge of a path: the bytes (nil when removed or absent), the sequence it happened
/// at, and whether the newest thing that side knows is a removal.
typedef struct {
    NSData *data;
    int64_t stamp;
    BOOL known;
    BOOL removed;
} CN1WearableEntry;

static CN1WearableEntry cn1WearableEntryFor(NSDictionary *ctx, NSString *path) {
    CN1WearableEntry e;
    e.data = nil;
    e.stamp = 0;
    e.known = NO;
    e.removed = NO;
    if (ctx == nil) {
        return e;
    }
    id value = ctx[cn1WearableValueKey(path)];
    id stamp = ctx[cn1WearableStampKey(path)];
    id tomb = ctx[cn1WearableTombKey(path)];
    if ([value isKindOfClass:[NSData class]]) {
        e.data = value;
        e.known = YES;
        e.stamp = [stamp isKindOfClass:[NSNumber class]] ? [stamp longLongValue] : 0;
        // Every stamp we read raises our clock, wherever it came from. Done here rather than in the
        // receive callback because this is the ONE place entries are parsed -- getData, dataPaths
        // and didReceiveApplicationContext all funnel through it, so no read path can forget to.
        // Reading our own entry is a no-op: it can never exceed our own counter.
        cn1WearableObserveSequence(e.stamp);
    }
    if ([tomb isKindOfClass:[NSNumber class]]) {
        int64_t t = [tomb longLongValue];
        cn1WearableObserveSequence(t);
        // A removal published after the value wins over it -- that is the whole point of keeping the
        // tombstone rather than deleting the entry outright.
        if (!e.known || t > e.stamp) {
            e.data = nil;
            e.stamp = t;
            e.known = YES;
            e.removed = YES;
        }
    }
    return e;
}

/// Which side's knowledge of a path is authoritative.
///
/// Ties are broken by ROLE, not by ownership. "Ours wins" is the one answer that cannot work here:
/// both devices run this same function, so on an equal stamp each would keep its own value, each
/// would suppress the other's callback, and the pair would sit permanently disagreeing about the
/// path with no event left to resolve it. Equal stamps are not exotic either -- two sides
/// publishing inside the same millisecond, or entries that predate stamping, both land here.
///
/// The phone wins. The rule is arbitrary but it is *stable and shared*: each side knows which half
/// it is at compile time, so both compute the same winner without exchanging anything. That is the
/// same property the Android side gets from comparing node ids in outranks().
static BOOL cn1WearableLocalWins(CN1WearableEntry mine, CN1WearableEntry theirs) {
    if (!theirs.known) {
        return YES;
    }
    if (!mine.known) {
        return NO;
    }
    if (mine.stamp != theirs.stamp) {
        return mine.stamp > theirs.stamp;
    }
#if TARGET_OS_WATCH
    return NO;
#else
    return YES;
#endif
}

/// How long a tombstone is kept before it is dropped, and the ceiling on how many are kept at all.
///
/// A tombstone has to outlive the window in which the peer might still be holding the value it
/// supersedes -- otherwise dropping it lets that older value win again and the removal undoes
/// itself. But WatchConnectivity replaces the whole context on every publish and rejects one that
/// grows too large, so an app that creates and removes changing paths would eventually be unable to
/// publish at all. A day is far longer than any plausible replication delay and keeps the context
/// bounded; the count cap is the backstop for an app that churns paths faster than that.
static const int64_t kCN1TombstoneTTLMillis = 24 * 60 * 60 * 1000LL;
static const NSUInteger kCN1MaxTombstones = 256;

/// Drops tombstones that have outlived their purpose, oldest first.
///
/// `peerCtx` is what the peer last told us it holds. A tombstone may only go once the peer has
/// stopped holding an older value for that path -- otherwise dropping it lets that value win the
/// next comparison and the removal silently undoes itself. Age alone is not evidence of that: a peer
/// that has been offline for a week still has its old value when it comes back.
static void cn1WearablePruneTombstones(NSMutableDictionary *ctx, NSDictionary *peerCtx) {
    NSMutableArray *tombKeys = [NSMutableArray array];
    for (NSString *key in ctx.allKeys) {
        if ([key isKindOfClass:[NSString class]] && [key hasPrefix:kTombPrefix]) {
            [tombKeys addObject:key];
        }
    }
    int64_t now = (int64_t) ([[NSDate date] timeIntervalSince1970] * 1000.0);
    for (NSString *key in tombKeys) {
        id stamp = ctx[key];
        if (![stamp isKindOfClass:[NSNumber class]]) {
            // Not ours, or corrupt: nothing to preserve.
            [ctx removeObjectForKey:key];
            continue;
        }
        if (now - [stamp longLongValue] <= kCN1TombstoneTTLMillis) {
            continue;
        }
        // Old enough to consider -- but only actually drop it once the peer has acknowledged the
        // removal, meaning it no longer holds a value for that path older than the tombstone.
        NSString *path = [key substringFromIndex:kTombPrefix.length];
        CN1WearableEntry theirs = cn1WearableEntryFor(peerCtx, path);
        if (theirs.known && !theirs.removed && theirs.stamp < [stamp longLongValue]) {
            continue;
        }
        [ctx removeObjectForKey:key];
    }
    if (ctx.count <= kCN1MaxTombstones) {
        return;
    }
    NSMutableArray *remaining = [NSMutableArray array];
    for (NSString *key in ctx.allKeys) {
        if ([key isKindOfClass:[NSString class]] && [key hasPrefix:kTombPrefix]) {
            [remaining addObject:key];
        }
    }
    if (remaining.count <= kCN1MaxTombstones) {
        return;
    }
    [remaining sortUsingComparator:^NSComparisonResult(NSString *a, NSString *b) {
        int64_t sa = [ctx[a] isKindOfClass:[NSNumber class]] ? [ctx[a] longLongValue] : 0;
        int64_t sb = [ctx[b] isKindOfClass:[NSNumber class]] ? [ctx[b] longLongValue] : 0;
        return sa < sb ? NSOrderedAscending : (sa > sb ? NSOrderedDescending : NSOrderedSame);
    }];
    for (NSUInteger i = 0; i + kCN1MaxTombstones < remaining.count; i++) {
        NSString *key = remaining[i];
        NSString *path = [key substringFromIndex:kTombPrefix.length];
        CN1WearableEntry theirs = cn1WearableEntryFor(peerCtx, path);
        if (theirs.known && !theirs.removed && theirs.stamp < [ctx[key] longLongValue]) {
            // Still unacknowledged. The cap is a backstop against unbounded growth, not a licence to
            // resurrect data; an app churning this many unacknowledged removals while its peer stays
            // offline keeps them until the peer catches up.
            continue;
        }
        [ctx removeObjectForKey:key];
    }
}

/// Every application path either side knows about, removals included.
static NSSet *cn1WearableAllPaths(NSDictionary *local, NSDictionary *peer) {
    NSMutableSet *out = [NSMutableSet set];
    NSArray *contexts = @[(local == nil ? @{} : local), (peer == nil ? @{} : peer)];
    for (NSDictionary *ctx in contexts) {
        for (NSString *key in ctx) {
            if (![key isKindOfClass:[NSString class]]) {
                continue;
            }
            if ([key hasPrefix:kValuePrefix] || [key hasPrefix:kTombPrefix]) {
                [out addObject:[key substringFromIndex:kValuePrefix.length]];
            }
        }
    }
    return out;
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
    NSLock *ctxLock = cn1WearableContextLock();
    [ctxLock lock];
    NSMutableDictionary *ctx = [[s applicationContext] mutableCopy];
    if (ctx == nil) {
        ctx = [[NSMutableDictionary alloc] init];
    }
    // Stamp the publication so a reader can tell our value from a newer one the peer sent, and drop
    // any tombstone: republishing a removed path brings it back.
    ctx[cn1WearableValueKey(path)] = (payload == nil ? [NSData data] : payload);
    ctx[cn1WearableStampKey(path)] = @(cn1WearableNextSequence());
    [ctx removeObjectForKey:cn1WearableTombKey(path)];
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
    // publish stamps its path, so the two stamps decide -- and a removal carries a stamp too, so it
    // can outrank the other side's older value instead of that value resurfacing.
    CN1WearableEntry mine = cn1WearableEntryFor([s applicationContext], path);
    CN1WearableEntry theirs = cn1WearableEntryFor([s receivedApplicationContext], path);
    CN1WearableEntry winner = cn1WearableLocalWins(mine, theirs) ? mine : theirs;
    return winner.removed ? nil : winner.data;
}

- (void)removeData:(NSString *)path {
    WCSession *s = [self session];
    if (s == nil || path == nil) {
        return;
    }
    NSLock *ctxLock = cn1WearableContextLock();
    [ctxLock lock];
    NSMutableDictionary *ctx = [[s applicationContext] mutableCopy];
    if (ctx == nil) {
        ctx = [[NSMutableDictionary alloc] init];
    }
    // The value goes, but a stamped tombstone stays. Deleting the entry outright would let the
    // peer's older value for the same path win the next comparison, so a removal on the newer
    // publisher would resurrect data instead of clearing it.
    [ctx removeObjectForKey:cn1WearableValueKey(path)];
    [ctx removeObjectForKey:cn1WearableStampKey(path)];
    ctx[cn1WearableTombKey(path)] = @(cn1WearableNextSequence());
    cn1WearablePruneTombstones(ctx, [s receivedApplicationContext]);
    NSError *err = nil;
    [s updateApplicationContext:ctx error:&err];
    [ctxLock unlock];
    [ctx release];
}

- (NSArray<NSString *> *)dataPaths {
    WCSession *s = [self session];
    if (s == nil) {
        return @[];
    }
    NSDictionary *localCtx = [s applicationContext];
    NSDictionary *peerCtx = [s receivedApplicationContext];
    NSMutableArray *out = [NSMutableArray array];
    for (NSString *path in cn1WearableAllPaths(localCtx, peerCtx)) {
        CN1WearableEntry mine = cn1WearableEntryFor(localCtx, path);
        CN1WearableEntry theirs = cn1WearableEntryFor(peerCtx, path);
        CN1WearableEntry winner = cn1WearableLocalWins(mine, theirs) ? mine : theirs;
        // A tombstone is a path that was removed, not a path that has a value.
        if (!winner.removed) {
            [out addObject:path];
        }
    }
    return out;
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
    // Only ever a bare file name inside our directory. A caller-supplied name is untrusted input --
    // "../../Documents/state" would otherwise let stringByAppendingPathComponent: escape the staging
    // directory and overwrite an arbitrary file in the app's sandbox, and the completion handler
    // would then delete whatever directory it landed in.
    NSString *safeName = [name lastPathComponent];
    if (safeName.length == 0 || [safeName isEqualToString:@"."]
            || [safeName isEqualToString:@".."] || [safeName hasPrefix:@"/"]) {
        safeName = @"cn1-wearable-transfer";
    }
    NSString *file = [dir stringByAppendingPathComponent:safeName];
    if (![[file stringByDeletingLastPathComponent] isEqualToString:dir]) {
        NSLog(@"[cn1.wearable] refusing a transfer name that escapes its staging directory: %@", name);
        [[NSFileManager defaultManager] removeItemAtPath:dir error:nil];
        return;
    }
    if (![contents writeToFile:file atomically:YES]) {
        NSLog(@"[cn1.wearable] could not stage %@ for transfer", file);
        [[NSFileManager defaultManager] removeItemAtPath:dir error:nil];
        return;
    }
    [s transferFile:[NSURL fileURLWithPath:file]
           metadata:@{kPathKey: (path == nil ? @"" : path)}];
}

/// Deletes a staging directory once WatchConnectivity is done with it. The system owns the file until
/// the transfer finishes, so this can only happen from the completion delegate -- and it has to
/// happen there, or every transfer leaves a full copy of its payload in the container until the OS
/// decides to purge the temporary directory.
- (void)cn1CleanupStagedTransfer:(WCSessionFileTransfer *)transfer {
    NSURL *url = transfer.file.fileURL;
    if (url == nil) {
        return;
    }
    NSString *dir = [url.path stringByDeletingLastPathComponent];
    // Only ever our own staging directories, never a caller's file.
    if ([[dir lastPathComponent] hasPrefix:@"cn1-wearable-"]) {
        [[NSFileManager defaultManager] removeItemAtPath:dir error:nil];
    }
}

// --- WCSessionDelegate ---------------------------------------------------

- (void)session:(WCSession *)session
        didFinishFileTransfer:(WCSessionFileTransfer *)fileTransfer
                        error:(NSError *)error {
    if (error != nil) {
        NSLog(@"[cn1.wearable] file transfer failed: %@", error.localizedDescription);
    }
    [self cn1CleanupStagedTransfer:fileTransfer];
}

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
    // The peer replaces its whole context on every publish. Two things follow.
    //
    // First, a path the peer removed either carries a tombstone or has simply stopped being there,
    // and both have to reach the listener -- otherwise removeData on one side is invisible on the
    // other.
    //
    // Second, and this is the subtle one: a context that arrives after a reconnect can be OLDER than
    // what this side has already published. Delivering it unconditionally would walk a listener-driven
    // UI back to stale state while an immediate getData() still returned the newer local value -- the
    // listener and the getter disagreeing about the same path. So every path is compared against the
    // local entry first, and only a peer entry that actually wins is delivered.
    NSDictionary *localCtx = [session applicationContext];
    NSMutableSet *seen = [NSMutableSet set];
    for (NSString *path in cn1WearableAllPaths(nil, applicationContext)) {
        [seen addObject:path];
        CN1WearableEntry theirs = cn1WearableEntryFor(applicationContext, path);
        CN1WearableEntry mine = cn1WearableEntryFor(localCtx, path);
        if (cn1WearableLocalWins(mine, theirs)) {
            continue;
        }
        if (theirs.removed) {
            cn1_wearable_deliverDataRemoved(path.UTF8String);
        } else if (theirs.data != nil) {
            cn1_wearable_deliverDataChanged(path.UTF8String, theirs.data.bytes,
                                            (int) theirs.data.length);
        }
    }
    for (NSString *gone in _lastReceivedKeys) {
        if (![seen containsObject:gone]) {
            // Dropped out of the peer's context without a tombstone -- an older peer build, or a
            // context rebuilt from scratch. Treat it as the removal it is.
            CN1WearableEntry mine = cn1WearableEntryFor(localCtx, gone);
            if (!mine.known || mine.removed) {
                cn1_wearable_deliverDataRemoved(gone.UTF8String);
            }
        }
    }
    [_lastReceivedKeys release];
    _lastReceivedKeys = [seen retain];
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
