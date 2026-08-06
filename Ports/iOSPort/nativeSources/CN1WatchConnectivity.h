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

// WatchConnectivity glue backing com.codename1.wearable on Apple.
//
// The same file compiles into BOTH the phone target and the watch target: WCSession is symmetric,
// so the phone half and the watch half of a pair run identical code and the Java API is identical
// on both ends. WatchConnectivity is unavailable on tvOS and Mac Catalyst, and the whole file is
// additionally gated on CN1_USE_WATCHCONNECTIVITY, which the build defines only when the app
// references com.codename1.wearable -- apps that do not pay nothing and link no framework.
//
// Everything below moves opaque byte payloads; the value model lives in Java
// (com.codename1.wearable.WearableMessage), so this layer never has to understand it.

#ifndef CN1WatchConnectivity_h
#define CN1WatchConnectivity_h

#include "TargetConditionals.h"
// CN1_USE_WATCHCONNECTIVITY lives in the central header the builder edits. Every translation unit
// that tests it has to see that definition, so import it here rather than in the .m: without this
// the guard below is always false, the implementation compiles away, and the app fails to link
// against a class the natives call.
#import "CodenameOne_GLViewController.h"

#if defined(CN1_USE_WATCHCONNECTIVITY) && !TARGET_OS_TV && !TARGET_OS_MACCATALYST

#import <Foundation/Foundation.h>
#import <WatchConnectivity/WatchConnectivity.h>

@interface CN1WatchConnectivity : NSObject <WCSessionDelegate>

/// Returns the shared instance, activating the WCSession on first use.
+ (CN1WatchConnectivity *)shared;

/// True when this device supports the link at all. False on an iPad, and on an iPhone whose
/// WCSession is not supported.
- (BOOL)isSupported;

/// True when a counterpart device is paired. Always true from the watch side, which by definition
/// has a phone.
- (BOOL)isPaired;

/// True when the peer app can receive a live message right now.
- (BOOL)isReachable;

/// True when the counterpart app is installed on the paired device.
- (BOOL)isCompanionInstalled;

/// Sends a live message. A non-zero replyToken asks the peer for an answer, which comes back through
/// cn1_wearable_deliverReply.
- (void)sendMessage:(NSString *)path payload:(NSData *)payload replyToken:(int)replyToken;

/// Answers a message that arrived carrying a reply token.
- (void)sendReply:(int)replyToken payload:(NSData *)payload;

/// Publishes or replaces the replicated value at a path.
- (void)putData:(NSString *)path payload:(NSData *)payload;

/// Returns the replicated value at a path, or nil.
- (NSData *)getData:(NSString *)path;

/// Removes the replicated value at a path.
- (void)removeData:(NSString *)path;

/// Returns every path currently holding a replicated value.
- (NSArray<NSString *> *)dataPaths;

/// Queues a file transfer to the peer.
- (void)transferFile:(NSString *)path name:(NSString *)name contents:(NSData *)contents;

/// Drops a path's received marker so the next whole-context update delivers it again.
- (void)forgetReceivedPath:(NSString *)path;

@end

#endif // CN1_USE_WATCHCONNECTIVITY

// Entry points into the Java side, implemented in IOSNative.m so this file needs no knowledge of
// the VM. No-ops when the feature is compiled out.
void cn1_wearable_deliverMessage(const char *path, const void *payload, int payloadLength, int replyToken);
void cn1_wearable_deliverReply(int replyToken, const void *payload, int payloadLength, const char *error);
void cn1_wearable_deliverDataChanged(const char *path, const void *payload, int payloadLength);
/// Same delivery, but the inbox entry named by inboxToken is retired only once the EDT has actually
/// consumed the payload. Renaming at queue time marked a transfer delivered that a process death
/// could still lose, and the ".done" sweep would then drop it without ever replaying it.
void cn1_wearable_deliverDataChangedTracked(const char *path, const void *payload, int payloadLength,
                                            const char *inboxToken);
/// Retires an inbox entry. Called from Java once the delivery has run on the EDT.
void cn1_wearable_confirmInbox(const char *inboxToken);
/// Gives up an undelivered entry, keeping the file but clearing its in-flight mark so a later
/// activation can replay it.
void cn1_wearable_releaseInbox(const char *inboxToken);
/// Re-offers everything still parked in the inbox. Called once a listener exists.
void cn1_wearable_replayInbox(void);
/// Forgets that a path's value was received, so the next context update delivers it again.
void cn1_wearable_forgetReceived(const char *path);
void cn1_wearable_deliverDataRemoved(const char *path);
void cn1_wearable_notifyStateChanged(void);

#endif /* CN1WatchConnectivity_h */
