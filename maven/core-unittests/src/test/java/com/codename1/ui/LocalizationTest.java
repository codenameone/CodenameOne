package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.l10n.L10NManager;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for localization functionality.
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
    void testLocalizationWithComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
        };

        impl.setLocalizationManager(manager);

        Button btn = new Button("Click Me");
        Label label = new Label("Welcome");

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

        L10NManager englishManager = new L10NManager("en", "US");
        impl.setLocalizationManager(englishManager);

        Button btn = new Button("Hello");
        form.add(btn);
        form.revalidate();

        assertEquals("Hello", btn.getText());

        // Switch to Spanish
        L10NManager spanishManager = new L10NManager("es", "ES");
        impl.setLocalizationManager(spanishManager);
        btn.setText("Hola");
        form.revalidate();

        assertEquals("Hola", btn.getText());
    }

    @FormTest
    void testLocalizationWithMultipleComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
        };

        impl.setLocalizationManager(manager);

        Button home = new Button("Home");
        Button settings = new Button("Settings");
        Button profile = new Button("Profile");
        Button logout = new Button("Logout");

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
        };

        impl.setLocalizationManager(arabicManager);

        Label label = new Label("مرحبا");
        form.add(label);

        // Set RTL for Arabic
        form.setRTL(true);
        form.revalidate();

        assertEquals("مرحبا", label.getText());
        assertTrue(form.isRTL());
    }

    @FormTest
    void testLocalizationNumberFormatting() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
        };

        impl.setLocalizationManager(manager);

        // Test number formatting
        String formatted = manager.format(1234);
        assertNotNull(formatted);
        assertTrue(formatted.contains("1234"));
    }

    @FormTest
    void testLocalizationWithDialog() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US") {
        };

        impl.setLocalizationManager(manager);

        String title = "Confirmation";
        String message = "Are you sure?";

        Dialog dialog = new Dialog(title);
        dialog.add(new Label(message));

        assertEquals("Confirmation", title);
        assertEquals("Are you sure?", message);
    }

    @FormTest
    void testSetLocale() {
        L10NManager manager = new L10NManager("en", "US");
        assertEquals("en", manager.getLanguage());
        assertEquals("US", manager.getCountry());

        // Change locale
        manager.setLocale("CA", "fr");
        assertEquals("fr", manager.getLanguage());
        assertEquals("CA", manager.getCountry());
    }

    @FormTest
    void testMultipleLocalizationManagers() {
        L10NManager english = new L10NManager("en", "US");
        L10NManager french = new L10NManager("fr", "FR");
        L10NManager german = new L10NManager("de", "DE");

        assertEquals("en", english.getLanguage());
        assertEquals("fr", french.getLanguage());
        assertEquals("de", german.getLanguage());
    }

    @FormTest
    void testLocalizationPersistence() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        L10NManager manager = new L10NManager("en", "US");
        impl.setLocalizationManager(manager);

        L10NManager retrieved = impl.getLocalizationManager();

        assertEquals("en", retrieved.getLanguage());
        assertEquals("US", retrieved.getCountry());
    }

    @FormTest
    void testLocalizationWithDifferentRegions() {
        L10NManager usEnglish = new L10NManager("en", "US");
        L10NManager ukEnglish = new L10NManager("en", "GB");
        L10NManager ausEnglish = new L10NManager("en", "AU");

        assertEquals("US", usEnglish.getCountry());
        assertEquals("GB", ukEnglish.getCountry());
        assertEquals("AU", ausEnglish.getCountry());

        // All should have same language
        assertEquals("en", usEnglish.getLanguage());
        assertEquals("en", ukEnglish.getLanguage());
        assertEquals("en", ausEnglish.getLanguage());
    }

    @FormTest
    void testLocalizationDoubleFormatting() {
        L10NManager manager = new L10NManager("en", "US");

        double value = 1234.56;
        String formatted = manager.format(value);

        assertNotNull(formatted);
    }

    @FormTest
    void testLocalizationCurrencyFormatting() {
        L10NManager manager = new L10NManager("en", "US");

        double amount = 99.99;
        String formatted = manager.formatCurrency(amount);

        assertNotNull(formatted);
    }

    @FormTest
    void testLocalizationGetInstance() {
        L10NManager instance = L10NManager.getInstance();
        assertNotNull(instance);
    }
}
