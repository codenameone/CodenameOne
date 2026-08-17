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
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
/// #### Why the caller is checked
///
/// This activity has to be exported, because the launcher is what starts a shortcut. Exported
/// means *any* installed application can send it an `ACTION_VIEW` intent, and an unrestricted
/// trampoline would therefore let any app on the device invoke any declared capability with
/// parameters of its choosing -- including one marked destructive or deliberately not
/// discoverable. Keeping `CN1IntentService` unexported does nothing about that, since this
/// activity would be a proxy straight to it.
///
/// So a caller that is neither this application nor a home-screen launcher gets nothing. A
/// launcher is the only external party with a legitimate reason to start a shortcut.
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
                if (isTrustedCaller()) {
                    foreground = route(data);
                } else {
                    // Deliberately silent to the caller: an app probing for which capabilities
                    // exist should not learn anything from the difference between a refused
                    // known id and a refused unknown one.
                    Log.w(TAG, "Refusing an intent tap from an untrusted caller");
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not route an intent tap", t);
        }
        if (foreground) {
            launchMainActivity();
        }
        finish();
    }

    /// Returns true when the app should be brought forward.
    private boolean route(Uri data) {
        String host = data.getHost();
        if ("open".equals(host)) {
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
            // The headless flag travels in the URI rather than being looked up. At a cold start
            // -- a tapped shortcut on a dead process, which is the common case -- the generated
            // dispatcher has not installed itself yet, so the declaration table is empty and a
            // lookup would report every intent as non-headless and visibly open the app.
            boolean headless = "1".equals(data.getQueryParameter("h"));
            if (!headless) {
                com.codename1.intents.IntentDeclaration decl =
                        com.codename1.intents.Intents.getDeclaration(id);
                headless = decl != null && decl.isHeadless();
            }
            if (headless) {
                CN1IntentService.run(this, id, params);
                return false;
            }
            CN1IntentService.runInProcess(id, params);
            return true;
        }
        return true;
    }

    /// True when the caller is this application or a home-screen launcher.
    ///
    /// `getReferrer()` reports the package that started this activity. A null referrer is
    /// treated as untrusted: it is what an explicit intent from an arbitrary app looks like on
    /// the versions where the platform cannot attribute the caller.
    private boolean isTrustedCaller() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            // No way to attribute the caller at all, so the only safe answer is to accept
            // nothing from outside; a shortcut on these versions still works because the
            // launcher path below is unavailable to attackers too.
            return false;
        }
        Uri referrer = getReferrer();
        String caller = referrer == null ? null : referrer.getHost();
        if (caller == null) {
            return false;
        }
        if (caller.equals(getApplicationInfo().packageName)) {
            return true;
        }
        return launcherPackages().contains(caller);
    }

    /// The packages that can act as a home screen. Resolved rather than hard-coded, since the
    /// user's launcher is their choice.
    private List<String> launcherPackages() {
        List<String> out = new ArrayList<String>();
        // Only the query is guarded; the loop's implicit cast stays outside the catch, which
        // the repo's cast-semantics gate requires wherever the shape appears.
        List<ResolveInfo> found;
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            found = getPackageManager().queryIntentActivities(home, 0);
        } catch (Throwable t) {
            Log.w(TAG, "Could not resolve the launcher packages", t);
            return out;
        }
        if (found == null) {
            return out;
        }
        for (ResolveInfo info : found) {
            if (info.activityInfo != null && info.activityInfo.packageName != null) {
                out.add(info.activityInfo.packageName);
            }
        }
        return out;
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
