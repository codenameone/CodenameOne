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
package com.codename1.impl.android;

import android.app.Activity;
import android.content.Context;
import static android.content.Context.TELEPHONY_SERVICE;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import java.io.InputStream;
import java.util.Vector;

/**
 *
 * @author Chen
 */
class Audio implements Runnable, com.codename1.media.Media, MediaPlayer.OnInfoListener {
    private static final int MEDIA_INFO_BUFFERING_START = 701;
    private static final int MEDIA_INFO_BUFFERING_END = 702;
    private MediaPlayer player;
    private Runnable onComplete;
    private InputStream stream;
    private int lastTime;
    private int lastDuration;
    private Activity activity;
    private boolean buffering;

    private static Vector currentPlayingAudio = new Vector();
    private static PhoneStateListener phoneStateListener;
    
    public Audio(Activity activity, MediaPlayer player, InputStream stream, Runnable onComplete) {
        this.activity = activity;
        this.player = player;
        this.stream = stream;
        this.onComplete = onComplete;
        bindPlayerCleanupOnComplete();
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
            if (onComplete != null) {
                Display.getInstance().callSerially(onComplete);
                onComplete = null;
            }
            removeFromCurrentPlaying();
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
                run();
            }
        });
        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
                run();
                return false;
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
        try {
            if (player != null) {
                player.prepareAsync();
            }
        } catch(Throwable t) {
            // some exceptions might occur here, with all the various illegal states they rarely matter
            t.printStackTrace();
        }
    }
    
    @Override
    public void play() {
        try {
            if (player != null) {
                player.start();
                addToCurrentPlaying();
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
                removeFromCurrentPlaying();
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
    }

    public Object getVariable(String key) {
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
    private void addToCurrentPlaying() {
        if (currentPlayingAudio.size() == 0) {
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.post(new Runnable() {

                public void run() {
                    try {
                        TelephonyManager mgr = (TelephonyManager) activity.getSystemService(TELEPHONY_SERVICE);
                        if (mgr != null) {
                            phoneStateListener = new PhoneStateListener() {

                                @Override
                                public void onCallStateChanged(int state, String incomingNumber) {
                                    try {
                                        if (state == TelephonyManager.CALL_STATE_RINGING) {
                                            //Incoming call: Pause music
                                            for (int i = 0; i < currentPlayingAudio.size(); i++) {
                                                Audio m = (Audio) currentPlayingAudio.elementAt(i);
                                                if (m.isPlaying() && m.player != null) {
                                                    m.player.pause();
                                                }
                                            }
                                        } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                                            //Not in call: Play music
                                            for (int i = 0; i < currentPlayingAudio.size(); i++) {
                                                Audio m = (Audio) currentPlayingAudio.elementAt(i);
                                                if (!m.isPlaying() && m.player != null) {
                                                    m.player.start();
                                                }
                                            }
                                        } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                            //A call is dialing, active or on hold
                                            for (int i = 0; i < currentPlayingAudio.size(); i++) {
                                                Audio m = (Audio) currentPlayingAudio.elementAt(i);
                                                if (m.isPlaying() && m.player != null) {
                                                    m.player.pause();
                                                }
                                            }
                                        }
                                        super.onCallStateChanged(state, incomingNumber);

                                    } catch (Throwable t) {
                                        // some exceptions might occur here, with all the various illegal states they rarely matter
                                        t.printStackTrace();
                                    }
                                }

                            };
                            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        }
        currentPlayingAudio.add(this);
    }

    private void removeFromCurrentPlaying() {
        currentPlayingAudio.remove(this);
        if (currentPlayingAudio.size() == 0) {
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.post(new Runnable() {

                public void run() {
                    TelephonyManager mgr = (TelephonyManager) activity.getSystemService(TELEPHONY_SERVICE);
                    if (mgr != null) {
                        mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                    }
                }
            });
        }
    }

}
