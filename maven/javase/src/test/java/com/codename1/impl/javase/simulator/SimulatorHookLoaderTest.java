package com.codename1.impl.javase.simulator;

import com.codename1.system.SimulatorHookExecutor;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parse-level coverage for {@link SimulatorHookLoader}. The loader's parser
 * is the contract cn1libs depend on: a malformed properties file from one
 * cn1lib must not poison the rest of the menu, and well-formed files must
 * round-trip name/label/action/namespace/id faithfully.
 *
 * The {@code Display.callSerially} dispatch wrapper inside each Runnable
 * is intentionally not exercised here (would require a running Display);
 * the resolved {@code Method} is checked indirectly by relying on the
 * loader to skip entries with unresolvable or non-static targets.
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
        SimulatorHook first = hooks.get(0);
        assertEquals("Bluetooth", first.getMenuName());
        assertEquals("bluetooth", first.getNamespace(), "namespace should default to slugified name");
        assertEquals("item1", first.getId(), "id should default to property prefix");
        assertEquals("Alpha", first.getLabel());
        assertEquals("bluetooth:item1", first.getExecutorKey());
        assertTrue(first.hasMenuLabel());
        assertNotNull(first.getInvoke());
        assertEquals("Beta", hooks.get(1).getLabel());
    }

    @Test
    void honorsExplicitNamespaceAndId(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "name=Bluetooth\n"
                + "namespace=bt\n"
                + "item1.id=toggleAdapter\n"
                + "item1.label=Toggle\n"
                + "item1.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size());
        assertEquals("bt", hooks.get(0).getNamespace());
        assertEquals("toggleAdapter", hooks.get(0).getId());
        assertEquals("bt:toggleAdapter", hooks.get(0).getExecutorKey());
    }

    @Test
    void slugifiesMultiWordName(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "name=Push Notifications!\n"
                + "item1.label=Send\n"
                + "item1.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals("push-notifications", hooks.get(0).getNamespace());
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
    void apiOnlyHookHasNullLabelButIsCallable(@TempDir Path tempDir) throws Exception {
        // Label-less item: registered with the executor, hidden from the menu.
        writeProps(tempDir, "name=Bluetooth\n"
                + "namespace=bt\n"
                + "item1.id=script\n"
                + "item1.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n");

        SimulatorHookLoaderTestFixture.resetCounters();

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size());
        assertFalse(hooks.get(0).hasMenuLabel(), "label-less item must be hidden from menu");
        assertNull(hooks.get(0).getLabel());
        // ...and via the cross-platform entry point:
        assertTrue(SimulatorHookExecutor.execute("bt:script"));
    }

    @Test
    void executorReceivesEveryRegisteredHook(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "name=Bluetooth\n"
                + "namespace=bt\n"
                + "item1.id=alpha\n"
                + "item1.label=Alpha\n"
                + "item1.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "item2.id=beta\n"
                + "item2.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#beta\n");

        SimulatorHookLoader.load(classloaderFor(tempDir));

        assertTrue(SimulatorHookExecutor.isRegistered("bt:alpha"));
        assertTrue(SimulatorHookExecutor.isRegistered("bt:beta"));
        assertFalse(SimulatorHookExecutor.isRegistered("bt:unknown"));
        assertFalse(SimulatorHookExecutor.execute("bt:unknown"),
                "execute() must return false for unknown ids without throwing");
    }

    @Test
    void skipsFileWithoutName(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "item1.label=Orphan\n"
                + "item1.action=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertTrue(hooks.isEmpty(), "expected zero hooks but got: " + hooks);
    }

    @Test
    void skipsItemWithoutMatchingAction(@TempDir Path tempDir) throws Exception {
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

    @Test
    void slugifyHandlesEdgeCases() {
        assertEquals("bluetooth", SimulatorHookLoader.slugify("Bluetooth"));
        assertEquals("push-notifications", SimulatorHookLoader.slugify("Push Notifications!"));
        assertEquals("a-b-c", SimulatorHookLoader.slugify("A__B__C"));
        assertEquals("foo123", SimulatorHookLoader.slugify("foo123"));
        assertEquals("", SimulatorHookLoader.slugify("###"));
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
