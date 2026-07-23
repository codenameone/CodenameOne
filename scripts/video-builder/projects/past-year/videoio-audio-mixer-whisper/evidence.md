# Evidence map

Source: `docs/website/content/blog/videoio-audio-mixer-whisper.md`
Canonical: https://www.codenameone.com/blog/videoio-audio-mixer-whisper/

## Thesis

A deterministic frame, PCM, and timed-caption pipeline for generated video

## Supported beats

- **Frame-Accurate Decode:** The old Media API is a player. It can seek, but playback seek is usually key-frame based. That is not enough when you are generating thumbnails, verifying a rendered animation, extracting frames for processing, or round-tripping a generated video in a test.
- **Encoding App-Rendered Frames:** Then you build a writer. Frames are ordinary Image instances, so the pixels can come from your UI, a game renderer, an animation scrubber, a generated chart, or a custom drawing routine.
- **PCM Mixing On One Clock:** If you already know exact sample-frame offsets, use addTrackAtFrame(...) instead of millisecond offsets. The output clips to [-1, 1] and can be written through WAVWriter or passed into a VideoWriter.
- **Subtitles From Timed Whisper:** That get() call is blocking, so run it off the EDT or use the returned AsyncResource callback. Those segment timestamps are the missing link between "we transcribed the video" and "the video has subtitles." A generated training clip, game replay, tutorial or support recording can now get caption files automatically.
- **Where MorphTransition Fits:** PR #5314 is listed as a smaller enhancement in the release post, but it connects directly to media generation. MorphTransition is now scrubbable.
- **Platform Backends:** This gives each platform the codec set and acceleration it already has. App code still starts from the same VideoIO facade.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5315
- https://github.com/codenameone/CodenameOne/pull/5317
- https://github.com/codenameone/CodenameOne/pull/5319
- https://github.com/codenameone/CodenameOne/pull/5314

## Independent problem evidence

- WebCodecs: https://www.w3.org/TR/webcodecs/ — WebCodecs models decoded and encoded media as explicit video frames and audio data with timestamps.
- FFmpeg Filters Documentation: https://ffmpeg.org/ffmpeg-filters.html — FFmpeg audio filters combine, delay, trim, and normalize streams by their timing and sample properties.

## Product proof

- `docs/website/static/blog/videoio-audio-mixer-whisper/videoio-decoded-frames.png`
