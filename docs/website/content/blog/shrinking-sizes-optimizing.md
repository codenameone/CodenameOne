---
title: Shrinking Sizes & Optimizing
slug: shrinking-sizes-optimizing
url: /blog/shrinking-sizes-optimizing/
original_url: https://www.codenameone.com/blog/shrinking-sizes-optimizing.html
aliases:
- /blog/shrinking-sizes-optimizing.html
date: '2015-05-17'
author: Shai Almog
---

![Header Image](/blog/shrinking-sizes-optimizing/compress-jar-netbeans.png)

Our build servers are really fast, even if your laptop is relatively slow our iOS build servers are powerful machines  
equipped with fast SSD’s and they generate a full clean build of a typical app (with screenshots etc.) in a couple of minutes!  
So the real source of delay when building an app is size, it both slows the build but most of all it slows your upload  
process (upload is typically much slower than download). Reducing the size of your app will make it faster in runtime  
as well, e.g. if you have too many redundant resources you might be running into too many GC cycles slowing  
down execution. In this post we provide some tips to shrink your app size. 

#### Compress JAR

![](/blog/shrinking-sizes-optimizing/compress-jar-netbeans.png)

When we released Codename One’s beta we released a version that didn’t compress the built JAR. This wasn’t  
a big deal for small projects but it became an issue as Codename One applications grew. Make sure that the JAR  
output is compressed, so that server builds would be significantly smaller. This is crucial since it reduces upload  
time in the client side which is a huge contributor to the total build time. 

Notice that apps created in the past year should have this on by default but its always important to check. 

#### Inspect the Sent JAR

When optimizing the jar size its often hard to see where to begin. After you send a build if you open your dist folder  
(or bin folder for Eclipse) you will see what seems to be the jar of your application. However, in this case its the jar  
that was sent to the server including the native bits that need to be compiled on the server side.  
You can use a zip utility such as 7-zip to inspect the content of the jar and see what takes up space and how  
well are files compressed. Its possible that a specific file within the zip isn’t supposed to be there and its possible  
that things can be shrunk further. 

#### Shrinking Resources

The most likely case for a large file is the resource file and here there are two distinct tasks: finding out what is taking up  
the space and reducing the size. For the former we have `Image->Image Sizes`, in the designer  
tool. It returns the list of images ordered by the space they take up in the resource file, since images can’t be further  
compressed they are the biggest space hog in the resource file. 

Unless you found a specific image that takes up most of the space its probable that space is taken up by many  
multi-images. Multi-images are essentially a single image in multiple resolutions for different device densities,  
these images can grow to a very large size as we store a lot of resolutions for every such image. If there are densities  
you don’t need such as LOW &amp VERY_LOW (both of which don’t exist in smart devices) you can just delete  
all of these images from existing multi-image’s thru the menu item: `Image->Advanced->Remove DPI`. 

`Image->Delete Unused Images` presents you with a dialog containing the images that are unused.  
It allows you to select the images you wish to delete (all are selected by default). Notice that this method was designed for  
GUI builder applications and doesn’t scan the code for image usage. It will also miss images from code although it tries  
to scan the state machine class intelligently.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Jeff Brower** — October 18, 2015 at 1:22 pm ([permalink](https://www.codenameone.com/blog/shrinking-sizes-optimizing.html#comment-22421))

> Jeff Brower says:
>
> I am impressed that I received an email from you directing me to this link when my build went over size. Anyone else would just consider a great opportunity to upsell me and not be so helpful. I was disappointed that I ran over my maximum size by just doing a straight compile of one of the sample CodenameOne programs in Netbeans (even though it was Kitchen Sink), but now I know why and how to fix it!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fshrinking-sizes-optimizing.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
