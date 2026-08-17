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
//  CN1SmartHome.h
//  The HomeKit bridge behind com.codename1.home.
//
//  Everything here is gated on CN1_INCLUDE_HOMEKIT, which IPhoneBuilder
//  uncomments in CodenameOne_GLViewController.h only for apps that reference
//  com.codename1.home. That gate matters more than most: the HomeKit
//  entitlement is one Apple grants on the App ID, and an app carrying it
//  without cause fails codesigning for no reason its developer can see.
//
//  The home manager, the delegate, the trait table and the pending-request
//  registry are all file-static in CN1SmartHome.m -- nothing is exported --
//  so this header exists only to give the translation unit the feature gate
//  and the shared ordinal constants.
//
//  The #else branch of CN1SmartHome.m provides no-op trampolines for every
//  native declared in IOSNative.java, so a home-free app still links.
//

#ifndef CN1_SMART_HOME_H
#define CN1_SMART_HOME_H

#import <Foundation/Foundation.h>

// com.codename1.home.HomeAvailability ordinals. Keep in sync with the enum;
// the order there is the contract and this is the only place it is repeated.
#define CN1_HOME_AVAIL_AVAILABLE 0
#define CN1_HOME_AVAIL_COMMISSIONING_ONLY 1
#define CN1_HOME_AVAIL_PERMISSION_REQUIRED 2
#define CN1_HOME_AVAIL_SIGN_IN_REQUIRED 3
#define CN1_HOME_AVAIL_NOT_CONFIGURED 4
#define CN1_HOME_AVAIL_RESTRICTED 5
#define CN1_HOME_AVAIL_PROVIDER_NOT_INSTALLED 6
#define CN1_HOME_AVAIL_PROVIDER_UPDATE_REQUIRED 7
#define CN1_HOME_AVAIL_LOCAL_ONLY 8
#define CN1_HOME_AVAIL_NOT_SUPPORTED 9
#define CN1_HOME_AVAIL_PERMISSION_DENIED 10

// com.codename1.home.HomeAuthorizationStatus ordinals.
#define CN1_HOME_AUTH_NOT_DETERMINED 0
#define CN1_HOME_AUTH_AUTHORIZED 1
#define CN1_HOME_AUTH_RESTRICTED 2
#define CN1_HOME_AUTH_DENIED 3
#define CN1_HOME_AUTH_UNKNOWN 4

// com.codename1.home.TraitValueKind ordinals.
#define CN1_HOME_KIND_BOOLEAN 0
#define CN1_HOME_KIND_INT 1
#define CN1_HOME_KIND_DOUBLE 2
#define CN1_HOME_KIND_STRING 3
#define CN1_HOME_KIND_ENUM 4

// com.codename1.home.StructureChangeKind ordinals.
#define CN1_HOME_CHANGE_STRUCTURES 0
#define CN1_HOME_CHANGE_ACCESSORY_ADDED 1
#define CN1_HOME_CHANGE_ACCESSORY_REMOVED 2
#define CN1_HOME_CHANGE_ACCESSORY_RENAMED 3
#define CN1_HOME_CHANGE_ACCESSORY_MOVED 4
#define CN1_HOME_CHANGE_REACHABILITY 5
#define CN1_HOME_CHANGE_SCENES 6
#define CN1_HOME_CHANGE_AVAILABILITY 7

// com.codename1.home.commissioning.CommissioningStyle ordinals.
#define CN1_HOME_COMMISSION_OS_UI 0
#define CN1_HOME_COMMISSION_APP_HANDOFF 1
#define CN1_HOME_COMMISSION_NONE 2

#endif
