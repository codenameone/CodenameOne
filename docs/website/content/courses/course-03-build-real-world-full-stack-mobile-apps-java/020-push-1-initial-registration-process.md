---
title: "Push 1 - Initial Registration Process"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Push and In-App Purchase"
module_key: "08-push-and-in-app-purchase"
module_order: 8
lesson_order: 1
weight: 20
is_course_lesson: true
description: "Set up the Android and iOS pieces required before a Codename One app can receive push notifications."
---
> Module 8: Push and In-App Purchase

{{< youtube SBqYZSdIdec >}}

Push usually feels harder than it really is because the setup is split across several systems. You need Android-side configuration, iOS-side configuration, and project settings that tell Codename One how those pieces fit together. Once that groundwork is done, the rest of the push flow is much easier to understand.

On Android, the core job is to register the app with Google's push infrastructure and copy the sender information back into the project. The video uses the old Google Cloud Messaging terminology. Today the equivalent work is done through Firebase Cloud Messaging, but the role of the value is the same: Android needs to know which sender the application belongs to so it can receive notifications correctly.

In Codename One, that sender information ends up in the project configuration as a build hint. The video shows the legacy settings and plugin-based workflow, which is now out of date. In a modern Maven-based project you still provide the same information, but you manage the project through Maven and the current Codename One configuration flow instead of relying on the old IDE plugin setup.

The Android step is the easier half. iOS is where push becomes certificate-driven. Push on iOS does not just reuse your normal signing setup. It requires push-enabled certificates and provisioning profiles that match the app. That is why the original lesson spends so much time on the certificate wizard.

One warning from the video is still very important: if you already have working certificates, do not revoke them casually just because a wizard offers that option. Replacing or revoking an existing certificate can break other apps that depend on it. If this is your first push-enabled setup, generate what you need. If you already have a valid certificate chain, reference and reuse the existing material instead of resetting it blindly.

iOS also separates development and production push environments. A debug build installed directly on a device is not the same thing as a production build distributed through the App Store, and the push credentials need to match that distinction. If push works in one environment but not the other, this mismatch is one of the first things to check.

Once the iOS setup is complete, Codename One gives you the cloud certificate URLs and related values that the server side will later need when it sends notifications. Keep those values somewhere safe. They are not part of client registration, but they are essential later when the app server needs to deliver a push to a device.

At this stage, the goal is not to send a notification yet. The goal is to leave the project correctly identified on Android, correctly provisioned on iOS, and ready for the client and server code in the next lessons.

## Further Reading

- [Push 2 - Client Side Code](/courses/course-03-build-real-world-full-stack-mobile-apps-java/021-push-2-client-side-code/)
- [Push 3 - The Server Side and Build Logic](/courses/course-03-build-real-world-full-stack-mobile-apps-java/022-push-3-the-server-side-and-build-logic/)
- [Push Notifications](/push-notifications/)
- [Build Hints](/build-hints/)
