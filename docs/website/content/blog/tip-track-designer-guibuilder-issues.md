---
title: 'TIP: Track Designer & GUIBuilder Issues'
slug: tip-track-designer-guibuilder-issues
url: /blog/tip-track-designer-guibuilder-issues/
original_url: https://www.codenameone.com/blog/tip-track-designer-guibuilder-issues.html
aliases:
- /blog/tip-track-designer-guibuilder-issues.html
date: '2017-04-09'
author: Shai Almog
---

![Header Image](/blog/tip-track-designer-guibuilder-issues/tip.jpg)

We are on a mid-bootcamp break so the blog is back to life during this time (and there is so much to write!), more on that tomorrow but for now I’d like to write about fixing & tracking issues with the designer (resource editor) and the GUI builder.

Both of these tools have issues most of which resolve around their connection to the IDE. The reason for this is that they are external tools that aren’t a part of the IDE, this allows us to support all 3 IDE’s without too much of an effort but also creates some issues that are often hard to debug.

When you open either the designer or the GUI builder we install a JAR file within your system. This JAR file is located under the `.codenameone` directory in your home directory. E.g. on Linux/Mac it would be `~` but for Windows it can be under several hierarchies, see [this](https://en.wikipedia.org/wiki/Home_directory).

Once you locate the home directory peak inside, you should see two files: `designer_1.jar` & `guibuilder_1.jar`.

__ |  Both will only exist if you opened the designer and GUI builder, also notice that the `_1` part of the file name might be missing   
---|---  
  
### Command Line

You can launch both tools from Command Line. These tools write errors to the system console and you would see errors if they occur. Notice that both tools need Java 8 to work…​ To launch the designer use:
    
    
    java -jar designer_1.jar path-to-file.res

To launch the GUI builder use:
    
    
    java -jar guibuilder_1.jar

If you see errors please look them over and let us know what the full set of errors is.

### Problem: Designer Won’t Launch

This happens to some Eclipse users. The designer will launch from command line but not from Eclipse. Despite many attempts we failed to reproduce this.

Our current working theory is that Java home and the Java in the system path are incompatible maybe due to 64/32 bit modes or an older version of Java. Another theory is that a path with spaces or invalid characters is causing this issue.

If you are experiencing this issue please review your environment:

  * The bin directory of the JDK (important, the JDK **not** the JRE) must be first in the system path. Before anything else! Especially before Windows as some versions of the JDK stick Java into the system directory

  * `JAVA_HOME` **must** point at the JDK directory

  * You should have administrator privileges, I’m not sure if this is essential but please test this

  * Look in eclipse.ini and verify that the JDK listed there matches the JDK from the path

### GUI Builder Issues

There are several reasons for this and we try to address them with newer releases. To understand how this works check out the `.guiBuilder` directory in your home directory. In this directory you should see a file called `guibuilder.input` which is responsible for picking the right file to edit. This is mine:
    
    
    <?xml version="1.0" encoding="UTF-8"?>
    <con name="GuiBuilderTutorial" formName="MyGuiForm"  file="file:/Users/shai/temp/GuiBuilderTutorial/res/guibuilder/com/mycompany/myapp/MyGuiForm.gui" javaFile="file:/Users/shai/temp/GuiBuilderTutorial/src/com/mycompany/myapp/MyGuiForm.java" resFile="file:/Users/shai/temp/GuiBuilderTutorial/src/theme.res" outputFile="file:/Users/shai/.guiBuilder/733a5319-ceeb-458c-abad-6e2a6a061e05.ouput" running="file:/Users/shai/.guiBuilder/733a5319-ceeb-458c-abad-6e2a6a061e05" />

The important attributes here are `file` and `javaFile`. The former represents the XML gui file and the latter represents the Java source file related to that. If the path is invalid the GUI builder won’t find the right files and won’t know what to do.

The content of the `.gui` file might also be important if the GUI builder suddently stops working for a specific file.

### Finally

Please use the comments section for additional tips/questions on how to track issues in the GUI builder & the designer. I’ll try to update this post with newer details for GUI builder & designer debugging.

Hopefully, having this as a good resource will allow us to improve the tools to a level that this resource will become obsolete.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Señor Sentinel** — April 13, 2017 at 6:57 am ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-21575))

> Señor Sentinel says:
>
> I can’t launch the GUI builder and would appreciate some help. The whole issue is described at Stack Overflow: [http://stackoverflow.com/qu…](<http://stackoverflow.com/questions/43372597/getting-a-java-util-nosuchelementexception-error-when-launching-gui-designer>)
>



### **Shai Almog** — April 14, 2017 at 4:29 am ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-21739))

> Shai Almog says:
>
> This will be fixed by today’s release
>



### **Señor Sentinel** — April 14, 2017 at 6:29 am ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-23447))

> Señor Sentinel says:
>
> Hi Shai. Would that be version 3.6.2? My IntelliJ IDEA updated the plugin to that version, but I can’t find it on [Jetbrains plugin page]([https://plugins.jetbrains.c…](<https://plugins.jetbrains.com/plugin/7357-codename-one>)), which is a bit strange. Regardless, if you mean version 3.6.2 it didn’t solve anything for me. What’s the next step in trying to get this to work..?
>



### **Shai Almog** — April 15, 2017 at 4:29 am ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-23473))

> Shai Almog says:
>
> It’s 3.6.2 we were in the process of releasing it when you posted so the jetbrains site took a while to update. Did you try the steps above?  
> Does launching from command line work?  
> What’s in the XML launch file?
>



### **Señor Sentinel** — April 15, 2017 at 7:27 am ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-23171))

> Señor Sentinel says:
>
> Yep, I tried it all. It works just fine launching the designer using “java -jar guibuilder.jar”. I don’t have an XML file in the .guiBuilder directory. The only file I got there is a file called “CN1Preferences”, a binary files 151 bytes. (Also, the .guiBuilder dir itself has rights 777.)
>
> (As a side note I find it confusing getting support from Codename One. It’s a mix of commenting on blog posts, Stack Overflow, Google Groups and Intercom chat. Plus that I’ve requested twice last week that someone email me regarding your service “Client side development” to help me make an app – I never hear back from anyone. It’s a bit frustrating.)
>



### **Shai Almog** — April 16, 2017 at 4:43 am ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-21583))

> Shai Almog says:
>
> Do you have a guibuilder.input file? What’s inside it?
>
> I’ve explained the free support process here: [https://www.codenameone.com…](</blog/clarifying-our-support/>)
>
> I have no idea who you are but we got some contacts about app development over the past week which we answered. Either way our resources are pretty low at this time so if you need us to build your app for you we won’t be able to do anything before September.
>



### **Señor Sentinel** — April 16, 2017 at 7:12 pm ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-23504))

> Señor Sentinel says:
>
> No, I only have a CN1Preferences file (binary, 151 bytes) in the .guiBuilder directory. Just to be sure I did a “find . ~ | grep -i guibuilder.input” and found nothing anywhere in my home path.
>



### **Shai Almog** — April 17, 2017 at 5:38 am ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-23291))

> Shai Almog says:
>
> That sounds like a serious problem. Which OS are you using? Do you see anything in the intellij log after you try to launch the GUI builder?
>



### **Señor Sentinel** — April 17, 2017 at 6:31 am ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-23305))

> Señor Sentinel says:
>
> I’m on Debian Stable.
>
> Here’s the full log (106 lines) from me starting IntelliJ and trying to use the GUI Builder: [https://www.dropbox.com/s/1…](<https://www.dropbox.com/s/17onzpjebcnf19l/codenameone.log?dl=0>).
>
> (In the log I see that you add a file monitor at ~/.codenameone/open.txt. I don’t know if it’s relevant or important, but that file isn’t there. Just to be sure it wasn’t placed somewhere else I searched home, and it’s nowhere to be found. But, like I said, I have no idea if this is relevant at all.)
>



### **Shai Almog** — April 18, 2017 at 5:45 am ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-23155))

> Shai Almog says:
>
> That’s odd. Did you change something in the IntelliJ configuration?  
> Did you add modules or changed the default project structure?
>



### **Señor Sentinel** — April 18, 2017 at 6:32 am ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-23049))

> Señor Sentinel says:
>
> No, nothing, I don’t even know how to do that. I installed IntelliJ just to try Codename One. I haven’t even changed the colors of the editor.
>
> (I’ve used PyCharm for a while, but I can’t imagine their settings overlap. Besides, I’ve mostly adjusted colors and things like that, nothing deeply technical.)
>
> Basically, this is what I’ve done:  
> 1\. Installed IntelliJ  
> 2\. Installed CN1 from the built-in plugin repo  
> 3\. Created a standard CN1 Hello World project, explored the old GUI Builder a bit. Discovered there was a new GUI Builder.  
> 4\. Added a “Codename One Form” to the “src” folder (I remember getting an alert saying it should be in there).  
> 5\. Right-clicked the new form and tried “Codename One > GUI Builder”. Failed with error in SO link, googled – found nothing other than the recent error with chars in paths, which seems not applicable here.  
> 6\. Manually downloaded 3.5.3 from Jetbrains and installed using the function in IntelliJ (I didn’t copy files or anything – I have no idea where they go)  
> 7\. Tried, failed, googled again – found nothing that seemed relevant.  
> 8\. At next launch IntelliJ wanted to update plugin to 3.6.2, which I let it do.  
> 9\. Tried, failed.
>



### **shannah78** — April 18, 2017 at 5:46 pm ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-24145))

> shannah78 says:
>
> Looks like there is another bug in the IntelliJ plugin that prevents you from opening GUI forms that are in the root namespace. If you place your form inside a package (not just directly in your src directory), it should work. We’ll have this bug fixed with the next plugin update.
>



### **Señor Sentinel** — April 18, 2017 at 6:02 pm ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-23391))

> Señor Sentinel says:
>
> Ok, so I tried putting it inside a package instead, and now I didn’t get the same error anymore. But it did complain that my packagename couldn’t be found in the res/guibuilder/-path. So I created an empty directory there with the requested name and tried again. Now it complains “Couldn’t find GUI file matching: <path to=”” my=”” file=””>”. I’ve double checked the path in the error message, and that’s the exact file that I’m trying to use the GUI Builder on.
>



### **kutoman** — November 18, 2018 at 12:02 pm ([permalink](/blog/tip-track-designer-guibuilder-issues/#comment-24034))

> kutoman says:
>
> my saved states from the gui builder are not reflected on the respective java class. I’ve checked guibuilder.input, the paths are valid. There are also no errors printed when I run it over command line. I’ve recently initialized the codename one based project on IntelliJ IDEA. It’s the first time I’m using the (new) gui builder tool.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
