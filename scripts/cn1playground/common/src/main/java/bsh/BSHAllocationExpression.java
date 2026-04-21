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

import java.lang.reflect.Array;

/**
    New object, new array, or inner class style allocation with body.
*/
class BSHAllocationExpression extends SimpleNode
{
    BSHAllocationExpression(int id) { super(id); }
    private static int innerClassCount = 0;

    public Object eval( CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        // loose typed array initializer ex. new {1, 2, 3};
        if ( jjtGetNumChildren() == 1 && jjtGetChild(0)
                instanceof BSHArrayDimensions )
                return arrayAllocation( (BSHArrayDimensions) jjtGetChild(0),
                       Void.TYPE, callstack, interpreter );

        // type is either a class name or a primitive type
        Node type = jjtGetChild(0);

        // args is either constructor arguments or array dimensions
        Node args = jjtGetChild(1);

        if ( type instanceof BSHAmbiguousName )
        {
            BSHAmbiguousName name = (BSHAmbiguousName)type;

            if (args instanceof BSHArguments)
                return objectAllocation(name, (BSHArguments)args,
                    callstack, interpreter );
            else
                return objectArrayAllocation(name, (BSHArrayDimensions)args,
                    callstack, interpreter );
        }
        else
            return primitiveArrayAllocation((BSHPrimitiveType)type,
                (BSHArrayDimensions)args, callstack, interpreter );
    }

    private Object objectAllocation(
        BSHAmbiguousName nameNode, BSHArguments argumentsNode,
        CallStack callstack, Interpreter interpreter
    )
        throws EvalError
    {
        Object[] args = argumentsNode.getArguments( callstack, interpreter );
        if ( args == null)
            throw new EvalError( "Null args in new.", this, callstack );

        // Lookup class. Try a regular variable lookup first so that names
        // bound to a ScriptedClass (declared earlier in the same script)
        // resolve before we force a Java class lookup.
        Object scripted = lookupScriptedClass(nameNode.text, callstack);
        if (scripted != null) {
            ScriptedClass sc = (ScriptedClass) scripted;
            // Anonymous-class style: `new Iface() { method bodies }` —
            // synthesise a one-off subclass whose instance methods come from
            // the body, then construct it. Works for both interfaces and
            // regular classes.
            boolean hasBody = jjtGetNumChildren() > 2;
            if (hasBody) {
                BSHBlock body = (BSHBlock) jjtGetChild(2);
                ScriptedClass anonClass = ScriptedClass.build(
                        sc.getName() + "$anon", callstack.top(), body, sc,
                        callstack, interpreter);
                return anonClass.newInstance(args, callstack, interpreter);
            }
            if (sc.isInterface()) {
                throw new EvalError("Cannot instantiate scripted interface "
                        + sc.getName() + " directly; use a class that implements it.",
                        this, callstack);
            }
            // Non-static inner class — walk callstack for an enclosing
            // `this` whose ScriptedClass matches the required outer.
            ScriptedInstance enclosing = null;
            if (sc.getEnclosingClass() != null) {
                enclosing = findEnclosingInstance(sc.getEnclosingClass(), callstack);
                if (enclosing == null) {
                    throw new EvalError("Cannot construct non-static inner class "
                            + sc.getName() + " without an enclosing instance of "
                            + sc.getEnclosingClass().getName() + " in scope.",
                            this, callstack);
                }
            }
            return sc.newInstance(args, enclosing, callstack, interpreter);
        }
        Object obj = nameNode.toObject(
            callstack, interpreter, true /*force class*/ );


        Class<?> type = null;
        if ( obj instanceof ClassIdentifier )
            type = ((ClassIdentifier)obj).getTargetClass();
        else
            throw new EvalException(
                "Unknown class: "+nameNode.text, this, callstack );

        // Is an inner class style object allocation
        boolean hasBody = jjtGetNumChildren() > 2;

        // Validate if can construct a instance of this class
        try {
            Interpreter.mainSecurityGuard.canConstruct(type, args);
        } catch (SecurityError error) {
            throw error.toEvalError(this, callstack);
        }

        if ( hasBody )
        {
            BSHBlock body = (BSHBlock)jjtGetChild(2);
            if ( type.isInterface() )
                return constructWithInterfaceBody(
                    type, args, body, callstack, interpreter );
            else
                return constructWithClassBody(
                    type, args, body, callstack, interpreter );
        } else
            return constructObject( type, args, callstack, interpreter );
    }

    /** Walk the callstack looking for a `this` binding whose
     * ScriptedInstance is an instance of {@code outer} (or of any
     * nested non-static class whose enclosing chain reaches
     * {@code outer}). Used when constructing a non-static inner
     * class. */
    private static ScriptedInstance findEnclosingInstance(ScriptedClass outer, CallStack callstack) {
        for (int i = 0; i < callstack.depth(); i++) {
            NameSpace ns = callstack.get(i);
            while (ns != null) {
                try {
                    Object thisObj = ns.getVariable("this");
                    if (thisObj instanceof ScriptedInstance) {
                        ScriptedInstance si = (ScriptedInstance) thisObj;
                        ScriptedInstance walk = si;
                        while (walk != null) {
                            if (walk.getScriptedClass() == outer) return walk;
                            walk = walk.getEnclosingInstance();
                        }
                    }
                    if (thisObj instanceof This) {
                        Object inner = ((This) thisObj).namespace.getVariable("this");
                        if (inner instanceof ScriptedInstance) {
                            ScriptedInstance walk = (ScriptedInstance) inner;
                            while (walk != null) {
                                if (walk.getScriptedClass() == outer) return walk;
                                walk = walk.getEnclosingInstance();
                            }
                        }
                    }
                } catch (UtilEvalError ignore) {
                    // no `this` in this namespace — keep walking
                }
                ns = ns.getParent();
            }
        }
        return null;
    }

    private static Object lookupScriptedClass(String rawName, CallStack callstack) {
        if (rawName == null || rawName.length() == 0) return null;
        if (callstack == null) return null;
        // Strip a generic suffix like "Pair<String>" — type arguments are
        // erased at runtime in our scripted-class model.
        String simpleName = rawName;
        int lt = simpleName.indexOf('<');
        if (lt >= 0) simpleName = simpleName.substring(0, lt);
        // Dotted names like "Outer.Inner": resolve the head as a
        // ScriptedClass and walk through nested classes via the static
        // namespace of each enclosing class. We split manually because
        // String.split(regex) is not available on CN1's restricted String.
        java.util.List<String> partsList = new java.util.ArrayList<String>();
        int from = 0;
        for (int j = 0; j < simpleName.length(); j++) {
            if (simpleName.charAt(j) == '.') {
                partsList.add(simpleName.substring(from, j));
                from = j + 1;
            }
        }
        partsList.add(simpleName.substring(from));
        String[] parts = partsList.toArray(new String[partsList.size()]);
        if (parts.length == 0) return null;
        ScriptedClass current;
        try {
            Object v = callstack.top().getVariable(parts[0]);
            if (!(v instanceof ScriptedClass)) return null;
            current = (ScriptedClass) v;
        } catch (UtilEvalError ex) {
            return null;
        }
        for (int i = 1; i < parts.length; i++) {
            try {
                Object next = current.getStaticNameSpace().getVariable(parts[i]);
                if (!(next instanceof ScriptedClass)) return null;
                current = (ScriptedClass) next;
            } catch (UtilEvalError ex) {
                return null;
            }
        }
        return current;
    }

    Object constructFromEnclosingInstance(Object obj, CallStack callstack,
            Interpreter interpreter ) throws EvalError {
        // The outer instance `obj` must be a ScriptedInstance for the
        // `outer.new Inner(args)` form to resolve — Java inner classes
        // still aren't constructable without reflection.
        if (!(obj instanceof ScriptedInstance)) {
            throw new EvalError(
                    "Inner class allocation from a Java enclosing instance is not supported"
                            + " in the Codename One BeanShell runtime.",
                    this, callstack);
        }
        ScriptedInstance outerInstance = (ScriptedInstance) obj;
        ScriptedClass outerClass = outerInstance.getScriptedClass();

        Node typeNode = jjtGetChild(0);
        Node argsNode = jjtGetChild(1);
        if (!(typeNode instanceof BSHAmbiguousName) || !(argsNode instanceof BSHArguments)) {
            throw new EvalError(
                    "Unsupported inner-class allocation shape",
                    this, callstack);
        }
        String innerName = ((BSHAmbiguousName) typeNode).text;
        Object[] args = ((BSHArguments) argsNode).getArguments(callstack, interpreter);

        ScriptedClass inner;
        try {
            Object v = outerClass.getStaticNameSpace().getVariable(innerName);
            if (!(v instanceof ScriptedClass)) {
                throw new EvalError("No nested class '" + innerName + "' on "
                        + outerClass.getName(), this, callstack);
            }
            inner = (ScriptedClass) v;
        } catch (UtilEvalError ex) {
            throw ex.toEvalError(this, callstack);
        }
        if (inner.getEnclosingClass() == null) {
            throw new EvalError("Nested class '" + outerClass.getName() + "." + innerName
                    + "' is static — use new " + outerClass.getName() + "." + innerName
                    + "(...) instead.",
                    this, callstack);
        }
        // Ensure the supplied outer is an instance of the inner's
        // enclosing class (or a subclass thereof via the extends chain).
        ScriptedClass requiredOuter = inner.getEnclosingClass();
        ScriptedClass walkCls = outerClass;
        boolean ok = false;
        while (walkCls != null) {
            if (walkCls == requiredOuter) { ok = true; break; }
            walkCls = walkCls.getParent();
        }
        if (!ok) {
            throw new EvalError("Outer instance of type " + outerClass.getName()
                    + " cannot construct " + inner.getName()
                    + " — expected enclosing type " + requiredOuter.getName() + ".",
                    this, callstack);
        }
        return inner.newInstance(args, outerInstance, callstack, interpreter);
    }

    private Object constructObject(Class<?> type, Object[] args,
            CallStack callstack, Interpreter interpreter ) throws EvalError {
        final boolean isGeneratedClass = Reflect.isGeneratedClass(type);
        if (isGeneratedClass) {
            This.registerConstructorContext(callstack, interpreter);
        }
        Object obj;
        try {
            obj = Reflect.constructObject( type, args );
        } catch ( ReflectError e) {
            throw new EvalException(
                "Constructor error: " + e.getMessage(), this, callstack, e);
        } finally {
            if (isGeneratedClass)
                // clean up, prevent memory leak
                This.registerConstructorContext(null, null);
        }
        String className = type.getName();
        // Is it an inner class?
        if ( className.indexOf("$") == -1 )
            return obj;

        // work through to class 'this'
        This ths = callstack.top().getThis( null );
        NameSpace instanceNameSpace = ths.getNameSpace();

        // method and class name spaces acceptable
        if ( null != Name.getClassNameSpace(instanceNameSpace)
                && !Reflect.getClassModifiers(obj.getClass()).hasModifier("static") ) {
            Reflect.getThisNS(obj).setParent(instanceNameSpace);
        } else if ( Reflect.getClassModifiers(obj.getClass()).hasModifier("static") ) {
            // add class static parent as instance parent
            Reflect.getThisNS(obj).setParent(Reflect.getThisNS(obj.getClass()).getParent());
        }

        return obj;
    }

    private Object constructWithClassBody(
        Class<?> type, Object[] args, BSHBlock block,
        CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        throw new EvalError(
                "Anonymous class bodies are not supported in the Codename One BeanShell runtime.",
                this, callstack);
    }

    private Object constructWithInterfaceBody(
        Class<?> type, Object[] args, BSHBlock body,
        CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        throw new EvalError(
                "Anonymous interface implementations are not supported in the Codename One BeanShell runtime.",
                this, callstack);
    }

    private Object objectArrayAllocation(
        BSHAmbiguousName nameNode, BSHArrayDimensions dimensionsNode,
        CallStack callstack, Interpreter interpreter
    )
        throws EvalError
    {
        Class<?> type = nameNode.toClass( callstack, interpreter );

        return arrayAllocation( dimensionsNode, type, callstack, interpreter );
    }

    private Object primitiveArrayAllocation(
            BSHPrimitiveType typeNode, BSHArrayDimensions dimensionsNode,
            CallStack callstack, Interpreter interpreter)
            throws EvalError {
        Class<?> type = typeNode.getType();

        return arrayAllocation( dimensionsNode, type, callstack, interpreter );
    }

    private Object arrayAllocation(
            BSHArrayDimensions dimensionsNode, Class<?> type,
            CallStack callstack, Interpreter interpreter )
            throws EvalError {
        /*
            dimensionsNode can return either a fully initialized array or VOID.
            when VOID the prescribed array dimensions (defined and undefined)
            are contained in the node.
        */
        Object result = dimensionsNode.eval( type, callstack, interpreter );
        if ( result != Primitive.VOID )
            return result;
        else
            return arrayNewInstance( type, dimensionsNode, callstack, interpreter );
    }

    /**
        Create an array of the dimensions specified in dimensionsNode.
        dimensionsNode may contain a number of "undefined" as well as "defined"
        dimensions.
        <p>

        Background: in Java arrays are implemented in arrays-of-arrays style
        where, for example, a two dimensional array is a an array of arrays of
        some base type.  Each dimension-type has a Java class type associated
        with it... so if foo = new int[5][5] then the type of foo is
        int [][] and the type of foo[0] is int[], etc.  Arrays may also be
        specified with undefined trailing dimensions - meaning that the lower
        order arrays are not allocated as objects. e.g.
        if foo = new int [5][]; then foo[0] == null //true; and can later be
        assigned with the appropriate type, e.g. foo[0] = new int[5];
        (See Learning Java, O'Reilly & Associates more background).
        <p>

        To create an array with undefined trailing dimensions using the
        reflection API we must use an array type to represent the lower order
        (undefined) dimensions as the "base" type for the array creation...
        Java will then create the correct type by adding the dimensions of the
        base type to specified allocated dimensions yielding an array of
        dimensionality base + specified with the base dimensons unallocated.
        To create the "base" array type we simply create a prototype, zero
        length in each dimension, array and use it to get its class
        (Actually, I think there is a way we could do it with Class.forName()
        but I don't trust this).   The code is simpler than the explanation...
        see below.
    */
    private Object arrayNewInstance(
            Class<?> type, BSHArrayDimensions dimensionsNode,
            CallStack callstack, Interpreter interpreter) throws EvalError {
        if ( dimensionsNode.numUndefinedDims > 0 ) {
            type = arrayClass(type, dimensionsNode.numUndefinedDims);
        }

        try {
            Object arr = allocateArray(type, dimensionsNode.definedDimensions, 0);
            return arr;
        } catch( NegativeArraySizeException e1 ) {
            throw new TargetError( e1, this, callstack );
        } catch( Exception e ) {
            throw new EvalException("Can't construct primitive array: "
                    + e.getMessage(), this, callstack, e);
            }
    }

    private static Class<?> arrayClass(Class<?> baseType, int dimensions) {
        Class<?> type = baseType;
        for (int i = 0; i < dimensions; i++) {
            type = Array.newInstance(type, 0).getClass();
        }
        return type;
    }

    private static Object allocateArray(Class<?> baseType, int[] dimensions, int offset) {
        Class<?> componentType = arrayClass(baseType, dimensions.length - offset - 1);
        Object array = Array.newInstance(componentType, dimensions[offset]);
        if (offset == dimensions.length - 1) {
            return array;
        }
        Object[] objectArray = (Object[]) array;
        for (int i = 0; i < dimensions[offset]; i++) {
            objectArray[i] = allocateArray(baseType, dimensions, offset + 1);
        }
        return array;
    }
}
