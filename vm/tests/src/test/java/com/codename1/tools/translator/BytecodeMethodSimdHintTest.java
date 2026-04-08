package com.codename1.tools.translator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BytecodeMethodSimdHintTest {

    @BeforeEach
    void cleanParser() {
        Parser.cleanup();
    }

    @Test
    void failsForAnnotatedMethodsThatAreNotVectorizationCandidates() {
        BytecodeMethod method = new BytecodeMethod(
                "com_example_SimdCarrier",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "scalarBody",
                "()V",
                null,
                null
        );
        method.setSimdCandidateHint(true);
        method.setSimdWidthHint(16);
        method.setMaxes(1, 0);
        method.addInstruction(Opcodes.RETURN);
        assertThrows(IllegalStateException.class, () -> method.appendMethodC(new StringBuilder()),
                "SIMD-candidate methods with no array access opcodes should fail validation");
    }

    @Test
    void allowsAnnotatedMethodsThatContainArrayAccessOpcodes() {
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
        method.addInstruction(Opcodes.BALOAD);
        method.addInstruction(Opcodes.RETURN);
        assertDoesNotThrow(() -> method.appendMethodC(new StringBuilder()),
                "SIMD-candidate methods with array access opcodes should pass validation");
    }
}
