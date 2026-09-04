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
package com.codename1.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates the iOS packet-tunnel app extension behind
 * {@code com.codename1.vpn.tunnel}.
 *
 * <p>Called by {@code VpnTunnelNativeBuilder} for a project that sets
 * {@code ios.vpn.tunnel=true} and names its tunnel class in
 * {@code ios.vpn.tunnel.class}. Nothing else reaches it, and that gate is
 * deliberate: the entitlement this extension carries is one Apple grants
 * case by case, so the hint is also the project asserting it holds the
 * grant.</p>
 *
 * <p>This file used to open by saying NOTHING CALLS THIS, and for two rounds
 * that was true -- the generator was written and left without a caller,
 * because the piece believed to be missing was "a ByteCodeTranslator
 * translation rooted at the tunnel". That translation already existed and
 * had shipped: the watch slice is translated by a SECOND translator pass
 * rooted at its own entry point, and {@code VpnTunnelNativeBuilder} does the
 * same thing rooted at a generated tunnel stub. Rooting it there is what
 * makes an app-extension target possible at all -- the extension carries
 * what the tunnel reaches, so the port's UIKit natives, whose
 * {@code UIApplicationMain} and {@code [UIApplication sharedApplication]}
 * an {@code APPLICATION_EXTENSION_API_ONLY} target may not compile, are not
 * in it.</p>
 *
 * <p><b>Nothing in CI compiles what this writes</b>, here or in the
 * CodenameOne repository -- neither runs an Objective-C compiler. The output
 * is checked instead by generating it and running clang against the real iOS
 * SDK with {@code -fapplication-extension}, which is what
 * {@code scripts/check-vpn-tunnel-extension-compiles.sh} does on a
 * machine that has Xcode; it skips where there is none. Run it after
 * changing this file: a forward-declaration break that would have failed the
 * generated target's first build was found sitting here exactly that way.</p>
 *
 * <hr>
 *
 * <p>The iOS packet-tunnel app extension behind
 * {@code com.codename1.vpn.tunnel}.</p>
 *
 * <p>This one differs from every other extension this builder generates:
 * <b>it hosts a virtual machine</b>. The others are small Objective-C
 * handlers that answer the system and exit. A packet tunnel runs the
 * application's own {@code VpnTunnel} subclass, which is Java, so the
 * extension target is translated the way the app target is -- by the same
 * translator, from its own root -- and the generated provider below boots
 * the VM before handing packets to it.</p>
 *
 * <p>An earlier version of this framework recorded that this could not be
 * done -- that a Network Extension is "a separate process with no ParparVM
 * in it". The premise is half right, and the half that matters is the half
 * it got wrong: the extension is a target THIS BUILD produces, so what is in
 * it is this build's decision. {@code VpnTunnelNativeBuilder} runs a second
 * translator pass rooted at a stub that reaches the tunnel and nothing else,
 * so the VM in the extension is carried WITHOUT the application shell whose
 * natives call UIKit an extension may not touch.</p>
 *
 * <p>What IS true either way is that it shares nothing with the app: no
 * statics, no {@code Display}, no open connections. Everything the tunnel
 * needs travels in {@code TunnelSetup.data} and arrives as the provider
 * configuration. That is not a style rule -- a tunnel that reaches for the
 * application's own classes drags them into the rooted translation, and the
 * ones backed by the port's UIKit natives cannot be compiled into an app
 * extension at all, so it fails the extension's link rather than misbehaving
 * at run time.</p>
 *
 * <p><b>The entitlement is not injected.</b>
 * {@code com.apple.developer.networking.networkextension} is granted by Apple
 * case by case, unlike Personal VPN which any paid account can switch on. An
 * App ID without the grant fails codesigning with a message naming the
 * entitlement and not the reason it appeared, so the build hint that turns
 * this on is also the project asserting it holds the grant.</p>
 *
 * <p><b>Keep this file in sync with
 * {@code com.codename1.build.daemon.IOSVpnTunnelExtensionBuilder}.</b></p>
 */
public final class IOSVpnTunnelExtensionBuilder {

    /** The generated target's name. */
    public static final String EXTENSION_NAME = "CN1VpnTunnel";

    /**
     * What tells iOS this is a packet tunnel rather than some other provider.
     *
     * <p>The value is not a class name: iOS reads it to decide which
     * extension point the bundle implements, and a bundle whose
     * NSExtensionPointIdentifier does not match is never started -- with no
     * error anywhere, because nothing tried to start it.</p>
     */
    public static final String EXTENSION_POINT =
            "com.apple.networkextension.packet-tunnel";

    /**
     * The deployment target.
     *
     * <p>{@code NEPacketTunnelProvider} is iOS 9, but the provider below uses
     * {@code NEPacketTunnelNetworkSettings} with IPv4 and IPv6 settings and
     * the {@code providerConfiguration} dictionary, which settle at 10. There
     * is no reason to go lower: the app target's own floor is well past it.
     */
    public static final String DEPLOYMENT_TARGET = "12.0";

    private IOSVpnTunnelExtensionBuilder() {
    }

    /**
     * The files that make up the generated target.
     *
     * @param packageName  the host app's bundle identifier
     * @param displayName  what the system shows for the extension
     * @param shortVersion CFBundleShortVersionString, matching the host
     * @param bundleVersion CFBundleVersion, matching the host
     * @param tunnelClass  the application's VpnTunnel subclass, in binary
     *                     form -- {@code com.example.MyTunnel}
     * @return file name to contents
     */
    public static Map<String, byte[]> buildFileMap(String packageName,
            String displayName, String shortVersion, String bundleVersion,
            String tunnelClass, boolean convertSignalsToExceptions) {
        Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
        files.put("CN1VpnTunnelProvider.h", utf8(providerHeader()));
        files.put("CN1VpnTunnelProvider.m", utf8(providerSource(tunnelClass,
                convertSignalsToExceptions)));
        files.put("Info.plist", utf8(infoPlist(displayName, shortVersion,
                bundleVersion)));
        files.put(EXTENSION_NAME + ".entitlements", utf8(entitlements()));
        return files;
    }

    /** The bundle identifier the generated target signs under. */
    public static String bundleId(String packageName) {
        return packageName + ".vpntunnel";
    }

    /**
     * The extension's entitlements.
     *
     * <p>{@code packet-tunnel-provider} is the value that makes this a
     * packet tunnel; an extension without it is not started, and one whose
     * App ID has not been GRANTED the entitlement fails codesigning. The
     * grant is Apple's to give case by case, which is why the build only
     * writes this for a project that said it holds one.</p>
     *
     * @return the entitlements plist
     */
    static String entitlements() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\""
                + " \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                + "<plist version=\"1.0\">\n<dict>\n"
                + "    <key>com.apple.developer.networking.networkextension"
                + "</key>\n"
                + "    <array>\n"
                + "        <string>packet-tunnel-provider</string>\n"
                + "    </array>\n"
                + "</dict>\n</plist>\n";
    }

    static String providerHeader() {
        return "#import <NetworkExtension/NetworkExtension.h>\n"
                + "\n"
                + "@interface CN1VpnTunnelProvider : NEPacketTunnelProvider\n"
                + "@end\n";
    }

    /**
     * The extension's principal class.
     *
     * <p>Four things here are requirements rather than choices.</p>
     *
     * <p><b>The VM is started once, and not by UIApplicationMain.</b> An
     * extension has no application object. The generated app target's
     * {@code main} calls {@code initConstantPool} and then
     * {@code UIApplicationMain}; this calls the same initialisation and then
     * nothing, because the system already owns the run loop.</p>
     *
     * <p><b>Network settings must be applied before packets flow.</b>
     * {@code setTunnelNetworkSettings} is what makes the utun interface real;
     * reading packets before its completion handler runs returns nothing, for
     * ever, with no error.</p>
     *
     * <p><b>The read is re-armed from inside its own handler.</b>
     * {@code readPacketsWithCompletionHandler} delivers ONE batch. An
     * extension that does not ask again after each batch stops receiving
     * traffic and looks like a tunnel that hung.</p>
     *
     * <p><b>The extension is memory-capped, hard.</b> The packets go straight
     * into the pooled buffers the Java side already owns rather than being
     * copied into NSData objects first.</p>
     */
    static String providerSource(String tunnelClass,
            boolean convertSignalsToExceptions) {
        String mangled = mangle(tunnelClass);
        StringBuilder sb = new StringBuilder();
        sb.append("#import \"CN1VpnTunnelProvider.h\"\n");
        sb.append("#include \"cn1_globals.h\"\n");
        sb.append("#include \"com_codename1_impl_vpn_ExtensionTunnelHost.h\"\n");
        sb.append("#include \"com_codename1_impl_ios_IOSExtensionTunnel.h\"\n");
        sb.append("#include <stdatomic.h>\n");
        if (convertSignalsToExceptions) {
            sb.append("#include <signal.h>\n");
        }
        sb.append("\n");
        sb.append("// The application's tunnel, named by the build. Reached\n");
        sb.append("// through the translated allocator rather than by\n");
        sb.append("// reflection: this app is obfuscated by the time it gets\n");
        sb.append("// here, so a name looked up at run time would not be\n");
        sb.append("// there -- which is the same reason Class.forName is\n");
        sb.append("// banned in the framework itself.\n");
        sb.append("// THE ABI ParparVM actually emits, which these two\n");
        sb.append("// declarations got wrong twice over. __NEW_X takes the\n");
        sb.append("// thread state -- it uses it for class initialisation and\n");
        sb.append("// for the GC allocation -- and an empty parameter list is\n");
        sb.append("// an old-style declaration that compiles and then leaves\n");
        sb.append("// the argument register unset on arm64. And the\n");
        sb.append("// no-argument constructor is X___INIT____, not X_ctor__,\n");
        sb.append("// which is a symbol the translation never defines: the\n");
        sb.append("// extension would not have linked.\n");
        if (convertSignalsToExceptions) {
            sb.append("// The two the signal handler allocates. Reachable\n");
            sb.append("// because the generated stub names them; see\n");
            sb.append("// VpnTunnelNativeBuilder.writeStubSource.\n");
            sb.append("extern JAVA_OBJECT"
                    + " __NEW_INSTANCE_java_lang_NullPointerException(\n");
            sb.append("        CODENAME_ONE_THREAD_STATE);\n");
            sb.append("extern JAVA_OBJECT"
                    + " __NEW_INSTANCE_java_lang_RuntimeException(\n");
            sb.append("        CODENAME_ONE_THREAD_STATE);\n");
        }
        sb.append("extern JAVA_OBJECT __NEW_").append(mangled)
                .append("(CODENAME_ONE_THREAD_STATE);\n");
        sb.append("extern JAVA_VOID ").append(mangled)
                .append("___INIT____(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);\n");
        sb.append("\n");
        sb.append("static CN1VpnTunnelProvider *cn1tnProvider = nil;\n");
        sb.append("\n");
        sb.append("/// Which start put cn1tnProvider there.\n");
        sb.append("///\n");
        sb.append("/// Written and read under the same lock as the pointer,\n");
        sb.append("/// because the two are one fact. The counter below cannot\n");
        sb.append("/// stand in for it: a replacement start claims its\n");
        sb.append("/// generation before it publishes, so between the two the\n");
        sb.append("/// counter says somebody newer owns the slot while the\n");
        sb.append("/// slot still holds the old provider -- and a stop that\n");
        sb.append("/// consulted the counter left that provider in place to be\n");
        sb.append("/// released by NE with the global still naming it.\n");
        sb.append("static int cn1tnProviderGeneration = 0;\n");
        sb.append("\n");
        sb.append("// Which START the packet reads belong to.\n");
        sb.append("//\n");
        sb.append("// The extension process outlives a tunnel: NE starts and\n");
        sb.append("// stops this provider without tearing the process down, so\n");
        sb.append("// a read armed by one start can complete after it. The\n");
        sb.append("// handler used to deliver whatever it got and re-arm\n");
        sb.append("// itself unconditionally, and the only guard was a null\n");
        sb.append("// buffer meaning \"no tunnel running\" -- which stops\n");
        sb.append("// nothing once a NEW tunnel is running. So a stop followed\n");
        sb.append("// by a start left the old read alive, feeding packets\n");
        sb.append("// captured on the previous link into the new tunnel and\n");
        sb.append("// re-arming beside the new reader, two of them competing\n");
        sb.append("// for one flow from then on.\n");
        sb.append("//\n");
        sb.append("// ATOMIC because the two sides genuinely differ: the\n");
        sb.append("// counter is bumped from NE's start and stop callbacks and\n");
        sb.append("// read from the packet-flow completion handler, which is\n");
        sb.append("// not documented to be the same queue. A plain int would\n");
        sb.append("// leave the stale reader reading a cached generation --\n");
        sb.append("// which is the bug, not a smaller version of it.\n");
        sb.append("static atomic_int cn1tnReadGeneration;\n");
        sb.append("\n");
        sb.append("// WHAT THE GENERATION DOES AND DOES NOT PROMISE, because\n");
        sb.append("// the difference has been raised more than once.\n");
        sb.append("//\n");
        sb.append("// Every check against it is check-then-act: a stop can\n");
        sb.append("// always land between the load and the call after it.\n");
        sb.append("// What closes the gap is not the check but the fact that\n");
        sb.append("// the generation TRAVELS -- into buffer(), received() and\n");
        sb.append("// the writer -- so the Java side answers for the start\n");
        sb.append("// the caller belongs to or answers nothing. A packet\n");
        sb.append("// cannot therefore cross from one tunnel to the next in\n");
        sb.append("// either direction, however the two threads interleave.\n");
        sb.append("//\n");
        sb.append("// The checks here are still worth having: they stop a\n");
        sb.append("// batch early and keep a dead read from re-arming, which\n");
        sb.append("// is work avoided rather than correctness.\n");
        sb.append("//\n");
        sb.append("// Two orderings carry the rest of it, and both are\n");
        sb.append("// load-bearing. The provider pointer is published AFTER\n");
        sb.append("// this counter is claimed, so a pointer a writer can see\n");
        sb.append("// always belongs to a generation the writer can check;\n");
        sb.append("// and the writer snapshots that pointer BEFORE it checks,\n");
        sb.append("// so it can never use one that appeared later. On the\n");
        sb.append("// Java side ExtensionTunnelHost.begin refuses a start\n");
        sb.append("// older than the one already installed, under the lock\n");
        sb.append("// that publishes it, so a completion that lost its race\n");
        sb.append("// cannot replace a live tunnel with a cancelled one.\n");
        sb.append("//\n");
        sb.append("// What remains open is that such a completion still\n");
        sb.append("// reports success to NE for a start that was already\n");
        sb.append("// cancelled. Nothing follows from it: no host is\n");
        sb.append("// installed, no read is armed for a live generation and\n");
        sb.append("// no write is accepted. A lock is not the better trade\n");
        sb.append("// for that last inch either -- delivery runs the\n");
        sb.append("// application's onPacket, and holding a lock across it\n");
        sb.append("// would make stopTunnelWithReason wait behind user code\n");
        sb.append("// for a completion handler NE kills providers over.\n");
        sb.append("//\n");
        sb.append("// A lock is NOT the better trade here, and this is the\n");
        sb.append("// reason rather than an opinion. Delivery runs the\n");
        sb.append("// application's onPacket, which may take as long as it\n");
        sb.append("// likes; holding a lock across it would make\n");
        sb.append("// stopTunnelWithReason wait behind user code for its\n");
        sb.append("// completion handler, and NE kills a provider that does\n");
        sb.append("// not answer. Trading a window bounded by an XPC round\n");
        sb.append("// trip -- a stop AND a full start, settings included,\n");
        sb.append("// inside the gap between two instructions -- for a stop\n");
        sb.append("// that can be blocked by an app callback is a worse\n");
        sb.append("// extension, not a safer one.\n");
        sb.append("\n");
        sb.append("// FORWARD DECLARATIONS for the helpers below.\n");
        sb.append("//\n");
        sb.append("// The implementation calls them and their definitions\n");
        sb.append("// follow it, which reads well and does not compile: C99\n");
        sb.append("// removed implicit declarations and current clang makes\n");
        sb.append("// that an error, so the generated target failed on its\n");
        sb.append("// own first build. Nothing in this repository compiles\n");
        sb.append("// this file -- it is written here and built by Xcode on a\n");
        sb.append("// machine none of our tests run on -- which is the whole\n");
        sb.append("// reason a break like this could sit here unseen.\n");
        sb.append("static NEPacketTunnelNetworkSettings *cn1tnSettings(\n");
        sb.append("        NSString *wire);\n");
        sb.append("static JAVA_INT cn1tnReason(NEProviderStopReason reason);\n");
        sb.append("\n");
        if (convertSignalsToExceptions) {
            sb.append("/// The SIGSEGV-to-NullPointerException handler, which\n");
            sb.append("/// this target would otherwise be without.\n");
            sb.append("///\n");
            sb.append("/// ParparVM leans on it: a field read through null\n");
            sb.append("/// faults rather than checking, unless\n");
            sb.append("/// ios.fieldNullChecks is on, and a call through null\n");
            sb.append("/// faults in every configuration. The app target gets\n");
            sb.append("/// this from installSignalHandlers in\n");
            sb.append("/// CodenameOne_GLAppDelegate.m -- a UIApplication\n");
            sb.append("/// delegate, which an extension may not compile -- and\n");
            sb.append("/// the watch runtime mirrors it in CN1WatchRuntime.m\n");
            sb.append("/// for exactly this reason. Without it a null\n");
            sb.append("/// dereference anywhere in the tunnel's code kills the\n");
            sb.append("/// extension instead of arriving at the Throwable the\n");
            sb.append("/// tunnel host already catches, and iOS tears the VPN\n");
            sb.append("/// down with it.\n");
            sb.append("///\n");
            sb.append("/// Omitted when ios.convertSignalsToExceptions=false,\n");
            sb.append("/// which is the same hint that comments the call out\n");
            sb.append("/// of the app target: a developer who wants the fault\n");
            sb.append("/// to stay a fault gets that here too.\n");
            sb.append("static void cn1tnSignalHandler(int sig) {\n");
            sb.append("    if (sig == SIGSEGV || sig == SIGBUS) {\n");
            sb.append("        throwException(getThreadLocalData(),\n");
            sb.append("                __NEW_INSTANCE_java_lang_NullPointerException(\n");
            sb.append("                        getThreadLocalData()));\n");
            sb.append("    } else {\n");
            sb.append("        throwException(getThreadLocalData(),\n");
            sb.append("                __NEW_INSTANCE_java_lang_RuntimeException(\n");
            sb.append("                        getThreadLocalData()));\n");
            sb.append("    }\n");
            sb.append("}\n");
            sb.append("\n");
            sb.append("static void cn1tnInstallSignalHandlers(void) {\n");
            sb.append("    signal(SIGABRT, cn1tnSignalHandler);\n");
            sb.append("    signal(SIGILL, cn1tnSignalHandler);\n");
            sb.append("    signal(SIGSEGV, cn1tnSignalHandler);\n");
            sb.append("    signal(SIGFPE, cn1tnSignalHandler);\n");
            sb.append("    signal(SIGBUS, cn1tnSignalHandler);\n");
            sb.append("    signal(SIGPIPE, cn1tnSignalHandler);\n");
            sb.append("}\n");
            sb.append("\n");
        }
        sb.append("@implementation CN1VpnTunnelProvider {\n");
        sb.append("    /// Which start this provider published, or zero if it\n");
        sb.append("    /// never got that far.\n");
        sb.append("    ///\n");
        sb.append("    /// The stop needs the generation THIS object owns,\n");
        sb.append("    /// and the counter cannot tell it: a replacement that\n");
        sb.append("    /// has already claimed leaves the counter reading the\n");
        sb.append("    /// replacement's number, and a stop that read it back\n");
        sb.append("    /// tore down the tunnel that had taken its place.\n");
        sb.append("    /// Written and read under the same lock as the slot.\n");
        sb.append("    ///\n");
        sb.append("    /// ONE claim, not a stack of them, because NE does not\n");
        sb.append("    /// overlap sessions on a provider: it calls\n");
        sb.append("    /// stopTunnelWithReason and waits for that completion\n");
        sb.append("    /// handler before it starts another. Everything else\n");
        sb.append("    /// this file guards -- a settings completion resuming\n");
        sb.append("    /// after a stop, a read outstanding across a restart --\n");
        sb.append("    /// is OUR asynchrony inside one session, which the\n");
        sb.append("    /// generation carries. Raised in review as needing\n");
        sb.append("    /// per-session bookkeeping for a start that publishes\n");
        sb.append("    /// while an earlier stop is still running; that is the\n");
        sb.append("    /// system breaking its own sequence, and the answer to\n");
        sb.append("    /// it is the line in the stop that gives this claim up,\n");
        sb.append("    /// so a second stop finds nothing of anyone else's to\n");
        sb.append("    /// tear down.\n");
        sb.append("    int cn1tnMine;\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("- (void)startTunnelWithOptions:(NSDictionary *)options\n");
        sb.append("        completionHandler:(void (^)(NSError *))completionHandler {\n");
        sb.append("    // ONCE per process. The extension is started and\n");
        sb.append("    // stopped repeatedly within one process lifetime, and\n");
        sb.append("    // initialising the VM twice would reset every static\n");
        sb.append("    // the tunnel had.\n");
        sb.append("    static dispatch_once_t once;\n");
        sb.append("    dispatch_once(&once, ^{\n");
        sb.append("        initConstantPool();\n");
        if (convertSignalsToExceptions) {
            sb.append("        cn1tnInstallSignalHandlers();\n");
        }
        sb.append("    });\n");
        sb.append("    NSString *wire = @\"\";\n");
        sb.append("    NETunnelProviderProtocol *proto =\n");
        sb.append("            (NETunnelProviderProtocol *)self.protocolConfiguration;\n");
        sb.append("    if ([proto isKindOfClass:[NETunnelProviderProtocol class]]) {\n");
        sb.append("        id raw = [proto.providerConfiguration\n");
        sb.append("                objectForKey:@\"cn1TunnelSetup\"];\n");
        sb.append("        if ([raw isKindOfClass:[NSString class]]) {\n");
        sb.append("            wire = (NSString *)raw;\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("    // THIS start, claimed before anything asynchronous\n");
        sb.append("    // is issued. NE stops a provider whose start is still\n");
        sb.append("    // in flight -- a user toggling the switch back is\n");
        sb.append("    // enough -- and the settings completion below used to\n");
        sb.append("    // run regardless: it built the tunnel, told Java to\n");
        sb.append("    // begin, armed a read and reported the start a\n");
        sb.append("    // success, all after the stop. The tunnel then ran\n");
        sb.append("    // with cn1tnProvider nil, so every packet it forwarded\n");
        sb.append("    // was dropped with nothing to say so.\n");
        sb.append("    //\n");
        sb.append("    // The read generation doubles as the start's identity,\n");
        sb.append("    // which is what makes one counter enough: bumping it\n");
        sb.append("    // here also invalidates a read still outstanding from\n");
        sb.append("    // the previous tunnel.\n");
        sb.append("    int cn1tnStart =\n");
        sb.append("            atomic_fetch_add(&cn1tnReadGeneration, 1) + 1;\n");
        sb.append("    // PUBLISHED AFTER the generation is claimed, and that\n");
        sb.append("    // order is the whole of what makes the writer's check\n");
        sb.append("    // sound. Set first, a restart made the new provider\n");
        sb.append("    // visible while the counter still read the old\n");
        sb.append("    // generation -- so a write from the old tunnel found a\n");
        sb.append("    // generation that still matched and a provider that\n");
        sb.append("    // was already the new one, and went out on its link.\n");
        sb.append("    // Under the lock the writer takes, which is what\n");
        sb.append("    // lets it retain what it reads. This target compiles\n");
        sb.append("    // without ARC -- the translated sources cannot be\n");
        sb.append("    // built any other way -- so the global is a bare\n");
        sb.append("    // pointer, and a bare pointer is no promise that the\n");
        sb.append("    // object is still there.\n");
        sb.append("    @synchronized ([CN1VpnTunnelProvider class]) {\n");
        sb.append("        // STILL THIS START'S to give. Claiming and\n");
        sb.append("        // publishing are two steps, and a start suspended\n");
        sb.append("        // between them used to resume long after a stop\n");
        sb.append("        // and a restart had been and gone -- and put its\n");
        sb.append("        // own stopped provider in the slot, over the one\n");
        sb.append("        // the running tunnel had published. The writer\n");
        sb.append("        // then found a provider whose generation nothing\n");
        sb.append("        // compared, and the live tunnel's packets went out\n");
        sb.append("        // on a link that was already down.\n");
        sb.append("        if (cn1tnStart == atomic_load(&cn1tnReadGeneration)) {\n");
        sb.append("            cn1tnProvider = self;\n");
        sb.append("            cn1tnProviderGeneration = cn1tnStart;\n");
        sb.append("            cn1tnMine = cn1tnStart;\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("    // The settings the system needs BEFORE any packet\n");
        sb.append("    // moves; see the note on this class.\n");
        sb.append("    NEPacketTunnelNetworkSettings *settings =\n");
        sb.append("            cn1tnSettings(wire);\n");
        sb.append("    if (settings == nil) {\n");
        sb.append("        // The setup is unreadable. This process cannot\n");
        sb.append("        // answer the application -- it is a separate\n");
        sb.append("        // extension, and Tunnels.start() has long since\n");
        sb.append("        // returned -- so failing the START is the whole of\n");
        sb.append("        // what it can say, and it is the right thing to\n");
        sb.append("        // say: a tunnel that comes up on settings nobody\n");
        sb.append("        // asked for is worse than one that does not come\n");
        sb.append("        // up. Tunnels.start() validates the same record\n");
        sb.append("        // before it is saved, so in an ordinary start this\n");
        sb.append("        // is unreachable; it is on-demand relaunches from a\n");
        sb.append("        // configuration an OLDER version of the app saved\n");
        sb.append("        // that arrive here unchecked.\n");
        sb.append("        //\n");
        sb.append("        // The code mirrors VpnError.INVALID_CONFIGURATION\n");
        sb.append("        // so a system log reads the same as the API would,\n");
        sb.append("        // but nothing consumes it: this NSError goes to\n");
        sb.append("        // iOS, not to Java.\n");
        sb.append("        [self cn1ForgetIfCurrent:cn1tnStart];\n");
        sb.append("        completionHandler([NSError\n");
        sb.append("                errorWithDomain:@\"com.codename1.vpn\"\n");
        sb.append("                code:2\n");
        sb.append("                userInfo:[NSDictionary\n");
        sb.append("                        dictionaryWithObject:\n");
        sb.append("                                @\"The tunnel setup is not readable\"\n");
        sb.append("                        forKey:NSLocalizedDescriptionKey]]);\n");
        sb.append("        return;\n");
        sb.append("    }\n");
        sb.append("    // NOT serialised against an earlier start's request,\n");
        sb.append("    // and it does not need to be. A request still pending\n");
        sb.append("    // when its own session ends belongs to a session the\n");
        sb.append("    // system has already torn down, and the next session\n");
        sb.append("    // on this provider does not begin until the stop that\n");
        sb.append("    // ended this one has returned -- so there is no second\n");
        sb.append("    // start whose settings this could overwrite. What the\n");
        sb.append("    // completion below guards is the other half: this\n");
        sb.append("    // block resuming after that stop, which is ours to get\n");
        sb.append("    // wrong and is what the generation check refuses.\n");
        sb.append("    [self setTunnelNetworkSettings:settings\n");
        sb.append("            completionHandler:^(NSError *error) {\n");
        sb.append("        if (error != nil) {\n");
        sb.append("            [self cn1ForgetIfCurrent:cn1tnStart];\n");
        sb.append("            completionHandler(error);\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        if (cn1tnStart != atomic_load(&cn1tnReadGeneration)) {\n");
        sb.append("            // Stopped while these settings were pending.\n");
        sb.append("            // Nothing is started: no tunnel is built, Java\n");
        sb.append("            // is not told to begin, and no read is armed.\n");
        sb.append("            // The handler is still called exactly once,\n");
        sb.append("            // because NE requires that -- with an error,\n");
        sb.append("            // since this start did not happen.\n");
        sb.append("            [self cn1ForgetIfCurrent:cn1tnStart];\n");
        sb.append("            completionHandler([NSError\n");
        sb.append("                    errorWithDomain:@\"com.codename1.vpn\"\n");
        sb.append("                    code:1\n");
        sb.append("                    userInfo:[NSDictionary\n");
        sb.append("                            dictionaryWithObject:\n");
        sb.append("                                    @\"The tunnel was stopped before it started\"\n");
        sb.append("                            forKey:NSLocalizedDescriptionKey]]);\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        CODENAME_ONE_THREAD_STATE = getThreadLocalData();\n");
        sb.append("        // The writer FIRST: the tunnel's onStart may\n");
        sb.append("        // forward a packet, and a tunnel that forwards\n");
        sb.append("        // before the writer is installed drops it with\n");
        sb.append("        // nothing to say so.\n");
        sb.append("        //\n");
        sb.append("        // Installing before begin decides the winner is\n");
        sb.append("        // safe because the install itself will not go\n");
        sb.append("        // backwards: ExtensionTunnelHost.setWriter takes\n");
        sb.append("        // the generation and ignores one older than the\n");
        sb.append("        // writer already installed. A stale completion\n");
        sb.append("        // resuming here therefore cannot leave the live\n");
        sb.append("        // tunnel forwarding through a writer whose own\n");
        sb.append("        // check rejects every packet -- raised in review\n");
        sb.append("        // against this call, where the guard is not\n");
        sb.append("        // visible, and it lives on the other side of the\n");
        sb.append("        // boundary in the CodenameOne repository.\n");
        sb.append("        com_codename1_impl_ios_IOSExtensionTunnel_install___int(\n");
        sb.append("                threadStateData, cn1tnStart);\n");
        sb.append("        JAVA_OBJECT tunnel = __NEW_").append(mangled)
                .append("(threadStateData);\n");
        sb.append("        if (tunnel != JAVA_NULL\n");
        sb.append("                && threadStateData->exception == JAVA_NULL) {\n");
        sb.append("            ").append(mangled)
                .append("___INIT____(threadStateData, tunnel);\n");
        sb.append("        }\n");
        sb.append("        if (tunnel == JAVA_NULL\n");
        sb.append("                || threadStateData->exception != JAVA_NULL) {\n");
        sb.append("            // A CONSTRUCTOR that threw, or a class\n");
        sb.append("            // initializer that did. There is no java try\n");
        sb.append("            // region around a call made from here, and\n");
        sb.append("            // throwException with no handler on the thread\n");
        sb.append("            // RETURNS -- it records the exception and lets\n");
        sb.append("            // the caller carry on. So this went on to begin\n");
        sb.append("            // with a half-built tunnel and reported the\n");
        sb.append("            // start a success.\n");
        sb.append("            //\n");
        sb.append("            // Cleared as well as reported: the next thing\n");
        sb.append("            // this thread runs would otherwise find a\n");
        sb.append("            // pending exception from a start that is over.\n");
        sb.append("            threadStateData->exception = JAVA_NULL;\n");
        sb.append("            [self cn1ForgetIfCurrent:cn1tnStart];\n");
        sb.append("            completionHandler([NSError\n");
        sb.append("                    errorWithDomain:@\"com.codename1.vpn\"\n");
        sb.append("                    code:2\n");
        sb.append("                    userInfo:[NSDictionary\n");
        sb.append("                            dictionaryWithObject:\n");
        sb.append("                                    @\"The tunnel class could not be constructed\"\n");
        sb.append("                            forKey:NSLocalizedDescriptionKey]]);\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        // fromNSString is the TRANSLATOR's, not the\n");
        sb.append("        // port's -- cn1_globals.m defines it under\n");
        sb.append("        // #if defined(__APPLE__) && defined(__OBJC__),\n");
        sb.append("        // and IOSNative.m only extern-declares it, as\n");
        sb.append("        // CN1Vpn.m and a dozen other port sources do. It\n");
        sb.append("        // therefore arrives with the translation and is\n");
        sb.append("        // compiled into this target, which is why the\n");
        sb.append("        // extension can leave every port native out and\n");
        sb.append("        // still convert a string. (Raised as a link error\n");
        sb.append("        // in review on the premise that it was the\n");
        sb.append("        // port's; it is not.)\n");
        sb.append("        JAVA_BOOLEAN cn1tnBegan =\n");
        sb.append("                com_codename1_impl_vpn_ExtensionTunnelHost_begin___java_lang_Object_java_lang_String_int_R_boolean(\n");
        sb.append("                        threadStateData, tunnel,\n");
        sb.append("                        fromNSString(threadStateData, wire),\n");
        sb.append("                        cn1tnStart);\n");
        sb.append("        if (!cn1tnBegan) {\n");
        sb.append("            // A newer start owns the extension. The check\n");
        sb.append("            // at the top of this block is not enough on\n");
        sb.append("            // its own -- a stop and a restart can land\n");
        sb.append("            // between it and here -- and begin is where\n");
        sb.append("            // the decision is actually made, under the\n");
        sb.append("            // lock that publishes the host. Arming a read\n");
        sb.append("            // anyway put a second reader on the flow for a\n");
        sb.append("            // tunnel that does not exist, which can take a\n");
        sb.append("            // batch the live tunnel was owed and drop it\n");
        sb.append("            // at its own generation check.\n");
        sb.append("            [self cn1ForgetIfCurrent:cn1tnStart];\n");
        sb.append("            completionHandler([NSError\n");
        sb.append("                    errorWithDomain:@\"com.codename1.vpn\"\n");
        sb.append("                    code:2\n");
        sb.append("                    userInfo:[NSDictionary\n");
        sb.append("                            dictionaryWithObject:\n");
        sb.append("                                    @\"The tunnel was stopped while it was starting\"\n");
        sb.append("                            forKey:NSLocalizedDescriptionKey]]);\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        // Armed for THIS start, with the generation it\n");
        sb.append("        // claimed above rather than a fresh one -- bumping\n");
        sb.append("        // again here would invalidate the very start this\n");
        sb.append("        // completion belongs to. A read belonging to an\n");
        sb.append("        // earlier tunnel may still be outstanding, and it\n");
        sb.append("        // stops at its own generation check rather than\n");
        sb.append("        // delivering into the tunnel just installed.\n");
        sb.append("        [self cn1ReadPacketsForGeneration:cn1tnStart];\n");
        sb.append("        completionHandler(nil);\n");
        sb.append("    }];\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("/// Arms the next batch, and re-arms from inside the\n");
        sb.append("/// handler; see the note on this class.\n");
        sb.append("///\n");
        sb.append("/// Carries the generation of the start it belongs to. See\n");
        sb.append("/// cn1tnReadGeneration: without it a read outstanding\n");
        sb.append("/// across a stop delivered its packets into the NEXT\n");
        sb.append("/// tunnel and re-armed alongside that tunnel's own reader.\n");
        sb.append("/// Gives up this provider's claim on the global, if it\n");
        sb.append("/// still has one.\n");
        sb.append("///\n");
        sb.append("/// A start that FAILS is torn down without\n");
        sb.append("/// stopTunnelWithReason -- the tunnel never started, so\n");
        sb.append("/// there is nothing for NE to stop -- and the global went\n");
        sb.append("/// on naming the provider it had published. Without ARC\n");
        sb.append("/// that is a bare pointer to an object the system is free\n");
        sb.append("/// to dispose, and the next writer retained it before it\n");
        sb.append("/// ever looked at a generation.\n");
        sb.append("///\n");
        sb.append("/// Conditional, because a newer start may already own the\n");
        sb.append("/// global: this clears its own claim, never somebody\n");
        sb.append("/// else's.\n");
        sb.append("///\n");
        sb.append("/// The GENERATION as well as the pointer. NE may hand a\n");
        sb.append("/// restart to the same provider object, and then the\n");
        sb.append("/// pointer alone says yes to a stale completion: the\n");
        sb.append("/// newer start published the same self, and clearing it\n");
        sb.append("/// would leave the running tunnel writing through nil and\n");
        sb.append("/// dropping every packet.\n");
        sb.append("///\n");
        sb.append("/// The generation asked for is the SLOT's, not the\n");
        sb.append("/// counter's. A replacement start claims its generation\n");
        sb.append("/// before it publishes, and in that window the counter\n");
        sb.append("/// answers for a provider that is not in the slot yet --\n");
        sb.append("/// so a stop that asked it declined to clear, and left its\n");
        sb.append("/// own provider named by the global for NE to release\n");
        sb.append("/// underneath the next writer.\n");
        sb.append("- (void)cn1ForgetIfCurrent:(int)generation {\n");
        sb.append("    @synchronized ([CN1VpnTunnelProvider class]) {\n");
        sb.append("        if (cn1tnProvider == self\n");
        sb.append("                && cn1tnProviderGeneration == generation) {\n");
        sb.append("            cn1tnProvider = nil;\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("/// The read loop, one batch at a time.\n");
        sb.append("///\n");
        sb.append("/// A read outstanding across a restart is NOT waited for,\n");
        sb.append("/// and that is deliberate. Raised in review as packet\n");
        sb.append("/// loss: if NE hands a restart to this same object, the\n");
        sb.append("/// old callback is still on the same flow, and the batch\n");
        sb.append("/// it eventually gets -- read from the NEW link -- is\n");
        sb.append("/// dropped at the check below instead of delivered.\n");
        sb.append("///\n");
        sb.append("/// True, and worth less than the alternative costs. The\n");
        sb.append("/// loss is one batch, once, on a tunnel that has just come\n");
        sb.append("/// back up -- a moment when everything queued on the old\n");
        sb.append("/// link is gone anyway, and what survives is what the\n");
        sb.append("/// peers retransmit. Guaranteeing a single reader means\n");
        sb.append("/// tracking outstanding reads and handing the loop from a\n");
        sb.append("/// retiring generation to the current one, which puts a\n");
        sb.append("/// state machine on the hottest path in the extension:\n");
        sb.append("/// get it wrong and the tunnel reads nothing at all, which\n");
        sb.append("/// is every packet rather than one batch. The generation\n");
        sb.append("/// checks already stop the crossing that would corrupt a\n");
        sb.append("/// session -- packets read on one link reaching another --\n");
        sb.append("/// and that is the property worth defending here.\n");
        sb.append("- (void)cn1ReadPacketsForGeneration:(int)generation {\n");
        sb.append("    [self.packetFlow readPacketsWithCompletionHandler:\n");
        sb.append("            ^(NSArray<NSData *> *packets, NSArray<NSNumber *> *protocols) {\n");
        sb.append("        if (generation != atomic_load(&cn1tnReadGeneration)) {\n");
        sb.append("            // This read belongs to a tunnel that is over.\n");
        sb.append("            // Its packets were captured on a link the app\n");
        sb.append("            // has stopped, so delivering them is wrong\n");
        sb.append("            // whether or not anything is listening now,\n");
        sb.append("            // and re-arming would leave two readers on one\n");
        sb.append("            // flow for the rest of the process.\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        CODENAME_ONE_THREAD_STATE = getThreadLocalData();\n");
        sb.append("        for (NSUInteger i = 0; i < [packets count]; i++) {\n");
        sb.append("            if (generation != atomic_load(&cn1tnReadGeneration)) {\n");
        sb.append("                // PER PACKET, not once for the batch.\n");
        sb.append("                // Delivering one packet runs the\n");
        sb.append("                // application's onPacket, and a stop and\n");
        sb.append("                // a restart can both complete while it\n");
        sb.append("                // does -- the two are not documented to\n");
        sb.append("                // be this queue. buffer() would then hand\n");
        sb.append("                // back the NEW tunnel's pooled array and\n");
        sb.append("                // the rest of a batch captured on the old\n");
        sb.append("                // link would be pumped into it. A null\n");
        sb.append("                // buffer catches a stop with nothing\n");
        sb.append("                // started after it; this catches the\n");
        sb.append("                // restart.\n");
        sb.append("                break;\n");
        sb.append("            }\n");
        sb.append("            NSData *p = [packets objectAtIndex:i];\n");
        sb.append("            // Straight into the POOLED buffer. Allocating\n");
        sb.append("            // a Java array per packet and handing it over\n");
        sb.append("            // meant an allocation and a second copy for\n");
        sb.append("            // every packet at line rate, inside a process\n");
        sb.append("            // with a hard memory cap -- in an API whose\n");
        sb.append("            // buffers are pooled to avoid exactly that.\n");
        sb.append("            // The generation goes WITH the request. The\n");
        sb.append("            // check above is check-then-act, so a stop\n");
        sb.append("            // and a restart can land between it and this\n");
        sb.append("            // line; carrying the generation means the\n");
        sb.append("            // Java side answers for the start this read\n");
        sb.append("            // belongs to or answers null, rather than\n");
        sb.append("            // handing back the new tunnel's buffer.\n");
        sb.append("            JAVA_OBJECT bytes =\n");
        sb.append("                    com_codename1_impl_vpn_ExtensionTunnelHost_buffer___int_int_R_byte_1ARRAY(\n");
        sb.append("                            threadStateData,\n");
        sb.append("                            (JAVA_INT)[p length],\n");
        sb.append("                            generation);\n");
        sb.append("            if (bytes == JAVA_NULL) {\n");
        sb.append("                // No tunnel running; the rest of this\n");
        sb.append("                // batch has nowhere to go either.\n");
        sb.append("                break;\n");
        sb.append("            }\n");
        sb.append("            memcpy(((JAVA_ARRAY)bytes)->data, [p bytes],\n");
        sb.append("                    [p length]);\n");
        sb.append("            com_codename1_impl_vpn_ExtensionTunnelHost_received___int_int(\n");
        sb.append("                    threadStateData, (JAVA_INT)[p length],\n");
        sb.append("                    generation);\n");
        sb.append("        }\n");
        sb.append("        if (generation != atomic_load(&cn1tnReadGeneration)) {\n");
        sb.append("            // Stopped WHILE this batch was being handed to\n");
        sb.append("            // Java. Re-arming would leave a read for a dead\n");
        sb.append("            // tunnel outstanding on the flow, and the next\n");
        sb.append("            // start's first batch could go to it and be\n");
        sb.append("            // dropped at its generation check -- packets\n");
        sb.append("            // lost at the moment a tunnel comes up, which\n");
        sb.append("            // is the hardest moment to explain.\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        [self cn1ReadPacketsForGeneration:generation];\n");
        sb.append("    }];\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("- (void)stopTunnelWithReason:(NEProviderStopReason)reason\n");
        sb.append("        completionHandler:(void (^)(void))completionHandler {\n");
        sb.append("    CODENAME_ONE_THREAD_STATE = getThreadLocalData();\n");
        sb.append("    // THE GENERATION THIS PROVIDER OWNS, and only that\n");
        sb.append("    // one. Reading the counter here -- however carefully\n");
        sb.append("    // -- asks what the process is doing now, not what this\n");
        sb.append("    // callback is for: a replacement that has already\n");
        sb.append("    // claimed and published leaves the counter reading its\n");
        sb.append("    // number, and a stop that took it invalidated the live\n");
        sb.append("    // tunnel's reads and told Java to tear down the host\n");
        sb.append("    // that had replaced its own.\n");
        sb.append("    int cn1tnEnding;\n");
        sb.append("    @synchronized ([CN1VpnTunnelProvider class]) {\n");
        sb.append("        cn1tnEnding = cn1tnMine;\n");
        sb.append("        // GIVEN UP as it is read. A session is stopped\n");
        sb.append("        // once, so anything arriving here afterwards --\n");
        sb.append("        // a repeated stop, or one delivered late -- has\n");
        sb.append("        // nothing of its own to end, and zero is the\n");
        sb.append("        // generation every check below refuses.\n");
        sb.append("        cn1tnMine = 0;\n");
        sb.append("    }\n");
        sb.append("    // INVALIDATED only while the counter is still this\n");
        sb.append("    // start's, so a read that completes while this is\n");
        sb.append("    // tearing down finds a generation that has moved and\n");
        sb.append("    // neither delivers nor re-arms. Bumped here as well as\n");
        sb.append("    // on start because a process can sit stopped for a\n");
        sb.append("    // long time, and an outstanding read has no business\n");
        sb.append("    // surviving into whatever comes next -- but a stop\n");
        sb.append("    // that has been overtaken invalidates nothing: the\n");
        sb.append("    // start that overtook it already did, for itself.\n");
        sb.append("    //\n");
        sb.append("    // cn1tnEnded is what Java is told. On the ordinary\n");
        sb.append("    // path it is the watermark this stop left, which\n");
        sb.append("    // rejects a start still in flight; on the overtaken\n");
        sb.append("    // path it stays this stop's own generation, which the\n");
        sb.append("    // guard there reads as older than the running tunnel\n");
        sb.append("    // and leaves alone.\n");
        sb.append("    int cn1tnExpected = cn1tnEnding;\n");
        sb.append("    int cn1tnEnded = cn1tnEnding;\n");
        sb.append("    if (cn1tnEnding > 0\n");
        sb.append("            && atomic_compare_exchange_strong(\n");
        sb.append("                    &cn1tnReadGeneration, &cn1tnExpected,\n");
        sb.append("                    cn1tnEnding + 1)) {\n");
        sb.append("        cn1tnEnded = cn1tnEnding + 1;\n");
        sb.append("    }\n");
        sb.append("    // The counter as this stop left it, which is one\n");
        sb.append("    // past every start still in flight -- so a settings\n");
        sb.append("    // completion that already passed its own check and\n");
        sb.append("    // reaches begin() after this is rejected there rather\n");
        sb.append("    // than running the application's onStart for a tunnel\n");
        sb.append("    // that is over.\n");
        sb.append("    com_codename1_impl_vpn_ExtensionTunnelHost_end___int_int(\n");
        sb.append("            threadStateData, cn1tnReason(reason),\n");
        sb.append("            cn1tnEnded);\n");
        sb.append("    // CLEARED BEFORE the handler, and only if the claim\n");
        sb.append("    // is still this stop's. NE releases the provider once\n");
        sb.append("    // this handler returns, so a writer that got here\n");
        sb.append("    // first holds a retain and one that arrives after\n");
        sb.append("    // finds nil; cleared after the handler instead, there\n");
        sb.append("    // is a window in which the global names a deallocated\n");
        sb.append("    // object. Unconditionally, a stop preempted by a\n");
        sb.append("    // restart cleared the provider the restart had just\n");
        sb.append("    // published.\n");
        sb.append("    [self cn1ForgetIfCurrent:cn1tnEnding];\n");
        sb.append("    completionHandler();\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("@end\n");
        sb.append("\n");
        sb.append(fieldSource());
        sb.append(writerSource());
        sb.append(settingsSource());
        sb.append(reasonSource());
        return sb.toString();
    }

    /**
     * The C function the Java side calls to put a packet back on the link.
     *
     * <p>Named for the translated {@code IOSExtensionTunnel.writeNative}
     * native, so the whole path is Java to C to {@code packetFlow} with no
     * copy beyond the one NSData the API requires.</p>
     */
    static String writerSource() {
        return "void com_codename1_impl_ios_IOSExtensionTunnel_writeNative"
                + "___int_byte_1ARRAY_int_int(\n"
                + "        CODENAME_ONE_THREAD_STATE, JAVA_INT generation,\n"
                + "        JAVA_OBJECT packet, JAVA_INT offset,\n"
                + "        JAVA_INT length) {\n"
                + "    // SNAPSHOT FIRST, validate after. Read in this order\n"
                + "    // the pointer can only be this generation's or an\n"
                + "    // older one: anything installed by a later start is\n"
                + "    // published after that start claimed its generation,\n"
                + "    // so the check below sees the move and refuses. Read\n"
                + "    // the other way round -- check, then dereference the\n"
                + "    // global -- a restart landing in between handed this\n"
                + "    // packet to the new provider, which is the crossing\n"
                + "    // the generation exists to prevent.\n"
                + "    //\n"
                + "    // RETAINED, not just read. Without ARC the global is\n"
                + "    // a bare pointer and the stop clears it just before\n"
                + "    // NE releases the provider, so a write that overlapped\n"
                + "    // a stop could reach packetFlow on a deallocated\n"
                + "    // object. The retain is taken under the lock the stop\n"
                + "    // clears under, which is what makes it a retain of\n"
                + "    // something still alive rather than a race of its own.\n"
                + "    CN1VpnTunnelProvider *flow;\n"
                + "    @synchronized ([CN1VpnTunnelProvider class]) {\n"
                + "        // THIS start's provider, named as such. The\n"
                + "        // counter check below says the tunnel is still\n"
                + "        // the current one; this says the provider in the\n"
                + "        // slot is the one that tunnel published, which is\n"
                + "        // a different question whenever the two steps of a\n"
                + "        // start have been pulled apart.\n"
                + "        flow = cn1tnProviderGeneration == generation\n"
                + "                ? [cn1tnProvider retain] : nil;\n"
                + "    }\n"
                + "    if (flow == nil || packet == JAVA_NULL\n"
                + "            || length <= 0) {\n"
                + "        [flow release];\n"
                + "        return;\n"
                + "    }\n"
                + "    if (generation != atomic_load(&cn1tnReadGeneration)) {\n"
                + "        // A write from a tunnel that is over. Its\n"
                + "        // onPacket can still be running -- a callback\n"
                + "        // cannot be retracted, and the inbound checks\n"
                + "        // only stop packets before they enter Java -- and\n"
                + "        // ExtensionTunnelHost.end clears the host and the\n"
                + "        // transport but not the writer. Without this the\n"
                + "        // packet went out on whatever link was current,\n"
                + "        // so one session's traffic could leave on\n"
                + "        // another's tunnel. The generation travels with\n"
                + "        // the writer, installed per start.\n"
                + "        [flow release];\n"
                + "        return;\n"
                + "    }\n"
                + "    NSData *data = [NSData dataWithBytes:\n"
                + "            ((char *)((JAVA_ARRAY)packet)->data) + offset\n"
                + "            length:(NSUInteger)length];\n"
                + "    // The family the system needs to route it. Read from\n"
                + "    // the packet's own version nibble rather than assumed:\n"
                + "    // a v6 packet written as AF_INET is dropped silently.\n"
                + "    unsigned char first =\n"
                + "            ((unsigned char *)((JAVA_ARRAY)packet)->data)[offset];\n"
                + "    NSNumber *family = [NSNumber numberWithInt:\n"
                + "            ((first >> 4) == 6) ? AF_INET6 : AF_INET];\n"
                + "    [flow.packetFlow\n"
                + "            writePackets:[NSArray arrayWithObject:data]\n"
                + "            withProtocols:[NSArray arrayWithObject:family]];\n"
                + "    [flow release];\n"
                + "}\n\n";
    }

    /**
     * The record helpers, which have to exist in this process too.
     *
     * <p>The extension shares no code with the app beyond what is translated
     * into it, and the setup record arrives as text. These are the same three
     * operations {@code TunnelWire} performs on the Java side -- unescape a
     * field, split a list, turn a prefix length into a mask -- written in C
     * because they run before any of the tunnel's Java does.</p>
     */
    static String fieldSource() {
        return "/// One tab-delimited field, unescaped. The escape is the\n"
                + "/// one VpnWire writes: backslash, then t, n, r or\n"
                + "/// backslash.\n"
                + "static NSString *cn1tnField(NSArray *fields,\n"
                + "        NSUInteger index) {\n"
                + "    if (fields == nil || index >= [fields count]) {\n"
                + "        return @\"\";\n"
                + "    }\n"
                + "    NSString *v = [fields objectAtIndex:index];\n"
                + "    NSMutableString *out =\n"
                + "            [NSMutableString stringWithCapacity:[v length]];\n"
                + "    for (NSUInteger i = 0; i < [v length]; i++) {\n"
                + "        unichar c = [v characterAtIndex:i];\n"
                + "        if (c != '\\\\' || i + 1 >= [v length]) {\n"
                + "            [out appendFormat:@\"%C\", c];\n"
                + "            continue;\n"
                + "        }\n"
                + "        unichar n = [v characterAtIndex:++i];\n"
                + "        if (n == 't') {\n"
                + "            [out appendString:@\"\\t\"];\n"
                + "        } else if (n == 'n') {\n"
                + "            [out appendString:@\"\\n\"];\n"
                + "        } else if (n == 'r') {\n"
                + "            [out appendString:@\"\\r\"];\n"
                + "        } else {\n"
                + "            [out appendFormat:@\"%C\", n];\n"
                + "        }\n"
                + "    }\n"
                + "    return out;\n"
                + "}\n\n"
                + "/// A CIDR prefix length, or -1 when the text is not one.\n"
                + "///\n"
                + "/// STRICT, because intValue reads \"foo\" as zero and\n"
                + "/// zero is meaningful here: /0 is the default route a\n"
                + "/// full-tunnel VPN asks for. So route(\"10.0.0.0/foo\")\n"
                + "/// did not fail, it installed a route over ALL traffic --\n"
                + "/// the opposite of the one subnet it named. The caller\n"
                + "/// can tell an unreadable prefix from a legitimate zero\n"
                + "/// only if this refuses to guess.\n"
                + "static int cn1tnBits(NSString *prefix, int max) {\n"
                + "    NSUInteger n = [prefix length];\n"
                + "    if (n == 0 || n > 3) {\n"
                + "        return -1;\n"
                + "    }\n"
                + "    int v = 0;\n"
                + "    for (NSUInteger i = 0; i < n; i++) {\n"
                + "        unichar c = [prefix characterAtIndex:i];\n"
                + "        if (c < '0' || c > '9') {\n"
                + "            return -1;\n"
                + "        }\n"
                + "        v = v * 10 + (int)(c - '0');\n"
                + "    }\n"
                + "    return v > max ? -1 : v;\n"
                + "}\n\n"
                + "/// A dotted subnet mask for a CIDR prefix length.\n"
                + "///\n"
                + "/// ZERO is valid and is the one that matters: /0 is the\n"
                + "/// default route a full-tunnel VPN asks for. Folding it\n"
                + "/// into 32 gave 255.255.255.255, so the extension\n"
                + "/// installed a host route, started successfully, and\n"
                + "/// carried almost nothing. Takes the PARSED length, so an\n"
                + "/// unreadable prefix is refused by cn1tnBits before it\n"
                + "/// can arrive here as a plausible number.\n"
                + "static NSString *cn1tnMask(int bits) {\n"
                + "    if (bits < 0 || bits > 32) {\n"
                + "        bits = 32;\n"
                + "    }\n"
                + "    unsigned int m = bits == 0 ? 0\n"
                + "            : (0xFFFFFFFFu << (32 - bits));\n"
                + "    return [NSString stringWithFormat:@\"%u.%u.%u.%u\",\n"
                + "            (m >> 24) & 0xFF, (m >> 16) & 0xFF,\n"
                + "            (m >> 8) & 0xFF, m & 0xFF];\n"
                + "}\n\n"
                + "/// The comma-separated route list as NEIPv6Route objects.\n"
                + "///\n"
                + "/// Separate from the v4 helper because the two route\n"
                + "/// classes share no supertype that carries a\n"
                + "/// destination, and an empty list means no routes for the\n"
                + "/// same reason it does there.\n"
                + "static NSArray *cn1tnRoutes6(NSString *list) {\n"
                + "    NSMutableArray *out = [NSMutableArray array];\n"
                + "    NSArray *items =\n"
                + "            [list componentsSeparatedByString:@\",\"];\n"
                + "    for (NSUInteger i = 0; i < [items count]; i++) {\n"
                + "        if ([[items objectAtIndex:i] length] == 0) {\n"
                + "            // An EMPTY entry, which is what an empty list\n"
                + "            // is: componentsSeparatedByString returns one\n"
                + "            // empty item for @\"\", and a setup with no\n"
                + "            // routes of this family is ordinary -- a v6\n"
                + "            // tunnel passes @\"\" here. Without this the\n"
                + "            // v4 helper built a route whose destination\n"
                + "            // was the empty string and iOS refused the\n"
                + "            // whole settings object, so a valid setup\n"
                + "            // would not start. The v6 helper never showed\n"
                + "            // it: its own family test skips an empty\n"
                + "            // entry for having no colon in it.\n"
                + "            continue;\n"
                + "        }\n"
                + "        NSArray *parts = [[items objectAtIndex:i]\n"
                + "                componentsSeparatedByString:@\"/\"];\n"
                + "        NSString *net = [parts objectAtIndex:0];\n"
                + "        if ([net rangeOfString:@\":\"].location\n"
                + "                == NSNotFound) {\n"
                + "            // A v4 route in a v6 setup is not this\n"
                + "            // link's to carry; skipped rather than\n"
                + "            // handed to NEIPv6Route, which rejects it.\n"
                + "            continue;\n"
                + "        }\n"
                + "        int bits = [parts count] > 1\n"
                + "                ? cn1tnBits([parts objectAtIndex:1], 128)\n"
                + "                : 128;\n"
                + "        if (bits < 0) {\n"
                + "            // Skipped, exactly as a route of the wrong\n"
                + "            // family is. Read as zero it became ::/0 --\n"
                + "            // one unreadable entry silently turning a\n"
                + "            // split tunnel into a full one.\n"
                + "            continue;\n"
                + "        }\n"
                + "        [out addObject:[[[NEIPv6Route alloc]\n"
                + "                initWithDestinationAddress:net\n"
                + "                networkPrefixLength:\n"
                + "                        [NSNumber numberWithInt:bits]]\n"
                + "                autorelease]];\n"
                + "    }\n"
                + "    // The filtered list, EMPTY if that is what it came\n"
                + "    // to. Falling back to the default route here meant a\n"
                + "    // setup that listed only v4 routes -- a v6 interface\n"
                + "    // carrying one v4 subnet -- captured ALL v6 traffic,\n"
                + "    // which is the opposite of what it asked for. An empty\n"
                + "    // INPUT is now the same answer for the same reason:\n"
                + "    // this helper never invents a route the setup did not\n"
                + "    // ask for, whichever way the list came to be empty.\n"
                + "    return out;\n"
                + "}\n\n"
                + "/// The comma-separated route list as NEIPv4Route objects.\n"
                + "///\n"
                + "/// An empty list means NO routes, and emphatically not the\n"
                + "/// default one. It used to mean the default, on the\n"
                + "/// reasoning that a tunnel carrying nothing is useless so an\n"
                + "/// app that named none must have meant all of it. That\n"
                + "/// guessed, and guessed in the one direction where being\n"
                + "/// wrong is unrecoverable: a setup asking for no traffic\n"
                + "/// silently captured every packet on the device.\n"
                + "///\n"
                + "/// The API says otherwise and so does the other port.\n"
                + "/// TunnelSetup.route documents the full tunnel as the\n"
                + "/// explicit 0.0.0.0/0 and ::/0, never as an absence, and\n"
                + "/// Android's CN1VpnService adds exactly the routes it was\n"
                + "/// given with no fallback anywhere -- so one setup carried\n"
                + "/// nothing there and everything here. This is the same\n"
                + "/// mistake the filtered case below already names; the only\n"
                + "/// difference was which list arrived empty.\n"
                + "static NSArray *cn1tnRoutes(NSString *list) {\n"
                + "    NSMutableArray *out = [NSMutableArray array];\n"
                + "    NSArray *items =\n"
                + "            [list componentsSeparatedByString:@\",\"];\n"
                + "    for (NSUInteger i = 0; i < [items count]; i++) {\n"
                + "        if ([[items objectAtIndex:i] length] == 0) {\n"
                + "            // An EMPTY entry, which is what an empty list\n"
                + "            // is: componentsSeparatedByString returns one\n"
                + "            // empty item for @\"\", and a setup with no\n"
                + "            // routes of this family is ordinary -- a v6\n"
                + "            // tunnel passes @\"\" here. Without this the\n"
                + "            // v4 helper built a route whose destination\n"
                + "            // was the empty string and iOS refused the\n"
                + "            // whole settings object, so a valid setup\n"
                + "            // would not start. The v6 helper never showed\n"
                + "            // it: its own family test skips an empty\n"
                + "            // entry for having no colon in it.\n"
                + "            continue;\n"
                + "        }\n"
                + "        NSArray *parts = [[items objectAtIndex:i]\n"
                + "                componentsSeparatedByString:@\"/\"];\n"
                + "        NSString *net = [parts objectAtIndex:0];\n"
                + "        if ([net rangeOfString:@\":\"].location\n"
                + "                != NSNotFound) {\n"
                + "            // A v6 route in a v4 setup is not this link's\n"
                + "            // to carry. Skipped, as the v6 helper skips a\n"
                + "            // v4 one -- without this an NEIPv4Route was\n"
                + "            // built with a v6 destination and a dotted\n"
                + "            // mask, and the whole settings object was\n"
                + "            // invalid rather than simply carrying no v4.\n"
                + "            continue;\n"
                + "        }\n"
                + "        int bits = [parts count] > 1\n"
                + "                ? cn1tnBits([parts objectAtIndex:1], 32)\n"
                + "                : 32;\n"
                + "        if (bits < 0) {\n"
                + "            // Skipped, exactly as a route of the wrong\n"
                + "            // family is. Read as zero it became a mask of\n"
                + "            // 0.0.0.0 -- one unreadable entry silently\n"
                + "            // turning a split tunnel into a full one.\n"
                + "            continue;\n"
                + "        }\n"
                + "        NSString *mask = cn1tnMask(bits);\n"
                + "        [out addObject:[[[NEIPv4Route alloc]\n"
                + "                initWithDestinationAddress:net\n"
                + "                subnetMask:mask] autorelease]];\n"
                + "    }\n"
                + "    return out;\n"
                + "}\n\n";
    }

    static String settingsSource() {
        return "/// Builds the link settings from the setup record.\n"
                + "///\n"
                + "/// The tunnel remote address is display text on iOS and\n"
                + "/// the settings object requires one, so the server field\n"
                + "/// is used and a placeholder stands in when it is empty --\n"
                + "/// an empty string here makes the whole call fail.\n"
                + "static NEPacketTunnelNetworkSettings *cn1tnSettings(\n"
                + "        NSString *wire) {\n"
                + "    NSArray *f = [wire componentsSeparatedByString:@\"\\t\"];\n"
                + "    NSString *server = cn1tnField(f, 1);\n"
                + "    NEPacketTunnelNetworkSettings *s =\n"
                + "            [[[NEPacketTunnelNetworkSettings alloc]\n"
                + "                    initWithTunnelRemoteAddress:\n"
                + "                            [server length] > 0 ? server\n"
                + "                                    : @\"127.0.0.1\"]\n"
                + "                    autorelease];\n"
                + "    NSString *address = cn1tnField(f, 0);\n"
                + "    if ([address length] == 0) {\n"
                + "        // NO ADDRESS, so there is no link to establish.\n"
                + "        // Tunnels.start refuses this setup on every\n"
                + "        // platform and never saves it, so an ordinary\n"
                + "        // start cannot arrive here -- but an on-demand\n"
                + "        // relaunch reads whatever configuration was saved,\n"
                + "        // and one written by an older version of the app,\n"
                + "        // or carrying no cn1TunnelSetup at all, reaches\n"
                + "        // this function with an empty wire.\n"
                + "        //\n"
                + "        // Returning the settings object anyway was the\n"
                + "        // quiet failure: it named a placeholder remote\n"
                + "        // address, carried no IPv4 or IPv6 settings, and\n"
                + "        // iOS could accept it -- a tunnel reported\n"
                + "        // connected, with no usable interface, and Java\n"
                + "        // told to begin on an empty setup. Failing the\n"
                + "        // start is the answer the rest of this file\n"
                + "        // already gives for a configuration it cannot\n"
                + "        // read.\n"
                + "        return nil;\n"
                + "    }\n"
                + "    if ([address length] > 0) {\n"
                + "        NSArray *parts =\n"
                + "                [address componentsSeparatedByString:@\"/\"];\n"
                + "        NSString *ip = [parts objectAtIndex:0];\n"
                + "        BOOL v6 = [ip rangeOfString:@\":\"].location != NSNotFound;\n"
                + "        // The SUPPLIED prefix, parsed once for both\n"
                + "        // families. It used to be parsed twice and thrown\n"
                + "        // away once: the v6 branch always passed 128, so an\n"
                + "        // interface asked for fd00::2/64 was installed as a\n"
                + "        // host address, directly connected peers in that\n"
                + "        // subnet were unreachable, and the configuration\n"
                + "        // onStart reported did not describe what iOS had\n"
                + "        // established.\n"
                + "        int bits = [parts count] > 1\n"
                + "                ? cn1tnBits([parts objectAtIndex:1],\n"
                + "                        v6 ? 128 : 32)\n"
                + "                : (v6 ? 128 : 32);\n"
                + "        if (bits < 0) {\n"
                + "            // NIL, and the start fails with it. A route\n"
                + "            // whose prefix is unreadable can be dropped;\n"
                + "            // the interface address cannot, and coercing\n"
                + "            // it to zero would have established the whole\n"
                + "            // link on an address the app never asked for.\n"
                + "            return nil;\n"
                + "        }\n"
                + "        NSString *mask = cn1tnMask(bits);\n"
                + "        if (v6) {\n"
                + "            NEIPv6Settings *v6s = [[[NEIPv6Settings alloc]\n"
                + "                    initWithAddresses:[NSArray arrayWithObject:ip]\n"
                + "                    networkPrefixLengths:[NSArray arrayWithObject:\n"
                + "                            [NSNumber numberWithInt:bits]]]\n"
                + "                    autorelease];\n"
                + "            // The ROUTES too. Addresses establish the\n"
                + "            // interface and route nothing, so a v6 setup\n"
                + "            // came up and carried no traffic at all --\n"
                + "            // including one that asked for the default\n"
                + "            // route, which is the ordinary case.\n"
                + "            v6s.includedRoutes = cn1tnRoutes6(cn1tnField(f, 2));\n"
                + "            s.IPv6Settings = v6s;\n"
                + "        } else {\n"
                + "            NEIPv4Settings *v4s = [[[NEIPv4Settings alloc]\n"
                + "                    initWithAddresses:[NSArray arrayWithObject:ip]\n"
                + "                    subnetMasks:[NSArray arrayWithObject:mask]]\n"
                + "                    autorelease];\n"
                + "            // includedRoutes is what actually directs\n"
                + "            // traffic in; addresses alone establish the\n"
                + "            // interface and route nothing.\n"
                + "            v4s.includedRoutes = cn1tnRoutes(cn1tnField(f, 2));\n"
                + "            s.IPv4Settings = v4s;\n"
                + "        }\n"
                + "        // The OTHER family, when the routes ask for one.\n"
                + "        // The address decides which family carries the\n"
                + "        // interface, and the route helpers drop entries of\n"
                + "        // the other -- so address(\"10.0.0.2/32\")\n"
                + "        // .route(\"0.0.0.0/0\").route(\"::/0\") built v4\n"
                + "        // settings, discarded the v6 route, and reported\n"
                + "        // the tunnel connected while v6 traffic went\n"
                + "        // around it. A full tunnel that carries half the\n"
                + "        // traffic is the failure the exclusion work above\n"
                + "        // is all about, arrived at from the other side.\n"
                + "        //\n"
                + "        // Only when routes were NAMED: an empty list means\n"
                + "        // the default route, which belongs to the family\n"
                + "        // that has the address and not to both.\n"
                + "        //\n"
                + "        // Whether iOS accepts a family carrying routes and\n"
                + "        // no address is not something this can settle from\n"
                + "        // here. If it refuses, setTunnelNetworkSettings\n"
                + "        // fails and the error reaches the application --\n"
                + "        // which is the outcome to prefer over a tunnel\n"
                + "        // that says it carries everything and does not.\n"
                + "        //\n"
                + "        // Raised in review as something to refuse up\n"
                + "        // front, on the premise that iOS cannot configure\n"
                + "        // a family without an interface address. That is\n"
                + "        // the assumption this comment declines to make,\n"
                + "        // and refusing on it would reject a setup a given\n"
                + "        // iOS may well accept. The failure it would\n"
                + "        // prevent is already a failure WITH an error: the\n"
                + "        // start does not complete and the application is\n"
                + "        // told. Trading that for a refusal this side\n"
                + "        // guessed at buys nothing and can cost a working\n"
                + "        // tunnel.\n"
                + "        if ([cn1tnField(f, 2) length] > 0) {\n"
                + "            NSArray *other = v6\n"
                + "                    ? cn1tnRoutes(cn1tnField(f, 2))\n"
                + "                    : cn1tnRoutes6(cn1tnField(f, 2));\n"
                + "            if ([other count] > 0) {\n"
                + "                if (v6) {\n"
                + "                    NEIPv4Settings *o = [[[NEIPv4Settings alloc]\n"
                + "                            initWithAddresses:[NSArray array]\n"
                + "                            subnetMasks:[NSArray array]]\n"
                + "                            autorelease];\n"
                + "                    o.includedRoutes = other;\n"
                + "                    s.IPv4Settings = o;\n"
                + "                } else {\n"
                + "                    NEIPv6Settings *o = [[[NEIPv6Settings alloc]\n"
                + "                            initWithAddresses:[NSArray array]\n"
                + "                            networkPrefixLengths:[NSArray array]]\n"
                + "                            autorelease];\n"
                + "                    o.includedRoutes = other;\n"
                + "                    s.IPv6Settings = o;\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "    NSString *dns = cn1tnField(f, 3);\n"
                + "    NSString *domains = cn1tnField(f, 4);\n"
                + "    if ([dns length] > 0) {\n"
                + "        NEDNSSettings *d = [[[NEDNSSettings alloc]\n"
                + "                initWithServers:[dns componentsSeparatedByString:@\",\"]]\n"
                + "                autorelease];\n"
                + "        // The SEARCH DOMAINS, which TunnelSetup documents\n"
                + "        // iOS applying. Field 4 was carried across the\n"
                + "        // wire and then never read, so a short hostname\n"
                + "        // that resolved on Android did not here.\n"
                + "        if ([domains length] > 0) {\n"
                + "            d.searchDomains =\n"
                + "                    [domains componentsSeparatedByString:@\",\"];\n"
                + "        }\n"
                + "        s.DNSSettings = d;\n"
                + "    }\n"
                + "    NSString *mtu = cn1tnField(f, 5);\n"
                + "    if ([mtu intValue] > 0) {\n"
                + "        s.MTU = [NSNumber numberWithInt:[mtu intValue]];\n"
                + "    }\n"
                + "    return s;\n"
                + "}\n\n";
    }

    static String reasonSource() {
        return "/// Maps NEProviderStopReason onto TunnelStopReason's\n"
                + "/// ordinals, which cross the SPI and must not be\n"
                + "/// reordered: REQUESTED, USER_DISABLED, NETWORK_LOST,\n"
                + "/// SYSTEM_RECLAIMED, UNKNOWN.\n"
                + "static JAVA_INT cn1tnReason(NEProviderStopReason reason) {\n"
                + "    switch (reason) {\n"
                + "        case NEProviderStopReasonUserInitiated:\n"
                + "            return 0;\n"
                + "        case NEProviderStopReasonUserLogout:\n"
                + "        case NEProviderStopReasonUserSwitch:\n"
                + "            return 1;\n"
                + "        case NEProviderStopReasonNoNetworkAvailable:\n"
                + "        case NEProviderStopReasonUnrecoverableNetworkChange:\n"
                + "            return 2;\n"
                + "        case NEProviderStopReasonProviderDisabled:\n"
                + "        case NEProviderStopReasonSleep:\n"
                + "            return 3;\n"
                + "        default:\n"
                + "            return 4;\n"
                + "    }\n"
                + "}\n";
    }

    /**
     * The C symbol prefix ParparVM gives a class.
     *
     * <p>Dots become underscores; nothing else changes. Getting this wrong is
     * silent -- the extern below would simply name a function that does not
     * exist, and the link failure names a symbol the developer never wrote.
     * </p>
     *
     * @param binaryName the class in {@code com.example.MyTunnel} form
     * @return the mangled prefix
     */
    static String mangle(String binaryName) {
        // '$' and '/' as well as '.', which is what ParparVM does and what
        // WatchNativeBuilder.mangle already documented. Replacing only '.'
        // was right for every name anyone had tried and wrong for a NESTED
        // tunnel -- com.example.Outer$Tunnel is a legal value for
        // ios.vpn.tunnel.class, and the provider then declared __NEW_com_
        // example_Outer$Tunnel, a symbol the translation never defines. The
        // extension failed at link, which nothing in CI compiles far enough
        // to see.
        return binaryName == null ? ""
                : binaryName.replace('.', '_').replace('/', '_')
                        .replace('$', '_');
    }

    static String infoPlist(String displayName, String shortVersion,
            String bundleVersion) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\""
                + " \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n<dict>\n");
        sb.append("    <key>CFBundleDevelopmentRegion</key>\n");
        sb.append("    <string>en</string>\n");
        sb.append("    <key>CFBundleDisplayName</key>\n    <string>")
                .append(escapeXml(displayName)).append("</string>\n");
        // $(EXECUTABLE_NAME) rather than the target name: a project that
        // overrides PRODUCT_NAME builds a differently named executable, and
        // a plist naming the target would point at a binary that is not
        // there.
        sb.append("    <key>CFBundleExecutable</key>\n");
        sb.append("    <string>$(EXECUTABLE_NAME)</string>\n");
        sb.append("    <key>CFBundleIdentifier</key>\n");
        sb.append("    <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>\n");
        sb.append("    <key>CFBundleInfoDictionaryVersion</key>\n");
        sb.append("    <string>6.0</string>\n");
        sb.append("    <key>CFBundleName</key>\n");
        sb.append("    <string>$(PRODUCT_NAME)</string>\n");
        sb.append("    <key>CFBundlePackageType</key>\n");
        sb.append("    <string>XPC!</string>\n");
        sb.append("    <key>CFBundleShortVersionString</key>\n    <string>")
                .append(escapeXml(shortVersion)).append("</string>\n");
        sb.append("    <key>CFBundleVersion</key>\n    <string>")
                .append(escapeXml(bundleVersion)).append("</string>\n");
        sb.append("    <key>NSExtension</key>\n    <dict>\n");
        sb.append("        <key>NSExtensionPointIdentifier</key>\n");
        sb.append("        <string>").append(EXTENSION_POINT)
                .append("</string>\n");
        // NSExtensionPrincipalClass, NOT NSExtensionMainStoryboard or a
        // principal VIEW controller: a packet tunnel has no UI, and iOS
        // instantiates this class directly.
        sb.append("        <key>NSExtensionPrincipalClass</key>\n");
        sb.append("        <string>CN1VpnTunnelProvider</string>\n");
        sb.append("    </dict>\n");
        sb.append("</dict>\n</plist>\n");
        return sb.toString();
    }

    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException never) {
            // The platform default is NOT the fallback: it varies by host,
            // and a generated source written in one encoding and compiled as
            // another is a build that fails on somebody else's machine. Every
            // JVM has UTF-8, so this cannot happen -- and if it did, failing
            // is the honest answer.
            throw new IllegalStateException(never);
        }
    }
}
