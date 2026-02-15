---
title: Bouncy Castle Crypto API
slug: bouncy-castle-crypto-api
url: /blog/bouncy-castle-crypto-api/
original_url: https://www.codenameone.com/blog/bouncy-castle-crypto-api.html
aliases:
- /blog/bouncy-castle-crypto-api.html
date: '2013-06-05'
author: Shai Almog
---

![Header Image](/blog/bouncy-castle-crypto-api/bouncy-castle-crypto-api-1.png)

  
  
  
  
![Picture](/blog/bouncy-castle-crypto-api/bouncy-castle-crypto-api-1.png)  
  
  
  

We’ve got many requests in the past year for a cryptography API, initially we thought about adding something like this to the core but it seems somewhat niche so we decided to wrap up this great open source project and re-package it as a CodenameOne lib.The project is supported on all CodenameOne platforms right out of the box without any changes.  
  
  
  
  
You can download the full source code as well as a compiled binary from a Google code project here:   
[  
https://code.google.com/p/bouncy-castle-codenameone-lib/  
](https://code.google.com/p/bouncy-castle-codenameone-lib/)  
and you can download the compiled binary from here:  
[  
BouncyCastleCN1Lib.cn1lib  
](https://code.google.com/p/bouncy-castle-codenameone-lib/source/browse/trunk/BouncyCastleCN1Lib.cn1lib)  
(since Google removed the download section for new projects). 

  
You can learn more about bouncy castle and how to use it through the  
  
many resources on available here:  
[  
http://www.bouncycastle.org/  
](http://www.bouncycastle.org/)  

Happy coding! 

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — June 27, 2013 at 5:43 pm ([permalink](/blog/bouncy-castle-crypto-api/#comment-21701))

> Anonymous says:
>
> this is a very valuable addition !
>



### **Anonymous** — January 15, 2014 at 9:54 pm ([permalink](/blog/bouncy-castle-crypto-api/#comment-22025))

> Anonymous says:
>
> can you please tell me, how i can get sha1 or md5 hashing of a string using this lib?
>



### **Anonymous** — January 16, 2014 at 4:12 am ([permalink](/blog/bouncy-castle-crypto-api/#comment-21919))

> Anonymous says:
>
> Did you try this: 
>
> [https://www.google.com/sear…](<https://www.google.com/search?q=bouncy+castle+sha1>)
>



### **Anonymous** — October 26, 2014 at 6:27 am ([permalink](/blog/bouncy-castle-crypto-api/#comment-21900))

> Anonymous says:
>
> I tried to search for AES string encription, but all examples forces me to use [java.security](<http://java.security>).* (and I already have a working class for android written purely with [java.security/javax.crypto)](<http://java.security/javax.crypto>)), I need one for Your platform to cover the iPhone), which in return results with an error during compiling (error: package [java.security](<http://java.security>) does not exist). BouncyCastle is installed. Any example written PURELY with BouncyCastle?
>



### **Anonymous** — October 26, 2014 at 9:44 am ([permalink](/blog/bouncy-castle-crypto-api/#comment-21995))

> Anonymous says:
>
> Did you try searching our discussion forum for aes? 
>
> [https://groups.google.com/f…](<https://groups.google.com/forum/#!searchin/codenameone-discussions/aes>)
>



### **Anonymous** — October 26, 2014 at 3:52 pm ([permalink](/blog/bouncy-castle-crypto-api/#comment-22105))

> Anonymous says:
>
> Actually, I did not. Thanks.
>



### **Kaya TC** — March 7, 2016 at 11:21 am ([permalink](/blog/bouncy-castle-crypto-api/#comment-22700))

> Kaya TC says:
>
> Im having difficulties to Hash a passwordfield, i searched google for a while now, but the examples are not for cn1.  
> I could not find some classes which are in the examples so im stuck now.
>
> I just want to have a SHA hash =)
>



### **Shai Almog** — March 8, 2016 at 3:31 am ([permalink](/blog/bouncy-castle-crypto-api/#comment-22458))

> Shai Almog says:
>
> I think this contains some examples which might be helpful:  
> [http://www.programcreek.com…](<http://www.programcreek.com/java-api-examples/index.php?api=org.bouncycastle.crypto.digests.SHA1Digest>)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
