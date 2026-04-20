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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
    What's in a name?  I'll tell you...
    Name() is a somewhat ambiguous thing in the grammar and so is this.
    <p>

    This class is a name resolver.  It holds a possibly ambiguous dot
    separated name and reference to a namespace in which it allegedly lives.
    It provides methods that attempt to resolve the name to various types of
    entities: e.g. an Object, a Class, a declared scripted BeanShell method.
    <p>

    Name objects are created by the factory method NameSpace getNameResolver(),
    which caches them subject to a class namespace change.  This means that
    we can cache information about various types of resolution here.
    Currently very little if any information is cached.  However with a future
    "optimize" setting that defeats certain dynamic behavior we might be able
    to cache quite a bit.
*/
/*
    <strong>Implementation notes</strong>
    <pre>
    Thread safety: all of the work methods in this class must be synchronized
    because they share the internal intermediate evaluation state.

    Note about invokeMethod():  We could simply use resolveMethod and return
    the MethodInvoker (BshMethod or JavaMethod) however there is no easy way
    for the AST (BSHMehodInvocation) to use this as it doesn't have type
    information about the target to resolve overloaded methods.
    (In Java, overloaded methods are resolved at compile time... here they
    are, of necessity, dynamic).  So it would have to do what we do here
    and cache by signature.  We now do that for the client in Reflect.java.

    Note on this.caller resolution:
    Although references like these do work:

        this.caller.caller.caller...   // works

    the equivalent using successive calls:

        // does *not* work
        for( caller=this.caller; caller != null; caller = caller.caller );

    is prohibited by the restriction that you can only call .caller on a
    literal this or caller reference.  The effect is that magic caller
    reference only works through the current 'this' reference.
    The real explanation is that This referernces do not really know anything
    about their depth on the call stack.  It might even be hard to define
    such a thing...

    For those purposes we provide :

        this.callstack

    </pre>
*/
class Name implements java.io.Serializable
{
    // These do not change during evaluation
    public NameSpace namespace;
    String value = null;

    // ---------------------------------------------------------
    // The following instance variables mutate during evaluation and should
    // be reset by the reset() method where necessary

    // For evaluation
    /** Remaining text to evaluate */
    private String evalName;
    /**
        The last part of the name evaluated.  This is really only used for
        this, caller, and super resolution.
    */
    private String lastEvalName;
    private static String FINISHED = null; // null evalname and we're finished
    private Object evalBaseObject;  // base object for current eval

    private int callstackDepth;     // number of times eval hit 'this.caller'

    //
    //  End mutable instance variables.
    // ---------------------------------------------------------

    // Begin Cached result structures
    // These are optimizations

    // Note: it's ok to cache class resolution here because when the class
    // space changes the namespace will discard cached names.

    /**
        The result is a class
    */
    Class<?> asClass;

    /**
        The result is a static method call on the following class
    */
    Class<?> classOfStaticMethod;

    // End Cached result structures

    private void reset() {
        evalName = value;
        evalBaseObject = null;
        callstackDepth = 0;
    }

    /**
        This constructor should *not* be used in general.
        Use NameSpace getNameResolver() which supports caching.
        @see NameSpace getNameResolver().
    */
    // I wish I could make this "friendly" to only NameSpace
    Name( NameSpace namespace, String s )
    {
        this.namespace = namespace;
        value = s;
    }

    /**
        Resolve possibly complex name to an object value.

        Throws EvalError on various failures.
        A null object value is indicated by a Primitive.NULL.
        A return type of Primitive.VOID comes from attempting to access
        an undefined variable.

        Some cases:
            myVariable
            myVariable.foo
            myVariable.foo.bar
            java.awt.GridBagConstraints.BOTH
            my.package.stuff.MyClass.someField.someField...

        Interpreter reference is necessary to allow resolution of
        "this.interpreter" magic field.
        CallStack reference is necessary to allow resolution of
        "this.caller" magic field.
        "this.callstack" magic field.
    */
    public Object toObject( CallStack callstack, Interpreter interpreter )
        throws UtilEvalError
    {
        return toObject( callstack, interpreter, false );
    }

    /**
        @see toObject()
        @param forceClass if true then resolution will only produce a class.
        This is necessary to disambiguate in cases where the grammar knows
        that we want a class; where in general the var path may be taken.
    */
    synchronized public Object toObject(
        CallStack callstack, Interpreter interpreter, boolean forceClass )
        throws UtilEvalError
    {
        reset();

        Object obj = null;
        while( evalName != null )
            obj = consumeNextObjectField(
                callstack, interpreter, forceClass, false/*autoalloc*/  );

        if ( obj == null )
            throw new InterpreterError("null value in toObject()");

        return obj;
    }

    private Object completeRound(
        String lastEvalName, String nextEvalName, Object returnObject )
    {
        if ( returnObject == null )
            throw new InterpreterError("lastEvalName = "+lastEvalName);
        this.lastEvalName = lastEvalName;
        this.evalName = nextEvalName;
        this.evalBaseObject = returnObject;
        return returnObject;
    }

    /**
        Get the next object by consuming one or more components of evalName.
        Often this consumes just one component, but if the name is a classname
        it will consume all of the components necessary to make the class
        identifier.
    */
    private Object consumeNextObjectField(
        CallStack callstack, Interpreter interpreter,
        boolean forceClass, boolean autoAllocateThis )
        throws UtilEvalError
    {
        /*
            Is it a simple variable name?
            Doing this first gives the correct Java precedence for vars
            vs. imported class names (at least in the simple case - see
            tests/precedence1.bsh).  It should also speed things up a bit.
        */
        if ( evalBaseObject == null && !isCompound(evalName) && !forceClass ) {
            Object obj = resolveThisFieldReference(
                callstack, namespace, interpreter, evalName, false );

            if ( obj != Primitive.VOID )
                return completeRound( evalName, FINISHED, obj );
        }

        /*
            Is it a bsh script variable reference?
            If we're just starting the eval of name (no base object)
            or we're evaluating relative to a This type reference check.
        */
        String varName = prefix(evalName, 1);
        if ( ( evalBaseObject == null || evalBaseObject instanceof This  )
            && !forceClass )
        {
            Object obj;
            // switch namespace and special var visibility
            if ( evalBaseObject == null ) {
                obj = resolveThisFieldReference(
                    callstack, namespace, interpreter, varName, false );
            } else {
                obj = resolveThisFieldReference(
                    callstack, ((This)evalBaseObject).namespace,
                    interpreter, varName, true );
            }

            if ( obj != Primitive.VOID )
            {
                // Resolved the variable
                return completeRound( varName, suffix(evalName), obj );
            }
        }

        /*
            Is it a class name?
            If we're just starting eval of name try to make it, else fail.
        */
        if ( evalBaseObject == null )
        {
            Interpreter.debug( "trying class: ", evalName);

            /*
                Keep adding parts until we have a class
            */
            Class<?> clas = null;
            int i = 1;
            String className = null;
            for(; i <= countParts(evalName); i++)
            {
                className = prefix(evalName, i);
                if ( (clas = namespace.getClass(className)) != null )
                    break;
            }

            if ( clas != null )  {
                return completeRound(
                    className,
                    suffix( evalName, countParts(evalName)-i ),
                    new ClassIdentifier(clas)
                );
            }
            // not a class (or variable per above)
            Interpreter.debug( "not a class, trying var prefix ", evalName );
        }

        // No variable or class found in 'this' type ref.
        // if autoAllocateThis then create one; a child 'this'.
        if ( ( evalBaseObject == null || evalBaseObject instanceof This  )
            && !forceClass && autoAllocateThis )
        {
            NameSpace targetNameSpace =
                ( evalBaseObject == null ) ?
                    namespace : ((This)evalBaseObject).namespace;
            Object obj = new NameSpace(
                targetNameSpace, "auto: "+varName ).getThis( interpreter );
            targetNameSpace.setVariable( varName, obj, false, evalBaseObject == null );
            return completeRound( varName, suffix(evalName), obj );
        }

        /*
            If we didn't find a class or variable name (or prefix) above
            there are two possibilities:

            - If we are a simple name then we can check if we are an imported
              property or pass as a void variable reference.
            - If we are compound then we must fail at this point.
        */
        if ( evalBaseObject == null ) {
            if ( !isCompound(evalName) ) {
                Object obj = namespace.getPropertyValue(varName, interpreter);
                return completeRound( evalName, FINISHED, obj );
            } else
                throw new UtilEvalError(
                    "Class or variable not found: " + evalName);
        }

        /*
            --------------------------------------------------------
            After this point we're definitely evaluating relative to
            a base object.
            --------------------------------------------------------
        */

        /*
            Do some basic validity checks.
        */

        if ( evalBaseObject == Primitive.NULL) // previous round produced null
            throw new UtilTargetError( new NullPointerException(
                "Null Pointer while evaluating: " +value ) );

        if ( evalBaseObject == Primitive.VOID) // previous round produced void
            throw new UtilEvalError(
                "Undefined variable or class name while evaluating: "+value);

        if ( evalBaseObject instanceof Primitive)
            throw new UtilEvalError("Can't treat primitive like an object. "+
            "Error while evaluating: "+value);

        /*
            Resolve relative to a class type
            static field, inner class, ?
        */
        if ( evalBaseObject instanceof ClassIdentifier )
        {
            Class<?> clas = ((ClassIdentifier)evalBaseObject).getTargetClass();
            String field = prefix(evalName, 1);

            // Class qualified 'this' reference from inner class.
            // e.g. 'MyOuterClass.this'
            if ( field.equals("this") )
            {
                // find the enclosing class instance space of the class name
                NameSpace ns = namespace;
                while ( ns != null )
                {
                    // getClassInstance() throws exception if not there
                    if ( ns.classInstance != null
                        && ns.classInstance.getClass() == clas
                    )
                        return completeRound(
                            field, suffix(evalName), ns.classInstance );
                    ns=ns.getParent();
                }
                throw new UtilEvalError(
                    "Can't find enclosing 'this' instance of class: "+clas);
            }

            Object obj = null;

            // Validate if can get this static field
            Interpreter.mainSecurityGuard.canGetStaticField(clas, field);

            // static field?
            try {
                Interpreter.debug("Name call to getStaticFieldValue, class: ",
                        clas, ", field:", field);
                obj = Reflect.getStaticFieldValue(clas, field);
            } catch( ReflectError e ) {
                Interpreter.debug("field reflect error: ", e);
            }

            // inner class?
            if ( obj == null ) {
                String iclass = clas.getName()+"$"+field;
                Class<?> c = namespace.getClass( iclass );

                if (null == namespace.classInstance
                        && Reflect.isGeneratedClass(c)
                        && !Reflect.getClassModifiers(c).hasModifier("static"))
                    throw new UtilEvalError("an enclosing instance that contains "
                            + clas.getName() + "." + field + " is required");

                if ( c != null )
                    obj = new ClassIdentifier(c);
            }

            // static bean property
            if ( obj == null )
                obj = Reflect.getObjectProperty(clas, field);

            return completeRound( field, suffix(evalName), obj );
        }

        /*
            If we've fallen through here we are no longer resolving to
            a class type.
        */
        if ( forceClass )
            throw new UtilEvalError(
                value +" does not resolve to a class name." );

        /*
            Some kind of field access?
        */

        String field = prefix(evalName, 1);

        // Validate if can get this field
        Interpreter.mainSecurityGuard.canGetField(evalBaseObject, field);

        // length access on array?
        if ( field.equals("length") && evalBaseObject.getClass().isArray() )
        {
            Object obj = new Primitive(arrayLength(evalBaseObject));
            return completeRound( field, suffix(evalName), obj );
        }

        // Scripted-class instances expose fields via their backing namespace.
        if (evalBaseObject instanceof ScriptedInstance) {
            Object obj = ((ScriptedInstance) evalBaseObject).getField(field);
            if (obj != Primitive.VOID) {
                return completeRound( field, suffix(evalName), obj );
            }
        }
        // Static fields / enum constants on a scripted class.
        if (evalBaseObject instanceof ScriptedClass) {
            try {
                Object obj = ((ScriptedClass) evalBaseObject)
                        .getStaticNameSpace().getVariable(field);
                if (obj != Primitive.VOID) {
                    return completeRound( field, suffix(evalName), obj );
                }
            } catch (UtilEvalError ex) {
                // fall through to error path
            }
        }

        // Check for field on object
        // Note: could eliminate throwing the exception somehow
        try {
            Object obj = Reflect.getObjectFieldValue(evalBaseObject, field);
            return completeRound( field, suffix(evalName), obj );
        } catch(ReflectError e) { /* not a field */ }

        Object obj = Reflect.getObjectProperty(evalBaseObject, field);
        return completeRound( field, suffix(evalName), obj );

    }

    /**
        Resolve a variable relative to a This reference.

        This is the general variable resolution method, accommodating special
        fields from the This context.  Together the namespace and interpreter
        comprise the This context.  The callstack, if available allows for the
        this.caller construct.
        Optionally interpret special "magic" field names: e.g. interpreter.
        <p/>

        @param callstack may be null, but this is only legitimate in special
        cases where we are sure resolution will not involve this.caller.

        @param namespace the namespace of the this reference (should be the
        same as the top of the stack?
    */
    Object resolveThisFieldReference(
        CallStack callstack, NameSpace thisNameSpace, Interpreter interpreter,
        String varName, boolean specialFieldsVisible )
        throws UtilEvalError
    {
        if ( varName.equals("this") )
        {
            /*
                Somewhat of a hack.  If the special fields are visible (we're
                operating relative to a 'this' type already) dissallow further
                .this references to prevent user from skipping to things like
                super.this.caller
            */
            if ( specialFieldsVisible )
                throw new UtilEvalError("Redundant to call .this on This type");

            // Init this for block namespace and methods
            This thiz = thisNameSpace.getThis( interpreter );
            thisNameSpace = thiz.getNameSpace();

            // This is class namespace or instance reference
            NameSpace classNameSpace = getClassNameSpace( thisNameSpace );
            if ( classNameSpace != null )
            {
                if ( isCompound( evalName ) )
                    return classNameSpace.getThis( interpreter );
                else
                    return classNameSpace.getClassInstance();
            }

            return thiz;
        }

        /*
            Some duplication for "super".  See notes for "this" above
            If we're in an enclsing class instance and have a superclass
            instance our super is the superclass instance.
        */
        if ( varName.equals("super") )
        {

            // Allow getSuper() to go through BlockNameSpace to the method's super
            This zuper = thisNameSpace.getSuper( interpreter );
            thisNameSpace = zuper.getNameSpace();
            // super is now the closure's super or class instance

            // If we're a class instance and the parent is also a class instance
            // then super means our parent.
            if ( thisNameSpace.getParent() != null && thisNameSpace.getParent().isClass )
                return thisNameSpace.getSuper( interpreter );

            return zuper;
        }

        Object obj = null;

        if ( varName.equals("global") )
            obj = thisNameSpace.getGlobal( interpreter );

        if ( obj == null && specialFieldsVisible )
        {
            if (varName.equals("namespace"))
                obj = thisNameSpace;
            else if (varName.equals("variables"))
                obj = thisNameSpace.getVariableNames();
            else if (varName.equals("methods"))
                obj = thisNameSpace.getMethodNames();
            else if ( varName.equals("interpreter") )
                if ( lastEvalName.equals("this") )
                    obj = interpreter;
                else
                    throw new UtilEvalError(
                        "Can only call .interpreter on literal 'this'");
        }

        if ( obj == null && specialFieldsVisible && varName.equals("caller") )
        {
            if ( lastEvalName.equals("this") || lastEvalName.equals("caller") )
            {
                // get the previous context (see notes for this class)
                if ( callstack == null )
                    throw new InterpreterError("no callstack");
                obj = callstack.get( ++callstackDepth ).getThis(
                    interpreter );
            }
            else
                throw new UtilEvalError(
                "Can only call .caller on literal 'this' or literal '.caller'");

            // early return
            return obj;
        }

        if ( obj == null && specialFieldsVisible
            && varName.equals("callstack") )
        {
            if ( lastEvalName.equals("this") )
            {
                // get the previous context (see notes for this class)
                if ( callstack == null )
                    throw new InterpreterError("no callstack");
                obj = callstack;
            }
            else
                throw new UtilEvalError(
                "Can only call .callstack on literal 'this'");
        }

        if ( obj == null )
            obj = thisNameSpace.getVariable(varName, evalBaseObject == null);

        if ( obj == null )
            obj = Primitive.NULL;

        return obj;
    }

    /**
        @return the enclosing class body namespace or null if not in a class.
    */
    static NameSpace getClassNameSpace( NameSpace thisNameSpace )
    {
        if ( null == thisNameSpace )
            return null;

        // is a class instance
        if ( thisNameSpace.isClass )
            return thisNameSpace;

        // is a method parent is a class
        if ( thisNameSpace.isMethod
                && thisNameSpace.getParent() != null
                && thisNameSpace.getParent().isClass )
            return thisNameSpace.getParent();

        return null;
    }

    /**
        Check the cache, else use toObject() to try to resolve to a class
        identifier.

        @throws ClassNotFoundException on class not found.
        @throws ClassPathException (type of EvalError) on special case of
        ambiguous unqualified name after super import.
    */
    synchronized public Class<?> toClass()
        throws ClassNotFoundException, UtilEvalError
    {
        if ( asClass != null )
            return asClass;

        reset();

        // "var" means untyped, return null class
        if ( evalName.equals("var") )
            return asClass = null;

        /* Try straightforward class name first */
        Class<?> clas = namespace.getClass( evalName );

        if ( clas == null )
        {
            /*
                Try toObject() which knows how to work through inner classes
                and see what we end up with
            */
            Object obj = null;
            try {
                // Null interpreter and callstack references.
                // class only resolution should not require them.
                obj = toObject( null, null, true );
            } catch ( UtilEvalError  e ) { } // couldn't resolve it

            if ( obj instanceof ClassIdentifier )
                clas = ((ClassIdentifier)obj).getTargetClass();
        }

        if ( clas == null )
            throw new ClassNotFoundException(
                "Class: " + value+ " not found in namespace");

        asClass = clas;
        return asClass;
    }

    /*
    */
    synchronized public LHS toLHS(
        CallStack callstack, Interpreter interpreter )
        throws UtilEvalError
    {
        // Should clean this up to a single return statement
        reset();
        LHS lhs;

        // Simple (non-compound) variable assignment e.g. x=5;
        if ( !isCompound(evalName) )
        {
            if ( evalName.equals("this") )
                throw new UtilEvalError("Can't assign to 'this'." );

            if (namespace.isClass) // Loose type field
                lhs = new LHS( namespace, evalName );
            else
                lhs = new LHS( namespace, evalName, false/*bubble up if allowed*/);
            return lhs;
        }

        // Field e.g. foo.bar=5;
        Object obj = null;
        try {
            while( evalName != null && isCompound( evalName ) )
            {
                obj = consumeNextObjectField( callstack, interpreter,
                    false/*forcclass*/, true/*autoallocthis*/ );
            }
        }
        catch( UtilEvalError e ) {
            throw new UtilEvalError( "LHS evaluation: " + e.getMessage(), e);
        }

        // Finished eval and its a class.
        if ( evalName == null && obj instanceof ClassIdentifier )
            throw new UtilEvalError("Can't assign to class: " + value );

        if ( obj == null )
            throw new UtilEvalError("Error in LHS: " + value );

        // e.g. this.x=5;  or someThisType.x=5;
        if ( obj instanceof This )
        {
            // dissallow assignment to magic fields
            if (
                evalName.equals("namespace")
                || evalName.equals("variables")
                || evalName.equals("methods")
                || evalName.equals("caller")
            )
                throw new UtilEvalError(
                    "Can't assign to special variable: "+evalName );

            Interpreter.debug("found This reference evaluating LHS");
            // Scripted-class `this.x = v` inside an instance method:
            // BSH's resolveThisFieldReference returns the innermost This
            // (the method's local-scope This), so a plain VARIABLE LHS
            // ends up mutating a local variable rather than the instance
            // field. Walk the namespace chain for a user-bound
            // ScriptedInstance and target its instance namespace instead.
            NameSpace thisNs = ((This) obj).namespace;
            NameSpace walk = thisNs;
            while (walk != null) {
                try {
                    Object inner = walk.getVariable("this");
                    if (inner instanceof ScriptedInstance) {
                        return new LHS(
                                ((ScriptedInstance) inner).getInstanceNameSpace(),
                                evalName, true);
                    }
                } catch (UtilEvalError ignore) {
                    // try the parent namespace
                }
                walk = walk.getParent();
            }
            /*
                If this was a literal "super" reference then we allow recursion
                in setting the variable to get the normal effect of finding the
                nearest definition starting at the super scope.  On any other
                resolution qualified by a 'this' type reference we want to set
                the variable directly in that scope. e.g. this.x=5;  or
                someThisType.x=5;

                In the old scoping rules super didn't do this.
            */
            boolean localVar = !lastEvalName.equals("super");
            return new LHS( ((This)obj).namespace, evalName, localVar );
        }

        if ( evalName != null )
        {
            // Scripted-class field LHS: route through the instance's
            // namespace as a VARIABLE LHS so assignment updates the field
            // value without falling to Reflect.setObjectProperty (which
            // invokes getter/setter methods on a null callstack).
            if ( obj instanceof ScriptedInstance ) {
                return new LHS(((ScriptedInstance) obj).getInstanceNameSpace(),
                        evalName, true);
            }
            if ( obj instanceof ScriptedClass ) {
                return new LHS(((ScriptedClass) obj).getStaticNameSpace(),
                        evalName, true);
            }
            // When `this` resolves through BSH's builtin (returning a
            // bsh.This whose namespace contains a user-bound ScriptedInstance
            // under the variable name "this"), route the assignment to the
            // ScriptedInstance's instance namespace too.
            if ( obj instanceof This ) {
                try {
                    Object inner = ((This) obj).namespace.getVariable("this");
                    if ( inner instanceof ScriptedInstance ) {
                        return new LHS(
                                ((ScriptedInstance) inner).getInstanceNameSpace(),
                                evalName, true);
                    }
                } catch (UtilEvalError ex) {
                    // fall through
                }
            }
            try {
                if ( obj instanceof ClassIdentifier )
                {
                    Class<?> clas = ((ClassIdentifier)obj).getTargetClass();
                    lhs = Reflect.getLHSStaticField(clas, evalName);
                    return lhs;
                } else {
                    lhs = Reflect.getLHSObjectField(obj, evalName);
                    return lhs;
                }
            } catch(ReflectError e) {
                return new LHS(obj, evalName);
            }
        }

        throw new InterpreterError("Internal error in lhs...");
    }

    /**
        Invoke the method identified by this name.
        Performs caching of method resolution using SignatureKey.
        <p>

        Name contains a wholely unqualfied messy name; resolve it to
        ( object | static prefix ) + method name and invoke.
        <p>

        The interpreter is necessary to support 'this.interpreter' references
        in the called code. (e.g. debug());
        <p>

        <pre>
        Some cases:

            // dynamic
            local();
            myVariable.foo();
            myVariable.bar.blah.foo();
            // static
            java.lang.Integer.getInteger("foo");
        </pre>
    */
    /** Walk up the callstack to find a NameSpace whose `this` binding is a
     * ScriptedInstance. Used to back-out the receiver for `super.foo()`
     * dispatch from inside a scripted-class method body. */
    private static ScriptedInstance findScriptedThis(CallStack callstack) {
        if (callstack == null) return null;
        for (int i = 0; i < callstack.depth(); i++) {
            NameSpace ns = callstack.get(i);
            try {
                Object t = ns.getVariable("this");
                if (t instanceof ScriptedInstance) return (ScriptedInstance) t;
            } catch (UtilEvalError ex) {
                // continue walking
            }
        }
        return null;
    }

    public Object invokeMethod(
        Interpreter interpreter, Object[] args, CallStack callstack,
        Node callerInfo
    )
        throws UtilEvalError, EvalError, ReflectError
    {
        String methodName = Name.suffix(value, 1);
        BshClassManager bcm = interpreter.getClassManager();
        NameSpace namespace = callstack.top();

        // Optimization - If classOfStaticMethod is set then we have already
        // been here and determined that this is a static method invocation.
        // Note: maybe factor this out with path below... clean up.
        if ( classOfStaticMethod != null ) {
            // Validate if can invoke this static method
            Interpreter.mainSecurityGuard.canInvokeStaticMethod(classOfStaticMethod, methodName, args);

            return Reflect.invokeStaticMethod(
                bcm, classOfStaticMethod, methodName, args, callerInfo, callstack );
        }

        if ( !Name.isCompound(value) )
            return invokeLocalMethod(
                interpreter, args, callstack, callerInfo );

        // Note: if we want methods declared inside blocks to be accessible via
        // this.methodname() inside the block we could handle it here as a
        // special case.  See also resolveThisFieldReference() special handling
        // for BlockNameSpace case.  They currently work via the direct name
        // e.g. methodName().

        String prefix = Name.prefix(value);

        // Superclass method invocation? (e.g. super.foo())
        if ( prefix.equals("super") && Name.countParts(value) == 2 ) {
            // Scripted-class super dispatch: walk up the callstack to find the
            // bound ScriptedInstance and invoke the parent's method template.
            // The "current dispatch class" override is consulted first so
            // that chained super calls through multi-level inheritance
            // advance up the chain instead of looping on the same parent.
            ScriptedInstance scriptedThis = findScriptedThis(callstack);
            if (scriptedThis != null) {
                ScriptedClass dispatchClass = ScriptedClass.currentDispatchClass();
                if (dispatchClass == null) {
                    dispatchClass = scriptedThis.getScriptedClass();
                }
                ScriptedClass parentClass = dispatchClass.getParent();
                ScriptedClass.MethodTemplate parentTpl = parentClass == null
                        ? null
                        : parentClass.findInstanceMethodTemplate(methodName, args);
                if (parentTpl != null) {
                    BshMethod bound = parentTpl.bind(scriptedThis.getInstanceNameSpace());
                    ScriptedClass.pushDispatchClass(parentClass);
                    try {
                        return bound.invoke(args, interpreter, callstack, callerInfo, false);
                    } finally {
                        ScriptedClass.popDispatchClass();
                    }
                }
                throw new UtilEvalError("No super method "
                        + scriptedThis.getScriptedClass().getName()
                        + "." + methodName + "/"
                        + (args == null ? 0 : args.length));
            }
            // Allow getThis() to work through block namespaces first
            This ths = namespace.getThis( interpreter );
            NameSpace thisNameSpace = ths.getNameSpace();
            thisNameSpace.setNode(callerInfo);
            NameSpace classNameSpace = getClassNameSpace( thisNameSpace );
            if ( classNameSpace != null ) {
                Object instance = classNameSpace.getClassInstance();
                Class<?> classStatic = classNameSpace.classStatic;

                return ClassGenerator.getClassGenerator()
                    .invokeSuperclassMethod( bcm, instance, classStatic, methodName, args );
            }
        }

        // Find target object or class identifier
        Name targetName = namespace.getNameResolver( prefix );
        Object obj = targetName.toObject( callstack, interpreter );

        if ( obj == Primitive.VOID )
            throw new UtilEvalError( "Attempt to resolve method: "+methodName
                    +"() on undefined variable or class name: "+targetName);

        // if we've got an object, resolve the method
        if ( !(obj instanceof ClassIdentifier) ) {
            if (obj instanceof Primitive)
                if (obj == Primitive.NULL)
                    throw new UtilTargetError( new NullPointerException(
                        "Null Pointer in Method Invocation of " +methodName
                            +"() on variable: "+targetName) );

            // Scripted-class instances dispatch through their own descriptor,
            // not through the CN1 access registry.
            if ( obj instanceof ScriptedInstance ) {
                return ((ScriptedInstance) obj).invokeMethod(
                        methodName, args, interpreter, callstack, callerInfo);
            }
            // Static-method dispatch on a scripted class or interface or enum.
            if ( obj instanceof ScriptedClass ) {
                ScriptedClass sc = (ScriptedClass) obj;
                BshMethod m = sc.findStaticMethod(methodName, args);
                if (m != null) {
                    return m.invoke(args, interpreter, callstack, callerInfo);
                }
                Object builtin = sc.invokeEnumBuiltinStatic(methodName, args);
                if (builtin != null) return builtin;
                throw new UtilEvalError("No static method " + sc.getName()
                        + "." + methodName + "/"
                        + (args == null ? 0 : args.length));
            }

            // enum block members will be in namespace only
            if ( obj.getClass().isEnum() ) {
                NameSpace thisNamespace = Reflect.getThisNS(obj);
                if ( null != thisNamespace ) {
                    BshMethod m = thisNamespace.getMethod(methodName, Types.getTypes(args), true);
                    if ( null != m )
                        return m.invoke(args, interpreter, callstack, callerInfo);
                }
            }

            // Validate if can invoke this static method
            Interpreter.mainSecurityGuard.canInvokeMethod(obj, methodName, args);

            // found an object and it's not an undefined variable
            return Reflect.invokeObjectMethod(
                obj, methodName, args, interpreter, callstack, callerInfo );
        }

        // It's a class

        // try static method
        Interpreter.debug("invokeMethod: trying static - ", targetName);

        Class<?> clas = ((ClassIdentifier)obj).getTargetClass();

        // cache the fact that this is a static method invocation on this class
        classOfStaticMethod = clas;

        // return null; ???
        if ( clas == null )
            throw new UtilEvalError("invokeMethod: unknown target: " + targetName);

        // Validate if can invoke this static method
        Interpreter.mainSecurityGuard.canInvokeStaticMethod(clas, methodName, args);

        return Reflect.invokeStaticMethod( bcm, clas, methodName, args, callerInfo, callstack );
    }

    /**
        Invoke a locally declared method or a bsh command.
        If the method is not already declared in the namespace then try
        to load it as a resource from the imported command path (e.g.
        /bsh/commands)
    */
    private static boolean isNoOverride(String name) {
        return "eval".equals(name) || "assert".equals(name);
    }
    private Object invokeLocalMethod(
        Interpreter interpreter, Object[] args, CallStack callstack, Node callerInfo)
        throws EvalError
    {
        Interpreter.debug( "invokeLocalMethod: ", value );
        if ( interpreter == null )
            throw new InterpreterError(
                "invokeLocalMethod: interpreter = null");

        String methodName = value;
        Class<?>[] argTypes = Types.getTypes( args );

        try {
            Interpreter.mainSecurityGuard.canInvokeLocalMethod(methodName, args);
        } catch ( UtilEvalError e ) {
            throw e.toEvalError(callerInfo, callstack);
        }

        // Check for existing method
        BshMethod meth = null;
        try {
            meth = namespace.getMethod( methodName, argTypes );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError(
                "Local method invocation", callerInfo, callstack );
        }

        // If defined, invoke it
        if ( meth != null ) {
            // whether to use callstack.top or new child of declared name space
            // enables late binding for closures and namespace chaining #676
            boolean overrideChild = !namespace.isMethod
                    && !meth.isScriptedObject
                    && namespace.isChildOf(meth.declaringNameSpace)
                    && !namespace.getParent().isClass
                    && !isNoOverride(meth.getName());

            return meth.invoke( args, interpreter, callstack, callerInfo, overrideChild );
        }

        // super(args) inside a scripted-class constructor: forward to the
        // matching ctor on the parent ScriptedClass, or if the parent is a
        // Java class, construct a delegate Java instance and stash it on
        // the ScriptedInstance so subsequent method calls can fall through
        // to the real Java parent (common for Exception subclasses).
        if ("super".equals(methodName)) {
            ScriptedInstance scriptedThis = findScriptedThis(callstack);
            if (scriptedThis != null) {
                ScriptedClass sc = scriptedThis.getScriptedClass();
                if (sc.getParent() != null) {
                    ScriptedClass.MethodTemplate ctor = sc.getParent().findConstructorTemplate(args);
                    if (ctor != null) {
                        BshMethod bound = ctor.bind(scriptedThis.getInstanceNameSpace());
                        return bound.invoke(args, interpreter, callstack, callerInfo, false);
                    }
                }
                if (sc.getJavaParent() != null) {
                    try {
                        Object delegate = Reflect.constructObject(sc.getJavaParent(), args);
                        scriptedThis.setJavaDelegate(delegate);
                        return Primitive.VOID;
                    } catch (ReflectError ex) {
                        throw new EvalError("super() for Java parent "
                                + sc.getJavaParent().getName() + " failed: " + ex.getMessage(),
                                callerInfo, callstack);
                    }
                }
            }
        }

        // Look for a BeanShell command
        return namespace.invokeCommand(methodName, args, interpreter, callstack, callerInfo);
    }

    // Static methods that operate on compound ('.' separated) names
    // I guess we could move these to StringUtil someday
    private static class Parts {
        private static final Map<String, Parts> PARTSCACHE = new HashMap<String, Parts>();
        private final String[] prefix;
        private final String[] suffix;
        private final List<String> list;
        public final int count;
        private Parts(String value) {
            this.list = Arrays.asList(splitParts(value));
            this.count = list.size();
            this.prefix = new String[count + 1];
            this.suffix = new String[count + 1];
        }
        public String prefix(int parts) {
            if (1 > parts || count < parts)
                return null;
            if (null == prefix[parts])
                prefix[parts] = joinParts(list, 0, parts);
            return prefix[parts];
        }
        public String suffix(int parts) {
            if (1 > parts || count < parts)
                return null;
            if (null == suffix[parts])
                suffix[parts] = joinParts(list, count - parts, count);
            return suffix[parts];
        }
        public static Parts get(String value) {
            if (PARTSCACHE.containsKey(value)) {
                Parts parts = PARTSCACHE.get(value);
                if (null != parts)
                    return parts;
                PARTSCACHE.remove(value);
            }
            Parts parts = new Parts(value);
            PARTSCACHE.put(value, parts);
            parts.prefix[parts.count] = value;
            parts.suffix[parts.count] = value;
            if (parts.count == 1)
                return parts;
            parts.prefix[1] = parts.list.get(0);
            parts.suffix[1] = parts.list.get(parts.count - 1);
            return parts;
        }
    }

    static void clearParts() {
        synchronized (Parts.PARTSCACHE) {
            Parts.PARTSCACHE.clear();
        }
    }

    private static String[] splitParts(String value) {
        if (value == null || value.length() == 0) {
            return new String[] { "" };
        }
        int count = 1;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '.') {
                count++;
            }
        }
        String[] out = new String[count];
        int part = 0;
        int start = 0;
        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == '.') {
                out[part++] = value.substring(start, i);
                start = i + 1;
            }
        }
        return out;
    }

    private static String joinParts(List<String> list, int start, int end) {
        StringBuffer out = new StringBuffer();
        for (int i = start; i < end; i++) {
            if (i > start) {
                out.append('.');
            }
            out.append(list.get(i));
        }
        return out.toString();
    }

    private static int arrayLength(Object array) throws UtilEvalError {
        if (array instanceof Object[]) return ((Object[]) array).length;
        if (array instanceof int[]) return ((int[]) array).length;
        if (array instanceof long[]) return ((long[]) array).length;
        if (array instanceof short[]) return ((short[]) array).length;
        if (array instanceof byte[]) return ((byte[]) array).length;
        if (array instanceof char[]) return ((char[]) array).length;
        if (array instanceof boolean[]) return ((boolean[]) array).length;
        if (array instanceof float[]) return ((float[]) array).length;
        if (array instanceof double[]) return ((double[]) array).length;
        throw new UtilEvalError("Unsupported array type: " + array.getClass());
    }

    public static boolean isCompound(String value)
    {
        return countParts(value) > 1;
    }

    static int countParts(String value)
    {
        if( value == null )
            return 0;
        return Parts.get(value).count;
    }

    static String prefix(String value)
    {
        return prefix(value, countParts(value) - 1);
    }

    static String prefix(String value, int parts)
    {
        if (null == value)
            return null;
        return Parts.get(value).prefix(parts);
    }

    static String suffix(String value)
    {
        return suffix(value, countParts(value) - 1);
    }

    public static String suffix(String value, int parts)
    {
        if (null == value)
            return null;
        return Parts.get(value).suffix(parts);
    }

    // end compound name routines


    public String toString() { return value; }

}
