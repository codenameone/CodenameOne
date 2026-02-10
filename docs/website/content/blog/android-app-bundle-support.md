---
title: Android App Bundle Support
slug: android-app-bundle-support
url: /blog/android-app-bundle-support/
original_url: https://www.codenameone.com/blog/android-app-bundle-support.html
aliases:
- /blog/android-app-bundle-support.html
date: '2021-05-08'
author: Shai Almog
description: We have added Android App Bundle support which will become the required
  format for submitting apps to Google Play.
---

We have added Android App Bundle support which will become the required format for submitting apps to Google Play.

![](https://www.codenameone.com/wp-content/uploads/2021/05/Google-App-Bundle-Support.jpg)

A few months ago, we added Android App Bundle support and forgot to tell anyone… These things happen sometimes in a fast moving startup… Well, better late than never.

To try the App Bundle support use the build hint: android.appBundle=true

This will produce the regular APK and the app bundle file which you should be able to upload to Google.

### What is Android App Bundle?

For those who aren’t aware… Android App bundle is a new format designed by Google to replace the venerable APK.

It isn’t much different but is generally better suited for splitting so different versions can be sent to different devices more easily.

This makes sense for very fat applications, not so much for Codename One apps which are usually leaner than native Android apps.

In August, Android App Bundle will become the required format for submitting to Google Play. In a couple of weeks we will flip the default so the app bundle build hint will be true implicitly.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Francesco Galgani** — June 6, 2021 at 12:57 pm ([permalink](https://www.codenameone.com/blog/android-app-bundle-support.html#comment-24464))

> Francesco Galgani says:
>
> Since the aab format is not installable on an Android smartphone but can only be used to publish in the store, you will always continue to provide the apk format, right?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fandroid-app-bundle-support.html)


### **Shai Almog** — June 6, 2021 at 2:01 pm ([permalink](https://www.codenameone.com/blog/android-app-bundle-support.html#comment-24466))

> Shai Almog says:
>
> I won’t say “always” since things change but as long as AAB isn’t installable you will need an APK and we’ll provide it.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fandroid-app-bundle-support.html)


### **Chris Vorster** — September 11, 2021 at 12:56 am ([permalink](https://www.codenameone.com/blog/android-app-bundle-support.html#comment-24479))

> Chris Vorster says:
>
> Trying to upload a new version of the AAB on Google Dev Console results in signing error. Cannot find a way to fix this, tried support also.  
> “Your Android App Bundle is signed with the wrong key. Ensure that your App Bundle is signed with the correct signing key and try again. Your App Bundle is expected to be signed with the certificate with fingerprint:…”
>
> Can’t seem to find any solutions on how to deal with this.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fandroid-app-bundle-support.html)


### **Lianna Casper** — September 11, 2021 at 4:12 am ([permalink](https://www.codenameone.com/blog/android-app-bundle-support.html#comment-24480))

> Lianna Casper says:
>
> You need to pick the right keystore in Codename One Settings. How did you sign the app you first uploaded?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fandroid-app-bundle-support.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
