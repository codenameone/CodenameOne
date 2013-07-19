package net.sourceforge.retroweaver.runtime.java.lang;

public class Boolean_ {

	private Boolean_() {
		// private constructor
	}

	public static Boolean valueOf(final boolean b) {
		return b ? Boolean.TRUE : Boolean.FALSE;
	}

	public static boolean parseBoolean(final String s) {
		return (s != null) && s.equalsIgnoreCase("true");
	}

	public static int compareTo(final Boolean b1, final Boolean b2) {
		return (b1.booleanValue() == b2.booleanValue()) ? 0 : (b1.booleanValue() ? 1 : -1);
	}

}
