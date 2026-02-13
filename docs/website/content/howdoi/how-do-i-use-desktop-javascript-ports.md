---
title: USE THE DESKTOP AND JAVASCRIPT PORTS
slug: how-do-i-use-desktop-javascript-ports
url: /how-do-i/how-do-i-use-desktop-javascript-ports/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-desktop-javascript-ports.html
tags:
- pro
description: Build apps that run in PC's and Macs
youtube_id: hCjmHoktlrU
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-9-1.jpg
---

{{< youtube "hCjmHoktlrU" >}} 

#### Transcript

In this short video we will discuss the desktop and JavaScript ports of Codename One both of which are pretty confusing to new developers. We’ll start with the desktop port which is a pro feature. The reason for this is simple, desktop builds are huge and take up a lot of server resources.

A desktop build packages Mac applications as DMG files which is a common distribution format for Mac applications. You end up with the standard drag to install interface familiar from native Mac applications.

On Windows an EXE or an MSI file can be generated as a result, this is determined by build hints.

Under the hood the binaries on both platforms include a JRE within the installer. This is internal to the app and isn’t exposed to the user. The JRE is private to the app.

This is implemented thru the standard Java packager tool from the JDK. The Java packager tool requires a Mac for Mac apps and a Windows machine for Windows apps. This isn’t a problem with Codename One as we have build servers for both OS’s.

Notice that even though these are desktop builds the apps will still look like Codename One apps and often look like tablet apps. This isn’t a problem for some developers and we already use this internally. Our new GUI builder is built completely in Codename One and demonstrates the flexibility that this approach can deliver.

The JavaScript port of Codename One is an enterprise feature.

Notice that this tool doesn’t generate a website, it generates something that feels like an app. For some use cases this is better but you should check the feel of our demo applications first before making choices.

The JavaScript code uses TeaVM to statically translate Java bytecode to obfuscated JavaScript and even compiles the code in such a way that thread code works. This allows everything in Codename One to work including the event dispatch thread etc.

Graphics and the whole UI are rendered thru the HTML5 Canvas API which draws the elements on the browser just like it would draw them on a mobile device.

This generates code that runs completely on the client just like any other native OS port. This isn’t a tool that communicates to the server to fetch application state or run logic there. There is one big limitation in JavaScript itself that doesn’t exist in native mobile devices: same origin. JavaScript can’t open a network connection to any server other than the one it was delivered from. To workaround this we provide a proxy servlet that can proxy the requests from your application to any arbitrary server.

I didn’t mention the UWP port before, UWP stands for Universal Windows Platform and it’s the new approach for Native Windows supported by Microsoft. These 3 platforms have a lot of overlap between them and some users choose to target all 3 of them. However, your millage may vary so here is a brief comparison.

Desktop is effectively one port with 2 targets: Mac and Windows 8 or newer. JavaScript should work pretty much everywhere although a reasonably modern browser is required, performance might not be great when running on some of the older mobile devices though. The Universal Windows platform is a part of Windows 10 so you would need that at a minimum…

The desktop port only works on intel CPU’s. That does include a lot of the Windows tablets or convertible devices but not all of them. JavaScript should pretty much work on anything and UWP should work on any Windows phone, tablet or desktop as long as it’s running Windows 10 or newer.

Shipping is where these differ a great deal. It’s hard to ship the desktop apps in the stores. You would need to provide downloads in your website or distribute installers in your organization. JavaScript should be shipped via a webserver although it’s theoretically possible to package it this isn’t something that’s currently supported. This might be great but it might be a problem as stores provide a lot of value with things such as in-app-purchase. A UWP app can be shipped through the Microsoft store which is convenient for some use cases.

License requirements from you vary a great deal, desktop requires pro. JavaScript requires enterprise and UWP is available for everyone. Notice that even if you cancel your subscription, your distribution license isn’t revoked! So you are legally allowed to upgrade to enterprise, build a JavaScript version and cancel your subscription.

And finally native interfaces are a crucial way to extend a port and hugely important. The desktop port uses JavaSE under the hood. That means you can just invoke any feature in Java including JNI when you need a native interface. This is very convenient and has been a deciding factor for many developers. The “native” for the JavaScript port is JavaScript. This allows you to access JavaScript functionality that isn’t exposed but even when you use native within the JavaScript port you are still stuck in the sandbox of the browser due to the inherent limitations of the web platform.

The UWP port uses C# as the native language. Notice that this is the UWP variant of C# which has a few restrictions and limitations.

You can debug desktop behavior right in the simulator by picking the builtin Desktop skin. This skin is resizable and allows you to isolate behaviors that occur only in a resizable app.

You can just send a desktop or JavaScript build from the right click menu. It’s pretty easy. Notice there is a lot of nuance in both so check out our developer guide and pro/enterprise support if you run into issues!

Thanks for watching, I hope you found this helpful

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
