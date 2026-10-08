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
package androidx.appcompat.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Switch;

import androidx.appcompat.R;

/// AppCompat's switch: the framework `Switch` with AppCompat's thumb and
/// track attributes from the application namespace.
public class SwitchCompat extends Switch {

    public SwitchCompat(Context context) {
        this(context, null);
    }

    public SwitchCompat(Context context, AttributeSet attrs) {
        this(context, attrs, AppCompatAttrs.defStyle(context, R.attr.switchStyle, android.R.attr.switchStyle));
    }

    public SwitchCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwitchCompat, defStyleAttr, 0);
        try {
            if (a.hasValue(R.styleable.SwitchCompat_thumbTint)) {
                setThumbTintList(a.getColorStateList(R.styleable.SwitchCompat_thumbTint));
            }
            if (a.hasValue(R.styleable.SwitchCompat_trackTint)) {
                setTrackTintList(a.getColorStateList(R.styleable.SwitchCompat_trackTint));
            }
            int track = a.getResourceId(R.styleable.SwitchCompat_track, 0);
            if (track != 0) {
                setTrackResource(track);
            }
            if (a.hasValue(R.styleable.SwitchCompat_showText)) {
                setShowText(a.getBoolean(R.styleable.SwitchCompat_showText, false));
            }
            if (a.hasValue(R.styleable.SwitchCompat_switchPadding)) {
                setSwitchPadding(a.getDimensionPixelSize(R.styleable.SwitchCompat_switchPadding, 0));
            }
            if (a.hasValue(R.styleable.SwitchCompat_switchMinWidth)) {
                setSwitchMinWidth(a.getDimensionPixelSize(R.styleable.SwitchCompat_switchMinWidth, 0));
            }
            if (a.hasValue(R.styleable.SwitchCompat_splitTrack)) {
                setSplitTrack(a.getBoolean(R.styleable.SwitchCompat_splitTrack, false));
            }
        } finally {
            a.recycle();
        }
        AppCompatAttrs.background(this, attrs, defStyleAttr);
    }
}
