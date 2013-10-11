/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.android;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import com.codename1.ui.Component;
import java.io.InputStream;

/**
 *
 * @author Chen
 */
class Audio implements Runnable, com.codename1.media.Media {
    private MediaPlayer player;
    private Runnable onComplete;
    private InputStream stream;
    private int lastTime;
    private int lastDuration;
    private Activity activity;
    
   
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
                onComplete.run();
                onComplete = null;
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
    public void play() {
        if (player != null) {
            player.start();
        }
    }

    @Override
    public void pause() {
        if (player != null) {
            player.pause();
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
    }

    @Override
    public int getDuration() {
        if (player == null) {
            return lastDuration;
        }
        int d = player.getDuration();
        if(d == 0){
            return -1;
        }
        lastDuration = d;
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
        return player != null && player.isPlaying();
    }

    public void setVariable(String key, Object value) {
    }

    public Object getVariable(String key) {
        return null;
    }
}
