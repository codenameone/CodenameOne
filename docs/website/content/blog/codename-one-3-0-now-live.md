---
title: Codename One 3.0 Now Live & Special Offer!
slug: codename-one-3-0-now-live
url: /blog/codename-one-3-0-now-live/
original_url: https://www.codenameone.com/blog/codename-one-3-0-now-live.html
aliases:
- /blog/codename-one-3-0-now-live.html
date: '2015-04-26'
author: Shai Almog
---

![Header Image](/blog/codename-one-3-0-now-live/CodenameOne-Horizontal.png)

We are thrilled to announce the immediate availability of Codename One 3.0!  
To celebrate this release we are giving away a **$100 rebate discount** for annual pro  
subscriptions and **$300 rebate discount** for annual enterprise subscriptions.   
All you need to do to get this rebate is [signup](http://www.codenameone.com/pricing.html) for an annual subscription and the appropriate sum will  
be refunded within 24 hours thru PayPal. This offer is valid before June 1st 2015.  
You can check out the press release and full announcement [here](/files/pr/CodenameOne3.0PressRelease.pdf). 

**Go a head and update your Plugin now to get the new Release.**

### Highlights Of The Release – Click For Details

____New iOS VM

When Codename One debuted we used XMLVM as the underlying iOS virtual  
machine abstraction. XMLVM is an excellent product but its unmaintained and its goals are too different from the  
goals of Codename One. The new VM includes some features that would be remarkably hard to achieve with XMLVM  
such as: proper stack traces, faster builds (2x overall!), smaller code size, concurrent GC, deep OS binding (String – NSString relationship) etc.  
Read more about this work on [the announcement blog](/blog/new-codename-one-ios-vm-is-now-the-default.html). 

____JavaScript build target (technology preview)

Allows compiling Codename One applications to JavaScript client side webapps  
without server side code. Notice that this support includes threading support.  
Notice that this feature will be restricted to enterprise developers once it enters beta.  
The Java VM work is based on [TeaVM](http://teavm.org/) an OSS Java-JavaScript VM. 

Read more about this work in [this blog post](/blog/javascript-port.html). 

____Charts API

The charts API supports drawing a wide range of charts such as bar, pie, line etc. It supports animating  
charts and is based on the aChartEngine Android API.  
Read more about this work on [Steve’s blog post](/blog/codename-one-charts.html). 

____New Demos

  * [Property Cross](/blog/propertycross-demo.html) – Browse properties for sale in the UK using a JSON webservice. Shows off JSON webservices, InfiniteScroll, URLImage etc.
  * [Dr Sbaitso](/blog/dr-sbaitso.html) – Demonstrates an AI bubble chat interface, includes text to speech using native interface and more.
  * [Photoshare](/blog/build-mobile-ios-apps-in-java-using-codename-one-on-youtube) – A simple social networking app that allows sharing photos
  * [Charts](/blog/codename-one-charts) – Demonstrates all chart types
  * [Geoviz](/blog/geo-visualization-library.html) – Performs statistic analysis over US population based on locale specific data
  * [Flickr](/blog/cats-in-toolbars.html) – Demo for the Toolbar class showing special title area effects

____New Themes

New beautiful and functional themes are now available through the plugins and  
the designer tool. 

____Toolbar API

More advanced and highly customizable API for handling  
the title area. It allows adding search to the title, animating its appearance/folding, placing commands  
arbitrarily and using the side menu.   
Read more about this work on [this blog post](/blog/cats-in-toolbars.html). 

____URLImage

Simplified image download to an icon or preview,  
that allows to implicitly apply special effects to said image (e.g. round corners, scaling etc.).   
Read more about this work on [this blog post](/blog/image-from-url-made-easy.html). 

____Built demos into the Eclipse/NetBeans Plugins

The main Codename One demos are now built-into the plugin so you can try them immediately without  
fixing classpaths and without downloading additional software. 

____New Android graphics pipeline

We rewrote the graphics pipeline on Android to work better in Android 4.x+ and use hardware acceleration  
where applicable. This new pipeline also includes support for the Shape & transform API’s.  
Read more about this work on [this blog post](/blog/new-android-pipeline-fixes.html). 

____Regular expression and validation support

We added a new regular expression package and a new validation framework that simplifies  
error highlighting for input. As part of that work we also presented a rudimentary masked  
input UI.  
Read more about this work on [this blog post](/blog/validation-regex-masking.html). 

____High DPI Image Support

There are 3 new DPI levels in Codename One all of which are now supported by the designer:  
DENSITY_560, DENSITY_2HD & DENSITY_4K. 

____Support for opening HTML files with hierarchies & Tar support

The builtin HTML support was improved by providing a way to open a hierarchy of files and not just  
a self contained HTML file. As part of this improvement we also added support to the tar file format.  
Read more about this work on [this blog post](/blog/html-hierarchy-release-plan-teavm.html). 

____New Morph & Flip Transitions

The [morph transition](/blog/mighty-morphing-components.html) was inspired by the Material design UI, converting a component on one form to  
a component on another form. The [flip transition](/blog/easy-demos-flip-more.html) provides an impressive 3d effect thats trivial to apply  
to any form or transition. 

____InteractionDialog

A new “modless” dialog that can “float” on top of the  
UI using the layered pane capability of the parent form  
Read more about this work on [this blog post](/blog/not-a-dialog-again.html). 

____Significantly enhanced developer guide

We redid the developer guide from the ground up converting  
it to asciidoc and integrating it into the website in a more fluent way. We increased its breadth by over  
50%.  
Check it our [here](/manual/) or download the [pdf](/files/developer-guide.pdf). 

____iOS Beta Test Support (testflight)

Apple introduced a new way to test mobile applications for up to 1000 beta testers based on the testflight  
framework (but not to be confused with the old testflightapp.com product). We now support distributing  
apps via this process for pro users.  
Read more about this work on [this blog post](/blog/location-ios-beta-testing-better-input.html). 

____MiG layout support

MiG layout is one of the popular cross platform Java layout managers that works across FX, Swing, AWT  
and now on Codename One as well…  
Read more about this work on [this blog post](/blog/location-ios-beta-testing-better-input.html). 

____Facebook improvements such as publish support & version 2.0 API

Facebook made a lot of changes to its API such as requiring a special  
[publish permission](/blog/facebook-publish-android-localization.html)  
and migrating graph calls to version 2.0. Both are now integrated into Codename One. 

____Added webservice wizard to simplify client-server calls

The  
[webservice wizard](/how-do-i---access-remote-webservices-perform-operations-on-the-server.html) allows us to  
generate RPC calls to the server including servlet and client stubs.   
Read more about this work on [this blog post](/blog/webservice-wizard.html). 

____Support for badging the native OS icon on iOS

We now support updating and setting a badge on an app icons in iOS devices.  
Read more about this work on [this blog post](/blog/badges.html). 

____TCP socket support

We finally added support for TCP sockets into Codename One.  
Read more about this work on [this blog post](/blog/sockets-multiline-trees.html). 

____Advanced keyboard input in iOS that doesn’t fold implicitly

This is an implementation of a feature that was [requested](https://code.google.com/p/codenameone/issues/detail?id=361)  
quite a while back. Historically, when moving from one text field to the next the VKB would fold and reopen. We now allow you to seamlessly  
move between input fields. 

You can also read the far more detailed list of release notes [here](/codenameone-3-0-release-notes.html). 

One of the things we are announcing today is a switch to faster release cycles, we already announced the  
next two release dates which will probably map to versions 3.1 and 3.2 respectively: July 27th and October 27 2015.  
Notice that the dates are 3 months apart allowing us to make releases much faster and keep Codename One  
stable in shorter iterations. This effectively means that  
[versioned builds](/how-do-i---get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature.html)  
will become a more viable feature. It also means that the feature list for every release will be very volatile  
and we won’t announce them until the release is out of the door. 

### Warning For Users Of The Old VM (XMLVM)

If you are still using the build flag ios.newVM=false we **strongly** suggest you stop right now!  
Even if you don’t need appstore submission and aren’t worried about the impending July cutoff date for the old  
VM you would still need to migrate! 

Apple broke xcode 5.x builds with its new Mac OS release Yosemite, this has been broken for a while and it  
seems that Apple has no intention of fixing this. Unfortunately Apple has a tendency of tying xcode upgrades and  
OS upgrades together, our build servers are still running Mavericks (the previous OS X release) and we have  
no intention of upgrading them right now. However, if we find ourselves in need of a new server or if Apple  
forces us to use a new version of xcode (and thus a newer version of Mac OS) we would be forced to upgrade  
the servers.  
Such an upgrade will make all builds targeting XMLVM fail! 

The correct thing to do is move to the new VM which we are actively supporting as much as possible, we suggest  
you do it now rather than hastily at a later date. It provides many advantages including stack traces &amp a  
superior gc.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **manezi** — April 30, 2015 at 12:22 pm ([permalink](https://www.codenameone.com/blog/codename-one-3-0-now-live.html#comment-22103))

> manezi says:
>
> Fantastic news. Well done to the CN1 team!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-0-now-live.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
