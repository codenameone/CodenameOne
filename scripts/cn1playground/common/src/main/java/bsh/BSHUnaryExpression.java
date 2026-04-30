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

class BSHUnaryExpression extends SimpleNode implements ParserConstants
{
    public int kind;
    public boolean postfix = false;

    BSHUnaryExpression(int id) { super(id); }

    public Object eval( CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        Node node = jjtGetChild(0);

        // If this is a unary increment of decrement (either pre or postfix)
        // then we need an LHS to which to assign the result.  Otherwise
        // just do the unary operation for the value.
        try {
            if ( kind == INCR || kind == DECR ) {
                LHS lhs = ((BSHPrimaryExpression)node).toLHS(
                    callstack, interpreter );
                return lhsUnaryOperation( lhs, interpreter.getStrictJava() );
            } else
                return
                    unaryOperation( node.eval(callstack, interpreter), kind );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( this, callstack );
        }
    }

    private Object lhsUnaryOperation( LHS lhs, boolean strictJava )
        throws UtilEvalError
    {
        Interpreter.debug("lhsUnaryOperation");
        Object prevalue, postvalue;
        prevalue = lhs.getValue();
        postvalue = unaryOperation(prevalue, kind);

        Object retVal;
        if ( postfix )
            retVal = prevalue;
        else
            retVal = postvalue;

        lhs.assign( postvalue, strictJava );
        return retVal;
    }

    private Object unaryOperation( Object op, int kind ) throws UtilEvalError
    {
        if ( op instanceof Boolean )
            op = (Boolean) op ? Primitive.TRUE : Primitive.FALSE;

        // Array-element access (a[0] on an int[]) and field reads on
        // generated types yield a boxed wrapper rather than a Primitive.
        // Wrap numeric/char wrappers so postfix/prefix `++`/`--` on
        // subscripted elements works.
        if ( op instanceof Integer )
            op = new Primitive(((Integer) op).intValue());
        else if ( op instanceof Long )
            op = new Primitive(((Long) op).longValue());
        else if ( op instanceof Short )
            op = new Primitive(((Short) op).shortValue());
        else if ( op instanceof Byte )
            op = new Primitive(((Byte) op).byteValue());
        else if ( op instanceof Float )
            op = new Primitive(((Float) op).floatValue());
        else if ( op instanceof Double )
            op = new Primitive(((Double) op).doubleValue());
        else if ( op instanceof Character )
            op = new Primitive(((Character) op).charValue());

        if ( !(op instanceof Primitive) )
            throw new UtilEvalError( "Unary operation " + tokenImage[kind]
                + " inappropriate for object" );

        return Operators.unaryOperation((Primitive) op, kind);
    }

    @Override
    public String toString() {
        return super.toString() + ": " + tokenImage[kind];
    }
}
