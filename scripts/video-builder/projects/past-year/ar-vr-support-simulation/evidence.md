# Evidence map

Source: `docs/website/content/blog/ar-vr-support-simulation.md`
Canonical: https://www.codenameone.com/blog/ar-vr-support-simulation/

## Thesis

How a simulated room makes AR and VR code testable before reaching a device

## Supported beats

- **The Problem With Writing AR Code:** AR development has a miserable inner loop. The code only runs on a device, the interesting states are the ones you can't reproduce on demand (tracking loss, a plane that merges into another, a reference image entering the frame), and every debug cycle involves standing up and pointing a phone at your floor.
- **The AR Package:** The API follows the same shape as the camera package: check support, open a session with options, get one view. World tracking, plane detection (horizontal and vertical), hit testing, anchors, light estimation, image tracking, and face tracking are all in.
- **The Simulated Room:** The simulator ships a full AR backend modeled on the Android emulator's virtual scene. Open a session and the simulator "detects" the floor of a virtual room after a realistic delay, the same way real plane detection takes a moment.
- **The VR Package:** com.codename1.vr takes the opposite approach: no platform SDK at all. It's pure core code built on the existing com.codename1.gpu pipeline and the motion sensors API from last week, so it runs anywhere isGpuSupported() is true, including the simulator.
- **What It Costs Your Build: Nothing, Unless You Use It:** The build pipeline treats AR like the camera and car APIs: referencing com.codename1.ar is what injects the camera permission, the ARCore Gradle dependency, and the manifest entries, and marks ARCore as optional so your app still installs on devices without it (flip android.ar.required=true for an AR-only app).
- **Scoped Out, On Purpose:** Two things are deliberately missing rather than stubbed: 360 video and lens distortion correction. Video needs a media-to-texture path and distortion needs render-to-texture, and we'd rather ship no API than a placeholder that changes shape later.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5335

## Independent problem evidence

- Stack Overflow: https://stackoverflow.com/questions/45034240/is-it-possible-to-run-arkit-in-simulator/45034297
- Reddit: https://www.reddit.com/r/augmentedreality/comments/by4v4p/arkit_3_with_ar_foundation_in_unity3d_getting/
- Google ARCore emulator requirements: https://developers.google.com/ar/develop/java/emulator
- Google AR platform environments: https://developers.google.com/ar/
- Apple ARKit device support: https://developer.apple.com/documentation/arkit/verifying-device-support-and-user-permission
