# Evidence map

Source: `docs/website/content/blog/apple-tv-and-android-tv.md`
Canonical: https://www.codenameone.com/blog/apple-tv-and-android-tv/

## Thesis

Treating TV as a first-class form factor with runtime checks and CSS media variants

## Supported beats

- **Why TV is its own form factor:** A phone and a TV are both screens, but the interaction model is not the same. You sit ten feet away from a TV, so text and focus rings that look fine on a handset become unreadable.
- **The form-factor API and the @media styling story:** The detection API mirrors the existing isWatch(). You get CN.isTV(), Display.isTV(), and CodenameOneImplementation.isTV(). On iOS that's backed by a new isRunningOnTV() native call wired to TARGET_OS_TV. On Android it checks the television/leanback feature with a UiModeManager fallback.
- **Android TV: one APK, one build hint:** Android TV is the low-code switch. You don't build a separate artifact. The same APK runs on phones, tablets, and the TV. You turn it on with a build hint.
- **Apple TV: a separate Metal target:** Apple TV can't ride the same binary. tvOS needs its own Xcode target, the same way Mac Catalyst does. The new TvNativeBuilder adds an appletvos target with TARGETED_DEVICE_FAMILY=3, renders through Metal, and excludes the OpenGL-only .m files (tvOS has Metal and most of the iOS APIs, but no OpenGL ES).
- **Turning it on:** There is no separate SDK to learn. On Android you set android.tv=true and the same APK gains a TV launcher; on iOS you point the build at the tvOS target with codename1.tvMain and TvNativeBuilder generates the appletvos target.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5261

## Independent problem evidence

- Apple tvOS Human Interface Guidelines: https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos — Apple's television guidance treats focus movement, remote input, and distance as core interaction constraints.
- Android TV App Quality: https://developer.android.com/docs/quality-guidelines/tv-app-quality — Google requires television launch behavior, remote navigation, readable layouts, and TV-specific assets.

## Product proof

- `docs/website/static/blog/apple-tv-and-android-tv/chatview-appletv.png`
