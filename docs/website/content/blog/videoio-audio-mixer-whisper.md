---
title: "VideoIO, PCM Mixing And Timed Whisper Captions"
slug: videoio-audio-mixer-whisper
url: /blog/videoio-audio-mixer-whisper/
date: '2026-07-08'
author: Shai Almog
description: "Codename One can now generate and inspect real video: encode app-rendered frames with audio, decode exact frames back out, mix PCM on a sample clock, and turn Whisper segment timestamps into subtitles."
feed_html: '<img src="https://www.codenameone.com/blog/videoio-audio-mixer-whisper.jpg" alt="VideoIO, PCM Mixing And Timed Whisper Captions" /> Codename One can now generate and inspect real video: encode app-rendered frames, decode exact frames, mix PCM, and turn Whisper timestamps into subtitles.'
series: ["release-2026-07-03"]
---

![VideoIO, PCM Mixing And Timed Whisper Captions](/blog/videoio-audio-mixer-whisper.jpg)

This release turns media from "play a file" into "build a file."

[PR #5315](https://github.com/codenameone/CodenameOne/pull/5315) adds `VideoIO`, a cross-platform video subsystem that can encode app-rendered frames and audio into a standard video file, then decode an existing clip back into exact RGBA frames and PCM audio. [PR #5317](https://github.com/codenameone/CodenameOne/pull/5317) adds `AudioMixer`, a sample-accurate PCM timeline. [PR #5319](https://github.com/codenameone/CodenameOne/pull/5319) adds timed Whisper transcription segments with SRT and VTT output.

Together, these APIs let Codename One generate proper videos across supported app platforms.

![Frame-accurate VideoIO decode output from the Codename One test app](/blog/videoio-audio-mixer-whisper/videoio-decoded-frames.png)

## Encoding App-Rendered Frames

`VideoIO` is gated like other optional platform APIs:

```java
if (!VideoIO.isSupported()) {
    return;
}
```

Then you build a writer. Frames are ordinary `Image` instances, so the pixels can come from your UI, a game renderer, an animation scrubber, a generated chart, or a custom drawing routine.

```java
String out = FileSystemStorage.getInstance().getAppHomePath() + "/demo.mp4";

VideoWriter writer = new VideoWriterBuilder()
        .path(out)
        .container(VideoIO.CONTAINER_MP4)
        .width(720)
        .height(1280)
        .frameRate(30)
        .videoCodec(VideoIO.CODEC_H264)
        .videoBitRate(4000000)
        .hasAudio(true)
        .audioCodec(VideoIO.CODEC_AAC)
        .sampleRate(44100)
        .audioChannels(2)
        .build();

try {
    for (int i = 0; i < 90; i++) {
        Image frame = renderFrame(i);
        writer.writeFrame(frame, i * 1000L / 30L);
    }

    short[] pcm = renderAudioTrack();
    writer.writeAudio(pcm, 44100, 2, 0);
} finally {
    writer.close();
}
```

The exact codecs available are platform-dependent. `VideoIO.getAvailableEncoders()` and `getAvailableDecoders()` expose what the device actually supports, including hardware acceleration flags.

## Frame-Accurate Decode

The old `Media` API is a player. It can seek, but playback seek is usually key-frame based. That is not enough when you are generating thumbnails, verifying a rendered animation, extracting frames for processing, or round-tripping a generated video in a test.

`VideoReader.frameAt(ms)` returns the exact frame at a timestamp:

```java
VideoReader reader = VideoIO.getVideoIO().openReader(out);
try {
    VideoFrame oneSecond = reader.frameAt(1000);
    Image image = oneSecond.getImage();
} finally {
    reader.close();
}
```

You can also resample a whole clip to a constant frame rate:

```java
reader.readFrames(10, frame -> {
    process(frame.getImage(), frame.getTimestampMillis());
    return true;
});
```

And when the backend supports it, `readAudio()` returns the audio track as an `AudioBuffer`:

```java
AudioBuffer audio = reader.readAudio();
```

JavaScript currently supports frame decode and full encode, but decoded audio extraction is not wired there yet. TV, Watch and Car targets do not expose `VideoIO`.

## PCM Mixing On One Clock

`AudioMixer` is intentionally small. It combines interleaved float PCM tracks in the normalized `[-1, 1]` range on a single sample clock.

```java
AudioBuffer music = new AudioBuffer(musicPcm.length);
music.copyFrom(44100, 2, musicPcm);

AudioBuffer voice = new AudioBuffer(voicePcm.length);
voice.copyFrom(44100, 2, voicePcm);

AudioMixer mixer = new AudioMixer(44100, 2);
mixer.addTrack(music, 0, 0.65f);
mixer.addTrack(voice, 1200, 1.0f);

AudioBuffer mixed = mixer.mix();
```

If you already know exact sample-frame offsets, use `addTrackAtFrame(...)` instead of millisecond offsets. The output clips to `[-1, 1]` and can be written through `WAVWriter` or passed into a `VideoWriter`.

```java
float[] pcm = new float[mixed.getSize()];
mixed.copyTo(pcm);

WAVWriter wav = new WAVWriter(new File("mix.wav"),
        mixed.getSampleRate(),
        mixed.getNumChannels(),
        16);
try {
    wav.write(pcm, 0, mixed.getSize());
} finally {
    wav.close();
}
```

## Subtitles From Timed Whisper

The Whisper cn1lib already returned text. The new transcription API returns timed segments:

```java
String modelPath = FileSystemStorage.getInstance().getAppHomePath()
        + "/ggml-base.en.bin";
Transcriber t = WhisperRecognizer.transcriber(modelPath);
TranscriptionResult result = t.transcribe(
        new TranscriptionRequest(audioPath).setLanguageTag("en-US")).get();

String srt = result.toSrt();
String vtt = result.toVtt();
```

That `get()` call is blocking, so run it off the EDT or use the returned `AsyncResource` callback. Those segment timestamps are the missing link between "we transcribed the video" and "the video has subtitles." A generated training clip, game replay, tutorial or support recording can now get caption files automatically.

The Whisper PR also fixed native packaging gaps: Android now ships a JNI AAR with native slices, iOS exposes timed segments, JavaSE keeps a fallback segment, Linux and Windows native bridge modules are packaged, and JavaScript is explicitly unsupported.

## Where MorphTransition Fits

[PR #5314](https://github.com/codenameone/CodenameOne/pull/5314) is listed as a smaller enhancement in the release post, but it connects directly to media generation. `MorphTransition` is now scrubbable:

```java
MorphTransition transition = MorphTransition.create(300)
        .snapshotMode(true)
        .opacity("card", 0, 255)
        .scale("card", 0.8f, 1.0f)
        .rotation("card", -0.2f, 0);

transition.setProgress(0.5);
```

Scrubbing means a transition can be rendered deterministically at progress `0.0`, `0.1`, `0.2`, and onward. That is exactly what a video export loop needs.

## Platform Backends

The backends use native media stacks rather than one portable lowest-common-denominator codec:

- JavaSE and the simulator use bundled ffmpeg/ffprobe.
- iOS and macOS use AVFoundation.
- Android uses MediaCodec, MediaMuxer, MediaExtractor and MediaMetadataRetriever.
- Windows uses Media Foundation.
- Linux uses GStreamer.
- JavaScript uses HTML5 video/canvas decode and WebCodecs encode.

This gives each platform the codec set and acceleration it already has. App code still starts from the same `VideoIO` facade.

## Wrapping Up

Video export used to be something a Codename One app handed off to a backend or a native library. Now the app can render frames, mix PCM, encode a video, decode it back for validation, and generate timed captions. That opens up tutorials, game clips, social exports, support recordings, animated explainers and media QA workflows from the same Java codebase.

No special revenue model is attached to that. The app generates the media. The app owns the output.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
