---
title: Java 8 Support
slug: java-8-support
url: /blog/java-8-support/
original_url: https://www.codenameone.com/blog/java-8-support.html
aliases:
- /blog/java-8-support.html
date: '2015-07-05'
author: Shai Almog
---

![Header Image](/blog/java-8-support/java-8-lambada.png)

When we introduced Codename One initially we limited the API to CLDC level which is roughly a subset of Java 1.3, we then  
added support for a subset of Java 5 and we are now adding Java 8 language features!  
Thanks to some work from Steve and the great work done by the guys from the  
[Retro Lambda](https://github.com/orfjackal/retrolambda/) project we were able to add compatibility  
to the major Java 8 features, most notably lambda expressions. This is highly experimental and some features  
might not work but so far it seems things are functioning rather smoothly. 

Note that this feature will only be available with the next plugin update and isn’t online right now…  
The main motivation for doing this is in reducing a lot of the boilerplate code you would normally get when writing  
Codename One code, e.g. currently we write something like: 
    
    
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Dialog.show("Event Happened", "You pressed a button", "OK", null);
        }
    });

Whereas lambdas allow us to write this: 
    
    
    button.addActionListener((e) -> {
        Dialog.show("Event Happened", "You pressed a button", "OK", null);
    });

Technically both are practically identical!  
This can be applied across the board e.g.: 
    
    
    Display.getInstance().callSerially(() -> {
        // this code executes on the EDT....
    });

This won’t be the default until we are confident that this is stable enough, with an upcoming plugin update  
you will have an option to create a Java 8 project instead of a Java 5 project. You could also convert existing  
projects to Java 8 but that will require some effort: 

  1. Make sure you have Java 8 installed and that your IDE is running under Java 8
  2. Change all “source” and “target” values for Javac calls in build.xml from 1.5 to 1.8.
  3. Update the IDE build settings for the project (in the project properties menu) to use Java 8 source level
  4. Add the build hint `java.version=8`
  5. Update CLDC11.jar to the latest version using the Update Client Libs button in the project preferences

The post mostly covered lambdas but other newer Java features such as String based switch cases (from Java 7)  
try with resources etc. should work just fine however we didn’t do enough tests for the various features on all the  
platforms (hence the beta moniker).   
Try with resources is pretty cool e.g. you can do something like: 
    
    
    try(InputStream is = Display.getInstance().getResourceAsStream(getClass(), "/myFile")) {
        // work with file
    } catch(IOException err) {
        Dialog.show("Error", "Exception accessing the resource", "OK", null);
    }
    

Which doesn’t seem like much until you realize that this really replaced this code: 
    
    
    InputStream is = null; 
    try {
        is = Display.getInstance().getResourceAsStream(getClass(), "/myFile");
        // work with file
    } catch(IOException err) {
        Dialog.show("Error", "Exception accessing the resource", "OK", null);
    } finally {
        if(is != null) {
           try {
              is.close();
           } catch(IOException err) {}
        }
    }
    

Just to be fair, this code can be slightly more concise in the finalizer block with Codename One’s `Util.cleanup(is)` method… 

One feature of Java 8 that isn’t supported at the moment is [streams](https://docs.oracle.com/javase/8/docs/api/java/util/stream/package-summary.html).  
While they would be nice to have they probably won’t be as helpful as the other  
features in a mobile environment and won’t provide performance benefits in these cases. We might add them  
at a future date if there is demand for that. 

Notice that Java 8 support was mostly tested with Android, iOS & the desktop port. It should work well with most modern ports but might have issues  
in platforms such as J2ME/RIM where even the Java 5 compatibility is flaky.  
The image in the title of this post is from this [Takipi blog](http://blog.takipi.com/the-dark-side-of-lambda-expressions-in-java-8/) which  
is pretty relevant to the subject of this post.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **James Hastings** — July 6, 2015 at 5:19 pm ([permalink](/blog/java-8-support/#comment-22329))

> James Hastings says:
>
> This is great news. What about the localdate class? I have a program I wrote that uses a bunch of localdate functionality that I wanted to port into a mobile app. Refactoring everything to Calendar and Date objects has kept this on the backburner for me.
>



### **Shai Almog** — July 7, 2015 at 5:47 am ([permalink](/blog/java-8-support/#comment-22434))

> Shai Almog says:
>
> Thanks.  
> That isn’t supported at the moment and probably won’t be around in the immediate future. The main hurdle is compatibility to existing platforms so we are walking on eggshells here trying to minimize the impact of the core libraries.
>
> I feel your pain, every Java developer who dealt with dates in any way hates Calendar/Date. I am looking forward to migrating to something decent, but that might be a while.
>



### **Codrut Gusoi** — July 7, 2015 at 6:39 pm ([permalink](/blog/java-8-support/#comment-21543))

> Codrut Gusoi says:
>
> Yay lambdas!  
> Brace yourselves, assertException() is coming… at least when I will have time for a pull request.
>



### **Sanny Sanoff** — August 2, 2015 at 10:52 am ([permalink](/blog/java-8-support/#comment-22356))

> Sanny Sanoff says:
>
> Will they (lambdas) work on Android, too? How do you implement this feature if you just pass user java to dalvik?
>



### **Shai Almog** — August 4, 2015 at 4:27 am ([permalink](/blog/java-8-support/#comment-22150))

> Shai Almog says:
>
> Yes.  
> It works on all platforms even J2ME since it uses retrolambda on the server before the main processing of the bytecode.
>



### **Martin Grajcar** — November 27, 2019 at 2:21 pm ([permalink](/blog/java-8-support/#comment-24272))

> [Martin Grajcar](https://lh6.googleusercontent.com/-gclegbxVkVE/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3reP0qIwi57AHP6gC6BhvXA4p34zZA/photo.jpg) says:
>
> Unlike inner classes, lambdas don’t capture the enclosing class…. I just find out that I’m storing quite a few lambdas generated in forms, which must not retain the reference to the enclosing form.
>
> Do retrolambdas retain the reference to the enclosing class?
>



### **Shai Almog** — November 28, 2019 at 2:25 am ([permalink](/blog/java-8-support/#comment-24271))

> Shai Almog says:
>
> Retrolambda translates Java 8 lambdas to inner classes internally. Lambdas use `this` as a reference to their surrounding class so they have a reference to their parent just like non-static inner classes.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
