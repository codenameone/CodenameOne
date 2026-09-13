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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Shai Almog
 */
public class ByteCodeMethodArg {
    private final int arrayDimensions;
    private String type;
    private PrimitiveType primitiveType;

    public ByteCodeMethodArg(String type, int dim) {
        this.type = type.replace('/', '_').replace('$', '_');
        arrayDimensions = dim;
    }

    public ByteCodeMethodArg(PrimitiveType type, int dim) {
        this.primitiveType = type;
        arrayDimensions = dim;
    }

    public char getQualifier() {
        if(type != null || arrayDimensions > 0) {
            return 'o';
        }
        if(primitiveType == PrimitiveType.LONG) {
            return 'l';
        }
        if(primitiveType == PrimitiveType.DOUBLE) {
            return 'd';
        }
        if(primitiveType == PrimitiveType.FLOAT) {
            return 'f';
        }
        return 'i';
    }
    
    public void appendCSig(StringBuilder bl) {
        if(arrayDimensions > 0) {
            bl.append("JAVA_OBJECT ");
        } else {
            if(type != null) {
                bl.append("JAVA_OBJECT ");
            } else {
                bl.append(Util.getCType(primitiveType));
                bl.append(" ");
            }
        }
    }

    public void appendCMethodExt(StringBuilder bl) {
        bl.append("_");
        if(type != null) {
            bl.append(type);
        } else {
            bl.append(Util.getSigType(primitiveType));
        }
        if(arrayDimensions > 0) {
            bl.append("_");
            bl.append(arrayDimensions);
            bl.append("ARRAY");
        }
    }

    @Override
    public int hashCode() {
        if(type != null) {
            return type.hashCode();
        }
        // ordinal(), not hashCode(): Enum.hashCode is an identity hash on OpenJDK
        // and the ordinal in ParparVM's java.lang.Enum, so hashing on it would make
        // a hash container of these args iterate in a different order under the
        // self-hosted translator than under the JVM-hosted one.
        return primitiveType.ordinal();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ByteCodeMethodArg other = (ByteCodeMethodArg) obj;
        if (this.arrayDimensions != other.arrayDimensions) {
            return false;
        }
        if (type == null) {
            if(other.type != null) {
                return false;
            }
            return primitiveType.equals(other.primitiveType);
        }
        if(other.type == null) {
            return false;
        }
        return type.equals(other.type);
    }
    
    public boolean isVoid() {
        return primitiveType == PrimitiveType.VOID;
    }
    
    public boolean isDoubleOrLong() {
        return (primitiveType == PrimitiveType.DOUBLE || primitiveType == PrimitiveType.LONG) && arrayDimensions == 0;
    }

    /**
     * @return the arrayDimensions
     */
    public int getArrayDimensions() {
        return arrayDimensions;
    }
    
    public String getTypeName() {
        return type;
    }
    
    public PrimitiveType getPrimitiveType() {
        return primitiveType;
    }
    
    public boolean isObject() {
        return arrayDimensions > 0 || primitiveType == null;
    }
}
