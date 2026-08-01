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
package com.codename1.health;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/// Shows the app's privacy policy when the user asks why it wants health
/// data.
///
/// #### Not optional
///
/// Health Connect will not present its consent dialog at all to an app
/// that does not declare an activity handling
/// `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`. An app missing this
/// does not get a rejected permission request -- it gets a permission
/// request that silently never appears, which is considerably harder to
/// diagnose. Codename One declares it automatically whenever the build
/// detects health usage.
///
/// #### Why it lives in `com.codename1.health` rather than `impl.android`
///
/// It is referenced by name from the manifest, so it needs a stable,
/// declared class name -- the same reason
/// `com.codename1.location.CodenameOneBackgroundLocationActivity` sits
/// outside the implementation package.
///
/// The policy URL comes from the `android.health.privacyPolicyUrl` build
/// hint, which the Android builder requires.
public class HealthPermissionsRationaleActivity extends Activity {

    /// The generated string resource holding the configured policy URL.
    private static final String URL_RESOURCE = "cn1_health_privacy_policy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = resolvePolicyUrl();
        if (url != null && url.length() > 0) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Throwable t) {
                // No browser, or a malformed URL. Telling the user
                // something is better than a blank screen -- they arrived
                // here by explicitly asking why we want their health data.
                Toast.makeText(this, url, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this,
                    "This app reads health data to provide its features.",
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }

    /// Reads the configured policy URL from generated resources, or null.
    private String resolvePolicyUrl() {
        try {
            int id = getResources().getIdentifier(URL_RESOURCE, "string",
                    getPackageName());
            if (id != 0) {
                return getString(id);
            }
        } catch (Throwable ignored) {
            // Fall through to the generic message below.
        }
        return null;
    }
}
