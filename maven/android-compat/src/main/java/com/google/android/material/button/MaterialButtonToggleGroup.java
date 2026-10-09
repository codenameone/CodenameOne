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
package com.google.android.material.button;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.R;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A row of [MaterialButton]s acting as one control: the buttons become
/// checkable, adjoin (only the outer corners stay rounded, and neighbouring
/// strokes overlap), and with `singleSelection` checking one unchecks the
/// rest.
public class MaterialButtonToggleGroup extends LinearLayout {

    /// Called when a button in the group is checked or unchecked.
    public interface OnButtonCheckedListener {
        void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked);
    }

    private final List<OnButtonCheckedListener> mListeners = new ArrayList<OnButtonCheckedListener>();
    private final Map<MaterialButton, ShapeAppearanceModel> mOriginalShapes =
            new HashMap<MaterialButton, ShapeAppearanceModel>();
    private boolean mSingleSelection;
    private boolean mSelectionRequired;
    private int mDefaultCheckId = View.NO_ID;
    private boolean mSkipChecks;

    public MaterialButtonToggleGroup(Context context) {
        this(context, null);
    }

    public MaterialButtonToggleGroup(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.materialButtonToggleGroupStyle);
    }

    public MaterialButtonToggleGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialButtonToggleGroup, defStyleAttr, 0);
        mSingleSelection = a.getBoolean(R.styleable.MaterialButtonToggleGroup_singleSelection, false);
        mSelectionRequired = a.getBoolean(R.styleable.MaterialButtonToggleGroup_selectionRequired, false);
        mDefaultCheckId = a.getResourceId(R.styleable.MaterialButtonToggleGroup_checkedButton, View.NO_ID);
        a.recycle();
        setOrientation(HORIZONTAL);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!(child instanceof MaterialButton)) {
            super.addView(child, index, params);
            return;
        }
        final MaterialButton button = (MaterialButton) child;
        if (button.getId() == View.NO_ID) {
            button.setId(View.generateViewId());
        }
        button.setCheckable(true);
        button.setGroupListener(new MaterialButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(MaterialButton b, boolean isChecked) {
                onButtonCheckedChanged(b, isChecked);
            }
        });
        mOriginalShapes.put(button, button.getShapeAppearanceModel());
        super.addView(child, index, params);
        if (button.getId() == mDefaultCheckId) {
            button.setChecked(true);
        }
        updateChildShapes();
    }

    @Override
    public void removeView(View view) {
        if (view instanceof MaterialButton) {
            MaterialButton b = (MaterialButton) view;
            b.setGroupListener(null);
            ShapeAppearanceModel original = mOriginalShapes.remove(b);
            if (original != null) {
                b.setShapeAppearanceModel(original);
            }
        }
        super.removeView(view);
        updateChildShapes();
    }

    private void onButtonCheckedChanged(MaterialButton button, boolean isChecked) {
        if (mSkipChecks) {
            return;
        }
        if (!isChecked && mSelectionRequired && getCheckedButtonIds().isEmpty()) {
            mSkipChecks = true;
            button.setChecked(true);
            mSkipChecks = false;
            return;
        }
        if (isChecked && mSingleSelection) {
            mSkipChecks = true;
            for (int i = 0; i < getChildCount(); i++) {
                View c = getChildAt(i);
                if (c != button && c instanceof MaterialButton && ((MaterialButton) c).isChecked()) {
                    ((MaterialButton) c).setChecked(false);
                    dispatch(c.getId(), false);
                }
            }
            mSkipChecks = false;
        }
        dispatch(button.getId(), isChecked);
    }

    private void dispatch(int id, boolean checked) {
        for (OnButtonCheckedListener l : new ArrayList<OnButtonCheckedListener>(mListeners)) {
            l.onButtonChecked(this, id, checked);
        }
    }

    /// The outer corners keep the buttons' own shape; the inner ones are
    /// square, and every button after the first overlaps the previous one's
    /// stroke.
    private void updateChildShapes() {
        List<MaterialButton> visible = new ArrayList<MaterialButton>();
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c instanceof MaterialButton && c.getVisibility() != View.GONE) {
                visible.add((MaterialButton) c);
            }
        }
        RectF probe = new RectF(0, 0, 1000, 1000);
        for (int i = 0; i < visible.size(); i++) {
            MaterialButton b = visible.get(i);
            ShapeAppearanceModel original = mOriginalShapes.get(b);
            if (original == null) {
                continue;
            }
            boolean first = i == 0;
            boolean last = i == visible.size() - 1;
            ShapeAppearanceModel.Builder sb = original.toBuilder();
            if (!first) {
                sb.setTopLeftCornerSize(0).setBottomLeftCornerSize(0);
            } else {
                sb.setTopLeftCornerSize(original.getTopLeftCornerSize(probe))
                        .setBottomLeftCornerSize(original.getBottomLeftCornerSize(probe));
            }
            if (!last) {
                sb.setTopRightCornerSize(0).setBottomRightCornerSize(0);
            } else {
                sb.setTopRightCornerSize(original.getTopRightCornerSize(probe))
                        .setBottomRightCornerSize(original.getBottomRightCornerSize(probe));
            }
            b.setShapeAppearanceModel(sb.build());
            ViewGroup.LayoutParams lp = b.getLayoutParams();
            if (lp instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) lp).leftMargin = first ? 0 : -b.getStrokeWidth();
            }
        }
    }

    // ------------------------------------------------------------ checking

    public void check(int id) {
        View v = findViewById(id);
        if (v instanceof MaterialButton) {
            ((MaterialButton) v).setChecked(true);
        }
    }

    public void uncheck(int id) {
        View v = findViewById(id);
        if (v instanceof MaterialButton) {
            ((MaterialButton) v).setChecked(false);
        }
    }

    public void clearChecked() {
        mSkipChecks = true;
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c instanceof MaterialButton && ((MaterialButton) c).isChecked()) {
                ((MaterialButton) c).setChecked(false);
                dispatch(c.getId(), false);
            }
        }
        mSkipChecks = false;
    }

    /// The checked button's id under single selection, else `View.NO_ID`.
    public int getCheckedButtonId() {
        if (!mSingleSelection) {
            return View.NO_ID;
        }
        List<Integer> ids = getCheckedButtonIds();
        return ids.isEmpty() ? View.NO_ID : ids.get(0).intValue();
    }

    public List<Integer> getCheckedButtonIds() {
        List<Integer> ids = new ArrayList<Integer>();
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c instanceof MaterialButton && ((MaterialButton) c).isChecked()) {
                ids.add(Integer.valueOf(c.getId()));
            }
        }
        return ids;
    }

    public void addOnButtonCheckedListener(OnButtonCheckedListener listener) {
        mListeners.add(listener);
    }

    public void removeOnButtonCheckedListener(OnButtonCheckedListener listener) {
        mListeners.remove(listener);
    }

    public void clearOnButtonCheckedListeners() {
        mListeners.clear();
    }

    public boolean isSingleSelection() {
        return mSingleSelection;
    }

    public void setSingleSelection(boolean singleSelection) {
        mSingleSelection = singleSelection;
        clearChecked();
    }

    public void setSingleSelection(int id) {
        setSingleSelection(getResources().getBoolean(id));
    }

    public boolean isSelectionRequired() {
        return mSelectionRequired;
    }

    public void setSelectionRequired(boolean selectionRequired) {
        mSelectionRequired = selectionRequired;
    }
}
