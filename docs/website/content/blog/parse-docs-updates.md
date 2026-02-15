---
title: Parse, Docs & Updates
slug: parse-docs-updates
url: /blog/parse-docs-updates/
original_url: https://www.codenameone.com/blog/parse-docs-updates.html
aliases:
- /blog/parse-docs-updates.html
date: '2016-01-31'
author: Shai Almog
---

![Header Image](/blog/parse-docs-updates/parse.com-post-header.jpg)

Facebook recently announced the closing of parse.com which is disappointing but not totally surprising. Everyone  
knows a project from a large tech company can just shutdown in a blink of an eye leaving millions of users/developers  
in limbo. In that sense one of the questions that gets under my skin is what happens if Codename One calls it  
quits…  
You’d be better off than anyone who used parse and its far less likely to happen. Codename One is “what we do”  
and have been doing for the past 4 years. More importantly, unlike Parse you could just use the open source project  
and your users wouldn’t be the wiser. Since Parse is a service that needs to connect to a hosted server everyone  
who used parse (myself included) need to develop a rather complex strategy of moving their hosted data (which is  
live) while moving their users some of which might still have the old version that use parse on their devices… 

When you are looking at PaaS services you need to always evaluate the case of the PaaS closing down (this  
is true for IaaS as well but to a lesser degree), in that sense Parse is quite problematic since its effectively  
exposed to user installs. Codename One is far safer since the built app doesn’t really need Codename One anymore… 

As I mentioned above before taking a long detour, I have a personal app that relies on Parse so I will need to port  
it to a different infrastructure. Most of the code there is already well abstracted but I will need to find a way  
to migrate the data and also pick a host for the data. I will try to publish the process in a way that helps other  
parse4cn1 users with the migration. 

#### Slowing Down New Feature Development

We have set an ambitious goal of posting less news for the month of February and probably March too.   
As developers we always want to build a shiny new thing and satisfying a feature request from a user is often  
rewarding and cool. However, this means we don’t spend the time writing documentation, generating tutorials  
or working on things such as performance, the new windows phone port, fixing bugs etc. 

None of those things are really news worthy, so spending the time just writing a blog post about better  
JavaDocs/samples for a particular package is probably not very helpful. But this is crucial, we need stability  
and refined docs in order to communicate better even with our top developers. As we work on the docs we  
see exactly what people have been complaining about. We have a lot of documentation but it is quite bare  
and isn’t at the level it should be. 

This is a big task and you can already see a lot of the results in the first two chapters of the developer guide and  
within the javadocs. 

#### New Features/Enhancements

In direct contrast to the statement above, some features did get thru despite our best efforts…  
We added [getActualComponent()](/javadoc/com/codename1/ui/events/ActionEvent/#getActualComponent--)  
to [ActionEvent](/javadoc/com/codename1/ui/events/ActionEvent/),  
this allows us to handle events better for lead components such  
as [MultiButton](/javadoc/com/codename1/components/MultiButton/). 

Finally we added terse  
[encloseIn](/javadoc/com/codename1/ui/table/TableLayout/#encloseIn-int-com.codename1.ui.Component...-)  
methods to the  
[TableLayout](/javadoc/com/codename1/ui/table/TableLayout/)  
class.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — February 2, 2016 at 8:13 am ([permalink](/blog/parse-docs-updates/#comment-22476))

> Chidiebere Okwudire says:
>
> With regard to alternatives to Parse, I just came across this github repo: [https://github.com/relatedc…](<https://github.com/relatedcode/ParseAlternatives>)


### **Jérémy MARQUER** — February 2, 2016 at 8:42 am ([permalink](/blog/parse-docs-updates/#comment-22648))

> Jérémy MARQUER says:
>
> I’m pretty agree with the “Slowing Down New Feature Development” paragraph, but I find it unfortunate that you will post less news … It allows us (devs) to stay in touch with your development !


### **Shai Almog** — February 3, 2016 at 3:29 am ([permalink](/blog/parse-docs-updates/#comment-22551))

> Shai Almog says:
>
> There is also this: [http://www.slant.co/topics/…](<http://www.slant.co/topics/5219/~parse-alternatives-afor-android-app-development>)
>
> Which is more concise and to the point. That list is a bit too much of everything and hard to read.
>
> BTW slant has a Codename One entry that could probably use some love.


### **Shai Almog** — February 3, 2016 at 3:32 am ([permalink](/blog/parse-docs-updates/#comment-22183))

> Shai Almog says:
>
> Thanks. We’ll post news as it happens and we might even keep the bi-weekly posts.
>
> However, if you look at the past few months we introduced a few new features every week. This is probably not something that will recur. We do intend to keep you up to date about development but if there isn’t any development (just docs, debugging and maintenance) then we might skip a post or write an opinion piece instead.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
