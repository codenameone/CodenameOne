package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.Invoke;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        StringBuilder out = new StringBuilder();
        assertDoesNotThrow(() -> method.appendMethodC(out),
                "SIMD-candidate methods with array access opcodes should pass validation");
        String generated = out.toString();
        assertTrue(generated.contains("cn1_simd_com_example_SimdCarrier_vectorBody__"),
                "SIMD-eligible methods should emit weak SIMD hook declarations");
        assertTrue(generated.contains("#if defined(CN1_ENABLE_SIMD_PRAGMAS) && defined(__clang__)"),
                "SIMD-eligible methods should emit opt-in SIMD pragma guards");
        assertTrue(generated.contains("#pragma clang attribute push(__attribute__((target(\"neon\"))), apply_to=function)"),
                "SIMD-eligible methods should include NEON targeting pragmas under the opt-in guard");
    }

    @Test
    void emitsSimdApiInvokeMarkersForApiCallsAndValueTypes() {
        StringBuilder out = new StringBuilder();
        new Invoke(Opcodes.INVOKESTATIC, "com/codename1/simd/SIMD", "loadU8", "([BI)Lcom/codename1/simd/SIMD$U8x16;", false)
                .appendInstruction(out);
        new Invoke(Opcodes.INVOKESTATIC, "com/codename1/simd/SIMD", "laneU8", "(Lcom/codename1/simd/SIMD$U8x16;I)I", false)
                .appendInstruction(out);
        new Invoke(Opcodes.INVOKESPECIAL, "com/codename1/simd/SIMD$Int4", "<init>", "(IIII)V", false)
                .appendInstruction(out);

        String generated = out.toString();
        int markerCount = generated.split("CN1_SIMD_API_INVOKE:", -1).length - 1;
        assertTrue(markerCount == 3,
                "All SIMD API and SIMD value-type constructor calls should carry translation markers");
    }

}
