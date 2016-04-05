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

import com.codename1.tools.translator.bytecodes.Instruction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Parsed class file
 *
 * @author Shai Almog
 */
public class ByteCodeClass {
    private List<ByteCodeField> fullFieldList;
    private List<ByteCodeField> staticFieldList;
    private Set<String> dependsClassesInterfaces = new TreeSet<String>();
    private List<BytecodeMethod> methods = new ArrayList<BytecodeMethod>();
    private List<ByteCodeField> fields = new ArrayList<ByteCodeField>();
    private String clsName;
    private String baseClass;
    private List<String> baseInterfaces;
    private boolean isInterface;
    private boolean isAbstract;
    private boolean usedByNative;
    private static boolean saveUnitTests;
    private boolean isUnitTest;

    private static Set<String> arrayTypes = new TreeSet<String>();
    
    private ByteCodeClass baseClassObject;
    private List<ByteCodeClass> baseInterfacesObject;
    
    List<BytecodeMethod> virtualMethodList;
    private String sourceFile;

    private int classOffset;
    
    private boolean marked;
    private static ByteCodeClass mainClass;
    private boolean finalClass;
    
    public ByteCodeClass(String clsName) {
        this.clsName = clsName;
    }
    static ByteCodeClass getMainClass() {
		return mainClass;
    }
    
    static void setSaveUnitTests(boolean save) {
        saveUnitTests = save;
    }
    
    public void addMethod(BytecodeMethod m) {
        if(m.isMain()) {
            if (mainClass == null) {
                mainClass = this;
            } else {
                throw new RuntimeException("Multiple main classes: "+mainClass.clsName+" and "+this.clsName);
            }
        }
        m.setSourceFile(sourceFile);
        m.setForceVirtual(isInterface);
        methods.add(m);
    }

    public void addField(ByteCodeField m) {
        fields.add(m);
    }
    
    public String generateCSharpCode() {
        return "";
    }

    public static void markDependencies(List<ByteCodeClass> lst) {
        mainClass.markDependent(lst);
        for(ByteCodeClass bc : lst) {
            if(bc.clsName.equals("java_lang_Boolean")) {
                bc.markDependent(lst);
            }
            if(bc.clsName.equals("java_lang_String")) {
                bc.markDependent(lst);
            }
            if(bc.clsName.equals("java_lang_Integer")) {
                bc.markDependent(lst);
            }
            if(bc.clsName.equals("java_lang_Byte")) {
                bc.markDependent(lst);
            }
            if(bc.clsName.equals("java_lang_Short")) {
                bc.markDependent(lst);
            }
            if(bc.clsName.equals("java_lang_Character")) {
                bc.markDependent(lst);
            }
            if(bc.clsName.equals("java_lang_Thread")) {
                bc.markDependent(lst);
            }
            if(bc.clsName.equals("java_lang_Long")) {
                bc.markDependent(lst);
            }
            if(bc.clsName.equals("java_lang_Double")) {
                bc.markDependent(lst);
            }
            if(bc.clsName.equals("java_lang_Float")) {
                bc.markDependent(lst);
            }
            if(bc.clsName.equals("java_text_DateFormat")) {
                bc.markDependent(lst);
            }
            if(!bc.marked && bc.isUsedByNative()){
                bc.markDependent(lst);
            }
            if(!bc.marked && saveUnitTests && bc.isUnitTest) {
                bc.markDependent(lst);
            }
        }
        
        // mark all non-final classes that aren't inherited as final for use in 
        // additional optimizations
        for(ByteCodeClass bc : lst) {
            if(bc.isFinalClass() || bc.isInterface || bc.isIsAbstract()) {
                continue;
            }
            boolean found = false;
            for(ByteCodeClass bk : lst) {
                if(bk.baseClassObject == bc) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                bc.setFinalClass(true);
            }
        }
        
        // we try to disable the "virtual" aspect of methods where possible
        for(ByteCodeClass bc : lst) {
            if(bc.isFinalClass()) {
                for(BytecodeMethod meth : bc.methods) {
                    if(meth.canBeVirtual() && !bc.isMethodFromBaseOrInterface(meth)) {
                        meth.setVirtualOverriden(true);
                    }
                } 
            } 
        }        
    }

    public void unmark() {
        marked = false;
    }
    
    private void markDependent(List<ByteCodeClass> lst) {
        if(marked) {
            return;
        }
        marked = true;
        
        // make sure the method/classname are in the constant pool so we can later 
        // look them up in case of a stack trace exception
        Parser.addToConstantPool(clsName);
        for(BytecodeMethod bm : methods) {
            if(!bm.isEliminated()) {
                Parser.addToConstantPool(bm.getMethodName());
                bm.addToConstantPool();
            }
        }
        
        for(String s : dependsClassesInterfaces) {
            ByteCodeClass cls = findClass(s, lst);
            
            // annotation can be null
            if(cls != null) {
                cls.markDependent(lst);
            }
        }
    }
    
    public static List<ByteCodeClass> clearUnmarked(List<ByteCodeClass> lst) {
        List<ByteCodeClass> response = new ArrayList<ByteCodeClass>();
        for(ByteCodeClass bc : lst) {
            if(bc.marked) {
                response.add(bc);
            }
        }
        return response;
    }
    
    private ByteCodeClass findClass(String s, List<ByteCodeClass> lst) {
        for(ByteCodeClass c : lst) {
            if(c.clsName.equals(s)) {
                return c;
            }
        }
        return null;
    }
    
    public void updateAllDependencies() {
        dependsClassesInterfaces.clear();
        setBaseClass(baseClass);
        for(String s : baseInterfaces) {
            s = s.replace('/', '_').replace('$', '_');
            if(!dependsClassesInterfaces.contains(s)) {
                dependsClassesInterfaces.add(s);
            }
        }
        if(virtualMethodList != null) {
            virtualMethodList.clear();
        } else {
            virtualMethodList = new ArrayList<BytecodeMethod>();
        }
        fillVirtualMethodTable(virtualMethodList);
        for(BytecodeMethod m : methods) {
            if(m.isEliminated()) {
                continue;
            }
            for(String s : m.getDependentClasses()) {
                if(!dependsClassesInterfaces.contains(s)) {
                    dependsClassesInterfaces.add(s);
                }
            }
        }
        for(ByteCodeField m : fields) {
            for(String s : m.getDependentClasses()) {
                if(!dependsClassesInterfaces.contains(s)) {
                    dependsClassesInterfaces.add(s);
                }
            }
        }
    }
    
    private boolean isMethodFromBaseOrInterface(BytecodeMethod bm) {
        if(baseInterfacesObject != null) {
            for(ByteCodeClass bi : baseInterfacesObject) {
                if(bi.getMethods().contains(bm)) {
                    return true;
                }
                if(bi.getBaseClassObject() != null) {
                    boolean b = bi.isMethodFromBaseOrInterface(bm);
                    if(b) {
                        return true;
                    }
                }
            }
        }
        if(baseClassObject != null) {
            if(baseClassObject.getMethods().contains(bm)) {
                return true;
            }
            return baseClassObject.isMethodFromBaseOrInterface(bm);
        }
        return false;
    }
    
    private boolean hasDefaultConstructor() {
        for(BytecodeMethod bm : methods) {
            if(bm.isDefaultConstructor()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFinalizer() {
        for(BytecodeMethod bm : methods) {
            if(bm.isFinalizer()) {
                return true;
            }
        }
        return false;
    }
    
    public static void addArrayType(String type, int dimenstions) {
        String arr = dimenstions + "_" + type;
        if(!arrayTypes.contains(arr)) {
            arrayTypes.add(arr);
        }
    }
    
    public String generateCCode(List<ByteCodeClass> allClasses) {        
        StringBuilder b = new StringBuilder();
        b.append("#include \"");
        b.append(clsName);
        b.append(".h\"\n");
        
        b.append("const struct clazz *base_interfaces_for_");
        b.append(clsName);
        b.append("[] = {");
        boolean first = true;
        for(String ints : baseInterfaces) {
            if(!first) {
                b.append(", ");            
            }
            first = false;
            b.append("&class__");
            b.append(ints.replace('/', '_').replace('$', '_'));
        }
        b.append("};\n");
        
        
        // class struct, contains vtable, static fields and meta data (class name), type info etc.
        b.append("struct clazz class__");
        b.append(clsName);
        b.append(" = {\n");
        // object fields so class will be compatible to object
        
        if(clsName.equals("java_lang_Class")) {
            b.append("  DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, &__FINALIZER_");
        } else {
            b.append("  DEBUG_GC_INIT &class__java_lang_Class, 999999, 0, 0, 0, 0, &__FINALIZER_");
        }
        b.append(clsName);
        b.append(" ,0 , &__GC_MARK_");
        b.append(clsName);
        
        // initialized defaults to false
        b.append(",  0, ");
        
        // the numberic id of the class 
        b.append("cn1_class_id_");
        b.append(clsName);
        b.append(", ");
        
        // name of the class
        b.append("\"");
        b.append(clsName.replace('_', '.'));
        b.append("\", ");
        
        // is array class type
        b.append("0, ");
        
        // array type dimensions
        b.append("0, ");
        
        // array internal type
        b.append("0, ");
        
        // primitive type
        b.append("JAVA_FALSE, ");
        
        // reference to the base class
        if(baseClass != null) {
            b.append("&class__");
            b.append(baseClass.replace('/', '_').replace('$', '_'));
        } else {
            b.append("(const struct clazz*)0");
        }
        b.append(", ");
        
        // references to the base interfaces
        b.append("base_interfaces_for_");
        b.append(clsName);
        b.append(", ");

        // number of base interfaces
        b.append(baseInterfaces.size());
        
        // new instance function pointer
        if(!isInterface && !isAbstract && hasDefaultConstructor()) {
            b.append(", &__NEW_INSTANCE_");
            b.append(clsName);
        } else {
            b.append(", 0");
        }
        
        // vtable 
        b.append(", 0\n");
        
        b.append("};\n\n");

        // create class objects for 1 - 3 dimension arrays
        for(int iter = 1 ; iter < 4 ; iter++) {
            if(!(arrayTypes.contains(iter + "_" + clsName) || arrayTypes.contains((iter + 1) + "_" + clsName) || 
                    arrayTypes.contains((iter + 2) + "_" + clsName))) {
                continue;
            }
            b.append("struct clazz class_array");
            b.append(iter);
            b.append("__");
            b.append(clsName);
            if(clsName.equals("java_lang_Class")) {
                b.append(" = {\n DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, &arrayFinalizerFunction, &gcMarkArrayObject, 0, cn1_array_");
            } else {
                b.append(" = {\n DEBUG_GC_INIT &class__java_lang_Class, 999999, 0, 0, 0, 0, 0, &arrayFinalizerFunction, &gcMarkArrayObject, 0, cn1_array_");
            }
            b.append(iter);
            b.append("_id_");
            b.append(clsName);
            b.append(", \"");
            b.append(clsName.replace('_', '.'));
            b.append("[]\", ");

            // array class type, dimension & internal type
            b.append("JAVA_TRUE, ");
            b.append(iter);
            b.append(", &class__");
            b.append(clsName);

            // primitive type is always false here object is always the base class of the array it has no base interfaces
            b.append(", JAVA_FALSE, &class__java_lang_Object, EMPTY_INTERFACES, 0, ");

            // new instance function pointer and vtable 
            b.append("0, 0\n};\n\n");
        }

        staticFieldList = new ArrayList<ByteCodeField>();
        buildStaticFieldList(staticFieldList);

        // static fields for the class
        for(ByteCodeField bf : staticFieldList) {
            if(bf.isStaticField() && bf.getClsName().equals(clsName)) {
                if(bf.isFinal() && bf.getValue() != null) {
                    // static getter 
                    b.append(bf.getCDefinition());
                    b.append(" get_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName().replace('$', '_'));
                    b.append("(CODENAME_ONE_THREAD_STATE) {\n    return ");
                    if(bf.getValue() instanceof String) {
                        b.append("STRING_FROM_CONSTANT_POOL_OFFSET(");
                        b.append(Parser.addToConstantPool((String)bf.getValue()));
                        b.append(") /* ");
                        b.append(bf.getValue());
                        b.append(" */");
                    } else {
                        if(bf.getValue() instanceof Number) {
                            if(bf.getValue() instanceof Double) {
                                Double d = ((Double)bf.getValue());
                                if(d.isNaN()) {
                                    b.append("0.0/0.0");                                    
                                } else {
                                    if(d.isInfinite()) {
                                        if(d.doubleValue() > 0) {
                                            b.append("1.0f / 0.0f");
                                        } else {
                                            b.append("-1.0f / 0.0f");
                                        }
                                    } else {
                                        b.append(bf.getValue());
                                    }
                                }
                            } else {
                                if(bf.getValue() instanceof Float) {
                                    Float d = ((Float)bf.getValue());
                                    if(d.isNaN()) {
                                        b.append("0.0/0.0");                                    
                                    } else {
                                        if(d.isInfinite()) {
                                            if(d.floatValue() > 0) {
                                                b.append("1.0f / 0.0f");
                                            } else {
                                                b.append("-1.0f / 0.0f");
                                            }
                                        } else {
                                            b.append(bf.getValue());
                                        }
                                    }
                                } else {
                                    b.append(bf.getValue());
                                }
                            }
                        } else {
                            if(bf.getValue() instanceof Boolean) {
                                if(((Boolean)bf.getValue()).booleanValue()) {
                                    b.append("JAVA_TRUE");
                                } else {
                                    b.append("JAVA_FALSE");
                                }
                            } else {
                                b.append("JAVA_NULL");
                            }
                        }
                    }
                    b.append(";\n}\n\n");                    
                } else {
                    b.append(bf.getCDefinition());
                    b.append(" STATIC_FIELD_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append(" = 0;\n");

                    // static getter 
                    b.append(bf.getCDefinition());
                    b.append(" get_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName().replace('$', '_'));
                    b.append("(CODENAME_ONE_THREAD_STATE) {\n    __STATIC_INITIALIZER_");
                    b.append(bf.getClsName());
                    b.append("(threadStateData);\n     return STATIC_FIELD_");
                    b.append(bf.getClsName());
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append(";\n}\n\n");

                    // static setter 
                    b.append("void set_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName().replace('$', '_'));
                    b.append("(CODENAME_ONE_THREAD_STATE, ");
                    b.append(bf.getCDefinition());
                    b.append(" __cn1StaticVal) {\n    __STATIC_INITIALIZER_");
                    b.append(bf.getClsName());
                    if(bf.isObjectType()) {
                        if(bf.isFinal()) {
                            b.append("(threadStateData);\n    STATIC_FIELD_");                        
                        } else {
                            b.append("(threadStateData);\n    STATIC_FIELD_");
                        }
                    } else {
                        b.append("(threadStateData);\n    STATIC_FIELD_");
                    }
                    b.append(bf.getClsName());
                    b.append("_");
                    b.append(bf.getFieldName());
                    if(bf.shouldRemoveFromHeapCollection()) {
                        if(bf.getType() != null && bf.getType().endsWith("String")) {
                            b.append(" = __cn1StaticVal;\n    removeObjectFromHeapCollection(threadStateData, __cn1StaticVal);\n    if(__cn1StaticVal != 0) {\n        removeObjectFromHeapCollection(threadStateData, ((struct obj__java_lang_String*)__cn1StaticVal)->java_lang_String_value);\n    }\n}\n\n");
                        } else {
                            b.append(" = __cn1StaticVal;\n    removeObjectFromHeapCollection(threadStateData, __cn1StaticVal);\n}\n\n");
                        }
                    } else {
                        b.append(" = __cn1StaticVal;\n}\n\n");
                    }
                }
            }
        }        
        
        if(isInterface) {
            b.append("int **classToInterfaceMap_");
            b.append(clsName);
            b.append(";\n");
        }
        
        fullFieldList = new ArrayList<ByteCodeField>();
        buildInstanceFieldList(fullFieldList);
        for(ByteCodeField fld : fullFieldList) {
            b.append(fld.getCDefinition());
            b.append(" get_field_");
            b.append(clsName);
            b.append("_");
            b.append(fld.getFieldName());
            b.append("(JAVA_OBJECT __cn1T) {\n    return (*(struct obj__");
            b.append(clsName);
            b.append("*)__cn1T).");            
            b.append(fld.getClsName());
            b.append("_");
            b.append(fld.getFieldName());
            b.append(";\n}\n\n");

            b.append("void set_field_");
            b.append(clsName);
            b.append("_");
            b.append(fld.getFieldName());
            b.append("(CODENAME_ONE_THREAD_STATE, ");
            b.append(fld.getCDefinition());
            if(fld.isObjectType()) {
                b.append(" __cn1Val, JAVA_OBJECT __cn1T) {\n    (*(struct obj__");
            } else {
                b.append(" __cn1Val, JAVA_OBJECT __cn1T) {\n    (*(struct obj__");
            }
            b.append(clsName);
            b.append("*)__cn1T).");            
            b.append(fld.getClsName());
            b.append("_");
            b.append(fld.getFieldName());
            b.append(" = __cn1Val;\n}\n\n");
        }
                
        
        // finalizer and GC_RELEASE to cleanup variables
        b.append("JAVA_VOID __FINALIZER_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT objToDelete) {\n");
        if(hasFinalizer()) {
            b.append("    ");
            b.append(clsName);
            b.append("_finalize__(threadStateData, objToDelete);\n");
        }
        // invoke the finalize method of the base
        if(baseClass != null) {
            b.append("    __FINALIZER_");
            b.append(baseClass.replace('/', '_').replace('$', '_'));
            b.append("(threadStateData, objToDelete);\n");
        }
        
        b.append("}\n\n");
                
        // mark function for the GC mark cycle to tag the objects that are reachable
        b.append("void __GC_MARK_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT objToMark, JAVA_BOOLEAN force) {\n    struct obj__");
        b.append(clsName);
        b.append("* objInstance = (struct obj__");
        b.append(clsName);
        b.append("*)objToMark;\n");
        for(ByteCodeField fld : fullFieldList) {
            if(!fld.isStaticField() && fld.isObjectType() && fld.getClsName().equals(clsName)) {
                b.append("    gcMarkObject(threadStateData, objInstance->");
                b.append(fld.getClsName());
                b.append("_");
                b.append(fld.getFieldName());
                b.append(", force);\n");
            }
        }
        // invoke the mark method of the base
        if(baseClass != null) {
            b.append("    __GC_MARK_");
            b.append(baseClass.replace('/', '_').replace('$', '_'));
            b.append("(threadStateData, objToMark, force);\n");
        } else {
            // we can do this in Object.java only since all code will reach here eventually
            b.append("    objToMark->__codenameOneGcMark = currentGcMarkValue;\n");
        }
        b.append("}\n\n");

        // initialize object instances

        if(!isInterface && !isAbstract) {
            b.append("JAVA_OBJECT __NEW_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE) {\n    __STATIC_INITIALIZER_");
            b.append(clsName);
            b.append("(threadStateData);\n    JAVA_OBJECT o = codenameOneGcMalloc(threadStateData, sizeof(struct obj__");
            b.append(clsName);
            b.append("), &class__");
            b.append(clsName);
            b.append(");\n    return o;\n}\n\n");

            if(hasDefaultConstructor()) {
                b.append("JAVA_OBJECT __NEW_INSTANCE_");
                b.append(clsName);
                b.append("(CODENAME_ONE_THREAD_STATE) {\n    __STATIC_INITIALIZER_");
                b.append(clsName);
                b.append("(threadStateData);\n    JAVA_OBJECT o = codenameOneGcMalloc(threadStateData, sizeof(struct obj__");
                b.append(clsName);
                b.append("), &class__");
                b.append(clsName);
                b.append(");\n");
                b.append(clsName);
                b.append("___INIT____(threadStateData, o);\n    return o;\n}\n\n");
            }
        }
                
        if(arrayTypes.contains("1_" + clsName) || arrayTypes.contains("2_" + clsName) || arrayTypes.contains("3_" + clsName)) {
            b.append("JAVA_OBJECT __NEW_ARRAY_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {\n");
            b.append("    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__");
            b.append(clsName);
            b.append(", sizeof(JAVA_OBJECT), 1);\n    (*o).__codenameOneParentClsReference = &class_array1__");
            b.append(clsName);
            b.append(";\n    return o;\n}\n\n");
        }

        /*b.append("JAVA_OBJECT __NEW_MULTI_ARRAY_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE, JAVA_INT dimensions, JAVA_INT* sizes) {\n    JAVA_OBJECT o = JAVA_NULL;\n");
        b.append("    switch(dimensions) {\n        case 2: o = allocMultiArray(sizes, &class_array2__");
        b.append(clsName);
        b.append(", sizeof(JAVA_OBJECT), 2); break;\n");
        b.append("        case 3: o = allocMultiArray(sizes, &class_array3__");
        b.append(clsName);
        b.append(", sizeof(JAVA_OBJECT), 3); break;\n");
        b.append("        case 4: o = allocMultiArray(sizes, &class_array4__");
        b.append(clsName);
        b.append(", sizeof(JAVA_OBJECT), 4); break;\n");
        b.append("        default: return JAVA_NULL;\n    }\n    (*o).__codenameOneParentClsReference = &class__");
        b.append(clsName);
        b.append(";\n    return o;\n}\n\n");*/

        String clInitMethod = null;
        if(isInterface) {
            for(BytecodeMethod m : methods) {
                if(m.getMethodName().equals("__CLINIT__")) {
                    m.appendMethodC(b);
                    clInitMethod = clsName + "_" + m.getMethodName() + "__";
                } else {
                    m.appendInterfaceMethodC(b);
                }
            }
        } else {
            for(BytecodeMethod m : methods) {
                m.appendMethodC(b);
                if(m.getMethodName().indexOf("_CLINIT_") > -1) {
                    clInitMethod = clsName + "_" + m.getMethodName() + "__";
                }
                if(m.isMain()) {
                    b.append("\nint main(int argc, char *argv[]) {\n    initConstantPool();\n");
                    b.append("    ");
                    b.append(clsName);
                    b.append("_main___java_lang_String_1ARRAY(getThreadLocalData(), JAVA_NULL);\n}\n\n");
                }
            }
        }
        if(baseClassObject != null) {
            List<BytecodeMethod> bm = new ArrayList<BytecodeMethod>(methods);
            appendSuperStub(b, bm, baseClassObject);
        }
        int offset = 0;
        if(clsName.equals("java_lang_Class")) {
            // special case for Class which can't have a vtable since it has no class of its own...
            appendClassVFunctions(b);
        } else {
            if(isInterface) {
                // special case, object virtual calls on interfaces should act
                // as if they are regular virtual calls
                for(BytecodeMethod m : virtualMethodList) {
                    if(m.getClsName().equals("java_lang_Object")) {
                        m.appendVirtualMethodC(clsName, b, "" + offset, true);
                    } else {
                        // we pretend to have a virtual method here but the optimizer says its not really needed
                        if(!m.isVirtualOverriden()) {
                            m.appendVirtualMethodC(clsName, b, "classToInterfaceMap_" + clsName + 
                                    "[__cn1ThisObject->__codenameOneParentClsReference->classId][" + offset + "]", true);
                        }
                        offset++;
                    }
                }
            } else {
                for(BytecodeMethod m : virtualMethodList) {
                    m.appendVirtualMethodC(clsName, b, offset);
                    offset++;
                }
            }
        }
        if(!isInterface) {
            b.append("void __INIT_VTABLE_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE, void** vtable) {\n    ");
            if(baseClass != null) {
                b.append("    __INIT_VTABLE_");
                b.append(baseClass.replace('/', '_').replace('$', '_'));
                b.append("(threadStateData, vtable);\n");
            }
            for(int iter = 0 ; iter < virtualMethodList.size() ; iter++) {
                BytecodeMethod bm = virtualMethodList.get(iter);
                if(bm.getClsName().equals(clsName) && !bm.isVirtualOverriden()) {
                    b.append("    vtable[");
                    b.append(iter);
                    b.append("] = &");
                    bm.appendFunctionPointer(b);
                    b.append(";\n");
                }
            }
            b.append("}\n\n");
        }
        
        // insert static initializer
        b.append("void __STATIC_INITIALIZER_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE) {\n    if(class__");
        b.append(clsName);
        b.append(".initialized) return;\n\n    ");

        if(arrayTypes.contains("1_" + clsName) || arrayTypes.contains("2_" + clsName) || arrayTypes.contains("3_" + clsName)) {
            b.append("class_array1__");
            b.append(clsName);
            b.append(".vtable = initVtableForInterface();\n    ");
        }

        b.append("monitorEnter(threadStateData, (JAVA_OBJECT)&class__");
        
        b.append(clsName);
        b.append(");\n    if(class__");
        b.append(clsName);
        b.append(".initialized) {\n        monitorExit(threadStateData, (JAVA_OBJECT)&class__");
        b.append(clsName);
        b.append(");\n        return;\n    }\n\n");
        
        // create the vtable
        b.append("    class__");
        b.append(clsName);
        b.append(".vtable = malloc(sizeof(void*) *");
        b.append(virtualMethodList.size());
        b.append(");\n");
        if(isInterface) {
            // special case, java_lang_object calls on interfaces should
            // act like standard virtual method calls
            b.append("    class__");
            b.append(clsName);
            b.append(".vtable = initVtableForInterface();\n");
            b.append("    classToInterfaceMap_");
            b.append(clsName);
            b.append(" = malloc(sizeof(int*) * cn1_array_start_offset);\n");
            for(ByteCodeClass cls : allClasses) {
                if(!cls.isInterface) {
                    if(cls.doesImplement(this)) {
                        b.append("    classToInterfaceMap_");
                        b.append(clsName);
                        b.append("[cn1_class_id_");
                        b.append(cls.clsName);
                        b.append("] = malloc(sizeof(int*) * ");
                        b.append(getMethodCountIncludingBase());
                        b.append(");\n");
                        offset = 0;
                        for(BytecodeMethod m : virtualMethodList) {
                            if(!m.getClsName().equals("java_lang_Object")) {
                                b.append("    classToInterfaceMap_");
                                b.append(clsName);
                                b.append("[cn1_class_id_");
                                b.append(cls.clsName);
                                b.append("][");
                                b.append(offset);
                                b.append("] = ");
                                b.append(cls.virtualMethodList.indexOf(m));
                                b.append(";\n");
                                offset++;
                            }
                        }
                    }
                }
            }
        } else {
            b.append("    __INIT_VTABLE_");
            b.append(clsName);
            b.append("(threadStateData, class__");
            b.append(clsName);
            b.append(".vtable);\n");
            /*for(int iter = 0 ; iter < virtualMethodList.size() ; iter++) {
                b.append("    class__");
                b.append(clsName);
                b.append(".vtable[");
                b.append(iter);
                b.append("] = &");
                virtualMethodList.get(iter).appendFunctionPointer(b);
                b.append(";\n");
            }*/
        }
        b.append("    class__");
        b.append(clsName);
        b.append(".initialized = JAVA_TRUE;\n    monitorExit(threadStateData, (JAVA_OBJECT)&class__");
        b.append(clsName);
        b.append(");\n");

        // init static fields and invoke the static initializer code block
        if(clInitMethod != null) {
            b.append("    ");
            b.append(clInitMethod);
            b.append("(threadStateData);\n");
        }
        
        b.append("}\n\n");
        
        return b.toString();
    }

    private boolean doesImplement(ByteCodeClass interfaceObj) {
        if(baseInterfacesObject != null) {
            if(baseInterfacesObject.contains(interfaceObj)) {
                return true;
            }
            // check if one of the interfaces we implement derives from this interface
            for(ByteCodeClass i : baseInterfacesObject) {
                if(i.getBaseClassObject() == interfaceObj || i.doesImplement(interfaceObj)) {
                    return true;
                }
            }
        }
        if(baseClassObject != null) {
            return baseClassObject.doesImplement(interfaceObj);
        }
        return false;
    }
    
    private void appendSuperStub(StringBuilder b, List<BytecodeMethod> bm, ByteCodeClass base) {
        // append super stub
        BytecodeMethod.setAcceptStaticOnEquals(true);
        for(BytecodeMethod m : base.methods) {
            if(!m.isPrivate() && !bm.contains(m)) {
                m.appendSuperCall(b, clsName);
                bm.add(m);
            }
        }
        if(base.baseClassObject != null) {
            appendSuperStub(b, bm, base.baseClassObject);
        }
        BytecodeMethod.setAcceptStaticOnEquals(false);
    }
    
    private void appendSuperStubHeader(StringBuilder b, List<BytecodeMethod> bm, ByteCodeClass base) {
        BytecodeMethod.setAcceptStaticOnEquals(true);
        // append super stub
        for(BytecodeMethod m : base.methods) {
            if(!m.isPrivate() && !bm.contains(m)) {
                m.appendMethodHeader(b, clsName);
                bm.add(m);
            }
        }
        if(base.baseClassObject != null) {
            appendSuperStubHeader(b, bm, base.baseClassObject);
        }
        BytecodeMethod.setAcceptStaticOnEquals(false);
    }
    
    private void buildInstanceFieldList(List<ByteCodeField> fieldList) {
        for(ByteCodeField bf : fields) {
            if(!bf.isStaticField() && !fieldList.contains(bf)) {
                fieldList.add(bf);
            } 
        }
        if(baseClassObject != null) {
            baseClassObject.buildInstanceFieldList(fieldList);
        }        
    } 

    private List<ByteCodeField> buildStaticFieldList(List<ByteCodeField> fieldList) {
        for(ByteCodeField bf : fields) {
            if(bf.isStaticField() && !fieldList.contains(bf)) {
                fieldList.add(bf);
            } 
        }
        if(baseInterfacesObject != null) {
            for(ByteCodeClass baseInterface : baseInterfacesObject) {
                if(baseInterface.fields != null) {
                    for(ByteCodeField bf : baseInterface.fields) {
                        if(!fieldList.contains(bf)) {
                            fieldList.add(bf);
                        } 
                    }
                }
            }
        }
        if(baseClassObject != null) {
            baseClassObject.buildStaticFieldList(fieldList);
        }        
        return fieldList;
    } 
    
    private void addFields(StringBuilder b) {
        if(baseClassObject != null) {
            baseClassObject.addFields(b);
        }
        for(ByteCodeField bf : fields) {
            if(!bf.isStaticField()) {
                b.append("    ");
                b.append(bf.getCDefinition());
                b.append(" ");
                b.append(clsName);
                b.append("_");
                b.append(bf.getFieldName());
                b.append(";\n");
            } 
        }
    }
    
    public String generateCHeader() {
        StringBuilder b = new StringBuilder();
        b.append("#ifndef __");
        b.append(clsName.toUpperCase());
        b.append("__\n");
        b.append("#define __");
        b.append(clsName.toUpperCase());
        b.append("__\n\n");

        b.append("#include \"cn1_globals.h\"\n");
        
        for(String s : dependsClassesInterfaces) {
            if(s.startsWith("java_lang_annotation") || s.startsWith("java_lang_Deprecated") || 
                    s.startsWith("java_lang_Override") || s.startsWith("java_lang_SuppressWarnings")) {
                continue;
            }
            b.append("#include \"");
            b.append(s);
            b.append(".h\"\n");
        }

        b.append("extern struct clazz class__");
        b.append(clsName);
        b.append(";\n");

        if(arrayTypes.contains("1_" + clsName) || arrayTypes.contains("2_" + clsName) || arrayTypes.contains("3_" + clsName)) {
            b.append("extern struct clazz class_array1__");
            b.append(clsName);
            b.append(";\n");
        }

        if(arrayTypes.contains("2_" + clsName) || arrayTypes.contains("3_" + clsName)) {
            b.append("extern struct clazz class_array2__");
            b.append(clsName);
            b.append(";\n");
        }

        if(arrayTypes.contains("3_" + clsName)) {
            b.append("extern struct clazz class_array3__");
            b.append(clsName);
            b.append(";\n");
        }

        if(!isInterface) {
            b.append("extern void __INIT_VTABLE_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE, void** vtable);\n");
        }

        b.append("extern void __STATIC_INITIALIZER_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE);\n");
        
        b.append("extern void __FINALIZER_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT objToDelete);\n");

        b.append("extern void __GC_MARK_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT objToMark, JAVA_BOOLEAN force);\n");
        
        if(!isInterface && !isAbstract) {
            b.append("extern JAVA_OBJECT __NEW_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE);\n");

            if(hasDefaultConstructor()) {
                b.append("extern JAVA_OBJECT __NEW_INSTANCE_");
                b.append(clsName);
                b.append("(CODENAME_ONE_THREAD_STATE);\n");
            }
        }
                
        if(arrayTypes.contains("1_" + clsName)) {
            b.append("extern JAVA_OBJECT __NEW_ARRAY_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE, JAVA_INT size);\n");
        }
        
        appendMethodsToHeader(b);
        
        if(baseClassObject != null) {
            // append super stub
            List<BytecodeMethod> bm = new ArrayList<BytecodeMethod>(methods);
            appendSuperStubHeader(b, bm, baseClassObject);
        }
        
        for(BytecodeMethod m : virtualMethodList) {
            if(m.isVirtualOverriden()) {
                b.append("#define virtual_");
                b.append(m.getClsName());
                b.append("_");
                b.append(m.getMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append(" ");
                b.append(m.getClsName());
                b.append("_");
                b.append(m.getMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append("\n");
            } else {
                m.appendVirtualMethodHeader(b, clsName);
            }
        }

        // static fields for the class
        for(ByteCodeField bf : staticFieldList) {
            if(bf.isStaticField()) {
                if(bf.getClsName().equals(clsName)) {
                    b.append("extern ");
                    b.append(bf.getCDefinition());
                    b.append(" get_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append("();\n");
                    if(!(bf.isFinal() && bf.getValue() != null)) {
                        b.append("extern ");
                        b.append(bf.getCDefinition());
                        b.append(" STATIC_FIELD_");
                        b.append(clsName);
                        b.append("_");
                        b.append(bf.getFieldName());
                        b.append(";\n");

                        b.append("extern void");
                        b.append(" set_static_");
                        b.append(clsName);
                        b.append("_");
                        b.append(bf.getFieldName());
                        b.append("(CODENAME_ONE_THREAD_STATE, ");
                        b.append(bf.getCDefinition());
                        b.append(" v);\n");
                    }
                } else {
                    b.append("#define get_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append("(threadStateArgument) get_static_");
                    b.append(bf.getClsName());
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append("(threadStateArgument)\n");

                    b.append("#define set_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append("(threadStateArgument, valueArgument) set_static_");
                    b.append(bf.getClsName());
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append("(threadStateArgument, valueArgument)\n");
                }
            }
        }

        for(ByteCodeField fld : fullFieldList) {
            b.append(fld.getCDefinition());
            b.append(" get_field_");
            b.append(clsName);
            b.append("_");
            b.append(fld.getFieldName());
            b.append("(JAVA_OBJECT t);\n");

            b.append("void set_field_");
            b.append(clsName);
            b.append("_");
            b.append(fld.getFieldName());
            b.append("(CODENAME_ONE_THREAD_STATE, ");
            b.append(fld.getCDefinition());
            b.append(" __cn1Val, JAVA_OBJECT __cn1T);\n");
        }
        
        b.append("\n\n");

        // object struct contains instace field variables
        b.append("struct obj__");
        b.append(clsName);
        b.append(" {\n");
        // reference to the class, reference counter for the arc portion of the GC
        // and a mutex for synchronization code
        b.append("    DEBUG_GC_VARIABLES\n    struct clazz *__codenameOneParentClsReference;\n");
        b.append("    int __codenameOneReferenceCount;\n");
        b.append("    void* __codenameOneThreadData;\n");
        b.append("    int __codenameOneGcMark;\n");
        b.append("    void* __ownerThread;\n");
        b.append("    int __heapPosition;\n");

        
        addFields(b);
        
        b.append("};\n\n");
                     
        
        b.append("\n\n#endif //__");
        b.append(clsName.toUpperCase());
        b.append("__\n");
        return b.toString();
    }

    private void appendMethodsToHeader(StringBuilder b) {        
        for(BytecodeMethod m : methods) {
            m.appendMethodHeader(b);
            
            /*if(!m.isForceVirtual() && m.isVirtualBlockedDueToFinal() && !m.isVirtualOverriden()) {
                b.append("#define virtual_");
                b.append(clsName);
                b.append("_");
                b.append(m.getMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append(" ");
                b.append(m.getClsName());
                b.append("_");
                b.append(m.getMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append("\n");
            }*/
        }
        
        /*if(baseClassObject != null) {
            baseClassObject.appendVirtualBlockedMethodsToHeader(b, clsName);
        }*/
    }

    /*private void appendVirtualBlockedMethodsToHeader(StringBuilder b, String clsName) {        
        for(BytecodeMethod m : methods) {
            if(m.isVirtualBlockedDueToFinal() && !m.isVirtualOverriden()) {
                b.append("#define virtual_");
                b.append(clsName);
                b.append("_");
                b.append(m.getMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append(" ");
                b.append(m.getClsName());
                b.append("_");
                b.append(m.getMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append("\n");
            }
        }
        if(baseClassObject != null) {
            baseClassObject.appendVirtualBlockedMethodsToHeader(b, clsName);
        }
    }*/

    
    /**
     * @param baseClass the baseClass to set
     */
    public void setBaseClass(String baseClass) {
        this.baseClass = baseClass;
        if(baseClass != null) {
            String b = baseClass.replace('/', '_').replace('$', '_');
            if(!dependsClassesInterfaces.contains(b)) {
                dependsClassesInterfaces.add(b);
            }
        }
    }
    
    public void setBaseInterfaces(String[] interfaces) {
        baseInterfaces = Arrays.asList(interfaces);
        if(baseInterfaces != null) {
            for(String s : interfaces) {
                s = s.replace('/', '_').replace('$', '_');
                if(!dependsClassesInterfaces.contains(s)) {
                    dependsClassesInterfaces.add(s);
                }
            }
        }
    }

    /**
     * @return the clsName
     */
    public String getClsName() {
        return clsName;
    }

    /**
     * @return the baseClassObject
     */
    public ByteCodeClass getBaseClassObject() {
        return baseClassObject;
    }

    /**
     * @param baseClassObject the baseClassObject to set
     */
    public void setBaseClassObject(ByteCodeClass baseClassObject) {
        this.baseClassObject = baseClassObject;
    }

    /**
     * @return the baseInterfacesObject
     */
    public List<ByteCodeClass> getBaseInterfacesObject() {
        return baseInterfacesObject;
    }

    /**
     * @param baseInterfacesObject the baseInterfacesObject to set
     */
    public void setBaseInterfacesObject(List<ByteCodeClass> baseInterfacesObject) {
        this.baseInterfacesObject = baseInterfacesObject;
    }

    /**
     * @return the baseInterfaces
     */
    public List<String> getBaseInterfaces() {
        return baseInterfaces;
    }
    
    public void fillVirtualMethodTable(List<BytecodeMethod> virtualMethods) {
        fillVirtualMethodTable(virtualMethods, true);
    }
    
    private void fillVirtualMethodTable(List<BytecodeMethod> virtualMethods, boolean replace) {
        if(baseClassObject != null) {
            baseClassObject.fillVirtualMethodTable(virtualMethods, true);
        }
        if(baseInterfacesObject != null) {
            for(ByteCodeClass bc : baseInterfacesObject) {
                bc.fillVirtualMethodTable(virtualMethods, false);
            }
        }
        for(BytecodeMethod bm : methods) {
            if(bm.canBeVirtual()) {
                int offset = virtualMethods.indexOf(bm);
                if(offset < 0) {
                    virtualMethods.add(bm);
                    if(isInterface) {
                        bm.setForceVirtual(true);
                    }
                } else {
                    if(replace) {
                        virtualMethods.set(offset, bm);
                        if(isInterface) {
                            bm.setForceVirtual(true);
                        }
                    }
                }
            } else {
                
            } 
        }
    }
    
    /*private boolean isMethodIn(ByteCodeClass bc, BytecodeMethod bm) {
        if(methods.contains(bm)) {
            return true;
        }
        if(baseInterfacesObject != null) {
            for(ByteCodeClass cc : baseInterfacesObject) {
                if(isMethodIn(cc, bm)) {
                    return true;
                }
            }
        }
        if(baseClassObject != null) {
            return isMethodIn(baseClassObject, bm);
        }
        return false;
    }*/

    /**
     * @return the baseClass
     */
    public String getBaseClass() {
        return baseClass;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    } 

    /**
     * @return the classOffset
     */
    public int getClassOffset() {
        return classOffset;
    }

    /**
     * @param classOffset the classOffset to set
     */
    public void setClassOffset(int classOffset) {
        this.classOffset = classOffset;
    }
    
    public int updateMethodOffsets(int initial) {
        for(BytecodeMethod m : methods) {
            m.setMethodOffset(initial);
            initial++;
        }
        return initial;
    }
    
    public int getMethodCountIncludingBase() {
        int size = methods.size();
        if(baseClassObject != null) {
            size += baseClassObject.getMethodCountIncludingBase();
        }
        if(baseInterfacesObject != null) {
            for(ByteCodeClass bo : baseInterfacesObject) {
                size += bo.getMethodCountIncludingBase();
            }
        }
        return size;
    }
    
    public List<BytecodeMethod> getMethods() {
        return methods;
    }

    /**
     * @return the isInterface
     */
    public boolean isIsInterface() {
        return isInterface;
    }

    /**
     * @param isInterface the isInterface to set
     */
    public void setIsInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }
    
    public void setIsUnitTest(boolean isUnitTest) {
        this.isUnitTest = isUnitTest;
    }

    /**
     * @return the isAbstract
     */
    public boolean isIsAbstract() {
        return isAbstract;
    }

    /**
     * @param isAbstract the isAbstract to set
     */
    public void setIsAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }
    
    private void appendClassVFunctions(StringBuilder b) {
        // special case, class has no Class object within it so no real virtual functions
        b.append("JAVA_BOOLEAN virtual_java_lang_Class_equals___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1) {\n" +
            "    return java_lang_Object_equals___java_lang_Object_R_boolean(threadStateData, __cn1ThisObject, __cn1Arg1);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_OBJECT virtual_java_lang_Class_getClass___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    return java_lang_Object_getClass___R_java_lang_Class(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_INT virtual_java_lang_Class_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    return java_lang_Object_hashCode___R_int(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_VOID virtual_java_lang_Class_notify__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    java_lang_Object_notify__(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "JAVA_VOID virtual_java_lang_Class_notifyAll__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    java_lang_Object_notifyAll__(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_OBJECT virtual_java_lang_Class_toString___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    return java_lang_Object_toString___R_java_lang_String(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_VOID virtual_java_lang_Class_wait__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    java_lang_Object_wait__(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_VOID virtual_java_lang_Class_wait___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1) {\n" +
            "    java_lang_Object_wait___long(threadStateData, __cn1ThisObject, __cn1Arg1);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_VOID virtual_java_lang_Class_wait___long_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2) {\n" +
            "    java_lang_Object_wait___long_int(threadStateData, __cn1ThisObject, __cn1Arg1, __cn1Arg2);\n" +
            "}\n");
    }

    /**
     * @return the finalClass
     */
    public boolean isFinalClass() {
        return finalClass;
    }

    /**
     * @param finalClass the finalClass to set
     */
    public void setFinalClass(boolean finalClass) {
        this.finalClass = finalClass;
    }
    
    public void appendStaticFieldsExtern(StringBuilder b) {
        for(ByteCodeField bf : fields) {
            if(bf.isStaticField() && bf.isObjectType() && !bf.shouldRemoveFromHeapCollection()) {
                b.append("extern ");
                b.append(bf.getCDefinition());
                b.append(" STATIC_FIELD_");
                b.append(clsName);
                b.append("_");
                b.append(bf.getFieldName());
                b.append(";\n");
            }
        }
    }

    private boolean isTrulyFinal(ByteCodeField bf) {
        if(bf.isFinal()) {
            if(bf.isObjectType()) {
                if(bf.getType() != null) {
                    return bf.getType().endsWith("String");
                }
            } else {
                return true;
            }
        }
        return false;
    }
    
    public void appendStaticFieldsMark(StringBuilder b) {
        for(ByteCodeField bf : fields) {
            if(bf.isStaticField() && bf.isObjectType() && !bf.shouldRemoveFromHeapCollection()) {
                b.append("    gcMarkObject(threadStateData, STATIC_FIELD_");
                b.append(clsName);
                b.append("_");
                b.append(bf.getFieldName());
                b.append(", JAVA_TRUE);\n");
            }
        }
    }

    /**
     * @return the isUsedByNative
     */
    public boolean isUsedByNative() {
        if (!usedByNative){
            for (BytecodeMethod m : methods){
                if (m.isUsedByNative()){
                    usedByNative = true;
                    break;
                }
            }
        }
        return usedByNative;
    }

    boolean isUnitTest() {
        return isUnitTest;
    }

    
}
