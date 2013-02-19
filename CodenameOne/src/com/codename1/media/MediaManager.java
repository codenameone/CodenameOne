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
package com.codename1.media;

import com.codename1.ui.Display;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class is the Media Manager creator.
 * Media can be created with a String URI or with a stream of the media.
 * Notice that the underlying platforms contains the actual codecs, therefore 
 * you need to play common media file types such as mp3, mp4 to succeed playing 
 * on all target platforms.
 * 
 */
public class MediaManager {

    /**
     * Creates a Media from a given URI
     * 
     * @param uri the uri of the media can start with file://, http:// (can also
     * use rtsp:// although may not be supported on all target platforms)
     * @param isVideo a boolean flag to indicate if this is a video media
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * @throws IOException if creation of media from the given URI has failed
     */
    public static Media createMedia(String uri, boolean isVideo) throws IOException {
        return createMedia(uri, isVideo, null);
    }

    /**
     * Creates the Media in the given stream
     * Notice that a Media is "auto destroyed" on completion and cannot be played
     * twice!
     *
     * @param stream the stream containing the media data
     * @param mimeType the type of the data in the stream
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * @throws java.io.IOException if the creation of the Media has failed
     */
    public static Media createMedia(InputStream stream, String mimeType) throws IOException {
        return createMedia(stream, mimeType, null);
    }

    /**
     * Creates a Media from a given URI
     * 
     * @param uri the uri of the media can start with file://, http:// (can also
     * use rtsp:// although may not be supported on all target platforms)
     * @param isVideo a boolean flag to indicate if this is a video media
     * @param onCompletion a Runnable to be called when the media has finished
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * @throws IOException if creation of media from given URI failed
     */
    public static Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {
        return Display.getInstance().createMedia(uri, isVideo, onCompletion);
    }

    /**
     * Creates the Media in the given stream
     * Notice that a Media is "auto destroyed" on completion and cannot be played
     * twice!
     *
     * @param stream the stream containing the media data
     * @param mimeType the type of the data in the stream
     * @param onCompletion a Runnable to be called when the media has finished
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * @throws java.io.IOException if the URI access fails
     */
    public static Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return Display.getInstance().createMedia(stream, mimeType, onCompletion);
    }
    
    /**
     * Creates a Media recorder Object which will record from the device mic to
     * a file in the given path.
     * The output format will be amr-nb if supported by the platform.
     * 
     * @param path a file path to where to store the recording, if the file does
     * not exists it will be created.
     * @deprecated see createMediaRecorder(String path, String mimeType) instead
     */
    public static Media createMediaRecorder(String path) throws IOException {
        return createMediaRecorder(path, getAvailableRecordingMimeTypes()[0]);
    }
    
    /**
     * Gets the recording mime type for the returned Media from the 
     * createMediaRecorder method
     * 
     * @return the recording mime type
     * @deprecated see getAvailableRecordingMimeTypes() instead
     */
    public static String getMediaRecorderingMimeType(){
        return Display.getInstance().getMediaRecorderingMimeType();
    }
    
    /**
     * Gets the available recording MimeTypes
     */ 
    public static String[] getAvailableRecordingMimeTypes(){
        return Display.getInstance().getAvailableRecordingMimeTypes();        
    }
    
    /**
     * Creates a Media recorder Object which will record from the device mic to
     * a file in the given path.
     * 
     * @param path a file path to where to store the recording, if the file does
     * not exists it will be created.
     * @param mimeType the output mime type that is supported see 
     * getAvailableRecordingMimeTypes()
     * @throws IllegalArgumentException if given mime-type is not supported
     * @throws IOException id failed to create a Media object
     */
    public static Media createMediaRecorder(String path, String mimeType) throws IOException {
        boolean supportedMime = false;
        String [] supported  = getAvailableRecordingMimeTypes();
        for (int i = 0; i < supported.length; i++) {
            String mime = supported[i];
            if(mime.equals(mimeType)){
                supportedMime = true;
                break;
            }
        }
        if(!supportedMime){
            throw new IllegalArgumentException("Mime type " + mimeType + 
                    " is not supported on this platform use "
                    + "getAvailableRecordingMimeTypes()");
        }
        return Display.getInstance().createMediaRecorder(path, mimeType);
    }
}
