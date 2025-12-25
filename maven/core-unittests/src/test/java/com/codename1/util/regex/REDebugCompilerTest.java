package com.codename1.util.regex;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class REDebugCompilerTest extends UITestBase {

    @FormTest
    public void testREDebugCompiler() throws Exception {
        REDebugCompiler compiler = new REDebugCompiler();
        REProgram program = compiler.compile("a*b");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.io.Writer w = com.codename1.io.Util.getWriter(baos);

        compiler.dumpProgram(w);
        w.flush();

        String output = new String(baos.toByteArray(), "UTF-8");
        Assertions.assertTrue(output.contains("OP_STAR"));
        Assertions.assertTrue(output.contains("OP_ATOM"));
    }
}
