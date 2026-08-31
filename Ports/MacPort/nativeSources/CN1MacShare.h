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
#ifndef CN1MacShare_h
#define CN1MacShare_h
#import <TargetConditionals.h>
#if TARGET_OS_OSX

#import <AppKit/AppKit.h>

/// Presents the system share sheet for `items`, anchored in the window the share
/// was requested from.
///
/// `rectInPixels` is in Codename One pixels and is converted against that
/// window's own backing scale. `callbackId` is the ShareResultListener request
/// id, or 0 when the caller does not want an outcome -- with an id, the real
/// outcome is reported once the user has chosen a service and that service has
/// finished, rather than as soon as the sheet is on screen.
void CN1MacPresentSharePicker(NSArray *items, BOOL useRect, CGRect rectInPixels, int callbackId);

#endif
#endif
