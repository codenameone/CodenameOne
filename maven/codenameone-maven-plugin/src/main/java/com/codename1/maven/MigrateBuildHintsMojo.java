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
        /// Canonical keys to look for in the emitted manifest. Not the same list:
        /// a legacy spelling is deleted under the name the file used and comes
        /// back under the name the annotation carries.
        List<String> verifiedKeys = new ArrayList<String>();
        List<String> skipped = new ArrayList<String>();
        /// Canonical hint name to every properties key that names it.
        Map<String, List<String>> declaredAs = new TreeMap<String, List<String>>();
        Map<String, BuildHints.Hint> byHint = new TreeMap<String, BuildHints.Hint>();

        for (String key : new ArrayList<String>(settings.stringPropertyNames())) {
            if (!key.startsWith(BuildHints.ARG_PREFIX)) {
                continue;
            }
            String name = key.substring(BuildHints.ARG_PREFIX.length());
            if (kept.contains(name)) {
                skipped.add(name + " (kept by configuration)");
                continue;
            }
            // Through the alias, not at it. byName("cn1.androidTheme") returns the
            // alias entry, whose own isAnnotated() is false even though the
            // setting it names has an annotation -- so the legacy spellings, which
            // are exactly the ones an existing project is most likely to be
            // carrying, were reported as having no annotation and left behind.
            BuildHints.Hint hint = BuildHints.resolve(BuildHints.byName(name));
            if (hint == null || !hint.isAnnotated()) {
                skipped.add(name + " (no annotation for this hint yet)");
                continue;
            }
            List<String> spellings = declaredAs.get(hint.name());
            if (spellings == null) {
                spellings = new ArrayList<String>();
                declaredAs.put(hint.name(), spellings);
                byHint.put(hint.name(), hint);
            }
            spellings.add(key);
        }

        for (Map.Entry<String, List<String>> e : declaredAs.entrySet()) {
            BuildHints.Hint hint = byHint.get(e.getKey());
            List<String> spellings = e.getValue();

            // One setting, spelled more than one way, with the two disagreeing.
            // Which one the build honours is decided per hint and not always by
            // the builder: and.captureRecord is read after android.captureRecord
            // and overrides it, while the theme aliases are each handed to
            // Display.setProperty and resolved in the framework. So there is no
            // rule to apply here, and picking either value would change what the
            // app builds with while reporting a successful migration -- the
            // verification cannot catch it, since it checks that the hint came
            // back and not what it holds. The developer decides.
            String value = settings.getProperty(spellings.get(0));
            boolean disagree = false;
            for (String other : spellings) {
                String v = settings.getProperty(other);
                if (value == null ? v != null : !value.equals(v)) {
                    disagree = true;
                }
            }
            if (disagree) {
                skipped.add(e.getKey() + " (declared as " + spellings + " with different values; "
                        + "delete all but one and run this again)");
                continue;
            }

            String literal = toSourceLiteral(hint, value, kotlinTarget);
            if (literal == null) {
                boolean padded = value != null && !value.equals(value.trim());
                skipped.add(e.getKey() + " = '" + value + "' ("
                        + (padded
                            ? "has surrounding whitespace, which builders read differently; "
                              + "remove it and run this again"
                            : "value is outside the hint's supported set")
                        + ")");
                continue;
            }
            String annotation = hint.group().annotationSimpleName();
            Map<String, String> members = plan.get(annotation);
            if (members == null) {
                members = new TreeMap<String, String>();
                plan.put(annotation, members);
            }
            members.put(hint.attr(), literal);
            // Every spelling goes, because they were all naming this one setting.
            migratedKeys.addAll(spellings);
            // Verified under the CANONICAL key: that is what the annotation makes
            // the processor emit. Checking the alias the file happened to use
            // reported it missing and rolled a correct migration back.
            verifiedKeys.add(BuildHints.ARG_PREFIX + hint.name());
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

        String missing = verifyAnnotationsAreProcessed(projectDir, verifiedKeys);
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
        // Where the processor will actually write, which is not always
        // target/classes: a module is free to configure build/outputDirectory,
        // and looking in the wrong place would report a successful build as
        // having produced nothing and roll a correct migration back.
        org.apache.maven.project.MavenProject owner = moduleAt(projectDir);
        File outputDir = configuredOutputDirectory(owner, projectDir);
        File emitted = new File(outputDir, ANNOTATION_HINTS_RESOURCE);
        if (emitted.isFile() && !emitted.delete()) {
            return "Could not remove the previous " + ANNOTATION_HINTS_RESOURCE
                    + ", so this build's output could not be told apart from it.";
        }
        // Run the REACTOR and select the owning module, rather than pointing
        // Maven at that module's own POM. A module POM on its own resolves its
        // siblings from the local repository, so a project whose main module
        // depends on another module of the same build -- normal, and not
        // necessarily installed -- fails to resolve here and the migration rolls
        // back over a build that a plain `mvn package` performs happily.
        File modulePom = new File(projectDir, "pom.xml");
        File reactorPom = new File(project.getBasedir(), "pom.xml");
        InvocationRequest request = new DefaultInvocationRequest();
        if (reactorPom.isFile() && owner != null) {
            request.setPomFile(reactorPom);
            request.setProjects(Collections.singletonList(
                    owner.getGroupId() + ":" + owner.getArtifactId()));
            request.setAlsoMake(true);
        } else {
            request.setPomFile(modulePom.isFile() ? modulePom : reactorPom);
        }
        request.setGoals(Collections.singletonList("process-classes"));
        Properties props = new Properties();
        props.setProperty("skipTests", "true");
        // Reproduce the invocation the developer actually made. A project that
        // needs `-Pcustomer` to compile, or `-Dfeature=true` to bind
        // process-annotations, is a different build without them: the check
        // would roll back a migration that works, or -- worse -- pass a build
        // whose processing an outer -D was switching off. The user properties go
        // in first so skipTests below cannot be silently overridden by one.
        if (getSession() != null) {
            Properties user = getSession().getUserProperties();
            if (user != null) {
                for (String name : user.stringPropertyNames()) {
                    props.setProperty(name, user.getProperty(name));
                }
            }
            props.setProperty("skipTests", "true");
            List<String> profiles = activeProfileIds();
            if (!profiles.isEmpty()) {
                request.setProfiles(profiles);
            }
        }
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
            return "No " + ANNOTATION_HINTS_RESOURCE + " was written under " + outputDir + ".";
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

    /**
     * The profiles this invocation was started with, by id.
     *
     * <p>Taken from the request rather than from the resolved project, because
     * what has to be reproduced is what the developer typed: a profile activated
     * by a property or a file is activated again on its own terms in the nested
     * build, while one named with {@code -P} is not unless it is passed on.</p>
     */
    private List<String> activeProfileIds() {
        List<String> out = new ArrayList<String>();
        if (getSession() == null || getSession().getRequest() == null) {
            return out;
        }
        List<String> active = getSession().getRequest().getActiveProfiles();
        if (active != null) {
            for (String id : active) {
                if (id != null && id.trim().length() > 0) {
                    out.add(id.trim());
                }
            }
        }
        return out;
    }

    /** The reactor module whose basedir is {@code dir}, or null when none is. */    /** The reactor module whose basedir is {@code dir}, or null when none is. */
    private org.apache.maven.project.MavenProject moduleAt(File dir) {
        if (reactorProjects == null || dir == null) {
            return null;
        }
        File wanted = canonical(dir);
        for (org.apache.maven.project.MavenProject p : reactorProjects) {
            if (p.getBasedir() != null && canonical(p.getBasedir()).equals(wanted)) {
                return p;
            }
        }
        return null;
    }

    /**
     * The directory {@code process-classes} writes compiled output to.
     *
     * <p>Read off the module rather than assumed, since a POM may configure it.
     * Falls back to the conventional path when the directory is not a reactor
     * module at all -- an Ant-layout project, for instance.</p>
     */
    private static File configuredOutputDirectory(org.apache.maven.project.MavenProject owner,
                                                  File projectDir) {
        if (owner != null && owner.getBuild() != null
                && owner.getBuild().getOutputDirectory() != null
                && owner.getBuild().getOutputDirectory().length() > 0) {
            return new File(owner.getBuild().getOutputDirectory());
        }
        return new File(projectDir, "target" + File.separator + "classes");
    }

    private static File canonical(File f) {
        try {
            return f.getCanonicalFile();
        } catch (IOException ex) {
            return f.getAbsoluteFile();
        }
    }

    /** Name of the resource the annotation processor emits into target/classes. */    /** Name of the resource the annotation processor emits into target/classes. */
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

    /// Whether `value` is spelled exactly as the domain declares it, alias or not.
    ///
    /// canonicalValue ignores case because a reader might; the migration cannot
    /// afford to, because a reader might not.
    private static boolean exactlySpelled(BuildHints.Hint hint, String value) {
        for (String allowed : hint.values()) {
            if (allowed.equals(value)) {
                return true;
            }
        }
        return hint.valueAliases().containsKey(value);
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
        // A scalar with surrounding whitespace is NOT migrated at all. It looks
        // like it means what it says, and it does not: AndroidGradleBuilder
        // compares android.hideStatusBar with .equals("true"), so `=true ` is
        // false today, while other builders trim or ignore case. Trimming it into
        // an annotation `true` would change what the app builds with and report a
        // successful migration, since the verification asks whether the hint came
        // back and not what it holds. Which reading is right differs per builder,
        // so this refuses rather than picks.
        if (hint.type() != HintType.STRING && hint.type() != HintType.STRING_LIST
                && !value.equals(value.trim())) {
            return null;
        }
        String v = value;
        switch (hint.type()) {
            case BOOLEAN:
                // Exactly, not ignoring case. AndroidGradleBuilder compares
                // android.hideStatusBar with .equals("true"), so `=TRUE` is false
                // today, while other hints are read with equalsIgnoreCase. Which
                // applies is per hint and this cannot know, so a value that is not
                // already canonical is refused rather than normalised into one
                // that may mean the opposite.
                if ("true".equals(v)) return "true";
                if ("false".equals(v)) return "false";
                return null;
            case INT:
                try {
                    // Round-tripped for the same reason: 007 and +5 parse, and a
                    // builder comparing the raw string would not see the 7 or 5
                    // this would otherwise write.
                    String canonical = String.valueOf(Integer.parseInt(v));
                    return canonical.equals(v) ? canonical : null;
                } catch (NumberFormatException ex) {
                    return null;
                }
            case ENUM: {
                // A documented spelling that is not its own constant migrates to
                // the constant it means -- ios.themeMode=flat becomes
                // IosThemeMode.IOS7 -- rather than being refused as outside the
                // domain, which is what an existing project setting a legacy
                // spelling would have hit.
                //
                // Case-sensitively, though: installNativeTheme compares with
                // .equals, so `MODERN` is not `modern` to the runtime and
                // migrating it would change the theme rather than preserve it.
                String canonical = hint.canonicalValue(v);
                if (canonical == null || !exactlySpelled(hint, v)) {
                    return null;
                }
                return hint.enumName() + "." + enumConstant(canonical);
            }
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
     *
     * <p>Everything outside ASCII is written as a {@code \}{@code uXXXX} escape,
     * which both languages accept. {@code Properties.load} turns a
     * {@code \}{@code u20ac} in the file into a real euro sign, and the source is
     * written back through ISO-8859-1 to keep the rest of the file byte-identical
     * -- so emitting the character raw would replace it with {@code ?}, or write a
     * high byte that corrupts a UTF-8 source. Neither shows up in the
     * verification build, which checks that the hint came back, not what its
     * value was.</p>
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
                default:
                    if (c < 0x20 || c > 0x7e) {
                        sb.append("\\u");
                        for (int shift = 12; shift >= 0; shift -= 4) {
                            sb.append(Character.forDigit((c >> shift) & 0xf, 16));
                        }
                    } else {
                        sb.append(c);
                    }
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
        String simple = main.trim();
        String expectedPkg = pkg == null ? "" : pkg.trim();
        String[] roots = {"src" + File.separator + "main" + File.separator + "java",
                          "src" + File.separator + "main" + File.separator + "kotlin",
                          "src"};
        String[] extensions = {".java", ".kt"};
        for (String root : roots) {
            for (String ext : extensions) {
                File f = new File(projectDir, root + File.separator + path + ext);
                // Declaring the class, not merely sitting at the conventional
                // path: a Kotlin main class moved into a differently named file
                // can leave the old one behind holding something else.
                if (f.isFile() && declares(f, expectedPkg, simple)) {
                    return f.getAbsolutePath();
                }
            }
        }
        // Those three are a convention. Maven is the authority on where this
        // module's sources are -- a module may add src/app/java or a Kotlin root
        // -- and Kotlin does not require a file to be named after its class, so
        // the file is identified by what it DECLARES. Without this the goal
        // aborted with "Could not find the source" on a project Maven compiles
        // perfectly well.
        org.apache.maven.project.MavenProject owner = moduleAt(projectDir);
        if (owner == null || owner.getCompileSourceRoots() == null) {
            return null;
        }
        for (String root : owner.getCompileSourceRoots()) {
            File dir = new File(root);
            if (!dir.isDirectory()) {
                continue;
            }
            File hit = findDeclaringFile(dir, expectedPkg, simple, 0);
            if (hit != null) {
                return hit.getAbsolutePath();
            }
        }
        return null;
    }

    /// The first .java or .kt under `dir` declaring `simple` in `pkg`, or null.
    private File findDeclaringFile(File dir, String pkg, String simple, int depth) {
        if (depth > 24) {
            return null;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return null;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                File hit = findDeclaringFile(f, pkg, simple, depth + 1);
                if (hit != null) {
                    return hit;
                }
            } else if ((f.getName().endsWith(".java") || f.getName().endsWith(".kt"))
                    && declares(f, pkg, simple)) {
                return f;
            }
        }
        return null;
    }

    /// Whether `f` declares type `simple` in package `pkg`.
    ///
    /// Through the annotation processor's helpers rather than a second copy: the
    /// two have already drifted apart once in this change, and "what counts as a
    /// declaration" should have one answer.
    private boolean declares(File f, String pkg, String simple) {
        String text;
        try {
            text = read(f);
        } catch (IOException ex) {
            return false;
        }
        return pkg.equals(com.codename1.maven.processors.BuildHintAnnotationProcessor
                        .declaredPackageIn(text))
                && com.codename1.maven.processors.BuildHintAnnotationProcessor
                        .declaresType(text, simple);
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
        // Top level means brace depth zero, not column zero. Anchoring the
        // pattern to the start of a line refused `  public class MyApp`, which
        // compiles perfectly well -- so the goal rolled back with "Could not find
        // the class declaration" on a project whose source it had just accepted
        // through the token-aware lookup.
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(text);
        int first = -1;
        int depth = 0;
        int i = 0;
        while (i < code.length()) {
            char c = code.charAt(i);
            if (c == '{') {
                depth++;
                i++;
                continue;
            }
            if (c == '}') {
                depth--;
                i++;
                continue;
            }
            if (depth != 0 || !Character.isJavaIdentifierStart(c)
                    || (i > 0 && Character.isJavaIdentifierPart(code.charAt(i - 1)))) {
                i++;
                continue;
            }
            int wordEnd = i;
            while (wordEnd < code.length()
                    && Character.isJavaIdentifierPart(code.charAt(wordEnd))) {
                wordEnd++;
            }
            String word = code.substring(i, wordEnd);
            if (isTypeKind(word, kotlin)) {
                int n = wordEnd;
                while (n < code.length() && Character.isWhitespace(code.charAt(n))) {
                    n++;
                }
                int end = n;
                while (end < code.length()
                        && Character.isJavaIdentifierPart(code.charAt(end))) {
                    end++;
                }
                if (end > n) {
                    // The declaration's own modifiers come before the keyword, and
                    // the annotations have to go before those.
                    int start = startOfModifiers(code, i);
                    if (simpleName != null && simpleName.length() > 0
                            && code.substring(n, end).equals(simpleName)) {
                        return start;
                    }
                    if (first < 0) {
                        first = start;
                    }
                }
            }
            i = wordEnd;
        }
        return first;
    }

    private static boolean isTypeKind(String word, boolean kotlin) {
        if (kotlin) {
            return "class".equals(word) || "object".equals(word) || "interface".equals(word);
        }
        return "class".equals(word) || "interface".equals(word) || "enum".equals(word)
                || "record".equals(word);
    }

    /// Back up over the modifiers preceding the keyword at `at`, so the
    /// annotations land above `public final class` rather than inside it.
    private static int startOfModifiers(String code, int at) {
        int start = at;
        while (true) {
            int i = start - 1;
            while (i >= 0 && (code.charAt(i) == ' ' || code.charAt(i) == '\t')) {
                i--;
            }
            if (i < 0) {
                return start;
            }
            int wordEnd = i + 1;
            while (i >= 0 && (Character.isJavaIdentifierPart(code.charAt(i))
                    || code.charAt(i) == '-')) {
                i--;
            }
            String word = code.substring(i + 1, wordEnd);
            if (word.length() == 0 || !isModifier(word)) {
                return start;
            }
            start = i + 1;
        }
    }

    private static boolean isModifier(String word) {
        String[] modifiers = {"public", "protected", "private", "abstract", "final", "static",
                              "strictfp", "sealed", "non-sealed", "internal", "open", "data",
                              "value", "annotation", "inner", "expect", "actual"};
        for (String m : modifiers) {
            if (m.equals(word)) {
                return true;
            }
        }
        return false;
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
     * unescaped {@code =}, {@code :} or whitespace, and {@code \}{@code uXXXX}
     * decodes to the character it names. The escape matters because the key this
     * returns is compared against one {@code Properties.load} produced: a file
     * writing {@code codename1.arg.\}{@code u0069os.teamId} declares
     * {@code ios.teamId}, and reading it as {@code u0069os.teamId} leaves the
     * original line in place, so the migration rolls back over a duplicate
     * declaration it created itself.</p>
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
                char escaped = logicalLine.charAt(++i);
                if (escaped == 'u' && i + 4 < logicalLine.length()) {
                    String hex = logicalLine.substring(i + 1, i + 5);
                    int value = hexValue(hex);
                    if (value >= 0) {
                        key.append((char) value);
                        i += 4;
                        continue;
                    }
                }
                key.append(escaped);
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

    /** Four hex digits as a char value, or -1 when they are not four hex digits. */
    private static int hexValue(String hex) {
        int value = 0;
        for (int i = 0; i < hex.length(); i++) {
            int digit = Character.digit(hex.charAt(i), 16);
            if (digit < 0) {
                return -1;
            }
            value = value * 16 + digit;
        }
        return value;
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
