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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.codename1.compat.jdk.ResourceNames;
import com.codename1.fxml.css.CssSheetData;

/// Compiles the FXML documents and the style sheets among an application's
/// desktop resources; the one entry point both build plugins call, before
/// the application's sources are compiled.
///
/// #### What goes where
///
/// - Every `*.fxml` under a resource directory becomes
///   `com/codename1/generated/fxml/Fxml_<name>.java` under the Java output
///   directory, which the build adds to the sources javac compiles
///   ([FxmlCompiler]).
/// - Every `*.css` becomes `<flat name>.cn1css` at the root of the resource
///   output directory, where `<flat name>` is what
///   `ResourceNames.flatName` gives the sheet's path: `styles/app.css` is
///   compiled to `styles__app.css.cn1css`. The build ships that directory
///   with the application's other resources, and a file at its root keeps
///   its name ([CssCompiler]).
///
/// The path of a file relative to its resource directory is its resource
/// path, the one `getResource` finds it under on a desktop. With several
/// resource directories the first that has a path wins, as on a class path.
///
/// #### Idempotence
///
/// A file is only written when its content changes, and an output of an
/// earlier run whose source is gone is deleted, so a build that changes no
/// document recompiles nothing downstream.
public final class DesktopResourceCompiler {

    /// Where progress and warnings are reported.
    public interface Log {
        /// Something worth a line of build output.
        void info(String message);

        /// Something that was left out of the build's result.
        void warn(String message);
    }

    private final List<File> resourceDirs;
    private final List<File> classpath;
    private final File javaOut;
    private final File resourcesOut;
    private final Log log;
    private int fxmlCount;
    private int cssCount;

    /// Creates a compiler.
    ///
    /// #### Parameters
    ///
    /// - `resourceDirs`: the directories of desktop resources; one that
    ///   does not exist is ignored
    ///
    /// - `classpath`: the application's compile class path, where the
    ///   classes the documents name are read from
    ///
    /// - `javaOut`: the root of the generated Java sources
    ///
    /// - `resourcesOut`: the directory of the compiled style sheets
    ///
    /// - `log`: where to report
    public DesktopResourceCompiler(List<File> resourceDirs, List<File> classpath, File javaOut, File resourcesOut,
            Log log) {
        this.resourceDirs = new ArrayList<File>(resourceDirs);
        this.classpath = new ArrayList<File>(classpath);
        this.javaOut = javaOut;
        this.resourcesOut = resourcesOut;
        this.log = log;
    }

    /// How many documents the last run compiled.
    public int fxmlCount() {
        return fxmlCount;
    }

    /// How many style sheets the last run compiled.
    public int cssCount() {
        return cssCount;
    }

    /// Compiles everything. Warnings go to the log; errors are answered,
    /// each as `file:line:column: message`, and an empty list means the
    /// outputs are complete. With errors the outputs of the documents that
    /// failed are absent.
    public List<String> run() throws IOException {
        Map<String, File> fxml = new TreeMap<String, File>();
        Map<String, File> css = new TreeMap<String, File>();
        for (File dir : resourceDirs) {
            if (dir.isDirectory()) {
                collect(dir, "", fxml, css);
            }
        }
        Messages messages = new Messages();
        compileFxml(fxml, messages);
        compileCss(css, messages);
        for (String warning : messages.warnings()) {
            log.warn(warning);
        }
        fxmlCount = fxml.size();
        cssCount = css.size();
        if (fxmlCount + cssCount > 0 && !messages.hasErrors()) {
            log.info("Compiled " + fxmlCount + " FXML document" + (fxmlCount == 1 ? "" : "s") + " and " + cssCount
                    + " style sheet" + (cssCount == 1 ? "" : "s"));
        }
        return messages.errors();
    }

    private static void collect(File dir, String prefix, Map<String, File> fxml, Map<String, File> css) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files);
        for (File f : files) {
            String name = f.getName();
            if (f.isDirectory()) {
                collect(f, prefix + name + "/", fxml, css);
            } else if (name.endsWith(".fxml") && !fxml.containsKey(prefix + name)) {
                fxml.put(prefix + name, f);
            } else if (name.endsWith(".css") && !css.containsKey(prefix + name)) {
                css.put(prefix + name, f);
            }
        }
    }

    private static byte[] read(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private void compileFxml(Map<String, File> files, Messages messages) throws IOException {
        File dir = new File(javaOut, FxmlCompiler.PACKAGE.replace('.', '/'));
        Set<String> written = new HashSet<String>();
        if (!files.isEmpty()) {
            Map<String, FxmlDocument> documents = new LinkedHashMap<String, FxmlDocument>();
            for (Map.Entry<String, File> e : files.entrySet()) {
                InputStream in = new FileInputStream(e.getValue());
                try {
                    FxmlDocument doc = FxmlDocument.parse(e.getValue().getPath(), e.getKey(), in, messages);
                    if (doc != null) {
                        documents.put(e.getKey(), doc);
                    }
                } finally {
                    in.close();
                }
            }
            ClassModel model = new ClassModel(classpath);
            try {
                FxmlCompiler compiler = new FxmlCompiler(model, documents, messages);
                for (FxmlDocument doc : documents.values()) {
                    String source = compiler.compile(doc);
                    if (source != null) {
                        String name = FxmlCompiler.className(doc.path) + ".java";
                        FxmlDispatchGenerator.writeIfDifferent(new File(dir, name),
                                source.getBytes(StandardCharsets.UTF_8));
                        written.add(name);
                    }
                }
            } finally {
                model.close();
            }
        }
        deleteStale(dir, "Fxml_", ".java", written);
    }

    private void compileCss(Map<String, File> files, Messages messages) throws IOException {
        Set<String> written = new HashSet<String>();
        for (Map.Entry<String, File> e : files.entrySet()) {
            String text = new String(read(e.getValue()), StandardCharsets.UTF_8);
            CssSheetData data = CssCompiler.compile(e.getValue().getPath(), e.getKey(), text, files.keySet(),
                    messages);
            if (data != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                data.write(out);
                String name = CssSheetData.compiledName(ResourceNames.flatName(e.getKey()));
                FxmlDispatchGenerator.writeIfDifferent(new File(resourcesOut, name), out.toByteArray());
                written.add(name);
            }
        }
        deleteStale(resourcesOut, "", CssSheetData.SUFFIX, written);
    }

    private static void deleteStale(File dir, String prefix, String suffix, Set<String> keep) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String name = f.getName();
            if (f.isFile() && name.startsWith(prefix) && name.endsWith(suffix) && !keep.contains(name)
                    && !f.delete()) {
                throw new IOException("Could not delete the stale " + f);
            }
        }
    }
}
