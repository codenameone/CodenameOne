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

//
//  CN1Nearby.h
//  The Nearby Interaction, MultipeerConnectivity and AccessorySetupKit
//  bridge behind com.codename1.nearby.
//
//  Everything here is gated on CN1_INCLUDE_NEARBY, which IPhoneBuilder
//  uncomments in CodenameOne_GLViewController.h only for apps that reference
//  com.codename1.nearby. The three halves are gated again, separately, on
//  CN1_NEARBY_RANGING, CN1_NEARBY_TRANSPORT and CN1_NEARBY_COMPANION -- an app
//  that only wants ranging must not link MultipeerConnectivity, because
//  linking it obliges NSLocalNetworkUsageDescription and puts a local-network
//  prompt in front of a user who never asked for one.
//
//  The sessions, the peer registry and the pending-request bookkeeping are all
//  file-static in CN1Nearby.m -- nothing is exported -- so this header exists
//  only to carry the shared ordinal constants.
//
//  The #else branch of CN1Nearby.m provides no-op trampolines for every native
//  declared in IOSNative.java, so an app that never touched the package still
//  links.
//

#ifndef CN1_NEARBY_H
#define CN1_NEARBY_H

#import <Foundation/Foundation.h>

// com.codename1.nearby.NearbyAvailability ordinals. The order there is the
// contract and this is the only place it is repeated, so every constant is
// spelled with its Java name and NearbyNativeConstantParityTest compares the
// two: appending to that enum silently repoints these defines, and the failure
// is a device reporting the wrong state, which no build can show.
#define CN1_NEARBY_AVAIL_AVAILABLE 0
#define CN1_NEARBY_AVAIL_LOCAL_ONLY 1
#define CN1_NEARBY_AVAIL_UNAUTHORIZED 2
#define CN1_NEARBY_AVAIL_TEMPORARILY_UNAVAILABLE 3
#define CN1_NEARBY_AVAIL_NOT_SUPPORTED 4

// com.codename1.nearby.NearbyError ordinals.
#define CN1_NEARBY_ERR_NOT_SUPPORTED 0
#define CN1_NEARBY_ERR_UNAUTHORIZED 1
#define CN1_NEARBY_ERR_RADIO_UNAVAILABLE 2
#define CN1_NEARBY_ERR_PEER_UNAVAILABLE 3
#define CN1_NEARBY_ERR_SESSION_FAILED 4
#define CN1_NEARBY_ERR_SESSION_INVALIDATED 5
#define CN1_NEARBY_ERR_INVALID_TOKEN 6
#define CN1_NEARBY_ERR_TIMEOUT 7
#define CN1_NEARBY_ERR_BUSY 8
#define CN1_NEARBY_ERR_USER_CANCELED 9
#define CN1_NEARBY_ERR_IO_ERROR 10
#define CN1_NEARBY_ERR_UNKNOWN 11

// com.codename1.nearby.ranging.RangingRemovalReason ordinals.
#define CN1_NEARBY_REMOVED_PEER_ENDED 0
#define CN1_NEARBY_REMOVED_TIMEOUT 1
#define CN1_NEARBY_REMOVED_UNKNOWN 2

// com.codename1.nearby.transport.PayloadStatus ordinals.
#define CN1_NEARBY_PAYLOAD_IN_PROGRESS 0
#define CN1_NEARBY_PAYLOAD_SUCCESS 1
#define CN1_NEARBY_PAYLOAD_FAILURE 2
#define CN1_NEARBY_PAYLOAD_CANCELED 3

// com.codename1.nearby.transport.TransportStrategy ordinals.
#define CN1_NEARBY_STRATEGY_CLUSTER 0
#define CN1_NEARBY_STRATEGY_STAR 1
#define CN1_NEARBY_STRATEGY_POINT_TO_POINT 2

// com.codename1.nearby.companion.DeviceFilter kind constants.
#define CN1_NEARBY_FILTER_BLE_SERVICE 0
#define CN1_NEARBY_FILTER_NAME_PATTERN 1
#define CN1_NEARBY_FILTER_ADDRESS 2
#define CN1_NEARBY_FILTER_WIFI_SSID 3

// com.codename1.nearby.spi.NearbyBridge capability bits.
#define CN1_NEARBY_CAP_DISTANCE 1
#define CN1_NEARBY_CAP_DIRECTION 2
#define CN1_NEARBY_CAP_ELEVATION 4
#define CN1_NEARBY_CAP_CAMERA_ASSISTANCE 8
#define CN1_NEARBY_CAP_ACCESSORY 16
#define CN1_NEARBY_CAP_BACKGROUND 32

// com.codename1.nearby.spi.NearbyBridge payload types.
/// The transport's own frame header: one kind byte then a four-byte payload
/// id. Both ends of a MultipeerConnectivity session are this port, so the
/// framing is symmetric by construction.
#define CN1_NEARBY_FRAME_HEADER 5
#define CN1_NEARBY_FRAME_DATA 0
#define CN1_NEARBY_FRAME_ACK 1

#define CN1_NEARBY_PAYLOAD_BYTES 0
#define CN1_NEARBY_PAYLOAD_FILE 1

#endif
