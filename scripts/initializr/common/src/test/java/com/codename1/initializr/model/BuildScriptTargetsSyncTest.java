package com.codename1.initializr.model;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Guards the CLI build scripts (build.sh / build.bat) shipped inside common.zip.
 *
 * <p>These scripts are generated from {@code maven/cli/src/main/batsh/build.batsh}
 * but the committed copies are effectively frozen (the install-into-archetype copy
 * step points at a dead path and the batsh compiler needs PHP), so they drift: the
 * Mac native targets never reached them and the removed windows-device/UWP target
 * lingered. This test asserts both scripts expose the Mac native build targets and
 * no longer reference windows-device.
 *
 * <p>Companion guards: {@link IdeBuildTargetsSyncTest} (IDE bundles) and
 * {@link IdeaWorkspaceSyncTest} (IntelliJ workspace.xml byte-for-byte).
 */
public class BuildScriptTargetsSyncTest extends AbstractTest {

    private static final String[] SCRIPT_ENTRIES = { "build.sh", "build.bat" };

    private static final String[] REQUIRED_TARGETS = {
            "mac-os-x-native", // mac_native (cloud)
            "mac-source"       // mac_native_source (local Xcode)
    };

    private static final String[] FORBIDDEN_TARGETS = {
            "windows-device" // UWP build, removed in #4624
    };

    @Override
    public boolean runTest() throws Exception {
        Map<String, byte[]> entries = readZipEntries(readResource("/common.zip"));
        for (String script : SCRIPT_ENTRIES) {
            byte[] bytes = entries.get(script);
            assertNotNull(bytes, "common.zip is missing " + script);
            String content = new String(bytes, "UTF-8");
            for (String target : REQUIRED_TARGETS) {
                assertTrue(content.contains(target),
                        script + " (in common.zip) is missing build target '" + target
                                + "'. Update maven/cli/src/main/batsh/build.batsh and regenerate"
                                + " the build scripts in common.zip.");
            }
            for (String target : FORBIDDEN_TARGETS) {
                assertFalse(content.contains(target),
                        script + " (in common.zip) still references removed build target '" + target
                                + "'. Update maven/cli/src/main/batsh/build.batsh and regenerate"
                                + " the build scripts in common.zip.");
            }
        }
        return true;
    }

    private byte[] readResource(String path) throws IOException {
        InputStream in = getClass().getResourceAsStream(path);
        assertNotNull(in, "Missing classpath resource " + path);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Util.copyNoClose(in, bos, 8192);
            return bos.toByteArray();
        } finally {
            in.close();
        }
    }

    private static Map<String, byte[]> readZipEntries(byte[] zipData) throws IOException {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipData));
        try {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    Util.copyNoClose(zis, bos, 8192);
                    entries.put(entry.getName(), bos.toByteArray());
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        } finally {
            zis.close();
        }
        return entries;
    }
}
