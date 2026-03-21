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
 EvalException indicates that the script has encountered a
 runtime exception and the current node cannot be successfully
 evaluated.

 EvalException may be thrown for a script syntax error, an evaluation
 error such as referring to an undefined variable.  Errors such as this
 mean that the current node cannot continue, but the interpreter is
 still ok.

 Exceptions where the interpreter has been corrupted or where execution
 cannot continue are handled by {@link EvalError}.

 @see EvalError
 @see TargetError
 */
public class EvalException extends EvalError
{
    public EvalException( String s, Node node, CallStack callstack, Throwable cause ) {
        super(s, node, callstack, cause);
    }

    public EvalException( String s, Node node, CallStack callstack ) {
        super(s, node, callstack);
    }

    /**
     Return the error to re-throw, prepending the specified message.
     Method does not throw itself as this messes with the tooling.
     */
    public EvalException reThrow( String msg ) {
        prependMessage( msg );
        return this;
    }
}
