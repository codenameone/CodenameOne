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
package androidx.appcompat.app;

import android.app.ActivityThread;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.R;
import androidx.appcompat.widget.Toolbar;

import com.codename1.androidcompat.runtime.MenuImpl;
import com.codename1.androidcompat.runtime.MenuPresenter;

/// AppCompat's activity: the support action bar (a `Toolbar` in the layout
/// after `setSupportActionBar`, else the activity's own), AppCompat widgets
/// for the framework tags in its layouts, and night mode.
///
/// It is a `FragmentActivity`, so it hosts AndroidX fragments through
/// `getSupportFragmentManager()`.
public class AppCompatActivity extends FragmentActivity {

    private AppCompatDelegate mDelegate;
    private ToolbarActionBar mToolbarActionBar;
    private WindowDecorActionBar mDecorActionBar;

    public AppCompatActivity() {
    }

    public AppCompatActivity(int contentLayoutId) {
        mContentLayoutId = contentLayoutId;
    }

    private int mContentLayoutId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        installAppCompatViewFactory();
        super.onCreate(savedInstanceState);
        if (mContentLayoutId != 0) {
            setContentView(mContentLayoutId);
        }
    }

    /// Inflates framework tags as AppCompat widgets, unless the application
    /// installed its own factory first.
    void installAppCompatViewFactory() {
        LayoutInflater inflater = getLayoutInflater();
        if (inflater.getFactory2() == null) {
            inflater.setFactory2(AppCompatViewInflater.forTheme(this));
        }
    }

    public AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = new AppCompatDelegate(this);
        }
        return mDelegate;
    }

    // ------------------------------------------------------------ action bar

    /// The `Toolbar` set with `setSupportActionBar`, or the activity's own
    /// action bar; null when the theme has none and no toolbar was set.
    public ActionBar getSupportActionBar() {
        if (mToolbarActionBar != null) {
            return mToolbarActionBar;
        }
        if (!themeHasActionBar() || getActionBar() == null) {
            return null;
        }
        if (mDecorActionBar == null) {
            mDecorActionBar = new WindowDecorActionBar(getActionBar(), this);
        }
        return mDecorActionBar;
    }

    private boolean themeHasActionBar() {
        // Sorted by id, as obtainStyledAttributes requires: framework first.
        TypedArray a = obtainStyledAttributes(new int[] {android.R.attr.windowActionBar, R.attr.windowActionBar});
        try {
            if (a.hasValue(1)) {
                return a.getBoolean(1, true);
            }
            return a.getBoolean(0, true);
        } finally {
            a.recycle();
        }
    }

    /// Makes `toolbar` the action bar: its title follows the activity's, the
    /// options menu shows on it, and up navigation runs from its navigation
    /// button. As on Android, the theme must not supply an action bar of its
    /// own (use a `NoActionBar` theme).
    public void setSupportActionBar(Toolbar toolbar) {
        if (toolbar != null && themeHasActionBar() && getActionBar() != null && getActionBar().isShowing()) {
            throw new IllegalStateException("This Activity already has an action bar supplied by the window decor. "
                    + "Do not request Window.FEATURE_SUPPORT_ACTION_BAR and set windowActionBar to false in your "
                    + "theme to use a Toolbar instead.");
        }
        if (toolbar == null) {
            mToolbarActionBar = null;
            ActivityThread.setMenuPresenter(this, null);
            return;
        }
        ToolbarActionBar ab = new ToolbarActionBar(toolbar, this);
        mToolbarActionBar = ab;
        ActivityThread.setMenuPresenter(this, new ToolbarMenuPresenter(toolbar));
        invalidateOptionsMenu();
    }

    /// Shows the activity's options menu on the support action bar's toolbar.
    private static final class ToolbarMenuPresenter implements MenuPresenter {
        private final Toolbar toolbar;

        ToolbarMenuPresenter(Toolbar toolbar) {
            this.toolbar = toolbar;
        }

        @Override
        public void presentMenu(MenuImpl menu, boolean show) {
            toolbar.setPresentedMenu(menu, show);
        }
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        if (mToolbarActionBar != null) {
            mToolbarActionBar.setTitle(title);
        }
    }

    public void supportInvalidateOptionsMenu() {
        invalidateOptionsMenu();
    }

    /// The navigation button of the support action bar was tapped with
    /// `DISPLAY_HOME_AS_UP`: `onOptionsItemSelected` with `android.R.id.home`
    /// first, then `onSupportNavigateUp`.
    void dispatchSupportHome() {
        MenuImpl menu = new MenuImpl(this);
        MenuItem home = menu.add(0, android.R.id.home, 0, "");
        if (onOptionsItemSelected(home)) {
            return;
        }
        onSupportNavigateUp();
    }

    /// Navigates up. The activity stack already holds the parent an Android
    /// task would navigate up to, so this finishes the activity.
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public boolean supportRequestWindowFeature(int featureId) {
        return requestWindowFeature(featureId);
    }
}
