---
title: Questions of the Week 45
slug: questions-of-the-week-45
url: /blog/questions-of-the-week-45/
original_url: https://www.codenameone.com/blog/questions-of-the-week-45.html
aliases:
- /blog/questions-of-the-week-45.html
date: '2017-03-02'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-45/qanda-friday2.jpg)

We had some painful downtime due the the big Amazon S3 crash that brought down a huge amount of services with it. I’ve been toying with the idea of improving the system so it doesn’t fully depend on S3 (mostly for performance as S3 isn’t as fast as one would expect). But right now we are so busy with “real work” that this probably won’t happen.

This weeks update will again replace the push servers with a newer version that deals with encoding the push key on the newer supported platforms as well as support for UWP.

On the pull request front we had [2051](https://github.com/codenameone/CodenameOne/pull/2051) from [Terry Wilkinson](https://github.com/twilkinson) which adds a mode to the `JSONParser` where null attributes will still be included in the `keySet` of the parse `Map`.

Other than that todays update mostly includes bug fixes and hardly any new features.

On stack overflow [kevin](http://stackoverflow.com/users/919222/ikevin8me) asked about [event listening in a Container hierarchy](http://stackoverflow.com/questions/42502712/codename-one-event-listening-within-a-container-which-contains-more-sub-containe) which made [Diamond](http://stackoverflow.com/users/2931146/diamond) point him at lead component. This is always challenging for me as an API designer, how do you build an API in a way that people will discover without knowing what they are looking for?  
This is especially true for lead component which exists in no other API as far as I know.

[HelloWorld](http://stackoverflow.com/users/6351897/helloworld) stumbled on the [common pitfall with borders](http://stackoverflow.com/questions/42530678/why-is-the-designer-in-codename-one-not-reporting-the-attributes-i-set) where they take priority over everything else when designing a UI. I’d love to rewrite the theme designer and have some good ideas on how to do it. But we are still recovering from the rewrite of the GUI builder and picking up another windmill at this time is probably too much.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
