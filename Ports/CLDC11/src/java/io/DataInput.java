/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package java.io;
/**
 * The DataInput interface provides for reading bytes from a binary stream and reconstructing from them data in any of the Java primitive types. There is also a facility for reconstructing a String from data in Java modified UTF-8 format.
 * It is generally true of all the reading routines in this interface that if end of file is reached before the desired number of bytes has been read, an EOFException (which is a kind of IOException) is thrown. If any byte cannot be read for any reason other than end of file, an IOException other than EOFException is thrown. In particular, an IOException may be thrown if the input stream has been closed.
 * Since: JDK1.0, CLDC 1.0 See Also:DataInputStream, DataOutput
 */
public interface DataInput extends AutoCloseable {
    /**
     * Reads one input byte and returns true if that byte is nonzero, false if that byte is zero. This method is suitable for reading the byte written by the writeBoolean method of interface DataOutput.
     */
    public abstract boolean readBoolean() throws java.io.IOException;

    /**
     * Reads and returns one input byte. The byte is treated as a signed value in the range -128 through 127, inclusive. This method is suitable for reading the byte written by the writeByte method of interface DataOutput.
     */
    public abstract byte readByte() throws java.io.IOException;

    /**
     * Reads an input char and returns the char value. A Unicode char is made up of two bytes. Let a be the first byte read and b be the second byte. The value returned is:
     * (char)((a 8) | (b 0xff)) This method is suitable for reading bytes written by the writeChar method of interface DataOutput.
     */
    public abstract char readChar() throws java.io.IOException;

    /**
     * Reads eight input bytes and returns a double value. It does this by first constructing a long value in exactly the manner of the readlong method, then converting this long value to a double in exactly the manner of the method Double.longBitsToDouble. This method is suitable for reading bytes written by the writeDouble method of interface DataOutput.
     */
    public abstract double readDouble() throws java.io.IOException;

    /**
     * Reads four input bytes and returns a float value. It does this by first constructing an int value in exactly the manner of the readInt method, then converting this int value to a float in exactly the manner of the method Float.intBitsToFloat. This method is suitable for reading bytes written by the writeFloat method of interface DataOutput.
     */
    public abstract float readFloat() throws java.io.IOException;

    /**
     * Reads some bytes from an input stream and stores them into the buffer array b. The number of bytes read is equal to the length of b.
     * This method blocks until one of the following conditions occurs:
     * b.length bytes of input data are available, in which case a normal return is made. End of file is detected, in which case an EOFException is thrown. An I/O error occurs, in which case an IOException other than EOFException is thrown.
     * If b is null, a NullPointerException is thrown. If b.length is zero, then no bytes are read. Otherwise, the first byte read is stored into element b[0], the next one into b[1], and so on. If an exception is thrown from this method, then it may be that some but not all bytes of b have been updated with data from the input stream.
     */
    public abstract void readFully(byte[] b) throws java.io.IOException;

    /**
     * Reads len bytes from an input stream.
     * This method blocks until one of the following conditions occurs:
     * len bytes of input data are available, in which case a normal return is made. End of file is detected, in which case an EOFException is thrown. An I/O error occurs, in which case an IOException other than EOFException is thrown.
     * If b is null, a NullPointerException is thrown. If off is negative, or len is negative, or off+len is greater than the length of the array b, then an IndexOutOfBoundsException is thrown. If len is zero, then no bytes are read. Otherwise, the first byte read is stored into element b[off], the next one into b[off+1], and so on. The number of bytes read is, at most, equal to len.
     */
    public abstract void readFully(byte[] b, int off, int len) throws java.io.IOException;

    /**
     * Reads four input bytes and returns an int value. Let a be the first byte read, b be the second byte, c be the third byte, and d be the fourth byte. The value returned is:
     * (((a 0xff) 24) | ((b 0xff) 16) | ((c 0xff) 8) | (d 0xff)) This method is suitable for reading bytes written by the writeInt method of interface DataOutput.
     */
    public abstract int readInt() throws java.io.IOException;

    /**
     * Reads eight input bytes and returns a long value. Let a be the first byte read, b be the second byte, c be the third byte, d be the fourth byte, e be the fifth byte, f be the sixth byte, g be the seventh byte, and h be the eighth byte. The value returned is:
     * (((long)(a 0xff) 56) | ((long)(b 0xff) 48) | ((long)(c 0xff) 40) | ((long)(d 0xff) 32) | ((long)(e 0xff) 24) | ((long)(f 0xff) 16) | ((long)(g 0xff) 8) | ((long)(h 0xff)))
     * This method is suitable for reading bytes written by the writeLong method of interface DataOutput.
     */
    public abstract long readLong() throws java.io.IOException;

    /**
     * Reads two input bytes and returns a short value. Let a be the first byte read and b be the second byte. The value returned is:
     * (short)((a 8) | (b 0xff)) This method is suitable for reading the bytes written by the writeShort method of interface DataOutput.
     */
    public abstract short readShort() throws java.io.IOException;

    /**
     * Reads one input byte, zero-extends it to type int, and returns the result, which is therefore in the range 0 through 255. This method is suitable for reading the byte written by the writeByte method of interface DataOutput if the argument to writeByte was intended to be a value in the range 0 through 255.
     */
    public abstract int readUnsignedByte() throws java.io.IOException;

    /**
     * Reads two input bytes, zero-extends it to type int, and returns an int value in the range 0 through 65535. Let a be the first byte read and b be the second byte. The value returned is:
     * (((a 0xff) 8) | (b 0xff)) This method is suitable for reading the bytes written by the writeShort method of interface DataOutput if the argument to writeShort was intended to be a value in the range 0 through 65535.
     */
    public abstract int readUnsignedShort() throws java.io.IOException;

    /**
     * Reads in a string that has been encoded using a modified UTF-8 format. The general contract of readUTF is that it reads a representation of a Unicode character string encoded in Java modified UTF-8 format; this string of characters is then returned as a String.
     * First, two bytes are read and used to construct an unsigned 16-bit integer in exactly the manner of the readUnsignedShort method . This integer value is called the UTF length and specifies the number of additional bytes to be read. These bytes are then converted to characters by considering them in groups. The length of each group is computed from the value of the first byte of the group. The byte following a group, if any, is the first byte of the next group.
     * If the first byte of a group matches the bit pattern 0xxxxxxx (where x means "may be 0 or 1"), then the group consists of just that byte. The byte is zero-extended to form a character.
     * If the first byte of a group matches the bit pattern 110xxxxx, then the group consists of that byte a and a second byte b. If there is no byte b (because byte a was the last of the bytes to be read), or if byte b does not match the bit pattern 10xxxxxx, then a UTFDataFormatException is thrown. Otherwise, the group is converted to the character:
     * (char)(((a 0x1F) 6) | (b 0x3F)) If the first byte of a group matches the bit pattern 1110xxxx, then the group consists of that byte a and two more bytes b and c. If there is no byte c (because byte a was one of the last two of the bytes to be read), or either byte b or byte c does not match the bit pattern 10xxxxxx, then a UTFDataFormatException is thrown. Otherwise, the group is converted to the character:
     * (char)(((a 0x0F) 12) | ((b 0x3F) 6) | (c 0x3F)) If the first byte of a group matches the pattern 1111xxxx or the pattern 10xxxxxx, then a UTFDataFormatException is thrown.
     * If end of file is encountered at any time during this entire process, then an EOFException is thrown.
     * After every group has been converted to a character by this process, the characters are gathered, in the same order in which their corresponding groups were read from the input stream, to form a String, which is returned.
     * The writeUTF method of interface DataOutput may be used to write data that is suitable for reading by this method.
     */
    public abstract java.lang.String readUTF() throws java.io.IOException;

    /**
     * Makes an attempt to skip over n bytes of data from the input stream, discarding the skipped bytes. However, it may skip over some smaller number of bytes, possibly zero. This may result from any of a number of conditions; reaching end of file before n bytes have been skipped is only one possibility. This method never throws an EOFException. The actual number of bytes skipped is returned.
     */
    public abstract int skipBytes(int n) throws java.io.IOException;

}
