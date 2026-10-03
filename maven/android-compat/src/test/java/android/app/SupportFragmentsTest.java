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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentFactory;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.junit.Test;

/// The AndroidX fragments run on the platform fragment engine as a second
/// host of the activity: their back stack is a back callback that runs
/// before the activity's own back handling, they keep their view models
/// across a configuration change, and a restored fragment is created through
/// the application's fragment factory.
public class SupportFragmentsTest {

    /// A support fragment with a fixed public type, so the test's factory can
    /// create it by name.
    public static final class Page extends androidx.fragment.app.Fragment {
    }

    static final class Counter extends ViewModel {
        int value;
        boolean cleared;

        @Override
        protected void onCleared() {
            cleared = true;
        }
    }

    static final ViewModelProvider.Factory COUNTERS = new ViewModelProvider.Factory() {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new Counter();
        }
    };

    static final class HeadlessActivity extends FragmentActivity {
        boolean finished;
        private android.view.LayoutInflater inflater;

        @Override
        public android.view.LayoutInflater getLayoutInflater() {
            if (inflater == null) {
                inflater = new com.codename1.androidcompat.runtime.PhoneLayoutInflater(this);
            }
            return inflater;
        }

        @Override
        public void finish() {
            finished = true;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
        }

        @Override
        protected void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
        }
    }

    private static void resume(Activity a) {
        a.hostsDispatchActivityCreated();
        a.hostsDispatchStart();
        a.hostsDispatchResume();
    }

    @Test
    public void backPopsTheSupportBackStackBeforeFinishing() {
        HeadlessActivity a = new HeadlessActivity();
        a.onCreate(null);
        resume(a);
        androidx.fragment.app.FragmentManager fm = a.getSupportFragmentManager();
        Page first = new Page();
        fm.beginTransaction().add(first, "first").commitNow();
        Page second = new Page();
        fm.beginTransaction().remove(first).add(second, "second").addToBackStack("s").commit();
        fm.executePendingTransactions();
        assertEquals(1, fm.getBackStackEntryCount());
        assertEquals("s", fm.getBackStackEntryAt(0).getName());
        assertTrue(second.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED));

        a.onBackPressed();
        assertFalse("the back stack was popped instead", a.finished);
        assertEquals(0, fm.getBackStackEntryCount());
        assertSame(first, fm.findFragmentByTag("first"));
        assertEquals(Lifecycle.State.DESTROYED, second.getLifecycle().getCurrentState());

        a.onBackPressed();
        assertTrue("an empty back stack finishes", a.finished);
    }

    @Test
    public void viewModelsSurviveRecreationAndClearWhenGone() {
        HeadlessActivity a = new HeadlessActivity();
        a.onCreate(null);
        resume(a);
        Page page = new Page();
        a.getSupportFragmentManager().beginTransaction().add(page, "page").commitNow();
        Counter activityCounter = new ViewModelProvider(a, COUNTERS).get(Counter.class);
        activityCounter.value = 7;
        Counter pageCounter = new ViewModelProvider(page, COUNTERS).get(Counter.class);
        pageCounter.value = 11;

        // Recreate, as for a configuration change.
        Bundle saved = new Bundle();
        Activity base = a;
        base.hostsDispatchPause();
        a.onSaveInstanceState(saved);
        base.hostsDispatchStop();
        Object nonConfig = a.onRetainNonConfigurationInstance();
        base.mChangingConfigurations = true;
        base.hostsDispatchDestroy();
        a.onDestroy();
        assertFalse(activityCounter.cleared);
        assertFalse(pageCounter.cleared);

        HeadlessActivity b = new HeadlessActivity();
        ((Activity) b).mLastNonConfigurationInstance = nonConfig;
        b.getSupportFragmentManager().setFragmentFactory(new FragmentFactory() {
            @Override
            public androidx.fragment.app.Fragment instantiate(ClassLoader classLoader, String className) {
                return new Page();
            }
        });
        b.onCreate(saved);
        resume(b);
        Page restored = (Page) b.getSupportFragmentManager().findFragmentByTag("page");
        assertNotNull("restored through the application's factory", restored);
        assertSame(activityCounter, new ViewModelProvider(b, COUNTERS).get(Counter.class));
        assertSame(pageCounter, new ViewModelProvider(restored, COUNTERS).get(Counter.class));
        assertEquals(11, pageCounter.value);

        // Finished for good: everything is cleared.
        Activity bBase = b;
        bBase.hostsDispatchPause();
        bBase.hostsDispatchStop();
        bBase.hostsDispatchDestroy();
        b.onDestroy();
        assertTrue(activityCounter.cleared);
        assertTrue(pageCounter.cleared);
    }
}
