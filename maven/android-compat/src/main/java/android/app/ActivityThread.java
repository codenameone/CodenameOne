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
        /// Launched with `FLAG_ACTIVITY_NO_HISTORY`: finished, rather than
        /// stopped, as soon as another activity covers it.
        boolean noHistory;
        ActivityForm form;
        FrameLayout decor;
        FrameLayout content;
        ActionBar actionBar;
        MenuImpl menu;
        boolean started;
        boolean resumed;
        boolean destroying;
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
        /// Set by create(): the first start still owes `onRestoreInstanceState`
        /// (with [#restoreState], when there is one) and `onPostCreate`.
        boolean postCreatePending;
        Bundle restoreState;
        AndroidApp.ActivityInfo info;
        /// Shows the options menu instead of the form's toolbar, or null.
        com.codename1.androidcompat.runtime.MenuPresenter menuPresenter;
        /// Results of activities this one started that finished while it was
        /// not resumed, as `{requestCode, resultCode, data}`: delivered before
        /// its next `onResume`, as Android queues them on the caller.
        ArrayList<Object[]> pendingResults;
    }

    private static final ArrayList<Record> STACK = new ArrayList<Record>();
    private static boolean appForeground = true;
    private static int pendingLaunches;

    private ActivityThread() {
    }

    public static Activity getTopActivity() {
        return STACK.isEmpty() ? null : STACK.get(STACK.size() - 1).activity;
    }

    /// Runtime use: snapshots for a process lifecycle observer attaching
    /// during an activity callback. A record on the stack may still be in
    /// onCreate and must not be counted as started or resumed yet.
    public static ArrayList<Activity> getStartedActivities() {
        return activitiesInState(false);
    }

    /// Runtime use: activities whose onResume has completed.
    public static ArrayList<Activity> getResumedActivities() {
        return activitiesInState(true);
    }

    private static ArrayList<Activity> activitiesInState(boolean resumed) {
        ArrayList<Activity> out = new ArrayList<Activity>();
        for (Record r : STACK) {
            if (resumed ? r.resumed : r.started) {
                out.add(r.activity);
            }
        }
        return out;
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

    /// The live instance standing for `a`: `a` itself, or the instance
    /// that replaced it when it was recreated (a configuration change,
    /// `recreate()`), followed through every recreation since. Null when
    /// that instance is destroyed for good. A result that arrives from
    /// outside the activity stack -- the gallery -- goes here, as Android
    /// delivers it to the recreated activity rather than the old one.
    public static Activity currentInstance(Activity a) {
        while (a != null && a.mReplacement != null) {
            a = a.mReplacement;
        }
        return a == null || a.mDestroyed ? null : a;
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

    /// The manifest entry `intent` resolves to, or null; nothing is started
    /// and nothing throws.
    private static AndroidApp.ActivityInfo resolve(Intent intent) {
        AndroidApp app = AndroidRuntime.getInstance().getApp();
        Class<?> cls = intent.getComponentClass();
        if (cls != null) {
            return app.activityInfo(cls);
        }
        if (intent.getComponent() != null) {
            return app.activityInfo(intent.getComponent().getClassName());
        }
        return intent.getAction() != null ? app.activityForIntent(intent) : null;
    }

    /// Whether starting `intent` from `caller` would hand it to `caller`
    /// itself through `onNewIntent` rather than create an activity: the
    /// caller is on top, the intent resolves to its own class, and the
    /// launch is single-top (by flag or launch mode) or reuses the instance.
    /// That is the case `startActivityIfNeeded` returns false for.
    static boolean deliversToCaller(Activity caller, Intent intent) {
        Record t = top();
        if (intent == null || t == null || t.activity != caller) {
            return false;
        }
        AndroidApp.ActivityInfo info = resolve(intent);
        if (info == null || info.type != caller.getClass()) {
            return false;
        }
        int flags = intent.getFlags();
        return info.launchMode != android.content.pm.ActivityInfo.LAUNCH_MULTIPLE
                || (flags & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0
                || (flags & (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP))
                == Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
    }

    /// Whether an instance of the activity `intent` resolves to is beneath
    /// `a` on the stack.
    static boolean isBelow(Activity a, Intent intent) {
        AndroidApp.ActivityInfo info = resolve(intent);
        Record r = recordOf(a);
        if (info == null || r == null) {
            return false;
        }
        for (int i = STACK.indexOf(r) - 1; i >= 0; i--) {
            if (STACK.get(i).activity.getClass() == info.type) {
                return true;
            }
        }
        return false;
    }

    /// `Activity.navigateUpTo`, as Android's activity manager does it: when
    /// the parent is beneath `a`, everything above the parent is finished and
    /// the parent sees `upIntent` -- through `onNewIntent` when it is
    /// single-top, single-task or the intent carries `CLEAR_TOP`, otherwise
    /// as a new instance in its place. When it is not, `a` alone is finished
    /// and the answer is false.
    static boolean navigateUpTo(Activity a, Intent upIntent) {
        if (!isBelow(a, upIntent)) {
            a.finish();
            return false;
        }
        Intent up = new Intent(upIntent);
        if ((upIntent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0) {
            up.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        up.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(a, up, -1);
        return true;
    }

    // ------------------------------------------------------------ starting

    /// Starts the activity `intent` names, from `caller` (null at launch).
    public static void startActivity(Context caller, Intent intent, int requestCode) {
        AndroidRuntime rt = AndroidRuntime.getInstance();
        if (intent == null) {
            throw new NullPointerException("intent");
        }
        // A chooser wraps the activity intent; resolve that inner intent
        // through the same in-app and platform paths as a direct launch.
        if (Intent.ACTION_CHOOSER.equals(intent.getAction())) {
            Intent inner = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (inner != null) {
                intent = inner;
            }
        }
        // A launch crosses a process boundary on Android, so the activity
        // never shares the caller's object: a caller that reuses or mutates
        // its Intent after startActivity() must not change getIntent() or
        // what onNewIntent() was given.
        intent = snapshotIntent(intent);
        // Activities start on the EDT, which is the main looper's thread.
        android.os.Looper.prepareMainLooper();
        // A component's package, and an implicit intent's setPackage(), are
        // deliberately not checked against this application's. This runtime
        // hosts one application and cannot launch another, and its package
        // name is the manifest's (the Gradle namespace), not the Gradle
        // applicationId Android reports: a strict match would reject the
        // common ComponentName(BuildConfig.APPLICATION_ID, ...) wherever the
        // two differ. A foreign-package intent naming one of this
        // application's own classes is not worth that breakage.
        Class<?> cls = intent.getComponentClass();
        AndroidApp.ActivityInfo info = null;
        if (cls != null) {
            info = rt.getApp().activityInfo(cls);
        } else if (intent.getComponent() != null) {
            info = rt.getApp().activityInfo(intent.getComponent().getClassName());
        } else if (intent.getAction() != null) {
            info = rt.getApp().activityForIntent(intent);
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
        // The manifest's launchMode, on the one task this runtime has:
        // singleTop behaves as FLAG_ACTIVITY_SINGLE_TOP, and singleTask /
        // singleInstance as CLEAR_TOP onto the existing instance (no separate
        // task, no task affinity).
        boolean clearTask = (intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0;
        boolean clearTopFlag = (intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0;
        boolean reuseInStack = info.launchMode >= android.content.pm.ActivityInfo.LAUNCH_SINGLE_TASK;
        boolean singleTop = (intent.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0
                || info.launchMode == android.content.pm.ActivityInfo.LAUNCH_SINGLE_TOP;
        // A standard-mode target of a plain CLEAR_TOP is finished and a new
        // instance created in its place, as Android does; onCreate then sees
        // the new intent. Only a single-top or single-task target is reused.
        Record recreated = null;
        if (!clearTask && (clearTopFlag || reuseInStack)) {
            for (int i = STACK.size() - 1; i >= 0; i--) {
                if (STACK.get(i).activity.getClass() == info.type) {
                    Record target = STACK.get(i);
                    Record current = top();
                    if (current != null && current.resumed) {
                        pause(current);
                    }
                    while (STACK.size() - 1 > i) {
                        // Finished, not just destroyed, as Android does: a
                        // result the removed activity owes (RESULT_CANCELED
                        // unless it set one) still reaches its caller.
                        Record removed = STACK.remove(STACK.size() - 1);
                        destroy(removed, false);
                        queueResult(removed);
                    }
                    if (!singleTop && !reuseInStack) {
                        // Destroyed once the new instance is showing, so
                        // the display never falls back to the host form.
                        recreated = STACK.remove(i);
                        recreated.activity.mFinished = true;
                        break;
                    }
                    // Reuse keeps the original intent, as Android does, with
                    // or without CLEAR_TOP: the new one reaches onNewIntent
                    // only, and getIntent() changes when the activity calls
                    // setIntent() itself.
                    target.activity.onNewIntent(intent);
                    resumeRecord(target, true);
                    return;
                }
            }
        }
        if (!clearTask && !clearTopFlag && (intent.getFlags() & Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) != 0) {
            // The existing instance moves to the top with the activities
            // above it left in place, and sees the new intent through
            // onNewIntent(). Android ignores the flag under CLEAR_TOP, which
            // the branch above already handled. When the instance is
            // already on top this is the single-top case below.
            Record current = top();
            for (int i = STACK.size() - 2; i >= 0; i--) {
                Record target = STACK.get(i);
                if (target.activity.getClass() == info.type) {
                    if (current.resumed) {
                        pause(current);
                    }
                    if (target.relaunchPending != 0) {
                        // A configuration change it missed while covered,
                        // as when it comes back through finish().
                        target = relaunch(target, target.relaunchPending, false);
                        if (target == null) {
                            resumeRecord(current, false);
                            return;
                        }
                    }
                    STACK.remove(i);
                    STACK.add(target);
                    target.activity.onNewIntent(intent);
                    resumeRecord(target, false);
                    if (current.noHistory && STACK.remove(current)) {
                        destroy(current, false);
                        queueResult(current);
                    } else {
                        stop(current);
                    }
                    return;
                }
            }
            if (current != null && current.activity.getClass() == info.type) {
                singleTop = true;
            }
        }
        if (!clearTask && singleTop) {
            Record t = top();
            if (t != null && t.activity.getClass() == info.type) {
                // Paused around onNewIntent() and resumed after it, as
                // Android does and as the reuse branch above does: work an
                // application keeps in onResume() sees the new intent.
                if (t.resumed) {
                    pause(t);
                }
                t.activity.onNewIntent(intent);
                resumeRecord(t, false);
                return;
            }
        }
        Activity callerActivity = caller instanceof Activity ? (Activity) caller : null;
        Activity a = rt.getApp().createActivity(info.type);
        if (a == null) {
            throw new ActivityNotFoundException(info.className);
        }
        Record prev = top();
        Record r = new Record();
        // onPause may finish the last activity. Keep this launch pending until
        // the destination is on the stack so that finish() doesn't close the
        // task in the middle of navigation.
        pendingLaunches++;
        try {
            if (prev != null && prev.resumed) {
                pause(prev);
            }
            r.activity = a;
            r.caller = requestCode >= 0 ? callerActivity : null;
            r.requestCode = requestCode;
            r.info = info;
            // The manifest's android:noHistory and the launch flag mean the same.
            r.noHistory = info.noHistory || (intent.getFlags() & Intent.FLAG_ACTIVITY_NO_HISTORY) != 0;
            attach(r, intent);
            if (STACK.isEmpty() && recreated == null) {
                // The first activity of an application hosted inside a Codename
                // One app: remember the host's form to return to.
                AndroidRuntime.getInstance().noteHostForm(com.codename1.ui.Display.getInstance().getCurrent());
            }
            STACK.add(r);
        } finally {
            pendingLaunches--;
        }
        if (clearTask) {
            // Keep the new record on the stack while disposing the old task,
            // before any launch callbacks can finish or redirect this launch.
            while (STACK.indexOf(r) > 0) {
                destroy(STACK.remove(0), false);
            }
            prev = null;
        }
        create(r, null);
        start(r);
        resumeRecord(r, false);
        if (prev != null && isLive(prev) && top() != prev) {
            if (prev.noHistory && STACK.remove(prev)) {
                // Never returned to, as on Android: finishing the new
                // activity resumes the one beneath. A result it asked for
                // is lost with it, which Android documents too. It is
                // finished, not just destroyed: the result it owes its own
                // caller (RESULT_CANCELED unless it set one) still goes out.
                destroy(prev, false);
                queueResult(prev);
            } else {
                stop(prev);
            }
        }
        if (recreated != null) {
            destroy(recreated, false);
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

    private static boolean isLive(Record r) {
        return !r.activity.mFinished && !r.activity.mDestroyed && STACK.contains(r);
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
        if (!isLive(r)) {
            return;
        }
        a.hostsDispatchActivityCreated();
        if (!isLive(r)) {
            return;
        }
        for (Application.ActivityLifecycleCallbacks cb : app.callbacks()) {
            cb.onActivityCreated(a, saved);
            if (!isLive(r)) {
                return;
            }
        }
        // Android restores the instance state and calls onPostCreate after
        // onStart, so they run in the first start(): a relaunched activity
        // further down the stack stays created and gets them when it comes
        // back.
        if (!isLive(r)) {
            return;
        }
        r.postCreatePending = true;
        r.restoreState = saved;
    }

    private static void start(Record r) {
        if (!isLive(r)) {
            return;
        }
        Activity a = r.activity;
        a.hostsNoteStateNotSaved();
        a.hostsExecPendingActions();
        if (!isLive(r)) {
            return;
        }
        r.started = true;
        a.onStart();
        if (!isLive(r)) {
            return;
        }
        a.hostsDispatchStart();
        if (!isLive(r)) {
            return;
        }
        for (Application.ActivityLifecycleCallbacks cb : a.mApplication.callbacks()) {
            cb.onActivityStarted(a);
            if (!isLive(r)) {
                return;
            }
        }
        if (isLive(r) && r.postCreatePending) {
            r.postCreatePending = false;
            Bundle saved = r.restoreState;
            r.restoreState = null;
            if (saved != null) {
                a.onRestoreInstanceState(saved);
            }
            if (isLive(r)) {
                a.onPostCreate(saved);
            }
        }
    }

    private static void resumeRecord(Record r, boolean back) {
        if (!isLive(r) || top() != r) {
            return;
        }
        Activity a = r.activity;
        if (!r.started) {
            if (r.restartable) {
                a.onRestart();
            }
            start(r);
        }
        if (!isLive(r) || top() != r) {
            return;
        }
        deliverPendingResults(r);
        if (!isLive(r) || top() != r) {
            return;
        }
        if (!r.menuCreated) {
            r.menuCreated = true;
            buildOptionsMenu(r);
        }
        if (!isLive(r) || top() != r) {
            return;
        }
        applyOrientation(a);
        if (back) {
            r.form.showBack();
        } else {
            r.form.show();
        }
        r.decor.dispatchAttachedToWindow(true);
        r.resumed = true;
        a.mWindow.setActive(true);
        a.onResume();
        if (!isLive(r) || top() != r) {
            return;
        }
        a.hostsDispatchResume();
        a.hostsExecPendingActions();
        if (!isLive(r) || top() != r) {
            return;
        }
        a.onPostResume();
        if (!isLive(r) || top() != r) {
            return;
        }
        for (Application.ActivityLifecycleCallbacks cb : a.mApplication.callbacks()) {
            cb.onActivityResumed(a);
            if (!isLive(r) || top() != r) {
                return;
            }
        }
        if (!isLive(r) || top() != r) {
            return;
        }
        a.onWindowFocusChanged(true);
        r.decor.dispatchWindowFocusChanged(true);
    }

    private static void pause(Record r) {
        r.resumed = false;
        Activity a = r.activity;
        a.mWindow.setActive(false);
        // The activity first, then its views, as Android's decor view does;
        // without the views, hasWindowFocus() stayed true under a covering
        // activity and focus-guarded work kept running.
        a.onWindowFocusChanged(false);
        if (a.mDestroyed) {
            return;
        }
        r.decor.dispatchWindowFocusChanged(false);
        if (a.mDestroyed) {
            return;
        }
        a.hostsDispatchPause();
        if (a.mDestroyed) {
            return;
        }
        a.onPause();
        for (Application.ActivityLifecycleCallbacks cb : a.mApplication.callbacks()) {
            if (a.mDestroyed) {
                return;
            }
            cb.onActivityPaused(a);
        }
    }

    private static void stop(Record r) {
        if (!r.started) {
            return;
        }
        // Save/stop callbacks may finish the activity and reenter destroy().
        r.started = false;
        r.restartable = true;
        Activity a = r.activity;
        // A finishing activity is never restored, so Android does not ask it
        // to save its state; only one that may come back (stopped under
        // another activity, the app backgrounded, a relaunch) saves.
        if (!a.mFinished) {
            Bundle out = new Bundle();
            a.onSaveInstanceState(out);
            for (Application.ActivityLifecycleCallbacks cb : a.mApplication.callbacks()) {
                if (a.mDestroyed) {
                    return;
                }
                cb.onActivitySaveInstanceState(a, out);
            }
            if (a.mDestroyed) {
                return;
            }
            r.savedState = out;
        }
        a.hostsDispatchStop();
        if (a.mDestroyed) {
            return;
        }
        a.onStop();
        for (Application.ActivityLifecycleCallbacks cb : a.mApplication.callbacks()) {
            if (a.mDestroyed) {
                return;
            }
            cb.onActivityStopped(a);
        }
    }

    private static void destroy(Record r, boolean relaunching) {
        Activity a = r.activity;
        if (r.destroying || a.mDestroyed) {
            return;
        }
        r.destroying = true;
        // Finishing from the first callback on, as on Android: isFinishing()
        // is true in onPause, and stop() skips the state save. A relaunch is
        // not finishing (isFinishing() stays false, isChangingConfigurations()
        // is true), and has already stopped and saved the instance.
        if (!relaunching) {
            a.mFinished = true;
        }
        if (r.resumed) {
            pause(r);
        }
        stop(r);
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
            boolean wasLast = below == null;
            if (below != null) {
                if (below.relaunchPending != 0) {
                    Activity replaced = below.activity;
                    below = relaunch(below, below.relaunchPending, false);
                    // relaunch() moves the caller of every record still on
                    // the stack to the new instance, but this one was just
                    // taken off it: without this its result went to the
                    // destroyed caller and was dropped.
                    if (below != null && r.caller == replaced) {
                        r.caller = below.activity;
                    }
                }
                // Queued rather than delivered now: resumeRecord restarts the
                // stopped caller (onRestart, onStart) and only then hands it
                // its pending results, before onResume, as Android does.
                if (below != null) {
                    queueResult(r);
                    resumeRecord(below, true);
                }
            }
            destroy(r, false);
            if (wasLast && STACK.isEmpty() && pendingLaunches == 0) {
                AndroidRuntime.getInstance().onLastActivityFinished();
            }
        } else {
            destroy(r, false);
            queueResult(r);
        }
    }

    /// Hands the result of `r`, just finished below the top, to its caller.
    /// The caller need not be on top -- A starts B, B starts C, and A
    /// finishes B with finishActivity -- so the result goes to the caller's
    /// own record, and waits there unless it is resumed.
    private static void queueResult(Record r) {
        Record caller = r.caller == null || r.requestCode < 0 ? null : recordOf(r.caller);
        if (caller == null) {
            return;
        }
        if (caller.resumed) {
            deliverResult(r, caller);
        } else {
            if (caller.pendingResults == null) {
                caller.pendingResults = new ArrayList<Object[]>();
            }
            Activity a = r.activity;
            caller.pendingResults.add(new Object[] {Integer.valueOf(r.requestCode),
                Integer.valueOf(a.mResultCode), snapshotIntent(a.mResultData)});
        }
    }

    private static Intent snapshotIntent(Intent source) {
        if (source == null) {
            return null;
        }
        Intent copy = new Intent(source);
        Bundle extras = source.getExtras();
        if (extras != null) {
            copy.replaceExtras(extras.deepCopy());
        }
        return copy;
    }

    /// Delivers the results queued on `r` while it was not resumed.
    private static void deliverPendingResults(Record r) {
        ArrayList<Object[]> pending = r.pendingResults;
        if (pending == null) {
            return;
        }
        r.pendingResults = null;
        for (Object[] p : pending) {
            Intent data = p[2] instanceof Intent ? (Intent) p[2] : null;
            r.activity.dispatchActivityResult(((Integer) p[0]).intValue(), ((Integer) p[1]).intValue(), data);
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
            if (!isLive(r)) {
                return null;
            }
        }
        if (r.started) {
            stop(r);
            if (!isLive(r)) {
                return null;
            }
        }
        Bundle saved = r.savedState;
        if (saved == null) {
            saved = new Bundle();
            a.onSaveInstanceState(saved);
            if (!isLive(r)) {
                return null;
            }
        }
        int[] requests = a.snapshotFragmentRequests();
        Object nonConfig = a.onRetainNonConfigurationInstance();
        if (!isLive(r)) {
            return null;
        }
        java.util.HashMap<String, ArrayList<Fragment>> retained = a.hostsRetainNonConfig();
        if (!isLive(r)) {
            return null;
        }
        destroy(r, true);
        if (index >= STACK.size() || STACK.get(index) != r || a.mFinished) {
            return null;
        }
        Activity fresh = AndroidRuntime.getInstance().getApp().createActivity(r.info.type);
        fresh.mLastNonConfigurationInstance = nonConfig;
        fresh.mLastRetainedFragments = retained;
        a.mReplacement = fresh;
        Record n = new Record();
        n.activity = fresh;
        n.caller = r.caller;
        n.requestCode = r.requestCode;
        n.noHistory = r.noHistory;
        n.info = r.info;
        n.pendingResults = r.pendingResults;
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
            if (t != null) {
                resumeRecord(t, false);
            }
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
        // Both halves of the key, as on Android: a handler that claims the
        // down and acts in onKeyUp() (an activity's or a focused view's) is
        // otherwise a no-op. A down that called startTracking() gets a
        // tracked up, which is what Activity's default onKeyUp() navigates
        // on. The up is not sent to an activity the down already finished --
        // on Android it would reach the next window, which never saw the
        // down and ignores it.
        long now = android.os.SystemClock.uptimeMillis();
        android.view.KeyEvent down = new android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_DOWN,
                android.view.KeyEvent.KEYCODE_BACK, 0);
        boolean handled = a.dispatchKeyEvent(down);
        if (!a.isFinishing()) {
            boolean tracked = handled && (down.getFlags() & android.view.KeyEvent.FLAG_START_TRACKING) != 0;
            handled |= a.dispatchKeyEvent(new android.view.KeyEvent(now, android.os.SystemClock.uptimeMillis(),
                    android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_BACK, 0, 0, 0, 0,
                    tracked ? android.view.KeyEvent.FLAG_TRACKING : 0));
        }
        if (handled) {
            return;
        }
        a.onBackPressed();
    }

    static void requestPermissions(final Activity a, final String[] permissions, final int requestCode) {
        final String[] requested = permissions.clone();
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                Activity target = currentInstance(a);
                if (target != null) {
                    int[] grants = new int[requested.length];
                    target.onRequestPermissionsResult(requestCode, requested, grants);
                }
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
