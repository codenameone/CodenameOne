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
package android.app;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.codename1.androidcompat.runtime.ActivityForm;
import com.codename1.androidcompat.runtime.AndroidApp;
import com.codename1.androidcompat.runtime.AndroidRuntime;
import com.codename1.androidcompat.runtime.MenuImpl;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;

/// Runs activities: creates them through the generated factory, gives each a
/// Codename One form with a decor view, drives the lifecycle in Android's
/// order and keeps the back stack. Everything here runs on the event
/// dispatch thread.
public final class ActivityThread {

    /// The runtime state of one activity.
    static final class Record {
        Activity activity;
        Activity caller;
        int requestCode = -1;
        ActivityForm form;
        FrameLayout decor;
        FrameLayout content;
        ActionBar actionBar;
        MenuImpl menu;
        boolean started;
        boolean resumed;
        boolean menuCreated;
        /// Set once the activity has been stopped, so the next start is a
        /// restart.
        boolean restartable;
        /// The configuration the activity last saw.
        android.content.res.Configuration config;
        /// Configuration changes that arrived while the activity was not
        /// visible and that it does not handle: it is recreated when it
        /// comes back.
        int relaunchPending;
        Bundle savedState;
        AndroidApp.ActivityInfo info;
        /// Shows the options menu instead of the form's toolbar, or null.
        com.codename1.androidcompat.runtime.MenuPresenter menuPresenter;
    }

    private static final ArrayList<Record> STACK = new ArrayList<Record>();
    private static boolean appForeground = true;

    private ActivityThread() {
    }

    public static Activity getTopActivity() {
        return STACK.isEmpty() ? null : STACK.get(STACK.size() - 1).activity;
    }

    public static int getActivityCount() {
        return STACK.size();
    }

    /// The form the runtime's screens are on. While a show transition runs,
    /// `Display.getCurrent()` is still the form being left -- it changes only
    /// when the transition ends, and the Android theme slides -- so anything
    /// attached to it then (a frame callback, an overlay, a popup) lands on a
    /// form about to leave the screen. The top activity's form is the one
    /// arriving.
    public static com.codename1.ui.Form visibleForm() {
        com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
        if (d.isInTransition()) {
            Record t = top();
            if (t != null && t.form != null) {
                return t.form;
            }
        }
        return d.getCurrent();
    }

    private static Record top() {
        return STACK.isEmpty() ? null : STACK.get(STACK.size() - 1);
    }

    private static Record recordOf(Activity a) {
        for (Record r : STACK) {
            if (r.activity == a) {
                return r;
            }
        }
        return null;
    }

    static boolean isRoot(Activity a) {
        return !STACK.isEmpty() && STACK.get(0).activity == a;
    }

    // ------------------------------------------------------------ starting

    /// Starts the activity `intent` names, from `caller` (null at launch).
    public static void startActivity(Context caller, Intent intent, int requestCode) {
        AndroidRuntime rt = AndroidRuntime.getInstance();
        if (intent == null) {
            throw new NullPointerException("intent");
        }
        Class<?> cls = intent.getComponentClass();
        AndroidApp.ActivityInfo info = null;
        if (cls != null) {
            info = rt.getApp().activityInfo(cls);
        } else if (intent.getComponent() != null) {
            info = rt.getApp().activityInfo(intent.getComponent().getClassName());
        } else if (intent.getAction() != null) {
            info = rt.getApp().activityForAction(intent.getAction());
            if (info == null) {
                if (rt.handleImplicitIntent(intent)) {
                    return;
                }
                throw new ActivityNotFoundException("No Activity found to handle " + intent);
            }
        }
        if (info == null) {
            throw new ActivityNotFoundException("Unable to find explicit activity class "
                    + (intent.getComponent() == null ? "" : intent.getComponent().getClassName())
                    + "; have you declared this activity in your AndroidManifest.xml?");
        }
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0) {
            for (int i = STACK.size() - 1; i >= 0; i--) {
                if (STACK.get(i).activity.getClass() == info.type) {
                    Record target = STACK.get(i);
                    Record current = top();
                    if (current != null && current.resumed) {
                        pause(current);
                    }
                    while (STACK.size() - 1 > i) {
                        destroy(STACK.remove(STACK.size() - 1), false);
                    }
                    // Android finishes and recreates the target unless
                    // SINGLE_TOP is set; delivering the intent to the live
                    // instance keeps its state, which is what callers of
                    // CLEAR_TOP rely on in practice.
                    target.activity.setIntent(intent);
                    target.activity.onNewIntent(intent);
                    resumeRecord(target, true);
                    return;
                }
            }
        }
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0) {
            Record t = top();
            if (t != null && t.activity.getClass() == info.type) {
                t.activity.onNewIntent(intent);
                return;
            }
        }
        boolean clearTask = (intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0;
        Activity callerActivity = caller instanceof Activity ? (Activity) caller : null;
        Activity a = rt.getApp().createActivity(info.type);
        if (a == null) {
            throw new ActivityNotFoundException(info.className);
        }
        Record prev = top();
        if (prev != null && prev.resumed) {
            pause(prev);
        }
        Record r = new Record();
        r.activity = a;
        r.caller = requestCode >= 0 ? callerActivity : null;
        r.requestCode = requestCode;
        r.info = info;
        attach(r, intent);
        if (STACK.isEmpty()) {
            // The first activity of an application hosted inside a Codename
            // One app: remember the host's form to return to.
            AndroidRuntime.getInstance().noteHostForm(com.codename1.ui.Display.getInstance().getCurrent());
        }
        STACK.add(r);
        create(r, null);
        start(r);
        resumeRecord(r, false);
        if (prev != null) {
            stop(prev);
        }
        if (clearTask) {
            for (int i = STACK.size() - 2; i >= 0; i--) {
                destroy(STACK.remove(i), false);
            }
        }
    }

    private static void attach(Record r, Intent intent) {
        AndroidRuntime rt = AndroidRuntime.getInstance();
        Activity a = r.activity;
        a.mRecord = r;
        a.mIntent = intent;
        a.mApplication = rt.getApplication();
        int theme = r.info.theme != 0 ? r.info.theme : rt.getApp().getAppTheme();
        if (theme == 0) {
            theme = android.R.style.Theme_DeviceDefault_Light_DarkActionBar;
        }
        a.attachBaseContextForRuntime(rt.getApplication(), theme);
        a.mWindow = new Window(a);
        if (r.info.softInputMode != null) {
            a.mWindow.setSoftInputMode(softInputMode(r.info.softInputMode));
        }
        a.mTitle = r.info.label != null ? r.info.label
                : r.info.labelRes != 0 ? a.getText(r.info.labelRes) : rt.getApplicationLabel();
        a.mRequestedOrientation = r.info.screenOrientation;

        r.decor = new FrameLayout(a);
        r.content = new FrameLayout(a);
        r.content.setId(android.R.id.content);
        r.decor.addView(r.content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        TypedValue tv = new TypedValue();
        if (a.getTheme().resolveAttribute(android.R.attr.windowBackground, tv, true)) {
            Drawable bg = tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT
                    ? new ColorDrawable(tv.data)
                    : tv.resourceId != 0 ? a.getResources().getDrawable(tv.resourceId, a.getTheme()) : null;
            r.decor.setBackground(bg);
        }
        final Record rec = r;
        a.mWindow.attach(r.decor, new Runnable() {
            @Override
            public void run() {
                updateToolbar(rec);
            }
        });
        r.actionBar = new ActionBar(r);
        r.actionBar.setTitle(a.mTitle);
        r.config = new android.content.res.Configuration(a.getResources().getConfiguration());

        r.form = new ActivityForm(a, r.decor.getPeer());
        r.form.setBackCommand(new Command("") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                onBack(rec);
            }
        });
        updateToolbar(r);
    }

    private static void create(Record r, Bundle saved) {
        Activity a = r.activity;
        Application app = a.mApplication;
        a.mCalled = false;
        a.onCreate(saved);
        if (!a.mCalled) {
            throw new IllegalStateException("Activity " + a.getClass().getName()
                    + " did not call through to super.onCreate()");
        }
        a.hostsDispatchActivityCreated();
        for (Application.ActivityLifecycleCallbacks cb : app.callbacks()) {
            cb.onActivityCreated(a, saved);
        }
        if (saved != null) {
            a.onRestoreInstanceState(saved);
        }
        a.onPostCreate(saved);
    }

    private static void start(Record r) {
        Activity a = r.activity;
        a.hostsNoteStateNotSaved();
        a.hostsExecPendingActions();
        a.onStart();
        a.hostsDispatchStart();
        r.started = true;
        for (Application.ActivityLifecycleCallbacks cb : a.mApplication.callbacks()) {
            cb.onActivityStarted(a);
        }
    }

    private static void resumeRecord(Record r, boolean back) {
        Activity a = r.activity;
        if (!r.started) {
            if (r.restartable) {
                a.onRestart();
            }
            start(r);
        }
        if (!r.menuCreated) {
            r.menuCreated = true;
            buildOptionsMenu(r);
        }
        applyOrientation(a);
        if (back) {
            r.form.showBack();
        } else {
            r.form.show();
        }
        r.decor.dispatchAttachedToWindow(true);
        a.onResume();
        r.resumed = true;
        a.hostsDispatchResume();
        a.hostsExecPendingActions();
        a.onPostResume();
        for (Application.ActivityLifecycleCallbacks cb : a.mApplication.callbacks()) {
            cb.onActivityResumed(a);
        }
        a.onWindowFocusChanged(true);
    }

    private static void pause(Record r) {
        Activity a = r.activity;
        a.onWindowFocusChanged(false);
        a.hostsDispatchPause();
        a.onPause();
        r.resumed = false;
        for (Application.ActivityLifecycleCallbacks cb : a.mApplication.callbacks()) {
            cb.onActivityPaused(a);
        }
    }

    private static void stop(Record r) {
        if (!r.started) {
            return;
        }
        Activity a = r.activity;
        Bundle out = new Bundle();
        a.onSaveInstanceState(out);
        for (Application.ActivityLifecycleCallbacks cb : a.mApplication.callbacks()) {
            cb.onActivitySaveInstanceState(a, out);
        }
        r.savedState = out;
        a.hostsDispatchStop();
        a.onStop();
        r.started = false;
        r.restartable = true;
        for (Application.ActivityLifecycleCallbacks cb : a.mApplication.callbacks()) {
            cb.onActivityStopped(a);
        }
    }

    private static void destroy(Record r, boolean deliverResult) {
        Activity a = r.activity;
        if (r.resumed) {
            pause(r);
        }
        stop(r);
        a.mFinished = true;
        r.decor.dispatchAttachedToWindow(false);
        a.hostsDispatchDestroy();
        a.onDestroy();
        a.mDestroyed = true;
        for (Application.ActivityLifecycleCallbacks cb : a.mApplication.callbacks()) {
            cb.onActivityDestroyed(a);
        }
    }

    // ------------------------------------------------------------ finishing

    static void finish(Activity a) {
        Record r = recordOf(a);
        if (r == null || a.mFinished) {
            return;
        }
        a.mFinished = true;
        int index = STACK.indexOf(r);
        boolean isTop = index == STACK.size() - 1;
        STACK.remove(index);
        if (isTop) {
            if (r.resumed) {
                pause(r);
            }
            Record below = top();
            if (below != null) {
                if (below.relaunchPending != 0) {
                    below = relaunch(below, below.relaunchPending, false);
                }
                deliverResult(r, below);
                resumeRecord(below, true);
            }
            destroy(r, false);
            if (below == null) {
                AndroidRuntime.getInstance().onLastActivityFinished();
            }
        } else {
            destroy(r, false);
            Record t = top();
            if (t != null) {
                deliverResult(r, t);
            }
        }
    }

    private static void deliverResult(Record finished, Record target) {
        if (finished.caller != null && finished.requestCode >= 0 && finished.caller == target.activity) {
            Activity a = finished.activity;
            target.activity.dispatchActivityResult(finished.requestCode, a.mResultCode, a.mResultData);
        }
    }

    /// Finishes every activity at once, as `finishAffinity` on the root
    /// does: none of them is resumed on the way out. In an application hosted
    /// inside a Codename One app, the host's form comes back.
    public static void finishAllActivities() {
        if (!STACK.isEmpty()) {
            finishAll();
        }
    }

    static void finishAll() {
        while (!STACK.isEmpty()) {
            Record r = STACK.remove(STACK.size() - 1);
            r.activity.mFinished = true;
            destroy(r, false);
        }
        AndroidRuntime.getInstance().onLastActivityFinished();
    }

    static void finishChild(Activity caller, int requestCode) {
        for (int i = STACK.size() - 1; i >= 0; i--) {
            Record r = STACK.get(i);
            if (r.caller == caller && r.requestCode == requestCode) {
                finish(r.activity);
                return;
            }
        }
    }

    static void recreate(Activity a) {
        Record r = recordOf(a);
        if (r != null) {
            relaunch(r, 0, true);
        }
    }

    /// Destroys the activity and creates a new instance in its place from
    /// its saved state, as Android does for `recreate()` and for a
    /// configuration change the activity does not handle. Fragments that
    /// retain their instance, and `onRetainNonConfigurationInstance`, carry
    /// over. A visible activity is resumed again; one further down the stack
    /// stays created until it comes back.
    private static Record relaunch(Record r, int changes, boolean resumeIfTop) {
        Activity a = r.activity;
        int index = STACK.indexOf(r);
        boolean top = index == STACK.size() - 1;
        a.mChangingConfigurations = true;
        a.mConfigChangeFlags = changes;
        if (r.resumed) {
            pause(r);
        }
        if (r.started) {
            stop(r);
        }
        Bundle saved = r.savedState;
        if (saved == null) {
            saved = new Bundle();
            a.onSaveInstanceState(saved);
        }
        int[] requests = a.snapshotFragmentRequests();
        Object nonConfig = a.onRetainNonConfigurationInstance();
        java.util.HashMap<String, ArrayList<Fragment>> retained = a.hostsRetainNonConfig();
        destroy(r, false);
        Activity fresh = AndroidRuntime.getInstance().getApp().createActivity(r.info.type);
        fresh.mLastNonConfigurationInstance = nonConfig;
        fresh.mLastRetainedFragments = retained;
        Record n = new Record();
        n.activity = fresh;
        n.caller = r.caller;
        n.requestCode = r.requestCode;
        n.info = r.info;
        attach(n, a.getIntent());
        STACK.set(index, n);
        for (Record other : STACK) {
            if (other.caller == a) {
                other.caller = fresh;
            }
        }
        create(n, saved);
        fresh.restoreFragmentRequests(requests, a.nextFragmentRequest());
        if (top && resumeIfTop && appForeground) {
            start(n);
            resumeRecord(n, false);
        }
        return n;
    }

    // ------------------------------------------------------------ app lifecycle

    /// Codename One moved the application to the background.
    public static void onAppStop() {
        if (!appForeground) {
            return;
        }
        appForeground = false;
        Record t = top();
        if (t != null) {
            t.activity.onUserLeaveHint();
            if (t.resumed) {
                pause(t);
            }
            stop(t);
        }
    }

    /// Codename One brought the application back.
    public static void onAppStart() {
        if (appForeground) {
            return;
        }
        appForeground = true;
        // The locale or dark mode may have changed while the application was
        // in the background. Nothing resizes on the way back, so ask now, as
        // Android delivers the change before resuming the activity.
        com.codename1.androidcompat.runtime.ResourceManager rm =
                com.codename1.androidcompat.runtime.ResourceManager.get();
        if (!STACK.isEmpty() && rm.refresh()) {
            onConfigurationChanged(rm.configuration());
        }
        Record t = top();
        if (t != null) {
            if (t.relaunchPending != 0) {
                t = relaunch(t, t.relaunchPending, false);
            }
            resumeRecord(t, false);
        }
    }

    /// The configuration changed (rotation, dark mode, locale). As on
    /// Android, an activity that declares the change in
    /// `android:configChanges` is told through `onConfigurationChanged`;
    /// any other is recreated from its saved state -- at once when it is
    /// visible, when it comes back otherwise.
    public static void onConfigurationChanged(android.content.res.Configuration config) {
        for (final Record r : new ArrayList<Record>(STACK)) {
            int diff = r.config == null ? 0 : r.config.diff(config);
            r.config = new android.content.res.Configuration(config);
            r.activity.getTheme().rebase();
            if (diff == 0) {
                r.decor.requestLayout();
                continue;
            }
            if ((diff & ~r.info.configChanges) != 0) {
                r.relaunchPending |= diff;
                if (r == top()) {
                    // After the event that reported the change, not inside it.
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            if (appForeground && STACK.contains(r) && r.relaunchPending != 0 && r == top()) {
                                int changes = r.relaunchPending;
                                r.relaunchPending = 0;
                                relaunch(r, changes, true);
                            }
                        }
                    });
                }
                continue;
            }
            Activity a = r.activity;
            a.mCalled = false;
            a.onConfigurationChanged(config);
            if (!a.mCalled) {
                throw new SuperNotCalledException("Activity " + a.getClass().getName()
                        + " did not call through to super.onConfigurationChanged()");
            }
            r.decor.requestLayout();
        }
    }

    /// The manifest's `android:windowSoftInputMode` flags as the window's mode.
    static int softInputMode(String flags) {
        int mode = 0;
        int start = 0;
        while (start <= flags.length()) {
            int bar = flags.indexOf('|', start);
            String f = (bar < 0 ? flags.substring(start) : flags.substring(start, bar)).trim();
            if (f.equals("stateUnchanged")) {
                mode |= WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED;
            } else if (f.equals("stateHidden")) {
                mode |= WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN;
            } else if (f.equals("stateAlwaysHidden")) {
                mode |= WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
            } else if (f.equals("stateVisible")) {
                mode |= WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
            } else if (f.equals("stateAlwaysVisible")) {
                mode |= WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;
            } else if (f.equals("adjustResize")) {
                mode |= WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
            } else if (f.equals("adjustPan")) {
                mode |= WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
            } else if (f.equals("adjustNothing")) {
                mode |= WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
            }
            if (bar < 0) {
                break;
            }
            start = bar + 1;
        }
        return mode;
    }

    static void onBack(Record r) {
        if (com.codename1.androidcompat.runtime.PopupHost.dismissTopOnBack()) {
            return;
        }
        Activity a = r.activity;
        if (a.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,
                android.view.KeyEvent.KEYCODE_BACK))) {
            return;
        }
        a.onBackPressed();
    }

    static void requestPermissions(final Activity a, final String[] permissions, final int requestCode) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                int[] grants = new int[permissions.length];
                a.onRequestPermissionsResult(requestCode, permissions, grants);
            }
        });
    }

    static void applyOrientation(Activity a) {
        int o = a.mRequestedOrientation;
        Display d = Display.getInstance();
        if (!d.canForceOrientation()) {
            return;
        }
        if (o == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || o == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                || o == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            d.lockOrientation(true);
        } else if (o == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                || o == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                || o == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            d.lockOrientation(false);
        } else {
            d.unlockOrientation();
        }
    }

    // ------------------------------------------------------------ toolbar & menu

    /// Reflects the theme, the action bar and the window flags onto the
    /// form's toolbar.
    static void updateToolbar(Record r) {
        if (r.form == null) {
            return;
        }
        Activity a = r.activity;
        Toolbar tb = r.form.getToolbar();
        if (tb == null) {
            return;
        }
        TypedArray t = a.getTheme().obtainStyledAttributes(new int[] {android.R.attr.windowActionBar,
            android.R.attr.windowNoTitle, android.R.attr.colorPrimary, android.R.attr.textColorPrimaryInverse,
            android.R.attr.windowFullscreen, android.R.attr.statusBarColor});
        boolean hasBar = t.getBoolean(0, true) && !t.getBoolean(1, false) && !a.mWindow.isNoTitleRequested()
                && r.actionBar.isShowing();
        int primary = t.getColor(2, 0xff212121);
        int titleColor = t.getColor(3, 0xffffffff);
        int statusBar = t.getColor(5, 0);
        t.recycle();
        r.form.setStatusBarColor(hasBar ? 0 : statusBar);
        tb.setVisible(hasBar);
        tb.setHidden(!hasBar);
        if (!hasBar) {
            return;
        }
        Drawable bg = r.actionBar.getBackgroundDrawable();
        if (bg instanceof ColorDrawable) {
            primary = ((ColorDrawable) bg).getColor();
        }
        Style s = tb.getAllStyles();
        s.setBgColor(primary & 0xffffff);
        s.setBgTransparency(255);
        CharSequence title = (r.actionBar.getDisplayOptions() & ActionBar.DISPLAY_SHOW_TITLE) != 0
                ? r.actionBar.getTitle() : null;
        tb.setTitle(title == null ? "" : title.toString());
        if (tb.getTitleComponent() != null) {
            tb.getTitleComponent().getAllStyles().setFgColor(titleColor & 0xffffff);
        }
        r.form.setUpButton((r.actionBar.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0, titleColor);
        r.form.styleToolbar(titleColor);
        r.form.revalidate();
    }

    private static void buildOptionsMenu(final Record r) {
        if (r.menu == null) {
            r.menu = new MenuImpl(r.activity);
            r.menu.setItemHandler(new MenuImpl.ItemHandler() {
                @Override
                public boolean onItemSelected(MenuItem item) {
                    return r.activity.onOptionsItemSelected(item)
                            || r.activity.hostsOptionsItemSelected(item);
                }
            });
        }
        r.menu.clear();
        r.menu.setListener(null);
        // The activity's items first, then each fragment's, in the order the
        // fragments were added; either may ask for the menu to show.
        Activity a = r.activity;
        boolean create = a.onCreateOptionsMenu(r.menu) | a.hostsCreateOptionsMenu(r.menu,
                a.getMenuInflater());
        boolean show = create && (a.onPrepareOptionsMenu(r.menu) | a.hostsPrepareOptionsMenu(r.menu));
        r.menu.setListener(new MenuImpl.Listener() {
            @Override
            public void menuChanged(MenuImpl menu) {
                presentMenu(r, menu, true);
            }
        });
        presentMenu(r, r.menu, show);
    }

    private static void presentMenu(Record r, MenuImpl menu, boolean show) {
        if (r.menuPresenter != null) {
            r.form.showMenu(menu, false);
            r.menuPresenter.presentMenu(menu, show);
        } else {
            r.form.showMenu(menu, show);
        }
    }

    /// Runtime use: shows `a`'s options menu through `presenter` (a support
    /// action bar's `Toolbar`) instead of the form's toolbar; null restores
    /// the form's toolbar. The menu is rebuilt if it has been created.
    public static void setMenuPresenter(Activity a, com.codename1.androidcompat.runtime.MenuPresenter presenter) {
        Record r = recordOf(a);
        if (r == null) {
            return;
        }
        r.menuPresenter = presenter;
        if (r.menuCreated) {
            buildOptionsMenu(r);
        }
    }

    static void invalidateOptionsMenu(Activity a) {
        Record r = recordOf(a);
        if (r != null && r.menuCreated) {
            buildOptionsMenu(r);
        }
    }

    static void openOptionsMenu(Activity a) {
        Record r = recordOf(a);
        if (r != null) {
            r.form.openOverflow();
        }
    }

    /// The action bar home/up button was tapped.
    public static void onHomePressed(Activity a) {
        Record r = recordOf(a);
        MenuImpl m = r == null ? new MenuImpl(a) : r.menu;
        MenuItem home = m.add(0, android.R.id.home, 0, "");
        if (!a.onOptionsItemSelected(home) && !a.hostsOptionsItemSelected(home)) {
            a.onBackPressed();
        }
        m.removeItem(android.R.id.home);
    }
}
