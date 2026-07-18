package com.codename1.dart.transpiler.harness;

import com.codename1.dart.transpiler.api.GeneratedFile;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Behavioral execution tests: each dir under src/test/resources/behavior/
 * holds main.dart + expect.txt. The dart is transpiled, compiled with the
 * JDK 17 javac (JAVA17_HOME), executed, and stdout diffed against
 * expect.txt. Pins semantics: int math, double formatting, map ordering,
 * closure capture.
 *
 * <p>Skipped when JAVA17_HOME or the dart-runtime jar is unavailable.</p>
 */
public class BehaviorTest {

    private static final File BEHAVIOR_ROOT = new File("src/test/resources/behavior");

    @TestFactory
    public List<DynamicTest> behaviorCases() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        File[] cases = BEHAVIOR_ROOT.listFiles();
        if (cases != null) {
            for (File dir : cases) {
                if (dir.isDirectory()) {
                    tests.add(DynamicTest.dynamicTest(dir.getName(), () -> runCase(dir)));
                }
            }
        }
        assertTrue(!tests.isEmpty(), "no behavior cases found under " + BEHAVIOR_ROOT.getAbsolutePath());
        return tests;
    }

    private void runCase(File dir) throws Exception {
        File java17 = TestSupport.java17Home();
        File dartRuntime = TestSupport.findJar("codenameone-dart-runtime");
        File core = TestSupport.findJar("codenameone-core");
        assumeTrue(java17 != null, "JAVA17_HOME not set — skipping behavioral execution");
        assumeTrue(dartRuntime != null && core != null, "runtime jars not built — skipping");
        String rtClasspath = dartRuntime.getAbsolutePath() + File.pathSeparator + core.getAbsolutePath();

        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"main.dart", TestSupport.read(new File(dir, "main.dart"))}
        });
        assertTrue(!r.diags.hasErrors(), "diagnostics: " + r.diags.asList());

        File work = Files.createTempDirectory("dart-behavior-" + dir.getName()).toFile();
        File srcDir = new File(work, "src/" + TestSupport.PKG.replace('.', '/'));
        List<String> javacArgs = new ArrayList<String>();
        for (GeneratedFile gf : r.files) {
            File out = new File(srcDir, gf.relativePath);
            TestSupport.write(out, gf.content);
            javacArgs.add(out.getAbsolutePath());
        }
        File runner = new File(work, "src/Runner.java");
        TestSupport.write(runner, "public class Runner {\n"
                + "    public static void main(String[] args) {\n"
                + "        " + TestSupport.PKG + ".MainLib.main$();\n"
                + "    }\n"
                + "}\n");
        javacArgs.add(runner.getAbsolutePath());

        File classes = new File(work, "classes");
        classes.mkdirs();
        List<String> javac = new ArrayList<String>();
        javac.add(new File(java17, "bin/javac").getAbsolutePath());
        javac.add("-cp");
        javac.add(rtClasspath);
        javac.add("-d");
        javac.add(classes.getAbsolutePath());
        javac.addAll(javacArgs);
        Object[] compileResult = TestSupport.run(javac, work);
        assertEquals(0, compileResult[0], "javac failed:\n" + compileResult[1]);

        List<String> java = new ArrayList<String>();
        java.add(new File(java17, "bin/java").getAbsolutePath());
        java.add("-cp");
        java.add(classes.getAbsolutePath() + File.pathSeparator + rtClasspath);
        java.add("Runner");
        Object[] runResult = TestSupport.run(java, work);
        assertEquals(0, runResult[0], "execution failed:\n" + runResult[1]);

        String expected = TestSupport.read(new File(dir, "expect.txt"));
        assertEquals(expected.trim().replace("\r\n", "\n"),
                ((String) runResult[1]).trim().replace("\r\n", "\n"),
                "behavioral output mismatch for " + dir.getName());
    }
}
