---
title: Questions of the Week XXVII
slug: questions-of-the-week-xxvii
url: /blog/questions-of-the-week-xxvii/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xxvii.html
aliases:
- /blog/questions-of-the-week-xxvii.html
date: '2016-10-13'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xxvii/qanda-friday.jpg)

This has been a **REALLY** busy week. We had to release an emergency plugin update during the week to  
workaround a critical issue in the previous plugin update. This was pretty hard!  
To make things extra difficult we had two huge blog posts that I’ve worked on launching, the code itself wasn’t  
such a big deal as is all the hassle around them e.g. appstore submissions are never fun especially when traveling…​

After all these releases we still chose to release an update today  
It doesn’t have any ground breaking changes but does include the refinements I  
[discussed yesterday in the blog](/blog/msuikit-template-inspired-changes.html).

On stack overflow things were as usual:

### Soft back button not working

This should work regardless of where you call `setBackCommand`

[Read on stackoverflow…​](http://stackoverflow.com/questions/39997649/codenameone-soft-back-button-not-working)

### Dropbox plugin?

We have a pretty old [dropbox plugin](https://github.com/chen-fishbein/dropbox-codenameone-sdk) which  
should work but uses the old OAuth solution. Ideally this should be updated to the OAuth2 approach to allow  
upload etc. I know some developers did that but sadly they didn’t contribute their work back…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39986891/codename-one-dropbox-plugin)

### Building from xcode source, missing “pods” library

This is a bit confusing, you need to open the xworkspace file in the new pods build

[Read on stackoverflow…​](http://stackoverflow.com/questions/39970015/building-from-xcode-source-missing-pods-library)

### Codenameone theming sidemenu

We just showed how deeply you can customize the sidemenu in the  
[latest template demo](https://www.codenameone.com/blog/1template-mobile-material-screens-ui-kit.html)

[Read on stackoverflow…​](http://stackoverflow.com/questions/39959882/codenameone-theming-sidemenu)

### Codenameone app version issue

When you use a [build hint](/manual/advanced-topics.html) like `android.versionCode` you need to pay close  
attention. It’s best to avoid that specific hint…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39946836/codenameone-app-version-issue)

### Pubnub-CodeNameOne library – missing methods (Access Manager)

Craig from pubnub gave a great [reference for that](https://www.pubnub.com/docs/codenameone-java/pam-security)

[Read on stackoverflow…​](http://stackoverflow.com/questions/39946067/pubnub-codenameone-library-missing-methods-access-manager)

### Send log file

The device acts differently when using send email

[Read on stackoverflow…​](http://stackoverflow.com/questions/39935004/codenameone-send-log-file)

### Unable to build codename one app after adding admobfullscreen lib

We need to update the old extensions to include the build hints inside them

[Read on stackoverflow…​](http://stackoverflow.com/questions/39924124/unable-to-build-codename-one-app-after-adding-admobfullscreen-lib)

### Listen to side menu / hamburger menu clicked event

We need to implement this functionality in the sidemenu itself but until we do you can just add your own button  
as a side menu button and hide the default side menu button

[Read on stackoverflow…​](http://stackoverflow.com/questions/39909501/listen-to-side-menu-hamburger-menu-clicked-event)

### Navigation between forms fires infiniteContainer refresh

It’s hard to give an answer when I have so little information to work with, I can’t stress this enough: use images  
to illustrate your point…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39905014/navigation-between-forms-fires-infinitecontainer-refresh)

### How to enable Facebook authentication for Codename One on Android

SHA1 works fine on all devices guaranteed

[Read on stackoverflow…​](http://stackoverflow.com/questions/39895414/how-to-enable-facebook-authentication-for-codename-one-on-android)

### ComboBox in codename one

I don’t like `ComboBox` as it derives from `List` which is problematic and adds problems of it’s own…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39887406/combobox-in-codename-one)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
