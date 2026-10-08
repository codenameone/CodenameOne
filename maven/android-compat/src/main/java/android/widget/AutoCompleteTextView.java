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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/// An editable text view that shows completion suggestions in a drop-down
/// list while the user types. The suggestions come from a filterable adapter;
/// choosing one replaces the text with it.
public class AutoCompleteTextView extends EditText implements Filter.FilterListener {

    public interface Validator {
        boolean isValid(CharSequence text);

        CharSequence fixText(CharSequence invalidText);
    }

    public interface OnDismissListener {
        void onDismiss();
    }

    private ListAdapter mAdapter;
    private Filter mFilter;
    private int mThreshold;
    private final ListPopupWindow mPopup;
    private CharSequence mHintText;
    private TextView mHintView;
    private int mDropDownAnchorId = View.NO_ID;
    private AdapterView.OnItemClickListener mItemClickListener;
    private AdapterView.OnItemSelectedListener mItemSelectedListener;
    private boolean mDropDownDismissedOnCompletion = true;
    private boolean mBlockCompletion;
    private boolean mPopupCanBeUpdated = true;
    private Validator mValidator;
    private OnDismissListener mOnDismissListener;

    public AutoCompleteTextView(Context context) {
        this(context, null);
    }

    public AutoCompleteTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.autoCompleteTextViewStyle);
    }

    public AutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mPopup = new ListPopupWindow(context, attrs, defStyleAttr, defStyleRes);
        mPopup.setSoftInputMode(0x10);
        mPopup.setPromptPosition(ListPopupWindow.POSITION_PROMPT_BELOW);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.AutoCompleteTextView, defStyleAttr,
                defStyleRes);
        mThreshold = a.getInt(android.R.styleable.AutoCompleteTextView_completionThreshold, 2);
        Drawable selector = a.getDrawable(android.R.styleable.AutoCompleteTextView_dropDownSelector);
        if (selector != null) {
            mPopup.setListSelector(selector);
        }
        mDropDownAnchorId = a.getResourceId(android.R.styleable.AutoCompleteTextView_dropDownAnchor, View.NO_ID);
        mPopup.setWidth(a.getLayoutDimension(android.R.styleable.AutoCompleteTextView_dropDownWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mPopup.setHeight(a.getLayoutDimension(android.R.styleable.AutoCompleteTextView_dropDownHeight,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        CharSequence hint = a.getText(android.R.styleable.AutoCompleteTextView_completionHint);
        a.recycle();
        if (mThreshold <= 0) {
            mThreshold = 1;
        }
        mPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                performCompletion(v, position, id);
            }
        });
        mPopup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mItemSelectedListener != null) {
                    mItemSelectedListener.onItemSelected(parent, view, position, id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (mItemSelectedListener != null) {
                    mItemSelectedListener.onNothingSelected(parent);
                }
            }
        });
        mPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (mOnDismissListener != null) {
                    mOnDismissListener.onDismiss();
                }
            }
        });
        setCompletionHint(hint);
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!mBlockCompletion) {
                    refreshAutoCompleteResults();
                }
            }
        });
    }

    // ------------------------------------------------------------ adapter

    public ListAdapter getAdapter() {
        return mAdapter;
    }

    public <T extends ListAdapter & Filterable> void setAdapter(T adapter) {
        mAdapter = adapter;
        mFilter = adapter == null ? null : ((Filterable) adapter).getFilter();
        mPopup.setAdapter(adapter);
    }

    protected Filter getFilter() {
        return mFilter;
    }

    // ------------------------------------------------------------ properties

    public void setCompletionHint(CharSequence hint) {
        mHintText = hint;
        if (hint != null) {
            if (mHintView == null) {
                mHintView = new TextView(getContext());
                int pad = Math.round(16 * getResources().getDisplayMetrics().density);
                mHintView.setPadding(pad, pad / 2, pad, pad / 2);
                mHintView.setTextSize(14);
            }
            mHintView.setText(hint);
            mPopup.setPromptView(mHintView);
        } else {
            mPopup.setPromptView(null);
        }
    }

    public CharSequence getCompletionHint() {
        return mHintText;
    }

    public int getThreshold() {
        return mThreshold;
    }

    public void setThreshold(int threshold) {
        mThreshold = threshold <= 0 ? 1 : threshold;
    }

    public int getDropDownWidth() {
        return mPopup.getWidth();
    }

    public void setDropDownWidth(int width) {
        mPopup.setWidth(width);
    }

    public int getDropDownHeight() {
        return mPopup.getHeight();
    }

    public void setDropDownHeight(int height) {
        mPopup.setHeight(height);
    }

    public int getDropDownAnchor() {
        return mDropDownAnchorId;
    }

    public void setDropDownAnchor(int id) {
        mDropDownAnchorId = id;
        mPopup.setAnchorView(null);
    }

    public Drawable getDropDownBackground() {
        return mPopup.getBackground();
    }

    public void setDropDownBackgroundDrawable(Drawable d) {
        mPopup.setBackgroundDrawable(d);
    }

    public void setDropDownBackgroundResource(int id) {
        mPopup.setBackgroundDrawable(getContext().getDrawable(id));
    }

    public void setDropDownVerticalOffset(int offset) {
        mPopup.setVerticalOffset(offset);
    }

    public int getDropDownVerticalOffset() {
        return mPopup.getVerticalOffset();
    }

    public void setDropDownHorizontalOffset(int offset) {
        mPopup.setHorizontalOffset(offset);
    }

    public int getDropDownHorizontalOffset() {
        return mPopup.getHorizontalOffset();
    }

    public void setDropDownAnimationStyle(int animationStyle) {
        mPopup.setAnimationStyle(animationStyle);
    }

    public int getDropDownAnimationStyle() {
        return mPopup.getAnimationStyle();
    }

    public boolean isDropDownAlwaysVisible() {
        return mPopup.isDropDownAlwaysVisible();
    }

    public void setDropDownAlwaysVisible(boolean dropDownAlwaysVisible) {
        mPopup.setDropDownAlwaysVisible(dropDownAlwaysVisible);
    }

    public boolean isDropDownDismissedOnCompletion() {
        return mDropDownDismissedOnCompletion;
    }

    public void setDropDownDismissedOnCompletion(boolean dropDownDismissedOnCompletion) {
        mDropDownDismissedOnCompletion = dropDownDismissedOnCompletion;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener l) {
        mItemClickListener = l;
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener l) {
        mItemSelectedListener = l;
    }

    public AdapterView.OnItemClickListener getOnItemClickListener() {
        return mItemClickListener;
    }

    public AdapterView.OnItemSelectedListener getOnItemSelectedListener() {
        return mItemSelectedListener;
    }

    public void setOnDismissListener(OnDismissListener dismissListener) {
        mOnDismissListener = dismissListener;
    }

    public void setValidator(Validator validator) {
        mValidator = validator;
    }

    public Validator getValidator() {
        return mValidator;
    }

    /// Replaces the text with the validator's fix when it is not valid.
    public void performValidation() {
        if (mValidator == null) {
            return;
        }
        CharSequence text = getText();
        if (text.length() > 0 && !mValidator.isValid(text)) {
            setText(mValidator.fixText(text));
        }
    }

    // ------------------------------------------------------------ filtering

    public boolean enoughToFilter() {
        return getText().length() >= mThreshold;
    }

    public void refreshAutoCompleteResults() {
        if (enoughToFilter()) {
            if (mFilter != null) {
                mPopupCanBeUpdated = true;
                performFiltering(getText(), 0);
            }
        } else {
            if (!mPopup.isDropDownAlwaysVisible()) {
                dismissDropDown();
            }
            if (mFilter != null) {
                mFilter.filter(null);
            }
        }
    }

    protected void performFiltering(CharSequence text, int keyCode) {
        mFilter.filter(text, this);
    }

    @Override
    public void onFilterComplete(int count) {
        boolean alwaysVisible = mPopup.isDropDownAlwaysVisible();
        boolean enough = enoughToFilter();
        if ((count > 0 || alwaysVisible) && enough) {
            if (isFocused() && mPopupCanBeUpdated) {
                showDropDown();
            }
        } else if (!alwaysVisible && isPopupShowing()) {
            dismissDropDown();
            mPopupCanBeUpdated = true;
        }
    }

    protected CharSequence convertSelectionToString(Object selectedItem) {
        return mFilter == null ? String.valueOf(selectedItem) : mFilter.convertResultToString(selectedItem);
    }

    // ------------------------------------------------------------ completion

    public void setText(CharSequence text, boolean filter) {
        if (filter) {
            setText(text);
        } else {
            mBlockCompletion = true;
            try {
                setText(text);
            } finally {
                mBlockCompletion = false;
            }
        }
    }

    protected void replaceText(CharSequence text) {
        setText(text);
        setSelection(getText().length());
    }

    public void performCompletion() {
        performCompletion(null, -1, -1);
    }

    private void performCompletion(View selectedView, int position, long id) {
        if (isPopupShowing()) {
            Object selectedItem;
            if (position < 0) {
                selectedItem = mPopup.getSelectedItem();
            } else {
                selectedItem = mAdapter.getItem(position);
            }
            if (selectedItem == null) {
                return;
            }
            mBlockCompletion = true;
            try {
                replaceText(convertSelectionToString(selectedItem));
            } finally {
                mBlockCompletion = false;
            }
            if (mItemClickListener != null) {
                View v = selectedView;
                int pos = position;
                long rowId = id;
                if (v == null || pos < 0) {
                    v = mPopup.getSelectedView();
                    pos = mPopup.getSelectedItemPosition();
                    rowId = mPopup.getSelectedItemId();
                }
                mItemClickListener.onItemClick(mPopup.getListView(), v, pos, rowId);
            }
        }
        if (mDropDownDismissedOnCompletion && !mPopup.isDropDownAlwaysVisible()) {
            dismissDropDown();
        }
    }

    public boolean isPerformingCompletion() {
        return mBlockCompletion;
    }

    public void clearListSelection() {
        mPopup.clearListSelection();
    }

    public void setListSelection(int position) {
        mPopup.setSelection(position);
    }

    public int getListSelection() {
        return mPopup.getSelectedItemPosition();
    }

    // ------------------------------------------------------------ popup

    public boolean isPopupShowing() {
        return mPopup.isShowing();
    }

    public void showDropDown() {
        View anchor = null;
        if (mDropDownAnchorId != View.NO_ID) {
            anchor = getRootView().findViewById(mDropDownAnchorId);
        }
        mPopup.setAnchorView(anchor != null ? anchor : this);
        mPopup.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NEEDED);
        mPopup.show();
    }

    public void dismissDropDown() {
        mPopup.dismiss();
        mPopupCanBeUpdated = false;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (!focused) {
            performValidation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        dismissDropDown();
        super.onDetachedFromWindow();
    }

    public CharSequence getAccessibilityClassName() {
        return AutoCompleteTextView.class.getName();
    }
}
