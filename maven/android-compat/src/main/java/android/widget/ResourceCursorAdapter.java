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
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/// A cursor adapter whose item views are inflated from a layout resource.
public abstract class ResourceCursorAdapter extends CursorAdapter {

    private int mLayout;
    private int mDropDownLayout;
    private LayoutInflater mInflater;
    private LayoutInflater mDropDownInflater;

    @Deprecated
    public ResourceCursorAdapter(Context context, int layout, Cursor c) {
        super(context, c);
        setup(context, layout);
    }

    @Deprecated
    public ResourceCursorAdapter(Context context, int layout, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        setup(context, layout);
    }

    public ResourceCursorAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, c, flags);
        setup(context, layout);
    }

    private void setup(Context context, int layout) {
        mLayout = layout;
        mDropDownLayout = layout;
        mInflater = LayoutInflater.from(context);
        mDropDownInflater = mInflater;
    }

    public void setDropDownViewTheme(android.content.res.Resources.Theme theme) {
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(mLayout, parent, false);
    }

    @Override
    public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
        return mDropDownInflater.inflate(mDropDownLayout, parent, false);
    }

    public void setViewResource(int layout) {
        mLayout = layout;
    }

    public void setDropDownViewResource(int dropDownLayout) {
        mDropDownLayout = dropDownLayout;
    }
}
