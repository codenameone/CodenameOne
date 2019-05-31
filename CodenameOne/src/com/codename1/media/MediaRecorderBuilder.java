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

/**
 * A builder class to generate a Media recorder with specific settings.  
 * @author shannah 
 * @since 7.0
 */
public class MediaRecorderBuilder {
    private int audioChannels=1,
            bitRate=64000,
            samplingRate=44100;
    
    private String mimeType = Display.getInstance().getAvailableRecordingMimeTypes()[0],
            path;
    
    /**
     * Set the number of audio channels in the media recorder.  Default 1.
     * @param numChannels The number of audio channels in the media recorder.
     * @return Self for chaining
     */
    public MediaRecorderBuilder audioChannels(int numChannels) {
        this.audioChannels = numChannels;
        return this;
    }
    
    /**
     * Sets the bit rate for the recorder.  Default 64000.
     * 
     * @param bitRate The bit rate for the recorder.
     * @return Self for chaining.
     */
    public MediaRecorderBuilder bitRate(int bitRate) {
        this.bitRate = bitRate;
        return this;
    }
    
    /**
     * Sets the sampling rate for the recorder.  Default 44100
     * @param samplingRate The sample rate for the recorder.
     * @return Self for chaining.
     */
    public MediaRecorderBuilder samplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
        return this;
    }
    
    /**
     * Sets the mimetype to use for encoding the audio file.
     * @param mimeType The mimetype to use for encoding the audio file.
     * @return Self for chaining.
     * @see MediaManager#getAvailableRecordingMimeTypes() 
     */
    public MediaRecorderBuilder mimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }
    
    /**
     * Sets the output path where the audio recording should be saved.
     * @param path The output path where the recording should be saved.
     * @return Self for chaining.
     */
    public MediaRecorderBuilder path(String path) {
        this.path = path;
        return this;
    }
    
    /**
     * Builds the MediaRecorder with the given settings.
     * @return
     * @throws IOException 
     * @throws IllegalStateException If {@link #path(java.lang.String) } is not set.
     */
    public Media build() throws IOException {
        if (path == null) {
            throw new IllegalStateException("Must set path for MediaRecorderBuilder");
        }
        
        return Display.getInstance().createMediaRecorder(this);
    }

    /**
     * Gets the current audio channels settings.
     * @return the audioChannels
     */
    public int getAudioChannels() {
        return audioChannels;
    }

    /**
     * Gets the current bit rate.
     * @return the bitRate
     */
    public int getBitRate() {
        return bitRate;
    }

    /**
     * Gets the current sampling rate.
     * @return the samplingRate
     * 
     */
    public int getSamplingRate() {
        return samplingRate;
    }

    

    /**
     * Gets the current mimetype.
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Gets the current output path.
     * @return the path
     */
    public String getPath() {
        return path;
    }
            
            
    
}
