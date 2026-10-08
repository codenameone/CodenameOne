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
package com.codenameone.examples.hellocodenameone.tests.androidcompat;

import android.app.Activity;
import com.example.droid.FragmentsActivity;

/// A <fragment> inflated from the layout and a detail fragment replaced in through a transaction.
public class AndroidCompatFragmentsScreenshotTest extends AndroidCompatScreenshotTest {
    public AndroidCompatFragmentsScreenshotTest() {
        super(FragmentsActivity.class, "AndroidCompatFragments");
    }

    @Override
    protected void prepare(Activity activity) {
        // Pick the second title, as a tap would: the detail pane fills in.
        android.widget.ListView list = findListView(activity.getWindow().getDecorView());
        if (list != null && list.getAdapter() != null && list.getAdapter().getCount() > 1) {
            list.performItemClick(null, 1, list.getAdapter().getItemId(1));
        }
    }
}
