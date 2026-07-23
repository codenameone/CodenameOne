# Evidence map

Source: `docs/website/content/blog/swift-and-kotlin-native-interfaces-faster-base64.md`
Canonical: https://www.codenameone.com/blog/swift-and-kotlin-native-interfaces-faster-base64/

## Thesis

Writing native interfaces in Swift and Kotlin while keeping a portable Java boundary

## Supported beats

- **Native Interfaces Now Support Swift and Kotlin:** Native interfaces are one of the most important features in Codename One. They guarantee you won’t be stuck if you need something that we can’t/won’t deliver. They let our community extend Codename One in ways we couldn’t possibly imagine.
- **Typical File Locations:** By default, the generation process still produces the traditional Java and Objective-C stubs. That is intentional. We do not want to disrupt existing projects or workflows.
- **Swift on iOS:** The main detail to keep in mind is that the runtime still expects the same implementation naming convention. So if you implement the class in Swift, you should keep the generated implementation name and annotate it with @objc(...) so the runtime can discover it properly.
- **Kotlin on Android:** Some Android APIs, SDK samples, and documentation are Kotlin-first now. When you are integrating something native, it might be the path of least resistance.
- **Base64 Performance Improved Dramatically:** Base64 sits in all kinds of important paths: encoded assets, payload handling, persistence, transport, auth flows, and platform bridges. If it is slow, you feel it in places that are hard to diagnose and annoying to optimize.
- **Android Results:** That is a very significant swing, especially on decoding. But I especially love how our API beats the pants off of Android’s native API...

## Referenced evidence

- https://github.com/codenameone/CodenameOne/issues/3274
- https://developer.apple.com/fonts/
- https://github.com/codenameone/CodenameOne/issues
