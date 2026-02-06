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
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/// Allow us to create {@ling com.codename1.media.Media} objects using String URI's or with a stream to
/// the media data.
///
/// Notice that the underlying platforms contains the actual codecs, therefore
/// you need to play common media file types such as mp3, mp4 to successfully play them across devices
/// on all target platforms. The simulator can't accurately reproduce the behavior of this class in all devices/cases.
///
/// The sample code below demonstrates simple video playback.
///
/// ```java
/// final Form hi = new Form("MediaPlayer", new BorderLayout());
/// hi.setToolbar(new Toolbar());
/// Style s = UIManager.getInstance().getComponentStyle("Title");
/// FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_VIDEO_LIBRARY, s);
/// hi.getToolbar().addCommandToRightBar(new Command("", icon) {
/// @Override
///     public void actionPerformed(ActionEvent evt) {
///         Display.getInstance().openGallery((e) -> {
///             if(e != null && e.getSource() != null) {
///                 String file = (String)e.getSource();
///                 try {
///                     Media video = MediaManager.createMedia(file, true);
///                     hi.removeAll();
///                     hi.add(BorderLayout.CENTER, new MediaPlayer(video));
///                     hi.revalidate();
///                 } catch(IOException err) {
///                     Log.e(err);
///                 }
///             }
///         }, Display.GALLERY_VIDEO);
///     }
/// });
/// hi.show();
/// ```
///
/// The code below demonstrates capturing audio using the Capture API and playing back audio files using the Media API:
///
/// ```java
/// Form hi = new Form("Capture", BoxLayout.y());
/// hi.setToolbar(new Toolbar());
/// Style s = UIManager.getInstance().getComponentStyle("Title");
/// FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_MIC, s);
///
/// FileSystemStorage fs = FileSystemStorage.getInstance();
/// String recordingsDir = fs.getAppHomePath() + "recordings/";
/// fs.mkdir(recordingsDir);
/// try {
///     for(String file : fs.listFiles(recordingsDir)) {
///         MultiButton mb = new MultiButton(file.substring(file.lastIndexOf("/") + 1));
///         mb.addActionListener((e) -> {
///             try {
///                 Media m = MediaManager.createMedia(recordingsDir + file, false);
///                 m.play();
///             } catch(IOException err) {
///                 Log.e(err);
///             }
///         });
///         hi.add(mb);
///     }
///
///     hi.getToolbar().addCommandToRightBar("", icon, (ev) -> {
///         try {
///             String file = Capture.captureAudio();
///             if(file != null) {
///                 SimpleDateFormat sd = new SimpleDateFormat("yyyy-MMM-dd-kk-mm");
///                 String fileName =sd.format(new Date());
///                 String filePath = recordingsDir + fileName;
///                 Util.copy(fs.openInputStream(file), fs.openOutputStream(filePath));
///                 MultiButton mb = new MultiButton(fileName);
///                 mb.addActionListener((e) -> {
///                     try {
///                         Media m = MediaManager.createMedia(filePath, false);
///                         m.play();
///                     } catch(IOException err) {
///                         Log.e(err);
///                     }
///                 });
///                 hi.add(mb);
///                 hi.revalidate();
///             }
///         } catch(IOException err) {
///             Log.e(err);
///         }
///     });
/// } catch(IOException err) {
///     Log.e(err);
/// }
/// hi.show();
/// ```
///
/// The code below demonstrates capturing audio and playing back audio using the Media, MediaManager and MediaRecorderBuilder APIs,
/// as alternative and more customizable approach than using the Capture API:
///
/// ```java
///     private static final EasyThread countTime = EasyThread.start("countTime");
///
///     public void start() {
///         if (current != null) {
///             current.show();
///             return;
///         }
///         Form hi = new Form("Recording audio", BoxLayout.y());
///         hi.add(new SpanLabel("Example of recording and playback audio using the Media, MediaManager and MediaRecorderBuilder APIs"));
///         hi.add(recordAudio((String filePath) -> {
///             ToastBar.showInfoMessage("Do something with the recorded audio file: " + filePath);
///         }));
///         hi.show();
///     }
///
///     public static Component recordAudio(OnComplete callback) {
///         try {
///             // mime types supported by Android: audio/amr, audio/aac, audio/mp4
///             // mime types supported by iOS: audio/mp4, audio/aac, audio/m4a
///             // mime type supported by Simulator: audio/wav
///             // more info: https://www.iana.org/assignments/media-types/media-types.xhtml
///
///             List availableMimetypes = Arrays.asList(MediaManager.getAvailableRecordingMimeTypes());
///             String mimetype;
///             if (availableMimetypes.contains("audio/aac")) {
///                 // Android and iOS
///                 mimetype = "audio/aac";
///             } else if (availableMimetypes.contains("audio/wav")) {
///                 // Simulator
///                 mimetype = "audio/wav";
///             } else {
///                 // others
///                 mimetype = availableMimetypes.get(0);
///             }
///             String fileName = "audioExample." + mimetype.substring(mimetype.indexOf("/") + 1);
///             String output = FileSystemStorage.getInstance().getAppHomePath() + "/" + fileName;
///             // https://tritondigitalcommunity.force.com/s/article/Choosing-Audio-Bitrate-Settings
///             MediaRecorderBuilder options = new MediaRecorderBuilder()
///                     .mimeType(mimetype)
///                     .path(output)
///                     .bitRate(64000)
///                     .samplingRate(44100);
///             Media[] microphone = {MediaManager.createMediaRecorder(options)};
///             Media[] speaker = {null};
///
///             Container recordingUI = new Container(BoxLayout.y());
///             Label time = new Label("0:00");
///             Button recordBtn = new Button("", FontImage.MATERIAL_FIBER_MANUAL_RECORD, "Button");
///             Button playBtn = new Button("", FontImage.MATERIAL_PLAY_ARROW, "Button");
///             Button stopBtn = new Button("", FontImage.MATERIAL_STOP, "Button");
///             Button sendBtn = new Button("Send");
///             sendBtn.setEnabled(false);
///             Container buttons = GridLayout.encloseIn(3, recordBtn, stopBtn, sendBtn);
///             recordingUI.addAll(FlowLayout.encloseCenter(time), FlowLayout.encloseCenter(buttons));
///
///             recordBtn.addActionListener(l -> {
///                 try {
///                     // every time we have to create a new instance of Media to make it working correctly (as reported in the Javadoc)
///                     microphone[0] = MediaManager.createMediaRecorder(options);
///                     if (speaker[0] != null && speaker[0].isPlaying()) {
///                         return; // do nothing if the audio is currently recorded or played
///                     }
///                     recordBtn.setEnabled(false);
///                     sendBtn.setEnabled(true);
///                     Log.p("Audio recording started", Log.DEBUG);
///                     if (buttons.contains(playBtn)) {
///                         buttons.replace(playBtn, stopBtn, CommonTransitions.createEmpty());
///                         buttons.revalidateWithAnimationSafety();
///                     }
///                     if (speaker[0] != null) {
///                         speaker[0].pause();
///                     }
///
///                     microphone[0].play();
///                     startWatch(time);
///                 } catch (IOException ex) {
///                     Log.p("ERROR recording audio", Log.ERROR);
///                     Log.e(ex);
///                 }
///             });
///
///             stopBtn.addActionListener(l -> {
///                 if (!microphone[0].isPlaying() && (speaker[0] == null || !speaker[0].isPlaying())) {
///                     return; // do nothing if the audio is NOT currently recorded or played
///                 }
///                 recordBtn.setEnabled(true);
///                 sendBtn.setEnabled(true);
///                 Log.p("Audio recording stopped");
///                 if (microphone[0].isPlaying()) {
///                     microphone[0].pause();
///                 } else if (speaker[0] != null) {
///                     speaker[0].pause();
///                 } else {
///                     return;
///                 }
///                 stopWatch(time);
///                 if (buttons.contains(stopBtn)) {
///                     buttons.replace(stopBtn, playBtn, CommonTransitions.createEmpty());
///                     buttons.revalidateWithAnimationSafety();
///                 }
///                 if (FileSystemStorage.getInstance().exists(output)) {
///                     Log.p("Audio saved to: " + output);
///                 } else {
///                     ToastBar.showErrorMessage("Error recording audio", 5000);
///                     Log.p("ERROR SAVING AUDIO");
///                 }
///             });
///
///             playBtn.addActionListener(l -> {
///                 // every time we have to create a new instance of Media to make it working correctly (as reported in the Javadoc)
///                 if (microphone[0].isPlaying() || (speaker[0] != null && speaker[0].isPlaying())) {
///                     return; // do nothing if the audio is currently recorded or played
///                 }
///                 recordBtn.setEnabled(false);
///                 sendBtn.setEnabled(true);
///                 if (buttons.contains(playBtn)) {
///                     buttons.replace(playBtn, stopBtn, CommonTransitions.createEmpty());
///                     buttons.revalidateWithAnimationSafety();
///                 }
///                 if (FileSystemStorage.getInstance().exists(output)) {
///                     try {
///                         speaker[0] = MediaManager.createMedia(output, false, () -> {
///                             // callback on completation
///                             recordBtn.setEnabled(true);
///                             if (speaker[0].isPlaying()) {
///                                 speaker[0].pause();
///                             }
///                             stopWatch(time);
///                             if (buttons.contains(stopBtn)) {
///                                 buttons.replace(stopBtn, playBtn, CommonTransitions.createEmpty());
///                                 buttons.revalidateWithAnimationSafety();
///                             }
///                         });
///                         speaker[0].play();
///                         startWatch(time);
///                     } catch (IOException ex) {
///                         Log.p("ERROR playing audio", Log.ERROR);
///                         Log.e(ex);
///                     }
///                 }
///             });
///
///             sendBtn.addActionListener(l -> {
///                 if (microphone[0].isPlaying()) {
///                     microphone[0].pause();
///                 }
///                 if (speaker[0] != null && speaker[0].isPlaying()) {
///                     speaker[0].pause();
///                 }
///                 if (buttons.contains(stopBtn)) {
///                     buttons.replace(stopBtn, playBtn, CommonTransitions.createEmpty());
///                     buttons.revalidateWithAnimationSafety();
///                 }
///                 stopWatch(time);
///                 recordBtn.setEnabled(true);
///
///                 callback.completed(output);
///             });
///
///             return FlowLayout.encloseCenter(recordingUI);
///
///         } catch (IOException ex) {
///             Log.p("ERROR recording audio", Log.ERROR);
///             Log.e(ex);
///             return new Label("Error recording audio");
///         }
///
///
///     }
///
///     private static void startWatch(Label label) {
///         label.putClientProperty("stopTime", Boolean.FALSE);
///         countTime.run(() -> {
///             long startTime = System.currentTimeMillis();
///             while (label.getClientProperty("stopTime") == Boolean.FALSE) {
///                 // the sleep is every 200ms instead of 1000ms to make the app more reactive when stop is tapped
///                 Util.sleep(200);
///                 int seconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
///                 String min = (seconds / 60) + "";
///                 String sec = (seconds % 60) + "";
///                 if (sec.length() == 1) {
///                     sec = "0" + sec;
///                 }
///                 String newTime = min + ":" + sec;
///                 if (!label.getText().equals(newTime)) {
///                     CN.callSerially(() -> {
///                         label.setText(newTime);
///                         if (label.getParent() != null) {
///                             label.getParent().revalidateWithAnimationSafety();
///                         }
///                     });
///                 }
///             }
///         });
///     }
///
///     private static void stopWatch(Label label) {
///         label.putClientProperty("stopTime", Boolean.TRUE);
///     }
/// ```
public abstract class MediaManager {

    /// A static map of audio buffers.  These can be used to register an Audio buffer to receive
    /// raw PCM data from the microphone.
    ///
    /// #### Since
    ///
    /// 7.0
    private static final Map<String, AudioBuffer> audioBuffers = new HashMap<String, AudioBuffer>();
    private static RemoteControlListener remoteControlListener;

    /// Gets an audio buffer at the given path.
    ///
    /// #### Parameters
    ///
    /// - `path`: @param path The path to the Audio buffer.  This path doesn't correspond to a real file.  It is just
    ///             used as a key to map to the audio buffer so that it can be addressed.
    ///
    /// #### Returns
    ///
    /// The AudioBuffer or null if no buffer exists at that path.
    ///
    /// #### Since
    ///
    /// 7.0
    public static AudioBuffer getAudioBuffer(String path) {
        return getAudioBuffer(path, false, 256);
    }

    /// Gets or creates an audio buffer at the given path.
    ///
    /// #### Parameters
    ///
    /// - `path`: @param path   The path to the Audio buffer.  This path doesn't correspond to a real file.  It is just
    ///               used as a key to map to the audio buffer so that it can be addressed.
    ///
    /// - `create`: @param create If this flag is true and no buffer exists at the given path,
    ///               then the buffer will be created.
    ///
    /// - `size`: The maximum size of the buffer.
    ///
    /// #### Returns
    ///
    /// The audio buffer or null if no buffer exists at that path and the create flag is false.
    ///
    /// #### Since
    ///
    /// 7.0
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

    /// Releases an audio buffer at a given path.  Audio buffers use a simple reference counter
    /// mechanism.  Every call to `boolean, int)` will increment
    /// the counter, and calls to `#releaseAudioBuffer(java.lang.String)` will decrement the counter.
    ///
    /// #### Parameters
    ///
    /// - `path`: The path to the buffer.
    ///
    /// #### Since
    ///
    /// 7.0
    public static void releaseAudioBuffer(String path) {
        AudioBuffer buf = audioBuffers.get(path);
        if (buf != null) {
            int refCount = buf.release();
            if (refCount <= 0) {
                audioBuffers.remove(path);
            }
        }
    }

    /// Deletes the audio buffer at the given path.
    ///
    /// #### Parameters
    ///
    /// - `path`: The path to the audio buffer to delete.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### Deprecated
    ///
    /// Prefer to use `#releaseAudioBuffer(java.lang.String)`
    public static void deleteAudioBuffer(String path) {
        audioBuffers.remove(path);
    }

    /// Gets the currently registered remote control listener.
    ///
    /// #### Returns
    ///
    /// @return The currently registered remote control listener, or null if
    /// none is registered.
    ///
    /// #### Since
    ///
    /// 7.0
    public static RemoteControlListener getRemoteControlListener() {
        return remoteControlListener;
    }

    /// Registers a listener to be notified of remote control events - e.g.
    /// the play/pause/seek buttons on the user's lock screen when background
    /// media is being played.
    ///
    /// #### Parameters
    ///
    /// - `l`: The remote control listener to set.  null to set no listener.
    ///
    /// #### Since
    ///
    /// 7.0
    public static synchronized void setRemoteControlListener(RemoteControlListener l) {
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

    /// Creates an audio media that can be played in the background.
    ///
    /// #### Parameters
    ///
    /// - `uri`: @param uri the uri of the media can start with jar://, file://, http://
    ///            (can also use rtsp:// if supported on the platform)
    ///
    /// #### Returns
    ///
    /// @return Media a Media Object that can be used to control the playback
    /// of the media
    ///
    /// #### Throws
    ///
    /// - `IOException`: if creation of media from the given URI has failed
    public static Media createBackgroundMedia(String uri) throws IOException {
        return Display.getInstance().createBackgroundMedia(uri);
    }

    /// Creates an audio media asynchronously that can be played in the background.
    ///
    /// #### Parameters
    ///
    /// - `uri`: @param uri the uri of the media can start with jar://, file://, http://
    ///            (can also use rtsp:// if supported on the platform)
    ///
    /// #### Returns
    ///
    /// @return Media a Media Object that can be used to control the playback
    /// of the media
    ///
    /// #### Since
    ///
    /// 7.0
    public static AsyncResource<Media> createBackgroundMediaAsync(String uri) {
        return Display.getInstance().createBackgroundMediaAsync(uri);
    }

    /// Creates a Media from a given URI
    ///
    /// #### Parameters
    ///
    /// - `uri`: @param uri     the uri of the media can start with file://, http:// (can also
    ///                use rtsp:// although may not be supported on all target platforms)
    ///
    /// - `isVideo`: a boolean flag to indicate if this is a video media
    ///
    /// #### Returns
    ///
    /// @return Media a Media Object that can be used to control the playback
    /// of the media
    ///
    /// #### Throws
    ///
    /// - `IOException`: if creation of media from the given URI has failed
    public static Media createMedia(String uri, boolean isVideo) throws IOException {
        return createMedia(uri, isVideo, null);
    }

    /// Creates the Media in the given stream.
    /// Notice that you should invoke cleanup on a media once you are done with it.
    ///
    /// #### Parameters
    ///
    /// - `stream`: the stream containing the media data
    ///
    /// - `mimeType`: the type of the data in the stream
    ///
    /// #### Returns
    ///
    /// @return Media a Media Object that can be used to control the playback
    /// of the media
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: if the creation of the Media has failed
    public static Media createMedia(InputStream stream, String mimeType) throws IOException {
        return createMedia(stream, mimeType, null);
    }

    /// Creates the Media in the given stream asynchronously.
    /// Notice that you should invoke cleanup on a media once you are done with it.
    ///
    /// #### Parameters
    ///
    /// - `stream`: the stream containing the media data
    ///
    /// - `mimeType`: the type of the data in the stream
    ///
    /// #### Returns
    ///
    /// @return Media a Media Object that can be used to control the playback
    /// of the media
    ///
    /// #### Since
    ///
    /// 7.0
    public static AsyncResource<Media> createMediaAsync(InputStream stream, String mimeType, Runnable onCompletion) {
        return Display.getInstance().createMediaAsync(stream, mimeType, onCompletion);
    }

    /// Creates a Media from a given URI
    ///
    /// #### Parameters
    ///
    /// - `uri`: @param uri          the uri of the media can start with file://, http:// (can also
    ///                     use rtsp:// although may not be supported on all target platforms)
    ///
    /// - `isVideo`: a boolean flag to indicate if this is a video media
    ///
    /// - `onCompletion`: a Runnable to be called when the media has finished
    ///
    /// #### Returns
    ///
    /// @return Media a Media Object that can be used to control the playback
    /// of the media
    ///
    /// #### Throws
    ///
    /// - `IOException`: if creation of media from given URI failed
    public static Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {
        return Display.getInstance().createMedia(uri, isVideo, onCompletion);
    }

    /// Creates a Media from a given URI asynchronously.
    ///
    /// #### Parameters
    ///
    /// - `uri`: @param uri          the uri of the media can start with file://, http:// (can also
    ///                     use rtsp:// although may not be supported on all target platforms)
    ///
    /// - `isVideo`: a boolean flag to indicate if this is a video media
    ///
    /// - `onCompletion`: a Runnable to be called when the media has finished
    ///
    /// #### Returns
    ///
    /// @return Media a Media Object that can be used to control the playback
    /// of the media
    ///
    /// #### Since
    ///
    /// 7.0
    public static AsyncResource<Media> createMediaAsync(String uri, boolean isVideo, Runnable onCompletion) {
        return Display.getInstance().createMediaAsync(uri, isVideo, onCompletion);
    }

    /// Adds a callback to a Media element that will be called when the media finishes playing.
    ///
    /// #### Parameters
    ///
    /// - `media`: The media to add the callback to.
    ///
    /// - `onCompletion`: The callback that will run on the EDT when the playback completes.
    ///
    /// #### See also
    ///
    /// - #removeCompletionHandler(com.codename1.media.Media, java.lang.Runnable)
    public static void addCompletionHandler(Media media, Runnable onCompletion) {
        Display.getInstance().addCompletionHandler(media, onCompletion);
    }

    /// Removes onComplete callback from Media element.
    ///
    /// #### Parameters
    ///
    /// - `media`: The media element.
    ///
    /// - `onCompletion`: The callback.
    ///
    /// #### See also
    ///
    /// - #addCompletionHandler(com.codename1.media.Media, java.lang.Runnable)
    public static void removeCompletionHandler(Media media, Runnable onCompletion) {
        Display.getInstance().removeCompletionHandler(media, onCompletion);
    }

    /// Creates the Media in the given stream
    /// Notice that you should invoke cleanup on a media once you are done with it.
    ///
    /// #### Parameters
    ///
    /// - `stream`: the stream containing the media data
    ///
    /// - `mimeType`: the type of the data in the stream
    ///
    /// - `onCompletion`: a Runnable to be called when the media has finished
    ///
    /// #### Returns
    ///
    /// @return Media a Media Object that can be used to control the playback
    /// of the media
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: if the URI access fails
    public static Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return Display.getInstance().createMedia(stream, mimeType, onCompletion);
    }

    /// Creates a Media recorder Object which will record from the device mic to
    /// a file in the given path.
    /// The output format will be amr-nb if supported by the platform.
    ///
    /// #### Parameters
    ///
    /// - `path`: @param path a file path to where to store the recording, if the file does
    ///             not exists it will be created.
    ///
    /// #### Deprecated
    ///
    /// see createMediaRecorder(String path, String mimeType) instead
    public static Media createMediaRecorder(String path) throws IOException {
        return createMediaRecorder(path, getAvailableRecordingMimeTypes()[0]);
    }

    /// Gets the recording mime type for the returned Media from the
    /// createMediaRecorder method
    ///
    /// #### Returns
    ///
    /// the recording mime type
    ///
    /// #### Deprecated
    ///
    /// see getAvailableRecordingMimeTypes() instead
    public static String getMediaRecorderingMimeType() {
        return Display.getInstance().getMediaRecorderingMimeType();
    }

    /// Gets the available recording MimeTypes
    public static String[] getAvailableRecordingMimeTypes() {
        return Display.getInstance().getAvailableRecordingMimeTypes();
    }

    /// Creates a Media recorder Object which will record from the device mic to
    /// a file in the given path.
    ///
    /// #### Parameters
    ///
    /// - `path`: @param path     a file path to where to store the recording, if the file does
    ///                 not exists it will be created.
    ///
    /// - `mimeType`: @param mimeType the output mime type that is supported see
    ///                 getAvailableRecordingMimeTypes()
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if given mime-type is not supported
    ///
    /// - `IOException`: id failed to create a Media object
    public static Media createMediaRecorder(String path, String mimeType) throws IOException {
        return createMediaRecorder(new MediaRecorderBuilder().path(path).mimeType(mimeType));
    }

    /// Creates a Media recorder Object which will record from the device mic to
    /// a file in the given path.
    ///
    /// #### Parameters
    ///
    /// - `builder`: media settings
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if given mime-type is not supported
    ///
    /// - `IOException`: id failed to create a Media object
    ///
    /// #### Since
    ///
    /// 7.0
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
        String[] supported = getAvailableRecordingMimeTypes();
        int slen = supported.length;
        for (int i = 0; i < slen; i++) {
            String mime = supported[i];
            if (mime.equals(mimeType)) {
                supportedMime = true;
                break;
            }
        }
        if (!supportedMime) {
            throw new IllegalArgumentException("Mime type " + mimeType +
                    " is not supported on this platform use "
                    + "getAvailableRecordingMimeTypes()");
        }

        return Display.getInstance().createMediaRecorder(path, mimeType);
    }

    /// Converts the media object into an AsyncMedia object.  Many media objects
    /// area already instances of AsyncMedia, so this method would perform
    /// a simple cast.  For media objects that are not already async, this will
    /// return an Async wrapper.
    ///
    /// #### Parameters
    ///
    /// - `media`: The media object to convert.
    ///
    /// #### Returns
    ///
    /// The media object as an AsyncMedia instance.
    ///
    /// #### Since
    ///
    /// 7.0
    public static AsyncMedia getAsyncMedia(final Media media) {
        if (media instanceof AsyncMedia) {
            return (AsyncMedia) media;
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
            public int getVolume() {
                return media.getVolume();
            }

            @Override
            public void setVolume(int vol) {
                media.setVolume(vol);
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
            public boolean isNativePlayerMode() {
                return media.isNativePlayerMode();
            }

            @Override
            public void setNativePlayerMode(boolean nativePlayer) {
                media.setNativePlayerMode(nativePlayer);
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
