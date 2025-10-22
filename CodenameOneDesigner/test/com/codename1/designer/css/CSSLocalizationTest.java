package com.codename1.designer.css;

import com.codename1.ui.util.EditableResources;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Basic regression tests for the CSS localization integration.
 */
public class CSSLocalizationTest {

    public static void main(String[] args) throws Exception {
        testLoadLocalizationBundles();
        testApplyLocalizationBundles();
    }

    private static void testLoadLocalizationBundles() throws Exception {
        Path tempDir = Files.createTempDirectory("cn1-css-localization");
        try {
            Path localizationRoot = Files.createDirectory(tempDir.resolve("l10n"));

            writeProperties(localizationRoot.resolve("Messages.properties"),
                    "greeting=Hello" + System.lineSeparator());
            writeProperties(localizationRoot.resolve("Messages_fr.properties"),
                    "greeting=Bonjour" + System.lineSeparator());
            Path appDir = Files.createDirectories(localizationRoot.resolve("com/example"));
            writeProperties(appDir.resolve("App.properties"),
                    "title=Base" + System.lineSeparator());
            writeProperties(appDir.resolve("App_en_ca.properties"),
                    "title=Canadian" + System.lineSeparator());
            Path nested = Files.createDirectories(localizationRoot.resolve("nested"));
            writeProperties(nested.resolve("Bundle_sr_latn_RS.properties"),
                    "welcome=Welcome" + System.lineSeparator());

            Map<String, Map<String, Map<String, String>>> bundles = loadLocalizationBundles(localizationRoot.toFile());

            assertTrue(bundles.containsKey("Messages"), "Expected Messages bundle to be detected");
            Map<String, Map<String, String>> messages = bundles.get("Messages");
            assertEquals(new TreeSet<>(messages.keySet()), setOf("", "fr"), "Messages locales");
            assertEquals(messages.get(""), stringMap("greeting", "Hello"), "Default Messages translation");
            assertEquals(messages.get("fr"), stringMap("greeting", "Bonjour"), "French Messages translation");

            assertTrue(bundles.containsKey("com.example.App"), "Expected com.example.App bundle");
            Map<String, Map<String, String>> app = bundles.get("com.example.App");
            assertEquals(new TreeSet<>(app.keySet()), setOf("", "en_CA"), "App locales");
            assertEquals(app.get(""), stringMap("title", "Base"), "Default App translation");
            assertEquals(app.get("en_CA"), stringMap("title", "Canadian"), "Canadian App translation");

            assertTrue(bundles.containsKey("nested.Bundle"), "Expected nested.Bundle bundle");
            Map<String, Map<String, String>> nestedBundle = bundles.get("nested.Bundle");
            assertEquals(new TreeSet<>(nestedBundle.keySet()), setOf("sr_Latn_RS"), "Nested bundle locale");
            assertEquals(nestedBundle.get("sr_Latn_RS"), stringMap("welcome", "Welcome"), "Serbian translation");
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private static void testApplyLocalizationBundles() throws Exception {
        Map<String, Map<String, Map<String, String>>> bundles = new LinkedHashMap<>();
        Map<String, Map<String, String>> messages = new LinkedHashMap<>();
        messages.put("", stringMap("greeting", "Hello"));
        messages.put("fr", stringMap("greeting", "Bonjour"));
        bundles.put("Messages", messages);

        Map<String, Map<String, String>> app = new LinkedHashMap<>();
        app.put("", stringMap("title", "Base"));
        app.put("en_CA", stringMap("title", "Canadian"));
        bundles.put("com.example.App", app);

        CSSTheme theme = new CSSTheme();
        Path tempRes = Files.createTempFile("css-localization", ".res");
        try {
            theme.resourceFile = tempRes.toFile();
            theme.res = null; // force lazy initialization
            theme.applyLocalizationBundles(bundles);

            EditableResources resources = theme.res;
            assertTrue(resources != null, "Resources should be initialized");
            Set<String> names = new TreeSet<>(Arrays.asList(resources.getL10NResourceNames()));
            assertEquals(names, setOf("Messages", "com.example.App"), "Localization bundle names");

            Hashtable<String, String> defaultMessages = resources.getL10N("Messages", "");
            Hashtable<String, String> frenchMessages = resources.getL10N("Messages", "fr");
            assertEquals(defaultMessages, stringMap("greeting", "Hello"), "Default Messages values");
            assertEquals(frenchMessages, stringMap("greeting", "Bonjour"), "French Messages values");

            Hashtable<String, String> defaultApp = resources.getL10N("com.example.App", "");
            Hashtable<String, String> canadianApp = resources.getL10N("com.example.App", "en_CA");
            assertEquals(defaultApp, stringMap("title", "Base"), "Default App values");
            assertEquals(canadianApp, stringMap("title", "Canadian"), "Canadian App values");
        } finally {
            Files.deleteIfExists(tempRes);
        }
    }

    private static Map<String, String> stringMap(String... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Entries must be key/value pairs");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put(entries[i], entries[i + 1]);
        }
        return result;
    }

    private static Set<String> setOf(String... values) {
        return new TreeSet<>(Arrays.asList(values));
    }

    private static void writeProperties(Path path, String contents) throws IOException {
        Files.write(path, contents.getBytes(StandardCharsets.ISO_8859_1),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Map<String, String>>> loadLocalizationBundles(File dir)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = CN1CSSCLI.class.getDeclaredMethod("loadLocalizationBundles", File.class);
        m.setAccessible(true);
        return (Map<String, Map<String, Map<String, String>>>) m.invoke(null, dir);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object actual, Object expected, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
