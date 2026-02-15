---
title: 'TIP: Search the Website'
slug: tip-search-the-website
url: /blog/tip-search-the-website/
original_url: https://www.codenameone.com/blog/tip-search-the-website.html
aliases:
- /blog/tip-search-the-website.html
date: '2016-12-11'
author: Shai Almog
---

![Header Image](/blog/tip-search-the-website/just-the-tip.jpg)

A frequent complaint we get is the lack of a search feature on the site and we get that. It’s frustrating to us too.  
We’d like to add it but are still looking at the “right way” to do it which I’ll discuss at more length below but for  
now I’d like to discuss a couple of relatively simple workarounds.

### The Current Workarounds

#### Google Site Search

We can just type site:codenameone.com followed by your query into google when searching to search within the site  
e.g. like this <https://www.google.co.il/search?q=site%3Acodenameone.com+Button>

This is the same as Google’s custom search which some sites use but provides a “sub par” experience.

#### JavaDoc Index

I’ve discussed this in the past, if you are looking for a method, variable or class this is probably the best place  
to start: </javadoc/index-all/>

This is the full “index” of our JavaDoc. It doesn’t include everything obviously it doesn’t include cn1libs and some  
information might be missing but the ability to use the browser search within that page is very valuable.

#### Developer Guide PDF

The [developer guide is available in PDF form](https://www.codenameone.com/files/developer-guide.pdf) and that  
is easily searchable. It contains a lot of useful information on almost everything.

#### 3rd Party Hosts

One of the bigger problems is that a lot of our content isn’t on our site. Searching stackoverflow is rather convenient  
but you should do it within the tag itself otherwise it might be needle in a haystack: <http://stackoverflow.com/tags/codenameone>

Google Groups have a mediocre search experience but it’s usable, notice that you need to search the group  
directly as the main Google search often misses that and will not show that content for “site:” searches:  
<https://groups.google.com/forum/#!forum/codenameone-discussions>

### Why don’t we “just” Implement Search?

We use [JBake](http://jbake.org/) for our backend. We still have a dynamic server and could do some logic (e.g.  
lucene) in there but it seems like a “mess” when dealing with static data. This is compounded by the fact that  
some of our searchable data comes from different sources (e.g. JavaDoc). I’ll ignore stackoverflow and  
the google group searches for now as they probably fall outside of the initial problem.

What we really want is a static search tool that will work with the static site generation, this allows us to leverage  
solutions like cloudflare to scale really well!

There are some tools like that which are pretty cool e.g. [lunr.js](http://lunrjs.com/) but they don’t scale well to  
sites as large as ours. We have over 15,000 non-trivial unique single word keywords…​ Creating an index to  
map this takes up at least 5mb just for the index of keywords in every page. This makes client side search a  
bit difficult.

We’ve played with some ideas such as a split index but there doesn’t seem to be a “ready made” solution.

If you are aware of something we’re missing that will play nicely with JBake on scale let us know. If not we  
might have to build something of our own.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
