---
title: Property Cross Revisited
slug: property-cross-revisited
url: /blog/property-cross-revisited/
original_url: https://www.codenameone.com/blog/property-cross-revisited.html
aliases:
- /blog/property-cross-revisited.html
date: '2016-05-23'
author: Shai Almog
---

![Header Image](/blog/property-cross-revisited/property-cross-new.jpg)

[PropertyCross](https://github.com/codenameone/PropertyCross) is one of our newer demos, due to that there was  
relatively very little work needed to modernize it and this resulted in a stunning improvement over the existing  
demo. During that process we also discovered a small regression due to changed in the web service we relied on.

**Check the live version running on the right hand side thanks to the power of the Codename One JavaScript port!**

Property Cross uses the nestoria webservice to list houses (properties) for sale in the UK. The webservice is a  
typical JSON based web service with the common use cases of searching, listing, thumbnail view, favorites etc.

The demo is specifically designed to be very simple and use caching/location to enhance the native app experience  
as opposed to just using a web app. The core of this demo was developed to help compare cross platform tools  
and showcase their differences.

Our implementation of property cross is 100% portable and contains no platform specific or native code, it  
includes 630 well commented lines of code (including imports and boilerplate) with no extra XML’s or special  
designs. This makes the Codename One implementation the smallest one we can find in the native tools  
implementations (in some cases 5x smaller).

In fact Codename Ones implementation is so terse it is smaller than most HTML5 implementations despite  
JavaScripts famed terseness.

This is despite the fact that Codename One implements some functionality that isn’t specified such as  
infinite scrolling etc…​

### What Changed

To modernize the demo we started by updating the syntax to use Java 8 features such as lambdas, diamond operator  
etc.

We then migrated the code to use the new [Toolbar API](https://www.codenameone.com/javadoc/com/codename1/ui/Toolbar.html)  
& updated the syntax to use the terse version of the layout code.

We changed the back button behavior to use the `Toolbar` which looks great but the biggest effect was in a  
small change to use the native thin fonts. It really made the app feel more native.

Other than that we used the new material icon fonts for the star/favorite functionality which makes the app both  
more adaptable and more attractive.

### The Source

Check out the full source code for the demo in the  
[github repository for the PropertyCross demo](https://github.com/codenameone/PropertyCross).

This demo will be integrated into the upcoming new project wizards in the various IDEs.

### Up Next

I was working on a couple of more challenging apps/demos but ran into time constraints, I’d hope to tackle the  
bigger fish as we move forward but I’m not sure if this will be feasible in the next couple of weeks with the current  
workload in place.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
