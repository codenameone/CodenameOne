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
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import com.codename1.androidcompat.runtime.PopupHost;

/// A popup window holding a list, anchored to a view: the drop-down of a
/// spinner, a popup menu or an auto-complete field.
public class ListPopupWindow {

    public static final int POSITION_PROMPT_ABOVE = 0;
    public static final int POSITION_PROMPT_BELOW = 1;
    public static final int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
    public static final int INPUT_METHOD_FROM_FOCUSABLE = PopupWindow.INPUT_METHOD_FROM_FOCUSABLE;
    public static final int INPUT_METHOD_NEEDED = PopupWindow.INPUT_METHOD_NEEDED;
    public static final int INPUT_METHOD_NOT_NEEDED = PopupWindow.INPUT_METHOD_NOT_NEEDED;

    private final Context mContext;
    private final PopupWindow mPopup;
    private ListAdapter mAdapter;
    private ListView mDropDownList;
    private int mDropDownWidth = WRAP_CONTENT;
    private int mDropDownHeight = WRAP_CONTENT;
    private int mDropDownHorizontalOffset;
    private int mDropDownVerticalOffset;
    private int mDropDownGravity = Gravity.NO_GRAVITY;
    private boolean mDropDownAlwaysVisible;
    private boolean mForceIgnoreOutsideTouch;
    private View mDropDownAnchorView;
    private Drawable mDropDownListHighlight;
    private View mPromptView;
    private int mPromptPosition = POSITION_PROMPT_ABOVE;
    private boolean mModal;
    private AdapterView.OnItemClickListener mItemClickListener;
    private AdapterView.OnItemSelectedListener mItemSelectedListener;
    private DataSetObserver mObserver;
    private final Rect mTempRect = new Rect();

    public ListPopupWindow(Context context) {
        this(context, null, android.R.attr.listPopupWindowStyle, 0);
    }

    public ListPopupWindow(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.listPopupWindowStyle, 0);
    }

    public ListPopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ListPopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.ListPopupWindow,
                defStyleAttr, defStyleRes);
        mDropDownHorizontalOffset = a.getDimensionPixelOffset(
                android.R.styleable.ListPopupWindow_dropDownHorizontalOffset, 0);
        mDropDownVerticalOffset = a.getDimensionPixelOffset(
                android.R.styleable.ListPopupWindow_dropDownVerticalOffset, 0);
        a.recycle();
        mPopup = new PopupWindow(context, attrs, defStyleAttr, defStyleRes);
        mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
    }

    // ------------------------------------------------------------ adapter

    public void setAdapter(ListAdapter adapter) {
        if (mObserver == null) {
            mObserver = new DataSetObserver() {
                @Override
                public void onChanged() {
                    if (isShowing()) {
                        show();
                    }
                }

                @Override
                public void onInvalidated() {
                    dismiss();
                }
            };
        } else if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
        }
        mAdapter = adapter;
        if (adapter != null) {
            adapter.registerDataSetObserver(mObserver);
        }
        if (mDropDownList != null) {
            mDropDownList.setAdapter(mAdapter);
        }
    }

    // ------------------------------------------------------------ properties

    public void setPromptPosition(int position) {
        mPromptPosition = position;
    }

    public int getPromptPosition() {
        return mPromptPosition;
    }

    public void setPromptView(View prompt) {
        boolean showing = isShowing();
        if (showing) {
            dismiss();
        }
        mPromptView = prompt;
        mDropDownList = null;
        if (showing) {
            show();
        }
    }

    public void setModal(boolean modal) {
        mModal = modal;
        mPopup.setFocusable(modal);
    }

    public boolean isModal() {
        return mModal;
    }

    public void setForceIgnoreOutsideTouch(boolean forceIgnoreOutsideTouch) {
        mForceIgnoreOutsideTouch = forceIgnoreOutsideTouch;
    }

    public void setDropDownAlwaysVisible(boolean dropDownAlwaysVisible) {
        mDropDownAlwaysVisible = dropDownAlwaysVisible;
    }

    public boolean isDropDownAlwaysVisible() {
        return mDropDownAlwaysVisible;
    }

    public void setSoftInputMode(int mode) {
        mPopup.setSoftInputMode(mode);
    }

    public int getSoftInputMode() {
        return mPopup.getSoftInputMode();
    }

    public void setListSelector(Drawable selector) {
        mDropDownListHighlight = selector;
        if (mDropDownList != null && selector != null) {
            mDropDownList.setSelector(selector);
        }
    }

    public Drawable getBackground() {
        return mPopup.getBackground();
    }

    public void setBackgroundDrawable(Drawable d) {
        mPopup.setBackgroundDrawable(d);
    }

    public void setAnimationStyle(int animationStyle) {
        mPopup.setAnimationStyle(animationStyle);
    }

    public int getAnimationStyle() {
        return mPopup.getAnimationStyle();
    }

    public View getAnchorView() {
        return mDropDownAnchorView;
    }

    public void setAnchorView(View anchor) {
        mDropDownAnchorView = anchor;
    }

    public int getHorizontalOffset() {
        return mDropDownHorizontalOffset;
    }

    public void setHorizontalOffset(int offset) {
        mDropDownHorizontalOffset = offset;
    }

    public int getVerticalOffset() {
        return mDropDownVerticalOffset;
    }

    public void setVerticalOffset(int offset) {
        mDropDownVerticalOffset = offset;
    }

    public void setDropDownGravity(int gravity) {
        mDropDownGravity = gravity;
    }

    public int getWidth() {
        return mDropDownWidth;
    }

    public void setWidth(int width) {
        mDropDownWidth = width;
    }

    /// Sets the width so the list's content, not the window, is `width`
    /// wide: the background's horizontal padding is added.
    public void setContentWidth(int width) {
        Drawable bg = mPopup.getBackground();
        if (bg != null) {
            bg.getPadding(mTempRect);
            mDropDownWidth = mTempRect.left + mTempRect.right + width;
        } else {
            setWidth(width);
        }
    }

    public int getHeight() {
        return mDropDownHeight;
    }

    public void setHeight(int height) {
        mDropDownHeight = height;
    }

    public void setWindowLayoutType(int layoutType) {
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener clickListener) {
        mItemClickListener = clickListener;
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener selectedListener) {
        mItemSelectedListener = selectedListener;
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        mPopup.setOnDismissListener(listener);
    }

    public void setInputMethodMode(int mode) {
        mPopup.setInputMethodMode(mode);
    }

    public int getInputMethodMode() {
        return mPopup.getInputMethodMode();
    }

    public boolean isInputMethodNotNeeded() {
        return mPopup.getInputMethodMode() == INPUT_METHOD_NOT_NEEDED;
    }

    public void setEpicenterBounds(Rect bounds) {
    }

    public boolean isShowing() {
        return mPopup.isShowing();
    }

    public ListView getListView() {
        return mDropDownList;
    }

    // ------------------------------------------------------------ selection

    public void setSelection(int position) {
        if (isShowing() && mDropDownList != null) {
            mDropDownList.setSelection(position);
            if (mDropDownList.getChoiceMode() != AbsListView.CHOICE_MODE_NONE) {
                mDropDownList.setItemChecked(position, true);
            }
        }
    }

    public void clearListSelection() {
    }

    public boolean performItemClick(int position) {
        if (isShowing() && mItemClickListener != null && mDropDownList != null && mAdapter != null) {
            View child = mDropDownList.getChildAt(position - mDropDownList.getFirstVisiblePosition());
            mItemClickListener.onItemClick(mDropDownList, child, position, mAdapter.getItemId(position));
            return true;
        }
        return false;
    }

    public Object getSelectedItem() {
        return isShowing() && mDropDownList != null ? mDropDownList.getSelectedItem() : null;
    }

    public int getSelectedItemPosition() {
        return isShowing() && mDropDownList != null ? mDropDownList.getSelectedItemPosition()
                : AdapterView.INVALID_POSITION;
    }

    public long getSelectedItemId() {
        return isShowing() && mDropDownList != null ? mDropDownList.getSelectedItemId() : AdapterView.INVALID_ROW_ID;
    }

    public View getSelectedView() {
        return isShowing() && mDropDownList != null ? mDropDownList.getSelectedView() : null;
    }

    public void postShow() {
    }

    // ------------------------------------------------------------ show

    public void show() {
        View anchor = mDropDownAnchorView;
        if (anchor == null) {
            return;
        }
        View content = buildDropDown();
        int width;
        if (mDropDownWidth == MATCH_PARENT) {
            width = PopupHost.displayFrame().getWidth();
        } else if (mDropDownWidth == WRAP_CONTENT) {
            width = anchor.getWidth();
        } else {
            width = mDropDownWidth;
        }
        int height = mDropDownHeight;
        if (height == WRAP_CONTENT) {
            height = measureContentHeight(content, anchor, width);
        } else if (height == MATCH_PARENT) {
            height = mPopup.getMaxAvailableHeight(anchor, mDropDownVerticalOffset) + backgroundPaddingV();
        }
        mPopup.setWidth(width);
        mPopup.setHeight(height);
        // As on Android: a touch outside closes even a non-modal drop-down
        // (an auto-complete list) unless it is told to stay.
        mPopup.setOutsideTouchable(!mForceIgnoreOutsideTouch && !mDropDownAlwaysVisible);
        if (mPopup.isShowing()) {
            mPopup.update(anchor, mDropDownHorizontalOffset, mDropDownVerticalOffset, width, height);
        } else {
            mPopup.setContentView(content);
            mPopup.showAsDropDown(anchor, mDropDownHorizontalOffset, mDropDownVerticalOffset,
                    mDropDownGravity == Gravity.NO_GRAVITY ? Gravity.TOP | Gravity.START : mDropDownGravity);
        }
    }

    public void dismiss() {
        mPopup.dismiss();
        mPopup.setContentView(null);
    }

    private int backgroundPaddingV() {
        Drawable bg = mPopup.getBackground();
        if (bg == null) {
            return 0;
        }
        bg.getPadding(mTempRect);
        return mTempRect.top + mTempRect.bottom;
    }

    private int measureContentHeight(View content, View anchor, int width) {
        int padH = 0;
        Drawable bg = mPopup.getBackground();
        if (bg != null) {
            bg.getPadding(mTempRect);
            padH = mTempRect.left + mTempRect.right;
        }
        int max = mPopup.getMaxAvailableHeight(anchor, mDropDownVerticalOffset);
        content.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0, width - padH), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(Math.max(0, max), View.MeasureSpec.AT_MOST));
        return content.getMeasuredHeight() + backgroundPaddingV();
    }

    private View buildDropDown() {
        if (mDropDownList == null) {
            ListView list = new ListView(mContext);
            list.setDivider(null);
            if (mDropDownListHighlight != null) {
                list.setSelector(mDropDownListHighlight);
            }
            list.setAdapter(mAdapter);
            list.setFocusable(true);
            list.setFocusableInTouchMode(true);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClick(parent, view, position, id);
                    }
                }
            });
            list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
            mDropDownList = list;
        }
        if (mPromptView == null) {
            return mDropDownList;
        }
        if (mPromptView.getParent() instanceof ViewGroup) {
            ((ViewGroup) mPromptView.getParent()).removeView(mPromptView);
        }
        if (mDropDownList.getParent() instanceof ViewGroup) {
            ((ViewGroup) mDropDownList.getParent()).removeView(mDropDownList);
        }
        LinearLayout box = new LinearLayout(mContext);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        LinearLayout.LayoutParams promptParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (mPromptPosition == POSITION_PROMPT_BELOW) {
            box.addView(mDropDownList, listParams);
            box.addView(mPromptView, promptParams);
        } else {
            box.addView(mPromptView, promptParams);
            box.addView(mDropDownList, listParams);
        }
        return box;
    }
}
