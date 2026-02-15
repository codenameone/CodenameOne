---
title: Comparing PhoneGap/Cordova and Codename One
slug: comparing-phonegap-cordova-and-codename-one
url: /blog/comparing-phonegap-cordova-and-codename-one/
original_url: https://www.codenameone.com/blog/comparing-phonegap-cordova-and-codename-one.html
aliases:
- /blog/comparing-phonegap-cordova-and-codename-one.html
date: '2016-06-27'
author: Shai Almog
---

![Header Image](/blog/comparing-phonegap-cordova-and-codename-one/compare-to-cordova.jpg)

Last time around we compared [Codename One to Xamarin](/blog/comparing-xamarin-and-codename-one.html) and this  
time around I’d like to compare [Codename One](/) to what is probably the market leader: PhoneGap/Cordova. If Xamain  
is big then Cordova is huge, it is so prevalent that it is often the default assumption when people mention cross  
platform today.

In fact, one of the big problems we had when describing Codename One was distinguishing it from HTML5 based  
solutions like Cordova.

One of the things we’d like to clarify before delving into the comparison is that our goal isn’t to proclaim Codename One  
as the “end all” of WORA…​  
We are biased so the comparison might be flawed. However, we think Cordova/PhoneGap are remarkably  
innovative tools that changed the marketplace significantly. The goal of this comparison is to highlight the  
differences/tradeoffs of each solution. In fact we rather like Cordova and even offer some  
[Cordova compatibility](/blog/phonegap-cordova-compatibility-for-codename-one.html) in Codename One…​

We were debating a lot on whether we should include Ionic in this comparison and decided to mostly skip it for now.  
We mention it and related frameworks in the comparison in a few points mostly because PhoneGap is usually used  
with such frameworks. Unfortunately because of the number of JavaScript frameworks and tools built on top  
of PhoneGap it’s just impractical to single out each and every one of them.

We might do a proper discussion of Ionic as I think it’s a very interesting solution that is very similar to Codename One  
in some regards. Most of the things we discuss about PhoneGap/Cordova apply to ionic too.

### Background

PhoneGap was started in the iPhoneDevCamp hackathon, this highlights the genius behind the solution:  
the underlying idea is trivial. PhoneGap is in-effect a **very** simple solution which is a pretty good thing.

PhoneGap places a single web view UI within an app which it then uses to display HTML. It exposes a set of  
JavaScript API’s to provide access to native functionality e.g. camera, filesystem etc.

It also includes a rich set of plugins that provide quite a few capabilities, in fact we just ported one of those  
great plugins to add [bluetooth support to Codename One](/blog/bluetooth-support.html)…​

Adobe purchased Nitobi who made PhoneGap and open sourced the project thru Apache as the Cordova project.  
Both PhoneGap and Cordova are very similar with PhoneGap offering some pretty interesting features on top of  
the core open source product such as PhoneGap build.

Because Cordova/PhoneGap is so simple it was adopted throughout the industry practically all major enterprise mobility  
tools such as Worklight rely on it for their client side UI. It’s become a synonym for the idea of packaging HTML  
as an application and even tools that are not using Cordova internally are often referred to as PhoneGap or Cordova.

In fact even we announced [PhoneGap/Cordova support](/blog/phonegap-cordova-compatibility-for-codename-one.html)  
a while back and it didn’t take much to implement that…​

### At a Glance

Category | PhoneGap/Cordova | Codename One  
---|---|---  
Language |  HTML, CSS & JavaScript |  Java  
Packaging |  Packaged code optionally obfuscated |  Compiled binaries  
VM Ownership |  Device/OS vendor |  Codename One  
Cloud Build |  Yes thru PhoneGap build |  Yes  
Web Deployment |  Yes |  Yes  
Widgets |  HTML/Lightweight |  Lightweight  
Size Overhead |  Depends on framework |  Small  
  
### In Detail

#### Language

We’ve always said that if you love doing client side HTML/JavaScript for mobile devices then PhoneGap/Cordova  
is probably the right tool for you. HTML5 has come a very long way and is no longer as restrictive as it used to be  
especially with recent iOS updates.

However, Java is still a formidable opponent. Unlike HTML/JavaScript, Java is strict, statically typed and compiled.

JavaScript isn’t the most intuitive language to interface into native code. Native concepts such as threads or even  
binary data don’t map in a natural way to the higher level functions of JavaScript. This isn’t the case for Java which  
is literally the native language of Android and maps rather well to C/Objective-C/Swift concepts.

With modern Java 8 semantics Java is also quite terse and should be comparably elegant to JavaScript in most  
regards.

#### Packaging

PhoneGap applications are just zipped into the standard OS distribution. A complaint some PhoneGap developers  
have is that hackers in some markets unzip their apps and take the HTML/JavaScript/CSS etc. into their own  
app. This allows them to sell the app within the standard markets as if it was their own. Such scams are  
quite common and very hard to catch/enforce.

This is not feasible in Codename One where the source code is compiled together and obfuscated by default  
on Android making it even more “secure”. A Codename One application can be decompiled but this would be  
a far harder process than doing the same for a PhoneGap/Cordova application.

#### VM Ownership

Java is the native platform for Android whereas JavaScript/HTML are supported everywhere by the OS vendor.

This brings about some interesting situations, Android completely replaced it’s browser implementation between  
versions of the OS relatively abruptly. This can trigger a situation where shipping applications will start misbehaving  
due to OS changes. This also means that the only way to fix some issues is thru a workaround, there is no central  
authority that can fix an HTML rendering issue or add a missing feature to an old OS.

With Codename One the VM and UI are the responsibility of a single entity. Since the implementation of Codename One  
is at a lower point in the porting stack most of the relevant code can be ported/fixed or worked around by Codename One  
itself. This means that if a low level reproducible failure happens, Codename One has the ability to fix it whereas  
PhoneGap developers would need to workaround it.

#### Cloud Build

PhoneGap Build supports building native applications via the cloud which is a wonderful approach.  
Codename One works in the same way but unlike PhoneGap build, Codename One is built around that  
approach as a basic expectation.

PhoneGap build works with a set of pre-determined plugins whereas Codename One is more flexible with  
it’s support for native code and 3rd party libraries.

#### Widgets

Codename One uses lightweight widgets to do it’s rendering. Arguably HTML can be considered lightweight  
as well but it is often not the case.

One of the core powers of HTML has been it’s complex support for dynamic reflows, this allows positioning  
components using a very elaborate box model. However, this power is also the source of HTML’s greatest performance  
challenges. Some HTML frameworks choose to position elements absolutely and lay them out thru code  
logic, which is pretty close to what Codename One does in it’s layout managers. However, this forces the  
JavaScript developer into a custom environment that won’t “play nicely” with everything else within the ecosystem.

Frameworks like Ionic have taken up the lightweight approach to creating native “themes” in a similar way to  
Codename One. This provides Ionic with many of the advantages Codename One enjoys but also some of the  
drawbacks/advantages inherent from layering on top of Cordova.

One of the core capabilities of Codename One is in embedding native widgets directly into the app. This is demonstrated  
in Codename One thru the native Google Maps support and other such capabilities. Since embedding an OS  
native widget into HTML is “problematic” that level of platform extension can’t be accomplished in Cordova.

#### Size Overhead

Cordova and Codename One can be used to create very small applications. In fact Cordova can be even smaller  
than Codename One in the hands of a skilled developer.

However, using Cordova without a JavaScript framework/tooling is more challenging and less common today.  
These tools include their own overhead which is rather extensive in some cases. E.g. Ionic is specifically well  
known for producing very large application binaries.

### Property Cross Comparison

The PropertyCross demo was built as a tool that allows us to compare two cross platform frameworks, as such  
there are versions of the demo for many such platforms. You can check out details of the Codename One  
implementation [here](/blog/property-cross-revisited.html). The github repository for this demo  
is [here](https://github.com/codenameone/PropertyCross/).

Since PhoneGap/Cordova is mostly an infrastructure tool there are several implementations that support PhoneGap  
Build. E.g. [jquerymobile](https://github.com/tastejs/PropertyCross/tree/master/jquerymobile),  
[ionic](https://github.com/tastejs/PropertyCross/tree/master/ionic),  
[sencha touch](https://github.com/tastejs/PropertyCross/tree/master/senchatouch2) and too many others to even  
count…​

With so many variations how can we properly compare the two frameworks?

There are several things we can gleam from property cross by reviewing all of the above:

  * Codename One has 2 file types that aren’t configuration or build scripts: res file (resources) & Java files.  
PhoneGap solutions use JavaScript, HTML & CSS.

  * Codename One doesn’t have splashscreens or multiple icon sizes…​ Those are generated automatically unlike  
the PhoneGap based solutions where you need dozens of resource files even without tablet support!

  * Codename One is terse. Ionic, is one of the smallest implementations. It includes more than 600 lines of  
undocumented code spread across JavaScript and HTML (not counting angular, other JS libraries or CSS).  
The Codename One implementation has 623 lines of code out of which over 150 lines are comments and 40  
lines are import statements…​  
**Who says Java is verbose?**

### Final Word

Notice that Codename One can  
[embed PhoneGap/Cordova code](/blog/phonegap-cordova-compatibility-for-codename-one.html) into  
Codename One applications. Would that make it superior to just using Cordova directly?

Not necessarily. Cordova is a mature, widely supported solution. If you like working with HTML/JavaScript  
then it’s hard to compete with that…​

However, if you want to use proper Java to develop your app and care more about the security of your code  
then Codename One has advantages.

If you think we misrepresented Codename One or PhoneGap/Cordova in any way let us know in the comments.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — June 28, 2016 at 8:46 pm ([permalink](/blog/comparing-phonegap-cordova-and-codename-one/#comment-21514))

> bryan says:
>
> CN1 vs React Native ?
>



### **Shai Almog** — June 29, 2016 at 4:19 am ([permalink](/blog/comparing-phonegap-cordova-and-codename-one/#comment-22451))

> Shai Almog says:
>
> Agreed. I already wrote something but it wasn’t a real comparison: [http://www.codenameone.com/…](<http://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html>)
>



### **bryan** — June 29, 2016 at 4:34 am ([permalink](/blog/comparing-phonegap-cordova-and-codename-one/#comment-22920))

> bryan says:
>
> ah.. yes I remember now. Would be good to get a feature for feature comparison.
>



### **José Ignacio Santa Cruz** — June 29, 2016 at 12:39 pm ([permalink](/blog/comparing-phonegap-cordova-and-codename-one/#comment-22950))

> José Ignacio Santa Cruz says:
>
> Seems you haven’t seen [http://microsoft.github.io/…](<http://microsoft.github.io/ace/>) yet.  
> Nice comparison, I used CN1 in the early LWUIT days. Due to the lack of Android support those days I went native and finally hybrid using jQuery mobile. Today I’m using Ionic, but the main reason is because of how fast can my team deliver an almost ready product, making HTML is fast and easy. JavaScript developers are not so difficult to find, and if you have design issues, giving the CSS to a designer is no big deal, he/she won’t have to learn anything new. It needs tweaking and tooling and putting brain on the deployment process to get small sized and small memory footprint apps, just like any other developing strategy chosen.  
> Not better or worse, just different, aimed for different needs.
>



### **Shai Almog** — June 29, 2016 at 1:20 pm ([permalink](/blog/comparing-phonegap-cordova-and-codename-one/#comment-22927))

> Shai Almog says:
>
> Thanks for the headsup, I actually did see this when it was announced and it totally slipped my mind. I wonder how well it works with the native HTML rendering?
>
> Codename One is totally different from LWUIT by now, we even have some CSS support. Although the “giving CSS to a designer” line is a bit… I’ve worked a lot with designers and never got anything remotely close to a usable CSS snippet from them. The best they could offer is the CSS photoshop produces for a layer which isn’t much…
>
> In mobile where the design needs to be aware of screen size, density, orientation, font constraints etc. the design requires proper programming skills. I’ve yet to see a designer produce something half decent for a website and I can’t imagine one producing something workable for cross platform mobile devices…
>



### **David Hinckley** — July 1, 2016 at 11:15 am ([permalink](/blog/comparing-phonegap-cordova-and-codename-one/#comment-22784))

> David Hinckley says:
>
> I am a Java/Android developer and was asked to create an Android watch application to be added to an existing Cordova project. I was hoping that Cordova could receive requests from the watch through Android Wear messaging, but our Cordova expert says that Cordova can only receive Wear messages, if the Cordova app is currently in the foreground. It appears Cordova can’t run in the background without writing native code. If this is true, it may be important when considering Cordova. So, I ended up writing the native Android Wear message receiving piece for Cordova.
>



### **amikeliunas** — July 6, 2016 at 4:58 am ([permalink](/blog/comparing-phonegap-cordova-and-codename-one/#comment-22410))

> amikeliunas says:
>
> Was that a comment, concern, or just venting out?
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
