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
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class FragmentViewStateTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    static final class Host extends Activity {
        final FrameLayout container;
        Host(Context context) {
            attachBaseContext(context);
            container = new FrameLayout(context);
            container.setId(100);
        }
        @Override public <T extends View> T findViewById(int id) { return container.findViewById(id); }
        @Override public LayoutInflater getLayoutInflater() { return LayoutInflater.from(getBaseContext()); }
    }
    static final class FormFragment extends Fragment {
        EditText text;
        CheckBox check;
        String restoredText;
        @Override public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
            LinearLayout layout = new LinearLayout(parent.getContext());
            text = new EditText(parent.getContext());
            text.setId(101);
            check = new CheckBox(parent.getContext());
            check.setId(102);
            layout.addView(text);
            layout.addView(check);
            return layout;
        }
        @Override public void onViewStateRestored(Bundle saved) {
            super.onViewStateRestored(saved);
            restoredText = text.getText().toString();
        }
    }
    @Test public void backStackRestoresInputBeforeViewStateCallback() {
        Host host = new Host(AndroidTestSupport.context());
        FragmentManagerImpl fm = host.mFragments;
        fm.dispatchCreate();
        fm.dispatchActivityCreated();
        fm.dispatchStart();
        fm.dispatchResume();
        try {
            FormFragment first = new FormFragment();
            fm.beginTransaction().add(100, first, "first").commitNow();
            for (int round = 0; round < 2; round++) {
                String value = "draft " + round;
                first.text.setText(value);
                first.check.setChecked(true);
                View originalView = first.getView();
                fm.beginTransaction().replace(100, new FormFragment(), "other").addToBackStack("other").commit();
                fm.executePendingTransactions();
                assertNull(first.getView());
                assertTrue(fm.popBackStackImmediate());
                assertNotSame(originalView, first.getView());
                assertEquals(value, first.text.getText().toString());
                assertEquals(value, first.restoredText);
                assertTrue(first.check.isChecked());
            }
            // A fragment without a live view must also carry that hierarchy
            // through its saved state when its host is recreated.
            fm.beginTransaction().replace(100, new FormFragment(), "other").addToBackStack("saved").commit();
            fm.executePendingTransactions();
            Fragment.SavedState snapshot = fm.saveFragmentInstanceState(first);
            assertTrue(fm.popBackStackImmediate());
            FormFragment restored = new FormFragment();
            restored.setInitialSavedState(snapshot);
            fm.beginTransaction().replace(100, restored, "restored").commitNow();
            assertEquals("draft 1", restored.text.getText().toString());
            assertEquals("draft 1", restored.restoredText);
            assertTrue(restored.check.isChecked());
        } finally { fm.dispatchDestroy(); }
    }
}
