---
title: Camera Demo
slug: camera-demo
url: /blog/camera-demo/
original_url: https://www.codenameone.com/blog/camera-demo.html
aliases:
- /blog/camera-demo.html
date: '2016-05-09'
author: Shai Almog
---

![Header Image](/blog/camera-demo/camera-demo-blog.png)

With the 3.4 release we discussed the process of modernizing the demos and the first one we picked  
for this task is the [camera demo](/demos-Camera.html) which is probably the easiest one of all the demos…​  
The demo is trivial and doesn’t really demonstrate anything other than capturing and showing an image captured  
from the camera/retrieved from the gallery but this is where it gets interesting. It even works in the  
JavaScript port so you can even [run this in the browser and it works as you’d expect](/demos/CameraDemo/)!

Notice that since browsers don’t have anything quite like a “gallery” that feature won’t be very useful but capture  
works really well.

### The Source

Check out the full source code for the demo in the  
[github repository for the CameraDemo](https://github.com/codenameone/CameraDemo) notice that  
[as we announced yesterday](/blog/java-8-switch-new-preferences-demo-structure.html) we are  
moving the demos to separate repositories and will retire the monolithic  
[codenameone-demos](https://github.com/codenameone/codenameone-demos) repository. This will allow  
us to be more nimble and will also simplify the process of working with these demos.

This demo will be integrated into the upcoming new project wizards in the various IDEs. This plays into the  
move to Java 8 in the plugins which [we also announced yesterday](/blog/java-8-switch-new-preferences-demo-structure.html).

### Moving Forward

There are **many** demos in the [codenameone-demos](https://github.com/codenameone/codenameone-demos)  
repository and some outside of it. Most of them are out of date and “abandoned” we’ll try to make this a weekly  
segment of moving demos to the newer API’s and refining them. Some of these demos will take more effort (e.g Kitchen Sink)  
but others should be simpler.

Ideally all demos should be included in the IDE plugins and should be accessible by everyone easily.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
