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

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AppExtensionInfoPlistTest {

    /** An archive that overrides no build settings of its own. */
    private static final Map<String, String> NO_SETTINGS = new HashMap<String, String>();

    /** What an extension folder exported from a modern Xcode target actually ships. */
    private static final String NO_IDENTITY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<plist version=\"1.0\">\n"
            + "<dict>\n"
            + "\t<key>CFBundleName</key>\n"
            + "\t<string>WalletUIExtension</string>\n"
            + "\t<key>NSExtension</key>\n"
            + "\t<dict>\n"
            + "\t\t<key>NSExtensionPointIdentifier</key>\n"
            + "\t\t<string>com.apple.PassKit.issuer-provisioning.authorization</string>\n"
            + "\t</dict>\n"
            + "</dict>\n"
            + "</plist>\n";

    @Test
    public void missingIdentifierIsAddedAsABuildSettingReference() {
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(NO_IDENTITY, "5.4", "5.4", NO_SETTINGS, changes);
        // Without this the .appex is built with no identifier at all and the archive fails in the
        // app's own target: "Embedded Binary Bundle Identifier: (null)".
        assertTrue(out.contains("<key>CFBundleIdentifier</key>\n\t<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
        assertTrue(out.contains("<key>CFBundleShortVersionString</key>\n\t<string>5.4</string>"));
        assertTrue(out.contains("<key>CFBundleVersion</key>\n\t<string>5.4</string>"));
    }

    @Test
    public void theBundleIsGivenTheKeysThatMakeItABundle() {
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(NO_IDENTITY, "5.4", "5.4", NO_SETTINGS,
                changes);
        // Without CFBundleExecutable the .appex does not claim its own binary, and App Store
        // validation rejects the upload after a build that succeeded: "the ... binary file is not
        // permitted ... other than a valid CFBundleExecutable of supported bundles".
        assertTrue(out.contains("<key>CFBundleExecutable</key>\n\t<string>$(EXECUTABLE_NAME)</string>"));
        assertTrue(out.contains("<key>CFBundlePackageType</key>\n\t<string>XPC!</string>"));
        assertTrue(out.contains("<key>CFBundleName</key>"));
        assertTrue(out.contains("<key>CFBundleInfoDictionaryVersion</key>\n\t<string>6.0</string>"));
        // The reference, not a literal: an extension whose development language is not English
        // carries DEVELOPMENT_LANGUAGE in its own settings, and those reach this target.
        assertTrue(out.contains("<key>CFBundleDevelopmentRegion</key>\n\t"
                + "<string>$(DEVELOPMENT_LANGUAGE)</string>"));
    }

    @Test
    public void anExtensionsOwnBundleKeysAreKept() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleExecutable</key>\n\t<string>TheirName</string>\n"
                + "\t<key>CFBundlePackageType</key>\n\t<string>XPC!</string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<string>TheirName</string>"));
        assertFalse(changes.toString().contains("CFBundleExecutable"));
    }

    @Test
    public void addedKeysStayInsideTheTopLevelDict() {
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(NO_IDENTITY, "5.4", "5.4", NO_SETTINGS, changes);
        // The nested NSExtension dict closes first, so appending at the LAST </dict> is what keeps
        // the new keys out of it.
        assertTrue(out.indexOf("<key>CFBundleIdentifier</key>")
                > out.indexOf("NSExtensionPointIdentifier"));
        assertTrue(out.endsWith("</dict>\n</plist>\n"));
    }

    @Test
    public void aStaleVersionIsAlignedWithTheApp() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleShortVersionString</key>\n\t<string>1.0</string>\n\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        // Apple requires an embedded extension to carry the version of the app containing it.
        assertTrue(out.contains("<key>CFBundleShortVersionString</key>\n\t<string>5.4</string>"));
        assertTrue(changes.toString().contains("was 1.0"));
    }

    @Test
    public void aPlistThatIsAlreadyRightIsNotRewritten() {
        // Everything a target built by Xcode would have generated: the identity AND the keys that
        // make the directory a bundle. Nothing here is ours to change.
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string>com.example.app.Ext</string>\n"
                + "\t<key>CFBundleShortVersionString</key>\n\t<string>5.4</string>\n"
                + "\t<key>CFBundleVersion</key>\n\t<string>5.4</string>\n"
                + "\t<key>CFBundleExecutable</key>\n\t<string>$(EXECUTABLE_NAME)</string>\n"
                + "\t<key>CFBundlePackageType</key>\n\t<string>XPC!</string>\n"
                + "\t<key>CFBundleInfoDictionaryVersion</key>\n\t<string>6.0</string>\n"
                + "\t<key>CFBundleDevelopmentRegion</key>\n\t<string>$(DEVELOPMENT_LANGUAGE)</string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(changes.toString(), changes.isEmpty());
        assertEquals(plist, out);
    }

    @Test
    public void aReferenceThatAlreadyResolvesToTheAppsVersionIsLeftAlone() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleShortVersionString</key>\n\t<string>$(MARKETING_VERSION)</string>\n"
                + "\t<key>CFBundleName</key>");
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("MARKETING_VERSION", "5.4");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", settings, changes);
        assertTrue(out.contains("<string>$(MARKETING_VERSION)</string>"));
        assertFalse(changes.toString().contains("CFBundleShortVersionString"));
    }

    @Test
    public void aReferenceToAStaleSettingIsReplaced() {
        // The archive's buildSettings.properties are copied into this target's build
        // configurations, so the reference lands on 1.0 and the extension ships a version the
        // containing app does not have.
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleShortVersionString</key>\n\t<string>$(MARKETING_VERSION)</string>\n"
                + "\t<key>CFBundleName</key>");
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("MARKETING_VERSION", "1.0");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", settings, changes);
        assertTrue(out.contains("<key>CFBundleShortVersionString</key>\n\t<string>5.4</string>"));
        assertTrue(changes.toString(), changes.toString().contains("resolves to '1.0'"));
    }

    @Test
    public void aNestedReferenceIsExpandedBeforeItIsJudged() {
        // MARKETING_VERSION names another setting. Expanding the map once, in whatever order
        // Properties hands it over, can leave $(VERSION_SUFFIX) behind and read the version as the
        // app's own 5.4 -- while the device resolves it to 5.41 and validation rejects the pair.
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleShortVersionString</key>\n\t<string>$(MARKETING_VERSION)</string>\n"
                + "\t<key>CFBundleName</key>");
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("VERSION_SUFFIX", "1");
        settings.put("MARKETING_VERSION", "5.4$(VERSION_SUFFIX)");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", settings, changes);
        assertTrue(out.contains("<key>CFBundleShortVersionString</key>\n\t<string>5.4</string>"));
        assertTrue(changes.toString(), changes.toString().contains("resolves to '5.41'"));
    }

    @Test
    public void aNestedReferenceThatLandsOnTheAppsVersionStillStands() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleShortVersionString</key>\n\t<string>$(MARKETING_VERSION)</string>\n"
                + "\t<key>CFBundleName</key>");
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("VERSION_MAJOR", "5");
        settings.put("MARKETING_VERSION", "$(VERSION_MAJOR).4");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", settings, changes);
        assertTrue(out.contains("<string>$(MARKETING_VERSION)</string>"));
        assertFalse(changes.toString().contains("CFBundleShortVersionString"));
    }

    @Test
    public void aCycleSettlesAsUnresolvableRatherThanSpinning() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleVersion</key>\n\t<string>$(A)</string>\n\t<key>CFBundleName</key>");
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("A", "$(B)");
        settings.put("B", "$(A)");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", settings, changes);
        assertTrue(out.contains("<key>CFBundleVersion</key>\n\t<string>5.4</string>"));
    }

    @Test
    public void aReferenceToNothingIsReplacedToo() {
        // Nothing defines CURRENT_PROJECT_VERSION here: the target this build generates carries no
        // version settings, so Xcode resolves the reference to the empty string.
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleVersion</key>\n\t<string>$(CURRENT_PROJECT_VERSION)</string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<key>CFBundleVersion</key>\n\t<string>5.4</string>"));
    }

    @Test
    public void anExplicitIdentifierReferenceIsStillNeverTouched() {
        // The identifier is never overwritten when it is there and not empty, reference or not:
        // $(PRODUCT_BUNDLE_IDENTIFIER) is what this build sets on the target anyway.
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
        assertFalse(changes.toString().contains("CFBundleIdentifier"));
    }

    @Test
    public void aNonStringValueOfTheKeyIsReplacedAndOthersLeftAlone() {
        // Apple requires these keys to be strings, so <integer>7</integer> is not a version to
        // preserve -- it is an invalid bundle. What must NOT happen is the rewrite wandering off
        // to CFBundleName's <string>, which is the different bug the anchored lookup prevents.
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleVersion</key>\n\t<integer>7</integer>\n\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out, out.contains("<key>CFBundleVersion</key>\n\t<string>5.4</string>"));
        assertTrue(out.contains("<key>CFBundleName</key>\n\t<string>WalletUIExtension</string>"));
        assertTrue(changes.toString(), changes.toString().contains("not a string"));
    }

    @Test
    public void aValueInsideANestedDictIsNotMistakenForTheKeys() {
        // The NSExtension dict holds a <string> of its own further down the file.
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(NO_IDENTITY, "5.4", "5.4", NO_SETTINGS,
                changes);
        assertTrue(out.contains("<string>com.apple.PassKit.issuer-provisioning.authorization</string>"));
    }

    @Test
    public void anEmptyIdentifierIsFilledEvenThoughAnExplicitOneIsKept() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string/>\n\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        // An empty identifier is no identifier: it fails the embedded-binary check exactly like a
        // missing one, so "do not overwrite an explicit value" must not cover it.
        assertTrue(out.contains("<key>CFBundleIdentifier</key>\n\t"
                + "<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
    }

    @Test
    public void theOpenAndCloseEmptyFormIsFilledToo() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string></string>\n\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
    }

    @Test
    public void aMarkupOnlyIdentifierIsEmptyAndGetsFilled() {
        // <string><!-- ... --></string> is a nonzero run of text and an empty value. Reading it as
        // an identifier that is already there leaves the extension with none.
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string><!-- filled in by CI --></string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
        assertTrue(changes.toString(), changes.toString().contains("was empty"));
    }

    @Test
    public void aWhitespaceOnlyIdentifierIsEmptyAndGetsFilled() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string>   </string>\n\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
        assertTrue(changes.toString(), changes.toString().contains("was empty"));
    }

    @Test
    public void whitespaceInsideCdataIsEmptyToo() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string><![CDATA[   ]]></string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
    }

    @Test
    public void anEmptyCdataSectionIsEmptyToo() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleVersion</key>\n\t<string><![CDATA[]]></string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<key>CFBundleVersion</key>\n\t<string>5.4</string>"));
    }

    @Test
    public void aCdataSpellingOfTheRightVersionIsLeftAsWritten() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleVersion</key>\n\t<string><![CDATA[5.4]]></string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<string><![CDATA[5.4]]></string>"));
        assertFalse(changes.toString().contains("CFBundleVersion"));
    }

    @Test
    public void paddingRoundTheRightVersionIsNormalised() {
        // A plist parser keeps those spaces, so Apple compares " 5.4 " with the app's "5.4".
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleVersion</key>\n\t<string> 5.4 </string>\n\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<key>CFBundleVersion</key>\n\t<string>5.4</string>"));
    }

    @Test
    public void whitespaceBeforeTheSlashIsStillTheEmptyForm() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleVersion</key>\n\t<string />\n\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<key>CFBundleVersion</key>\n\t<string>5.4</string>"));
    }

    @Test
    public void anExplicitIdentifierIsNeverOverwritten() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string>com.example.Own</string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<string>com.example.Own</string>"));
        assertFalse(changes.toString().contains("CFBundleIdentifier"));
    }

    @Test
    public void aNestedKeyOfTheSameNameIsNotTheBundlesIdentity() {
        // NSExtensionAttributes comes before the top-level keys and carries a key of the same
        // name. A whole-file text search finds that one first: the stamper would then read the
        // bundle as already identified, or write the app's version into an extension attribute.
        String plist = NO_IDENTITY.replace("\t\t<key>NSExtensionPointIdentifier</key>\n",
                "\t\t<key>NSExtensionAttributes</key>\n\t\t<dict>\n"
                + "\t\t\t<key>CFBundleIdentifier</key>\n\t\t\t<string>com.nested.value</string>\n"
                + "\t\t\t<key>CFBundleVersion</key>\n\t\t\t<string>0.1</string>\n\t\t</dict>\n"
                + "\t\t<key>NSExtensionPointIdentifier</key>\n");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<string>com.nested.value</string>"));
        assertTrue(out.contains("<key>CFBundleVersion</key>\n\t\t\t<string>0.1</string>"));
        // and the bundle's own identity was added at the top level, after the NSExtension dict
        assertTrue(out.contains("<key>CFBundleIdentifier</key>\n\t"
                + "<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
        assertTrue(out.contains("<key>CFBundleVersion</key>\n\t<string>5.4</string>"));
    }

    @Test
    public void aCommentedOutKeyIsNotTheKey() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<!-- <key>CFBundleVersion</key><string>0.1</string> -->\n\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out.contains("<!-- <key>CFBundleVersion</key><string>0.1</string> -->"));
        assertTrue(out.contains("<key>CFBundleVersion</key>\n\t<string>5.4</string>"));
    }

    @Test
    public void aCompactPlistGetsItsKeysInsideTheDict() {
        // No newline between the closing tags, which is legal and which a generator may well emit.
        String plist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<plist version=\"1.0\"><dict><key>CFBundleName</key><string>Ext</string></dict></plist>";
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        // Inserting at the wrong closing tag puts the keys between </dict> and </plist>, which is
        // not a property list at all.
        assertTrue(out, out.indexOf("<key>CFBundleIdentifier</key>") < out.indexOf("</dict>"));
        assertTrue(out.endsWith("</dict></plist>"));
    }

    @Test
    public void paddingInsideCdataCountsAsPaddingToo() {
        // plutil parses <string><![CDATA[ 5.4 ]]></string> as " 5.4 ", which Apple compares with
        // the app's "5.4" and rejects. Judging it on trimmed text called it a match.
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleVersion</key>\n\t<string><![CDATA[ 5.4 ]]></string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", NO_SETTINGS, changes);
        assertTrue(out, out.contains("<key>CFBundleVersion</key>\n\t<string>5.4</string>"));
    }

    @Test
    public void aSettingsOwnTrailingSpaceIsNotNormalisedAway() {
        // The properties file's value is written into the Xcode setting verbatim, so this really
        // does expand to "5.4 " on the device.
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleShortVersionString</key>\n\t<string>$(MARKETING_VERSION)</string>\n"
                + "\t<key>CFBundleName</key>");
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("MARKETING_VERSION", "5.4 ");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", settings, changes);
        assertTrue(out, out.contains("<key>CFBundleShortVersionString</key>\n\t<string>5.4</string>"));
    }

    @Test
    public void anIdentifierReferenceIsJudgedByWhatItResolvesTo() {
        // The archive overrides PRODUCT_BUNDLE_IDENTIFIER with the identifier from the project it
        // was exported from, so the usual reference lands outside this app and the embedded bundle
        // is refused for not being prefixed by its container.
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>\n"
                + "\t<key>CFBundleName</key>");
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("PRODUCT_BUNDLE_IDENTIFIER", "com.old.project.WalletUIExtension");
        List<String> changes = new ArrayList<String>();
        assertFalse(IPhoneBuilder.identifierBelongsToApp(plist, "com.new.app", settings));
        assertTrue(IPhoneBuilder.identifierBelongsToApp(plist, "com.old.project", settings));
        // and a reference nothing defines resolves to the empty string, which is not an
        // identifier either -- Xcode ships the .appex with none.
        assertFalse(IPhoneBuilder.identifierBelongsToApp(plist, "com.new.app", NO_SETTINGS));
    }

    @Test
    public void aLiteralIdentifierFromAnotherProjectIsReplaced() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string>com.old.project.Ext</string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", "com.new.app",
                NO_SETTINGS, changes);
        assertTrue(out, out.contains("<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
    }

    @Test
    public void aLiteralIdentifierUnderTheAppIsKept() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string>com.new.app.Ext</string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", "com.new.app",
                NO_SETTINGS, changes);
        assertTrue(out.contains("<string>com.new.app.Ext</string>"));
        assertFalse(changes.toString().contains("CFBundleIdentifier"));
    }

    @Test
    public void aBinaryPlistIsReportedRatherThanMangled() {
        List<String> changes = new ArrayList<String>();
        assertNull(IPhoneBuilder.stampInfoPlistIdentity("bplist00 ", "5.4", "5.4", NO_SETTINGS, changes));
        assertEquals(1, changes.size());
    }

    @Test
    public void aVersionReferenceFollowsTheConditionalTheArchiveGets() {
        // The base matches the app, the device-qualified value does not -- and the qualified one
        // is what Xcode uses for this archive, so the extension shipped a version its container
        // does not have.
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleShortVersionString</key>\n\t<string>$(MARKETING_VERSION)</string>\n"
                + "\t<key>CFBundleName</key>");
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("MARKETING_VERSION", "5.4");
        settings.put("MARKETING_VERSION[sdk=iphoneos*]", "5.3");

        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", null,
                IPhoneBuilder.flattenForContext(settings,
                        IPhoneBuilder.ArchiveContext.of("iphoneos14.4", "Release", "arm64", settings)),
                changes);

        assertTrue(out, out.contains("<key>CFBundleShortVersionString</key>\n\t<string>5.4</string>"));
        assertTrue(changes.toString(), changes.toString().contains("resolves to '5.3'"));
    }

    @Test
    public void aConditionalThatMatchesTheAppIsLeftAlone() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleShortVersionString</key>\n\t<string>$(MARKETING_VERSION)</string>\n"
                + "\t<key>CFBundleName</key>");
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("MARKETING_VERSION", "1.0");
        settings.put("MARKETING_VERSION[sdk=iphoneos*]", "5.4");

        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", null,
                IPhoneBuilder.flattenForContext(settings,
                        IPhoneBuilder.ArchiveContext.of("iphoneos14.4", "Release", "arm64", settings)),
                changes);

        // The qualified value is the app's version, so the reference is right as written.
        assertTrue(out.contains("<string>$(MARKETING_VERSION)</string>"));
        assertFalse(changes.toString().contains("CFBundleShortVersionString"));
    }

    @Test
    public void aPaddedIdentifierIsNotTheIdentifierItReadsAs() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string> com.new.app.Ext </string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", "com.new.app",
                NO_SETTINGS, changes);
        // A plist parser keeps the padding, so this ships as " com.new.app.Ext " -- an identifier
        // Apple refuses, however well it trims.
        assertTrue(out, out.contains("<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
    }

    @Test
    public void paddingInsideCdataIsNoDifferent() {
        String plist = NO_IDENTITY.replace("<key>CFBundleName</key>",
                "<key>CFBundleIdentifier</key>\n\t<string><![CDATA[ com.new.app.Ext ]]></string>\n"
                + "\t<key>CFBundleName</key>");
        List<String> changes = new ArrayList<String>();
        String out = IPhoneBuilder.stampInfoPlistIdentity(plist, "5.4", "5.4", "com.new.app",
                NO_SETTINGS, changes);
        assertTrue(out, out.contains("<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
    }
}
