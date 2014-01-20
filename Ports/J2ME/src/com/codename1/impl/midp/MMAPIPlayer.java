/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl.midp;

import com.codename1.media.Media;
import com.codename1.ui.Component;
import com.codename1.ui.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

/**
 * Simple abstraction to the player API in MMAPI used by the MIDP and Blackberry ports.
 * This class is public only because the blackberry port relies on it and is in a different
 * package, it is not meant for general use and is an implementation detail subject to change!
 * 
 * this class might be changed at any moment it is an implementation detail
 * @author Shai Almog
 */
public class MMAPIPlayer implements PlayerListener, Media{
    private static int volume = -1;
    private boolean deleted;
    private int lastTime;
    Player nativePlayer;
    private static Vector playing;
    private InputStream sourceStream;
    private Runnable onComplete;

    private MMAPIPlayer(Player p) {
        this.nativePlayer = p;
        if(volume > -1){
            setVolume(volume);
        }else{
            setVolume(100);
        }
    }

    public int getVolume() {
        if(volume > -1) {
            return volume;
        }
        synchronized(MMAPIPlayer.class) {
            if(playing != null && playing.size() > 0) {
                MMAPIPlayer current = (MMAPIPlayer)playing.elementAt(0);
                VolumeControl volc = (VolumeControl) current.nativePlayer.getControl("VolumeControl");
                if(volc != null) {
                    return volc.getLevel();
                }
            }
        }
        return -1;
    }

    public void setVolume(int v) {
        volume = v;
        if(playing != null) {
            synchronized(MMAPIPlayer.class) {
                for(int iter = 0 ; iter < playing.size() ; iter++) {
                    MMAPIPlayer current = (MMAPIPlayer)playing.elementAt(iter);
                    VolumeControl volc = (VolumeControl) current.nativePlayer.getControl("VolumeControl");
                    if(volc != null) {
                        volc.setLevel(v);
                    }
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    public static MMAPIPlayer createPlayer(String uri, Runnable onCompletion) throws IOException {
        try {
            Player p = Manager.createPlayer((String)uri);
            p.realize();
            MMAPIPlayer m = new MMAPIPlayer(p);
            m.bindPlayerCleanupOnComplete(p, null, onCompletion);
            return m;
        } catch (MediaException ex) {
            ex.printStackTrace();
            throw new IOException(ex.toString());
        }
    }

    public static MMAPIPlayer createPlayer(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        try {
            Player p = Manager.createPlayer(stream, mimeType);
            p.realize();
            MMAPIPlayer m = new MMAPIPlayer(p);
            m.bindPlayerCleanupOnComplete(p, stream, onCompletion);
            return m;
        } catch (MediaException ex) {
            if("audio/mpeg".equals(mimeType)) {
                return createPlayer(stream, "audio/mp3", onCompletion);
            }

            ex.printStackTrace();
            throw new IOException(ex.toString());
        }
    }

    private void bindPlayerCleanupOnComplete(final Player p, final InputStream i, final Runnable onComplete) {
        if(volume > -1) {
            VolumeControl v = (VolumeControl) p.getControl("VolumeControl");
            if(v != null) {
                v.setLevel(volume);
            }
        }
        sourceStream = i;
        this.onComplete = onComplete;
        p.addPlayerListener(this);
    }


    public void cleanup() {
        if(deleted) {
            return;
        }
        deleted = true;
        try {
            synchronized(MMAPIPlayer.class) {
                playing.removeElement(this);
            }
            try {
                nativePlayer.stop();
            } catch(Throwable t) {}
            nativePlayer.close();
            nativePlayer = null;
        } catch(Throwable t) {}
    }

    public void prepare() {
        if(deleted){
            return;
        }
        try {
            nativePlayer.prefetch();
        } catch (MediaException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.toString());
        }
    }
    
    public void play() {
        if(deleted){
            return;
        }
        try {
            if(playing == null) {
                playing = new Vector();
            }
            synchronized(MMAPIPlayer.class) {
                playing.addElement(this);
            }
            nativePlayer.start();
        } catch (MediaException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.toString());
        }
    }

    public void pause() {
        if(deleted){
            return;
        }
        try {
            if(nativePlayer != null) {
                nativePlayer.stop();
            }
        } catch (MediaException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.toString());
        }
    }

    public int getTime() {
        try {
            // this allows us to get the time even on a closed player
            if(nativePlayer == null || deleted) {
                return lastTime;
            }
            lastTime = (int)(nativePlayer.getMediaTime() / 1000);
            return lastTime;
        } catch(Throwable t) {
            return lastTime;
        }
    }

    public void setTime(int time) {
        if(deleted){
            return;
        }
        try {
            nativePlayer.setMediaTime(time * 1000);
        } catch (MediaException ex) {
            ex.printStackTrace();
        }
    }

    public int getDuration() {
        if(nativePlayer == null || deleted) {
            return 1000;
        }
        return (int)(nativePlayer.getDuration() / 1000);
    }

    public void playerUpdate(Player player, String event, Object eventData) {
        if(deleted) {
            return;
        }
        if(PlayerListener.END_OF_MEDIA.equals(event) || PlayerListener.ERROR.equals(event)) {
            lastTime = (int)(nativePlayer.getMediaTime() / 1000);
            cleanup();
            if(sourceStream != null) {
                try {
                    sourceStream.close();
                } catch(Throwable t) {}
            }
            if(onComplete != null) {
                onComplete.run();
            }
        }
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

    public boolean isPlaying() {
        return nativePlayer.getState() == Player.STARTED;
    }
    
    public void setVariable(String key, Object value) {
    }

    public Object getVariable(String key) {
        return null;
    }
}
