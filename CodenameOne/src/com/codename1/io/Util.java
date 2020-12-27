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

import com.codename1.components.InfiniteProgress;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.Externalizable;
import com.codename1.io.IOProgressListener;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Storage;
import com.codename1.io.gzip.GZConnectionRequest;
import com.codename1.l10n.DateFormat;
import com.codename1.l10n.L10NManager;
import com.codename1.l10n.ParseException;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.properties.PropertyBusinessObject;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;
import com.codename1.util.CallbackAdapter;
import com.codename1.util.EasyThread;
import com.codename1.util.FailureCallback;
import com.codename1.util.OnComplete;
import com.codename1.util.SuccessCallback;
import com.codename1.util.Wrapper;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
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
    private static final Random downloadUrlSafelyRandom = new Random(System.currentTimeMillis()); 

    static {
        register("EncodedImage", EncodedImage.class);
    }
    
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

    /**
     * <p>Registers this externalizable so readObject will be able to load such objects.</p>
     * <p>
     * The sample below demonstrates the usage and registration of the {@link com.codename1.io.Externalizable} interface:
     * </p>
     * <script src="https://gist.github.com/codenameone/858d8634e3cf1a82a1eb.js"></script>
     *
     *
     * @param e the externalizable instance
     */
    public static void register(Externalizable e) {
        externalizables.put(e.getObjectId(), e.getClass());
    }

    /**
     * <p>Registers this externalizable so readObject will be able to load such objects.</p>
     *
     * <p>
     * The sample below demonstrates the usage and registration of the {@link com.codename1.io.Externalizable} interface:
     * </p>
     * <script src="https://gist.github.com/codenameone/858d8634e3cf1a82a1eb.js"></script>
     *
     * @param id id of the externalizable
     * @param c the class for the externalizable
     */
    public static void register(String id, Class c) {
        externalizables.put(id, c);
    }

    /**
     * <p>Writes an object to the given output stream, notice that it should be externalizable or one of
     * the supported types.</p>
     * 
     * <p>
     * The sample below demonstrates the usage and registration of the {@link com.codename1.io.Externalizable} interface:
     * </p>
     * <script src="https://gist.github.com/codenameone/858d8634e3cf1a82a1eb.js"></script>
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
        if(o instanceof PropertyBusinessObject) {
            Externalizable e = ((PropertyBusinessObject)o).getPropertyIndex().asExternalizable();
            out.writeUTF(e.getObjectId());
            out.writeInt(e.getVersion());
            e.externalize(out);
            return;
        }

        if(o instanceof Vector) {
            Vector v = (Vector)o;
            out.writeUTF("java.util.Vector");
            int size = v.size();
            out.writeInt(size);
            for(int iter = 0 ; iter < size ; iter++) {
                writeObject(v.elementAt(iter), out);
            }
            return;
        }

        if(o instanceof Collection) {
            Collection v = (Collection)o;
            out.writeUTF("java.util.Collection");
            int size = v.size();
            out.writeInt(size);
            for(Object cur : v) {
                writeObject(cur, out);
            }
            return;
        }

        if(o instanceof Hashtable) {
            Hashtable v = (Hashtable)o;
            out.writeUTF("java.util.Hashtable");
            out.writeInt(v.size());
            Enumeration k = v.keys();
            while(k.hasMoreElements()) {
                Object key = k.nextElement();
                writeObject(key, out);
                writeObject(v.get(key), out);
            }
            return;
        }
        if(o instanceof Map) {
            Map v = (Map)o;
            out.writeUTF("java.util.Map");
            out.writeInt(v.size());
            for(Object key : v.keySet()) {
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
        
        if (o instanceof EncodedImage) {
            out.writeUTF("EncodedImage");
            EncodedImage e = (EncodedImage)o;
            out.writeInt(e.getWidth());
            out.writeInt(e.getHeight());
            out.writeBoolean(e.isOpaque());
            byte[] b = e.getImageData();
            out.writeInt(b.length);
            out.write(b);
            return;
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
            out.write(v);
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

    /**
     * This method allows working around <a href="http://code.google.com/p/codenameone/issues/detail?id=58">issue 58</a>
     * 
     * @param o object to test
     * @return true if it matches the state
     * @deprecated this method serves as a temporary workaround for an XMLVM bug and will be removed 
     * once the bug is fixed
     */
    public static boolean instanceofObjArray(Object o) {
        return getImplementation().instanceofObjArray(o);
    }
    
    /**
     * This method allows working around <a href="http://code.google.com/p/codenameone/issues/detail?id=58">issue 58</a>
     * 
     * @param o object to test
     * @return true if it matches the state
     * @deprecated this method serves as a temporary workaround for an XMLVM bug and will be removed 
     * once the bug is fixed
     */
    public static boolean instanceofByteArray(Object o) {
        return getImplementation().instanceofByteArray(o);
    }
    
    /**
     * This method allows working around <a href="http://code.google.com/p/codenameone/issues/detail?id=58">issue 58</a>
     * 
     * @param o object to test
     * @return true if it matches the state
     * @deprecated this method serves as a temporary workaround for an XMLVM bug and will be removed 
     * once the bug is fixed
     */
    public static boolean instanceofShortArray(Object o) {
        return getImplementation().instanceofShortArray(o);
    }
    
    /**
     * This method allows working around <a href="http://code.google.com/p/codenameone/issues/detail?id=58">issue 58</a>
     * 
     * @param o object to test
     * @return true if it matches the state
     * @deprecated this method serves as a temporary workaround for an XMLVM bug and will be removed 
     * once the bug is fixed
     */
    public static boolean instanceofLongArray(Object o) {
        return getImplementation().instanceofLongArray(o);
    }
    
    /**
     * This method allows working around <a href="http://code.google.com/p/codenameone/issues/detail?id=58">issue 58</a>
     * 
     * @param o object to test
     * @return true if it matches the state
     * @deprecated this method serves as a temporary workaround for an XMLVM bug and will be removed 
     * once the bug is fixed
     */
    public static boolean instanceofIntArray(Object o) {
        return getImplementation().instanceofIntArray(o);
    }
    
    /**
     * This method allows working around <a href="http://code.google.com/p/codenameone/issues/detail?id=58">issue 58</a>
     * 
     * @param o object to test
     * @return true if it matches the state
     * @deprecated this method serves as a temporary workaround for an XMLVM bug and will be removed 
     * once the bug is fixed
     */
    public static boolean instanceofFloatArray(Object o) {
        return getImplementation().instanceofFloatArray(o);
    }
    
    /**
     * This method allows working around <a href="http://code.google.com/p/codenameone/issues/detail?id=58">issue 58</a>
     * 
     * @param o object to test
     * @return true if it matches the state
     * @deprecated this method serves as a temporary workaround for an XMLVM bug and will be removed 
     * once the bug is fixed
     */
    public static boolean instanceofDoubleArray(Object o) {
        return getImplementation().instanceofDoubleArray(o);
    }

    /**
     * <p>Reads an object from the stream, notice that this is the inverse of the 
     * {@link #writeObject(java.lang.Object, java.io.DataOutputStream)}.</p>
     *
     * <p>
     * The sample below demonstrates the usage and registration of the {@link com.codename1.io.Externalizable} interface:
     * </p>
     * <script src="https://gist.github.com/codenameone/858d8634e3cf1a82a1eb.js"></script>
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
                int vlen = v.length;
                for (int iter = 0; iter < vlen; iter++) {
                    v[iter] = readObject(input);
                }
                return v;
            }
            if ("ByteArray".equals(type)) {
                byte[] v = new byte[input.readInt()];
                input.readFully(v);
                return v;
            }
            if ("LongArray".equals(type)) {
                long[] v = new long[input.readInt()];
                int vlen = v.length;
                for (int iter = 0; iter < vlen; iter++) {
                    v[iter] = input.readLong();
                }
                return v;
            }
            if ("ShortArray".equals(type)) {
                short[] v = new short[input.readInt()];
                int vlen = v.length;
                for (int iter = 0; iter < vlen; iter++) {
                    v[iter] = input.readShort();
                }
                return v;
            }
            if ("DoubleArray".equals(type)) {
                double[] v = new double[input.readInt()];
                int vlen = v.length;
                for (int iter = 0; iter < vlen; iter++) {
                    v[iter] = input.readDouble();
                }
                return v;
            }
            if ("FloatArray".equals(type)) {
                float[] v = new float[input.readInt()];
                int vlen = v.length;
                for (int iter = 0; iter < vlen; iter++) {
                    v[iter] = input.readFloat();
                }
                return v;
            }
            if ("IntArray".equals(type)) {
                int[] v = new int[input.readInt()];
                int vlen = v.length;
                for (int iter = 0; iter < vlen; iter++) {
                    v[iter] = input.readInt();
                }
                return v;
            }
            if ("java.util.Vector".equals(type)) {
                Vector v = new Vector();
                int size = input.readInt();
                for (int iter = 0; iter < size; iter++) {
                    v.addElement(readObject(input));
                }
                return v;
            }
            if ("java.util.Hashtable".equals(type)) {
                Hashtable v = new Hashtable();
                int size = input.readInt();
                for(int iter = 0 ; iter < size ; iter++) {
                    v.put(readObject(input), readObject(input));
                }
                return v;
            }
            if ("java.util.Collection".equals(type)) {
                Collection v = new ArrayList();
                int size = input.readInt();
                for (int iter = 0; iter < size; iter++) {
                    v.add(readObject(input));
                }
                return v;
            }
            if ("java.util.Map".equals(type)) {
                Map v = new HashMap();
                int size = input.readInt();
                for(int iter = 0 ; iter < size ; iter++) {
                    v.put(readObject(input), readObject(input));
                }
                return v;
            }
            if ("EncodedImage".equals(type)) {
                int width = input.readInt();
                int height = input.readInt();
                boolean op = input.readBoolean();
                byte[] data = new byte[input.readInt()];
                input.readFully(data);
                return EncodedImage.create(data, width, height, op);
            }
            Class cls = (Class) externalizables.get(type);
            if (cls != null) {
                Object o = cls.newInstance();
                if(o instanceof Externalizable) {
                    Externalizable ex = (Externalizable)o; 
                    ex.internalize(input.readInt(), input);
                    return ex;
                } else {
                    PropertyBusinessObject pb = (PropertyBusinessObject)o;
                    pb.getPropertyIndex().asExternalizable().internalize(input.readInt(), input);
                    return pb;
                }
            }
            throw new IOException("Object type not supported: " + type);
        } catch (InstantiationException ex1) {
            Log.e(ex1);
            throw new IOException(ex1.getClass().getName() + ": " + ex1.getMessage());
        } catch (IllegalAccessException ex1) {
            Log.e(ex1);
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
    
    /**
     * Encodes the provided string as a URL (with %20 for spaces).
     * @param str The URL to encode
     * @param doNotEncodeChars A string whose characters will not be encoded.
     * @return 
     */
    public static String encodeUrl(final String str, String doNotEncodeChars) {
        return encode(str.toCharArray(), "%20", doNotEncodeChars);
    }

    /**
     * toCharArray should return a new array always, however some devices might
     * suffer a bug that allows mutating a String (serious security hole in the JVM)
     * hence this method simulates the proper behavior
     * @param s a string
     * @return the contents of the string as a char array guaranteed to be a copy of the current array
     */
    public static char[] toCharArray(String s) {
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
    
    /**
     * Decodes a String URL encoded URL
     * 
     * @param s the string
     * @param enc the encoding (defaults to UTF-8 if null)
     * @param plusToSpace true if plus signs be converted to spaces
     * @return a decoded string
     */
    public static String decode(String s, String enc, boolean plusToSpace) {
        boolean modified = false;
        if(enc == null || enc.length() == 0) {
            enc = "UTF-8";
        }
        int numChars = s.length();
        StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;

        char c;
        byte[] bytes = null;
        while (i < numChars) {
            c = s.charAt(i);
            switch (c) {
                case '+':
                    if(plusToSpace) {
                        sb.append(' ');
                    } else {
                        sb.append('+');
                    }
                    i++;
                    modified = true;
                    break;

                case '%':
                    try {
                        if (bytes == null) {
                            bytes = new byte[(numChars - i) / 3];
                        }
                        
                        int pos = 0;

                        while (((i + 2) < numChars) && (c == '%')) {
                            bytes[pos++] = (byte) Integer.parseInt(s.substring(i + 1, i + 3), 16);
                            i += 3;
                            if (i < numChars) {
                                c = s.charAt(i);
                            }
                        }

                        if ((i < numChars) && (c == '%')) {
                            throw new IllegalArgumentException("Illegal URL % character: " + s);
                        }

                        try {
                            sb.append(new String(bytes, 0, pos, enc));
                        }
                        catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e.toString());
                        }
                    }
                    catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Illegal URL encoding: " + s);
                    }
                    modified = true;
                    break;

                default:
                    sb.append(c);
                    i++;
                    break;
            }
        }

        if(modified) {
            return sb.toString();
        }
        return s;
    }
   
    private static String encode(char[] buf, String spaceChar) {
        return encode(buf, spaceChar, null);
    }
    
    private static String encode(char[] buf, String spaceChar, String doNotEncode) {
        final StringBuilder sbuf = new StringBuilder(buf.length * 3);
        int blen = buf.length;
        for (int i = 0; i < blen; i++) {
            final char ch = buf[i];
            
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') ||
                    (ch == '-' || ch == '_' || ch == '.' || ch == '~' || ch == '!'
          || ch == '*' || ch == '\'' || ch == '(' || ch == ')' || ignoreCharsWhenEncoding.indexOf(ch) > -1) || (doNotEncode != null && doNotEncode.indexOf(ch) > -1)) {
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
        int blen = buf.length;
        for(int iter = 0 ; iter < blen ; iter++) {
            b[iter] = (char)buf[iter];
        }
        return encode(b, "+");
    }

    private static void appendHex(StringBuilder sbuf, char ch) {
        int firstLiteral = ch / 256;
        int secLiteral = ch % 256;
        if(firstLiteral == 0 && secLiteral < 127) {
            sbuf.append("%");
            String s = Integer.toHexString(secLiteral).toUpperCase();
            if(s.length() == 1) {
                sbuf.append("0");
            } 
            sbuf.append(s);
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
    
    /**
     * Merges arrays into one larger array
     */
    public static void mergeArrays(Object[] arr1, Object[] arr2, Object[] destinationArray) {
        System.arraycopy(arr1, 0, destinationArray, 0, arr1.length);
        System.arraycopy(arr2, 0, destinationArray, arr1.length, arr2.length);
    }
    
    /**
     * Removes the object at the source array offset and copies all other objects to the destination array
     * @param sourceArray the source array
     * @param destinationArray the resulting array which should be of the length sourceArray.length - 1
     * @param o the object to remove from the array
     */
    public static void removeObjectAtOffset(Object[] sourceArray, Object[] destinationArray, Object o) {
        int off = indexOf(sourceArray, o);
        removeObjectAtOffset(sourceArray, destinationArray, off);
    }
    
    /**
     * Removes the object at the source array offset and copies all other objects to the destination array
     * @param sourceArray the source array
     * @param destinationArray the resulting array which should be of the length sourceArray.length - 1
     * @param offset the offset of the array
     */
    public static void removeObjectAtOffset(Object[] sourceArray, Object[] destinationArray, int offset) {
        System.arraycopy(sourceArray, 0, destinationArray, 0, offset);
        System.arraycopy(sourceArray, offset + 1, destinationArray, offset, sourceArray.length - offset - 1);
    }
    
    /**
     * Inserts the object at the destination array offset 
     * @param sourceArray the source array
     * @param destinationArray the resulting array which should be of the length sourceArray.length + 1
     * @param offset the offset of the array
     * @param o the object
     */
    public static void insertObjectAtOffset(Object[] sourceArray, Object[] destinationArray, int offset, Object o) {
        if(offset == 0) {
            destinationArray[0] = o;
            System.arraycopy(sourceArray, 0, destinationArray, 1, sourceArray.length);
        } else {
            if(offset == sourceArray.length) {
                System.arraycopy(sourceArray, 0, destinationArray, 0, sourceArray.length);
                destinationArray[sourceArray.length] = o;
            } else {
                System.arraycopy(sourceArray, 0, destinationArray, 0, offset);
                destinationArray[offset] = o;
                System.arraycopy(sourceArray, offset, destinationArray, offset + 1, sourceArray.length - offset);
            }
        }
    }

    /**
     * Finds the object at the given offset while using the == operator and not the equals method call, it doesn't
     * rely on the ordering of the elements like the Arrays method.
     * @param arr the array
     * @param value the value to search
     * @return the offset or -1
     */
    public static int indexOf(Object[] arr, Object value) {
        int l = arr.length;
        for(int iter = 0 ; iter < l ; iter++) {
            if(arr[iter] == value) {
                return iter;
            }
        }
        return -1;
    }
    
    /**
     * Blocking method that will download the given URL to storage and return when the 
     * operation completes
     * @param url the URL
     * @param fileName the storage file name
     * @param showProgress whether to block the UI until download completes/fails
     * @return true on success false on error
     */
    public static boolean downloadUrlToStorage(String url, String fileName, boolean showProgress) {
        return downloadUrlTo(url, fileName, showProgress, false, true, null);
    }

    /**
     * Blocking method that will download the given URL to the file system storage and return when the 
     * operation completes
     * @param url the URL
     * @param fileName the file name
     * @param showProgress whether to block the UI until download completes/fails
     * @return true on success false on error
     */
    public static boolean downloadUrlToFile(String url, String fileName, boolean showProgress) {
        return downloadUrlTo(url, fileName, showProgress, false, false, null);
    }

    /**
     * <p>Non-blocking method that will download the given URL to storage in the background and return 
     * immediately. This method can be used to fetch data dynamically and asynchronously e.g. in this code it is used
     * to fetch book covers for the {@link com.codename1.components.ImageViewer}:</p>
     * 
     * <script src="https://gist.github.com/codenameone/305c3f5426b0e2e80833.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/components-imageviewer-dynamic.png" alt="Image viewer with dynamic URL fetching model" />
     * @param url the URL
     * @param fileName the storage file name
     */
    public static void downloadUrlToStorageInBackground(String url, String fileName) {
        downloadUrlTo(url, fileName, false, true, true, null);
    }

    /**
     * Non-blocking method that will download the given URL to file system storage in the background and return immediately
     * @param url the URL
     * @param fileName the file name
     */
    public static void downloadUrlToFileSystemInBackground(String url, String fileName) {
        downloadUrlTo(url, fileName, false, true, false, null);
    }

    /**
     * Non-blocking method that will download the given URL to storage in the background and return immediately
     * @param url the URL
     * @param fileName the storage file name
     * @param onCompletion invoked when download completes
     */
    public static void downloadUrlToStorageInBackground(String url, String fileName, ActionListener onCompletion) {
        downloadUrlTo(url, fileName, false, true, true, onCompletion);
    }
    
    /**
     * Non-blocking method that will download the given URL to file system storage in the background and return immediately
     * @param url the URL
     * @param fileName the file name
     * @param onCompletion invoked when download completes
     */
    public static void downloadUrlToFileSystemInBackground(String url, String fileName, ActionListener onCompletion) {
        downloadUrlTo(url, fileName, false, true, false, onCompletion);
    }
    
    /**
     * Downloads an image to the file system asynchronously.  If the image is already downloaded it will just load it directly from 
     * the file system.
     * @param url The URL to download the image from.
     * @param fileName The the path to the file where the image should be downloaded.  If this file already exists, it will simply load this file and skip the 
     * network request altogether.
     * @param onSuccess Callback called on success.
     * @param onFail Callback called if we fail to load the image.
     * @since 3.4
     * @see ConnectionRequest#downloadImageToFileSystem(java.lang.String, com.codename1.util.SuccessCallback, com.codename1.util.FailureCallback) 
     */
    public static void downloadImageToFileSystem(String url, String fileName, SuccessCallback<Image> onSuccess, FailureCallback<Image> onFail) {
        implInstance.downloadImageToFileSystem(url, fileName, onSuccess, onFail);
    }
    
    
    /**
     * Downloads an image to the file system asynchronously.  If the image is already downloaded it will just load it directly from 
     * the file system.
     * @param url The URL to download the image from.
     * @param fileName The the path to the file where the image should be downloaded.  If this file already exists, it will simply load this file and skip the 
     * network request altogether.
     * @since 7.0
     * @see ConnectionRequest#downloadImageToFileSystem(java.lang.String, com.codename1.util.SuccessCallback, com.codename1.util.FailureCallback) 
     */
    public static AsyncResource<Image> downloadImageToFileSystem(String url, String fileName) {
        final AsyncResource<Image> out = new AsyncResource<Image>();
        downloadImageToFileSystem(url, fileName, new SuccessCallback<Image>() {
            @Override
            public void onSucess(Image value) {
                out.complete(value);
            }

        },
                new FailureCallback<Image>() {
            @Override
            public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                out.error(err);
            }
        }
        );
        return out;
    }
    
    /**
     * Downloads an image to the file system asynchronously.  If the image is already downloaded it will just load it directly from 
     * the file system.
     * @param url The URL to download the image from.
     * @param fileName The the path to the file where the image should be downloaded.  If this file already exists, it will simply load this file and skip the 
     * network request altogether.
     * @param onSuccess Callback called on success.
     * @since 3.4
     * @see ConnectionRequest#downloadImageToFileSystem(java.lang.String, com.codename1.util.SuccessCallback) 
     */
    public static void downloadImageToFileSystem(String url, String fileName, SuccessCallback<Image> onSuccess) {
        downloadImageToFileSystem(url, fileName, onSuccess, new CallbackAdapter<Image>());
    }
    
    /**
     * Downloads an image to storage asynchronously.  If the image is already downloaded it will just load it directly from 
     * storage.
     * @param url The URL to download the image from.
     * @param fileName The the storage file to save the image to.  If this file already exists, it will simply load this file and skip the 
     * network request altogether.
     * @param onSuccess Callback called on success.
     * @param onFail Callback called if we fail to load the image.
     * @since 3.4
     * @see ConnectionRequest#downloadImageToStorage(java.lang.String, com.codename1.util.SuccessCallback, com.codename1.util.FailureCallback) 
     */
    public static void downloadImageToStorage(String url, String fileName, SuccessCallback<Image> onSuccess, FailureCallback<Image> onFail) {
        implInstance.downloadImageToStorage(url, fileName, onSuccess, onFail);
    }
    
    /**
     * Downloads an image to storage asynchronously.  If the image is already downloaded it will just load it directly from 
     * storage.
     * @param url The URL to download the image from.
     * @param fileName The the storage file to save the image to.  If this file already exists, it will simply load this file and skip the 
     * network request altogether.
     * @since 7.0
     * @see ConnectionRequest#downloadImageToStorage(java.lang.String, com.codename1.util.SuccessCallback, com.codename1.util.FailureCallback) 
     */
    public static AsyncResource<Image> downloadImageToStorage(String url, String fileName) {
        final AsyncResource<Image> out = new AsyncResource<Image>();
        downloadImageToStorage(url, fileName, new SuccessCallback<Image>() {
            @Override
            public void onSucess(Image value) {
                out.complete(value);
            }

        },
                new FailureCallback<Image>() {
            @Override
            public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                out.error(err);
            }
        }
        );
        return out;
    }
    
    /**
     * Downloads an image to the cache asynchronously.
     * @param url The URL to download.
     * @param onSuccess Callback to run on successful completion.
     * @param onFail Callback to run if download fails.
     */
    public static void downloadImageToCache(String url, SuccessCallback<Image> onSuccess, FailureCallback<Image> onFail) {
        implInstance.downloadImageToCache(url, onSuccess, onFail);
        
    }
    
    /**
     * Downloads an image to the cache asynchronously.
     * @param url The URL of the image to download.
     * @return AsyncResource to wrap the Image.
     * @since 7.0
     */
    public static AsyncResource<Image> downloadImageToCache(String url) {
        final AsyncResource<Image> out = new AsyncResource<Image>();
        downloadImageToCache(url, new SuccessCallback<Image>() {
            @Override
            public void onSucess(Image value) {
                out.complete(value);
            }

        },
                new FailureCallback<Image>() {
            @Override
            public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                out.error(err);
            }
        }
        );
        return out;
    }
    
    /**
     * Downloads an image to storage asynchronously.  If the image is already downloaded it will just load it directly from 
     * storage.
     * @param url The URL to download the image from.
     * @param fileName The the storage file to save the image to.  If this file already exists, it will simply load this file and skip the 
     * network request altogether.
     * @param onSuccess Callback called on success.
     * @since 3.4
     * @see ConnectionRequest#downloadImageToStorage(java.lang.String, com.codename1.util.SuccessCallback) 
     */
    public static void downloadImageToStorage(String url, String fileName, SuccessCallback<Image> onSuccess) {
        downloadImageToStorage(url, fileName, onSuccess, new CallbackAdapter<Image>());
    }
    
    private static boolean downloadUrlTo(String url, String fileName, boolean showProgress, boolean background, boolean storage, ActionListener callback) {
        ConnectionRequest cr = new ConnectionRequest();
        cr.setPost(false);
        cr.setFailSilently(true);
        cr.setReadResponseForErrors(false);
        cr.setDuplicateSupported(true);
        cr.setUrl(url);
        if(callback != null) {
            cr.addResponseListener(callback);
        }
        if(storage) {
            cr.setDestinationStorage(fileName);
        } else {
            cr.setDestinationFile(fileName);
        }
        if(background) {
            NetworkManager.getInstance().addToQueue(cr);
            return true;
        } 
        if(showProgress) {
            InfiniteProgress ip = new InfiniteProgress();
            Dialog d = ip.showInifiniteBlocking();
            NetworkManager.getInstance().addToQueueAndWait(cr);
            d.dispose();
        } else {
            NetworkManager.getInstance().addToQueueAndWait(cr);
        }
        if(cr.getContentLength() > 0) {
            // verify the resulting file has the same size as the content length
            if(storage) {
                if(Storage.getInstance().entrySize(fileName) < cr.getContentLength()) {
                    return false;
                }
            } else {
                if(FileSystemStorage.getInstance().getLength(fileName) < cr.getContentLength()) {
                    return false;
                }
            }
        }
        int rc = cr.getResponseCode();
        return rc == 200 || rc == 201;
    }
    
    /**
     * Shorthand method for Thread sleep that doesn't throw the stupid interrupted checked exception
     * @param t the time
     */
    public static void sleep(int t) {
        try {
            Thread.sleep(t);
        } catch(InterruptedException e) {
        }
    }
    
    
    /**
     * Shorthand method wait method that doesn't throw the stupid interrupted checked exception, it also
     * includes the synchronized block to further reduce code clutter
     * @param o the object to wait on
     * @param t the time
     */
    public static void wait(Object o, int t) {
        synchronized(o) {
            try {
                o.wait(t);
            } catch(InterruptedException e) {
            }
        }
    }    
    
    /**
     * Shorthand method wait method that doesn't throw the stupid interrupted checked exception, it also
     * includes the synchronized block to further reduce code clutter
     * @param o the object to wait on
     */
    public static void wait(Object o) {
        synchronized(o) {
            try {
                o.wait();
            } catch(InterruptedException e) {
            }
        }
    }    
    
    /**
     * Returns true or false based on a "soft" object
     * @param val a boolean value as a Boolean object, String or number
     * @return true or false
     */
    public static boolean toBooleanValue(Object val) {
        if(val == null) {
            return false;
        }
        if(val instanceof Boolean) {
            return ((Boolean)val).booleanValue();
        }
        if(val instanceof String) {
            String sl = ((String)val).toLowerCase();
            return sl.startsWith("t") || sl.equals("1");
        }
        return toIntValue(val) != 0;
    }
    
    /**
     * Returns the number object as an int
     * @param number this can be a String or any number type
     * @return an int value or an exception
     */
    public static int toIntValue(Object number) {
        if(number == null) {
            return 0;
        }
        // we should convert this to use Number
        if(number instanceof Integer) {
            return ((Integer)number).intValue();
        }
        if(number instanceof String) {
            String n = (String)number;
            if(n.length() == 0 || n.equals(" ")) {
                return 0;
            }
            return Integer.parseInt(n);
        }
        if(number instanceof Double) {
            return ((Double)number).intValue();
        }
        if(number instanceof Float) {
            return ((Float)number).intValue();
        }
        if(number instanceof Long) {
            return ((Long)number).intValue();
        }
        /*if(number instanceof Short) {
            return ((Short)number).intValue();
        }
        if(number instanceof Byte) {
            return ((Byte)number).intValue();
        }*/
        if(number instanceof Boolean) {
            Boolean b = (Boolean)number;
            if(b.booleanValue()) {
                return 1;
            }
            return 0;
        }
        throw new IllegalArgumentException("Not a number: " + number);
    }

    /**
     * Returns the number object as a long
     * @param number this can be a String or any number type
     * @return a long value or an exception
     */
    public static long toLongValue(Object number) {
        // we should convert this to use Number
        if(number instanceof Long) {
            return ((Long)number).longValue();
        }
        if(number instanceof Integer) {
            return ((Integer)number).longValue();
        }
        if(number instanceof String) {
            return Long.parseLong((String)number);
        }
        if(number instanceof Double) {
            return ((Double)number).longValue();
        }
        if(number instanceof Float) {
            return ((Float)number).longValue();
        }
        if(number instanceof Date) {
            return ((Date)number).getTime();
        }
        /*if(number instanceof Short) {
            return ((Short)number).longValue();
        }
        if(number instanceof Byte) {
            return ((Byte)number).longValue();
        }*/
        if(number instanceof Boolean) {
            Boolean b = (Boolean)number;
            if(b.booleanValue()) {
                return 1;
            }
            return 0;
        }
        throw new IllegalArgumentException("Not a number: " + number);
    }

    /**
     * Returns the number object as a float
     * @param number this can be a String or any number type
     * @return a float value or an exception
     */
    public static float toFloatValue(Object number) {
        // we should convert this to use Number
        if(number instanceof Float) {
            return ((Float)number).floatValue();
        }
        if(number instanceof Long) {
            return ((Long)number).floatValue();
        }
        if(number instanceof Integer) {
            return ((Integer)number).floatValue();
        }
        if(number instanceof String) {
            return Float.parseFloat((String)number);
        }
        if(number instanceof Double) {
            return ((Double)number).floatValue();
        }
        /*if(number instanceof Short) {
            return ((Short)number).floatValue();
        }
        if(number instanceof Byte) {
            return ((Byte)number).floatValue();
        }*/
        if(number instanceof Boolean) {
            Boolean b = (Boolean)number;
            if(b.booleanValue()) {
                return 1;
            }
            return 0;
        }
        throw new IllegalArgumentException("Not a number: " + number);
    }

    /**
     * Returns the number object as a double
     * @param number this can be a String or any number type
     * @return a double value or an exception
     */
    public static double toDoubleValue(Object number) {
        // we should convert this to use Number
        if(number instanceof Double) {
            return ((Double)number).doubleValue();
        }
        if(number instanceof Float) {
            return ((Float)number).doubleValue();
        }
        if(number instanceof Long) {
            return ((Long)number).doubleValue();
        }
        if(number instanceof Integer) {
            return ((Integer)number).doubleValue();
        }
        if(number instanceof String) {
            return Double.parseDouble((String)number);
        }
        /*if(number instanceof Short) {
            return ((Short)number).doubleValue();
        }
        if(number instanceof Byte) {
            return ((Byte)number).doubleValue();
        }*/
        if(number instanceof Boolean) {
            Boolean b = (Boolean)number;
            if(b.booleanValue()) {
                return 1;
            }
            return 0;
        }
        throw new IllegalArgumentException("Not a number: " + number);
    }
    
    private static SimpleDateFormat dateFormatter;
    
    /**
     * Sets a custom formatter to use when toDateValue is invoked
     * @param formatter the formatter to use
     */
    public static void setDateFormatter(SimpleDateFormat formatter) {
        dateFormatter = formatter;
    }
    
    /**
     * Tries to convert an arbitrary object to a date
     * @param o an object that can be a string, number or date
     * @return a Date object
     */
    public static Date toDateValue(Object o) {
        if(o == null) {
            return null;
        }
        if(o instanceof Date) {
            return (Date)o;
        }
        if(o instanceof String) {
            if(dateFormatter != null) {
                try {
                    return dateFormatter.parse((String)o);
                } catch(ParseException e) {
                    // falls back to the default formatting
                    Log.e(e);
                }
            }
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse((String)o);
            } catch(ParseException e) {
                throw new IllegalArgumentException("Not a supported date, we use this format 'yyyy-MM-dd'T'HH:mm:ss.SSS': " + o);
            }
        }
        return new Date(toLongValue(o));
    }
    
    /**
     * Encodes a string in a way that makes it harder to read it "as is" this makes it possible for Strings to be 
     * "encoded" within the app and thus harder to discover by a casual search.
     * 
     * @param s the string to decode
     * @return the decoded string
     */
    public  static String xorDecode(String s) {
        try { 
            byte[] dat = Base64.decode(s.getBytes("UTF-8"));
            for(int iter = 0 ; iter < dat.length ; iter++) {
                dat[iter] = (byte)(dat[iter] ^ (iter % 254 + 1));
            }
            return new String(dat, "UTF-8");
        } catch(UnsupportedEncodingException err) {
            // will never happen damn stupid exception
            err.printStackTrace();
            return null;
        }
    }

    /**
     * The inverse method of xorDecode, this is normally unnecessary and is here mostly for completeness
     * 
     * @param s a regular string
     * @return a String that can be used in the xorDecode method
     */
    public static String xorEncode(String s) {
        try { 
            byte[] dat = s.getBytes("UTF-8");
            for(int iter = 0 ; iter < dat.length ; iter++) {
                dat[iter] = (byte)(dat[iter] ^ (iter % 254 + 1));
            }
            return Base64.encodeNoNewline(dat);
        } catch(UnsupportedEncodingException err) {
            // will never happen damn stupid exception
            err.printStackTrace();
            return null;
        }
    }
    
    /**
     * Tries to determine the mime type of a file based on its first
     * bytes.Direct inspection of the bytes to determine the content type is
     * often more accurate than believing the content type claimed by the
     * <code>http</code> server or by the file extension.
     *
     * @param sourceFile, it automatically choose Storage API or
     * FileSystemStorage API
     * @return the detected mime type, or "application/octet-stream" if the type
     * is not detected
     * @throws IOException
     */
    public static String guessMimeType(String sourceFile) throws IOException {
        InputStream inputStream;
        if (sourceFile.indexOf('/') > -1) {
            inputStream = FileSystemStorage.getInstance().openInputStream(sourceFile);
        } else {
            // Storage is a flat file system
            inputStream = Storage.getInstance().createInputStream(sourceFile);
        }

        return guessMimeType(inputStream);
    }

    /**
     * Tries to determine the mime type of an InputStream based on its first
     * bytes.Direct inspection of the bytes to determine the content type is
     * often more accurate than believing the content type claimed by the
     * <code>http</code> server or by the file extension.
     *
     * @param in
     * @return the detected mime type, or "application/octet-stream" if the type
     * is not detected
     * @throws IOException
     */
    public static String guessMimeType(InputStream in) throws IOException {
        byte[] header = new byte[11];
        in.read(header, 0, 11);
        return guessMimeType(header);
    }

    /**
     * Tries to determine the mime type of a byte array based on its first
     * bytes.Direct inspection of the bytes to determine the content type is
     * often more accurate than believing the content type claimed by the
     * <code>http</code> server or by the file extension.
     *
     * @param data
     * @return the detected mime type, or "application/octet-stream" if the type
     * is not detected
     */
    public static String guessMimeType(byte[] data) {
        // I took the most of header codes from: https://github.com/Servoy/servoy-client/blob/e7f5bce3c3dc0f0eb1cd240fce48c75143a25432/servoy_shared/src/com/servoy/j2db/util/MimeTypes.java
        // For further reference, the header codes used by OpenJDK8 are here: https://github.com/frohoff/jdk8u-dev-jdk/blob/master/src/share/classes/java/net/URLConnection.java
        // This method can be improved according to the needs.

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("guessMimeType(byte[] data) -> data cannot be empty or null");
        }
        // If you change the number of byte, REMEMBER to change that number also in the method guessMimeType(InputStream in)
        byte[] header = new byte[11];
        System.arraycopy(data, 0, header, 0, Math.min(data.length, header.length));
        int c1 = header[0] & 0xff;
        int c2 = header[1] & 0xff;
        int c3 = header[2] & 0xff;
        int c4 = header[3] & 0xff;
        int c5 = header[4] & 0xff;
        int c6 = header[5] & 0xff;
        int c7 = header[6] & 0xff;
        int c8 = header[7] & 0xff;
        int c9 = header[8] & 0xff;
        int c10 = header[9] & 0xff;
        int c11 = header[10] & 0xff;

        if (c1 == 0xCA && c2 == 0xFE && c3 == 0xBA && c4 == 0xBE) {
            return "application/java-vm";
        }

        if (c1 == 0xD0 && c2 == 0xCF && c3 == 0x11 && c4 == 0xE0 && c5 == 0xA1 && c6 == 0xB1 && c7 == 0x1A && c8 == 0xE1) {
            return "application/msword";
        }
        if (c1 == 0x25 && c2 == 0x50 && c3 == 0x44 && c4 == 0x46 && c5 == 0x2d && c6 == 0x31 && c7 == 0x2e) {
            return "application/pdf";
        }

        if (c1 == 0x38 && c2 == 0x42 && c3 == 0x50 && c4 == 0x53 && c5 == 0x00 && c6 == 0x01) {
            return "image/photoshop";
        }

        if (c1 == 0x25 && c2 == 0x21 && c3 == 0x50 && c4 == 0x53) {
            return "application/postscript";
        }

        if (c1 == 0xff && c2 == 0xfb && c3 == 0x30) {
            return "audio/mp3";
        }

        if (c1 == 0x49 && c2 == 0x44 && c3 == 0x33) {
            return "audio/mp3";
        }

        if (c1 == 0xAC && c2 == 0xED) {
            // next two bytes are version number, currently 0x00 0x05
            return "application/x-java-serialized-object";
        }

        if (c1 == '<') {
            if (c2 == '!'
                    || ((c2 == 'h' && (c3 == 't' && c4 == 'm' && c5 == 'l' || c3 == 'e' && c4 == 'a' && c5 == 'd') || (c2 == 'b' && c3 == 'o' && c4 == 'd' && c5 == 'y')))
                    || ((c2 == 'H' && (c3 == 'T' && c4 == 'M' && c5 == 'L' || c3 == 'E' && c4 == 'A' && c5 == 'D') || (c2 == 'B' && c3 == 'O' && c4 == 'D' && c5 == 'Y')))) {
                return "text/html";
            }

            if (c2 == '?' && c3 == 'x' && c4 == 'm' && c5 == 'l' && c6 == ' ') {
                return "application/xml";
            }
        }

        // big and little endian UTF-16 encodings, with byte order mark
        if (c1 == 0xfe && c2 == 0xff) {
            if (c3 == 0 && c4 == '<' && c5 == 0 && c6 == '?' && c7 == 0 && c8 == 'x') {
                return "application/xml";
            }
        }

        if (c1 == 0xff && c2 == 0xfe) {
            if (c3 == '<' && c4 == 0 && c5 == '?' && c6 == 0 && c7 == 'x' && c8 == 0) {
                return "application/xml";
            }
        }

        if (c1 == 'B' && c2 == 'M') {
            return "image/bmp";
        }

        if (c1 == 0x49 && c2 == 0x49 && c3 == 0x2a && c4 == 0x00) {
            return "image/tiff";
        }

        if (c1 == 0x4D && c2 == 0x4D && c3 == 0x00 && c4 == 0x2a) {
            return "image/tiff";
        }

        if (c1 == 'G' && c2 == 'I' && c3 == 'F' && c4 == '8') {
            return "image/gif";
        }

        if (c1 == '#' && c2 == 'd' && c3 == 'e' && c4 == 'f') {
            return "image/x-bitmap";
        }

        if (c1 == '!' && c2 == ' ' && c3 == 'X' && c4 == 'P' && c5 == 'M' && c6 == '2') {
            return "image/x-pixmap";
        }

        if (c1 == 137 && c2 == 80 && c3 == 78 && c4 == 71 && c5 == 13 && c6 == 10 && c7 == 26 && c8 == 10) {
            return "image/png";
        }

        if (c1 == 0xFF && c2 == 0xD8 && c3 == 0xFF) {
            if (c4 == 0xE0) {
                return "image/jpeg";
            }

            /**
             * File format used by digital cameras to store images. Exif Format
             * can be read by any application supporting JPEG. Exif Spec can be
             * found at:
             * http://www.pima.net/standards/it10/PIMA15740/Exif_2-1.PDF
             */
            if ((c4 == 0xE1) && (c7 == 'E' && c8 == 'x' && c9 == 'i' && c10 == 'f' && c11 == 0)) {
                return "image/jpeg";
            }

            if (c4 == 0xEE) {
                return "image/jpg";
            }
        }

        /**
         * According to
         * http://www.opendesign.com/files/guestdownloads/OpenDesign_Specification_for_.dwg_files.pdf
         * first 6 bytes are of type "AC1018" (for example) and the next 5 bytes
         * are 0x00.
         */
        if ((c1 == 0x41 && c2 == 0x43) && (c7 == 0x00 && c8 == 0x00 && c9 == 0x00 && c10 == 0x00 && c11 == 0x00)) {
            return "application/acad";
        }

        if (c1 == 0x2E && c2 == 0x73 && c3 == 0x6E && c4 == 0x64) {
            return "audio/basic"; // .au
            // format,
            // big
            // endian
        }

        if (c1 == 0x64 && c2 == 0x6E && c3 == 0x73 && c4 == 0x2E) {
            return "audio/basic"; // .au
            // format,
            // little
            // endian
        }

        if (c1 == 'R' && c2 == 'I' && c3 == 'F' && c4 == 'F') {
            /*
             * I don't know if this is official but evidence suggests that .wav files start with "RIFF" - brown
             */
            return "audio/x-wav";
        }

        if (c1 == 'P' && c2 == 'K') {
            return "application/zip";
        }

        return "application/octet-stream"; // unknown file type
    }
    
    /**
     * Returns -1 if the content length is unknown, a value greater than 0 if
     * the Content-Length is known.
     *
     * @param url
     * @return Content-Length if known
     */
    public static long getFileSizeWithoutDownload(final String url) {
        return getFileSizeWithoutDownload(url, false);
    }

    /**
     * Returns -2 if the server doesn't accept partial downloads (and if
     * checkPartialDownloadSupport is true), -1 if the content length is unknow,
     * a value greater than 0 if the Content-Length is known.
     *
     * @param url
     * @param checkPartialDownloadSupport if true returns -2 if the server
     * doesn't accept partial downloads.
     * @return Content-Length if known
     */
    public static long getFileSizeWithoutDownload(final String url, final boolean checkPartialDownloadSupport) {
        // documentation about the headers: https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
        // code discussed here: https://stackoverflow.com/a/62130371
        final Wrapper<Long> result = new Wrapper<Long>(0l);
        ConnectionRequest cr = new GZConnectionRequest() {
            @Override
            protected void readHeaders(Object connection) throws IOException {
                String acceptRanges = getHeader(connection, "Accept-Ranges");
                if (checkPartialDownloadSupport && (acceptRanges == null || !acceptRanges.equals("bytes"))) {
                    // Log.p("The partial downloads of " + url + " are not supported.", Log.WARNING);
                    result.set(-2l);
                } else {
                    String contentLength = getHeader(connection, "Content-Length");
                    if (contentLength != null) {
                        result.set(Long.parseLong(contentLength));
                    } else {
                        // Log.p("The Content-Length of " + url + " is unknown.", Log.WARNING);
                        result.set(-1l);
                    }
                }
            }
        };
        cr.setUrl(url);
        cr.setHttpMethod("HEAD");
        cr.setPost(false);
        NetworkManager.getInstance().addToQueueAndWait(cr);
        return result.get();
    }
    
    /**
     * <p>
     * Safely download the given URL to the Storage or to the FileSystemStorage:
     * this method is resistant to network errors and capable of resume the
     * download as soon as network conditions allow and in a completely
     * transparent way for the user; note that in the global network error
     * handling, there must be an automatic
     * <pre>.retry()</pre>, as in the code example below.</p>
     * <p>
     * This method is useful if the server correctly returns Content-Length and
     * if it supports partial downloads: if not, it works like a normal
     * download.</p>
     * <p>
     * Pros: always allows you to complete downloads, even if very heavy (e.g.
     * 100MB), even if the connection is unstable (network errors) and even if
     * the app goes temporarily in the background (on some platforms the
     * download will continue in the background, on others it will be
     * temporarily suspended).</p>
     * <p>
     * Cons: since this method is based on splitting the download into small
     * parts (512kbytes is the default), this approach causes many GET requests
     * that slightly slow down the download and cause more traffic than normally
     * needed.</p>
     * <p>
     * Usage example:</p>
     * <script src="https://gist.github.com/jsfan3/554590a12c3102a3d77e17533e7eca98.js"></script>
     * 
     *
     * @param url
     * @param fileName must be a valid Storage file name or FileSystemStorage
     * file path
     * @param percentageCallback invoked (in EDT) during the download to notify
     * the progress (from 0 to 100); it can be null if you are not interested in
     * monitoring the progress
     * @param filesavedCallback invoked (in EDT) only when the download is
     * finished; if null, no action is taken
     * @throws IOException
     */
    public static void downloadUrlSafely(String url, final String fileName, final OnComplete<Integer> percentageCallback, final OnComplete<String> filesavedCallback) throws IOException {
        // Code discussion here: https://stackoverflow.com/a/62137379/1277576
        String partialDownloadsDir = FileSystemStorage.getInstance().getAppHomePath() + FileSystemStorage.getInstance().getFileSystemSeparator() + "partialDownloads";
        if (!FileSystemStorage.getInstance().isDirectory(partialDownloadsDir)) {
            FileSystemStorage.getInstance().mkdir(partialDownloadsDir);
        }
        final String uniqueId = url.hashCode() + "" + downloadUrlSafelyRandom.nextInt(); // do its best to be unique if there are parallel downloads
        final String partialDownloadPath = partialDownloadsDir + FileSystemStorage.getInstance().getFileSystemSeparator() + uniqueId;
        final boolean isStorage = fileName.indexOf("/") < 0; // as discussed here: https://stackoverflow.com/a/57984257
        final long fileSize = getFileSizeWithoutDownload(url, true); // total expected download size, with a check partial download support
        final int splittingSize = 512 * 1024; // 512 kbyte, size of each small download
        final Wrapper<Long> downloadedTotalBytes = new Wrapper<Long>(0l);
        final OutputStream out;
        if (isStorage) {
            out = Storage.getInstance().createOutputStream(fileName); // leave it open to append partial downloads
        } else {
            out = FileSystemStorage.getInstance().openOutputStream(fileName);
        }
        final EasyThread mergeFilesThread = EasyThread.start("mergeFilesThread"); // Codename One thread that supports crash protection and similar Codename One features.

        final ConnectionRequest cr = new GZConnectionRequest();
        cr.setUrl(url);
        cr.setPost(false);
        if (fileSize > splittingSize) {
            // Which byte should the download start from?
            cr.addRequestHeader("Range", "bytes=0-" + splittingSize);
            cr.setDestinationFile(partialDownloadPath);
        } else {
            Util.cleanup(out);
            if (isStorage) {
                cr.setDestinationStorage(fileName);
            } else {
                cr.setDestinationFile(fileName);
            }
        }
        cr.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                mergeFilesThread.run(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // We append the just saved partial download to the fileName, if it exists
                            if (FileSystemStorage.getInstance().exists(partialDownloadPath)) {
                                InputStream in = FileSystemStorage.getInstance().openInputStream(partialDownloadPath);
                                Util.copyNoClose(in, out, 8192);
                                Util.cleanup(in);
                                // before deleting the file, we check and update how much data we have actually downloaded
                                downloadedTotalBytes.set(downloadedTotalBytes.get() + FileSystemStorage.getInstance().getLength(partialDownloadPath));
                                FileSystemStorage.getInstance().delete(partialDownloadPath);
                            }
                            // Is the download finished?
                            if (downloadedTotalBytes.get() > fileSize) {
                                throw new IllegalStateException("More content has been downloaded than the file length, check the code.");
                            }
                            if (fileSize <= 0 || downloadedTotalBytes.get() == fileSize) {
                                // yes, download finished
                                Util.cleanup(out);
                                if (filesavedCallback != null) {
                                    CN.callSerially(new Runnable() {
                                        @Override
                                        public void run() {
                                            filesavedCallback.completed(fileName);
                                        }
                                    });
                                }
                            } else {
                                // no, it's not finished, we repeat the request after updating the "Range" header
                                cr.addRequestHeader("Range", "bytes=" + downloadedTotalBytes.get() + "-" + (Math.min(downloadedTotalBytes.get() + splittingSize, fileSize)));
                                NetworkManager.getInstance().addToQueue(cr);
                            }

                        } catch (IOException ex) {
                            Log.p("Error in appending splitted file to output file", Log.ERROR);
                            Log.e(ex);
                            Log.sendLogAsync();
                        }
                    }
                });
            }
        });
        NetworkManager.getInstance().addToQueue(cr);
        NetworkManager.getInstance().addProgressListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                if (cr == evt.getConnectionRequest() && fileSize > 0) {
                    // the following casting to long is necessary when the file is bigger than 21MB, otherwise the result of the calculation is wrong
                    if (percentageCallback != null) {
                        CN.callSerially(new Runnable() {
                            @Override
                            public void run() {
                                percentageCallback.completed((int) ((long) downloadedTotalBytes.get() * 100 / fileSize));
                            }
                        });
                    }
                }
            }
        });
    }
    
       /**
     * <p>
     * Creates a new UUID, that is a 128-bit number used to identify information
     * in computer systems. UUIDs aim to be unique for practical purposes.</p>
     *
     * <p>
     * This implementation uses the system clock and some device info as seeds
     * for random data, that are enough for practical usage. More specifically,
     * two instances of Random, instantiated with different seeds, are used. The
     * first seed corresponds to the timestamp in which the first object of the
     * static class UUID is created, the second seed is a number (long type)
     * that identifies the current installation of the app and is assumed to be
     * as different as possible from other installations of the app. A unique
     * identifier (long type) associated with the current app installation can
     * be specified by the developer via the Preference "CustomDeviceId__$" (as
     * in the following example) BEFORE the generation of the first UIID, or -
     * if it is not specified - it is obtained from an internal Codename One
     * implementation; in the worst case, if an identifier has not been
     * specified by the developer and Codename One is unable to distinguish the
     * current installation of the app from other installations, an internal
     * algorithm will be used that will generate a number based on some hardware
     * and software characteristics of the device: the number thus generated
     * will be the same on identical models of the same device and with the same
     * version of the operating system, but will vary between different models.
     * Even in the worst case scenario, the probability that two app
     * installations with identical device identifiers will generate the first
     * UIID in the same timestamp is very low.</p>
     *
     * <p>
     * As a tip, consider that any alphanumeric text string (corresponding for
     * example to a username) can be converted into a long type number,
     * considering this string as a number based on 36, provided it does not
     * exceed 12 characters. This suggestion is applied in the following
     * example.</p>
     *
     * <p>
     * Code example:</p>
     * <script src="https://gist.github.com/jsfan3/2fdc5fae2b723cba40e65faab923e552.js"></script>
     *
     * @return a pseudo-random Universally Unique Identifier in its canonical
     * textual representation
     */
    public static String getUUID() {
        return new Util.UUID().toString();
    }

    /**
     * Creates a custom UUID, from the given two <code>long</code> values.
     *
     * @param time the upper 64 bits
     * @param clockSeqAndNode the lower 64 bits
     *
     * @return a Universally Unique Identifier in its canonical textual
     * representation
     */
    public static String getUUID(long time, long clockSeqAndNode) {
        return new Util.UUID(time, clockSeqAndNode).toString();
    }

    /**
     * This class represents an UUID according to the DCE Universal Token
     * Identifier specification.
     * <p>
     * All you need to know:
     * <pre>
     * UUID u = new UUID().toString();
     * </pre>
     */
    static class UUID {

        /*
         * UUID - an implementation of the UUID specification for Codename One
         * by Francesco Galgani
         *
         * You can use this class as you want (public-domain license).
         */
        private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        private static final Random randomTime = new Random(System.currentTimeMillis());
        private static final Random randomClockSeqAndNode = new Random(getUniqueDeviceID());

        private long time = 0l;
        private long clockSeqAndNode = 0l;

        /**
         * Constructor for UUID, it uses the system clock and some device info
         * as seeds for random data, that are enough for practical usage.
         */
        public UUID() {
            this.time = randomTime.nextLong();
            this.clockSeqAndNode = randomClockSeqAndNode.nextLong();
        }

        /**
         * Constructs a UUID from two <code>long</code> values.
         *
         * @param time the upper 64 bits
         * @param clockSeqAndNode the lower 64 bits
         */
        public UUID(long time, long clockSeqAndNode) {
            this.time = time;
            this.clockSeqAndNode = clockSeqAndNode;
        }

        /**
         * Returns this UUID as a String.
         *
         * @return a String, never <code>null</code>
         */
        @Override
        public final String toString() {
            return toCanonicalForm();
        }

        private String toCanonicalForm() {
            String out = "";
            out = append(out, (int) (time >> 32)) + '-'
                    + append(out, (short) (time >> 16)) + '-'
                    + append(out, (short) time) + '-'
                    + append(out, (short) (clockSeqAndNode >> 48)) + '-'
                    + append(out, clockSeqAndNode, 12);
            return out;
        }

        private static String append(String a, short in) {
            return append(a, (long) in, 4);
        }

        private static String append(String a, int in) {
            return append(a, (long) in, 8);
        }

        private static String append(String a, long in, int length) {
            int lim = (length << 2) - 4;
            while (lim >= 0) {
                a += (DIGITS[(byte) (in >> lim) & 0x0f]);
                lim -= 4;
            }
            return a;
        }

        private static long getUniqueDeviceID() {
            long id = Preferences.get("CustomDeviceId__$", (long) -1);
            if (id == -1) {
                id = Preferences.get("DeviceId__$", (long) -1);
            }
            if (id == -1) {
                id = generateLongFromDeviceInfo();
            }
            return id;
        }

        /**
         * Generates a long number using some device info: the same type of
         * device (a specific model of a specific brand, with the same OS
         * version) will always produce the same number, while different devices
         * will most likely produce different numbers.
         *
         * @return
         */
        private static long generateLongFromDeviceInfo() {
            long random = CN.getDeviceDensity()
                    * CN.getDisplayHeight()
                    * CN.getDisplayWidth()
                    * CN.convertToPixels(10)
                    * Long.parseLong(sanitizeString(CN.getPlatformName()), 36)
                    * Long.parseLong(sanitizeString(CN.getProperty("User-Agent", "1")), 36)
                    * Long.parseLong(sanitizeString(CN.getProperty("OSVer", "1")), 36);
            return random;
        }

        /**
         * Removes all non-alphanumeric characters from a string and returns the
         * first 10 characters.
         *
         * @param input
         * @return
         */
        private static String sanitizeString(String input) {
            String result = "";
            for (char myChar : input.toCharArray()) {
                if ((myChar >= '0' && myChar <= '9') || (myChar >= 'a' && myChar <= 'z') || (myChar >= 'A' && myChar <= 'Z')) {
                    result += myChar;
                }
            }
            return result.substring(0, Math.min(10, result.length())).toUpperCase();
        }

    }
}
