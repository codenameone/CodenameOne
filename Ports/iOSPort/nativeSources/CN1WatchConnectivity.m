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
/// The highest value this process has PERSISTED, which is what a new write has to beat.
///
/// Seeded from the stored floor on first use and raised on every write. Re-reading only the value
/// restored at startup was wrong the moment a second observation arrived: after persisting 1000, a
/// later stamp of 900 still exceeded the ORIGINAL floor and overwrote the stored value with the
/// lower one -- so a process that exited before its next local publication came back with a floor
/// beneath the peer's existing entry, and published values that lost to it until wall time caught
/// up. Guarded by the same lock as cn1WearableLast.
static int64_t cn1WearablePersistedFloor = 0;

static int64_t cn1WearableClockFloor(void) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1WearablePersistedFloor =
                (int64_t) [[NSUserDefaults standardUserDefaults] doubleForKey:kCn1WearableClockKey];
    });
    return cn1WearablePersistedFloor;
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
        // peer the advantage back. The high-water mark moves with it, so a lower later stamp
        // cannot overwrite this one.
        cn1WearablePersistedFloor = seen;
        [[NSUserDefaults standardUserDefaults] setDouble:(double) seen forKey:kCn1WearableClockKey];
    }
    [lock unlock];
}

/// How long an unanswered inbound reply block is kept. Comfortably past the sender's own reply
/// deadline: by the time this fires the peer has already given up, so the block can only be
/// discarded, never usefully invoked.
static const NSTimeInterval kCN1ReplyExpirySeconds = 120.0;

/// Stand-in stamp meaning "this path's removal has already been announced". A real stamp is a
/// wall-clock millisecond value, so a negative sentinel cannot collide with one.
static const int64_t kCN1RemovalAnnounced = -1;

/// Drops reply blocks the app never answered. Caller holds the _pendingReplies monitor.
static void cn1WearableExpireReplies(NSMutableDictionary *replies, NSMutableDictionary *arrivedAt) {
    if (replies.count == 0) {
        return;
    }
    NSTimeInterval now = [[NSDate date] timeIntervalSince1970];
    NSMutableArray *stale = [NSMutableArray array];
    for (NSNumber *key in arrivedAt.allKeys) {
        NSNumber *at = arrivedAt[key];
        if (at == nil || now - at.doubleValue > kCN1ReplyExpirySeconds) {
            [stale addObject:key];
        }
    }
    for (NSNumber *key in stale) {
        [replies removeObjectForKey:key];
        [arrivedAt removeObjectForKey:key];
    }
}

/// Where received transfers are parked so a process death cannot lose them.
static NSString *cn1WearableInboxDir(void) {
    NSArray *dirs = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory,
                                                        NSUserDomainMask, YES);
    NSString *base = dirs.count > 0 ? dirs[0] : NSTemporaryDirectory();
    NSString *dir = [base stringByAppendingPathComponent:@"cn1-wearable-inbox"];
    [[NSFileManager defaultManager] createDirectoryAtPath:dir withIntermediateDirectories:YES
                                               attributes:nil error:NULL];
    return dir;
}

/// Writes the encoded transfer to the inbox and returns its file name, or nil.
static NSString *cn1WearableStashInbox(NSString *path, NSData *wrapped) {
    if (wrapped == nil) {
        return nil;
    }
    NSString *name = [[NSUUID UUID] UUIDString];
    NSString *full = [cn1WearableInboxDir() stringByAppendingPathComponent:name];
    NSMutableDictionary *entry = [NSMutableDictionary dictionary];
    entry[@"p"] = path == nil ? @"" : path;
    entry[@"b"] = wrapped;
    if (![NSKeyedArchiver respondsToSelector:@selector(archivedDataWithRootObject:requiringSecureCoding:error:)]) {
        return nil;
    }
    NSData *blob = [NSKeyedArchiver archivedDataWithRootObject:entry
                                        requiringSecureCoding:NO error:NULL];
    if (blob == nil || ![blob writeToFile:full atomically:YES]) {
        return nil;
    }
    return name;
}

/// Inbox entries this process has already handed to the runtime and is still waiting to have
/// confirmed.
///
/// Delivery is asynchronous -- the payload sits in WearableConnection's queue until the EDT runs
/// it -- and cn1WearableDrainInbox runs on every session activation, not only the first. A session
/// that deactivates and reactivates while a delivery is still queued (the watch-switch flow does
/// exactly this) would otherwise find the same entry on disk and replay it, so the app would
/// receive one one-shot transfer twice. Entries leave this set when they are confirmed, which is
/// also when the file goes.
static NSMutableSet *cn1WearableInFlight(void) {
    static NSMutableSet *inFlight = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        inFlight = [[NSMutableSet alloc] init];
    });
    return inFlight;
}

/// Serializes access to the in-flight set: deliveries are handed over from the WCSession delegate
/// queue while confirmations arrive from the EDT.
static NSLock *cn1WearableInFlightLock(void) {
    static NSLock *lock = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        lock = [[NSLock alloc] init];
    });
    return lock;
}

/// Marks an entry as handed to the runtime. Returns NO when it already was.
static BOOL cn1WearableMarkInFlight(NSString *name) {
    if (name == nil) {
        return NO;
    }
    NSLock *lock = cn1WearableInFlightLock();
    [lock lock];
    NSMutableSet *inFlight = cn1WearableInFlight();
    BOOL fresh = ![inFlight containsObject:name];
    if (fresh) {
        [inFlight addObject:name];
    }
    [lock unlock];
    return fresh;
}

static void cn1WearableClearInFlight(NSString *name) {
    if (name == nil) {
        return;
    }
    NSLock *lock = cn1WearableInFlightLock();
    [lock lock];
    [cn1WearableInFlight() removeObject:name];
    [lock unlock];
}

/// Redelivers anything left in the inbox by a previous run, then clears it.
///
/// Called on session activation: reaching that point means this process is alive and the CN1
/// runtime is up, so anything still parked was written by a run that did not get that far.
static void cn1WearableDrainInbox(void) {
    NSString *dir = cn1WearableInboxDir();
    NSArray<NSString *> *names = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:dir
                                                                                     error:NULL];
    for (NSString *name in names) {
        NSString *full = [dir stringByAppendingPathComponent:name];
        if ([name hasSuffix:@".done"]) {
            // Legacy marker: entries are now retired by cn1_wearable_confirmInbox once the EDT has
            // consumed them, so nothing writes these any more. An app updated across that change
            // can still find one parked here, and it was already delivered.
            [[NSFileManager defaultManager] removeItemAtPath:full error:NULL];
            continue;
        }
        if (!cn1WearableMarkInFlight(name)) {
            // Already handed to the runtime by this process and still awaiting confirmation.
            // Replaying it now would deliver one one-shot transfer twice.
            continue;
        }
        NSData *blob = [NSData dataWithContentsOfFile:full];
        if (blob != nil) {
            NSSet *classes = [NSSet setWithObjects:[NSDictionary class], [NSString class],
                                                    [NSData class], nil];
            NSDictionary *entry = [NSKeyedUnarchiver unarchivedObjectOfClasses:classes
                                                                      fromData:blob
                                                                         error:NULL];
            NSString *path = entry[@"p"];
            NSData *body = entry[@"b"];
            if ([path isKindOfClass:[NSString class]] && [body isKindOfClass:[NSData class]]) {
                // Tracked: the entry is retired from the EDT, after the app has the payload.
                // Marking it here instead would retire it the moment the replay was QUEUED, and a
                // process death before the EDT ran it would lose the file for good -- the exact
                // failure this inbox exists to prevent.
                cn1_wearable_deliverDataChangedTracked(path.UTF8String, body.bytes,
                                                       (int) body.length, name.UTF8String);
                continue;
            }
        }
        // Unreadable or malformed: nothing to deliver and replaying it forever helps nobody.
        cn1WearableClearInFlight(name);
        [[NSFileManager defaultManager] removeItemAtPath:full error:NULL];
    }
}

/// Gives up a delivery without retiring it.
///
/// The pending-delivery queue evicted the payload before any listener saw it. The file must STAY
/// on disk -- it is still the only copy -- but the in-flight mark has to go, or every later drain
/// in this process skips the entry it is protecting and the transfer is never offered again.
void cn1_wearable_releaseInbox(const char *inboxToken) {
    if (inboxToken == NULL) {
        return;
    }
    NSString *name = [NSString stringWithUTF8String:inboxToken];
    if (name.length == 0) {
        return;
    }
    cn1WearableClearInFlight(name);
}

/// Re-offers everything still parked in the inbox.
///
/// Called from Java once a data listener exists, after a delivery was evicted from the pending
/// queue. Clearing the in-flight mark only makes the entry ELIGIBLE again; without this nothing
/// would look at it until the next session activation or a restart.
void cn1_wearable_replayInbox(void) {
    cn1WearableDrainInbox();
}

/// Forgets that a path's current value was received, so the next whole-context update delivers it
/// again.
///
/// For a delivery the pending-delivery cap had to discard: the entry is already recorded in
/// _lastReceived, so every later context replace treats it as unchanged and the app never sees it.
/// Dropping the record makes the very next update look new.
void cn1_wearable_forgetReceived(const char *path) {
    if (path == NULL) {
        return;
    }
    NSString *p = [NSString stringWithUTF8String:path];
    CN1WatchConnectivity *shared = [CN1WatchConnectivity shared];
    if (shared == nil || p.length == 0) {
        return;
    }
    @synchronized (shared) {
        [shared forgetReceivedPath:p];
    }
    // Forgetting alone recovers nothing: it makes the path eligible again, and then waits for a
    // context update that the peer may never send -- it has already published this value. The
    // context currently held IS the value, so re-run the delivery over it.
    //
    // Coalesced onto one pass: core hands paths back one at a time, and re-processing the whole
    // context per path would deliver every other path in it that many times over.
    [shared scheduleReceivedContextReplay];
}

/// Retires a delivered inbox entry.
///
/// Invoked from Java on the EDT after the payload has been handed to the application, which is the
/// only moment at which losing the durable copy is safe.
void cn1_wearable_confirmInbox(const char *inboxToken) {
    if (inboxToken == NULL) {
        return;
    }
    NSString *name = [NSString stringWithUTF8String:inboxToken];
    if (name.length == 0) {
        return;
    }
    NSString *full = [cn1WearableInboxDir() stringByAppendingPathComponent:name];
    [[NSFileManager defaultManager] removeItemAtPath:full error:NULL];
    cn1WearableClearInFlight(name);
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
    // This write must move the high-water mark too. Leaving it behind would let a later, LOWER
    // observation still look like a rise against a stale mark and overwrite this value -- the same
    // regression cn1WearableObserveSequence guards against, reintroduced through the other writer.
    // The sequence is monotonic, so result is always the highest value yet persisted.
    cn1WearablePersistedFloor = result;
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

/// When each tombstone was actually created, in local wall-clock milliseconds.
///
/// Kept OUTSIDE the application context deliberately. The context value is the ordering stamp, and
/// that is a Lamport sequence: cn1WearableObserveSequence drags it ahead of local time whenever a
/// peer's clock is ahead, so comparing it against `now` kept a tombstone for the clock offset PLUS
/// the advertised day. Changing the context value's shape would also change the wire format the
/// peer parses, so the age lives here instead, in this device's own defaults.
static NSString *const kCN1TombBirthKey = @"cn1.wearable.tombstoneBirth";

/// How long to wait before looking at a retained tombstone again. Hourly: the thing being waited
/// for is a peer coming back, which is not a fast event, and each pass is a dictionary walk.
static const int64_t kCN1TombstonePruneRetryMillis = 60 * 60 * 1000;

static int64_t cn1WearableNowMillis(void) {
    return (int64_t) ([[NSDate date] timeIntervalSince1970] * 1000.0);
}

static void cn1WearableNoteTombstoneBirth(NSString *path) {
    NSUserDefaults *d = [NSUserDefaults standardUserDefaults];
    // Both branches must be OWNED: ARC is off here, and releasing an autoreleased fallback below
    // would over-release it and crash when the pool drains.
    NSMutableDictionary *births = [[d dictionaryForKey:kCN1TombBirthKey] mutableCopy];
    if (births == nil) {
        births = [[NSMutableDictionary alloc] init];
    }
    births[path] = @(cn1WearableNowMillis());
    [d setObject:births forKey:kCN1TombBirthKey];
    [births release];
}

static void cn1WearableForgetTombstoneBirth(NSString *path) {
    NSUserDefaults *d = [NSUserDefaults standardUserDefaults];
    NSDictionary *stored = [d dictionaryForKey:kCN1TombBirthKey];
    if (stored[path] == nil) {
        return;
    }
    NSMutableDictionary *births = [stored mutableCopy];
    [births removeObjectForKey:path];
    [d setObject:births forKey:kCN1TombBirthKey];
    [births release];
}

/// The tombstone's age in millis. An entry with no recorded birth -- written by an earlier build,
/// or restored -- starts its clock now, which delays pruning by at most one TTL and never shortens
/// it, so a removal cannot undo itself because of missing bookkeeping.
static int64_t cn1WearableTombstoneAge(NSString *path) {
    NSDictionary *births = [[NSUserDefaults standardUserDefaults] dictionaryForKey:kCN1TombBirthKey];
    id born = births[path];
    if (![born isKindOfClass:[NSNumber class]]) {
        cn1WearableNoteTombstoneBirth(path);
        return 0;
    }
    return cn1WearableNowMillis() - [born longLongValue];
}

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
    for (NSString *key in tombKeys) {
        id stamp = ctx[key];
        if (![stamp isKindOfClass:[NSNumber class]]) {
            // Not ours, or corrupt: nothing to preserve.
            [ctx removeObjectForKey:key];
            continue;
        }
        NSString *path = [key substringFromIndex:kTombPrefix.length];
        // Age from the recorded birth, never from the ordering stamp -- see kCN1TombBirthKey.
        if (cn1WearableTombstoneAge(path) <= kCN1TombstoneTTLMillis) {
            continue;
        }
        // Old enough to consider -- but only actually drop it once the peer has acknowledged the
        // removal, meaning it no longer holds a value for that path older than the tombstone.
        CN1WearableEntry theirs = cn1WearableEntryFor(peerCtx, path);
        if (theirs.known && !theirs.removed && theirs.stamp < [stamp longLongValue]) {
            continue;
        }
        [ctx removeObjectForKey:key];
        cn1WearableForgetTombstoneBirth(path);
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
    // Walk the WHOLE list oldest-first and stop once the count is under the cap, rather than
    // examining only the oldest (count - cap) entries. With the old bound, a protected entry among
    // those oldest ones consumed one of the slots examined and nothing took its place: 300
    // tombstones whose oldest 44 were still protecting peer values meant every pass skipped and all
    // 300 stayed, forever, even though 256 newer ones could safely have been kept. The choice was
    // deterministic, so repeating the prune changed nothing.
    NSUInteger keep = remaining.count;
    for (NSUInteger i = 0; i < remaining.count && keep > kCN1MaxTombstones; i++) {
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
        // The birth record goes with it. Pruning finds those records only through tombstones still
        // in the context, so an entry dropped here becomes unreachable -- churn through unique
        // paths would grow the defaults dictionary without bound. Same reason as the TTL and
        // republish paths.
        cn1WearableForgetTombstoneBirth([key substringFromIndex:kTombPrefix.length]);
        keep--;
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
    /// When each pending reply arrived, so one that is never answered can be retired. Parallel to
    /// _pendingReplies and guarded by the same monitor.
    NSMutableDictionary<NSNumber *, NSNumber *> *_pendingReplyAt;
    /// Guards the recurring tombstone sweep to a single pending chain; see pruneTombstonesNow.
    BOOL _tombstoneSweepScheduled;
    /// Guards the post-removal deadline sweep to one block, however many paths are removed.
    BOOL _tombstoneDeadlineScheduled;
    /// Guards the received-context replay to one pass, however many paths are forgotten.
    BOOL _receivedReplayScheduled;
    int _nextInboundToken;
    /// Keys the peer's last context carried, so a key that vanishes is reported as a removal.
    /// What the peer's context last said for each path: the authoritative stamp we delivered, or
    /// the tombstone stamp for a removal. Stamps rather than bare keys, because WCSession hands
    /// over the WHOLE context on any change -- a set of keys cannot tell an unchanged entry from a
    /// re-sent one.
    NSMutableDictionary<NSString *, NSNumber *> *_lastReceived;
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
        _pendingReplyAt = [[NSMutableDictionary alloc] init];
        _nextInboundToken = 1;
        _lastReceived = [[NSMutableDictionary alloc] init];
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
        [_pendingReplyAt removeObjectForKey:key];
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
    // And its birth record. Pruning only visits tombstones still in the context, so a path that is
    // republished leaves an entry nothing will ever revisit -- an app cycling through changing
    // paths would grow that dictionary without bound.
    cn1WearableForgetTombstoneBirth(path);
    // Pruned here as well, not only in removeData. Tombstones raised while the peer was offline
    // become prunable the moment it reconnects and acknowledges them -- but an app that then only
    // ever calls putData() never reached the sweep, because removeData() held its only call site.
    // The context kept growing until a perfectly ordinary value publication was rejected for size,
    // with every tombstone in it eligible for removal. Publishing is exactly when that matters,
    // since publishing is what the oversized context breaks.
    cn1WearablePruneTombstones(ctx, [s receivedApplicationContext]);
    NSError *err = nil;
    [s updateApplicationContext:ctx error:&err];
    // Released before the error branch below, not after it: an early return there would leave the
    // lock held for the life of the process and every later putData/removeData would block on it.
    [ctxLock unlock];
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
    cn1WearableNoteTombstoneBirth(path);
    cn1WearablePruneTombstones(ctx, [s receivedApplicationContext]);
    // Also swept when THIS tombstone comes of age. Pruning ran only from putData and removeData, so
    // an app whose last act is a removal kept that final batch in its persisted context for good --
    // the tombstone is necessarily younger than its TTL at the moment it is written, and nothing
    // else would ever look again.
    // ONE deadline block, however many paths are removed. Every removeData used to queue its own
    // 24-hour block, each retaining the delegate, so churn through many paths accumulated
    // thousands of them and as many redundant whole-context passes. The first tombstone's deadline
    // is the earliest one that can matter; the sweep it triggers re-arms itself hourly while
    // anything is still held, which covers every tombstone raised after it.
    if (!_tombstoneDeadlineScheduled) {
        _tombstoneDeadlineScheduled = YES;
        CN1WatchConnectivity *keepAlive = [self retain];
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW,
                                     (int64_t) (kCN1TombstoneTTLMillis + 1000) * NSEC_PER_MSEC),
                       dispatch_get_main_queue(), ^{
            keepAlive->_tombstoneDeadlineScheduled = NO;
            [keepAlive pruneTombstonesNow];
            [keepAlive release];
        });
    }
    NSError *err = nil;
    [s updateApplicationContext:ctx error:&err];
    if (err != nil) {
        // The tombstone never entered the session context, so its birth record has nothing to be
        // discovered through: pruning walks tombstones in the context, and one with no tombstone is
        // unreachable for good. Repeated failed removals of unique paths would grow the defaults
        // dictionary without bound.
        cn1WearableForgetTombstoneBirth(path);
    }
    [ctxLock unlock];
    [ctx release];
}

/// Drops a path's received marker; see cn1_wearable_forgetReceived.
- (void)forgetReceivedPath:(NSString *)path {
    [_lastReceived removeObjectForKey:path];
}

/// Re-runs the received-context delivery once, after any number of paths have been forgotten.
- (void)scheduleReceivedContextReplay {
    @synchronized (self) {
        if (_receivedReplayScheduled) {
            return;
        }
        _receivedReplayScheduled = YES;
    }
    CN1WatchConnectivity *keepAlive = [self retain];
    dispatch_async(dispatch_get_main_queue(), ^{
        @synchronized (keepAlive) {
            keepAlive->_receivedReplayScheduled = NO;
        }
        WCSession *s = [keepAlive session];
        NSDictionary *ctx = s == nil ? nil : [s receivedApplicationContext];
        if (ctx != nil && ctx.count > 0) {
            // The ordinary delivery path, so every rule about winners, tombstones and
            // acknowledgement applies exactly as it does for a context that just arrived.
            [keepAlive session:s didReceiveApplicationContext:ctx];
        }
        [keepAlive release];
    });
}

/// Prunes without publishing anything else, for the scheduled sweep, on activation, and whenever
/// the peer's context arrives.
///
/// Re-arms itself while anything is still held back: a tombstone past its TTL is kept until the
/// peer acknowledges the removal, and a peer that is offline at the deadline acknowledges later --
/// with no sweep left to notice, a quiet app kept that tombstone for good.
- (void)pruneTombstonesNow {
    WCSession *s = [self session];
    if (s == nil) {
        return;
    }
    NSLock *ctxLock = cn1WearableContextLock();
    [ctxLock lock];
    NSMutableDictionary *ctx = [[s applicationContext] mutableCopy];
    if (ctx == nil) {
        [ctxLock unlock];
        return;
    }
    NSUInteger before = ctx.count;
    cn1WearablePruneTombstones(ctx, [s receivedApplicationContext]);
    BOOL stillHeld = NO;
    for (NSString *key in ctx.allKeys) {
        if ([key isKindOfClass:[NSString class]] && [key hasPrefix:kTombPrefix]) {
            stillHeld = YES;
            break;
        }
    }
    if (stillHeld && !_tombstoneSweepScheduled) {
        // ONE chain, ever. This runs on activation and on every received application context, and
        // each call used to start its own recurring hourly sweep -- a peer syncing frequently would
        // accumulate thousands of delayed blocks, each retaining the delegate and rescheduling
        // itself. The flag is cleared when the sweep fires, so exactly one is ever pending.
        _tombstoneSweepScheduled = YES;
        CN1WatchConnectivity *again = [self retain];
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW,
                                     (int64_t) kCN1TombstonePruneRetryMillis * NSEC_PER_MSEC),
                       dispatch_get_main_queue(), ^{
            again->_tombstoneSweepScheduled = NO;
            [again pruneTombstonesNow];
            [again release];
        });
    }
    if (ctx.count != before) {
        // Only when something actually went: updateApplicationContext with an unchanged dictionary
        // is a wasted transfer, and on the peer it looks like a fresh context to re-examine.
        NSError *err = nil;
        [s updateApplicationContext:ctx error:&err];
    }
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
    // Anything a previous run parked but may not have delivered. Reaching activation means this
    // process is up and the CN1 runtime with it, so a file still sitting in the inbox belongs to a
    // run that did not get this far.
    cn1WearableDrainInbox();
    // And sweep tombstones, which covers the case a timer cannot: an app whose last act was a
    // removal and which then exited takes its scheduled sweep with it, so the next launch is the
    // only thing that will ever look at that batch again.
    [self pruneTombstonesNow];
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
            // Retire anything the app never answered. sendReply is the only other way out of this
            // dictionary, so a build that registers no message listener -- WearableConnection parks
            // those deliveries indefinitely -- never removes a single entry, and every reply-bearing
            // message the peer sends leaks a copied block. The senders have long since timed out by
            // then, so answering late is pointless; the entry just has to go.
            cn1WearableExpireReplies(_pendingReplies, _pendingReplyAt);
            token = _nextInboundToken++;
            // -copy returns +1 under manual reference counting and the dictionary retains it too,
            // so hand off the copy's ownership rather than leaking it.
            void (^stored)(NSDictionary<NSString *, id> *) = [replyHandler copy];
            _pendingReplies[@(token)] = stored;
            _pendingReplyAt[@(token)] = @([[NSDate date] timeIntervalSince1970]);
            // Also swept when THIS batch expires. Expiry ran only when the next request arrived, so
            // a finite burst -- or a single request -- to an app that never registers a message
            // listener held its copied reply blocks for the rest of the process, long after every
            // sender had given up. The retention window is a promise about the request, not about
            // how often requests happen to arrive.
            CN1WatchConnectivity *keepAlive = [self retain];
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW,
                                         (int64_t) ((kCN1ReplyExpirySeconds + 1) * NSEC_PER_SEC)),
                           dispatch_get_main_queue(), ^{
                // Same monitor the insert holds: _pendingReplies guards both maps.
                @synchronized (keepAlive->_pendingReplies) {
                    cn1WearableExpireReplies(keepAlive->_pendingReplies, keepAlive->_pendingReplyAt);
                }
                [keepAlive release];
            });
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
    // Third: WCSession hands over the peer's ENTIRE context whenever any part of it changes, so
    // every unchanged peer path arrives again on every publish. Announcing them all meant
    // publishing /b produced a dataChanged for /a as well, and every past removal was re-announced
    // indefinitely. Only an entry whose authoritative stamp actually moved is delivered.
    // The peer's context is exactly the evidence a retained tombstone was waiting for, so look at
    // them again now. Without this, a tombstone kept past its TTL because the peer was offline had
    // only the periodic retry to release it.
    [self pruneTombstonesNow];
    NSDictionary *localCtx = [session applicationContext];
    NSMutableDictionary *seen = [NSMutableDictionary dictionary];
    NSMutableArray *acknowledge = [NSMutableArray array];
    for (NSString *path in cn1WearableAllPaths(nil, applicationContext)) {
        CN1WearableEntry theirs = cn1WearableEntryFor(applicationContext, path);
        CN1WearableEntry mine = cn1WearableEntryFor(localCtx, path);
        seen[path] = @(theirs.stamp);
        if (cn1WearableLocalWins(mine, theirs)) {
            continue;
        }
        NSNumber *previous = _lastReceived[path];
        if (previous != nil && previous.longLongValue == theirs.stamp) {
            // Same entry we already delivered, re-sent as part of the whole-context replace.
            continue;
        }
        // A tombstone we have ALREADY announced is also unchanged, and it does not compare equal:
        // what was recorded for it is the sentinel, not the peer's stamp. Without this, every
        // unrelated publication by the peer re-delivered dataRemoved for that path -- once per
        // whole-context update until the tombstone was finally pruned -- and an app that treats a
        // removal as an event acted on it each time.
        //
        // Only while it is STILL a tombstone: a republish under the same path arrives with
        // theirs.removed false and falls through to be delivered normally.
        if (previous != nil && previous.longLongValue == kCN1RemovalAnnounced && theirs.removed) {
            seen[path] = @(kCN1RemovalAnnounced);
            continue;
        }
        if (theirs.removed) {
            seen[path] = @(kCN1RemovalAnnounced);
            if (mine.known && !mine.removed) {
                // Acknowledge it by dropping OUR live value for the path. The remover keeps a
                // tombstone until the value it removed is gone from our context, so without this
                // its tombstones accumulate past the cap and eventually the context can no longer
                // be published at all. Clearing the value is the acknowledgement.
                [acknowledge addObject:path];
            }
            cn1_wearable_deliverDataRemoved(path.UTF8String);
        } else if (theirs.data != nil) {
            cn1_wearable_deliverDataChanged(path.UTF8String, theirs.data.bytes,
                                            (int) theirs.data.length);
        }
    }
    if (acknowledge.count > 0) {
        NSLock *ctxLock = cn1WearableContextLock();
        [ctxLock lock];
        NSMutableDictionary *mineCtx = [[session applicationContext] mutableCopy];
        if (mineCtx != nil) {
            BOOL acknowledged = NO;
            for (NSString *path in acknowledge) {
                // Re-decided against the context as it is NOW, under the lock. The decision to
                // acknowledge was taken against a snapshot read before this lock was held, and the
                // app can republish the path in between -- removing the value on the strength of
                // the old snapshot would then delete a NEWER, higher-stamped publication and lose
                // it silently. If our current entry still loses to the peer's tombstone the
                // acknowledgement stands; if it now wins, the republish is the newer fact and the
                // peer will see it and drop its tombstone in turn.
                CN1WearableEntry current = cn1WearableEntryFor(mineCtx, path);
                CN1WearableEntry tomb = cn1WearableEntryFor(applicationContext, path);
                if (cn1WearableLocalWins(current, tomb)) {
                    continue;
                }
                [mineCtx removeObjectForKey:cn1WearableValueKey(path)];
                [mineCtx removeObjectForKey:cn1WearableStampKey(path)];
                acknowledged = YES;
            }
            if (acknowledged) {
                NSError *ackErr = nil;
                [session updateApplicationContext:mineCtx error:&ackErr];
                if (ackErr != nil) {
                    // The acknowledgement did NOT go out, so our stale value is still in the
                    // context and the peer must keep its tombstone. Recording the tombstone as
                    // announced anyway would send every later context update down the
                    // unchanged-sentinel branch, so this path would never be acknowledged again and
                    // the peer would hold that tombstone for good. Forget the marks instead: the
                    // next context update re-runs the acknowledgement.
                    for (NSString *path in acknowledge) {
                        [seen removeObjectForKey:path];
                    }
                }
            }
            [mineCtx release];
        }
        [ctxLock unlock];
    }
    for (NSString *gone in _lastReceived.allKeys) {
        if (seen[gone] == nil) {
            // Dropped out of the peer's context without a tombstone -- an older peer build, or a
            // context rebuilt from scratch. Treat it as the removal it is.
            //
            // Except when we already announced that removal. A tombstone the peer PRUNED after its
            // 24-hour TTL also disappears from the context, and the listener was told about it when
            // the tombstone first arrived; re-announcing on the next routine context update turns
            // ordinary housekeeping into a duplicate dataRemoved. _lastReceived remembers the
            // tombstone's stamp, and a negative marker records that its removal has been reported.
            CN1WearableEntry mine = cn1WearableEntryFor(localCtx, gone);
            NSNumber *previous = _lastReceived[gone];
            BOOL alreadyAnnounced = previous != nil && previous.longLongValue == kCN1RemovalAnnounced;
            // mine.removed means WE hold the tombstone -- this device called removeData. The peer's
            // value then disappears from its context precisely because it acknowledged our removal,
            // and reporting that back would fire dataRemoved on the device that asked for it, which
            // WearableDataListener promises never to do. Only a path we never had, or never removed
            // ourselves, is a peer-originated disappearance.
            if (mine.known && !mine.removed) {
                // A LIVE local value survived the peer's disappearance. Both devices had published
                // this path, the peer's newer value was the one delivered, and now the peer's entry
                // is gone without a tombstone -- so the winner is our own value again. Saying
                // nothing left the listener on the vanished peer value while getData() already
                // returned the local one, and nothing later would reconcile them.
                //
                // Announced only once per disappearance: _lastReceived is rewritten from `seen`
                // below, so `gone` drops out of it and this branch cannot re-fire on the next
                // context update.
                if (mine.data != nil && !alreadyAnnounced) {
                    cn1_wearable_deliverDataChanged(gone.UTF8String, mine.data.bytes,
                                                    (int) mine.data.length);
                }
            } else if (!mine.known && !alreadyAnnounced) {
                cn1_wearable_deliverDataRemoved(gone.UTF8String);
            }
        }
    }
    [_lastReceived release];
    _lastReceived = [seen mutableCopy];
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
    // Copied somewhere durable BEFORE this returns. WatchConnectivity deletes its temporary file as
    // soon as the delegate returns and considers the transfer complete, while the payload at that
    // point exists only in WearableConnection's in-process queue or an un-run callSerially. A
    // process death in between loses a one-shot file that the sender has already been told arrived,
    // and nothing redelivers it -- the sender's copy is gone too.
    NSData *wrapped = cn1WearableWrapFile(file.fileURL.lastPathComponent, body);
    NSString *stashed = cn1WearableStashInbox(path, wrapped);
    // Marked before the hand-off, so a reactivation that drains the inbox mid-flight skips it.
    cn1WearableMarkInFlight(stashed);
    // Retired from the EDT once the app actually has the payload, not here. Deleting or marking at
    // this point discards the only durable copy while the delivery is still merely queued, and a
    // process death in that window loses a one-shot transfer the sender was already told arrived.
    cn1_wearable_deliverDataChangedTracked(path.UTF8String, wrapped.bytes, (int) wrapped.length,
                                           stashed == nil ? NULL : stashed.UTF8String);
}

@end

#endif // CN1_USE_WATCHCONNECTIVITY
