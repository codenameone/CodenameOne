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
package android.widget;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.View;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class FormWidgetSavedStateTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    private SparseArray<Parcelable> save(View view) {
        view.setId(42);
        SparseArray<Parcelable> state = new SparseArray<Parcelable>();
        view.saveHierarchyState(state);
        return state;
    }
    @Test public void timePickerRestoresTimeAndClockModeWithoutNotifying() {
        TimePicker source = new TimePicker(AndroidTestSupport.context());
        source.setHour((source.getHour() + 12) % 24);
        source.setMinute(37);
        source.setIs24HourView(true);
        SparseArray<Parcelable> state = save(source);
        TimePicker restored = new TimePicker(AndroidTestSupport.context());
        restored.setId(42);
        final int[] calls = {0};
        restored.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            public void onTimeChanged(TimePicker view, int hour, int minute) { calls[0]++; }
        });
        restored.restoreHierarchyState(state);
        assertEquals(source.getHour(), restored.getHour());
        assertEquals(37, restored.getMinute());
        assertTrue(restored.is24HourView());
        assertEquals(0, calls[0]);
    }
    @Test public void progressBarRestoresBothValues() {
        checkProgress(new ProgressBar(AndroidTestSupport.context(), null, android.R.attr.progressBarStyleHorizontal),
                new ProgressBar(AndroidTestSupport.context(), null, android.R.attr.progressBarStyleHorizontal));
    }
    @Test public void seekBarRestoresBothValues() {
        checkProgress(new SeekBar(AndroidTestSupport.context()), new SeekBar(AndroidTestSupport.context()));
    }
    @Test public void ratingBarRestoresBothValues() {
        checkProgress(new RatingBar(AndroidTestSupport.context()), new RatingBar(AndroidTestSupport.context()));
    }
    private void checkProgress(ProgressBar source, ProgressBar restored) {
        source.setProgress(source.getMax() / 2);
        source.setSecondaryProgress(source.getMax() * 3 / 4);
        SparseArray<Parcelable> state = save(source);
        restored.setId(42);
        restored.restoreHierarchyState(state);
        assertEquals(source.getProgress(), restored.getProgress());
        assertEquals(source.getSecondaryProgress(), restored.getSecondaryProgress());
    }
    private ArrayAdapter<String> adapter() {
        return new ArrayAdapter<String>(AndroidTestSupport.context(), android.R.layout.simple_spinner_item,
                new String[] {"one", "two", "three"});
    }
    @Test public void spinnerRestoresSelectionWithAdapterAlreadyAttached() {
        Spinner source = new Spinner(AndroidTestSupport.context());
        source.setAdapter(adapter());
        source.setSelection(2);
        Spinner restored = new Spinner(AndroidTestSupport.context());
        restored.setAdapter(adapter());
        restored.setId(42);
        restored.restoreHierarchyState(save(source));
        assertEquals(2, restored.getSelectedItemPosition());
        assertEquals("three", restored.getSelectedItem());
    }
    @Test public void spinnerWaitsForAdapterDataBeforeRestoring() {
        Spinner source = new Spinner(AndroidTestSupport.context());
        source.setAdapter(adapter());
        source.setSelection(1);
        Spinner restored = new Spinner(AndroidTestSupport.context());
        restored.setId(42);
        restored.restoreHierarchyState(save(source));
        ArrayAdapter<String> data = new ArrayAdapter<String>(AndroidTestSupport.context(),
                android.R.layout.simple_spinner_item, new java.util.ArrayList<String>());
        restored.setAdapter(data);
        data.setNotifyOnChange(false);
        data.add("one");
        data.add("two");
        data.add("three");
        data.notifyDataSetChanged();
        assertEquals(1, restored.getSelectedItemPosition());
        assertEquals("two", restored.getSelectedItem());
    }
}
