package com.codename1.dart.transpiler.harness;

import com.codename1.dart.transpiler.api.GeneratedFile;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Compiles the transpiled counter app with the real JDK 17 javac against the
 * dart-runtime and flutter-runtime jars — proves the emitter's output links
 * against the hand-written runtime API (mirrors svg-transcoder's
 * CompileGeneratedSourceTest).
 */
public class CompileGeneratedTest {

    @Test
    public void m2DemoOutputCompilesAgainstRuntimes() throws Exception {
        compileFixture("src/test/resources/fixtures/m2_demo.dart");
    }

    @Test
    public void counterAppOutputCompilesAgainstRuntimes() throws Exception {
        compileFixture("src/test/resources/fixtures/counter_main.dart");
    }

    private void compileFixture(String fixture) throws Exception {
        File java17 = TestSupport.java17Home();
        File dartRuntime = TestSupport.findJar("codenameone-dart-runtime");
        File flutterRuntime = TestSupport.findJar("codenameone-flutter-runtime");
        File core = TestSupport.findJar("codenameone-core");
        assumeTrue(java17 != null, "JAVA17_HOME not set — skipping compile check");
        assumeTrue(dartRuntime != null && flutterRuntime != null && core != null,
                "runtime jars not built — skipping compile check");

        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"main.dart", TestSupport.read(new File(fixture))}
        });
        assertTrue(!r.diags.hasErrors(), "diagnostics: " + r.diags.asList());

        File work = Files.createTempDirectory("dart-compile-check").toFile();
        File srcDir = new File(work, TestSupport.PKG.replace('.', '/'));
        List<String> args = new ArrayList<String>();
        for (GeneratedFile gf : r.files) {
            File out = new File(srcDir, gf.relativePath);
            TestSupport.write(out, gf.content);
            args.add(out.getAbsolutePath());
        }
        File classes = new File(work, "classes");
        classes.mkdirs();
        List<String> javac = new ArrayList<String>();
        javac.add(new File(java17, "bin/javac").getAbsolutePath());
        javac.add("-cp");
        javac.add(dartRuntime.getAbsolutePath() + File.pathSeparator
                + flutterRuntime.getAbsolutePath() + File.pathSeparator + core.getAbsolutePath());
        javac.add("-d");
        javac.add(classes.getAbsolutePath());
        javac.addAll(args);
        Object[] result = TestSupport.run(javac, work);
        assertEquals(0, result[0], "generated counter app failed to compile:\n" + result[1]);
    }
}
