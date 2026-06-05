package com.codename1.initializr.model;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Guards against the recurring drift between the canonical IntelliJ run-config
 * template (maintained by the Codename One Maven plugin and installed by the
 * {@code cn1:configure-intellij} goal) and the {@code idea.zip} blob that the
 * initializr unpacks into every generated project's {@code .idea/} directory.
 *
 * <p>The run configs live in two places that have to stay in lock-step:
 * <ul>
 *   <li>the canonical template
 *       {@code maven/codenameone-maven-plugin/src/main/resources/com/codename1/maven/intellij/workspace.xml}, and</li>
 *   <li>{@code scripts/initializr/common/src/main/resources/idea.zip} (the one
 *       shipped to users) at entry {@code .idea/workspace.xml}.</li>
 * </ul>
 *
 * <p>Historically these drifted silently: a new build target (e.g. the Mac
 * native cloud/local targets) would be added to the canonical template but the
 * binary {@code idea.zip} was never regenerated, so a freshly downloaded project
 * was missing the targets and still carried long-removed ones (e.g.
 * {@code windows-device}/UWP). This test fails the build when that happens.
 *
 * <p>To regenerate after intentionally changing the canonical template:
 * <pre>
 * CANON=maven/codenameone-maven-plugin/src/main/resources/com/codename1/maven/intellij/workspace.xml
 * TMP=$(mktemp -d); mkdir -p "$TMP/.idea"; cp "$CANON" "$TMP/.idea/workspace.xml"
 * (cd "$TMP" &amp;&amp; zip -q -X "$OLDPWD/scripts/initializr/common/src/main/resources/idea.zip" .idea/workspace.xml)
 * </pre>
 */
public class IdeaWorkspaceSyncTest extends AbstractTest {

    private static final String WORKSPACE_ENTRY = ".idea/workspace.xml";

    private static final String CANONICAL_RELATIVE_PATH =
            "maven/codenameone-maven-plugin/src/main/resources/com/codename1/maven/intellij/workspace.xml";

    /** Targets that MUST be present in a freshly generated IntelliJ project. */
    private static final String[] REQUIRED_TARGETS = {
            "mac-os-x-native", // Mac Native Build (cloud)
            "mac-source",      // Mac Native Project (local Xcode)
            "ios-source",
            "ios-device",
            "android-device",
            "android-source",
            "javascript",
            "mac-os-x-desktop",
            "windows-desktop"
    };

    /** Targets that have been removed and must NOT reappear in generated projects. */
    private static final String[] FORBIDDEN_TARGETS = {
            "windows-device" // UWP build, removed in #4624
    };

    @Override
    public boolean runTest() throws Exception {
        // Read the .idea/workspace.xml exactly as it is shipped inside the bundled
        // idea.zip resource - i.e. BEFORE GeneratorModel runs its per-project
        // placeholder substitution (app name, package, cn1 version tags). That raw
        // template is the artifact maintainers edit-and-forget, so it is what we
        // guard against drift.
        Map<String, byte[]> entries = readZipEntries(readResource("/idea.zip"));
        byte[] shippedBytes = entries.get(WORKSPACE_ENTRY);
        assertNotNull(shippedBytes,
                "idea.zip is missing " + WORKSPACE_ENTRY
                        + " (the bundled IntelliJ run configs are broken)");
        String shipped = normalize(new String(shippedBytes, "UTF-8"));

        // 1) Invariant checks that encode the contract regardless of whether the
        //    canonical template can be located (e.g. isolated/standalone builds).
        for (String target : REQUIRED_TARGETS) {
            assertTrue(shipped.contains("\"" + target + "\""),
                    "idea.zip's .idea/workspace.xml is missing build target '" + target
                            + "'. Regenerate scripts/initializr/common/src/main/resources/idea.zip"
                            + " from the canonical workspace.xml (see this test's javadoc).");
        }
        for (String target : FORBIDDEN_TARGETS) {
            assertFalse(shipped.contains("\"" + target + "\""),
                    "idea.zip's .idea/workspace.xml still contains removed build target '" + target
                            + "'. Regenerate scripts/initializr/common/src/main/resources/idea.zip"
                            + " from the canonical workspace.xml (see this test's javadoc).");
        }

        // 2) Strongest guard: when the canonical template is on disk (full repo
        //    checkout, which is the case in CI), require byte-for-byte equality so
        //    ANY future drift - not just the targets we hard-code above - is caught.
        //    cn1:configure-intellij copies this exact file verbatim into a project,
        //    so the raw idea.zip entry must match it byte-for-byte.
        File canonical = locateCanonicalTemplate();
        if (canonical != null) {
            String expected = normalize(readFile(canonical));
            assertEqual(expected, shipped,
                    "The IntelliJ run configs in idea.zip have drifted from the canonical template "
                            + canonical.getPath()
                            + ". Regenerate scripts/initializr/common/src/main/resources/idea.zip"
                            + " from that file (see this test's javadoc for the exact command).");
        } else {
            log("Canonical workspace.xml not found on disk; skipping byte-for-byte comparison "
                    + "(invariant target checks still ran).");
        }
        return true;
    }

    private byte[] readResource(String path) throws IOException {
        java.io.InputStream in = getClass().getResourceAsStream(path);
        assertNotNull(in, "Missing classpath resource " + path);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Util.copyNoClose(in, bos, 8192);
            return bos.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Walks up from the test working directory looking for the canonical run-config
     * template in the maven plugin module. Returns {@code null} when it cannot be
     * found (e.g. the initializr is built outside a full repo checkout).
     */
    private static File locateCanonicalTemplate() {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (dir != null) {
            File candidate = new File(dir, CANONICAL_RELATIVE_PATH);
            if (candidate.isFile()) {
                return candidate;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    private static String readFile(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Util.copyNoClose(in, bos, 8192);
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /** Normalize line endings so a CRLF/LF difference doesn't masquerade as drift. */
    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static Map<String, byte[]> readZipEntries(byte[] zipData) throws IOException {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        ByteArrayInputStream input = new ByteArrayInputStream(zipData);
        ZipInputStream zis = new ZipInputStream(input);
        try {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    Util.copyNoClose(zis, bos, 8192);
                    entries.put(entry.getName(), bos.toByteArray());
                    bos.close();
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        } finally {
            zis.close();
            input.close();
        }
        return entries;
    }
}
