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

/**
    A formal parameter declaration.
    For loose variable declaration type is null.
*/
class BSHFormalParameter extends SimpleNode
{
    public static final Class UNTYPED = null;
    public String name;
    // unsafe caching of type here
    public Class type;
    boolean isFinal = false;
    boolean isVarArgs = false;
    int dimensions = 0;

    BSHFormalParameter(int id) { super(id); }

    public String getTypeDescriptor(
        CallStack callstack, Interpreter interpreter, String defaultPackage )
    {
        if ( jjtGetNumChildren() > 0 )
            return (isVarArgs ? "[" : "") + ((BSHType)jjtGetChild(0)).getTypeDescriptor(
                callstack, interpreter, defaultPackage );
        else
            // this will probably not get used
            return  (isVarArgs ? "[" : "") +"Ljava/lang/Object;";  // Object type
    }

    /**
        Evaluate the type.
    */
    public Object eval( CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        if ( jjtGetNumChildren() > 0 )
            type = ((BSHType)jjtGetChild(0)).getType( callstack, interpreter );
        else
            type = UNTYPED;

        if (isVarArgs)
            type = Array.newInstance(type, 0).getClass();

        return type;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + name + ", final=" + isFinal + ", varargs=" + isVarArgs;
    }
}

