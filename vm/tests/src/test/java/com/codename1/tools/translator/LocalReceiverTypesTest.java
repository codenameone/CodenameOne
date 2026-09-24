package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Invoke;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalReceiverTypesTest {
    @Test
    void provesLocalAllocationsAndKeepsUnknownReceiversGuarded() throws Exception {
        Parser.cleanup();
        Path directory = Files.createTempDirectory("local-receiver-types");
        Path source = directory.resolve("Flow.java");
        StringBuilder deep = new StringBuilder("static void deep() { List x=new ArrayList();");
        for (int i = 0; i < 5000; i++) deep.append("x=x;");
        deep.append("x.iterator(); }");
        Files.write(source, ("import java.util.*; public class Flow {" + deep
                + "List field; Flow(List value) { field=value; } List getter() { return field; }"
                + "static List factory() { List x=new ArrayList(); x.add(new Object()); return x; }"
                + "List produced() { return new ArrayList(); }"
                + "static class Derived extends Flow { Derived() { super(null); } List produced() { return new LinkedList(); } }"
                + "static void polymorphic(Flow value) { value.produced().iterator(); }"
                + "static List branched(boolean b) { if (b) return new ArrayList(); return new ArrayList(4); }"
                + "static void factoryWalk() { factory().iterator(); }"
                + "static void branchWalk() { branched(true).iterator(); }"
                + "static List pass(List value) { return value; }"
                + "static void throughParameter(List value) { value.iterator(); }"
                + "static void exact() { pass(factory()).iterator(); throughParameter(factory()); }"
                + "static int walkMap() { HashMap m=new HashMap(); int n=0; for(Object o:m.values()) n++; return n; }"
                + "static int reuse() { List x=new ArrayList(); int n=0; for(Object o:x) n++; int k=x.size(); return n+k; }"
                + "static int reuseBranch(boolean b) { List x=new ArrayList(); int n=0; for(Object o:x) n++; if(b) { int k=3; n+=k; } else { int k=7; n+=k; } return n; }"
                + "static int walk() { List x=new ArrayList(); int n=0; for(Object o:x) n++; return n; }"
                + "int fieldWalk() { int n=0; for(Object o:field) n++; return n; }"
                + "int resultWalk() { int n=0; for(Object o:getter()) n++; return n; }"
                + "static void fields() { new Flow(factory()).getter().iterator(); }"
                + "static void mixed(boolean branch) { List x=new ArrayList(); if(branch) x=new LinkedList(); x.iterator(); }"
                + "static void loop(boolean branch) { List x=new ArrayList(); while(branch) { x=new LinkedList(); branch=false; } x.iterator(); }"
                + "static void cast() { Object x=new ArrayList(); ((List)x).iterator(); }"
                + "static void caught(boolean b) { List x=new ArrayList(); try { if(b) throw new RuntimeException(); } catch(RuntimeException e) { x=new LinkedList(); } x.iterator(); }"
                + "static void mergedUnknown(List input, boolean b) { List x=input; if(b) x=new ArrayList(); x.iterator(); }"
                + "static void unknown(List input) { input.iterator(); }"
                + "static void arrayUnknown(Object[] input, boolean b) { List x=new ArrayList(); if(b) x=(List)input[0]; x.iterator(); }"
                + "static void literalUnknown(boolean b) { List x=new ArrayList(); if(b) x=(List)(Object)\"text\"; x.iterator(); }"
                + "}").getBytes(StandardCharsets.UTF_8));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-source", "8", "-target", "8", "-d", directory.toString(), source.toString()));
        Parser.parse(directory.resolve("Flow.class").toFile());
        Parser.parse(directory.resolve("Flow$Derived.class").toFile());
        LocalReceiverTypes.resolveFactories();
        ByteCodeClass cls = Parser.getClassObject("Flow");
        for (BytecodeMethod method : cls.getMethods()) {
            if (!method.getMethodName().equals("reuse") && !method.getMethodName().equals("reuseBranch")) continue;
            method.freezeFramelessEligibility();
            method.intrinsifyForEach();
            assertEquals(1, method.getForEachIntrinsicCount(), method.getMethodName());
            StringBuilder generated = new StringBuilder();
            method.appendMethodC(generated);
            assertTrue(generated.toString().contains("DEFINE_METHOD_STACK_FRAMELESS"), generated.toString());
            assertFalse(generated.toString().contains("DEFINE_METHOD_STACK_FAST_REF"));
            assertTrue(generated.toString().contains("CN1_KEEP_NATIVE_OWNER(__feOwner_0, JAVA_NULL)"));
            assertTrue(generated.toString().contains("__feOwner_0 = locals["));
        }
        for (BytecodeMethod method : cls.getMethods()) {
            if (!method.getMethodName().equals("fieldWalk") && !method.getMethodName().equals("resultWalk")) continue;
            method.intrinsifyForEach();
            assertEquals(1, method.getForEachIntrinsicCount());
            boolean stackConsumer = false;
            for (Instruction instruction : method.getInstructions())
                if (instruction instanceof NativeTraversal.StackBegin) stackConsumer = true;
            assertTrue(stackConsumer, "raw receivers must keep their stack producer");
        }
        assertTrue(iterator(cls, "factoryWalk").hasExactReceiver("java_util_ArrayList"));
        assertTrue(iterator(cls, "branchWalk").hasExactReceiver("java_util_ArrayList"));
        assertFalse(iterator(cls, "polymorphic").hasExactReceiver("java_util_ArrayList"));
        assertFalse(iterator(cls, "exact").hasExactReceiver("java_util_ArrayList"));
        assertTrue(iterator(cls, "deep").hasExactReceiver("java_util_ArrayList"));
        assertFalse(iterator(cls, "throughParameter").hasExactReceiver("java_util_ArrayList"));
        assertFalse(iterator(cls, "fields").hasExactReceiver("java_util_ArrayList"));
        assertFalse(iterator(cls, "mixed").hasExactReceiver("java_util_ArrayList"));
        assertFalse(iterator(cls, "loop").hasExactReceiver("java_util_ArrayList"));
        assertFalse(iterator(cls, "mergedUnknown").hasExactReceiver("java_util_ArrayList"));
        assertFalse(iterator(cls, "unknown").hasExactReceiver("java_util_ArrayList"));
        assertFalse(iterator(cls, "arrayUnknown").hasExactReceiver("java_util_ArrayList"));
        assertFalse(iterator(cls, "literalUnknown").hasExactReceiver("java_util_ArrayList"));
        assertTrue(iterator(cls, "cast").hasExactReceiver("java_util_ArrayList"));
        assertFalse(iterator(cls, "caught").hasExactReceiver("java_util_ArrayList"));
        for (BytecodeMethod method : cls.getMethods()) if (method.getMethodName().equals("walk")) {
            assertTrue(iterator(cls, "walk").hasExactReceiver("java_util_ArrayList"));
            method.intrinsifyForEach();
            StringBuilder emitted = new StringBuilder();
            for (Instruction instruction : method.getInstructions()) {
                if (instruction instanceof com.codename1.tools.translator.bytecodes.CustomIntruction)
                    emitted.append(((com.codename1.tools.translator.bytecodes.CustomIntruction) instruction).getCode());
            }
            String code = emitted.toString();
            assertTrue(code.contains("cn1RefBlockGet"));
            assertTrue(code.contains("CN1_THROW_CME"));
            assertTrue(code.contains("locals["));
            assertFalse(code.contains("virtual_java_util_Iterator"));
            assertFalse(code.contains("cn1IterScopeBegin"));
            assertFalse(code.contains("CN1_OBJ_CLASS("));
        }
        for (BytecodeMethod method : cls.getMethods()) if (method.getMethodName().equals("walkMap")) {
            method.intrinsifyForEach();
            StringBuilder emitted = new StringBuilder();
            for (Instruction instruction : method.getInstructions()) {
                if (instruction instanceof com.codename1.tools.translator.bytecodes.CustomIntruction)
                    emitted.append(((com.codename1.tools.translator.bytecodes.CustomIntruction) instruction).getCode());
            }
            String code = emitted.toString();
            // The values are the table's part 1, derived from the keys root.
            assertTrue(code.contains("cn1TablePart(get_field_java_util_HashMap_cn1KeysBlock(") && code.contains(", 1)"));
            assertFalse(code.contains("_values___"));
            assertFalse(code.contains("virtual_java_util_Iterator"));
            assertFalse(code.contains("CN1_OBJ_CLASS("));
        }
        for (BytecodeMethod method : cls.getMethods()) if (method.getMethodName().equals("caught")) {
            method.freezeFramelessEligibility();
            method.markStackIterators();
            StringBuilder generated = new StringBuilder();
            method.appendMethodC(generated);
            // A try/catch no longer refuses frameless codegen (7019abafdc): the method is
            // frameless, and its frame must then carry the two names the exception macros
            // restore to -- which is what CN1_FRAMELESS_TRY_FRAME supplies. The receiver
            // is not proven exact (asserted above), so no stack iterator is placed and the
            // catch path cannot leave one dangling.
            String c = generated.toString();
            assertTrue(c.contains("DEFINE_METHOD_STACK_FRAMELESS"), c);
            assertTrue(c.contains("CN1_FRAMELESS_TRY_FRAME();"), c);
            assertFalse(c.contains("cn1IterScopeBegin"), c);
        }
        Parser.cleanup();
    }

    @Test
    void provesAllPrivateFieldWritesAndRejectsUnknownOrNativeWriters() throws Exception {
        Parser.cleanup();
        Path directory = Files.createTempDirectory("field-receiver-types");
        Path source = directory.resolve("Fields.java");
        Files.write(source, ("import java.util.*; public class Fields {"
                + "private List exact=new ArrayList(); private List unknown=new ArrayList();"
                + "private List mixed=new ArrayList(); List exposed=new ArrayList();"
                + "private static List shared=make(); private List copied=exact;"
                + "private List branch; private List empty; private List tainted;"
                + "Fields(boolean b) { branch=b ? new ArrayList() : new ArrayList(4); }"
                + "static List make() { return new ArrayList(); }"
                + "void mergeWrite(List x, boolean b) { tainted=b ? new ArrayList() : x; } void taintedRead() { tainted.iterator(); }"
                + "void update(List x) { unknown=x; } void change() { mixed=new LinkedList(); }"
                + "void reset() { exact=null; exact=new ArrayList(2); }"
                + "int exactWalk() { int n=0; for(Object x:exact) n++; return n; }"
                + "void exactRead() { exact.iterator(); } void unknownRead() { unknown.iterator(); }"
                + "void mixedRead() { mixed.iterator(); } void exposedRead() { exposed.iterator(); }"
                + "void copiedRead() { copied.iterator(); } static void sharedRead() { shared.iterator(); }"
                + "void branchRead() { branch.iterator(); } void emptyRead() { empty.iterator(); }"
                + "static class Native { private List value=new ArrayList(); native void mutate();"
                + "void read() { value.iterator(); } }"
                + "}").getBytes(StandardCharsets.UTF_8));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-source", "8", "-target", "8", "-d", directory.toString(), source.toString()));
        Parser.parse(directory.resolve("Fields.class").toFile());
        Parser.parse(directory.resolve("Fields$Native.class").toFile());
        LocalReceiverTypes.resolveFactories();
        ByteCodeClass cls = Parser.getClassObject("Fields");
        for (String name : new String[]{"exactRead", "sharedRead", "branchRead", "exactWalk"}) {
            assertTrue(iterator(cls, name).hasExactReceiver("java_util_ArrayList"), name);
        }
        for (String name : new String[]{"unknownRead", "mixedRead", "exposedRead", "copiedRead", "emptyRead", "taintedRead"}) {
            assertFalse(iterator(cls, name).hasExactReceiver("java_util_ArrayList"), name);
        }
        assertFalse(iterator(Parser.getClassObject("Fields_Native"), "read").hasExactReceiver("java_util_ArrayList"));
        for (BytecodeMethod method : cls.getMethods()) if (method.getMethodName().equals("exactWalk")) {
            method.freezeFramelessEligibility();
            method.intrinsifyForEach();
            StringBuilder generated = new StringBuilder();
            method.appendMethodC(generated);
            assertTrue(generated.toString().contains("cn1RefBlockGet"));
            assertTrue(generated.toString().contains("CN1_KEEP_NATIVE_OWNER"));
            assertFalse(generated.toString().contains("virtual_java_util_Iterator"));
            assertFalse(generated.toString().contains("CN1_OBJ_CLASS("));
        }
        Parser.cleanup();
        Parser.parse(directory.resolve("Fields.class").toFile());
        LocalReceiverTypes.resolveFactories(new NativeSymbolIndex(new String[]{
                "void other_native(JAVA_OBJECT value) { set_field_Fields_exact(replacement, value); }"}));
        assertFalse(iterator(Parser.getClassObject("Fields"), "exactRead").hasExactReceiver("java_util_ArrayList"));
        Parser.cleanup();
        // An earlier parse's writer summary must never make a later parse exact.
        Parser.parse(directory.resolve("Fields.class").toFile());
        LocalReceiverTypes.clear();
        LocalReceiverTypes.resolveFactories();
        assertFalse(iterator(Parser.getClassObject("Fields"), "exactRead").hasExactReceiver("java_util_ArrayList"));
        Parser.cleanup();
    }

    private Invoke iterator(ByteCodeClass cls, String name) {
        for (BytecodeMethod method : cls.getMethods()) if (method.getMethodName().equals(name)) {
            for (Instruction instruction : method.getInstructions()) if (instruction instanceof Invoke) {
                Invoke invoke = (Invoke) instruction;
                if (invoke.getName().equals("iterator")) return invoke;
            }
        }
        throw new AssertionError("Missing iterator call in " + name);
    }
}
