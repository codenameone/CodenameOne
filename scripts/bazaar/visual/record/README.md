# Walkthrough recordings

The scripts that make the Bazaar walkthrough videos on the demo page and in
`~/Downloads/bazaar-videos`. Nothing opens a window on the host: the app tours
run in podman under Xvfb, the web tours in headless Chromium.

## App walkthroughs

```bash
visual/record/rec.sh <maven-repo>        # e.g. ../../.m2-repo
```

`DemoTourTest` drives each tour and the simulator's recorder captures it.
`RUNS` picks the tours (`tour:device:skin`, default all seven:
`shop:phone:GooglePixel pos:tablet:iPadAir2 merchant:desktop:- admin:desktop:- pos:desktop:-
merchant:tablet:iPadAir2 pos:phone:GooglePixel`; the merchant tablet run makes the
guide's `*-tablet` back-office shots);
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
resolvable (`NODE_PATH=<checkout>/scripts/node_modules`) and `OUT` must be an
absolute path. Start the backend on a port of its own (`backend/server.sh run
--jvm --prebuilt --port 18097` with `CN1_PROFILE=test`), never 8095.

Two capture modes share one timeline:

- **Between scrolls** a CDP screencast keeps every frame with the timestamp the
  browser painted it at, and an ffconcat holds each frame for its real
  duration. Clicks, typing and captions keep their natural timing.
- **A scroll is stepped**, never animated: the page is set to each 60 fps
  position of an ease-in-out curve (clamped to the document's ends, with
  `overscroll-behavior: none`, so it never bounces) and one
  `Page.captureScreenshot` is taken per step, stamped exactly 1/60 s after the
  previous one. Later screencast frames are shifted so the timeline continues
  from the end of the scroll, however long the screenshots took. A screencast
  delivers only the paints the browser gets round to -- about 21 distinct
  frames a second during a long scroll -- so a scroll recorded that way
  stutters.

The result is encoded at a constant 60 fps. `scrolls-FLOW[-dark].json` lists
the scroll windows in video seconds. Measure them with a sensitive
`mpdecimate`; the default thresholds drop the 1-3 px steps at either end of an
eased scroll as "duplicates" and report about 21 fps for a scroll in which
every frame differs:

```bash
ffmpeg -i bazaar-web-storefront.mp4 -vf mpdecimate=hi=64:lo=32:frac=0.1 -fps_mode vfr -f framemd5 -
```

(about 53 distinct fps in the scroll windows, and none on a still screen). The
back office in a browser is the app at `/app/`, recorded with the app's own
walkthroughs.
