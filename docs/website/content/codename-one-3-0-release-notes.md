---
title: "Codename One 3.0 Release Notes"
date: 2015-04-27
slug: "codename-one-3-0-release-notes"
---

# Codename One 3.0 Release Notes

1. Home
2. 3.0 Release Notes

### Summary

Version 3.0 is a major leap in Codename One's mission of mobile write once run anywhere, its highlights include a completely rewritten VM for iOS advanced API's and a new JavaScript target that officially makes Codename One the most ubiquitous WORA (Write Once Run Anywhere) platform in the market today.

### Highlights - Click For Details

New iOS VM

When Codename One debuted we used XMLVM as the underlying iOS virtual machine abstraction. XMLVM is an excellent product but its unmaintained and its goals are too different from the goals of Codename One. The new VM includes some features that would be remarkably hard to achieve with XMLVM such as: proper stack traces, faster builds (2x overall!), smaller code size, concurrent GC, deep OS binding (String - NSString relationship) etc. Read more about this work on [the announcement blog](/blog/new-codename-one-ios-vm-is-now-the-default.html).

JavaScript build target (technology preview)

Allows compiling Codename One applications to JavaScript client side webapps without server side code. Notice that this support includes threading support. Notice that this feature will be restricted to enterprise developers once it enters beta. The Java VM work is based on [TeaVM](http://teavm.org/) an OSS Java-JavaScript VM.

Read more about this work in [this blog post](/blog/javascript-port.html).

Charts API

The charts API supports drawing a wide range of charts such as bar, pie, line etc. It supports animating charts and is based on the aChartEngine Android API. Read more about this work on [Steve's blog post](/blog/codename-one-charts.html).

New Demos

- [Property Cross](/blog/propertycross-demo.html) - Browse properties for sale in the UK using a JSON webservice. Shows off JSON webservices, InfiniteScroll, URLImage etc.
- [Dr Sbaitso](/blog/dr-sbaitso.html) - Demonstrates an AI bubble chat interface, includes text to speech using native interface and more.
- [Photoshare](/blog/build-mobile-ios-apps-in-java-using-codename-one-on-youtube) - A simple social networking app that allows sharing photos
- [Charts](/blog/codename-one-charts) - Demonstrates all chart types
- [Geoviz](/blog/geo-visualization-library.html) - Performs statistic analysis over US population based on locale specific data
- [Flickr](/blog/cats-in-toolbars.html) - Demo for the Toolbar class showing special title area effects

New Themes

New beautiful and functional themes are now available through the plugins and the designer tool.

Toolbar API

More advanced and highly customizable API for handling the title area. It allows adding search to the title, animating its appearance/folding, placing commands arbitrarily and using the side menu. Read more about this work on [this blog post](/blog/cats-in-toolbars.html).

URLImage

Simplified image download to an icon or preview, that allows to implicitly apply special effects to said image (e.g. round corners, scaling etc.). Read more about this work on [this blog post](/blog/image-from-url-made-easy.html).

Built demos into the Eclipse/NetBeans Plugins

The main Codename One demos are now built-into the plugin so you can try them immediately without fixing classpaths and without downloading additional software.

New Android graphics pipeline

We rewrote the graphics pipeline on Android to work better in Android 4.x+ and use hardware acceleration where applicable. This new pipeline also includes support for the Shape & transform API's. Read more about this work on [this blog post](/blog/new-android-pipeline-fixes.html).

Regular expression and validation support

We added a new regular expression package and a new validation framework that simplifies error highlighting for input. As part of that work we also presented a rudimentary masked input UI. Read more about this work on [this blog post](/blog/validation-regex-masking.html).

High DPI Image Support

There are 3 new DPI levels in Codename One all of which are now supported by the designer: DENSITY\_560, DENSITY\_2HD & DENSITY\_4K.

Support for opening HTML files with hierarchies & Tar support

The builtin HTML support was improved by providing a way to open a hierarchy of files and not just a self contained HTML file. As part of this improvement we also added support to the tar file format. Read more about this work on [this blog post](/blog/html-hierarchy-release-plan-teavm.html).

New Morph & Flip Transitions

The [morph transition](/blog/mighty-morphing-components.html) was inspired by the Material design UI, converting a component on one form to a component on another form. The [flip transition](/blog/easy-demos-flip-more.html) provides an impressive 3d effect thats trivial to apply to any form or transition.

InteractionDialog

A new "modless" dialog that can "float" on top of the UI using the layered pane capability of the parent form Read more about this work on [this blog post](/blog/not-a-dialog-again.html).

Significantly enhanced developer guide

We redid the developer guide from the ground up converting it to asciidoc and integrating it into the website in a more fluent way. We increased its breadth by over 50%. Check it our [here](/manual/) or download the [pdf](/files/developer-guide.pdf).

iOS Beta Test Support (testflight)

Apple introduced a new way to test mobile applications for up to 1000 beta testers based on the testflight framework (but not to be confused with the old testflightapp.com product). We now support distributing apps via this process for pro users. Read more about this work on [this blog post](/blog/location-ios-beta-testing-better-input.html).

MiG layout support

MiG layout is one of the popular cross platform Java layout managers that works across FX, Swing, AWT and now on Codename One as well... Read more about this work on [this blog post](/blog/location-ios-beta-testing-better-input.html).

Facebook improvements such as publish support & version 2.0 API

Facebook made a lot of changes to its API such as requiring a special [publish permission](/blog/facebook-publish-android-localization.html) and migrating graph calls to version 2.0. Both are now integrated into Codename One.

Added webservice wizard to simplify client-server calls

The [webservice wizard](/how-do-i---access-remote-webservices-perform-operations-on-the-server.html) allows us to generate RPC calls to the server including servlet and client stubs. Read more about this work on [this blog post](/blog/webservice-wizard.html).

Support for badging the native OS icon on iOS

We now support updating and setting a badge on an app icons in iOS devices. Read more about this work on [this blog post](/blog/badges.html).

TCP socket support

We finally added support for TCP sockets into Codename One. Read more about this work on [this blog post](/blog/sockets-multiline-trees.html).

Advanced keyboard input in iOS that doesn't fold implicitly

This is an implementation of a feature that was [requested](https://code.google.com/p/codenameone/issues/detail?id=361) quite a while back. Historically, when moving from one text field to the next the VKB would fold and reopen. We now allow you to seamlessly move between input fields.

### Details

- Memory issue with downloading large files caused by native downloading overwhelming the gc.
- Fixed issue with picker type strings not remembering selected value. https://github.com/codenameone/CodenameOne/issues/1433
- Fixed double toString precision issue. https://github.com/codenameone/CodenameOne/issues/1432
- Fix for potential memory leak when downloading.
- Allowed separating the version number from the build number
- Made some improvements for issue 1272: SwipeBackSupport not working properly
- But this seems to be an inherent problem that's not easy to fix.
- Minor improvements to test util
- Fix for null pointer press argument in generated test case
- Merged pull request #1441 from sdwolf/feature/add\_cntest\_methods
- Added different assertion methods to AbstractTest and TestUtils
- Fix for issue #228: ComboBox setCommandList() has no effect.
- Fix for issue #253: An option to reset location of optipng
- Added new themes templates
- Fix for issue 1032: Switch of OnOffSwitch disappears after double-click while in "on" state.
- Added the assertion methods to AbstractTest and TestUtils
- Fix for issue 1031: on-off switch had ends with 3 points enabled
- Implemented RFE 1060 InfiniteScrollAdapter doesn't provide a removeComponent method
- Fixed picker to localize the strings
- Fixed changing SpanLabel style via code issue 1101
- Fix for RFE 1123: Add short function key (f1,f2,f3..) to screen capture on the simulator
- Fix for RFE: optimization in Display class issue 1127
- Updated themes templates in the designer
- Fix for #1420 - Relative file paths should throw an exception in the simulator
- Fix for #1436 - Android Date Picker "Cancel" button click disabled the component
- Fixed issue #1230 - Network monitor in simulator shows HTTP method as "GET" when method actually used was "HEAD"
- Fix for #1418 - Wrong ComboBox height in Android after screen keyboard hiding
- Wrong ComboBox height in Android after screen keyboard hiding #1418
- Better scrolling while dragging #1412
- Tar and html hierarchy support
- New package to support tar
- Support for opening HTML files with hierarchies
- Fix for issue #1403 - Tensile Drag - in X+Y direction
- Fix for #1295 - On DateTimeSpinner can localize only the month, not the day.
- Fix for #1417 - Scrolling container locks into visible region at either end of container, with 2 quick scroll/drag gestures
- Scrolling container locks into visible region at either end of
- container, with 2 quick scroll/drag gestures. #1417
- Added flat Orange theme
- Fixes for the VM mostly around issue 1426
- Fix for issue 1426 which was triggered by removing the 1D array when only the 2D array exists
- Fixed a compilation issue regression due to changes in the project hierarchy.
- Fixed issue with iOS not being able to delete cookies. Issue https://github.com/codenameone/CodenameOne/issues/1422
- Fix for fillRect transparency issue. https://github.com/codenameone/CodenameOne/issues/1419
- Added flat red theme
- High DPI Image Support
- Added flat blue theme
- Fix for issue #1421 - Android Date/ Time Picker "Cancel" button click disabled the component
- Attempted fix for issue 1423, small refactoring to classes so they will pass thru the Blackberry translation code
- Fix for issue 1416 memory leak after prior GC fix for very short lived objects
- Reduced overhead per component by removing an event dispatcher on focus that might not be necessary
- Major performance bottleneck improvement. Creating image arrays on every getPreferredSize() call!
- Improved removing of references and logging
- Improved removing of references to prevent GC race conditions
- Fixed issue with drawArc/fillArc in iOS.
- Fixed issues with Purchase.getProducts() hanging if there is a network error or if a product name comes back as null. Issue https://github.com/codenameone/CodenameOne/issues/1408
- Added docs for wasPurchased(). Issue https://github.com/codenameone/CodenameOne/issues/1409
- Fix for iPad splash screen problem in iOS8. Issue https://github.com/codenameone/CodenameOne/issues/1381
- Missed some UIManager code in the previous commit
- Initial commit of a simple validation framework and the old regex API that Steve ported from the Apache project
- Fixes for Issue 1414: Spanning columns of Table component does not work properly
- Fix for landscape mode corrupted view. Issue 1412
- Fixed in-app purchase problems with wasPurchased and getProducts. Issues 1409 and 1410.
- Fixed iOS video pause behaviour.
- Improved performance for gallery on desktops with very large files.
- Fix for nullpointer when drawing empty shapes.
- Fixed the preserveAspectRatio to take effect once selected
- Added property cross demo
- Added a check that the play store is installed when choosing a location implementation
- Fixed regression that caused framebuffer to not be resized when device is rotated.
- Removed call to delete framebuffer when layoutSubviews is called. It caused problems with screen redraw when the video component is in use, and it seems to be a performance killer in general.
- improved file copy to use nio for better performance
- Slightly improved tree expansion animation.
- Changed use of retainCN1 and releaseCN1 calls on strings to just use toNSString before the dispatch\_async calls. There are still some instances of releaseCN1/retainCN1 on more complex objects, e.g. arrays that will be more involved to refactor... will do that when I get more time.
- Replaced all instances of CN1\_THREAD\_STATE\_XXX with CN1\_THREAD\_GET\_STATE\_XXX inside dispatch\_sync and dispatch\_async methods.
- Fix for ommission in Long object
- Fixed possible race conditions with GC when Java object parameters are used inside dispatch\_async calls. Created retainCN1() and releaseCN1() functions that can be used to do reference counting on java objects that may live outside the purview of the GC.
- Fix for crash in iOS8.2 when purchasing product with IAP.
- Fix for Issue 1398: LocationManager stop send updates when lock screen
- Added ability to jump to the top of the scroll when tapping the status bar area.
- Made form back behave more sensibly for an empty title which will allow people to more easily track down the issue with the back in the toolbar area
- Added ability to show a label (or button) even if its value is blank which is necessary for the scroll to the top ability
- Fixed issue 1399: Dragging com.codename1.ui.list.ContainerList smaller than scrollable area fires action on Android
- Fixed issue 1401: Dragging a Button should not fire its ActionListeners when Display.dragStartPercentage between tap and release
- Added reset skins from menu and from cmd args - Issue 1396: Allow simulator skin to be reset to default
- Fixed a sizechanged event issue - which is caused due the the javafx panel mix
- Prevented a runtime exception from a client code to crash the backgroundThread
- Added support for image gallery to show videos, images, or both. Issue 1377. Note: This change adds the CoreMobileServices.framework as a dependency.
- Factored out some core features into the CN1GeoViz library to encourage reuse.
- Fix for Issue 1366: Dodgy tensile scrolling
- Fixed a regression caused by my recent commits. Threads weren't deleted correctly.
- fix for Issue 1365: Dodgy click recognition after drag operation
- Made the GC suspend itself when the app is minimized so it won't crash due to background GC activity, it will resume implicitly on 100k allocations even when in the background.
- Fixes for issue 1389: Timer.cancel() causes crash on new iOS vm
- Made thread destruction go thru the standard GC channels instead of going around it.
- Removed some code that isn't exactly clear from Timer.java
- Fixes for issue 1389: Timer.cancel() causes crash on new iOS vm
- Made thread destruction go thru the standard GC channels instead of going around it.
- fix for Issue 1392: openImageGallery does not return path on android 5 for images synced with G+
- Updated charts demo to use new setXXXTextFont() methods for text sizing so that the labels will be sized more appropriately for the display resolution.
- Added support for setting chart text sizes using CN1 fonts so that they will be sized appropriately for the device resolution.
- German Umlauts by string literal constructor don't work in the new VM issue 1292
- Added support for openGallery to for video selection
- Removed a redundant scheduleBackgroundTask which caused the command to not get called in some use cases
- Fix for issue 1386 on new iOS vm String.equalsIgnoreCase(null) throws NullPointerException
- Fix for class cast issue in tree
- Fix an issue where interfaces aren't allocated enough space. Which happened for the NavigatableMap interface where 29 instead of 36 elements were allocated. It now allocates 71 which is too much but that's probably better than too little since its still not that much.
- Implemented calcPreferredSize() on ChartComponent.
- Fixed issue with classes that are used in native code being stripped out if they are not also linked directly by Java source.
- Better fix for hiding keyboard when datepicker appears.
- Fixed sub-pixel positioning inconsistencies between device and simulator for drawing primitives. Related to Issue 1223.
- Fix for SpanLabel breaking lines incorrectly
- Keyboard now closes when datepicker is opened.
- Fixed issue with alpha not being respected by DrawLine in iOS.
- Fix for exceptions in the designer mentioned in issue 1370: Designer freezes on loading res file
- Fix for Orientation listener not called when menu is open
- Fixed keyboard height calculation for non-builtin iOS keyboards (e.g. swift key). https://code.google.com/p/codenameone/issues/detail?id=1368
- Added some customizability to the flip transition
- Toggled HTML flags on the package htmls
- Made callSeriallyAndWait throw an exception when invoked on the EDT since this is a common mistake people make.
- Fix for issue with the picker dialog
- Fixed the missing public class in BigDecimal
- Better fix for issue 1362: ConnectionRequest looping with new iOS VM
- Fix for issue 1362: ConnectionRequest looping with new iOS VM
- Added video capture to the camera demo
- Fix for Issue 1360: buggy emoji management in TextField an TextArea with iOS newVM=true
- Changed DrawRect to accommodate differences between device OpenGL and simulator's emulation of OpenGL. Fix for issue https://code.google.com/p/codenameone/issues/detail?id=1223
- Optimized String <-> NSString conversion
- Fixed issue 1348: Switching databases can crash new iOS VM
- Updated PieChart demo to demonstrate pinch zoom, panning, and seriesReleased() methods for creating more interactivity.
- Added improved interactivity support for charts. Also added arcTo() method in GeneralPath that approximates an arc using bezier paths.
- Reduced garbage in some image handling code, this relies on the fact that all image stuff will happen on the EDT so we might as well reuse the object and reduce GC.
- Made the weak references use a hashtable for thread safety
- Fixed play services location binding
- Fixed clipping issue on iOS ES2. This may require some more investigation as to why the clipping and drawing of rects is slightly different between ES1 and ES2 pipelines. This fixes issue https://code.google.com/p/codenameone/issues/detail?id=1223
- Added clock demo.
- Improved both quality and performance of drawing arcs and circles in charts. Used to use 36 line segments in path. Now just use 8 cubic beziers to approximate full circle. Also fixed issue with line segment showing up at degree zero.
- Fixed accidental typo from last commit.
- Fixed issue with datepicker not resizing when device rotated. Also removed extra white border at bottom of datepicker. Issues https://code.google.com/p/codenameone/issues/detail?id=1356 and https://code.google.com/p/codenameone/issues/detail?id=1356
- Fixed GC regressions due to recent changes. Deleting the latest generation deleted newly created objects which was a problem.
- Fixed issue with cdatepicker crash on iPad. https://code.google.com/p/codenameone/issues/detail?id=1353
- Fix for issue 1354: Designer stores Theme.res which cannot be loaded
- Fixed a bug in synchronized (monitor enter) that unlocked a critical section that wasn't held...
- Fix for Issue 1352: String with emoji chars make build fail with iOS newVM=true
- It seems String.replace doesn't work very well with emoji and this really failed badly with the recursion.
- Fixed bug in bytecode store calls which triggered GC issues due to incorrectly marked types
- Changed GC to increment mark value on mark now that we no longer use the reference counting approach. The previous way triggered GC issues in odd cases where the VM would premark an object and if it was the only way to reach a different object (e.g. in a Hashmap) we would have an old object "hidden" by a new object. In the past this wasn't a problem since we always cleaned the old generation (again policy that was necessary for the refcounting approach).
- Fixed VM crash for some special cases, not sure why this value would trigger that.
- Fix for issue 1345 in codenameone: URI objects return null for all getter calls, except getScheme()
- Added support for perspective transform in flip transition.
- Fixed major regression with new VM. The allocations of stringToUTF8 were leaking so this was fixed by allocating in the thread object. However, this means the same pointer is recycled over again causing bugs if the method is invoked twice. This broke everything from getResourceAsStream onwards...
- As part of the fix I also converted a lot of code that used stringToUTF8 to get to NSString to use toNSString directly. This is FAR more efficient in the new VM and might result in a HUGE speed boost for that VM since the NSString is cached in String.
- Added a FlipTransition (based on Steve's work from the card flip demo)
- Fixed issue with Object.wait(timeout) not waiting long enough.
- Fixed interaction dialog to not pass events down the hierarchy.
- Improved memory utilization for image rotation of the infinite progress implementation
- Changed the default of the text area to use initial caps which makes more sense
- Added a WeakHashMap implementation for easier caching
- Fixed potential thread issue with UTF-8 conversion
- Fixed a potential race condition with NSString deletion, the GC might collect java.lang.String while we are still pending on async native code that relies on the NSString that is bound to it. So we release the NSString in the native iOS thread thus making sure that all async operations already launched will complete successfully.
- Fix for Issue 1335: ImageIO didn't save objects in the new VM because the GC is less eager so it didn't collect the input stream immediately thus didn't invoke close for a stream. This is an old bug of a missing close() call that the new VM exposed.
- Removed some static usage to improve performance on the new VM
- Fixed a bug in the web browser component with error code tracking
- Removed some unused code and a fix for issue 1339: Sample program that crashes the new iOS VM, Part 2
- Added lastPurchaseData property
- Optimized String generation for the new VM
- Fix for issue with threads that might finish and still have allocations that aren't in the memory pool
- Added aggressive thread detection code that doesn't release the thread until after the sweep completes. This prevents one thread that keeps allocating non-stop from chocking the GC and destroying RAM while the GC is sweeping
- Removed implicit mark of objects added from a thread heap. These are young gen objects and very likely to be garbage by now so this doesn't make much sense
- Added an improved way of viewing memory data by viewing statistics on the recent sweep operation
- Fixed issue with share button on iOS 8.1, 64-bit, iPad
- Made object deletion more synchronous and removed a redundant stage that made sense only when the reference counter was around.
- Fixed a compilation issue with the old VM
- Fixed a couple of bugs in the previous optimization
- Fixed minor compiler issue with no return value for ES1 pipeline
- Fixed major problem in bytecode translator that optimized away finalizers thus creating huge memory leaks in native code that were impossible to detect
- Improved array memory allocation to work as one allocation instead of two.
- Fixed a bug where a dead thread was still marked as active so the GC would get stuck waiting for a dead thread to sleep.
- Added demos build to the plugin
- Fixed issue 1340: Display.getInstance().execute() causes iOS app to crash to the home screen
- This was triggered due to a hard to reproduce race condition where the String object was apparently GC'd before the asynchronous call was performed.
- Made minor improvements for String method calls to make it slightly faster
- Made System.gc() run two cycles of GC to also clean up the younger generation
- Fixed memory leak in iOS splash screens
- Added logging capabilities to track memory usage and fixed a few insignificant memory leaks.
- Old commit that I missed, some changes for the Good work and a couple of bug fixes with wrong thread calls
- Fixed crash when pressing share button on iPad/iOS8/64-bit. Added parameter to Display.share() to specify source rect, which is used by iPad to position the popover dialog. https://code.google.com/p/codenameone/issues/detail?id=1338
- Performance improvements for iOS and JavaSE graphics. Tried to make transforms that are \*almost\* the identity, automatically snap to the identity for the purposes of not having to do extra work later on. Moved matrix multiplication into native code on iOS. The chartEngine demo now runs correctly on JavaSE and iOS. Was already working on Android, but still need to test that it didn't have the same performance issues as iOS did.
- Fixed Math.floor implementation for negative numbers. https://code.google.com/p/codenameone/issues/detail?id=1334
- Fix for issue 1333: iOS app crashes when accessing a NULL field in the SQLITE database
- Optimized the previous fix to search in a far more efficient way
- Fixed a memory leak in the GC which always increased the pool of available objects until running out of memory rather than trying to find the best place
- Fixed a race condition if garbage collection is already running and is needed again urgently
- Fixed null pointer exception in Desktop apps for media playback
- Initial implementation of PropertyCross demo based on http://www.propertycross.com/
- Fix for issue 1267: Ability to specify how many pixel a text should shift in Label tickering
- Fix for issue 1328: NewVM App Crashes after sitting idle for 10 minutes
- Probably also fixes 1304 as well. Made the thread allocator block when a thread starts allocating too much and made it force a GC for such a case.
- Fixed issue 1332: Build server failed with iOS AppStore build
- Fields were inconsistently named with the $ notation or \_ notation which caused a warning on debug builds but failure on release builds.
- Implemented single arg constructor for IOException. https://code.google.com/p/codenameone/issues/detail?id=1300
- Fixed picker size issue on larger screens. Also fixed screen going black after clicking cancel or ok in datepicker. https://code.google.com/p/codenameone/issues/detail?id=1330
- Added google play location services
- Fixed Issue 1290: Strings appended after £ disappear on iPhone4 using new iOS VM
- Fixed the GC to be more efficient with static marking
- Reverted an optimization that places the strings into the constant pool. This creates a conflict with the String's static initializer since its too early in the VM initialization process...
- Removed dangling if statements in POP\_MANY\_AND\_PUSH\_XXX macros that were left from the latest changes.
- added InfiniteContainer API which simplifies the infinite adapter usage
- Added a missing check from the last commit
- Added finalizer to String thus allowing it to cleanup dangling NSString references.
- Added the builtin string into the interned string pool which might improve memory utilization. Added cleanup method to String that allows removing the NSString associated with a String and fix a memory leak.
- Fix for Issue 1317: Problems storing accented characters in database with new iOS VM which is really a problem in reading said characters.
- Improved GC behavior in low memory situations
- Updated the zbar support for 64 bit which we already did in the past and somehow got lost
- Optimized the common case of getBytes() with ASCII data. Also made the getBytes method respect the additional ISO encodings added earlier.
- Made the garbage collector more robust for cases where memory runs low
- Made System.gc() sleep a bit to give the GC time to wakeup
- Fix for Issue 1287, lack of String comparator causes TreeMap to fail
- Fix for Issue 1196: textfields on iOS only w/ constraint TextArea.ANY behave as if they are effectively set to TextArea.INITIAL\_CAPS\_SENTENCE by default.
- Fix for issue 1322 \\u notation broke in case of nesting a \\u. Switched the notation to differ from Java as ~~u
- Fixed a GC issue that triggered stalling
- Fixed issue with transforms not being applied during transitions on iOS https://code.google.com/p/codenameone/issues/detail?id=1315.
- Also added better for support for comparing transforms. Previously Transform.equals() didn't work properly. Android, JavaSE, and iOS all updated to support this new functionality.
- Fixed charts to work with J2ME builds
- Small performance improvement (garbage reduction) for layout managers
- toArray() now throws exception if it receives an array smaller than the size of the collection. This makes it easier to debug. https://code.google.com/p/codenameone/issues/detail?id=1314
- Fixed translation bug with class literals for primitive arrays
- Fixes to GC covering both crashes and performance improvements by removing redundant checks. Made GC cycles shorter to correspond to the removal of the reference counting
- Missing commit on an input stream fix and a class forName fix for inner class semantics
- Fixed most of the charts in the chart demo on the new VM
- Fixed parseDouble for large numbers and more decimal places on NewVM. https://code.google.com/p/codenameone/issues/detail?id=1283
- Fixed AutoCompleteTextField to paint correctly when ios keyboard opens/closes
- Fixed issue with getResourceAsStream() not handling files with multiple extensions on iOS. https://code.google.com/p/codenameone/issues/detail?id=1318
- Fixed issue with nullpointerexception when calling editTextImpl on newVM. This fix may be masking another problem with the VM as the null check shouldn't be necessary as far as I can tell. https://code.google.com/p/codenameone/issues/detail?id=1279
- Fixed issue with Double.toString() returning different result than simulator. https://code.google.com/p/codenameone/issues/detail?id=1251
- Fixed instances of Collections.toArray\[0\] that caused nullpointer exceptions in the new vm. Parts of charts demo are now working in new vm, but there are still some deadlocks to deal with in some charts.
- Fixed issue with collections of boxed numbers causing EXC\_BAD\_ACCESS on new VM. https://code.google.com/p/codenameone/issues/detail?id=1305
- Added support for ISO-8859-1/2 encodings
- Fixed memory corruption issues with print stack trace
- Fixed incorrect parsing of doubles for multiples of 10. https://code.google.com/p/codenameone/issues/detail?id=1313
- Fixed swap instruction. https://code.google.com/p/codenameone/issues/detail?id=1311
- Added DENSITY\_560
- Updated the FaceBookAccess to use version 2.0
- Removed reference to Object.finalize(). https://code.google.com/p/codenameone/issues/detail?id=1307
- Fixed landscape/portrait images for the iphone6+ device
- Issue 1302: Some launch image problems with the new IOS vm
- Fix for variation of Issue 1271: New iOS VM causes build failure with data mapper
- Arrays weren't registered correctly for some bytecodes and were optimized out.
- Fixed multiple issues
- Fixed issues with transforms on Android port.
- Fixed issue with lock-ups on with async editing on iOS
- Added Charts demo app.
- Disabled pan and zoom by default on the chartcomponent.
- Added copyright notices to new files in charts package.
- Added charts package.
- Updated CLDC11.jar to latest with fix for missing Class.getResourceAsStream()
- Fixed push to work again on android 5
- Fixed the text to speech to work on Android.
- Fixed a duplicate title issue
- Fixed arrayindex out of bounds exception when transforming point on JavaSE.
- Fixed the text to speech to work on Android.
- Fixed nullpointer exception in Font.getHeight(), getAscent(), and getDescent() that caused problems on build server.
- Workaround for recent build regression
- Fixed bug with rotate() that caused rotation around the wrong axis, and angle to be interpreted incorrectly .
- Added convenience 2D transformation functions. Performing 2D rotations using a 3D transform is not intuitive, so this is meant to make it easier to use the api in 2D contexts.
- Added support for basline vertical alignment with flow layout. Also implemented a getBaseline method in Label that can be used by flow layout. This allows you to layout multiple buttons and labels and have them vertically aligned on their baseline.
- Fixed font ascent and height to make more consistent.
- Changed baseline text to use a separate method (drawStringBaseline) instead of using a parameter.
- Added some javadocs for new baseline text support.
- Added support for drawing strings on baseline to simulator.
- Added support for drawing text on the baseline.
- Fix for iOS text input. async editing mode with form bottom padding.
- Fixed issue 1266 DateTimeSpinner not setting current date. https://code.google.com/p/codenameone/issues/detail?id=1266
- Forgot to commit debugging line for location manager in ios8.
- Fixed GC infinite loop issue
- Fixed synchronization on static methods
- Improved the performance of character encoding
- Text input improvements for iOS that allow using padding instead of increased scrolling
- Updated NetBeans plugin and library
- Fixed issue with missing AppIcon120x120 when submitting app to app store.
- Fixed a regression in the SideMenuBar
- Fix for Issue 1248: Calendar.getInstance().getTime().getTime() does not provide correct time in ms in newVM=true
- Workaround for null pointer exception in the side menu bar due to recent commits
- Made stopEditing public to allow a use case of automatically transitioning from one text input to the next
- Fixed double parsing on iOS
- Made input on iOS more robust
- Fixed issues with the new VM
- Fixed regression when building with newVM related to the datePicker localization patch. Also Included source fix for location manager access in iOS8. https://code.google.com/p/codenameone/issues/detail?id=1256 . This needs to be accompanied by inclusion of the NSLocationWhenInUseUsageDescription key in the Info.plist file, which will be automatically added by the build server (patch to come).
- Localized Cancel and OK buttons of datepicker. Also fixed issue with selected date not being returned if the user click OK on the current date.
- Changed DatePicker to be modal in iOS.
- Fixed issue with datepicker not returning null when the cancel button is selected.
- Removed ios7-specific datepicker code so that ios7 now just uses ios8 code. This should also fix ios7 specific issues with transparency in the date picker.
- Fix for android drawShape stroke ignoring stroke. https://code.google.com/p/codenameone/issues/detail?id=1257
- Fix for Issue 1255: Bug in JSONParser: always parse numbers as double even if long is expected
- Fixed an issue with poping stacks containing longs/doubles.
- Missing commits from the native VM
- Fixed possible regression introduced by recent fix to issue 1156 which could affect clipping of non-convex polygon shapes. Changed so that pixels sufficiently transparent are just discarded rather than pained... Haven't done performance testing but this may also give better performance.
- Fixed regression introduced by last fix for issue 1260. Needed to add notion of padding for texture masks to account for the difference in the path bounds and the texture bounds - as the texture bounds could exceed the path bounds if the stroke is non-null.
- Fix for issue 1260: Graphics.drawShape() has horizontal / vertical artefacts for wide stroke https://code.google.com/p/codenameone/issues/detail?id=1260
- Fixed issue with white border on iOS fillShape. https://code.google.com/p/codenameone/issues/detail?id=1156
- Added a faded Toolbar demo
- Adding the ability to place the Toolbar on top of the content
- Updated demo to scroll Toolbar off screen
- Added a ScrollListener API
- Added pointer events to Component
- Added hide/show functionality to the Toolbar
- Missing commit of the before swipe back fix
- Added error handling to setTitle
- Fixed some toolbar edge cases
- Fixed a bad format on facebook share - https://groups.google.com/forum/#!topic/codenameone-discussions/pRJ5loqJ6ug
- Changed default Toolbar menu transition to fade
- Added a new Demo (Flickr tags demo)- features the Toolbar
- Fixed a collision between the native action bar and the new Toolbar
- Some related fixes to the Toolbar
- Fixed an issue with Toolbar to init the menu bar
- Added Toolbar feature to the core API
- Workaround a bug where the async view came back black from a background run
- Fixed back command to not show on the Title area if the hideBackCommandBool is true
- Fixed a couple of the AndroidBrowserComponent methods to be called on the android ui thread
- Reverted lollipop support which caused issues
- Attempt at conditioning the previous lollipop support changes
- Minor reference counting and GC improvements
- Added ability to detect whether SMS will trigger a UI or be sent in the background and the ability to request the behavior when applicable.
- Added ability to set the UIID for a specific command in the side menu bar and possibly in other UI's
- Made table work better with the native input when moving from one cell to the next
- Added lollipop ActionBar support
- Fixed a NPE bug in getComponentAt
- Implemented missing API call.
- Fixed dates and improved GC performance.
- Added a help menu to the simulator
- Many fixes to the mark sweep GC to make it more aggressive and fixes to the static variable mark process to prevent threading issues from breaking global static variable marking.
- Delayed GCing of elements to prevent them from being collected due to a race condition with the GC thread.
- Fixed some alignment issues in the AutoCompleteTextField
- Fixed List pointer selection to take into account the padding in some use cases
- Some fixes to the SwipeableContainer
- Added onNewIntent to the main Activity
- Made a few bug fixes to the AutoCompleteTextField
- Added new SwipeableContainer class
- New morph transition effect
- Initial commit of the new iOS Java VM into the public repository
- Fixed an issue in AutoCompleteTextField where the List got the event too soon before it was displayed
- Fixed NetworkEvent to return responseCode if available
- Fix for potential threading issue in event dispatcher.
- Added IAP v3 support
- Fix for NPE with null dialog titles
- Added ability to tabs to work with Components and not just radio buttons, this allows a tab that is a composite component type.
- Added version support to the facebook api
- Fix for landscape keyboard issues in iOS 8
- Added flag to block tab images from being installed implicitly in iOS.
- Improved touch scrolling in ContainerList
- Improved playback and desktop platform overrides
- Fixed issues with push notification on iOS 8 due to API changes from Apple.
- Worked around the issues with the picker due to API changes in the new iOS 8
- Fixed an issue with actionPerformed not being invoked in the async mode when switching directly between text fields
- Worked around an issue with local video file playback
- Fix for issue 1210 GPS speed on J2ME all the time zero
- Added a constructor to Oauth2 that doesn't mandate scope param
- Fixed sizes of the AutoCompleteTextField popup
- Fix for an endless loop in the Contacts implementation
- Fix for Issue 1169: URI scheme and AppArg on Android
- Experimental autoplay and loop functionality for the media player class
- Ability for cloud emails to fail silently
- Fix for slider min value issues
- Added ability to disable tab animations
- Improved pickers behavior on setType
- Plugin fix for older IDE's
- Small fixes to the AutoCompleteTextField
- Fixed a painting issue in Slider
- Added a PullToRefresh uiid
- Fixes for compiling with XCode 6 and running on iPhone 6+. There were some assumptions in the code that retina == 2
- Fixed a desktop build issue in the eclipse plugin
- Added missing methods to the ContactsManager
- New iPhone 6 and 6+ skins for OTA install
- New get all contacts API that should make contact retrieval in bulk faster on Android
- Added cookie callbacks to the connection request allowing simpler cookie parsing
- Fixed regression in connection request from the last commit
- Added minor fixes for SimpleDateFormat
- Fixed spacing in the AutoCompleteTextField
- Fixed too spacing in the browser component
- Fixed a deadlock on iOS with the auto complete text field (fix in Component.java) this occurred since revalidate (triggered by auto complete) modified the setY value essentially making the new virtual keyboard switch to the scroll mode while the user is still typing
- Fixed designer exception in the localization editor
- Fixed very high density Android devices
- Fixed exception in peer component for some transitions
- Fixed the disabling of GPS on J2ME
- Fixed date formatting on JavaSE
- Fixed a couple of test recorder bugs
- Fixed user agent issues with iOS & some input bugs added canExecute and fixed some issues in video
- New get all contacts API that should make contact retrieval in bulk faster on Android
- Added cookie callbacks to the connection request allowing simpler cookie parsing
- Fixed regression in connection request from the last commit
- Added minor fixes for SimpleDateFormat
- Fixed spacing in the AutoCompleteTextField
- Fixed too much spacing in the browser component
- Fixed a deadlock on iOS with the auto complete text field (fix in Component.java) this occurred since revalidate (triggered by auto complete) modified the setY value essentially making the new virtual keyboard switch to the scroll mode while the user is still typing
- Fixed designer exception in the localization editor
- Fixed very high density Android devices
- Fixed exception in peer component for some transitions
- Fixed the disabling of GPS on J2ME
- Fixed date formatting on JavaSE
- Fixed a couple of test recorder bugs
- Fixed user agent issues with iOS & some input bugs added canExecute and fixed some issues in video
- Added some painting optimizations.
- Fixed paintBackground to draw the backgrounds of the parents when one of the Components contained is performing repaint
- Fixed video play/pause
- Patched a strange issue on some new Android devices (4.4) - https://code.google.com/p/codenameone/issues/detail?id=1173
- Fixed issue with split-second black screen between splash screen and first form. This was caused by the transform matrix not being set yet.
- Initial commit of the new PhotoShare demo which is part of the new Codename One crash workshop https://www.udemy.com/build-mobile-ios-apps-in-java-using-codename-one/
- Added initial work to support animations to the image viewer.
- Fixed connection request to support killing and resubmitting a connection better.
- Minor improvement to simple date format to allow millisecond formatting
- Fixed position events of the map component
- Fixed events in the AutoCompleteTextField and an exception with the margin
- Added support for higher device densities which aren't yet mapped in the tools
- Fixed an exception in URLImage
- Fix for picker and app home
- Fixed a performance issue
- Added an icon indication for the ui resources (Container, Form, Dialog)
- Added animation to the Tabs navigation
- Improved ImageViewer IMAGE\_FILL javadocs
- Fixed pan/zoom when ImageViewer is in IMAGE\_FILL
- Added fill & fit options to the ImageViewer
- Added ability to customize the tint color of the infinite progress dialog
- Fixes for title command removal
- Added ability to get the internal list of the DefaultListModel for various filtering algorithm usage.
- Fixed bug in datetime spinner caused by integer overflow
- Committing the todo demo
- Added support for RestoreCallbacks so that we can get feedback when restores of IAP are complete.
- Fix for regression in facebook impl
- Added canExecute API
- Fixed some regressions due to the mig layout
- Fixed issue with replacing the arrow in pull to refresh
- Improved Spinner localization
- Added simplified big decimal/integer support
- Renamed the t() method to getTransformMatrix() to make it more descriptive.
- Removed unused imports added while debugging.
- Implemented perspective transform for Android port.
- Updated the MiG packages and deprecated it
- Fixed text editing on iOS to stop in some cases
- Added ability to set the multipart boundary string
- Fixed recursion in sameWidth/Height
- Modernized the GroupLayout logic a bit
- Fixed text area alignment regression
- Added ability for back gesture for the UIBuilder
- Added a few methods to the MathUtil class
- Added proper perspective scaling when transforming coordinates. Only affects perspective transformations.
- Fixed issue with concatenate that caused it to fail for special transforms like scales and translations.
- Experimental MiG layout support
- Fixed NPE when concatenating a scale or translate transform with another transform. Fixed typo in setTransform() that overwrote scaleX with scaleZ.
- Added sameWidth/Height API to component allowing us to set the preferred size for components
- Added SwipeBackSupport allowing iOS7 like back swipe functionality as an option
- Added LazyValue interface that allows accepting values that would be generated on the fly
- Fix for image locking in labels which triggered issues with URLImage
- Made shouldSendPointerReleaseToOtherForm protected for the back swipe API
- Added eagerSwipeMode to tabs to make it easier to swiping
- Added the ability to lazily create a motion object for the transition which is used by the back swipe functionality
- Made some minor fixes to pull to refresh trying to track an elusive issue
- Added rendering prototype to picker
- Fixed exception in table editing
- fixed and.facebook\_permissions in the Android port
- New interaction dialog component
- Fixed exception in image viewer
- Improved EncodedImage behavior in scaling
- Fixed a bug when replacing the image in pull to refresh
- Fixed a couple of table refresh bugs
- Fix for Issue 1141: Alpha transparency ignored on polygon and lines with asyncPaint=true
- Initial work for the Android new graphics
- Fix for issue 1136: Support for nextAfter and ulp is missing from mailxzel23
- Fix for Issue 1108: MathUtil.atn2(x,y) function producing wrong results from mailxzel23
- Fix for ..cn1 lib directory issue
- Specified that rotate angles in Transform should be in radians.
- Fixed regression in iOS ES2 pipeline for transforms and for shapes. Also fixed ArrayOutOfBounds exeception in GeneralPath.curveTo(). Also added support for zoomLevels when setting transforms in the JavaSE port.
- Hints for push notification and scaling algorithm improvements
- Fixed long pointer pressed events for lead components
- Added API to set the Tab UIID
- Added a getter for border for future enhancement
- Fix for compilation issue in xcode 5.1
- A fix for some android painting issues in asyncPaint mode
- Fix for Issue 1107: Simulator in Designer Looks in Wrong Folder for Font in "src" Directory
- Fix for performance issue of sqlite
- Fix for issue 1129: little bug on source code for facebook Photo object
- Improved performance on iOS ES2 pipeline by changing each executable op to use its own custom shader program. Also changed so that the transform is only applied when it is changed. Still a lot of low-hanging fruit for improving performance.
- A fix for the fixAttachmentPath - https://groups.google.com/forum/?hl=en#!topic/codenameone-discussions/hj1m98R30EQ
- Patched a rare race conditions between the EDT and the Android thread
- Initial support for shapes and transforms. Still some issues during transitions and transforming components. Also not thoroughly tested yet.
- Changed return type of popClip to void. Returning the clip isn't all that helpful and it makes porting to different platforms just a bit more difficult.
- Changed license in header of Transform. Accidentally copied wrong one originally.
- A few fixes to the Facebook login
- Removed Shape import from image class that was accidentally added in last commit. Also added copyright notices to the FillPolygon executable ops.
- Moved Matrix class inside IOS implementation. Replaced with Transform class that is a thin wrapper around the implementation. This gives more control to the platform on how to implement transforms. Also added convex polygon drawing support in iOS. Currently this is only used for generating non-rectangular clip regions but will eventually be expanded to handle all drawing of shapes that qualify as convex polygons.
- Added api to open native navigation app
- A fix for CustomFont
- An attempt to fix fixAttachmentPath - https://groups.google.com/forum/?hl=en#!topic/codenameone-discussions/hj1m98R30EQ
- Some fixes to the graphics code, specifically a compilation error due to usage of unavailable classes and compilation issues with the new VM
- Fixes for some cases when killing an existing network connection
- Added an ability to block the auto complete from poping up unless the text is past a minimum length
- Fixed a listener leak in SideMenuBar that caused repeated event triggers
- Fixed some edge cases with the Android 4 blue glow
- Added Facebook publish capabilities to iOS
- Removed ununsed alpha mask methods in CodenameOneImplementation.
- Commented out a popClip() call as this api isn't finished yet.
- Re-added change that had been lost in ES2 update.
- Re-added some changes that had been accidentally reverted in the ES2 patch.
- Re-added setBrowserUserAgent() method to iOSNative. It was inadvertently deleted in ES2 update.
- Changed pushClip() and popClip() to be empty instead of abstract temporarily so as not to break builds of non-ios targets.
- New 2D drawing API and ES2 support for iOS. To enable ES2 you need to change NO\_USE\_ES2 in CN1ES2Compat.h to USE\_ES2.
- Removed classes that were just committed for history sake. We are currently going with the WebServiceProxyCall as the official direction.
- New webservice proxy API support with some additional classes that would be removed soon.
- Another fix for array arguments in ConnectionRequest on XMLVM.
- Fix for potential regression in Display
- Added ability to customize the pull to refresh image
- Fixed a bug in ConnectionRequest on XMLVM that didn't work properly with the String arrays.
- Added ability to handle errors properly in ImageDownloadService
- Added ability to commands so they won't dispose the dialogs
- Added facebook libs to the project
- Added the Facebook implementation class
- Added implementation for the GPS detection API
- Added publish permissions request api for the native FB integration
- Added API to detect if GPS is enabled
- Badging support native code and fix for conflict between native mapping and push. Also minor fix for the headphones detection support.
- Fix for a potential web browser component exception in the designer
- Fix for code scanner to return null on a device without a camera
- Another attempt to fix the spinner issue with different time zones
- Additional ImageIO API's for image scaling
- Fix for the persist option in android notifications
- Made location simulation persistent across executions
- Added badging support for iOS
- Fixed video positioning
- New badge number API for iOS
- Fix for remove TitleCommands
- Added the ability to override the SideMenuBar Menu button
- Support for proper stack traces under the new iOS VM
- Couple of bug fixes for the new VM
- Added flags to indicate whether the current platform is a desktop and whether the dial method is supported on this device.
- Another fix for vserv ads allowing for HTML ads to also pop correctly.
- The new JSONParser will now use LinkedHashMap instead of HashMap which will preserve the order of the elements from within the JSON
- Enabled duplicates to image download service, this is problematic conceptually but the alternative is that if we submit two duplicates only one will be received which isn't good...
- Fix for an exception in FilterProxyListModel
- Fixed timezone issues with the DateTimeSpinner
- Fixed an issue in XML import in the designer, it should default to UTF-8
- Fixed unicode CJK characters in the simulator and in the desktop apps for Windows machines.
- Fixed scrollwheel not to click the components we are scrolling over in the JavaSE port
- Support for proper stack traces under the new iOS VM
- adding the ability to custom the uiid of the TitleCommand
- a workaround for a EOF exception in some android versions
- adding support for left Title Commands in SideMenuBar
- Fix for "jump" when going back and forth in list views
- Fix for parser to generate long values as well as double values
- Fix for vserv ads when they are tapped an incorrect URL open logic is triggered. They can now only be tapped via the button.
- Fix for glass pane drawing on a Container which is important for table line drawing when a cell has focus
- Fix for multi-list placeholders with URLImage
- Fixes to support the new changes to the Codename One JVM. This allows passing a thread context on the stack saving expensive thread lookup and allowing very efficient concurrent GC.
- Another fix for the sort algorithm
- Fixed a small nokia back command bug
- Fixed exit app
- Added ability to disable cookies in the connection request
- Made the auto complete text field non-predictive by default which is pretty important for iOS
- Fixed a recursion issue in lead component which was triggered when disabling the lead
- Fixed a potential null pointer for side menu that can occur when a title command is added to a dialog
- Fixed a potential exception in Windows Phone with gridbag layout
- Exposed the fire data changed event due to request in the forum
- A few fixes for timezone issues with the spinners
- Fixed the style dialog when opened from the GUI builder
- Fixed some edge cases with loading XML UI's in the designer
- Fixed a socket exception in the Android implementation
- Fixed location manager to throw the right exception when the native location manager throws an exception
- Added a workaround that allows placing a comma character in the Android native numeric text field which is useful for German locale
- In MIDP an exception was thrown when a user denied access to the address book. We now return null.
- Fixed some device density bugs in the desktop builds
- Fixed Java SE sockets support
- Fixed video playing in RIM
- Added potential ARC support and prepared the code for the new VM
- Added a check before adding to the playing vector
- A fix for the media volume control
- Added location simulation
- Adding the option to share text and image, although some apps don't respect sharing both
- A small batch of fixes before VM change
- Fixed a regression in audio oncomplete
- Fix for issue 1097: Wrong separator in file://home path on PC Simulator
- Fixes for issue 1099: Reading from closed socket
- Fixes for issue 1098: readUTF bug
- Fixed setLeadComponent to accept null as a value thus disable the lead component mode
- Added private constructor to preferences to prevent people mistakenly initializing the class
- Fixed exception in server sockets
- Made a small improvement to auto complete text field
- Fixed regression in the processing package
- Added disposeOnComplete flag
- Implemented a workaround to the async paint issues
- Added a "Clean Storage" for the simulator
- Added "Remove All" to the NetworkMonitor
- Implemented getSelectedRect to ContainerList
- Fixed potential exception in PeerComponent
- Fixed NPE in iOS startup when dragging the finger on the splash screen
- Fix for showing the auto complete list for text field
- Made the SideMenuBars speed configurable
- Fixed NPE in generic list cell renderer
- Added support for setting the user agent of the browser explicitly
- Fixed a simulator bug in the socket implementation
- Added multi images to the share icon
- Fixed push types
- Fixed android peer image
- Fixed fullscreen native video on iOS
- Improved button release functionality which made buttons a bit less sensitive on iOS touch devices
- Improvement to canceling connection requests in progress, this happened for cases where a connection was retried then canceled. It got stuck in an infinite loop.
- Fixed map event functionality
- Fixed image scaling functionality on Windows Phone which doesn't support PNG image IO
- Made some functionality/performance improvements to the side menu bar as part of chasing the responsiveness issue mentioned
- Added some improvements to the peer component behavior on Android to enable Google maps functionality
- Fixed fullscreen native video on iOS
- Fixed mailto: links within the Android browser component to do what's expected of them
- Fixed the processing package to use Map/List instead of Vector/Hashtable so it will work with the new JSON parsing code
- Added cloud file storage API's to purge old files
- Fixed a layout issue with ImageDownloadService
- Added ability to open a popup dialog based on screen location which is important for native component integration
- Added a more refined version of encoded image that allows creating encoded images more efficiently
- Added ability for native code to receive Android lifecycle event callbacks
- Fixed a bug in Display execute in Android where mailto:... didn't work as it previously did
- Caught a potential exception in Android surface view
- Added ability for JavaSE apps to store their state into a different folder from .cn1
- Fixed some file access bugs in the iOS port and made a small peer component improvement
- Allowed facebook on older iOS devices to fallback to the lightweight login method
- Added map listener API that provides updates on map changes e.g. zoom, pan etc.
- Fixed potential infinite loop in component drag events
- Fixed J2ME null pointer exception regression with Encoded images
- Improved peer component creation logic allowing native layers to return null for a peer component
- Another client properties persistence bug fix
- Fixed exception with client properties
- Added a rendering prototype to the DateSpinner
- An improvement for popup display logic
- Fixed sidemenu button click to show the animation
- Made drag sensitivity play nicely with buttons so the behavior of drag events is more fine tuned to specific component requirements.
- Fixed issue 1072: NullPointerException: Attempt to invoke virtual method 'boolean com.codename1.ui.Component.isVisible()'
- Added getPendingPush
- Another set of fixes for URLImage coupled with a new feature to scale to fill
- Added new URLImage support to replace the ImageDownloadService class, this works both standalone and as part of the GenericListCellRenderer
- Fixes for file upload in the cloud storage API
- Added a callback option for the new download file API
- Fixed an autocomplete bug that onu shows the text on iOS after the keypad was closed
- Added a flag to disable paintlock manually, this is used in the tabs component and contributes to some performance degradation.
- Added ability to explicitly scale to EncodedImage
- Another fix for JSONParser
- Fixed tensile highlight regression
- Fixed exception in new JSON parser logic
- Updated javadoc with latest features/corrections
- Placing renderers first in ui xml
- Fixed bottom glow to paint in the right position
- Added Java 5 support for the JSONParser class
- Fixed auto complete text field to not popup when the list is empty for some use cases
- Fixed a text input bug on iOS for some edge cases
- Fixed a null pointer exception issue on Tree
- Added a warning to the UI builder regarding inheritance limitations
- Minor improvement for contact API to simplify access to email address
- Implemented support for client properties in XML syntax and runtime of designer
- Fixed scrolling with scroll wheel in a page with text fields or while editing
- Fixed the Java SE port to return mac or win for the platforms when running on the desktop
- Fixing the connection timeout
- Made edge highlighting in Android behave closer to the way it does in the native Android 4 OS
- Fixed a skipping in the side menu slidein animation on iOS
- Fixed a potential bug in cloud storage file upload
- Added a feature to generic list cell renderer allowing a component to have a different UIID based on model data
- Allowed developers to customize back command behavior in the UI builder
- Fixed a class cast exception in the JavaSEPort
- Fixed the menu bar for iOS 7
- Fixed switch look and feel on iOS 7
- Added simplified file download support API's
- Additional API's for L10NManager localization and parsing, these aren't incorportated in the ports yet
- Fixed a visibility issue in the popup of the auto complete text field
- Fixed a regression caused by a change that invoked setScrollX/Y at a crucial point, this effectively broke the Spinner component for some edge cases
- Added a shouldLocalize flag to label allowing users to block localization
- Added maxElementHeight property to list to constrain its preferred size
- Added constants to side menu bar for convenience, hid an empty back command from the side menu bar
- Made a fix for date time spinner to take the timezone offset into consideration, otherwise the day was offset in countries such as Canada where the distance from the GMT baseline is too great
- Fixed a bug with streams to files in the JavaSE port
- Made the location manager in the simulator fire its events on the EDT only
- Fixed device density on the ipad retina which seems to be incorrect
- Committing local changes to avoid more conflicts
- changed the view to the surfaceview by default
- fixed flickering text input on android 4.4
- Fix for issue 1062: audio capture not working on iOS device
- Fix for issue with image capture that caused a crash due to a memory warning that arrived while the device was rendering something else.
- Minor regression with facebook missing a comment
- Added a read timeout
- Fixed the native re-layout to listen to sizeChanged events
- Added a sizeChangedListener
- Fix for issue 1059: ArrayIndexOutOfBounds exception on Picker for date type from Nirmal
- Fix for issue 1060: Missing setUIID() in TextField constructor
- Improved bar code scanning in j2me
- Workaround for potential crash in iPhone camera
- Fixes for iOS 7 status bar height
- Fix for ads on some devices
- Improvements to the picker functionality
- Better facebook integration
- Fixed a regression in push
- new eclipse version
- Small performance improvement to clipping on iOS by using a more efficient clip rect
- Added preserve aspect ratio and ability to describe percentage scale of multi-image
- Fixed iOS filesystem roots.
- Generified combo box which was missing after we generified the list
- Worked around an issue with the side menu bar clashing with the glass pane of the table in iOS
- Made sidemenu bar work correctly on iOS
- Fixed a long standing bug in rectangle which affected iOS clipping performance
- fixing endsWith3Points for TextArea
- init scrolling positions on removeAll
- Added picker to the GUI builder
- Added ability to define the swipe threshold of the ImageViewer and fixed an incorrect getter
- Made dragging more responsive for specific components such as list and possibly sidemenu
- Added better logging for push notification
- Made the auto complete text field hide the popup in case of an empty list
- Fixed Android execute regression
- Added mousewheel support for the JavaSE port and desktop builds
- Fixed switch to calc size properly on non ios mode
- Fixed orientationListener to be called only on orientation change.
- Fix for potential web browser crash on Android
- Fixed IDEA bug
- Fixed the -regen flag
- Added a new block overdraw mode which is currently off by default but might improve android performance for some cases with the new pipeline, this includes changes to all the layout managers to detect when an element is obscuring another element
- Made span label use container style to allow the label to behave more like a "label"
- Made infinite progress animations smoother and less EDT hostile
- Removed deletion of buffer in MapComponent which was causing an issue with the new Android pipeline where the buffer was deleted but was still queued for paint
- Made isCellRenderer public for consistency with the setCellRenderer API and for the performance monitor improvements
- Minor performance improvements for draw string, we now ignore drawString " " which happens a lot
- Fixed bug in command icon gap client property
- Added support for configuring the size of the side menu
- Fixed border layout center alignment absolute center mode to properly align, this is really a fix for issue 1042: Title Area Alignment on iOS 7
- Added case sensitive mode to the XMLParser class for consistency with XML
- Fixed a bug in exporting blank localizations
- Improved spacing for ads on Android devices to grab the exact size required in pixels.
- Added some improvements to Android's getProperty, now various properties instead of returning null will return the default value. If an intent sent an argument you should be able to retrieve it via getProperty.
- Fixed a simulator/SE port bug with text input that was very obvious in the iOS\_7 5 skin
- Made share behavior on iOS work for OS's prior to 6 by falling to lightweight share functionality.
- Added OS 7 skins to build script
- Made updateValue protected as a fix for issue 1057: Date Picker Format
- Added support for passing arguments via intent
- Fixed action bar icons dpi issue
- Fixed InfiniteProgress rotate speed to be more native like
- Fix for executing file URL's.
- Patched a very strange awt bug on windows in the file chooser dialog
- Slightly improved peer components and text fields in the new pipeline
- Added an option to create an InfiniteScrollAdapter that doesn't calls the fetch upon creation plus a few fixes to the impl.
- Updated popup dialog padding
- Updated popup dialog padding
- Restored the patch to workaround android paint issues.
- New component rendering debugging tool in the JavaSE port
- Fixed async rendering mode in Android to be much faster and some rendering glitches
- Patched duplicate background painting in form
- Fix for ad spacing in iOS ads
- Fixed exception in browser component in the SE port
- OnOffSwitch now supports iOS 7 rendering mode
- Filtering now supports the starts with both in the proxy model and the auto complete
- Fix for issue of dependencies between UI elements in the case of embedded container.
- Fixed some animations in new Android pipeline and removed layer to slightly speedup rendering
- Async paint pipeline fixes, still has some issues
- Exposed should write UTF from connection request
- Fixed issue with font loading in the designer
- Fixed a classcastexception
- Fix for issue 1050: drawLinearGradient broken in iOS
- Fix for issue 1049: Plugin 1.0.67: iOS7 Status bar is hidden by full screen using SocialBoo
- Made all URL's work with space characters on all platforms
- Added ability to customize side menu swipe behavior
- Allowed localizing date/time spinner
- Added ios 6 skins
- Fixed resources being GC'd even when set to remain in ram
- Added prefetch media API
- Made span button default to more columns
- Changed picker start year to something more sensible
- Patched the blackberry mkdir method
- Capability to prefetch audio
- Added endsWith3Points to the TextArea
- Fixes for several issues were missing
- Scale should now act correctly for maps
- Fix for issue 1043 from Steve: iOS nextPowerOf2 needlessly goes one power too far
- Fix for image sizing issue with ARGB images from Steve
- Added flag to hide the text field toolbar for DONE/NEXT functionality
- Many fixes for text field input in the vkb always open mode
- Fixed the UIPopover to work correctly without artifacts on iOS 7 and fixed the picker on ipad to actually work
- Added the socket implementation code
- Added the media prepare API
- Regenerating the state machine code on regen.
- Regenerating the state machine code on regen.
- Fix for bug in component group in new XML UI mode
- Another fix for issue 805: keyReleased not invoked
- Added support for XML ui elements as an optional experimental feature and an ability to "compile" the XML changes to a resource file without launching the UI of the designer
- Minor fixes for text input in iOS with the async flag
- Fixed the JSON parser to support decimals in scientific notation based on request from the forums
- Added support for expand/collapse path in a tree
- Fixed event handling in multiline tree node mode with lead component.
- Fixed a performance issue - the system resources was messing up the local res caching.
- Fixes for Issue 1024: Double-tap Zoom in MapComponent
- Fix for Issue 1023: MapComponent pinch zoom to wrong position
- Fix for issue 1021: com.codename1.l10n.SimpleDateFormat.parse unexpectedly throws java.lang.StringIndexOutOfBoundsException
- Implementation of RFE 1019: MapComponent pointerDragged new tiles not loaded
- Fix for issue 1015: Generated "Back" command disappears from menu on Android when returning from next form
- Socket support for iOS, Android & Java SE
- Fixed potential exception in the image viewer when working in the designer
- Fixed on-off switch to not resize the button dynamically as it moves back and forth on Android
- Fixed the auto complete text field to work with the keyboard too
- Improved the always on keyboard mode in iOS to handle various edge cases correctly
- Added support for multine tree elements and arbitrary component types as entries within the tree
- Fixed pull to refresh y position
- Updated skins to fix issue 1014: Multi Image issue on Simulator and Real Devices
- Fix for Issue 1014: Multi Image issue on Simulator and Real Devices
- Fix for issue 1012: Effect of padding on horizontal position of a component
- Support for executing file URL's on iOS
- Fix for RFE 1004: Support copy and paste URLs from the Network Monitor
- Fix for Issue 805: keyReleased not invoked
- Fix for issue 982: SimpleDateFormat crashes app on Windows Phone 8
- Moved all date formatting code into Codename One package space to remove platform dependencies.
- Support for string based picker component
- Fix for Issue 361 on iOS: Ability to scroll Form/Components while virtual keyboard is showing
- New support for asynchronous mode for the editing component required for that fix. This allows edit string to effectively return immediately
- Ability to determine the height of the VKB from the implementation to provide more scroll space and to restrict the area of scroll rect to visible
- Ability to determine number of rows the text field can grow to
- Fix for RFE 1009: timeout on static ImageDownloadService methods
- Added a set default timeout static method
- Added an improvement to the behavior of native lookup to enable native interfaces in JavaSE
- Fix for issue 1010: Multiple cookie headers with different casing is not recognized on Android and JavaSE
- Fix for issue 1008: NoSuchElementException when sharing an image via ShareButton in iOS
- Made a fix for truetype fonts to fallback to classpath so the JavaSE packaged applications will work properly with TTF's
- Added headphone processing callbacks
- Redid the logic for next text field in iOS behavior to match the logic used in Android.
- added support for multiple right commands (Action Bar)
