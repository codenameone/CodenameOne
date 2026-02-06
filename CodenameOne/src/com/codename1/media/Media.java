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


/// Media control interface allows for media playback, recording. To get an instance
/// of this interface (implemented by the native port) see the MediaManager class.
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
///
/// #### See also
///
/// - MediaManager
public interface Media {
    /// Write only variable that can be used with setVariable to pass a title for the
    /// native layer so the currently playing media title will be displayed in the lock screen
    /// where applicable
    String VARIABLE_BACKGROUND_TITLE = "bgTitle";

    /// Write only variable that can be used with setVariable to pass the artist name to the
    /// native layer so the currently playing media title will be displayed in the lock screen
    /// where applicable
    String VARIABLE_BACKGROUND_ARTIST = "bgArtist";

    /// Write only variable that can be used with setVariable to pass the duration for the media as a Long object to the
    /// native layer so the currently playing media title will be displayed in the lock screen
    /// where applicable
    String VARIABLE_BACKGROUND_DURATION = "bgDuration";

    /// Write only variable that can be used with setVariable to pass the album cover Image to the
    /// native layer so the currently playing media title will be displayed in the lock screen
    /// where applicable
    String VARIABLE_BACKGROUND_ALBUM_COVER = "bgCover";

    /// Write only variable that can be used with setVariable to pass the position in the media (Long object) to the
    /// native layer so the currently playing media title will be displayed in the lock screen
    /// where applicable
    String VARIABLE_BACKGROUND_POSITION = "bgPosition";

    /// Read only variable that can be used with getVariable to query whether the
    /// native layer supports displaying the currently playing media information
    /// in the lock screen. This will return null or Boolean.TRUE.
    String VARIABLE_BACKGROUND_SUPPORTED = "bgInfoSupported";

    /// Write-only variable that can be used with getVariable() to set whether this
    /// video should include embedded native controls.
    String VARIABLE_NATIVE_CONTRLOLS_EMBEDDED = "nativeControlsVisible";

    /// Starts playing or recording the media file
    void play();

    /// Pauses (actually stops) the playback or the recording of the media file
    void pause();

    /// Optional call that allows the caller to prepare the upcoming media player. This is useful
    /// when streaming multiple streams one after another.
    ///
    /// Note: On some platforms (iOS), the poster frame and native embedded controls will not appear
    /// for the video until you call this method, otherwise.
    void prepare();

    /// Stops the audio playback and cleans up the resources related to it immediately.
    void cleanup();

    /// Returns the time in milliseconds in the audio file or -1 if not known
    ///
    /// #### Returns
    ///
    /// time in milliseconds
    int getTime();

    /// Sets the position in the audio file or doesn't effect if not supported
    ///
    /// #### Parameters
    ///
    /// - `time`: in milliseconds
    void setTime(int time);

    /// Returns the length in milliseconds of the audio file or -1 if not known
    ///
    /// #### Returns
    ///
    /// time in milliseconds
    int getDuration();

    /// Returns the media playback volume in percentage
    ///
    /// #### Returns
    ///
    /// the volume percentage
    int getVolume();

    /// Sets the media playback volume in percentage
    ///
    /// #### Parameters
    ///
    /// - `vol`: the volume for media playback
    void setVolume(int vol);

    /// Returns true if the media is currently playing or recording
    ///
    /// #### Returns
    ///
    /// true if playing
    boolean isPlaying();

    /// Gets the VideoComponent of this Video.
    ///
    /// #### Returns
    ///
    /// @return a Component of the video to be placed on a Form or null if this
    /// Media is not a Video
    Component getVideoComponent();

    /// This method returns true if this is a Video Media
    ///
    /// #### Returns
    ///
    /// true if video
    boolean isVideo();

    /// This method returns true if this video is in full screen mode.
    ///
    /// #### Returns
    ///
    /// true if full screen
    boolean isFullScreen();

    /// Sets the Media to be displayed full screen, make sure the
    /// getVideoComponent() is called on the Video Component is added to the
    /// current Form
    ///
    /// #### Parameters
    ///
    /// - `fullScreen`
    void setFullScreen(boolean fullScreen);

    /// Returns true if this Video Media is in Native player mode.
    /// Some platforms such as BlackBerry is able to play video only on the native
    /// player on those platforms isNativePlayerMode() will always return true
    /// If Media supports native playing by calling to play() the video will start
    /// playing in the native player in full screen.
    ///
    /// #### Returns
    ///
    /// true if the player is in native mode
    boolean isNativePlayerMode();

    /// By calling this the Media (if it's a Video) will be played full screen
    /// on the native Player of the device.
    /// Native playing assumes getVideoComponent() hasn't been called on this Media
    /// unexpected behaviour may occur if getVideoComponent() has been called
    /// and the Component is placed on a Form.
    /// Some platforms such as BlackBerry is able to play video only on the native
    /// player on those platforms isNativePlayerMode() will always return true
    void setNativePlayerMode(boolean nativePlayer);

    /// Allows for platform specific enhancements for media playback
    ///
    /// #### Parameters
    ///
    /// - `key`: the key to set to the media that is platform specific
    ///
    /// - `value`: the value
    void setVariable(String key, Object value);

    /// Allows querying platform specific information from the media object
    ///
    /// #### Parameters
    ///
    /// - `key`: the key query
    ///
    /// #### Returns
    ///
    /// the value or null
    Object getVariable(String key);
}
