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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Wrapper for primitive types in Bsh.  This is package public because it
 * is used in the implementation of some bsh commands.</p>
 *
 * <p>See the note in LHS.java about wrapping objects.</p>
 *
 * @author Pat Niemeyer
 * @author Daniel Leuck
 */
public final class Primitive implements Serializable {
    /*
    Note: this class is final because we may test == Primitive.class in places.
    If we need to change that search for those tests.
    */
    /** default serial version id */
    private static final long serialVersionUID = 1L;

    static final Map<Class<?>, Class<?>> wrapperMap = new HashMap<>();
    static {
        wrapperMap.put( Boolean.class, Boolean.class );
        wrapperMap.put( byte.class, Byte.class );
        wrapperMap.put( Short.class, Short.class );
        wrapperMap.put( Character.class, Character.class );
        wrapperMap.put( int.class, Integer.class );
        wrapperMap.put( long.class, Long.class );
        wrapperMap.put( Float.class, Float.class );
        wrapperMap.put( double.class, Double.class );
        wrapperMap.put( Boolean.class, Boolean.class );
        wrapperMap.put( Byte.class, byte.class );
        wrapperMap.put( Short.class, Short.class );
        wrapperMap.put( Character.class, Character.class );
        wrapperMap.put( Integer.class, int.class );
        wrapperMap.put( Long.class, long.class );
        wrapperMap.put( Float.class, Float.class );
        wrapperMap.put( Double.class, double.class );
    }

    /** The primitive value stored in its java.lang wrapper class */
    private Object value;

    public static final Primitive TRUE = new Primitive(true);
    public static final Primitive FALSE = new Primitive(false);

    /** Default zero value constants */
    public static final Primitive ZERO_CHAR = new Primitive('\000');
    public static final Primitive ZERO_BYTE = new Primitive((byte) 0);
    public static final Primitive ZERO_SHORT = new Primitive((short) 0);
    public static final Primitive ZERO_INT = new Primitive(0);
    public static final Primitive ZERO_LONG = new Primitive(0L);
    public static final Primitive ZERO_FLOAT = new Primitive(0f);
    public static final Primitive ZERO_DOUBLE = new Primitive(0d);
    private enum Special { NULL_VALUE, VOID_TYPE }

    /*
        NULL means "no value".
        This ia a placeholder for primitive null value.
    */
    public static final Primitive NULL = new Primitive(Special.NULL_VALUE);

    /**
        VOID means "no type".
        Strictly speaking, this makes no sense here.  But for practical
        reasons we'll consider the lack of a type to be a special value.
    */
    public static final Primitive VOID = new Primitive(Special.VOID_TYPE);

    private Object readResolve() throws ObjectStreamException
    {
        if (value == Special.NULL_VALUE)
            return Primitive.NULL;
        return this;
    }

    // private to prevent invocation with param that isn't a primitive-wrapper
    private Primitive( Object value )
    {
        if ( value == null )
            throw new InterpreterError(
                "Use Primitve.NULL instead of Primitive(null)");

        this.value = value;
    }

    public Primitive(boolean value) { this(value ? Boolean.TRUE : Boolean.FALSE); }
    public Primitive(byte value) { this(Byte.valueOf(value)); }
    public Primitive(short value) { this(Short.valueOf(value)); }
    public Primitive(char value) { this(Character.valueOf(value)); }
    public Primitive(int value) { this(Integer.valueOf(value)); }
    public Primitive(long value) { this(Long.valueOf(value)); }
    public Primitive(float value) { this(Float.valueOf(value)); }
    public Primitive(double value) { this(Double.valueOf(value)); }
    /**
        Return the primitive value stored in its java.lang wrapper class
    */
    public Object getValue()
    {
        if ( value == Special.NULL_VALUE )
            return null;
        if ( value == Special.VOID_TYPE )
                throw new InterpreterError("attempt to unwrap void type");
        return value;
    }

    public String toString()
    {
        if(value == Special.NULL_VALUE)
            return "null";
        if(value == Special.VOID_TYPE)
            return "void";
        return value.toString();
    }

    /**
        Get the corresponding Java primitive TYPE class for this Primitive.
        @return the primitive TYPE class type of the value or Void.TYPE for
        Primitive.VOID or null value for type of Primitive.NULL
    */
    public Class<?> getType()
    {
        if ( this == Primitive.VOID )
            return Void.TYPE;

        // NULL return null as type... we currently use null type to indicate
        // loose typing throughout bsh.
        if ( this == Primitive.NULL )
            return null;

        return unboxType( value.getClass() );
    }

    public static Primitive shrinkWrap(Object number) {
        if (!(number instanceof Number))
            throw new InterpreterError("Can only shrink wrap Number types");
        Number value = (Number) number;
        if ( Types.isFloatingpoint(number) )
            return new Primitive(value.doubleValue());
        long longValue = value.longValue();
        if ( longValue <= Integer.MAX_VALUE && longValue >= Integer.MIN_VALUE )
            return new Primitive((int) longValue);
        return new Primitive(longValue);
    }

    /**
        Determine if this primitive is a numeric type.
        i.e. not boolean, null, or void (but including char)
    */
    public boolean isNumber() {
        return !(value instanceof Boolean)
            && this != NULL && this != VOID
            && Types.isNumeric(value);
    }

    public Number numberValue() {
        Object value = this.value;

        // Promote character to Number type for these purposes
        if (value instanceof Character)
            value = Integer.valueOf(((Character)value).charValue());

        if (value instanceof Number)
            return (Number) value;

        if (value instanceof Boolean)
            return (Boolean) value ? 1 : 0;

        throw new InterpreterError("Primitive not a number");
    }

    /**
        Primitives compare equal with other Primitives containing an equal
        wrapped value.
    */
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof Primitive ) )
            if ( wrapperMap.containsKey(obj.getClass()) )
                obj = new Primitive(obj);
            else
                return false;
        Primitive pobj = (Primitive) obj;
        if ( pobj.isNumber() && this.isNumber() ) {
            if ( Types.isFloatingpoint(this.value) || Types.isFloatingpoint(pobj.value) )
                return this.numberValue().doubleValue() == pobj.numberValue().doubleValue();
            return this.numberValue().longValue() == pobj.numberValue().longValue();
        }
        return this.value.equals( pobj.value );
    }

    /**
        The hash of the Primitive is tied to the hash of the wrapped value but
        shifted so that they are not the same.
    */
    public int hashCode()
    {
        return this.value.hashCode() * 21; // arbitrary
    }

    /**
        Unwrap primitive values and map voids to nulls.
        Non Primitive types remain unchanged.

        @param obj object type which may be bsh.Primitive
        @return corresponding "normal" Java type, "unwrapping"
            any bsh.Primitive types to their wrapper types.
    */
    public static Object unwrap( Object obj )
    {
        // map voids to nulls for the outside world
        if (obj == Primitive.VOID)
            return null;

        // unwrap primitives
        if (obj instanceof Primitive)
            return((Primitive)obj).getValue();

        return obj;
    }

    /*
        Unwrap Primitive wrappers to their java.lang wrapper values.
        e.g. Primitive(42) becomes Integer(42)
        @see #unwrap( Object )
    */
    public static Object [] unwrap( Object[] args )
    {
        if ( args == null )
            return null;
        Object [] oa = new Object[ args.length ];
        for(int i=0; i<args.length; i++)
            oa[i] = unwrap( args[i] );
        return oa;
    }

    /*
    */
    public static Object [] wrap( Object[] args, Class<?> [] paramTypes )
    {
        if ( args == null )
            return null;
        Object [] oa = new Object[ args.length ];
        for(int i=0; i<args.length; i++)
            oa[i] = wrap( args[i], paramTypes[i] );
        return oa;
    }

    /**
        Wrap primitive values (as indicated by type param) and nulls in the
        Primitive class.  Values not primitive or null are left unchanged.
        Primitive values are represented by their wrapped values in param value.
        <p/>
        The value null is mapped to Primitive.NULL.
        Any value specified with type Void.TYPE is mapped to Primitive.VOID.
    */
    public static Object wrap(
        Object value, Class<?> type )
    {
        if ( type == Void.TYPE )
            return Primitive.VOID;

        if ( value == null )
            return Primitive.NULL;

        if (type == null)
            return value;

        if (Types.isPrimitive(type))
            if ( value instanceof Boolean )
                return ((Boolean) value).booleanValue()
                    ? Primitive.TRUE : Primitive.FALSE;

            else if ( isWrapperType( value.getClass() ) )
                return new Primitive( value );

        return value;
    }


    /**
        Get the appropriate default value per JLS 4.5.4
    */
    public static Primitive getDefaultValue( Class<?> type )
    {
        if ( type == null )
            return Primitive.NULL;
        if ( Boolean.class == type )
            return Primitive.FALSE;
        if ( Character.class == type )
            return Primitive.ZERO_CHAR;
        if ( byte.class == type || Byte.class == type )
            return Primitive.ZERO_BYTE;
        if ( Short.class == type )
            return Primitive.ZERO_SHORT;
        if ( int.class == type || Integer.class == type )
            return Primitive.ZERO_INT;
        if ( long.class == type || Long.class == type )
            return Primitive.ZERO_LONG;
        if ( Float.class == type )
            return Primitive.ZERO_FLOAT;
        if ( double.class == type || Double.class == type )
            return Primitive.ZERO_DOUBLE;
        return Primitive.NULL;
    }

    /**
        Get the corresponding java.lang wrapper class for the primitive TYPE
        class.
        e.g.  Integer.TYPE -> Integer.class
    */
    public static Class<?> boxType( Class<?> primitiveType )
    {
        Class<?> c = wrapperMap.get( primitiveType );
        if ( c != null && !c.isPrimitive() )
            return c;
        throw new InterpreterError(
            "Not a primitive type: "+ primitiveType );
    }

    /**
        Get the corresponding primitive TYPE class for the java.lang wrapper
        class type.
        e.g.  Integer.class -> Integer.TYPE
    */
    public static Class<?> unboxType( Class<?> wrapperType )
    {
        Class<?> c = wrapperMap.get( wrapperType );
        if ( c != null && (c.isPrimitive() || c == wrapperType) )
            return c;
        throw new InterpreterError(
            "Not a primitive wrapper type: "+wrapperType );
    }

    /**
        Cast this bsh.Primitive value to a new bsh.Primitive value
        This is usually a numeric type cast.  Other cases include:
            A boolean can be cast to boolen
            null can be cast to any object type and remains null
            Attempting to cast a void causes an exception
        @param toType is the java object or primitive TYPE class
    */
    public Primitive castToType( Class<?> toType, int operation )
        throws UtilEvalError
    {
        return castPrimitive(
            toType, getType()/*fromType*/, this/*fromValue*/,
            false/*checkOnly*/, operation );
    }

    /*
        Cast or check a cast of a primitive type to another type.
        Normally both types are primitive (e.g. numeric), but a null value
        (no type) may be cast to any type.
        <p/>

        @param toType is the target type of the cast.  It is normally a
        java primitive TYPE, but in the case of a null cast can be any object
        type.

        @param fromType is the java primitive TYPE type of the primitive to be
        cast or null, to indicate that the fromValue was null or void.

        @param fromValue is, optionally, the value to be converted.  If
        checkOnly is true fromValue must be null.  If checkOnly is false,
        fromValue must be non-null (Primitive.NULL is of course valid).
    */
    static Primitive castPrimitive( Class<?> toType, Class<?> fromType,
            Primitive fromValue, boolean checkOnly, int operation )
            throws UtilEvalError {
        // can't cast void to anything
        if ( fromType == Void.TYPE )
            if ( checkOnly )
                return Types.INVALID_CAST;
            else
                throw Types.castError( StringUtil.typeString(toType),
                    "void value", operation );

        // Do numeric cast
        if ( !checkOnly && fromValue.isNumber() && Types.isNumeric(toType) )
            return new Primitive( castNumber(toType, fromValue.numberValue()) );

        if ( toType.isPrimitive() ) {
            // Cast null value to primitive default value
            if ( fromType == null && !Primitive.VOID.equals(fromValue) )
                return checkOnly ? Types.VALID_CAST : getDefaultValue(toType);
            if (toType == Boolean.class)
                return checkOnly ? Types.VALID_CAST : new Primitive( castWrapper(toType, fromValue) );
        } else {
            // Trying to cast primitive to an object type
            // Primitive.NULL can be cast to any object type
            if ( fromType == null )
                return checkOnly ? Types.VALID_CAST :
                    Primitive.NULL;

            if ( checkOnly )
                return Types.INVALID_CAST;

            throw Types.castError(
                    "object type " + toType.getName(), "primitive value", operation);
        }

        // can only cast boolean to boolean
        if ( checkOnly && fromType == Boolean.class ) {
            if ( toType != Boolean.class )
                return Types.INVALID_CAST;

            return Types.VALID_CAST;
        }

        // Only allow legal Java assignment unless we're a CAST operation
        if ( operation == Types.ASSIGNMENT
            && !Types.isJavaAssignable( toType, fromType ) ) {
            if ( checkOnly )
                return Types.INVALID_CAST;
        }

        return checkOnly ? Types.VALID_CAST :
            new Primitive( castWrapper(toType, fromValue) );
    }

    public static boolean isWrapperType( Class<?> type )
    {
        return null != type && wrapperMap.containsKey( type ) && !type.isPrimitive();
    }

    /**
        Cast a primitive value represented by its java.lang wrapper type to the
        specified java.lang wrapper type.  e.g.  Byte(5) to Integer(5) or
        Integer(5) to Byte(5)
        @param toType is the java TYPE type
        @param value is the value in java.lang wrapper.
        value may not be null.
    */
    static Object castWrapper( Class<?> toType, Object value ) {
        if ( Primitive.VOID.equals(value) )
            return value;

        value = Primitive.unwrap(value);

        if ( !(Primitive.isWrapperType(toType) || toType.isPrimitive()) )
            throw new InterpreterError("invalid type in castWrapper: "+toType);

        // first promote char to Number type to avoid duplicating code
        if ( value instanceof Character )
            value = Integer.valueOf(((Character)value).charValue());

        if ( toType == Boolean.class ) {
            if ( value instanceof Boolean )
                return value;
            else if ( value instanceof String )
                return !"".equals(String.valueOf(value));
            else if ( value instanceof Number )
                return ((Number) value).intValue() != 0;
            else
                return value != null;
        }

        if ( value == null && toType.isPrimitive() )
            value = Primitive.unwrap(getDefaultValue(toType));

        if ( value instanceof String ) try {
            value = Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException nfe) {
            throw new InterpreterError("cannot cast string \""+value+"\" to number", nfe);
        }

        if ( value instanceof Boolean )
            value = (Boolean) value ? 1 : 0;

        if ( !(value instanceof Number) )
            throw new InterpreterError("bad type in cast "+StringUtil.typeValueString(value));

        return castNumber(toType, (Number) value);
    }

    static Object castNumber(Class<?> toType, Number number) {
        if (toType.isInstance(number) || toType == unboxType(number.getClass()))
            return number;
        if ((toType == Byte.class || toType == byte.class)
                && number.shortValue() <= 0xff && number.shortValue() >= Byte.MIN_VALUE)
            return number.byteValue();
        if (toType == Short.class
                && number.intValue() <= Short.MAX_VALUE && number.intValue() >= Short.MIN_VALUE)
            return number.shortValue();
        if (toType == Character.class
                && number.intValue() <= (int) Character.MAX_VALUE && number.intValue() >= (int) Character.MIN_VALUE)
            return (char) number.intValue();
        if ((toType == Integer.class || toType == int.class)
                && number.longValue() <= Integer.MAX_VALUE && number.longValue() >= Integer.MIN_VALUE)
            return number.intValue();
        if (toType == Float.class
                && !Float.isInfinite(number.floatValue()))
            return number.floatValue();
        if ((toType == Double.class || toType == double.class)
                && !Double.isInfinite(number.doubleValue()))
            return number.doubleValue();
        if (toType == Long.class || toType == long.class)
            return number.longValue();
        throw new InterpreterError("cannot assign number "+number+" to type "+toType.getSimpleName());
    }

    static Object castNumberStrictJava(Class<?> toType, Number number) {
        if (toType == Byte.class || toType == byte.class)
            return number.byteValue();
        if (toType == Short.class)
            return number.shortValue();
        if (toType == Character.class)
            return (char) number.intValue();
        if (toType == Integer.class || toType == int.class)
            return number.intValue();
        if (toType == Long.class || toType == long.class)
            return number.longValue();
        if (toType == Float.class)
            return number.floatValue();
        if (toType == Double.class || toType == double.class)
            return number.doubleValue();
        return castNumber(toType, number);
    }

}
