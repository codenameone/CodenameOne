/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.tools.javac;

/**
 * Constant folding (JLS 15.29) over the boxed representation attribution uses:
 * Integer for int/short/byte, Character for char, Long, Float, Double, Boolean, String.
 */
final class Constants {
    private Constants() {
    }

    static boolean isZero(Object v) {
        if (v instanceof Integer) {
            return ((Integer) v).intValue() == 0;
        }
        if (v instanceof Long) {
            return ((Long) v).longValue() == 0L;
        }
        if (v instanceof Character) {
            return ((Character) v).charValue() == 0;
        }
        return false;
    }

    static int intValue(Object v) {
        if (v instanceof Character) {
            return ((Character) v).charValue();
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return 0;
    }

    private static long longValue(Object v) {
        if (v instanceof Character) {
            return ((Character) v).charValue();
        }
        return ((Number) v).longValue();
    }

    private static float floatValue(Object v) {
        if (v instanceof Character) {
            return ((Character) v).charValue();
        }
        return ((Number) v).floatValue();
    }

    private static double doubleValue(Object v) {
        if (v instanceof Character) {
            return ((Character) v).charValue();
        }
        return ((Number) v).doubleValue();
    }

    private static boolean isNumber(Object v) {
        return v instanceof Number || v instanceof Character;
    }

    /** The constant converted to type t's representation, or null when t is not a constant type. */
    static Object cast(Object v, Type t) {
        if (v == null) {
            return null;
        }
        switch (t.tag) {
            case BOOLEAN:
                return v instanceof Boolean ? v : null;
            case CHAR:
                return isNumber(v) ? Character.valueOf((char) intValue(v)) : null;
            case BYTE:
                return isNumber(v) ? Integer.valueOf((byte) castToInt(v)) : null;
            case SHORT:
                return isNumber(v) ? Integer.valueOf((short) castToInt(v)) : null;
            case INT:
                return isNumber(v) ? Integer.valueOf(castToInt(v)) : null;
            case LONG:
                if (v instanceof Float) {
                    return Long.valueOf((long) ((Float) v).floatValue());
                }
                if (v instanceof Double) {
                    return Long.valueOf((long) ((Double) v).doubleValue());
                }
                return isNumber(v) ? Long.valueOf(longValue(v)) : null;
            case FLOAT:
                if (v instanceof Double) {
                    return Float.valueOf((float) ((Double) v).doubleValue());
                }
                if (v instanceof Long) {
                    return Float.valueOf((float) ((Long) v).longValue());
                }
                return isNumber(v) ? Float.valueOf(floatValue(v)) : null;
            case DOUBLE:
                if (v instanceof Long) {
                    return Double.valueOf((double) ((Long) v).longValue());
                }
                if (v instanceof Float) {
                    return Double.valueOf((double) ((Float) v).floatValue());
                }
                return isNumber(v) ? Double.valueOf(doubleValue(v)) : null;
            case CLASS:
                return v instanceof String ? v : null;
            default:
                return null;
        }
    }

    /** Narrowing to int with Java's float/double rules. */
    private static int castToInt(Object v) {
        if (v instanceof Float) {
            return (int) ((Float) v).floatValue();
        }
        if (v instanceof Double) {
            return (int) ((Double) v).doubleValue();
        }
        if (v instanceof Long) {
            return (int) ((Long) v).longValue();
        }
        return intValue(v);
    }

    static Object unary(Token.Kind op, Object v, Type operand) {
        if (v == null || !isNumber(v)) {
            return null;
        }
        switch (operand.tag) {
            case INT: {
                int x = intValue(v);
                return Integer.valueOf(op == Token.Kind.SUB ? -x : op == Token.Kind.TILDE ? ~x : x);
            }
            case LONG: {
                long x = longValue(v);
                return Long.valueOf(op == Token.Kind.SUB ? -x : op == Token.Kind.TILDE ? ~x : x);
            }
            case FLOAT: {
                float x = floatValue(v);
                return Float.valueOf(op == Token.Kind.SUB ? -x : x);
            }
            case DOUBLE: {
                double x = doubleValue(v);
                return Double.valueOf(op == Token.Kind.SUB ? -x : x);
            }
            default:
                return null;
        }
    }

    static Object binary(Token.Kind op, Object l, Object r, Type operand) {
        if (l == null || r == null) {
            return null;
        }
        if (operand.tag == Type.Tag.BOOLEAN) {
            if (!(l instanceof Boolean) || !(r instanceof Boolean)) {
                return null;
            }
            boolean a = ((Boolean) l).booleanValue();
            boolean b = ((Boolean) r).booleanValue();
            switch (op) {
                case AMPAMP:
                case AMP: return Boolean.valueOf(a && b);
                case BARBAR:
                case BAR: return Boolean.valueOf(a || b);
                case CARET: return Boolean.valueOf(a ^ b);
                case EQEQ: return Boolean.valueOf(a == b);
                case BANGEQ: return Boolean.valueOf(a != b);
                default: return null;
            }
        }
        if (!isNumber(l) || !isNumber(r)) {
            return null;
        }
        switch (operand.tag) {
            case INT: {
                int a = intValue(l);
                int b = op == Token.Kind.LTLT || op == Token.Kind.GTGT || op == Token.Kind.GTGTGT ? (int) longValue(r) : intValue(r);
                switch (op) {
                    case PLUS: return Integer.valueOf(a + b);
                    case SUB: return Integer.valueOf(a - b);
                    case STAR: return Integer.valueOf(a * b);
                    case SLASH: return b == 0 ? null : Integer.valueOf(a / b);
                    case PERCENT: return b == 0 ? null : Integer.valueOf(a % b);
                    case AMP: return Integer.valueOf(a & b);
                    case BAR: return Integer.valueOf(a | b);
                    case CARET: return Integer.valueOf(a ^ b);
                    case LTLT: return Integer.valueOf(a << b);
                    case GTGT: return Integer.valueOf(a >> b);
                    case GTGTGT: return Integer.valueOf(a >>> b);
                    case LT: return Boolean.valueOf(a < b);
                    case GT: return Boolean.valueOf(a > b);
                    case LTEQ: return Boolean.valueOf(a <= b);
                    case GTEQ: return Boolean.valueOf(a >= b);
                    case EQEQ: return Boolean.valueOf(a == b);
                    case BANGEQ: return Boolean.valueOf(a != b);
                    default: return null;
                }
            }
            case LONG: {
                long a = longValue(l);
                long b = longValue(r);
                switch (op) {
                    case PLUS: return Long.valueOf(a + b);
                    case SUB: return Long.valueOf(a - b);
                    case STAR: return Long.valueOf(a * b);
                    case SLASH: return b == 0 ? null : Long.valueOf(a / b);
                    case PERCENT: return b == 0 ? null : Long.valueOf(a % b);
                    case AMP: return Long.valueOf(a & b);
                    case BAR: return Long.valueOf(a | b);
                    case CARET: return Long.valueOf(a ^ b);
                    case LTLT: return Long.valueOf(a << (int) b);
                    case GTGT: return Long.valueOf(a >> (int) b);
                    case GTGTGT: return Long.valueOf(a >>> (int) b);
                    case LT: return Boolean.valueOf(a < b);
                    case GT: return Boolean.valueOf(a > b);
                    case LTEQ: return Boolean.valueOf(a <= b);
                    case GTEQ: return Boolean.valueOf(a >= b);
                    case EQEQ: return Boolean.valueOf(a == b);
                    case BANGEQ: return Boolean.valueOf(a != b);
                    default: return null;
                }
            }
            case FLOAT: {
                float a = floatValue(l);
                float b = floatValue(r);
                switch (op) {
                    case PLUS: return Float.valueOf(a + b);
                    case SUB: return Float.valueOf(a - b);
                    case STAR: return Float.valueOf(a * b);
                    case SLASH: return Float.valueOf(a / b);
                    case PERCENT: return Float.valueOf(a % b);
                    case LT: return Boolean.valueOf(a < b);
                    case GT: return Boolean.valueOf(a > b);
                    case LTEQ: return Boolean.valueOf(a <= b);
                    case GTEQ: return Boolean.valueOf(a >= b);
                    case EQEQ: return Boolean.valueOf(a == b);
                    case BANGEQ: return Boolean.valueOf(a != b);
                    default: return null;
                }
            }
            case DOUBLE: {
                double a = doubleValue(l);
                double b = doubleValue(r);
                switch (op) {
                    case PLUS: return Double.valueOf(a + b);
                    case SUB: return Double.valueOf(a - b);
                    case STAR: return Double.valueOf(a * b);
                    case SLASH: return Double.valueOf(a / b);
                    case PERCENT: return Double.valueOf(a % b);
                    case LT: return Boolean.valueOf(a < b);
                    case GT: return Boolean.valueOf(a > b);
                    case LTEQ: return Boolean.valueOf(a <= b);
                    case GTEQ: return Boolean.valueOf(a >= b);
                    case EQEQ: return Boolean.valueOf(a == b);
                    case BANGEQ: return Boolean.valueOf(a != b);
                    default: return null;
                }
            }
            default:
                return null;
        }
    }

    /** How string concatenation renders a constant of type t. */
    static String stringOf(Object v, Type t) {
        if (t.tag == Type.Tag.CHAR) {
            return String.valueOf((char) intValue(v));
        }
        if (v instanceof Character) {
            if (t.isPrimitive() && t.tag != Type.Tag.CHAR) {
                return String.valueOf((int) ((Character) v).charValue());
            }
            return String.valueOf(((Character) v).charValue());
        }
        if (t.tag == Type.Tag.BYTE || t.tag == Type.Tag.SHORT || t.tag == Type.Tag.INT) {
            return String.valueOf(intValue(v));
        }
        return String.valueOf(v);
    }
}
