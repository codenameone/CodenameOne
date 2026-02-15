---
title: Sticky Headers
slug: sticky-headers
url: /blog/sticky-headers/
original_url: https://www.codenameone.com/blog/sticky-headers.html
aliases:
- /blog/sticky-headers.html
date: '2016-05-22'
author: Shai Almog
---

![Header Image](/blog/sticky-headers/sticky-headers.png)

Sticky headers was one of the first big requests we said no to. Back in the day a lot of people asked for  
it but we always shot it down because it was too hard to implement on top of our Swing inspired lists.  
This predated our Container improvement, [InfiniteContainer](/javadoc/com/codename1/ui/InfiniteContainer/)  
and [InfiniteScrollAdapter](/javadoc/com/codename1/components/InfiniteScrollAdapter/).

While our preference for lists has waned we never got around to show how easy it is to do sticky headers with `Container`  
until last week when [Chen](https://twitter.com/CFishbein/) released the  
[sticky headers demo](https://github.com/chen-fishbein/stickyheaders-codenameone).

It’s a pretty basic demo and relatively simple at that but that’s exactly the point. Codename One is flexible  
enough to do pretty much anything you want especially if you don’t lock yourself into a rather restrictive  
component like `List`.

**Check out a live demo to the right here…​**

You can see the full source for the demo  
[here](https://github.com/chen-fishbein/stickyheaders-codenameone).

Chen chose to implement this on top of the glass pane which provides some capabilities and control over graphics,  
you could also use the layered pane or a `LayeredLayout` to achieve similar effects as well.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chibuike Mba** — May 26, 2016 at 7:15 am ([permalink](/blog/sticky-headers/#comment-22859))

> Chibuike Mba says:
>
> Hi Shai, does List component support InfiniteScrollAdapter?
>



### **Shai Almog** — May 27, 2016 at 5:09 am ([permalink](/blog/sticky-headers/#comment-22450))

> Shai Almog says:
>
> Hi,  
> No. It’s an either/or situation. Infinite components are meant to replace list.
>



### **Chibuike Mba** — May 27, 2016 at 6:43 am ([permalink](/blog/sticky-headers/#comment-22919))

> Chibuike Mba says:
>
> Ok Shai, I will experiment with Infinite components to know if they will serve my needs. But is there any performance advantage of using Infinite components over List?
>



### **Shai Almog** — May 29, 2016 at 2:55 am ([permalink](/blog/sticky-headers/#comment-22783))

> Shai Almog says:
>
> That depends a lot on your use case but generally yes.  
> The performance advantage starts to erode in the thousands of elements.
>



### **Nigel Chomba** — May 30, 2016 at 9:48 am ([permalink](/blog/sticky-headers/#comment-22703))

> Nigel Chomba says:
>
> its causing Stack overflow error..[EDT] 0:0:0,0 – java.lang.StackOverflowError
>



### **Chen Fishbein** — May 30, 2016 at 11:57 am ([permalink](/blog/sticky-headers/#comment-22674))

> Chen Fishbein says:
>
> Can you please open an issue here:[https://github.com/chen-fis…](<https://github.com/chen-fishbein/stickyheaders-codenameone>)  
> With a snippet of code that reproduces this error
>



### **Chibuike Mba** — July 21, 2016 at 3:05 pm ([permalink](/blog/sticky-headers/#comment-22983))

> Chibuike Mba says:
>
> Hi Shai and Chen,  
> I use StickyHeader class in one of my app’s UIs and it gave me what I want in terms of layout as can be seen in first image as attached to my post.
>
> But am having 2 challenges:
>
> 1) When the list (MultiButtons inside Tab) is scrolled, if the floating action button (FAB) is on screen the StickyHeader component overlaps the Toolbar as can be seen in the second image but if I remove the FAB (which is added to LayeredPane) it displays well as can be seen in the third image.
>
> In my Form layout I have 3 components added directly to the Form (BoxLayout.y): First Container, Second StickyHeader and third Tab as can be seen in the fourth image.
>
> 2) When the list is scrolled and the StickyHeader component (which contains my Tab title buttons) sticks at the top of the screen the tab title buttons(SUBSCRIBERS AND FOLLOW UPS) stops responding to clicks.
>
> Please you assistance in helping me resolve these issues is highly anticipated and appreciated.
>
> Kind Regards.
>



### **Shai Almog** — July 22, 2016 at 4:45 am ([permalink](/blog/sticky-headers/#comment-22951))

> Shai Almog says:
>
> Nice looking app! Is it in the gallery?
>
> 1\. I don’t see that issue, am I missing something in the image?  
> It looks like you are using title hiding too, is it possible it collides with that?
>
> 2\. Can you reproduce this in a standalone testcase?
>



### **Chibuike Mba** — July 22, 2016 at 1:09 pm ([permalink](/blog/sticky-headers/#comment-22905))

> Chibuike Mba says:
>
> “Nice looking app!” Thanks.
>
> “Is it in the gallery?” Yes, but this particular screen is part of new version currently under development and will be out before the end of August.
>
> “1. I don’t see that issue, am I missing something in the image?” Sorry, the images reordered from the way I uploaded them.
>
> “It looks like you are using title hiding too, is it possible it collides with that?” No, am not using Title hidden, that is where the issue lies, the StickyHeader component just collides with and covers the Toolbar while scrolling (as you can see in the image without back icon) and this occurs only when I add any component to the Form’s LayeredPane (link FAB).
>
> If I did not add any component to the Form’s LayeredPane (as you can see in the image without FAB button at the bottomright side of the screen) the StickyHeader component will behave well and stick below the Toolbar while scrolling.
>
> The StickyHeader component dimension is highlighted with black bordered rectangle in on of the images.
>
> “2. Can you reproduce this in a standalone testcase?” Ok, I will try to reproduce it this weekend.
>
> Am suspecting that LayeredPane components conflicts with GlassPane components in the same Form.
>
> Thanks.
>



### **Shai Almog** — July 23, 2016 at 4:50 am ([permalink](/blog/sticky-headers/#comment-22886))

> Shai Almog says:
>
> Layered pane and glass pane are very different but if you placed major components in the layered pane this might produce a conflict as we sometimes use the layered pane for various effects. We now support a multi-layered pane which should work better with less conflicts but haven’t really migrated to that.
>



### **Chibuike Mba** — July 23, 2016 at 11:57 pm ([permalink](/blog/sticky-headers/#comment-21517))

> Chibuike Mba says:
>
> I modified the StickyHeader class and it worked for me.
>
> I changed from GlassPane used by StickyHeader class to LayeredPane and my 2 challenges where solved:  
> 1)No more overlapping with Toolbar(sticks below Toolbar as expected).  
> 2)The StickyHeader is now interactive(can respond to events when it sticks below Toolbar).
>
> Here is the modified StickyHeader class code:
>
> /*  
> * To change this license header, choose License Headers in Project Properties.  
> * To change this template file, choose Tools | Templates  
> * and open the template in the editor.  
> */  
> package com.codename1.ui;
>
> import com.codename1.ui.events.ScrollListener;  
> import com.codename1.ui.layouts.BorderLayout;  
> import java.util.ArrayList;
>
> /**  
> *  
> * @author Chen  
> */  
> public class StickyHeader extends Container implements ScrollListener {
>
> private int previousPosition;
>
> private boolean needToCheck = false;  
> private Component comp = null;
>
> public StickyHeader() {  
> }
>
> @Override  
> protected void initComponent() {  
> super.initComponent();  
> Container p = getParent();  
> p.addScrollListener(this);  
> previousPosition = getParent().getAbsoluteY() – getAbsoluteY();  
> }
>
> @Override  
> protected void laidOut() {  
> if(getY() == 0){  
> needToCheck = true;  
> }  
> }
>
> @Override  
> public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {  
> int position = getParent().getAbsoluteY() + scrollY – getAbsoluteY();  
> if(position >= 0){  
> if(previousPosition < 0){  
> needToCheck = true;  
> }  
> }else{  
> if(previousPosition > 0){  
> needToCheck = true;  
> }  
> }  
> if (scrollY – oldscrollY >= 0) {  
> if(needToCheck){  
> pushToHeader();  
> }  
> }else{  
> ArrayList stack = (ArrayList) getParent().getClientProperty(“sticky”);  
> if (stack != null && !stack.isEmpty()&& stack.get(0) == this && position<0) {  
> popFromHeader();  
> }  
> }  
> previousPosition = position;  
> needToCheck = false;  
> }
>
> @Override  
> void paintGlassImpl(Graphics g) {  
> }
>
> private void popFromHeader() {  
> ArrayList stack = (ArrayList) getParent().getClientProperty(“sticky”);  
> stack.remove(0);
>
> if(!stack.isEmpty()){  
> StickyHeader h = (StickyHeader)stack.get(0);  
> h.installSticky();  
> }else{  
> getComponentForm().getLayeredPane().removeComponent(comp);  
> addComponent(comp);  
> }  
> }
>
> private void pushToHeader() {  
> ArrayList stack = (ArrayList) getParent().getClientProperty(“sticky”);  
> if (stack == null) {  
> stack = new ArrayList();  
> getParent().putClientProperty(“sticky”, stack);  
> }  
> if(!stack.isEmpty()){  
> StickyHeader h = (StickyHeader) stack.get(0);  
> if(getY() < h.getY()){  
> return;  
> }  
> }  
> stack.add(0, this);  
> installSticky();  
> }
>
> void installSticky() {  
> if(getComponentCount()<1)  
> return;
>
> comp = getComponentAt(0);  
> removeComponent(comp);  
> getComponentForm().getLayeredPane().addComponent(BorderLayout.NORTH, comp);  
> }  
> }
>
> The changes:
>
> 1) Added comp variable (to hold component added to StickyHeader before removing the component from the parent form and adding to layered pane).
>
> 2) Modified popFromHeader() method (precisely, the else statement)
>
> 3) Modified installSticky() method
>
> 4) Removed createPainter() method (as is no longer needed)
>
> NB: The modified StickyHeader class assumes that the layered pane has borderlayout and that not component is already added to the north.
>
> NB: The multi-layered pane you mentioned will be handy to make sure that the StickyHeader component remains in its own layered pane.
>
> The image bellow shows my UI as I expect.
>
> Thanks for your guide.
>



### **Shai Almog** — July 24, 2016 at 3:45 am ([permalink](/blog/sticky-headers/#comment-22550))

> Shai Almog says:
>
> Looks great. I really like the floating folded button, what did you do there?
>



### **Chibuike Mba** — July 24, 2016 at 2:27 pm ([permalink](/blog/sticky-headers/#comment-21632))

> Chibuike Mba says:
>
> Thanks Shai.
>
> I have FAB class (Floating Action Button – custom component) that accept 3 arguments (bg image, icon and title)
>
> In some of the screens above with single FAB button (with menu icon), I just added the button to the layered pane of the Form class and when clicked, I add other FAB buttons to Dialog class layered pane and show it, as can be seen above.
>
> The code for add FAB buttons to Dialog is as below:
>
> Container con = BoxLayout.encloseY(fabNewsletter, fabAdd, fabMark, fabClear);
>
> Dialog f = new Dialog();  
> f.setDialogUIID(“Container”);
>
> f.getLayeredPane().removeAll();  
> f.showPacked(BorderLayout.CENTER, false);  
> f.getLayeredPane().setLayout(new BorderLayout());  
> f.getLayeredPane().add(BorderLayout.SOUTH, new Container(new BorderLayout()).add(BorderLayout.EAST, con));
>
> con.animateLayoutAndWait(10);
>
> Kind Regards.
>



### **Shai Almog** — July 25, 2016 at 4:36 am ([permalink](/blog/sticky-headers/#comment-22829))

> Shai Almog says:
>
> Thanks, that looks very similar to some of the code I wrote that does the same. We didn’t publish it because we’d like to get a lot of the nuance of that component into place before we bring out official support.
>



### **Ch Hjelm** — September 18, 2016 at 9:57 am ([permalink](/blog/sticky-headers/#comment-22980))

> Ch Hjelm says:
>
> Hi Chen or Shai,  
> The StickyHeaders are a really great addition to the UI, do you think you might consider incorporating them into the CN1 platform?
>
> Right now, as Chen wrote, you need to copy the [StickyHeader.java](<http://StickyHeader.java>) file into your local copy of CN1 sources, which as far as I understand means that you cannot use them when you build for a device.
>
> I’ve also noticed that the StickyHeader is shown on top of the ToolBar instead of on top of the scrollable list, so not quite useable as-is.
>
> Finally, if you do incorporate them, it would be a neat touch to add an animation when one header replaces another.
>
> Hoping for the best 🙂
>



### **Shai Almog** — September 19, 2016 at 3:54 am ([permalink](/blog/sticky-headers/#comment-23134))

> Shai Almog says:
>
> Hi,  
> no you use it in your own project which means you can use it when building for devices. Chen’s implementation depends on internal package protected behavior which means that one of the classes must reside within the com.codename1.ui package.  
> This doesn’t require any modification to the Codename One sources.
>
> Because this class isn’t as mature as some other classes we don’t have any short term plans to incorporate this into Codename One. It is also pretty difficult to implement this in a generic way.
>
> Also check out alternative implementations mentioned in the comments below.
>



### **Ch Hjelm** — September 20, 2016 at 8:43 pm ([permalink](/blog/sticky-headers/#comment-23093))

> Ch Hjelm says:
>
> Thanks, but on Chen’s github page ([https://github.com/chen-fis…](<https://github.com/chen-fishbein/stickyheaders-codenameone>)) it says “Important – make sure the class stays in the com.codename1.ui package.” And when I put the [StickyHeader.java](<http://StickyHeader.java>) file there (in my copy of the CN1 sources), I can compile on my machine, but the build for devices fails, saying that [StickyHeaders.java](<http://StickyHeaders.java>) is missing. So, I don’t understand how it can be used?
>
> I also tried Chibuike’s version below, but it caused some other problems. And I’d obviously prefer to benefit from the existing implementation to not have to get into the source code myself 🙂
>



### **Shai Almog** — September 21, 2016 at 4:23 am ([permalink](/blog/sticky-headers/#comment-23149))

> Shai Almog says:
>
> You can create that package within your project. You shouldn’t modify our sources to do that. In your project create a new package with the right name and place it there.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
