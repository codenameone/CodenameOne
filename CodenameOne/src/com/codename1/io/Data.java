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
package com.codename1.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * An interface that can be implemented by any object to allow it to be treated as data.  Data
 * has a size, and can be appended to an OutputStream.  It is useful primarily for adding a custom
 * body to a ConnectionRequest.
 * @author shannah
 * @since 7.0
 */
public interface Data {
    
    /**
     * Appends the data's content to an output stream.
     * @param output The output stream to append to.
     * @throws IOException 
     */
    public void appendTo(OutputStream output) throws IOException;
    
    /**
     * Gets the size of the data content.
     * @return Size of content in bytes.
     * @throws IOException 
     */
    public long getSize() throws IOException;
    
    public static class StringData implements Data {
        private byte[] bytes;
        
        public StringData(String str) {
            this(str, "UTF-8");
        }
        
        public StringData(String str, String charset) {
            try {
                bytes = str.getBytes(charset);
            } catch (UnsupportedEncodingException ex) {
                Log.e(ex);
                throw new RuntimeException("Failed to create StringData with encoding "+charset);
            }
        }
        @Override
        public void appendTo(OutputStream output) throws IOException {
            output.write(bytes);
        }

        @Override
        public long getSize() throws IOException {
            return bytes.length;
        }
        
    }
    
    /**
     * Wraps a File as a Data object.
     * @since 7.0
     */
    public static class FileData implements Data {
        private File file;

        /**
         * Creates a new Data wrapper for a file.
         * @param file The file to be wrapped.
         */
        public FileData(File file) {
            this.file = file;
        }
        
        /**
         * {@inheritDoc }
         */
        @Override
        public void appendTo(OutputStream output) throws IOException {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            Util.copyNoClose(fs.openInputStream(file.getAbsolutePath()), output, 8192);
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public long getSize() throws IOException {
            return file.length();
        }
    }
    
    /**
     * Wraps a Storage object as a Data object.
     * @since 7.0
     */
    public static class StorageData implements Data {
        private String key;
        
        /**
         * Creates a new Data wrapper for a storage key.
         * @param key The storage key.
         */
        public StorageData(String key) {
            this.key = key;
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public void appendTo(OutputStream output) throws IOException {
            Util.copyNoClose(Storage.getInstance().createInputStream(key), output, 8192);
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public long getSize() throws IOException {
            return Storage.getInstance().entrySize(key);
        }
    }
    
    /**
     * Wraps a byte[] array as a Data object.
     * @since 7.0
     */
    public static class ByteData implements Data {
        private byte[] bytes;
        
       
        /**
         * Creates a new Data object that wraps a byte array.
         * @param bytes 
         */
        public ByteData(byte[] bytes) {
           this.bytes = bytes;
        }
        
        /**
         * {@inheritDoc }
         * 
         */
        @Override
        public void appendTo(OutputStream output) throws IOException {
            output.write(bytes);
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public long getSize() throws IOException {
            return bytes.length;
        }
    }
}
