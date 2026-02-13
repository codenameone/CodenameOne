---
title: Windows Phone Improvements & Build Screenshots
slug: windows-phone-improvements-build-screenshots
url: /blog/windows-phone-improvements-build-screenshots/
original_url: https://www.codenameone.com/blog/windows-phone-improvements-build-screenshots.html
aliases:
- /blog/windows-phone-improvements-build-screenshots.html
date: '2014-03-25'
author: Shai Almog
---

![Header Image](/blog/windows-phone-improvements-build-screenshots/windows-phone-improvements-build-screenshots-1.png)

  
  
  
  
![Picture](/blog/windows-phone-improvements-build-screenshots/windows-phone-improvements-build-screenshots-1.png)  
  
  
  

A lot of developers have asked us for QR code support on Windows Phone 8, we finally took the plunge with the first attempt here. It seems QR support for ZXing on Windows Phone is WAY behind the level on Android or iOS. 

As part of that work we made some additional Windows Phone improvements including integration with the native image gallery (via the show gallery API in Display), email, SMS API support and more. 

Recently we encountered a problem with a build for one of  
[  
our enterprise customers  
](https://play.google.com/store/apps/details?id=il.co.fooxia.apps.travel2gether)  
where builds would just fail on iOS without even reaching the build stage… This happens when the  
[  
screenshot process on the server  
](http://www.codenameone.com/3/post/2014/03/the-7-screenshots-of-ios.html)  
fails, however we looked thru their code and could see nothing there! It all seemed complex yet not something that should exceed the 20 second timeout per screenshot.

This particular application has a very rich resource file of more than 3.5mb, that is a lot… Our reference to the resource is a soft reference by default and it can be removed at any minute. The developer of the app was aware of this due to previous support incidents and invoked setKeepResourcesInRam(true); in the constructor of the state machine. However, this still caused the resource file to load/unload several times before that code was invoked… Moving it to init vars like this, solved the problem:  
  
protected void initVars() {  
  
setKeepResourcesInRam(true);  
  
}

Notice that this version of the initVars() doesn’t take any arguments and it happens before the constructor or anything… 

To understand why that is you need to understand a few things on the Java language specification, say I have two classes:

class A {  
  
public A() {  
  
print();  
  
}

public void print() {  
  
}  
  
}

class B extends A{  
  
int i = -1;  
  
public B() {  
  
}  
  
public void print() {  
  
System.out.println(i);  
  
}  
  
}

This will printout 0. 

To understand why look at this which is semantically identical to the above B class. The javac compiler effectively translates class level assignment into assignment within the constructor so class B looks like this:  
  
class B extends A{  
  
int i;  
  
public B() {  
  
super();  
  
i = 1;  
  
}  
  
public void print() {  
  
System.out.println(i);  
  
}  
  
}

This is how javac compiles the class so while super (A’s constructor) is executing the variable is still in the default 0 state…

It is in fact an antipattern to invoke an overridable method from the constructor which unfortunately we did a few years ago when we created the original designer, when we noticed that mistake it was already too late to fix so instead we patched it with the initVars() method. That is why you should use one of those two methods when dealing with initialization and should generally not rely on the constructor or default values set to variables.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — March 13, 2015 at 7:17 am ([permalink](https://www.codenameone.com/blog/windows-phone-improvements-build-screenshots.html#comment-22201))

> Anonymous says:
>
> It might work we haven’t tested that
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fwindows-phone-improvements-build-screenshots.html)


### **Anonymous** — March 13, 2015 at 7:17 am ([permalink](https://www.codenameone.com/blog/windows-phone-improvements-build-screenshots.html#comment-22273))

> Anonymous says:
>
> Is it just QR code support or can you also scan conventional barcodes now, UPC, EAN etc.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fwindows-phone-improvements-build-screenshots.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
