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
package androidx.savedstate;

import android.os.Bundle;

import java.util.LinkedHashMap;
import java.util.Map;

/// Components' contributions to their owner's saved instance state: each
/// registers a provider under a key, and reads back what it saved when the
/// owner is recreated.
public final class SavedStateRegistry {

    /// Produces a component's state when its owner saves.
    public interface SavedStateProvider {
        Bundle saveState();
    }

    /// Restores state when the owner restores; see
    /// [#runOnNextRecreation(Class)].
    public interface AutoRecreated {
        void onRecreated(SavedStateRegistryOwner owner);
    }

    private static final String SAVED_COMPONENTS_KEY = "androidx.lifecycle.BundlableSavedStateRegistry.key";

    private final Map<String, SavedStateProvider> mComponents = new LinkedHashMap<String, SavedStateProvider>();
    private Bundle mRestoredState;
    private boolean mRestored;

    /// Runtime use: the saved state the owner was created with.
    public void performRestore(Bundle savedState) {
        if (mRestored) {
            throw new IllegalStateException("SavedStateRegistry was already restored.");
        }
        mRestoredState = savedState == null ? null : savedState.getBundle(SAVED_COMPONENTS_KEY);
        mRestored = true;
    }

    /// Runtime use: every provider's state, into the owner's saved state.
    public void performSave(Bundle outBundle) {
        Bundle components = new Bundle();
        if (mRestoredState != null) {
            components.putAll(mRestoredState);
        }
        for (Map.Entry<String, SavedStateProvider> e : mComponents.entrySet()) {
            components.putBundle(e.getKey(), e.getValue().saveState());
        }
        if (!components.isEmpty()) {
            outBundle.putBundle(SAVED_COMPONENTS_KEY, components);
        }
    }

    public boolean isRestored() {
        return mRestored;
    }

    /// The state saved under `key`, once; null when there is none.
    public Bundle consumeRestoredStateForKey(String key) {
        if (!mRestored) {
            throw new IllegalStateException("You can consumeRestoredStateForKey only after super.onCreate of "
                    + "corresponding component");
        }
        if (mRestoredState == null) {
            return null;
        }
        Bundle result = mRestoredState.getBundle(key);
        mRestoredState.remove(key);
        if (mRestoredState.isEmpty()) {
            mRestoredState = null;
        }
        return result;
    }

    public void registerSavedStateProvider(String key, SavedStateProvider provider) {
        if (mComponents.containsKey(key)) {
            throw new IllegalArgumentException("SavedStateProvider with the given key is already registered");
        }
        mComponents.put(key, provider);
    }

    public SavedStateProvider getSavedStateProvider(String key) {
        return mComponents.get(key);
    }

    public void unregisterSavedStateProvider(String key) {
        mComponents.remove(key);
    }
}
