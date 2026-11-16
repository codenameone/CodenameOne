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
package com.codename1.impl.pdf;

import java.util.ArrayList;
import java.util.List;

/// The PostScript calculator of type 4 functions. A program compiles to
/// an array of `Double` operands, `String` operators and nested `Object[]`
/// procedures; booleans are 1 and 0 on the stack.
final class PdfCalculator {
    private static final int MAX_STACK = 100;
    private static final int MAX_DEPTH = 16;

    private PdfCalculator() {
    }

    static Object[] compile(byte[] src) {
        int[] pos = {0};
        while (pos[0] < src.length && src[pos[0]] != '{') {
            pos[0]++;
        }
        if (pos[0] >= src.length) {
            return null;
        }
        pos[0]++;
        return block(src, pos, 0);
    }

    private static Object[] block(byte[] s, int[] pos, int depth) {
        List<Object> out = new ArrayList<Object>();
        while (pos[0] < s.length) {
            int c = s[pos[0]] & 0xff;
            if (PdfFormat.isWhitespace(c)) {
                pos[0]++;
            } else if (c == '{') {
                pos[0]++;
                if (depth >= MAX_DEPTH) {
                    return null;
                }
                Object[] b = block(s, pos, depth + 1);
                if (b == null) {
                    return null;
                }
                out.add(b);
            } else if (c == '}') {
                pos[0]++;
                return out.toArray();
            } else {
                int start = pos[0];
                while (pos[0] < s.length && !PdfFormat.isWhitespace(s[pos[0]] & 0xff) && s[pos[0]] != '{'
                        && s[pos[0]] != '}') {
                    pos[0]++;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = start; i < pos[0]; i++) {
                    sb.append((char) (s[i] & 0xff));
                }
                String t = sb.toString();
                char f = t.charAt(0);
                if (f >= '0' && f <= '9' || f == '-' || f == '+' || f == '.') {
                    try {
                        out.add(Double.valueOf(Double.parseDouble(t)));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                } else if ("true".equals(t)) {
                    out.add(Double.valueOf(1));
                } else if ("false".equals(t)) {
                    out.add(Double.valueOf(0));
                } else {
                    out.add(t);
                }
            }
        }
        return out.toArray();
    }

    static double[] run(Object[] program, double[] in, int outs) {
        double[] st = new double[MAX_STACK];
        int[] sp = {0};
        for (int i = 0; i < in.length && i < MAX_STACK; i++) {
            st[sp[0]++] = in[i];
        }
        exec(program, st, sp, 0);
        double[] out = new double[outs];
        int base = sp[0] - outs;
        for (int i = 0; i < outs; i++) {
            out[i] = base + i >= 0 ? st[base + i] : 0;
        }
        return out;
    }

    private static void exec(Object[] p, double[] st, int[] spRef, int depth) {
        if (depth > MAX_DEPTH) {
            return;
        }
        int sp = spRef[0];
        for (int i = 0; i < p.length; i++) {
            Object o = p[i];
            if (o instanceof Double) {
                if (sp < MAX_STACK) {
                    st[sp++] = ((Double) o).doubleValue();
                }
                continue;
            }
            if (o instanceof Object[]) {
                // a procedure is consumed by the if / ifelse that follows
                Object next = i + 1 < p.length ? p[i + 1] : null;
                if ("if".equals(next)) {
                    boolean c = sp > 0 && st[--sp] != 0;
                    if (c) {
                        spRef[0] = sp;
                        exec((Object[]) o, st, spRef, depth + 1);
                        sp = spRef[0];
                    }
                    i++;
                } else if (next instanceof Object[] && i + 2 < p.length && "ifelse".equals(p[i + 2])) {
                    boolean c = sp > 0 && st[--sp] != 0;
                    spRef[0] = sp;
                    exec(c ? (Object[]) o : (Object[]) next, st, spRef, depth + 1);
                    sp = spRef[0];
                    i += 2;
                }
                continue;
            }
            if (!(o instanceof String)) {
                continue;
            }
            String op = (String) o;
            if (sp < 1 && !"true".equals(op)) {
                break;
            }
            double a = st[sp - 1];
            double b = sp >= 2 ? st[sp - 2] : 0;
            if ("add".equals(op)) {
                st[sp - 2] = b + a;
                sp--;
            } else if ("sub".equals(op)) {
                st[sp - 2] = b - a;
                sp--;
            } else if ("mul".equals(op)) {
                st[sp - 2] = b * a;
                sp--;
            } else if ("div".equals(op)) {
                st[sp - 2] = a == 0 ? 0 : b / a;
                sp--;
            } else if ("idiv".equals(op)) {
                st[sp - 2] = (long) a == 0 ? 0 : truncate((double) (long) b / (long) a);
                sp--;
            } else if ("mod".equals(op)) {
                st[sp - 2] = (long) a == 0 ? 0 : (double) ((long) b % (long) a);
                sp--;
            } else if ("neg".equals(op)) {
                st[sp - 1] = -a;
            } else if ("abs".equals(op)) {
                st[sp - 1] = Math.abs(a);
            } else if ("ceiling".equals(op)) {
                st[sp - 1] = Math.ceil(a);
            } else if ("floor".equals(op)) {
                st[sp - 1] = Math.floor(a);
            } else if ("round".equals(op)) {
                st[sp - 1] = Math.floor(a + 0.5);
            } else if ("truncate".equals(op)) {
                st[sp - 1] = a < 0 ? Math.ceil(a) : Math.floor(a);
            } else if ("cvi".equals(op)) {
                st[sp - 1] = (long) a;
            } else if ("cvr".equals(op)) {
                st[sp - 1] = a;
            } else if ("sqrt".equals(op)) {
                st[sp - 1] = Math.sqrt(Math.abs(a));
            } else if ("sin".equals(op)) {
                st[sp - 1] = Math.sin(Math.toRadians(a));
            } else if ("cos".equals(op)) {
                st[sp - 1] = Math.cos(Math.toRadians(a));
            } else if ("atan".equals(op)) {
                double deg = Math.toDegrees(atan2(b, a));
                st[sp - 2] = deg < 0 ? deg + 360 : deg;
                sp--;
            } else if ("exp".equals(op)) {
                st[sp - 2] = pow(b, a);
                sp--;
            } else if ("ln".equals(op)) {
                st[sp - 1] = a > 0 ? log(a) : 0;
            } else if ("log".equals(op)) {
                st[sp - 1] = a > 0 ? log(a) / log(10) : 0;
            } else if ("dup".equals(op)) {
                if (sp < MAX_STACK) {
                    st[sp++] = a;
                }
            } else if ("pop".equals(op)) {
                sp--;
            } else if ("exch".equals(op)) {
                if (sp >= 2) {
                    st[sp - 1] = b;
                    st[sp - 2] = a;
                }
            } else if ("copy".equals(op)) {
                int n = (int) a;
                sp--;
                if (n > 0 && n <= sp && sp + n <= MAX_STACK) {
                    System.arraycopy(st, sp - n, st, sp, n);
                    sp += n;
                }
            } else if ("index".equals(op)) {
                int n = (int) a;
                st[sp - 1] = n >= 0 && n < sp - 1 ? st[sp - 2 - n] : 0;
            } else if ("roll".equals(op)) {
                int j = (int) a;
                int n = (int) b;
                sp -= 2;
                if (n > 0 && n <= sp) {
                    j = ((j % n) + n) % n;
                    double[] t = new double[n];
                    for (int k = 0; k < n; k++) {
                        t[(k + j) % n] = st[sp - n + k];
                    }
                    System.arraycopy(t, 0, st, sp - n, n);
                }
            } else if ("eq".equals(op)) {
                st[sp - 2] = b == a ? 1 : 0;
                sp--;
            } else if ("ne".equals(op)) {
                st[sp - 2] = b != a ? 1 : 0;
                sp--;
            } else if ("gt".equals(op)) {
                st[sp - 2] = b > a ? 1 : 0;
                sp--;
            } else if ("ge".equals(op)) {
                st[sp - 2] = b >= a ? 1 : 0;
                sp--;
            } else if ("lt".equals(op)) {
                st[sp - 2] = b < a ? 1 : 0;
                sp--;
            } else if ("le".equals(op)) {
                st[sp - 2] = b <= a ? 1 : 0;
                sp--;
            } else if ("and".equals(op)) {
                st[sp - 2] = (double) ((long) b & (long) a);
                sp--;
            } else if ("or".equals(op)) {
                st[sp - 2] = (double) ((long) b | (long) a);
                sp--;
            } else if ("xor".equals(op)) {
                st[sp - 2] = (double) ((long) b ^ (long) a);
                sp--;
            } else if ("not".equals(op)) {
                st[sp - 1] = a == 0 ? 1 : a == 1 ? 0 : (double) ~(long) a;
            } else if ("bitshift".equals(op)) {
                int s = (int) a;
                long v = (long) b;
                st[sp - 2] = s >= 0 ? (double) (v << s) : (double) (v >> -s);
                sp--;
            }
            if (sp < 0) {
                sp = 0;
            }
        }
        spRef[0] = sp;
    }

    // Math.pow, log and atan2 are not in the device class library
    // (Ports/CLDC11); com.codename1.util.MathUtil supplies the first two
    // and atan, from which atan2 follows.
    private static double pow(double a, double b) {
        return com.codename1.util.MathUtil.pow(a, b);
    }

    private static double log(double a) {
        return com.codename1.util.MathUtil.log(a);
    }

    static double atan2(double y, double x) {
        if (x > 0) {
            return com.codename1.util.MathUtil.atan(y / x);
        }
        if (x < 0) {
            return com.codename1.util.MathUtil.atan(y / x) + (y >= 0 ? Math.PI : -Math.PI);
        }
        return y > 0 ? Math.PI / 2 : y < 0 ? -Math.PI / 2 : 0;
    }

    /// `v` rounded toward zero, as integer division rounds.
    private static double truncate(double v) {
        return v < 0 ? Math.ceil(v) : Math.floor(v);
    }
}
