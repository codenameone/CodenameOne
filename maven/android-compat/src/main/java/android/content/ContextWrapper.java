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
package android.content;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Looper;

/// Delegates everything to a base context.
public class ContextWrapper extends Context {

    private Context mBase;

    public ContextWrapper(Context base) {
        mBase = base;
    }

    protected void attachBaseContext(Context base) {
        if (mBase != null) {
            throw new IllegalStateException("Base context already set");
        }
        mBase = base;
    }

    public Context getBaseContext() {
        return mBase;
    }

    @Override
    public Resources getResources() {
        return mBase.getResources();
    }

    @Override
    public Resources.Theme getTheme() {
        return mBase.getTheme();
    }

    @Override
    public void setTheme(int resid) {
        mBase.setTheme(resid);
    }

    @Override
    public Context getApplicationContext() {
        return mBase.getApplicationContext();
    }

    @Override
    public String getPackageName() {
        return mBase.getPackageName();
    }

    @Override
    public Object getSystemService(String name) {
        return mBase.getSystemService(name);
    }

    @Override
    public void startActivity(Intent intent) {
        mBase.startActivity(intent);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return mBase.getSharedPreferences(name, mode);
    }

    @Override
    public AssetManager getAssets() {
        return mBase.getAssets();
    }

    @Override
    public Looper getMainLooper() {
        return Looper.getMainLooper();
    }
}
