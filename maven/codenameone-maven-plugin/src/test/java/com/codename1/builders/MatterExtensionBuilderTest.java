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

import com.codename1.util.MatterExtensionBuilder;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated MatterAddDeviceExtension.
 *
 * <p>Every assertion here is about a value that fails at <b>runtime</b> if it
 * is wrong -- the sheet opens and cannot find the handler, or Apple refuses to
 * launch the extension -- with nothing at build time to say so. That is what
 * makes them worth pinning: a cloud build would report success either way.</p>
 */
public class MatterExtensionBuilderTest {

    /** The containing app's versions, which the extension has to echo. */
    private static final String SHORT_VERSION = "2.4";
    private static final String BUNDLE_VERSION = "17";

    private static final String PACKAGE = "com.example.lights";
    private static final String GROUP = "group.com.example.lights";

    private static String text(Map<String, byte[]> files, String name)
            throws UnsupportedEncodingException {
        byte[] raw = files.get(name);
        assertNotNull(raw, "the extension must contain " + name + ", got "
                + files.keySet());
        return new String(raw, "UTF-8");
    }

    /**
     * The commissioning callback is generated either way, and inert by
     * default.
     *
     * <p>commissionDevice is where an app that runs its own Matter fabric
     * joins the accessory to it. Codename One asks for nothing of the sort by
     * default -- the accessory joins the user's ecosystem, which is what the
     * sheet itself does -- so the implementation ships commented out, with
     * the controller it would need beside it. A developer who turns it on
     * gets exactly the code they read.</p>
     */
    @Test
    public void theCommissioningCallbackIsCommentedOutByDefault()
            throws Exception {
        String swift = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR),
                "RequestHandler.swift");
        assertTrue(swift.contains("//    override func commissionDevice("),
                "the override must be present and commented out");
        assertFalse(swift.contains("\n    override func commissionDevice("),
                "and must not be live: " + swift);
        assertTrue(swift.contains("//final class CN1MatterSession"),
                "the controller it needs travels with it, commented out too");
        // The imports stay live: uncommenting the implementation must be the
        // only step, and an import of a framework nothing calls costs the
        // extension nothing.
        assertTrue(swift.contains("\nimport Matter\n"), swift);
    }

    /**
     * Asking for a fabric of the app's own emits the same code, live.
     *
     * <p>Same text, so what a default build shows a reader is what an
     * enabled build compiles -- and the enabled one carries Apple's Matter
     * stack, the storage the fabric persists through, and the keypair that
     * signs for it.</p>
     */
    @Test
    public void askingForItsOwnFabricEmitsTheImplementationLive()
            throws Exception {
        String swift = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Lights", SHORT_VERSION, BUNDLE_VERSION, true, VENDOR),
                "RequestHandler.swift");
        assertTrue(swift.contains("    override func commissionDevice("),
                "the override must be live: " + swift);
        assertFalse(swift.contains("//    override func commissionDevice("),
                "and not also commented out");
        for (String needed : new String[] {"MTRDeviceControllerFactory",
                "MTRSetupPayload", "MTRKeypair", "MTRStorage",
                "commissionNode(", "setupCommissioningSession("}) {
            assertTrue(swift.contains(needed),
                    "expected " + needed + " in the live implementation");
        }
        // Its own explanatory comments are still comments; what must not
        // survive is a commented-out declaration.
        for (String dead : new String[] {"//enum CN1MatterFabric",
                "//final class CN1MatterSession", "//final class CN1MatterKeypair",
                "//    override func commissionDevice("}) {
            assertFalse(swift.contains(dead),
                    dead + " must be live in an enabled build");
        }
        assertTrue(swift.contains("static let vendorID: UInt16 = 65521"),
                "the vendor id is the build's, as a Swift literal: " + swift);
    }

    /**
     * An own-fabric build's extension needs the release its controller
     * exists in.
     *
     * <p>MTRDeviceControllerFactory arrived in 16.4, three point releases
     * after MatterSupport. At 16.1 the extension still builds -- the
     * controller sits behind an availability check -- and then does nothing
     * on 16.1 through 16.3 while the flow reports success, which is the one
     * outcome the caller cannot detect. The extension's own floor moves; the
     * app's does not, because an extension above its host is ordinary and
     * raising the app would cost every user below 16.4 the whole
     * application.</p>
     */
    @Test
    public void anOwnFabricExtensionRaisesItsOwnFloorAndThrowsBelowIt()
            throws Exception {
        assertEquals("16.1", MatterExtensionBuilder.deploymentTarget(false));
        assertEquals("16.4", MatterExtensionBuilder.deploymentTarget(true));
        assertEquals(MatterExtensionBuilder.DEPLOYMENT_TARGET,
                MatterExtensionBuilder.deploymentTarget(false));
        String swift = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Lights", SHORT_VERSION, BUNDLE_VERSION, true, VENDOR),
                "RequestHandler.swift");
        assertFalse(swift.contains("guard #available(iOS 16.4, *) else { return }"),
                "returning from the guard reports success for a fabric that"
                        + " never gained the accessory");
        assertTrue(swift.contains("throw NSError("), swift);
    }

    /**
     * The vendor id is a number or the build stops.
     *
     * <p>It is compiled into the generated extension, so anything the check
     * does not constrain is Swift -- and in a build that never asked for a
     * fabric, a newline in it walks straight out of the {@code //} that
     * comments the implementation out, because a comment ends at its
     * line.</p>
     */
    @Test
    public void theVendorIdIsAcceptedOnlyAsANumber() {
        // The number, not the spelling: 0XFFF1 parses here and is not a
        // Swift integer literal, so echoing it back produced an extension
        // that did not compile.
        assertEquals("65521", MatterExtensionBuilder.vendorLiteral("0xFFF1"));
        assertEquals("65521", MatterExtensionBuilder.vendorLiteral("0XFFF1"));
        assertEquals("4660", MatterExtensionBuilder.vendorLiteral(" 4660 "));
        assertEquals("0", MatterExtensionBuilder.vendorLiteral("0"));
        for (String bad : new String[] {"1, x: run()", "0xFFF1\n    exit(0)",
                "", null, "65536", "-1", "0x10000", "FFF1"}) {
            try {
                MatterExtensionBuilder.vendorLiteral(bad);
                fail("expected a refusal for '" + bad + "'");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("vendorId"),
                        expected.getMessage());
            }
        }
    }

    /**
     * The app group is an identifier or the build stops.
     *
     * <p>It lands in a Swift string literal, where the XML escaper the rest
     * of this generator uses means nothing: a quote closes the literal, and a
     * newline walks out of the {@code //} that comments the implementation
     * out in a build with no fabric of its own.</p>
     */
    @Test
    public void theAppGroupIsAcceptedOnlyAsAnIdentifier() {
        assertEquals(GROUP, MatterExtensionBuilder.groupLiteral(GROUP));
        assertEquals("group.a-b.c",
                MatterExtensionBuilder.groupLiteral(" group.a-b.c "));
        for (String bad : new String[] {"group.x\" + run() + \"",
                "group.x\n    exit(0)", "com.example.app", "group.", "",
                null, "group.x y"}) {
            try {
                MatterExtensionBuilder.groupLiteral(bad);
                fail("expected a refusal for '" + bad + "'");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("appGroup"),
                        expected.getMessage());
            }
        }
    }

    /** The Matter test vendor, which is what a build defaults to. */
    private static final String VENDOR = "0xFFF1";

    @Test
    public void theExtensionCarriesItsHandlerPlistAndEntitlements()
            throws Exception {
        Map<String, byte[]> files =
                MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP, "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR);
        assertEquals(3, files.size(), files.keySet().toString());
        assertNotNull(files.get("RequestHandler.swift"));
        assertNotNull(files.get("Info.plist"));
        assertNotNull(files.get(
                MatterExtensionBuilder.EXTENSION_NAME + ".entitlements"));
    }

    /**
     * The extension point is how the operating system finds this handler at
     * all. A wrong value produces a sheet that opens and immediately fails,
     * with an error about the app rather than about the identifier.
     */
    @Test
    public void thePlistNamesApplesAddDeviceExtensionPoint() throws Exception {
        String plist = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR), "Info.plist");
        assertTrue(plist.contains(
                "com.apple.matter.support.extension.device-setup"), plist);
        assertTrue(plist.contains("NSExtensionPointIdentifier"), plist);
    }

    /**
     * An extension's versions must equal its containing app's, or archive
     * validation rejects the bundle -- for every release that is not
     * literally 1.0, which is every release after the first.
     */
    @Test
    public void theExtensionEchoesTheHostVersions() throws Exception {
        String plist = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR), "Info.plist");
        assertTrue(plist.contains("<string>" + SHORT_VERSION + "</string>"),
                plist);
        assertTrue(plist.contains("<string>" + BUNDLE_VERSION + "</string>"),
                plist);
        assertFalse(plist.contains("<string>1.0</string>"),
                "the hard-coded marketing version must be gone: " + plist);
    }

    /**
     * The overrides have to match MatterSupport's own signatures exactly, and
     * nothing in a Codename One build compiles this file until a customer
     * runs a commissioning build -- so a wrong type here is a defect that
     * only ever surfaces on somebody else's machine.
     */
    @Test
    public void theHandlerUsesMatterSupportsAssociationTypes()
            throws Exception {
        String swift = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR),
                "RequestHandler.swift");
        assertTrue(swift.contains("WiFiNetworkAssociation"), swift);
        assertTrue(swift.contains("ThreadNetworkAssociation"), swift);
        // The names that do not exist, spelled out so this fails loudly if
        // somebody "simplifies" them back.
        assertFalse(swift.contains(".WiFiAssociation"), swift);
        assertFalse(swift.contains(".ThreadAssociation"), swift);
    }

    /**
     * An explicit Info.plist is not merged with anything.
     *
     * <p>PRODUCT_BUNDLE_IDENTIFIER on the target does not reach a plist that
     * does not ask for it, and a bundle with no identifier and no executable
     * key compiles and then fails validation on the way to the device -- which
     * blocks the whole build, not just commissioning.</p>
     */
    @Test
    public void thePlistCarriesTheBundleKeysXcodeDoesNotMergeIn()
            throws Exception {
        String plist = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR), "Info.plist");
        assertTrue(plist.contains("<key>CFBundleExecutable</key>"), plist);
        assertTrue(plist.contains("$(EXECUTABLE_NAME)"), plist);
        assertTrue(plist.contains("<key>CFBundleIdentifier</key>"), plist);
        assertTrue(plist.contains("$(PRODUCT_BUNDLE_IDENTIFIER)"), plist);
        assertTrue(plist.contains("<key>CFBundleVersion</key>"), plist);
        assertTrue(plist.contains("<key>CFBundleShortVersionString</key>"),
                plist);
    }

    /**
     * Swift classes are namespaced by their module, so the unqualified name
     * does not resolve at runtime. {@code $(PRODUCT_MODULE_NAME)} is what
     * makes it work without the builder having to know what the developer
     * called their project.
     */
    @Test
    public void thePrincipalClassIsModuleQualified() throws Exception {
        String plist = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR), "Info.plist");
        assertTrue(plist.contains(
                "$(PRODUCT_MODULE_NAME).RequestHandler"), plist);
    }

    @Test
    public void thePlistIsAnExtensionBundle() throws Exception {
        String plist = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR), "Info.plist");
        assertTrue(plist.contains("<string>XPC!</string>"), plist);
    }

    /**
     * The app group is the only channel between the extension and its host,
     * and Apple refuses to launch an extension whose group does not match.
     */
    @Test
    public void theEntitlementsCarryTheSharedAppGroup() throws Exception {
        String entitlements = text(
                MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP, "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR),
                MatterExtensionBuilder.EXTENSION_NAME + ".entitlements");
        assertTrue(entitlements.contains(
                "com.apple.security.application-groups"), entitlements);
        assertTrue(entitlements.contains(GROUP), entitlements);
    }

    @Test
    public void theDefaultAppGroupIsDerivedFromTheBundleId() {
        assertEquals("group." + PACKAGE,
                MatterExtensionBuilder.defaultAppGroup(PACKAGE));
    }

    /**
     * MatterSupport arrived in iOS 16.1, and linking it below that fails at
     * launch rather than at build time.
     */
    @Test
    public void theDeploymentTargetIsWhereMatterSupportBegins() {
        assertEquals("16.1", MatterExtensionBuilder.DEPLOYMENT_TARGET);
    }

    /**
     * The three overrides Apple's handler protocol expects. Absent ones do
     * not fail the Swift compile -- they have defaults -- but the network
     * selection ones deferring to the system is what lets the OS ask the user
     * which Wi-Fi the accessory should join.
     */
    @Test
    public void theHandlerImplementsApplesProtocol() throws Exception {
        String swift = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR), "RequestHandler.swift");
        assertTrue(swift.contains("import MatterSupport"), swift);
        assertTrue(swift.contains(
                "MatterAddDeviceExtensionRequestHandler"), swift);
        assertTrue(swift.contains("validateDeviceCredential"), swift);
        assertTrue(swift.contains("selectWiFiNetwork"), swift);
        assertTrue(swift.contains("selectThreadNetwork"), swift);
        assertTrue(swift.contains("@available(iOS 16.1, *)"), swift);
    }

    /**
     * A display name is the user's own text and reaches an XML document, so
     * an ampersand in an app name has to survive as data rather than opening
     * an entity.
     */
    @Test
    public void aDisplayNameWithMarkupInItIsEscaped() throws Exception {
        String plist = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Tom & Jerry <Home>", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR), "Info.plist");
        assertTrue(plist.contains("Tom &amp; Jerry &lt;Home&gt;"), plist);
    }

    /**
     * The whole feature exists to be generated rather than hand-written, so
     * regenerating has to be deterministic -- a build that produced a
     * different extension each time would churn the signed bundle for nothing.
     */
    @Test
    public void generationIsDeterministic() throws Exception {
        String first = text(MatterExtensionBuilder.buildFileMap(PACKAGE, GROUP,
                "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR), "Info.plist");
        String second = text(MatterExtensionBuilder.buildFileMap(PACKAGE,
                GROUP, "Lights", SHORT_VERSION, BUNDLE_VERSION, false, VENDOR), "Info.plist");
        assertEquals(first, second);
    }
}
