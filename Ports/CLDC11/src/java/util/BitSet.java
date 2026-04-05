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

package java.util;


/// The `BitSet` class implements a bit field. Each element in a
/// `BitSet` can be on(1) or off(0). A `BitSet` is created with a
/// given size and grows if this size is exceeded. Growth is always rounded to a
/// 64 bit boundary.
public class BitSet {
    private static final int OFFSET = 6;

    private static final int ELM_SIZE = 1 << OFFSET;

    private static final int RIGHT_BITS = ELM_SIZE - 1;

    private static final long[] TWO_N_ARRAY = new long[] { 0x1L, 0x2L, 0x4L,
            0x8L, 0x10L, 0x20L, 0x40L, 0x80L, 0x100L, 0x200L, 0x400L, 0x800L,
            0x1000L, 0x2000L, 0x4000L, 0x8000L, 0x10000L, 0x20000L, 0x40000L,
            0x80000L, 0x100000L, 0x200000L, 0x400000L, 0x800000L, 0x1000000L,
            0x2000000L, 0x4000000L, 0x8000000L, 0x10000000L, 0x20000000L,
            0x40000000L, 0x80000000L, 0x100000000L, 0x200000000L, 0x400000000L,
            0x800000000L, 0x1000000000L, 0x2000000000L, 0x4000000000L,
            0x8000000000L, 0x10000000000L, 0x20000000000L, 0x40000000000L,
            0x80000000000L, 0x100000000000L, 0x200000000000L, 0x400000000000L,
            0x800000000000L, 0x1000000000000L, 0x2000000000000L,
            0x4000000000000L, 0x8000000000000L, 0x10000000000000L,
            0x20000000000000L, 0x40000000000000L, 0x80000000000000L,
            0x100000000000000L, 0x200000000000000L, 0x400000000000000L,
            0x800000000000000L, 0x1000000000000000L, 0x2000000000000000L,
            0x4000000000000000L, 0x8000000000000000L };

    private long[] bits;

    private transient boolean needClear;

    private transient int actualArrayLength;

    private transient boolean isLengthActual;

    /// Create a new `BitSet` with size equal to 64 bits.
    ///
    /// #### See also
    ///
    /// - #clear(int)
    ///
    /// - #set(int)
    ///
    /// - #clear()
    ///
    /// - #clear(int, int)
    ///
    /// - #set(int, boolean)
    ///
    /// - #set(int, int)
    ///
    /// - #set(int, int, boolean)
    public BitSet() {
        bits = new long[1];
        actualArrayLength = 0;
        isLengthActual = true;
    }

    /// Create a new `BitSet` with size equal to nbits. If nbits is not a
    /// multiple of 64, then create a `BitSet` with size nbits rounded to
    /// the next closest multiple of 64.
    ///
    /// #### Parameters
    ///
    /// - `nbits`: the size of the bit set.
    ///
    /// #### Throws
    ///
    /// - `NegativeArraySizeException`: if `nbits` is negative.
    ///
    /// #### See also
    ///
    /// - #clear(int)
    ///
    /// - #set(int)
    ///
    /// - #clear()
    ///
    /// - #clear(int, int)
    ///
    /// - #set(int, boolean)
    ///
    /// - #set(int, int)
    ///
    /// - #set(int, int, boolean)
    public BitSet(int nbits) {
        if (nbits < 0) {
            throw new NegativeArraySizeException();
        }
        bits = new long[(nbits >> OFFSET) + ((nbits & RIGHT_BITS) > 0 ? 1 : 0)];
        actualArrayLength = 0;
        isLengthActual = true;
    }

    /// Private constructor called from get(int, int) method
    ///
    /// #### Parameters
    ///
    /// - `bits`: the size of the bit set
    private BitSet(long[] bits, boolean needClear, int actualArrayLength,
            boolean isLengthActual) {
        this.bits = bits;
        this.needClear = needClear;
        this.actualArrayLength = actualArrayLength;
        this.isLengthActual = isLengthActual;
    }

    /// Compares the argument to this `BitSet` and returns whether they are
    /// equal. The object must be an instance of `BitSet` with the same
    /// bits set.
    ///
    /// #### Parameters
    ///
    /// - `obj`: the `BitSet` object to compare.
    ///
    /// #### Returns
    ///
    /// @return a `boolean` indicating whether or not this `BitSet` and
    /// `obj` are equal.
    ///
    /// #### See also
    ///
    /// - #hashCode
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BitSet) {
            long[] bsBits = ((BitSet) obj).bits;
            int length1 = this.actualArrayLength, length2 = ((BitSet) obj).actualArrayLength;
            if (this.isLengthActual && ((BitSet) obj).isLengthActual
                    && length1 != length2) {
                return false;
            }
            // If one of the BitSets is larger than the other, check to see if
            // any of its extra bits are set. If so return false.
            if (length1 <= length2) {
                for (int i = 0; i < length1; i++) {
                    if (bits[i] != bsBits[i]) {
                        return false;
                    }
                }
                for (int i = length1; i < length2; i++) {
                    if (bsBits[i] != 0) {
                        return false;
                    }
                }
            } else {
                for (int i = 0; i < length2; i++) {
                    if (bits[i] != bsBits[i]) {
                        return false;
                    }
                }
                for (int i = length2; i < length1; i++) {
                    if (bits[i] != 0) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /// Increase the size of the internal array to accommodate `pos` bits.
    /// The new array max index will be a multiple of 64.
    ///
    /// #### Parameters
    ///
    /// - `len`: the index the new array needs to be able to access.
    private final void growLength(int len) {
        long[] tempBits = new long[Math.max(len, bits.length * 2)];
        System.arraycopy(bits, 0, tempBits, 0, this.actualArrayLength);
        bits = tempBits;
    }

    /// Computes the hash code for this `BitSet`. If two `BitSet`s are equal
    /// the have to return the same result for `hashCode()`.
    ///
    /// #### Returns
    ///
    /// @return the `int` representing the hash code for this bit
    /// set.
    ///
    /// #### See also
    ///
    /// - #equals
    ///
    /// - java.util.Hashtable
    @Override
    public int hashCode() {
        long x = 1234;
        for (int i = 0, length = actualArrayLength; i < length; i++) {
            x ^= bits[i] * (i + 1);
        }
        return (int) ((x >> 32) ^ x);
    }

    /// Retrieves the bit at index `pos`. Grows the `BitSet` if
    /// `pos > size`.
    ///
    /// #### Parameters
    ///
    /// - `pos`: the index of the bit to be retrieved.
    ///
    /// #### Returns
    ///
    /// @return `true` if the bit at `pos` is set,
    /// `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: if `pos` is negative.
    ///
    /// #### See also
    ///
    /// - #clear(int)
    ///
    /// - #set(int)
    ///
    /// - #clear()
    ///
    /// - #clear(int, int)
    ///
    /// - #set(int, boolean)
    ///
    /// - #set(int, int)
    ///
    /// - #set(int, int, boolean)
    public boolean get(int pos) {
        if (pos < 0) {
            // Negative index specified
            throw new IndexOutOfBoundsException("Negative index: " + pos);
        }

        int arrayPos = pos >> OFFSET;
        if (arrayPos < actualArrayLength) {
            return (bits[arrayPos] & TWO_N_ARRAY[pos & RIGHT_BITS]) != 0;
        }
        return false;
    }

    /// Retrieves the bits starting from `pos1` to `pos2` and returns
    /// back a new bitset made of these bits. Grows the `BitSet` if
    /// `pos2 > size`.
    ///
    /// #### Parameters
    ///
    /// - `pos1`: beginning position.
    ///
    /// - `pos2`: ending position.
    ///
    /// #### Returns
    ///
    /// new bitset of the range specified.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException
    /// if `pos1` or `pos2` is negative, or if
    /// `pos2` is smaller than `pos1`.
    ///
    /// #### See also
    ///
    /// - #get(int)
    public BitSet get(int pos1, int pos2) {
        if (pos1 < 0 || pos2 < 0 || pos2 < pos1) {
            throw new IndexOutOfBoundsException("" + pos1 + " and: " + pos2);
        }

        int last = actualArrayLength << OFFSET;
        if (pos1 >= last || pos1 == pos2) {
            return new BitSet(0);
        }
        if (pos2 > last) {
            pos2 = last;
        }

        int idx1 = pos1 >> OFFSET;
        int idx2 = (pos2 - 1) >> OFFSET;
        long factor1 = (~0L) << (pos1 & RIGHT_BITS);
        long factor2 = (~0L) >>> (ELM_SIZE - (pos2 & RIGHT_BITS));

        if (idx1 == idx2) {
            long result = (bits[idx1] & (factor1 & factor2)) >>> (pos1 % ELM_SIZE);
            if (result == 0) {
                return new BitSet(0);
            }
            return new BitSet(new long[] { result }, needClear, 1, true);
        }
        long[] newbits = new long[idx2 - idx1 + 1];
        // first fill in the first and last indexes in the new bitset
        newbits[0] = bits[idx1] & factor1;
        newbits[newbits.length - 1] = bits[idx2] & factor2;

        // fill in the in between elements of the new bitset
        for (int i = 1; i < idx2 - idx1; i++) {
            newbits[i] = bits[idx1 + i];
        }

        // shift all the elements in the new bitset to the right by pos1
        // % ELM_SIZE
        int numBitsToShift = pos1 & RIGHT_BITS;
        int actualLen = newbits.length;
        if (numBitsToShift != 0) {
            for (int i = 0; i < newbits.length; i++) {
                // shift the current element to the right regardless of
                // sign
                newbits[i] = newbits[i] >>> (numBitsToShift);

                // apply the last x bits of newbits[i+1] to the current
                // element
                if (i != newbits.length - 1) {
                    newbits[i] |= newbits[i + 1] << (ELM_SIZE - (numBitsToShift));
                }
                if (newbits[i] != 0) {
                    actualLen = i + 1;
                }
            }
        }
        return new BitSet(newbits, needClear, actualLen,
                newbits[actualLen - 1] != 0);
    }

    /// Sets the bit at index `pos` to 1. Grows the `BitSet` if
    /// `pos > size`.
    ///
    /// #### Parameters
    ///
    /// - `pos`: the index of the bit to set.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: if `pos` is negative.
    ///
    /// #### See also
    ///
    /// - #clear(int)
    ///
    /// - #clear()
    ///
    /// - #clear(int, int)
    public void set(int pos) {
        if (pos < 0) {
            throw new IndexOutOfBoundsException("" + pos);
        }

        int len = (pos >> OFFSET) + 1;
        if (len > bits.length) {
            growLength(len);
        }
        bits[len - 1] |= TWO_N_ARRAY[pos & RIGHT_BITS];
        if (len > actualArrayLength) {
            actualArrayLength = len;
            isLengthActual = true;
        }
        needClear();
    }

    /// Sets the bit at index `pos` to `val`. Grows the
    /// `BitSet` if `pos > size`.
    ///
    /// #### Parameters
    ///
    /// - `pos`: the index of the bit to set.
    ///
    /// - `val`: value to set the bit.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: if `pos` is negative.
    ///
    /// #### See also
    ///
    /// - #set(int)
    public void set(int pos, boolean val) {
        if (val) {
            set(pos);
        } else {
            clear(pos);
        }
    }

    /// Sets the bits starting from `pos1` to `pos2`. Grows the
    /// `BitSet` if `pos2 > size`.
    ///
    /// #### Parameters
    ///
    /// - `pos1`: beginning position.
    ///
    /// - `pos2`: ending position.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException
    /// if `pos1` or `pos2` is negative, or if
    /// `pos2` is smaller than `pos1`.
    ///
    /// #### See also
    ///
    /// - #set(int)
    public void set(int pos1, int pos2) {
        if (pos1 < 0 || pos2 < 0 || pos2 < pos1) {
            throw new IndexOutOfBoundsException("" + pos1 + " and: " + pos2);
        }

        if (pos1 == pos2) {
            return;
        }
        int len2 = ((pos2 - 1) >> OFFSET) + 1;
        if (len2 > bits.length) {
            growLength(len2);
        }

        int idx1 = pos1 >> OFFSET;
        int idx2 = (pos2 - 1) >> OFFSET;
        long factor1 = (~0L) << (pos1 & RIGHT_BITS);
        long factor2 = (~0L) >>> (ELM_SIZE - (pos2 & RIGHT_BITS));

        if (idx1 == idx2) {
            bits[idx1] |= (factor1 & factor2);
        } else {
            bits[idx1] |= factor1;
            bits[idx2] |= factor2;
            for (int i = idx1 + 1; i < idx2; i++) {
                bits[i] |= (~0L);
            }
        }
        if (idx2 + 1 > actualArrayLength) {
            actualArrayLength = idx2 + 1;
            isLengthActual = true;
        }
        needClear();
    }

    private void needClear() {
        this.needClear = true;
    }

    /// Sets the bits starting from `pos1` to `pos2` to the given
    /// `val`. Grows the `BitSet` if `pos2 > size`.
    ///
    /// #### Parameters
    ///
    /// - `pos1`: beginning position.
    ///
    /// - `pos2`: ending position.
    ///
    /// - `val`: value to set these bits.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException
    /// if `pos1` or `pos2` is negative, or if
    /// `pos2` is smaller than `pos1`.
    ///
    /// #### See also
    ///
    /// - #set(int,int)
    public void set(int pos1, int pos2, boolean val) {
        if (val) {
            set(pos1, pos2);
        } else {
            clear(pos1, pos2);
        }
    }

    /// Clears all the bits in this `BitSet`.
    ///
    /// #### See also
    ///
    /// - #clear(int)
    ///
    /// - #clear(int, int)
    public void clear() {
        if (needClear) {
            for (int i = 0; i < bits.length; i++) {
                bits[i] = 0L;
            }
            actualArrayLength = 0;
            isLengthActual = true;
            needClear = false;
        }
    }

    /// Clears the bit at index `pos`. Grows the `BitSet` if
    /// `pos > size`.
    ///
    /// #### Parameters
    ///
    /// - `pos`: the index of the bit to clear.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: if `pos` is negative.
    ///
    /// #### See also
    ///
    /// - #clear(int, int)
    public void clear(int pos) {
        if (pos < 0) {
            // Negative index specified
            throw new IndexOutOfBoundsException("" + pos);
        }

        if (!needClear) {
            return;
        }
        int arrayPos = pos >> OFFSET;
        if (arrayPos < actualArrayLength) {
            bits[arrayPos] &= ~(TWO_N_ARRAY[pos & RIGHT_BITS]);
            if (bits[actualArrayLength - 1] == 0) {
                isLengthActual = false;
            }
        }
    }

    /// Clears the bits starting from `pos1` to `pos2`. Grows the
    /// `BitSet` if `pos2 > size`.
    ///
    /// #### Parameters
    ///
    /// - `pos1`: beginning position.
    ///
    /// - `pos2`: ending position.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException
    /// if `pos1` or `pos2` is negative, or if
    /// `pos2` is smaller than `pos1`.
    ///
    /// #### See also
    ///
    /// - #clear(int)
    public void clear(int pos1, int pos2) {
        if (pos1 < 0 || pos2 < 0 || pos2 < pos1) {
            throw new IndexOutOfBoundsException("" + pos1 + " and: " + pos2);
        }

        if (!needClear) {
            return;
        }
        int last = (actualArrayLength << OFFSET);
        if (pos1 >= last || pos1 == pos2) {
            return;
        }
        if (pos2 > last) {
            pos2 = last;
        }

        int idx1 = pos1 >> OFFSET;
        int idx2 = (pos2 - 1) >> OFFSET;
        long factor1 = (~0L) << (pos1 & RIGHT_BITS);
        long factor2 = (~0L) >>> (ELM_SIZE - (pos2 & RIGHT_BITS));

        if (idx1 == idx2) {
            bits[idx1] &= ~(factor1 & factor2);
        } else {
            bits[idx1] &= ~factor1;
            bits[idx2] &= ~factor2;
            for (int i = idx1 + 1; i < idx2; i++) {
                bits[i] = 0L;
            }
        }
        if ((actualArrayLength > 0) && (bits[actualArrayLength - 1] == 0)) {
            isLengthActual = false;
        }
    }

    /// Flips the bit at index `pos`. Grows the `BitSet` if
    /// `pos > size`.
    ///
    /// #### Parameters
    ///
    /// - `pos`: the index of the bit to flip.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: if `pos` is negative.
    ///
    /// #### See also
    ///
    /// - #flip(int, int)
    public void flip(int pos) {
        if (pos < 0) {
            throw new IndexOutOfBoundsException("" + pos);
        }

        int len = (pos >> OFFSET) + 1;
        if (len > bits.length) {
            growLength(len);
        }
        bits[len - 1] ^= TWO_N_ARRAY[pos & RIGHT_BITS];
        if (len > actualArrayLength) {
            actualArrayLength = len;
        }
        isLengthActual = !((actualArrayLength > 0) && (bits[actualArrayLength - 1] == 0));
        needClear();
    }

    /// Flips the bits starting from `pos1` to `pos2`. Grows the
    /// `BitSet` if `pos2 > size`.
    ///
    /// #### Parameters
    ///
    /// - `pos1`: beginning position.
    ///
    /// - `pos2`: ending position.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException
    /// if `pos1` or `pos2` is negative, or if
    /// `pos2` is smaller than `pos1`.
    ///
    /// #### See also
    ///
    /// - #flip(int)
    public void flip(int pos1, int pos2) {
        if (pos1 < 0 || pos2 < 0 || pos2 < pos1) {
            throw new IndexOutOfBoundsException("" + pos1 + " and: " + pos2);
        }

        if (pos1 == pos2) {
            return;
        }
        int len2 = ((pos2 - 1) >> OFFSET) + 1;
        if (len2 > bits.length) {
            growLength(len2);
        }

        int idx1 = pos1 >> OFFSET;
        int idx2 = (pos2 - 1) >> OFFSET;
        long factor1 = (~0L) << (pos1 & RIGHT_BITS);
        long factor2 = (~0L) >>> (ELM_SIZE - (pos2 & RIGHT_BITS));

        if (idx1 == idx2) {
            bits[idx1] ^= (factor1 & factor2);
        } else {
            bits[idx1] ^= factor1;
            bits[idx2] ^= factor2;
            for (int i = idx1 + 1; i < idx2; i++) {
                bits[i] ^= (~0L);
            }
        }
        if (len2 > actualArrayLength) {
            actualArrayLength = len2;
        }
        isLengthActual = !((actualArrayLength > 0) && (bits[actualArrayLength - 1] == 0));
        needClear();
    }

    /// Checks if these two `BitSet`s have at least one bit set to true in the same
    /// position.
    ///
    /// #### Parameters
    ///
    /// - `bs`: `BitSet` used to calculate the intersection.
    ///
    /// #### Returns
    ///
    /// @return `true` if bs intersects with this `BitSet`,
    /// `false` otherwise.
    public boolean intersects(BitSet bs) {
        long[] bsBits = bs.bits;
        int length1 = actualArrayLength, length2 = bs.actualArrayLength;

        if (length1 <= length2) {
            for (int i = 0; i < length1; i++) {
                if ((bits[i] & bsBits[i]) != 0L) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < length2; i++) {
                if ((bits[i] & bsBits[i]) != 0L) {
                    return true;
                }
            }
        }

        return false;
    }

    /// Performs the logical AND of this `BitSet` with another
    /// `BitSet`. The values of this `BitSet` are changed accordingly.
    ///
    /// #### Parameters
    ///
    /// - `bs`: `BitSet` to AND with.
    ///
    /// #### See also
    ///
    /// - #or
    ///
    /// - #xor
    public void and(BitSet bs) {
        long[] bsBits = bs.bits;
        if (!needClear) {
            return;
        }
        int length1 = actualArrayLength, length2 = bs.actualArrayLength;
        if (length1 <= length2) {
            for (int i = 0; i < length1; i++) {
                bits[i] &= bsBits[i];
            }
        } else {
            for (int i = 0; i < length2; i++) {
                bits[i] &= bsBits[i];
            }
            for (int i = length2; i < length1; i++) {
                bits[i] = 0;
            }
            actualArrayLength = length2;
        }
        isLengthActual = !((actualArrayLength > 0) && (bits[actualArrayLength - 1] == 0));
    }

    /// Clears all bits in the receiver which are also set in the parameter
    /// `BitSet`. The values of this `BitSet` are changed accordingly.
    ///
    /// #### Parameters
    ///
    /// - `bs`: `BitSet` to ANDNOT with.
    public void andNot(BitSet bs) {
        long[] bsBits = bs.bits;
        if (!needClear) {
            return;
        }
        int range = actualArrayLength < bs.actualArrayLength ? actualArrayLength
                : bs.actualArrayLength;
        for (int i = 0; i < range; i++) {
            bits[i] &= ~bsBits[i];
        }

        if (actualArrayLength < range) {
            actualArrayLength = range;
        }
        isLengthActual = !((actualArrayLength > 0) && (bits[actualArrayLength - 1] == 0));
    }

    /// Performs the logical OR of this `BitSet` with another `BitSet`.
    /// The values of this `BitSet` are changed accordingly.
    ///
    /// #### Parameters
    ///
    /// - `bs`: `BitSet` to OR with.
    ///
    /// #### See also
    ///
    /// - #xor
    ///
    /// - #and
    public void or(BitSet bs) {
        int bsActualLen = bs.getActualArrayLength();
        if (bsActualLen > bits.length) {
            long[] tempBits = new long[bsActualLen];
            System.arraycopy(bs.bits, 0, tempBits, 0, bs.actualArrayLength);
            for (int i = 0; i < actualArrayLength; i++) {
                tempBits[i] |= bits[i];
            }
            bits = tempBits;
            actualArrayLength = bsActualLen;
            isLengthActual = true;
        } else {
            long[] bsBits = bs.bits;
            for (int i = 0; i < bsActualLen; i++) {
                bits[i] |= bsBits[i];
            }
            if (bsActualLen > actualArrayLength) {
                actualArrayLength = bsActualLen;
                isLengthActual = true;
            }
        }
        needClear();
    }

    /// Performs the logical XOR of this `BitSet` with another `BitSet`.
    /// The values of this `BitSet` are changed accordingly.
    ///
    /// #### Parameters
    ///
    /// - `bs`: `BitSet` to XOR with.
    ///
    /// #### See also
    ///
    /// - #or
    ///
    /// - #and
    public void xor(BitSet bs) {
        int bsActualLen = bs.getActualArrayLength();
        if (bsActualLen > bits.length) {
            long[] tempBits = new long[bsActualLen];
            System.arraycopy(bs.bits, 0, tempBits, 0, bs.actualArrayLength);
            for (int i = 0; i < actualArrayLength; i++) {
                tempBits[i] ^= bits[i];
            }
            bits = tempBits;
            actualArrayLength = bsActualLen;
            isLengthActual = !((actualArrayLength > 0) && (bits[actualArrayLength - 1] == 0));
        } else {
            long[] bsBits = bs.bits;
            for (int i = 0; i < bsActualLen; i++) {
                bits[i] ^= bsBits[i];
            }
            if (bsActualLen > actualArrayLength) {
                actualArrayLength = bsActualLen;
                isLengthActual = true;
            }
        }
        needClear();
    }

    /// Returns the number of bits this `BitSet` has.
    ///
    /// #### Returns
    ///
    /// the number of bits contained in this `BitSet`.
    ///
    /// #### See also
    ///
    /// - #length
    public int size() {
        return bits.length << OFFSET;
    }

    /// Returns the number of bits up to and including the highest bit set.
    ///
    /// #### Returns
    ///
    /// the length of the `BitSet`.
    public int length() {
        int idx = actualArrayLength - 1;
        while (idx >= 0 && bits[idx] == 0) {
            --idx;
        }
        actualArrayLength = idx + 1;
        if (idx == -1) {
            return 0;
        }
        int i = ELM_SIZE - 1;
        long val = bits[idx];
        while ((val & (TWO_N_ARRAY[i])) == 0 && i > 0) {
            i--;
        }
        return (idx << OFFSET) + i + 1;
    }

    private final int getActualArrayLength() {
        if (isLengthActual) {
            return actualArrayLength;
        }
        int idx = actualArrayLength - 1;
        while (idx >= 0 && bits[idx] == 0) {
            --idx;
        }
        actualArrayLength = idx + 1;
        isLengthActual = true;
        return actualArrayLength;
    }

    /// Returns a string containing a concise, human-readable description of the
    /// receiver.
    ///
    /// #### Returns
    ///
    /// a comma delimited list of the indices of all bits that are set.
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(bits.length / 2);
        int bitCount = 0;
        sb.append('{');
        boolean comma = false;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] == 0) {
                bitCount += ELM_SIZE;
                continue;
            }
            for (int j = 0; j < ELM_SIZE; j++) {
                if (((bits[i] & (TWO_N_ARRAY[j])) != 0)) {
                    if (comma) {
                        sb.append(", "); //$NON-NLS-1$
                    }
                    sb.append(bitCount);
                    comma = true;
                }
                bitCount++;
            }
        }
        sb.append('}');
        return sb.toString();
    }

    /// Returns the position of the first bit that is `true` on or after `pos`.
    ///
    /// #### Parameters
    ///
    /// - `pos`: the starting position (inclusive).
    ///
    /// #### Returns
    ///
    /// -1 if there is no bits that are set to `true` on or after `pos`.
    public int nextSetBit(int pos) {
        if (pos < 0) {
            throw new IndexOutOfBoundsException("" + pos);
        }

        if (pos >= actualArrayLength << OFFSET) {
            return -1;
        }

        int idx = pos >> OFFSET;
        // first check in the same bit set element
        if (bits[idx] != 0L) {
            for (int j = pos & RIGHT_BITS; j < ELM_SIZE; j++) {
                if (((bits[idx] & (TWO_N_ARRAY[j])) != 0)) {
                    return (idx << OFFSET) + j;
                }
            }

        }
        idx++;
        while (idx < actualArrayLength && bits[idx] == 0L) {
            idx++;
        }
        if (idx == actualArrayLength) {
            return -1;
        }

        // we know for sure there is a bit set to true in this element
        // since the bitset value is not 0L
        for (int j = 0; j < ELM_SIZE; j++) {
            if (((bits[idx] & (TWO_N_ARRAY[j])) != 0)) {
                return (idx << OFFSET) + j;
            }
        }

        return -1;
    }

    /// Returns the position of the first bit that is `false` on or after `pos`.
    ///
    /// #### Parameters
    ///
    /// - `pos`: the starting position (inclusive).
    ///
    /// #### Returns
    ///
    /// @return the position of the next bit set to `false`, even if it is further
    /// than this `BitSet`'s size.
    public int nextClearBit(int pos) {
        if (pos < 0) {
            throw new IndexOutOfBoundsException("" + pos);
        }

        int length = actualArrayLength;
        int bssize = length << OFFSET;
        if (pos >= bssize) {
            return pos;
        }

        int idx = pos >> OFFSET;
        // first check in the same bit set element
        if (bits[idx] != (~0L)) {
            for (int j = pos % ELM_SIZE; j < ELM_SIZE; j++) {
                if (((bits[idx] & (TWO_N_ARRAY[j])) == 0)) {
                    return idx * ELM_SIZE + j;
                }
            }
        }
        idx++;
        while (idx < length && bits[idx] == (~0L)) {
            idx++;
        }
        if (idx == length) {
            return bssize;
        }

        // we know for sure there is a bit set to true in this element
        // since the bitset value is not 0L
        for (int j = 0; j < ELM_SIZE; j++) {
            if (((bits[idx] & (TWO_N_ARRAY[j])) == 0)) {
                return (idx << OFFSET) + j;
            }
        }

        return bssize;
    }

    /// Returns true if all the bits in this `BitSet` are set to false.
    ///
    /// #### Returns
    ///
    /// @return `true` if the `BitSet` is empty,
    /// `false` otherwise.
    public boolean isEmpty() {
        if (!needClear) {
            return true;
        }
        int length = bits.length;
        for (int idx = 0; idx < length; idx++) {
            if (bits[idx] != 0L) {
                return false;
            }
        }
        return true;
    }

    /// Returns the number of bits that are `true` in this `BitSet`.
    ///
    /// #### Returns
    ///
    /// the number of `true` bits in the set.
    public int cardinality() {
        if (!needClear) {
            return 0;
        }
        int count = 0;
        int length = bits.length;
        // FIXME: need to test performance, if still not satisfied, change it to
        // 256-bits table based
        for (int idx = 0; idx < length; idx++) {
            count += pop(bits[idx] & 0xffffffffL);
            count += pop(bits[idx] >>> 32);
        }
        return count;
    }

    private final int pop(long x) {
        x = x - ((x >>> 1) & 0x55555555);
        x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
        x = (x + (x >>> 4)) & 0x0f0f0f0f;
        x = x + (x >>> 8);
        x = x + (x >>> 16);
        return (int) x & 0x0000003f;
    }
}
