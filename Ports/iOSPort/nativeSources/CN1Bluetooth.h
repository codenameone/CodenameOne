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

// CoreBluetooth bridge for the com.codename1.bluetooth API. The entire
// implementation lives in CN1Bluetooth.m gated on CN1_INCLUDE_BLUETOOTH
// (flipped by IPhoneBuilder when the classpath scanner sees
// com.codename1.bluetooth.*); when the define is off every
// com_codename1_impl_ios_IOSNative_bt* trampoline is a linkable stub and no
// CoreBluetooth symbol is referenced.
//
// Nothing is exported from this translation unit -- the singleton
// controller, its dispatch queue and the L2CAP handle table are all
// file-static in CN1Bluetooth.m.

#ifndef CN1_BLUETOOTH_H
#define CN1_BLUETOOTH_H

#import <Foundation/Foundation.h>

#endif // CN1_BLUETOOTH_H
