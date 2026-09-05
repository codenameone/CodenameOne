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
package com.codename1.impl.android.locationbutton;

import android.app.Activity;
import android.os.Build;
import android.view.View;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.io.Log;
import com.codename1.location.LocationManager;
import com.codename1.location.spi.LocationButtonBridge;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.util.SuccessCallback;

import androidx.core.locationbutton.LocationButton;
import androidx.core.locationbutton.OnErrorListener;
import androidx.core.locationbutton.OnPermissionResultListener;
import androidx.core.locationbutton.OnRequestPermissionsListener;

/// Carries [com.codename1.location.LocationButton] onto the platform's own
/// location button.
///
/// The control is `androidx.core.locationbutton.LocationButton`, which from
/// Android 17 (API 37) hands its rendering to the system: the button is drawn
/// by another process into a `SurfaceView`, and a tap on a button the system
/// itself drew is what earns the session-scoped precise-location grant Google
/// Play requires for transactional use.
///
/// Below API 37 there is no system button. The library does carry a locally
/// drawn stand-in, and this bridge deliberately does not use it -- see
/// [#isSupported()].
public class AndroidLocationButtonBridge implements LocationButtonBridge {

    /// The level at which the platform started drawing the button itself. Read
    /// off the library's own gate rather than assumed: `LocationButton` decides
    /// between its remote and local paths on exactly this comparison.
    private static final int SYSTEM_RENDERED_SDK = 37;

    /// Passed for a colour the caller did not choose. Matches the constant
    /// [com.codename1.location.LocationButton] documents; -1 rather than 0
    /// because black is a colour a caller can legitimately ask for.
    private static final int UNSET_COLOR = -1;

    public boolean isSupported() {
        if (Build.VERSION.SDK_INT < SYSTEM_RENDERED_SDK) {
            // The library's own fallback below 37 is an ordinary view that
            // requests ACCESS_FINE_LOCATION through an ActivityResultLauncher,
            // and it registers that launcher only when the host activity is an
            // androidx ActivityResultRegistryOwner. CodenameOneActivity extends
            // android.app.Activity and is not one, so the fallback throws
            // IllegalStateException on the first tap. Codename One's own button
            // does the same job, is themed with the rest of the application and
            // asks through the permission machinery every other Codename One
            // API already uses -- so below 37 there is nothing here worth
            // having, and the component uses that instead.
            return false;
        }
        return AndroidImplementation.getActivity() != null;
    }

    public PeerComponent createButton(final int textType,
            final int backgroundColor, final int textColor,
            final SuccessCallback<Boolean> onResult,
            final Runnable onUnavailable) {
        final Activity activity = AndroidImplementation.getActivity();
        if (activity == null || !isSupported()) {
            return null;
        }
        final View[] created = new View[1];
        // Measured on the UI thread beside the build, because measure() is a
        // UI-thread call and because the caller needs the size before it can
        // lay anything out.
        final int[] measured = new int[2];
        final Throwable[] failure = new Throwable[1];
        final boolean[] done = new boolean[1];
        final Object lock = new Object();
        // On the Android UI thread, because a View may only be constructed
        // there, and blocking until it exists because the caller needs the peer
        // to put in a Container it is building right now.
        activity.runOnUiThread(new Runnable() {
            public void run() {
                synchronized (lock) {
                    try {
                        created[0] = build(activity, textType, backgroundColor,
                                textColor, onResult, onUnavailable);
                        if (created[0] != null) {
                            int unspecified = View.MeasureSpec.makeMeasureSpec(
                                    0, View.MeasureSpec.UNSPECIFIED);
                            created[0].measure(unspecified, unspecified);
                            measured[0] = created[0].getMeasuredWidth();
                            measured[1] = created[0].getMeasuredHeight();
                        }
                    } catch (Throwable err) {
                        failure[0] = err;
                    }
                    // A flag rather than "created[0] != null": a build that
                    // returned null without throwing would otherwise leave the
                    // caller waiting on a notification that had already fired.
                    done[0] = true;
                    lock.notifyAll();
                }
            }
        });
        synchronized (lock) {
            while (!done[0]) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {
                    return null;
                }
            }
        }
        if (failure[0] != null) {
            // THROWN, not swallowed into a null. The component distinguishes
            // the two: null means "no control here, not now" and keeps the
            // ordinary fallback with a retry, while a throw means a control
            // this device HAS could not be built -- a failed session, which
            // the fallback is the wrong answer to, because an exclusive build
            // has that request refused outright and a transactional one is
            // trying not to need it.
            //
            // Returning null here made every genuine failure on Android look
            // like the first case, so the distinction the component draws
            // could never be reached from the port that needs it.
            Log.e(failure[0]);
            throw new RuntimeException(
                    "the location button could not be built", failure[0]);
        }
        PeerComponent peer = PeerComponent.create(created[0]);
        // The peer's preferred size is NOT derived from the native view --
        // AndroidImplementation's own instructions for building a peer say so
        // in as many words -- so without this the component reports 0x0 and a
        // layout that respects preferred sizes, BoxLayout and FlowLayout among
        // them, collapses it to nothing. A system control the user cannot see
        // or tap is the worst way for this feature to fail, because the build
        // and the manifest are both perfectly correct.
        //
        // Only when the view answered with something. A zero measurement means
        // the platform declined to size it, and forcing zero in would be
        // writing down the failure rather than leaving the layout to do
        // whatever it does for an unset size.
        if (peer != null && measured[0] > 0 && measured[1] > 0) {
            peer.setPreferredW(measured[0]);
            peer.setPreferredH(measured[1]);
        }
        return peer;
    }

    /// The manager without the permission prompts, because the button has
    /// already been granted precise location for this session.
    ///
    /// See AndroidImplementation.getLocationManagerWithoutPermissionPrompt for
    /// what the ordinary path would have asked, and why asking it here is the
    /// one thing this control must not do.
    ///
    /// #### Returns
    ///
    /// the platform location manager, or null when there is none
    public LocationManager getGrantedLocationManager() {
        AndroidImplementation impl = AndroidImplementation.getInstance();
        if (impl == null) {
            return null;
        }
        return impl.getLocationManagerWithoutPermissionPrompt();
    }

    /// Forwards the state onto the wrapped view, which is the only thing the
    /// system button reads. Nothing in Codename One does this for a peer.
    ///
    /// #### Parameters
    ///
    /// - `button`: a peer this bridge created
    /// - `enabled`: whether the button should accept taps
    public void setButtonEnabled(PeerComponent button, final boolean enabled) {
        if (button == null) {
            return;
        }
        Object nativePeer = button.getNativePeer();
        // instanceof rather than a cast whose failure we would catch: on iOS a
        // failed CHECKCAST does not throw, and this class is written to the
        // same rule as the rest of the tree even though it is Android-only.
        if (!(nativePeer instanceof View)) {
            return;
        }
        final View view = (View) nativePeer;
        Activity activity = AndroidImplementation.getActivity();
        if (activity == null) {
            return;
        }
        // Posted rather than blocking. Unlike createButton, whose caller needs
        // the peer before it can lay anything out, nothing here waits on the
        // result -- and setEnabled arrives from the EDT, which must not park on
        // the Android UI thread when it does not have to.
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    view.setEnabled(enabled);
                } catch (Throwable gone) {
                    Log.e(gone);
                }
            }
        });
    }

    /// Builds and configures the native control. Android UI thread only.
    private View build(Activity activity, int textType, int backgroundColor,
            int textColor, final SuccessCallback<Boolean> onResult,
            final Runnable onUnavailable) {
        LocationButton button = new LocationButton(activity);
        // Not left to the context walk. The library resolves an activity by
        // unwrapping the view's context, and a peer built from a context that
        // is not an Activity wrapper leaves it with none -- at which point the
        // session it needs an activity to open is never opened and the button
        // renders nothing at all.
        button.setParentActivity(activity);
        // An id, because the library keys its permission-launcher registration
        // on one and a view with NO_ID silently gets no registration. That path
        // is not reachable from here today, since this bridge only builds the
        // control where the system renders it -- but the cost of an id is a
        // single integer and the cost of the assumption changing underneath us
        // is a button that does nothing.
        button.setId(View.generateViewId());
        button.setTextType(toPlatformTextType(textType));
        if (backgroundColor != UNSET_COLOR) {
            button.setBackgroundColor(0xff000000 | backgroundColor);
        }
        if (textColor != UNSET_COLOR) {
            button.setTextColor(0xff000000 | textColor);
            button.setIconTint(0xff000000 | textColor);
        }
        button.setOnPermissionResultListener(new OnPermissionResultListener() {
            public void onPermissionResult(boolean granted) {
                onResult.onSucess(Boolean.valueOf(granted));
            }
        });
        button.setOnErrorListener(new OnErrorListener() {
            public void onError(Throwable error) {
                // The session died. Whatever is left on screen no longer talks
                // to anything, so the component is told to replace it rather
                // than leave the user tapping a picture of a button.
                Log.e(error);
                onUnavailable.run();
            }
        });
        // Consulted only on the locally drawn path, which this bridge does not
        // use today -- see isSupported(). Installed anyway because the library
        // is at 1.0.0-alpha01: if a future version routes a tap here on a
        // device where the system session could not start, the alternative is
        // the IllegalStateException its default handler throws when the host is
        // not an androidx activity, and Codename One's activity never is.
        button.setOnRequestPermissionsListener(
                new OnRequestPermissionsListener() {
                    public void onRequestPermissions() {
                        requestThroughCodenameOne(onResult);
                    }
                });
        return button;
    }

    /// Asks for the location permission the way the rest of the port does.
    ///
    /// On the Codename One EDT, because `checkForPermission` blocks through
    /// `invokeAndBlock` and shows Codename One dialogs; calling it from the
    /// Android UI thread, which is where a click listener runs, deadlocks the
    /// thread it needs to keep pumping.
    private void requestThroughCodenameOne(
            final SuccessCallback<Boolean> onResult) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                boolean granted = false;
                try {
                    granted = AndroidImplementation.checkForPermission(
                            "android.permission.ACCESS_FINE_LOCATION",
                            "This is required to get the location");
                } catch (Throwable err) {
                    Log.e(err);
                }
                onResult.onSucess(Boolean.valueOf(granted));
            }
        });
    }

    /// Maps the portable label constant onto the library's.
    ///
    /// Written out rather than passed through even though the two happen to
    /// agree today: they are separate APIs versioned separately, and a silent
    /// pass-through would turn a renumbering in an alpha library into a button
    /// with the wrong words on it, which nothing would catch.
    private static int toPlatformTextType(int textType) {
        switch (textType) {
            case com.codename1.location.LocationButton.TEXT_NONE:
                return LocationButton.TEXT_TYPE_NONE;
            case com.codename1.location.LocationButton.TEXT_USE_PRECISE_LOCATION:
                return LocationButton.TEXT_TYPE_USE_PRECISE_LOCATION;
            case com.codename1.location.LocationButton.TEXT_SHARE_PRECISE_LOCATION:
                return LocationButton.TEXT_TYPE_SHARE_PRECISE_LOCATION;
            case com.codename1.location.LocationButton.TEXT_NEAR_MY_PRECISE_LOCATION:
                return LocationButton.TEXT_TYPE_NEAR_MY_PRECISE_LOCATION;
            case com.codename1.location.LocationButton
                    .TEXT_NEAR_YOUR_PRECISE_LOCATION:
                return LocationButton.TEXT_TYPE_NEAR_YOUR_PRECISE_LOCATION;
            default:
                return LocationButton.TEXT_TYPE_PRECISE_LOCATION;
        }
    }
}
