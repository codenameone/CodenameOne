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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Objects;

import bsh.Types.MapEntry;
/**
    An LHS is a wrapper for an variable, field, or property.  It ordinarily
    holds the "left hand side" of an assignment and may be either resolved to
    a value or assigned a value.
    <p>

    There is one special case here termed METHOD_EVAL where the LHS is used
    in an intermediate evaluation of a chain of suffixes and wraps a method
    invocation.  In this case it may only be resolved to a value and cannot be
    assigned.  (You can't assign a value to the result of a method call e.g.
    "foo() = 5;").
    <p>
*/
class LHS implements ParserConstants, Serializable {
    private static final long serialVersionUID = 1L;

    NameSpace nameSpace;
    /** The assignment should be to a local variable */
    boolean localVar;

    /**
        Identifiers for the various types of LHS.
    */
    static final int
        VARIABLE = 0,
        FIELD = 1,
        PROPERTY = 2,
        INDEX = 3,
        METHOD_EVAL = 4,
        LOOSETYPE_FIELD = 5,
        MAP_ENTRY = 6;

    int type;

    String varName;
    Object propName;
    Invocable field;
    Object object;
    int index;
    Variable var;

    /**
        @param localVar if true the variable is set directly in the This
        reference's local scope.  If false recursion to look for the variable
        definition in parent's scope is allowed. (e.g. the default case for
        undefined vars going to global).
    */
    LHS( NameSpace nameSpace, String varName, boolean localVar )
    {
        type = VARIABLE;
        this.localVar = localVar;
        this.varName = varName;
        this.nameSpace = nameSpace;
    }

    LHS( NameSpace nameSpace, String varName )
    {
        type = LOOSETYPE_FIELD;
        this.varName = varName;
        this.nameSpace = nameSpace;
    }

    /**
        Static field LHS Constructor.
        This simply calls Object field constructor with null object.
    */
    LHS( Invocable field )
    {
        type = FIELD;
        this.object = field.getDeclaringClass();
        this.field = field;
        this.varName = field.getName();
    }

    /**
        Object field LHS Constructor.
    */
    LHS( Object object, Invocable field )
    {
        if ( object == null)
            throw new NullPointerException("constructed empty LHS");

        type = FIELD;
        this.object = object;
        this.field = field;
        if (null != field)
            this.varName = field.getName();
    }

    /**
        Object property LHS Constructor with Object property.
    */
    LHS( Object object, Object propName )
    {
        if( object == null )
            throw new NullPointerException("constructed empty LHS");

        type = PROPERTY;
        this.object = object;
        this.propName = propName;
    }

    /** Map Entry type LHS Constructor.
     * The provided key is returned along with its value as a Map.Entry
     * entity during assignment. */
    LHS( Object key )
    {
        type = MAP_ENTRY;
        this.object = key;
    }

    /**
        Array index LHS Constructor.
    */
    LHS( Object array, int index )
    {
        type = INDEX;
        this.object = array;
        this.index = index;
    }

    public Object getValue() throws UtilEvalError
    {
        if ( type == FIELD ) {
            // Validate if can get this field
            if (Reflect.isStatic(field))
                Interpreter.mainSecurityGuard.canGetStaticField(field.getDeclaringClass(), field.getName());
            else
                Interpreter.mainSecurityGuard.canGetField(object, field.getName());
        }

        return this.getValueImpl();
    }

    private Object getValueImpl() throws UtilEvalError
    {
        if ( type == VARIABLE )
            return nameSpace.getVariableOrProperty( varName, null );

        if ( type == FIELD ) try {
            return Objects.requireNonNull(field,
                "get value, field cannot be null").invoke(object);
        } catch( Exception e2 ) {
            throw new UtilEvalError("Can't read field: " + field, e2);
        }

        if ( type == PROPERTY ) try {
            return Reflect.getObjectProperty(object, this.propName);
        } catch(ReflectError e) {
            Interpreter.debug(e.getMessage());
            throw new UtilEvalError("No such property: " + propName, e);
        }

        if ( type == INDEX ) try {
            return BshArray.getIndex(object, index);
        } catch(Exception e) {
            throw new UtilEvalError("Array access: " + e, e);
        }

        if ( type == LOOSETYPE_FIELD )
            return nameSpace.getVariable( varName );

        throw new InterpreterError("LHS type");
    }

    public String getName() {
        if ( null != field )
            return field.getName();
        if ( null != var )
            return var.getName();
        return varName;
    }

    public Class<?> getType() {
        if ( null != field )
            return field.getReturnType();
        if ( null != getVariable() )
            return var.getType();
        try {
            return Types.getType(getValueImpl());
        } catch ( UtilEvalError e ) {
            return null;
        }
    }

    public boolean isStatic() {
        if ( null != field )
            return field.isStatic();
        if ( null == this.var )
            return false;
        return var.hasModifier("static");
    }

    public boolean isFinal() {
        if ( getVariable() == null )
            return false;
        return var.hasModifier("final");
    }

    public Variable getVariable() {
        if ( null != var )
            return this.var;
        if ( null != nameSpace )
            this.var = Reflect.getVariable(nameSpace, getName());
        else if ( isStatic() )
            if ( Reflect.isGeneratedClass(field.getDeclaringClass()) )
                this.var = Reflect.getVariable(field.getDeclaringClass(), getName());
            else
                this.var = new Variable(field.getName(), field.getReturnType(), this);
        else if ( Reflect.isGeneratedClass(object.getClass()) )
            this.var = Reflect.getVariable(object, getName());
        else if ( null != field )
            this.var = new Variable(field.getName(), field.getReturnType(), this);
        return this.var;
    }


    /** Overloaded assign with false as strict java.
     * @param val value to assign
     * @return result based on type
     * @throws UtilEvalError on exception */
    public Object assign( Object val )
            throws UtilEvalError {
        return this.assign(val, false);
    }

    /**
        Assign a value to the LHS.
    */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object assign( Object val, boolean strictJava )
        throws UtilEvalError
    {
        if ( type == VARIABLE )
        {
            // Set the variable in namespace according to localVar flag
            if ( localVar )
                nameSpace.setLocalVariableOrProperty( varName, val, strictJava );
            else
                nameSpace.setVariableOrProperty( varName, val, strictJava );
            return getValueImpl();
        } else  if ( type == FIELD )  try {
            Objects.requireNonNull(field,
                "assign value, field cannot be null").invoke( object, val);
            return getValueImpl();
        } catch (Exception e) {
            throw new UtilEvalError(
                "LHS ("+field.getName()+") can't access field: "+e, e);
        } else if ( type == PROPERTY ) try {
            if (propName instanceof String)
                return  Reflect.setObjectProperty(object, (String) propName, val);
            return  Reflect.setObjectProperty(object, propName, val);
        } catch(ReflectError e) {
            Interpreter.debug("Assignment: " + e.getMessage());
            throw new UtilEvalError("No such property: " + propName, e);
        }
        else if ( type == INDEX ) try {
            if ( object.getClass().isArray() && val != null ) try {
                val = Types.castObject( val,
                      Types.arrayElementType(object.getClass()),
                      Types.ASSIGNMENT);
            } catch (Exception e) { /* ignore cast exceptions */ }

            BshArray.setIndex(object, index, val);
        } catch ( UtilTargetError e1 ) { // pass along target error
            if ( IndexOutOfBoundsException.class.isAssignableFrom(e1.getCause().getClass()) )
                throw new UtilEvalError("Error array set index: "+e1.getMessage(), e1);
            throw e1;
        } catch ( Exception e ) {
            throw new UtilEvalError("Assignment: " + e.getMessage(), e);
        } else if ( type == LOOSETYPE_FIELD ) {
            Modifiers mods = new Modifiers(Modifiers.FIELD);
            mods.addModifier("public");
            if ( nameSpace.isInterface )
                mods.setConstant();
            nameSpace.setTypedVariable(varName, Types.getType(val), val, mods);
            return val;
        } else if ( type == MAP_ENTRY ) {
            if ( object instanceof Entry )
                return ((Entry) object).setValue(val);
            // returns a Map.Entry of key and value pair
            return new MapEntry(object, val);
        } else
            throw new InterpreterError("unknown lhs type");
        return val;
    }

    public String toString() {
        return "LHS: "
            +((field!=null)? "field = "+field.toString():"")
            +(varName!=null ? " varName = "+varName: "")
            +(nameSpace!=null ? " nameSpace = "+nameSpace.toString(): "");
    }

    /** Reflect Field is not serializable, hide it.
     * @param s serializer
     * @throws IOException mandatory throwing exception */
    private synchronized void writeObject(final ObjectOutputStream s) throws IOException {
        if ( null != field ) {
            this.object = field.getDeclaringClass();
            this.varName = field.getName();
            this.field = null;
        }
        s.defaultWriteObject();
    }

    /** Fetch field removed from serializer.
     * @param in secializer.
     * @throws IOException mandatory throwing exception
     * @throws ClassNotFoundException mandatory throwing exception  */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if ( null == this.object )
            return;
        Class<?> cls = this.object.getClass();
        if ( this.object instanceof Class )
            cls = (Class<?>) this.object;
        this.field = BshClassManager.memberCache.get(cls).findField(varName);
    }
}
