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
        PrintStream ps = new PrintStream(baos);

        compiler.dumpProgram(ps);

        String output = baos.toString();
        Assertions.assertTrue(output.contains("OP_STAR"));
        Assertions.assertTrue(output.contains("OP_ATOM"));
    }
}
