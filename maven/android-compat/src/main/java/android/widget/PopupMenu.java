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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.codename1.androidcompat.runtime.MenuImpl;
import com.codename1.androidcompat.runtime.MenuPopup;

/// A menu shown in a popup anchored to a view.
public class PopupMenu {

    public interface OnMenuItemClickListener {
        boolean onMenuItemClick(MenuItem item);
    }

    public interface OnDismissListener {
        void onDismiss(PopupMenu menu);
    }

    private final Context mContext;
    private final View mAnchor;
    private final MenuImpl mMenu;
    private final int mPopupStyleAttr;
    private final int mPopupStyleRes;
    private int mGravity;
    private OnMenuItemClickListener mMenuItemClickListener;
    private OnDismissListener mOnDismissListener;
    private ListPopupWindow mPopup;

    public PopupMenu(Context context, View anchor) {
        this(context, anchor, Gravity.NO_GRAVITY);
    }

    public PopupMenu(Context context, View anchor, int gravity) {
        this(context, anchor, gravity, android.R.attr.popupMenuStyle, 0);
    }

    public PopupMenu(Context context, View anchor, int gravity, int popupStyleAttr, int popupStyleRes) {
        mContext = context;
        mAnchor = anchor;
        mGravity = gravity;
        mPopupStyleAttr = popupStyleAttr;
        mPopupStyleRes = popupStyleRes;
        mMenu = new MenuImpl(context);
        mMenu.setItemHandler(new MenuImpl.ItemHandler() {
            @Override
            public boolean onItemSelected(MenuItem item) {
                return mMenuItemClickListener != null && mMenuItemClickListener.onMenuItemClick(item);
            }
        });
    }

    public void setGravity(int gravity) {
        mGravity = gravity;
    }

    public int getGravity() {
        return mGravity;
    }

    /// Drag-to-open is not supported: the listener lets every touch through.
    private static final View.OnTouchListener NO_DRAG_TO_OPEN = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, android.view.MotionEvent event) {
            return false;
        }
    };

    public View.OnTouchListener getDragToOpenListener() {
        return NO_DRAG_TO_OPEN;
    }

    public Menu getMenu() {
        return mMenu;
    }

    public MenuInflater getMenuInflater() {
        return new MenuInflater(mContext);
    }

    public void inflate(int menuRes) {
        getMenuInflater().inflate(menuRes, mMenu);
    }

    public void setForceShowIcon(boolean forceShowIcon) {
    }

    public void show() {
        if (mPopup != null && mPopup.isShowing()) {
            return;
        }
        mPopup = MenuPopup.show(mContext, mMenu, mAnchor, mGravity, mPopupStyleAttr, mPopupStyleRes,
                new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        mPopup = null;
                        if (mOnDismissListener != null) {
                            mOnDismissListener.onDismiss(PopupMenu.this);
                        }
                    }
                });
    }

    public void dismiss() {
        if (mPopup != null) {
            mPopup.dismiss();
        }
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        mMenuItemClickListener = listener;
    }

    public void setOnDismissListener(OnDismissListener listener) {
        mOnDismissListener = listener;
    }
}
