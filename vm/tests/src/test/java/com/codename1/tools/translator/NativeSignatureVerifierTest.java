/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.tools.translator;

import com.codename1.tools.translator.NativeSignatureVerifier.Kind;
import com.codename1.tools.translator.NativeSignatureVerifier.Problem;
import com.codename1.tools.translator.NativeSignatureVerifier.Signature;
import com.codename1.tools.translator.NativeSignatureVerifier.SourceIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the gate that catches a hand-written ParparVM native whose C name or
 * prototype is not the one the translator will call.
 */
class NativeSignatureVerifierTest {
    @TempDir
    Path natives;

    @BeforeEach
    void cleanParser() {
        Parser.cleanup();
    }

    // ---------------------------------------------------------------- mangling

    /**
     * The anti-drift test: what the verifier demands is character for character
     * what the translator itself declares. If the mangling ever changes, this
     * fails rather than the gate quietly blessing spellings nothing links to.
     */
    @Test
    void theRequiredPrototypeIsExactlyWhatTheTranslatorDeclares() throws Exception {
        Path classFile = writeNativeHolder();
        Parser.parse(classFile.toFile());

        ByteCodeClass cls = Parser.getClassObject("com_example_Natives");
        cls.setBaseInterfacesObject(Collections.<ByteCodeClass>emptyList());
        cls.updateAllDependencies();
        cls.generateCCode(Collections.singletonList(cls)); // populates the field tables
        String header = normalize(cls.generateCHeader());

        int checked = 0;
        for (BytecodeMethod m : cls.getMethods()) {
            if (!m.isNative()) {
                continue;
            }
            Signature signature = m.getNativeSignature();
            assertTrue(header.contains(signature.prototype),
                    "the generated header does not declare " + signature.prototype
                            + "\nheader was:\n" + header);
            checked++;
        }
        assertEquals(5, checked, "expected every native method to be covered");
    }

    @Test
    void nonVoidReturnsGetTheRSuffixAndArraysCollapseToOneToken() {
        assertEquals("com_example_Natives_flag___R_boolean",
                signature("flag", "()Z", false).symbol);
        assertEquals("com_example_Natives_noop__",
                signature("noop", "()V", false).symbol);
        // one leading "_" per argument, on top of the "__" that opens the list
        assertEquals("com_example_Natives_scale___int_R_long",
                signature("scale", "(I)J", false).symbol);
        // byte[][] is byte_2ARRAY, never byte_1ARRAY_1ARRAY
        assertEquals("com_example_Natives_blob___byte_2ARRAY",
                signature("blob", "([[B)V", false).symbol);
        assertEquals("com_example_Natives_name___R_java_lang_String",
                signature("name", "()Ljava/lang/String;", false).symbol);
    }

    @Test
    void anInstanceNativeTakesSelfAndAStaticOneDoesNot() {
        assertEquals(Arrays.asList("CODENAME_ONE_THREAD_STATE", "JAVA_OBJECT", "JAVA_INT"),
                signature("scale", "(I)J", false).paramTypes);
        assertEquals(Arrays.asList("CODENAME_ONE_THREAD_STATE", "JAVA_INT"),
                signature("scale", "(I)J", true).paramTypes);
    }

    // ----------------------------------------------------------- missing names

    @Test
    void aNativeWithNoImplementationAtAllIsFatal() throws IOException {
        List<Problem> problems = verify(signature("flag", "()Z", false), "// nothing here\n");

        assertEquals(1, problems.size());
        assertEquals(Kind.MISSING, problems.get(0).kind);
        assertTrue(problems.get(0).fatal);
        assertTrue(problems.get(0).message.contains(
                "JAVA_BOOLEAN com_example_Natives_flag___R_boolean("
                + "CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject)"),
                problems.get(0).message);
    }

    /** The shape that shipped broken: the {@code _R_boolean} suffix left off. */
    @Test
    void aMissingReturnSuffixIsReportedAgainstTheNameThatWasWrittenInstead() throws IOException {
        List<Problem> problems = verify(signature("flag", "()Z", false),
                "JAVA_BOOLEAN com_example_Natives_flag__(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me) {\n    return JAVA_TRUE;\n}\n");

        assertEquals(1, problems.size());
        Problem problem = problems.get(0);
        assertEquals(Kind.MISSING, problem.kind);
        assertTrue(problem.fatal);
        assertTrue(problem.message.contains("Found instead at natives.m:1:"
                + " com_example_Natives_flag__"), problem.message);
    }

    /** The other shape: {@code byte_1ARRAY_1ARRAY} for a {@code byte[][]}. */
    @Test
    void aWronglyMangledArrayDimensionIsReported() throws IOException {
        List<Problem> problems = verify(signature("blob", "([[B)V", false),
                "void com_example_Natives_blob___byte_1ARRAY_1ARRAY(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me, JAVA_OBJECT b) {\n}\n");

        assertEquals(1, problems.size());
        assertEquals(Kind.MISSING, problems.get(0).kind);
        assertTrue(problems.get(0).message.contains("byte_1ARRAY_1ARRAY"),
                problems.get(0).message);
        assertTrue(problems.get(0).message.contains("com_example_Natives_blob___byte_2ARRAY"),
                problems.get(0).message);
    }

    @Test
    void aCorrectImplementationIsAccepted() throws IOException {
        assertEquals(Collections.emptyList(), verify(signature("scale", "(I)J", false),
                "JAVA_LONG com_example_Natives_scale___int_R_long(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me, JAVA_INT n) {\n    return n;\n}\n"));
    }

    /** Some natives spell the thread-state macro out instead of using it. */
    @Test
    void theExpandedThreadStateMacroIsStillTheThreadState() throws IOException {
        assertEquals(Collections.emptyList(), verify(signature("flag", "()Z", false),
                "JAVA_BOOLEAN com_example_Natives_flag___R_boolean("
                + "struct ThreadLocalData* threadStateData, JAVA_OBJECT me) {\n"
                + "    return JAVA_TRUE;\n}\n"));
    }

    /** {@code JAVA_VOID} is a typedef for {@code void}; both spellings are fine. */
    @Test
    void voidAndJavaVoidAreTheSameType() throws IOException {
        assertEquals(Collections.emptyList(), verify(signature("noop", "()V", false),
                "void com_example_Natives_noop__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {\n}\n"));
        assertEquals(Collections.emptyList(), verify(signature("noop", "()V", false),
                "JAVA_VOID com_example_Natives_noop__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {\n}\n"));
    }

    // -------------------------------------------------------- wrong prototypes

    /**
     * The failure the linker cannot see: right name, wrong machine type. This one
     * links and then reads a 64-bit argument out of a 32-bit register.
     */
    @Test
    void aParameterOfTheWrongMachineTypeIsFatal() throws IOException {
        List<Problem> problems = verify(signature("scale", "(J)J", false),
                "JAVA_LONG com_example_Natives_scale___long_R_long(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me, JAVA_INT n) {\n    return n;\n}\n");

        assertEquals(1, problems.size());
        assertEquals(Kind.SIGNATURE, problems.get(0).kind);
        assertTrue(problems.get(0).fatal);
        assertTrue(problems.get(0).message.contains(
                "parameter 3 is JAVA_INT where JAVA_LONG is required"), problems.get(0).message);
    }

    @Test
    void anInstanceNativeThatForgotItsSelfParameterIsFatal() throws IOException {
        List<Problem> problems = verify(signature("scale", "(I)J", false),
                "JAVA_LONG com_example_Natives_scale___int_R_long(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_INT n) {\n    return n;\n}\n");

        assertEquals(1, problems.size());
        assertEquals(Kind.SIGNATURE, problems.get(0).kind);
        assertTrue(problems.get(0).message.contains("takes 2 parameter(s) where 3 are required"),
                problems.get(0).message);
    }

    @Test
    void aReturnTypeOfTheWrongMachineTypeIsFatal() throws IOException {
        List<Problem> problems = verify(signature("scale", "(I)J", false),
                "JAVA_INT com_example_Natives_scale___int_R_long(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me, JAVA_INT n) {\n    return n;\n}\n");

        assertEquals(1, problems.size());
        assertEquals(Kind.SIGNATURE, problems.get(0).kind);
        assertTrue(problems.get(0).message.contains(
                "returns JAVA_INT where JAVA_LONG is required"), problems.get(0).message);
    }

    /**
     * JAVA_BOOLEAN, JAVA_CHAR, JAVA_BYTE, JAVA_SHORT and JAVA_INT are all
     * {@code int}: interchangeable at the ABI, so not worth failing a build over.
     */
    @Test
    void theIntFamilyIsInterchangeable() throws IOException {
        assertEquals(Collections.emptyList(), verify(signature("flag", "()Z", false),
                "JAVA_INT com_example_Natives_flag___R_boolean(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me) {\n    return 1;\n}\n"));
    }

    // ------------------------------------------------------------ the C parser

    @Test
    void aPrototypeInACommentOrAStringIsNotAnImplementation() throws IOException {
        List<Problem> problems = verify(signature("flag", "()Z", false),
                "// JAVA_BOOLEAN com_example_Natives_flag___R_boolean(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me) { }\n"
                + "/* JAVA_BOOLEAN com_example_Natives_flag___R_boolean(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me) { } */\n"
                + "static const char* doc = \"com_example_Natives_flag___R_boolean(x) {\";\n");

        assertEquals(1, problems.size());
        assertEquals(Kind.MISSING, problems.get(0).kind);
    }

    /**
     * A URL inside a string is not a comment, and a {@code /*} inside one does not
     * open a block comment. Getting this wrong blanks out the rest of the file and
     * reports every implementation after it as missing.
     */
    @Test
    void aUrlInsideAStringDoesNotSwallowTheRestOfTheFile() throws IOException {
        assertEquals(Collections.emptyList(), verify(signature("flag", "()Z", false),
                "static NSString* u = @\"https://www.codenameone.com/x\";\n"
                + "static NSString* v = @\"/* not a comment\";\n"
                + "JAVA_BOOLEAN com_example_Natives_flag___R_boolean(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me) {\n    return JAVA_TRUE;\n}\n"));
    }

    @Test
    void aMacroThatLooksLikeADefinitionIsNotOne() throws IOException {
        List<Problem> problems = verify(signature("flag", "()Z", false),
                "#define com_example_Natives_flag___R_boolean(s, me) { return 1; }\n");

        assertEquals(1, problems.size());
        assertEquals(Kind.MISSING, problems.get(0).kind);
    }

    @Test
    void aDeclarationIsNotADefinition() throws IOException {
        List<Problem> problems = verify(signature("flag", "()Z", false),
                "extern JAVA_BOOLEAN com_example_Natives_flag___R_boolean("
                + "CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n");

        assertEquals(1, problems.size());
        assertEquals(Kind.MISSING, problems.get(0).kind);
    }

    @Test
    void aCallSiteIsNotADefinition() throws IOException {
        List<Problem> problems = verify(signature("flag", "()Z", false),
                "void other(void) {\n"
                + "    if (com_example_Natives_flag___R_boolean(threadStateData, me)) {\n"
                + "        return;\n"
                + "    }\n"
                + "}\n");

        assertEquals(1, problems.size());
        assertEquals(Kind.MISSING, problems.get(0).kind);
    }

    /** The #if / #else pair the iOS port uses for watchOS and tvOS stubs. */
    @Test
    void bothArmsOfAConditionalDefinitionAreAccepted() throws IOException {
        assertEquals(Collections.emptyList(), verify(signature("flag", "()Z", false),
                "#if TARGET_OS_WATCH\n"
                + "JAVA_BOOLEAN com_example_Natives_flag___R_boolean(CN1_THREAD_STATE_MULTI_ARG"
                + " JAVA_OBJECT me) {\n    return JAVA_FALSE;\n}\n"
                + "#else\n"
                + "JAVA_BOOLEAN com_example_Natives_flag___R_boolean(CN1_THREAD_STATE_MULTI_ARG"
                + " JAVA_OBJECT me) {\n    return JAVA_TRUE;\n}\n"
                + "#endif\n"));
    }

    /**
     * EVERY arm has to match, not just one. They are the same C symbol under
     * mutually exclusive branches, so a build that selects the bad arm links by
     * name and then reads its arguments wrong -- exactly what this gate exists to
     * stop, and invisible if one good arm were enough.
     */
    @Test
    void oneBadArmOfAConditionalDefinitionIsStillFatal() throws IOException {
        List<Problem> problems = verify(signature("flag", "()Z", false),
                "#if TARGET_OS_WATCH\n"
                + "JAVA_BOOLEAN com_example_Natives_flag___R_boolean(CN1_THREAD_STATE_MULTI_ARG"
                + " JAVA_OBJECT me) {\n    return JAVA_FALSE;\n}\n"
                + "#else\n"
                // the watch arm above is right; this one forgot the thread state
                + "JAVA_BOOLEAN com_example_Natives_flag___R_boolean(JAVA_OBJECT me) {\n"
                + "    return JAVA_TRUE;\n}\n"
                + "#endif\n");

        assertEquals(1, problems.size());
        assertEquals(Kind.SIGNATURE, problems.get(0).kind);
        assertTrue(problems.get(0).fatal);
        // and it points at the offending arm, not merely the first definition
        assertTrue(problems.get(0).message.startsWith("natives.m:6:"), problems.get(0).message);
    }

    /**
     * {@code JAVA_LONG} is {@code long long}; a native that spells the underlying
     * type is ABI-correct and must not be rejected. Reading only the token nearest
     * the name would see {@code long} and call it a mismatch.
     */
    @Test
    void aMultiwordReturnTypeIsReadWhole() throws IOException {
        assertEquals(Collections.emptyList(), verify(signature("scale", "(I)J", false),
                "long long com_example_Natives_scale___int_R_long(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me, JAVA_INT n) {\n    return n;\n}\n"));
        assertEquals(Collections.emptyList(), verify(signature("scale", "(I)I", false),
                "unsigned int com_example_Natives_scale___int_R_int(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me, JAVA_INT n) {\n    return n;\n}\n"));
    }

    /**
     * ...but reading leftwards must stop at the type. Nothing separates a return
     * type from what precedes the declaration, and an Objective-C {@code @end} on
     * the line above was being swallowed into it, turning every implementation that
     * follows a class into a bogus mismatch.
     */
    @Test
    void whatPrecedesTheDeclarationIsNotPartOfTheReturnType() throws IOException {
        assertEquals(Collections.emptyList(), verify(signature("flag", "()Z", false),
                "@interface Foo : NSObject\n@end\n"
                + "JAVA_BOOLEAN com_example_Natives_flag___R_boolean(CN1_THREAD_STATE_MULTI_ARG"
                + " JAVA_OBJECT me) {\n    return JAVA_TRUE;\n}\n"));
        assertEquals(Collections.emptyList(), verify(signature("noop", "()V", false),
                "static NSString* CN1_TAG = @\"x\";\n"
                + "static void com_example_Natives_noop__(CN1_THREAD_STATE_MULTI_ARG"
                + " JAVA_OBJECT me) {\n}\n"));
    }

    // --------------------------------------------------------- near-miss policy

    /**
     * A near miss whose correct symbol is also present is dead code, not a broken
     * build -- unless other native code calls it, which is the shape the iOS port
     * uses everywhere: a thin suffixed wrapper over the real implementation.
     */
    @Test
    void aNearMissThatOtherNativeCodeCallsIsNotReported() throws IOException {
        assertEquals(Collections.emptyList(), verify(signature("flag", "()Z", false),
                "JAVA_BOOLEAN com_example_Natives_flag__(CN1_THREAD_STATE_MULTI_ARG"
                + " JAVA_OBJECT me) {\n    return JAVA_TRUE;\n}\n"
                + "JAVA_BOOLEAN com_example_Natives_flag___R_boolean(CN1_THREAD_STATE_MULTI_ARG"
                + " JAVA_OBJECT me) {\n"
                + "    return com_example_Natives_flag__(CN1_THREAD_STATE_PASS_ARG me);\n}\n"));
    }

    @Test
    void aNearMissNothingCallsIsAWarningNotAnError() throws IOException {
        List<Problem> problems = verify(signature("flag", "()Z", false),
                "JAVA_BOOLEAN com_example_Natives_flag__(CN1_THREAD_STATE_MULTI_ARG"
                + " JAVA_OBJECT me) {\n    return JAVA_FALSE;\n}\n"
                + "JAVA_BOOLEAN com_example_Natives_flag___R_boolean(CN1_THREAD_STATE_MULTI_ARG"
                + " JAVA_OBJECT me) {\n    return JAVA_TRUE;\n}\n");

        assertEquals(1, problems.size());
        assertEquals(Kind.ORPHAN, problems.get(0).kind);
        assertFalse(problems.get(0).fatal, "dead code must not fail the build");
    }

    /** Overloads share a prefix; each still has to be spelled its own way. */
    @Test
    void anOverloadIsNotMistakenForAnotherOverloadsImplementation() throws IOException {
        List<Signature> required = Arrays.asList(
                signature("scale", "(I)J", false), signature("scale", "(J)J", false));
        List<Problem> problems = verify(required,
                "JAVA_LONG com_example_Natives_scale___int_R_long(CODENAME_ONE_THREAD_STATE,"
                + " JAVA_OBJECT me, JAVA_INT n) {\n    return n;\n}\n");

        assertEquals(1, problems.size());
        assertEquals("com_example_Natives_scale___long_R_long", problems.get(0).symbol);
    }

    // -------------------------------------------------------------- opt-in mode

    /**
     * The check must stay off unless a build asks for it. Turning the old soft
     * failure hard would change the outcome of app builds that succeed today, so
     * it is Codename One's CI that opts in, not the customer who trips over it.
     */
    @Test
    void theCheckIsOffUnlessTheBuildAsksForIt() {
        assertEquals(NativeSignatureVerifier.Mode.OFF, NativeSignatureVerifier.modeOf(null));
        assertEquals(NativeSignatureVerifier.Mode.OFF, NativeSignatureVerifier.modeOf(""));
        assertEquals(NativeSignatureVerifier.Mode.OFF, NativeSignatureVerifier.modeOf("off"));
        assertEquals(NativeSignatureVerifier.Mode.STRICT, NativeSignatureVerifier.modeOf("strict"));
        assertEquals(NativeSignatureVerifier.Mode.STRICT, NativeSignatureVerifier.modeOf(" STRICT "));
        assertEquals(NativeSignatureVerifier.Mode.WARN, NativeSignatureVerifier.modeOf("warn"));
        // an unrecognised value must not silently become strict
        assertEquals(NativeSignatureVerifier.Mode.OFF, NativeSignatureVerifier.modeOf("yes please"));
    }

    @Test
    void thePropertyIsReadWhenSet() {
        String previous = System.getProperty(NativeSignatureVerifier.MODE_PROPERTY);
        try {
            System.setProperty(NativeSignatureVerifier.MODE_PROPERTY, "strict");
            assertEquals(NativeSignatureVerifier.Mode.STRICT, NativeSignatureVerifier.mode());
            System.setProperty(NativeSignatureVerifier.MODE_PROPERTY, "off");
            assertEquals(NativeSignatureVerifier.Mode.OFF, NativeSignatureVerifier.mode());
        } finally {
            if (previous == null) {
                System.clearProperty(NativeSignatureVerifier.MODE_PROPERTY);
            } else {
                System.setProperty(NativeSignatureVerifier.MODE_PROPERTY, previous);
            }
        }
    }

    // ------------------------------------------------- translator runtime C

    /**
     * The translator copies some of its own runtime C into the project AFTER this
     * check runs -- on the clean target {@code java_io_File.m} is conditional on a
     * header {@code writeOutput} generates -- so the index has to read those off
     * the classpath. Missing this reported all 23 {@code java.io.File} natives as
     * unimplemented and failed FileClassIntegrationTest.
     */
    @Test
    void theTranslatorsOwnRuntimeCIsIndexedEvenBeforeItIsCopiedOut() throws IOException {
        Map<String, String> bundled = NativeSignatureVerifier.bundledRuntimeSources();
        assertTrue(bundled.containsKey("java_io_File.m"),
                "java_io_File.m must be readable from the translator jar; found " + bundled.keySet());

        SourceIndex index = new SourceIndex(Collections.<File>emptyList(), bundled);
        assertFalse(index.get("java_io_File_existsImpl___java_lang_String_R_boolean").isEmpty(),
                "the bundled java.io.File runtime should define existsImpl");
        assertFalse(index.get("java_lang_System_currentTimeMillis___R_long").isEmpty(),
                "the bundled nativeMethods.m should define currentTimeMillis");
    }

    // ------------------------------------------------------------ escape hatch

    @Test
    void aSymbolListedInTheIgnoreFileIsSkipped() throws IOException {
        Files.write(natives.resolve(NativeSignatureVerifier.IGNORE_FILE),
                ("# provided by a prebuilt library\n"
                 + "com_example_Natives_flag___R_boolean\n").getBytes(StandardCharsets.UTF_8));

        assertEquals(Collections.emptyList(),
                verify(signature("flag", "()Z", false), "// nothing here\n"));
    }

    @Test
    void anIgnoreGlobSkipsAWholePrefix() throws IOException {
        Files.write(natives.resolve(NativeSignatureVerifier.IGNORE_FILE),
                "com_example_Natives_*\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(Collections.emptyList(),
                verify(signature("flag", "()Z", false), "// nothing here\n"));
    }

    // ----------------------------------------------------------------- helpers

    private static Signature signature(String name, String desc, boolean isStatic) {
        int access = Opcodes.ACC_NATIVE | (isStatic ? Opcodes.ACC_STATIC : 0);
        return NativeSignatureVerifier.signatureOf("com/example/Natives", access, name, desc);
    }

    private List<Problem> verify(Signature required, String source) throws IOException {
        return verify(Collections.singletonList(required), source);
    }

    private List<Problem> verify(List<Signature> required, String source) throws IOException {
        Files.write(natives.resolve("natives.m"), source.getBytes(StandardCharsets.UTF_8));
        List<File> files = NativeSignatureVerifier.listNativeSources(natives.toFile());
        return NativeSignatureVerifier.verify(required, new SourceIndex(files));
    }

    /** The generated header spaces its declarations loosely; the prototype does not. */
    private static String normalize(String code) {
        return code.replaceAll("\\s+", " ");
    }

    private Path writeNativeHolder() throws IOException {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Natives", null,
                "java/lang/Object", null);
        writer.visitMethod(Opcodes.ACC_NATIVE, "noop", "()V", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_NATIVE, "flag", "()Z", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_NATIVE, "scale", "(I)J", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_NATIVE, "blob", "([[B)V", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC, "name",
                "()Ljava/lang/String;", null, null).visitEnd();
        writer.visitEnd();

        Path file = Files.createTempDirectory("natives").resolve("Natives.class");
        Files.write(file, writer.toByteArray());
        return file;
    }
}
