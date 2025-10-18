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

/**
 * Static library that provides all operations related with division and modular
 * arithmetic to {@link TBigInteger}. Some methods are provided in both mutable
 * and immutable way. There are several variants provided listed below:
 *
 * <ul type="circle">
 * <li><b>Division</b>
 * <ul type="circle">
 * <li>{@link TBigInteger} division and remainder by {@link TBigInteger}.</li>
 * <li>{@link TBigInteger} division and remainder by {@code int}.</li>
 * <li><i>gcd</i> between {@link TBigInteger} numbers.</li>
 * </ul>
 * </li>
 * <li><b>Modular arithmetic </b>
 * <ul type="circle">
 * <li>Modular exponentiation between {@link TBigInteger} numbers.</li>
 * <li>Modular inverse of a {@link TBigInteger} numbers.</li>
 * </ul>
 * </li>
 * </ul>
 */
class TDivision {

    /**
     * Divides the array 'a' by the array 'b' and gets the quotient and the
     * remainder. Implements the Knuth's division algorithm. See D. Knuth, The
     * Art of Computer Programming, vol. 2. Steps D1-D8 correspond the steps in
     * the algorithm description.
     *
     * @param quot       the quotient
     * @param quotLength the quotient's length
     * @param a          the dividend
     * @param aLength    the dividend's length
     * @param b          the divisor
     * @param bLength    the divisor's length
     * @return the remainder
     */
    static int[] divide(int[] quot, int quotLength, int[] a, int aLength, int[] b, int bLength) {

        int[] normA = new int[aLength + 1]; // the normalized dividend
        // an extra byte is needed for correct shift
        int[] normB = new int[bLength + 1]; // the normalized divisor;
        int normBLength = bLength;
        /*
         * Step D1: normalize a and b and put the results to a1 and b1 the
         * normalized divisor's first digit must be >= 2^31
         */
        int divisorShift = TBigDecimal.numberOfLeadingZeros(b[bLength - 1]);
        if (divisorShift != 0) {
            TBitLevel.shiftLeft(normB, b, 0, divisorShift);
            TBitLevel.shiftLeft(normA, a, 0, divisorShift);
        } else {
            System.arraycopy(a, 0, normA, 0, aLength);
            System.arraycopy(b, 0, normB, 0, bLength);
        }
        int firstDivisorDigit = normB[normBLength - 1];
        // Step D2: set the quotient index
        int i = quotLength - 1;
        int j = aLength;

        while (i >= 0) {
            // Step D3: calculate a guess digit guessDigit
            int guessDigit = 0;
            if (normA[j] == firstDivisorDigit) {
                // set guessDigit to the largest unsigned int value
                guessDigit = -1;
            } else {
                long product = (((normA[j] & 0xffffffffL) << 32) + (normA[j - 1] & 0xffffffffL));
                long res = TDivision.divideLongByInt(product, firstDivisorDigit);
                guessDigit = (int) res; // the quotient of divideLongByInt
                int rem = (int) (res >> 32); // the remainder of
                // divideLongByInt
                // decrease guessDigit by 1 while leftHand > rightHand
                if (guessDigit != 0) {
                    long leftHand = 0;
                    long rightHand = 0;
                    boolean rOverflowed = false;
                    guessDigit++; // to have the proper value in the loop
                    // below
                    do {
                        guessDigit--;
                        if (rOverflowed) {
                            break;
                        }
                        // leftHand always fits in an unsigned long
                        leftHand = (guessDigit & 0xffffffffL) * (normB[normBLength - 2] & 0xffffffffL);
                        /*
                         * rightHand can overflow; in this case the loop
                         * condition will be true in the next step of the loop
                         */
                        rightHand = ((long) rem << 32) + (normA[j - 2] & 0xffffffffL);
                        long longR = (rem & 0xffffffffL) + (firstDivisorDigit & 0xffffffffL);
                        /*
                         * checks that longR does not fit in an unsigned int;
                         * this ensures that rightHand will overflow unsigned
                         * long in the next step
                         */
                        if (TBigDecimal.numberOfLeadingZeros((int) (longR >>> 32)) < 32) {
                            rOverflowed = true;
                        } else {
                            rem = (int) longR;
                        }
                    } while (((leftHand ^ 0x8000000000000000L) > (rightHand ^ 0x8000000000000000L)));
                }
            }
            // Step D4: multiply normB by guessDigit and subtract the production
            // from normA.
            if (guessDigit != 0) {
                int borrow = TDivision.multiplyAndSubtract(normA, j - normBLength, normB, normBLength, guessDigit);
                // Step D5: check the borrow
                if (borrow != 0) {
                    // Step D6: compensating addition
                    guessDigit--;
                    long carry = 0;
                    for (int k = 0; k < normBLength; k++) {
                        carry += (normA[j - normBLength + k] & 0xffffffffL) + (normB[k] & 0xffffffffL);
                        normA[j - normBLength + k] = (int) carry;
                        carry >>>= 32;
                    }
                }
            }
            if (quot != null) {
                quot[i] = guessDigit;
            }
            // Step D7
            j--;
            i--;
        }
        /*
         * Step D8: we got the remainder in normA. Denormalize it id needed
         */
        if (divisorShift != 0) {
            // reuse normB
            TBitLevel.shiftRight(normB, normBLength, normA, 0, divisorShift);
            return normB;
        }
        System.arraycopy(normA, 0, normB, 0, bLength);
        return normA;
    }

    /**
     * Divides an array by an integer value. Implements the Knuth's division
     * algorithm. See D. Knuth, The Art of Computer Programming, vol. 2.
     *
     * @param dest      the quotient
     * @param src       the dividend
     * @param srcLength the length of the dividend
     * @param divisor   the divisor
     * @return remainder
     */
    static int divideArrayByInt(int[] dest, int[] src, final int srcLength, final int divisor) {

        long rem = 0;
        long bLong = divisor & 0xffffffffL;

        for (int i = srcLength - 1; i >= 0; i--) {
            long temp = (rem << 32) | (src[i] & 0xffffffffL);
            long quot;
            if (temp >= 0) {
                quot = (temp / bLong);
                rem = (temp % bLong);
            } else {
                /*
                 * make the dividend positive shifting it right by 1 bit then
                 * get the quotient a remainder and correct them properly
                 */
                long aPos = temp >>> 1;
                long bPos = divisor >>> 1;
                quot = aPos / bPos;
                rem = aPos % bPos;
                // double the remainder and add 1 if a is odd
                rem = (rem << 1) + (temp & 1);
                if ((divisor & 1) != 0) {
                    // the divisor is odd
                    if (quot <= rem) {
                        rem -= quot;
                    } else {
                        if (quot - rem <= bLong) {
                            rem += bLong - quot;
                            quot -= 1;
                        } else {
                            rem += (bLong << 1) - quot;
                            quot -= 2;
                        }
                    }
                }
            }
            dest[i] = (int) (quot & 0xffffffffL);
        }
        return (int) rem;
    }

    /**
     * Divides an array by an integer value. Implements the Knuth's division
     * algorithm. See D. Knuth, The Art of Computer Programming, vol. 2.
     *
     * @param src       the dividend
     * @param srcLength the length of the dividend
     * @param divisor   the divisor
     * @return remainder
     */
    static int remainderArrayByInt(int[] src, final int srcLength, final int divisor) {

        long result = 0;

        for (int i = srcLength - 1; i >= 0; i--) {
            long temp = (result << 32) + (src[i] & 0xffffffffL);
            long res = divideLongByInt(temp, divisor);
            result = (int) (res >> 32);
        }
        return (int) result;
    }

    /**
     * Divides a <code>BigInteger</code> by a signed <code>int</code> and
     * returns the remainder.
     *
     * @param dividend the BigInteger to be divided. Must be non-negative.
     * @param divisor  a signed int
     * @return divide % divisor
     */
    static int remainder(TBigInteger dividend, int divisor) {
        return remainderArrayByInt(dividend.digits, dividend.numberLength, divisor);
    }

    /**
     * Divides an unsigned long a by an unsigned int b. It is supposed that the
     * most significant bit of b is set to 1, i.e. b < 0
     *
     * @param a the dividend
     * @param b the divisor
     * @return the long value containing the unsigned integer remainder in the
     * left half and the unsigned integer quotient in the right half
     */
    static long divideLongByInt(long a, int b) {
        long quot;
        long rem;
        long bLong = b & 0xffffffffL;

        if (a >= 0) {
            quot = (a / bLong);
            rem = (a % bLong);
        } else {
            /*
             * Make the dividend positive shifting it right by 1 bit then get
             * the quotient a remainder and correct them properly
             */
            long aPos = a >>> 1;
            long bPos = b >>> 1;
            quot = aPos / bPos;
            rem = aPos % bPos;
            // double the remainder and add 1 if a is odd
            rem = (rem << 1) + (a & 1);
            if ((b & 1) != 0) { // the divisor is odd
                if (quot <= rem) {
                    rem -= quot;
                } else {
                    if (quot - rem <= bLong) {
                        rem += bLong - quot;
                        quot -= 1;
                    } else {
                        rem += (bLong << 1) - quot;
                        quot -= 2;
                    }
                }
            }
        }
        return (rem << 32) | (quot & 0xffffffffL);
    }

    /**
     * Computes the quotient and the remainder after a division by an
     * {@code int} number.
     *
     * @return an array of the form {@code [quotient, remainder]}.
     */
    static TBigInteger[] divideAndRemainderByInteger(TBigInteger val, int divisor, int divisorSign) {
        // res[0] is a quotient and res[1] is a remainder:
        int[] valDigits = val.digits;
        int valLen = val.numberLength;
        int valSign = val.sign;
        if (valLen == 1) {
            long a = (valDigits[0] & 0xffffffffL);
            long b = (divisor & 0xffffffffL);
            long quo = a / b;
            long rem = a % b;
            if (valSign != divisorSign) {
                quo = -quo;
            }
            if (valSign < 0) {
                rem = -rem;
            }
            return new TBigInteger[]{TBigInteger.valueOf(quo), TBigInteger.valueOf(rem)};
        }
        int quotientLength = valLen;
        int quotientSign = ((valSign == divisorSign) ? 1 : -1);
        int[] quotientDigits = new int[quotientLength];
        int[] remainderDigits;
        remainderDigits = new int[]{TDivision.divideArrayByInt(quotientDigits, valDigits, valLen, divisor)};
        TBigInteger result0 = new TBigInteger(quotientSign, quotientLength, quotientDigits);
        TBigInteger result1 = new TBigInteger(valSign, 1, remainderDigits);
        result0.cutOffLeadingZeroes();
        result1.cutOffLeadingZeroes();
        return new TBigInteger[]{result0, result1};
    }

    /**
     * Multiplies an array by int and subtracts it from a subarray of another
     * array.
     *
     * @param a     the array to subtract from
     * @param start the start element of the subarray of a
     * @param b     the array to be multiplied and subtracted
     * @param bLen  the length of b
     * @param c     the multiplier of b
     * @return the carry element of subtraction
     */
    static int multiplyAndSubtract(int[] a, int start, int[] b, int bLen, int c) {
        long carry0 = 0;
        long carry1 = 0;

        for (int i = 0; i < bLen; i++) {
            carry0 = TMultiplication.unsignedMultAddAdd(b[i], c, (int) carry0, 0);
            carry1 = (a[start + i] & 0xffffffffL) - (carry0 & 0xffffffffL) + carry1;
            a[start + i] = (int) carry1;
            carry1 >>= 32; // -1 or 0
            carry0 >>>= 32;
        }

        carry1 = (a[start + bLen] & 0xffffffffL) - carry0 + carry1;
        a[start + bLen] = (int) carry1;
        return (int) (carry1 >> 32); // -1 or 0
    }

    /**
     * @param m   a positive modulus Return the greatest common divisor of op1
     *            and op2,
     * @param op1 must be greater than zero
     * @param op2 must be greater than zero
     * @return {@code GCD(op1, op2)}
     * @see TBigInteger#gcd(TBigInteger)
     */
    static TBigInteger gcdBinary(TBigInteger op1, TBigInteger op2) {
        // PRE: (op1 > 0) and (op2 > 0)

        /*
         * Divide both number the maximal possible times by 2 without rounding
         * gcd(2*a, 2*b) = 2 * gcd(a,b)
         */
        int lsb1 = op1.getLowestSetBit();
        int lsb2 = op2.getLowestSetBit();
        int pow2Count = Math.min(lsb1, lsb2);

        TBitLevel.inplaceShiftRight(op1, lsb1);
        TBitLevel.inplaceShiftRight(op2, lsb2);

        TBigInteger swap;
        // I want op2 > op1
        if (op1.compareTo(op2) == TBigInteger.GREATER) {
            swap = op1;
            op1 = op2;
            op2 = swap;
        }

        do { // INV: op2 >= op1 && both are odd unless op1 = 0

            // Optimization for small operands
            // (op2.bitLength() < 64) implies by INV (op1.bitLength() < 64)
            if ((op2.numberLength == 1) || ((op2.numberLength == 2) && (op2.digits[1] > 0))) {
                op2 = TBigInteger.valueOf(TDivision.gcdBinary(op1.longValue(), op2.longValue()));
                break;
            }

            // Implements one step of the Euclidean algorithm
            // To reduce one operand if it's much smaller than the other one
            if (op2.numberLength > op1.numberLength * 1.2) {
                op2 = op2.remainder(op1);
                if (op2.signum() != 0) {
                    TBitLevel.inplaceShiftRight(op2, op2.getLowestSetBit());
                }
            } else {

                // Use Knuth's algorithm of successive subtract and shifting
                do {
                    TElementary.inplaceSubtract(op2, op1); // both are odd
                    TBitLevel.inplaceShiftRight(op2, op2.getLowestSetBit());
                } while (op2.compareTo(op1) >= TBigInteger.EQUALS);
            }
            // now op1 >= op2
            swap = op2;
            op2 = op1;
            op1 = swap;
        } while (op1.sign != 0);
        return op2.shiftLeft(pow2Count);
    }

    /**
     * Performs the same as {@link #gcdBinary(TBigInteger, TBigInteger)}, but
     * with numbers of 63 bits, represented in positives values of {@code long}
     * type.
     *
     * @param op1 a positive number
     * @param op2 a positive number
     * @return <code>GCD(op1, op2)</code>
     * @see #gcdBinary(TBigInteger, TBigInteger)
     */
    static long gcdBinary(long op1, long op2) {
        // PRE: (op1 > 0) and (op2 > 0)
        int lsb1 = TBigDecimal.numberOfTrailingZeros(op1);
        int lsb2 = TBigDecimal.numberOfTrailingZeros(op2);
        int pow2Count = Math.min(lsb1, lsb2);

        if (lsb1 != 0) {
            op1 >>>= lsb1;
        }
        if (lsb2 != 0) {
            op2 >>>= lsb2;
        }
        do {
            if (op1 >= op2) {
                op1 -= op2;
                op1 >>>= TBigDecimal.numberOfTrailingZeros(op1);
            } else {
                op2 -= op1;
                op2 >>>= TBigDecimal.numberOfTrailingZeros(op2);
            }
        } while (op1 != 0);
        return (op2 << pow2Count);
    }

    /**
     * Calculates a.modInverse(p) Based on: Savas, E; Koc, C "The Montgomery
     * Modular Inverse - Revised"
     */
    static TBigInteger modInverseMontgomery(TBigInteger a, TBigInteger p) {

        if (a.sign == 0) {
            // ZERO hasn't inverse
            throw new ArithmeticException("BigInteger not invertible");
        }

        if (!p.testBit(0)) {
            // montgomery inverse require even modulo
            return modInverseHars(a, p);
        }

        int m = p.numberLength * 32;
        // PRE: a \in [1, p - 1]
        TBigInteger u, v, r, s;
        u = p.copy(); // make copy to use inplace method
        v = a.copy();
        int max = Math.max(v.numberLength, u.numberLength);
        r = new TBigInteger(1, 1, new int[max + 1]);
        s = new TBigInteger(1, 1, new int[max + 1]);
        s.digits[0] = 1;
        // s == 1 && v == 0

        int k = 0;

        int lsbu = u.getLowestSetBit();
        int lsbv = v.getLowestSetBit();
        int toShift;

        if (lsbu > lsbv) {
            TBitLevel.inplaceShiftRight(u, lsbu);
            TBitLevel.inplaceShiftRight(v, lsbv);
            TBitLevel.inplaceShiftLeft(r, lsbv);
            k += lsbu - lsbv;
        } else {
            TBitLevel.inplaceShiftRight(u, lsbu);
            TBitLevel.inplaceShiftRight(v, lsbv);
            TBitLevel.inplaceShiftLeft(s, lsbu);
            k += lsbv - lsbu;
        }

        r.sign = 1;
        while (v.signum() > 0) {
            // INV v >= 0, u >= 0, v odd, u odd (except last iteration when v is
            // even (0))

            while (u.compareTo(v) > TBigInteger.EQUALS) {
                TElementary.inplaceSubtract(u, v);
                toShift = u.getLowestSetBit();
                TBitLevel.inplaceShiftRight(u, toShift);
                TElementary.inplaceAdd(r, s);
                TBitLevel.inplaceShiftLeft(s, toShift);
                k += toShift;
            }

            while (u.compareTo(v) <= TBigInteger.EQUALS) {
                TElementary.inplaceSubtract(v, u);
                if (v.signum() == 0)
                    break;
                toShift = v.getLowestSetBit();
                TBitLevel.inplaceShiftRight(v, toShift);
                TElementary.inplaceAdd(s, r);
                TBitLevel.inplaceShiftLeft(r, toShift);
                k += toShift;
            }
        }
        if (!u.isOne()) {
            throw new ArithmeticException("BigInteger not invertible.");
        }
        if (r.compareTo(p) >= TBigInteger.EQUALS) {
            TElementary.inplaceSubtract(r, p);
        }

        r = p.subtract(r);

        // Have pair: ((BigInteger)r, (Integer)k) where r == a^(-1) * 2^k mod
        // (module)
        int n1 = calcN(p);
        if (k > m) {
            r = monPro(r, TBigInteger.ONE, p, n1);
            k = k - m;
        }

        r = monPro(r, TBigInteger.getPowerOfTwo(m - k), p, n1);
        return r;
    }

    /**
     * Calculate the first digit of the inverse
     */
    private static int calcN(TBigInteger a) {
        long m0 = a.digits[0] & 0xFFFFFFFFL;
        long n2 = 1L; // this is a'[0]
        long powerOfTwo = 2L;
        do {
            if (((m0 * n2) & powerOfTwo) != 0) {
                n2 |= powerOfTwo;
            }
            powerOfTwo <<= 1;
        } while (powerOfTwo < 0x100000000L);
        n2 = -n2;
        return (int) (n2 & 0xFFFFFFFFL);
    }

    static TBigInteger squareAndMultiply(TBigInteger x2, TBigInteger a2, TBigInteger exponent, TBigInteger modulus,
                                         int n2) {
        TBigInteger res = x2;
        for (int i = exponent.bitLength() - 1; i >= 0; i--) {
            res = monPro(res, res, modulus, n2);
            if (TBitLevel.testBit(exponent, i)) {
                res = monPro(res, a2, modulus, n2);
            }
        }
        return res;
    }

    /**
     * Implements the "Shifting Euclidean modular inverse algorithm". "Laszlo
     * Hars - Modular Inverse Algorithms Without Multiplications for
     * Cryptographic Applications"
     *
     * @param a a positive number
     * @param m a positive modulus
     * @see TBigInteger#modInverse(TBigInteger)
     */
    static TBigInteger modInverseHars(TBigInteger a, TBigInteger m) {
        // PRE: (a > 0) and (m > 0)
        TBigInteger u, v, r, s, temp;
        // u = MAX(a,m), v = MIN(a,m)
        if (a.compareTo(m) == TBigInteger.LESS) {
            u = m;
            v = a;
            r = TBigInteger.ZERO;
            s = TBigInteger.ONE;
        } else {
            v = m;
            u = a;
            s = TBigInteger.ZERO;
            r = TBigInteger.ONE;
        }
        int uLen = u.bitLength();
        int vLen = v.bitLength();
        int f = uLen - vLen;

        while (vLen > 1) {
            if (u.sign == v.sign) {
                u = u.subtract(v.shiftLeft(f));
                r = r.subtract(s.shiftLeft(f));
            } else {
                u = u.add(v.shiftLeft(f));
                r = r.add(s.shiftLeft(f));
            }
            uLen = u.abs().bitLength();
            vLen = v.abs().bitLength();
            f = uLen - vLen;
            if (f < 0) {
                // SWAP(u,v)
                temp = u;
                u = v;
                v = temp;
                // SWAP(r,s)
                temp = r;
                r = s;
                s = temp;

                f = -f;
                vLen = uLen;
            }
        }
        if (v.sign == 0) {
            return TBigInteger.ZERO;
        }
        if (v.sign < 0) {
            s = s.negate();
        }
        if (s.compareTo(m) == TBigInteger.GREATER) {
            return s.subtract(m);
        }
        if (s.sign < 0) {
            return s.add(m);
        }
        return s; // a^(-1) mod m
    }

    /*
     * Implements the Montgomery modular exponentiation based in <i>The sliding
     * windows algorithm and the MongomeryReduction</i>.
     *
     * @ar.org.fitc.ref
     * "A. Menezes,P. van Oorschot, S. Vanstone - Handbook of Applied Cryptography"
     * ;
     *
     * @see #oddModPow(BigInteger, BigInteger, BigInteger)
     */
    static TBigInteger slidingWindow(TBigInteger x2, TBigInteger a2, TBigInteger exponent, TBigInteger modulus, int n2) {
        // fill odd low pows of a2
        TBigInteger[] pows = new TBigInteger[8];
        TBigInteger res = x2;
        int lowexp;
        TBigInteger x3;
        int acc3;
        pows[0] = a2;

        x3 = monPro(a2, a2, modulus, n2);
        for (int i = 1; i <= 7; i++) {
            pows[i] = monPro(pows[i - 1], x3, modulus, n2);
        }

        for (int i = exponent.bitLength() - 1; i >= 0; i--) {
            if (TBitLevel.testBit(exponent, i)) {
                lowexp = 1;
                acc3 = i;

                for (int j = Math.max(i - 3, 0); j <= i - 1; j++) {
                    if (TBitLevel.testBit(exponent, j)) {
                        if (j < acc3) {
                            acc3 = j;
                            lowexp = (lowexp << (i - j)) ^ 1;
                        } else {
                            lowexp = lowexp ^ (1 << (j - acc3));
                        }
                    }
                }

                for (int j = acc3; j <= i; j++) {
                    res = monPro(res, res, modulus, n2);
                }
                res = monPro(pows[(lowexp - 1) >> 1], res, modulus, n2);
                i = acc3;
            } else {
                res = monPro(res, res, modulus, n2);
            }
        }
        return res;
    }

    /**
     * Performs modular exponentiation using the Montgomery Reduction. It
     * requires that all parameters be positive and the modulus be odd. >
     *
     * @see TBigInteger#modPow(TBigInteger, TBigInteger)
     * @see #monPro(TBigInteger, TBigInteger, TBigInteger, int)
     * @see #slidingWindow(TBigInteger, TBigInteger, TBigInteger, TBigInteger,
     * int)
     * @see #squareAndMultiply(TBigInteger, TBigInteger, TBigInteger,
     * TBigInteger, int)
     */
    static TBigInteger oddModPow(TBigInteger base, TBigInteger exponent, TBigInteger modulus) {
        // PRE: (base > 0), (exponent > 0), (modulus > 0) and (odd modulus)
        int k = (modulus.numberLength << 5); // r = 2^k
        // n-residue of base [base * r (mod modulus)]
        TBigInteger a2 = base.shiftLeft(k).mod(modulus);
        // n-residue of base [1 * r (mod modulus)]
        TBigInteger x2 = TBigInteger.getPowerOfTwo(k).mod(modulus);
        TBigInteger res;
        // Compute (modulus[0]^(-1)) (mod 2^32) for odd modulus

        int n2 = calcN(modulus);
        if (modulus.numberLength == 1) {
            res = squareAndMultiply(x2, a2, exponent, modulus, n2);
        } else {
            res = slidingWindow(x2, a2, exponent, modulus, n2);
        }

        return monPro(res, TBigInteger.ONE, modulus, n2);
    }

    /**
     * Performs modular exponentiation using the Montgomery Reduction. It
     * requires that all parameters be positive and the modulus be even. Based
     * <i>The square and multiply algorithm and the Montgomery Reduction C. K.
     * Koc - Montgomery Reduction with Even Modulus</i>. The square and multiply
     * algorithm and the Montgomery Reduction.
     *
     * @ar.org.fitc.ref "C. K. Koc - Montgomery Reduction with Even Modulus"
     * @see TBigInteger#modPow(TBigInteger, TBigInteger)
     */
    static TBigInteger evenModPow(TBigInteger base, TBigInteger exponent, TBigInteger modulus) {
        // PRE: (base > 0), (exponent > 0), (modulus > 0) and (modulus even)
        // STEP 1: Obtain the factorization 'modulus'= q * 2^j.
        int j = modulus.getLowestSetBit();
        TBigInteger q = modulus.shiftRight(j);

        // STEP 2: Compute x1 := base^exponent (mod q).
        TBigInteger x1 = oddModPow(base, exponent, q);

        // STEP 3: Compute x2 := base^exponent (mod 2^j).
        TBigInteger x2 = pow2ModPow(base, exponent, j);

        // STEP 4: Compute q^(-1) (mod 2^j) and y := (x2-x1) * q^(-1) (mod 2^j)
        TBigInteger qInv = modPow2Inverse(q, j);
        TBigInteger y = (x2.subtract(x1)).multiply(qInv);
        inplaceModPow2(y, j);
        if (y.sign < 0) {
            y = y.add(TBigInteger.getPowerOfTwo(j));
        }
        // STEP 5: Compute and return: x1 + q * y
        return x1.add(q.multiply(y));
    }

    /**
     * It requires that all parameters be positive.
     *
     * @return {@code base<sup>exponent</sup> mod (2<sup>j</sup>)}.
     * @see TBigInteger#modPow(TBigInteger, TBigInteger)
     */
    static TBigInteger pow2ModPow(TBigInteger base, TBigInteger exponent, int j) {
        // PRE: (base > 0), (exponent > 0) and (j > 0)
        TBigInteger res = TBigInteger.ONE;
        TBigInteger e = exponent.copy();
        TBigInteger baseMod2toN = base.copy();
        TBigInteger res2;
        /*
         * If 'base' is odd then it's coprime with 2^j and phi(2^j) = 2^(j-1);
         * so we can reduce reduce the exponent (mod 2^(j-1)).
         */
        if (base.testBit(0)) {
            inplaceModPow2(e, j - 1);
        }
        inplaceModPow2(baseMod2toN, j);

        for (int i = e.bitLength() - 1; i >= 0; i--) {
            res2 = res.copy();
            inplaceModPow2(res2, j);
            res = res.multiply(res2);
            if (TBitLevel.testBit(e, i)) {
                res = res.multiply(baseMod2toN);
                inplaceModPow2(res, j);
            }
        }
        inplaceModPow2(res, j);
        return res;
    }

    private static void monReduction(int[] res, TBigInteger modulus, int n2) {

        /* res + m*modulus_digits */
        int[] modulus_digits = modulus.digits;
        int modulusLen = modulus.numberLength;
        long outerCarry = 0;

        for (int i = 0; i < modulusLen; i++) {
            long innnerCarry = 0;
            int m = (int) TMultiplication.unsignedMultAddAdd(res[i], n2, 0, 0);
            for (int j = 0; j < modulusLen; j++) {
                innnerCarry = TMultiplication.unsignedMultAddAdd(m, modulus_digits[j], res[i + j], (int) innnerCarry);
                res[i + j] = (int) innnerCarry;
                innnerCarry >>>= 32;
            }

            outerCarry += (res[i + modulusLen] & 0xFFFFFFFFL) + innnerCarry;
            res[i + modulusLen] = (int) outerCarry;
            outerCarry >>>= 32;
        }

        res[modulusLen << 1] = (int) outerCarry;

        /* res / r */
        for (int j = 0; j < modulusLen + 1; j++) {
            res[j] = res[j + modulusLen];
        }
    }

    /**
     * Implements the Montgomery Product of two integers represented by
     * {@code int} arrays. The arrays are supposed in <i>little endian</i>
     * notation.
     *
     * @param a       The first factor of the product.
     * @param b       The second factor of the product.
     * @param modulus The modulus of the operations. Z<sub>modulus</sub>.
     * @param n2      The digit modulus'[0].
     * @ar.org.fitc.ref "C. K. Koc - Analyzing and Comparing Montgomery
     * Multiplication Algorithms"
     * @see #modPowOdd(TBigInteger, TBigInteger, TBigInteger)
     */
    static TBigInteger monPro(TBigInteger a, TBigInteger b, TBigInteger modulus, int n2) {
        int modulusLen = modulus.numberLength;
        int[] res = new int[(modulusLen << 1) + 1];
        TMultiplication.multArraysPAP(a.digits, Math.min(modulusLen, a.numberLength), b.digits,
                Math.min(modulusLen, b.numberLength), res);
        monReduction(res, modulus, n2);
        return finalSubtraction(res, modulus);

    }

    /**
     * Performs the final reduction of the Montgomery algorithm.
     *
     * @see #monPro(TBigInteger, TBigInteger, TBigInteger, long)
     * @see #monSquare(TBigInteger, TBigInteger, long)
     */
    static TBigInteger finalSubtraction(int[] res, TBigInteger modulus) {

        // skipping leading zeros
        int modulusLen = modulus.numberLength;
        boolean doSub = res[modulusLen] != 0;
        if (!doSub) {
            int[] modulusDigits = modulus.digits;
            doSub = true;
            for (int i = modulusLen - 1; i >= 0; i--) {
                if (res[i] != modulusDigits[i]) {
                    doSub = (res[i] != 0) && ((res[i] & 0xFFFFFFFFL) > (modulusDigits[i] & 0xFFFFFFFFL));
                    break;
                }
            }
        }

        TBigInteger result = new TBigInteger(1, modulusLen + 1, res);

        // if (res >= modulusDigits) compute (res - modulusDigits)
        if (doSub) {
            TElementary.inplaceSubtract(result, modulus);
        }

        result.cutOffLeadingZeroes();
        return result;
    }

    /**
     * @param x an odd positive number.
     * @param n the exponent by which 2 is raised.
     * @return {@code x<sup>-1</sup> (mod 2<sup>n</sup>)}.
     */
    static TBigInteger modPow2Inverse(TBigInteger x, int n) {
        // PRE: (x > 0), (x is odd), and (n > 0)
        TBigInteger y = new TBigInteger(1, new int[1 << n]);
        y.numberLength = 1;
        y.digits[0] = 1;
        y.sign = 1;

        for (int i = 1; i < n; i++) {
            if (TBitLevel.testBit(x.multiply(y), i)) {
                // Adding 2^i to y (setting the i-th bit)
                y.digits[i >> 5] |= (1 << (i & 31));
            }
        }
        return y;
    }

    /**
     * Performs {@code x = x mod (2<sup>n</sup>)}.
     *
     * @param x a positive number, it will store the result.
     * @param n a positive exponent of {@code 2}.
     */
    static void inplaceModPow2(TBigInteger x, int n) {
        // PRE: (x > 0) and (n >= 0)
        int fd = n >> 5;
        int leadingZeros;

        if ((x.numberLength < fd) || (x.bitLength() <= n)) {
            return;
        }
        leadingZeros = 32 - (n & 31);
        x.numberLength = fd + 1;
        x.digits[fd] &= (leadingZeros < 32) ? (-1 >>> leadingZeros) : 0;
        x.cutOffLeadingZeroes();
    }

}
