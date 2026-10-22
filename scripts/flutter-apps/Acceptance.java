/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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

import com.codename1.build.Log;
import com.codename1.dart.transpiler.api.DartTranspiler;
import com.codename1.dart.transpiler.api.Diagnostic;
import com.codename1.dart.transpiler.api.TranspileRequest;
import com.codename1.dart.transpiler.api.TranspileResult;
import com.codename1.maven.FlutterProjectImporter;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.tools.ToolProvider;

/// Imports a Flutter app exactly as `cn1:import-flutter-project` would, transpiles it
/// with the stubs the runtime publishes, and compiles the generated Java against the
/// runtime classes. An "ordinary" app must pass all three steps with no edits.
///
/// Usage: `java -cp <importer+transpiler classes> Acceptance.java <app> <work> <stubs> <javac-cp>`
/// where `<stubs>` is the flutter-runtime classes or jar and `<javac-cp>` the runtime
/// classpath (core, dart-runtime, flutter-runtime). `run-acceptance.sh` assembles both.
/// Exits 0 only when nothing is uncovered, nothing fails to transpile and javac is clean.
public class Acceptance {
    public static void main(String[] args) throws Exception {
        File app = new File(args[0]);
        File work = new File(args[1]);
        File common = new File(work, "common");
        Log log = (Log) Proxy.newProxyInstance(Acceptance.class.getClassLoader(), new Class<?>[] {Log.class},
                (p, m, a) -> m.getReturnType() == boolean.class ? Boolean.FALSE : null);
        FlutterProjectImporter.Result r = new FlutterProjectImporter(log)
                .importProject(app, common, "com.example", "Main");
        System.out.println("== import " + r.appName + ": " + r.copiedFiles + " app file(s)");
        for (Map.Entry<String, Integer> e : r.transpiledPackages.entrySet()) {
            System.out.println("   transpiled " + e.getKey() + " (" + e.getValue() + " file(s))");
        }
        System.out.println("   covered by the runtime: " + r.covered);
        boolean ok = true;
        for (String u : r.uncovered) {
            System.out.println("   NOT supported: " + u);
            ok = false;
        }
        for (Map.Entry<String, String> e : r.unsupportedPackages.entrySet()) {
            System.out.println("   NOT supported: " + e.getKey() + " -- " + e.getValue());
        }

        File out = new File(work, "generated");
        TranspileResult t = new DartTranspiler().transpile(new TranspileRequest()
                .sourceRoot(new File(common, "src/main/flutter"))
                .stubClasspathEntry(new File(args[2]))
                .javaSourceRoot(new File(common, "src/main/java"))
                .outputDir(out));
        List<Diagnostic> errors = t.errors();
        System.out.println("== transpile: " + t.getGeneratedFiles().size() + " file(s), " + errors.size()
                + " error(s)");
        for (Diagnostic d : errors) {
            System.out.println("   " + d);
        }
        ok &= errors.isEmpty();
        if (errors.isEmpty()) {
            List<String> javac = new ArrayList<>();
            File classes = new File(work, "classes");
            classes.mkdirs();
            javac.add("-nowarn");
            javac.add("-encoding");
            javac.add("UTF-8");
            javac.add("-Xmaxerrs");
            javac.add("200");
            javac.add("-d");
            javac.add(classes.getPath());
            javac.add("-cp");
            javac.add(args[3]);
            try (java.util.stream.Stream<Path> s = Files.walk(out.toPath())) {
                javac.addAll(s.filter(p -> p.toString().endsWith(".java")).map(Path::toString)
                        .collect(Collectors.toList()));
            }
            // The import-generated entry point too: it names the packs whose hooks it
            // installs, and the transpiler copies only the packs something names.
            Path own = new File(common, "src/main/java").toPath();
            if (Files.isDirectory(own)) {
                try (java.util.stream.Stream<Path> s = Files.walk(own)) {
                    javac.addAll(s.filter(p -> p.toString().endsWith(".java")).map(Path::toString)
                            .collect(Collectors.toList()));
                }
            }
            int rc = ToolProvider.getSystemJavaCompiler().run(null, null, null, javac.toArray(new String[0]));
            System.out.println("== javac: " + (rc == 0 ? "clean" : "FAILED"));
            ok &= rc == 0;
        }
        System.out.println(ok ? "ACCEPTED" : "REJECTED");
        System.exit(ok ? 0 : 1);
    }
}
