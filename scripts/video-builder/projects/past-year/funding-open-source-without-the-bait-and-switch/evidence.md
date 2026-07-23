# Evidence map

Source: `docs/website/content/blog/funding-open-source-without-the-bait-and-switch.md`
Canonical: https://www.codenameone.com/blog/funding-open-source-without-the-bait-and-switch/

## Thesis

A sustainable open-source business boundary that does not sabotage the free product

## Supported beats

- **The deal we actually offer:** Codename One the open source project and Codename One the company are not the same thing, and that distinction is the whole game.
- **A privacy-first analytics API:** PR #5266 replaces the old Google Analytics v1 AnalyticsService with the generic provider SPI shown above.
- **Maps you control, down to the pixel:** PR #5264 brings mapping back into core and modernizes it, retiring the old tile-based MapComponent and the external Google Maps cn1lib. Two components share one MapSurface API.
- **Apple TV, Android TV, and CSS that knows the form factor:** PR #5261 adds Apple TV (tvOS) and Google TV (Android TV) support, modeled on the Apple Watch port from last week. The same CN.isTV() branch you would expect, the same single Android APK running on phones, tablets and the television, and on iOS a separate tvOS Xcode target driven by a build hint.
- **Rich text and code editing:** PR #5272 adds two visual editors. RichTextArea is a WYSIWYG HTML editor, bold, italic, lists, links, colors, headings, with getHtml and setHtml.
- **Device integrity and app review:** Two smaller APIs round out the week, both built in core rather than as cn1libs. PR #5277 adds DeviceIntegrity, a portable runtime self-protection API for high-security apps: Play Integrity and iOS App Attest attestation, root, jailbreak and Frida detection, and an accessibility-service abuse guard, most of it driven by build hints with a runtime API on top.

## Referenced evidence

- https://debugagent.com/open-source-bait-and-switch
- https://github.com/codenameone/CodenameOne/pull/5266
- https://stats.example.com
- https://github.com/codenameone/CodenameOne/pull/5264
- https://github.com/codenameone/CodenameOne/pull/5261
- https://github.com/codenameone/CodenameOne/pull/5272
- https://github.com/codenameone/CodenameOne/pull/5277
- https://github.com/codenameone/CodenameOne/pull/5268
- https://github.com/codenameone/CodenameOne/pull/5250
- https://github.com/codenameone/CodenameOne/pull/5253
- https://github.com/codenameone/CodenameOne/issues
- https://www.codenameone.com/discussion-forum.html

## Independent problem evidence

- GNU Classpath License: https://www.gnu.org/software/classpath/license.html — GNU Classpath explains how the exception permits linking independent modules with the library without placing the resulting application under the GPL.
- Open Source Guides: Getting Paid: https://opensource.guide/getting-paid/ — Open Source Guides documents services, support, crowdfunding, and other ways maintainers can fund ongoing work rather than relying on invisible unpaid labor.

## Product proof

- `docs/website/static/blog/privacy-first-analytics/analytics-console-overview.png`
- `docs/website/static/blog/vector-and-native-maps/maps-vector.png`
- `docs/website/static/blog/rich-text-and-code-editing/components-codeeditor.png`
