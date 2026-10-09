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
package com.google.android.material.bottomnavigation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MenuItem;

import com.google.android.material.R;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.navigation.NavigationBarView;

/// The Material bottom navigation bar: three to five top-level destinations
/// along the bottom of the screen.
public class BottomNavigationView extends NavigationBarView {

    /// The listener of the older API; see `setOnItemSelectedListener`.
    public interface OnNavigationItemSelectedListener {
        boolean onNavigationItemSelected(MenuItem item);
    }

    /// The listener of the older API; see `setOnItemReselectedListener`.
    public interface OnNavigationItemReselectedListener {
        void onNavigationItemReselected(MenuItem item);
    }

    public BottomNavigationView(Context context) {
        this(context, null);
    }

    public BottomNavigationView(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.bottomNavigationStyle));
    }

    public BottomNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, MaterialAttrs.isMaterial3(context)
                ? R.style.Widget_Material3_BottomNavigationView : R.style.Widget_MaterialComponents_BottomNavigationView);
    }

    public void setOnNavigationItemSelectedListener(final OnNavigationItemSelectedListener listener) {
        setOnItemSelectedListener(listener == null ? null : new SelectedAdapter(listener));
    }

    public void setOnNavigationItemReselectedListener(final OnNavigationItemReselectedListener listener) {
        setOnItemReselectedListener(listener == null ? null : new ReselectedAdapter(listener));
    }

    private static final class SelectedAdapter implements OnItemSelectedListener {
        private final OnNavigationItemSelectedListener mListener;

        SelectedAdapter(OnNavigationItemSelectedListener listener) {
            mListener = listener;
        }

        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            return mListener.onNavigationItemSelected(item);
        }
    }

    private static final class ReselectedAdapter implements OnItemReselectedListener {
        private final OnNavigationItemReselectedListener mListener;

        ReselectedAdapter(OnNavigationItemReselectedListener listener) {
            mListener = listener;
        }

        @Override
        public void onNavigationItemReselected(MenuItem item) {
            mListener.onNavigationItemReselected(item);
        }
    }
}
