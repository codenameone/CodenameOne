package net.sourceforge.retroweaver.runtime.java.io;

import java.io.IOException;
import java.io.Writer;

public class Writer_ {

	public static Writer append(Writer w, char c) throws IOException {
		w.write(c);
		return w;
	}

	public static Writer append(Writer w, net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence csq) throws IOException {
		w.write(csq==null?"null":csq.toString());
		return w;
	}

	public static Writer append(Writer w, net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence csq, int start, int end)
			throws IOException {
		w.write(csq==null?"null".substring(start, end):csq.subSequence(start, end).toString());
		return w;
	}

}
