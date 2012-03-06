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

package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.Externalizable;
import com.codename1.ui.Display;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Various utility methods used for HTTP/IO operations
 *
 * @author Shai Almog
 */
public class Util {
    private static CodenameOneImplementation implInstance;
    private static Hashtable externalizables = new Hashtable();

    private static boolean charArrayBugTested;
    private static boolean charArrayBug;
    private static boolean xmlVMInstanceofBug;
    private static boolean xmlVMInstanceofBugTested;

    /**
     * Fix for RFE 427: http://java.net/jira/browse/LWUIT-427
     * Allows determining chars that should not be encoded
     */
    private static String ignoreCharsWhenEncoding = "";

    /**
     *  These chars will not be encoded by the encoding method in this class
     * as requested in RFE 427 http://java.net/jira/browse/LWUIT-427
     * @param s set of characters to skip when encoding
     */
    public static void setIgnorCharsWhileEncoding(String s) {
        ignoreCharsWhenEncoding = s;
    }

    /**
     *  These chars will not be encoded by the encoding method in this class
     * as requested in RFE 427 http://java.net/jira/browse/LWUIT-427
     * @return chars skipped
     */
    public static String getIgnorCharsWhileEncoding() {
        return ignoreCharsWhenEncoding;
    }


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
     * Copy the input stream into the output stream, closes both streams when finishing or in
     * a case of an exception
     *
     * @param i source
     * @param o destination
     * @param bufferSize the size of the buffer, which should be a power of 2 large enoguh
     */
    public static void copy(InputStream i, OutputStream o, int bufferSize) throws IOException {
        try {
            byte[] buffer = new byte[bufferSize];
            int size = i.read(buffer);
            while(size > -1) {
                o.write(buffer, 0, size);
                size = i.read(buffer);
            }
        } finally {
            Util.getImplementation().cleanup(o);
            Util.getImplementation().cleanup(i);
        }
    }

    /**
     * Closes the object (connection, stream etc.) without throwing any exception, even if the
     * object is null
     *
     * @param o Connection, Stream or other closeable object
     */
    public static void cleanup(Object o) {
        Util.getImplementation().cleanup(o);
    }

    /**
     * Reads an input stream to a string
     * 
     * @param i the input stream
     * @return a UTF-8 string
     * @throws IOException thrown by the stream
     */
    public static String readToString(InputStream i) throws IOException {
        byte[] b = readInputStream(i);
        return new String(b, 0, b.length, "UTF-8");
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

    /**
     * Registers this externalizable so readObject will be able to load such objects
     *
     * @param e the externalizable instance
     */
    public static void register(Externalizable e) {
        externalizables.put(e.getObjectId(), e.getClass());
    }

    /**
     * Registers this externalizable so readObject will be able to load such objects
     *
     * @param id id of the externalizable
     * @param c the class for the externalizable
     */
    public static void register(String id, Class c) {
        externalizables.put(id, c);
    }

    /**
     * Writes an object to the given output stream
     *
     * @param o the object to write which can be null
     * @param out the destination output stream
     * @throws IOException thrown by the stream
     */
    public static void writeObject(Object o, DataOutputStream out) throws IOException {
        if(o == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        if(o instanceof Externalizable) {
            Externalizable e = (Externalizable)o;
            out.writeUTF(e.getObjectId());
            out.writeInt(e.getVersion());
            e.externalize(out);
            return;
        }

        if(o instanceof Vector) {
            Vector v = (Vector)o;
            out.writeUTF(o.getClass().getName());
            int size = v.size();
            out.writeInt(size);
            for(int iter = 0 ; iter < size ; iter++) {
                writeObject(v.elementAt(iter), out);
            }
            return;
        }

        if(o instanceof Hashtable) {
            Hashtable v = (Hashtable)o;
            out.writeUTF(o.getClass().getName());
            out.writeInt(v.size());
            Enumeration k = v.keys();
            while(k.hasMoreElements()) {
                Object key = k.nextElement();
                writeObject(key, out);
                writeObject(v.get(key), out);
            }
            return;
        }

        if(o instanceof String) {
            String v = (String)o;
            out.writeUTF("String");
            out.writeUTF(v);
            return;
        }

        if(o instanceof Date) {
            Date v = (Date)o;
            out.writeUTF("Date");
            out.writeLong(v.getTime());
            return;
        }

        if(o instanceof Integer) {
            Integer v = (Integer)o;
            out.writeUTF("int");
            out.writeInt(v.intValue());
            return;
        }
        if(o instanceof Long) {
            Long v = (Long)o;
            out.writeUTF("long");
            out.writeLong(v.longValue());
            return;
        }

        if(o instanceof Byte) {
            Byte v = (Byte)o;
            out.writeUTF("byte");
            out.writeByte(v.byteValue());
            return;
        }

        if(o instanceof Short) {
            Short v = (Short)o;
            out.writeUTF("short");
            out.writeShort(v.shortValue());
            return;
        }

        if(o instanceof Float) {
            Float v = (Float)o;
            out.writeUTF("float");
            out.writeFloat(v.floatValue());
            return;
        }

        if(o instanceof Double) {
            Double v = (Double)o;
            out.writeUTF("double");
            out.writeDouble(v.doubleValue());
            return;
        }

        if(o instanceof Boolean) {
            Boolean v = (Boolean)o;
            out.writeUTF("bool");
            out.writeBoolean(v.booleanValue());
            return;
        }
        
        if(!xmlVMInstanceofBugTested) {
            xmlVMInstanceofBug = Display.getInstance().getPlatformName().equals("ios");
            xmlVMInstanceofBugTested = true;
        }

        if(instanceofObjArray(o)) {
            Object[] v = (Object[])o;
            out.writeUTF("ObjectArray");
            int size = v.length;
            out.writeInt(size);
            for(int iter = 0 ; iter < size ; iter++) {
                writeObject(v[iter], out);
            }
            return;
        }
        if(instanceofByteArray(o)) {
            byte[] v = (byte[])o;
            out.writeUTF("ByteArray");
            int size = v.length;
            out.writeInt(size);
            for(int iter = 0 ; iter < size ; iter++) {
                out.writeByte(v[iter]);
            }
            return;
        }
        if(instanceofShortArray(o)) {
            short[] v = (short[])o;
            out.writeUTF("ShortArray");
            int size = v.length;
            out.writeInt(size);
            for(int iter = 0 ; iter < size ; iter++) {
                out.writeShort(v[iter]);
            }
            return;
        }
        if(instanceofDoubleArray(o)) {
            double[] v = (double[])o;
            out.writeUTF("DoubleArray");
            int size = v.length;
            out.writeInt(size);
            for(int iter = 0 ; iter < size ; iter++) {
                out.writeDouble(v[iter]);
            }
            return;
        }
        if(instanceofFloatArray(o)) {
            float[] v = (float[])o;
            out.writeUTF("FloatArray");
            int size = v.length;
            out.writeInt(size);
            for(int iter = 0 ; iter < size ; iter++) {
                out.writeFloat(v[iter]);
            }
            return;
        }
        if(instanceofIntArray(o)) {
            int[] v = (int[])o;
            out.writeUTF("IntArray");
            int size = v.length;
            out.writeInt(size);
            for(int iter = 0 ; iter < size ; iter++) {
                out.writeInt(v[iter]);
            }
            return;
        }
        if(instanceofLongArray(o)) {
            long[] v = (long[])o;
            out.writeUTF("LongArray");
            int size = v.length;
            out.writeInt(size);
            for(int iter = 0 ; iter < size ; iter++) {
                out.writeLong(v[iter]);
            }
            return;
        }

        throw new IOException("Object type not supported: " + o.getClass().getName()
                + " value: " + o);
    }

    private static boolean instanceofObjArray(Object o) {
        if(xmlVMInstanceofBug) {
            try {
                o = (Object[])o;
                return true;
            } catch(ClassCastException e) {
                return false;
            }
        }
        return o instanceof Object[];
    }
    
    private static boolean instanceofByteArray(Object o) {
        if(xmlVMInstanceofBug) {
            try {
                o = (byte[])o;
                return true;
            } catch(ClassCastException e) {
                return false;
            }
        }
        return o instanceof byte[];
    }
    
    private static boolean instanceofShortArray(Object o) {
        if(xmlVMInstanceofBug) {
            try {
                o = (short[])o;
                return true;
            } catch(ClassCastException e) {
                return false;
            }
        }
        return o instanceof short[];
    }
    
    private static boolean instanceofLongArray(Object o) {
        if(xmlVMInstanceofBug) {
            try {
                o = (long[])o;
                return true;
            } catch(ClassCastException e) {
                return false;
            }
        }
        return o instanceof long[];
    }
    
    private static boolean instanceofIntArray(Object o) {
        if(xmlVMInstanceofBug) {
            try {
                o = (int[])o;
                return true;
            } catch(ClassCastException e) {
                return false;
            }
        }
        return o instanceof int[];
    }
    
    private static boolean instanceofFloatArray(Object o) {
        if(xmlVMInstanceofBug) {
            try {
                o = (float[])o;
                return true;
            } catch(ClassCastException e) {
                return false;
            }
        }
        return o instanceof float[];
    }
    
    private static boolean instanceofDoubleArray(Object o) {
        if(xmlVMInstanceofBug) {
            try {
                o = (double[])o;
                return true;
            } catch(ClassCastException e) {
                return false;
            }
        }
        return o instanceof double[];
    }

    /**
     * Reads an object from the stream
     *
     * @param input the source input stream
     * @throws IOException thrown by the stream
     */
    public static Object readObject(DataInputStream input) throws IOException {
        try {
            if (!input.readBoolean()) {
                return null;
            }
            String type = input.readUTF();
            if ("int".equals(type)) {
                return new Integer(input.readInt());
            }
            if ("byte".equals(type)) {
                return new Byte(input.readByte());
            }
            if ("short".equals(type)) {
                return new Short(input.readShort());
            }
            if ("long".equals(type)) {
                return new Long(input.readLong());
            }
            if ("float".equals(type)) {
                return new Float(input.readFloat());
            }
            if ("double".equals(type)) {
                return new Double(input.readDouble());
            }
            if ("bool".equals(type)) {
                return new Boolean(input.readBoolean());
            }
            if ("String".equals(type)) {
                return input.readUTF();
            }
            if ("Date".equals(type)) {
                return new Date(input.readLong());
            }

            if ("ObjectArray".equals(type)) {
                Object[] v = new Object[input.readInt()];
                for (int iter = 0; iter < v.length; iter++) {
                    v[iter] = readObject(input);
                }
                return v;
            }
            if ("ByteArray".equals(type)) {
                byte[] v = new byte[input.readInt()];
                for (int iter = 0; iter < v.length; iter++) {
                    v[iter] = input.readByte();
                }
                return v;
            }
            if ("LongArray".equals(type)) {
                long[] v = new long[input.readInt()];
                for (int iter = 0; iter < v.length; iter++) {
                    v[iter] = input.readLong();
                }
                return v;
            }
            if ("ShortArray".equals(type)) {
                short[] v = new short[input.readInt()];
                for (int iter = 0; iter < v.length; iter++) {
                    v[iter] = input.readShort();
                }
                return v;
            }
            if ("DoubleArray".equals(type)) {
                double[] v = new double[input.readInt()];
                for (int iter = 0; iter < v.length; iter++) {
                    v[iter] = input.readDouble();
                }
                return v;
            }
            if ("FloatArray".equals(type)) {
                float[] v = new float[input.readInt()];
                for (int iter = 0; iter < v.length; iter++) {
                    v[iter] = input.readFloat();
                }
                return v;
            }
            if ("IntArray".equals(type)) {
                int[] v = new int[input.readInt()];
                for (int iter = 0; iter < v.length; iter++) {
                    v[iter] = input.readInt();
                }
                return v;
            }
            Class cls = (Class) externalizables.get(type);
            if (cls != null) {
                Externalizable ex = (Externalizable) cls.newInstance();
                ex.internalize(input.readInt(), input);
                return ex;
            }
            Object o = null;
            o = Class.forName(type).newInstance();
            if (o instanceof Vector) {
                Vector v = (Vector) o;
                int size = input.readInt();
                for (int iter = 0; iter < size; iter++) {
                    v.addElement(readObject(input));
                }
                return v;
            }
            if (o instanceof Hashtable) {
                Hashtable v = (Hashtable) o;
                int size = input.readInt();
                for(int iter = 0 ; iter < size ; iter++) {
                    v.put(readObject(input), readObject(input));
                }
                return v;
            }
            throw new IOException("Object type not supported: " + o.getClass().getName());
        } catch (InstantiationException ex1) {
            ex1.printStackTrace();
            throw new IOException(ex1.getClass().getName() + ": " + ex1.getMessage());
        } catch (IllegalAccessException ex1) {
            ex1.printStackTrace();
            throw new IOException(ex1.getClass().getName() + ": " + ex1.getMessage());
        } catch (ClassNotFoundException ex1) {
            System.out.println("Verify that you invoked Util.register for your externalizable classes properly!");
            ex1.printStackTrace();
            throw new IOException(ex1.getClass().getName() + ": " + ex1.getMessage());
        }
    }

    /**
     * Encode a string for HTML requests
     *
     * @param str none encoded string
     * @return encoded string
     */
    public static String encodeUrl(final String str) {
        return encode(str, "%20");
    }

    private static char[] toCharArray(String s) {
        // toCharArray should return a new array always, however some devices might
        // suffer a bug that allows mutating a String (serious security hole in the JVM)
        // hence this method simulates the proper behavior
        if(!charArrayBugTested) {
            charArrayBugTested = true;
            if(s.toCharArray() == s.toCharArray()) {
                charArrayBug = true;
            }
        }
        if(charArrayBug) {
            char[] c = new char[s.length()];
            System.arraycopy(s.toCharArray(), 0, c, 0, c.length);
            return c;
        }
        return s.toCharArray();
    }

    private static String encode(String str, String spaceChar) {
        if (str == null) {
            return null;
        }
        return encode(toCharArray(str), spaceChar);
    }
    
    private static String encode(char[] buf, String spaceChar) {
        final StringBuffer sbuf = new StringBuffer(buf.length * 3);
        for (int i = 0; i < buf.length; i++) {
            final char ch = buf[i];
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') ||
                    (ch == '-' || ch == '_' || ch == '.' || ch == '~' || ch == '!'
          || ch == '*' || ch == '\'' || ch == '(' || ch == ')' || ignoreCharsWhenEncoding.indexOf(ch) > -1)) {
                sbuf.append(ch);
            } else if (ch == ' ') {
                sbuf.append(spaceChar);
            } else {
                appendHex(sbuf, ch);
            }
        }
        return sbuf.toString();
    }

    /**
     * Encode a string for HTML post requests matching the style used in application/x-www-form-urlencoded
     *
     * @param str none encoded string
     * @return encoded string
     */
    public static String encodeBody(final String str) {
        return encode(str, "+");
    }

    /**
     * Encode a string for HTML requests
     *
     * @param buf none encoded string
     * @return encoded string
     * @deprecated use encodeUrl(char[]) instead
     */
    public static String encodeUrl(final byte[] buf) {
        char[] b = new char[buf.length];
        for(int iter = 0 ; iter < buf.length ; iter++) {
            b[iter] = (char)buf[iter];
        }
        return encode(b, "%20");
    }

    /**
     * Encode a string for HTML requests
     *
     * @param buf none encoded string
     * @return encoded string
     */
    public static String encodeUrl(final char[] buf) {
        return encode(buf, "%20");
    }

    /**
     * Encode a string for HTML post requests matching the style used in application/x-www-form-urlencoded
     *
     * @param buf none encoded string
     * @return encoded string
     */
    public static String encodeBody(final char[] buf) {
        return encode(buf, "+");
    }

    /**
     * Encode a string for HTML post requests matching the style used in application/x-www-form-urlencoded
     *
     * @param buf none encoded string
     * @return encoded string
     * @deprecated use encodeUrl(char[]) instead
     */
    public static String encodeBody(final byte[] buf) {
        char[] b = new char[buf.length];
        for(int iter = 0 ; iter < buf.length ; iter++) {
            b[iter] = (char)buf[iter];
        }
        return encode(b, "+");
    }

    private static void appendHex(StringBuffer sbuf, char ch) {
        int firstLiteral = ch / 256;
        int secLiteral = ch % 256;
        if(firstLiteral == 0 && secLiteral < 127) {
            sbuf.append("%");
            sbuf.append(Integer.toHexString(secLiteral).toUpperCase());
            return;
        }
        if (ch <= 0x07ff) {
            // 2 literals unicode
            firstLiteral = 192 + (firstLiteral << 2) +(secLiteral >> 6);
            secLiteral=128+(secLiteral & 63);
            sbuf.append("%");
            sbuf.append(Integer.toHexString(firstLiteral).toUpperCase());
            sbuf.append("%");
            sbuf.append(Integer.toHexString(secLiteral).toUpperCase());
        } else {
            // 3 literals unicode
            int thirdLiteral = 128 + (secLiteral & 63);
            secLiteral = 128 + ((firstLiteral % 16) << 2) + (secLiteral >> 6);
            firstLiteral=224+(firstLiteral>>4);
            sbuf.append("%");
            sbuf.append(Integer.toHexString(firstLiteral).toUpperCase());
            sbuf.append("%");
            sbuf.append(Integer.toHexString(secLiteral).toUpperCase());
            sbuf.append("%");
            sbuf.append(Integer.toHexString(thirdLiteral).toUpperCase());
        }
    }

    /**
     * Converts a relative url e.g.: /myfile.html to an absolute url
     *
     * @param baseURL a source URL whose properties should be used to construct the actual URL
     * @param relativeURL relative address
     * @return an absolute URL
     */
    public static String relativeToAbsolute(String baseURL, String relativeURL) {
        if(relativeURL.startsWith("/")) {
            return getURLProtocol(baseURL) + "://" + getURLHost(baseURL) + relativeURL;
        } else {
            return getURLProtocol(baseURL) + "://" + getURLHost(baseURL) + getURLBasePath(baseURL) + relativeURL;
        }
    }

    /**
     * Returns the protocol of an absolute URL e.g. http, https etc.
     *
     * @param url absolute URL
     * @return protocol
     */
    public static String getURLProtocol(String url) {
        int index = url.indexOf("://");
        if (index != -1) {
            return url.substring(0, index);
        }
        return null;
    }

    /**
     * Returns the URL's host portion
     *
     * @param url absolute URL
     * @return the domain of the URL
     */
    public static String getURLHost(String url) {
        int start = url.indexOf("://");
        int end  = url.indexOf('/', start + 3);
        if (end != -1) {
            return url.substring(start + 3, end);
        } else {
            return url.substring(start + 3);
        }
    }

    /**
     * Returns the URL's path
     *
     * @param url absolute URL
     * @return the path within the host
     */
    public static String getURLPath(String url) {
        int start  = url.indexOf('/', url.indexOf("://") + 3);
        if (start != -1) {
            return url.substring(start + 1);
        } 
        return "/";
    }


    /**
     * Returns the URL's base path, which is the same as the path only without an ending file e.g.:
     * http://domain.com/f/f.html would return as: /f/
     *
     * @param url absolute URL
     * @return the path within the host
     */
    public static String getURLBasePath(String url) {
        int start  = url.indexOf('/', url.indexOf("://") + 3);
        int end  = url.lastIndexOf('/');
        if (start != -1 && end > start) {
            return url.substring(start, end + 1);
        }
        return "/";
    }

    /**
     * Writes a string with a null flag, this allows a String which may be null
     *
     * @param s the string to write
     * @param d the destination output stream
     * @throws java.io.IOException
     */
    public static void writeUTF(String s, DataOutputStream d) throws IOException {
        if(s == null) {
            d.writeBoolean(false);
            return;
        }
        d.writeBoolean(true);
        d.writeUTF(s);
    }

    /**
     * Reads a UTF string that may be null previously written by writeUTF
     *
     * @param d the stream
     * @return a string or null
     * @throws java.io.IOException
     */
    public static String readUTF(DataInputStream d) throws IOException {
        if(d.readBoolean()) {
            return d.readUTF();
        }
        return null;
    }
    
    /**
     * The read fully method from data input stream is very useful for all types of
     * streams...
     *
     * @param      b   the buffer into which the data is read.
     * @exception  IOException   the stream has been closed and the contained
     * 		   input stream does not support reading after close, or
     * 		   another I/O error occurs.
     */
    public static void readFully(InputStream i, byte b[]) throws IOException {
        readFully(i, b, 0, b.length);
    }

    /**
     * The read fully method from data input stream is very useful for all types of
     * streams...
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset of the data.
     * @param      len   the number of bytes to read.
     * @exception  IOException   the stream has been closed and the contained
     * 		   input stream does not support reading after close, or
     * 		   another I/O error occurs.
     */
    public static final void readFully(InputStream i, byte b[], int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
    	while (n < len) {
            int count = i.read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
    }

    /**
     * Reads until the array is full or until the stream ends
     *
     * @param      b     the buffer into which the data is read.
     * @return     the amount read
     * @exception  IOException   the stream has been closed and the contained
     * 		   input stream does not support reading after close, or
     * 		   another I/O error occurs.
     */
    public static int readAll(InputStream i, byte b[]) throws IOException {
        int len = b.length;
        int n = 0;
    	while (n < len) {
            int count = i.read(b, n, len - n);
            if (count < 0) {
                return n;
            }
            n += count;
        }
        return n;
    }

    /**
     * Provides a utility method breaks a given String to array of String according 
     * to the given separator
     * @param original the String to break
     * @param separator the pattern to look in the original String
     * @return array of Strings from the original String
     */
    public static String[] split(String original, String separator) {
        
        Vector nodes = new Vector();

        int index = original.indexOf(separator);
        while (index >= 0) {
            nodes.addElement(original.substring(0, index));
            original = original.substring(index + separator.length());
            index = original.indexOf(separator);
        }
        nodes.addElement(original);
        
        String [] ret = new String[nodes.size()];
         for (int i = 0; i < nodes.size(); i++) {
             ret[i] = (String) nodes.elementAt(i);             
         }
        return ret;
    }
     
    /**
     * Invoked internally from Display, this method is for internal use only
     * 
     * @param impl implementation instance
     */
    public static void setImplementation(CodenameOneImplementation impl) {
        implInstance = impl;
    }
    
    static CodenameOneImplementation getImplementation() { 
        return implInstance;
    }
}
