package net.sourceforge.retroweaver.runtime.java.lang;

public class Float_ {

	private Float_() {
		// private constructor
	}

	public static Float valueOf(final float val) {
		return new Float(val);
	}

    public static String toHexString(float f) {
        /*
         * Reference: http://en.wikipedia.org/wiki/IEEE_754
         */
        if (f != f) {
            return "NaN"; //$NON-NLS-1$
        }
        if (f == Float.POSITIVE_INFINITY) {
            return "Infinity"; //$NON-NLS-1$
        }
        if (f == Float.NEGATIVE_INFINITY) {
            return "-Infinity"; //$NON-NLS-1$
        }

        int bitValue = Float.floatToIntBits(f);

        boolean negative = (bitValue & 0x80000000) != 0;
        // mask exponent bits and shift down
        int exponent = (bitValue & 0x7f800000) >>> 23;
        // mask significand bits and shift up
        // significand is 23-bits, so we shift to treat it like 24-bits
        int significand = (bitValue & 0x007FFFFF) << 1;

        if (exponent == 0 && significand == 0) {
            return (negative ? "-0x0.0p0" : "0x0.0p0"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        StringBuffer hexString = new StringBuffer(10);
        if (negative) {
            hexString.append("-0x"); //$NON-NLS-1$
        } else {
            hexString.append("0x"); //$NON-NLS-1$
        }

        if (exponent == 0) { // denormal (subnormal) value
            hexString.append("0."); //$NON-NLS-1$
            // significand is 23-bits, so there can be 6 hex digits
            int fractionDigits = 6;
            // remove trailing hex zeros, so Integer.toHexString() won't print
            // them
            while ((significand != 0) && ((significand & 0xF) == 0)) {
                significand >>>= 4;
                fractionDigits--;
            }
            // this assumes Integer.toHexString() returns lowercase characters
            String hexSignificand = Integer.toHexString(significand);

            // if there are digits left, then insert some '0' chars first
            if (significand != 0 && fractionDigits > hexSignificand.length()) {
                int digitDiff = fractionDigits - hexSignificand.length();
                while (digitDiff-- != 0) {
                    hexString.append('0');
                }
            }
            hexString.append(hexSignificand);
            hexString.append("p-126"); //$NON-NLS-1$
        } else { // normal value
            hexString.append("1."); //$NON-NLS-1$
            // significand is 23-bits, so there can be 6 hex digits
            int fractionDigits = 6;
            // remove trailing hex zeros, so Integer.toHexString() won't print
            // them
            while ((significand != 0) && ((significand & 0xF) == 0)) {
                significand >>>= 4;
                fractionDigits--;
            }
            // this assumes Integer.toHexString() returns lowercase characters
            String hexSignificand = Integer.toHexString(significand);

            // if there are digits left, then insert some '0' chars first
            if (significand != 0 && fractionDigits > hexSignificand.length()) {
                int digitDiff = fractionDigits - hexSignificand.length();
                while (digitDiff-- != 0) {
                    hexString.append('0');
                }
            }
            hexString.append(hexSignificand);
            hexString.append('p');
            // remove exponent's 'bias' and convert to a string
            hexString.append(Integer.toString(exponent - 127));
        }
        return hexString.toString();
    }
}
