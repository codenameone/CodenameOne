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
package com.codename1.builders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Local JavaScript builder backed by ParparVM's bytecode-to-JS translator.
 *
 * Status: Enterprise-tier preview. The build server still owns the canonical
 * TeaVM-based pipeline (see BuildDaemon's JavascriptBuilder). This builder
 * is the local-machine counterpart for the new ParparVM JS port and is
 * deliberately undocumented while we iterate on parity.
 */
public class JavaScriptBuilder extends Executor {

    // Mirrors the CodenameOneBuildDaemon user-rank tiers:
    //   <  9000  trial
    //   >= 9000  free
    //   >= 11000 pro
    //   >= 12000 enterprise
    //   >= 13000 large-enterprise
    // Local JS builds gate at the enterprise threshold.
    private static final int ENTERPRISE_THRESHOLD = 12000;

    private File jsDistDir;
    private File jsOutputZip;

    @Override
    protected String getDeviceIdCode() {
        return "\"\"";
    }

    @Override
    protected String generatePeerComponentCreationCode(String methodCallString) {
        return "PeerComponent.create(" + methodCallString + ")";
    }

    @Override
    protected String convertPeerComponentToNative(String param) {
        return "(java.awt.Component)" + param + ".getNativePeer()";
    }

    public File getJavaScriptDistDir() {
        return jsDistDir;
    }

    public File getJavaScriptOutputZip() {
        return jsOutputZip;
    }

    @Override
    public boolean build(File sourceZip, BuildRequest request) throws BuildException {
        if (!checkUserLevel(request)) {
            return false;
        }

        debug("Request Args: ");
        debug("-----------------");
        for (String arg : request.getArgs()) {
            debug(arg + "=" + request.getArg(arg, null));
        }
        debug("-------------------");

        try {
            File buildDir = getBuildDirectory();
            if (buildDir == null) {
                buildDir = createTmpDir();
            } else {
                buildDir.mkdirs();
            }
            tmpDir = buildDir;

            File stageClasses = new File(buildDir, "stage-classes");
            File portClasses = new File(buildDir, "port-classes");
            File translatorOut = new File(buildDir, "translator-output");
            stageClasses.mkdirs();
            portClasses.mkdirs();
            translatorOut.mkdirs();

            // Order matches the script-based JS port pipeline:
            //   1. user app (codenameone-core + java-runtime + user classes via the local-javascript jar-with-deps)
            //   2. parparvm-java-api last, so it overrides any stale java.* / com.codename1.impl.* stubs
            stageSourceJar(sourceZip, stageClasses);
            stageJavaApi(stageClasses);

            File portSources = locateJavaScriptPortSources(request);
            File portClassesStaged = stageJavaScriptPort(request, portSources, stageClasses, portClasses);

            String translatorAppName = sanitizeIdentifier(request.getMainClass()) + "JavaScriptMain";
            File launcherJava = writeLauncher(buildDir, translatorAppName, request.getPackageName(), request.getMainClass());
            compileLauncher(launcherJava, stageClasses, portClassesStaged);

            File parparvmCompilerJar = extractParparVMCompiler();

            if (!runByteCodeTranslator(parparvmCompilerJar, stageClasses, translatorOut, translatorAppName,
                    request.getPackageName(), request.getMainClass(), request.getVersion())) {
                return false;
            }

            File distDir = locateDistDir(translatorOut, translatorAppName);
            if (distDir == null) {
                error("Translator did not produce a JS bundle under " + translatorOut, null);
                return false;
            }
            mergeTranslatorRootResources(translatorOut, distDir);

            File finalDist = new File(translatorOut, "dist" + File.separator + request.getMainClass() + "-js");
            if (!distDir.equals(finalDist)) {
                if (finalDist.exists()) {
                    delTree(finalDist, true);
                }
                if (!distDir.renameTo(finalDist)) {
                    copyTree(distDir, finalDist);
                }
                distDir = finalDist;
            }
            jsDistDir = distDir;

            jsOutputZip = new File(buildDir, request.getMainClass() + "-js.zip");
            zipDirectory(distDir, jsOutputZip, distDir.getName());
            log("Wrote browser bundle to " + jsOutputZip);
            return true;
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            error("JavaScript build failed", ex);
            throw new BuildException("JavaScript build failed: " + ex.getMessage(), ex);
        }
    }

    private boolean checkUserLevel(BuildRequest request) {
        String raw = firstNonEmpty(
                request.getArg("javascript.userLevel", null),
                request.getArg("userLevel", null),
                request.getArg("user.level", null),
                System.getProperty("codename1.userLevel"),
                System.getenv("CN1_USER_LEVEL"));
        int rank = parseUserRank(raw);
        log("Local JavaScript builder: user-level=" + (raw == null ? "<unset>" : raw)
                + " (rank=" + rank + ", required>=" + ENTERPRISE_THRESHOLD + ")");
        if (rank >= ENTERPRISE_THRESHOLD) {
            return true;
        }
        log("ERROR: The local JavaScript build is licensed only to Enterprise and higher tier users. "
                + "Set codename1.arg.javascript.userLevel=Enterprise (or a higher tier) in codenameone_settings.properties, "
                + "or define the CN1_USER_LEVEL environment variable, to enable this preview. "
                + "See https://www.codenameone.com/pricing.html for tier details.");
        return false;
    }

    private static int parseUserRank(String raw) {
        if (raw == null) {
            return 0;
        }
        String s = raw.trim().toLowerCase();
        if (s.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignore) {
            // Symbolic tier names. The naming mirrors what the build server uses internally.
        }
        if (s.equals("trial")) return 1000;
        if (s.equals("free") || s.equals("basic")) return 9000;
        if (s.equals("pro") || s.equals("professional")) return 11000;
        if (s.equals("enterprise")) return 12000;
        if (s.equals("midsizeenterprise") || s.equals("midsize") || s.equals("midsize-enterprise")) return 13000;
        if (s.equals("bigcorp") || s.equals("big-corp") || s.equals("large") || s.equals("largeenterprise")) return 14000;
        return 0;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && v.trim().length() > 0) {
                return v;
            }
        }
        return null;
    }

    private void stageJavaApi(File stageClasses) throws IOException {
        InputStream is = getResourceAsStream("/parparvm-java-api.jar");
        if (is == null) {
            throw new IOException("parparvm-java-api.jar resource is missing from the plugin classpath");
        }
        try {
            unzip(is, stageClasses, stageClasses, stageClasses);
        } finally {
            is.close();
        }
    }

    private void stageSourceJar(File sourceZip, File stageClasses) throws IOException {
        if (sourceZip == null || !sourceZip.isFile()) {
            throw new IOException("Application source jar is missing: " + sourceZip);
        }
        unzip(sourceZip, stageClasses, stageClasses, stageClasses);
    }

    private File locateJavaScriptPortSources(BuildRequest request) {
        String explicit = request.getArg("javascript.portSources", null);
        if (explicit != null && explicit.trim().length() > 0) {
            File f = new File(explicit);
            if (f.isDirectory()) return f;
            log("javascript.portSources is set to " + explicit + " but the directory does not exist; falling back to auto-detection");
        }
        // Walk up the build directory and the current working directory looking for
        // a checked-out cn1 repo. The JS port lives at Ports/JavaScriptPort/src/main/java.
        List<File> roots = new ArrayList<File>();
        if (getBuildDirectory() != null) roots.add(getBuildDirectory());
        roots.add(new File(System.getProperty("user.dir")));
        for (File root : roots) {
            File hit = walkForPortSources(root);
            if (hit != null) return hit;
        }
        return null;
    }

    private static File walkForPortSources(File start) {
        File cur = start;
        for (int i = 0; cur != null && i < 12; i++) {
            File candidate = new File(cur, "Ports" + File.separator + "JavaScriptPort"
                    + File.separator + "src" + File.separator + "main" + File.separator + "java");
            if (candidate.isDirectory()) return candidate;
            cur = cur.getParentFile();
        }
        return null;
    }

    private File stageJavaScriptPort(BuildRequest request, File portSources, File stageClasses, File portClasses)
            throws Exception {
        // Prefer a pre-built JavaScriptPort.jar bundled as a plugin resource.
        InputStream bundled = getResourceAsStream("/JavaScriptPort.jar");
        if (bundled != null) {
            try {
                File jar = File.createTempFile("JavaScriptPort", ".jar");
                jar.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(jar);
                try {
                    copy(bundled, fos);
                } finally {
                    fos.close();
                }
                unzip(jar, stageClasses, stageClasses, stageClasses);
                return stageClasses;
            } finally {
                bundled.close();
            }
        }
        if (portSources == null) {
            throw new BuildException("Cannot locate JavaScript port sources. "
                    + "Either run the build from a Codename One source checkout (so Ports/JavaScriptPort is reachable), "
                    + "or set the javascript.portSources build hint to the absolute path of "
                    + "Ports/JavaScriptPort/src/main/java");
        }
        log("Compiling JavaScript port sources from " + portSources);
        List<File> javaFiles = new ArrayList<File>();
        collectJavaFiles(portSources, javaFiles);
        if (javaFiles.isEmpty()) {
            throw new BuildException("No .java files found under " + portSources);
        }
        File sourceList = new File(tmpDir, "javascript-port-sources.txt");
        PrintWriter sw = new PrintWriter(new FileWriter(sourceList));
        try {
            for (File f : javaFiles) {
                String name = f.getName();
                if ("Stub.java".equals(name)) continue;
                sw.println(f.getAbsolutePath());
            }
        } finally {
            sw.close();
        }
        portClasses.mkdirs();
        String javac = resolveJavac();
        boolean ok = exec(tmpDir, -1, javac, "-source", "8", "-target", "8",
                "-cp", stageClasses.getAbsolutePath(),
                "-d", portClasses.getAbsolutePath(),
                "@" + sourceList.getAbsolutePath());
        if (!ok) {
            throw new BuildException("Failed to compile JavaScript port sources");
        }
        copyTree(portClasses, stageClasses);
        return stageClasses;
    }

    private static void collectJavaFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                collectJavaFiles(f, out);
            } else if (f.getName().endsWith(".java")) {
                out.add(f);
            }
        }
    }

    private String resolveJavac() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            File j = new File(javaHome, "bin" + File.separator + (is_windows ? "javac.exe" : "javac"));
            if (j.canExecute()) return j.getAbsolutePath();
            // JDK is sometimes at java.home/.. on older JREs
            File j2 = new File(new File(javaHome).getParentFile(), "bin" + File.separator + (is_windows ? "javac.exe" : "javac"));
            if (j2.canExecute()) return j2.getAbsolutePath();
        }
        return "javac";
    }

    private File writeLauncher(File workDir, String launcherName, String packageName, String mainClass) throws IOException {
        File f = new File(workDir, launcherName + ".java");
        PrintWriter pw = new PrintWriter(new FileWriter(f));
        try {
            pw.println("import com.codename1.impl.html5.ParparVMBootstrap;");
            pw.println("import " + packageName + "." + mainClass + ";");
            pw.println();
            pw.println("public final class " + launcherName + " {");
            pw.println("    public static void main(String[] args) {");
            pw.println("        ParparVMBootstrap.bootstrap(new " + mainClass + "());");
            pw.println("    }");
            pw.println("}");
        } finally {
            pw.close();
        }
        return f;
    }

    private void compileLauncher(File launcherJava, File stageClasses, File portClasses) throws Exception {
        String javac = resolveJavac();
        boolean ok = exec(tmpDir, -1, javac, "-source", "8", "-target", "8",
                "-cp", stageClasses.getAbsolutePath() + File.pathSeparator + portClasses.getAbsolutePath(),
                "-d", stageClasses.getAbsolutePath(),
                launcherJava.getAbsolutePath());
        if (!ok) {
            throw new BuildException("Failed to compile JavaScript launcher class");
        }
    }

    private File extractParparVMCompiler() throws BuildException {
        try {
            return getResourceAsFile("/parparvm-compiler.jar", ".jar");
        } catch (IOException ex) {
            throw new BuildException("Failed to extract parparvm-compiler.jar", ex);
        }
    }

    private boolean runByteCodeTranslator(File compilerJar, File stageClasses, File translatorOut,
                                          String translatorAppName, String packageName, String mainClass,
                                          String version) throws Exception {
        Map<String, String> env = new HashMap<String, String>();
        log("Running ByteCodeTranslator (javascript target) for " + mainClass);
        return exec(tmpDir, env, -1,
                "java", "-Xmx512m", "-cp", compilerJar.getAbsolutePath(),
                "com.codename1.tools.translator.ByteCodeTranslator",
                "javascript",
                stageClasses.getAbsolutePath(),
                translatorOut.getAbsolutePath(),
                translatorAppName,
                packageName,
                mainClass,
                version == null ? "1.0" : version,
                "ios",
                "none");
    }

    private File locateDistDir(File translatorOut, String translatorAppName) {
        File primary = new File(translatorOut, "dist" + File.separator + translatorAppName + "-js");
        if (primary.isDirectory()) return primary;
        File distRoot = new File(translatorOut, "dist");
        if (!distRoot.isDirectory()) return null;
        File[] children = distRoot.listFiles();
        if (children == null) return null;
        for (File c : children) {
            if (c.isDirectory() && new File(c, "worker.js").isFile()) {
                return c;
            }
        }
        return null;
    }

    private void mergeTranslatorRootResources(File translatorOut, File distDir) throws IOException {
        File[] children = translatorOut.listFiles();
        if (children == null) return;
        for (File c : children) {
            if (c.getName().equals("dist")) continue;
            File target = new File(distDir, c.getName());
            if (c.isDirectory()) {
                copyTree(c, target);
            } else {
                Files.copy(c.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        File assetsDir = new File(distDir, "assets");
        assetsDir.mkdirs();
        File md = new File(distDir, "material-design-font.ttf");
        if (md.isFile() && !new File(assetsDir, md.getName()).isFile()) {
            Files.move(md.toPath(), new File(assetsDir, md.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyTree(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            if (!dst.exists() && !dst.mkdirs() && !dst.isDirectory()) {
                throw new IOException("Failed to create " + dst);
            }
            String[] names = src.list();
            if (names == null) return;
            for (String name : names) {
                copyTree(new File(src, name), new File(dst, name));
            }
        } else {
            File parent = dst.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void zipDirectory(File sourceDir, File outZip, String rootEntryName) throws IOException {
        FileOutputStream fos = new FileOutputStream(outZip);
        try {
            ZipOutputStream zos = new ZipOutputStream(fos);
            try {
                Path base = sourceDir.toPath();
                walkAndZip(sourceDir, base, rootEntryName, zos);
            } finally {
                zos.close();
            }
        } finally {
            fos.close();
        }
    }

    private static void walkAndZip(File current, Path base, String rootEntryName, ZipOutputStream zos) throws IOException {
        if (current.isDirectory()) {
            File[] children = current.listFiles();
            if (children == null) return;
            for (File c : children) {
                walkAndZip(c, base, rootEntryName, zos);
            }
            return;
        }
        String rel = base.relativize(current.toPath()).toString().replace(File.separatorChar, '/');
        String entryName = rootEntryName + "/" + rel;
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        FileInputStream fis = new FileInputStream(current);
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) > 0) {
                zos.write(buf, 0, n);
            }
        } finally {
            fis.close();
        }
        zos.closeEntry();
    }

    private static String sanitizeIdentifier(String s) {
        if (s == null || s.isEmpty()) return "AppMain";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isJavaIdentifierPart(ch)) sb.append(ch); else sb.append('_');
        }
        if (sb.length() > 0 && !Character.isJavaIdentifierStart(sb.charAt(0))) {
            sb.insert(0, '_');
        }
        return sb.toString();
    }
}
