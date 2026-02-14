---
title: New GUI Builder
slug: new-gui-builder
url: /blog/new-gui-builder/
original_url: https://www.codenameone.com/blog/new-gui-builder.html
aliases:
- /blog/new-gui-builder.html
date: '2015-09-20'
author: Shai Almog
---

![Header Image](/blog/new-gui-builder/new-gui-builder-preview.png)

Our GUI builder is the result of many twists and turns in our product line mostly due to corporate bureaucracy  
hacks and last minute deadlines from our days at Sun. Its also written using Swing which is pretty much a dead  
end API that isn’t seeing any maintenence and since FX is even worse off there isn’t much hope for the  
tools future.  
The GUI builder is also one of the most controversial parts of our UI. While it has its strengths and simplicities its  
UI could do with a facelift and its architecture was designed before tablets existed. It was designed when  
Apple just barely surpassed Blackberry sales and had just announced the iPhone 4 (first with Retina display). 

With that in mind we decided to rethink the GUI builder from the ground up with a complete from scratch  
rewrite. This allows us to keep the existing GUI builder for 100% compatibility for the forseeable future  
and add a decent migration path to developers who want to move to the new approach.  
We did learn from the bad experience with FX where there is still no decent migration path for Swing apps to  
this day so we intend to make the long term migration as easy as possible and intend to support the existing  
tool for a very long time!  
The obvious question is how will the new GUI builder differ from our current GUI builder? 

#### Built Using Codename One

With the new certificate wizard you saw the beginning of a trend that we hope to accelerate. Writing our  
tool using Codename One itself. This has numerous advantages to us but is also probably the best tool out  
there with Swing/FX being effectively dead, SWT somewhat stagnated there is really no decent Java UI  
library for us to use other than Codename One.  
When you add the JavaScript port of Codename One into the mix (not to mention the ability to run on devices)  
the value proposition is even bigger! 

#### The Resource File Format

The resource file format is a binary format we originally invented for themes, images etc. This made features  
like the GUI builder powerful yet somewhat awkward.   
As we moved forward we added an auxiliary XML format that supports themes and allows teams to work  
more effectively on the resource file. As part of that we added GUI builder XML file format support, this  
is the format that we will use for the GUI builder which will make the migration process very easy. 

#### The Statemachine

The original GUI builder was designed for simpler mobile apps that had far simpler UI. Most of the UI builders  
work was navigating between forms and adding commands. The state machine was a common practice that  
made a lot of sense for that point in time.  
Today we support, tablets, desktops and even web. With that in mind we decided to switch to a more traditional  
approach similar to standard desktop GUI builders where every Form/Container or Dialog in the GUI builder is  
represented by a single class and an XML file used to maintain the GUI builder state (see above).  
However, we intend to add a statemachine compatibility layer in the future to allow easier migration from the existing  
GUI builder to the new GUI builder with as little work as possible (although we can’t promise 100% seamlessness). 

#### Code Generation

The new GUI builder will generate code directly into a class representing the UI. So if you have a form called  
“MyForm” then you will have a Java source file named “MyForm.java” under the “src” hierarchy and an XML file  
called “res/guibuilder/mypackage/MyForm.gui” under the “res” hierarchy. This “.gui” file will be used to store/load  
the actual state from the GUI builder. The GUI builder ignores the source file almost completely except for the  
special case of event handling.  
The code generation will happen thru a new ant task that we already introduced in the build XML. It runs during  
build and scans the res directory for “.gui” files which it then converts to Java source blocks. The “MyForm.java”  
will have a special commented section that you aren’t allowed to touch, this is very similar to the NetBeans GUI  
builder architecture but we probably won’t physically lock these code blocks like NetBeans does. 

#### Releases & Limitations

We’d like to have a technology preview out for 3.2 but will probably release something sooner than that maybe  
within the next month (which marks the 5 year anniversary of the launch of the previous GUI builder).  
We don’t recommend the technology preview for production apps since the new tool might generate invalid  
code and require that you manually edit the XML files to workaround issues. With the 3.3 release  
we hope to graduate into beta level where we will start emphasizing this tool as “the gui builder”  
for new projects.   
The initial release won’t include standardized migration tools although we’ll try to add them as soon as possible  
as this is the best possible way for us to test the tool (by converting existing apps).  
The following big ticket features from the designer won’t be available with the initial release: 

  * List renderers – currently the designer uses a separate container as a renderer. Since the new GUI builder  
works in isolation this would be more tricky for us to expose.
  * Commands & Navigation – similarly to renderers this requires something more than a simplistic view of a single form.
  * Back Navigation, Embedded Container – These features won’t be available in the standard GUI builder since they  
rely on the statemachine architecture. We will add back navigation to the compatibility layer we plan to add and  
will try to include embedded container there too
  * `Toolbar` – the old GUI builder was very flexible in terms of command behavior which effectively made it  
very limited since it assumed nothing about command behavior. This turned out to be very in-flexible and prevented  
basic usability features such as “placing a button on the title”. The new designer assumes that the  
`Toolbar` class is used and will allow greater control over command placement to facilitate that.
  * Style manipulation/Localization – the existing GUI builder allowed customizing styles directly from the GUI builder UI. Since  
we are now separating the tools styles & localization won’t be available. You’d still be able to make use of both  
but not directly from the GUI builder UI.

#### Feedback

We are looking for feedback on the tooling and direction, we want to make the transition as seamless and painless  
as possible as we move forward. With that in mind I hope that once we launch this tool you’d give it a try  
and let us know what you think.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Jaanus Hansen** — September 21, 2015 at 3:00 pm ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-22346))

> Jaanus Hansen says:
>
> This is good news, that the long StateMachine will be split in the future 🙂
>



### **bryan** — September 21, 2015 at 8:39 pm ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-22331))

> bryan says:
>
> Sounds interesting. I wonder what you know about FX that the rest of us don’t – I can’t see Oracle trash canning it as I’m sure they have lots of desktop code they need some UI platform for.
>



### **Shai Almog** — September 22, 2015 at 3:54 am ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-24199))

> Shai Almog says:
>
> The top guys running FX are no longer on the core platform but were re-assigned. For a while focus shifted to embedded/IoT but the guy who was running this (one of the smartest developers/people I know) got fired with his entire staff because of corporate bureaucracy… Corporate bureaucracy is disabled when dealing with a truly important platform. We tried to get him to come on-board but he decided to go to Google.  
> No one uses FX despite years of no alternative. The only “real app” usage is in Swing apps that need what is now basic features such as a decent web browser etc. I’ve talked with several top Java consultants and asked them about their FX work, they all concur that this observation is accurate. The few “pure FX” projects are small student works etc.  
> The world moved in a different direction, FX has many similarities to Flash. In some regards its better but in others its worse. Adobe with all its abilities was unable to get Flash into devices (it even sucked on Android) and on modern desktops its hated. Java currently doesn’t have a “working by default” deployment strategy. Webstart and Applet’s were riddled with bugs and security holes, javafxwrapper is immature and has basic functionality missing.
>
> I agree that Java needs a desktop solution but FX is still far off from that point and needs a lot of effort to get there. Currently Oracle products use web for most of their UI’s and server side Java. E.g. the architecture of MAF which has the architecture diagram that is the very definition of over engineering.
>
> Unlike Google that does Spring cleaning, Oracle/Sun never really kill off projects. They just sort of abandon them and don’t invest time in them but everyone knows they are dead. Sun effectively abandoned Swing because it “only” had 30% market share, FX is probably under 1%.
>
> I was joking with the NetBeans guys a while back saying that they should rewrite NetBeans in Codename One, I don’t think that’s a joke anymore…
>



### **bryan** — September 22, 2015 at 6:51 am ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-21547))

> bryan says:
>
> I hope FX doesn’t disappear anytime soon as I’ve just spent the last 18 months on an FX app.
>
> With your new stuff, will you be supporting it on the desktop on all of Windows, Max and Linux ?
>



### **Shai Almog** — September 23, 2015 at 5:09 am ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-22427))

> Shai Almog says:
>
> FX is already “gone”, but it won’t disappear just like Swing didn’t disappear. The problem with FX will be similar to the problems with Swing, huge gaping bugs that go unfixed for quite a while e.g. file dialog not working on Windows 7 for ages was plaguing us on Swing… Getting DPI to work correctly on Macs with Retina display is a HUGE issue still, we can do basic things like adapt images but other than that…
>
> We support all OS’s with the new GUI builder as we always did.
>



### **bryan** — September 23, 2015 at 5:21 am ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-22288))

> bryan says:
>
> This is getting a bit off topic as regards actually using CN1, but I’m curious how you’re going to support all the platforms. I don’t imagine you’re going to have your own low level (Glib for example on Linux) graphics implementation – I assume you’re still going to use Swing or FX but have your own widget drawing primitives – essentially what CN1 does on Android etc now ?
>



### **Shai Almog** — September 23, 2015 at 11:09 am ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-22486))

> Shai Almog says:
>
> We already have a desktop port which uses that architecture and a JavaScript port which is more complex: [http://www.codenameone.com/…](<http://www.codenameone.com/blog/javascript-port.html>)  
> One of our thoughts for the long term is to actually port directly to native since we already have native OpenGL code in the iOS port. Right now the benefit of doing that is very low. The core benefit is being a bit closer to the way things are on the device and being able to build a truly native app for desktop (e.g. 1mb desktop app instead of bundling the whole VM).
>



### **Chidiebere Okwudire** — October 12, 2015 at 10:13 am ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-22494))

> Chidiebere Okwudire says:
>
> Nice to hear. Looking forward to it! I hope though that:
>
> 1\. The new GUI builder will make it easier to combine handcoded forms with GUI builder forms.
>
> 2\. It will be better documented (if for nothing else, to reduce the number of times you get asked the same questions on the forum about back navigation, skipping a form in the navigation stack, etc. :D). For even simple customizations to default behavior, I find myself having to dive into the source code either because the documentation is unclear or not quite available.
>
> 3\. Last but certainly not least, we’ll see more GUI builder-based demos because it’s hard to sell something that you guys yourselves don’t seem to be using that much 🙂
>
> Cheers
>



### **Shai Almog** — October 12, 2015 at 12:08 pm ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-22436))

> Shai Almog says:
>
> 1\. Its designed to be a more traditional GUI builder so it should be pretty easy to mix GUI/handcoded styles. It would also be possible to use it in a library.
>
> You would use it with a Manual type application and not with a GUI builder application.
>
> 2/3. Our existing GUI builder quite a bit of documentation and tutorials, most older demos/tutorials were based on it e.g. [http://udemy.com/build-mobi…](<http://udemy.com/build-mobile-ios-apps-in-java-using-codename-one/>)
>
> Some things like the back behavior are really hard to teach/document since people don’t really know where to begin. Unlike coding where you have the javadoc and developer guide when you are in a GUI builder environment the terminology isn’t there as much… Since the new GUI builder won’t focus on navigation as much this sort of behavior won’t be as common.
>
> We stopped using the GUI builder for newer demos because it made it harder for some developers to follow the process (as the result was binary and not source). We planned this rewrite for years so we intended to focus on the new GUI builder when it lands.
>



### **Chidiebere Okwudire** — December 9, 2015 at 11:26 am ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-21489))

> Chidiebere Okwudire says:
>
> Hi,
>
> I’m curious about the status of the new GUI builder? Any ideas when the first stable version is due? Early next year I want to port an application to CN1 and the concepts in the new GUI builder are more suited for my use case. Of course, I know I can code the forms manually but let’s say, I’m more visually-oriented and/or lazy 🙂
>



### **Shai Almog** — December 9, 2015 at 12:01 pm ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-22531))

> Shai Almog says:
>
> Hi,  
> we are aiming for beta quality with 3.3 which is due for January 27th if I remember correctly. Production grade probably won’t appear before 3.4.
>
> Right now the biggest problem is the lack of issues which partially relates to lack of tutorials (still pointing at the old GUI builder). We are reluctant to redo the tutorials pointing at a shifting/changing tool so this isn’t ideal.
>
> The nice thing about the new GUI builder is that even if it completely breaks down you can still use the XML to recover so a disaster is less likely.
>



### **Chidiebere Okwudire** — December 9, 2015 at 8:49 pm ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-22153))

> Chidiebere Okwudire says:
>
> Ok; thanks for the update. _When_ I start the migration, I hope I can provide some useful feedback.
>



### **Akinniranye James** — September 10, 2016 at 5:52 pm ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-23040))

> Akinniranye James says:
>
> The builder is having issues with tabs. Anybody tried creating tabs?
>



### **Shai Almog** — September 11, 2016 at 6:38 am ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-22854))

> Shai Almog says:
>
> Yes, it partially works.  
> We fixed several issues in tabs that will be included in the coming update.
>



### **Akinniranye James** — September 11, 2016 at 7:06 am ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-21656))

> Akinniranye James says:
>
> Do you have any date in mind?
>



### **Shai Almog** — September 12, 2016 at 4:16 am ([permalink](https://www.codenameone.com/blog/new-gui-builder.html#comment-22715))

> Shai Almog says:
>
> We hope to have it in this Friday release.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
