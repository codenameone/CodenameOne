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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/// The port report of a Swing or JavaFX application: where it stands as a
/// Codename One application, read top down.
///
/// The compliance check writes it on every run, pass or fail, to
/// `codenameone/desktop-port-report.md` in the build directory
/// (`-Dcn1.desktop.report=<file>` names another place). It says how much of
/// the application builds as it is, what to change file by file with the
/// nearest supported alternative, what became of each dependency, and what
/// cannot run on a device at all.
///
/// The text is a function of the compiled classes: ordered, with no time
/// and no absolute path in it, so it can be committed and compared.
public final class DesktopPortReport {

    /// The property that names the report file.
    public static final String PROPERTY = "cn1.desktop.report";

    /// The report's path inside the build directory.
    public static final String DEFAULT_PATH = "codenameone/desktop-port-report.md";

    /// The share of a library's shipped classes, in percent, that must use
    /// API a device lacks before the report calls for replacing the library
    /// rather than working around those classes.
    static final int REPLACE_SHARE_PERCENT = 10;

    private final ComplianceFindings findings;
    private final Set<String> applicationFiles = new TreeSet<String>();
    private final Map<String, CompatLibraries.Library> libraries = new TreeMap<String, CompatLibraries.Library>();
    private final Map<String, Integer> libraryClassesWithFindings = new TreeMap<String, Integer>();
    private int applicationClasses;
    private int toolkitCallSites;
    private int toolkitCallSitesMissing;
    private int newerClassFiles;
    private int newestJava;

    /// A report of `findings`.
    public DesktopPortReport(ComplianceFindings findings) {
        this.findings = findings;
    }

    /// The application's own classes: how many, and the source files they
    /// were compiled from, as paths from the source root.
    public DesktopPortReport application(int classes, Collection<String> sourceFiles) {
        applicationClasses = classes;
        applicationFiles.addAll(sourceFiles);
        return this;
    }

    /// How many references the application makes into a desktop toolkit,
    /// and how many of those are to API the layers lack.
    public DesktopPortReport toolkitCallSites(int all, int missing) {
        toolkitCallSites = all;
        toolkitCallSitesMissing = missing;
        return this;
    }

    /// What the build decided about each dependency ([CompatLibraries]),
    /// and for each library how many of its classes have a finding.
    public DesktopPortReport libraries(Map<String, CompatLibraries.Library> recorded,
                                       Map<String, Integer> classesWithFindings) {
        libraries.putAll(recorded);
        libraryClassesWithFindings.putAll(classesWithFindings);
        return this;
    }

    /// How many class files were compiled for a Java newer than the device
    /// format, and the newest Java among them.
    public DesktopPortReport newerClassFiles(int count, int newestJavaVersion) {
        newerClassFiles = count;
        newestJava = newestJavaVersion;
        return this;
    }

    private static int percent(int part, int whole) {
        return whole == 0 ? 100 : (int) (part * 100L / whole);
    }

    /// What heads the APIs of a file that the alternatives table has no row
    /// for.
    static final String NO_ADVICE = "Not on a device, and no alternative is listed:";

    /// The report, as Markdown.
    public String render() {
        ComplianceFindings.Source app = findings.application();
        Map<String, List<ComplianceFindings.Api>> byFile = app.byFile();
        List<String> unchanged = new ArrayList<String>();
        for (String f : applicationFiles) {
            if (!byFile.containsKey(f)) {
                unchanged.add(f);
            }
        }
        StringBuilder out = new StringBuilder();
        out.append("# Desktop port report\n\n");
        out.append("What it takes for this application to build and run as a Codename One application, from its "
                + "compiled classes.\n\n");

        out.append("## Summary\n\n");
        out.append("- ").append(ComplianceFindings.plural(applicationClasses, "application class",
                "application classes")).append(" from ").append(ComplianceFindings.plural(applicationFiles.size(),
                "source file", "source files")).append(".\n");
        if (toolkitCallSites > 0) {
            int supported = toolkitCallSites - toolkitCallSitesMissing;
            out.append("- ").append(supported).append(" of ").append(toolkitCallSites)
                    .append(" call sites into Swing, AWT and JavaFX are supported (")
                    .append(percent(supported, toolkitCallSites)).append("%).\n");
        }
        out.append("- ").append(ComplianceFindings.plural(unchanged.size(), "source file has", "source files have"))
                .append(" no finding; ").append(byFile.size()).append(byFile.size() == 1 ? " has" : " have")
                .append(" at least one.\n");
        if (findings.isEmpty()) {
            out.append("- The compliance check passes: every API the application and its libraries use is "
                    + "available on a device.\n");
        } else {
            int inLibraries = findings.total() - app.callSites();
            out.append("- The compliance check fails: ").append(ComplianceFindings.plural(findings.total(),
                    "reference", "references")).append(" to ").append(ComplianceFindings.plural(findings.apiCount(),
                    "API", "APIs")).append(" a device does not have (").append(app.callSites())
                    .append(" in the application, ").append(inLibraries).append(" in its libraries).\n");
        }
        if (newerClassFiles > 0) {
            out.append("- ").append(ComplianceFindings.plural(newerClassFiles, "class file was", "class files were"))
                    .append(" compiled for a Java newer than 17 (up to Java ").append(newestJava)
                    .append(") and rewritten to the Java 17 class format. The Java 18+ API they use is checked like "
                            + "any other, and is listed below where a device lacks it.\n");
        }
        if (app.callSites() > 0) {
            out.append("\n| In the application | Missing APIs | Call sites |\n|---|---:|---:|\n");
            for (ComplianceFindings.Category c : ComplianceFindings.Category.values()) {
                List<ComplianceFindings.Api> apis = app.apis(c);
                if (!apis.isEmpty()) {
                    out.append("| ").append(c.heading()).append(" | ").append(apis.size()).append(" | ")
                            .append(app.callSites(c)).append(" |\n");
                }
            }
        }

        out.append("\n## What works unchanged\n\n");
        if (unchanged.isEmpty()) {
            out.append("No source file is free of findings.\n");
        } else {
            out.append(ComplianceFindings.plural(unchanged.size(), "source file uses", "source files use"))
                    .append(" only API that is available on a device:\n\n");
            for (String f : unchanged) {
                out.append("- `").append(f).append("`\n");
            }
        }

        out.append("\n## What to change\n\n");
        if (byFile.isEmpty()) {
            out.append("Nothing in the application's own sources.\n");
        }
        for (Map.Entry<String, List<ComplianceFindings.Api>> e : byFile.entrySet()) {
            out.append("### `").append(e.getKey()).append("`\n\n");
            // One entry for each thing to do: a class, its constructor and
            // the method called on it are one change, and share their advice.
            Map<String, List<ComplianceFindings.Api>> byAdvice = new java.util.LinkedHashMap<String,
                    List<ComplianceFindings.Api>>();
            for (ComplianceFindings.Api a : e.getValue()) {
                String advice = a.alternative() == null ? NO_ADVICE : a.alternative();
                List<ComplianceFindings.Api> list = byAdvice.get(advice);
                if (list == null) {
                    list = new ArrayList<ComplianceFindings.Api>();
                    byAdvice.put(advice, list);
                }
                list.add(a);
            }
            for (Map.Entry<String, List<ComplianceFindings.Api>> advice : byAdvice.entrySet()) {
                out.append("- ").append(advice.getKey()).append('\n');
                for (ComplianceFindings.Api a : advice.getValue()) {
                    out.append("  - ").append(lines(a, e.getKey())).append("`").append(a.name()).append("`\n");
                }
            }
            out.append('\n');
        }

        out.append(byFile.isEmpty() ? "\n" : "").append("## Dependencies\n\n");
        if (libraries.isEmpty()) {
            out.append("The application ships no third-party library.\n");
        } else {
            out.append("| Dependency | What it is | Classes shipped | Verdict |\n|---|---|---:|---|\n");
            for (CompatLibraries.Library lib : libraries.values()) {
                out.append("| ").append(lib.jar()).append(" | ").append(lib.kind() == null ? "library"
                        : lib.kind().label()).append(" | ").append(lib.shippedCount()).append(" of ")
                        .append(lib.classCount()).append(" | ").append(verdict(lib)).append(" |\n");
            }
        }

        out.append("\n## What cannot run on a device\n\n");
        List<String> cannot = cannotRun(app);
        if (cannot.isEmpty()) {
            out.append("Nothing was found that has no counterpart on a device.\n");
        }
        for (String line : cannot) {
            out.append("- ").append(line).append('\n');
        }
        return out.toString();
    }

    /// `line 3, 9: ` for the uses of `api` in `file`; empty without lines.
    private static String lines(ComplianceFindings.Api api, String file) {
        StringBuilder out = new StringBuilder();
        for (ComplianceFindings.Use u : api.uses()) {
            if (u.file().equals(file) && u.line() > 0) {
                out.append(out.length() == 0 ? "" : ", ").append(u.line());
            }
        }
        if (out.length() == 0) {
            return "";
        }
        return (out.indexOf(",") < 0 ? "line " : "lines ") + out + ": ";
    }

    private ComplianceFindings.Source source(String jar) {
        for (ComplianceFindings.Source s : findings.libraries()) {
            if (jar.equals(s.library())) {
                return s;
            }
        }
        return null;
    }

    /// What the report says about one dependency.
    String verdict(CompatLibraries.Library lib) {
        if (lib.kind() == DependencyClassifier.Kind.LAYER_PROVIDED) {
            return "Handled by the " + lib.layer() + " layer, whose own classes ship in its place.";
        }
        if (lib.shippedCount() == 0) {
            return "Not used by the application's classes; left out.";
        }
        ComplianceFindings.Source s = source(lib.jar());
        StringBuilder out = new StringBuilder();
        if (lib.nativeCode() != null) {
            out.append("Native code: cannot run on a device (it ").append(lib.nativeCode()).append("). ");
        }
        if (s == null) {
            if (lib.nativeCode() != null) {
                return out.append("Put what it does behind an interface with a device implementation.").toString();
            }
            return lib.kind() == DependencyClassifier.Kind.UI_LIBRARY
                    ? "Works: bundled whole and relocated with the application."
                    : "Works: the classes the application uses are relocated and pass the check.";
        }
        Integer classes = libraryClassesWithFindings.get(lib.jar());
        int with = classes == null ? 0 : classes;
        out.append(with).append(" of ").append(lib.shippedCount())
                .append(" shipped classes use API a device lacks: ");
        Map<String, Integer> families = new TreeMap<String, Integer>();
        for (ComplianceFindings.Category c : ComplianceFindings.Category.values()) {
            for (Map.Entry<String, Integer> f : s.families(c).entrySet()) {
                String name = c == ComplianceFindings.Category.TOOLKIT ? "desktop toolkit" : f.getKey();
                Integer n = families.get(name);
                families.put(name, (n == null ? 0 : n) + f.getValue());
            }
        }
        int i = 0;
        for (Map.Entry<String, Integer> f : ComplianceFindings.byCount(families).entrySet()) {
            if (i == 4) {
                break;
            }
            out.append(i++ == 0 ? "" : ", ").append(f.getKey()).append(" (").append(f.getValue()).append(")");
        }
        out.append(". ");
        String instead = lib.mainPackage() == null ? null : ComplianceAlternatives.lookup(lib.mainPackage() + ".");
        if (lib.nativeCode() != null || with * 100 >= REPLACE_SHARE_PERCENT * lib.shippedCount()) {
            out.append("Needs a device replacement");
            return out.append(instead == null ? ", or an interface with a device implementation." : ": " + instead)
                    .toString();
        }
        out.append("Close: those classes are what stands between it and a device");
        return out.append(instead == null ? "." : ". " + instead).toString();
    }

    /// What has no counterpart on a device: libraries of native code, the
    /// application's uses of API the alternatives table has nothing for,
    /// and language features.
    private List<String> cannotRun(ComplianceFindings.Source app) {
        List<String> out = new ArrayList<String>();
        for (CompatLibraries.Library lib : libraries.values()) {
            if (lib.nativeCode() != null && lib.shippedCount() > 0) {
                out.add(lib.jar() + " " + lib.nativeCode() + ". Native code written for a desktop does not run on a "
                        + "device.");
            }
        }
        // One entry per piece of advice: ProcessBuilder, its constructor
        // and its start() are one thing a device cannot do, not three.
        Map<String, List<ComplianceFindings.Api>> byAdvice = new java.util.LinkedHashMap<String,
                List<ComplianceFindings.Api>>();
        for (ComplianceFindings.Category c : ComplianceFindings.Category.values()) {
            for (ComplianceFindings.Api a : app.apis(c)) {
                String advice = a.alternative();
                if (advice != null && (c == ComplianceFindings.Category.LANGUAGE || isNoEquivalent(advice))) {
                    List<ComplianceFindings.Api> list = byAdvice.get(advice);
                    if (list == null) {
                        list = new ArrayList<ComplianceFindings.Api>();
                        byAdvice.put(advice, list);
                    }
                    list.add(a);
                }
            }
        }
        for (Map.Entry<String, List<ComplianceFindings.Api>> e : byAdvice.entrySet()) {
            Set<String> files = new TreeSet<String>();
            int uses = 0;
            StringBuilder names = new StringBuilder();
            int named = 0;
            for (ComplianceFindings.Api a : e.getValue()) {
                uses += a.count();
                for (ComplianceFindings.Use u : a.uses()) {
                    files.add(u.file());
                }
                if (named < 3) {
                    names.append(named++ == 0 ? "" : ", ").append('`').append(a.name()).append('`');
                }
            }
            if (e.getValue().size() > named) {
                names.append(" and ").append(e.getValue().size() - named).append(" more");
            }
            StringBuilder where = new StringBuilder();
            int shown = 0;
            for (String f : files) {
                if (shown == 6) {
                    where.append(" and ").append(files.size() - shown).append(" more");
                    break;
                }
                where.append(shown++ == 0 ? "" : ", ").append('`').append(f).append('`');
            }
            out.add(names + " (" + ComplianceFindings.plural(uses, "use", "uses") + " in " + where + "). "
                    + e.getKey());
        }
        return out;
    }

    /// Whether `advice` says a device has nothing for the API, as against
    /// naming what to use instead.
    static boolean isNoEquivalent(String advice) {
        return advice.startsWith("No ") || advice.startsWith("A device cannot")
                || advice.startsWith("Native code");
    }
}
