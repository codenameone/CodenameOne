package net.sourceforge.retroweaver.runtime.java.lang;

public interface Appendable {

	Appendable append(char c);

	Appendable append(net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence csq);

	Appendable append(net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence csq, int start, int end);

}