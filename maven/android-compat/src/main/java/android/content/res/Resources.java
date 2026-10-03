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

import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import com.codename1.androidcompat.runtime.CompiledAttributeSet;
import com.codename1.androidcompat.runtime.DrawableInflater;
import com.codename1.androidcompat.runtime.PluralRules;
import com.codename1.androidcompat.runtime.ResTable;
import com.codename1.androidcompat.runtime.ResValue;
import com.codename1.androidcompat.runtime.ResourceManager;
import com.codename1.androidcompat.runtime.XmlNode;
import com.codename1.ui.Display;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/// Access to an application's resources: the values, styles and compiled XML
/// the build put in the resource table, resolved for the current device
/// configuration exactly as Android resolves them.
public class Resources {

    public static final int ID_NULL = 0;

    /// Thrown when a resource id does not exist, as on Android.
    public static class NotFoundException extends RuntimeException {
        public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }

        public NotFoundException(String name, Exception cause) {
            super(name);
        }
    }

    private static Resources system;
    private final ResourceManager manager;

    public Resources(ResourceManager manager) {
        this.manager = manager;
    }

    /// Resources limited to the framework's own; on Android this has no access
    /// to application resources, and here it simply shares the manager.
    public static Resources getSystem() {
        if (system == null) {
            system = new Resources(ResourceManager.get());
        }
        return system;
    }

    public ResourceManager getManager() {
        return manager;
    }

    // ------------------------------------------------------------ core lookup

    ResTable.Entry entry(int id) {
        ResTable.Entry e = manager.entry(id);
        if (e == null) {
            throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id));
        }
        return e;
    }

    /// The best variant of `id`, never null.
    public Object item(int id) {
        Object it = entry(id).best(manager.device());
        if (it == null) {
            throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id)
                    + " has no variant for the current configuration");
        }
        return it;
    }

    /// Fills `out` from `v`, following theme attributes through `theme` and,
    /// when `resolveRefs`, references to simple values. A reference to a bag
    /// (style, array) stays a reference; one to a file becomes a string naming
    /// it, as on Android.
    public void resolve(ResValue v, TypedValue out, Theme theme, boolean resolveRefs) {
        resolve(v, out, theme, resolveRefs, 0);
    }

    private void resolve(ResValue v, TypedValue out, Theme theme, boolean resolveRefs, int depth) {
        out.type = v.type;
        out.data = v.data;
        out.string = v.type == TypedValue.TYPE_STRING ? v.string : null;
        out.assetCookie = 0;
        if (depth > 20) {
            out.type = TypedValue.TYPE_NULL;
            return;
        }
        if (v.type == TypedValue.TYPE_ATTRIBUTE) {
            if (theme == null || !theme.resolveAttribute(v.data, out, resolveRefs, depth + 1)) {
                out.type = TypedValue.TYPE_NULL;
                out.data = TypedValue.DATA_NULL_UNDEFINED;
            }
            return;
        }
        if (v.type == TypedValue.TYPE_REFERENCE && resolveRefs && v.data != 0) {
            int id = v.data;
            ResTable.Entry e = manager.entry(id);
            if (e == null) {
                return;
            }
            Object it = e.best(manager.device());
            if (it instanceof ResValue) {
                resolve((ResValue) it, out, theme, true, depth + 1);
                if (out.resourceId == 0 || ((ResValue) it).type != TypedValue.TYPE_REFERENCE) {
                    out.resourceId = id;
                }
            } else if (it instanceof ResTable.FileRef) {
                out.type = TypedValue.TYPE_STRING;
                out.string = "res/" + e.name + "/" + ((ResTable.FileRef) it).name;
                out.data = 0;
                out.resourceId = id;
            } else if (it instanceof XmlNode) {
                out.type = TypedValue.TYPE_STRING;
                out.string = "res/" + e.name + ".xml";
                out.data = 0;
                out.resourceId = id;
            } else {
                out.resourceId = id;
            }
        }
    }

    public void getValue(int id, TypedValue outValue, boolean resolveRefs) {
        Object it = item(id);
        outValue.resourceId = 0;
        if (it instanceof ResValue) {
            resolve((ResValue) it, outValue, null, resolveRefs);
            if (outValue.resourceId == 0) {
                outValue.resourceId = id;
            }
        } else {
            resolve(new ResValue(TypedValue.TYPE_REFERENCE, id, null), outValue, null, true);
        }
    }

    public void getValue(String name, TypedValue outValue, boolean resolveRefs) {
        int id = getIdentifier(name, null, null);
        if (id == 0) {
            throw new NotFoundException("String resource name " + name);
        }
        getValue(id, outValue, resolveRefs);
    }

    private TypedValue value(int id) {
        TypedValue tv = new TypedValue();
        getValue(id, tv, true);
        return tv;
    }

    // ------------------------------------------------------------ strings

    public CharSequence getText(int id) {
        TypedValue tv = value(id);
        if (tv.type == TypedValue.TYPE_STRING) {
            return tv.string;
        }
        CharSequence s = tv.coerceToString();
        if (s == null) {
            throw new NotFoundException("String resource ID #0x" + Integer.toHexString(id));
        }
        return s;
    }

    public CharSequence getText(int id, CharSequence def) {
        try {
            return getText(id);
        } catch (NotFoundException e) {
            return def;
        }
    }

    public String getString(int id) {
        return getText(id).toString();
    }

    public String getString(int id, Object... formatArgs) {
        return String.format(getString(id), formatArgs);
    }

    public CharSequence getQuantityText(int id, int quantity) {
        Object it = item(id);
        if (!(it instanceof ResTable.Bag)) {
            throw new NotFoundException("Plurals resource ID #0x" + Integer.toHexString(id));
        }
        ResTable.Bag b = (ResTable.Bag) it;
        int cat = PluralRules.select(manager.device().language, quantity);
        ResValue v = b.get(cat);
        if (v == null && quantity == 0) {
            v = b.get(PluralRules.ZERO);
        }
        if (v == null) {
            v = b.get(PluralRules.OTHER);
        }
        if (v == null && b.values.length > 0) {
            v = b.values[0];
        }
        if (v == null) {
            throw new NotFoundException("Plurals resource ID #0x" + Integer.toHexString(id) + " is empty");
        }
        TypedValue tv = new TypedValue();
        resolve(v, tv, null, true);
        return tv.type == TypedValue.TYPE_STRING ? tv.string : tv.coerceToString();
    }

    public String getQuantityString(int id, int quantity) {
        return getQuantityText(id, quantity).toString();
    }

    public String getQuantityString(int id, int quantity, Object... formatArgs) {
        return String.format(getQuantityString(id, quantity), formatArgs);
    }

    private ResTable.Bag array(int id) {
        Object it = item(id);
        if (!(it instanceof ResTable.Bag)) {
            throw new NotFoundException("Array resource ID #0x" + Integer.toHexString(id));
        }
        return (ResTable.Bag) it;
    }

    public CharSequence[] getTextArray(int id) {
        ResTable.Bag b = array(id);
        CharSequence[] out = new CharSequence[b.values.length];
        TypedValue tv = new TypedValue();
        for (int i = 0; i < out.length; i++) {
            resolve(b.values[i], tv, null, true);
            out[i] = tv.type == TypedValue.TYPE_STRING ? tv.string : tv.coerceToString();
        }
        return out;
    }

    public String[] getStringArray(int id) {
        CharSequence[] t = getTextArray(id);
        String[] out = new String[t.length];
        for (int i = 0; i < t.length; i++) {
            out[i] = t[i] == null ? null : t[i].toString();
        }
        return out;
    }

    public int[] getIntArray(int id) {
        ResTable.Bag b = array(id);
        int[] out = new int[b.values.length];
        TypedValue tv = new TypedValue();
        for (int i = 0; i < out.length; i++) {
            resolve(b.values[i], tv, null, true);
            out[i] = tv.data;
        }
        return out;
    }

    public TypedArray obtainTypedArray(int id) {
        ResTable.Bag b = array(id);
        TypedValue[] values = new TypedValue[b.values.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = new TypedValue();
            resolve(b.values[i], values[i], null, true);
        }
        return new TypedArray(this, null, values, null);
    }

    // ------------------------------------------------------------ simple values

    public int getColor(int id) {
        return getColor(id, null);
    }

    public int getColor(int id, Theme theme) {
        Object it = item(id);
        if (it instanceof XmlNode) {
            return getColorStateList(id, theme).getDefaultColor();
        }
        TypedValue tv = new TypedValue();
        resolve((ResValue) it, tv, theme, true);
        if (tv.type >= TypedValue.TYPE_FIRST_INT && tv.type <= TypedValue.TYPE_LAST_INT) {
            return tv.data;
        }
        if (tv.type == TypedValue.TYPE_STRING && tv.resourceId != 0) {
            return getColorStateList(tv.resourceId, theme).getDefaultColor();
        }
        throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " type #0x"
                + Integer.toHexString(tv.type) + " is not valid as a color");
    }

    private final Map<Integer, ColorStateList> cslCache = new HashMap<Integer, ColorStateList>();
    private int cslGeneration = -1;

    public ColorStateList getColorStateList(int id) {
        return getColorStateList(id, null);
    }

    public ColorStateList getColorStateList(int id, Theme theme) {
        Object it = item(id);
        if (it instanceof XmlNode) {
            boolean cacheable = theme == null;
            if (cacheable) {
                if (cslGeneration != manager.device().generation) {
                    cslCache.clear();
                    cslGeneration = manager.device().generation;
                }
                ColorStateList c = cslCache.get(Integer.valueOf(id));
                if (c != null) {
                    return c;
                }
            }
            ColorStateList c = DrawableInflater.inflateColorStateList(this, (XmlNode) it, theme);
            if (cacheable) {
                cslCache.put(Integer.valueOf(id), c);
            }
            return c;
        }
        TypedValue tv = new TypedValue();
        resolve((ResValue) it, tv, theme, true);
        if (tv.type == TypedValue.TYPE_STRING && tv.resourceId != 0 && tv.resourceId != id) {
            return getColorStateList(tv.resourceId, theme);
        }
        return ColorStateList.valueOf(tv.data);
    }

    public float getDimension(int id) {
        TypedValue tv = value(id);
        if (tv.type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimension(tv.data, getDisplayMetrics());
        }
        if (tv.type == TypedValue.TYPE_FLOAT) {
            return tv.getFloat();
        }
        throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " is not a dimension");
    }

    public int getDimensionPixelSize(int id) {
        TypedValue tv = value(id);
        if (tv.type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimensionPixelSize(tv.data, getDisplayMetrics());
        }
        throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " is not a dimension");
    }

    public int getDimensionPixelOffset(int id) {
        TypedValue tv = value(id);
        if (tv.type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimensionPixelOffset(tv.data, getDisplayMetrics());
        }
        throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " is not a dimension");
    }

    public float getFraction(int id, int base, int pbase) {
        TypedValue tv = value(id);
        if (tv.type == TypedValue.TYPE_FRACTION) {
            return TypedValue.complexToFraction(tv.data, base, pbase);
        }
        throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " is not a fraction");
    }

    public float getFloat(int id) {
        TypedValue tv = value(id);
        if (tv.type == TypedValue.TYPE_FLOAT) {
            return tv.getFloat();
        }
        throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " is not a float");
    }

    public int getInteger(int id) {
        TypedValue tv = value(id);
        if (tv.type >= TypedValue.TYPE_FIRST_INT && tv.type <= TypedValue.TYPE_LAST_INT) {
            return tv.data;
        }
        throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " is not an integer");
    }

    public boolean getBoolean(int id) {
        TypedValue tv = value(id);
        if (tv.type >= TypedValue.TYPE_FIRST_INT && tv.type <= TypedValue.TYPE_LAST_INT) {
            return tv.data != 0;
        }
        throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " is not a boolean");
    }

    // ------------------------------------------------------------ drawables & files

    /// The font a `res/font` resource names: a font file, or the first font
    /// a font family XML lists.
    public android.graphics.Typeface getFont(int id) {
        Object it = item(id);
        if (it instanceof ResTable.FileRef) {
            return android.graphics.Typeface.fromFile(getResourceEntryName(id), ((ResTable.FileRef) it).name);
        }
        if (it instanceof com.codename1.androidcompat.runtime.XmlNode) {
            com.codename1.androidcompat.runtime.XmlNode n = (com.codename1.androidcompat.runtime.XmlNode) it;
            for (com.codename1.androidcompat.runtime.XmlNode f : n.children) {
                ResValue v = f.value(com.codename1.androidcompat.runtime.XmlNode.NS_ANDROID, "font");
                if (v == null) {
                    v = f.value(com.codename1.androidcompat.runtime.XmlNode.NS_APP, "font");
                }
                if (v != null && v.data != 0) {
                    return getFont(v.data);
                }
            }
        }
        throw new NotFoundException("Font resource ID #0x" + Integer.toHexString(id));
    }

    public Drawable getDrawable(int id) {
        return getDrawable(id, null);
    }

    public Drawable getDrawable(int id, Theme theme) {
        return DrawableInflater.load(this, id, theme);
    }

    public Drawable getDrawableForDensity(int id, int density) {
        return getDrawable(id, null);
    }

    public Drawable getDrawableForDensity(int id, int density, Theme theme) {
        return getDrawable(id, theme);
    }

    /// A parser over the compiled XML resource `id` (`res/xml`, or any
    /// other XML file resource).
    public XmlResourceParser getXml(int id) {
        return new com.codename1.androidcompat.runtime.CompiledXmlParser(getXmlNode(id));
    }

    public XmlResourceParser getLayout(int id) {
        return getXml(id);
    }

    public XmlResourceParser getAnimation(int id) {
        return getXml(id);
    }

    /// The compiled XML of a layout, drawable, menu or xml resource.
    public XmlNode getXmlNode(int id) {
        Object it = item(id);
        if (!(it instanceof XmlNode)) {
            throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " is not an XML resource");
        }
        return (XmlNode) it;
    }

    public InputStream openRawResource(int id) {
        Object it = item(id);
        if (!(it instanceof ResTable.FileRef)) {
            throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id) + " is not a file");
        }
        InputStream in = Display.getInstance().getResourceAsStream(getClass(), "/" + ((ResTable.FileRef) it).name);
        if (in == null) {
            throw new NotFoundException("File " + ((ResTable.FileRef) it).name + " is missing from the application");
        }
        return in;
    }

    public InputStream openRawResource(int id, TypedValue value) {
        getValue(id, value, true);
        return openRawResource(id);
    }

    // ------------------------------------------------------------ names

    public int getIdentifier(String name, String defType, String defPackage) {
        String pkg = defPackage;
        String type = defType;
        String n = name;
        if (n.startsWith("@")) {
            n = n.substring(1);
        }
        int colon = n.indexOf(':');
        if (colon >= 0) {
            pkg = n.substring(0, colon);
            n = n.substring(colon + 1);
        }
        int slash = n.indexOf('/');
        if (slash >= 0) {
            type = n.substring(0, slash);
            n = n.substring(slash + 1);
        }
        if (type == null) {
            return 0;
        }
        int id = manager.identifier(pkg, type, n);
        if (id == 0 && !"android".equals(pkg)) {
            id = manager.identifier(pkg, type, n.replace('_', '.'));
        }
        return id;
    }

    public String getResourceName(int id) {
        ResTable.Entry e = entry(id);
        return getResourcePackageName(id) + ":" + e.name;
    }

    public String getResourceEntryName(int id) {
        return entry(id).entryName();
    }

    public String getResourceTypeName(int id) {
        return entry(id).type();
    }

    public String getResourcePackageName(int id) {
        entry(id);
        if ((id >>> 24) == 0x01) {
            return "android";
        }
        com.codename1.androidcompat.runtime.AndroidRuntime rt = com.codename1.androidcompat.runtime.AndroidRuntime.getInstance();
        return rt == null ? "app" : rt.getPackageName();
    }

    // ------------------------------------------------------------ configuration

    public DisplayMetrics getDisplayMetrics() {
        return manager.metrics();
    }

    public Configuration getConfiguration() {
        return manager.configuration();
    }

    public void updateConfiguration(Configuration config, DisplayMetrics metrics) {
        // The configuration is the device's; it is refreshed from the display,
        // not set by the application.
    }

    public final Theme newTheme() {
        return new Theme();
    }

    public TypedArray obtainAttributes(AttributeSet set, int[] attrs) {
        return new Theme().obtainStyledAttributes(set, attrs, 0, 0);
    }

    // ------------------------------------------------------------ theme

    /// The attribute values of a theme: the styles applied to it, flattened.
    public final class Theme {

        private final Map<Integer, ResValue> values = new HashMap<Integer, ResValue>();
        /// Applied styles in order, as `resId` and `force` pairs, for
        /// [#setTo(Theme)] and [#rebase()].
        private int[] applied = new int[0];
        private boolean[] appliedForce = new boolean[0];

        Theme() {
        }

        public Resources getResources() {
            return Resources.this;
        }

        public void applyStyle(int resId, boolean force) {
            if (resId == 0) {
                return;
            }
            Map<Integer, ResValue> style = manager.style(resId);
            for (Map.Entry<Integer, ResValue> e : style.entrySet()) {
                if (force || !values.containsKey(e.getKey())) {
                    values.put(e.getKey(), e.getValue());
                }
            }
            int n = applied.length;
            int[] a = new int[n + 1];
            boolean[] f = new boolean[n + 1];
            System.arraycopy(applied, 0, a, 0, n);
            System.arraycopy(appliedForce, 0, f, 0, n);
            a[n] = resId;
            f[n] = force;
            applied = a;
            appliedForce = f;
        }

        public void setTo(Theme other) {
            values.clear();
            values.putAll(other.values);
            applied = other.applied.clone();
            appliedForce = other.appliedForce.clone();
        }

        /// Re-applies every style, picking up configuration-dependent variants.
        public void rebase() {
            int[] a = applied;
            boolean[] f = appliedForce;
            values.clear();
            applied = new int[0];
            appliedForce = new boolean[0];
            for (int i = 0; i < a.length; i++) {
                applyStyle(a[i], f[i]);
            }
        }

        /// The raw (unresolved) value of `attr` in this theme, or null.
        public ResValue rawValue(int attr) {
            return values.get(Integer.valueOf(attr));
        }

        public boolean resolveAttribute(int resid, TypedValue outValue, boolean resolveRefs) {
            outValue.resourceId = 0;
            return resolveAttribute(resid, outValue, resolveRefs, 0);
        }

        boolean resolveAttribute(int resid, TypedValue outValue, boolean resolveRefs, int depth) {
            ResValue v = values.get(Integer.valueOf(resid));
            if (v == null) {
                return false;
            }
            Resources.this.resolve(v, outValue, this, resolveRefs, depth);
            return true;
        }

        public TypedArray obtainStyledAttributes(int[] attrs) {
            return obtainStyledAttributes(null, attrs, 0, 0);
        }

        public TypedArray obtainStyledAttributes(int resId, int[] attrs) {
            return obtainStyledAttributes(null, attrs, 0, resId);
        }

        /// Android's precedence, attr by attr: the XML element's own value,
        /// then the `style=` it names, then the default style (`defStyleAttr`
        /// looked up in this theme, else `defStyleRes`), then the theme's
        /// value for the attr itself.
        public TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
            CompiledAttributeSet cas = set instanceof CompiledAttributeSet ? (CompiledAttributeSet) set : null;
            Map<Integer, ResValue> styleFromXml = null;
            if (cas != null) {
                int styleRes = cas.getStyleAttribute();
                if (styleRes != 0 && cas.isStyleAttributeThemeReference()) {
                    TypedValue tv = new TypedValue();
                    styleRes = resolveAttribute(styleRes, tv, true) ? (tv.resourceId != 0 ? tv.resourceId : tv.data) : 0;
                }
                if (styleRes != 0) {
                    styleFromXml = manager.style(styleRes);
                }
            }
            Map<Integer, ResValue> defStyle = null;
            if (defStyleAttr != 0) {
                TypedValue tv = new TypedValue();
                if (resolveAttribute(defStyleAttr, tv, true)) {
                    int sid = tv.type == TypedValue.TYPE_REFERENCE ? tv.data : tv.resourceId;
                    if (sid != 0) {
                        defStyle = manager.style(sid);
                    }
                }
            }
            if (defStyle == null && defStyleRes != 0) {
                defStyle = manager.style(defStyleRes);
            }
            TypedValue[] out = new TypedValue[attrs.length];
            boolean[] fromXml = new boolean[attrs.length];
            for (int i = 0; i < attrs.length; i++) {
                Integer key = Integer.valueOf(attrs[i]);
                ResValue v = null;
                if (cas != null) {
                    v = cas.valueForAttr(attrs[i]);
                    fromXml[i] = v != null;
                }
                if (v == null && styleFromXml != null) {
                    v = styleFromXml.get(key);
                }
                if (v == null && defStyle != null) {
                    v = defStyle.get(key);
                }
                if (v == null) {
                    v = values.get(key);
                }
                if (v == null) {
                    continue;
                }
                TypedValue tv = new TypedValue();
                Resources.this.resolve(v, tv, this, true);
                if (tv.type == TypedValue.TYPE_NULL && tv.data != TypedValue.DATA_NULL_EMPTY) {
                    continue;
                }
                out[i] = tv;
            }
            return new TypedArray(Resources.this, attrs, out, this, fromXml);
        }

        public Drawable getDrawable(int id) {
            return Resources.this.getDrawable(id, this);
        }

        public int getChangingConfigurations() {
            return 0;
        }

        public void dump(int priority, String tag, String prefix) {
            android.util.Log.println(priority, tag, prefix + "Theme with " + values.size() + " attributes");
        }
    }
}
