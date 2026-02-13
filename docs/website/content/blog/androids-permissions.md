---
title: Androids Permissions
slug: androids-permissions
url: /blog/androids-permissions/
original_url: https://www.codenameone.com/blog/androids-permissions.html
aliases:
- /blog/androids-permissions.html
date: '2014-03-23'
author: Shai Almog
---

![Header Image](/blog/androids-permissions/androids-permissions-1.jpg)

  
  
  
  
![Picture](/blog/androids-permissions/androids-permissions-1.jpg)  
  
  
  

One of the annoying tasks when programming native Android applications is tuning all the required permissions to match your codes requirements, when we started Codename One we aimed to simplify this. Our build server automatically introspects the classes you sent as part of the build and injects the right set of permissions required by your app. 

However, sometimes you might find the permissions that come up a bit confusing and might not understand why a specific permission came up. This maps Android permissions to the methods/classes in Codename One that would trigger them:

android.permission.WRITE_EXTERNAL_STORAGE – this permission appears by default for Codename One applications, since the File API which is used extensively relies on it. You can explicitly disable it using the build argument android.blockExternalStoragePermission=true, notice that this is something we don’t test and it might fail for you on the device.

android.permission.INTERNET – this is a hardcoded permission in Codename One, the ability to connect to the network is coded into all Codename One applications.

android.hardware.camera & android.permission.RECORD_AUDIO – are triggered by com.codename1.Capture 

android.permission.RECORD_AUDIO – is triggered by MediaManager.createMediaRecorder() & Display.createMediaRecorder()

android.permission.READ_PHONE_STATE – is triggered by com.codename1.ads package, com.codename1.components.Ads, com.codename1.components.ShareButton, com.codename1.media, com.codename1.push, Display.getUdid() & Display.getMsisdn(). This permission is required for media in order to suspend audio playback when you get a phone call.

android.hardware.location, android.hardware.location.gps, android.permission.ACCESS_FINE_LOCATION, android.permission.ACCESS_MOCK_LOCATION & android.permission.ACCESS_COARSE_LOCATION – map to com.codename1.maps & com.codename1.location.

package.permission.C2D_MESSAGE, com.google.android.c2dm.permission.RECEIVE, android.permission.RECEIVE_BOOT_COMPLETED – are requested by the com.codename1.push package

android.permission.READ_CONTACTS – triggers by the package com.codename1.contacts & Display.getAllContacts().

android.permission.VIBRATE – is triggered by Display.vibrate() and Display.notifyStatusBar()

android.permission.SEND_SMS – is triggered by Display.sendSMS()

android.permission.WAKE_LOCK – is triggered by Display.lockScreen() & Display.setScreenSaverEnabled()

android.permission.WRITE_CONTACTS – is triggered by Display.createContact(), Display.deleteContact(), ContactsManager.createContact() & ContactsManager.  
  
deleteContact() 

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
