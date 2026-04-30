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

import java.util.List;
import java.util.Map.Entry;

class BSHPrimarySuffix extends SimpleNode
{
    public static final int
        CLASS = 6,
        INDEX = 1,
        NAME = 2,
        PROPERTY = 3,
        NEW = 4;

    public int operation;
    Object index;
    public String field;
    public boolean slice = false, step = false,
        hasLeftIndex = false, hasRightIndex = false,
        safeNavigate = false;

    BSHPrimarySuffix(int id) { super(id); }

    /*
        Perform a suffix operation on the given object and return the
        new value.
        <p>

        obj will be a Node when suffix evaluation begins, allowing us to
        interpret it contextually. (e.g. for .class) Thereafter it will be
        a value object or LHS (as determined by toLHS).
        <p>

        We must handle the toLHS case at each point here.
        <p>
    */
    public Object doSuffix(
        Object obj, boolean toLHS,
        CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        // Handle ".class" suffix operation
        // Prefix must be a BSHType
        if ( operation == CLASS )
            if ( obj instanceof BSHType ) {
                if ( toLHS )
                    throw new EvalException("Can't assign .class",
                        this, callstack );
                return ((BSHType)obj).getType( callstack, interpreter );
            } else
                throw new EvalException(
                    "Attempt to use .class suffix on non class.",
                    this, callstack );

        /*
            Evaluate our prefix if it needs evaluating first.
            If this is the first evaluation our prefix mayb be a Node
            (directly from the PrimaryPrefix) - eval() it to an object.
            If it's an LHS, resolve to a value.

            Note: The ambiguous name construct is now necessary where the node
            may be an ambiguous name.  If this becomes common we might want to
            make a static method nodeToObject() or something.  The point is
            that we can't just eval() - we need to direct the evaluation to
            the context sensitive type of result; namely object, class, etc.
        */
        if ( obj instanceof Node )
            if ( obj instanceof BSHAmbiguousName )
                obj = ((BSHAmbiguousName)obj).toObject(callstack, interpreter);
            else
                obj = ((Node)obj).eval(callstack, interpreter);
        else
            if ( obj instanceof LHS ) try {
                obj = ((LHS)obj).getValue();
            } catch ( UtilEvalError e ) {
                throw e.toEvalError( this, callstack );
            }

        try
        {
            switch(operation)
            {
                case INDEX:
                    return doIndex( obj, toLHS, callstack, interpreter );

                case NAME:
                    return doName( obj, toLHS, callstack, interpreter );

                case PROPERTY:
                    return doProperty( toLHS, obj, callstack, interpreter );

                case NEW:
                    return doNewInner(obj, toLHS, callstack, interpreter);
                default:
                    throw new InterpreterError( "Unknown suffix type" );
            }
        }
        catch(ReflectError e)
        {
            throw new EvalError("reflection error: " + e, this, callstack, e );
        }
    }

    /*
        Instance.new InnerClass() implementation
    */
    private Object doNewInner(Object obj, boolean toLHS,
            CallStack callstack, Interpreter interpreter) throws EvalError {
        BSHAllocationExpression alloc = (BSHAllocationExpression) jjtGetChild(0);
        if (Reflect.isGeneratedClass(obj.getClass())) {
            callstack.pop();
            callstack.push(Reflect.getThisNS(obj));
            return alloc.eval(callstack, interpreter);
        }

        return alloc.constructFromEnclosingInstance(obj, callstack, interpreter);
    }

    /*
        Field access, .length on array, or a method invocation
        Must handle toLHS case for each.
    */
    private Object doName(
        Object obj, boolean toLHS,
        CallStack callstack, Interpreter interpreter)
            throws EvalError, ReflectError {
        try {
            // Safe Navigate operator ?. abort on null
            if (this.safeNavigate && Primitive.NULL == obj)
                throw SafeNavigate.doAbort();

            // .length on array
            if ( field.equals("length") && obj.getClass().isArray() ) {
                // Validate if can get this field
                Interpreter.mainSecurityGuard.canGetField(obj, field);

                if ( toLHS )
                    throw new EvalError(
                        "Can't assign array length", this, callstack );
                else
                    return new Primitive(BshArray.arrayLength(obj));
            }

            // field access
            if ( jjtGetNumChildren() == 0 ) {
                // Validate if can get this field
                Interpreter.mainSecurityGuard.canGetField(obj, field);

                // Scripted-class field access goes through the instance namespace.
                if (obj instanceof ScriptedInstance) {
                    ScriptedInstance si = (ScriptedInstance) obj;
                    if (toLHS) {
                        // Use VARIABLE LHS (setVariable) so an assignment
                        // updates a pre-declared field's value without
                        // re-typing it. LOOSETYPE_FIELD calls setTypedVariable
                        // which can fail to overwrite an Object-typed field
                        // with a concrete ScriptedInstance.
                        return new LHS(si.getInstanceNameSpace(), field, true);
                    }
                    Object v = si.getField(field);
                    return v == Primitive.VOID ? Primitive.VOID : v;
                }
                // Static field/constant access on a scripted class or enum.
                if (obj instanceof ScriptedClass) {
                    ScriptedClass sc = (ScriptedClass) obj;
                    NameSpace staticNs = sc.getStaticNameSpace();
                    if (toLHS) {
                        return new LHS(staticNs, field);
                    }
                    try {
                        Object v = staticNs.getVariable(field);
                        if (v != Primitive.VOID) return v;
                    } catch (UtilEvalError ex) {
                        // fall through
                    }
                    throw new EvalError("No static field " + sc.getName()
                            + "." + field, this, callstack);
                }

                if ( toLHS ) try {
                    return Reflect.getLHSObjectField(obj, field);
                } catch (Throwable t) {
                    return new LHS(obj, field);
                }
                else try {
                    return Reflect.getObjectFieldValue( obj, field );
                } catch (Throwable t) {
                    try {
                        return Reflect.getObjectProperty( obj, field );
                    } catch (Throwable tt) {
                        return Primitive.VOID;
                    }
                }
            }

            // Method invocation
            // (LHS or non LHS evaluation can both encounter method calls)
            Object[] oa = ((BSHArguments)jjtGetChild(0)).getArguments(
                callstack, interpreter);

            // Validate if can invoke this method
            Interpreter.mainSecurityGuard.canInvokeMethod(obj, field, oa);

            // Scripted-class method dispatch.
            if (obj instanceof ScriptedInstance) {
                return ((ScriptedInstance) obj).invokeMethod(
                        field, oa, interpreter, callstack, this);
            }
            // Static-method dispatch on a scripted class/interface/enum.
            if (obj instanceof ScriptedClass) {
                ScriptedClass sc = (ScriptedClass) obj;
                BshMethod m = sc.findStaticMethod(field, oa);
                if (m != null) {
                    return m.invoke(oa, interpreter, callstack, this, false);
                }
                Object builtin = sc.invokeEnumBuiltinStatic(field, oa);
                if (builtin != null) return builtin;
                throw new EvalError("No static method " + sc.getName()
                        + "." + field + "/"
                        + (oa == null ? 0 : oa.length), this, callstack);
            }

            return Reflect.invokeObjectMethod(
                obj, field, oa, interpreter, callstack, this );
        } catch (UtilEvalError e1) {
            throw e1.toEvalError(this, callstack);
        }
    }

    /**
    */
    static int getIndexAux(Object obj, int idx, CallStack callstack,
        Interpreter interpreter, Node callerInfo )
                throws EvalError {
        int index;
        try {
            Object indexVal = callerInfo.jjtGetChild(idx).eval(
                    callstack, interpreter );
            if ( !(indexVal instanceof Primitive) )
                indexVal = Types.castObject(
                    indexVal, Integer.TYPE, Types.ASSIGNMENT );
            index = (int) Primitive.castWrapper(Integer.TYPE, indexVal);
        } catch( Exception e ) {
            Interpreter.debug("doIndex: "+e);
            throw new EvalError(
                "Array index does not evaluate to an integer.",
                callerInfo, callstack, e);
        }
        return index;
    }

    /** Array index or bracket expression implementation.
     * @param obj array or list instance
     * @param toLHS whether to return an LHS instance
     * @param callstack the evaluation call stack
     * @param interpreter the evaluation interpreter
     * @return data as per index expression or LHS for assignment
     * @throws EvalError with evaluation exceptions */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object doIndex(
        Object obj, boolean toLHS,
        CallStack callstack, Interpreter interpreter )
                throws EvalError {
        // Map or Entry index access not applicable to strict java
        if ( !interpreter.getStrictJava() ) {
            // allow index access for maps
            if ( Types.isPropertyTypeMap(obj) ) {
                Object key = jjtGetChild(0).eval(callstack, interpreter);
                return toLHS ? new LHS(obj, key)
                      : Reflect.getObjectProperty(obj, key);
            }
            // allow index access for map entries
            if ( Types.isPropertyTypeEntry(obj) ) {
                Object key = jjtGetChild(0).eval(callstack, interpreter);
                if ( toLHS ) {
                    if ( key.equals(((Entry) obj).getKey()) )
                        return new LHS(obj);
                    throw new EvalError("No such property: " + key, this, callstack);
                }
                return Reflect.getObjectProperty(obj, key);
            }
        }

        Class<?> cls = obj.getClass();
        if ( ( interpreter.getStrictJava() || !(obj instanceof List) )
                && !cls.isArray() )
            throw new EvalError("Not an array or List type", this, callstack );

        int length = obj instanceof List
                ? ((List) obj).size() : BshArray.arrayLength(obj);

        int index = length + 1;
        // allow index access for a Map.Entry array.
        if ( !interpreter.getStrictJava()
                && Types.isPropertyTypeEntryList(cls) ) {
            Object key = jjtGetChild(0).eval(callstack, interpreter);
            int idx = 0;
            if ( ((key instanceof Primitive && ((Primitive) key).isNumber())
                        || Primitive.isWrapperType(key.getClass()))
                    && length > (idx = (int) Primitive.castWrapper(Integer.TYPE, key))
                    && -length < idx)
                index = idx;
            else if (toLHS)
                return new LHS(Reflect.getEntryForKey(key, (Entry[]) obj));
            else
                return Reflect.getObjectProperty(obj, key);
        } else if ( index > length )
            index = getIndexAux( obj, 0, callstack, interpreter, this );

        // Negative index or slice expressions not applicable to strict java
        if ( !interpreter.getStrictJava() ) {
            if ( 0 > index )
                index = length + index;
            if ( this.slice ) {
                if ( toLHS )
                    throw new EvalError("cannot assign to array slice",
                            this, callstack);
                int rindex = 0, stepby = 0;
                if ( this.step ) {
                    Integer step = null;
                    if ( hasLeftIndex && hasRightIndex
                            && jjtGetNumChildren() == 3 )
                        step = getIndexAux(obj, 2, callstack, interpreter, this);
                    else if ( (!hasLeftIndex || !hasRightIndex)
                            && jjtGetNumChildren() == 2 )
                        step = getIndexAux(obj, 1, callstack, interpreter, this);
                    else if ( !hasLeftIndex && !hasRightIndex ) {
                        step = getIndexAux(obj, 0, callstack, interpreter, this);
                        index = 0;
                    }
                    if ( null != step ) {
                        if ( step == 0 )
                            throw new EvalError("array slice step cannot be zero",
                                    this, callstack);
                        stepby = step;
                    }
                }
                if ( hasLeftIndex && hasRightIndex )
                    rindex = getIndexAux(obj, 1, callstack, interpreter, this);
                else if ( !hasRightIndex )
                    rindex = length;
                else {
                    rindex = index;
                    index = 0;
                }
                if ( 0 > rindex )
                    rindex = length + rindex;
                if ( obj.getClass().isArray() )
                    return BshArray.slice(obj, index, rindex, stepby);
                return BshArray.slice((List<Object>) obj, index, rindex, stepby);
            }
        } else if ( this.slice )
            throw new EvalError("expected ']' but found ':'", this, callstack);


        if ( toLHS )
            return new LHS(obj, index);
        else try {
            return BshArray.getIndex(obj, index);
        } catch ( UtilEvalError e ) {
            throw e.toEvalError("Error array get index", this, callstack);
        }
    }

    /**
        Property access.
        Must handle toLHS case.
    */
    private Object doProperty( boolean toLHS,
        Object obj, CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        if(obj == Primitive.VOID)
            throw new EvalError(
            "Attempt to access property on undefined variable or class name",
                this, callstack );

        if ( obj instanceof Primitive )
            throw new EvalError("Attempt to access property on a primitive",
                this, callstack );

        Object value = jjtGetChild(0).eval(callstack, interpreter);

        if ( !( value instanceof String ) )
            throw new EvalError(
                "Property expression must be a String or identifier.",
                this, callstack );

        if ( toLHS )
            return new LHS(obj, (String)value);

        try {
            Object val = Reflect.getObjectProperty( obj, (String)value );
            return null == val ?  Primitive.NULL : Primitive.unwrap(val);
        }
        catch (ReflectError e)
        {
            throw new EvalError("No such property: " + value, this, callstack, e);
        }
    }

    @Override
    public String toString() {
        if (operation == INDEX)
            return super.toString() + ":INDEX [" + hasLeftIndex + ":" + slice + " " + hasRightIndex + ":" + step + "]";
        if (operation == NAME)
            return super.toString() + ":NAME " + field;
        if (operation == PROPERTY)
            return super.toString() + ":PROPERTY {}";
        if (operation == NEW)
            return super.toString() + ":NEW new";
        if (operation == CLASS)
            return super.toString() + ":CLASS class";
        return super.toString() + ":NO OPERATION";
    }
}
