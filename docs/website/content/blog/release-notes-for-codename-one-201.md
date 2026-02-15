---
title: Release Notes For Codename One 2.0
slug: release-notes-for-codename-one-201
url: /blog/release-notes-for-codename-one-201/
original_url: https://www.codenameone.com/blog/release-notes-for-codename-one-201.html
aliases:
- /blog/release-notes-for-codename-one-201.html
date: '2013-12-22'
author: Shai Almog
---

![Header Image](/blog/release-notes-for-codename-one-201/codename-one-charts-1.png)

We will release the full Codename One 2.0 in a couple of days in the meantime here are the release notes covering the changes for this version. 

**  
Highlights Of This Release  
**

  * Support for IntelliJ IDEA  

  * Support for Google Play Ads on iOS/Android  

  * Support for 3rd party libraries using the cn1lib format  

  * Major improvements to the Windows Phone 8 port making it far more reliable  

  * Fine tuned performance on iOS optimized garbage collector behavior  

  * Extensive code migration to use Java 5 features and collections, generified some core interfaces  

  * XML mode for Codename One Designer allows for better usage in source control scenarios  

  * Improved text input and browser support in the simulator  

  * Native Facebook login support on iOS/Android  

  * Native share functionality on iOS  

  * Support for iOS 7 appearance/functionality, including new device skins  

  * New native picker API allows opening time/date picker of the native platform when applicable  

  * Support for push notification on blackberry  

  * Added export/import of CSV/Android XML string bundles (localization) to the designer tool  

  * Added standardized API for pinch to zoom (simulated by a right click drag in the simulator)  

  * Image background styles can now be scaled to fit and fill as well as the many other options  

  * Updated developer guide increasing its content by more than 30% to 200 pages 

**  
New Components & Functionality of note  
**

  * Image viewer class allowing pinch to zoom and swiping between images  

  * Auto complete text field component  

  * Layered pane allows a more refined way of placing components on top of the entire UI  

  * Infinite scroll adapter allowing a user to scroll a container and add  
  
elements as he reaches the bottom  

  * Many improvements to the SideMenuBar – swipe to open, title commands, side menu or top and to the right  

  * Span button – button component that can span multiple lines  

  * Span label – label component that can span multiple lines  

  * Made location manager simpler to work with by adding a synchronous method to  
  
fetch location  

  * Added JSObject API contributed by Steve Hannah  

  * Added an XMLWriter API  

  * Added new demos: Poker game, Push  

  * Added image rotation API’s designed for camera images that can rotate or flip an image by square angles, this is important for rotating an image that isn’t a perfect square (e.g. taken by camera)  

  * Added support for the GridBagLayout from AWT  

  * Added access point support to the Android port (contributed by Fabricio from Pumpop)  

  * Added gzip stream support and a GZipConnectionRequest 

**  
Details  
**

  * Changed friction values in scrolling algorithm  

  * Improved text area input handling in iOS by making it use the Codename One styling and have next/done buttons  

  * Made media event callbacks on Android happen on the EDT  

  * Tabs are now more sensitive to swipe gestures  

  * Improved the simulator to behave more like a device in terms of event delivery, filesystem mappings and more  

  * Fixed issue 794 which prevented Codename One from running on some older J2ME devices due to usage of UnsupportedOperationException in the retroweaver code  

  * Simulator can now simulate capturing audio/video  

  * Fix for contact images in J2ME to work correctly  

  * Rewrote event processing to be much faster and reduce GC thrushing  

  * Removed some usage of Hashtable to speed up rendering  

  * Fixed locationManager to call providerStateChanged once the gps status has changed  

  * Fixed getCurrentLocation to return fast – get the current location if exists then fallback to network location.  

  * Fix for issue 825 : iOS BrowserComponent doesn’t provide error codes on error  

  * Implemented RFE 826: Network activity indicator while loading pages on iOS WebBrowser  

  * Fix for issue 893 in the Codename One designer : Unselected style lost properties when delete Selected style  

  * Fix for issue 944 : DateSpinners can’t be disabled  

  * Fix for issue 949 : Dragging Component inside scrollable container doesn’t properly scroll the container in both directions  

  * Fixed the WebBrowser component to no longer flicker in the simulator!  

  * Fix for Issue 960 : Result.getAsInteger(path) throws number format exception  

  * Improved simulator text input with fixes for Issue 957 : Simulator TextField misc bugs  

  * Fixed audio on Android to pause the playing media when a call comes in  

  * Fixed android native editing to fire the event only once the data has changed  

  * Fix for a checkbox/mutli-button issue. When the multi button is in checked mode within a MultiList the emblem icon sometimes disappears  

  * Fixed issue 955 : hamburger menu command not respecting enabled  

  * Added ability to “inject” additional cookies into a connection request  

  * Fix for storage file names to be normalized  

  * Made the framerate lock faster to smooth up animation a bit  

  * Fixed back command behavior in the side menu with the GUI builder which is triggered because the command is both added and set to the form  

  * Fixed the XML parser for reading some files with BOM  

  * Fix for an issue where opening the mail app caused the app to restart  

  * Fixed Audio playback not to crash on exceptions thrown by the Android device  

  * Fixed an SMS sending issue on blackberry devices and J2ME devices  

  * Made minor refinements for iOS peer cleanup and push notification behavior in case of an error  

  * Some improvements to memory utilization in iOS as part of fixes for issue 926 : iOS crash with sounds playing  

  * Fix for issue 946 : Dropdown on Windows Phone is transparent  

  * Fixed native commands on Android to refresh correctly when modified for cases where withText or other hints were used  

  * Fixed issue 939 : Two Memory Leaks in iOS Obj C Code  

  * Fixed the designer to hide properties that are overriden by custom component attributes  

  * Fix for RFE 936: text shadow bottom right (i.e. drop shadow)  

  * Fixed push registration to not fail completely in China  

  * Fixed full screen ads which had a race condition that prevented ad skipping after clicking  

  * Added support for array arguments to connection requests  

  * Fixed fail silently to not broadcast fail events anymore  

  * Added support for timeout in the location manager sync request  

  * Fixed a potential Android exception that can happen during background playback of audio due to display not checking whether it is initialized  

  * Added auto complete text field to the GUI builder  

  * Fixed User-Agent behavior on iOS to return the correct user agent value  

  * Fixed Android audio playback to return playing false when buffering and also not fail with exception on some cases.  

  * Fixed iOS audio capture  

  * Fixed an issue with Image capture on iOS  

  * Fixed media playback hints for iOS  

  * Fix for issue 937 : GenericSpinner does not allow you to provide new renderer via the setRenderer() method  

  * Fixed Android web progress dialogs to dismiss after 10 sec max  

  * Fixed Android’s back button on video to not minimize the app  

  * Fix for Issue 917 : Keyboard hide sometimes crashes on Android 4.3 – changed the SurfaceView to TextureView for SDK version > 18  

  * Implementation of RFE 928: Add “Contains” operator for Expression Language Contributed by Eric Coolman  

  * Fix for killAndWait which seems to have violated the EDT since its inception  

  * Fix for an issue with sidemenu bar with the keyboard open  

  * Fixed a couple of sidemenu issues with screen resize and dragging the side menu from the open menu button.  

  * Fixed the dragStop to check all parents if it’s in a middle of a scroll gesture  

  * Fixed an issue with multibutton drag and drop  

  * Fixed an issue with pointer released being invoked on a component when stopping a swipe scroll operation  

  * Added an option to customize the cookie HTTP header to workaround a bug in PHP which isn’t treating the cookie header as case insensitive  

  * Fixed issue with side swiping the side menu bar where pressing again while swiping back a menu that was previously swiped in triggered the seamless creation of a new menu that didn’t have the limits of the old menu thus allowed dragging the menu to cover the entire screen  

  * Added action listener to slider to allow more coarse event dispatch  

  * Fixed exception handling behavior in addToQueueAndWait  

  * Fixed a bug in android where pressing back while dialog was showing minimized the app  

  * Fixed TextField right alignment issue (mostly relevant for rtl langs)  

  * Added rtl support to the android native editing  

  * Fixed an issue with video capture on nexus 7 devices  

  * Implemented RFE 919: TextArea – Localization by adding a span button  

  * Improved Analytics API that integrates with the application analytics  

  * Changed the detect network URL to google.com and made it configurable  

  * Fix for dialog regression where the onShow event wasn’t fired for Dialog  

  * Changed isTablet implementation to better detect tablets  

  * Fixed bug in ImageIO.save() where the image was recycled too soon  

  * Fixed Capture to save the scaled versions with a .jpg extension to allow the gallery to identify the file and show a preview in the gallery  

  * Fixed issue 918 Sidemenu closes automatically when keyboard is open  

  * Fixed LayeredLayout to consider the margins in the calc size  

  * Implemented RFE 898: Textfield hint is not displayed while keyboard is open  

  * Fixed issue 901 : Polling push support not calling registeredForPush  

  * Fixed camera implementation based on issue 913  

  * Updated capture to use the version of the ImageIO.save() method that accepts a file path to prevent out of memory errors  

  * Added support for non-cyclic flicking of the ImageViewer curtesy of Fabrício Carvalho Cabeça of Pumpop  

  * Fixed barcode reading crash on iOS 7  

  * Fixes push with meta data that had a race condition with the initialization of Display.  

  * Fixed remote control playback, so events from the screensaver on iOS will be delivered  

  * Added Media information API’s and an OS version API  

  * Fixed several crashes related to images and image scaling  

  * Added new Media API’s allowing the developer to pass various hints to the playing media. Currently the first batch of hints allow developers to manipulate the “now playing” screen in iOS  

  * Improved notify status bar to include hints including the ability for a non-dismissible notice, added the option to dismiss a notice manually  

  * Provided an API to indicate whether notifications are supported  

  * Added an option to hide the zoom map keys as a theme constant  

  * Fix for commands, two identical commands with different client properties would resolve as equal and only one of them will get added  

  * Added an OSVer constant to Display.getProperties() to return the OS version  

  * Fixed issue 911 NPE occurs in TextArea constructor when input text is null  

  * Fixed an issue with hints on List disappearing in the designer  

  * Fixed an exception in the designer in EditableResources  

  * Fixed push with type 3 to work as expected  

  * Made logging on Android smarter, instead of using System.out we will now use Log.d with the application name.  

  * Fixed an issue with purchase where the success callback wasn’t sent to the activity  

  * Fixed Issue 890 : TitledMap shows over open SideMenuBar on ios  

  * Added an ability to drag vertically when a horizontal scroll component is in the hierarchy  

  * Fixed a potential edge case in RMS storage on J2ME devices  

  * Fixed issue 900 (repeatedly) OutOfMemoryError in XMLParsing  

  * Fixed issue 896 : XMLParser endTag callback is not called for self-closing tags  

  * Fixed issue 892 : text field with URL/email constraint still starts with upper case  

  * Fixed issue 895 : OnOffSwitch alternate text does not apply on first display  

  * Fixed Android image loading to use Bitmap.Config.ARGB_8888 which should give better UI fidelity and possibly better performance  

  * Fixed issue 889 : OnOffSwitch state restoring  

  * Added support for RFE 880: scroll event callback mechanism  

  * Fixed an issue where drag events when entering the side menu set the dragged variable and then when we got back from it the button wouldn’t function on first touch.  

  * Added feature for a single softbutton on J2ME devices.  

  * Made UIBuilder throw a nicer exception when failing to show a non-existing form  

  * Fixed issue 884 in the designer : Derive All does not work  

  * Fixed issue 883 in the designer : when you change the Pressed Icon and save, it doesn’t save the change  

  * Implemented RFE 722: Create side menu on both sides of title  

  * Fixed the native Blackberry popup field to be white on black  

  * Implemented RFE 870: Ability to remove bounce in sidemenu  

  * Implemented RFE 874: Add pressed state icon for sidemenu  

  * Added drop shadow effect to the sidemenu  

  * Fixed issue 868: Hardware volume button needs repeated presses  

  * Fixed issue 867: Sidemenu closing transition problems  

  * Fixed issue 871: Title of Form is not centered  

  * Fixed issue 865 : XMLParser CDATA hiccup  

  * Improved Twitter API to work with new OAuth API 1.1  

  * Fixed vserv ads in a case of suspended apps  

  * Enhanced google map provider to allow configurable sizes  

  * Fix for side menu bar in a case of a back command + a Title Command where effectively the side menu isn’t showing  

  * Enhanced the table UIID behaviors  

  * Fixed array index out of bounds in the Base64 class for some edge cases  

  * Fixed an IO stream leak in ImageIO  

  * Added another option to the ImageIO that accepts file path  

  * Fixed email sending on iOS to support multiple recipients  

  * Fix for issue in in-app-purchase on iOS.  

  * Added ability to clear the preferences API  

  * Added ability to attach multiple message attachments  

  * Fixed the processing package which had multiple localization encoding issues all over the place and added ability to pass readers rather than input streams  

  * Fixed a potential race condition in the crash reporter  

  * Added better current day handling to the calendar class  

  * Implemented RFE 855: Simulate internet connection cases. Simulator can now simulate slow connection and disconnected internet  

  * Fixed issue 827 : disposing a web browser doesn’t “clear” it  

  * Improved the auto complete documentation and functionality to allow more dynamic data sets and asynchronous fetch  

  * Fixed the processing package and read to String to work better with different encoding types  

  * Made the scaled method for EncodedImage more intelligent, it now tries to generate an EncodedImage image when possible  

  * Added ability to create an encoded image from ARGB data, this is possible by using the ImageIO API  

  * Fixed a minor bug in the calendar class where it didn’t default to the selected day view  

  * Made an improvement to combo box so it can be used with popup dialogs otherwise they were too close to the combo  

  * Added support for media key events  

  * Exposed the current selected functionality of list which is useful for some renderer use cases  

  * Fixed border layout padding in absolute center mode required for iOS 7 title padding  

  * Added performance optimization for fixed images in renderers  

  * Fixed popup dialog borders which had some bugs with various size options  

  * Fix for running in JavaSE port without a skin  

  * Potential fix for multiple threads on the Network class in iOS  

  * Fix for exception in in-app-purchase code on iOS  

  * Fixed WebBrowser to remove the progress in HTMLComponent when there is an exception in the network request  

  * Fixed ActionBar to stay visible if the Form has commands  

  * Fixed issue 814 : SimpleDateFormat returns incorrect time on iPhone/MIDP/RIM  

  * Fixed Issue 787 : iOS networking error with cookies containing expires header  

  * Fix for issue 841 : local use class points to absolute path making team development harder  

  * Added some helper methods in Rectangle to ease working with it  

  * Fixed a null pointer exception in Log sending  

  * Added ability not to act on long press in list  

  * Fixed Dialog’s onShowCompleted() which wasn’t invoked  

  * Fixed arrows in popup dialogs for some cases  

  * Fixed exceptions in RSS reader  

  * Added ability to block redirect after OAuth  

  * Fixed issue 835 : SideMenu opens when command list is empty  

  * Implemented RFE’s 837 & 838 to scroll down the performance log and add a way to clear it  

  * Fixed several missing stream close() calls on Android & Resources.  

  * Fixed race condition while editing text on Android  

  * Fixed issue on Windows Phone with GUI builder applications which generally is the same issue we dealt with in iOS (xmlvm instanceof issue)  

  * Fixed a race condition exception in Tree  

  * Generified List, renderers etc.  

  * Fixed a rarely occurring null pointer exception in Container  

  * Fixed a spelling mistake in ConnectionRequest  

  * Fixed and NPE and StackOverflow in the AndroidImplementation code  

  * Updated zxing to be bundled on the server to resolve some J2ME/Blackberry issues  

  * Added a new form of lock() that works asynchronously thus allowing us to load an image in the background while showing a dummy and still enjoy the convenience of lock()  

  * Made lock() more robust and added the ability to check if an image lock is applied.  

  * Added lock() detection functionality to the performance monitor  

  * Disabled a block on EDT exception handling in the Android port which fixed error reporting for Android devices thru the cloud  

  * Fix for issue 755 : Streaming MP3 not working in ios  

  * Added ability to get the size of an element within storage  

  * Added static getter instance to the L10NManager for convenience  

  * Fixed font anti alias in derived ttf on the Android platform  

  * Fixed the barcode scanner download app dialog to work properly  

  * Fixed issue 819 : tel: links in webbrowser on android redirect to a protocol error page  

  * Fixed J2ME email to respect more properties if the native platform supports them  

  * Fixed the j2me VirtualKeyboard appearance  

  * Added feature that allows J2ME developers to show the status bar  

  * Implemented RFE 807: add title UIID to the designer  

  * Improvement for resource handling allowing for better control over DPI/multi image loading. This is important for some gaming use cases  

  * Fix for issue 802 : Geolocation API doesn’t work in Android Implementation of WebBrowser  

  * Added getContactById with params which allows much faster retrieval of contact details  

  * Simulator now sends the destroy app event  

  * Made multiple improvements for iOS contact handling  

  * Added ability for google maps provider to determine language  

  * Fixed RTL transition direction regression  

  * Made push registration synchronous and more predictable  

  * Fixed the implementation of CachedDataService  

  * Fixed issue 795 : Windows Phone MapComponent does not work  

  * Added the option of invoking createTrueTypeFont(name, null);  

  * Fixed issue 785 : MultiButton background problem  

  * Fix for issue 776 : iOS Multibutton problem. The multi button didn’t behave like regular buttons when dragging a pressed button  

  * Fix for issue 777 : TextArea setGrowByContent problem  

  * Fixed uncover transition drawing  

  * Added ability to get multiple headers with the same name in the connection request  

  * We now throw an exception for an improper use of rename  

  * Implemented RFE 769: Slow performance of opening the side menu on IOS due to usage of mutable image  

  * Fix for issue 773 : merged error input stream into stream for errors on Java SE and Android  

  * Added support to check whether on iOS a user has granted contact access permissions  

  * Fix some push related issues on Android  

  * Added an ability to differentiate the source of the back command on Android  

  * Added an ability to hide the loading progress status for the web browser component  

  * Fixed an issue where android sometimes sends double sizeChanged events  

  * Fixed r and t handling in the JSONParser  

  * Added support for the lastmodified property in the file system API  

  * Disabled shared cookies by default for iOS because it might break analytics  

  * Fix for drawString on iOS for cases where the string is longer than the maximum texture size (can happen for a case of a ticker)  

  * Improved support for video RSS’s and fixed an exception when parsing a content type that has an additional type attribute (happens for RSS streams)  

  * Implemented back button support for RTL allowing it to switch directions  

  * Added support for mirroring images to the reverse direction  

  * Added “uncover” transition that is the opposite of the cover transition allowing a form to “slide out” after it covered the screen.  

  * Fixed newline parsing in JSON  

  * Added support for maintaining aspect ratio in the image download service  

  * Fixed a race condition in the contacts model that kept showing loading for some entries  

  * Fixed an issue with http redirect header when an infinite progress is showing  

  * Fixed an encoding bug with Facebook login  

  * Fixed issue 760: Media.getDuration() returning 0 instead of -1 when instance reads mp3 metadata  

  * Fixed issue 761: Audio implementation for Media interface doesn’t keep the time when paused  

  * Fixed issue 759 : Media.isPlaying() crashed on Android if media buffered the first bytes  

  * Border layout now disables scrolling when applied to a component to prevent a common mistake of placing a border layout on a scrollable container  

  * Reduced EDT thread priority on Android to prevent issues on the Blackberry Z10  

  * Fixed issue with facebook URL’s which include the pipe (|) character in them which breaks on iOS (Apple is right here, this is against the RFC)  

  * Added major performance improvement to char width on iOS which really improves the performance of TextArea on iOS (critical for complex UI’s).  

  * Improved the performance of DrawStringTextureCache on iOS by removing redundant tests and reordering the tests so the faster code happens first (hence if it fails the cost is really low).  

  * Made XML/JSON parsers use StringBuilder to improve their performance noticeably, this removes synchronization code which is especially slow on iOS  

  * Made multiple improvements to working with RSS’s in foreign languages and encodings  

  * Removed synchronization from text area and made non-EDT code fail “gracefully”, this synchronization code is REALLY slow on iOS and since text area is used for multiline labels this was impacting performance  

  * Changed text area to optimize it for scrolling speed, it would constantly revalidate for no reason  

  * Implemented RFE 745: added more arguments to the postOnWall method of FaceBookAccess  

  * Made improvements to facebook allowing the listing of wall posts, fetching images synchronously as well as their URL’s  

  * Fixed a null pointer exception in facebook pages.  

  * Fixed a potential race condition for the in place text editing on Android  

  * Fixed a layout bug that caused elements to not appear in edge cases where scrollable Y returns false (tensile disabled) due to padding being too big.  

  * Added ability to get a picture synchronously from facebook  

  * Fixed potential null pointer exception in action listeners  

  * Fixed major bug in StringUtil’s tokenization feature  

  * Fixed issue 733: NetworkManager.getInstance().killAndWait(request) freezes Device and simulator  

  * Fixed issue 740 : NullPointerException in openImageGallery on simulator  

  * Added ability to fetch facebook images for ContainerList  

  * Added ability to login for some facebook functionality without OAuth  

  * Fixed issue 737: Cannot display browser in container  

  * Enabled updating the UIManager externally for two look and feels in one UI  

  * Added support for Android 4 and iOS 7 dialog buttons which use a thin line to separate plush components  

  * Added a done listener to the TextField as a fix for issue 596 : TextField.addActionListener() should not be called on Back button  

  * Fixed the contacts to return a sorted list  

  * Fixed a couple of issues with the webview loading feature on Android  

  * Fixed spinners and fixed Lists to move selection upon pointer press  

  * Fixed simulator media time to milliseconds  

  * Fixed issue 714 : iOS crash at startup  

  * Implemented RFE 726: IOS – contacts createContact and deleteContact  

  * Fixed exception in designer when saving with override resources in place  

  * Added ability to define smaller fonts in designer tool when choosing millimeters  

  * Fixed video capture on J2ME devices  

  * Fixed a performance issue that is visible in spinners with large data such as the DateTime spinner  

  * Fixed for issue 730 : Text fields with password constraint are capitalized by default  

  * Added ability for table layout to grow its last column without requiring constraints  

  * Fixed a bug in multi button when placing the icon where the emblem is supposed to be  

  * Added ability to define the prototype for generic spinner  

  * Added some more refined events for drag over events  

  * Added ability to get the component closest to a given point which is really useful for drag and drop  

  * Added ability to get drag notification events while dragging over an area  

  * Fixes for table layout to span better  

  * Fixed default side menu transparency  

  * Fixed a NullPointerException in grid layout and UIBuilder  

  * Made editing stop if a command was dispatched from the native menu on Android  

  * Fixed issue 727 : Problem with LeadComponent when the lead component is a TextField  

  * Added a Nokia Asha theme  

  * Fixed action “withText” to show on the ActionBar if there is room on Android devices  

  * Added ability to string util to tokenize by more than one character  

  * Added ability to define multiple columns to a generic spinner  

  * Fixed small exception in transitions  

  * Box layout now supports a version of X axis layout that makes more sense for most purposes which doesn’t grow beyond its preferred size vertically  

  * Fix for sliders that couldn’t be dragged all the way to the edge  

  * Fix for text area preferred size when hints are defined  

  * Minor improvement for dialog behavior  

  * Added ability to read the content of an HTTP response that has an error code e.g. 500 or 400 etc. which could be useful for some cases  

  * Fixed transition NPE in iOS for some cases.  

  * Fixed iOS builds on Eclipse which caused some issues due to ADT being somewhat incompatible with javac  

  * Added support for writing Codename One Maker plugins  

  * Fix for a NullPointerException in a case of an empty Tree  

  * Fixed the SideMenuBar to open on menu key and close on back key  

  * Fixed getDatabasePath() to work correctly on all platforms and return the proper path for a databased that wasn’t created  

  * Fixed multipart request which used the wrong content length  

  * Fixed the cursor color on Android during native editing  

  * Fixed potentially dropped push notifications on iOS  

  * Made generic spinner more robust before it is actually shown if its value was set via the model  

  * Fixed lead component functionality on 3 button mode which is a fix for Issue 721 : MultiButton problem with setThirdSoftButton(true)  

  * Fixed a bug in drag and drop that prevented the second drag of the same component from working  

  * Fixed scanning to call scanCancel when the action is canceled by user  

  * Added a Page object to the facebook api and simpler getter methods to the FaceBookAccess  

  * Fixed issue 715 text field max size doesn’t work  

  * Fix for app version to return a value in iOS  

  * Multiple fixes for test execution minor null pointer issue in a race condition  

  * Native utility class for Android allowing easier native code integration 

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — December 23, 2013 at 11:14 am ([permalink](/blog/release-notes-for-codename-one-201/#comment-21835))

> Anonymous says:
>
> Amazing! 
>
> Can’t wait, right on time!
>



### **Anonymous** — December 24, 2013 at 3:19 am ([permalink](/blog/release-notes-for-codename-one-201/#comment-21659))

> Anonymous says:
>
> this is literally the best xmas gift im getting this year 😀 awesome work guys love that list of fixes!
>



### **Anonymous** — December 26, 2013 at 1:32 pm ([permalink](/blog/release-notes-for-codename-one-201/#comment-21716))

> Anonymous says:
>
> Thanks so much guys… You rock
>



### **Anonymous** — December 31, 2013 at 9:45 am ([permalink](/blog/release-notes-for-codename-one-201/#comment-21828))

> Anonymous says:
>
> This is awesome! I’m proud of you guys…
>



### **Anonymous** — February 11, 2014 at 10:18 am ([permalink](/blog/release-notes-for-codename-one-201/#comment-21836))

> Anonymous says:
>
> where is it?
>



### **Anonymous** — February 11, 2014 at 1:44 pm ([permalink](/blog/release-notes-for-codename-one-201/#comment-21682))

> Anonymous says:
>
> Everywhere. If you use the plugin and keep it up to date you are always on the latest version.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
