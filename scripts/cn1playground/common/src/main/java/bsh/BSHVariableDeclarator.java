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

/**
    name [ = initializer ]
    evaluate name and return optional initializer
*/
class BSHVariableDeclarator extends SimpleNode
{
    // The token.image text of the name... never changes.
    public String name;
    public int dimensions = 0;

    BSHVariableDeclarator(int id) { super(id); }

    /**
        Evaluate the optional initializer value.
        (The name was set at parse time.)

        A variable declarator can be evaluated with or without preceding
        type information. Currently the type info is only used by array
        initializers in the case where there is no explicitly declared type.

        @param typeNode is the BSHType node.  Its info is passed through to any
        variable intializer children for the case where the array initializer
        does not declare the type explicitly. e.g.
            int [] a = { 1, 2 };
        typeNode may be null to indicate no type information available.
    */
    public Object eval(
        BSHType typeNode, Modifiers modifiers, CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        // null value means no value
        Object value = modifiers.hasModifier("final")
                ? null
                : Primitive.isWrapperType(typeNode.getBaseType())
                    ? null
                    : Primitive.getDefaultValue(typeNode.getBaseType());

        if ( jjtGetNumChildren() > 0 )
        {
            Node initializer = jjtGetChild(0);

            /*
                If we have type info and the child is an array initializer
                pass it along...  Else use the default eval style.
                (This allows array initializer to handle the problem...
                allowing for future enhancements in loosening types there).
            */
            if ( initializer instanceof BSHArrayInitializer )
                value = ((BSHArrayInitializer)initializer).eval(typeNode.getBaseType(),
                    this.getArrayDims(typeNode), callstack, interpreter);
            else
                value = initializer.eval( callstack, interpreter);
        }

        if ( value == Primitive.VOID )
            throw new EvalException("Void initializer.", this, callstack );

        return value;
    }

    private int getArrayDims(BSHType typeNode) {
        if ( dimensions > 0 )
            return dimensions;
        if ( typeNode.getArrayDims() > 0 )
            return typeNode.getArrayDims();
        return -1;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + name;
    }
}
