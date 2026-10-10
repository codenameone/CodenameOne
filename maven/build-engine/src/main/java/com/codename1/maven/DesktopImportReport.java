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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/// What an import says about the dependencies of the project it imported:
/// one row for each jar, with what the build will do with it and what the
/// developer should.
///
/// The rows come from [DependencyClassifier], which is what the build itself
/// decides by ([CompatLibraries]), so an import cannot promise something the
/// build then does differently. The one difference is the evidence: a build
/// follows the references of the compiled application, an import has only
/// the sources' `import` lines, so a class used by its full name alone is
/// counted at build time and not here.
public final class DesktopImportReport {

    /// The newest Java release whose class files a Codename One build reads.
    public static final int HIGHEST_JAVA = 17;

    private DesktopImportReport() {
    }

    /// One dependency.
    public static final class Row {
        private final String dependency;
        private final String kind;
        private final String suggestion;

        Row(String dependency, String kind, String suggestion) {
            this.dependency = dependency;
            this.kind = kind;
            this.suggestion = suggestion;
        }

        /// The jar's file name.
        public String dependency() {
            return dependency;
        }

        /// What it was classified as ([DependencyClassifier.Kind#label]).
        public String kind() {
            return kind;
        }

        /// What to do about it, as one sentence or two.
        public String suggestion() {
            return suggestion;
        }
    }

    /// [#rows(Collection, File, List)] against both desktop layers: an
    /// import cannot know yet which of them the application will have.
    public static List<Row> rows(Collection<File> jars, File sourceDir) throws IOException {
        List<Relocation> layers = new ArrayList<Relocation>();
        layers.add(CompatLayers.SWING);
        layers.add(CompatLayers.JAVAFX);
        return rows(jars, sourceDir, layers);
    }

    /// Classifies `jars` against the layers of `layers` and follows what the
    /// sources under `sourceDir` import into them. Answers a row for each
    /// jar that holds classes or resources, in the order given.
    public static List<Row> rows(Collection<File> jars, File sourceDir, List<Relocation> layers) throws IOException {
        List<DependencyClassifier.Library> libraries = DependencyClassifier.classify(jars, layers);
        Set<String> seeds = DependencyClassifier.sourceReferences(sourceDir, libraries);
        DependencyClassifier.reach(seeds, libraries);
        List<Row> out = new ArrayList<Row>();
        for (DependencyClassifier.Library lib : libraries) {
            out.add(new Row(lib.jar(), lib.kind().label(), suggestion(lib)));
        }
        return out;
    }

    /// What to do about `lib`, once [DependencyClassifier#reach] has run.
    static String suggestion(DependencyClassifier.Library lib) {
        switch (lib.kind()) {
            case LAYER_PROVIDED:
                return "Keep it with scope provided: the sources compile against it and the " + lib.layer()
                        + " layer's own classes ship in its place.";
            case PLATFORM:
                return "Nothing to do: the Codename One build handles it.";
            case NO_CLASSES:
                return "Nothing to do: it holds no classes.";
            default:
                break;
        }
        String instead = lib.mainPackage() == null ? null : ComplianceAlternatives.lookup(lib.mainPackage() + ".");
        if (lib.reached().isEmpty()) {
            return "The sources import nothing from it: the build leaves it out unless a class that ships uses it."
                    + (lib.nativeCode() == null ? "" : " It " + lib.nativeCode() + ", which cannot run on a device.");
        }
        if (lib.nativeCode() != null) {
            return "Replace it: it " + lib.nativeCode() + ", which cannot run on a device. "
                    + (instead == null ? "Put what it does behind an interface with a device implementation."
                    : instead);
        }
        StringBuilder out = new StringBuilder();
        if (lib.kind() == DependencyClassifier.Kind.UI_LIBRARY) {
            out.append("Keep it with scope compile: all ").append(lib.classCount())
                    .append(" classes are bundled and relocated with the application.");
        } else {
            out.append("Keep it with scope compile: the classes the application uses are bundled (")
                    .append(lib.reached().size()).append(" of ").append(lib.classCount())
                    .append(" from the imports), relocated and checked.");
        }
        if (instead != null) {
            out.append(" If the build reports it: ").append(instead);
        } else {
            out.append(" The build reports any API it uses that a device lacks.");
        }
        return out.toString();
    }

    /// The table as lines of text: a header, a rule, and for each row the
    /// dependency and its class on one line with the suggestion indented
    /// under it, since a suggestion is a sentence and a column is not.
    public static List<String> table(List<Row> rows) {
        int name = "Dependency".length();
        for (Row r : rows) {
            name = Math.max(name, r.dependency().length());
        }
        List<String> out = new ArrayList<String>();
        out.add(pad("Dependency", name) + "  Class");
        out.add(pad("", name).replace(' ', '-') + "  -----");
        for (Row r : rows) {
            out.add(pad(r.dependency(), name) + "  " + r.kind());
            out.add("    " + r.suggestion());
        }
        return out;
    }

    private static String pad(String s, int width) {
        StringBuilder out = new StringBuilder(s);
        while (out.length() < width) {
            out.append(' ');
        }
        return out.toString();
    }

    /// What to tell the developer about a project that declares Java
    /// `level`; null when the build takes that level as it is, or the project
    /// does not say (0).
    public static String javaLevelWarning(int level) {
        if (level <= HIGHEST_JAVA) {
            return null;
        }
        return "The project is built for Java " + level + "; a Codename One application is compiled as Java "
                + HIGHEST_JAVA + ". The imported sources are compiled with -source/-target " + HIGHEST_JAVA
                + ", so a language feature newer than that is a compile error to rewrite, and API added to the JDK "
                + "after " + HIGHEST_JAVA
                + " is reported by the compliance check like any other a device lacks. A dependency whose class "
                + "files are newer than Java " + HIGHEST_JAVA + " is rewritten to that level when it is bundled.";
    }
}
