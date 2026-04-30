/** Copyright 2018 Nick nickl- Lombard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package bsh;

import java.util.Arrays;
import java.util.List;

class Operators implements ParserConstants {

    private static final List<Integer> OVERFLOW_OPS
        = Arrays.asList(PLUS, MINUS, STAR, POWER);
    private static final List<Integer> COMPARABLE_OPS
        = Arrays.asList(LT, LTX, GT, GTX, EQ, LE, LEX, GE, GEX, NE);

    /** Constructor private no instance required. */
    private Operators() {}

    private static boolean isShiftOp(int kind) {
        return kind == LSHIFT || kind == LSHIFTX
                || kind == RSIGNEDSHIFT || kind == RSIGNEDSHIFTX
                || kind == RUNSIGNEDSHIFT || kind == RUNSIGNEDSHIFTX;
    }

    /** Binary operations on arbitrary objects.
     * @param lhs left hand side value
     * @param rhs right hand side value
     * @param kind operator type
     * @return operator applied value
     * @throws UtilEvalError evaluation error */
    @SuppressWarnings("unchecked")
    public static Object arbitraryObjectsBinaryOperation(Object lhs, Object rhs, int kind)
            throws UtilEvalError {
        if ( kind == EQ )
            return (lhs == rhs) ? Primitive.TRUE : Primitive.FALSE;
        if ( kind == NE )
            return (lhs != rhs) ? Primitive.TRUE : Primitive.FALSE;

        if ( lhs == Primitive.VOID || rhs == Primitive.VOID )
            throw new UtilEvalError(
                "illegal use of undefined variable, class, or"
                    + " 'void' literal");

        if (kind == SPACESHIP) {
            Object left = Primitive.unwrap(lhs);
            Object right = Primitive.unwrap(rhs);
            int comp;
            if (left == right) {
                comp = 0;
            } else if (left == null) {
                comp = -1;
            } else if (right == null) {
                comp = 1;
            } else if (left instanceof Comparable && left.getClass().isInstance(right)) {
                comp = ((Comparable<Object>) left).compareTo(right);
            } else {
                comp = String.valueOf(left).compareTo(String.valueOf(right));
            }
            return Primitive.wrap(comp < 0 ? -1 : comp > 0 ? 1 : 0, int.class);
        }

        if ( kind == PLUS ) {
            // String concatenation operation
            if ( lhs instanceof String || rhs instanceof String )
                return BSHLiteral.internStrings
                    ? (String.valueOf(lhs) + String.valueOf(rhs)).intern()
                    : String.valueOf(lhs) + String.valueOf(rhs);
            // array concatenation operation
            if ( lhs.getClass().isArray() && rhs instanceof List )
                rhs = ((List<?>) rhs).toArray();
            if ( lhs.getClass().isArray()
                    && rhs.getClass().isArray() )
                return BshArray.concat(lhs, rhs);
            // list concatenation operation
            if ( lhs instanceof List && rhs.getClass().isArray() )
                rhs = Types.castObject(rhs, List.class, Types.CAST);
            if ( lhs instanceof List && rhs instanceof List )
                return BshArray.concat(
                        (List<?>) lhs, (List<?>) rhs);
        }
        if ( kind == STAR ) {
            // array repeat operation
            if ( lhs.getClass().isArray() )
                return BshArray.repeat(lhs,
                    (int) Primitive.castWrapper(int.class, rhs));
            if ( rhs.getClass().isArray() )
                return BshArray.repeat(rhs,
                    (int) Primitive.castWrapper(int.class, lhs));
            // List repeat operation
            if ( lhs instanceof List )
                return BshArray.repeat((List<Object>) lhs,
                    (int) Primitive.castWrapper(int.class, rhs));
            if ( rhs instanceof List )
                return BshArray.repeat((List<Object>) rhs,
                    (int) Primitive.castWrapper(int.class, lhs));
            try {
                // String repeat operation
                if ( lhs instanceof String )
                    return repeatString(String.valueOf(lhs),
                            (int) Primitive.castWrapper(int.class, rhs));
                if ( rhs instanceof String )
                    return repeatString(String.valueOf(rhs),
                            (int) Primitive.castWrapper(int.class, lhs));
            } catch (NegativeArraySizeException e) {
                throw new UtilEvalError("Negative repeat operand: "+e.getMessage(), e);
            }
        }

        if ( lhs instanceof String || rhs instanceof String )
            throw new UtilEvalError(
                "Use of non + operator with String" );
        if ( lhs.getClass().isArray() || rhs.getClass().isArray()
               || lhs instanceof List || rhs instanceof List)
            throw new UtilEvalError(
                "Use of invalid operator " + tokenImage[kind]
                    + " with array or List type" );
        if ( lhs == Primitive.NULL || rhs == Primitive.NULL )
            throw new UtilEvalError(
                "illegal use of null value or 'null' literal");

        throw new UtilEvalError("Operator: " + tokenImage[kind]
                    + " inappropriate for objects");
    }
    /**
    Perform a binary operation on two Primitives or wrapper types.
    If both original args were Primitives return a Primitive result
    else it was mixed (wrapper/primitive) return the wrapper type.
    The exception is for boolean operations where we will return the
    primitive type either way.
    */
    public static Object binaryOperation(Object obj1, Object obj2, int kind)
            throws UtilEvalError {

        // Unwrap primitives
        Object lhs = Primitive.unwrap(obj1);
        Object rhs = Primitive.unwrap(obj2);

        // Java shift semantics: result width follows lhs width. If lhs is int-sized
        // (byte/short/char/int) we must do the shift as an int — promoting to long
        // changes the meaning of `>>>` and `>>` for negative values.
        if (isShiftOp(kind) && Types.isNumeric(lhs) && Types.isNumeric(rhs)
                && !(lhs instanceof Long) && !Types.isFloatingpoint(lhs)) {
            int l = promoteToInteger(lhs).intValue();
            int r = promoteToInteger(rhs).intValue();
            int res;
            switch (kind) {
                case LSHIFT: case LSHIFTX:
                    res = l << r; break;
                case RSIGNEDSHIFT: case RSIGNEDSHIFTX:
                    res = l >> r; break;
                case RUNSIGNEDSHIFT: case RUNSIGNEDSHIFTX:
                    res = l >>> r; break;
                default:
                    throw new UtilEvalError("Unimplemented int shift kind");
            }
            Object result = Integer.valueOf(res);
            if (obj1 instanceof Primitive && obj2 instanceof Primitive)
                return Primitive.shrinkWrap(result);
            return Primitive.shrinkWrap(result).getValue();
        }

        if ( Types.isNumeric(lhs) && Types.isNumeric(rhs) ) {
            Object[] operands = promotePrimitives(lhs, rhs);
            lhs = operands[0];
            rhs = operands[1];
        }

        if ( lhs.getClass() != rhs.getClass() )
            throw new UtilEvalError("Type mismatch in operator.  "
                    + lhs.getClass() + " cannot be used with " + rhs.getClass());

        Object result;
        try {
            result = binaryOperationImpl( lhs, rhs, kind );
        } catch (ArithmeticException e) {
            throw new UtilTargetError("Arithemetic Exception in binary op", e);
        }

        if ( result instanceof Boolean )
            return ((Boolean) result).booleanValue() ? Primitive.TRUE : Primitive.FALSE;

        // If both original args were Primitives return a Primitive result
        // else it was mixed (wrapper/primitive) return the wrapper type
        // Exception is for boolean result, return the primitive
        if ( obj1 instanceof Primitive && obj2 instanceof Primitive )
            return Primitive.shrinkWrap(result);

        return Primitive.shrinkWrap(result).getValue();
    }

    @SuppressWarnings("unchecked")
    static <T> Object binaryOperationImpl( T lhs, T rhs, int kind )
        throws UtilEvalError
    {
        if (kind == SPACESHIP) // compares two non null numbers
            return ((Comparable<T>)lhs).compareTo(rhs);
        if (lhs instanceof Boolean)
            return booleanBinaryOperation( (Boolean) lhs, (Boolean) rhs, kind );
        if (COMPARABLE_OPS.contains(kind))
            return comparableBinaryBooleanOperations((Comparable<T>) lhs, rhs, kind);
        if (Types.isFloatingpoint(lhs))
            return doubleBinaryOperation( (Double) lhs, (Double) rhs, kind );
        if (lhs instanceof Number)
            return longBinaryOperation( (Long) lhs, (Long) rhs, kind );
        throw new UtilEvalError("Invalid types in binary operator" );
    }

    static Boolean booleanBinaryOperation(Boolean B1, Boolean B2, int kind)
    {
        boolean lhs = B1.booleanValue();
        boolean rhs = B2.booleanValue();

        switch(kind)
        {
            case EQ:
                return lhs == rhs;

            case NE:
                return lhs != rhs;

            case BOOL_OR:
            case BOOL_ORX:
                // already evaluated lhs TRUE
                // see BSHBinaryExpression
                return false || rhs;

            case BOOL_AND:
            case BOOL_ANDX:
                // already evaluated lhs FALSE
                // see BSHBinaryExpression
                return true && rhs;

            case BIT_AND:
            case BIT_ANDX:
                return lhs & rhs;

            case BIT_OR:
            case BIT_ORX:
                return lhs | rhs;

            case XOR:
            case XORX:
                return lhs ^ rhs;

        }
        throw new InterpreterError("unimplemented binary operator");
    }

    static <T> Boolean comparableBinaryBooleanOperations(Comparable<T> lhs, T rhs, int kind) {
        switch(kind)
        {
            // boolean
            case LT:
            case LTX:
                return lhs.compareTo(rhs) < 0;

            case GT:
            case GTX:
                return lhs.compareTo(rhs) > 0;

            case LE:
            case LEX:
                return lhs.compareTo(rhs) <= 0;

            case GE:
            case GEX:
                return lhs.compareTo(rhs) >= 0;

            case NE:
                return lhs.compareTo(rhs) != 0;

            case EQ:
            default:
                return lhs.compareTo(rhs) == 0;
        }
    }

    // returns Object covering both Long and Boolean return types
    static Object longBinaryOperation(long lhs, long rhs, int kind)
    {
        switch(kind)
        {
            // arithmetic
            case PLUS:
                if ( lhs > 0 && (Long.MAX_VALUE - lhs) < rhs )
                    break;
                return lhs + rhs;

            case MINUS:
                if ( lhs < 0 && (Long.MIN_VALUE - lhs) > -rhs )
                    break;
                return lhs - rhs;

            case STAR:
                if ( lhs != 0 && Long.MAX_VALUE / lhs < rhs )
                    break;
                return lhs * rhs;

            case SLASH:
                return lhs / rhs;

            case MOD:
            case MODX:
                return lhs % rhs;

            case POWER:
            case POWERX:
                return powLong(lhs, rhs);

            // bitwise
            case LSHIFT:
            case LSHIFTX:
                return lhs << rhs;

            case RSIGNEDSHIFT:
            case RSIGNEDSHIFTX:
                return lhs >> rhs;

            case RUNSIGNEDSHIFT:
            case RUNSIGNEDSHIFTX:
                return lhs >>> rhs;

            case BIT_AND:
            case BIT_ANDX:
                return lhs & rhs;

            case BIT_OR:
            case BIT_ORX:
                return lhs | rhs;

            case XOR:
            case XORX:
                return lhs ^ rhs;

        }
        throw new InterpreterError(
                "Unimplemented binary long operator");
    }

    // returns Object covering both Double and Boolean return types
    static Object doubleBinaryOperation(double lhs, double rhs, int kind)
        throws UtilEvalError
    {
        switch(kind)
        {
            // arithmetic
            case PLUS:
                if ( lhs > 0d && (Double.MAX_VALUE - lhs) < rhs )
                    break;
                return lhs + rhs;

            case MINUS:
                if ( lhs < 0d && (-Double.MAX_VALUE - lhs) > -rhs )
                    break;
                return lhs - rhs;

            case STAR:
                if ( lhs != 0 && Double.MAX_VALUE / lhs < rhs )
                    break;
                return lhs * rhs;

            case SLASH:
                return lhs / rhs;

            case MOD:
            case MODX:
                return lhs % rhs;

            case POWER:
            case POWERX:
                double check = powDouble(lhs, rhs);
                if ( Double.isInfinite(check) )
                    break;
                return check;

            // can't shift floating-point values
            case LSHIFT:
            case LSHIFTX:
            case RSIGNEDSHIFT:
            case RSIGNEDSHIFTX:
            case RUNSIGNEDSHIFT:
            case RUNSIGNEDSHIFTX:
                throw new UtilEvalError("Can't shift floatingpoint values");

        }
        throw new InterpreterError(
                "Unimplemented binary double operator");
    }

    /**
        Promote primitive wrapper type to Integer wrapper type
    */
    static Number promoteToInteger(Object wrapper )
    {
        if ( wrapper instanceof Character )
            return Integer.valueOf(((Character) wrapper).charValue());
        if ( wrapper instanceof Byte || wrapper instanceof Short )
            return Integer.valueOf(((Number) wrapper).intValue());

        return (Number) wrapper;
    }

    /**
        Promote the pair of primitives to the maximum type of the two.
        e.g. [int,long]->[long,long]
    */
    static Object[] promotePrimitives(Object lhs, Object rhs)
    {
        Number lnum = promoteToInteger(lhs);
        Number rnum = promoteToInteger(rhs);

        if ( Types.isFloatingpoint(lhs) || Types.isFloatingpoint(rhs)) {
            if ( !(lhs instanceof Double) )
                lhs = Double.valueOf(lnum.doubleValue());
            if ( !(rhs instanceof Double) )
                rhs = Double.valueOf(rnum.doubleValue());
        } else {
            if ( !(lhs instanceof Long) )
                lhs = Long.valueOf(lnum.longValue());
            if ( !(rhs instanceof Long) )
                rhs = Long.valueOf(rnum.longValue());
        }

        return new Object[] { lhs, rhs };
    }

    public static Primitive unaryOperation(Primitive val, int kind)
        throws UtilEvalError
    {
        if (val == Primitive.NULL)
            throw new UtilEvalError(
                "illegal use of null object or 'null' literal");
        if (val == Primitive.VOID)
            throw new UtilEvalError(
                "illegal use of undefined object or 'void' literal");

        Class<?> operandType = val.getType();

        if ( operandType == Boolean.class )
            return booleanUnaryOperation((Boolean) val.getValue(), kind)
                ? Primitive.TRUE : Primitive.FALSE;

        Number operand = promoteToInteger(val.getValue());
        if(operand instanceof Integer)
        {
            int result = intUnaryOperation((Integer) operand, kind);

            // ++ and -- must be cast back the original type
            if(kind == INCR || kind == DECR)
            {
                if(operandType == byte.class)
                    return new Primitive((byte) result);
                if(operandType == Short.class)
                    return new Primitive((short) result);
                if(operandType == Character.class)
                    return new Primitive((char) result);
            }

            return new Primitive(result);
        }
        if(operand instanceof Long)
            return new Primitive(longUnaryOperation(operand.longValue(), kind));
        if(operand instanceof Float)
            return new Primitive(floatUnaryOperation(operand.floatValue(), kind));
        if(operand instanceof Double)
            return new Primitive(doubleUnaryOperation(operand.doubleValue(), kind));
        throw new InterpreterError(
            "An error occurred.  Please call technical support.");
    }

    static boolean booleanUnaryOperation(Boolean B, int kind)
        throws UtilEvalError
    {
        boolean operand = B.booleanValue();
        switch(kind)
        {
            case BANG:
                return !operand;
        }
        throw new UtilEvalError("Operator inappropriate for boolean");
    }

    static int intUnaryOperation(Integer I, int kind)
    {
        int operand = I.intValue();

        switch(kind)
        {
            case PLUS:
                return operand;
            case MINUS:
                return -operand;
            case TILDE:
                return ~operand;
            case INCR:
                return operand + 1;
            case DECR:
                return operand - 1;
        }
        throw new InterpreterError("bad integer unaryOperation");
    }

    static long longUnaryOperation(Long L, int kind)
    {
        long operand = L.longValue();

        switch(kind)
        {
            case PLUS:
                return operand;
            case MINUS:
                return -operand;
            case TILDE:
                return ~operand;
            case INCR:
                return operand + 1;
            case DECR:
                return operand - 1;
        }
        throw new InterpreterError("bad long unaryOperation");
    }

    static float floatUnaryOperation(Float F, int kind)
    {
        float operand = F.floatValue();

        switch(kind)
        {
            case PLUS:
                return operand;
            case MINUS:
                return -operand;
            case INCR:
                return operand + 1;
            case DECR:
                return operand - 1;
        }
        throw new InterpreterError("bad float unaryOperation");
    }

    static double doubleUnaryOperation(Double D, int kind)
    {
        double operand = D.doubleValue();

        switch(kind)
        {
            case PLUS:
                return operand;
            case MINUS:
                return -operand;
            case INCR:
                return operand + 1;
            case DECR:
                return operand - 1;
        }
        throw new InterpreterError("bad double unaryOperation");
    }

    private static String repeatString(String value, int count) {
        if (count < 0) {
            throw new NegativeArraySizeException(String.valueOf(count));
        }
        StringBuilder out = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            out.append(value);
        }
        String repeated = out.toString();
        return BSHLiteral.internStrings ? repeated.intern() : repeated;
    }

    private static long powLong(long lhs, long rhs) {
        if (rhs < 0) {
            return 0L;
        }
        long result = 1L;
        long base = lhs;
        long exp = rhs;
        while (exp > 0) {
            if ((exp & 1L) == 1L) {
                result *= base;
            }
            exp >>= 1;
            if (exp > 0) {
                base *= base;
            }
        }
        return result;
    }

    private static double powDouble(double lhs, double rhs) {
        if (rhs == 0d) {
            return 1d;
        }
        if (rhs < 0d) {
            return 1d / powDouble(lhs, -rhs);
        }
        long whole = (long) rhs;
        if (rhs == whole) {
            double result = 1d;
            double base = lhs;
            long exp = whole;
            while (exp > 0) {
                if ((exp & 1L) == 1L) {
                    result *= base;
                }
                exp >>= 1;
                if (exp > 0) {
                    base *= base;
                }
            }
            return result;
        }
        return Double.POSITIVE_INFINITY;
    }
}
