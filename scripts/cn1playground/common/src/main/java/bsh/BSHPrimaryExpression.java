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

class BSHPrimaryExpression extends SimpleNode
{
    private static final long serialVersionUID = 1L;
    private Object cached = null;
    boolean isArrayExpression = false;
    boolean isMapExpression = false;

    BSHPrimaryExpression(int id) { super(id); }

    /** Clear the eval cache.  */
    public void clearCache() {
        cached = null;
    }

    /** Called from BSHArrayInitializer during node creation informing us
     * that we are an array expression.
     * If parent BSHAssignment has an ASSIGN operation then this is a map
     * expression. If the initializer reference has multiple dimensions
     * it gets configure as being a map in array.
     * @param init reference to the calling array initializer */
    void setArrayExpression(BSHArrayInitializer init) {
        this.isArrayExpression = true;
        if ( parent instanceof BSHAssignment
                && ((BSHAssignment) parent).operator != null
                && (isMapExpression = (((BSHAssignment) parent).operator
                        == ParserConstants.ASSIGN))
                && init.jjtGetParent() instanceof BSHArrayInitializer )
            init.setMapInArray(true);
    }

    /**
        Evaluate to a value object.
    */
    public Object eval( CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        return eval( false, callstack, interpreter );
    }

    /**
        Evaluate to a value object.
    */
    public LHS toLHS( CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        // loosely typed map expression new {a=1, b=2} are treated
        // as non assignment (LHS) to retrieve Map.Entry key values
        // then wrapped in a MAP_ENTRY type LHS for value assignment.
        return (LHS) eval( interpreter.getStrictJava() || !isMapExpression,
                callstack, interpreter );
    }

    /*
        Our children are a prefix expression and any number of suffixes.
        <p>

        We don't eval() any nodes until the suffixes have had an
        opportunity to work through them.  This lets the suffixes decide
        how to interpret an ambiguous name (e.g. for the .class operation).
    */
    private Object eval( boolean toLHS,
        CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        // We can cache array expressions evaluated during type inference
        if ( isArrayExpression && null != cached )
            return cached;

        Object obj = jjtGetChild(0);

        for( int i=1; i < jjtGetNumChildren(); i++ )
            obj = ((BSHPrimarySuffix) jjtGetChild(i)).doSuffix(
                obj, toLHS, callstack, interpreter);

        /*
            If the result is a Node eval() it to an object or LHS
            (as determined by toLHS)
        */
        if ( obj instanceof Node )
            if ( obj instanceof BSHAmbiguousName )
                if ( toLHS )
                    obj = ((BSHAmbiguousName) obj).toLHS(
                        callstack, interpreter);
                else
                    obj = ((BSHAmbiguousName) obj).toObject(
                        callstack, interpreter);
            else
                // Some arbitrary kind of node
                if ( toLHS )
                    // is this right?
                    throw new EvalException("Can't assign to prefix.",
                        this, callstack );
                else
                    obj = ((Node) obj).eval(callstack, interpreter);

        if ( isMapExpression ) {
            if ( obj == Primitive.VOID )
                throw new EvalException(
                    "illegal use of undefined variable or 'void' literal",
                    this, callstack );
            // we have a valid map expression return an assignable Map.Entry
            obj = new LHS(obj);
        }

        if ( isArrayExpression )
            cached = obj;
        return obj;
    }
}

