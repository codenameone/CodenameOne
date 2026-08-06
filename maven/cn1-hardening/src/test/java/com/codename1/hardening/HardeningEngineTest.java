/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.hardening;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** End-to-end pipeline test: demux, ProGuard rename, string encryption, repackage, mapping. */
public class HardeningEngineTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String SECRETS = "com/codename1/hardening/fixture/Secrets";
    private static final String HELPER = "com/codename1/hardening/fixture/Helper";
    // Deliberately includes NUL and high bytes to prove byte-for-byte resource preservation,
    // written explicitly so the source stays pure ASCII.
    private static final byte[] RES_BYTES = new byte[]{
            'C', 'N', '1', '-', 'B', 'L', 'O', 'B', 0x00, (byte) 0xFF, (byte) 0x80, 0x7F, 'z'};

    private File buildInputJar() throws Exception {
        File jar = tmp.newFile("app.jar");
        FileOutputStream fo = new FileOutputStream(jar);
        ZipOutputStream zos = new ZipOutputStream(fo);
        putClass(zos, SECRETS);
        putClass(zos, HELPER);
        zos.putNextEntry(new ZipEntry("theme.res"));
        zos.write(RES_BYTES);
        zos.closeEntry();
        zos.finish();
        fo.close();
        return jar;
    }

    private void putClass(ZipOutputStream zos, String internal) throws Exception {
        InputStream in = getClass().getResourceAsStream("/" + internal + ".class");
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) >= 0) {
            b.write(buf, 0, r);
        }
        in.close();
        zos.putNextEntry(new ZipEntry(internal + ".class"));
        zos.write(b.toByteArray());
        zos.closeEntry();
    }

    private HardeningResult harden(HardeningProfile profile, String platform, boolean renameSupported)
            throws Exception {
        File in = buildInputJar();
        File out = tmp.newFile("app-hardened.jar");
        File mapping = tmp.newFile("mapping.txt");
        File report = tmp.newFile("report.json");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", profile.name().toLowerCase());
        HardeningConfig cfg = HardeningConfig.from(hints, platform, renameSupported);
        HardeningRequest req = new HardeningRequest()
                .inputJar(in).outputJar(out).mappingFile(mapping).reportFile(report)
                .workDir(tmp.newFolder("work")).config(cfg)
                // Keep Secrets so the test can load it by name; Helper still gets renamed.
                .mainClass("com.codename1.hardening.fixture.Secrets")
                .buildKey("TESTKEY");
        return HardeningEngine.harden(req);
    }

    @Test
    public void standardHardenRenamesEncryptsAndPreservesResources() throws Exception {
        // ProGuard 7.3.2 can't read JDK 21+ class files; the renamer runs on JDK <=20 in production.
        org.junit.Assume.assumeTrue("ProGuard renamer needs JDK <=20", HardeningEngine.proguardCanRunHere());
        HardeningResult r = harden(HardeningProfile.STANDARD, "ios", true);
        assertTrue(r.isHardened());

        Map<String, byte[]> outEntries = readAll(r.getHardenedJar());

        // Non-class resource carried across byte-for-byte.
        assertArrayEquals(RES_BYTES, outEntries.get("theme.res"));

        // Helper (not kept) was renamed away; Secrets (kept as main) remains.
        assertFalse("Helper should have been renamed", outEntries.containsKey(HELPER + ".class"));
        assertTrue("kept main class should remain", outEntries.containsKey(SECRETS + ".class"));
        assertTrue("a zq-prefixed renamed class should exist", hasZqClass(outEntries.keySet()));

        // Mapping records the rename and Helper is present in it.
        String mapping = new String(Files.readAllBytes(r.getMappingFile().toPath()), Charset.forName("UTF-8"));
        assertTrue(mapping.contains("com.codename1.hardening.fixture.Helper ->"));
        assertTrue(mapping.contains("# mappingId:"));
        assertEquals(64, r.getMappingId().length());

        // Standard = constants mode: the static-final API constant is encrypted (including its
        // inlined read in api()); a plain method literal like the greeting is left alone.
        // Behaviour is intact when loaded either way.
        byte[] secrets = outEntries.get(SECRETS + ".class");
        assertFalse(StringEncryptTransform.containsStringLiteral(secrets,
                "https://api.example.com/secret-endpoint"));
        assertTrue(StringEncryptTransform.containsStringLiteral(secrets, "hello secret world"));

        URLClassLoader cl = new URLClassLoader(new URL[]{r.getHardenedJar().toURI().toURL()},
                getClass().getClassLoader().getParent());
        Class<?> c = Class.forName("com.codename1.hardening.fixture.Secrets", true, cl);
        assertEquals("hello secret world", c.getMethod("greet").invoke(null));
        assertEquals("https://api.example.com/secret-endpoint", c.getMethod("api").invoke(null));
        cl.close();
    }

    @Test
    public void serviceProviderClassesAreKept() throws Exception {
        org.junit.Assume.assumeTrue("ProGuard renamer needs JDK <=20", HardeningEngine.proguardCanRunHere());
        // Build a jar where Helper is declared as a service provider; it must survive un-renamed
        // so the verbatim-copied descriptor still resolves via ServiceLoader.
        File jar = tmp.newFile("svc.jar");
        FileOutputStream fo = new FileOutputStream(jar);
        ZipOutputStream zos = new ZipOutputStream(fo);
        putClass(zos, SECRETS);
        putClass(zos, HELPER);
        zos.putNextEntry(new ZipEntry("META-INF/services/com.example.MyService"));
        zos.write("# a provider\ncom.codename1.hardening.fixture.Helper\n"
                .getBytes(Charset.forName("UTF-8")));
        zos.closeEntry();
        zos.finish();
        fo.close();

        File out = tmp.newFile("svc-hardened.jar");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "standard");
        HardeningRequest req = new HardeningRequest()
                .inputJar(jar).outputJar(out).mappingFile(tmp.newFile("svc-map.txt"))
                .workDir(tmp.newFolder("svc-work"))
                .config(HardeningConfig.from(hints, "ios", true))
                .mainClass("com.codename1.hardening.fixture.Secrets");
        HardeningResult r = HardeningEngine.harden(req);
        assertTrue(r.isHardened());
        Map<String, byte[]> outEntries = readAll(out);
        assertTrue("service provider class must be kept, not renamed",
                outEntries.containsKey(HELPER + ".class"));
        assertArrayEquals("# a provider\ncom.codename1.hardening.fixture.Helper\n"
                        .getBytes(Charset.forName("UTF-8")),
                outEntries.get("META-INF/services/com.example.MyService"));
    }

    @Test
    public void offProfileIsSkippedAndReturnsInput() throws Exception {
        HardeningResult r = harden(HardeningProfile.OFF, "ios", true);
        assertFalse(r.isHardened());
        assertEquals(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, r.getOutcome());
    }

    @Test
    public void androidRenameOnlyIsHardenedViaR8() throws Exception {
        // Android (renameSupported=false), standard with strings off: the engine renames nothing,
        // but R8 will, so the build must be marked hardened rather than skipped.
        File in = buildInputJar();
        File out = tmp.newFile("and-hardened.jar");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "standard");
        hints.put("harden.strings", "off");
        HardeningRequest req = new HardeningRequest()
                .inputJar(in).outputJar(out).mappingFile(tmp.newFile("and-map.txt"))
                .workDir(tmp.newFolder("and-work"))
                .config(HardeningConfig.from(hints, "and", false))
                .mainClass("com.codename1.hardening.fixture.Secrets");
        HardeningResult r = HardeningEngine.harden(req);
        assertTrue("Android rename-only must be marked hardened (R8 renames)", r.isHardened());
        assertEquals(0, r.getRenamedClasses());
        assertTrue(r.getTransformsApplied().contains("rename:r8"));
    }

    @Test
    public void nonOffLevelWithAllTransformsDisabledIsSkipped() throws Exception {
        // standard, but rename off and strings off -> nothing to do -> not stamped hardened.
        File in = buildInputJar();
        File out = tmp.newFile("noop-hardened.jar");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "standard");
        hints.put("harden.rename", "false");
        hints.put("harden.strings", "off");
        HardeningRequest req = new HardeningRequest()
                .inputJar(in).outputJar(out).mappingFile(tmp.newFile("noop-map.txt"))
                .workDir(tmp.newFolder("noop-work"))
                .config(HardeningConfig.from(hints, "ios", true))
                .mainClass("com.codename1.hardening.fixture.Secrets");
        HardeningResult r = HardeningEngine.harden(req);
        assertFalse("a config that applies no transform must not be marked hardened", r.isHardened());
        assertEquals(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, r.getOutcome());
    }

    @Test
    public void androidDoesNotRenameButStillEncrypts() throws Exception {
        // renameSupported=false models Android, where R8 is the sole renamer.
        HardeningResult r = harden(HardeningProfile.STANDARD, "and", false);
        assertTrue(r.isHardened());
        Map<String, byte[]> outEntries = readAll(r.getHardenedJar());
        // Nothing renamed: both classes keep their names.
        assertTrue(outEntries.containsKey(HELPER + ".class"));
        assertTrue(outEntries.containsKey(SECRETS + ".class"));
        assertEquals(0, r.getRenamedClasses());
        // Standard = constants mode: the static-final API constant is encrypted (including its
        // inlined copy in api()), but a plain method literal like the greeting is left alone.
        assertTrue(r.getEncryptedStrings() >= 1);
        byte[] secrets = outEntries.get(SECRETS + ".class");
        assertFalse("declared constant must be encrypted",
                StringEncryptTransform.containsStringLiteral(secrets, "https://api.example.com/secret-endpoint"));
        assertTrue("a plain (non-constant) literal is left alone in constants mode",
                StringEncryptTransform.containsStringLiteral(secrets, "hello secret world"));
    }

    @Test
    public void javascriptSkipsStringEncryption() throws Exception {
        org.junit.Assume.assumeTrue("ProGuard renamer needs JDK <=20", HardeningEngine.proguardCanRunHere());
        HardeningResult r = harden(HardeningProfile.AGGRESSIVE, "javascript", true);
        assertTrue(r.isHardened());
        // On JS the bridge could break, so string encryption is off; renaming still happens.
        assertEquals(0, r.getEncryptedStrings());
        assertTrue(r.getRenamedClasses() >= 1);
    }

    private boolean hasZqClass(java.util.Set<String> names) {
        for (String n : names) {
            if (n.endsWith(".class") && n.substring(n.lastIndexOf('/') + 1).startsWith(Cn1NameFactory.PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, byte[]> readAll(File jar) throws Exception {
        Map<String, byte[]> out = new HashMap<String, byte[]>();
        ZipInputStream zis = new ZipInputStream(Files.newInputStream(jar.toPath()));
        ZipEntry e;
        while ((e = zis.getNextEntry()) != null) {
            if (e.isDirectory()) {
                continue;
            }
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = zis.read(buf)) >= 0) {
                b.write(buf, 0, r);
            }
            out.put(e.getName(), b.toByteArray());
        }
        zis.close();
        return out;
    }
}
