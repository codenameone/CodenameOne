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

public class Variable implements Serializable, BshClassManager.Listener
{
    public static final int DECLARATION=0, ASSIGNMENT=1;
    /** A null type means an untyped variable */
    String name;
    Class<?> type = null;
    String typeDescriptor;
    Object value;
    Modifiers modifiers;
    LHS lhs;

    Variable( String name, Class<?> type, LHS lhs )
    {
        this.name = name;
        this.lhs = lhs;
        this.type = type;
    }

    Variable( String name, Object value, Modifiers modifiers )
        throws UtilEvalError
    {
        this( name, (Class<?>) null/*type*/, value, modifiers );
    }

    /**
        This constructor is used in class generation.
    */
    Variable(
        String name, String typeDescriptor, Object value, Modifiers modifiers
    )
        throws UtilEvalError
    {
        this( name, (Class<?>) null/*type*/, value, modifiers );
        this.typeDescriptor = typeDescriptor;
    }

    /**
        @param value may be null if this
    */
    Variable( String name, Class<?> type, Object value, Modifiers modifiers )
        throws UtilEvalError
    {
        this.name=name;
        this.type = type;
        this.setModifiers( modifiers );
        this.setValue( value, DECLARATION );
    }

    /**
        Set the value of the typed variable.
        @param value should be an object or wrapped bsh Primitive type.
        if value is null the appropriate default value will be set for the
        type: e.g. false for boolean, zero for integer types.
    */
    public void setValue( Object value, int context )
        throws UtilEvalError
    {

        // prevent final variable re-assign
        if (hasModifier("final")) {
            if (this.value != null)
                throw new UtilEvalError("Cannot re-assign final variable "+name+".");
            if (value == null)
                return;
        }

        // TODO: should add isJavaCastable() test for strictJava
        // (as opposed to isJavaAssignable())
        if ( type != null && type != Object.class && value != null ) {
            this.value = Types.castObject( value, type,
                context == DECLARATION ? Types.CAST : Types.ASSIGNMENT
            );
            value = this.value;
        }

        this.value = value;

        if ( this.value == null && context != DECLARATION )
            this.value = Primitive.getDefaultValue( type );

        if ( lhs != null )
            this.value = lhs.assign( this.value, false/*strictjava*/ );

    }

    void validateFinalIsSet(boolean isStatic) {
        if (!hasModifier("final") || this.value != null)
            return;
        if (isStatic == hasModifier("static"))
            throw new RuntimeException((isStatic ? "Static f" : "F")
                    +"inal variable "+name+" is not initialized.");
    }

    /*
        Note: UtilEvalError here comes from lhs.getValue().
        A Variable can represent an LHS for the case of an imported class or
        object field.
    */
    Object getValue() throws UtilEvalError {
        if ( lhs != null )
            return type == null ?
                lhs.getValue() : Primitive.wrap( lhs.getValue(), type );

        return value;
    }

    public String getName() { return name; }
    public Class<?> getType() { return type;   }

    public String getTypeDescriptor() {
        if (null == typeDescriptor)
            typeDescriptor = BSHType.getTypeDescriptor(
                type == null ? Object.class : type);
        return typeDescriptor;
    }

    public Modifiers getModifiers() {
        if (modifiers == null)
            this.setModifiers(new Modifiers(Modifiers.FIELD));
        return this.modifiers;
    }

    private void setModifiers(Modifiers modifiers) {
        this.modifiers = modifiers;
    }


    public boolean hasModifier( String name ) {
        return getModifiers().hasModifier(name);
    }

    public void setConstant() {
        if (hasModifier("private") || hasModifier("protected"))
            throw new IllegalArgumentException("Illegal modifier for interface field "
                    + getName() + ". Only public static & final are permitted.");
        getModifiers().setConstant();
    }

    public String toString() {
        return "Variable: " + StringUtil.variableString(this)
                + ", value:" + value + ", lhs = " + lhs;
    }

    /** {@inheritDoc} */
    @Override
    public void classLoaderChanged() {
        if (Reflect.isGeneratedClass(type)) try {
            type = Reflect.getThisNS(type).getClass(type.getName());
        } catch (UtilEvalError e) { /** should not happen on reload */ }
    }
}
