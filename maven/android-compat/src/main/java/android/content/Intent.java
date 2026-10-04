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

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/// A request to start an activity or deliver a broadcast. Explicit intents
/// name an activity class; implicit ones with a VIEW, DIAL, SENDTO or SEND
/// action are handed to the platform (browser, dialer, mail, share sheet).
public class Intent implements Parcelable, Cloneable {

    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String ACTION_EDIT = "android.intent.action.EDIT";
    public static final String ACTION_PICK = "android.intent.action.PICK";
    public static final String ACTION_DIAL = "android.intent.action.DIAL";
    public static final String ACTION_CALL = "android.intent.action.CALL";
    public static final String ACTION_SEND = "android.intent.action.SEND";
    public static final String ACTION_SENDTO = "android.intent.action.SENDTO";
    public static final String ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE";
    public static final String ACTION_GET_CONTENT = "android.intent.action.GET_CONTENT";
    public static final String ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT";
    public static final String ACTION_WEB_SEARCH = "android.intent.action.WEB_SEARCH";
    public static final String ACTION_CHOOSER = "android.intent.action.CHOOSER";
    public static final String ACTION_SEARCH = "android.intent.action.SEARCH";
    public static final String ACTION_SETTINGS = "android.settings.SETTINGS";
    public static final String ACTION_APPLICATION_DETAILS_SETTINGS = "android.settings.APPLICATION_DETAILS_SETTINGS";
    public static final String ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED";
    public static final String ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON";
    public static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
    public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final String CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE";
    public static final String CATEGORY_HOME = "android.intent.category.HOME";
    public static final String CATEGORY_OPENABLE = "android.intent.category.OPENABLE";
    public static final String EXTRA_TEXT = "android.intent.extra.TEXT";
    public static final String EXTRA_SUBJECT = "android.intent.extra.SUBJECT";
    public static final String EXTRA_EMAIL = "android.intent.extra.EMAIL";
    public static final String EXTRA_CC = "android.intent.extra.CC";
    public static final String EXTRA_BCC = "android.intent.extra.BCC";
    public static final String EXTRA_TITLE = "android.intent.extra.TITLE";
    public static final String EXTRA_STREAM = "android.intent.extra.STREAM";
    public static final String EXTRA_INTENT = "android.intent.extra.INTENT";
    public static final String EXTRA_PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER";
    public static final String EXTRA_ALLOW_MULTIPLE = "android.intent.extra.ALLOW_MULTIPLE";
    public static final String EXTRA_MIME_TYPES = "android.intent.extra.MIME_TYPES";

    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    public static final int FLAG_ACTIVITY_CLEAR_TOP = 0x04000000;
    public static final int FLAG_ACTIVITY_SINGLE_TOP = 0x20000000;
    public static final int FLAG_ACTIVITY_CLEAR_TASK = 0x00008000;
    public static final int FLAG_ACTIVITY_NO_HISTORY = 0x40000000;
    public static final int FLAG_ACTIVITY_REORDER_TO_FRONT = 0x00020000;
    public static final int FLAG_ACTIVITY_NO_ANIMATION = 0x00010000;
    public static final int FLAG_ACTIVITY_MULTIPLE_TASK = 0x08000000;
    public static final int FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS = 0x00800000;
    public static final int FLAG_ACTIVITY_PREVIOUS_IS_TOP = 0x01000000;
    public static final int FLAG_GRANT_READ_URI_PERMISSION = 0x00000001;
    public static final int FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002;

    private String action;
    private Uri data;
    private String type;
    private ComponentName component;
    private Class<?> componentClass;
    private Set<String> categories;
    private Bundle extras;
    private int flags;
    private String pkg;

    public Intent() {
    }

    public Intent(Intent o) {
        action = o.action;
        data = o.data;
        type = o.type;
        component = o.component;
        componentClass = o.componentClass;
        categories = o.categories == null ? null : new HashSet<String>(o.categories);
        extras = o.extras == null ? null : new Bundle(o.extras);
        flags = o.flags;
        pkg = o.pkg;
    }

    public Intent(String action) {
        this.action = action;
    }

    public Intent(String action, Uri uri) {
        this.action = action;
        this.data = uri;
    }

    public Intent(Context packageContext, Class<?> cls) {
        setClass(packageContext, cls);
    }

    public Intent(String action, Uri uri, Context packageContext, Class<?> cls) {
        this(action, uri);
        setClass(packageContext, cls);
    }

    public static Intent createChooser(Intent target, CharSequence title) {
        Intent i = new Intent(ACTION_CHOOSER);
        i.putExtra(EXTRA_INTENT, target);
        if (title != null) {
            i.putExtra(EXTRA_TITLE, title);
        }
        return i;
    }

    public static Intent makeMainActivity(ComponentName mainActivity) {
        Intent i = new Intent(ACTION_MAIN);
        i.setComponent(mainActivity);
        i.addCategory(CATEGORY_LAUNCHER);
        return i;
    }

    @Override
    public Object clone() {
        return new Intent(this);
    }

    public Intent cloneFilter() {
        Intent i = new Intent(action, data);
        i.type = type;
        i.component = component;
        i.componentClass = componentClass;
        return i;
    }

    public String getAction() {
        return action;
    }

    public Intent setAction(String action) {
        this.action = action;
        return this;
    }

    public Uri getData() {
        return data;
    }

    public String getDataString() {
        return data == null ? null : data.toString();
    }

    public String getScheme() {
        return data == null ? null : data.getScheme();
    }

    public Intent setData(Uri data) {
        this.data = data;
        this.type = null;
        return this;
    }

    public String getType() {
        return type;
    }

    public Intent setType(String type) {
        this.type = type;
        this.data = null;
        return this;
    }

    public Intent setDataAndType(Uri data, String type) {
        this.data = data;
        this.type = type;
        return this;
    }

    public ComponentName getComponent() {
        return component;
    }

    public Intent setComponent(ComponentName component) {
        this.component = component;
        this.componentClass = null;
        return this;
    }

    public Intent setClass(Context packageContext, Class<?> cls) {
        this.componentClass = cls;
        this.component = new ComponentName(packageContext == null ? "" : packageContext.getPackageName(), cls.getName());
        return this;
    }

    public Intent setClassName(Context packageContext, String className) {
        return setComponent(new ComponentName(packageContext, className));
    }

    public Intent setClassName(String packageName, String className) {
        return setComponent(new ComponentName(packageName, className));
    }

    /// The target activity class of an explicit intent made with a `Class`.
    public Class<?> getComponentClass() {
        return componentClass;
    }

    public String getPackage() {
        return pkg;
    }

    public Intent setPackage(String packageName) {
        this.pkg = packageName;
        return this;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public boolean hasCategory(String category) {
        return categories != null && categories.contains(category);
    }

    public Intent addCategory(String category) {
        if (categories == null) {
            categories = new HashSet<String>();
        }
        categories.add(category);
        return this;
    }

    public void removeCategory(String category) {
        if (categories != null) {
            categories.remove(category);
        }
    }

    public int getFlags() {
        return flags;
    }

    public Intent setFlags(int flags) {
        this.flags = flags;
        return this;
    }

    public Intent addFlags(int flags) {
        this.flags |= flags;
        return this;
    }

    public void removeFlags(int flags) {
        this.flags &= ~flags;
    }

    public ComponentName resolveActivity(android.content.pm.PackageManager pm) {
        if (pm.resolveActivity(this, 0) == null) {
            return null;
        }
        com.codename1.androidcompat.runtime.AndroidRuntime rt =
                com.codename1.androidcompat.runtime.AndroidRuntime.getInstance();
        return rt == null ? component : rt.resolveComponent(this);
    }

    // ------------------------------------------------------------ extras

    private Bundle ensureExtras() {
        if (extras == null) {
            extras = new Bundle();
        }
        return extras;
    }

    /// A copy of the extras, or null when there are none. A copy on purpose:
    /// Android's `Intent.getExtras()` is `new Bundle(mExtras)` too, so code
    /// that changes the returned bundle has never changed the intent there;
    /// `putExtra`, `removeExtra` and `replaceExtras` are the way to.
    public Bundle getExtras() {
        return extras == null ? null : new Bundle(extras);
    }

    public boolean hasExtra(String name) {
        return extras != null && extras.containsKey(name);
    }

    public Intent putExtras(Bundle b) {
        if (b != null) {
            ensureExtras().putAll(b);
        }
        return this;
    }

    public Intent putExtras(Intent src) {
        if (src.extras != null) {
            ensureExtras().putAll(src.extras);
        }
        return this;
    }

    public Intent replaceExtras(Bundle b) {
        extras = b == null ? null : new Bundle(b);
        return this;
    }

    public void removeExtra(String name) {
        if (extras != null) {
            extras.remove(name);
        }
    }

    public Intent putExtra(String name, boolean value) {
        ensureExtras().putBoolean(name, value);
        return this;
    }

    public Intent putExtra(String name, byte value) {
        ensureExtras().putByte(name, value);
        return this;
    }

    public Intent putExtra(String name, char value) {
        ensureExtras().putChar(name, value);
        return this;
    }

    public Intent putExtra(String name, short value) {
        ensureExtras().putShort(name, value);
        return this;
    }

    public Intent putExtra(String name, int value) {
        ensureExtras().putInt(name, value);
        return this;
    }

    public Intent putExtra(String name, long value) {
        ensureExtras().putLong(name, value);
        return this;
    }

    public Intent putExtra(String name, float value) {
        ensureExtras().putFloat(name, value);
        return this;
    }

    public Intent putExtra(String name, double value) {
        ensureExtras().putDouble(name, value);
        return this;
    }

    public Intent putExtra(String name, String value) {
        ensureExtras().putString(name, value);
        return this;
    }

    public Intent putExtra(String name, CharSequence value) {
        ensureExtras().putCharSequence(name, value);
        return this;
    }

    public Intent putExtra(String name, Parcelable value) {
        ensureExtras().putParcelable(name, value);
        return this;
    }

    public Intent putExtra(String name, Parcelable[] value) {
        ensureExtras().putParcelableArray(name, value);
        return this;
    }

    public Intent putExtra(String name, java.io.Serializable value) {
        ensureExtras().putSerializable(name, value);
        return this;
    }

    public Intent putExtra(String name, Bundle value) {
        ensureExtras().putBundle(name, value);
        return this;
    }

    public Intent putExtra(String name, int[] value) {
        ensureExtras().putIntArray(name, value);
        return this;
    }

    public Intent putExtra(String name, long[] value) {
        ensureExtras().putLongArray(name, value);
        return this;
    }

    public Intent putExtra(String name, float[] value) {
        ensureExtras().putFloatArray(name, value);
        return this;
    }

    public Intent putExtra(String name, double[] value) {
        ensureExtras().putDoubleArray(name, value);
        return this;
    }

    public Intent putExtra(String name, boolean[] value) {
        ensureExtras().putBooleanArray(name, value);
        return this;
    }

    public Intent putExtra(String name, byte[] value) {
        ensureExtras().putByteArray(name, value);
        return this;
    }

    public Intent putExtra(String name, String[] value) {
        ensureExtras().putStringArray(name, value);
        return this;
    }

    public Intent putExtra(String name, CharSequence[] value) {
        ensureExtras().putCharSequenceArray(name, value);
        return this;
    }

    public Intent putStringArrayListExtra(String name, ArrayList<String> value) {
        ensureExtras().putStringArrayList(name, value);
        return this;
    }

    public Intent putIntegerArrayListExtra(String name, ArrayList<Integer> value) {
        ensureExtras().putIntegerArrayList(name, value);
        return this;
    }

    public Intent putParcelableArrayListExtra(String name, ArrayList<? extends Parcelable> value) {
        ensureExtras().putParcelableArrayList(name, value);
        return this;
    }

    public boolean getBooleanExtra(String name, boolean defaultValue) {
        return extras == null ? defaultValue : extras.getBoolean(name, defaultValue);
    }

    public byte getByteExtra(String name, byte defaultValue) {
        return extras == null ? defaultValue : extras.getByte(name, defaultValue).byteValue();
    }

    public char getCharExtra(String name, char defaultValue) {
        return extras == null ? defaultValue : extras.getChar(name, defaultValue);
    }

    public short getShortExtra(String name, short defaultValue) {
        return extras == null ? defaultValue : extras.getShort(name, defaultValue);
    }

    public int getIntExtra(String name, int defaultValue) {
        return extras == null ? defaultValue : extras.getInt(name, defaultValue);
    }

    public long getLongExtra(String name, long defaultValue) {
        return extras == null ? defaultValue : extras.getLong(name, defaultValue);
    }

    public float getFloatExtra(String name, float defaultValue) {
        return extras == null ? defaultValue : extras.getFloat(name, defaultValue);
    }

    public double getDoubleExtra(String name, double defaultValue) {
        return extras == null ? defaultValue : extras.getDouble(name, defaultValue);
    }

    public String getStringExtra(String name) {
        if (extras == null) {
            return null;
        }
        Object o = extras.get(name);
        return o == null ? null : o.toString();
    }

    public CharSequence getCharSequenceExtra(String name) {
        return extras == null ? null : extras.getCharSequence(name);
    }

    public <T extends Parcelable> T getParcelableExtra(String name) {
        return extras == null ? null : extras.<T>getParcelable(name);
    }

    public <T> T getParcelableExtra(String name, Class<T> clazz) {
        return extras == null ? null : extras.getParcelable(name, clazz);
    }

    public Parcelable[] getParcelableArrayExtra(String name) {
        return extras == null ? null : extras.getParcelableArray(name);
    }

    public <T extends Parcelable> ArrayList<T> getParcelableArrayListExtra(String name) {
        return extras == null ? null : extras.<T>getParcelableArrayList(name);
    }

    public java.io.Serializable getSerializableExtra(String name) {
        return extras == null ? null : extras.getSerializable(name);
    }

    public <T extends java.io.Serializable> T getSerializableExtra(String name, Class<T> clazz) {
        return extras == null ? null : extras.getSerializable(name, clazz);
    }

    public Bundle getBundleExtra(String name) {
        return extras == null ? null : extras.getBundle(name);
    }

    public int[] getIntArrayExtra(String name) {
        return extras == null ? null : extras.getIntArray(name);
    }

    public long[] getLongArrayExtra(String name) {
        return extras == null ? null : extras.getLongArray(name);
    }

    public float[] getFloatArrayExtra(String name) {
        return extras == null ? null : extras.getFloatArray(name);
    }

    public double[] getDoubleArrayExtra(String name) {
        return extras == null ? null : extras.getDoubleArray(name);
    }

    public boolean[] getBooleanArrayExtra(String name) {
        return extras == null ? null : extras.getBooleanArray(name);
    }

    public byte[] getByteArrayExtra(String name) {
        return extras == null ? null : extras.getByteArray(name);
    }

    public String[] getStringArrayExtra(String name) {
        return extras == null ? null : extras.getStringArray(name);
    }

    public CharSequence[] getCharSequenceArrayExtra(String name) {
        return extras == null ? null : extras.getCharSequenceArray(name);
    }

    public ArrayList<String> getStringArrayListExtra(String name) {
        return extras == null ? null : extras.getStringArrayList(name);
    }

    public ArrayList<Integer> getIntegerArrayListExtra(String name) {
        return extras == null ? null : extras.getIntegerArrayList(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel dest, int flags) {
        dest.writeValue(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Intent { ");
        if (action != null) {
            sb.append("act=").append(action).append(' ');
        }
        if (data != null) {
            sb.append("dat=").append(data).append(' ');
        }
        if (type != null) {
            sb.append("typ=").append(type).append(' ');
        }
        if (component != null) {
            sb.append("cmp=").append(component.flattenToShortString()).append(' ');
        }
        if (extras != null) {
            sb.append("(has extras) ");
        }
        return sb.append('}').toString();
    }
}
