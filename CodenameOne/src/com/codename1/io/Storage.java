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

import com.codename1.util.StringUtil;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstracts the underlying application specific storage system such as RMS
 *
 * @author Shai Almog
 */
public class Storage {
    private final CacheMap cache = new CacheMap();
    private static Storage INSTANCE;
    private static FSStorage FSINSTANCE;
    private boolean normalizeNames = true;
    
    /**
     * Type value for Storage object that accesses regular storage.
     * @see #getType() 
     * @since 3.4
     */
    public static final int TYPE_STORAGE=1;
    
    /**
     * Type value for Storage object that wraps {@link FileSystemStorage}
     * @since 3.4
     */
    public static final int TYPE_FILESYSTEM=2;
    
    /**
     * @since 3.4
     * @see #getType() 
     */
    protected int type=TYPE_STORAGE;

    /**
     * Indicates the caching size, storage can be pretty slow
     * 
     * @param size size in elements (not kb!)
     */
    public void setHardCacheSize(int size) {
        cache.setCacheSize(size);
    }

    /**
     * If a file name contains slashes replace them with underscores, same goes for *, %, ? etc.
     * @param name the file name
     * @return the fixed filename
     */
    private String fixFileName(String name) {
        if(normalizeNames) {
            name = StringUtil.replaceAll(name, "/", "_");
            name = StringUtil.replaceAll(name, "\\", "_");
            name = StringUtil.replaceAll(name, "%", "_");
            name = StringUtil.replaceAll(name, "?", "_");
            name = StringUtil.replaceAll(name, "*", "_");
            name = StringUtil.replaceAll(name, ":", "_");
            name = StringUtil.replaceAll(name, "=", "_");            
        }
        return name;
    }
    
    /**
     * This method must be invoked before using the storage otherwise some platforms
     * might fail without the application data.
     *
     * @param data either the name of the application e.g. on CDC platforms or
     * a context object on other platforms
     */
    private static void init(Object data) {
        Util.getImplementation().setStorageData(data);
        INSTANCE = new Storage();
    }
    
    /**
     * Gets the type of storage that this storage object represents.  Will be one of {@link #TYPE_STORAGE} or
     * {@link #TYPE_FILESYSTEM}.
     * @return 
     * @since 3.4
     */
    public int getType() {
        return type;
    }

    /**
     * Returns true if the storage is initialized
     * 
     * @return true if the storage is initialized
     */
    public static boolean isInitialized(){
        return INSTANCE != null;
    }
    
    /**
     * Returns the storage instance or null if the storage wasn't initialized using
     * a call to init(String) first.
     *
     * @return storage instance
     */
    public static Storage getInstance() {
        if(INSTANCE == null) {
            init("cn1");
        }
        return INSTANCE;
    }
    
    /**
     * Obtains the Storage object for the specified type of storage.  
     * @param type Should be one of {@link #TYPE_FILESYSTEM} or {@link #TYPE_STORAGE}.
     * @return A Storage object for writing to storage.  If type is {@link #TYPE_FILESYSTEM},
     * then this will return a wrapper around {@link FileSystemStorage} that conforms to the {@link Storage} API.
     */
    public static Storage getInstance(int type) {
        switch (type) {
            case TYPE_FILESYSTEM: {
                if (FSINSTANCE == null) {
                    FSINSTANCE = new FSStorage();
                }
                return FSINSTANCE;
            }
            default :
                return getInstance();
        }
    }

    /**
     * Storage is cached for faster access, however this might cause a problem with refreshing
     * objects since they are not cloned. Clearing the cache allows to actually reload from the
     * storage file.
     */
    public void clearCache() {
        cache.clearAllCache();
    }
    
    /**
     * Flush the storage cache allowing implementations that cache storage objects
     * to store
     */
    public void flushStorageCache() {
        Util.getImplementation().flushStorageCache();
    }

    /**
     * Deletes the given file name from the storage
     *
     * @param name the name of the storage file
     */
    public void deleteStorageFile(String name) {
        name = fixFileName(name);
        Util.getImplementation().deleteStorageFile(name);
        cache.delete(name);
    }

    /**
     * Deletes all the files in the application storage
     */
    public void clearStorage() {
        Util.getImplementation().clearStorage();
        cache.clearAllCache();
    }

    /**
     * Creates an output stream to the storage with the given name
     *
     * @param name the storage file name
     * @return an output stream of limited capacity
     */
    public OutputStream createOutputStream(String name) throws IOException {
        name = fixFileName(name);
        return Util.getImplementation().createStorageOutputStream(name);
    }

    /**
     * Creates an input stream to the given storage source file
     *
     * @param name the name of the source file
     * @return the input stream
     */
    public InputStream createInputStream(String name) throws IOException {
        name = fixFileName(name);
        return Util.getImplementation().createStorageInputStream(name);
    }

    /**
     * Returns true if the given storage file exists
     *
     * @param name the storage file name
     * @return true if it exists
     */
    public boolean exists(String name) {
        name = fixFileName(name);
        return Util.getImplementation().storageFileExists(name);
    }

    /**
     * Lists the names of the storage files
     *
     * @return the names of all the storage files
     */
    public String[] listEntries() {
        return Util.getImplementation().listStorageEntries();
    }

    /**
     * Returns the size in bytes of the given entry
     * @param name the name of the entry
     * @return the size in bytes
     */
    public int entrySize(String name) {
        name = fixFileName(name);
        return Util.getImplementation().getStorageEntrySize(name);
    }
    
    /**
     * Writes the given object to storage assuming it is an externalizable type
     * or one of the supported types
     *
     * @param name store name
     * @param o object to store
     * @return true for success, false for failue
     */
    public boolean writeObject(String name, Object o) {
        name = fixFileName(name);
        cache.put(name, o);
        DataOutputStream d = null;
        try {
            d = new DataOutputStream(createOutputStream(name));
            Util.writeObject(o, d);
            d.close();
            return true;
        } catch(Exception err) {
            err.printStackTrace();
            Util.getImplementation().deleteStorageFile(name);
            Util.getImplementation().cleanup(d);
            return false;
        }
    }

    /**
     * Reads the object from the storage, returns null if the object isn't there
     *
     * @param name name of the store
     * @return object stored under that name
     */
    public Object readObject(String name) {
        name = fixFileName(name);
        Object o = cache.get(name);
        if(o != null) {
            return o;
        }
        DataInputStream d = null;
        try {
            if(!exists(name)) {
                return null;
            }
            d = new DataInputStream(createInputStream(name));
            o = Util.readObject(d);
            d.close();
            cache.put(name, o);
            return o;
        } catch(Exception err) {
            err.printStackTrace();
            Util.getImplementation().cleanup(d);
            return null;
        }
    }

    /**
     * Indicates whether characters that are typically illegal in filesystems should
     * be sanitized and replaced with underscore  
     * @return the normalizeNames
     */
    public boolean isNormalizeNames() {
        return normalizeNames;
    }

    /**
     * Indicates whether characters that are typically illegal in filesystems should
     * be sanitized and replaced with underscore  
     * @param normalizeNames the normalizeNames to set
     */
    public void setNormalizeNames(boolean normalizeNames) {
        this.normalizeNames = normalizeNames;
    }
    
    /**
     * Wrapper class around FileSystemStorage.  Use {@link #getInstance(int) }
     * to access instance of this object.
     */
    private static class FSStorage extends Storage {
        FileSystemStorage fs;
        private FSStorage() {
            fs = FileSystemStorage.getInstance();
            type = TYPE_FILESYSTEM;
        }

        @Override
        public InputStream createInputStream(String name) throws IOException {
            return fs.openInputStream(name);
        }

        @Override
        public OutputStream createOutputStream(String name) throws IOException {
            return fs.openOutputStream(name);
        }

        @Override
        public void deleteStorageFile(String name) {
            fs.delete(name);
        }

        @Override
        public boolean exists(String name) {
            return fs.exists(name);
        }

        @Override
        public int entrySize(String name) {
            return (int)fs.getLength(name);
        }

        @Override
        public String[] listEntries() {
            throw new RuntimeException("listEntries() not supported in FileSystemStorage");
        }

        @Override
        public void setHardCacheSize(int size) {
            throw new RuntimeException("setHardCacheSize() not supported in FileSystemStorage");
        }

        @Override
        public void clearCache() {
            throw new RuntimeException("clearCache() not supported in FileSystemStorage");
        }

        @Override
        public Object readObject(String name) {
            if (!fs.exists(name)) {
                return null;
            }
            InputStream is=null;
            DataInputStream dis=null;
            try {
                is = fs.openInputStream(name);
                dis = new DataInputStream(is);
                Object out = Util.readObject(dis);
                return out;
            } catch (Exception ex) {
                return null;
            } finally {
                Util.cleanup(dis);
                Util.cleanup(is);
            }
        }

        @Override
        public boolean writeObject(String name, Object o) {
            DataOutputStream daos=null;
            try {
                daos = new DataOutputStream(fs.openOutputStream(name));
                Util.writeObject(o, daos);
                return true;
            } catch (Exception ex) {
                Log.e(ex);
                throw new RuntimeException("Problem occurred writing object to '"+name+"' in file system.  Please ensure that the parent directory exists: " + ex.getMessage());
            } finally {
                Util.cleanup(daos);
            }
        }
        
        @Override
        public void flushStorageCache() {
            throw new RuntimeException("flushStorageCache() not supported in FileSystemStorage");
        }

        @Override
        public void clearStorage() {
            throw new RuntimeException("clearStorage() not supported in FileSystemStorage");
        }
    }
}
