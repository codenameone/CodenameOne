---
title: CREATE AN IOS PROVISIONING PROFILE
slug: how-do-i-create-an-ios-provisioning-profile
url: /how-do-i/how-do-i-create-an-ios-provisioning-profile/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-create-an-ios-provisioning-profile.html
tags:
- featured
- signing
description: Signing & provisioning on iOS is a bit painful, hopefully this will aleviate
  some of the pain
youtube_id: OWHizrNyizQ
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-24.jpg
---
{{< youtube "OWHizrNyizQ" >}}

Creating an iOS provisioning profile is really about assembling a matching set of Apple-side credentials and identifiers so that the app you build, the devices you test on, and the certificate you sign with all agree with each other. If any one of those pieces is misaligned, the build may still complete, but installation or submission will fail later.

The first step is to make sure the app identifier is correct. In Apple terms, the App ID needs to match the package identifier of your Codename One application. This is one reason package naming matters so much early in a project. Once provisioning, signing, and store submission are involved, renaming is much more annoying than it looks.

From there, development and distribution provisioning split into two different jobs. Development provisioning is for testing on specific physical devices. That means the devices must be registered with the Apple developer account, and the development profile must explicitly include them. Distribution provisioning is for App Store delivery and does not include test devices in the same way.

Certificates are the next piece. You need the right development and distribution signing certificates, and you need them exported in a form the build process can use. The old video explains this through Apple’s portal and the Keychain export flow, and that basic idea is still correct even though Apple’s UI changes over time. The stable rule is simple: the certificate, the provisioning profile, and the app identifier must all refer to the same app and account context.

Once those files exist, Codename One needs to know where they are and what passwords protect them. That is the bridge between the Apple-side account setup and the actual build. If the certificates import correctly and the provisioning profile matches the project, sending an iOS build becomes routine. If they do not, the error usually appears later in a frustrating way, which is why getting the identity chain right up front matters so much.

The video is useful as a picture of the overall flow, but the modern thing to remember is that Apple’s portal screens change while the underlying relationships do not. You always need the same core pieces: a matching app ID, development devices for local testing, development and distribution profiles, and exportable certificates that line up with them.

## Further Reading

- [Build Server](/build-server/)
- [Development Environment](/development-environment/)
- [Hello World](/hello-world/)
- [Developer Guide](/developer-guide/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
