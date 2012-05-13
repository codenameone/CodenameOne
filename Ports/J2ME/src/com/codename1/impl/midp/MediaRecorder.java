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
package com.codename1.impl.midp;

import com.codename1.io.FileSystemStorage;
import com.codename1.media.Media;
import com.codename1.ui.Component;
import java.io.IOException;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.RecordControl;

/**
 *
 * @author Chen
 */
public class MediaRecorder implements Media {

    private Player recorder;
    private RecordControl rc;
    private boolean isPlaying = false;

    public MediaRecorder(String path) throws IOException {
        try {
            String [] supportedContentType = Manager.getSupportedContentTypes("capture");
            boolean amrSupported = false;
            for (int i = 0; i < supportedContentType.length; i++) {
                if(supportedContentType[i].equals("audio/amr")){
                    amrSupported = true;
                }
            }
            if(amrSupported){
                try {
                    //some j2me devices will report they supports amr, but they are actually 
                    //don't so we will try to realize the player and if fails the
                    //fallback would be to create it with the default capture encoding
                    recorder = Manager.createPlayer("capture://audio?encoding=audio/amr");                                
                    recorder.realize();
                } catch (Exception e) {
                    recorder = Manager.createPlayer("capture://audio");
                    recorder.realize();
                }
            }else{
                recorder = Manager.createPlayer("capture://audio");
                recorder.realize();
            }
            rc = (RecordControl) recorder.getControl("RecordControl");
            rc.setRecordStream(FileSystemStorage.getInstance().openOutputStream(path));
        } catch (MediaException ex) {
            ex.printStackTrace();
        }
    }

    public void play() {
        try {
            rc.startRecord();
            recorder.start();
        } catch (MediaException ex) {
            ex.printStackTrace();
        }
    }

    public void pause() {
         try {
            rc.stopRecord();
            recorder.stop();
        } catch (MediaException ex) {
            ex.printStackTrace();
        }
    }

    public void cleanup() {
         try {
            rc.commit();
            recorder.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public int getTime() {
        return -1;
    }

    public void setTime(int time) {
    }

    public int getDuration() {
        return -1;
    }

    public void setVolume(int vol) {
    }

    public int getVolume() {
        return -1;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Component getVideoComponent() {
        return null;
    }

    public boolean isVideo() {
        return false;
    }

    public boolean isFullScreen() {
        return false;
    }

    public void setFullScreen(boolean fullScreen) {
    }

    public void setNativePlayerMode(boolean nativePlayer) {
    }

    public boolean isNativePlayerMode() {
        return false;
    }
}
