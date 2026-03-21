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

public class DelayedEvalBshMethod extends BshMethod
{
    private static final long serialVersionUID = 1L;
    String returnTypeDescriptor;
    BSHReturnType returnTypeNode;
    String [] paramTypeDescriptors;
    BSHFormalParameters paramTypesNode;

    // used for the delayed evaluation...
    transient CallStack callstack;
    transient Interpreter interpreter;
    private BSHArguments argsNode = null;
    private Invocable constructor = null;
    private Object[] constructorArgs = null;


    /**
        This constructor is used in class generation.  It supplies String type
        descriptors for return and parameter class types and allows delay of
        the evaluation of those types until they are requested.  It does this
        by holding BSHType nodes, as well as an evaluation callstack, and
        interpreter which are called when the class types are requested.
    */
    /*
        Note: technically I think we could get by passing in only the
        current namespace or perhaps BshClassManager here instead of
        CallStack and Interpreter.  However let's just play it safe in case
        of future changes - anywhere you eval a node you need these.
    */
    DelayedEvalBshMethod(
        String name,
        String returnTypeDescriptor, BSHReturnType returnTypeNode,
        String [] paramNames,
        String [] paramTypeDescriptors, BSHFormalParameters paramTypesNode,
        BSHBlock methodBody,
        NameSpace declaringNameSpace, Modifiers modifiers,
        boolean isVarArgs,
        CallStack callstack, Interpreter interpreter
    ) {
        super( name, null/*returnType*/, paramNames, null/*paramTypes*/,
               null/*paramModifiers*/, methodBody, declaringNameSpace, modifiers, isVarArgs );

        this.returnTypeDescriptor = returnTypeDescriptor;
        this.returnTypeNode = returnTypeNode;
        this.paramTypeDescriptors = paramTypeDescriptors;
        this.paramTypesNode = paramTypesNode;
        this.callstack = callstack;
        this.interpreter = interpreter;
    }

    /** Wrap super constructor as a BshMethod.
     * @param name constructor name
     * @param con the super constructor
     * @param declaringNameSpace the name space */
    DelayedEvalBshMethod(String name, Invocable con,
            NameSpace declaringNameSpace) {
        this(name, con.getReturnTypeDescriptor(), null,
            new String[con.getParameterCount()], con.getParamTypeDescriptors(),
             null, new BSHBlock(0), declaringNameSpace, null, con.isVarArgs(),
             null, null);

        this.constructor = con;
        this.modifiers = new Modifiers(Modifiers.CONSTRUCTOR);
        this.getModifiers().addModifier("public");
        this.getParameterModifiers();
        declaringNameSpace.setMethod(this);
        this.constructorArgs = This.CONTEXT_ARGS.get().remove(name);
    }

    public String getReturnTypeDescriptor() { return returnTypeDescriptor; }

    public Class<?> getReturnType()
    {
        if ( returnTypeNode == null )
            return null;

        // BSHType will cache the type for us
        try {
            return returnTypeNode.evalReturnType( callstack, interpreter );
        } catch ( EvalError e ) {
            throw new InterpreterError("can't eval return type: "+e, e);
        }
    }

    public String [] getParamTypeDescriptors() { return paramTypeDescriptors; }

    public Class<?>[] getParameterTypes()
    {
        if ( null != this.constructor )
            return this.constructor.getParameterTypes();
        // BSHFormalParameters will cache the type for us
        try {
            return (Class [])paramTypesNode.eval( callstack, interpreter );
        } catch ( EvalError e ) {
            throw new InterpreterError("can't eval param types: "+e, e);
        }
    }

    public String getAltConstructor() {
        if ( null != this.constructor )
            return "super";
        if ( this.methodBody.jjtGetNumChildren() == 0 )
            return null;
        Node firstStatement = this.methodBody.jjtGetChild(0);
        while ( !(firstStatement instanceof BSHMethodInvocation)
                && firstStatement.jjtGetNumChildren() > 0 )
            firstStatement = firstStatement.jjtGetChild(0);

        if ( firstStatement instanceof BSHMethodInvocation ) {
            BSHMethodInvocation methodNode = (BSHMethodInvocation) firstStatement;
            String methodName = methodNode.getNameNode().text;
            if ( methodName.equals("super") || methodName.equals("this") ) {
                this.argsNode = methodNode.getArgsNode();
                return methodName;
            }
        }
        return null;
    }

    public BSHArguments getArgsNode() {
        return this.argsNode;
    }

    public Object[] getConstructorArgs() {
        return constructorArgs;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;
        if (o.getClass() != this.getClass())
            return false;
        DelayedEvalBshMethod m = (DelayedEvalBshMethod)o;
        if( !getName().equals(m.getName())
                || getParameterCount() != m.getParameterCount() )
            return false;
        for (int i = 0; i < this.getParamTypeDescriptors().length; i++)
            if (!equal(this.getParamTypeDescriptors()[i],
                    m.getParamTypeDescriptors()[i]))
                return false;
        if (isVarArgs != m.isVarArgs)
           return false;
        return true;
    }

    @Override
    public int hashCode() {
        int h = getName().hashCode() + getClass().hashCode();
        for (final String cparamType : getParamTypeDescriptors())
            h += 3 + (cparamType == null ? 0 : cparamType.hashCode());
        return h + getParameterCount();
    }
}
