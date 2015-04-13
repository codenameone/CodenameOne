/**
 * Copyright 2012 Kamran Zafar 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 */

/**
 * The original source has been modified by Paul Williams.
 */

package com.codename1.io.tar;

import java.io.IOException;

import com.codename1.io.FileSystemStorage;

/**
 * @author Kamran
 * 
 */
public class TarUtils {
    /**
     * Determines the tar file size of the given folder/file path
     * 
     * @param path
     * @return
     * @throws IOException 
     */
    public static long calculateTarSize(String path) throws IOException {
        return tarSize( path ) + TarConstants.EOF_BLOCK;
    }

    private static long tarSize(String path) throws IOException {
        long size = 0;

        FileSystemStorage fileSystem = FileSystemStorage.getInstance();
        
        if (fileSystem.isDirectory(path)) {
            String[] subFiles = fileSystem.listFiles(path);

            if (subFiles != null && subFiles.length > 0) {
                for (String file : subFiles) {
                    if (fileSystem.isDirectory(file)) {
                        size += tarSize( file );
                    } else {
                    	size += entrySize( fileSystem.getLength(file) );
                    }
                }
            } else {
                // Empty folder header
                return TarConstants.HEADER_BLOCK;
            }
        } else {
        	return entrySize( fileSystem.getLength(path) );
        }

        return size;
    }

    private static long entrySize(long fileSize) {
        long size = 0;
        size += TarConstants.HEADER_BLOCK; // Header
        size += fileSize; // File size

        long extra = size % TarConstants.DATA_BLOCK;

        if (extra > 0) {
            size += ( TarConstants.DATA_BLOCK - extra ); // pad
        }

        return size;
    }
}
