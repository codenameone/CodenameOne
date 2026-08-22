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
package com.codename1.maven;

import com.codename1.build.shared.BuildHints;
import com.codename1.build.shared.HintType;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Rewrites {@code codename1.arg.*} lines in {@code codenameone_settings.properties}
 * as build hint annotations on the application's main class.
 *
 * <p>A build hint written as a properties line is a string nothing checks: a
 * misspelled name is accepted, never read, and silently does nothing. The same
 * hint written as an annotation is checked by the compiler. This goal moves the
 * ones that have an annotation and leaves the rest alone.</p>
 *
 * <p>Runs in place and prints what it did. Pass {@code -Dcn1.migrate.dryRun=true}
 * to see the plan without touching anything.</p>
 */
// Aggregator: every module of a Codename One project resolves to the same
// codenameone_settings.properties, so running per-module would try to migrate
// the same file several times and the second pass would find its own output.
@Mojo(name = "migrate-build-hints", requiresProject = true, aggregator = true,
      requiresDependencyResolution = ResolutionScope.COMPILE)
public class MigrateBuildHintsMojo extends AbstractCN1Mojo {

    /**
     * The whole reactor. This goal is an aggregator, so {@code project} is the
     * root pom -- which carries no codenameone-core dependency of its own. The
     * core has to be looked for across the modules.
     */
    @Parameter(defaultValue = "${session.projects}", readonly = true, required = true)
    private java.util.List<org.apache.maven.project.MavenProject> reactorProjects;

    /** Report what would change without writing anything. */
    @Parameter(property = "cn1.migrate.dryRun", defaultValue = "false")
    private boolean dryRun;

    /**
     * Hints to leave in the properties file even though they have an annotation.
     * {@code java.version} is always kept: it selects the toolchain that compiles
     * the class the annotations would live on, so it has to be readable before
     * any of the project's own code exists.
     */
    @Parameter(property = "cn1.migrate.keep")
    private String keep;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File projectDir = getCN1ProjectDir();
        if (projectDir == null) {
            throw new MojoExecutionException("No Codename One project directory found; "
                    + "this goal must run in a project with a codenameone_settings.properties.");
        }
        File settingsFile = new File(projectDir, "codenameone_settings.properties");
        if (!settingsFile.isFile()) {
            throw new MojoExecutionException("No codenameone_settings.properties in " + projectDir);
        }

        // The annotations ship in codenameone-core. A project pinned to a release
        // that predates them would migrate cleanly here and then fail to compile,
        // so refuse rather than hand back a broken project.
        if (!coreHasBuildHintAnnotations()) {
            throw new MojoFailureException("This project builds against a Codename One version "
                    + "whose core has no com.codename1.annotations.buildhints package, so the "
                    + "annotations would not resolve. Update the project's cn1.version first.");
        }

        Properties settings = new Properties();
        try (FileInputStream in = new FileInputStream(settingsFile)) {
            settings.load(in);
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not read " + settingsFile, ex);
        }

        List<String> kept = new ArrayList<String>();
        kept.add("java.version");
        if (keep != null) {
            for (String k : keep.split(",")) {
                if (k.trim().length() > 0) {
                    kept.add(k.trim());
                }
            }
        }

        // Resolve the target language before rendering: Kotlin writes an array
        // literal as [a, b] and rejects Java's {a, b}, so the same hint renders
        // differently depending on which file it is going into.
        String mainSourcePath = findMainClassSource(projectDir, settings);
        boolean kotlinTarget = mainSourcePath != null && mainSourcePath.endsWith(".kt");

        // annotation simple name -> attribute -> source literal
        Map<String, Map<String, String>> plan = new TreeMap<String, Map<String, String>>();
        List<String> migratedKeys = new ArrayList<String>();
        List<String> skipped = new ArrayList<String>();

        for (String key : new ArrayList<String>(settings.stringPropertyNames())) {
            if (!key.startsWith(BuildHints.ARG_PREFIX)) {
                continue;
            }
            String name = key.substring(BuildHints.ARG_PREFIX.length());
            if (kept.contains(name)) {
                skipped.add(name + " (kept by configuration)");
                continue;
            }
            BuildHints.Hint hint = BuildHints.byName(name);
            if (hint == null || !hint.isAnnotated()) {
                skipped.add(name + " (no annotation for this hint yet)");
                continue;
            }
            String literal = toSourceLiteral(hint, settings.getProperty(key), kotlinTarget);
            if (literal == null) {
                skipped.add(name + " = '" + settings.getProperty(key)
                        + "' (value is outside the hint's supported set)");
                continue;
            }
            String annotation = hint.group().annotationSimpleName();
            Map<String, String> members = plan.get(annotation);
            if (members == null) {
                members = new TreeMap<String, String>();
                plan.put(annotation, members);
            }
            members.put(hint.attr(), literal);
            migratedKeys.add(key);
        }

        if (plan.isEmpty()) {
            getLog().info("cn1: nothing to migrate -- no annotated build hint is set in "
                    + settingsFile.getName());
            for (String s : skipped) {
                getLog().debug("cn1:   left in place: " + s);
            }
            return;
        }

        String mainSource = mainSourcePath;
        StringBuilder rendered = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> e : plan.entrySet()) {
            rendered.append(render(e.getKey(), e.getValue())).append('\n');
        }

        getLog().info("cn1: move these onto " + (mainSource == null ? "your main class"
                : new File(mainSource).getName()) + ":");
        for (String line : rendered.toString().split("\n")) {
            getLog().info("cn1:   " + line);
        }
        for (String s : skipped) {
            getLog().info("cn1: leaving " + s);
        }

        if (dryRun) {
            getLog().info("cn1: dry run -- nothing written");
            return;
        }
        if (mainSource == null) {
            throw new MojoFailureException("Could not find the source of the main class named by "
                    + "codename1.mainName. Add the annotations above by hand, then delete the "
                    + "migrated lines from " + settingsFile.getName() + ".");
        }

        try {
            insertAnnotations(new File(mainSource), rendered.toString(),
                    settings.getProperty("codename1.mainName", "").trim());
            removeMigratedLines(settingsFile, migratedKeys);
        } catch (IOException ex) {
            throw new MojoExecutionException("Migration failed: " + ex.getMessage(), ex);
        }
        getLog().info("cn1: migrated " + migratedKeys.size() + " build hint(s) into "
                + new File(mainSource).getName());
    }

    /**
     * Whether the codenameone-core on this project's compile classpath actually
     * carries the annotations.
     */
    private boolean coreHasBuildHintAnnotations() {
        java.util.List<org.apache.maven.project.MavenProject> projects = reactorProjects;
        if (projects == null || projects.isEmpty()) {
            projects = java.util.Collections.singletonList(project);
        }
        for (org.apache.maven.project.MavenProject p : projects) {
            if (carriesBuildHintAnnotations(p)) {
                return true;
            }
        }
        return false;
    }

    private boolean carriesBuildHintAnnotations(org.apache.maven.project.MavenProject p) {
        try {
            for (Object element : p.getCompileClasspathElements()) {
                File f = new File((String) element);
                if (f.isDirectory()) {
                    if (new File(f, "com/codename1/annotations/buildhints/Ios.class").isFile()) {
                        return true;
                    }
                } else if (f.isFile()) {
                    try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(f)) {
                        if (zip.getEntry("com/codename1/annotations/buildhints/Ios.class") != null) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            getLog().debug("cn1: could not inspect the compile classpath: " + ex.getMessage());
            return true;
        }
        return false;
    }

    /**
     * Renders a value as the Java literal for its attribute.
     *
     * @return the literal, or null when the value is outside a closed domain --
     *         which is worth reporting rather than silently translating, because
     *         it means the properties file has been setting something the build
     *         never understood
     */
    String toSourceLiteral(BuildHints.Hint hint, String value, boolean kotlin) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        switch (hint.type()) {
            case BOOLEAN:
                if ("true".equalsIgnoreCase(v)) return "true";
                if ("false".equalsIgnoreCase(v)) return "false";
                return null;
            case INT:
                try {
                    return String.valueOf(Integer.parseInt(v));
                } catch (NumberFormatException ex) {
                    return null;
                }
            case ENUM:
                for (String allowed : hint.values()) {
                    if (allowed.equalsIgnoreCase(v)) {
                        return hint.enumName() + "." + enumConstant(allowed);
                    }
                }
                return null;
            case STRING_LIST: {
                String sep = hint.separator();
                if (sep == null || sep.length() == 0) {
                    return quote(v);
                }
                String[] parts = v.split(java.util.regex.Pattern.quote(sep), -1);
                StringBuilder sb = new StringBuilder(kotlin ? "[" : "{");
                int written = 0;
                for (String part : parts) {
                    String t = part.trim();
                    if (t.length() == 0) {
                        continue;
                    }
                    if (written++ > 0) {
                        sb.append(", ");
                    }
                    sb.append(quote(t));
                }
                return sb.append(kotlin ? ']' : '}').toString();
            }
            default:
                return quote(v);
        }
    }

    /** Mirrors the generator's wire-value to constant-name mapping. */
    static String enumConstant(String wire) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wire.length(); i++) {
            char c = wire.charAt(i);
            if (Character.isUpperCase(c) && sb.length() > 0
                    && Character.isLowerCase(wire.charAt(i - 1))) {
                sb.append('_');
            }
            sb.append(Character.isLetterOrDigit(c) ? Character.toUpperCase(c) : '_');
        }
        String out = sb.toString();
        return out.length() > 0 && Character.isDigit(out.charAt(0)) ? "V" + out : out;
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    static String render(String annotation, Map<String, String> members) {
        StringBuilder sb = new StringBuilder("@").append(annotation).append('(');
        int i = 0;
        for (Map.Entry<String, String> e : members.entrySet()) {
            if (i++ > 0) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append(" = ").append(e.getValue());
        }
        return sb.append(')').toString();
    }

    private String findMainClassSource(File projectDir, Properties settings) {
        String main = settings.getProperty("codename1.mainName");
        String pkg = settings.getProperty("codename1.packageName");
        if (main == null || main.trim().length() == 0) {
            return null;
        }
        String path = (pkg == null ? "" : pkg.trim().replace('.', File.separatorChar)
                + File.separator) + main.trim();
        String[] roots = {"src" + File.separator + "main" + File.separator + "java",
                          "src" + File.separator + "main" + File.separator + "kotlin",
                          "src"};
        String[] extensions = {".java", ".kt"};
        for (String root : roots) {
            for (String ext : extensions) {
                File f = new File(projectDir, root + File.separator + path + ext);
                if (f.isFile()) {
                    return f.getAbsolutePath();
                }
            }
        }
        return null;
    }

    /**
     * Splices the annotations in above the class declaration, with the import.
     *
     * <p>Textual rather than a parse: the file may be Java or Kotlin, it may use
     * any formatting, and rewriting it through a parser would reformat code the
     * developer did not ask to have touched.</p>
     */
    private void insertAnnotations(File source, String annotations, String simpleName)
            throws IOException {
        String text = read(source);
        boolean kotlin = source.getName().endsWith(".kt");
        String importLine = kotlin
                ? "import com.codename1.annotations.buildhints.*"
                : "import com.codename1.annotations.buildhints.*;";
        if (text.contains("com.codename1.annotations.buildhints")) {
            throw new IOException(source.getName() + " already imports the build hint "
                    + "annotations; migrate the remaining hints by hand so nothing is "
                    + "overwritten.");
        }

        int declaration = classDeclarationIndex(text, kotlin, simpleName);
        if (declaration < 0) {
            throw new IOException("Could not find the class declaration in " + source.getName());
        }
        String head = text.substring(0, declaration);
        String tail = text.substring(declaration);

        int lastImport = head.lastIndexOf("\nimport ");
        if (lastImport >= 0) {
            int eol = head.indexOf('\n', lastImport + 1);
            head = head.substring(0, eol + 1) + importLine + "\n" + head.substring(eol + 1);
        } else {
            int pkgEnd = head.indexOf('\n', head.indexOf("package "));
            head = head.substring(0, pkgEnd + 1) + "\n" + importLine + "\n"
                    + head.substring(pkgEnd + 1);
        }
        write(source, head + annotations + tail);
    }

    /**
     * Index of the start of the line declaring the top-level type.
     *
     * <p>Matched by pattern rather than against a list of prefixes: a declaration
     * can carry any combination of modifiers -- {@code public final class},
     * {@code internal data class} -- and a missing combination would abort the
     * migration on a perfectly ordinary file. Anchored to column zero so a
     * nested type or a mention inside an indented doc comment cannot match, and
     * the type named by {@code codename1.mainName} is preferred over whatever
     * happens to appear first.</p>
     */
    static int classDeclarationIndex(String text, boolean kotlin, String simpleName) {
        String modifiers = kotlin
                ? "(?:public |internal |private |open |abstract |final |sealed |data |value |annotation )*"
                : "(?:public |protected |private |abstract |final |static |strictfp |sealed |non-sealed )*";
        String kinds = kotlin ? "(?:class|object|interface)" : "(?:class|interface|enum|record)";
        java.util.regex.Pattern named = java.util.regex.Pattern.compile(
                "(?m)^" + modifiers + kinds + "\\s+"
                        + java.util.regex.Pattern.quote(simpleName == null ? "" : simpleName)
                        + "\\b");
        java.util.regex.Matcher m = named.matcher(text);
        if (simpleName != null && simpleName.length() > 0 && m.find()) {
            return m.start();
        }
        java.util.regex.Matcher any = java.util.regex.Pattern.compile(
                "(?m)^" + modifiers + kinds + "\\s+\\w").matcher(text);
        return any.find() ? any.start() : -1;
    }

    /**
     * Deletes the migrated lines, leaving every other line -- comments,
     * ordering, unrelated settings -- byte for byte as it was.
     */
    private void removeMigratedLines(File settingsFile, List<String> keys) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(settingsFile), "ISO-8859-1"));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                lines.add(line);
            }
        } finally {
            r.close();
        }
        Map<String, Boolean> wanted = new LinkedHashMap<String, Boolean>();
        for (String k : keys) {
            wanted.put(k, Boolean.TRUE);
        }
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            boolean drop = false;
            if (t.length() > 0 && t.charAt(0) != '#' && t.charAt(0) != '!') {
                int eq = t.indexOf('=');
                int colon = t.indexOf(':');
                int split = eq < 0 ? colon : (colon < 0 ? eq : Math.min(eq, colon));
                if (split > 0 && wanted.containsKey(t.substring(0, split).trim())) {
                    drop = true;
                }
            }
            if (!drop) {
                out.append(line).append('\n');
            }
        }
        write(settingsFile, out.toString());
    }

    private static String read(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        try {
            int c;
            while ((c = r.read()) >= 0) {
                sb.append((char) c);
            }
        } finally {
            r.close();
        }
        return sb.toString();
    }

    private static void write(File f, String content) throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try {
            w.write(content);
        } finally {
            w.close();
        }
    }
}
