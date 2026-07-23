# Evidence map

Source: `docs/website/content/blog/motion-input-form-factors.md`
Canonical: https://www.codenameone.com/blog/motion-input-form-factors/

## Thesis

One input model for sensors, stylus, trackpads, wheels, and foldable posture

## Supported beats

- **Motion Sensors And Gestures:** The new com.codename1.sensors package exposes accelerometer, gyroscope and magnetometer readings. It also derives gravity, linear acceleration and orientation in the core when the platform does not provide them directly.
- **Rich Pointer Detail:** Classic pointer callbacks tell you where a press happened. Modern input asks more questions: was it a mouse, a finger, a stylus, or an eraser? Which button? What pressure? Was the pen tilted? Was the pointer hovering?
- **Wheel, Trackpad And Context Menus:** WheelEvent is now the cross-platform scroll-gesture event. It handles mouse wheels, precision trackpads, horizontal scrolling, iOS/iPadOS pointer devices and the Apple Watch Digital Crown through the same path.
- **Stylus And Foldables:** On Android, foldable support is opt-in with android.foldableSupport=true, so apps that do not need the AndroidX window library do not carry it. The simulator can generate posture changes for testing.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5310
- https://github.com/codenameone/CodenameOne/pull/5309

## Independent problem evidence

- W3C Pointer Events: https://www.w3.org/TR/pointerevents3/ — The web standard defines one pointer model with device type, buttons, pressure, tilt, contact geometry, and capture.
- Android Foldable App Quality: https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/learn-about-foldables — Android's window APIs report folding features and posture so layouts can respond to tabletop and separating-hinge configurations.

## Product proof

- `docs/website/static/blog/motion-input-form-factors.jpg`
