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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Android half of app intents is launcher shortcuts plus a headless service. These pin the
 * manifest and resource generation, including the two things that fail silently on a device: a
 * shortcut meta-data placed outside the launcher activity, and an exported service that would let
 * any installed app invoke a capability.
 */
class AndroidIntentShortcutsTest {

    private static String entriesFor(Path dir, String manifestJson) throws Exception {
        File assets = new File(dir.toFile(), "assets");
        File res = new File(dir.toFile(), "res");
        assets.mkdirs();
        res.mkdirs();
        if (manifestJson != null) {
            // The generated manifest is namespaced under META-INF so it cannot collide with an
            // application's own intents.json; the builder reads only that path.
            File manifestDir = new File(assets, "META-INF/codenameone");
            manifestDir.mkdirs();
            FileWriter w = new FileWriter(new File(manifestDir, "intents.json"));
            try {
                w.write(manifestJson);
            } finally {
                w.close();
            }
        }
        AndroidGradleBuilder b = new AndroidGradleBuilder();
        Method m = AndroidGradleBuilder.class.getDeclaredMethod("buildIntentsManifestEntries",
                File.class, File.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(b, assets, res, "com.example.app");
    }

    private static String shortcutsXml(Path dir) throws IOException {
        File f = new File(new File(dir.toFile(), "res"), "xml/cn1_shortcuts.xml");
        assertTrue(f.exists(), "cn1_shortcuts.xml was not written");
        return new String(Files.readAllBytes(f.toPath()), Charset.forName("UTF-8"));
    }

    private static String metaDataAfter(Path dir, String manifestJson) throws Exception {
        entriesFor(dir, manifestJson);
        AndroidGradleBuilder b = new AndroidGradleBuilder();
        Method m = AndroidGradleBuilder.class.getDeclaredMethod("buildIntentsManifestEntries",
                File.class, File.class, String.class);
        m.setAccessible(true);
        m.invoke(b, new File(dir.toFile(), "assets"), new File(dir.toFile(), "res"),
                "com.example.app");
        Field f = AndroidGradleBuilder.class.getDeclaredField("intentsShortcutsMetaData");
        f.setAccessible(true);
        return (String) f.get(b);
    }

    /// A shortcut label is application text, so it will eventually contain an apostrophe.
    /// XML has nothing to say about one, so it reached AAPT bare and AAPT rejected the
    /// resource -- an Android build failing on a title the developer was entitled to write.
    @Test
    public void anApostropheInATitleIsEscapedForAapt(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"intents\":[{\"id\":\"log_workout\","
                + "\"title\":\"Today's \\\"big\\\" workout\",\"headless\":false,"
                + "\"discoverable\":true,\"destructive\":false,\"opensRoute\":\"\","
                + "\"exposure\":[\"ASSISTANT\"],\"params\":[]}],\"entities\":[]}");

        File values = new File(new File(dir.toFile(), "res"), "values");
        String strings = new String(Files.readAllBytes(
                new File(values, "cn1_shortcuts_strings.xml").toPath()),
                Charset.forName("UTF-8"));

        assertTrue(strings.contains("Today\\'s"),
                "an unescaped apostrophe fails the Android build:\n" + strings);
        assertTrue(strings.contains("\\&quot;big\\&quot;"),
                "the quote needs the resource escape too, and it has to be applied before the "
                        + "XML escape or there is no quote left to see:\n" + strings);
    }

    /// A percent sign is ordinary text in a title, and a title is never a format string --
    /// nothing ever passes it an argument. AAPT does not know that: it parses every <string>
    /// as a potential format resource and rejects one carrying more than a single
    /// non-positional token, so "Move %s to %s" failed the whole Android build and told the
    /// developer to write positional arguments for a label that is only ever displayed.
    @Test
    public void aTitleCarryingFormatTokensDoesNotFailAapt(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"intents\":[{\"id\":\"move_item\","
                + "\"title\":\"Move %s to %s\",\"headless\":false,"
                + "\"discoverable\":true,\"destructive\":false,\"opensRoute\":\"\","
                + "\"exposure\":[\"ASSISTANT\"],\"params\":[]}],\"entities\":[]}");

        File values = new File(new File(dir.toFile(), "res"), "values");
        String strings = new String(Files.readAllBytes(
                new File(values, "cn1_shortcuts_strings.xml").toPath()),
                Charset.forName("UTF-8"));

        assertTrue(strings.contains("formatted=\"false\""),
                "without formatted=\"false\" AAPT reads the label as a format string and "
                        + "rejects multiple non-positional substitutions:\n" + strings);
        assertTrue(strings.contains("Move %s to %s"),
                "the title itself must survive verbatim:\n" + strings);
    }

    /// res/xml is not processed for manifest placeholders, so ${applicationId} written there
    /// reaches the launcher literally and the explicit component cannot resolve -- every
    /// An explicitly written exposure = {} chose no platform consumer. The processor preserves
    /// it; treating it as the default here published a launcher shortcut that this app's own
    /// trampoline then refuses, which reads to the user as the action being broken.
    @Test
    void anEmptyExposureIsNotALauncherShortcut(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\": 1, \"intents\": ["
                + "{\"id\": \"internal_only\", \"title\": \"Internal\","
                + " \"headless\": true, \"discoverable\": true, \"destructive\": false,"
                + " \"opensRoute\": \"\", \"params\": [], \"exposure\": []},"
                + "{\"id\": \"log_workout\", \"title\": \"Log a workout\","
                + " \"headless\": true, \"discoverable\": true, \"destructive\": false,"
                + " \"opensRoute\": \"\", \"params\": [],"
                + " \"exposure\": [\"ASSISTANT\"]}]}");

        String xml = shortcutsXml(dir);

        assertFalse(xml.contains("internal_only"),
                "an empty exposure selected no consumer: " + xml);
        assertTrue(xml.contains("log_workout"));
    }

    /// The quota keeps the first few and drops the rest, and the order it kept them in came
    /// from ClassScanner walking File.listFiles(), which the filesystem orders however it
    /// likes. Two clean builds of unchanged source on different runners could publish
    /// different shortcuts, so an action a user had could vanish on a rebuild that changed
    /// nothing.
    @Test
    void theShortcutSubsetDoesNotDependOnTraversalOrder(@TempDir Path dir) throws Exception {
        StringBuilder json = new StringBuilder("{\"schema\":1,\"intents\":[");
        String[] declaredInThisOrder = {"zulu", "mike", "alpha", "delta", "bravo"};
        for (int i = 0; i < declaredInThisOrder.length; i++) {
            json.append(i == 0 ? "" : ",");
            json.append("{\"id\":\"").append(declaredInThisOrder[i])
                    .append("\",\"title\":\"T\",\"discoverable\":true}");
        }
        json.append("]}");

        entriesFor(dir, json.toString());

        String xml = shortcutsXml(dir);
        assertTrue(xml.contains("alpha"), xml);
        assertTrue(xml.contains("bravo"), xml);
        assertTrue(xml.contains("delta"), xml);
        assertFalse(xml.contains("mike"),
                "the subset is chosen by id, not by whatever order the disk gave: " + xml);
        assertFalse(xml.contains("zulu"));
    }

    /// A static shortcut is minted at build time, so it carries no runtime nonce and the
    /// trampoline treats a tap on it as unauthenticated -- and that policy refuses anything
    /// destructive. Emitting one anyway would put an entry on the launcher that opens the app
    /// and logs a refusal on every tap, which reads as the action being broken rather than
    /// protected. It is not donatable either, which is what the build message now says.
    @Test
    void aDestructiveIntentIsNeverAStaticShortcut(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\": 1, \"intents\": ["
                + "{\"id\": \"delete_all\", \"title\": \"Delete everything\","
                + " \"headless\": false, \"discoverable\": true, \"destructive\": true,"
                + " \"opensRoute\": \"\", \"params\": [],"
                + " \"exposure\": [\"ASSISTANT\"]},"
                + "{\"id\": \"log_workout\", \"title\": \"Log a workout\","
                + " \"headless\": true, \"discoverable\": true, \"destructive\": false,"
                + " \"opensRoute\": \"\", \"params\": [],"
                + " \"exposure\": [\"ASSISTANT\"]}]}");

        String xml = shortcutsXml(dir);

        assertFalse(xml.contains("delete_all"),
                "a destructive intent must not reach the launcher: " + xml);
        assertTrue(xml.contains("log_workout"),
                "and the rest of the catalogue is unaffected: " + xml);
    }

    /// generated static shortcut would open nothing at all.
    @Test
    void aShortcutNamesTheRealPackage(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\": 1, \"intents\": [{\"id\": \"log_workout\","
                + " \"title\": \"Log a workout\", \"headless\": true,"
                + " \"discoverable\": true, \"destructive\": false,"
                + " \"opensRoute\": \"\", \"params\": [],"
                + " \"exposure\": [\"ASSISTANT\"]}]}");

        String xml = shortcutsXml(dir);

        assertTrue(xml.contains("android:targetPackage=\"com.example.app\""), xml);
        assertFalse(xml.contains("${applicationId}"),
                "a manifest placeholder is not substituted in res/xml: " + xml);
    }

    /// A declared route needs a window to land in, so the URI must not claim headless however
    /// the intent was declared -- otherwise the trampoline hands it to the service and the Form
    /// is built where nobody sees it, in a runtime that then stops.
    @Test
    void anIntentWithARouteIsNotMarkedHeadless(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\": 1, \"intents\": [{\"id\": \"show_order\","
                + " \"title\": \"Show order\", \"headless\": true,"
                + " \"discoverable\": true, \"destructive\": false,"
                + " \"opensRoute\": \"/orders\", \"params\": [],"
                + " \"exposure\": [\"ASSISTANT\"]}]}");

        String xml = shortcutsXml(dir);

        assertTrue(xml.contains("show_order"), xml);
        assertFalse(xml.contains("h=1"),
                "a route has to open the app, so the shortcut must not ask for headless: " + xml);
    }

    /// The counterpart: a genuinely headless intent still gets the flag.
    @Test
    void aHeadlessIntentWithoutARouteKeepsTheFlag(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\": 1, \"intents\": [{\"id\": \"log_workout\","
                + " \"title\": \"Log a workout\", \"headless\": true,"
                + " \"discoverable\": true, \"destructive\": false,"
                + " \"opensRoute\": \"\", \"params\": [],"
                + " \"exposure\": [\"ASSISTANT\"]}]}");

        assertTrue(shortcutsXml(dir).contains("h=1"));
    }

    @Test
    void theServiceThatRunsCapabilitiesIsNotExported(@TempDir Path dir) throws Exception {
        String entries = entriesFor(dir, null);

        assertTrue(entries.contains("CN1IntentService"));
        assertTrue(entries.contains("android:name=\"com.codename1.impl.android.intents.CN1IntentService\"\n"
                        + "                 android:exported=\"false\"")
                        || entries.replaceAll("\\s+", " ").contains(
                                "CN1IntentService\" android:exported=\"false\""),
                "an exported service would let any installed app ask this one to perform an "
                        + "application capability:\n" + entries);
    }

    @Test
    void onlyTheTrampolineIsExported(@TempDir Path dir) throws Exception {
        String entries = entriesFor(dir, null);
        String flat = entries.replaceAll("\\s+", " ");

        assertTrue(flat.contains("CN1IntentTrampolineActivity\" android:theme"), flat);
        assertTrue(flat.contains("android:exported=\"true\""));
        assertTrue(flat.contains("android:scheme=\"cn1intent\""));
    }

    @Test
    void aMissingManifestStillWiresTheRuntimeButWritesNoShortcuts(@TempDir Path dir)
            throws Exception {
        // Legitimate: the app only indexes content or donates at runtime, so nothing was
        // generated at build time. iOS behaves the same way; surfaces, by contrast, fails.
        String entries = entriesFor(dir, null);

        assertTrue(entries.contains("CN1IntentTrampolineActivity"));
        assertFalse(new File(new File(dir.toFile(), "res"), "xml/cn1_shortcuts.xml").exists());
    }

    @Test
    void aDeclaredIntentBecomesAStaticShortcut(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\":1,\"intents\":[{\"id\":\"log_workout\","
                + "\"title\":\"Log a workout\",\"discoverable\":true}]}");

        String xml = shortcutsXml(dir);
        assertTrue(xml.contains("android:shortcutId=\"log_workout\""));
        assertTrue(xml.contains("android:shortcutShortLabel=\"@string/cn1_shortcut_log_workout\""));
        assertTrue(xml.contains("cn1intent://run?id=log_workout"));
        assertTrue(xml.contains("CN1IntentTrampolineActivity"));
    }

    @Test
    void anIntentThatOptedOutOfDiscoveryIsNotOffered(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\":1,\"intents\":["
                + "{\"id\":\"public_one\",\"title\":\"Public\",\"discoverable\":true},"
                + "{\"id\":\"hidden_one\",\"title\":\"Hidden\",\"discoverable\":false}]}");

        String xml = shortcutsXml(dir);
        assertTrue(xml.contains("public_one"));
        assertFalse(xml.contains("hidden_one"),
                "discoverable=false means the platform must not offer it unprompted");
    }

    @Test
    void theStaticShortcutListLeavesRoomForRuntimePublications(@TempDir Path dir) throws Exception {
        StringBuilder json = new StringBuilder("{\"schema\":1,\"intents\":[");
        for (int i = 0; i < 9; i++) {
            json.append(i == 0 ? "" : ",");
            json.append("{\"id\":\"intent_").append(i).append("\",\"title\":\"T").append(i)
                    .append("\",\"discoverable\":true}");
        }
        json.append("]}");

        entriesFor(dir, json.toString());

        String xml = shortcutsXml(dir);
        int count = xml.split("<shortcut ").length - 1;
        assertEquals(3, count,
                "getMaxShortcutCountPerActivity() returns 5 on plenty of devices and that is "
                        + "the combined static and dynamic quota, so filling it here left "
                        + "Intents.index() and Intents.donate() nothing to publish into -- and "
                        + "pushDynamicShortcut cannot evict a manifest shortcut to make room. "
                        + "The reserved slots are the only thing the build can do about it.");
    }

    @Test
    void theShortcutMetaDataIsDestinedForTheLauncherActivity(@TempDir Path dir) throws Exception {
        String metaData = metaDataAfter(dir, "{\"schema\":1,\"intents\":"
                + "[{\"id\":\"log_workout\",\"title\":\"Log\",\"discoverable\":true}]}");

        assertTrue(metaData.contains("android.app.shortcuts"));
        assertTrue(metaData.contains("@xml/cn1_shortcuts"));

        String entries = entriesFor(dir, "{\"schema\":1,\"intents\":"
                + "[{\"id\":\"log_workout\",\"title\":\"Log\",\"discoverable\":true}]}");
        assertFalse(entries.contains("android.app.shortcuts"),
                "the launcher ignores this meta-data outside the launcher activity, so emitting "
                        + "it at application level would silently produce no shortcuts");
    }

    @Test
    void aHeadlessIntentCarriesItsFlagInTheShortcutUri(@TempDir Path dir) throws Exception {
        // At a cold start the declaration table is not installed yet, so the trampoline cannot
        // look the flag up -- it has to arrive in the URI or the app visibly opens.
        entriesFor(dir, "{\"schema\":1,\"intents\":[{\"id\":\"log_workout\","
                + "\"title\":\"Log\",\"discoverable\":true,\"headless\":true}]}");

        String xml = shortcutsXml(dir);
        assertTrue(xml.contains("cn1intent://run?id=log_workout&amp;h=1"), xml);
    }

    @Test
    void aForegroundIntentCarriesNoHeadlessFlag(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\":1,\"intents\":[{\"id\":\"show_order\","
                + "\"title\":\"Show\",\"discoverable\":true,\"headless\":false}]}");

        String xml = shortcutsXml(dir);
        assertTrue(xml.contains("cn1intent://run?id=show_order\""), xml);
        assertFalse(xml.contains("h=1"), xml);
    }

    @Test
    void shortcutLabelsAreResourceBackedBecauseAaptDemandsIt(@TempDir Path dir) throws Exception {
        // android:shortcutShortLabel is defined as a resource reference; a literal is rejected
        // by AAPT during resource linking, so the whole Android build fails.
        entriesFor(dir, "{\"schema\":1,\"intents\":[{\"id\":\"log_workout\","
                + "\"title\":\"Log a workout\",\"discoverable\":true}]}");

        String xml = shortcutsXml(dir);
        assertTrue(xml.contains("android:shortcutShortLabel=\"@string/cn1_shortcut_log_workout\""),
                xml);

        File strings = new File(new File(dir.toFile(), "res"), "values/cn1_shortcuts_strings.xml");
        assertTrue(strings.exists(), "the referenced string resource was not written");
        String body = new String(Files.readAllBytes(strings.toPath()), Charset.forName("UTF-8"));
        assertTrue(body.contains(
                "<string formatted=\"false\" name=\"cn1_shortcut_log_workout\">"
                        + "Log a workout</string>"),
                body);
    }

    @Test
    void anIntentNeedingAParameterIsNotOfferedAsAStaticShortcut(@TempDir Path dir)
            throws Exception {
        // A static shortcut carries no parameters and Android has no picker on this path, so
        // offering one would invoke the handler with a null or a zero and do the wrong thing.
        entriesFor(dir, "{\"schema\":1,\"intents\":[{\"id\":\"log_workout\","
                + "\"title\":\"Log\",\"discoverable\":true,\"params\":["
                + "{\"name\":\"kind\",\"type\":\"string\",\"required\":true}]}]}");

        assertFalse(new File(new File(dir.toFile(), "res"), "xml/cn1_shortcuts.xml").exists(),
                "no launchable intent means no shortcut resource at all");
    }

    @Test
    void aRequiredParameterWithADefaultIsStillLaunchable(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\":1,\"intents\":[{\"id\":\"log_workout\","
                + "\"title\":\"Log\",\"discoverable\":true,\"params\":["
                + "{\"name\":\"kind\",\"type\":\"string\",\"required\":true,"
                + "\"default\":\"run\"}]}]}");

        assertTrue(shortcutsXml(dir).contains("log_workout"));
    }

    @Test
    void anOptionalParameterDoesNotBlockAShortcut(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\":1,\"intents\":[{\"id\":\"log_workout\","
                + "\"title\":\"Log\",\"discoverable\":true,\"params\":["
                + "{\"name\":\"note\",\"type\":\"string\",\"required\":false}]}]}");

        assertTrue(shortcutsXml(dir).contains("log_workout"));
    }

    @Test
    void aModelOnlyIntentIsNotPutOnTheLauncher(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\":1,\"intents\":["
                + "{\"id\":\"public_one\",\"title\":\"Public\",\"discoverable\":true,"
                + "\"exposure\":[\"ASSISTANT\"]},"
                + "{\"id\":\"model_one\",\"title\":\"Model\",\"discoverable\":true,"
                + "\"exposure\":[\"MODEL\"]}]}");

        String xml = shortcutsXml(dir);
        assertTrue(xml.contains("public_one"));
        assertFalse(xml.contains("model_one"),
                "exposure is a restriction: a model-only capability is not a launcher action");
    }

    @Test
    void aTitleWithMarkupCannotBreakTheResource(@TempDir Path dir) throws Exception {
        entriesFor(dir, "{\"schema\":1,\"intents\":[{\"id\":\"log_workout\","
                + "\"title\":\"Log <b>a</b> \\\"workout\\\" & more\",\"discoverable\":true}]}");

        // The title now lives in the string resource the shortcut references, so that is where
        // unescaped markup would break the build.
        File strings = new File(new File(dir.toFile(), "res"), "values/cn1_shortcuts_strings.xml");
        String body = new String(Files.readAllBytes(strings.toPath()), Charset.forName("UTF-8"));
        assertTrue(body.contains("&lt;b&gt;"), body);
        assertTrue(body.contains("&quot;"), body);
        assertTrue(body.contains("&amp;"), body);
    }
}
