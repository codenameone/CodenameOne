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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

/// A drop-down picker. Shows the selected item through the adapter's
/// `getView`; tapping opens the items' `getDropDownView`s in a popup anchored
/// to the spinner, or in a dialog with `android:spinnerMode="dialog"`.
public class Spinner extends AbsSpinner implements DialogInterface.OnClickListener {

    public static final int MODE_DIALOG = 0;
    public static final int MODE_DROPDOWN = 1;
    private static final int MAX_ITEMS_MEASURED = 15;

    private int mMode = MODE_DROPDOWN;
    private CharSequence mPrompt;
    private int mDropDownWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
    private Drawable mPopupBackground;
    private int mGravity = Gravity.START | Gravity.CENTER_VERTICAL;
    private int mDropDownHorizontalOffset;
    private int mDropDownVerticalOffset;
    private ListPopupWindow mDropDown;
    private final AttributeSet mPopupAttrs;
    private final int mPopupDefStyleAttr;
    private final int mPopupDefStyleRes;
    private final Rect mTempRect = new Rect();
    private AlertDialog mDialog;
    private int mShownPosition = INVALID_POSITION;

    public Spinner(Context context) {
        this(context, (AttributeSet) null);
    }

    public Spinner(Context context, int mode) {
        this(context, null, android.R.attr.spinnerStyle, mode);
    }

    public Spinner(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.spinnerStyle);
    }

    public Spinner(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, -1);
    }

    public Spinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
        this(context, attrs, defStyleAttr, 0, mode);
    }

    public Spinner(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, int mode) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mPopupAttrs = attrs;
        mPopupDefStyleAttr = defStyleAttr;
        mPopupDefStyleRes = defStyleRes;
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.Spinner, defStyleAttr, defStyleRes);
        mMode = mode >= 0 ? mode : a.getInt(android.R.styleable.Spinner_spinnerMode, MODE_DROPDOWN);
        mPrompt = a.getText(android.R.styleable.Spinner_prompt);
        mDropDownWidth = a.getLayoutDimension(android.R.styleable.Spinner_dropDownWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupBackground = a.getDrawable(android.R.styleable.Spinner_popupBackground);
        mGravity = a.getInt(android.R.styleable.Spinner_gravity, mGravity);
        mDropDownHorizontalOffset = a.getDimensionPixelOffset(android.R.styleable.Spinner_dropDownHorizontalOffset, 0);
        mDropDownVerticalOffset = a.getDimensionPixelOffset(android.R.styleable.Spinner_dropDownVerticalOffset, 0);
        a.recycle();
        setClickable(true);
        setFocusable(true);
    }

    public int getMode() {
        return mMode;
    }

    public void setPrompt(CharSequence prompt) {
        mPrompt = prompt;
    }

    public void setPromptId(int promptId) {
        setPrompt(getContext().getText(promptId));
    }

    public CharSequence getPrompt() {
        return mPrompt;
    }

    public void setDropDownWidth(int pixels) {
        mDropDownWidth = pixels;
    }

    public int getDropDownWidth() {
        return mDropDownWidth;
    }

    public void setPopupBackgroundDrawable(Drawable background) {
        mPopupBackground = background;
    }

    public void setPopupBackgroundResource(int resId) {
        setPopupBackgroundDrawable(getContext().getDrawable(resId));
    }

    public Drawable getPopupBackground() {
        return mPopupBackground;
    }

    public void setDropDownHorizontalOffset(int pixels) {
        mDropDownHorizontalOffset = pixels;
    }

    public void setDropDownVerticalOffset(int pixels) {
        mDropDownVerticalOffset = pixels;
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == 0) {
                gravity |= Gravity.START;
            }
            mGravity = gravity;
            requestLayout();
        }
    }

    public int getGravity() {
        return mGravity;
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener l) {
        throw new RuntimeException("setOnItemClickListener cannot be used with a spinner.");
    }

    // ------------------------------------------------------------ measure & layout

    private View selectedView(View recycled) {
        if (mAdapter == null || mSelectedPosition < 0 || mSelectedPosition >= mItemCount) {
            return null;
        }
        View v = mAdapter.getView(mSelectedPosition, recycled, this);
        if (v.getLayoutParams() == null) {
            v.setLayoutParams(generateDefaultLayoutParams());
        }
        return v;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int padH = getPaddingLeft() + getPaddingRight();
        int padV = getPaddingTop() + getPaddingBottom();
        int preferredWidth = 0;
        int preferredHeight = 0;
        View current = getChildCount() > 0 ? getChildAt(0) : null;
        View view = current != null && !mDataChanged && mShownPosition == mSelectedPosition
                ? current : selectedView(current == null ? null : current);
        if (view != null) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            view.measure(getChildMeasureSpec(widthMeasureSpec, padH, lp.width),
                    getChildMeasureSpec(heightMeasureSpec, padV, lp.height));
            preferredWidth = view.getMeasuredWidth() + padH;
            preferredHeight = view.getMeasuredHeight() + padV;
        }
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            preferredWidth = Math.max(preferredWidth, measureContentWidth(null) + padH);
        }
        preferredWidth = Math.max(preferredWidth, getSuggestedMinimumWidth());
        preferredHeight = Math.max(preferredHeight, getSuggestedMinimumHeight());
        setMeasuredDimension(resolveSizeAndState(preferredWidth, widthMeasureSpec, 0),
                resolveSizeAndState(preferredHeight, heightMeasureSpec, 0));
    }

    /// The widest of the first items, measured unconstrained: the
    /// collapsed views for the spinner itself, the drop-down views (plus the
    /// popup background's padding) for the drop-down.
    int measureContentWidth(Drawable background) {
        return measureContentWidth(background, false);
    }

    private int measureContentWidth(Drawable background, boolean dropDown) {
        if (mAdapter == null) {
            return 0;
        }
        int width = 0;
        View itemView = null;
        int itemType = 0;
        int start = Math.max(0, mSelectedPosition);
        int end = Math.min(mAdapter.getCount(), start + MAX_ITEMS_MEASURED);
        start = Math.max(0, start - (MAX_ITEMS_MEASURED - (end - start)));
        int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        for (int i = start; i < end; i++) {
            int positionType = mAdapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }
            itemView = dropDown ? mAdapter.getDropDownView(i, itemView, this) : mAdapter.getView(i, itemView, this);
            if (itemView.getLayoutParams() == null) {
                itemView.setLayoutParams(generateDefaultLayoutParams());
            }
            itemView.measure(spec, spec);
            width = Math.max(width, itemView.getMeasuredWidth());
        }
        if (background != null) {
            background.getPadding(mTempRect);
            width += mTempRect.left + mTempRect.right;
        }
        return width;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View current = getChildCount() > 0 ? getChildAt(0) : null;
        View view = current;
        if (current == null || mDataChanged || mShownPosition != mSelectedPosition) {
            if (current != null) {
                removeViewInLayout(current);
            }
            view = selectedView(current);
            if (view != null) {
                if (view.getParent() instanceof ViewGroup && view.getParent() != this) {
                    ((ViewGroup) view.getParent()).removeView(view);
                }
                addViewInLayout(view, 0, view.getLayoutParams(), true);
            }
            mShownPosition = mSelectedPosition;
            mDataChanged = false;
        }
        if (view == null) {
            return;
        }
        int padH = getPaddingLeft() + getPaddingRight();
        int padV = getPaddingTop() + getPaddingBottom();
        int width = r - l;
        int height = b - t;
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        view.measure(getChildMeasureSpec(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), padH, lp.width),
                getChildMeasureSpec(MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY), padV, lp.height));
        int cw = view.getMeasuredWidth();
        int ch = view.getMeasuredHeight();
        int childLeft;
        int hg = Gravity.getAbsoluteGravity(mGravity, getLayoutDirection()) & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (hg == Gravity.CENTER_HORIZONTAL) {
            childLeft = getPaddingLeft() + (width - padH - cw) / 2;
        } else if (hg == Gravity.RIGHT) {
            childLeft = width - getPaddingRight() - cw;
        } else {
            childLeft = getPaddingLeft();
        }
        int childTop = getPaddingTop() + (height - padV - ch) / 2;
        view.layout(childLeft, childTop, childLeft + cw, childTop + ch);
        view.setEnabled(isEnabled());
    }

    // ------------------------------------------------------------ popup

    @Override
    public boolean performClick() {
        boolean handled = super.performClick();
        if (!handled && mAdapter != null && mItemCount > 0) {
            handled = true;
            if (mMode == MODE_DIALOG) {
                showDialog();
            } else {
                showDropDown();
            }
        }
        return handled;
    }

    /// The dialog lists the adapter's own `getDropDownView` rows, as the
    /// drop-down does and as Android's dialog mode does, so custom row
    /// layouts, icons and disabled items survive; the selected row is
    /// marked through the list's single choice.
    private void showDialog() {
        ListView list = new ListView(getContext());
        list.setAdapter(new DropDownAdapter(mAdapter));
        list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        if (mSelectedPosition >= 0) {
            list.setItemChecked(mSelectedPosition, true);
            list.setSelection(mSelectedPosition);
        }
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Spinner.this.onClick(mDialog, position);
            }
        });
        AlertDialog.Builder b = new AlertDialog.Builder(getContext());
        if (mPrompt != null) {
            b.setTitle(mPrompt);
        }
        b.setView(list);
        mDialog = b.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        setSelection(which);
        if (dialog != null) {
            dialog.dismiss();
        }
        mDialog = null;
    }

    /// The drop-down list, laid out like Android's: as wide as the widest
    /// item or the spinner, whichever is wider, with its items lined up with
    /// the spinner's own content.
    private void showDropDown() {
        if (mDropDown == null) {
            mDropDown = new ListPopupWindow(getContext(), mPopupAttrs, mPopupDefStyleAttr, mPopupDefStyleRes);
            mDropDown.setModal(true);
            mDropDown.setPromptPosition(ListPopupWindow.POSITION_PROMPT_ABOVE);
            mDropDown.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    setSelection(position);
                    if (getOnItemClickListener() != null) {
                        performItemClick(view, position, mAdapter.getItemId(position));
                    }
                    mDropDown.dismiss();
                }
            });
        }
        if (mPopupBackground != null) {
            mDropDown.setBackgroundDrawable(mPopupBackground);
        }
        mDropDown.setAdapter(new DropDownAdapter(mAdapter));
        mDropDown.setAnchorView(this);
        mDropDown.setVerticalOffset(mDropDownVerticalOffset);
        Drawable bg = mDropDown.getBackground();
        int hOffset = 0;
        if (bg != null) {
            bg.getPadding(mTempRect);
            hOffset = isLayoutRtl() ? mTempRect.right : -mTempRect.left;
        } else {
            mTempRect.set(0, 0, 0, 0);
        }
        int padL = getPaddingLeft();
        int padR = getPaddingRight();
        int spinnerWidth = getWidth();
        if (mDropDownWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
            int contentWidth = measureContentWidth(bg, true);
            int limit = getResources().getDisplayMetrics().widthPixels - mTempRect.left - mTempRect.right;
            contentWidth = Math.min(contentWidth, limit);
            mDropDown.setContentWidth(Math.max(contentWidth, spinnerWidth - padL - padR));
        } else if (mDropDownWidth == ViewGroup.LayoutParams.MATCH_PARENT) {
            mDropDown.setContentWidth(spinnerWidth - padL - padR);
        } else {
            mDropDown.setContentWidth(mDropDownWidth);
        }
        if (isLayoutRtl()) {
            hOffset += spinnerWidth - padR - mDropDown.getWidth();
        } else {
            hOffset += padL;
        }
        mDropDown.setHorizontalOffset(hOffset);
        mDropDown.show();
        ListView list = mDropDown.getListView();
        if (list != null) {
            list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            mDropDown.setSelection(getSelectedItemPosition());
        }
    }

    private void dismissPopup() {
        if (mDropDown != null && mDropDown.isShowing()) {
            mDropDown.dismiss();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dismissPopup();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    /// Shows a spinner adapter's drop-down views in a list.
    private static final class DropDownAdapter implements ListAdapter, SpinnerAdapter {
        private final SpinnerAdapter mAdapter;

        DropDownAdapter(SpinnerAdapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public int getCount() {
            return mAdapter.getCount();
        }

        @Override
        public Object getItem(int position) {
            return mAdapter.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return mAdapter.getItemId(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mAdapter.getDropDownView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return mAdapter.getDropDownView(position, convertView, parent);
        }

        @Override
        public boolean hasStableIds() {
            return mAdapter.hasStableIds();
        }

        @Override
        public void registerDataSetObserver(android.database.DataSetObserver observer) {
            mAdapter.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(android.database.DataSetObserver observer) {
            mAdapter.unregisterDataSetObserver(observer);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return !(mAdapter instanceof ListAdapter) || ((ListAdapter) mAdapter).areAllItemsEnabled();
        }

        @Override
        public boolean isEnabled(int position) {
            return !(mAdapter instanceof ListAdapter) || ((ListAdapter) mAdapter).isEnabled(position);
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0;
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Spinner.class.getName();
    }
}
