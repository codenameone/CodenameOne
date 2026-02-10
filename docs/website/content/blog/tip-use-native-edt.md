---
title: 'TIP: Use the Native EDT'
slug: tip-use-native-edt
url: /blog/tip-use-native-edt/
original_url: https://www.codenameone.com/blog/tip-use-native-edt.html
aliases:
- /blog/tip-use-native-edt.html
date: '2017-04-30'
author: Shai Almog
---

![Header Image](/blog/tip-use-native-edt/tip.jpg)

The bootcamp is winding down and I’m finally back to our usual scheduled posts. I’d like to open with a common practice that isn’t nearly documented enough: use the native main/EDT threads. Our EDT serves many purposes but one of it’s biggest roles is portability. By having an EDT we get consistent behavior across platforms.

iOS, Android & pretty much any modern OS has an EDT like thread that handles events etc. The problem is that they differ in their nuanced behavior. E.g. Android will usually respect calls off of the EDT and iOS will often crash. Some OS’s enforce EDT access rigidly and will throw an exception when you violate that…​

Normally you don’t need to know about these things, hidden functionality within our implementation bridges between our EDT and the native EDT to provide consistent cross platform behavior. But when you write native code you need awareness.

This begs the question:

### Why not Implicitly call Native Interfaces on the Native EDT?

Good question and I’m glad you asked it!

Calling into the native EDT includes overhead and it might not be necessary for some features (e.g. IO, polling etc.). Furthermore, some calls might work well with asynchronous calls while others might need synchronous results and we can’t know in advance which ones you would need.

### How do we Access the Native EDT?

Within your native code in Android do something like:
    
    
    com.codename1.impl.android.AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
        public void run() {
           // your native code here...
        }
    });

This will execute the block within `run()` asynchronously on the native Android UI thread. If you need synchronous execution we have a special method for Codename One:
    
    
    com.codename1.impl.android.AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
        public void run() {
           // your native code here...
        }
    });

This blocks in a way that’s OK with the Codename One EDT which is unique to our Android port.

#### iOS

On iOS this is pretty similar (if you consider objective-c to be similar). This is used for asynchronous invocation:
    
    
    dispatch_async(dispatch_get_main_queue(), ^{
        // your native code here...
    });

You can use this for synchronous invocation, notice the lack of the `a` in the dispatch call:
    
    
    dispatch_sync(dispatch_get_main_queue(), ^{
        // your native code here...
    });

The problem with the synchronous call is that it will block the caller thread, if the caller thread is the EDT this can cause performance issues and even a deadlock. It’s important to be very cautious with this call!

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
