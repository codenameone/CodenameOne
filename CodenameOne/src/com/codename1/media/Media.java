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

import com.codename1.ui.Component;


/**
 * This is the Media control interface 
 * @see MediaManager
 */
public interface Media {
    
    /**
     * Starts playing the audio file
     *
     * @param handle the handle object returned by create audio
     */
    public void play();

    /**
     * Pauses the playback of the audio file
     *
     * @param handle the handle object returned by create audio
     */
    public void pause();

    /**
     * Stops the audio playback and cleans up the resources related to it immediately.
     *
     * @param handle the playback handle
     */
    public void cleanup();

    /**
     * Returns the time in seconds in the audio file
     *
     * @param handle the handle object returned by create audio
     * @return time in milli seconds
     */
    public int getTime();

    /**
     * Sets the position in the audio file
     *
     * @param handle the handle object returned by create audio
     * @param time in milli seconds
     */
    public void setTime(int time);

    /**
     * Returns the length in seconds of the audio file
     *
     * @param handle the handle object returned by create audio
     * @return time in milli seconds
     */
    public int getDuration();

    /**
     * Sets the media playback volume in percentage
     *
     * @param vol the volume for media playback
     */
    public void setVolume(int vol);

    /**
     * Returns the media playback volume in percentage
     *
     * @return the volume percentage
     */
    public int getVolume();
    
    /**
     * Returns true if the media is currently playing
     * @return 
     */
    public boolean isPlaying();
    
    /**
     * Gets the VideoComponent of this Video.
     * 
     * @return a Component of the video to be placed on a Form or null if this
     * Media is not a Video
     */
    public Component getVideoComponent();
    
    /**
     * This method returns true if this is a Video Media
     * @return true if video
     */
    public boolean isVideo();
    
    /**
     * This method returns true if this video is in full screen mode.
     * @return true if full screen
     */
    public boolean isFullScreen();

    /**
     * Sets the Media to be displayed full screen, make sure the 
     * getVideoComponent() is called on the Video Component is added to the 
     * current Form
     * @param fullScreen 
     */
    public void setFullScreen(boolean fullScreen);
    
    /**
     * By calling this the Media (if it's a Video) will be played full screen
     * on the native Player of the device.
     * Native playing assumes getVideoComponent() hasn't been called on this Media
     * unexpected behaviour may occur if getVideoComponent() has been called
     * and the Component is placed on a Form.
     * Some platforms such as BlackBerry is able to play video only on the native
     * player on those platforms isNativePlayerMode() will always return true
     */
    public void setNativePlayerMode(boolean nativePlayer);
    
    /**
     * Returns true if this Video Media is in Native player mode.
     * Some platforms such as BlackBerry is able to play video only on the native
     * player on those platforms isNativePlayerMode() will always return true
     * @return 
     */
    public boolean isNativePlayerMode();
    
}
