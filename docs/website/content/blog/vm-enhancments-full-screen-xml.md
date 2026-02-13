---
title: VM Enhancements, Full Screen and XML
slug: vm-enhancments-full-screen-xml
url: /blog/vm-enhancments-full-screen-xml/
original_url: https://www.codenameone.com/blog/vm-enhancments-full-screen-xml.html
aliases:
- /blog/vm-enhancments-full-screen-xml.html
date: '2019-02-20'
author: Shai Almog
---

![Header Image](/blog/vm-enhancments-full-screen-xml/new-features-4.jpg)

We are in code freeze…​ As part of the release process I’m gathering the changes we implemented over the past few months. Quite a few didn’t get a blog post during this time. So here is a list of the important things we didn’t document.

### VM Changes

We did two big things for ParparVM (our iOS VM) which allowed us to expose this functionality in other platforms as well. First is support for most of the small methods in `Class` which can be helpful when we write some generic code. Specifically the methods: `isEnum()`, `isSynthetic()`, `isInterface()`, `isAnonymousClass()`, `isPrimitive()` and `isAnnotation()`.

We can’t guarantee that these will work perfectly for all cases as there are some complexities. E.g. our process for supporting Java 8 might trigger true for `isAnonymousClass()` due to a lambda. But this is a start.

Another important piece is `java.util.Objects`. Technically we implemented this class via [com.codename1.compat.java.util.Objects](https://www.codenameone.com/javadoc/com/codename1/compat/java/util/Objects.html) but if you use the [java.util.Objects](https://www.codenameone.com/javadoc/java/util/Objects.html) it will work just fine. The processor will translate the calls to the Codename One equivalents.

`Objects` includes some common useful utility methods. Hopefully we’ll add a few more classes like that as needed.

### XML Properties

We supported JSON in [properties](https://www.codenameone.com/blog/properties-are-amazing.html) from day one. XML is a bit harded but not by much. We added highly experimental support for XML into properties which can be useful when working with XML data from the server or storage.

__ |  This feature is experimental and the API might change/break   
---|---  
  
`PropertyIndex` includes several new API’s to enable that. First we have:
    
    
    public Element asElement();

This returns the `PropertyBusinessObject` as an XML `Element` object which is the parsed form of XML in Codename One. You can use `XMLWriter` to convert this to a String or use XML processing code to work with the `Element`.

The `toXML()` method in `PropertyIndex` takes the next step of converting that XML `Element` to a String and returning it:
    
    
    public String toXML();

A property in the object can appear as the text element e.g. `<tag>Text Element</tag>`. You can explicitly define which by using these methods in `PropertyIndex`:
    
    
    public void setXmlTextElement(PropertyBase p, boolean t);
    public boolean isXmlTextElement(PropertyBase p);

Finally, we can parse XML by using:
    
    
    public void fromXml(Element e);

This will walk the parsed XML and convert it to the object we’re using.

### Full Screen

The Desktop and JavaScript ports support running in full screen mode. This is useful for games and media applications. It’s also useful for PoS and multiple other purposes.  
Historically, we had a build hint that allowed you to set full screen mode for the desktop port. That isn’t ideal as full screen might be something you wish to toggle dynamically within the app. It might require user permission too as is the case for JavaScript.

We now have a new full screen API. The following code assumes you have an import of `CN` specifically `import static com.codename1.ui.CN.*;`:
    
    
    if(isFullScreenSupported() && !isInFullScreenMode()) {
        requestFullScreen();
    }

You can also use `exitFullScreen()` to perform the inverse operation.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
