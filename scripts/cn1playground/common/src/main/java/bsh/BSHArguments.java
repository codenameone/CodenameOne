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

class BSHArguments extends SimpleNode
{
    BSHArguments(int id) { super(id); }

    /**
        This node holds a set of arguments for a method invocation or
        constructor call.

        Note: arguments are not currently allowed to be VOID.
    */
    /*
        Disallowing VOIDs here was an easy way to support the throwing of a
        more descriptive error message on use of an undefined argument to a
        method call (very common).  If it ever turns out that we need to
        support that for some reason we'll have to re-evaluate how we get
        "meta-information" about the arguments in the various invoke() methods
        that take Object [].  We could either pass BSHArguments down to
        overloaded forms of the methods or throw an exception subtype
        including the argument position back up, where the error message would
        be compounded.
    */
    public Object[] getArguments( CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        // evaluate each child
        Object[] args = new Object[jjtGetNumChildren()];
        for(int i = 0; i < args.length; i++)
        {
            args[i] = jjtGetChild(i).eval(callstack, interpreter);
            if ( args[i] == Primitive.VOID )
                throw new EvalException( "Undefined argument: " +
                    jjtGetChild(i).getText(), this, callstack );
        }

        return args;
    }
}

