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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>Unlike networking, the file system storage mostly tries to emulate java.io.File with
 * some simplifications for mobile devices.<br>
 * Check out a more thorough discussion of this API {@link com.codename1.io here}.<br>
 * A lot of API's rely on {@code FileSystemStorage} as its the API native code usually uses consistently.
 * E.g. in this sample below the {@code FileSystemStorage} is used to save a screenshot image for sharing
 * on social media:</p>
 * <script src="https://gist.github.com/codenameone/6bf5e68b329ae59a25e3.js"></script>
 *
 * <p>
 * The sample below shows the {@code FileSystemStorage} as a tree:
 * </p>
 * <script src="https://gist.github.com/codenameone/2877412809a8cff646af.js"></script>            
 * <img src="https://www.codenameone.com/img/developer-guide/filesystem-tree.png" alt="Simple sample of a tree for the FileSystemStorage API">
 * 
 * @author Shai Almog
 */
public class FileSystemStorage {
    private static FileSystemStorage INSTANCE = new FileSystemStorage();

    /**
     * Represents the type for the get root type method, this type generally represents the main
     * phone memory
     */
    public static final int ROOT_TYPE_MAINSTORAGE = 1;

    /**
     * Represents the type for the get root type method, this type generally represents an
     * SD card although due to variability in phone standards an SD card might be
     * detected incorrectly. E.g. newer Nokia devices such as N97 have a large storage
     * area that is marked as "E:" but is really internal storage. If an SD card isn't
     * physically in the phone the "F:" won't be returned and it will be impossible to
     * detect that "E:" is not the actual SD card.
     */
    public static final int ROOT_TYPE_SDCARD = 2;

    /**
     * Returned for different types of root for which there is no specific knowledge one
     * way or the other.
     */
    public static final int ROOT_TYPE_UNKNOWN = 3;

    private FileSystemStorage() {
    }

    /**
     * This class is a singleton
     *
     * @return instance of this class
     */
    public static FileSystemStorage getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the filesystem roots from which the structure of the file system
     * can be traversed
     *
     * @return the roots of the filesystem
     */
    public String[] getRoots() {
        return Util.getImplementation().listFilesystemRoots();
    }

    /**
     * Returns the type of the root often by guessing
     *
     * @param root the root whose type we are checking
     * @return one of the type constants above
     */
    public int getRootType(String root) {
        return Util.getImplementation().getRootType(root);
    }


    /**
     * Lists the files within the given directory, returns relative file names and not
     * full file names.
     *
     * @param directory the directory in which files should be listed
     * @return array of file names
     */
    public String[] listFiles(String directory) throws IOException {
        return Util.getImplementation().listFiles(directory);
    }

    /**
     * Returns the size of the given root directory
     *
     * @param root the root directory in the filesystem
     * @return the byte size of the directory
     */
    public long getRootSizeBytes(String root) {
        return Util.getImplementation().getRootSizeBytes(root);
    }

    /**
     * Returns the available space in the given root directory
     *
     * @param root the root directory in the filesystem
     * @return the bytes available in the directory
     */
    public long getRootAvailableSpace(String root) {
        return Util.getImplementation().getRootAvailableSpace(root);
    }

    /**
     * Creates the given directory
     *
     * @param directory the directory name to create
     */
    public void mkdir(String directory) {
        Util.getImplementation().mkdir(directory);
    }
    

    /**
     * Deletes the specific file or empty directory.
     *
     * @param file file or empty directory to delete
     */
    public void delete(String file) {
        Util.getImplementation().deleteFile(file);
    }

    /**
     * Deletes the specific file or empty directory, if the platform supports a 
     * delete on exit this method will activate it. Regardless it will retry 
     * deleting (with delay) several times to allow streams time to close.
     *
     * @param file file to delete
     * @param retryCount the number of times to retry
     */
    public void deleteRetry(final String file, final int retryCount) {
        System.gc();
        try {
            Util.getImplementation().deleteFile(file);
        } catch(Throwable t) {
        }
        if(Util.getImplementation().exists(file)) {
            if(retryCount > 0)  {
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        deleteRetry(file, retryCount - 1);
                    }
                }, 500);
            }
        }
    }


    /**
     * Indicates whether a file exists
     *
     * @param file the file to check
     * @return true if the file exists and false otherwise
     */
    public boolean exists(String file) {
        return Util.getImplementation().exists(file);
    }

    /**
     * Indicates the hidden state of the file
     *
     * @param file file
     * @return true for a hidden file
     */
    public boolean isHidden(String file) {
        return Util.getImplementation().isHidden(file);
    }

    /**
     * Toggles the hidden state of the file
     *
     * @param file file
     * @param h hidden state
     */
    public void setHidden(String file, boolean h) {
        Util.getImplementation().setHidden(file, h);
    }

    /**
     * Renames a file to the given name, expects the new name to be relative to the
     * current directory
     *
     * @param file absolute file name
     * @param newName relative new name
     */
    public void rename(String file, String newName) {
        if(newName.indexOf('/') > -1) {
            throw new RuntimeException("Rename accepts only relative file names not full paths: " + newName);
        }
        Util.getImplementation().rename(file, newName);        
    }

    /**
     * Returns the length of the file
     *
     * @param file file
     * @return length of said file
     */
    public long getLength(String file) {
        return Util.getImplementation().getFileLength(file);
    }

    /**
     * Returns the time that the file denoted by this abstract pathname was 
     * last modified.
     * @return A long value representing the time the file was last modified, 
     * measured in milliseconds
     */ 
    public long getLastModified(String file) {
        return Util.getImplementation().getFileLastModified(file);
    }
    
    /**
     * Indicates whether the given file is a directory
     *
     * @param file file
     * @return true if its a directory
     */
    public boolean isDirectory(String file) {
        return Util.getImplementation().isDirectory(file);
    }

    /**
     * Returns the file system separator char normally '/'
     *
     * @return the separator char
     */
    public char getFileSystemSeparator() {
        return Util.getImplementation().getFileSystemSeparator();
    }

    /**
     * Opens an output stream to the given file
     * 
     * @param file the file
     * @return the output stream
     */
    public OutputStream openOutputStream(String file) throws IOException {
        return Util.getImplementation().openFileOutputStream(file);
    }

    /**
     * Opens an input stream to the given file
     *
     * @param file the file
     * @return the input stream
     */
    public InputStream openInputStream(String file) throws IOException {
        return Util.getImplementation().openFileInputStream(file);
    }

    /**
     * Opens an output stream to the given file
     *
     * @param file the file
     * @param offset position in the file
     * @return the output stream
     */
    public OutputStream openOutputStream(String file, int offset) throws IOException {
        return Util.getImplementation().openOutputStream(file, offset);
    }

    /**
     * <p>The application home directory is a "safe place" to store files for this application in a portable way.
     * On some platforms such as Android  &amp; iOS this path may be visible only to the 
     * application itself, other apps won't have permission to access this path.<br>
     * The sample below uses the app home directory to save a file so we can share it using the {@link com.codename1.components.ShareButton}:</p>
     * 
     *  <script src="https://gist.github.com/codenameone/6bf5e68b329ae59a25e3.js"></script>
     * 
     * @return a writable directory that represent the application home directory
     */
     public String getAppHomePath(){
         return Util.getImplementation().getAppHomePath();
    }
     
     /**
      * Returns true if the device has a directory dedicated for "cache" files
      * @return true if a caches style directory exists in this device type
      */
     public boolean hasCachesDir() {
         return Util.getImplementation().hasCachesDir();
     }

     /**
      * Returns a device specific directory designed for cache style files, or null if {@link #hasCachesDir()}
      * is false
      * @return file URL or null
      */
     public String getCachesDir() {
         return Util.getImplementation().getCachesDir();
     }
}
