---
title: ToastBar Actions & URLImage Caching
slug: toastbar-actions-urlimage-caching
url: /blog/toastbar-actions-urlimage-caching/
original_url: https://www.codenameone.com/blog/toastbar-actions-urlimage-caching.html
aliases:
- /blog/toastbar-actions-urlimage-caching.html
date: '2017-02-15'
author: Shai Almog
---

![Header Image](/blog/toastbar-actions-urlimage-caching/new-features-5.jpg)

I [wrote on the Friday](/blog/questions-of-the-week-42.html) post about a few cool pull requests from Diamond but I didn’t provide a usage example for that API. Probably the best usage example is gmail style undo. If you are not a gmail user then the gmail app essentially never prompts for confirmation!

It just does whatever you ask and pops a “toast message” with an option to undo. So if you clicked by mistake you have 3-4 seconds to take that back!

This is great and I wanted it for a while in Codename One. Diamond beat me to it by adding the ability to [have an action on a Toast message](https://github.com/codenameone/CodenameOne/pull/2033). This simple example shows you how you can undo any addition to the UI in a similar way to gmail:
    
    
    Form hi = new Form("Undo", BoxLayout.y());
    Button add = new Button("Add");
    
    add.addActionListener(e -> {
        Label l = new Label("Added this");
        hi.add(l);
        hi.revalidate();
        ToastBar.showMessage("Added, click here to undo...", FontImage.MATERIAL_UNDO,
                ee -> {
                    l.remove();
                    hi.revalidate();
                });
    });
    hi.add(add);
    hi.show();

That’s a pretty cool feature. I’m sure there are other use cases people can come up with…​

### Better URLImage

`URLImage` is great, it really changed the way we do some things in Codename One and I’m a bit shocked it took us **years** to introduce it…​

However, when we introduced it we didn’t have support for [cache filesystem](/blog/cache-sorted-properties-preferences-listener.html) or for the JavaScript port. The cache filesystem is probably the best place for images of `URLImage` so supporting that as a target is a “no brainer” but JavaScript seems to work so why would it need a special case?

Well, JavaScript already knows how to download and cache images from the web. `URLImage` is actually a step back from the things a good browser can do so why not use the native abilities of the browser when we are running there and fallback to using the cache filesystem if it’s available and as a last resort go to storage…​

That’s exactly what the new method of `URLImage` does:
    
    
    public static Image createCachedImage(String imageName, String url, Image placeholder, int resizeRule);

There are a few important things you need to notice about this method:

  * It returns **Image** and not **URLImage**. This is crucial. Down casting to `URLImage* will work on the simulator but might fail in some platforms (e.g. JavaScript) so don’t do that!  
Since this is implemented natively in JavaScript we need a different abstraction for that platform.

  * It doesn’t support image adapters and instead uses a simplified resize rule. Image adapters work on `URLImage` since we have a lot of control in that class. However, in the browser our control is limited and so an adapter won’t work.

If you do use this approach it would be far more efficient when running in the JavaScript port and will make better use of caching in most OS’s.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
