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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
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
 * This is the default ParparVM-backed JavaScript pipeline used by local and
 * cloud builds.
 */
public class JavaScriptBuilder extends Executor {

    private File jsDistDir;
    private File jsOutputZip;
    private File jsDeployableArtifact;
    // Directory holding the JavaScript-port web assets (port.js, js/, style.css,
    // ...) for this build. Handed to the translator via the
    // -Dcodename1.javascriptport.webapp override so it bundles port.js (the
    // worker-side native bindings) even when the build runs outside a CN1
    // source checkout. Set by stageJavaScriptPort.
    private File jsPortWebApp;

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

    public File getJavaScriptDeployableArtifact() {
        return jsDeployableArtifact;
    }

    @Override
    public boolean build(File sourceZip, BuildRequest request) throws BuildException {
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

            // For every NativeInterface in the app, generate a <Interface>Impl whose
            // methods bridge to the developer's JS stub on the MAIN thread (via
            // NativeInterfaceBridge -> browser_bridge.js -> cn1_native_interfaces). The
            // launcher registers each impl with NativeLookup so create() resolves and the
            // optimizer keeps the impl (it is otherwise only reached reflectively).
            List<Class<?>> nativeInterfaces = findNativeInterfaces(stageClasses);
            List<File> generatedImpls = generateNativeInterfaceImpls(buildDir, nativeInterfaces);

            String translatorAppName = sanitizeIdentifier(request.getMainClass()) + "JavaScriptMain";
            File launcherJava = writeLauncher(buildDir, translatorAppName, request.getPackageName(), request.getMainClass(), stageClasses, nativeInterfaces);
            compileLauncher(launcherJava, generatedImpls, stageClasses, portClassesStaged);

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

            jsDeployableArtifact = JavaScriptProxyPackager.packageProxy(distDir, buildDir,
                    request.getMainClass(), request, new JavaScriptProxyPackager.Logger() {
                        public void log(String message) {
                            JavaScriptBuilder.this.log(message);
                        }
                    });
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
        // Recompute per build: a reused builder instance must not carry a
        // previous target's webapp path when this one has none.
        jsPortWebApp = null;
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
                // JavaScriptPort.jar carries the port webapp under webapp/. Move it
                // OUT of the translate-input tree (stageClasses) so the translator
                // doesn't re-copy it into the bundle; it is handed to the translator
                // via -Dcodename1.javascriptport.webapp in runByteCodeTranslator.
                File stagedWebApp = new File(stageClasses, "webapp");
                if (stagedWebApp.isDirectory()) {
                    // tmpDir is this build's own work dir, so port-webapp is already
                    // build-unique; clear any stale copy before moving.
                    File dest = new File(tmpDir, "port-webapp");
                    if (dest.exists()) {
                        delTree(dest, true);
                    }
                    try {
                        Files.move(stagedWebApp.toPath(), dest.toPath());
                    } catch (IOException moveFailed) {
                        // e.g. cross-device move: fall back to copy + delete.
                        copyTree(stagedWebApp, dest);
                        delTree(stagedWebApp, true);
                    }
                    jsPortWebApp = dest;
                }
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
        PrintWriter sw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sourceList), StandardCharsets.UTF_8));
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
        // Source-checkout build: the webapp sits next to the sources at
        // src/main/webapp (portSources is src/main/java).
        File srcWebApp = new File(portSources.getParentFile(), "webapp");
        if (srcWebApp.isDirectory()) {
            jsPortWebApp = srcWebApp;
        }
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

    private File writeLauncher(File workDir, String launcherName, String packageName, String mainClass, File stageClasses,
                               List<Class<?>> nativeInterfaces) throws IOException {
        // If the build-time SVG transcoder generated com.codename1.generated.svg.SVGRegistry
        // for this app, register the transcoded SVGs at startup -- the JS-port analogue of
        // JavaSEPort.init's reflective installGlobal(). A DIRECT call (not reflection) is
        // used so ParparVM's dead-code elimination keeps installGlobal reachable; it is
        // emitted ONLY when the class exists so apps without SVGs still compile.
        boolean hasGeneratedSvg = stageClasses != null
                && new File(stageClasses, "com/codename1/generated/svg/SVGRegistry.class").exists();
        File f = new File(workDir, launcherName + ".java");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));
        try {
            pw.println("import com.codename1.impl.html5.ParparVMBootstrap;");
            pw.println("import " + packageName + "." + mainClass + ";");
            pw.println();
            pw.println("public final class " + launcherName + " {");
            pw.println("    public static void main(String[] args) {");
            if (hasGeneratedSvg) {
                pw.println("        com.codename1.generated.svg.SVGRegistry.installGlobal();");
            }
            // Register the generated native interface implementations. The DIRECT
            // class references (not reflection) also keep the optimizer from culling
            // the *Impl classes, which are otherwise reached only via NativeLookup.
            if (nativeInterfaces != null) {
                for (Class<?> iface : nativeInterfaces) {
                    String ifaceName = iface.getName();
                    pw.println("        com.codename1.system.NativeLookup.register("
                            + ifaceName + ".class, " + ifaceName + "Impl.class);");
                }
            }
            pw.println("        ParparVMBootstrap.bootstrap(new " + mainClass + "());");
            pw.println("    }");
            pw.println("}");
        } finally {
            pw.close();
        }
        return f;
    }

    private void compileLauncher(File launcherJava, List<File> generatedImpls, File stageClasses, File portClasses) throws Exception {
        String javac = resolveJavac();
        List<String> cmd = new ArrayList<String>();
        cmd.add(javac);
        cmd.add("-source"); cmd.add("8");
        cmd.add("-target"); cmd.add("8");
        cmd.add("-cp"); cmd.add(stageClasses.getAbsolutePath() + File.pathSeparator + portClasses.getAbsolutePath());
        cmd.add("-d"); cmd.add(stageClasses.getAbsolutePath());
        cmd.add(launcherJava.getAbsolutePath());
        if (generatedImpls != null) {
            for (File impl : generatedImpls) {
                cmd.add(impl.getAbsolutePath());
            }
        }
        boolean ok = exec(tmpDir, -1, cmd.toArray(new String[cmd.size()]));
        if (!ok) {
            throw new BuildException("Failed to compile JavaScript launcher / native interface impl classes");
        }
    }

    // ----- Native interface binding --------------------------------------------------
    // Scans the staged app classes for com.codename1.system.NativeInterface subtypes and
    // generates, per interface, a <Interface>Impl whose methods delegate to
    // NativeInterfaceBridge.call* (a HOST_HOOK native). At runtime those calls suspend the
    // worker and run the developer's JS stub (cn1_native_interfaces[...][method_]) on the
    // MAIN thread, then resume the worker with the result. Mirrors the cloud builder's
    // JSStubGenerator + NativeLookup.register flow, adapted to the worker/host-call model.

    private List<Class<?>> findNativeInterfaces(File stageClasses) {
        List<Class<?>> result = new ArrayList<Class<?>>();
        URLClassLoader loader = null;
        try {
            loader = new URLClassLoader(new URL[]{ stageClasses.toURI().toURL() },
                    JavaScriptBuilder.class.getClassLoader());
            Class<?> niClass;
            try {
                niClass = loader.loadClass("com.codename1.system.NativeInterface");
            } catch (Throwable t) {
                log("com.codename1.system.NativeInterface not on the classpath; no native interfaces to bind");
                return result;
            }
            List<File> classFiles = new ArrayList<File>();
            collectClassFiles(stageClasses, classFiles);
            for (File cf : classFiles) {
                // Cheap pre-filter: only classes whose bytes mention the marker interface
                // are candidates (native interfaces extend it directly). Avoids loading the
                // thousands of unrelated core/runtime classes.
                byte[] bytes;
                try {
                    bytes = java.nio.file.Files.readAllBytes(cf.toPath());
                } catch (Throwable t) {
                    continue;
                }
                if (!new String(bytes, StandardCharsets.ISO_8859_1).contains("com/codename1/system/NativeInterface")) {
                    continue;
                }
                String cn = classNameFor(stageClasses, cf);
                if (cn == null) {
                    continue;
                }
                try {
                    Class<?> c = loader.loadClass(cn);
                    if (c.isInterface() && !c.equals(niClass) && niClass.isAssignableFrom(c)) {
                        result.add(c);
                        log("Found native interface: " + c.getName());
                    }
                } catch (Throwable ignore) {
                    // class not loadable in isolation (missing deps) -- not a native interface we can bind
                }
            }
        } catch (Throwable t) {
            log("Failed scanning for native interfaces: " + t);
        } finally {
            if (loader != null) {
                try {
                    loader.close();
                } catch (Throwable ignore) {
                }
            }
        }
        return result;
    }

    private static void collectClassFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                collectClassFiles(f, out);
            } else if (f.getName().endsWith(".class") && f.getName().indexOf('$') < 0) {
                out.add(f);
            }
        }
    }

    private static String classNameFor(File root, File classFile) {
        String rootPath = root.getAbsolutePath();
        String filePath = classFile.getAbsolutePath();
        if (!filePath.startsWith(rootPath)) {
            return null;
        }
        String rel = filePath.substring(rootPath.length());
        if (rel.startsWith(File.separator)) {
            rel = rel.substring(1);
        }
        if (!rel.endsWith(".class")) {
            return null;
        }
        rel = rel.substring(0, rel.length() - ".class".length());
        return rel.replace(File.separatorChar, '.').replace('/', '.');
    }

    private List<File> generateNativeInterfaceImpls(File buildDir, List<Class<?>> nativeInterfaces) throws IOException {
        List<File> generated = new ArrayList<File>();
        if (nativeInterfaces == null || nativeInterfaces.isEmpty()) {
            return generated;
        }
        File genDir = new File(buildDir, "generated-native-impls");
        genDir.mkdirs();
        for (Class<?> iface : nativeInterfaces) {
            File jf = writeNativeInterfaceImpl(genDir, iface);
            if (jf != null) {
                generated.add(jf);
            }
        }
        return generated;
    }

    private File writeNativeInterfaceImpl(File genDir, Class<?> iface) throws IOException {
        String pkg = iface.getPackage() != null ? iface.getPackage().getName() : "";
        String simpleImpl = iface.getSimpleName() + "Impl";
        String registryKey = iface.getName().replace('.', '_');

        File pkgDir = pkg.isEmpty() ? genDir : new File(genDir, pkg.replace('.', File.separatorChar));
        pkgDir.mkdirs();
        File out = new File(pkgDir, simpleImpl + ".java");

        StringBuilder sb = new StringBuilder();
        if (!pkg.isEmpty()) {
            sb.append("package ").append(pkg).append(";\n\n");
        }
        sb.append("public class ").append(simpleImpl)
                .append(" implements ").append(iface.getName()).append(" {\n");
        sb.append("    private static final String __NI = \"").append(registryKey).append("\";\n\n");

        for (Method m : iface.getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            appendNativeInterfaceImplMethod(sb, m);
        }
        sb.append("}\n");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8));
        try {
            pw.print(sb.toString());
        } finally {
            pw.close();
        }
        return out;
    }

    private void appendNativeInterfaceImplMethod(StringBuilder sb, Method m) {
        Class<?>[] params = m.getParameterTypes();
        Class<?> ret = m.getReturnType();
        String methodKey = nativeInterfaceMethodKey(m);

        sb.append("    public ").append(ret.getCanonicalName()).append(" ").append(m.getName()).append("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(params[i].getCanonicalName()).append(" p").append(i);
        }
        sb.append(") {\n");

        // Build the boxed argument array.
        StringBuilder args = new StringBuilder();
        if (params.length == 0) {
            args.append("new Object[0]");
        } else {
            args.append("new Object[]{ ");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) args.append(", ");
                args.append(boxArgExpression(params[i], "p" + i));
            }
            args.append(" }");
        }

        String call = "com.codename1.impl.platform.js.NativeInterfaceBridge.";
        String invokeArgs = "__NI, \"" + methodKey + "\", " + args.toString();

        if (ret == void.class) {
            sb.append("        ").append(call).append("callVoid(").append(invokeArgs).append(");\n");
        } else if (ret == boolean.class) {
            sb.append("        return ").append(call).append("callBoolean(").append(invokeArgs).append(");\n");
        } else if (ret == int.class) {
            sb.append("        return ").append(call).append("callInt(").append(invokeArgs).append(");\n");
        } else if (ret == long.class) {
            sb.append("        return ").append(call).append("callLong(").append(invokeArgs).append(");\n");
        } else if (ret == double.class) {
            sb.append("        return ").append(call).append("callDouble(").append(invokeArgs).append(");\n");
        } else if (ret == float.class) {
            sb.append("        return ").append(call).append("callFloat(").append(invokeArgs).append(");\n");
        } else if (ret == byte.class) {
            sb.append("        return ").append(call).append("callByte(").append(invokeArgs).append(");\n");
        } else if (ret == short.class) {
            sb.append("        return ").append(call).append("callShort(").append(invokeArgs).append(");\n");
        } else if (ret == char.class) {
            sb.append("        return ").append(call).append("callChar(").append(invokeArgs).append(");\n");
        } else if (ret == String.class) {
            sb.append("        return ").append(call).append("callString(").append(invokeArgs).append(");\n");
        } else if (ret.isArray()) {
            // Primitive arrays + String[]: callArray builds the correctly-typed
            // Java array from the JS array the host returns (componentToken picks
            // the element type).
            sb.append("        return (").append(ret.getCanonicalName()).append(") ")
                    .append(call).append("callArray(").append(invokeArgs)
                    .append(", \"").append(arrayComponentToken(ret.getComponentType())).append("\");\n");
        } else if ("com.codename1.ui.PeerComponent".equals(ret.getName())) {
            // The stub returns a native element (delivered to the worker as a
            // host-ref); wrap it as a Codename One peer component.
            sb.append("        return com.codename1.ui.PeerComponent.create(")
                    .append(call).append("callObject(").append(invokeArgs).append("));\n");
        } else {
            sb.append("        return (").append(ret.getCanonicalName()).append(") ")
                    .append(call).append("callObject(").append(invokeArgs).append(");\n");
        }
        sb.append("    }\n\n");
    }

    private static String boxArgExpression(Class<?> type, String var) {
        // Pass a PeerComponent's underlying native element (a host-ref) to the
        // stub, not the Java peer wrapper.
        if ("com.codename1.ui.PeerComponent".equals(type.getName())) {
            return var + ".getNativePeer()";
        }
        if (type == int.class) return "Integer.valueOf(" + var + ")";
        if (type == long.class) return "Long.valueOf(" + var + ")";
        if (type == double.class) return "Double.valueOf(" + var + ")";
        if (type == float.class) return "Float.valueOf(" + var + ")";
        if (type == boolean.class) return "Boolean.valueOf(" + var + ")";
        if (type == byte.class) return "Byte.valueOf(" + var + ")";
        if (type == short.class) return "Short.valueOf(" + var + ")";
        if (type == char.class) return "Character.valueOf(" + var + ")";
        return var;
    }

    // Mirrors StubGenerator's JS stub key: methodName + "_" + ("_" + xmlvmType) per param.
    private static String nativeInterfaceMethodKey(Method m) {
        StringBuilder key = new StringBuilder(m.getName()).append("_");
        for (Class<?> p : m.getParameterTypes()) {
            if ("com.codename1.ui.PeerComponent".equals(p.getName())) {
                key.append("_com_codename1_ui_PeerComponent");
            } else {
                key.append("_").append(xmlvmTypeName(p));
            }
        }
        return key.toString();
    }

    // Runtime newArray() component-class token for an array's element type.
    private static String arrayComponentToken(Class<?> component) {
        if (component == int.class) return "JAVA_INT";
        if (component == long.class) return "JAVA_LONG";
        if (component == double.class) return "JAVA_DOUBLE";
        if (component == float.class) return "JAVA_FLOAT";
        if (component == boolean.class) return "JAVA_BOOLEAN";
        if (component == byte.class) return "JAVA_BYTE";
        if (component == short.class) return "JAVA_SHORT";
        if (component == char.class) return "JAVA_CHAR";
        if (component == String.class) return "java_lang_String";
        return component.getName().replace('.', '_');
    }

    private static String xmlvmTypeName(Class<?> type) {
        if (type.isArray()) {
            return xmlvmTypeName(type.getComponentType()) + "_1ARRAY";
        }
        if (type.isPrimitive()) {
            return type.getName();
        }
        return type.getName().replace('.', '_');
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
        java.util.List<String> cmd = new java.util.ArrayList<String>();
        cmd.add("java");
        // Pass through extra translator JVM options (e.g. -Dparparvm.js.*
        // size/diagnostic knobs and kill switches, or a larger -Xmx) from the
        // CN1_TRANSLATOR_OPTS environment variable. The forked JVM does
        // not inherit the Maven process's -D properties, so this is the
        // only way to reach the translator for bisection / tuning.
        String translatorOpts = System.getenv("CN1_TRANSLATOR_OPTS");
        boolean heapOverridden = false;
        java.util.List<String> extraOpts = new java.util.ArrayList<String>();
        if (translatorOpts != null && !translatorOpts.trim().isEmpty()) {
            for (String opt : translatorOpts.trim().split("\\s+")) {
                if (!opt.isEmpty()) {
                    extraOpts.add(opt);
                    if (opt.startsWith("-Xmx")) {
                        heapOverridden = true;
                    }
                }
            }
        }
        // Hand the translator the JavaScript-port webapp (port.js, js/, style.css,
        // ...) so it bundles port.js -- the worker-side native bindings that make
        // Window.current() etc. resolve. Off-repo builds can't find it via the
        // translator's own source-tree walk, so we pass the copy staged from
        // JavaScriptPort.jar. An explicit override in CN1_TRANSLATOR_OPTS wins --
        // detected by an actual -D token, not a loose substring match.
        boolean webAppOverridden = false;
        for (String opt : extraOpts) {
            // Only a -Dkey=value form actually sets the property; a bare -Dkey
            // does not, so it must NOT suppress the plugin-provided value.
            if (opt.startsWith("-Dcodename1.javascriptport.webapp=")) {
                webAppOverridden = true;
                break;
            }
        }
        if (jsPortWebApp != null && jsPortWebApp.isDirectory() && !webAppOverridden) {
            extraOpts.add("-Dcodename1.javascriptport.webapp=" + jsPortWebApp.getAbsolutePath());
        }
        // Default heap; a -Xmx in CN1_TRANSLATOR_OPTS takes precedence (apps
        // that disable tree-shaking, e.g. the Playground, emit a much larger
        // bundle and need a bigger heap to avoid OutOfMemoryError mid-emit).
        if (!heapOverridden) {
            cmd.add("-Xmx512m");
        }
        cmd.addAll(extraOpts);
        cmd.add("-cp");
        cmd.add(compilerJar.getAbsolutePath());
        cmd.add("com.codename1.tools.translator.ByteCodeTranslator");
        cmd.add("javascript");
        cmd.add(stageClasses.getAbsolutePath());
        cmd.add(translatorOut.getAbsolutePath());
        cmd.add(translatorAppName);
        cmd.add(packageName);
        cmd.add(mainClass);
        cmd.add(version == null ? "1.0" : version);
        cmd.add("ios");
        cmd.add("none");
        return exec(tmpDir, env, -1, cmd.toArray(new String[0]));
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
        // getResourceAsStream resources are fetched by the host from
        // ``assets/<name>`` FIRST (port.js mirrors getResourceAsStream's
        // assets/ rewrite), falling back to the dist root only if that
        // 404s. The translator emits these bundled resources at the dist
        // ROOT, so locally the root fallback hides the problem -- but the
        // deployed website serves the app from a path where only assets/
        // is reliably present, so the root fallback also 404s and the
        // resource read fails. Relocate the resources the app fetches at
        // runtime into assets/ so the FIRST fetch succeeds everywhere:
        //   * TrueType fonts (material-design icon font + CSS-merged theme
        //     fonts such as Initializr's Inter-*.ttf),
        //   * the Initializr generator's template archives (*.zip) and
        //     pom templates (*-pom.xml), e.g. common.zip, idea.zip,
        //     eclipse.zip, barebones-*.zip / barebones-pom.xml -- without
        //     these, clicking "Generate Project" 404s the templates and
        //     fails (previously misreported as "Browser storage is full").
        // index.html, *.js, manifest.json, *.res and the generic pom.xml
        // (maven artifact metadata, not a template) are left at the root.
        File assetsDir = new File(distDir, "assets");
        assetsDir.mkdirs();
        File[] rootFiles = distDir.listFiles();
        if (rootFiles != null) {
            for (File rf : rootFiles) {
                if (!rf.isFile()) {
                    continue;
                }
                String lower = rf.getName().toLowerCase();
                boolean relocate = lower.endsWith(".ttf")
                        || lower.endsWith(".zip")
                        || lower.endsWith("-pom.xml");
                if (relocate) {
                    File dest = new File(assetsDir, rf.getName());
                    Files.move(rf.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        // BrowserComponent.setURLHierarchy(path) loads the app's bundled HTML
        // hierarchy from ``assets/cn1html/<path>`` (HTML5Implementation
        // .setBrowserPageInHierarchy). Those files ship packed in html.tar; the
        // CN1 runtime only ever unpacks it into the in-app FileSystemStorage
        // (installTar), which the browser can't fetch over HTTP for an iframe
        // src. Unpack html.tar into assets/cn1html/ so the iframe URL resolves
        // to a real served file (same-origin) -- without this the editor iframe
        // 404s. (Leave html.tar at the root too; installTar still reads it.)
        File htmlTar = new File(distDir, "html.tar");
        if (htmlTar.isFile()) {
            extractHtmlHierarchy(htmlTar, new File(assetsDir, "cn1html"));
        }
    }

    /** Unpack html.tar into {@code destDir} (used to HTTP-serve setURLHierarchy content). */
    private void extractHtmlHierarchy(File htmlTar, File destDir) throws IOException {
        destDir.mkdirs();
        org.xeustechnologies.jtar.TarInputStream tis =
                new org.xeustechnologies.jtar.TarInputStream(new java.io.BufferedInputStream(new FileInputStream(htmlTar)));
        try {
            org.xeustechnologies.jtar.TarEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = tis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null || name.length() == 0 || name.contains("..")) {
                    continue;
                }
                File out = new File(destDir, name);
                if (entry.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(out);
                try {
                    int n;
                    while ((n = tis.read(buf)) != -1) {
                        fos.write(buf, 0, n);
                    }
                } finally {
                    fos.close();
                }
            }
        } finally {
            tis.close();
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
