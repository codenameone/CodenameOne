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
    Implementation of the for(;;) statement.
*/
class BSHForStatement extends SimpleNode implements ParserConstants
{
    final int blockId;
    public boolean hasForInit;
    public boolean hasExpression;
    public boolean hasForUpdate;

    String label;

    BSHForStatement(int id) {
        super(id);
        blockId = ++BlockNameSpace.blockCount;
    }

    public Object eval(CallStack callstack , Interpreter interpreter) throws EvalError {
        int i = 0;
        final Node forInit = hasForInit ? jjtGetChild(i++) : null;
        final Node expression = hasExpression ? jjtGetChild(i++) : null;
        final Node forUpdate = hasForUpdate ? jjtGetChild(i++) : null;
        final Node statement = i < jjtGetNumChildren() ? jjtGetChild(i) : null;
        final NameSpace enclosingNameSpace= callstack.top();
        final NameSpace forNameSpace = new BlockNameSpace(enclosingNameSpace, blockId);

        /*
            Note: some interesting things are going on here.

            1) We swap instead of push...  The primary mode of operation
            acts like we are in the enclosing namespace...  (super must be
            preserved, etc.)

            2) We do *not* call the body block eval with the namespace
            override.  Instead we allow it to create a second subordinate
            BlockNameSpace child of the forNameSpace.  Variable propagation
            still works through the chain, but the block's child cleans the
            state between iteration.
            (which is correct Java behavior... see forscope4.bsh)
        */

        // put forNameSpace on the top of the stack
        callstack.swap( forNameSpace );
        try {
            if ( hasForInit ) forInit.eval( callstack, interpreter );
            while (true) {
                if (hasExpression && !BSHIfStatement.evaluateCondition(
                        expression, callstack, interpreter))
                    break;

                if ( statement != null ) { // not empty statement
                    Object ret = statement instanceof BSHBlock
                        ? ((BSHBlock)statement).eval( callstack, interpreter, null)
                        : statement.eval( callstack, interpreter );

                    if (ret instanceof ReturnControl) {
                        ReturnControl control = (ReturnControl)ret;

                        if (null != control.label)
                            if (null == label || !label.equals(control.label))
                                return ret;

                        if (control.kind == RETURN)
                            return ret;
                        else if (control.kind == BREAK)
                            break;
                        // if CONTINUE we just carry on
                    }
                }
                if ( hasForUpdate ) forUpdate.eval( callstack, interpreter );
            }
            return Primitive.VOID;
        } finally {
            callstack.swap( enclosingNameSpace );  // put it back
        }
    }

    @Override
    public String toString() {
        return super.toString() + ": " + label + ": " + hasForInit + " ; " + hasExpression + " ; " + hasForUpdate;
    }
}
