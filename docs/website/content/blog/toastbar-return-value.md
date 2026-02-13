---
title: ToastBar Return Value
slug: toastbar-return-value
url: /blog/toastbar-return-value/
original_url: https://www.codenameone.com/blog/toastbar-return-value.html
aliases:
- /blog/toastbar-return-value.html
date: '2018-06-25'
author: Shai Almog
---

![Header Image](/blog/toastbar-return-value/generic-java-2.jpg)

Last week I pushed out an enhancement to `ToastBar` that changed the static `showMessage` methods. I made them return the `Status` object instead of `void` which would allow more control of the toast message after it’s shown. Unfortunately, I totally forgot that I can’t do that without breaking some binary compatibility.

In Java return types create a distinct method signature, so even though the language doesn’t allow you to do this:
    
    
    void myMethod() {
    }
    int myMethod() {
        return 1;
    }

This is actually valid in the bytecode level. Furthermore, it’s a used by the JVM to implement Java language features like covariant return types.

So if you used `ToastBar` and compiled against older libraries you might experience issues when building. Make sure to update your client libraries before sending a build by going into Codename One Settings → Basic → Update Client Libs. Then do a clean/build before sending a new build.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
