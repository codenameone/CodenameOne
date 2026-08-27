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

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Renders the build hints that the ANNOTATIONS declare into the two forms that
 * cannot read them directly.
 *
 * <p>It generates no code. The annotations in
 * {@code com.codename1.annotations.buildhints} are hand-written and are the
 * source of truth for the hints they expose; the hints with no annotation are
 * written by hand in this module. Everything that can read bytecode -- the
 * Maven plugin, the annotation processor -- reads the annotations themselves
 * through {@link BuildHintAnnotationReader} and never comes here.</p>
 *
 * <p>What it writes is data and prose:</p>
 *
 * <ul>
 *   <li>{@code build-hints.json}, for the two editors that are Codename One
 *       applications and so have no class reader: the Settings tool, through
 *       {@link BuildHints}, and the simulator's Build Hint editor, which
 *       carries its own copy because its module does not depend on this
 *       one.</li>
 *   <li>the developer guide's hint table, rendered on every guide build and
 *       not checked in.</li>
 * </ul>
 *
 * <p>Run through {@code scripts/gen-build-hint-annotations.sh}, whose
 * {@code --check} mode is what CI uses to catch a data file that no longer
 * matches the annotations beside it.</p>
 */
public final class BuildHintCodeGenerator {

    /** The generated data file, at the root of whichever resource tree gets it. */
    private static final String DATA_FILE = "cn1-build-hints.json";

    /** Where the hand-written annotations live, read but never written. */
    private static final String PKG_PATH = "com/codename1/annotations/buildhints";

    private BuildHintCodeGenerator() {
    }

    /**
     * @param args the annotation source root, the resource root for the data
     *             file, then any number of further outputs: a directory
     *             receives its own copy of the data file, an {@code .adoc} file
     *             receives the developer guide's table
     */
    public static void main(String[] args) throws IOException {
        // The developer guide's table is not checked in: it is rendered from the
        // catalog every time the guide is built, so it cannot drift and there is
        // nothing for a hand edit to survive in. This mode writes that one file
        // and nothing else, so a documentation build does not also rewrite source
        // trees it has no business touching.
        if (args.length == 3 && "--table-only".equals(args[0])) {
            write(new File(args[2]),
                    asciidocTable(everything(BuildHintAnnotationReader.readFromSources(
                            new File(args[1], PKG_PATH)))));
            return;
        }
        if (args.length < 2) {
            System.err.println("usage: BuildHintCodeGenerator <annotation-src-root> "
                    + "<catalog-src-root> [output...]");
            System.exit(2);
        }
        File annRoot = new File(args[0], PKG_PATH);
        if (!annRoot.isDirectory()) {
            throw new IOException("No build hint annotations at " + annRoot);
        }

        // The annotations are the source of truth for the hints they expose, so
        // they are READ here rather than written. Everything below is a view of
        // them; nothing restates them.
        List<BuildHints.Hint> annotated = BuildHintAnnotationReader.readFromSources(annRoot);

        // By the enum's own order rather than whichever group a hint name sorts
        // into first, so the views below do not reshuffle when a hint is added.
        Map<HintGroup, List<BuildHints.Hint>> byGroup =
                new TreeMap<HintGroup, List<BuildHints.Hint>>();
        for (BuildHints.Hint h : annotated) {
            List<BuildHints.Hint> list = byGroup.get(h.group());
            if (list == null) {
                list = new ArrayList<BuildHints.Hint>();
                byGroup.put(h.group(), list);
            }
            list.add(h);
        }
        // Sorted by attribute, so the outputs are byte-stable whatever order a
        // group's hints happen to be listed in.
        for (Map.Entry<HintGroup, List<BuildHints.Hint>> e : byGroup.entrySet()) {
            Collections.sort(e.getValue(), new Comparator<BuildHints.Hint>() {
                public int compare(BuildHints.Hint a, BuildHints.Hint b) {
                    return a.attr().compareTo(b.attr());
                }
            });
        }

        // Data, never code: this renders what the hand-written annotations declare
        // for the two consumers that cannot read bytecode -- the Settings editor
        // and the simulator's, both Codename One apps with no class reader.
        // Everything that can read bytecode reads the annotations themselves.
        //
        // At the resource ROOT rather than under com/codename1/build/, because
        // .gitignore's repo-wide `**/build/*` rule covers resources: a data file
        // in that package would be silently untracked, building locally from the
        // working tree and failing CI with a file nobody can see is missing.
        write(new File(args[1], DATA_FILE), json(annotated));
        for (int i = 2; i < args.length; i++) {
            File target = new File(args[i]);
            if (target.getName().endsWith(".adoc") || target.getName().endsWith(".asciidoc")) {
                write(target, asciidocTable(everything(annotated)));
            } else {
                write(new File(target, DATA_FILE), json(annotated));
            }
        }
        System.out.println("cn1: generated the build hint views");
    }

    /// The annotated hints as data.
    ///
    /// One object per hint, values are strings or arrays of strings, and a flag
    /// is present only when it is true. BuildHintsJson reads exactly this
    /// subset; there is no JSON library on either side, and this module has no
    /// dependencies beyond JUnit because the build service shares it.
    private static String json(List<BuildHints.Hint> hints) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < hints.size(); i++) {
            BuildHints.Hint h = hints.get(i);
            sb.append("  {");
            List<String> members = new ArrayList<String>();
            members.add(member("name", h.name()));
            members.add(member("group", h.group().name()));
            members.add(member("attr", h.attr()));
            if (h.enumName() != null) {
                members.add(member("enum", h.enumName()));
                members.add(array("values", h.values()));
                if (!h.valueAliases().isEmpty()) {
                    List<String> pairs = new ArrayList<String>();
                    for (Map.Entry<String, String> e : h.valueAliases().entrySet()) {
                        pairs.add(e.getKey());
                        pairs.add(e.getValue());
                    }
                    members.add(array("valueAliases", pairs));
                }
                if (!h.valueLabels().isEmpty()) {
                    members.add(array("valueLabels", h.valueLabels()));
                }
                if (!h.valueConstants().isEmpty()) {
                    members.add(array("valueConstants", h.valueConstants()));
                }
                if (h.unsetConstant() != null) {
                    members.add(member("unsetConstant", h.unsetConstant()));
                }
            }
            members.add(member("type", h.type().name()));
            // What the editors need and cannot work out for themselves: the
            // widget, the label and the group heading. Derived here so the
            // Settings and simulator loaders stay a plain application of data --
            // neither can call into this module.
            members.add(member("editor", BuildHints.editorWidget(h.type())));
            members.add(member("label", humanize(h.attr())));
            members.add(member("groupLabel", groupLabel(h.group())));
            // The annotation's own simple name, because the loaders cannot derive
            // it: DESKTOP is @DesktopBuild and GENERAL is @Build, so a loader that
            // camel-cased the enum constant made up group keys of its own.
            members.add(member("annotation", h.group().annotationSimpleName()));
            if (h.valuePattern() != null) {
                members.add(member("valuePattern", h.valuePattern()));
            }
            if (h.separator() != null) {
                members.add(member("separator", h.separator()));
            }
            if (h.platform() != null) {
                members.add(member("platform", h.platform()));
            }
            if (h.aliasOf() != null) {
                members.add(member("aliasOf", h.aliasOf()));
            }
            if (h.deprecated() != null) {
                members.add(member("deprecated", h.deprecated()));
            }
            if (h.isExternal()) {
                members.add("\"external\": true");
            }
            if (h.isEnterpriseOnly()) {
                members.add("\"enterpriseOnly\": true");
            }
            if (h.link() != null && h.link().length() > 0) {
                members.add(member("link", h.link()));
            }
            members.add(member("doc", h.doc()));
            for (int m = 0; m < members.size(); m++) {
                sb.append(m == 0 ? "" : ", ").append(members.get(m));
            }
            sb.append('}').append(i == hints.size() - 1 ? "\n" : ",\n");
        }
        return sb.append("]\n").toString();
    }

    private static String member(String key, String value) {
        return "\"" + key + "\": " + jsonString(value == null ? "" : value);
    }

    private static String array(String key, List<String> values) {
        StringBuilder sb = new StringBuilder("\"" + key + "\": [");
        for (int i = 0; i < values.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(jsonString(values.get(i)));
        }
        return sb.append(']').toString();
    }

    private static String jsonString(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20 || c > 0x7e) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    /// The hints the catalog still describes, plus the annotated ones read from
    /// source.
    ///
    /// Deliberately not BuildHints.entries(): that includes the generated view
    /// this same run is about to rewrite, so a stale one would render itself
    /// back into the guide.
    private static List<BuildHints.Hint> everything(List<BuildHints.Hint> annotated) {
        List<BuildHints.Hint> all = new ArrayList<BuildHints.Hint>();
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (!h.isAnnotated()) {
                all.add(h);
            }
        }
        all.addAll(annotated);
        Collections.sort(all, new Comparator<BuildHints.Hint>() {
            public int compare(BuildHints.Hint a, BuildHints.Hint b) {
                return a.name().compareTo(b.name());
            }
        });
        return all;
    }

    private static String asciidocTable(List<BuildHints.Hint> everything) {
        List<BuildHints.Hint> all = new ArrayList<BuildHints.Hint>(everything);
        Collections.sort(all, new Comparator<BuildHints.Hint>() {
            public int compare(BuildHints.Hint a, BuildHints.Hint b) {
                int byPlatform = a.platform().compareTo(b.platform());
                return byPlatform != 0 ? byPlatform : a.name().compareTo(b.name());
            }
        });
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated from com.codename1.build.shared.BuildHints by\n");
        sb.append("// BuildHintCodeGenerator. Do not edit by hand -- edit the catalog and re-run\n");
        sb.append("// scripts/gen-build-hint-annotations.sh.\n");
        sb.append("//\n");
        sb.append("// The Annotation column names the compiler-checked form where one exists;\n");
        sb.append("// those hints can be written on the application's main class instead of in\n");
        sb.append("// codenameone_settings.properties. Their Default reads \"set by the\n");
        sb.append("// build\": leaving the attribute out means the build decides, and the\n");
        sb.append("// annotation deliberately does not pin down what it decides.\n\n");
        sb.append("[cols=\"2,1,1,2,4\"]\n");
        sb.append("|===\n");
        sb.append("|Name |Type |Default |Annotation |Description\n\n");
        for (BuildHints.Hint h : all) {
            // Dynamic families are listed too: their names are patterns rather than
            // keys, but they are real settings a reader needs to find.
            sb.append('|').append(cell(h.name())).append('\n');
            sb.append('|').append(cell(adocType(h))).append('\n');
            // A default is a literal value, not prose. One containing a quote --
            // android.file_paths defaults to an XML fragment -- trips Vale's
            // Microsoft.Quotes rule, which the developer guide enforces as an
            // error. Protect just that line using the mechanism .vale.ini
            // documents for individual false positives.
            if (h.def() != null && h.def().indexOf('"') >= 0) {
                sb.append("// vale-skip: Microsoft.Quotes: this is a literal default value, ")
                  .append("not prose -- the quotes belong to the value.\n");
            }
            // An annotated hint records no default on purpose: the attribute has
            // to mean "nothing was said" so the build server decides, and the
            // server may change that answer. Saying so beats "(none)", which
            // reads as "there is no default" next to a description that often
            // states one.
            sb.append('|').append(h.def() == null || h.def().length() == 0
                    ? (h.isAnnotated() ? "_(set by the build)_" : "_(none)_")
                    : "`" + cell(h.def()) + "`").append('\n');
            sb.append('|').append(h.isAnnotated()
                    ? "`@" + h.group().annotationSimpleName() + "(" + h.attr() + ")`"
                    : (h.isDynamic() ? "_(properties file only)_" : "_(none)_")).append('\n');
            String doc = h.doc();
            if (doc == null || doc.length() == 0) {
                doc = h.isExternal()
                        ? "Consumed by the build service. Not read by anything in the framework "
                          + "repository, so there is no in-repo reference for it."
                        : "";
            }
            sb.append('|').append(cell(doc)).append("\n\n");
        }
        sb.append("|===\n");
        return sb.toString();
    }

    /**
     * Escapes a value for an AsciiDoc table cell.
     *
     * <p>A bare {@code |} starts a new cell, so a hint whose documentation
     * contains one -- {@code ios.spm.packages} is written
     * {@code identity|url|requirement} -- silently shifts every following column
     * and asciidoctor reports "dropping cells from incomplete row" for the whole
     * table.</p>
     */
    private static String cell(String text) {
        return text == null ? "" : text.replace("|", "\\|");
    }

    private static String adocType(BuildHints.Hint h) {
        if (h.type() == HintType.ENUM) {
            StringBuilder sb = new StringBuilder();
            for (String v : h.values()) {
                sb.append(sb.length() == 0 ? "" : ", ").append('`').append(v).append('`');
            }
            return sb.toString();
        }
        if (h.type() == HintType.STRING_LIST) {
            String sep = "\n".equals(h.separator()) ? "newline" : "`" + h.separator() + "`";
            return "list (" + sep + " delimited)";
        }
        // ROOT: a hint type is a wire token, not text for a human, and
        // toLowerCase() in a Turkish locale turns I into a dotless i.
        return h.type().name().toLowerCase(java.util.Locale.ROOT);
    }

    private static String groupLabel(HintGroup g) {
        switch (g) {
            case IOS: return "iOS";
            case ANDROID: return "Android";
            case DESKTOP: return "Desktop";
            case HARDENING: return "App Hardening";
            case ON_DEVICE_DEBUG: return "On-Device Debugging";
            case IOS_PRIVACY: return "iOS Privacy Strings";
            case GENERAL: return "General";
            default: return g.annotationSimpleName();
        }
    }

    /** newStorageLocation -> "New storage location". */
    private static String humanize(String attr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attr.length(); i++) {
            char c = attr.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(attr.charAt(i - 1))) {
                sb.append(' ').append(Character.toLowerCase(c));
            } else {
                sb.append(i == 0 ? Character.toUpperCase(c) : c);
            }
        }
        return sb.toString();
    }


    /**
     * Folds text to ASCII.
     *
     * <p>The prose is imported from the developer guide, which uses typographic
     * punctuation. {@code CodenameOne/src} is also compiled by an Ant javac step
     * with ASCII encoding, where a single em dash is
     * {@code error: unmappable character for encoding ASCII} -- a build failure,
     * not a warning. A Unicode escape would not help: javac processes
     * {@code \\uXXXX} before it strips comments, so the character would simply
     * reappear.</p>
     */
    static String toAscii(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\u2014': sb.append("--"); break;      // em dash
                case '\u2013': sb.append('-'); break;       // en dash
                case '\u2018':
                case '\u2019': sb.append('\''); break;      // curly single quotes
                case '\u201c':
                case '\u201d': sb.append('"'); break;       // curly double quotes
                case '\u2026': sb.append("..."); break;     // ellipsis
                case '\u2192': sb.append("->"); break;      // right arrow
                case '\u00d7': sb.append('x'); break;       // multiplication sign
                case '\u00a0': sb.append(' '); break;       // non-breaking space
                default:
                    if (c < 0x80) {
                        sb.append(c);
                        break;
                    }
                    // Refuse rather than drop it. Silently deleting a character
                    // from a hint's documentation is a worse outcome than telling
                    // whoever edited the catalog to add a mapping here.
                    throw new IllegalArgumentException("Build hint documentation contains '"
                            + c + "' (U+" + Integer.toHexString(c).toUpperCase(java.util.Locale.ROOT)
                            + "), which has no ASCII equivalent in toAscii(). The Ant javac step "
                            + "compiles CodenameOne/src as ASCII and rejects it as unmappable. "
                            + "Add a mapping, or reword the text.");
            }
        }
        return sb.toString();
    }

    private static void write(File f, String content) throws IOException {
        if (f.getName().endsWith(".java")) {
            for (int i = 0; i < content.length(); i++) {
                if (content.charAt(i) >= 0x80) {
                    throw new IOException(f.getName() + " would contain the non-ASCII character '"
                            + content.charAt(i) + "' (U+"
                            + Integer.toHexString(content.charAt(i)).toUpperCase(java.util.Locale.ROOT)
                            + "), which the Ant javac step rejects as unmappable. Add it to "
                            + "toAscii().");
                }
            }
        }
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try {
            w.write(content);
        } finally {
            w.close();
        }
    }
}
