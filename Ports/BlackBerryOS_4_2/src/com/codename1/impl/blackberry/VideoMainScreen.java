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
package com.codename1.impl.blackberry;

import com.codename1.media.Media;
import com.codename1.ui.Component;
import com.codename1.ui.Label;
import javax.microedition.media.MediaException;
import javax.microedition.media.control.VideoControl;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.container.MainScreen;

/**
 *
 * @author Chen
 */
public class VideoMainScreen extends MainScreen implements Media {

    private final VideoControl videoControl;
    private final MMAPIPlayer player;
    private BlackBerryImplementation impl;
    
    public VideoMainScreen(MMAPIPlayer p, BlackBerryImplementation impl) {
        super(Manager.NO_VERTICAL_SCROLL);
        this.player = p;
        this.impl = impl;
        this.videoControl = (VideoControl) player.nativePlayer.getControl("VideoControl");

        if (this.videoControl != null) {
            try {
                // Initialize the field where the content of the camera shall be displayed.
                Field videoField = (Field) this.videoControl.initDisplayMode(VideoControl.USE_GUI_PRIMITIVE, "net.rim.device.api.ui.Field");
                add(videoField);
                addMenuItem(new MenuItem("Pause", 0, 100) {

                    public void run() {
                        player.pause();
                    }
                });
                addMenuItem(new MenuItem("Play", 0, 100) {

                    public void run() {
                        player.play();
                    }
                });
                
                // Display the video control.
                this.videoControl.setDisplayFullScreen(true);
                this.videoControl.setVisible(true);

            } catch (MediaException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void play() {
        impl.showNativeScreen(this);
        player.play();
    }

    public void pause() {
        player.pause();
    }

    public void cleanup() {
        player.cleanup();
    }

    public int getTime() {
        return player.getTime();
    }

    public void setTime(int time) {
        player.setTime(time);
    }

    public int getDuration() {
        return player.getDuration();
    }

    public void setVolume(int vol) {
        player.setVolume(vol);
    }

    public int getVolume() {
        return player.getVolume();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public Component getVideoComponent() {
        return new Label("Blackberry video playing only works in NativePlayerMode");
    }

    public boolean isVideo() {
        return true;
    }

    public boolean isFullScreen() {
        return true;
    }

    public void setFullScreen(boolean fullScreen) {
    }

    public void setNativePlayerMode(boolean nativePlayer) {
    }

    public boolean isNativePlayerMode() {
        return true;
    }

    protected boolean keyDown(int keycode, int time) {
        if (Keypad.key(keycode) == Keypad.KEY_ESCAPE) {
            cleanup();
            impl.confirmControlView();
            return true;
        }
        return super.keyDown(keycode, time);
    }
    
    
    
}
