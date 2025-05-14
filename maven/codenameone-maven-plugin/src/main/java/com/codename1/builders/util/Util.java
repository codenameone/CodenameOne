/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.builders.util;


import java.io.*;

/**
 *
 * @author shannah
 */
public class Util {
    
    /**
     * Copy the input stream into the output stream, closes both streams when finishing or in
     * a case of an exception
     * 
     * @param i source
     * @param o destination
     */
    public static void copy(InputStream i, OutputStream o) throws IOException {
        copy(i, o, 8192);
    }

    /**
     * Copy the input stream into the output stream, without closing the streams when done
     *
     * @param i source
     * @param o destination
     * @param bufferSize the size of the buffer, which should be a power of 2 large enough
     */
    public static void copyNoClose(InputStream i, OutputStream o, int bufferSize) throws IOException {
        copyNoClose(i, o, bufferSize, null);
    }
    
    /**
     * Copy the input stream into the output stream, without closing the streams when done
     *
     * @param i source
     * @param o destination
     * @param bufferSize the size of the buffer, which should be a power of 2 large enough
     * @param callback called after each copy step
     */
    public static void copyNoClose(InputStream i, OutputStream o, int bufferSize, IOProgressListener callback) throws IOException {

        byte[] buffer = new byte[bufferSize];
        int size = i.read(buffer);
        int total = 0;
        while(size > -1) {
            o.write(buffer, 0, size);
            if(callback != null) {
                callback.ioStreamUpdate(i, total += size);
            }
            size = i.read(buffer);
        }
    }
    
    /**
     * Copy the input stream into the output stream, closes both streams when finishing or in
     * a case of an exception
     *
     * @param i source
     * @param o destination
     * @param bufferSize the size of the buffer, which should be a power of 2 large enough
     */
    public static void copy(InputStream i, OutputStream o, int bufferSize) throws IOException {
        try {
            copyNoClose(i, o, bufferSize);
        } finally {
            cleanup(o);
            cleanup(i);
        }
    }

    /**
     * Closes the object (connection, stream etc.) without throwing any exception, even if the
     * object is null
     *
     * @param o Connection, Stream or other closeable object
     */
    public static void cleanup(Object o) {
        try {
            if(o != null) {
                if(o instanceof InputStream) {
                    ((InputStream) o).close();
                    return;
                }
                if(o instanceof OutputStream) {
                    ((OutputStream) o).close();
                    return;
                }
                if(o instanceof Reader) {
                    ((Reader) o).close();
                    return;
                }
                if(o instanceof Writer) {
                    ((Writer) o).close();
                    return;
                }
                if (o instanceof Closeable) {
                    ((Closeable)o).close();
                    return;
                }

            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Reads an input stream to a string
     * 
     * @param i the input stream
     * @return a UTF-8 string
     * @throws IOException thrown by the stream
     */
    public static String readToString(InputStream i) throws IOException {
        return readToString(i, "UTF-8");
    }

    /**
     * Reads an input stream to a string
     * 
     * @param i the input stream
     * @param encoding the encoding of the stream
     * @return a string
     * @throws IOException thrown by the stream
     */
    public static String readToString(InputStream i, String encoding) throws IOException {
        byte[] b = readInputStream(i);
        return new String(b, 0, b.length, encoding);
    }
    
    /**
     * Reads a reader to a string
     * 
     * @param i the input stream
     * @param encoding the encoding of the stream
     * @return a string
     * @throws IOException thrown by the stream
     * @since 7.0
     */
    public static String readToString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int len;
        while ((len = reader.read(buf)) != -1) {
            sb.append(buf, 0, len);
        }
        return sb.toString();
    }

    /**
     * Converts a small input stream to a byte array
     *
     * @param i the stream to convert
     * @return byte array of the content of the stream
     */
    public static byte[] readInputStream(InputStream i) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        copy(i, b);
        return b.toByteArray();
    }
}
