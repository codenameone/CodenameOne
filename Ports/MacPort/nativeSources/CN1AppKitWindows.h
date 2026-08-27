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
#ifndef CN1AppKitWindows_h
#define CN1AppKitWindows_h
#import <TargetConditionals.h>
#if TARGET_OS_OSX

#import <AppKit/AppKit.h>

/// A window that can be told to stop accepting keyboard focus.
///
/// The framework blocks a window behind a modal by disabling its rendering
/// view, covering its native peers and disabling its title bar buttons. None of
/// that stops AppKit making the window key: the port begins an application modal
/// session but never pumps it with runModalSession:, and an unpumped session
/// does not keep other windows from taking key -- measured, not assumed. The
/// blocked window would then hold the keyboard while its view discarded
/// everything typed into it, so the keystrokes reached nothing at all.
///
/// canBecomeKeyWindow still defers to super when the gate is open, so a
/// borderless window keeps answering what it answered before.
@interface CN1MacWindow : NSWindow
@property (nonatomic, assign) BOOL cn1AcceptsKey;
@end

/// `CN1MacWindow` for a utility window, which is an NSPanel rather than an
/// NSWindow and so cannot inherit from it.
@interface CN1MacPanel : NSPanel
@property (nonatomic, assign) BOOL cn1AcceptsKey;
@end

/// Opens or closes `w`'s key gate, if `w` has one. A window this port did not
/// create -- there is none today, but a sheet or a system panel would be one --
/// is left alone rather than assumed.
void CN1MacWindowSetAcceptsKey(NSWindow *w, BOOL accepts);

#endif
#endif
