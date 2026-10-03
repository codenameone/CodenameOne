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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The view models of one owner, by key.
public class ViewModelStore {

    private final Map<String, ViewModel> mMap = new LinkedHashMap<String, ViewModel>();

    public final void put(String key, ViewModel viewModel) {
        ViewModel old = mMap.put(key, viewModel);
        if (old != null && old != viewModel) {
            old.clear();
        }
    }

    public final ViewModel get(String key) {
        return mMap.get(key);
    }

    public Set<String> keys() {
        return new HashMap<String, ViewModel>(mMap).keySet();
    }

    /// Clears every view model, as the owner is gone for good.
    public final void clear() {
        List<ViewModel> all = new ArrayList<ViewModel>(mMap.values());
        mMap.clear();
        for (ViewModel vm : all) {
            vm.clear();
        }
    }
}
