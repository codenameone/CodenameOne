---
title: Runtime Debugging with Groovy Console
slug: runtime-debugging-with-groovy-console
url: /blog/runtime-debugging-with-groovy-console/
original_url: https://www.codenameone.com/blog/runtime-debugging-with-groovy-console.html
aliases:
- /blog/runtime-debugging-with-groovy-console.html
date: '2019-11-04'
author: Steve Hannah
---

![Header Image](/blog/runtime-debugging-with-groovy-console/new-features-5.jpg)

We recently added a new tool to the  
Codename One simulator that will allow you to run arbitrary code  
snippets in your app’s runtime environment. You can access  
it from “File” > “Groovy Console”, while your app is running in  
the simulator.

  
![](/blog/runtime-debugging-with-groovy-console/image3.png)

This will open the following dialog  
that will allow you to execute arbitrary Groovy code:

  
![](/blog/runtime-debugging-with-groovy-console/image5.png)

As you may suspect, you should use  
the Groovy programming language in this console. If you’re  
not familiar with Groovy, don’t worry. The language is  
basically just Java with some nice short-cuts. In fact, in  
many cases, you can probably just write Java code, and it will  
still work. Personally, I like the way that Groovy lets you  
refer to object properties via their property name, so you don’t  
need to use the getter or setter methods explicitly. E.g. You  
can do “form.title”, instead of “form.getTitle()”, to get the  
form’s title. The former is converted into the latter  
automatically for you.

### First Example:  
Getting the Current form’s title

As a simple first example, let’s get  
the current form’s title and print the value in the console.   
The default content in the console already has a reference to the  
current form via the “form=CN.currentForm” line. So all we  
need to do is add:

Then press “Command-Enter” (or  
Ctrl-Enter on Windows/Linux”), to see the output. You should  
see something like the following in the bottom-half of the  
console:

  
![](/blog/runtime-debugging-with-groovy-console/image4.png)

### Second Example:  
Showing a Dialog:

You can also interact with the UI.  
Create forms, components, dialogs – or essentially create  
entire apps. Below, you can see us displaying a simple dialog  
directly in the console.

  
![](/blog/runtime-debugging-with-groovy-console/image1.png)

### Using the  
ComponentSelector

My most frequent uses of the console  
is to find out the properties on some component in the UI.  
For this sort of thing, the ComponentSelector class is an  
invaluable tool. It makes it easy to find the components that  
you’re interested in, and to inspect its state.

For example, we might want to look  
at the font height of all instances of the Label class on the  
current form:

  
![](/blog/runtime-debugging-with-groovy-console/image2.png)

The above example demonstrates some  
fundamentals of both Groovy, and of the ComponentSelector  
class.

  1. `$(‘*’, form)` – This creates a set  
of all components in the current form. “*” matches  
all.
  2. `.filter{ ..}` – This method  
filters the set so that only the components for which the closure  
returns `true` are included. Notice the convenient  
notation for a closure in Groovy: `{ .. }`. And it  
provides you with the implicit variable “it” which refers to the  
parameter in a single-parameter method closure.
  3. `.each{…}` – Allow you to execute  
code on “each” element in the set. Again, we use the  
convenient closure notation and implicit “it” object provided by  
Groovy.

### Useful for  
Troubleshooting Requests

One potential area where the console  
may have some powerful uses, is in the area of trouble-shooting and  
bug reporting. If a developer asks the community for help in  
debugging a problem in their app it is now possible for community  
members to share snippets that might help to diagnose the problem.  
It was already possible to share snippets, but the console  
makes it easier to provide advice like: “When you get to the  
part of your app that is having the problem, try running this  
snippet of code inside the console, and let me know what the output  
is”.

## Versus  
Debugging in the IDE

It is worth noting that you could  
already inspect the runtime state of your app using your IDE  
debugger. You can pause the app, then evaluate arbitrary Java  
expressions to see their output. You can Add break-points,  
and even make source code changes that are applied seamlessly at  
runtime. The console is not meant to replace this  
functionality. It is meant to provide a slightly  
lighter-weight approach to achieve many of the same things.  

You’ll have to find your own usage  
patterns, but I have found that it is handy to be able to just open  
up a console, whether or not my app is being run in “Debug” mode,  
and start tinkering with some code to get a clearer picture of what  
is going on inside my app.

### Share Your  
Snippets

I’ve only posted a couple of simple  
examples of how you might use the console, but the sky is the  
limit. Please share your own snippets and tips if you  
discover any tricks that might be helpful to other users. You  
can share them below in the comments.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
