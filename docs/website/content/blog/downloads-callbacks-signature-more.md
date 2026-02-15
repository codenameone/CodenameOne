---
title: Downloads, Callbacks, Signature & More
slug: downloads-callbacks-signature-more
url: /blog/downloads-callbacks-signature-more/
original_url: https://www.codenameone.com/blog/downloads-callbacks-signature-more.html
aliases:
- /blog/downloads-callbacks-signature-more.html
date: '2016-02-28'
author: Shai Almog
---

![Header Image](/blog/downloads-callbacks-signature-more/signature.jpg)

We’ve had a busy week working with several customers on various tasks/issues as well as the documentation  
which is practically unrecognizable by now (more than 600 pages by now). As a result a lot of small fixes and  
enhancements were made to the code as well as one new niche component.

### ConnectionRequest.downloadImageToStorage

We have multiple ways to download an image and our general favorite is the [URLImage](/javadoc/com/codename1/ui/URLImage/).  
However, the [URLImage](/javadoc/com/codename1/ui/URLImage/) assumes  
that you know the size of the image in advance or that you are willing to resize it. In that regard it works great for  
some use cases but not so much for others.

In those other cases we usually recommend [one of the Util methods](/javadoc/com/codename1/io/Util/#downloadUrlToStorageInBackground-java.lang.String-java.lang.String-com.codename1.ui.events.ActionListener-).  
However, that might not always be appropriate since some edge cases might require more complex  
manipulation of requests e.g. making a `POST` request to get an image.

__ |  Adding global headers is another use case but you can use  
[addDefaultHeader](/javadoc/com/codename1/io/NetworkManager/#addDefaultHeader-java.lang.String-java.lang.String-)  
to add those.   
---|---  
  
To make this process simpler we added a set of helper methods to  
[ConnectionRequest that downloads images directly](/javadoc/com/codename1/io/ConnectionRequest/#downloadImageToStorage-java.lang.String-com.codename1.util.SuccessCallback-).

These methods complement the `Util` methods but go a bit further and feature very terse syntax e.g. we can just  
download a `ConnectionRequest` to `Storage` using code like this:
    
    
    request.downloadImageToStorage(url, (img) -> theImageIsHereDoSomethingWithIt(img));

### New Callback & Callback Dispatcher

You will notice that the terse code above accepted an argument that isn’t an `ActionListener` or a `Runnable`  
since it passes an `Image` object in the callback.

Historically we had a little known interface in Codename One that was inspired by GWT called  
[Callback](/javadoc/com/codename1/util/Callback/). This interface  
was used mainly for webservice calls so it had `onSuccess`/`onError` methods and a generic based response.

With Java 8 lambdas it occurred to us that it makes more sense to split this interface into two interfaces so  
we can write more terse code and so we now have:  
[SuccessCallback](/javadoc/com/codename1/util/SuccessCallback/) &  
[FailureCallback](/javadoc/com/codename1/util/FailureCallback/).

These are pretty convenient and I’m pretty sure we’ll use them elsewhere.

### Signature

As part of his work for a customer Steve implemented the [SignatureComponent](/javadoc/com/codename1/components/SignatureComponent/).  
This allows an app to show a surface where the user can scribble a signature to approve a contract or detail  
within the app.

![The Signature Component](/blog/downloads-callbacks-signature-more/components-signature2.png)

Simple usage of the `SignatureComponent` class looks like this:
    
    
    Form hi = new Form("Signature Component");
    hi.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
    hi.add("Enter Your Name:");
    hi.add(new TextField());
    hi.add("Signature:");
    SignatureComponent sig = new SignatureComponent();
    sig.addActionListener((evt)-> {
        System.out.println("The signature was changed");
        Image img = sig.getSignatureImage();
        // Now we can do whatever we want with the image of this signature.
    });
    hi.addComponent(sig);
    hi.show();

### Small Changes

#### Component.remove()

We added the ability to do [Component.remove()](/javadoc/com/codename1/ui/Component/#remove--).  
This is really shorthand syntax for the somewhat contrived `getParent()` syntax but it’s also pretty helpful when  
we aren’t sure if there is a parent.

So in effect it saves us the cost of an additional if statement.

#### Generic Action Listener & NetworkEvent Callbacks

Up until now [NetworkEvent](/javadoc/com/codename1/io/NetworkEvent/)  
was pretty painful to use. E.g. if you wanted to monitor networking code via a listener you always had to  
downcast to the `NetworkEvent` e.g.:
    
    
    NetworkManager.getInstance().addErrorListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            NetworkEvent e = (NetworkEvent)ev;
            // do something...
        }
    });

Or with Java 8 syntax:
    
    
    NetworkManager.getInstance().addErrorListener((ev) -> {
        NetworkEvent e = (NetworkEvent)ev;
        // do something...
    });

This sucks and made us constantly rethink our decision to use `ActionListener` everywhere…​

Until we suddenly realized we can generify `ActionListener` which is now `ActionListener<T>`. As a result  
we made all the listener events in `NetworkManager` and `ConnectionRequest` use `NetworkEvent`  
as the generic and the result is that this works:
    
    
    NetworkManager.getInstance().addErrorListener((networkEventInstance) -> {
        // do something...
    });

#### Shorter Padding/Margin Syntax

I’ve been writing a lot of demos and one of the things I try to do when writing demo code is to keep it as self  
contained as possible so it will work on your machines.

This is sometimes challenging as providing things such as theme instructions to reproduce my results is probably  
not intuitive. So to reproduce some behaviors I use `Style` manipulation code rather than themes which are superior  
but harder to convey in a manual.

As part of that I was manipulating margin and wanted to make sure the margin is in DIPs (millimeters). The common  
code for this is:
    
    
    cmp.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS);

This applies to all 4 sides of the margin/padding but if this seems redundant to you then you are not the only one…​

So from now on if you have less than 4 elements only the first one will count so this statement will be equivalent:
    
    
    cmp.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);

This also applies to padding so you can use that syntax there.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Yaakov Gesher** — March 1, 2016 at 10:15 am ([permalink](/blog/downloads-callbacks-signature-more/#comment-22509))

> Great stuff! It’s wonderful to see constant API improvements and new features every month!
>



### **Flying Bytes Jansen** — March 9, 2016 at 2:46 pm ([permalink](/blog/downloads-callbacks-signature-more/#comment-22477))

> Flying Bytes Jansen says:
>
> The SignatureComponent is just what I need. When will it be available?
>



### **Shai Almog** — March 10, 2016 at 3:44 am ([permalink](/blog/downloads-callbacks-signature-more/#comment-22649))

> Shai Almog says:
>
> It was released yesterday. Just do an Update Client Libs to get the latest and it will “just work”.
>



### **Flying Bytes Jansen** — March 10, 2016 at 8:49 am ([permalink](/blog/downloads-callbacks-signature-more/#comment-22541))

> Flying Bytes Jansen says:
>
> Great, thank you!
>



### **Mahmoud** — July 30, 2018 at 9:56 am ([permalink](/blog/downloads-callbacks-signature-more/#comment-23766))

> Mahmoud says:
>
> Dear Shai,
>
> There is an issue in Signature Component (ios,android) i cant draw dot(point) by one click  
> to draw it i need to press down and small move to any side  
> your help plz
>
> BR,
>



### **Shai Almog** — July 31, 2018 at 5:20 am ([permalink](/blog/downloads-callbacks-signature-more/#comment-23768))

> Shai Almog says:
>
> Hi,  
> I’ve made a small change which should hopefully resolve this.
>



### **Mahmoud** — July 31, 2018 at 5:38 am ([permalink](/blog/downloads-callbacks-signature-more/#comment-23799))

> Mahmoud says:
>
> you mean new change in new bulid or new update lib?
>



### **Shai Almog** — August 2, 2018 at 5:51 am ([permalink](/blog/downloads-callbacks-signature-more/#comment-23441))

> Shai Almog says:
>
> In the library. We’ll have the fix after you update this Friday. If it happens after that let me know.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
