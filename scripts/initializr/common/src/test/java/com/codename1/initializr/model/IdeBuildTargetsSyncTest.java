package com.codename1.initializr.model;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Guards the build-target run configs across EVERY supported IDE bundle, not just
 * IntelliJ. Each IDE ships its run/build commands as a binary resource zip
 * (see {@link IDE}); historically a new build target was added to one surface
 * (e.g. IntelliJ's workspace.xml) and the others silently rotted.
 *
 * <p>For every IDE zip this test asserts the new Mac native targets are present
 * and the removed {@code windows-device}/UWP target is gone. It is intentionally
 * format-agnostic: Eclipse {@code .launch} files, NetBeans
 * {@code nb-configuration.xml} and the VS Code {@code settings.json} all embed the
 * literal {@code codename1.buildTarget} value, so a substring scan over the whole
 * bundle catches drift regardless of the per-IDE file format.
 *
 * <p>IntelliJ's idea.zip additionally has a stricter byte-for-byte guard against
 * the canonical plugin template in {@link IdeaWorkspaceSyncTest}.
 *
 * <p>To add a target: update the canonical IntelliJ workspace.xml AND regenerate
 * each IDE zip under {@code scripts/initializr/common/src/main/resources/}, then
 * add the target to {@link #REQUIRED_TARGETS} here.
 */
public class IdeBuildTargetsSyncTest extends AbstractTest {

    /** The IDE bundles to scan - mirrors {@link IDE}. */
    private static final String[] IDE_ZIPS = {
            "/idea.zip", "/eclipse.zip", "/netbeans.zip", "/vscode.zip"
    };

    /** Build targets every IDE bundle must expose. */
    private static final String[] REQUIRED_TARGETS = {
            "mac-os-x-native", // Mac Native Build (cloud)
            "mac-source"       // Mac Native Project (local Xcode)
    };

    /** Build targets that were removed and must not linger in any IDE bundle. */
    private static final String[] FORBIDDEN_TARGETS = {
            "windows-device" // UWP build, removed in #4624
    };

    @Override
    public boolean runTest() throws Exception {
        for (String zip : IDE_ZIPS) {
            String bundle = readAllEntriesAsText(zip);
            for (String target : REQUIRED_TARGETS) {
                assertTrue(bundle.contains(target),
                        zip + " is missing build target '" + target
                                + "'. Regenerate the IDE bundle under"
                                + " scripts/initializr/common/src/main/resources/ to include it.");
            }
            for (String target : FORBIDDEN_TARGETS) {
                assertFalse(bundle.contains(target),
                        zip + " still contains removed build target '" + target
                                + "'. Regenerate the IDE bundle under"
                                + " scripts/initializr/common/src/main/resources/ to drop it.");
            }
        }
        return true;
    }

    /** Concatenates the text of every (non-directory) entry in a classpath zip resource. */
    private String readAllEntriesAsText(String resourcePath) throws IOException {
        InputStream in = getClass().getResourceAsStream(resourcePath);
        assertNotNull(in, "Missing classpath resource " + resourcePath);
        StringBuilder sb = new StringBuilder();
        ZipInputStream zis = new ZipInputStream(in);
        try {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    Util.copyNoClose(zis, bos, 8192);
                    sb.append(new String(bos.toByteArray(), "UTF-8")).append('\n');
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        } finally {
            zis.close();
            in.close();
        }
        return sb.toString();
    }
}
