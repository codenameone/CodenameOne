---
title: ParparVM Spreads Its Wings
slug: parparvm-spreads-its-wings
url: /blog/parparvm-spreads-its-wings/
original_url: https://www.codenameone.com/blog/parparvm-spreads-its-wings.html
aliases:
- /blog/parparvm-spreads-its-wings.html
date: '2015-12-08'
author: Shai Almog
---

![Header Image](/blog/parparvm-spreads-its-wings/parparvm-blog.jpg)

We wrote quite a bit about the architecture of the new VM we built for iOS and why we built it. Up until recently  
we always viewed it as a Codename One specific tool. Something that would only be useful for us. We used open  
source because “that is our way” and didn’t give it much thought after that.  
It started to dawn on us recently that this tool could be useful for other developers that might take it in a different  
direction from our original intention. We also came to the conclusion that this might not be a bad idea altogether.  
So we are are effectively launching the Codename One VM as  
[ParparVM](https://github.com/codenameone/CodenameOne/tree/master/vm)  
and it includes a lot of interesting benefits. 

To avoid confusion and complex support overhead we always indicated that we don’t provide support for building  
[Codename One](https://www.codenameone.com/) natively. This made sense back in the  
day when our main support channels were email & the discussion forum. However, now that we are  
focusing support around StackOverflow this shouldn’t be as much of a barrier since it won’t increase  
the “noise”. We can’t guarantee an answer for every question as these things might step out of our  
comfort zone, but we’ll try to do our best as usual. So feel free to ask questions about the VM and native  
compilation on [stackoverflow with the “codenameone” tag](http://stackoverflow.com/tags/codenameone). 

#### Getting Started

The source is available  
[here](https://github.com/codenameone/CodenameOne/tree/master/vm),  
The ByteCodeTranslator and JavaAPI projects are designed as a NetBeans project although it should be  
possible to work with any Java IDE or ant directly. It requires asm 5.0.3 which you can find in the  
[cn1-binaries](http://github.com/codenameone/cn1-binaries) project.  
You can run the translation process using: 
    
    
    java -jar ByteCodeTranslator.jar ios path_to_stub_class:path_to/vm/JavaAPI/build/classes;path_to_your_classes  dest_build_dir MainClassName com.package.name "Title For Project" "1.0" ios none

Once the translation process succeeds you should have a valid xcode project that you can run and use as  
usual. You will need a Mac for this to work.  
The main class name is expected to have a `public static void main(String[])` method and it is  
assumed to reside in the `com.package.name` directory (figuratively, you need to replace  
`com.package.name` with your actual package passed to the translator). 

#### Why Another VM for iOS?

It seems like there are a lot of open source iOS Java VM’s in the field but the reality is that most of them are either  
proprietary or rely on a path that is very risky.  
By translating bytecode to C source code ParparVM is effectively the only VM that we are aware of, that uses  
a 100% supported by Apple approach for Java compatibility. The closest 2nd place would be J2ObjC from  
Google but it isn’t intended as a full VM and actually fills a very different roll from ParparVM. 

XMLVM’s C backend had a similar architecture but the project is no longer actively maintained.  
All other Java VM’s for iOS that are actively maintained use approaches that Apple doesn’t officially support  
such as LLVM code or ARM code. This makes these solutions very fragile to changes made by Apple. E.g.  
[this quote](https://groups.google.com/d/msg/robovm/OnE3moz3d-8/nba0ury5CwAJ): 

> Our work to add full support for iOS 9 in time for its public release was one of the most daunting challenges we’ve faced in our existence 
> 
> Henric Müller

By contrast ParparVM required no code changes to support iOS 9, 64 bit, bitcode or other changes made by  
Apple.  
The core work for ParparVM took us about a month and the VM is trivial by comparison. Trivial in this sense  
is also good, as it means even novices can extend and enhance the VM further without serious compiler  
engineering background. 

#### Taking Action

Check out the [ParparVM](https://github.com/codenameone/CodenameOne/tree/master/vm)  
page in the Codename One project, star/fork it and start playing around with it.  
Let us know what you think and how we can improve the VM’s reach/feature set in the comments below.  
We think we can add a lot of features to the VM as conditional options and thus keep things that  
Codename One doesn’t need as a 3rd party extension that can be turned on at will.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
