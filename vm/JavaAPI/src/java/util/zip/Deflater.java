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
package java.util.zip;

/** Pure Java DEFLATE compressor, with zlib framing unless nowrap is true. */
public class Deflater {
    public static final int DEFAULT_COMPRESSION = -1, NO_COMPRESSION = 0, BEST_SPEED = 1, BEST_COMPRESSION = 9;
    public static final int DEFAULT_STRATEGY = 0, FILTERED = 1, HUFFMAN_ONLY = 2, DEFLATED = 8;
    public static final int NO_FLUSH = 0, SYNC_FLUSH = 2, FULL_FLUSH = 3;
    private com.codename1.impl.zip.Deflater engine;
    private final boolean nowrap;
    private int level, strategy;
    private boolean finishing, done, paramsChanged;
    public Deflater() {
        this(DEFAULT_COMPRESSION);
    }
    public Deflater(int level) {
        this(level, false);
    }
    public Deflater(int level, boolean nowrap) {
        checkLevel(level);
        this.level = level;
        this.nowrap = nowrap;
        init();
    }
    private static void checkLevel(int level) {
        if (level < -1 || level > 9)
            throw new IllegalArgumentException("Invalid compression level");
    }
    private void init() {
        engine = new com.codename1.impl.zip.Deflater();
        if (engine.init(level, nowrap) != 0)
            throw new IllegalArgumentException("Cannot initialize deflater");
        if (strategy != 0)
            engine.params(level, strategy);
        finishing = false;
        done = false;
        paramsChanged = false;
    }
    private void open() {
        if (engine == null)
            throw new NullPointerException("Deflater ended");
    }
    static void bounds(byte[] b, int off, int len) {
        if (b == null)
            throw new NullPointerException();
        if (off < 0 || len < 0 || off > b.length - len)
            throw new ArrayIndexOutOfBoundsException();
    }
    public void setInput(byte[] b) {
        setInput(b, 0, b.length);
    }
    public void setInput(byte[] b, int off, int len) {
        bounds(b, off, len);
        open();
        engine.setInput(b, off, len, false);
    }
    public void setDictionary(byte[] b) {
        setDictionary(b, 0, b.length);
    }
    public void setDictionary(byte[] b, int off, int len) {
        bounds(b, off, len);
        open();
        byte[] copy = new byte[len];
        System.arraycopy(b, off, copy, 0, len);
        if (engine.setDictionary(copy, len) != 0)
            throw new IllegalArgumentException("Invalid dictionary state");
    }
    public void setLevel(int level) {
        checkLevel(level);
        this.level = level;
        paramsChanged = true;
    }
    public void setStrategy(int strategy) {
        if (strategy < 0 || strategy > 2)
            throw new IllegalArgumentException("Invalid strategy");
        this.strategy = strategy;
        paramsChanged = true;
    }
    public boolean needsInput() {
        open();
        return engine.availIn == 0;
    }
    public void finish() {
        finishing = true;
    }
    public boolean finished() {
        return done;
    }
    public int deflate(byte[] b) {
        return deflate(b, 0, b.length);
    }
    public int deflate(byte[] b, int off, int len) {
        return deflate(b, off, len, NO_FLUSH);
    }
    public int deflate(byte[] b, int off, int len, int flush) {
        bounds(b, off, len);
        open();
        if (flush != NO_FLUSH && flush != SYNC_FLUSH && flush != FULL_FLUSH)
            throw new IllegalArgumentException("Invalid flush");
        if (len == 0 || done)
            return 0;
        engine.setOutput(b, off, len);
        if (paramsChanged) {
            int rc = engine.params(level, strategy);
            if (rc == 0)
                paramsChanged = false;
            else if (rc != -5)
                throw new IllegalStateException(engine.msg);
            // deflateParams may itself produce output, including a partial flush.
            return len - engine.availOut;
        }
        int rc = engine.deflate(finishing ? 4 : flush);
        if (rc == 1)
            done = true;
        else if (rc != 0 && rc != -5)
            throw new IllegalStateException(engine.msg);
        return len - engine.availOut;
    }
    public int getAdler() {
        open();
        return (int) engine.getAdler();
    }
    public long getBytesRead() {
        open();
        return engine.totalIn;
    }
    public long getBytesWritten() {
        open();
        return engine.totalOut;
    }
    public int getTotalIn() {
        return (int) getBytesRead();
    }
    public int getTotalOut() {
        return (int) getBytesWritten();
    }
    public void reset() {
        open();
        engine.end();
        init();
    }
    public void end() {
        if (engine != null) {
            engine.end();
            engine = null;
        }
    }
}
