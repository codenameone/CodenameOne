package net.sourceforge.retroweaver.runtime.java.lang;

public class Long_ {

	private Long_() {
		// private constructor
	}

    /**
     * <p>
     * Constant for the number of bits to represent a <code>long</code> in
     * two's compliment form.
     * </p>
     * 
     * @since 1.5
     */
    public static final int SIZE = 64;

    /**
     * <p>
     * Determines the highest (leftmost) bit that is 1 and returns the value
     * that is the bit mask for that bit. This is sometimes referred to as the
     * Most Significant 1 Bit.
     * </p>
     * 
     * @param lng
     *            The <code>long</code> to interrogate.
     * @return The bit mask indicating the highest 1 bit.
     * @since 1.5
     */
    public static long highestOneBit(long lng) {
        lng |= (lng >> 1);
        lng |= (lng >> 2);
        lng |= (lng >> 4);
        lng |= (lng >> 8);
        lng |= (lng >> 16);
        lng |= (lng >> 32);
        return (lng & ~(lng >>> 1));
    }

    /**
     * <p>
     * Determines the lowest (rightmost) bit that is 1 and returns the value
     * that is the bit mask for that bit. This is sometimes referred to as the
     * Least Significant 1 Bit.
     * </p>
     * 
     * @param lng
     *            The <code>long</code> to interrogate.
     * @return The bit mask indicating the lowest 1 bit.
     * @since 1.5
     */
    public static long lowestOneBit(long lng) {
        return (lng & (-lng));
    }

    /**
     * <p>
     * Determines the number of leading zeros in the <code>long</code> passed
     * prior to the {@link #highestOneBit(long) highest one bit}.
     * </p>
     * 
     * @param lng
     *            The <code>long</code> to process.
     * @return The number of leading zeros.
     * @since 1.5
     */
    public static int numberOfLeadingZeros(long lng) {
        lng |= lng >> 1;
        lng |= lng >> 2;
        lng |= lng >> 4;
        lng |= lng >> 8;
        lng |= lng >> 16;
        lng |= lng >> 32;
        return bitCount(~lng);
    }

    /**
     * <p>
     * Determines the number of trailing zeros in the <code>long</code> passed
     * after the {@link #lowestOneBit(long) lowest one bit}.
     * </p>
     * 
     * @param lng
     *            The <code>long</code> to process.
     * @return The number of trailing zeros.
     * @since 1.5
     */
    public static int numberOfTrailingZeros(long lng) {
        return bitCount((lng & -lng) - 1);
    }

    /**
     * <p>
     * Counts the number of 1 bits in the <code>long</code> value passed; this
     * is sometimes referred to as a population count.
     * </p>
     * 
     * @param lng
     *            The <code>long</code> value to process.
     * @return The number of 1 bits.
     * @since 1.5
     */
    public static int bitCount(long lng) {
        lng = (lng & 0x5555555555555555L) + ((lng >> 1) & 0x5555555555555555L);
        lng = (lng & 0x3333333333333333L) + ((lng >> 2) & 0x3333333333333333L);
        // adjust for 64-bit integer
        int i = (int) ((lng >>> 32) + lng);
        i = (i & 0x0F0F0F0F) + ((i >> 4) & 0x0F0F0F0F);
        i = (i & 0x00FF00FF) + ((i >> 8) & 0x00FF00FF);
        i = (i & 0x0000FFFF) + ((i >> 16) & 0x0000FFFF);
        return i;
    }

    /**
     * <p>
     * Rotates the bits of <code>lng</code> to the left by the
     * <code>distance</code> bits.
     * </p>
     * 
     * @param lng
     *            The <code>long</code> value to rotate left.
     * @param distance
     *            The number of bits to rotate.
     * @return The rotated value.
     * @since 1.5
     */
    public static long rotateLeft(long lng, int distance) {
        if (distance == 0) {
            return lng;
        }
        /*
         * According to JLS3, 15.19, the right operand of a shift is always
         * implicitly masked with 0x3F, which the negation of 'distance' is
         * taking advantage of.
         */
        return ((lng << distance) | (lng >>> (-distance)));
    }

    /**
     * <p>
     * Rotates the bits of <code>lng</code> to the right by the
     * <code>distance</code> bits.
     * </p>
     * 
     * @param lng
     *            The <code>long</code> value to rotate right.
     * @param distance
     *            The number of bits to rotate.
     * @return The rotated value.
     * @since 1.5
     */
    public static long rotateRight(long lng, int distance) {
        if (distance == 0) {
            return lng;
        }
        /*
         * According to JLS3, 15.19, the right operand of a shift is always
         * implicitly masked with 0x3F, which the negation of 'distance' is
         * taking advantage of.
         */
        return ((lng >>> distance) | (lng << (-distance)));
    }

    /**
     * <p>
     * Reverses the bytes of a <code>long</code>.
     * </p>
     * 
     * @param lng
     *            The <code>long</code> to reverse.
     * @return The reversed value.
     * @since 1.5
     */
    public static long reverseBytes(long lng) {
        long b7 = lng >>> 56;
        long b6 = (lng >>> 40) & 0xFF00L;
        long b5 = (lng >>> 24) & 0xFF0000L;
        long b4 = (lng >>> 8) & 0xFF000000L;
        long b3 = (lng & 0xFF000000L) << 8;
        long b2 = (lng & 0xFF0000L) << 24;
        long b1 = (lng & 0xFF00L) << 40;
        long b0 = lng << 56;
        return (b0 | b1 | b2 | b3 | b4 | b5 | b6 | b7);
    }

    /**
     * <p>
     * Reverses the bytes of a <code>long</code>.
     * </p>
     * 
     * @param lng
     *            The <code>long</code> to reverse.
     * @return The reversed value.
     * @since 1.5
     */
    public static long reverse(long lng) {
        // From Hacker's Delight, 7-1, Figure 7-1
        lng = (lng & 0x5555555555555555L) << 1 | (lng >> 1)
                & 0x5555555555555555L;
        lng = (lng & 0x3333333333333333L) << 2 | (lng >> 2)
                & 0x3333333333333333L;
        lng = (lng & 0x0F0F0F0F0F0F0F0FL) << 4 | (lng >> 4)
                & 0x0F0F0F0F0F0F0F0FL;
        return reverseBytes(lng);
    }
    
    public static String toHexString(long t) {
        return Long.toString(t, 16);
    }

    /**
     * <p>
     * The <code>signum</code> function for <code>long</code> values. This
     * method returns -1 for negative values, 1 for positive values and 0 for
     * the value 0.
     * </p>
     * 
     * @param lng
     *            The <code>long</code> value.
     * @return -1 if negative, 1 if positive otherwise 0.
     * @since 1.5
     */
    public static int signum(long lng) {
        return (lng == 0 ? 0 : (lng < 0 ? -1 : 1));
    }

    /**
     * <p>
     * Returns a <code>Long</code> instance for the <code>long</code> value
     * passed. This method is preferred over the constructor, as this method may
     * maintain a cache of instances.
     * </p>
     * 
     * @param lng
     *            The long value.
     * @return A <code>Long</code> instance.
     * @since 1.5
     */
    public static Long valueOf(long lng) {
        if (lng < -128 || lng > 127) {
            return new Long(lng);
        }
        return valueOfCache.CACHE[128+(int)lng];
    }

    static class valueOfCache {
        /**
         * <p>
         * A cache of instances used by {@link Long#valueOf(long)} and auto-boxing.
         * </p>
         */
        static final Long[] CACHE = new Long[256];

        static {
            for(int i=-128; i<=127; i++) {
                CACHE[i+128] = new Long(i);
            }
        }
    }

}
