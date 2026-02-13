---
title: Gallery, Icons & Updates
slug: gallery-icon-update
url: /blog/gallery-icon-update/
original_url: https://www.codenameone.com/blog/gallery-icon-update.html
aliases:
- /blog/gallery-icon-update.html
date: '2015-03-17'
author: Shai Almog
---

![Header Image](/blog/gallery-icon-update/gallery-chart-fonts-icon.png)

Now that the new website is live we are down to standard business again beyond just the typical bug fixes. Chen just updated the gallery support to include the ability to pick a video file and not just an image file.  
The default functionality remains the same and compatible but now you can do something like: 
    
    
    Display.getInstance().openGallery(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            if(ev != null && ev.getSource() != null) {
                String filePathToGalleryImageOrVideo = (String)ev.getSource();
                ....
            }
        }
    }, Display.GALLERY_ALL);

You can now use GALLERY_ALL, GALLERY_VIDEO or GALLERY_IMAGE to pick either one or both when showing  
the gallery UI.

A rather interesting issue was opened last week where a developer got a reject from Apple’s upload tool  
due to an invalid binary. Missing the Icon.png file…  
Our iOS project build is pretty complex e.g.  
[it takes screenshots](http://www.codenameone.com/blog/the-7-screenshots-of-ios)  
and it also handles things like icon resizing. The latter is much simpler, we generate the roughly 10 icon  
sizes the Apple needs for the various iOS devices from the one icon you supply (seriously they expect 10 icons…). 

We store these icons in the bundle and in this case the developer also stored an icon.png file in the root triggering  
a collision. Naturally our build process needs to be smarter and catch onto those things but I would recommend  
that you also practice caution when placing files in the root and try to place as much as possible within res files  
(yes you can have more than one) especially because they let you take advantage of multi-images. 

#### Quick note about the github migration plans

We haven’t started yet since we need to cleanup a lot of things first. We also need to start updating the  
plugins and everything involved to the new URL’s. Once everything is planned out we will start a migration,  
hopefully issues will be able to make the transition.  
We hope to publish a detailed transition plan sometime in the next couple of weeks.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
