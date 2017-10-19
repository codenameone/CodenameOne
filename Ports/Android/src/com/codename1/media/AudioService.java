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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.codename1.ui.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class is used when the media is been requested to run on the background
 *
 * @author Chen
 */
public class AudioService extends Service  {

    private IBinder mBinder = new LocalBinder();
    java.util.Map<Integer,BackgroundMedia> backgroundMedia = new java.util.HashMap<Integer, BackgroundMedia>();
    @Override
    public void onCreate() {
        Log.d("CN1", "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String mediaLink = intent.getExtras().getString("mediaLink");
        int mediaId = intent.getExtras().getInt("mediaId");
        backgroundMedia.put(mediaId, new BackgroundMedia(mediaId, mediaLink));
        return START_STICKY;
    }

    public Media getMedia(int mediaId) {
        return backgroundMedia.get(mediaId);
    }

    @Override
    public void onDestroy() {
        for (BackgroundMedia m : backgroundMedia.values()) {
            try {
                m.cleanup();
            } catch (Throwable t) {
            }
        }

        super.onDestroy();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



    public class LocalBinder extends Binder {

        public AudioService getService() {
            return AudioService.this;
        }
    }

    class BackgroundMedia implements Media, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
            MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener {

        private MediaPlayer mediaPlayer = new MediaPlayer();
        private String mediaLink;
        private File tempFile;
        private int mediaId;

        BackgroundMedia(int mediaId, String mediaLink) {
            this.mediaId = mediaId;
            this.mediaLink = mediaLink;
            Log.d("CN1", mediaLink);
            Uri uri = null;
            InputStream is = null;
            if (mediaLink.startsWith("file://")) {
                mediaLink = mediaLink.substring(7);
                if (mediaLink.indexOf(':') < 0) {
                    File file = new File(mediaLink);
                    uri = Uri.fromFile(file);
                }
            } else if (mediaLink.startsWith("jar://")) {
                mediaLink = mediaLink.substring(6);
                if (mediaLink.startsWith("/")) {
                    mediaLink = mediaLink.substring(1);
                }
                try {
                    is = getAssets().open(mediaLink);
                } catch (IOException ex) {
                    Log.e("CN1", "error", ex);
                }

            } else {
                uri = Uri.parse(mediaLink);
            }

            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
            mediaPlayer.setOnInfoListener(this);
            try {
                if (uri != null) {
                    mediaPlayer.setDataSource(AudioService.this, uri);
                } else if (is != null) {
                    tempFile = File.createTempFile("mtmp", "dat");
                    tempFile.deleteOnExit();
                    OutputStream out = new FileOutputStream(tempFile);

                    byte buf[] = new byte[256];
                    int len = 0;
                    while ((len = is.read(buf, 0, buf.length)) > -1) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                    is.close();
                    mediaPlayer.setDataSource(new FileInputStream(tempFile).getFD());
                }
                mediaPlayer.prepare();
            } catch (Exception ex) {
                Log.e("CN1", "error", ex);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            pause();
            if (tempFile != null) {
                tempFile.delete();
            }
            backgroundMedia.remove(mediaId);
            if (backgroundMedia.isEmpty()) {
                stopSelf();
            }
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                    Toast.makeText(AudioService.this,
                            "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra,
                            Toast.LENGTH_SHORT).show();
                    break;
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    Toast.makeText(AudioService.this, "MEDIA ERROR SERVER DIED " + extra,
                            Toast.LENGTH_SHORT).show();
                    break;
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    Toast.makeText(AudioService.this, "MEDIA ERROR UNKNOWN " + extra,
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }

        @Override
        public void onSeekComplete(MediaPlayer mp) {

        }

        @Override
        public boolean onInfo(MediaPlayer mp, int i, int i1) {
            return false;
        }

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int i) {
        }

        public void play() {
            for (BackgroundMedia bm : backgroundMedia.values()) {
                try {
                    if (bm != this && bm.isPlaying()) {
                        bm.pause();
                    }
                } catch (Throwable t){}
            }
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        }

        @Override
        public void pause() {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        }

        @Override
        public void prepare() {
            mediaPlayer.prepareAsync();
        }

        @Override
        public void cleanup() {
            backgroundMedia.remove(mediaId);
            pause();
            mediaPlayer.release();

        }

        @Override
        public int getTime() {
            return mediaPlayer.getCurrentPosition();
        }

        @Override
        public void setTime(int time) {
            mediaPlayer.seekTo(time);
        }

        @Override
        public int getDuration() {
            return mediaPlayer.getDuration();
        }

        @Override
        public void setVolume(int vol) {
            float v = ((float) vol) / 100.0F;
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(v, v);
            }
        }

        @Override
        public int getVolume() {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            return am.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        @Override
        public boolean isPlaying() {
            return mediaPlayer.isPlaying();
        }

        @Override
        public Component getVideoComponent() {
            return null;
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
        public void setNativePlayerMode(boolean nativePlayer) {
        }

        @Override
        public boolean isNativePlayerMode() {
            return true;
        }

        @Override
        public void setVariable(String key, Object value) {
        }

        @Override
        public Object getVariable(String key) {
            return null;
        }
    }


}
