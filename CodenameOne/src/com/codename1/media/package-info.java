/* 
    Document   : package
    Created on : Oct 11, 2007, 10:38:26 AM
    Author     : Shai Almog
*/

/// Video and Audio playback support are handled within this package using the
/// `com.codename1.media.Media` & `com.codename1.media.MediaManager` APIs.
/// Said API's allow for video playback both within a native full screen player and embedded within
/// an application screen.
///
/// Simplified video playback API is also available via the `com.codename1.components.MediaPlayer` class.
/// Capture/recording is handled separately for the most part thru the
/// `com.codename1.capture.Capture` API. However, there is some basic low level recording
/// functionality within `com.codename1.media.MediaManager` as well.
///
/// The code below demonstrates capturing and playing back audio files using this API:
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
package com.codename1.media;
