---
title: Sign In With…
slug: sign-in-with
url: /blog/sign-in-with/
original_url: https://www.codenameone.com/blog/sign-in-with.html
aliases:
- /blog/sign-in-with.html
date: '2015-06-21'
author: Shai Almog
---

![Header Image](/blog/sign-in-with/google-sign-in.png)

Chen recently published a major refactoring of our connection framework which up until now only supported  
Facebook login. With this recent refactoring the code to connect to various authentication services becomes  
far simpler and various services should become more pluggable. The default implementation centers  
around the `Login` and `LoginCallback` classes that use oAuth by default to  
perform the login. 

`FacebookConnect` keeps working just like it always did but now it extends the Login class to  
provide generic login functionality which means we can seamlessly provide additional login targets either via  
oAuth or even via native integration with 3rd party SDK’s. One of the first integrations here is the  
`GoogleConnect` support which allows logging into a Google account to sign in. This is especially  
great on Android devices where the process to sign in is seamless! 

Working with the `GoogleConnect` class requires a corresponding Google cloud project. To  
enable this you need to follow the instructions [here](https://developers.google.com/+/web/signin/)  
to allow web login. The web login option is essential for simulator login thus crucial for debugging. You would also  
need to follow the instructions for [Android](https://developers.google.com/+/mobile/android/getting-started)  
and [iOS](https://developers.google.com/+/mobile/ios/getting-started) respectively. 

The login framework should serve as the basis can can be extended easily via cn1lib’s, we hope to provide such  
login frameworks for various services out there and also to migrate some of the existing oAuth usages by cn1lib’s to  
use this framework instead of the mix and match options we have right now.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chris Smith** — April 28, 2017 at 4:45 am ([permalink](/blog/sign-in-with/#comment-23123))

> Chris Smith says:
>
> I have this working, but I’m hoping to post a document to Google drive – is this supported with this login?
>



### **Shai Almog** — April 29, 2017 at 6:12 am ([permalink](/blog/sign-in-with/#comment-23400))

> Shai Almog says:
>
> I think this guy on stackoverflow was trying to do the same thing with Codename One: [http://stackoverflow.com/qu…](<http://stackoverflow.com/questions/41352584/401-error-unauthorized-when-trying-to-post-to-a-public-google-sheet-using-th>)  
> I have no idea if he succeeded or not
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
