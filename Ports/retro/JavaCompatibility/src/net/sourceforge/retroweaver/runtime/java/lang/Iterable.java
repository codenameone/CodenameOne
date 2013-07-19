package net.sourceforge.retroweaver.runtime.java.lang;

import net.sourceforge.retroweaver.harmony.runtime.java.util.Iterator;

/**
 * A version of the 1.5 java.lang.Iterable class for the 1.4 VM.
 */
public interface Iterable<E> {
	Iterator<E> iterator();
}
