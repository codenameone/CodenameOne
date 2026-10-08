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
package com.codename1.androidcompat.testing;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.codename1.androidcompat.runtime.AndroidApp;
import com.codename1.androidcompat.runtime.AndroidRuntime;

/// The Android runtime started on [HeadlessImplementation] with an
/// application that has no resources of its own, so tests can construct,
/// measure and lay out framework and library views.
public final class AndroidTestSupport {

    private AndroidTestSupport() {
    }

    /// An empty activity the test application declares, for tests that run
    /// the activity stack.
    public static final class TestActivity extends Activity {
        public interface ResultHandler {
            void onResult(TestActivity activity, int requestCode);
        }

        public static ResultHandler resultHandler;

        /// Fills the options menu of the next activities started, or null.
        public static OptionsMenu optionsMenu;

        /// Handles the key events of the next activities started instead of
        /// the platform's defaults, or null.
        public static KeyHandler keys;

        /// The request code of the last result delivered, or -1.
        public int resultRequestCode = -1;

        /// The result code of the last result delivered.
        public int resultCode;
        public android.content.Intent resultData;
        public int permissionResults;
        public String[] lastPermissions;
        public int[] lastPermissionGrants;
        public int xmlClickCount;

        public void xmlClick(View view) {
            xmlClickCount++;
        }

        /// How many `onStart` calls preceded the last result delivered, or -1.
        public int startsBeforeResult = -1;

        /// The start and restore callbacks this instance received, in order.
        public final java.util.List<String> calls = new java.util.ArrayList<String>();

        /// What `isFinishing()` answered in `onDestroy`, or null before it.
        public Boolean finishingWhenDestroyed;

        /// Builds the content view of the next activities created, or null.
        public static Content content;

        @Override
        protected void onCreate(android.os.Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Content c = content;
            if (c != null) {
                setContentView(c.create(this));
            }
        }

        @Override
        protected void onDestroy() {
            finishingWhenDestroyed = Boolean.valueOf(isFinishing());
            super.onDestroy();
        }

        @Override
        protected void onStart() {
            super.onStart();
            calls.add("start");
        }

        @Override
        protected void onRestoreInstanceState(android.os.Bundle savedInstanceState) {
            super.onRestoreInstanceState(savedInstanceState);
            calls.add("restore");
        }

        @Override
        protected void onPostCreate(android.os.Bundle savedInstanceState) {
            super.onPostCreate(savedInstanceState);
            calls.add("postCreate");
        }

        @Override
        public boolean onCreateOptionsMenu(android.view.Menu menu) {
            if (optionsMenu != null) {
                optionsMenu.fill(menu);
            }
            return true;
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
            startsBeforeResult = 0;
            for (String c : calls) {
                if ("start".equals(c)) {
                    startsBeforeResult++;
                }
            }
            resultRequestCode = requestCode;
            this.resultCode = resultCode;
            this.resultData = data;
            if (resultHandler != null) {
                resultHandler.onResult(this, requestCode);
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
            permissionResults++;
            lastPermissions = permissions;
            lastPermissionGrants = grants;
        }

        @Override
        public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
            KeyHandler k = keys;
            return k != null ? k.onKey(this, event) : super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
            KeyHandler k = keys;
            return k != null ? k.onKey(this, event) : super.onKeyUp(keyCode, event);
        }

        /// The platform's own `onKeyDown`, for a [KeyHandler] that defers to it.
        public boolean defaultKeyDown(android.view.KeyEvent event) {
            return super.onKeyDown(event.getKeyCode(), event);
        }

        /// The platform's own `onKeyUp`, for a [KeyHandler] that defers to it.
        public boolean defaultKeyUp(android.view.KeyEvent event) {
            return super.onKeyUp(event.getKeyCode(), event);
        }
    }

    /// Handles a test activity's key events.
    public interface KeyHandler {
        boolean onKey(Activity activity, android.view.KeyEvent event);
    }

    /// An activity that declares `android:configChanges="uiMode"`, so a dark
    /// mode change reaches it through `onConfigurationChanged` instead of
    /// recreating it.
    public static final class UiModeHandlingActivity extends Activity {
    }

    /// An activity declared `android:launchMode="singleTop"`.
    public static final class SingleTopActivity extends Activity {
        /// How many intents `onNewIntent` delivered to this instance.
        public int newIntents;

        @Override
        protected void onNewIntent(android.content.Intent intent) {
            super.onNewIntent(intent);
            newIntents++;
        }
    }

    /// An activity declared `android:launchMode="singleTask"`.
    public static final class SingleTaskActivity extends Activity {
        /// How many intents `onNewIntent` delivered to this instance.
        public int newIntents;

        @Override
        protected void onNewIntent(android.content.Intent intent) {
            super.onNewIntent(intent);
            newIntents++;
        }
    }

    /// An activity declared `android:noHistory="true"`.
    public static final class NoHistoryActivity extends Activity {
    }

    /// An activity whose manifest parent (`android:parentActivityName`) is
    /// [TestActivity].
    public static final class ChildActivity extends Activity {
    }

    /// An AppCompat activity whose manifest parent is [TestActivity].
    public static final class CompatChildActivity extends androidx.appcompat.app.AppCompatActivity {
    }

    /// A `ComponentActivity` that registers a `GetContent` launcher in
    /// `onCreate`, as an application does, and keeps what it receives.
    public static final class GalleryActivity extends androidx.activity.ComponentActivity {
        /// The launcher this instance registered.
        public androidx.activity.result.ActivityResultLauncher<String> launcher;

        /// The results this instance's callback received.
        public final java.util.List<android.net.Uri> results = new java.util.ArrayList<android.net.Uri>();
        public androidx.activity.result.ActivityResultLauncher<String> immediateLauncher;
        public final java.util.List<String> immediateResults = new java.util.ArrayList<String>();

        @Override
        protected void onCreate(android.os.Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            launcher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                    new androidx.activity.result.ActivityResultCallback<android.net.Uri>() {
                        @Override
                        public void onActivityResult(android.net.Uri result) {
                            results.add(result);
                        }
                    });
            immediateLauncher = registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContract<String, String>() {
                        @Override public android.content.Intent createIntent(Context context, String input) {
                            throw new AssertionError("synchronous contract does not create an intent");
                        }
                        @Override public String parseResult(int resultCode, android.content.Intent intent) {
                            throw new AssertionError("synchronous contract does not parse a result");
                        }
                        @Override public SynchronousResult<String> getSynchronousResult(Context context, String input) {
                            return new SynchronousResult<String>(input);
                        }
                    }, new androidx.activity.result.ActivityResultCallback<String>() {
                        @Override public void onActivityResult(String result) {
                            immediateResults.add(result);
                        }
                    });
        }
    }

    /// Builds a test activity's content view.
    public interface Content {
        View create(Activity activity);
    }

    /// Builds a test activity's options menu.
    public interface OptionsMenu {
        void fill(android.view.Menu menu);
    }

    /// Finishes at a launch callback selected by an intent extra.
    public static final class FinishingActivity extends Activity {
        public static FinishingActivity last;
        public final java.util.List<String> events = new java.util.ArrayList<String>();
        private void event(String name) {
            events.add(name);
            if (name.equals(getIntent().getStringExtra("finishAt"))) {
                finish();
            }
        }
        @Override protected void onCreate(android.os.Bundle state) {
            super.onCreate(state);
            last = this;
            event("create");
        }
        @Override protected void onStart() { super.onStart(); event("start"); }
        @Override protected void onResume() { super.onResume(); event("resume"); }
        @Override protected void onPostResume() { super.onPostResume(); event("postResume"); }
        @Override protected void onPause() {
            super.onPause();
            if ("pause".equals(getIntent().getStringExtra("finishAt"))) {
                event("pause");
            }
        }
        @Override protected void onSaveInstanceState(android.os.Bundle state) {
            super.onSaveInstanceState(state);
            if (stopScenario()) event("save");
        }
        @Override protected void onStop() {
            super.onStop();
            if (stopScenario()) event("stop");
        }
        private boolean stopScenario() {
            String at = getIntent().getStringExtra("finishAt");
            return at != null && (at.startsWith("save") || at.startsWith("stop"));
        }
        @Override protected void onDestroy() { super.onDestroy(); event("destroy"); }
    }

    private static final class TestApp extends AndroidApp {
        TestApp() {
            super(null, "com.codename1.androidcompat.test");
            activity(FinishingActivity.class, FinishingActivity.class.getName(),
                    android.R.style.Theme_Material_Light_DarkActionBar, 0, "Finishing", null, null, false);
            activity(TestActivity.class, TestActivity.class.getName(),
                    android.R.style.Theme_Material_Light_DarkActionBar, 0, "Test", null, null, false);
            android.content.IntentFilter chooserFilter = intentFilter(TestActivity.class);
            chooserFilter.addAction("com.codename1.androidcompat.test.CHOOSER_TARGET");
            chooserFilter.addCategory(android.content.Intent.CATEGORY_DEFAULT);
            activity(UiModeHandlingActivity.class, UiModeHandlingActivity.class.getName(),
                    android.R.style.Theme_Material_Light_DarkActionBar, 0, "UiMode", null, null, false);
            configChanges(UiModeHandlingActivity.class, android.content.pm.ActivityInfo.CONFIG_UI_MODE);
            activity(SingleTopActivity.class, SingleTopActivity.class.getName(),
                    android.R.style.Theme_Material_Light_DarkActionBar, 0, "SingleTop", null, null, false);
            launchMode(SingleTopActivity.class, android.content.pm.ActivityInfo.LAUNCH_SINGLE_TOP);
            activity(SingleTaskActivity.class, SingleTaskActivity.class.getName(),
                    android.R.style.Theme_Material_Light_DarkActionBar, 0, "SingleTask", null, null, false);
            launchMode(SingleTaskActivity.class, android.content.pm.ActivityInfo.LAUNCH_SINGLE_TASK);
            activity(NoHistoryActivity.class, NoHistoryActivity.class.getName(),
                    android.R.style.Theme_Material_Light_DarkActionBar, 0, "NoHistory", null, null, false);
            noHistory(NoHistoryActivity.class);
            activity(ChildActivity.class, ChildActivity.class.getName(),
                    android.R.style.Theme_Material_Light_DarkActionBar, 0, "Child", null, null, false);
            parentActivity(ChildActivity.class, TestActivity.class.getName());
            activity(CompatChildActivity.class, CompatChildActivity.class.getName(), 0, 0, "CompatChild", null,
                    null, false);
            parentActivity(CompatChildActivity.class, TestActivity.class.getName());
            activity(GalleryActivity.class, GalleryActivity.class.getName(),
                    android.R.style.Theme_Material_Light_DarkActionBar, 0, "Gallery", null, null, false);
        }

        @Override
        public String[] viewTags() {
            return new String[0];
        }

        @Override
        public View createView(int index, Context context, AttributeSet attrs) {
            return null;
        }

        @Override
        public Activity createActivity(Class<?> type) {
            if (type == FinishingActivity.class) {
                return new FinishingActivity();
            }
            if (type == TestActivity.class) {
                return new TestActivity();
            }
            if (type == SingleTopActivity.class) {
                return new SingleTopActivity();
            }
            if (type == SingleTaskActivity.class) {
                return new SingleTaskActivity();
            }
            if (type == NoHistoryActivity.class) {
                return new NoHistoryActivity();
            }
            if (type == GalleryActivity.class) {
                return new GalleryActivity();
            }
            if (type == ChildActivity.class) {
                return new ChildActivity();
            }
            if (type == CompatChildActivity.class) {
                return new CompatChildActivity();
            }
            return type == UiModeHandlingActivity.class ? new UiModeHandlingActivity() : null;
        }

        @Override
        public Application createApplication() {
            return new Application();
        }

        @Override
        public boolean dispatchOnClick(Object target, String method, View view) {
            if (target instanceof TestActivity && "xmlClick".equals(method)) {
                ((TestActivity) target).xmlClick(view);
                return true;
            }
            return false;
        }
    }

    /// The application's base context, starting everything on first use.
    public static synchronized Context context() {
        HeadlessImplementation.install();
        AndroidRuntime rt = AndroidRuntime.getInstance();
        if (rt == null) {
            rt = AndroidRuntime.install(new TestApp());
        }
        return rt.getBaseContext();
    }

    /// Puts the display in a known state for a test that runs activities: no
    /// activity, no dialog, a plain form current and no transition running.
    /// The display is shared by every test in the JVM, and what one leaves
    /// behind -- a transition still sliding, Codename One's error dialog --
    /// would otherwise become the next one's host form.
    public static void cleanDisplay() {
        context();
        final com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
        d.callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                android.app.ActivityThread.finishAllActivities();
                com.codename1.ui.Form current = d.getCurrent();
                if (current instanceof com.codename1.ui.Dialog) {
                    ((com.codename1.ui.Dialog) current).dispose();
                }
                com.codename1.ui.Form blank = new com.codename1.ui.Form("Test");
                blank.setTransitionInAnimator(null);
                blank.setTransitionOutAnimator(null);
                blank.show();
            }
        });
        long deadline = System.currentTimeMillis() + 10000;
        final boolean[] busy = {true};
        while (busy[0] && System.currentTimeMillis() < deadline) {
            d.callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                    busy[0] = d.isInTransition();
                }
            });
            if (busy[0]) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
