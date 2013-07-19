package net.sourceforge.retroweaver.runtime.java.io;

import java.io.PrintStream;

public class PrintStream_ {

	public static PrintStream append(PrintStream s, char c) {
		s.print(c);
		return s;
	}

	public static PrintStream append(PrintStream s, net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence csq) {
		s.print(csq==null?"null":csq.toString());
		return s;
	}

	public static PrintStream append(PrintStream s, net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence csq, int start, int end) {
		s.print(csq==null?"null".substring(start, end):csq.subSequence(start, end).toString());
		return s;
	}

}
