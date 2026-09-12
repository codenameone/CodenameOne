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

package com.codename1.tools.translator;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Shai Almog
 */
public class ByteCodeTranslator {
    public enum OutputType {
        
        OUTPUT_TYPE_IOS {
            @Override
            public String extension() {
                return "m";
            }

            @Override
            public boolean isApple() {
                return true;
            }
        },
        /// The native macOS (AppKit) target. Emits Objective-C exactly like
        /// OUTPUT_TYPE_IOS -- the difference is the Xcode project written around
        /// it, not the translated code -- so anything keyed on "are we emitting
        /// .m" must ask isApple() rather than compare against OUTPUT_TYPE_IOS.
        OUTPUT_TYPE_MACOS {
            @Override
            public String extension() {
                return "m";
            }

            @Override
            public boolean isApple() {
                return true;
            }
        },
        OUTPUT_TYPE_CLEAN {
            @Override
            public String extension() {
                return "c";
            }

        },
        OUTPUT_TYPE_JAVASCRIPT {
            @Override
            public String extension() {
                return "js";
            }

        };

        public abstract String extension();

        /// True for the targets that emit Objective-C into a generated Xcode
        /// project (iOS and native macOS), false for the C and JavaScript ones.
        public boolean isApple() {
            return false;
        }
    }
    /// Which Apple platform the generated Xcode project targets.
    ///
    /// The translated Objective-C is identical for both -- everything from
    /// cn1_globals through Parser.writeOutput is shared verbatim. The platform
    /// only selects the four things that describe the *project*: the pbxproj
    /// template, the Info.plist template, the asset-catalog Contents.json, and
    /// whether the iOS-only device-family/launch-image machinery applies.
    enum ApplePlatform {
        IOS("/template", true),
        MACOS("/template-macos", false);

        private final String templateRoot;
        private final boolean iosDeviceIdioms;

        ApplePlatform(String templateRoot, boolean iosDeviceIdioms) {
            this.templateRoot = templateRoot;
            this.iosDeviceIdioms = iosDeviceIdioms;
        }

        /// Classpath root holding template.xcodeproj/ and template/ for this platform.
        String templateRoot() {
            return templateRoot;
        }

        /// True when TARGETED_DEVICE_FAMILY, launch images and the iPhone/iPad
        /// app-type distinction mean anything. A Mac app has no device family.
        boolean hasIosDeviceIdioms() {
            return iosDeviceIdioms;
        }
    }

    public static OutputType output = OutputType.OUTPUT_TYPE_IOS;
    public static boolean verbose = true;

    /**
     * Provenance of every file that lands in the generated source directory, so a
     * consumer of the build log can tell a codegen defect from a port bug from
     * somebody else's vendored code. Written out beside the generated project at the
     * end of each output handler; see {@link SourceManifest}.
     */
    static final SourceManifest sourceManifest = new SourceManifest();
    
    ByteCodeTranslator() {
    }
    
    /**
     * Recursively parses the files in the hierarchy to the output directory
     */
    void execute(File[] sourceDirs, File outputDir) throws Exception {
        for(File f : sourceDirs) {
            execute(f, outputDir);
        }
    }
    
    private static void sortByName(File[] files) {
        if (files != null) {
            java.util.Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
        }
    }

    void execute(File sourceDir, File outputDir) throws Exception {
        File[] directoryList = Util.listFiles(sourceDir, pathname ->
                !pathname.isHidden() && !pathname.getName().startsWith(".") && pathname.isDirectory());
        File[] fileList = Util.listFiles(sourceDir, pathname ->
                !pathname.isHidden() && !pathname.getName().startsWith(".") && !pathname.isDirectory());
        // listFiles() returns whatever order the filesystem hands back, which can
        // differ between two builds of the same input (the app classes are
        // re-extracted per build). Parse order is observable in the output --
        // cross-class analyses see either the raw or the already-optimized form of
        // another class -- so sort it and keep builds reproducible.
        sortByName(directoryList);
        sortByName(fileList);
        if(fileList != null) {
            for(File f : fileList) {
                if (f.getName().equals("module-info.class")) {
                    // Remove module-info.class that might have been added by jdk9 compiler
                    System.out.println("WARNING: Found module-info.class file at "+f+".  One or more of your jars must have been built for JDK9 or higher.  -target 8 or lower is required.");
                    System.out.println("         Will ignore this warning and attempt build anyways.");
                    continue;
                }
                if(f.getName().endsWith(".class")) {
                    Parser.parse(f);
                } else {
                    if(!f.isDirectory() && !isBuildMetadata(f)) {
                        // copy the file to the dest dir
                        copy(new FileInputStream(f), new FileOutputStream(new File(outputDir, f.getName())));
                        // Everything that reaches here is hand-written: a port native, a
                        // cn1lib's native, or an application resource. This is the only
                        // point at which its ORIGIN is still known -- one line further on
                        // it is an anonymous sibling of the generated code -- so record it
                        // here or lose the distinction for good.
                        sourceManifest.recordPort(f.getName(), f);
                    }
                }
            }
        }
        if(directoryList != null) {
            for(File f : directoryList) {
                if(f.getName().endsWith(".bundle") || f.getName().endsWith(".xcdatamodeld")) {
                    copyDir(f, outputDir);
                    continue;
                }
                execute(f, outputDir);
            }
        }
    }
    
    private void copyDir(File source, File destDir) throws IOException {
        File destFile = new File(destDir, source.getName());
        destFile.mkdirs();
        File[] files = source.listFiles();
        if (files == null) {
            return;
        }
        for(File f : files) {
            if(f.isDirectory()) {
                copyDir(f, destFile);
            } else {
                copy(new FileInputStream(f), new FileOutputStream(new File(destFile, f.getName())));
            }
        }
    }

    /**
     * @param args the command line arguments
     */

    /**
     * True when the application uses com.codename1.db and therefore needs the bundled SQLite
     * engine compiled in. Set by the platform builders from their class scan.
     */
    static boolean isBundledSqliteEnabled() {
        return "true".equals(Util.getProperty("cn1.sqlite", "false"));
    }

    /**
     * True when the application uses encrypted databases. On iOS this additionally replaces the
     * system libsqlite3, which has no cipher support, with the bundled engine.
     */
    static boolean isBundledSqliteCipherEnabled() {
        return "true".equals(Util.getProperty("cn1.sqlcipher", "false"));
    }

    /**
     * True when CHECKCAST should actually verify the cast and throw ClassCastException
     * instead of expanding to nothing (issue #5531). Opt-in, because enforcing it changes
     * the outcome of app builds that succeed today: a cast that silently produced the wrong
     * object now throws where nothing threw before. Server-side (clean-target) builds handle
     * untrusted input and should always enable it.
     *
     * <p>This one flag drives both halves and they must stay in agreement: it makes
     * TypeInstruction emit BC_CHECKCAST_CHECKED, and it makes ByteCodeClass retain
     * java.lang.ClassCastException. Emitting the check without retaining the class would
     * leave an unresolved symbol at link time.
     */
    /**
     * EXPERIMENTAL, and deliberately INERT unless asked for.
     *
     * Checked casts exist for the server-side clean target, where there is no EDT
     * catch upstream and a bad cast otherwise walks into generated native field
     * access. They are OFF by default on purpose -- including on the clean target --
     * because the feature is not finished and nothing in this repository ships with
     * it on. Turning it on by default was suggested in review and is wrong: it would
     * change codegen for every clean-target build in the tree to exercise a path that
     * is still being designed.
     *
     * Enable it deliberately with -Dcn1.checkedCasts=true. The emitted checks
     * (BC_CHECKCAST_CHECKED, CN1_ARRAY_STORE_CHECK) are maintained and reviewed under
     * that flag; they are not a claim that the VM validates casts today. See
     * CLAUDE.md, "Never rely on ClassCastException", which remains the rule for every
     * shipping target.
     */
    public static boolean isCheckedCastsEnabled() {
        return "true".equalsIgnoreCase(Util.getProperty("cn1.checkedCasts", "false"));
    }

    /// Writes the bundled SQLite engine into a source root, or takes it back out.
    ///
    /// Emitted only for an application that uses `com.codename1.db`, and its ciphers only for one
    /// that configures encryption -- but a source root is reused between runs, and the CMake
    /// project globs every `.c` beside it. An engine left behind by an earlier run with the
    /// switch on would be compiled by a build with it off: the gate would appear to do nothing,
    /// and a plaintext application would carry the ciphers it was meant to be spared. So the
    /// files this decides on are removed when they are not wanted, rather than merely not
    /// written.
    ///
    /// #### Parameters
    ///
    /// - `srcRoot`: the directory the translated sources are written to
    /**
     * Copies one of the ParparVM runtime sources bundled in the translator's jar into
     * the generated project, recording its provenance on the way through.
     *
     * <p>Every runtime file goes through here rather than calling {@link #copy} directly,
     * so that one cannot be added later without being recorded. A file the manifest does
     * not name is indistinguishable from generated code to anything reading the build
     * log, which is exactly the confusion {@link SourceManifest} exists to prevent.</p>
     *
     * @param srcRoot the generated project's source directory
     * @param name the resource name, which is also the file name it is written under
     * @return the file that was written, for callers that go on to edit it
     */
    private static File copyRuntimeResource(File srcRoot, String name) throws IOException {
        return copyRuntimeResource(srcRoot, name, name);
    }

    /**
     * As {@link #copyRuntimeResource(File, String)}, for the clean target, which writes
     * several of the runtime sources out under a different name than the resource has --
     * {@code cn1_globals.m} becomes {@code cn1_globals.c}, because that target compiles C
     * rather than Objective-C. The manifest records the name the file has ON DISK, since
     * that is the name a compiler diagnostic will carry.
     *
     * @param srcRoot the generated project's source directory
     * @param name the resource name inside the translator jar
     * @param destName the file name to write it under
     * @return the file that was written, for callers that go on to edit it
     */
    private static File copyRuntimeResource(File srcRoot, String name, String destName) throws IOException {
        File dest = new File(srcRoot, destName);
        copy(ByteCodeTranslator.class.getResourceAsStream("/" + name), new FileOutputStream(dest));
        sourceManifest.recordRuntime(destName, "/" + name);
        return dest;
    }

    /**
     * As {@link #copyRuntimeResource}, for third-party code we bundle but do not
     * maintain. Kept separate rather than taking an origin parameter because the two
     * differ in what a warning MEANS: a warning in the runtime is ours to fix, and one
     * in vendored code is reported and never gated.
     *
     * @param srcRoot the generated project's source directory
     * @param name the resource name, which is also the file name it is written under
     * @return the file that was written, for callers that go on to edit it
     */
    private static File copyVendoredResource(File srcRoot, String name) throws IOException {
        File dest = new File(srcRoot, name);
        copy(ByteCodeTranslator.class.getResourceAsStream("/" + name), new FileOutputStream(dest));
        sourceManifest.recordVendored(name, "/" + name);
        return dest;
    }

    private static void emitBundledSqlite(File srcRoot) throws IOException {
        File sqliteUnity = new File(srcRoot, "cn1_sqlite3.c");
        File sqliteHeader = new File(srcRoot, "cn1_sqlite3.h");
        File sqliteAmalgamation = new File(srcRoot, "cn1_sqlite3_amalgamation.h");
        File sqliteCipherMarker = new File(srcRoot, "cn1_sqlite3_cipher.h");
        if (isBundledSqliteEnabled()) {
            copyVendoredResource(srcRoot, "cn1_sqlite3.c");
            replaceInFile(sqliteUnity, "//#define CN1_INCLUDE_SQLITE", "#define CN1_INCLUDE_SQLITE");
            copyVendoredResource(srcRoot, "cn1_sqlite3.h");
            copyVendoredResource(srcRoot, "cn1_sqlite3_amalgamation.h");
        } else {
            deleteIfPresent(sqliteUnity);
            deleteIfPresent(sqliteHeader);
            deleteIfPresent(sqliteAmalgamation);
        }
        if (isBundledSqliteEnabled() && isBundledSqliteCipherEnabled()) {
            // A marker, not a define: the engine, the shared bindings and IOSNative.m are three
            // translation units that all have to agree on whether a cipher exists, and
            // __has_include is the only thing they can agree on without per-target compiler
            // flags. Emitted only for an application that configures encryption, so everyone else
            // compiles the engine as plain SQLite and links no keying code at all.
            copyVendoredResource(srcRoot, "cn1_sqlite3_cipher.h");
        } else {
            // Left behind, this would put the ciphers back into an engine emitted without them --
            // __has_include does not care which run wrote the file.
            deleteIfPresent(sqliteCipherMarker);
        }
    }

    /// Removes a file this run decided not to emit, and says so if it cannot.
    ///
    /// Silence here would be the failure the caller is trying to avoid: the stale file would be
    /// compiled and nothing would report why.
    ///
    /// #### Parameters
    ///
    /// - `stale`: the file to remove
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the file is there and cannot be removed
    private static void deleteIfPresent(File stale) throws IOException {
        if (stale.exists() && !stale.delete()) {
            throw new IOException("A previous build left " + stale + " behind and it could not be "
                    + "removed. It would be compiled into this one, which is what emitting it "
                    + "conditionally is meant to prevent.");
        }
    }

    public static void main(String[] args) throws Exception {
        // Provenance describes ONE translation, and this is the start of one. It has to
        // be cleared here rather than in Parser.cleanup(): that runs from the `finally`
        // of Parser.writeOutput(), which is BEFORE the output handler writes the
        // manifest, so resetting there threw away everything the translation had just
        // recorded -- the generated files most of all. Clearing on the way IN is also
        // the property actually wanted, since what must not leak is the PREVIOUS
        // application's files, and the tests translate repeatedly in one JVM through
        // this method.
        sourceManifest.reset();
        if(args.length == 0) {
            new File("build/kitchen").mkdirs();
            args = new String[] {"ios", "/Users/shai/dev/CodenameOne/ByteCodeTranslator/tmp;/Users/shai/dev/cn1/vm/JavaAPI/build/classes;/Users/shai/dev/cn1/Ports/iOSPort/build/classes;/Users/shai/dev/cn1/Ports/iOSPort/nativeSources;/Users/shai/dev/cn1/CodenameOne/build/classes;/Users/shai/dev/codenameone-demos/KitchenSink/build/classes", 
                "build/kitchen", "KitchenSink", "com.codename1.demos.kitchen", "Kitchen Sink", "1.0", "ios", "none"};
        }
        
        if(args.length != 9) {
            System.out.println("We accept 9 arguments output type (ios, clean, javascript), input directory, output directory, app name, package name, app dispaly name, version, type (ios/iphone/ipad) and additional frameworks");
            System.exit(1);
            return;
        }
        final String appName = args[3];
        final String appPackageName = args[4];
        final String appDisplayName = args[5];
        final String appVersion = args[6];
        final String appType = args[7];
        final String addFrameworks = args[8];
        // we accept 3 argument output types, input directory and output directory
        if (Util.getProperty("saveUnitTests", "false").equals("true")) {
            System.out.println("Generating Unit Tests");
            ByteCodeClass.setSaveUnitTests(true);
        }
        boolean recognizedOutputType = true;
        if(args[0].equalsIgnoreCase("ios")) {
            output = OutputType.OUTPUT_TYPE_IOS;
        } else if(args[0].equalsIgnoreCase("macos")) {
            output = OutputType.OUTPUT_TYPE_MACOS;
        } else if(args[0].equalsIgnoreCase("clean")) {
            output = OutputType.OUTPUT_TYPE_CLEAN;
        } else if(args[0].equalsIgnoreCase("javascript")) {
            output = OutputType.OUTPUT_TYPE_JAVASCRIPT;
        } else {
            // Unrecognized output type falls back to the plain copy-through default handler
            recognizedOutputType = false;
        }
        String[] sourceDirectories = Util.splitLiteral(args[1], ';');
        File[] sources = new File[sourceDirectories.length];
        for(int iter = 0 ; iter < sourceDirectories.length ; iter++) {
            sources[iter] = new File(sourceDirectories[iter]);
            if(!sources[iter].exists() && sources[iter].isDirectory()) {
                System.out.println("Source directory doesn't exist: " + sources[iter].getAbsolutePath());
                System.exit(2);
                return;
            }
        }
        File dest = new File(args[2]);
        if(!dest.exists() && dest.isDirectory()) {
            System.out.println("Dest directory doesn't exist: " + dest.getAbsolutePath());
            System.exit(3);
            return;
        }
        
        // Select which @Concrete attribute the parser honours: the native Windows
        // app type prefers Concrete.win(), the native Linux app type prefers
        // Concrete.linux(), the native macOS app type prefers Concrete.mac(),
        // every other target uses Concrete.name() (the iOS pipeline). Set before
        // any parsing happens below.
        //
        // Getting this wrong is silent rather than fatal: ParparVM devirtualizes
        // calls on an annotated type straight to the concrete class, so a macOS
        // build left on the iOS attribute would bind every CodenameOneImplementation
        // call to IOSImplementation and never run a MacImplementation override.
        ByteCodeClass.setConcreteTarget("windows".equalsIgnoreCase(appType) ? "win"
                : "linux".equalsIgnoreCase(appType) ? "linux"
                : "mac".equalsIgnoreCase(appType) ? "mac" : null);

        ByteCodeTranslator b = new ByteCodeTranslator();
        try {
            if (!recognizedOutputType) {
                handleDefaultOutput(b, sources, dest);
                return;
            }
            switch (output) {
                case OUTPUT_TYPE_IOS:
                    handleAppleOutput(b, sources, dest, appName, appPackageName, appDisplayName, appVersion, appType, addFrameworks, ApplePlatform.IOS);
                    break;
                case OUTPUT_TYPE_MACOS:
                    handleAppleOutput(b, sources, dest, appName, appPackageName, appDisplayName, appVersion, appType, addFrameworks, ApplePlatform.MACOS);
                    break;
                case OUTPUT_TYPE_CLEAN:
                    handleCleanOutput(b, sources, dest, appName, appType);
                    break;
                case OUTPUT_TYPE_JAVASCRIPT:
                    handleJavascriptOutput(b, sources, dest, appName);
                    break;
                default:
                    handleDefaultOutput(b, sources, dest);
            }
        } catch (NativeSignatureVerifier.VerificationFailedException ex) {
            // The verifier has already printed the per-method diagnosis. A stack
            // trace here would only push it out of view in the build log.
            System.err.println(ex.getMessage());
            System.exit(4);
        }
    }

    private static void handleDefaultOutput(ByteCodeTranslator b, File[] sources, File dest) throws Exception {
        b.execute(sources, dest);
        Parser.writeOutput(dest);
    }

    private static void handleCleanOutput(ByteCodeTranslator b, File[] sources, File dest, String appName, String appType) throws Exception {
        File root = new File(dest, "dist");
        root.mkdirs();
        if(verbose) {
            System.out.println("Root is: " + root.getAbsolutePath());
        }
        File srcRoot = new File(root, appName + "-src");
        srcRoot.mkdirs();

        b.execute(sources, srcRoot);

        File cn1Globals = copyRuntimeResource(srcRoot, "cn1_globals.h");
        copyRuntimeResource(srcRoot, "cn1_intrinsics.h");
        // Virtual threads: the switch is a few instructions of assembly per
        // architecture, so the .S travels with the runtime rather than being
        // generated. A project that gets the C and not the .S links against a
        // missing symbol, which is at least loud.
        emitVirtualThreadRuntime(srcRoot);
        if (Util.getProperty("INCLUDE_NPE_CHECKS", "false").equals("true")) {
            replaceInFile(cn1Globals, "//#define CN1_INCLUDE_NPE_CHECKS",  "#define CN1_INCLUDE_NPE_CHECKS");
        }
        if ("true".equalsIgnoreCase(Util.getProperty("cn1.onDeviceDebug", "false"))) {
            replaceInFile(cn1Globals, "//#define CN1_ON_DEVICE_DEBUG", "#define CN1_ON_DEVICE_DEBUG");
        }
        copyRuntimeResource(srcRoot, "cn1_globals.m", "cn1_globals.c");
        copyRuntimeResource(srcRoot, "nativeMethods.m", "nativeMethods.c");
        if (Util.getProperty("USE_RPMALLOC", "false").equals("true")) {
            copyRuntimeResource(srcRoot, "malloc.c");
            copyRuntimeResource(srcRoot, "rpmalloc.c");
            copyRuntimeResource(srcRoot, "rpmalloc.h");
        }
        // The bundled SQLite engine is emitted only for applications that actually use
        // com.codename1.db, so everyone else pays nothing for it. cn1_sqlite3.c is gated on
        // CN1_INCLUDE_SQLITE internally as well, so an emitted-but-undefined build compiles it
        // to an empty object rather than failing.
        // Always emitted: it defines the native entry points either way, as real bindings when
        // the engine is present and as stubs when it is not, so an application that references
        // com.codename1.db links regardless of how the translator was invoked.
        copyRuntimeResource(srcRoot, "cn1_db_sqlite_impl.h");
        emitBundledSqlite(srcRoot);
        copyRuntimeResource(srcRoot, "xmlvm.h");

        // Win32 POSIX compatibility shim. Always emitted; both files are gated on
        // _WIN32 internally, so they compile to nothing on iOS/macOS/Linux and
        // provide pthreads/usleep/gettimeofday on Windows (clang-cl / MSVC ABI).
        copyRuntimeResource(srcRoot, "cn1_win_compat.h");
        copyRuntimeResource(srcRoot, "cn1_win_compat.c");

        Parser.writeOutput(srcRoot);

        File javaIoFileHeader = new File(srcRoot, "java_io_File.h");
        if (javaIoFileHeader.exists()) {
            copyRuntimeResource(srcRoot, "java_io_File.m", "java_io_File_runtime.c");
        }

        File classMethodIndexM = new File(srcRoot, "cn1_class_method_index.m");
        if (classMethodIndexM.exists()) {
            File classMethodIndexC = new File(srcRoot, "cn1_class_method_index.c");
            copy(new FileInputStream(classMethodIndexM), new FileOutputStream(classMethodIndexC));
            if(!classMethodIndexM.delete()) {
                System.err.println("Deletion of " + classMethodIndexM.getAbsolutePath() + " failed");
            }
            // Parser recorded the .m it wrote; this target compiles C, so the file that
            // actually reaches the compiler -- and that a diagnostic will name -- is the
            // .c. Re-record under the surviving name and drop the one that no longer
            // exists, or the manifest describes a file nothing will ever build.
            sourceManifest.renameGenerated("cn1_class_method_index.m", "cn1_class_method_index.c");
        }

        // Native Windows produces a single self-contained .exe (there is no .app
        // bundle directory to sit resources beside): the app's classpath resources
        // (theme.res, images, l10n, ...) are embedded into the executable's PE
        // resource section and read back at runtime via getResourceAsStream ->
        // FindResource. Mirrors how the iOS .app carries them, but inside the exe.
        if ("windows".equalsIgnoreCase(appType)) {
            embedWindowsResources(sources, srcRoot);
        }

        // Native Linux mirrors the Windows single-binary model: there is no .app
        // bundle to sit resources beside, so the classpath resources are .incbin'd
        // into the ELF .rodata via a generated assembly stub and read back at
        // runtime through getResourceAsStream -> cn1LinuxFindResource.
        if ("linux".equalsIgnoreCase(appType)) {
            embedLinuxResources(sources, srcRoot);
        }

        writeCmakeProject(root, srcRoot, appName, appType);

        // Written to the project root rather than srcRoot on purpose: writeCmakeProject
        // globs srcRoot for sources, and the Apple path lists it into the Xcode project,
        // where an unrecognised extension lands in the resources phase and ships inside
        // the bundle. See SourceManifest.
        sourceManifest.write(root);
    }

    /**
     * Stages every classpath resource found across the translator source roots
     * (any file that is not a translated class or a C/C++/ObjC/Java source) and
     * emits two artifacts into {@code srcRoot}:
     * <ul>
     *   <li>{@code cn1_resources.rc} -- a Win32 resource script that embeds each
     *       resource as an {@code RCDATA} blob under a small integer id, compiled
     *       by the RC language and linked into the exe (see writeCmakeProject);</li>
     *   <li>{@code cn1_resources_table.c} -- a {@code name -> id} lookup
     *       ({@code cn1WinFindResourceId}) the native getResourceAsStream uses to
     *       resolve a classpath path (e.g. {@code /theme.res}) to its RCDATA id.</li>
     * </ul>
     * The table file is always written (even with no resources) so the symbol the
     * native layer links against always exists; the .rc is written only when at
     * least one resource was found. The first root that provides a given resource
     * path wins, so app resources shadow framework ones (the source-root order
     * places the app classes first).
     */
    private static void embedWindowsResources(File[] sources, File srcRoot) throws IOException {
        java.util.LinkedHashMap<String, File> resources = new java.util.LinkedHashMap<String, File>();
        if (sources != null) {
            for (File rootDir : sources) {
                if (rootDir != null && rootDir.isDirectory()) {
                    collectResources(rootDir, rootDir, resources);
                }
            }
        }

        StringBuilder table = new StringBuilder();
        table.append("/* Auto-generated by the ParparVM windows target: maps a classpath\n");
        table.append(" * resource path to the RCDATA id embedded in the executable. */\n");
        table.append("#include <string.h>\n\n");
        table.append("typedef struct { const char* name; int id; } CN1ResourceEntry;\n\n");
        table.append("static const CN1ResourceEntry cn1ResourceTable[] = {\n");

        if (!resources.isEmpty()) {
            File resDir = new File(srcRoot, "cn1_resources");
            resDir.mkdirs();
            StringBuilder rc = new StringBuilder();
            rc.append("#include <windows.h>\n");
            int id = 1;
            for (java.util.Map.Entry<String, File> e : resources.entrySet()) {
                File staged = new File(resDir, "res" + id);
                copy(new FileInputStream(e.getValue()), new FileOutputStream(staged));
                // RC filenames are resolved relative to the .rc (srcRoot); llvm-rc and
                // rc.exe both accept forward slashes.
                rc.append(id).append(" RCDATA \"cn1_resources/res").append(id).append("\"\n");
                table.append("    {\"").append(escapeCString(e.getKey())).append("\", ").append(id).append("},\n");
                id++;
            }
            sourceManifest.recordGenerated("cn1_resources.rc");
            Util.writeBytes(new File(srcRoot, "cn1_resources.rc"),
                    rc.toString().getBytes(StandardCharsets.UTF_8));
        }

        table.append("    {0, 0}\n};\n\n");
        table.append("int cn1WinFindResourceId(const char* name) {\n");
        table.append("    int i;\n");
        table.append("    if (name == 0) { return 0; }\n");
        table.append("    for (i = 0; cn1ResourceTable[i].name != 0; i++) {\n");
        table.append("        if (strcmp(cn1ResourceTable[i].name, name) == 0) { return cn1ResourceTable[i].id; }\n");
        table.append("    }\n");
        table.append("    return 0;\n");
        table.append("}\n");
        sourceManifest.recordGenerated("cn1_resources_table.c");
        Util.writeBytes(new File(srcRoot, "cn1_resources_table.c"),
                table.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Linux analog of {@link #embedWindowsResources}: stages every classpath
     * resource and emits two artifacts into {@code srcRoot}:
     * <ul>
     *   <li>{@code cn1_resources_data.S} -- an assembly stub that {@code .incbin}s
     *       each staged resource into {@code .rodata} under a {@code cn1res_N} /
     *       {@code cn1res_N_end} symbol pair (start + end markers give the blob
     *       length without a separate size table). Compiled by the ASM language
     *       enabled in writeCmakeProject and linked into the executable.</li>
     *   <li>{@code cn1_resources_table.c} -- a {@code name -> (data,end)} lookup
     *       ({@code cn1LinuxFindResource}) the native getResourceAsStream uses to
     *       resolve a classpath path (e.g. {@code /theme.res}) to its embedded
     *       bytes and length.</li>
     * </ul>
     * The table file is always written (even with no resources) so the symbol the
     * native layer links against always exists; the {@code .S} is written only
     * when at least one resource was found. The first root that provides a given
     * resource path wins, so app resources shadow framework ones.
     */
    private static void embedLinuxResources(File[] sources, File srcRoot) throws IOException {
        java.util.LinkedHashMap<String, File> resources = new java.util.LinkedHashMap<String, File>();
        if (sources != null) {
            for (File rootDir : sources) {
                if (rootDir != null && rootDir.isDirectory()) {
                    collectResources(rootDir, rootDir, resources);
                }
            }
        }

        StringBuilder table = new StringBuilder();
        table.append("/* Auto-generated by the ParparVM linux target: maps a classpath\n");
        table.append(" * resource path to the bytes .incbin'd into the executable. */\n");
        table.append("#include <string.h>\n\n");

        if (!resources.isEmpty()) {
            File resDir = new File(srcRoot, "cn1_resources");
            resDir.mkdirs();
            StringBuilder asm = new StringBuilder();
            // A read-only data section; symbols are word-aligned so they survive
            // any toolchain default alignment. Works with the GNU and clang
            // integrated assemblers (gcc / clang / zig cc) alike.
            asm.append("    .section .rodata\n");
            int id = 1;
            for (java.util.Map.Entry<String, File> e : resources.entrySet()) {
                File staged = new File(resDir, "res" + id);
                copy(new FileInputStream(e.getValue()), new FileOutputStream(staged));
                // Absolute path so .incbin resolves regardless of the assembler's
                // working directory (the build runs out of a separate build dir).
                String incPath = escapeCString(staged.getAbsolutePath().replace('\\', '/'));
                asm.append("    .align 8\n");
                asm.append("    .global cn1res_").append(id).append("\n");
                asm.append("cn1res_").append(id).append(":\n");
                asm.append("    .incbin \"").append(incPath).append("\"\n");
                asm.append("    .global cn1res_").append(id).append("_end\n");
                asm.append("cn1res_").append(id).append("_end:\n");
                table.append("extern const unsigned char cn1res_").append(id)
                        .append("[]; extern const unsigned char cn1res_").append(id).append("_end[];\n");
                id++;
            }
            sourceManifest.recordGenerated("cn1_resources_data.S");
            Util.writeBytes(new File(srcRoot, "cn1_resources_data.S"),
                    asm.toString().getBytes(StandardCharsets.UTF_8));
        }

        table.append("\ntypedef struct { const char* name; const unsigned char* data; const unsigned char* end; } CN1ResourceEntry;\n\n");
        table.append("static const CN1ResourceEntry cn1ResourceTable[] = {\n");
        int id = 1;
        for (java.util.Map.Entry<String, File> e : resources.entrySet()) {
            table.append("    {\"").append(escapeCString(e.getKey())).append("\", cn1res_")
                    .append(id).append(", cn1res_").append(id).append("_end},\n");
            id++;
        }
        table.append("    {0, 0, 0}\n};\n\n");
        table.append("const unsigned char* cn1LinuxFindResource(const char* name, int* lenOut) {\n");
        table.append("    int i;\n");
        table.append("    if (name == 0) { if (lenOut) { *lenOut = 0; } return 0; }\n");
        table.append("    for (i = 0; cn1ResourceTable[i].name != 0; i++) {\n");
        table.append("        if (strcmp(cn1ResourceTable[i].name, name) == 0) {\n");
        table.append("            if (lenOut) { *lenOut = (int)(cn1ResourceTable[i].end - cn1ResourceTable[i].data); }\n");
        table.append("            return cn1ResourceTable[i].data;\n");
        table.append("        }\n");
        table.append("    }\n");
        table.append("    if (lenOut) { *lenOut = 0; }\n");
        table.append("    return 0;\n");
        table.append("}\n");
        sourceManifest.recordGenerated("cn1_resources_table.c");
        Util.writeBytes(new File(srcRoot, "cn1_resources_table.c"),
                table.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Recursively collects classpath resources under {@code dir}, keyed by their
     * path relative to {@code root} (with a leading {@code /}, matching the names
     * getResourceAsStream is called with). Translated/compiled inputs -- class,
     * Java, and C/C++/ObjC sources/headers -- are skipped; everything else is a
     * resource. Existing keys are kept (first root wins).
     */
    private static void collectResources(File root, File dir, java.util.LinkedHashMap<String, File> out) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collectResources(root, f, out);
                continue;
            }
            String name = f.getName();
            int dot = name.lastIndexOf('.');
            String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
            if (ext.equals("class") || ext.equals("java") || ext.equals("c") || ext.equals("cpp")
                    || ext.equals("cc") || ext.equals("h") || ext.equals("hpp") || ext.equals("m")
                    || ext.equals("mm") || ext.equals("rc")) {
                continue;
            }
            // Relative path by absolute-prefix strip rather than Path.relativize:
            // JavaAPI has no java.nio.file, and the translator compiles against it
            // when it translates itself. f is always under root here -- it came from
            // a walk of root -- so the prefix always matches.
            String rootAbs = root.getAbsolutePath();
            String fileAbs = f.getAbsolutePath();
            String rel = fileAbs.startsWith(rootAbs)
                    ? fileAbs.substring(rootAbs.length())
                    : fileAbs;
            while (rel.startsWith(File.separator) || rel.startsWith("/")) {
                rel = rel.substring(1);
            }
            rel = rel.replace('\\', '/');
            String key = "/" + rel;
            if (!out.containsKey(key)) {
                out.put(key, f);
            }
        }
    }

    /** Escapes a string for a C double-quoted literal (backslash and quote). */
    private static String escapeCString(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '"') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static void handleJavascriptOutput(ByteCodeTranslator b, File[] sources, File dest, String appName) throws Exception {
        File root = new File(dest, "dist");
        root.mkdirs();
        if(verbose) {
            System.out.println("Root is: " + root.getAbsolutePath());
        }
        File srcRoot = new File(root, appName + "-js");
        srcRoot.mkdirs();
        ByteCodeClass.setPreferredMainClass(appName);
        b.execute(sources, srcRoot);
        Parser.writeOutput(srcRoot);
    }

    private static void handleAppleOutput(ByteCodeTranslator b, File[] sources, File dest, String appName, String appPackageName, String appDisplayName, String appVersion, String appType, String addFrameworks, ApplePlatform platform) throws Exception {
        final String templateRoot = platform.templateRoot();
        File root = new File(dest, "dist");
        root.mkdirs();
         if(verbose) {
             System.out.println("Root is: " + root.getAbsolutePath());
         }
        File srcRoot = new File(root, appName + "-src");
        srcRoot.mkdirs();
        //cleanDir(srcRoot);

        if(verbose) {
            System.out.println("srcRoot is: " + srcRoot.getAbsolutePath() );
        }

        File imagesXcassets = new File(srcRoot, "Images.xcassets");
        imagesXcassets.mkdirs();
        //cleanDir(imagesXcassets);

        // A Mac app has no launch image -- the concept is iOS-only, and an
        // empty LaunchImage.launchimage in the catalog is an asset-catalog
        // warning rather than something the platform ever consults.
        if (platform.hasIosDeviceIdioms()) {
            File  launchImageLaunchimage = new File(imagesXcassets, "LaunchImage.launchimage");
            launchImageLaunchimage.mkdirs();
            //cleanDir(launchImageLaunchimage);

            copy(ByteCodeTranslator.class.getResourceAsStream("/LaunchImages.json"), new FileOutputStream(new File(launchImageLaunchimage, "Contents.json")));
        }

        File appIconAppiconset = new File(imagesXcassets, "AppIcon.appiconset");
        appIconAppiconset.mkdirs();
        //cleanDir(appIconAppiconset);

        // The icon idioms differ: iOS wants the phone/pad/marketing sizes, macOS
        // wants the 16..512 @1x/@2x "mac" idiom ladder.
        copy(ByteCodeTranslator.class.getResourceAsStream(
                        platform.hasIosDeviceIdioms() ? "/Icons.json" : "/Icons-macos.json"),
                new FileOutputStream(new File(appIconAppiconset, "Contents.json")));


        File xcproj = new File(root, appName + ".xcodeproj");
        xcproj.mkdirs();
        //cleanDir(xcproj);

        File projectXCworkspace = new File(xcproj, "project.xcworkspace");
        projectXCworkspace.mkdirs();

        b.execute(sources, srcRoot);

        File cn1Globals = copyRuntimeResource(srcRoot, "cn1_globals.h");
        copyRuntimeResource(srcRoot, "cn1_intrinsics.h");
        // Virtual threads: the switch is a few instructions of assembly per
        // architecture, so the .S travels with the runtime rather than being
        // generated. A project that gets the C and not the .S links against a
        // missing symbol, which is at least loud.
        emitVirtualThreadRuntime(srcRoot);
        if (Util.getProperty("INCLUDE_NPE_CHECKS", "false").equals("true")) {
            replaceInFile(cn1Globals, "//#define CN1_INCLUDE_NPE_CHECKS",  "#define CN1_INCLUDE_NPE_CHECKS");
        }
        if ("true".equalsIgnoreCase(Util.getProperty("cn1.onDeviceDebug", "false"))) {
            replaceInFile(cn1Globals, "//#define CN1_ON_DEVICE_DEBUG", "#define CN1_ON_DEVICE_DEBUG");
        }
        copyRuntimeResource(srcRoot, "cn1_globals.m");
        copyRuntimeResource(srcRoot, "nativeMethods.m");
        // java_io_File_RUNTIME.m, not java_io_File.m. When the application retains
        // java.io.File -- which the filesystem fallback makes ordinary -- Parser
        // .writeOutput emits the translated class to java_io_File.m and overwrites
        // the port's hand-written native that was copied here first. The generated
        // File.exists() then has no existsImpl to link against.
        //
        // OBSERVED as `Undefined symbols: _java_io_File_existsImpl ... referenced
        // from _java_io_File_exists___R_boolean in java_io_File.o` on the iOS legs.
        // The clean target already avoids the same collision by emitting
        // java_io_File_runtime.c; this is that fix for the Apple path. The name only
        // has to differ from the generated one -- the compiler globs the directory,
        // and NativeSignatureVerifier reads the RESOURCE "/java_io_File.m" rather
        // than the emitted filename.
        copyRuntimeResource(srcRoot, "java_io_File.m", "java_io_File_runtime.m");

        if (Util.getProperty("USE_RPMALLOC", "false").equals("true")) {
            copyRuntimeResource(srcRoot, "malloc.c");
            copyRuntimeResource(srcRoot, "rpmalloc.c");
            copyRuntimeResource(srcRoot, "rpmalloc.h");
        }
        // The bundled SQLite engine is emitted only for applications that actually use
        // com.codename1.db, so everyone else pays nothing for it. cn1_sqlite3.c is gated on
        // CN1_INCLUDE_SQLITE internally as well, so an emitted-but-undefined build compiles it
        // to an empty object rather than failing.
        // Always emitted: it defines the native entry points either way, as real bindings when
        // the engine is present and as stubs when it is not, so an application that references
        // com.codename1.db links regardless of how the translator was invoked.
        copyRuntimeResource(srcRoot, "cn1_db_sqlite_impl.h");
        emitBundledSqlite(srcRoot);

        Parser.writeOutput(srcRoot);

        File templateInfoPlist = new File(srcRoot, appName + "-Info.plist");
        copy(ByteCodeTranslator.class.getResourceAsStream(templateRoot + "/template/template-Info.plist"), new FileOutputStream(templateInfoPlist));

        File templatePch = new File(srcRoot, appName + "-Prefix.pch");
        copy(ByteCodeTranslator.class.getResourceAsStream(templateRoot + "/template/template-Prefix.pch"), new FileOutputStream(templatePch));

        copyRuntimeResource(srcRoot, "xmlvm.h");

        File projectWorkspaceData = new File(projectXCworkspace, "contents.xcworkspacedata");
        copy(ByteCodeTranslator.class.getResourceAsStream(templateRoot + "/template.xcodeproj/project.xcworkspace/contents.xcworkspacedata"), new FileOutputStream(projectWorkspaceData));
        replaceInFile(projectWorkspaceData, "KitchenSink", appName);


        File projectPbx = new File(xcproj, "project.pbxproj");
        copy(ByteCodeTranslator.class.getResourceAsStream(templateRoot + "/template.xcodeproj/project.pbxproj"), new FileOutputStream(projectPbx));

        // File.list(FilenameFilter) is not in JavaAPI; filter the plain listing.
        String[] allNames = srcRoot.list();
        java.util.List<String> keptNames = new java.util.ArrayList<String>();
        if (allNames != null) {
            for (String string : allNames) {
                File pathname = new File(srcRoot, string);
                if (string.endsWith(".bundle") || string.endsWith(".xcdatamodeld")
                        || !pathname.isHidden() && !string.startsWith(".")
                           && !"Images.xcassets".equals(string)) {
                    keptNames.add(string);
                }
            }
        }
        String[] sourceFiles = keptNames.toArray(new String[keptNames.size()]);

        StringBuilder fileOneEntry = new StringBuilder();
        StringBuilder fileTwoEntry = new StringBuilder();
        StringBuilder fileListEntry = new StringBuilder();
        StringBuilder fileThreeEntry = new StringBuilder();
        StringBuilder frameworks = new StringBuilder();
        StringBuilder frameworks2 = new StringBuilder();
        StringBuilder resources = new StringBuilder();

        List<String> noArcFiles = new ArrayList<>();
        noArcFiles.add("CVZBarReaderViewController.m");
        noArcFiles.add("OpenUDID.m");

        List<String> includeFrameworks = new ArrayList<>();
        Set<String> optionalFrameworks = new HashSet<>();
        for (String optionalFramework : Util.splitLiteral(Util.getProperty("optional.frameworks", ""), ';')) {
            optionalFramework = optionalFramework.trim();
            if (!optionalFramework.isEmpty()) {
                optionalFrameworks.add(optionalFramework);
            }
        }
        optionalFrameworks.add("UserNotifications.framework");
        includeFrameworks.add("libiconv.dylib");
        //includeFrameworks.add("AdSupport.framework");
        // AddressBookUI and AddressBook are iOS-only. On macOS the contacts API
        // is Contacts.framework, and linking the iOS ones fails outright rather
        // than merely going unused.
        if (platform.hasIosDeviceIdioms()) {
            includeFrameworks.add("AddressBookUI.framework");
            includeFrameworks.add("AddressBook.framework");
        } else {
            includeFrameworks.add("Contacts.framework");
        }
        includeFrameworks.add("SystemConfiguration.framework");
        includeFrameworks.add("MapKit.framework");
        includeFrameworks.add("AudioToolbox.framework");
        includeFrameworks.add("libxml2.dylib");
        includeFrameworks.add("QuartzCore.framework");
        if (!isBundledSqliteCipherEnabled()) {
            // The system libsqlite3 has no cipher support, so an application that encrypts
            // databases links the bundled engine instead. Linking both would put two SQLite
            // implementations in one process, which is at best wasteful and at worst confusing
            // to debug when they disagree about a file.
            includeFrameworks.add("libsqlite3.dylib");
            includeFrameworks.add("libsqlite3.0.dylib");
        }
        includeFrameworks.add("GameKit.framework");
        includeFrameworks.add("Security.framework");
        //includeFrameworks.add("StoreKit.framework");
        // CoreMotion has no macOS implementation -- a Mac has no accelerometer to
        // report -- MessageUI and MediaPlayer are UIKit composers, and
        // MobileCoreServices is the iOS spelling of the C-level type identifier
        // API that macOS keeps in CoreServices. Each of these is a link failure
        // rather than dead weight if it stays.
        if (platform.hasIosDeviceIdioms()) {
            includeFrameworks.add("CoreMotion.framework");
            includeFrameworks.add("MessageUI.framework");
            includeFrameworks.add("MediaPlayer.framework");
            includeFrameworks.add("MobileCoreServices.framework");
        }
        // UniformTypeIdentifiers is NOT the macOS spelling of MobileCoreServices, which is what
        // the split above used to assume: MobileCoreServices carries only the deprecated C
        // functions (UTTypeCreatePreferredIdentifierForTag and friends), while the UTType CLASS
        // lives here on every platform from iOS 14 / macOS 11 / watchOS 7 / tvOS 14.
        // CN1DragAndDrop.m compiles that class into every slice -- the MIME-to-identifier
        // mapping sits outside its
        // UIKit guard because the clipboard natives need it too -- so leaving the framework off the
        // iOS branch linked an application that referenced _OBJC_CLASS_$_UTType and nothing that
        // defined it, and every device archive failed at the link.
        //
        // Weak, because ios.deployment_target reaches back to releases that predate the framework
        // and a hard link against one the device does not have kills the process at launch. The
        // uses are @available fenced, so an older device simply takes the legacy path.
        includeFrameworks.add("UniformTypeIdentifiers.framework");
        optionalFrameworks.add("UniformTypeIdentifiers.framework");
        includeFrameworks.add("CoreLocation.framework");
        includeFrameworks.add("AVFoundation.framework");
        includeFrameworks.add("CoreText.framework");
        includeFrameworks.add("CoreVideo.framework");
        includeFrameworks.add("QuickLook.framework");
        //includeFrameworks.add("iAd.framework");
        includeFrameworks.add("CoreMedia.framework");
        includeFrameworks.add("libz.dylib");
        includeFrameworks.add("AVKit.framework");
        if(!addFrameworks.equalsIgnoreCase("none")) {
            includeFrameworks.addAll(Arrays.asList(Util.splitLiteral(addFrameworks, ';')));
        }

        int currentValue = 0xF63EAAA;

        ArrayList<String> arr = new ArrayList<>();
        arr.addAll(includeFrameworks);
        arr.addAll(sourceFiles != null ? Arrays.asList(sourceFiles) : new ArrayList<>());

        for(String file : arr) {
            fileListEntry.append("		0");
            currentValue++;
            String fileOneValue = Integer.toHexString(currentValue).toUpperCase();
            fileListEntry.append(fileOneValue);
            fileListEntry.append("18E9ABBC002F3D1D /* ");
            fileListEntry.append(file);
            fileListEntry.append(" */ = {isa = PBXFileReference; lastKnownFileType = ");
            fileListEntry.append(getFileType(file));
            if(file.endsWith(".framework") || file.endsWith(".dylib") || file.endsWith(".tbd") || file.endsWith(".a")) {
                fileListEntry.append("; name = \"");
                fileListEntry.append(file);
                if(file.endsWith(".dylib") || file.endsWith(".tbd")) {
                    fileListEntry.append("\"; path = \"usr/lib/");
                    fileListEntry.append(file);
                    fileListEntry.append("\"; sourceTree = SDKROOT; };\n");
                } else {
                    if(file.endsWith(".a")) {
                        fileListEntry.append("\"; path = \"");
                        fileListEntry.append(appName);
                        fileListEntry.append("-src/");
                        fileListEntry.append(file);
                        fileListEntry.append("\"; sourceTree = \"<group>\"; };\n");
                    } else {
                        fileListEntry.append("\"; path = System/Library/Frameworks/");
                        fileListEntry.append(file);
                        fileListEntry.append("; sourceTree = SDKROOT; };\n");
                    }
                }
            } else {
                fileListEntry.append("; path = \"");
                if(file.endsWith(".m") || file.endsWith(".S") || file.endsWith(".s") || file.endsWith(".c") || file.endsWith(".cpp") || file.endsWith(".mm") || file.endsWith(".h") ||
                        file.endsWith(".swift") || file.endsWith(".bundle") || file.endsWith(".xcdatamodeld") || file.endsWith(".hh") || file.endsWith(".hpp") || file.endsWith(".xib") ||
                        file.endsWith(".metal")) {
                    fileListEntry.append(file);
                } else {
                    fileListEntry.append(appName);
                    fileListEntry.append("-src/");
                    fileListEntry.append(file);
                }
                fileListEntry.append("\"; sourceTree = \"<group>\"; };\n");
            }
            currentValue++;
            fileOneEntry.append("		0");
            String referenceValue = Integer.toHexString(currentValue).toUpperCase();
            fileOneEntry.append(referenceValue);
            fileOneEntry.append("18E9ABBC002F3D1D /* ");
            fileOneEntry.append(file);
            fileOneEntry.append(" */ = {isa = PBXBuildFile; fileRef = 0");
            fileOneEntry.append(fileOneValue);
            fileOneEntry.append("18E9ABBC002F3D1D /* ");
            fileOneEntry.append(file);
            String injectFileSettings = "";
            if (optionalFrameworks.contains(file)) {
                injectFileSettings += " ATTRIBUTES = (Weak, );";
            }
            String fileSettingsDefault = "settings = {"+injectFileSettings.trim()+" }; ";
            if(noArcFiles.contains(file)) {
                fileOneEntry.append(" */; settings = {COMPILER_FLAGS = \"-fno-objc-arc\";")
                        .append(injectFileSettings)
                        .append(" }; };\n");
            } else {
                fileOneEntry.append(" */;")
                        .append(fileSettingsDefault)
                        .append(" };\n");
            }
            
            if(file.endsWith(".m") || file.endsWith(".S") || file.endsWith(".s") || file.endsWith(".c") || file.endsWith(".cpp") || file.endsWith(".hh") || file.endsWith(".hpp") ||
                    file.endsWith(".swift") || file.endsWith(".mm") || file.endsWith(".h") || file.endsWith(".bundle") || file.endsWith(".xcdatamodeld") || file.endsWith(".xib") ||
                    file.endsWith(".metal")) {
                
                // bundle also needs to be a runtime resource
                if(file.endsWith(".bundle") || file.endsWith(".xcdatamodeld")) {
                    resources.append("\n				0");
                    resources.append(referenceValue);
                    resources.append("18E9ABBC002F3D1D /* ");
                    resources.append(file);
                    resources.append(" */,");                        
                }
                
                fileTwoEntry.append("				0");
                fileTwoEntry.append(fileOneValue);
                fileTwoEntry.append("18E9ABBC002F3D1D /* ");
                fileTwoEntry.append(file);
                fileTwoEntry.append(" */,\n");

                if(!file.endsWith(".h") && !file.endsWith(".hpp") && !file.endsWith(".hh") && !file.endsWith(".bundle")) {
                    fileThreeEntry.append("				0");
                    fileThreeEntry.append(referenceValue);
                    fileThreeEntry.append("18E9ABBC002F3D1D /* ");
                    fileThreeEntry.append(file);
                    fileThreeEntry.append(" */,\n");
                }
            } else {
                if(file.endsWith(".a") || file.endsWith(".framework") || file.endsWith(".dylib") || file.endsWith(".tbd") || (file.endsWith("Info.plist") && !"GoogleService-Info.plist".equals(file)) || file.endsWith(".pch")) {
                    frameworks.append("				0");
                    frameworks.append(referenceValue);
                    frameworks.append("18E9ABBC002F3D1D /* ");
                    frameworks.append(file);
                    frameworks.append(" */,\n");

                    frameworks2.append("				0");
                    frameworks2.append(fileOneValue);
                    frameworks2.append("18E9ABBC002F3D1D /* ");
                    frameworks2.append(file);
                    frameworks2.append(" */,\n");
                } else {
                    // standard resource file
                    resources.append("\n				0");
                    resources.append(referenceValue);
                    resources.append("18E9ABBC002F3D1D /* ");
                    resources.append(file);
                    resources.append(" */,");
                }
            }
        }

        // TARGETED_DEVICE_FAMILY is an iOS idiom. The macOS template never
        // declares one, so there is no placeholder to rewrite and asking the
        // app type about iPhone-vs-iPad is meaningless there.
        if(platform.hasIosDeviceIdioms() && !appType.equalsIgnoreCase("ios")) {
            String devFamily = "TARGETED_DEVICE_FAMILY = \"2\";";
            if(appType.equalsIgnoreCase("iphone")) {
                devFamily = "TARGETED_DEVICE_FAMILY = \"1\";";
            } 
            replaceInFile(projectPbx, "template", appName, "**ACTUAL_FILES**", fileListEntry.toString(),
                    "**FILE_LIST**", fileOneEntry.toString(), "** FILE_LIST_2 **", fileTwoEntry.toString(),
                    "**FILES_3**", fileThreeEntry.toString(), "***FRAMEWORKS***", frameworks.toString(),
                    "***FRAMEWORKS2***", frameworks2.toString(), "TARGETED_DEVICE_FAMILY = \"1,2\";", devFamily,
                    "***RESOURCES***", resources.toString());
        } else {
            replaceInFile(projectPbx, "template", appName, "**ACTUAL_FILES**", fileListEntry.toString(),
                    "**FILE_LIST**", fileOneEntry.toString(), "** FILE_LIST_2 **", fileTwoEntry.toString(),
                    "**FILES_3**", fileThreeEntry.toString(), "***FRAMEWORKS***", frameworks.toString(),
                    "***FRAMEWORKS2***", frameworks2.toString(), "***RESOURCES***", resources.toString());
        }

        String bundleVersion = Util.getProperty("bundleVersionNumber", appVersion);
        replaceInFile(templateInfoPlist, "com.codename1pkg", appPackageName, "${PRODUCT_NAME}", appDisplayName, "VERSION_VALUE", appVersion, "VERSION_BUNDLE_VALUE", bundleVersion);

        // Written to the project root, NOT to srcRoot. srcRoot.list() above feeds the
        // Xcode project, and getFileType() has no case for .txt, so a manifest left in
        // srcRoot would fall through to ***RESOURCES*** and be copied inside the shipped
        // .app. See SourceManifest.
        sourceManifest.write(root);
    }

    private static void writeCmakeProject(File projectRoot, File srcRoot, String appName, String appType) throws IOException {
        File cmakeLists = new File(projectRoot, "CMakeLists.txt");
        String srcRootPath = srcRoot.getAbsolutePath();
        // The native Windows desktop port links the translated runtime into a
        // standalone executable (Direct2D/DirectWrite rendering + Win32 windowing
        // live in the bundled nativeSources). Every other consumer of the clean
        // target embeds the translated code as a library, which stays the default.
        // The native Windows and Linux desktop ports link the translated runtime
        // into a standalone executable (the platform render/windowing layer lives
        // in the bundled nativeSources). Every other consumer of the clean target
        // embeds the translated code as a library, which stays the default.
        boolean windows = "windows".equalsIgnoreCase(appType);
        boolean linux = "linux".equalsIgnoreCase(appType);
        boolean executable = windows || linux;
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(cmakeLists), "UTF-8")) {
            writer.append("cmake_minimum_required(VERSION 3.10)\n");
            // The native Windows port mixes the translated C runtime with a C++
            // layer for the COM APIs that have no C binding (DirectWrite), so the
            // Windows executable enables CXX; the Linux port (GTK/Cairo) is pure C.
            // Both executables embed the app's classpath resources: Windows links a
            // Win32 .rc into its PE resource section (RC language); Linux links a
            // generated .S that .incbin's the resource blobs (ASM language).
            boolean embedResources = (windows && new File(srcRoot, "cn1_resources.rc").isFile())
                    || (linux && new File(srcRoot, "cn1_resources_data.S").isFile());
            // Assembly is driven by what is actually THERE, not by which feature put it
            // there. The resource .S used to be the only one, so the ASM language and its
            // glob were gated on embedResources; the virtual-thread context switch is a
            // second .S, and under that gate the clean target compiled its C half and
            // failed to link on _cn1VirtualThreadSwitch with the source sitting in the
            // same directory.
            boolean hasAsm = false;
            String[] rootFiles = srcRoot.list();
            if (rootFiles != null) {
                for (String f : rootFiles) {
                    if (f.endsWith(".S") || f.endsWith(".s")) {
                        hasAsm = true;
                        break;
                    }
                }
            }
            if (windows) {
                // Windows declares no ASM: MSVC cannot assemble GNU syntax, and the
                // cross-compiled case is handled by an enable_language(ASM) guarded on
                // NOT MSVC further down, once project() has told CMake which it got.
                writer.append("project(").append(appName).append(embedResources
                        ? " LANGUAGES C CXX RC)\n" : " LANGUAGES C CXX)\n");
            } else {
                // Linux and the clean target answer this identically -- assembly is
                // declared when a .S is actually present -- so they share one branch
                // rather than two spelled the same way.
                writer.append("project(").append(appName).append(hasAsm
                        ? " LANGUAGES C ASM)\n" : " LANGUAGES C)\n");
            }
            // C11 for <stdatomic.h> (cn1_globals.h) and _Static_assert (Win32 shim);
            // supported by clang/clang-cl, gcc and Xcode's clang alike.
            writer.append("set(CMAKE_C_STANDARD 11)\n");
            if (windows) {
                // Overridable from the configure line (-DCMAKE_CXX_STANDARD=20): the
                // Widgets Board provider (windows.msix=true) needs C++20 because
                // C++/WinRT at C++17 falls back to <experimental/coroutine>, which the
                // modern MSVC STL rejects under clang-cl.
                writer.append("if(NOT DEFINED CMAKE_CXX_STANDARD)\n");
                writer.append("  set(CMAKE_CXX_STANDARD 17)\n");
                writer.append("endif()\n");
            }
            writer.append("set(CN1_APP_SOURCE_ROOT \"")
                    .append(escapeCmakePath(srcRootPath))
                    .append("\")\n");
            writer.append("include_directories(${CN1_APP_SOURCE_ROOT})\n");
            writer.append("file(GLOB TRANSLATOR_SOURCES \"${CN1_APP_SOURCE_ROOT}/*.c\")\n");
            writer.append("file(GLOB TRANSLATOR_HEADERS \"${CN1_APP_SOURCE_ROOT}/*.h\")\n");
            if (linux) {
                // The Linux executable is otherwise pure C (GTK/Cairo/Pango/GdkPixbuf
                // are C libraries). Two things can put a .S beside it: the generated
                // resource blob (.incbin of each classpath resource, so
                // getResourceAsStream reads straight out of the ELF .rodata) and the
                // virtual-thread context switch. Both are picked up by presence.
                String asmGlob = "";
                if (hasAsm) {
                    writer.append("file(GLOB TRANSLATOR_ASM_SOURCES \"${CN1_APP_SOURCE_ROOT}/*.S\")\n");
                    asmGlob = " ${TRANSLATOR_ASM_SOURCES}";
                }
                writer.append("add_executable(${PROJECT_NAME} ${TRANSLATOR_SOURCES}")
                        .append(asmGlob).append(" ${TRANSLATOR_HEADERS})\n");
                writer.append("target_include_directories(${PROJECT_NAME} PUBLIC ${CN1_APP_SOURCE_ROOT})\n");
                writeLinuxLinkSet(writer);
            } else if (windows) {
                // The port's nativeSources contribute the C++ DirectWrite layer.
                writer.append("file(GLOB TRANSLATOR_CXX_SOURCES \"${CN1_APP_SOURCE_ROOT}/*.cpp\")\n");
                // Assembly is a COMPILER question here, not an app-type one. MSVC cannot
                // assemble GNU syntax, but the Windows app type is also cross-built with
                // clang on a POSIX host, where _WIN32 is undefined, the virtual-thread
                // switch is live, and the link fails without it. CMake knows which one it
                // got only after project() has enabled C, so ask it there rather than
                // guessing from the app type. Under MSVC the variable stays unset and
                // expands to nothing.
                if (hasAsm) {
                    writer.append("if(NOT MSVC)\n");
                    writer.append("    enable_language(ASM)\n");
                    writer.append("    file(GLOB TRANSLATOR_ASM_SOURCES \"${CN1_APP_SOURCE_ROOT}/*.S\")\n");
                    writer.append("endif()\n");
                }
                if (embedResources) {
                    // The resource script compiles to a .res linked into the exe,
                    // putting the app's classpath resources in the PE resource section.
                    writer.append("file(GLOB TRANSLATOR_RC_SOURCES \"${CN1_APP_SOURCE_ROOT}/*.rc\")\n");
                    writer.append("add_executable(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_CXX_SOURCES} ${TRANSLATOR_ASM_SOURCES} ${TRANSLATOR_RC_SOURCES} ${TRANSLATOR_HEADERS})\n");
                } else {
                    writer.append("add_executable(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_CXX_SOURCES} ${TRANSLATOR_ASM_SOURCES} ${TRANSLATOR_HEADERS})\n");
                }
                writer.append("target_include_directories(${PROJECT_NAME} PUBLIC ${CN1_APP_SOURCE_ROOT})\n");
                // Math lives in the CRT under MSVC (no separate libm to link); every
                // other platform needs an explicit libm link for the translated
                // runtime. The Win32 import libs back the Direct2D/DirectWrite
                // rendering, WIC image decode, WinHTTP networking, WinSock raw
                // sockets (ws2_32) and the Win32 windowing/input layer the port's
                // nativeSources call into; they are exercised on the Windows CI
                // legs (windows-latest / windows-11-arm).
                writer.append("if(WIN32)\n");
                // d2d1/dwrite/dxgi/windowscodecs: Direct2D + DirectWrite + WIC render layer.
                // mf*/mfplat/mfuuid: Media Foundation Media Engine (cn1_windows_media.cpp).
                // oleaut32: SysAllocString for the media source URL BSTR.
                // shell32: ShellExecuteW (execute/dial/sendSMS/sendMessage launches).
                // comdlg32: GetOpenFileNameW/GetSaveFileNameW (native file picker).
                // crypt32: CryptProtectData/CryptUnprotectData (DPAPI secure storage).
                // winmm: waveIn audio recording (cn1_windows_audiorec.c).
                // runtimeobject: WinRT activation (RoGetActivationFactory) for the
                //   biometric / location / contacts / share natives (cn1_windows_winrt.cpp).
                //   The WinRT ABI headers + this import lib ship in every Windows SDK the
                //   port builds against, including the xwin-laid-out SDK the Linux
                //   cross-compile uses, so it is linked unconditionally.
                // dbghelp: lets the last-resort unhandled-exception handler symbolize its
                // own native backtrace in-process (SymFromAddr against the /Zi .pdb), so a
                // native crash logs Java/C function names instead of bare RVAs.
                writer.append("    target_link_libraries(${PROJECT_NAME} d2d1 dwrite dxgi windowscodecs winhttp ws2_32 user32 gdi32 ole32 oleaut32 uuid mf mfplat mfreadwrite mfuuid shell32 comdlg32 crypt32 bcrypt ncrypt winmm runtimeobject dbghelp)\n");
                // BrowserComponent is backed by WebView2 (cn1_windows_browser.cpp),
                // gated on the SDK being present: when WEBVIEW2_SDK_DIR points at a
                // Microsoft.Web.WebView2 build/native folder we link the static
                // loader (arch-specific) and define CN1_HAVE_WEBVIEW2; otherwise the
                // browser natives compile as stubs and the port reports the browser
                // as unsupported. version/shell32/advapi32/shlwapi back the loader.
                writer.append("    if(DEFINED ENV{WEBVIEW2_SDK_DIR} AND EXISTS \"$ENV{WEBVIEW2_SDK_DIR}/include/WebView2.h\")\n");
                writer.append("        target_include_directories(${PROJECT_NAME} PRIVATE \"$ENV{WEBVIEW2_SDK_DIR}/include\")\n");
                writer.append("        target_compile_definitions(${PROJECT_NAME} PRIVATE CN1_HAVE_WEBVIEW2=1)\n");
                // VsDevCmd sets VSCMD_ARG_TGT_ARCH to the *target* arch (arm64/x64),
                // which is what we must match for the static loader -- the host-based
                // CMAKE_SYSTEM_PROCESSOR can disagree on the arm64 dev VM.
                writer.append("        if(\"$ENV{VSCMD_ARG_TGT_ARCH}\" STREQUAL \"arm64\" OR CMAKE_SYSTEM_PROCESSOR MATCHES \"[Aa][Rr][Mm]64|aarch64\")\n");
                writer.append("            target_link_libraries(${PROJECT_NAME} \"$ENV{WEBVIEW2_SDK_DIR}/arm64/WebView2LoaderStatic.lib\")\n");
                writer.append("        else()\n");
                writer.append("            target_link_libraries(${PROJECT_NAME} \"$ENV{WEBVIEW2_SDK_DIR}/x64/WebView2LoaderStatic.lib\")\n");
                writer.append("        endif()\n");
                writer.append("        target_link_libraries(${PROJECT_NAME} shlwapi version shell32 advapi32)\n");
                writer.append("    endif()\n");
                // Debug info always goes to a SEPARATE .pdb (clang-cl /Zi + linker
                // /DEBUG), never embedded in the exe -- so native crash addresses
                // symbolize to function names (llvm-symbolizer) from the .pdb while
                // the shipped exe stays lean. Optimizations stay on for the shipping
                // Release build: /O2 from the Release config plus the linker
                // dead-stripping unreferenced functions (/OPT:REF) and folding
                // identical COMDATs (/OPT:ICF). /OPT:REF/ICF are re-stated for
                // Release because /DEBUG turns them off by default. (Unlike Linux's
                // removable .eh_frame, the x64 .pdata/.xdata unwind tables are part
                // of the Windows ABI and must stay in the image.)
                writer.append("    target_compile_options(${PROJECT_NAME} PRIVATE /Zi)\n");
                writer.append("    if(CMAKE_BUILD_TYPE STREQUAL \"Debug\" OR CMAKE_BUILD_TYPE STREQUAL \"RelWithDebInfo\")\n");
                writer.append("        target_link_options(${PROJECT_NAME} PRIVATE /DEBUG)\n");
                writer.append("    else()\n");
                writer.append("        target_link_options(${PROJECT_NAME} PRIVATE /DEBUG /OPT:REF /OPT:ICF)\n");
                writer.append("    endif()\n");
                // GUI subsystem so double-clicking the exe does not pop a console
                // window; keep main() as the entry via mainCRTStartup. The app still
                // writes to a parent console when launched from cmd (initDisplay calls
                // AttachConsole(ATTACH_PARENT_PROCESS)), and a redirected stdout pipe
                // (the screenshot CI harness) is inherited regardless of subsystem.
                writer.append("    target_link_options(${PROJECT_NAME} PRIVATE /SUBSYSTEM:WINDOWS /ENTRY:mainCRTStartup)\n");
                writer.append("else()\n");
                writer.append("    target_link_libraries(${PROJECT_NAME} m)\n");
                writer.append("endif()\n");
            } else {
                String asmGlob = "";
                if (hasAsm) {
                    writer.append("file(GLOB TRANSLATOR_ASM_SOURCES \"${CN1_APP_SOURCE_ROOT}/*.S\")\n");
                    asmGlob = " ${TRANSLATOR_ASM_SOURCES}";
                }
                writer.append("add_library(${PROJECT_NAME} ${TRANSLATOR_SOURCES}")
                        .append(asmGlob).append(" ${TRANSLATOR_HEADERS})\n");
                writer.append("target_include_directories(${PROJECT_NAME} PUBLIC ${CN1_APP_SOURCE_ROOT})\n");
            }

            // JAVA SEMANTICS FLAGS -- required for correctness, every target:
            // -fwrapv: Java int/long arithmetic is DEFINED to wrap on overflow;
            //   in C signed overflow is UB and clang -O3 provably exploits it
            //   (observed: `long += int + int` fused into a 64-bit add, skipping
            //   the Java-mandated 32-bit wrap -- checksum diverged by 2^32 per
            //   overflow). The Xcode template sets the same via OTHER_CFLAGS.
            // -fno-strict-aliasing: the generated code accesses one allocation
            //   through JavaObjectPrototype, JavaArrayPrototype and obj__<class>
            //   simultaneously; TBAA is unsound for it.
            // -fno-builtin-fmod(f): on Darwin/-fno-math-errno clang treats fmod as
            //   pure and IF-CONVERTS rarely-taken clamp guards into branchless
            //   selects that SPECULATE a full libm fmod call every iteration
            //   (measured 1.7x slowdown on a transcendental loop). Dropping just
            //   fmod's builtin status keeps the guard a branch; sqrt/sin/cos keep
            //   their intrinsics.
            // clang-cl (the Windows toolchain) takes the GNU spellings via /clang:.
            writer.append("if(MSVC)\n");
            writer.append("    target_compile_options(${PROJECT_NAME} PRIVATE /clang:-fwrapv /clang:-fno-strict-aliasing /clang:-fno-builtin-fmod /clang:-fno-builtin-fmodf)\n");
            writer.append("else()\n");
            writer.append("    target_compile_options(${PROJECT_NAME} PRIVATE -fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf)\n");
            writer.append("endif()\n");
            if (executable && !windows) {
                // ThinLTO for the Release Linux executable: the translator emits one
                // C function per Java method, so cross-TU inlining is where the
                // remaining call overhead lives (measured: call-heavy benchmarks up
                // to 1.6x faster; thin backend runs parallel, wall-time ~neutral).
                // Executable-only: a static LIBRARY full of LLVM bitcode would break
                // consumers whose final link is not LTO-aware, and clang-cl/lld-link
                // on Windows is a separate follow-up. The iOS Xcode template enables
                // the same via LLVM_LTO=YES_THIN on its optimized configurations.
                // -flto=thin is CLANG syntax; gcc (Alpine/musl default) rejects it.
                // Plain gcc -flto is deliberately NOT substituted -- untested here.
                writer.append("if(CMAKE_C_COMPILER_ID MATCHES \"Clang\")\n");
                writer.append("    target_compile_options(${PROJECT_NAME} PRIVATE $<$<CONFIG:Release>:-flto=thin>)\n");
                writer.append("    target_link_options(${PROJECT_NAME} PRIVATE $<$<CONFIG:Release>:-flto=thin>)\n");
                writer.append("endif()\n");
            }

            // Opt-in Link-Time Optimization. The translator emits a separate C
            // function per reachable Java method; LTO lets the C compiler inline
            // the tiny per-call frame helpers and the array-access inline helpers
            // across translation units, which is where most of the AOT call/array
            // overhead hides. It considerably increases link time, so it is OFF by
            // default and must stay out of regular CI -- enable with
            // -DCN1_ENABLE_LTO=ON only for release/perf builds. Uses CMake's
            // INTERPROCEDURAL_OPTIMIZATION so it maps to -flto (clang/gcc) or
            // /LTCG (MSVC) per toolchain; ${PROJECT_NAME} exists in every branch above.
            writer.append("option(CN1_ENABLE_LTO \"Enable Link-Time Optimization (slow link; off in CI)\" OFF)\n");
            writer.append("if(CN1_ENABLE_LTO)\n");
            writer.append("    include(CheckIPOSupported)\n");
            writer.append("    check_ipo_supported(RESULT CN1_IPO_OK OUTPUT CN1_IPO_MSG)\n");
            writer.append("    if(CN1_IPO_OK)\n");
            writer.append("        set_property(TARGET ${PROJECT_NAME} PROPERTY INTERPROCEDURAL_OPTIMIZATION TRUE)\n");
            writer.append("    else()\n");
            writer.append("        message(WARNING \"CN1_ENABLE_LTO requested but IPO/LTO unsupported: ${CN1_IPO_MSG}\")\n");
            writer.append("    endif()\n");
            writer.append("endif()\n");
        }
    }

    /**
     * Emits the Linux (GTK3/Cairo) executable link set into the generated
     * CMakeLists. The whole stack is required: the render/widget layer (GTK3 +
     * Cairo + Pango + GdkPixbuf + GLib/GIO), HTTP (libcurl), and the capability
     * libraries the port's nativeSources include unconditionally -- GStreamer
     * (media/camera/audio), WebKitGTK (browser), libsecret (secure storage),
     * libnotify (notifications), GeoClue (location) and epoxy/EGL/GLES (the 3D
     * backend). These are all standard distribution {@code -dev} packages; a build
     * host installs them once (see the developer guide). The musl toolchain is
     * supplied by the builder's CMAKE_C_COMPILER (zig cc / musl-gcc), so nothing
     * musl-specific is emitted here.
     */
    private static void writeLinuxLinkSet(Writer writer) throws IOException {
        writer.append("find_package(PkgConfig REQUIRED)\n");
        // Core GTK3 stack -- LINKED. The shipped binary requires only these (plus
        // libc/libm/pthread/dl) at runtime. Splitting the GLES probe out keeps a
        // clear error if only the GL bits are missing.
        writer.append("pkg_check_modules(CN1DEPS REQUIRED\n");
        writer.append("    gtk+-3.0 cairo pango pangocairo gdk-pixbuf-2.0 glib-2.0 gobject-2.0 gio-2.0\n");
        writer.append("    fontconfig freetype2\n");
        // libcrypto backs the port's crypto bridge (cn1_linux_crypto.c). It is
        // already present wherever libcurl is: the OpenSSL-flavoured curl links
        // it, and the -dev package pulls in the headers.
        writer.append("    libcurl libcrypto)\n");
        writer.append("pkg_check_modules(CN1GL REQUIRED epoxy egl glesv2)\n");
        // Optional feature libs (browser/media/secure-storage/notifications/
        // location). The port dlopen()s these lazily at first use (see
        // cn1_linux_browser.c / cn1_linux_media.c / cn1_linux_services.c), so we
        // use their headers at compile time but do NOT link them -- the shipped
        // binary carries no DT_NEEDED for webkit2gtk/gstreamer/etc., and a desktop
        // without them still runs every non-optional feature (the optional one
        // reports unsupported). Probed REQUIRED so the *build* host has the headers.
        writer.append("pkg_check_modules(CN1OPT REQUIRED\n");
        writer.append("    gstreamer-1.0 gstreamer-app-1.0 gstreamer-video-1.0\n");
        writer.append("    webkit2gtk-4.1 libsecret-1 libnotify libgeoclue-2.0)\n");
        writer.append("target_include_directories(${PROJECT_NAME} PRIVATE ${CN1DEPS_INCLUDE_DIRS} ${CN1GL_INCLUDE_DIRS} ${CN1OPT_INCLUDE_DIRS})\n");
        writer.append("target_compile_options(${PROJECT_NAME} PRIVATE ${CN1DEPS_CFLAGS_OTHER} ${CN1GL_CFLAGS_OTHER} ${CN1OPT_CFLAGS_OTHER})\n");
        // libm/pthread back the translated runtime + GC; dl is required for the
        // dlopen() of the optional libs above. Note: CN1OPT_LIBRARIES is
        // deliberately NOT linked.
        writer.append("target_link_libraries(${PROJECT_NAME} ${CN1DEPS_LIBRARIES} ${CN1GL_LIBRARIES} m pthread dl)\n");

        // Dead-strip unreferenced functions/data, matching the Windows port's
        // /OPT:REF + /OPT:ICF. The translator emits a C function per reachable
        // method but the linker keeps ALL of them by default on ELF, so without
        // this the binary was ~2.4x the equivalent Windows exe. -ffunction-sections
        // /-fdata-sections puts each in its own section and --gc-sections drops the
        // ones nothing references (virtual targets stay live via the vtables that
        // reference them, exactly as on Windows).
        writer.append("target_compile_options(${PROJECT_NAME} PRIVATE -ffunction-sections -fdata-sections)\n");
        writer.append("target_link_options(${PROJECT_NAME} PRIVATE -Wl,--gc-sections)\n");
        // ParparVM uses setjmp/longjmp for exceptions, not the C++ unwinder, so the
        // DWARF asynchronous-unwind tables (.eh_frame/.eh_frame_hdr) are dead weight
        // at runtime (they were ~30% of the binary). Drop them, and instead emit
        // debug info that, for a shipping build, is SPLIT into a separate
        // <exe>.debug companion: the shipped executable carries no symbol table,
        // debug info or .eh_frame, while crashes still symbolize from the .debug
        // file (function name = the mangled Java method, plus generated-C lines).
        // -g1 is lines + function names and NOTHING about variables or types, which is
        // why a post-mortem of a crash from this build answers "No locals", "No
        // arguments" and "unknown type" for every global -- the core is fine, the DWARF
        // simply does not describe the data. That is the right default for a shipping
        // binary, where the companion exists to turn an address back into a Java method.
        // It is the wrong one for a CI build whose whole job is to be autopsied, so the
        // level is a cache variable: unset it and nothing changes for anybody.
        writer.append("set(CN1_DEBUG_INFO_LEVEL \"1\" CACHE STRING\n");
        writer.append("    \"DWARF level for the <exe>.debug companion: 1 = lines + function names (lean, the default), 3 = full variable and type information (autopsyable)\")\n");
        writer.append("target_compile_options(${PROJECT_NAME} PRIVATE -g${CN1_DEBUG_INFO_LEVEL} -fno-asynchronous-unwind-tables -fno-unwind-tables)\n");
        writer.append("if(NOT (CMAKE_BUILD_TYPE STREQUAL \"Debug\" OR CMAKE_BUILD_TYPE STREQUAL \"RelWithDebInfo\"))\n");
        writer.append("    find_program(CN1_OBJCOPY NAMES objcopy llvm-objcopy gobjcopy)\n");
        writer.append("    if(CN1_OBJCOPY)\n");
        writer.append("        add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD\n");
        writer.append("            COMMAND ${CN1_OBJCOPY} --only-keep-debug $<TARGET_FILE:${PROJECT_NAME}> $<TARGET_FILE:${PROJECT_NAME}>.debug\n");
        writer.append("            COMMAND ${CN1_OBJCOPY} --strip-all $<TARGET_FILE:${PROJECT_NAME}>\n");
        writer.append("            COMMAND ${CN1_OBJCOPY} --add-gnu-debuglink=$<TARGET_FILE:${PROJECT_NAME}>.debug $<TARGET_FILE:${PROJECT_NAME}>\n");
        writer.append("            COMMENT \"Splitting debug/symbol info into a separate <exe>.debug companion (symbolize crashes with it; the shipped binary stays lean)\")\n");
        writer.append("    else()\n");
        writer.append("        target_link_options(${PROJECT_NAME} PRIVATE -s)\n");
        writer.append("    endif()\n");
        writer.append("endif()\n");
    }

    private static String escapeCmakePath(String path) {
        // Use forward slashes, which CMake accepts on every platform (including
        // Windows). Emitting backslashes would make CMake treat sequences like
        // "C:\Users" as invalid string escapes ('\U') when the globbed paths are
        // expanded into add_executable/add_library.
        return path.replace("\\", "/");
    }
    
    private static String getFileType(String s) {
        if(s.endsWith(".framework")) {
            return "wrapper.framework";
        }
        if(s.endsWith(".a")) {
            return "archive.ar";
        }
        if(s.endsWith(".dylib")) {
            return "compiled.mach-o.dylib";
        }
        if(s.endsWith(".tbd")) {
            return "sourcecode.text-based-dylib-definition";
        }
        if(s.endsWith(".h")) {
            return "sourcecode.c.h";
        }
        if(s.endsWith(".pch")) {
            return "sourcecode.c.objc.preprocessed";
        }
        if(s.endsWith(".hh") || s.endsWith(".hpp")) {
            return "sourcecode.cpp.h";
        }
        if(s.endsWith(".plist")) {
            return "text.plist.xml";
        } 
        if(s.endsWith(".bundle") || s.endsWith("xcdatamodeld")) {
            return "wrapper.plug-in";
        }
        if(s.endsWith(".m") || s.endsWith(".c")) {
            return "sourcecode.c.objc";
        }
        // Assembly. An extension Xcode does not recognise becomes
        // `lastKnownFileType = file`, which lands the file in the RESOURCES phase: it
        // ships into the bundle and is never assembled, so the link fails naming a
        // symbol whose source is sitting right there in the project.
        //
        // sourcecode.asm is the identifier to use for BOTH spellings. Xcode's
        // StandardFileTypes.xcspec lists it as `Extensions = (s)` with
        // `GccDialectName = assembler-with-cpp`, so it runs the preprocessor -- which
        // the capability gate in cn1_virtual_thread_asm.S needs. .S is not in any
        // Extensions list of its own, and the neighbouring sourcecode.asm.asm is for
        // .asm, not for it.
        if(s.endsWith(".S") || s.endsWith(".s")) {
            return "sourcecode.asm";
        }
        if(s.endsWith(".xcassets")) {
            return "folder.assetcatalog";
        }
        if(s.endsWith(".mm") || s.endsWith(".cpp")) {
            return "sourcecode.cpp.objc";
        }
        if(s.endsWith(".swift")) {
            return "sourcecode.swift";
        }
        if(s.endsWith(".metal")) {
            return "sourcecode.metal";
        }
        if(s.endsWith(".xib")) {
            return "file.xib";
        }
        if(s.endsWith(".res") || s.endsWith(".ttf") ) {
            return "file";
        }
        if(s.endsWith(".png")) {
            return "image.png";
        }
        if(s.endsWith(".strings")) {
            return "text.plist.strings";
        }
        return "file";
    }
    //
    // Be kind to the GC; read as a StringBuilder, a data type designed
    // to be mutated. Also, expire the temporary byte[] buffer so it can
    // be collected.
    //
    private static String readFileAsString(File sourceFile) throws IOException
    {
        try(DataInputStream dis = new DataInputStream(new FileInputStream(sourceFile))) {
            byte[] data = new byte[(int) sourceFile.length()];
            dis.readFully(data);
            return new String(data, StandardCharsets.UTF_8);
        }
    }
    //
    // Use more appropriate data
    // structures, minimizing gc thrashing.  This avoids a big
    // spike in memory and gc usage (and corresponding build 
    // failures due to OutOfMemoryError) at the very end of the build 
    // process for large projects.  
    //
    private static void replaceInFile(File sourceFile, String... values) throws IOException {
        // A String rather than a StringBuilder because the translator has to compile
        // against ParparVM's own JavaAPI in order to translate itself, and
        // StringBuilder there has neither indexOf nor replace.
        //
        // One pass per target, appending into a fresh builder. The obvious
        // translation of the old in-place edit -- indexOf on str.toString(), then
        // substring/concat the whole buffer back together per match -- copies the
        // ENTIRE file twice for every occurrence, which is the opposite of this
        // method's purpose: it exists to avoid the memory spike that made large
        // Xcode project.pbxproj rewrites fail with OutOfMemoryError. Each target
        // now costs one traversal and one output buffer regardless of how many
        // times it matches.
        String str = readFileAsString(sourceFile);
        int totchanges = 0;

        for (int iter = 0; iter < values.length; iter += 2) {
            String target = values[iter];
            String replacement = values[iter + 1];
            int index = str.indexOf(target);
            if (index < 0) {
                continue;
            }
            StringBuilder out = new StringBuilder(str.length() + 64);
            int from = 0;
            while (index >= 0) {
                out.append(str, from, index).append(replacement);
                from = index + target.length();
                totchanges++;
                index = str.indexOf(target, from);
            }
            out.append(str, from, str.length());
            str = out.toString();
        }

        //
        // don't start the output file until all the processing is done
        //
        if(verbose) {
            System.out.println("Rewrite " + sourceFile + " with " + totchanges + " changes");
        }
        try(Writer fios = new OutputStreamWriter(new FileOutputStream(sourceFile), "UTF-8")) {
            fios.write(str);
        }
    }
    

    

    /**
     * Build descriptors that ride along inside an application jar's
     * {@code META-INF} but are not application resources. Every non-class input
     * file is copied into the output by basename, so without this filter a
     * Maven-built app leaks its pom.xml (dependency list and all),
     * pom.properties and MANIFEST.MF into the bundle -- and for the JavaScript
     * target that bundle IS a public document root.
     *
     * <p>The check is path-aware on purpose: an app is free to ship a resource
     * of its own that happens to be called pom.xml, and only the copies living
     * under META-INF are jar metadata.
     */
    private static boolean isBuildMetadata(File f) {
        String name = f.getName();
        if (!"pom.xml".equals(name)
                && !"pom.properties".equals(name)
                && !"MANIFEST.MF".equals(name)) {
            return false;
        }
        for (File parent = f.getParentFile(); parent != null; parent = parent.getParentFile()) {
            if ("META-INF".equals(parent.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copy the input stream into the output stream, closes both streams when finishing or in
     *  the case of an exception
     * 
     * @param i source
     * @param o destination
     */
    /**
     * Emit the virtual-thread runtime beside the generated sources.
     *
     * Three files rather than one because the switch has to be assembly: glibc
     * aborts a cross-stack longjmp under _FORTIFY_SOURCE and musl has no
     * makecontext, so neither portable route survives every target we ship.
     */
    private static void emitVirtualThreadRuntime(File srcRoot) throws IOException {
        String[] names = { "cn1_virtual_thread.h", "cn1_virtual_thread.c", "cn1_virtual_thread_asm.S" };
        for (String name : names) {
            InputStream in = ByteCodeTranslator.class.getResourceAsStream("/" + name);
            if (in == null) {
                // Missing here means the build did not stage it; failing now names the
                // cause, where the link error later names only a symbol.
                throw new IOException("virtual-thread runtime resource missing: " + name);
            }
            copy(in, new FileOutputStream(new File(srcRoot, name)));
            sourceManifest.recordRuntime(name, "/" + name);
        }
    }

    public static void copy(InputStream i, OutputStream o) throws IOException {
        copy(i, o, 8192);
    }

    /**
     * Copy the input stream into the output stream, closes both streams when finishing or in
     * the case of an exception
     *
     * @param i source
     * @param o destination
     * @param bufferSize the size of the buffer, which should be a power of 2 large enoguh
     */
    public static void copy(InputStream i, OutputStream o, int bufferSize) throws IOException {
        try {
            byte[] buffer = new byte[bufferSize];
            int size = i.read(buffer);
            while(size > -1) {
                o.write(buffer, 0, size);
                size = i.read(buffer);
            }
        } finally {
            cleanup(o);
            cleanup(i);
        }
    }

    /**
     * Closes the object (connection, stream, etc.) without throwing any exception, even if the
     * object is null
     *
     * @param o Connection, Stream or another closeable object
     */
    public static void cleanup(Object o) {
        try {
            if(o instanceof OutputStream) {
                ((OutputStream)o).close();
                return;
            }
            if(o instanceof InputStream) {
                ((InputStream)o).close();
            }
        } catch(IOException err) {
            err.printStackTrace();
        }
    }
}
