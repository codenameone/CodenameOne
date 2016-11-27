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

import android.app.Activity;
import android.content.Context;
import static android.content.Context.TELEPHONY_SERVICE;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import java.io.InputStream;
import java.util.Vector;

/**
 *
 * @author Chen
 */
public class Audio implements Runnable, com.codename1.media.Media, MediaPlayer.OnInfoListener, AudioManager.OnAudioFocusChangeListener {
    private static final int MEDIA_INFO_BUFFERING_START = 701;
    private static final int MEDIA_INFO_BUFFERING_END = 702;
    private MediaPlayer player;
    private Runnable onComplete;
    private InputStream stream;
    private int lastTime;
    private int lastDuration;
    private Activity activity;
    private boolean buffering;
    private boolean disposeOnComplete = true;
    private int tempVolume = -1;
    
    private static Vector currentPlayingAudio = new Vector();
    private static PhoneStateListener phoneStateListener;
    
    public Audio(Activity activity, MediaPlayer player, InputStream stream, Runnable onComplete) {
        this.activity = activity;
        this.player = player;
        this.stream = stream;
        this.onComplete = onComplete;
        bindPlayerCleanupOnComplete();

        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
    }

    private void cleanVars() {
        if (player != null) {
            try {
                player.release();
            } catch (Throwable t) {
            }
            player = null;
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable t) {
                }
                stream = null;
            }
            System.gc();
        }
    }

    public void run() {
        if (player != null) {
            cleanup();
            cleanVars();
        }
    }

    private void bindPlayerCleanupOnComplete() {
        if(player == null) {
            return;
        }
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            public void onCompletion(MediaPlayer arg0) {
                if(disposeOnComplete){
                    run();
                }
                if (onComplete != null) {
                    Display.getInstance().callSerially(onComplete);
                }
                
            }
        });
        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
                run();
                return true;
            }
        });
    }

    @Override
    public void cleanup() {
        try {
            if (player != null) {
                if (player.isPlaying()) {
                    player.stop();
                }
                cleanVars();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void prepare() {
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            @Override
            public void run() {
                try {
                    if (player != null) {
                        player.prepare();
                    }
                } catch(Throwable t) {
                    // some exceptions might occur here, with all the various illegal states they rarely matter
                    t.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void play() {
        try {
            if (player != null) {
                player.start();
            }
        } catch(Throwable t) {
            // some exceptions might occur here, with all the various illegal states they rarely matter
            t.printStackTrace();
        }
    }

    @Override
    public void pause() {
        try {
            if (player != null) {
                player.pause();
            }
        } catch(Throwable t) {
            // some exceptions might occur here, with all the various illegal states they rarely matter
            t.printStackTrace();
        }
    }

    @Override
    public int getTime() {
        if (player == null) {
            return lastTime;
        }
        try {
            lastTime = player.getCurrentPosition();
            return lastTime;
        } catch (IllegalStateException err) {
            // no idea???
            //err.printStackTrace();
            return lastTime;
        }
    }

    @Override
    public void setTime(int time) {
        try {
            if (player == null) {
                return;
            }
            final boolean[] flag = new boolean[1];
            player.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {

                public void onSeekComplete(MediaPlayer arg0) {
                    flag[0] = true;
                }
            });
            if (player.isPlaying()) {
                player.seekTo(time);
            } else {
                player.start();
                player.seekTo(time);
                player.pause();
            }
        } catch(Throwable t) {
            // some exceptions might occur here, with all the various illegal states they rarely matter
            t.printStackTrace();
        }
    }

    @Override
    public int getDuration() {
        if (player == null) {
            return lastDuration;
        }
        try {
            int d = player.getDuration();
            if (d == 0) {
                return -1;
            }
            lastDuration = d;
        } catch (IllegalStateException err) {
            return -1;
        }
        return lastDuration;
    }

    @Override
    public void setVolume(int vol) {
        float v = ((float) vol) / 100.0F;
        if (player != null) {
            player.setVolume(v, v);
        }
    }

    @Override
    public int getVolume() {
        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        return am.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean isVideo() {
        return false;
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void setFullScreen(boolean fullScreen) {
    }

    @Override
    public Component getVideoComponent() {
        return null;
    }

    @Override
    public void setNativePlayerMode(boolean nativePlayer) {
    }

    @Override
    public boolean isNativePlayerMode() {
        return false;
    }

    @Override
    public boolean isPlaying() {
        try {
            return player != null && player.isPlaying() && !buffering;
        } catch(Exception err) {
            return false;
        }
    }

    public void setVariable(String key, Object value) {
        if(key != null){
            if(key.equals("disposeOnComplete")){
                if(value != null){
                    String v= value.toString();
                    disposeOnComplete = v.equalsIgnoreCase("true");
                }
            }
        }
    }

    public Object getVariable(String key) {
        if(key != null){
            if(key.equals("disposeOnComplete")){
                return "" + disposeOnComplete;
            }
        }
        return null;
    }

    /**
     * Allows us to detect buffering of media to return a better result in playback
     */
    @Override
    public boolean onInfo(MediaPlayer mp, int i, int i1) {
        switch(i) {
            case MEDIA_INFO_BUFFERING_START:
                buffering = true;
                break;
            case MEDIA_INFO_BUFFERING_END:
                buffering = false;
                break;
        } 
        return false;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (!isPlaying() && player != null) {
                    player.start();
                    if(tempVolume > -1) {
                        setVolume(tempVolume);
                        tempVolume = -1;
                    }
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                cleanup();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.d("CN1", "AUDIOFOCUS_LOSS_TRANSIENT");
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (isPlaying()) {
                    pause();
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (isPlaying()) {
                    tempVolume = getVolume();
                    setVolume(10);
                }
                break;
        }
    }
}
