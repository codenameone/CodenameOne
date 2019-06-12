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

import com.codename1.ui.Image;

/**
 * MetaData for use by {@link RemoteControlListener} to provide information about
 * the currently playing background media on the device's lock screen.
 * @author shannah
 * @since 7.0
 */
public class MediaMetaData {

    /**
     * Gets the media title.
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the media title.
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the media subtitle.
     * @return the subtitle
     */
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * Sets the media subtitle.
     * @param subtitle the subtitle to set
     */
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    /**
     * Gets the media track number.
     * @return the trackNumber
     */
    public int getTrackNumber() {
        return trackNumber;
    }

    /**
     * Sets the media track number.
     * @param trackNumber the trackNumber to set
     */
    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    /**
     * Gets the current number of tracks in the current play list.
     * @return the numTracks
     */
    public int getNumTracks() {
        return numTracks;
    }

    /**
     * Sets the current number of tracks in the current play list.
     * @param numTracks the numTracks to set
     */
    public void setNumTracks(int numTracks) {
        this.numTracks = numTracks;
    }

    /**
     * Gets the display icon for the media.
     * @return the displayIcon
     */
    public Image getDisplayIcon() {
        return displayIcon;
    }

    /**
     * Sets the display icon for the media.
     * @param displayIcon the displayIcon to set
     */
    public void setDisplayIcon(Image displayIcon) {
        this.displayIcon = displayIcon;
    }

    /**
     * Gets the album art for the media.
     * @return the albumArt
     */
    public Image getAlbumArt() {
        return albumArt;
    }

    /**
     * Sets the album art for the media.
     * @param albumArt the albumArt to set
     */
    public void setAlbumArt(Image albumArt) {
        this.albumArt = albumArt;
    }

    /**
     * Gets the art for the current media.
     * @return the art
     */
    public Image getArt() {
        return art;
    }

    /**
     * Sets the art for the current media.
     * @param art the art to set
     */
    public void setArt(Image art) {
        this.art = art;
    }
    private String title, subtitle;
    private int trackNumber, numTracks;
    private Image displayIcon, albumArt, art;
}
