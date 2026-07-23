# Evidence map

Source: `docs/website/content/blog/liquid-glass-material-3-modern-native-themes.md`
Canonical: https://www.codenameone.com/blog/liquid-glass-material-3-modern-native-themes/

## Thesis

Adopting Liquid Glass and Material 3 through controlled custom-rendered themes

## Supported beats

- **Try it right now in the Playground:** The easiest way to see any of this is the Playground. The Playground now defaults to iOS Modern when the device toggle is set to iPhone and Android Material 3 when it is set to Android, in both light and dark mode.
- **The new native themes:** For most of Codename One's life the iOS native theme has been the venerable iOS 7 flat theme, and the Android native theme has been Holo Light.
- **iOS Modern:** This is the ShowcaseTheme capture from the new screenshot suite, run on iOS in light and dark. Same Form, same components, swap Display.setDarkMode(...) and re-resolve. The form is built like this.
- **Android Material 3:** Same ShowcaseTheme source on Android. The Material 3 baseline palette gives Default the primary container color and Raised the elevated-surface tone, with the dark variant flipping the relationship correctly via the dark color-role mapping.
- **Translucent surfaces:** This is the DialogTheme capture against the screenshot suite's textured diagonal-stripe backdrop. The backdrop is intentional — it lets reviewers see whether anything that is supposed to be translucent actually is.
- **Runtime palette overrides:** The native theme is meant to be a starting point — you can layer your own palette on top without forking the theme.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/blob/master/native-themes/ios-modern/theme.css
- https://github.com/codenameone/CodenameOne/blob/master/native-themes/android-material/theme.css
- https://github.com/codenameone/CodenameOne/blob/master/scripts/hellocodenameone/common/src/main/java/com/codenameone/examples/hellocodenameone/tests/DarkLightShowcaseThemeScreenshotTest.java
- https://github.com/codenameone/CodenameOne/issues/4807
- https://github.com/codenameone/CodenameOne/tree/master/scripts/hellocodenameone/common/src/main/java/com/codenameone/examples/hellocodenameone/tests
- https://github.com/codenameone/CodenameOne/issues/4825
- https://github.com/codenameone/CodenameOne/issues/4781
- https://github.com/codenameone/CodenameOne/issues/4838
- https://github.com/codenameone/CodenameOne/issues/4841
- https://github.com/codenameone/CodenameOne/issues/4819
- https://github.com/codenameone/CodenameOne/issues/4824
- https://github.com/codenameone/CodenameOne/issues/4811
