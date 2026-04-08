/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *                                                                           *
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/

package bsh;

import static bsh.ClassGenerator.Type.CLASS;
import static bsh.ClassGenerator.Type.ENUM;
import static bsh.ClassGenerator.Type.INTERFACE;
import static bsh.This.Keys.BSHCLASSMODIFIERS;
import static bsh.This.Keys.BSHCONSTRUCTORS;
import static bsh.This.Keys.BSHINIT;
import static bsh.This.Keys.BSHSTATIC;
import static bsh.This.Keys.BSHTHIS;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import bsh.org.objectweb.asm.ClassWriter;
import bsh.org.objectweb.asm.Label;
import bsh.org.objectweb.asm.MethodVisitor;
import bsh.org.objectweb.asm.Opcodes;
import bsh.org.objectweb.asm.Type;

/**
 * ClassGeneratorUtil utilizes the ASM (www.objectweb.org) bytecode generator
 * by Eric Bruneton in order to generate class "stubs" for BeanShell at
 * runtime.
 * <p/>
 * <p/>
 * Stub classes contain all of the fields of a BeanShell scripted class
 * as well as two "callback" references to BeanShell namespaces: one for
 * static methods and one for instance methods. Methods of the class are
 * delegators which invoke corresponding methods on either the static or
 * instance bsh object and then unpack and return the results. The static
 * namespace utilizes a static import to delegate variable access to the
 * class' static fields. The instance namespace utilizes a dynamic import
 * (i.e. mixin) to delegate variable access to the class' instance variables.
 * <p/>
 * <p/>
 * Constructors for the class delegate to the static initInstance() method of
 * ClassGeneratorUtil to initialize new instances of the object. initInstance()
 * invokes the instance intializer code (init vars and instance blocks) and
 * then delegates to the corresponding scripted constructor method in the
 * instance namespace. Constructors contain special switch logic which allows
 * the BeanShell to control the calling of alternate constructors (this() or
 * super() references) at runtime.
 * <p/>
 * <p/>
 * Specially named superclass delegator methods are also generated in order to
 * allow BeanShell to access overridden methods of the superclass (which
 * reflection does not normally allow).
 * <p/>
 *
 * @author Pat Niemeyer
 */
public class ClassGeneratorUtil implements Opcodes {
    /**
     * The switch branch number for the default constructor.
     * The value -1 will cause the default branch to be taken.
     */
    static final int DEFAULTCONSTRUCTOR = -1;
    static final int ACCESS_MODIFIERS =
            ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED;

    private static final String OBJECT = "Ljava/lang/Object;";

    private final String className;
    private final String classDescript;
    /**
     * fully qualified class name (with package) e.g. foo/bar/Blah
     */
    private final String fqClassName;
    private final String uuid;
    private final Class<?> superClass;
    private final String superClassName;
    private final Class<?>[] interfaces;
    private final Variable[] vars;
    private final DelayedEvalBshMethod[] constructors;
    private final DelayedEvalBshMethod[] methods;
    private final Modifiers classModifiers;
    private final ClassGenerator.Type type;

    /**
     * @param packageName e.g. "com.foo.bar"
     */
    public ClassGeneratorUtil(Modifiers classModifiers, String className,
            String packageName, Class<?> superClass, Class<?>[] interfaces,
            Variable[] vars, DelayedEvalBshMethod[] bshmethods,
            NameSpace classStaticNameSpace, ClassGenerator.Type type) {
        this.classModifiers = classModifiers;
        this.className = className;
        this.type = type;
        if (packageName != null)
            this.fqClassName = packageName.replace('.', '/') + "/" + className;
        else
            this.fqClassName = className;
        this.classDescript = "L"+fqClassName.replace('.', '/')+";";

        if (superClass == null)
            if (type == ENUM)
                superClass = Enum.class;
            else
                superClass = Object.class;
        this.superClass = superClass;
        this.superClassName = Type.getInternalName(superClass);
        if (interfaces == null)
            interfaces = Reflect.ZERO_TYPES;
        this.interfaces = interfaces;
        this.vars = vars;
        classStaticNameSpace.isInterface = type == INTERFACE;
        classStaticNameSpace.isEnum = type == ENUM;
        This.contextStore.put(this.uuid = UUID.randomUUID().toString(), classStaticNameSpace);

        // Split the methods into constructors and regular method lists
        List<DelayedEvalBshMethod> consl = new ArrayList<>();
        List<DelayedEvalBshMethod> methodsl = new ArrayList<>();
        String classBaseName = Types.getBaseName(className); // for inner classes
        for (DelayedEvalBshMethod bshmethod : bshmethods)
            if (bshmethod.getName().equals(classBaseName)) {
                if (!bshmethod.modifiers.isAppliedContext(Modifiers.CONSTRUCTOR))
                    bshmethod.modifiers.changeContext(Modifiers.CONSTRUCTOR);
                consl.add(bshmethod);
            } else
                methodsl.add(bshmethod);

        constructors = consl.toArray(new DelayedEvalBshMethod[consl.size()]);
        methods = methodsl.toArray(new DelayedEvalBshMethod[methodsl.size()]);

        Interpreter.debug("Generate class ", type, " ", fqClassName, " cons:",
                consl.size(), " meths:", methodsl.size(), " vars:", vars.length);

        if (type == INTERFACE && !classModifiers.hasModifier("abstract"))
            classModifiers.addModifier("abstract");
        if (type == ENUM && !classModifiers.hasModifier("static"))
            classModifiers.addModifier("static");
    }

    /**
     * This method provides a hook for the class generator implementation to
     * store additional information in the class's bsh static namespace.
     * Currently this is used to store an array of consructors corresponding
     * to the constructor switch in the generated class.
     *
     * This method must be called to initialize the static space even if we
     * are using a previously generated class.
     */
    public void initStaticNameSpace(NameSpace classStaticNameSpace, BSHBlock instanceInitBlock) {
        try {
            classStaticNameSpace.setLocalVariable(""+BSHCLASSMODIFIERS, classModifiers, false/*strict*/);
            classStaticNameSpace.setLocalVariable(""+BSHCONSTRUCTORS, constructors, false/*strict*/);
            classStaticNameSpace.setLocalVariable(""+BSHINIT, instanceInitBlock, false/*strict*/);
        } catch (UtilEvalError e) {
            throw new InterpreterError("Unable to init class static block: " + e, e);
        }
    }

    /**
     * Generate the class bytecode for this class.
     */
    public byte[] generateClass() {
        NameSpace classStaticNameSpace = This.contextStore.get(this.uuid);
        // Force the class public for now...
        int classMods = getASMModifiers(classModifiers) | ACC_PUBLIC;
        if (type == INTERFACE)
            classMods |= ACC_INTERFACE | ACC_ABSTRACT;
        else if (type == ENUM)
            classMods |= ACC_FINAL | ACC_SUPER | ACC_ENUM;
        else {
            classMods |= ACC_SUPER;
            if ( (classMods & ACC_ABSTRACT) > 0 )
                // bsh classes are not abstract
                classMods -= ACC_ABSTRACT;
        }

        String[] interfaceNames = new String[interfaces.length + 1]; // +1 for GeneratedClass
        for (int i = 0; i < interfaces.length; i++) {
            interfaceNames[i] = Type.getInternalName(interfaces[i]);
            if (Reflect.isGeneratedClass(interfaces[i]))
                for (Variable v : Reflect.getVariables(interfaces[i]))
                    classStaticNameSpace.setVariableImpl(v);
        }
        // Everyone implements GeneratedClass
        interfaceNames[interfaces.length] = Type.getInternalName(GeneratedClass.class);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        String signature = type == ENUM ? "Ljava/lang/Enum<"+classDescript+">;" : null;
        cw.visit(V1_8, classMods, fqClassName, signature, superClassName, interfaceNames);

        if ( type != INTERFACE )
            // Generate the bsh instance 'This' reference holder field
            generateField(BSHTHIS+className, "Lbsh/This;", ACC_PUBLIC, cw);
        // Generate the static bsh static This reference holder field
        generateField(BSHSTATIC+className, "Lbsh/This;", ACC_PUBLIC + ACC_STATIC + ACC_FINAL, cw);
        // Generate class UUID
        generateField("UUID", "Ljava/lang/String;", ACC_PUBLIC + ACC_STATIC + ACC_FINAL, this.uuid, cw);

        // Generate the fields
        for (Variable var : vars) {
            // Don't generate private fields
            if (var.hasModifier("private"))
                continue;

            String fType = var.getTypeDescriptor();
            int modifiers = getASMModifiers(var.getModifiers());

            if ( type == INTERFACE ) {
                var.setConstant();
                classStaticNameSpace.setVariableImpl(var);
                // keep constant fields virtual
                continue;
            } else if ( type == ENUM && var.hasModifier("enum") ) {
                modifiers |= ACC_ENUM | ACC_FINAL;
                fType = classDescript;
            }

            generateField(var.getName(), fType, modifiers, cw);
        }

        if (type == ENUM)
            generateEnumSupport(fqClassName, className, classDescript, cw);

        // Generate the static initializer.
        generateStaticInitializer(cw);

        // Generate the constructors
        boolean hasConstructor = false;
        for (int i = 0; i < constructors.length; i++) {
            // Don't generate private constructors
            if (constructors[i].hasModifier("private"))
                continue;

            int modifiers = getASMModifiers(constructors[i].getModifiers());
            if (constructors[i].isVarArgs())
               modifiers |= ACC_VARARGS;
            generateConstructor(i, constructors[i].getParamTypeDescriptors(), modifiers, cw);
            hasConstructor = true;
        }

        // If no other constructors, generate a default constructor
        if ( type == CLASS && !hasConstructor )
            generateConstructor(DEFAULTCONSTRUCTOR/*index*/, new String[0], ACC_PUBLIC, cw);

        // Generate methods
        for (DelayedEvalBshMethod method : methods) {

            // Don't generate private methods
            if (method.hasModifier("private"))
                continue;

            if ( type == INTERFACE
                    && !method.hasModifier("static")
                    && !method.hasModifier("default")
                    && !method.hasModifier("abstract") )
                method.getModifiers().addModifier("abstract");
            int modifiers = getASMModifiers(method.getModifiers());
            if (method.isVarArgs())
               modifiers |= ACC_VARARGS;
            boolean isStatic = (modifiers & ACC_STATIC) > 0;

            generateMethod(className, fqClassName, method.getName(), method.getReturnTypeDescriptor(),
                    method.getParamTypeDescriptors(), modifiers, cw);

            // check if method overrides existing method and generate super delegate.
            if ( null != classContainsMethod(superClass, method.getName(), method.getParamTypeDescriptors()) && !isStatic )
                generateSuperDelegateMethod(superClass, superClassName, method.getName(), method.getReturnTypeDescriptor(),
                        method.getParamTypeDescriptors(), ACC_PUBLIC, cw);
        }

        return cw.toByteArray();
    }

    /**
     * Translate bsh.Modifiers into ASM modifier bitflags.
     * Only a subset of modifiers are baked into classes.
     */
    private static int getASMModifiers(Modifiers modifiers) {
        int mods = 0;

        if (modifiers.hasModifier(ACC_PUBLIC))
            mods |= ACC_PUBLIC;
        if (modifiers.hasModifier(ACC_PRIVATE))
            mods |= ACC_PRIVATE;
        if (modifiers.hasModifier(ACC_PROTECTED))
            mods |= ACC_PROTECTED;
        if (modifiers.hasModifier(ACC_STATIC))
            mods |= ACC_STATIC;
        if (modifiers.hasModifier(ACC_SYNCHRONIZED))
            mods |= ACC_SYNCHRONIZED;
        if (modifiers.hasModifier(ACC_ABSTRACT))
            mods |= ACC_ABSTRACT;

        // if no access modifiers declared then we make it public
        if ( ( modifiers.getModifiers() & ACCESS_MODIFIERS ) == 0 ) {
            mods |= ACC_PUBLIC;
            modifiers.addModifier(ACC_PUBLIC);
        }

        return mods;
    }

    /** Generate a field - static or instance. */
    private static void generateField(String fieldName, String type, int modifiers, ClassWriter cw) {
        generateField(fieldName, type, modifiers, null/*value*/, cw);
    }
    /** Generate field and assign initial value. */
    private static void generateField(String fieldName, String type, int modifiers, Object value, ClassWriter cw) {
        cw.visitField(modifiers, fieldName, type, null/*signature*/, value);
    }

    /**
     * Build the signature for the supplied parameter types.
     * @param paramTypes list of parameter types
     * @return parameter type signature
     */
    private static String getTypeParameterSignature(String[] paramTypes) {
        StringBuilder sb = new StringBuilder("<");
        for (final String pt : paramTypes)
            sb.append(pt).append(":");
        return sb.toString();
    }

    /** Generate support code needed for Enum types.
     * Generates enum values and valueOf methods, default private constructor with initInstance call.
     * Instead of maintaining a synthetic array of enum values we greatly reduce the required bytecode
     * needed by delegating to This.enumValues and building the array dynamically.
     * @param fqClassName fully qualified class name
     * @param className class name string
     * @param classDescript class descriptor string
     * @param cw current class writer */
    private void generateEnumSupport(String fqClassName, String className, String classDescript, ClassWriter cw) {
        // generate enum values() method delegated to static This.enumValues.
        MethodVisitor cv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "values", "()["+classDescript, null, null);
        pushBshStatic(fqClassName, className, cv);
        cv.visitMethodInsn(INVOKEVIRTUAL, "bsh/This", "enumValues", "()[Ljava/lang/Object;", false);
        generatePlainReturnCode("["+classDescript, cv);
        cv.visitMaxs(0, 0);
        // generate Enum.valueOf delegate method
        cv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "valueOf", "(Ljava/lang/String;)"+classDescript, null, null);
        cv.visitLdcInsn(Type.getType(classDescript));
        cv.visitVarInsn(ALOAD, 0);
        cv.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
        generatePlainReturnCode(classDescript, cv);
        cv.visitMaxs(0, 0);
        // generate default private constructor and initInstance call
        cv = cw.visitMethod(ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V", null, null);
        cv.visitVarInsn(ALOAD, 0);
        cv.visitVarInsn(ALOAD, 1);
        cv.visitVarInsn(ILOAD, 2);
        cv.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false);
        cv.visitVarInsn(ALOAD, 0);
        cv.visitLdcInsn(className);
        generateParameterReifierCode(new String[0], false/*isStatic*/, cv);
        cv.visitMethodInsn(INVOKESTATIC, "bsh/This", "initInstance", "(Lbsh/GeneratedClass;Ljava/lang/String;[Ljava/lang/Object;)V", false);
        cv.visitInsn(RETURN);
        cv.visitMaxs(0, 0);
    }

    /** Generate the static initialization of the enum constants. Called from clinit.
     * @param fqClassName fully qualified class name
     * @param classDescript class descriptor string
     * @param cv clinit method visitor */
    private void generateEnumStaticInit(String fqClassName, String classDescript, MethodVisitor cv) {
        int ordinal = ICONST_0;
        for ( Variable var : vars ) if ( var.hasModifier("enum") ) {
            cv.visitTypeInsn(NEW, fqClassName);
            cv.visitInsn(DUP);
            cv.visitLdcInsn(var.getName());
            if ( ICONST_5 >= ordinal )
                cv.visitInsn(ordinal++);
            else
                cv.visitIntInsn(BIPUSH, ordinal++ - ICONST_0);
            cv.visitMethodInsn(INVOKESPECIAL, fqClassName, "<init>", "(Ljava/lang/String;I)V", false);
            cv.visitFieldInsn(PUTSTATIC, fqClassName, var.getName(), classDescript);
        }
    }

    /**
     * Generate a delegate method - static or instance.
     * The generated code packs the method arguments into an object array
     * (wrapping primitive types in bsh.Primitive), invokes the static or
     * instance This invokeMethod() method, and then returns
     * the result.
     */
    private void generateMethod(String className, String fqClassName, String methodName, String returnType, String[] paramTypes, int modifiers, ClassWriter cw) {
        String[] exceptions = null;
        boolean isStatic = (modifiers & ACC_STATIC) != 0;

        if (returnType == null) // map loose return type to Object
            returnType = OBJECT;

        String methodDescriptor = getMethodDescriptor(returnType, paramTypes);

        String paramTypesSig = getTypeParameterSignature(paramTypes);

        // Generate method body
        MethodVisitor cv = cw.visitMethod(modifiers, methodName, methodDescriptor, paramTypesSig, exceptions);

        if ((modifiers & ACC_ABSTRACT) != 0)
            return;

        // Generate code to push the BSHTHIS or BSHSTATIC field
        if ( isStatic||type == INTERFACE )
            pushBshStatic(fqClassName, className, cv);
        else
            pushBshThis(fqClassName, className, cv);

        // Push the name of the method as a constant
        cv.visitLdcInsn(methodName);

        // Generate code to push arguments as an object array
        generateParameterReifierCode(paramTypes, isStatic, cv);

        // Push the boolean constant 'true' (for declaredOnly)
        cv.visitInsn(ICONST_1);

        // Invoke the method This.invokeMethod( name, Class [] sig, boolean )
        cv.visitMethodInsn(INVOKEVIRTUAL, "bsh/This", "invokeMethod", "(Ljava/lang/String;[Ljava/lang/Object;Z)Ljava/lang/Object;", false);

        // Generate code to return the value
        generateReturnCode(returnType, cv);

        // values here are ignored, computed automatically by ClassWriter
        cv.visitMaxs(0, 0);
    }

    /**
     * Generate a constructor.
     */
    void generateConstructor(int index, String[] paramTypes, int modifiers, ClassWriter cw) {
        /** offset after params of the args object [] var */
        final int argsVar = paramTypes.length + 1;
        /** offset after params of the ConstructorArgs var */
        final int consArgsVar = paramTypes.length + 2;

        String[] exceptions = null;
        String methodDescriptor = getMethodDescriptor("V", paramTypes);

        String paramTypesSig = getTypeParameterSignature(paramTypes);

        // Create this constructor method
        MethodVisitor cv = cw.visitMethod(modifiers, "<init>", methodDescriptor, paramTypesSig, exceptions);

        // Generate code to push arguments as an object array
        generateParameterReifierCode(paramTypes, false/*isStatic*/, cv);
        cv.visitVarInsn(ASTORE, argsVar);

        // Generate the code implementing the alternate constructor switch
        generateConstructorSwitch(index, argsVar, consArgsVar, cv);

        // Generate code to invoke the ClassGeneratorUtil initInstance() method

        // push 'this'
        cv.visitVarInsn(ALOAD, 0);

        // Push the class/constructor name as a constant
        cv.visitLdcInsn(className);

        // Push arguments as an object array
        cv.visitVarInsn(ALOAD, argsVar);

        // invoke the initInstance() method
        cv.visitMethodInsn(INVOKESTATIC, "bsh/This", "initInstance", "(Lbsh/GeneratedClass;Ljava/lang/String;[Ljava/lang/Object;)V", false);

        cv.visitInsn(RETURN);

        // values here are ignored, computed automatically by ClassWriter
        cv.visitMaxs(0, 0);
    }

    /**
     * Generate the static initializer for the class
     */
    void generateStaticInitializer(ClassWriter cw) {

        // Generate code to invoke the ClassGeneratorUtil initStatic() method
        MethodVisitor cv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null/*sig*/, null/*exceptions*/);

        // initialize _bshStaticThis
        cv.visitFieldInsn(GETSTATIC, fqClassName, "UUID", "Ljava/lang/String;");
        cv.visitMethodInsn(INVOKESTATIC, "bsh/This", "pullBshStatic", "(Ljava/lang/String;)Lbsh/This;", false);
        cv.visitFieldInsn(PUTSTATIC, fqClassName, BSHSTATIC+className, "Lbsh/This;");

        if ( type == ENUM )
            generateEnumStaticInit(fqClassName, classDescript, cv);

        // equivalent of my.ClassName.class
        cv.visitLdcInsn(Type.getType(classDescript));

        // invoke the initStatic() method
        cv.visitMethodInsn(INVOKESTATIC, "bsh/This", "initStatic", "(Ljava/lang/Class;)V", false);

        cv.visitInsn(RETURN);

        // values here are ignored, computed automatically by ClassWriter
        cv.visitMaxs(0, 0);
    }

    /**
     * Generate a switch with a branch for each possible alternate
     * constructor. This includes all superclass constructors and all
     * constructors of this class. The default branch of this switch is the
     * default superclass constructor.
     * <p/>
     * This method also generates the code to call the static
     * ClassGeneratorUtil
     * getConstructorArgs() method which inspects the scripted constructor to
     * find the alternate constructor signature (if any) and evaluate the
     * arguments at runtime. The getConstructorArgs() method returns the
     * actual arguments as well as the index of the constructor to call.
     */
    void generateConstructorSwitch(int consIndex, int argsVar, int consArgsVar,
            MethodVisitor cv) {
        Label defaultLabel = new Label();
        Label endLabel = new Label();
        List<Invocable> superConstructors = BshClassManager.memberCache
                .get(superClass).members(superClass.getName());
        int cases =  superConstructors.size() + constructors.length;

        Label[] labels = new Label[cases];
        for (int i = 0; i < cases; i++)
            labels[i] = new Label();

        // Generate code to call ClassGeneratorUtil to get our switch index
        // and give us args...

        // push super class name .class
        cv.visitLdcInsn(Type.getType(BSHType.getTypeDescriptor(superClass)));

        // Push the bsh static namespace field
        pushBshStatic(fqClassName, className, cv);

        // push args
        cv.visitVarInsn(ALOAD, argsVar);

        // push this constructor index number onto stack
        cv.visitIntInsn(BIPUSH, consIndex);

        // invoke the ClassGeneratorUtil getConstructorsArgs() method
        cv.visitMethodInsn(INVOKESTATIC, "bsh/This", "getConstructorArgs", "(Ljava/lang/Class;Lbsh/This;[Ljava/lang/Object;I)" + "Lbsh/This$ConstructorArgs;", false);

        // store ConstructorArgs in consArgsVar
        cv.visitVarInsn(ASTORE, consArgsVar);

        // Get the ConstructorArgs selector field from ConstructorArgs

        // push ConstructorArgs
        cv.visitVarInsn(ALOAD, consArgsVar);
        cv.visitFieldInsn(GETFIELD, "bsh/This$ConstructorArgs", "selector", "I");

        // start switch
        cv.visitTableSwitchInsn(0/*min*/, cases - 1/*max*/, defaultLabel, labels);

        // generate switch body
        int index = 0;
        for (int i = 0; i < superConstructors.size(); i++, index++)
            doSwitchBranch(index, superClassName, superConstructors.get(i).getParamTypeDescriptors(), endLabel, labels, consArgsVar, cv);
        for (int i = 0; i < constructors.length; i++, index++)
            doSwitchBranch(index, fqClassName, constructors[i].getParamTypeDescriptors(), endLabel, labels, consArgsVar, cv);

        // generate the default branch of switch
        cv.visitLabel(defaultLabel);
        // default branch always invokes no args super
        cv.visitVarInsn(ALOAD, 0); // push 'this'
        cv.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", "()V", false);

        // done with switch
        cv.visitLabel(endLabel);
    }

    // push the class static This object
    private static void pushBshStatic(String fqClassName, String className, MethodVisitor cv) {
        cv.visitFieldInsn(GETSTATIC, fqClassName, BSHSTATIC + className, "Lbsh/This;");
    }

    // push the class instance This object
    private static void pushBshThis(String fqClassName, String className, MethodVisitor cv) {
        // Push 'this'
        cv.visitVarInsn(ALOAD, 0);
        // Get the instance field
        cv.visitFieldInsn(GETFIELD, fqClassName, BSHTHIS + className, "Lbsh/This;");
    }

    /** Generate a branch of the constructor switch.
     * This method is called by generateConstructorSwitch. The code generated by this method assumes
     * that the argument array is on the stack.
     * @param index label index
     * @param targetClassName class name
     * @param paramTypes array of type descriptor strings
     * @param endLabel jump label
     * @param labels visit labels
     * @param consArgsVar constructor args
     * @param cv the code visitor to be used to generate the bytecode. */
    private void doSwitchBranch(int index, String targetClassName, String[] paramTypes, Label endLabel,
            Label[] labels, int consArgsVar, MethodVisitor cv) {
        cv.visitLabel(labels[index]);

        cv.visitVarInsn(ALOAD, 0); // push this before args

        // Unload the arguments from the ConstructorArgs object
        for (String type : paramTypes) {
            final String method;
            if (type.equals("Z"))
                method = "getBoolean";
            else if (type.equals("B"))
                method = "getByte";
            else if (type.equals("C"))
                method = "getChar";
            else if (type.equals("S"))
                method = "getShort";
            else if (type.equals("I"))
                method = "getInt";
            else if (type.equals("J"))
                method = "getLong";
            else if (type.equals("D"))
                method = "getDouble";
            else if (type.equals("F"))
                method = "getFloat";
            else
                method = "getObject";

            // invoke the iterator method on the ConstructorArgs
            cv.visitVarInsn(ALOAD, consArgsVar); // push the ConstructorArgs
            String className = "bsh/This$ConstructorArgs";
            String retType;
            if (method.equals("getObject"))
                retType = OBJECT;
            else
                retType = type;

            cv.visitMethodInsn(INVOKEVIRTUAL, className, method, "()" + retType, false);
            // if it's an object type we must do a check cast
            if (method.equals("getObject"))
                cv.visitTypeInsn(CHECKCAST, descriptorToClassName(type));
        }

        // invoke the constructor for this branch
        String descriptor = getMethodDescriptor("V", paramTypes);
        cv.visitMethodInsn(INVOKESPECIAL, targetClassName, "<init>", descriptor, false);
        cv.visitJumpInsn(GOTO, endLabel);
    }

    private static String getMethodDescriptor(String returnType, String[] paramTypes) {
        StringBuilder sb = new StringBuilder("(");
        for (String paramType : paramTypes)
            sb.append(paramType);

        sb.append(')').append(returnType);
        return sb.toString();
    }

    /**
     * Generate a superclass method delegate accessor method.
     * These methods are specially named methods which allow access to
     * overridden methods of the superclass (which the Java reflection API
     * normally does not allow).
     */
    // Maybe combine this with generateMethod()
    private void generateSuperDelegateMethod(Class<?> superClass, String superClassName, String methodName, String returnType, String[] paramTypes, int modifiers, ClassWriter cw) {
        String[] exceptions = null;

        if (returnType == null) // map loose return to Object
            returnType = OBJECT;

        String methodDescriptor = getMethodDescriptor(returnType, paramTypes);

        String paramTypesSig = getTypeParameterSignature(paramTypes);

        // Add method body
        MethodVisitor cv = cw.visitMethod(modifiers, "_bshSuper" + superClass.getSimpleName() + methodName, methodDescriptor, paramTypesSig, exceptions);

        cv.visitVarInsn(ALOAD, 0);
        // Push vars
        int localVarIndex = 1;
        for (String paramType : paramTypes) {
            if (isPrimitive(paramType))
                cv.visitVarInsn(ILOAD, localVarIndex);
            else
                cv.visitVarInsn(ALOAD, localVarIndex);
            localVarIndex += paramType.equals("D") || paramType.equals("J") ? 2 : 1;
        }

        cv.visitMethodInsn(INVOKESPECIAL, superClassName, methodName, methodDescriptor, false);

        generatePlainReturnCode(returnType, cv);

        // values here are ignored, computed automatically by ClassWriter
        cv.visitMaxs(0, 0);
    }

    /** Validate abstract method implementation.
     * Check that class is abstract or implements all abstract methods.
     * BSH classes are not abstract which allows us to instantiate abstract
     * classes. Also applies inheritance rules @see checkInheritanceRules().
     * @param type The class to check.
     * @throws RuntimException if validation fails. */
    static void checkAbstractMethodImplementation(Class<?> type) {
        final List<Method> meths = new ArrayList<>();
        class Reflector {
            void gatherMethods(Class<?> type) {
                if (null != type.getSuperclass())
                    gatherMethods(type.getSuperclass());
                meths.addAll(Arrays.asList(type.getDeclaredMethods()));
                for (Class<?> i : type.getInterfaces())
                    gatherMethods(i);
            }
        }
        new Reflector().gatherMethods(type);
        // for each filtered abstract method
        meths.stream().filter( m -> ( m.getModifiers() & ACC_ABSTRACT ) > 0 )
        .forEach( method -> {
            Method[] meth = meths.stream()
                    // find methods of the same name
                .filter( m -> method.getName().equals(m.getName() )
                    // not abstract nor private
                    && ( m.getModifiers() & (ACC_ABSTRACT|ACC_PRIVATE) ) == 0
                    // with matching parameters
                    && Types.areSignaturesEqual(
                            method.getParameterTypes(), m.getParameterTypes()))
                // sort most visible methods to the top
                // comparator: -1 if a is public or b not public or protected
                //              0 if access modifiers for a and b are equal
                .sorted( (a, b) -> ( a.getModifiers() & ACC_PUBLIC ) > 0
                      || ( b.getModifiers() & (ACC_PUBLIC|ACC_PROTECTED) ) == 0
                            ? -1 : ( a.getModifiers() & ACCESS_MODIFIERS ) ==
                                   ( b.getModifiers() & ACCESS_MODIFIERS )
                            ?  0 : 1 )
                .toArray(Method[]::new);
            // with no overriding methods class must be abstract
            if ( meth.length == 0 && !Reflect.getClassModifiers(type)
                    .hasModifier("abstract") )
                throw new RuntimeException(type.getSimpleName()
                    + " is not abstract and does not override abstract method "
                    + method.getName() + "() in "
                    + method.getDeclaringClass().getSimpleName());
            // apply inheritance rules to most visible method at index 0
            if ( meth.length > 0)
                checkInheritanceRules(method.getModifiers(),
                        meth[0].getModifiers(), method.getDeclaringClass());
        });
    }

    /** Apply inheritance rules. Overridden methods may not reduce visibility.
     * @param parentModifiers parent modifiers of method being overridden
     * @param overriddenModifiers overridden modifiers of new method
     * @param parentClass parent class name
     * @return true if visibility is not reduced
     * @throws RuntimeException if validation fails */
    static boolean checkInheritanceRules(int parentModifiers, int overriddenModifiers, Class<?> parentClass) {
        int prnt = parentModifiers & ( ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED );
        int chld = overriddenModifiers & ( ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED );

        if ( chld == prnt || prnt == ACC_PRIVATE || chld == ACC_PUBLIC || prnt == 0 && chld != ACC_PRIVATE )
            return true;

        throw new RuntimeException("Cannot reduce the visibility of the inherited method from "
                + parentClass.getName());
    }

    /** Check if method name and type descriptor signature is overridden.
     * @param clas super class
     * @param methodName name of method
     * @param paramTypes type descriptor of parameter types
     * @return matching method or null if not found */
    static Method classContainsMethod(Class<?> clas, String methodName, String[] paramTypes) {
        while ( clas != null ) {
            for ( Method method : clas.getDeclaredMethods() )
                if ( method.getName().equals(methodName)
                        && paramTypes.length == method.getParameterCount() ) {
                    String[] methodParamTypes = getTypeDescriptors(method.getParameterTypes());
                    boolean found = true;
                    for ( int j = 0; j < paramTypes.length; j++ )
                        if (false == (found = paramTypes[j].equals(methodParamTypes[j])))
                            break;
                    if (found) return method;
                }
            clas = clas.getSuperclass();
        }
        return null;
    }

    /** Generate return code for a normal bytecode
     * @param returnType expect type descriptor string
     * @param cv the code visitor to be used to generate the bytecode. */
    private static void generatePlainReturnCode(String returnType, MethodVisitor cv) {
        if (returnType.equals("V"))
            cv.visitInsn(RETURN);
        else if (isPrimitive(returnType)) {
            int opcode = IRETURN;
            if (returnType.equals("D"))
                opcode = DRETURN;
            else if (returnType.equals("F"))
                opcode = FRETURN;
            else if (returnType.equals("J")) //long
                opcode = LRETURN;

            cv.visitInsn(opcode);
        } else {
            cv.visitTypeInsn(CHECKCAST, descriptorToClassName(returnType));
            cv.visitInsn(ARETURN);
        }
    }

    /**  Generates the code to reify the arguments of the given method.
     * For a method "int m (int i, String s)", this code is the bytecode
     * corresponding to the "new Object[] { new bsh.Primitive(i), s }"
     * expression.
     * @author Eric Bruneton
     * @author Pat Niemeyer
     * @param cv the code visitor to be used to generate the bytecode.
     * @param isStatic the enclosing methods is static */
    private void generateParameterReifierCode(String[] paramTypes, boolean isStatic, final MethodVisitor cv) {
        cv.visitIntInsn(SIPUSH, paramTypes.length);
        cv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        int localVarIndex = isStatic ? 0 : 1;
        for (int i = 0; i < paramTypes.length; ++i) {
            String param = paramTypes[i];
            cv.visitInsn(DUP);
            cv.visitIntInsn(SIPUSH, i);
            if (isPrimitive(param)) {
                int opcode;
                if (param.equals("F"))
                    opcode = FLOAD;
                else if (param.equals("D"))
                    opcode = DLOAD;
                else if (param.equals("J"))
                    opcode = LLOAD;
                else
                    opcode = ILOAD;

                String type = "bsh/Primitive";
                cv.visitTypeInsn(NEW, type);
                cv.visitInsn(DUP);
                cv.visitVarInsn(opcode, localVarIndex);
                cv.visitMethodInsn(INVOKESPECIAL, type, "<init>", "(" + param + ")V", false);
                cv.visitInsn(AASTORE);
            } else {
                // If null wrap value as bsh.Primitive.NULL.
                cv.visitVarInsn(ALOAD, localVarIndex);
                Label isnull = new Label();
                cv.visitJumpInsn(IFNONNULL, isnull);
                cv.visitFieldInsn(GETSTATIC, "bsh/Primitive", "NULL", "Lbsh/Primitive;");
                cv.visitInsn(AASTORE);
                // else store parameter as Object.
                Label notnull = new Label();
                cv.visitJumpInsn(GOTO, notnull);
                cv.visitLabel(isnull);
                cv.visitVarInsn(ALOAD, localVarIndex);
                cv.visitInsn(AASTORE);
                cv.visitLabel(notnull);
            }
            localVarIndex += param.equals("D") || param.equals("J") ? 2 : 1;
        }
    }

    /** Generates the code to unreify the result of the given method.
     * For a method "int m (int i, String s)", this code is the bytecode
     * corresponding to the "((Integer)...).intValue()" expression.
     * @author Eric Bruneton
     * @author Pat Niemeyer
     * @param returnType expect type descriptor string
     * @param cv the code visitor to be used to generate the bytecode. */
    private void generateReturnCode(String returnType, MethodVisitor cv) {
        if (returnType.equals("V")) {
            cv.visitInsn(POP);
            cv.visitInsn(RETURN);
        } else if (isPrimitive(returnType)) {
            int opcode = IRETURN;
            String type;
            String meth;
            if (returnType.equals("Z")) {
                type = "java/lang/Boolean";
                meth = "booleanValue";
            } else if (returnType.equals("C")) {
                type = "java/lang/Character";
                meth = "charValue";
            } else if (returnType.equals("B")) {
                type = "java/lang/Byte";
                meth = "byteValue";
            } else if (returnType.equals("S") ) {
                type = "java/lang/Short";
                meth = "shortValue";
            } else if (returnType.equals("F")) {
                opcode = FRETURN;
                type = "java/lang/Float";
                meth = "floatValue";
            } else if (returnType.equals("J")) {
                opcode = LRETURN;
                type = "java/lang/Long";
                meth = "longValue";
            } else if (returnType.equals("D")) {
                opcode = DRETURN;
                type = "java/lang/Double";
                meth = "doubleValue";
            } else /*if (returnType.equals("I"))*/ {
                type = "java/lang/Integer";
                meth = "intValue";
            }

            String desc = returnType;
            cv.visitTypeInsn(CHECKCAST, type); // type is correct here
            cv.visitMethodInsn(INVOKEVIRTUAL, type, meth, "()" + desc, false);
            cv.visitInsn(opcode);
        } else {
            cv.visitTypeInsn(CHECKCAST, descriptorToClassName(returnType));
            cv.visitInsn(ARETURN);
        }
    }

    /**
     * Does the type descriptor string describe a primitive type?
     */
    private static boolean isPrimitive(String typeDescriptor) {
        return typeDescriptor.length() == 1; // right?
    }

    /** Returns type descriptors for the parameter types.
     * @param cparams class list of parameter types
     * @return String list of type descriptors */
    static String[] getTypeDescriptors(Class<?>[] cparams) {
        String[] sa = new String[cparams.length];
        for (int i = 0; i < sa.length; i++)
            sa[i] = BSHType.getTypeDescriptor(cparams[i]);
        return sa;
    }

    /** If a non-array object type, remove the prefix "L" and suffix ";".
     * @param s expect type descriptor string.
     * @return class name */
    private static String descriptorToClassName(String s) {
        if (s.startsWith("[") || !s.startsWith("L"))
            return s;
        return s.substring(1, s.length() - 1);
    }

    /**
     * Attempt to load a script named for the class: e.g. Foo.class Foo.bsh.
     * The script is expected to (at minimum) initialize the class body.
     * That is, it should contain the scripted class definition.
     *
     * This method relies on the fact that the ClassGenerator generateClass()
     * method will detect that the generated class already exists and
     * initialize it rather than recreating it.
     *
     * The only interact that this method has with the process is to initially
     * cache the correct class in the class manager for the interpreter to
     * insure that it is found and associated with the scripted body.
     */
    public static void startInterpreterForClass(Class<?> genClass) {
        String fqClassName = genClass.getName();
        String baseName = Name.suffix(fqClassName, 1);
        String resName = baseName + ".bsh";

        URL url = genClass.getResource(resName);
        if (null == url)
            throw new InterpreterError("Script (" + resName + ") for BeanShell generated class: " + genClass + " not found.");

        // Set up the interpreter
        try (Reader reader = new FileReader(genClass.getResourceAsStream(resName))) {
            Interpreter bsh = new Interpreter();
            NameSpace globalNS = bsh.getNameSpace();
            globalNS.setName("class_" + baseName + "_global");
            globalNS.getClassManager().associateClass(genClass);

            // Source the script
            bsh.eval(reader, globalNS, resName);
        } catch (TargetError e) {
            System.out.println("Script threw exception: " + e);
            if (e.inNativeCode())
                e.printStackTrace(System.err);
        } catch (IOException | EvalError e) {
            System.out.println("Evaluation Error: " + e);
        }
    }
}
