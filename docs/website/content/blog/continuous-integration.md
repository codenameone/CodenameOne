---
title: Continuous Integration
slug: continuous-integration
url: /blog/continuous-integration/
original_url: https://www.codenameone.com/blog/continuous-integration.html
aliases:
- /blog/continuous-integration.html
date: '2014-12-07'
author: Shai Almog
---

![Header Image](/blog/continuous-integration/continuous-integration-1.png)

  
  
  
  
![Picture](/blog/continuous-integration/continuous-integration-1.png)  
  
  
  

  
  
  
Building enterprise mobile apps can be pretty challenging especially when dealing with rapid changes, prototyping and corporate development requirements.  
  
One of the tools that has really revolutionized this field is Jenkins and related CI tools that allow developers to instantly track failures back to a specific revision of their code commits with respect to QA work. 

Codename One was essentially built for continuous integration since our build servers are effectively a building block for such an architecture. However, there are several problems with that the first of which is the limitation of server builds. If all users would start sending builds with every commit our servers would instantly become unusable due to the heavy load. To circumvent this we are now introducing CI support but only on the Enterprise level which allows us to stock more servers to cope with the rise in demand related to the feature.

To integrate with any CI solution just use our standard Ant targets such as  

  
  
  
  
build-for-android-device  
  
  
, build-for-iphone-device etc. Normally, this would be a problem since the build is sent but since it isn’t blocking you wouldn’t get the build result and wouldn’t be able to determine if the build passed or failed. To enable this just edit the build XML and add the attribute automated=”true” to the codeNameOne tag in the appropriate targets. This will deliver a result.zip file under the dist folder containing the binaries of a successful build. It will also block until the build is completed. This should be pretty easy to integrate with any CI system together with our  
[  
automated testing solutions  
](http://www.codenameone.com/blog/test-it)  
. 

This is just a first piece in what would hopefully be a larger piece of the puzzle e.g. running the automated tests on cloud devices using one of the device hosting services available. We will address those pieces based on demand/requirements from our enterprise users.  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — December 8, 2014 at 3:40 pm ([permalink](https://www.codenameone.com/blog/continuous-integration.html#comment-22236))

> Anonymous says:
>
> Thanks, this is a great addition to Codename One, any chances it will have some basic support to Pro users ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcontinuous-integration.html)


### **Anonymous** — December 9, 2014 at 11:52 am ([permalink](https://www.codenameone.com/blog/continuous-integration.html#comment-24241))

> Anonymous says:
>
> That is unlikely, too much server load.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcontinuous-integration.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
