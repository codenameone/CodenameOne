package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class M3DemoTranspileTest {

    @Test
    public void m3DemoTranspilesWithoutErrors() throws Exception {
        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"main.dart", TestSupport.read(new File("src/test/resources/fixtures/m3_demo.dart"))}
        });
        assertTrue(!r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }
}
