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
package com.codename1.builders;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Builds a native macOS (AppKit) application.
 *
 * <p>This is a peer of {@link IPhoneBuilder} rather than a delegate of it. Mac
 * Catalyst is a slice of the iOS Xcode project and is built by handing
 * {@code IPhoneBuilder} one extra hint; the AppKit port is a different SDK, a
 * different UI framework and a different project shape, and it wants none of the
 * iOS project geometry -- no app extensions, no {@code SUPPORTS_MACCATALYST}, no
 * multi-scene manifest, no Ruby {@code xcodeproj} overlay.</p>
 *
 * <p>What it does share is the Apple toolchain: the translator, xcodebuild,
 * codesign and notarization. Those live on {@link Executor} and are used here
 * directly.</p>
 *
 * <p>The project itself is emitted by the translator's {@code macos} output
 * type, which is the same pipeline as iOS parameterised by four things -- the
 * pbxproj template, the Info.plist, the framework list and the asset catalog.
 * The plists and entitlements this builder writes over the top come from
 * {@link MacOSXcodeProject}, which is deliberately free of any Xcode dependency
 * so the generation is unit-testable on a machine with no developer tools.</p>
 *
 * @author Shai Almog
 */
public class MacOSNativeBuilder extends Executor {

    private File resultDir;
    private File appBundle;
    private File xcodeProjectDir;
    private final List<File> artifacts = new ArrayList<File>();
    /// The scan result and the dlopen flag, kept because the entitlements are
    /// written once per signing channel while the scan is worth doing once.
    private MacOSXcodeProject.MacOSCapabilities capabilities;
    private boolean loadsExternalCode;

    /** The produced {@code .app} bundle, or {@code null} if the build failed. */
    public File getAppBundle() {
        return appBundle;
    }

    /**
     * Everything the build produced, in the order it was produced: each channel's
     * {@code .app} followed by the dmg and/or pkg built from it.
     */
    public List<File> getArtifacts() {
        return artifacts;
    }

    /** The directory holding the build output. */
    public File getResultDir() {
        return resultDir;
    }

    /**
     * The directory holding the generated Xcode project, for the
     * {@code mac-source} target which stops after generating it.
     *
     * <p>The directory rather than the {@code .xcodeproj} bundle: the sources
     * the project references sit beside it, so handing back the bundle alone
     * gives a project that opens and cannot build.</p>
     */
    public File getXcodeProjectDir() {
        return xcodeProjectDir;
    }

    @Override
    protected String getDeviceIdCode() {
        return "\"\"";
    }

    @Override
    protected String hardeningPlatform(BuildRequest request) {
        return "mac";
    }

    // The generated XxxImplCodenameOne carries the native methods; a
    // PeerComponent-returning native interface method is bridged through the
    // native view pointer, exactly as the iOS builder does, because the peer
    // machinery on this port is the shared Apple one.
    // Same suffix the iOS builder uses, so a project's Objective-C native
    // interface implementation is found under the same name on both.
    @Override
    protected String getImplSuffix() {
        return "ImplCodenameOne";
    }

    @Override
    protected String nativeInterfaceFrameworkImports() {
        return "#import \"CodenameOne_GLViewController.h\"\n"
                + "#import <AppKit/AppKit.h>\n";
    }

    @Override
    protected String generatePeerComponentCreationCode(String methodCallString) {
        return "PeerComponent.create(new long[] {" + methodCallString + "})";
    }

    @Override
    protected String convertPeerComponentToNative(String param) {
        return "((long[])" + param + ".getNativePeer())[0]";
    }

    @Override
    public boolean build(File sourceZip, BuildRequest request) throws BuildException {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            throw new BuildException("A native macOS application can only be built on a Mac: "
                    + "the build runs xcodebuild against the macosx SDK.");
        }

        final MacOSBuildHints hints = new MacOSBuildHints();
        hints.parse(new MacOSBuildHints.HintSource() {
            @Override
            public String get(String key, String defaultValue) {
                return request.getArg(key, defaultValue);
            }
        }, request.getPackageName());
        for (String warning : hints.getWarnings()) {
            log(warning);
        }

        File tmpFile = getBuildDirectory();
        tmpFile.mkdirs();
        File classesDir = new File(tmpFile, "classes");
        File resDir = new File(tmpFile, "res");
        File buildinRes = new File(tmpFile, "btres");
        // Emptied, not merely created. These paths are stable across builds and
        // unzip() only overwrites the entries the archive carries, so on a
        // rebuild without `mvn clean` a class or resource deleted since the last
        // build stays here -- and is then scanned for permissions and crypto
        // usage, and translated into the app. Removed code that keeps shipping,
        // and an entitlement the application no longer justifies, are both worse
        // than a slower build.
        try {
            deleteRecursively(classesDir);
            deleteRecursively(resDir);
            deleteRecursively(buildinRes);
        } catch (IOException ex) {
            throw new BuildException("Failed to clear the staged build inputs", ex);
        }
        classesDir.mkdirs();
        resDir.mkdirs();
        buildinRes.mkdirs();

        try {
            unzip(sourceZip, classesDir, resDir, resDir, buildinRes);
        } catch (Exception ex) {
            throw new BuildException("Failed to unzip the application sources", ex);
        }

        // The JDK class set the translator emits as java_lang_*.m/.h. Shipped
        // separately and unzipped into a translator source root; without it
        // cn1_globals.c references java_lang_Class.h and no such header is
        // produced. Same wire-up as every other ParparVM target.
        try {
            unzip(getResourceAsStream("/parparvm-java-api.jar"), classesDir, classesDir, classesDir);
        } catch (Exception ex) {
            throw new BuildException("Failed to load JavaAPI.jar", ex);
        }

        File parparVMCompilerJar;
        File portClasses;
        File nativeSources;
        try {
            parparVMCompilerJar = getResourceAsFile("/parparvm-compiler.jar", ".jar");
            File portDir = new File(tmpFile, "macPort");
            portClasses = new File(portDir, "classes");
            nativeSources = new File(portDir, "nativeSources");
            portClasses.mkdirs();
            nativeSources.mkdirs();
            // Provided by the codenameone-mac 'bundle' artifact on the plugin
            // classpath. nativemac.jar is the materialised native set -- the
            // MacPort sources plus the shared iOSPort ones the exclusion
            // manifest keeps -- so what is staged here is exactly what clang
            // sees, and exactly what the offline signature gate verifies.
            extractJarResource("/MacPort.jar", portClasses);
            extractJarResource("/nativemac.jar", nativeSources);
        } catch (Exception ex) {
            throw new BuildException("Failed to stage the MacPort native layer. The codenameone "
                    + "maven plugin must provide the codenameone-mac 'bundle' artifact "
                    + "(MacPort.jar + nativemac.jar) on its classpath.", ex);
        }

        DatabaseUsage databaseUsage;
        try {
            databaseUsage = scanForDatabaseUsage(classesDir).merge(scanForDatabaseUsage(buildinRes));
        } catch (IOException ex) {
            throw new BuildException("Failed to scan for database usage", ex);
        }

        // The crypto primitives are compiled in only for an application that
        // references them, as on iOS -- an application that does not never
        // references a CommonCrypto or Security symbol.
        //
        // This is not an optimisation to skip. The stubs the toggle leaves in
        // place return unsupported for the ciphers, but secureRandomBytes just
        // leaves the caller's buffer alone: two calls then agree, because both
        // read back the zeroes that were already there. A random source that
        // silently returns a constant is the worst possible failure, and it is
        // what an unconfigured build ships.
        final boolean[] usesCrypto = {false};
        try {
            scanClassesForPermissions(classesDir, new CryptoScanner(usesCrypto));
        } catch (IOException ex) {
            throw new BuildException("Failed to scan the application for crypto usage", ex);
        }
        if (usesCrypto[0]) {
            // In the staged native tree, not in the resources: that tree is what
            // the translator copies into the Xcode project, and it is the copy
            // clang reads. The iOS builder edits its resources directory because
            // that is where it unzips the port's natives; here they are their own
            // translator source root.
            File cn1Crypto = new File(nativeSources, "CN1Crypto.h");
            if (!cn1Crypto.exists()) {
                // Not skipped quietly. The stub secureRandomBytes leaves the
                // caller's buffer untouched, so the failure is a random source
                // that returns a constant -- which nothing downstream can detect.
                throw new BuildException("The application uses com.codename1.security but "
                        + "CN1Crypto.h is missing from the staged native sources at "
                        + cn1Crypto.getAbsolutePath());
            }
            try {
                replaceInFile(cn1Crypto, "//#define CN1_INCLUDE_CRYPTO", "#define CN1_INCLUDE_CRYPTO");
                if ("true".equalsIgnoreCase(request.getArg("macos.crypto.gcm",
                        request.getArg("ios.crypto.gcm", "false")))) {
                    replaceInFile(cn1Crypto, "//#define CN1_INCLUDE_CRYPTO_GCM",
                            "#define CN1_INCLUDE_CRYPTO_GCM");
                }
            } catch (Exception ex) {
                throw new BuildException("Failed to configure CN1Crypto.h", ex);
            }
        }
        log("Crypto API " + (usesCrypto[0] ? "enabled" : "disabled"));

        // The application's entry point. A Codename One main class is a
        // Lifecycle subclass with no main(String[]), and the translator refuses
        // a class set with no main at all -- so the stub is what makes the
        // application translatable, not a convenience.
        File stubSource = new File(tmpFile, "stubSource");
        // Emptied rather than merged into: the build directory survives between
        // runs, and a stub left over from a previous one is compiled alongside
        // the new one. Two classes with the same name is a compile error at
        // best, and at worst the translator finds two entry points.
        deleteRecursive(stubSource);
        stubSource.mkdirs();
        try {
            writeStub(request, stubSource, classesDir);
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to generate the application entry point", ex);
        }
        // The native-interface bridges, written beside the stub so the one
        // javac pass below compiles them together with it.
        generateNativeInterfaceBindings(stubSource, resDir);
        if (!compileStub(stubSource, classesDir, portClasses)) {
            return false;
        }

        String version = request.getVersion() != null ? request.getVersion() : "1.0";
        File translatedOut = new File(tmpFile, "translated");
        // Emptied for the same reason the stub source is: the build directory
        // survives between runs, and a project generated under a previous app
        // name would be collected alongside the current one.
        deleteRecursive(translatedOut);
        translatedOut.mkdirs();

        List<String> parparCmd = new ArrayList<String>();
        parparCmd.add("java");
        List<String> translatorOpts = TranslatorHeap.extraJvmOptions();
        parparCmd.addAll(translatorOpts);
        boolean heapOverridden = TranslatorHeap.specifiesHeap(translatorOpts);
        int heapMB = TranslatorHeap.maxHeapMB(2048);
        if (!heapOverridden) {
            parparCmd.add("-Xmx" + heapMB + "m");
        }
        // Without this the translated java.io.File overwrites the port's native
        // java_io_File.m, because both want the same file name in srcRoot. Every
        // Apple target sets it.
        parparCmd.add("-DconcatenateFiles=true");
        parparCmd.add("-Dcn1.sqlite=" + databaseUsage.usesDatabase());
        parparCmd.add("-Dcn1.sqlcipher=" + databaseUsage.usesDatabaseCipher());
        NativeVerifyOption.addTo(parparCmd, request, "mac");
        parparCmd.add("-jar");
        parparCmd.add(parparVMCompilerJar.getAbsolutePath());
        // The macos output type: Objective-C plus an AppKit Xcode project, and
        // the "mac" app type below binds CodenameOneImplementation to its
        // @Concrete mac() target (MacImplementation) during translation. Without
        // that binding every devirtualized call lands on IOSImplementation and
        // the port is inert -- a green build with nothing of it running.
        parparCmd.add("macos");
        parparCmd.add(join(";", classesDir, portClasses, resDir, buildinRes, nativeSources));
        parparCmd.add(translatedOut.getAbsolutePath());
        // The application's name, not the stub's: it names the Xcode project and
        // the bundle. The stub is found as the entry point because it is the one
        // class with a main, which is how every ParparVM target works.
        parparCmd.add(request.getMainClass());
        parparCmd.add(request.getPackageName());
        parparCmd.add(request.getDisplayName());
        parparCmd.add(version);
        parparCmd.add("mac");
        parparCmd.add("none");
        try {
            int outputMark = message.length();
            if (!exec(tmpFile, 900000, parparCmd.toArray(new String[0]))) {
                if (!heapOverridden && TranslatorHeap.looksOutOfMemory(message.substring(outputMark))) {
                    error(TranslatorHeap.outOfMemoryAdvice(heapMB, true), null);
                }
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Failure while running the ParparVM translator (macos target)", ex);
        }

        File distDir = new File(translatedOut, "dist");
        String appName = request.getMainClass();
        File xcodeproj = new File(distDir, appName + ".xcodeproj");
        File srcRoot = new File(distDir, appName + "-src");
        if (!xcodeproj.exists()) {
            throw new BuildException("Translator did not emit an Xcode project at "
                    + xcodeproj.getAbsolutePath());
        }
        xcodeProjectDir = distDir;

        String bundleVersion = request.getArg("macos.bundleVersion",
                request.getArg("ios.bundleVersion", version));
        try {
            writeGeneratedPlists(request, hints, srcRoot, appName, version, bundleVersion, classesDir);
        } catch (IOException ex) {
            throw new BuildException("Failed to write the macOS bundle metadata", ex);
        }

        resultDir = new File(tmpFile, "result");
        resultDir.mkdirs();

        if (request.getArg("macos.sourceOnly", "false").equalsIgnoreCase("true")) {
            // mac-source: the deliverable is the project, so stop before
            // xcodebuild. Reported rather than silently skipped, because a build
            // that produces no .app and says nothing reads as a failure.
            log("macos.sourceOnly is set; the Xcode project has been generated and will not be built.");
            return true;
        }

        return buildAndPackage(request, hints, distDir, appName);
    }

    /**
     * Writes the {@code <MainClass>Stub} that boots the application.
     *
     * <p>Deliberately a fraction of the iOS one. That stub carries Facebook,
     * Google Sign-In, Apple Sign-In, WebAuthn, maps, push, ads, Firebase and a
     * watch entry point, because those integrations are wired in at the entry
     * point on iOS. None of them is part of what makes an application start, and
     * the ones that apply to macOS reach the same shared implementation without
     * the stub's help.</p>
     *
     * <p>It extends the shared {@code com.codename1.impl.ios.Lifecycle} rather
     * than a macOS one of its own: that class is pure Java with no natives, it
     * ships in both ports, and the transitions it declares are the ones
     * CN1MacAppDelegate reports.</p>
     */
    private void writeStub(BuildRequest request, File stubSource, File classesDir) throws Exception {
        String svgRegistryInstall = new File(classesDir,
                "com/codename1/generated/svg/SVGRegistry.class").isFile()
                ? "            com.codename1.generated.svg.SVGRegistry.installGlobal();\n"
                : "";
        String registerNatives = registerNativeImplementationsAndCreateStubs(
                new java.net.URLClassLoader(new java.net.URL[]{getCodenameOneJar().toURI().toURL()}),
                stubSource, classesDir);
        String stubName = request.getMainClass() + "Stub";
        String src = "package " + request.getPackageName() + ";\n\n"
                + "import com.codename1.ui.*;\n"
                + "import com.codename1.system.*;\n\n"
                + "public class " + stubName + " extends com.codename1.impl.ios.Lifecycle implements Runnable {\n"
                + "    public static final String PACKAGE_NAME = \"" + request.getPackageName() + "\";\n"
                + "    public static final String APPLICATION_VERSION = \""
                + (request.getVersion() != null ? request.getVersion() : "1.0") + "\";\n"
                + "    private boolean initialized;\n"
                + "    private boolean stopped;\n"
                + "    private " + request.getMainClass() + " i = new " + request.getMainClass() + "();\n\n"
                + "    public void run() {\n"
                + "        if(!initialized) {\n"
                + "            initialized = true;\n"
                // Stamped before anything else runs, so Hardening.isHardened()
                // and the crash report's mapping id are right from the first
                // line of application code. Without it a hardened build reports
                // itself unhardened and its crash reports name no mapping, which
                // makes the uploaded mapping unreachable -- the failure is a
                // stack trace nobody can retrace, months later.
                + hardeningRuntimeProperties(request)
                + svgRegistryInstall
                + "            i.init(this);\n"
                + createStartInvocation(request, "i")
                + "        } else {\n"
                + createStartInvocation(request, "i")
                + "        }\n"
                + "    }\n\n"
                // Hide and unhide are what CN1MacAppDelegate reports as the
                // background transitions; a Mac has no suspended state.
                + "    public void applicationDidEnterBackground() {\n"
                + "        stopped = true;\n"
                + "        Display.getInstance().callSerially(new Runnable() {\n"
                + "            public void run() {\n"
                + "                i.stop();\n"
                + "            }\n"
                + "        });\n"
                + "    }\n\n"
                + "    public void applicationWillEnterForeground() {\n"
                + "        if(stopped) {\n"
                + "            stopped = false;\n"
                + "            Display.getInstance().callSerially(this);\n"
                + "        }\n"
                + "    }\n\n"
                + "    public boolean shouldApplicationHandleURL(String url, String caller) {\n"
                + "        if(i instanceof com.codename1.system.URLCallback) {\n"
                + "            return ((com.codename1.system.URLCallback)i).shouldApplicationHandleURL(url, caller);\n"
                + "        }\n"
                + "        return true;\n"
                + "    }\n\n"
                + "    public void applicationWillTerminate() {\n"
                + "        if(!stopped) {\n"
                + "            i.stop();\n"
                + "            stopped = true;\n"
                + "        }\n"
                + "        i.destroy();\n"
                + "    }\n\n"
                + "    public static void main(String[] argv) {\n"
                + "        if(!(argv != null && argv.length > 0 && argv[0].equals(\"ignoreNative\"))) {\n"
                + registerNatives
                + "        }\n"
                + "        " + stubName + " stub = new " + stubName + "();\n"
                + "        com.codename1.impl.ios.IOSImplementation.setMainClass(stub.i);\n"
                + routeDispatcherInstallSource(null, "        ")
                + "        Display.init(stub);\n"
                + "    }\n"
                + "}\n";
        File pkgDir = new File(stubSource,
                request.getPackageName().replace('.', File.separatorChar));
        pkgDir.mkdirs();
        FileOutputStream out = new FileOutputStream(new File(pkgDir, stubName + ".java"));
        try {
            out.write(src.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        f.delete();
    }

    private boolean compileStub(File stubSource, File classesDir, File portClasses) throws BuildException {
        String javacPath = System.getProperty("java.home") + "/../bin/javac";
        if (!new File(javacPath).exists()) {
            javacPath = System.getProperty("java.home") + "/bin/javac";
        }
        if (!new File(javacPath).exists()) {
            javacPath = "javac";
        }
        String[] sourceTarget = getStubCompileSourceTarget(javacPath);
        try {
            return execWithFiles(stubSource, stubSource, ".java", javacPath,
                    "-source", sourceTarget[0], "-target", sourceTarget[1],
                    // The port classes as well as the application's: the stub
                    // extends the shared Lifecycle, which ships in MacPort.jar
                    // and is not unzipped into the application's own classes.
                    "-classpath", classesDir.getAbsolutePath() + File.pathSeparator
                            + portClasses.getAbsolutePath(),
                    "-d", classesDir.getAbsolutePath());
        } catch (Exception ex) {
            throw new BuildException("Failed to compile the application entry point", ex);
        }
    }

    /**
     * Emits the Info.plist and, when the build is signed, the entitlements.
     *
     * <p>The plist is written from a map rather than patched into a template.
     * That is what removes the injection-conflict problem the Catalyst path has
     * to guard against: a key the application supplies either merges or is
     * refused, and there is no textual substitution that can half-apply.</p>
     */
    private void writeGeneratedPlists(BuildRequest request, MacOSBuildHints hints, File srcRoot,
            String appName, String version, String bundleVersion, File classesDir) throws IOException {
        // Scanned once, up front. The same answer decides two things that have to
        // agree: which sandbox entitlements the signature carries, and which
        // usage descriptions the bundle carries. An entitlement without its
        // description is a capability the app is allowed to ask for and is
        // killed for using.
        MacOSXcodeProject.MacOSCapabilities caps = new MacOSXcodeProject.MacOSCapabilities();
        try {
            scanClassesForPermissions(classesDir, new CapabilityScanner(caps));
        } catch (IOException ex) {
            // A failed scan must not silently produce an entitlement set that
            // omits a capability the application uses, because the failure then
            // shows up as a permission denial at runtime with no explanation.
            throw new IOException("Failed to scan the application for macOS capabilities", ex);
        }
        capabilities = caps;
        // loadsExternalCode: a hardened-runtime bundle that dlopens anything --
        // which a Codename One application does not, but a cn1lib shipping a
        // dylib might -- needs the library-validation exception or the load is
        // refused at runtime with nothing in the application's own logs.
        loadsExternalCode = "true".equalsIgnoreCase(
                request.getArg("macos.loadsExternalCode", "false"));

        Map<String, Object> plist = MacOSXcodeProject.infoPlist(request.getDisplayName(),
                hints.getBundleId(), version, bundleVersion, hints);

        int[] fixedSize = MacOSXcodeProject.parseFixedWindowSize(hints.getFixedWindowSize());
        if (fixedSize != null) {
            // Read back by CN1MacHost to pin the content size. The screenshot
            // suite's only lever for a deterministic pixel comparison, so it is
            // in the bundle rather than in a build-time define -- a test run has
            // to be able to set it without recompiling the port.
            plist.put("CN1FixedWindowWidth", Integer.valueOf(fixedSize[0]));
            plist.put("CN1FixedWindowHeight", Integer.valueOf(fixedSize[1]));
        }

        // Written before the inject merge so an application can still override one
        // of them, and after the generated keys so a capability the scan found
        // cannot ship without its sentence. macOS kills a process that touches a
        // TCC-gated API with no usage description -- no prompt, no catchable
        // error -- so a camera app built without this crashes on first use.
        // Calendars travels separately from the scanned capabilities: it is not
        // something the class scan detects, it is inherited from the iOS
        // usage-description hints -- and whichever channel grants the
        // entitlement needs the descriptions in the one shared plist.
        boolean calendarsGranted = false;
        for (String channel : hints.getChannels()) {
            calendarsGranted |= hints.entitlementsFor(channel).calendars(false);
        }
        plist.putAll(MacOSXcodeProject.privacyUsageDescriptions(
                effectiveCapabilities(hints, caps), calendarsGranted,
                new MacOSXcodeProject.UsageDescriptionResolver() {
                    @Override
                    public String get(String key) {
                        return hints.getUsageDescription(key);
                    }
                }));

        String inject = request.getArg("macos.plistInject", null);
        if (inject != null && inject.trim().length() > 0) {
            Map<String, Object> extra = new LinkedHashMap<String, Object>();
            for (String line : inject.split("\n")) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    extra.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
            List<String> collisions = MacOSXcodeProject.mergePlist(plist, extra);
            for (String key : collisions) {
                log("macos.plistInject overrides the generated " + key
                        + "; the port depends on the generated value, so check this deliberately.");
            }
        }

        MacOSXcodeProject.writePlist(plist, new File(srcRoot, appName + "-Info.plist"));
        writeAppIcon(request, srcRoot);

        // Written here rather than only at signing time, so mac-source hands the
        // developer a project that is complete: they open it in Xcode, sign it
        // themselves, and the entitlements are already sitting beside the plist
        // under the channel name. buildChannel points xcodebuild at the same
        // files rather than writing its own.
        for (String channel : hints.getChannels()) {
            writeEntitlements(hints, srcRoot, appName, channel, channelSuffix(channel));
        }
    }

    /**
     * Renders the application icon into the generated asset catalog.
     *
     * <p>The translator emits {@code AppIcon.appiconset/Contents.json} naming ten
     * PNGs and none of them, because the icon does not travel in the source
     * archive -- it rides the {@code BuildRequest} separately, which is why the
     * iOS and Windows builders read it explicitly too. Left unwritten, the
     * catalog references files that are not there: the bundle shows a generic
     * icon in the Dock and App Store validation refuses it for having none.</p>
     *
     * <p>The sizes are the ten the generated Contents.json asks for. They are
     * listed here rather than derived from it because the catalog is the
     * contract: a size the plist names and the build does not produce is the
     * failure this method exists to prevent, and it should be visible in one
     * place.</p>
     */
    private void writeAppIcon(BuildRequest request, File srcRoot) throws IOException {
        byte[] iconBytes = request.getIcon();
        File iconSet = new File(new File(srcRoot, "Images.xcassets"), "AppIcon.appiconset");
        if (iconBytes == null || iconBytes.length == 0 || !iconSet.isDirectory()) {
            if (iconSet.isDirectory()) {
                log("The build request carries no icon, so the generated asset catalog stays "
                        + "empty and the application ships with the system's generic icon.");
            }
            return;
        }
        BufferedImage icon = ImageIO.read(new ByteArrayInputStream(iconBytes));
        if (icon == null) {
            log("The build request's icon could not be decoded; the application ships with "
                    + "the system's generic icon.");
            return;
        }
        int[][] sizes = {
            {16, 1}, {16, 2}, {32, 1}, {32, 2}, {128, 1},
            {128, 2}, {256, 1}, {256, 2}, {512, 1}, {512, 2},
        };
        for (int[] size : sizes) {
            String name = "mac_" + size[0] + "x" + size[0]
                    + (size[1] == 2 ? "@2x" : "") + ".png";
            int px = size[0] * size[1];
            createIconFile(new File(iconSet, name), icon, px, px);
        }
    }

    /**
     * The capabilities the bundle has to describe: what the scan found, plus
     * anything a {@code macos.entitlements.device.*} override turned on.
     *
     * <p>The union rather than the scan, because the two have to agree. An
     * override exists precisely for access the scanner cannot see -- through a
     * cn1lib, or native code -- and granting the entitlement while omitting the
     * usage description is the worst of both: the app is allowed to ask, and
     * macOS kills it the moment it does.</p>
     *
     * <p>Across every channel, since the plist is one file and a capability
     * enabled for either channel ships in it.</p>
     */
    private MacOSXcodeProject.MacOSCapabilities effectiveCapabilities(MacOSBuildHints hints,
            MacOSXcodeProject.MacOSCapabilities scanned) {
        MacOSXcodeProject.MacOSCapabilities out = new MacOSXcodeProject.MacOSCapabilities();
        out.usesCamera = scanned.usesCamera;
        out.usesMicrophone = scanned.usesMicrophone;
        out.usesBluetooth = scanned.usesBluetooth;
        out.usesLocation = scanned.usesLocation;
        out.usesServerSockets = scanned.usesServerSockets;
        for (String channel : hints.getChannels()) {
            MacOSBuildHints.EntitlementOverrides o = hints.entitlementsFor(channel);
            out.usesCamera |= o.camera(scanned.usesCamera);
            out.usesMicrophone |= o.microphone(scanned.usesMicrophone);
            out.usesBluetooth |= o.bluetooth(scanned.usesBluetooth);
            out.usesLocation |= o.location(scanned.usesLocation);
            out.usesServerSockets |= o.networkServer(scanned.usesServerSockets);
        }
        return out;
    }

    /// The file-name suffix for one signing channel. One place, because the
    /// entitlements are written under it and named to xcodebuild under it, and
    /// the two disagreeing would sign against a file nobody generated.
    private static String channelSuffix(String channel) {
        return MacOSBuildHints.DISTRIBUTION_APP_STORE.equals(channel) ? "appstore" : "developerid";
    }

    /**
     * Writes the entitlements for one signing channel and returns the file.
     *
     * <p>Per channel rather than once, because the two channels do not want the
     * same set: the App Store requires the sandbox and a direct build usually
     * does not. That is also why {@code distribution=both} cannot be one
     * xcodebuild run -- the entitlements are baked into the signature.</p>
     */
    private File writeEntitlements(MacOSBuildHints hints, File srcRoot, String appName,
            String channel, String suffix) throws IOException {
        boolean appStore = MacOSBuildHints.DISTRIBUTION_APP_STORE.equals(channel);
        MacOSBuildHints.EntitlementOverrides overrides = hints.entitlementsFor(channel);
        Map<String, Object> ent = MacOSXcodeProject.entitlements(appStore, overrides,
                capabilities, loadsExternalCode);
        File out = new File(srcRoot, appName + "-" + suffix + ".entitlements");
        // macos.entitlements.extra rides along as raw XML rather than through the
        // map: it exists precisely for keys this builder does not model, so
        // parsing it would defeat it.
        MacOSXcodeProject.writePlist(ent, overrides.getExtra(), out);
        return out;
    }

    /**
     * Notices whether the application reaches the crypto primitives.
     *
     * <p>Biometrics and secure storage live in the same package and are
     * deliberately not counted: they need LocalAuthentication rather than the
     * cipher implementations, and this port links the former unconditionally.</p>
     */
    private static final class CryptoScanner implements Executor.ClassScanner {
        private final boolean[] flag;

        CryptoScanner(boolean[] flag) {
            this.flag = flag;
        }

        @Override
        public void implementsInterface(String cls, String iface) {
        }

        @Override
        public void usesClass(String cls) {
            if (cls == null || !cls.startsWith("com/codename1/security/")) {
                return;
            }
            String shortName = cls.substring("com/codename1/security/".length());
            boolean isBiometric = shortName.startsWith("Biometric")
                    || shortName.equals("SecureStorage")
                    || shortName.equals("AuthenticationOptions");
            if (!isBiometric) {
                flag[0] = true;
            }
        }

        @Override
        public void usesClassMethod(String cls, String method) {
        }
    }

    /** Maps class references onto the entitlements they require. */
    private static final class CapabilityScanner implements Executor.ClassScanner {
        private final MacOSXcodeProject.MacOSCapabilities caps;

        CapabilityScanner(MacOSXcodeProject.MacOSCapabilities caps) {
            this.caps = caps;
        }

        @Override
        public void implementsInterface(String cls, String iface) {
        }

        @Override
        public void usesClass(String cls) {
            if (cls == null) {
                return;
            }
            if (cls.startsWith("com/codename1/capture/") || cls.startsWith("com/codename1/media/")) {
                caps.usesCamera = true;
                caps.usesMicrophone = true;
            }
            if (cls.startsWith("com/codename1/bluetooth/")) {
                caps.usesBluetooth = true;
            }
            if (cls.startsWith("com/codename1/location/") || cls.startsWith("com/codename1/maps/")) {
                caps.usesLocation = true;
            }
            if (cls.startsWith("com/codename1/io/websocket/")
                    || cls.equals("com/codename1/io/ServerSocket")) {
                caps.usesServerSockets = true;
            }
        }

        @Override
        public void usesClassMethod(String cls, String method) {
        }
    }

    /**
     * Runs xcodebuild once per requested signing channel, then packages,
     * signs and notarizes each channel's output.
     *
     * <p>{@code distribution=both} is genuinely two builds. The channels differ
     * in the signing certificate AND in the entitlements the signature carries --
     * the App Store one has to be sandboxed -- so a single binary cannot be
     * relabelled into the other channel afterwards.</p>
     */
    private boolean buildAndPackage(BuildRequest request, MacOSBuildHints hints, File distDir,
            String appName) throws BuildException {
        File srcRoot = new File(distDir, appName + "-src");
        List<String> channels = hints.getChannels();
        for (String channel : channels) {
            if (!buildChannel(request, hints, distDir, srcRoot, appName, channel,
                    channels.size() > 1)) {
                return false;
            }
        }
        return true;
    }

    private boolean buildChannel(BuildRequest request, MacOSBuildHints hints, File distDir,
            File srcRoot, String appName, String channel, boolean multiChannel)
            throws BuildException {
        String suffix = channelSuffix(channel);
        // A derived-data directory per channel: the two builds differ only in
        // signing settings, and sharing one would let xcodebuild hand the second
        // channel the first channel's signature out of the build cache.
        File derived = new File(distDir, multiChannel ? "DerivedData-" + suffix : "DerivedData");
        derived.mkdirs();

        String signingIdentity = hints.getSigningIdentityFor(channel);
        boolean automatic = hints.usesAutomaticSigning();
        List<String> cmd = new ArrayList<String>();
        cmd.add("xcodebuild");
        cmd.add("-project");
        cmd.add(new File(distDir, appName + ".xcodeproj").getAbsolutePath());
        cmd.add("-target");
        cmd.add(appName);
        cmd.add("-configuration");
        cmd.add(request.getArg("macos.configuration", "Release"));
        // SYMROOT/OBJROOT rather than -derivedDataPath. xcodebuild refuses
        // -derivedDataPath unless it is also given -scheme ("The flag -scheme,
        // -testProductsPath, or -xctestrun is required when specifying
        // -derivedDataPath"), and the generated project has no shared scheme --
        // the screenshot script writes one itself precisely because the
        // translator emits none. These two settings are target-compatible and
        // put the products in the same place under `derived`, which is where
        // findAppBundle looks.
        cmd.add("SYMROOT=" + new File(derived, "Build/Products").getAbsolutePath());
        cmd.add("OBJROOT=" + new File(derived, "Build/Intermediates.noindex").getAbsolutePath());
        // Universal by default. A Mac application is expected to run on both
        // architectures, and a single-architecture build is the kind of thing
        // nobody notices until an Intel user reports it.
        cmd.add("ARCHS=" + request.getArg("macos.arch", "arm64 x86_64"));
        cmd.add("ONLY_ACTIVE_ARCH=NO");
        cmd.add("MACOSX_DEPLOYMENT_TARGET=" + hints.getMinDeploymentTarget());
        // The template hard-codes ENABLE_HARDENED_RUNTIME = YES in both
        // configurations, so the default was already right and only the opt-out
        // was broken. Passed explicitly either way, because a setting the build
        // reads from two places is a setting that disagrees with itself the next
        // time the template is edited.
        cmd.add("ENABLE_HARDENED_RUNTIME=" + (hints.isHardenedRuntime() ? "YES" : "NO"));
        if (signingIdentity == null) {
            cmd.add("CODE_SIGNING_ALLOWED=NO");
            cmd.add("CODE_SIGN_IDENTITY=");
        } else {
            File entitlements;
            try {
                entitlements = writeEntitlements(hints, srcRoot, appName, channel, suffix);
            } catch (IOException ex) {
                throw new BuildException("Failed to write the " + channel + " entitlements", ex);
            }
            if (automatic) {
                // No CODE_SIGN_IDENTITY under automatic signing: Xcode resolves
                // the certificate from the team and the provisioning profile, and
                // naming one here while it picks another is how a build ends up
                // signed by a certificate nobody chose.
                cmd.add("CODE_SIGN_STYLE=Automatic");
            } else {
                cmd.add("CODE_SIGN_STYLE=Manual");
                cmd.add("CODE_SIGN_IDENTITY=" + signingIdentity);
            }
            // The generated entitlements have to be named here. Neither the
            // project template nor codesign picks the file up by convention, so
            // without this the signature carries none of them: an App Store build
            // is signed without the sandbox entitlement it is required to have,
            // and the scanned capabilities -- camera, Bluetooth, location,
            // network server -- are absent, which shows up as a submission
            // rejection or a permission denial rather than as a build failure.
            cmd.add("CODE_SIGN_ENTITLEMENTS=" + entitlements.getAbsolutePath());
            if (hints.getTeamId() != null && hints.getTeamId().length() > 0) {
                cmd.add("DEVELOPMENT_TEAM=" + hints.getTeamId());
            }
        }
        cmd.add("build");
        try {
            if (!exec(distDir, 3600000, cmd.toArray(new String[0]))) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("xcodebuild failed for the macOS target", ex);
        }

        File built = findAppBundle(derived, appName);
        if (built == null) {
            throw new BuildException("xcodebuild reported success but no " + appName
                    + ".app was produced under " + derived.getAbsolutePath());
        }
        String base = multiChannel ? appName + "-" + suffix : appName;
        File bundle = new File(resultDir, base + ".app");
        collectBundle(built, bundle);
        if (appBundle == null) {
            appBundle = bundle;
        }
        artifacts.add(bundle);

        List<File> containers = packageChannel(hints, channel, bundle, base);
        artifacts.addAll(containers);

        // Notarization needs a signed bundle, so an unsigned build says so
        // rather than running a notarization that would be rejected.
        if (hints.isNotarize()) {
            if (MacOSBuildHints.DISTRIBUTION_APP_STORE.equals(channel)) {
                // Notarization requires Developer ID signing, so submitting the
                // store-signed artifact is rejected -- and with distribution=both
                // that rejection would fail the whole build after BOTH artifacts
                // were successfully produced. The store notarizes its own uploads.
                log("Skipping notarization of the App Store package: notarization applies to "
                        + "Developer ID distribution, and App Store submissions are notarized by "
                        + "the store.");
            } else if (signingIdentity == null) {
                log("macos.notarize is set but the " + channel + " build is unsigned, so there is "
                        + "nothing to notarize. Configure a Developer ID signing identity.");
            } else if (containers.isEmpty()) {
                notarize(request, hints, bundle);
            } else {
                // The container carries the ticket. Stapling to the .app inside a
                // dmg is invisible to the person who downloads the dmg, so it is
                // the dmg or the pkg that gets submitted and stapled.
                for (File container : containers) {
                    notarize(request, hints, container);
                }
            }
        }
        return true;
    }

    /**
     * Produces the artifacts {@code macos.packaging} asks for, beside the
     * {@code .app}, and returns them. Without this the build hands back a bundle
     * directory: nothing to upload to the App Store, and on the cloud builder
     * nothing that can even be transferred, since a result is read as a file.
     */
    private List<File> packageChannel(MacOSBuildHints hints, String channel, File bundle,
            String base) throws BuildException {
        String packaging = hints.getPackagingFor(channel);
        boolean wantsDmg = "dmg".equalsIgnoreCase(packaging) || "both".equalsIgnoreCase(packaging);
        boolean wantsPkg = "pkg".equalsIgnoreCase(packaging) || "both".equalsIgnoreCase(packaging);
        if (!wantsDmg && !wantsPkg && !"app".equalsIgnoreCase(packaging)) {
            log("Unrecognized macos.packaging value \"" + packaging
                    + "\"; producing the .app only. Valid values are app, dmg, pkg and both.");
        }
        List<File> containers = new ArrayList<File>();
        if (wantsDmg) {
            containers.add(buildDmg(hints, channel, bundle, base));
        }
        if (wantsPkg) {
            containers.add(buildPkg(hints, channel, bundle, base));
        }
        return containers;
    }

    private File buildDmg(MacOSBuildHints hints, String channel, File bundle, String base)
            throws BuildException {
        File dmg = new File(resultDir, base + ".dmg");
        dmg.delete();
        try {
            if (!exec(resultDir, 1800000, "hdiutil", "create", "-volname", base,
                    "-srcfolder", bundle.getAbsolutePath(), "-ov", "-format", "UDZO",
                    dmg.getAbsolutePath())) {
                throw new BuildException("hdiutil failed to create " + dmg.getName());
            }
            String identity = hints.getSigningIdentityFor(channel);
            if (identity != null) {
                // Gatekeeper checks the container on first open, so an unsigned
                // dmg around a signed app still warns.
                if (!exec(resultDir, 600000, "codesign", "--force", "--sign", identity,
                        "--timestamp", dmg.getAbsolutePath())) {
                    throw new BuildException("Failed to sign " + dmg.getName());
                }
            }
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to build " + dmg.getName(), ex);
        }
        return dmg;
    }

    private File buildPkg(MacOSBuildHints hints, String channel, File bundle, String base)
            throws BuildException {
        File pkg = new File(resultDir, base + ".pkg");
        pkg.delete();
        List<String> cmd = new ArrayList<String>();
        cmd.add("productbuild");
        cmd.add("--component");
        cmd.add(bundle.getAbsolutePath());
        cmd.add("/Applications");
        String installer = emptyToNull(hints.getInstallerIdentity());
        if (installer != null) {
            cmd.add("--sign");
            cmd.add(installer);
        } else if (MacOSBuildHints.DISTRIBUTION_APP_STORE.equals(channel)) {
            // Fatal, not a warning. productbuild accepts an unsigned package
            // happily and App Store Connect refuses it a long way downstream, so
            // warning here hands back an artifact that cannot be submitted.
            throw new BuildException("macos.signingIdentity.installer is not set, so the App "
                    + "Store package would be unsigned and App Store Connect would refuse it. "
                    + "It wants a \"3rd Party Mac Developer Installer\" certificate, which is a "
                    + "different certificate from the application signing identity.");
        }
        cmd.add(pkg.getAbsolutePath());
        try {
            if (!exec(resultDir, 1800000, cmd.toArray(new String[0]))) {
                throw new BuildException("productbuild failed to create " + pkg.getName());
            }
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to build " + pkg.getName(), ex);
        }
        return pkg;
    }

    private static String emptyToNull(String s) {
        return s == null || s.trim().length() == 0 ? null : s;
    }

    /**
     * Submits one artifact to notarytool and staples the ticket to it.
     *
     * <p>Takes a {@code .app}, a {@code .dmg} or a {@code .pkg}. Only the bundle
     * needs archiving first; notarytool accepts a dmg or a pkg directly, and
     * wrapping one in a zip would staple the ticket to the zip instead of to the
     * thing the user opens.</p>
     */
    private void notarize(BuildRequest request, MacOSBuildHints hints, File artifact)
            throws BuildException {
        File zip = null;
        try {
            File submission = artifact;
            if (artifact.isDirectory()) {
                zip = new File(resultDir, artifact.getName() + ".zip");
                // ditto rather than zip: notarytool rejects an archive that does
                // not preserve the bundle's symlinks and extended attributes, and
                // a plain zip does not.
                if (!exec(resultDir, 600000, "ditto", "-c", "-k", "--keepParent",
                        artifact.getAbsolutePath(), zip.getAbsolutePath())) {
                    throw new BuildException("Failed to archive the application for notarization");
                }
                submission = zip;
            }
            int passwordIndex = -1;
            List<String> submit = new ArrayList<String>();
            submit.add("xcrun");
            submit.add("notarytool");
            submit.add("submit");
            submit.add(submission.getAbsolutePath());
            if (hints.getNotarizeKeychainProfile() != null
                    && hints.getNotarizeKeychainProfile().length() > 0) {
                submit.add("--keychain-profile");
                submit.add(hints.getNotarizeKeychainProfile());
            } else {
                submit.add("--apple-id");
                submit.add(hints.getNotarizeAppleId());
                submit.add("--team-id");
                submit.add(hints.getNotarizeTeamId());
                submit.add("--password");
                // Through the hints, not straight off the request: the legacy
                // macNative.notarize.password spelling is still supported and
                // only the hint parser knows about it. Reading the modern key
                // directly submits an empty password for a migrated project.
                passwordIndex = submit.size();
                submit.add(hints.getNotarizePassword() == null
                        ? "" : hints.getNotarizePassword());
            }
            submit.add("--wait");
            if (!exec(resultDir, 3600000, submit.toArray(new String[0]))) {
                throw new BuildException("Notarization was rejected");
            }
            // Stapling the ticket is what makes the application launch without a
            // network round trip on the user's machine.
            if (!exec(resultDir, 600000, "xcrun", "stapler", "staple",
                    artifact.getAbsolutePath())) {
                throw new BuildException("Failed to staple the notarization ticket");
            }
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Notarization failed", ex);
        } finally {
            if (zip != null) {
                zip.delete();
            }
        }
    }

    private File findAppBundle(File dir, String appName) {
        File[] children = dir.listFiles();
        if (children == null) {
            return null;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                if (f.getName().equals(appName + ".app")) {
                    return f;
                }
                File found = findAppBundle(f, appName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Copies the built {@code .app} into the result directory.
     *
     * <p>ditto, not a recursive file copy. A bundle that embeds a framework
     * carries symlinks -- {@code Versions/Current} and the top-level binary and
     * resource entries -- and {@code File.isDirectory()} and
     * {@code FileInputStream} both follow them, so a hand-rolled copy
     * materializes each link as a real file. The copy then no longer matches the
     * signature Xcode just produced, and codesign verification and notarization
     * reject it; the original bundle passes, so it looks like the packaging step
     * broke the app. ditto also preserves the mode bits and extended attributes
     * the signature covers.</p>
     *
     * <p>The destination is removed first. Without that, a rebuild without
     * {@code mvn clean} merges the new bundle over the old one: a resource or a
     * nested binary deleted since the last build stays in what ships, and for a
     * signed build those unsealed files fail verification.</p>
     */
    private void collectBundle(File src, File dest) throws BuildException {
        try {
            if (dest.exists()) {
                deleteRecursively(dest);
            }
            dest.getParentFile().mkdirs();
            if (!exec(resultDir, 600000, "ditto", src.getAbsolutePath(), dest.getAbsolutePath())) {
                throw new BuildException("Failed to collect " + src.getName()
                        + " into the result directory");
            }
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to collect the built application bundle", ex);
        }
    }

    /// Removes a tree, tolerating one that is not there.
    ///
    /// No symlink following: descending through a link would delete outside the
    /// tree being removed, and a signed bundle is full of them.
    private static void deleteRecursively(File f) throws IOException {
        if (!f.exists()) {
            return;
        }
        if (f.isDirectory() && !isSymlink(f)) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!f.delete() && f.exists()) {
            throw new IOException("Failed to delete " + f.getAbsolutePath());
        }
    }

    private static boolean isSymlink(File f) throws IOException {
        return !f.getCanonicalFile().equals(f.getAbsoluteFile());
    }

    private void extractJarResource(String resource, File destDir) throws Exception {
        InputStream is = getResourceAsStream(resource);
        if (is == null) {
            throw new BuildException("Required bundled resource missing: " + resource);
        }
        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is);
        try {
            java.util.zip.ZipEntry entry;
            byte[] buf = new byte[8192];
            String destCanon = destDir.getCanonicalPath() + File.separator;
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(destDir, entry.getName());
                // Zip slip: reject an entry that escapes destDir.
                if (!out.getCanonicalPath().startsWith(destCanon)) {
                    throw new BuildException("Refusing to extract entry outside the target "
                            + "directory (zip slip): " + entry.getName());
                }
                if (entry.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                out.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(out);
                try {
                    int n;
                    while ((n = zis.read(buf)) != -1) {
                        fos.write(buf, 0, n);
                    }
                } finally {
                    fos.close();
                }
            }
        } finally {
            zis.close();
        }
    }

    private static String join(String sep, File... files) {
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(f.getAbsolutePath());
        }
        return sb.toString();
    }
}
