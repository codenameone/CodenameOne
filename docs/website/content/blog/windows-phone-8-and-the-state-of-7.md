---
title: Windows Phone 8 And The State Of 7
slug: windows-phone-8-and-the-state-of-7
url: /blog/windows-phone-8-and-the-state-of-7/
original_url: https://www.codenameone.com/blog/windows-phone-8-and-the-state-of-7.html
aliases:
- /blog/windows-phone-8-and-the-state-of-7.html
date: '2013-04-01'
author: Shai Almog
---

![Header Image](/blog/windows-phone-8-and-the-state-of-7/hqdefault.jpg)

  

A preliminary Windows Phone 8 build has been available on our servers for the past couple of days. We differentiate between a Windows Phone 7 and 8 version by a build argument that indicates the version (win.ver=8) this will be exposed by the GUI in the next update of the plugin. But now I would like to discuss the architecture and logic behind this port which will help you understand how to optimize the port and maybe even help us with the actual port.  
  
  
  
The Windows Phone 7 and 8 ports are both based on the XMLVM translation to C# code, we picked this approach because all other automated approaches proved to be duds. iKVM which seems like the most promising option, isn’t supported on mobile so that only left the XMLVM option. 

  
The Windows Phone 7 port was based on XNA (3d C# based API) which has its share of problems but was more appropriate to our needs in Codename One. Unfortunately Microsoft chose to kill off XNA for Windows Phone 8 which put us in a bit of a bind when trying to build the Windows Phone 8 port.  

  
While externally Windows Phone 8  
  
and 7 look very similar, their underlying architecture is completely different and very incompatible. You cannot compile a universal binary that will work on all of Microsoft’s platforms, so just to make order within this mess:  

  *   
Windows Phone 7 – based on the old Windows CE kernel. Allows only managed runtimes (e.g. C# not C++), graphics can be done using XAML or XNA (more on that later.  
  

  *   
  
Windows Phone 8 – based on an ARM port of Windows 8 kernel. Allows unmanaged apps (C# or C++) graphics can be done in XAML or Direct3D when using C++ (but not silverlight). 
  * Windows RT/Desktop – the full windows 8 kernel either for ARM or for PC. They are partially compatible to one another so I’m putting them together. This is actually pretty similar to the Windows Phone 8 port, but incompatible so a different build is needed and slightly different API usage. 

As you understand we can’t use XNA since it isn’t supported by the new platforms, we toyed a bit with the idea of using Direct3D but integrating it with text input, fonts etc. seemed like a nightmare. Furthermore, doing another C++ port would mean a HUGE amount of work!

  
So Codename One is based on the XAML API.  
  
  
Most people would think of XAML as an XML based API, but you can use it from C# and just ignore most of the XML aspects of it which is what we need since our UI is constructed dynamically. However, this is more complicated than it seems.  

  
To understand the complexity you need to understand the idea of a Scene Graph. If you used Codename One you are using a more immediate mode graphics API, where the paint method is invoked and just paints the component whenever its needed. This is the simplest most portable way of doing graphics and is pretty common, its used natively by Android, OpenGL, Direct3D etc. and is very familiar to developers.  

  
In recent years many Scene Graph API’s sprung up, XAML is one of them and so is JavaFX, Flash, SVG and many others. In a Scene Graph world you construct a graphics hierarchy and then let it be rendered, the whole paint() sequence is hidden from the developer. The best way to explain it is that our components in Codename One are really a scene graph, only at a higher abstraction level. Windows/Flash placed the scene graph on the graphics as well, so to draw a rectangle you would just add it to the tree (and remove it when you no longer need it).  

  
This is actually pretty powerful, you can do animations just by changing component values in trees and performance can be pretty spectacular since the paint loop can be GPU optimized.  

  
However, the reality of this is that most developers find these API’s harder to work with (since they need to keep track of a rather complex unintuitive tree), the API’s aren’t portable at all since the hierarchies are so different. Performance is also very hard to tune since so much is hidden by the underlying hidden paint logic.  

  
For Codename One this is a huge problem, we need our API to act as if its painting in immediate mode while constructing/updating a scene! When we initially built this the performance was indeed as bad as you might imagine. While we are not in the clear yet, the performance is much improved…  

  
How did we solve this?  

  
There are several different issues involved, the first is the number of elements on the screen. We noticed that if we have more than 200 elements on the screen performance quickly degraded. This was a HUGE problem since we have thousands of paint operations happening just in the process of transitioning into a new form. To solve this we associate every graphics component with a component and when the component is repainted we remove all operations related to it, we also try to reuse graphics resources such as images from the previous paint operation.  

  
When painting a component in Codename One we normally traverse up the component tree and paint the first opaque component forward (known as painters algorithm) however, since the scene already has the parent component painting it again would result in many copies of the image being within the scene graph. E.g. I have a background image on a form, when painting a translucent label I have to paint the background image within a clipping region matching the label…. In the Windows Phone port we have a special hook that just disables this functionality, this hook alone pushed us over the top to reasonable graphics performance!  

  
We are working on getting additional performance oriented features into place and fixing some issues related to this approach, its not a simple task since the API wasn’t designed with this in mind but it is doable. We would appreciate you taking the time to review the port  
  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
