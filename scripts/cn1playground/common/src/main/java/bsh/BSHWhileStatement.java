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
 * This class handles both {@code while} statements and {@code do..while} statements.
 */
class BSHWhileStatement extends SimpleNode implements ParserConstants {

    /**
     * Set by Parser, default {@code false}
     */
    boolean isDoStatement;
    String label;


    BSHWhileStatement(int id) {
        super(id);
    }


    public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
        int numChild = jjtGetNumChildren();
        // Order of body and condition is swapped for do / while
        final Node condExp;
        final Node body;
        if (isDoStatement) {
            condExp = jjtGetChild(1);
            body = jjtGetChild(0);
        } else {
            condExp = jjtGetChild(0);
            body = numChild > 1 ? jjtGetChild(1) : null;
        }
        boolean doOnceFlag = isDoStatement;
        while (doOnceFlag || BSHIfStatement.evaluateCondition(condExp, callstack, interpreter)) {
            doOnceFlag = false;
            if (body == null) continue; // no body
            Object ret = body instanceof BSHBlock
                ? ((BSHBlock)body).eval(callstack, interpreter, null)
                : body.eval(callstack, interpreter);
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
        return Primitive.VOID;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + label + ": do=" + isDoStatement;
    }
}
