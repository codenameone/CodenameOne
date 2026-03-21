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

import java.io.Serializable;
import java.util.Arrays;

/**
    This represents an instance of a bsh method declaration in a particular
    namespace.  This is a thin wrapper around the BSHMethodDeclaration
    with a pointer to the declaring namespace.
    <p>

    When a method is located in a subordinate namespace or invoked from an
    arbitrary namespace it must nonetheless execute with its 'super' as the
    context in which it was declared.
    <p/>
*/
/*
    Note: this method incorrectly caches the method structure.  It needs to
    be cleared when the classloader changes.
*/
public class BshMethod implements Serializable, Cloneable, BshClassManager.Listener {

    private static final long serialVersionUID = 1L;

    /*
        This is the namespace in which the method is set.
        It is a back-reference for the node, which needs to execute under this
        namespace.  It is not necessary to declare this transient, because
        we can only be saved as part of our namespace anyway... (currently).
    */
    NameSpace declaringNameSpace;

    // Begin Method components

    Modifiers modifiers;
    private String name;
    private Class<?> creturnType;

    // Arguments
    private String [] paramNames;
    private int paramCount;
    private Class<?> [] cparamTypes;
    private Modifiers [] paramModifiers;

    // Scripted method body
    protected BSHBlock methodBody;
    // Java Method, for a BshObject that delegates to a real Java method
    private Invocable javaMethod;
    private Object javaObject;
    protected boolean isVarArgs;
    boolean isScriptedObject = false;

    // End method components

    BshMethod(
        BSHMethodDeclaration method,
        NameSpace declaringNameSpace, Modifiers modifiers, boolean isScriptedObject )
    {
        this( method.name, method.returnType, method.paramsNode.getParamNames(),
            method.paramsNode.paramTypes, method.paramsNode.getParamModifiers(),
            method.blockNode, declaringNameSpace, modifiers, method.isVarArgs );
        this.isScriptedObject = isScriptedObject;
    }

    BshMethod(
        String name, Class<?> returnType, String [] paramNames,
        Class<?> [] paramTypes, Modifiers [] paramModifiers, BSHBlock methodBody,
        NameSpace declaringNameSpace, Modifiers modifiers, boolean isVarArgs
    ) {
        this.name = name;
        this.creturnType = returnType;
        this.paramNames = paramNames;
        this.paramModifiers = paramModifiers;
        if ( paramNames != null )
            this.paramCount = paramNames.length;
        this.cparamTypes = paramTypes;
        this.methodBody = methodBody;
        this.declaringNameSpace = declaringNameSpace;
        this.modifiers = modifiers;
        this.isVarArgs = isVarArgs;
    }

    /*
        Create a BshMethod that delegates to a real Java method upon invocation.
        This is used to represent imported object methods.
    */
    BshMethod( Invocable method, Object object )
    {
        this( method.getName(), method.getReturnType(), null/*paramNames*/,
            method.getParameterTypes(), null/*paramModifiers*/, null/*method.block*/,
            null/*declaringNameSpace*/, null/*modifiers*/, method.isVarArgs() );

        this.javaMethod = method;
        this.javaObject = object;
    }

    /** Cloneable clone method implementation, delegated to parent.
     * {@inheritDoc} */
    @Override
    public BshMethod clone() {
        BshMethod returnClone = null;
        try {
            returnClone = (BshMethod) super.clone();
        } catch (CloneNotSupportedException e) { /* is cloneable */ }
        return returnClone;
    }

    /**
        Get the argument types of this method.
        loosely typed (untyped) arguments will be represented by null argument
        types.
    */
    /*
        Note: bshmethod needs to re-evaluate arg types here
        This is broken.
    */
    public Class<?> [] getParameterTypes() {
        if (null == this.javaMethod) {
            reloadTypes();
            return cparamTypes;
        }
        return this.javaMethod.getParameterTypes();
    }

    public String [] getParameterNames() {
        if (null == paramNames) {
            int count = getParameterCount();
            paramNames = new String[count];
            for (int i = 0; i < count; i++) {
                paramNames[i] = String.valueOf((char) ('a' + i));
            }
        }
        return paramNames;
    }

    public Modifiers [] getParameterModifiers() {
        if (null == paramModifiers)
            paramModifiers = new Modifiers[getParameterCount()];
        return paramModifiers;
    }

    public int getParameterCount() {
        if (null == this.javaMethod)
            return paramCount;
        return this.javaMethod.getParameterCount();
    }
    /**
        Get the return type of the method.
        @return Returns null for a loosely typed return value,
            Void.TYPE for a void return type, or the Class of the type.
    */
    /*
        Note: bshmethod needs to re-evaluate the method return type here.
        This is broken.
    */
    public Class<?> getReturnType() {
        if (null == this.javaMethod) {
            reloadTypes();
            return creturnType;
        }
        return this.javaMethod.getReturnType();
    }

    public Modifiers getModifiers() {
        if (this.modifiers == null)
            this.modifiers = new Modifiers(Modifiers.METHOD);
        return this.modifiers;
    }

    public String getName() {
        if (null == this.javaMethod)
            return name;
        return this.javaMethod.getName();
    }

    public boolean isVarArgs() {
        if (null == this.javaMethod)
            return isVarArgs;
        return this.javaMethod.isVarArgs();
    }

    /**
        Invoke the declared method with the specified arguments and interpreter
        reference.  This is the simplest form of invoke() for BshMethod
        intended to be used in reflective style access to bsh scripts.
    */
    public Object invoke(
        Object[] argValues, Interpreter interpreter )
        throws EvalError
    {
        return invoke( argValues, interpreter, null, null, false );
    }

    /**
        Invoke the bsh method with the specified args, interpreter ref,
        and callstack.
        callerInfo is the node representing the method invocation
        It is used primarily for debugging in order to provide access to the
        text of the construct that invoked the method through the namespace.
        @param callerInfo is the BeanShell AST node representing the method
            invocation.  It is used to print the line number and text of
            errors in EvalError exceptions.  If the node is null here error
            messages may not be able to point to the precise location and text
            of the error.
        @param callstack is the callstack.  If callstack is null a new one
            will be created with the declaring namespace of the method on top
            of the stack (i.e. it will look for purposes of the method
            invocation like the method call occurred in the declaring
            (enclosing) namespace in which the method is defined).
    */
    public Object invoke(
        Object[] argValues, Interpreter interpreter, CallStack callstack,
            Node callerInfo )
        throws EvalError
    {
        return invoke( argValues, interpreter, callstack, callerInfo, false );
    }

    /**
        Invoke the bsh method with the specified args, interpreter ref,
        and callstack.
        callerInfo is the node representing the method invocation
        It is used primarily for debugging in order to provide access to the
        text of the construct that invoked the method through the namespace.
        @param callerInfo is the BeanShell AST node representing the method
            invocation.  It is used to print the line number and text of
            errors in EvalError exceptions.  If the node is null here error
            messages may not be able to point to the precise location and text
            of the error.
        @param callstack is the callstack.  If callstack is null a new one
            will be created with the declaring namespace of the method on top
            of the stack (i.e. it will look for purposes of the method
            invocation like the method call occurred in the declaring
            (enclosing) namespace in which the method is defined).
        @param overrideNameSpace
            When true the method is executed in the namespace on the top of the
            stack instead of creating its own local namespace.  This allows it
            to be used in constructors.
    */
    Object invoke(
        Object[] argValues, Interpreter interpreter, CallStack callstack,
            Node callerInfo, boolean overrideNameSpace )
        throws EvalError
    {
        Interpreter.debug("Bsh method invoke: ", this.name, " overrideNameSpace: ", overrideNameSpace);
        if ( argValues != null )
            for (int i=0; i<argValues.length; i++)
                if ( argValues[i] == null )
                    throw new Error("HERE!");

        if ( javaMethod != null )
        {
            try {
                // Validate if can invoke this method
                if (Reflect.isStatic(javaMethod))
                    Interpreter.mainSecurityGuard.canInvokeStaticMethod(javaMethod.getDeclaringClass(), javaMethod.getName(), argValues);
                else
                    Interpreter.mainSecurityGuard.canInvokeMethod(javaObject, javaMethod.getName(), argValues);

                return javaMethod.invoke(javaObject, argValues);
            } catch ( ReflectError e ) {
                throw new EvalError(
                    "Error invoking Java method: "+e, callerInfo, callstack );
            } catch ( RuntimeException e2 ) {
                throw new TargetError(
                    "Exception invoking imported object method.",
                    e2, callerInfo, callstack, true/*isNative*/ );
            } catch (UtilEvalError e3) {
                throw e3.toEvalError(callerInfo, callstack);
            }
        }

        // is this a syncrhonized method?
        if ( modifiers != null && modifiers.hasModifier("synchronized") )
        {
            // The lock is our declaring namespace's This reference
            // (the method's 'super').  Or in the case of a class it's the
            // class instance.
            Object lock;
            if ( declaringNameSpace.isClass )
            {
                try {
                    lock = declaringNameSpace.getClassInstance();
                } catch ( UtilEvalError e ) {
                    throw new InterpreterError(
                        "Can't get class instance for synchronized method.");
                }
            } else
                lock = declaringNameSpace.getThis(interpreter); // ???

            synchronized( lock )
            {
                return invokeImpl(
                    argValues, interpreter, callstack,
                    callerInfo, overrideNameSpace );
            }
        } else
            return invokeImpl( argValues, interpreter, callstack, callerInfo,
                overrideNameSpace );
    }

    private Object invokeImpl(
        Object[] argValues, Interpreter interpreter, CallStack callstack,
            Node callerInfo, boolean overrideNameSpace )
        throws EvalError
    {
        if (hasModifier("abstract"))
            throw new EvalError(
                    "Cannot invoke abstract method "
                    + name, callerInfo, callstack );

        Class<?> returnType = getReturnType();
        Class<?> [] paramTypes = getParameterTypes();

        if ( callstack == null )
            callstack = new CallStack( declaringNameSpace );

        if ( argValues == null )
            argValues = Reflect.ZERO_ARGS;

        // Cardinality (number of args) mismatch
        if ( !isVarArgs() && argValues.length != getParameterCount() ) {
        /*
            // look for help string
            try {
                // should check for null namespace here
                String help =
                    (String)declaringNameSpace.get(
                    "bsh.help."+name, interpreter );

                interpreter.println(help);
                return Primitive.VOID;
            } catch ( Exception e ) {
                throw eval error
            }
        */
            throw new EvalError(
                "Wrong number of arguments for local method: "
                + name, callerInfo, callstack );
        }

        // Make the local namespace for the method invocation
        NameSpace localNameSpace;
        if ( overrideNameSpace )
            localNameSpace = callstack.top();
        else {
            localNameSpace = new NameSpace( declaringNameSpace, name );
            localNameSpace.isMethod = true;
        }
        localNameSpace.setNode( callerInfo );

        /*
         * Check for VarArgs processing
         */
        int lastParamIndex = getParameterCount() - 1;
        Object varArgs = null;
        if (isVarArgs()) {
            Class<?> lastP = paramTypes[lastParamIndex];
            // Interpreter.debug("varArgs= "+name+" "+varArgs.getClass().getName());
            // Interpreter.debug("Varargs processing for "+name+" "+Arrays.toString(argValues));
            // Interpreter.debug(" parameter types "+Arrays.toString(paramTypes));
            // Interpreter.debug(" varArg comp type="+lastP.getComponentType());
            if ((getParameterCount() == argValues.length) &&
                (argValues[lastParamIndex] == null ||
                 (argValues[lastParamIndex].getClass().isArray() &&
                  lastP.getComponentType().isAssignableFrom(argValues[lastParamIndex].getClass().getComponentType())))) {
                /*
                 * This is the case that the final argument is
                 * a null or it contains an array of the component
                 * type of the vararg.  In either case the argument
                 * is passed as is without packing in to an array.
                 */
                varArgs = null;
            } else if (argValues.length >= (getParameterCount()-1)) {
                /*
                 * This is the case that the final varargs need
                 * to be packed in to an array.  Allow for 0 to many
                 * additional arguments.
                 */
                varArgs = new Object[argValues.length-lastParamIndex];
            }
       }

        // set the method parameters in the local namespace
        for (int i=0; i < argValues.length; i++) {

            int k = i >= lastParamIndex ? lastParamIndex : i;
            Class<?> paramType = varArgs != null && k == lastParamIndex
                    ? paramTypes[k].getComponentType()
                    : paramTypes[k];

            // Set typed variable
            if ( null != paramType ) {
                try {
                    argValues[i] = Types.castObject(
                            argValues[i], paramType, Types.ASSIGNMENT );
                }
                catch( UtilEvalError e) {
                    throw new EvalError(
                        "Invalid argument: "
                        + "`"+paramNames[k]+"'" + " for method: "
                        + name + " : " +
                        e.getMessage(), callerInfo, callstack );
                }
                try {
                    if (varArgs != null && i >= lastParamIndex)
                        ((Object[]) varArgs)[i-k] = Primitive.unwrap(argValues[i]);
                    else
                        localNameSpace.setTypedVariable( paramNames[k],
                            paramType, argValues[i], paramModifiers[k]);
                } catch ( UtilEvalError e2 ) {
                    throw e2.toEvalError( "Typed method parameter assignment",
                        callerInfo, callstack  );
                }
            } else {  // untyped param

                // getAssignable would catch this for typed param
                if ( argValues[i] == Primitive.VOID)
                    throw new EvalError(
                        "Undefined variable or class name, parameter: " +
                        paramNames[k] + " to method: "
                        + name, callerInfo, callstack );
                else try {
                    localNameSpace.setLocalVariable(
                        paramNames[k], argValues[i],
                        interpreter.getStrictJava() );
                } catch ( UtilEvalError e3 ) {
                    throw e3.toEvalError( "Typed method parameter assignment",
                            callerInfo, callstack );
                }
            }
        }

        if (varArgs != null) try {
            localNameSpace.setTypedVariable(
                    paramNames[lastParamIndex],
                    paramTypes[lastParamIndex],
                    varArgs,
                    paramModifiers[lastParamIndex]);
        } catch (UtilEvalError e1) {
            throw e1.toEvalError("Typed method parameter assignment",
                    callerInfo, callstack);
        }

        // Push the new namespace on the call stack
        if ( !overrideNameSpace )
            callstack.push( localNameSpace );

        // Invoke the block, overriding namespace with localNameSpace
        Object ret = null;
        CallStack returnStack = null;
        try {
            ret = methodBody.eval(callstack, interpreter, true/*override*/ );
            // save the callstack including the called method, just for error mess
            returnStack = callstack.copy();
        } finally {
            // Get back to caller namespace
            if ( !overrideNameSpace )
                callstack.pop();
        }

        ReturnControl retControl = null;
        if ( ret instanceof ReturnControl ) {
            retControl = (ReturnControl) ret;

            // Method body can only use 'return' statement type return control.
            if ( retControl.kind == ReturnControl.RETURN )
                ret = retControl.value;
            else
                // retControl.returnPoint is the Node of the return statement
                throw new EvalException("'continue' or 'break' in method body",
                    retControl.returnPoint, returnStack );

            // Check for explicit return of value from void method type.
            // retControl.returnPoint is the Node of the return statement
            if ( returnType == Void.TYPE && ret != Primitive.VOID )
                throw new EvalException( "Cannot return value from void method",
                retControl.returnPoint, returnStack);
        }

        if ( returnType != null ) {
            // If return type void, return void as the value.
            if ( returnType == Void.TYPE )
                return Primitive.VOID;

            // return type is a class
            try {
                ret = Types.castObject( ret, returnType, Types.ASSIGNMENT );
            } catch( UtilEvalError e ) {
                // Point to return statement point if we had one.
                // (else it was implicit return? What's the case here?)
                Node node = callerInfo;
                if ( retControl != null )
                    node = retControl.returnPoint;
                throw e.toEvalError(
                    "Incorrect type returned from method: "
                    + name + e.getMessage(), node, callstack );
            }
        }

        // when cloning a generated class deep copy This reference #421
        if ("clone".equals(getName())) {
            String className = ret.getClass().getSimpleName();
            This thiz = Reflect.getClassInstanceThis(ret, className);
            if (null != thiz) // not a generated class instance
                return thiz.cloneMethodImpl(callerInfo, callstack, ret);
        }

        return ret;
    }

    public boolean hasModifier( String name ) {
        if ( javaMethod != null )
            return Reflect.hasModifier(name, javaMethod.getModifiers());
        return modifiers != null && modifiers.hasModifier(name);
    }

    public String toString() {
        return "Method: "
            + StringUtil.methodString( this );
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;
        if (o.getClass() != this.getClass())
            return false;
        BshMethod m = (BshMethod)o;
        if( !name.equals(m.name) || getParameterCount() != m.getParameterCount() )
            return false;
        for( int i = 0; i < getParameterCount(); i++ )
            if( !equal(getParameterTypes()[i], m.getParameterTypes()[i]) )
                return false;
        return true;
    }

    protected static boolean equal(Object obj1,Object obj2) {
        return obj1==null ? obj2==null : obj1.equals(obj2);
    }

    @Override
    public int hashCode() {
        int h = name.hashCode() + getClass().hashCode();
        for (final Class<?> cparamType : getParameterTypes())
            h += 3 + (cparamType == null ? 0 : cparamType.hashCode());
        return h + getParameterCount();
    }

    private boolean reload = false;

    /** Reload types if reload is true */
    private void reloadTypes() {
        if (reload) try {
            reload = false;
            if (Reflect.isGeneratedClass(creturnType))
                creturnType = declaringNameSpace.getClass(creturnType.getName());
            for (int i = 0; i < cparamTypes.length; i++)
                if (Reflect.isGeneratedClass(cparamTypes[i]))
                    cparamTypes[i] = declaringNameSpace.getClass(cparamTypes[i].getName());
        } catch (UtilEvalError e) { /* should not happen on reload */ }
    }

    /** {@inheritDoc} */
    @Override
    public void classLoaderChanged() {
        reload = Reflect.isGeneratedClass(creturnType)
            || Arrays.asList(cparamTypes).stream()
                .anyMatch(Reflect::isGeneratedClass);
    }
}
