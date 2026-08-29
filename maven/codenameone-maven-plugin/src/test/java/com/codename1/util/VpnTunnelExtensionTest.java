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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The generated packet-tunnel extension.
///
/// This target is unlike the other generated extensions in one way that
/// matters: it HOSTS A VM and runs the application's own Java. So the checks
/// here are about the three things that make that work and fail silently when
/// they do not -- the VM being initialised, the writer being installed before
/// the tunnel can forward anything, and the read being re-armed.
class VpnTunnelExtensionTest {

    private static String provider() {
        return IOSVpnTunnelExtensionBuilder.providerSource(
                "com.example.MyTunnel");
    }

    private static String text(Map<String, byte[]> files, String name)
            throws Exception {
        return new String(files.get(name), "UTF-8");
    }

    /// The generated source with its comment lines removed.
    ///
    /// The checks below look for constructs that must not appear in the
    /// generated code, and the comments explaining why they must not appear
    /// name them -- so an assertion run over the raw text fails on the note
    /// describing the bug it is guarding against.
    private static String code(String src) {
        StringBuilder sb = new StringBuilder();
        String[] lines = src.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().startsWith("//")) {
                sb.append(lines[i]).append('\n');
            }
        }
        return sb.toString();
    }

    @Test
    void theVmIsStartedBeforeAnyJavaRuns() {
        String src = provider();
        assertTrue(src.contains("initConstantPool()"),
                "the extension has no UIApplicationMain to do this for it");
        assertTrue(src.contains("dispatch_once"),
                "start and stop repeat within one process; initialising twice"
                + " would reset every static the tunnel holds");
    }

    @Test
    void theWriterIsInstalledBeforeTheTunnelIsConstructed() {
        String src = provider();
        int install = src.indexOf("IOSExtensionTunnel_install__(");
        // The CONSTRUCTION, not the extern that declares the allocator: the
        // extern necessarily comes first, and matching it made this compare
        // the wrong two positions and pass whatever the order really was.
        int construct = src.indexOf("JAVA_OBJECT tunnel = __NEW_");
        assertTrue(install >= 0, "the writer has to be installed");
        assertTrue(construct >= 0, "the app's tunnel has to be constructed");
        assertTrue(install < construct,
                "onStart may forward a packet, and a forward before the"
                + " writer is installed is dropped with nothing to say so");
    }

    @Test
    void theReadIsReArmedFromInsideItsOwnHandler() {
        String src = provider();
        // Twice: the initial arm after the settings are applied, and again
        // inside the completion handler. readPacketsWithCompletionHandler
        // delivers ONE batch, so an extension that does not ask again stops
        // receiving traffic and looks like a tunnel that hung.
        int first = src.indexOf("cn1ReadPackets");
        assertTrue(first >= 0);
        assertTrue(src.indexOf("cn1ReadPackets", first + 1) > 0,
                "the handler must arm the next batch");
    }

    @Test
    void packetsGoIntoThePooledBuffer() {
        // An allocation and a second copy per packet, at line rate, in a
        // process with a hard memory cap -- in an API whose buffers are
        // pooled to avoid precisely that.
        String src = provider();
        assertFalse(src.contains("__NEW_ARRAY_JAVA_BYTE"),
                "no per-packet Java array");
        assertTrue(src.contains("ExtensionTunnelHost_buffer___int"),
                "the pooled buffer is asked for instead");
        assertTrue(src.contains("ExtensionTunnelHost_received___int"),
                "and told how much was written");
    }

    @Test
    void theTunnelClassIsNamedRatherThanLookedUp() {
        // Class.forName would not survive obfuscation, which is why the
        // framework bans it -- so the class is baked in as a symbol at
        // build time.
        String src = provider();
        assertTrue(src.contains("com_example_MyTunnel"),
                "the tunnel is reached as a translated symbol");
        // A CALL, not the word. The comment above the extern explains why
        // reflection is not used here and names Class.forName doing it, so a
        // substring test for the name matches the explanation and reports
        // the opposite of what it meant to check.
        assertFalse(src.contains("forName("),
                "a name looked up at run time would be gone by then");
    }

    @Test
    void settingsAreAppliedBeforePacketsAreRead() {
        String src = provider();
        int settings = src.indexOf("setTunnelNetworkSettings");
        int read = src.indexOf("cn1ReadPackets");
        assertTrue(settings >= 0 && read > settings,
                "reading before the settings land returns nothing, for ever,"
                + " with no error");
    }

    @Test
    void anIpv6SetupRoutesTraffic() {
        // Addresses establish the interface and route nothing, so a v6
        // tunnel that assigned only addresses came up carrying nothing --
        // including one that asked for the default route.
        String src = provider();
        assertTrue(src.contains("v6s.includedRoutes"),
                "the v6 branch has to install routes, like the v4 one");
        assertTrue(src.contains("NEIPv6Route"),
                "v6 routes are their own class; the v4 helper cannot make"
                + " them");
    }

    @Test
    void aFilteredIpv6ListDoesNotBecomeTheDefaultRoute() {
        // A setup listing only v4 routes on a v6 interface asked for no v6
        // traffic. Falling back to the default route after filtering them
        // all out captured every v6 packet instead -- the opposite of the
        // request. The default belongs to an EMPTY input, where the app
        // named no routes at all.
        String src = provider();
        int helper = src.indexOf("cn1tnRoutes6");
        assertTrue(helper >= 0);
        String body = src.substring(helper);
        int guard = body.indexOf("if ([list length] == 0)");
        int tail = body.indexOf("return out;");
        assertTrue(guard >= 0 && tail > guard,
                "the empty-input default stays, and the filtered list is"
                + " returned as it stands");
    }

    @Test
    void bothRouteHelpersFilterTheOtherFamily() {
        // The v6 helper skipped v4 entries and the v4 helper skipped
        // nothing, so address("10.0.0.2/32").route("::/0") built an
        // NEIPv4Route whose destination was "::" and whose mask was a dotted
        // quad. That is not a route the system can install, and it does not
        // fail alone -- the whole NEPacketTunnelNetworkSettings object is
        // rejected, so a setup naming one v6 route brought down the v4 half
        // with it.
        //
        // Asserted as a pair rather than on the v4 helper alone: the defect
        // was the ASYMMETRY, and a filter added to one side is exactly what
        // produced it.
        String src = provider();
        for (String helper : new String[] {"cn1tnRoutes(NSString",
                "cn1tnRoutes6(NSString"}) {
            int at = src.indexOf(helper);
            assertTrue(at >= 0, helper + " has to exist");
            String body = src.substring(at, src.indexOf("return out;", at));
            assertTrue(body.contains("rangeOfString:@\":\"")
                    && body.contains("continue;"),
                    helper + " has to skip entries of the other family,"
                    + " not build a route class for them");
        }
    }

    @Test
    void theHelpersAreDeclaredBeforeTheyAreCalled() {
        // The implementation calls cn1tnSettings and cn1tnReason and their
        // definitions follow @end, which reads well and does not compile:
        // C99 removed implicit declarations and current clang makes that an
        // error, so the generated target failed on its own first build.
        //
        // Nothing in this repository compiles this file -- it is written
        // here and built by Xcode on a machine none of our tests run on --
        // which is exactly why a break like this sat here unseen. Checked
        // by generating the provider and running clang against the real iOS
        // SDK; that cannot run in a unit test, so this holds the property
        // the fix established.
        String src = provider();
        int impl = src.indexOf("@implementation");
        assertTrue(impl > 0, "the provider has an implementation");
        String preamble = src.substring(0, impl);
        for (String helper : new String[] {"cn1tnSettings", "cn1tnReason"}) {
            assertTrue(preamble.contains(helper),
                    helper + " is called from the implementation, so it has"
                    + " to be declared above it: " + preamble);
        }
    }

    @Test
    void routesOfBothFamiliesReachTheLink() {
        // The address decides which family carries the interface and the
        // route helpers drop entries of the other, so
        // address("10.0.0.2/32").route("0.0.0.0/0").route("::/0") built v4
        // settings, discarded the v6 route, and reported the tunnel
        // connected while v6 traffic went around it -- a full tunnel
        // carrying half the traffic.
        String src = provider();
        int at = src.indexOf("The OTHER family");
        assertTrue(at > 0, "the other family has to be considered at all");
        String block = src.substring(at, src.indexOf("NSString *dns", at));
        assertTrue(block.contains("cn1tnRoutes6(cn1tnField(f, 2))")
                        && block.contains("cn1tnRoutes(cn1tnField(f, 2))"),
                "both helpers are consulted, whichever family the address is");
        assertTrue(block.contains("[cn1tnField(f, 2) length] > 0"),
                "and only when routes were NAMED -- an empty list means the"
                + " default route, which belongs to the family that has the"
                + " address");
    }

    @Test
    void searchDomainsReachTheLink() {
        // TunnelSetup documents iOS applying these, and field 4 was carried
        // across the wire and then never read -- so a short hostname that
        // resolved on Android did not here.
        String src = provider();
        assertTrue(src.contains("searchDomains"),
                "the documented behaviour has to be the implemented one");
        assertTrue(src.contains("cn1tnField(f, 4)"),
                "field 4 is where the wire puts them");
    }

    @Test
    void theDefaultRouteSurvivesTheMaskHelper() {
        // /0 is what a full-tunnel VPN asks for and what the documentation
        // shows. Folding zero into 32 gave 255.255.255.255, so the extension
        // installed a host route, started successfully, and carried almost
        // nothing -- the same bug the Android parser had, in the other
        // language.
        String src = provider();
        assertFalse(src.contains("if (bits <= 0 || bits > 32)"),
                "zero is a valid prefix, and the important one");
        assertTrue(src.contains("if (bits < 0 || bits > 32)"),
                "only a negative or oversized prefix is unusable");
    }

    @Test
    void theSuppliedIpv6PrefixIsUsed() {
        // fd00::2/64 was parsed and then discarded for a hardcoded 128, so
        // the interface did not match the requested subnet and what onStart
        // reported was not what iOS installed.
        //
        // Asserted on the ARGUMENT rather than on the local's name: the
        // first version of this checked for "v6bits", which broke the moment
        // the two families started sharing one parse and said nothing about
        // whether the value still reached the settings object.
        String src = provider();
        int v6 = src.indexOf("NEIPv6Settings *v6s = [[[NEIPv6Settings alloc]");
        assertTrue(v6 >= 0, "the v6 settings have to be built");
        String block = src.substring(v6, src.indexOf("autorelease];", v6));
        assertTrue(block.contains("networkPrefixLengths:"),
                "the prefix length is what this is about");
        assertFalse(block.matches("(?s).*numberWithInt:\\s*\\d.*"),
                "a literal there is the hardcoded 128 coming back;"
                + " the parsed prefix has to reach NEIPv6Settings");
    }

    @Test
    void theInfoPlistDeclaresAPacketTunnel() throws Exception {
        Map<String, byte[]> files = IOSVpnTunnelExtensionBuilder.buildFileMap(
                "com.example.app", "My VPN", "1.0", "17",
                "com.example.MyTunnel");
        String plist = text(files, "Info.plist");
        assertTrue(plist.contains(
                "com.apple.networkextension.packet-tunnel"),
                "a bundle whose extension point does not match is never"
                + " started, and nothing reports that it was not");
        assertTrue(plist.contains("CN1VpnTunnelProvider"),
                "iOS instantiates the principal class directly");
        assertTrue(plist.contains("$(EXECUTABLE_NAME)"),
                "a project may rename the product; the plist has to follow");
    }

    @Test
    void theExtensionCarriesTheEntitlementThatMakesItATunnel() throws Exception {
        Map<String, byte[]> files = IOSVpnTunnelExtensionBuilder.buildFileMap(
                "com.example.app", "My VPN", "1.0", "17",
                "com.example.MyTunnel");
        String ent = text(files, "CN1VpnTunnel.entitlements");
        assertTrue(ent.contains(
                "com.apple.developer.networking.networkextension"),
                "without it the extension is never started");
        assertTrue(ent.contains("packet-tunnel-provider"),
                "and this is the value that says which kind it is");
        // An ARRAY, not a string: this key is array-valued, and a string
        // either fails codesigning or is dropped.
        assertTrue(ent.contains("<array>"),
                "the key is array-valued");
    }

    @Test
    void anUnreadablePrefixIsRefusedRatherThanReadAsZero() {
        // NSString's intValue reads "foo" as 0, and 0 is meaningful here:
        // /0 is the default route. So route("10.0.0.0/foo") did not fail --
        // it installed a route over ALL traffic, which is the opposite of
        // the single subnet it named, and the tunnel came up reporting
        // success. The interface address had the same coercion.
        //
        // Asserted as the ABSENCE of the coercion primitive from the three
        // places that read a prefix, rather than the presence of a
        // particular guard: any parse that goes back through intValue has
        // the defect back, whatever the code around it looks like.
        //
        // Scoped to prefixes rather than to the whole file, because the MTU
        // legitimately coerces: there zero is not a meaningful value, so an
        // unreadable one falls through to the system default, which is the
        // recoverable answer TunnelWire.mtu picks on the Java side too.
        String src = provider();
        assertTrue(src.contains("static int cn1tnBits(NSString *prefix,"
                + " int max)"),
                "the strict parser is what tells -1 from a legitimate 0");

        // Every one of the three places that reads a prefix uses it, and
        // each does the only thing it can with a refusal: a route is
        // dropped, the interface address is not droppable and fails the
        // whole settings object.
        for (String helper : new String[] {"cn1tnRoutes(NSString",
                "cn1tnRoutes6(NSString"}) {
            int at = src.indexOf(helper);
            assertTrue(at >= 0, helper + " has to exist");
            String body = code(
                    src.substring(at, src.indexOf("return out;", at)));
            assertTrue(body.contains("cn1tnBits("),
                    helper + " has to parse its prefix strictly");
            assertFalse(body.contains("intValue"),
                    helper + " may not read a prefix with NSString's"
                    + " lenient coercion");
            assertTrue(body.contains("if (bits < 0) {"),
                    helper + " has to act on a refusal, not ignore it");
        }
        int settings = src.indexOf(
                "cn1tnSettings(\n        NSString *wire) {");
        assertTrue(settings >= 0, "the settings builder has to exist");
        // Ends at the DNS block: the MTU that follows it coerces on purpose.
        String body = code(src.substring(settings,
                src.indexOf("s.DNSSettings", settings)));
        assertTrue(body.contains("cn1tnBits(") && body.contains("return nil;"),
                "an interface address cannot be dropped, so an unreadable"
                + " prefix has to fail the settings object");
        assertFalse(body.contains("intValue"),
                "and its prefix may not be read with the lenient coercion"
                + " either");
    }

    @Test
    void aFailedSettingsBuildFailsTheStartRatherThanTheLink() {
        // The extension is a separate process and Tunnels.start() returned
        // long ago, so failing the start is the whole of what it can say --
        // and it is the right thing to say. Proceeding with nil settings
        // would establish a link on a configuration nobody asked for.
        String src = provider();
        int nilCheck = src.indexOf("if (settings == nil) {");
        int apply = src.indexOf("[self setTunnelNetworkSettings:settings");
        assertTrue(nilCheck >= 0, "the nil settings case has to be handled");
        assertTrue(apply > nilCheck,
                "and handled BEFORE the settings are applied");
        assertTrue(src.substring(nilCheck, apply).contains(
                "completionHandler([NSError"),
                "the start has to fail with an error, not silently return");
    }

    @Test
    void theBundleIdIsUnderTheHostApp() {
        // An extension's identifier has to be prefixed by the host's, or the
        // App ID cannot be created and codesigning fails.
        assertEquals("com.example.app.vpntunnel",
                IOSVpnTunnelExtensionBuilder.bundleId("com.example.app"));
    }

    @Test
    void theClassSymbolIsMangledTheWayParparvmDoesIt() {
        assertEquals("com_example_MyTunnel",
                IOSVpnTunnelExtensionBuilder.mangle("com.example.MyTunnel"));
    }
}
