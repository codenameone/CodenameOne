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

import java.lang.reflect.Array;

class BSHType extends SimpleNode implements BshClassManager.Listener {
    private static final long serialVersionUID = 1L;
    /**
        baseType is used during evaluation of full type and retained for the
        case where we are an array type.
        In the case where we are not an array this will be the same as type.
    */
    private Class<?> baseType;
    /**
        If we are an array type this will be non zero and indicate the
        dimensionality of the array.  e.g. 2 for String[][];
    */
    private int arrayDims;

    /**
        Internal cache of the type.  Cleared on classloader change.
    */
    private Class<?> type;

    /** Flag to track if instance is already a listener */
    private boolean isListener = false;

    String descriptor;

    BSHType(int id) {
        super(id);
    }

    /**
        Used by the grammar to indicate dimensions of array types
        during parsing.
    */
    public void addArrayDimension() {
        arrayDims++;
    }

    Node getTypeNode() {
        return jjtGetChild(0);
    }

    /**
         Returns a class descriptor for this type.
         If the type is an ambiguous name (object type) evaluation is
         attempted through the namespace in order to resolve imports.
         If it is not found and the name is non-compound we assume the default
         package for the name.
    */
    public String getTypeDescriptor(
        CallStack callstack, Interpreter interpreter, String defaultPackage )
    {
        // return cached type if available
        if ( descriptor != null )
            return descriptor;

        String descriptor;
        //  first node will either be PrimitiveType or AmbiguousName
        Node node = getTypeNode();
        if ( node instanceof BSHPrimitiveType )
            descriptor = getTypeDescriptor( ((BSHPrimitiveType)node).type );
        else
        {
            String clasName = ((BSHAmbiguousName)node).text;
            String innerClass = callstack.top().importedClasses.get(clasName);

            Class<?> clas = null;
            if ( innerClass == null ) try {
                clas = ((BSHAmbiguousName)node).toClass(
                    callstack, interpreter );
            } catch ( EvalError e ) {
                // Lets assume we have a generics raw type
                if (clasName.length() == 1)
                    clasName = "java.lang.Object";
            } else
                clasName = innerClass.replace('.', '$');

            if ( clas != null ) {
                descriptor = getTypeDescriptor( clas );
            } else {
                if ( defaultPackage == null || Name.isCompound( clasName ) )
                    descriptor = "L" + clasName.replace('.','/') + ";";
                else
                    descriptor =
                        "L"+defaultPackage.replace('.','/')+"/"+clasName + ";";
            }
        }

        for(int i=0; i<arrayDims; i++)
            descriptor = "["+descriptor;

        this.descriptor = descriptor;
        return descriptor;
    }

    private static boolean resolvesToScriptedClass(Node node, CallStack callstack) {
        if (callstack == null) return false;
        try {
            String text = node.getText();
            if (text == null) return false;
            text = text.trim();
            int lt = text.indexOf('<');
            if (lt >= 0) text = text.substring(0, lt).trim();
            if (text.length() == 0 || text.indexOf('.') >= 0) return false;
            NameSpace top = callstack.top();
            if (top == null) return false;
            Object v = top.getVariable(text);
            return v instanceof ScriptedClass;
        } catch (UtilEvalError ex) {
            return false;
        }
    }

    public Class<?> getType( CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        // return cached type if available
        if ( type != null )
            return type;

        //  first node will either be PrimitiveType or AmbiguousName
        Node node = getTypeNode();
        if ( node instanceof BSHPrimitiveType )
            baseType = ((BSHPrimitiveType)node).getType();
        else if (resolvesToScriptedClass(node, callstack)) {
            // A locally-declared scripted class shadows any imported Java
            // class of the same simple name (e.g. a user-declared
            // `record Point(...)` should win over com.codename1.ui.geom.Point).
            // We use Object.class so the assignment is loose; runtime checks
            // happen at method-call sites.
            baseType = Object.class;
        }
        else
            try {
            baseType = ((BSHAmbiguousName)node).toClass(
                callstack, interpreter );
            } catch (EvalError e) {
                // Assuming generics raw type
                if (node.getText().trim().length() == 1
                        && e.getCause() instanceof ClassNotFoundException)
                    baseType = Object.class;
                else
                    throw e; // roll up unhandled error
            }

        if ( arrayDims > 0 ) {
            try {
                // Build the array class incrementally to avoid relying on
                // Array.newInstance(Class, int[]), which is not available in
                // the JavaScript runtime subset.
                type = arrayClass(null == baseType ? Object.class : baseType, arrayDims);
            } catch(Exception e) {
                throw new EvalException("Couldn't construct array type",
                    this, callstack, e);
            }
        } else
            type = baseType;

        // add listener to reload type if class is reloaded see #699
        if (!isListener) { // only add once
            interpreter.getClassManager().addListener(this);
            isListener = true;
        }

        return type;
    }

    private static Class<?> arrayClass(Class<?> baseType, int dimensions) {
        Class<?> type = baseType;
        for (int i = 0; i < dimensions; i++) {
            type = Array.newInstance(type, 0).getClass();
        }
        return type;
    }

    /**
        baseType is used during evaluation of full type and retained for the
        case where we are an array type.
        In the case where we are not an array this will be the same as type.
    */
    public Class<?> getBaseType() {
        return baseType;
    }
    /**
        If we are an array type this will be non zero and indicate the
        dimensionality of the array.  e.g. 2 for String[][];
    */
    public int getArrayDims() {
        return arrayDims;
    }

    /** Clear instance cache to reload types on class loader change #699 */
    public void classLoaderChanged() {
        type = null;
        baseType = null;
    }

    public static String getTypeDescriptor( Class<?> clas )
    {
        if ( clas == Boolean.class ) return "Z";
        if ( clas == Character.class ) return "C";
        if ( clas == byte.class ) return "B";
        if ( clas == Short.class ) return "S";
        if ( clas == int.class ) return "I";
        if ( clas == long.class ) return "J";
        if ( clas == Float.class ) return "F";
        if ( clas == double.class ) return "D";
        if ( clas == Void.TYPE ) return "V";

        String name = clas.getName().replace('.','/');

        if ( name.startsWith("[") || name.endsWith(";") )
            return name;
        else
            return "L"+ name.replace('.','/') +";";
    }
}
