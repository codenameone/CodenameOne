package com.codename1.impl;

import com.codename1.impl.javase.tools.ImplementationDecoratorGenerator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies that the generated {@link CodenameOneImplementationDecorator} is in
 * sync with the current {@link CodenameOneImplementation} API. When this test
 * fails, regenerate the decorator:
 *
 * <pre>bash scripts/javase/generate-impl-decorator.sh</pre>
 */
public class DecoratorCoverageTest {
    private static final String REGEN = " - regenerate with: bash scripts/javase/generate-impl-decorator.sh";

    @Test
    public void everyForwardableMethodIsOverridden() {
        List<Method> methods = ImplementationDecoratorGenerator.forwardedMethods(CodenameOneImplementation.class);
        assertTrue(methods.size() > 500, "Unexpectedly few forwardable methods: " + methods.size());
        for (Method m : methods) {
            Method override;
            try {
                override = CodenameOneImplementationDecorator.class.getDeclaredMethod(m.getName(), m.getParameterTypes());
            } catch (NoSuchMethodException ex) {
                fail("CodenameOneImplementationDecorator is missing an override for "
                        + m + REGEN);
                return;
            }
            assertEquals(m.getReturnType(), override.getReturnType(),
                    "Return type mismatch for " + m.getName() + REGEN);
        }
    }

    /**
     * The generator special-cases init/editString to forward through the final
     * initImpl/editStringImpl methods so the delegate's private state is set.
     * If core changes the shape of those final methods, the special-casing
     * assumptions need to be revisited (see ImplementationDecoratorGenerator).
     */
    @Test
    public void specialCasedFinalMethodsStillExist() throws Exception {
        Method initImpl = CodenameOneImplementation.class.getMethod("initImpl", Object.class);
        assertTrue(Modifier.isFinal(initImpl.getModifiers()), "initImpl is no longer final" + REGEN);

        Method editStringImpl = CodenameOneImplementation.class.getMethod("editStringImpl",
                Class.forName("com.codename1.ui.Component"), int.class, int.class, String.class, int.class);
        assertTrue(Modifier.isFinal(editStringImpl.getModifiers()), "editStringImpl is no longer final" + REGEN);
    }

    /**
     * Byte-for-byte comparison of the checked-in generated source against a
     * fresh generation, catching any drift the reflective checks cannot see.
     * Runs only when the source tree is present (it is during the normal
     * maven/javase build, whose working directory is the module basedir).
     */
    @Test
    public void generatedSourceMatchesGenerator() throws Exception {
        File src = new File("../../Ports/JavaSE/src/com/codename1/impl/CodenameOneImplementationDecorator.java");
        if (!src.exists()) {
            // building outside the repo layout; the reflective checks above still ran
            return;
        }
        String expected = invokeGenerate();
        byte[] data = new byte[(int) src.length()];
        InputStream in = new FileInputStream(src);
        try {
            int off = 0;
            while (off < data.length) {
                int r = in.read(data, off, data.length - off);
                if (r < 0) {
                    break;
                }
                off += r;
            }
        } finally {
            in.close();
        }
        String actual = new String(data, StandardCharsets.UTF_8);
        assertEquals(expected, actual, "Checked-in CodenameOneImplementationDecorator.java is stale" + REGEN);
    }

    private String invokeGenerate() throws Exception {
        Method generate = ImplementationDecoratorGenerator.class.getDeclaredMethod("generate", Class.class);
        generate.setAccessible(true);
        Object result = generate.invoke(null, CodenameOneImplementation.class);
        assertNotNull(result);
        return (String) result;
    }
}
