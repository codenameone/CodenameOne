---
title: ACCESS NATIVE DEVICE FUNCTIONALITY? INVOKE NATIVE INTERFACES?
slug: how-do-i-access-native-device-functionality-invoke-native-interfaces
url: /how-do-i/how-do-i-access-native-device-functionality-invoke-native-interfaces/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-access-native-device-functionality-invoke-native-interfaces.html
tags:
- advanced
description: How to integrate native iOS, Android etc. functionality into your Codename
  One app without sacrificing the portability of the whole app
youtube_id: 2xKvvv7XoVQ
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-10-1.jpg
---

{{< youtube "2xKvvv7XoVQ" >}} 

#### Transcript

In this video I’ll discuss native interfaces, it’s a big subject so I’ll cover the basics but you will need the developer guide and obviously the native API guides to get you thru this. Native interfaces are the system we use to call into native code from Codename One without disrupting the portability of Codename One.

You can just add a native interface to your app or to a cn1lib which is a standalone library. One of the cool things in cn1libs is that they support native interfaces and thus work seamlessly. You can wrap complex native logic and hide that from the user of the cn1lib.

When we say native in Codename One we mean something that’s technically very different from the standard Java definition of native. We mean “use the platforms natural language” when we say native.

So when we are on Android and we invoke a native interface we will go into Java code. But it will be separate code where we can use the full Android API and 3rd party libraries. That code will only execute on Android so you can just use anything you want including the Native Development Kit which allows you to get all the way into C code.

On iOS we expose Objective-C callbacks which are more natural to the platform.  
On Windows a native interface will map to a C# object.  
When compiling to JavaScript you can call into JavaScript itself and write logic there.  
And with the desktop port you can just write JavaSE code that accesses Swing and other such API’s.

So how does it work or why do we call it a native interface?  
Well, because it’s actually an interface. You need to define an interface and it needs to extend the NativeInterface interface so we’ll know that this is a native call. Here I defined a relatively simple one method interface but you can have more elaborate interfaces in place. NativeInterface itself defines one method isSupported() which always returns false by default. This means that you can easily ignore platforms which you don’t support and just check the isSupported() flag.  
So how does this work?

You right click that interface in the IDE and select the generate native option. This will generate stubs into the native directory under your project root, in Eclipse and IDEA the native directory should be visible. In NetBeans you will see it when you switch to files mode.

This is the stub generated for Android, you will notice it’s just a standard Java file but in this code we can reference any Android API we want as long as we import it correctly.

We can implement this just like we can any other class, notice that we also returned true for isSupported() otherwise the code might be ignored.  
Notice that the JavaSE stub and implementation are practically identical so I’ll skip those.

For iOS the code looks a bit different but you will notice the default implementation for isSupported that returns NO and the method itself that returns nil. Notice that in Objective-C argument names have meanings so you can’t change the argument names as this will break compatibility…

Again the implementation in iOS for this native code is mostly trivial. Notice we return an NSString which is the Objective-C native representation of a String but we seamlessly get a java.lang.String on the Java side. That’s just some of the magic done by the native interface binding. It’s designed to make all of this act with the least amount of friction.

I won’t go much into other platforms but this is the C# stub, notice it’s very similar.

This is the JavaScript stub. With JavaScript we must do a callback instead of returning a value directly. This is important as JavaScript is single threaded but we need to break down the code to allow the feel of multi-threading so the callback return is essential.

Finally we can invoke the native code, we use the lookup class to find the right native implementation. This can sometimes be null for instance in the build servers so it’s something we must check. We also check the isSupported method to make sure this specific platform was implemented. We can then use the native code as if it was a standard Java method.

So how does it work. Normally Codename One sends only bytecode to the servers but in the case of native interfaces the native source must be sent to the server where we can compile it with the native compiler. So even if you have a PC you can write Objective-C code and it will compile in the cloud. But this creates a situation where code completion won’t work, the source will be highlighted in RED as if it can’t compile even if you have the right SDKs installed.

One of the tricks we use is to send a build with include source. We then implement the native interface in the native IDE. Test and debug it. Then copy and paste the debugged source code back into the native directory.

Native interfaces are very restrictive in terms of the types you can use. You can pass primitives, Strings, arrays of bytes and peers. The reasons for these restrictions relate to the difficulty in the translation process. Imagine working with a complex Java object from Objective-C or JavaScript. Using these restrictions we can simplify the translation code and also guarantee good performance.

We do support `PeerComponent` which is a huge use case. It allows the native code to return a native component that you can add into your layout as if it’s any other component. The best example of this is the native google maps implementation which literally returns a native map widget as a peer component. You can check out the source code of that cn1lib for reference.

Native code can do callbacks back into Codename One code. It is a bit challenging though so I suggest reading about it in the developer guide where we go into more details.

One of the biggest difficulties when dealing with native code is in the configuration and not in the code. Build hints allow us to support all types of configurations. While the list of build hints is pretty extensive I would suggest consulting with us if you run into difficulties as some of the settings can be very nuanced.

Native library integration instructions often start with gradle dependency instructions for Android or a cocoapod instruction for iOS. Both of these are supported thru build hints and you can just integrate a native library by defining the right hints. You can then use it directly from the native interface.

Another challenge is with changes required for the manifest or plist. Both allow you to inject elements into them. You might need to add specific files such as additional libraries or source files. You can just place them in the respective native directory next to your native implementation code and it will get packaged during the build process with the rest of the native code.

I highly recommend checking out our cn1libs which implement many native interfaces and can serve as samples for pretty much anything.

Thanks for watching, I hope you found this helpful.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
