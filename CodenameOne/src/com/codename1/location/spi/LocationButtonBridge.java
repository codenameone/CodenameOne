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
package com.codename1.location.spi;

import com.codename1.location.LocationManager;
import com.codename1.ui.PeerComponent;
import com.codename1.util.SuccessCallback;

/// The platform half of [com.codename1.location.LocationButton].
///
/// A port implements this only when the platform draws a location button of
/// its own -- one the system renders and the system therefore trusts, so that
/// a tap grants precise location for a single session without the app holding
/// a persistent grant. Ports that have no such control implement nothing and
/// `com.codename1.ui.Display#getLocationButtonBridge()` keeps returning null,
/// which makes [com.codename1.location.LocationButton] fall back to an
/// ordinary Codename One button that asks for location the usual way.
///
/// Obtained through `com.codename1.ui.Display#getLocationButtonBridge()`.
/// Application code never touches it.
public interface LocationButtonBridge {

    /// True when this device will actually draw the system button.
    ///
    /// Separate from the bridge merely existing, because the answer is a
    /// runtime one: Android's button is system-rendered only from API 37, and
    /// the same build of the same port runs on both sides of that line. Below
    /// the line there is no system button to render and the caller uses its own,
    /// which is also what the platform's own compatibility path would fall back
    /// to.
    ///
    /// #### Returns
    ///
    /// whether a tap will go through the system's own button
    boolean isSupported();

    /// The platform's location manager, for a caller that already holds the
    /// grant.
    ///
    /// The system button hands back a session-scoped precise-location grant and
    /// the fix is fetched immediately afterwards, so the ordinary acquisition
    /// path -- which asks for ACCESS_FINE_LOCATION, and on an app that also
    /// geofences asks for ACCESS_BACKGROUND_LOCATION -- has nothing left to ask
    /// and much to get wrong: from Android 30 that second question is a trip to
    /// a settings screen, which is precisely the experience a transactional
    /// button exists to avoid.
    ///
    /// Implementations return the same manager the ordinary path would, minus
    /// the prompting. Null when the platform has none, which the caller treats
    /// as "no location" rather than as a reason to prompt.
    ///
    /// #### Returns
    ///
    /// the platform location manager, or null
    LocationManager getGrantedLocationManager();

    /// Enables or disables a button this bridge created.
    ///
    /// Needed because the system button is a NATIVE view, and Codename One's
    /// enabled flag never reaches one: neither `PeerComponent` nor the Android
    /// peer forwards `setEnabled` to the view it wraps, so a disabled
    /// [com.codename1.location.LocationButton] went on taking taps and asking
    /// for location. Every other component gets this for free by being drawn
    /// and hit-tested by the framework.
    ///
    /// Implementations must tolerate a peer they did not create and a peer
    /// whose native view has already gone away.
    ///
    /// #### Parameters
    ///
    /// - `button`: a peer returned by [#createButton(int,int,int,com.codename1.util.SuccessCallback,java.lang.Runnable)]
    /// - `enabled`: whether the button should accept taps
    void setButtonEnabled(PeerComponent button, boolean enabled);

    /// Creates the native button.
    ///
    /// The result reports only whether precise location was granted --
    /// turning a grant into a fix is [com.codename1.location.LocationButton]'s
    /// job, and it is the same job on every port.
    ///
    /// #### Parameters
    ///
    /// - `textType`: one of the `TEXT_` constants on
    ///   [com.codename1.location.LocationButton]
    /// - `backgroundColor`: an RRGGBB colour, or -1 to keep the platform's own
    /// - `textColor`: an RRGGBB colour, or -1 to keep the platform's own
    /// - `onResult`: invoked with the grant result, on any thread
    /// - `onUnavailable`: invoked, on any thread, when the system button this
    ///   device promised turns out not to work -- the platform session can fail
    ///   after the control is already on screen, and what is left behind is a
    ///   button that draws and does nothing. The caller replaces it with its own.
    ///
    /// #### Returns
    ///
    /// the peer, or null when this device cannot draw one
    PeerComponent createButton(int textType, int backgroundColor,
            int textColor, SuccessCallback<Boolean> onResult,
            Runnable onUnavailable);

    /// Whether a control this bridge built can no longer serve a tap.
    ///
    /// A platform may retire the context a control was built against while
    /// the control itself survives -- Android recreates its activity on a
    /// configuration change and the port re-attaches the SAME view -- and the
    /// system session a tap needs belongs to the retired one. The control
    /// still draws, so nothing else can tell.
    ///
    /// Asked before a peer that is already in place is left alone, and again
    /// on every LAYOUT of the component -- which is the only hook an Android
    /// activity recreation reaches, since it leaves the component initialised
    /// and re-initialises the native peers directly. So the answer has to be
    /// cheap and must not build anything: a field comparison is the intended
    /// shape, not a call into the platform. A bridge with no such notion
    /// returns false.
    ///
    /// @param button a peer this bridge returned from
    ///               [#createButton(int,int,int,SuccessCallback,Runnable)]
    /// @return whether it must be replaced rather than reused
    boolean isStale(PeerComponent button);
}
