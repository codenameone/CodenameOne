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
import java.io.*;
import java.util.*;

public class Exceptions {
    static class MyException extends Exception {
        final int code;

        MyException(String msg, int code) {
            super(msg);
            this.code = code;
        }
    }

    static class Res implements AutoCloseable {
        final String name;
        final boolean failClose;

        Res(String name, boolean failClose) {
            this.name = name;
            this.failClose = failClose;
            System.out.println("open " + name);
        }

        public void close() throws IOException {
            System.out.println("close " + name);
            if (failClose) {
                throw new IOException("close failed " + name);
            }
        }
    }

    static int finallyReturn() {
        try {
            return 1;
        } finally {
            System.out.println("finally runs");
        }
    }

    @SuppressWarnings("finally")
    static int finallyOverrides() {
        try {
            throw new RuntimeException("x");
        } finally {
            return 2;
        }
    }

    static String nested(int mode) {
        StringBuilder log = new StringBuilder();
        try {
            try {
                log.append("a");
                if (mode == 1) {
                    throw new IllegalStateException("one");
                }
                if (mode == 2) {
                    return log.append("r").toString();
                }
                log.append("b");
            } catch (IllegalStateException e) {
                log.append("c:").append(e.getMessage());
                if (mode == 1) {
                    throw new RuntimeException("wrapped", e);
                }
            } finally {
                log.append("f1");
            }
            log.append("x");
        } catch (RuntimeException e) {
            log.append("outer:").append(e.getMessage()).append(":").append(e.getCause().getMessage());
        } finally {
            log.append("f2");
        }
        return log.toString();
    }

    static void thrower(int i) throws MyException {
        if (i > 1) {
            throw new MyException("too big " + i, i);
        }
    }

    static int loopWithFinally() {
        int count = 0;
        for (int i = 0; i < 5; i++) {
            try {
                if (i == 1) {
                    continue;
                }
                if (i == 3) {
                    break;
                }
                count += 10;
            } finally {
                count++;
            }
        }
        return count;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(finallyReturn());
        System.out.println(finallyOverrides());
        System.out.println(nested(0));
        System.out.println(nested(1));
        System.out.println(nested(2));
        for (int i = 0; i < 3; i++) {
            try {
                thrower(i);
                System.out.println("ok " + i);
            } catch (MyException e) {
                System.out.println(e.getMessage() + " code=" + e.code);
            }
        }
        try {
            Object o = "s";
            Integer bad = (Integer) o;
            System.out.println(bad);
        } catch (ClassCastException | ArithmeticException e) {
            System.out.println("multi " + e.getClass().getSimpleName());
        }
        try {
            int[] arr = new int[2];
            arr[5] = 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("aioobe");
        }
        try {
            System.out.println(10 / (args.length));
        } catch (ArithmeticException e) {
            System.out.println("div " + e.getMessage());
        }
        try (Res a = new Res("A", false); Res b = new Res("B", false)) {
            System.out.println("body " + a.name + b.name);
        }
        try (Res a = new Res("C", true)) {
            throw new IllegalStateException("in body");
        } catch (IllegalStateException e) {
            System.out.println("caught " + e.getMessage() + " suppressed=" + e.getSuppressed().length + " " + e.getSuppressed()[0].getMessage());
        }
        try (Res a = new Res("D", true)) {
            System.out.println("fine body");
        } catch (IOException e) {
            System.out.println("close exception " + e.getMessage());
        }
        Res outer = new Res("E", false);
        try (outer) {
            System.out.println("existing var");
        }
        System.out.println(loopWithFinally());
        Object lock = new Object();
        int total = 0;
        synchronized (lock) {
            total += 5;
        }
        System.out.println(total);
        try {
            synchronized (lock) {
                throw new RuntimeException("sync throw");
            }
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            pw.print("written");
        }
        System.out.println(sw);
        label:
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 2) {
                    continue label;
                }
                if (i == 2) {
                    break label;
                }
                System.out.print(i + "" + j + " ");
            }
        }
        System.out.println();
        int w = 0;
        do {
            w += 3;
        } while (w < 10);
        System.out.println(w);
        block:
        {
            if (w > 5) {
                break block;
            }
            System.out.println("not printed");
        }
        try {
            recurse(0);
        } catch (StackOverflowError e) {
            System.out.println("deep");
        }
    }

    static int recurse(int n) {
        return recurse(n + 1) + 1;
    }
}
