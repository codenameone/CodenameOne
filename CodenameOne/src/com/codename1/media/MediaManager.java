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

import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.AsyncResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>
 * Allow us to create {@ling com.codename1.media.Media} objects using String URI's or with a stream to 
 * the media data.<br>
 * Notice that the underlying platforms contains the actual codecs, therefore 
 * you need to play common media file types such as mp3, mp4 to successfully play them across devices
 * on all target platforms. The simulator can't accurately reproduce the behavior of this class in all devices/cases.
 * </p>
 * <p>
 * The sample code below demonstrates simple video playback.
 * </p>
 * 
 * <script src="https://gist.github.com/codenameone/fb73f5d47443052f8956.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-mediaplayer.png" alt="Media player sample" />
 * 
 * <p>
 *     The code below demonstrates capturing audio using the Capture API and playing back audio files using the Media API:
 * </p>
 * <script src="https://gist.github.com/codenameone/a347dc9dcadaa759d0cb.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/capture-audio.png" alt="Captured recordings in the demo" />
 *
 * <p>
 *     The code below demonstrates capturing audio and playing back audio using the Media, MediaManager and MediaRecorderBuilder APIs,
 *     as alternative and more customizable approach than using the Capture API:
 * </p>
 * <script src="https://gist.github.com/jsfan3/419f44a9ad49d8fc1c1e3e325d1e5422.js"></script>
 * <img src="https://user-images.githubusercontent.com/1997316/78480286-02131b00-7735-11ea-8a70-5ca5512e7d92.png" alt="Demonstrates capturing of audio files and their playback using the Codename One APIs Media, MediaManager and MediaRecorderBuilder" />
 * 
 */
public class MediaManager {
    
    /**
     * A static map of audio buffers.  These can be used to register an Audio buffer to receive
     * raw PCM data from the microphone.
     * @since 7.0
     */
    private static Map<String, AudioBuffer> audioBuffers = new HashMap<String, AudioBuffer>();
    private static RemoteControlListener remoteControlListener;
    
    /**
     * Gets an audio buffer at the given path.
     * @param path The path to the Audio buffer.  This path doesn't correspond to a real file.  It is just
     * used as a key to map to the audio buffer so that it can be addressed.
     * @return The AudioBuffer or null if no buffer exists at that path.
     * @since 7.0
     */
    public static AudioBuffer getAudioBuffer(String path) {
        return getAudioBuffer(path, false, 256);
    }
    
    /**
     * Gets or creates an audio buffer at the given path.
     * @param path The path to the Audio buffer.  This path doesn't correspond to a real file.  It is just
     * used as a key to map to the audio buffer so that it can be addressed.
     * @param create If this flag is {@literal true} and no buffer exists at the given path,
     * then the buffer will be created.
     * @param size The maximum size of the buffer.
     * @return The audio buffer or null if no buffer exists at that path and the {@literal create} flag is {@literal false}.
     * @since 7.0
     */
    public static AudioBuffer getAudioBuffer(String path, boolean create, int size) {
        AudioBuffer buf = null;
        if (create && !audioBuffers.containsKey(path)) {
            buf = new AudioBuffer(size);
            audioBuffers.put(path, buf);
        }
        
        buf = audioBuffers.get(path);
        buf.retain();
        return buf;
        
    }
    
    /**
     * Releases an audio buffer at a given path.  Audio buffers use a simple reference counter
     * mechanism.  Every call to {@link #getAudioBuffer(java.lang.String, boolean, int) } will increment
     * the counter, and calls to {@link #releaseAudioBuffer(java.lang.String) } will decrement the counter.
     * @param path The path to the buffer.
     * @since 7.0
     */
    public static void releaseAudioBuffer(String path) {
        AudioBuffer buf = audioBuffers.get(path);
        if (buf != null) {
            int refCount = buf.release();
            if (refCount <= 0) {
                audioBuffers.remove(path);
            }
        }
    }
    /**
     * Deletes the audio buffer at the given path.
     * @param path The path to the audio buffer to delete.
     * @since 7.0
     * @deprecated Prefer to use {@link #releaseAudioBuffer(java.lang.String) }
     */
    public static void deleteAudioBuffer(String path) {
       audioBuffers.remove(path);
    }
    
    
    
    /**
     * Registers a listener to be notified of remote control events - e.g.
     * the play/pause/seek buttons on the user's lock screen when background
     * media is being played.
     * @param l The remote control listener to set.  null to set no listener.
     * @since 7.0
     */
    public static void setRemoteControlListener(RemoteControlListener l) {
        boolean shouldStop = remoteControlListener != null && l == null;
        if (shouldStop) {
                Display.getInstance().stopRemoteControl();
        }
        boolean shouldStart = remoteControlListener == null && l != null;
        remoteControlListener = l;
        if (shouldStart) {
            Display.getInstance().startRemoteControl();
        }
        
    }
    
    /**
     * Gets the currently registered remote control listener.
     * @return The currently registered remote control listener, or null if 
     * none is registered.
     * @since 7.0
     */
    public static RemoteControlListener getRemoteControlListener() {
        return remoteControlListener;
    }

    /**
     * Creates an audio media that can be played in the background.
     * 
     * @param uri the uri of the media can start with jar://, file://, http:// 
     * (can also use rtsp:// if supported on the platform)
     * 
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * 
     * @throws IOException if creation of media from the given URI has failed
     */ 
    public static Media createBackgroundMedia(String uri) throws IOException {
        return Display.getInstance().createBackgroundMedia(uri);
    }
    
    /**
     * Creates an audio media asynchronously that can be played in the background.
     * 
     * @param uri the uri of the media can start with jar://, file://, http:// 
     * (can also use rtsp:// if supported on the platform)
     * 
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * 
     * @since 7.0
     */ 
    public static AsyncResource<Media> createBackgroundMediaAsync(String uri) {
        return Display.getInstance().createBackgroundMediaAsync(uri);
    }
    
    /**
     * Creates a Media from a given URI
     * 
     * @param uri the uri of the media can start with file://, http:// (can also
     * use rtsp:// although may not be supported on all target platforms)
     * @param isVideo a boolean flag to indicate if this is a video media
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * @throws IOException if creation of media from the given URI has failed
     */
    public static Media createMedia(String uri, boolean isVideo) throws IOException {
        return createMedia(uri, isVideo, null);
    }

    /**
     * Creates the Media in the given stream.
     * Notice that you should invoke cleanup on a media once you are done with it.
     *
     * @param stream the stream containing the media data
     * @param mimeType the type of the data in the stream
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * @throws java.io.IOException if the creation of the Media has failed
     */
    public static Media createMedia(InputStream stream, String mimeType) throws IOException {
        return createMedia(stream, mimeType, null);
    }
    
    /**
     * Creates the Media in the given stream asynchronously.
     * Notice that you should invoke cleanup on a media once you are done with it.
     *
     * @param stream the stream containing the media data
     * @param mimeType the type of the data in the stream
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * @since 7.0
     */
    public static AsyncResource<Media> createMediaAsync(InputStream stream, String mimeType, Runnable onCompletion) {
        return Display.getInstance().createMediaAsync(stream, mimeType, onCompletion);
    }

    /**
     * Creates a Media from a given URI
     * 
     * @param uri the uri of the media can start with file://, http:// (can also
     * use rtsp:// although may not be supported on all target platforms)
     * @param isVideo a boolean flag to indicate if this is a video media
     * @param onCompletion a Runnable to be called when the media has finished
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * @throws IOException if creation of media from given URI failed
     */
    public static Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {
        return Display.getInstance().createMedia(uri, isVideo, onCompletion);
    }
    
    /**
     * Creates a Media from a given URI asynchronously.
     * 
     * @param uri the uri of the media can start with file://, http:// (can also
     * use rtsp:// although may not be supported on all target platforms)
     * @param isVideo a boolean flag to indicate if this is a video media
     * @param onCompletion a Runnable to be called when the media has finished
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * @since 7.0
     */
    public static AsyncResource<Media> createMediaAsync(String uri, boolean isVideo, Runnable onCompletion) {
        return Display.getInstance().createMediaAsync(uri, isVideo, onCompletion);
    }
    
    /**
     * Adds a callback to a Media element that will be called when the media finishes playing.
     * 
     * @param media The media to add the callback to.
     * @param onCompletion The callback that will run on the EDT when the playback completes.
     * @see #removeCompletionHandler(com.codename1.media.Media, java.lang.Runnable) 
     */
    public static void addCompletionHandler(Media media, Runnable onCompletion) {
        Display.getInstance().addCompletionHandler(media, onCompletion);
    }
    
    /**
     * Removes onComplete callback from Media element.
     * @param media The media element.
     * @param onCompletion The callback.
     * @see #addCompletionHandler(com.codename1.media.Media, java.lang.Runnable) 
     */
    public static void removeCompletionHandler(Media media, Runnable onCompletion) {
        Display.getInstance().removeCompletionHandler(media, onCompletion);
    }

    /**
     * Creates the Media in the given stream
     * Notice that you should invoke cleanup on a media once you are done with it.
     *
     * @param stream the stream containing the media data
     * @param mimeType the type of the data in the stream
     * @param onCompletion a Runnable to be called when the media has finished
     * @return Media a Media Object that can be used to control the playback 
     * of the media
     * @throws java.io.IOException if the URI access fails
     */
    public static Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return Display.getInstance().createMedia(stream, mimeType, onCompletion);
    }
    
    /**
     * Creates a Media recorder Object which will record from the device mic to
     * a file in the given path.
     * The output format will be amr-nb if supported by the platform.
     * 
     * @param path a file path to where to store the recording, if the file does
     * not exists it will be created.
     * @deprecated see createMediaRecorder(String path, String mimeType) instead
     */
    public static Media createMediaRecorder(String path) throws IOException {
        return createMediaRecorder(path, getAvailableRecordingMimeTypes()[0]);
    }
    
    /**
     * Gets the recording mime type for the returned Media from the 
     * createMediaRecorder method
     * 
     * @return the recording mime type
     * @deprecated see getAvailableRecordingMimeTypes() instead
     */
    public static String getMediaRecorderingMimeType(){
        return Display.getInstance().getMediaRecorderingMimeType();
    }
    
    /**
     * Gets the available recording MimeTypes
     */ 
    public static String[] getAvailableRecordingMimeTypes(){
        return Display.getInstance().getAvailableRecordingMimeTypes();        
    }
    
    /**
     * Creates a Media recorder Object which will record from the device mic to
     * a file in the given path.
     * 
     * @param path a file path to where to store the recording, if the file does
     * not exists it will be created.
     * @param mimeType the output mime type that is supported see 
     * getAvailableRecordingMimeTypes()
     * @throws IllegalArgumentException if given mime-type is not supported
     * @throws IOException id failed to create a Media object
     */
    public static Media createMediaRecorder(String path, String mimeType) throws IOException {
        return createMediaRecorder(new MediaRecorderBuilder().path(path).mimeType(mimeType));
    }
    
    /**
     * Creates a Media recorder Object which will record from the device mic to
     * a file in the given path.
     * 
     * @param builder media settings
     * @throws IllegalArgumentException if given mime-type is not supported
     * @throws IOException id failed to create a Media object
     * @since 7.0
     */
    public static Media createMediaRecorder(MediaRecorderBuilder builder) throws IOException {
        if (builder.isRedirectToAudioBuffer()) {
            return builder.build();
        }
        String mimeType = builder.getMimeType();
        if (mimeType == null && getAvailableRecordingMimeTypes().length > 0) {
            mimeType = getAvailableRecordingMimeTypes()[0];
        }
        String path = builder.getPath();
        boolean supportedMime = false;
        String [] supported  = getAvailableRecordingMimeTypes();
        int slen = supported.length;
        for (int i = 0; i < slen; i++) {
            String mime = supported[i];
            if(mime.equals(mimeType)){
                supportedMime = true;
                break;
            }
        }
        if(!supportedMime){
            throw new IllegalArgumentException("Mime type " + mimeType + 
                    " is not supported on this platform use "
                    + "getAvailableRecordingMimeTypes()");
        }
        
        return Display.getInstance().createMediaRecorder(path, mimeType);
    }
    
    /**
     * Converts the media object into an AsyncMedia object.  Many media objects
     * area already instances of AsyncMedia, so this method would perform
     * a simple cast.  For media objects that are not already async, this will
     * return an Async wrapper.
     * @param media The media object to convert.
     * @return The media object as an AsyncMedia instance.
     * @since 7.0
     */
    public static AsyncMedia getAsyncMedia(final Media media) {
        if (media instanceof AsyncMedia) {
            return (AsyncMedia)media;
        }
        return new AbstractMedia() {
            @Override
            protected void playImpl() {
                State oldState = getState();
                media.play();
                if (media.isPlaying() && oldState != State.Playing) {
                    fireMediaStateChange(State.Playing);
                }
                if (!media.isPlaying()) {
                    final Timer t = new Timer();
                    t.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (media.isPlaying()) {
                                t.cancel();
                                fireMediaStateChange(State.Playing);
                            }
                        }

                    }, 50, 50);
                }
                
            }

            @Override
            protected void pauseImpl() {
                State oldState = getState();
                media.pause();
                if (!media.isPlaying() && oldState != State.Paused) {
                    fireMediaStateChange(State.Paused);
                }
                if (media.isPlaying()) {
                    final Timer t = new Timer();
                    t.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (!media.isPlaying()) {
                                t.cancel();
                                fireMediaStateChange(State.Paused);
                            }
                        }

                    }, 50, 50);
                }
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
        
        };
    }

}
