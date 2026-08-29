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
 * NOTHING CALLS THIS, and that is the current state rather than an oversight.
 *
 * <p>IPhoneBuilder refuses {@code ios.vpn.tunnel=true} and never enables
 * {@code CN1_VPN_TUNNEL}, so {@code vpnTunnelSupported()} compiles to false
 * in every build and no target is generated. The class is kept, and kept
 * under test, because the piece that is missing is a ByteCodeTranslator
 * translation rooted at the tunnel -- not any of this -- and throwing the
 * generator away would mean writing it again from nothing when that lands.
 *
 * <p>It has been read as evidence that the iOS tunnel works, twice. It is
 * not: a generator with no caller and a {@code #if} whose macro nothing
 * defines produce no code at all. The guide says the iOS half is unbuilt
 * because the iOS half is unbuilt.
 *
 * <p>One consequence worth stating plainly: no build in this repository
 * compiles what this writes. The output was checked by generating it and
 * running clang against the real iOS SDK by hand, which is how a
 * forward-declaration break that would have failed the target's first build
 * was found sitting here. Treat a change to this file as unverified until
 * that is done again.
 *
 * <hr>
 *
 * <p>What it WOULD generate, when there is something to call it: the iOS
 * packet-tunnel app extension behind {@code com.codename1.vpn.tunnel}.</p>
 *
 * <p>This one differs from every other extension this builder generates:
 * <b>it hosts a virtual machine</b>. The others are small Objective-C
 * handlers that answer the system and exit. A packet tunnel runs the
 * application's own {@code VpnTunnel} subclass, which is Java, so the
 * extension target is translated exactly as the app target is and the
 * generated provider below boots the VM before handing packets to it.</p>
 *
 * <p>An earlier version of this framework recorded that this could not be
 * done -- that a Network Extension is "a separate process with no ParparVM
 * in it". That premise is half right, and the half that matters is the
 * reason nothing calls this yet: the extension WOULD be a target this build
 * produces, so what is in it would be this build's decision -- but the
 * translation that would put a VM in it without the application shell, whose
 * natives call UIKit an extension may not touch, has not been written. What
 * IS true either way is that it shares nothing with the app: no statics, no
 * {@code Display}, no open connections. Everything the tunnel needs travels
 * in {@code TunnelSetup.data} and arrives as the provider configuration.</p>
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
            String tunnelClass) {
        Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
        files.put("CN1VpnTunnelProvider.h", utf8(providerHeader()));
        files.put("CN1VpnTunnelProvider.m", utf8(providerSource(tunnelClass)));
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
    static String providerSource(String tunnelClass) {
        String mangled = mangle(tunnelClass);
        StringBuilder sb = new StringBuilder();
        sb.append("#import \"CN1VpnTunnelProvider.h\"\n");
        sb.append("#include \"cn1_globals.h\"\n");
        sb.append("#include \"com_codename1_impl_vpn_ExtensionTunnelHost.h\"\n");
        sb.append("#include \"com_codename1_impl_ios_IOSExtensionTunnel.h\"\n");
        sb.append("\n");
        sb.append("// The application's tunnel, named by the build. Reached\n");
        sb.append("// through the translated allocator rather than by\n");
        sb.append("// reflection: this app is obfuscated by the time it gets\n");
        sb.append("// here, so a name looked up at run time would not be\n");
        sb.append("// there -- which is the same reason Class.forName is\n");
        sb.append("// banned in the framework itself.\n");
        sb.append("extern JAVA_OBJECT __NEW_").append(mangled).append("();\n");
        sb.append("extern void ").append(mangled)
                .append("_ctor__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);\n");
        sb.append("\n");
        sb.append("static CN1VpnTunnelProvider *cn1tnProvider = nil;\n");
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
        sb.append("@implementation CN1VpnTunnelProvider\n");
        sb.append("\n");
        sb.append("- (void)startTunnelWithOptions:(NSDictionary *)options\n");
        sb.append("        completionHandler:(void (^)(NSError *))completionHandler {\n");
        sb.append("    cn1tnProvider = self;\n");
        sb.append("    // ONCE per process. The extension is started and\n");
        sb.append("    // stopped repeatedly within one process lifetime, and\n");
        sb.append("    // initialising the VM twice would reset every static\n");
        sb.append("    // the tunnel had.\n");
        sb.append("    static dispatch_once_t once;\n");
        sb.append("    dispatch_once(&once, ^{\n");
        sb.append("        initConstantPool();\n");
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
        sb.append("        completionHandler([NSError\n");
        sb.append("                errorWithDomain:@\"com.codename1.vpn\"\n");
        sb.append("                code:2\n");
        sb.append("                userInfo:[NSDictionary\n");
        sb.append("                        dictionaryWithObject:\n");
        sb.append("                                @\"The tunnel setup is not readable\"\n");
        sb.append("                        forKey:NSLocalizedDescriptionKey]]);\n");
        sb.append("        return;\n");
        sb.append("    }\n");
        sb.append("    [self setTunnelNetworkSettings:settings\n");
        sb.append("            completionHandler:^(NSError *error) {\n");
        sb.append("        if (error != nil) {\n");
        sb.append("            completionHandler(error);\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        CODENAME_ONE_THREAD_STATE = getThreadLocalData();\n");
        sb.append("        // The writer FIRST: the tunnel's onStart may\n");
        sb.append("        // forward a packet, and a tunnel that forwards\n");
        sb.append("        // before the writer is installed drops it with\n");
        sb.append("        // nothing to say so.\n");
        sb.append("        com_codename1_impl_ios_IOSExtensionTunnel_install__(\n");
        sb.append("                threadStateData);\n");
        sb.append("        JAVA_OBJECT tunnel = __NEW_").append(mangled).append("();\n");
        sb.append("        ").append(mangled)
                .append("_ctor__(threadStateData, tunnel);\n");
        sb.append("        com_codename1_impl_vpn_ExtensionTunnelHost_begin___java_lang_Object_java_lang_String(\n");
        sb.append("                threadStateData, tunnel,\n");
        sb.append("                fromNSString(threadStateData, wire));\n");
        sb.append("        [self cn1ReadPackets];\n");
        sb.append("        completionHandler(nil);\n");
        sb.append("    }];\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("/// Arms the next batch, and re-arms from inside the\n");
        sb.append("/// handler; see the note on this class.\n");
        sb.append("- (void)cn1ReadPackets {\n");
        sb.append("    [self.packetFlow readPacketsWithCompletionHandler:\n");
        sb.append("            ^(NSArray<NSData *> *packets, NSArray<NSNumber *> *protocols) {\n");
        sb.append("        CODENAME_ONE_THREAD_STATE = getThreadLocalData();\n");
        sb.append("        for (NSUInteger i = 0; i < [packets count]; i++) {\n");
        sb.append("            NSData *p = [packets objectAtIndex:i];\n");
        sb.append("            // Straight into the POOLED buffer. Allocating\n");
        sb.append("            // a Java array per packet and handing it over\n");
        sb.append("            // meant an allocation and a second copy for\n");
        sb.append("            // every packet at line rate, inside a process\n");
        sb.append("            // with a hard memory cap -- in an API whose\n");
        sb.append("            // buffers are pooled to avoid exactly that.\n");
        sb.append("            JAVA_OBJECT bytes =\n");
        sb.append("                    com_codename1_impl_vpn_ExtensionTunnelHost_buffer___int_R_byte_1ARRAY(\n");
        sb.append("                            threadStateData,\n");
        sb.append("                            (JAVA_INT)[p length]);\n");
        sb.append("            if (bytes == JAVA_NULL) {\n");
        sb.append("                // No tunnel running; the rest of this\n");
        sb.append("                // batch has nowhere to go either.\n");
        sb.append("                break;\n");
        sb.append("            }\n");
        sb.append("            memcpy(((JAVA_ARRAY)bytes)->data, [p bytes],\n");
        sb.append("                    [p length]);\n");
        sb.append("            com_codename1_impl_vpn_ExtensionTunnelHost_received___int(\n");
        sb.append("                    threadStateData, (JAVA_INT)[p length]);\n");
        sb.append("        }\n");
        sb.append("        [self cn1ReadPackets];\n");
        sb.append("    }];\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("- (void)stopTunnelWithReason:(NEProviderStopReason)reason\n");
        sb.append("        completionHandler:(void (^)(void))completionHandler {\n");
        sb.append("    CODENAME_ONE_THREAD_STATE = getThreadLocalData();\n");
        sb.append("    com_codename1_impl_vpn_ExtensionTunnelHost_end___int(\n");
        sb.append("            threadStateData, cn1tnReason(reason));\n");
        sb.append("    cn1tnProvider = nil;\n");
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
                + "___byte_1ARRAY_int_int(\n"
                + "        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT packet,\n"
                + "        JAVA_INT offset, JAVA_INT length) {\n"
                + "    if (cn1tnProvider == nil || packet == JAVA_NULL\n"
                + "            || length <= 0) {\n"
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
                + "    [cn1tnProvider.packetFlow\n"
                + "            writePackets:[NSArray arrayWithObject:data]\n"
                + "            withProtocols:[NSArray arrayWithObject:family]];\n"
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
        return binaryName == null ? "" : binaryName.replace('.', '_');
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
