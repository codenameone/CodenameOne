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
package androidx.lifecycle;

import android.app.Application;
import android.os.Bundle;

import androidx.savedstate.SavedStateRegistryOwner;

/// The default factory of activities and fragments: creates view models
/// whose constructor takes a [SavedStateHandle], the application, both, or
/// nothing.
public final class SavedStateViewModelFactory implements ViewModelProvider.Factory {

    private final Application mApplication;
    private final Bundle mDefaultArgs;

    public SavedStateViewModelFactory() {
        this(null, null, null);
    }

    public SavedStateViewModelFactory(Application application, SavedStateRegistryOwner owner) {
        this(application, owner, null);
    }

    public SavedStateViewModelFactory(Application application, SavedStateRegistryOwner owner, Bundle defaultArgs) {
        // The owner is not kept on purpose: AndroidX writes a handle through
        // the saved-state registry for the case where saved state outlives
        // the view model store, which is process death. This runtime has no
        // process death -- saved state only exists for a relaunch, and
        // ActivityThread.relaunch always carries the store over with it
        // (onRetainNonConfigurationInstance), so the view model and its
        // handle come back as they were and a recreated handle never has a
        // bundle to restore from.
        mApplication = application;
        mDefaultArgs = defaultArgs;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return ViewModelProvider.createGenerated(modelClass, mApplication, true, mDefaultArgs);
    }
}
