package com.bench;

/**
 * REPRODUCER, NOT A TORTURE -- deliberately NOT in run-gauntlet.sh, because its output
 * does NOT match the host JVM and the defect it shows is pre-existing.
 *
 * String.toUpperCase/toLowerCase are natives. On Apple targets they go through NSString
 * and behave correctly. Everywhere else -- Linux, Windows, the clean target -- they
 * reach cn1StringConvertCase, which calls towupper/towlower, and those run in the "C"
 * locale where they case ASCII and nothing else. So a Latin-1 accented string comes back
 * unchanged where the JDK cases it, micro sign is left alone where the JDK maps it to
 * capital Mu, and the Turkish dotted capital I differs in LENGTH as well: the JDK
 * lowercases it to two code units and this returns one.
 *
 * Nothing in the gauntlet covers non-ASCII case conversion, which is why this survived --
 * StrCmp exercises equals/compareTo/sort and surrogates, not casing.
 *
 * VERIFIED PRE-EXISTING: this file's output is byte-identical before and after the
 * compact-representation change to cn1StringConvertCase, so that change is not the cause.
 * Fixing the locale behaviour is separate work and needs its own gate.
 *
 * Build with translate-and-build.sh and diff against the host by hand. The literals are
 * escaped because Java sources in this tree must be pure ASCII.
 */
public class CaseT {
    public static void main(String[] a) {
        String[] s = {
            "hello world",
            "MiXeD CaSe 123",
            "",
            "x",
            "\u00FF\u00E9\u00E0",       // y-diaeresis, e-acute, a-grave: all Latin-1
            "\u00B5abc",                 // micro sign: Latin-1 in, non-Latin-1 out on a JDK
            "\u0130stanbul",             // dotted capital I: JDK lowercases to TWO units
            "caf\u00E9",
            "\u4E2D\u6587abc",          // CJK: never Latin-1
            "ALREADY UPPER",
            "already lower"
        };
        long ck = 0;
        for (String x : s) {
            String u = x.toUpperCase();
            String l = x.toLowerCase();
            System.out.println("[" + x.length() + "] ul=" + u.length() + " ll=" + l.length()
                    + " eqU=" + (u == x) + " eqL=" + (l == x));
            for (int i = 0; i < u.length(); i++) {
                ck += u.charAt(i) * 31 + i;
            }
            for (int i = 0; i < l.length(); i++) {
                ck += l.charAt(i) * 17 + i;
            }
            ck += u.hashCode() + l.hashCode() + (u.equals(l) ? 1 : 0) + u.compareTo(l);
        }
        System.out.println("checksum=" + ck);
    }
}
