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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/// A LinearLayout that keeps at most one of its RadioButton children checked.
public class RadioGroup extends LinearLayout {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(RadioGroup group, int checkedId);
    }

    public static class LayoutParams extends LinearLayout.LayoutParams {
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(int w, int h, float initWeight) {
            super(w, h, initWeight);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }
    }

    private int mCheckedId = View.NO_ID;
    private boolean mProtectFromCheckedChange;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private final CompoundButton.OnCheckedChangeListener mChildListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mProtectFromCheckedChange) {
                return;
            }
            if (!isChecked) {
                // A button unchecked from code leaves the group with no
                // selection, so it must not keep reporting that button's id.
                // Android records the id regardless, which leaves
                // getCheckedRadioButtonId() naming an unchecked child.
                if (buttonView.getId() == mCheckedId) {
                    setCheckedId(View.NO_ID);
                }
                return;
            }
            mProtectFromCheckedChange = true;
            try {
                if (mCheckedId != View.NO_ID) {
                    setCheckedStateForView(mCheckedId, false);
                }
            } finally {
                mProtectFromCheckedChange = false;
            }
            setCheckedId(buttonView.getId());
        }
    };

    public RadioGroup(Context context) {
        super(context);
        setOrientation(VERTICAL);
    }

    public RadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.RadioGroup);
        int value = a.getResourceId(android.R.styleable.RadioGroup_checkedButton, View.NO_ID);
        if (value != View.NO_ID) {
            mCheckedId = value;
        }
        int index = a.getInt(android.R.styleable.RadioGroup_orientation, VERTICAL);
        a.recycle();
        setOrientation(index);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child instanceof RadioButton) {
            RadioButton button = (RadioButton) child;
            if (button.getId() == View.NO_ID) {
                button.setId(View.generateViewId());
            }
            button.setOnCheckedChangeWidgetListener(mChildListener);
            if (button.isChecked()) {
                mProtectFromCheckedChange = true;
                try {
                    if (mCheckedId != View.NO_ID && mCheckedId != button.getId()) {
                        setCheckedStateForView(mCheckedId, false);
                    }
                } finally {
                    mProtectFromCheckedChange = false;
                }
                setCheckedId(button.getId());
            } else if (button.getId() == mCheckedId) {
                mProtectFromCheckedChange = true;
                try {
                    button.setChecked(true);
                } finally {
                    mProtectFromCheckedChange = false;
                }
            }
        }
    }

    @Override
    public void onViewRemoved(View child) {
        if (child instanceof RadioButton) {
            ((RadioButton) child).setOnCheckedChangeWidgetListener(null);
        }
        super.onViewRemoved(child);
    }

    public void check(int id) {
        if (id != View.NO_ID && id == mCheckedId) {
            return;
        }
        mProtectFromCheckedChange = true;
        try {
            if (mCheckedId != View.NO_ID) {
                setCheckedStateForView(mCheckedId, false);
            }
            if (id != View.NO_ID) {
                setCheckedStateForView(id, true);
            }
        } finally {
            mProtectFromCheckedChange = false;
        }
        setCheckedId(id);
    }

    private void setCheckedId(int id) {
        boolean changed = id != mCheckedId;
        mCheckedId = id;
        if (changed && mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener.onCheckedChanged(this, mCheckedId);
        }
    }

    private void setCheckedStateForView(int viewId, boolean checked) {
        View checkedView = findViewById(viewId);
        if (checkedView instanceof RadioButton) {
            ((RadioButton) checkedView).setChecked(checked);
        }
    }

    public int getCheckedRadioButtonId() {
        return mCheckedId;
    }

    public void clearCheck() {
        check(View.NO_ID);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new RadioGroup.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
        return new RadioGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return RadioGroup.class.getName();
    }
}
