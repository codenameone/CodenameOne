---
title: Why we don't Support the Full Java API
slug: why-we-dont-support-the-full-java-api
url: /blog/why-we-dont-support-the-full-java-api/
original_url: https://www.codenameone.com/blog/why-we-dont-support-the-full-java-api.html
aliases:
- /blog/why-we-dont-support-the-full-java-api.html
date: '2016-09-14'
author: Shai Almog
---

![Header Image](/blog/why-we-dont-support-the-full-java-api/generic-java-1.jpg)

This is something we run into every week. A new Codename One user writes asks why “feature X” from Java isn’t  
supported. In this post we’d like to explain the “bigger picture” or why less is more…​

Supporting the full Java API in Codename One would be a mistake that will lead us down a problematic path.  
It would cost you a great deal in functionality, performance, portability, stability and more!

__ |  We are still adding features to the VM but we trickle them in rather than supporting “everything”   
---|---  
  
### Why we Don’t Support “Everything”?

For the lazy here is the “cliff notes” version:

Supporting everything will increase the size of the distribution, eliminate true portability, reduce performance &  
ironically make Java compliance harder!

### Size/Performance

This is pretty easy to prove, the JDK’s `rt.jar` is 63mb and even supporting an incomplete version of it closer to  
what Android supports would result in a binary that is 10x-20x larger than ours for a simple hello world.

This is pretty easy to calculate, ARM code is at least 4x larger than bytecode. iOS apps need at least 2 ARM  
platforms which means at least 8x without the bitcode portion. A good compiler can strip out unused bytecode  
which is why an implementation will be 10x-20x larger (rather than 60x larger) but there are limits.

In fact we did look at some experimental attempts to provide a larger portion of the JVM. Those attempts delivered  
50MB binaries for things we delivered in 3MB.

Performance is a complicated subject in mobile CPU’s but size is a big factor in such devices. Furthermore, mobile  
OS’s place restrictions on larger applications (no OTA updates etc.). If we are paying in size/performance, what we  
are getting in return should be worth it!

### No Benefit in Supporting the Full JVM

The assumptions for many developers is that if we support the full JVM they can just “take code” and it will work…​  
This is a problematic and incorrect assumption for most cases.

Code that relies on `java.io` will need work so it can fit into device filesystem restrictions.

UI needs to be adapted to mobile and most Java UI framework code can’t be.

SQL/Database code can’t be used since connecting from a device to a remote database is “impractical”.

Bytecode manipulation won’t work since compiled code no longer has the bytecode. Reflection would be problematic  
as it will increase the distribution size even more and be ridiculously slow on mobile without a JIT.

Furthermore, on platforms such as Android we obfuscate the code to make it faster/smaller and harder to crack.  
Obfuscation is recommended by Google and an important performance tool reducing size significantly. However,  
it collides with some features in Java such as reflection and dynamic class loading.

Dynamic class loading is also redundant as all classes must be packaged in advance and known during compile.  
Using tools such as `Class.forName()` creates redundant indirection that problems with obfuscation/optimization,  
these can be replaced with class literals e.g. `MyClass.class`. Class literals don’t suffer from these problems and provide  
similar flexibility in devices.

#### Java EE/Android Code

There are two HUGE markets of Java developers: Java EE & Android developers.

All other markets (Swing/FX/JavaME/Embedded etc.) are [small and shrinking](/blog/should-oracle-spring-clean-javafx.html).

Java EE developers can’t reuse code “as is” anyway so the point of compatibility is mute, you would need to do  
a lot of work to move code from Java EE so doing a bit more shouldn’t be a deal breaker.

Reusing Android code to some extent is an attractive proposition, however the real value we can provide is in reusing  
Android UI elements. We get a lot of developer requests to do this but when we started an effort to bring this to  
developers there was no tangible activity around this. If you would like to see more Android compatibility so you  
can port apps more easily check out [this blog post](/blog/android-migration-tool.html).

### Testability

A large product is harder to test. Testing on devices is harder still!

I’ve worked for years at Sun, we had a great QA team and test suites (none of which are accessible to open source  
projects). Some developers think picking the Android or Oracle code bases means that those code bases are  
stable on devices such as iOS. The fact is that this couldn’t be further from the truth!

E.g. iOS has some limitations on networking that are unintuitive. Code would seem to work on device and  
simulator but would fail to work in some conditions in the field…​

Codename One avoids this fate by brining in code in small pieces in a way designed for portability.

### Portability

To increase portability we need a small well defined porting layer. The more features we add the harder it is to port.

The VM layer is the hardest one to add features to as it’s not portable by definition. Each platform has its own  
VM and thus we need to add changes to every single platform. This is error prone and can produce inconsistencies.

E.g. we picked up `SimpleDateFormat` based on code contribution from a developer. It’s natively implemented on  
Android so this created inconsistency between our iOS implementation of `java.text.SimpleDateFormat` and the  
Android version.

The solution was to place our version in the `com.codename1.l10n` package and thus provide a portable/consistent  
version.

Consistency proved itself superior to functionality in this case, placing functionality in the `com.codename1` package  
space meant we could reuse/enhance and improve on the original implementation.

### Compliance

Someone who didn’t go thru a Java compliance process would assume implementing the full VM would make compliance  
easier. This isn’t the case.

Java compliance is comprised of tiers from lower spec VM’s for embedded/mobile upwards. The full JDK compliance  
process is hard, extensive and might not be available for mobile devices…​

We would want to reach compliance with a Java standard and the sensible goal is to aim low. At a VM level that  
is in the JavaME 8 camp and not at the full Java SE specification.

That would mean far fewer tests to pass and since every test suite needs to run on all supported platforms this  
matters a lot.

### In Detail

Above I listed some of the high level problems related to supporting the full JVM but lets go into some more details.

#### Reflection/IoC

As mentioned above mobile code is usually obfuscated and statically linked.

Both of these technologies are designed for decoupling implementation from other logic (e.g. UI, tests etc.). Since  
all the code is packaged this isn’t really useful at runtime.

For some cases it might be practical to offer a bytecode manipulation solution during compilation that will allow  
some injection functionality. We are gathering feedback on such requirements for a potential future feature.

#### Arbitrary JARs

The ability to include an arbitrary JAR into the project is a common request. That’s problematic as we can’t  
check such JARs and see that they don’t use features that we don’t support. They might work in the simulator and even  
Android but then fail on iOS or JavaScript…​

In theory we could run such tests but in most cases they would fail as most real world JARs use files, networking,  
& countless other features.

#### Maven/Gradle

A few developers complained about the use of the antiquated Ant build system in favor of Maven/Gradle.

We’ve made several attempts at moving to both and eventually abandoned those attempts as both tools aren’t  
nearly as good as ant.

  * Both Gradle and Maven are MUCH slower than Ant

  * Their key feature is dependency management which wouldn’t be helpful due to the arbitrary JAR limits

  * They don’t support cn1libs

Google’s Android team chose to go with a Gradle build process. This resulted in a painfully slow build process for  
Android development that provides very limited benefits.

#### java.net API’s

The `java.net` API’s are very elaborate and layered. All the networking code is implemented on top of sockets which  
are modeled according to typical POSIX sockets. The problem is that most mobile platforms don’t have “proper” POSIX  
sockets and when they do they might have some issues associated with them (e.g. iOS).

It’s impossible to implement `java.net` in a compliant way while still working correctly on devices!

In the future we might introduce a higher level abstractions that implements some common use cases of `java.net`  
but aren’t compliant. This would just mean changing the package name for 99% of the code to get it to work.  
It’s the 1% of unique functionality that is problematic.

#### java.io.File

Mobile devices don’t have filesystems in the same way that desktops do. There are areas to you are restricted to  
and apps are typically “isolated” from one another. We might provide a compatibility migration API similar to the one  
we might include for `java.net` code.

#### NIO

There are several pieces of NIO but the most applicable piece is probably buffers which allow direct memory/filesystem access.  
We don’t really need NIO since we don’t keep secure access to the native code, there is no need since we already  
are in native code on iOS.

Having said that it might still be useful to add these to native interfaces especially when running on Android or  
other JVM platforms. We don’t have any short/long term plans for doing this as of this writing but it’s possible.

The main use case for NIO is IO performance, our current recommended strategy is to use native code for such cases  
which alleviates the need for NIO.

#### String.split()

This is a tough one. I love `String.split()` and would love to have it as part of the “official” API. But it has two major  
problems that are currently holding it back: It’s in String & it’s complex.

`String` is a core class which means we can’t strip it from the JVM when compiling an iOS app. If we include `split()`  
the cost of that additional method (and it’s complex regex parsing engine) will apply to all apps whether they use  
it or not.

That seems like a fair price but the problem is far worse…​

Since the implementation for iOS will be developed by us and not by Oracle/Google it will likely differ in subtle ways.  
Simple things like `split(";")` will probably work the same but complex calls might fail on iOS and succeed elsewhere.

The worst bugs are the ones that happen on device and don’t happen on the simulator, they are hard to track and  
painful. Any minute saved by using `String.split()` instead of our `regex` package or `StringUtils` class could be wiped  
by weeks of tracking a device specific bug. By keeping the API small we can assure that it is more consistent  
across the various devices we support.

### Final Word

We try to add things to the VM’s when possible, it’s non-trivial because we have to do this for every one of the VM’s.

When we implement things in the VM level caution and a conservative stance are key. These promote portability  
and code reuse. That means we only need to do a task once instead of 4x.

Having said that we are always looking at the places where we can improve and help migrate code. The date/timezone  
classes need work, `Number` etc. are all things we’d love to add/improve in the VM’s given the resources.

We’d also like to add a standardized alternative to common reflection use cases based on static bytecode manipulation.

If you have feedback on these things and how we can improve the supported API we’d love to hear it. Feel free to  
post questions/thoughts in the comments. We’d also love RFE’s with things that you are missing in Codename One  
but we want you to justify that enhancement. Specifically don’t ask for something because you want it but rather  
explain the problem that can’t be solved or is awkward to solve without this.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Sanny Sanoff** — September 17, 2016 at 8:48 am ([permalink](/blog/why-we-dont-support-the-full-java-api/#comment-22763))

> Sanny Sanoff says:
>
> > how we can improve the supported API we’d love to hear it
>
> Please have the suggestions in [http://pastebin.com/cYcKaedD](<http://pastebin.com/cYcKaedD>)  
> (apart from gc-related stuff which I just saw)
>



### **Shai Almog** — September 18, 2016 at 4:03 am ([permalink](/blog/why-we-dont-support-the-full-java-api/#comment-23115))

> Shai Almog says:
>
> You’ve submitted pull requests in the past so why not submit pull requests here?  
> It’s a bit hard to read and some things seem redundant e.g. Integer.SIZE?
>
> Notice that for all of these changes to take effect you need to change them in CLDC so they map to the compile classpath too. Then you need to add them to the other ports where applicable.
>
> PrintStackTrace won’t work with that implementation as far as I can tell…
>
> The timezone fix is interesting, did you test it? We have a couple of timezone related issues we need to address.
>



### **BENSALEH ZainElabidine** — May 2, 2018 at 1:00 pm ([permalink](/blog/why-we-dont-support-the-full-java-api/#comment-23980))

> BENSALEH ZainElabidine says:
>
> Hi, i have a java API that encrypte passwords from desktop application.With this api, i can use a database that was generated by FOSUser bundle which he use Sha512 encrypt…  
> I hardly make the api in the desktop application, now in my Mobile app , i need to use the API , so i need to make the equivalente of that api in my codenameone app.  
> and i need the equivalente of those class in codenameone if there is:  
> i searched for the Base64 and i com.codename1.util.Base64 but i need those one too:  
> import java.security.MessageDigest;  
> import java.security.SecureRandom;  
> this is my api in github [https://github.com/zain17/F…](<https://github.com/zain17/FOSJCryptAPI>)  
> And thank you very much for your help 🙂
>



### **Shai Almog** — May 3, 2018 at 6:02 am ([permalink](/blog/why-we-dont-support-the-full-java-api/#comment-23797))

> Shai Almog says:
>
> These are available in the bouncy castle cn1lib. SecureRandom is under a different package name “javabc”.  
> MessageDigest is problematic as I explained here: [https://stackoverflow.com/q…](<https://stackoverflow.com/questions/50135726/package-in-java-s-equivalent-in-codenameone>)
>



### **Martin Grajcar** — November 11, 2018 at 3:58 pm ([permalink](/blog/why-we-dont-support-the-full-java-api/#comment-24100))

> Martin Grajcar says:
>
> Concerning reflection, I have some 300 generated classes, which I need to create by name (they’re used for parsing and formatting a stupid EANCOM-like format). So I generated a huge switch like `case “StupidName1”: return new StupidName1();` and a `Map<class<?>, String>` as a `Class#getSimpleName` replacement. It seems to work, at least in Android. Can I expect it to work everywhere? Should I expect problems?
>



### **Shai Almog** — November 12, 2018 at 4:20 am ([permalink](/blog/why-we-dont-support-the-full-java-api/#comment-24002))

> Shai Almog says:
>
> It should work fine in iOS too. We use that trick as well for some cases.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
