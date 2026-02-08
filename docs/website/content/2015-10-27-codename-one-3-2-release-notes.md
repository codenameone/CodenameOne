---
title: "Codename One 3.2 Release Notes"
date: 2015-10-27
slug: "codename-one-3-2-release-notes"
---

# Codename One 3.2 Release Notes

1. [Home](/)
2. 3.2 Release Notes

### Summary

Version 3.2 sets the pace for many upcoming features & migration processes such as the new cloud infrastructure for push servers, modernized GUI builder etc.

### Highlights - Click For Details

New GUI Builder (technology preview)

The new GUI builder is a big departure from our existing designer tool. This tool is now in "technology preview" status meaning that its not quite ready for prime time but we want feedback on its direction and issues.  
Read more about this work in [this blog post](/blog/new-gui-builder.html).

Local Notifications on iOS and Android

Local notifications are similar to push notifications, except that they are initiated locally by the app, rather than remotely. They are useful for communicating information to the user while the app is running in the background, since they manifest themselves as pop-up notifications on supported devices.  
Read more about this work in [this blog post](/blog/local-notifications.html).

Introduced New Push Server Architecture

We completely overhauled the way Codename One handles push services and added several long time RFE's to the mix.  
Read more about this work in [this blog post](/blog/new-push-servers.html).

Added Ability for cn1libs To Include Build Hints

cn1libs now include the ability to include build hints thus integrate more seamlessly without complex integration instructions.  
Read more about this work in [this blog post](/blog/deprecations-simplified-cn1lib-installs-theme-layering.html).

Improved iOS/Android Rendering Speed

Thanks to a community contribution we took a deep look at the rendering code and are using faster code for tiling/string rendering.  
Read more about this work in [this github pull request](https://github.com/codenameone/CodenameOne/pull/1580).

Added A Permanent Side Menu Option

The Toolbar API has really picked up, in order to make it more useful for Tablets we added the ability to keep the SideMenuBar that's builtin to it always on.  
Read more about this work in [this blog post](/blog/permanent-sidemenu-getAllStyles-scrollbar-and-more.html).

Get All Styles - Simplified Handcoding Theme Elements

getAllStyles() allows writing code that is more concise to perform an operation on multiple style objects at once.  
Read more about this work in [this blog post](/blog/permanent-sidemenu-getAllStyles-scrollbar-and-more.html).

Added Support For Facebooks "Invite A Friend"

New integration for Facebooks "invite a friend" feature that simplifies viral marketing for your app.  
Read more about this work in [this blog post](/blog/invite-friends-websockets-windows-phone-more.html).

Terse Syntax For Building UI's

A shorter syntax for adding components and labels into the UI resulting in less code for the same functionality.  
Read more about this work in [this blog post](/blog/terse-syntax-migration-wizard-more.html).

Java 8 Language Features are now on by default

We fixed many things in this implementation over the past three months and feel confident enough to switch this into the default.  
Read more about this work in [this blog post](/blog/java-8-support.html).

### Details

- Added helper methods to `RadioButton` to create toggle buttons in a more concise way

- Tuned `SpanLabel` to avoid unnecessary line breaks

- Fixed an issue with `URLImage` & `ImageViewer` that caused the images not to download in some cases

- Fixed `BigDecimal` and `BigInteger` to behave more like their `java.lang` counterparts

- Fixed alpha handling in `fillShape()` on iOS as part of issue #1594

- Improved multiline support in `ContainerList` which seems to have regressed

- Fixed stack overflow with `SocketConnection` on iOS issue #1581

- Added ability to customize the completion list of the `AutoCompleteTextField` via code

- Fixed language id's for iOS 9 which started adding variants into language codes

- Fixed race condition in the AppArg property on iOS

- Fixed an issue with reloading a resource file using a different DPI

- Added ability to customize the long press interval

- Added ability to create a `Container` that encapsulates a component or group of components with one line of code

- Added ability to specify a `SimpleDateFormat` for a picker to allow a more custom look

- Enhancements for issue #1572 that log dangling cursors in the SQL API into the console

- Fixed issues with the `Timer`API on iOS

- Fixed issue with `SCALE_TO_FILL` in `URLImage`, when rounding causes `IllegalArgumentException`

- Added constructor to border layout for simpler/shorter code

- Added `validateToken` to the Login framework

- Fixed null pointer on `String.valueOf(Object)` in the iOS VM

- Added helper methods to `UIManager` to reduce the boilerplate when initializing projects

- Fixed vertical position in toolbar apps with morph transition

- New utility methods to simplify sleep/wait calls

- Added ability to mask images fetched thru URL image.
- Added many new shortcut methods such as add methods to containers and text field getter for integer, new constructors for text fields.
- Fixed upload to provide progress indication on iOS
- Added ability for dialogs to dynamically adapt their size
- Added command support to `SpanButton`
