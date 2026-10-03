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
package com.codename1.androidcompat.jdk;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/// `java.io.PrintWriter` for the Codename One runtime. Like the JDK class it
/// never throws: an I/O error is recorded and reported by `checkError`.
public class PrintWriter extends Writer {

    protected Writer out;
    private final boolean autoFlush;
    private boolean trouble;

    public PrintWriter(Writer out) {
        this(out, false);
    }

    public PrintWriter(Writer out, boolean autoFlush) {
        this.out = out;
        this.autoFlush = autoFlush;
    }

    public PrintWriter(OutputStream out) {
        this(out, false);
    }

    public PrintWriter(OutputStream out, boolean autoFlush) {
        this(utf8(out), autoFlush);
    }

    public PrintWriter(String fileName) throws java.io.FileNotFoundException {
        this(utf8(new FileOutputStream(fileName)), false);
    }

    public PrintWriter(File file) throws java.io.FileNotFoundException {
        this(utf8(new FileOutputStream(file)), false);
    }

    private static Writer utf8(OutputStream out) {
        try {
            return new OutputStreamWriter(out, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported");
        }
    }

    public boolean checkError() {
        if (out != null) {
            flush();
        }
        return trouble;
    }

    protected void setError() {
        trouble = true;
    }

    protected void clearError() {
        trouble = false;
    }

    @Override
    public void flush() {
        try {
            out.flush();
        } catch (IOException e) {
            trouble = true;
        }
    }

    @Override
    public void close() {
        try {
            out.close();
        } catch (IOException e) {
            trouble = true;
        }
    }

    @Override
    public void write(int c) {
        try {
            out.write(c);
        } catch (IOException e) {
            trouble = true;
        }
    }

    @Override
    public void write(char[] buf, int off, int len) {
        try {
            out.write(buf, off, len);
        } catch (IOException e) {
            trouble = true;
        }
    }

    @Override
    public void write(char[] buf) {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(String s, int off, int len) {
        try {
            out.write(s, off, len);
        } catch (IOException e) {
            trouble = true;
        }
    }

    @Override
    public void write(String s) {
        write(s, 0, s.length());
    }

    public void print(String s) {
        write(s == null ? "null" : s);
    }

    public void print(Object o) {
        print(String.valueOf(o));
    }

    public void print(char c) {
        write(c);
    }

    public void print(char[] s) {
        write(s);
    }

    public void print(boolean b) {
        print(String.valueOf(b));
    }

    public void print(int i) {
        print(String.valueOf(i));
    }

    public void print(long l) {
        print(String.valueOf(l));
    }

    public void print(float f) {
        print(String.valueOf(f));
    }

    public void print(double d) {
        print(String.valueOf(d));
    }

    public void println() {
        write('\n');
        if (autoFlush) {
            flush();
        }
    }

    public void println(String s) {
        print(s);
        println();
    }

    public void println(Object o) {
        print(o);
        println();
    }

    public void println(char c) {
        print(c);
        println();
    }

    public void println(char[] s) {
        print(s);
        println();
    }

    public void println(boolean b) {
        print(b);
        println();
    }

    public void println(int i) {
        print(i);
        println();
    }

    public void println(long l) {
        print(l);
        println();
    }

    public void println(float f) {
        print(f);
        println();
    }

    public void println(double d) {
        print(d);
        println();
    }

    public PrintWriter append(CharSequence csq) {
        print(String.valueOf(csq));
        return this;
    }

    public PrintWriter append(char c) {
        print(c);
        return this;
    }
}
