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

package com.codename1.tools.translator.bytecodes;

import java.util.List;

/**
 * A custom instruction allows us to override existing bytecode with an optimization
 * 
 * @author Shai Almog
 */
public class CustomIntruction extends Instruction implements AssignableExpression {
    private String code;
    private String complexCode;
    private List<String> dependencies;
    private AssignableExpression assignableExpression;
    
    public CustomIntruction(String code, String complexCode, List<String> dependencies, AssignableExpression assignable) {
        super(-1);
        this.code = code;
        this.complexCode = complexCode;
        this.dependencies = dependencies;
        this.assignableExpression = assignable;
    }
    
    public CustomIntruction(String code, String complexCode, List<String> dependencies) {
        this(code, complexCode, dependencies, null);
    }
    
    
    public void appendInstruction(StringBuilder b, List<Instruction> l) {
        if(hasInstructions) {
            b.append(complexCode);
        } else {
            b.append(code);
        }
    }

    public void addDependencies(List<String> dependencyList) {
        if(dependencies != null) {
            dependencyList.addAll(dependencies);
        }
    }
    
    public void setAssignableExpression(AssignableExpression ex) {
        assignableExpression = ex;
    }
    
    public AssignableExpression getAssignableExpression() {
        return assignableExpression;
    }

    @Override
    public boolean assignTo(String varName, String typeVarName, StringBuilder sb) {
        if (assignableExpression != null) {
            return assignableExpression.assignTo(varName, typeVarName, sb);
        }
        return false;
    }
    
    
}
