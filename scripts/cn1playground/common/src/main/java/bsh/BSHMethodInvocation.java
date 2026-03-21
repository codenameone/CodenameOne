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

class BSHMethodInvocation extends SimpleNode
{
    BSHMethodInvocation (int id) { super(id); }

    BSHAmbiguousName getNameNode() {
        return (BSHAmbiguousName)jjtGetChild(0);
    }

    BSHArguments getArgsNode() {
        return (BSHArguments)jjtGetChild(1);
    }

    /**
        Evaluate the method invocation with the specified callstack and
        interpreter
    */
    public Object eval( CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        NameSpace namespace = callstack.top();
        BSHAmbiguousName nameNode = getNameNode();

      // get caller info for assert fail
      if ("fail".equals(nameNode.text))
          interpreter.getNameSpace().setNode(this);

        // Do not evaluate methods this() or super() in class instance space
        // (i.e. inside a constructor)
        if ( namespace.getParent() != null && namespace.getParent().isClass
            && ( nameNode.text.equals("super") || nameNode.text.equals("this") )
        )
            return Primitive.VOID;

        Name name = nameNode.getName(namespace);
        Object[] args = getArgsNode().getArguments(callstack, interpreter);

        try {
            return name.invokeMethod( interpreter, args, callstack, this);
        } catch (ReflectError e) {
            throw new EvalException(
                "Error in method invocation: " + e.getMessage(),
                    this, callstack, e);
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( this, callstack );
        }
    }
}
