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
package com.codename1.impl.pdf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/// A parsed dictionary. Values are the parsed objects: [PdfName],
/// [PdfString], [PdfRef], [PdfDict], [PdfStream], `java.util.List`,
/// `Integer`, `Double`, `Boolean`, or null for the null object. The typed
/// getters resolve references through the owning file.
@com.codename1.impl.SharedWithBackend
public class PdfDict {
    private final Map<String, Object> map = new HashMap<String, Object>();
    PdfFile file;

    /// Sets `key` (without the slash) to `value`.
    public void put(String key, Object value) {
        map.put(key, value);
    }

    /// The value of `key` with references resolved, or null.
    public Object get(String key) {
        Object v = map.get(key);
        return file != null ? file.resolve(v) : v;
    }

    /// The value of `key` exactly as written, references unresolved.
    public Object getRaw(String key) {
        return map.get(key);
    }

    /// True when the dictionary has `key`.
    public boolean has(String key) {
        return map.containsKey(key);
    }

    /// The keys.
    public Iterator<String> keys() {
        return map.keySet().iterator();
    }

    /// The value of `key` when it is a dictionary (or the dictionary of a
    /// stream), null otherwise.
    public PdfDict getDict(String key) {
        Object v = get(key);
        return v instanceof PdfDict ? (PdfDict) v : null;
    }

    /// The value of `key` when it is an array, null otherwise.
    @SuppressWarnings("unchecked")
    public java.util.List<Object> getArray(String key) {
        Object v = get(key);
        return v instanceof java.util.List ? (java.util.List<Object>) v : null;
    }

    /// The name of `key` without the slash, or null.
    public String getName(String key) {
        Object v = get(key);
        return v instanceof PdfName ? ((PdfName) v).value : null;
    }

    /// The number of `key`, or `def`.
    public double getNumber(String key, double def) {
        Object v = get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    /// The integer of `key`, or `def`.
    public int getInt(String key, int def) {
        Object v = get(key);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    /// The text of string `key`, or null.
    public String getText(String key) {
        Object v = get(key);
        return v instanceof PdfString ? ((PdfString) v).toText() : null;
    }

    /// The owning file, null for a dictionary built by hand.
    public PdfFile getFile() {
        return file;
    }
}
