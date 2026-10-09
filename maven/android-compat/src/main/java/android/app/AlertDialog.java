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
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/// A dialog with a title, a message or a list of items, an optional custom
/// view and up to three buttons, laid out as the Material alert dialog.
public class AlertDialog extends Dialog implements DialogInterface {

    public static final int THEME_DEVICE_DEFAULT_LIGHT = 5;

    private CharSequence mMessage;
    /// The message's view once built, or null: the dialog is built once, so
    /// a later `setMessage` updates this view, as Android's does.
    private TextView mMessageView;
    private View mView;
    /// Replaces the title row (icon and title text) when set, as Android's
    /// `setCustomTitle` does.
    private View mCustomTitle;
    private Drawable mIcon;
    private final Button[] mButtons = new Button[3];
    private final CharSequence[] mButtonText = new CharSequence[3];
    private final OnClickListener[] mButtonListeners = new OnClickListener[3];
    private CharSequence[] mItems;
    private OnClickListener mItemsListener;
    private int mChoiceMode;
    private int mCheckedItem = -1;
    private boolean[] mCheckedItems;
    private OnMultiChoiceClickListener mMultiListener;
    private final ArrayList<TextView> mItemViews = new ArrayList<TextView>();
    private boolean mBuilt;

    protected AlertDialog(Context context) {
        super(context, 0);
    }

    protected AlertDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    public void setMessage(CharSequence message) {
        mMessage = message;
        if (mMessageView != null) {
            mMessageView.setText(message);
        }
    }

    public void setView(View view) {
        mView = view;
    }

    public void setCustomTitle(View customTitleView) {
        mCustomTitle = customTitleView;
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
    }

    public void setIcon(int resId) {
        mIcon = resId == 0 ? null : getContext().getDrawable(resId);
    }

    public void setButton(int whichButton, CharSequence text, OnClickListener listener) {
        int i = index(whichButton);
        mButtonText[i] = text;
        mButtonListeners[i] = listener;
    }

    private static int index(int which) {
        return which == BUTTON_POSITIVE ? 0 : which == BUTTON_NEGATIVE ? 1 : 2;
    }

    public Button getButton(int whichButton) {
        build();
        return mButtons[index(whichButton)];
    }

    private int dp(float v) {
        return Math.round(v * getContext().getResources().getDisplayMetrics().density);
    }

    private int themeColor(int attr, int def) {
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attr, tv, true) && tv.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return tv.data;
        }
        return def;
    }

    private void build() {
        if (mBuilt) {
            return;
        }
        mBuilt = true;
        Context c = getContext();
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(24), 0, dp(8));
        int textPrimary = themeColor(android.R.attr.textColorPrimary, 0xde000000);
        int textSecondary = themeColor(android.R.attr.textColorSecondary, 0x8a000000);
        int accent = themeColor(android.R.attr.colorAccent, 0xff009688);
        CharSequence title = getTitle();
        if (mCustomTitle != null) {
            if (mCustomTitle.getParent() instanceof ViewGroup) {
                ((ViewGroup) mCustomTitle.getParent()).removeView(mCustomTitle);
            }
            root.addView(mCustomTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        } else if (title != null && title.length() > 0) {
            LinearLayout titleRow = new LinearLayout(c);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            titleRow.setPadding(dp(24), 0, dp(24), dp(mMessage != null || mItems != null ? 16 : 8));
            if (mIcon != null) {
                ImageView icon = new ImageView(c);
                icon.setImageDrawable(mIcon);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(32), dp(32));
                lp.rightMargin = dp(8);
                titleRow.addView(icon, lp);
            }
            TextView t = new TextView(c);
            t.setText(title);
            t.setTextSize(20);
            t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            t.setTextColor(textPrimary);
            titleRow.addView(t);
            root.addView(titleRow);
        }
        if (mMessage != null) {
            TextView m = new TextView(c);
            m.setText(mMessage);
            m.setTextSize(16);
            m.setTextColor(textSecondary);
            m.setPadding(dp(24), 0, dp(24), dp(16));
            root.addView(m);
            mMessageView = m;
        }
        if (mItems != null) {
            for (int i = 0; i < mItems.length; i++) {
                final int which = i;
                TextView item = new TextView(c);
                item.setMinimumHeight(dp(48));
                item.setGravity(Gravity.CENTER_VERTICAL);
                item.setPadding(dp(24), 0, dp(24), 0);
                item.setTextSize(16);
                item.setTextColor(textPrimary);
                item.setBackgroundResource(android.R.drawable.item_background_material);
                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onItemClicked(which);
                    }
                });
                mItemViews.add(item);
                root.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            refreshItems();
        }
        if (mView != null) {
            FrameLayout custom = new FrameLayout(c);
            if (mView.getParent() instanceof ViewGroup) {
                ((ViewGroup) mView.getParent()).removeView(mView);
            }
            custom.addView(mView);
            root.addView(custom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (mButtonText[0] != null || mButtonText[1] != null || mButtonText[2] != null) {
            LinearLayout bar = new LinearLayout(c);
            bar.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            bar.setPadding(dp(12), dp(4), dp(12), 0);
            int[] order = {2, 1, 0};
            int[] which = {BUTTON_NEUTRAL, BUTTON_NEGATIVE, BUTTON_POSITIVE};
            for (int k = 0; k < 3; k++) {
                final int i = order[k];
                final int w = which[i == 0 ? 2 : i == 1 ? 1 : 0];
                Button b = new Button(c, null, 0, android.R.style.Widget_Material_Button_Borderless);
                b.setText(mButtonText[i] == null ? "" : mButtonText[i]);
                b.setTextColor(accent);
                b.setVisibility(mButtonText[i] == null ? View.GONE : View.VISIBLE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mButtonListeners[i] != null) {
                            mButtonListeners[i].onClick(AlertDialog.this, w);
                        }
                        dismiss();
                    }
                });
                mButtons[i] = b;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i == 2) {
                    lp.weight = 1;
                    lp.width = 0;
                    b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                }
                bar.addView(b, lp);
            }
            root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        setContentView(root);
    }

    private void onItemClicked(int which) {
        if (mChoiceMode == 0) {
            if (mItemsListener != null) {
                mItemsListener.onClick(this, which);
            }
            dismiss();
        } else if (mChoiceMode == 1) {
            mCheckedItem = which;
            refreshItems();
            if (mItemsListener != null) {
                mItemsListener.onClick(this, which);
            }
        } else {
            mCheckedItems[which] = !mCheckedItems[which];
            refreshItems();
            if (mMultiListener != null) {
                mMultiListener.onClick(this, which, mCheckedItems[which]);
            }
        }
    }

    private void refreshItems() {
        for (int i = 0; i < mItemViews.size(); i++) {
            String mark = "";
            if (mChoiceMode == 1) {
                mark = i == mCheckedItem ? "\u25c9  " : "\u25cb  ";
            } else if (mChoiceMode == 2) {
                mark = mCheckedItems[i] ? "\u2611  " : "\u2610  ";
            }
            mItemViews.get(i).setText(mark + mItems[i]);
        }
    }

    @Override
    public void show() {
        build();
        super.show();
    }

    /// Builds an AlertDialog.
    public static class Builder {
        private final Context context;
        private final int theme;
        private CharSequence title;
        private CharSequence message;
        private View view;
        private View customTitle;
        private int viewLayout;
        private Drawable icon;
        private final CharSequence[] buttonText = new CharSequence[3];
        private final OnClickListener[] buttonListeners = new OnClickListener[3];
        private CharSequence[] items;
        private OnClickListener itemsListener;
        private int choiceMode;
        private int checkedItem = -1;
        private boolean[] checkedItems;
        private OnMultiChoiceClickListener multiListener;
        private boolean cancelable = true;
        private OnCancelListener cancelListener;
        private OnDismissListener dismissListener;

        public Builder(Context context) {
            this(context, 0);
        }

        public Builder(Context context, int themeResId) {
            this.context = context;
            this.theme = themeResId;
        }

        public Context getContext() {
            return context;
        }

        public Builder setTitle(CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder setTitle(int titleId) {
            return setTitle(context.getText(titleId));
        }

        public Builder setCustomTitle(View customTitleView) {
            customTitle = customTitleView;
            return this;
        }

        public Builder setMessage(CharSequence message) {
            this.message = message;
            return this;
        }

        public Builder setMessage(int messageId) {
            return setMessage(context.getText(messageId));
        }

        public Builder setIcon(int iconId) {
            icon = iconId == 0 ? null : context.getDrawable(iconId);
            return this;
        }

        public Builder setIcon(Drawable icon) {
            this.icon = icon;
            return this;
        }

        public Builder setView(View view) {
            this.view = view;
            return this;
        }

        public Builder setView(int layoutResId) {
            viewLayout = layoutResId;
            return this;
        }

        public Builder setPositiveButton(CharSequence text, OnClickListener listener) {
            buttonText[0] = text;
            buttonListeners[0] = listener;
            return this;
        }

        public Builder setPositiveButton(int textId, OnClickListener listener) {
            return setPositiveButton(context.getText(textId), listener);
        }

        public Builder setNegativeButton(CharSequence text, OnClickListener listener) {
            buttonText[1] = text;
            buttonListeners[1] = listener;
            return this;
        }

        public Builder setNegativeButton(int textId, OnClickListener listener) {
            return setNegativeButton(context.getText(textId), listener);
        }

        public Builder setNeutralButton(CharSequence text, OnClickListener listener) {
            buttonText[2] = text;
            buttonListeners[2] = listener;
            return this;
        }

        public Builder setNeutralButton(int textId, OnClickListener listener) {
            return setNeutralButton(context.getText(textId), listener);
        }

        public Builder setItems(CharSequence[] items, OnClickListener listener) {
            this.items = items;
            this.itemsListener = listener;
            this.choiceMode = 0;
            return this;
        }

        public Builder setItems(int itemsId, OnClickListener listener) {
            return setItems(context.getResources().getTextArray(itemsId), listener);
        }

        public Builder setSingleChoiceItems(CharSequence[] items, int checked, OnClickListener listener) {
            this.items = items;
            this.checkedItem = checked;
            this.itemsListener = listener;
            this.choiceMode = 1;
            return this;
        }

        public Builder setSingleChoiceItems(int itemsId, int checked, OnClickListener listener) {
            return setSingleChoiceItems(context.getResources().getTextArray(itemsId), checked, listener);
        }

        public Builder setMultiChoiceItems(CharSequence[] items, boolean[] checked, OnMultiChoiceClickListener l) {
            this.items = items;
            this.checkedItems = checked == null ? new boolean[items.length] : checked;
            this.multiListener = l;
            this.choiceMode = 2;
            return this;
        }

        public Builder setMultiChoiceItems(int itemsId, boolean[] checked, OnMultiChoiceClickListener l) {
            return setMultiChoiceItems(context.getResources().getTextArray(itemsId), checked, l);
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Builder setOnCancelListener(OnCancelListener l) {
            cancelListener = l;
            return this;
        }

        public Builder setOnDismissListener(OnDismissListener l) {
            dismissListener = l;
            return this;
        }

        /// The dialog `create` fills in. AppCompat's builder answers its own
        /// `AlertDialog` subclass here.
        protected AlertDialog newDialog(Context context, int themeResId) {
            return new AlertDialog(context, themeResId);
        }

        public AlertDialog create() {
            AlertDialog d = newDialog(context, theme);
            d.setTitle(title);
            d.mMessage = message;
            d.mIcon = icon;
            d.mCustomTitle = customTitle;
            d.mView = view != null ? view : viewLayout != 0
                    ? d.getLayoutInflater().inflate(viewLayout, null, false) : null;
            for (int i = 0; i < 3; i++) {
                d.mButtonText[i] = buttonText[i];
                d.mButtonListeners[i] = buttonListeners[i];
            }
            d.mItems = items;
            d.mItemsListener = itemsListener;
            d.mChoiceMode = choiceMode;
            d.mCheckedItem = checkedItem;
            d.mCheckedItems = checkedItems;
            d.mMultiListener = multiListener;
            d.setCancelable(cancelable);
            d.setCanceledOnTouchOutside(cancelable);
            d.setOnCancelListener(cancelListener);
            d.setOnDismissListener(dismissListener);
            return d;
        }

        public AlertDialog show() {
            AlertDialog d = create();
            d.show();
            return d;
        }
    }
}
