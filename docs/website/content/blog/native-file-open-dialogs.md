---
title: Native File Open Dialogs
slug: native-file-open-dialogs
url: /blog/native-file-open-dialogs/
original_url: https://www.codenameone.com/blog/native-file-open-dialogs.html
aliases:
- /blog/native-file-open-dialogs.html
date: '2016-11-16'
author: Steve Hannah
---

![Header Image](/blog/native-file-open-dialogs/file-chooser.jpg)

Codename One has always provided access to the device’s photos and videos via `Display.openGallery()` but it hasn’t provided an API to open arbitrary file types because this type of functionality was not available on most mobile platforms. Times have changed and most platforms now offer support for more than just images and photos, so we decided to provide access to this functionality as a cn1lib. Here is a short introduction to the cn1-filechooser library.

### Introduction by Example

I recently developed an app that provided optical character recognition (OCR) on images and PDFs. As you might expect, an app like this needs to allow the user to select a PDF or an image to be converted by some mechanism. Using `Display.openGallery()` I could present the user with access to their images, but I also needed them to be able to select PDFs. The [cn1-filechooser library](https://github.com/shannah/cn1-filechooser) comes to the rescue in this case:

After installing the cn1-filechooser library into my project, I added the following snippet to respond to the event where the user taps the “open” button:
    
    
    ActionListener callback = e->{
       if (e != null && e.getSource() != null) {
           String filePath = (String)e.getSource();
    
           //  Now do something with this file
       }
    };
    
    if (FileChooser.isAvailable()) {
        FileChooser.showOpenDialog(".pdf,application/pdf,.gif,image/gif,.png,image/png,.jpg,image/jpg,.tif,image/tif,.jpeg", callback);
    } else {
        Display.getInstance().openGallery(callback, Display.GALLERY_IMAGE);
    }

The `FileChooser.isAvailable()` should return `true` on iOS, Android, Windows 10 (UWP), JavaSE (the Simulator), and Javascript, so it’s almost not necessary. Nonetheless, I do provide a fallback to the standard image gallery in case I happen to later want to build my app on another platform that doesn’t support the file chooser yet.

Notice that the first parameter to `showOpenDialog()` is a string with a comma-delimited list of extensions and mimetypes. You can include both mime-types and extensions here. The syntax is designed to be compatible with the HTML `file` input’s [`accept` attribute](http://www.w3schools.com/tags/att_input_accept.asp).

### Screenshots

So let’s see what this looks like on the various platforms:

**iOS** :

When you open the file chooser it gives you a list of your device’s installed [document providers](https://developer.apple.com/library/content/documentation/General/Conceptual/ExtensibilityPG/FileProvider.html):

![iOS filechooser](/blog/native-file-open-dialogs/ios-filechooser.png)

In my case I have my iCloud drive (everyone will have this), and Dropbox because I have the Dropbox app installed on my phone. But if you have other apps have DocumentProvider extensions, then those will also be listed.

It also includes an “Images” option that allows the user to browse their local images if they have included any “image” types in the call to `showOpenDialog()`

__ |  On iOS, your App ID **must** include the iCloud entitlement, so, when you create your app ID in your apple developer account, make sure that you check this option.   
---|---  
  
**Android** :

On android it will open a versatile file chooser that allows you to browse and select files in your Google Drive, local images, Downloads, or on internal storage.

![Android filechooser](/blog/native-file-open-dialogs/android-filechooser.png)

**Windows Phone 10** :

On Windows Phone 10, the dialog allows users to browse their OneDrive, or local files.

![Windows Phone 10 filechooser](/blog/native-file-open-dialogs/winphone10-filechooser.png)

**Desktop**

In the desktop builds, the simulator, javascript (on the desktop), and Windows 10 Desktop, you will just see the name file chooser dialog.

### More Information

For more information, you can check out the [cn1-filechooser github repo](https://github.com/shannah/cn1-filechooser).

The best way to install this library is through the Extensions section of Codename One Settings.

You can also try out the [OCR.net](https://www.ocr.net) app which uses the cn1-filechooser plugin.

  * [OCR.net App in iTunes Store](https://itunes.apple.com/ca/app/ocr.net/id1167120765?mt=8)

  * [OCR.net App in Google Play](https://play.google.com/store/apps/details?id=net.ocr.app&hl=en)

  * [OCR.net App in the Windows Store](https://www.microsoft.com/en-us/store/p/ocrnet/9nblggh4343l)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Dave Gunawan** — November 18, 2016 at 2:56 pm ([permalink](https://www.codenameone.com/blog/native-file-open-dialogs.html#comment-23144))

> for the FileChooser, how do I make it so that it takes all types of file ?  
> (instead of just the one listed in the param in example below)
>
> com.codename1.ext.filechooser.FileChooser.showOpenDialog(“.pdf,application/pdf,.gif,image/gif,.png,image/png,.jpg,image/jpg,.tif,image/tif,.jpeg”, callback);
>
> Also I tried it in Simulator and real Android devices (OnePlus2, Nexus 5) and FileChooser.isAvailable() return false …. ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-file-open-dialogs.html)


### **shannah78** — November 18, 2016 at 5:45 pm ([permalink](https://www.codenameone.com/blog/native-file-open-dialogs.html#comment-23095))

> Use “*/*” for all mimetypes. Or.. you should be able to just pass null for the type.
>
> Regarding it returning false in the simulator and on real android devices, there must be a problem in your project’s build.xml file. I’ve responded to your query in Codename One support.
>
> EDIT: I found that there is a bug in the current release. The Simulator doesn’t support “ALL” mimetypes right now. I have made some modifications, but it required changes in both the cn1 core and the lib. They’ll be available in the next plugin update. The cn1-filechooser extension is already updated, and all required changes are on GitHub. To get them before the next plugin update, you’ll need to build CN1 from source and use the modified JavaSE.jar in your project.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-file-open-dialogs.html)


### **Stefan Blomen** — January 18, 2017 at 11:33 am ([permalink](https://www.codenameone.com/blog/native-file-open-dialogs.html#comment-23189))

> 1.) The UWP build fails if FileChooser is used  
> 2.) On some versions of Android (maybe < 5) files with extensions unknwon to the system (i.e. “[test.abc](<http://test.abc>)”) cannot be opened. The ActionEvent is null in this case. I tried several mime types including “*/*”. On an Android device with version 7 it works. Any clues?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-file-open-dialogs.html)


### **shannah78** — January 18, 2017 at 5:29 pm ([permalink](https://www.codenameone.com/blog/native-file-open-dialogs.html#comment-23318))

> shannah78 says:
>
> “The UWP build fails if FileChooser is used”
>
> I have a couple of UWP apps using the file chooser, and they seem to build and work OK. Please open an issue on this and include a simple test case that I can try.
>
> “On some versions of Android (maybe < 5) files with extensions unknwon to the system (i.e. “[test.abc](<http://test.abc>)”) cannot be opened. ”
>
> Please open an issue on this too so it won’t get lost. All of my test cases so far were for common extensions.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-file-open-dialogs.html)


### **Kai** — February 7, 2017 at 1:00 pm ([permalink](https://www.codenameone.com/blog/native-file-open-dialogs.html#comment-23297))

> Kai says:
>
> Is it possible to use this FileChooser as a directory browser, to specify where I want to store a file from my app?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-file-open-dialogs.html)


### **Shai Almog** — February 8, 2017 at 8:35 am ([permalink](https://www.codenameone.com/blog/native-file-open-dialogs.html#comment-23070))

> Shai Almog says:
>
> No.  
> Mobile devices don’t allow that. Notice that if you receive a file from this chooser it might not be the “actual file” you picked but rather a copy of it within the area that your app is allowed to access.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-file-open-dialogs.html)


### **Francesco Galgani** — May 7, 2018 at 4:37 pm ([permalink](https://www.codenameone.com/blog/native-file-open-dialogs.html#comment-23758))

> Francesco Galgani says:
>
> Thank you for this library. For anyone that is in trouble on adding the iCloud entitlement to the App Id (maybe because it’s the first time), I suggest to follow these instructions (that helped me):  
> [https://stackoverflow.com/a…](<https://stackoverflow.com/a/28062326>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-file-open-dialogs.html)


### **Ankush Sharma** — October 11, 2018 at 9:28 am ([permalink](https://www.codenameone.com/blog/native-file-open-dialogs.html#comment-23849))

> Ankush Sharma says:
>
> Hi, Will this work in unity? Is there any other dependency I should know about? Thanks!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-file-open-dialogs.html)


### **Shai Almog** — October 12, 2018 at 4:07 am ([permalink](https://www.codenameone.com/blog/native-file-open-dialogs.html#comment-24094))

> Shai Almog says:
>
> See [https://help.codenameone.co…](<https://help.codenameone.com/en-us/article/whats-codename-one-how-does-it-work-1343oho/>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-file-open-dialogs.html)


### **Thomas McNeill** — December 26, 2018 at 5:56 pm ([permalink](https://www.codenameone.com/blog/native-file-open-dialogs.html#comment-23989))

> Thomas McNeill says:
>
> I was hoping to use the document provider to save to google drive or dropbox. Will we ever have this ability?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-file-open-dialogs.html)


### **Shai Almog** — December 27, 2018 at 4:28 am ([permalink](https://www.codenameone.com/blog/native-file-open-dialogs.html#comment-24056))

> Shai Almog says:
>
> This should be easy to do with native interfaces. Whether we add it depends a lot on user requests. We didn’t get a lot of requests for this feature and no enterprise requests which tend to move things faster.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-file-open-dialogs.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
