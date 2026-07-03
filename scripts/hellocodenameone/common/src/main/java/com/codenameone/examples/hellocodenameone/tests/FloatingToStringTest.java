package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.JSONParser;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Device-side guard for float/double string conversion in the platform runtime.
 * iOS and other ParparVM targets use the native toStringImpl() path, while
 * JavaSE/Android/JavaScript use their own runtime implementations.
 */
public class FloatingToStringTest extends BaseTest {

    @Override
    public boolean runTest() {
        try {
            check("double-min-scientific", Double.toString(10000000.0d), "1.0E7");
            check("double-trimmed-scientific", Double.toString(12500000.0d), "1.25E7");
            check("double-large-scientific", Double.toString(125000000.0d), "1.25E8");
            check("float-min-scientific", Float.toString(10000000.0f), "1.0E7");
            check("float-trimmed-scientific", Float.toString(12500000.0f), "1.25E7");
            check("float-large-scientific", Float.toString(16777216.0f), "1.6777216E7");
            check("string-valueOf-double", String.valueOf(12500000.0d), "1.25E7");
            check("string-valueOf-float", String.valueOf(12500000.0f), "1.25E7");
            check("concat-double", "balance=" + 12500000.0d, "balance=1.25E7");
            check("builder-double", new StringBuilder().append(12500000.0d).toString(), "1.25E7");
            check("buffer-float", new StringBuffer().append(12500000.0f).toString(), "1.25E7");
            check("arrays-double", Arrays.toString(new double[]{12500000.0d}), "[1.25E7]");
            check("arrays-float", Arrays.toString(new float[]{12500000.0f}), "[1.25E7]");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(out);
            ps.print(12500000.0d);
            ps.print('|');
            ps.print(12500000.0f);
            check("printstream", out.toString(), "1.25E7|1.25E7");

            Map<String, Object> json = new HashMap<String, Object>();
            json.put("balance", Double.valueOf(12500000.0d));
            json.put("floatBalance", Float.valueOf(12500000.0f));
            String jsonText = JSONParser.toJson(json);
            assertTrue(jsonText.indexOf("\"balance\":1.25E7") >= 0,
                    "JSON double should use terminated floating string, got " + jsonText);
            assertTrue(jsonText.indexOf("\"floatBalance\":1.25E7") >= 0,
                    "JSON float should use terminated floating string, got " + jsonText);
        } catch (Throwable t) {
            fail("Floating toString test failed: " + t);
            return false;
        }
        done();
        return true;
    }

    private void check(String name, String actual, String expected) {
        assertEqual(expected, actual,
                name + " expected=[" + expected + "] actual=[" + actual + "] len=" + actual.length());
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
