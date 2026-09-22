package com.bench;

/**
 * StringBuilder.capacity() must match the JDK exactly, through every path that sets
 * it. The inline constructor intrinsic once installed a heap builder's storage
 * without recording its capacity, so new StringBuilder().capacity() answered 0
 * where the JDK answers 16; an escape-proven (stack) builder, which took the
 * native, answered 16. Nothing else noticed, because growth recovers from 0.
 * Covers heap and stack builders, the sized constructor, in-place growth that
 * fits the existing storage, growth that does not, ensureCapacity and trimToSize.
 */
public class SbCapacityT {
    static StringBuilder keep;

    static int local() {
        StringBuilder s = new StringBuilder();
        s.append("abcdefghijklmnopq");          // 17 chars: grows 16 -> 34
        return s.capacity() * 1000 + s.length();
    }

    public static void main(String[] args) {
        StringBuilder h = new StringBuilder();
        keep = h;
        System.out.println("heap default=" + h.capacity());
        h.append("abcdefghijklmnopq");
        System.out.println("heap grown=" + h.capacity() + " len=" + h.length());
        h.append("0123456789012345678901234567890123456789");
        System.out.println("heap grown2=" + h.capacity() + " len=" + h.length());
        System.out.println("local=" + local());
        StringBuilder s = new StringBuilder(40);
        keep = s;
        System.out.println("sized=" + s.capacity());
        s.ensureCapacity(100);
        System.out.println("ensured=" + s.capacity());
        s.append("xyz");
        s.trimToSize();
        System.out.println("trimmed=" + s.capacity() + " " + s);
        StringBuilder w = new StringBuilder();
        keep = w;
        w.append("abc").append('\u20ac').append("defghijklmnopqrstu");
        System.out.println("wide=" + w.capacity() + " len=" + w.length());
        StringBuilder f = new StringBuilder("seed");
        keep = f;
        System.out.println("fromString=" + f.capacity());
    }
}
