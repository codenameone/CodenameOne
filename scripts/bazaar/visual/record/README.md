# Walkthrough recordings

The scripts that make the Bazaar walkthrough videos on the demo page and in
`~/Downloads/bazaar-videos`. Nothing opens a window on the host: the app tours
run in podman under Xvfb, the web tours in headless Chromium.

## App walkthroughs

```bash
visual/record/rec.sh <maven-repo>        # e.g. ../../.m2-repo
```

`DemoTourTest` drives each tour and the simulator's recorder captures it.
`RUNS` picks the tours (`tour:device:skin`, default all five:
`shop:phone:GooglePixel pos:tablet:iPadAir2 merchant:desktop:- admin:desktop:- pos:desktop:-`);
skins are copied from `SKINS` (default `~/.codenameone`). Videos land in
`target/record/out/video`, the guide screenshots in `target/record/out/shots`.

Two things make the result usable, and both are easy to lose:

- **The recorder captures the `SCREEN` frame**, so a phone or tablet video is
  the device's own pixels at its own aspect ratio, and a desktop video is the
  window's content. The `SKIN` frame is the whole skin canvas with the device
  small in the middle of it.
- **ffmpeg is installed in the image** (`visual/Containerfile`). Without it the
  recorder falls back to writing every frame as a PNG; at device resolution
  that encode is far slower than the frame clock, the bounded queue fills, and
  the capture loop repeats the last frame to keep time -- the video plays at
  the right speed but stutters at a handful of distinct frames a second. With
  ffmpeg the raw frames go straight down a pipe to libx264. `FPS` (default 60)
  sets the rate.

## Encoding

```bash
visual/record/enc.sh target/record/out/video/bazaar-shopper-phone.mp4 bazaar-shopper-phone 960
```

writes `target/record/enc/full/<name>.mp4` (own size and rate), a docs copy
fitted inside the box given (`docs/<name>.mp4` plus `-poster.jpg`) and a
contact sheet for review. Optional 4th and 5th arguments are the docs CRF and
frame rate; keep the docs copies under about 3 MB.

Measure what a video really delivers rather than trusting its nominal rate:
`ffmpeg -i v.mp4 -vf mpdecimate -fps_mode vfr -f framemd5 -` counts the
frames that differ from the previous one.

## Web walkthroughs

```bash
ORIGIN=http://localhost:18097 OUT=target/record/web SCHEME=light FLOW=storefront node visual/record/web-rec.mjs
```

records the server-rendered pages (`FLOW` is `storefront` or `account`,
`SCHEME` `light` or `dark`) against a running backend; Playwright must be
resolvable from the working directory. It keeps every screencast frame with the
timestamp the browser painted it at, writes an ffconcat that holds each frame
for its real duration, and encodes that at a constant 60 fps, so motion keeps
its timing. Scrolling is an eased `requestAnimationFrame` tween clamped to the
page's ends, never a jump. The back office in a browser is the app at `/app/`,
recorded with the app's own walkthroughs.
