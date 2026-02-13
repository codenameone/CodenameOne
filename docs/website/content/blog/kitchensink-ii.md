---
title: KitchenSink II
slug: kitchensink-ii
url: /blog/kitchensink-ii/
original_url: https://www.codenameone.com/blog/kitchensink-ii.html
aliases:
- /blog/kitchensink-ii.html
date: '2016-08-30'
author: Shai Almog
---

![Header Image](/blog/kitchensink-ii/kitchensink-2-flat.jpg)

> I try all things, I achieve what I can. 

— Herman Melville  
Moby-Dick

Opening with a Moby-Dick quote seems rather appropriate for taking on the kitchen sink demo. It often seemed like a task that is too big and unsurmountable as we were working our way thru it. In fact the version that we are releasing now has far less features and abilities than our original whiteboard…​

The kitchen sink was our first demo, we drew inspiration from previous demos we did at Sun where the custom is to show off every UI element.

Over the years it aged and included bad practices in the code. Since it’s such a major demo we knew we needed something special and set about with the following goals:

  * Different tablet UI – We think it’s important for applications to adapt intelligently to tablet UI’s and the kitchen sink always demonstrated that

![The kitchen sink demo running on an iphone](/blog/kitchensink-ii/kitchensink-ii-iphone.png)

Figure 1. The kitchen sink demo running on an iphone

![The kitchen sink demo running on an ipad](/blog/kitchensink-ii/kitchensink-ii-ipad.png)

Figure 2. The kitchen sink demo running on an ipad

  * Usable in free tier – the old kitchen sink was a multi-megabyte demo unusable by free subscribers. We wanted a big demo that would still fit in the 1mb free user limit

  * Aesthetically refined – we wanted a UI that’s modern and subtle, the goal isn’t to create something “exceptional”…​ The goal is to create something reasonable and achievable

  * Realistic use cases – with the one exception of the layouts demo, most of the demos show usage that might be reasonable in a “real world” application

**Check the live version running on the right hand side thanks to the power of the Codename One JavaScript port!**

Notice that the demo to the right is running in phone mode, to see it running in tablet mode open it in the desktop browser using this [link](/demos/KitchenSink/).

You can install the native Android version of the kitchen sink from [Google Play](https://play.google.com/store/apps/details?id=com.codename1.demos.kitchen) & the [native iOS version from itunes](https://itunes.apple.com/us/app/kitchen-sink-codename-one/id635048865). We’ll try to publish a UWP version and will update this space when we get it out.

With those goals in mind we recreated and re-invented the demos that are a part of kitchen sink.

Before going into the individual demos notice that app now has a UI that is more “card like”. We dynamically round the edges of the images (using masking) and allow you to search thru the demos using the standard search `Toolbar`.

You can also switch between grid/list view in the standard cell phone mode (this isn’t available in the tablet mode due to the different UX).

![List mode - click the top right icon to toggle modes](/blog/kitchensink-ii/kitchensink-ii-list-mode.png)

Figure 3. List mode – click the top right icon to toggle modes

When in list mode we use round version of the images (again with masking). We layer a decorative border on top to make them stand out a bit.

The current 2.0 version contains the following demos:

#### Layouts

Shows some of the core layouts in Codename One this is the one demo that bares some semblance to the old kitchen sink demo

![Layouts demo](/blog/kitchensink-ii/kitchensink-ii-layouts.png)

Figure 4. Layouts demo

#### Dogs

Demonstrates usage of a webservice to fetch a list of dog images that are then rendered as a list and fetched dynamically. We use `InfiniteContainer` to fetch data in batches.

When you click an entry we open it in an `ImageViewer` and dynamically fetch the full sized image.

![Dogs websevice/image demo](/blog/kitchensink-ii/kitchensink-ii-dogs.png)

Figure 5. Dogs websevice/image demo

#### Time

This is the old clock demo. It demonstrates low level graphics and also presents dynamic image creation

![Time Demo](/blog/kitchensink-ii/kitchensink-ii-time.png)

Figure 6. Time Demo

#### Themes

The themes demo shows dynamically downloadable/installable themes. We tried to keep this demo as simple as possible so we integrated the theme list and hardcoded them in.

We could have embedded the themes in the app but that would have increased its size considerably thus crossing the free quota limit.

![Theme demo](/blog/kitchensink-ii/kitchensink-ii-themes.png)

Figure 7. Theme demo

#### Contacts

This is one of the more elaborate demos, we fetch the device contacts when available. E.g. JavaScript has no access to device contacts so we show a list of GoT characters.

You can swipe any contacts to the left/right to show details/options e.g. on the left you can dial a contact or share the contact information. On the right you can delete the contact permanently from your phone…​

![The contacts demo](/blog/kitchensink-ii/kitchensink-ii-contacts.png)

Figure 8. The contacts demo

#### Input

A simple table UI showing user input e.g. text fields etc.

This UI adapts itself to use the extra tablet space. When running in the phone it works differently for portrait/landscape. On rotation it adapts the UI with an animation.

We can also grab a picture from the gallery/camera to use on the top of the input form.

![The input UI when running in portrait](/blog/kitchensink-ii/kitchensink-ii-input.png)

Figure 9. The input UI when running in portrait

![The input UI when running in landscape or a tablet](/blog/kitchensink-ii/kitchensink-ii-input2.png)

Figure 10. The input UI when running in landscape or a tablet

#### Sales

The sales demo shows off a typical business application UI with a simple table whose changes you can see reflected in the chart above.

You can maximize/restore both the table and the chart using the icon on the top right.

We have two simple charts, the API is far more elaborate but we wanted to allow easy editing so we avoided complex UI’s.

![Sales demo chart](/blog/kitchensink-ii/kitchensink-ii-sales.png)

Figure 11. Sales demo chart

### Final Word

We spent some time refining this demo and we hope it shows. We think there is still a lot we should improve with it.

We will include the updated version in upcoming plugin updates but you can check out the code in github [here](https://github.com/codenameone/KitchenSink).

Notice that when we ship the demo in the stores we don’t call it a demo and refer to it as a tutorial…​ This is intentional, appstores prohibit demos but are fine with tutorial applications. It’s a silly semantic nuance but essential so you can try Codename One without jumping thru hoops.

__ |  We made the store screenshots using [theapplaunchpad.com](http://theapplaunchpad.com/), check it out   
---|---
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Lukman Javalove Idealist Jaji** — September 1, 2016 at 4:40 am ([permalink](https://www.codenameone.com/blog/kitchensink-ii.html#comment-23020))

> Hi Shai
>
> This is beautiful and congrtd to you and the team for the beautiful work. I must confess this demo will sell Codenameone better as it gives a better first impression. I once read one of the articles which says you’re working also on new Themes, It’s still the old ones that are here. Did I read wrongly or the themes will come in the coming updates.?
>
> Lukman
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fkitchensink-ii.html)


### **Shai Almog** — September 1, 2016 at 6:25 am ([permalink](https://www.codenameone.com/blog/kitchensink-ii.html#comment-22872))

> Hi,  
> thanks!
>
> We do want to work on better themes but this is something we tried repeatedly in the past with partial success. I think the right thing to do is work with a designer unfortunately we don’t have one on-staff at this time.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fkitchensink-ii.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
