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
package com.codename1.hardening;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one public entry point of the hardening engine. Given the merged application
 * jar and a resolved config, it produces a hardened jar plus a cross-platform
 * ProGuard mapping.
 *
 * <p>The pipeline is: demux the fat jar to a class-only jar (non-class entries
 * preserved byte-for-byte); assemble keep rules; rename with ProGuard using the
 * prefixed dictionary (skipped on Android, where R8 remains the sole renamer);
 * encrypt strings; guard against ParparVM mangle collisions; verify every class;
 * rebuild the output jar; and finalize the mapping.
 */
public final class HardeningEngine {

    public static final String ENGINE_VERSION = "1.0.0";
    public static final String PROGUARD_VERSION = "7.3.2";
    /** Highest Java feature version whose class files ProGuard 7.3.2 can read. */
    public static final int PROGUARD_MAX_JDK = 20;

    private HardeningEngine() {
    }

    /**
     * Whether ProGuard can run on the current JVM. 7.3.2 cannot read class files newer than
     * JDK 20 (it fails on the JDK's own module classes), so renaming must run on JDK 8-20 --
     * the cloud daemon forks the engine on JDK 17. String encryption and control flow have no
     * such limit.
     */
    public static boolean proguardCanRunHere() {
        return currentJdkFeature() <= PROGUARD_MAX_JDK;
    }

    static int currentJdkFeature() {
        String v = System.getProperty("java.specification.version", "1.8");
        if (v.startsWith("1.")) {
            v = v.substring(2);
        }
        int dot = v.indexOf('.');
        if (dot >= 0) {
            v = v.substring(0, dot);
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    public static String engineVersion() {
        return ENGINE_VERSION;
    }

    public static HardeningResult harden(HardeningRequest req) throws HardeningException {
        HardeningConfig cfg = req.getConfig();
        require(req.getInputJar() != null && req.getInputJar().isFile(), "input jar is missing");
        require(req.getOutputJar() != null, "output jar path is missing");
        require(cfg != null, "config is missing");

        if (cfg.getProfile() == HardeningProfile.OFF) {
            return HardeningResult.skipped(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, req.getInputJar());
        }
        if (!cfg.isPlatformEnabled()) {
            return HardeningResult.skipped(HardeningResult.Outcome.SKIPPED_PLATFORM_DISABLED, req.getInputJar());
        }
        // A non-off level whose transforms are all individually disabled (e.g. harden.rename=false +
        // harden.strings=off + no control flow) does nothing -- don't rebuild the jar and stamp it
        // "hardened" with an empty transform set. Treat it as not requested.
        if (!willApplyAnyTransform(cfg)) {
            return HardeningResult.skipped(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, req.getInputJar());
        }

        File workDir = req.getWorkDir();
        if (workDir == null) {
            workDir = req.getOutputJar().getAbsoluteFile().getParentFile();
        }
        workDir.mkdirs();

        try {
            return run(req, cfg, workDir);
        } catch (IOException e) {
            throw new HardeningException("Hardening failed: " + e.getMessage(), e);
        }
    }

    private static HardeningResult run(HardeningRequest req, HardeningConfig cfg, File workDir)
            throws HardeningException, IOException {
        File classesJar = new File(workDir, "classes-in.jar");
        JarDemuxer.NonClassEntries nonClass = JarDemuxer.split(req.getInputJar(), classesJar);
        Map<String, byte[]> inClasses = JarDemuxer.readClasses(classesJar);
        int classesIn = inClasses.size();

        List<String> keepRules = new ArrayList<String>();
        keepRules.addAll(BuiltinKeepRules.rules(req.getMainClass()));
        InputJarKeepScanner scanner = new InputJarKeepScanner();
        scanner.scan(inClasses);
        keepRules.addAll(scanner.keepRules());
        // Keep classes named by META-INF/services descriptors: those files are copied verbatim, so
        // ServiceLoader would fail if the service interface or a provider class were renamed.
        keepRules.addAll(serviceDescriptorKeeps(nonClass));
        keepRules.addAll(cfg.getExtraKeepRules());

        // Export the derived keep rules for a downstream renamer the engine doesn't drive itself.
        // On Android R8 is the sole renamer (isRenameEnabled()==false), so without this the classes
        // the scanner found reflectively (Class.forName targets, service providers, name-bound
        // property objects, the app's own harden.keep) would be invisible to R8 and get renamed.
        if (req.getR8KeepFile() != null) {
            StringBuilder r8 = new StringBuilder();
            for (String rule : keepRules) {
                r8.append(rule).append('\n');
            }
            writeText(req.getR8KeepFile(), r8.toString());
        }

        Map<String, byte[]> renamed;
        int renamedCount = 0;
        File mappingFile = req.getMappingFile();
        File hierarchyJar;

        if (cfg.isRenameEnabled()) {
            if (!proguardCanRunHere()) {
                throw new HardeningException("App hardening's renamer (ProGuard " + PROGUARD_VERSION
                        + ") must run on JDK 8-" + PROGUARD_MAX_JDK + ", but this JVM is JDK "
                        + currentJdkFeature() + ". The Codename One build server runs the engine on "
                        + "JDK 17; for a local hardened build, run it on JDK 8-" + PROGUARD_MAX_JDK + ".");
            }
            File dict = new File(workDir, "cn1-dict.txt");
            // Seed the dictionary so harden.seed / the build key actually changes the mapping.
            Cn1NameFactory.writeDictionary(dict,
                    Cn1NameFactory.dictionarySizeFor(classesIn, maxMemberNamingScope(inClasses)),
                    deriveSeed(cfg, req.getBuildKey()));
            File renamedJar = new File(workDir, "renamed.jar");
            ProGuardRunner.rename(classesJar, renamedJar, mappingFile,
                    req.getLibraryJars(), keepRules, dict, workDir, cfg.getPlatform());
            renamed = JarDemuxer.readClasses(renamedJar);
            renamedCount = countRenamed(inClasses.keySet(), renamed.keySet());
            hierarchyJar = renamedJar;
        } else {
            renamed = new LinkedHashMap<String, byte[]>(inClasses);
            hierarchyJar = classesJar;
            if (mappingFile != null) {
                writeText(mappingFile, "");
            }
        }

        // Classloader over the (renamed) app classes plus the library jars, so stack-map frame
        // computation resolves the class hierarchy without loading types through the engine's own
        // classloader (see FrameClassWriter).
        ClassLoader hierarchy = buildHierarchyLoader(hierarchyJar, req.getLibraryJars());

        int seed = deriveSeed(cfg, req.getBuildKey());
        int encryptedStrings = 0;
        int concatLiterals = 0;
        int legacyInterfaceConstants = 0;
        int oversizedLiterals = 0;
        int condyLiterals = 0;
        int indyLiterals = 0;
        int shortLiterals = 0;
        int hierarchyIncompleteSkips = 0;
        int externallyReadConstants = 0;
        int clinitFullLiterals = 0;
        int annotationLiterals = 0;
        int jarExcludedLiterals = 0;
        int libraryExcludedLiterals = 0;
        int sourcePreservedConstants = 0;
        boolean stringsApplied = cfg.isAnyStringEncryption() && stringEncryptionSafeFor(cfg.getPlatform());
        if (stringsApplied) {
            // In "constants" mode, first collect the values declared as static-final String
            // constants across the whole jar, so we encrypt exactly those (and javac's inlined
            // copies) and nothing incidental.
            java.util.Set<String> constantValues = null;
            if (!cfg.isEncryptAllStrings()) {
                constantValues = new java.util.HashSet<String>();
                for (byte[] cls : renamed.values()) {
                    StringEncryptTransform.collectConstantValues(cls, constantValues);
                }
            }
            // On a ParparVM-C target a compile-time literal is a never-interned constant-pool object,
            // while an encrypted app copy is intern()ed, so encrypting an app value that ALSO appears as
            // a literal in an UNHARDENED library class (core, java-runtime, dependencies -- never in
            // 'renamed') would break a valid literal == against the library copy that held before
            // hardening. Collect those library literals and exclude them from encryption. On a real-JVM
            // or Android target every compile-time literal is interned to the same pool intern() uses, so
            // the constraint does not apply and the scan is skipped (full coverage).
            java.util.Set<String> libraryLiterals = null;
            if (translatesThroughParparVMC(cfg.getPlatform()) && req.getLibraryJars() != null) {
                libraryLiterals = new java.util.HashSet<String>();
                for (File lib : req.getLibraryJars()) {
                    collectJarLiterals(lib, libraryLiterals);
                }
            }
            final java.util.Set<String> libLiterals =
                    libraryLiterals != null && !libraryLiterals.isEmpty() ? libraryLiterals : null;
            java.util.Set<String> libraryExcluded = new java.util.HashSet<String>();
            // On a target that compiles the .java/.kt source bundled in the app jar against the
            // transformed classes (Android places those sources in src/main/java and the hardened classes
            // in libs/userClasses.jar), stripping a static-final String ConstantValue would break a
            // case-label/annotation/const-initializer reference to it in that source. Collect the
            // identifiers such sources reference so those constants keep their ConstantValue.
            java.util.Set<String> srcNames = null;
            if (compilesCarriedSource(cfg.getPlatform())) {
                java.util.Set<String> ids = new java.util.HashSet<String>();
                for (Map.Entry<String, byte[]> e : nonClass.asMap().entrySet()) {
                    String name = e.getKey().toLowerCase();
                    if (name.endsWith(".java") || name.endsWith(".kt")) {
                        collectSourceIdentifiers(new String(e.getValue(),
                                java.nio.charset.Charset.forName("UTF-8")), ids);
                    }
                }
                if (!ids.isEmpty()) {
                    srcNames = ids;
                }
            }
            final java.util.Set<String> sourceReferencedNames = srcNames;
            // Fields read by a GETSTATIC anywhere in the jar (a non-inlined constant read from generated
            // bytecode): their ConstantValue must not be moved to <clinit>, which would change reentrant
            // initialization ordering. Scanned once over the renamed classes so owner/name match the
            // renamed field names the transform checks.
            final java.util.Set<String> externallyReadStaticFields = new java.util.HashSet<String>();
            for (byte[] cls : renamed.values()) {
                StringEncryptTransform.collectGetStaticStringReads(cls, externallyReadStaticFields);
            }
            // Pass 1 (from a snapshot of the input bytes): transform every class, tally the counts, and
            // collect the values any class could NOT encrypt (a method too full for the decode call, or a
            // class whose pool cannot fit the decoder). A value encrypted+interned in one class but left
            // plaintext in another would fail a valid literal == on ParparVM's deduplicated pool, so
            // those values must be excluded jar-wide -- encrypted everywhere or nowhere.
            java.util.Map<String, byte[]> original = new java.util.HashMap<String, byte[]>(renamed);
            java.util.Set<String> jarExcluded = new java.util.HashSet<String>();
            for (Map.Entry<String, byte[]> e : renamed.entrySet()) {
                StringEncryptTransform t = new StringEncryptTransform(
                        cfg.isEncryptAllStrings(), seed, hierarchy, constantValues, null);
                t.setLibraryLiterals(libLiterals);
                t.setSourceReferencedNames(sourceReferencedNames);
                t.setExternallyReadStaticFields(externallyReadStaticFields);
                e.setValue(t.transform(e.getValue()));
                jarExcluded.addAll(t.getNewlyExcluded());
                libraryExcluded.addAll(t.getLibraryExcludedValues());
                sourcePreservedConstants += t.getSourcePreservedConstantCount();
                externallyReadConstants += t.getExternallyReadConstantCount();
                encryptedStrings += t.getEncryptedCount();
                concatLiterals += t.getConcatLiteralCount();
                legacyInterfaceConstants += t.getLegacyInterfaceConstantCount();
                oversizedLiterals += t.getOversizedLiteralCount();
                condyLiterals += t.getCondyLiteralCount();
                indyLiterals += t.getIndyLiteralCount();
                shortLiterals += t.getShortLiteralCount();
                hierarchyIncompleteSkips += t.isHierarchyIncompleteSkipped() ? 1 : 0;
                clinitFullLiterals += t.getClinitFullLiteralCount();
                annotationLiterals += t.getAnnotationLiteralCount();
            }
            // Pass 2 only when pass 1 found values it could not consistently encrypt: re-transform every
            // class from the original bytes with those values excluded (so a value is plaintext in all
            // classes or none), replacing pass 1's outputs and re-tallying. In the common case the
            // exclusion set is empty and pass 1's result stands -- one transform per class.
            if (!jarExcluded.isEmpty()) {
                encryptedStrings = 0;
                concatLiterals = 0;
                legacyInterfaceConstants = 0;
                oversizedLiterals = 0;
                condyLiterals = 0;
                indyLiterals = 0;
                shortLiterals = 0;
                hierarchyIncompleteSkips = 0;
                externallyReadConstants = 0;
                clinitFullLiterals = 0;
                annotationLiterals = 0;
                sourcePreservedConstants = 0;
                libraryExcluded.clear();
                for (Map.Entry<String, byte[]> e : renamed.entrySet()) {
                    StringEncryptTransform t = new StringEncryptTransform(
                            cfg.isEncryptAllStrings(), seed, hierarchy, constantValues, jarExcluded);
                    t.setLibraryLiterals(libLiterals);
                    t.setSourceReferencedNames(sourceReferencedNames);
                    t.setExternallyReadStaticFields(externallyReadStaticFields);
                    e.setValue(t.transform(original.get(e.getKey())));
                    libraryExcluded.addAll(t.getLibraryExcludedValues());
                    sourcePreservedConstants += t.getSourcePreservedConstantCount();
                    externallyReadConstants += t.getExternallyReadConstantCount();
                    encryptedStrings += t.getEncryptedCount();
                    concatLiterals += t.getConcatLiteralCount();
                    legacyInterfaceConstants += t.getLegacyInterfaceConstantCount();
                    oversizedLiterals += t.getOversizedLiteralCount();
                    condyLiterals += t.getCondyLiteralCount();
                    indyLiterals += t.getIndyLiteralCount();
                    shortLiterals += t.getShortLiteralCount();
                    hierarchyIncompleteSkips += t.isHierarchyIncompleteSkipped() ? 1 : 0;
                    clinitFullLiterals += t.getClinitFullLiteralCount();
                    annotationLiterals += t.getAnnotationLiteralCount();
                }
            }
            jarExcludedLiterals = jarExcluded.size();
            libraryExcludedLiterals = libraryExcluded.size();
        }

        int guardedMethods = 0;
        int oversizedGuardMethods = 0;
        boolean controlFlowApplied = cfg.isControlFlow() && controlFlowSafeFor(cfg.getPlatform());
        if (controlFlowApplied) {
            for (Map.Entry<String, byte[]> e : renamed.entrySet()) {
                ControlFlowTransform t = new ControlFlowTransform(hierarchy, cfg.getControlFlowIntensity());
                byte[] out = t.transform(e.getValue());
                if (out != e.getValue()) {
                    e.setValue(out);
                }
                oversizedGuardMethods += t.getOversizedMethods();
                guardedMethods += t.getGuardedMethods();
            }
        }

        // Even when the config asked for a transform, the input may contain nothing eligible (e.g.
        // rename off, and no static-final string longer than two characters to encrypt): every
        // counter stays zero and no transform actually ran. Stamping cn1.hardened=true for a
        // byte-unchanged app would be dishonest, so report SKIPPED and let the caller keep the input.
        // Android still counts as hardened here because R8 renames downstream (isRenameRequested).
        boolean anyApplied = cfg.isRenameEnabled()
                || cfg.isRenameRequested()
                || (stringsApplied && encryptedStrings > 0)
                || (controlFlowApplied && guardedMethods > 0);
        if (!anyApplied) {
            return HardeningResult.skipped(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, req.getInputJar());
        }

        // The a.b_c / a.b.c -> a_b_c collision only exists in the ParparVM C symbol mangle. On
        // Android (R8/DEX -- and the engine does not even rename there) and JavaSE (plain JVM),
        // '.' vs '_' stay distinct, so two legal classes must not abort the build.
        if (translatesThroughParparVMC(cfg.getPlatform())) {
            MangleCollisionCheck.check(renamed.keySet());
        }
        OutputVerifier.verify(renamed, hierarchy);

        // Idempotence marker: a nested builder delegation must not harden twice.
        nonClass.asMap().put("META-INF/CN1-HARDENED",
                (ENGINE_VERSION + " " + cfg.getProfile().name().toLowerCase())
                        .getBytes(java.nio.charset.Charset.forName("UTF-8")));

        JarDemuxer.rebuild(req.getOutputJar(), renamed, nonClass);

        // Only the engine's own rename produces a mapping worth an id. On Android the engine does
        // not rename (R8 is the sole renamer and produces the real per-build mapping later), so the
        // engine mapping is empty -- hashing it would stamp a meaningless constant id. Leave it empty.
        String mappingId = "";
        if (mappingFile != null && cfg.isRenameEnabled()) {
            // Record the real source filename for classes whose SourceFile the engine just stripped and
            // whose name cannot reconstruct it (Kotlin Screen.kt, a package-private class in Main.java),
            // captured from the INPUT classes before the strip. Done before finalize so the id covers it.
            MappingWriter.injectSourceFiles(mappingFile, collectSourceFiles(inClasses));
            mappingId = MappingWriter.finalizeMapping(mappingFile, ENGINE_VERSION, PROGUARD_VERSION,
                    cfg.getPlatform(), req.getBuildKey());
        }

        HardeningResult result = HardeningResult.hardened(req.getOutputJar(), mappingFile);
        result.setClassesIn(classesIn);
        result.setClassesOut(renamed.size());
        result.setRenamedClasses(renamedCount);
        result.setEncryptedStrings(encryptedStrings);
        result.setMappingId(mappingId);
        // Report only what actually ran, so a "half-hardened" build can never claim a
        // transform it skipped. This is what the downstream verifier checks against.
        if (cfg.isRenameEnabled()) {
            result.getTransformsApplied().add("rename");
        } else if (cfg.isRenameRequested()) {
            // Android: the engine doesn't rename, R8 does. Still a rename, still hardened.
            result.getTransformsApplied().add("rename:r8");
        }
        if (stringsApplied && encryptedStrings > 0) {
            result.getTransformsApplied().add(cfg.isEncryptAllStrings() ? "strings:all" : "strings:constants");
        }
        if (controlFlowApplied && guardedMethods > 0) {
            result.getTransformsApplied().add(cfg.getControlFlowIntensity() >= 2
                    ? "controlFlow:intense" : "controlFlow");
        }
        if (cfg.isControlFlow() && !controlFlowApplied) {
            result.getWarnings().add("control-flow obfuscation is not applied on platform '"
                    + cfg.getPlatform() + "' (unsafe for the ParparVM optimizer); skipped");
        }
        if (cfg.isAnyStringEncryption() && !stringsApplied) {
            result.getWarnings().add("string encryption is not applied on platform '"
                    + cfg.getPlatform() + "' (would break the JavaScript native bridge); skipped");
        }
        if (stringsApplied && concatLiterals > 0) {
            // javac from JDK 9 compiles string concatenation to an invokedynamic whose literal
            // fragments live in the StringConcatFactory recipe, not in an LDC or a ConstantValue, so
            // the string channels cannot reach them. Report it rather than let a build believe it is
            // fully string-encrypted; -XDstringConcat=inline (or an older -target) emits StringBuilder
            // the engine does encrypt.
            result.getWarnings().add(concatLiterals + " string-concatenation literal group(s) compiled "
                    + "to invokedynamic (JDK 9+ javac) were not encrypted; compile with "
                    + "-XDstringConcat=inline or an older -target to encrypt concatenation literals");
        }
        if (stringsApplied && legacyInterfaceConstants > 0) {
            // A pre-Java-8 interface cannot host a <clinit>/decoder, so its own static-final String
            // constants stay plaintext in that class file. Reads elsewhere were inlined and are
            // encrypted; report the declaring-interface leak rather than ship it unremarked.
            result.getWarnings().add(legacyInterfaceConstants + " static-final String constant(s) on "
                    + "pre-Java-8 interface(s) were not encrypted (such interfaces cannot host the "
                    + "decoder); recompile the interface at -target 8+ to encrypt its constant pool");
        }
        if (stringsApplied && condyLiterals > 0) {
            // Java 11+ can carry plaintext through an LDC constant-dynamic whose bootstrap arguments
            // hold the string; it is resolved at link time, so it cannot be rewritten to a decode call.
            result.getWarnings().add(condyLiterals + " constant-dynamic (LDC ConstantDynamic) site(s) "
                    + "carrying string bootstrap arguments were not encrypted; such constants are "
                    + "resolved at link time and remain in plaintext");
        }
        if (stringsApplied && indyLiterals > 0) {
            // A custom invokedynamic (not a StringConcatFactory concat) can carry a plaintext string in
            // its bootstrap arguments, resolved at link time, which no LDC/ConstantValue pass reaches.
            result.getWarnings().add(indyLiterals + " invokedynamic site(s) carrying string bootstrap "
                    + "arguments (a non-concat bootstrap emitted by a bytecode generator) were not "
                    + "encrypted; such constants are resolved at link time and remain in plaintext");
        }
        if (stringsApplied && cfg.isEncryptAllStrings() && shortLiterals > 0) {
            // One- and two-character literals are left plaintext (the decoder overhead dwarfs them, and a
            // two-char value is trivially brute-forced even encrypted). Disclose so strings:all is honest.
            result.getWarnings().add(shortLiterals + " distinct one- or two-character string literal(s) "
                    + "were left in plaintext (too short to be worth encrypting); a short value is "
                    + "trivially recovered even when encrypted, so this is a disclosure note");
        }
        if (hierarchyIncompleteSkips > 0) {
            // A class whose frame merge could not be resolved past a supertype absent from the supplied
            // jars is shipped UNHARDENED (original frames) rather than risk an Object-widened frame that
            // fails on-device verification. Disclose it so the coverage claim is honest.
            result.getWarnings().add(hierarchyIncompleteSkips + " class(es) were left unhardened because a "
                    + "supertype needed to compute their stack-map frames was absent from the supplied "
                    + "library jars; supplying that platform's jars lets them be hardened");
        }
        if (stringsApplied && externallyReadConstants > 0) {
            // A constant read by a GETSTATIC (non-inlined, from generated bytecode) keeps its ConstantValue
            // -- moving it into <clinit> would change reentrant-initialization ordering -- so it stays
            // plaintext. Disclose it rather than let strings:all imply it was encrypted.
            result.getWarnings().add(externallyReadConstants + " static-final String constant(s) are read by "
                    + "a non-inlined GETSTATIC and were left in plaintext to preserve class-initialization "
                    + "ordering; move such a secret out of a constant to hide it");
        }
        if (stringsApplied && oversizedLiterals > 0) {
            // A literal longer than ~21,845 chars can widen to a 3-byte-per-char constant whose
            // ciphertext overflows the 65535-byte constant pool, so it is left plaintext. Report it
            // rather than let an strings:all build claim it encrypted everything.
            result.getWarnings().add(oversizedLiterals + " string literal(s) were too large to encrypt "
                    + "(their ciphertext would overflow the 65535-byte constant pool) and remain in "
                    + "plaintext; move a large embedded secret/blob out of a string constant to hide it");
        }
        if (stringsApplied && clinitFullLiterals > 0) {
            result.getWarnings().add(clinitFullLiterals + " static-final String constant(s) were left in "
                    + "plaintext because the class is near a JVM limit (its static initializer's size or "
                    + "the constant pool) and could not hold the decode step; those field values are "
                    + "dead once javac inlines their reads, so this is a disclosure note");
        }
        if (stringsApplied && jarExcludedLiterals > 0) {
            // Values that at least one class could not encrypt (a method already near the 65535-byte
            // limit, or a class whose constant pool cannot fit the decoder) are left plaintext in EVERY
            // class, so a decoded+interned copy never compares != to a plaintext copy on ParparVM.
            result.getWarnings().add(jarExcludedLiterals + " distinct string value(s) were left in "
                    + "plaintext in every class because at least one class could not encrypt them (a "
                    + "method or constant pool near the JVM limit); those literals stay readable");
        }
        if (stringsApplied && sourcePreservedConstants > 0) {
            // A static-final String referenced by a bundled .java/.kt source in a constant-expression
            // context keeps its ConstantValue so that source still compiles; disclose the plaintext.
            result.getWarnings().add(sourcePreservedConstants + " static-final String constant(s) kept "
                    + "their plaintext ConstantValue because a bundled .java/.kt source may reference them "
                    + "as a compile-time constant (a case label or annotation value), which requires the "
                    + "attribute to compile; their inlined reads are still encrypted");
        }
        if (stringsApplied && libraryExcludedLiterals > 0) {
            // Values that also appear as a literal in an unhardened library class are left plaintext so a
            // literal == against the library's (never interned) constant-pool copy still holds on ParparVM.
            result.getWarnings().add(libraryExcludedLiterals + " distinct string value(s) were left in "
                    + "plaintext because an unhardened framework/dependency class also holds them as a "
                    + "literal (encrypting only the app copy would break a valid reference-equality check "
                    + "against the library copy on the ParparVM native targets); those literals stay readable");
        }
        if (stringsApplied && annotationLiterals > 0) {
            // Annotation element values live in the annotation metadata, not an LDC or a ConstantValue,
            // so no encryption channel reaches them. CN1 has no reflection to read them back, so this is
            // a disclosure note; don't put a secret in an annotation and expect it hidden.
            result.getWarnings().add(annotationLiterals + " string(s) in annotation values/defaults were "
                    + "not encrypted (they live in annotation metadata, not code); do not place a secret "
                    + "in an annotation");
        }
        if (controlFlowApplied && oversizedGuardMethods > 0) {
            result.getWarnings().add(oversizedGuardMethods + " method(s) were left with plain control "
                    + "flow because their class is already near a JVM limit (a method near the "
                    + "65535-byte limit, or a constant pool near the 65535-entry limit) and adding the "
                    + "guard would overflow it");
        }
        if (req.getReportFile() != null) {
            writeReport(req.getReportFile(), cfg, result);
        }
        return result;
    }

    /**
     * String encryption is disabled on the JavaScript port for now: the ParparVM JS backend's
     * minifier treats certain string literals as live references into the CN1 native bridge, and
     * encrypting one would break the bridge. Every other port is safe (the decoder is ordinary
     * translated/compiled code).
     */
    /** True when at least one transform will actually run for this config and platform. */
    static boolean willApplyAnyTransform(HardeningConfig cfg) {
        // An off profile skips everything: harden() returns SKIPPED for OFF (before any transform),
        // even when a stale individual override such as harden.rename=true is still present. Match
        // that here so a non-entitled build that explicitly set harden.level=off is skipped rather
        // than rejected as not-entitled just because an override was left behind.
        if (cfg.getProfile() == HardeningProfile.OFF) {
            return false;
        }
        // A per-platform opt-out means nothing runs for this target -- so a non-entitled build with
        // harden.<platform>.enabled=false is skipped, not rejected as not-entitled.
        if (!cfg.isPlatformEnabled()) {
            return false;
        }
        // renameRequested (not renameEnabled): on Android the engine does not rename, but R8 does,
        // so a rename-only Android build is still a hardened build and must not be skipped.
        if (cfg.isRenameRequested()) {
            return true;
        }
        if (cfg.isAnyStringEncryption() && stringEncryptionSafeFor(cfg.getPlatform())) {
            return true;
        }
        return cfg.isControlFlow() && controlFlowSafeFor(cfg.getPlatform());
    }

    static boolean stringEncryptionSafeFor(String platform) {
        return !"javascript".equals(platform);
    }

    /**
     * Control-flow obfuscation runs only on the JVM-bytecode ports (Android, desktop). The
     * ParparVM native ports (ios/mac/watch/tv/win/linux) translate to C, where the opaque
     * predicate fights the optimizer/devirtualizer and the arithmetic reducer, and the JavaScript
     * port inflates the bundle and confuses the suspension analysis; those are left untouched.
     */
    static boolean controlFlowSafeFor(String platform) {
        return "and".equals(platform) || "android".equals(platform)
                || "javase".equals(platform) || "desktop".equals(platform);
    }

    /**
     * The ports whose classes are translated to C by ParparVM, where the class/package mangle
     * ({@code . / $} all collapse to {@code _}) can make two legal Java names share one C symbol.
     * The collision guard is meaningful only for these; Android (DEX) and JavaSE (JVM) keep the
     * names distinct, and JavaScript uses a different mangling entirely.
     */
    static boolean translatesThroughParparVMC(String platform) {
        return "ios".equals(platform) || "mac".equals(platform) || "watch".equals(platform)
                || "tv".equals(platform) || "win".equals(platform) || "linux".equals(platform);
    }

    /**
     * True for a target that javac/kotlinc-compiles the {@code .java}/{@code .kt} sources bundled in the
     * app jar against the transformed classes. Android does (the sources land in {@code src/main/java}
     * and the hardened classes in {@code libs/userClasses.jar}); iOS routes carried source into the
     * resource tree and never compiles it, win/linux compile only the ParparVM translator's generated
     * {@code .java}, and JavaSE runs the bytecode directly. On such a target a stripped
     * {@code ConstantValue} would break a constant-expression reference from the carried source.
     */
    static boolean compilesCarriedSource(String platform) {
        return "and".equals(platform) || "android".equals(platform);
    }

    /**
     * Maps the original (dotted) class name to its {@code SourceFile} attribute, for the classes worth
     * recording in the mapping: those whose filename differs from the synthesized {@code <SimpleName>.java}
     * default, i.e. Kotlin sources and package-private classes declared in a differently named file. Read
     * from the INPUT classes, before the engine strips the attribute.
     */
    private static java.util.Map<String, String> collectSourceFiles(java.util.Map<String, byte[]> classes) {
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        for (java.util.Map.Entry<String, byte[]> e : classes.entrySet()) {
            final String[] sf = new String[1];
            new org.objectweb.asm.ClassReader(e.getValue()).accept(
                    new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        @Override
                        public void visitSource(String source, String debug) {
                            sf[0] = source;
                        }
                    // NOT SKIP_DEBUG -- that flag skips the SourceFile attribute, which is exactly what
                    // visitSource reports and what we are here to capture.
                    }, org.objectweb.asm.ClassReader.SKIP_CODE | org.objectweb.asm.ClassReader.SKIP_FRAMES);
            if (sf[0] != null && sf[0].length() > 0 && !sf[0].equals(defaultSourceFile(e.getKey()))) {
                out.put(e.getKey().replace('/', '.'), sf[0]);
            }
        }
        return out;
    }

    /**
     * The largest member NAMING scope in {@code classes}, which the obfuscation dictionary must exceed so
     * ProGuard never exhausts it and falls back to short names. Two scopes matter, both beyond the class
     * count:
     * <ul>
     *   <li>Fields: a class cannot declare two fields with the same name, so a class's fields all need
     *       distinct names -- the per-class field count.</li>
     *   <li>Methods: ProGuard cannot give two same-descriptor methods in one inheritance hierarchy the
     *       same obfuscated name without creating an accidental override, so same-descriptor methods
     *       accumulate ACROSS a hierarchy, not just within one class. The exact per-hierarchy count needs
     *       a full hierarchy walk; the count of methods sharing a descriptor across the WHOLE jar is a
     *       safe (and, for a generated deep hierarchy, tight) upper bound.</li>
     * </ul>
     */
    static int maxMemberNamingScope(java.util.Map<String, byte[]> classes) {
        final int[] maxFields = new int[1];
        final java.util.Map<String, Integer> methodsByDescriptor = new java.util.HashMap<String, Integer>();
        for (byte[] bytes : classes.values()) {
            final int[] fields = new int[1];
            new org.objectweb.asm.ClassReader(bytes).accept(
                    new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        @Override
                        public org.objectweb.asm.FieldVisitor visitField(int a, String n, String d,
                                String s, Object v) {
                            fields[0]++;
                            return null;
                        }

                        @Override
                        public org.objectweb.asm.MethodVisitor visitMethod(int a, String n, String desc,
                                String s, String[] e) {
                            Integer c = methodsByDescriptor.get(desc);
                            methodsByDescriptor.put(desc, c == null ? 1 : c + 1);
                            return null;
                        }
                    }, org.objectweb.asm.ClassReader.SKIP_CODE | org.objectweb.asm.ClassReader.SKIP_DEBUG
                            | org.objectweb.asm.ClassReader.SKIP_FRAMES);
            if (fields[0] > maxFields[0]) {
                maxFields[0] = fields[0];
            }
        }
        int maxMethodsPerDescriptor = 0;
        for (int c : methodsByDescriptor.values()) {
            if (c > maxMethodsPerDescriptor) {
                maxMethodsPerDescriptor = c;
            }
        }
        return Math.max(maxFields[0], maxMethodsPerDescriptor);
    }

    /** The {@code <SimpleName>.java} a retrace synthesizes from an internal class name (its default). */
    private static String defaultSourceFile(String internalName) {
        String simple = internalName;
        int slash = simple.lastIndexOf('/');
        if (slash >= 0) {
            simple = simple.substring(slash + 1);
        }
        int dollar = simple.indexOf('$');
        if (dollar > 0) {
            simple = simple.substring(0, dollar);
        }
        return simple + ".java";
    }

    /** Adds every Java/Kotlin identifier token in {@code source} to {@code out} (a safe over-set). */
    private static void collectSourceIdentifiers(String source, java.util.Set<String> out) {
        int n = source.length();
        int i = 0;
        while (i < n) {
            char c = source.charAt(i);
            if (Character.isJavaIdentifierStart(c)) {
                int start = i;
                i++;
                while (i < n && Character.isJavaIdentifierPart(source.charAt(i))) {
                    i++;
                }
                out.add(source.substring(start, i));
            } else {
                i++;
            }
        }
    }

    /**
     * Collects every string literal ({@code LDC} operand or {@code static final String}
     * {@code ConstantValue}) in every class of {@code jar} into {@code out}. Used to gather the
     * literals of the unhardened library jars so an app value shared with a framework/dependency class
     * is not encrypted (which would break a literal {@code ==} against the un-interned library copy on
     * ParparVM). A jar that cannot be read is skipped -- a library the engine cannot scan simply
     * contributes no exclusions rather than aborting the build.
     */
    private static void collectJarLiterals(File jar, java.util.Set<String> out) {
        if (jar == null || !jar.isFile()) {
            return;
        }
        try {
            java.io.FileInputStream fi = new java.io.FileInputStream(jar);
            try {
                java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(fi);
                java.util.zip.ZipEntry entry;
                byte[] buf = new byte[8192];
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                        continue;
                    }
                    java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
                    int r;
                    while ((r = zis.read(buf)) >= 0) {
                        bout.write(buf, 0, r);
                    }
                    try {
                        StringEncryptTransform.collectAllLiterals(bout.toByteArray(), out);
                    } catch (RuntimeException ignored) {
                        // A class ASM cannot parse (a newer format than this ASM, a malformed entry)
                        // contributes no exclusions; it must not fail the scan of the rest of the jar.
                    }
                }
            } finally {
                fi.close();
            }
        } catch (IOException ex) {
            // An unreadable library jar simply contributes no exclusions.
        }
    }

    /**
     * A classloader over the (renamed) application classes plus the library jars, for stack-map
     * frame computation. JDK library classes resolve through the parent (bootstrap) loader, so the
     * jmods are intentionally not added -- URLClassLoader can't read them and java.* resolves via
     * the parent anyway. Never initializes classes (FrameClassWriter uses initialize=false).
     */
    private static ClassLoader buildHierarchyLoader(File hierarchyJar, List<File> libraryJars) {
        List<java.net.URL> urls = new ArrayList<java.net.URL>();
        try {
            if (hierarchyJar != null && hierarchyJar.isFile()) {
                urls.add(hierarchyJar.toURI().toURL());
            }
            if (libraryJars != null) {
                for (File lib : libraryJars) {
                    if (lib != null && lib.isFile()) {
                        urls.add(lib.toURI().toURL());
                    }
                }
            }
        } catch (java.net.MalformedURLException e) {
            return HardeningEngine.class.getClassLoader();
        }
        return new java.net.URLClassLoader(urls.toArray(new java.net.URL[urls.size()]),
                HardeningEngine.class.getClassLoader());
    }

    /**
     * Keep rules for every class named by a {@code META-INF/services/*} descriptor -- the service
     * interface (the file name) and each provider class listed inside. The descriptors are carried
     * across verbatim, so renaming any of these would break {@code ServiceLoader}.
     */
    private static List<String> serviceDescriptorKeeps(JarDemuxer.NonClassEntries nonClass) {
        List<String> rules = new ArrayList<String>();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        String prefix = "META-INF/services/";
        for (Map.Entry<String, byte[]> e : nonClass.asMap().entrySet()) {
            String name = e.getKey();
            if (!name.startsWith(prefix) || name.length() <= prefix.length()) {
                continue;
            }
            addServiceKeep(rules, seen, name.substring(prefix.length()));
            String body = new String(e.getValue(), java.nio.charset.Charset.forName("UTF-8"));
            for (String line : body.split("\\r?\\n")) {
                int hash = line.indexOf('#');
                if (hash >= 0) {
                    line = line.substring(0, hash);
                }
                addServiceKeep(rules, seen, line.trim());
            }
        }
        return rules;
    }

    private static void addServiceKeep(List<String> rules, java.util.Set<String> seen, String className) {
        String c = className.trim();
        if (c.length() == 0 || !isPlausibleClassName(c) || !seen.add(c)) {
            return;
        }
        rules.add("-keep class " + c + " { *; }");
    }

    private static boolean isPlausibleClassName(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.' && c != '$') {
                return false;
            }
        }
        return true;
    }

    private static int deriveSeed(HardeningConfig cfg, String buildKey) {
        String basis = cfg.getSeed() != null ? cfg.getSeed()
                : (buildKey == null || buildKey.isEmpty() ? "cn1-hardening" : buildKey);
        int h = 0;
        for (int i = 0; i < basis.length(); i++) {
            h = h * 31 + basis.charAt(i);
        }
        return h;
    }

    private static int countRenamed(java.util.Set<String> before, java.util.Set<String> after) {
        int n = 0;
        for (String b : before) {
            if (!after.contains(b)) {
                n++;
            }
        }
        return n;
    }

    private static void writeReport(File reportFile, HardeningConfig cfg, HardeningResult r)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"engine\": \"").append(ENGINE_VERSION).append("\",\n");
        sb.append("  \"proguard\": \"").append(PROGUARD_VERSION).append("\",\n");
        sb.append("  \"platform\": \"").append(json(cfg.getPlatform())).append("\",\n");
        sb.append("  \"profile\": \"").append(cfg.getProfile().name().toLowerCase()).append("\",\n");
        sb.append("  \"outcome\": \"").append(r.getOutcome().name()).append("\",\n");
        sb.append("  \"classesIn\": ").append(r.getClassesIn()).append(",\n");
        sb.append("  \"classesOut\": ").append(r.getClassesOut()).append(",\n");
        sb.append("  \"renamedClasses\": ").append(r.getRenamedClasses()).append(",\n");
        sb.append("  \"encryptedStrings\": ").append(r.getEncryptedStrings()).append(",\n");
        sb.append("  \"mappingId\": \"").append(json(r.getMappingId())).append("\",\n");
        sb.append("  \"transforms\": [");
        List<String> t = r.getTransformsApplied();
        for (int i = 0; i < t.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(json(t.get(i))).append('"');
        }
        sb.append("],\n");
        // Serialize the warnings too: a warning records a known limitation (e.g. plaintext left in a
        // JDK 9+ concat recipe, or a transform skipped as unsafe for the platform). A consumer reading
        // the report -- not the forked-process log -- would otherwise see "transforms":["strings:all"]
        // with no hint that some plaintext was excluded, which the doc promises the report surfaces.
        sb.append("  \"warnings\": [");
        List<String> w = r.getWarnings();
        for (int i = 0; i < w.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(json(w.get(i))).append('"');
        }
        sb.append("]\n");
        sb.append("}\n");
        writeText(reportFile, sb.toString());
    }

    private static String json(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void writeText(File f, String text) throws IOException {
        FileOutputStream fo = new FileOutputStream(f);
        try {
            Writer w = new OutputStreamWriter(fo, Charset.forName("UTF-8"));
            w.write(text);
            w.flush();
        } finally {
            fo.close();
        }
    }

    private static void require(boolean cond, String message) throws HardeningException {
        if (!cond) {
            throw new HardeningException(message);
        }
    }
}
