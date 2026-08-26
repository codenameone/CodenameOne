/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.build.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single source of truth for Codename One build hints.
 *
 * <p>A build hint is a {@code codename1.arg.<name>=<value>} entry that reaches
 * a builder as {@code BuildRequest.getArg(name, default)}. Historically the set
 * of hints was described in five places that drifted apart: a prose AsciiDoc
 * table in the developer guide, a runtime scraper of that table in the Settings
 * tool, a fifteen-entry schema in the simulator, a fourteen-entry separator map
 * in the Maven plugin, and a hand-written reference shipped to coding agents.
 * None of them was checked against the builders, so a hint could be documented
 * and unread, or read and undocumented, or simply misspelled in the reference
 * with nothing to catch it.</p>
 *
 * <p>This table replaces all five. It is Java rather than a data file because
 * its consumers span five classpaths with no JSON library in common &mdash; the
 * Java&nbsp;5 core, the Java&nbsp;7 JavaSE port, the Java&nbsp;8 Maven plugin
 * and build daemon, and the Java&nbsp;17 Settings tool &mdash; and because a
 * catalog that javac itself checks is the same argument the annotation feature
 * rests on.</p>
 *
 * <p><b>Keep this file in sync with the BuildDaemon copy.</b> Like
 * {@link PlatformFeatureCatalog}, this class is mirrored into the out-of-repo
 * build service; a single {@code .java} file is the sync unit.</p>
 *
 * <p>Registration is split into one method per group rather than a single
 * static block on purpose: a class initializer carrying every entry would
 * exceed the JVM's 64KB per-method bytecode limit.</p>
 */
public final class BuildHints {

    /** Prefix every build hint carries inside a settings or library properties file. */
    public static final String ARG_PREFIX = "codename1.arg.";

    private static final List<Hint> ENTRIES;
    private static final Map<String, Hint> BY_NAME;

    static {
        List<Hint> h = new ArrayList<Hint>();
        BuildHintsIos.register(h);
        BuildHintsAndroid.register(h);
        BuildHintsApple.register(h);
        BuildHintsDesktop.register(h);
        BuildHintsGeneral.register(h);
        BuildHintsDynamic.register(h);
        BuildHintsExternal.register(h);
        // The hints the annotations expose, rendered back into the catalog for
        // the consumers that cannot read bytecode -- the Settings editor above
        // all. A view of the annotations, not a second statement of them.
        BuildHintsFromAnnotations.register(h);

        Map<String, Hint> byName = new LinkedHashMap<String, Hint>();
        for (Hint entry : h) {
            if (byName.put(entry.name(), entry) != null) {
                throw new IllegalStateException("Duplicate build hint: " + entry.name());
            }
        }
        ENTRIES = Collections.unmodifiableList(h);
        BY_NAME = Collections.unmodifiableMap(byName);
    }

    private BuildHints() {
    }

    /** Every catalogued hint, in registration order. */
    public static List<Hint> entries() {
        return ENTRIES;
    }

    /**
     * Looks up a hint by its bare name.
     *
     * @param name the hint name, with or without the {@link #ARG_PREFIX}
     * @return the hint, or null when the catalog does not describe it
     */
    public static Hint byName(String name) {
        if (name == null) {
            return null;
        }
        return BY_NAME.get(strip(name));
    }

    /** Removes the {@link #ARG_PREFIX} if present. */
    public static String strip(String name) {
        if (name != null && name.startsWith(ARG_PREFIX)) {
            return name.substring(ARG_PREFIX.length());
        }
        return name;
    }

    /**
     * The string that joins two values of this hint when a cn1lib appends to a
     * project's value, and that splits an annotation's {@code String[]} back
     * into wire form.
     *
     * <p>Returns the empty string for a hint the catalog does not describe,
     * which is the historical bare-concatenation behaviour that the XML
     * fragment hints depend on.</p>
     */
    public static String separatorFor(String name) {
        Hint entry = byName(name);
        if (entry == null || entry.separator() == null) {
            return "";
        }
        return entry.separator();
    }

    /**
     * Resolves an alias to the hint whose value it overrides. A few Android
     * hints have a short {@code and.} spelling that takes precedence over the
     * {@code android.} one; both names denote a single effective setting, so
     * conflict detection has to collapse them.
     *
     * @return the aliased hint, or the argument itself when it is not an alias
     */
    public static Hint resolve(Hint entry) {
        if (entry == null || entry.aliasOf() == null) {
            return entry;
        }
        Hint target = byName(entry.aliasOf());
        return target == null ? entry : target;
    }

    /** The hint a name ultimately denotes, following an alias if there is one. */
    public static String canonicalName(String name) {
        Hint entry = byName(name);
        if (entry == null) {
            return strip(name);
        }
        return resolve(entry).name();
    }

    /**
     * The type vocabulary the Settings tool searches and validates by. Derived
     * so it can no longer drift from {@link HintType}.
     *
     * @return one of BOOLEAN, INTEGER, VERSION, ENUM, XML, PATH, URL, CSV,
     *         SECRET, TEXT
     */
    public static String settingsType(HintType type) {
        switch (type) {
            case BOOLEAN: return "BOOLEAN";
            case INT: return "INTEGER";
            case VERSION: return "VERSION";
            case ENUM: return "ENUM";
            case XML: return "XML";
            case PATH: return "PATH";
            case URL: return "URL";
            case STRING_LIST: return "CSV";
            case SECRET: return "SECRET";
            default: return "TEXT";
        }
    }

    /**
     * The widget the simulator's Build Hint editor renders. Derived so it can
     * no longer drift from {@link HintType}.
     *
     * @return one of TextField, TextArea, Checkbox, Select
     */
    public static String editorWidget(HintType type) {
        switch (type) {
            case BOOLEAN: return "Checkbox";
            case ENUM: return "Select";
            case TEXT_BLOCK:
            case STRING_LIST:
            case XML: return "TextArea";
            default: return "TextField";
        }
    }

    /**
     * One build hint.
     *
     * <p>Built fluently. Only {@link #name} is required; everything else
     * defaults to "plain string, no default, not annotated", which is the
     * correct shallow description of a hint nobody has curated yet.</p>
     */
    public static final class Hint {
        private final String name;
        private String aliasOf;
        private String deprecated;
        private HintGroup group = HintGroup.NONE;
        private String attr;
        private HintType type = HintType.STRING;
        private String enumName;
        private final List<String> values = new ArrayList<String>();
        private final Map<String, String> valueAliases = new LinkedHashMap<String, String>();
        private final List<String> valueLabels = new ArrayList<String>();
        private final List<String> valueConstants = new ArrayList<String>();
        private String unsetConstant;
        private String def;
        private String separator;
        private String platform = "general";
        private boolean dynamic;
        private String pattern;
        private final List<String> consumedBy = new ArrayList<String>();
        private boolean external;
        private boolean enterpriseOnly;
        private String link;
        private String doc = "";

        Hint(String name) {
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("Build hint name is required");
            }
            this.name = name;
        }

        /** Marks this hint as an override alias of another. */
        public Hint aliasOf(String other) {
            this.aliasOf = other;
            return this;
        }

        /** Records that this hint is deprecated, naming what replaces it. */
        public Hint deprecated(String reason) {
            this.deprecated = reason;
            return this;
        }

        /** Assigns the annotation type and the attribute name it is exposed as. */
        public Hint annotatedAs(HintGroup g, String attribute) {
            this.group = g;
            this.attr = attribute;
            return this;
        }

        /** Sets the group without exposing the hint as an annotation attribute. */
        public Hint group(HintGroup g) {
            this.group = g;
            return this;
        }

        public Hint type(HintType t) {
            this.type = t;
            return this;
        }

        /**
         * Extra wire values the runtime accepts for an already-declared domain,
         * each mapped to the canonical value it means.
         *
         * <p>Deliberately separate from {@link #values}: these do NOT become enum
         * constants, because two constants for one behaviour is an API that asks
         * a question with no right answer. They exist so that validation accepts
         * what the runtime accepts &mdash; {@code ios.themeMode=flat} and
         * {@code and.themeMode=material} are real, documented spellings, and
         * rejecting them told a developer their working configuration was
         * invalid &mdash; and so a migration can render one as the canonical
         * constant rather than refusing it.</p>
         *
         * @param pairs alias then canonical, repeated
         */
        public Hint valueAliases(String... pairs) {
            if (pairs.length % 2 != 0) {
                throw new IllegalArgumentException(
                        "Build hint " + name + ": valueAliases takes alias/canonical pairs");
            }
            for (int i = 0; i < pairs.length; i += 2) {
                if (!values.contains(pairs[i + 1])) {
                    throw new IllegalArgumentException("Build hint " + name + " aliases "
                            + pairs[i] + " to " + pairs[i + 1] + ", which is not in its domain");
                }
                valueAliases.put(pairs[i], pairs[i + 1]);
            }
            return this;
        }

        /**
         * Declares a closed value domain. Values are in <em>wire</em> form, i.e.
         * exactly what the builder compares against, never the enum constant
         * name.
         */
        public Hint values(String enumTypeName, String... wireValues) {
            this.type = HintType.ENUM;
            this.enumName = enumTypeName;
            this.values.clear();
            for (String v : wireValues) {
                if (v.indexOf(',') >= 0) {
                    throw new IllegalArgumentException(
                            "Build hint " + name + " value '" + v + "' contains a comma, which the "
                                    + "simulator's Build Hint editor uses to delimit its value list");
                }
                this.values.add(v);
            }
            return this;
        }

        /** Optional human labels for the value domain, parallel to the values. */
        /// The enum CONSTANT names, positionally matching {@link #values()}.
        ///
        /// Recorded rather than derived from the wire value: `Toggle.ON` sends
        /// `true`, and upper-casing the value would look for a constant called
        /// TRUE that does not exist. The annotation processor resolves a
        /// constant through this table, so a mismatch is not a wrong value on
        /// the wire but a build error.
        public Hint valueConstants(String... constants) {
            this.valueConstants.clear();
            Collections.addAll(this.valueConstants, constants);
            return this;
        }

        /// The constant that means "not set", which sends nothing at all.
        public Hint unsetConstant(String constant) {
            this.unsetConstant = constant;
            return this;
        }

        public Hint valueLabels(String... labels) {
            this.valueLabels.clear();
            Collections.addAll(this.valueLabels, labels);
            return this;
        }

        /**
         * The builder's own default, i.e. the second argument of the
         * {@code getArg} call that reads this hint.
         */
        public Hint def(String value) {
            this.def = value;
            return this;
        }

        /**
         * The string that joins appended values. Empty string means the values
         * abut directly, which is what the XML-fragment hints want.
         */
        public Hint separator(String sep) {
            this.separator = sep;
            return this;
        }

        public Hint platform(String p) {
            this.platform = p;
            return this;
        }

        /** Declares an open-ended family of hints matching a name pattern. */
        public Hint dynamic(String namePattern) {
            this.dynamic = true;
            this.pattern = namePattern;
            return this;
        }

        /** Names the builders or mojos that read this hint. */
        public Hint consumedBy(String... classSimpleNames) {
            Collections.addAll(this.consumedBy, classSimpleNames);
            return this;
        }

        /**
         * Marks a hint that is read outside this repository, by a build-daemon
         * lane whose source is not mirrored here. Such a hint has no in-repo
         * consumer and that is not evidence it is dead.
         */
        public Hint external() {
            this.external = true;
            return this;
        }

        public Hint enterpriseOnly() {
            this.enterpriseOnly = true;
            return this;
        }

        public Hint link(String url) {
            this.link = url;
            return this;
        }

        /** One paragraph, reused verbatim by the docs, the javadoc and the UI. */
        public Hint doc(String text) {
            this.doc = text == null ? "" : text;
            return this;
        }

        public String name() { return name; }
        public String aliasOf() { return aliasOf; }
        public String deprecated() { return deprecated; }
        public HintGroup group() { return group; }
        public String attr() { return attr; }
        public HintType type() { return type; }
        public String enumName() { return enumName; }
        public List<String> values() { return Collections.unmodifiableList(values); }

        /** Accepted spellings that are not their own value, alias to canonical. */
        public Map<String, String> valueAliases() {
            return Collections.unmodifiableMap(valueAliases);
        }

        /**
         * The canonical form of {@code value}, or null when the domain does not
         * accept it. Case-insensitive, matching every reader of these hints.
         */
        public String canonicalValue(String value) {
            if (value == null) {
                return null;
            }
            for (String allowed : values) {
                if (allowed.equalsIgnoreCase(value)) {
                    return allowed;
                }
            }
            for (Map.Entry<String, String> e : valueAliases.entrySet()) {
                if (e.getKey().equalsIgnoreCase(value)) {
                    return e.getValue();
                }
            }
            return null;
        }
        public List<String> valueLabels() { return Collections.unmodifiableList(valueLabels); }
        public List<String> valueConstants() {
            return Collections.unmodifiableList(valueConstants);
        }
        public String unsetConstant() { return unsetConstant; }
        public String def() { return def; }
        public String separator() { return separator; }
        public String platform() { return platform; }
        public boolean isDynamic() { return dynamic; }
        public String pattern() { return pattern; }
        public List<String> consumedBy() { return Collections.unmodifiableList(consumedBy); }
        public boolean isExternal() { return external; }
        public boolean isEnterpriseOnly() { return enterpriseOnly; }
        public String link() { return link; }
        public String doc() { return doc; }

        /** Whether this hint is exposed as an annotation attribute. */
        public boolean isAnnotated() {
            return attr != null && group.isAnnotated();
        }

        /** The full settings-file key, including the {@link #ARG_PREFIX}. */
        public String propertyKey() {
            return ARG_PREFIX + name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
