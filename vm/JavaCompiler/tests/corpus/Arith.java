/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
public class Arith {
    static final int K = 3;
    static final String S = "con" + "st" + K;
    static final long BIG = 1L << 40;
    static final char C = 'A' + 2;

    public static void main(String[] args) {
        int a = Integer.MAX_VALUE;
        a++;
        System.out.println(a);
        long l = Long.MAX_VALUE;
        System.out.println(l + 1);
        System.out.println(7 / 2 + " " + (-7 / 2) + " " + (-7 % 3) + " " + (7.0 / 2) + " " + (7 % -3));
        System.out.println(1 << 33);
        System.out.println(1L << 33);
        System.out.println(-16 >> 2);
        System.out.println(-16 >>> 28);
        System.out.println(-16L >>> 60);
        System.out.println(0x7f & 0x3c | 0x100 ^ 0xff);
        System.out.println(~5 + " " + -(-3));
        double nan = 0.0 / 0.0;
        System.out.println((nan < 1) + " " + (nan > 1) + " " + (nan == nan) + " " + (nan != nan) + " " + (1.0 / 0));
        float fl = 1.1f;
        System.out.println(fl * 3 + " " + (double) fl + " " + (int) 3.99 + " " + (int) -3.99 + " " + (long) 1e19 + " " + (int) Float.NaN);
        byte b = (byte) 200;
        short sh = (short) 70000;
        char ch = (char) 65;
        System.out.println(b + " " + sh + " " + ch + " " + (int) ch + " " + (char) (ch + 1));
        ch++;
        ch += 2;
        System.out.println(ch);
        b += 100;
        System.out.println(b);
        int x = 5;
        x *= 2.5;
        System.out.println(x);
        x >>= 1;
        x <<= 3;
        x ^= 7;
        x %= 6;
        System.out.println(x);
        System.out.println(S + " " + BIG + " " + C + " " + K);
        Integer boxed = 127;
        Integer boxed2 = 127;
        System.out.println((boxed == boxed2) + " " + boxed.equals(boxed2));
        Integer counter = 0;
        counter++;
        counter += 5;
        System.out.println(counter);
        Long lg = 5L;
        long sum = lg + 10;
        System.out.println(sum);
        Double d = 2.5;
        double dd = d * 2;
        System.out.println(dd);
        Character cc = 'q';
        int ci = cc + 1;
        System.out.println(ci);
        Boolean flag = true;
        if (flag && !Boolean.FALSE) {
            System.out.println("flag");
        }
        Object o = 3;
        int fromObj = (Integer) o;
        int fromObj2 = (int) o;
        System.out.println(fromObj + fromObj2);
        long mixed = 3 + 4L * 2;
        System.out.println(mixed);
        System.out.println(1 + 2 + "s" + 1 + 2);
        System.out.println('a' + 'b' + "c" + 'd' + 1.5f + true + null + 3L);
        char[] chars = {'h', 'i'};
        System.out.println(String.valueOf(chars) + chars.length);
        String s = null;
        s += "x";
        System.out.println(s);
        int[] arr = new int[3];
        arr[1] += 5;
        arr[2]++;
        ++arr[0];
        System.out.println(arr[0] + arr[1] + arr[2]);
        long[] la = {1, 2};
        la[0] <<= 4;
        System.out.println(la[0]);
        int[][] grid = new int[3][4];
        grid[1][2] = 9;
        System.out.println(grid[1][2] + grid.length + grid[0].length);
        int[][] jag = {{1}, {2, 3}, {}};
        System.out.println(jag[1][1] + jag.length);
        String[][] names = new String[2][];
        names[0] = new String[]{"a"};
        System.out.println(names[0][0] + names[1]);
        double tern = true ? 1 : 2.0;
        System.out.println(tern);
        Integer nullable = null;
        Object pick = false ? nullable : "str";
        System.out.println(pick);
        int i = 0;
        i = i++ + ++i;
        System.out.println(i);
        System.out.println(Math.max(3, 7L) + Math.abs(-2.5));
        System.out.println(Integer.toBinaryString(42) + Long.toHexString(255L) + Integer.parseInt("-12"));
        System.out.println(0.1 + 0.2);
        System.out.println(100.0f / 3);
        System.out.println((float) 1e10 + " " + 1e-5 + " " + 123456789.0 + " " + 1.0E7);
        System.out.println((byte) -129 + " " + (short) -32769 + " " + (char) -1 / 2);
        final int local = 10;
        switch (5) {
            case local - 5:
                System.out.println("const label");
                break;
            default:
                System.out.println("no");
        }
    }
}
