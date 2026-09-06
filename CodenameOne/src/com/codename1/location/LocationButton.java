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

import com.codename1.io.Log;
import com.codename1.location.spi.LocationButtonBridge;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.List;

/// A button the user taps to share their precise location once.
///
/// This is the *transactional* half of the location API: a "find shops near
/// me" button, an address auto-fill, a one-time "share where I am". It is not
/// a replacement for [LocationManager], which stays the right API for
/// navigation, tracking, geofencing and anything else that follows the device
/// over time.
///
/// The distinction is not cosmetic. Where the platform draws a location button
/// of its own, this component uses it, and a tap grants precise location for
/// that session only -- the app never holds a persistent grant, and the user is
/// never asked again on the next tap. Where the platform has no such control
/// the component is an ordinary Codename One button that asks for location the
/// usual way, so the same code compiles and runs everywhere.
///
/// ```java
/// LocationButton share = new LocationButton(LocationButton.TEXT_SHARE_PRECISE_LOCATION);
/// share.addLocationSharedListener(loc -> {
///     if (loc == null) {
///         status.setText("Location not shared");
///     } else {
///         status.setText(loc.getLatitude() + ", " + loc.getLongitude());
///     }
///     status.getParent().revalidate();
/// });
/// form.add(share);
/// ```
///
/// #### Android and Google Play
///
/// Google Play requires the system-rendered location button for transactional
/// precise-location use in apps targeting Android 17 (API 37) and later; a
/// persistent `ACCESS_FINE_LOCATION` grant is reserved for core functionality
/// and carries a Play Console declaration. Referencing this class is what makes
/// the Android build declare `USE_LOCATION_BUTTON` and add the platform
/// library, so the button is system-rendered on API 37 and later and falls back
/// to the standard permission prompt below it.
///
/// An app whose *only* use of precise location is this button should also set
/// the `android.locationButton.exclusive=true` build hint. That marks
/// `ACCESS_FINE_LOCATION` as reachable through the button alone
/// (`usesPermissionFlags="onlyForLocationButton"`), which is what removes the
/// need for the persistent-location declaration. Do not set it in an app that
/// also tracks, navigates or geofences: those calls would then be refused the
/// grant they need.
///
/// #### Other platforms
///
/// iOS has no system-rendered button, but its own permission dialog offers
/// "Allow Once", which is the same session-scoped grant reached a different
/// way; the fallback button is the correct behaviour there. The simulator and
/// every other port behave the same way.
public class LocationButton extends Container {

    /// No label -- the location icon alone. The icon is always drawn, on the
    /// platform's own control and on the fallback alike, so this is the
    /// icon-only variant rather than an empty button.
    public static final int TEXT_NONE = 0;

    /// Labelled "Precise location". The default.
    public static final int TEXT_PRECISE_LOCATION = 1;

    /// Labelled "Use precise location".
    public static final int TEXT_USE_PRECISE_LOCATION = 2;

    /// Labelled "Share precise location".
    public static final int TEXT_SHARE_PRECISE_LOCATION = 3;

    /// Labelled "Near my precise location".
    public static final int TEXT_NEAR_MY_PRECISE_LOCATION = 4;

    /// Labelled "Near your precise location".
    public static final int TEXT_NEAR_YOUR_PRECISE_LOCATION = 5;

    /// Passed for a colour the caller did not choose, which leaves the
    /// platform's own. Not 0, because black is a colour a caller can
    /// legitimately ask for and would then be indistinguishable from "unset".
    private static final int UNSET_COLOR = -1;

    private final List<LocationSharedListener> listeners =
            new ArrayList<LocationSharedListener>();

    private int textType = TEXT_PRECISE_LOCATION;
    private int backgroundColor = UNSET_COLOR;
    private int textColor = UNSET_COLOR;
    /// How long past its own deadline an in-flight request may run before the
    /// next tap takes the slot from it.
    private static final long STALE_MARGIN = 5000;

    /// A little past the deadline, so the wake-up finds the request stale
    /// rather than racing the clock and rescheduling itself.
    private static final long WAKE_SLACK = 250;

    /// The default wait for a fix, and the floor for [#setTimeout(long)].
    private static final long DEFAULT_TIMEOUT = 30000;

    private long timeout = DEFAULT_TIMEOUT;

    /// The child currently showing, so a configuration change can replace it.
    private Component body;

    /// True while a tap is being served, so a second tap does not start a
    /// second fix. The system button cannot be tapped twice in a session, but
    /// the fallback button can, and two overlapping `getCurrentLocationSync`
    /// calls would share the one listener slot [LocationManager] has: the
    /// second clears the first's listener, and the first never returns.
    ///
    /// STATIC, because that listener slot is shared by every button on the
    /// form and not just by one button's own taps. An instance field guarded a
    /// double tap on the same control and did nothing for two controls: tapping
    /// the second while the first was waiting replaced the first's listener,
    /// and the first sat until its timeout and reported no location -- with a
    /// fix available the whole time.
    ///
    /// No lock around it. Codename One is single threaded: every read and write
    /// here is on the EDT, inside the callSerially below, and adding
    /// synchronization to core would be the wrong answer to a race that cannot
    /// happen.
    private static boolean inFlight;

    /// When the in-flight request started, and the deadline it was given.
    ///
    /// The wait this component performs is not always bounded by the timeout it
    /// asks for. LocationManager.getCurrentLocationSync applies its deadline
    /// only when NO listener is installed; when the application is already
    /// tracking location it calls getCurrentLocation() instead, and the play
    /// services manager can wait there without a deadline for its API client to
    /// connect. So a tap in a tracking app can block for as long as that takes.
    ///
    /// That is the framework's behaviour and not this component's to change --
    /// every caller of getCurrentLocationSync has the same contract. What this
    /// component must not do is let one stuck request hold the shared slot for
    /// good, which would stop every other button on the form answering. Once
    /// the deadline is past, the next tap takes the slot over rather than
    /// queueing behind something that may never finish.
    private static long inFlightSince;
    private static long inFlightDeadline;

    /// Which request currently owns the shared slot.
    ///
    /// Incremented every time a request takes it. A request that went stale and
    /// was superseded still returns eventually, and its completion must not
    /// clear the flag its successor is holding: that would let a third button
    /// into serveGrant beside the running one, which is the exact overlap the
    /// flag exists to prevent. Each request compares this token before it
    /// clears anything.
    private static long inFlightGeneration;

    /// Buttons whose grant arrived while another button was being served.
    ///
    /// Static for the same reason [#inFlight] is, and a list rather than a
    /// single slot because a form may hold more than two. Drained one at a time
    /// as each request finishes; a button already waiting is not added twice.
    ///
    /// No lock. Every touch of this list is on the EDT, inside a callSerially.
    private static final List<Pending> WAITING = new ArrayList<Pending>();

    /// One grant waiting its turn: which button earned it, and from which
    /// system button.
    ///
    /// A record per GRANT rather than a slot per button, because a button can
    /// hold two. A tap on the old peer waiting while a setter replaces the
    /// control and the replacement is tapped; or the same fallback control
    /// tapped twice before the queue moves. Keeping the stamp in a field on
    /// the button and one entry in the list made those collide -- the second
    /// tap overwrote the first's stamp and was refused a slot, so two taps
    /// produced one answer -- and each way of colliding had to be patched
    /// separately. A grant is the thing that waits, so a grant is what the
    /// queue holds.
    private static final class Pending {

        /// The button that earned this grant.
        private final LocationButton button;

        /// Which system button it came from, or [#NO_SESSION].
        private final int generation;

        private Pending(LocationButton button, int generation) {
            this.button = button;
            this.generation = generation;
        }
    }

    /// Set once the platform's button has failed, which stops [#rebuild()] from
    /// asking for another one and makes [#isUnavailable()] answer true.
    private boolean unavailable;

    /// Which system button's session failed, when one has.
    ///
    /// [#unavailable] is component-wide, and a request in flight belongs to one
    /// particular peer. A setter can replace the control while a granted lookup
    /// is still running, and the REPLACEMENT's session can then fail without
    /// anyone having touched it -- at which point suppressing the completion of
    /// the request that is still running answers the user's tap with the
    /// failure's null and throws away the location it actually obtained.
    ///
    /// So the suppression asks whose failure it was. A session that failed
    /// under the request's own stamp still silences it, which is the case that
    /// rule was written for: getCurrentLocationSync parks through
    /// invokeAndBlock, the EDT keeps pumping, and firing twice for one tap is
    /// the listener contract broken where callers notice least.
    private int failedGeneration = NO_SESSION;

    /// Which system button the callbacks now arriving belong to.
    ///
    /// A setter that changes the label or the colours has to REPLACE the
    /// platform control, because it takes those at construction. The old
    /// native view is removed from this container, but nothing unregisters the
    /// listeners it holds -- they are the ones handed to `createButton`, and
    /// they point here, not at the peer. So a session that dies after its
    /// button has been replaced still called [#systemButtonFailed(int)] and
    /// retired the healthy control that took its place, and a permission
    /// result from the retired view was served as though the new one had
    /// produced it.
    ///
    /// Each system button is stamped as it is built, and its callbacks compare
    /// the stamp against this field before acting. Installing anything that is
    /// not a peer -- the fallback button, the placeholder -- advances it too,
    /// because at that point no system button is current and nothing an old one
    /// reports is either.
    ///
    /// No lock, and none needed: both callbacks marshal onto the EDT before
    /// they look at it, and every write is on the EDT as well.
    private int systemButtonGeneration;

    /// Set when a rebuild wanted a system button and could not get one.
    ///
    /// A setter replaces the control, and the platform can decline right then
    /// -- the Android bridge needs the current Activity and answers null
    /// without one. Installing the ordinary fallback at that moment loses the
    /// session-scoped path for good: initComponent's retry only runs on
    /// ATTACH, and replacing a child does not re-attach an already initialised
    /// component, so the fallback stayed after the Activity came back.
    ///
    /// So the existing peer is kept and the rebuild is remembered. The setter
    /// has not taken effect yet, which is visible and recoverable; a button
    /// that quietly asks for persistent location instead is neither.
    private boolean rebuildPending;

    /// The stamp for an answer that belongs to no platform session.
    ///
    /// The fallback button has no session: it is an ordinary Codename One
    /// button whose tap means "go and get a location the usual way". Stamping
    /// it with the live generation made it STALE the moment a system button
    /// arrived -- and initComponent installs one on every attach where the
    /// fallback is showing, so a tap that was queued behind another request and
    /// then upgraded on the way back to a form was answered null, and one still
    /// in its callSerially was dropped without a word. Neither is a thing the
    /// generation check is for: it exists to disown a REPLACED PEER, and the
    /// fallback never had one.
    ///
    /// Never a real generation, which only ever counts up from zero.
    private static final int NO_SESSION = -1;

    /// True when this answer is not from a control that has since been
    /// replaced. An answer with no session behind it is never superseded.
    private boolean stillCurrent(int generation) {
        return generation == NO_SESSION
                || generation == systemButtonGeneration;
    }

    /// Creates a button labelled "Precise location".
    public LocationButton() {
        this(TEXT_PRECISE_LOCATION);
    }

    /// Creates a button with the given label.
    ///
    /// #### Parameters
    ///
    /// - `textType`: one of the `TEXT_` constants
    public LocationButton(int textType) {
        super(new BorderLayout());
        this.textType = textType;
        rebuild();
    }

    /// True when this device draws the button itself, which is what makes the
    /// grant session-scoped.
    ///
    /// Useful for explanatory copy -- there is nothing to branch on otherwise,
    /// since the component works either way.
    ///
    /// #### Returns
    ///
    /// whether a tap goes through the system's own button
    public static boolean isSystemRendered() {
        try {
            LocationButtonBridge bridge =
                    Display.getInstance().getLocationButtonBridge();
            return bridge != null && bridge.isSupported();
        } catch (Throwable unavailable) {
            // Answered before the display is up, or by a port that reaches the
            // platform to decide. "No" is the safe answer either way: it is
            // what the component itself falls back to.
            return false;
        }
    }

    /// The label the button carries.
    ///
    /// #### Returns
    ///
    /// one of the `TEXT_` constants
    public int getTextType() {
        return textType;
    }

    /// Sets the label the button carries.
    ///
    /// #### Parameters
    ///
    /// - `textType`: one of the `TEXT_` constants
    public void setTextType(int textType) {
        if (this.textType == textType) {
            return;
        }
        this.textType = textType;
        rebuild();
    }

    /// Overrides the button's background colour.
    ///
    /// Left alone by default, which is deliberate: a location button the user
    /// recognises is the point of the control, and the system's own colours are
    /// what make it recognisable. Restyle only when the default is unreadable
    /// against the surface it sits on.
    ///
    /// #### Parameters
    ///
    /// - `color`: an RRGGBB colour, or -1 to restore the platform's own
    public void setButtonBackgroundColor(int color) {
        if (backgroundColor == color) {
            return;
        }
        backgroundColor = color;
        rebuild();
    }

    /// The background colour override, or -1 when the platform's own is used.
    public int getButtonBackgroundColor() {
        return backgroundColor;
    }

    /// Overrides the button's text and icon colour. See
    /// [#setButtonBackgroundColor(int)] on why the default is worth keeping.
    ///
    /// #### Parameters
    ///
    /// - `color`: an RRGGBB colour, or -1 to restore the platform's own
    public void setButtonTextColor(int color) {
        if (textColor == color) {
            return;
        }
        textColor = color;
        rebuild();
    }

    /// The text colour override, or -1 when the platform's own is used.
    public int getButtonTextColor() {
        return textColor;
    }

    /// How long to wait for a fix after the grant, in milliseconds.
    ///
    /// #### Returns
    ///
    /// the timeout in milliseconds, always positive
    public long getTimeout() {
        return timeout;
    }

    /// Sets how long to wait for a fix after the grant.
    ///
    /// The default is 30 seconds. A value of zero or less restores that
    /// default rather than meaning "forever".
    ///
    /// There is deliberately no way to wait indefinitely. A first GPS fix
    /// indoors can simply never arrive, and this wait is not this button's
    /// alone: one request is served at a time across every LocationButton on
    /// the form, because [LocationManager] has a single listener slot. An
    /// unbounded wait therefore parks every other button behind it for the life
    /// of the process -- and if the platform session fails while it is parked
    /// there is nothing that can end it, since the wait has no cancel. A
    /// transactional control that answers late is a nuisance; one that silently
    /// stops all its siblings answering is a defect.
    ///
    /// #### Parameters
    ///
    /// - `timeout`: milliseconds; zero or less restores the 30 second default
    public void setTimeout(long timeout) {
        this.timeout = timeout > 0 ? timeout : DEFAULT_TIMEOUT;
    }

    /// Adds a listener for the location this button obtains.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addLocationSharedListener(LocationSharedListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    /// Removes a previously added listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeLocationSharedListener(LocationSharedListener l) {
        listeners.remove(l);
    }

    /// Builds, or rebuilds, the child that does the work.
    ///
    /// The system button is configured at construction -- the platform control
    /// takes its label and colours when it is created -- so a setter has to
    /// replace it rather than adjust it. That is cheap and happens before the
    /// form is shown in every ordinary use.
    private void rebuild() {
        if (unavailable) {
            setBody(createUnavailablePlaceholder());
            return;
        }
        PeerComponent system = createSystemButton();
        if (unavailable) {
            // Set by createSystemButton when a supported control threw while
            // being built. The fallback is not the answer to that -- see the
            // catch there.
            setBody(createUnavailablePlaceholder());
            return;
        }
        if (system == null && body instanceof PeerComponent) {
            // Declined right now, with a working control already on screen.
            // Keeping it beats downgrading to the fallback: see rebuildPending.
            rebuildPending = true;
            return;
        }
        rebuildPending = false;
        setBody(system);
    }

    /// Installs `replacement`, or the fallback button when it is null.
    private void setBody(Component replacement) {
        if (body != null) {
            removeComponent(body);
            body = null;
        }
        body = replacement == null ? createFallbackButton() : replacement;
        if (!(body instanceof PeerComponent)) {
            // Not a system button, so no outstanding callback from one is
            // current any more. Covers the placeholder a failed session leaves,
            // the unavailable rebuild, and the fallback button on a device that
            // has no platform control at all.
            systemButtonGeneration++;
        }
        addComponent(BorderLayout.CENTER, body);
        applyEnabledState();
        if (isInitialized()) {
            revalidate();
        }
    }

    /// Enables or disables this button.
    ///
    /// Overridden because neither thing below it honours the inherited
    /// behaviour on its own. `Container.setEnabled` reaches the children
    /// present WHEN IT IS CALLED, so a replacement installed later came back
    /// enabled; and it reaches them UNCONDITIONALLY, so re-enabling this
    /// component -- which a form does wholesale, to every control it owns --
    /// woke the placeholder that a failed session had deliberately disabled.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: whether this button should accept taps
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        applyEnabledState();
    }

    /// Puts the child into the state this component's own state implies.
    ///
    /// Called after every install and every setEnabled, because those are the
    /// two ways the pair can disagree. Two rules the inherited propagation does
    /// not know:
    ///
    /// - an unavailable placeholder stays disabled whatever the container says,
    ///   because it has no listener and an enabled-looking inert button is
    ///   worse than a disabled one;
    /// - a system button is a NATIVE view, and no part of Codename One forwards
    ///   an enabled flag to one. It has to be told through the bridge or it
    ///   goes on taking taps and asking for location while this component
    ///   reports disabled.
    private void applyEnabledState() {
        if (body == null) {
            return;
        }
        boolean live = isEnabled() && !unavailable;
        body.setEnabled(live);
        if (body instanceof PeerComponent) {
            // Cast OUTSIDE the try, and that is not style. The try catches
            // Throwable, so a cast inside it would read as one whose failure
            // this handles -- and on iOS a failed CHECKCAST does not throw at
            // all, it hands the wrong object on. check-cast-semantics.sh
            // reports exactly that shape, guard or no guard.
            PeerComponent peer = (PeerComponent) body;
            try {
                LocationButtonBridge bridge =
                        Display.getInstance().getLocationButtonBridge();
                if (bridge != null) {
                    bridge.setButtonEnabled(peer, live);
                }
            } catch (Throwable unsupported) {
                // A port that cannot answer leaves the native view alone. The
                // component still reports the right state, and refusing to
                // build over it is worse than a button that stays tappable.
            }
        }
    }

    /// The platform's own button, or null when this device has none.
    private PeerComponent createSystemButton() {
        LocationButtonBridge bridge;
        try {
            // ASKING can fail, and a port that cannot answer has, in effect,
            // no system control -- which is what the fallback is for.
            //
            // isSupported IS the asking, so it belongs inside this try and not
            // after it: a port answers that question by consulting native
            // state, and a throw there escaped into rebuild() and aborted the
            // CONSTRUCTOR. A component that cannot be built at all is worse
            // than one that falls back, which is what this catch was written
            // to say and did not cover.
            bridge = Display.getInstance().getLocationButtonBridge();
            if (bridge == null || !bridge.isSupported()) {
                return null;
            }
        } catch (Throwable cannotAnswer) {
            Log.e(cannotAnswer);
            return null;
        }
        try {
            // Stamped before the control exists, because the callbacks are
            // handed to createButton and there is no later moment at which
            // this peer could be told which generation it belongs to.
            // The stamp this control WOULD carry, taken but not yet in
            // force. It becomes current only if a peer actually comes back,
            // because a rebuild the platform declines keeps the peer already on
            // screen -- and advancing the counter regardless retired THAT
            // peer's callbacks while it was still the visible control. A tap
            // on it, or its session failing, was then discarded as stale, so
            // the button the user can see did nothing at all until some later
            // attach replaced it.
            //
            // Assigned rather than rolled back: nothing is superseded until
            // something supersedes it, and there is no window in which the
            // counter says otherwise. Safe to defer because a null here means
            // no view was built -- a bridge that FAILS to build one throws, and
            // that path is below.
            final int generation = systemButtonGeneration + 1;
            PeerComponent peer = bridge.createButton(
                    textType, backgroundColor, textColor,
                    new SuccessCallback<Boolean>() {
                        @Override
                        public void onSucess(Boolean granted) {
                            permissionResult(generation, granted != null
                                    && granted.booleanValue());
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            systemButtonFailed(generation);
                        }
                    });
            if (peer != null) {
                systemButtonGeneration = generation;
            }
            return peer;
        } catch (Throwable failed) {
            // A port that said it SUPPORTS the control and then threw building
            // it has a failed session, not an absent feature -- so this is
            // systemButtonFailed's case, and the answer is the one that method
            // argues for at length: an exclusive build has the ordinary
            // request refused outright by onlyForLocationButton, so the
            // fallback could never produce a location, and a non-exclusive one
            // would have its transactional flow quietly downgraded to the
            // persistent grant this component exists to avoid asking for.
            //
            // An earlier comment here claimed the opposite -- that a button
            // asking the ordinary way beats no button at all -- which
            // contradicted systemButtonFailed two methods down for the same
            // situation reached a different way.
            //
            // Note what does NOT come here: a bridge that answers false to
            // isSupported, and a createButton that returns null. Those are the
            // platform saying "not here, not now", and they keep the fallback
            // and initComponent's retry. Only a throw is treated as a failure.
            Log.e(failed);
            unavailable = true;
            return null;
        }
    }

    /// The system button failed after it was already on screen.
    ///
    /// Android's system button is drawn by another process, and that session can
    /// fail at any point -- including once the control is laid out. What it
    /// leaves behind draws like a button and responds to nothing, so it cannot
    /// be left there.
    ///
    /// What replaces it is NOT the fallback button, and that is the whole point
    /// of this method existing separately. On a device where the system renders
    /// the button, the ordinary permission prompt is not the right answer to the
    /// session failing: an app built with
    /// `android.locationButton.exclusive=true` has that request refused
    /// outright by `onlyForLocationButton`, so the substitute could never
    /// produce a location; and an app built without it would have a
    /// transactional flow quietly downgraded to the persistent grant this
    /// component exists to avoid asking for. Either way the honest answer is
    /// that no location is available here, so the control becomes a disabled
    /// placeholder and [#isUnavailable()] starts answering true.
    ///
    /// Sticky for the life of the component: retrying a session that has
    /// already failed once produces the same dead control.
    private void systemButtonFailed(final int generation) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!stillCurrent(generation)) {
                    // The session that died belonged to a button a setter has
                    // already replaced. Retiring the component now would kill
                    // a healthy control on behalf of one nobody can see.
                    return;
                }
                if (unavailable) {
                    return;
                }
                unavailable = true;
                failedGeneration = generation;
                // Out of the queue as well. A button whose grant arrived while
                // another was being served is WAITING, and a session that fails
                // before its turn used to leave it there: serveNextWaiting
                // reached it anyway, grantedManager saw a placeholder rather
                // than a peer and took the ORDINARY path -- prompting, which is
                // the one thing the system button exists to avoid -- and the
                // listeners heard a second answer after the null below.
                //
                // LocationButton.this, not this: inside this Runnable the bare
                // form is the RUNNABLE, which a List<LocationButton> never
                // contains, so the line removed nothing and the comment above
                // it described something that was not happening. Not a live
                // bug only because serveNextWaiting skips unavailable entries
                // as it drains -- but a queue that grows entries nobody removes
                // and a backstop carrying the whole guarantee alone are both
                // worth not having.
                // Unless what is queued is a FALLBACK tap. Its answer has
                // no session behind it -- it goes through the ordinary
                // prompting manager, which this failure has not touched -- so
                // removing it here threw away a request that could still have
                // been served, and the user's tap was answered by the null of
                // a session it never used. Same question serveGrant asks of a
                // running request: whose failure was this?
                // THIS session's grants, not every session's. A grant from a
                // peer that was already replaced is stale, not dead: the drain
                // answers it with its own null, and removing it here meant one
                // null for two taps. And a fallback grant is neither -- it
                // never had a session to lose.
                for (int at = WAITING.size() - 1; at >= 0; at--) {
                    Pending waiting = WAITING.get(at);
                    if (waiting.button == LocationButton.this //NOPMD CompareObjectsWithEquals
                            && waiting.generation == generation) {
                        WAITING.remove(at);
                    }
                }
                setBody(createUnavailablePlaceholder());
                // And tell the listeners. A session that failed IS this button
                // finishing without producing a location, which is exactly what
                // the listener contract covers -- and without this an app that
                // taps and then waits for the completion waits forever, because
                // the platform reports the failure here and never through
                // onPermissionResult.
                //
                // Fired whether or not a tap preceded it, because the platform
                // gives no way to tell: the session opens when the control is
                // attached, so it can fail before anyone touches it. "No
                // location was shared" is true either way, and a listener that
                // hears it early is a better failure than one that never hears.
                fire(null);
            }
        });
    }

    /// True when the platform's button was drawn and then failed, so this
    /// component cannot obtain a location at all.
    ///
    /// A failed session leaves nothing usable: see [#systemButtonFailed(int)] on
    /// why substituting an ordinary permission request would be wrong rather
    /// than merely worse. The component shows a disabled placeholder so the
    /// layout does not jump; an app that would rather show its own message can
    /// ask this and replace the component.
    ///
    /// #### Returns
    ///
    /// whether this button has become unusable
    public boolean isUnavailable() {
        return unavailable;
    }

    /// The disabled stand-in shown once the platform's button has failed.
    ///
    /// Same label and UIID as the fallback, so nothing moves, and disabled so
    /// the user is not invited to tap something that cannot work.
    private Button createUnavailablePlaceholder() {
        Button b = new Button(labelFor(textType));
        b.setUIID("Button");
        // No colour override here, unlike the fallback beside it. This button
        // is disabled, so what the user should read off it is "the theme's
        // disabled style", and painting the application's own accent over a
        // control that cannot be tapped says the opposite.
        FontImage.setMaterialIcon(b, FontImage.MATERIAL_MY_LOCATION);
        b.setEnabled(false);
        return b;
    }

    /// The ordinary Codename One button used where the platform has none.
    ///
    /// UIID "Button" rather than a UIID of its own. A theme that does not know
    /// a UIID gives it blank defaults, and a CSS theme replaces the whole
    /// theme -- so a "LocationButton" UIID would have rendered as an unstyled
    /// rectangle in exactly the projects most likely to use this.
    private Button createFallbackButton() {
        Button b = new Button(labelFor(textType));
        b.setUIID("Button");
        // Colours FIRST, then the icon. FontImage.setIcon SNAPSHOTS the style
        // it finds -- it copies the unselected, selected, pressed and disabled
        // styles and renders a glyph from each -- so an icon generated before
        // setFgColor keeps the theme's foreground while the label takes the
        // override, and setButtonTextColor promises it controls both.
        if (backgroundColor != UNSET_COLOR) {
            b.getAllStyles().setBgColor(backgroundColor);
            b.getAllStyles().setBgTransparency(255);
        }
        if (textColor != UNSET_COLOR) {
            b.getAllStyles().setFgColor(textColor);
        }
        // The location glyph, always -- not only for TEXT_NONE. The platform's
        // own control draws an icon the user is meant to recognise and pairs it
        // with the label, so a fallback that showed text alone was already the
        // odd one out; with TEXT_NONE it was worse than odd, because labelFor
        // returns an empty string and the button rendered blank. That is the
        // documented icon-only variant showing nothing at all.
        FontImage.setMaterialIcon(b, FontImage.MATERIAL_MY_LOCATION);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                // True rather than a permission check of its own: the fallback
                // path reaches location through LocationManager, and every port
                // that has one already prompts from there. Asking here as well
                // would show the user two dialogs for one tap.
                // NO_SESSION, not the generation as it stands: this button is
                // not a system button and has nothing that can be superseded.
                // Reading the live counter here made the answer stale as soon
                // as initComponent upgraded the fallback to a real control,
                // which is exactly when the user's tap most needs to survive.
                permissionResult(NO_SESSION, true);
            }
        });
        return b;
    }

    /// The label text for the fallback button, localizable through the theme's
    /// localization bundle under the keys named here.
    private String labelFor(int type) {
        UIManager m = UIManager.getInstance();
        switch (type) {
            case TEXT_NONE:
                return m.localize("LocationButton.none", "");
            case TEXT_USE_PRECISE_LOCATION:
                return m.localize("LocationButton.usePreciseLocation",
                        "Use precise location");
            case TEXT_SHARE_PRECISE_LOCATION:
                return m.localize("LocationButton.sharePreciseLocation",
                        "Share precise location");
            case TEXT_NEAR_MY_PRECISE_LOCATION:
                return m.localize("LocationButton.nearMyPreciseLocation",
                        "Near my precise location");
            case TEXT_NEAR_YOUR_PRECISE_LOCATION:
                return m.localize("LocationButton.nearYourPreciseLocation",
                        "Near your precise location");
            default:
                return m.localize("LocationButton.preciseLocation",
                        "Precise location");
        }
    }

    /// Turns a grant into a fix, then tells the listeners.
    ///
    /// On the EDT throughout. `getCurrentLocationSync` blocks through
    /// `invokeAndBlock`, which needs the EDT to keep running underneath it, and
    /// the listeners are UI code.
    private void permissionResult(final int generation,
            final boolean granted) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!stillCurrent(generation)) {
                    // ANSWERED, not dropped. systemButtonGeneration is this
                    // component's own counter, so a stale generation here does
                    // not mean some other button -- it means THIS component
                    // rebuilt its peer, which setText and the colour setters
                    // do, and which can happen while the permission UI is
                    // open. The user tapped this control and the listeners are
                    // this control's; saying nothing leaves a tap that was
                    // made, and possibly granted, with no answer at all.
                    //
                    // An earlier revision returned silently and argued the
                    // component had "promised nothing" before an answer was
                    // queued. That read the generation as identifying a
                    // different button. It does not, and serveGrant already
                    // states the rule this now follows: a tap asked a question
                    // and deserves to report, even from a request that lost
                    // its slot.
                    //
                    // Null rather than the fix, even where the grant arrived:
                    // the session belonged to a peer that no longer exists, so
                    // this reports an attempt that produced nothing and leaves
                    // the user free to tap the control that is on screen. It
                    // is what the queue answers a superseded entry with, so
                    // the two paths no longer disagree.
                    //
                    // systemButtonFailed keeps its silent return, and that
                    // asymmetry is deliberate: it decides whether to RETIRE
                    // the component, and a dead old peer is no reason to kill
                    // the healthy control that replaced it.
                    fire(null);
                    return;
                }
                if (!granted) {
                    fire(null);
                    return;
                }
                if (inFlight && !inFlightIsStale()) {
                    // QUEUED, not dropped. By the time this runs the platform
                    // has already granted this button its session -- the user
                    // tapped and said yes -- so answering nothing at all is the
                    // one thing that must not happen. Returning here silently
                    // is what an earlier revision did, and it turned "two
                    // buttons corrupt each other's request" into "the second
                    // button never reports anything".
                    // One entry per GRANT, not per button. Two taps that
                    // land here are two grants and get two entries: the drain
                    // answers a superseded one with null and serves a current
                    // one, and neither has to know about the other. Keeping a
                    // stamp on the button and one slot in the list collided
                    // instead -- the second tap overwrote the first's stamp
                    // and was refused a slot, so two taps produced one answer.
                    WAITING.add(new Pending(LocationButton.this, generation));
                    scheduleStaleWake();
                    return;
                }
                serveGrant(generation);
            }
        });
    }

    /// Reads the fix for a grant this button already holds, tells the
    /// listeners, and hands the manager to whoever was waiting for it.
    ///
    /// Serialized across every button through `inFlight`, because
    /// [LocationManager] has ONE listener slot: two overlapping
    /// `getCurrentLocationSync` calls would have the second clear the first's
    /// listener, leaving the first to wait out its timeout with a fix
    /// available the whole time.
    private void serveGrant(int generation) {
        inFlight = true;
        final long slot = ++inFlightGeneration;
        inFlightSince = System.currentTimeMillis();
        // The deadline this request is entitled to, plus room for the platform
        // to answer late. Generous on purpose: taking the slot from a request
        // that was about to succeed is worse than waiting a little longer.
        inFlightDeadline = saturatedSum(timeout, STALE_MARGIN);
        // A wake for whoever is BEHIND this request, because the queue may not
        // be empty and this request is not guaranteed to end.
        //
        // getCurrentLocationSync honours its timeout only while nobody holds
        // LocationManager's single listener slot; when somebody does it falls
        // through to getCurrentLocation(), which takes as long as the platform
        // takes. A stale request that is still parked holds exactly that slot,
        // so the request being started here can outlive its own deadline
        // without bound -- and serveNextWaiting cancelled the only wake on its
        // way in. Everything still queued behind it then waits on this request
        // returning, or on another tap, neither of which has to happen.
        //
        // scheduleStaleWake is a no-op while one is pending, so the ordinary
        // path where this drains synchronously costs nothing.
        if (!WAITING.isEmpty()) {
            scheduleStaleWake();
        }
        Location result = null;
        try {
            LocationManager manager = grantedManager(generation);
            if (manager != null) {
                result = manager.getCurrentLocationSync(timeout);
            }
        } catch (Throwable err) {
            Log.e(err);
        } finally {
            // Only if this request still owns the slot. A stale one that was
            // superseded and then returned used to clear its successor's.
            if (inFlightGeneration == slot) {
                inFlight = false;
            }
        }
        // Only if the session did not fail underneath us. getCurrentLocationSync
        // blocks through invokeAndBlock, so the EDT keeps pumping and
        // systemButtonFailed can run WHILE this request is parked -- it sets
        // unavailable, swaps the peer for a placeholder and fires null. Firing
        // again here would answer one tap twice, which is the listener contract
        // broken in the way callers notice least and trust most: a completion
        // they already handled, arriving a second time with a different value.
        // Fired regardless of ownership: this tap asked a question and got an
        // answer, and a request that lost the slot still deserves to report.
        // Suppressed only when the session that failed is THIS request's. A
        // newer peer failing is not this tap's answer, and the tap did obtain
        // a location.
        if (!unavailable || failedGeneration != generation) {
            fire(result);
        }
        // Driving the queue is the OWNER's job. A superseded request doing it
        // would serve the next button while its successor is still running.
        if (inFlightGeneration == slot) {
            serveNextWaiting();
        }
    }

    /// Starts the next queued button, if any.
    ///
    /// Through callSerially rather than by calling straight down, so a form
    /// full of buttons tapped at once cannot build a stack one frame deep per
    /// button -- and so each request begins from a clean EDT turn, the way the
    /// first one did.
    private static void serveNextWaiting() {
        cancelStaleWake();
        // Drains dead entries as it goes. A button whose session failed while
        // it waited has already been told so; skipping to whoever is behind it
        // matters because one dead entry must not strand the rest of the queue.
        //
        // Except a FALLBACK tap, which is not dead. Its answer never depended
        // on a platform session -- grantedManager sends NO_SESSION down the
        // prompting path -- so a peer that failed after the tap was queued,
        // possibly a peer the upgrade installed and nobody ever touched, has
        // taken nothing away from it. Dropping it here answered the user with
        // the null of a session their tap never used, and that is the same
        // principle already applied to a fallback tap that survives an upgrade
        // rather than a new rule: what has no session cannot be superseded by
        // one, and cannot be killed by one failing either.
        Pending found = null;
        while (found == null && !WAITING.isEmpty()) {
            Pending candidate = WAITING.remove(0);
            // Staleness FIRST. A button that has gone unavailable installed a
            // placeholder, which advanced its stamp, so every grant it still
            // has queued is stale -- and skipping on unavailable before asking
            // dropped them without a word. systemButtonFailed fires one null,
            // which answers the grant of the session that failed; a grant from
            // a session that was already replaced is a different tap and needs
            // its own.
            if (!candidate.button.stillCurrent(candidate.generation)) {
                // The control that earned this grant was replaced while the
                // grant waited. Answering it is the honest outcome: the tap
                // happened and produced no location. Serving it instead would
                // run a lookup against a session nobody granted and hand the
                // result to listeners as the untapped replacement's.
                candidate.button.fire(null);
                continue;
            }
            if (candidate.button.unavailable
                    && candidate.generation != NO_SESSION) {
                continue;
            }
            found = candidate;
        }
        if (found == null) {
            return;
        }
        final Pending next = found;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!next.button.stillCurrent(next.generation)) {
                    // Replaced between being dequeued and running, which the
                    // drain above cannot see because it already let this one
                    // through. Same answer as there.
                    next.button.fire(null);
                    serveNextWaiting();
                    return;
                }
                if (next.button.unavailable
                        && next.generation != NO_SESSION) {
                    // Failed between being dequeued and running.
                    serveNextWaiting();
                    return;
                }
                if (inFlight && !inFlightIsStale()) {
                    // Somebody else got in first; back to the queue rather
                    // than into a second concurrent request. And schedule the
                    // wake again: serveNextWaiting cancelled the pending one on
                    // the way in, so without this the requeued button waits on
                    // a tap that may never come.
                    WAITING.add(next);
                    scheduleStaleWake();
                    return;
                }
                next.button.serveGrant(next.generation);
            }
        });
    }

    /// Retries the system button when this component is attached.
    ///
    /// The platform's answer to "is there a system button" is not fixed for the
    /// life of the process. On Android it needs the current Activity, and a
    /// component constructed during a transition -- or before the first Form is
    /// shown -- gets told no. Without this that no was FINAL: the ordinary
    /// fallback stayed installed for good, and a later tap went through the
    /// prompting path asking for persistent location, or was refused outright
    /// in exclusive mode. Either way the app shipped a button that looked right
    /// and behaved like the thing this feature replaces.
    ///
    /// Only upwards, and only from the fallback. A working system button is
    /// never disturbed, an unavailable one stays unavailable, and a port that
    /// still has no button answers null and leaves the fallback alone -- so on
    /// every platform without one this costs a null check per attach.
    @Override
    protected void initComponent() {
        super.initComponent();
        if (unavailable) {
            return;
        }
        // A peer already here and nothing owed: leave a working control alone.
        if (body instanceof PeerComponent && !rebuildPending) {
            return;
        }
        PeerComponent system = createSystemButton();
        if (unavailable) {
            // A supported control that threw while being built, exactly as in
            // rebuild(). Leaving the fallback in place here left a LIVE button
            // on a component already reporting itself unavailable, and a tap on
            // it walks into the ordinary permission prompt -- refused outright
            // in an exclusive build, and the persistent grant this component
            // exists to avoid in any other.
            setBody(createUnavailablePlaceholder());
            return;
        }
        if (system != null) {
            rebuildPending = false;
            setBody(system);
        }
    }

    /// The one pending wake-up that re-examines a stale request.
    ///
    /// The staleness test is worthless without something to run it. Nothing
    /// re-enters this class while a request is stuck -- serveNextWaiting runs
    /// when a request FINISHES, and the stuck one never does -- so a button
    /// queued before the deadline would have waited for a third tap that may
    /// never come. This wakes the queue at the deadline instead.
    ///
    /// One at a time, cancelled as soon as it is not needed: java.util.Timer's
    /// thread is not a daemon, and leaving one behind keeps a desktop JVM alive
    /// after the app is done -- the same trap CallAction's safety net documents.
    private static java.util.Timer staleWake;

    /// Wakes the queue when the in-flight request passes its deadline.
    private static void scheduleStaleWake() {
        if (staleWake != null) {
            return;
        }
        long now = System.currentTimeMillis();
        long deadlineAt = saturatedSum(inFlightSince, inFlightDeadline);
        if (deadlineAt > Long.MAX_VALUE - WAKE_SLACK - now) {
            // No wake at all, rather than one as far away as we can express.
            //
            // Timer adds the delay to the clock and REFUSES the result if it
            // overflows -- "Illegal execution time" -- so saturating the delay
            // to Long.MAX_VALUE moved the overflow one frame along instead of
            // removing it, and the throw came back out through the callSerially
            // that was queueing a grant.
            //
            // Nothing is lost by not scheduling: this wake exists to re-drive
            // the queue once the request in flight has outstayed its deadline,
            // and a deadline that cannot be reached is one the request will
            // never outstay. Whoever is waiting is served when it finishes,
            // which is the only way it can end.
            return;
        }
        long delay = deadlineAt + WAKE_SLACK - now;
        if (delay < 0) {
            delay = 0;
        }
        final java.util.Timer timer = new java.util.Timer();
        staleWake = timer;
        try {
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    timer.cancel();
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            // Identity is the question: clear the field only
                            // if it still holds THIS timer, so a wake-up that
                            // was superseded cannot erase its replacement.
                            // Timer has no equals and two of them are never
                            // interchangeable.
                            if (staleWake == timer) { //NOPMD CompareObjectsWithEquals
                                staleWake = null;
                            }
                            serveNextWaiting();
                        }
                    });
                }
            }, delay);
        } catch (IllegalStateException cancelledAlready) {
            staleWake = null;
        }
    }

    /// Drops the pending wake-up, because the queue is being served now.
    ///
    /// NonThreadSafeSingleton reads the null check and assignment as an unsafe
    /// singleton. It is neither a singleton nor racy: this is one EDT-owned
    /// field, and every touch of it is inside a callSerially. Locking core to
    /// answer a rule about two threads would be the wrong trade.
    private static void cancelStaleWake() {
        if (staleWake != null) { //NOPMD NonThreadSafeSingleton
            staleWake.cancel();
            staleWake = null;
        }
    }

    /// Whether the in-flight request has outlived the deadline it was given.
    ///
    /// The blocked call is not cancelled -- there is no API for that, and it
    /// may still return later. This releases the QUEUE, so a stuck request
    /// costs its own tap and not every tap after it.
    /// Adds two non-negative values without wrapping past the end of time.
    ///
    /// setTimeout takes any positive long, and Long.MAX_VALUE is how a caller
    /// says "wait as long as it takes". Adding STALE_MARGIN to that wrapped
    /// NEGATIVE, so the deadline was already behind us: every request looked
    /// stale the moment it started, the next grant was free to begin beside
    /// it, and two of them shared LocationManager's single listener slot --
    /// which is the exact collision inFlight exists to prevent, reached by
    /// asking for a longer wait.
    ///
    /// Both callers pass values that cannot be negative -- a timeout, a margin,
    /// an epoch in milliseconds -- so the one-sided test is the whole test.
    private static long saturatedSum(long first, long second) {
        return first > Long.MAX_VALUE - second
                ? Long.MAX_VALUE
                : first + second;
    }

    private static boolean inFlightIsStale() {
        return inFlight
                && System.currentTimeMillis() - inFlightSince
                        > inFlightDeadline;
    }

    /// The manager to read the fix from, which is not always the ordinary one.
    ///
    /// After the SYSTEM button reports a grant, the platform has already handed
    /// this session precise location, and the ordinary acquisition path would
    /// ask again -- for ACCESS_FINE_LOCATION, and on an app that also geofences
    /// for ACCESS_BACKGROUND_LOCATION, which from Android 30 is a settings
    /// screen. Being sent there by a control that promised a one-time share is
    /// the experience this whole feature exists to remove, so the bridge hands
    /// back a manager that asks for nothing.
    ///
    /// The FALLBACK button is the opposite case and deliberately keeps the
    /// ordinary path: nothing has granted anything, and the prompt it raises is
    /// the one the user has to see.
    ///
    /// #### Returns
    ///
    /// the manager to read a fix from, or null when there is none
    private LocationManager grantedManager(int generation) {
        // The REQUEST's origin, not the body's shape at the moment it is
        // served. Those disagree exactly where it matters: a fallback tap can
        // be queued behind another request, and initComponent can upgrade the
        // component to a system button while it waits. Reading `body` then
        // sent a tap that never had a platform grant down the granted path,
        // which answers null for want of a session nobody opened -- and the
        // ordinary prompt it was entitled to never happened.
        //
        // NO_SESSION is what the fallback stamps its answers with, so the
        // stamp already carries this and nothing new has to be threaded
        // through the queue.
        if (generation != NO_SESSION) {
            // No fall-through from here. The bridge's contract says a null
            // manager means the platform has none and the caller treats that
            // as "no location" rather than as a reason to prompt -- and the
            // ordinary path prompts: for fine location, and on an app that
            // geofences for background location, which from Android 30 is a
            // settings screen. Reaching that after a transactional grant is
            // the exact outcome this component exists to avoid, so a bridge
            // that cannot answer costs this tap its fix and nothing more.
            try {
                LocationButtonBridge bridge =
                        Display.getInstance().getLocationButtonBridge();
                if (bridge != null) {
                    return bridge.getGrantedLocationManager();
                }
            } catch (Throwable unsupported) {
                Log.e(unsupported);
            }
            return null;
        }
        return Display.getInstance().getLocationManager();
    }

    private void fire(Location location) {
        // Over a copy, because a listener that removes itself is the ordinary
        // way to make a one-shot flow one-shot, and mutating the list under
        // the iteration would skip the listener after it.
        LocationSharedListener[] snapshot = listeners.toArray(
                new LocationSharedListener[listeners.size()]);
        for (LocationSharedListener listener : snapshot) {
            try {
                listener.locationShared(location);
            } catch (Throwable err) {
                // One listener's failure is not the others' business, and it is
                // certainly not this component's.
                Log.e(err);
            }
        }
    }
}
