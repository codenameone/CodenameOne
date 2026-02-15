---
title: Using The New Facebook API
slug: using-the-new-facebook-api
url: /blog/using-the-new-facebook-api/
original_url: https://www.codenameone.com/blog/using-the-new-facebook-api.html
aliases:
- /blog/using-the-new-facebook-api.html
date: '2013-11-30'
author: Shai Almog
---

![Header Image](/blog/using-the-new-facebook-api/using-the-new-facebook-api-1.png)

  
  
  
[  
![Picture](/blog/using-the-new-facebook-api/using-the-new-facebook-api-1.png)  
](/img/blog/old_posts/using-the-new-facebook-api-large-2.png)  
  
  

We recently introduced a new Facebook API to allow native integration since Facebook has stated their intention to no longer accept OAuth logins from applications. This is very much work in progress so some things will change as we move along especially related to elevated permissions for posting. 

  
If you just want something in the form of a “share button” we suggest you refer to the builtin share button which uses native sharing on both iOS and Android.  

  
The new Facebook API is very simple, in fact as of this writing it includes only 5 methods. Notice that the old Facebook API will still work as expected with the exception of the login aspect which will be handled by the new API.  
  
  
  
There are really 3 significant method  
  
s in the API, login/logout and isLoggedIn(). You can also bind a listener to login event callbacks which is really pretty simple. The difficulty isn’t here though. 

  
Before you get started you need to go to the page on Facebook for app creation:  
  
[  
https://developers.facebook.com/apps  
](https://developers.facebook.com/apps)  
  
  
Here you should create your app and make sure to enter the package name of the Codename One application both for the section marked as Bundle Id and Package Name  
  
(see the red highlighting in the attached image).

  
  
Once you do that you need to define the build argument facebook.appId to the app ID in the Facebook application (see the red marking at the top of the image).

  
Now when you send a build and invoke FacebookConnect.login() this should work as expected on iOS but it will fail on Android. The reason is that Facebook  
  
requires a hash from Android developers to identify your app. However, their instructions to generate said hash don’t work… The only way we could find for generating the hash properly is on an Android device.  
  
  
If you have DDMS you can connect the device to your machine and see the printouts including the hashcode (notice the hashcode will change whenever you send a debug build so make sure to only use Android release builds). You can also get the value of the hashcode from Display.getInstance().getProperty(”  
  
facebook_hash”, null);  
  
  
This will return the hash only  
  
on Android ofcourse.

  
You can take this hash and paste it into the section marked Key Hashes in the native android app section. Notice you can have multiple hashes if you have more than one certificates or applications.  
  
  
  
  
  
  
Once login is successful the existing facebook API’s from the Facebook package should work pretty much as you would expect.  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — December 2, 2013 at 7:55 am ([permalink](/blog/using-the-new-facebook-api/#comment-21641))

> Anonymous says:
>
> What about platforms which donot have native facebook integration like J2ME ? When is facebook planning to stop OAUTH login from applications ? This is really a shocking piece of news!!
>



### **Anonymous** — December 2, 2013 at 2:40 pm ([permalink](/blog/using-the-new-facebook-api/#comment-22036))

> Anonymous says:
>
> At the moment they still work but it was bound to happen with Facebook. If you are relying on Facebook as your only means of authentication I would look for alternative solutions.
>



### **Anonymous** — December 4, 2013 at 6:02 am ([permalink](/blog/using-the-new-facebook-api/#comment-22064))

> Anonymous says:
>
> Great and very important addition to CodeNameOne! 
>
> This improves the user experience a great deal – a very important issue if you have a socially boosted app. 
>
> A few things I broke my head about: 
>
> – The new API is in a different package: [com.codename1.social](<http://com.codename1.social>) 
>
> – Once you use the new API, you should use getToken() in order to get the token to use the old API for sharing and stuff. 
>
> – The hash key is changed by Facebook. If you get an error that the hash key doesn’t match, try this: 
>
> [http://stackoverflow.com/qu…](<http://stackoverflow.com/questions/19485004/android-app-key-hash-doesnt-match-any-stored-key-hashes>) 
>
> For me it worked – I changed the hash key from s_XroFyP24CL… to s/XroFyP24CL… and log in was successful. 
>
> Good luck!
>



### **Anonymous** — December 5, 2013 at 11:13 am ([permalink](/blog/using-the-new-facebook-api/#comment-21911))

> Anonymous says:
>
> Thanks, the token should automatically be set to FaceBookAccess so after login it should seamlessly be there and you should be able to use the class.
>



### **Anonymous** — January 20, 2014 at 5:37 am ([permalink](/blog/using-the-new-facebook-api/#comment-21905))

> Anonymous says:
>
> Hello, 
>
> I’m getting an error on importing import com.codename1.social.FacebookConnect (the whole social package is missing) . 
>
> Am I missing the latest version of CodeNameOneBuildClient.jar? 
>
> How do I get the required jars? 
>
> (using IntelliJ IDEA)
>



### **Anonymous** — January 20, 2014 at 3:25 pm ([permalink](/blog/using-the-new-facebook-api/#comment-21982))

> Anonymous says:
>
> It seems there is a bug in the IDEA plugin where Update Client Libs is incorrectly mapped to refresh libs. As a workaround just send a build (to any platform) without this import, it will update the libraries for you after which point you should be able to import this package.
>



### **Anonymous** — February 3, 2015 at 10:55 am ([permalink](/blog/using-the-new-facebook-api/#comment-22090))

> Anonymous says:
>
> Hi, 
>
> I have an application that uses Facebook as a login option but it always asks for the credentials, i.e. instead of clicking on the ‘login with facebook’ and entering my app, it requires the user to enter his Facebook login info even if he is already signed in to Facebook (Web or/and Facebook App)
>



### **Anonymous** — February 3, 2015 at 10:56 am ([permalink](/blog/using-the-new-facebook-api/#comment-22017))

> Anonymous says:
>
> 🙂 forgot to ask the question.. 
>
> What can I do so it will go in straight without the additional required login?
>



### **Anonymous** — February 3, 2015 at 1:42 pm ([permalink](/blog/using-the-new-facebook-api/#comment-24165))

> Anonymous says:
>
> Make sure that on your device settings facebook login is enabled otherwise the native facebook login falls back to web login.
>



### **Mr Emma** — September 26, 2015 at 5:08 pm ([permalink](/blog/using-the-new-facebook-api/#comment-22179))

> Mr Emma says:
>
> i cant generate the key hash.
>
> how do i go about doing that. i keep getting the error
>
> ‘openssl’ is not recognized as an internal or external command,  
> operable program or batch file.
>
> when i use this below –
>
> keytool -exportcert -alias (your_keystore_alias) -keystore (path_to_your_keystore) | openssl sha1 -binary | openssl base64
>



### **Shai Almog** — September 27, 2015 at 3:51 am ([permalink](/blog/using-the-new-facebook-api/#comment-21603))

> Shai Almog says:
>
> In which os?  
> See the newer guide: [http://www.codenameone.com/…](<http://www.codenameone.com/facebook-login.html>)
>



### **Mr Emma** — September 27, 2015 at 7:24 am ([permalink](/blog/using-the-new-facebook-api/#comment-22241))

> Mr Emma says:
>
> Am on the windows 10 OS and I want to generate the key hash for my Android app. I have gone through the new guide my only issue is generating the key hash
>



### **Shai Almog** — September 28, 2015 at 4:08 am ([permalink](/blog/using-the-new-facebook-api/#comment-21652))

> Shai Almog says:
>
> Second entry when googling: “facebook openssl windows”  
> [http://stackoverflow.com/qu…](<http://stackoverflow.com/questions/4347924/key-hash-for-facebook-android-sdk>)
>



### **Mr Emma** — September 28, 2015 at 11:28 pm ([permalink](/blog/using-the-new-facebook-api/#comment-22253))

> Mr Emma says:
>
> thanx it helped alot
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
