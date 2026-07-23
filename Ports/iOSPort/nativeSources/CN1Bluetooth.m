/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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

// CoreBluetooth implementation of the IOSNative bt* trampolines backing
// com.codename1.bluetooth (see IOSBluetooth.java for the Java half).
//
// Architecture:
// - A single CN1BluetoothController owns the CBCentralManager and (on
//   slices that have it) the CBPeripheralManager. Both are created lazily:
//   instantiating a CBCentralManager is what pops the OS Bluetooth
//   permission dialog, so nothing is allocated until the app touches the
//   Bluetooth API.
// - All CoreBluetooth work runs on one serial dispatch queue
//   ("com.codename1.bluetooth"), never the main queue. Void natives hop
//   onto it with dispatch_async; value-returning natives use a
//   run-inline-if-already-on-queue sync so that Java callbacks re-entering
//   the bridge from the queue thread cannot deadlock.
// - Discovered CBPeripheral objects are retained for the lifetime of the
//   controller in an identifier-keyed dictionary. CoreBluetooth only keeps
//   weak knowledge of unretained peripherals -- connections silently die
//   without this. Entries are small and device discovery counts are
//   bounded in practice, so no eviction pass is attempted (documented
//   trade-off).
// - Java arrays/strings passed into a native are converted to NSData /
//   NSString *before* any dispatch_async: once the Java frame returns the
//   JAVA_OBJECT is no longer rooted and must not be touched.
// - Delegate callbacks call the generated
//   com_codename1_impl_ios_IOSBluetooth_nativeBt* trampolines directly from
//   the Bluetooth queue (getThreadLocalData() attaches the thread), same as
//   the camera/NFC bridges.
//
// Memory management is manual -- the iOS port builds with
// CLANG_ENABLE_OBJC_ARC=NO.

#include "xmlvm.h"
#import "CodenameOne_GLViewController.h"
#import "CN1Bluetooth.h"

#ifdef CN1_INCLUDE_BLUETOOTH

#import <CoreBluetooth/CoreBluetooth.h>
#include <unistd.h>
#include <string.h>
#include "com_codename1_impl_ios_IOSBluetooth.h"

// The BLE peripheral role (CBPeripheralManager) does not exist on tvOS or
// watchOS; the whole server/advertising/L2CAP-publish section compiles out
// there and isBlePeripheralSupported reports false.
#if !TARGET_OS_TV && !TARGET_OS_WATCH
#define CN1_BT_PERIPHERAL_ROLE 1
#import <UIKit/UIKit.h>
#endif

extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str);
extern NSString*   toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT nsDataToByteArr(NSData *data);

// Error codes shared with IOSBluetooth.java -- keep the two lists in sync.
#define CN1_BT_ERR_UNKNOWN 0
#define CN1_BT_ERR_NOT_SUPPORTED 1
#define CN1_BT_ERR_POWERED_OFF 2
#define CN1_BT_ERR_UNAUTHORIZED 3
#define CN1_BT_ERR_SCAN_FAILED 4
#define CN1_BT_ERR_ADVERTISE_FAILED 5
#define CN1_BT_ERR_CONNECTION_FAILED 6
#define CN1_BT_ERR_CONNECTION_LOST 7
#define CN1_BT_ERR_NOT_CONNECTED 8
#define CN1_BT_ERR_GATT 9
#define CN1_BT_ERR_TIMEOUT 10
#define CN1_BT_ERR_IO 11

// --------------------------------------------------------------------------
// dispatch queue

static dispatch_queue_t cn1btQueue = nil;
static void *kCN1BtQueueKey = &kCN1BtQueueKey;

static dispatch_queue_t cn1btGetQueue(void) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1btQueue = dispatch_queue_create("com.codename1.bluetooth",
                DISPATCH_QUEUE_SERIAL);
        dispatch_queue_set_specific(cn1btQueue, kCN1BtQueueKey,
                (void *)1, NULL);
    });
    return cn1btQueue;
}

/** Runs the block on the Bluetooth queue and waits; runs inline when the
 * caller is already on the queue (Java callbacks issued from delegate code
 * re-enter the bridge synchronously -- dispatch_sync would deadlock). */
static void cn1btSync(void (^block)(void)) {
    if (dispatch_get_specific(kCN1BtQueueKey)) {
        block();
    } else {
        dispatch_sync(cn1btGetQueue(), block);
    }
}

/** Fire-and-forget onto the Bluetooth queue (inline when already there,
 * preserving ordering on the serial queue in both cases). */
static void cn1btAsync(void (^block)(void)) {
    if (dispatch_get_specific(kCN1BtQueueKey)) {
        block();
    } else {
        dispatch_async(cn1btGetQueue(), block);
    }
}

// --------------------------------------------------------------------------
// small helpers

static JAVA_OBJECT cn1btJString(NSString *s) {
    return s == nil ? JAVA_NULL : fromNSString(getThreadLocalData(), s);
}

static JAVA_OBJECT cn1btJBytes(NSData *d) {
    return d == nil ? JAVA_NULL : nsDataToByteArr(d);
}

static NSData *cn1btDataFromJavaArray(JAVA_OBJECT arr) {
    if (arr == JAVA_NULL) {
        return [NSData data];
    }
    JAVA_ARRAY a = (JAVA_ARRAY)arr;
    if (a->length <= 0) {
        return [NSData data];
    }
    return [NSData dataWithBytes:a->data length:a->length];
}

static NSString *cn1btHexFromData(NSData *d) {
    if (d == nil || [d length] == 0) {
        return @"";
    }
    const unsigned char *bytes = (const unsigned char *)[d bytes];
    NSUInteger len = [d length];
    NSMutableString *out = [NSMutableString stringWithCapacity:len * 2];
    for (NSUInteger i = 0; i < len; i++) {
        [out appendFormat:@"%02x", bytes[i]];
    }
    return out;
}

static int cn1btHexDigit(unichar c) {
    if (c >= '0' && c <= '9') {
        return c - '0';
    }
    if (c >= 'a' && c <= 'f') {
        return c - 'a' + 10;
    }
    if (c >= 'A' && c <= 'F') {
        return c - 'A' + 10;
    }
    return -1;
}

static NSData *cn1btDataFromHex(NSString *hex) {
    NSUInteger len = [hex length] / 2;
    if (len == 0) {
        return nil;
    }
    NSMutableData *out = [NSMutableData dataWithLength:len];
    unsigned char *bytes = (unsigned char *)[out mutableBytes];
    for (NSUInteger i = 0; i < len; i++) {
        int hi = cn1btHexDigit([hex characterAtIndex:i * 2]);
        int lo = cn1btHexDigit([hex characterAtIndex:i * 2 + 1]);
        if (hi < 0 || lo < 0) {
            return nil;
        }
        bytes[i] = (unsigned char)((hi << 4) | lo);
    }
    return out;
}

static CBUUID *cn1btUuid(NSString *uuidStr) {
    if (uuidStr == nil || [uuidStr length] == 0) {
        return nil;
    }
    @try {
        return [CBUUID UUIDWithString:uuidStr];
    } @catch (NSException *e) {
        return nil;
    }
}

/** Converts a Java String[] to CBUUIDs; must run on the calling Java
 * thread (before any dispatch). */
static NSArray *cn1btUuidArray(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT arr) {
    if (arr == JAVA_NULL) {
        return nil;
    }
    JAVA_ARRAY a = (JAVA_ARRAY)arr;
    if (a->length <= 0) {
        return nil;
    }
    JAVA_ARRAY_OBJECT *data = (JAVA_ARRAY_OBJECT *)a->data;
    NSMutableArray *out = [NSMutableArray arrayWithCapacity:a->length];
    for (int i = 0; i < a->length; i++) {
        if (data[i] == JAVA_NULL) {
            continue;
        }
        CBUUID *u = cn1btUuid(toNSString(threadStateData,
                (JAVA_OBJECT)data[i]));
        if (u != nil) {
            [out addObject:u];
        }
    }
    return [out count] > 0 ? out : nil;
}

static int cn1btAuthorizationValue(void) {
    if (@available(iOS 13.1, tvOS 13.1, watchOS 6.1, *)) {
        switch (CBManager.authorization) {
            case CBManagerAuthorizationNotDetermined:
                return 0;
            case CBManagerAuthorizationRestricted:
                return 1;
            case CBManagerAuthorizationDenied:
                return 2;
            case CBManagerAuthorizationAllowedAlways:
            default:
                return 3;
        }
    }
    return 3; // pre-13.1: no per-app Bluetooth authorization
}

static int cn1btMapNSError(NSError *err, int fallback) {
    if (err == nil) {
        return fallback;
    }
    if ([err.domain isEqualToString:CBATTErrorDomain]) {
        return CN1_BT_ERR_GATT;
    }
    if ([err.domain isEqualToString:CBErrorDomain]) {
        switch (err.code) {
            case CBErrorNotConnected:
                return CN1_BT_ERR_NOT_CONNECTED;
            case CBErrorConnectionTimeout:
                return CN1_BT_ERR_TIMEOUT;
            case CBErrorPeripheralDisconnected:
                return CN1_BT_ERR_CONNECTION_LOST;
            case CBErrorOperationCancelled:
                return CN1_BT_ERR_UNKNOWN;
            default:
                return fallback;
        }
    }
    return fallback;
}

// --------------------------------------------------------------------------
// Java callback wrappers (all safe to call from the Bluetooth queue)

static void cn1btSendRequestError(int rid, int code, NSString *msg) {
    if (rid <= 0) {
        return;
    }
    com_codename1_impl_ios_IOSBluetooth_nativeBtRequestError___int_int_java_lang_String(
            getThreadLocalData(), rid, code, cn1btJString(msg));
}

static void cn1btSendNSError(int rid, NSError *err, int fallback) {
    cn1btSendRequestError(rid, cn1btMapNSError(err, fallback),
            err != nil ? err.localizedDescription : nil);
}

static void cn1btSendOpComplete(int rid) {
    if (rid <= 0) {
        return;
    }
    com_codename1_impl_ios_IOSBluetooth_nativeBtOperationComplete___int(
            getThreadLocalData(), rid);
}

// --------------------------------------------------------------------------
// L2CAP channel wrappers -- guarded by their own lock (not the queue) so
// the blocking read/write natives can poll off-queue without starving the
// delegates.

@interface CN1BtL2capChannel : NSObject
@property (nonatomic, retain) NSObject *channel; // CBL2CAPChannel
@property (nonatomic, retain) NSInputStream *input;
@property (nonatomic, retain) NSOutputStream *output;
@property (nonatomic, assign) BOOL closed;
@end

@implementation CN1BtL2capChannel
- (void)dealloc {
    [_channel release];
    [_input release];
    [_output release];
    [super dealloc];
}
@end

static NSMutableDictionary *cn1btL2capChannels = nil;
static long long cn1btNextL2capHandle = 1;

static NSMutableDictionary *cn1btChannels(void) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1btL2capChannels = [[NSMutableDictionary alloc] init];
    });
    return cn1btL2capChannels;
}

/** Wraps a freshly opened CBL2CAPChannel, opens its streams and returns
 * the opaque handle handed to Java. The streams are consumed by polling
 * (hasBytesAvailable / hasSpaceAvailable + read/write) from the Java
 * caller's thread, so no runloop scheduling is required. */
static long long cn1btRegisterL2capChannel(CBL2CAPChannel *channel)
        API_AVAILABLE(ios(11.0), tvos(11.0), watchos(4.0)) {
    CN1BtL2capChannel *w = [[CN1BtL2capChannel alloc] init];
    w.channel = (NSObject *)channel;
    w.input = channel.inputStream;
    w.output = channel.outputStream;
    [w.input open];
    [w.output open];
    long long h;
    NSMutableDictionary *dict = cn1btChannels();
    @synchronized (dict) {
        h = cn1btNextL2capHandle++;
        [dict setObject:w forKey:[NSNumber numberWithLongLong:h]];
    }
    [w release];
    return h;
}

static CN1BtL2capChannel *cn1btChannelForHandle(long long h) {
    NSMutableDictionary *dict = cn1btChannels();
    @synchronized (dict) {
        CN1BtL2capChannel *w = [dict objectForKey:
                [NSNumber numberWithLongLong:h]];
        return [[w retain] autorelease];
    }
}

// --------------------------------------------------------------------------
// per-peripheral bookkeeping

@interface CN1BtPeripheralEntry : NSObject
@property (nonatomic, retain) CBPeripheral *peripheral;
// GATT discovery aggregation
@property (nonatomic, assign) int discoverRid;
@property (nonatomic, assign) int pendingCharDiscoveries;
@property (nonatomic, assign) int pendingDescDiscoveries;
// pending request ids keyed by "serviceIndex/charIndex" (+ "/descUuid")
@property (nonatomic, retain) NSMutableDictionary *pendingReads;
@property (nonatomic, retain) NSMutableDictionary *pendingWrites;
@property (nonatomic, retain) NSMutableDictionary *pendingNotifyState;
@property (nonatomic, retain) NSMutableDictionary *pendingDescReads;
@property (nonatomic, retain) NSMutableDictionary *pendingDescWrites;
@property (nonatomic, assign) int pendingRssiRid;
// array of @{@"rid": ..., @"psm": ...} awaiting didOpenL2CAPChannel
@property (nonatomic, retain) NSMutableArray *pendingL2capOpens;
@end

@implementation CN1BtPeripheralEntry
- (id)init {
    if ((self = [super init])) {
        self.pendingReads = [NSMutableDictionary dictionary];
        self.pendingWrites = [NSMutableDictionary dictionary];
        self.pendingNotifyState = [NSMutableDictionary dictionary];
        self.pendingDescReads = [NSMutableDictionary dictionary];
        self.pendingDescWrites = [NSMutableDictionary dictionary];
        self.pendingL2capOpens = [NSMutableArray array];
    }
    return self;
}

- (void)dealloc {
    [_peripheral release];
    [_pendingReads release];
    [_pendingWrites release];
    [_pendingNotifyState release];
    [_pendingDescReads release];
    [_pendingDescWrites release];
    [_pendingL2capOpens release];
    [super dealloc];
}
@end

// --------------------------------------------------------------------------
// controller

typedef void (^CN1BtCharOp)(CN1BtPeripheralEntry *e, CBCharacteristic *ch,
        NSString *key);

@interface CN1BluetoothController : NSObject <CBCentralManagerDelegate,
        CBPeripheralDelegate
#ifdef CN1_BT_PERIPHERAL_ROLE
        , CBPeripheralManagerDelegate
#endif
        > {
    CBCentralManager *central;
    NSMutableDictionary *entries; // identifier UUID string -> entry
    BOOL scanActive;
    BOOL scanDuplicates;
    NSArray *scanServiceUuids;
#ifdef CN1_BT_PERIPHERAL_ROLE
    CBPeripheralManager *peripheralMgr;
    NSMutableArray *pendingServerOpenRids;   // NSNumber(int)
    int pendingAdvertiseRid;                 // 0 = none
    NSDictionary *pendingAdvertiseData;      // retained until started
    BOOL advertiseSubmitted;                 // startAdvertising already sent
    NSMutableDictionary *localServices;      // @(sid) -> CBMutableService
    NSMutableDictionary *localChars;         // @(cid) -> CBMutableCharacteristic
    NSMutableDictionary *staticValues;       // @(cid) -> NSData
    NSMutableDictionary *pendingAddServiceRids; // @(svc ptr) -> @(rid)
    NSMutableDictionary *attRequests;        // @(handle) -> entry dict
    long long nextAttHandle;
    NSMutableArray *pendingNotifies;         // job dicts, FIFO
    NSMutableDictionary *subscribedCentrals; // central id -> CBCentral
    NSMutableArray *pendingPublishRequests;  // pre-poweredOn @{rid, secure}
    NSMutableArray *publishAwaitingCallback; // NSNumber(rid), FIFO
#endif
}
- (void)ensureCentral;
- (void)startScanUuids:(NSArray *)uuids duplicates:(BOOL)dup;
- (void)stopScan;
- (NSString *)retrievePeripheral:(NSString *)pid;
- (NSString *)connectedPeripheralsFor:(CBUUID *)serviceUuid;
- (void)connectPeripheral:(NSString *)pid;
- (void)disconnectPeripheral:(NSString *)pid;
- (void)discoverServices:(int)requestId peripheral:(NSString *)pid;
- (void)withChar:(int)requestId peripheral:(NSString *)pid
        svcUuid:(NSString *)svcUuid svcInst:(int)svcInst
        charUuid:(NSString *)charUuid charInst:(int)charInst
        op:(CN1BtCharOp)op;
- (void)readRssi:(int)requestId peripheral:(NSString *)pid;
- (int)maxWriteLength:(NSString *)pid withResponse:(BOOL)withResponse;
- (void)openL2cap:(int)requestId peripheral:(NSString *)pid psm:(int)psm;
#ifdef CN1_BT_PERIPHERAL_ROLE
- (void)openGattServer:(int)requestId;
- (void)addService:(int)requestId definition:(NSString *)def;
- (void)removeServiceById:(int)serviceLocalId;
- (void)closeGattServer;
- (void)startAdvertising:(int)requestId name:(NSString *)name
        uuids:(NSArray *)uuids;
- (void)stopAdvertising;
- (void)notifyValue:(int)requestId charId:(int)charLocalId
        data:(NSData *)data central:(NSString *)centralId;
- (void)respondToRead:(long long)handle data:(NSData *)data
        status:(int)attStatus;
- (void)respondToWrite:(long long)handle status:(int)attStatus;
- (void)publishL2cap:(int)requestId secure:(BOOL)secure;
- (void)unpublishL2cap:(int)psm;
#endif
@end

static CN1BluetoothController *cn1btController = nil;

static CN1BluetoothController *cn1btGetController(void) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1btController = [[CN1BluetoothController alloc] init];
    });
    return cn1btController;
}

@implementation CN1BluetoothController

- (id)init {
    if ((self = [super init])) {
        entries = [[NSMutableDictionary alloc] init];
#ifdef CN1_BT_PERIPHERAL_ROLE
        pendingServerOpenRids = [[NSMutableArray alloc] init];
        localServices = [[NSMutableDictionary alloc] init];
        localChars = [[NSMutableDictionary alloc] init];
        staticValues = [[NSMutableDictionary alloc] init];
        pendingAddServiceRids = [[NSMutableDictionary alloc] init];
        attRequests = [[NSMutableDictionary alloc] init];
        pendingNotifies = [[NSMutableArray alloc] init];
        subscribedCentrals = [[NSMutableDictionary alloc] init];
        pendingPublishRequests = [[NSMutableArray alloc] init];
        publishAwaitingCallback = [[NSMutableArray alloc] init];
#endif
    }
    return self;
}

// The controller is a process-lifetime singleton -- no dealloc path.

- (void)ensureCentral {
    if (central == nil) {
        // creating the manager triggers the OS permission prompt
        central = [[CBCentralManager alloc] initWithDelegate:self
                queue:cn1btGetQueue()];
    }
}

- (CN1BtPeripheralEntry *)entryForPeripheral:(CBPeripheral *)p {
    NSString *pid = p.identifier.UUIDString;
    CN1BtPeripheralEntry *e = [entries objectForKey:pid];
    if (e == nil) {
        e = [[CN1BtPeripheralEntry alloc] init];
        e.peripheral = p;
        p.delegate = self;
        [entries setObject:e forKey:pid];
        [e release];
    }
    return e;
}

- (CN1BtPeripheralEntry *)entryForId:(NSString *)pid {
    return pid == nil ? nil : [entries objectForKey:pid];
}

// ---- attribute lookup ----------------------------------------------------

- (CBService *)serviceIn:(CBPeripheral *)p uuid:(NSString *)uuidStr
        instance:(int)inst {
    CBUUID *u = cn1btUuid(uuidStr);
    if (u == nil) {
        return nil;
    }
    NSArray *svcs = p.services;
    if (inst >= 0 && inst < (int)[svcs count]) {
        CBService *s = [svcs objectAtIndex:inst];
        if ([s.UUID isEqual:u]) {
            return s;
        }
    }
    for (CBService *s in svcs) {
        if ([s.UUID isEqual:u]) {
            return s;
        }
    }
    return nil;
}

- (CBCharacteristic *)charIn:(CBService *)s uuid:(NSString *)uuidStr
        instance:(int)inst {
    if (s == nil) {
        return nil;
    }
    CBUUID *u = cn1btUuid(uuidStr);
    if (u == nil) {
        return nil;
    }
    NSArray *chars = s.characteristics;
    if (inst >= 0 && inst < (int)[chars count]) {
        CBCharacteristic *c = [chars objectAtIndex:inst];
        if ([c.UUID isEqual:u]) {
            return c;
        }
    }
    for (CBCharacteristic *c in chars) {
        if ([c.UUID isEqual:u]) {
            return c;
        }
    }
    return nil;
}

- (CBDescriptor *)descIn:(CBCharacteristic *)c uuid:(NSString *)uuidStr {
    CBUUID *u = cn1btUuid(uuidStr);
    if (c == nil || u == nil) {
        return nil;
    }
    for (CBDescriptor *d in c.descriptors) {
        if ([d.UUID isEqual:u]) {
            return d;
        }
    }
    return nil;
}

/** "serviceIndex/charIndex" key for the pending-request dictionaries. */
- (NSString *)keyForChar:(CBCharacteristic *)c in:(CBPeripheral *)p {
    CBService *s = c.service;
    if (s == nil) {
        return nil;
    }
    NSUInteger si = [p.services indexOfObject:s];
    NSUInteger ci = [s.characteristics indexOfObject:c];
    if (si == NSNotFound || ci == NSNotFound) {
        return nil;
    }
    return [NSString stringWithFormat:@"%d/%d", (int)si, (int)ci];
}

- (NSString *)keyForDesc:(CBDescriptor *)d in:(CBPeripheral *)p {
    CBCharacteristic *c = d.characteristic;
    if (c == nil) {
        return nil;
    }
    NSString *ck = [self keyForChar:c in:p];
    if (ck == nil) {
        return nil;
    }
    return [NSString stringWithFormat:@"%@/%@", ck,
            [d.UUID.UUIDString uppercaseString]];
}

/** Fails every request pending on this peripheral (link went down). */
- (void)flushPendingOps:(CN1BtPeripheralEntry *)e code:(int)code
        msg:(NSString *)msg {
    NSArray *dicts = [NSArray arrayWithObjects:e.pendingReads,
            e.pendingWrites, e.pendingNotifyState, e.pendingDescReads,
            e.pendingDescWrites, nil];
    for (NSMutableDictionary *d in dicts) {
        for (NSNumber *rid in [d allValues]) {
            cn1btSendRequestError([rid intValue], code, msg);
        }
        [d removeAllObjects];
    }
    if (e.discoverRid > 0) {
        cn1btSendRequestError(e.discoverRid, code, msg);
        e.discoverRid = 0;
    }
    e.pendingCharDiscoveries = 0;
    e.pendingDescDiscoveries = 0;
    if (e.pendingRssiRid > 0) {
        cn1btSendRequestError(e.pendingRssiRid, code, msg);
        e.pendingRssiRid = 0;
    }
    for (NSDictionary *job in e.pendingL2capOpens) {
        cn1btSendRequestError([[job objectForKey:@"rid"] intValue], code,
                msg);
    }
    [e.pendingL2capOpens removeAllObjects];
}

// ---- central-role operations (run on the Bluetooth queue) -----------------

- (void)startScanUuids:(NSArray *)uuids duplicates:(BOOL)dup {
    [self ensureCentral];
    scanActive = YES;
    scanDuplicates = dup;
    if (scanServiceUuids != uuids) {
        [scanServiceUuids release];
        scanServiceUuids = [uuids retain];
    }
    if (central.state == CBManagerStatePoweredOn) {
        [self issueScan];
    }
    // otherwise centralManagerDidUpdateState issues the scan on poweredOn
}

- (void)issueScan {
    NSDictionary *opts = [NSDictionary dictionaryWithObject:
            [NSNumber numberWithBool:scanDuplicates]
            forKey:CBCentralManagerScanOptionAllowDuplicatesKey];
    [central scanForPeripheralsWithServices:scanServiceUuids options:opts];
}

- (void)stopScan {
    scanActive = NO;
    if (central != nil && central.state == CBManagerStatePoweredOn) {
        [central stopScan];
    }
}

- (NSString *)retrievePeripheral:(NSString *)pid {
    [self ensureCentral];
    NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:pid] autorelease];
    if (uuid == nil) {
        return nil;
    }
    NSArray *found = [central retrievePeripheralsWithIdentifiers:
            [NSArray arrayWithObject:uuid]];
    if ([found count] == 0) {
        return nil;
    }
    CBPeripheral *p = [found objectAtIndex:0];
    [self entryForPeripheral:p];
    return p.name != nil ? p.name : @"";
}

- (NSString *)connectedPeripheralsFor:(CBUUID *)serviceUuid {
    [self ensureCentral];
    NSArray *found = [central retrieveConnectedPeripheralsWithServices:
            [NSArray arrayWithObject:serviceUuid]];
    NSMutableString *out = [NSMutableString string];
    for (CBPeripheral *p in found) {
        [self entryForPeripheral:p];
        if ([out length] > 0) {
            [out appendString:@"\n"];
        }
        [out appendFormat:@"%@\t%@", p.identifier.UUIDString,
                p.name != nil ? p.name : @""];
    }
    return out;
}

- (void)connectPeripheral:(NSString *)pid {
    [self ensureCentral];
    CN1BtPeripheralEntry *e = [self entryForId:pid];
    if (e == nil) {
        // not seen in a scan; try resolving the persisted identifier
        NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:pid]
                autorelease];
        NSArray *found = uuid == nil ? nil
                : [central retrievePeripheralsWithIdentifiers:
                        [NSArray arrayWithObject:uuid]];
        if ([found count] > 0) {
            e = [self entryForPeripheral:[found objectAtIndex:0]];
        }
    }
    if (e == nil) {
        com_codename1_impl_ios_IOSBluetooth_nativeBtConnectFailed___java_lang_String_int_java_lang_String(
                getThreadLocalData(), cn1btJString(pid),
                CN1_BT_ERR_CONNECTION_FAILED,
                cn1btJString(@"Unknown peripheral identifier"));
        return;
    }
    [central connectPeripheral:e.peripheral options:nil];
}

- (void)disconnectPeripheral:(NSString *)pid {
    CN1BtPeripheralEntry *e = [self entryForId:pid];
    if (e != nil && central != nil) {
        [central cancelPeripheralConnection:e.peripheral];
    }
}

- (void)discoverServices:(int)requestId peripheral:(NSString *)pid {
    CN1BtPeripheralEntry *e = [self entryForId:pid];
    if (e == nil || e.peripheral.state != CBPeripheralStateConnected) {
        cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_CONNECTED,
                @"Peripheral is not connected");
        return;
    }
    if (e.discoverRid > 0) {
        cn1btSendRequestError(requestId, CN1_BT_ERR_UNKNOWN,
                @"Discovery already in progress");
        return;
    }
    e.discoverRid = requestId;
    e.pendingCharDiscoveries = 0;
    e.pendingDescDiscoveries = 0;
    [e.peripheral discoverServices:nil];
}

/** Resolves the addressed characteristic and hands it to `op`, or fails
 * the request. Shared by read/write/notify-arm/descriptor operations. */
- (void)withChar:(int)requestId peripheral:(NSString *)pid
        svcUuid:(NSString *)svcUuid svcInst:(int)svcInst
        charUuid:(NSString *)charUuid charInst:(int)charInst
        op:(CN1BtCharOp)op {
    CN1BtPeripheralEntry *e = [self entryForId:pid];
    if (e == nil || e.peripheral.state != CBPeripheralStateConnected) {
        cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_CONNECTED,
                @"Peripheral is not connected");
        return;
    }
    CBService *s = [self serviceIn:e.peripheral uuid:svcUuid
            instance:svcInst];
    CBCharacteristic *ch = [self charIn:s uuid:charUuid instance:charInst];
    NSString *key = ch != nil ? [self keyForChar:ch in:e.peripheral] : nil;
    if (ch == nil || key == nil) {
        cn1btSendRequestError(requestId, CN1_BT_ERR_GATT,
                @"Characteristic not found -- rerun discoverServices");
        return;
    }
    op(e, ch, key);
}

- (void)readRssi:(int)requestId peripheral:(NSString *)pid {
    CN1BtPeripheralEntry *e = [self entryForId:pid];
    if (e == nil || e.peripheral.state != CBPeripheralStateConnected) {
        cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_CONNECTED,
                @"Peripheral is not connected");
        return;
    }
    e.pendingRssiRid = requestId;
    [e.peripheral readRSSI];
}

- (int)maxWriteLength:(NSString *)pid withResponse:(BOOL)withResponse {
    CN1BtPeripheralEntry *e = [self entryForId:pid];
    if (e == nil || e.peripheral.state != CBPeripheralStateConnected) {
        return 0;
    }
    return (int)[e.peripheral maximumWriteValueLengthForType:
            (withResponse ? CBCharacteristicWriteWithResponse
                    : CBCharacteristicWriteWithoutResponse)];
}

- (void)openL2cap:(int)requestId peripheral:(NSString *)pid psm:(int)psm {
    CN1BtPeripheralEntry *e = [self entryForId:pid];
    if (e == nil || e.peripheral.state != CBPeripheralStateConnected) {
        cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_CONNECTED,
                @"Peripheral is not connected");
        return;
    }
    if (@available(iOS 11.0, tvOS 11.0, watchOS 4.0, *)) {
        NSMutableDictionary *job = [NSMutableDictionary dictionary];
        [job setObject:[NSNumber numberWithInt:requestId] forKey:@"rid"];
        [job setObject:[NSNumber numberWithInt:psm] forKey:@"psm"];
        [e.pendingL2capOpens addObject:job];
        [e.peripheral openL2CAPChannel:(CBL2CAPPSM)psm];
    } else {
        cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_SUPPORTED,
                @"L2CAP requires iOS 11+");
    }
}

// ---- CBCentralManagerDelegate ----------------------------------------------

- (void)centralManagerDidUpdateState:(CBCentralManager *)c {
    com_codename1_impl_ios_IOSBluetooth_nativeBtStateChanged___int_int(
            getThreadLocalData(), (int)c.state, cn1btAuthorizationValue());
    if (c.state == CBManagerStatePoweredOn && scanActive) {
        [self issueScan];
    }
}

- (void)centralManager:(CBCentralManager *)c
        didDiscoverPeripheral:(CBPeripheral *)peripheral
        advertisementData:(NSDictionary *)advertisementData
        RSSI:(NSNumber *)RSSI {
    @autoreleasepool {
        [self entryForPeripheral:peripheral];
        NSString *pid = peripheral.identifier.UUIDString;
        NSString *localName = [advertisementData objectForKey:
                CBAdvertisementDataLocalNameKey];
        NSMutableString *uuidsCsv = [NSMutableString string];
        NSArray *advUuids = [advertisementData objectForKey:
                CBAdvertisementDataServiceUUIDsKey];
        NSArray *overflow = [advertisementData objectForKey:
                CBAdvertisementDataOverflowServiceUUIDsKey];
        for (NSArray *list in [NSArray arrayWithObjects:advUuids, overflow,
                nil]) {
            for (CBUUID *u in list) {
                if ([uuidsCsv length] > 0) {
                    [uuidsCsv appendString:@","];
                }
                [uuidsCsv appendString:u.UUIDString];
            }
        }
        NSData *mfg = [advertisementData objectForKey:
                CBAdvertisementDataManufacturerDataKey];
        NSMutableString *svcDataCsv = [NSMutableString string];
        NSDictionary *svcData = [advertisementData objectForKey:
                CBAdvertisementDataServiceDataKey];
        for (CBUUID *u in svcData) {
            if ([svcDataCsv length] > 0) {
                [svcDataCsv appendString:@","];
            }
            [svcDataCsv appendFormat:@"%@=%@", u.UUIDString,
                    cn1btHexFromData([svcData objectForKey:u])];
        }
        NSNumber *tx = [advertisementData objectForKey:
                CBAdvertisementDataTxPowerLevelKey];
        NSNumber *connectable = [advertisementData objectForKey:
                CBAdvertisementDataIsConnectable];
        JAVA_OBJECT jPid = cn1btJString(pid);
        JAVA_OBJECT jName = cn1btJString(peripheral.name);
        JAVA_OBJECT jUuids = cn1btJString(uuidsCsv);
        JAVA_OBJECT jLocalName = cn1btJString(localName);
        JAVA_OBJECT jMfg = cn1btJBytes(mfg);
        JAVA_OBJECT jSvcData = cn1btJString(svcDataCsv);
        com_codename1_impl_ios_IOSBluetooth_nativeBtScanResult___java_lang_String_java_lang_String_int_boolean_java_lang_String_java_lang_String_byte_1ARRAY_java_lang_String_int(
                getThreadLocalData(), jPid, jName, [RSSI intValue],
                connectable == nil || [connectable boolValue] ? 1 : 0,
                jUuids, jLocalName, jMfg, jSvcData,
                tx != nil ? [tx intValue] : -999);
    }
}

- (void)centralManager:(CBCentralManager *)c
        didConnectPeripheral:(CBPeripheral *)peripheral {
    [self entryForPeripheral:peripheral];
    com_codename1_impl_ios_IOSBluetooth_nativeBtConnected___java_lang_String(
            getThreadLocalData(),
            cn1btJString(peripheral.identifier.UUIDString));
}

- (void)centralManager:(CBCentralManager *)c
        didFailToConnectPeripheral:(CBPeripheral *)peripheral
        error:(NSError *)error {
    com_codename1_impl_ios_IOSBluetooth_nativeBtConnectFailed___java_lang_String_int_java_lang_String(
            getThreadLocalData(),
            cn1btJString(peripheral.identifier.UUIDString),
            cn1btMapNSError(error, CN1_BT_ERR_CONNECTION_FAILED),
            cn1btJString(error != nil ? error.localizedDescription : nil));
}

- (void)centralManager:(CBCentralManager *)c
        didDisconnectPeripheral:(CBPeripheral *)peripheral
        error:(NSError *)error {
    CN1BtPeripheralEntry *e = [self entryForId:
            peripheral.identifier.UUIDString];
    if (e != nil) {
        [self flushPendingOps:e code:CN1_BT_ERR_NOT_CONNECTED
                msg:@"Peripheral disconnected"];
    }
    com_codename1_impl_ios_IOSBluetooth_nativeBtDisconnected___java_lang_String_int_java_lang_String(
            getThreadLocalData(),
            cn1btJString(peripheral.identifier.UUIDString),
            error != nil
                    ? cn1btMapNSError(error, CN1_BT_ERR_CONNECTION_LOST) : 0,
            cn1btJString(error != nil ? error.localizedDescription : nil));
}

// ---- CBPeripheralDelegate: discovery aggregation ----------------------------

- (void)peripheral:(CBPeripheral *)p didDiscoverServices:(NSError *)error {
    CN1BtPeripheralEntry *e = [self entryForId:p.identifier.UUIDString];
    if (e == nil || e.discoverRid <= 0) {
        return;
    }
    if (error != nil) {
        int rid = e.discoverRid;
        e.discoverRid = 0;
        cn1btSendNSError(rid, error, CN1_BT_ERR_GATT);
        return;
    }
    NSArray *svcs = p.services;
    e.pendingCharDiscoveries = (int)[svcs count];
    e.pendingDescDiscoveries = 0;
    if ([svcs count] == 0) {
        [self maybeEmitGattDb:e];
        return;
    }
    for (CBService *s in svcs) {
        [p discoverCharacteristics:nil forService:s];
    }
}

- (void)peripheral:(CBPeripheral *)p
        didDiscoverCharacteristicsForService:(CBService *)service
        error:(NSError *)error {
    CN1BtPeripheralEntry *e = [self entryForId:p.identifier.UUIDString];
    if (e == nil || e.discoverRid <= 0) {
        return;
    }
    e.pendingCharDiscoveries--;
    if (error == nil) {
        for (CBCharacteristic *c in service.characteristics) {
            e.pendingDescDiscoveries++;
            [p discoverDescriptorsForCharacteristic:c];
        }
    }
    [self maybeEmitGattDb:e];
}

- (void)peripheral:(CBPeripheral *)p
        didDiscoverDescriptorsForCharacteristic:(CBCharacteristic *)c
        error:(NSError *)error {
    CN1BtPeripheralEntry *e = [self entryForId:p.identifier.UUIDString];
    if (e == nil || e.discoverRid <= 0) {
        return;
    }
    e.pendingDescDiscoveries--;
    [self maybeEmitGattDb:e];
}

/** Emits the aggregated GATT database once every characteristic and
 * descriptor discovery round-trip finished. Instance ids are array
 * indices, mirrored by the lookup helpers above. */
- (void)maybeEmitGattDb:(CN1BtPeripheralEntry *)e {
    if (e.discoverRid <= 0 || e.pendingCharDiscoveries > 0
            || e.pendingDescDiscoveries > 0) {
        return;
    }
    @autoreleasepool {
        CBPeripheral *p = e.peripheral;
        NSMutableString *db = [NSMutableString string];
        NSArray *svcs = p.services;
        for (NSUInteger si = 0; si < [svcs count]; si++) {
            CBService *s = [svcs objectAtIndex:si];
            [db appendFormat:@"S|%@|%d|%d\n", s.UUID.UUIDString,
                    s.isPrimary ? 1 : 0, (int)si];
            NSArray *chars = s.characteristics;
            for (NSUInteger ci = 0; ci < [chars count]; ci++) {
                CBCharacteristic *c = [chars objectAtIndex:ci];
                [db appendFormat:@"C|%@|%d|%d\n", c.UUID.UUIDString,
                        (int)c.properties, (int)ci];
                for (CBDescriptor *d in c.descriptors) {
                    [db appendFormat:@"D|%@\n", d.UUID.UUIDString];
                }
            }
        }
        int rid = e.discoverRid;
        e.discoverRid = 0;
        com_codename1_impl_ios_IOSBluetooth_nativeBtServicesDiscovered___int_java_lang_String_java_lang_String(
                getThreadLocalData(), rid,
                cn1btJString(p.identifier.UUIDString), cn1btJString(db));
    }
}

// ---- CBPeripheralDelegate: GATT client callbacks ----------------------------

- (void)peripheral:(CBPeripheral *)p
        didUpdateValueForCharacteristic:(CBCharacteristic *)c
        error:(NSError *)error {
    CN1BtPeripheralEntry *e = [self entryForId:p.identifier.UUIDString];
    if (e == nil) {
        return;
    }
    NSString *key = [self keyForChar:c in:p];
    NSNumber *rid = key != nil ? [e.pendingReads objectForKey:key] : nil;
    if (rid != nil) {
        // response to an app-issued read
        [e.pendingReads removeObjectForKey:key];
        if (error != nil) {
            cn1btSendNSError([rid intValue], error, CN1_BT_ERR_GATT);
        } else {
            com_codename1_impl_ios_IOSBluetooth_nativeBtValue___int_byte_1ARRAY(
                    getThreadLocalData(), [rid intValue],
                    cn1btJBytes(c.value));
        }
        return;
    }
    if (error != nil || key == nil) {
        return;
    }
    // unsolicited -> notification / indication
    CBService *s = c.service;
    NSUInteger si = [p.services indexOfObject:s];
    NSUInteger ci = [s.characteristics indexOfObject:c];
    com_codename1_impl_ios_IOSBluetooth_nativeBtNotification___java_lang_String_java_lang_String_int_java_lang_String_int_byte_1ARRAY(
            getThreadLocalData(),
            cn1btJString(p.identifier.UUIDString),
            cn1btJString(s.UUID.UUIDString), (int)si,
            cn1btJString(c.UUID.UUIDString), (int)ci,
            cn1btJBytes(c.value));
}

- (void)peripheral:(CBPeripheral *)p
        didWriteValueForCharacteristic:(CBCharacteristic *)c
        error:(NSError *)error {
    CN1BtPeripheralEntry *e = [self entryForId:p.identifier.UUIDString];
    NSString *key = [self keyForChar:c in:p];
    NSNumber *rid = e != nil && key != nil
            ? [e.pendingWrites objectForKey:key] : nil;
    if (rid == nil) {
        return;
    }
    [e.pendingWrites removeObjectForKey:key];
    if (error != nil) {
        cn1btSendNSError([rid intValue], error, CN1_BT_ERR_GATT);
    } else {
        cn1btSendOpComplete([rid intValue]);
    }
}

- (void)peripheral:(CBPeripheral *)p
        didUpdateNotificationStateForCharacteristic:(CBCharacteristic *)c
        error:(NSError *)error {
    CN1BtPeripheralEntry *e = [self entryForId:p.identifier.UUIDString];
    NSString *key = [self keyForChar:c in:p];
    NSNumber *rid = e != nil && key != nil
            ? [e.pendingNotifyState objectForKey:key] : nil;
    if (rid == nil) {
        return;
    }
    [e.pendingNotifyState removeObjectForKey:key];
    if (error != nil) {
        cn1btSendNSError([rid intValue], error, CN1_BT_ERR_GATT);
    } else {
        cn1btSendOpComplete([rid intValue]);
    }
}

- (void)peripheral:(CBPeripheral *)p
        didUpdateValueForDescriptor:(CBDescriptor *)d
        error:(NSError *)error {
    CN1BtPeripheralEntry *e = [self entryForId:p.identifier.UUIDString];
    NSString *key = [self keyForDesc:d in:p];
    NSNumber *rid = e != nil && key != nil
            ? [e.pendingDescReads objectForKey:key] : nil;
    if (rid == nil) {
        return;
    }
    [e.pendingDescReads removeObjectForKey:key];
    if (error != nil) {
        cn1btSendNSError([rid intValue], error, CN1_BT_ERR_GATT);
        return;
    }
    // descriptor values surface as NSData / NSString / NSNumber depending
    // on the UUID; normalize to bytes (numbers little-endian, 2 bytes)
    NSData *data = nil;
    id v = d.value;
    if ([v isKindOfClass:[NSData class]]) {
        data = v;
    } else if ([v isKindOfClass:[NSString class]]) {
        data = [(NSString *)v dataUsingEncoding:NSUTF8StringEncoding];
    } else if ([v isKindOfClass:[NSNumber class]]) {
        unsigned short n = [(NSNumber *)v unsignedShortValue];
        unsigned char b[2] = {(unsigned char)(n & 0xFF),
                (unsigned char)((n >> 8) & 0xFF)};
        data = [NSData dataWithBytes:b length:2];
    }
    com_codename1_impl_ios_IOSBluetooth_nativeBtValue___int_byte_1ARRAY(
            getThreadLocalData(), [rid intValue], cn1btJBytes(data));
}

- (void)peripheral:(CBPeripheral *)p
        didWriteValueForDescriptor:(CBDescriptor *)d
        error:(NSError *)error {
    CN1BtPeripheralEntry *e = [self entryForId:p.identifier.UUIDString];
    NSString *key = [self keyForDesc:d in:p];
    NSNumber *rid = e != nil && key != nil
            ? [e.pendingDescWrites objectForKey:key] : nil;
    if (rid == nil) {
        return;
    }
    [e.pendingDescWrites removeObjectForKey:key];
    if (error != nil) {
        cn1btSendNSError([rid intValue], error, CN1_BT_ERR_GATT);
    } else {
        cn1btSendOpComplete([rid intValue]);
    }
}

- (void)peripheral:(CBPeripheral *)p didReadRSSI:(NSNumber *)RSSI
        error:(NSError *)error {
    CN1BtPeripheralEntry *e = [self entryForId:p.identifier.UUIDString];
    if (e == nil || e.pendingRssiRid <= 0) {
        return;
    }
    int rid = e.pendingRssiRid;
    e.pendingRssiRid = 0;
    if (error != nil) {
        cn1btSendNSError(rid, error, CN1_BT_ERR_GATT);
    } else {
        com_codename1_impl_ios_IOSBluetooth_nativeBtRssi___int_int(
                getThreadLocalData(), rid, [RSSI intValue]);
    }
}

- (void)peripheral:(CBPeripheral *)p
        didModifyServices:(NSArray *)invalidatedServices {
    com_codename1_impl_ios_IOSBluetooth_nativeBtServicesInvalidated___java_lang_String(
            getThreadLocalData(),
            cn1btJString(p.identifier.UUIDString));
}

- (void)peripheral:(CBPeripheral *)p
        didOpenL2CAPChannel:(CBL2CAPChannel *)channel
        error:(NSError *)error {
    CN1BtPeripheralEntry *e = [self entryForId:p.identifier.UUIDString];
    if (e == nil || [e.pendingL2capOpens count] == 0) {
        return;
    }
    if (error != nil || channel == nil) {
        NSDictionary *job = [e.pendingL2capOpens objectAtIndex:0];
        int rid = [[job objectForKey:@"rid"] intValue];
        [e.pendingL2capOpens removeObjectAtIndex:0];
        cn1btSendNSError(rid, error, CN1_BT_ERR_IO);
        return;
    }
    if (@available(iOS 11.0, tvOS 11.0, watchOS 4.0, *)) {
        int psm = (int)channel.PSM;
        NSUInteger matchIdx = NSNotFound;
        for (NSUInteger i = 0; i < [e.pendingL2capOpens count]; i++) {
            NSDictionary *job = [e.pendingL2capOpens objectAtIndex:i];
            if ([[job objectForKey:@"psm"] intValue] == psm) {
                matchIdx = i;
                break;
            }
        }
        if (matchIdx == NSNotFound) {
            matchIdx = 0;
        }
        NSDictionary *job = [e.pendingL2capOpens objectAtIndex:matchIdx];
        int rid = [[job objectForKey:@"rid"] intValue];
        [e.pendingL2capOpens removeObjectAtIndex:matchIdx];
        long long handle = cn1btRegisterL2capChannel(channel);
        com_codename1_impl_ios_IOSBluetooth_nativeBtL2capOpened___int_int_long(
                getThreadLocalData(), rid, psm, (JAVA_LONG)handle);
    }
}

#ifdef CN1_BT_PERIPHERAL_ROLE

// ---- peripheral-role operations (run on the Bluetooth queue) ---------------

- (void)ensurePeripheralManager {
    if (peripheralMgr == nil) {
        peripheralMgr = [[CBPeripheralManager alloc] initWithDelegate:self
                queue:cn1btGetQueue()];
    }
}

- (int)localIdForCharacteristic:(CBCharacteristic *)c {
    for (NSNumber *k in localChars) {
        if ([localChars objectForKey:k] == (id)c) {
            return [k intValue];
        }
    }
    return -1;
}

- (long long)stashAttRequest:(CBATTRequest *)rq
        batch:(NSMutableDictionary *)batch {
    long long h = ++nextAttHandle;
    NSMutableDictionary *e = [NSMutableDictionary dictionary];
    [e setObject:rq forKey:@"req"];
    if (batch != nil) {
        [e setObject:batch forKey:@"batch"];
    }
    [attRequests setObject:e forKey:[NSNumber numberWithLongLong:h]];
    return h;
}

/** YES when the notify job was consumed (delivered or failed for good);
 * NO when CoreBluetooth's update queue is full and it must be retried from
 * peripheralManagerIsReadyToUpdateSubscribers. */
- (BOOL)processNotifyJob:(NSDictionary *)job {
    int rid = [[job objectForKey:@"rid"] intValue];
    CBMutableCharacteristic *ch = [localChars objectForKey:
            [job objectForKey:@"cid"]];
    if (ch == nil) {
        cn1btSendRequestError(rid, CN1_BT_ERR_GATT,
                @"Characteristic is no longer registered");
        return YES;
    }
    NSArray *targets = nil;
    id centralId = [job objectForKey:@"central"];
    if (centralId != nil && centralId != (id)[NSNull null]) {
        CBCentral *target = [subscribedCentrals objectForKey:centralId];
        if (target == nil) {
            cn1btSendRequestError(rid, CN1_BT_ERR_NOT_CONNECTED,
                    @"Central is not subscribed");
            return YES;
        }
        targets = [NSArray arrayWithObject:target];
    }
    BOOL ok = [peripheralMgr updateValue:[job objectForKey:@"data"]
            forCharacteristic:ch onSubscribedCentrals:targets];
    if (ok) {
        cn1btSendOpComplete(rid);
        return YES;
    }
    return NO;
}

- (void)openGattServer:(int)requestId {
    [self ensurePeripheralManager];
    if (peripheralMgr.state == CBManagerStatePoweredOn) {
        com_codename1_impl_ios_IOSBluetooth_nativeBtGattServerOpened___int(
                getThreadLocalData(), requestId);
    } else if (peripheralMgr.state == CBManagerStateUnsupported) {
        cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_SUPPORTED,
                @"BLE peripheral role unsupported");
    } else if (peripheralMgr.state == CBManagerStateUnauthorized) {
        cn1btSendRequestError(requestId, CN1_BT_ERR_UNAUTHORIZED,
                @"Bluetooth permission denied");
    } else if (peripheralMgr.state == CBManagerStatePoweredOff) {
        cn1btSendRequestError(requestId, CN1_BT_ERR_POWERED_OFF,
                @"Bluetooth is powered off");
    } else {
        // unknown / resetting: wait for peripheralManagerDidUpdateState
        [pendingServerOpenRids addObject:
                [NSNumber numberWithInt:requestId]];
    }
}

/** Maps the Java GattLocalCharacteristic permission bits to
 * CBAttributePermissions. */
+ (CBAttributePermissions)mapPermissions:(int)javaPerms {
    CBAttributePermissions p = 0;
    if (javaPerms & 0x01) {
        p |= CBAttributePermissionsReadable;
    }
    if (javaPerms & 0x02) {
        p |= CBAttributePermissionsReadEncryptionRequired;
    }
    if (javaPerms & 0x10) {
        p |= CBAttributePermissionsWriteable;
    }
    if (javaPerms & 0x20) {
        p |= CBAttributePermissionsWriteEncryptionRequired;
    }
    if (p == 0) {
        p = CBAttributePermissionsReadable;
    }
    return p;
}

/** Parses the S|/C|/D| definition produced by IOSGattServer.doAddService
 * and registers the CBMutableService. Static characteristic values are
 * NOT handed to CoreBluetooth (its cached-value mode forces read-only
 * characteristics); they are kept in `staticValues` and served from
 * didReceiveReadRequest without a Java round trip. */
- (void)addService:(int)requestId definition:(NSString *)def {
    @autoreleasepool {
        if (peripheralMgr == nil
                || peripheralMgr.state != CBManagerStatePoweredOn) {
            cn1btSendRequestError(requestId, CN1_BT_ERR_UNKNOWN,
                    @"GATT server is not open");
            return;
        }
        CBMutableService *svc = nil;
        int sid = 0;
        NSMutableArray *chars = [NSMutableArray array];
        CBMutableCharacteristic *curChar = nil;
        NSMutableArray *curDescs = nil;
        for (NSString *line in [def componentsSeparatedByString:@"\n"]) {
            NSArray *f = [line componentsSeparatedByString:@"|"];
            if ([f count] < 2) {
                continue;
            }
            NSString *kind = [f objectAtIndex:0];
            if ([kind isEqualToString:@"S"] && [f count] >= 4) {
                sid = [[f objectAtIndex:1] intValue];
                CBUUID *u = cn1btUuid([f objectAtIndex:2]);
                if (u == nil) {
                    break;
                }
                svc = [[[CBMutableService alloc] initWithType:u
                        primary:[[f objectAtIndex:3] intValue] != 0]
                        autorelease];
            } else if ([kind isEqualToString:@"C"] && [f count] >= 5
                    && svc != nil) {
                if (curChar != nil && [curDescs count] > 0) {
                    curChar.descriptors = curDescs;
                }
                int cid = [[f objectAtIndex:1] intValue];
                CBUUID *u = cn1btUuid([f objectAtIndex:2]);
                if (u == nil) {
                    curChar = nil;
                    curDescs = nil;
                    continue;
                }
                int props = [[f objectAtIndex:3] intValue];
                int perms = [[f objectAtIndex:4] intValue];
                NSData *sv = [f count] >= 6
                        ? cn1btDataFromHex([f objectAtIndex:5]) : nil;
                curChar = [[[CBMutableCharacteristic alloc]
                        initWithType:u
                        properties:(CBCharacteristicProperties)props
                        value:nil
                        permissions:[CN1BluetoothController
                                mapPermissions:perms]] autorelease];
                curDescs = [NSMutableArray array];
                [chars addObject:curChar];
                NSNumber *cidKey = [NSNumber numberWithInt:cid];
                [localChars setObject:curChar forKey:cidKey];
                if (sv != nil) {
                    [staticValues setObject:sv forKey:cidKey];
                }
            } else if ([kind isEqualToString:@"D"] && [f count] >= 4
                    && curChar != nil) {
                CBUUID *u = cn1btUuid([f objectAtIndex:2]);
                NSData *dv = [f count] >= 5
                        ? cn1btDataFromHex([f objectAtIndex:4]) : nil;
                if (u != nil) {
                    @try {
                        // CBMutableDescriptor supports a limited UUID set
                        // and requires a static value
                        CBMutableDescriptor *d =
                                [[[CBMutableDescriptor alloc]
                                initWithType:u
                                value:(dv != nil ? (id)dv
                                        : (id)[NSData data])] autorelease];
                        [curDescs addObject:d];
                    } @catch (NSException *ex) {
                        // unsupported descriptor type -- skip it
                    }
                }
            }
        }
        if (curChar != nil && [curDescs count] > 0) {
            curChar.descriptors = curDescs;
        }
        if (svc == nil) {
            cn1btSendRequestError(requestId, CN1_BT_ERR_UNKNOWN,
                    @"Malformed service definition");
            return;
        }
        svc.characteristics = chars;
        [localServices setObject:svc forKey:[NSNumber numberWithInt:sid]];
        [pendingAddServiceRids setObject:[NSNumber numberWithInt:requestId]
                forKey:[NSNumber numberWithUnsignedLongLong:
                        (unsigned long long)(uintptr_t)svc]];
        [peripheralMgr addService:svc];
    }
}

- (void)removeServiceById:(int)serviceLocalId {
    NSNumber *key = [NSNumber numberWithInt:serviceLocalId];
    CBMutableService *svc = [localServices objectForKey:key];
    if (svc == nil) {
        return;
    }
    // drop the char registrations belonging to this service
    NSMutableArray *toRemove = [NSMutableArray array];
    for (NSNumber *cidKey in localChars) {
        id ch = [localChars objectForKey:cidKey];
        if ([svc.characteristics containsObject:ch]) {
            [toRemove addObject:cidKey];
        }
    }
    for (NSNumber *cidKey in toRemove) {
        [localChars removeObjectForKey:cidKey];
        [staticValues removeObjectForKey:cidKey];
    }
    if (peripheralMgr != nil) {
        [peripheralMgr removeService:svc];
    }
    [localServices removeObjectForKey:key];
}

- (void)closeGattServer {
    if (peripheralMgr != nil) {
        [peripheralMgr removeAllServices];
    }
    [localServices removeAllObjects];
    [localChars removeAllObjects];
    [staticValues removeAllObjects];
    [pendingNotifies removeAllObjects];
}

- (void)startAdvertising:(int)requestId name:(NSString *)name
        uuids:(NSArray *)uuids {
    [self ensurePeripheralManager];
    if (pendingAdvertiseRid > 0) {
        cn1btSendRequestError(requestId, CN1_BT_ERR_ADVERTISE_FAILED,
                @"An advertising start is already pending");
        return;
    }
    NSMutableDictionary *ad = [NSMutableDictionary dictionary];
    if (uuids != nil) {
        [ad setObject:uuids forKey:CBAdvertisementDataServiceUUIDsKey];
    }
    if (name != nil) {
        NSString *resolved = name;
        if ([resolved length] == 0) {
            resolved = [UIDevice currentDevice].name;
        }
        if (resolved != nil) {
            [ad setObject:resolved forKey:CBAdvertisementDataLocalNameKey];
        }
    }
    pendingAdvertiseRid = requestId;
    [pendingAdvertiseData release];
    pendingAdvertiseData = [ad retain];
    if (peripheralMgr.state == CBManagerStatePoweredOn) {
        advertiseSubmitted = YES;
        [peripheralMgr startAdvertising:ad];
    } else {
        advertiseSubmitted = NO;
        // peripheralManagerDidUpdateState starts it on poweredOn
    }
}

- (void)stopAdvertising {
    if (peripheralMgr != nil) {
        [peripheralMgr stopAdvertising];
    }
    pendingAdvertiseRid = 0;
    advertiseSubmitted = NO;
    [pendingAdvertiseData release];
    pendingAdvertiseData = nil;
}

- (void)notifyValue:(int)requestId charId:(int)charLocalId
        data:(NSData *)data central:(NSString *)centralId {
    if (peripheralMgr == nil
            || peripheralMgr.state != CBManagerStatePoweredOn) {
        cn1btSendRequestError(requestId, CN1_BT_ERR_UNKNOWN,
                @"GATT server is not open");
        return;
    }
    NSMutableDictionary *job = [NSMutableDictionary dictionary];
    [job setObject:[NSNumber numberWithInt:requestId] forKey:@"rid"];
    [job setObject:[NSNumber numberWithInt:charLocalId] forKey:@"cid"];
    [job setObject:data forKey:@"data"];
    [job setObject:(centralId != nil ? (id)centralId : (id)[NSNull null])
            forKey:@"central"];
    if (![self processNotifyJob:job]) {
        [pendingNotifies addObject:job];
    }
}

- (void)respondToRead:(long long)handle data:(NSData *)data
        status:(int)attStatus {
    NSNumber *key = [NSNumber numberWithLongLong:handle];
    NSDictionary *entry = [[[attRequests objectForKey:key] retain]
            autorelease];
    if (entry == nil || peripheralMgr == nil) {
        return;
    }
    [attRequests removeObjectForKey:key];
    CBATTRequest *rq = [entry objectForKey:@"req"];
    if (attStatus != 0) {
        [peripheralMgr respondToRequest:rq
                withResult:(CBATTError)attStatus];
    } else {
        rq.value = data != nil ? data : [NSData data];
        [peripheralMgr respondToRequest:rq withResult:CBATTErrorSuccess];
    }
}

- (void)respondToBatch:(NSMutableDictionary *)batch status:(int)attStatus {
    if ([[batch objectForKey:@"responded"] boolValue]) {
        return;
    }
    CBATTRequest *first = [batch objectForKey:@"first"];
    if (attStatus != 0) {
        [batch setObject:[NSNumber numberWithBool:YES]
                forKey:@"responded"];
        [peripheralMgr respondToRequest:first
                withResult:(CBATTError)attStatus];
        return;
    }
    int remaining = [[batch objectForKey:@"remaining"] intValue] - 1;
    [batch setObject:[NSNumber numberWithInt:remaining]
            forKey:@"remaining"];
    if (remaining <= 0) {
        [batch setObject:[NSNumber numberWithBool:YES]
                forKey:@"responded"];
        [peripheralMgr respondToRequest:first withResult:CBATTErrorSuccess];
    }
}

- (void)respondToWrite:(long long)handle status:(int)attStatus {
    NSNumber *key = [NSNumber numberWithLongLong:handle];
    NSDictionary *entry = [[[attRequests objectForKey:key] retain]
            autorelease];
    if (entry == nil || peripheralMgr == nil) {
        return;
    }
    [attRequests removeObjectForKey:key];
    NSMutableDictionary *batch = [entry objectForKey:@"batch"];
    if (batch != nil) {
        [self respondToBatch:batch status:attStatus];
    } else {
        CBATTRequest *rq = [entry objectForKey:@"req"];
        [peripheralMgr respondToRequest:rq withResult:(attStatus != 0
                ? (CBATTError)attStatus : CBATTErrorSuccess)];
    }
}

- (void)publishL2cap:(int)requestId secure:(BOOL)secure {
    [self ensurePeripheralManager];
    if (@available(iOS 11.0, *)) {
        if (peripheralMgr.state == CBManagerStatePoweredOn) {
            [publishAwaitingCallback addObject:
                    [NSNumber numberWithInt:requestId]];
            [peripheralMgr publishL2CAPChannelWithEncryption:secure];
        } else {
            NSMutableDictionary *job = [NSMutableDictionary dictionary];
            [job setObject:[NSNumber numberWithInt:requestId]
                    forKey:@"rid"];
            [job setObject:[NSNumber numberWithBool:secure]
                    forKey:@"secure"];
            [pendingPublishRequests addObject:job];
        }
    } else {
        cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_SUPPORTED,
                @"L2CAP requires iOS 11+");
    }
}

- (void)unpublishL2cap:(int)psm {
    if (peripheralMgr != nil) {
        if (@available(iOS 11.0, *)) {
            [peripheralMgr unpublishL2CAPChannel:(CBL2CAPPSM)psm];
        }
    }
}

// ---- CBPeripheralManagerDelegate ---------------------------------------------

- (void)peripheralManagerDidUpdateState:(CBPeripheralManager *)mgr {
    CBManagerState state = mgr.state;
    // also forward through the adapter-state pipe so permission requests
    // resolve even when only the peripheral role was used
    com_codename1_impl_ios_IOSBluetooth_nativeBtStateChanged___int_int(
            getThreadLocalData(), (int)state, cn1btAuthorizationValue());
    if (state == CBManagerStatePoweredOn) {
        for (NSNumber *rid in pendingServerOpenRids) {
            com_codename1_impl_ios_IOSBluetooth_nativeBtGattServerOpened___int(
                    getThreadLocalData(), [rid intValue]);
        }
        [pendingServerOpenRids removeAllObjects];
        if (pendingAdvertiseRid > 0 && !advertiseSubmitted
                && pendingAdvertiseData != nil) {
            advertiseSubmitted = YES;
            [mgr startAdvertising:pendingAdvertiseData];
        }
        if (@available(iOS 11.0, *)) {
            for (NSDictionary *job in pendingPublishRequests) {
                [publishAwaitingCallback addObject:
                        [job objectForKey:@"rid"]];
                [mgr publishL2CAPChannelWithEncryption:
                        [[job objectForKey:@"secure"] boolValue]];
            }
        }
        [pendingPublishRequests removeAllObjects];
        return;
    }
    int code;
    if (state == CBManagerStateUnsupported) {
        code = CN1_BT_ERR_NOT_SUPPORTED;
    } else if (state == CBManagerStateUnauthorized) {
        code = CN1_BT_ERR_UNAUTHORIZED;
    } else if (state == CBManagerStatePoweredOff) {
        code = CN1_BT_ERR_POWERED_OFF;
    } else {
        return; // unknown / resetting: wait for a definitive state
    }
    NSString *msg = [NSString stringWithFormat:
            @"Bluetooth peripheral manager state %d", (int)state];
    for (NSNumber *rid in pendingServerOpenRids) {
        cn1btSendRequestError([rid intValue], code, msg);
    }
    [pendingServerOpenRids removeAllObjects];
    if (pendingAdvertiseRid > 0 && !advertiseSubmitted) {
        cn1btSendRequestError(pendingAdvertiseRid, code, msg);
        pendingAdvertiseRid = 0;
        [pendingAdvertiseData release];
        pendingAdvertiseData = nil;
    }
    for (NSDictionary *job in pendingPublishRequests) {
        cn1btSendRequestError([[job objectForKey:@"rid"] intValue], code,
                msg);
    }
    [pendingPublishRequests removeAllObjects];
}

- (void)peripheralManager:(CBPeripheralManager *)mgr
        didAddService:(CBService *)service error:(NSError *)error {
    NSNumber *key = [NSNumber numberWithUnsignedLongLong:
            (unsigned long long)(uintptr_t)service];
    NSNumber *rid = [pendingAddServiceRids objectForKey:key];
    if (rid == nil) {
        return;
    }
    [pendingAddServiceRids removeObjectForKey:key];
    if (error != nil) {
        cn1btSendNSError([rid intValue], error, CN1_BT_ERR_GATT);
    } else {
        cn1btSendOpComplete([rid intValue]);
    }
}

- (void)peripheralManagerDidStartAdvertising:(CBPeripheralManager *)mgr
        error:(NSError *)error {
    int rid = pendingAdvertiseRid;
    pendingAdvertiseRid = 0;
    advertiseSubmitted = NO;
    [pendingAdvertiseData release];
    pendingAdvertiseData = nil;
    if (rid <= 0) {
        return;
    }
    if (error != nil) {
        cn1btSendNSError(rid, error, CN1_BT_ERR_ADVERTISE_FAILED);
    } else {
        com_codename1_impl_ios_IOSBluetooth_nativeBtAdvertiseStarted___int(
                getThreadLocalData(), rid);
    }
}

- (void)peripheralManager:(CBPeripheralManager *)mgr
        didReceiveReadRequest:(CBATTRequest *)request {
    int cid = [self localIdForCharacteristic:request.characteristic];
    if (cid < 0) {
        [mgr respondToRequest:request
                withResult:CBATTErrorAttributeNotFound];
        return;
    }
    // serve app-supplied static values without a Java round trip,
    // matching the Android port's static-value behavior
    NSData *sv = [staticValues objectForKey:[NSNumber numberWithInt:cid]];
    if (sv != nil) {
        if (request.offset > [sv length]) {
            [mgr respondToRequest:request
                    withResult:CBATTErrorInvalidOffset];
        } else {
            request.value = [sv subdataWithRange:NSMakeRange(request.offset,
                    [sv length] - request.offset)];
            [mgr respondToRequest:request withResult:CBATTErrorSuccess];
        }
        return;
    }
    long long h = [self stashAttRequest:request batch:nil];
    com_codename1_impl_ios_IOSBluetooth_nativeBtReadRequest___long_java_lang_String_int_int_int(
            getThreadLocalData(), (JAVA_LONG)h,
            cn1btJString(request.central.identifier.UUIDString), cid, -1,
            (int)request.offset);
}

- (void)peripheralManager:(CBPeripheralManager *)mgr
        didReceiveWriteRequests:(NSArray *)requests {
    if ([requests count] == 0) {
        return;
    }
    // Apple contract: respond exactly once, to the first request, covering
    // the whole batch. The batch dict tracks outstanding per-request
    // responses from Java; the first error (or the last success) answers.
    NSMutableDictionary *batch = [NSMutableDictionary dictionary];
    [batch setObject:[requests objectAtIndex:0] forKey:@"first"];
    [batch setObject:[NSNumber numberWithInt:(int)[requests count]]
            forKey:@"remaining"];
    [batch setObject:[NSNumber numberWithBool:NO] forKey:@"responded"];
    for (CBATTRequest *rq in requests) {
        int cid = [self localIdForCharacteristic:rq.characteristic];
        long long h = [self stashAttRequest:rq batch:batch];
        com_codename1_impl_ios_IOSBluetooth_nativeBtWriteRequest___long_java_lang_String_int_int_byte_1ARRAY_int_boolean(
                getThreadLocalData(), (JAVA_LONG)h,
                cn1btJString(rq.central.identifier.UUIDString), cid, -1,
                cn1btJBytes(rq.value), (int)rq.offset, 1);
    }
}

- (void)peripheralManager:(CBPeripheralManager *)mgr
        central:(CBCentral *)c
        didSubscribeToCharacteristic:(CBCharacteristic *)characteristic {
    NSString *centralId = c.identifier.UUIDString;
    [subscribedCentrals setObject:c forKey:centralId];
    int cid = [self localIdForCharacteristic:characteristic];
    if (cid < 0) {
        return;
    }
    com_codename1_impl_ios_IOSBluetooth_nativeBtSubscriptionChanged___java_lang_String_int_int_boolean(
            getThreadLocalData(), cn1btJString(centralId),
            (int)c.maximumUpdateValueLength + 3, cid, 1);
}

- (void)peripheralManager:(CBPeripheralManager *)mgr
        central:(CBCentral *)c
        didUnsubscribeFromCharacteristic:(CBCharacteristic *)characteristic {
    int cid = [self localIdForCharacteristic:characteristic];
    if (cid < 0) {
        return;
    }
    com_codename1_impl_ios_IOSBluetooth_nativeBtSubscriptionChanged___java_lang_String_int_int_boolean(
            getThreadLocalData(),
            cn1btJString(c.identifier.UUIDString),
            (int)c.maximumUpdateValueLength + 3, cid, 0);
}

- (void)peripheralManagerIsReadyToUpdateSubscribers:
        (CBPeripheralManager *)mgr {
    while ([pendingNotifies count] > 0) {
        NSDictionary *job = [pendingNotifies objectAtIndex:0];
        if (![self processNotifyJob:job]) {
            break; // transmit queue filled up again; wait for next ready
        }
        [pendingNotifies removeObjectAtIndex:0];
    }
}

- (void)peripheralManager:(CBPeripheralManager *)mgr
        didPublishL2CAPChannel:(CBL2CAPPSM)psm error:(NSError *)error {
    if ([publishAwaitingCallback count] == 0) {
        return;
    }
    int rid = [[publishAwaitingCallback objectAtIndex:0] intValue];
    [publishAwaitingCallback removeObjectAtIndex:0];
    if (error != nil) {
        cn1btSendNSError(rid, error, CN1_BT_ERR_IO);
        return;
    }
    com_codename1_impl_ios_IOSBluetooth_nativeBtL2capPublished___int_int(
            getThreadLocalData(), rid, (int)psm);
}

- (void)peripheralManager:(CBPeripheralManager *)mgr
        didOpenL2CAPChannel:(CBL2CAPChannel *)channel
        error:(NSError *)error {
    if (error != nil || channel == nil) {
        return; // nothing to route -- the central side sees its own error
    }
    if (@available(iOS 11.0, *)) {
        long long handle = cn1btRegisterL2capChannel(channel);
        com_codename1_impl_ios_IOSBluetooth_nativeBtL2capIncoming___int_long(
                getThreadLocalData(), (int)channel.PSM, (JAVA_LONG)handle);
    }
}

#endif // CN1_BT_PERIPHERAL_ROLE

@end

// --------------------------------------------------------------------------
// IOSNative trampolines (CN1_INCLUDE_BLUETOOTH enabled)

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isBlePeripheralSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_getBluetoothAuthorization___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return cn1btAuthorizationValue();
}

void com_codename1_impl_ios_IOSNative_startBluetoothStateMonitor__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    cn1btAsync(^{
        [cn1btGetController() ensureCentral];
    });
}

void com_codename1_impl_ios_IOSNative_btStartScan___java_lang_String_1ARRAY_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT serviceUuids,
        JAVA_BOOLEAN allowDuplicates) {
    NSArray *uuids = cn1btUuidArray(threadStateData, serviceUuids);
    BOOL dup = allowDuplicates ? YES : NO;
    cn1btAsync(^{
        [cn1btGetController() startScanUuids:uuids duplicates:dup];
    });
}

void com_codename1_impl_ios_IOSNative_btStopScan__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    cn1btAsync(^{
        [cn1btGetController() stopScan];
    });
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_btRetrievePeripheral___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT peripheralId) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    if (pid == nil) {
        return JAVA_NULL;
    }
    __block NSString *name = nil;
    cn1btSync(^{
        NSString *n = [cn1btGetController() retrievePeripheral:pid];
        name = [n copy];
    });
    if (name == nil) {
        return JAVA_NULL;
    }
    JAVA_OBJECT out = fromNSString(threadStateData, name);
    [name release];
    return out;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_btGetKnownPeripherals___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT serviceUuid) {
    CBUUID *u = cn1btUuid(toNSString(threadStateData, serviceUuid));
    if (u == nil) {
        return JAVA_NULL;
    }
    __block NSString *result = nil;
    cn1btSync(^{
        NSString *r = [cn1btGetController() connectedPeripheralsFor:u];
        result = [r copy];
    });
    if (result == nil) {
        return JAVA_NULL;
    }
    JAVA_OBJECT out = fromNSString(threadStateData, result);
    [result release];
    return out;
}

void com_codename1_impl_ios_IOSNative_btConnect___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT peripheralId) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    cn1btAsync(^{
        [cn1btGetController() connectPeripheral:pid];
    });
}

void com_codename1_impl_ios_IOSNative_btDisconnect___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT peripheralId) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    cn1btAsync(^{
        [cn1btGetController() disconnectPeripheral:pid];
    });
}

void com_codename1_impl_ios_IOSNative_btDiscoverServices___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    cn1btAsync(^{
        [cn1btGetController() discoverServices:requestId peripheral:pid];
    });
}

void com_codename1_impl_ios_IOSNative_btReadCharacteristic___int_java_lang_String_java_lang_String_int_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_OBJECT serviceUuid,
        JAVA_INT serviceInstance, JAVA_OBJECT charUuid,
        JAVA_INT charInstance) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    NSString *su = toNSString(threadStateData, serviceUuid);
    NSString *cu = toNSString(threadStateData, charUuid);
    cn1btAsync(^{
        [cn1btGetController() withChar:requestId peripheral:pid svcUuid:su
                svcInst:serviceInstance charUuid:cu charInst:charInstance
                op:^(CN1BtPeripheralEntry *e, CBCharacteristic *ch,
                        NSString *key) {
            [e.pendingReads setObject:[NSNumber numberWithInt:requestId]
                    forKey:key];
            [e.peripheral readValueForCharacteristic:ch];
        }];
    });
}

void com_codename1_impl_ios_IOSNative_btWriteCharacteristic___int_java_lang_String_java_lang_String_int_java_lang_String_int_byte_1ARRAY_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_OBJECT serviceUuid,
        JAVA_INT serviceInstance, JAVA_OBJECT charUuid,
        JAVA_INT charInstance, JAVA_OBJECT value,
        JAVA_BOOLEAN withResponse) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    NSString *su = toNSString(threadStateData, serviceUuid);
    NSString *cu = toNSString(threadStateData, charUuid);
    NSData *data = cn1btDataFromJavaArray(value);
    BOOL wr = withResponse ? YES : NO;
    cn1btAsync(^{
        [cn1btGetController() withChar:requestId peripheral:pid svcUuid:su
                svcInst:serviceInstance charUuid:cu charInst:charInstance
                op:^(CN1BtPeripheralEntry *e, CBCharacteristic *ch,
                        NSString *key) {
            if (wr) {
                [e.pendingWrites setObject:
                        [NSNumber numberWithInt:requestId] forKey:key];
                [e.peripheral writeValue:data forCharacteristic:ch
                        type:CBCharacteristicWriteWithResponse];
            } else {
                [e.peripheral writeValue:data forCharacteristic:ch
                        type:CBCharacteristicWriteWithoutResponse];
                // no delegate callback for write-without-response
                cn1btSendOpComplete(requestId);
            }
        }];
    });
}

void com_codename1_impl_ios_IOSNative_btReadDescriptor___int_java_lang_String_java_lang_String_int_java_lang_String_int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_OBJECT serviceUuid,
        JAVA_INT serviceInstance, JAVA_OBJECT charUuid,
        JAVA_INT charInstance, JAVA_OBJECT descriptorUuid) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    NSString *su = toNSString(threadStateData, serviceUuid);
    NSString *cu = toNSString(threadStateData, charUuid);
    NSString *du = toNSString(threadStateData, descriptorUuid);
    cn1btAsync(^{
        CN1BluetoothController *ctl = cn1btGetController();
        [ctl withChar:requestId peripheral:pid svcUuid:su
                svcInst:serviceInstance charUuid:cu charInst:charInstance
                op:^(CN1BtPeripheralEntry *e, CBCharacteristic *ch,
                        NSString *key) {
            CBDescriptor *d = [ctl descIn:ch uuid:du];
            if (d == nil) {
                cn1btSendRequestError(requestId, CN1_BT_ERR_GATT,
                        @"Descriptor not found");
                return;
            }
            NSString *dk = [ctl keyForDesc:d in:e.peripheral];
            if (dk != nil) {
                [e.pendingDescReads setObject:
                        [NSNumber numberWithInt:requestId] forKey:dk];
            }
            [e.peripheral readValueForDescriptor:d];
        }];
    });
}

void com_codename1_impl_ios_IOSNative_btWriteDescriptor___int_java_lang_String_java_lang_String_int_java_lang_String_int_java_lang_String_byte_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_OBJECT serviceUuid,
        JAVA_INT serviceInstance, JAVA_OBJECT charUuid,
        JAVA_INT charInstance, JAVA_OBJECT descriptorUuid,
        JAVA_OBJECT value) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    NSString *su = toNSString(threadStateData, serviceUuid);
    NSString *cu = toNSString(threadStateData, charUuid);
    NSString *du = toNSString(threadStateData, descriptorUuid);
    NSData *data = cn1btDataFromJavaArray(value);
    cn1btAsync(^{
        CN1BluetoothController *ctl = cn1btGetController();
        [ctl withChar:requestId peripheral:pid svcUuid:su
                svcInst:serviceInstance charUuid:cu charInst:charInstance
                op:^(CN1BtPeripheralEntry *e, CBCharacteristic *ch,
                        NSString *key) {
            CBDescriptor *d = [ctl descIn:ch uuid:du];
            if (d == nil) {
                cn1btSendRequestError(requestId, CN1_BT_ERR_GATT,
                        @"Descriptor not found");
                return;
            }
            NSString *dk = [ctl keyForDesc:d in:e.peripheral];
            if (dk != nil) {
                [e.pendingDescWrites setObject:
                        [NSNumber numberWithInt:requestId] forKey:dk];
            }
            [e.peripheral writeValue:data forDescriptor:d];
        }];
    });
}

void com_codename1_impl_ios_IOSNative_btSetNotify___int_java_lang_String_java_lang_String_int_java_lang_String_int_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_OBJECT serviceUuid,
        JAVA_INT serviceInstance, JAVA_OBJECT charUuid,
        JAVA_INT charInstance, JAVA_BOOLEAN enable) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    NSString *su = toNSString(threadStateData, serviceUuid);
    NSString *cu = toNSString(threadStateData, charUuid);
    BOOL en = enable ? YES : NO;
    cn1btAsync(^{
        [cn1btGetController() withChar:requestId peripheral:pid svcUuid:su
                svcInst:serviceInstance charUuid:cu charInst:charInstance
                op:^(CN1BtPeripheralEntry *e, CBCharacteristic *ch,
                        NSString *key) {
            [e.pendingNotifyState setObject:
                    [NSNumber numberWithInt:requestId] forKey:key];
            [e.peripheral setNotifyValue:en forCharacteristic:ch];
        }];
    });
}

void com_codename1_impl_ios_IOSNative_btReadRssi___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    cn1btAsync(^{
        [cn1btGetController() readRssi:requestId peripheral:pid];
    });
}

JAVA_INT com_codename1_impl_ios_IOSNative_btGetMaxWriteLength___java_lang_String_boolean_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT peripheralId,
        JAVA_BOOLEAN withResponse) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    BOOL wr = withResponse ? YES : NO;
    __block int result = 0;
    cn1btSync(^{
        result = [cn1btGetController() maxWriteLength:pid withResponse:wr];
    });
    return result;
}

void com_codename1_impl_ios_IOSNative_btOpenL2cap___int_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_INT psm) {
    NSString *pid = toNSString(threadStateData, peripheralId);
    cn1btAsync(^{
        [cn1btGetController() openL2cap:requestId peripheral:pid psm:psm];
    });
}

// ---- peripheral role trampolines -------------------------------------------

void com_codename1_impl_ios_IOSNative_btOpenGattServer___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    cn1btAsync(^{
        [cn1btGetController() openGattServer:requestId];
    });
#else
    cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_SUPPORTED,
            @"BLE peripheral role is unavailable on this platform");
#endif
}

void com_codename1_impl_ios_IOSNative_btAddService___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT serviceDefinition) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    NSString *def = toNSString(threadStateData, serviceDefinition);
    cn1btAsync(^{
        [cn1btGetController() addService:requestId definition:def];
    });
#else
    cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_SUPPORTED,
            @"BLE peripheral role is unavailable on this platform");
#endif
}

void com_codename1_impl_ios_IOSNative_btRemoveService___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT serviceLocalId) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    cn1btAsync(^{
        [cn1btGetController() removeServiceById:serviceLocalId];
    });
#endif
}

void com_codename1_impl_ios_IOSNative_btCloseGattServer__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    cn1btAsync(^{
        [cn1btGetController() closeGattServer];
    });
#endif
}

void com_codename1_impl_ios_IOSNative_btStartAdvertising___int_java_lang_String_java_lang_String_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT localName, JAVA_OBJECT serviceUuids) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    NSString *name = localName == JAVA_NULL
            ? nil : toNSString(threadStateData, localName);
    NSArray *uuids = cn1btUuidArray(threadStateData, serviceUuids);
    cn1btAsync(^{
        [cn1btGetController() startAdvertising:requestId name:name
                uuids:uuids];
    });
#else
    cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_SUPPORTED,
            @"BLE peripheral role is unavailable on this platform");
#endif
}

void com_codename1_impl_ios_IOSNative_btStopAdvertising__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    cn1btAsync(^{
        [cn1btGetController() stopAdvertising];
    });
#endif
}

void com_codename1_impl_ios_IOSNative_btNotifyValue___int_int_byte_1ARRAY_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT charLocalId, JAVA_OBJECT value, JAVA_OBJECT centralId) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    NSData *data = cn1btDataFromJavaArray(value);
    NSString *cid = centralId == JAVA_NULL
            ? nil : toNSString(threadStateData, centralId);
    cn1btAsync(^{
        [cn1btGetController() notifyValue:requestId charId:charLocalId
                data:data central:cid];
    });
#else
    cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_SUPPORTED,
            @"BLE peripheral role is unavailable on this platform");
#endif
}

void com_codename1_impl_ios_IOSNative_btRespondToReadRequest___long_byte_1ARRAY_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG requestHandle,
        JAVA_OBJECT value, JAVA_INT attStatus) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    NSData *data = value == JAVA_NULL ? nil : cn1btDataFromJavaArray(value);
    long long h = (long long)requestHandle;
    cn1btAsync(^{
        [cn1btGetController() respondToRead:h data:data status:attStatus];
    });
#endif
}

void com_codename1_impl_ios_IOSNative_btRespondToWriteRequest___long_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG requestHandle,
        JAVA_INT attStatus) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    long long h = (long long)requestHandle;
    cn1btAsync(^{
        [cn1btGetController() respondToWrite:h status:attStatus];
    });
#endif
}

void com_codename1_impl_ios_IOSNative_btPublishL2cap___int_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_BOOLEAN secure) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    BOOL sec = secure ? YES : NO;
    cn1btAsync(^{
        [cn1btGetController() publishL2cap:requestId secure:sec];
    });
#else
    cn1btSendRequestError(requestId, CN1_BT_ERR_NOT_SUPPORTED,
            @"BLE peripheral role is unavailable on this platform");
#endif
}

void com_codename1_impl_ios_IOSNative_btUnpublishL2cap___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT psm) {
#ifdef CN1_BT_PERIPHERAL_ROLE
    cn1btAsync(^{
        [cn1btGetController() unpublishL2cap:psm];
    });
#endif
}

// ---- L2CAP stream I/O -------------------------------------------------------

JAVA_INT com_codename1_impl_ios_IOSNative_btL2capRead___long_byte_1ARRAY_int_int_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG channelHandle,
        JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT len) {
    CN1BtL2capChannel *w = cn1btChannelForHandle((long long)channelHandle);
    if (w == nil || buffer == JAVA_NULL || len <= 0) {
        return -2;
    }
    NSInputStream *in = w.input;
    for (;;) {
        if (w.closed) {
            return -1;
        }
        NSStreamStatus st = in.streamStatus;
        if (st == NSStreamStatusAtEnd || st == NSStreamStatusClosed) {
            return -1;
        }
        if (st == NSStreamStatusError) {
            return -2;
        }
        if (in.hasBytesAvailable) {
            break;
        }
        usleep(10000); // 10ms poll; blocking-read contract, off the BT queue
    }
    JAVA_ARRAY a = (JAVA_ARRAY)buffer;
    if (offset < 0 || offset + len > a->length) {
        return -2;
    }
    NSInteger n = [in read:((uint8_t *)a->data) + offset
            maxLength:(NSUInteger)len];
    if (n == 0) {
        return -1;
    }
    if (n < 0) {
        return w.closed ? -1 : -2;
    }
    return (JAVA_INT)n;
}

JAVA_INT com_codename1_impl_ios_IOSNative_btL2capWrite___long_byte_1ARRAY_int_int_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG channelHandle,
        JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT len) {
    CN1BtL2capChannel *w = cn1btChannelForHandle((long long)channelHandle);
    if (w == nil || buffer == JAVA_NULL || len <= 0) {
        return -2;
    }
    NSOutputStream *out = w.output;
    for (;;) {
        if (w.closed) {
            return -2;
        }
        NSStreamStatus st = out.streamStatus;
        if (st == NSStreamStatusAtEnd || st == NSStreamStatusClosed
                || st == NSStreamStatusError) {
            return -2;
        }
        if (out.hasSpaceAvailable) {
            break;
        }
        usleep(10000);
    }
    JAVA_ARRAY a = (JAVA_ARRAY)buffer;
    if (offset < 0 || offset + len > a->length) {
        return -2;
    }
    NSInteger n = [out write:((const uint8_t *)a->data) + offset
            maxLength:(NSUInteger)len];
    if (n <= 0) {
        return -2;
    }
    return (JAVA_INT)n;
}

void com_codename1_impl_ios_IOSNative_btL2capClose___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG channelHandle) {
    NSMutableDictionary *dict = cn1btChannels();
    CN1BtL2capChannel *w = nil;
    @synchronized (dict) {
        NSNumber *key = [NSNumber numberWithLongLong:
                (long long)channelHandle];
        w = [[[dict objectForKey:key] retain] autorelease];
        [dict removeObjectForKey:key];
    }
    if (w != nil) {
        w.closed = YES;
        [w.input close];
        [w.output close];
    }
}

#else // CN1_INCLUDE_BLUETOOTH not defined ------------------------------------

// Stubs when CN1_INCLUDE_BLUETOOTH is not defined: the app never referenced
// com.codename1.bluetooth.*, so IOSBluetooth never calls these -- but
// ParparVM still needs the symbols to satisfy the IOSNative declarations at
// link time. No CoreBluetooth symbol is referenced on this path.

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isBlePeripheralSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getBluetoothAuthorization___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return 2; // denied
}

void com_codename1_impl_ios_IOSNative_startBluetoothStateMonitor__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}

void com_codename1_impl_ios_IOSNative_btStartScan___java_lang_String_1ARRAY_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT serviceUuids,
        JAVA_BOOLEAN allowDuplicates) {
}

void com_codename1_impl_ios_IOSNative_btStopScan__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_btRetrievePeripheral___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT peripheralId) {
    return JAVA_NULL;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_btGetKnownPeripherals___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT serviceUuid) {
    return JAVA_NULL;
}

void com_codename1_impl_ios_IOSNative_btConnect___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT peripheralId) {
}

void com_codename1_impl_ios_IOSNative_btDisconnect___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT peripheralId) {
}

void com_codename1_impl_ios_IOSNative_btDiscoverServices___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId) {
}

void com_codename1_impl_ios_IOSNative_btReadCharacteristic___int_java_lang_String_java_lang_String_int_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_OBJECT serviceUuid,
        JAVA_INT serviceInstance, JAVA_OBJECT charUuid,
        JAVA_INT charInstance) {
}

void com_codename1_impl_ios_IOSNative_btWriteCharacteristic___int_java_lang_String_java_lang_String_int_java_lang_String_int_byte_1ARRAY_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_OBJECT serviceUuid,
        JAVA_INT serviceInstance, JAVA_OBJECT charUuid,
        JAVA_INT charInstance, JAVA_OBJECT value,
        JAVA_BOOLEAN withResponse) {
}

void com_codename1_impl_ios_IOSNative_btReadDescriptor___int_java_lang_String_java_lang_String_int_java_lang_String_int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_OBJECT serviceUuid,
        JAVA_INT serviceInstance, JAVA_OBJECT charUuid,
        JAVA_INT charInstance, JAVA_OBJECT descriptorUuid) {
}

void com_codename1_impl_ios_IOSNative_btWriteDescriptor___int_java_lang_String_java_lang_String_int_java_lang_String_int_java_lang_String_byte_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_OBJECT serviceUuid,
        JAVA_INT serviceInstance, JAVA_OBJECT charUuid,
        JAVA_INT charInstance, JAVA_OBJECT descriptorUuid,
        JAVA_OBJECT value) {
}

void com_codename1_impl_ios_IOSNative_btSetNotify___int_java_lang_String_java_lang_String_int_java_lang_String_int_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_OBJECT serviceUuid,
        JAVA_INT serviceInstance, JAVA_OBJECT charUuid,
        JAVA_INT charInstance, JAVA_BOOLEAN enable) {
}

void com_codename1_impl_ios_IOSNative_btReadRssi___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId) {
}

JAVA_INT com_codename1_impl_ios_IOSNative_btGetMaxWriteLength___java_lang_String_boolean_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT peripheralId,
        JAVA_BOOLEAN withResponse) {
    return 0;
}

void com_codename1_impl_ios_IOSNative_btOpenGattServer___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId) {
}

void com_codename1_impl_ios_IOSNative_btAddService___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT serviceDefinition) {
}

void com_codename1_impl_ios_IOSNative_btRemoveService___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT serviceLocalId) {
}

void com_codename1_impl_ios_IOSNative_btCloseGattServer__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}

void com_codename1_impl_ios_IOSNative_btStartAdvertising___int_java_lang_String_java_lang_String_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT localName, JAVA_OBJECT serviceUuids) {
}

void com_codename1_impl_ios_IOSNative_btStopAdvertising__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}

void com_codename1_impl_ios_IOSNative_btNotifyValue___int_int_byte_1ARRAY_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT charLocalId, JAVA_OBJECT value, JAVA_OBJECT centralId) {
}

void com_codename1_impl_ios_IOSNative_btRespondToReadRequest___long_byte_1ARRAY_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG requestHandle,
        JAVA_OBJECT value, JAVA_INT attStatus) {
}

void com_codename1_impl_ios_IOSNative_btRespondToWriteRequest___long_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG requestHandle,
        JAVA_INT attStatus) {
}

void com_codename1_impl_ios_IOSNative_btOpenL2cap___int_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT peripheralId, JAVA_INT psm) {
}

void com_codename1_impl_ios_IOSNative_btPublishL2cap___int_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_BOOLEAN secure) {
}

void com_codename1_impl_ios_IOSNative_btUnpublishL2cap___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT psm) {
}

JAVA_INT com_codename1_impl_ios_IOSNative_btL2capRead___long_byte_1ARRAY_int_int_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG channelHandle,
        JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT len) {
    return -2;
}

JAVA_INT com_codename1_impl_ios_IOSNative_btL2capWrite___long_byte_1ARRAY_int_int_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG channelHandle,
        JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT len) {
    return -2;
}

void com_codename1_impl_ios_IOSNative_btL2capClose___long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_LONG channelHandle) {
}

#endif // CN1_INCLUDE_BLUETOOTH
