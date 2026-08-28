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
#ifndef CN1MacAccessibility_h
#define CN1MacAccessibility_h
#import <TargetConditionals.h>
#if TARGET_OS_OSX

#import <AppKit/AppKit.h>

/// Publishes the framework's accessibility semantics tree through NSAccessibility.
///
/// Codename One paints its own controls, so there is no AppKit view for
/// VoiceOver to inspect. The framework answers that with a portable semantics
/// tree -- see com.codename1.ui.accessibility -- and hands each port the same
/// JSON: a flat list of nodes carrying a role, a label, a value, bounds, and the
/// actions each node supports. A port's whole job is to project that list onto
/// its platform's accessibility API. The UIKit port projects it onto
/// UIAccessibilityElement; this projects the same list onto
/// NSAccessibilityElement.
///
/// `changeType` carries the framework's change flags; bit 256 means the screen
/// itself changed rather than part of it, which is the difference between
/// telling VoiceOver to re-read everything and telling it the layout moved.
void CN1MacAccessibilityUpdateTree(NSString *json, int changeType);

#endif
#endif
