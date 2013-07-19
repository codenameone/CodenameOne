package net.sourceforge.retroweaver.runtime.java.io;

import java.lang.reflect.Method;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Replacements for methods added to java.io.Closeable in Java 1.5, used
 * for targets of the "foreach" statement.
 */
public final class Closeable_ {

	private Closeable_() {
		// private constructor
	}

	public static void close(final Object o) throws IOException {
		if (o == null) {
			throw new NullPointerException(); // NOPMD by xlv
		}

		/* check for common java.io.* ancestors */
		if (o instanceof InputStream) {
			((InputStream) o).close();
		} else if (o instanceof OutputStream) {
			((OutputStream) o).close();
		} else if (o instanceof Reader) {
			((Reader) o).close();
		} else if (o instanceof Writer) {
			((Writer) o).close();
		} else if (o instanceof net.sourceforge.retroweaver.runtime.java.io.Closeable) {
			// weaved classes inheriting from Closeable
			((net.sourceforge.retroweaver.runtime.java.io.Closeable) o).close();
		} else {
			// use reflection to try to get the close method if it was present pre 1.5
			throw new RuntimeException("close() call on " + o.getClass());
		}
	}

}
