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
package com.codenameone.devruntime.tools;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates the subclasses that let interpreted code extend a framework class.
 *
 * <p>Neither mobile platform lets you define a class at run time -- ParparVM has
 * no {@code defineClass} and iOS forbids writing executable memory, while
 * Android has both but Play forbids loading dex. So the subclass has to exist
 * before the app ships, with each overridable method compiled to ask the
 * interpreter whether the pushed class overrides it and otherwise call
 * {@code super}.</p>
 *
 * <h2>Nothing here is curated</h2>
 *
 * <p>The set is every public, non-final, constructible class and every public
 * interface under {@code com.codename1}, discovered by walking the framework
 * jar. That is the only honest scope: an application may subclass anything the
 * API exposes, and the failure mode of guessing wrong is not an error message
 * but an override that is silently never called.</p>
 *
 * <p>It is not free -- roughly 56,000 overridable methods, doubled by the
 * {@code super_} bridges. Android carries that with multidex, which large
 * applications do routinely, and this app is a development tool that is
 * already built with the optimizer off and nothing culled. iOS does not pay it
 * at all: there, runtime vtable synthesis subclasses any class with no
 * generated code whatsoever.</p>
 *
 * @author Shai Almog
 */
public final class GenerateInterpShims {
    /**
     * Packages whose classes an interpreted class may extend or implement.
     *
     * <p>A prefix, not a list of classes. The set is derived by walking the
     * framework jar, because a curated list is a promise that every application
     * only subclasses what somebody anticipated -- and the failure when it does
     * not is a method that silently never gets called.</p>
     */
    private static final String API_PREFIX = "com/codename1/";

    /**
     * The one principled exclusion: the port implementation layer.
     *
     * <p>{@code com.codename1.impl} is not app-facing. Interpreted code reaches
     * the platform through the framework, never by subclassing
     * {@code CodenameOneImplementation} -- which alone declares 760 overridable
     * methods. Excluding it is a statement about what the API is, not a guess
     * about what applications need.</p>
     */
    /**
     * Classes the language itself forbids naming as a superclass. Only the
     * compiler may generate a subclass of these.
     */
    private static final Set<String> FORBIDDEN_SUPERTYPES = new LinkedHashSet<String>(
            Arrays.asList("java.lang.Enum", "java.lang.Record"));

    private static final String[] EXCLUDED_PREFIXES = {
            "com/codename1/impl/",
            // The interpreter's own types. A shim for InterpBacked would
            // implement the interface the shims already implement.
            "com/codename1/impl/interp/",
    };

    /**
     * The one subsystem this runtime does not carry in any form.
     *
     * <p>Android Auto and CarPlay are a separate surface with their own
     * manifest, templates and review process, and nothing about a car app can
     * be driven from a pushed program -- so there is nothing to gain by
     * carrying it and a large, awkward dependency to lose.</p>
     *
     * <p>Everything else that cannot be provided honestly is *mocked* instead
     * of excluded: see {@code DeviceRuntimeMocks} in the runtime app for
     * purchases and social login, which are the two a developer most often
     * needs to exercise and least often can. Excluding them left pushed code
     * facing an {@code isSupported()} that answered false, which debugs
     * nothing.</p>
     */
    private static final String[] NATIVE_HEAVY_PREFIXES = {
            // Android Auto and CarPlay: a car app is a separate surface with
            // its own manifest, templates and review process, and none of it
            // can be driven from a pushed program.
            "com/codename1/car/",
    };

    /**
     * Subsystems the runtime carries the native half of, on purpose.
     *
     * <p>These are the reason to run on a device at all. A simulator can fake a
     * layout; it cannot honestly imitate the camera, on-device inference, AR,
     * the health store or a live activity, and a runtime that reported them
     * unsupported would leave exactly the interesting half undebuggable.</p>
     *
     * <p>Linking and subclassing are different needs and the shim set serves
     * only the second. The build decides what native SDK to link by scanning
     * the app for references to the API that fronts it, and most of these types
     * are final or have no accessible constructor -- {@code TextRecognizer} is
     * final -- so no shim mentions them and the SDK is left out. The generator
     * therefore emits a class declaring a field of each type: a field
     * descriptor is what that scan reads, where a class literal is invisible to
     * it (an LDC, not a type instruction).</p>
     *
     * <p>It is not free. ML Kit's bundled models and pipelines are 287MB of
     * native libraries across four ABIs, which is what made an earlier build
     * 323MB. The answer is an ABI split rather than dropping the feature: one
     * ABI is 110MB, and a Play bundle delivers one ABI per device anyway.</p>
     */
    private static final String[] NATIVE_CAPABILITY_PREFIXES = {
            "com/codename1/ai/",
            "com/codename1/ar/",
            "com/codename1/camera/",
            "com/codename1/capture/",
            "com/codename1/health/",
            "com/codename1/media/",
            "com/codename1/bluetooth/",
            "com/codename1/vr/",
            "com/codename1/surfaces/",
            // The vector map, which needs no key and no native provider: a
            // NativeMap with nothing wired in delegates to an embedded MapView,
            // so a pushed program gets a real map rather than a blank one.
            "com/codename1/maps/",
    };

    private GenerateInterpShims() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: GenerateInterpShims <output-source-dir> <framework-jar>"
                    + " [--exclude <file>]");
            System.exit(2);
        }
        // Shim names the caller has already established cannot be compiled.
        // Supplied as a file rather than baked in, because the set is
        // discovered by compiling and belongs to the framework version, not to
        // this generator.
        Set<String> excludedShims = new LinkedHashSet<String>();
        for (int i = 2; i + 1 < args.length; i++) {
            if ("--exclude".equals(args[i])) {
                File f = new File(args[i + 1]);
                if (f.isFile()) {
                    for (String line : new String(java.nio.file.Files.readAllBytes(f.toPath()),
                            StandardCharsets.UTF_8).split("\n")) {
                        String t = line.trim();
                        if (t.length() > 0 && !t.startsWith("#")) {
                            excludedShims.add(t);
                        }
                    }
                }
            }
        }
        File outDir = new File(args[0], "com/codenameone/devruntime/gen");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IllegalStateException("cannot create " + outDir);
        }
        // Clear what a previous run wrote. Maven does not clean a generated
        // source directory between builds, so a class the framework has since
        // dropped leaves its shim behind -- and the next build fails compiling
        // a shim for a type that no longer exists, naming a symbol nobody can
        // find. Regenerating everything from the current jar is the only state
        // that is ever correct.
        File[] previous = outDir.listFiles();
        if (previous != null) {
            for (File f : previous) {
                if (f.getName().endsWith(".java") && !f.delete()) {
                    throw new IllegalStateException("cannot delete stale " + f);
                }
            }
        }
        for (int i = 2; i + 1 < args.length; i++) {
            if ("--java-runtime".equals(args[i])) {
                File jar = new File(args[i + 1]);
                if (jar.isFile()) {
                    deviceJavaTypes = readTypeNames(jar);
                    deviceJavaMethods = readMethodTables(jar, false);
                    deviceFinalMethods = readMethodTables(jar, true);
                }
            }
        }
        File frameworkJar = new File(args[1]);
        if (!frameworkJar.isFile()) {
            throw new IllegalStateException("no framework jar at " + frameworkJar);
        }

        List<String> generated = new ArrayList<String>();
        List<String> ifaceShims = new ArrayList<String>();
        List<Class<?>> classes = new ArrayList<Class<?>>();
        List<Class<?>> interfaces = new ArrayList<Class<?>>();
        scan(frameworkJar, classes, interfaces);

        int skipped = 0;
        int skippedByExclusion = 0;
        List<Class<?>> okClasses = new ArrayList<Class<?>>();
        List<Class<?>> okInterfaces = new ArrayList<Class<?>>();
        for (Class<?> c : classes) {
            String simple = shimName("Interp_", c);
            if (excludedShims.contains(simple)) {
                skippedByExclusion++;
                continue;
            }
            try {
                writeShim(outDir, simple, c);
                generated.add(simple);
                okClasses.add(c);
            } catch (Exception e) {
                // A class the shim cannot legally extend -- an inaccessible
                // supertype, a name collision -- is dropped rather than
                // emitted broken. Reported, so the count is never silently
                // smaller than the API.
                System.out.println("skipping " + c.getName() + ": " + e.getMessage());
                skipped++;
            }
        }
        for (Class<?> c : interfaces) {
            String simple = shimName("Interp_I_", c);
            if (excludedShims.contains(simple)) {
                skippedByExclusion++;
                continue;
            }
            try {
                writeInterfaceShim(outDir, simple, c);
                ifaceShims.add(simple);
                okInterfaces.add(c);
            } catch (Exception e) {
                System.out.println("skipping " + c.getName() + ": " + e.getMessage());
                skipped++;
            }
        }

        writeNativeCapabilities(outDir, frameworkJar);
        writeRegistry(outDir, generated, okClasses, ifaceShims, okInterfaces);
        System.out.println("generated " + generated.size() + " class shims, "
                + ifaceShims.size() + " interface shims, " + skipped + " skipped, "
                + skippedByExclusion + " excluded, into " + outDir);
    }

    /**
     * Writes the class that names every native-backed capability.
     *
     * <p>Class literals and nothing else. Each is a constant-pool entry, which
     * is exactly what the Codename One build scans for when deciding which
     * native SDK an app needs -- and none of them runs, so the app pays the
     * link cost and no behaviour.</p>
     *
     * <p>Without this the runtime links a camera SDK only if some *shim*
     * happens to mention the camera API, and most of these classes are final or
     * have no accessible constructor, so they get no shim. The result was an
     * app that reported "unsupported" for the very things a device is for.</p>
     */
    private static void writeNativeCapabilities(File dir, File jar) throws Exception {
        List<String> names = new ArrayList<String>();
        java.util.jar.JarFile jf = new java.util.jar.JarFile(jar);
        try {
            java.util.Enumeration<java.util.jar.JarEntry> e = jf.entries();
            while (e.hasMoreElements()) {
                String n = e.nextElement().getName();
                if (!n.endsWith(".class") || !isNativeCapability(n) || n.indexOf('$') >= 0) {
                    continue;
                }
                names.add(n.substring(0, n.length() - 6).replace('/', '.'));
            }
        } finally {
            jf.close();
        }
        java.util.Collections.sort(names);

        java.net.URLClassLoader loader = new java.net.URLClassLoader(
                new java.net.URL[]{ jar.toURI().toURL() },
                GenerateInterpShims.class.getClassLoader());
        List<String> referenced = new ArrayList<String>();
        for (String name : names) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                if (java.lang.reflect.Modifier.isPublic(c.getModifiers())) {
                    referenced.add(name);
                }
            } catch (Throwable notLoadable) {
                // A class the framework jar names but this tool chain cannot
                // load is not one the build will link either.
            }
        }
        loader.close();

        PrintWriter w = new PrintWriter(new File(dir, "InterpNativeCapabilities.java"), "UTF-8");
        try {
            header(w);
            w.println("package com.codenameone.devruntime.gen;");
            w.println();
            w.println("/**");
            w.println(" * The native capabilities this runtime carries, named so the build links");
            w.println(" * them.");
            w.println(" *");
            w.println(" * <p>Generated, and nothing here runs: the class is never instantiated and");
            w.println(" * the fields are never read. A field's descriptor is the reference -- the");
            w.println(" * Codename One build decides which native SDK an app needs by scanning");
            w.println(" * field and method descriptors, and a class literal is invisible to that");
            w.println(" * scan because it is an LDC rather than a type instruction.</p>");
            w.println(" *");
            w.println(" * <p>Most of these types are final or have no accessible constructor, so no");
            w.println(" * shim mentions them and without this file the runtime would report the");
            w.println(" * camera, on-device inference, AR and the health store as unsupported --");
            w.println(" * on a device that supports them, which is the one place it matters.</p>");
            w.println(" */");
            w.println("public final class InterpNativeCapabilities {");
            int index = 0;
            for (String name : referenced) {
                w.println("    " + name + " c" + index + ";");
                index++;
            }
            w.println();
            w.println("    private InterpNativeCapabilities() {");
            w.println("    }");
            w.println("}");
        } finally {
            w.close();
        }
        System.out.println("native capabilities referenced: " + referenced.size());
    }

    private static boolean isNativeCapability(String entry) {
        for (String p : NATIVE_CAPABILITY_PREFIXES) {
            if (entry.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every app-facing framework type an interpreted class could extend or
     * implement.
     *
     * <p>Walking the jar rather than naming classes is the whole point: an
     * application may subclass anything the API exposes, and a list maintained
     * by hand is a list that is wrong the first time somebody subclasses
     * something unusual.</p>
     */
    private static void scan(File jar, List<Class<?>> classes, List<Class<?>> interfaces)
            throws Exception {
        java.util.jar.JarFile jf = new java.util.jar.JarFile(jar);
        java.net.URLClassLoader loader = new java.net.URLClassLoader(
                new java.net.URL[]{ jar.toURI().toURL() },
                GenerateInterpShims.class.getClassLoader());
        List<String> names = new ArrayList<String>();
        try {
            java.util.Enumeration<java.util.jar.JarEntry> e = jf.entries();
            while (e.hasMoreElements()) {
                String n = e.nextElement().getName();
                if (!n.endsWith(".class") || !n.startsWith(API_PREFIX) || excluded(n)) {
                    continue;
                }
                names.add(n.substring(0, n.length() - 6).replace('/', '.'));
            }
        } finally {
            jf.close();
        }
        // The device's java.* subset is API too. An application that says
        // `implements Runnable` -- which is most of them -- needs a shim for
        // Runnable exactly as much as for ActionListener, and scanning only
        // com.codename1 silently left it out.
        names.addAll(javaApiTypeNames());
        java.util.Collections.sort(names);
        for (String n : names) {
            Class<?> c;
            try {
                c = Class.forName(n, false, loader);
            } catch (Throwable t) {
                continue;   // a class whose own dependencies are absent here
            }
            if (!isReachable(c) || c.isAnonymousClass() || c.isSynthetic()) {
                continue;
            }
            int m = c.getModifiers();
            if (c.isInterface()) {
                if (c.isAnnotation()) {
                    continue;
                }
                // An interface shim is concrete, so it must implement every
                // method -- the same rule as a class, and it fails the same way
                // when the JDK declares one the device does not have.
                String missing = interfaceMethodNotOnDevice(c);
                if (missing != null) {
                    System.out.println("not shimmable: " + c.getName() + " (" + missing
                            + " is not on the device or names a type that is not)");
                    continue;
                }
                interfaces.add(c);
                continue;
            }
            if (Modifier.isFinal(m) || c.isEnum()) {
                continue;
            }
            if (!hasReachableConstructor(c)) {
                continue;
            }
            if (c.isMemberClass() && !Modifier.isStatic(c.getModifiers())) {
                continue;   // needs an enclosing instance to construct
            }
            if (FORBIDDEN_SUPERTYPES.contains(c.getName())) {
                continue;   // the language forbids extending these directly
            }
            // A hierarchy difference is deliberately NOT a reason to skip. The
            // JDK's Throwable implements Serializable and the device's does not,
            // which rejected every exception class in the framework -- and
            // `class MyException extends RuntimeException` is table stakes for a
            // real application. Serializable and Cloneable declare no methods at
            // all, so the difference is invisible to a subclass; where the extra
            // interface does declare methods (Closeable.close, Readable.read),
            // the shim implements them because javac here demands it and they
            // are harmless there -- on the device they override nothing.
            //
            // What actually blocks a shim is a signature the device cannot
            // express, which is the check below, applied per method.
            String unrepresentable = abstractMethodNotOnDevice(c);
            if (unrepresentable != null) {
                // A concrete shim must implement every abstract method it
                // inherits, and this one names a type the device does not have
                // -- Format.parseObject(String,ParsePosition) when the subset
                // has no ParsePosition. Writing it would not compile here and
                // referencing it would not run there.
                System.out.println("not shimmable: " + c.getName()
                        + " (" + unrepresentable + ")");
                continue;
            }
            String blocker = packagePrivateAbstract(c);
            if (blocker != null) {
                // Java forbids implementing a package-private abstract method
                // from another package, so no subclass of this class can exist
                // outside its own -- SurfaceNode.serializeContent is one. This
                // is the framework's shape, not a generator limitation.
                System.out.println("not subclassable outside its package: "
                        + c.getName() + " (" + blocker + " is package-private abstract)");
                continue;
            }
            classes.add(c);
        }
    }

    /**
     * Type names of the device's {@code java.*} subset, from its source tree.
     *
     * <p>Taken from {@code vm/JavaAPI/src} rather than from the JDK, because
     * what matters is what the device has. The classes are then loaded from the
     * JDK -- they are the same types -- and any place the two disagree is
     * caught by the compile and compliance gates rather than shipped.</p>
     */
    private static List<String> javaApiTypeNames() {
        return deviceJavaTypes == null
                ? new ArrayList<String>()
                : new ArrayList<String>(deviceJavaTypes);
    }

    /**
     * Whether the device's version of the class has this constructor.
     *
     * <p>Same skew as methods: the JDK's {@code Timer} has
     * {@code Timer(String, boolean)} and the device's does not, and a peer
     * constructor chaining to it fails the compliance gate.</p>
     */
    private static boolean declaredOnDevice(Constructor<?> k) {
        if (deviceJavaMethods == null) {
            return true;
        }
        String owner = k.getDeclaringClass().getName();
        if (!owner.startsWith("java.") && !owner.startsWith("javax.")) {
            return true;
        }
        Set<String> declared = deviceJavaMethods.get(owner);
        if (declared == null) {
            return false;
        }
        StringBuilder sb = new StringBuilder("<init>(");
        for (Class<?> p : k.getParameterTypes()) {
            sb.append(descriptorOf(p));
        }
        return declared.contains(sb.append(")V").toString());
    }

    /** Whether the device declares this method final, whatever the JDK says. */
    private static boolean finalOnDevice(Method m) {
        if (deviceFinalMethods == null) {
            return false;
        }
        String owner = m.getDeclaringClass().getName();
        if (!owner.startsWith("java.") && !owner.startsWith("javax.")) {
            return false;
        }
        Set<String> declared = deviceFinalMethods.get(owner);
        return declared != null && declared.contains(m.getName() + descriptorOf(m));
    }

    /**
     * Whether the device's own version of the declaring class has this method.
     *
     * <p>Only asked of {@code java.*}: the framework classes come from the very
     * jar the app compiles against, so there is nothing to disagree with.</p>
     */
    private static boolean declaredOnDevice(Method m) {
        if (deviceJavaMethods == null) {
            return true;
        }
        String owner = m.getDeclaringClass().getName();
        if (!owner.startsWith("java.") && !owner.startsWith("javax.")) {
            return true;
        }
        Set<String> declared = deviceJavaMethods.get(owner);
        return declared != null && declared.contains(m.getName() + descriptorOf(m));
    }

    /** Reads name+descriptor sets straight out of each class file's method table. */
    private static Map<String, Set<String>> readMethodTables(File jar, final boolean finalOnly) {
        Map<String, Set<String>> out = new LinkedHashMap<String, Set<String>>();
        try {
            java.util.jar.JarFile jf = new java.util.jar.JarFile(jar);
            try {
                java.util.Enumeration<java.util.jar.JarEntry> e = jf.entries();
                while (e.hasMoreElements()) {
                    java.util.jar.JarEntry entry = e.nextElement();
                    if (!entry.getName().endsWith(".class")) {
                        continue;
                    }
                    java.io.InputStream in = jf.getInputStream(entry);
                    try {
                        final Set<String> methods = new LinkedHashSet<String>();
                        new org.objectweb.asm.ClassReader(in).accept(
                                new org.objectweb.asm.ClassVisitor(
                                        org.objectweb.asm.Opcodes.ASM9) {
                                    public org.objectweb.asm.MethodVisitor visitMethod(
                                            int access, String name, String desc,
                                            String sig, String[] ex) {
                                        boolean isFinal = (access
                                                & org.objectweb.asm.Opcodes.ACC_FINAL) != 0;
                                        if (!finalOnly || isFinal) {
                                            methods.add(name + desc);
                                        }
                                        return null;
                                    }
                                },
                                org.objectweb.asm.ClassReader.SKIP_CODE
                                        | org.objectweb.asm.ClassReader.SKIP_DEBUG
                                        | org.objectweb.asm.ClassReader.SKIP_FRAMES);
                        out.put(entry.getName().substring(0, entry.getName().length() - 6)
                                .replace('/', '.'), methods);
                    } finally {
                        in.close();
                    }
                }
            } finally {
                jf.close();
            }
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("cannot read " + jar, ex);
        }
        return out;
    }

    /** Every class name in a jar. */
    private static Set<String> readTypeNames(File jar) {
        Set<String> out = new LinkedHashSet<String>();
        try {
            java.util.jar.JarFile jf = new java.util.jar.JarFile(jar);
            try {
                java.util.Enumeration<java.util.jar.JarEntry> e = jf.entries();
                while (e.hasMoreElements()) {
                    String n = e.nextElement().getName();
                    if (n.endsWith(".class")) {
                        out.add(n.substring(0, n.length() - 6).replace('/', '.'));
                    }
                }
            } finally {
                jf.close();
            }
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("cannot read " + jar, ex);
        }
        return out;
    }

    private static boolean excluded(String entry) {
        for (String p : NATIVE_HEAVY_PREFIXES) {
            if (entry.startsWith(p)) {
                return true;
            }
        }
        for (String p : EXCLUDED_PREFIXES) {
            if (entry.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the type can be named from another package at all.
     *
     * <p>Public is not enough for a nested type: {@code GeneralPath.ShapeUtil}
     * is a public class inside a package-private one, and naming it from
     * outside does not compile.</p>
     */
    private static boolean isReachable(Class<?> c) {
        for (Class<?> k = c; k != null; k = k.getEnclosingClass()) {
            if (!Modifier.isPublic(k.getModifiers())) {
                return false;
            }
        }
        return true;
    }

    /**
     * The first interface method the device does not have, or null.
     *
     * <p>Walks the raw declarations rather than {@link #collectInterfaceMethods},
     * which already drops what the device lacks -- asking it would be asking
     * whether anything it removed is missing.</p>
     */
    private static String interfaceMethodNotOnDevice(Class<?> c) {
        List<Method> all = new ArrayList<Method>();
        collectRawInterfaceMethods(c, all);
        for (Method m : all) {
            if (!declaredOnDevice(m)) {
                return m.getDeclaringClass().getSimpleName() + "." + m.getName();
            }
            if (!isUsable(m.getReturnType())) {
                return m.getDeclaringClass().getSimpleName() + "." + m.getName();
            }
            for (Class<?> p : m.getParameterTypes()) {
                if (!isUsable(p)) {
                    return m.getDeclaringClass().getSimpleName() + "." + m.getName();
                }
            }
        }
        return null;
    }

    private static void collectRawInterfaceMethods(Class<?> c, List<Method> out) {
        for (Method m : declaredMethodsSorted(c)) {
            int mo = m.getModifiers();
            if (Modifier.isStatic(mo) || m.isSynthetic() || m.isBridge()
                    || !Modifier.isAbstract(mo)) {
                continue;
            }
            out.add(m);
        }
        for (Class<?> i : c.getInterfaces()) {
            collectRawInterfaceMethods(i, out);
        }
    }

    /**
     * The first abstract method whose signature the device cannot express, or
     * null. Such a class cannot have a concrete subclass generated for it.
     */
    private static String abstractMethodNotOnDevice(Class<?> c) {
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Method m : declaredMethodsSorted(k)) {
                if (!Modifier.isAbstract(m.getModifiers()) || m.isSynthetic() || m.isBridge()) {
                    continue;
                }
                // An abstract method the device's version does not declare is
                // not a problem: javac here requires the shim to implement it,
                // and there it is simply a method that overrides nothing. Only
                // a signature the device cannot name rules the class out.
                //
                // Final on the device against abstract here does rule it out,
                // and it is not hypothetical: Calendar.add is abstract on the
                // JDK and final on the device, so a concrete subclass must
                // declare it to compile here and must not declare it to compile
                // there. No shim can satisfy both, so Calendar has none.
                if (finalOnDevice(m)) {
                    return "abstract " + k.getSimpleName() + "." + m.getName()
                            + " is final on the device";
                }
                if (!isUsable(m.getReturnType())) {
                    return "abstract " + k.getSimpleName() + "." + m.getName()
                            + " names a type the device does not have";
                }
                for (Class<?> p : m.getParameterTypes()) {
                    if (!isUsable(p)) {
                        return "abstract " + k.getSimpleName() + "." + m.getName()
                                + " names a type the device does not have";
                    }
                }
            }
        }
        return null;
    }

    /**
     * The first package-private abstract method that makes this class
     * unsubclassable from outside its package, or null.
     */
    private static String packagePrivateAbstract(Class<?> c) {
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Method m : declaredMethodsSorted(k)) {
                int mo = m.getModifiers();
                if (!Modifier.isAbstract(mo)) {
                    continue;
                }
                if (!Modifier.isPublic(mo) && !Modifier.isProtected(mo)) {
                    return k.getSimpleName() + "." + m.getName();
                }
            }
        }
        return null;
    }

    /** Whether {@code super()} with no arguments is legal from another package. */
    private static boolean hasNoArgConstructor(Class<?> c) {
        for (Constructor<?> k : declaredConstructorsSorted(c)) {
            int km = k.getModifiers();
            if (k.getParameterTypes().length == 0
                    && (Modifier.isPublic(km) || Modifier.isProtected(km))
                    && declaredOnDevice(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a shim in another package could call {@code super(...)} at all.
     * A class with only private or package-private constructors cannot be
     * subclassed from outside its package, so no shim is possible or needed.
     */
    private static boolean hasReachableConstructor(Class<?> c) {
        for (Constructor<?> k : declaredConstructorsSorted(c)) {
            int km = k.getModifiers();
            if ((Modifier.isPublic(km) || Modifier.isProtected(km)) && declaredOnDevice(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A unique flat name for a shim.
     *
     * <p>Simple names collide across packages once the whole API is in scope --
     * there is more than one {@code Border}, more than one {@code Style} -- so
     * the package rides along, flattened.</p>
     */
    private static String shimName(String prefix, Class<?> c) {
        String n = c.getName();
        if (n.startsWith("com.codename1.")) {
            n = n.substring("com.codename1.".length());
        }
        return prefix + n.replace('.', '_').replace('$', '_');
    }

    /**
     * Emits a concrete class implementing a framework interface, forwarding
     * every method to the interpreter.
     *
     * <p>Simpler than a class shim in the one way that matters: an interface has
     * no implementation to fall back to, so there is no {@code super_} bridge
     * and no "not overridden" case worth deferring. A pushed class that declares
     * the interface and then fails to implement a method is a class that would
     * not have compiled, so the unreachable branch throws rather than
     * pretending.</p>
     */
    private static void writeInterfaceShim(File dir, String simpleName, Class<?> target)
            throws Exception {
        PrintWriter w = new PrintWriter(new File(dir, simpleName + ".java"), "UTF-8");
        try {
            header(w);
            w.println("package com.codenameone.devruntime.gen;");
            w.println();
            w.println("import com.codename1.impl.interp.InterpBacked;");
            w.println("import com.codename1.impl.interp.InterpObject;");
            w.println("import com.codename1.impl.interp.InterpRuntime;");
            w.println();
            w.println("/** Lets an interpreted class implement {@link " + typeName(target)
                    + "}. */");
            w.println("public final class " + simpleName + " implements " + typeName(target)
                    + ", InterpBacked {");
            w.println("    private final InterpObject $interp;");
            w.println("    private final InterpRuntime $runtime;");
            w.println();
            w.println("    public " + simpleName + "(InterpRuntime runtime, InterpObject interp) {");
            w.println("        this.$runtime = runtime;");
            w.println("        this.$interp = interp;");
            w.println("    }");
            w.println();
            w.println("    public InterpObject getInterpObject() {");
            w.println("        return $interp;");
            w.println("    }");
            w.println();

            Map<String, Class<?>> bindings = typeBindings(target);
            Map<String, Method> ifaceMethods = collectInterfaceMethods(target);
            for (Method m : ifaceMethods.values()) {
                emitInterfaceMethod(w, m, target, bindings);
            }
            emitObjectMethods(w, ifaceMethods.keySet(), target);
            w.println("}");
        } finally {
            w.close();
        }
    }

    /** Every method an implementation of the interface has to provide. */
    private static Map<String, Method> collectInterfaceMethods(Class<?> target) {
        Map<String, Method> out = new LinkedHashMap<String, Method>();
        collectInterfaceMethods(target, out);
        return out;
    }

    private static void collectInterfaceMethods(Class<?> iface, Map<String, Method> out) {
        for (Method m : declaredMethodsSorted(iface)) {
            if (Modifier.isStatic(m.getModifiers()) || m.isSynthetic() || m.isBridge()
                    || !declaredOnDevice(m)) {
                continue;
            }
            String key = m.getName() + paramDescriptorOf(m);
            if (!out.containsKey(key)) {
                out.put(key, m);
            }
        }
        for (Class<?> parent : iface.getInterfaces()) {
            collectInterfaceMethods(parent, out);
        }
    }

    private static void emitInterfaceMethod(PrintWriter w, Method m, Class<?> target,
                                            Map<String, Class<?>> bindings) {
        Class<?>[] params = resolvedParams(m, bindings);
        Class<?> ret = resolved(m.getGenericReturnType(), m.getReturnType(), m, bindings);
        StringBuilder sig = new StringBuilder();
        StringBuilder boxed = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sig.append(", ");
                boxed.append(", ");
            }
            sig.append(typeName(params[i])).append(" a").append(i);
            boxed.append("a").append(i);
        }
        w.println("    public " + typeName(ret) + " " + m.getName() + "(" + sig + ")"
                + throwsClause(m) + " {");
        emitDispatch(w, m, "$runtime.dispatch($interp, \"" + m.getName() + "\", \""
                + descriptorOf(params, ret) + "\", new Object[]{" + boxed + "})");
        if (!m.isDefault()) {
            // A stopped program's peer is still held by whatever registered it.
            // Answering nothing is the point of detaching; throwing here would
            // turn an expected late callback into an event-thread failure.
            w.println("        if ($r == InterpRuntime.DETACHED) {");
            w.println(ret == Void.TYPE ? "            return;"
                    : "            return " + zero(ret) + ";");
            w.println("        }");
        }
        w.println("        if (" + (m.isDefault() ? MISS : "$r == InterpRuntime.NOT_OVERRIDDEN")
                + ") {");
        if (m.isDefault()) {
            // The interface's own default implementation. A pushed class that
            // implements a host interface and does not override a default
            // method is ordinary Java, and throwing here would make its peer
            // fail on a method the interface plainly provides.
            String call = target.getName() + ".super." + m.getName() + "(" + boxed + ")";
            if (ret == Void.TYPE) {
                w.println("            " + call + ";");
                w.println("            return;");
            } else {
                w.println("            return " + call + ";");
            }
        } else {
            w.println("            throw new AbstractMethodError(\"" + target.getName() + "."
                    + m.getName() + "\");");
        }
        w.println("        }");
        if (ret != Void.TYPE) {
            w.println("        return " + unbox(ret, "$r") + ";");
        }
        w.println("    }");
        w.println();
        if (m.isDefault()) {
            emitInterfaceSuperBridge(w, m, target, params, ret);
        }
    }

    /**
     * The bridge {@code HostInterface.super.method(...)} needs.
     *
     * <p>Interpreted code writing that produces an invokespecial, which the
     * runtime serves by calling {@code super_method} on the peer. Without a
     * bridge the fallback calls the method itself, and on a reflective linker
     * {@code Method.invoke} dispatches virtually -- straight back into the
     * shim's override, which asks the interpreter, which calls super again,
     * until the stack gives out.</p>
     */
    private static void emitInterfaceSuperBridge(PrintWriter w, Method m, Class<?> target,
                                                 Class<?>[] params, Class<?> ret) {
        StringBuilder sig = new StringBuilder();
        StringBuilder call = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sig.append(", ");
                call.append(", ");
            }
            sig.append(typeName(params[i])).append(" a").append(i);
            call.append("a").append(i);
        }
        w.println("    public " + typeName(ret) + " super_" + m.getName() + "(" + sig + ")"
                + throwsClause(m) + " {");
        String invocation = target.getName() + ".super." + m.getName() + "(" + call + ")";
        if (ret == Void.TYPE) {
            w.println("        " + invocation + ";");
        } else {
            w.println("        return " + invocation + ";");
        }
        w.println("    }");
        w.println();
    }

    private static void writeShim(File dir, String simpleName, Class<?> target) throws Exception {
        PrintWriter w = new PrintWriter(new File(dir, simpleName + ".java"), "UTF-8");
        try {
            header(w);
            w.println("package com.codenameone.devruntime.gen;");
            w.println();
            w.println("import com.codename1.impl.interp.InterpBacked;");
            w.println("import com.codename1.impl.interp.InterpObject;");
            w.println("import com.codename1.impl.interp.InterpRuntime;");
            w.println();
            w.println("/**");
            w.println(" * Lets an interpreted class extend {@link " + typeName(target) + "}.");
            w.println(" *");
            w.println(" * <p>Generated by GenerateInterpShims. Every override asks the runtime");
            w.println(" * whether the pushed class provides the method and otherwise defers to");
            w.println(" * super, so a class that overrides nothing behaves exactly like the");
            w.println(" * framework class it extends.</p>");
            w.println(" */");
            w.println("public final class " + simpleName + " extends " + typeName(target)
                    + " implements InterpBacked {");
            w.println("    private final InterpObject $interp;");
            w.println("    private final InterpRuntime $runtime;");
            w.println();
            // Only when super() is legal. Many framework classes have no
            // accessible no-argument constructor, and an implicit super() call
            // to one that does not exist does not compile.
            if (hasNoArgConstructor(target)) {
                w.println("    public " + simpleName + "(InterpRuntime runtime, InterpObject interp) {");
                w.println("        this.$runtime = runtime;");
                w.println("        this.$interp = interp;");
                w.println("    }");
                w.println();
            }
            // One constructor per framework constructor, so `super("title")` in
            // interpreted code reaches the real superclass constructor instead
            // of silently collapsing to the no-arg one and losing its arguments.
            emitConstructors(w, simpleName, target);
            w.println("    public InterpObject getInterpObject() {");
            w.println("        return $interp;");
            w.println("    }");
            w.println();

            Map<String, Class<?>> bindings = typeBindings(target);
            Map<String, Method> methods = collectOverridable(target);
            for (Map.Entry<String, Method> e : methods.entrySet()) {
                emitOverride(w, e.getValue(), bindings);
            }
            emitObjectMethods(w, methods.keySet(), target);
            w.println("}");
        } finally {
            w.close();
        }
    }

    /**
     * Emits one constructor per accessible framework constructor, plus a
     * descriptor-keyed factory the runtime uses to pick the right one.
     *
     * <p>The runtime learns which superclass constructor the interpreted class
     * chained to only when it sees the {@code invokespecial <init>}, so the
     * choice has to be made by descriptor at that moment rather than baked in.</p>
     */
    private static void emitConstructors(PrintWriter w, String simpleName, Class<?> target) {
        List<Constructor<?>> ctors = new ArrayList<Constructor<?>>();
        for (Constructor<?> c : declaredConstructorsSorted(target)) {
            int mod = c.getModifiers();
            // The shim lives in its own package, so only public and protected
            // constructors are reachable from its super(...) call. A
            // package-private one compiles here and fails at the call site.
            if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
                continue;
            }
            boolean usable = true;
            for (Class<?> p : c.getParameterTypes()) {
                if (!isUsable(p)) {
                    usable = false;
                    break;
                }
            }
            if (usable && declaredOnDevice(c)) {
                ctors.add(c);
            }
        }
        for (Constructor<?> c : ctors) {
            Class<?>[] params = c.getParameterTypes();
            if (params.length == 0) {
                // The (runtime, interp) constructor emitted above already
                // chains to the no-arg superclass constructor; declaring it
                // again here would be the same signature twice.
                continue;
            }
            StringBuilder sig = new StringBuilder();
            StringBuilder call = new StringBuilder();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    sig.append(", ");
                    call.append(", ");
                }
                sig.append(typeName(params[i])).append(" a").append(i);
                call.append("a").append(i);
            }
            String sep = params.length == 0 ? "" : ", ";
            w.println("    public " + simpleName + "(InterpRuntime runtime, InterpObject interp"
                    + sep + sig + ")" + ctorThrows(c) + " {");
            w.println("        super(" + call + ");");
            w.println("        this.$runtime = runtime;");
            w.println("        this.$interp = interp;");
            w.println("    }");
            w.println();
        }

        w.println("    /** Builds the peer for the superclass constructor the pushed class used. */");
        w.println("    public static Object create(InterpRuntime rt, InterpObject o,");
        w.println("                                String descriptor, Object[] args) throws Throwable {");
        for (Constructor<?> c : ctors) {
            Class<?>[] params = c.getParameterTypes();
            StringBuilder desc = new StringBuilder("(");
            for (Class<?> p : params) {
                desc.append(descriptorOf(p));
            }
            desc.append(")V");
            StringBuilder cast = new StringBuilder();
            for (int i = 0; i < params.length; i++) {
                cast.append(", ").append(castFromObject(params[i], "args[" + i + "]"));
            }
            w.println("        if (\"" + desc + "\".equals(descriptor)) {");
            w.println("            return new " + simpleName + "(rt, o" + cast + ");");
            w.println("        }");
        }
        // Anything else is a constructor this shim does not have -- one the
        // device's API lacks, or one the generator could not emit. Substituting
        // the no-argument peer would run a different constructor than the
        // program wrote, silently losing both its arguments and whatever that
        // constructor does; saying so names the class and the descriptor.
        w.println("        throw new UnsupportedOperationException(\"" + typeName(target)
                + " has no constructor \" + descriptor + \" on this device\");");
        w.println("    }");
        w.println();
    }

    private static String castFromObject(Class<?> t, String expr) {
        if (t == Boolean.TYPE) return "((Boolean)" + expr + ").booleanValue()";
        if (t == Byte.TYPE) return "((Number)" + expr + ").byteValue()";
        if (t == Character.TYPE) return "((Character)" + expr + ").charValue()";
        if (t == Short.TYPE) return "((Number)" + expr + ").shortValue()";
        if (t == Integer.TYPE) return "((Number)" + expr + ").intValue()";
        if (t == Long.TYPE) return "((Number)" + expr + ").longValue()";
        if (t == Float.TYPE) return "((Number)" + expr + ").floatValue()";
        if (t == Double.TYPE) return "((Number)" + expr + ").doubleValue()";
        return "(" + typeName(t) + ")" + expr;
    }

    /** Overridable methods of the target, keyed by erasure signature. */
    private static Map<String, Method> collectOverridable(Class<?> target) {
        Map<String, Method> out = new LinkedHashMap<String, Method>();
        Map<String, Class<?>> bindings = typeBindings(target);
        // Signatures already emitted, keyed after type-variable resolution: a
        // concrete compare(String,String) and Comparator's resolved compare(T,T)
        // are one method, and emitting both is a duplicate definition.
        Set<String> seen = new LinkedHashSet<String>();
        // A method that is final anywhere between the target and the class that
        // declares it cannot be overridden, even though a superclass declares
        // it non-final. Component.getComponentForm() is overridable; Form
        // re-declares it final, so a subclass of Form may not touch it. Collect
        // those first and let them block the inherited declaration.
        Set<String> blocked = new LinkedHashSet<String>();
        for (Class<?> c = target; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : declaredMethodsSorted(c)) {
                if (Modifier.isFinal(m.getModifiers()) || Modifier.isPrivate(m.getModifiers())
                        || finalOnDevice(m)) {
                    blocked.add(m.getName() + paramDescriptorOf(m));
                    blocked.add(m.getName()
                            + descriptorOfParams(resolvedParams(m, bindings)));
                }
            }
        }
        for (Class<?> c = target; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : declaredMethodsSorted(c)) {
                int mod = m.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || Modifier.isPrivate(mod)) {
                    continue;
                }
                if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
                    continue;
                }
                // A concrete method the device does not have is not worth
                // overriding -- there is nothing there to call back into it.
                // An abstract one has to be implemented anyway or the shim will
                // not compile here, whatever the device thinks of it.
                if (m.isSynthetic() || m.isBridge()
                        || (!declaredOnDevice(m) && !Modifier.isAbstract(mod))) {
                    continue;
                }
                if (m.getDeclaringClass() == Throwable.class) {
                    // printStackTrace and fillInStackTrace are Throwable's, not
                    // Codename One's, and the device API subset does not carry
                    // them -- the bytecode compliance gate rejects a shim that
                    // references them. An interpreted exception subclass has
                    // nothing to gain from overriding them anyway.
                    continue;
                }
                // A parameter or return type the app does not expose would not
                // compile in the shim; skip rather than emit something broken.
                if (!isUsable(m.getReturnType())) {
                    continue;
                }
                boolean usable = true;
                for (Class<?> p : m.getParameterTypes()) {
                    if (!isUsable(p)) {
                        usable = false;
                        break;
                    }
                }
                if (!usable) {
                    continue;
                }
                String key = m.getName() + paramDescriptorOf(m);
                if (blocked.contains(key)) {
                    continue;
                }
                String resolvedKey = m.getName()
                        + descriptorOfParams(resolvedParams(m, bindings));
                if (!out.containsKey(key) && !seen.contains(resolvedKey)) {
                    out.put(key, m);
                    seen.add(resolvedKey);
                }
            }
        }
        // Interface abstracts last, and only for signatures no class in the
        // chain implements. Collecting them first let StyleListener's abstract
        // styleChanged beat Component's concrete one, so the shim threw
        // AbstractMethodError where it should have called super.
        collectInterfaceAbstracts(target, target, out, bindings, seen);
        for (java.util.Iterator<Map.Entry<String, Method>> it = out.entrySet().iterator();
                it.hasNext(); ) {
            Map.Entry<String, Method> e = it.next();
            String resolvedKey = e.getValue().getName()
                    + descriptorOfParams(resolvedParams(e.getValue(), bindings));
            if (blocked.contains(e.getKey()) || blocked.contains(resolvedKey)) {
                it.remove();
            }
        }
        return out;
    }

    /**
     * Whether some class between {@code from} and Object already provides a
     * concrete implementation of this interface method's name and parameters.
     *
     * <p>Compared on name and parameters only, deliberately: a covariant
     * override has a different return type and is still the implementation.</p>
     */
    private static boolean implementedByAClass(Class<?> from, Method m) {
        for (Class<?> k = from; k != null; k = k.getSuperclass()) {
            for (Method candidate : declaredMethodsSorted(k)) {
                int cm = candidate.getModifiers();
                // Private and static methods do not implement an interface
                // method however well their signatures match.
                if (!candidate.getName().equals(m.getName())
                        || Modifier.isAbstract(cm) || Modifier.isStatic(cm)
                        || Modifier.isPrivate(cm)) {
                    continue;
                }
                if (java.util.Arrays.equals(candidate.getParameterTypes(),
                        m.getParameterTypes())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Abstract methods reachable only through an interface.
     *
     * <p>A concrete shim has to implement them or it will not compile, and the
     * superclass walk does not see them: the class may declare the interface
     * without declaring the method.</p>
     */
    private static void collectInterfaceAbstracts(Class<?> target, Class<?> c,
            Map<String, Method> out,
            Map<String, Class<?>> bindings, Set<String> seen) {
        if (c == null) {
            return;
        }
        for (Class<?> i : c.getInterfaces()) {
            for (Method m : declaredMethodsSorted(i)) {
                int mo = m.getModifiers();
                if (Modifier.isStatic(mo) || m.isSynthetic() || m.isBridge()) {
                    continue;
                }
                if (!Modifier.isAbstract(mo)) {
                    continue;   // a default method already has an implementation
                }
                // Not filtered on declaredOnDevice: this is an abstract method
                // of an interface the JDK's copy of the superclass declares and
                // the device's does not -- Closeable.close on InputStream. The
                // shim has to implement it to compile here; there it overrides
                // nothing and costs a method. Its signature still has to be
                // expressible, which is what isUsable enforces.
                //
                // Unless a class in the chain already implements it, in which
                // case implementing it again is not merely redundant but wrong:
                // Writer implements Appendable and narrows the return type to
                // Writer, so a shim declaring Appendable append(CharSequence)
                // does not override Writer's -- it clashes with it.
                if (implementedByAClass(target, m)) {
                    continue;
                }
                if (!isUsable(m.getReturnType())) {
                    continue;
                }
                boolean expressible = true;
                for (Class<?> p : m.getParameterTypes()) {
                    if (!isUsable(p)) {
                        expressible = false;
                        break;
                    }
                }
                if (!expressible) {
                    continue;
                }
                String key = m.getName() + paramDescriptorOf(m);
                String resolvedKey = m.getName()
                        + descriptorOfParams(resolvedParams(m, bindings));
                if (!out.containsKey(key) && !seen.contains(resolvedKey)) {
                    out.put(key, m);
                    seen.add(resolvedKey);
                }
            }
            collectInterfaceAbstracts(target, i, out, bindings, seen);
        }
        collectInterfaceAbstracts(target, c.getSuperclass(), out, bindings, seen);
    }

    /**
     * Maps each type variable in a hierarchy to what the subtype binds it to.
     *
     * <p>{@code CaseInsensitiveOrder implements Comparator<String>} binds
     * {@code Comparator}'s {@code T} to {@code String}, so its
     * {@code compare(T,T)} has to be written {@code compare(String,String)} --
     * the erasure {@code compare(Object,Object)} is a different method and
     * clashes with the real one. Skipping such methods instead was worse: an
     * abstract one then leaves the shim uncompilable, which is what put half
     * the generic API on the unshimmable list.</p>
     *
     * <p>A variable with no binding -- the target's own, as in
     * {@code MutableStack<T>} extended raw -- resolves to its erasure, which is
     * what a raw supertype gives you anyway.</p>
     */
    private static Map<String, Class<?>> typeBindings(Class<?> target) {
        Map<String, Class<?>> out = new LinkedHashMap<String, Class<?>>();
        if (target.getTypeParameters().length > 0) {
            // The shim extends this raw -- it has no type arguments to supply --
            // and a raw supertype erases every inherited member, binding or no
            // binding. BooleanProperty<K> extends Property<Boolean,K>, yet
            // through the raw type super.get() is Object, not Boolean. So no
            // substitution applies here; erasures are the truth.
            return out;
        }
        collectBindings(target, out);
        return out;
    }

    private static void collectBindings(java.lang.reflect.Type t, Map<String, Class<?>> out) {
        if (t instanceof Class) {
            Class<?> c = (Class<?>) t;
            if (c.getGenericSuperclass() != null) {
                collectBindings(c.getGenericSuperclass(), out);
            }
            for (java.lang.reflect.Type i : c.getGenericInterfaces()) {
                collectBindings(i, out);
            }
            return;
        }
        if (!(t instanceof java.lang.reflect.ParameterizedType)) {
            return;
        }
        java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) t;
        Class<?> raw = (Class<?>) pt.getRawType();
        java.lang.reflect.TypeVariable<?>[] vars = raw.getTypeParameters();
        java.lang.reflect.Type[] args = pt.getActualTypeArguments();
        for (int i = 0; i < vars.length && i < args.length; i++) {
            // Resolve through what is already known: descending from
            // Set<Component> into Collection<E>, that E is Set's variable and
            // only the bindings collected so far say it means Component.
            // Erasing it in place gave Object, so Collection.add(E) came out as
            // add(Object) and clashed with the class's own add(Component).
            Class<?> bound = erase(substitute(args[i], out));
            if (bound != null) {
                // Keyed by declaring class so two supertypes may both use "T".
                out.put(raw.getName() + "#" + vars[i].getName(), bound);
            }
        }
        collectBindings(raw, out);
    }

    /** A type variable replaced by whatever an outer binding already fixed it to. */
    private static java.lang.reflect.Type substitute(java.lang.reflect.Type t,
                                                     Map<String, Class<?>> known) {
        if (!(t instanceof java.lang.reflect.TypeVariable)) {
            return t;
        }
        java.lang.reflect.TypeVariable<?> v = (java.lang.reflect.TypeVariable<?>) t;
        Object owner = v.getGenericDeclaration();
        if (owner instanceof Class) {
            Class<?> bound = known.get(((Class<?>) owner).getName() + "#" + v.getName());
            if (bound != null) {
                return bound;
            }
        }
        return t;
    }

    /** The class a generic type erases to, or null if it cannot be named. */
    private static Class<?> erase(java.lang.reflect.Type t) {
        if (t instanceof Class) {
            return (Class<?>) t;
        }
        if (t instanceof java.lang.reflect.ParameterizedType) {
            return erase(((java.lang.reflect.ParameterizedType) t).getRawType());
        }
        if (t instanceof java.lang.reflect.TypeVariable) {
            java.lang.reflect.Type[] bounds =
                    ((java.lang.reflect.TypeVariable<?>) t).getBounds();
            return bounds.length > 0 ? erase(bounds[0]) : Object.class;
        }
        if (t instanceof java.lang.reflect.WildcardType) {
            java.lang.reflect.Type[] upper =
                    ((java.lang.reflect.WildcardType) t).getUpperBounds();
            return upper.length > 0 ? erase(upper[0]) : Object.class;
        }
        return null;
    }

    /**
     * The type to write for one position of a method's signature, with the
     * declaring class's variables resolved against what the shim's target
     * binds them to.
     */
    /** A method's parameter types with the declaring class's variables resolved. */
    private static Class<?>[] resolvedParams(Method m, Map<String, Class<?>> bindings) {
        Class<?>[] erased = m.getParameterTypes();
        java.lang.reflect.Type[] generic = m.getGenericParameterTypes();
        Class<?>[] out = new Class<?>[erased.length];
        for (int i = 0; i < erased.length; i++) {
            out[i] = i < generic.length
                    ? resolved(generic[i], erased[i], m, bindings)
                    : erased[i];
        }
        return out;
    }

    private static Class<?> resolved(java.lang.reflect.Type generic, Class<?> erased,
                                     Method m, Map<String, Class<?>> bindings) {
        if (!(generic instanceof java.lang.reflect.TypeVariable)) {
            return erased;
        }
        String key = m.getDeclaringClass().getName() + "#"
                + ((java.lang.reflect.TypeVariable<?>) generic).getName();
        Class<?> bound = bindings.get(key);
        return bound != null ? bound : erased;
    }

    /**
     * Whether a method can be overridden by generated code at all.
     *
     * <p>Excludes anything whose signature mentions a type variable. Reflection
     * reports the erasure -- {@code AsyncResource.get(T)} arrives as
     * {@code get(Object)} -- while the compiler sees the specialised view
     * through {@code BleScan extends AsyncResource&lt;Boolean&gt;}, and an
     * override written against the erasure clashes with it instead of
     * overriding it.</p>
     */
    private static boolean isShimmable(Method m) {
        if (mentionsTypeVariable(m.getGenericReturnType())) {
            return false;
        }
        for (java.lang.reflect.Type t : m.getGenericParameterTypes()) {
            if (mentionsTypeVariable(t)) {
                return false;
            }
        }
        return true;
    }

    private static boolean mentionsTypeVariable(java.lang.reflect.Type t) {
        if (t instanceof java.lang.reflect.TypeVariable) {
            return true;
        }
        if (t instanceof java.lang.reflect.GenericArrayType) {
            return mentionsTypeVariable(
                    ((java.lang.reflect.GenericArrayType) t).getGenericComponentType());
        }
        if (t instanceof java.lang.reflect.ParameterizedType) {
            for (java.lang.reflect.Type a
                    : ((java.lang.reflect.ParameterizedType) t).getActualTypeArguments()) {
                if (mentionsTypeVariable(a)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isUsable(Class<?> c) {
        if (c.isArray()) {
            return isUsable(c.getComponentType());
        }
        if (c.isPrimitive()) {
            return true;
        }
        if (!Modifier.isPublic(c.getModifiers())) {
            return false;
        }
        return onDevice(c);
    }

    /**
     * The device's {@code java.*} types, by name.
     *
     * <p>Read from the {@code codenameone-java-runtime} artifact, which is what
     * the application tool chain actually compiles against -- 355 classes, not
     * the JDK's thousands and not everything under {@code vm/JavaAPI} either.
     * {@code ReentrantLock} exists in the VM's sources but is not on that
     * classpath, and a shim for it fails to resolve.</p>
     */
    private static Set<String> deviceJavaTypes;

    /**
     * {@code class -> {name+descriptor}} for the device's {@code java.*}.
     *
     * <p>Type-level filtering is not enough. The device's {@code InputStream}
     * is a real {@code java.io.InputStream}, but the JDK's has
     * {@code readAllBytes()} and the device's does not -- reflection over the
     * running JDK reports methods that will not exist on the phone, and a shim
     * overriding one fails the bytecode compliance gate. So the jar's own
     * method table decides.</p>
     */
    private static Map<String, Set<String>> deviceJavaMethods;

    /**
     * The same, restricted to methods the device declares {@code final}.
     *
     * <p>Final-ness is skewed too, not just presence: {@code Writer.append} is
     * overridable in the JDK and final on the device, so reflection says
     * "override this" and javac says you may not.</p>
     */
    private static Map<String, Set<String>> deviceFinalMethods;

    /**
     * Whether the device's {@code java.*} subset actually has this type.
     *
     * <p>The framework compiles against a full JDK; the device does not.
     * {@code Reader.read(java.nio.CharBuffer)} is a real method of a real
     * framework class and referencing it from a shim fails the bytecode
     * compliance gate, because ParparVM has no {@code java.nio.CharBuffer}.
     * Asking the subset directly is the general rule -- the alternative is
     * discovering each absent type one build at a time.</p>
     */
    private static boolean onDevice(Class<?> c) {
        if (deviceJavaTypes == null) {
            return true;   // no runtime supplied; assume the caller knows
        }
        String n = c.getName();
        if (!n.startsWith("java.") && !n.startsWith("javax.")) {
            return true;   // framework and app types are not the subset's business
        }
        return deviceJavaTypes.contains(n);
    }

    /**
     * {@code toString}, {@code hashCode} and {@code equals}, routed to the
     * interpreted class.
     *
     * <p>They are not reached by the ordinary walk, which stops below
     * {@code Object}, and nothing in the framework declares them either -- so
     * without this a shim keeps Object's versions. The effect is visible
     * immediately: a list of interpreted objects prints as
     * {@code Interp_I_java_lang_Comparable@df828bb} rather than by the class's
     * own {@code toString}, and an interpreted {@code equals} is ignored by
     * every collection that relies on it.</p>
     *
     * <p>Skipped where the class already provides one, which is what the key
     * set is for -- emitting it twice would not compile.</p>
     */
    private static void emitObjectMethods(PrintWriter w, Set<String> alreadyEmitted,
                                          Class<?> target) {
        if (!alreadyEmitted.contains("toString()") && !sealed(target, "toString")) {
            w.println("    @Override");
            w.println("    public String toString() {");
            w.println("        Object $r = $runtime == null ? InterpRuntime.NOT_OVERRIDDEN");
            w.println("                : $runtime.dispatch($interp, \"toString\", "
                    + "\"()Ljava/lang/String;\", new Object[]{});");
            w.println("        if (" + MISS + ") {");
            w.println("            return super.toString();");
            w.println("        }");
            w.println("        return (String)$r;");
            w.println("    }");
            w.println();
        }
        if (!alreadyEmitted.contains("hashCode()") && !sealed(target, "hashCode")) {
            w.println("    @Override");
            w.println("    public int hashCode() {");
            w.println("        Object $r = $runtime == null ? InterpRuntime.NOT_OVERRIDDEN");
            w.println("                : $runtime.dispatch($interp, \"hashCode\", \"()I\", "
                    + "new Object[]{});");
            w.println("        if (" + MISS + ") {");
            w.println("            return super.hashCode();");
            w.println("        }");
            w.println("        return $r == null ? 0 : ((Number)$r).intValue();");
            w.println("    }");
            w.println();
        }
        if (!alreadyEmitted.contains("equals(Ljava/lang/Object;)")
                && !sealed(target, "equals")) {
            w.println("    @Override");
            w.println("    public boolean equals(Object a0) {");
            w.println("        Object $r = $runtime == null ? InterpRuntime.NOT_OVERRIDDEN");
            w.println("                : $runtime.dispatch($interp, \"equals\", "
                    + "\"(Ljava/lang/Object;)Z\", new Object[]{a0});");
            w.println("        if (" + MISS + ") {");
            w.println("            return super.equals(a0);");
            w.println("        }");
            w.println("        return $r != null && ((Boolean)$r).booleanValue();");
            w.println("    }");
            w.println();
        }
    }

    /**
     * Whether some class in the chain declares this Object method final.
     *
     * <p>{@code Vec2.toString} and {@code BluetoothDevice.equals} are final, and
     * a shim that redeclares them does not compile. Final on the device counts
     * too, for the same reason it does anywhere else here.</p>
     */
    private static boolean sealed(Class<?> target, String name) {
        for (Class<?> k = target; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Method m : declaredMethodsSorted(k)) {
                if (!m.getName().equals(name) || m.getParameterTypes().length > 1) {
                    continue;
                }
                if (Modifier.isFinal(m.getModifiers()) || finalOnDevice(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void emitOverride(PrintWriter w, Method m, Map<String, Class<?>> bindings) {
        Class<?>[] params = resolvedParams(m, bindings);
        Class<?> ret = resolved(m.getGenericReturnType(), m.getReturnType(), m, bindings);
        StringBuilder sig = new StringBuilder();
        StringBuilder call = new StringBuilder();
        StringBuilder boxed = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sig.append(", ");
                call.append(", ");
                boxed.append(", ");
            }
            sig.append(typeName(params[i])).append(" a").append(i);
            call.append("a").append(i);
            boxed.append("a").append(i);
        }

        String visibility = Modifier.isPublic(m.getModifiers()) ? "public" : "protected";
        // An abstract method has no implementation to fall back to. The shim
        // still has to declare it -- it is a concrete class -- but "not
        // overridden" is then a program that would not have compiled, so it
        // reports that rather than calling a super that does not exist.
        boolean abstractMethod = Modifier.isAbstract(m.getModifiers());
        // @Override is a real check -- it is what catches a generic resolution
        // that quietly produced a signature overriding nothing -- so it is kept
        // wherever it can be. It is omitted for exactly the methods the device
        // does not declare, because there the annotation would be a lie and
        // javac would reject it.
        if (declaredOnDevice(m)) {
            w.println("    @Override");
        }
        w.println("    " + visibility + " " + typeName(ret) + " " + m.getName()
                + "(" + sig + ")" + throwsClause(m) + " {");
        // $runtime is null while the framework superclass constructor is still
        // running: Java assigns a subclass's fields only after super() returns,
        // and Form's constructor calls overridable methods. Deferring to super
        // in that window is not a workaround -- the interpreted object genuinely
        // has no state yet, so the base behaviour is the correct one.
        emitDispatch(w, m, "$runtime == null ? InterpRuntime.NOT_OVERRIDDEN\n"
                + "                : $runtime.dispatch($interp, \"" + m.getName() + "\", \""
                + descriptorOf(params, ret) + "\", new Object[]{" + boxed + "})");
        if (abstractMethod) {
            // See zero(): a callback for a program that has been stopped.
            w.println("        if ($r == InterpRuntime.DETACHED) {");
            w.println(ret == Void.TYPE ? "            return;"
                    : "            return " + zero(ret) + ";");
            w.println("        }");
        }
        w.println("        if (" + (abstractMethod ? "$r == InterpRuntime.NOT_OVERRIDDEN" : MISS)
                + ") {");
        if (abstractMethod) {
            w.println("            throw new AbstractMethodError(\"" + typeName(m.getDeclaringClass())
                    + "." + m.getName() + "\");");
            w.println("        }");
        } else if (ret == Void.TYPE) {
            w.println("            super." + m.getName() + "(" + call + ");");
            w.println("            return;");
            w.println("        }");
        } else {
            w.println("            return super." + m.getName() + "(" + call + ");");
            w.println("        }");
        }
        if (ret != Void.TYPE) {
            w.println("        return " + unbox(ret, "$r") + ";");
        }
        w.println("    }");
        w.println();
        // A super_ bridge is the only way interpreted code can reach super --
        // and only exists where there is a super to reach.
        if (!abstractMethod) {
            w.println("    public " + typeName(ret) + " super_" + m.getName() + "(" + sig + ")"
                    + throwsClause(m) + " {");
            if (ret == Void.TYPE) {
                w.println("        super." + m.getName() + "(" + call + ");");
            } else {
                w.println("        return super." + m.getName() + "(" + call + ");");
            }
            w.println("    }");
            w.println();
        }
    }

    /**
     * The test a generated method uses for "the interpreter did not answer".
     *
     * <p>Two sentinels, one branch: NOT_OVERRIDDEN means the pushed class does
     * not provide the method, DETACHED means the program that did has been
     * stopped. Both are handled by doing what the framework class would do on
     * its own, which for anything with a body is calling it.</p>
     */
    private static final String MISS =
            "$r == InterpRuntime.NOT_OVERRIDDEN || $r == InterpRuntime.DETACHED";

    /**
     * The value a method returns when a callback arrives for a program that has
     * been stopped, and there is no implementation to defer to.
     *
     * <p>A timer or a global listener still holds the old peer, and a late
     * callback must not become an AbstractMethodError on the event thread: the
     * program is gone, so the method quietly answers nothing.</p>
     */
    private static String zero(Class<?> t) {
        if (t == Boolean.TYPE) return "false";
        if (t == Byte.TYPE) return "(byte)0";
        if (t == Character.TYPE) return "(char)0";
        if (t == Short.TYPE) return "(short)0";
        if (t == Integer.TYPE) return "0";
        if (t == Long.TYPE) return "0L";
        if (t == Float.TYPE) return "0f";
        if (t == Double.TYPE) return "0d";
        return "null";
    }

    private static String unbox(Class<?> t, String expr) {
        if (t == Boolean.TYPE) return "$r == null ? false : ((Boolean)" + expr + ").booleanValue()";
        if (t == Byte.TYPE) return "$r == null ? (byte)0 : ((Number)" + expr + ").byteValue()";
        if (t == Character.TYPE) return "$r == null ? (char)0 : ((Character)" + expr + ").charValue()";
        if (t == Short.TYPE) return "$r == null ? (short)0 : ((Number)" + expr + ").shortValue()";
        if (t == Integer.TYPE) return "$r == null ? 0 : ((Number)" + expr + ").intValue()";
        if (t == Long.TYPE) return "$r == null ? 0L : ((Number)" + expr + ").longValue()";
        if (t == Float.TYPE) return "$r == null ? 0f : ((Number)" + expr + ").floatValue()";
        if (t == Double.TYPE) return "$r == null ? 0d : ((Number)" + expr + ").doubleValue()";
        return "(" + typeName(t) + ")" + expr;
    }

    private static String typeName(Class<?> c) {
        if (c.isArray()) {
            return typeName(c.getComponentType()) + "[]";
        }
        return c.getName().replace('$', '.');
    }

    /**
     * The {@code throws} clause a peer constructor has to repeat.
     *
     * <p>Its body calls {@code super(...)}, so every checked exception the
     * framework constructor declares propagates. {@code URL(String)} throws
     * {@code URISyntaxException}, which is why its shim would not compile.</p>
     */
    private static String ctorThrows(Constructor<?> c) {
        Class<?>[] ex = c.getExceptionTypes();
        if (ex.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" throws ");
        for (int i = 0; i < ex.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(typeName(ex[i]));
        }
        return sb.toString();
    }

    /**
     * The {@code throws} clause an override has to repeat.
     *
     * <p>Narrowing is legal, dropping a checked exception the body can still
     * raise is not -- and the body here calls {@code super}.</p>
     */
    /**
     * Emits the dispatch call, preserving the checked exceptions the method
     * declares.
     *
     * <p>The interpreter cannot throw a checked exception through its own
     * signature, so it wraps one in an InterpThrowable. The shim, on the other
     * hand, declares exactly what the framework method declares -- so an
     * interpreted implementation of {@code Row.getString()} that throws
     * IOException should reach the caller's {@code catch (IOException)} rather
     * than arriving as an unexpected runtime exception. Each declared type is
     * unwrapped by an instanceof-guarded cast; anything else keeps travelling
     * as it was.</p>
     */
    private static void emitDispatch(PrintWriter w, Method m, String call) {
        Class<?>[] declared = m.getExceptionTypes();
        java.util.List<Class<?>> checked = new java.util.ArrayList<Class<?>>();
        for (Class<?> e : declared) {
            if (!RuntimeException.class.isAssignableFrom(e) && !Error.class.isAssignableFrom(e)) {
                checked.add(e);
            }
        }
        if (checked.isEmpty()) {
            w.println("        Object $r = " + call + ";");
            return;
        }
        w.println("        Object $r;");
        w.println("        try {");
        w.println("            $r = " + call + ";");
        w.println("        } catch (com.codename1.impl.interp.InterpThrowable $t) {");
        // hostThrowable, not getThrown: a pushed exception class arrives as an
        // InterpObject whose peer is the host exception, and only the peer can
        // match a catch clause.
        w.println("            Throwable $thrown = $t.hostThrowable();");
        for (Class<?> e : checked) {
            w.println("            if ($thrown instanceof " + typeName(e) + ") {");
            w.println("                throw (" + typeName(e) + ") $thrown;");
            w.println("            }");
        }
        w.println("            throw $t;");
        w.println("        }");
    }

    private static String throwsClause(Method m) {
        Class<?>[] ex = m.getExceptionTypes();
        if (ex.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" throws ");
        for (int i = 0; i < ex.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(typeName(ex[i]));
        }
        return sb.toString();
    }

    /** Parameter descriptor of an already-resolved signature. */
    private static String descriptorOfParams(Class<?>[] params) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : params) {
            sb.append(descriptorOf(p));
        }
        return sb.append(')').toString();
    }

    /** Just the parameter part of the descriptor, for erasure-signature keys. */
    private static String paramDescriptorOf(Method m) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : m.getParameterTypes()) {
            sb.append(descriptorOf(p));
        }
        return sb.append(')').toString();
    }

    /** The JVM descriptor of a resolved signature. */
    private static String descriptorOf(Class<?>[] params, Class<?> ret) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : params) {
            sb.append(descriptorOf(p));
        }
        return sb.append(')').append(descriptorOf(ret)).toString();
    }

    /**
     * Declared methods in a stable order: by name, then by descriptor.
     *
     * <p>{@code Class.getDeclaredMethods} makes no ordering promise and really
     * does vary between runs of the same JVM. These shims are checked in, so an
     * unstable order turns every regeneration into an 800,000-line diff that
     * hides the change that actually matters. It also settles which of two
     * methods that resolve to one signature is the one emitted, rather than
     * leaving that to whatever order reflection happened to return.</p>
     */
    private static Method[] declaredMethodsSorted(Class<?> c) {
        Method[] all = c.getDeclaredMethods();
        java.util.Arrays.sort(all, new java.util.Comparator<Method>() {
            public int compare(Method a, Method b) {
                int byName = a.getName().compareTo(b.getName());
                return byName != 0 ? byName : descriptorOf(a).compareTo(descriptorOf(b));
            }
        });
        return all;
    }

    /** Declared constructors in a stable order, for the reason above. */
    private static Constructor<?>[] declaredConstructorsSorted(Class<?> c) {
        Constructor<?>[] all = c.getDeclaredConstructors();
        java.util.Arrays.sort(all, new java.util.Comparator<Constructor<?>>() {
            public int compare(Constructor<?> a, Constructor<?> b) {
                return descriptorOfParams(a.getParameterTypes())
                        .compareTo(descriptorOfParams(b.getParameterTypes()));
            }
        });
        return all;
    }

    private static String descriptorOf(Method m) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : m.getParameterTypes()) {
            sb.append(descriptorOf(p));
        }
        return sb.append(')').append(descriptorOf(m.getReturnType())).toString();
    }

    private static String descriptorOf(Class<?> c) {
        if (c == Void.TYPE) return "V";
        if (c == Boolean.TYPE) return "Z";
        if (c == Byte.TYPE) return "B";
        if (c == Character.TYPE) return "C";
        if (c == Short.TYPE) return "S";
        if (c == Integer.TYPE) return "I";
        if (c == Long.TYPE) return "J";
        if (c == Float.TYPE) return "F";
        if (c == Double.TYPE) return "D";
        if (c.isArray()) return c.getName().replace('.', '/');
        return "L" + c.getName().replace('.', '/') + ";";
    }

    /** A registry mapping a framework class to the shim that extends it. */
    /**
     * A registry mapping a framework type to the shim that stands in for it.
     *
     * <p>A hash lookup to an index, then a switch -- not a chain of string
     * comparisons. With the whole API in scope there are hundreds of entries,
     * and every interpreted {@code new} and every host call through a peer goes
     * through here.</p>
     */
    private static void writeRegistry(File dir, List<String> shims, List<Class<?>> classes,
                                      List<String> ifaceShims, List<Class<?>> interfaces)
            throws Exception {
        PrintWriter w = new PrintWriter(new File(dir, "InterpShimRegistry.java"), "UTF-8");
        try {
            header(w);
            w.println("package com.codenameone.devruntime.gen;");
            w.println();
            w.println("import com.codename1.impl.interp.InterpObject;");
            w.println("import com.codename1.impl.interp.InterpRuntime;");
            w.println("import java.util.Hashtable;");
            w.println();
            w.println("/** Maps a framework type to the generated shim that stands in for it. */");
            w.println("public final class InterpShimRegistry {");
            w.println("    private static final Hashtable CLASS_IDS = new Hashtable();");
            w.println("    private static final Hashtable IFACE_IDS = new Hashtable();");
            w.println("    private static final Hashtable PEER_NAMES = new Hashtable();");
            w.println();
            w.println("    static {");
            for (int i = 0; i < classes.size(); i++) {
                w.println("        CLASS_IDS.put(\"" + internal(classes.get(i))
                        + "\", Integer.valueOf(" + i + "));");
            }
            for (int i = 0; i < interfaces.size(); i++) {
                w.println("        IFACE_IDS.put(\"" + internal(interfaces.get(i))
                        + "\", Integer.valueOf(" + i + "));");
            }
            w.println("    }");
            w.println();
            w.println("    private InterpShimRegistry() {");
            w.println("    }");
            w.println();
            w.println("    /** Whether a shim exists for the given JVM internal name. */");
            w.println("    public static boolean canExtend(String internalName) {");
            w.println("        return CLASS_IDS.containsKey(internalName);");
            w.println("    }");
            w.println();
            w.println("    /** Whether a shim exists implementing the given interface. */");
            w.println("    public static boolean canImplement(String internalName) {");
            w.println("        return IFACE_IDS.containsKey(internalName);");
            w.println("    }");
            w.println();
            w.println("    /**");
            w.println("     * Creates the shim for a framework class, choosing the constructor");
            w.println("     * the pushed class chained to.");
            w.println("     */");
            w.println("    public static Object create(String internalName, InterpRuntime rt,");
            w.println("                                InterpObject obj, String descriptor,");
            w.println("                                Object[] args) throws Throwable {");
            w.println("        Integer id = (Integer)CLASS_IDS.get(internalName);");
            w.println("        if (id == null) {");
            w.println("            return null;");
            w.println("        }");
            w.println("        switch (id.intValue()) {");
            for (int i = 0; i < shims.size(); i++) {
                w.println("            case " + i + ": return " + shims.get(i)
                        + ".create(rt, obj, descriptor, args);");
            }
            w.println("            default: return null;");
            w.println("        }");
            w.println("    }");
            w.println();
            w.println("    /** Creates the shim implementing a framework interface. */");
            w.println("    public static Object createInterface(String internalName,");
            w.println("                                         InterpRuntime rt, InterpObject obj) {");
            w.println("        Integer id = (Integer)IFACE_IDS.get(internalName);");
            w.println("        if (id == null) {");
            w.println("            return null;");
            w.println("        }");
            w.println("        switch (id.intValue()) {");
            for (int i = 0; i < ifaceShims.size(); i++) {
                w.println("            case " + i + ": return new " + ifaceShims.get(i)
                        + "(rt, obj);");
            }
            w.println("            default: return null;");
            w.println("        }");
            w.println("    }");
            w.println();
            w.println("    /**");
            w.println("     * The JVM internal name of a shim instance.");
            w.println("     *");
            w.println("     * <p>Registered by the shim itself rather than read from");
            w.println("     * getClass().getName(), which ParparVM derives from the mangled C");
            w.println("     * symbol -- where a package separator and an underscore are the");
            w.println("     * same character, so Interp_ui_Form comes back as Interp/ui/Form.</p>");
            w.println("     */");
            w.println("    public static String nameOf(Object peer) {");
            w.println("        return peer == null ? null");
            w.println("                : (String)PEER_NAMES.get(peer.getClass());");
            w.println("    }");
            w.println();
            w.println("    static {");
            for (int i = 0; i < shims.size(); i++) {
                w.println("        PEER_NAMES.put(" + shims.get(i)
                        + ".class, \"com/codenameone/devruntime/gen/" + shims.get(i) + "\");");
            }
            for (int i = 0; i < ifaceShims.size(); i++) {
                w.println("        PEER_NAMES.put(" + ifaceShims.get(i)
                        + ".class, \"com/codenameone/devruntime/gen/" + ifaceShims.get(i) + "\");");
            }
            w.println("    }");
            w.println("}");
        } finally {
            w.close();
        }
    }

    /** The JVM internal name of a class. */
    private static String internal(Class<?> c) {
        return c.getName().replace('.', '/');
    }

    private static void header(PrintWriter w) {
        w.println("/*");
        w.println(" * Generated by GenerateInterpShims. Do not edit.");
        w.println(" *");
        w.println(" * Lets interpreted code extend a framework class on a platform that cannot");
        w.println(" * define classes at run time. Regenerate after changing the curated list in");
        w.println(" * the generator.");
        w.println(" */");
    }
}
