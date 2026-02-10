---
title: Partners, Demos, Custom GUI Component & iPhone-Old Deprecation
slug: partners-demos-custom-gui-components-iphone-old-deprecation
url: /blog/partners-demos-custom-gui-components-iphone-old-deprecation/
original_url: https://www.codenameone.com/blog/partners-demos-custom-gui-components-iphone-old-deprecation.html
aliases:
- /blog/partners-demos-custom-gui-components-iphone-old-deprecation.html
date: '2016-11-08'
author: Shai Almog
---

![Header Image](/blog/partners-demos-custom-gui-components-iphone-old-deprecation/new-gui-builder.jpg)

We are launching a partners page in the website that will refer to consultants that we recommend/approve.  
If you are a software development company that works with Codename One we’d like to feature you in our website  
and provide the following benefits:

  * Do-follow link from us (we have good page rank for valuable industry keywords)

  * Official reference – we will only feature companies that we recommend

  * Work from Codename One – when we outsource work we will only use official partners

  * Potential for guest posts, success stories, case studies, press releases etc.

For a featured position we currently have the following requirements:

  * At least one high quality app in the featured apps gallery available at least on iOS & Android

  * A do follow link to Codename One from your website

We reserve the right to deny anyone based on our considerations e.g. if a developer has bad reputation we can’t  
endorse such a developer.

To signup for this please use the chat contact button in the bottom right of the page and provide us with some  
details about your company, the app links, logo etc.

### Demos in the new Plugin

With the new plugin update we added a lot of the [new demos](/demos.html) to the plugin to make it easier for  
you to see them. This is true for both the NetBeans and IntelliJ plugins although not yet a part of the Eclipse plugin  
which is harder to maintain.

Every recent update of the new GUI builder is a bit of a mixed bag as we fix and improve we also introduce  
painful regressions. This time around we have a regression with `MultiButton` and probably all lead components  
(e.g. `SpanButton`) where events are no longer delivered. This probably means we’ll have to release a plugin  
update this Friday as well.

However, we did add a lot of other fixes and improvements including the ability to define a lead component properly  
and the long time RFE for the old GUI builder:

### Using Custom Components

The new GUI Builder allows us to custom components into the GUI. Just pick the custom component from the  
palette and drag it into place.

Once you do that you will need to create a method with the signature:
    
    
    private Component create_ComponentName() {
        return new MyComponent();
    }

__ |  the upcoming version of the plugin will generate that method implicitly   
---|---  
  
Here you can return any arbitrary component instance, bind listeners and do any initialization code you want. The  
GUI builder will only be used as a layout tool in the case of that specific component and you would have the  
full power to place anything there.

### 64bit Debug Builds

One annoyance of iOS 10 is that it now pops up a warning that the application is built for an older OS and might  
be slow. This is one of those “Appleisms” that tries to force developers to move to 64bit, we already support  
64bit but no longer build it in the debug version to avoid the performance cost.

If you build a release build and install it via testflight this should behave fine, however if you find this warning  
annoying you can just build a 64bit version instead of a 32bit version with the upcoming build hint:

`ios.debug.archs=arm64`

Notice that once you enable that flag the app will fail installation on older devices that don’t have 64bit support.  
That is why this is off by default. Normally we compile both 64 and 32 bit and package everything in a “fat binary”  
however for debug builds that effectively doubles the build time and increases the download size significantly.

### End of Life for iphone_old

If you rely on the iphone_old build target please migrate your code/certificates until January. We intend to migrate  
the last remaining servers to xcode 7.x by the time we release 3.6.

By now xcode 8 and a new version of Mac OS are already out so we need to keep up and the old build target  
is no longer practical. If you are still experiencing issues and are waiting for the fix now is the time to start working  
on getting these issues fixed!

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
