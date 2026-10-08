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
package com.codename1.androidcompat.runtime;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;

import java.util.HashMap;
import java.util.Map;

/// The application's base context.
final class ContextImpl extends Context {

    private final AndroidRuntime runtime;
    private final Resources resources;
    private Resources.Theme theme;
    private int themeRes;
    private Application application;
    private LayoutInflater inflater;
    private AssetManager assets;
    private final Map<String, SharedPreferences> prefs = new HashMap<String, SharedPreferences>();

    ContextImpl(AndroidRuntime runtime) {
        this.runtime = runtime;
        this.resources = new Resources(ResourceManager.get());
        this.themeRes = runtime.getApp().getAppTheme();
        if (themeRes == 0) {
            themeRes = android.R.style.Theme_DeviceDefault_Light_DarkActionBar;
        }
    }

    void setApplication(Application app) {
        application = app;
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    @Override
    public Resources.Theme getTheme() {
        if (theme == null) {
            theme = resources.newTheme();
            theme.applyStyle(themeRes, true);
        }
        return theme;
    }

    @Override
    public void setTheme(int resid) {
        themeRes = resid;
        theme = null;
    }

    @Override
    public Context getApplicationContext() {
        return application;
    }

    @Override
    public String getPackageName() {
        return runtime.getPackageName();
    }

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (inflater == null) {
                inflater = new PhoneLayoutInflater(this);
            }
            return inflater;
        }
        if (WINDOW_SERVICE.equals(name)) {
            return new WindowManagerImpl();
        }
        if (INPUT_METHOD_SERVICE.equals(name)) {
            return new InputMethodManager();
        }
        CompatReport.unsupported("system service", name);
        return null;
    }

    @Override
    public void startActivity(Intent intent) {
        android.app.ActivityThread.startActivity(this, intent, -1);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        String key = name == null ? "null" : name;
        SharedPreferences p = prefs.get(key);
        if (p == null) {
            p = new SharedPreferencesImpl(key);
            prefs.put(key, p);
        }
        return p;
    }

    @Override
    public AssetManager getAssets() {
        if (assets == null) {
            assets = new AssetManager();
        }
        return assets;
    }

    @Override
    public Looper getMainLooper() {
        return Looper.getMainLooper();
    }
}
