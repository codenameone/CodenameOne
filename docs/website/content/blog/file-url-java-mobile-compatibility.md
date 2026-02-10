---
title: File and URL for Better Java Mobile Compatibility
slug: file-url-java-mobile-compatibility
url: /blog/file-url-java-mobile-compatibility/
original_url: https://www.codenameone.com/blog/file-url-java-mobile-compatibility.html
aliases:
- /blog/file-url-java-mobile-compatibility.html
date: '2016-10-19'
author: Shai Almog
---

![Header Image](/blog/file-url-java-mobile-compatibility/port-java-code.jpg)

I explained [why we don’t support the full Java API](/blog/why-we-dont-support-the-full-java-api.html)  
(and the difficulties involved) not so long ago. The logic behind this is solid. However, the utility of porting  
existing Java code to Codename One is also with a lot of merit.

We try to strike a balance between portability, compatibility to the Java API etc. and that is a very delicate  
balance. To improve the situation we created two new classes: `com.codename1.io.File` & `com.codename1.io.URL`.  
They are meant to be drop-in replacements for `java.io.File` & `java.net.URL` to help you port existing code.

This doesn’t mean that every API will work or behave as you expect as the mapping is sometimes counter intuitive  
e.g. `File` works with relative paths which we don’t support. We had some thoughts about the “right way” to  
implement the `URL` API and eventually decided to use our internal synchronous API and not the high level  
`ConnectionRequest` API.

That means that the `URL` API can block the EDT (illegally) and should be used with caution. This makes it  
more compatible with the JavaSE API of the same name. This also means that using URL should be completely  
separate from `ConnectionRequest` and will not block the network thread when you do so.

We’d like to start looking at big ticket Java libraries that people use that we can port to Codename One. So we  
can learn from the process and provide both “best practices” and better support from within.  
If you have a wishlist of a jars you want to use in Codename One let us know and we’ll add them to the consideration.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
