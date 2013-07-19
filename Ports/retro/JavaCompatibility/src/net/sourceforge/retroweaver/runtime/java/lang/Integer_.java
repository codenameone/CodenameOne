package net.sourceforge.retroweaver.runtime.java.lang;

public class Integer_ {

	private Integer_() {
		// private constructor
	}

    /**
     * <p>
     * Constant for the number of bits to represent an <code>int</code> in
     * two's compliment form.
     * </p>
     * 
     * @since 1.5
     */
    public static final int SIZE = 32;

    /**
     * <p>
     * Determines the highest (leftmost) bit that is 1 and returns the value
     * that is the bit mask for that bit. This is sometimes referred to as the
     * Most Significant 1 Bit.
     * </p>
     * 
     * @param i
     *            The <code>int</code> to interrogate.
     * @return The bit mask indicating the highest 1 bit.
     * @since 1.5
     */
    public static int highestOneBit(int i) {
        i |= (i >> 1);
        i |= (i >> 2);
        i |= (i >> 4);
        i |= (i >> 8);
        i |= (i >> 16);
        return (i & ~(i >>> 1));
    }

    /**
     * <p>
     * Determines the lowest (rightmost) bit that is 1 and returns the value
     * that is the bit mask for that bit. This is sometimes referred to as the
     * Least Significant 1 Bit.
     * </p>
     * 
     * @param i
     *            The <code>int</code> to interrogate.
     * @return The bit mask indicating the lowest 1 bit.
     * @since 1.5
     */
    public static int lowestOneBit(int i) {
        return (i & (-i));
    }

    /**
     * <p>
     * Determines the number of leading zeros in the <code>int</code> passed
     * prior to the {@link #highestOneBit(int) highest one bit}.
     * </p>
     * 
     * @param i
     *            The <code>int</code> to process.
     * @return The number of leading zeros.
     * @since 1.5
     */
    public static int numberOfLeadingZeros(int i) {
        i |= i >> 1;
        i |= i >> 2;
        i |= i >> 4;
        i |= i >> 8;
        i |= i >> 16;
        return bitCount(~i);
    }

    /**
     * <p>
     * Determines the number of trailing zeros in the <code>int</code> passed
     * after the {@link #lowestOneBit(int) lowest one bit}.
     * </p>
     * 
     * @param i
     *            The <code>int</code> to process.
     * @return The number of trailing zeros.
     * @since 1.5
     */
    public static int numberOfTrailingZeros(int i) {
        return bitCount((i & -i) - 1);
    }

    /**
     * <p>
     * Counts the number of 1 bits in the <code>int</code> value passed; this
     * is sometimes referred to as a population count.
     * </p>
     * 
     * @param i
     *            The <code>int</code> value to process.
     * @return The number of 1 bits.
     * @since 1.5
     */
    public static int bitCount(int i) {
        i -= ((i >> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >> 2) & 0x33333333);
        i = (((i >> 4) + i) & 0x0F0F0F0F);
        i += (i >> 8);
        i += (i >> 16);
        return (i & 0x0000003F);
    }

    /**
     * <p>
     * Rotates the bits of <code>i</code> to the left by the
     * <code>distance</code> bits.
     * </p>
     * 
     * @param i
     *            The <code>int</code> value to rotate left.
     * @param distance
     *            The number of bits to rotate.
     * @return The rotated value.
     * @since 1.5
     */
    public static int rotateLeft(int i, int distance) {
        if (distance == 0) {
            return i;
        }
        /*
         * According to JLS3, 15.19, the right operand of a shift is always
         * implicitly masked with 0x1F, which the negation of 'distance' is
         * taking advantage of.
         */
        return ((i << distance) | (i >>> (-distance)));
    }

    /**
     * <p>
     * Rotates the bits of <code>i</code> to the right by the
     * <code>distance</code> bits.
     * </p>
     * 
     * @param i
     *            The <code>int</code> value to rotate right.
     * @param distance
     *            The number of bits to rotate.
     * @return The rotated value.
     * @since 1.5
     */
    public static int rotateRight(int i, int distance) {
        if (distance == 0) {
            return i;
        }
        /*
         * According to JLS3, 15.19, the right operand of a shift is always
         * implicitly masked with 0x1F, which the negation of 'distance' is
         * taking advantage of.
         */
        return ((i >>> distance) | (i << (-distance)));
    }

    /**
     * <p>
     * Reverses the bytes of a <code>int</code>.
     * </p>
     * 
     * @param i
     *            The <code>int</code> to reverse.
     * @return The reversed value.
     * @since 1.5
     */
    public static int reverseBytes(int i) {
        int b3 = i >>> 24;
        int b2 = (i >>> 8) & 0xFF00;
        int b1 = (i & 0xFF00) << 8;
        int b0 = i << 24;
        return (b0 | b1 | b2 | b3);
    }

    /**
     * <p>
     * Reverses the bytes of a <code>int</code>.
     * </p>
     * 
     * @param i
     *            The <code>int</code> to reverse.
     * @return The reversed value.
     * @since 1.5
     */
    public static int reverse(int i) {
        // From Hacker's Delight, 7-1, Figure 7-1
        i = (i & 0x55555555) << 1 | (i >> 1) & 0x55555555;
        i = (i & 0x33333333) << 2 | (i >> 2) & 0x33333333;
        i = (i & 0x0F0F0F0F) << 4 | (i >> 4) & 0x0F0F0F0F;
        return reverseBytes(i);
    }

    /**
     * <p>
     * The <code>signum</code> function for <code>int</code> values. This
     * method returns -1 for negative values, 1 for positive values and 0 for
     * the value 0.
     * </p>
     * 
     * @param i
     *            The <code>int</code> value.
     * @return -1 if negative, 1 if positive otherwise 0.
     * @since 1.5
     */
    public static int signum(int i) {
        return (i == 0 ? 0 : (i < 0 ? -1 : 1));
    }

    /**
     * <p>
     * Returns a <code>Integer</code> instance for the <code>int</code>
     * value passed. This method is preferred over the constructor, as this
     * method may maintain a cache of instances.
     * </p>
     * 
     * @param i
     *            The int value.
     * @return A <code>Integer</code> instance.
     * @since 1.5
     */
    public static Integer valueOf(int i) {
        if (i < -128 || i > 127) {
            return new Integer(i);
        }
        return valueOfCache.CACHE [i+128];

    }

   static class valueOfCache {
        /**
         * <p>
         * A cache of instances used by {@link Integer#valueOf(int)} and auto-boxing.
         * </p>
         */
        static final Integer[] CACHE = new Integer[256];

        static {
            for(int i=-128; i<=127; i++) {
                CACHE[i+128] = new Integer(i);
            }
        }
    }


    public static String toHexString(int t) {
        return Integer.toString(t, 16);
    }
}
