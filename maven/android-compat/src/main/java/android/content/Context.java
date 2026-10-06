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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.AttributeSet;

/// Access to application resources, services and activity launching.
public abstract class Context {

    public static final int MODE_PRIVATE = 0x0000;
    public static final int MODE_WORLD_READABLE = 0x0001;
    public static final int MODE_WORLD_WRITEABLE = 0x0002;
    public static final int MODE_APPEND = 0x8000;
    public static final int MODE_MULTI_PROCESS = 0x0004;
    public static final int BIND_AUTO_CREATE = 0x0001;
    public static final int RECEIVER_EXPORTED = 0x2;
    public static final int RECEIVER_NOT_EXPORTED = 0x4;

    public static final String LAYOUT_INFLATER_SERVICE = "layout_inflater";
    public static final String WINDOW_SERVICE = "window";
    public static final String INPUT_METHOD_SERVICE = "input_method";
    public static final String CLIPBOARD_SERVICE = "clipboard";
    public static final String CONNECTIVITY_SERVICE = "connectivity";
    public static final String VIBRATOR_SERVICE = "vibrator";
    public static final String NOTIFICATION_SERVICE = "notification";
    public static final String ALARM_SERVICE = "alarm";
    public static final String LOCATION_SERVICE = "location";
    public static final String AUDIO_SERVICE = "audio";
    public static final String ACTIVITY_SERVICE = "activity";
    public static final String KEYGUARD_SERVICE = "keyguard";
    public static final String POWER_SERVICE = "power";
    public static final String SENSOR_SERVICE = "sensor";
    public static final String TELEPHONY_SERVICE = "phone";
    public static final String WIFI_SERVICE = "wifi";
    public static final String DOWNLOAD_SERVICE = "download";
    public static final String ACCESSIBILITY_SERVICE = "accessibility";
    public static final String STORAGE_SERVICE = "storage";
    public static final String DISPLAY_SERVICE = "display";
    public static final String UI_MODE_SERVICE = "uimode";

    public abstract Resources getResources();

    public abstract Resources.Theme getTheme();

    public abstract void setTheme(int resid);

    public abstract Context getApplicationContext();

    public abstract String getPackageName();

    public abstract Object getSystemService(String name);

    public abstract void startActivity(Intent intent);

    public abstract SharedPreferences getSharedPreferences(String name, int mode);

    public abstract AssetManager getAssets();

    public abstract Looper getMainLooper();

    public void startActivity(Intent intent, android.os.Bundle options) {
        startActivity(intent);
    }

    public void startActivities(Intent[] intents) {
        for (Intent i : intents) {
            startActivity(i);
        }
    }

    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    public android.content.pm.ApplicationInfo getApplicationInfo() {
        return new android.content.pm.ApplicationInfo(getPackageName());
    }

    public android.content.pm.PackageManager getPackageManager() {
        return new android.content.pm.PackageManager(this);
    }

    public ContentResolver getContentResolver() {
        return new ContentResolver(this);
    }

    public final String getString(int resId) {
        return getResources().getString(resId);
    }

    public final String getString(int resId, Object... formatArgs) {
        return getResources().getString(resId, formatArgs);
    }

    public final CharSequence getText(int resId) {
        return getResources().getText(resId);
    }

    public final int getColor(int id) {
        return getResources().getColor(id, getTheme());
    }

    public final ColorStateList getColorStateList(int id) {
        return getResources().getColorStateList(id, getTheme());
    }

    public final Drawable getDrawable(int id) {
        return getResources().getDrawable(id, getTheme());
    }

    @SuppressWarnings("unchecked")
    public final <T> T getSystemService(Class<T> serviceClass) {
        String name = getSystemServiceName(serviceClass);
        return name == null ? null : (T) getSystemService(name);
    }

    public String getSystemServiceName(Class<?> serviceClass) {
        if (serviceClass == android.view.LayoutInflater.class) {
            return LAYOUT_INFLATER_SERVICE;
        }
        if (serviceClass == android.view.WindowManager.class) {
            return WINDOW_SERVICE;
        }
        if (serviceClass == android.view.inputmethod.InputMethodManager.class) {
            return INPUT_METHOD_SERVICE;
        }
        return null;
    }

    public final TypedArray obtainStyledAttributes(int[] attrs) {
        return getTheme().obtainStyledAttributes(attrs);
    }

    public final TypedArray obtainStyledAttributes(int resid, int[] attrs) {
        return getTheme().obtainStyledAttributes(resid, attrs);
    }

    public final TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs) {
        return getTheme().obtainStyledAttributes(set, attrs, 0, 0);
    }

    public final TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
        return getTheme().obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes);
    }

    public int checkSelfPermission(String permission) {
        return android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    public int checkCallingOrSelfPermission(String permission) {
        return android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    // ------------------------------------------------------------ files

    /// The application's private directories live under the Codename One
    /// application home, laid out like Android's data directory. `java.io.File`
    /// here is the compatibility runtime's file class once the build has
    /// relocated the code.
    private static java.io.File appDir(String name) {
        String home = com.codename1.io.FileSystemStorage.getInstance().getAppHomePath();
        return ensureDir(new java.io.File(home.endsWith("/") ? home + name : home + "/" + name));
    }

    /// Creates `dir` when missing. Like Android's, the directory getters
    /// still answer it when that fails, and report the failure in the log.
    private static java.io.File ensureDir(java.io.File dir) {
        if (!dir.exists() && !dir.mkdirs() && !dir.isDirectory()) {
            com.codename1.io.Log.p("Unable to create directory " + dir);
        }
        return dir;
    }

    public java.io.File getDataDir() {
        return new java.io.File(com.codename1.io.FileSystemStorage.getInstance().getAppHomePath());
    }

    public java.io.File getFilesDir() {
        return appDir("files");
    }

    public java.io.File getNoBackupFilesDir() {
        return appDir("no_backup");
    }

    public java.io.File getCodeCacheDir() {
        return appDir("code_cache");
    }

    public java.io.File getCacheDir() {
        com.codename1.io.FileSystemStorage fs = com.codename1.io.FileSystemStorage.getInstance();
        if (fs.hasCachesDir()) {
            String caches = fs.getCachesDir();
            if (caches != null) {
                return ensureDir(new java.io.File(caches));
            }
        }
        return appDir("cache");
    }

    public java.io.File getDir(String name, int mode) {
        checkFileName(name);
        return appDir("app_" + name);
    }

    /// There is no shared external storage on most targets; the external
    /// directories are application-private directories of their own.
    public java.io.File getExternalFilesDir(String type) {
        return type == null ? appDir("external/files") : appDir("external/files/" + type);
    }

    public java.io.File[] getExternalFilesDirs(String type) {
        return new java.io.File[] {getExternalFilesDir(type)};
    }

    public java.io.File getExternalCacheDir() {
        return appDir("external/cache");
    }

    public java.io.File[] getExternalCacheDirs() {
        return new java.io.File[] {getExternalCacheDir()};
    }

    public java.io.File getObbDir() {
        return appDir("external/obb");
    }

    public java.io.File getFileStreamPath(String name) {
        checkFileName(name);
        return new java.io.File(getFilesDir(), name);
    }

    public java.io.FileInputStream openFileInput(String name) throws java.io.FileNotFoundException {
        return new java.io.FileInputStream(getFileStreamPath(name));
    }

    public java.io.FileOutputStream openFileOutput(String name, int mode) throws java.io.FileNotFoundException {
        return new java.io.FileOutputStream(getFileStreamPath(name), (mode & MODE_APPEND) != 0);
    }

    public boolean deleteFile(String name) {
        return getFileStreamPath(name).delete();
    }

    public String[] fileList() {
        String[] names = getFilesDir().list();
        return names == null ? new String[0] : names;
    }

    /// Android refuses only `/`. A backslash is refused too: the remapped
    /// `java.io.File` turns it into `/` (as does the Windows simulator's real
    /// one), so a name such as `..\shared_prefs\x` would leave the files
    /// directory.
    private static void checkFileName(String name) {
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("File " + name + " contains a path separator");
        }
    }

    /// Where a named database lives: the platform's database path when it
    /// exposes one, otherwise the `databases` directory Android would use.
    public java.io.File getDatabasePath(String name) {
        if (name.startsWith("/") || name.startsWith("file:")) {
            return new java.io.File(name);
        }
        try {
            if (com.codename1.db.Database.isCustomPathSupported()) {
                String p = com.codename1.db.Database.getDatabasePath(name);
                if (p != null) {
                    return new java.io.File(p);
                }
            }
        } catch (IllegalArgumentException e) {
            // A name the platform refuses: use the conventional location.
        }
        return new java.io.File(appDir("databases"), name);
    }

    // ------------------------------------------------------------ databases

    public android.database.sqlite.SQLiteDatabase openOrCreateDatabase(String name, int mode,
            android.database.sqlite.SQLiteDatabase.CursorFactory factory) {
        return openOrCreateDatabase(name, mode, factory, null);
    }

    /// Databases are named, not placed: the name is the Codename One
    /// database name, stored where the platform keeps databases.
    public android.database.sqlite.SQLiteDatabase openOrCreateDatabase(String name, int mode,
            android.database.sqlite.SQLiteDatabase.CursorFactory factory,
            android.database.DatabaseErrorHandler errorHandler) {
        checkDatabaseName(name);
        return android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(name, factory, errorHandler);
    }

    public boolean deleteDatabase(String name) {
        checkDatabaseName(name);
        return android.database.sqlite.SQLiteDatabase.deleteDatabase(name);
    }

    /// The databases in the platform's database directory, where the
    /// platform exposes one as a file system path; empty otherwise.
    public String[] databaseList() {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        try {
            if (!com.codename1.db.Database.isCustomPathSupported()) {
                return new String[0];
            }
            String probe = com.codename1.db.Database.getDatabasePath("probe.db");
            int slash = probe == null ? -1 : probe.lastIndexOf('/');
            if (slash < 0) {
                return new String[0];
            }
            String[] files = com.codename1.io.FileSystemStorage.getInstance().listFiles(probe.substring(0, slash + 1));
            if (files == null) {
                return new String[0];
            }
            for (String f : files) {
                if (f != null && !f.endsWith("/")) {
                    out.add(f);
                }
            }
        } catch (java.io.IOException e) {
            return new String[0];
        } catch (RuntimeException e) {
            return new String[0];
        }
        String[] names = new String[out.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = out.get(i);
        }
        return names;
    }

    private static void checkDatabaseName(String name) {
        if (name.indexOf('/') >= 0) {
            throw new IllegalArgumentException("File " + name + " contains a path separator");
        }
    }

    public void sendBroadcast(Intent intent) {
        com.codename1.androidcompat.runtime.AndroidRuntime rt = com.codename1.androidcompat.runtime.AndroidRuntime.getInstance();
        if (rt != null) {
            rt.sendBroadcast(this, intent);
        }
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        com.codename1.androidcompat.runtime.AndroidRuntime rt = com.codename1.androidcompat.runtime.AndroidRuntime.getInstance();
        if (rt != null) {
            rt.registerReceiver(receiver, filter);
        }
        return null;
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags) {
        return registerReceiver(receiver, filter);
    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
        com.codename1.androidcompat.runtime.AndroidRuntime rt = com.codename1.androidcompat.runtime.AndroidRuntime.getInstance();
        if (rt != null) {
            rt.unregisterReceiver(receiver);
        }
    }

    public boolean isRestricted() {
        return false;
    }
}
