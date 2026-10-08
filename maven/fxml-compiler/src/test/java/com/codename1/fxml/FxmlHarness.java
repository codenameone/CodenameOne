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
package com.codename1.fxml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import com.codename1.compat.jdk.ResourceNames;
import com.codename1.compat.jdk.Resources;
import com.codename1.fxcompat.runtime.FxmlDispatch;

import javafx.fxml.FXMLLoader;

/// What a build does to an application's FXML and style sheets, in a
/// test: the resource compiler, javac over the generated sources and the
/// application's own, the dispatch generator, and a class loader the
/// result runs from.
///
/// The steps and their order are the ones of a real build. Nothing here
/// stands in for a step: the controllers of a test are reached through the
/// generated dispatcher and by no other route.
final class FxmlHarness {

    /// The registry a test generates. Not the layer's own name, which the
    /// test class path already holds as a placeholder.
    static final String REGISTRY = "fxmltest/Registry";

    private final File res;
    private final File src;
    private final File gen;
    private final File out;
    private final File classes;
    private final File fxmlSources;
    private final List<String> sources = new ArrayList<String>();
    final List<String> warnings = new ArrayList<String>();
    final List<String> infos = new ArrayList<String>();
    private URLClassLoader loader;
    private FxmlDispatchGenerator generator;

    FxmlHarness(String name) throws IOException {
        File work = new File("target/fxml-test-work/" + name).getAbsoluteFile();
        delete(work);
        res = new File(work, "res");
        src = new File(work, "src");
        gen = new File(work, "gen");
        out = new File(work, "out");
        classes = new File(work, "classes");
        fxmlSources = new File(work, "fxml");
        for (File dir : new File[] {res, src, gen, out, classes}) {
            if (!dir.mkdirs()) {
                throw new IOException("Cannot create " + dir);
            }
        }
    }

    private static void delete(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File child : children) {
                delete(child);
            }
        }
        f.delete();
    }

    private static void write(File f, String text) throws IOException {
        f.getParentFile().mkdirs();
        OutputStream o = new FileOutputStream(f);
        try {
            o.write(text.getBytes(StandardCharsets.UTF_8));
        } finally {
            o.close();
        }
    }

    /// Adds a resource of the application: an FXML document, a style
    /// sheet, or a file one of them names.
    FxmlHarness resource(String path, String text) throws IOException {
        write(new File(res, path), text);
        return this;
    }

    /// The file of a resource, as messages name it.
    String file(String path) {
        return new File(res, path).getPath();
    }

    /// Adds a class of the application.
    FxmlHarness source(String className, String text) throws IOException {
        File f = new File(src, className.replace('.', '/') + ".java");
        write(f, text);
        sources.add(f.getPath());
        return this;
    }

    /// The class path of the test, as the entries the layer, the core and
    /// the shims were loaded from.
    static List<File> classpath() {
        List<File> path = new ArrayList<File>();
        Class<?>[] of = {javafx.scene.Node.class, com.codename1.ui.Display.class, Resources.class};
        for (Class<?> c : of) {
            try {
                File f = new File(c.getProtectionDomain().getCodeSource().getLocation().toURI());
                if (!path.contains(f)) {
                    path.add(f);
                }
            } catch (java.net.URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return path;
    }

    private DesktopResourceCompiler.Log log() {
        return new DesktopResourceCompiler.Log() {
            @Override
            public void info(String message) {
                infos.add(message);
            }

            @Override
            public void warn(String message) {
                warnings.add(message);
            }
        };
    }

    /// Runs the compilers in the order of a build and answers their errors:
    /// the style sheets and the source that stands for the documents, javac
    /// over that and the application's sources, then the documents.
    List<String> compile() throws IOException {
        List<String> errors = new DesktopResourceCompiler(java.util.Collections.singletonList(res), gen, out, log())
                .run();
        if (!errors.isEmpty()) {
            return errors;
        }
        List<String> files = new ArrayList<String>(sources);
        collect(gen, ".java", files);
        if (!files.isEmpty()) {
            StringBuilder cp = new StringBuilder();
            for (File f : classpath()) {
                cp.append(cp.length() == 0 ? "" : File.pathSeparator).append(f.getPath());
            }
            List<String> args = new ArrayList<String>();
            args.add("-nowarn");
            args.add("-encoding");
            args.add("UTF-8");
            args.add("-classpath");
            args.add(cp.toString());
            args.add("-d");
            args.add(classes.getPath());
            args.addAll(files);
            JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            int rc = javac.run(null, null, err, args.toArray(new String[0]));
            if (rc != 0) {
                throw new AssertionError("javac failed:\n" + new String(err.toByteArray(), StandardCharsets.UTF_8));
            }
        }
        return new FxmlClassCompiler(java.util.Collections.singletonList(res), classpath(),
                java.util.Collections.<File>emptyList(), classes, fxmlSources, log()).run();
    }

    /// The generated source of a document.
    String generated(String path) throws IOException {
        File f = new File(fxmlSources, FxmlCompiler.PACKAGE.replace('.', '/') + "/" + FxmlCompiler.className(path)
                + ".java");
        return new String(read(f), StandardCharsets.UTF_8);
    }

    /// The compiled form of a style sheet, as the build wrote it.
    File compiledSheet(String path) {
        return new File(out, ResourceNames.flatName(path) + ".cn1css");
    }

    private static byte[] read(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) > 0) {
                o.write(buffer, 0, n);
            }
            return o.toByteArray();
        } finally {
            in.close();
        }
    }

    private static void collect(File dir, String suffix, List<String> into) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, suffix, into);
            } else if (child.getName().endsWith(suffix)) {
                into.add(child.getPath());
            }
        }
    }

    private static void index(File dir, String prefix, Map<String, File> into) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                index(child, prefix + child.getName() + "/", into);
            } else {
                into.put(ResourceNames.flatName(prefix + child.getName()), child);
            }
        }
    }

    /// The whole build: compiles the resources, then the sources, then
    /// generates the dispatcher and makes it the one the layer asks.
    FxmlHarness build() throws Exception {
        List<String> errors = compile();
        if (!errors.isEmpty()) {
            throw new AssertionError("The resources do not compile: " + errors);
        }
        generator =new FxmlDispatchGenerator(classes, FxmlDispatchGenerator.unrelocated(REGISTRY));
        generator.run(FxmlDispatchGenerator.classesIn(classes));
        loader = new URLClassLoader(new URL[] {classes.toURI().toURL()}, FxmlHarness.class.getClassLoader());
        FxmlDispatch.install((FxmlDispatch) loader.loadClass(REGISTRY.replace('/', '.')).newInstance());
        final Map<String, File> shipped = new HashMap<String, File>();
        index(res, "", shipped);
        index(out, "", shipped);
        Resources.cn1SetProvider(new Resources.Provider() {
            @Override
            public InputStream open(String flatName) throws IOException {
                File f = shipped.get(flatName);
                return f == null ? null : new FileInputStream(f);
            }
        });
        return this;
    }

    /// The generator of the last build.
    FxmlDispatchGenerator generator() {
        return generator;
    }

    /// The directory of the compiled classes.
    File classes() {
        return classes;
    }

    /// A class of the built application.
    Class<?> type(String name) throws ClassNotFoundException {
        return loader.loadClass(name);
    }

    /// The location of a document, as an application would hand it to a
    /// loader. The directories before the resource path are the kind a
    /// desktop URL carries and a device knows nothing about.
    static URL location(String path) throws IOException {
        return new URL("file:/work/app/target/classes/" + path);
    }

    /// A loader for a document.
    FXMLLoader loader(String path) throws IOException {
        return new FXMLLoader(location(path));
    }

    /// Loads a document.
    <T> T load(String path) throws IOException {
        return loader(path).<T>load();
    }

    /// Reads a field of an object of the built application. The test is
    /// not the layer: it may use reflection to look at what the generated
    /// code did.
    static Object field(Object target, String name) throws Exception {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    /// Undoes what [#build()] installed.
    void close() throws IOException {
        FxmlDispatch.install(null);
        Resources.cn1SetProvider(null);
        if (loader != null) {
            loader.close();
        }
    }
}
