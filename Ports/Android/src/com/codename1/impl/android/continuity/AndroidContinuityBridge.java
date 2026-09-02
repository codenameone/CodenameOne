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
package com.codename1.impl.android.continuity;

import android.os.Bundle;

import com.codename1.continuity.Continuity;
import com.codename1.continuity.spi.ContinuityBridge;
import com.codename1.continuity.spi.ContinuityCallback;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.LifecycleListener;
import com.codename1.io.Log;

import java.util.Map;

/// Android's half of the continuity framework, which is smaller than the Apple one because the
/// platform offers less.
///
/// #### What Android has
///
/// Saving and restoring on this device, which is the part that matters most here: Android reclaims
/// a backgrounded process routinely, far more readily than iOS does, so an app without this comes
/// back to its first screen after nothing more than a few minutes in another app. That half is
/// pure `com.codename1.io.Storage` and needs no bridge at all; what this class adds is a flush at
/// the one moment the platform tells the app it is about to be killed.
///
/// #### What Android does not have
///
/// There is no system service that advertises what the user is doing to the other devices they
/// own, and no key/value store the platform syncs between them. Both are reported unsupported
/// rather than emulated: an app told "yes" by a bridge that then dropped the state would be worse
/// off than one told "no", which can fall back to a `com.codename1.continuity.StateRelay` and
/// reach an iPhone as easily as another Android.
///
/// This is the honest shape of the platform difference, and it is why the developer guide's
/// capability table has a column per platform rather than a single "supported" claim.
public class AndroidContinuityBridge implements ContinuityBridge {

    /// Registers the flush hook. Called once, when the port builds the bridge.
    public AndroidContinuityBridge() {
        try {
            AndroidNativeUtil.addLifecycleListener(new FlushOnSave());
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    public void setCallback(ContinuityCallback callback) {
        // Nothing to deliver: neither capability below exists on this platform, so the framework's
        // inbound seam is never reached from here. States still arrive on Android -- through a
        // StateRelay, which the framework drives itself and which needs no port support.
    }

    public boolean isContinuationSupported() {
        return false;
    }

    public void publishContinuation(String activityType, String title,
            Map<String, Object> userInfo) {
    }

    public void clearContinuation() {
    }

    public boolean isSyncedStoreSupported() {
        return false;
    }

    public void syncedStorePut(String key, String value) {
    }

    public String syncedStoreGet(String key) {
        return null;
    }

    public void syncedStoreRemove(String key) {
    }

    public String[] syncedStoreKeys() {
        return new String[0];
    }

    /// Flushes the checkpoint when the platform says the process may be killed.
    ///
    /// `onSaveInstanceState` is the right hook and `onStop` is not. Android calls this one *before*
    /// stopping, while the app is still whole, and it is the last callback guaranteed to run
    /// before a background process is reclaimed. The app's own `stop()` is not: the generated
    /// activity blocks Android's main thread waiting for it, so work added there is paid for on
    /// every ordinary suspend.
    ///
    /// The framework has almost always written the state already -- it checkpoints as the user
    /// navigates rather than at shutdown -- so this exists for the payload edited after the last
    /// navigation, and is a no-op the rest of the time.
    private static final class FlushOnSave implements LifecycleListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        }

        @Override
        public void onResume() {
            // A relay is the only channel Android has, and nothing reads it on its own. Asking
            // here is what makes "picked it up on the iPad, opened the phone" work: the poll is a
            // background request that returns immediately and does nothing at all when no relay is
            // installed.
            try {
                Continuity.pollRelay();
            } catch (Throwable t) {
                Log.e(t);
            }
        }

        @Override
        public void onPause() {
        }

        @Override
        public void onDestroy() {
        }

        @Override
        public void onSaveInstanceState(Bundle b) {
            try {
                Continuity.checkpoint();
            } catch (Throwable t) {
                // Never allowed to escape. This runs on Android's main thread inside a platform
                // callback, and an exception here takes down the activity as it is being saved --
                // turning a missed checkpoint into a crash on every suspend.
                Log.e(t);
            }
        }

        @Override
        public void onLowMemory() {
        }
    }
}
