/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * This code is licensed under GPLv2 with the Classpath exception.
 */
package java.util.zip;

/** Pure Java DEFLATE decompressor. */
public class Inflater {
    private com.codename1.impl.zip.Inflater engine;
    private final boolean nowrap;
    private boolean done, dictionary;
    public Inflater() {
        this(false);
    }
    public Inflater(boolean nowrap) {
        this.nowrap = nowrap;
        init();
    }
    private void init() {
        engine = new com.codename1.impl.zip.Inflater();
        engine.init(nowrap);
        engine.setInput(new byte[0], 0, 0, false);
        done = false;
        dictionary = false;
    }
    private void open() {
        if (engine == null)
            throw new NullPointerException("Inflater ended");
    }
    public void setInput(byte[] b) {
        setInput(b, 0, b.length);
    }
    public void setInput(byte[] b, int off, int len) {
        Deflater.bounds(b, off, len);
        open();
        engine.setInput(b, off, len, false);
    }
    public void setDictionary(byte[] b) {
        setDictionary(b, 0, b.length);
    }
    public void setDictionary(byte[] b, int off, int len) {
        Deflater.bounds(b, off, len);
        open();
        byte[] copy = new byte[len];
        System.arraycopy(b, off, copy, 0, len);
        if (engine.setDictionary(copy, len) != 0)
            throw new IllegalArgumentException("Invalid dictionary");
        dictionary = false;
    }
    public boolean needsInput() {
        return getRemaining() == 0;
    }
    public boolean needsDictionary() {
        return dictionary;
    }
    public boolean finished() {
        return done;
    }
    public int inflate(byte[] b) throws DataFormatException {
        return inflate(b, 0, b.length);
    }
    public int inflate(byte[] b, int off, int len) throws DataFormatException {
        Deflater.bounds(b, off, len);
        open();
        if (len == 0 || done)
            return 0;
        engine.setOutput(b, off, len);
        int rc = engine.inflate(0);
        if (rc == 1)
            done = true;
        else if (rc == 2)
            dictionary = true;
        else if (rc != 0 && rc != -5)
            throw new DataFormatException(engine.msg);
        return len - engine.availOut;
    }
    public int getRemaining() {
        open();
        return engine.availIn;
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
