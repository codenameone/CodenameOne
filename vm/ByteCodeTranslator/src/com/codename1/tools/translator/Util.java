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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Shai Almog
 */
public class Util {
    private static final Map<Class, String> ctypeMap = new HashMap<Class, String>();
    private static final Map<Class, String> sigTypeMap = new HashMap<Class, String>();
    static {
        ctypeMap.put(Integer.TYPE, "JAVA_INT");
        ctypeMap.put(Long.TYPE, "JAVA_LONG");
        ctypeMap.put(Short.TYPE, "JAVA_SHORT");
        ctypeMap.put(Byte.TYPE, "JAVA_BYTE");
        ctypeMap.put(Double.TYPE, "JAVA_DOUBLE");
        ctypeMap.put(Float.TYPE, "JAVA_FLOAT");
        ctypeMap.put(Boolean.TYPE, "JAVA_BOOLEAN");
        ctypeMap.put(Character.TYPE, "JAVA_CHAR");
        ctypeMap.put(Void.TYPE, "JAVA_VOID");
        sigTypeMap.put(Integer.TYPE, "int");
        sigTypeMap.put(Long.TYPE, "long");
        sigTypeMap.put(Short.TYPE, "short");
        sigTypeMap.put(Byte.TYPE, "byte");
        sigTypeMap.put(Double.TYPE, "double");
        sigTypeMap.put(Float.TYPE, "float");
        sigTypeMap.put(Boolean.TYPE, "boolean");
        sigTypeMap.put(Character.TYPE, "char");
        sigTypeMap.put(Void.TYPE, "void");
    }
    
    public static String getCType(Class cls) {
        return ctypeMap.get(cls);
    }

    public static String getSigType(Class cls) {
        return sigTypeMap.get(cls);
    }
    
    public static List<ByteCodeMethodArg> getMethodArgs(String methodDesc) {
        List<ByteCodeMethodArg> arguments = new ArrayList<ByteCodeMethodArg>();
        int currentArrayDim = 0;
        
        String desc = methodDesc;
        int pos = desc.lastIndexOf(')');
        desc = desc.substring(1, pos);
        for(int i = 0 ; i < desc.length() ; i++) {
            char currentType = desc.charAt(i);
            switch(currentType) {
                case '[':
                    // array of...
                    currentArrayDim++;
                    continue;
                case 'L':
                    // Object skip until ;
                    int idx = desc.indexOf(';', i);
                    String objectType = desc.substring(i + 1, idx);
                    objectType = objectType.replace('/', '_').replace('$', '_');
                    //if(!dependentClasses.contains(objectType)) {
                    //    dependentClasses.add(objectType);
                    //}
                    i = idx;
                    arguments.add(new ByteCodeMethodArg(objectType, currentArrayDim));
                    break;
                case 'I':
                    arguments.add(new ByteCodeMethodArg(Integer.TYPE, currentArrayDim));
                    break;
                case 'J':
                    arguments.add(new ByteCodeMethodArg(Long.TYPE, currentArrayDim));
                    break;
                case 'B':
                    arguments.add(new ByteCodeMethodArg(Byte.TYPE, currentArrayDim));
                    break;
                case 'S':
                    arguments.add(new ByteCodeMethodArg(Short.TYPE, currentArrayDim));
                    break;
                case 'F':
                    arguments.add(new ByteCodeMethodArg(Float.TYPE, currentArrayDim));
                    break;
                case 'D':
                    arguments.add(new ByteCodeMethodArg(Double.TYPE, currentArrayDim));
                    break;
                case 'Z':
                    arguments.add(new ByteCodeMethodArg(Boolean.TYPE, currentArrayDim));
                    break;
                case 'C':
                    arguments.add(new ByteCodeMethodArg(Character.TYPE, currentArrayDim));
                    break;
            }
            currentArrayDim = 0;
        }
        return arguments;
    }
}
