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
package com.codename1.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/// What the compliance check found, arranged the way a developer reads it.
///
/// A check that fails on ten thousand references is not ten thousand
/// problems. It is a few hundred missing APIs, most of them used by a
/// library the developer did not write. So the findings are kept by where
/// they are -- the application first, then each bundled library -- then by
/// what kind of API is missing ([Category]), and each API once, with the
/// places that use it.
///
/// Everything here is ordered and holds no absolute path, so the same
/// classes give the same text on every machine.
public final class ComplianceFindings {

    /// What kind of API a finding is about.
    public enum Category {
        /// Swing, AWT or JavaFX API a compatibility layer does not have.
        TOOLKIT("Swing, AWT and JavaFX API the compatibility layers lack"),
        /// API of a UI library a compatibility layer stands in for.
        UI_LIBRARY("UI library API the compatibility layers lack"),
        /// JDK API a device does not have.
        JDK("JDK API a device lacks"),
        /// A Java language feature compiled to something a device cannot run.
        LANGUAGE("Language features a device cannot run"),
        /// A class of some other library that is not in the application.
        LIBRARY("Other libraries"),
        /// What fits none of these.
        OTHER("Other findings");

        private final String heading;

        Category(String heading) {
            this.heading = heading;
        }

        /// The category as a report heads it.
        public String heading() {
            return heading;
        }
    }

    /// One place that uses a missing API.
    public static final class Use implements Comparable<Use> {
        private final String file;
        private final int line;

        Use(String file, int line) {
            this.file = file;
            this.line = line;
        }

        /// The source file, as a path from the source root
        /// (`com/example/Orders.java`), or the class file when the class
        /// has no debug information.
        public String file() {
            return file;
        }

        /// The line, or 0 when it is not known.
        public int line() {
            return line;
        }

        @Override
        public int compareTo(Use o) {
            int c = file.compareTo(o.file);
            return c != 0 ? c : Integer.compare(line, o.line);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Use && ((Use) o).file.equals(file) && ((Use) o).line == line;
        }

        @Override
        public int hashCode() {
            return file.hashCode() * 31 + line;
        }
    }

    /// One missing API and everything that uses it.
    public static final class Api {
        private final String name;
        private final Category category;
        private final String family;
        private final String alternative;
        private final List<Use> uses = new ArrayList<Use>();

        Api(String name, Category category, String family, String alternative) {
            this.name = name;
            this.category = category;
            this.family = family;
            this.alternative = alternative;
        }

        /// The API as its documentation spells it.
        public String name() {
            return name;
        }

        public Category category() {
            return category;
        }

        /// The group within the category: "files", "networking", a library's
        /// package.
        public String family() {
            return family;
        }

        /// The nearest supported alternative, or null.
        public String alternative() {
            return alternative;
        }

        /// How many references there are to it.
        public int count() {
            return uses.size();
        }

        /// The places that use it, ordered by file and line, each once.
        public List<Use> uses() {
            return new ArrayList<Use>(new TreeSet<Use>(uses));
        }
    }

    /// The findings of one source: the application, or one library.
    public static final class Source {
        private final String library;
        private final Map<String, Api> apis = new LinkedHashMap<String, Api>();
        private int callSites;

        Source(String library) {
            this.library = library;
        }

        /// The library's jar, or null for the application's own classes.
        public String library() {
            return library;
        }

        /// How many references to a missing API the source makes.
        public int callSites() {
            return callSites;
        }

        /// How many different APIs those are.
        public int apiCount() {
            return apis.size();
        }

        /// The missing APIs of `category`, most used first.
        public List<Api> apis(Category category) {
            List<Api> out = new ArrayList<Api>();
            for (Api a : apis.values()) {
                if (a.category == category) {
                    out.add(a);
                }
            }
            Collections.sort(out, MOST_USED);
            return out;
        }

        /// How many references the source makes to each family of
        /// `category`, largest first.
        public Map<String, Integer> families(Category category) {
            Map<String, Integer> counts = new TreeMap<String, Integer>();
            for (Api a : apis.values()) {
                if (a.category == category) {
                    Integer n = counts.get(a.family);
                    counts.put(a.family, (n == null ? 0 : n) + a.count());
                }
            }
            return byCount(counts);
        }

        /// The source files with a finding, each with its APIs.
        public Map<String, List<Api>> byFile() {
            Map<String, List<Api>> out = new TreeMap<String, List<Api>>();
            for (Api a : apis.values()) {
                Set<String> files = new TreeSet<String>();
                for (Use u : a.uses) {
                    files.add(u.file);
                }
                for (String f : files) {
                    List<Api> list = out.get(f);
                    if (list == null) {
                        list = new ArrayList<Api>();
                        out.put(f, list);
                    }
                    list.add(a);
                }
            }
            for (List<Api> list : out.values()) {
                Collections.sort(list, BY_NAME);
            }
            return out;
        }

        int callSites(Category category) {
            int n = 0;
            for (Api a : apis.values()) {
                if (a.category == category) {
                    n += a.count();
                }
            }
            return n;
        }
    }

    private static final Comparator<Api> BY_NAME = new Comparator<Api>() {
        @Override
        public int compare(Api a, Api b) {
            return a.name.compareTo(b.name);
        }
    };

    private static final Comparator<Api> MOST_USED = new Comparator<Api>() {
        @Override
        public int compare(Api a, Api b) {
            return a.count() != b.count() ? Integer.compare(b.count(), a.count()) : a.name.compareTo(b.name);
        }
    };

    private final Source application = new Source(null);
    private final Map<String, Source> libraries = new TreeMap<String, Source>();
    private int total;

    /// Records one reference to a missing API.
    ///
    /// @param library the jar the referring class came from, or null for a
    ///        class of the application's own
    /// @param file the source file from the source root, or the class file
    /// @param line the line, or 0
    /// @param owner the class the API belongs to as the application was
    ///        compiled against it, as an internal name
    ///        (`javax/swing/JTable`); null when the finding names no class
    /// @param api the API as its documentation spells it
    /// @param key the API for the alternatives table: the owner and, for a
    ///        member, its name (`java.lang.Runtime.exec`); null to use
    ///        the owner
    public void add(String library, String file, int line, String owner, String api, String key) {
        Source source = application;
        if (library != null) {
            source = libraries.get(library);
            if (source == null) {
                source = new Source(library);
                libraries.put(library, source);
            }
        }
        Api entry = source.apis.get(api);
        if (entry == null) {
            Category category = categoryOf(owner);
            String lookup = key != null ? key : owner == null ? null : owner.replace('/', '.').replace('$', '.');
            entry = new Api(api, category, familyOf(category, owner), ComplianceAlternatives.lookup(lookup));
            source.apis.put(api, entry);
        }
        entry.uses.add(new Use(file, line));
        source.callSites++;
        total++;
    }

    /// Records a finding that is a language feature rather than an API.
    public void addLanguage(String library, String file, int line, String feature, String advice) {
        Source source = application;
        if (library != null) {
            source = libraries.get(library);
            if (source == null) {
                source = new Source(library);
                libraries.put(library, source);
            }
        }
        Api entry = source.apis.get(feature);
        if (entry == null) {
            entry = new Api(feature, Category.LANGUAGE, "language", advice);
            source.apis.put(feature, entry);
        }
        entry.uses.add(new Use(file, line));
        source.callSites++;
        total++;
    }

    /// Every reference recorded.
    public int total() {
        return total;
    }

    public boolean isEmpty() {
        return total == 0;
    }

    /// The application's own findings.
    public Source application() {
        return application;
    }

    /// The libraries with findings, the one with the most first.
    public List<Source> libraries() {
        List<Source> out = new ArrayList<Source>(libraries.values());
        Collections.sort(out, new Comparator<Source>() {
            @Override
            public int compare(Source a, Source b) {
                return a.callSites != b.callSites ? Integer.compare(b.callSites, a.callSites)
                        : a.library.compareTo(b.library);
            }
        });
        return out;
    }

    /// The application, when it has findings, and then the libraries.
    public List<Source> sources() {
        List<Source> out = new ArrayList<Source>();
        if (application.callSites > 0) {
            out.add(application);
        }
        out.addAll(libraries());
        return out;
    }

    /// How many different APIs are missing, over every source.
    public int apiCount() {
        Set<String> names = new TreeSet<String>(application.apis.keySet());
        for (Source s : libraries.values()) {
            names.addAll(s.apis.keySet());
        }
        return names.size();
    }

    private static final String[] TOOLKIT_ROOTS = {"java/", "javax/", "javafx/", "android/", "androidx/"};

    private static final String[] JDK_ROOTS = {"java/", "javax/", "jdk/", "sun/", "com/sun/", "org/w3c/", "org/xml/",
        "org/ietf/", "org/omg/"};

    /// The kind of API `owner` is, by the class's name as the application
    /// was compiled against it.
    public static Category categoryOf(String owner) {
        if (owner == null) {
            return Category.OTHER;
        }
        if (CompatLayers.owning(owner) != null) {
            for (String root : TOOLKIT_ROOTS) {
                if (owner.startsWith(root)) {
                    return Category.TOOLKIT;
                }
            }
            return Category.UI_LIBRARY;
        }
        if (owner.startsWith("com/sun/jna/")) {
            return Category.LIBRARY;
        }
        for (String root : JDK_ROOTS) {
            if (owner.startsWith(root)) {
                return Category.JDK;
            }
        }
        return Category.LIBRARY;
    }

    /// The JDK's packages by what a developer would call them. The first
    /// entry that the class's name starts with decides.
    private static final String[][] JDK_FAMILIES = {
        {"java/nio/file/", "files"},
        {"java/nio/channels/", "channels and memory-mapped files"},
        {"java/nio/", "NIO buffers and charsets"},
        {"java/io/Object", "serialization"},
        {"java/io/Serial", "serialization"},
        {"java/io/Externalizable", "serialization"},
        {"java/io/", "files and streams"},
        {"java/util/regex/", "regular expressions"},
        {"java/net/", "networking"},
        {"javax/net/", "networking"},
        {"java/lang/reflect/", "reflection"},
        {"java/lang/invoke/", "reflection"},
        {"java/lang/annotation/", "reflection"},
        {"java/lang/Class", "reflection and class loading"},
        {"java/lang/Module", "reflection and class loading"},
        {"java/lang/Package", "reflection and class loading"},
        {"java/util/ServiceLoader", "reflection and class loading"},
        {"java/lang/Process", "processes"},
        {"java/lang/Runtime", "processes and the runtime"},
        {"java/lang/management/", "management"},
        {"javax/management/", "management"},
        {"java/lang/ref/", "references"},
        {"java/lang/Thread", "threads"},
        {"java/lang/", "java.lang"},
        {"java/security/", "security and crypto"},
        {"javax/crypto/", "security and crypto"},
        {"javax/security/", "security and crypto"},
        {"java/time/", "date and time"},
        {"java/text/", "text formatting"},
        {"java/util/concurrent/", "concurrency"},
        {"java/util/stream/", "streams"},
        {"java/util/function/", "functional interfaces"},
        {"java/util/zip/", "archives"},
        {"java/util/jar/", "archives"},
        {"java/util/logging/", "logging"},
        {"java/util/prefs/", "preferences"},
        {"java/util/", "collections and utilities"},
        {"java/sql/", "databases"},
        {"javax/sql/", "databases"},
        {"javax/xml/", "XML"},
        {"org/w3c/", "XML"},
        {"org/xml/", "XML"},
        {"java/math/", "big numbers"},
        {"java/beans/", "beans"},
        {"sun/", "JDK internals"},
        {"jdk/", "JDK internals"},
        {"com/sun/", "JDK internals"},
    };

    /// The group `owner` belongs to within its category.
    public static String familyOf(Category category, String owner) {
        if (owner == null) {
            return "other";
        }
        if (category == Category.JDK) {
            for (String[] family : JDK_FAMILIES) {
                if (owner.startsWith(family[0])) {
                    return family[1];
                }
            }
        }
        return packageOf(owner, category == Category.JDK || category == Category.TOOLKIT ? 8 : 3);
    }

    /// The first `depth` parts of `owner`'s package, dotted.
    private static String packageOf(String owner, int depth) {
        int slash = owner.lastIndexOf('/');
        if (slash < 0) {
            return "(no package)";
        }
        String[] parts = owner.substring(0, slash).split("/");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length && i < depth; i++) {
            out.append(i == 0 ? "" : ".").append(parts[i]);
        }
        return out.toString();
    }

    static Map<String, Integer> byCount(Map<String, Integer> counts) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                int c = Integer.compare(b.getValue(), a.getValue());
                return c != 0 ? c : a.getKey().compareTo(b.getKey());
            }
        });
        Map<String, Integer> out = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> e : entries) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    static String plural(int n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }

    private static String sourceName(Source s) {
        return s.library == null ? "The application" : s.library;
    }

    /// The findings in a few lines, for the console: one line for the
    /// application and one per category of it, then one line per library.
    public String summary() {
        StringBuilder out = new StringBuilder();
        out.append(plural(total, "reference", "references")).append(" to ")
                .append(plural(apiCount(), "API", "APIs")).append(" a device does not have:");
        for (Source s : sources()) {
            out.append("\n  ").append(sourceName(s)).append(": ").append(plural(s.callSites, "call site", "call sites"))
                    .append(", ").append(plural(s.apiCount(), "API", "APIs"));
            if (s.library != null) {
                out.append(" -- ").append(top(allFamilies(s), 4));
                continue;
            }
            for (Category c : Category.values()) {
                List<Api> apis = s.apis(c);
                if (apis.isEmpty()) {
                    continue;
                }
                out.append("\n    ").append(c.heading()).append(": ").append(plural(apis.size(), "API", "APIs"))
                        .append(", ").append(plural(s.callSites(c), "call site", "call sites")).append(" -- ");
                if (c == Category.JDK || c == Category.LIBRARY) {
                    out.append(top(s.families(c), 5));
                } else {
                    Map<String, Integer> most = new LinkedHashMap<String, Integer>();
                    for (Api a : apis) {
                        most.put(a.name, a.count());
                    }
                    out.append(top(most, 3));
                }
            }
        }
        return out.toString();
    }

    /// A library's references by family, over every category.
    private static Map<String, Integer> allFamilies(Source s) {
        Map<String, Integer> counts = new TreeMap<String, Integer>();
        for (Api a : s.apis.values()) {
            String family = a.category == Category.TOOLKIT ? "desktop toolkit" : a.family;
            Integer n = counts.get(family);
            counts.put(family, (n == null ? 0 : n) + a.count());
        }
        return byCount(counts);
    }

    private static String top(Map<String, Integer> counts, int max) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (i == max) {
                out.append(", and ").append(counts.size() - max).append(" more");
                break;
            }
            out.append(i == 0 ? "" : ", ").append(e.getKey()).append(' ').append(e.getValue());
            i++;
        }
        return out.toString();
    }

    /// Every finding: each source, each category of it, each API once with
    /// its number of uses, where they are and what to use instead.
    public String report() {
        StringBuilder out = new StringBuilder();
        for (Source s : sources()) {
            String title = sourceName(s) + ": " + plural(s.callSites, "call site", "call sites") + " of "
                    + plural(s.apiCount(), "missing API", "missing APIs");
            out.append(title).append('\n');
            for (int i = 0; i < title.length(); i++) {
                out.append('=');
            }
            out.append('\n');
            for (Category c : Category.values()) {
                List<Api> apis = s.apis(c);
                if (apis.isEmpty()) {
                    continue;
                }
                out.append('\n').append(c.heading()).append(" (").append(plural(apis.size(), "API", "APIs"))
                        .append(", ").append(plural(s.callSites(c), "call site", "call sites")).append(")\n");
                if (c == Category.JDK || c == Category.LIBRARY) {
                    out.append("  by family: ").append(top(s.families(c), 12)).append('\n');
                }
                for (Api a : apis) {
                    out.append("\n  ").append(a.count()).append(" x ").append(a.name).append('\n');
                    out.append("      ").append(locations(a.uses(), 12)).append('\n');
                    if (a.alternative != null) {
                        out.append("      Instead: ").append(a.alternative).append('\n');
                    }
                }
            }
            out.append('\n');
        }
        return out.toString();
    }

    /// `a/B.java:3, 9; a/C.java:14`, at most `max` places.
    static String locations(List<Use> uses, int max) {
        StringBuilder out = new StringBuilder();
        String file = null;
        int shown = 0;
        for (Use u : uses) {
            if (shown == max) {
                out.append("; and ").append(uses.size() - max).append(" more");
                break;
            }
            if (!u.file.equals(file)) {
                out.append(file == null ? "" : "; ").append(u.file);
                if (u.line > 0) {
                    out.append(':').append(u.line);
                }
                file = u.file;
            } else if (u.line > 0) {
                out.append(", ").append(u.line);
            }
            shown++;
        }
        return out.toString();
    }
}
