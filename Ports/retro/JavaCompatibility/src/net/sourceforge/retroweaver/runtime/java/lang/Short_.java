package net.sourceforge.retroweaver.runtime.java.lang;

public class Short_ {

	private Short_() {
		// private constructor
	}

	private static Short[] boxedVals = new Short[256];

	// Small lookup table for boxed objects
	//
	// The spec says that the range should be from -127 to 128,
	// but a byte's range is from -128 to 127. Neal Gafter seems to imply
	// that this is a bug in the spec.
	static {
		for (int i = 0; i < 256; ++i) {
			byte val = (byte) (i - 128);
			boxedVals[i] = new Short(val); // NOPMD by xlv
		}
	}

	public static Short valueOf(final short val) {
		if (val > -129 && val < 128) {
			return boxedVals[val + 128];
		} else {
			return new Short(val);
		}
	}
}
