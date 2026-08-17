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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

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
                foreground = route(data);
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
            com.codename1.intents.IntentDeclaration decl =
                    com.codename1.intents.Intents.getDeclaration(id);
            if (decl != null && decl.isHeadless()) {
                CN1IntentService.run(this, id, params);
                return false;
            }
            CN1IntentService.runInProcess(id, params);
            return true;
        }
        return true;
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
