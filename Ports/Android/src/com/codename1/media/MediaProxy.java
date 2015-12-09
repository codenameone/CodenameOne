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
 *
 * @author Chen
 */
public class MediaProxy implements Media{

    private Media media;
    
    public MediaProxy(Media m) {
        this.media = m;
    }
    
    
    @Override
    public void play() {
        media.play();
    }

    @Override
    public void pause() {
        media.pause();
    }

    @Override
    public void prepare() {
        media.prepare();
    }

    @Override
    public void cleanup() {
        media.cleanup();
    }

    @Override
    public int getTime() {
        return media.getTime();
    }

    @Override
    public void setTime(int time) {
        media.setTime(time);
    }

    @Override
    public int getDuration() {
        return media.getDuration();
    }

    @Override
    public void setVolume(int vol) {
        media.setVolume(vol);
    }

    @Override
    public int getVolume() {
        return media.getVolume();
    }

    @Override
    public boolean isPlaying() {
        return media.isPlaying();
    }

    @Override
    public Component getVideoComponent() {
        return media.getVideoComponent();
    }

    @Override
    public boolean isVideo() {
        return media.isVideo();
    }

    @Override
    public boolean isFullScreen() {
        return media.isFullScreen();
    }

    @Override
    public void setFullScreen(boolean fullScreen) {
        media.setFullScreen(fullScreen);
    }

    @Override
    public void setNativePlayerMode(boolean nativePlayer) {
        media.setNativePlayerMode(nativePlayer);
    }

    @Override
    public boolean isNativePlayerMode() {
        return media.isNativePlayerMode();
    }

    @Override
    public void setVariable(String key, Object value) {
        media.setVariable(key, value);
    }

    @Override
    public Object getVariable(String key) {
        return media.getVariable(key);
    }
    
}
