# Promotional Videos, Device Skins and Foldables

Read this when the user asks for a **demo or promotional video** of the app, screenshots
on a **specific phone**, a **skin for a device** the simulator does not ship, or how the
app behaves on a **foldable**. Everything here runs from the shell, with **no window on
screen**, so the developer can keep working while you produce it.

## 1. Start the simulator hidden

```bash
./mvnw cn1:agent -Dcn1.agent.hidden=true &          # Gradle: ./gradlew cn1Agent -Pcn1.agent.hidden=true &
S=.agent-skills/codename-one/tools/Sim.java
java $S wait
```

Hidden mode lays the window out and renders into it, but never shows it: screenshots,
recordings and every MCP tool behave exactly as with a visible window. The environment
variable `CN1_SIMULATOR_HIDDEN=1` does the same for any way of starting the simulator.
On Linux with no display `cn1:agent` still starts Xvfb itself.

To run on a particular device, point the simulator at its skin before starting it:

```bash
CN1_SIMULATOR_SKIN=$PWD/target/pixel9.skin ./mvnw cn1:agent -Dcn1.agent.hidden=true &
```

## 2. Record a video that looks good

```bash
java $S record-start target/demo.mp4 '{"frame":"skin","fps":60,"touches":"ripple"}'
java $S tap "Sign In" ; java $S caption "One tap sign-in"
java $S record-stop
```

- `frame: "skin"` records the **device in its bezel** on a 16:9 backdrop, at one constant
  size: this is the framing for a promo. `frame: "screen"` records the bare screen.
- `touches` (`ripple`, `dot`, `none`) shows where taps land; `caption` overlays text.
- `fps` 60 for anything with motion. ffmpeg must be on the PATH.
- `java $S call sim_rotate '{"orientation":"landscape"}'` turns the device with a 3D
  animation that the recording captures.

### Scripted clips are smooth by construction

Pass a `script` and the recording becomes **lockstep**: every video frame is rendered for
its own moment of the animation, so folds and turns play smoothly at the requested frame
rate however slowly the machine renders. Steps are timed in milliseconds from the start:

```bash
java $S record-start target/tour.mp4 '{"frame":"skin","fps":60,"touches":"none","script":[
  {"at":1200,"posture":"closed","animateMs":900},
  {"at":3600,"posture":"flat","animateMs":900},
  {"at":6000,"posture":"half","animateMs":900},
  {"at":8400,"orientation":"landscape"},
  {"at":11000,"orientation":"portrait"}]}'
sleep 14
java $S record-stop
```

`record-stop` waits until the video is as long as the wall-clock time since
`record-start`, so call it when the last step has had time to play (here 11 s plus the
2.6 s turn). Rendering every frame takes longer than real time; that is expected. The app's own
animations still run on the wall clock, so drive UI interaction **before** or **after** a
fold sequence rather than during one, or keep it in an unscripted recording.

A human gets the same tour from the simulator menu: **Simulate -> Record Device Tour...**.

## 3. Foldables

A foldable skin (the bundled `iPhoneDuo.skin`, or one you build in section 4) draws the
device in 3D: closing it moves the app to the cover display, half folding it raises
posture events, and both halves stay live and tappable. Screens are never shaded, so what
the user sees is always the app's real pixels.

```bash
java $S fold half            # flat | half | closed, animated
java $S call sim_fold '{"angle":110,"animateMs":600}'
```

Check the app at each posture: closed (cover screen size and safe area), half (does the
layout split across the hinge -- `Display.getDevicePosture()`, `getFoldBounds()`), flat.
`references/mobile-adaptability.md` covers reacting to posture in code.

## 4. Build a skin for any phone

`tools/SkinTool.java` builds a simulator skin with the same output as the Skin Designer
(`https://www.codenameone.com/skindesigner/`), from the **current** device database (over
5,000 phones and tablets, fetched from GitHub and cached for a day under `~/.codenameone`).

```bash
T=.agent-skills/codename-one/tools/SkinTool.java
java $T devices "pixel 9"                # search: id, name, resolution, ppi, size, cutout
java $T devices --foldable               # every foldable
java $T devices --brand Samsung --platform android --limit 20
java $T device google_google_pixel_9            # the full record as JSON
java $T create target/pixel9.skin --device google_google_pixel_9
java $T create target/duo.skin --device apple_iphone_duo
java $T create target/custom.skin --width 1080 --height 2400 --ppi 420 --cutout hole \
     --corner-radius 120 --platform android
java $T --help                           # every override (bezel, colour, safe areas, fold options)
```

A phone that is not in the database is built from its published resolution, density and
cutout. Foldables take `--foldable`, `--fold-orientation vertical|horizontal`
(book or flip), `--fold-type continuous|dualScreen`, and the cover display's size and
density. Load the result with `CN1_SIMULATOR_SKIN` (section 1), take a screenshot and
look at it before using it in a video.

## What to hand back

The video or screenshot paths, the skin file if you built one, and a frame or two you
looked at yourself (`ffmpeg -i demo.mp4 -vf "select=not(mod(n\,60)),tile=4x3" -frames:v 1 sheet.png`).
Stop the simulator you started (`kill %1`).
