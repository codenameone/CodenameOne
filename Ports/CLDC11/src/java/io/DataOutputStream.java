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
 * A data output stream lets an application write primitive Java data types to an output stream in a portable way. An application can then use a data input stream to read the data back in.
 * Since: JDK1.0, CLDC 1.0 See Also:DataInputStream
 */
public class DataOutputStream extends java.io.OutputStream implements java.io.DataOutput{
    /**
     * The output stream.
     */
    protected java.io.OutputStream out;

    /**
     * Creates a new data output stream to write data to the specified underlying output stream.
     * out - the underlying output stream, to be saved for later use.
     */
    public DataOutputStream(java.io.OutputStream out){
         //TODO codavaj!!
    }

    /**
     * Closes this output stream and releases any system resources associated with the stream.
     * The close method calls its flush method, and then calls the close method of its underlying output stream.
     */
    public void close() throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Flushes this data output stream. This forces any buffered output bytes to be written out to the stream.
     * The flush method of DataOutputStream calls the flush method of its underlying output stream.
     */
    public void flush() throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off to the underlying output stream.
     */
    public void write(byte[] b, int off, int len) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Writes the specified byte (the low eight bits of the argument b) to the underlying output stream.
     * Implements the write method of OutputStream.
     */
    public void write(int b) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Writes a boolean to the underlying output stream as a 1-byte value. The value true is written out as the value (byte)1; the value false is written out as the value (byte)0.
     */
    public final void writeBoolean(boolean v) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Writes out a byte to the underlying output stream as a 1-byte value.
     */
    public final void writeByte(int v) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Writes a char to the underlying output stream as a 2-byte value, high byte first.
     */
    public final void writeChar(int v) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Writes a string to the underlying output stream as a sequence of characters. Each character is written to the data output stream as if by the writeChar method.
     */
    public final void writeChars(java.lang.String s) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Converts the double argument to a long using the doubleToLongBits method in class Double, and then writes that long value to the underlying output stream as an 8-byte quantity, high byte first.
     */
    public final void writeDouble(double v) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Converts the float argument to an int using the floatToIntBits method in class Float, and then writes that int value to the underlying output stream as a 4-byte quantity, high byte first.
     */
    public final void writeFloat(float v) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Writes an int to the underlying output stream as four bytes, high byte first.
     */
    public final void writeInt(int v) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Writes a long to the underlying output stream as eight bytes, high byte first.
     */
    public final void writeLong(long v) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Writes a short to the underlying output stream as two bytes, high byte first.
     */
    public final void writeShort(int v) throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Writes a string to the underlying output stream using UTF-8 encoding in a machine-independent manner.
     * First, two bytes are written to the output stream as if by the writeShort method giving the number of bytes to follow. This value is the number of bytes actually written out, not the length of the string. Following the length, each character of the string is output, in sequence, using the UTF-8 encoding for the character.
     */
    public final void writeUTF(java.lang.String str) throws java.io.IOException{
        return; //TODO codavaj!!
    }

}
