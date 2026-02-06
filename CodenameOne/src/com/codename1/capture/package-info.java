/// Package for capturing photos, audio or video from the camera/microphone.
///
///     The code below demonstrates capturing and playing back audio files using this API:
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
package com.codename1.capture;
