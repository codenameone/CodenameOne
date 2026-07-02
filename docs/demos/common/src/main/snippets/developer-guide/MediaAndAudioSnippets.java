// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::media-and-audio-java-001[]
final Form hi = new Form("MediaPlayer", new BorderLayout());
hi.setToolbar(new Toolbar());
Style s = UIManager.getInstance().getComponentStyle("Title");
FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_VIDEO_LIBRARY, s);
hi.getToolbar().addCommandToRightBar("", icon, (evt) -> {
    Display.getInstance().openGallery((e) -> {
        if(e != null && e.getSource() != null) {
            String file = (String)e.getSource();
            try {
                Media video = MediaManager.createMedia(file, true);
                hi.removeAll();
                hi.add(BorderLayout.CENTER, new MediaPlayer(video));
                hi.revalidate();
            } catch(IOException err) {
                Log.e(err);
            }
        }
    }, Display.GALLERY_VIDEO);
});
hi.show();
// end::media-and-audio-java-001[]

// tag::media-and-audio-java-002[]
String bufferKey = "voice-buffer";
AudioBuffer livePcm = MediaManager.getAudioBuffer(bufferKey, true, 8192);
livePcm.addCallback(buffer -> {
    float[] frame = new float[buffer.getSize()];
    buffer.copyTo(frame);
    // Process or copy this frame before the next callback arrives.
});

Media recorder = MediaManager.createMediaRecorder(
        new MediaRecorderBuilder()
                .path(bufferKey)
                .samplingRate(44100)
                .audioChannels(1)
                .redirectToAudioBuffer(true));
recorder.play();

// Later:
recorder.cleanup();
MediaManager.releaseAudioBuffer(bufferKey);
// end::media-and-audio-java-002[]

// tag::media-and-audio-java-003[]
if (VideoIO.isSupported()) {
    VideoReader reader = VideoIO.getVideoIO().openReader(videoPath);
    AudioBuffer videoPcm = reader.readAudio();
    if (videoPcm != null) {
        // Mix or process videoPcm.
    }
}
// end::media-and-audio-java-003[]

// tag::media-and-audio-java-004[]
AudioBuffer music = new AudioBuffer(musicPcm.length);
music.copyFrom(44100, 2, musicPcm);

AudioBuffer effect = new AudioBuffer(effectPcm.length);
effect.copyFrom(44100, 2, effectPcm);

AudioMixer mixer = new AudioMixer(44100, 2);
mixer.addTrack(music, 0, 0.75f);       // Start immediately at 75% gain
mixer.addTrack(effect, 250, 1.0f);     // Start 250 ms into the timeline

AudioBuffer mixed = mixer.mix();
float[] pcm = new float[mixed.getSize()];
mixed.copyTo(pcm);

WAVWriter writer = new WAVWriter(new File("mix.wav"), mixed.getSampleRate(), mixed.getNumChannels(), 16);
try {
    writer.write(pcm, 0, mixed.getSize());
} finally {
    writer.close();
}
// end::media-and-audio-java-004[]

// tag::media-and-audio-java-005[]
AudioBuffer voice = AudioEffects.normalize(livePcm, 0.9f);
AudioBuffer brighter = AudioEffects.equalize(voice,
        0.8f,  // low band gain
        1.0f,  // mid band gain
        1.25f  // high band gain
);

AudioBuffer karaoke = AudioEffects.removeCenter(stereoMusic);
AudioBuffer centeredSpeech = AudioEffects.isolateCenter(stereoInterview);
AudioBuffer voiceEnhanced = AudioEffects.midSide(stereoInterview, 1.4f, 0.6f);
// end::media-and-audio-java-005[]
