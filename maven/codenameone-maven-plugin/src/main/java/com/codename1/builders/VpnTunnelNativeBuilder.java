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

import com.codename1.util.IOSVpnTunnelExtensionBuilder;

import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The iOS packet-tunnel Network Extension: the host half of
 * {@code com.codename1.vpn.tunnel}.
 *
 * <p>A delegate owned by {@link IPhoneBuilder}, in the shape
 * {@link WatchNativeBuilder} established -- and for the same reason, because
 * this is the same mechanism. A packet tunnel on iOS runs in a separate
 * process with its own bundle, so the application's {@code VpnTunnel} has to
 * be a program of its own: a second ByteCodeTranslator pass rooted at a
 * generated stub that reaches the tunnel, compiled into an app-extension
 * target the host embeds.</p>
 *
 * <p><b>Rooting the translation there is the whole design, not an
 * optimisation.</b> An app extension compiles with
 * {@code APPLICATION_EXTENSION_API_ONLY}, so a target carrying the
 * application's own translation cannot build: that tree reaches the iOS
 * port's natives, and {@code IOSNative.m} calls {@code UIApplicationMain} and
 * {@code [UIApplication sharedApplication]}, which {@code UIApplication.h}
 * declares {@code NS_EXTENSION_UNAVAILABLE_IOS}. Turning the setting off
 * compiles and is rejected at submission. Rooted at the tunnel the tree
 * contains what the tunnel reaches, the port's application shell is not in
 * it, and the target compiles as itself rather than as a smaller copy of the
 * app.</p>
 *
 * <p>The consequence a developer feels, and the guide says it too: a tunnel
 * that reaches for the application's classes drags them into this
 * translation, and the ones backed by the port's UIKit natives fail the
 * extension's LINK with an undefined symbol. That is the honest failure --
 * the alternative is a tunnel that builds and then finds nothing there,
 * because the extension shares no statics, no {@code Display} and no open
 * connections with the app. Everything the tunnel needs travels in
 * {@code TunnelSetup.data}.</p>
 *
 * <p><b>Gated twice, deliberately.</b> The app must reference
 * {@code com.codename1.vpn.tunnel} AND set {@code ios.vpn.tunnel=true}. The
 * extension's entitlement,
 * {@code com.apple.developer.networking.networkextension}, is one Apple
 * grants case by case rather than one a paid account switches on, so an App
 * ID without the grant fails codesigning with a message naming the
 * entitlement and not the reason it appeared. The hint is the project saying
 * it holds the grant; a class reference alone must never be enough.</p>
 *
 * <p><b>On the start/stop window.</b> The generated provider claims a
 * generation before anything asynchronous and re-checks it in the settings
 * completion, but a stop can still land between that check and the calls
 * after it -- a check-then-act window no amount of re-checking closes. It is
 * left open deliberately rather than serialised with a lock, because what
 * survives it is inert: the read is armed for a generation the stop has
 * already moved past, so nothing is delivered and nothing is re-armed, and
 * the writer is installed for that same generation, so nothing is written.
 * What remains is a Java tunnel object the next start replaces, and a
 * success reported to NE for a start it had already cancelled. A lock around
 * the completion and the stop would improve neither, and could not touch the
 * case that actually mattered -- an application callback already running,
 * which is why the WRITER carries its generation instead.</p>
 *
 * <p>Every change here is additive: without the hint no stub is written, no
 * second translation runs, no target is created and the iOS build is
 * byte-for-byte what it was. Keep this file in sync with the
 * cloud builder's {@code com.codename1.build.daemon.VpnTunnelNativeBuilder}.</p>
 */
class VpnTunnelNativeBuilder {
    private final IPhoneBuilder owner;

    /// Set only when the app references the package AND the project claims the
    /// Network Extension grant; see the class notes on why both are required.
    private boolean enabled;

    /// The application's VpnTunnel subclass, in source form
    /// (com.example.MyTunnel), from ios.vpn.tunnel.class.
    ///
    /// Initialised rather than left null. Every caller reaches it behind
    /// isEnabled(), which is only true once parseHints has set this -- but
    /// "only reached behind another field" is an invariant a reader has to
    /// go and check, and SpotBugs reads it as a field dereferenced before
    /// any constructor set it (UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR).
    /// Empty means the same thing here as null did and costs nothing.
    private String tunnelClass = "";

    VpnTunnelNativeBuilder(IPhoneBuilder owner) {
        this.owner = owner;
    }

    /**
     * The build hint that turns the extension on.
     *
     * <p>Reserved for years and documented as failing the build; it is now
     * what generates the target.</p>
     */
    static final String HINT_ENABLED = "ios.vpn.tunnel";

    /** Which {@code VpnTunnel} subclass the extension runs. */
    static final String HINT_CLASS = "ios.vpn.tunnel.class";

    /**
     * Where the second translation is written.
     *
     * <p>Its own output root, out of the phone's, so neither pass can see the
     * other's dist tree -- exactly as {@code WatchNativeBuilder} keeps
     * {@code watchvm} separate.</p>
     */
    static File translationDir(File tmpFile) {
        return new File(tmpFile, "vpntunnelvm");
    }

    /**
     * The translator root: the class name the second pass is given.
     *
     * <p>Named after the main class so the two translations are obviously the
     * same application, and suffixed so the generated tree and the target
     * cannot be confused with the phone's.</p>
     */
    static String translationRoot(String mainClass) {
        return mainClass + "VpnTunnel";
    }

    /** The generated stub the tunnel translation is rooted at. */
    static String stubClass(String mainClass) {
        return translationRoot(mainClass) + "Stub";
    }

    /** Where the staged tunnel translation lives, relative to the app's -src directory. */
    static final String SRC_DIR = "vpn-tunnel-src";

    /**
     * The iOS port's hand-written native sources, which the extension target
     * must not compile.
     *
     * <p>Recorded from the directories hand-written sources are unzipped
     * into rather than listed here, because a list is wrong in both
     * directions. This started
     * as one -- the sources that call {@code UIApplicationMain} or
     * {@code [UIApplication sharedApplication]}, which an
     * {@code APPLICATION_EXTENSION_API_ONLY} target rejects -- and that list
     * excluded {@code IOSNative.m} while still compiling fifteen port
     * sources that reference symbols {@code IOSNative.m} defines:
     * {@code toNSString}, {@code nsDataToByteArr}, {@code scaleValue},
     * {@code displayWidth}, {@code repaintUI}. The target would have failed
     * to LINK, and nothing here or in CI compiles it, so the first thing to
     * notice would have been a customer's device build.</p>
     *
     * <p>The rule that is actually true: <b>the extension compiles the
     * translated program and the ParparVM runtime, and none of the port.</b>
     * It can, because the tree is rooted at the tunnel -- the only
     * {@code com.codename1.impl.ios} class in it is
     * {@code IOSExtensionTunnel}, whose one native the generated provider
     * implements itself. The runtime ({@code cn1_globals.m},
     * {@code nativeMethods.m}, {@code java_io_File.m} and the allocators)
     * comes from the translator, not from the port, so it is never in this
     * set and is never excluded.</p>
     *
     * <p>BOTH roots, not only the port's. The translator COPIES every
     * non-class file it walks into each translation, and the tunnel pass is
     * given the resource root as well as the library one -- so an
     * application's own {@code NativeInterface} implementation lands in the
     * tunnel's tree exactly as the port's natives do. A tunnel that never
     * mentions that native would otherwise have had it compiled under
     * {@code APPLICATION_EXTENSION_API_ONLY}, and a valid build could fail
     * on somebody else's UIKit call.</p>
     *
     * <p>A native belonging to a submitted library or to the application is
     * excluded with the rest. That is the documented
     * bargain rather than a gap: a tunnel gets nothing from the application
     * process, and one that calls a library's native fails the extension's
     * link naming the symbol -- which is the same report as reaching for any
     * other app-side class, and better than a tunnel that builds and finds
     * nothing there.</p>
     *
     * <p>Keyed by basename because the translation FLATTENS what it copies
     * -- a native two directories deep arrives beside the emitted sources
     * with nothing left of its path -- and valued by the digests of the
     * files that basename was recorded from. The digests are what tell a
     * copied file from an emitted one: an application native called
     * {@code nativeMethods.m} or {@code cn1_globals.m} shares its name with
     * something the translator writes itself, and the file that survives in
     * the translation is the translator's. Excluded on the name alone, the
     * extension would have been staged without a runtime source it has to
     * link, and the report would have been a missing symbol on a machine
     * none of our tests run on.</p>
     */
    private final java.util.Map<String, java.util.Set<String>>
            handWrittenNatives =
            new java.util.HashMap<String, java.util.Set<String>>();

    /**
     * Records what the port, the submitted libraries and the application
     * hand-wrote, so {@link #stageTranslation} can leave it out.
     *
     * <p>Called once those have been unzipped and before the translation is
     * staged. Cheap, and only for a build that generates the extension.</p>
     *
     * @param roots the directories the build unzipped hand-written sources
     *              into: the library root and the resource root
     */
    void recordHandWrittenNatives(File... roots) {
        for (File root : roots) {
            if (root == null || root.listFiles() == null) {
                // Refused rather than carried on with a short set: a name
                // this does not know is a source compiled into the
                // extension, which is exactly the broken target this exists
                // to prevent, and it would fail at link on a machine none of
                // our tests run on.
                throw new BuildException("Could not read the hand-written"
                        + " native sources at " + root + ", so the packet"
                        + " tunnel extension cannot know which sources belong"
                        + " to the application rather than to it.");
            }
            recordTree(root);
        }
    }

    /**
     * Walks a root the way the translator walks it.
     *
     * <p>RECURSIVELY, because {@code Executor.unzip} keeps a submitted
     * archive's subdirectories and {@code ByteCodeTranslator.execute()}
     * descends into them and FLATTENS what it finds into the translation's
     * output. A native two directories down therefore arrives in the
     * extension's tree beside the top-level ones, and a snapshot of the
     * root's immediate children would not have known its name.</p>
     *
     * <p>{@code .bundle} and {@code .xcdatamodeld} are skipped for the same
     * reason the translator treats them apart: it copies those as
     * directories rather than flattening them, so nothing inside one ever
     * becomes a source the extension target could compile.</p>
     */
    private void recordTree(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (name.endsWith(".bundle") || name.endsWith(".xcdatamodeld")) {
                    continue;
                }
                recordTree(f);
                continue;
            }
            java.util.Set<String> digests =
                    handWrittenNatives.get(f.getName());
            if (digests == null) {
                digests = new java.util.HashSet<String>();
                handWrittenNatives.put(f.getName(), digests);
            }
            digests.add(digest(f));
        }
    }

    /**
     * Reads the hints and decides whether the extension is generated.
     *
     * <p>Called at hint-parse time, after the class scanner has settled
     * whether the app references the package at all.</p>
     *
     * @param request the build request
     * @param usesCustomTunnel whether the scan found com.codename1.vpn.tunnel
     */
    void parseHints(BuildRequest request, boolean usesCustomTunnel) {
        if (!"true".equals(request.getArg(HINT_ENABLED, "false"))) {
            return;
        }
        if (!usesCustomTunnel) {
            // The hint without the package is a project asking for an
            // extension that would run nothing. Refused rather than ignored:
            // the entitlement it carries is a granted one, so a target
            // generated on a misunderstanding fails codesigning and the
            // developer is left reading an error about an entitlement they
            // did not know they had asked for.
            throw new BuildException(HINT_ENABLED + "=true, but this app"
                    + " does not reference com.codename1.vpn.tunnel. The"
                    + " extension exists to run a VpnTunnel subclass; write"
                    + " one, or remove the hint.");
        }
        tunnelClass = request.getArg(HINT_CLASS, "");
        tunnelClass = tunnelClass == null ? "" : tunnelClass.trim();
        if (tunnelClass.length() == 0) {
            // NAMED, not discovered. The scanner sees that the package is
            // used; it cannot know which subclass is the one the extension
            // should boot, and an app may legitimately have more than one.
            // Guessing would put the wrong packet loop in the tunnel.
            throw new BuildException(HINT_ENABLED + "=true needs "
                    + HINT_CLASS + " as well: the extension runs one"
                    + " VpnTunnel subclass and the build cannot know which"
                    + " one. Set it to the fully qualified class name, e.g. "
                    + HINT_CLASS + "=com.example.MyTunnel");
        }
        enabled = true;
    }

    /** Whether this build generates the packet-tunnel extension. */
    boolean isEnabled() {
        return enabled;
    }

    /** The application's VpnTunnel subclass, or empty when disabled. */
    String getTunnelClass() {
        return tunnelClass;
    }

    /**
     * Checks that the named tunnel class is one the translator will parse.
     *
     * <p>Checked HERE, against the compiled classes, rather than left to the
     * generated stub's javac. Both refuse, but this one can say which hint
     * was wrong; javac would report an unresolvable symbol inside a source
     * file the developer never wrote.</p>
     *
     * <p><b>A LOOSE class file, and deliberately not one inside a jar.</b>
     * This briefly accepted an archive, on the reasoning that
     * {@code foldInCallAndVpnLibraryUsage} recognises tunnel usage inside a
     * submitted library so the build should too. It cannot:
     * {@code ByteCodeTranslator.execute()} parses {@code *.class} and COPIES
     * every other file it walks, so a class that exists only inside an
     * archive is never translated -- and the provider then calls an
     * allocator and a constructor the extension has no definition of, which
     * is a link error instead of a refusal. An accurate refusal is worth
     * more than an acceptance the translator cannot honour, so this stays a
     * loose-file check and the message says what to do about it.</p>
     *
     * <p>EXISTENCE only. Whether the class is a {@code VpnTunnel} and
     * whether it has a no-argument constructor are left to javac on the
     * generated stub, which reads them off the real class rather than
     * guessing at its bytes -- and the stub carries a comment saying so, so
     * the error names the hint even though the file naming it is generated.
     * Parsing the constructor out of the {@code .class} here would be a
     * second, worse implementation of a check the compiler already does
     * exactly.</p>
     *
     * @param classesDir the compiled application classes
     */
    void verifyTunnelClass(File classesDir) {
        // A PACKAGE first. The stub is generated into the application's
        // package, and java in a named package cannot name a class in the
        // default one, so "Tunnel" would compile as <app package>.Tunnel and
        // fail on a class the developer never wrote. Refused here rather than
        // taught to the stub: the stub's package is also the folder
        // isolateStub filters and part of what the translation is rooted at,
        // and a default package tunnel is not worth three moving parts.
        if (tunnelClass.indexOf('.') < 0) {
            throw new BuildException(HINT_CLASS + " names " + tunnelClass
                    + ", which has no package. The extension is compiled from"
                    + " a generated class in the application's own package,"
                    + " and java cannot reference a class in the default"
                    + " package from a named one, so the tunnel has to live in"
                    + " a package -- give it one and name it here in full.");
        }
        String entry = tunnelClass.replace('.', '/') + ".class";
        if (new File(classesDir, entry.replace('/', File.separatorChar)).isFile()) {
            return;
        }
        throw new BuildException(HINT_CLASS + " names "
                + tunnelClass + ", which is not among this application's"
                + " compiled classes. The extension is translated from that"
                + " class and the translator reads loose class files, so the"
                + " name has to be the fully qualified one -- including the"
                + " package, and with $ for a nested class -- of a VpnTunnel"
                + " subclass this project compiles. A tunnel that lives only"
                + " inside a submitted library jar is never translated and"
                + " cannot be the extension's entry point; move it into the"
                + " application.");
    }

    /**
     * Writes the stub the tunnel translation is rooted at.
     *
     * <p>Deliberately the smallest program that reaches everything the
     * generated provider calls, and nothing else. It is not a
     * {@code Lifecycle} and it never touches {@code Display}: the phone stub
     * instantiates the application, and a translation rooted there would drag
     * the whole UI graph -- and with it the port natives an app extension
     * cannot compile -- into the extension.</p>
     *
     * <p>Every line is a reference the translator follows out of this root.
     * {@code main} is never executed: the extension's entry point is
     * {@code NSExtensionMain}, and the provider calls these same entry points
     * from Objective-C. What the body does is make them REACHABLE, which is
     * the only way a symbol survives the dead-code cull.</p>
     *
     * @param request the build request
     * @param stubSource the folder the stubs are compiled from
     */
    void writeStubSource(BuildRequest request, File stubSource)
            throws IOException {
        String stubClass = stubClass(request.getMainClass());
        String body = "package " + request.getPackageName() + ";\n\n"
                + "/**\n"
                + " * Generated packet-tunnel entry point, rooted at "
                + HINT_CLASS + " (" + tunnelClass + ").\n"
                + " *\n"
                + " * <p>Never runs. The extension's entry point is"
                + " NSExtensionMain and the generated\n"
                + " * CN1VpnTunnelProvider calls these methods directly;"
                + " main() exists so the\n"
                + " * translator keeps them.</p>\n"
                + " *\n"
                + " * <p>If javac fails HERE rather than in your own code,"
                + " it is almost always\n"
                + " * the constructor: the extension builds the tunnel"
                + " itself, in a process\n"
                + " * where Tunnels.start() never ran, so "
                + HINT_CLASS + " has to name a\n"
                + " * VpnTunnel subclass with an accessible no-argument"
                + " constructor.</p>\n"
                + " */\n"
                + "public class " + stubClass + " {\n"
                + "    public static void main(String[] argv) {\n"
                + "        // The writer the provider implements. Installed"
                + " from Java so the\n"
                + "        // class -- and its native declaration -- are in"
                + " the tree at all.\n"
                + "        com.codename1.impl.ios.IOSExtensionTunnel"
                + ".install(0);\n"
                + "        // The application's tunnel. A DIRECT constructor"
                + " call: the provider\n"
                + "        // reaches it through the translated allocator,"
                + " and a name looked up\n"
                + "        // at run time would not survive obfuscation --"
                + " which is why\n"
                + "        // Class.forName is banned in the framework"
                + " itself.\n"
                + "        com.codename1.vpn.tunnel.VpnTunnel tunnel =\n"
                + "                new " + sourceName(tunnelClass) + "();\n"
                + "        // The four the provider calls, in the order it"
                + " calls them.\n"
                + "        com.codename1.impl.vpn.ExtensionTunnelHost.begin("
                + "tunnel, argv[0], 0);\n"
                + "        com.codename1.impl.vpn.ExtensionTunnelHost.buffer("
                + "0, 0);\n"
                + "        com.codename1.impl.vpn.ExtensionTunnelHost"
                + ".received(0, 0);\n"
                + "        com.codename1.impl.vpn.ExtensionTunnelHost.end("
                + "0, 0);\n"
                + "    }\n"
                + "}\n";
        OutputStream out = new java.io.FileOutputStream(
                new File(stubSource, stubClass + ".java"));
        try {
            out.write(body.getBytes("UTF-8"));
        } finally {
            out.close();
        }
        owner.log("[vpnTunnel] Wrote " + stubClass + ".java; the packet-tunnel"
                + " extension is translated from " + tunnelClass
                + " rather than sharing the app's translation");
    }

    /**
     * The hint's value as JAVA source names it.
     *
     * <p>{@code ios.vpn.tunnel.class} is a binary name, so a nested tunnel
     * arrives as {@code com.example.Outer$Tunnel} -- which is what the class
     * file is called and what {@link #verifyTunnelClass} looks for, and is
     * not something javac will parse. The generated stub has to say
     * {@code com.example.Outer.Tunnel}.</p>
     */
    static String sourceName(String binaryName) {
        return binaryName == null ? "" : binaryName.replace('$', '.');
    }

    /**
     * Moves the tunnel stub out of the shared classes tree.
     *
     * <p>The translator parses everything on its classpath and refuses a
     * classpath carrying two mains, so each pass has to be handed exactly
     * one. Same arrangement, and the same reason, as
     * {@link WatchNativeBuilder#isolateStub}.</p>
     *
     * <p>Inner classes travel with their outer class, and a name that merely
     * starts the same way does not: {@code FooVpnTunnelStubHelper} stays
     * where it is.</p>
     *
     * @return the directory holding the tunnel stub
     */
    File isolateStub(BuildRequest request, File classesDir, File tmpFile)
            throws IOException {
        String stubClass = stubClass(request.getMainClass());
        File dest = new File(tmpFile, "vpntunnelstub");
        String pkgPath = request.getPackageName() == null
                || request.getPackageName().isEmpty()
                ? "" : request.getPackageName().replace('.', File.separatorChar);
        File fromDir = pkgPath.isEmpty() ? classesDir : new File(classesDir, pkgPath);
        File toDir = pkgPath.isEmpty() ? dest : new File(dest, pkgPath);
        toDir.mkdirs();
        File[] candidates = fromDir.listFiles();
        if (candidates == null) {
            throw new IOException("no compiled classes at " + fromDir);
        }
        int moved = 0;
        for (File f : candidates) {
            String name = f.getName();
            if (!f.isFile() || !name.endsWith(".class")) {
                continue;
            }
            if (!name.equals(stubClass + ".class")
                    && !name.startsWith(stubClass + "$")) {
                continue;
            }
            IPhoneBuilder.copy(f, new File(toDir, name));
            if (!f.delete()) {
                throw new IOException("could not move " + f + " out of the"
                        + " shared classes tree; leaving it there would give"
                        + " the translator two main classes");
            }
            moved++;
        }
        if (moved == 0) {
            throw new IOException("expected " + stubClass + ".class in " + fromDir);
        }
        return dest;
    }

    /**
     * Moves the tunnel translation next to the Xcode project and reports its
     * source file names.
     *
     * <p>The second pass writes into its own root so the two translations
     * cannot see each other, but the project has to reference the files with
     * a path relative to itself -- so the tree is copied into
     * {@code <MainClass>-src/vpn-tunnel-src/} once, here. Same staging, and
     * the same reason, as the watch's.</p>
     *
     * @return the base names the extension target must compile
     */
    List<String> stageTranslation(BuildRequest request, File tmpFile,
            File appSrcDir) throws IOException {
        List<String> compiled = new ArrayList<String>();
        File from = new File(translationDir(tmpFile),
                "dist/" + translationRoot(request.getMainClass()) + "-src");
        if (!from.isDirectory()) {
            throw new IOException("the packet-tunnel translation produced no"
                    + " sources at " + from);
        }
        File to = new File(appSrcDir, SRC_DIR);
        to.mkdirs();
        File[] files = from.listFiles();
        if (files == null) {
            throw new IOException("could not read the packet-tunnel"
                    + " translation at " + from);
        }
        for (File f : files) {
            String name = f.getName();
            if (!f.isFile()) {
                continue;
            }
            // .S and .s are sources here, and leaving them out was a link
            // error waiting for a device. The translator emits
            // cn1_virtual_thread_asm.S beside cn1_virtual_thread.c, and on
            // arm64 the C half calls into it -- cn1VirtualThreadSwitch,
            // cn1VirtualThreadPrime, cn1VirtualThreadTrampoline -- so
            // staging the caller without the callee gives the extension
            // three undefined symbols. (The watch's staging has the same
            // omission and does not fail on it: watchOS is arm64_32, where
            // the assembly is not built.)
            boolean source = name.endsWith(".m") || name.endsWith(".c")
                    || name.endsWith(".mm") || name.endsWith(".cpp")
                    || name.endsWith(".cc") || name.endsWith(".S")
                    || name.endsWith(".s");
            if (!source && !name.endsWith(".h")) {
                // Only the code. The extension's plist and entitlements are
                // written by IOSVpnTunnelExtensionBuilder against the HOST's
                // identity; the second translation's copies describe a
                // standalone application and would overwrite them.
                continue;
            }
            // .swift is deliberately absent from that list, unlike the
            // watch's. A NativeInterface implemented in Swift belongs to the
            // application, and an extension that reached one would be
            // reaching into the app process -- so if the rooted translation
            // emits one, the undefined symbol at link is the report worth
            // getting.
            IPhoneBuilder.copy(f, new File(to, name));
            if (source && !isExcluded(f)) {
                compiled.add(name);
            }
        }
        // A prefix header of this tree's OWN, so its quoted includes resolve
        // here. The app's pch does #include "cn1_class_method_index.h", and a
        // quoted include resolves against the directory of the file doing the
        // including -- so compiled with the app's pch every extension source
        // would see the APP's class index, which does not declare the ids
        // this translation just generated.
        File appPch = new File(appSrcDir, request.getMainClass() + "-Prefix.pch");
        if (appPch.isFile()) {
            IPhoneBuilder.copy(appPch, new File(to, prefixHeader(request.getMainClass())));
        }
        owner.log("[vpnTunnel] Staged " + compiled.size()
                + " translated sources for the packet-tunnel extension,"
                + " rooted at " + tunnelClass);
        return compiled;
    }

    /** The extension tree's own prefix header; see {@link #stageTranslation}. */
    static String prefixHeader(String mainClass) {
        return translationRoot(mainClass) + "-Prefix.pch";
    }

    /**
     * Whether a staged source was hand-written rather than emitted by the
     * translator.
     *
     * <p>Answers false for everything until
     * {@link #recordHandWrittenNatives} has run, which is why that is not
     * optional: an unrecorded build would compile the whole port into the
     * extension and fail at link.</p>
     */
    boolean isExcluded(String name) {
        return handWrittenNatives.containsKey(name);
    }

    /**
     * Whether one staged file is a copy of something hand-written.
     *
     * <p>The name narrows it and the CONTENT decides it; see the field.
     * A staged file whose basename was recorded but whose bytes match none
     * of what was recorded under it is the translator's own, and belongs in
     * the extension.</p>
     *
     * @param staged the file as the translation left it
     */
    boolean isExcluded(File staged) {
        java.util.Set<String> digests =
                handWrittenNatives.get(staged.getName());
        return digests != null && digests.contains(digest(staged));
    }

    /** One file's SHA-1, as hex. */
    private String digest(File f) {
        try {
            java.security.MessageDigest md =
                    java.security.MessageDigest.getInstance("SHA-1");
            java.io.InputStream in = new java.io.FileInputStream(f);
            try {
                byte[] buf = new byte[8192];
                for (int r = in.read(buf); r > 0; r = in.read(buf)) {
                    md.update(buf, 0, r);
                }
            } finally {
                in.close();
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : md.digest()) {
                hex.append(Character.forDigit((b >> 4) & 0xf, 16));
                hex.append(Character.forDigit(b & 0xf, 16));
            }
            return hex.toString();
        } catch (Exception err) {
            // REFUSED rather than guessed. Guessing hand-written drops a
            // source the extension may have to link; guessing emitted
            // compiles a port native into an app extension. Both fail on a
            // machine none of our tests run on, so this fails here instead.
            throw new BuildException("Could not read " + f + " while deciding"
                    + " whether it belongs to the packet-tunnel extension.",
                    err);
        }
    }

    /**
     * The bundle identifier the extension signs under, honouring the
     * override.
     *
     * <p>Resolved in ONE place because three things have to agree: the
     * target's {@code PRODUCT_BUNDLE_IDENTIFIER}, the provisioning profile
     * the archive validates against, and the
     * {@code CN1VpnTunnelExtensionIdentifier} the host plist carries -- which
     * is what {@code CN1Vpn.m} puts in
     * {@code NETunnelProviderProtocol.providerBundleIdentifier}. A tunnel
     * whose host names a different identifier than the extension ships under
     * starts nothing: iOS has no provider to associate the saved
     * configuration with, and says so nowhere.</p>
     *
     * <p>Mirrors {@code IPhoneBuilder.callDirectoryBundleId}, including both
     * refusals, for the same reasons that method gives at length.</p>
     */
    String bundleId(BuildRequest request) {
        String override = request.getArg(
                "ios.vpn.tunnel.buildSettings.PRODUCT_BUNDLE_IDENTIFIER", null);
        if (override != null && override.trim().length() > 0) {
            String value = override.trim();
            // NO Xcode substitution. Xcode expands $(...) for the target it
            // builds; nothing expands it here, so the same hint would be one
            // identifier in the target and the literal text in the host plist
            // and the profile check.
            if (value.indexOf("$(") >= 0 || value.indexOf("${") >= 0) {
                throw new BuildException("ios.vpn.tunnel.buildSettings"
                        + ".PRODUCT_BUNDLE_IDENTIFIER is '" + value + "'."
                        + " Xcode build-setting substitutions cannot be used"
                        + " here: the same string is written into the host"
                        + " Info.plist and checked against the provisioning"
                        + " profile, and neither expands it. Give the bundle"
                        + " identifier itself.");
            }
            // INSIDE the host's namespace. Apple requires an embedded app
            // extension's identifier to be the containing app's plus a
            // suffix and rejects the archive at embedded-binary validation
            // otherwise -- at upload, long after every check here has
            // passed, and with a message about the binary rather than about
            // this hint.
            String host = request.getPackageName();
            if (host != null && host.length() > 0
                    && !(value.startsWith(host + ".")
                            && value.length() > host.length() + 1)) {
                throw new BuildException("ios.vpn.tunnel.buildSettings"
                        + ".PRODUCT_BUNDLE_IDENTIFIER is '" + value + "',"
                        + " which is not inside '" + host + "'. An app"
                        + " extension's bundle identifier has to be the"
                        + " containing app's followed by a suffix, so Apple"
                        + " would reject the archive when it validates the"
                        + " embedded binary. Use something like '" + host
                        + ".vpntunnel'.");
            }
            return value;
        }
        return IOSVpnTunnelExtensionBuilder.bundleId(request.getPackageName());
    }

    /**
     * The build settings the extension target carries.
     *
     * <p>Package-visible and separate from the Ruby so a test can read them:
     * three of these are the difference between a target that builds and one
     * that cannot, and none of them is verifiable anywhere in CI.</p>
     *
     * @param request the build request
     * @param deviceFamily the TARGETED_DEVICE_FAMILY the host resolved
     */
    Map<String, String> buildSettings(BuildRequest request, String deviceFamily) {
        String name = IOSVpnTunnelExtensionBuilder.EXTENSION_NAME;
        Map<String, String> settings = new LinkedHashMap<String, String>();
        // Through the resolver, so an override is validated once and the
        // target, the profile check and the host plist cannot disagree.
        settings.put("PRODUCT_BUNDLE_IDENTIFIER", bundleId(request));
        settings.put("PRODUCT_NAME", "$(TARGET_NAME)");
        settings.put("INFOPLIST_FILE", name + "/Info.plist");
        settings.put("CODE_SIGN_ENTITLEMENTS", name + "/" + name + ".entitlements");
        settings.put("IPHONEOS_DEPLOYMENT_TARGET",
                IOSVpnTunnelExtensionBuilder.DEPLOYMENT_TARGET);
        settings.put("TARGETED_DEVICE_FAMILY", deviceFamily);
        settings.put("LD_RUNPATH_SEARCH_PATHS",
                "$(inherited) @executable_path/Frameworks"
                + " @executable_path/../../Frameworks");
        settings.put("SKIP_INSTALL", "YES");
        // OFF, matching the app target. The port and the translated sources
        // are manual-retain -- so is the generated provider, which releases
        // its watcher by hand -- and compiling them under ARC is not a
        // setting difference, it is a different language.
        settings.put("CLANG_ENABLE_OBJC_ARC", "NO");
        settings.put("CLANG_ENABLE_MODULES", "YES");
        // The point of the target, stated rather than inherited. Xcode
        // defaults an app-extension target to YES; saying it here is what
        // makes a future edit that removes it visible as a decision.
        settings.put("APPLICATION_EXTENSION_API_ONLY", "YES");
        // An .appex has no main(). NSExtensionMain is the entry point, and
        // the translated tree carries a main from the generated stub that
        // must never be the one the loader runs. This flag is what Xcode's
        // own app-extension template sets, for the same reason.
        settings.put("OTHER_LDFLAGS", "$(inherited) -e _NSExtensionMain");
        settings.put("GCC_PREFIX_HEADER",
                "$(SRCROOT)/" + request.getMainClass() + "-src/" + SRC_DIR
                + "/" + prefixHeader(request.getMainClass()));
        settings.put("USER_HEADER_SEARCH_PATHS",
                "$(SRCROOT)/" + request.getMainClass() + "-src/" + SRC_DIR
                + " $(SRCROOT)/" + IOSVpnTunnelExtensionBuilder.EXTENSION_NAME);
        settings.put("HEADER_SEARCH_PATHS",
                "$(inherited) $(SRCROOT)/" + request.getMainClass() + "-src/"
                + SRC_DIR);
        // CN1_VPN_TUNNEL is uncommented in the port's header for the APP
        // target, which is how the app half knows it has a tunnel to start.
        // The extension needs it for the same header, and it needs to know
        // it IS an extension: CN1_APP_EXTENSION is what a port source would
        // test if one ever has to.
        settings.put("GCC_PREPROCESSOR_DEFINITIONS",
                "$(inherited) CN1_VPN_TUNNEL=1 CN1_APP_EXTENSION=1");
        return settings;
    }
}
