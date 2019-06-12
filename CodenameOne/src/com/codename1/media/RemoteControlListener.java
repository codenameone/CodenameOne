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

/**
 * A base class that is meant to be overridden to implement functionality that
 * responds to the device's remote control for media playback.  This allows you
 * to tie into the media buttons on the lock screen for background media.
 * <p>Apps should implement their own subclass and register it with the app using
 * {@link MediaManager#setRemoteControlListener(com.codename1.media.RemoteControlListener) }</p>
 * @author shannah
 * @since 7.0
 */
public class RemoteControlListener {
    
    /**
     * Called when user presses play button on remote control.
     */
    public void play() {
        
    }
    
    /**
     * Called when user presses the pause button on remote control.
     */
    public void pause() {
        
    }
    
    
    /**
     * Called when user presses the toggle play/pause button on remote control.
     */
    public void togglePlayPause() {
        
    }
    
    /**
     * Called when user seeks to a position of the currently playing media 
     * on the remote control.
     * @param pos 
     */
    public void seekTo(long pos) {
        
    }
    
    /**
     * Called when user presses the "next" button on remote control.
     */
    public void skipToNext() {
        
    }
    
    /**
     * Called when user presses the "previous" button on remote control.
     */
    public void skipToPrevious() {
        
    }
    
    /**
     * Called when user presses the "Stop" button on remote control.
     */
    public void stop() {
        
    }
    
    /**
     * Called when user presses the "fast forward" button on remote control.
     */
    public void fastForward() {
        
    }
    
    /**
     * Called when user presses the "rewind" button on remote control.
     */
    public void rewind() {
        
    }
    
    /**
     * Is used by remote control to determine if the media is currently playing.
     * @return 
     */
    public boolean isPlaying() {
        return false;
    }
    
    /**
     * Is called when the user adjusts the volume on the remote control
     * @param leftVolume
     * @param rightVolume 
     */
    public void setVolume(float leftVolume, float rightVolume) {
        
    }
    
    /**
     * Should return the meta data about the currently playing media.
     * @return 
     */
    public MediaMetaData getMetaData() {
        return null;
    }
}
