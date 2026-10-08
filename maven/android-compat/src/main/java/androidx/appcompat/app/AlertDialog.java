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
package androidx.appcompat.app;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.View;

/// AppCompat's alert dialog: the framework's, with AppCompat's builder.
public class AlertDialog extends android.app.AlertDialog {

    protected AlertDialog(Context context) {
        super(context);
    }

    protected AlertDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    /// Builds an `AlertDialog`; the same calls as the framework's builder.
    public static class Builder {
        private final Context mContext;
        private final android.app.AlertDialog.Builder mP;

        public Builder(Context context) {
            this(context, 0);
        }

        public Builder(Context context, int themeResId) {
            mContext = context;
            mP = new android.app.AlertDialog.Builder(context, themeResId) {
                @Override
                protected android.app.AlertDialog newDialog(Context c, int theme) {
                    return new AlertDialog(c, theme);
                }
            };
        }

        public Context getContext() {
            return mContext;
        }

        public Builder setTitle(CharSequence title) {
            mP.setTitle(title);
            return this;
        }

        public Builder setTitle(int titleId) {
            mP.setTitle(titleId);
            return this;
        }

        public Builder setCustomTitle(View customTitleView) {
            mP.setCustomTitle(customTitleView);
            return this;
        }

        public Builder setMessage(CharSequence message) {
            mP.setMessage(message);
            return this;
        }

        public Builder setMessage(int messageId) {
            mP.setMessage(messageId);
            return this;
        }

        public Builder setIcon(int iconId) {
            mP.setIcon(iconId);
            return this;
        }

        public Builder setIcon(Drawable icon) {
            mP.setIcon(icon);
            return this;
        }

        public Builder setView(View view) {
            mP.setView(view);
            return this;
        }

        public Builder setView(int layoutResId) {
            mP.setView(layoutResId);
            return this;
        }

        public Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) {
            mP.setPositiveButton(text, listener);
            return this;
        }

        public Builder setPositiveButton(int textId, DialogInterface.OnClickListener listener) {
            mP.setPositiveButton(textId, listener);
            return this;
        }

        public Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) {
            mP.setNegativeButton(text, listener);
            return this;
        }

        public Builder setNegativeButton(int textId, DialogInterface.OnClickListener listener) {
            mP.setNegativeButton(textId, listener);
            return this;
        }

        public Builder setNeutralButton(CharSequence text, DialogInterface.OnClickListener listener) {
            mP.setNeutralButton(text, listener);
            return this;
        }

        public Builder setNeutralButton(int textId, DialogInterface.OnClickListener listener) {
            mP.setNeutralButton(textId, listener);
            return this;
        }

        public Builder setItems(CharSequence[] items, DialogInterface.OnClickListener listener) {
            mP.setItems(items, listener);
            return this;
        }

        public Builder setItems(int itemsId, DialogInterface.OnClickListener listener) {
            mP.setItems(itemsId, listener);
            return this;
        }

        public Builder setSingleChoiceItems(CharSequence[] items, int checkedItem,
                                            DialogInterface.OnClickListener listener) {
            mP.setSingleChoiceItems(items, checkedItem, listener);
            return this;
        }

        public Builder setSingleChoiceItems(int itemsId, int checkedItem, DialogInterface.OnClickListener listener) {
            mP.setSingleChoiceItems(itemsId, checkedItem, listener);
            return this;
        }

        public Builder setMultiChoiceItems(CharSequence[] items, boolean[] checkedItems,
                                           DialogInterface.OnMultiChoiceClickListener listener) {
            mP.setMultiChoiceItems(items, checkedItems, listener);
            return this;
        }

        public Builder setMultiChoiceItems(int itemsId, boolean[] checkedItems,
                                           DialogInterface.OnMultiChoiceClickListener listener) {
            mP.setMultiChoiceItems(itemsId, checkedItems, listener);
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            mP.setCancelable(cancelable);
            return this;
        }

        public Builder setOnCancelListener(DialogInterface.OnCancelListener listener) {
            mP.setOnCancelListener(listener);
            return this;
        }

        public Builder setOnDismissListener(DialogInterface.OnDismissListener listener) {
            mP.setOnDismissListener(listener);
            return this;
        }

        public AlertDialog create() {
            android.app.AlertDialog d = mP.create();
            if (d instanceof AlertDialog) {
                return (AlertDialog) d;
            }
            throw new IllegalStateException("the framework builder did not create an AppCompat dialog");
        }

        public AlertDialog show() {
            AlertDialog d = create();
            d.show();
            return d;
        }
    }
}
