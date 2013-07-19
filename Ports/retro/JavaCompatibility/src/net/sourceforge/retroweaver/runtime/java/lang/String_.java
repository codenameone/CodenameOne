package net.sourceforge.retroweaver.runtime.java.lang;


public class String_ {

	private String_() {
		// private constructor
	}

	public static String replace(String s, net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence target,
            net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence replacement) {
		
        if (target == null) {
            throw new NullPointerException("target should not be null");
        }
        if (replacement == null) {
            throw new NullPointerException("replacement should not be null");
        }
        String ts = target.toString();
        int index = s.indexOf(ts, 0);

        if (index == -1)
            return s;

        String rs = replacement.toString();
        StringBuffer buffer = new StringBuffer(s.length());
        int tl = target.length();
        int tail = 0;
        char[] value = s.toCharArray();
        int offset = 0;
        do {
            buffer.append(value, offset + tail, index - tail);
            buffer.append(rs);
            tail = index + tl;
        } while ((index = s.indexOf(ts, tail)) != -1);
        //append trailing chars 
        buffer.append(value, offset + tail, s.length() - tail);

        return buffer.toString();
	}

	public static boolean contains(String s, net.sourceforge.retroweaver.harmony.runtime.java.lang.CharSequence seq) {
		if (seq == null) {
			throw new NullPointerException();
		}
		return s.indexOf(seq.toString()) != -1;
	}


    /**
     * Searches in this string for the last index of the specified string. The
     * search for the string starts at the end and moves towards the beginning
     * of this string.
     * 
     * @param string
     *            the string to find.
     * @return the index of the first character of the specified string in this
     *         string, -1 if the specified string is not a substring.
     * @throws NullPointerException
     *             if {@code string} is {@code null}.
     */
    public static int lastIndexOf(String orig, String string) {
        // Use count instead of count - 1 so lastIndexOf("") answers count
        return orig.lastIndexOf(string, orig.length());
    }

    /**
     * Searches in this string for the index of the specified string. The search
     * for the string starts at the specified offset and moves towards the
     * beginning of this string.
     * 
     * @param subString
     *            the string to find.
     * @param start
     *            the starting offset.
     * @return the index of the first character of the specified string in this
     *         string , -1 if the specified string is not a substring.
     * @throws NullPointerException
     *             if {@code subString} is {@code null}.
     */
    public static int lastIndexOf(String orig, String subString, int start) {
        int count = orig.length();
        int subCount = subString.length();
        if (subCount <= count && start >= 0) {
            if (subCount > 0) {
                if (start > count - subCount) {
                    start = count - subCount;
                }
                // count and subCount are both >= 1
                char[] target = subString.toCharArray();
                int subOffset = 0;
                char firstChar = target[subOffset];
                int end = subOffset + subCount;
                while (true) {
                    int i = orig.lastIndexOf(firstChar, start);
                    if (i == -1) {
                        return -1;
                    }
                    int o1 = i, o2 = subOffset;
                    while (++o2 < end && orig.charAt(++o1) == target[o2]) {
                        // Intentionally empty
                    }
                    if (o2 == end) {
                        return i;
                    }
                    start = i - 1;
                }
            }
            return start < count ? start : count;
        }
        return -1;
    }
}
