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
    /// The duration variants as well as the shared list. CN1MacPickers builds an
    /// hours-only and a minutes-only field set, but Picker asks this before it
    /// calls showNativePicker() -- so while the inherited answer named only
    /// PICKER_TYPE_DURATION, both of those branches were unreachable and the
    /// lightweight spinner was shown instead. Native code that nothing can call
    /// is the failure this port has to be most careful about.
    @Override
    public boolean isNativePickerTypeSupported(int pickerType) {
        return pickerType == com.codename1.ui.Display.PICKER_TYPE_DURATION_HOURS
                || pickerType == com.codename1.ui.Display.PICKER_TYPE_DURATION_MINUTES
                || super.isNativePickerTypeSupported(pickerType);
    }

    /// @inheritDoc
    ///
    /// "macOS", not the inherited "iOS". getPlatformName() has reported "mac"
    /// since this port existed, but the documented property API is a second way
    /// to ask the same question and it still answered for the superclass -- so
    /// an application or a diagnostic that reads OS or Platform saw an iOS
    /// process and could pick iOS behaviour on a Mac. Every other key stays with
    /// IOSImplementation, which is the point of subclassing it.
    @Override
    public String getProperty(String key, String defaultValue) {
        if ("OS".equalsIgnoreCase(key) || "Platform".equalsIgnoreCase(key)) {
            return "macOS";
        }
        return super.getProperty(key, defaultValue);
    }

    /// @inheritDoc
    ///
    /// False, because nothing on this port can ever deliver it. The capability
    /// is a promise that `Lifecycle.onReceivedSharedContent()` will be called,
    /// and the single producer of that callback is the share-extension handler
    /// in `CodenameOne_GLAppDelegate.m` -- a UIKit source this build excludes,
    /// fed by an app extension the macOS builder deliberately does not generate.
    /// The AppKit URL handler is not a substitute: it delivers through `AppArg`
    /// and the `URLCallback` interface, which is a different API.
    ///
    /// Inheriting the iOS answer meant an application that gates its sharing UI
    /// on this waited for a callback that had no way to arrive -- worse than a
    /// missing feature, because it looks like one that is merely slow.
    @Override
    public boolean isReceiveSharedContentSupported() {
        return false;
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

    /// @inheritDoc
    ///
    /// Null until there is an AppKit 3D backend. The inherited answer is
    /// non-null whenever Metal rendering is on, which it always is here, so
    /// isGpuSupported() said yes -- and then createPeer() returned null every
    /// time, because gl3dCreateContext is one of the CN1AppKitGL3D stubs and
    /// answers 0. An application checking the capability took its GPU path and
    /// got no surface, rather than taking the fallback it has for platforms
    /// without one.
    ///
    /// A real implementation over MTKView is a separate piece of work, and the
    /// stubs say so where they are defined.
    @Override
    public com.codename1.impl.gpu.GpuImplementation getGpuImplementation() {
        return null;
    }

    /// @inheritDoc
    ///
    /// Null, which is the base class's answer for a platform without location.
    ///
    /// Not simply a matter of enabling INCLUDE_LOCATION_USAGE: CoreLocation is
    /// on macOS and the natives compile, but the delegate that receives
    /// didUpdateLocations, didEnterRegion and didExitRegion is the
    /// `@implementation` in the `!TARGET_OS_OSX` half of
    /// CodenameOne_GLViewController.m, and this port compiles the AppKit
    /// controller instead. Turning the define on would hand back a working-
    /// looking LocationManager that never reports a position -- which is worse
    /// than saying no, because nothing downstream can tell the difference.
    ///
    /// Making it work means the AppKit controller adopting
    /// CLLocationManagerDelegate and carrying those callbacks, which is its own
    /// piece of work rather than a build flag. `IOSNative.isGeofencingSupported()`
    /// reports false for the same reason.
    @Override
    public com.codename1.location.LocationManager getLocationManager() {
        return null;
    }

    /// @inheritDoc
    ///
    /// False. This port delivers mouse and trackpad events, not touch, and the
    /// inherited answer is an unconditional true because every other consumer of
    /// IOSImplementation is a touch device.
    ///
    /// Display caches it once, so a Mac build came up with every touch-only
    /// behaviour selected -- command, spinner, map, focus and scrollbar paths
    /// all pick their variant from this.
    @Override
    public boolean isTouchDevice() {
        return false;
    }

    /// @inheritDoc
    ///
    /// A Mac has no SMS. sendSMS() is a no-op on this port, and the inherited
    /// answer said SMS_INTERACTIVE -- so an application offered a compose path
    /// that silently did nothing. The macOS chapter promises Display reports it
    /// unsupported, which is now true.
    @Override
    public int getSMSSupport() {
        return com.codename1.ui.Display.SMS_NOT_SUPPORTED;
    }

    /// @inheritDoc
    ///
    /// False: a desktop window has no orientation to lock, lockOrientation is a
    /// no-op here, and the macOS chapter promises this reads as unsupported.
    /// The inherited answer excludes only the watch and the TV.
    @Override
    public boolean canForceOrientation() {
        return false;
    }

    /// @inheritDoc
    ///
    /// False until there is an AppKit accessibility tree. The projection native
    /// does nothing on this port, so a true answer both discarded every update
    /// AND suppressed Component.focusGainedInternal's announcement fallback --
    /// leaving a custom-drawn control with neither semantics nor focus
    /// announcements. Saying no gets the fallback back.
    ///
    /// Exposing a custom-drawn interface to VoiceOver is a real project, and the
    /// macOS chapter says so rather than implying it comes free.
    @Override
    public boolean isAccessibilityTreeSupported() {
        return false;
    }

    /// @inheritDoc
    ///
    /// False. The inherited check reads the OS version and says yes on macOS 11
    /// and later, but the native behind it is compiled out unless the build
    /// defines CN1_USE_APPREVIEW and links StoreKit, which this builder does
    /// not -- so requestNativeInAppReview did nothing while its wrapper reported
    /// success, and AppReview never showed its portable fallback.
    @Override
    public boolean isNativeInAppReviewSupported() {
        return false;
    }

    /// @inheritDoc
    ///
    /// False: there is no contacts backend on this port. The shared
    /// implementation is the legacy AddressBook C API, which does not exist on
    /// macOS -- the Catalyst slice already undefines the gate for the same
    /// reason -- and no Contacts.framework backend has been written.
    ///
    /// Reported rather than left at the inherited true so an application that
    /// checks before reading contacts takes its unsupported path, instead of
    /// calling in and receiving an iOS build-hint exception.
    @Override
    public boolean isContactsPermissionGranted() {
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
