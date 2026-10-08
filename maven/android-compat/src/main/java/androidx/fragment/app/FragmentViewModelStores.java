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
package androidx.fragment.app;

import androidx.activity.ComponentActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The view model stores of an activity's fragments, kept as a view model of
/// the activity so they survive a configuration change with it, and cleared
/// with it when the activity finishes.
final class FragmentViewModelStores extends ViewModel {

    private static final String KEY = "androidx.fragment.app.FragmentViewModelStores";

    private final Map<String, ViewModelStore> mStores = new HashMap<String, ViewModelStore>();

    static FragmentViewModelStores of(ComponentActivity activity) {
        return new ViewModelProvider(activity.getViewModelStore(), new ViewModelProvider.Factory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(Class<T> modelClass) {
                return (T) new FragmentViewModelStores();
            }
        }).get(KEY, FragmentViewModelStores.class);
    }

    ViewModelStore storeFor(String key) {
        ViewModelStore s = mStores.get(key);
        if (s == null) {
            s = new ViewModelStore();
            mStores.put(key, s);
        }
        return s;
    }

    void clear(String key) {
        ViewModelStore s = mStores.remove(key);
        if (s != null) {
            s.clear();
        }
    }

    @Override
    protected void onCleared() {
        List<ViewModelStore> all = new ArrayList<ViewModelStore>(mStores.values());
        mStores.clear();
        for (ViewModelStore s : all) {
            s.clear();
        }
    }
}
