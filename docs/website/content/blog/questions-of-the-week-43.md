---
title: Questions of the Week 43
slug: questions-of-the-week-43
url: /blog/questions-of-the-week-43/
original_url: https://www.codenameone.com/blog/questions-of-the-week-43.html
aliases:
- /blog/questions-of-the-week-43.html
date: '2017-02-16'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-43/qanda-friday2.jpg)

Surprisingly the big VM update we had last week didn’t trigger any major regressions but we did break some versioned build behavior with the [string obfuscation feature](/blog/obfuscated-constants.html). Todays update is far more tame and should mostly include minor bug fixes and not to many new/disruptive features.

There were a lot of questions but most of them weren’t broad enough for this forum, so I’ll discuss only two questions and in both cases I’ll talk less about the question and more about the implications.

[Beck](http://stackoverflow.com/users/4930974/beck) asked about [an issue with QR code scanning](http://stackoverflow.com/questions/42082234/littlemonkey-qrscanner-library-build-issue) a while back. This is an issue with a 3rd party library and as a result there wasn’t much we could do. I eventually submitted a pull request to the library but it was ignored and didn’t seem to solve the issue completely. Eventually we decided to patch the library ourselves and updated the cn1lib extensions today.

This highlights the fine line between using our libraries and 3rd parties, we can’t warrant community code but if something drags out too long we try to help. It’s not something we can guarantee though.

[Andrew](http://stackoverflow.com/users/7436930/andrew-snejovski) asked about [the security of Storage](http://stackoverflow.com/questions/42204695/codename-one-storing-sensitive-data) to which [Diamond](http://stackoverflow.com/users/2931146/diamond) gave a great answer. But this does highlight the fact that security isn’t covered in depth anywhere so I can’t just point the developer to the security section of the developer guide…​

This is something we hope to remedy now that with the results of our recent efforts to improve some of the security features in Codename One. So hopefully in the 3.7 timeline we will have a security section in the developer guide.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **beck** — February 17, 2017 at 6:26 pm ([permalink](https://www.codenameone.com/blog/questions-of-the-week-43.html#comment-23278))

> beck says:
>
> Thankyou shai.. much appreciated. It works in older devices but in marshmallow there is a permission denial error. Please have a look [http://stackoverflow.com/qu…](<http://stackoverflow.com/questions/42082234/littlemonkey-qrscanner-library-build-issue>) thankyou
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fquestions-of-the-week-43.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
