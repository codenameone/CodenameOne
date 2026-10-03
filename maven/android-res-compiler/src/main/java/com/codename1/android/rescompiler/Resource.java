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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// One resource -- `string/app_name`, `layout/main` -- with every configured
/// variant the res tree gave it.
public final class Resource {

    /// What a variant holds once encoded.
    public abstract static class Item {
        public final String file;
        public final int line;

        Item(String file, int line) {
            this.file = file;
            this.line = line;
        }
    }

    /// A single value: a string, a color, a dimension, a reference.
    public static final class Simple extends Item {
        public final String raw;
        /// Format the source element implies (`<dimen>` -> dimension), used
        /// in place of an attr declaration when encoding.
        public final AttrDef format;
        public Value value;

        public Simple(String file, int line, String raw, AttrDef format) {
            super(file, line);
            this.raw = raw;
            this.format = format;
        }

        public Simple(String file, int line, Value value) {
            super(file, line);
            this.raw = value.string;
            this.format = null;
            this.value = value;
        }
    }

    /// One `key = value` member of a bag before encoding.
    public static final class RawEntry {
        /// Attr name for a style item, quantity for plurals, null for arrays.
        public final String key;
        public final String raw;
        public final int line;
        public final AttrDef format;

        public RawEntry(String key, String raw, int line, AttrDef format) {
            this.key = key;
            this.raw = raw;
            this.line = line;
            this.format = format;
        }
    }

    /// A style, an array or plurals: an ordered map from integer keys to
    /// values. Style keys are attr ids, array keys are indexes, plural keys
    /// are the quantity codes in [#QUANTITIES].
    public static final class Bag extends Item {
        public final String parentRaw;
        /// True when the parent comes from the style's dotted name
        /// (`Theme.App.Dark` -> `Theme.App`) rather than a parent attribute.
        public boolean implicitParent;
        public final List<RawEntry> entries = new ArrayList<RawEntry>();
        public int parentId;
        public int[] keys;
        public Value[] values;

        public Bag(String file, int line, String parentRaw) {
            super(file, line);
            this.parentRaw = parentRaw;
        }
    }

    /// A compiled XML file: a layout, a drawable, a menu.
    public static final class Xml extends Item {
        public final RawNode root;
        public XmlTree compiled;

        public Xml(String file, int line, RawNode root) {
            super(file, line);
            this.root = root;
        }
    }

    /// A file that is shipped as is (an image, a font, a raw resource) under
    /// a flat name, because Codename One resources live in one directory.
    public static final class FileRes extends Item {
        public final File source;
        public final String flatName;

        public FileRes(String file, File source, String flatName) {
            super(file, 0);
            this.source = source;
            this.flatName = flatName;
        }
    }

    static final java.util.List<String> QUANTITIES = java.util.Collections.unmodifiableList(
            java.util.Arrays.asList("zero", "one", "two", "few", "many", "other"));

    public static final class Variant {
        public final ResConfig config;
        public final Item item;

        public Variant(ResConfig config, Item item) {
            this.config = config;
            this.item = item;
        }
    }

    public final ResType type;
    public final String name;
    public int id;
    public final List<Variant> variants = new ArrayList<Variant>();

    public Resource(ResType type, String name) {
        this.type = type;
        this.name = name;
    }

    /// The variant for `canonical`, or null.
    public Variant variant(String canonical) {
        for (Variant v : variants) {
            if (v.config.canonical().equals(canonical)) {
                return v;
            }
        }
        return null;
    }
}
