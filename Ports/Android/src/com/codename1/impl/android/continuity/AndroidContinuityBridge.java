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
import com.codename1.ui.Display;

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

    /// How long the suspend flush may hold Android's main thread waiting for the event thread.
    ///
    /// Bounded because the alternative is an ANR: if the event thread is wedged, waiting forever
    /// turns a missed checkpoint into a killed application. The state written by the last
    /// navigation is still on disk when this gives up.
    private static final int CHECKPOINT_TIMEOUT_MILLIS = 1500;

    /// Registers the flush hook. Called once, when the port builds the bridge.
    public AndroidContinuityBridge() {
        try {
            AndroidNativeUtil.addLifecycleListener(new FlushOnSave());
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    @Override
    public void setCallback(ContinuityCallback callback) {
        // Nothing to deliver: neither capability below exists on this platform, so the framework's
        // inbound seam is never reached from here. States still arrive on Android -- through a
        // StateRelay, which the framework drives itself and which needs no port support.
    }

    @Override
    public boolean isContinuationSupported() {
        return false;
    }

    @Override
    public void publishContinuation(String activityType, String title,
            Map<String, Object> userInfo) {
    }

    @Override
    public void clearContinuation() {
    }

    @Override
    public boolean isSyncedStoreSupported() {
        return false;
    }

    @Override
    public boolean syncedStorePut(String key, String value) {
        return false;
    }

    @Override
    public String syncedStoreGet(String key) {
        return null;
    }

    @Override
    public void syncedStoreRemove(String key) {
    }

    @Override
    public String[] syncedStoreKeys() {
        return new String[0];
    }

    /// The checkpoint, as a constant rather than an anonymous class per callback.
    ///
    /// It captures nothing -- everything it touches is static -- so an inner class would hold the
    /// listener alive for no reason and allocate on a path that runs at every suspend.
    private static final Runnable CHECKPOINT = new Runnable() {
        @Override
        public void run() {
            try {
                if (!Continuity.isCheckpointPending()) {
                    // Asked HERE rather than before the hop. The framework writes through as the
                    // user navigates, so by the time Android says it may kill the process there
                    // is usually nothing owed -- but the answer lives in EDT-owned fields, and
                    // reading it from Android's main thread was the one place this port reached
                    // into the framework's state from off the event thread.
                    return;
                }
                Continuity.checkpoint();
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    };

    /// The resume poll, as a constant for the reason CHECKPOINT is one.
    private static final Runnable POLL = new Runnable() {
        @Override
        public void run() {
            try {
                Continuity.pollRelay();
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    };

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
            //
            // Marshalled, and NOT waited for. This runs on Android's main thread and pollRelay()
            // reads EDT-owned fields to decide whether a fetch is already out; blocking here for
            // a request that returns immediately anyway would only slow every resume.
            try {
                Display.getInstance().callSerially(POLL);
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
                // Onto the Codename One event thread, and waited for. This callback runs on
                // Android's own main thread, which is not the EDT: StateProvider.saveState is
                // application code documented to run on the EDT, and the route stack it is
                // captured beside is an EDT-owned list. Reading either from here would race the
                // running application, so the whole decision -- including whether anything is
                // owed at all -- is made on the other side of the hop.
                //
                // Waiting blocks Android's main thread. That is the cost of a guaranteed flush at
                // the last callback before the process can be reclaimed, and it is bounded.
                Display.getInstance().callSeriallyAndWait(CHECKPOINT, CHECKPOINT_TIMEOUT_MILLIS);
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
