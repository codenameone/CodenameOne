package net.sourceforge.retroweaver.runtime.java.lang;

public class System_ {

	private System_() {
		// private constructor
	}

	/**
	 * @since 1.5
	 */
	public static long nanoTime() {
		return System.currentTimeMillis() * 1000000L;
	}

}
