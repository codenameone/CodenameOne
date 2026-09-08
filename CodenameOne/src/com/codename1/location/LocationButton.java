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
package com.codename1.location;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.List;

/// A button that asks for the device location once, for a single transaction.
///
/// This is the Codename One face of the platform's own location button. From
/// Android 17 (API level 37) Google Play requires transactional precise-location
/// use -- "find restaurants near me", filling in an address, tagging a photo --
/// to go through a button the *system* draws, rather than through an app-held
/// `ACCESS_FINE_LOCATION` grant. A tap on a button the system drew earns a
/// session-scoped grant, so the application never has to hold precise location
/// permanently.
///
/// ```java
/// LocationButton b = new LocationButton(LocationButton.TEXT_USE_PRECISE_LOCATION);
/// b.addLocationSharedListener(loc -> {
///     if (loc != null) {
///         search(loc.getLatitude(), loc.getLongitude());
///     }
/// });
/// form.add(b);
/// ```
///
/// #### Where the system draws it, and where it does not
///
/// On a platform that has such a control -- today only Android 17 and up -- this
/// component *is* that control: it is rendered by the system in its own process
/// and this application cannot restyle its label or intercept its taps, which is
/// exactly what makes the grant trustworthy. Everywhere else, and on older
/// Android, the component is an ordinary Codename One [com.codename1.ui.Button]
/// that asks for the location permission the usual way. Either way the listener
/// receives a [Location] or null, so an application is written once.
///
/// [#isSystemRendered()] reports which of the two this button ended up with,
/// for an application that wants to say something different about it. Ask
/// `com.codename1.ui.Display#isLocationButtonSupported()` instead when the
/// question is what the platform can do rather than what happened here.
///
/// #### On Android
///
/// Referencing this class makes the build add `USE_LOCATION_BUTTON` to the
/// manifest, which the platform requires before it will render the control.
/// Nothing else is needed.
///
/// An application whose *only* location use is transactional can go one step
/// further and declare that precise location is reachable through the button
/// alone, which removes the "allow precise location" question from the app
/// entirely. That is a manifest attribute rather than an API, so it is set
/// through the `android.xpermissions` build hint:
///
/// ```
/// codename1.arg.android.xpermissions=<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:usesPermissionFlags="onlyForLocationButton" />
/// ```
///
/// Do not set that in an application that also tracks, navigates or geofences:
/// those need a grant this flag takes away.
public class LocationButton extends Container {

    /// An icon with no label. The narrowest form of the control.
    public static final int TEXT_NONE = 0;

    /// Labelled "Precise location".
    public static final int TEXT_PRECISE_LOCATION = 1;

    /// Labelled "Use precise location". The default.
    public static final int TEXT_USE_PRECISE_LOCATION = 2;

    /// Labelled "Share precise location".
    public static final int TEXT_SHARE_PRECISE_LOCATION = 3;

    /// Labelled "Near my precise location".
    public static final int TEXT_NEAR_MY_PRECISE_LOCATION = 4;

    /// Labelled "Near your precise location".
    public static final int TEXT_NEAR_YOUR_PRECISE_LOCATION = 5;

    /// Passed to the platform for a colour the application did not choose, so
    /// the system picks its own. -1 rather than 0 because black is a colour a
    /// caller can legitimately ask for.
    private static final int UNSET_COLOR = -1;

    private static final int MAX_TEXT_TYPE = TEXT_NEAR_YOUR_PRECISE_LOCATION;

    /// The smallest the platform will draw its own control.
    ///
    /// Android's minimum touch target is 48dp. Codename One's "dips" are
    /// millimetres, and 48dp is 0.3in, which is 7.62mm -- but asking for
    /// exactly that measured 122px against the platform's own 126px, because
    /// [com.codename1.ui.Display#convertToPixels(float)] rounds through an int
    /// twice. 8mm is 50.4dp before that loss and stays above 48dp after it, at
    /// any density, which is what stops the platform clamping.
    private static final float MIN_TOUCH_TARGET_MM = 8f;

    private int textType;
    private int buttonBackgroundColor = UNSET_COLOR;
    private int buttonTextColor = UNSET_COLOR;
    private long timeout = 30000;
    private boolean acquiring;

    /// Always built, and used for the preferred size even when the platform
    /// draws the control -- the system button has no size of its own, we tell it
    /// how big to be, and telling it the size an ordinary themed button would
    /// have is what keeps it looking like part of the application.
    private final Button fallback;

    private PeerComponent peer;

    /// Set once the platform's own control has failed on this device. It is not
    /// asked for again: it already answered, and retrying would put the dead
    /// control back in a component that has visibly recovered from it.
    private boolean platformFailed;

    /// Which built child the live platform callback belongs to.
    ///
    /// A setter rebuilds the child, and the control it removes can still have a
    /// callback queued on the platform's side. Every peer calls the same
    /// [#permissionResult] on this component, so without this a late error from
    /// the replaced control would call [#useFallback()] and remove the good one,
    /// and a late grant would start an acquisition nobody asked for. The
    /// per-view guard on the Android side cannot see this: the replacement is a
    /// different view with its own generation.
    ///
    /// Only ever read or written on the EDT, which is where components are
    /// built and where the callback is delivered.
    private int childGeneration;

    private final List<LocationSharedListener> listeners =
            new ArrayList<LocationSharedListener>();

    /// A button labelled "Use precise location".
    public LocationButton() {
        this(TEXT_USE_PRECISE_LOCATION);
    }

    /// A button with one of the labels the platform offers.
    ///
    /// #### Parameters
    ///
    /// - `textType`: one of the `TEXT_` constants
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `textType` is not one of them
    public LocationButton(int textType) {
        super(new BorderLayout());
        checkTextType(textType);
        this.textType = textType;
        setUIID("Container");
        getAllStyles().stripMarginAndPadding();
        fallback = new Button(labelFor(textType), "LocationButton");
        FontImage.setMaterialIcon(fallback, FontImage.MATERIAL_MY_LOCATION);
        fallback.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                acquire();
            }
        });
    }

    /// Whether a tap on *this* button goes through a control the system itself
    /// drew.
    ///
    /// False means this component is not showing the system's control: every
    /// platform other than Android, Android below API level 37, before the
    /// component has been shown, in the moment between the control being
    /// created and the system opening its session, and on a device where the
    /// platform HAS the control but its session failed and [#useFallback()]
    /// replaced it.
    ///
    /// That last case is why this is an instance question rather than a static
    /// one. `Display.isLocationButtonSupported()` answers what the platform can
    /// do and is the right call to make before building anything; only the
    /// component knows what actually ended up on screen.
    ///
    /// #### Returns
    ///
    /// whether the system drew the control this button is showing
    public boolean isSystemRendered() {
        // Not just "a peer exists". A control the system renders in another
        // process is created here and drawn into later, so between the two
        // there is a peer that is present and blank -- and a session that never
        // opens leaves it that way, with nothing reporting a failure to fall
        // back from. Asking the platform whether it is actually drawing is the
        // only way to tell that apart from a working control.
        return peer != null && Display.getInstance().isLocationButtonReady(peer);
    }

    /// The label this button carries.
    ///
    /// #### Returns
    ///
    /// one of the `TEXT_` constants
    public int getTextType() {
        return textType;
    }

    /// Chooses the label this button carries.
    ///
    /// #### Parameters
    ///
    /// - `textType`: one of the `TEXT_` constants
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `textType` is not one of them
    public void setTextType(int textType) {
        checkTextType(textType);
        if (this.textType == textType) {
            return;
        }
        this.textType = textType;
        fallback.setText(labelFor(textType));
        rebuildChild();
    }

    /// Asks the platform to draw the button on this background colour.
    ///
    /// Only reaches the system-rendered control; the fallback button is themed
    /// through its `LocationButton` UIID like any other component.
    ///
    /// #### Parameters
    ///
    /// - `color`: an RRGGBB colour, or -1 to let the system choose
    public void setButtonBackgroundColor(int color) {
        if (buttonBackgroundColor == color) {
            return;
        }
        this.buttonBackgroundColor = color;
        rebuildChild();
    }

    /// The background colour asked of the platform, or -1.
    ///
    /// #### Returns
    ///
    /// an RRGGBB colour or -1
    public int getButtonBackgroundColor() {
        return buttonBackgroundColor;
    }

    /// Asks the platform to draw the button's label in this colour.
    ///
    /// #### Parameters
    ///
    /// - `color`: an RRGGBB colour, or -1 to let the system choose
    public void setButtonTextColor(int color) {
        if (buttonTextColor == color) {
            return;
        }
        this.buttonTextColor = color;
        rebuildChild();
    }

    /// The label colour asked of the platform, or -1.
    ///
    /// #### Returns
    ///
    /// an RRGGBB colour or -1
    public int getButtonTextColor() {
        return buttonTextColor;
    }

    /// How long to wait for a fix once the request has been granted.
    ///
    /// #### Returns
    ///
    /// the timeout in milliseconds, or -1 to wait indefinitely
    public long getTimeout() {
        return timeout;
    }

    /// How long to wait for a fix once the request has been granted.
    ///
    /// A cold GPS fix is legitimately slow, so this is deliberately generous;
    /// on expiry the listener is invoked with null rather than left hanging.
    ///
    /// #### Parameters
    ///
    /// - `timeout`: milliseconds, or -1 to wait indefinitely
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /// Registers a listener for the location this button obtains.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addLocationSharedListener(LocationSharedListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    /// Removes a previously registered listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeLocationSharedListener(LocationSharedListener l) {
        listeners.remove(l);
    }

    /// The size an ordinary themed button with the same label would have, but
    /// never below the platform's minimum touch target.
    ///
    /// The system-rendered control is given whatever size we ask for, so this
    /// answer is used for both paths and the two look alike. The floor is not a
    /// nicety: Android clamps a location button up to 48dp and says so in the
    /// log ("Clamping height up from 60 to 126 px"), and the button it then
    /// draws overflows the slot Codename One laid out for it -- observed on an
    /// Android 17 emulator, where the control was visibly cut in half.
    ///
    /// #### Returns
    ///
    /// the preferred size
    @Override
    protected Dimension calcPreferredSize() {
        Dimension size = new Dimension(fallback.getPreferredSize());
        int floor = Display.getInstance().convertToPixels(MIN_TOUCH_TARGET_MM);
        if (size.getHeight() < floor) {
            size.setHeight(floor);
        }
        // Both axes. Only the height was seen to clip -- the platform logged
        // that clamp -- but 48dp is a minimum touch target, which has two of
        // them, and the width is what the system surface gets created at
        // exactly like the height. TEXT_NONE is an icon with no label, so it is
        // the form that can get there. Whether the platform also clamps width
        // was not measured; the floor does not depend on it.
        if (size.getWidth() < floor) {
            size.setWidth(floor);
        }
        return size;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (getComponentCount() == 0) {
            buildChild();
        }
    }

    /// Puts either the platform's control or the fallback button in place.
    private void buildChild() {
        if (!platformFailed && Display.getInstance().isLocationButtonSupported()) {
            final int forGeneration = ++childGeneration;
            peer = Display.getInstance().createLocationButton(textType,
                    buttonBackgroundColor, buttonTextColor,
                    new SuccessCallback<Boolean>() {
                        @Override
                        public void onSucess(Boolean granted) {
                            permissionResult(granted, forGeneration);
                        }
                    });
            if (peer != null) {
                add(BorderLayout.CENTER, peer);
                return;
            }
        }
        peer = null;
        add(BorderLayout.CENTER, fallback);
    }

    /// Builds the child again so a setter that arrived after the first show
    /// reaches the platform's control.
    ///
    /// The system button is configured when its session is opened and the
    /// component builds its child exactly once, so without this a setter would
    /// silently move the field and change nothing on screen -- the worst
    /// outcome, because the application has every reason to believe it worked.
    ///
    /// Nothing to do before the first build: `initComponent` will use whatever
    /// the fields say by then. Nothing to do for the fallback either -- it is
    /// an ordinary Codename One button that has already taken its new text and
    /// takes its colours from the theme.
    private void rebuildChild() {
        if (peer == null) {
            return;
        }
        removeComponent(peer);
        peer = null;
        buildChild();
        revalidateSelf();
    }

    /// The platform's answer to a tap, or to the session it opened.
    ///
    /// #### Parameters
    ///
    /// - `granted`: TRUE when the user shared their location, FALSE when they
    ///   declined, and null when the platform's control failed -- which can
    ///   arrive without a tap, because the session is opened when the control
    ///   is attached
    private void permissionResult(final Boolean granted, final int forGeneration) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (forGeneration != childGeneration) {
                    // From a control this component has already replaced.
                    return;
                }
                // Deliberately NOT also invalidated by deinitialization, which
                // review has asked for: remove-and-immediately-re-add would then
                // let a queued result through, so bump childGeneration there
                // too and the window closes.
                //
                // The window is between the platform calling back on the Android
                // UI thread and the next slice of this one -- anything arriving
                // after the view detaches is already rejected on the native
                // side, where closing a session ends its generation. What can
                // slip through is a result that was real when it was produced:
                // the user tapped a control the system drew and answered it, and
                // the grant it refers to still holds. Delivering that a beat
                // late to the same component is not wrong.
                //
                // Bumping on deinitialization buys nothing against it and costs
                // the opposite error -- a genuine answer dropped because the
                // application happened to move the component in the same
                // millisecond. Between a late-but-true result and a lost one,
                // this takes the late one.
                if (!isInitialized()) {
                    // The component left the form between the platform's
                    // callback and this hop onto the EDT, so the control that
                    // produced this answer is no longer on screen. Acting would
                    // start acquiring a location for something the user has
                    // navigated away from.
                    //
                    // isInitialized() rather than "does the peer still have a
                    // live session", which would also have worked here -- the
                    // session was measured still open at grant time, the system's
                    // consent activity pauses the application without closing it.
                    // But that check reads a state the platform owns, and if a
                    // platform ever closed its session on the grant itself it
                    // would drop every result instead of the stale ones. This
                    // one is ours and cannot misfire that way. Pausing does not
                    // deinitialize a component, and neither does the rotation
                    // that detaches and reattaches the native view.
                    return;
                }
                if (granted == null) {
                    // Deliberately no listener call. A failed session is not an
                    // answer from the user: it usually arrives before the
                    // control has been touched at all, so reporting it as a null
                    // location would announce a decline nobody made. And it
                    // strands nobody -- a tap on the system's control is not
                    // observable to the application, so there is no pending
                    // request waiting to be resolved. What the application can
                    // see is isSystemRendered(), which now says false.
                    useFallback();
                    return;
                }
                if (granted.booleanValue()) {
                    acquire();
                } else {
                    fireLocationShared(null);
                }
            }
        });
    }

    /// Replaces a control the platform could not open with the ordinary button.
    ///
    /// The alternative is a control that is present, correct and dead, which is
    /// the worst way for this to fail: the build and the manifest are both fine
    /// and nothing says otherwise.
    private void useFallback() {
        platformFailed = true;
        // Nothing else the dead control says is worth hearing.
        childGeneration++;
        if (peer == null) {
            return;
        }
        removeComponent(peer);
        peer = null;
        add(BorderLayout.CENTER, fallback);
        revalidateSelf();
    }

    private void revalidateSelf() {
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
        } else {
            revalidate();
        }
    }

    /// Fetches the fix and tells the listeners, on the EDT.
    ///
    /// Called after a granted system session, and directly from the fallback
    /// button's action -- in which case obtaining the manager is what asks the
    /// user for permission.
    private void acquire() {
        if (acquiring) {
            return;
        }
        acquiring = true;
        try {
            final LocationManager manager = LocationManager.getLocationManager();
            if (manager == null) {
                fireLocationShared(null);
                return;
            }
            final Location[] result = new Location[1];
            // invokeAndBlock so a cold fix does not freeze the form; the EDT
            // keeps pumping while the platform looks for one.
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    result[0] = manager.getCurrentLocationSync(timeout);
                }
            });
            fireLocationShared(result[0]);
        } finally {
            acquiring = false;
        }
    }

    private void fireLocationShared(Location location) {
        // A copy, because a listener is entitled to remove itself while it
        // is being told.
        List<LocationSharedListener> copy =
                new ArrayList<LocationSharedListener>(listeners);
        for (LocationSharedListener l : copy) {
            l.locationShared(location);
        }
    }

    private static void checkTextType(int textType) {
        if (textType < TEXT_NONE || textType > MAX_TEXT_TYPE) {
            throw new IllegalArgumentException("Unknown text type: " + textType);
        }
    }

    /// The wording the platform uses, for the fallback button to match.
    ///
    /// Localized through the resource bundle so an application that translates
    /// its UI translates this too; the platform localizes its own control.
    private static String labelFor(int textType) {
        switch (textType) {
            case TEXT_NONE:
                return "";
            case TEXT_PRECISE_LOCATION:
                return localize("LocationButton.preciseLocation", "Precise location");
            case TEXT_SHARE_PRECISE_LOCATION:
                return localize("LocationButton.sharePreciseLocation", "Share precise location");
            case TEXT_NEAR_MY_PRECISE_LOCATION:
                return localize("LocationButton.nearMyPreciseLocation", "Near my precise location");
            case TEXT_NEAR_YOUR_PRECISE_LOCATION:
                return localize("LocationButton.nearYourPreciseLocation", "Near your precise location");
            default:
                return localize("LocationButton.usePreciseLocation", "Use precise location");
        }
    }

    private static String localize(String key, String defaultValue) {
        UIManager manager = UIManager.getInstance();
        if (manager == null) {
            return defaultValue;
        }
        return manager.localize(key, defaultValue);
    }
}
