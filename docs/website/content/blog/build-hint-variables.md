---
title: Build Hint Variables
slug: build-hint-variables
url: /blog/build-hint-variables/
original_url: https://www.codenameone.com/blog/build-hint-variables.html
aliases:
- /blog/build-hint-variables.html
date: '2016-08-03'
author: Shai Almog
---

![Header Image](/blog/build-hint-variables/build-hints.png)

This is a feature Steve added way back in June but I didn’t get around to documenting it. Build hints can sometimes get “unwieldy” e.g. in the case of `ios.plistInject` or `android.xapplication` we sometimes have pretty verbose values.

We now have a way to define a “build hint variable” which the build server substitutes seamlessly. This is useful for “key” values required by API’s that sometimes require boilerplate e.g. the [Google Maps support](https://github.com/codenameone/codenameone-google-maps/) has this:
    
    
    android.xapplication=<meta-data android_name="com.google.android.maps.v2.API_KEY" android_value="YOUR_ANDROID_API_KEY"/>

Notice that the one thing that actually matters here is `YOUR_ANDROID_API_KEY` which makes this problematic. We can’t add this string to the build hints automatically because this is a value you need to set…​

The variables allow us to do this instead:
    
    
    var.androidAPIKey=YOUR_ANDROID_API_KEY
    android.xapplication=<meta-data android_name="com.google.android.maps.v2.API_KEY" android_value="${var.androidAPIKey}"/>

This might seem more verbose but notice that the cn1lib can now inject the second line automatically and you will need to add this single line: `var.androidAPIKey=YOUR_ANDROID_API_KEY`

That is far simpler than before and less error prone.

Most cn1libs still don’t take advantage of this syntax but hopefully we’ll move them in that direction as we move forward.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Gareth Murfin** — August 5, 2016 at 12:35 pm ([permalink](https://www.codenameone.com/blog/build-hint-variables.html#comment-22830))

> Gareth Murfin says:
>
> Had not even considered this, very useful indeed!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbuild-hint-variables.html)


### **Torjmen Hamza** — April 30, 2017 at 12:28 pm ([permalink](https://www.codenameone.com/blog/build-hint-variables.html#comment-23491))

> Torjmen Hamza says:
>
> Hello,  
> I have problems with Google Maps in Codename One, I tried to make a useful path like DirectionRoute in javascript but it is always a black line, it’s like that i have airlines, could some one help me with that ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbuild-hint-variables.html)


### **Shai Almog** — May 1, 2017 at 3:52 am ([permalink](https://www.codenameone.com/blog/build-hint-variables.html#comment-23281))

> Shai Almog says:
>
> Hi,  
> why not ask that in the maps post [https://www.codenameone.com…](<https://www.codenameone.com/blog/new-improved-native-google-maps.html>)  
> I suggest asking there and mentioning what you did.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbuild-hint-variables.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
