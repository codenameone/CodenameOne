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
package com.codename1.android.rescompiler;

/// Turns the text of a resource value -- an XML attribute, a style item, a
/// `<dimen>` body -- into a typed [Value], the way aapt2 does: references and
/// theme attributes first, then whatever the target attribute's declared
/// formats allow, in aapt's order of preference.
public final class ValueEncoder {

    /// Callback for references that do not resolve, so the caller can attach a
    /// file and line.
    public interface Problems {
        void error(String code, String message);

        void warning(String code, String message);
    }

    private final SymbolTable symbols;
    /// The symbol-table key of the package being compiled: `app`, or
    /// `android` when compiling the framework.
    private final String localPackage;

    public ValueEncoder(SymbolTable symbols, String localPackage) {
        this.symbols = symbols;
        this.localPackage = localPackage;
    }

    /// Encodes `raw` for an attribute with declaration `def` (null when the
    /// attribute is unknown, in which case the type is inferred from the text).
    public Value encode(String raw, AttrDef def, Problems problems) {
        if (raw == null) {
            return new Value(Value.TYPE_NULL, Value.DATA_NULL_UNDEFINED, null);
        }
        String t = raw.trim();
        boolean allowsString = def == null || def.allows(AttrDef.FORMAT_STRING);
        if (t.length() > 0 && (t.charAt(0) == '@' || t.charAt(0) == '?')) {
            Value ref = encodeReference(t, problems);
            if (ref != null) {
                return ref;
            }
            if (!allowsString) {
                return new Value(Value.TYPE_NULL, Value.DATA_NULL_UNDEFINED, raw);
            }
            return Value.string(AndroidStrings.unescape(raw));
        }
        if (def != null && (def.formats & AttrDef.FORMAT_ENUM) != 0 && def.enums.containsKey(t)) {
            return new Value(Value.TYPE_INT_DEC, def.enums.get(t), raw);
        }
        if (def != null && (def.formats & AttrDef.FORMAT_FLAGS) != 0 && !def.flags.isEmpty()) {
            int bits = 0;
            boolean all = true;
            for (String part : t.split("\\|")) {
                Integer v = def.flags.get(part.trim());
                if (v == null) {
                    all = false;
                    break;
                }
                bits |= v;
            }
            if (all) {
                return new Value(Value.TYPE_INT_HEX, bits, raw);
            }
        }
        if (def == null || def.allows(AttrDef.FORMAT_COLOR)) {
            Value c = encodeColor(t, raw);
            if (c != null) {
                return c;
            }
        }
        if (def == null || def.allows(AttrDef.FORMAT_DIMENSION)) {
            Value d = encodeDimension(t, raw);
            if (d != null) {
                return d;
            }
        }
        if (def == null || def.allows(AttrDef.FORMAT_FRACTION)) {
            Value f = encodeFraction(t, raw);
            if (f != null) {
                return f;
            }
        }
        if (def == null || def.allows(AttrDef.FORMAT_INTEGER)) {
            Value i = encodeInteger(t, raw);
            if (i != null) {
                return i;
            }
        }
        if (def == null || def.allows(AttrDef.FORMAT_BOOLEAN)) {
            if (t.equals("true") || t.equals("false")) {
                return Value.bool(t.equals("true"), raw);
            }
        }
        if (def == null || def.allows(AttrDef.FORMAT_FLOAT)) {
            Value f = encodeFloat(t, raw);
            if (f != null) {
                return f;
            }
        }
        if (!allowsString && problems != null) {
            problems.warning("W0101", "'" + raw + "' is not a valid value for "
                    + def.name + " (" + AttrDef.formatsToString(def.formats) + ")");
        }
        return Value.string(AndroidStrings.unescape(raw));
    }

    /// `@[+][package:]type/name`, `@null`, `@empty`, `?[package:][attr/]name`.
    /// Returns null if `t` is not reference syntax at all; reports and answers
    /// a null value if it is but names nothing.
    public Value encodeReference(String t, Problems problems) {
        if (t.equals("@null")) {
            return new Value(Value.TYPE_REFERENCE, 0, t);
        }
        if (t.equals("@empty")) {
            return new Value(Value.TYPE_NULL, Value.DATA_NULL_EMPTY, t);
        }
        boolean attr = t.charAt(0) == '?';
        String body = t.substring(1);
        if (body.startsWith("+")) {
            body = body.substring(1);
        }
        if (body.startsWith("*")) {
            // @*android:type/name -- private framework resource; same lookup.
            body = body.substring(1);
        }
        String pkg = localPackage;
        int colon = body.indexOf(':');
        int slash = body.indexOf('/');
        if (colon >= 0 && (slash < 0 || colon < slash)) {
            pkg = normalizePackage(body.substring(0, colon));
            body = body.substring(colon + 1);
            slash = body.indexOf('/');
        }
        String typeName;
        String name;
        if (slash < 0) {
            if (!attr) {
                return null;
            }
            typeName = "attr";
            name = body;
        } else {
            typeName = body.substring(0, slash);
            name = body.substring(slash + 1);
        }
        ResType type = ResType.fromTag(typeName);
        if (type == null || name.length() == 0) {
            if (problems != null) {
                problems.error("E0101", "'" + t + "' is not a valid resource reference");
            }
            return new Value(Value.TYPE_NULL, Value.DATA_NULL_UNDEFINED, t);
        }
        // Style names keep their dots here; only the R field spelling turns
        // them into underscores.
        int id = symbols.get(pkg, type, name);
        if (id == 0) {
            if (problems != null) {
                problems.error("E0102", "resource " + (pkg.equals(localPackage) ? "" : pkg + ":")
                        + type.tag + "/" + name + " not found (from '" + t + "')");
            }
            return new Value(Value.TYPE_NULL, Value.DATA_NULL_UNDEFINED, t);
        }
        return new Value(attr ? Value.TYPE_ATTRIBUTE : Value.TYPE_REFERENCE, id, t);
    }

    /// Maps the package spellings a reference may use onto symbol-table keys.
    /// Library and application packages are all merged into the local table,
    /// exactly as an Android build merges them into the application's R.
    public String normalizePackage(String p) {
        if (p.equals("android")) {
            return SymbolTable.FRAMEWORK;
        }
        return localPackage;
    }

    public static Value encodeColor(String t, String raw) {
        if (t.length() < 2 || t.charAt(0) != '#') {
            return null;
        }
        String hex = t.substring(1);
        for (int i = 0; i < hex.length(); i++) {
            if (Character.digit(hex.charAt(i), 16) < 0) {
                return null;
            }
        }
        long v;
        int type;
        switch (hex.length()) {
            case 3: {
                int r = Character.digit(hex.charAt(0), 16);
                int g = Character.digit(hex.charAt(1), 16);
                int b = Character.digit(hex.charAt(2), 16);
                v = 0xff000000L | ((long) r * 0x11 << 16) | ((long) g * 0x11 << 8) | ((long) b * 0x11);
                type = Value.TYPE_INT_COLOR_RGB4;
                break;
            }
            case 4: {
                int a = Character.digit(hex.charAt(0), 16);
                int r = Character.digit(hex.charAt(1), 16);
                int g = Character.digit(hex.charAt(2), 16);
                int b = Character.digit(hex.charAt(3), 16);
                v = ((long) a * 0x11 << 24) | ((long) r * 0x11 << 16) | ((long) g * 0x11 << 8) | ((long) b * 0x11);
                type = Value.TYPE_INT_COLOR_ARGB4;
                break;
            }
            case 6:
                v = 0xff000000L | Long.parseLong(hex, 16);
                type = Value.TYPE_INT_COLOR_RGB8;
                break;
            case 8:
                v = Long.parseLong(hex, 16);
                type = Value.TYPE_INT_COLOR_ARGB8;
                break;
            default:
                return null;
        }
        return new Value(type, (int) v, raw);
    }

    public static Value encodeDimension(String t, String raw) {
        String[] units = {"dip", "dp", "sp", "px", "pt", "in", "mm"};
        int[] codes = {Value.COMPLEX_UNIT_DIP, Value.COMPLEX_UNIT_DIP, Value.COMPLEX_UNIT_SP,
                Value.COMPLEX_UNIT_PX, Value.COMPLEX_UNIT_PT, Value.COMPLEX_UNIT_IN, Value.COMPLEX_UNIT_MM};
        for (int i = 0; i < units.length; i++) {
            if (t.endsWith(units[i])) {
                Float f = parseFloat(t.substring(0, t.length() - units[i].length()).trim());
                if (f == null) {
                    return null;
                }
                return new Value(Value.TYPE_DIMENSION, Value.floatToComplex(f, codes[i]), raw);
            }
        }
        return null;
    }

    public static Value encodeFraction(String t, String raw) {
        int unit;
        String num;
        if (t.endsWith("%p")) {
            unit = Value.COMPLEX_UNIT_FRACTION_PARENT;
            num = t.substring(0, t.length() - 2);
        } else if (t.endsWith("%")) {
            unit = Value.COMPLEX_UNIT_FRACTION;
            num = t.substring(0, t.length() - 1);
        } else {
            return null;
        }
        Float f = parseFloat(num.trim());
        if (f == null) {
            return null;
        }
        return new Value(Value.TYPE_FRACTION, Value.floatToComplex(f / 100f, unit), raw);
    }

    public static Value encodeInteger(String t, String raw) {
        try {
            if (t.startsWith("0x") || t.startsWith("0X")) {
                return new Value(Value.TYPE_INT_HEX, (int) Long.parseLong(t.substring(2), 16), raw);
            }
            if (t.length() == 0) {
                return null;
            }
            for (int i = 0; i < t.length(); i++) {
                char c = t.charAt(i);
                if (!(c >= '0' && c <= '9') && !(i == 0 && (c == '-' || c == '+'))) {
                    return null;
                }
            }
            return new Value(Value.TYPE_INT_DEC, Integer.parseInt(t.startsWith("+") ? t.substring(1) : t), raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Value encodeFloat(String t, String raw) {
        Float f = parseFloat(t);
        if (f == null) {
            return null;
        }
        return new Value(Value.TYPE_FLOAT, Float.floatToIntBits(f), raw);
    }

    private static Float parseFloat(String s) {
        if (s.length() == 0) {
            return null;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || c == '.' || ((c == '-' || c == '+') && i == 0)
                    || c == 'e' || c == 'E';
            if (!ok) {
                return null;
            }
        }
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
