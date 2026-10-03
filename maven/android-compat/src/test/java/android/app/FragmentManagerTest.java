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

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.codename1.androidcompat.runtime.MenuImpl;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The fragment state machine without a display: headless fragments (no
/// view) through the activity's states, the back stack, retained instances
/// and options-menu merging.
public class FragmentManagerTest {

    static final class Recorder extends Fragment {
        final List<String> log;
        final String name;

        Recorder(List<String> log, String name) {
            this.log = log;
            this.name = name;
        }

        private void add(String s) {
            log.add(name + ":" + s);
        }

        @Override
        public void onAttach(android.content.Context context) {
            super.onAttach(context);
            add("attach");
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            add("create" + (savedInstanceState != null ? "+" + savedInstanceState.getString("k") : ""));
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            add("activityCreated");
        }

        @Override
        public void onStart() {
            super.onStart();
            add("start");
        }

        @Override
        public void onResume() {
            super.onResume();
            add("resume");
        }

        @Override
        public void onPause() {
            super.onPause();
            add("pause");
        }

        @Override
        public void onStop() {
            super.onStop();
            add("stop");
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            add("destroyView");
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            add("destroy");
        }

        @Override
        public void onDetach() {
            super.onDetach();
            add("detach");
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("k", name);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            menu.add(0, name.hashCode(), 0, name);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == name.hashCode()) {
                add("selected");
                return true;
            }
            return false;
        }
    }

    /// An activity with no window: just the inflater fragments ask for.
    static final class HeadlessActivity extends Activity {
        private android.view.LayoutInflater inflater;

        @Override
        public android.view.LayoutInflater getLayoutInflater() {
            if (inflater == null) {
                inflater = new com.codename1.androidcompat.runtime.PhoneLayoutInflater(this);
            }
            return inflater;
        }
    }

    private static Activity resumedActivity() {
        Activity a = new HeadlessActivity();
        a.mFragments.dispatchCreate();
        a.mFragments.dispatchActivityCreated();
        a.mFragments.dispatchStart();
        a.mFragments.dispatchResume();
        return a;
    }

    private static String join(List<String> log) {
        StringBuilder sb = new StringBuilder();
        for (String s : log) {
            sb.append(sb.length() == 0 ? "" : " ").append(s);
        }
        return sb.toString();
    }

    @Test
    public void runsTheLifecycleInAndroidOrder() {
        List<String> log = new ArrayList<String>();
        Activity a = resumedActivity();
        Recorder f = new Recorder(log, "A");
        a.getFragmentManager().beginTransaction().add(f, "a").commit();
        a.getFragmentManager().executePendingTransactions();
        assertEquals("A:attach A:create A:activityCreated A:start A:resume", join(log));
        assertTrue(f.isAdded());
        assertTrue(f.isResumed());
        assertSame(f, a.getFragmentManager().findFragmentByTag("a"));
        assertSame(a, f.getActivity());

        log.clear();
        a.mFragments.dispatchPause();
        a.mFragments.dispatchStop();
        a.mFragments.dispatchDestroy();
        assertEquals("A:pause A:stop A:destroyView A:destroy A:detach", join(log));
        assertFalse(f.isAdded());
        assertNull(f.getActivity());
    }

    @Test
    public void popsTheBackStack() {
        List<String> log = new ArrayList<String>();
        Activity a = resumedActivity();
        FragmentManager fm = a.getFragmentManager();
        final int[] changes = new int[1];
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                changes[0]++;
            }
        });
        Recorder first = new Recorder(log, "A");
        fm.beginTransaction().add(first, "a").commit();
        fm.executePendingTransactions();
        log.clear();
        Recorder second = new Recorder(log, "B");
        fm.beginTransaction().remove(first).add(second, "b").addToBackStack("swap").commit();
        fm.executePendingTransactions();
        // The removed fragment is on the back stack: it loses its view but is
        // neither destroyed nor detached.
        assertEquals("A:pause A:stop A:destroyView B:attach B:create B:activityCreated B:start B:resume", join(log));
        assertEquals(1, fm.getBackStackEntryCount());
        assertEquals("swap", fm.getBackStackEntryAt(0).getName());
        assertTrue(first.isRemoving());
        assertSame(first, fm.findFragmentByTag("a"));

        log.clear();
        assertTrue(fm.popBackStackImmediate());
        assertEquals("B:pause B:stop B:destroyView B:destroy B:detach A:activityCreated A:start A:resume", join(log));
        assertEquals(0, fm.getBackStackEntryCount());
        assertEquals(2, changes[0]);
        assertTrue(first.isResumed());
        assertNull(fm.findFragmentByTag("b"));
        assertFalse(fm.popBackStackImmediate());

        // Back pops the stack before it finishes the activity.
        fm.beginTransaction().hide(first).addToBackStack(null).commit();
        fm.executePendingTransactions();
        assertTrue(first.isHidden());
        a.onBackPressed();
        assertFalse(first.isHidden());
        assertFalse(a.isFinishing());
    }

    @Test
    public void popsByNameInclusive() {
        List<String> log = new ArrayList<String>();
        Activity a = resumedActivity();
        FragmentManager fm = a.getFragmentManager();
        fm.beginTransaction().add(new Recorder(log, "A"), "a").addToBackStack("one").commit();
        fm.beginTransaction().add(new Recorder(log, "B"), "b").addToBackStack("two").commit();
        fm.beginTransaction().add(new Recorder(log, "C"), "c").addToBackStack("three").commit();
        fm.executePendingTransactions();
        assertEquals(3, fm.getBackStackEntryCount());
        assertTrue(fm.popBackStackImmediate("two", 0));
        assertEquals(2, fm.getBackStackEntryCount());
        assertNull(fm.findFragmentByTag("c"));
        assertTrue(fm.popBackStackImmediate("one", FragmentManager.POP_BACK_STACK_INCLUSIVE));
        assertEquals(0, fm.getBackStackEntryCount());
        assertNull(fm.findFragmentByTag("a"));
    }

    @Test
    public void keepsRetainedInstancesAcrossRecreation() {
        List<String> log = new ArrayList<String>();
        Activity a = resumedActivity();
        Recorder f = new Recorder(log, "R");
        f.setRetainInstance(true);
        a.getFragmentManager().beginTransaction().add(f, "r").commit();
        a.getFragmentManager().executePendingTransactions();
        log.clear();

        a.mFragments.dispatchPause();
        Bundle state = a.mFragments.saveAllState();
        ArrayList<Fragment> retained = a.mFragments.retainNonConfig();
        a.mFragments.dispatchStop();
        a.mFragments.dispatchDestroy();
        // Detached, never destroyed.
        assertEquals("R:pause R:stop R:destroyView R:detach", join(log));

        log.clear();
        Activity b = new HeadlessActivity();
        b.mFragments.restoreAllState(state, retained);
        b.mFragments.dispatchCreate();
        b.mFragments.dispatchActivityCreated();
        b.mFragments.dispatchStart();
        b.mFragments.dispatchResume();
        // Attached again, never created again.
        assertEquals("R:attach R:activityCreated R:start R:resume", join(log));
        assertSame(f, b.getFragmentManager().findFragmentByTag("r"));
        assertSame(b, f.getActivity());
    }

    @Test
    public void mergesOptionsMenus() {
        List<String> log = new ArrayList<String>();
        Activity a = resumedActivity();
        Recorder f = new Recorder(log, "M");
        a.getFragmentManager().beginTransaction().add(f, "m").commit();
        a.getFragmentManager().executePendingTransactions();
        MenuImpl menu = new MenuImpl(a);
        assertFalse(a.mFragments.dispatchCreateOptionsMenu(menu, null));
        f.setHasOptionsMenu(true);
        assertTrue(a.mFragments.dispatchCreateOptionsMenu(menu, null));
        assertEquals(1, menu.size());
        log.clear();
        assertTrue(a.mFragments.dispatchOptionsItemSelected(menu.getItem(0)));
        assertEquals("M:selected", join(log));
        a.getFragmentManager().beginTransaction().hide(f).commit();
        a.getFragmentManager().executePendingTransactions();
        assertFalse(a.mFragments.dispatchOptionsItemSelected(menu.getItem(0)));
    }

    @Test
    public void refusesCommitsAfterStateIsSaved() {
        Activity a = resumedActivity();
        a.mFragments.saveAllState();
        try {
            a.getFragmentManager().beginTransaction().add(new Fragment(), "x").commit();
            throw new AssertionError("commit after onSaveInstanceState must throw");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("after onSaveInstanceState"));
        }
        a.getFragmentManager().beginTransaction().add(new Fragment(), "y").commitAllowingStateLoss();
        a.getFragmentManager().executePendingTransactions();
        assertTrue(a.getFragmentManager().findFragmentByTag("y") != null);
    }
}
