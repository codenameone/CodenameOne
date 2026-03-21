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
    The name of this class is somewhat misleading.  This covers both the case
    where there is an array initializer and
*/
class BSHArrayDimensions extends SimpleNode
{
    private static final long serialVersionUID = 1L;
    public Class<?> baseType;
    public int numDefinedDims;
    public int numUndefinedDims;
    /**
        The Length in each defined dimension.  This value set by the eval()
        Since the values can come from Expressions we should be re-eval()d each
        time.
    */
    public int [] definedDimensions;
    BSHArrayDimensions(int id) { super(id); }

    public void addDefinedDimension() { numDefinedDims++; }
    public void addUndefinedDimension() { numUndefinedDims++; }

    public Object eval(
            Class<?> type, CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        Interpreter.debug("array base type = ", type);
        baseType = type;

        return eval( callstack, interpreter );
    }

    /**
        Evaluate the structure of the array in one of two ways:

            a) an initializer exists, evaluate it and return
            the fully constructed array object, also record the dimensions
            of that array

            b) evaluate and record the lengths in each dimension and
            return void.

        The structure of the array dims is maintained in dimensions.
    */
    public Object eval( CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        Node child = jjtGetChild(0);

        /*
            Child is array initializer.  Evaluate it and fill in the
            dimensions it returns.  Initialized arrays are always fully defined
            (no undefined dimensions to worry about).
            The syntax uses the undefinedDimension count.
            e.g. int [][] { 1, 2 };
        */
        if (child instanceof BSHArrayInitializer)
        {
            Object initValue = ((BSHArrayInitializer) child).eval(
                baseType, numUndefinedDims, callstack, interpreter);

            // eval may return Map, MapEntry, Collection, or Bean types
            if ( !initValue.getClass().isArray() )
                return initValue;

            definedDimensions = BshArray.dimensions(initValue);

            // loose typed array inferred dimensions
            if ( -1 == numUndefinedDims )
                numUndefinedDims = definedDimensions.length;

            // Compare with number of dimensions actually created with the
            // number specified (syntax uses the undefined ones here)
            if ( definedDimensions.length != numUndefinedDims )
                throw new EvalException(
                "Incompatible initializer. Allocation calls for a " +
                numUndefinedDims+ " dimensional array, but initializer is a " +
                definedDimensions.length + " dimensional array", this, callstack );

            return initValue;
        }
        else
        // Evaluate the defined dimensions of the array
        {
            definedDimensions = new int[ numDefinedDims ];

            for(int i = 0; i < numDefinedDims; i++)
            {
                try {
                    Object length = jjtGetChild(i).eval(callstack, interpreter);
                    definedDimensions[i] = (int) Primitive.castWrapper(Integer.TYPE, length);
                }
                catch(Exception e)
                {
                    throw new EvalException(
                        "Array index: " + i +
                        " length does not evaluate to an integer", this, callstack, e );
                }
            }
        }

        return Primitive.VOID;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + numDefinedDims + ", " + numUndefinedDims;
    }
}
