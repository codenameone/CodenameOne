---
title: Questions of the Week VIII
slug: questions-of-the-week-viii
url: /blog/questions-of-the-week-viii/
original_url: https://www.codenameone.com/blog/questions-of-the-week-viii.html
aliases:
- /blog/questions-of-the-week-viii.html
date: '2016-06-02'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-viii/qanda-friday.jpg)

It’s been a remarkably busy week with so many big announcements and it’s shaping up to be a very busy  
month…​ We wanted to release a new plugin update this week but due to some external pressure we will  
update the plugin next week and keep this Friday update only to the libraries.

On stackoverflow there were quite a lot of questions:

### Is there a way in which one can read data stored in a file with extension of .sql

Shipping an app with a default “starter” sql database is pretty common and still pretty confusing.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37591037/is-there-a-way-in-which-one-can-read-data-stored-in-a-file-with-extension-of-sq/37605869)

### How to migrate my “Codenameone” Project from (java 5) to (Java 8)

This is a pretty common FAQ about the move to Java 8, we should blog about it more as clearly this isn’t clear  
enough…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37596326/how-to-migrate-my-codenameone-project-from-java-5-to-java-8/37605789)

### Why is the JSONParser always returning a double?

One painful ommission from the API subset supported by Codename One is the Number class. We’d really like  
to fix that ommission soon.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37477926/why-is-the-jsonparser-always-returning-a-double)

### Why won’t my MenuBar show?

As a product that evolved over time the “right way” to do things has changed drastically over the years and sometimes  
the legacy support is hard to distinguish. In this case legacy doesn’t really warrant the overly harsh deprecation  
but confuses developers still…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37475660/why-wont-my-menubar-show)

### Right side menu shadow is flipped

This was a regression we introduced with this weeks release, it should be fixed by the time you read this…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37475286/codename-one-right-side-menu-shadow-is-flipped)

### Codename1 Android not building on 3.4

Some issues with Android builds and google play are just painful, we addressed most of those problems but sometimes  
walking thru the maze of build hints is non-trivial.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37472878/codename1-android-not-building-on-3-4)

### Need examples headers and cookies

Working with cookies in Codename One is something that is quite missing from the documentation, we should  
bolster that at least by asking questions…​.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37470283/need-examples-headers-and-cookies)

### BrowserComponent / WebBrowser doesn’t close when back is pressed

When you use the andWait version of methods sometimes weird things happen because the rest of the execution  
chain doesn’t go thru. You should use these methods but be prudent about it…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37466886/browsercomponent-webbrowser-doesnt-close-when-back-is-pressed)

### CodeName One error: cannot find symbol

You can’t change the class/package name after creation easily. You need to change it everywhere within the  
properties files and the project. It’s non-trivial.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37590391/codename-one-error-cannot-find-symbol)

### Library update broke textboxes

This was already fixed by the time the question was asked. FYI you can see commit history in the github project  
and you can subscribe to notification as explained in  
[this post](/blog/keep-track-of-codename-one-changes-duplicates.html).

[Read on stackoverflow…​](http://stackoverflow.com/questions/37576427/codenameone-library-update-broke-textboxes)

### Codename one missing from NetBeans plugins on Ubuntu

Turns out OpenJDK is quite problematic with NetBeans plugins and various other tools including Codename One.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37575313/codename-one-missing-from-netbeans-plugins-on-ubuntu#37575313)

### Setting image as background is not working

Labels/Buttons hide themselves when they are blank. This is often confusing to developers but is quite useful  
for many cases…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37567718/setting-image-as-background-is-not-working)

### Black content pane instead of tiled background

If you ask a question that has a visual aspect a screenshot always goes a long way to help with the answer.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37566899/black-content-pane-instead-of-tiled-background)

### Some questioning about codenameone

This question is already marked for deletion by moderators so it might not be there by the time you see this…​  
Generally product opinion questions are frowned upon in stack overflow as they tend to promote religious debate.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37555496/some-questioning-about-codenameone)

### Writing a string at a given position in a drawing

The position of a drawing are slightly different from the Swing/AWT convention. This is a common  
pitfall for developers.  
You should also keep in mind that component size can always change (and frequently does)…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37552960/writing-a-string-at-a-given-position-in-a-drawing-in-codename-one)

### Styling the side Menu

The sidemenu is quite customizable in the Toolbar API. We lost some of the customization abilities we had in the  
old SideMenuBar but we gained a lot of simplicity and usability in return…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37546781/codename-one-styling-the-side-menu)

### InfiniteProgress component issue in postForm()

Handling progress while doing something else usually falls flat with people abusing the EDT or failing to account  
for the fact that things are still happening in the background…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37544243/infiniteprogress-component-issue-in-postform-codenameone)

### Change app language in emulator

We should probably add deeper configuration for this as we move forward but right now you can hack a lot  
of things relatively easily by just manipulating the VM arguments for the simulator…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37541579/codename-one-change-app-language-in-emulator)

### Zipme implementation

The newly announced [ZipSupport](https://github.com/codenameone/ZipSupport/) cn1lib already has some  
interest. We didn’t get a chance to document it as it was pretty much a quick port. Since it’s so similar to the  
JavaSE version of zip support it should be pretty easy to use.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37519402/zipme-implementation-in-codenameone)

### The app needs to be configured to android Tablets only from Google play store

We have a combo box to configure this for iOS builds but Google made this a bit more “interesting” and  
the definition of what is a tablet on the play store is also pretty challenging…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37517779/the-app-needs-to-be-configured-to-android-tablets-only-from-google-play-store)

### Show image only on specific device

Different UI’s for tablets/phones is a pretty common question.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37514465/codename-one-show-image-only-on-specific-device)

### ios.googleAdUnitId bottom banner doesn’t show on iPhone 6 and 6+ in landscape mode

Most developers don’t use banner ads anymore as they provide very poor returns so we just don’t get that many  
bug reports on our banner ad support. This issue is probably related to the version of Google play services we  
use in the iOS builds. Thanks to the latest cocoapods support we might be able to solve this effectively.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37509805/ios-googleadunitid-bottom-banner-doesnt-show-on-iphone-6-and-6-in-landscape-mo)

### Re fade animation not working properly

This has been an ongoing question that we’re trying to track down unsuccessfully. Unfortunately narrowing this  
down to see the problem whether in the code or in Codename One requires something more terse.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37505911/re-fade-animation-not-working-properly-codenameone)

### Why can’t I select the first item in the side menu?

If the top most item is very small and near the area of the status bar of the phone it will be impossible to touch it  
as the native OS will mistake your taps for attempts to touch the status bar area. The solution is not to place  
elements that high and style things to have enough space for touch.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37494902/why-cant-i-select-the-first-item-in-the-side-menu)

### Why are my menu items too small or too big?

When using the native theme it’s important to make sure that elements are styled correctly. The native theme  
doesn’t always override elements such as `SideComponent` etc. out of the assumptions that these will always be  
styled by the user.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37479232/why-are-my-menu-items-too-small-or-too-big)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
