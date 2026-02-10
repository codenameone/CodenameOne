---
title: Performance True Story
slug: performance-true-story
url: /blog/performance-true-story/
original_url: https://www.codenameone.com/blog/performance-true-story.html
aliases:
- /blog/performance-true-story.html
date: '2016-08-24'
author: Shai Almog
---

![Header Image](/blog/performance-true-story/phone-espresso.jpg)

We had almost everything ready for the release of the kitchen sink demo this week until one of our fixes broke the build and we couldn’t get everything out in time. It’s disappointing but this means one more week to refine the demo.

During our debugging of the contacts demo that is a part of the new kitchen sink we noticed its performance was sub par. I assumed this was due to the implementation of `getAllContacts` & that there is nothing to do. While debugging another issue Steve noticed an anomaly during the loading of the contacts.

He then discovered that we are loading the same resource file over and over again for every single contact in the list!

In the new Contacts demo we have a share button for each contact, the code for constructing a `ShareButton` looks like this:
    
    
    public ShareButton() {
        setUIID("ShareButton");
        FontImage.setMaterialIcon(this, FontImage.MATERIAL_SHARE);
        addActionListener(this);
        shareServices.addElement(new SMSShare());
        shareServices.addElement(new EmailShare());
        shareServices.addElement(new FacebookShare());
    }

This seems reasonable until you realize that the constructors for `SMSShare`, `EmailShare` & `FacebookShare` load the icons for each of those…​

These icons are in a shared resource file that we load and don’t properly cache. The initial workaround was to cache this resource but a better solution was to convert this code:
    
    
    public SMSShare() {
        super("SMS", Resources.getSystemResource().getImage("sms.png"));
    }

Into this code:
    
    
    public SMSShare() {
        super("SMS", null);
    }
    
    @Override
    public Image getIcon() {
        Image i = super.getIcon();
        if(i == null) {
            i = Resources.getSystemResource().getImage("sms.png");
            setIcon(i);
        }
        return i;
    }

This way the resource uses lazy loading as needed.

This small change boosted the loading performance and probably the general performance due to less memory fragmentation.

The lesson that we should learn every day is to never assume about performance…​

### Scroll Performance – Threads aren’t magic

Another performance pitfall in this same demo came during scrolling. Scrolling was janky (uneven/unsmooth) right after loading finished would recover after a couple of minutes.

This relates to the images of the contacts.

To hasten the loading of contacts we load them all without images. We then launch a thread that iterates the contacts and loads an individual image for a contact. Then sets that image to the contact and replaces the placeholder image.

This performed well in the simulator but didn’t do too well even on powerful mobile phones. We assumed this wouldn’t be a problem because we used `Util.sleep()` to yield CPU time but that wasn’t enough.

Often when we see performance penalty the response is: “move it to a separate thread”. The problem is that this separate thread needs to compete for the same system resources and merge its changes back into the EDT. When we perform something intensive we need to make sure that the CPU isn’t needed right now…​

In this and past cases we solved this using a class member indicating the last time a user interacted with the UI.  
Here we defined:
    
    
    private long lastScroll;

Then we did this within the background thread
    
    
    // don't do anything while we are scrolling or animating
    long idle = System.currentTimeMillis() - lastScroll;
    while(idle < 1500 || contactsDemo.getAnimationManager().isAnimating() || scrollY != contactsDemo.getScrollY()) {
        scrollY = contactsDemo.getScrollY();
        Util.sleep(Math.min(1500, Math.max(100, 2000 - ((int)idle))));
        idle = System.currentTimeMillis() - lastScroll;
    }

Notice that we also check if the scroll changes, this allows us to notice cases like the animation of scroll winding down.

All we need to do now is update the `lastScroll` variable whenever user interaction is in place. This works for user touches:
    
    
    parentForm.addPointerDraggedListener(e -> lastScroll = System.currentTimeMillis());

This works for general scrolling:
    
    
    contactsDemo.addScrollListener(new ScrollListener() {
        int initial = -1;
        @Override
        public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
            // scrolling is sensitive on devices...
            if(initial < 0) {
                initial = scrollY;
            }
            lastScroll = System.currentTimeMillis();
            ...
        }
    });

__ |  Due to technical constraints we can’t use a lambda in this specific case…​   
---|---  
  
### Final Word

Performance is a chase that never ends. Its non-trivial and always changes on device/between devices.

The nice thing about cross platform tools is that once you optimize something on Android this often maps back to iOS etc. giving you a nice cross platform boost.

Some performance tips are generic and you can check them out in [our developer guide](/manual/) but performance is basically application specific. We wouldn’t have seen the `ShareButton` issue since the overhead is so small. But once we used `ShareButton` in an app where we created hundreds of buttons it became an issue…​
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Jérémy MARQUER** — August 26, 2016 at 7:19 am ([permalink](https://www.codenameone.com/blog/performance-true-story.html#comment-24205))

> Jérémy MARQUER says:
>
> One more time an interesting post …I feel particularly concerned by this performance problem because I’m using multiple threads in addition to EDT and network one. I have noticed that my application is affected by performance when there is a lot of components in a form (with images) on an IPAD 3 while it’s running very smoothly on IPAD AIR 2. I know that there is a huge difference between this 2 iPad but I have seen a lot of applications running smoothly on IPAD 3 …
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fperformance-true-story.html)


### **Shai Almog** — August 27, 2016 at 5:10 am ([permalink](https://www.codenameone.com/blog/performance-true-story.html#comment-22914))

> Shai Almog says:
>
> Thanks.  
> Notice that this behavior affects native code too, it’s mostly about stealing CPU from the rendering thread.
>
> We tried several workarounds in the past such as reducing the CPU priority of the network/auxiliary threads but that wasn’t very effective. We already have some API’s designed to indicate that CPU is needed for animation but they aren’t exposed, I think we need to offer something like this as a standard API but I need to give this some thought…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fperformance-true-story.html)


### **Carlos** — March 10, 2017 at 6:21 pm ([permalink](https://www.codenameone.com/blog/performance-true-story.html#comment-23321))

> Carlos says:
>
> Thank you. I had a similar performance issue due to loading images in a scrolling component. The scrolling was really bad during some seconds even in a very powerful device. I solved it by loading every image in a separate thread, now it works fine. It would be indeed helpful to have an api for this.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fperformance-true-story.html)


### **Shai Almog** — March 11, 2017 at 6:14 am ([permalink](https://www.codenameone.com/blog/performance-true-story.html#comment-23079))

> Shai Almog says:
>
> I’ve had a similar thought when I wrote this up but I can’t think of an API that would be generic enough to implement this pattern intelligently and intuitively.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fperformance-true-story.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
