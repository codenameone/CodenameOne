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

import com.codename1.build.BuildFailureException;
import com.codename1.build.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/// Overlays the build hints declared as annotations on the main class onto the
/// settings a build is assembled from, and refuses the builds where those
/// annotations cannot have reached it.
///
/// Moved out of the Maven plugin's `AbstractCN1Mojo` so the Gradle plugin makes
/// the same decisions. The only thing the build tool has to answer is
/// [Sources]: which source roots produced a classpath element.
public final class AnnotationBuildHints {
    /// Where the classes in a classpath element were compiled from.
    ///
    /// In a Maven application the classes come from `common` while a platform
    /// module runs the build, so the running module's own roots answer for the
    /// wrong module; the build tool maps the element back to the module that
    /// produced it. A single-project build (Gradle) has one answer for all.
    public interface Sources {
        /// Every source root the producing module compiles, or empty when unknown.
        List<String> rootsFor(File element);

        /// The source encoding the producing module compiles with, or null.
        String encodingFor(File element);
    }

    private final Log log;
    private final Properties settings;
    private final Properties userProperties;
    private final Sources sources;

    /// @param settings the project's `codenameone_settings.properties`, for the
    ///        main class the hints must be stamped with; may be null
    /// @param userProperties the command-line properties, which win over an
    ///        annotation; may be null
    public AnnotationBuildHints(Log log, Properties settings, Properties userProperties, Sources sources) {
        this.log = log;
        this.settings = settings;
        this.userProperties = userProperties;
        this.sources = sources;
    }

    /**
     * Name of the resource {@code BuildHintAnnotationProcessor} emits into
     * {@code target/classes} at PROCESS_CLASSES.
     */
    private static final String ANNOTATION_HINTS_RESOURCE =
            "META-INF/codenameone/build-hints.properties";

    /**
     * Overlays the build hints that came from annotations onto the settings the
     * build request is assembled from.
     *
     * <p>Read from the compile classpath rather than from the staged fat jar.
     * {@code mergeJars} builds that jar with Ant's {@code Zip} in update mode,
     * which adds and overwrites entries but never removes one, and the jar is
     * reused when it is not stale &mdash; so a project that had its annotations
     * deleted would keep shipping yesterday's hints. The classpath directory is
     * written by the annotation processor on every build and deleted by it when
     * the last annotation goes away, so it always reflects the current source.</p>
     */
    public void merge(Properties target, List<String> classpathElements)
            throws BuildFailureException {
        if (target == null || classpathElements == null) {
            return;
        }
        String expectedMain = null;
        if (settings != null) {
            String main = settings.getProperty("codename1.mainName");
            String pkg = settings.getProperty("codename1.packageName");
            if (main != null && main.trim().length() > 0) {
                expectedMain = (pkg == null || pkg.trim().length() == 0)
                        ? main.trim() : pkg.trim() + "." + main.trim();
            }
        }
        // The manifest's presence, not its contents, is what proves the processor
        // ran. An annotation with every member left at its default -- @Ios() after
        // the last attribute was deleted -- is legal Java, and the processor emits
        // a manifest carrying only the main-class stamp for it. Judging by the hint
        // count alone would read that as "never processed" and refuse a build that
        // is in fact perfectly configured.
        String stale = null;
        // The processor writes the manifest into the SAME directory it scanned,
        // so a manifest stamped for a main class was written beside that class.
        // An element carrying the stamp without the class is therefore left over
        // -- the shape is moving the application class from a platform module
        // into common without cleaning -- and project output comes before
        // dependencies on the classpath, so the leftover was found first, applied
        // its old values and returned before the real one was ever read.
        //
        // Ordered rather than refused. Refusing assumes the two can never be
        // packaged apart, and an element that assembles resources from another
        // module would then lose its hints entirely; putting the colocated ones
        // first fixes the stale-wins bug and still falls back to a manifest that
        // is all there is.
        for (String element : colocatedFirst(classpathElements, expectedMain)) {
            Properties found = readAnnotationHints(new File(element));
            if (found == null) {
                continue;
            }
            String stamped = found.getProperty("cn1.buildHints.mainClass");
            if (expectedMain != null && !expectedMain.equals(stamped)) {
                // A cn1lib that bound the goal itself, a stale artifact, or a
                // file a project keeps in src/main/resources -- anything on the
                // classpath can carry this name. It has to say it was generated
                // for THIS main class: an unstamped one used to be accepted, and
                // then it both applied somebody else's hints and counted as
                // proof that the processor ran, which is what suppresses the
                // refusal below when the goal is not bound at all.
                log.debug("cn1: ignoring build hints from " + element
                        + " -- they were generated for " + stamped);
                continue;
            }
            if (found.getProperty(com.codename1.maven.processors.BuildHintAnnotationProcessor
                    .SOURCE_DIGEST_KEY) == null) {
                // The processor always records one. A manifest without it was
                // written by something else.
                log.debug("cn1: ignoring build hints from " + element
                        + " -- no processor fingerprint");
                continue;
            }
            // The stamp says which class produced this file, not when. Nothing
            // clears target/classes between builds, so a project that ran the
            // processor once and then stopped -- goal unbound, skipped, or bound
            // to a phase that no longer runs -- keeps a manifest naming the right
            // class while the annotations beside it have changed. Comparing the
            // recorded fingerprint against the compiled class is what tells those
            // apart; without it the build silently ships the older values and the
            // guard below never runs.
            String mismatch = digestMismatch(new File(element), expectedMain, found, classpathElements);
            if (mismatch != null) {
                log.debug("cn1: ignoring build hints from " + element + " -- " + mismatch);
                stale = mismatch;
                continue;
            }
            int applied = 0;
            for (String key : found.stringPropertyNames()) {
                if (!key.startsWith("codename1.arg.")) {
                    continue;
                }
                // The processor refuses a hint declared as an annotation and as a
                // properties line, but it can only refuse the builds it runs in.
                // The fingerprint above covers the annotations and nothing else,
                // so with processing skipped a line added to the properties file
                // afterwards leaves a manifest that still matches -- and this
                // overlay would quietly replace the value the developer just
                // wrote, until the next clean build regenerated the manifest and
                // failed. Same declaration, same answer, whether or not
                // target/classes happened to be cleaned.
                String conflict = conflictingPropertiesDeclaration(target, key, found);
                if (conflict != null) {
                    throw new BuildFailureException(conflict);
                }
                if (overriddenOnTheCommandLine(key)) {
                    // -D wins, and it wins whichever SPELLING it used. The
                    // overlay below only replaces the same key, so
                    // -Dcodename1.arg.cn1.androidTheme against an annotated
                    // and.themeMode left both set -- and the two readers then
                    // disagreed with each other: JavaSEPort takes the canonical
                    // and falls back to the alias, so the annotation won in the
                    // simulator, while AndroidGradleBuilder writes both and the
                    // alias landed last, so -D won on the device. Same command
                    // line, opposite results.
                    log.debug("cn1: " + key + " comes from the command line, "
                            + "so the annotation value is not applied");
                    continue;
                }
                target.setProperty(key, found.getProperty(key));
                applied++;
            }
            // The FIRST accepted manifest is the answer, whether or not it set
            // anything. Continuing when it happened to apply nothing let a later
            // element decide instead: `@Ios()` with no members -- legal Java, and
            // what is left after the last attribute is deleted -- produces a
            // current manifest with no hint keys, and an older copy of the same
            // main class further down the classpath carries a manifest that
            // passes its own digest check, because it is fingerprinted against
            // the stale class sitting beside it. Its obsolete hints were then
            // applied over the authoritative empty one. The same is true of a
            // manifest whose every key is overridden on the command line.
            if (applied > 0) {
                log.info("cn1: applied " + applied + " build hint(s) from annotations");
            } else {
                log.debug("cn1: annotations were processed and set no build hint");
            }
            failOnMisplacedAnnotations(classpathElements, expectedMain);
            return;
        }
        // No manifest at all. If the compiled classes carry build hint
        // annotations anyway, the processor never ran -- a mojo's defaultPhase
        // does not add an execution to a project's POM, so an app that adopts the
        // annotations without binding process-annotations compiles cleanly and
        // ships with every annotated hint missing. Refuse rather than build that.
        String annotated = classCarryingBuildHintAnnotations(classpathElements, expectedMain);
        if (annotated != null) {
            throw new BuildFailureException(annotated + " carries build hint annotations, but "
                    + (stale == null
                        ? "no " + ANNOTATION_HINTS_RESOURCE + " was produced"
                        : "the only " + ANNOTATION_HINTS_RESOURCE + " on the classpath is left "
                          + "over from an earlier build (" + stale + ")")
                    + ", so none of them reached this "
                    + "build.\n\nThe cn1 process-annotations goal has to run on the module that "
                    + "compiles it:\n"
                    + "    <execution>\n"
                    + "      <id>cn1-process-classes</id>\n"
                    + "      <phase>process-classes</phase>\n"
                    + "      <goals>\n"
                    + "        <goal>process-annotations</goal>\n"
                    + "      </goals>\n"
                    + "    </execution>");
        }
    }

    /**
     * Refuses a build where a class other than the main one carries build hint
     * annotations.
     *
     * <p>Run even when a manifest was accepted, because accepting one does not
     * mean the processor ran THIS build: the fingerprint covers the main class,
     * so an annotation added to a live class beside it leaves the manifest
     * looking entirely current. With the goal unbound or skipped, the hints from
     * that class reach nothing and the build succeeds having neither applied
     * them nor said the annotation is in the wrong place -- the silent failure
     * this whole feature exists to remove. Had the processor run, it would have
     * refused the build for the same class.</p>
     */
    private void failOnMisplacedAnnotations(List<String> classpathElements, String expectedMain)
            throws BuildFailureException {
        if (expectedMain == null) {
            return;
        }
        java.util.Collection<String> descriptors = hintAnnotationDescriptors(classpathElements);
        for (String element : classpathElements) {
            String live = liveAnnotatedClass(new File(element), descriptors, expectedMain);
            if (live != null) {
                throw new BuildFailureException(live + " carries build hint annotations, but they "
                        + "are only read from the application's main class (" + expectedMain
                        + "), so none of them reached this build.\n\nMove them onto "
                        + expectedMain + ", or set those hints in "
                        + "codenameone_settings.properties.");
            }
        }
    }

    /**
     * The duplicate-declaration message for a hint set by an annotation and by a
     * properties line, or null when there is no clash.
     *
     * <p>Only reached when the processor did not run this build; when it did, it
     * has already failed for the same reason and with more to say -- it can point
     * at the offending line. An alias counts as the same setting, matching what
     * the processor checks, so declaring {@code and.captureRecord} in the file
     * still collides with {@code @Android(captureRecord)}.</p>
     */
    private String conflictingPropertiesDeclaration(Properties settings, String key,
                                                    Properties manifest) {
        String name = key.substring("codename1.arg.".length());
        java.util.Set<String> names = new java.util.LinkedHashSet<String>();
        names.add(name);
        for (com.codename1.build.shared.BuildHints.Hint h
                : com.codename1.build.shared.BuildHints.entries()) {
            if (name.equals(h.aliasOf())
                    || name.equals(com.codename1.build.shared.BuildHints.canonicalName(h.name()))) {
                names.add(h.name());
            }
        }
        for (String candidate : names) {
            String candidateKey = "codename1.arg." + candidate;
            String fromFile = settings.getProperty(candidateKey);
            if (fromFile == null) {
                continue;
            }
            String origin = manifest.getProperty("cn1.buildHints.origin." + name);
            return candidateKey + " is declared twice.\n"
                    + "    annotation : " + (origin == null ? "on the main class" : origin)
                    + " = " + manifest.getProperty(key) + "\n"
                    + "    properties : codenameone_settings.properties\n"
                    + "                 " + candidateKey + "=" + fromFile + "\n"
                    + "    A build hint has one source of truth. Delete the properties line and "
                    + "keep the annotation, or delete the annotation attribute and keep the line. "
                    + "(-D" + candidateKey + "=... overrides either and is not a conflict.)";
        }
        return null;
    }

    /**
     * {@code classpathElements}, with those containing {@code expectedMain}'s
     * class file first and the relative order otherwise preserved.
     *
     * @param classpathElements the compile classpath, in Maven's order
     * @param expectedMain the binary name of the app's main class, or null
     * @return the elements to search, colocated ones first
     */
    private List<String> colocatedFirst(List<String> classpathElements, String expectedMain) {
        if (expectedMain == null) {
            return classpathElements;
        }
        List<String> beside = new ArrayList<String>();
        List<String> rest = new ArrayList<String>();
        for (String element : classpathElements) {
            boolean here;
            try {
                here = readClass(new File(element), expectedMain) != null;
            } catch (IOException | com.codename1.maven.annotations.ProcessingException ex) {
                // Unreadable is not evidence either way; leave it where it was.
                log.debug("cn1: could not look for " + expectedMain + " in " + element, ex);
                here = false;
            }
            (here ? beside : rest).add(element);
        }
        beside.addAll(rest);
        return beside;
    }

    /**
     * Why a manifest cannot have come from the class beside it, or null when it can.
     *
     * <p>Answered only when both halves are actually available: with no
     * {@code codename1.mainName}, no class file for it ANYWHERE on the classpath,
     * or no recorded fingerprint, there is nothing to compare and the manifest is
     * taken at face value -- the same as before this check existed. It refuses
     * only on positive evidence of a mismatch.</p>
     */
    private String digestMismatch(File element, String expectedMain, Properties manifest,
                                  List<String> classpathElements) {
        String recorded = manifest.getProperty(
                com.codename1.maven.processors.BuildHintAnnotationProcessor.SOURCE_DIGEST_KEY);
        if (expectedMain == null || recorded == null || recorded.length() == 0) {
            return null;
        }
        // Beside the manifest first, then anywhere on the classpath. Looking only
        // beside it answers "no evidence" for the layout that packages resources
        // apart from classes, and a manifest with no evidence against it is taken
        // at face value -- so a manifest left by a build that no longer runs the
        // processor applied its obsolete hints, and counted as proof the processor
        // ran, while the recompiled class sat in another element.
        com.codename1.maven.annotations.AnnotatedClass cls = classOnClasspath(element, expectedMain);
        if (cls == null) {
            for (String other : classpathElements) {
                cls = classOnClasspath(new File(other), expectedMain);
                if (cls != null) {
                    break;
                }
            }
        }
        if (cls == null) {
            return null;
        }
        try {
            String actual = com.codename1.maven.processors.BuildHintAnnotationProcessor
                    .sourceDigest(cls);
            if (recorded.equals(actual)) {
                return null;
            }
            return "it was generated from a different set of annotations on "
                    + expectedMain + " than the one compiled onto the classpath";
        } catch (com.codename1.maven.annotations.ProcessingException ex) {
            // Unreadable is not evidence of staleness.
            log.debug("cn1: could not fingerprint " + expectedMain, ex);
            return null;
        }
    }

    /** {@code expectedMain} read out of {@code element}, or null when it is not there. */
    private com.codename1.maven.annotations.AnnotatedClass classOnClasspath(File element,
                                                                           String expectedMain) {
        try {
            return readClass(element, expectedMain);
        } catch (IOException | com.codename1.maven.annotations.ProcessingException ex) {
            log.debug("cn1: could not look for " + expectedMain + " in " + element, ex);
            return null;
        }
    }

    /** Reads one compiled class out of a classpath directory or jar. */
    private com.codename1.maven.annotations.AnnotatedClass readClass(File element, String binaryName)
            throws IOException, com.codename1.maven.annotations.ProcessingException {
        String path = binaryName.replace('.', '/') + ".class";
        if (element.isDirectory()) {
            File f = new File(element, path.replace('/', File.separatorChar));
            if (!f.isFile()) {
                return null;
            }
            try (InputStream in = new FileInputStream(f)) {
                return com.codename1.maven.annotations.ClassScanner.readClass(in, f);
            }
        }
        if (element.isFile() && element.getName().endsWith(".jar")) {
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(element)) {
                java.util.zip.ZipEntry entry = zip.getEntry(path);
                if (entry == null) {
                    return null;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    return com.codename1.maven.annotations.ClassScanner.readClass(in, element);
                }
            }
        }
        return null;
    }

    /**
     * The application class carrying a build hint annotation, or null.
     *
     * <p>Read straight out of the class file's annotation table rather than from
     * source, so it sees exactly what the compiler emitted.</p>
     *
     * <p>The MAIN class alone when the project names one. The processor honours
     * no other class, so no other class is evidence that annotations went
     * unprocessed &mdash; and scanning them all meant a stale annotated
     * {@code .class}, left behind by a rename without a clean, failed every build
     * with "no manifest was produced" for a class the developer had already
     * deleted. The processor ignores that orphan; so does this. Only when the
     * project names no main class at all does this fall back to scanning
     * everything, since then there is nothing more specific to ask about.</p>
     */
    private String classCarryingBuildHintAnnotations(List<String> classpathElements,
                                                     String expectedMain) {
        java.util.Collection<String> descriptors = hintAnnotationDescriptors(classpathElements);
        if (expectedMain != null) {
            for (String element : classpathElements) {
                try {
                    if (mainClassCarriesAnnotation(new File(element), expectedMain, descriptors)) {
                        return expectedMain;
                    }
                } catch (IOException | RuntimeException ex) {
                    log.debug("cn1: could not read " + expectedMain + " from " + element, ex);
                }
            }
            // The main class carries none. A LIVE class elsewhere still counts:
            // @Target(TYPE) accepts the placement, so without this the build
            // succeeds having neither applied the hint nor said the annotation is
            // in the wrong place -- which is the silent failure the whole feature
            // removes. It is only stale output that must not count, and that is a
            // question about the source, not about which class it is.
            //
            // Reported as a misplacement, because that is what it is: had the
            // processor run it would have refused the build for this class.
            for (String element : classpathElements) {
                String live = liveAnnotatedClass(new File(element), descriptors);
                if (live != null) {
                    return live;
                }
            }
            return null;
        }
        // A reactor `package` build hands us the dependency module's jar rather
        // than its output directory, which findAnnotatedClasses handles alongside
        // a directory -- that is exactly the shape this check has to work in.
        for (String element : classpathElements) {
            String hit = findAnnotatedClass(new File(element), descriptors);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /**
     * The first annotated class in this element whose source still exists, or null.
     *
     * <p>Stale output is excluded the same way the processor excludes it &mdash;
     * by asking whether the module's configured source roots still declare the
     * class &mdash; so an orphan left by a rename cannot fail the build, while a
     * class the developer actually wrote does.</p>
     */
    private String liveAnnotatedClass(File element, java.util.Collection<String> descriptors) {
        return liveAnnotatedClass(element, descriptors, null);
    }

    /// As above, ignoring `exclude` -- the class the manifest was generated for,
    /// which carrying annotations is the whole point of.
    private String liveAnnotatedClass(File element, java.util.Collection<String> descriptors,
                                      String exclude) {
        List<String> roots;
        String encoding;
        try {
            // The module that PRODUCED this element, not the one running: see
            // Sources. The complete set of its roots, since this list is used to
            // decide that a source is ABSENT, and the charset it compiles with,
            // since the scan decides absence by reading the source.
            roots = sources.rootsFor(element);
            encoding = sources.encodingFor(element);
        } catch (RuntimeException ex) {
            return null;
        }
        if (roots == null || roots.isEmpty()) {
            // Not told where the sources are, so staleness cannot be judged and
            // an orphan would fail the build. Silence is the lesser harm: the
            // processor still refuses this placement whenever it runs.
            return null;
        }
        // Every candidate, not the first: rejecting one stale class must not end
        // the search, or whether a live misplacement is reported depends on the
        // order the directory happened to be listed in.
        for (String hit : findAnnotatedClasses(element, descriptors)) {
            if (exclude != null && exclude.equals(hit)) {
                continue;
            }
            try {
                com.codename1.maven.annotations.AnnotatedClass cls = readClass(element, hit);
                if (cls != null && com.codename1.maven.processors.BuildHintAnnotationProcessor
                        .hasBackingSource(cls, roots, encoding)) {
                    return hit;
                }
            } catch (IOException | com.codename1.maven.annotations.ProcessingException ex) {
                log.debug("cn1: could not read " + hit + " from " + element, ex);
            }
        }
        return null;
    }

    /**
     * Whether {@code -D} already sets this hint, under any spelling.
     *
     * <p>An alias and its target are ONE effective setting -- the builder reads
     * {@code android.captureRecord} and then lets {@code and.captureRecord}
     * override it -- so a command line that names either of them is setting the
     * hint an annotation would otherwise supply. Comparing the keys literally
     * missed that, and the documented rule is that {@code -D} overrides both the
     * annotation and the properties file.</p>
     */
    private boolean overriddenOnTheCommandLine(String key) {
        return overriddenOnTheCommandLine(key, userProperties);
    }

    /// Whether the command line sets `key`, under any spelling of the hint.
    public static boolean overriddenOnTheCommandLine(String key, Properties user) {
        if (user == null) {
            return false;
        }
        String canonical = com.codename1.build.shared.BuildHints.canonicalName(
                com.codename1.build.shared.BuildHints.strip(key));
        for (String name : user.stringPropertyNames()) {
            if (!name.startsWith(com.codename1.build.shared.BuildHints.ARG_PREFIX)) {
                continue;
            }
            String other = com.codename1.build.shared.BuildHints.canonicalName(
                    com.codename1.build.shared.BuildHints.strip(name));
            if (canonical.equals(other)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the named class, read from this classpath element, is annotated. */
    private boolean mainClassCarriesAnnotation(File element, String binaryName,
                                               java.util.Collection<String> descriptors)
            throws IOException {
        String path = binaryName.replace('.', '/') + ".class";
        if (element.isDirectory()) {
            File f = new File(element, path.replace('/', File.separatorChar));
            if (!f.isFile()) {
                return false;
            }
            try (InputStream in = new FileInputStream(f)) {
                return carriesBuildHintAnnotation(in, descriptors);
            }
        }
        if (element.isFile() && element.getName().endsWith(".jar")) {
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(element)) {
                java.util.zip.ZipEntry entry = zip.getEntry(path);
                if (entry == null) {
                    return false;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    return carriesBuildHintAnnotation(in, descriptors);
                }
            }
        }
        return false;
    }


    private boolean carriesBuildHintAnnotation(InputStream in,
                                               java.util.Collection<String> descriptors)
            throws IOException {
        final boolean[] seen = {false};
        new org.objectweb.asm.ClassReader(in).accept(
                new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                    @Override
                    public org.objectweb.asm.AnnotationVisitor visitAnnotation(
                            String desc, boolean visible) {
                        if (descriptors.contains(desc)) {
                            seen[0] = true;
                        }
                        return null;
                    }
                },
                org.objectweb.asm.ClassReader.SKIP_CODE
                        | org.objectweb.asm.ClassReader.SKIP_DEBUG
                        | org.objectweb.asm.ClassReader.SKIP_FRAMES);
        return seen[0];
    }

    private String findAnnotatedClass(File dir, java.util.Collection<String> descriptors) {
        List<String> all = findAnnotatedClasses(dir, descriptors);
        return all.isEmpty() ? null : all.get(0);
    }

    /**
     * Every annotated class under this element, by BINARY name.
     *
     * <p>The binary name, not the file's own: returning {@code Wrong} for
     * {@code com/example/Wrong.class} made the message name a class that does not
     * exist, and made re-reading it by name fail, so the guard saw nothing.</p>
     *
     * <p>All of them, not the first: an incremental output directory can hold a
     * stale annotated class and a live one at once, and stopping at whichever
     * {@code File.listFiles} returned first made the answer depend on directory
     * order.</p>
     */
    private List<String> findAnnotatedClasses(File element, java.util.Collection<String> descriptors) {
        List<String> out = new ArrayList<String>();
        if (element.isDirectory()) {
            collectAnnotatedClasses(element, element, descriptors, out);
        } else if (element.isFile() && element.getName().endsWith(".jar")) {
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(element)) {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                        continue;
                    }
                    try (InputStream in = zip.getInputStream(entry)) {
                        if (carriesBuildHintAnnotation(in, descriptors)) {
                            out.add(entry.getName()
                                    .substring(0, entry.getName().length() - ".class".length())
                                    .replace('/', '.'));
                        }
                    }
                }
            } catch (IOException | RuntimeException ex) {
                log.debug("cn1: could not scan " + element + ": " + ex.getMessage());
            }
        }
        return out;
    }

    private void collectAnnotatedClasses(File root, File dir,
                                         java.util.Collection<String> descriptors,
                                         List<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                collectAnnotatedClasses(root, f, descriptors, out);
                continue;
            }
            if (!f.getName().endsWith(".class")) {
                continue;
            }
            try (InputStream in = new FileInputStream(f)) {
                if (carriesBuildHintAnnotation(in, descriptors)) {
                    String rel = f.getAbsolutePath()
                            .substring(root.getAbsolutePath().length())
                            .replace(File.separatorChar, '/');
                    while (rel.startsWith("/")) {
                        rel = rel.substring(1);
                    }
                    out.add(rel.substring(0, rel.length() - ".class".length()).replace('/', '.'));
                }
            } catch (IOException | RuntimeException ex) {
                log.debug("cn1: could not scan " + f + ": " + ex.getMessage());
            }
        }
    }

    /// The build hint annotation types, read off the classpath they live on.
    ///
    /// The package is enumerated rather than listed: a generated table naming
    /// each annotation was a second statement of which ones exist, and it went
    /// stale the moment one was added without regenerating it.
    private java.util.Collection<String> hintAnnotationDescriptors(
            List<String> classpathElements) {
        try {
            return com.codename1.build.shared.BuildHintAnnotationReader
                    .bindingsFromClasspath(classpathElements).descriptors();
        } catch (IOException ex) {
            log.debug("cn1: could not read the build hint annotations", ex);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Reads the emitted hints out of a classpath element, which is either the
     * module's output directory or a jar.
     *
     * @return the properties, or null when this element carries none
     */
    private Properties readAnnotationHints(File element) {
        if (element == null || !element.exists()) {
            return null;
        }
        try {
            if (element.isDirectory()) {
                File f = new File(element, ANNOTATION_HINTS_RESOURCE);
                if (!f.isFile()) {
                    return null;
                }
                try (FileInputStream in = new FileInputStream(f)) {
                    Properties p = new Properties();
                    p.load(in);
                    return p;
                }
            }
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(element)) {
                java.util.zip.ZipEntry entry = zip.getEntry(ANNOTATION_HINTS_RESOURCE);
                if (entry == null) {
                    return null;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    Properties p = new Properties();
                    p.load(in);
                    return p;
                }
            }
        } catch (IOException ex) {
            log.warn("cn1: could not read build hints from " + element + ": "
                    + ex.getMessage());
            return null;
        }
    }
}
