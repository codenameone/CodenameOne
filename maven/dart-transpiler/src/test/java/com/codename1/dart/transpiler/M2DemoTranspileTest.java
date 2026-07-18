package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class M2DemoTranspileTest {

    @Test
    public void m2DemoTranspilesWithoutErrors() throws Exception {
        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"main.dart", TestSupport.read(new File("src/test/resources/fixtures/m2_demo.dart"))}
        });
        for (com.codename1.dart.transpiler.api.GeneratedFile f : r.files) {
            if (f.relativePath.equals("_DemoPageState.java")) {
                System.out.println(f.content);
            }
        }
        assertTrue(!r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }
}
