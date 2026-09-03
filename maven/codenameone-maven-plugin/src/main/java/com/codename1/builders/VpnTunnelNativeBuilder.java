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
    private String tunnelClass;

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
     * Sources the extension target must not compile.
     *
     * <p>A mechanical list, not a policy one. Each entry was found by
     * scanning the iOS port's native sources for {@code UIApplicationMain}
     * and {@code [UIApplication sharedApplication]} -- the two
     * {@code NS_EXTENSION_UNAVAILABLE_IOS} spellings that an
     * {@code APPLICATION_EXTENSION_API_ONLY} target rejects -- plus the five
     * the watch target excludes for a different reason: a {@code .metal}
     * shader is compiled by the Metal compiler and a {@code .xib} is
     * Interface Builder data, so neither has a preprocessor to guard.</p>
     *
     * <p>They are excluded rather than guarded because the tunnel's
     * translation should not be reaching them in the first place. A guard
     * would let a tunnel that DOES reach one link against a stub and fail at
     * run time, in a process with no log and no UI to fail in; excluding
     * makes the same mistake an undefined symbol at build time, naming the
     * native it wanted.</p>
     *
     * <p>TO ADD AN ENTRY, scan rather than guess:</p>
     * <pre>
     * grep -l 'sharedApplication\\|UIApplicationMain' Ports/iOSPort/nativeSources/*.m
     * </pre>
     */
    static final String[] EXCLUDED_EXTENSION_SOURCES = {
        // The application shell and the natives that reach UIApplication.
        "IOSNative.m",
        "CodenameOne_GLAppDelegate.m",
        "CodenameOne_GLSceneDelegate.m",
        "CodenameOne_GLViewController.m",
        "UIWebViewEventDelegate.m",
        "AudioPlayer.m",
        "NetworkConnectionImpl.m",
        "cn1_debugger.m",
        "CN1AppleSignIn.m",
        "CN1OidcBrowser.m",
        "CN1WebAuthn.m",
        "CN1SmartHome.m",
        "GoogleConnectImpl.m",
        "CN1MacWindows.m",
        // No preprocessor to run; see above.
        "CN1MetalShaders.metal",
        "CodenameOne_GLViewController.xib", "MainWindow.xib",
        "CodenameOne_METALViewController.xib", "MainWindowMETAL.xib"
    };

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

    /** The application's VpnTunnel subclass, or null when disabled. */
    String getTunnelClass() {
        return tunnelClass;
    }

    /**
     * Checks that the named tunnel class is actually in the application.
     *
     * <p>Checked HERE, against the compiled classes, rather than left to the
     * generated stub's javac. Both refuse, but this one can say which hint
     * was wrong; javac would report an unresolvable symbol inside a source
     * file the developer never wrote.</p>
     *
     * @param classesDir the compiled application classes
     */
    void verifyTunnelClass(File classesDir) {
        File cls = new File(classesDir,
                tunnelClass.replace('.', File.separatorChar) + ".class");
        if (!cls.isFile()) {
            throw new BuildException(HINT_CLASS + " names "
                    + tunnelClass + ", which is not in this application."
                    + " The extension has to instantiate that class, so the"
                    + " name has to be the fully qualified one -- including"
                    + " the package -- of a VpnTunnel subclass this project"
                    + " compiles.");
        }
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
                + " */\n"
                + "public class " + stubClass + " {\n"
                + "    public static void main(String[] argv) {\n"
                + "        // The writer the provider implements. Installed"
                + " from Java so the\n"
                + "        // class -- and its native declaration -- are in"
                + " the tree at all.\n"
                + "        com.codename1.impl.ios.IOSExtensionTunnel"
                + ".install();\n"
                + "        // The application's tunnel. A DIRECT constructor"
                + " call: the provider\n"
                + "        // reaches it through the translated allocator,"
                + " and a name looked up\n"
                + "        // at run time would not survive obfuscation --"
                + " which is why\n"
                + "        // Class.forName is banned in the framework"
                + " itself.\n"
                + "        com.codename1.vpn.tunnel.VpnTunnel tunnel =\n"
                + "                new " + tunnelClass + "();\n"
                + "        // The four the provider calls, in the order it"
                + " calls them.\n"
                + "        com.codename1.impl.vpn.ExtensionTunnelHost.begin("
                + "tunnel, argv[0]);\n"
                + "        com.codename1.impl.vpn.ExtensionTunnelHost.buffer("
                + "0);\n"
                + "        com.codename1.impl.vpn.ExtensionTunnelHost"
                + ".received(0);\n"
                + "        com.codename1.impl.vpn.ExtensionTunnelHost.end("
                + "0);\n"
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
            boolean source = name.endsWith(".m") || name.endsWith(".c")
                    || name.endsWith(".mm") || name.endsWith(".cpp")
                    || name.endsWith(".cc");
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
            if (source && !isExcluded(name)) {
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

    /** Whether a translated source is one the extension target must not compile. */
    static boolean isExcluded(String name) {
        for (String excluded : EXCLUDED_EXTENSION_SOURCES) {
            if (excluded.equals(name)) {
                return true;
            }
        }
        return false;
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
        settings.put("PRODUCT_BUNDLE_IDENTIFIER",
                IOSVpnTunnelExtensionBuilder.bundleId(request.getPackageName()));
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
