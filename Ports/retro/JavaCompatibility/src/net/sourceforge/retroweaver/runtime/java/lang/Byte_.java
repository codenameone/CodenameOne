package net.sourceforge.retroweaver.runtime.java.lang;

public class Byte_ {

	private Byte_() {
		// private constructor
	}

	private static Byte[] boxedVals = new Byte[256];

	// Small lookup table for boxed objects
	//
	// The spec says that the range should be from -127 to 128,
	// but a byte's range is from -128 to 127. Neal Gafter seems to imply
	// that this is a bug in the spec.
	static {
		for (int i = 0; i < 256; ++i) {
			byte val = (byte) (i - 128);
			boxedVals[i] = new Byte(val); // NOPMD by xlv
		}
	}

	public static Byte valueOf(final byte val) {
		return boxedVals[val + 128];
	}

}
