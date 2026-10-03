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
package com.google.android.material;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatViewInflater;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.theme.MaterialComponentsViewInflater;

import org.junit.Test;

/// The Material themes' values and the widgets' state logic, checked against
/// what Material Components does on Android.
public class MaterialComponentsTest {

    private static Context themed(int theme) {
        return new ContextThemeWrapper(AndroidTestSupport.context(), theme);
    }

    private static int color(Context c, int attr) {
        TypedValue tv = new TypedValue();
        assertTrue(c.getTheme().resolveAttribute(attr, tv, true));
        return tv.data;
    }

    @Test
    public void material3LightUsesTheBaselineColorRoles() {
        Context c = themed(R.style.Theme_Material3_Light_NoActionBar);
        assertEquals(0xff6750a4, color(c, R.attr.colorPrimary));
        assertEquals(0xffffffff, color(c, R.attr.colorOnPrimary));
        assertEquals(0xffeaddff, color(c, R.attr.colorPrimaryContainer));
        assertEquals(0xfffef7ff, color(c, R.attr.colorSurface));
        assertEquals(0xff79747e, color(c, R.attr.colorOutline));
        assertEquals(0xffb3261e, color(c, R.attr.colorError));
    }

    @Test
    public void material3DarkAndMaterialComponentsUseTheirOwnBaselines() {
        assertEquals(0xffd0bcff, color(themed(R.style.Theme_Material3_Dark), R.attr.colorPrimary));
        Context mdc = themed(R.style.Theme_MaterialComponents_Light);
        assertEquals(0xff6200ee, color(mdc, R.attr.colorPrimary));
        assertEquals(0xff03dac6, color(mdc, R.attr.colorSecondary));
    }

    @Test
    public void materialThemesInflateMaterialWidgets() {
        assertTrue(AppCompatViewInflater.forTheme(themed(R.style.Theme_Material3_Light))
                instanceof MaterialComponentsViewInflater);
        assertTrue(AppCompatViewInflater.forTheme(themed(R.style.Theme_MaterialComponents_Light_DarkActionBar))
                instanceof MaterialComponentsViewInflater);
        assertFalse(AppCompatViewInflater.forTheme(themed(androidx.appcompat.R.style.Theme_AppCompat_Light))
                instanceof MaterialComponentsViewInflater);
        View b = AppCompatViewInflater.forTheme(themed(R.style.Theme_Material3_Light))
                .createView(null, "Button", themed(R.style.Theme_Material3_Light), null);
        assertTrue(b instanceof MaterialButton);
    }

    @Test
    public void material3ButtonIsAPrimaryPill() {
        Context c = themed(R.style.Theme_Material3_Light);
        MaterialButton b = new MaterialButton(c);
        assertEquals(0xff6750a4, b.getBackgroundTintList().getDefaultColor());
        assertEquals(0xffffffff, b.getCurrentTextColor());
        assertEquals(MaterialAttrs.dpi(c, 20), b.getCornerRadius());
        b.setText("OK");
        b.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        assertEquals(MaterialAttrs.dpi(c, 48), b.getMeasuredHeight());
    }

    @Test
    public void toggleGroupKeepsOneButtonChecked() {
        Context c = themed(R.style.Theme_Material3_Light);
        MaterialButtonToggleGroup g = new MaterialButtonToggleGroup(c);
        g.setSingleSelection(true);
        MaterialButton a = new MaterialButton(c);
        a.setId(View.generateViewId());
        MaterialButton b = new MaterialButton(c);
        b.setId(View.generateViewId());
        g.addView(a);
        g.addView(b);
        g.check(a.getId());
        g.check(b.getId());
        assertFalse(a.isChecked());
        assertTrue(b.isChecked());
        assertEquals(b.getId(), g.getCheckedButtonId());
    }

    @Test
    public void chipGroupSingleSelection() {
        Context c = themed(R.style.Theme_Material3_Light);
        ChipGroup g = new ChipGroup(c);
        g.setSingleSelection(true);
        Chip one = new Chip(c);
        one.setId(View.generateViewId());
        one.setCheckable(true);
        Chip two = new Chip(c);
        two.setId(View.generateViewId());
        two.setCheckable(true);
        g.addView(one);
        g.addView(two);
        one.setChecked(true);
        two.setChecked(true);
        assertFalse(one.isChecked());
        assertEquals(two.getId(), g.getCheckedChipId());
    }

    @Test
    public void sliderSnapsToItsSteps() {
        Slider s = new Slider(themed(R.style.Theme_Material3_Light));
        s.setValueFrom(0);
        s.setValueTo(10);
        s.setStepSize(2);
        s.setValue(3.1f);
        assertEquals(4f, s.getValue(), 0f);
        s.setValue(42f);
        assertEquals(10f, s.getValue(), 0f);
    }

    @Test
    public void textInputLayoutErrorAndPasswordToggle() {
        Context c = themed(R.style.Theme_Material3_Light);
        TextInputLayout til = new TextInputLayout(c);
        TextInputEditText field = new TextInputEditText(c);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        til.addView(field);
        assertSame(field, til.getEditText());
        til.setError("Required");
        assertEquals("Required", til.getError().toString());
        assertTrue(til.isErrorEnabled());
        til.setError(null);
        assertNull(til.getError());
        til.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        til.setHint("Password");
        assertEquals("Password", til.getHint().toString());
    }

    @Test
    public void tabLayoutMediatorBuildsOneTabPerPage() {
        Context c = themed(R.style.Theme_Material3_Light);
        TabLayout tabs = new TabLayout(c);
        ViewPager2 pager = new ViewPager2(c);
        pager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                TextView v = new TextView(parent.getContext());
                v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                return new RecyclerView.ViewHolder(v) {
                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });
        new TabLayoutMediator(tabs, pager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(TabLayout.Tab tab, int position) {
                tab.setText("Page " + position);
            }
        }).attach();
        assertEquals(3, tabs.getTabCount());
        assertEquals("Page 2", tabs.getTabAt(2).getText().toString());
        assertEquals(0, tabs.getSelectedTabPosition());
        tabs.getTabAt(2).select();
        assertEquals(2, pager.getCurrentItem());
    }

    /// The bar checks its menu items while it rebuilds; each check used to
    /// notify the menu's listener, which rebuilt the bar again, until the
    /// stack overflowed on the first menu.
    @Test
    public void bottomNavigationBuildsAndSelectsWithoutRecursing() {
        com.google.android.material.bottomnavigation.BottomNavigationView nav =
                new com.google.android.material.bottomnavigation.BottomNavigationView(
                        themed(com.google.android.material.R.style.Theme_Material3_Light_NoActionBar));
        android.view.Menu menu = nav.getMenu();
        menu.add(0, 11, 0, "Home");
        menu.add(0, 12, 1, "Search");
        menu.add(0, 13, 2, "Profile");
        assertEquals(11, nav.getSelectedItemId());
        nav.setSelectedItemId(13);
        assertEquals(13, nav.getSelectedItemId());
        assertTrue(menu.findItem(13).isChecked());
        assertFalse(menu.findItem(11).isChecked());
    }

    /// A checked Material 3 chip drops its outline; unchecking it must bring
    /// the outline back (it used to stay off for good).
    @Test
    public void uncheckedChipGetsItsOutlineBack() {
        Context c = themed(com.google.android.material.R.style.Theme_Material3_Light_NoActionBar);
        Chip chip = new Chip(c);
        chip.setCheckable(true);
        chip.setChipStrokeWidth(3f);
        com.google.android.material.shape.MaterialShapeDrawable surface =
                (com.google.android.material.shape.MaterialShapeDrawable) chip.getBackground();
        chip.setChecked(true);
        assertEquals(0f, surface.getStrokeWidth(), 0f);
        chip.setChecked(false);
        assertEquals(3f, surface.getStrokeWidth(), 0f);
    }
}
