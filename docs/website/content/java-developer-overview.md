---
title: "Java Developer Overview"
date: 2015-03-03
slug: "java-developer-overview"
---

# Introducing Codename One

High level overview for Java developers.

1. [Home](/)
2. Introducing Codename One

<iframe width="853" height="480" src="https://www.youtube.com/embed/wSe23eUjLZo?rel=0" frameborder="0" allowfullscreen></iframe>

#### Special Notes For RIM/J2ME Developers

If you have prior experience in LWUIT then Codename One would feel right at home, it is based on the original LWUIT source code and was created by the original creators of LWUIT!  
  
Codename One doesn't provide access to the MIDlet class or any of the RIM/MIDP API's, however all such API's have functional equivalents in the Codename One API's. Instead of using the MIDlet class you use the lifecycle class generated for you by the wizard and work from there.  
Instead of using HttpConnection and the Connector class you should use ConnectionRequest and the NetworkManager class.  
  
There are no JSR's in Codename One since everything is included, Codename One automatically detects the JSR's you use and automatically adds the permissions/JSR's as necessary.  
There is no need to obfuscate since Codename One handles all of the complexities for you!  
  
When working in Codename One you can either use the Codename One GUI builder or handcode your application by using Forms and showing them as necessary.
