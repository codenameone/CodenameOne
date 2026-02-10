---
title: Progressive Web Apps
slug: progressive-web-apps
url: /blog/progressive-web-apps/
original_url: https://www.codenameone.com/blog/progressive-web-apps.html
aliases:
- /blog/progressive-web-apps.html
date: '2018-01-29'
author: Steve Hannah
---

![Header Image](/blog/progressive-web-apps/new-features-6.jpg)

PWAs (Progressive Web Apps) are an extremely hot topic right now, and Codename One apps are very well suited to being deployed this way. In case you haven’t been following the PWA buzz, the idea is that it’s a web app that behaves like a native app. When they are first loaded in a user’s browser, they behave like a normal responsive web app, but users can install them to their home screen just like native apps. At which point, they can behave as “offline-first” apps. Parts of this have been available for quite some time, but the concept of PWA brings a lot of little things under a single umbrella.

If this all sounds familiar it’s because the JavaScript port of Codename One was practically built for PWA…​

### Deploying as a Progressive Web App

Out of the box, your app is ready to be deployed as a progressive web app (PWA). That means that users can access the app directly in their browser, but once the browser determines that the user is frequenting the app, it will “politely” prompt the user to install the app on their home screen. Once installed on the home screen, the app will behave just like a native app. It will continue to work while offline, and if the user launches the app, it will open without the browser’s navigation bar. If you were to install the native and PWA versions of your app side by side, you would be hard pressed to find the difference – especially on newer devices.

Below is a screenshot from Chrome for Android where the browser is prompting the user to add the app to their home screen.

![Add app to homescreen banner](/blog/progressive-web-apps/javascript-pwa-add-app-banner.png)

Figure 1. Add app to homescreen banner

If the app is available as a native app, in the Play store, you can indicate this using the `javascript.manifest.related_applications` and `javascript.manifest.prefer_related_applications` build hints. Then, instead of prompting the user to add the web app to their home screen, they’ll be prompted to install the native app from the Play store, as shown below.

![Add native app banner](/blog/progressive-web-apps/javascript-pwa-add-native-app-banner.png)

Figure 2. Add native app banner

__ |  The PWA standard requires that you host your app on over HTTPS. For testing purposes, it will also work when accessed at a `localhost` address. You can use the [Lighthoust PWA analysis tool](https://developers.google.com/web/ilt/pwa/lighthouse-pwa-analysis-tool) to ensure compliance.   
---|---  
  
For more information about Progressive Web Apps see [Google’s introduction to the subject](https://developers.google.com/web/progressive-web-apps/).

#### Customizing the App Manifest File

At the heart of a progressive web app is the [web app manifest](https://developer.mozilla.org/en-US/docs/Web/Manifest). It specifies things like the app’s name, icons, description, preferred orientation, display mode (e.g. whether to display browser navigation or to open with the full screen like a native app), associated native apps, etc.. The Codename One build server will automatically generate a manifest file for your app but you can (and should) customize this file via build hints.

Build hints of the form `javascript.manifest.XXX` will be injected into the app manifest. E.g. To set the app’s description, you could add the build hint:
    
    
    javascript.manifest.description=An app for doing cool stuff

You can find a full list of available manifest keys [here](https://developer.mozilla.org/en-US/docs/Web/Manifest). The build server will automatically generate all of the icons so you don’t need to worry about those. The “name” and “short_name” properties will default to the app’s display name, but they can be overridden via the `javascript.manifest.name` and `javascript.manifest.short_name` build hints respectively.

__ |  The `javascript.manifest.related_applications` build hint expects a JSON formatted list, just like in the raw manifest file.   
---|---  
  
#### Related Applications

One nice feature (discussed above) of progressive web apps, is the ability to specify related applications in the app manifest. Browsers that support the PWA standard use some heuristics to “offer” the user to install the associated native app when it is clear that the user is using the app on a regular basis. Use the `javascript.manifest.related_applications` build hint to specify the location of the native version of your app. E.g.

`javascript.manifest.related_applications=[{"platform":"play", "id":"my.app.id"}]`

You can declare that the native app is the preferred way to use the app by setting the `javascript.manifest.prefer_related_applications` build hint to “true”.

__ |  According to the [app manifest documentation](https://developer.mozilla.org/en-US/docs/Web/Manifest), this should only be used if the related native apps really do offer something that the web application can’t do.   
---|---  
  
#### Device/Browser Support for PWAs

Chrome and Firefox both support PWAs on desktop and on Android. iOS doesn’t support the PWA standard, however, many aspects of it are supported. E.g. On iOS you can add the app to your home screen, after which time it will appear and behave like a native app – and it will continue to work while offline. However, many other nice features of PWA like “Install this app on your home screen” banners, push notifications, and invitations to install the native version of the app, are not supported. It is unclear when, or even, whether Apple will ever add full support; but most experts predict that they will join the rest of the civilized world and add PWA support in the near future.

On the desktop, Chrome provides an analogous feature to “add to your homescreen”: “Add to shelf”. If it looks like the user is using the app on a regular basis, and it isn’t yet installed, it will show a banner at the top of the page asking the user if they want to add to their shelf.

![Add to shelf banner](/blog/progressive-web-apps/javascript-pwa-add-to-shelf-banner.png)

Figure 3. Add to shelf banner

Clicking the “Add button” prompts the user for the name they wish the app to appear as:

![Add to shelf prompt](/blog/progressive-web-apps/javascript-pwa-add-to-shelf-prompt.png)

Figure 4. Add to shelf prompt

Upon submission, Chrome will generate a real application (on Mac, it will be a “.app”, on Windows, an “exe”, etc..) which the user can double click to open the app directly in the Chrome. And, importantly, the app will still work when the user is offline.

The app will also appear in their “Shelf” which you can always access at `chrome://apps`, or by opening the “Chrome App Launcher” app (on OS X this is located in “~/Applications/Chrome Apps/Application Launcher”).

![Chrome App Launcher](/blog/progressive-web-apps/javascript-pwa-chrome-app-launcher.png)

Figure 5. Chrome App Launcher

__ |  The Chrome App Launcher lists apps installed both via the Chrome Web Store and via the “Add to Shelf” feature that we discuss here. The features we describe in this article are orthogonal to the Chrome Web Store and will not be affected by its closure.   
---|---
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **ZombieLover** — March 21, 2018 at 11:29 am ([permalink](https://www.codenameone.com/blog/progressive-web-apps.html#comment-23658))

> This is very exciting. Can we know how an app’s local SqLite DB will be impacted by this? When accessed by browser, where will the DB be stored? Will it be persisted for the user’s next visit? Will it be “downloaded” and stored locally at the app location when the app is installed?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fprogressive-web-apps.html)


### **Shai Almog** — March 22, 2018 at 6:26 am ([permalink](https://www.codenameone.com/blog/progressive-web-apps.html#comment-23812))

> We support SQL in the JavaScript port via web SQL: [https://en.wikipedia.org/wi…](<https://en.wikipedia.org/wiki/Web_SQL_Database>)
>
> So it’s stored in the browser. It works for most browsers but it’s a bit problematic because it isn’t a W3C standard. Also some things can’t be implemented there such as shipping the app with a “ready made” database etc.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fprogressive-web-apps.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
