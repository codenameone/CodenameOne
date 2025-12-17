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
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import com.codename1.tools.translator.bytecodes.LabelInstruction;

/**
 *
 * @author Shai Almog
 */
public class Parser extends ClassVisitor {
    private ByteCodeClass cls;
    private String clsName;
    private static String[] nativeSources;
    private static List<ByteCodeClass> classes = new ArrayList<ByteCodeClass>();
    private static Map<String, Integer> methodUsageCounts;
    private static Map<BytecodeMethod, Set<String>> methodCallMap;
    private static Set<BytecodeMethod> removedFromUsageIndex;
    private int lambdaCounter;
    public static void cleanup() {
        nativeSources = null;
        classes.clear();
        methodUsageCounts = null;
        methodCallMap = null;
        removedFromUsageIndex = null;
        LabelInstruction.cleanup();
    }
    public static void parse(File sourceFile) throws Exception {
        if(ByteCodeTranslator.verbose) {
            System.out.println("Parsing: " + sourceFile.getAbsolutePath());
        }
        ClassReader r = new ClassReader(new FileInputStream(sourceFile));
        /*if(ByteCodeTranslator.verbose) {
            System.out.println("Class: " + r.getClassName() + " derives from: " + r.getSuperName() + " interfaces: " + Arrays.asList(r.getInterfaces()));
        }*/
        Parser p = new Parser();
        
        p.clsName = r.getClassName().replace('/', '_').replace('$', '_');
        p.cls = new ByteCodeClass(p.clsName, r.getClassName());
        r.accept(p, ClassReader.EXPAND_FRAMES);
        
        classes.add(p.cls);
    }
    
    private static ByteCodeClass getClassByName(String name) {
        name = name.replace('/', '_').replace('$', '_');
        for(ByteCodeClass bc : classes) {
            if(bc.getClsName().equals(name)) {
                return bc;
            }
        }        
        return null;
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

    private static ArrayList<String> constantPool = new ArrayList<String>();
    
    public static ByteCodeClass getClassObject(String name) {
        for(ByteCodeClass cls : classes) {
            if(cls.getClsName().equals(name)) {
                return cls;
            }
        }
        return null;
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
        ArrayList<BytecodeMethod> methods = new ArrayList<BytecodeMethod>();
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
            bldM.append("");
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

            /*bld.append("#define cn1_array_4_id_");
            bld.append(bc.getClsName());
            bld.append(" ");
            bld.append(arrayId);
            bld.append("\n");
            arrayId++;*/
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
            bldM.append("");
        }
        bldM.append("};\n\n");
        
        ArrayList<Integer> instances = new ArrayList<Integer>();
        int counter = 0;
        for(ByteCodeClass bc : classes) {
            /*bld.append("extern int classInstanceOfArr");
            bld.append(counter);
            bld.append("[];\n");*/
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
        counter = 0;
        for(ByteCodeClass bc : classes) {
            if(first) {
                bldM.append("\n    ");
            } else {
                bldM.append(",\n    ");
            }
            first = false;
            bldM.append("classInstanceOfArr");
            bldM.append(counter);
            counter++;
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
        fos.write(bld.toString().getBytes("UTF-8"));
        fos.close();
        fos = new FileOutputStream(new File(outputDirectory, "cn1_class_method_index.m"));
        fos.write(bldM.toString().getBytes("UTF-8"));
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
        System.out.println("outputDirectory is: " + outputDirectory.getAbsolutePath() );
        if(ByteCodeClass.getMainClass()==null){
			System.out.println("Error main class is not defined. The main class name is expected to have a public static void main(String[]) method and it is assumed to reside in the com.package.name directory");
			System.exit(1);
		}
        String file = "Unknown File";
        try {
            for(ByteCodeClass bc : classes) {
                // special case for object
                if(bc.getClsName().equals("java_lang_Object")) {
                    continue;
                }
                file = bc.getClsName();
                bc.setBaseClassObject(getClassByName(bc.getBaseClass()));
                List<ByteCodeClass> lst = new ArrayList<ByteCodeClass>();
                for(String s : bc.getBaseInterfaces()) {
					ByteCodeClass bcode=getClassByName(s);
					if(bcode==null){
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
            // because native source may be the only thing referencing a class,
            // and the class may be purged before it even has a shot.
            readNativeFiles(outputDirectory);

            for(ByteCodeClass bc : classes) {
                file = bc.getClsName();
                bc.updateAllDependencies();
            }
            ByteCodeClass.markDependencies(classes, nativeSources);
            Set<ByteCodeClass> unmarked = new HashSet<ByteCodeClass>(classes);
            classes = ByteCodeClass.clearUnmarked(classes);
            unmarked.removeAll(classes);
            int neliminated = 0;
            for (ByteCodeClass removedClass : unmarked) {
                removedClass.setEliminated(true);
                neliminated++;
            }

            // loop over methods and start eliminating the body of unused methods
            if (BytecodeMethod.optimizerOn) {
                System.out.println("Optimizer On: Removing unused methods and classes...");
                Date now = new Date();
                neliminated += eliminateUnusedMethods();
                Date later = new Date();
                long dif = later.getTime()-now.getTime();
                System.out.println("unusued Method cull removed "+neliminated+" methods in "+(dif/1000)+" seconds");
            }

            generateClassAndMethodIndexHeader(outputDirectory);

            boolean concatenate = "true".equals(System.getProperty("concatenateFiles", "false"));
            ConcatenatingFileOutputStream cos = concatenate ? new ConcatenatingFileOutputStream(outputDirectory) : null;

            for(ByteCodeClass bc : classes) {
                file = bc.getClsName();
                writeFile(bc, outputDirectory, cos);
            }
            if (cos != null) cos.realClose();

        } catch(Throwable t) {
            System.out.println("Error while working with the class: " + file);
            t.printStackTrace();
            if(t instanceof Exception) {
                throw (Exception)t;
            }
            if(t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
        }
        finally { cleanup(); }
    }
    
    private static void readNativeFiles(File outputDirectory) throws IOException {
        File[] mFiles = outputDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".m") || file.getName().endsWith("." + ByteCodeTranslator.output.extension());
            }
        });
        nativeSources = new String[mFiles.length];
        int size = 0;
        System.out.println(""+mFiles.length +" native files");
        for(int iter = 0 ; iter < mFiles.length ; iter++) { 
        	FileInputStream fi = new FileInputStream(mFiles[iter]);
            DataInputStream di = new DataInputStream(fi);
            int len = (int)mFiles[iter].length();
            size += len;
            byte[] dat = new byte[len];
            di.readFully(dat);
            fi.close();
            nativeSources[iter] = new String(dat, "UTF-8");
        }
        System.out.println("Native files total "+(size/1024)+"K");

    }

    private static String buildMethodUsageKey(BytecodeMethod method) {
        return method.getMethodUsageKey();
    }

    private static void ensureMethodUsageIndex() {
        if (methodUsageCounts != null) {
            return;
        }
        buildMethodUsageIndex();
    }

    private static Set<String> collectCalledMethods(BytecodeMethod caller) {
        Set<String> calls = new HashSet<String>();
        String callerKey = buildMethodUsageKey(caller);
        for (String key : caller.getCalledMethodSignatureKeys()) {
            if (!key.equals(callerKey)) {
                calls.add(key);
            }
        }
        return calls;
    }

    private static void buildMethodUsageIndex() {
        methodUsageCounts = new HashMap<String, Integer>();
        methodCallMap = new HashMap<BytecodeMethod, Set<String>>();
        removedFromUsageIndex = new HashSet<BytecodeMethod>();

        for (ByteCodeClass bc : classes) {
            for (BytecodeMethod method : bc.getMethods()) {
                if (method.isEliminated()) {
                    continue;
                }
                Set<String> calls = collectCalledMethods(method);
                methodCallMap.put(method, calls);
                for (String key : calls) {
                    Integer count = methodUsageCounts.get(key);
                    methodUsageCounts.put(key, count == null ? 1 : count + 1);
                }
            }
        }
    }

    private static void removeFromMethodUsageIndex(BytecodeMethod method) {
        if (removedFromUsageIndex == null) {
            removedFromUsageIndex = new HashSet<BytecodeMethod>();
        }
        if (removedFromUsageIndex.contains(method)) {
            return;
        }
        ensureMethodUsageIndex();
        Set<String> calls = methodCallMap.remove(method);
        if (calls == null) {
            calls = collectCalledMethods(method);
        }
        for (String key : calls) {
            Integer count = methodUsageCounts.get(key);
            if (count == null) {
                continue;
            }
            if (count <= 1) {
                methodUsageCounts.remove(key);
            } else {
                methodUsageCounts.put(key, count - 1);
            }
        }
        removedFromUsageIndex.add(method);
    }

    private static int eliminateUnusedMethods() {
        return(eliminateUnusedMethods(false, 0));
    }

    private static int eliminateUnusedMethods(boolean forceFound, int depth) {
        ensureMethodUsageIndex();
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
                if(mtd.isEliminated() || mtd.isMain() || mtd.getMethodName().equals("__CLINIT__") || mtd.getMethodName().equals("finalize") || mtd.isNative()) {
                    if (!mtd.isEliminated() && mtd.getMethodName().contains("yield")) {
                        System.out.println("Not eliminating method ");
                        System.out.println("main="+mtd.isMain()+", isNative="+mtd.isNative());
                    }
                    continue;
                }

                if(!isMethodUsed(mtd, bc)) {
                    if(isMethodUsedByBaseClassOrInterface(mtd, bc)) {
                        continue;
                    }
                    mtd.setEliminated(true);
                    removeFromMethodUsageIndex(mtd);
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
        System.out.println("cullClasses()");
        if(found && depth < 4) {
            for(ByteCodeClass bc : classes) {
                bc.updateAllDependencies();
            }   

            ByteCodeClass.markDependencies(classes, nativeSources);
            List<ByteCodeClass> tmp = ByteCodeClass.clearUnmarked(classes);
            /*if(ByteCodeTranslator.verbose) {
            System.out.println("Classes removed from: " + classCount + " to " + classes.size());
            for(ByteCodeClass bc : classes) {
            if(!tmp.contains(bc)) {
            System.out.println("Removed class: " + bc.getClsName());
            }
            }
            }*/

            // 2nd pass to mark classes as eliminated so that we can propagate down to each
            // method of the class to mark it eliminated so that virtual methods
            // aren't included later on when writing virtual methods
            Set<ByteCodeClass> removedClasses = new HashSet<ByteCodeClass>(classes);
            removedClasses.removeAll(tmp);
            int nfound = 0;
            for (ByteCodeClass cls : removedClasses) {
                nfound += cls.setEliminated(true);
                for (BytecodeMethod method : cls.getMethods()) {
                    removeFromMethodUsageIndex(method);
                }
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
        ensureMethodUsageIndex();
        Integer count = methodUsageCounts.get(buildMethodUsageKey(m));
        return count != null && count > 0;
    }

    private static void writeFile(ByteCodeClass cls, File outputDir, ConcatenatingFileOutputStream writeBufferInstead) throws Exception {
        OutputStream outMain =
                writeBufferInstead != null && ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_IOS ?
                        writeBufferInstead :
                        new FileOutputStream(new File(outputDir, cls.getClsName() + "." + ByteCodeTranslator.output.extension()));

        if (outMain instanceof ConcatenatingFileOutputStream) {
            ((ConcatenatingFileOutputStream)outMain).beginNextFile(cls.getClsName());
        }
        if(ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_CSHARP) {
            outMain.write(cls.generateCSharpCode().getBytes());
            outMain.close();
        } else {
            outMain.write(cls.generateCCode(classes).getBytes());
            outMain.close();

            // we also need to write the header file for C outputs
            String headerName = cls.getClsName() + ".h";
            FileOutputStream outHeader = new FileOutputStream(new File(outputDir, headerName));
            outHeader.write(cls.generateCHeader().getBytes());
            outHeader.close();
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
        JSRInlinerAdapter a = new JSRInlinerAdapter(new MethodVisitorWrapper(super.visitMethod(access, name, desc, signature, exceptions), mtd), access, name, desc, signature, exceptions);
        return a; 
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
        private BytecodeMethod mtd;
        public MethodVisitorWrapper(MethodVisitor mv, BytecodeMethod mtd) {
            super(Opcodes.ASM9, mv);
            this.mtd = mtd;
        }

        @Override
        public void visitEnd() {
            super.visitEnd(); 
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

                ctor.addInstruction(Opcodes.ALOAD); // 25
                ctor.addVariableOperation(Opcodes.ALOAD, 0);
                ctor.addInvoke(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

                int varIndex = 1;
                for (int i = 0; i < capturedArgs.length; i++) {
                    ctor.addInstruction(Opcodes.ALOAD);
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
                Type instantiatedMethodType = (Type) bsmArgs[2];

                String samMethodName = name; // Name from invokedynamic
                String samMethodDesc = samMethodType.getDescriptor(); // Signature from BSM arg 0

                BytecodeMethod interfaceMethod = new BytecodeMethod(lambdaClassName, Opcodes.ACC_PUBLIC, samMethodName, samMethodDesc, null, null);
                lambdaClass.addMethod(interfaceMethod);

                // Method Body:
                // Load captured arguments from fields
                // Load method arguments
                // Invoke implMethod
                // Return result

                // Handle Constructor Reference (special case)
                boolean isCtorRef = (implMethod.getTag() == Opcodes.H_NEWINVOKESPECIAL);
                if (isCtorRef) {
                    interfaceMethod.addTypeInstruction(Opcodes.NEW, implMethod.getOwner());
                    interfaceMethod.addInstruction(Opcodes.DUP);
                }

                // Load captured args
                for (int i = 0; i < capturedArgs.length; i++) {
                    interfaceMethod.addInstruction(Opcodes.ALOAD);
                    interfaceMethod.addVariableOperation(Opcodes.ALOAD, 0);
                    String fieldName = "arg$" + (i + 1);
                    interfaceMethod.addField(lambdaClass, Opcodes.GETFIELD, lambdaClassName, fieldName, capturedArgs[i].getDescriptor());
                }

                // Load method args
                Type[] samArgs = samMethodType.getArgumentTypes();
                int localIndex = 1;
                for (Type t : samArgs) {
                    interfaceMethod.addVariableOperation(t.getOpcode(Opcodes.ILOAD), localIndex);
                    localIndex += t.getSize();
                }

                // Invoke implMethod
                int invokeOpcode;
                switch (implMethod.getTag()) {
                    case Opcodes.H_INVOKESTATIC: invokeOpcode = Opcodes.INVOKESTATIC; break;
                    case Opcodes.H_INVOKEVIRTUAL: invokeOpcode = Opcodes.INVOKEVIRTUAL; break;
                    case Opcodes.H_INVOKEINTERFACE: invokeOpcode = Opcodes.INVOKEINTERFACE; break;
                    case Opcodes.H_INVOKESPECIAL: invokeOpcode = Opcodes.INVOKESPECIAL; break;
                    case Opcodes.H_NEWINVOKESPECIAL: invokeOpcode = Opcodes.INVOKESPECIAL; break;
                    default: invokeOpcode = Opcodes.INVOKESTATIC; // Fallback
                }

                if (isCtorRef) {
                    interfaceMethod.addInvoke(Opcodes.INVOKESPECIAL, implMethod.getOwner(), implMethod.getName(), implMethod.getDesc(), false);
                } else {
                    interfaceMethod.addInvoke(invokeOpcode, implMethod.getOwner(), implMethod.getName(), implMethod.getDesc(), implMethod.isInterface());
                }

                // Return
                Type returnType = samMethodType.getReturnType();
                interfaceMethod.addInstruction(returnType.getOpcode(Opcodes.IRETURN));
                interfaceMethod.setMaxes(20, 20); // Approximation


                // 6. Add static factory method
                String factoryMethodName = "lambda$factory";
                String factoryDesc = desc; // The desc of invokedynamic is (CapturedArgs)Interface.

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

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            mtd.addInvoke(opcode, owner, name, desc, itf);
            super.visitMethodInsn(opcode, owner, name, desc, itf); 
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            super.visitMethodInsn(opcode, owner, name, desc); 
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
    
    class FieldVisitorWrapper extends FieldVisitor {

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
    
    class AnnotationVisitorWrapper extends AnnotationVisitor {

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
}
