---
title: Map, String Performance & JavaScript Native
slug: map-string-performance-javascript-native
url: /blog/map-string-performance-javascript-native/
original_url: https://www.codenameone.com/blog/map-string-performance-javascript-native.html
aliases:
- /blog/map-string-performance-javascript-native.html
date: '2015-06-09'
author: Shai Almog
---

![Header Image](/blog/map-string-performance-javascript-native/performance.jpg)

We’ve been spending a lot of times looking at performance for one of our enterprise customers. As part of the investigation  
he came up with an interesting and very important find… He found that `hashMap.get("String")` was much  
slower under the new VM than under the old VM. Its these sort of “simple” finds that help narrow down bigger issues  
in performance that might impact a lot of things in Codename One, since HashMap is so ubiquitous the implications  
of improving it can be huge. 

One of the advantages of the new VM is in the fact that we now also control the full API including the HashMap API and  
can rewrite large portions of it natively for full performance advantage. But it seems that a lot of the things that were  
really affecting performance were in the more “subtle” pieces touched by the `String` class and  
`get` method… 

We moved many small bottlenecks into native code e.g. `toString()`, `String.equals()`,  
`String.hashCode()`, etc…  
But one of the bigger performance improvements came from improving the behavior of the finalizer methods  
which reduces CPU usage and potential stuttering. 

#### JavaScript Native

We’ve made some updates to the developer guide specifically a lot of details related to the  
[JavaScript section](/manual/appendix-javascript.html) and information on the native interfaces  
for JavaScript. This is important if you want to access the JavaScript code directly which means you need to  
regenerate native interfaces for older cn1libs to be compatible. If you don’t do that compilation with such  
libraries can result in a compilation error.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
