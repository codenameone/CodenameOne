---
title: Native iOS Code Callbacks
slug: native-ios-code-callbacks
url: /blog/native-ios-code-callbacks/
original_url: https://www.codenameone.com/blog/native-ios-code-callbacks.html
aliases:
- /blog/native-ios-code-callbacks.html
date: '2014-06-01'
author: Shai Almog
---

![Header Image](/blog/native-ios-code-callbacks/native-ios-code-callbacks-1.jpg)

  
  
  
  
![Picture](/blog/native-ios-code-callbacks/native-ios-code-callbacks-1.jpg)  
  
  
  

Writing native code in Codename One is pretty simple, however one piece is relatively vague and that is the ability to call back from native code into the Java code. The reason we left this relatively vague is due to the complexity involved in exposing/documenting this across multiple OS’s/languages so we chose to document this on a case by case basis. 

A common trick for calling back is to just define a static method and then trigger it from native code. This works nicely for Android, J2ME & Blackberry however mapping this to iOS requires some basic understanding of how the iOS VM works. Worse, due to the changes we made in the new VM if you are using such code you will need to adapt it as we migrate to the new VM or your code will stop working.

For the purpose of this explanation lets pretend we have a class called NativeCallback in the src hierarchy under the package com.mycompany that has the method: public static void callback().  
  
So if we want to invoke that method from Objective-C we normally would have just done the following. Added an include as such:  
  
#include “com_mycompany_NativeCallback.h”

Then when we want to trigger the method just do:  
  
com_mycompany_NativeCallback_callback__();

This will not compile with the new VM. The new VM now passes the thread context along method calls to save on API calls (thread context is heavily used in Java for synchronization, gc and more). However, to keep code compatible we added a few macros that allow us to maintain XMLVM/newVM portability and they are defined as blank when running under XMLVM. So to do something like this in the new VM all we need to do is add an include for CodenameOne_GLViewController.h as such:  
  
#include “CodenameOne_GLViewController.h”

Then we can invoke the method like this:  
  
com_mycompany_NativeCallback_callback__(CN1_THREAD_STATE_PASS_SINGLE_ARG);

But what if we defined the method as such: public static void callback(int arg)

This would map under XMLVM to something like this:  
  
com_mycompany_NativeCallback_callback___int(intValue);

(notice the extra _ before int). You can adapt this for the new VM like this:  
  
com_mycompany_NativeCallback_callback___int(CN1_THREAD_GET_STATE_PASS_ARG intValue);

Notice that there is no comma between the CN1_THREAD_GET_STATE_PASS_ARG and the value! This is important since under XMLVM CN1_THREAD_GET_STATE_PASS_ARG is defined as nothing, yet under the new VM it will include the necessary comma. Its not an ideal solution but it solves the portability issue as we slowly migrate to the new VM.

Many of you have been passing string values to the Java side, or really NSString* which is iOS equivalent. So assuming a method like this: public static void callback(String arg)

You would need to convert the NSString value you already have to a java.lang.String which the callback expects. We used to do this as such:  
  
com_mycompany_NativeCallback_callback___int(fromNSString(nsStringValue));

However, the from NSString method also needs this special argument so you will need to modify the method as such:  
  
com_mycompany_NativeCallback_callback___int(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG nsStringValue));

And finally you might want to return a value from callback as such: public static int callback(int arg)

This is tricky since we had to change the method signatures to support covariant return types and so the signature of that method under XMLVM would be:  
  
com_mycompany_NativeCallback_callback___int(intValue);

But under the new VM it is:  
  
com_mycompany_NativeCallback_callback___int_R_int(intValue);

The upper case R allows us to differentiate between void callback(int,int) and int callback(int). Unfortunately portability here isn’t trivial so ifdef’s are the only way. What we do is something like this:  
  
#ifdef NEW_CODENAME_ONE_VM  
  
JAVA_INT val = com_mycompany_NativeCallback_callback___int_R_int(intValue);  
  
#else  
  
JAVA_INT val = com_mycompany_NativeCallback_callback___int(intValue);  
  
#endif

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — October 22, 2014 at 4:14 am ([permalink](/blog/native-ios-code-callbacks/#comment-22099))

> Anonymous says:
>
> Should public static void callback(String arg) translate to 
>
> com_mycompany_NativeCallback_callback___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG nsStringValue)); 
>
> and not 
>
> com_mycompany_NativeCallback_callback___int(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG nsStringValue)); 
>
> ?
>



### **Anonymous** — October 23, 2014 at 12:30 am ([permalink](/blog/native-ios-code-callbacks/#comment-24185))

> Anonymous says:
>
> Thanks, yes I missed that one. Sorry.
>



### **Anonymous** — December 4, 2014 at 12:27 pm ([permalink](/blog/native-ios-code-callbacks/#comment-22053))

> Anonymous says:
>
> For me it is unclear, when should be used 
>
> CN1_THREAD_STATE_PASS_ARG 
>
> and when 
>
> CN1_THREAD_GET_STATE_PASS_ARG 
>
> ? 
>
> It is clear, that when threadStateData is not available in current context, then we have to use CN1_THREAD_GET_STATE_PASS_ARG, but do we need it also when threadStateData is available? 
>
> For example in iOSPort/nativeSources/CodenameOne_GLViewController.m line 334 is like this: 
>
> str = com_codename1_ui_plaf_UIManager_localize___java_lang_String_java_lang_String_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG obj, fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @”next”), fromNSString(CN1_THREAD_GET_STATE_PASS_ARG @”Next”)); 
>
> does it mean, that sometimes we need to use CN1_THREAD_GET_STATE_PASS_ARG even when threadStateData variable is available in current context?
>



### **Anonymous** — December 5, 2014 at 3:26 am ([permalink](/blog/native-ios-code-callbacks/#comment-22166))

> Anonymous says:
>
> The defines are: 
>
> #define CN1_THREAD_STATE_PASS_ARG threadStateData, 
>
> #define CN1_THREAD_GET_STATE_PASS_ARG getThreadLocalData(), 
>
> Obviously the former will fail if there is no threadStateData variable in the current context (e.g. if you are in a callback from native). In most cases when writing a native interface you will need to use the latter form.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
