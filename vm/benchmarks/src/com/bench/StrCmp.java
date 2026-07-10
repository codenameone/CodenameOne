package com.bench;
import java.util.Arrays;
public class StrCmp {
    public static void main(String[] args) {
        long ck = 0;
        String[] words = new String[400];
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            b.setLength(0);
            b.append("prefix-").append(i % 37).append("-").append((char) ('a' + (i % 26))).append(i);
            words[i] = b.toString();
        }
        // equals: identical / same-content-different-identity / differ at each position
        for (int i = 0; i < 400; i++) {
            for (int j = 0; j < 400; j += 7) {
                ck += words[i].equals(words[j]) ? 1 : 2;
            }
        }
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        for (int p = 0; p < base.length(); p++) {
            char[] c = base.toCharArray();
            c[p] ^= 1;
            String mut = new String(c);
            ck += base.equals(mut) ? 100 : 3;
            ck += base.compareTo(mut);
            ck += mut.compareTo(base);
        }
        // hash-precheck interplay: compare AFTER hashing both
        String h1 = ("dyn" + 42).intern() + "x";
        String h2 = "dyn42y";
        ck += h1.hashCode() + h2.hashCode();
        ck += h1.equals(h2) ? 4 : 5;
        // compareTo: lengths, prefixes, unicode ordering
        String[] samples = {"", "a", "ab", "abc", "abd", "ab\u00e9", "ab\uffff", "\ud800\udc00", "zzz", "zz"};
        for (String x : samples) for (String y : samples) {
            int c = x.compareTo(y);
            ck = ck * 31 + (c < 0 ? -1 : c > 0 ? 1 : 0) + c;
        }
        // sorting stability of the full word set
        String[] sorted = new String[400];
        System.arraycopy(words, 0, sorted, 0, 400);
        Arrays.sort(sorted);
        for (int i = 0; i < 400; i += 13) ck = ck * 31 + sorted[i].hashCode();
        // charAt must bound by LOGICAL length, not backing-array capacity
        // (builder buffers are larger than the built string)
        StringBuilder cb = new StringBuilder(); // 32-char capacity
        cb.append("abc");
        String shortStr = cb.toString();
        ck += shortStr.charAt(2);
        try {
            ck += shortStr.charAt(3); // == length(): must throw
            ck += 100000;
        } catch (IndexOutOfBoundsException e) {
            ck += 41;
        }
        try {
            ck += shortStr.charAt(-1);
            ck += 100000;
        } catch (IndexOutOfBoundsException e) {
            ck += 43;
        }

        System.out.println("CK " + ck);
        System.out.println("DONE");
    }
}
