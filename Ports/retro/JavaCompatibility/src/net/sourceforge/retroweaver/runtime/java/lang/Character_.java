package net.sourceforge.retroweaver.runtime.java.lang;

public class Character_ {

	private Character_() {
		// private constructor
	}

	private static Character[] boxedVals = new Character[256];

	// Small lookup table for boxed objects
	//
	// The spec says that the range should be from -127 to 128,
	// but a byte's range is from -128 to 127. Neal Gafter seems to imply
	// that this is a bug in the spec.
	static {
		for (int i = 0; i < 256; ++i) {
			byte val = (byte) (i - 128);
			boxedVals[i] = new Character((char) val); // NOPMD by xlv
		}
	}

	public static Character valueOf(final char val) {
		if (val > -129 && val < 128) {
			return boxedVals[val + 128];
		} else {
			return new Character(val);
		}
	}

	public static final char MIN_HIGH_SURROGATE = '\uD800';
	public static final char MAX_HIGH_SURROGATE = '\uDBFF';
	public static final char MIN_LOW_SURROGATE = '\uDC00';
	public static final char MAX_LOW_SURROGATE = '\uDFFF';
	public static final char MIN_SURROGATE = '\uD800';
	public static final char MAX_SURROGATE = '\uDFFF';
	public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x10000;
	public static final int MIN_CODE_POINT = 0x0;
	public static final int MAX_CODE_POINT = 0x10ffff;
	public static final int SIZE = 0x10;


	public static boolean isLowerCase(int codePoint) {
		if (codePoint >= MIN_CODE_POINT && codePoint < MIN_SUPPLEMENTARY_CODE_POINT) {
			return Character.isLowerCase((char) codePoint);
		}
		return false;
	}

	public static boolean isUpperCase(int codePoint) {
		if (codePoint >= MIN_CODE_POINT && codePoint < MIN_SUPPLEMENTARY_CODE_POINT) {
			return Character.isUpperCase((char) codePoint);
		}
		return false;
	}

}
