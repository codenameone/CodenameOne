/*
 * Copyright (c) 2019, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.samples;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.DatagramSocket;
import java.net.ServerSocket;

public class IOUtil {
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
    
     public static String readToString(InputStream input) throws IOException {
        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                    input));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) 
            response.append(inputLine).append("\n");

        in.close();

        return response.toString();
    }
    
     
    public static void copyResourceToFile(Class context, String resource, File dest) throws IOException {
        try (InputStream is = context.getResourceAsStream(resource)) {
            try (OutputStream os = new FileOutputStream(dest)) {
                copy(is, os);
            }
        }
    }
    
    private static final int BUFFER_SIZE = 8192;

/**
     * Reads all bytes from an input stream and writes them to an output stream.
     */
    public static long copy(InputStream source, OutputStream sink)
        throws IOException
    {
        long nread = 0L;
        byte[] buf = new byte[BUFFER_SIZE];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }
    
    public static Object readObject(Object lock, DataInputStream dis) throws IOException, ClassNotFoundException {
        synchronized(lock) {
            int len = dis.readInt();
            byte[] buf = new byte[len];
            dis.readFully(buf);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                return ois.readObject();
            }
        }
    }
    
    public static void writeObject(Object lock, DataOutputStream dos, Object obj) throws IOException {
        synchronized(lock) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(obj);
            }
            byte[] bytes = baos.toByteArray();
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }
    }
    
    
    /**
     * Checks if a port is available.
     * @param port
     * @return 
     */
    public static boolean available(int port) {
        
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }
    
    
}