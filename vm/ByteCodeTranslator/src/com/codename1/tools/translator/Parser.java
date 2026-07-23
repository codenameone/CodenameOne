/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import com.codename1.tools.translator.bytecodes.BasicInstruction;
import com.codename1.tools.translator.bytecodes.Instruction;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import com.codename1.tools.translator.bytecodes.LabelInstruction;

/**
 *
 * @author Shai Almog
 */
public class Parser extends ClassVisitor {
    private static final String DISABLE_DEBUG_INFO_ANNOTATION = "Lcom/codename1/annotations/DisableDebugInfo;";
    private static final String DISABLE_NULL_AND_ARRAY_BOUNDS_CHECKS_ANNOTATION =
            "Lcom/codename1/annotations/DisableNullChecksAndArrayBoundsChecks;";
    private static final String CONCRETE_ANNOTATION = "Lcom/codename1/annotations/Concrete;";
    private static final String STACK_ALLOCATE_ANNOTATION = "Lcom/codename1/annotations/StackAllocate;";
    private static final String FUSED_ANNOTATION = "Lcom/codename1/annotations/Fused;";
    private ByteCodeClass cls;
    private String clsName;
    private static String[] nativeSources;
    private static List<ByteCodeClass> classes = new ArrayList<>();

    // ---- CLOSED-WORLD DEVIRTUALIZATION -----------------------------------
    // ParparVM compiles a closed world: after dead-code elimination the class
    // list is final, so an INVOKEVIRTUAL whose method has NO reachable override
    // below the static receiver type can be emitted as a DIRECT call to the
    // implementing class's C function -- removing the vtable load + indirect
    // branch AND letting ThinLTO inline it (a vtable-dispatched getter like
    // String.length()/HashMap.size() otherwise stays an opaque call in every
    // hot loop). The subclass index is (re)built lazily whenever the class list
    // changed; emission runs after the list is final.
    private static java.util.Map<String, java.util.List<ByteCodeClass>> cn1SubclassIndex;
    private static int cn1SubclassIndexSize = -1;

    private static void cn1EnsureSubclassIndex() {
        if (cn1SubclassIndexSize == classes.size()) {
            return;
        }
        cn1SubclassIndex = new java.util.HashMap<String, java.util.List<ByteCodeClass>>();
        for (ByteCodeClass c : classes) {
            if (c.getBaseClass() != null) {
                String b = c.getBaseClass().replace('/', '_').replace('$', '_');
                java.util.List<ByteCodeClass> l = cn1SubclassIndex.get(b);
                if (l == null) {
                    l = new java.util.ArrayList<ByteCodeClass>();
                    cn1SubclassIndex.put(b, l);
                }
                l.add(c);
            }
        }
        cn1SubclassIndexSize = classes.size();
    }

    /**
     * If (name, desc) invoked virtually on {@code owner} has no reachable
     * override in any non-eliminated subclass, returns the mangled name of the
     * class whose non-abstract declaration implements it (owner or an
     * ancestor); otherwise null (the call must stay a vtable dispatch).
     */
    public static synchronized String resolveDevirtualizedOwner(ByteCodeClass owner, String name, String desc) {
        if (owner == null) {
            return null;
        }
        cn1EnsureSubclassIndex();
        // any subclass DECLARING the method (abstract or not) keeps it virtual
        java.util.ArrayDeque<ByteCodeClass> stack = new java.util.ArrayDeque<ByteCodeClass>();
        java.util.List<ByteCodeClass> kids = cn1SubclassIndex.get(owner.getClsName());
        if (kids != null) {
            stack.addAll(kids);
        }
        while (!stack.isEmpty()) {
            ByteCodeClass c = stack.pop();
            if (!c.isEliminated() && c.hasDeclaredMethod(name, desc)) {
                return null;
            }
            kids = cn1SubclassIndex.get(c.getClsName());
            if (kids != null) {
                stack.addAll(kids);
            }
        }
        // resolve the implementing declaration at or above the static type
        ByteCodeClass c = owner;
        while (c != null) {
            if (c.hasDeclaredNonAbstractMethod(name, desc)) {
                return c.getClsName();
            }
            String b = c.getBaseClass();
            if (b == null) {
                return null;
            }
            c = getClassObject(b.replace('/', '_').replace('$', '_'));
        }
        return null;
    }
    private static final MethodDependencyGraph dependencyGraph = new MethodDependencyGraph();
    private int lambdaCounter;
    private int stringConcatCounter;
    public static void cleanup() {
        nativeSources = null;
        classes.clear();
        // classes is cleared in place (same List reference), so the name index's
        // (reference, size) guard cannot detect a subsequent same-size refill across
        // translation runs in the same JVM (e.g. the unit tests). Invalidate it here.
        classIndexMap = null;
        classIndexSource = null;
        classIndexSize = -1;
        dependencyGraph.clear();
        BytecodeMethod.setDependencyGraph(null);
        ByteCodeClass.cleanup();
        LabelInstruction.cleanup();
    }
    public static void parse(File sourceFile) throws Exception {
        if(ByteCodeTranslator.verbose) {
            System.out.println("Parsing: " + sourceFile.getAbsolutePath());
        }
        BytecodeMethod.setDependencyGraph(dependencyGraph);
        ClassReader r;
        try (InputStream in = Files.newInputStream(sourceFile.toPath())) {
            r = new ClassReader(in);
        }
        Parser p = new Parser();
        
        p.clsName = r.getClassName().replace('/', '_').replace('$', '_');
        p.cls = new ByteCodeClass(p.clsName, r.getClassName());
        r.accept(p, ClassReader.EXPAND_FRAMES);
        
        classes.add(p.cls);
    }
    
    private static ByteCodeClass getClassByName(String name) {
        return classIndex().get(name.replace('/', '_').replace('$', '_'));
    }

    /**
     * Resolves a class by name and returns its post-{@link #writeOutput}
     * classOffset, or -1 if no class with that name exists. Used by the
     * on-device-debug side-table emitter to wire stable IDs into the
     * generated frame_info structs.
     */
    public static int getClassOffset(String name) {
        ByteCodeClass bc = getClassByName(name);
        return bc == null ? -1 : bc.getClassOffset();
    }

    /**
     * On-device-debug field-id allocator. Maps a (declaring-class, field-name)
     * pair to a stable int the device and proxy both use to address an
     * instance field. Field ids start at 1 — 0 is reserved as a "no-field"
     * sentinel so JDWP code can use 0 to mean "skip".
     *
     * IDs are persistent only within a single translator invocation; the
     * sidecar carries them so the proxy doesn't need to recompute the same
     * mapping.
     */
    private static final java.util.LinkedHashMap<String, Integer> fieldIdByKey = new java.util.LinkedHashMap<>();
    private static int nextFieldId = 1;

    public static int getOrAssignFieldId(String declClassMangled, String fieldName) {
        String key = declClassMangled + "." + fieldName;
        Integer id = fieldIdByKey.get(key);
        if (id != null) return id;
        id = nextFieldId++;
        fieldIdByKey.put(key, id);
        return id;
    }

    /**
     * Returns the JVM-style descriptor for a ByteCodeField — "I" for int,
     * "Lcom/example/Foo;" for object, "[I" for int[], etc. The translator
     * normally exposes underscore-mangled type names; this helper converts
     * to JVM form for JDWP wire compatibility.
     */
    public static String jvmDescriptorOf(ByteCodeField bf) {
        StringBuilder sb = new StringBuilder();
        String rd = bf.getRuntimeDescriptor();
        // getRuntimeDescriptor returns either a JVM single-char (I/J/...)
        // or an underscore-mangled object type, possibly followed by "[]"
        // repeats for array dimensions.
        int arrayDims = 0;
        while (rd != null && rd.endsWith("[]")) {
            arrayDims++;
            rd = rd.substring(0, rd.length() - 2);
        }
        for (int i = 0; i < arrayDims; i++) sb.append('[');
        if (rd != null && rd.length() == 1 && "ZBSCIJFD".indexOf(rd.charAt(0)) >= 0) {
            sb.append(rd);
        } else if (rd != null && !rd.isEmpty()) {
            sb.append('L').append(rd.replace('_', '/')).append(';');
        } else {
            sb.append("Ljava/lang/Object;");
        }
        return sb.toString();
    }

    /**
     * Returns a JDWP-style modifier bitmask (PUBLIC=1, PRIVATE=2, STATIC=8,
     * FINAL=16, VOLATILE=64, TRANSIENT=128). We don't track protected/transient
     * — fields are reported as public unless flagged private.
     */
    public static int jdwpAccessFlagsOf(ByteCodeField bf) {
        int f = 0;
        if (bf.isPrivate()) f |= 0x0002; else f |= 0x0001;
        if (bf.isStaticField()) f |= 0x0008;
        if (bf.isFinal()) f |= 0x0010;
        if (bf.isVolatile()) f |= 0x0040;
        return f;
    }

    /**
     * Emits the on-device-debug symbol table, gzip-compressed, as a generated
     * C source ({@code cn1_debug_symbols.c}) that links straight into the
     * iOS binary. The device serves it to the desktop debug proxy on demand
     * (CMD_GET_SYMBOLS) so the proxy never needs a local file — which is what
     * makes on-device debugging work for cloud builds (Windows/Linux), where
     * the translator ran on the build server and no sidecar ever reaches the
     * developer's machine.
     *
     * The uncompressed payload is the same line-based ASCII the proxy's
     * SymbolTable parses:
     *   version &lt;n&gt;
     *   class   &lt;classId&gt; &lt;clsName&gt; &lt;sourceFile&gt; &lt;superId&gt; &lt;jvmName&gt;
     *   method  &lt;methodId&gt; &lt;classId&gt; &lt;methodName&gt; &lt;desc&gt; &lt;isStatic&gt;
     *   line    &lt;methodId&gt; &lt;sourceLine&gt;
     *   var     &lt;methodId&gt; &lt;slot&gt; &lt;name&gt; &lt;desc&gt;
     *   field   &lt;classId&gt; &lt;fieldId&gt; &lt;name&gt; &lt;desc&gt; &lt;access&gt;
     */
    private static void writeSymbolSidecar(File outputDirectory) throws IOException {
        java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream(1 << 20);
        try (Writer w = new OutputStreamWriter(raw, StandardCharsets.UTF_8)) {
            w.write("version\t1\n");
            for (ByteCodeClass bc : classes) {
                String src = bc.getSourceFile();
                if (src == null) {
                    src = "";
                }
                w.write(classSymbolRow(bc, src));
            }
            // Emit instance-field metadata so the proxy can answer JDWP
            // ClassType.Fields / FieldsWithGeneric without a device round-trip,
            // and so ObjectReference.GetValues knows what (type, declaring class)
            // each fieldId resolves to. We list inherited fields under each
            // class that physically stores them — JDWP expects a class's
            // Fields response to include only its own declarations, but
            // listing inherited fields too keeps single-table lookup cheap on
            // the proxy side; the proxy filters to "declared here" itself.
            for (ByteCodeClass bc : classes) {
                int classId = bc.getClassOffset();
                for (ByteCodeField bf : bc.getFields()) {
                    if (bf.isStaticField()) continue;
                    int fid = getOrAssignFieldId(bc.getClsName(), bf.getFieldName());
                    String desc = jvmDescriptorOf(bf);
                    int access = jdwpAccessFlagsOf(bf);
                    w.write("field\t" + classId + "\t" + fid
                            + "\t" + bf.getFieldName()
                            + "\t" + desc
                            + "\t" + access + "\n");
                }
            }
            for (ByteCodeClass bc : classes) {
                int classId = bc.getClassOffset();
                for (BytecodeMethod m : bc.getMethods()) {
                    if (m.isEliminated()) {
                        continue;
                    }
                    String desc = m.getDesc();
                    if (desc == null) {
                        desc = "";
                    }
                    // Extended method row: classId, name, desc, isStatic.
                    // Older proxies that only know 4 columns ignore the 5th
                    // because the parser slices with `split("\t", -1)` and
                    // size-checks before reading.
                    w.write("method\t" + m.getMethodOffset() + "\t" + classId
                            + "\t" + m.getMethodName()
                            + "\t" + desc
                            + "\t" + (m.isStatic() ? "1" : "0") + "\n");
                    Set<Integer> lines = new TreeSet<>();
                    for (com.codename1.tools.translator.bytecodes.Instruction ins : m.getInstructions()) {
                        if (ins instanceof com.codename1.tools.translator.bytecodes.LineNumber) {
                            lines.add(((com.codename1.tools.translator.bytecodes.LineNumber)ins).getLine());
                        }
                    }
                    for (Integer line : lines) {
                        w.write("line\t" + m.getMethodOffset() + "\t" + line + "\n");
                    }
                    // Local variables: emitted as "always-live" scope until a
                    // follow-up resolves ASM labels to source lines properly.
                    // jdb tolerates this — uninitialised slots just show 0.
                    for (com.codename1.tools.translator.bytecodes.LocalVariable lv : m.getLocalVariables()) {
                        w.write("var\t" + m.getMethodOffset()
                                + "\t" + lv.getIndex()
                                + "\t" + lv.getOrigName()
                                + "\t" + lv.getDesc() + "\n");
                    }
                }
            }
        }

        // gzip the payload — symbol tables are large and highly repetitive,
        // so this keeps the debug binary's footprint modest.
        java.io.ByteArrayOutputStream gzOut = new java.io.ByteArrayOutputStream(raw.size() / 3 + 64);
        try (java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(gzOut)) {
            raw.writeTo(gz);
        }
        byte[] gz = gzOut.toByteArray();

        File f = new File(outputDirectory, "cn1_debug_symbols.c");
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(f.toPath()), StandardCharsets.UTF_8)) {
            w.write("/* Auto-generated by the Codename One iOS translator. Do not edit.\n");
            w.write(" * On-device-debug symbol table (gzip-compressed), streamed to the\n");
            w.write(" * desktop debug proxy over CMD_GET_SYMBOLS. */\n");
            // cn1_globals.h carries the CN1_ON_DEVICE_DEBUG #define (flipped on
            // by the builder for debug builds); include it so the guard below
            // actually sees the macro rather than silently compiling to nothing.
            w.write("#include \"cn1_globals.h\"\n");
            w.write("#ifdef CN1_ON_DEVICE_DEBUG\n");
            w.write("static const unsigned char cn1_debug_symbols_gz[] = {\n");
            for (int i = 0; i < gz.length; i++) {
                if ((i & 15) == 0) {
                    w.write("    ");
                }
                w.write("0x");
                int b = gz[i] & 0xff;
                w.write(Character.forDigit(b >> 4, 16));
                w.write(Character.forDigit(b & 0xf, 16));
                w.write(',');
                w.write((i & 15) == 15 ? '\n' : ' ');
            }
            w.write("\n};\n");
            w.write("static const int cn1_debug_symbols_gz_len = " + gz.length + ";\n");
            w.write("const unsigned char* cn1_debug_symbols_data(void) { return cn1_debug_symbols_gz; }\n");
            w.write("int cn1_debug_symbols_length(void) { return cn1_debug_symbols_gz_len; }\n");
            w.write("#endif\n");
        }
        if (ByteCodeTranslator.verbose) {
            System.out.println("Wrote on-device-debug symbol blob: " + f.getAbsolutePath()
                    + " (" + gz.length + " gz bytes, " + raw.size() + " raw)");
        }
    }

    /**
     * Builds a symbol-table class row while retaining both ParparVM's mangled
     * identifier and the original JVM internal name. The latter is required
     * for JDWP signatures because mangling maps both {@code '/'} and
     * {@code '$'} to {@code '_'}, making anonymous and inner classes
     * impossible to reconstruct reliably in the debugger proxy.
     */
    static String classSymbolRow(ByteCodeClass bc, String sourceFile) {
        int superId = -1;
        if (bc.getBaseClassObject() != null) {
            superId = bc.getBaseClassObject().getClassOffset();
        }
        String jvmName = bc.getOriginalClassName();
        if (jvmName == null || jvmName.isEmpty()) {
            // Defensive compatibility for synthetic ByteCodeClass instances
            // that predate original-name tracking.
            jvmName = bc.getClsName().replace('_', '/');
        }
        return "class\t" + bc.getClassOffset() + "\t" + bc.getClsName()
                + "\t" + sourceFile + "\t" + superId + "\t" + jvmName + "\n";
    }
    
    private static void appendClassOffset(ByteCodeClass bc, List<Integer> clsIds) {
        if(bc.getBaseClassObject() != null) {
            if(!clsIds.contains(bc.getBaseClassObject().getClassOffset())) {
                clsIds.add(bc.getBaseClassObject().getClassOffset());
                appendClassOffset(bc.getBaseClassObject(), clsIds);
            }
        }
        if(bc.getBaseInterfacesObject() != null) {
            for(ByteCodeClass c : bc.getBaseInterfacesObject()) {
                if(c != null && !clsIds.contains(c.getClassOffset())) {
                    clsIds.add(c.getClassOffset());
                    if(c.getBaseClassObject() != null) {
                        appendClassOffset(c, clsIds);
                    }
                }
            }
        }
    }

    // Inverted index over the native sources for O(1) "is this symbol referenced
    // by native code" queries (see NativeSymbolIndex). Built lazily and cached
    // against the nativeSources array identity, mirroring the per-method memo in
    // BytecodeMethod.isMethodUsedByNative.
    private static NativeSymbolIndex nativeSymbolIndex;
    private static String[] nativeSymbolIndexSources;
    public static NativeSymbolIndex getNativeSymbolIndex(String[] nativeSources) {
        if (nativeSymbolIndex == null || nativeSymbolIndexSources != nativeSources) {
            nativeSymbolIndex = new NativeSymbolIndex(nativeSources);
            nativeSymbolIndexSources = nativeSources;
        }
        return nativeSymbolIndex;
    }

    private static final ArrayList<String> constantPool = new ArrayList<>();
    
    // Name -> class index, replacing the O(N) linear scans that getClassObject /
    // getClassByName / ByteCodeClass.findClass used to do. Those run per dependency
    // per class during the dead-code cull, so the scans were O(N^2) per pass.
    // Rebuilt lazily when `classes` changes. `classes` is only ever reassigned (new
    // reference) or grown in place via add()/cleared -- it is never mutated to a
    // same-reference, same-size, different-content state -- so the (reference, size)
    // pair uniquely identifies its state and makes this self-correcting.
    private static HashMap<String, ByteCodeClass> classIndexMap;
    private static List<ByteCodeClass> classIndexSource;
    private static int classIndexSize;
    private static HashMap<String, ByteCodeClass> classIndex() {
        if (classIndexMap == null || classIndexSource != classes || classIndexSize != classes.size()) {
            HashMap<String, ByteCodeClass> m = new HashMap<String, ByteCodeClass>(classes.size() * 2);
            for (ByteCodeClass cls : classes) {
                // first-wins, matching the old "return the first match" linear scan
                if (!m.containsKey(cls.getClsName())) {
                    m.put(cls.getClsName(), cls);
                }
            }
            classIndexMap = m;
            classIndexSource = classes;
            classIndexSize = classes.size();
        }
        return classIndexMap;
    }

    public static ByteCodeClass getClassObject(String name) {
        return classIndex().get(name);
    }
    
    /**
     * Adds the given string to the hardcoded constant pool strings returns the offset in the pool
     */
    public static int addToConstantPool(String s) {
        int i = constantPool.indexOf(s);
        if(i < 0) {
            constantPool.add(s);
            return constantPool.size() - 1;
        }
        return i;
    }
    
    
    
    private static void generateClassAndMethodIndexHeader(File outputDirectory) throws Exception {
        int classOffset = 0;
        int methodOffset = 0;
        ArrayList<BytecodeMethod> methods = new ArrayList<>();
        for(ByteCodeClass bc : classes) {
            bc.setClassOffset(classOffset);
            classOffset++;
            
            methodOffset = bc.updateMethodOffsets(methodOffset);
            methods.addAll(bc.getMethods());
        }
        
        StringBuilder bld = new StringBuilder();
        StringBuilder bldM = new StringBuilder();
        bldM.append("#include \"cn1_class_method_index.h\"\n");
        bldM.append("#include \"cn1_globals.h\"\n\n");
        bld.append("#ifndef __CN1_CLASS_METHOD_INDEX_H__\n#define __CN1_CLASS_METHOD_INDEX_H__\n\n");
        
        
        bld.append("// maps to offsets in the constant pool below\nextern int classNameLookup[];\n");
        bldM.append("// maps to offsets in the constant pool below\nint classNameLookup[] = {");
        boolean first = true;
        for(ByteCodeClass bc : classes) {
            if(first) {
                bldM.append("\n    ");
            } else {
                bldM.append(",\n    ");
            }
            first = false;
            bldM.append(addToConstantPool(bc.getClsName().replace('_', '.')));
        }
        bldM.append("};\n\n");
        
        for(ByteCodeClass bc : classes) {
            bld.append("#define cn1_class_id_");
            bld.append(bc.getClsName());
            bld.append(" ");
            bld.append(bc.getClassOffset());
            bld.append("\n");
        }
        
        int arrayId = classes.size() + 1;
        
        bld.append("#define cn1_array_start_offset ");
        bld.append(arrayId);
        bld.append("\n");
        
        // leave space for primitive arrays
        arrayId += 100;
        
        for(ByteCodeClass bc : classes) {
            bld.append("#define cn1_array_1_id_");
            bld.append(bc.getClsName());
            bld.append(" ");
            bld.append(arrayId);
            bld.append("\n");
            arrayId++;

            bld.append("#define cn1_array_2_id_");
            bld.append(bc.getClsName());
            bld.append(" ");
            bld.append(arrayId);
            bld.append("\n");
            arrayId++;

            bld.append("#define cn1_array_3_id_");
            bld.append(bc.getClsName());
            bld.append(" ");
            bld.append(arrayId);
            bld.append("\n");
            arrayId++;
        }

        bld.append("\n\n");

        bld.append("// maps to offsets in the constant pool below\nextern int methodNameLookup[];\n");
        bldM.append("// maps to offsets in the constant pool below\nint methodNameLookup[] = {");
        first = true;
        for(BytecodeMethod m : methods) {
            if(first) {
                bldM.append("\n    ");
            } else {
                bldM.append(",\n    ");
            }
            first = false;
            bldM.append(addToConstantPool(m.getMethodName()));
        }
        bldM.append("};\n\n");
        
        ArrayList<Integer> instances = new ArrayList<>();
        int counter = 0;
        for(ByteCodeClass bc : classes) {
            bldM.append("int classInstanceOfArr");
            bldM.append(counter);
            bldM.append("[] = {");
            counter++;
            appendClassOffset(bc, instances);
            
            for(Integer i : instances) {
                bldM.append(i);
                bldM.append(", ");
            }
            instances.clear();
            bldM.append("-1};\n");
        }
        bld.append("extern int *classInstanceOf[];\n");
        bldM.append("int *classInstanceOf[");
        bldM.append(classes.size());
        bldM.append("] = {");
        first = true;
        int classCount = classes.size();
        for(counter = 0 ; counter < classCount ; counter++) {
            if(first) {
                bldM.append("\n    ");
            } else {
                bldM.append(",\n    ");
            }
            first = false;
            bldM.append("classInstanceOfArr");
            bldM.append(counter);
        }
        bldM.append("};\n\n");
        
        bld.append("#define CN1_CONSTANT_POOL_SIZE ");
        bld.append(constantPool.size());
        bld.append("\n\nextern const char * const constantPool[];\n");

        bldM.append("\n\nconst char * const constantPool[] = {\n");
        first = true;
        int offset = 0;
        for(String con : constantPool) {
            if(first) {
                bldM.append("\n    \"");
            } else {
                bldM.append(",\n    \"");
            }
            first = false;            
            try {
                bldM.append(encodeString(con));
            } catch(Throwable t) {
                t.printStackTrace();
                System.out.println("Error writing the constant pool string: '" + con + "'");
                System.exit(1);
            }
            bldM.append("\" /* ");
            bldM.append(offset);
            offset++;
            bldM.append(" */");
        }
        bldM.append("};\n\nint classListSize = ");
        bldM.append(classes.size());
        bldM.append(";\n");

        for(ByteCodeClass bc : classes) {
            bldM.append("extern struct clazz class__");
            bldM.append(bc.getClsName().replace('/', '_').replace('$', '_'));
            bldM.append(";\n");
        }
        bldM.append("\n\nstruct clazz* classesList[] = {");
        first = true;
        for(ByteCodeClass bc : classes) {
            if(first) {
                bldM.append("\n    ");
            } else {
                bldM.append(",\n    ");
            }
            first = false;
            bldM.append("    &class__");
            bldM.append(bc.getClsName().replace('/', '_').replace('$', '_'));
        }
        bldM.append("};\n\n\n");
        
        // generate the markStatics method
        for(ByteCodeClass bc : classes) {
            bc.appendStaticFieldsExtern(bldM);
        }        
        bldM.append("\n\nextern int recursionKey;\nvoid markStatics(CODENAME_ONE_THREAD_STATE) {\n    recursionKey++;\n");
        for(ByteCodeClass bc : classes) {
            bc.appendStaticFieldsMark(bldM);
        }        
        bldM.append("}\n\n");
        
        
        bld.append("\n\n#endif // __CN1_CLASS_METHOD_INDEX_H__\n");        
        
        FileOutputStream fos = new FileOutputStream(new File(outputDirectory, "cn1_class_method_index.h"));
        fos.write(bld.toString().getBytes(StandardCharsets.UTF_8));
        fos.close();
        fos = new FileOutputStream(new File(outputDirectory, "cn1_class_method_index.m"));
        fos.write(bldM.toString().getBytes(StandardCharsets.UTF_8));
        fos.close();
    }
    
    private static String encodeString(String con) {
        String str = con.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return encodeStringSlashU(str);
    }
    
    private static String encodeStringSlashU(String str) {
        int len = str.length();
        char[] chr = str.toCharArray();
        for(int iter = 0 ; iter < len ; iter++) {
            char c = chr[iter];
            if(c > 127 || c < 32) {
                // needs encoding... Verify there are no more characters to encode
                StringBuilder d = new StringBuilder();
                for(int internal = 0 ; internal < len ; internal++) {
                    c = chr[internal];
                    if(c > 127 || c < 32) {
                        d.append("~~u");
                        d.append(fourChars(Integer.toHexString(c)));
                    } else {
                        d.append(c);
                    }
                }
                return d.toString();
            }
        }
        return str;
    }
    
    private static String fourChars(String s) {
        switch(s.length()) {
            case 1: 
                return "000" + s;
            case 2: 
                return "00" + s;
            case 3: 
                return "0" + s;
        }
        return s;
    }
    
    public static void writeOutput(File outputDirectory) throws Exception {
        if(ByteCodeTranslator.verbose) {
            System.out.println("outputDirectory is: " + outputDirectory.getAbsolutePath() );
        }
        if(ByteCodeClass.getMainClass()==null){
			System.out.println("Error main class is not defined. The main class name is expected to have a public static void main(String[]) method and it is assumed to reside in the com.package.name directory");
			System.exit(1);
		}
        String file = "Unknown File";
        List<ByteCodeClass> javascriptClassPool = ByteCodeTranslator.output
                == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT
                ? new ArrayList<ByteCodeClass>(classes) : null;
        try {
            for(ByteCodeClass bc : classes) {
                // special case for an object
                if(bc.getClsName().equals("java_lang_Object")) {
                    continue;
                }
                file = bc.getClsName();
                bc.setBaseClassObject(getClassByName(bc.getBaseClass()));
                List<ByteCodeClass> lst = new ArrayList<>();
                for(String s : bc.getBaseInterfaces()) {
					ByteCodeClass byteCode = getClassByName(s);
					if(byteCode == null){
					  System.out.println("Error while working with the class: " + s+" file:"+file+" no class definition");
					} else {
						lst.add(getClassByName(s));
					}
                }
                bc.setBaseInterfacesObject(lst);
            }
            boolean foundNewUnitTests = true;
            while (foundNewUnitTests) {
                foundNewUnitTests = false;
                for (ByteCodeClass bc : classes) {
                    if (!bc.isUnitTest() && bc.getBaseClassObject() != null && bc.getBaseClassObject().isUnitTest()) {
                        bc.setIsUnitTest(true);
                        foundNewUnitTests = true;
                    }
                }
            }

            // load the native sources (including user native code)
            // We need to load native sources before we clear any unmarked classes
            // because a native source may be the only thing referencing a class,
            // and the class may be purged before it even has a shot.
            readNativeFiles(outputDirectory);

            for(ByteCodeClass bc : classes) {
                file = bc.getClsName();
                bc.updateAllDependencies();
            }
            ByteCodeClass.markDependencies(classes, nativeSources);
            Set<ByteCodeClass> unmarked = new HashSet<>(classes);
            classes = ByteCodeClass.clearUnmarked(classes);
            classes.forEach(unmarked::remove);
            int neliminated = 0;
            for (ByteCodeClass removedClass : unmarked) {
                removedClass.setEliminated(true);
                neliminated++;
            }

            // loop over methods and start eliminating the body of unused methods
            if (BytecodeMethod.optimizerOn) {
                if(ByteCodeTranslator.verbose) {
                    System.out.println("Optimizer On: Removing unused methods and classes...");
                }
                Date now = new Date();
                neliminated += eliminateUnusedMethods();
                Date later = new Date();
                long dif = later.getTime()-now.getTime();
                if(ByteCodeTranslator.verbose) {
                    System.out.println("unused Method cull removed "+neliminated+" methods in "+(dif/1000)+" seconds");
                }
            }

            // JavaScript-target-only Rapid Type Analysis pass. Runs AFTER
            // the existing desc.name-keyed culler so we start from an
            // already-pruned class list. RTA only eliminates additional
            // methods the conservative graph considered "used" because
            // some OTHER class with the same name+desc was invoked; it
            // never resurrects methods the earlier pass removed. Gated
            // on OUTPUT_TYPE_JAVASCRIPT because the iOS runtime relies on
            // different dispatch mechanics and may break under stricter
            // reachability.
            if (BytecodeMethod.optimizerOn
                    && ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT
                    && System.getProperty("parparvm.js.rta.off") == null) {
                Date rtaStart = new Date();
                int rtaEliminated = JavascriptReachability.run(
                        classes, javascriptClassPool, nativeSources);
                Date rtaEnd = new Date();
                long rtaDif = rtaEnd.getTime() - rtaStart.getTime();
                if (ByteCodeTranslator.verbose) {
                    System.out.println("JS RTA pass removed " + rtaEliminated + " additional methods in "
                            + (rtaDif / 1000) + " seconds");
                }
                neliminated += rtaEliminated;
            }

            // JavaScript-target-only suspension analysis: decide which
            // surviving methods can be emitted as plain ``function``
            // (no generator allocation / no yield*) vs which must stay
            // as ``function*``. Must run after RTA so eliminated
            // methods don't pollute the analysis.
            if (ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT) {
                Date suspStart = new Date();
                int syncCount = JavascriptSuspensionAnalysis.run(classes);
                Date suspEnd = new Date();
                if (ByteCodeTranslator.verbose) {
                    System.out.println("JS suspension analysis: " + syncCount
                            + " methods classified synchronous in "
                            + ((suspEnd.getTime() - suspStart.getTime()) / 1000) + " seconds");
                }
            }

            if (ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT) {
                JavascriptBundleWriter.write(outputDirectory, classes);
            } else {
                generateClassAndMethodIndexHeader(outputDirectory);

                boolean concatenate = "true".equals(System.getProperty("concatenateFiles", "false"));
                ConcatenatingFileOutputStream cos = concatenate ? new ConcatenatingFileOutputStream(outputDirectory) : null;

                for(ByteCodeClass bc : classes) {
                    file = bc.getClsName();
                    writeFile(bc, outputDirectory, cos);
                }
                if (cos != null) cos.realClose();

                if (BytecodeMethod.isOnDeviceDebug()) {
                    writeSymbolSidecar(outputDirectory);
                }
            }
        } catch(Throwable t) {
            System.out.println("Error while working with the class: " + file);
            t.printStackTrace();
            if(t instanceof Exception) {
                throw (Exception)t;
            }
            // Errors (notably OutOfMemoryError while emitting a very large
            // bundle) previously fell through here, so the translator exited
            // 0 with a half-written dist (e.g. parparvm_runtime.js but no
            // worker.js / translated_app.js). Rethrow so the caller fails
            // loudly instead of shipping a truncated app bundle.
            if(t instanceof Error) {
                throw (Error)t;
            }
            throw new RuntimeException(t);
        }
        finally { cleanup(); }
    }
    
    private static void readNativeFiles(File outputDirectory) throws IOException {
        File[] mFiles = outputDirectory.listFiles(file ->
                file.getName().endsWith(".m") || file.getName().endsWith("." + ByteCodeTranslator.output.extension()));
        if(mFiles == null) {
            return;
        }
        nativeSources = new String[mFiles.length];
        int size = 0;
        if(ByteCodeTranslator.verbose) {
            System.out.println(mFiles.length + " native files");
        }
        for(int iter = 0 ; iter < mFiles.length ; iter++) { 
        	FileInputStream fi = new FileInputStream(mFiles[iter]);
            DataInputStream di = new DataInputStream(fi);
            int len = (int)mFiles[iter].length();
            size += len;
            byte[] dat = new byte[len];
            di.readFully(dat);
            fi.close();
            nativeSources[iter] = new String(dat, StandardCharsets.UTF_8);
        }
        if(ByteCodeTranslator.verbose) {
            System.out.println("Native files total "+(size/1024)+"K");
        }
    }
    
    private static int eliminateUnusedMethods() {
        return(eliminateUnusedMethods(false, 0));
    }

    private static int eliminateUnusedMethods(boolean forceFound, int depth) {
        int nfound = cullMethods();
        nfound += cullClasses(nfound>0 || forceFound, depth);
        return(nfound);
    }

    private static int cullMethods() {
        int nfound = 0;
        for(ByteCodeClass bc : classes) {
            bc.unmark();
            if(bc.isIsInterface() || bc.getBaseClass() == null) {
                continue;
            }
            for(BytecodeMethod mtd : bc.getMethods()) {
                // Pure-Java twins that the JS runtime's bindNative delegates call
                // (getImpl/putImpl/toStringImpl/valueOfHeap...): no bytecode call
                // site exists, so without this keep they would be culled and the
                // JS delegation would throw ReferenceError. Kept on all targets --
                // the C natives use some of them as fallbacks too.
                if(JavascriptNativeRegistry.isRuntimeDelegateTarget(bc.getClsName(), mtd.getMethodName())) {
                    continue;
                }
                if(mtd.isEliminated() || mtd.isMain() || mtd.getMethodName().equals("__CLINIT__") || mtd.getMethodName().equals("finalize") || mtd.isNative()) {
                    if (!mtd.isEliminated() && mtd.getMethodName().contains("yield")) {
                        if(ByteCodeTranslator.verbose) {
                            System.out.println("Not eliminating method ");
                            System.out.println("main="+mtd.isMain()+", isNative="+mtd.isNative());
                        }
                    }
                    continue;
                }
                // Preserve virtual-root methods of java.lang.Object when
                // on-device-debug is on. jdb's `print` formats objects by
                // calling Object.toString, and a debugger user can ask to
                // invoke equals/hashCode/etc. without a static call site
                // to keep their body alive on its own.
                if (BytecodeMethod.isOnDeviceDebug()
                        && "java_lang_Object".equals(bc.getClsName())
                        && !mtd.isStatic()) {
                    continue;
                }

                if(!isMethodUsed(mtd, bc)) {
                    if(isMethodUsedByBaseClassOrInterface(mtd, bc)) {
                        continue;
                    }
                    mtd.setEliminated(true);
                    dependencyGraph.removeMethod(mtd);
                    nfound++;
                }
            }

        }
        return nfound;
    }

    private static boolean isMethodUsedByBaseClassOrInterface(BytecodeMethod mtd, ByteCodeClass cls) {
        boolean b = checkMethodUsedByBaseClassOrInterface(mtd, cls.getBaseClassObject());
        if(b) {
            return true;
        }
        for(ByteCodeClass bc : cls.getBaseInterfacesObject()) {
            b = checkMethodUsedByBaseClassOrInterface(mtd, bc);
            if(b) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkMethodUsedByBaseClassOrInterface(BytecodeMethod mtd, ByteCodeClass cls) {
        if(cls == null) {
            return false;
        }
        if(cls.getBaseInterfacesObject() != null) {
            for(ByteCodeClass bc : cls.getBaseInterfacesObject()) {
                for(BytecodeMethod m :  bc.getMethods()) {
                    if (m.isEliminated()) continue;
                    if(m.getMethodName().equals(mtd.getMethodName())) {
                        if (isMethodUsed(m, bc)) {
                            return true;
                        }
                        break;
                    }
                }
            }
        }
        for(BytecodeMethod m :  cls.getMethods()) {
            if (m.isEliminated()) continue;
            if(m.getMethodName().equals(mtd.getMethodName())) {
                if(isMethodUsed(m, cls)) {
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private static int cullClasses(boolean found, int depth) {
        if(ByteCodeTranslator.verbose) {
            System.out.println("cullClasses()");
        }
        if(found && depth < 4) {
            for(ByteCodeClass bc : classes) {
                bc.updateAllDependencies();
            }   

            ByteCodeClass.markDependencies(classes, nativeSources);
            List<ByteCodeClass> tmp = ByteCodeClass.clearUnmarked(classes);

            // 2nd pass to mark classes as eliminated so that we can propagate down to each
            // method of the class to mark it eliminated so that virtual methods
            // aren't included later on when writing virtual methods
            Set<ByteCodeClass> removedClasses = new HashSet<>(classes);
            tmp.forEach(removedClasses::remove);
            int nfound = 0;
            for (ByteCodeClass cls : removedClasses) {
                nfound += cls.setEliminated(true);
                dependencyGraph.removeClass(cls.getClsName());
            }
            classes = tmp;
            return nfound + eliminateUnusedMethods(nfound > 0, depth + 1);
        }

        // Note: We may still have a lot of classes that are kept around solely because
        // they implement an interface that is used.
        // We should try to remove such classes
        return 0;
    }
    

    
    private static boolean isMethodUsed(BytecodeMethod m, ByteCodeClass cls) {
        if (!m.isEliminated() && m.isMethodUsedByNative(nativeSources, cls)) {
            return true;
        }
        List<BytecodeMethod> callers = dependencyGraph.getCallers(m.getLookupSignature());
        for (BytecodeMethod caller : callers) {
            if(caller.isEliminated() || caller == m) {
                continue;
            }
            if(caller.isMethodUsed(m)) {
                return true;
            }
        }
        return false;
    }

    private static void writeFile(ByteCodeClass cls, File outputDir, ConcatenatingFileOutputStream writeBufferInstead) throws Exception {
        OutputStream outMain =
                writeBufferInstead != null && ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_IOS ?
                        writeBufferInstead :
                        Files.newOutputStream(new File(outputDir, cls.getClsName() + "." + ByteCodeTranslator.output.extension()).toPath());

        if (outMain instanceof ConcatenatingFileOutputStream) {
            ((ConcatenatingFileOutputStream)outMain).beginNextFile(cls.getClsName());
        }
        if (ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT) {
            outMain.write(cls.generateJavascriptCode(classes).getBytes(StandardCharsets.UTF_8));
            outMain.close();
        } else {
            outMain.write(cls.generateCCode(classes).getBytes(StandardCharsets.UTF_8));
            outMain.close();

            // we also need to write the header file for C outputs
            String headerName = cls.getClsName() + ".h";
            try(FileOutputStream outHeader = new FileOutputStream(new File(outputDir, headerName))) {
                outHeader.write(cls.generateCHeader().getBytes(StandardCharsets.UTF_8));
            }
        }
    }
    
    public Parser() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visitEnd() {
        super.visitEnd(); 
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        BytecodeMethod mtd = new BytecodeMethod(clsName, access, name, desc, signature, exceptions);
        cls.addMethod(mtd);
        // Tee the (post-JSR-inlined) bytecode into a MethodNode so visitEnd can run
        // ASM frame analysis to resolve category-2-aware DUP/POP2 forms. The wrapper
        // sits INSIDE the JSRInlinerAdapter, so the MethodNode it feeds matches the
        // instruction stream BytecodeMethod is built from. Parser's own ClassVisitor
        // has no delegate writer (super.visitMethod returns null), so routing the
        // MethodNode as the wrapper's delegate loses nothing.
        MethodNode analysisNode = new MethodNode(Opcodes.ASM9, access, name, desc, signature, exceptions);
        MethodVisitorWrapper wrapper = new MethodVisitorWrapper(analysisNode, mtd);
        wrapper.dupAnalysisOwner = clsName;
        wrapper.dupAnalysisNode = analysisNode;
        return new JSRInlinerAdapter(wrapper, access, name, desc, signature, exceptions);
    }

    // Category-sensitive stack opcodes: their correct operand-stack-ENTRY shuffle
    // depends on whether the operands are category-2 (long/double), because the JS
    // backend models a long/double as ONE entry while the JVM defines these in slots.
    private static boolean isCategorySensitiveStackOp(int op) {
        return op == Opcodes.DUP2 || op == Opcodes.DUP2_X1 || op == Opcodes.DUP2_X2
                || op == Opcodes.DUP_X2 || op == Opcodes.POP2;
    }

    /**
     * Runs ASM frame analysis over the parsed MethodNode and stamps each
     * category-sensitive DUP/POP2 ``BasicInstruction`` with its resolved
     * entry-form (see {@link BasicInstruction#setDupForm}). On any analysis
     * failure the forms are left unset and the emitter uses its legacy
     * (category-1-assuming) path -- i.e. no worse than before.
     */
    private static void resolveDupForms(String owner, MethodNode mn, BytecodeMethod mtd) {
        if (owner == null || mn == null || mn.instructions == null || mn.instructions.size() == 0) {
            return;
        }
        Frame<BasicValue>[] frames;
        try {
            frames = new Analyzer<BasicValue>(new BasicInterpreter()).analyze(owner, mn);
        } catch (Throwable t) {
            return;
        }
        // Decisions for category-sensitive ops, in linear (parse) order.
        List<int[]> decisions = new ArrayList<int[]>();
        AbstractInsnNode[] insns = mn.instructions.toArray();
        for (int i = 0; i < insns.length; i++) {
            int op = insns[i].getOpcode();
            if (!isCategorySensitiveStackOp(op)) {
                continue;
            }
            decisions.add(dupForm(op, frames[i]));
        }
        // Stamp the matching ParparVM BasicInstructions, in the same linear order.
        // The dup/pop2 sequence is identical between the ASM node and the
        // BytecodeMethod (both built from the same post-JSR stream), so they zip
        // 1:1. Stamping the instruction object (not a positional queue) keeps the
        // form correct even though the structured emitter later reorders blocks.
        int di = 0;
        for (Instruction instr : mtd.getInstructions()) {
            if (!(instr instanceof BasicInstruction) || !isCategorySensitiveStackOp(instr.getOpcode())) {
                continue;
            }
            if (di >= decisions.size()) {
                break;
            }
            int[] form = decisions.get(di++);
            if (form != null) {
                ((BasicInstruction) instr).setDupForm(form[0], form[1]);
            }
        }
    }

    /**
     * Resolves a category-sensitive stack opcode into entry terms given the frame
     * BEFORE it. Returns {nDup, nSkip} for the DUP family (duplicate the top nDup
     * entries, reinsert beneath the next nSkip), or {entriesToPop, -1} for POP2.
     * Returns null when the frame is unavailable (unreachable code).
     * ASM analysis frames model a long/double as ONE stack entry with size 2.
     */
    private static int[] dupForm(int op, Frame<BasicValue> f) {
        if (f == null) {
            return null;
        }
        int sp = f.getStackSize();
        if (sp < 1) {
            return null;
        }
        boolean topWide = f.getStack(sp - 1).getSize() == 2;
        switch (op) {
            case Opcodes.POP2:
                return new int[]{ topWide ? 1 : 2, -1 };
            case Opcodes.DUP2:
                return topWide ? new int[]{1, 0} : new int[]{2, 0};
            case Opcodes.DUP2_X1:
                return topWide ? new int[]{1, 1} : new int[]{2, 1};
            case Opcodes.DUP_X2: {
                // Top is category-1; skip one entry if the value below is category-2.
                boolean belowWide = sp >= 2 && f.getStack(sp - 2).getSize() == 2;
                return belowWide ? new int[]{1, 1} : new int[]{1, 2};
            }
            case Opcodes.DUP2_X2:
                if (topWide) {
                    boolean belowWide = sp >= 2 && f.getStack(sp - 2).getSize() == 2;
                    return belowWide ? new int[]{1, 1} : new int[]{1, 2};
                } else {
                    // Top two entries are category-1; skip one entry if the value
                    // beneath them is category-2.
                    boolean belowWide = sp >= 3 && f.getStack(sp - 3).getSize() == 2;
                    return belowWide ? new int[]{2, 1} : new int[]{2, 2};
                }
            default:
                return null;
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        ByteCodeField fld = new ByteCodeField(clsName, access, name, desc, signature, value);
        cls.addField(fld);
        return new FieldVisitorWrapper(super.visitField(access, name, desc, signature, value));
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if(name.equals(cls.getOriginalClassName())) {
            cls.setIsAnonymous(innerName==null);
        }
        super.visitInnerClass(name, outerName, innerName, access); 
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr); 
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return new AnnotationVisitorWrapper(super.visitTypeAnnotation(typeRef, typePath, desc, visible)); 
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (STACK_ALLOCATE_ANNOTATION.equals(desc)) {
            cls.setStackAllocatable(true);
            return new AnnotationVisitorWrapper(super.visitAnnotation(desc, visible));
        }
        if (FUSED_ANNOTATION.equals(desc)) {
            cls.setFused(true);
            return new AnnotationVisitorWrapper(super.visitAnnotation(desc, visible));
        }
        if (CONCRETE_ANNOTATION.equals(desc)) {
            return new AnnotationVisitorWrapper(super.visitAnnotation(desc, visible)) {
                private String defaultConcrete;
                private String winConcrete;
                private String linuxConcrete;

                @Override
                public void visit(String name, Object value) {
                    if ("name".equals(name) && value instanceof String) {
                        defaultConcrete = (String) value;
                    } else if ("win".equals(name) && value instanceof String) {
                        winConcrete = (String) value;
                    } else if ("linux".equals(name) && value instanceof String) {
                        linuxConcrete = (String) value;
                    }
                    super.visit(name, value);
                }

                @Override
                public void visitEnd() {
                    // Pick the concrete implementation for the active translation
                    // target: the native Windows build uses @Concrete.win(), the
                    // native Linux build uses @Concrete.linux(), every other target
                    // uses @Concrete.name() (the iOS pipeline). When building
                    // Windows/Linux and no win()/linux() is given (e.g. IOSSimd,
                    // which has only an iOS specialization), leave the concrete
                    // unset so the portable base class is translated instead of
                    // pulling in the absent iOS class.
                    String target = ByteCodeClass.getConcreteTarget();
                    String concrete;
                    if ("win".equals(target)) {
                        concrete = winConcrete;
                    } else if ("linux".equals(target)) {
                        concrete = linuxConcrete;
                    } else {
                        concrete = defaultConcrete;
                    }
                    if (concrete != null && concrete.length() > 0) {
                        cls.setConcreteClass(concrete.replace('.', '/'));
                    }
                    super.visitEnd();
                }
            };
        }
        return new AnnotationVisitorWrapper(super.visitAnnotation(desc, visible));
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        super.visitOuterClass(owner, name, desc); 
    }

    @Override
    public void visitSource(String source, String debug) {
        cls.setSourceFile(source);
        super.visitSource(source, debug); 
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cls.setBaseClass(superName);
        cls.setBaseInterfaces(interfaces);
        if((access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT) {
            cls.setIsAbstract(true);
        }
        if((access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE) {
            cls.setIsInterface(true);
        }
        if((access & Opcodes.ACC_ANNOTATION) == Opcodes.ACC_ANNOTATION) {
            cls.setIsAnnotation(true);
        }
        if((access & Opcodes.ACC_SYNTHETIC) == Opcodes.ACC_SYNTHETIC) {
            cls.setIsSynthetic(true);
        }
        
        if((access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
            cls.setFinalClass(true);
        }
        if ("com/codename1/testing/UnitTest".equals(superName) || "com/codename1/testing/AbstractTest".equals(superName)) {
            cls.setIsUnitTest(true);
        }
        if ((access & Opcodes.ACC_ENUM) == Opcodes.ACC_ENUM) {
            cls.setIsEnum(true);
        }
        super.visit(version, access, name, signature, superName, interfaces); 
    }    
    
    class MethodVisitorWrapper extends MethodVisitor {
        private final BytecodeMethod mtd;
        String dupAnalysisOwner;
        MethodNode dupAnalysisNode;
        public MethodVisitorWrapper(MethodVisitor mv, BytecodeMethod mtd) {
            super(Opcodes.ASM9, mv);
            this.mtd = mtd;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            // LEVER B: snapshot the inlinable-constructor plan from the RAW instruction
            // list now, before optimize() (run later, per-class) folds the PUTFIELDs.
            mtd.computeInlinableConstructorPlan();
            resolveDupForms(dupAnalysisOwner, dupAnalysisNode, mtd);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            mtd.setMaxes(maxStack, maxLocals);
            super.visitMaxs(maxStack, maxLocals); 
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            mtd.addDebugInfo(line);
            super.visitLineNumber(line, start); 
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
            return new AnnotationVisitorWrapper(super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible));
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            mtd.addLocalVariable(name, desc, signature, start, end, index);
            super.visitLocalVariable(name, desc, signature, start, end, index); 
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return new AnnotationVisitorWrapper(super.visitTryCatchAnnotation(typeRef, typePath, desc, visible));
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            mtd.addTryCatchBlock(start, end, handler, type);
            super.visitTryCatchBlock(start, end, handler, type); 
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return new AnnotationVisitorWrapper(super.visitInsnAnnotation(typeRef, typePath, desc, visible)); 
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            mtd.addMultiArray(desc, dims);
            super.visitMultiANewArrayInsn(desc, dims); 
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            mtd.addSwitch(dflt, keys, labels);
            super.visitLookupSwitchInsn(dflt, keys, labels); 
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            int[] keys = new int[labels.length];
            int counter = min;
            for(int iter = 0 ; iter < keys.length ; iter++) {
                keys[iter] = counter;
                counter++;
            }
            mtd.addSwitch(dflt, keys, labels);
            super.visitTableSwitchInsn(min, max, dflt, labels); 
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            mtd.addIInc(var, increment);
            super.visitIincInsn(var, increment); 
        }

        @Override
        public void visitLdcInsn(Object cst) {
            mtd.addLdc(cst);
            super.visitLdcInsn(cst); 
        }

        @Override
        public void visitLabel(Label label) {
            mtd.addLabel(label);
            super.visitLabel(label); 
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            mtd.addJump(opcode, label);
            super.visitJumpInsn(opcode, label); 
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            if ("java/lang/invoke/StringConcatFactory".equals(bsm.getOwner()) &&
                ("makeConcatWithConstants".equals(bsm.getName()) || "makeConcat".equals(bsm.getName()))) {

                Type invokedType = Type.getMethodType(desc);
                if (!Type.getType(String.class).equals(invokedType.getReturnType())) {
                    super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
                    return;
                }

                String helperName = "cn1$concat$" + (stringConcatCounter++);
                BytecodeMethod helper = new BytecodeMethod(clsName, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, helperName, desc, null, null);
                cls.addMethod(helper);

                // FAST PATH: when every argument is already String-typed (the common interpolation /
                // a + b + c shape), build the result compactly in one pass via String.cn1ConcatN
                // instead of the char[]-backed StringBuilder -- which decodes each compact byte[] arg
                // to char on append and re-encodes to byte in toString, over a 2-byte/char scratch
                // buffer (4 allocations + 2 conversions per concat). cn1ConcatN is 2 allocations and
                // no conversion. Only for 2..5 total parts (constants + args); anything else, or a
                // non-String arg, falls through to the general StringBuilder helper below.
                boolean cn1AllStringArgs = invokedType.getArgumentTypes().length > 0;
                for (Type at : invokedType.getArgumentTypes()) {
                    if (at.getSort() != Type.OBJECT || !"java/lang/String".equals(at.getInternalName())) {
                        cn1AllStringArgs = false;
                        break;
                    }
                }
                if (cn1AllStringArgs) {
                    // Ordered parts: a String means "literal/constant -> LDC", an Integer means
                    // "arg at that local -> ALOAD". Empty literals are dropped.
                    List<Object> cn1Parts = new ArrayList<>();
                    Type[] cn1Ats = invokedType.getArgumentTypes();
                    if ("makeConcat".equals(bsm.getName())) {
                        int li = 0;
                        for (Type at : cn1Ats) { cn1Parts.add(Integer.valueOf(li)); li += at.getSize(); }
                    } else {
                        String rcp = bsmArgs != null && bsmArgs.length > 0 ? String.valueOf(bsmArgs[0]) : "";
                        List<String> csts = new ArrayList<>();
                        if (bsmArgs != null) {
                            for (int i = 1; i < bsmArgs.length; i++) csts.add(String.valueOf(bsmArgs[i]));
                        }
                        int ci = 0, li = 0, ai = 0;
                        StringBuilder lit = new StringBuilder();
                        for (int i = 0; i < rcp.length(); i++) {
                            char ch = rcp.charAt(i);
                            if (ch == '\u0001') {
                                if (lit.length() > 0) { cn1Parts.add(lit.toString()); lit.setLength(0); }
                                if (ai < cn1Ats.length) { cn1Parts.add(Integer.valueOf(li)); li += cn1Ats[ai++].getSize(); }
                            } else if (ch == '\u0002') {
                                if (lit.length() > 0) { cn1Parts.add(lit.toString()); lit.setLength(0); }
                                if (ci < csts.size()) cn1Parts.add(csts.get(ci++));
                            } else {
                                lit.append(ch);
                            }
                        }
                        if (lit.length() > 0) cn1Parts.add(lit.toString());
                        while (ai < cn1Ats.length) { cn1Parts.add(Integer.valueOf(li)); li += cn1Ats[ai++].getSize(); }
                    }
                    int cn1N = cn1Parts.size();
                    if (cn1N >= 2 && cn1N <= 5) {
                        for (Object p : cn1Parts) {
                            if (p instanceof String) {
                                helper.addLdc((String) p);
                            } else {
                                helper.addVariableOperation(Opcodes.ALOAD, ((Integer) p).intValue());
                            }
                        }
                        StringBuilder cn1Sig = new StringBuilder("(");
                        for (int k = 0; k < cn1N; k++) cn1Sig.append("Ljava/lang/String;");
                        cn1Sig.append(")Ljava/lang/String;");
                        helper.addInvoke(Opcodes.INVOKESTATIC, "java/lang/String", "cn1Concat" + cn1N, cn1Sig.toString(), false);
                        helper.addInstruction(Opcodes.ARETURN);
                        int cn1MaxLocal = 0;
                        for (Type t : cn1Ats) cn1MaxLocal += t.getSize();
                        helper.setMaxes(cn1N + 1, cn1MaxLocal + 2);
                        mtd.addInvoke(Opcodes.INVOKESTATIC, clsName, helperName, desc, false);
                        return;
                    }
                }

                // Pre-size the StringBuilder from the recipe literals + per-argument length
                // estimates so the common-case concat never grows its char[] (each growth is
                // a fresh array + arraycopy). Over-estimates are harmless; under-estimates
                // (e.g. a long String arg) still grow correctly.
                int cn1Cap = 0;
                if ("makeConcatWithConstants".equals(bsm.getName())) {
                    String rcp = bsmArgs != null && bsmArgs.length > 0 ? String.valueOf(bsmArgs[0]) : "";
                    for (int ci = 0; ci < rcp.length(); ci++) {
                        char rc = rcp.charAt(ci);
                        if (rc != 0x0001 && rc != 0x0002) cn1Cap++; // skip arg/const markers
                    }
                    if (bsmArgs != null) {
                        for (int ci = 1; ci < bsmArgs.length; ci++) cn1Cap += String.valueOf(bsmArgs[ci]).length();
                    }
                }
                for (Type at : invokedType.getArgumentTypes()) {
                    switch (at.getSort()) {
                        case Type.LONG: cn1Cap += 20; break;
                        case Type.DOUBLE: cn1Cap += 24; break;
                        case Type.FLOAT: cn1Cap += 15; break;
                        case Type.BOOLEAN: cn1Cap += 5; break;
                        case Type.CHAR: cn1Cap += 1; break;
                        case Type.OBJECT: case Type.ARRAY: cn1Cap += 16; break;
                        default: cn1Cap += 11; break; // int/short/byte
                    }
                }
                if (cn1Cap < 16) cn1Cap = 16; else if (cn1Cap > 8192) cn1Cap = 8192;
                helper.addTypeInstruction(Opcodes.NEW, "java/lang/StringBuilder");
                helper.addInstruction(Opcodes.DUP);
                helper.addInstruction(Opcodes.SIPUSH, cn1Cap);
                helper.addInvoke(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(I)V", false);

                Type[] argTypes = invokedType.getArgumentTypes();
                int maxLocal = 0;
                for (Type t : argTypes) {
                    maxLocal += t.getSize();
                }

                int localIndex = 0;
                int argIndex = 0;
                if ("makeConcat".equals(bsm.getName())) {
                    for (Type argType : argTypes) {
                        appendConcatArgument(helper, argType, localIndex);
                        localIndex += argType.getSize();
                    }
                } else {
                    String recipe = bsmArgs != null && bsmArgs.length > 0 ? String.valueOf(bsmArgs[0]) : "";
                    List<String> constants = new ArrayList<>();
                    if (bsmArgs != null) {
                        for (int i = 1; i < bsmArgs.length; i++) {
                            constants.add(String.valueOf(bsmArgs[i]));
                        }
                    }
                    int constantIndex = 0;
                    StringBuilder literal = new StringBuilder();
                    for (int i = 0; i < recipe.length(); i++) {
                        char ch = recipe.charAt(i);
                        if (ch == '\u0001') {
                            appendConcatLiteral(helper, literal);
                            literal.setLength(0);
                            if (argIndex < argTypes.length) {
                                Type argType = argTypes[argIndex++];
                                appendConcatArgument(helper, argType, localIndex);
                                localIndex += argType.getSize();
                            }
                        } else if (ch == '\u0002') {
                            appendConcatLiteral(helper, literal);
                            literal.setLength(0);
                            if (constantIndex < constants.size()) {
                                appendConcatLiteral(helper, constants.get(constantIndex++));
                            }
                        } else {
                            literal.append(ch);
                        }
                    }
                    appendConcatLiteral(helper, literal);
                    while (argIndex < argTypes.length) {
                        Type argType = argTypes[argIndex++];
                        appendConcatArgument(helper, argType, localIndex);
                        localIndex += argType.getSize();
                    }
                }

                helper.addInvoke(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                helper.addInstruction(Opcodes.ARETURN);
                helper.setMaxes(16, maxLocal + 2);

                mtd.addInvoke(Opcodes.INVOKESTATIC, clsName, helperName, desc, false);
                return;
            }

            if ("java/lang/invoke/LambdaMetafactory".equals(bsm.getOwner()) &&
                ("metafactory".equals(bsm.getName()) || "altMetafactory".equals(bsm.getName()))) {

                // 1. Generate a unique class name for the lambda
                String lambdaClassName = clsName + "_lambda_" + (lambdaCounter++);

                // 2. Create the ByteCodeClass for the lambda
                ByteCodeClass lambdaClass = new ByteCodeClass(lambdaClassName, lambdaClassName.replace('_', '/'));
                lambdaClass.setBaseClass("java/lang/Object");

                // The interface implemented is the return type of the invokedynamic descriptor
                Type invokedType = Type.getMethodType(desc);
                Type interfaceType = invokedType.getReturnType();
                lambdaClass.setBaseInterfaces(new String[]{interfaceType.getInternalName()});

                // 3. Add fields for captured arguments
                Type[] capturedArgs = invokedType.getArgumentTypes();
                for (int i = 0; i < capturedArgs.length; i++) {
                    String fieldName = "arg$" + (i + 1);
                    String fieldDesc = capturedArgs[i].getDescriptor();
                    ByteCodeField field = new ByteCodeField(lambdaClassName, Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, fieldDesc, null, null);
                    lambdaClass.addField(field);
                }

                // 4. Add Constructor
                StringBuilder ctorDesc = new StringBuilder("(");
                for (Type t : capturedArgs) {
                    ctorDesc.append(t.getDescriptor());
                }
                ctorDesc.append(")V");

                BytecodeMethod ctor = new BytecodeMethod(lambdaClassName, Opcodes.ACC_PUBLIC, "<init>", ctorDesc.toString(), null, null);
                lambdaClass.addMethod(ctor);

                // Constructor body (we need to generate instructions manually)
                // ALOAD 0
                // INVOKESPECIAL java/lang/Object.<init>
                // ... assign fields ...
                // RETURN

                // NOTE: do NOT also emit `addInstruction(Opcodes.ALOAD)` here.
                // `addVariableOperation(Opcodes.ALOAD, 0)` is the canonical aload_0.
                // Emitting both produces a BasicInstruction with opcode=ALOAD and value=0
                // that the C backend (iOS) silently ignores but the JavaScript backend
                // translates as a real `push locals[0]`. The extra push corrupts the
                // operand stack simulation: invokespecial/putfield/invokevirtual then
                // pop from the wrong positions, producing method calls with the wrong
                // target and shifted arguments (e.g. lambda `run()` ending up invoking
                // its captured method on the captured Form rather than on the
                // enclosing `this`, surfacing as VIRTUAL_FAIL missing_interface_default_method).
                ctor.addVariableOperation(Opcodes.ALOAD, 0);
                ctor.addInvoke(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

                int varIndex = 1;
                for (int i = 0; i < capturedArgs.length; i++) {
                    ctor.addVariableOperation(Opcodes.ALOAD, 0); // this

                    Type t = capturedArgs[i];
                    int opcode = t.getOpcode(Opcodes.ILOAD); // correct load opcode for type
                    ctor.addVariableOperation(opcode, varIndex);
                    varIndex += t.getSize();

                    String fieldName = "arg$" + (i + 1);
                    ctor.addField(lambdaClass, Opcodes.PUTFIELD, lambdaClassName, fieldName, t.getDescriptor());
                }
                ctor.addInstruction(Opcodes.RETURN);
                ctor.setMaxes(varIndex + 1, varIndex); // Approximate maxes


                // 5. Implement the interface method
                Type samMethodType = (Type) bsmArgs[0];
                Handle implMethod = (Handle) bsmArgs[1];

                // Name from invokedynamic
                String samMethodDesc = samMethodType.getDescriptor(); // Signature from BSM arg 0

                BytecodeMethod interfaceMethod = new BytecodeMethod(lambdaClassName, Opcodes.ACC_PUBLIC, name, samMethodDesc, null, null);
                lambdaClass.addMethod(interfaceMethod);

                // Method Body:
                // Load captured arguments from fields
                // Load method arguments
                // Invoke implMethod
                // Return result

                // Determine how the implementation method is invoked up front:
                // we need to know whether it has an implicit receiver (an
                // instance call consumes the first captured arg as `this`)
                // before we can line captured/SAM args up against the target
                // parameter types for adaptation.
                boolean isCtorRef = (implMethod.getTag() == Opcodes.H_NEWINVOKESPECIAL);
                int invokeOpcode;
                switch (implMethod.getTag()) {
                    case Opcodes.H_INVOKESTATIC: invokeOpcode = Opcodes.INVOKESTATIC; break;
                    case Opcodes.H_INVOKEVIRTUAL: invokeOpcode = Opcodes.INVOKEVIRTUAL; break;
                    case Opcodes.H_INVOKEINTERFACE: invokeOpcode = Opcodes.INVOKEINTERFACE; break;
                    case Opcodes.H_INVOKESPECIAL:
                    case Opcodes.H_NEWINVOKESPECIAL:
                        invokeOpcode = Opcodes.INVOKESPECIAL; break;
                    default:
                        invokeOpcode = Opcodes.INVOKESTATIC;
                        break;// Fallback
                }
                boolean instanceCall = !isCtorRef &&
                        (invokeOpcode == Opcodes.INVOKEVIRTUAL ||
                         invokeOpcode == Opcodes.INVOKEINTERFACE ||
                         invokeOpcode == Opcodes.INVOKESPECIAL);

                // The values the implementation invocation consumes, in order:
                // the receiver (for instance calls) followed by the declared
                // parameter types. LambdaMetafactory adapts each captured/SAM
                // argument to the matching target type (box, unbox, widen or
                // cast) -- e.g. a SAM that hands us a Double bound to a method
                // taking a primitive double must unbox via doubleValue(). We
                // must replicate that here; loading the SAM args verbatim and
                // invoking the impl method directly emits a C call that passes a
                // JAVA_OBJECT where a primitive (or vice versa) is expected and
                // fails to compile in the generated Xcode project.
                Type[] implArgTypes = Type.getArgumentTypes(implMethod.getDesc());
                Type[] targetTypes = new Type[(instanceCall ? 1 : 0) + implArgTypes.length];
                int tIdx = 0;
                if (instanceCall) {
                    targetTypes[tIdx++] = Type.getObjectType(implMethod.getOwner());
                }
                for (Type t : implArgTypes) {
                    targetTypes[tIdx++] = t;
                }
                Type[] samArgs = samMethodType.getArgumentTypes();
                // Defensive: only adapt when our model of the consumed values
                // matches the values we push (captured + SAM args). If it does
                // not we fall back to the verbatim load/invoke below.
                boolean adapt = targetTypes.length == capturedArgs.length + samArgs.length;

                // Handle Constructor Reference (special case)
                if (isCtorRef) {
                    interfaceMethod.addTypeInstruction(Opcodes.NEW, implMethod.getOwner());
                    interfaceMethod.addInstruction(Opcodes.DUP);
                }

                // Load captured args
                // Same caveat as the constructor: do not also emit addInstruction(Opcodes.ALOAD).
                int targetIndex = 0;
                for (int i = 0; i < capturedArgs.length; i++) {
                    interfaceMethod.addVariableOperation(Opcodes.ALOAD, 0);
                    String fieldName = "arg$" + (i + 1);
                    interfaceMethod.addField(lambdaClass, Opcodes.GETFIELD, lambdaClassName, fieldName, capturedArgs[i].getDescriptor());
                    if (adapt) {
                        adaptLambdaType(interfaceMethod, capturedArgs[i], targetTypes[targetIndex]);
                    }
                    targetIndex++;
                }

                // Load method args
                int localIndex = 1;
                for (Type t : samArgs) {
                    interfaceMethod.addVariableOperation(t.getOpcode(Opcodes.ILOAD), localIndex);
                    localIndex += t.getSize();
                    if (adapt) {
                        adaptLambdaType(interfaceMethod, t, targetTypes[targetIndex]);
                    }
                    targetIndex++;
                }

                // Invoke implMethod
                if (isCtorRef) {
                    interfaceMethod.addInvoke(Opcodes.INVOKESPECIAL, implMethod.getOwner(), implMethod.getName(), implMethod.getDesc(), false);
                } else {
                    interfaceMethod.addInvoke(invokeOpcode, implMethod.getOwner(), implMethod.getName(), implMethod.getDesc(), implMethod.isInterface());
                }

                // Adapt the value left on the stack by the implementation method
                // to the SAM's return type (unbox/box/widen/cast, or pop for a
                // void SAM), then return per the SAM descriptor.
                Type returnType = samMethodType.getReturnType();
                Type implReturnType = isCtorRef ? Type.getObjectType(implMethod.getOwner()) : Type.getReturnType(implMethod.getDesc());
                adaptLambdaType(interfaceMethod, implReturnType, returnType);
                interfaceMethod.addInstruction(returnType.getOpcode(Opcodes.IRETURN));
                interfaceMethod.setMaxes(20, 20); // Approximation


                // 6. Add static factory method
                String factoryMethodName = "lambda$factory";
                // The desc of invokedynamic is (CapturedArgs)Interface.

                // We want factory to be (CapturedArgs)LambdaClass (to match NEW output but wrapped)
                // Actually, replacing invokedynamic with INVOKESTATIC means the return type on stack should match
                // what invokedynamic promised, which is the Interface.
                // Our factory returns LambdaClass, which implements Interface. So it's assignment compatible.
                // However, the method signature in C needs to return an object pointer anyway.

                // Let's make the factory return the class type explicitly in signature
                String factoryRetType = "L" + lambdaClassName + ";";
                String actualFactoryDesc = desc.substring(0, desc.lastIndexOf(')') + 1) + factoryRetType;

                BytecodeMethod factory = new BytecodeMethod(lambdaClassName, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, factoryMethodName, actualFactoryDesc, null, null);
                lambdaClass.addMethod(factory);

                factory.addTypeInstruction(Opcodes.NEW, lambdaClassName);
                factory.addInstruction(Opcodes.DUP);

                // Load factory arguments (captured args)
                localIndex = 0; // Static method
                for (Type t : capturedArgs) {
                    factory.addVariableOperation(t.getOpcode(Opcodes.ILOAD), localIndex);
                    localIndex += t.getSize();
                }

                factory.addInvoke(Opcodes.INVOKESPECIAL, lambdaClassName, "<init>", ctorDesc.toString(), false);
                factory.addInstruction(Opcodes.ARETURN);
                factory.setMaxes(localIndex + 2, localIndex);

                // 7. Register the new class
                classes.add(lambdaClass);

                // 8. Replace invokedynamic with INVOKESTATIC to factory
                mtd.addInvoke(Opcodes.INVOKESTATIC, lambdaClassName, factoryMethodName, actualFactoryDesc, false);

                return;
            }

            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs); 
        }

        /**
         * Emits the box / unbox / widen / cast instructions LambdaMetafactory
         * inserts when adapting a value of type {@code from} to type {@code to}
         * (e.g. when a method reference's parameter or return type differs from
         * the functional interface's). A no-op when the types already match.
         */
        private void adaptLambdaType(BytecodeMethod m, Type from, Type to) {
            if (from.equals(to)) {
                return;
            }
            int toSort = to.getSort();
            int fromSort = from.getSort();
            if (toSort == Type.VOID) {
                if (fromSort != Type.VOID) {
                    m.addInstruction(from.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP);
                }
                return;
            }
            if (fromSort == Type.VOID) {
                return; // nothing on the stack to adapt
            }
            boolean toRef = toSort == Type.OBJECT || toSort == Type.ARRAY;
            boolean fromRef = fromSort == Type.OBJECT || fromSort == Type.ARRAY;

            if (!toRef && fromRef) {
                // unbox: cast to the wrapper (when the source is Object/Number
                // rather than the exact wrapper) then invoke its xxxValue()
                String wrapper = wrapperType(toSort);
                if (wrapper == null) {
                    return;
                }
                if (!wrapper.equals(from.getInternalName())) {
                    m.addTypeInstruction(Opcodes.CHECKCAST, wrapper);
                }
                m.addInvoke(Opcodes.INVOKEVIRTUAL, wrapper, unboxMethod(toSort), "()" + to.getDescriptor(), false);
                return;
            }
            if (toRef && !fromRef) {
                // box via Wrapper.valueOf(primitive); the boxed wrapper is
                // assignment compatible with the reference target by contract
                String wrapper = wrapperType(fromSort);
                if (wrapper == null) {
                    return;
                }
                m.addInvoke(Opcodes.INVOKESTATIC, wrapper, "valueOf", "(" + from.getDescriptor() + ")L" + wrapper + ";", false);
                return;
            }
            if (!toRef && !fromRef) {
                emitPrimitiveConversion(m, from, to);
                return;
            }
            // both references: narrow with a checkcast (generic erasure)
            if (!"java/lang/Object".equals(to.getInternalName())) {
                m.addTypeInstruction(Opcodes.CHECKCAST, to.getInternalName());
            }
        }

        private void emitPrimitiveConversion(BytecodeMethod m, Type from, Type to) {
            int t = to.getSort();
            int f = from.getSort();
            // byte/short/char/boolean live as int on the operand stack
            int fc = (f == Type.BOOLEAN || f == Type.BYTE || f == Type.SHORT || f == Type.CHAR) ? Type.INT : f;
            switch (t) {
                case Type.LONG:
                    if (fc == Type.INT) m.addInstruction(Opcodes.I2L);
                    else if (fc == Type.FLOAT) m.addInstruction(Opcodes.F2L);
                    else if (fc == Type.DOUBLE) m.addInstruction(Opcodes.D2L);
                    return;
                case Type.FLOAT:
                    if (fc == Type.INT) m.addInstruction(Opcodes.I2F);
                    else if (fc == Type.LONG) m.addInstruction(Opcodes.L2F);
                    else if (fc == Type.DOUBLE) m.addInstruction(Opcodes.D2F);
                    return;
                case Type.DOUBLE:
                    if (fc == Type.INT) m.addInstruction(Opcodes.I2D);
                    else if (fc == Type.LONG) m.addInstruction(Opcodes.L2D);
                    else if (fc == Type.FLOAT) m.addInstruction(Opcodes.F2D);
                    return;
                case Type.INT:
                case Type.BYTE:
                case Type.SHORT:
                case Type.CHAR:
                case Type.BOOLEAN:
                    // bring the source down to int first, then narrow if needed
                    if (fc == Type.LONG) m.addInstruction(Opcodes.L2I);
                    else if (fc == Type.FLOAT) m.addInstruction(Opcodes.F2I);
                    else if (fc == Type.DOUBLE) m.addInstruction(Opcodes.D2I);
                    if (t == Type.BYTE) m.addInstruction(Opcodes.I2B);
                    else if (t == Type.SHORT) m.addInstruction(Opcodes.I2S);
                    else if (t == Type.CHAR) m.addInstruction(Opcodes.I2C);
                    return;
                default:
                    return;
            }
        }

        private String wrapperType(int sort) {
            switch (sort) {
                case Type.BOOLEAN: return "java/lang/Boolean";
                case Type.BYTE: return "java/lang/Byte";
                case Type.CHAR: return "java/lang/Character";
                case Type.SHORT: return "java/lang/Short";
                case Type.INT: return "java/lang/Integer";
                case Type.LONG: return "java/lang/Long";
                case Type.FLOAT: return "java/lang/Float";
                case Type.DOUBLE: return "java/lang/Double";
                default: return null;
            }
        }

        private String unboxMethod(int sort) {
            switch (sort) {
                case Type.BOOLEAN: return "booleanValue";
                case Type.BYTE: return "byteValue";
                case Type.CHAR: return "charValue";
                case Type.SHORT: return "shortValue";
                case Type.INT: return "intValue";
                case Type.LONG: return "longValue";
                case Type.FLOAT: return "floatValue";
                case Type.DOUBLE: return "doubleValue";
                default: return null;
            }
        }

        private void appendConcatLiteral(BytecodeMethod targetMethod, CharSequence literal) {
            if (literal == null || literal.length() == 0) {
                return;
            }
            targetMethod.addLdc(literal.toString());
            targetMethod.addInvoke(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        }

        private void appendConcatArgument(BytecodeMethod targetMethod, Type argType, int localIndex) {
            targetMethod.addVariableOperation(argType.getOpcode(Opcodes.ILOAD), localIndex);
            String appendDesc;
            switch (argType.getSort()) {
                case Type.BOOLEAN:
                    appendDesc = "(Z)Ljava/lang/StringBuilder;";
                    break;
                case Type.CHAR:
                    appendDesc = "(C)Ljava/lang/StringBuilder;";
                    break;
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    appendDesc = "(I)Ljava/lang/StringBuilder;";
                    break;
                case Type.LONG:
                    appendDesc = "(J)Ljava/lang/StringBuilder;";
                    break;
                case Type.FLOAT:
                    appendDesc = "(F)Ljava/lang/StringBuilder;";
                    break;
                case Type.DOUBLE:
                    appendDesc = "(D)Ljava/lang/StringBuilder;";
                    break;
                case Type.OBJECT:
                    if ("java/lang/String".equals(argType.getInternalName())) {
                        appendDesc = "(Ljava/lang/String;)Ljava/lang/StringBuilder;";
                    } else {
                        appendDesc = "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
                    }
                    break;
                case Type.ARRAY:
                default:
                    appendDesc = "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
                    break;
            }
            targetMethod.addInvoke(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", appendDesc, false);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            mtd.addInvoke(opcode, owner, name, desc, itf);
            super.visitMethodInsn(opcode, owner, name, desc, itf); 
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            mtd.addField(cls, opcode, owner, name, desc);
            super.visitFieldInsn(opcode, owner, name, desc); 
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            mtd.addTypeInstruction(opcode, type);
            super.visitTypeInsn(opcode, type); 
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            mtd.addVariableOperation(opcode, var);
            super.visitVarInsn(opcode, var); 
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            mtd.addVariableOperation(opcode, operand);
            super.visitIntInsn(opcode, operand); 
        }

        @Override
        public void visitInsn(int opcode) {
            mtd.addInstruction(opcode);
            super.visitInsn(opcode); 
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            super.visitFrame(type, nLocal, local, nStack, stack); 
        }

        @Override
        public void visitCode() {
            super.visitCode(); 
        }

        @Override
        public void visitAttribute(Attribute attr) {
            super.visitAttribute(attr); 
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            if (mv == null) return null;
            return new AnnotationVisitorWrapper(super.visitParameterAnnotation(parameter, desc, visible));
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            if (mv == null) return null;
            return new AnnotationVisitorWrapper(super.visitTypeAnnotation(typeRef, typePath, desc, visible));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if ("Lcom/codename1/html5/js/JSBody;".equals(desc) || "Lorg/teavm/jso/JSBody;".equals(desc)) {
                return new JSBodyAnnotationVisitor(mtd);
            }
            if (DISABLE_DEBUG_INFO_ANNOTATION.equals(desc)) {
                mtd.setDisableDebugInfo(true);
            } else if (DISABLE_NULL_AND_ARRAY_BOUNDS_CHECKS_ANNOTATION.equals(desc)) {
                mtd.setDisableNullAndArrayBoundsChecks(true);
            }
            if (mv == null) return null;
            return new AnnotationVisitorWrapper(super.visitAnnotation(desc, visible));
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            if (mv == null) return null;
            return new AnnotationVisitorWrapper(super.visitAnnotationDefault());
        }

        @Override
        public void visitParameter(String name, int access) {
            super.visitParameter(name, access); 
        }    
        
        
    }
    
    static class FieldVisitorWrapper extends FieldVisitor {

        public FieldVisitorWrapper(FieldVisitor fv) {
            super(Opcodes.ASM9, fv);
        }

        @Override
        public void visitEnd() {
            super.visitEnd(); 
        }

        @Override
        public void visitAttribute(Attribute attr) {
            super.visitAttribute(attr); 
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return super.visitTypeAnnotation(typeRef, typePath, desc, visible); 
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return super.visitAnnotation(desc, visible); 
        }
        
    }
    
    static class AnnotationVisitorWrapper extends AnnotationVisitor {

        public AnnotationVisitorWrapper(AnnotationVisitor av) {
            super(Opcodes.ASM9, av);
        }

        @Override
        public void visitEnd() {
            super.visitEnd(); 
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if (av == null) return null;
            return super.visitArray(name); 
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            if (av == null) return null;
            return super.visitAnnotation(name, desc); 
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            super.visitEnum(name, desc, value); 
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value); 
        }
        
        
    
    }

    static class JSBodyAnnotationVisitor extends AnnotationVisitor {
        private final BytecodeMethod method;
        private String script;
        private java.util.List<String> params = new java.util.ArrayList<>();

        public JSBodyAnnotationVisitor(BytecodeMethod method) {
            super(Opcodes.ASM9);
            this.method = method;
        }

        @Override
        public void visit(String name, Object value) {
            if ("script".equals(name)) {
                script = (String) value;
            }
            super.visit(name, value);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if ("params".equals(name)) {
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String name, Object value) {
                        params.add((String) value);
                    }
                };
            }
            return super.visitArray(name);
        }

        @Override
        public void visitEnd() {
            if (script != null) {
                method.setJsBodyScript(script);
                method.setJsBodyParams(params.toArray(new String[0]));
            }
            super.visitEnd();
        }
    }
}
