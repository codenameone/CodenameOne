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
import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class ByteCodeField {
    private final String clsName;
    private boolean staticField;
    private String fieldName;

    private List<String> dependentClasses = new ArrayList<String>();
    
    private int arrayDimensions;
    private String type;
    private Class primitiveType;
    private boolean finalField;
    private Object value;
    
    public ByteCodeField(String clsName, int access, String name, String desc, String signature, Object value) {
        this.clsName = clsName;
        this.value = value;
        if(value != null && value instanceof String) {
            Parser.addToConstantPool((String)value);
        }
        staticField = (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
        finalField = (access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL;
        fieldName = name.replace('$', '_');

        arrayDimensions = 0;
        while(desc.startsWith("[")) {
            desc = desc.substring(1);
            arrayDimensions++;
        }
        char currentType = desc.charAt(0);
        switch(currentType) {
            case 'L':
                // Object skip until ;
                int idx = desc.indexOf(';');
                String objectType = desc.substring(1, idx);
                objectType = objectType.replace('/', '_').replace('$', '_');
                if(!dependentClasses.contains(objectType)) {
                    dependentClasses.add(objectType);
                }
                type = objectType;
                break;
            case 'I':
                primitiveType = Integer.TYPE;
                break;
            case 'J':
                primitiveType = Long.TYPE;
                break;
            case 'B':
                primitiveType = Byte.TYPE;
                break;
            case 'S':
                primitiveType = Short.TYPE;
                break;
            case 'F':
                primitiveType = Float.TYPE;
                break;
            case 'D':
                primitiveType = Double.TYPE;
                break;
            case 'Z':
                primitiveType = Boolean.TYPE;
                break;
            case 'C':
                primitiveType = Character.TYPE;
                break;
        }
    }

    public String getFieldName() {
        return fieldName;
    }
    
    public String getCDefinition() {
        if(type != null || arrayDimensions > 0) {
            return "JAVA_OBJECT";
        }
        return Util.getCType(primitiveType);
    }
    
    public List<String> getDependentClasses() {
        return dependentClasses;
    }

    /**
     * @return the staticField
     */
    public boolean isStaticField() {
        return staticField;
    }
    
    @Override
    public boolean equals(Object o) {
        return fieldName.equals(((ByteCodeField)o).fieldName);
    }
    
    @Override
    public int hashCode() {
        return fieldName.hashCode();
    }

    /**
     * @return the clsName
     */
    public String getClsName() {
        return clsName;
    }
    
    public boolean isObjectType() {
        return arrayDimensions > 0 || primitiveType == null;
    }
    
    public boolean isFinal() {
        return finalField;
    }
    
    public boolean shouldRemoveFromHeapCollection() {
        if(finalField && isObjectType()) {
            // 2d arrays can be modified in runtime resulting in broken arrays
            if(arrayDimensions < 2) {
                // this should probably be more elaborate to detect various mutable object types...
                return true;
            }
        }
        return false;
    }
    
    public Object getValue() {
        return value;
    }
    
    public String getType() {
        return type;
    }
}
