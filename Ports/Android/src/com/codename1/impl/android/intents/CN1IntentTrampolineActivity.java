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
package com.codename1.impl.android.intents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.codename1.intents.Exposure;
import com.codename1.intents.IntentDeclaration;
import com.codename1.intents.IntentParameterInfo;
import com.codename1.intents.Intents;


/// Invisible trampoline for a tapped shortcut or an indexed item.
///
/// Registered by the build with `Theme.NoDisplay`, it decodes the `cn1intent://` URI and decides
/// between the two things a tap can mean:
///
/// - `cn1intent://run?id=..&p=..` runs a declared intent. A headless one is handed to
///   `CN1IntentService` so it completes without the app appearing; anything else foregrounds
///   the app first, because it may want to show something.
/// - `cn1intent://open?uid=type:id` is a tap on indexed content, delivered to the application's
///   selection handler after the app is brought forward.
///
/// #### Why an untrusted caller is limited rather than identified
///
/// This activity has to be exported, because the launcher is what starts a shortcut. Exported
/// means *any* installed application can send it an `ACTION_VIEW` intent, so an unrestricted
/// trampoline would let any app on the device invoke any declared capability with parameters of
/// its choosing. Keeping `CN1IntentService` unexported does nothing about that, since this
/// activity would be a proxy straight to it.
///
/// Identifying the caller is not available to us. `getReferrer()` looks like the answer and is
/// not: it returns the caller-supplied `Intent.EXTRA_REFERRER` in preference to anything the
/// platform attributes, so an attacker simply claims to be the launcher. `getCallingPackage()`
/// is null unless the caller used `startActivityForResult`, which a launcher does not.
///
/// So the trust comes from the URI instead of the caller. Shortcuts this application publishes
/// at runtime carry a nonce minted on first use and never leaves the app's private storage;
/// a URI presenting it can do anything. A URI without it -- a build-time static shortcut, or an
/// arbitrary app's fabrication -- is held to exactly what the launcher already offers: an
/// intent that is discoverable, not destructive, and needs no parameters. Its parameters are
/// dropped, so nothing can be injected. That bounds an attacker to what the user could already
/// do by tapping the app's own launcher shortcut.
///
/// Either way this finishes immediately: it exists to route, never to be seen.
public class CN1IntentTrampolineActivity extends Activity {

    private static final String TAG = "CN1Intents";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean foreground = true;
        try {
            Intent intent = getIntent();
            Uri data = intent == null ? null : intent.getData();
            if (data != null) {
                foreground = route(data, CN1IntentNonce.matches(this, data.getQueryParameter("n")));
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not route an intent tap", t);
        }
        if (foreground) {
            launchMainActivity();
        }
        finish();
    }

    /// Returns true when the app should be brought forward. `trusted` is set when the URI
    /// presented this application's own nonce.
    private boolean route(Uri data, boolean trusted) {
        String host = data.getHost();
        if ("open".equals(host)) {
            // Indexed shortcuts are always published at runtime, so they always carry the
            // nonce. An "open" without one is therefore never ours, and honouring it would let
            // any app drive this application's selection handler with an id of its choosing.
            if (!trusted) {
                Log.w(TAG, "Refusing an unauthenticated indexed-item tap");
                return true;
            }
            String uid = data.getQueryParameter("uid");
            if (uid != null) {
                // Queued by the framework until a handler is registered, so a tap that cold
                // started the process still arrives.
                com.codename1.intents.Intents.dispatchSpotlightSelection(uid);
            }
            return true;
        }
        if ("run".equals(host)) {
            String id = data.getQueryParameter("id");
            if (id == null) {
                return true;
            }
            String params = data.getQueryParameter("p");
            // The headless flag travels in the URI rather than being looked up, because at a
            // cold start the declaration table does not exist yet. A caller-supplied flag is
            // only ever believed in the direction that costs nothing: claiming headless can at
            // worst deny an intent its window, while claiming foreground for a headless intent
            // would only ever make the app appear. The declaration overrides it the moment one
            // exists.
            boolean headless = "1".equals(data.getQueryParameter("h"));
            IntentDeclaration decl = Intents.getDeclaration(id);
            if (decl != null) {
                // runsHeadless, not isHeadless: a declared route is foregrounded however the
                // intent was declared. See IntentDeclaration#runsHeadless.
                headless = decl.runsHeadless();
            }
            // Applied to a trusted request as well, and that is the point. A nonce says this
            // shortcut was minted by this app; it says nothing about when. A donation made
            // before an update that marked the intent destructive keeps working, so a shortcut
            // the launcher has held for weeks would run a destructive action on one tap --
            // past the confirmation that flag exists for, and past every door that refuses it
            // today. The declaration above is already reread for exactly this class of drift,
            // for the headless flag; this is the same reread applied to policy.
            if (decl != null && !isStillPermittedOnOneTap(decl)) {
                Log.w(TAG, "Refusing \"" + id + "\": the shortcut predates a declaration change "
                        + "that no longer allows it to run on a single tap");
                return true;
            }
            if (!trusted) {
                if (decl != null || !Intents.getDeclarations().isEmpty()) {
                    // The runtime is up, so the policy can be applied now: held to what the
                    // launcher already offers, and stripped of parameters so a fabricated URI
                    // cannot choose the values a capability acts on.
                    if (!isSafeForUntrustedCallers(this, id)) {
                        Log.w(TAG, "Refusing an unauthenticated request for \"" + id + "\"");
                        return true;
                    }
                } else {
                    // Cold start: the generated dispatcher has not installed itself, so there is
                    // nothing to check the request against yet. Rejecting here would break every
                    // build-time static shortcut, which is exactly the case that has no nonce.
                    // So the request is parked and re-evaluated once the declarations exist.
                    //
                    // Foregrounding is decided by the URI's own hint, which the build wrote.
                    // Returning true unconditionally made the first tap after process death
                    // visibly open the app for an intent that declared headless=true, and by
                    // the time the parked request ran there was no undoing it.
                    AndroidIntentBridge.parkUntrustedRequest(id);
                    if (headless) {
                        // Something has to boot the runtime, and for a headless request that is
                        // the service rather than the main Activity. The parked id is what
                        // actually runs, once registerIntents has judged it.
                        CN1IntentService.wake(this);
                        return false;
                    }
                    return true;
                }
                params = null;
            }
            if (headless) {
                CN1IntentService.run(this, id, params);
                return false;
            }
            // Not headless: the handler may touch a Form, so it must not run until there is one.
            // Parking it and letting onCreate foreground first is the difference between the
            // handler seeing its own screen and seeing whatever the app was showing when it was
            // last backgrounded -- or no window at all.
            AndroidIntentBridge.parkForegroundRequest(id, params);
            return true;
        }
        return true;
    }

    /// Whether the *current* declaration still allows this intent to run from a single tap.
    ///
    /// Narrower than the unauthenticated policy on purpose: a trusted shortcut may legitimately
    /// carry parameters and need not be discoverable, so only the rules that make one tap
    /// unsafe apply. Destructive is one -- there is nothing between the tap and the action --
    /// and so is an exposure that no longer includes the assistant, since the shortcut surface
    /// is what that opts out of.
    static boolean isStillPermittedOnOneTap(IntentDeclaration decl) {
        return !decl.isDestructive() && decl.isExposedTo(Exposure.ASSISTANT);
    }

    /// True when an unauthenticated caller may run this intent: exactly the set the build
    /// already published as launcher shortcuts.
    ///
    /// An intent the application declared as not discoverable, or as destructive, or that needs
    /// a value nobody supplied, is never in that set -- so an arbitrary app cannot reach the
    /// capabilities most worth protecting.
    static boolean isSafeForUntrustedCallers(Context ctx, String id) {
        // A parameterization is a runtime publication, not one of the launcher actions this
        // policy exists to expose -- and it is the more dangerous of the two, because its whole
        // point is that it carries values the user already chose. Stripping params off an
        // untrusted request does nothing about those: dispatch merges the bound values in
        // afterwards, so a fabricated URI naming a guessable dynamic id would run the base
        // handler with the user's own saved arguments and no nonce anywhere.
        //
        // Nothing legitimate is lost. A donated parameterization does not travel under its
        // runtime id at all: AndroidIntentBridge.donate mints the shortcut against the *base*
        // intent with the bound values merged into the URI, precisely so the shortcut outlives
        // the process that registered it. So an untrusted request naming a dynamic id is
        // already something no shortcut this app published could have produced.
        if (Intents.getDynamicIntent(id) != null) {
            return false;
        }
        IntentDeclaration decl = Intents.getDeclaration(id);
        if (decl == null) {
            return false;
        }
        if (!decl.isDiscoverable() || decl.isDestructive()) {
            return false;
        }
        // exposure is a restriction, not a hint. An intent offered only to a language model
        // opted out of platform access entirely, and this Activity is exported -- so without
        // this check any installed app could reach it with a fabricated cn1intent:// URI. The
        // static shortcut builder already filters on exposure; the runtime door has to agree,
        // because it is the one an attacker actually knocks on.
        if (!decl.isExposedTo(Exposure.ASSISTANT)) {
            return false;
        }
        for (IntentParameterInfo p : decl.getParameters()) {
            if (p.isRequired() && (p.getDefaultValue() == null
                    || p.getDefaultValue().length() == 0)) {
                return false;
            }
        }
        // Asked of the launcher rather than inferred, which is the difference between "would
        // have qualified" and "was actually offered". The build publishes at most
        // MAX_STATIC_SHORTCUTS of the intents that pass every test above, so an app declaring
        // more than that had capabilities this predicate called launcher-equivalent while the
        // launcher had never shown them -- and this Activity is exported, so any installed app
        // could reach a fourth or later one with a fabricated cn1intent:// URI carrying no
        // nonce. The manifest set is the authority on what a tap can legitimately mean here,
        // and it is also the only set that arrives without a nonce: everything this framework
        // publishes at runtime is stamped with one.
        return AndroidIntentBridge.hasManifestShortcut(ctx, id);
    }

    private void launchMainActivity() {
        try {
            Intent launch = getPackageManager()
                    .getLaunchIntentForPackage(getApplicationInfo().packageName);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(launch);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not bring the app forward", t);
        }
    }
}
