package com.codename1.impl.javase.simulator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parse-level coverage for {@link SimulatorHookLoader}. The loader's parser
 * is the contract cn1libs depend on: a malformed properties file from one
 * cn1lib must not poison the rest of the menu, and well-formed files must
 * round-trip name/label/action faithfully.
 *
 * The CN1-EDT dispatch wrapper inside each Runnable is intentionally not
 * exercised here (would require a running Display); the resolved {@code Method}
 * is checked indirectly by relying on the loader to skip entries with
 * unresolvable or non-static targets.
 */
class SimulatorHookLoaderTest {

    @Test
    void parsesWellFormedFile(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "name=Bluetooth\n"
                + "item1.label=Alpha\n"
                + "item1.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "item2.label=Beta\n"
                + "item2.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#beta\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(2, hooks.size());
        assertEquals("Bluetooth", hooks.get(0).getMenuName());
        assertEquals("Alpha", hooks.get(0).getLabel());
        assertEquals("Bluetooth", hooks.get(1).getMenuName());
        assertEquals("Beta", hooks.get(1).getLabel());
        assertNotNull(hooks.get(0).getInvoke());
        assertNotNull(hooks.get(1).getInvoke());
    }

    @Test
    void preservesDeclarationOrder(@TempDir Path tempDir) throws Exception {
        // item3/item1/item2 in file order should appear in that order, not sorted.
        writeProps(tempDir, "name=Bluetooth\n"
                + "item3.label=Third\n"
                + "item3.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "item1.label=First\n"
                + "item1.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "item2.label=Second\n"
                + "item2.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(3, hooks.size());
        assertEquals("Third", hooks.get(0).getLabel());
        assertEquals("First", hooks.get(1).getLabel());
        assertEquals("Second", hooks.get(2).getLabel());
    }

    @Test
    void skipsFileWithoutName(@TempDir Path tempDir) throws Exception {
        // Missing "name=" → entire file dropped, no exception.
        writeProps(tempDir, "item1.label=Orphan\n"
                + "item1.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertTrue(hooks.isEmpty(), "expected zero hooks but got: " + hooks);
    }

    @Test
    void skipsItemWithoutMatchingAction(@TempDir Path tempDir) throws Exception {
        // item1 has label but no action; item2 is well-formed → only item2 survives.
        writeProps(tempDir, "name=Bluetooth\n"
                + "item1.label=Dangling\n"
                + "item2.label=Beta\n"
                + "item2.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#beta\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size());
        assertEquals("Beta", hooks.get(0).getLabel());
    }

    @Test
    void skipsUnknownClassButKeepsRest(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "name=Bluetooth\n"
                + "item1.label=Missing\n"
                + "item1.action=com.example.DoesNotExist#nope\n"
                + "item2.label=Alpha\n"
                + "item2.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size());
        assertEquals("Alpha", hooks.get(0).getLabel());
    }

    @Test
    void skipsNonStaticMethod(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "name=Bluetooth\n"
                + "item1.label=Instance\n"
                + "item1.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#instanceOnly\n"
                + "item2.label=Alpha\n"
                + "item2.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size());
        assertEquals("Alpha", hooks.get(0).getLabel());
    }

    @Test
    void skipsMalformedActionString(@TempDir Path tempDir) throws Exception {
        // No '#' separator at all.
        writeProps(tempDir, "name=Bluetooth\n"
                + "item1.label=Bad\n"
                + "item1.action=not_a_method_reference\n"
                + "item2.label=Alpha\n"
                + "item2.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size());
        assertEquals("Alpha", hooks.get(0).getLabel());
    }

    /** Writes the fixture to {@code <tempDir>/META-INF/codenameone/simulator-hooks.properties}. */
    private static void writeProps(Path tempDir, String content) throws Exception {
        Path metaInf = tempDir.resolve("META-INF").resolve("codenameone");
        Files.createDirectories(metaInf);
        File f = metaInf.resolve("simulator-hooks.properties").toFile();
        FileOutputStream out = new FileOutputStream(f);
        try {
            Writer w = new OutputStreamWriter(out, "UTF-8");
            w.write(content);
            w.flush();
        } finally {
            out.close();
        }
    }

    /**
     * Classloader whose only "extra" root is the temp dir, so
     * {@code getResources("META-INF/codenameone/simulator-hooks.properties")}
     * sees exactly the fixture and the fixture class is resolvable via parent
     * delegation. Avoids polluting the surrounding test classpath with a
     * resource that other tests would also discover.
     */
    private static ClassLoader classloaderFor(Path tempDir) throws Exception {
        URL url = tempDir.toUri().toURL();
        return new URLClassLoader(new URL[]{url}, SimulatorHookLoaderTest.class.getClassLoader());
    }
}
