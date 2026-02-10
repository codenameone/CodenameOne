---
title: Can Execute Hint
slug: can-execute-hint
url: /blog/can-execute-hint/
original_url: https://www.codenameone.com/blog/can-execute-hint.html
aliases:
- /blog/can-execute-hint.html
date: '2017-01-24'
author: Shai Almog
---

![Header Image](/blog/can-execute-hint/new-features-1.jpg)

`Display.canExecute(url)` provides us with a generic tool to test the availability of a feature before executing a command. This is very useful for inter-app communications and allows us to achieve various things such as launching Google Map instead of Apple Maps on iOS.

Lets say I want to navigate using Google Maps if it’s installed in iOS but if not I’ll settle for Apple Maps I can do something like this:
    
    
    String url = "comgooglemaps://?q=" + Util.encodeUrl(address);
    Boolean b = Display.getInstance().canExecute(url);
    if(b != null && b.booleanValue()) {
        Display.getInstance().execute(url);
    } else {
        // google maps is probably not installed
        Display.getInstance().openNativeNavigationApp(address);
    }

This should work and would have worked but might not have…​

The reason this might have failed in recent iOS & xcode versions is because Apple changed the behavior of the underlying API used by `canExecute` to always return `false`. The workaround is to add the prefix of the URL into a list within the plist file. We have a build hint for that which accepts a comma separated list:
    
    
    ios.applicationQueriesSchemes=comgooglemaps

If there are additional entries you can separate them with a comma. You can use up to 50 URL prefixes.

The reason for this requirement isn’t 100% clear but it’s assumed that some apps used this feature to query the installed apps on the device thus violating users privacy in a subtle way.

Thanks to the recent [build hint automation](/blog/automatic-build-hints-configuration.html) we can detect cases where you use the `canExecute` API and automatically setup the build hint correctly. Obviously, this won’t work for dynamic values that change on the device but should work for standard prefixes.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
