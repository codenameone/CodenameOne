package com.codename1.io;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreferencesTest {

    @BeforeEach
    void setUp() {
        TestImplementationProvider.resetImplementation();
        TestImplementationProvider.installImplementation(true);
    }

    @AfterEach
    void tearDown() {
        TestImplementationProvider.resetImplementation();
    }

    @Test
    void setAndGetVariousTypes() {
        Preferences.set("string", "value");
        Preferences.set("int", 42);
        Preferences.set("long", 1000L);
        Preferences.set("double", 3.5d);
        Preferences.set("float", 2.5f);
        Preferences.set("bool", true);

        assertEquals("value", Preferences.get("string", ""));
        assertEquals(42, Preferences.get("int", 0));
        assertEquals(1000L, Preferences.get("long", 0L));
        assertEquals(3.5d, Preferences.get("double", 0d));
        assertEquals(2.5f, Preferences.get("float", 0f));
        assertTrue(Preferences.get("bool", false));
    }

    @Test
    void getAndSetPersistsDefaults() {
        assertEquals("fallback", Preferences.getAndSet("missingString", "fallback"));
        assertEquals("fallback", Preferences.get("missingString", ""));

        assertEquals(10, Preferences.getAndSet("missingInt", 10));
        assertEquals(10, Preferences.get("missingInt", 0));

        Preferences.set("missingInt", 20);
        assertEquals(20, Preferences.getAndSet("missingInt", 0));
    }

    @Test
    void batchSetUpdatesMultipleValues() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("one", "1");
        updates.put("two", 2);
        Preferences.set(updates);

        assertEquals("1", Preferences.get("one", ""));
        assertEquals(2, Preferences.get("two", 0));
    }

    @Test
    void deleteAndClearAllTriggerListeners() {
        AtomicReference<Object> prior = new AtomicReference<>();
        AtomicReference<Object> current = new AtomicReference<>();
        Preferences.addPreferenceListener("key", (pref, oldValue, newValue) -> {
            prior.set(oldValue);
            current.set(newValue);
        });
        Preferences.set("key", "first");
        Preferences.set("key", "second");
        assertEquals("first", prior.get());
        assertEquals("second", current.get());

        Preferences.delete("key");
        assertEquals("second", prior.get());
        assertNull(current.get());

        Preferences.set("key", "restored");
        Preferences.clearAll();
        assertEquals("restored", prior.get());
        assertNull(current.get());
    }

    @Test
    void preferenceLocationIsolation() {
        Preferences.set("shared", "original");
        String defaultLocation = Preferences.getPreferencesLocation();

        Preferences.setPreferencesLocation("CustomPrefs");
        assertEquals("fallback", Preferences.get("shared", "fallback"));
        Preferences.set("shared", "custom");

        Preferences.setPreferencesLocation(defaultLocation);
        assertEquals("original", Preferences.get("shared", ""));
    }

}
