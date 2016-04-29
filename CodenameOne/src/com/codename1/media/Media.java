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
 *<p> Media control interface allows for media playback, recording. To get an instance
 * of this interface (implemented by the native port) see the MediaManager class.</p>
 * <p>
 * The sample code below demonstrates simple video playback.
 * </p>
 * 
 * <script src="https://gist.github.com/codenameone/fb73f5d47443052f8956.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-mediaplayer.png" alt="Media player sample" />
 * 
 * <p>
 *     The code below demonstrates capturing and playing back audio files using this API:
 * </p>
 * <script src="https://gist.github.com/codenameone/a347dc9dcadaa759d0cb.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/capture-audio.png" alt="Captured recordings in the demo" />
 * 
 * @see MediaManager
 */
public interface Media {
    /**
     * Write only variable that can be used with setVariable to pass a title for the 
     * native layer so the currently playing media title will be displayed in the lock screen
     * where applicable
     */
    public static final String VARIABLE_BACKGROUND_TITLE = "bgTitle";

    /**
     * Write only variable that can be used with setVariable to pass the artist name to the
     * native layer so the currently playing media title will be displayed in the lock screen
     * where applicable
     */
    public static final String VARIABLE_BACKGROUND_ARTIST = "bgArtist";

    /**
     * Write only variable that can be used with setVariable to pass the duration for the media as a Long object to the
     * native layer so the currently playing media title will be displayed in the lock screen
     * where applicable
     */
    public static final String VARIABLE_BACKGROUND_DURATION = "bgDuration";

    /**
     * Write only variable that can be used with setVariable to pass the album cover Image to the
     * native layer so the currently playing media title will be displayed in the lock screen
     * where applicable
     */
    public static final String VARIABLE_BACKGROUND_ALBUM_COVER = "bgCover";

    /**
     * Write only variable that can be used with setVariable to pass the position in the media (Long object) to the
     * native layer so the currently playing media title will be displayed in the lock screen
     * where applicable
     */
    public static final String VARIABLE_BACKGROUND_POSITION = "bgPosition";

    /**
     * Read only variable that can be used with getVariable to query whether the 
     * native layer supports displaying the currently playing media information 
     * in the lock screen. This will return null or Boolean.TRUE.
     */
    public static final String VARIABLE_BACKGROUND_SUPPORTED = "bgInfoSupported";
            
    /**
     * Starts playing the audio file
     */
    public void play();

    /**
     * Pauses the playback of the audio file
     */
    public void pause();
    
    /**
     * Optional call that allows the caller to prepare the upcoming media player. This is useful
     * when streaming multiple streams one after another.
     */
    public void prepare();
    
    /**
     * Stops the audio playback and cleans up the resources related to it immediately.
     */
    public void cleanup();

    /**
     * Returns the time in seconds in the audio file or -1 if not known
     *
     * @return time in milli seconds
     */
    public int getTime();

    /**
     * Sets the position in the audio file or doesn't effect if not supported 
     *
     * @param time in milli seconds
     */
    public void setTime(int time);

    /**
     * Returns the length in seconds of the audio file or -1 if not known
     *
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
     * @return true if playing
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
     * If Media supports native playing by calling to play() the video will start
     * playing in the native player in full screen.
     * @return  true if the player is in native mode
     */
    public boolean isNativePlayerMode();
        
    /**
     * Allows for platform specific enhancements for media playback
     * @param key the key to set to the media that is platform specific
     * @param value the value
     */
    public void setVariable(String key, Object value);
    
    /**
     * Allows querying platform specific information from the media object
     * @param key the key query
     * @return the value or null
     */
    public Object getVariable(String key);
}
