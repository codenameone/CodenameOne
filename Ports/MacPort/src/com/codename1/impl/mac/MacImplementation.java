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
    /// Desktop layers, not the inherited iPad ones.
    ///
    /// IOSImplementation.isTablet() answers `isDesktop() || ...`, and this port
    /// forces isDesktop() true, so the inherited implementation reported
    /// `tablet`, `ios`, `ipad`. Resources.openLayered then applied an
    /// application's iOS and iPad layers on a Mac and never its `_desktop.ovr`
    /// -- the wrong artwork on the one platform that has a desktop layer to
    /// apply.
    ///
    /// The JavaSE desktop port answers `desktop`, `tablet` for the same reason,
    /// and matching it means one override written for the desktop covers both.
    /// `mac` follows so a layer can name this port specifically, the way `ipad`
    /// does on iOS.
    @Override
    public String[] getPlatformOverrides() {
        return new String[] {"desktop", "tablet", "mac"};
    }

    /// The natives this class needs directly. The window manager owns its own;
    /// density is asked for long before any secondary window exists.
    private final MacNative macNative = new MacNative();

    /// @inheritDoc
    ///
    /// From the monitor's backing scale, not from how wide the window is.
    ///
    /// The inherited iOS answer reads isTablet() -- true here -- and then treats
    /// any display at least 1100 pixels wide as a Retina iPad, so an ordinary 1x
    /// desktop window got DENSITY_VERY_HIGH and about twice the theme scaling it
    /// should have. A desktop's width says nothing about its density. This is the
    /// same rule the JavaSE desktop port applies to its own retina scale, so the
    /// two desktops agree.
    ///
    /// Deliberately not cached, unlike the iOS implementation: dragging a window
    /// between a Retina and a 1x monitor changes the answer, and a cached one
    /// would keep scaling for the display the application started on.
    @Override
    public int getDeviceDensity() {
        int scaleTimes100 = macNative.macMonitorScaleTimes100(
                Math.max(0, macNative.macMonitorForMainWindow()));
        if (scaleTimes100 <= 0) {
            return super.getDeviceDensity();
        }
        return scaleTimes100 > 150
                ? com.codename1.ui.Display.DENSITY_VERY_HIGH
                : com.codename1.ui.Display.DENSITY_MEDIUM;
    }

    /// @inheritDoc
    ///
    /// Scaled off the monitor rather than off the iPad constants.
    ///
    /// The inherited version picks a pixels-per-millimetre figure from the same
    /// width heuristic getDeviceDensity() used, so an ordinary 1x desktop window
    /// was handed the Retina-iPad constant and every millimetre-based dimension
    /// came out roughly twice its intended size. It cannot simply defer to the
    /// density-based mapping in CodenameOneImplementation either, because super
    /// here is IOSImplementation, which is the implementation being corrected.
    ///
    /// Five pixels per millimetre at 1x is what that mapping gives DENSITY_MEDIUM
    /// and is about right for a desktop display; a Retina Mac has twice the
    /// pixels in the same millimetre, so the scale multiplies it directly. Note
    /// this deliberately does NOT use macMonitorDpi(): NSDeviceResolution reports
    /// the backing store's nominal 72 or 144, not the physical density these
    /// units need.
    @Override
    public int convertToPixels(int dipCount, boolean horizontal) {
        int scaleTimes100 = macNative.macMonitorScaleTimes100(
                Math.max(0, macNative.macMonitorForMainWindow()));
        if (scaleTimes100 <= 0) {
            return super.convertToPixels(dipCount, horizontal);
        }
        return (int) Math.round(((double) dipCount) * 5.0 * scaleTimes100 / 100.0);
    }

    /// @inheritDoc
    ///
    /// The arrows, mapped the way every other desktop port maps them.
    ///
    /// CN1MacKeyCodeForEvent already emits the JavaSE convention -- -91 up, -92
    /// down, -93 left, -94 right -- but the inherited iOS implementation turns
    /// any code at or below -20 into its negation, so Up arrived as 91 and no
    /// GAME_* comparison in Form, Window or any list ever matched it. Keyboard
    /// focus traversal and arrow navigation therefore did nothing at all on this
    /// port. Anything else still gets the iOS answer, which is what the media
    /// keys rely on.
    ///
    /// Return is the fire key, as it is on JavaSE, but note the code differs:
    /// JavaSE emits -90 for VK_ENTER while CN1MacKeyCode emits 10, so 10 is what
    /// is mapped here. Without it Button.keyPressed()/keyReleased() -- which
    /// activate only on GAME_FIRE -- never fire, and the port can traverse to a
    /// button with the arrows but cannot press it.
    @Override
    public int getGameAction(int keyCode) {
        switch (keyCode) {
            case -91: return com.codename1.ui.Display.GAME_UP;
            case -92: return com.codename1.ui.Display.GAME_DOWN;
            case -93: return com.codename1.ui.Display.GAME_LEFT;
            case -94: return com.codename1.ui.Display.GAME_RIGHT;
            case 10: return com.codename1.ui.Display.GAME_FIRE;
            default: return super.getGameAction(keyCode);
        }
    }

    /// Names the window a component lives in, so a text session binds to it.
    ///
    /// The native side records the session's owner when it starts, and without
    /// this it would take the key window -- right when the user clicked into the
    /// field, wrong when the application called startEditingAsync() on a field
    /// in a window that is visible but not key. Only this class can answer it:
    /// the shared editing entry points take no window, and the component knows
    /// its own top level.
    private void nameEditingWindow(com.codename1.ui.Component cmp) {
        macNative.macTextInputSetOwnerWindow(ownerWindowId(cmp));
    }

    /// Which surface the native side should treat as the owner of the next
    /// presentation or editing session.
    ///
    /// Three answers, not two. A component in a secondary Window gives that
    /// window's framework id. A component on the main Form gives -2, the main
    /// rendering surface: it has no Window and therefore no id, and answering
    /// "unknown" for it let the pending owner be cleared, which fell back to
    /// whichever window was key -- the wrong one in exactly the case worth
    /// naming, a picker opened for the main Form while a secondary window has
    /// focus. -1 is the only genuinely unknown case, a null source, and is the
    /// one entitled to fall back.
    ///
    /// -2 for the main window and -1 for none are the values createWindow()
    /// already uses for ownership, rather than a second convention.
    ///
    /// The value is the framework's window id, NOT the native slot: a slot is an
    /// index into the window table and is reused once a window is disposed,
    /// while an id belongs to the Window and starts at 1. Handing one where the
    /// other was expected named a different window, or none, and the ownership
    /// check then refused the field's keys.
    private static int ownerWindowId(com.codename1.ui.Component cmp) {
        if (cmp == null) {
            return -1;
        }
        com.codename1.ui.TopLevelContainer top = cmp.getTopLevelContainer();
        if (top instanceof com.codename1.ui.Window) {
            return ((com.codename1.ui.Window) top).getWindowId();
        }
        return -2;
    }

    /// @inheritDoc
    ///
    /// Names the source component's window so the popover anchors there.
    ///
    /// The coordinates handed to the native picker are relative to the source
    /// component's window, but the presentation anchored in the KEY window --
    /// right when the user tapped the Picker, wrong when the application opened
    /// one for a component in another visible window, which put the popover over
    /// the wrong window at unrelated coordinates.
    @Override
    public Object showNativePicker(int type, com.codename1.ui.Component source,
            Object currentValue, Object data) {
        nameNextPresentationWindow(source);
        return super.showNativePicker(type, source, currentValue, data);
    }

    /// Names the surface a component lives on for the next popover presentation.
    private void nameNextPresentationWindow(com.codename1.ui.Component cmp) {
        macNative.macPresentationSetOwnerWindow(ownerWindowId(cmp));
    }

    /// @inheritDoc
    ///
    /// Names the editing component's window before the shared path starts the
    /// session, which is what keeps a programmatic edit in a non-key window from
    /// binding the session to whichever window happens to be key.
    @Override
    public void editString(com.codename1.ui.Component cmp, int maxSize, int constraint,
            String text, int initiatingKeycode) {
        nameEditingWindow(cmp);
        super.editString(cmp, maxSize, constraint, text, initiatingKeycode);
    }

    /// @inheritDoc
    ///
    /// As `#editString(com.codename1.ui.Component, int, int, String, int)`: the
    /// pure editor reaches the same session and needs the same owner.
    @Override
    public Object startTextInput(com.codename1.ui.TextInputClient client,
            com.codename1.ui.TextInputConfig config) {
        // TextInputClient declares no component accessor, but every client the
        // pure editor uses IS one -- EditField, CodeEditor, RichTextArea. A
        // client that is not leaves the owner unnamed and the native side falls
        // back to the key window, which is the old behaviour and no worse.
        nameEditingWindow(client instanceof com.codename1.ui.Component
                ? (com.codename1.ui.Component) client : null);
        return super.startTextInput(client, config);
    }

    /// @inheritDoc
    ///
    /// From the modifier state AppKit last reported, rather than the base
    /// implementation's unconditional false.
    ///
    /// These four are how application code asks what was held during a key
    /// press, and every one of them answered false on this port: the modifier
    /// mask reached pointer and wheel listeners but nothing else, so Shift-Tab
    /// was indistinguishable from Tab outside the native menu and text paths.
    /// The native side records the flags from key events AND from flagsChanged:,
    /// because a modifier pressed on its own produces no key event at all.
    @Override
    public boolean isShiftKeyDown() {
        return (macNative.macCurrentModifiers() & 1) != 0;
    }

    /// @inheritDoc
    ///
    /// See `#isShiftKeyDown()`.
    @Override
    public boolean isControlKeyDown() {
        return (macNative.macCurrentModifiers() & 2) != 0;
    }

    /// @inheritDoc
    ///
    /// Option is what Alt is called on a Mac keyboard. See `#isShiftKeyDown()`.
    @Override
    public boolean isAltKeyDown() {
        return (macNative.macCurrentModifiers() & 4) != 0;
    }

    /// @inheritDoc
    ///
    /// Command is Meta here. See `#isShiftKeyDown()`.
    @Override
    public boolean isMetaKeyDown() {
        return (macNative.macCurrentModifiers() & 8) != 0;
    }

    /// @inheritDoc
    ///
    /// False. A Mac reports one pointer, and this port only ever sends one.
    ///
    /// Every pointer path in METALView delivers a length of 1, because an NSEvent
    /// carries a single cursor location -- the trackpad's extra fingers arrive as
    /// gestures such as magnify, not as additional pointers. The inherited answer
    /// is an unconditional true earned by a touchscreen, so an application that
    /// asks before enabling a multi-touch interaction enabled one that can never
    /// be driven here.
    @Override
    public boolean isMultiTouch() {
        return false;
    }

    /// @inheritDoc
    ///
    /// False, because this port has no call detector to speak for.
    ///
    /// The inherited answer is an unconditional true, and it is earned there by
    /// applicationWillResignActive() setting the flag isInCall() returns -- on
    /// iOS, resigning active is how a call announces itself. This port routes
    /// that transition to macApplicationWillResignActive() instead, deliberately,
    /// because on a Mac it only means another application came to the front. So
    /// nothing ever sets the flag and isInCall() is permanently false. Claiming
    /// the capability while always answering "no call" is worse than declining
    /// it: a caller that checks first cannot tell the difference between a quiet
    /// line and a detector that never fires. Flip this when a macOS backend
    /// exists to set the flag.
    @Override
    public boolean isCallDetectionSupported() {
        return false;
    }

    /// @inheritDoc
    ///
    /// The reverse of `#getGameAction(int)`, so code that asks which key drives
    /// an action gets an answer rather than the inherited -1.
    @Override
    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case com.codename1.ui.Display.GAME_UP: return -91;
            case com.codename1.ui.Display.GAME_DOWN: return -92;
            case com.codename1.ui.Display.GAME_LEFT: return -93;
            case com.codename1.ui.Display.GAME_RIGHT: return -94;
            case com.codename1.ui.Display.GAME_FIRE: return 10;
            default: return super.getKeyCode(gameAction);
        }
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
