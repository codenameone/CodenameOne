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

import android.app.FragmentManagerImpl;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;

/// An activity hosting AndroidX fragments, through
/// [#getSupportFragmentManager()]. The back key pops their back stack
/// before anything else the activity does with it, as AndroidX registers
/// the fragment back stack as a back callback.
public class FragmentActivity extends ComponentActivity {

    static final String FRAGMENTS_TAG = "android:support:fragments";

    private final FragmentManagerImpl mSupportHost;
    private final FragmentManager mSupportFragments;
    private final OnBackPressedCallback mBackCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (!mSupportHost.isStateSaved()) {
                mSupportHost.popBackStackImmediate();
            }
        }
    };

    public FragmentActivity() {
        mSupportHost = FragmentManagerImpl.attachHost(this, FRAGMENTS_TAG);
        mSupportFragments = SupportFragmentManager.of(mSupportHost);
        mSupportHost.addOnBackStackChangedListener(new android.app.FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                mBackCallback.setEnabled(mSupportHost.getBackStackEntryCount() > 0);
            }
        });
        getOnBackPressedDispatcher().addCallback(mBackCallback);
    }

    public FragmentActivity(int contentLayoutId) {
        this();
        mContentLayoutId = contentLayoutId;
    }

    private int mContentLayoutId;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBackCallback.setEnabled(mSupportHost.getBackStackEntryCount() > 0);
        if (mContentLayoutId != 0) {
            setContentView(mContentLayoutId);
        }
    }

    public FragmentManager getSupportFragmentManager() {
        return mSupportFragments;
    }

    /// Called when an AndroidX fragment attaches to this activity.
    public void onAttachFragment(Fragment fragment) {
    }

    @Override
    public void onAttachFragment(android.app.Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof Fragment) {
            onAttachFragment((Fragment) fragment);
        }
    }

    /// `<fragment>` tags and `FragmentContainerView`s in this activity's
    /// layouts hold AndroidX fragments.
    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if ("fragment".equals(name) || "androidx.fragment.app.FragmentContainerView".equals(name)) {
            return mSupportHost.onCreateView(parent, name, context, attrs);
        }
        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        onResumeFragments();
    }

    /// Called once the activity and its fragments are resumed.
    protected void onResumeFragments() {
    }

    public void supportInvalidateOptionsMenu() {
        invalidateOptionsMenu();
    }

    public void supportFinishAfterTransition() {
        finish();
    }

    public void supportPostponeEnterTransition() {
    }

    public void supportStartPostponedEnterTransition() {
    }
}
