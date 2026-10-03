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
import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import com.codename1.androidcompat.runtime.AndroidRuntime;
import com.codename1.generated.android.AndroidAppImpl;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// One screen of the Android compatibility gallery
/// (scripts/android-compat-samples/gallery), which this app compiles from its
/// Android Studio module unmodified. The test starts the activity the way an
/// Android app would, captures the activity's form once it has settled, and
/// finishes every activity again -- the runtime then returns to this app's own
/// form, as an Android screen embedded in a Codename One app should.
public abstract class AndroidCompatScreenshotTest extends BaseTest {

    private final Class<?> activity;
    private final String imageName;

    protected AndroidCompatScreenshotTest(Class<?> activity, String imageName) {
        this.activity = activity;
        this.imageName = imageName;
    }

    /// The gallery's application context. The runtime is installed on first
    /// use, never launched: this app, not the gallery's launcher activity, owns
    /// the screen.
    protected static Context galleryContext() {
        AndroidRuntime rt = AndroidRuntime.getInstance();
        if (rt == null) {
            rt = AndroidRuntime.install(new AndroidAppImpl());
        }
        return rt.getApplication();
    }

    /// Runs before the activity starts: resets state the screen persists, so
    /// a rerun captures the same pixels.
    protected void beforeStart(Context app) {
    }

    /// Runs once the activity is up: puts the screen in the state to capture.
    protected void prepare(Activity activity) {
    }

    @Override
    public boolean runTest() throws Exception {
        Context app = galleryContext();
        beforeStart(app);
        Intent intent = new Intent(app, activity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        app.startActivity(intent);
        Activity top = ActivityThread.getTopActivity();
        if (top == null) {
            fail("the activity did not start: " + activity.getName());
            done();
            return false;
        }
        // Android focuses the first text field; a blinking caret has no place
        // in a screenshot baseline.
        View focused = top.getCurrentFocus();
        if (focused != null) {
            focused.clearFocus();
        }
        prepare(top);
        // The activity's own form, not Display.getCurrent(): while the show
        // transition runs (the Android theme slides) the current form is still
        // the one being left, and a timer bound to that one never fires.
        final Form form = top.getWindow().getDecorView().getPeer().getComponentForm();
        // Showing the form focused its first text field, whose caret blinks.
        // Drop that focus now, well before the capture: on a device the
        // repaint it triggers reaches the screen a frame later, and a capture
        // taken in the same instant still had the caret.
        form.setFocused(null);
        UITimer.timer(1500, false, form, new Runnable() {
            @Override
            public void run() {
                captureWhenSettled(form, imageName, new Runnable() {
                    @Override
                    public void run() {
                        ActivityThread.finishAllActivities();
                        done();
                    }
                });
            }
        });
        return true;
    }

    /// Hides every indeterminate progress bar under `root`: they animate for
    /// as long as they show, which no screenshot can hold still. Their space
    /// in the layout is kept.
    protected static void hideIndeterminateProgress(View root) {
        if (root instanceof android.widget.ProgressBar && ((android.widget.ProgressBar) root).isIndeterminate()) {
            root.setVisibility(View.INVISIBLE);
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                hideIndeterminateProgress(g.getChildAt(i));
            }
        }
    }

    /// The first list view under `root`, depth first, or null.
    protected static android.widget.ListView findListView(View root) {
        if (root instanceof android.widget.ListView) {
            return (android.widget.ListView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                android.widget.ListView found = findListView(g.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
