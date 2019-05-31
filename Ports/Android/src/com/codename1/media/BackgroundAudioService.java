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


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;

import android.os.Bundle;
import android.os.ResultReceiver;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import android.text.TextUtils;

import com.codename1.ui.Display;


import java.util.List;

/**
 * A service used for communicating with the remote control (on the lock screen) for background
 * media.
 * @author shannah
 */

public class BackgroundAudioService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener  {

    public static final String COMMAND_EXAMPLE = "command_example";
    private static final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";
    private static BackgroundAudioService instance;
    public static BackgroundAudioService getInstance() {
        return instance;
    }


    //private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mMediaSessionCompat;

    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if( mMediaPlayer != null && mMediaPlayer.isPlaying() ) {
            if( RemoteControlCallback.isPlaying() ) {
                RemoteControlCallback.pause();
                initMediaSession();
                showPausedNotification();
            }
        }
    };



    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPlay() {
            //super.onPlay();
            if( !successfullyRetrievedAudioFocus() ) {
                return;
            }

            mMediaSessionCompat.setActive(true);
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);


            //mMediaPlayer.start();
            RemoteControlCallback.play();
            initMediaSessionMetadata();
            showPlayingNotification();
        }

        @Override
        public void onPause() {
            //super.onPause();

            if( RemoteControlCallback.isPlaying()) {
                //mMediaPlayer.pause();
                RemoteControlCallback.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                showPausedNotification();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            RemoteControlCallback.fastForward();
        }

        @Override
        public void onRewind() {
            super.onRewind();
            RemoteControlCallback.rewind();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            RemoteControlCallback.skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            RemoteControlCallback.skipToPrevious();
        }

        @Override
        public void onStop() {
            super.onStop();
            RemoteControlCallback.stop();
            initMediaSessionMetadata();
            showPausedNotification();
        }
        


        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
            if( COMMAND_EXAMPLE.equalsIgnoreCase(command) ) {
                //Custom command here
            }
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            RemoteControlCallback.seekTo(pos);
        }

    };

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        if( builder != null ) {
            startForeground(1, builder.build());
        }
        //initMediaPlayer();
        initMediaSession();
        initNoisyReceiver();
    }

    private void initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mNoisyReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
        unregisterReceiver(mNoisyReceiver);
        mMediaSessionCompat.release();
        NotificationManagerCompat.from(this).cancel(1);
        stopForeground(true);
    }

   

    void showPlayingNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
        NotificationCompat.Builder builder = MediaStyleHelper.from(BackgroundAudioService.this, mMediaSessionCompat);
        if( builder == null ) {
            return;
        }

        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "Prev", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));



        builder.setStyle(new MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this, PlaybackStateCompat.ACTION_STOP))
                .setMediaSession(mMediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(android.R.drawable.stat_notify_sync);
        Notification notification = builder.build();

        NotificationManagerCompat.from(BackgroundAudioService.this).notify(1, notification);
    }

    void showPausedNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mMediaSessionCompat);
        if( builder == null ) {
            return;
        }

        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "Prev", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));


        builder.setStyle(new MediaStyle().setShowActionsInCompactView(0, 1, 2).setMediaSession(mMediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(android.R.drawable.stat_notify_sync);
        Notification notification = builder.build();

        NotificationManagerCompat.from(this).notify(1, notification);
    }





    private void initMediaSession() {
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag", mediaButtonReceiver, null);

        mMediaSessionCompat.setCallback(mMediaSessionCallback);
        mMediaSessionCompat.setFlags( MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

        setSessionToken(mMediaSessionCompat.getSessionToken());
    }

    private void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mMediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
    }

    void initMediaSessionMetadata() {
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        //Notification icon in card
        MediaMetaData md = RemoteControlCallback.getMetaData();
        if (md == null) {
            
            return;
        }
        if (md.getDisplayIcon() != null) {
            Bitmap i = (Bitmap)md.getDisplayIcon().getImage();
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, i);
        }
        if (md.getAlbumArt() != null) {
            Bitmap i = (Bitmap)md.getDisplayIcon().getImage();
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, i);
        }

        //lock screen icon for pre lollipop
        if (md.getArt() != null) {
            Bitmap i = (Bitmap)md.getDisplayIcon().getImage();
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, i);
        }
        
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, md.getTitle());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, md.getSubtitle());
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, md.getTrackNumber());
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, md.getNumTracks());

        mMediaSessionCompat.setMetadata(metadataBuilder.build());
    }

    private boolean successfullyRetrievedAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
    }


    //Not important for general audio service, required for class
    //@Nullable
    @Override
    public BrowserRoot onGetRoot( String clientPackageName, int clientUid,  Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(Display.getInstance().getProperty("AppName", "CodenameOne"), null);
        }

        return null;
    }

    //Not important for general audio service, required for class
    @Override
    public void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch( focusChange ) {
            case AudioManager.AUDIOFOCUS_LOSS: {
                if( RemoteControlCallback.isPlaying() ) {
                    RemoteControlCallback.stop();
                    initMediaSessionMetadata();
                    showPausedNotification();
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                RemoteControlCallback.pause();
                initMediaSessionMetadata();
                showPausedNotification();
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                if( RemoteControlCallback.isPlaying() ) {
                    RemoteControlCallback.setVolume(0.3f, 0.3f);
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_GAIN: {
                
                if( !RemoteControlCallback.isPlaying() ) {
                    RemoteControlCallback.play();
                    initMediaSessionMetadata();
                    showPlayingNotification();
                }
                RemoteControlCallback.setVolume(1.0f, 1.0f);
                
                break;
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        
        /*
        if( mediaPlayer != null ) {
            mMediaPlayer.release();
        }
*/
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        return super.onStartCommand(intent, flags, startId);
    }
    
    static final String CHANNEL_ID = "media_playback_channel";

    @android.support.annotation.RequiresApi(Build.VERSION_CODES.O)
    private  void createChannel() {
        createChannelStatic(this);
    }



    @android.support.annotation.RequiresApi(Build.VERSION_CODES.O)
    static void createChannelStatic(Context ctx) {
        NotificationManager
                mNotificationManager =
                (NotificationManager) ctx
                        .getSystemService(Context.NOTIFICATION_SERVICE);
        // The id of the channel.
        String id = CHANNEL_ID;
        // The user-visible name of the channel.
        CharSequence name = "Media playback";
        // The user-visible description of the channel.
        String description = "Media playback controls";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);
    }
}