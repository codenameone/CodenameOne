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
package com.codename1.impl.mac;

import com.codename1.impl.ios.IOSImplementation;

/// The native macOS (AppKit) implementation.
///
/// It extends the iOS one rather than restating it, because the two ports share
/// far more than they differ: of the Objective-C behind `IOSNative`, roughly six
/// lines in seven touch no UIKit at all -- Foundation, CoreGraphics, CoreText,
/// Metal, AVFoundation, EventKit, CoreBluetooth, Network and Security are the
/// same frameworks on both platforms. What differs is the shell around them: the
/// application object, the windows, the event source, the text input client and
/// the menu bar. Those are what this class overrides.
///
/// #### Why the subclass has to be named in `@Concrete`
///
/// ParparVM devirtualizes every call made through a `CodenameOneImplementation`
/// reference straight to the class named in that annotation. Without
/// `mac = "com.codename1.impl.mac.MacImplementation"` on
/// `CodenameOneImplementation`, a macOS build binds all of them to
/// `IOSImplementation` and nothing here ever runs -- a green build with the port
/// inert. The annotation is the wiring, not documentation of it.
///
/// #### Why the native bindings stay in `com.codename1.impl.ios`
///
/// ParparVM also mangles the declaring package and class into every C symbol, so
/// moving `IOSNative` here would rename all 836 of its natives and oblige this
/// port to reimplement `IOSNative.m` rather than share it. That package is
/// therefore the Apple native-binding namespace rather than the iPhone package,
/// and it is shipped by both ports. Natives that genuinely cannot be shared live
/// on their own class instead: the Catalyst windowing ones on
/// `CatalystWindowNative`, which this port does not ship, and the AppKit ones on
/// `MacNative`, which the iOS port does not.
///
/// @author Shai Almog
public class MacImplementation extends IOSImplementation {

    @Override
    public String getPlatformName() {
        return "mac";
    }

    /// macOS is a desktop, always -- unlike the iOS port, which has to ask the
    /// running process whether it happens to be a Catalyst app.
    @Override
    public boolean isDesktop() {
        return true;
    }

    /// @inheritDoc
    ///
    /// False until there is an AppKit capture path. The inherited answer is an
    /// unconditional true, but capturePhoto() and captureVideo() are inherited
    /// from the iOS port and cannot work here: the macOS builder never sets
    /// INCLUDE_CAMERA_USAGE, and the native behind them is UIImagePickerController,
    /// which macOS does not have. An application told yes selects the capture
    /// path and gets an iOS build-hint exception instead of a photo.
    ///
    /// AVFoundation capture does exist on macOS. This is a port that has not
    /// been written, not a platform that cannot; the ledger records it beside
    /// the photo picker.
    @Override
    public boolean hasCamera() {
        return false;
    }

    private AppKitWindowManager windowManager;

    /// @inheritDoc
    ///
    /// Unconditional here. The iOS port has to read
    /// `UIApplicationSupportsMultipleScenes` back out of the bundle before it can
    /// offer windows, because on Mac Catalyst that key is what decides whether a
    /// second scene can be activated at all. AppKit is multi-window natively,
    /// with no manifest key and no opt-in, so there is nothing to consult and no
    /// build that could answer differently.
    @Override
    public com.codename1.impl.WindowManager getWindowManager() {
        if (windowManager == null) {
            windowManager = new AppKitWindowManager(this);
        }
        return windowManager;
    }
}
