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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

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
    private static final String TAG = "CN1Surfaces";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = getIntent();
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
