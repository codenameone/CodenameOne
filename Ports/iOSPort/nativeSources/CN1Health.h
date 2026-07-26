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
//  CN1Health.h
//  The HealthKit bridge behind com.codename1.health.
//
//  Everything here is gated on CN1_INCLUDE_HEALTH, which IPhoneBuilder
//  uncomments in CodenameOne_GLViewController.h only for apps that use a
//  health store. The store, the serial queue and the query registry are all
//  file-static in CN1Health.m -- nothing is exported -- so this header
//  exists only to give the translation unit the feature gate and the shared
//  error codes.
//
//  The #else branch of CN1Health.m provides no-op trampolines for every
//  native declared in IOSNative.java, so a health-free app still links.
//

#ifndef CN1_HEALTH_H
#define CN1_HEALTH_H

#import <Foundation/Foundation.h>

// Error codes handed back to IOSHealth.nativeHkRequestError. Keep in sync
// with the constants of the same names in IOSHealth.java.
#define CN1_HK_ERR_UNKNOWN 0
#define CN1_HK_ERR_NOT_AVAILABLE 1
#define CN1_HK_ERR_AUTH_DENIED 2
#define CN1_HK_ERR_AUTH_NOT_DETERMINED 3
#define CN1_HK_ERR_INVALID_ARGUMENT 4
#define CN1_HK_ERR_NO_DATA 5
#define CN1_HK_ERR_DATABASE_INACCESSIBLE 6
#define CN1_HK_ERR_USER_CANCELED 7
#define CN1_HK_ERR_NOT_SUPPORTED 8
#define CN1_HK_ERR_ANCHOR_INVALID 9
#define CN1_HK_ERR_RATE_LIMITED 10

#endif
