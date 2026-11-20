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
package com.codename1.spreadsheet;

/// The engineering functions: number bases, bit operations, the error function and unit
/// conversion. The complex number functions are not here.
final class EngineeringFunctions {
    static final String SCALAR_NAMES = "BIN2DEC BIN2HEX BIN2OCT BITAND BITLSHIFT BITOR BITRSHIFT BITXOR CONVERT DEC2BIN"
            + " DEC2HEX DEC2OCT DELTA ERF ERF.PRECISE ERFC ERFC.PRECISE GESTEP HEX2BIN HEX2DEC HEX2OCT OCT2BIN OCT2DEC"
            + " OCT2HEX";

    static final String RANGE_NAMES = "";

    private static final double MAX_BITS = 281474976710655d;

    /// Units as name, group and the factor to the group's base unit, one after the other.
    private static final String[] UNITS = {
        "g", "mass", "1", "kg", "mass", "1000", "sg", "mass", "14593.9029372064", "lbm", "mass", "453.59237",
        "u", "mass", "1.66053906660e-24", "ozm", "mass", "28.349523125", "grain", "mass", "0.06479891",
        "stone", "mass", "6350.29318", "ton", "mass", "907184.74", "uk_ton", "mass", "1016046.9088",
        "m", "length", "1", "km", "length", "1000", "cm", "length", "0.01", "mm", "length", "0.001",
        "mi", "length", "1609.344", "Nmi", "length", "1852", "in", "length", "0.0254", "ft", "length", "0.3048",
        "yd", "length", "0.9144", "ang", "length", "1e-10", "ly", "length", "9.4607304725808e15",
        "sec", "time", "1", "s", "time", "1", "min", "time", "60", "mn", "time", "60", "hr", "time", "3600",
        "day", "time", "86400", "d", "time", "86400", "yr", "time", "31557600",
        "Pa", "pressure", "1", "p", "pressure", "1", "atm", "pressure", "101325", "at", "pressure", "101325",
        "mmHg", "pressure", "133.322", "psi", "pressure", "6894.75729316836", "Torr", "pressure", "133.322368421053",
        "N", "force", "1", "dyn", "force", "1e-5", "dy", "force", "1e-5", "lbf", "force", "4.4482216152605",
        "J", "energy", "1", "kJ", "energy", "1000", "e", "energy", "1e-7", "c", "energy", "4.184", "cal", "energy",
        "4.1868", "eV", "energy", "1.602176634e-19", "ev", "energy", "1.602176634e-19", "HPh", "energy",
        "2684519.53769617", "hh", "energy", "2684519.53769617", "Wh", "energy", "3600", "wh", "energy", "3600",
        "kWh", "energy", "3600000", "flb", "energy", "1.3558179483314", "BTU", "energy", "1055.05585262",
        "btu", "energy", "1055.05585262",
        "W", "power", "1", "w", "power", "1", "kW", "power", "1000", "HP", "power", "745.69987158227",
        "h", "power", "745.69987158227", "PS", "power", "735.49875",
        "l", "volume", "1", "L", "volume", "1", "lt", "volume", "1", "ml", "volume", "0.001", "tsp", "volume",
        "0.00492892159375", "tbs", "volume", "0.01478676478125", "oz", "volume", "0.0295735295625", "cup", "volume",
        "0.2365882365", "pt", "volume", "0.473176473", "us_pt", "volume", "0.473176473", "uk_pt", "volume",
        "0.56826125", "qt", "volume", "0.946352946", "gal", "volume", "3.785411784", "uk_gal", "volume", "4.54609",
        "m3", "volume", "1000", "m^3", "volume", "1000", "ft3", "volume", "28.316846592", "ft^3", "volume",
        "28.316846592", "in3", "volume", "0.016387064", "in^3", "volume", "0.016387064",
        "m2", "area", "1", "m^2", "area", "1", "ha", "area", "10000", "ft2", "area", "0.09290304", "ft^2", "area",
        "0.09290304", "in2", "area", "0.00064516", "in^2", "area", "0.00064516", "mi2", "area", "2589988.110336",
        "mi^2", "area", "2589988.110336", "yd2", "area", "0.83612736", "yd^2", "area", "0.83612736", "uk_acre",
        "area", "4046.8564224", "us_acre", "area", "4046.87260987425",
        "bit", "data", "1", "byte", "data", "8", "kbit", "data", "1000", "kbyte", "data", "8000",
        "Mbyte", "data", "8000000", "Gbyte", "data", "8000000000", "kibyte", "data", "8192", "Mibyte", "data",
        "8388608", "Gibyte", "data", "8589934592",
        "m/s", "speed", "1", "m/sec", "speed", "1", "m/h", "speed", "0.000277777777777778", "m/hr", "speed",
        "0.000277777777777778", "mph", "speed", "0.44704", "kn", "speed", "0.514444444444444", "km/h", "speed",
        "0.277777777777778", "km/hr", "speed", "0.277777777777778",
        "T", "magnetism", "1", "ga", "magnetism", "0.0001",
    };

    private EngineeringFunctions() {
    }

    private static Double d(double v) {
        return Double.valueOf(v);
    }

    /// Reads a number of up to ten digits in a base; a tenth digit at or above half the base
    /// makes it negative, two's complement.
    private static double parse(Object arg, int radix) {
        String s = arg instanceof Double ? Values.general(((Double) arg).doubleValue()) : Values.text(arg);
        if (s.length() > 10) {
            throw EvalError.NUM;
        }
        double v = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int digit;
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            } else if (c >= 'a' && c <= 'f') {
                digit = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'F') {
                digit = c - 'A' + 10;
            } else {
                throw EvalError.NUM;
            }
            if (digit >= radix) {
                throw EvalError.NUM;
            }
            v = v * radix + digit;
        }
        double full = 1;
        for (int i = 0; i < 10; i++) {
            full *= radix;
        }
        if (s.length() == 10 && v >= full / 2) {
            v -= full;
        }
        return v;
    }

    private static String print(double value, int radix, Object[] a, int placesIndex) {
        double v = value < 0 ? Math.ceil(value) : Math.floor(value);
        double full = 1;
        for (int i = 0; i < 10; i++) {
            full *= radix;
        }
        if (v >= full / 2 || v < -full / 2) {
            throw EvalError.NUM;
        }
        boolean negative = v < 0;
        long bits = (long) (negative ? v + full : v);
        StringBuilder sb = new StringBuilder();
        do {
            int digit = (int) (bits % radix);
            sb.insert(0, (char) (digit < 10 ? '0' + digit : 'A' + digit - 10));
            bits /= radix;
        } while (bits > 0);
        if (!negative && Evaluator.has(a, placesIndex)) {
            int places = Evaluator.integer(a, placesIndex);
            if (places < sb.length() || places > 10) {
                throw EvalError.NUM;
            }
            while (sb.length() < places) {
                sb.insert(0, '0');
            }
        }
        return sb.toString();
    }

    private static long bits(Object[] a, int i) {
        double v = Evaluator.num(a, i);
        if (v < 0 || v > MAX_BITS || v != Math.floor(v)) {
            throw EvalError.NUM;
        }
        return (long) v;
    }

    static Object call(String name, Evaluator ev, Object[] a) {
        switch (Functions.index(SWITCH_1, name)) {
            case 0: // BIN2DEC
                return d(parse(Evaluator.arg(a, 0), 2));
            case 1: // OCT2DEC
                return d(parse(Evaluator.arg(a, 0), 8));
            case 2: // HEX2DEC
                return d(parse(Evaluator.arg(a, 0), 16));
            case 3: // BIN2OCT
                return print(parse(Evaluator.arg(a, 0), 2), 8, a, 1);
            case 4: // BIN2HEX
                return print(parse(Evaluator.arg(a, 0), 2), 16, a, 1);
            case 5: // OCT2BIN
                return print(parse(Evaluator.arg(a, 0), 8), 2, a, 1);
            case 6: // OCT2HEX
                return print(parse(Evaluator.arg(a, 0), 8), 16, a, 1);
            case 7: // HEX2BIN
                return print(parse(Evaluator.arg(a, 0), 16), 2, a, 1);
            case 8: // HEX2OCT
                return print(parse(Evaluator.arg(a, 0), 16), 8, a, 1);
            case 9: // DEC2BIN
                return print(Evaluator.num(a, 0), 2, a, 1);
            case 10: // DEC2OCT
                return print(Evaluator.num(a, 0), 8, a, 1);
            case 11: // DEC2HEX
                return print(Evaluator.num(a, 0), 16, a, 1);
            case 12: // BITAND
                return d(bits(a, 0) & bits(a, 1));
            case 13: // BITOR
                return d(bits(a, 0) | bits(a, 1));
            case 14: // BITXOR
                return d(bits(a, 0) ^ bits(a, 1));
            case 15: // BITLSHIFT
            case 16: { // BITRSHIFT
                long v = bits(a, 0);
                int shift = Evaluator.integer(a, 1);
                if (shift < -53 || shift > 53) {
                    throw EvalError.NUM;
                }
                if ("BITRSHIFT".equals(name)) {
                    shift = -shift;
                }
                double r = shift >= 0 ? (double) v * Values.pow2(shift) : (double) (v >> -shift);
                if (r > MAX_BITS) {
                    throw EvalError.NUM;
                }
                return d(r);
            }
            case 17: // DELTA
                return d(Evaluator.num(a, 0) == Evaluator.num(a, 1, 0) ? 1 : 0);
            case 18: // GESTEP
                return d(Evaluator.num(a, 0) >= Evaluator.num(a, 1, 0) ? 1 : 0);
            case 19: // ERF
                if (Evaluator.has(a, 1)) {
                    return d(DistributionFunctions.erf(Evaluator.num(a, 1)) - DistributionFunctions.erf(Evaluator.num(a, 0)));
                }
                return d(DistributionFunctions.erf(Evaluator.num(a, 0)));
            case 20: // ERF.PRECISE
                return d(DistributionFunctions.erf(Evaluator.num(a, 0)));
            case 21: // ERFC
            case 22: // ERFC.PRECISE
                return d(DistributionFunctions.erfc(Evaluator.num(a, 0)));
            case 23: // CONVERT
                return d(convert(Evaluator.num(a, 0), Evaluator.text(a, 1), Evaluator.text(a, 2)));
            default:
                return CellError.NAME;
        }
    }

    private static int unit(String name) {
        for (int i = 0; i < UNITS.length; i += 3) {
            if (UNITS[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /// The factor of a metric prefix, or 0 for a character that is not one.
    private static double prefix(String p) {
        switch (Functions.index(SWITCH_2, p)) {
            case 0: // Y
                return 1e24;
            case 1: // Z
                return 1e21;
            case 2: // E
                return 1e18;
            case 3: // P
                return 1e15;
            case 4: // T
                return 1e12;
            case 5: // G
                return 1e9;
            case 6: // M
                return 1e6;
            case 7: // k
                return 1e3;
            case 8: // h
                return 1e2;
            case 9: // da
            case 10: // e
                return 1e1;
            case 11: // d
                return 1e-1;
            case 12: // c
                return 1e-2;
            case 13: // m
                return 1e-3;
            case 14: // u
                return 1e-6;
            case 15: // n
                return 1e-9;
            case 16: // p
                return 1e-12;
            case 17: // f
                return 1e-15;
            case 18: // a
                return 1e-18;
            default:
                return 0;
        }
    }

    /// Finds a unit, with a metric prefix if the name as written is not one: [index, factor].
    private static double[] resolve(String name) {
        int i = unit(name);
        if (i >= 0) {
            return new double[] {i, 1};
        }
        for (int len = 2; len >= 1; len--) {
            if (name.length() > len) {
                double f = prefix(name.substring(0, len));
                int u = unit(name.substring(len));
                if (f != 0 && u >= 0) {
                    return new double[] {u, f};
                }
            }
        }
        throw EvalError.NA;
    }

    private static double convert(double value, String from, String to) {
        double kelvin = toKelvin(value, from);
        if (!Double.isNaN(kelvin)) {
            double result = fromKelvin(kelvin, to);
            if (Double.isNaN(result)) {
                throw EvalError.NA;
            }
            return result;
        }
        double[] f = resolve(from);
        double[] t = resolve(to);
        int fi = (int) f[0];
        int ti = (int) t[0];
        if (!UNITS[fi + 1].equals(UNITS[ti + 1])) {
            throw EvalError.NA;
        }
        return value * f[1] * Double.parseDouble(UNITS[fi + 2]) / (t[1] * Double.parseDouble(UNITS[ti + 2]));
    }

    private static double toKelvin(double v, String unit) {
        switch (Functions.index(SWITCH_3, unit)) {
            case 0: // C
            case 1: // cel
                return v + 273.15;
            case 2: // F
            case 3: // fah
                return (v - 32) * 5 / 9 + 273.15;
            case 4: // K
            case 5: // kel
                return v;
            case 6: // Rank
                return v * 5 / 9;
            default:
                return Double.NaN;
        }
    }

    private static double fromKelvin(double k, String unit) {
        switch (Functions.index(SWITCH_4, unit)) {
            case 0: // C
            case 1: // cel
                return k - 273.15;
            case 2: // F
            case 3: // fah
                return (k - 273.15) * 9 / 5 + 32;
            case 4: // K
            case 5: // kel
                return k;
            case 6: // Rank
                return k * 9 / 5;
            default:
                return Double.NaN;
        }
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "BIN2DEC", "OCT2DEC", "HEX2DEC", "BIN2OCT", "BIN2HEX", "OCT2BIN", "OCT2HEX", "HEX2BIN",
            "HEX2OCT", "DEC2BIN", "DEC2OCT", "DEC2HEX", "BITAND", "BITOR", "BITXOR", "BITLSHIFT",
            "BITRSHIFT", "DELTA", "GESTEP", "ERF", "ERF.PRECISE", "ERFC", "ERFC.PRECISE", "CONVERT");

    private static final java.util.HashMap<String, Integer> SWITCH_2 = Functions.switchTable(
            "Y", "Z", "E", "P", "T", "G", "M", "k", "h", "da", "e", "d", "c", "m", "u", "n", "p",
            "f", "a");

    private static final java.util.HashMap<String, Integer> SWITCH_3 = Functions.switchTable(
            "C", "cel", "F", "fah", "K", "kel", "Rank");

    private static final java.util.HashMap<String, Integer> SWITCH_4 = Functions.switchTable(
            "C", "cel", "F", "fah", "K", "kel", "Rank");
}
