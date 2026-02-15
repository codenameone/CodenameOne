---
title: HTML Maps, Z-Order Peer & Properties Update
slug: html-maps-z-order-peer-properties-update
url: /blog/html-maps-z-order-peer-properties-update/
original_url: https://www.codenameone.com/blog/html-maps-z-order-peer-properties-update.html
aliases:
- /blog/html-maps-z-order-peer-properties-update.html
date: '2017-01-18'
author: Shai Almog
---

![Header Image](/blog/html-maps-z-order-peer-properties-update/maps.jpg)

One of the problems with native maps is that they work very differently between the device and the simulator. This is because we use `MapComponent` on the simulator and as a fallback on the devices where Google Maps isn’t available. We just committed a new mode for maps that allows you to use the Google HTML maps as the fallback instead of the `MapComponent`.

This is faster, has better support from Google and is more similar to the way maps work on the physical device because the browser component is also a peer component so similar restrictions will apply. This is off by default since the HTML maps require a key and right now we didn’t finish mapping all the components. This also needs some server functionality so I’m not sure when this will land in the actual extension but it’s already there is you build from source. We’ll post more about this when we do an official refresh of the extension.

### Z-Ordering in Peer Components

We did a lot of work getting z-ordering in Android a while back and then dropped the ball on it by postponing the rest of the work to 3.7 at the last minute. For those of you who don’t remember peer components are native OS widgets like video, browser, native maps etc.

This change allows us to draw on top of them and place components on top of them so we can create pretty rich applications from subtitling a video to augmented reality apps would be (relatively) easy with such a change. Without this change you would need native code to do this…​  
It will also make apps that rely heavily on Native Maps far easier to write with far better results.

Z-ordering is a big change and just didn’t fit into the schedule, however we didn’t abandon this effort and Steve just committed a major overhaul of our peer handling in the simulator/desktop port which will allow z-ordering on those platforms.

iOS will probably be more challenging but we hope to do it sooner rather than later so we will have plenty of time to test before 3.7 lands.

### Properties Update

We’re working on some big changes for the `Properties` API. We added a new `MapProperty` option which is effectively a property that functions as a `java.util.Map` equivalent similar to the existing `ListProperty`. We also added properties to represent primitive or numeric types e.g. instead of writing:
    
    
    public final Property<Integer,MyObject> myNumber = new Property<>("myNumber");

We would now write:
    
    
    public final IntProperty<MyObject> myNumber = new IntProperty<>("myNumber");

The main motivation for this is erasure. Parsing code that accesses `myNumber` wouldn’t know the generic type since that is removed during compilation. However, we can know the type of `IntProperty` & thus implicitly convert numeric types during parsing. This isn’t as “generic” but since `NumericProperty` is a common base class for all number properties and derives from `Property` we can write common code relatively easily.

The `NumericProperty` also introduces support for non-nullable elements in this case which will fail if you try to set a null value and thus work better with auto-boxed values.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Blessing Mahlalela** — February 16, 2017 at 11:46 am ([permalink](/blog/html-maps-z-order-peer-properties-update/#comment-23303))

> Blessing Mahlalela says:
>
> Hi, currently trying latest Github CN1 Google maps code. I added Java script api key, however on simulator it shows Open street maps. Is it possible to display html5 JS maps on simulator and every other device that does not have the SDK?


### **Blessing Mahlalela** — February 16, 2017 at 12:12 pm ([permalink](/blog/html-maps-z-order-peer-properties-update/#comment-23151))

> Blessing Mahlalela says:
>
> Ok, issue fixed I was using an old CN1Google lib. It will be good to remove it as it causes un necessary confusion.
>
> [https://github.com/codename…](<https://github.com/codenameone/codenameone-google-maps/blob/master/GoogleMaps.cn1lib>)
>
> Secondly Android build causes an error (have not tried iOS):
>
> Exception is:  
> org.gradle.api.tasks.TaskExecutionException: Execution failed for task ‘:transformClassesAndResourcesWithProguardForRelease’.


### **Shai Almog** — February 16, 2017 at 2:34 pm ([permalink](/blog/html-maps-z-order-peer-properties-update/#comment-23266))

> Shai Almog says:
>
> This isn’t implemented completely but should work for desktop and does work for my test case. The Android build isn’t something I tested with the current version but I’ll need the full logs as this isn’t the actual error message.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
