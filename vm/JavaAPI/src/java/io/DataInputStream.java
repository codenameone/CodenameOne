/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package java.io;
/**
 * A data input stream lets an application read primitive Java data types from an underlying input stream in a machine-independent way. An application uses a data output stream to write data that can later be read by a data input stream.
 * Since: JDK1.0, CLDC 1.0 See Also:DataOutputStream
 */
public class DataInputStream extends FilterInputStream implements java.io.DataInput{
    private final byte[] scratch = new byte[8];

    /**
     * Creates a DataInputStream and saves its argument, the input stream in, for later use.
     * in - the input stream.
     */
    public DataInputStream(java.io.InputStream in){
         super(in);
    }

    /**
     * See the general contract of the readBoolean method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final boolean readBoolean() throws java.io.IOException{
        int temp = in.read();
        if (temp < 0) {
            throw new EOFException();
        }
        return temp != 0;
    }

    /**
     * See the general contract of the readByte method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final byte readByte() throws java.io.IOException{
        int temp = in.read();
        if (temp < 0) {
            throw new EOFException();
        }
        return (byte) temp;
    }

    /**
     * See the general contract of the readChar method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final char readChar() throws java.io.IOException{
        return (char) readShort();
    }

    /**
     * See the general contract of the readDouble method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final double readDouble() throws java.io.IOException{
        return Double.longBitsToDouble(readLong());
    }

    /**
     * See the general contract of the readFloat method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final float readFloat() throws java.io.IOException{
        return Float.intBitsToFloat(readInt());
    }

    /**
     * See the general contract of the readFully method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final void readFully(byte[] b) throws java.io.IOException{
        readFully(b, 0, b.length);
    }

    /**
     * See the general contract of the readFully method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final void readFully(byte[] b, int off, int len) throws java.io.IOException{
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
    	while (n < len) {
            int count = read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
    }

    /**
     * See the general contract of the readInt method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final int readInt() throws java.io.IOException{
        readFully(scratch, 0, 4);
        return (((scratch[0] & 0xff) << 24) |
                    ((scratch[1] & 0xff) << 16) |
                    ((scratch[2] & 0xff) <<  8) |
                    ((scratch[3] & 0xff) <<  0));
    }

    /**
     * See the general contract of the readLong method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final long readLong() throws java.io.IOException{
        readFully(scratch, 0, 8);
        int h = ((scratch[0] & 0xff) << 24) |
                ((scratch[1] & 0xff) << 16) |
                ((scratch[2] & 0xff) <<  8) |
                ((scratch[3] & 0xff) <<  0);
        int l = ((scratch[4] & 0xff) << 24) |
                ((scratch[5] & 0xff) << 16) |
                ((scratch[6] & 0xff) <<  8) |
                ((scratch[7] & 0xff) <<  0);
        return (((long) h) << 32L) | ((long) l) & 0xffffffffL;
    }

    /**
     * See the general contract of the readShort method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final short readShort() throws java.io.IOException{
        readFully(scratch, 0, 2);
        return (short) ((scratch[0] << 8) | (scratch[1] & 0xff));
    }

    /**
     * See the general contract of the readUnsignedByte method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final int readUnsignedByte() throws java.io.IOException{
        int temp = in.read();
        if (temp < 0) {
            throw new EOFException();
        }
        return temp;
    }

    /**
     * See the general contract of the readUnsignedShort method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final int readUnsignedShort() throws java.io.IOException{
        return ((int) readShort()) & 0xffff;
    }

    /**
     * See the general contract of the readUTF method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final String readUTF() throws IOException {
        return decodeUTF(readUnsignedShort());
    }

    String decodeUTF(int utfSize) throws IOException {
        return decodeUTF(utfSize, this);
    }

    private static String decodeUTF(int utfSize, DataInput in) throws IOException {
        byte[] buf = new byte[utfSize];
        in.readFully(buf, 0, utfSize);
        return decode(buf, new char[utfSize], 0, utfSize);
    }

    /**
     * Decodes a byte array containing <i>modified UTF-8</i> bytes into a string.
     *
     * <p>Note that although this method decodes the (supposedly impossible) zero byte to U+0000,
     * that's what the RI does too.
     */
    private static String decode(byte[] in, char[] out, int offset, int utfSize) throws UTFDataFormatException {
        int count = 0, s = 0, a;
        while (count < utfSize) {
            if ((out[s] = (char) in[offset + count++]) < '\u0080') {
                s++;
            } else if (((a = out[s]) & 0xe0) == 0xc0) {
                if (count >= utfSize) {
                    throw new RuntimeException("bad second byte at " + count);
                }
                int b = in[offset + count++];
                if ((b & 0xC0) != 0x80) {
                    throw new RuntimeException("bad second byte at " + (count - 1));
                }
                out[s++] = (char) (((a & 0x1F) << 6) | (b & 0x3F));
            } else if ((a & 0xf0) == 0xe0) {
                if (count + 1 >= utfSize) {
                    throw new RuntimeException("bad third byte at " + (count + 1));
                }
                int b = in[offset + count++];
                int c = in[offset + count++];
                if (((b & 0xC0) != 0x80) || ((c & 0xC0) != 0x80)) {
                    throw new RuntimeException("bad second or third byte at " + (count - 2));
                }
                out[s++] = (char) (((a & 0x0F) << 12) | ((b & 0x3F) << 6) | (c & 0x3F));
            } else {
                throw new RuntimeException("bad byte at " + (count - 1));
            }
        }
        return new String(out, 0, s);
    }
    
    /**
     * Reads from the stream in a representation of a Unicode character string encoded in Java modified UTF-8 format; this string of characters is then returned as a String. The details of the modified UTF-8 representation are exactly the same as for the readUTF method of DataInput.
     */
    public static final java.lang.String readUTF(java.io.DataInput in) throws java.io.IOException{
        return decodeUTF(in.readUnsignedShort(), in);
    }

    /**
     * See the general contract of the skipBytes method of DataInput.
     * Bytes for this operation are read from the contained input stream.
     */
    public final int skipBytes(int count) throws java.io.IOException{
        int skipped = 0;
        long skip;
        while (skipped < count && (skip = in.skip(count - skipped)) != 0) {
            skipped += skip;
        }
        return skipped;
    }

    public final String readLine() throws IOException {
        StringBuilder line = new StringBuilder(80); // Typical line length
        boolean foundTerminator = false;
        while (true) {
            int nextByte = in.read();
            switch (nextByte) {
                case -1:
                    if (line.length() == 0 && !foundTerminator) {
                        return null;
                    }
                    return line.toString();
                case (byte) '\r':
                    if (foundTerminator) {
                        ((PushbackInputStream) in).unread(nextByte);
                        return line.toString();
                    }
                    foundTerminator = true;
                    /* Have to be able to peek ahead one byte */
                    if (!(in.getClass() == PushbackInputStream.class)) {
                        in = new PushbackInputStream(in);
                    }
                    break;
                case (byte) '\n':
                    return line.toString();
                default:
                    if (foundTerminator) {
                        ((PushbackInputStream) in).unread(nextByte);
                        return line.toString();
                    }
                    line.append((char) nextByte);
            }
        }
    }
}
