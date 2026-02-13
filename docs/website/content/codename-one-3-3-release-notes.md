---
title: "Codename One 3.3 Release Notes"
date: 2016-01-27
slug: "codename-one-3-3-release-notes"
---

# Codename One 3.3 Release Notes

1. [Home](/)
2. 3.3 Release Notes

### Summary

Version 3.3 starts a trend of refinement that we hope to stretch into version 3.4. We modernized a lot of details and continued trends from 3.2 such as performance, background process support, terse syntax etc.

### Highlights - Click For Details

Faster rendering of backgrounds & Labels

Up until now the logic for rendering the background of the component resided entirely within Component.java & DefaultLookAndFeel.java.  
This allowed for a simple rendering logic that is implemented in a single place, however it didn't allow us to deeply optimize some operations in a platform specific way. We moved the rendering into CodenameOneImplementation.java which allowed us to override the logic both on Android & iOS to deliver native grade performance on any device.  
On iOS this has been a strait forward change where most of the low level logic is now written using very efficient C code. On Android the pipeline complexity is far greater, but thanks to this approach we were able to reuse many system resources and reduce JANK significantly in applications. This work is still ongoing but the bit effort has been implemented.  
This is probably the biggest piece of multiple changes that went into this release including fast tiling support, better font/string texture caching, Container API optimizations etc.  
Read more about this work in [this blog post](/blog/code-freeze-for-3.3-performance.html).

Animation Manager, Title Animations & Style Animations

We rewrote the animation logic in Codename One for 3.3. This broke some backwards compatibility but this was for a good cause as we now have a central class that manages all animation events going forward. This means that you should no longer get odd exceptions when using many animations in sequence.  
As part of this enhancement we also added new animation types such as title scroll animation and the ability to animate a style object UIID.  
Read more about this work in [this blog post](/blog/new-animation-manager.html).

"Remastered" Documentation (ongoing)

We are redoing a lot of the Codename One documentation from scratch with Codename One 3.3. This is ongoing and we barely just started but the new documentation is far more readable, detailed and clear. Moving forward we are confident that our developer guide, JavaDocs & videos will be in a league of their own!  
Read more about this work in [this blog post](/blog/wiki-parparvm-performance-actionevent-type.html).

Material Design Icons

FontImage has been around for a while but up until now we didn't use it to its full extent. It required getting an icon font, configuring it and we just skipped it for some cases.  
With 3.3 we integrated the material design icon font which makes adding flat icons to your application remarkably simple!  
Read more about this work in [this blog post](/blog/material-icons-background-music-geofencing-gradle.html).

Media Playback & Geo Fencing in the Background

We continued the background process trend with 3.3 as we enabled both geofencing (to track device location in the background) and media playback in the background.  
Read more about this work in [this blog post](/blog/material-icons-background-music-geofencing-gradle.html).

PhoneGap/Cordova Compatibility

Codename One always supported embedding HTML & JavaScript but it didn't support embedding things such as the Cordova/PhoneGap API's.  
With the new open source project we announced we can now convert many Cordova/PhoneGap apps to Codename One apps and deliver quite a few compelling advantages.  
Read more about this work in [this blog post](/blog/phonegap-cordova-compatibility-for-codename-one.html).

New hello world project & icon

A major focus of this release was making Codename One useful and attractive right out of the box. As part of that work we replaced the default icon, redid the hello world app to a more impressive (yet simple) demo and updated the default fonts.  
Read more about this work in [this blog post](/blog/good-looking-by-default-native-fonts-simulator-detection-more.html).

New Simplified Native Fonts

Fonts were a difficult subject prior to 3.3. You could either use the portable but ugly system fonts, or go with the gorgeous but flaky TTF fonts. Both don't make sense when Android ships with the great Roboto font and iOS ships with the gorgeous Helvetica Neue font.  
We now have support for a new font notation with the native: prefix. This notation (supported by the Designer), allows us to leverage the existing native fonts of the device which look both native and gorgeous.  
Read more about this work in [this blog post](/blog/good-looking-by-default-native-fonts-simulator-detection-more.html).

Terse syntax enhancements

In 3.2 we started moving towards terse syntax for container hierarchy construction and with 3.3 we brought that to fruition. We added methods such as an add method that accepts an image. We added factory encloseIn methods to almost all of the layout managers, we added form constructors that accept layout managers and much more!  
Read more about this work in [this blog post](/blog/properties-continued-terseness.html).

ParparVM Performance & Open Source

Our iOS VM has been open source from the start but we didn't encourage its usage outside of Codename One. This changed with 3.3 and we are actively promoting the ParparVM OpenSource project.  
Unrelated to that we made a lot of performance improvements to the core VM translation logic, it should be very competitive in terms of generated code to pretty much everything else on the market. Especially with API calls as our entire API is hand-coded and highly optimized.  
Read more about this work in [this blog post](/blog/parparvm-spreads-its-wings.html).

Properties file format support

We didn't have support for Java venerable Properties file format before 3.3. Surprisingly developers didn't really complain about that ommission as we support XML, CSV & JSON. Now we can add Properties to that list!  
Read more about this work in [this blog post](/blog/properties-continued-terseness.html).

Ending Support for the codescan API

3.3 will be the last release that includes an implementation of the codescan API for QR code/barcode reading. We will remove this API completely and we ask users to migrate their code to use the new [codescan cn1lib](https://github.com/codenameone/cn1-codescan/). When we initially introduced this API we didn't have support for cn1libs and integrated this into the core directly.  
Read more about this work in [this blog post](/blog/video-new-defaults-barcode-qrcode-changes.html).

### Details

- Fixed bug in Javascript bridge that seemed to affect Android only. This bug would sometimes cause get() to return null for a Javascript expression that should be non-null.

- Fix for #1645 when a style was changed on the EDT the currently queued async paint got changed as well

- Updated the usage of the @inheritDoc javadoc tag which was misused thru the entire codebase

- Fix for NPE when initializing the UI too soon Fix for NPE when initializing the UI too soon

- Added JavaDocs and code samples to Dialog Added JavaDocs and code samples to Dialog

- Minimised the overhead of drawString in Android in a similar way to the iOS port by drawing on a bitmap within the EDT instead of on the Android thread.

- Fixed infinite progress javadocs

- Fixed bug with OnOff switch that printed the 'on-off' state of the switch as text even if the flag was set to disable this.

- Added ability to programmatically set the on, off, and mask images for the OnOff switch to allow for greater 'per-instance' customization of an off switch.

- Fixed transform issue on iOS mutable images reported at http://stackoverflow.com/q/34770700/2935174

- Added easy ability to toggle on/off the new paint modes for Android

- Initial work on new graphics optimization that caches paint data

- Merge pull request #1643 from @McSym28 Socket.java. isSupported() should be static

- Merge pull request #1637 from @ddyer0 - remember the requested size for derived fonts

- Fixed issue with text fields intermittently showing the text from the previous text field when clicking 'Next' in async edit mode on iOS.

- Merge pull request #1636 from @ddyer0 - Fix an additional error loop in the VM socket implementation

- Minor performance improvement for async paint

- Workaround for usage of gradients in designer where developers set the same color on a linear gradient to get a solid color

- Fix for URLImage to throw exceptions sooner: http://stackoverflow.com/questions/34713004/an-exception-occurred-during-transition-paint

- Added support for Picker preferredPopupWidth and preferredPopupHeight that can be used on platforms like iPad to decide how big the picker popup should be. Currently only implemented for iPad, and not relevant to iPhone.

- Merge pull request #1633 from @ddyer0 - fix a fencepost error in flowlayout

- Merge pull request #1633 from @ddyer0 - when readFromStream or writeToStream throw an IOError, mark the stream as no longer connected.

- Merge pull request #1633 from @ddyer0 - Added event type information to ActionEvent

- Removed vestigal keyboard support sections as they can't effectively be used anyways. Keyboard changes ended up being too far reaching for this simple flag to be sufficient for reverting to old handling. The new handling appears to be working for all use-cases anyways.

- Added 2D transform support for mutable images on iOS. This effectively fixes issues with being able to share charts that are generated as images #1629

- Added new UITimer constructor for further simplicity

- Replaced array.length calls inside for loops with local 'length' variables

- Fixed issue with transforms on mutable images in JavaSE port that would cause Graphics.getTransform() to always return the identity transform

- Added support for drawing shapes on mutable images in iOS #1629

- Fixed async editing support on Android when TextureView is used.

- Added addComponentToSideMenu() to Toolbar this will allow devs to customize the sidePanel even for permanent side menu

- Added ability to center the title in the Toolbar Added ability to center the title in the Toolbar

- Fixed cancelLocalNotification to only delete the notification whose deletion was requested

- Fixed regression in left aligned labels http://stackoverflow.com/questions/34565387/button-settextpositionlabel-left-render-wrong/34581225

- Fixed remove command from title

- Added ScaleImageLabel to simplify the process of adding a label that will scale correctly

- Added AssignableExpression to ParparVM implementation. This will allow the translator to work with instructions in a more generic way when determining if an instruction can be reduced to a single expression that can be assigned to a variable or passed as a parameter

- Changed ParparVM primitive local variables to use C locals instead of storing them in the stack. This should bring performance improvements on two fronts: 1. STORE operations now map to a single assignment instead of two, since we don't have to assign the variable type. 2. LLVM should be able to perform better optimization surrounding C locals.

- Fix for launching the android sms activity

- Pull request from @ddyer0 Fix for two problems with Sockets; data corruption and infinite error loop when input socket is closed.

- Fix for #1623 tickering should cycle

- Refactored ParparVM's custom Invoke and Jump optimizations into separate Instruction classes so that there is no confusion on how the Jump and Invoke instructions interact with the stack.

- Added getStackOutputTypes() and getStackInputTypes() methods to Instruction in ParparVM so that optimizers can inspect instructions to see how they affect the stack. This will eventually allow for better reduction of instructions during the optimization phases.

- Improved CustomInvoke optimizations in ParparVM to cover many more cases that were missed before. This should further reduce redundant stack operations surrounding qualifying method invocations.

- Added small optimization for binary integer arithmetic ops in ParparVM to directly assign result to local variable instead of first pushing it to the stack, then popping it to a local

- Improved the performance monitor to show UIID's

- Fix for some edge cases of component animation: https://groups.google.com/d/msgid/codenameone-discussions/fdd7ce70-d8ac-418b-96e0-0cd9ca529cb1%40googlegroups.com

- Fixed issue with android async keyboard still showing on form change

- Added the missing overflow icon

- Fixed the decay motion to stop if last 4 returned animation values are the same

- Fixed potential exception in animation layout transition that might collide with a remove operation

- Deprecated the com.codename1.codescan package. Going forward developers are encouraged to use the cn1-codescan cn1lib instead. https://github.com/codenameone/cn1-codescan/

- New ability to force popup dialog in a specific direction

- Flipped the default for "ends with 3 points" to be false as this slows down performance slightly

- Fixed special case that caused scroll to 0,0 when first field of form requests focus whether or not it is already visible. This improves usability of the MaterialLogin demo.

- Fixed problem with rendering FontImage, IndexedImage & RGBImage

- Caching derived font instances which can significantly boost performance by allowing the native side of the fence to cache additional data on the font object

- Fixed issue with async editing changes causing text field to close and reopen sometimes when switching to a different field

- Fixed masked icons to render correctly in the new pipeline

- Fixed the android get contacts to ignore case upon returning contacts

- Added a picture to some of the Simulator contacts

- Implemented component drawing for labels and buttons in the native async layer

- Native implementation of label and background drawing to squeeze every ounce of performance

- Major refactoring of the UI with the goal of improving on device performance for iOS/Android by moving frequently used code into the IMPL layer and allowing us to optimize it

- Added support for Android async editing

- Improved efficiency of Container.contains() for containers with many children

- Adjusted the decay scroll parameters based on comparisons with native iOS app scrolling

- Added additional checks for peer components before firing the onParentPositionChange event. This dramatically improves smoothness of drag scrolling

- Performance optimization for scrolling a large set of elements when we have no peers

- Changed default scroll motion to use exponential decay instead of friction to simulate more closely native behavior. You can revert to the previous friction style of scrolling by setting the theme constant 'ScrollMotion' to 'FRICTION'. You can tune the decay parameter 'DecayMotionScaleFactorInt' to adjust how sensitive the scrolling is.

- Removed excessive pointer events from event dispatch to prevent slowness during dragging

- Optimization for stringWidth that should avoid calls to that relatively slow API by caching the value within labels

- Added a binary search for the 'viewable' children of a container that can be used to make it easier to only paint visible children for large containers

- Removed redundant opengl calls to try to make painting more efficient

- Fixed contact getDisplayName() to never return null

- Removed a rectangle allocation that caused a GC in the EDT paint cycle. This fixes some jitter that occurs while scrolling

- Fixed the animation manager scrolling behavior so it won't misbehave when pulling from the top

- Made style animation work as expected for multiple elaborate cases

- Improved add/remove component to act as if the component was already added/removed and avoid some exceptions in cases that would be serialized

- Added ability to detect an empty border

- Fixed regression with ComponentGroup due to changing method signature

- Better error handling in the JavaSEPort

- New title animations and style animations API's

- Additional improvements for side menu bar preventing potential issue with animations

- Fix for potential exception if an animation is running while changing the Toolbar

- Fixed the animations and add/remove operations so they won't collide. If an animation is in progress add/remove will get queued until after the animation is performed

- New animation framework that accepts all animation requests globally and processes them

- New methods for Integer & Character classes

- Added getTitleComponent to the Toolbar

- Fixed regression in ios keyboard that caused it to not respect the ios.keyboardOpen build hint. Now it gives multiple levels of control over whether a field uses async editing or legacy editing

- Fixed getPrimaryPhoneNumber to return a number if a number exists

- Fixed issue with background location monitoring on iOS 9

- Fixed textarea resizing to work properly on iOS 7 and below

- Fixed edge case of text area being edited in legacy mode when part of the text area is covered by the keyboard. Now the native editing component is sized to only occupy the space above the keyboard so that scrolling happens more smoothly as typing, and the textview will auto scroll as the cursor moves in and out of view

- Fix so that legacy keyboard doesn't push the selected text area up any higher than the top of the text area

- Fixed issue with keyboard upside down multiplier on iOS 7

- Changes to keyboard on iOS to deal with quirks on iOS 5. Also modified some of the orientation detection handling that didn't seem to be working correctly on iOS 5.x

- Added ability to have more than one layered pane

- Workaround for regression in checkbox/combobox that broke their theme styles on the unselected state due to loading of theme beforehand

- Added support for tuning the speed of the infinite progress

- Fixed ConnectionRequest issue with setReadResponseForErrors is true

- Added a static factory method to UITimer

- Made building Codename One easier for 3rd parties with some build.xml teaks

- Added ability to disable the automatic tab UIID switching

- Fixed NPE in validator interaction dialog on scroll change

- Fixed deadlock when toggling between fields in legacy mode on iOS. Behaviour was seen only on iOS 7

- Fixed issue with iOS isTextEditing not correctly reporting because it deferred to the superclass isTextEditing methods. This resulted, occasionally, in text fields not being rendered properly when in async edit mode

- Fixed issue with keyboard opening, then closing, then opening again the first time a field is edited with async editing

- Fixed new fonts to respect italic/bold setting on Android

- Fixed text field editing with form bottom padding fields

- Added the onShowComplete callback when a show() is called on a Form which is the current Form

- Fixes in ParparVM encoding for cases where UTF characters might reside on the edge between bytes Fix for encoding issues triggered by partial reading of the bytes

- Reworked text input to reduce the cyclomatic complexity. Working well on iOS 9 with async, legacy, and form bottom padding test cases on iPhone 4S

- Added return statement to prevent calling editString twice simultaneously

- Added DeviceName property and added implementation to the stopTextEditing method

- Added optimization to reduce ParparVM stack operations involved in invoking methods that take only constants and local variables as parameters

- Added explicit hook in keyboardWillShow to hide the picker, if it is showing

- Added textposition support to title command

- Added ability to make rows in the MultiButton stick together

- Optimized stack operations in ParparVM surrounding binary integer operations

- Added some optimizations to reduce stack operations surrounding some integer comparisons in ParparVM

- Fixed issues with scrolling fields into view on iOS when using 3rd party keyboard

- Fairly major changes to text editing on iOS to fix some bugs that appeared when using the SwiftKey keyboard Since swiftkey will call the keyboardShown event multiple times while it is showing, with different keyboard sizes reported, we needed to handle these events more incrementally

- Fixed issue with not scrolling to focused text field on iOS when VKBAlwaysOpen is set to YES

- Added support for opacity in resource files

- Added support for opacity in FontImage

- Added simpler methods to edit/stop editing and track editing state for a text component

- Added a builtin ability to launch the text area editing when a form is shown which simplifies some common boilerplate code that is often implemented incorrectly

- Added a builtin ability to launch the text area editing when a form is shown which simplifies some common boilerplate code that is often implemented incorrectly

- Implemented RFE #1510 "simplifying error message handling"

- Added north, south, east and west methods to match the center method in the BorderLayout

- Made the parent scrollable public, this is important for some generic code that resides outside the package specifically the validation manager

- FloatingHint a new type of textfield/label wrapper that allows the hint to migrate to a label on top

- Fixed multiple issues in the popup positioning and arrow functionality

- Minor configuration fine tuneing for the progress indicator

- Fix for uncover transition

- Made iOS simulators use Helvetica Neue on Macs

- Added constructSideNavigationComponent to Toolbar to allow overriding the panel

- Fix for issue with native font loading using the new native: syntax

- Added missing theme constants for checkboxes Added missing theme constants for checkboxes

- Fix for RFE #1609 added support for the assert keyword

- Updated the size of the InfiniteProgress and made it spin faster

- Fixed pull to refresh to show the infinite progress

- Fixed issue with local sending a local notification causing push to be registered

- Moved createBackgroundMedia to the CodenameOneImplementation

- Removed some iOS 6 dependencies for compatibility with older versions

- New support for material design icon fonts

- Added background media playback to android and JavaSE port

- Fixed issue with network thread getting deadlocked in some cases when a setDisposeOnCompletion is set

- Fixed issue with infinite progress dialog not showing up if an existing progress dialog is opened

- Fixed issue with multiple concurrent infinite progress dialogs interfering with each other

- Fixed getAllContacts(...) to return the contacts sorted by name

- Support for the new hello world project look Support for the new hello world project look

- Fixed the fonts in the top 2 themes

- Fixed issue with iOS apps still being in 'minimized' state during the start() method

- Fixed issue with string picker showing up underneath keyboard on iOS. Now keyboard will auto fold when string picker is shown

- Merge pull request #1608 from @Pmovil Fixed multipart utf8 string (fails on android)

- Added support for onCompletionCallback for video components in iOS. Related to #1595

- Some improvements to the default demo in the designer

- Added isNativeFont supported and new flow layout terse factory methods

- Added ability to detect running on simulator which is useful for crash reporting

- Simplified code that allows binding standard boilerplate crash reporting logic

- Shorthand terse syntax for layered layout

- Fixed regression with testflight builds

- Added support for native OS fonts that are builtin to iOS/Android with multiple weights. This is mapped to the simulator on Windows/Linux as Roboto due to copyright issues with Helvetica.

- Fixed for exception in table layout

- Fixed issue that caused a revalidate to occur off the EDT in iOS when the keyboard is about to be shown. This may fix numerous issues. But, at least seems to fix an NPE from #1601

- Fixed issue where the first contact is always blank on iOS. Changed contacts to come out sorted by default #1598

- Changed Contact.getDisplayName() to return an alternate value like the phone, email, name if the contact has no display name #1598

- Fixed iOS port not respecting setChunkedStreamingMode. iOS8 had been using chunked streaming by default for multipart requests. Now it will only use chunked streaming when setChunkedStreamingMode() is called with a non-negative integer argument

- Fixed regression in Javascript bridge that broke execution on Android

- Fix for #1602 - Menu missing with Android 6 Marshmallow

- Added ability to hide/show components without removing them (setHidden)

- Fixed data dir location to .cn1 inside the project directory. This directory is used by the simulator for localStorage

- Added async methods to JavascriptContext for better performance when synchronous calls are not needed. Also fixed Javascript bridge callbacks in simulator

- Added improved debugging in the Simulator's BrowserComponent

- Fixed iOS local notifications. Turned out they had been commented out by a merge right after they were implemented

- Added some error logging when mkdir() fails

- Fixed simulator BrowserComponent support for localStorage

- Fixed bug in extracting html hierarchies

- Fixed issue with some fonts being cut off at the bottom in iOS

- Fixed stream closed error in video component on iOS #1595

- Removed unnecessary retain in creating video component that could cause memory leak

- Added terse API to download/parse JSON synchronously useful for webservices

- Added tearse add() methods to container that accept images to make the process of adding even simpler

- Created shorter default creation method to URLImage

- Added factory methods based on encloseWith to BorderLayout and BoxLayout to simplify/shorten the resulting code

- Added ability to construct a form with a layout manager

- Slider is now recognized by the generic list cell renderer and can be used in a renderer component

- Workaround for EDT performance issues while dragging

- Added the ability to extend the Toolbar and modify the side panel

- Added support for background location services and geo fencing on iOS

- Added support for Properties file format

- Exposed a public constructor for Label which accepts the text and a UIID, this makes terse code simpler

- Made table layout constraint construction more terse for easier layout of tables

- Added geofence and background location tracking
