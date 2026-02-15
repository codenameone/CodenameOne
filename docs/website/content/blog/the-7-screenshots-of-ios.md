---
title: The 7 Screenshots Of iOS
slug: the-7-screenshots-of-ios
url: /blog/the-7-screenshots-of-ios/
original_url: https://www.codenameone.com/blog/the-7-screenshots-of-ios.html
aliases:
- /blog/the-7-screenshots-of-ios.html
date: '2014-03-11'
author: Shai Almog
---

![Header Image](/blog/the-7-screenshots-of-ios/the-7-screenshots-of-ios-1.jpg)

  
  
  
  
![Picture](/blog/the-7-screenshots-of-ios/the-7-screenshots-of-ios-1.jpg)  
  
  
  

Have you ever noticed how iOS apps start almost instantly in comparison to Android apps?  
  
There is a trick to that. iOS applications have a file traditionally called Default.png that includes a 320×480 pixel image of the first screen of the application. So you are treated to an “illusion” of the application instantly coming to life and filling up with data, this is pretty cool on the surface but is a source to no end of trouble in iOS and becomes a huge hassle. 

Initially this was a pretty clever workaround then Apple introduced the retina display 640×960 so you needed to add a `Default@2x.png` file then it added the iPad, iPad Retina and iPhone 5 (which is slightly higher resolution), to make matters worse iPad apps can be launched in landscape mode so that’s two more resolutions for the horizontal orientation iPad. Overall as of this writing (or until Apple adds more resolutions) we need 7 screenshots for a typical iOS app!

iOS developers literally run their applications 7 times with blank data to grab these screenshots every time they change something in the first form of their application! 

When we started working on Codename One we understood that this will not be feasible, initially we thought we would show a hardcoded screenshot but that wouldn’t be “right” then we came up with a simple idea. We run the application 7 times in the build server, grab the right sized screenshot in our simulator and then build the app!  
  
This means the process of the iPhone splash screen is almost seamless to you… But its not completely seamless.

Every abstraction leaks and this one has quite a few leaks/pitfalls you should be aware of as a developer. 

**  
Size  
**  
  
One of the first things we ran into when building one of our demos was a case where an app that wasn’t very big in terms of functionality took up 30mb!  
  
After inspecting the app we discovered that the iPad retina PNG files were close to 5mb in size… Since we had 2 of them (landscape and portrait) this was the main problem.  
  
The iPad retina is a 2048×1536 device and with the leather theme the PNG images are almost impossible to compress because of the richness of details within that theme. This produced the huge screenshots that ballooned the application.

**  
Mutable first screen  
**  
  
A very common use case is to have an application that pops up a login dialog on first run. This doesn’t work well since the server takes a picture of the login screen so the login screen will appear briefly for future loads and will never appear again.

**  
Unsupported component  
**  
  
One of the biggest obstacles is with heavyweight components, e.g. if you use a browser or maps on the first screen of the app you will see a partially loaded/distorted MapComponent and the native webkit browser obviously can’t be rendered properly by our servers.

The workaround for such issues is to have a splash screen that doesn’t include any of the above. Its OK to show it for a very brief amount of time since the screenshot process is pretty fast. 

On the right hand side of this article you can see the upcoming native Google maps implementation running on Android. There are still bugs there but we are getting there…  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
