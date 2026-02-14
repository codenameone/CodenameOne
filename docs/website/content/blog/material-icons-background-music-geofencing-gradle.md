---
title: Material Icons, Background Music, Geofencing & Gradle
slug: material-icons-background-music-geofencing-gradle
url: /blog/material-icons-background-music-geofencing-gradle/
original_url: https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html
aliases:
- /blog/material-icons-background-music-geofencing-gradle.html
date: '2015-11-22'
author: Shai Almog
---

![Header Image](/blog/material-icons-background-music-geofencing-gradle/material-icon-fonts.png)

We became infatuated with [icon fonts](http://codenameone.com/blog/icon-fonts-oldvm-swan-song.html)  
a while back and used them quite a bit, recently we added the  
[FontImage](http://codenameone.com/blog/icon-fonts-oldvm-swan-song.html) class that made  
them really easy to use.  
However, up until now you had to download a font. Look up the value and enter it in. This was OK but not  
ideal in terms of syntax/availability especially for simpler apps. 

Google’s material design includes a really cool mobile friendly icon font and using that for all our builtin features  
seems like a no-brainer. This reduces the total download size while making the fidelity of the UI much higher.  
As a result of this features like pull-to-refresh, infinite progress, share button etc. will implicitly look different and use  
the icon font.   
You can also use one of the huge list of Google’s material design fonts with just one line of code! 

To do that just go to the [Material Design Icon Catalog](https://www.google.com/design/icons/)  
and type the icon name in the search field (or just scroll thru the images). E.g. say I want a thumb up button  
I can just apply it to a button like this: 
    
    
    FontImage.setMaterialIcon(getStarted, FontImage.MATERIAL_THUMB_UP);

The cool part about that code is that you don’t have to define a special font, color or anything!  
The API will implicitly extract the button styles and use that to set the right icon into place. 

If you just want the image to use in any way you see fit you can just use: 
    
    
    FontImage img = FontImage.createMaterial(FontImage.MATERIAL_THUMB_UP, style);

Where the style object indicates the size and color of the image. Notice you can also use `getMaterialDesignFont`  
to get the actual font object. 

#### Background Music

As part of our continuing effort to improve the portability of background processes within Codename One we  
recently added support for background music playback. This support isn’t totally portable since the Android  
and iOS approaches for background music playback differ a great deal.  
To get this to work on Android we added the new API: `MediaManager.createBackgroundMedia`.  
You should use that API when you want to create a media stream that will work even when your  
app is minimized and this should work for Android. 

For iOS you will also need to add a special build hint: `ios.background_modes=music`. Which  
should allow background playback of music. 

#### Background Location Updates – Geofencing

This was actually committed last month but documenting this has been a bit more challenging so it got pushed  
back a bit. Background location is even more complex than background media, polling location is generally  
expensive and requires a special permission on iOS. Its also implemented rather differently both in iOS  
and Android.  
Because of the nature of background location the API is non-trivial. It starts with the venerable `LocationManager`  
but instead of using the standard API you need to use `setBackgroundLocationListener`, but  
this is where it flips. Instead of passing a location listener instance you need to pass a `Class`  
object instance. This is important because background location might be invoked when the app isn’t  
running and an object would need to be allocated. 

Notice that you should NOT perform long operations in the background listener callback. IOS wake-up time is  
limited to approximately 10 seconds and the app could get killed if it exceeds that time slice.  
Notice that the listener can sends events also when the app is in the foreground, therefore it is recommended  
to check the app state before deciding how to process this event. You can use `Display.isMinimized()`  
to determine if the app is currently running or in the background. 

When implementing this make sure that the class passed is a public class in the global scope (not inner class  
or anything like that). Make sure that the class has a public no-argument constructor and make sure you  
pass it as a class literal e.g. `MyClassName.class`. Don’t use  
`Class.forName("my.package.MyClassName")`!  
Class names are problematic since device builds are obfuscated, you should only use literals which the  
obfuscator detects and handles correctly. 

#### Android Native Gradle Support

Google made a lot of changes to their native tooling since we launched Codename One. Up until now we  
are still using Ant on our servers to build the native Android apps. This has been rather convenient but as  
Google is migrating away from Ant to Gradle its becoming an issue where some features can’t be supported  
by the build system. 

So we introduced the new build hint: `android.gradle`. This hint can be either true or false and  
will default to true once 3.3 launches so we suggest kicking the wheels right now by setting it to true and  
reporting issues!  
One of the really great features you get by building with Gradle support is that you will now be able to just  
open the Android project in Android studio when using  
[include source](/how-do-i---use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc.html)  
and just run them as usual!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — November 23, 2015 at 9:51 am ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22428))

> Diamond says:
>
> My Christmas came early this year as this is the biggest gift I ever received. 2 of my apps have reached a deadlock as they both require background location polling when app is not running.
>
> Thank you for the implementation. I can’t wait for the next plugin update.
>
> Could you please share a sample usage snippet of background location polling to help kickstart the implementation with less errors.
>



### **Chen Fishbein** — November 23, 2015 at 10:25 am ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22171))

> Chen Fishbein says:
>
> Hi,  
> This really depends on your use case, but in most cases when you are in the background you usually want GeoFencing.  
> With GeoFencing you can ask for an event from the device when the user gets near a certain location or walks away from a certain location.  
> See the new addGeoFencing method in the LocationManager, when you get the callback check if the app isMinimized and if true you usually want to alert the user that he is now near a certain place, the only way to communicate with the user when you are in the background is by sending a local notification to the user, see:  
> [http://www.codenameone.com/…](<http://www.codenameone.com/blog/local-notifications.html>)
>



### **bryan** — November 23, 2015 at 9:36 pm ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22521))

> bryan says:
>
> Can I use the FontImage images to automagically work in different resolutions in place of multi-images ?
>



### **Shai Almog** — November 24, 2015 at 4:19 am ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22244))

> Shai Almog says:
>
> Yes. That’s the general idea.  
> Scaling a font image is effectively just drawing the text larger. It uses the native device kerning so anti-aliasing will be gorgeous.  
> A huge bonus is that the font color auto-adapts to the color palette.
>



### **Chidiebere Okwudire** — December 9, 2015 at 11:34 am ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22505))

> Chidiebere Okwudire says:
>
> Hi,
>
> With regard to background location updates, what is the frequency with which the listener will be invoked? Is it configurable?
>



### **Shai Almog** — December 9, 2015 at 12:04 pm ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22359))

> Shai Almog says:
>
> Hi,  
> the LocationRequest class receives an interval which is the time in milliseconds for polling as far as I understand.
>



### **Chidiebere Okwudire** — December 9, 2015 at 8:45 pm ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22452))

> Chidiebere Okwudire says:
>
> Hmmm… I still don’t get it: A LocationRequest object is not a parameter of the setBackgroundLocationListener() method…
>



### **Shai Almog** — December 10, 2015 at 3:38 am ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22369))

> Shai Almog says:
>
> I think wires got crossed in my brain as I never used the background location listener.  
> I think its always coarse and polls in a system defined way to preserve battery life but Chen or Steve can probably give a better answer here.
>



### **Chen Fishbein** — December 10, 2015 at 8:10 am ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22280))

> Chen Fishbein says:
>
> The LocationRequest is useful for foreground location listening to preserve battery state.  
> Background location is different and should be used when the app is not running, the app gets updates only when significant location change happens
>



### **Chidiebere Okwudire** — December 10, 2015 at 11:56 am ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22423))

> Chidiebere Okwudire says:
>
> Hi Chen, thanks for the clarification though my question remains unanswered. What is a ‘significant location change’ in this context?
>



### **Chidiebere Okwudire** — January 20, 2016 at 12:04 pm ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22672))

> Chidiebere Okwudire says:
>
> Hi,
>
> With regards to material icons, as far as I understand, these are Google’s icons (so most suited for Android), right? When creating an app with native look-and-feel, I’d really love to have iOS icon style on iOS, WP icons on WP, etc. This will make for a richer native experience instead of having the same icons on all platforms. What’s the recommended way to realize this in CN1?
>
> [https://icons8.com/](<https://icons8.com/>) provides what am looking for but the problem is that I have to download several images possibly in different colors to meet my needs. Is there a better way (e.g., an icon font that provide platform-specific versions of icons)?
>



### **Shai Almog** — January 20, 2016 at 2:16 pm ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-22407))

> Shai Almog says:
>
> Hi,  
> true. We thought about this quite a bit before settling on the material icon set and not something with wider reach.
>
> The reason we added it was to enable us and you guys to prototype things that look good on all platforms without worrying about the “art” side of the fence. This helps smaller apps, internal apps and demos look reasonable and even more native as the icons adapt to the specific platform convention colors. Material icons are free, unencumbered and cheap in terms of size overhead so they were pretty easy to add out of the box.
>
> With a refined design as we see in most of our professional grade apps, you wouldn’t use any of our builtin icons and focus on your own so having icons for everything would be redundant.
>
> You can have platform specific icons like these guys offer thru the resource file override feature which was designed with this exact thing in mind: [https://www.codenameone.com…]([https://www.codenameone.com/how-do-i—create-different-uis-or-use-different-images-for-specific-platformsform-factors-using-the-override-option-in-the-gui-builder.html](https://www.codenameone.com/how-do-i---create-different-uis-or-use-different-images-for-specific-platformsform-factors-using-the-override-option-in-the-gui-builder.html))
>
> However, I think this is often overstated. I think 99% of users care don’t really care that much about these conventions. If your app looks good and respects the general platform paradigm (e.g. Menu button, back button behavior etc.) it should be good.
>



### **Chidiebere Okwudire** — January 25, 2016 at 9:16 am ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-24258))

> Chidiebere Okwudire says:
>
> Thanks for the explanation
>



### **ayush jagga** — June 23, 2020 at 7:28 am ([permalink](https://www.codenameone.com/blog/material-icons-background-music-geofencing-gradle.html#comment-24278))

> [ayush jagga](https://lh3.googleusercontent.com/a-/AOh14Gg4AfIvrf-y03PXgpEK7I199ci7_UNyHimjmlpE) says:
>
> Geofencing remains relatively new. While more businesses experiment with technology, creative marketers will come up with more innovative ways to maximize their potential for long-term benefits. If you are thinking of experimenting with [**geofencing for promotion**](https://www.bol7.com/geo-fencing/), confirm you are qualifying your prospects supported supply and demand, and make an irresistible offer for the simplest results. 
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
