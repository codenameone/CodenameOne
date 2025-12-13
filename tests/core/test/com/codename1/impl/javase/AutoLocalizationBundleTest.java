package com.codename1.impl.javase;

import com.codename1.testing.AbstractTest;
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

            return true;
        } finally {
            deleteRecursive(tempDir);
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
