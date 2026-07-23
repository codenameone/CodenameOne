# Evidence map

Source: `docs/website/content/blog/mac-native-builds-and-desktop-integration.md`
Canonical: https://www.codenameone.com/blog/mac-native-builds-and-desktop-integration/

## Thesis

Turning the iOS native pipeline into a real Mac app with desktop integration

## Supported beats

- **A native Mac build from the iOS pipeline:** PR #5053 adds a Mac Native target that takes the existing project through the same build as the iPhone builder and the ParparVM pipeline that produces an iOS app. In this case it emits a native Mac variant of it.
- **Certificates:** There's one major gap with the Mac target: signing. Right now our certificate wizard, settings etc. are geared towards iOS/Android. Mac uses a different store and different signing tools. We didn't update all of that infrastructure yet, and it might take some time to update.
- **Desktop integration:** PR #5136 and the follow-up PR #5170 make a desktop target behave like a desktop app rather than a tablet app in a window. Everything here is opt-in, on by default for newly generated apps, and completely inert on mobile or when disabled.
- **Window chrome and the OS title bar:** In native mode the Codename One Toolbar is suppressed, the form title goes to the OS title bar, and your commands are bridged to a real native menu bar (a Swing JMenuBar that becomes the macOS screen menu on JavaSE, a UIMenuBuilder menu on Mac Catalyst).
- **Commands land in the right menu:** setDesktopMenu(...) takes any of DESKTOP_MENU_APP, ABOUT, PREFERENCES, QUIT, FILE, EDIT, VIEW, WINDOW, HELP, or a custom top-level title string, so Preferences and Quit show up where a Mac user expects them.
- **Interactive scrollbars:** Desktop scrollbars are now grab-and-drag with a draggable thumb, click-track paging, and an always-visible track, following the macOS and Material conventions. The thumb shows its hover style under the pointer and its pressed style while dragged, and a minimum thumb size keeps it grabbable on very long content.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5053
- https://github.com/codenameone/CodenameOne/pull/5136
- https://github.com/codenameone/CodenameOne/pull/5170
