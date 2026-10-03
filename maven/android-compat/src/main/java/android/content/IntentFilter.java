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
package android.content;

import java.util.ArrayList;

/// The actions and categories a receiver listens for.
public class IntentFilter {

    private final ArrayList<String> actions = new ArrayList<String>();
    private final ArrayList<String> categories = new ArrayList<String>();

    public IntentFilter() {
    }

    public IntentFilter(String action) {
        addAction(action);
    }

    public final void addAction(String action) {
        if (!actions.contains(action)) {
            actions.add(action);
        }
    }

    public final void addCategory(String category) {
        categories.add(category);
    }

    public final void addDataScheme(String scheme) {
    }

    public final void addDataType(String type) {
    }

    public final void setPriority(int priority) {
    }

    public final int countActions() {
        return actions.size();
    }

    public final String getAction(int index) {
        return actions.get(index);
    }

    public final boolean hasAction(String action) {
        return actions.contains(action);
    }

    public final boolean matchAction(String action) {
        return action != null && actions.contains(action);
    }
}
