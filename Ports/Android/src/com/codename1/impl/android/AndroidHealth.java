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
package com.codename1.impl.android;

import android.content.Intent;
import android.net.Uri;

import com.codename1.health.Health;
import com.codename1.health.HealthAvailability;
import com.codename1.health.HealthStore;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.impl.health.EdtResult;

/// The Android health entry point, backed by Health Connect through the
/// injected [HealthConnectDelegate].
///
/// Reports the provider's real state, which matters more on Android than
/// elsewhere: Health Connect ships as a separate app on Android 13 and
/// below and can be missing or out of date, and both are recoverable by
/// the user through [#openProviderSetup()].
class AndroidHealth extends Health {

    private final AndroidHealthStore store = new AndroidHealthStore();

    public boolean isSupported() {
        return AndroidHealthSupport.getDelegate() != null;
    }

    public HealthAvailability getAvailability() {
        HealthConnectDelegate d = AndroidHealthSupport.getDelegate();
        if (d == null) {
            return HealthAvailability.NOT_SUPPORTED;
        }
        switch (d.sdkStatus()) {
            case HealthConnectDelegate.SDK_AVAILABLE:
                return HealthAvailability.AVAILABLE;
            case HealthConnectDelegate.SDK_UPDATE_REQUIRED:
                return HealthAvailability.PROVIDER_UPDATE_REQUIRED;
            default:
                return HealthAvailability.PROVIDER_NOT_INSTALLED;
        }
    }

    public HealthStore getStore() {
        return store;
    }

    /// Opens Health Connect's own permission screen for this app.
    public AsyncResource<Boolean> openHealthSettings() {
        AsyncResource<Boolean> out = new EdtResult<Boolean>();
        try {
            Intent i = new Intent(
                    "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS");
            AndroidNativeUtil.getContext().startActivity(
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            out.complete(Boolean.TRUE);
        } catch (Throwable t) {
            out.complete(Boolean.FALSE);
        }
        return out;
    }

    /// Sends the user to install or update the Health Connect provider.
    ///
    /// The `healthconnect://onboarding` referrer is what makes the Play
    /// listing land on the provider's setup flow rather than a bare app
    /// page.
    public AsyncResource<Boolean> openProviderSetup() {
        AsyncResource<Boolean> out = new EdtResult<Boolean>();
        HealthConnectDelegate d = AndroidHealthSupport.getDelegate();
        String pkg = d == null ? "com.google.android.apps.healthdata"
                : d.providerPackageName();
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "market://details?id=" + pkg
                    + "&url=healthconnect%3A%2F%2Fonboarding"));
            i.setPackage("com.android.vending");
            i.putExtra("overlay", true);
            i.putExtra("callerId",
                    AndroidNativeUtil.getContext().getPackageName());
            AndroidNativeUtil.getContext().startActivity(
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            out.complete(Boolean.TRUE);
        } catch (Throwable t) {
            out.complete(Boolean.FALSE);
        }
        return out;
    }

    public java.util.List<String> getConfigurationProblems() {
        java.util.List<String> problems = new java.util.ArrayList<String>();
        if (AndroidHealthSupport.getDelegate() == null) {
            problems.add("No Health Connect bridge is registered. The build "
                    + "server injects one when the app references "
                    + "com.codename1.health; if you are seeing this on a "
                    + "device build, check that android.health.read or "
                    + "android.health.write is declared.");
        }
        return problems;
    }

    /// Marshals a bridge callback onto the EDT, since the bridge completes
    /// on a coroutine dispatcher.
    static void onEdt(Runnable r) {
        Display.getInstance().callSerially(r);
    }
}
