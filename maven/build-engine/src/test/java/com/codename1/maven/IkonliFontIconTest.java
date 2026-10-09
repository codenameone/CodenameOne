/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.maven;

import com.codename1.build.BuildArtifact;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// A real library that finds its parts with `ServiceLoader`: Ikonli's Swing
/// icons, with the Font Awesome pack, the way an application depends on
/// them.
///
/// `FontIcon.of(FontAwesomeSolid.CODE_BRANCH, ...)` asks a resolver for the
/// handler of that icon -- the handlers are service providers the pack lists
/// in `META-INF/services`, named by no class -- and the handler for its
/// font, a file under the pack's `META-INF/resources`. Here an application
/// that does exactly that is compiled, remapped with the three jars bundled,
/// held to the compliance check, and run with nothing on its class path but
/// the remapped classes, the headless implementation and the framework: the
/// icon has to come out with a size, which it has only if the handler and
/// the font were both found through what the build shipped.
///
/// The jars are compiled for a newer JVM than the one this module's tests
/// may run on, so the fixture is compiled and run by the JDK `JAVA17_HOME`
/// (or `JAVA21_HOME`) names; the test is skipped where there is none.
public class IkonliFontIconTest {

    private static final String MAIN = "package com.acme.icons;\n"
            + "import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;\n"
            + "import org.kordamp.ikonli.swing.FontIcon;\n"
            + "public class Main {\n"
            + "    public static void main(String[] args) {\n"
            + "        new javax.swing.JLabel(\"Branch\", FontIcon.of(FontAwesomeSolid.CODE_BRANCH, 16,\n"
            + "                java.awt.Color.RED), javax.swing.JLabel.LEFT);\n"
            + "    }\n"
            + "    public static String probe() {\n"
            + "        FontIcon icon = FontIcon.of(FontAwesomeSolid.CODE_BRANCH, 16, java.awt.Color.RED);\n"
            + "        return icon.getIconWidth() + \"x\" + icon.getIconHeight() + \" \" + icon.getIkon().getDescription();\n"
            + "    }\n"
            + "}\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    private final List<String> logged = new ArrayList<String>();

    private final com.codename1.build.Log log = new com.codename1.build.Log() {
        public void debug(CharSequence c) { }
        public void debug(CharSequence c, Throwable e) { }
        public void debug(Throwable e) { }
        public void info(CharSequence c) { }
        public void info(CharSequence c, Throwable e) { }
        public void info(Throwable e) { }
        public void warn(CharSequence c) { logged.add("warn: " + c); }
        public void warn(CharSequence c, Throwable e) { }
        public void warn(Throwable e) { }
        public void error(CharSequence c) { }
        public void error(CharSequence c, Throwable e) { }
        public void error(Throwable e) { }
        public boolean isDebugEnabled() { return false; }
        public boolean isInfoEnabled() { return true; }
        public boolean isWarnEnabled() { return true; }
        public boolean isErrorEnabled() { return true; }
    };

    private static File jdk() {
        String v = System.getProperty("java.specification.version");
        if (!v.startsWith("1.") && Integer.parseInt(v) >= 11) {
            return new File(System.getProperty("java.home"));
        }
        for (String name : new String[] {"JAVA17_HOME", "JAVA21_HOME", "JAVA25_HOME"}) {
            String home = System.getenv(name);
            if (home != null && new File(home, "bin/javac").isFile()) {
                return new File(home);
            }
        }
        return null;
    }

    private static String path(List<File> files) {
        StringBuilder cp = new StringBuilder();
        for (File f : files) {
            cp.append(cp.length() == 0 ? "" : File.pathSeparator).append(f.getAbsolutePath());
        }
        return cp.toString();
    }

    private static String[] fork(String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        // A fork that never ends is a failure of this test, not of the build.
        Thread watchdog = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(120000L);
                    p.destroyForcibly();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "ikonli-probe-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = p.getInputStream();
            byte[] buffer = new byte[8192];
            for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
                out.write(buffer, 0, n);
            }
            return new String[] {String.valueOf(p.waitFor()), new String(out.toByteArray(), "UTF-8")};
        } finally {
            watchdog.interrupt();
        }
    }

    @Test
    public void anIconIsResolvedThroughTheShippedProvidersAndFont() throws Exception {
        File jdk = jdk();
        Assume.assumeTrue("needs a JDK 11 or newer (JAVA17_HOME)", jdk != null);
        File scratch = tmp.newFolder("jars");
        List<File> ikonli = Arrays.asList(RealCompatJars.jar("ikonli-swing", scratch),
                RealCompatJars.jar("ikonli-core", scratch), RealCompatJars.jar("ikonli-fontawesome5-pack", scratch));
        List<File> runtimes = Arrays.asList(RealCompatJars.swing(scratch), RealCompatJars.javafx(scratch),
                RealCompatJars.jdk(scratch), RealCompatJars.core(scratch));

        // javac, as the application's build runs it: against the JDK's
        // Swing and the three jars.
        File src = new File(tmp.newFolder(), "com/acme/icons/Main.java");
        assertTrue(src.getParentFile().mkdirs());
        Files.write(src.toPath(), MAIN.getBytes("UTF-8"));
        File classes = tmp.newFolder("classes");
        String[] compiled = fork(new File(jdk, "bin/javac").getAbsolutePath(), "--release", "11", "-nowarn", "-g",
                "-cp", path(ikonli), "-d", classes.getAbsolutePath(), src.getAbsolutePath());
        assertEquals(compiled[1], "0", compiled[0]);

        List<File> cp = new ArrayList<File>(runtimes);
        cp.addAll(ikonli);
        File record = DesktopSources.entryRecord(tmp.newFolder());
        Files.write(record.toPath(), "mainClass=com.acme.icons.Main\nkind=swing\n".getBytes("UTF-8"));
        assertTrue(new CompatRemapper(classes, cp, null, log).withDesktopEntryRecord(record)
                .withApplicationMain("com.acme.MyApp").withResourceDirectories(Collections.<File>emptyList())
                .withApplicationLibraries(ikonli).run());

        // The handlers are reached through the services file alone, and the
        // font is where the runtime looks for it.
        assertTrue(new File(classes, "org/kordamp/ikonli/fontawesome5/FontAwesomeSolidIkonHandler.class").isFile());
        assertTrue(new File(classes, "META-INF__resources__fontawesome5__5.15.3__fonts__fa-solid-900.ttf").isFile());
        assertFalse(new File(classes, "META-INF/resources").exists());
        assertFalse(new File(classes, "META-INF/services").exists());
        assertTrue("Every provider the packs list can be created: " + logged, logged.isEmpty());

        // Throws on a single finding, in the application or the three jars.
        TestProjectHost host = RealCompatJars.host(classes, tmp.newFolder(), scratch);
        for (File jar : ikonli) {
            host.artifacts.add(new BuildArtifact("org.kordamp.ikonli", jar.getName().replace(".jar", ""), "1", null,
                    "jar", "compile", jar, null));
        }
        new BytecodeCompliance(host).execute();

        // Run: the probe's own class, then nothing but what a device has.
        File probe = new File(RemappedProbeMain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String[] ran = fork(new File(jdk, "bin/java").getAbsolutePath(), "-Djava.awt.headless=true", "-cp",
                path(Arrays.asList(classes, RealCompatJars.jar("codenameone-compat-testing", scratch),
                        RealCompatJars.core(scratch), probe)),
                RemappedProbeMain.class.getName(), "com.acme.icons.Main", "probe");
        assertEquals(ran[1], "0", ran[0]);
        String result = null;
        for (String line : ran[1].split("\n")) {
            if (line.startsWith("RESULT:")) {
                result = line.substring("RESULT:".length()).trim();
            }
        }
        assertTrue(ran[1], result != null && result.endsWith(" fas-code-branch"));
        String size = result.substring(0, result.indexOf(' '));
        int width = Integer.parseInt(size.substring(0, size.indexOf('x')));
        int height = Integer.parseInt(size.substring(size.indexOf('x') + 1));
        assertTrue("The icon has the size of its glyph: " + result, width > 0 && height > 0);
    }
}
