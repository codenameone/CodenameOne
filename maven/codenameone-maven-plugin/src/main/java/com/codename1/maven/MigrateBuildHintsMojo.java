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
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
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

        // Add the annotations, prove the build actually turns them back into hints,
        // and only then delete the properties. Deciding that from the POM instead
        // meant guessing whether process-annotations would run -- and the goal is
        // skippable, bindable to a phase with no compiled classes, bindable to the
        // wrong module, and skippable through a property expression. Observing the
        // emitted resource answers all of those at once, and answers correctly for
        // whatever the next way to not-run turns out to be.
        // Apply the whole migration, prove the build turns the annotations back
        // into hints, and roll both files back if it does not.
        //
        // Both files have to move together before the check: leaving the
        // properties in place while the annotations are added *is* the
        // duplicate-declaration case, so the build would fail for that reason and
        // never tell us whether processing works at all.
        //
        // Deciding this from the POM instead meant guessing whether
        // process-annotations would run, and the goal is skippable, bindable to a
        // phase with no compiled classes, bindable to the wrong module, and
        // skippable through a property expression. Observing the emitted resource
        // answers all of those at once, and answers correctly for whatever the
        // next way to not-run turns out to be.
        File source = new File(mainSource);
        String originalSource;
        String originalSettings;
        try {
            originalSource = read(source);
            originalSettings = readProperties(settingsFile);
        } catch (IOException ex) {
            throw new MojoExecutionException("Migration failed: " + ex.getMessage(), ex);
        }

        // Anything that throws from here on has to put both files back. The half
        // that fails is not always the second one: if the annotations go in and
        // the properties rewrite then fails -- an unwritable file, a full disk, a
        // partial write -- the project is left declaring the same hint twice,
        // which is exactly the state the next build refuses to compile. Leaving
        // the developer with that is worse than not migrating at all.
        try {
            insertAnnotations(source, rendered.toString(),
                    settings.getProperty("codename1.mainName", "").trim());
            removeMigratedLines(settingsFile, migratedKeys);
        } catch (IOException | RuntimeException ex) {
            throw new MojoExecutionException("Migration failed, so " + source.getName() + " and "
                    + settingsFile.getName() + " have been put back as they were: "
                    + ex.getMessage()
                    + restore(source, originalSource, settingsFile, originalSettings), ex);
        }

        String missing = verifyAnnotationsAreProcessed(projectDir, migratedKeys);
        if (missing != null) {
            String restoreFailed = restore(source, originalSource, settingsFile, originalSettings);
            throw new MojoFailureException("The annotations were added but the build did not turn "
                    + "them into build hints, so " + source.getName() + " and "
                    + settingsFile.getName() + " have been put back as they were.\n\n"
                    + missing + "\n\nThe usual cause is that this module does not run the cn1 "
                    + "process-annotations goal, or runs it skipped or before compile. Add it and "
                    + "try again:\n"
                    + "    <execution>\n"
                    + "      <id>cn1-process-classes</id>\n"
                    + "      <phase>process-classes</phase>\n"
                    + "      <goals>\n"
                    + "        <goal>process-annotations</goal>\n"
                    + "      </goals>\n"
                    + "    </execution>"
                    + restoreFailed);
        }

        getLog().info("cn1: migrated " + migratedKeys.size() + " build hint(s) into "
                + new File(mainSource).getName());
    }

    /**
     * Puts both files back as they were.
     *
     * @return an empty string when both were restored, otherwise a description of
     *         what could not be, to append to the failure being reported
     */
    private String restore(File source, String originalSource,
                           File settingsFile, String originalSettings) {
        StringBuilder failed = new StringBuilder();
        try {
            write(source, originalSource);
        } catch (IOException ex) {
            failed.append("\nCould not restore ").append(source).append(": ")
                    .append(ex.getMessage());
        }
        try {
            writeProperties(settingsFile, originalSettings);
        } catch (IOException ex) {
            failed.append("\nCould not restore ").append(settingsFile).append(": ")
                    .append(ex.getMessage());
        }
        return failed.toString();
    }

    /**
     * Runs the project's own build over the module that holds the main class and
     * checks that every migrated hint came back out of it.
     *
     * @return null when all of them did, otherwise a description of what is
     *         missing, suitable for showing to the developer
     */
    private String verifyAnnotationsAreProcessed(File projectDir, List<String> migratedKeys) {
        getLog().info("cn1: building " + projectDir.getName()
                + " to confirm the annotations produce the hints...");
        // Delete any manifest an earlier build left behind first. Checking that
        // the file exists and holds the right keys proves nothing if it was
        // already there: with processing now skipped or unbound the nested build
        // leaves it untouched, the check passes, the properties are deleted, and
        // the next clean build removes the stale artifact and the hints with it.
        File emitted = new File(projectDir, "target/classes/" + ANNOTATION_HINTS_RESOURCE);
        if (emitted.isFile() && !emitted.delete()) {
            return "Could not remove the previous " + ANNOTATION_HINTS_RESOURCE
                    + ", so this build's output could not be told apart from it.";
        }
        File pom = new File(projectDir, "pom.xml");
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pom.isFile() ? pom : new File(project.getBasedir(), "pom.xml"));
        request.setGoals(Collections.singletonList("process-classes"));
        Properties props = new Properties();
        props.setProperty("skipTests", "true");
        request.setProperties(props);
        request.setBatchMode(true);
        try {
            InvocationResult result = new DefaultInvoker().execute(request);
            if (result.getExitCode() != 0) {
                return "The build failed with exit code " + result.getExitCode()
                        + ", so the annotations could not be checked.";
            }
        } catch (MavenInvocationException ex) {
            return "The build could not be run (" + ex.getMessage()
                    + "), so the annotations could not be checked.";
        }

        if (!emitted.isFile()) {
            return "No " + ANNOTATION_HINTS_RESOURCE + " was written under "
                    + projectDir.getName() + "/target/classes.";
        }
        Properties produced = new Properties();
        try (FileInputStream in = new FileInputStream(emitted)) {
            produced.load(in);
        } catch (IOException ex) {
            return "Could not read " + emitted + ": " + ex.getMessage();
        }
        List<String> absent = new ArrayList<String>();
        for (String key : migratedKeys) {
            if (produced.getProperty(key) == null) {
                absent.add(key);
            }
        }
        if (!absent.isEmpty()) {
            return "These hints were annotated but did not come back out of the build: " + absent;
        }
        return null;
    }

    /** Name of the resource the annotation processor emits into target/classes. */
    private static final String ANNOTATION_HINTS_RESOURCE =
            "META-INF/codenameone/build-hints.properties";

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
                    return quoteFor(v, kotlin);
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
                    sb.append(quoteFor(t, kotlin));
                }
                return sb.append(kotlin ? ']' : '}').toString();
            }
            default:
                return quoteFor(v, kotlin);
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

    /**
     * Renders a value as a string literal for the target language.
     *
     * <p>Kotlin interpolates {@code $} inside a string, and hint values contain
     * it: an {@code android.gradleDep} of
     * {@code implementation "com.x:y:${'$'}{version}"} would either fail to
     * compile as an unresolved reference or silently resolve to something else.
     * Java has no such construct, so the escape is emitted only for Kotlin.</p>
     */
    static String quoteFor(String s, boolean kotlin) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '$':
                    sb.append(kotlin ? "\\$" : "$");
                    break;
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
            // No existing import. Anchor on the package declaration, and when the
            // class is in the default package anchor on the class declaration
            // instead: indexOf("package ") returns -1 there, and the old
            // arithmetic then put the import at the first newline in the file,
            // which is inside the copyright comment. The result compiled to
            // nothing useful while the properties entries had already been
            // deleted.
            int pkg = head.indexOf("package ");
            int anchor = pkg >= 0 ? head.indexOf('\n', pkg) + 1 : head.length();
            head = head.substring(0, anchor) + (pkg >= 0 ? "\n" : "")
                    + importLine + "\n" + (pkg >= 0 ? "" : "\n") + head.substring(anchor);
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
    /**
     * Deletes the migrated declarations, leaving every other line -- comments,
     * ordering, unrelated settings -- byte for byte as it was.
     *
     * <p>Keys are recognised the way {@code Properties.load} defines them, not
     * just {@code key=value}: {@code key:value} and {@code key value} are equally
     * valid, and a line ending in an odd number of backslashes continues onto the
     * next. A declaration this pass fails to recognise is left behind while the
     * annotation is added, and the very next build fails with the duplicate-hint
     * error this goal exists to avoid.</p>
     *
     * <p>Written back as ISO-8859-1 because that is what {@code Properties.load}
     * reads a {@code .properties} stream as. Rewriting the file as UTF-8 would
     * turn any non-ASCII byte elsewhere in it -- an accented
     * {@code codename1.displayName}, say -- into mojibake, even though it has
     * nothing to do with the hint being migrated.</p>
     */
    private void removeMigratedLines(File settingsFile, List<String> keys) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(settingsFile), PROPERTIES_ENCODING));
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
        for (int i = 0; i < lines.size(); i++) {
            // Gather the whole logical line: continuations belong to the same
            // declaration and have to go with it.
            int last = i;
            StringBuilder logical = new StringBuilder(lines.get(i));
            while (continues(lines.get(last)) && last + 1 < lines.size()) {
                last++;
                logical.append(lines.get(last).replaceFirst("^\\s+", ""));
            }
            String key = propertyKeyOf(logical.toString());
            if (key != null && wanted.containsKey(key)) {
                i = last;
                continue;
            }
            for (int j = i; j <= last; j++) {
                out.append(lines.get(j)).append('\n');
            }
            i = last;
        }
        writeProperties(settingsFile, out.toString());
    }

    /** Whether a physical line ends in an odd number of backslashes. */
    private static boolean continues(String line) {
        int backslashes = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 != 0;
    }

    /**
     * The key a logical properties line declares, or null when the line is blank
     * or a comment.
     *
     * <p>Follows {@code java.util.Properties}: the key runs to the first
     * unescaped {@code =}, {@code :} or whitespace.</p>
     */
    static String propertyKeyOf(String logicalLine) {
        int i = 0;
        while (i < logicalLine.length() && isPropertySpace(logicalLine.charAt(i))) {
            i++;
        }
        if (i >= logicalLine.length()) {
            return null;
        }
        char first = logicalLine.charAt(i);
        if (first == '#' || first == '!') {
            return null;
        }
        StringBuilder key = new StringBuilder();
        for (; i < logicalLine.length(); i++) {
            char c = logicalLine.charAt(i);
            if (c == '\\' && i + 1 < logicalLine.length()) {
                key.append(logicalLine.charAt(++i));
                continue;
            }
            if (c == '=' || c == ':' || isPropertySpace(c)) {
                break;
            }
            key.append(c);
        }
        return key.length() == 0 ? null : key.toString();
    }

    private static boolean isPropertySpace(char c) {
        return c == ' ' || c == '\t' || c == '\f';
    }

    /** The encoding {@code Properties.load(InputStream)} reads. */
    private static final String PROPERTIES_ENCODING = "ISO-8859-1";

    private static void writeProperties(File f, String content) throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(f), PROPERTIES_ENCODING);
        try {
            w.write(content);
        } finally {
            w.close();
        }
    }

    /**
     * Reads a properties file as ISO-8859-1, matching {@link #writeProperties}.
     *
     * <p>The rollback snapshot has to round-trip byte for byte. Taking it through
     * the UTF-8 {@link #read} and restoring it with the ISO-8859-1 writer would
     * mangle any raw high byte in an unrelated property -- an accented
     * {@code codename1.displayName}, say -- while the goal reports that both
     * files were put back as they were.</p>
     */
    private static String readProperties(File f) throws IOException {
        return read(f, PROPERTIES_ENCODING);
    }

    /**
     * Reads a source file byte-transparently.
     *
     * <p>ISO-8859-1 maps every byte 0-255 to the same char, so decoding with it,
     * splicing in text that is pure ASCII, and encoding back reproduces the
     * original bytes exactly -- whatever the project's real source encoding is.
     * Hard-coding UTF-8 here reinterpreted the whole file, so a raw byte in a
     * comment or a string literal came back changed even when the migration
     * succeeded, and reading {@code project.build.sourceEncoding} would only
     * narrow that to projects that declare it correctly.</p>
     *
     * <p>The markers this class searches for -- {@code package}, {@code import},
     * the class declaration -- are ASCII, and every ASCII-compatible encoding
     * decodes them identically under this scheme.</p>
     */
    private static String read(File f) throws IOException {
        return read(f, SOURCE_BYTE_TRANSPARENT_ENCODING);
    }

    /** See {@link #read(File)}: byte-transparent, not a claim about the file. */
    private static final String SOURCE_BYTE_TRANSPARENT_ENCODING = "ISO-8859-1";

    private static String read(File f, String encoding) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), encoding));
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

    /** Restores a source file after a failed migration. */
    private static void writeSource(File f, String content) throws IOException {
        write(f, content);
    }

    private static void write(File f, String content) throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(f), SOURCE_BYTE_TRANSPARENT_ENCODING);
        try {
            w.write(content);
        } finally {
            w.close();
        }
    }
}
