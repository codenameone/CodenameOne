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

class BSHThrowStatement extends SimpleNode
{
    BSHThrowStatement(int id) { super(id); }

    public Object eval( CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        Object obj = jjtGetChild(0).eval(callstack, interpreter);

        // Scripted-class instances aren't Throwable at the Java level —
        // wrap them in a ScriptedThrowable so the JVM's exception pipe
        // can carry them through the try/catch machinery. Catch matching
        // unwraps when the declared type is the instance's ScriptedClass.
        if (obj instanceof ScriptedInstance) {
            throw new TargetError(
                    new ScriptedThrowable((ScriptedInstance) obj),
                    this, callstack);
        }

        if(!(obj instanceof Throwable))
            throw new EvalException("Expression in 'throw' must be Throwable type",
                this, callstack );

        // wrap the exception in a TargetException to propagate it up
        throw new TargetError( (Throwable) obj, this, callstack );
    }
}

