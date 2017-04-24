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
 * This class implements an output stream in which the data is written into a byte array. The buffer automatically grows as data is written to it. The data can be retrieved using toByteArray() and toString().
 * Since: JDK1.0, CLDC 1.0
 */
public class ByteArrayOutputStream extends java.io.OutputStream{
    /**
     * The buffer where data is stored.
     */
    protected byte[] buf;

    /**
     * The number of valid bytes in the buffer.
     */
    protected int count;

    /**
     * Creates a new byte array output stream. The buffer capacity is initially 32 bytes, though its size increases if necessary.
     */
    public ByteArrayOutputStream(){
         //TODO codavaj!!
    }

    /**
     * Creates a new byte array output stream, with a buffer capacity of the specified size, in bytes.
     * size - the initial size.
     * - if size is negative.
     */
    public ByteArrayOutputStream(int size){
         //TODO codavaj!!
    }

    /**
     * Closes this output stream and releases any system resources associated with this stream. A closed stream cannot perform output operations and cannot be reopened.
     */
    public void close() throws java.io.IOException{
        return; //TODO codavaj!!
    }

    /**
     * Resets the count field of this byte array output stream to zero, so that all currently accumulated output in the output stream is discarded. The output stream can be used again, reusing the already allocated buffer space.
     */
    public void reset(){
        return; //TODO codavaj!!
    }

    /**
     * Returns the current size of the buffer.
     */
    public int size(){
        return 0; //TODO codavaj!!
    }

    /**
     * Creates a newly allocated byte array. Its size is the current size of this output stream and the valid contents of the buffer have been copied into it.
     */
    public byte[] toByteArray(){
        return null; //TODO codavaj!!
    }

    /**
     * Converts the buffer's contents into a string, translating bytes into characters according to the platform's default character encoding.
     */
    public java.lang.String toString(){
        return null; //TODO codavaj!!
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
     */
    public void write(byte[] b, int off, int len){
        return; //TODO codavaj!!
    }

    /**
     * Writes the specified byte to this byte array output stream.
     */
    public void write(int b){
        return; //TODO codavaj!!
    }

}
