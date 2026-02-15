---
title: Mapping Natively
slug: mapping-natively
url: /blog/mapping-natively/
original_url: https://www.codenameone.com/blog/mapping-natively.html
aliases:
- /blog/mapping-natively.html
date: '2014-03-16'
author: Shai Almog
---

![Header Image](/blog/mapping-natively/mapping-natively-1.jpg)

  
  
  
  
![Picture](/blog/mapping-natively/mapping-natively-1.jpg)  
  
  
  

This has been a frequent RFE in the groups but it never made its way up because of the complexity involved. A corporate account recently requested support for native Maps so we had to promote the task upwards. We decided to build the  
[  
native maps as an external cn1lib  
](https://github.com/codenameone/codenameone-google-maps)  
rather than build them into Codename One itself, the reasoning is two fold:  
  
1\. Show how to map a native component – next time someone wants to add a native widget into Codename One they can use the Maps as a reference and go around us. 

2\. Since Maps are VERY complex we wanted to give developers the ability to customize the native code easily.

Going forward we might introduce more and more extensions as cn1libs rather than as builtin functionality. There is one drawback we didn’t predict though, Googles Map API for iOS is 50mb in size which makes sending the builds pretty slow. This is solveable by changing the compression option in the project properties to compress the jar, it is still pretty large though.

To browse/checkout the code/demo-test for maps just go to the project page  
[  
here  
](https://github.com/codenameone/codenameone-google-maps)  
. For simplicities sake you can just  
[  
download the binary here  
](https://github.com/codenameone/codenameone-google-maps/raw/master/GoogleMaps.cn1lib)  
and place it in your lib directory for the project then right click the project and select refresh client libs.

However, configuration does require a bit of work and there are a few limitations.

**  
Limitations  
**  
  
1\. The native maps are only supported on Android devices that have the Google Play store (e.g. not on Amazon Kindle) and on iOS devices. All other devices will show the MapComponent by default.  
  
Map component will be used on the simulator as well.

2\. Since a native component is used placing overlays is problematic. You will need to use Dialogs and the API’s of the MapContainer class to implement this.

**  
Configuration  
**  
  
The configuration portion is the hardest part, Google made it especially painful in the Google typical way. You can follow the instructions from Google to get started for  
[  
Android  
](https://developers.google.com/maps/documentation/android/start)  
and for  
[  
iOS  
](https://developers.google.com/maps/documentation/ios/start/)  
.  
  
You will need to follow their instructions to generate your map keys. Then define the following build arguments within your project:  

* * *

Make sure to replace the values YOUR_ANDROID_API_KEY & YOUR_IOS_API_KEY with the values you obtained from the Google Cloud console by following the instructions for  
[  
Android  
](https://developers.google.com/maps/documentation/android/start)  
and for  
[  
iOS  
](https://developers.google.com/maps/documentation/ios/start/)  
. 

Now that all of that is done you should be able to create a Map, add markers and paths:  

At the time of this writing there are still issues with Maps some of which are pretty hard to resolve. Currently when poping a dialog over the Map it turns white on Android and we aren’t really sure why. There are some artifacts on iOS as well but those might not be fixable.  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — March 17, 2014 at 3:22 pm ([permalink](/blog/mapping-natively/#comment-22095))

> Anonymous says:
>
> I really like this approach of introducing new features as modules. This enables us to drill down to the core of what CN1 is. In the future it might even be possible to incorporate a sort of package manager into the designer to automatically load the modules that you need for your project. Sort of like developing Netbeans platform plugins. I know we’re a ways away from that, but this small step of introducing maps as a separate module is perhaps a precedent-setting step in the right direction.
>



### **Anonymous** — March 17, 2014 at 6:51 pm ([permalink](/blog/mapping-natively/#comment-21791))

> Anonymous says:
>
> I’ve only used the MapComponent very simply, so am not really across any issues with it, so I’m wondering what the extra complexity of native maps buys me ?
>



### **Anonymous** — March 17, 2014 at 8:50 pm ([permalink](/blog/mapping-natively/#comment-21723))

> Anonymous says:
>
> Excellent, thanks for this. I’ve just tried it with my own API keys (that is a debacle – thanks Google) but once set up it works great. Hopefully the graphical glitches can be addressed sometime but its looking and performing great.
>



### **Anonymous** — March 18, 2014 at 2:59 am ([permalink](/blog/mapping-natively/#comment-21710))

> Anonymous says:
>
> Actually most of the infrastructure for adding dynamic classes to the designer is already there (classloader etc.) we had something like that in the LWUIT days so its doable. 
>
> The main reason we didn’t add this back is that we want to migrate to a new designer architecture eventually. This is still in drawing board stages but we have a pretty good idea of where we want to go here.
>



### **Anonymous** — March 18, 2014 at 3:00 am ([permalink](/blog/mapping-natively/#comment-21724))

> Anonymous says:
>
> Its mostly a matter of feel/performance. The native map is very fluid and fast, Google wrote it on OpenGL using vector graphics and its really smooth.
>



### **Anonymous** — March 20, 2014 at 7:19 am ([permalink](/blog/mapping-natively/#comment-22076))

> Anonymous says:
>
> Hi, I see the Android dialog blank screen. Is it possible to capture the screen before dialog is displayed and then use it as a background image behind the dialog? 
>
> Reason being I think users will complain about the blank screen, because they are used to seeing the map in the background.
>



### **Anonymous** — March 20, 2014 at 4:36 pm ([permalink](/blog/mapping-natively/#comment-21960))

> Anonymous says:
>
> We do that automatically for all peer components. Map is special (check the code to understand why) we had an issue in the AndroidPeer which should be fixed in current builds.
>



### **Anonymous** — March 21, 2014 at 1:36 pm ([permalink](/blog/mapping-natively/#comment-21883))

> Anonymous says:
>
> Hi I have just updated my SVN CN1 source project and refreshed libs. The Google map still becomes blank when a dialog is displayed.
>



### **Anonymous** — March 21, 2014 at 1:37 pm ([permalink](/blog/mapping-natively/#comment-22057))

> Anonymous says:
>
> Hi I have just updated my CN1 sources project and refreshed libs. The Google map still becomes blank when a dialog is displayed.
>



### **Anonymous** — March 21, 2014 at 2:24 pm ([permalink](/blog/mapping-natively/#comment-21875))

> Anonymous says:
>
> I’ll try this.
>



### **Anonymous** — March 23, 2014 at 7:10 am ([permalink](/blog/mapping-natively/#comment-21932))

> Anonymous says:
>
> For some reason the fix didn’t propagate to the build servers. We are updating them again now. Should be there in a half hour or so.
>



### **Anonymous** — March 23, 2014 at 8:15 am ([permalink](/blog/mapping-natively/#comment-21440))

> Anonymous says:
>
> Thanks map now appears in the background of dialog.
>



### **Anonymous** — September 5, 2014 at 3:22 am ([permalink](/blog/mapping-natively/#comment-21935))

> Anonymous says:
>
> hi, 
>
> If I am developing only for iOS do I still need the android key? 
>
> Thanks 
>
> Greg
>



### **Anonymous** — September 5, 2014 at 12:42 pm ([permalink](/blog/mapping-natively/#comment-22132))

> Anonymous says:
>
> Hi, 
>
> no. Should work fine without.
>



### **Anonymous** — November 10, 2014 at 3:41 am ([permalink](/blog/mapping-natively/#comment-22292))

> Anonymous says:
>
> I tried to put GoogleMaps.cn1lib into the lib folder in my Intellij IDEA project workspace and got java.lang.OutOfMemoryError:Java heap space at build.xml, line 449. 
>
> Are there other steps required, beside just dropping library into libs folder especially for IDEA environment?
>



### **Anonymous** — November 26, 2014 at 8:13 am ([permalink](/blog/mapping-natively/#comment-22123))

> Anonymous says:
>
> Sorry for the delay answering (got buried in my inbox). 
>
> Its an issue with IDEA that’s easily fixable, check out this: [http://devnet.jetbrains.com…](<http://devnet.jetbrains.com/message/5301721>)
>



### **Bobo Collen** — April 8, 2015 at 2:49 pm ([permalink](/blog/mapping-natively/#comment-22063))

> Bobo Collen says:
>
> How do i define iOS & Android build arguments within the project? i want to include GoogleMaps in this GoogleMapTest Demo using my GoogleMaps API, so that i can be more familiar with the process before i develop my Maps project.
>



### **Shai Almog** — April 9, 2015 at 5:29 am ([permalink](/blog/mapping-natively/#comment-22109))

> Shai Almog says:
>
> Right click the project, select preferences. In the Codename One section you should see a “Build Hints” tab and there you can add keys/values.
>



### **Bobo Collen** — April 13, 2015 at 3:27 pm ([permalink](/blog/mapping-natively/#comment-21612))

> Bobo Collen says:
>
> Thank you for your prompt response, I included the build arguments and the project is showing an Openstreet Maps instead of Google Maps. Are these Google maps APIs for Android and iOS going to cater for those of Windows Phone and Black Berry? If not, which maps APIs must we use since Black Berry and Windows Phone are not covered by Google maps?
>



### **Shai Almog** — April 14, 2015 at 5:20 am ([permalink](/blog/mapping-natively/#comment-24167))

> Shai Almog says:
>
> That is the map component fallback, you can define it by replacing the map implementation. Keep in mind that on the device this will look completely different anyway since it will run native code. I suggest you start with device testing and make sure that works for you since there are quite a few hurdles there.
>



### **Marco Grabmüller** — September 13, 2016 at 1:50 pm ([permalink](/blog/mapping-natively/#comment-22964))

> Marco Grabmüller says:
>
> Hello Shai,  
> i think, we have the issue with the white screen instead of the map. we use the native map and we have defined the build arguments corectly.  
> previously we used map component. Unfortunately this was not performing well enough but worked.
>
> in the simulator, we see the openstreetmap and a second java window with a loaded google map.  
> directly on the phone, we see only a white space.
>
> maybe you have an idea what we can do?
>
> Thx
>
> This is our Code:
>
> Before Part:  
> MapContainer cnt = new MapContainer();  
> imageViewerContainer.addComponent(BorderLayout.CENTER, cnt);
>
> Post Part:  
> Coord coord = new Coord(geoMapDataModel.getLatitude(), geoMapDataModel.getLongitude());  
> cnt.setCameraPosition(coord);  
> cnt.addMarker(null, coord, “test”, “”, null);
>



### **Shai Almog** — September 14, 2016 at 4:33 am ([permalink](/blog/mapping-natively/#comment-23061))

> Shai Almog says:
>
> Hi,  
> make sure you are up to date on the latest map from the extensions menu (under Codename One Settings). This usually means the SHA1 or something is incorrect so make sure you are using a proper release build etc.  
> If this still fails you need to connect the device with a cable and look thru the DDMS Android tool at the console. Google prints errors to the console with more details and you should be able to see the misconfiguration there.
>



### **Marco Grabmüller** — September 14, 2016 at 8:06 am ([permalink](/blog/mapping-natively/#comment-23081))

> Marco Grabmüller says:
>
> hello,  
> thx for your hints.  
> first we had a wrong api-key. the same key we have used @ the mapcomponent.  
> we think, maybe was this the fault. 1 key = 1 Project…
>
> second we had a bug in our layout.  
> parent container was borderlayout -> children Map container also borderlayout in center -> therefore we saw only a white screen.
>
> now the map works great! thx for support!
>



### **essay writer** — September 19, 2016 at 7:26 pm ([permalink](/blog/mapping-natively/#comment-21459))

> essay writer says:
>
> The mapping natively makes it easier for the performance. It might be difficult in the start but the complete working with research and limitation leads this as perfect one. It shoddily not has complexity in its findings.
>



### **Yngve Moe** — September 23, 2016 at 11:09 pm ([permalink](/blog/mapping-natively/#comment-23083))

> Yngve Moe says:
>
> Warning: be careful not to add any leading or trailing spaces in the iOS build hints (I got them added automatically when copying from this page in Chrome). The build server chokes on the extra spaces.
>



### **youssef abdeen** — December 1, 2016 at 5:25 pm ([permalink](/blog/mapping-natively/#comment-21449))

> youssef abdeen says:
>
> i am new at this, i dont know where to put this code in my app, any help please !!
>



### **Shai Almog** — December 2, 2016 at 5:31 am ([permalink](/blog/mapping-natively/#comment-22754))

> Shai Almog says:
>
> It’s the main application code, you can just include the cn1lib from the extensions menu and use MapContainer in code like other components. Notice you need to define some build hints based on the instructions in the extensions menu under Codename One Settings.
>



### **youssef abdeen** — December 2, 2016 at 11:54 pm ([permalink](/blog/mapping-natively/#comment-23248))

> youssef abdeen says:
>
> i did what you said and the application worked, but i did not use the google map API key, i got the API key but i don’t know where to put it. and sorry i didn’t understand the part you said about the “build hints”.  
> thank you
>



### **youssef abdeen** — December 3, 2016 at 12:30 am ([permalink](/blog/mapping-natively/#comment-23103))

> youssef abdeen says:
>
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/6cc4ec8421f4ac9f6cc219dba9cf877069c721be0d7a9c17f7de71e32d311797.jpg>)  
> where exactly to put the argument thing and the keys here??
>



### **Shai Almog** — December 3, 2016 at 9:15 am ([permalink](/blog/mapping-natively/#comment-23117))

> Shai Almog says:
>
> In the right click menu go to the Codename One Settings app where a build hints option is available. Native maps work on the simulator but are very different on the device…
>



### **Marco Grabmüller** — December 5, 2016 at 7:31 am ([permalink](/blog/mapping-natively/#comment-23126))

> Marco Grabmüller says:
>
> Hello Shai,  
> I need your help again 🙂
>
> We get the following build errors after sending a debug ios build -> fatal error: “GoogleMaps.h” file not found  
> #import “GoogleMaps.h”
>
> Our native Android apps works flawlessly.
>
> We have done all the points as described above.  
> We downloaded the native map extension using the codenameone Settings Tool.
>
> Any ideas, what is wrong?
>
> Thank you very much
>



### **Shai Almog** — December 6, 2016 at 5:27 am ([permalink](/blog/mapping-natively/#comment-23255))

> Shai Almog says:
>
> Hi,  
> the post above is outdated and you should refer to the github project page for instructions. Ideally we’ll post an updated blog on doing this.  
> The new extension which is installable via the extensions menu in the Codename One Settings auto-setups most of these hints so if you added ios.* hints as instructed above you shouldn’t have and should remove them.
>



### **Marco Grabmüller** — December 6, 2016 at 8:19 am ([permalink](/blog/mapping-natively/#comment-23264))

> Marco Grabmüller says:
>
> Hello and thank you for your answer.
>
> We have looked at the Git guide -> [https://github.com/codename…](<https://github.com/codenameone/codenameone-google-maps>)
>
> For our understanding:  
> This build hint -> ios.glAppDelegateHeader = #import “GoogleMaps / GoogleMaps.h” is automatically set by the extension.  
> We can not delete it, because it is always re-created.  
> The build hint is also not in your example?  
> Our build hints are included as screenshot.
>
> Strange is:  
> We are now no longer the error reported by us above.  
> There is still a build error, but there is no error in the log.
>
> Is there a way to send the log file to you? Or. Can you see it?
>
> Thanks again! [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/1e361fa5b098d7bcdd0c8359676d4fdb5f286c271ef3ba99a3718de4f67c8aea.png>)
>



### **Shai Almog** — December 7, 2016 at 6:39 am ([permalink](/blog/mapping-natively/#comment-23086))

> Shai Almog says:
>
> In the past GoogleMaps.h was included but the new syntax is GoogleMaps/GoogleMaps.h due to the cocoa pods change which is why I said this is a setup problem.
>
> For build errors we have a support engineer in the chat application on the bottom right side who can help with those. Just let them know you have an error and paste the link our server returned. They will review the error and try to guide you.
>
> FYI 9 out of 10 iOS build errors happen when you generate your own certificates incorrectly instead of using the certificate wizard.
>



### **youssef abdeen** — December 8, 2016 at 8:39 pm ([permalink](/blog/mapping-natively/#comment-22956))

> youssef abdeen says:
>
> i am developing an app using codename one to notify a driver if he drives in a the wrong direction of a street using google maps i already downloaded the google map extension and it opened in my app i just want any ideas how to know the direction of a certain street on google map and use it in my code, so if a driver went on the wrong direction of this street i would notify him by a dialog message.  
> thank you
>



### **Shai Almog** — December 9, 2016 at 7:37 am ([permalink](/blog/mapping-natively/#comment-23015))

> Shai Almog says:
>
> That’s probably a part of the google GEO API. I’m afraid I’m unfamiliar with that and can’t give you any pointers. I think you will need to modify the implementation of the google maps cn1lib to include this functionality so it’s non-trivial and requires some native code.
>



### **Stephen Michael** — January 18, 2017 at 6:13 pm ([permalink](/blog/mapping-natively/#comment-23069))

> Stephen Michael says:
>
> Is there any work in progress to update the native mapping library to utilize V3 of the Google Maps API? I am looking to integrate the “unlimited/free” access via Android and IOS native maps API’s, and am wondering about the accessibility of V3 Google Maps API’s via CodenameOne. Thoughts?
>



### **Shai Almog** — January 19, 2017 at 6:18 am ([permalink](/blog/mapping-natively/#comment-23088))

> Shai Almog says:
>
> V2 is the latest. V3 is for JavaScript only and we use the native API’s.  
> We are actually updating the Maps right now to have an optional JavaScript fallback instead of the MapComponent fallback. I think the UX is better overall. There are already some committed changes on that but this isn’t released yet since it’s work in progress.
>



### **Terry Wilkinson** — February 2, 2017 at 8:21 pm ([permalink](/blog/mapping-natively/#comment-23302))

> Terry Wilkinson says:
>
> Hi Shai,
>
> I’m just getting started with this, and am having trouble getting this Native Maps demo running using eclipse. I’ve copied the GoogleMaps.cn1lib file to the lib directory in my project, but the statement
>
> import com.codename1.googlemaps.MapContainer;
>
> is giving the error
>
> “The import com.codename1.googlemaps cannot be resolved [GoogleMapsTestApp.java/Goog…](<http://GoogleMapsTestApp.java/GoogleMapsTestApp/src/com/codename1/test/googlemaps>) line 3″
>
> Is this because GoogleMaps.cn1lib is not a jar file? What am I doing wrong?
>
> Thank,  
> Terry
>



### **Shai Almog** — February 3, 2017 at 7:42 am ([permalink](/blog/mapping-natively/#comment-23076))

> Shai Almog says:
>
> Hi,  
> I suggest using the extension manager tool in Codename One Settings to install cn1libs. I hope to post a refreshed version of this guide within a couple of weeks once we get out the new version of the library.
>



### **Terry Wilkinson** — February 3, 2017 at 5:01 pm ([permalink](/blog/mapping-natively/#comment-24245))

> Terry Wilkinson says:
>
> Thanks, Shai, I look forward to your updated guide.
>
> In the mean time, your suggestion helped me, and I have succeeded in building and installing the demo app. However, when I run it, it comes up using OpenStreetMaps. I must have missed something else 🙂
>



### **Shai Almog** — February 4, 2017 at 8:18 am ([permalink](/blog/mapping-natively/#comment-23209))

> Shai Almog says:
>
> You need to make sure all the keys from google are setup correctly in the build hints.
>



### **Terry Wilkinson** — February 4, 2017 at 1:42 pm ([permalink](/blog/mapping-natively/#comment-22822))

> Terry Wilkinson says:
>
> That’s what it took – thanks very much. I thought I’d done that, but is must have gotten deleted somehow – grrrr.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
