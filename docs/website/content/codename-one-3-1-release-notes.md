---
title: "Codename One 3.1 Release Notes"
date: 2015-07-27
slug: "codename-one-3-1-release-notes"
---

# Codename One 3.1 Release Notes

1. [Home](/)
2. 3.1 Release Notes

### Summary

Version 3.1 is the first release in our fast pace release cycle of 4 releases per year. It brings stability, bug fixes and great new features to the table. The biggest highlights of this release are support for Java 8 and simplified certificate generation for iOS. Check out the list below for more details.

### Highlights - Click For Details

Java 8 Language Features

Support for Java 8 features such as Lambdas, try with resources etc. This is a beta grade feature but is showing great promise so far.  
Read more about this work in [this blog post](/blog/java-8-support.html).

iOS Certificate Wizard

Simple generation of certificates and provisioning for itunes without a Mac using a wizard interface.  
Read more about this work in [this blog post](/blog/ios-certificate-wizard.html).

Authentication Framework

Provides the ability to signin to various services in a generic way including builtin support for Google, Facebook and generic oAuth 2.0 services.  
Read more about this work in [this blog post](/blog/sign-in-with.html).

Font Icons

Support for using icon fonts to represent images in the UI thus reducing the reliance on multi-images.  
Read more about this work in [this blog post](/blog/icon-fonts-oldvm-swan-song.html).

Better Crash Reporting

As part of migrating away from Google App Engine we shifted crash reports to use new servers which make them far more usable by embedding the logs directly into the email body. Read more about this work in [this blog post](/blog/migrating-away-from-app-engine.html).

### Details

- Fixed issue #1460 - made methods of PurchaseCallback run on the EDT for Android
- Fixed issue #1465 - Error starting MyApplication5: Module 'MyApplication5-10' has verification error 2924 at offset cac9 (codfile version 79)
- Fixed RFE #1477 - Added TextField password obfuscation on desktop builds
- Fixed issue #1457 - Validator is not re-enabling submit buttons
- Migrated the location of storage files to a new location
- Fixed issue #1479 - OAuth2 result access token processing causes StringIndexOutOfBoundsException
- Created native implementations for some of the heavier used String and StringBuilder methods for faster performance
- Fixed issue #1484 - IOS GLUImage.m Memory Leak
- Fixed issue #1410 - iOS: Animations still running while app is background
- Fixed RFE #1430 - Added support for instantly killing connections when kill() is called on them so that they don't continue to block the queue.
- Fixed issue #1100 - FileSystemStorage.getLastModified() returns 0 on iOS
- Fixed issue #1424 - Failed to fetch address from contact on iOS
- Fixed String.indexOf() to allow for negative starting positions for the new VM
- Fixed issue #1231 - SQL errors now throw IOExceptions on iOS
- Fixed issue #1074 - virtual keyboard might scroll content behind statusbar on iOS7
- Added arc() method to GeneralPath
- Fixed issue #1488 - OnOffSwitch for android not working
- Fixed problem with accessing static variables in the new VM
- Fixed GC issue with static objects such as arrays of strings or multi-dimension array
- Fixed issue #1470 - Auto-scrolling while dragging does not behave well
- Fixed String.toUpperCase()/toLowerCase() to work with non-ascii chars
- Fixed issue #1497 - Media Player Controls behavior on Android
- Fixed issue #1498 - null pointer exception when attach/detach bluetooth barcode scanner
- Fixed issue related to #1372 where NSString was failing silently on data with the wrong encoding
- Added macros CN1\_YIELD\_THREAD and CN1\_RESUME\_THREAD that an be used in native code that blocks and could potentially cause deadlocks with the GC thread
- Fix for issue #1505 - static final strings were removed from the GC but their internal char array was not
- Fix for issue #1481 - FlowLayout with setFillRows, true draws contained component (button) shifted to the right
- Added fireActionEvent to Calendar
- Deprecated cloud storage
- Fixed Socket available() always returning 0 on connectionEstablished() in iOS.
- Fixed issue #1513 removing attribute that might cause an issue with appstore submission
- Implemented Database.execute(String,Object\[\])
- Fixed Database.execute to deal properly with null and blob parameters
- Fixed nullpointer exception on apps that support Push - but are built for a platform that does not support Push
- Fixed issue #1520 - admob shows black container
- Fixed the admob adview which stole the focus from the main view causing it to not paint properly
- Fixed issue #1425 - out of memory errors on iOS with large Multipart requests
- Made it possible to find the component matching a given command if applicable
- Fixed issue #1095 - Container.replace() with null transition does not update x, y, width and height of next component
- Fixed issue #1007 - position of back command wrong on first call to addCommand in SideMenuBar class
- Improved the general performance of String methods and HashMap elements.
- Fixed issue #1526 - Android port, recent updated build server, ConnectionRequest missing sent Content-Length while POST
- Fixed problem with arcs in the charts package on Android 4. Arcs were showing up distorted
- Changed the requestFocus to happen only when there are ads on the Form
- Fixed scrolling for forms with ChartComponent. Fixed label text size in charts.
- Fixed possible infinite loop in chart rendering
- Changed charts to use built-in arc function instead of using a path. Should improve performance on some platforms
- Fixed issue #1519 - Synchronized block deadlock after an exceptions on iOS
- Fixed catch all exception which wasn't caught correctly using 0 instead of -1 class id
- Fixed exception pointer to point at the right location after an exception in a nested try/catch was thrown so if an exception was caught then rethrown it wasn't caught in the correct catch statement and initially even caused a deadlock
- Removed deprecated maker package
- Simplified the java.util.Timer code on the iOS port to make it function more correctly
- Added a setChunkedStreamingMode API
- Fixed the infinite progress caching
- Fixed issue #1377 for regression with showing text fields in iPad on iOS 7
- Fixed oauth2 to handle json token responses
- Fixed issue with fillArc in iOS
- Fix for issue with URLImage not working with ImageViewer
- Made force revalidate work more aggressively
- Fixed Issue #1540 - Network request is halted when the dialog cannot be shown up due to network problem.
- Made Container Iterable to allow for-each code such as for(Component cmp : cont) { .... }
- Improved XML parser case sensitivity
- Fixed the app home path in the JavaSE port when the filesystem is exposed (desktop port)
- Fixed iOS drawArc to work for both negative and positive angles properly
- Added support for improved for each with XML element
- Modernized some XML processing code allowing for-each on entries
- Fixed issue with dup\_x2 instruction not handling form 2 (where the 'middle' word is a double or long)
- Updated facebook to version 4.4.0 and added inviteFriends
