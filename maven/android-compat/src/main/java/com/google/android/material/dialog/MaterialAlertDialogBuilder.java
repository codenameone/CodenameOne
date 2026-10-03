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
package com.google.android.material.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

/// Builds an AppCompat `AlertDialog` with Material's look: themed by the
/// theme's `materialAlertDialogTheme` and drawn on a rounded surface (28dp
/// corners in Material 3).
public class MaterialAlertDialogBuilder extends AlertDialog.Builder {

    private Drawable mBackground;

    public MaterialAlertDialogBuilder(Context context) {
        this(context, 0);
    }

    public MaterialAlertDialogBuilder(Context context, int overrideThemeResId) {
        super(context, overrideThemeResId != 0 ? overrideThemeResId : dialogTheme(context));
    }

    private static int dialogTheme(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.materialAlertDialogTheme, tv, true) && tv.resourceId != 0) {
            return tv.resourceId;
        }
        return 0;
    }

    public MaterialAlertDialogBuilder setBackground(Drawable background) {
        mBackground = background;
        return this;
    }

    public Drawable getBackground() {
        return mBackground;
    }

    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        Drawable bg = mBackground;
        if (bg == null) {
            Context c = dialog.getContext();
            TypedValue tv = new TypedValue();
            float corner = MaterialAttrs.dp(c, MaterialAttrs.isMaterial3(c) ? 28 : 4);
            if (c.getTheme().resolveAttribute(androidx.appcompat.R.attr.dialogCornerRadius, tv, true)
                    && tv.type == TypedValue.TYPE_DIMENSION) {
                corner = TypedValue.complexToDimension(tv.data, c.getResources().getDisplayMetrics());
            }
            MaterialShapeDrawable surface = new MaterialShapeDrawable(
                    ShapeAppearanceModel.builder().setAllCornerSizes(corner).build());
            int fill = MaterialColors.getColor(c, android.R.attr.colorBackground,
                    MaterialColors.getColor(c, R.attr.colorSurface, 0xffffffff));
            surface.setFillColor(ColorStateList.valueOf(fill));
            bg = surface;
        }
        dialog.getWindow().setBackgroundDrawable(bg);
        return dialog;
    }

    @Override
    public AlertDialog show() {
        AlertDialog d = create();
        d.show();
        return d;
    }

    @Override
    public MaterialAlertDialogBuilder setTitle(CharSequence title) {
        super.setTitle(title);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setTitle(int titleId) {
        super.setTitle(titleId);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setCustomTitle(View customTitleView) {
        super.setCustomTitle(customTitleView);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setMessage(CharSequence message) {
        super.setMessage(message);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setMessage(int messageId) {
        super.setMessage(messageId);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setIcon(int iconId) {
        super.setIcon(iconId);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setIcon(Drawable icon) {
        super.setIcon(icon);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setView(View view) {
        super.setView(view);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setView(int layoutResId) {
        super.setView(layoutResId);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) {
        super.setPositiveButton(text, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setPositiveButton(int textId, DialogInterface.OnClickListener listener) {
        super.setPositiveButton(textId, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) {
        super.setNegativeButton(text, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setNegativeButton(int textId, DialogInterface.OnClickListener listener) {
        super.setNegativeButton(textId, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setNeutralButton(CharSequence text, DialogInterface.OnClickListener listener) {
        super.setNeutralButton(text, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setNeutralButton(int textId, DialogInterface.OnClickListener listener) {
        super.setNeutralButton(textId, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setItems(CharSequence[] items, DialogInterface.OnClickListener listener) {
        super.setItems(items, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setItems(int itemsId, DialogInterface.OnClickListener listener) {
        super.setItems(itemsId, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setSingleChoiceItems(CharSequence[] items, int checkedItem,
                                                           DialogInterface.OnClickListener listener) {
        super.setSingleChoiceItems(items, checkedItem, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setSingleChoiceItems(int itemsId, int checkedItem,
                                                           DialogInterface.OnClickListener listener) {
        super.setSingleChoiceItems(itemsId, checkedItem, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setMultiChoiceItems(CharSequence[] items, boolean[] checkedItems,
                                                          DialogInterface.OnMultiChoiceClickListener listener) {
        super.setMultiChoiceItems(items, checkedItems, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setMultiChoiceItems(int itemsId, boolean[] checkedItems,
                                                          DialogInterface.OnMultiChoiceClickListener listener) {
        super.setMultiChoiceItems(itemsId, checkedItems, listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setCancelable(boolean cancelable) {
        super.setCancelable(cancelable);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setOnCancelListener(DialogInterface.OnCancelListener listener) {
        super.setOnCancelListener(listener);
        return this;
    }

    @Override
    public MaterialAlertDialogBuilder setOnDismissListener(DialogInterface.OnDismissListener listener) {
        super.setOnDismissListener(listener);
        return this;
    }
}
