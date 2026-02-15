---
title: PhoneGap/Cordova Compatibility For Codename One
slug: phonegap-cordova-compatibility-for-codename-one
url: /blog/phonegap-cordova-compatibility-for-codename-one/
original_url: https://www.codenameone.com/blog/phonegap-cordova-compatibility-for-codename-one.html
aliases:
- /blog/phonegap-cordova-compatibility-for-codename-one.html
date: '2015-11-11'
author: Shai Almog
---

![Header Image](/blog/phonegap-cordova-compatibility-for-codename-one/cordova.jpg)

We just released the first version of the open source  
[CN1Cordova project](https://github.com/codenameone/CN1Cordova) on github. This  
means you can take a common Cordova/PhoneGap app, import it into NetBeans and  
build a native app using our cloud build servers without any changes!  
Before we delve into the exact process of converting an app lets start by reviewing the exact benefits  
PhoneGap/Cordova developers can gain from Codename One. You can also check out the video tutorial and  
slides below. 

#### Why Would I Want To Convert?

What Advantages Does Codename One Hold For PhoneGap/Cordova Developers?

____Build Cloud On Steroids

Codename One provides a build cloud similar to PhoneGap build only far more advanced.  
It can translate Java bytecode into native thus allowing you to write a great deal of your “native” API  
in Java instead of writing it again and again for every platform. 

____Better Native Code Support

You can and should write your “native” code in Java thus removing  
the need to write Objective-C/Swift code when building a plugin. However, you can still use  
[cn1lib’s](/cn1libs.html) and  
[native interfaces](/how-do-i---access-native-device-functionality-invoke-native-interfaces.html)  
to implement pretty much anything using true native calls to Objective-C, Dalvik/ART runtimes. 

____Better Protection Of IP

Cordova/PhoneGap apps are effectively just a set of HTML/JavaScript & CSS files.  
Since native packaging is “just a zip file”, it triggered a cottage industry of unzipping such applications and reselling them thru  
other accounts/stores.  
Its much harder to do this for compiled applications, making the Java code in Codename One more opaque to the  
casual hacker. If your application contains sensitive logic you can code it in Java for extra security. We are also considering  
a hardened version of the PhoneGap integration that will encrypt the files within making the process even more secure. 

____IDE Integration Java – JavaScript – HTML

NetBeans has remarkable JavaScript & html/css support. It  
also supports all the common web frameworks such as react, angular etc. while being completely free…  
NetBeans is also one of the best Java IDE’s in the market and having one single all encompassing environment  
is a huge benefit. 

____Easy, Doesn’t Require A Mac, Automates Certificates/Signing

Codename One’s environment works on Mac, Linux & Windows  
without a problem. One of the “killer features” of Codename One is the  
[certificate wizard](/blog/ios-certificate-wizard.html) that makes the normally nightmarish  
process of signing an iOS app, bare-able. 

____Migration To Java

If your team prefers Java you can move to a Java application in  
stages or even integrate pieces that are written in Java with Cordova elements. 

#### What Are The Limitations?

The first version of the converter doesn’t currently translate plugins, it does have some builtin plugins for features  
such as camera etc.  
  
This is something we intend to address soon so the translation process will be smoother. 

Since the native implementation of plugins in Codename One differs a great deal from native plugins in  
PhoneGap/Cordova there will be some manual work required to migrate plugins. Thankfully since most  
plugin functionality is already supported in the core Codename One Java API, this is relatively trivial work  
for most cases. 

Currently the browser component used in Codename One is based on the JavaFX browser component which  
is pretty awful. Its based on webkit but has many limitations in regard to full HTML5 compliance and doesn’t  
correctly specify things such as user-agent. It does work with most JavaScript frameworks correctly though.  
We have plans to replace that component with a more mature browser component based on chromium  
if there is enough community interest to justify the effort. 

#### Why Didn’t You Do This Sooner?

We were (and still are) concerned about confusion. Codename One uses a rather elaborate architecture of  
converting bytecode to native code. This is coupled with an OpenGL ES based rendering pipeline and native  
widget mixing. Yet despite that fact and the fact that we specifically wrote in the top bar of the page that  
Codename One is NOT an HTML5 solution we still got the feedback of “well another HTML5 framework”.  
Our concern was that if we would include PhoneGap this confusion and ambiguity would just grow and hinder  
our ability to differentiate Codename One in a market dominated by 3 core ideas (HTML5, native & porting tool)  
as something that doesn’t fit in either one of those 3 pillars. 

However, a few months ago we started adding things like  
[JavaScript support](/blog/javascript-port.html) which uses  
[TeaVM](http://teavm.org/) and our own set of complex libraries to facilitate  
WORA (Write Once Run Anywhere). Which makes our offering even more complex…  
Furthermore, at this point with so many other projects making noise about cross  
platform mobile development, we came to the conclusion that “avoiding confusion” isn’t the right  
strategy. 

#### Does This Mean a Shift In Focus For Codename One?

**No!**  
Our core competitive advantage has always been our huge API and client UI libraries, even with the support  
above it is still one of the key advantages.  
Playing with PhoneGap and some other tools these past few months has further cemented our conviction  
that despite the fact that almost 4 years have passed since launching Codename One we are still in a league  
of our own. 

### Porting A Cordova/PhoneGap app To Codename One

Pre-requisites for this are: [NetBeans IDE](http://netbeans.org/) with the [Codename One Plugin installed](/getting-started/),  
JDK 8 (not Java JRE.. JDK!) & [Apache Ant](https://ant.apache.org/bindownload.cgi).  
Download [cn1-cordova-tools.zip](https://github.com/codenameone/CN1Cordova/blob/master/cn1-cordova-tools.zip?raw=true)  
and extract. 

From the terminal or command prompt 
    
    
    $ cd cn1-cordova-tools
    $ ant create -Dsource=/full/path/to/cordova/app

**Note:** make sure you are using Java 8 and if not make sure your JAVA_HOME environment variable points  
at the Java 8 home directory otherwise you might get an `UnsupportedClassVersionError`.  
Also make sure you are using the full path to the Cordova app and not a short path or this won’t work (no relative  
paths etc.).   
This will create Netbeans Project inside the cn1-cordova-tools directory with settings (package id and name)  
matching the app specified in the `-Dsource` argument. The contents of the app’s  
`www` directory will be copied to the project’s `src/html` directory. 

You can open this project up in NetBeans to start working on it. You will be able to run and debug the Java  
source files in the project. To send a cloud build or change project configuration just use the right click menu  
and select the correct options. 

As mentioned above plugins won’t be imported. If the app has plugins installed, you’ll see a warning printed.   
Future versions should add support for this in some form (e.g. replacing well known plugins with Codename One plugins).  
There is already support in Codename One for developing Cordova plugins and distributing them as cn1libs  
(Codename One’s native library/plugin format). We will publish the instructions for this as the integration matures. 

**[Codename one Cordova/PhoneGap Support](//www.slideshare.net/vprise/codename-one-cordovaphonegap-support "Codename one Cordova/PhoneGap Support") ** from **[Shai Almog](//www.slideshare.net/vprise)**
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **sao** — November 12, 2015 at 10:33 am ([permalink](https://www.codenameone.com/blog/phonegap-cordova-compatibility-for-codename-one.html#comment-22464))

> sao says:
>
> This is great feat.
>
> Afam Okonkwo


### **shannah78** — November 12, 2015 at 3:57 pm ([permalink](https://www.codenameone.com/blog/phonegap-cordova-compatibility-for-codename-one.html#comment-22473))

> shannah78 says:
>
> Nice video and slides, Shai. One note: You should be able to use relative paths for the -Dsource parameter. You just can’t use the “~” tilde shorthand for home directory. Paths like “../myapp” or “myapp” should work though.


### **Valeriy Skachko** — November 29, 2015 at 2:18 pm ([permalink](https://www.codenameone.com/blog/phonegap-cordova-compatibility-for-codename-one.html#comment-21552))

> Valeriy Skachko says:
>
> Hi! I have some strange error.
>
> 1) Create cordova app – ok  
> 2) Porting A Cordova/PhoneGap app To Codename One -ok  
> 3) Launch on emulator – ok  
> 4) Send build – Updating libraries – ok, build succesfull  
> 5) After that if i run on emulator i see next:error: cannot find symbol  
> theme = UIManager.initFirstTheme(“/theme”);  
> symbol: method initFirstTheme(String)  
> location: class UIManager  
> 6) Now i cant run on send build after Updating libraries


### **Shai Almog** — November 29, 2015 at 2:47 pm ([permalink](https://www.codenameone.com/blog/phonegap-cordova-compatibility-for-codename-one.html#comment-22549))

> Shai Almog says:
>
> That’s odd maybe there was a regression here.  
> Copy over the jar files on top of the existing jar files (both JavaSE.jar and the jars in the lib directory).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
