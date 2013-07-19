package net.sourceforge.retroweaver.runtime.java.lang;

public class StringBuffer_ {

	private StringBuffer_() {
		// private constructor
	}

	public static StringBuffer StringBuffer(final net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence cs) {
		return new StringBuffer(cs.toString());
	}

	public static void trimToSize(final StringBuffer b) {
		// do nothing: according to the 1.5 javadoc,
		// there is no garantee the buffer capacity will be reduced to
		// fit the actual size
	}

	public static StringBuffer append(final StringBuffer b,
			final net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence cs) {
		return b.append(cs==null?"null":cs.toString());
	}

	public static StringBuffer append(final StringBuffer b,
			final net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence cs, final int start, final int end) {
		return b.append(cs==null?"null".substring(start, end):cs.subSequence(start, end).toString());
	}

	public static StringBuffer insert(final StringBuffer b, final int offset,
			final net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence cs) {
		return b.insert(offset, cs.toString());
	}

	public static StringBuffer insert(final StringBuffer b, final int offset,
			final net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence cs, final int start, final int end) {
		return b.insert(offset, cs.subSequence(start, end).toString());
	}

}
