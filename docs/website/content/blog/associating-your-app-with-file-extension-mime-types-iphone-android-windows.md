---
title: Associating Your App with File Extension/Mime Type on iPhone (iOS), Android
  & Windows
slug: associating-your-app-with-file-extension-mime-types-iphone-android-windows
url: /blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/
original_url: https://www.codenameone.com/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows.html
aliases:
- /blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows.html
date: '2016-11-07'
author: Steve Hannah
---

![Header Image](/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/file-type-associations.jpg)

One of the compelling reasons to go native (vs say a web app) is to better integrate with the platform. One form of integration that is frequently handy is the ability register your app to handle certain file types so that it is listed as one of the options when a user tries to view a file of that type. Codename One supports this use case via the “AppArg” display property – the same, simple mechanism used for handling custom link types in your app.

With the “Meme Maker” demo that I just created, I wanted users to be able to select a photo from another app (like Photos on Android), and send it directly to the Meme Maker app as the basis for creating a Meme.

In case you’re not familiar with “Memes”, they are those sometimes annoying photos that litter your facebook feed with witty captions laid over them. E.g:

![Example cat meme](/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/example-cat-meme.jpg)

Figure 1. Example cat meme

Meme Maker is a very simple app. It allows the user to select a photo, and it provides some text laid over the photo which the user can edit. When the meme is finished, it can be exported as an image, and/or shared to Facebook or other social media.

All this is simple to do in Codename One. Selecting an image, can be achieved using `Display.openGallery()`, which allows the user to choose from one of the images in their device’s photos. Sharing an image can be achieved via `Display.execute()` or with the `ShareButton` component.

I wanted to go a step further, though, so that users could launch MemeMaker directly from their Photos app. For a simple app like this, allowing the user to “Share” an image **to** the app can significantly improve the user experience.

### How to Register an App to open a File Type

Registering your app to open a a file type involves two parts:

  1. Add some build hints to inject the appropriate metadata into each native platform config files (e.g. The info.plist on iOS, the manifest file on Android, etc..) to inform the native platform that the app can open the specified file types.

  2. Check for `Display.getInstance().getProperty("AppArg", null)` at the beginning of your app’s `start()` method to see if the app was opened as a result of file being opened or shared. If present, it will be the path to a file that you can access using `FileSystemStorage`.

### An example from the “Meme Maker” demo

Lets’ start by looking at the code that handles the “AppArg”. At the beginning of the `start()` method we have:
    
    
    Display disp = Display.getInstance();
    String arg = disp.getProperty("AppArg", null);
    if (arg != null) {
        disp.setProperty("AppArg", null);
        disp.callSerially(()->{
            fireImageSelected(arg);
        });
    }

So, what we’ve done here is

  1. Check the “AppArg” property.

     1. If it is not null, we set it null (just so we don’t mistake it being set in future starts).

     2. I use `callSerially()` to defer the actual selection of the image until after the rest of the `start()` method has run. That is app-specific, and not necessary in general for processing app arguments.

That’s all there is to it.

Now the app is equipped to “handle” files that are passed to it on startup. However we still need to register the app with each platform so that the operating system knows to make our app available as a share target (or an “open with” target).

### Android-Specific Configuration

There are two build-hints related to Android that we will need to employ:

  1. `android.activity.launchMode=singleTask`.

The default launch mode for Codename One apps is “singleTop”. Unfortunately this doesn’t really work very well if the app can be launched from other apps to open files. I won’t go into specifics here about the differences between “singleTop” and “singleTask” launch mode. Just know that if you want your app to work properly as a share target, you need to set this build hint to “singleTask”.

You can read more about Android’s `activity:launchMode` directive [here](https://developer.android.com/guide/topics/manifest/activity-element.html).

  2. `android.xintent_filter`

This is where we add the `<intent-filter>` tags to be injected into the app’s manifest file. These filters will register our app to open specific file types. The value I used for **Meme Maker** is:
         
         <intent-filter>
             <action android_name="android.intent.action.SEND" />
             <category android_name="android.intent.category.DEFAULT" />
             <data android_mimeType="image/*" />
         </intent-filter>
         <intent-filter >
             <action android_name="android.intent.action.VIEW" />
             <category android_name="android.intent.category.DEFAULT" />
             <data android_mimeType="image/*" />
         </intent-filter>

The first filter says that the app is an eligible “share” target for files with mimetype “image/**“. The second says that the app is eligible to “Open” files with mimetype “image/** “. Each is used in different instances. A simple way to think of this is, from a Codename One’s app perspective:

     1. `Display.execute(filepath)` – will allow the user to “open” the file using apps that have registered an appropriate intent filter with action `android.intent.action.VIEW`.

     2. `Display.share(null, filepath, "image/png")` – will allow the user to “send” the file to an app that has registered an appropriate intent filter with action `android.intent.action.SEND`.

Here are some sceen-shots of how the integration looks on my Nexus 5.

I did a search on Google for “blank meme photos”. Once I found a photo, I did a long-press on the image (in Chrome), to open the context menu:

![Android chrome context menu](/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/android-chrome-context-menu.png)

Figure 2. Android chrome context menu

Then when I tap on “Share”, it gives me a list of the apps that I can share this image to. MemeMaker is listed there:

![Share image to meme maker](/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/android-chrome-sharing-menu.png)

Figure 3. Share image to meme maker

Then it opens Meme Maker with the image already loaded into the background:

![Meme maker with preloaded image](/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/android-mememaker-fistpump.png)

Figure 4. Meme maker with preloaded image

### iOS-Specific Configuration

On iOS, we only need concern ourselves with one build hint:

`ios.plistInject`

The content I used for the Meme Maker app is:
    
    
    <key>CFBundleDocumentTypes</key>
    <array>
        <dict>
            <key>CFBundleTypeName</key>
            <string>image</string>
            <key>CFBundleTypeRole</key>
            <string>Viewer</string>
            <key>LSHandlerRank</key>
            <string>Alternate</string>
            <key>LSItemContentTypes</key>
            <array>
                <string>public.image</string>
            </array>
         </dict>
    </array>

Don’t be intimidated by this snippet. There’s a lot there, but for the most part it is just boiler-plate copy and paste. Here is a break-down of the values and their meaning:

  1. `CFBundleTypeName` – A name for this bundle type. You can provide pretty much any value you want here. I used “image”, but it could have been “foo” or “bar”.

  2. `CFBundleTypeRole` – The role of this app. In our case I’m just registering it as an image viewer. The value can be Editor, Viewer, Shell, or None. This key is required.

  3. `LSHandlerRank` – How iOS ranks the relevance against other apps that open this file type. Possible values: “Owner”, “Alternate”, “Default”, “None”

  4. `LSItemContentTypes` – A list of the content types that are being registered to be opened by the app. iOS uses UTIs instead of mimetypes here. The `public.image` UTI is basically the same as the `image/*` mimetype. You can see a list of all public UTIs [here](https://developer.apple.com/library/content/documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html).

__ |  iOS has (at least) two different mechanisms for handling file types in your app. The above `ios.plistInject` value will register the app to be able to “Open” an image file, but it won’t allow it to receive it as a share target. The distinction is subtle and it depends on what mechanism is used to launch the “Open with” or “Share” dialog in the source app. E.g. If you view a PDF inside Safari, it will provide an “Open with…​” (label changed to “More…​” in iOS 10) link in the top left, which, if tapped, will provide the user with a list of registered apps that can open a PDF. If our app was registered to open a PDF in the same way that it is registered to open images, then our app would appear in this list of elligible apps.   
---|---  
  
However, there is also a “Share” button at the bottom of the screen in Safari. This won’t include our app as it uses a different mechanism for registering apps. Registration to appear in this menu is more complicated and beyond the scope of this post.

Unfortunately I couldn’t find an example in the latest OS where an app provides “Open with” an image. It seems that things are shifting towards “sharing” when images are involved, and as I mentioned above, this is a little more complex. However, for other files types, like PDF, the “open with” workflow is still common. For example, here is a sample of a PDF as viewed in iOS’ Safari. If I tap on the PDF, it provides a little menu along the top with a “More…​” option, as shown here:

![iOS Open with menu bar](/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/ios-pdf-open-with.png)

And when I tap on “More…​” I see:

![iOS Open with dialog](/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/ios-open-with-dialog-ocr.png)

Figure 5. iOS Open with dialog

The first application listed here is “OCR.net”, which is an app that I developed using Codename One. It includes the following `ios.plistInject` directive to be shown here:
    
    
    <key>CFBundleDocumentTypes</key>
    <array>
        <dict>
        <key>CFBundleTypeName</key>
            <string>pdf</string>
            <key>CFBundleTypeRole</key>
            <string>Viewer</string>
            <key>LSHandlerRank</key>
            <string>Alternate</string>
            <key>LSItemContentTypes</key>
            <array>
                <string>com.adobe.pdf</string>
            </array>
        </dict>
        <dict>
            <key>CFBundleTypeName</key>
            <string>image</string>
            <key>CFBundleTypeRole</key>
            <string>Viewer</string>
            <key>LSHandlerRank</key>
            <string>Alternate</string>
            <key>LSItemContentTypes</key>
            <array>
                <string>public.image</string>
            </array>
        </dict>
    </array>

### Windows-Specific Configuration

The process for UWP is similar to both iOS and Android. In this case we use the `windows.extensions` directive to inject content into the windows manifest file. In this case, we use:
    
    
    <uap:Extension Category="windows.fileTypeAssociation">
        <uap:FileTypeAssociation Name="image">
            <uap:Logo>imagesicon.png</uap:Logo>
            <uap:SupportedFileTypes>
                <uap:FileType ContentType="image/jpeg">.jpg</uap:FileType>
                <uap:FileType ContentType="image/jpeg">.jpeg</uap:FileType>
                <uap:FileType ContentType="image/gif">.gif</uap:FileType>
                <uap:FileType ContentType="image/png">.png</uap:FileType>
            </uap:SupportedFileTypes>
        </uap:FileTypeAssociation>
    </uap:Extension>

With this build hint, our app is registered to open files with .jpg, jpeg, .gif, and .png files. On the desktop, this means you can right click on files of these types, select “Open with” in the contextual menu, and then select “Meme maker” as shown here:

![Windows 10 open with option](/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/uwp-open-with.png)

For more information about the available options in UWP, see [handling file activation](https://msdn.microsoft.com/en-us/windows/uwp/launch-resume/handle-file-activation) on MSDN.

#### Windows 10 Share Targets

As with iOS and Android, Windows 10 treats “share targets” slightly differently than file associations. The `FileTypeAssociation` tag registers the app to be able to “open” files of the specified types, but it doesn’t register to be a share target. Share targets are those apps that appear in the sharing dialog when a users chooses “Share” from a context menu. E.g. When I right click on this image in Edge, it gives me an option to “Share” the image:

![Windows 10 share picture](/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/windows-10-share-picture.png)

On the desktop, this will open a sidebar with a list of applications to which this image can be shared:

![Windows 10 share sidebar](/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/windows-10-share-sidebar.png)

In the above screenshot, notice that Meme Maker is listed as one of the apps. This version of MemeMaker was built using an some additional options in the `windows.extensions` build hint:
    
    
    <uap:Extension Category="windows.shareTarget">
      <uap:ShareTarget Description="Images">
        <uap:SupportedFileTypes>
          <uap:FileType>.jpg</uap:FileType>
          <uap:FileType>.gif</uap:FileType>
          <uap:FileType>.png</uap:FileType>
          <uap:FileType>.jpeg</uap:FileType>
        </uap:SupportedFileTypes>
        <uap:DataFormat>StorageItems</uap:DataFormat>
      </uap:ShareTarget>
    </uap:Extension>

__ |  For more information about the “windows.shareTarget” category, see [Microsoft’s docs](https://msdn.microsoft.com/en-us/library/windows/apps/br211466.aspx) on the subject.   
---|---  
  
With this share target information, the app was listed in the Sharing sidebar when an image file was shared by another app. Selecting “Meme maker” in this sidebar would open Mememaker inside the sharing sidebar as shown here:

![Meme maker loaded inside Windows 10 sidebar](/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows/meme-maker-in-uwp-sharing-sidebar.png)

Figure 6. Meme maker loaded inside Windows 10 sidebar

__ |  Ultimately I opted not to include the “shareTarget” functionality in the finished app because it resulted in some peculiar behaviour when the app was opened in both the sharing sidebar and as a stand-alone app.   
---|---  
  
### Get the Meme Maker App

  1. [On the Play Store](https://play.google.com/store/apps/details?id=com.codename1.demos.mememaker)

  2. [In the Windows Store](https://www.microsoft.com/en-us/store/p/codename-one-meme-maker/9nblggh441nf)

  3. [In the iTunes Store](https://itunes.apple.com/us/app/codename-one-meme-maker/id1171538632)

  4. [On GitHub](https://github.com/shannah/mememaker)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Carlos** — November 8, 2016 at 4:03 pm ([permalink](https://www.codenameone.com/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows.html#comment-23053))

> Excelent.
>
> One big step forward would be to read Exif rotation and correct the image accordingly, as this is something that happens very often. Most devices don’t actually rotate pictures, but mark them as such in the exif data.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fassociating-your-app-with-file-extension-mime-types-iphone-android-windows.html)


### **bryan** — November 8, 2016 at 7:37 pm ([permalink](https://www.codenameone.com/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows.html#comment-22806))

> Great tutorial Steve – thanks.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fassociating-your-app-with-file-extension-mime-types-iphone-android-windows.html)


### **Shai Almog** — November 9, 2016 at 7:31 am ([permalink](https://www.codenameone.com/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows.html#comment-23043))

> Thanks.
>
> AFAIK we already do that implicitly in our capture implementation.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fassociating-your-app-with-file-extension-mime-types-iphone-android-windows.html)


### **Carlos** — November 9, 2016 at 9:09 am ([permalink](https://www.codenameone.com/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows.html#comment-23085))

> This is what I get in what should be a vertical pic…
>
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/eb29ed44f9b8ffdb82216b13f3e785b9cfbe3f9fe055c4468ee3ee1dc9cda839.png>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fassociating-your-app-with-file-extension-mime-types-iphone-android-windows.html)


### **Shai Almog** — November 10, 2016 at 4:57 am ([permalink](https://www.codenameone.com/blog/associating-your-app-with-file-extension-mime-types-iphone-android-windows.html#comment-22989))

> Looking at the code it seems to no longer be there, not sure why. I’ll have to ask on that.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fassociating-your-app-with-file-extension-mime-types-iphone-android-windows.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
