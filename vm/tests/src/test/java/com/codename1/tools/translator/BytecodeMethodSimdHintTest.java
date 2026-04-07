package com.codename1.tools.translator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BytecodeMethodSimdHintTest {

    @BeforeEach
    void cleanParser() {
        Parser.cleanup();
    }

    @Test
    void emitsSimdWidthHintConstantForCandidateMethods() {
        BytecodeMethod method = new BytecodeMethod(
                "com_example_SimdCarrier",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "vectorBody",
                "()V",
                null,
                null
        );
        method.setSimdCandidateHint(true);
        method.setSimdWidthHint(16);
        method.setMaxes(1, 0);
        method.addInstruction(Opcodes.RETURN);

        StringBuilder out = new StringBuilder();
        method.appendMethodC(out);
        String generated = out.toString();

        assertTrue(generated.contains("__cn1SimdWidthHint = 16"),
                "Generated C should embed SIMD width hint constant for candidate methods");
    }

    @Test
    void doesNotEmitSimdWidthHintConstantWhenHintIsMissingOrInvalid() {
        BytecodeMethod method = new BytecodeMethod(
                "com_example_SimdCarrier",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "scalarBody",
                "()V",
                null,
                null
        );
        method.setSimdCandidateHint(true);
        method.setSimdWidthHint(0);
        method.setMaxes(1, 0);
        method.addInstruction(Opcodes.RETURN);

        StringBuilder out = new StringBuilder();
        method.appendMethodC(out);
        String generated = out.toString();

        assertFalse(generated.contains("__cn1SimdWidthHint"),
                "Generated C should not emit SIMD width hint constant for invalid width values");
    }
}
