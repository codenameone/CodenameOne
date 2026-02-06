/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.codename1.util;


/// Immutable objects describing settings such as rounding mode and digit
/// precision for the numerical operations provided by class `TBigDecimal`.
final class TMathContext {

    /// A `MathContext` which corresponds to the IEEE 754r quadruple
    /// decimal precision format: 34 digit precision and
    /// `TRoundingMode#HALF_EVEN` rounding.
    public static final TMathContext DECIMAL128 = new TMathContext(34,
            TRoundingMode.HALF_EVEN);

    /// A `MathContext` which corresponds to the IEEE 754r single decimal
    /// precision format: 7 digit precision and `TRoundingMode#HALF_EVEN`
    /// rounding.
    public static final TMathContext DECIMAL32 = new TMathContext(7,
            TRoundingMode.HALF_EVEN);

    /// A `MathContext` which corresponds to the IEEE 754r double decimal
    /// precision format: 16 digit precision and `TRoundingMode#HALF_EVEN`
    /// rounding.
    public static final TMathContext DECIMAL64 = new TMathContext(16,
            TRoundingMode.HALF_EVEN);

    /// A `MathContext` for unlimited precision with
    /// `TRoundingMode#HALF_UP` rounding.
    public static final TMathContext UNLIMITED = new TMathContext(0,
            TRoundingMode.HALF_UP);
    /// An array of `char` containing: `'p','r','e','c','i','s','i','o','n','='`. It's used to improve the
    /// methods related to `String` conversion.
    ///
    /// #### See also
    ///
    /// - #MathContext(String)
    ///
    /// - #toString()
    private final static char[] chPrecision = {'p', 'r', 'e', 'c', 'i', 's',
            'i', 'o', 'n', '='};
    /// An array of `char` containing: `'r','o','u','n','d','i','n','g','M','o','d','e','='`. It's used to
    /// improve the methods related to `String` conversion.
    ///
    /// #### See also
    ///
    /// - #MathContext(String)
    ///
    /// - #toString()
    private final static char[] chRoundingMode = {'r', 'o', 'u', 'n', 'd',
            'i', 'n', 'g', 'M', 'o', 'd', 'e', '='};
    /// A `RoundingMode` object which specifies the algorithm to be used
    /// for rounding.
    private final TRoundingMode roundingMode;
    /// The number of digits to be used for an operation; results are rounded to
    /// this precision.
    private int precision;

    /// Constructs a new `MathContext` with the specified precision and
    /// with the rounding mode `HALF_UP`. If the
    /// precision passed is zero, then this implies that the computations have to
    /// be performed exact, the rounding mode in this case is irrelevant.
    ///
    /// #### Parameters
    ///
    /// - `precision`: the precision for the new `MathContext`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `precision < 0`.
    public TMathContext(int precision) {
        this(precision, TRoundingMode.HALF_UP);
    }

    /// Constructs a new `MathContext` with the specified precision and
    /// with the specified rounding mode. If the precision passed is zero, then
    /// this implies that the computations have to be performed exact, the
    /// rounding mode in this case is irrelevant.
    ///
    /// #### Parameters
    ///
    /// - `precision`: the precision for the new `MathContext`.
    ///
    /// - `roundingMode`: the rounding mode for the new `MathContext`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `precision < 0`.
    ///
    /// - `NullPointerException`: if `roundingMode` is `null`.
    public TMathContext(int precision, TRoundingMode roundingMode) {
        if (precision < 0) {
            throw new IllegalArgumentException("Digits < 0");
        }
        if (roundingMode == null) {
            throw new NullPointerException("null RoundingMode");
        }
        this.precision = precision;
        this.roundingMode = roundingMode;
    }

    /// Constructs a new `MathContext` from a string. The string has to
    /// specify the precision and the rounding mode to be used and has to follow
    /// the following syntax: "precision=<precision> roundingMode=<roundingMode>"
    /// This is the same form as the one returned by the `#toString`
    /// method.
    ///
    /// #### Parameters
    ///
    /// - `val`: @param val a string describing the precision and rounding mode for the
    ///            new `MathContext`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException if the string is not in the correct format or if the
    ///                                  precision specified is < 0.
    public TMathContext(String val) {
        char[] charVal = val.toCharArray();
        int i; // Index of charVal
        int j; // Index of chRoundingMode
        int digit; // It will contain the digit parsed
        this.precision = 0;

        if ((charVal.length < 27) || (charVal.length > 45)) {
            throw new IllegalArgumentException("bad string format");
        }
        // Parsing "precision=" String
        for (i = 0; (i < chPrecision.length) && (charVal[i] == chPrecision[i]); i++) { // NOPMD EmptyControlStatement
        }

        if (i < chPrecision.length) {
            throw new IllegalArgumentException("bad string format");
        }
        // Parsing the value for "precision="...
        digit = Character.digit(charVal[i], 10);
        if (digit == -1) {
            throw new IllegalArgumentException("bad string format");
        }
        this.precision = this.precision * 10 + digit;
        i++;

        do {
            digit = Character.digit(charVal[i], 10);
            if (digit == -1) {
                if (charVal[i] == ' ') {
                    // It parsed all the digits
                    i++;
                    break;
                }
                // It isn't  a valid digit, and isn't a white space
                throw new IllegalArgumentException("bad string format");
            }
            // Accumulating the value parsed
            this.precision = this.precision * 10 + digit;
            if (this.precision < 0) {
                throw new IllegalArgumentException("bad string format");
            }
            i++;
        } while (true);
        // Parsing "roundingMode="
        for (j = 0; (j < chRoundingMode.length) && (charVal[i] == chRoundingMode[j]); i++, j++) { // NOPMD EmptyControlStatement
        }

        if (j < chRoundingMode.length) {
            throw new IllegalArgumentException("bad string format");
        }
        // Parsing the value for "roundingMode"...
        this.roundingMode = TRoundingMode.valueOf(String.valueOf(charVal, i, charVal.length - i));
    }

    /* Public Methods */

    /// Returns the precision. The precision is the number of digits used for an
    /// operation. Results are rounded to this precision. The precision is
    /// guaranteed to be non negative. If the precision is zero, then the
    /// computations have to be performed exact, results are not rounded in this
    /// case.
    ///
    /// #### Returns
    ///
    /// the precision.
    public int getPrecision() {
        return precision;
    }

    /// Returns the rounding mode. The rounding mode is the strategy to be used
    /// to round results.
    ///
    /// The rounding mode is one of
    /// `TRoundingMode#UP`,
    /// `TRoundingMode#DOWN`,
    /// `TRoundingMode#CEILING`,
    /// `TRoundingMode#FLOOR`,
    /// `TRoundingMode#HALF_UP`,
    /// `TRoundingMode#HALF_DOWN`,
    /// `TRoundingMode#HALF_EVEN`, or
    /// `TRoundingMode#UNNECESSARY`.
    ///
    /// #### Returns
    ///
    /// the rounding mode.
    public TRoundingMode getRoundingMode() {
        return roundingMode;
    }

    /// Returns true if x is a `MathContext` with the same precision
    /// setting and the same rounding mode as this `MathContext` instance.
    ///
    /// #### Parameters
    ///
    /// - `x`: object to be compared.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `MathContext` instance is equal to the
    /// `x` argument; `false` otherwise.
    @Override
    public boolean equals(Object x) {
        return ((x instanceof TMathContext)
                && (((TMathContext) x).getPrecision() == precision) && (((TMathContext) x)
                .getRoundingMode() == roundingMode));
    }

    /// Returns the hash code for this `MathContext` instance.
    ///
    /// #### Returns
    ///
    /// the hash code for this `MathContext`.
    @Override
    public int hashCode() {
        // Make place for the necessary bits to represent 8 rounding modes
        return ((precision << 3) | roundingMode.ordinal());
    }

    /// Returns the string representation for this `MathContext` instance.
    /// The string has the form
    /// `"precision= roundingMode="` where `` is an integer describing the number
    /// of digits used for operations and `` is the
    /// string representation of the rounding mode.
    ///
    /// #### Returns
    ///
    /// a string representation for this `MathContext` instance
    @Override
    public String toString() {

        return String.valueOf(chPrecision) +
                precision +
                ' ' +
                String.valueOf(chRoundingMode) +
                roundingMode;
    }
}
