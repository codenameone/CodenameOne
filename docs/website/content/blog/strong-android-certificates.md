---
title: Strong Android Certificates
slug: strong-android-certificates
url: /blog/strong-android-certificates/
original_url: https://www.codenameone.com/blog/strong-android-certificates.html
aliases:
- /blog/strong-android-certificates.html
date: '2017-01-30'
author: Shai Almog
---

![Header Image](/blog/strong-android-certificates/security.jpg)

When Android launched RSA1024 with SHA1 was considered strong enough for the foreseeable future, this hasn’t changed completely but the recommendation today is to use stronger cyphers for signing & encrypting as those can be compromised.

APK’s are signed as part of the build process when we upload an app to the Google Play Store. This process seems redundant as we generate the signature/certificate ourselves (unlike Apple which generates it for us). However, this is a crucial step as it allows the device to verify upgrades and make sure a new update is from the same original author!

This means that if a hacker takes over your account on Google Play, he still won’t be able to ship fake updates to your apps without your certificate. That’s important since if a hacker would have access to your certificate he could create an app update that would just send him all the users private information e.g. if you are a bank this could be a disaster.

Android launched with RSA1024/SHA1 as the signing certificates. This was good enough at the time and is still pretty secure. However, these algorithms are slowly eroding and it is conceivable that within the 10-15 year lifetime of an app they might be compromised using powerful hardware. That is why Google introduced support for stronger cryptographic signing into newer versions of Android and you can use that.

### The Bad News

There is a downside…​

Google only introduced that capability in Android 4.3 so using these new keys will break compatibility with older devices. If you are building a highly secure app this is probably a tradeoff you should accept. If not this might not be worth it for some theoretical benefit.

Furthermore, if your app is already shipping you are out of luck. Due to the obvious security implications once you shipped an app the certificate is final. Google doesn’t provide a way to update the certificate of a shipping app. Thus this feature only applies to apps that aren’t yet in the play store.

### The Good

If you are building a new app this is pretty easy to integrate and requires no changes on your part. Just a new certificate. You can generate the new secure key using instructions in articles like [this one](https://guardianproject.info/2015/12/29/how-to-migrate-your-android-apps-signing-key/).

If you are using Codename One Setting we will have a new option in the next update to generate an SHA512 key which will harden the security for the APK:

![New SHA512 option for Android key generation](/blog/strong-android-certificates/sha512-settings.png)

Figure 1. New SHA512 option for Android key generation

### Security

As you might have noticed we made a lot of posts about hardening the security of Codename One applications, we’d like to make Codename One applications more secure by default and make it easy for you to find what you need in regards to security.

One part of that is an introduction of a new security tag to the blog. We’ll also include more detailed documentation on hardening a Codename One application in an upcoming blog post and within the developer guide.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Miguel Munoz** — August 14, 2020 at 10:22 pm ([permalink](https://www.codenameone.com/blog/strong-android-certificates.html#comment-24321))

> Where does it put the certificate? I’m moving my source to GitHub, and I naturally don’t want the certificates stored there, too. I found the iOS certificates in a folder called iosCerts, and excluded those from github, but I can’t find them for the android build. What files should I exclude?
>



### **Shai Almog** — August 15, 2020 at 5:17 am ([permalink](https://www.codenameone.com/blog/strong-android-certificates.html#comment-24325))

> Shai Almog says:
>
> It should be under your users home directory under the “.codenameone” directory. So no worries.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
