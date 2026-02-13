---
title: Intercepting URL's On iOS & Android
slug: intercepting-urls-on-ios-android
url: /blog/intercepting-urls-on-ios-android/
original_url: https://www.codenameone.com/blog/intercepting-urls-on-ios-android.html
aliases:
- /blog/intercepting-urls-on-ios-android.html
date: '2014-07-15'
author: Shai Almog
---

![Header Image](/blog/intercepting-urls-on-ios-android/intercepting-urls-on-ios-android-1.png)

  
  
  
  
![Picture](/blog/intercepting-urls-on-ios-android/intercepting-urls-on-ios-android-1.png)  
  
  
  

**  
Notice:  
**  
the original version of this post incorrectly specified the property as AppArgs instead of AppArg. This is now fixed. For Android you would probably also want to add the build argument android.xactivity=android:exported=”false”.  
  
  
  
  
  
  
A common trick in mobile application development, is communication between two unrelated applications. In Android we have intents which are pretty elaborate and can be used via Display.execute, however what if you would like to expose the functionality of your application to a different application running on the device. This would allow that application to launch your application.  
  
  
This isn’t something we builtin to Codename One, however we did expose enough of the platform capabilities to enable that functionality rather easily on Android.  
  
  
  
  
On Android we need to define an intent filter which we can do using the android.xintent_filter build argument, this accepts the XML to filter whether a request is relevant to our application:  
  
  
android.xintent_filter=<intent-filter> <action android_name=”android.intent.action.VIEW” /> <category android_name=”android.intent.category.DEFAULT” /> <category android_name=”android.intent.category.BROWSABLE” /> <data android_scheme=”myapp” /> </intent-filter>  
  
  
  
  
  
  
  
  
  
This is taken from this  
[  
stack overflow question  
](http://stackoverflow.com/questions/11421048/android-ios-custom-uri-protocol-handling)  
, to bind the myapp:// URL to your application. So if you will type myapp://x into the Android browser your application will launch.  
  
  
  
  
So how do you get the “launch arguments”, passed to your application?  
  
  
  
  
Display.getInstance().getProperty(“AppArg”) should contain the value of the URL that launched the app or would be null if it was launched via the icon.  
  
  
  
  
  
  
  
  
  
  
iOS is practically identical with some small caveats, iOS’s equivalent of the manifest is the plist. So we allow injecting more data into the plist thru the  
  
  
ios.plistInject build argument.  
  
  
  
So the equivalent in the iOS side would be ios.plistInject=<key>CFBundleURLTypes</key> <array> <dict> <key>CFBundleURLName</key> <string>com.yourcompany.myapp</string> </dict> <dict> <key>CFBundleURLSchemes</key> <array> <string>myapp</string> </array> </dict> </array>  
  

  
  
On a separate unrelated note the guys from Java Code Geeks announced the winners of  
[  
our raffle of two tickets  
](http://www.javacodegeeks.com/2014/06/codename-one-java-code-geeks-are-giving-away-free-javaone-tickets-worth-3300.html)  
, yesterday. Congratulations to both winners!  
  
  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — August 12, 2017 at 10:56 am ([permalink](https://www.codenameone.com/blog/intercepting-urls-on-ios-android.html#comment-23753))

> Francesco Galgani says:
>
> It seems an old post. I’m interested in intercepting URLs on iOS & Android because an use case like this:
>
> 1\. The server sends a verification code to a given phone number by sms  
> 2\. Because Codename One cannot receive sms, the verification code can be taken by the app when the user tape the url inside the sms.
>
> Of course I can have different use cases, all with the purpose to pass one or more arguments to my app from an external app (native sms app, other sms/messaging apps like Signal or Whatsapp, browsers, etc.).
>
> So, my questions are if this post is still update and if it’s correctly formatted (it’s not clear if the build arguments for Android and iOS are in only one row).
>
> I suppose that the build arguments are the ones described in “Sending Arguments To The Build Server”, is it right?  
> [https://www.codenameone.com…](<https://www.codenameone.com/manual/advanced-topics.html#_sending_arguments_to_the_build_server>)
>
> Thank you.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fintercepting-urls-on-ios-android.html)


### **Shai Almog** — August 13, 2017 at 5:32 am ([permalink](https://www.codenameone.com/blog/intercepting-urls-on-ios-android.html#comment-23424))

> Shai Almog says:
>
> This post is in the developer guide too (I think under the misc section). Everything is in one row since that’s how build hints work. Since it’s XML it doesn’t matter.
>
> SMS and URL interception are very different things. If you look at apps like banking apps, uber, whatsapp etc. in all of them SMS activation is done by typing in the value you get from the native SMS app. Some rare apps catch the incoming SMS on Android but since this requires some pretty scary sounding permissions a lot of apps just give up on that feature and work the same way as they do on iOS.
>
> This can be done easily in Codename One and is done by the JAT app.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fintercepting-urls-on-ios-android.html)


### **Francesco Galgani** — July 11, 2018 at 6:22 pm ([permalink](https://www.codenameone.com/blog/intercepting-urls-on-ios-android.html#comment-23649))

> Francesco Galgani says:
>
> At the begging of this post, you wrote: «For Android you would probably also want to add the build argument android.xactivity=android:exported=”false”». Indeed this build hint cannot be used, because it causes that Android cannot start the app, giving the error: “The app is not installed”. The reason is explained here: [https://stackoverflow.com/a…](<https://stackoverflow.com/a/49471457>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fintercepting-urls-on-ios-android.html)


### **Shai Almog** — July 12, 2018 at 5:39 am ([permalink](https://www.codenameone.com/blog/intercepting-urls-on-ios-android.html#comment-23986))

> Shai Almog says:
>
> I don’t recall this at all, it’s been 4 years since I wrote that so no idea…
>
> This isn’t mentioned in the developer guide section though: [https://www.codenameone.com…](<https://www.codenameone.com/manual/advanced-topics.html>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fintercepting-urls-on-ios-android.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
