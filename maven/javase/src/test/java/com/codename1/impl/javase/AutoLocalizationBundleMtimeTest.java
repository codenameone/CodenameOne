package com.codename1.impl.javase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Regression coverage for the feedback loop between {@code AutoLocalizationBundle} and the
/// CSS staleness check.
///
/// The auto-bundle persists to disk every time the running app reads a missing key (see
/// `AutoLocalizationBundle.get`). Without the mtime-preservation contract these tests pin,
/// each persist advanced the bundle file's modified-time, which would in turn make
/// `CompileCSSMojo.getLocalizationModificationTime` treat the runtime cache flush as a fresh
/// user edit and force a ~30s CSS recompile on every subsequent `cn1:run`. The reproducer is
/// the obvious dev-loop: opt in to "Auto Update Default Bundle", add `getString("hello")` to
/// the app, run twice -- the second `cn1:run` should not recompile CSS because the user
/// changed nothing.
///
/// `AutoLocalizationBundle` is a private inner class of `JavaSEPort`. The pre-existing
/// `AutoLocalizationBundleTest` in `tests/core/...` already drives it via reflection, so we
/// follow the same pattern here rather than widening the visibility just for tests.
class AutoLocalizationBundleMtimeTest {

    @Test
    void runtimePersistKeepsOriginalFileMtime(@TempDir Path tempDir) throws Exception {
        Path bundleFile = setupExistingBundle(tempDir, "hello=world\n");
        long originalMtime = ageFile(bundleFile, 60_000L);

        Object bundle = newBundle(bundleFile.toFile(), null);
        @SuppressWarnings("unchecked")
        Map<String, String> bundleMap = (Map<String, String>) bundle;

        // Trigger a runtime auto-fabrication: missing key -> persist.
        bundleMap.get("missingKey");

        assertEquals(originalMtime, bundleFile.toFile().lastModified(),
                "Auto-bundle persist must restore the file mtime so CompileCSSMojo's"
                        + " getLocalizationModificationTime doesn't treat a runtime cache flush"
                        + " as a fresh user edit");

        // And the actual content must still be written through.
        Properties on_disk = loadProperties(bundleFile);
        assertEquals("missingKey", on_disk.getProperty("missingKey"),
                "Mtime restoration must not skip the content write");
    }

    @Test
    void multipleRuntimePersistsKeepOriginalFileMtime(@TempDir Path tempDir) throws Exception {
        Path bundleFile = setupExistingBundle(tempDir, "hello=world\n");
        long originalMtime = ageFile(bundleFile, 60_000L);

        Object bundle = newBundle(bundleFile.toFile(), null);
        @SuppressWarnings("unchecked")
        Map<String, String> bundleMap = (Map<String, String>) bundle;

        // The simulator session reads many missing keys; each one persists. None of them is a
        // user edit, so none of them should advance the file mtime.
        bundleMap.get("one");
        bundleMap.get("two");
        bundleMap.get("three");
        bundleMap.put("four", "fourValue");

        assertEquals(originalMtime, bundleFile.toFile().lastModified(),
                "Successive auto-bundle persists must not drift the mtime forward");
    }

    @Test
    void externalUserEditPropagatesToMtimeEvenAfterAutoBundleWrite(@TempDir Path tempDir) throws Exception {
        Path bundleFile = setupExistingBundle(tempDir, "hello=world\n");
        ageFile(bundleFile, 60_000L);

        Object bundle = newBundle(bundleFile.toFile(), null);
        @SuppressWarnings("unchecked")
        Map<String, String> bundleMap = (Map<String, String>) bundle;

        bundleMap.get("first");
        long mtimeAfterFirstPersist = bundleFile.toFile().lastModified();

        // Simulate the user opening their IDE and adding a new translation while the
        // simulator is still running. The auto-bundle does NOT know about this edit
        // a priori; the next persist must detect it (mtime drifted from what we last
        // restored) and adopt the user's mtime as the new preserved baseline, so the
        // edit propagates through `CompileCSSMojo.getLocalizationModificationTime`.
        long userEditMtime = mtimeAfterFirstPersist + 30_000L;
        assertTrue(bundleFile.toFile().setLastModified(userEditMtime),
                "Test setup precondition: the host filesystem must accept setLastModified");

        bundleMap.get("triggersPersistAfterUserEdit");

        long mtimeAfterSecondPersist = bundleFile.toFile().lastModified();
        assertNotEquals(mtimeAfterFirstPersist, mtimeAfterSecondPersist,
                "After a user edit, the next auto-bundle persist must surface the user's mtime");
        assertEquals(userEditMtime, mtimeAfterSecondPersist,
                "The preserved mtime should follow the user's edit, not snap back to ours");
    }

    @Test
    void freshFilePersistEstablishesPreservedMtime(@TempDir Path tempDir) throws Exception {
        // Project with no Bundle.properties yet -- the constructor creates an empty file.
        Path l10nDir = Files.createDirectories(tempDir.resolve("l10n"));
        File bundleFile = new File(l10nDir.toFile(), "Bundle.properties");
        assertTrue(!bundleFile.exists());

        Object bundle = newBundle(bundleFile, null);
        @SuppressWarnings("unchecked")
        Map<String, String> bundleMap = (Map<String, String>) bundle;
        assertTrue(bundleFile.exists(), "Constructor should have created the empty bundle");

        long mtimeAfterCreation = bundleFile.lastModified();
        // Force the OS to record a clearly later "now" so a drift-forward bug would be visible.
        Thread.sleep(50L);

        bundleMap.get("alpha");
        bundleMap.get("beta");

        assertEquals(mtimeAfterCreation, bundleFile.lastModified(),
                "Once a baseline mtime is established (here, the empty-file creation timestamp),"
                        + " subsequent auto-bundle persists must not drift it forward.");
    }

    private Path setupExistingBundle(Path tempDir, String contents) throws Exception {
        Path l10nDir = Files.createDirectories(tempDir.resolve("l10n"));
        Path bundleFile = l10nDir.resolve("Bundle.properties");
        try (OutputStream out = new FileOutputStream(bundleFile.toFile())) {
            out.write(contents.getBytes("UTF-8"));
        }
        return bundleFile;
    }

    /// Files freshly created by the test land with "now" as their mtime, which makes it
    /// impossible to tell a real restoration from a no-op (current mtime == "now" either way).
    /// Push the mtime back by `millisInPast` so any drift-forward bug is unambiguous.
    private long ageFile(Path file, long millisInPast) {
        long target = System.currentTimeMillis() - millisInPast;
        assertTrue(file.toFile().setLastModified(target),
                "Test setup precondition: setLastModified must be honored on the host FS");
        return target;
    }

    private Object newBundle(File bundleFile, Map<String, String> base) throws Exception {
        Class<?> bundleClass = Class.forName("com.codename1.impl.javase.JavaSEPort$AutoLocalizationBundle");
        Constructor<?> ctor = bundleClass.getDeclaredConstructor(File.class, Map.class);
        ctor.setAccessible(true);
        return ctor.newInstance(bundleFile, base == null ? new HashMap<String, String>() : base);
    }

    private Properties loadProperties(Path file) throws Exception {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file.toFile())) {
            props.load(in);
        }
        return props;
    }
}
