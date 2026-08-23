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
package com.codename1.impl.android.surfaces;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.math.BigInteger;
import java.security.SecureRandom;

/// Invisible trampoline receiving surface taps (widget nodes, live activity notifications).
/// Registered by the build with `Theme.NoDisplay`, it decodes the action extras, queues the
/// action with `AndroidSurfaceBridge` (which forwards to
/// `com.codename1.surfaces.Surfaces.dispatchAction` -- the framework queues internally until the
/// app registers a handler, so taps survive a cold start), brings the main activity forward the
/// same way `CodenameOneShareReceiverActivity` does, and finishes immediately.
public class CN1SurfaceActionActivity extends Activity {
    /// Intent extra carrying the widget kind id or live activity type.
    public static final String EXTRA_SOURCE = "CN1SurfaceSource";
    /// Intent extra carrying the action id of the tapped node.
    public static final String EXTRA_ACTION_ID = "CN1SurfaceActionId";
    /// Intent extra carrying the action parameters as a JSON object string.
    public static final String EXTRA_ACTION_PARAMS = "CN1SurfaceActionParams";
    /// Intent extra proving the tap came from a surface this app rendered. See [#token].
    public static final String EXTRA_TOKEN = "CN1SurfaceActionToken";
    private static final String TAG = "CN1Surfaces";
    private static final String TOKEN_PREFS = "cn1_surface_action";
    private static final String TOKEN_KEY = "token";

    /// A per-install secret shared between the code that renders a surface and this trampoline.
    ///
    /// A Tile's tap is not a `PendingIntent`. ProtoLayout's `LaunchAction` names a component and
    /// the TILE HOST starts it, from its own process, so the trampoline has to be exported for a
    /// Tile tap to arrive at all -- and an exported activity can be started by any app on the
    /// watch, with extras of its choosing. Without this, another app could name any action id it
    /// liked and this class would forward it to `Surfaces.dispatchAction` as though the user had
    /// tapped it.
    ///
    /// The value never leaves the device: it is generated on first use, kept in the app's own
    /// private preferences, and travels only through the layout the app hands the tile host,
    /// which no other app can read. A caller that cannot produce it did not get here from a
    /// surface this app drew.
    ///
    /// - `ctx`: any context
    ///
    /// Returns the token, generating it on first use, or null when it could not be stored.
    ///
    /// A token that was not persisted is worse than none. The tap it authenticates is handled
    /// later -- often by another process -- which reads the preference, finds nothing, generates
    /// a different value and rejects the very action this app drew. Two nodes rendered in one
    /// pass could even carry different unusable tokens. So a failed commit returns null and the
    /// caller leaves the action off: the surface still renders and the tap does nothing, which is
    /// the honest outcome when the device cannot keep a secret for us.
    public static synchronized String token(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(TOKEN_PREFS, Context.MODE_PRIVATE);
        String existing = prefs.getString(TOKEN_KEY, null);
        if (existing != null && existing.length() > 0) {
            return existing;
        }
        String fresh = new BigInteger(130, new SecureRandom()).toString(32);
        // commit() and not apply(), because the answer is the point: apply() is asynchronous and
        // reports nothing, so there would be no moment at which this could know.
        if (!prefs.edit().putString(TOKEN_KEY, fresh).commit()) {
            Log.w(TAG, "Could not persist the surface action token; actions on this surface are "
                    + "left unauthenticated and will not dispatch. The device is most likely out "
                    + "of storage.");
            return null;
        }
        return fresh;
    }

    /// Attaches the token to an action intent. Every producer of these extras calls this, so the
    /// check below can be unconditional wherever it applies.
    ///
    /// - `ctx`: any context
    /// - `intent`: the action intent being built
    static void authenticate(Context ctx, Intent intent) {
        String token = token(ctx);
        if (token != null) {
            intent.putExtra(EXTRA_TOKEN, token);
        }
        // Absent when the token could not be stored. An intent without it is rejected by
        // trusted() exactly as an untrusted caller's would be, which is the intended outcome:
        // better a tap that does nothing than one that dispatches without the check.
    }

    /// Whether this activity is reachable from outside the app, which is true exactly when a
    /// Tile was generated. Read from the merged manifest rather than assumed, so the check
    /// follows what was actually declared.
    private boolean isExported() {
        try {
            return getPackageManager().getActivityInfo(getComponentName(), 0).exported;
        } catch (Throwable t) {
            // The manifest says what it says; a failed lookup is not a reason to start trusting
            // callers. Non-exported is the historical shape and the safe answer for the phone.
            Log.w(TAG, "Could not read this activity's export state; treating taps as trusted", t);
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = getIntent();
            if (intent != null && !trusted(intent)) {
                // Nothing at all, not merely no dispatch. Bringing the app forward is itself the
                // interesting half of what this activity does: an app that cannot forge an action
                // could still start the trampoline in a loop and foreground this application over
                // and over, which is a nuisance the user would blame on us. Checked before the
                // action is read, so an intent carrying no action id is treated the same way.
                finish();
                return;
            }
            if (intent != null) {
                String actionId = intent.getStringExtra(EXTRA_ACTION_ID);
                if (actionId != null) {
                    AndroidSurfaceBridge.postAction(intent.getStringExtra(EXTRA_SOURCE),
                            actionId, intent.getStringExtra(EXTRA_ACTION_PARAMS));
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to decode a surface action", t);
        }
        launchMainActivity();
        finish();
    }

    /// Whether this tap may be dispatched.
    ///
    /// Only asked where it can matter. While the trampoline is private -- every build without a
    /// Tile -- nothing outside the app can start it, and an intent that arrives is one this app
    /// built; requiring a token there would break a `PendingIntent` a widget handed the launcher
    /// before the app was updated, for no gain.
    private boolean trusted(Intent intent) {
        if (!isExported()) {
            return true;
        }
        String presented = intent.getStringExtra(EXTRA_TOKEN);
        if (presented != null && presented.equals(token(this))) {
            return true;
        }
        // Loud, because the honest cases are an app update that rotated nothing and a genuinely
        // hostile caller, and the two look identical from here.
        Log.w(TAG, "Refusing a surface action that did not come from a surface this app drew");
        return false;
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
            Log.w(TAG, "Failed to launch the main activity", t);
        }
    }
}
