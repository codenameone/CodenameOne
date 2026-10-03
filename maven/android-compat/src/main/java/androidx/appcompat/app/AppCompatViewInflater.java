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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.AppCompatToggleButton;

/// Creates the AppCompat widget for a framework tag inflated in an AppCompat
/// activity (`<TextView>` becomes `AppCompatTextView`), with `new` rather
/// than reflection. A library that substitutes its own widgets -- as Material
/// Components does through the theme's `viewInflaterClass` -- extends this
/// class, overrides the `create` methods, and is chosen in [#forTheme].
public class AppCompatViewInflater implements LayoutInflater.Factory2 {

    /// The inflater the theme of `context` asks for. A library inflater
    /// is added here by the name its themes set in `viewInflaterClass`.
    public static AppCompatViewInflater forTheme(Context context) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(androidx.appcompat.R.attr.viewInflaterClass, tv, true)
                && tv.string != null
                && com.google.android.material.theme.MaterialComponentsViewInflater.CLASS_NAME
                        .equals(tv.string.toString())) {
            return new com.google.android.material.theme.MaterialComponentsViewInflater();
        }
        return new AppCompatViewInflater();
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return createView(parent, name, context, attrs);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return createView(null, name, context, attrs);
    }

    public final View createView(View parent, String name, Context context, AttributeSet attrs) {
        if ("TextView".equals(name)) {
            return createTextView(context, attrs);
        }
        if ("ImageView".equals(name)) {
            return createImageView(context, attrs);
        }
        if ("Button".equals(name)) {
            return createButton(context, attrs);
        }
        if ("EditText".equals(name)) {
            return createEditText(context, attrs);
        }
        if ("Spinner".equals(name)) {
            return createSpinner(context, attrs);
        }
        if ("ImageButton".equals(name)) {
            return createImageButton(context, attrs);
        }
        if ("CheckBox".equals(name)) {
            return createCheckBox(context, attrs);
        }
        if ("RadioButton".equals(name)) {
            return createRadioButton(context, attrs);
        }
        if ("CheckedTextView".equals(name)) {
            return createCheckedTextView(context, attrs);
        }
        if ("AutoCompleteTextView".equals(name)) {
            return createAutoCompleteTextView(context, attrs);
        }
        if ("MultiAutoCompleteTextView".equals(name)) {
            return createMultiAutoCompleteTextView(context, attrs);
        }
        if ("RatingBar".equals(name)) {
            return createRatingBar(context, attrs);
        }
        if ("SeekBar".equals(name)) {
            return createSeekBar(context, attrs);
        }
        if ("ToggleButton".equals(name)) {
            return createToggleButton(context, attrs);
        }
        return createView(context, name, attrs);
    }

    /// A tag this inflater does not substitute; null lets the inflater create it.
    protected View createView(Context context, String name, AttributeSet attrs) {
        return null;
    }

    protected AppCompatTextView createTextView(Context context, AttributeSet attrs) {
        return new AppCompatTextView(context, attrs);
    }

    protected AppCompatImageView createImageView(Context context, AttributeSet attrs) {
        return new AppCompatImageView(context, attrs);
    }

    protected AppCompatButton createButton(Context context, AttributeSet attrs) {
        return new AppCompatButton(context, attrs);
    }

    protected AppCompatEditText createEditText(Context context, AttributeSet attrs) {
        return new AppCompatEditText(context, attrs);
    }

    protected AppCompatSpinner createSpinner(Context context, AttributeSet attrs) {
        return new AppCompatSpinner(context, attrs);
    }

    protected AppCompatImageButton createImageButton(Context context, AttributeSet attrs) {
        return new AppCompatImageButton(context, attrs);
    }

    protected AppCompatCheckBox createCheckBox(Context context, AttributeSet attrs) {
        return new AppCompatCheckBox(context, attrs);
    }

    protected AppCompatRadioButton createRadioButton(Context context, AttributeSet attrs) {
        return new AppCompatRadioButton(context, attrs);
    }

    protected AppCompatCheckedTextView createCheckedTextView(Context context, AttributeSet attrs) {
        return new AppCompatCheckedTextView(context, attrs);
    }

    protected AppCompatAutoCompleteTextView createAutoCompleteTextView(Context context, AttributeSet attrs) {
        return new AppCompatAutoCompleteTextView(context, attrs);
    }

    protected AppCompatMultiAutoCompleteTextView createMultiAutoCompleteTextView(Context context, AttributeSet attrs) {
        return new AppCompatMultiAutoCompleteTextView(context, attrs);
    }

    protected AppCompatRatingBar createRatingBar(Context context, AttributeSet attrs) {
        return new AppCompatRatingBar(context, attrs);
    }

    protected AppCompatSeekBar createSeekBar(Context context, AttributeSet attrs) {
        return new AppCompatSeekBar(context, attrs);
    }

    protected AppCompatToggleButton createToggleButton(Context context, AttributeSet attrs) {
        return new AppCompatToggleButton(context, attrs);
    }
}
