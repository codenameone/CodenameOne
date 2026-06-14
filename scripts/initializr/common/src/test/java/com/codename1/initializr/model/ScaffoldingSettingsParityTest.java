package com.codename1.initializr.model;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.util.StringUtil;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parity guard between the two project-scaffolding paths.
 *
 * A newly generated Codename One project must look the same regardless of whether it came
 * from the initializr (start.codenameone.com -- driven by {@link GeneratorModel} and the
 * embedded {@code common.zip}) or from the Maven archetype ({@code cn1app-archetype}). In
 * particular both must ship the SAME build-hint properties in
 * {@code common/codenameone_settings.properties}.
 *
 * <p>This is a regression test for a real drift: iOS/Android on-device-debug hints
 * ({@code ios.onDeviceDebug}, {@code ios.onDeviceDebug.proxyHost/proxyPort/waitForAttach},
 * {@code android.onDeviceDebug}) -- plus desktop title-bar, j2me, languageLevel and rim.*
 * keys -- were added to the archetype template but never to the initializr's
 * {@code common.zip}, so they were silently missing from every initializr project even
 * though they are documented in the developer guide.</p>
 *
 * <p>We generate a project through the real initializr generator, then compare the
 * <em>set of property/hint entries</em> (commented-out hint lines included) against the
 * archetype template, after canonicalising the per-project placeholders (app name,
 * package, java version) and ignoring comment prose / ordering. Any other difference is
 * real drift and fails the test.</p>
 *
 * <p>The companion CI gate is
 * {@code maven/integration-tests/scaffolding-settings-parity-test.sh}, which performs the
 * same comparison directly on the two committed template artifacts.</p>
 */
public class ScaffoldingSettingsParityTest extends AbstractTest {

    /** Repo-relative path of the archetype's settings template. */
    private static final String ARCHETYPE_REL_PATH =
            "maven/cn1app-archetype/src/main/resources/archetype-resources/common/codenameone_settings.properties";

    private static final String SETTINGS_ENTRY = "common/codenameone_settings.properties";

    @Override
    public boolean runTest() throws Exception {
        File archetypeFile = locateArchetypeSettings();
        if (archetypeFile == null) {
            // The initializr sources are also published/built standalone, outside the
            // CodenameOne monorepo, where the maven/ tree is absent. Don't fail there --
            // the integration-tests parity script is the authoritative CI cross-check.
            System.out.println("[WARN] ScaffoldingSettingsParityTest: could not locate " + ARCHETYPE_REL_PATH
                    + " above user.dir=" + System.getProperty("user.dir")
                    + " -- skipping (expected for standalone initializr builds).");
            return true;
        }

        String appName = "ParityGuardApp";
        String packageName = "com.acme.parityguard";

        // Generate through the REAL initializr generator (default options => Java 17 barebones).
        byte[] zip = createProjectZip(IDE.INTELLIJ, Template.BAREBONES, appName, packageName);
        Map<String, byte[]> entries = readZipEntries(zip);
        byte[] generated = entries.get(SETTINGS_ENTRY);
        assertNotNull(generated, "Generated initializr project must contain " + SETTINGS_ENTRY);

        Set<String> initializrEntries = parseSettings(StringUtil.newString(generated), appName, packageName);
        Set<String> archetypeEntries = parseSettings(readFile(archetypeFile), "${mainName}", "${package}");

        assertFalse(initializrEntries.isEmpty(), "No codename1.* settings parsed from the generated initializr project");
        assertFalse(archetypeEntries.isEmpty(), "No codename1.* settings parsed from the archetype template");

        List<String> missing = difference(archetypeEntries, initializrEntries); // in archetype, not initializr
        List<String> extra = difference(initializrEntries, archetypeEntries);    // in initializr, not archetype

        if (missing.isEmpty() && extra.isEmpty()) {
            return true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Default initializr and default Maven scaffolding must produce identical ");
        sb.append("codenameone_settings.properties, but their build-hint sets differ.\n");
        if (!missing.isEmpty()) {
            sb.append("\nPresent in the Maven archetype but MISSING from the initializr common.zip:\n");
            for (int i = 0; i < missing.size(); i++) {
                sb.append("  - ").append(missing.get(i)).append('\n');
            }
            sb.append("  -> add these to ").append(SETTINGS_ENTRY).append(" inside\n");
            sb.append("     scripts/initializr/common/src/main/resources/common.zip\n");
        }
        if (!extra.isEmpty()) {
            sb.append("\nPresent in the initializr common.zip but MISSING from the Maven archetype:\n");
            for (int i = 0; i < extra.size(); i++) {
                sb.append("  + ").append(extra.get(i)).append('\n');
            }
            sb.append("  -> add these to ").append(ARCHETYPE_REL_PATH).append('\n');
        }
        fail(sb.toString());
        return false;
    }

    /**
     * Parse a codenameone_settings.properties body into a canonical set of entry strings.
     * Each entry is {@code [#]key=value}; the leading {@code #} marks a commented-out hint
     * (preserved so we catch a hint dropped from one scaffold). Prose comments and Velocity
     * {@code #set(...)} directives are ignored. The per-project {@code appToken}/{@code pkgToken}
     * and the java version value are canonicalised so the two scaffolds line up.
     */
    private Set<String> parseSettings(String text, String appToken, String pkgToken) {
        Set<String> result = new HashSet<String>();
        List<String> lines = StringUtil.tokenize(text, "\n");
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.length() == 0 || line.startsWith("#set(")) {
                continue;
            }
            boolean commented = false;
            String body = line;
            if (body.startsWith("#")) {
                commented = true;
                body = body.substring(1).trim();
            }
            if (!body.startsWith("codename1.")) {
                continue; // prose comment or unrelated line
            }
            int eq = body.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = body.substring(0, eq).trim();
            if (key.indexOf(' ') >= 0 || key.indexOf('\t') >= 0) {
                continue;
            }
            String value = body.substring(eq + 1).trim();
            value = canonicalizeValue(key, value, appToken, pkgToken);
            result.add((commented ? "#" : "") + key + "=" + value);
        }
        return result;
    }

    private String canonicalizeValue(String key, String value, String appToken, String pkgToken) {
        // The java language level is a per-project choice (archetype resolves ${javaVersion}
        // at generation time; initializr bakes a concrete default), not a scaffold difference.
        if (key.equals("codename1.arg.java.version")) {
            return "<JAVAVER>";
        }
        value = StringUtil.replaceAll(value, pkgToken, "<PKG>");
        value = StringUtil.replaceAll(value, appToken, "<APP>");
        return value;
    }

    private List<String> difference(Set<String> a, Set<String> b) {
        List<String> out = new ArrayList<String>();
        for (String entry : a) {
            if (!b.contains(entry)) {
                out.add(entry);
            }
        }
        Collections.sort(out);
        return out;
    }

    private File locateArchetypeSettings() {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int i = 0; i < 12 && dir != null; i++) {
            File candidate = new File(dir, ARCHETYPE_REL_PATH);
            if (candidate.isFile()) {
                return candidate;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    private String readFile(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Util.copyNoClose(in, bos, 8192);
            return StringUtil.newString(bos.toByteArray());
        } finally {
            in.close();
        }
    }

    private static byte[] createProjectZip(IDE ide, Template template, String appName, String packageName) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GeneratorModel.create(ide, template, appName, packageName).writeProjectZip(output);
        return output.toByteArray();
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
