package com.codename1.impl.javase;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.plaf.UIManager;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tests for the auto-updating localization bundle used by the Java SE simulator.
 */
public class AutoLocalizationBundleTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        File tempDir = File.createTempFile("cn1-auto-bundle", "");
        if (tempDir.exists() && !tempDir.delete()) {
            throw new RuntimeException("Failed to delete temp file " + tempDir);
        }
        if (!tempDir.mkdirs()) {
            throw new RuntimeException("Failed to create temp directory " + tempDir);
        }

        File bundleDir = new File(tempDir, "nested");
        File bundleFile = new File(bundleDir, "Bundle.properties");

        Map<String, String> base = new HashMap<String, String>();
        base.put("hello", "world");

        Class<?> bundleClass = Class.forName("com.codename1.impl.javase.JavaSEPort$AutoLocalizationBundle");
        Constructor<?> ctor = bundleClass.getDeclaredConstructor(File.class, Map.class);
        ctor.setAccessible(true);

        try {
            Object bundle = ctor.newInstance(bundleFile, base);
            @SuppressWarnings("unchecked")
            Map<String, String> bundleMap = (Map<String, String>) bundle;

            assertTrue(bundleFile.exists(), "Bundle file should have been created");

            Properties props = load(bundleFile);
            assertEqual("world", props.getProperty("hello"), "Base entries should be written to the bundle file");

            String generated = bundleMap.get("missingKey");
            assertEqual("missingKey", generated, "Missing lookups should generate default values");

            props = load(bundleFile);
            assertEqual("missingKey", props.getProperty("missingKey"), "Generated entry should be persisted");

            bundleMap.put("hello", "updated");
            props = load(bundleFile);
            assertEqual("updated", props.getProperty("hello"), "Explicit put should persist new value");

            bundleMap.remove("hello");
            props = load(bundleFile);
            assertNull(props.getProperty("hello"), "Removed keys should be deleted from the bundle file");

            Object bundleReloaded = ctor.newInstance(bundleFile, null);
            @SuppressWarnings("unchecked")
            Map<String, String> bundleReloadedMap = (Map<String, String>) bundleReloaded;
            assertEqual("missingKey", bundleReloadedMap.get("missingKey"), "Existing persisted values should be loaded");

            // Regression: meta-keys (anything starting with `@`) must NOT be auto-fabricated.
            // The auto-fabrication was breaking UIManager.setBundle, which queries `@im` /
            // `@rtl` on every install and uses null-vs-non-null to mean "feature disabled".
            // When the bundle echoed "@im" -> "@im", setBundle tokenized it, queried
            // "@im-@im", got "@im-@im" back, and parseTextFieldInputMode crashed on
            // substring(0, indexOf('=')) for a token with no '=' (issue #4850).
            assertNull(bundleReloadedMap.get("@im"), "@-prefixed meta-keys must not be auto-fabricated");
            assertNull(bundleReloadedMap.get("@rtl"), "@-prefixed meta-keys must not be auto-fabricated");
            assertNull(bundleReloadedMap.get("@im-FOO"), "@-prefixed meta-keys must not be auto-fabricated");

            // But real meta-key values that exist in the underlying file are still returned.
            // Stage one by writing it through the explicit put path (which persists to disk).
            bundleReloadedMap.put("@rtl", "true");
            assertEqual("true", bundleReloadedMap.get("@rtl"), "Existing meta-key values should still be returned");

            verifySetBundleSmokeOnFreshProject(ctor, tempDir);

            return true;
        } finally {
            deleteRecursive(tempDir);
        }
    }

    /// Smoke-tests the full simulator-init handoff: AutoLocalizationBundle wrapped around a
    /// fresh project's empty `src/main/l10n` directory, then handed to `UIManager.setBundle`
    /// the same way `JavaSEPort.enableAutoLocalizationBundle` does at simulator boot.
    ///
    /// Pre-fix this combination crashed deterministically: `setBundle` queried `@im` on the
    /// bundle, the bundle echoed `@im` back, `setBundle` tokenized that to `["@im"]`, queried
    /// `@im-@im`, got `@im-@im` back, and `parseTextFieldInputMode` blew up on
    /// `substring(0, indexOf('='))` for a token with no `=` (issue #4850).
    ///
    /// This regression was invisible in CI because `enableAutoLocalizationBundle` is gated on
    /// `cn1.autoDefaultResourceBundle`, which CI runners default to false but real users have
    /// stuck to true via the simulator menu. Exercising the gated code path directly here
    /// makes the regression catchable regardless of preference state.
    private void verifySetBundleSmokeOnFreshProject(Constructor<?> ctor, File tempDir) throws Exception {
        File freshProjectDir = new File(tempDir, "fresh-project");
        File freshBundleFile = new File(new File(freshProjectDir, "src" + File.separator + "main" + File.separator + "l10n"), "Bundle.properties");

        Object freshBundle = ctor.newInstance(freshBundleFile, null);
        @SuppressWarnings("unchecked")
        Map<String, String> freshBundleMap = (Map<String, String>) freshBundle;

        UIManager manager = UIManager.getInstance();
        Map<String, String> savedBundle = manager.getBundle();
        try {
            // Pre-fix: throws StringIndexOutOfBoundsException out of parseTextFieldInputMode.
            manager.setBundle(freshBundleMap);
            assertSame(freshBundleMap, manager.getBundle(), "setBundle should install the AutoLocalizationBundle on a fresh project");
        } finally {
            manager.setBundle(savedBundle);
        }
    }

    private Properties load(File file) throws Exception {
        Properties props = new Properties();
        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            try {
                props.load(fis);
            } finally {
                fis.close();
            }
        }
        return props;
    }

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
