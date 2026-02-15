---
title: JavaDoc Source Samples That Don't Suck
slug: javadoc-source-samples-that-dont-suck
url: /blog/javadoc-source-samples-that-dont-suck/
original_url: https://www.codenameone.com/blog/javadoc-source-samples-that-dont-suck.html
aliases:
- /blog/javadoc-source-samples-that-dont-suck.html
date: '2016-01-17'
author: Shai Almog
---

![Header Image](/blog/javadoc-source-samples-that-dont-suck/with-javascipt-gist.png)

JavaDoc source code embeds suck!  
I love JavaDoc but it didn’t age well. When you work with other tools (e.g. in the Microsoft world) suddenly the  
embedded samples look amazing and “search” functionality is just built in!  
**Why can’t we have that?**  
JDK 9 is [introducing new support for search](http://mail.openjdk.java.net/pipermail/jdk9-dev/2016-January/003362.html)  
but source embeds can be so much better and are a crucial learning tool… 

Since documentation and proper code samples are so crucial we decided to revisit our javadocs and start from  
the ground up, to that point we created the new open source project:  
[JavaDoc Source Embed](https://github.com/codenameone/JavaDocSourceEmbed). 

The goal of this project is to allow using github “gist” in JavaDoc which allows you to create JavaDoc that looks  
like [this](/javadoc/com/codename1/location/LocationManager/)  
instead of the normally anemic source embeds.   
If you are unfamiliar with [github gists](https://gist.github.com/) its essentially  
a code snippet hosting service that both formats the code nicely and allows you to easily maintain it thru  
github (fork, star, watch etc.).  
The central hosting is the true “killer feature”, it allows you to embed the sample everywhere that’s applicable  
instead of copying and pasting it. E.g. the [LocationManager](/javadoc/com/codename1/location/LocationManager/)  
is a good place to hold the sample but so is the [Geofence](http://codenameone.com/javadoc/com/codename1/location/Geofence.html) class.  
In those cases we only had to copy this small snippet in the javadoc: 
    
    
    <script src="https://gist.github.com/codenameone/b0fa5280bde905a8f0cd.js"></script>

The only two problems with gist are its lack of searchability and the fact that it won’t appear in IDE’s that don’t  
render JavaScript. The [JavaDoc Source Embed](https://github.com/codenameone/JavaDocSourceEmbed)  
project effectively solves that by automatically generating a “noscript” tag with the latest version of the gist  
so it appears properly everywhere it is referenced. 

We’ll try to update our javadocs but would be happy for pull requests and issues pointing at missing samples  
and where they should be placed in the code. 

#### Developer Guide Wiki

In other news we just finished the migration of the developer guide to the github wiki page and already it looks  
radically different. The approach of using githubs wiki pages has its drawbacks and asciidoc does have  
some pain points but overall I think this is a good direction for an open project.  
[Ismael Baum](https://github.com/Isborg) made a lot of wiki edits fixing many  
grammatical and logical errors picking up so many mistakes in the process!  
Besides the many rewrites and fixes we made for the document we also authored a script that translates  
Codename One class names to links into the JavaDoc. 

So now instead of just highlighting the mention of `LocationManager` you would see  
[LocationManager](/javadoc/com/codename1/location/LocationManager/)  
which is far more useful. Notice that this shouldn’t affect things like code blocks only mentions of a  
specific class. From this point on we’ll try to interconnect the documentation to produce a more coherent  
experience with the docs.  
I’d open source the script we used for the links but its mostly a bunch of very specific `sed`  
commands which probably won’t be useful for anyone. We won’t run it again since its a “one off” script,  
we’ll just need to keep the linking going. 

#### Feedback

Do you know of other tools we can use to improve the state of our documentation?  
We are looking for several things that still seem to be hard with the current toolchain: 

  * Better JavaDoc integrations – ability to embed it into the existing web design would be wonderful!  
CSS is a bit too limiting.
  * Improving the look of the asciidoc PDF – Currently the PDF looks too academic in the opening page  
there are some solutions for that but most seem hackish. 
  * Grammar & Style tools – There are some decent grammar checkers for word processors but  
we couldn’t find anything that works with asciidoc. The same is missing for writing analysis tools that  
can point out unclear writing. I saw that gitbooks has some interesting tools there but I’m unsure whether  
we want to use it.

Let us know if you are familiar with such tools or something else that we might not be aware of.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
