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


/// This class represents immutable arbitrary precision decimal numbers. Each
/// `BigDecimal` instance is represented with a unscaled arbitrary
/// precision mantissa (the unscaled value) and a scale. The value of the `BigDecimal` is `unscaledValue` 10^(-`scale`).
class TBigDecimal {

    /// The constant zero as a `BigDecimal`.
    public static final TBigDecimal ZERO = new TBigDecimal(0, 0);

    /// The constant one as a `BigDecimal`.
    public static final TBigDecimal ONE = new TBigDecimal(1, 0);

    /// The constant ten as a `BigDecimal`.
    public static final TBigDecimal TEN = new TBigDecimal(10, 0);

    /// Rounding mode where positive values are rounded towards positive infinity
    /// and negative values towards negative infinity.
    ///
    /// #### See also
    ///
    /// - TRoundingMode#UP
    public static final int ROUND_UP = 0;

    /// Rounding mode where the values are rounded towards zero.
    ///
    /// #### See also
    ///
    /// - TRoundingMode#DOWN
    public static final int ROUND_DOWN = 1;

    /// Rounding mode to round towards positive infinity. For positive values
    /// this rounding mode behaves as `#ROUND_UP`, for negative values as
    /// `#ROUND_DOWN`.
    ///
    /// #### See also
    ///
    /// - TRoundingMode#CEILING
    public static final int ROUND_CEILING = 2;

    /// Rounding mode to round towards negative infinity. For positive values
    /// this rounding mode behaves as `#ROUND_DOWN`, for negative values as
    /// `#ROUND_UP`.
    ///
    /// #### See also
    ///
    /// - TRoundingMode#FLOOR
    public static final int ROUND_FLOOR = 3;

    /// Rounding mode where values are rounded towards the nearest neighbor.
    /// Ties are broken by rounding up.
    ///
    /// #### See also
    ///
    /// - TRoundingMode#HALF_UP
    public static final int ROUND_HALF_UP = 4;

    /// Rounding mode where values are rounded towards the nearest neighbor.
    /// Ties are broken by rounding down.
    ///
    /// #### See also
    ///
    /// - TRoundingMode#HALF_DOWN
    public static final int ROUND_HALF_DOWN = 5;

    /// Rounding mode where values are rounded towards the nearest neighbor.
    /// Ties are broken by rounding to the even neighbor.
    ///
    /// #### See also
    ///
    /// - TRoundingMode#HALF_EVEN
    public static final int ROUND_HALF_EVEN = 6;

    /// Rounding mode where the rounding operations throws an `ArithmeticException` for the case that rounding is necessary, i.e. for
    /// the case that the value cannot be represented exactly.
    ///
    /// #### See also
    ///
    /// - TRoundingMode#UNNECESSARY
    public static final int ROUND_UNNECESSARY = 7;

    /// This is the serialVersionUID used by the sun implementation.
    private static final long serialVersionUID = 6108874887143696463L;

    /// The double closer to `Log10(2)`.
    private static final double LOG10_2 = 0.3010299956639812;
    /// An array with powers of five that fit in the type `long`
    /// (`5^0,5^1,...,5^27`).
    private static final TBigInteger[] FIVE_POW;
    /// An array with powers of ten that fit in the type `long`
    /// (`10^0,10^1,...,10^18`).
    private static final TBigInteger[] TEN_POW;
    /// An array with powers of ten that fit in the type `long`
    /// (`10^0,10^1,...,10^18`).
    private static final long[] LONG_TEN_POW = new long[]
            {1L,
                    10L,
                    100L,
                    1000L,
                    10000L,
                    100000L,
                    1000000L,
                    10000000L,
                    100000000L,
                    1000000000L,
                    10000000000L,
                    100000000000L,
                    1000000000000L,
                    10000000000000L,
                    100000000000000L,
                    1000000000000000L,
                    10000000000000000L,
                    100000000000000000L,
                    1000000000000000000L};
    private static final long[] LONG_FIVE_POW = new long[]
            {1L,
                    5L,
                    25L,
                    125L,
                    625L,
                    3125L,
                    15625L,
                    78125L,
                    390625L,
                    1953125L,
                    9765625L,
                    48828125L,
                    244140625L,
                    1220703125L,
                    6103515625L,
                    30517578125L,
                    152587890625L,
                    762939453125L,
                    3814697265625L,
                    19073486328125L,
                    95367431640625L,
                    476837158203125L,
                    2384185791015625L,
                    11920928955078125L,
                    59604644775390625L,
                    298023223876953125L,
                    1490116119384765625L,
                    7450580596923828125L};
    private static final int[] LONG_FIVE_POW_BIT_LENGTH = new int[LONG_FIVE_POW.length];
    private static final int[] LONG_TEN_POW_BIT_LENGTH = new int[LONG_TEN_POW.length];
    private static final int BI_SCALED_BY_ZERO_LENGTH = 11;
    /// An array with the first `BigInteger` scaled by zero.
    /// (`[0,0],[1,0],...,[10,0]`).
    private static final TBigDecimal[] BI_SCALED_BY_ZERO = new TBigDecimal[BI_SCALED_BY_ZERO_LENGTH];
    /// An array with the zero number scaled by the first positive scales.
    /// (`0*10^0, 0*10^1, ..., 0*10^10`).
    private static final TBigDecimal[] ZERO_SCALED_BY = new TBigDecimal[11];
    /// An array filled with characters `'0'`.
    private static final char[] CH_ZEROS = new char[100];

    static {
        // To fill all static arrays.
        int i = 0;

        for (; i < ZERO_SCALED_BY.length; i++) {
            BI_SCALED_BY_ZERO[i] = new TBigDecimal(i, 0);
            ZERO_SCALED_BY[i] = new TBigDecimal(0, i);
            CH_ZEROS[i] = '0';
        }

        for (; i < CH_ZEROS.length; i++) {
            CH_ZEROS[i] = '0';
        }
        for (int j = 0; j < LONG_FIVE_POW_BIT_LENGTH.length; j++) {
            LONG_FIVE_POW_BIT_LENGTH[j] = bitLength(LONG_FIVE_POW[j]);
        }
        for (int j = 0; j < LONG_TEN_POW_BIT_LENGTH.length; j++) {
            LONG_TEN_POW_BIT_LENGTH[j] = bitLength(LONG_TEN_POW[j]);
        }

        // Taking the references of useful powers.
        TEN_POW = TMultiplication.bigTenPows;
        FIVE_POW = TMultiplication.bigFivePows;
    }

    /// The `String` representation is cached.
    private transient String toStringImage = null;
    /// Cache for the hash code.
    private transient int hashCode = 0;
    /// The arbitrary precision integer (unscaled value) in the internal
    /// representation of `BigDecimal`.
    private TBigInteger intVal;

    private transient int bitLength;

    private transient long smallValue;

    /// The 32-bit integer scale in the internal representation of `BigDecimal`.
    private int scale;

    /// Represent the number of decimal digits in the unscaled value. This
    /// precision is calculated the first time, and used in the following calls
    /// of method `precision()`. Note that some call to the private
    /// method `inplaceRound()` could update this field.
    ///
    /// #### See also
    ///
    /// - #precision()
    ///
    /// - #inplaceRound(TMathContext)
    private transient int precision = 0;

    private TBigDecimal(long smallValue, int scale) {
        this.smallValue = smallValue;
        this.scale = scale;
        this.bitLength = bitLength(smallValue);
    }

    private TBigDecimal(int smallValue, int scale) {
        this.smallValue = smallValue;
        this.scale = scale;
        this.bitLength = bitLength(smallValue);
    }

    /// Constructs a new `BigDecimal` instance from a string representation
    /// given as a character array.
    ///
    /// #### Parameters
    ///
    /// - `in`: @param in     array of characters containing the string representation of
    ///               this `BigDecimal`.
    ///
    /// - `offset`: first index to be copied.
    ///
    /// - `len`: number of characters to be used.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `in == null`.
    ///
    /// - `NumberFormatException`: if `offset = in.length`.
    ///
    /// - `NumberFormatException`: @throws NumberFormatException if in does not contain a valid string representation of a big
    ///                               decimal.
    public TBigDecimal(char[] in, int offset, int len) {
        int begin = offset; // first index to be copied
        int last = offset + (len - 1); // last index to be copied
        String scaleString = null; // buffer for scale
        StringBuilder unscaledBuffer; // buffer for unscaled value
        long newScale; // the new scale

        if (in == null) {
            throw new NullPointerException();
        }
        if ((last >= in.length) || (offset < 0) || (len <= 0) || (last < 0)) {
            throw new NumberFormatException();
        }
        unscaledBuffer = new StringBuilder(len);
        int bufLength = 0;
        // To skip a possible '+' symbol
        if ((offset <= last) && (in[offset] == '+')) {
            offset++;
            begin++;
        }
        int counter = 0;
        boolean wasNonZero = false;
        // Accumulating all digits until a possible decimal point
        for (; (offset <= last) && (in[offset] != '.')
                && (in[offset] != 'e') && (in[offset] != 'E'); offset++) {
            if (!wasNonZero) {
                if (in[offset] == '0') {
                    counter++;
                } else {
                    wasNonZero = true;
                }
            }

        }
        unscaledBuffer.append(in, begin, offset - begin);
        bufLength += offset - begin;
        // A decimal point was found
        if ((offset <= last) && (in[offset] == '.')) {
            offset++;
            // Accumulating all digits until a possible exponent
            begin = offset;
            for (; (offset <= last) && (in[offset] != 'e')
                    && (in[offset] != 'E'); offset++) {
                if (!wasNonZero) {
                    if (in[offset] == '0') {
                        counter++;
                    } else {
                        wasNonZero = true;
                    }
                }
            }
            scale = offset - begin;
            bufLength += scale;
            unscaledBuffer.append(in, begin, scale);
        } else {
            scale = 0;
        }
        // An exponent was found
        if ((offset <= last) && ((in[offset] == 'e') || (in[offset] == 'E'))) {
            offset++;
            // Checking for a possible sign of scale
            begin = offset;
            if ((offset <= last) && (in[offset] == '+')) {
                offset++;
                if ((offset <= last) && (in[offset] != '-')) {
                    begin++;
                }
            }
            // Accumulating all remaining digits
            scaleString = String.valueOf(in, begin, last + 1 - begin);
            // Checking if the scale is defined
            newScale = (long) scale - Integer.parseInt(scaleString);
            scale = (int) newScale;
            if (newScale != scale) {
                throw new NumberFormatException("Scale out of range.");
            }
        }
        // Parsing the unscaled value
        if (bufLength < 19) {
            smallValue = Long.parseLong(unscaledBuffer.toString());
            bitLength = bitLength(smallValue);
        } else {
            setUnscaledValue(new TBigInteger(unscaledBuffer.toString()));
        }
        precision = unscaledBuffer.length() - counter;
        if (unscaledBuffer.charAt(0) == '-') {
            precision--;
        }
    }

    /// Constructs a new `BigDecimal` instance from a string representation
    /// given as a character array.
    ///
    /// #### Parameters
    ///
    /// - `in`: @param in     array of characters containing the string representation of
    ///               this `BigDecimal`.
    ///
    /// - `offset`: first index to be copied.
    ///
    /// - `len`: number of characters to be used.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `in == null`.
    ///
    /// - `NumberFormatException`: if `offset = in.length`.
    ///
    /// - `NumberFormatException`: @throws NumberFormatException if `in` does not contain a valid string representation
    ///                               of a big decimal.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException   if `mc.precision > 0` and `mc.roundingMode ==
    ///                               UNNECESSARY` and the new big decimal cannot be represented
    ///                               within the given precision without rounding.
    public TBigDecimal(char[] in, int offset, int len, TMathContext mc) {
        this(in, offset, len);
        inplaceRound(mc);
    }

    /// Constructs a new `BigDecimal` instance from a string representation
    /// given as a character array.
    ///
    /// #### Parameters
    ///
    /// - `in`: @param in array of characters containing the string representation of
    ///           this `BigDecimal`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `in == null`.
    ///
    /// - `NumberFormatException`: @throws NumberFormatException if `in` does not contain a valid string representation
    ///                               of a big decimal.
    public TBigDecimal(char[] in) {
        this(in, 0, in.length);
    }

    /// Constructs a new `BigDecimal` instance from a string representation
    /// given as a character array. The result is rounded according to the
    /// specified math context.
    ///
    /// #### Parameters
    ///
    /// - `in`: @param in array of characters containing the string representation of
    ///           this `BigDecimal`.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `in == null`.
    ///
    /// - `NumberFormatException`: @throws NumberFormatException if `in` does not contain a valid string representation
    ///                               of a big decimal.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException   if `mc.precision > 0` and `mc.roundingMode ==
    ///                               UNNECESSARY` and the new big decimal cannot be represented
    ///                               within the given precision without rounding.
    public TBigDecimal(char[] in, TMathContext mc) {
        this(in, 0, in.length);
        inplaceRound(mc);
    }

    /// Constructs a new `BigDecimal` instance from a string
    /// representation.
    ///
    /// #### Parameters
    ///
    /// - `val`: string containing the string representation of this `BigDecimal`.
    ///
    /// #### Throws
    ///
    /// - `NumberFormatException`: @throws NumberFormatException if `val` does not contain a valid string representation
    ///                               of a big decimal.
    public TBigDecimal(String val) {
        this(val.toCharArray(), 0, val.length());
    }

    /// Constructs a new `BigDecimal` instance from a string
    /// representation. The result is rounded according to the specified math
    /// context.
    ///
    /// #### Parameters
    ///
    /// - `val`: string containing the string representation of this `BigDecimal`.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Throws
    ///
    /// - `NumberFormatException`: @throws NumberFormatException if `val` does not contain a valid string representation
    ///                               of a big decimal.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException   if `mc.precision > 0` and `mc.roundingMode ==
    ///                               UNNECESSARY` and the new big decimal cannot be represented
    ///                               within the given precision without rounding.
    public TBigDecimal(String val, TMathContext mc) {
        this(val.toCharArray(), 0, val.length());
        inplaceRound(mc);
    }


    /// Constructs a new `BigDecimal` instance from the 64bit double
    /// `val`. The constructed big decimal is equivalent to the given
    /// double. For example, `new BigDecimal(0.1)` is equal to `0.1000000000000000055511151231257827021181583404541015625`. This happens
    /// as `0.1` cannot be represented exactly in binary.
    ///
    /// To generate a big decimal instance which is equivalent to `0.1` use
    /// the `BigDecimal(String)` constructor.
    ///
    /// #### Parameters
    ///
    /// - `val`: double value to be converted to a `BigDecimal` instance.
    ///
    /// #### Throws
    ///
    /// - `NumberFormatException`: if `val` is infinity or not a number.
    public TBigDecimal(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) {
            throw new NumberFormatException("Infinite or NaN");
        }
        long bits = Double.doubleToLongBits(val); // IEEE-754
        long mantisa;
        int trailingZeros;
        // Extracting the exponent, note that the bias is 1023
        scale = 1075 - (int) ((bits >> 52) & 0x7FFL);
        // Extracting the 52 bits of the mantisa.
        mantisa = scale == 1075 ? (bits & 0xFFFFFFFFFFFFFL) << 1 : (bits & 0xFFFFFFFFFFFFFL) | 0x10000000000000L;
        if (mantisa == 0) {
            scale = 0;
            precision = 1;
        }
        // To simplify all factors '2' in the mantisa
        if (scale > 0) {
            trailingZeros = Math.min(scale, numberOfTrailingZeros(mantisa));
            mantisa >>>= trailingZeros;
            scale -= trailingZeros;
        }
        // Calculating the new unscaled value and the new scale
        if ((bits >> 63) != 0) {
            mantisa = -mantisa;
        }
        int mantisaBits = bitLength(mantisa);
        if (scale < 0) {
            bitLength = mantisaBits == 0 ? 0 : mantisaBits - scale;
            if (bitLength < 64) {
                smallValue = mantisa << (-scale);
            } else {
                intVal = TBigInteger.valueOf(mantisa).shiftLeft(-scale);
            }
            scale = 0;
        } else if (scale > 0) {
            // m * 2^e =  (m * 5^(-e)) * 10^e
            if (scale < LONG_FIVE_POW.length && mantisaBits + LONG_FIVE_POW_BIT_LENGTH[scale] < 64) {
                smallValue = mantisa * LONG_FIVE_POW[scale];
                bitLength = bitLength(smallValue);
            } else {
                setUnscaledValue(TMultiplication.multiplyByFivePow(TBigInteger.valueOf(mantisa), scale));
            }
        } else { // scale == 0
            smallValue = mantisa;
            bitLength = mantisaBits;
        }
    }

    /// Constructs a new `BigDecimal` instance from the 64bit double
    /// `val`. The constructed big decimal is equivalent to the given
    /// double. For example, `new BigDecimal(0.1)` is equal to `0.1000000000000000055511151231257827021181583404541015625`. This happens
    /// as `0.1` cannot be represented exactly in binary.
    ///
    /// To generate a big decimal instance which is equivalent to `0.1` use
    /// the `BigDecimal(String)` constructor.
    ///
    /// #### Parameters
    ///
    /// - `val`: double value to be converted to a `BigDecimal` instance.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Throws
    ///
    /// - `NumberFormatException`: if `val` is infinity or not a number.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException   if `mc.precision > 0` and `mc.roundingMode ==
    ///                               UNNECESSARY` and the new big decimal cannot be represented
    ///                               within the given precision without rounding.
    public TBigDecimal(double val, TMathContext mc) {
        this(val);
        inplaceRound(mc);
    }

    /// Constructs a new `BigDecimal` instance from the given big integer
    /// `val`. The scale of the result is `0`.
    ///
    /// #### Parameters
    ///
    /// - `val`: `BigInteger` value to be converted to a `BigDecimal` instance.
    public TBigDecimal(TBigInteger val) {
        this(val, 0);
    }


    /// Constructs a new `BigDecimal` instance from the given big integer
    /// `val`. The scale of the result is `0`.
    ///
    /// #### Parameters
    ///
    /// - `val`: `BigInteger` value to be converted to a `BigDecimal` instance.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: @throws ArithmeticException if `mc.precision > 0` and `mc.roundingMode ==
    ///                             UNNECESSARY` and the new big decimal cannot be represented
    ///                             within the given precision without rounding.
    public TBigDecimal(TBigInteger val, TMathContext mc) {
        this(val);
        inplaceRound(mc);
    }

    /// Constructs a new `BigDecimal` instance from a given unscaled value
    /// `unscaledVal` and a given scale. The value of this instance is
    /// `unscaledVal` 10^(-`scale`).
    ///
    /// #### Parameters
    ///
    /// - `unscaledVal`: @param unscaledVal `BigInteger` representing the unscaled value of this
    ///                    `BigDecimal` instance.
    ///
    /// - `scale`: scale of this `BigDecimal` instance.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `unscaledVal == null`.
    public TBigDecimal(TBigInteger unscaledVal, int scale) {
        if (unscaledVal == null) {
            throw new NullPointerException();
        }
        this.scale = scale;
        setUnscaledValue(unscaledVal);
    }

    /// Constructs a new `BigDecimal` instance from a given unscaled value
    /// `unscaledVal` and a given scale. The value of this instance is
    /// `unscaledVal` 10^(-`scale`). The result is rounded according
    /// to the specified math context.
    ///
    /// #### Parameters
    ///
    /// - `unscaledVal`: @param unscaledVal `BigInteger` representing the unscaled value of this
    ///                    `BigDecimal` instance.
    ///
    /// - `scale`: scale of this `BigDecimal` instance.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: @throws ArithmeticException  if `mc.precision > 0` and `mc.roundingMode ==
    ///                              UNNECESSARY` and the new big decimal cannot be represented
    ///                              within the given precision without rounding.
    ///
    /// - `NullPointerException`: if `unscaledVal == null`.
    public TBigDecimal(TBigInteger unscaledVal, int scale, TMathContext mc) {
        this(unscaledVal, scale);
        inplaceRound(mc);
    }

    /// Constructs a new `BigDecimal` instance from the given int
    /// `val`. The scale of the result is 0.
    ///
    /// #### Parameters
    ///
    /// - `val`: int value to be converted to a `BigDecimal` instance.
    public TBigDecimal(int val) {
        this(val, 0);
    }

    /// Constructs a new `BigDecimal` instance from the given int `val`. The scale of the result is `0`. The result is rounded
    /// according to the specified math context.
    ///
    /// #### Parameters
    ///
    /// - `val`: int value to be converted to a `BigDecimal` instance.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: @throws ArithmeticException if `mc.precision > 0` and `c.roundingMode ==
    ///                             UNNECESSARY` and the new big decimal cannot be represented
    ///                             within the given precision without rounding.
    public TBigDecimal(int val, TMathContext mc) {
        this(val, 0);
        inplaceRound(mc);
    }

    /// Constructs a new `BigDecimal` instance from the given long `val`. The scale of the result is `0`.
    ///
    /// #### Parameters
    ///
    /// - `val`: long value to be converted to a `BigDecimal` instance.
    public TBigDecimal(long val) {
        this(val, 0);
    }

    /// Constructs a new `BigDecimal` instance from the given long `val`. The scale of the result is `0`. The result is rounded
    /// according to the specified math context.
    ///
    /// #### Parameters
    ///
    /// - `val`: long value to be converted to a `BigDecimal` instance.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: @throws ArithmeticException if `mc.precision > 0` and `mc.roundingMode ==
    ///                             UNNECESSARY` and the new big decimal cannot be represented
    ///                             within the given precision without rounding.
    public TBigDecimal(long val, TMathContext mc) {
        this(val);
        inplaceRound(mc);
    }

    /// Determines the number of trailing zeros in the `long` passed
    /// after the `lowest one bit`.
    ///
    /// #### Parameters
    ///
    /// - `lng`: The `long` to process.
    ///
    /// #### Returns
    ///
    /// The number of trailing zeros.
    ///
    /// #### Since
    ///
    /// 1.5
    static int numberOfTrailingZeros(long lng) {
        return bitCount((lng & -lng) - 1);
    }

    /// Determines the number of leading zeros in the `long` passed
    /// prior to the `highest one bit`.
    ///
    /// #### Parameters
    ///
    /// - `lng`: The `long` to process.
    ///
    /// #### Returns
    ///
    /// The number of leading zeros.
    ///
    /// #### Since
    ///
    /// 1.5
    public static int numberOfLeadingZeros(long lng) {
        lng |= lng >> 1;
        lng |= lng >> 2;
        lng |= lng >> 4;
        lng |= lng >> 8;
        lng |= lng >> 16;
        lng |= lng >> 32;
        return bitCount(~lng);
    }

    /// Counts the number of 1 bits in the `long` value passed; this
    /// is sometimes referred to as a population count.
    ///
    /// #### Parameters
    ///
    /// - `lng`: The `long` value to process.
    ///
    /// #### Returns
    ///
    /// The number of 1 bits.
    ///
    /// #### Since
    ///
    /// 1.5
    static int bitCount(long lng) {
        lng = (lng & 0x5555555555555555L) + ((lng >> 1) & 0x5555555555555555L);
        lng = (lng & 0x3333333333333333L) + ((lng >> 2) & 0x3333333333333333L);
        // adjust for 64-bit integer
        int i = (int) ((lng >>> 32) + lng);
        i = (i & 0x0F0F0F0F) + ((i >> 4) & 0x0F0F0F0F);
        i = (i & 0x00FF00FF) + ((i >> 8) & 0x00FF00FF);
        i = (i & 0x0000FFFF) + ((i >> 16) & 0x0000FFFF);
        return i;
    }

    /// The `signum` function for `long` values. This
    /// method returns -1 for negative values, 1 for positive values and 0 for
    /// the value 0.
    ///
    /// #### Parameters
    ///
    /// - `lng`: The `long` value.
    ///
    /// #### Returns
    ///
    /// -1 if negative, 1 if positive otherwise 0.
    ///
    /// #### Since
    ///
    /// 1.5
    static int signum(long lng) {
        return (lng == 0 ? 0 : (lng < 0 ? -1 : 1));
    }

    static char forDigit(int digit, int radix) {
        if (radix < 2 || radix > 36 || digit >= radix) {
            return '\0';
        }
        return digit < 10 ? (char) ('0' + digit) : (char) ('a' + digit - 10);
    }

    /// Determines the highest (leftmost) bit that is 1 and returns the value
    /// that is the bit mask for that bit. This is sometimes referred to as the
    /// Most Significant 1 Bit.
    ///
    /// #### Parameters
    ///
    /// - `i`: The `int` to interrogate.
    ///
    /// #### Returns
    ///
    /// The bit mask indicating the highest 1 bit.
    ///
    /// #### Since
    ///
    /// 1.5
    static int highestOneBit(int i) {
        i |= (i >> 1);
        i |= (i >> 2);
        i |= (i >> 4);
        i |= (i >> 8);
        i |= (i >> 16);
        return (i & ~(i >>> 1));
    }

    /* Public Methods */

    /// Returns a new `BigDecimal` instance whose value is equal to `unscaledVal` 10^(-`scale`). The scale of the result is `scale`, and its unscaled value is `unscaledVal`.
    ///
    /// #### Parameters
    ///
    /// - `unscaledVal`: unscaled value to be used to construct the new `BigDecimal`.
    ///
    /// - `scale`: scale to be used to construct the new `BigDecimal`.
    ///
    /// #### Returns
    ///
    /// @return `BigDecimal` instance with the value `unscaledVal`*
    /// 10^(-`unscaledVal`).
    public static TBigDecimal valueOf(long unscaledVal, int scale) {
        if (scale == 0) {
            return valueOf(unscaledVal);
        }
        if ((unscaledVal == 0) && (scale >= 0)
                && (scale < ZERO_SCALED_BY.length)) {
            return ZERO_SCALED_BY[scale];
        }
        return new TBigDecimal(unscaledVal, scale);
    }

    /// Returns a new `BigDecimal` instance whose value is equal to `unscaledVal`. The scale of the result is `0`, and its unscaled
    /// value is `unscaledVal`.
    ///
    /// #### Parameters
    ///
    /// - `unscaledVal`: value to be converted to a `BigDecimal`.
    ///
    /// #### Returns
    ///
    /// `BigDecimal` instance with the value `unscaledVal`.
    public static TBigDecimal valueOf(long unscaledVal) {
        if ((unscaledVal >= 0) && (unscaledVal < BI_SCALED_BY_ZERO_LENGTH)) {
            return BI_SCALED_BY_ZERO[(int) unscaledVal];
        }
        return new TBigDecimal(unscaledVal, 0);
    }

    /// Returns a new `BigDecimal` instance whose value is equal to `val`. The new decimal is constructed as if the `BigDecimal(String)`
    /// constructor is called with an argument which is equal to `Double.toString(val)`. For example, `valueOf("0.1")` is converted to
    /// (unscaled=1, scale=1), although the double `0.1` cannot be
    /// represented exactly as a double value. In contrast to that, a new `BigDecimal(0.1)` instance has the value `0.1000000000000000055511151231257827021181583404541015625` with an
    /// unscaled value `1000000000000000055511151231257827021181583404541015625`
    /// and the scale `55`.
    ///
    /// #### Parameters
    ///
    /// - `val`: double value to be converted to a `BigDecimal`.
    ///
    /// #### Returns
    ///
    /// `BigDecimal` instance with the value `val`.
    ///
    /// #### Throws
    ///
    /// - `NumberFormatException`: if `val` is infinite or `val` is not a number
    public static TBigDecimal valueOf(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) {
            throw new NumberFormatException("Infinity or NaN");
        }
        return new TBigDecimal(Double.toString(val));
    }

    private static TBigDecimal addAndMult10(TBigDecimal thisValue, TBigDecimal augend, int diffScale) {
        if (diffScale < LONG_TEN_POW.length &&
                Math.max(thisValue.bitLength, augend.bitLength + LONG_TEN_POW_BIT_LENGTH[diffScale]) + 1 < 64) {
            return valueOf(thisValue.smallValue + augend.smallValue * LONG_TEN_POW[diffScale], thisValue.scale);
        }
        return new TBigDecimal(thisValue.getUnscaledValue().add(
                TMultiplication.multiplyByTenPow(augend.getUnscaledValue(), diffScale)), thisValue.scale);
    }

    private static TBigDecimal divideBigIntegers(TBigInteger scaledDividend, TBigInteger scaledDivisor, int scale,
                                                 TRoundingMode roundingMode) {
        TBigInteger[] quotAndRem = scaledDividend.divideAndRemainder(scaledDivisor);  // quotient and remainder
        // If after division there is a remainder...
        TBigInteger quotient = quotAndRem[0];
        TBigInteger remainder = quotAndRem[1];
        if (remainder.signum() == 0) {
            return new TBigDecimal(quotient, scale);
        }
        int sign = scaledDividend.signum() * scaledDivisor.signum();
        int compRem;                                      // 'compare to remainder'
        if (scaledDivisor.bitLength() < 63) { // 63 in order to avoid out of long after <<1
            long rem = remainder.longValue();
            long divisor = scaledDivisor.longValue();
            compRem = longCompareTo(Math.abs(rem) << 1, Math.abs(divisor));
            // To look if there is a carry
            compRem = roundingBehavior(quotient.testBit(0) ? 1 : 0,
                    sign * (5 + compRem), roundingMode);

        } else {
            // Checking if:  remainder * 2 >= scaledDivisor
            compRem = remainder.abs().shiftLeftOneBit().compareTo(scaledDivisor.abs());
            compRem = roundingBehavior(quotient.testBit(0) ? 1 : 0, sign * (5 + compRem), roundingMode);
        }
        if (compRem != 0) {
            if (quotient.bitLength() < 63) {
                return valueOf(quotient.longValue() + compRem, scale);
            }
            quotient = quotient.add(TBigInteger.valueOf(compRem));
            return new TBigDecimal(quotient, scale);
        }
        // Constructing the result with the appropriate unscaled value
        return new TBigDecimal(quotient, scale);
    }

    private static TBigDecimal dividePrimitiveLongs(long scaledDividend, long scaledDivisor, int scale,
                                                    TRoundingMode roundingMode) {
        long quotient = scaledDividend / scaledDivisor;
        long remainder = scaledDividend % scaledDivisor;
        int sign = signum(scaledDividend) * signum(scaledDivisor);
        if (remainder != 0) {
            // Checking if:  remainder * 2 >= scaledDivisor
            int compRem;                                      // 'compare to remainder'
            compRem = longCompareTo(Math.abs(remainder) << 1, Math.abs(scaledDivisor));
            // To look if there is a carry
            quotient += roundingBehavior(((int) quotient) & 1, sign * (5 + compRem), roundingMode);
        }
        // Constructing the result with the appropriate unscaled value
        return valueOf(quotient, scale);
    }

    private static int longCompareTo(long value1, long value2) {
        return value1 > value2 ? 1 : (value1 < value2 ? -1 : 0);
    }

    /// Return an increment that can be -1,0 or 1, depending of
    /// `roundingMode`.
    ///
    /// #### Parameters
    ///
    /// - `parityBit`: @param parityBit    can be 0 or 1, it's only used in the case
    ///                     `HALF_EVEN`
    ///
    /// - `fraction`: the mantisa to be analyzed
    ///
    /// - `roundingMode`: the type of rounding
    ///
    /// #### Returns
    ///
    /// the carry propagated after rounding
    private static int roundingBehavior(int parityBit, int fraction, TRoundingMode roundingMode) {
        int increment = 0; // the carry after rounding

        switch (roundingMode) {
            case UNNECESSARY:
                if (fraction != 0) {
                    throw new ArithmeticException("Rounding necessary");
                }
                break;
            case UP:
                increment = signum(fraction);
                break;
            case DOWN:
                break;
            case CEILING:
                increment = Math.max(signum(fraction), 0);
                break;
            case FLOOR:
                increment = Math.min(signum(fraction), 0);
                break;
            case HALF_UP:
                if (Math.abs(fraction) >= 5) {
                    increment = signum(fraction);
                }
                break;
            case HALF_DOWN:
                if (Math.abs(fraction) > 5) {
                    increment = signum(fraction);
                }
                break;
            case HALF_EVEN:
                if (Math.abs(fraction) + parityBit > 5) {
                    increment = signum(fraction);
                }
                break;
        }
        return increment;
    }

    /// It tests if a scale of type `long` fits in 32 bits. It
    /// returns the same scale being casted to `int` type when is
    /// possible, otherwise throws an exception.
    ///
    /// #### Parameters
    ///
    /// - `longScale`: a 64 bit scale
    ///
    /// #### Returns
    ///
    /// a 32 bit scale when is possible
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: @throws ArithmeticException when `scale` doesn't
    ///                             fit in `int` type
    ///
    /// #### See also
    ///
    /// - #scale
    private static int toIntScale(long longScale) {
        if (longScale < Integer.MIN_VALUE) {
            throw new ArithmeticException("Overflow");
        } else if (longScale > Integer.MAX_VALUE) {
            throw new ArithmeticException("Underflow");
        } else {
            return (int) longScale;
        }
    }

    /// It returns the value 0 with the most approximated scale of type
    /// `int`. if `longScale > Integer.MAX_VALUE` the
    /// scale will be `Integer.MAX_VALUE`; if
    /// `longScale < Integer.MIN_VALUE` the scale will be
    /// `Integer.MIN_VALUE`; otherwise `longScale` is
    /// casted to the type `int`.
    ///
    /// #### Parameters
    ///
    /// - `longScale`: the scale to which the value 0 will be scaled.
    ///
    /// #### Returns
    ///
    /// the value 0 scaled by the closer scale of type `int`.
    ///
    /// #### See also
    ///
    /// - #scale
    private static TBigDecimal zeroScaledBy(long longScale) {
        if (longScale == (int) longScale) {
            return valueOf(0, (int) longScale);
        }
        if (longScale >= 0) {
            return new TBigDecimal(0, Integer.MAX_VALUE);
        }
        return new TBigDecimal(0, Integer.MIN_VALUE);
    }

    private static int bitLength(long smallValue) {
        if (smallValue < 0) {
            smallValue = ~smallValue;
        }
        return 64 - numberOfLeadingZeros(smallValue);
    }

    private static int bitLength(int smallValue) {
        if (smallValue < 0) {
            smallValue = ~smallValue;
        }
        return 32 - numberOfLeadingZeros(smallValue);
    }

    /// Returns a new `BigDecimal` whose value is `this + augend`.
    /// The scale of the result is the maximum of the scales of the two
    /// arguments.
    ///
    /// #### Parameters
    ///
    /// - `augend`: value to be added to `this`.
    ///
    /// #### Returns
    ///
    /// `this + augend`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `augend == null`.
    public TBigDecimal add(TBigDecimal augend) {
        int diffScale = this.scale - augend.scale;
        // Fast return when some operand is zero
        if (this.isZero()) {
            if (diffScale <= 0) {
                return augend;
            }
            if (augend.isZero()) {
                return this;
            }
        } else if (augend.isZero()) {
            if (diffScale >= 0) {
                return this;
            }
        }
        // Let be:  this = [u1,s1]  and  augend = [u2,s2]
        if (diffScale == 0) {
            // case s1 == s2: [u1 + u2 , s1]
            if (Math.max(this.bitLength, augend.bitLength) + 1 < 64) {
                return valueOf(this.smallValue + augend.smallValue, this.scale);
            }
            return new TBigDecimal(this.getUnscaledValue().add(augend.getUnscaledValue()), this.scale);
        } else if (diffScale > 0) {
            // case s1 > s2 : [(u1 + u2) * 10 ^ (s1 - s2) , s1]
            return addAndMult10(this, augend, diffScale);
        } else {
            // case s2 > s1 : [(u2 + u1) * 10 ^ (s2 - s1) , s2]
            return addAndMult10(augend, this, -diffScale);
        }
    }

    /// Returns a new `BigDecimal` whose value is `this + augend`.
    /// The result is rounded according to the passed context `mc`.
    ///
    /// #### Parameters
    ///
    /// - `augend`: value to be added to `this`.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Returns
    ///
    /// `this + augend`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `augend == null` or `mc == null`.
    public TBigDecimal add(TBigDecimal augend, TMathContext mc) {
        TBigDecimal larger; // operand with the largest unscaled value
        TBigDecimal smaller; // operand with the smallest unscaled value
        TBigInteger tempBI;
        long diffScale = (long) this.scale - augend.scale;
        int largerSignum;
        // Some operand is zero or the precision is infinity
        if ((augend.isZero()) || (this.isZero()) || (mc.getPrecision() == 0)) {
            return add(augend).round(mc);
        }
        // Cases where there is room for optimizations
        if (this.aproxPrecision() < diffScale - 1) {
            larger = augend;
            smaller = this;
        } else if (augend.aproxPrecision() < -diffScale - 1) {
            larger = this;
            smaller = augend;
        } else {
            // No optimization is done
            return add(augend).round(mc);
        }
        if (mc.getPrecision() >= larger.aproxPrecision()) {
            // No optimization is done
            return add(augend).round(mc);
        }
        // Cases where it's unnecessary to add two numbers with very different scales
        largerSignum = larger.signum();
        if (largerSignum == smaller.signum()) {
            tempBI = TMultiplication.multiplyByPositiveInt(larger.getUnscaledValue(), 10)
                    .add(TBigInteger.valueOf(largerSignum));
        } else {
            tempBI = larger.getUnscaledValue().subtract(TBigInteger.valueOf(largerSignum));
            tempBI = TMultiplication.multiplyByPositiveInt(tempBI, 10).add(TBigInteger.valueOf(largerSignum * 9L));
        }
        // Rounding the improved adding
        larger = new TBigDecimal(tempBI, larger.scale + 1);
        return larger.round(mc);
    }

    /// Returns a new `BigDecimal` whose value is `this - subtrahend`.
    /// The scale of the result is the maximum of the scales of the two arguments.
    ///
    /// #### Parameters
    ///
    /// - `subtrahend`: value to be subtracted from `this`.
    ///
    /// #### Returns
    ///
    /// `this - subtrahend`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `subtrahend == null`.
    public TBigDecimal subtract(TBigDecimal subtrahend) {
        int diffScale = this.scale - subtrahend.scale;
        // Fast return when some operand is zero
        if (this.isZero()) {
            if (diffScale <= 0) {
                return subtrahend.negate();
            }
            if (subtrahend.isZero()) {
                return this;
            }
        } else if (subtrahend.isZero()) {
            if (diffScale >= 0) {
                return this;
            }
        }
        // Let be: this = [u1,s1] and subtrahend = [u2,s2] so:
        if (diffScale == 0) {
            // case s1 = s2 : [u1 - u2 , s1]
            if (Math.max(this.bitLength, subtrahend.bitLength) + 1 < 64) {
                return valueOf(this.smallValue - subtrahend.smallValue, this.scale);
            }
            return new TBigDecimal(this.getUnscaledValue().subtract(subtrahend.getUnscaledValue()), this.scale);
        } else if (diffScale > 0) {
            // case s1 > s2 : [ u1 - u2 * 10 ^ (s1 - s2) , s1 ]
            if (diffScale < LONG_TEN_POW.length &&
                    Math.max(this.bitLength, subtrahend.bitLength + LONG_TEN_POW_BIT_LENGTH[diffScale]) + 1 < 64) {
                return valueOf(this.smallValue - subtrahend.smallValue * LONG_TEN_POW[diffScale], this.scale);
            }
            return new TBigDecimal(this.getUnscaledValue().subtract(
                    TMultiplication.multiplyByTenPow(subtrahend.getUnscaledValue(), diffScale)), this.scale);
        } else {
            // case s2 > s1 : [ u1 * 10 ^ (s2 - s1) - u2 , s2 ]
            diffScale = -diffScale;
            if (diffScale < LONG_TEN_POW.length &&
                    Math.max(this.bitLength + LONG_TEN_POW_BIT_LENGTH[diffScale], subtrahend.bitLength) + 1 < 64) {
                return valueOf(this.smallValue * LONG_TEN_POW[diffScale] - subtrahend.smallValue, subtrahend.scale);
            }
            return new TBigDecimal(TMultiplication.multiplyByTenPow(this.getUnscaledValue(), diffScale)
                    .subtract(subtrahend.getUnscaledValue()), subtrahend.scale);
        }
    }

    /// Returns a new `BigDecimal` whose value is `this - subtrahend`.
    /// The result is rounded according to the passed context `mc`.
    ///
    /// #### Parameters
    ///
    /// - `subtrahend`: value to be subtracted from `this`.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Returns
    ///
    /// `this - subtrahend`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `subtrahend == null` or `mc == null`.
    public TBigDecimal subtract(TBigDecimal subtrahend, TMathContext mc) {
        long diffScale = subtrahend.scale - (long) this.scale;
        int thisSignum;
        TBigDecimal leftOperand; // it will be only the left operand (this)
        TBigInteger tempBI;
        // Some operand is zero or the precision is infinity
        if (subtrahend.isZero() || isZero() || mc.getPrecision() == 0) {
            return subtract(subtrahend).round(mc);
        }
        // Now:   this != 0   and   subtrahend != 0
        if (subtrahend.aproxPrecision() < diffScale - 1) {
            // Cases where it is unnecessary to subtract two numbers with very different scales
            if (mc.getPrecision() < this.aproxPrecision()) {
                thisSignum = this.signum();
                if (thisSignum != subtrahend.signum()) {
                    tempBI = TMultiplication.multiplyByPositiveInt(this.getUnscaledValue(), 10)
                            .add(TBigInteger.valueOf(thisSignum));
                } else {
                    tempBI = this.getUnscaledValue().subtract(TBigInteger.valueOf(thisSignum));
                    tempBI = TMultiplication.multiplyByPositiveInt(tempBI, 10)
                            .add(TBigInteger.valueOf(thisSignum * 9L));
                }
                // Rounding the improved subtracting
                leftOperand = new TBigDecimal(tempBI, this.scale + 1);
                return leftOperand.round(mc);
            }
        }
        // No optimization is done
        return subtract(subtrahend).round(mc);
    }

    /// Returns a new `BigDecimal` whose value is `this *
    /// multiplicand`. The scale of the result is the sum of the scales of the
    /// two arguments.
    ///
    /// #### Parameters
    ///
    /// - `multiplicand`: value to be multiplied with `this`.
    ///
    /// #### Returns
    ///
    /// `this * multiplicand`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `multiplicand == null`.
    public TBigDecimal multiply(TBigDecimal multiplicand) {
        long newScale = (long) this.scale + multiplicand.scale;

        if (isZero() || multiplicand.isZero()) {
            return zeroScaledBy(newScale);
        }
        /* Let be: this = [u1,s1] and multiplicand = [u2,s2] so:
         * this x multiplicand = [ s1 * s2 , s1 + s2 ] */
        if (this.bitLength + multiplicand.bitLength < 64) {
            return valueOf(this.smallValue * multiplicand.smallValue, toIntScale(newScale));
        }
        return new TBigDecimal(this.getUnscaledValue().multiply(
                multiplicand.getUnscaledValue()), toIntScale(newScale));
    }

    /// Returns a new `BigDecimal` whose value is `this *
    /// multiplicand`. The result is rounded according to the passed context
    /// `mc`.
    ///
    /// #### Parameters
    ///
    /// - `multiplicand`: value to be multiplied with `this`.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Returns
    ///
    /// `this * multiplicand`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `multiplicand == null` or `mc == null`.
    public TBigDecimal multiply(TBigDecimal multiplicand, TMathContext mc) {
        TBigDecimal result = multiply(multiplicand);

        result.inplaceRound(mc);
        return result;
    }

    /// Returns a new `BigDecimal` whose value is `this / divisor`.
    /// As scale of the result the parameter `scale` is used. If rounding
    /// is required to meet the specified scale, then the specified rounding mode
    /// `roundingMode` is applied.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// - `scale`: the scale of the result returned.
    ///
    /// - `roundingMode`: rounding mode to be used to round the result.
    ///
    /// #### Returns
    ///
    /// @return `this / divisor` rounded according to the given rounding
    /// mode.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null`.
    ///
    /// - `IllegalArgumentException`: if `roundingMode` is not a valid rounding mode.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException      if `roundingMode == ROUND_UNNECESSARY` and rounding is
    ///                                  necessary according to the given scale.
    public TBigDecimal divide(TBigDecimal divisor, int scale, int roundingMode) {
        return divide(divisor, scale, TRoundingMode.valueOf(roundingMode));
    }

    /// Returns a new `BigDecimal` whose value is `this / divisor`.
    /// As scale of the result the parameter `scale` is used. If rounding
    /// is required to meet the specified scale, then the specified rounding mode
    /// `roundingMode` is applied.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// - `scale`: the scale of the result returned.
    ///
    /// - `roundingMode`: rounding mode to be used to round the result.
    ///
    /// #### Returns
    ///
    /// @return `this / divisor` rounded according to the given rounding
    /// mode.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null` or `roundingMode == null`.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException  if `roundingMode == RoundingMode.UNNECESSAR`Y and
    ///                              rounding is necessary according to the given scale and given
    ///                              precision.
    public TBigDecimal divide(TBigDecimal divisor, int scale, TRoundingMode roundingMode) {
        // Let be: this = [u1,s1]  and  divisor = [u2,s2]
        if (roundingMode == null) {
            throw new NullPointerException();
        }
        if (divisor.isZero()) {
            throw new ArithmeticException("Division by zero");
        }

        long diffScale = ((long) this.scale - divisor.scale) - scale;
        if (this.bitLength < 64 && divisor.bitLength < 64) {
            if (diffScale == 0) {
                return dividePrimitiveLongs(this.smallValue, divisor.smallValue, scale, roundingMode);
            } else if (diffScale > 0) {
                if (diffScale < LONG_TEN_POW.length &&
                        divisor.bitLength + LONG_TEN_POW_BIT_LENGTH[(int) diffScale] < 64) {
                    return dividePrimitiveLongs(this.smallValue,
                            divisor.smallValue * LONG_TEN_POW[(int) diffScale],
                            scale,
                            roundingMode);
                }
            } else { // diffScale < 0
                if (-diffScale < LONG_TEN_POW.length &&
                        this.bitLength + LONG_TEN_POW_BIT_LENGTH[(int) -diffScale] < 64) {
                    return dividePrimitiveLongs(this.smallValue * LONG_TEN_POW[(int) -diffScale],
                            divisor.smallValue, scale, roundingMode);
                }
            }
        }
        TBigInteger scaledDividend = this.getUnscaledValue();
        TBigInteger scaledDivisor = divisor.getUnscaledValue(); // for scaling of 'u2'

        if (diffScale > 0) {
            // Multiply 'u2'  by:  10^((s1 - s2) - scale)
            scaledDivisor = TMultiplication.multiplyByTenPow(scaledDivisor, (int) diffScale);
        } else if (diffScale < 0) {
            // Multiply 'u1'  by:  10^(scale - (s1 - s2))
            scaledDividend = TMultiplication.multiplyByTenPow(scaledDividend, (int) -diffScale);
        }
        return divideBigIntegers(scaledDividend, scaledDivisor, scale, roundingMode);
    }

    /// Returns a new `BigDecimal` whose value is `this / divisor`.
    /// The scale of the result is the scale of `this`. If rounding is
    /// required to meet the specified scale, then the specified rounding mode
    /// `roundingMode` is applied.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// - `roundingMode`: rounding mode to be used to round the result.
    ///
    /// #### Returns
    ///
    /// @return `this / divisor` rounded according to the given rounding
    /// mode.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null`.
    ///
    /// - `IllegalArgumentException`: if `roundingMode` is not a valid rounding mode.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException      if `roundingMode == ROUND_UNNECESSARY` and rounding is
    ///                                  necessary according to the scale of this.
    public TBigDecimal divide(TBigDecimal divisor, int roundingMode) {
        return divide(divisor, scale, TRoundingMode.valueOf(roundingMode));
    }

    /// Returns a new `BigDecimal` whose value is `this / divisor`.
    /// The scale of the result is the scale of `this`. If rounding is
    /// required to meet the specified scale, then the specified rounding mode
    /// `roundingMode` is applied.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// - `roundingMode`: rounding mode to be used to round the result.
    ///
    /// #### Returns
    ///
    /// @return `this / divisor` rounded according to the given rounding
    /// mode.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null` or `roundingMode == null`.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException  if `roundingMode == RoundingMode.UNNECESSARY` and
    ///                              rounding is necessary according to the scale of this.
    public TBigDecimal divide(TBigDecimal divisor, TRoundingMode roundingMode) {
        return divide(divisor, scale, roundingMode);
    }

    /// Returns a new `BigDecimal` whose value is `this / divisor`.
    /// The scale of the result is the difference of the scales of `this`
    /// and `divisor`. If the exact result requires more digits, then the
    /// scale is adjusted accordingly. For example, `1/128 = 0.0078125`
    /// which has a scale of `7` and precision `5`.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// #### Returns
    ///
    /// `this / divisor`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null`.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    ///
    /// - `ArithmeticException`: if the result cannot be represented exactly.
    public TBigDecimal divide(TBigDecimal divisor) {
        TBigInteger p = this.getUnscaledValue();
        TBigInteger q = divisor.getUnscaledValue();
        TBigInteger gcd; // greatest common divisor between 'p' and 'q'
        TBigInteger[] quotAndRem;
        long diffScale = (long) scale - divisor.scale;
        int newScale; // the new scale for final quotient
        int k; // number of factors "2" in 'q'
        int l = 0; // number of factors "5" in 'q'
        int i = 1;
        int lastPow = FIVE_POW.length - 1;

        if (divisor.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        if (p.signum() == 0) {
            return zeroScaledBy(diffScale);
        }
        // To divide both by the GCD
        gcd = p.gcd(q);
        p = p.divide(gcd);
        q = q.divide(gcd);
        // To simplify all "2" factors of q, dividing by 2^k
        k = q.getLowestSetBit();
        q = q.shiftRight(k);
        // To simplify all "5" factors of q, dividing by 5^l
        do {
            quotAndRem = q.divideAndRemainder(FIVE_POW[i]);
            if (quotAndRem[1].signum() == 0) {
                l += i;
                if (i < lastPow) {
                    i++;
                }
                q = quotAndRem[0];
            } else {
                if (i == 1) {
                    break;
                }
                i = 1;
            }
        } while (true);
        // If  abs(q) != 1  then the quotient is periodic
        if (!q.abs().equals(TBigInteger.ONE)) {
            throw new ArithmeticException("Non-terminating decimal expansion; no exact representable decimal result.");
        }
        // The sign of the is fixed and the quotient will be saved in 'p'
        if (q.signum() < 0) {
            p = p.negate();
        }
        // Checking if the new scale is out of range
        newScale = toIntScale(diffScale + Math.max(k, l));
        // k >= 0  and  l >= 0  implies that  k - l  is in the 32-bit range
        i = k - l;

        p = (i > 0) ? TMultiplication.multiplyByFivePow(p, i)
                : p.shiftLeft(-i);
        return new TBigDecimal(p, newScale);
    }

    /// Returns a new `BigDecimal` whose value is `this / divisor`.
    /// The result is rounded according to the passed context `mc`. If the
    /// passed math context specifies precision `0`, then this call is
    /// equivalent to `this.divide(divisor)`.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Returns
    ///
    /// `this / divisor`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null` or `mc == null`.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException  if `mc.getRoundingMode() == UNNECESSARY` and rounding
    ///                              is necessary according `mc.getPrecision()`.
    public TBigDecimal divide(TBigDecimal divisor, TMathContext mc) {
        /* Calculating how many zeros must be append to 'dividend'
         * to obtain a  quotient with at least 'mc.precision()' digits */
        long traillingZeros = mc.getPrecision() + 2L + divisor.aproxPrecision() - aproxPrecision();
        long diffScale = (long) scale - divisor.scale;
        long newScale = diffScale; // scale of the final quotient
        int compRem; // to compare the remainder
        int i = 1; // index
        int lastPow = TEN_POW.length - 1; // last power of ten
        TBigInteger integerQuot; // for temporal results
        TBigInteger[] quotAndRem = {getUnscaledValue()};
        // In special cases it reduces the problem to call the dual method
        if ((mc.getPrecision() == 0) || (this.isZero())
                || (divisor.isZero())) {
            return this.divide(divisor);
        }
        if (traillingZeros > 0) {
            // To append trailing zeros at end of dividend
            quotAndRem[0] = getUnscaledValue().multiply(TMultiplication.powerOf10(traillingZeros));
            newScale += traillingZeros;
        }
        quotAndRem = quotAndRem[0].divideAndRemainder(divisor.getUnscaledValue());
        integerQuot = quotAndRem[0];
        // Calculating the exact quotient with at least 'mc.precision()' digits
        if (quotAndRem[1].signum() != 0) {
            // Checking if:   2 * remainder >= divisor ?
            compRem = quotAndRem[1].shiftLeftOneBit().compareTo(divisor.getUnscaledValue());
            // quot := quot * 10 + r;     with 'r' in {-6,-5,-4, 0,+4,+5,+6}
            integerQuot = integerQuot.multiply(TBigInteger.TEN)
                    .add(TBigInteger.valueOf((long) quotAndRem[0].signum() * (5 + compRem)));
            newScale++;
        } else {
            // To strip trailing zeros until the preferred scale is reached
            while (!integerQuot.testBit(0)) {
                quotAndRem = integerQuot.divideAndRemainder(TEN_POW[i]);
                if ((quotAndRem[1].signum() == 0)
                        && (newScale - i >= diffScale)) {
                    newScale -= i;
                    if (i < lastPow) {
                        i++;
                    }
                    integerQuot = quotAndRem[0];
                } else {
                    if (i == 1) {
                        break;
                    }
                    i = 1;
                }
            }
        }
        // To perform rounding
        return new TBigDecimal(integerQuot, toIntScale(newScale), mc);
    }

    /// Returns a new `BigDecimal` whose value is the integral part of
    /// `this / divisor`. The quotient is rounded down towards zero to the
    /// next integer. For example, `0.5/0.2 = 2`.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// #### Returns
    ///
    /// integral part of `this / divisor`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null`.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    public TBigDecimal divideToIntegralValue(TBigDecimal divisor) {
        TBigInteger integralValue; // the integer of result
        TBigInteger powerOfTen; // some power of ten
        long newScale = (long) this.scale - divisor.scale;
        long tempScale = 0;
        int i = 1;
        int lastPow = TEN_POW.length - 1;

        if (divisor.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        if ((divisor.aproxPrecision() + newScale > this.aproxPrecision() + 1L)
                || (this.isZero())) {
            /* If the divisor's integer part is greater than this's integer part,
             * the result must be zero with the appropriate scale */
            integralValue = TBigInteger.ZERO;
        } else if (newScale == 0) {
            integralValue = getUnscaledValue().divide(divisor.getUnscaledValue());
        } else if (newScale > 0) {
            powerOfTen = TMultiplication.powerOf10(newScale);
            integralValue = getUnscaledValue().divide(divisor.getUnscaledValue().multiply(powerOfTen));
            integralValue = integralValue.multiply(powerOfTen);
        } else {
            // (newScale < 0)
            powerOfTen = TMultiplication.powerOf10(-newScale);
            integralValue = getUnscaledValue().multiply(powerOfTen).divide(divisor.getUnscaledValue());
            // To strip trailing zeros approximating to the preferred scale
            TBigInteger[] quotAndRem;
            while (!integralValue.testBit(0)) {
                quotAndRem = integralValue.divideAndRemainder(TEN_POW[i]);
                if ((quotAndRem[1].signum() == 0)
                        && (tempScale - i >= newScale)) {
                    tempScale -= i;
                    if (i < lastPow) {
                        i++;
                    }
                    integralValue = quotAndRem[0];
                } else {
                    if (i == 1) {
                        break;
                    }
                    i = 1;
                }
            }
            newScale = tempScale;
        }
        return ((integralValue.signum() == 0)
                ? zeroScaledBy(newScale)
                : new TBigDecimal(integralValue, toIntScale(newScale)));
    }

    /// Returns a new `BigDecimal` whose value is the integral part of
    /// `this / divisor`. The quotient is rounded down towards zero to the
    /// next integer. The rounding mode passed with the parameter `mc` is
    /// not considered. But if the precision of `mc > 0` and the integral
    /// part requires more digits, then an `ArithmeticException` is thrown.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// - `mc`: @param mc      math context which determines the maximal precision of the
    ///                result.
    ///
    /// #### Returns
    ///
    /// integral part of `this / divisor`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null` or `mc == null`.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException  if `mc.getPrecision() > 0` and the result requires more
    ///                              digits to be represented.
    public TBigDecimal divideToIntegralValue(TBigDecimal divisor, TMathContext mc) {
        int mcPrecision = mc.getPrecision();
        int diffPrecision = this.precision() - divisor.precision();
        int lastPow = TEN_POW.length - 1;
        long diffScale = (long) this.scale - divisor.scale;
        long newScale = diffScale;
        long quotPrecision = diffPrecision - diffScale + 1;
        TBigInteger[] quotAndRem = new TBigInteger[2];
        // In special cases it call the dual method
        if ((mcPrecision == 0) || (this.isZero()) || (divisor.isZero())) {
            return this.divideToIntegralValue(divisor);
        }
        // Let be:   this = [u1,s1]   and   divisor = [u2,s2]
        if (quotPrecision <= 0) {
            quotAndRem[0] = TBigInteger.ZERO;
        } else if (diffScale == 0) {
            // CASE s1 == s2:  to calculate   u1 / u2
            quotAndRem[0] = this.getUnscaledValue().divide(divisor.getUnscaledValue());
        } else if (diffScale > 0) {
            // CASE s1 >= s2:  to calculate   u1 / (u2 * 10^(s1-s2)
            quotAndRem[0] = this.getUnscaledValue().divide(
                    divisor.getUnscaledValue().multiply(TMultiplication.powerOf10(diffScale)));
            // To chose  10^newScale  to get a quotient with at least 'mc.precision()' digits
            newScale = Math.min(diffScale, Math.max(mcPrecision - quotPrecision + 1, 0));
            // To calculate: (u1 / (u2 * 10^(s1-s2)) * 10^newScale
            quotAndRem[0] = quotAndRem[0].multiply(TMultiplication.powerOf10(newScale));
        } else {
            // CASE s2 > s1:
            /* To calculate the minimum power of ten, such that the quotient
             *   (u1 * 10^exp) / u2   has at least 'mc.precision()' digits. */
            long exp = Math.min(-diffScale, Math.max((long) mcPrecision - diffPrecision, 0));
            long compRemDiv;
            // Let be:   (u1 * 10^exp) / u2 = [q,r]
            quotAndRem = this.getUnscaledValue().multiply(TMultiplication.powerOf10(exp)).
                    divideAndRemainder(divisor.getUnscaledValue());
            newScale += exp; // To fix the scale
            exp = -newScale; // The remaining power of ten
            // If after division there is a remainder...
            if ((quotAndRem[1].signum() != 0) && (exp > 0)) {
                // Log10(r) + ((s2 - s1) - exp) > mc.precision ?
                compRemDiv = (new TBigDecimal(quotAndRem[1])).precision()
                        + exp - divisor.precision();
                if (compRemDiv == 0) {
                    // To calculate:  (r * 10^exp2) / u2
                    quotAndRem[1] = quotAndRem[1].multiply(TMultiplication.powerOf10(exp)).
                            divide(divisor.getUnscaledValue());
                    compRemDiv = Math.abs(quotAndRem[1].signum());
                }
                if (compRemDiv > 0) {
                    // The quotient won't fit in 'mc.precision()' digits
                    throw new ArithmeticException("Division impossible");
                }
            }
        }
        // Fast return if the quotient is zero
        if (quotAndRem[0].signum() == 0) {
            return zeroScaledBy(diffScale);
        }
        TBigInteger strippedBI = quotAndRem[0];
        TBigDecimal integralValue = new TBigDecimal(quotAndRem[0]);
        long resultPrecision = integralValue.precision();
        int i = 1;
        // To strip trailing zeros until the specified precision is reached
        while (!strippedBI.testBit(0)) {
            quotAndRem = strippedBI.divideAndRemainder(TEN_POW[i]);
            if ((quotAndRem[1].signum() == 0) &&
                    ((resultPrecision - i >= mcPrecision)
                            || (newScale - i >= diffScale))) {
                resultPrecision -= i;
                newScale -= i;
                if (i < lastPow) {
                    i++;
                }
                strippedBI = quotAndRem[0];
            } else {
                if (i == 1) {
                    break;
                }
                i = 1;
            }
        }
        // To check if the result fit in 'mc.precision()' digits
        if (resultPrecision > mcPrecision) {
            throw new ArithmeticException("Division impossible");
        }
        integralValue.scale = toIntScale(newScale);
        integralValue.setUnscaledValue(strippedBI);
        return integralValue;
    }

    /// Returns a new `BigDecimal` whose value is `this % divisor`.
    ///
    /// The remainder is defined as `this -
    /// this.divideToIntegralValue(divisor) * divisor`.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// #### Returns
    ///
    /// `this % divisor`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null`.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    public TBigDecimal remainder(TBigDecimal divisor) {
        return divideAndRemainder(divisor)[1];
    }

    /// Returns a new `BigDecimal` whose value is `this % divisor`.
    ///
    /// The remainder is defined as `this -
    /// this.divideToIntegralValue(divisor) * divisor`.
    ///
    /// The specified rounding mode `mc` is used for the division only.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// - `mc`: rounding mode and precision to be used.
    ///
    /// #### Returns
    ///
    /// `this % divisor`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null`.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException  if `mc.getPrecision() > 0` and the result of `this.divideToIntegralValue(divisor, mc)` requires more digits
    ///                              to be represented.
    public TBigDecimal remainder(TBigDecimal divisor, TMathContext mc) {
        return divideAndRemainder(divisor, mc)[1];
    }

    /// Returns a `BigDecimal` array which contains the integral part of
    /// `this / divisor` at index 0 and the remainder `this %
    /// divisor` at index 1. The quotient is rounded down towards zero to the
    /// next integer.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// #### Returns
    ///
    /// @return `[this.divideToIntegralValue(divisor),
    /// this.remainder(divisor)]`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null`.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    ///
    /// #### See also
    ///
    /// - #divideToIntegralValue
    ///
    /// - #remainder
    public TBigDecimal[] divideAndRemainder(TBigDecimal divisor) {
        TBigDecimal[] quotAndRem = new TBigDecimal[2];

        quotAndRem[0] = this.divideToIntegralValue(divisor);
        quotAndRem[1] = this.subtract(quotAndRem[0].multiply(divisor));
        return quotAndRem;
    }

    /// Returns a `BigDecimal` array which contains the integral part of
    /// `this / divisor` at index 0 and the remainder `this %
    /// divisor` at index 1. The quotient is rounded down towards zero to the
    /// next integer. The rounding mode passed with the parameter `mc` is
    /// not considered. But if the precision of `mc > 0` and the integral
    /// part requires more digits, then an `ArithmeticException` is thrown.
    ///
    /// #### Parameters
    ///
    /// - `divisor`: value by which `this` is divided.
    ///
    /// - `mc`: @param mc      math context which determines the maximal precision of the
    ///                result.
    ///
    /// #### Returns
    ///
    /// @return `[this.divideToIntegralValue(divisor),
    /// this.remainder(divisor)]`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `divisor == null`.
    ///
    /// - `ArithmeticException`: if `divisor == 0`.
    ///
    /// #### See also
    ///
    /// - #divideToIntegralValue
    ///
    /// - #remainder
    public TBigDecimal[] divideAndRemainder(TBigDecimal divisor, TMathContext mc) {
        TBigDecimal[] quotAndRem = new TBigDecimal[2];

        quotAndRem[0] = this.divideToIntegralValue(divisor, mc);
        quotAndRem[1] = this.subtract(quotAndRem[0].multiply(divisor));
        return quotAndRem;
    }

    /// Returns a new `BigDecimal` whose value is `this ^ n`. The
    /// scale of the result is `n` times the scales of `this`.
    ///
    /// `x.pow(0)` returns `1`, even if `x == 0`.
    ///
    /// Implementation Note: The implementation is based on the ANSI standard
    /// X3.274-1996 algorithm.
    ///
    /// #### Parameters
    ///
    /// - `n`: exponent to which `this` is raised.
    ///
    /// #### Returns
    ///
    /// `this ^ n`.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: if `n  999999999`.
    public TBigDecimal pow(int n) {
        if (n == 0) {
            return ONE;
        }
        if ((n < 0) || (n > 999999999)) {
            throw new ArithmeticException("Invalid Operation");
        }
        long newScale = scale * (long) n;
        // Let be: this = [u,s]   so:  this^n = [u^n, s*n]
        return ((isZero())
                ? zeroScaledBy(newScale)
                : new TBigDecimal(getUnscaledValue().pow(n), toIntScale(newScale)));
    }

    /// Returns a new `BigDecimal` whose value is `this ^ n`. The
    /// result is rounded according to the passed context `mc`.
    ///
    /// Implementation Note: The implementation is based on the ANSI standard
    /// X3.274-1996 algorithm.
    ///
    /// #### Parameters
    ///
    /// - `n`: exponent to which `this` is raised.
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Returns
    ///
    /// `this ^ n`.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: if `n  999999999`.
    public TBigDecimal pow(int n, TMathContext mc) {
        // The ANSI standard X3.274-1996 algorithm
        int m = Math.abs(n);
        int mcPrecision = mc.getPrecision();
        int elength = (int) MathUtil.log10(m) + 1;   // decimal digits in 'n'
        int oneBitMask; // mask of bits
        TBigDecimal accum; // the single accumulator
        TMathContext newPrecision = mc; // MathContext by default

        // In particular cases, it reduces the problem to call the other 'pow()'
        if ((n == 0) || ((isZero()) && (n > 0))) {
            return pow(n);
        }
        if ((m > 999999999) || ((mcPrecision == 0) && (n < 0))
                || ((mcPrecision > 0) && (elength > mcPrecision))) {
            throw new ArithmeticException("Invalid Operation");
        }
        if (mcPrecision > 0) {
            newPrecision = new TMathContext(mcPrecision + elength + 1,
                    mc.getRoundingMode());
        }
        // The result is calculated as if 'n' were positive
        accum = round(newPrecision);
        oneBitMask = highestOneBit(m) >> 1;

        while (oneBitMask > 0) {
            accum = accum.multiply(accum, newPrecision);
            if ((m & oneBitMask) == oneBitMask) {
                accum = accum.multiply(this, newPrecision);
            }
            oneBitMask >>= 1;
        }
        // If 'n' is negative, the value is divided into 'ONE'
        if (n < 0) {
            accum = ONE.divide(accum, newPrecision);
        }
        // The final value is rounded to the destination precision
        accum.inplaceRound(mc);
        return accum;
    }

    /// Returns a new `BigDecimal` whose value is the absolute value of
    /// `this`. The scale of the result is the same as the scale of this.
    ///
    /// #### Returns
    ///
    /// `abs(this)`
    public TBigDecimal abs() {
        return ((signum() < 0) ? negate() : this);
    }

    /// Returns a new `BigDecimal` whose value is the absolute value of
    /// `this`. The result is rounded according to the passed context
    /// `mc`.
    ///
    /// #### Parameters
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Returns
    ///
    /// `abs(this)`
    public TBigDecimal abs(TMathContext mc) {
        return round(mc).abs();
    }

    /// Returns a new `BigDecimal` whose value is the `-this`. The
    /// scale of the result is the same as the scale of this.
    ///
    /// #### Returns
    ///
    /// `-this`
    public TBigDecimal negate() {
        if (bitLength < 63 || (bitLength == 63 && smallValue != Long.MIN_VALUE)) {
            return valueOf(-smallValue, scale);
        }
        return new TBigDecimal(getUnscaledValue().negate(), scale);
    }

    /// Returns a new `BigDecimal` whose value is the `-this`. The
    /// result is rounded according to the passed context `mc`.
    ///
    /// #### Parameters
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Returns
    ///
    /// `-this`
    public TBigDecimal negate(TMathContext mc) {
        return round(mc).negate();
    }

    /// Returns a new `BigDecimal` whose value is `+this`. The scale
    /// of the result is the same as the scale of this.
    ///
    /// #### Returns
    ///
    /// `this`
    public TBigDecimal plus() {
        return this;
    }

    /// Returns a new `BigDecimal` whose value is `+this`. The result
    /// is rounded according to the passed context `mc`.
    ///
    /// #### Parameters
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Returns
    ///
    /// `this`, rounded
    public TBigDecimal plus(TMathContext mc) {
        return round(mc);
    }

    /// Returns the sign of this `BigDecimal`.
    ///
    /// #### Returns
    ///
    /// `-1` if `this  0`.
    public int signum() {
        if (bitLength < 64) {
            return signum(this.smallValue);
        }
        return getUnscaledValue().signum();
    }

    private boolean isZero() {
        //Watch out: -1 has a bitLength=0
        return bitLength == 0 && this.smallValue != -1;
    }

    /// Returns the scale of this `BigDecimal`. The scale is the number of
    /// digits behind the decimal point. The value of this `BigDecimal` is
    /// the unsignedValue * 10^(-scale). If the scale is negative, then this
    /// `BigDecimal` represents a big integer.
    ///
    /// #### Returns
    ///
    /// the scale of this `BigDecimal`.
    public int scale() {
        return scale;
    }

    /// Returns the precision of this `BigDecimal`. The precision is the
    /// number of decimal digits used to represent this decimal. It is equivalent
    /// to the number of digits of the unscaled value. The precision of `0`
    /// is `1` (independent of the scale).
    ///
    /// #### Returns
    ///
    /// the precision of this `BigDecimal`.
    public int precision() {
        // Checking if the precision already was calculated
        if (precision > 0) {
            return precision;
        }
        int bitLength = this.bitLength;
        int decimalDigits = 1; // the precision to be calculated
        double doubleUnsc = 1;  // intVal in 'double'

        if (bitLength < 1024) {
            // To calculate the precision for small numbers
            if (bitLength >= 64) {
                doubleUnsc = getUnscaledValue().doubleValue();
            } else if (bitLength >= 1) {
                doubleUnsc = smallValue;
            }
            decimalDigits += MathUtil.log10(Math.abs(doubleUnsc));
        } else {
            // (bitLength >= 1024)
            /* To calculate the precision for large numbers
             * Note that: 2 ^(bitlength() - 1) <= intVal < 10 ^(precision()) */
            decimalDigits += (bitLength - 1) * LOG10_2;
            // If after division the number isn't zero, exists an aditional digit
            if (getUnscaledValue().divide(TMultiplication.powerOf10(decimalDigits)).signum() != 0) {
                decimalDigits++;
            }
        }
        precision = decimalDigits;
        return precision;
    }

    /// Returns the unscaled value (mantissa) of this `BigDecimal` instance
    /// as a `BigInteger`. The unscaled value can be computed as `this` 10^(scale).
    ///
    /// #### Returns
    ///
    /// unscaled value (this * 10^(scale)).
    public TBigInteger unscaledValue() {
        return getUnscaledValue();
    }

    /// Returns a new `BigDecimal` whose value is `this`, rounded
    /// according to the passed context `mc`.
    ///
    /// If `mc.precision = 0`, then no rounding is performed.
    ///
    /// If `mc.precision > 0` and `mc.roundingMode == UNNECESSARY`,
    /// then an `ArithmeticException` is thrown if the result cannot be
    /// represented exactly within the given precision.
    ///
    /// #### Parameters
    ///
    /// - `mc`: rounding mode and precision for the result of this operation.
    ///
    /// #### Returns
    ///
    /// `this` rounded according to the passed context.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: @throws ArithmeticException if `mc.precision > 0` and `mc.roundingMode ==
    ///                             UNNECESSARY` and this cannot be represented within the given
    ///                             precision.
    public TBigDecimal round(TMathContext mc) {
        TBigDecimal thisBD = new TBigDecimal(getUnscaledValue(), scale);

        thisBD.inplaceRound(mc);
        return thisBD;
    }

    /// Returns a new `BigDecimal` instance with the specified scale.
    ///
    /// If the new scale is greater than the old scale, then additional zeros are
    /// added to the unscaled value. In this case no rounding is necessary.
    ///
    /// If the new scale is smaller than the old scale, then trailing digits are
    /// removed. If these trailing digits are not zero, then the remaining
    /// unscaled value has to be rounded. For this rounding operation the
    /// specified rounding mode is used.
    ///
    /// #### Parameters
    ///
    /// - `newScale`: scale of the result returned.
    ///
    /// - `roundingMode`: rounding mode to be used to round the result.
    ///
    /// #### Returns
    ///
    /// a new `BigDecimal` instance with the specified scale.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `roundingMode == null`.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException  if `roundingMode == ROUND_UNNECESSARY` and rounding is
    ///                              necessary according to the given scale.
    public TBigDecimal setScale(int newScale, TRoundingMode roundingMode) {
        if (roundingMode == null) {
            throw new NullPointerException();
        }
        long diffScale = newScale - (long) scale;
        // Let be:  'this' = [u,s]
        if (diffScale == 0) {
            return this;
        }
        if (diffScale > 0) {
            // return  [u * 10^(s2 - s), newScale]
            if (diffScale < LONG_TEN_POW.length &&
                    (this.bitLength + LONG_TEN_POW_BIT_LENGTH[(int) diffScale]) < 64) {
                return valueOf(this.smallValue * LONG_TEN_POW[(int) diffScale], newScale);
            }
            return new TBigDecimal(TMultiplication.multiplyByTenPow(getUnscaledValue(), (int) diffScale), newScale);
        }
        // diffScale < 0
        // return  [u,s] / [1,newScale]  with the appropriate scale and rounding
        if (this.bitLength < 64 && -diffScale < LONG_TEN_POW.length) {
            return dividePrimitiveLongs(this.smallValue, LONG_TEN_POW[(int) -diffScale], newScale, roundingMode);
        }
        return divideBigIntegers(this.getUnscaledValue(), TMultiplication.powerOf10(-diffScale), newScale, roundingMode);
    }

    /// Returns a new `BigDecimal` instance with the specified scale.
    ///
    /// If the new scale is greater than the old scale, then additional zeros are
    /// added to the unscaled value. In this case no rounding is necessary.
    ///
    /// If the new scale is smaller than the old scale, then trailing digits are
    /// removed. If these trailing digits are not zero, then the remaining
    /// unscaled value has to be rounded. For this rounding operation the
    /// specified rounding mode is used.
    ///
    /// #### Parameters
    ///
    /// - `newScale`: scale of the result returned.
    ///
    /// - `roundingMode`: rounding mode to be used to round the result.
    ///
    /// #### Returns
    ///
    /// a new `BigDecimal` instance with the specified scale.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `roundingMode` is not a valid rounding mode.
    ///
    /// - `ArithmeticException`: @throws ArithmeticException      if `roundingMode == ROUND_UNNECESSARY` and rounding is
    ///                                  necessary according to the given scale.
    public TBigDecimal setScale(int newScale, int roundingMode) {
        return setScale(newScale, TRoundingMode.valueOf(roundingMode));
    }

    /// Returns a new `BigDecimal` instance with the specified scale. If
    /// the new scale is greater than the old scale, then additional zeros are
    /// added to the unscaled value. If the new scale is smaller than the old
    /// scale, then trailing zeros are removed. If the trailing digits are not
    /// zeros then an ArithmeticException is thrown.
    ///
    /// If no exception is thrown, then the following equation holds: `x.setScale(s).compareTo(x) == 0`.
    ///
    /// #### Parameters
    ///
    /// - `newScale`: scale of the result returned.
    ///
    /// #### Returns
    ///
    /// a new `BigDecimal` instance with the specified scale.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: if rounding would be necessary.
    public TBigDecimal setScale(int newScale) {
        return setScale(newScale, TRoundingMode.UNNECESSARY);
    }

    /// Returns a new `BigDecimal` instance where the decimal point has
    /// been moved `n` places to the left. If `n = 0`.
    ///
    /// #### Parameters
    ///
    /// - `n`: number of placed the decimal point has to be moved.
    ///
    /// #### Returns
    ///
    /// `this * 10^(-n`).
    public TBigDecimal movePointLeft(int n) {
        return movePoint(scale + (long) n);
    }

    private TBigDecimal movePoint(long newScale) {
        if (isZero()) {
            return zeroScaledBy(Math.max(newScale, 0));
        }
        /* When:  'n'== Integer.MIN_VALUE  isn't possible to call to movePointRight(-n)
         * since  -Integer.MIN_VALUE == Integer.MIN_VALUE */
        if (newScale >= 0) {
            if (bitLength < 64) {
                return valueOf(smallValue, toIntScale(newScale));
            }
            return new TBigDecimal(getUnscaledValue(), toIntScale(newScale));
        }
        if (-newScale < LONG_TEN_POW.length &&
                bitLength + LONG_TEN_POW_BIT_LENGTH[(int) -newScale] < 64) {
            return valueOf(smallValue * LONG_TEN_POW[(int) -newScale], 0);
        }
        return new TBigDecimal(TMultiplication.multiplyByTenPow(getUnscaledValue(), (int) -newScale), 0);
    }

    /// Returns a new `BigDecimal` instance where the decimal point has
    /// been moved `n` places to the right. If `n = 0.
    ///
    /// #### Parameters
    ///
    /// - `n`: number of placed the decimal point has to be moved.
    ///
    /// #### Returns
    ///
    /// `this * 10^n`.
    public TBigDecimal movePointRight(int n) {
        return movePoint(scale - (long) n);
    }

    /// Returns a new `BigDecimal` whose value is `this` 10^`n`.
    /// The scale of the result is `this.scale()` - `n`.
    /// The precision of the result is the precision of `this`.
    ///
    /// This method has the same effect as `#movePointRight`, except that
    /// the precision is not changed.
    ///
    /// #### Parameters
    ///
    /// - `n`: number of places the decimal point has to be moved.
    ///
    /// #### Returns
    ///
    /// `this * 10^n`
    public TBigDecimal scaleByPowerOfTen(int n) {
        long newScale = scale - (long) n;
        if (bitLength < 64) {
            //Taking care when a 0 is to be scaled
            if (smallValue == 0) {
                return zeroScaledBy(newScale);
            }
            return valueOf(smallValue, toIntScale(newScale));
        }
        return new TBigDecimal(getUnscaledValue(), toIntScale(newScale));
    }

    /// Returns a new `BigDecimal` instance with the same value as `this` but with a unscaled value where the trailing zeros have been
    /// removed. If the unscaled value of `this` has n trailing zeros, then
    /// the scale and the precision of the result has been reduced by n.
    ///
    /// #### Returns
    ///
    /// @return a new `BigDecimal` instance equivalent to this where the
    /// trailing zeros of the unscaled value have been removed.
    public TBigDecimal stripTrailingZeros() {
        int i = 1; // 1 <= i <= 18
        int lastPow = TEN_POW.length - 1;
        long newScale = scale;

        if (isZero()) {
            return new TBigDecimal("0");
        }
        TBigInteger strippedBI = getUnscaledValue();
        TBigInteger[] quotAndRem;

        // while the number is even...
        while (!strippedBI.testBit(0)) {
            // To divide by 10^i
            quotAndRem = strippedBI.divideAndRemainder(TEN_POW[i]);
            // To look the remainder
            if (quotAndRem[1].signum() == 0) {
                // To adjust the scale
                newScale -= i;
                if (i < lastPow) {
                    // To set to the next power
                    i++;
                }
                strippedBI = quotAndRem[0];
            } else {
                if (i == 1) {
                    // 'this' has no more trailing zeros
                    break;
                }
                // To set to the smallest power of ten
                i = 1;
            }
        }
        return new TBigDecimal(strippedBI, toIntScale(newScale));
    }

    /// Compares this `BigDecimal` with `val`. Returns one of the
    /// three values `1`, `0`, or `-1`. The method behaves as
    /// if `this.subtract(val)` is computed. If this difference is > 0 then
    /// 1 is returned, if the difference is  val`, `-1` if `this < val`,
    /// `0` if `this == val`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `val == null`.
    //@Override
    public int compareTo(TBigDecimal val) {
        int thisSign = signum();
        int valueSign = val.signum();

        if (thisSign == valueSign) {
            if (this.scale == val.scale && this.bitLength < 64 && val.bitLength < 64) {
                return (smallValue < val.smallValue) ? -1 : (smallValue > val.smallValue) ? 1 : 0;
            }
            long diffScale = (long) this.scale - val.scale;
            int diffPrecision = this.aproxPrecision() - val.aproxPrecision();
            if (diffPrecision > diffScale + 1) {
                return thisSign;
            } else if (diffPrecision < diffScale - 1) {
                return -thisSign;
            } else {
                // thisSign == val.signum()  and  diffPrecision is aprox. diffScale
                TBigInteger thisUnscaled = this.getUnscaledValue();
                TBigInteger valUnscaled = val.getUnscaledValue();
                // If any of both precision is bigger, append zeros to the shorter one
                if (diffScale < 0) {
                    thisUnscaled = thisUnscaled.multiply(TMultiplication.powerOf10(-diffScale));
                } else if (diffScale > 0) {
                    valUnscaled = valUnscaled.multiply(TMultiplication.powerOf10(diffScale));
                }
                return thisUnscaled.compareTo(valUnscaled);
            }
        } else if (thisSign < valueSign) {
            return -1;
        } else {
            return 1;
        }
    }

    /// Returns `true` if `x` is a `BigDecimal` instance and if
    /// this instance is equal to this big decimal. Two big decimals are equal if
    /// their unscaled value and their scale is equal. For example, 1.0
    /// (10*10^(-1)) is not equal to 1.00 (100*10^(-2)). Similarly, zero
    /// instances are not equal if their scale differs.
    ///
    /// #### Parameters
    ///
    /// - `x`: object to be compared with `this`.
    ///
    /// #### Returns
    ///
    /// true if `x` is a `BigDecimal` and `this == x`.
    @Override
    public boolean equals(Object x) {
        if (this == x) {
            return true;
        }
        if (x instanceof TBigDecimal) {
            TBigDecimal x1 = (TBigDecimal) x;
            return x1.scale == scale
                    && (bitLength < 64 ? (x1.smallValue == smallValue)
                    : intVal.equals(x1.intVal));


        }
        return false;
    }

    /// Returns the minimum of this `BigDecimal` and `val`.
    ///
    /// #### Parameters
    ///
    /// - `val`: value to be used to compute the minimum with this.
    ///
    /// #### Returns
    ///
    /// `min(this, val`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `val == null`.
    public TBigDecimal min(TBigDecimal val) {
        return ((compareTo(val) <= 0) ? this : val);
    }

    /// Returns the maximum of this `BigDecimal` and `val`.
    ///
    /// #### Parameters
    ///
    /// - `val`: value to be used to compute the maximum with this.
    ///
    /// #### Returns
    ///
    /// `max(this, val`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `val == null`.
    public TBigDecimal max(TBigDecimal val) {
        return ((compareTo(val) >= 0) ? this : val);
    }

    /// Returns a hash code for this `BigDecimal`.
    ///
    /// #### Returns
    ///
    /// hash code for `this`.
    @Override
    public int hashCode() {
        if (hashCode != 0) {
            return hashCode;
        }
        if (bitLength < 64) {
            hashCode = (int) (smallValue & 0xffffffff);
            hashCode = 33 * hashCode + (int) ((smallValue >> 32) & 0xffffffff);
            hashCode = 17 * hashCode + scale;
            return hashCode;
        }
        hashCode = 17 * intVal.hashCode() + scale;
        return hashCode;
    }

    /// Returns a canonical string representation of this `BigDecimal`. If
    /// necessary, scientific notation is used. This representation always prints
    /// all significant digits of this value.
    ///
    /// If the scale is negative or if `scale - precision >= 6` then
    /// scientific notation is used.
    ///
    /// #### Returns
    ///
    /// @return a string representation of `this` in scientific notation if
    /// necessary.
    @Override
    public String toString() {
        if (toStringImage != null) {
            return toStringImage;
        }
        if (bitLength < 32) {
            toStringImage = TConversion.toDecimalScaledString(smallValue, scale);
            return toStringImage;
        }
        String intString = getUnscaledValue().toString();
        if (scale == 0) {
            return intString;
        }
        int begin = (getUnscaledValue().signum() < 0) ? 2 : 1;
        int end = intString.length();
        long exponent = -(long) scale + end - begin;
        StringBuilder result = new StringBuilder();

        result.append(intString);
        if ((scale > 0) && (exponent >= -6)) {
            if (exponent >= 0) {
                result.insert(end - scale, '.');
            } else {
                result.insert(begin - 1, "0.");

                result.insert(begin + 1, String.valueOf(CH_ZEROS).substring(0, -(int) exponent - 1));
            }
        } else {
            if (end - begin >= 1) {
                result.insert(begin, '.');
                end++;
            }
            result.insert(end, 'E');
            if (exponent > 0) {
                result.insert(++end, '+');
            }
            result.insert(++end, exponent);
        }
        toStringImage = result.toString();
        return toStringImage;
    }

    /// Returns a string representation of this `BigDecimal`. This
    /// representation always prints all significant digits of this value.
    ///
    /// If the scale is negative or if `scale - precision >= 6` then
    /// engineering notation is used. Engineering notation is similar to the
    /// scientific notation except that the exponent is made to be a multiple of
    /// 3 such that the integer part is >= 1 and < 1000.
    ///
    /// #### Returns
    ///
    /// @return a string representation of `this` in engineering notation
    /// if necessary.
    public String toEngineeringString() {
        String intString = getUnscaledValue().toString();
        if (scale == 0) {
            return intString;
        }
        int begin = (getUnscaledValue().signum() < 0) ? 2 : 1;
        int end = intString.length();
        long exponent = -(long) scale + end - begin;
        StringBuilder result = new StringBuilder(intString);

        if ((scale > 0) && (exponent >= -6)) {
            if (exponent >= 0) {
                result.insert(end - scale, '.');
            } else {
                result.insert(begin - 1, "0."); //$NON-NLS-1$
                result.insert(begin + 1, String.valueOf(CH_ZEROS).substring(0, -(int) exponent - 1));
            }
        } else {
            int delta = end - begin;
            int rem = (int) (exponent % 3);

            if (rem != 0) {
                // adjust exponent so it is a multiple of three
                if (getUnscaledValue().signum() == 0) {
                    // zero value
                    rem = (rem < 0) ? -rem : 3 - rem;
                    exponent += rem;
                } else {
                    // nonzero value
                    rem = (rem < 0) ? rem + 3 : rem;
                    exponent -= rem;
                    begin += rem;
                }
                if (delta < 3) {
                    for (int i = rem - delta; i > 0; i--) {
                        result.insert(end++, '0');
                    }
                }
            }
            if (end - begin >= 1) {
                result.insert(begin, '.');
                end++;
            }
            if (exponent != 0) {
                result.insert(end, 'E');
                if (exponent > 0) {
                    result.insert(++end, '+');
                }
                result.insert(++end, exponent);
            }
        }
        return result.toString();
    }

    /// Returns a string representation of this `BigDecimal`. No scientific
    /// notation is used. This methods adds zeros where necessary.
    ///
    /// If this string representation is used to create a new instance, this
    /// instance is generally not identical to `this` as the precision
    /// changes.
    ///
    /// `x.equals(new BigDecimal(x.toPlainString())` usually returns
    /// `false`.
    ///
    /// `x.compareTo(new BigDecimal(x.toPlainString())` returns `0`.
    ///
    /// #### Returns
    ///
    /// a string representation of `this` without exponent part.
    public String toPlainString() {
        String intStr = getUnscaledValue().toString();
        if ((scale == 0) || ((isZero()) && (scale < 0))) {
            return intStr;
        }
        int begin = (signum() < 0) ? 1 : 0;
        int delta = scale;
        // We take space for all digits, plus a possible decimal point, plus 'scale'
        StringBuilder result = new StringBuilder(intStr.length() + 1 + Math.abs(scale));

        if (begin == 1) {
            // If the number is negative, we insert a '-' character at front
            result.append('-');
        }
        if (scale > 0) {
            delta -= (intStr.length() - begin);
            if (delta >= 0) {
                result.append("0."); //$NON-NLS-1$
                // To append zeros after the decimal point
                for (; delta > CH_ZEROS.length; delta -= CH_ZEROS.length) {
                    result.append(CH_ZEROS);
                }
                result.append(CH_ZEROS, 0, delta);
                result.append(intStr.substring(begin));
            } else {
                delta = begin - delta;
                result.append(intStr.substring(begin, delta));
                result.append('.');
                result.append(intStr.substring(delta));
            }
        } else {
            // (scale <= 0)
            result.append(intStr.substring(begin));
            // To append trailing zeros
            for (; delta < -CH_ZEROS.length; delta += CH_ZEROS.length) {
                result.append(CH_ZEROS);
            }
            result.append(CH_ZEROS, 0, -delta);
        }
        return result.toString();
    }

    /// Returns this `BigDecimal` as a big integer instance. A fractional
    /// part is discarded.
    ///
    /// #### Returns
    ///
    /// this `BigDecimal` as a big integer instance.
    public TBigInteger toBigInteger() {
        if ((scale == 0) || (isZero())) {
            return getUnscaledValue();
        } else if (scale < 0) {
            return getUnscaledValue().multiply(TMultiplication.powerOf10(-(long) scale));
        } else {
            // (scale > 0)
            return getUnscaledValue().divide(TMultiplication.powerOf10(scale));
        }
    }

    /// Returns this `BigDecimal` as a big integer instance if it has no
    /// fractional part. If this `BigDecimal` has a fractional part, i.e.
    /// if rounding would be necessary, an `ArithmeticException` is thrown.
    ///
    /// #### Returns
    ///
    /// this `BigDecimal` as a big integer value.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: if rounding is necessary.
    public TBigInteger toBigIntegerExact() {
        if ((scale == 0) || (isZero())) {
            return getUnscaledValue();
        } else if (scale < 0) {
            return getUnscaledValue().multiply(TMultiplication.powerOf10(-(long) scale));
        } else {
            // (scale > 0)
            TBigInteger[] integerAndFraction;
            // An optimization before do a heavy division
            if ((scale > aproxPrecision()) || (scale > getUnscaledValue().getLowestSetBit())) {
                throw new ArithmeticException("Rounding necessary");
            }
            integerAndFraction = getUnscaledValue().divideAndRemainder(TMultiplication.powerOf10(scale));
            if (integerAndFraction[1].signum() != 0) {
                // It exists a non-zero fractional part
                throw new ArithmeticException("Rounding necessary");
            }
            return integerAndFraction[0];
        }
    }

    /// Returns this `BigDecimal` as a long value. Any fractional part is
    /// discarded. If the integral part of `this` is too big to be
    /// represented as a long, then `this` % 2^64 is returned.
    ///
    /// #### Returns
    ///
    /// this `BigDecimal` as a long value.
    //@Override
    public long longValue() {
        /* If scale <= -64 there are at least 64 trailing bits zero in 10^(-scale).
         * If the scale is positive and very large the long value could be zero. */
        return ((scale <= -64) || (scale > aproxPrecision())
                ? 0L
                : toBigInteger().longValue());
    }

    /// Returns this `BigDecimal` as a long value if it has no fractional
    /// part and if its value fits to the int range ([-2^{63}..2^{63}-1]). If
    /// these conditions are not met, an `ArithmeticException` is thrown.
    ///
    /// #### Returns
    ///
    /// this `BigDecimal` as a long value.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: if rounding is necessary or the number doesn't fit in a long.
    public long longValueExact() {
        return valueExact(64);
    }

    /// Returns this `BigDecimal` as an int value. Any fractional part is
    /// discarded. If the integral part of `this` is too big to be
    /// represented as an int, then `this` % 2^32 is returned.
    ///
    /// #### Returns
    ///
    /// this `BigDecimal` as a int value.
    //@Override
    public int intValue() {
        /* If scale <= -32 there are at least 32 trailing bits zero in 10^(-scale).
         * If the scale is positive and very large the long value could be zero. */
        return ((scale <= -32) || (scale > aproxPrecision())
                ? 0
                : toBigInteger().intValue());
    }

    /* Private Methods */

    /// Returns this `BigDecimal` as a int value if it has no fractional
    /// part and if its value fits to the int range ([-2^{31}..2^{31}-1]). If
    /// these conditions are not met, an `ArithmeticException` is thrown.
    ///
    /// #### Returns
    ///
    /// this `BigDecimal` as a int value.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: if rounding is necessary or the number doesn't fit in a int.
    public int intValueExact() {
        return (int) valueExact(32);
    }

    /// Returns this `BigDecimal` as a short value if it has no fractional
    /// part and if its value fits to the short range ([-2^{15}..2^{15}-1]). If
    /// these conditions are not met, an `ArithmeticException` is thrown.
    ///
    /// #### Returns
    ///
    /// this `BigDecimal` as a short value.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: @throws ArithmeticException if rounding is necessary of the number doesn't fit in a
    ///                             short.
    public short shortValueExact() {
        return (short) valueExact(16);
    }

    /// Returns this `BigDecimal` as a byte value if it has no fractional
    /// part and if its value fits to the byte range ([-128..127]). If these
    /// conditions are not met, an `ArithmeticException` is thrown.
    ///
    /// #### Returns
    ///
    /// this `BigDecimal` as a byte value.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: if rounding is necessary or the number doesn't fit in a byte.
    public byte byteValueExact() {
        return (byte) valueExact(8);
    }

    /// Returns this `BigDecimal` as a float value. If `this` is too
    /// big to be represented as a float, then `Float.POSITIVE_INFINITY`
    /// or `Float.NEGATIVE_INFINITY` is returned.
    ///
    /// Note, that if the unscaled value has more than 24 significant digits,
    /// then this decimal cannot be represented exactly in a float variable. In
    /// this case the result is rounded.
    ///
    /// For example, if the instance `x1 = new BigDecimal("0.1")` cannot be
    /// represented exactly as a float, and thus `x1.equals(new
    /// BigDecimal(x1.folatValue())` returns `false` for this case.
    ///
    /// Similarly, if the instance `new BigDecimal(16777217)` is converted
    /// to a float, the result is `1.6777216E`7.
    ///
    /// #### Returns
    ///
    /// this `BigDecimal` as a float value.
    //@Override
    public float floatValue() {
        /* A similar code like in doubleValue() could be repeated here,
         * but this simple implementation is quite efficient. */
        float floatResult = signum();
        long powerOfTwo = this.bitLength - (long) (scale / LOG10_2);
        if ((powerOfTwo < -149) || (floatResult == 0.0f)) {
            // Cases which 'this' is very small
            floatResult *= 0.0f;
        } else if (powerOfTwo > 129) {
            // Cases which 'this' is very large
            floatResult *= Float.POSITIVE_INFINITY;
        } else {
            floatResult = (float) doubleValue();
        }
        return floatResult;
    }

    /// Returns this `BigDecimal` as a double value. If `this` is too
    /// big to be represented as a float, then `Double.POSITIVE_INFINITY`
    /// or `Double.NEGATIVE_INFINITY` is returned.
    ///
    /// Note, that if the unscaled value has more than 53 significant digits,
    /// then this decimal cannot be represented exactly in a double variable. In
    /// this case the result is rounded.
    ///
    /// For example, if the instance `x1 = new BigDecimal("0.1")` cannot be
    /// represented exactly as a double, and thus `x1.equals(new
    /// BigDecimal(x1.doubleValue())` returns `false` for this case.
    ///
    /// Similarly, if the instance `new BigDecimal(9007199254740993L)` is
    /// converted to a double, the result is `9.007199254740992E15`.
    ///
    /// #### Returns
    ///
    /// this `BigDecimal` as a double value.
    //@Override
    public double doubleValue() {
        int sign = signum();
        int exponent = 1076; // bias + 53
        int lowestSetBit;
        int discardedSize;
        long powerOfTwo = this.bitLength - (long) (scale / LOG10_2);
        long bits; // IEEE-754 Standard
        long tempBits; // for temporal calculations
        TBigInteger mantisa;

        if ((powerOfTwo < -1074) || (sign == 0)) {
            // Cases which 'this' is very small
            return (sign * 0.0d);
        } else if (powerOfTwo > 1025) {
            // Cases which 'this' is very large
            return (sign * Double.POSITIVE_INFINITY);
        }
        mantisa = getUnscaledValue().abs();
        // Let be:  this = [u,s], with s > 0
        if (scale <= 0) {
            // mantisa = abs(u) * 10^s
            mantisa = mantisa.multiply(TMultiplication.powerOf10(-scale));
        } else {
            // (scale > 0)
            TBigInteger[] quotAndRem;
            TBigInteger powerOfTen = TMultiplication.powerOf10(scale);
            int k = 100 - (int) powerOfTwo;
            int compRem;

            if (k > 0) {
                /* Computing (mantisa * 2^k) , where 'k' is a enough big
                 * power of '2' to can divide by 10^s */
                mantisa = mantisa.shiftLeft(k);
                exponent -= k;
            }
            // Computing (mantisa * 2^k) / 10^s
            quotAndRem = mantisa.divideAndRemainder(powerOfTen);
            // To check if the fractional part >= 0.5
            compRem = quotAndRem[1].shiftLeftOneBit().compareTo(powerOfTen);
            // To add two rounded bits at end of mantisa
            mantisa = quotAndRem[0].shiftLeft(2).add(
                    TBigInteger.valueOf(((long) compRem * (compRem + 3)) / 2 + 1));
            exponent -= 2;
        }
        lowestSetBit = mantisa.getLowestSetBit();
        discardedSize = mantisa.bitLength() - 54;
        if (discardedSize > 0) {
            // (n > 54)
            // mantisa = (abs(u) * 10^s) >> (n - 54)
            bits = mantisa.shiftRight(discardedSize).longValue();
            tempBits = bits;
            // #bits = 54, to check if the discarded fraction produces a carry
            if ((((bits & 1) == 1) && (lowestSetBit < discardedSize))
                    || ((bits & 3) == 3)) {
                bits += 2;
            }
        } else {
            // (n <= 54)
            // mantisa = (abs(u) * 10^s) << (54 - n)
            bits = mantisa.longValue() << -discardedSize;
            tempBits = bits;
            // #bits = 54, to check if the discarded fraction produces a carry:
            if ((bits & 3) == 3) {
                bits += 2;
            }
        }
        // Testing bit 54 to check if the carry creates a new binary digit
        if ((bits & 0x40000000000000L) == 0) {
            // To drop the last bit of mantisa (first discarded)
            bits >>= 1;
            // exponent = 2^(s-n+53+bias)
            exponent += discardedSize;
        } else {
            // #bits = 54
            bits >>= 2;
            exponent += discardedSize + 1;
        }
        // To test if the 53-bits number fits in 'double'
        if (exponent > 2046) {
            // (exponent - bias > 1023)
            return (sign * Double.POSITIVE_INFINITY);
        } else if (exponent <= 0) {
            // (exponent - bias <= -1023)
            // Denormalized numbers (having exponent == 0)
            if (exponent < -53) {
                // exponent - bias < -1076
                return (sign * 0.0d);
            }
            // -1076 <= exponent - bias <= -1023
            // To discard '- exponent + 1' bits
            bits = tempBits >> 1;
            tempBits = bits & (-1L >>> (63 + exponent));
            bits >>= (-exponent);
            // To test if after discard bits, a new carry is generated
            if (((bits & 3) == 3) || (((bits & 1) == 1) && (tempBits != 0)
                    && (lowestSetBit < discardedSize))) {
                bits += 1;
            }
            exponent = 0;
            bits >>= 1;
        }
        // Construct the 64 double bits: [sign(1), exponent(11), mantisa(52)]
        bits = (sign & 0x8000000000000000L) | ((long) exponent << 52) | (bits & 0xFFFFFFFFFFFFFL);
        return Double.longBitsToDouble(bits);
    }

    /// Returns the unit in the last place (ULP) of this `BigDecimal`
    /// instance. An ULP is the distance to the nearest big decimal with the same
    /// precision.
    ///
    /// The amount of a rounding error in the evaluation of a floating-point
    /// operation is often expressed in ULPs. An error of 1 ULP is often seen as
    /// a tolerable error.
    ///
    /// For class `BigDecimal`, the ULP of a number is simply 10^(-scale).
    ///
    /// For example, `new BigDecimal(0.1).ulp()` returns `1E-55`.
    ///
    /// #### Returns
    ///
    /// unit in the last place (ULP) of this `BigDecimal` instance.
    public TBigDecimal ulp() {
        return valueOf(1, scale);
    }

    /// It does all rounding work of the public method
    /// `round(MathContext)`, performing an inplace rounding
    /// without creating a new object.
    ///
    /// #### Parameters
    ///
    /// - `mc`: the `MathContext` for perform the rounding.
    ///
    /// #### See also
    ///
    /// - #round(TMathContext)
    private void inplaceRound(TMathContext mc) {
        int mcPrecision = mc.getPrecision();
        if (aproxPrecision() - mcPrecision <= 0 || mcPrecision == 0) {
            return;
        }
        int discardedPrecision = precision() - mcPrecision;
        // If no rounding is necessary it returns immediately
        if ((discardedPrecision <= 0)) {
            return;
        }
        // When the number is small perform an efficient rounding
        if (this.bitLength < 64) {
            smallRound(mc, discardedPrecision);
            return;
        }
        // Getting the integer part and the discarded fraction
        TBigInteger sizeOfFraction = TMultiplication.powerOf10(discardedPrecision);
        TBigInteger[] integerAndFraction = getUnscaledValue().divideAndRemainder(sizeOfFraction);
        long newScale = (long) scale - discardedPrecision;
        int compRem;
        TBigDecimal tempBD;
        // If the discarded fraction is non-zero, perform rounding
        if (integerAndFraction[1].signum() != 0) {
            // To check if the discarded fraction >= 0.5
            compRem = (integerAndFraction[1].abs().shiftLeftOneBit().compareTo(sizeOfFraction));
            // To look if there is a carry
            compRem = roundingBehavior(integerAndFraction[0].testBit(0) ? 1 : 0,
                    integerAndFraction[1].signum() * (5 + compRem),
                    mc.getRoundingMode());
            if (compRem != 0) {
                integerAndFraction[0] = integerAndFraction[0].add(TBigInteger.valueOf(compRem));
            }
            tempBD = new TBigDecimal(integerAndFraction[0]);
            // If after to add the increment the precision changed, we normalize the size
            if (tempBD.precision() > mcPrecision) {
                integerAndFraction[0] = integerAndFraction[0].divide(TBigInteger.TEN);
                newScale--;
            }
        }
        // To update all internal fields
        scale = toIntScale(newScale);
        precision = mcPrecision;
        setUnscaledValue(integerAndFraction[0]);
    }

    /// This method implements an efficient rounding for numbers which unscaled
    /// value fits in the type `long`.
    ///
    /// #### Parameters
    ///
    /// - `mc`: the context to use
    ///
    /// - `discardedPrecision`: the number of decimal digits that are discarded
    ///
    /// #### See also
    ///
    /// - #round(TMathContext)
    private void smallRound(TMathContext mc, int discardedPrecision) {
        long sizeOfFraction = LONG_TEN_POW[discardedPrecision];
        long newScale = (long) scale - discardedPrecision;
        long unscaledVal = smallValue;
        // Getting the integer part and the discarded fraction
        long integer = unscaledVal / sizeOfFraction;
        long fraction = unscaledVal % sizeOfFraction;
        int compRem;
        // If the discarded fraction is non-zero perform rounding
        if (fraction != 0) {
            // To check if the discarded fraction >= 0.5
            compRem = longCompareTo(Math.abs(fraction) << 1, sizeOfFraction);
            // To look if there is a carry
            integer += roundingBehavior(((int) integer) & 1,
                    signum(fraction) * (5 + compRem),
                    mc.getRoundingMode());
            // If after to add the increment the precision changed, we normalize the size
            if (MathUtil.log10(Math.abs(integer)) >= mc.getPrecision()) {
                integer /= 10;
                newScale--;
            }
        }
        // To update all internal fields
        scale = toIntScale(newScale);
        precision = mc.getPrecision();
        smallValue = integer;
        bitLength = bitLength(integer);
        intVal = null;
    }

    /// If `intVal` has a fractional part throws an exception,
    /// otherwise it counts the number of bits of value and checks if it's out of
    /// the range of the primitive type. If the number fits in the primitive type
    /// returns this number as `long`, otherwise throws an
    /// exception.
    ///
    /// #### Parameters
    ///
    /// - `bitLengthOfType`: @param bitLengthOfType number of bits of the type whose value will be calculated
    ///                        exactly
    ///
    /// #### Returns
    ///
    /// @return the exact value of the integer part of `BigDecimal`
    /// when is possible
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: @throws ArithmeticException when rounding is necessary or the
    ///                             number don't fit in the primitive type
    private long valueExact(int bitLengthOfType) {
        TBigInteger bigInteger = toBigIntegerExact();

        if (bigInteger.bitLength() < bitLengthOfType) {
            // It fits in the primitive type
            return bigInteger.longValue();
        }
        throw new ArithmeticException("Rounding necessary");
    }

    /// If the precision already was calculated it returns that value, otherwise
    /// it calculates a very good approximation efficiently . Note that this
    /// value will be `precision()` or `precision()-1`
    /// in the worst case.
    ///
    /// #### Returns
    ///
    /// an approximation of `precision()` value
    private int aproxPrecision() {
        return (precision > 0) ? precision
                : ((int) ((this.bitLength - 1) * LOG10_2)) + 1;
    }

    private TBigInteger getUnscaledValue() {
        if (intVal == null) {
            intVal = TBigInteger.valueOf(smallValue);
        }
        return intVal;
    }

    private void setUnscaledValue(TBigInteger unscaledValue) {
        this.intVal = unscaledValue;
        this.bitLength = unscaledValue.bitLength();
        if (this.bitLength < 64) {
            this.smallValue = unscaledValue.longValue();
        }
    }
}

