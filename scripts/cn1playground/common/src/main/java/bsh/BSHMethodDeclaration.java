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

class BSHMethodDeclaration extends SimpleNode
{
    public String name;

    // Begin Child node structure evaluated by insureNodesParsed

    BSHReturnType returnTypeNode;
    BSHFormalParameters paramsNode;
    BSHBlock blockNode;
    // index of the first throws clause child node
    int firstThrowsClause;

    // End Child node structure evaluated by insureNodesParsed

    public Modifiers modifiers = new Modifiers(Modifiers.METHOD);

    // Unsafe caching of type here.
    Class<?> returnType;  // null (none), Void.TYPE, or a Class
    int numThrows = 0;
    boolean isVarArgs;
    private boolean isScriptedObject;

    BSHMethodDeclaration(int id) { super(id); }

    /**
        Set the returnTypeNode, paramsNode, and blockNode based on child
        node structure.  No evaluation is done here.
    */
    synchronized void insureNodesParsed()
    {
        if ( paramsNode != null ) // there is always a paramsNode
            return;

        Object firstNode = jjtGetChild(0);
        firstThrowsClause = 1;
        if ( firstNode instanceof BSHReturnType )
        {
            returnTypeNode = (BSHReturnType)firstNode;
            paramsNode = (BSHFormalParameters)jjtGetChild(1);
            if ( jjtGetNumChildren() > 2+numThrows )
                blockNode = (BSHBlock)jjtGetChild(2+numThrows); // skip throws
            ++firstThrowsClause;
        }
        else
        {
            paramsNode = (BSHFormalParameters)jjtGetChild(0);
            blockNode = (BSHBlock)jjtGetChild(1+numThrows); // skip throws
        }

        if (null != blockNode && blockNode.jjtGetNumChildren() > 0) {
            Node crnt = blockNode.jjtGetChild(blockNode.jjtGetNumChildren() - 1);
            if (crnt instanceof BSHReturnStatement)
                while (crnt.hasNext())
                    if ((crnt = crnt.next()) instanceof BSHAmbiguousName)
                        isScriptedObject = ((BSHAmbiguousName)crnt).text.startsWith("this");
        }

        paramsNode.insureParsed();
        isVarArgs = paramsNode.isVarArgs;
    }

    /**
        Evaluate the return type node.
        @return the type or null indicating loosely typed return
    */
    Class<?> evalReturnType( CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        insureNodesParsed();
        if ( returnTypeNode != null )
            return returnTypeNode.evalReturnType( callstack, interpreter );
        else
            return null;
    }

    String getReturnTypeDescriptor(
        CallStack callstack, Interpreter interpreter, String defaultPackage )
    {
        insureNodesParsed();
        if ( returnTypeNode == null )
            return null;
        else
            return returnTypeNode.getTypeDescriptor(
                callstack, interpreter, defaultPackage );
    }

    BSHReturnType getReturnTypeNode() {
        insureNodesParsed();
        return returnTypeNode;
    }

    /**
        Evaluate the declaration of the method.  That is, determine the
        structure of the method and install it into the caller's namespace.
    */
    public Object eval( CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        returnType = evalReturnType( callstack, interpreter );
        evalNodes( callstack, interpreter );

        // Install an *instance* of this method in the namespace.
        // See notes in BshMethod

// This is not good...
// need a way to update eval without re-installing...
// so that we can re-eval params, etc. when classloader changes
// look into this
        NameSpace namespace = callstack.top();
        BshMethod bshMethod = new BshMethod( this, namespace, modifiers, isScriptedObject );
        if (!namespace.isMethod && !namespace.isClass)
            interpreter.getClassManager().addListener(bshMethod);
        else if (namespace.isMethod && !paramsNode.isListener()) {
            interpreter.getClassManager().addListener(paramsNode);
            paramsNode.setListener(true);
        }

        namespace.setMethod( bshMethod );

        return Primitive.VOID;
    }

    private void evalNodes( CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        insureNodesParsed();

        // validate that the throws names are class names
        for(int i=firstThrowsClause; i<numThrows+firstThrowsClause; i++)
            ((BSHAmbiguousName)jjtGetChild(i)).toClass(
                callstack, interpreter );

        paramsNode.eval( callstack, interpreter );

        // if strictJava mode, check for loose parameters and return type
        if ( interpreter.getStrictJava() )
        {
            for(int i=0; i<paramsNode.paramTypes.length; i++)
                if ( paramsNode.paramTypes[i] == null )
                    // Warning: Null callstack here.  Don't think we need
                    // a stack trace to indicate how we sourced the method.
                    throw new EvalException(
                "(Strict Java Mode) Undeclared argument type, parameter: " +
                    paramsNode.getParamNames()[i] + " in method: "
                    + name, this, null );

            if ( returnType == null )
                // Warning: Null callstack here.  Don't think we need
                // a stack trace to indicate how we sourced the method.
                throw new EvalException(
                "(Strict Java Mode) Undeclared return type for method: "
                    + name, this, null );
        }
    }

    public String toString() {
        return super.toString() + ": " + name;
    }
}
