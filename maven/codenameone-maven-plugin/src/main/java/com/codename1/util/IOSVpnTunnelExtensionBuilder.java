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
 * <p>This one differs from every other extension this builder generates:
 * <b>it hosts a virtual machine</b>. The others are small Objective-C
 * handlers that answer the system and exit. A packet tunnel runs the
 * application's own {@code VpnTunnel} subclass, which is Java, so the
 * extension target is translated exactly as the app target is and the
 * generated provider below boots the VM before handing packets to it.</p>
 *
 * <p>An earlier version of this framework recorded that this could not be
 * done -- that a Network Extension is "a separate process with no ParparVM
 * in it". The premise was wrong: the extension is a target this build
 * produces, so what is in it is this build's decision. What IS true is that
 * it shares nothing with the app: no statics, no {@code Display}, no open
 * connections. Everything the tunnel needs travels in
 * {@code TunnelSetup.data} and arrives as the provider configuration.</p>
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
        return files;
    }

    /** The bundle identifier the generated target signs under. */
    public static String bundleId(String packageName) {
        return packageName + ".vpntunnel";
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
        sb.append("            JAVA_OBJECT bytes = __NEW_ARRAY_JAVA_BYTE(\n");
        sb.append("                    threadStateData, (JAVA_INT)[p length]);\n");
        sb.append("            memcpy(((JAVA_ARRAY)bytes)->data, [p bytes],\n");
        sb.append("                    [p length]);\n");
        sb.append("            com_codename1_impl_vpn_ExtensionTunnelHost_deliver___byte_1ARRAY(\n");
        sb.append("                    threadStateData, bytes);\n");
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
                + "/// A dotted subnet mask for a CIDR prefix length.\n"
                + "///\n"
                + "/// ZERO is valid and is the one that matters: /0 is the\n"
                + "/// default route a full-tunnel VPN asks for. Folding it\n"
                + "/// into 32 gave 255.255.255.255, so the extension\n"
                + "/// installed a host route, started successfully, and\n"
                + "/// carried almost nothing.\n"
                + "static NSString *cn1tnMask(NSString *prefix) {\n"
                + "    int bits = [prefix intValue];\n"
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
                + "/// destination, and an empty list means the default\n"
                + "/// route for the same reason it does there.\n"
                + "static NSArray *cn1tnRoutes6(NSString *list) {\n"
                + "    if ([list length] == 0) {\n"
                + "        return [NSArray arrayWithObject:\n"
                + "                [NEIPv6Route defaultRoute]];\n"
                + "    }\n"
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
                + "        NSNumber *bits = [NSNumber numberWithInt:\n"
                + "                [parts count] > 1\n"
                + "                        ? [[parts objectAtIndex:1] intValue]\n"
                + "                        : 128];\n"
                + "        [out addObject:[[[NEIPv6Route alloc]\n"
                + "                initWithDestinationAddress:net\n"
                + "                networkPrefixLength:bits] autorelease]];\n"
                + "    }\n"
                + "    return [out count] > 0 ? out\n"
                + "            : [NSArray arrayWithObject:[NEIPv6Route defaultRoute]];\n"
                + "}\n\n"
                + "/// The comma-separated route list as NEIPv4Route objects.\n"
                + "///\n"
                + "/// An empty list means the default route, because a\n"
                + "/// tunnel with no included route carries nothing and an\n"
                + "/// app that set none plainly meant all of it.\n"
                + "static NSArray *cn1tnRoutes(NSString *list) {\n"
                + "    if ([list length] == 0) {\n"
                + "        return [NSArray arrayWithObject:\n"
                + "                [NEIPv4Route defaultRoute]];\n"
                + "    }\n"
                + "    NSMutableArray *out = [NSMutableArray array];\n"
                + "    NSArray *items =\n"
                + "            [list componentsSeparatedByString:@\",\"];\n"
                + "    for (NSUInteger i = 0; i < [items count]; i++) {\n"
                + "        NSArray *parts = [[items objectAtIndex:i]\n"
                + "                componentsSeparatedByString:@\"/\"];\n"
                + "        NSString *net = [parts objectAtIndex:0];\n"
                + "        NSString *mask = [parts count] > 1\n"
                + "                ? cn1tnMask([parts objectAtIndex:1])\n"
                + "                : @\"255.255.255.255\";\n"
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
                + "        NSString *mask = [parts count] > 1\n"
                + "                ? cn1tnMask([parts objectAtIndex:1])\n"
                + "                : @\"255.255.255.255\";\n"
                + "        // The SUPPLIED prefix, parsed just above and then\n"
                + "        // thrown away: the v6 branch always passed 128, so\n"
                + "        // an interface asked for fd00::2/64 was installed\n"
                + "        // as a host address. Directly connected peers in\n"
                + "        // that subnet were then unreachable, and the\n"
                + "        // configuration onStart reported did not describe\n"
                + "        // what iOS had actually established.\n"
                + "        int v6bits = [parts count] > 1\n"
                + "                ? [[parts objectAtIndex:1] intValue] : 128;\n"
                + "        if (v6bits < 0 || v6bits > 128) {\n"
                + "            v6bits = 128;\n"
                + "        }\n"
                + "        if (v6) {\n"
                + "            NEIPv6Settings *v6s = [[[NEIPv6Settings alloc]\n"
                + "                    initWithAddresses:[NSArray arrayWithObject:ip]\n"
                + "                    networkPrefixLengths:[NSArray arrayWithObject:\n"
                + "                            [NSNumber numberWithInt:v6bits]]]\n"
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
