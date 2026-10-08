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
package android.content.res;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

/// The values `obtainStyledAttributes` resolved, one slot per requested attr,
/// with Android's coercion rules for each accessor.
public class TypedArray {

    private final Resources resources;
    private final int[] attrs;
    private final TypedValue[] values;
    private final Resources.Theme theme;
    private final boolean[] fromXml;
    private int[] indices;

    TypedArray(Resources resources, int[] attrs, TypedValue[] values, Resources.Theme theme) {
        this(resources, attrs, values, theme, null);
    }

    TypedArray(Resources resources, int[] attrs, TypedValue[] values, Resources.Theme theme, boolean[] fromXml) {
        this.resources = resources;
        this.attrs = attrs;
        this.values = values;
        this.theme = theme;
        this.fromXml = fromXml;
    }

    public int length() {
        return values.length;
    }

    public int getIndexCount() {
        return indices().length;
    }

    public int getIndex(int at) {
        return indices()[at];
    }

    private int[] indices() {
        if (indices == null) {
            int n = 0;
            for (TypedValue v : values) {
                if (v != null) {
                    n++;
                }
            }
            indices = new int[n];
            n = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    indices[n++] = i;
                }
            }
        }
        return indices;
    }

    public Resources getResources() {
        return resources;
    }

    private TypedValue v(int index) {
        if (index < 0 || index >= values.length) {
            return null;
        }
        TypedValue tv = values[index];
        if (tv == null || tv.type == TypedValue.TYPE_NULL) {
            return null;
        }
        return tv;
    }

    private static boolean isInt(TypedValue tv) {
        return tv.type >= TypedValue.TYPE_FIRST_INT && tv.type <= TypedValue.TYPE_LAST_INT;
    }

    public CharSequence getText(int index) {
        TypedValue tv = v(index);
        if (tv == null) {
            return null;
        }
        if (tv.type == TypedValue.TYPE_STRING) {
            return tv.string;
        }
        return tv.coerceToString();
    }

    public String getString(int index) {
        CharSequence cs = getText(index);
        return cs == null ? null : cs.toString();
    }

    /// The value only when the XML element gave it as literal text.
    public String getNonResourceString(int index) {
        TypedValue tv = v(index);
        if (tv == null || tv.type != TypedValue.TYPE_STRING || tv.resourceId != 0
                || fromXml == null || !fromXml[index]) {
            return null;
        }
        return tv.string.toString();
    }

    public boolean getBoolean(int index, boolean defValue) {
        TypedValue tv = v(index);
        if (tv == null) {
            return defValue;
        }
        if (isInt(tv)) {
            return tv.data != 0;
        }
        if (tv.type == TypedValue.TYPE_STRING) {
            return "true".equals(tv.string.toString());
        }
        return defValue;
    }

    public int getInt(int index, int defValue) {
        TypedValue tv = v(index);
        if (tv == null) {
            return defValue;
        }
        if (isInt(tv)) {
            return tv.data;
        }
        if (tv.type == TypedValue.TYPE_FLOAT) {
            return (int) tv.getFloat();
        }
        if (tv.type == TypedValue.TYPE_STRING) {
            try {
                return Integer.parseInt(tv.string.toString().trim());
            } catch (NumberFormatException e) {
                return defValue;
            }
        }
        return defValue;
    }

    public float getFloat(int index, float defValue) {
        TypedValue tv = v(index);
        if (tv == null) {
            return defValue;
        }
        if (tv.type == TypedValue.TYPE_FLOAT) {
            return tv.getFloat();
        }
        if (isInt(tv)) {
            return tv.data;
        }
        if (tv.type == TypedValue.TYPE_STRING) {
            try {
                return Float.parseFloat(tv.string.toString().trim());
            } catch (NumberFormatException e) {
                return defValue;
            }
        }
        return defValue;
    }

    public int getColor(int index, int defValue) {
        TypedValue tv = v(index);
        if (tv == null) {
            return defValue;
        }
        if (isInt(tv)) {
            return tv.data;
        }
        if (tv.type == TypedValue.TYPE_STRING && tv.resourceId != 0) {
            return resources.getColorStateList(tv.resourceId, theme).getDefaultColor();
        }
        return defValue;
    }

    public ColorStateList getColorStateList(int index) {
        TypedValue tv = v(index);
        if (tv == null) {
            return null;
        }
        if (isInt(tv)) {
            return ColorStateList.valueOf(tv.data);
        }
        if (tv.resourceId != 0) {
            return resources.getColorStateList(tv.resourceId, theme);
        }
        return null;
    }

    public int getInteger(int index, int defValue) {
        TypedValue tv = v(index);
        if (tv == null) {
            return defValue;
        }
        if (isInt(tv)) {
            return tv.data;
        }
        return defValue;
    }

    public float getDimension(int index, float defValue) {
        TypedValue tv = v(index);
        if (tv == null) {
            return defValue;
        }
        if (tv.type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimension(tv.data, resources.getDisplayMetrics());
        }
        return defValue;
    }

    public int getDimensionPixelOffset(int index, int defValue) {
        TypedValue tv = v(index);
        if (tv == null) {
            return defValue;
        }
        if (tv.type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimensionPixelOffset(tv.data, resources.getDisplayMetrics());
        }
        return defValue;
    }

    public int getDimensionPixelSize(int index, int defValue) {
        TypedValue tv = v(index);
        if (tv == null) {
            return defValue;
        }
        if (tv.type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimensionPixelSize(tv.data, resources.getDisplayMetrics());
        }
        return defValue;
    }

    /// A layout dimension may be a size or one of the `match_parent` /
    /// `wrap_content` constants.
    public int getLayoutDimension(int index, String name) {
        TypedValue tv = v(index);
        if (tv == null) {
            throw new UnsupportedOperationException(getPositionDescription() + ": You must supply a " + name
                    + " attribute.");
        }
        return layoutDimension(tv, 0);
    }

    public int getLayoutDimension(int index, int defValue) {
        TypedValue tv = v(index);
        return tv == null ? defValue : layoutDimension(tv, defValue);
    }

    private int layoutDimension(TypedValue tv, int defValue) {
        if (isInt(tv)) {
            return tv.data;
        }
        if (tv.type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimensionPixelSize(tv.data, resources.getDisplayMetrics());
        }
        return defValue;
    }

    public float getFraction(int index, int base, int pbase, float defValue) {
        TypedValue tv = v(index);
        if (tv == null) {
            return defValue;
        }
        if (tv.type == TypedValue.TYPE_FRACTION) {
            return TypedValue.complexToFraction(tv.data, base, pbase);
        }
        return defValue;
    }

    public int getResourceId(int index, int defValue) {
        TypedValue tv = index < 0 || index >= values.length ? null : values[index];
        if (tv == null) {
            return defValue;
        }
        if (tv.resourceId != 0) {
            return tv.resourceId;
        }
        if (tv.type == TypedValue.TYPE_REFERENCE && tv.data != 0) {
            return tv.data;
        }
        return defValue;
    }

    public int getSourceResourceId(int index, int defValue) {
        return defValue;
    }

    public int getThemeAttributeId(int index, int defValue) {
        return defValue;
    }

    public Drawable getDrawable(int index) {
        TypedValue tv = v(index);
        if (tv == null) {
            return null;
        }
        if (isInt(tv) && tv.isColorType()) {
            return new ColorDrawable(tv.data);
        }
        if (tv.resourceId != 0) {
            return resources.getDrawable(tv.resourceId, theme);
        }
        if (tv.type == TypedValue.TYPE_REFERENCE && tv.data != 0) {
            return resources.getDrawable(tv.data, theme);
        }
        return null;
    }

    public CharSequence[] getTextArray(int index) {
        int id = getResourceId(index, 0);
        return id == 0 ? null : resources.getTextArray(id);
    }

    public boolean getValue(int index, TypedValue outValue) {
        TypedValue tv = index < 0 || index >= values.length ? null : values[index];
        if (tv == null) {
            return false;
        }
        outValue.setTo(tv);
        return tv.type != TypedValue.TYPE_NULL;
    }

    public int getType(int index) {
        TypedValue tv = index < 0 || index >= values.length ? null : values[index];
        return tv == null ? TypedValue.TYPE_NULL : tv.type;
    }

    public boolean hasValue(int index) {
        return v(index) != null;
    }

    public boolean hasValueOrEmpty(int index) {
        TypedValue tv = index < 0 || index >= values.length ? null : values[index];
        return tv != null && (tv.type != TypedValue.TYPE_NULL || tv.data == TypedValue.DATA_NULL_EMPTY);
    }

    public TypedValue peekValue(int index) {
        return v(index);
    }

    public String getPositionDescription() {
        return "<internal>";
    }

    public int getChangingConfigurations() {
        return 0;
    }

    public void recycle() {
        // Nothing is pooled.
    }

    public void close() {
        recycle();
    }

    /// The attr id requested at `index`.
    int attrAt(int index) {
        return attrs == null ? 0 : attrs[index];
    }

    @Override
    public String toString() {
        return "TypedArray[" + values.length + "]";
    }
}
