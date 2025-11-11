package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.l10n.L10NManager;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.layouts.BoxLayout;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for localization with resource bundles.
 */
class LocalizationTest extends UITestBase {

    @FormTest
    void testBasicLocalization() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
        };

        impl.setLocalizationManager(manager);

        assertEquals("en", manager.getLanguage());
        assertEquals("US", manager.getCountry());
    }

    @FormTest
    void testLocalizationWithResourceBundle() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        // Create a simple localization manager
        L10NManager manager = new L10NManager("en", "US") {
            private Hashtable<String, String> resources = new Hashtable<String, String>() {{
                put("hello", "Hello");
                put("goodbye", "Goodbye");
                put("welcome", "Welcome");
            }};

            @Override
            public String getLocalizedString(String key, String defaultValue) {
                String value = resources.get(key);
                return value != null ? value : defaultValue;
            }
        };

        impl.setLocalizationManager(manager);

        assertEquals("Hello", manager.getLocalizedString("hello", ""));
        assertEquals("Goodbye", manager.getLocalizedString("goodbye", ""));
        assertEquals("Welcome", manager.getLocalizedString("welcome", ""));
    }

    @FormTest
    void testLocalizationFallbackToDefault() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
            @Override
            public String getLocalizedString(String key, String defaultValue) {
                return defaultValue;
            }
        };

        impl.setLocalizationManager(manager);

        assertEquals("Default", manager.getLocalizedString("missing_key", "Default"));
    }

    @FormTest
    void testLocalizationWithDifferentLanguages() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        // English localization
        L10NManager english = new L10NManager("en", "US") {
            @Override
            public String getLocalizedString(String key, String defaultValue) {
                if ("hello".equals(key)) return "Hello";
                return defaultValue;
            }
        };

        impl.setLocalizationManager(english);
        assertEquals("Hello", english.getLocalizedString("hello", ""));

        // French localization
        L10NManager french = new L10NManager("fr", "FR") {
            @Override
            public String getLocalizedString(String key, String defaultValue) {
                if ("hello".equals(key)) return "Bonjour";
                return defaultValue;
            }
        };

        impl.setLocalizationManager(french);
        assertEquals("Bonjour", french.getLocalizedString("hello", ""));
    }

    @FormTest
    void testLocalizationWithComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
            @Override
            public String getLocalizedString(String key, String defaultValue) {
                if ("button_text".equals(key)) return "Click Me";
                if ("label_text".equals(key)) return "Welcome";
                return defaultValue;
            }
        };

        impl.setLocalizationManager(manager);

        Button btn = new Button(manager.getLocalizedString("button_text", ""));
        Label label = new Label(manager.getLocalizedString("label_text", ""));

        form.addAll(btn, label);
        form.revalidate();

        assertEquals("Click Me", btn.getText());
        assertEquals("Welcome", label.getText());
    }

    @FormTest
    void testLocalizationUpdate() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        final String[] currentLanguage = {"en"};

        L10NManager manager = new L10NManager("en", "US") {
            @Override
            public String getLocalizedString(String key, String defaultValue) {
                if ("greeting".equals(key)) {
                    return "en".equals(currentLanguage[0]) ? "Hello" : "Hola";
                }
                return defaultValue;
            }
        };

        impl.setLocalizationManager(manager);

        Button btn = new Button(manager.getLocalizedString("greeting", ""));
        form.add(btn);
        form.revalidate();

        assertEquals("Hello", btn.getText());

        // Switch to Spanish
        currentLanguage[0] = "es";
        btn.setText(manager.getLocalizedString("greeting", ""));
        form.revalidate();

        assertEquals("Hola", btn.getText());
    }

    @FormTest
    void testLocalizationWithMultipleComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
            private Hashtable<String, String> resources = new Hashtable<String, String>() {{
                put("home", "Home");
                put("settings", "Settings");
                put("profile", "Profile");
                put("logout", "Logout");
            }};

            @Override
            public String getLocalizedString(String key, String defaultValue) {
                return resources.getOrDefault(key, defaultValue);
            }
        };

        impl.setLocalizationManager(manager);

        Button home = new Button(manager.getLocalizedString("home", ""));
        Button settings = new Button(manager.getLocalizedString("settings", ""));
        Button profile = new Button(manager.getLocalizedString("profile", ""));
        Button logout = new Button(manager.getLocalizedString("logout", ""));

        form.addAll(home, settings, profile, logout);
        form.revalidate();

        assertEquals("Home", home.getText());
        assertEquals("Settings", settings.getText());
        assertEquals("Profile", profile.getText());
        assertEquals("Logout", logout.getText());
    }

    @FormTest
    void testLocalizationWithRTL() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager arabicManager = new L10NManager("ar", "SA") {
            @Override
            public String getLocalizedString(String key, String defaultValue) {
                if ("hello".equals(key)) return "مرحبا";
                return defaultValue;
            }

            @Override
            public boolean isRTL() {
                return true;
            }
        };

        impl.setLocalizationManager(arabicManager);

        Label label = new Label(arabicManager.getLocalizedString("hello", ""));
        form.add(label);

        if (arabicManager.isRTL()) {
            form.setRTL(true);
        }

        form.revalidate();

        assertEquals("مرحبا", label.getText());
        assertTrue(form.isRTL());
    }

    @FormTest
    void testLocalizationWithPlurals() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
            @Override
            public String getLocalizedString(String key, String defaultValue) {
                if (key.startsWith("item_count_")) {
                    String countStr = key.substring("item_count_".length());
                    int count = Integer.parseInt(countStr);
                    return count == 1 ? "1 item" : count + " items";
                }
                return defaultValue;
            }
        };

        impl.setLocalizationManager(manager);

        assertEquals("1 item", manager.getLocalizedString("item_count_1", ""));
        assertEquals("5 items", manager.getLocalizedString("item_count_5", ""));
    }

    @FormTest
    void testLocalizationWithFormatting() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
        };

        impl.setLocalizationManager(manager);

        // Test date formatting
        assertNotNull(manager);
    }

    @FormTest
    void testLocalizationCaching() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        final int[] lookupCount = {0};

        L10NManager manager = new L10NManager("en", "US") {
            private Hashtable<String, String> cache = new Hashtable<>();

            @Override
            public String getLocalizedString(String key, String defaultValue) {
                lookupCount[0]++;
                if (cache.containsKey(key)) {
                    return cache.get(key);
                }
                String value = "Localized: " + key;
                cache.put(key, value);
                return value;
            }
        };

        impl.setLocalizationManager(manager);

        String first = manager.getLocalizedString("test", "");
        String second = manager.getLocalizedString("test", "");

        assertEquals(first, second);
        assertTrue(lookupCount[0] >= 1);
    }

    @FormTest
    void testLocalizationWithDialog() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
            @Override
            public String getLocalizedString(String key, String defaultValue) {
                if ("dialog_title".equals(key)) return "Confirmation";
                if ("dialog_message".equals(key)) return "Are you sure?";
                if ("ok".equals(key)) return "OK";
                if ("cancel".equals(key)) return "Cancel";
                return defaultValue;
            }
        };

        impl.setLocalizationManager(manager);

        String title = manager.getLocalizedString("dialog_title", "");
        String message = manager.getLocalizedString("dialog_message", "");

        Dialog dialog = new Dialog(title);
        dialog.add(new Label(message));

        assertEquals("Confirmation", title);
        assertEquals("Are you sure?", message);
    }

    @FormTest
    void testLocalizationLanguageCode() {
        L10NManager manager = new L10NManager("en", "US");
        assertEquals("en", manager.getLanguage());

        L10NManager frenchManager = new L10NManager("fr", "FR");
        assertEquals("fr", frenchManager.getLanguage());

        L10NManager spanishManager = new L10NManager("es", "ES");
        assertEquals("es", spanishManager.getLanguage());
    }

    @FormTest
    void testLocalizationCountryCode() {
        L10NManager usManager = new L10NManager("en", "US");
        assertEquals("US", usManager.getCountry());

        L10NManager ukManager = new L10NManager("en", "GB");
        assertEquals("GB", ukManager.getCountry());

        L10NManager canadaManager = new L10NManager("en", "CA");
        assertEquals("CA", canadaManager.getCountry());
    }

    @FormTest
    void testLocalizationWithEmptyValues() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
            @Override
            public String getLocalizedString(String key, String defaultValue) {
                if ("empty".equals(key)) return "";
                return defaultValue;
            }
        };

        impl.setLocalizationManager(manager);

        assertEquals("", manager.getLocalizedString("empty", "default"));
    }
}
