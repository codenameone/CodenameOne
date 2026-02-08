---
title: "Faq"
date: 2020-09-11
slug: "faq"
---

- Home
- FAQ's

## FAQ

You can check out a more technical [FAQ at StackOverflow.](https://stackoverflow.com/questions/tagged/codenameone?sort=votes&pageSize=50)

- General
- Licensing & Legal
- Technical
- Company & Team

 What Is Codename One? How Does it Work?

Codename One allows Java developers to write their mobile apps using Java. It generates native OS binaries that you can upload to Apple/Google etc.

To understand how Codename One works, check the [introduction](https://www.codenameone.com/introduction.html) or this [stackoverflow answer](https://stackoverflow.com/questions/10639766/how-does-codename-one-work/10646336#10646336).

Is Codename One Free? Is it open source?

Yes. Codename One is open source and free for use commercially and non-commercially with no royalties or restrictions.

  
You can use the optional build service which is available in a free quota. This service makes building apps simpler and also lets developers on Windows/Linux build native iOS apps. Notice that the JavaScript port is closed source.  
  
Check out the [pricing page](https://www.codenameone.com/pricing.html) for more details.

What are the limits of the free version?

Notice that these only apply to the build cloud which is 100% optional.

You are limited to 100 build credits per month. 1 build credit equals 1 build, except for iOS where 8 credits are spent per 1 build to offset the higher costs of Mac servers.

Other than that, there is a JAR size limit of 1mb. Notice that this is a very high limit as it applies only to your bytcode and not to the overhead of the Codename One class libraries!

You are **allowed** to use your generated apps commercially and have no restrictions on them. You don’t get some of the benefits that come with the various [paid grades](https://www.codenameone.com/pricing.html).

Do I Need A Mac To Build Or Test An iOS (iPhone) Application?

You don’t if you use the build cloud which has Macs in the cloud.

However, Apple requires Macs for iOS development so if you use the free/open source option, you will need a Mac. The only place where you still need a Mac at this time is for store upload which at this time requires the Application Loader tool that is only available on a Mac.

We hope to automate that aspect too in a future update, in the meantime you can use services such as [Mac In Cloud](https://www.macincloud.com/) to accomplish this.

How performant is Codename One? How does its performance compare to Native/HTML5?

Codename One is fast. Simple benchmarks show it beating Objective-C and approaching [C performance on iOS](http://codenameone.blogspot.com/2012/12/codename-one-benchmarked-with-amazing.html). On other platforms, Codename One uses the native VM where possible to achieve similar results. Graphics rendering is implemented with the gaming API’s (e.g. OpenGL) hence providing game like performance.

In the new Codename One VM we also implemented a [non-blocking concurrent GC](https://www.codenameone.com/beating-the-arc.html) which completely eliminates any GC stalls. Codename One restricts the size of the API’s supported and can thus compile very small applications that are under 5mb in size for fully functional complex iOS applications with 64bit support.

See this [Comparison](https://www.codenameone.com/compare.html) for details.

How much memory does Codename One take?

Codename One can be very efficient and is able to run on devices with as little as 2mb of RAM. Notice that since Java is a GC’d language, tools such as memory monitors might misrepresent the actual memory being used.

Will Apple Allow This? Doesn't their EULA (End User License Agreement) Prohibit Things Like This?

That’s old news. Apple revisited their EULA and now allows tools such as Flash, Lua and other languages/meta-platforms on the device as long as the applications comply with the iOS store guidelines.

This means you as developers would need to work hard to create high quality applications and test them on the devices to see they behave properly but you do not need to code them manually in Objective-C.

  
One thing Apple doesn’t allow is JIT code, which means all code must be compiled before hand and things such as reflection becomes problematic.

  
There are plenty of Codename One apps in the store (check out the app gallery in the home page for examples) with well over 100M installs!

Is My Source Code Sent To The Cloud?

**No!** We only send compiled bytecode to the cloud and process that bytecode. We don’t touch your source code.

Notice you can always build offline using Maven if you’re truly concerned about this.

There is a special case with native code which must be sent in source code form since it can’t be compiled on the client e.g. Objective-C code can only be compiled on a Mac with XCode.

All communications with our servers are done securely over SSL.

What features of Java are supported? What features of Java aren't supported?

The most obvious thing missing is reflections. The main problem is that when we package the VM into devices that don’t have Java, we would have to include EVERYTHING. If reflections were included, they wouldn’t work anyway since we obfuscate the code for the platforms where reflections do work (e.g. Android). On top of that reflection code is generally slow and a bad idea on a mobile device to begin with. As an alternative some developers were successful with bytecode manipulation which is something that is completely seamless to the server and as performant/efficient as handcoding.

Many of the desktop API’s such as java.net, java.io.File etc. aren’t very appropriate for mobile devices and just didn’t make it. We provide our own alternatives which are more portable and better suited for mobile settings.

Of the other missing things, if you run into a missing method or ability, there are cases where that functionality can be added.

Can I use 3rd Party Libraries With Codename One? Can I Add External Library JAR's To The Classpath?

Yes and no. You need to use a special Codename One library format since we need the compilation to happen in a very specific way. We have an extensive tutorial about this [here](https://www.codenameone.com/integrating-3rd-party-native-sdks-part-1.html) and in our developer guide.

  
The big advantage of cn1libs is that you can write native code in them as well (if you wish), that means you can write Objective-C code for iOS and Java (Dalvik/ART) code for Android and have it all work seamlessly. There are already quite a few cn1libs written by both Codename One and the community, check out a [partial list here.](https://www.codenameone.com/cn1libs.html)  You can install such libraries via the extension manager tool in Codename One Settings.

Can I Use Other JVM Languages Such as Scala, JRuby, Jython etc?

Steve Hannah has built a [plugin to support Mirah](https://www.codenameone.com/mirah-for-codename-one.html) on Codename One. Mirah is very similar to Ruby without some of the elements that make it hard to optimize. Mirah is a compiled language so this work was easier (and Scala might be too) however this would require some work since Codename One doesn’t support the full Java language specification and probably won’t support things such as reflection etc. to keep the size small and efficient.

We intend to support all of the above in the future as the product matures by porting their environments and adapting it to mobile.

Are all iOS devices supported?

All iPads are supported, iPhones starting with 3GS and OS 4.3 or newer are required due to limitations from Apple (Apple dropped support for Arm6 on newer versions of Xcode & requires new versions to support the iPhone 5).

  
At this time we don’t yet support Apple TV or Watch due to lack of demand although both should be possible. Our VM supports bitcode which is the main piece required for that support.

 Will my App Still Work if I Cancel my Subscription?

Yes. The cloud is only used for build services. Once the app is built, it is a native local app. Notice that push notifications require server activity and will no longer function if you cancel the subscription.

Can I build more than one app?

There are no limits on the number of apps or number of users! For paid plans, our pricing is per developer seat.

How many licenses do I need?

One per developer on your team that is working with Codename One. Notice that all licenses within the organization must be of the same subscription level and every developer needs a license.

Can I purchase a basic license for one developer in my team and an enterprise license for another?

No. All licenses within the organization must be of the same subscription level and every developer needs a license.

  
We find that additional developers start routing support requests through the paid account making the complexity of supporting the enterprise/pro account within the organization much higher.

If we end development with Codename One but still have an app in the store do we still need to pay?

Only if you use push/cloud services/crash protection or other cloud features. If you don’t use the runtime cloud services, the application will work without the cloud regardless of your subscription status. Notice that the developer documentation highlights such services as cloud services and they are always limited to pro subscriptions or higher.

Do you offer academic licenses?

The base version of Codename One was designed to be sufficient for the needs of academic/research work so there is no need for academic specific licenses.

Can I download the sources generated by the server?

Paid subscribers can download the sources generated by the server. However, sources are only generated for Android/iOS & Windows. Notice that not all ports offer sources that are truly useful.

What is the license of the Codename One Source Code? Can I use the source code commercially?

The Codename One source code is licensed under the GPL with Classpath Exception, the classpath exception clause allows you to package binaries generated by the code into your application. You don’t need to open source your own code as a result, the only major requirement is that you contribute your changes to Codename One back or “make them available”.

Can I contribute code to Codename One?

We are always happy to receive code contributions. You can just file a pull request in the [GitHub project](https://github.com/codenameone/CodenameOne).

 I have a problem with Codename One what should I do?

Check the [issue tracker](https://github.com/codenameone/CodenameOne/issues) & ask us in the [Subreddit](https://www.reddit.com/r/cn1/). You can also post questions on [StackOverflow and tag them as codenameone.](https://stackoverflow.com/tags/codenameone)

 Who is behind Codename One?

Codename One was started by ex-Sun guys who have been working together for 15 years on mobile development tools.  
  
Check out our [team section](https://www.codenameone.com/team.html) for more details.

Are you hiring?

Follow our Linkedin page for job postings. Occasionally we might hire community members with exceptional abilities.

What happens if you go bankrupt or get purchased?

Since we are an open-source company, you would still be able to build your code.

  
We are confident that in the worst case scenario the community will simplify the process of offline builds as developers all over the globe depend on Codename One and will need it to keep working.

Enterprise developers get additional help both in building offline and in the SLA which provides an option for a source code escrow contract. The escrow contract provides access to the full Codename One server source code in case of insolvency or inability to provide its service. The SLA defines the service guarantees made by Codename One to enterprise licensees.

## IMPORTANT LINKS

- [Discussion Forum/Support/Help](https://www.reddit.com/r/cn1/)
- [StackOverflow - Codename One](https://stackoverflow.com/questions/tagged/codenameone)
- [Documentation](https://codenameone.com/developing-in-codename-one.html)
- [Blog](https://codenameone.com/blog.html)
