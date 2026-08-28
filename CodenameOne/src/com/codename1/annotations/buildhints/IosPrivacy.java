/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.annotations.buildhints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// iOS `Info.plist` privacy usage descriptions. Set the one for every protected
/// resource your app touches: the build server accepts an app without them, and
/// the App Store rejects it.
///
/// Place this on your application's main class -- the class named by
/// `codename1.mainName`. An attribute you do not set is not written at all, so
/// the build server's own default applies. The `default` clause below each
/// attribute names a constant that says nothing -- see [HintUnset] -- and this
/// package deliberately does not record what the server would do instead,
/// because that is the server's to change.
///
/// The platform is stated once on the annotation, not on every attribute. An
/// attribute repeats it only to disagree with it.
@Hint(platform = "ios")
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface IosPrivacy {

    /// Why the app uses Bluetooth. Supplied automatically when the app references
    /// `com.codename1.bluetooth`; set it to say something more specific than the
    /// default.
    @Hint(name = "ios.NSBluetoothAlwaysUsageDescription")
    String bluetoothAlwaysUsageDescription() default "";

    /// The pre-iOS 13 spelling of the Bluetooth usage description, supplied and
    /// overridable on the same terms.
    @Hint(name = "ios.NSBluetoothPeripheralUsageDescription")
    String bluetoothPeripheralUsageDescription() default "";

    /// The text iOS shows when the app first asks for the calendars full access.
    /// It becomes the `NSCalendarsFullAccessUsageDescription` key in `Info.plist`.
    /// The App Store rejects an app that touches this resource without one.
    @Hint(name = "ios.NSCalendarsFullAccessUsageDescription")
    String calendarsFullAccessUsageDescription() default "";

    /// The text iOS shows when the app first asks for the calendars. It becomes
    /// the `NSCalendarsUsageDescription` key in `Info.plist`. The App Store
    /// rejects an app that touches this resource without one.
    @Hint(name = "ios.NSCalendarsUsageDescription")
    String calendarsUsageDescription() default "";

    /// The text iOS shows when the app first asks for the calendars write only
    /// access. It becomes the `NSCalendarsWriteOnlyAccessUsageDescription` key in
    /// `Info.plist`. The App Store rejects an app that touches this resource
    /// without one.
    @Hint(name = "ios.NSCalendarsWriteOnlyAccessUsageDescription")
    String calendarsWriteOnlyAccessUsageDescription() default "";

    /// The text iOS shows when the app first asks for the camera. It becomes the
    /// `NSCameraUsageDescription` key in `Info.plist`. The App Store rejects an
    /// app that touches this resource without one.
    @Hint(name = "ios.NSCameraUsageDescription")
    String cameraUsageDescription() default "";

    /// The text iOS shows when the app first asks for the health share. It becomes
    /// the `NSHealthShareUsageDescription` key in `Info.plist`. The App Store
    /// rejects an app that touches this resource without one.
    @Hint(name = "ios.NSHealthShareUsageDescription")
    String healthShareUsageDescription() default "";

    /// The text iOS shows when the app first asks for the health update. It
    /// becomes the `NSHealthUpdateUsageDescription` key in `Info.plist`. The App
    /// Store rejects an app that touches this resource without one.
    @Hint(name = "ios.NSHealthUpdateUsageDescription")
    String healthUpdateUsageDescription() default "";

    /// The text iOS shows when the app first asks for the local network. It
    /// becomes the `NSLocalNetworkUsageDescription` key in `Info.plist`. The App
    /// Store rejects an app that touches this resource without one.
    @Hint(name = "ios.NSLocalNetworkUsageDescription")
    String localNetworkUsageDescription() default "";

    /// The text iOS shows when the app first asks for the location always and when
    /// in use. It becomes the `NSLocationAlwaysAndWhenInUseUsageDescription` key
    /// in `Info.plist`. The App Store rejects an app that touches this resource
    /// without one.
    @Hint(name = "ios.NSLocationAlwaysAndWhenInUseUsageDescription")
    String locationAlwaysAndWhenInUseUsageDescription() default "";

    /// The text iOS shows when the app first asks for the location always. It
    /// becomes the `NSLocationAlwaysUsageDescription` key in `Info.plist`. The App
    /// Store rejects an app that touches this resource without one.
    @Hint(name = "ios.NSLocationAlwaysUsageDescription")
    String locationAlwaysUsageDescription() default "";

    /// The text iOS shows when the app first asks for the location when in use. It
    /// becomes the `NSLocationWhenInUseUsageDescription` key in `Info.plist`. The
    /// App Store rejects an app that touches this resource without one.
    @Hint(name = "ios.NSLocationWhenInUseUsageDescription")
    String locationWhenInUseUsageDescription() default "";

    /// The text iOS shows when the app first asks for the microphone. It becomes
    /// the `NSMicrophoneUsageDescription` key in `Info.plist`. The App Store
    /// rejects an app that touches this resource without one.
    @Hint(name = "ios.NSMicrophoneUsageDescription")
    String microphoneUsageDescription() default "";

    /// The pre-iOS 16 spelling of the nearby-interaction usage description,
    /// supplied automatically when the app references the nearby APIs.
    @Hint(name = "ios.NSNearbyInteractionAllowOnceUsageDescription")
    String nearbyInteractionAllowOnceUsageDescription() default "";

    /// Why the app measures distance and direction to nearby devices. Supplied
    /// automatically when the app references the nearby APIs; set it to say
    /// something more specific than the default.
    @Hint(name = "ios.NSNearbyInteractionUsageDescription")
    String nearbyInteractionUsageDescription() default "";

    /// The text iOS shows when the app first asks for the reminders full access.
    /// It becomes the `NSRemindersFullAccessUsageDescription` key in `Info.plist`.
    /// The App Store rejects an app that touches this resource without one.
    @Hint(name = "ios.NSRemindersFullAccessUsageDescription")
    String remindersFullAccessUsageDescription() default "";

    /// The text iOS shows when the app first asks for the reminders. It becomes
    /// the `NSRemindersUsageDescription` key in `Info.plist`. The App Store
    /// rejects an app that touches this resource without one.
    @Hint(name = "ios.NSRemindersUsageDescription")
    String remindersUsageDescription() default "";

    /// Why the app sends speech for recognition. Supplied automatically when the
    /// app references the speech APIs; set it to say something more specific.
    @Hint(name = "ios.NSSpeechRecognitionUsageDescription")
    String speechRecognitionUsageDescription() default "";
}
