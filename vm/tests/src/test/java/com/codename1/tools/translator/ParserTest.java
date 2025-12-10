package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.Instruction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @BeforeEach
    void cleanParser() {
        Parser.cleanup();
    }

    @Test
    void parsesBasicClassWithFieldsAndMethodBodies() throws Exception {
        Path classFile = createSampleClass();

        Parser.parse(classFile.toFile());

        ByteCodeClass cls = Parser.getClassObject("com_example_Sample");
        assertNotNull(cls, "Class should be parsed");
        assertEquals("java/lang/Object", cls.getBaseClass());
        assertFalse(cls.isIsInterface());
        assertFalse(cls.isIsAbstract());

        ByteCodeField greeting = findField(cls, "GREETING");
        assertTrue(greeting.isStaticField());
        assertEquals("hi", greeting.getValue());

        ByteCodeField names = findField(cls, "names");
        assertTrue(names.isObjectType(), "Generics-based list field should be an object type");
        assertTrue(names.getDependentClasses().contains("java_util_List"));

        BytecodeMethod sum = findMethod(cls, "sum");
        assertFalse(sum.isStatic());
        List<Instruction> instructions = getInstructions(sum);
        List<Integer> opcodes = instructions.stream()
                .map(Instruction::getOpcode)
                .collect(Collectors.toList());
        assertTrue(opcodes.contains(Opcodes.IADD), "Method should include an integer add operation");
        assertEquals(Opcodes.IRETURN, opcodes.get(opcodes.size() - 1));
    }

    @Test
    void parsesInterfacesAsAbstractContracts() throws Exception {
        Path classFile = createTaskInterface();

        Parser.parse(classFile.toFile());

        ByteCodeClass cls = Parser.getClassObject("com_example_Task");
        assertNotNull(cls);
        assertTrue(cls.isIsInterface());
        assertTrue(cls.isIsAbstract());

        BytecodeMethod method = findMethod(cls, "runTask");
        assertTrue(method.canBeVirtual());
    }

    @Test
    void parsesEnumMetadataAndBaseType() throws Exception {
        Path classFile = createPriorityEnum();

        Parser.parse(classFile.toFile());

        ByteCodeClass cls = Parser.getClassObject("com_example_Priority");
        assertNotNull(cls);
        assertEquals("java/lang/Enum", cls.getBaseClass());
        assertTrue(readPrivateBoolean(cls, "isEnum"));
    }

    @Test
    void parsesAnnotationsWithCorrectFlags() throws Exception {
        Path classFile = createAnnotation();

        Parser.parse(classFile.toFile());

        ByteCodeClass cls = Parser.getClassObject("com_example_TestAnnotation");
        assertNotNull(cls);
        assertTrue(cls.isIsInterface(), "Annotations should be treated as interfaces");
        assertTrue(readPrivateBoolean(cls, "isAnnotation"));
        assertFalse(readPrivateBoolean(cls, "isSynthetic"));
    }

    private ByteCodeField findField(ByteCodeClass cls, String name) {
        return cls.getFields()
                .stream()
                .filter(f -> f.getFieldName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Field not found: " + name));
    }

    private BytecodeMethod findMethod(ByteCodeClass cls, String name) {
        return cls.getMethods()
                .stream()
                .filter(m -> m.getMethodName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Method not found: " + name));
    }

    @SuppressWarnings("unchecked")
    private List<Instruction> getInstructions(BytecodeMethod method) throws Exception {
        Field instructionsField = BytecodeMethod.class.getDeclaredField("instructions");
        instructionsField.setAccessible(true);
        return (List<Instruction>) instructionsField.get(method);
    }

    private boolean readPrivateBoolean(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private Path createSampleClass() throws Exception {
        return writeClass("com/example/Sample", cw -> {
            cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "com/example/Sample", null, "java/lang/Object", null);
            cw.visitField(Opcodes.ACC_PRIVATE, "counter", "I", null, null).visitEnd();
            cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "GREETING", "Ljava/lang/String;", null, "hi").visitEnd();
            cw.visitField(Opcodes.ACC_PUBLIC, "names", "Ljava/util/List;", "Ljava/util/List<Ljava/lang/String;>;", null).visitEnd();

            MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            init.visitCode();
            init.visitVarInsn(Opcodes.ALOAD, 0);
            init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            init.visitInsn(Opcodes.RETURN);
            init.visitMaxs(1, 1);
            init.visitEnd();

            MethodVisitor sum = cw.visitMethod(Opcodes.ACC_PUBLIC, "sum", "(II)I", null, null);
            sum.visitCode();
            sum.visitVarInsn(Opcodes.ILOAD, 1);
            sum.visitVarInsn(Opcodes.ILOAD, 2);
            sum.visitInsn(Opcodes.IADD);
            sum.visitInsn(Opcodes.IRETURN);
            sum.visitMaxs(2, 3);
            sum.visitEnd();

            cw.visitEnd();
        });
    }

    private Path createTaskInterface() throws Exception {
        return writeClass("com/example/Task", cw -> {
            cw.visit(
                    Opcodes.V1_5,
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
                    "com/example/Task",
                    null,
                    "java/lang/Object",
                    null
            );
            cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "runTask", "()V", null, null).visitEnd();
            cw.visitEnd();
        });
    }

    private Path createPriorityEnum() throws Exception {
        return writeClass("com/example/Priority", cw -> {
            cw.visit(
                    Opcodes.V1_5,
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER | Opcodes.ACC_ENUM,
                    "com/example/Priority",
                    null,
                    "java/lang/Enum",
                    null
            );
            cw.visitField(enumFieldFlags(), "LOW", "Lcom/example/Priority;", null, null).visitEnd();
            cw.visitField(enumFieldFlags(), "HIGH", "Lcom/example/Priority;", null, null).visitEnd();
            cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC, "$VALUES", "[Lcom/example/Priority;", null, null).visitEnd();

            MethodVisitor ctor = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V", null, null);
            ctor.visitCode();
            ctor.visitVarInsn(Opcodes.ALOAD, 0);
            ctor.visitVarInsn(Opcodes.ALOAD, 1);
            ctor.visitVarInsn(Opcodes.ILOAD, 2);
            ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false);
            ctor.visitInsn(Opcodes.RETURN);
            ctor.visitMaxs(3, 3);
            ctor.visitEnd();

            MethodVisitor codeMethod = cw.visitMethod(Opcodes.ACC_PUBLIC, "code", "()I", null, null);
            codeMethod.visitCode();
            codeMethod.visitVarInsn(Opcodes.ALOAD, 0);
            codeMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Enum", "ordinal", "()I", false);
            codeMethod.visitInsn(Opcodes.IRETURN);
            codeMethod.visitMaxs(1, 1);
            codeMethod.visitEnd();

            MethodVisitor values = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "values", "()[Lcom/example/Priority;", null, null);
            values.visitCode();
            values.visitFieldInsn(Opcodes.GETSTATIC, "com/example/Priority", "$VALUES", "[Lcom/example/Priority;");
            values.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "[Lcom/example/Priority;", "clone", "()Ljava/lang/Object;", false);
            values.visitTypeInsn(Opcodes.CHECKCAST, "[Lcom/example/Priority;");
            values.visitInsn(Opcodes.ARETURN);
            values.visitMaxs(1, 0);
            values.visitEnd();

            MethodVisitor valueOf = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "valueOf", "(Ljava/lang/String;)Lcom/example/Priority;", null, null);
            valueOf.visitCode();
            valueOf.visitLdcInsn(Type.getType("Lcom/example/Priority;"));
            valueOf.visitVarInsn(Opcodes.ALOAD, 0);
            valueOf.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
            valueOf.visitTypeInsn(Opcodes.CHECKCAST, "com/example/Priority");
            valueOf.visitInsn(Opcodes.ARETURN);
            valueOf.visitMaxs(2, 1);
            valueOf.visitEnd();

            MethodVisitor clinit = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.visitCode();
            clinit.visitTypeInsn(Opcodes.NEW, "com/example/Priority");
            clinit.visitInsn(Opcodes.DUP);
            clinit.visitLdcInsn("LOW");
            clinit.visitInsn(Opcodes.ICONST_0);
            clinit.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/example/Priority", "<init>", "(Ljava/lang/String;I)V", false);
            clinit.visitFieldInsn(Opcodes.PUTSTATIC, "com/example/Priority", "LOW", "Lcom/example/Priority;");

            clinit.visitTypeInsn(Opcodes.NEW, "com/example/Priority");
            clinit.visitInsn(Opcodes.DUP);
            clinit.visitLdcInsn("HIGH");
            clinit.visitInsn(Opcodes.ICONST_1);
            clinit.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/example/Priority", "<init>", "(Ljava/lang/String;I)V", false);
            clinit.visitFieldInsn(Opcodes.PUTSTATIC, "com/example/Priority", "HIGH", "Lcom/example/Priority;");

            clinit.visitInsn(Opcodes.ICONST_2);
            clinit.visitTypeInsn(Opcodes.ANEWARRAY, "com/example/Priority");
            clinit.visitInsn(Opcodes.DUP);
            clinit.visitInsn(Opcodes.ICONST_0);
            clinit.visitFieldInsn(Opcodes.GETSTATIC, "com/example/Priority", "LOW", "Lcom/example/Priority;");
            clinit.visitInsn(Opcodes.AASTORE);
            clinit.visitInsn(Opcodes.DUP);
            clinit.visitInsn(Opcodes.ICONST_1);
            clinit.visitFieldInsn(Opcodes.GETSTATIC, "com/example/Priority", "HIGH", "Lcom/example/Priority;");
            clinit.visitInsn(Opcodes.AASTORE);
            clinit.visitFieldInsn(Opcodes.PUTSTATIC, "com/example/Priority", "$VALUES", "[Lcom/example/Priority;");
            clinit.visitInsn(Opcodes.RETURN);
            clinit.visitMaxs(5, 0);
            clinit.visitEnd();

            cw.visitEnd();
        });
    }

    private Path createAnnotation() throws Exception {
        return writeClass("com/example/TestAnnotation", cw -> {
            cw.visit(
                    Opcodes.V1_5,
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION,
                    "com/example/TestAnnotation",
                    null,
                    "java/lang/Object",
                    new String[]{"java/lang/annotation/Annotation"}
            );
            cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "value", "()Ljava/lang/String;", null, null).visitEnd();
            cw.visitEnd();
        });
    }

    private int enumFieldFlags() {
        return Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM;
    }

    private Path writeClass(String internalName, ClassEmitter emitter) throws Exception {
        ClassWriter cw = new ClassWriter(0);
        emitter.accept(cw);
        Path outputDir = Files.createTempDirectory("parparvm-classes");
        Path classFile = outputDir.resolve(internalName + ".class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, cw.toByteArray());
        return classFile;
    }

    @FunctionalInterface
    private interface ClassEmitter {
        void accept(ClassWriter classWriter) throws Exception;
    }
}
