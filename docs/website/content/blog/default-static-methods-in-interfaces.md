---
title: Default & Static Methods In Interfaces
slug: default-static-methods-in-interfaces
url: /blog/default-static-methods-in-interfaces/
original_url: https://www.codenameone.com/blog/default-static-methods-in-interfaces.html
aliases:
- /blog/default-static-methods-in-interfaces.html
date: '2016-04-12'
author: Shai Almog
---

![Header Image](/blog/default-static-methods-in-interfaces/java-8-lambada.png)

In our original [Java 8 support announcement](/blog/java-8-support.html) post we specifically mentioned the  
lack of streams but completely missed the fact that default/static native interfaces didn’t work. This is now fixed  
thanks to an alert community member who pointed that out.

It seems that these features are turned off by default for [retrolambda](https://github.com/orfjackal/retrolambda)  
due to limitations that require a clean build to get them to work. This is no limitation for the Codename One  
build server architecture so these features should work just fine for Codename One apps.

### What are Default Interface Methods?

Default interface methods allow you to add new methods to an interface and provide a default implementation.  
This effectively enables us to move an API forward without breaking compatibility with someone who implemented  
this interface. E.g. :
    
    
    public interface DefaultInterfaceTest {
        String method();
    
        default String methodWithArg(String arg) {
            return method();
        }
    }

This isn’t as important for most developers as we normally can just add a new method and solve the issue.  
However, in the future as we move the implementation of Codename One to Java 8 syntax this will be a huge  
boost as it will allow us to add methods to older interfaces such as [PushCallback](/javadoc/com/codename1/push/PushCallback/).

### What are Static Interface Methods

Static interface methods are generally just static methods. In many cases we just hide static methods within  
clases but sometimes that doesn’t make sense. E.g. the [Push](/javadoc/com/codename1/push/Push/) class  
is entirely composed of static methods and doesn’t make much sense as a standalone class. We could have  
rolled all the methods within the class into the interface as static methods and eliminated the class entirely.

This isn’t necessarily “good practice” but for some use cases this might be a better place to hold the method.

E.g.:
    
    
    public interface StaticInterfaceTest {
        String method();
    
        static String getNotNull(StaticInterfaceTest it, String def) {
            String s = it.method();
            if(s == null) return def;
            return s;
        }
    }

You can read about default and static interface methods in the [Java Tutorial](https://docs.oracle.com/javase/tutorial/java/IandI/defaultmethods.html).

### Switch to Full Java 8?

As implied above we would get quite a bit of value from switching the code base of Codename One itself to Java 8.  
Right now we still support building Java 5 apps and would probably not change that before 3.4 rolls out as our  
current goals are stability more than anything else. However, once 3.4 rolls out we might implicitly make all  
builds use Java 8 features and switch the internal code base to use it.

Even if you use an old Java 5 project the builds should still work fine after such a transition and you won’t be forced  
to switch, however, this will allow us to use features such as default methods to implement some capabilities we  
need. It will also make out lives slightly easier by allowing us to use lambdas in our core implementation.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
